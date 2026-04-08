package com.attendance.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DatabaseConnection.java
 *
 * Replace the existing SQLite-based connection with this MySQL version.
 * File location: src/main/java/com/attendance/DatabaseConnection.java
 *
 * REQUIREMENTS:
 *   1. XAMPP must be running (Apache + MySQL).
 *   2. Import the MySQL JDBC driver in pom.xml (see below).
 *   3. Create the database using event_attendance_system.sql first.
 */
public class DatabaseConnection {

    // ── Configuration ──────────────────────────────────────────────────────
    private static final String HOST     = "localhost";
    private static final int    PORT     = 3306;          // default XAMPP MySQL port
    private static final String DATABASE = "event_attendance_system";
    private static final String USER     = "root";        // default XAMPP user
    private static final String PASSWORD = "";            // default XAMPP password (blank)
    // ───────────────────────────────────────────────────────────────────────

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false"
        + "&allowPublicKeyRetrieval=true"
        + "&serverTimezone=Asia/Manila"
        + "&characterEncoding=UTF-8";

    private static Connection connection = null;

    /**
     * Returns a singleton MySQL connection.
     * Opens a new connection if none exists or if the existing one is closed.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] Connected to MySQL: " + DATABASE);
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                    "MySQL JDBC Driver not found. Add it to pom.xml:\n" +
                    "<dependency>\n" +
                    "  <groupId>com.mysql</groupId>\n" +
                    "  <artifactId>mysql-connector-j</artifactId>\n" +
                    "  <version>8.3.0</version>\n" +
                    "</dependency>", e);
            } catch (SQLException e) {
                throw new SQLException(
                    "Cannot connect to MySQL. Make sure XAMPP is running and\n" +
                    "the database 'event_attendance_system' exists.\n" +
                    "Error: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    /**
     * Closes the connection (call on application exit).
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DB] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Initializes the database schema and seeds default admin user if needed.
     * Call this once at application startup.
     */
    public static void initializeDatabase() {
        try {
            Connection conn = getConnection();
            // Check if users table exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "users", new String[]{"TABLE"});
            
            if (!tables.next()) {
                System.out.println("[DB] Database schema not found. Please import event_attendance_system.sql");
                return;
            }
            
            // Check if default admin exists
            String checkAdminSQL = "SELECT COUNT(*) as cnt FROM users WHERE username = 'admin'";
            PreparedStatement checkStmt = conn.prepareStatement(checkAdminSQL);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("cnt") == 0) {
                // Create default admin user
                String insertAdminSQL = "INSERT INTO users (first_name, last_name, username, password, role, status) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertAdminSQL);
                insertStmt.setString(1, "System");
                insertStmt.setString(2, "Admin");
                insertStmt.setString(3, "admin");
                insertStmt.setString(4, "admin123"); // Should be hashed in production
                insertStmt.setString(5, "Super Admin");
                insertStmt.setString(6, "Active");
                insertStmt.executeUpdate();
                System.out.println("[DB] Default admin user created");
                insertStmt.close();
            }
            rs.close();
            checkStmt.close();
            
            System.out.println("[DB] Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("[DB] Error initializing database: " + e.getMessage());
        }
    }

    // Prevent instantiation
    private DatabaseConnection() {}
}
