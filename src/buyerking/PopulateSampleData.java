package buyerking;

import java.sql.Connection;
import java.sql.Statement;

public class PopulateSampleData {

    public static void main(String[] args) {
        System.out.println("Populating sample data...");

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();

            // Clear existing data (optional)
            System.out.println("Clearing existing data...");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM sellers");

            // Reset auto-increment counters (SQLite specific)
            try {
                stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='sellers'");
                stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='products'");
            } catch (Exception ignore) {
                // sqlite_sequence might not exist yet, ignore
            }

            // Insert Sellers
            System.out.println("Inserting sellers...");
            stmt.executeUpdate("INSERT INTO sellers (seller_name, category, rating, speed_rating) VALUES " +
                    "('Minang Authentic Resto', 'Food', 4.8, 25), " +
                    "('Budget Padang Meals', 'Food', 4.2, 45), " +
                    "('Salero Bundo Express', 'Food', 4.9, 30), " +
                    "('Bahari Warteg', 'Food', 4.5, 20), " +
                    "('Glodok Electronics', 'Electronics', 4.7, 1440), " +
                    "('Official iStore', 'Electronics', 5.0, 2880), " +
                    "('TechMart Indonesia', 'Electronics', 4.6, 720), " +
                    "('Gadget Galaxy', 'Electronics', 4.8, 1200), " +
                    "('Warung Makan Sederhana', 'Food', 4.3, 15), " +
                    "('Premium Food Court', 'Food', 4.9, 35), " +
                    "('Sweet Treats Bakery', 'Snacks', 4.8, 15), " +
                    "('Snack Corner', 'Snacks', 4.5, 10), " +
                    "('Fresh Juice & Boba', 'Beverages', 4.7, 5), " +
                    "('Kopi Premium Cafe', 'Beverages', 4.9, 8)");

            // Insert Food Products
            System.out.println("Inserting food products...");
            stmt.executeUpdate(
                    "INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES " +
                    // Nasi Padang Variants
                            "('Nasi Padang (Beef Rendang)', 'Food', 'Rice with tender beef rendang, jackfruit curry, green chili', 25000, 1), "
                            +
                            "('Nasi Padang (Budget Rendang)', 'Food', 'Budget meal with beef rendang', 18000, 2), " +
                            "('Nasi Padang (Special Rendang)', 'Food', 'Premium beef rendang, thick gulai sauce', 28000, 3), "
                            +
                            "('Nasi Padang (Grilled Chicken)', 'Food', 'Padang style grilled chicken with rice', 22000, 1), "
                            +
                            "('Nasi Padang (Fried Chicken)', 'Food', 'Fried chicken with galangal crumbs + veggies', 15000, 4), "
                            +
                            "('Nasi Padang (Omelette)', 'Food', 'Thick Padang style omelette with rice', 12000, 2), " +
                            // More Food Items
                            "('Nasi Goreng Special', 'Food', 'Indonesian fried rice with chicken, egg, and vegetables', 20000, 9), "
                            +
                            "('Mie Goreng', 'Food', 'Stir-fried noodles with vegetables and chicken', 18000, 9), " +
                            "('Soto Ayam', 'Food', 'Traditional chicken soup with rice and herbs', 15000, 4), " +
                            "('Gado-Gado', 'Food', 'Mixed vegetables with peanut sauce', 16000, 9), " +
                            "('Sate Ayam', 'Food', 'Grilled chicken skewers with peanut sauce (10 pcs)', 25000, 10), " +
                            "('Bakso Special', 'Food', 'Meatball soup with noodles and vegetables', 17000, 4), " +
                            "('Ayam Geprek', 'Food', 'Smashed fried chicken with sambal', 20000, 10), " +
                            "('Nasi Uduk', 'Food', 'Coconut rice with fried chicken and side dishes', 22000, 10)");

            // Insert Electronics Products
            System.out.println("Inserting electronics products...");
            stmt.executeUpdate(
                    "INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES " +
                    // Laptops
                            "('ASUS Gaming Laptop', 'Electronics', 'ASUS TUF Gaming, RTX 3050, 8GB RAM, 512GB SSD', 12000000, 5), "
                            +
                            "('MacBook Air M1', 'Electronics', 'Apple M1 Chip, 8GB RAM, 256GB SSD', 11500000, 6), " +
                            "('Lenovo ThinkPad', 'Electronics', 'Intel i5, 16GB RAM, 512GB SSD, Business Laptop', 9500000, 7), "
                            +
                            "('HP Pavilion', 'Electronics', 'AMD Ryzen 5, 8GB RAM, 512GB SSD', 8000000, 5), " +
                            // Smartphones
                            "('iPhone 13', 'Electronics', '128GB, Midnight Blue, 1 Year Warranty', 10000000, 6), " +
                            "('iPhone 14', 'Electronics', '256GB, Starlight, Official Warranty', 13500000, 6), " +
                            "('Samsung Galaxy S23', 'Electronics', '256GB, Android Flagship, AMOLED Display', 11000000, 5), "
                            +
                            "('Samsung Galaxy A54', 'Electronics', '128GB, Mid-range with great camera', 5500000, 8), "
                            +
                            "('Xiaomi 13 Pro', 'Electronics', '256GB, Snapdragon 8 Gen 2, Leica Camera', 9000000, 7), "
                            +
                            "('OPPO Reno 10', 'Electronics', '256GB, 5G, Fast Charging', 6500000, 8), " +
                            // Tablets
                            "('iPad Air', 'Electronics', '64GB, WiFi, 10.9 inch display', 8500000, 6), " +
                            "('Samsung Galaxy Tab S8', 'Electronics', '128GB, Android tablet with S-Pen', 7000000, 8), "
                            +
                            // Accessories
                            "('AirPods Pro', 'Electronics', 'Active Noise Cancellation, Wireless Charging', 3500000, 6), "
                            +
                            "('Samsung Galaxy Buds 2', 'Electronics', 'True wireless earbuds with ANC', 1800000, 5), " +
                            "('Logitech MX Master 3', 'Electronics', 'Wireless mouse for productivity', 1500000, 7), " +
                            "('Mechanical Keyboard RGB', 'Electronics', 'Gaming keyboard with RGB lighting', 1200000, 8)");

            // Insert Snacks Products
            System.out.println("Inserting snacks products...");
            stmt.executeUpdate(
                    "INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES " +
                            "('Chocolate Chip Cookies', 'Snacks', 'Freshly baked soft cookies (1 Dozen)', 45000, 11), "
                            +
                            "('Brownies Fudgy', 'Snacks', 'Rich and fudgy chocolate brownies', 55000, 11), " +
                            "('Keripik Singkong Pedas', 'Snacks', 'Spicy cassava chips, crunchy and addictive', 15000, 12), "
                            +
                            "('Pisang Goreng Keju', 'Snacks', 'Fried banana with cheese and chocolate', 20000, 12), " +
                            "('Martabak Manis', 'Snacks', 'Sweet thick pancake with chocolate and peanuts', 40000, 12)");

            // Insert Beverages Products
            System.out.println("Inserting beverages products...");
            stmt.executeUpdate(
                    "INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES " +
                            "('Iced Latte', 'Beverages', 'Cold espresso with fresh milk', 25000, 14), " +
                            "('Cappuccino', 'Beverages', 'Hot espresso with steamed milk foam', 28000, 14), " +
                            "('Mango Juice', 'Beverages', 'Freshly squeezed mango juice', 18000, 13), " +
                            "('Brown Sugar Boba', 'Beverages', 'Milk tea with brown sugar and boba pearls', 22000, 13), "
                            +
                            "('Sweet Iced Tea', 'Beverages', 'Classic refreshing sweet iced tea', 10000, 13)");

            System.out.println("✅ Sample data populated successfully!");
            System.out.println("Total sellers: 14");
            System.out.println("Total products: 40 (14 Food + 16 Electronics + 5 Snacks + 5 Beverages)");

            stmt.close();

        } catch (Exception e) {
            System.err.println("Error populating data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
