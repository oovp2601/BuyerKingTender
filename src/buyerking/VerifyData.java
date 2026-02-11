package buyerking;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class VerifyData {

    public static void main(String[] args) {
        System.out.println("Verifying database data...\n");

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();

            // Check sellers
            System.out.println("=== SELLERS ===");
            ResultSet rs = stmt.executeQuery("SELECT seller_id, seller_name, category FROM sellers");
            int sellerCount = 0;
            while (rs.next()) {
                System.out.println(rs.getInt("seller_id") + ". " +
                        rs.getString("seller_name") + " (" + rs.getString("category") + ")");
                sellerCount++;
            }
            System.out.println("Total sellers: " + sellerCount + "\n");

            // Check products
            System.out.println("=== PRODUCTS ===");
            rs = stmt.executeQuery("SELECT product_id, product_name, category, base_price FROM products LIMIT 10");
            int productCount = 0;
            while (rs.next()) {
                System.out.println(rs.getInt("product_id") + ". " +
                        rs.getString("product_name") + " (" + rs.getString("category") + ") - Rp" +
                        rs.getDouble("base_price"));
                productCount++;
            }

            // Get total count
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM products");
            if (rs.next()) {
                System.out.println("Total products: " + rs.getInt("total") + "\n");
            }

            // Check categories
            System.out.println("=== CATEGORIES ===");
            rs = stmt.executeQuery("SELECT DISTINCT category FROM products WHERE category IS NOT NULL");
            while (rs.next()) {
                String cat = rs.getString("category");
                System.out.println("- " + cat);
            }

            stmt.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
