package buyerking;

import java.sql.Connection;
import java.sql.Statement;

public class AddDrinksAndCookies {

    public static void main(String[] args) {
        System.out.println("Adding drinks and cookies to database...\n");

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();

            // Add more sellers for drinks and snacks
            System.out.println("Adding new sellers...");
            stmt.executeUpdate("INSERT INTO sellers (seller_name, category, rating, speed_rating) VALUES " +
                    "('Fresh Juice Bar', 'Beverages', 4.7, 10), " +
                    "('Coffee Corner', 'Beverages', 4.9, 15), " +
                    "('Tea House Premium', 'Beverages', 4.6, 12), " +
                    "('Sweet Treats Bakery', 'Snacks', 4.8, 20), " +
                    "('Cookie Monster Shop', 'Snacks', 4.9, 25)");

            // Add drinks with descriptive keywords
            System.out.println("Adding drinks...");
            stmt.executeUpdate(
                    "INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES " +
                    // Refreshing drinks
                            "('Fresh Orange Juice', 'Beverages', 'Refreshing cold-pressed orange juice, sweet and tangy', 15000, 11), "
                            +
                            "('Lemon Iced Tea', 'Beverages', 'Refreshing iced tea with fresh lemon, perfect for hot days', 12000, 13), "
                            +
                            "('Watermelon Juice', 'Beverages', 'Sweet refreshing watermelon juice, naturally hydrating', 14000, 11), "
                            +
                            "('Coconut Water', 'Beverages', 'Fresh young coconut water, naturally refreshing and hydrating', 10000, 11), "
                            +
                            "('Mint Lemonade', 'Beverages', 'Refreshing lemonade with fresh mint leaves, sweet and cooling', 13000, 11), "
                            +

                            // Coffee drinks
                            "('Cappuccino', 'Beverages', 'Rich espresso with steamed milk foam, smooth and creamy', 25000, 12), "
                            +
                            "('Iced Latte', 'Beverages', 'Cold espresso with milk over ice, smooth and refreshing', 28000, 12), "
                            +
                            "('Caramel Macchiato', 'Beverages', 'Sweet espresso drink with caramel, creamy and indulgent', 32000, 12), "
                            +
                            "('Mocha', 'Beverages', 'Chocolate espresso drink, sweet and rich', 30000, 12), " +

                            // Tea drinks
                            "('Green Tea Latte', 'Beverages', 'Smooth green tea with milk, healthy and refreshing', 22000, 13), "
                            +
                            "('Thai Tea', 'Beverages', 'Sweet creamy Thai tea, rich and indulgent', 18000, 13), " +
                            "('Jasmine Tea', 'Beverages', 'Fragrant jasmine tea, light and refreshing', 15000, 13), " +

                            // Smoothies
                            "('Mango Smoothie', 'Beverages', 'Sweet tropical mango smoothie, thick and refreshing', 20000, 11), "
                            +
                            "('Strawberry Banana Smoothie', 'Beverages', 'Sweet fruity smoothie, creamy and delicious', 22000, 11)");

            // Add cookies and snacks with descriptive keywords
            System.out.println("Adding cookies and snacks...");
            stmt.executeUpdate(
                    "INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES " +
                    // Sweet cookies
                            "('Chocolate Chip Cookies', 'Snacks', 'Classic sweet cookies with chocolate chips, crunchy and delicious (6 pcs)', 25000, 14), "
                            +
                            "('Double Chocolate Cookies', 'Snacks', 'Rich chocolate cookies, sweet and indulgent (6 pcs)', 28000, 15), "
                            +
                            "('Oatmeal Raisin Cookies', 'Snacks', 'Healthy sweet cookies with oats and raisins (6 pcs)', 22000, 14), "
                            +
                            "('Sugar Cookies', 'Snacks', 'Simple sweet butter cookies, classic and delicious (8 pcs)', 20000, 15), "
                            +
                            "('Peanut Butter Cookies', 'Snacks', 'Sweet cookies with peanut butter, rich and satisfying (6 pcs)', 24000, 15), "
                            +

                            // Other sweet snacks
                            "('Brownies', 'Snacks', 'Fudgy chocolate brownies, sweet and rich (4 pcs)', 30000, 14), " +
                            "('Red Velvet Cake Slice', 'Snacks', 'Sweet red velvet cake with cream cheese frosting, indulgent', 35000, 14), "
                            +
                            "('Cheesecake Slice', 'Snacks', 'Creamy sweet cheesecake, rich and smooth', 38000, 14), " +
                            "('Donut Assorted', 'Snacks', 'Sweet glazed donuts, variety pack (4 pcs)', 28000, 15), " +
                            "('Croissant', 'Snacks', 'Buttery flaky croissant, light and delicious', 18000, 14)");

            System.out.println("✅ Successfully added drinks and cookies!");
            System.out.println("Added 14 beverages and 10 snacks");

            stmt.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
