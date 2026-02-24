-- Database: buyerking_v2
CREATE DATABASE IF NOT EXISTS buyerking_v2;
USE buyerking_v2;

-- Tabel Penjual (Sellers)
CREATE TABLE sellers (
    seller_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_name VARCHAR(100) NOT NULL,
    category VARCHAR(50), -- e.g., Food, Electronics
    rating DECIMAL(3,2) DEFAULT 0.0,
    speed_rating INT DEFAULT 30, -- Avg delivery time in mins/hours
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Produk/Layanan (Menu)
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    base_price DECIMAL(12,2),
    seller_id INT,
    FOREIGN KEY (seller_id) REFERENCES sellers(seller_id)
);

-- Tabel Pembeli (Buyers)
CREATE TABLE buyers (
    buyer_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Tender/Order
CREATE TABLE tenders (
    tender_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    quantity INT DEFAULT 1,
    max_price DECIMAL(12,2),
    status ENUM('open', 'closed', 'awarded') DEFAULT 'open',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (buyer_id) REFERENCES buyers(buyer_id)
);

-- Data Seeding: Sellers
-- Data Seeding: Sellers
INSERT INTO sellers (seller_name, category, rating, speed_rating) VALUES
('Minang Authentic Resto', 'Food', 4.8, 25),
('Budget Padang Meals', 'Food', 4.2, 45),
('Salero Bundo Express', 'Food', 4.9, 30),
('Bahari Warteg', 'Food', 4.5, 20),
('Glodok Electronics', 'Electronics', 4.7, 1440), -- 1 day
('Official iStore', 'Electronics', 5.0, 2880); -- 2 days

-- Data Seeding: Products (Menu)
INSERT INTO products (product_name, category, description, base_price, seller_id) VALUES
-- Nasi Padang Variants
('Nasi Padang (Beef Rendang)', 'Food', 'Rice with tender beef rendang, jackfruit curry, green chili', 25000, 1),
('Nasi Padang (Budget Rendang)', 'Food', 'Budget meal with beef rendang', 18000, 2),
('Nasi Padang (Special Rendang)', 'Food', 'Premium beef rendang, thick gulai sauce', 28000, 3),
('Nasi Padang (Grilled Chicken)', 'Food', 'Padang style grilled chicken with rice', 22000, 1),
('Nasi Padang (Fried Chicken)', 'Food', 'Fried chicken with galangal crumbs + veggies', 15000, 4),
('Nasi Padang (Omelette)', 'Food', 'Thick Padang style omelette with rice', 12000, 2),

-- Electronics
('ASUS Gaming Laptop', 'Electronics', 'ASUS TUF Gaming, RTX 3050, 8GB RAM', 12000000, 5),
('MacBook Air M1', 'Electronics', 'Apple M1 Chip, 256GB SSD', 11500000, 6),
('iPhone 13', 'Electronics', '128GB, Midnight Blue', 10000000, 6),
('Samsung Galaxy S23', 'Electronics', 'Android Flagship', 11000000, 5);

-- Sample Buyer
INSERT INTO buyers (buyer_name, email, address) VALUES
('Rangga', 'rangga@email.com', 'South Jakarta');

-- Orders Table (created by buyers via chat, managed by sellers)
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_name VARCHAR(100) DEFAULT 'Guest',
    items_json TEXT NOT NULL,
    total_price DECIMAL(12,2) DEFAULT 0.00,
    status ENUM('pending','accepted','rejected') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);