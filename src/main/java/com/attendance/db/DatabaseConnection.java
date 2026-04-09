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

            // Seed NDMU colleges, departments, courses, students, and events (merge strategy)
            // Ensure Dean role is available in users table
            try {
                String alterRoleSql = "ALTER TABLE users MODIFY role ENUM('Super Admin', 'Dean', 'Program Head', 'Department President', 'Department Secretary', 'Department Treasurer') NOT NULL DEFAULT 'Program Head'";
                conn.createStatement().executeUpdate(alterRoleSql);
                System.out.println("[DB] Updated users role ENUM to include 'Dean'");
            } catch (SQLException e) {
                // Role ENUM might already have Dean or other version issues
                System.out.println("[DB] Note: Role ENUM update attempted (may already be correct)");
            }

            // Clean up old dean usernames first
            String deleteOldDeansSQL = "DELETE FROM users WHERE username LIKE 'dean.%' OR username LIKE 'coord.%'";
            try {
                conn.createStatement().executeUpdate(deleteOldDeansSQL);
                System.out.println("[DB] Cleaned up old dean/coordinator usernames");
            } catch (SQLException e) {
                // Ignore if no old users exist
            }

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
                    {"Secondary Education", "BSED Major in English", "BSED Major in Filipino", "BSED Major in Mathematics", "BSED Major in Science", "BSED Major in Religious Education", "BSED Major in Social Studies"},
                    {"Physical Education", "Bachelor of Physical Education (PE)"}
                },
                { {"College of Engineering, Architecture, and Computing"},
                    {"Engineering", "BS Civil Engineering", "BS Computer Engineering", "BS Electronics Engineering", "BS Electrical Engineering"},
                    {"Architecture", "BS Architecture"},
                    {"Computing & Information Technology", "BS Computer Science", "BS Information Technology", "Bachelor of Library and Information Science"}
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

            PreparedStatement insertCollegePs = conn.prepareStatement("INSERT INTO colleges (college_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement checkCollegePs = conn.prepareStatement("SELECT id, dean_user_id, dean_first_name, dean_middle_name, dean_last_name FROM colleges WHERE college_name = ?");
            PreparedStatement updateCollegePs = conn.prepareStatement("UPDATE colleges SET dean_user_id = ?, dean_first_name = ?, dean_middle_name = ?, dean_last_name = ? WHERE id = ?");
            PreparedStatement insertDeptPs = conn.prepareStatement("INSERT INTO departments (college_id, department_name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement checkDeptPs = conn.prepareStatement("SELECT id, coordinator_user_id, coordinator_first_name, coordinator_middle_name, coordinator_last_name FROM departments WHERE college_id = ? AND department_name = ?");
            PreparedStatement updateDeptPs = conn.prepareStatement("UPDATE departments SET coordinator_user_id = ?, coordinator_first_name = ?, coordinator_middle_name = ?, coordinator_last_name = ? WHERE id = ?");
            PreparedStatement insertCoursePs = conn.prepareStatement("INSERT INTO courses (department_id, course_name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement checkCoursePs = conn.prepareStatement("SELECT id FROM courses WHERE department_id = ? AND course_name = ?");
            PreparedStatement checkEventCountPs = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM events WHERE department_id = ?");
            PreparedStatement checkUserPs = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
            PreparedStatement getUserDetailsPs = conn.prepareStatement("SELECT first_name, middle_name, last_name FROM users WHERE id = ?");
            PreparedStatement insertUserPs = conn.prepareStatement("INSERT INTO users (first_name, last_name, username, password, role, status) VALUES (?, ?, ?, ?, ?, 'Active')", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement insertStudentPs = conn.prepareStatement("INSERT INTO students (college_id, department_id, course_id, student_id_number, first_name, middle_name, last_name, year_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            PreparedStatement checkStudentCountPs = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM students WHERE course_id = ?");
            PreparedStatement insertEventPs = conn.prepareStatement("INSERT INTO events (academic_year, department_id, start_date, end_date, start_time, end_time, grace_period, event_name, penalty_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

            int studentId = 1000;
            String[] deanAbbreviations = {"CAS", "CBGA", "CED", "CEAC", "CHS", "COL", "GS"};
            String[] deanFirstNames = {"Alyssa", "Miguel", "Marianne", "Jasper", "Leona", "Samuel", "Grace"};
            String[] coordFirstNames = {"Maria", "John", "Isabella", "Miguel", "Camila", "Ethan", "Claire", "Noah", "Liza", "Daniel", "Nicole", "James", "Kara", "Anton", "Hazel"};
            String[] coordLastNames = {"Perez", "Martinez", "Fernandez", "Diaz", "Cruz", "Rivera", "Flores", "Garcia", "Lopez", "Valdez", "Ruiz", "Salazar", "Serrano", "Cordero", "Domingo"};
            String[] studentFirstNames = {"Kent", "Claire", "Joshua", "Nicole", "Ryan", "Isabella", "Gabriel", "Laura", "Kevin", "Megan", "Jared", "Sarah", "Noah", "Chloe", "Ethan", "Hannah", "Caleb", "Jane", "Marcus", "Mia", "Derek", "Sophia", "Adrian", "Victoria", "Brandon", "Olivia", "Chris", "Emma", "David", "Zoe"};
            String[] studentLastNames = {"Zeroun", "Gonzales", "Santos", "Cruz", "Torres", "Romero", "Diaz", "Nunez", "Valdez", "Suarez", "Lopez", "Garcia", "Flores", "Morales", "Salazar", "Bernardo", "Medina", "Villanueva", "Ortega", "Alcantara", "Reyes", "Martinez", "Fernandez", "Rivera", "Delgado", "Mendoza", "Castillo", "Herrera", "Jimenez", "Velasco"};
            int collegeIndex = 0;

            // Check if students already seeded to avoid duplicate IDs
            PreparedStatement checkStudentsExistPs = conn.prepareStatement("SELECT COUNT(*) as cnt FROM students");
            ResultSet studentsExistRs = checkStudentsExistPs.executeQuery();
            boolean studentsAlreadySeeded = false;
            if (studentsExistRs.next()) {
                studentsAlreadySeeded = studentsExistRs.getInt(1) > 0;
            }
            studentsExistRs.close();
            checkStudentsExistPs.close();

            // Fix any old placeholder sample students from previous seed runs
            PreparedStatement fixSampleStudentsPs = conn.prepareStatement(
                    "SELECT id FROM students WHERE first_name='Sample' AND last_name LIKE 'Student-%' ORDER BY student_id_number");
            ResultSet sampleStudentsRs = fixSampleStudentsPs.executeQuery();
            int sampleIndex = 0;
            while (sampleStudentsRs.next()) {
                int sampleStudentId = sampleStudentsRs.getInt("id");
                String newFirstName = studentFirstNames[sampleIndex % studentFirstNames.length];
                String newLastName = studentLastNames[sampleIndex % studentLastNames.length];
                PreparedStatement updateSamplePs = conn.prepareStatement(
                        "UPDATE students SET first_name=?, middle_name='', last_name=? WHERE id=?");
                updateSamplePs.setString(1, newFirstName);
                updateSamplePs.setString(2, newLastName);
                updateSamplePs.setInt(3, sampleStudentId);
                updateSamplePs.executeUpdate();
                updateSamplePs.close();
                sampleIndex++;
            }
            sampleStudentsRs.close();
            fixSampleStudentsPs.close();

            for (String[][] collegeData : ndmuSeed) {
                String collegeName = collegeData[0][0];
                checkCollegePs.setString(1, collegeName);
                ResultSet checkCollege = checkCollegePs.executeQuery();
                int collegeId = 0;
                int currentDeanId = 0;
                String currentDeanFirst = null;
                String currentDeanLast = null;
                if (checkCollege.next()) {
                    collegeId = checkCollege.getInt(1);
                    currentDeanId = checkCollege.getInt(2);
                    currentDeanFirst = checkCollege.getString(3);
                    currentDeanLast = checkCollege.getString(5);
                } else {
                    insertCollegePs.setString(1, collegeName);
                    insertCollegePs.executeUpdate();
                    ResultSet keys = insertCollegePs.getGeneratedKeys();
                    if (keys.next()) collegeId = keys.getInt(1);
                    System.out.println("[DB] Added college: " + collegeName);
                }
                checkCollege.close();

                boolean deanNameMissing = currentDeanFirst == null || currentDeanFirst.isBlank() || currentDeanLast == null || currentDeanLast.isBlank();

                // Create dean if college doesn't have one or update blank dean names
                if (collegeId > 0) {
                    String deanAbbr = collegeIndex < deanAbbreviations.length ? deanAbbreviations[collegeIndex] : "Dean";
                    String deanFirstName = deanFirstNames[collegeIndex % deanFirstNames.length];
                    String deanMiddleName = "";
                    String deanUsername = "Dean." + deanAbbr;
                    int deanUserId = currentDeanId;

                    if (deanUserId <= 0) {
                        checkUserPs.setString(1, deanUsername);
                        ResultSet checkDean = checkUserPs.executeQuery();
                        if (checkDean.next()) {
                            deanUserId = checkDean.getInt(1);
                        } else {
                            insertUserPs.setString(1, deanFirstName);
                            insertUserPs.setString(2, deanAbbr);
                            insertUserPs.setString(3, deanUsername);
                            insertUserPs.setString(4, com.attendance.util.PasswordUtil.hashPassword("Dean@1234"));
                            insertUserPs.setString(5, "Dean");
                            insertUserPs.executeUpdate();
                            ResultSet deanKeys = insertUserPs.getGeneratedKeys();
                            if (deanKeys.next()) deanUserId = deanKeys.getInt(1);
                            System.out.println("[DB] Created dean user: " + deanUsername + " (ID: " + deanUserId + ")");
                        }
                        checkDean.close();
                    } else if (deanNameMissing) {
                        getUserDetailsPs.setInt(1, deanUserId);
                        ResultSet deanUserDetails = getUserDetailsPs.executeQuery();
                        if (deanUserDetails.next()) {
                            deanFirstName = deanUserDetails.getString("first_name");
                            deanMiddleName = deanUserDetails.getString("middle_name");
                            String fetchedLast = deanUserDetails.getString("last_name");
                            deanAbbr = fetchedLast != null && !fetchedLast.isBlank() ? fetchedLast : deanAbbr;
                        }
                        deanUserDetails.close();
                    }

                    if (deanUserId > 0 && deanNameMissing) {
                        updateCollegePs.setInt(1, deanUserId);
                        updateCollegePs.setString(2, deanFirstName);
                        updateCollegePs.setString(3, deanMiddleName == null ? "" : deanMiddleName);
                        updateCollegePs.setString(4, deanAbbr);
                        updateCollegePs.setInt(5, collegeId);
                        updateCollegePs.executeUpdate();
                        System.out.println("[DB] Updated college dean details: " + collegeName);
                    }
                }

                int departmentIndex = 0;
                for (int i = 1; i < collegeData.length; i++) {
                    String[] deptData = collegeData[i];
                    String deptName = deptData[0];
                    checkDeptPs.setInt(1, collegeId);
                    checkDeptPs.setString(2, deptName);
                    ResultSet checkDept = checkDeptPs.executeQuery();
                    int deptId = 0;
                    int currentCoordId = 0;
                    String currentCoordFirst = null;
                    String currentCoordLast = null;
                    if (checkDept.next()) {
                        deptId = checkDept.getInt(1);
                        currentCoordId = checkDept.getInt(2);
                        currentCoordFirst = checkDept.getString(3);
                        currentCoordLast = checkDept.getString(5);
                    } else {
                        insertDeptPs.setInt(1, collegeId);
                        insertDeptPs.setString(2, deptName);
                        insertDeptPs.executeUpdate();
                        ResultSet keys = insertDeptPs.getGeneratedKeys();
                        if (keys.next()) deptId = keys.getInt(1);
                        System.out.println("[DB] Added department: " + deptName);
                    }
                    checkDept.close();

                    boolean coordNameMissing = currentCoordFirst == null || currentCoordFirst.isBlank() || currentCoordLast == null || currentCoordLast.isBlank();

                    // Create coordinator if department doesn't have one
                    if (deptId > 0) {
                        String coordFirstName = coordFirstNames[departmentIndex % coordFirstNames.length];
                        String coordMiddleName = "";
                        String coordLastName = coordLastNames[departmentIndex % coordLastNames.length];
                        String coordUsername = "coord." + coordFirstName.toLowerCase() + "." + coordLastName.toLowerCase().replace(" ", "");
                        int coordUserId = currentCoordId;

                        if (coordUserId <= 0) {
                            checkUserPs.setString(1, coordUsername);
                            ResultSet checkCoord = checkUserPs.executeQuery();
                            if (checkCoord.next()) {
                                coordUserId = checkCoord.getInt(1);
                            } else {
                                insertUserPs.setString(1, coordFirstName);
                                insertUserPs.setString(2, coordLastName);
                                insertUserPs.setString(3, coordUsername);
                                insertUserPs.setString(4, com.attendance.util.PasswordUtil.hashPassword("Coord@1234"));
                                insertUserPs.setString(5, "Program Head");
                                insertUserPs.executeUpdate();
                                ResultSet coordKeys = insertUserPs.getGeneratedKeys();
                                if (coordKeys.next()) coordUserId = coordKeys.getInt(1);
                                System.out.println("[DB] Created coordinator user: " + coordUsername + " (ID: " + coordUserId + ")");
                            }
                            checkCoord.close();
                        }

                        if (coordUserId > 0 && coordNameMissing) {
                            if (currentCoordId > 0) {
                                getUserDetailsPs.setInt(1, coordUserId);
                                ResultSet coordUserDetails = getUserDetailsPs.executeQuery();
                                if (coordUserDetails.next()) {
                                    coordFirstName = coordUserDetails.getString("first_name");
                                    coordMiddleName = coordUserDetails.getString("middle_name");
                                    coordLastName = coordUserDetails.getString("last_name");
                                }
                                coordUserDetails.close();
                            }
                            updateDeptPs.setInt(1, coordUserId);
                            updateDeptPs.setString(2, coordFirstName);
                            updateDeptPs.setString(3, coordMiddleName == null ? "" : coordMiddleName);
                            updateDeptPs.setString(4, coordLastName);
                            updateDeptPs.setInt(5, deptId);
                            updateDeptPs.executeUpdate();
                            System.out.println("[DB] Linked coordinator to department: " + deptName);
                        }
                    }
                    departmentIndex++;

                    checkEventCountPs.setInt(1, deptId);
                    ResultSet eventCountRs = checkEventCountPs.executeQuery();
                    if (eventCountRs.next() && eventCountRs.getInt(1) == 0) {
                        insertEventPs.setString(1, "2025-2026");
                        insertEventPs.setInt(2, deptId);
                        insertEventPs.setString(3, "2025-10-05");
                        insertEventPs.setString(4, "2025-10-05");
                        insertEventPs.setString(5, "08:00:00");
                        insertEventPs.setString(6, "10:00:00");
                        insertEventPs.setInt(7, 15);
                        insertEventPs.setString(8, "Orientation - " + deptName);
                        insertEventPs.setDouble(9, 50.00);
                        insertEventPs.executeUpdate();
                    }
                    eventCountRs.close();

                    for (int j = 1; j < deptData.length; j++) {
                        String courseName = deptData[j];
                        checkCoursePs.setInt(1, deptId);
                        checkCoursePs.setString(2, courseName);
                        ResultSet checkCourse = checkCoursePs.executeQuery();
                        int courseId = 0;
                        if (checkCourse.next()) {
                            courseId = checkCourse.getInt(1);
                        } else {
                            insertCoursePs.setInt(1, deptId);
                            insertCoursePs.setString(2, courseName);
                            insertCoursePs.executeUpdate();
                            ResultSet keys = insertCoursePs.getGeneratedKeys();
                            if (keys.next()) courseId = keys.getInt(1);
                            System.out.println("[DB] Added course: " + courseName);
                        }
                        checkCourse.close();

                        checkStudentCountPs.setInt(1, courseId);
                        ResultSet checkStudent = checkStudentCountPs.executeQuery();
                        if (!studentsAlreadySeeded && checkStudent.next() && checkStudent.getInt(1) == 0) {
                            int studentIndex = studentId - 1000;
                            String studentFirstName = studentFirstNames[studentIndex % studentFirstNames.length];
                            String studentLastName = studentLastNames[studentIndex % studentLastNames.length];

                            insertStudentPs.setInt(1, collegeId);
                            insertStudentPs.setInt(2, deptId);
                            insertStudentPs.setInt(3, courseId);
                            insertStudentPs.setString(4, "2025" + String.format("%04d", studentId));
                            insertStudentPs.setString(5, studentFirstName);
                            insertStudentPs.setString(6, "");
                            insertStudentPs.setString(7, studentLastName);
                            insertStudentPs.setString(8, "First Year");
                            insertStudentPs.executeUpdate();
                            studentId++;
                        }
                        checkStudent.close();
                    }
                }
                collegeIndex++;
            }

            insertCollegePs.close();
            checkCollegePs.close();
            updateCollegePs.close();
            insertDeptPs.close();
            checkDeptPs.close();
            updateDeptPs.close();
            insertCoursePs.close();
            checkCoursePs.close();
            checkEventCountPs.close();
            checkUserPs.close();
            insertUserPs.close();
            insertStudentPs.close();
            checkStudentCountPs.close();
            insertEventPs.close();

            // Repair any existing college/department rows that lack linked dean/coordinator user IDs
            try {
                String repairCollegeLinks = "UPDATE colleges c " +
                    "JOIN users u ON u.username = CONCAT('Dean.', c.dean_last_name) " +
                    "SET c.dean_user_id = u.id " +
                    "WHERE c.dean_user_id IS NULL AND u.role = 'Dean'";
                conn.createStatement().executeUpdate(repairCollegeLinks);

                String repairDeptLinks = "UPDATE departments d " +
                    "JOIN users u ON u.username = CONCAT('coord.', LOWER(d.coordinator_first_name), '.', REPLACE(LOWER(d.coordinator_last_name), ' ', '')) " +
                    "SET d.coordinator_user_id = u.id " +
                    "WHERE d.coordinator_user_id IS NULL AND u.role = 'Program Head'";
                conn.createStatement().executeUpdate(repairDeptLinks);

                String syncCollegeNames = "UPDATE colleges c " +
                    "JOIN users u ON c.dean_user_id = u.id " +
                    "SET c.dean_first_name = u.first_name, c.dean_middle_name = u.middle_name, c.dean_last_name = u.last_name " +
                    "WHERE c.dean_user_id IS NOT NULL AND (c.dean_first_name IS NULL OR c.dean_last_name IS NULL)";
                conn.createStatement().executeUpdate(syncCollegeNames);

                String syncDeptNames = "UPDATE departments d " +
                    "JOIN users u ON d.coordinator_user_id = u.id " +
                    "SET d.coordinator_first_name = u.first_name, d.coordinator_middle_name = u.middle_name, d.coordinator_last_name = u.last_name " +
                    "WHERE d.coordinator_user_id IS NOT NULL AND (d.coordinator_first_name IS NULL OR d.coordinator_last_name IS NULL)";
                conn.createStatement().executeUpdate(syncDeptNames);

                System.out.println("[DB] Repaired college and department user links for status display.");
            } catch (SQLException repairEx) {
                System.out.println("[DB] Note: Repair pass for college/department linking failed: " + repairEx.getMessage());
            }

            System.out.println("[DB] NDMU data seeding completed with deans and coordinators linked.");
            System.out.println("[DB] Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("[DB] Error initializing database: " + e.getMessage());
        }
    }

    // Prevent instantiation
    private DatabaseConnection() {}
}
