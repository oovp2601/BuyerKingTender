package buyerking;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TenderManager {

    public int createTender(int buyerId, String title, String description,
            int quantity, double maxPrice, int deadlineDays) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Hitung deadline
            LocalDateTime deadline = LocalDateTime.now().plusDays(deadlineDays);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String deadlineStr = deadline.format(formatter);

            String sql = "INSERT INTO tenders (buyer_id, title, description, quantity, " +
                    "max_price, status, deadline) VALUES (?, ?, ?, ?, ?, 'open', ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, buyerId);
                pstmt.setString(2, title);
                pstmt.setString(3, description);
                pstmt.setInt(4, quantity);
                pstmt.setDouble(5, maxPrice);
                pstmt.setString(6, deadlineStr);
                pstmt.executeUpdate();

                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int tenderId = keys.getInt(1);
                        System.out.println("✅ Tender created successfully! ID: " + tenderId);

                        // Simulate seller bids
                        simulateSellerBids(conn, tenderId);

                        return tenderId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating tender: " + e.getMessage());
        }
        return -1;
    }

    private void simulateSellerBids(Connection conn, int tenderId) {
        try {
            // Get tender info
            String tenderSql = "SELECT * FROM tenders WHERE tender_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(tenderSql)) {
                pstmt.setInt(1, tenderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String title = rs.getString("title");
                        double maxPrice = rs.getDouble("max_price");
                        int quantity = rs.getInt("quantity");

                        // Get random sellers (SQLite RANDOM())
                        String sellerSql = "SELECT seller_id, seller_name FROM sellers ORDER BY RANDOM() LIMIT 3";
                        try (Statement stmt = conn.createStatement();
                                ResultSet sellerRs = stmt.executeQuery(sellerSql)) {

                            int bidCount = 0;
                            while (sellerRs.next()) {
                                int sellerId = sellerRs.getInt("seller_id");
                                String sellerName = sellerRs.getString("seller_name");

                                // Generate bid price (80-100% of maxPrice)
                                double randomFactor = 0.8 + (Math.random() * 0.2);
                                double bidPrice = maxPrice * randomFactor;

                                // Create bid
                                String insertBid = "INSERT INTO bids (tender_id, seller_id, bid_price, notes, " +
                                        "delivery_time) VALUES (?, ?, ?, ?, ?)";
                                try (PreparedStatement bidStmt = conn.prepareStatement(insertBid)) {
                                    bidStmt.setInt(1, tenderId);
                                    bidStmt.setInt(2, sellerId);
                                    bidStmt.setDouble(3, Math.round(bidPrice));
                                    bidStmt.setString(4, "Ready to ship " + quantity + " " + title);
                                    bidStmt.setString(5, (1 + (int) (Math.random() * 3)) + " days");
                                    bidStmt.executeUpdate();
                                }

                                bidCount++;
                                System.out.println("   📨 Bid received from: " + sellerName +
                                        " - Rp" + String.format("%,.0f", bidPrice));
                            }

                            if (bidCount > 0) {
                                System.out.println("✅ " + bidCount + " bids received for tender #" + tenderId);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error simulating bids: " + e.getMessage());
        }
    }

    public void showTenderStats() {
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT status, COUNT(*) as count FROM tenders GROUP BY status")) {

            System.out.println("\n📊 TENDER STATISTICS:");
            while (rs.next()) {
                System.out.println(rs.getString("status") + ": " + rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching stats: " + e.getMessage());
        }
    }
}