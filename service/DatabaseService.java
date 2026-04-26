package service;

import model.UserAccount;
import model.Transaction;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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

    private static HikariDataSource dataSource;

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());
    private volatile boolean shutdown = false;

    public DatabaseService() {
        loadEnv();
        initializeDatabase();
    }

    private void loadEnv() {
        DB_URL = System.getenv("DB_URL");
        DB_USER = System.getenv("DB_USER");
        DB_PASSWORD = System.getenv("DB_PASSWORD");
        boolean useSSL = "true".equalsIgnoreCase(System.getenv("DB_SSL"));
        boolean requireSSL = "true".equalsIgnoreCase(System.getenv("DB_REQUIRE_SSL"));

        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(".env")) {
                props.load(in);
            } catch (IOException e) {
                // Fallback to default - will fail gracefully if missing
            }
            if (DB_URL == null) DB_URL = props.getProperty("DB_URL", "jdbc:mariadb://localhost:3306");
            if (DB_USER == null) DB_USER = props.getProperty("DB_USER", "root");
            if (DB_PASSWORD == null) DB_PASSWORD = props.getProperty("DB_PASSWORD", "");
            useSSL = "true".equalsIgnoreCase(props.getProperty("DB_SSL", "false"));
            requireSSL = "true".equalsIgnoreCase(props.getProperty("DB_REQUIRE_SSL", "false"));
        }

        HikariConfig config = new HikariConfig();
        String dbFullUrl = DB_URL + "/" + DB_NAME;
        if (useSSL || requireSSL) {
            dbFullUrl += (dbFullUrl.contains("?") ? "&" : "?") + "useSSL=" + useSSL;
            if (requireSSL) {
                dbFullUrl += "&requireSSL=true";
            }
        }
        config.setJdbcUrl(dbFullUrl);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setDriverClassName(DRIVER);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(3);
        config.setConnectionTimeout(5000);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        logger.info("HikariCP connection pool initialized" + (useSSL ? " with SSL/TLS" : ""));
    }

    private void initializeDatabase() {
        executeWithRetry(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                stmt.execute("USE " + DB_NAME);

                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "pin VARCHAR(60) DEFAULT NULL, " +
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
                String defaultPinHash = EncryptionUtil.hashPin("1234");
                stmt.execute("INSERT IGNORE INTO users (username, balance, pin) VALUES ('john', 1000.00, '" + defaultPinHash + "')");
                stmt.execute("INSERT IGNORE INTO users (username, balance, pin) VALUES ('mary', 500.00, '" + defaultPinHash + "')");
                
                logger.info("Database tables ready");
            }
            return null;
        });
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
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

    public interface RetryableWithConnection<T> {
        T execute(Connection conn) throws SQLException;
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

    private <T> T executeWithConnection(RetryableWithConnection<T> operation) {
        Connection conn = null;
        SQLException lastException = null;
        int attempts = 0;
        
        while (attempts < MAX_RETRIES) {
            conn = null;
            try {
                conn = getConnection();
                lastException = null;
                T result = operation.execute(conn);
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        return executeWithConnection(conn -> {
            try (Statement stmt = conn.createStatement();
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        if (username == null || !isValidUsername(username)) {
            return false;
        }
        
        String pin = EncryptionUtil.hashPin("1234");
        String sql = "INSERT INTO users (username, balance, pin) VALUES (?, ?, ?)";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setDouble(2, initialBalance);
                pstmt.setString(3, pin);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    private boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 50) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    public boolean registerUser(String username, String pin) {
        if (username == null || pin == null) {
            return false;
        }
        if (!isValidUsername(username)) {
            return false;
        }
        if (pin.length() != 4 || !pin.matches("\\d{4}")) {
            return false;
        }
        
        String hashedPin = EncryptionUtil.hashPin(pin);
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO users (username, balance, pin) VALUES (?, 0.00, ?)")) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPin);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean updateBalance(int userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String storedPin = rs.getString("pin");
                    
                    if (storedPin == null || storedPin.isEmpty()) {
                        return "1234".equals(pin);
                    }
                    if (storedPin.startsWith("$")) {
                        return EncryptionUtil.verifyPinHash(pin, storedPin);
                    }
                    return storedPin.equals(pin);
                }
                return false;
            }
        });
    }

    public boolean isLocked(int userId) {
        String sql = "SELECT locked FROM users WHERE id = ?";
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean unlockAccount(int userId) {
        String sql = "UPDATE users SET locked = FALSE, failed_attempts = 0, lock_until = NULL, lock_count = 0 WHERE id = ?";
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean lockAccount(int userId, long lockUntil, int lockCount) {
        String sql = "UPDATE users SET locked = TRUE, lock_until = FROM_UNIXTIME(?/1000), lock_count = ? WHERE id = ?";
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, lockUntil);
                pstmt.setInt(2, lockCount);
                pstmt.setInt(3, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public long getLockUntil(int userId) {
        String sql = "SELECT lock_until FROM users WHERE id = ?";
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, hashedPin);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean saveTransaction(Transaction tx, int userId) {
        String sql = "INSERT INTO transactions (tx_id, type, amount, status, from_user, to_user, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public List<Transaction> getUserTransactions(int userId, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT tx_id, type, amount, status, from_user, to_user, timestamp FROM transactions WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, limit);
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, newBalance);
                pstmt.setString(2, SYSTEM_ACCOUNT);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean transferMoneyAtomic(int fromUserId, int toUserId, double amount, double fee) {
        return executeInTransaction(() -> {
            Connection conn = null;
            try {
                conn = getConnection();
                try (PreparedStatement withdrawStmt = conn.prepareStatement(
                        "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?");
                     PreparedStatement depositStmt = conn.prepareStatement(
                        "UPDATE users SET balance = balance + ? WHERE id = ?");
                     PreparedStatement feeStmt = conn.prepareStatement(
                        "UPDATE system_accounts SET balance = balance + ? WHERE account_name = ?")) {
                    
                    withdrawStmt.setDouble(1, amount + fee);
                    withdrawStmt.setInt(2, fromUserId);
                    withdrawStmt.setDouble(3, amount + fee);
                    int withdrawn = withdrawStmt.executeUpdate();
                    
                    if (withdrawn == 0) {
                        return false;
                    }
                    
                    depositStmt.setDouble(1, amount);
                    depositStmt.setInt(2, toUserId);
                    depositStmt.executeUpdate();
                    
                    if (fee > 0) {
                        feeStmt.setDouble(1, fee);
                        feeStmt.setString(2, SYSTEM_ACCOUNT);
                        feeStmt.executeUpdate();
                    }
                    
                    return true;
                }
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException se) {
                        logger.severe("Rollback failed: " + se.getMessage());
                    }
                }
                throw new RuntimeException("Transfer failed", e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        logger.warning("Failed to release connection: " + e.getMessage());
                    }
                }
            }
        });
    }

    public double getSavingsBalance(int userId) {
        String sql = "SELECT COALESCE(SUM(balance), 0) as total FROM savings WHERE user_id = ?";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean depositToSavings(int userId, double amount) {
        if (amount <= 0) return false;
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, newRate);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public double getSavingsInterestRate(int userId) {
        String sql = "SELECT interest_rate FROM savings WHERE user_id = ?";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, splitId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, amount);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean paySplitShare(int splitId, int userId) {
        String sql = "UPDATE bill_split_participants SET paid = TRUE WHERE split_id = ? AND user_id = ?";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, splitId);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        });
    }

    public boolean isSplitPaid(int splitId) {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN paid THEN 1 ELSE 0 END) as paid_count FROM bill_split_participants WHERE split_id = ?";
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        
        return executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        if (dataSource != null) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
    }
}