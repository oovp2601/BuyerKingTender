package buyerking.web;

import buyerking.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageServlet implements HttpHandler {

    public static void ensureMessagesTable() {
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // SQLite specific check
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet res = meta.getTables(null, null, "messages", null)) {
                if (!res.next()) {
                    stmt.execute(
                            "CREATE TABLE messages (" +
                                    "  msg_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "  sender VARCHAR(100)," +
                                    "  receiver VARCHAR(100)," +
                                    "  content TEXT," +
                                    "  is_read BOOLEAN DEFAULT FALSE," +
                                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                    ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Warning creating messages table: " + e.getMessage());
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            ensureMessagesTable();
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                String sender = parseJsonString(body, "sender");
                String receiver = parseJsonString(body, "receiver");
                String content = parseJsonString(body, "content");

                System.out.println("Processing Message POST: sender=" + sender + ", receiver=" + receiver + ", content="
                        + content);

                try (Connection conn = DatabaseManager.getConnection();
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO messages (sender, receiver, content) VALUES (?, ?, ?)")) {
                    ps.setString(1, sender);
                    ps.setString(2, receiver);
                    ps.setString(3, content);
                    ps.executeUpdate();
                    System.out.println("Message saved into database successfully.");
                    sendResponse(exchange, 200, ApiResponse.success("Message sent", "\"OK\""));
                }
            } else if ("GET".equals(exchange.getRequestMethod())) {
                String uri = exchange.getRequestURI().toString();
                String targetReceiver = "Guest";
                if (uri.contains("receiver=Seller")) {
                    targetReceiver = "Seller";
                }

                // Fetch unread messages
                try (Connection conn = DatabaseManager.getConnection()) {
                    List<Integer> idsToMark = new ArrayList<>();
                    StringBuilder jsonArray = new StringBuilder("[");

                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT msg_id, sender, content FROM messages WHERE receiver = ? AND is_read = FALSE")) {
                        ps.setString(1, targetReceiver);
                        try (ResultSet rs = ps.executeQuery()) {
                            boolean first = true;
                            while (rs.next()) {
                                if (!first)
                                    jsonArray.append(",");
                                first = false;
                                int id = rs.getInt("msg_id");
                                idsToMark.add(id);
                                jsonArray.append(String.format("{\"id\":%d,\"sender\":\"%s\",\"content\":\"%s\"}",
                                        id, ApiResponse.escape(rs.getString("sender")),
                                        ApiResponse.escape(rs.getString("content"))));
                            }
                        }
                    }
                    jsonArray.append("]");

                    if (!idsToMark.isEmpty()) {
                        String inClause = idsToMark.stream().map(String::valueOf).collect(Collectors.joining(","));
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("UPDATE messages SET is_read = TRUE WHERE msg_id IN (" + inClause + ")");
                        }
                    }

                    sendResponse(exchange, 200, ApiResponse.success("Messages fetched", jsonArray.toString()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, ApiResponse.error(e.getMessage()));
        }
    }

    private String parseJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1)
            return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1)
            return null;
        int valueStart = colon + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart)))
            valueStart++;
        if (valueStart >= json.length())
            return null;
        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            int end = valueStart + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\')
                    break;
                end++;
            }
            return json.substring(valueStart + 1, end).replace("\\\"", "\"");
        } else {
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
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
