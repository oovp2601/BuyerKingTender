package buyerking.web;

import buyerking.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Handles all order-related API requests:
 * GET /api/orders — list all orders (for seller dashboard)
 * POST /api/orders — create a new order (called by buyer chatbot)
 * POST /api/orders/update — update order status to accepted/rejected
 */
public class OrderServlet implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && path.equals("/api/orders")) {
            handleGetOrders(exchange);
        } else if ("POST".equals(method) && path.equals("/api/orders")) {
            handleCreateOrder(exchange);
        } else if ("POST".equals(method) && path.equals("/api/orders/update")) {
            handleUpdateOrder(exchange);
        } else {
            sendResponse(exchange, 405, ApiResponse.error("Method not allowed"));
        }
    }

    /** GET /api/orders — returns all orders ordered by newest first */
    private void handleGetOrders(HttpExchange exchange) throws IOException {
        try {
            ensureOrdersTableExists();
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT order_id, buyer_name, items_json, total_price, status, created_at " +
                            "FROM orders ORDER BY created_at DESC");

            StringBuilder jsonArray = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    jsonArray.append(",");
                first = false;
                // Escape the items_json value carefully
                String itemsJson = rs.getString("items_json");
                if (itemsJson == null)
                    itemsJson = "[]";
                jsonArray.append(String.format(
                        "{\"order_id\":%d,\"buyer_name\":\"%s\",\"items\":%s,\"total_price\":%.2f,\"status\":\"%s\",\"created_at\":\"%s\"}",
                        rs.getInt("order_id"),
                        ApiResponse.escape(rs.getString("buyer_name")),
                        itemsJson,
                        rs.getDouble("total_price"),
                        rs.getString("status"),
                        rs.getString("created_at")));
            }
            jsonArray.append("]");

            sendResponse(exchange, 200, ApiResponse.success("Orders fetched", jsonArray.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, ApiResponse.error("Internal Server Error: " + e.getMessage()));
        }
    }

    /** POST /api/orders — create a new order from the buyer chatbot */
    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        try {
            ensureOrdersTableExists();
            String body = readBody(exchange);

            // Parse simple JSON fields: buyerName, itemsJson, totalPrice
            String buyerName = parseJsonString(body, "buyerName");
            String itemsJson = parseJsonString(body, "itemsJson");
            String totalPriceStr = parseJsonString(body, "totalPrice");

            if (buyerName == null || buyerName.isEmpty())
                buyerName = "Guest";
            if (itemsJson == null || itemsJson.isEmpty())
                itemsJson = "[]";
            double totalPrice = 0.0;
            if (totalPriceStr != null && !totalPriceStr.isEmpty()) {
                try {
                    totalPrice = Double.parseDouble(totalPriceStr);
                } catch (NumberFormatException ignored) {
                }
            }

            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders (buyer_name, items_json, total_price, status) VALUES (?, ?, ?, 'pending')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, buyerName);
            ps.setString(2, itemsJson);
            ps.setDouble(3, totalPrice);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int newId = 0;
            if (keys.next())
                newId = keys.getInt(1);

            sendResponse(exchange, 200,
                    ApiResponse.success("Order created", String.valueOf(newId)));
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, ApiResponse.error("Internal Server Error: " + e.getMessage()));
        }
    }

    /** POST /api/orders/update — update order status */
    private void handleUpdateOrder(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange);
            String orderIdStr = parseJsonString(body, "orderId");
            String status = parseJsonString(body, "status");

            if (orderIdStr == null || status == null) {
                sendResponse(exchange, 400, ApiResponse.error("Missing orderId or status"));
                return;
            }
            if (!status.equals("accepted") && !status.equals("rejected") && !status.equals("pending")) {
                sendResponse(exchange, 400, ApiResponse.error("Invalid status value"));
                return;
            }

            int orderId = Integer.parseInt(orderIdStr.trim());
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET status = ? WHERE order_id = ?");
            ps.setString(1, status);
            ps.setInt(2, orderId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                sendResponse(exchange, 200, ApiResponse.success("Order updated", "\"" + status + "\""));
            } else {
                sendResponse(exchange, 404, ApiResponse.error("Order not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, ApiResponse.error("Internal Server Error: " + e.getMessage()));
        }
    }

    // ── Static helper used by ChatbotInterface to save orders ─────────────
    public static int saveOrder(String buyerName, String itemsJson, double totalPrice) {
        try {
            ensureOrdersTableExists();
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders (buyer_name, items_json, total_price, status) VALUES (?, ?, ?, 'pending')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, buyerName);
            ps.setString(2, itemsJson);
            ps.setDouble(3, totalPrice);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
        } catch (Exception e) {
            System.err.println("Failed to save order: " + e.getMessage());
        }
        return -1;
    }

    public static void ensureOrdersTableExists() {
        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS orders (" +
                            "  order_id INT AUTO_INCREMENT PRIMARY KEY," +
                            "  buyer_name VARCHAR(100) DEFAULT 'Guest'," +
                            "  items_json TEXT NOT NULL," +
                            "  total_price DECIMAL(12,2) DEFAULT 0.00," +
                            "  status ENUM('pending','accepted','rejected') DEFAULT 'pending'," +
                            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")");
        } catch (Exception e) {
            System.err.println("Warning creating orders table: " + e.getMessage());
        }
    }

    // ── Utility methods ────────────────────────────────────────────────────
    private String readBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        return new BufferedReader(isr).lines().collect(Collectors.joining("\n"));
    }

    /**
     * Very simple JSON string parser — finds "key":"value" in a flat JSON body.
     * Works for string values only (quoted). Returns null if not found.
     */
    private String parseJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1)
            return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1)
            return null;
        // Skip whitespace
        int valueStart = colon + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart)))
            valueStart++;
        if (valueStart >= json.length())
            return null;
        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            // Quoted string — find closing quote (not preceded by \)
            int end = valueStart + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\')
                    break;
                end++;
            }
            return json.substring(valueStart + 1, end).replace("\\\"", "\"");
        } else {
            // Unquoted (number/boolean) — read until , or }
            int end = valueStart;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}')
                end++;
            return json.substring(valueStart, end).trim();
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
