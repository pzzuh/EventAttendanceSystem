# EVENT ATTENDANCE SYSTEM — MySQL/XAMPP Migration Guide
## (SQLite → MySQL)

---

## WHAT YOU'RE GETTING

| File | Purpose |
|------|---------|
| `event_attendance_system.sql` | Full MySQL database schema + default admin account |
| `DatabaseConnection.java` | Replaces the SQLite connection class |
| `pom.xml` | Updated dependencies (MySQL driver, iText PDF, Apache POI) |
| `AttendancePdfExporter.java` | New PDF export class (fixes missing requirement) |

---

## STEP 1 — Start XAMPP

1. Open **XAMPP Control Panel**
2. Click **Start** next to **Apache**
3. Click **Start** next to **MySQL**
4. Both should show green status

---

## STEP 2 — Create the Database

**Option A — phpMyAdmin (easiest):**
1. Open browser → go to `http://localhost/phpmyadmin`
2. Click **Import** tab (top menu)
3. Choose file → select `event_attendance_system.sql`
4. Click **Go**

**Option B — MySQL Console:**
```
mysql -u root -p < event_attendance_system.sql
```
(press Enter when asked for password — it's blank by default in XAMPP)

---

## STEP 3 — Replace Project Files

In your `EventAttendanceSystem` project folder:

```
EventAttendanceSystem/
├── pom.xml                          ← REPLACE with provided pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── attendance/
                    ├── DatabaseConnection.java     ← REPLACE
                    └── AttendancePdfExporter.java  ← ADD (new file)
```

---

## STEP 4 — Find and Replace SQLite References in Source Code

Search your Java files for these SQLite-specific strings and update:

### 4a. SQLite JDBC URL → remove (handled by DatabaseConnection.java)
Search: `jdbc:sqlite`
Replace: (delete those lines — DatabaseConnection handles it now)

### 4b. SQLite driver class → remove
Search: `org.sqlite.JDBC`
Replace: (delete — MySQL driver is loaded automatically)

### 4c. SQLite AUTO_INCREMENT syntax
SQLite uses: `INTEGER PRIMARY KEY AUTOINCREMENT`
MySQL uses:  `INT AUTO_INCREMENT PRIMARY KEY`
(Already correct in the provided SQL file)

### 4d. Any `Connection con = DriverManager.getConnection("jdbc:sqlite:...")`
Replace with: `Connection con = DatabaseConnection.getConnection();`

---

## STEP 5 — Add PDF Export Button to AttendanceReportFrame

In your `AttendanceReportFrame.java`, find the Export button and update it:

```java
// Change from CSV-only export to PDF + Excel choice
JButton btnExportPDF = new JButton("Export to PDF");
btnExportPDF.addActionListener(e -> {
    AttendancePdfExporter.export(
        this,                     // parent JFrame
        attendanceTable,          // your JTable
        totalPresent,             // int from your stats
        totalLate,
        totalAbsent,
        totalPenalty,             // double
        getActiveFilterInfo()     // helper method returning filter description string
    );
});
```

---

## STEP 6 — Build and Run

```bash
cd EventAttendanceSystem
mvn clean package -q
java -jar target/EventAttendanceSystem-1.0-SNAPSHOT.jar
```

Or in VS Code: press **F5**

---

## STEP 7 — Default Login

| Username | Password |
|----------|----------|
| `superadmin` | `Admin@1234` |

---

## REQUIREMENTS CHECKLIST

| Requirement | Status |
|-------------|--------|
| Users CRUD + 5 Roles | ✅ In original repo |
| Colleges CRUD + Dean auto-account | ✅ In original repo |
| Departments CRUD + Coordinator auto-account | ✅ In original repo |
| Courses CRUD | ✅ In original repo |
| Students CRUD + Base64 photo + Year Level | ✅ In original repo |
| Events CRUD + grace period + penalty | ✅ In original repo |
| Scanner + late detection + duplicate prevention | ✅ In original repo |
| Attendance Report + all filters | ✅ In original repo |
| Statistics cards (Present/Late/Absent/Penalty) | ✅ In original repo |
| Dashboard + clickable module icons + charts | ✅ In original repo |
| **MySQL/XAMPP database** | ✅ Fixed with these files |
| **PDF export** | ✅ Fixed with AttendancePdfExporter.java |

---

## TROUBLESHOOTING

**"Communications link failure"**
→ XAMPP MySQL is not running. Start it in XAMPP Control Panel.

**"Access denied for user 'root'"**
→ Edit DatabaseConnection.java: change PASSWORD to your MySQL root password.

**"Unknown database 'event_attendance_system'"**
→ You haven't run the SQL file yet. Do Step 2.

**"com.mysql.cj.jdbc.Driver not found"**
→ Run `mvn clean package` first to download dependencies.

**Port conflict (3306 already in use)**
→ Another MySQL may be running. Change PORT in DatabaseConnection.java
  or stop the other MySQL service.
