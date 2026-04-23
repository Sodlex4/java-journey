package service;

import model.UserAccount;
import model.Transaction;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.*;

public class DatabaseService {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;
    private static final String DRIVER = "org.mariadb.jdbc.Driver";
    private static final String SYSTEM_ACCOUNT = "SAFARICOM";
    private static final String DB_NAME = "mpesa_db";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 500;

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());
    private volatile boolean shutdown = false;

    public DatabaseService() {
        loadEnv();
        initializeDatabase();
    }

    private void loadEnv() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(".env")) {
            props.load(in);
            DB_URL = props.getProperty("DB_URL", "jdbc:mariadb://localhost:3306");
            DB_USER = props.getProperty("DB_USER", "root");
            DB_PASSWORD = props.getProperty("DB_PASSWORD", "sodlex");
        } catch (IOException e) {
            DB_URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mariadb://localhost:3306";
            DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
            DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "sodlex";
        }
    }

    private void initializeDatabase() {
        executeWithRetry(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                stmt.execute("USE " + DB_NAME);

                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "pin VARCHAR(4) DEFAULT '1234', " +
                    "balance DECIMAL(15,2) DEFAULT 0.00, " +
                    "failed_attempts INT DEFAULT 0, " +
                    "locked BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

                stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "tx_id VARCHAR(20) UNIQUE NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "amount DECIMAL(15,2) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "from_user VARCHAR(50), " +
                    "to_user VARCHAR(50), " +
                    "user_id INT, " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

                stmt.execute("CREATE TABLE IF NOT EXISTS system_accounts (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "account_name VARCHAR(50) UNIQUE NOT NULL, " +
                    "balance DECIMAL(15,2) DEFAULT 0.00)");

                stmt.execute("CREATE TABLE IF NOT EXISTS savings (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "user_id INT NOT NULL, " +
                    "balance DECIMAL(15,2) DEFAULT 0.00, " +
                    "interest_rate DECIMAL(5,2) DEFAULT 4.00, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "last_interest_calc TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

                stmt.execute("CREATE TABLE IF NOT EXISTS bill_splits (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "creator_id INT NOT NULL, " +
                    "total_amount DECIMAL(15,2) NOT NULL, " +
                    "split_count INT NOT NULL, " +
                    "title VARCHAR(100), " +
                    "status VARCHAR(20) DEFAULT 'PENDING', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (creator_id) REFERENCES users(id))");

                stmt.execute("CREATE TABLE IF NOT EXISTS bill_split_participants (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "split_id INT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "amount DECIMAL(15,2) NOT NULL, " +
                    "paid BOOLEAN DEFAULT FALSE, " +
                    "FOREIGN KEY (split_id) REFERENCES bill_splits(id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

                stmt.execute("INSERT IGNORE INTO system_accounts (account_name, balance) VALUES ('SAFARICOM', 0.00)");
                stmt.execute("INSERT IGNORE INTO users (username, balance) VALUES ('john', 1000.00)");
                stmt.execute("INSERT IGNORE INTO users (username, balance) VALUES ('mary', 500.00)");
                
                logger.info("Database tables ready");
            }
            return null;
        });
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
            return DriverManager.getConnection(DB_URL + "/" + DB_NAME, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver not found", e);
        }
    }

    public void releaseConnection(Connection conn) {
        if (conn != null && !shutdown) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warning("Error releasing connection: " + e.getMessage());
            }
        }
    }

    public interface Retryable<T> {
        T execute() throws SQLException;
    }

    private <T> T executeWithRetry(Retryable<T> operation) {
        int attempts = 0;
        SQLException lastException = null;
        
        while (attempts < MAX_RETRIES) {
            Connection conn = null;
            try {
                conn = getConnection();
                lastException = null;
                T result = operation.execute();
                if (conn != null) {
                    conn.close();
                }
                return result;
            } catch (SQLException e) {
                lastException = e;
                attempts++;
                logger.warning("Database attempt " + attempts + " failed: " + e.getMessage());
                
                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        logger.warning("Error closing connection: " + e.getMessage());
                    }
                }
            }
        }
        logger.severe("Database operation failed after " + MAX_RETRIES + " attempts");
        throw new RuntimeException("Database operation failed", lastException);
    }

    public <T> T executeInTransaction(Retryable<T> operation) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            T result = operation.execute();
            conn.commit();
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException se) {
                    logger.severe("Rollback failed: " + se.getMessage());
                }
            }
            throw new RuntimeException("Transaction failed", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    releaseConnection(conn);
                } catch (SQLException e) {
                    logger.warning("Failed to release connection: " + e.getMessage());
                }
            }
        }
    }

    public UserAccount getUserById(int userId) {
        String sql = "SELECT id, username, balance FROM users WHERE id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return new UserAccount(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getDouble("balance")
                    );
                }
                return null;
            }
        });
    }

    public List<UserAccount> getAllUsers() {
        List<UserAccount> users = new ArrayList<>();
        String sql = "SELECT id, username, balance FROM users";

        return executeWithRetry(() -> {
            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    users.add(new UserAccount(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getDouble("balance")
                    ));
                }
                return users;
            }
        });
    }

    public UserAccount getUserByUsername(String username) {
        String sql = "SELECT id, username, balance FROM users WHERE username = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return new UserAccount(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getDouble("balance")
                    );
                }
                return null;
            }
        });
    }

    public boolean createUser(String username, double initialBalance) {
        String sql = "INSERT INTO users (username, balance) VALUES (?, ?)";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setDouble(2, initialBalance);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean updateBalance(int userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setDouble(1, newBalance);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
});
    }

    public boolean verifyPin(int userId, String pin) {
        if (isLocked(userId)) {
            return false;
        }
        
        String sql = "SELECT pin FROM users WHERE id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String storedPin = rs.getString("pin");
                    boolean valid = false;
                    
                    if (storedPin != null) {
                        valid = storedPin.equals(pin);
                    }
                    return valid;
                }
                return false;
            }
        });
    }

    public boolean isLocked(int userId) {
        String sql = "SELECT locked FROM users WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getBoolean("locked");
                }
                return false;
            }
        });
    }

    public int getFailedAttempts(int userId) {
        String sql = "SELECT failed_attempts FROM users WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("failed_attempts");
                }
                return 0;
            }
        });
    }

    public void incrementFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE id = ?";
        executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
                return null;
            }
        });
        if (getFailedAttempts(userId) >= 3) {
            lockAccount(userId);
        }
    }

    public boolean lockAccount(int userId) {
        String sql = "UPDATE users SET locked = TRUE WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean unlockAccount(int userId) {
        String sql = "UPDATE users SET locked = FALSE, failed_attempts = 0, lock_until = NULL, lock_count = 0 WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean lockAccount(int userId, long lockUntil, int lockCount) {
        String sql = "UPDATE users SET locked = TRUE, lock_until = FROM_UNIXTIME(?/1000), lock_count = ? WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setLong(1, lockUntil);
                pstmt.setInt(2, lockCount);
                pstmt.setInt(3, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public long getLockUntil(int userId) {
        String sql = "SELECT lock_until FROM users WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getTimestamp("lock_until") != null) {
                    return rs.getTimestamp("lock_until").getTime();
                }
                return 0L;
            }
        });
    }

    public int getLockCount(int userId) {
        String sql = "SELECT lock_count FROM users WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("lock_count");
                }
                return 0;
            }
        });
    }

    public boolean resetFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts = 0 WHERE id = ?";
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public long getLockRemainingSeconds(int userId) {
        long lockUntil = getLockUntil(userId);
        if (lockUntil == 0) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long remaining = (lockUntil - now) / 1000;
        if (remaining <= 0) {
            unlockAccount(userId);
            return 0;
        }
        return remaining;
    }

    public boolean updatePin(int userId, String newPin) {
        String hashedPin = EncryptionUtil.hashPin(newPin);
        String sql = "UPDATE users SET pin = ? WHERE id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, hashedPin);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean saveTransaction(Transaction tx, int userId) {
        String sql = "INSERT INTO transactions (tx_id, type, amount, status, from_user, to_user, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, tx.getId());
                pstmt.setString(2, tx.getType());
                pstmt.setDouble(3, tx.getAmount());
                pstmt.setString(4, tx.getStatus());
                pstmt.setString(5, tx.getFromUser());
                pstmt.setString(6, tx.getToUser());
                pstmt.setInt(7, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public List<Transaction> getUserTransactions(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT tx_id, type, amount, status, from_user, to_user, timestamp FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Transaction tx = new Transaction(
                        rs.getString("tx_id"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("status"),
                        rs.getString("from_user"),
                        rs.getString("to_user"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                    );
                    transactions.add(tx);
                }
                return transactions;
            }
        });
    }

    public double getSystemBalance() {
        String sql = "SELECT balance FROM system_accounts WHERE account_name = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, SYSTEM_ACCOUNT);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
                return 0.0;
            }
        });
    }

    public boolean updateSystemBalance(double newBalance) {
        String sql = "UPDATE system_accounts SET balance = ? WHERE account_name = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setDouble(1, newBalance);
                pstmt.setString(2, SYSTEM_ACCOUNT);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public double getSavingsBalance(int userId) {
        String sql = "SELECT COALESCE(SUM(balance), 0) as total FROM savings WHERE user_id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getDouble("total");
                }
                return 0.0;
            }
        });
    }

    public boolean createSavingsAccount(int userId) {
        String sql = "INSERT IGNORE INTO savings (user_id, balance) VALUES (?, 0.00)";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean depositToSavings(int userId, double amount) {
        if (amount <= 0) return false;
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(
                    "INSERT INTO savings (user_id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = balance + ?")) {
                pstmt.setInt(1, userId);
                pstmt.setDouble(2, amount);
                pstmt.setDouble(3, amount);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean withdrawFromSavings(int userId, double amount) {
        if (amount <= 0) return false;
        
        double current = getSavingsBalance(userId);
        if (current < amount) return false;
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(
                    "UPDATE savings SET balance = balance - ? WHERE user_id = ? AND balance >= ?")) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, amount);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean updateSavingsInterest(int userId, double newRate) {
        String sql = "UPDATE savings SET interest_rate = ? WHERE user_id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setDouble(1, newRate);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public double getSavingsInterestRate(int userId) {
        String sql = "SELECT interest_rate FROM savings WHERE user_id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getDouble("interest_rate");
                }
                return 4.0;
            }
        });
    }

    public int createBillSplit(int creatorId, double totalAmount, int splitCount, String title) {
        double eachAmount = totalAmount / splitCount;
        String sql = "INSERT INTO bill_splits (creator_id, total_amount, split_count, title) VALUES (?, ?, ?, ?)";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, creatorId);
                pstmt.setDouble(2, totalAmount);
                pstmt.setInt(3, splitCount);
                pstmt.setString(4, title);
                pstmt.executeUpdate();
                
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        });
    }

    public boolean addParticipant(int splitId, int userId, double amount) {
        String sql = "INSERT INTO bill_split_participants (split_id, user_id, amount) VALUES (?, ?, ?)";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, splitId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, amount);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean paySplitShare(int splitId, int userId) {
        String sql = "UPDATE bill_split_participants SET paid = TRUE WHERE split_id = ? AND user_id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, splitId);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean isSplitPaid(int splitId) {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN paid THEN 1 ELSE 0 END) as paid_count FROM bill_split_participants WHERE split_id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, splitId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int paid = rs.getInt("paid_count");
                    return total > 0 && total == paid;
                }
                return false;
            }
        });
    }

    public String getBillSplitDetails(int splitId) {
        String sql = "SELECT b.title, b.total_amount, b.split_count, b.status, u.username as creator " +
                   "FROM bill_splits b JOIN users u ON b.creator_id = u.id WHERE b.id = ?";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, splitId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String title = rs.getString("title");
                    double total = rs.getDouble("total_amount");
                    int count = rs.getInt("split_count");
                    String status = rs.getString("status");
                    String creator = rs.getString("creator");
                    
                    return "Bill: " + title + "\nTotal: KES " + total + "\nEach: KES " + (total/count) + 
                           "\nCreator: " + creator + "\nStatus: " + status;
                }
                return "Bill not found";
            }
        });
    }

    public List<String> getUserBillSplits(int userId) {
        List<String> splits = new ArrayList<>();
        
        String sql = "SELECT b.id, b.title, b.total_amount, b.split_count, b.status " +
                    "FROM bill_splits b WHERE b.creator_id = ? OR b.id IN " +
                    "(SELECT split_id FROM bill_split_participants WHERE user_id = ?)";
        
        return executeWithRetry(() -> {
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, userId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    String info = "Bill #" + rs.getInt("id") + ": " + rs.getString("title") + 
                                 " (KES " + rs.getDouble("total_amount") + ") - " + rs.getString("status");
                    splits.add(info);
                }
                return splits;
            }
        });
    }

    public Connection getConnectionForTransaction() {
        return null;
    }

    public void close() {
        shutdown = true;
        logger.info("DatabaseService closed");
    }
}