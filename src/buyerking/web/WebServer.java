package buyerking.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class WebServer {

    private static final int PORT = 8080;
    private HttpServer server;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // API Endpoints
            server.createContext("/api/chat", new ChatbotServlet());
            server.createContext("/api/tenders", new TenderServlet());
            OrderServlet orderServlet = new OrderServlet();
            server.createContext("/api/orders", orderServlet);

            // Static File Handler (Frontend)
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            System.out.println("🌐 Web Server started at http://localhost:" + PORT);

        } catch (IOException e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Web Server stopped.");
        }
    }

    // Static File Handler implementation
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/"))
                path = "/index.html";

            // Security check: prevent directory traversal
            if (path.contains("..")) {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
                return;
            }

            Path filePath = Paths.get("webapp", path);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(path);
                byte[] bytes = Files.readAllBytes(filePath);

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                sendResponse(exchange, 404, "404 Not Found", "text/plain");
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html"))
                return "text/html";
            if (path.endsWith(".css"))
                return "text/css";
            if (path.endsWith(".js"))
                return "application/javascript";
            if (path.endsWith(".png"))
                return "image/png";
            if (path.endsWith(".jpg"))
                return "image/jpeg";
            return "application/octet-stream";
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType)
                throws IOException {
            byte[] bytes = response.getBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
