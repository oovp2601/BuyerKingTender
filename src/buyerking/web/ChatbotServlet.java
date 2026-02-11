package buyerking.web;

import buyerking.ChatbotInterface;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ChatbotServlet implements HttpHandler {

    private ChatbotInterface chatbot;

    public ChatbotServlet() {
        this.chatbot = new ChatbotInterface();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equals(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            String response = ApiResponse.error("Method not allowed");
            sendResponse(exchange, 405, response);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String body = br.lines().collect(Collectors.joining("\n"));

        // Parse simple JSON { "message": "..." } manually to avoid heavy libs
        String message = "";
        if (body.contains("\"message\"")) {
            // Very basic parsing, assumes "message": "content" format
            int start = body.indexOf("\"message\"");
            if (start != -1) {
                start = body.indexOf(":", start) + 1;
                start = body.indexOf("\"", start) + 1;
                int end = body.lastIndexOf("\"");
                if (start > 0 && end > start) {
                    message = body.substring(start, end);
                    // Unescape quotes if any
                    message = message.replace("\\\"", "\"");
                }
            }
        }

        if (message.isEmpty()) {
            // Fallback for plain text if JSON parsing fails or isn't used
            if (!body.trim().startsWith("{")) {
                message = body.trim();
            }
        }

        if (message.isEmpty()) {
            sendResponse(exchange, 400, ApiResponse.error("Empty message"));
            return;
        }

        String reply = chatbot.processCommand(message);

        // formatting reply for JSON safety
        String jsonReply = "\"" + ApiResponse.escape(reply) + "\"";
        String response = ApiResponse.success("Reply generated", jsonReply);

        sendResponse(exchange, 200, response);
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
