package buyerking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestSearch {

    public static void main(String[] args) {
        System.out.println("Testing search queries...\n");

        String[] testKeywords = {
                "Food",
                "Electronics",
                "Show items in Food",
                "Show items in Electronics",
                "Nasi Padang",
                "Laptop"
        };

        try {
            Connection conn = DatabaseManager.getConnection();

            for (String keyword : testKeywords) {
                System.out.println("=== Searching for: \"" + keyword + "\" ===");

                String sql = "SELECT p.product_id, p.product_name, p.base_price, p.category " +
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
                    System.out.println("  - " + rs.getString("product_name") +
                            " (" + rs.getString("category") + ") - Rp" + rs.getDouble("base_price"));
                    count++;
                }

                if (count == 0) {
                    System.out.println("  NO RESULTS FOUND!");
                }
                System.out.println("  Total: " + count + " results\n");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
