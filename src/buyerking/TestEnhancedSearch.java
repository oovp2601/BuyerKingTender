package buyerking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestEnhancedSearch {

    public static void main(String[] args) {
        System.out.println("Testing enhanced search with descriptive queries...\n");

        String[] testQueries = {
                "something sweet",
                "drink something refreshing",
                "mie",
                "cookies",
                "coffee"
        };

        try {
            Connection conn = DatabaseManager.getConnection();

            for (String keyword : testQueries) {
                System.out.println("=== Query: \"" + keyword + "\" ===");

                String sql = "SELECT p.product_id, p.product_name, p.category, p.base_price " +
                        "FROM products p " +
                        "JOIN sellers s ON p.seller_id = s.seller_id " +
                        "WHERE p.product_name LIKE ? OR p.description LIKE ? OR p.category LIKE ? " +
                        "ORDER BY s.rating DESC LIMIT 5";

                PreparedStatement pstmt = conn.prepareStatement(sql);
                String searchPattern = "%" + keyword + "%";
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                ResultSet rs = pstmt.executeQuery();

                int count = 0;
                while (rs.next()) {
                    System.out.println("  ✓ " + rs.getString("product_name") +
                            " (" + rs.getString("category") + ") - Rp" +
                            String.format("%,.0f", rs.getDouble("base_price")));
                    count++;
                }

                if (count == 0) {
                    System.out.println("  ✗ NO RESULTS");
                }
                System.out.println("  Total: " + count + " results\n");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
