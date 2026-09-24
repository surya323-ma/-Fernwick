-- Fernwick nursery database schema + seed data.
-- Executed automatically on startup by com.nursery.DBInitializer.
-- Safe to run repeatedly: tables use IF NOT EXISTS and products use INSERT IGNORE.

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    latin_name VARCHAR(160),
    price DECIMAL(10,2) NOT NULL,
    was_price DECIMAL(10,2) NULL,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(500),
    badge VARCHAR(30),
    light VARCHAR(60),
    water VARCHAR(60),
    category VARCHAR(160),
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS cart (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id VARCHAR(40) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    UNIQUE KEY uniq_user_product (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Pending',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

INSERT IGNORE INTO products (id, name, latin_name, price, was_price, stock, image, badge, light, water, category, sort_order) VALUES
('monstera', 'Monstera Deliciosa', 'Monstera deliciosa', 42.00, 52.00, 24, 'https://images.unsplash.com/photo-1545241047-6083a3684587?w=700&q=72&auto=format&fit=crop', 'Sale', 'Bright indirect', 'Weekly', 'light-bright easy-care size-large sale', 0),
('calathea', 'Rattlesnake Calathea', 'Goeppertia insignis', 34.00, NULL, 18, 'https://images.unsplash.com/photo-1602923668104-8f9e03e77e62?w=700&q=72&auto=format&fit=crop', 'Pet safe', 'Low to medium', 'Twice weekly', 'light-low pet-safe size-medium', 1),
('bonsai', 'Ginseng Ficus Bonsai', 'Ficus microcarpa', 58.00, NULL, 4, 'https://images.unsplash.com/photo-1512428813834-c702c7702b78?w=700&q=72&auto=format&fit=crop', 'Low stock', 'Bright indirect', 'Twice weekly', 'light-bright size-small', 2),
('haworthia', 'Zebra Haworthia', 'Haworthiopsis attenuata', 18.00, NULL, 32, 'https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=700&q=72&auto=format&fit=crop', 'Pet safe', 'Bright direct', 'Fortnightly', 'light-bright easy-care pet-safe size-small', 3),
('moon-cactus', 'Moon Cactus', 'Gymnocalycium mihanovichii', 14.00, NULL, 27, 'https://images.unsplash.com/photo-1509937528035-ad76254b0356?w=700&q=72&auto=format&fit=crop', 'New in', 'Bright direct', 'Monthly', 'light-bright easy-care size-small', 4),
('echeveria', 'Echeveria in Concrete', 'Echeveria elegans', 22.00, NULL, 21, 'https://images.unsplash.com/photo-1483794344563-d27a8d18014e?w=700&q=72&auto=format&fit=crop', NULL, 'Bright direct', 'Fortnightly', 'light-bright easy-care pet-safe size-small', 5),
('bamboo', 'Lucky Bamboo', 'Dracaena sanderiana', 16.00, 20.00, 29, 'https://images.unsplash.com/photo-1567331711402-509c12c41959?w=700&q=72&auto=format&fit=crop', 'Sale', 'Low light', 'Weekly', 'light-low easy-care size-medium sale', 6),
('topiary', 'Boxwood Topiary', 'Buxus sempervirens', 46.00, NULL, 11, 'https://images.unsplash.com/photo-1520412099551-62b6bafeb5bb?w=700&q=72&auto=format&fit=crop', NULL, 'Bright indirect', 'Weekly', 'light-bright size-large', 7),
('succulent-trio', 'Succulent Trio', 'Mixed species', 26.00, NULL, 19, 'https://images.unsplash.com/photo-1459156212016-c812468e2115?w=700&q=72&auto=format&fit=crop', NULL, 'Bright direct', 'Fortnightly', 'light-bright easy-care pet-safe size-small', 8),
('eucalyptus', 'Eucalyptus Stems', 'Eucalyptus cinerea', 12.00, NULL, 6, 'https://images.unsplash.com/photo-1466781783364-36c955e42a7f?w=700&q=72&auto=format&fit=crop', 'Low stock', 'Bright indirect', 'Change water weekly', 'light-bright easy-care size-medium', 9);
