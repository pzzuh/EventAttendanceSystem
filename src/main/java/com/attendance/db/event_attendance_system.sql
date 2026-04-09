-- ============================================================
-- EVENT ATTENDANCE SYSTEM - MySQL Database Schema
-- For use with XAMPP (MySQL 5.7+ or MariaDB)
-- Run this in phpMyAdmin or MySQL console
-- ============================================================

CREATE DATABASE IF NOT EXISTS event_attendance_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE event_attendance_system;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name   VARCHAR(100) NOT NULL,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,  -- SHA-256 hash
    role        ENUM(
                    'Super Admin',
                    'Dean',
                    'Program Head',
                    'Department President',
                    'Department Secretary',
                    'Department Treasurer'
                ) NOT NULL DEFAULT 'Program Head',
    status      ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active',
    security_question VARCHAR(255),
    security_answer   VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Default Super Admin (password: Admin@1234 → SHA-256)
INSERT INTO users (first_name, middle_name, last_name, username, password, role, status)
VALUES ('Super', '', 'Admin', 'superadmin',
        '6a1aaa0a6e80b0d0c47e9dbd67b26b55e5ff6e5b5f8efb5f0e10d8c5e6e0b5a1',
        'Super Admin', 'Active')
ON DUPLICATE KEY UPDATE username = username;

-- ============================================================
-- TABLE: colleges
-- ============================================================
CREATE TABLE IF NOT EXISTS colleges (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    college_name    VARCHAR(200) NOT NULL,
    dean_first_name VARCHAR(100),
    dean_middle_name VARCHAR(100),
    dean_last_name  VARCHAR(100),
    dean_user_id    INT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dean_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: departments
-- ============================================================
CREATE TABLE IF NOT EXISTS departments (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    college_id          INT NOT NULL,
    department_name     VARCHAR(200) NOT NULL,
    coordinator_first_name  VARCHAR(100),
    coordinator_middle_name VARCHAR(100),
    coordinator_last_name   VARCHAR(100),
    coordinator_user_id     INT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE CASCADE,
    FOREIGN KEY (coordinator_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: courses
-- ============================================================
CREATE TABLE IF NOT EXISTS courses (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    department_id   INT NOT NULL,
    course_name     VARCHAR(200) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: students
-- ============================================================
CREATE TABLE IF NOT EXISTS students (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    college_id      INT NOT NULL,
    department_id   INT NOT NULL,
    course_id       INT NOT NULL,
    student_id_number VARCHAR(50) NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    middle_name     VARCHAR(100),
    last_name       VARCHAR(100) NOT NULL,
    photo           LONGTEXT,   -- Base64 encoded image
    year_level      ENUM('First Year','Second Year','Third Year','Fourth Year') NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (college_id)    REFERENCES colleges(id)    ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)     REFERENCES courses(id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: events
-- ============================================================
CREATE TABLE IF NOT EXISTS events (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    academic_year   VARCHAR(20) NOT NULL,   -- e.g. 2025-2026
    department_id   INT NOT NULL,
    start_date      DATE NOT NULL,          -- YYYY-MM-DD
    end_date        DATE NOT NULL,
    start_time      TIME NOT NULL,          -- HH:MM (24hr)
    end_time        TIME NOT NULL,
    grace_period    INT NOT NULL DEFAULT 15, -- minutes
    event_name      VARCHAR(200) NOT NULL,
    penalty_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: attendance
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    event_id        INT NOT NULL,
    time_in         DATETIME,
    time_out        DATETIME,
    remarks         ENUM('Present','Late','Absent') DEFAULT 'Present',
    penalty_amount  DECIMAL(10,2) DEFAULT 0.00,
    scan_date       DATE NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Prevent duplicate: one record per student per event per day
    UNIQUE KEY uq_attendance (student_id, event_id, scan_date),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id)   REFERENCES events(id)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- USEFUL VIEWS (optional but handy for reports)
-- ============================================================

CREATE OR REPLACE VIEW vw_attendance_report AS
SELECT
    a.id,
    a.scan_date,
    a.time_in,
    a.time_out,
    a.remarks,
    a.penalty_amount,
    CONCAT(s.first_name, ' ', IFNULL(s.middle_name,''), ' ', s.last_name) AS student_name,
    s.student_id_number,
    s.year_level,
    c.college_name,
    d.department_name,
    cr.course_name,
    e.event_name,
    e.academic_year,
    e.start_time,
    e.end_time,
    e.grace_period
FROM attendance a
JOIN students   s  ON a.student_id  = s.id
JOIN colleges   c  ON s.college_id  = c.id
JOIN departments d ON s.department_id = d.id
JOIN courses    cr ON s.course_id   = cr.id
JOIN events     e  ON a.event_id    = e.id;

-- ============================================================
-- Done! Default login: superadmin / Admin@1234
-- ============================================================
