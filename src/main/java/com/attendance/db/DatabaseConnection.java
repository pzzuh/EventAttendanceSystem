package com.attendance.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
            
            boolean adminExists = false;
            if (rs.next()) {
                adminExists = rs.getInt("cnt") > 0;
            }
            if (!adminExists) {
                // Create default admin user with hashed password
                String insertAdminSQL = "INSERT INTO users (first_name, last_name, username, password, role, status) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertAdminSQL);
                insertStmt.setString(1, "System");
                insertStmt.setString(2, "Admin");
                insertStmt.setString(3, "admin");
                insertStmt.setString(4, com.attendance.util.PasswordUtil.hashPassword("admin123"));
                insertStmt.setString(5, "Super Admin");
                insertStmt.setString(6, "Active");
                insertStmt.executeUpdate();
                System.out.println("[DB] Default admin user created");
                insertStmt.close();
            } else {
                String passwordCheckSql = "SELECT password FROM users WHERE username = 'admin'";
                try (PreparedStatement pwStmt = conn.prepareStatement(passwordCheckSql);
                     ResultSet pwRs = pwStmt.executeQuery()) {
                    if (pwRs.next()) {
                        String storedPw = pwRs.getString("password");
                        if (storedPw != null && storedPw.length() < 64) {
                            String updateAdminSql = "UPDATE users SET password = ? WHERE username = 'admin'";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateAdminSql)) {
                                updateStmt.setString(1, com.attendance.util.PasswordUtil.hashPassword(storedPw));
                                updateStmt.executeUpdate();
                                System.out.println("[DB] Updated admin password to hashed format.");
                            }
                        }
                    }
                }
            }
            rs.close();
            checkStmt.close();

            // Seed default colleges, departments, and courses if empty
            ResultSet collegeCountRs = conn.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM colleges");
            boolean collegesEmpty = collegeCountRs.next() && collegeCountRs.getInt("cnt") == 0;
            collegeCountRs.close();
            if (collegesEmpty) {
                String[][][] ndmuSeed = {
                    { {"College of Arts and Sciences"},
                        {"Humanities & Social Sciences", "AB Philosophy", "AB Psychology", "AB Political Science", "AB Communication"},
                        {"Natural & Applied Sciences", "BS Biology", "BS Chemistry", "BS Environmental Science", "BS Criminology", "BS Social Work", "BS Medical Technology (MedTech)", "BS Nursing"}
                    },
                    { {"College of Business, Governance, and Accountancy"},
                        {"Accounting & Finance", "BS Accountancy", "BS Management Accounting"},
                        {"Business Administration", "BS Business Administration (BSBA) Major in Financial Management", "BS Business Administration (BSBA) Major in Human Resource Development Management", "BS Business Administration (BSBA) Major in Marketing Management", "Weekend BSBA (for working students)"},
                        {"Hospitality & Tourism", "BS Hospitality Management"}
                    },
                    { {"College of Education"},
                        {"Elementary Education", "BEED"},
                        {"Secondary Education", "BSE Major in English", "BSE Major in Filipino", "BSE Major in Mathematics", "BSE Major in Science", "BSE Major in Religious Education", "BSE Major in Social Studies"},
                        {"Physical Education", "Bachelor of Physical Education (PE)"}
                    },
                    { {"College of Engineering, Architecture, and Computing"},
                        {"Engineering", "BS Civil Engineering", "BS Computer Engineering", "BS Electronics Engineering", "BS Electrical Engineering"},
                        {"Computing & Information Technology", "BS Computer Science", "BS Information Technology", "Bachelor of Library and Information Science"},
                        {"Architecture", "BS Architecture"}
                    },
                    { {"College of Health Sciences"},
                        {"Healthcare Professions", "BS Medical Technology (MedTech)", "BS Nursing"}
                    },
                    { {"College of Law"},
                        {"Law", "Juris Doctor (JD)"}
                    },
                    { {"Graduate School"},
                        {"Graduate Programs", "Master's Degrees", "Doctoral Degrees"}
                    }
                };

                String insertCollegeSql = "INSERT INTO colleges (college_name) VALUES (?)";
                String insertDeptSql = "INSERT INTO departments (college_id, department_name) VALUES (?, ?)";
                String insertCourseSql = "INSERT INTO courses (department_id, course_name) VALUES (?, ?)";
                String insertStudentSql = "INSERT INTO students (college_id, department_id, course_id, student_id_number, first_name, middle_name, last_name, year_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                String insertEventSql = "INSERT INTO events (academic_year, department_id, start_date, end_date, start_time, end_time, grace_period, event_name, penalty_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement collegePs = conn.prepareStatement(insertCollegeSql, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement deptPs = conn.prepareStatement(insertDeptSql, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement coursePs = conn.prepareStatement(insertCourseSql, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement studentPs = conn.prepareStatement(insertStudentSql);
                PreparedStatement eventPs = conn.prepareStatement(insertEventSql);

                int studentCounter = 1;
                for (String[][] collegeData : ndmuSeed) {
                    String collegeName = collegeData[0][0];
                    collegePs.setString(1, collegeName);
                    collegePs.executeUpdate();
                    ResultSet collegeKeys = collegePs.getGeneratedKeys();
                    collegeKeys.next();
                    int collegeId = collegeKeys.getInt(1);
                    collegeKeys.close();

                    for (int i = 1; i < collegeData.length; i++) {
                        String[] deptData = collegeData[i];
                        String deptName = deptData[0];
                        deptPs.setInt(1, collegeId);
                        deptPs.setString(2, deptName);
                        deptPs.executeUpdate();
                        ResultSet deptKeys = deptPs.getGeneratedKeys();
                        deptKeys.next();
                        int deptId = deptKeys.getInt(1);
                        deptKeys.close();

                        eventPs.setString(1, "2025-2026");
                        eventPs.setInt(2, deptId);
                        eventPs.setString(3, "2025-10-05");
                        eventPs.setString(4, "2025-10-05");
                        eventPs.setString(5, "08:00:00");
                        eventPs.setString(6, "10:00:00");
                        eventPs.setInt(7, 15);
                        eventPs.setString(8, "Orientation Seminar - " + deptName);
                        eventPs.setDouble(9, 50.00);
                        eventPs.executeUpdate();

                        for (int j = 1; j < deptData.length; j++) {
                            coursePs.setInt(1, deptId);
                            coursePs.setString(2, deptData[j]);
                            coursePs.executeUpdate();
                            ResultSet courseKeys = coursePs.getGeneratedKeys();
                            courseKeys.next();
                            int courseId = courseKeys.getInt(1);
                            courseKeys.close();

                            studentPs.setInt(1, collegeId);
                            studentPs.setInt(2, deptId);
                            studentPs.setInt(3, courseId);
                            studentPs.setString(4, String.format("2025%04d", studentCounter));
                            studentPs.setString(5, "Student");
                            studentPs.setString(6, "A.");
                            studentPs.setString(7, "No.%d".replace("%d", String.valueOf(studentCounter)));
                            studentPs.setString(8, "First Year");
                            studentPs.executeUpdate();
                            studentCounter++;
                        }
                    }
                }

                collegePs.close();
                deptPs.close();
                coursePs.close();
                studentPs.close();
                eventPs.close();
                System.out.println("[DB] Seeded default NDMU colleges, departments, courses, students, and events.");
            }

            System.out.println("[DB] Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("[DB] Error initializing database: " + e.getMessage());
        }
    }

    // Prevent instantiation
    private DatabaseConnection() {}
}
