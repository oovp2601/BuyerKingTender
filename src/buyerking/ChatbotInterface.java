package buyerking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ChatbotInterface {

    private Scanner scanner;
    private boolean isRunning;

    public ChatbotInterface() {
        this.scanner = new Scanner(System.in);
        this.isRunning = false;
    }

    public void start() {
        this.isRunning = true;
        System.out.println("\n🤖 BuyerKing Chatbot is ready to help!");
        System.out.println("Type 'help' to see available commands.");

        while (isRunning) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                stop();
            } else {
                processCommand(input);
            }
        }
    }

    public void stop() {
        this.isRunning = false;
        System.out.println("Chatbot stopped. Goodbye!");
    }

    public String processCommand(String input) {
        String command = input.toLowerCase().trim();

        if (command.equals("help")) {
            return showHelp();
        } else if (command.equals("menu") || command.equals("list menu") || command.contains("show menu")) {
            return showMenu();
        } else if (command.startsWith("i want") || command.startsWith("i need") || command.startsWith("buy")
                || command.startsWith("cari")) {
            // Extract keyword
            String keyword = input.replaceFirst("(?i)(i want|i need|buy|cari)\\s+", "").trim();
            return searchAndRecommend(keyword, "best");
        } else if (command.contains("cheapest")) {
            String keyword = input.replaceFirst("(?i)(cheapest)\\s*", "").trim();
            if (!keyword.isEmpty()) {
                return searchAndRecommend(keyword, "price");
            }
            return "What item do you want the cheapest option for? Try 'cheapest Nasi Padang'";
        } else if (command.contains("fastest")) {
            String keyword = input.replaceFirst("(?i)(fastest)\\s*", "").trim();
            if (!keyword.isEmpty()) {
                return searchAndRecommend(keyword, "speed");
            }
            return "What item do you need fast? Try 'fastest Laptop'";
        } else if (input.startsWith("ORDER:")) {
            // Handle order submission with item details
            return processOrder(input.substring(6).trim());
        } else if (command.contains("show items in") || command.contains("show items for")) {
            // Handle category selection from menu chips
            String keyword = input.replaceFirst("(?i)(show items in|show items for)\\s+", "").trim();
            return searchAndRecommend(keyword, "best");
        } else {
            // Conversational fallback
            if (command.contains("hello") || command.contains("hi")) {
                return "Hello! I am BuyerKing Assistant. Type 'Menu' to see what's available, or tell me what you need (e.g. 'I want Nasi Padang').";
            }
            // Default search
            return searchAndRecommend(command, "best");
        }
    }

    private String showHelp() {
        return "👋 **How can I help?**\n" +
                "- **Menu**: View categories\n" +
                "- **I want [Item]**: Find sellers for an item\n" +
                "- **Cheapest [Item]**: Find the best price";
    }

    private String showMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("$$MENU$$:");
        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM products WHERE category IS NOT NULL");

            boolean first = true;
            while (rs.next()) {
                if (!first)
                    sb.append("|");
                sb.append(rs.getString("category"));
                first = false;
            }
        } catch (SQLException e) {
            return "Error loading menu: " + e.getMessage();
        }
        return sb.toString();
    }

    private String searchAndRecommend(String keyword, String sortBy) {
        StringBuilder sb = new StringBuilder();
        try {
            Connection conn = DatabaseManager.getConnection();

            String orderBy = "s.rating DESC"; // Default best
            if (sortBy.equals("price")) {
                orderBy = "p.base_price ASC";
            } else if (sortBy.equals("speed")) {
                orderBy = "s.speed_rating ASC";
            }

            String sql = "SELECT p.product_id, p.product_name, p.base_price, p.description, s.seller_name, s.rating, s.speed_rating "
                    +
                    "FROM products p " +
                    "JOIN sellers s ON p.seller_id = s.seller_id " +
                    "WHERE p.product_name LIKE ? OR p.description LIKE ? OR p.category LIKE ? " +
                    "ORDER BY " + orderBy + " LIMIT 10";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            ResultSet rs = pstmt.executeQuery();

            boolean found = false;

            sb.append("Here are the best offers I found for **" + keyword + "**. Select the ones you want to order:\n");

            while (rs.next()) {
                found = true;
                // Format: $$SELECTABLE$$:ID|Name|Price|Seller|Rating|Speed
                sb.append("$$SELECTABLE$$:");
                sb.append(rs.getInt("product_id") + "|");
                sb.append(rs.getString("product_name") + "|");

                // Format Price nicely
                double price = rs.getDouble("base_price");
                sb.append(String.format("Rp%,.0f", price) + "|");

                sb.append(rs.getString("seller_name") + "|");
                sb.append(rs.getDouble("rating") + "|");

                int speed = rs.getInt("speed_rating");
                String speedStr = speed >= 60 ? (speed / 60 + "h") : (speed + "m");
                sb.append(speedStr);

                sb.append("\n");
            }

            if (!found) {
                return "No offers found for '" + keyword + "'. Try checking the **Menu**?";
            }

        } catch (SQLException e) {
            return "Error searching: " + e.getMessage();
        }

        return sb.toString();
    }

    private String processOrder(String orderJson) {
        try {
            StringBuilder response = new StringBuilder();
            response.append("📋 **Order Summary**\n\n");

            double total = 0.0;
            int itemCount = 0;

            // Extract each item by finding {...} patterns
            int pos = 0;
            while (pos < orderJson.length()) {
                int start = orderJson.indexOf("{", pos);
                if (start == -1)
                    break;

                int end = orderJson.indexOf("}", start);
                if (end == -1)
                    break;

                String item = orderJson.substring(start + 1, end);

                // Extract name: find "name":"value"
                String name = "";
                int nameIdx = item.indexOf("\"name\":");
                if (nameIdx != -1) {
                    int nameStart = item.indexOf("\"", nameIdx + 7) + 1;
                    int nameEnd = item.indexOf("\"", nameStart);
                    if (nameEnd > nameStart) {
                        name = item.substring(nameStart, nameEnd);
                    }
                }

                // Extract price: find "price":"value"
                String priceStr = "";
                int priceIdx = item.indexOf("\"price\":");
                if (priceIdx != -1) {
                    int priceStart = item.indexOf("\"", priceIdx + 8) + 1;
                    int priceEnd = item.indexOf("\"", priceStart);
                    if (priceEnd > priceStart) {
                        priceStr = item.substring(priceStart, priceEnd);
                    }
                }

                if (!name.isEmpty() && !priceStr.isEmpty()) {
                    itemCount++;
                    // Parse price (remove "Rp" and commas)
                    String cleanPrice = priceStr.replace("Rp", "").replace(",", "").trim();
                    double price = Double.parseDouble(cleanPrice);
                    total += price;

                    response.append(String.format("%d. %s - %s\n", itemCount, name, priceStr));
                }

                pos = end + 1;
            }

            response.append("\n---\n");
            response.append(String.format("**Total Items:** %d\n", itemCount));
            response.append(String.format("**Total Price:** Rp%,d\n\n", (long) total));
            response.append("✅ Your order has been recorded! Thank you for shopping with BuyerKing.");

            return response.toString();

        } catch (Exception e) {
            return "Error processing order: " + e.getMessage() + ". Please try again.";
        }
    }
}