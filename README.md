# Event Attendance System

A Java Swing desktop application for managing event attendance in academic institutions.

---

## ✅ Requirements
- **Java 17+** (JDK, not JRE)
- **Maven 3.6+**  OR  **VS Code** with the Java Extension Pack

---

## 🚀 How to Run

### Option A — VS Code (Recommended)
1. Install **Extension Pack for Java** from the VS Code Marketplace
2. Open this folder in VS Code: `File → Open Folder`
3. Wait for Java to index (bottom status bar)
4. Press **F5** or click **Run → Start Debugging**
5. Select **"Launch Event Attendance System"**

### Option B — Maven (Command Line)
```bash
cd EventAttendanceSystem
mvn clean package -q
java -jar target/EventAttendanceSystem-1.0-SNAPSHOT.jar
```

### Option C — Direct compile (no Maven)
```bash
# Download SQLite JDBC first:
# https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
# Put it in a lib/ folder, then:

javac -cp "lib/*" -d target/classes -sourcepath src/main/java \
  $(find src/main/java -name "*.java")

java -cp "target/classes:lib/*" com.attendance.Main
```

---

## 🔐 Default Login
| Field    | Value        |
|----------|--------------|
| Username | `superadmin` |
| Password | `Admin@1234` |

---

## 📋 Modules
| Module            | Description                                    |
|-------------------|------------------------------------------------|
| Login             | Secure login with generic error messages       |
| Sign Up           | Registration with full password validation     |
| Forgot Password   | 3-step recovery via security question          |
| Dashboard         | Stats overview + module navigation             |
| Users             | CRUD for system users                          |
| Colleges          | CRUD + auto dean account creation              |
| Departments       | CRUD + auto coordinator account creation       |
| Courses           | CRUD under departments                         |
| Students          | CRUD with photo (Base64) + year level          |
| Events            | CRUD with dates, times, grace period, penalty  |
| Scanner           | Barcode scanner with late detection            |
| Attendance Report | Filtered reports + CSV export                  |

---

## 🔒 Security Features Applied to Login & Register
- ✅ Generic error messages — no username/password enumeration
- ✅ Email format validation (regex)
- ✅ Password strength enforced: 8+ chars, uppercase, lowercase, number, symbol
- ✅ Password confirmation matching
- ✅ All required fields validated before submission
- ✅ Passwords stored as SHA-256 hashes (never plaintext)
- ✅ Forgot password via security question (3-step flow)

---

## 🗄️ Database
Uses **SQLite** — the file `attendance_system.db` is auto-created in the working directory on first run. No external database setup needed.
