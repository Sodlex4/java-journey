-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    pin VARCHAR(60) DEFAULT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    failed_attempts INT DEFAULT 0,
    locked BOOLEAN DEFAULT FALSE,
    lock_until BIGINT DEFAULT NULL,
    lock_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    tx_id VARCHAR(20) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    from_user VARCHAR(50),
    to_user VARCHAR(50),
    user_id INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create index on user_id for efficient transaction lookups
CREATE INDEX idx_transactions_user_id ON transactions(user_id);

-- Create system_accounts table
CREATE TABLE IF NOT EXISTS system_accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_name VARCHAR(50) UNIQUE NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00
);

-- Create savings table
CREATE TABLE IF NOT EXISTS savings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    interest_rate DECIMAL(5,2) DEFAULT 4.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_interest_calc TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create bill_splits table
CREATE TABLE IF NOT EXISTS bill_splits (
    id INT PRIMARY KEY AUTO_INCREMENT,
    creator_id INT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    split_count INT NOT NULL,
    title VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id)
);

-- Create bill_split_participants table
CREATE TABLE IF NOT EXISTS bill_split_participants (
    id INT PRIMARY KEY AUTO_INCREMENT,
    split_id INT NOT NULL,
    user_id INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (split_id) REFERENCES bill_splits(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
