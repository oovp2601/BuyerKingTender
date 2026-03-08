package buyerking.web;

import buyerking.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

public class TenderServlet implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            handleGet(exchange);
        } else {
            String response = ApiResponse.error("Method not allowed");
            sendResponse(exchange, 405, response);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM tenders ORDER BY created_at DESC LIMIT 20")) {

            StringBuilder jsonArray = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    jsonArray.append(",");
                first = false;

                jsonArray.append(String.format(
                        "{\"id\": %d, \"title\": \"%s\", \"description\": \"%s\", \"max_price\": %.2f, \"status\": \"%s\"}",
                        rs.getInt("tender_id"),
                        ApiResponse.escape(rs.getString("title")),
                        ApiResponse.escape(rs.getString("description")),
                        rs.getDouble("max_price"),
                        rs.getString("status")));
            }
            jsonArray.append("]");

            String response = ApiResponse.success("Tenders fetched", jsonArray.toString());
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, ApiResponse.error("Internal Server Error: " + e.getMessage()));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
