package buyerking;

import java.sql.*;

public class ShowCategories {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM products ORDER BY category");

            System.out.println("Available categories:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
