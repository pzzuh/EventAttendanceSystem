package com.attendance.db;

import java.sql.*;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:attendance_system.db";
    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    first_name TEXT NOT NULL," +
                "    middle_name TEXT," +
                "    last_name TEXT NOT NULL," +
                "    email TEXT UNIQUE," +
                "    username TEXT UNIQUE NOT NULL," +
                "    password TEXT NOT NULL," +
                "    role TEXT NOT NULL," +
                "    status TEXT DEFAULT 'Active'," +
                "    security_question TEXT," +
                "    security_answer TEXT," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS colleges (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    college_name TEXT NOT NULL UNIQUE," +
                "    dean_first_name TEXT," +
                "    dean_middle_name TEXT," +
                "    dean_last_name TEXT," +
                "    dean_user_id INTEGER," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS departments (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    college_id INTEGER NOT NULL," +
                "    department_name TEXT NOT NULL," +
                "    coordinator_first_name TEXT," +
                "    coordinator_middle_name TEXT," +
                "    coordinator_last_name TEXT," +
                "    coordinator_user_id INTEGER," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (college_id) REFERENCES colleges(id)" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS courses (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    department_id INTEGER NOT NULL," +
                "    course_name TEXT NOT NULL," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (department_id) REFERENCES departments(id)" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS students (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    college_id INTEGER," +
                "    department_id INTEGER," +
                "    course_id INTEGER," +
                "    student_id_number TEXT UNIQUE NOT NULL," +
                "    first_name TEXT NOT NULL," +
                "    middle_name TEXT," +
                "    last_name TEXT NOT NULL," +
                "    photo_base64 TEXT," +
                "    year_level TEXT," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (college_id) REFERENCES colleges(id)," +
                "    FOREIGN KEY (department_id) REFERENCES departments(id)," +
                "    FOREIGN KEY (course_id) REFERENCES courses(id)" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS events (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    academic_year TEXT NOT NULL," +
                "    department_id INTEGER," +
                "    start_date TEXT NOT NULL," +
                "    end_date TEXT NOT NULL," +
                "    start_time TEXT NOT NULL," +
                "    end_time TEXT NOT NULL," +
                "    grace_period INTEGER DEFAULT 15," +
                "    event_name TEXT NOT NULL," +
                "    penalty_amount REAL DEFAULT 0," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    FOREIGN KEY (department_id) REFERENCES departments(id)" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS attendance (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    event_id INTEGER NOT NULL," +
                "    student_id INTEGER NOT NULL," +
                "    scan_time TEXT NOT NULL," +
                "    scan_type TEXT NOT NULL," +
                "    remarks TEXT DEFAULT 'Present'," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    UNIQUE(event_id, student_id, scan_type)," +
                "    FOREIGN KEY (event_id) REFERENCES events(id)," +
                "    FOREIGN KEY (student_id) REFERENCES students(id)" +
                ")"
            );

            // Seed default super admin if none exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role='Super Admin'");
            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPw = com.attendance.util.PasswordUtil.hashPassword("Admin@1234");
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (first_name, last_name, email, username, password, role) VALUES (?,?,?,?,?,?)"
                );
                ps.setString(1, "Super");
                ps.setString(2, "Admin");
                ps.setString(3, "admin@system.com");
                ps.setString(4, "superadmin");
                ps.setString(5, hashedPw);
                ps.setString(6, "Super Admin");
                ps.executeUpdate();
            }

            // Seed colleges, departments, and courses if none exist
            ResultSet rsC = stmt.executeQuery("SELECT COUNT(*) FROM colleges");
            if (rsC.next() && rsC.getInt(1) == 0) {
                seedData(conn);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static long insertCollege(Connection conn, String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO colleges (college_name) VALUES (?)",
            java.sql.Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.executeUpdate();
        ResultSet k = ps.getGeneratedKeys();
        return k.next() ? k.getLong(1) : -1;
    }

    private static long insertDepartment(Connection conn, long collegeId, String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO departments (college_id, department_name) VALUES (?, ?)",
            java.sql.Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, collegeId);
        ps.setString(2, name);
        ps.executeUpdate();
        ResultSet k = ps.getGeneratedKeys();
        return k.next() ? k.getLong(1) : -1;
    }

    private static void insertCourse(Connection conn, long deptId, String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO courses (department_id, course_name) VALUES (?, ?)");
        ps.setLong(1, deptId);
        ps.setString(2, name);
        ps.executeUpdate();
    }

    private static void seedData(Connection conn) throws SQLException {
        long colId, deptId;

        // ── College of Arts and Sciences (CAS) ───────────────────────────
        colId = insertCollege(conn, "College of Arts and Sciences (CAS)");

        deptId = insertDepartment(conn, colId, "Humanities & Social Sciences Department");
        insertCourse(conn, deptId, "BA Communication");
        insertCourse(conn, deptId, "BA Philosophy");
        insertCourse(conn, deptId, "BA Political Science");
        insertCourse(conn, deptId, "BA Psychology");

        deptId = insertDepartment(conn, colId, "Natural Sciences Department");
        insertCourse(conn, deptId, "BS Biology");
        insertCourse(conn, deptId, "BS Chemistry");
        insertCourse(conn, deptId, "BS Environmental Science");

        deptId = insertDepartment(conn, colId, "Criminology / Social Work Department");
        insertCourse(conn, deptId, "BS Criminology");
        insertCourse(conn, deptId, "BS Social Work");

        // ── College of Engineering, Architecture & Computing (CEAC) ──────
        colId = insertCollege(conn, "College of Engineering, Architecture & Computing (CEAC)");

        deptId = insertDepartment(conn, colId, "Architecture Department");
        insertCourse(conn, deptId, "BS Architecture");

        deptId = insertDepartment(conn, colId, "Civil Engineering Department");
        insertCourse(conn, deptId, "BS Civil Engineering");

        deptId = insertDepartment(conn, colId, "Computer Studies / Computer Science Department");
        insertCourse(conn, deptId, "BS Computer Science");

        deptId = insertDepartment(conn, colId, "Information Technology Department");
        insertCourse(conn, deptId, "BS Information Technology");

        deptId = insertDepartment(conn, colId, "Computer Engineering Department");
        insertCourse(conn, deptId, "BS Computer Engineering");

        deptId = insertDepartment(conn, colId, "Electrical Engineering Department");
        insertCourse(conn, deptId, "BS Electrical Engineering");

        deptId = insertDepartment(conn, colId, "Electronics Engineering Department");
        insertCourse(conn, deptId, "BS Electronics Engineering");

        deptId = insertDepartment(conn, colId, "Library & Information Science Department");
        insertCourse(conn, deptId, "BLIS (Bachelor of Library & Information Science)");

        // ── College of Business, Governance & Accountancy (CBGA) ─────────
        colId = insertCollege(conn, "College of Business, Governance & Accountancy (CBGA)");

        deptId = insertDepartment(conn, colId, "Accountancy Department");
        insertCourse(conn, deptId, "BS Accountancy");
        insertCourse(conn, deptId, "BS Management Accounting");

        deptId = insertDepartment(conn, colId, "Business Administration Department");
        insertCourse(conn, deptId, "BSBA Human Resource Development Management");
        insertCourse(conn, deptId, "BSBA Financial Management");
        insertCourse(conn, deptId, "BSBA Marketing Management");

        deptId = insertDepartment(conn, colId, "Hospitality / Service Management Department");
        insertCourse(conn, deptId, "BS Hospitality Management");

        deptId = insertDepartment(conn, colId, "Public Administration Department");
        insertCourse(conn, deptId, "Bachelor of Public Administration");

        // ── College of Education (CED) ────────────────────────────────────
        colId = insertCollege(conn, "College of Education (CED)");

        deptId = insertDepartment(conn, colId, "Elementary Education Department");
        insertCourse(conn, deptId, "BEEd (Bachelor in Elementary Education)");

        deptId = insertDepartment(conn, colId, "Secondary Education Department");
        insertCourse(conn, deptId, "BSEd (Bachelor in Secondary Education, various majors)");

        deptId = insertDepartment(conn, colId, "Physical Education Department");
        insertCourse(conn, deptId, "BPED (Bachelor in Physical Education)");

        // ── College of Health Sciences (CHS) ─────────────────────────────
        colId = insertCollege(conn, "College of Health Sciences (CHS)");

        deptId = insertDepartment(conn, colId, "Nursing Department");
        insertCourse(conn, deptId, "BS Nursing");

        deptId = insertDepartment(conn, colId, "Medical Technology Department");
        insertCourse(conn, deptId, "BS Medical Technology");

        // ── College of Law (CL) ───────────────────────────────────────────
        colId = insertCollege(conn, "College of Law (CL)");

        deptId = insertDepartment(conn, colId, "Law Department");
        insertCourse(conn, deptId, "LLB (Bachelor of Laws)");

        // ── Graduate School ───────────────────────────────────────────────
        colId = insertCollege(conn, "Graduate School");

        deptId = insertDepartment(conn, colId, "Education & Teaching Department (Graduate School)");
        insertCourse(conn, deptId, "MAEd (various majors)");
        insertCourse(conn, deptId, "PhD in Educational Management");

        deptId = insertDepartment(conn, colId, "Science & Research Department (Graduate School)");
        insertCourse(conn, deptId, "MS / PhD in Biology");
        insertCourse(conn, deptId, "MS / PhD in Chemistry");
        insertCourse(conn, deptId, "MS / PhD in Mathematics");
        insertCourse(conn, deptId, "MS / PhD in Physics");
        insertCourse(conn, deptId, "MS / PhD in Information Technology");

        deptId = insertDepartment(conn, colId, "Business & Governance Department (Graduate School)");
        insertCourse(conn, deptId, "MBA");
        insertCourse(conn, deptId, "MPA");

        deptId = insertDepartment(conn, colId, "Library & Information Science Department (Graduate School)");
        insertCourse(conn, deptId, "MA Library Science");

        // ── Champagnat Community College (CCC) / TechVoc ─────────────────
        colId = insertCollege(conn, "Champagnat Community College (CCC) / TechVoc");

        deptId = insertDepartment(conn, colId, "Technical-Vocational Department (CCC / TechVoc)");
        insertCourse(conn, deptId, "TESDA-aligned short courses / Technical-Vocational programs");
    }
}
