package service;

import model.UserAccount;
import model.Transaction;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseService {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;
    private static final String DRIVER = "org.mariadb.jdbc.Driver";
    private static final String SYSTEM_ACCOUNT = "SAFARICOM";
    private static final String DB_NAME = "mpesa_db";

    public DatabaseService() {
        loadEnv();
        try {
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createDatabaseIfNeeded();
            connection.close();
            connection = DriverManager.getConnection(DB_URL + "/" + DB_NAME, DB_USER, DB_PASSWORD);
            initializeDatabase();
            System.out.println("Connected to MariaDB");
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
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

    private Connection connection;

    private void createDatabaseIfNeeded() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }
    }

    private void initializeDatabase() {
        String dbUrl = DB_URL + "/" + DB_NAME;
        try (Connection conn = DriverManager.getConnection(dbUrl, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute("USE " + DB_NAME);

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "balance DECIMAL(15,2) DEFAULT 0.00, " +
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

            System.out.println("Database tables ready");
            
            stmt.execute("INSERT IGNORE INTO system_accounts (account_name, balance) VALUES ('SAFARICOM', 0.00)");
            stmt.execute("INSERT IGNORE INTO users (username, balance) VALUES ('john', 1000.00)");
            stmt.execute("INSERT IGNORE INTO users (username, balance) VALUES ('mary', 500.00)");
            System.out.println("Default data loaded");
        } catch (SQLException e) {
            System.err.println("Init error: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public List<UserAccount> getAllUsers() {
        List<UserAccount> users = new ArrayList<>();
        String sql = "SELECT id, username, balance FROM users";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new UserAccount(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getDouble("balance")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        return users;
    }

    public UserAccount getUserByUsername(String username) {
        String sql = "SELECT id, username, balance FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UserAccount(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getDouble("balance")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting user: " + e.getMessage());
        }
        return null;
    }

    public boolean createUser(String username, double initialBalance) {
        String sql = "INSERT INTO users (username, balance) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setDouble(2, initialBalance);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBalance(int userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating balance: " + e.getMessage());
            return false;
        }
    }

    public boolean saveTransaction(Transaction tx, int userId) {
        String sql = "INSERT INTO transactions (tx_id, type, amount, status, from_user, to_user, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getType());
            pstmt.setDouble(3, tx.getAmount());
            pstmt.setString(4, tx.getStatus());
            pstmt.setString(5, tx.getFromUser());
            pstmt.setString(6, tx.getToUser());
            pstmt.setInt(7, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            return false;
        }
    }

    public List<Transaction> getUserTransactions(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT tx_id, type, amount, status, from_user, to_user, timestamp FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
                    rs.getTimestamp("transactions.timestamp").toLocalDateTime()
                );
                transactions.add(tx);
            }
        } catch (SQLException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
        return transactions;
    }

    public double getSystemBalance() {
        String sql = "SELECT balance FROM system_accounts WHERE account_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, SYSTEM_ACCOUNT);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            System.err.println("Error getting system balance: " + e.getMessage());
        }
        return 0;
    }

    public boolean updateSystemBalance(double newBalance) {
        String sql = "UPDATE system_accounts SET balance = ? WHERE account_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, SYSTEM_ACCOUNT);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating system balance: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}