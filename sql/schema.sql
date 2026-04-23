-- M-PESA Payment System Database Schema

-- Create database
CREATE DATABASE IF NOT EXISTS mpesa_db;
USE mpesa_db;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    tx_id VARCHAR(20) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    from_user VARCHAR(50),
    to_user VARCHAR(50),
    user_id INT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Safaricom (fee collector) account
CREATE TABLE IF NOT EXISTS system_accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(50) UNIQUE NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default system account
INSERT INTO system_accounts (account_name, balance) 
VALUES ('SAFARICOM', 0.00)
ON DUPLICATE KEY UPDATE balance = balance;

-- Insert sample users (commented out - uncomment to use)
-- INSERT INTO users (username, balance) VALUES ('john', 1000.00);
-- INSERT INTO users (username, balance) VALUES ('mary', 500.00);