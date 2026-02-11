package buyerking;

import java.sql.*;

public class BuyerSystem {

    public void createBuyer(String name, String email, String phone, String address) {
        try {
            String sql = "INSERT INTO buyers (buyer_name, email, phone, address) " +
                    "VALUES (?, ?, ?, ?)";
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            pstmt.setString(4, address);
            pstmt.executeUpdate();

            System.out.println("✅ Buyer registered successfully: " + name);

        } catch (SQLException e) {
            System.err.println("Error creating buyer: " + e.getMessage());
        }
    }

    public void showBuyerInfo(int buyerId) {
        try {
            String sql = "SELECT * FROM buyers WHERE buyer_id = ?";
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, buyerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n👤 BUYER INFORMATION");
                System.out.println("Name: " + rs.getString("buyer_name"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Phone: " + rs.getString("phone"));
                System.out.println("Address: " + rs.getString("address"));
                System.out.println("Joined: " + rs.getTimestamp("created_at"));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching buyer info: " + e.getMessage());
        }
    }
}