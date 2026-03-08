package buyerking;

import buyerking.web.OrderServlet;
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

            // Apply shortcuts mapping
            String lowerKeyword = keyword.toLowerCase();
            if (lowerKeyword.contains("something sweet") || lowerKeyword.contains("manisan")
                    || lowerKeyword.contains("sweet") || lowerKeyword.contains("snack")
                    || lowerKeyword.contains("cemilan")) {
                keyword = "Snacks";
            } else if (lowerKeyword.contains("minuman") || lowerKeyword.contains("drink")
                    || lowerKeyword.contains("beverage")) {
                keyword = "Beverages";
            } else if (lowerKeyword.contains("spicy") || lowerKeyword.contains("pedas")
                    || lowerKeyword.contains("nasi padang")) {
                keyword = "Nasi Padang";
            } else if (lowerKeyword.contains("makanan") || lowerKeyword.matches(".*\\bfood\\b.*")) {
                keyword = "Food";
            } else if (lowerKeyword.contains("rog") || lowerKeyword.contains("gaming")
                    || lowerKeyword.contains("laptop")) {
                keyword = "ASUS";
            } else if (lowerKeyword.contains("apple") || lowerKeyword.contains("mac")) {
                keyword = "MacBook";
            } else if (lowerKeyword.contains("phone") || lowerKeyword.contains("hp")) {
                keyword = "iPhone";
            } else if (lowerKeyword.contains("coffee") || lowerKeyword.contains("kopi")
                    || lowerKeyword.contains("caffeine") || lowerKeyword.contains("ngopi")) {
                keyword = "Latte"; // Maps to Iced Latte or Green Tea Latte, or we can just use "Coffee" which
                                   // might not match exact, but "Cappuccino" or "Macchiato" works. Let's use
                                   // "Cappuccino".
                keyword = "Cappuccino"; // Better match
            } else if (lowerKeyword.contains("tea") || lowerKeyword.contains("teh")
                    || lowerKeyword.contains("ngeteh")) {
                keyword = "Tea";
            } else if (lowerKeyword.contains("juice") || lowerKeyword.contains("jus")
                    || lowerKeyword.contains("segar")) {
                keyword = "Juice";
            } else if (lowerKeyword.contains("cake") || lowerKeyword.contains("kue")
                    || lowerKeyword.contains("dessert")) {
                keyword = "Cake";
            } else if (lowerKeyword.contains("biskuit") || lowerKeyword.contains("cookie")) {
                keyword = "Cookies";
            } else if (lowerKeyword.contains("chicken") || lowerKeyword.contains("ayam")) {
                keyword = "Chicken";
            } else if (lowerKeyword.contains("beef") || lowerKeyword.contains("daging")) {
                keyword = "Beef";
            } else {
                // Determine if it matches an exact category to support "berlakukan ke semua
                // menu"
                try (Connection conn = DatabaseManager.getConnection();
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt
                                .executeQuery("SELECT DISTINCT category FROM products WHERE category IS NOT NULL")) {
                    while (rs.next()) {
                        String cat = rs.getString("category");
                        if (lowerKeyword.contains(cat.toLowerCase())) {
                            keyword = cat;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Ignore, fallback to keyword search
                }
            }

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
        } else if (input.startsWith("DELETE_ORDER:")) {
            // Handle order history deletion
            return deleteOrderHistory(input.substring(13).trim());
        } else if (command.contains("show items in") || command.contains("show items for")) {
            // Handle category selection from menu chips
            String keyword = input.replaceFirst("(?i)(show items in|show items for)\\s+", "").trim();
            return searchAndRecommend(keyword, "best");
        } else if (command.contains("history") || command.contains("my order") || command.contains("my payment")) {
            return showOrderHistory();
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
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM products WHERE category IS NOT NULL")) {

            boolean first = true;
            while (rs.next()) {
                if (!first)
                    sb.append("|");
                sb.append(rs.getString("category"));
                first = false;
            }
        } catch (Exception e) {
            return "Error loading menu: " + e.getMessage();
        }
        return sb.toString();
    }

    private String searchAndRecommend(String keyword, String sortBy) {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DatabaseManager.getConnection()) {

            String orderBy = "s.rating DESC"; // Default best
            if (sortBy.equals("price")) {
                orderBy = "p.base_price ASC";
            } else if (sortBy.equals("speed")) {
                orderBy = "s.speed_rating ASC";
            }

            String sql = "SELECT p.product_id, p.product_name, p.base_price, p.description, s.seller_name, s.category, s.rating, s.speed_rating "
                    +
                    "FROM products p " +
                    "JOIN sellers s ON p.seller_id = s.seller_id " +
                    "WHERE p.product_name LIKE ? OR p.description LIKE ? OR p.category LIKE ? " +
                    "ORDER BY " + orderBy + " LIMIT 10";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String searchPattern = "%" + keyword + "%";
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean found = false;

                    while (rs.next()) {
                        if (!found) { // Append header only once if results are found
                            sb.append("Here are the best offers I found for **" + keyword
                                    + "**. Select the ones you want to order:\n");
                        }
                        found = true;
                        // Format: $$SELECTABLE$$:ID|Name|Price|Seller|Rating|Speed|Category
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
                        sb.append(speedStr + "|");

                        sb.append(rs.getString("category"));

                        sb.append("\n");
                    }

                    if (!found) {
                        return "Sorry, I couldn't find any " + keyword + " at the moment.";
                    }
                }
            }
        } catch (Exception e) {
            // Fallback for demo when DB is not running
            sb.append("*(Offline Mode)* Here are simulated offers for **" + keyword + "**:\n");

            sb.append("$$SELECTABLE$$:991|Demo Product 1|Rp150,000|Mock Seller A|4.8|30m|Food\n");
            sb.append("$$SELECTABLE$$:992|Demo Product 2|Rp75,000|Mock Seller B|4.5|45m|Food\n");
            sb.append("$$SELECTABLE$$:993|Premium Demo Item|Rp1,200,000|Tech Store|4.9|1h|Electronics\n");
        }

        return sb.toString();
    }

    private String showOrderHistory() {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT order_id, items_json, total_price, status, created_at, note FROM orders WHERE buyer_name = 'Guest' ORDER BY created_at DESC LIMIT 5")) {

            boolean found = false;
            sb.append("📜 **Your Recent Orders & Payment History:**\n\n");

            while (rs.next()) {
                found = true;
                int id = rs.getInt("order_id");
                double total = rs.getDouble("total_price");
                String status = rs.getString("status");
                String note = rs.getString("note");
                String date = rs.getString("created_at");

                sb.append(String.format("- **Order #%d** (%s): Rp%,.0f [%s]\n", id, date, total, status.toUpperCase()));
                sb.append(String.format(
                        "  <button onclick=\"sendMessage('DELETE_ORDER:%d')\" style=\"padding:4px 8px; font-size:0.8rem; background:#dc3545; color:white; border:none; border-radius:4px; cursor:pointer;\">🗑️ Clear Record</button>\n",
                        id));
                if (note != null && !note.trim().isEmpty()) {
                    sb.append(String.format("  *Note: %s*\n", note));
                }

                // Try parsing items
                String itemsJson = rs.getString("items_json");
                if (itemsJson != null && itemsJson.length() > 2) {
                    try {
                        int pos = 0;
                        while (pos < itemsJson.length()) {
                            int start = itemsJson.indexOf("{", pos);
                            if (start == -1)
                                break;
                            int end = itemsJson.indexOf("}", start);
                            if (end == -1)
                                break;
                            String itemObj = itemsJson.substring(start + 1, end);

                            // Naive parse name and price
                            String n = extractJsonField(itemObj, "\"name\":");
                            String p = extractJsonField(itemObj, "\"price\":");
                            if (!n.isEmpty() && !p.isEmpty()) {
                                sb.append(String.format("  - %s (%s)\n", n, p));
                            }
                            pos = end + 1;
                        }
                    } catch (Exception ignored) {
                    }
                }
                sb.append("\n");
            }

            if (!found) {
                return "You don't have any past orders yet.";
            }
        } catch (Exception e) {
            return "Error loading history: " + e.getMessage();
        }
        return sb.toString();
    }

    private String deleteOrderHistory(String orderIdStr) {
        try {
            int orderId = Integer.parseInt(orderIdStr);
            try (Connection conn = DatabaseManager.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM orders WHERE order_id = ? AND buyer_name = 'Guest'")) {
                ps.setInt(1, orderId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    return "✅ Successfully deleted Order #" + orderId
                            + " from your history.\nType 'history' to view your updated records.";
                } else {
                    return "⚠️ Could not find Order #" + orderId + " or it does not belong to you.";
                }
            }
        } catch (NumberFormatException e) {
            return "❌ Invalid order ID provided.";
        } catch (Exception e) {
            return "❌ Error trying to delete order: " + e.getMessage();
        }
    }

    private String processOrder(String orderJson) {
        try {
            double lat = 0.0;
            double lng = 0.0;
            String address = "";
            if (orderJson.contains("|LNG:")) {
                int lngIdx = orderJson.indexOf("|LNG:");
                String lngStr = orderJson.substring(lngIdx + 5).trim();
                try {
                    lng = Double.parseDouble(lngStr);
                } catch (Exception ignored) {
                }
                orderJson = orderJson.substring(0, lngIdx);
            }
            if (orderJson.contains("|LAT:")) {
                int latIdx = orderJson.indexOf("|LAT:");
                String latStr = orderJson.substring(latIdx + 5).trim();
                try {
                    lat = Double.parseDouble(latStr);
                } catch (Exception ignored) {
                }
                orderJson = orderJson.substring(0, latIdx);
            }
            if (orderJson.contains("|ADDRESS:")) {
                int addrIdx = orderJson.indexOf("|ADDRESS:");
                address = orderJson.substring(addrIdx + 9).trim();
                orderJson = orderJson.substring(0, addrIdx);
            }
            String note = "";
            if (orderJson.contains("|NOTE:")) {
                int noteIdx = orderJson.indexOf("|NOTE:");
                note = orderJson.substring(noteIdx + 6).trim();
                orderJson = orderJson.substring(0, noteIdx);
            }

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

                // Extract name
                String name = "";
                int nameIdx = item.indexOf("\"name\":");
                if (nameIdx != -1) {
                    int nameStart = item.indexOf("\"", nameIdx + 7) + 1;
                    int nameEnd = item.indexOf("\"", nameStart);
                    if (nameEnd > nameStart) {
                        name = item.substring(nameStart, nameEnd);
                    }
                }

                // Extract price
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
            if (!note.isEmpty()) {
                response.append(String.format("**Note:** %s\n\n", note));
            }

            // ── Persist the order to the database ────────────────────────────
            int orderId = OrderServlet.saveOrder("Guest", orderJson, total, note, address, lat, lng);
            if (orderId > 0) {
                response.append("✅ Your order has been sent to the seller! Order #" + orderId + "\n");
                response.append("The seller will accept or reject it shortly. Check back soon!");
            } else {
                response.append("✅ Your order has been recorded! Thank you for shopping with BuyerKing.");
            }

            return response.toString();

        } catch (Exception e) {
            return "Error processing order: " + e.getMessage() + ". Please try again.";
        }
    }

    private String extractJsonField(String item, String key) {
        int idx = item.indexOf(key);
        if (idx != -1) {
            int valStart = item.indexOf("\"", idx + key.length()) + 1;
            int valEnd = item.indexOf("\"", valStart);
            if (valEnd > valStart) {
                return item.substring(valStart, valEnd);
            }
        }
        return "";
    }
}