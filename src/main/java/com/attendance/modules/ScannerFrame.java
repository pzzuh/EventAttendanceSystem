package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ScannerFrame extends JFrame {
    private JLabel lblDateTime, lblEvent, lblCollege, lblDept, lblDates, lblTimeRange;
    private JLabel lblScanMessage, lblStatus, lblStatusTimeIn, lblStatusPenalty;
    private JTextField txtBarcode, txtSearch;
    private JComboBox<String> cmbEvent, cmbStudentStatus;
    private JButton btnBrowseStudents;
    private JTable recentTable, searchTable;
    private DefaultTableModel recentModel, searchModel;
    private JPanel searchResultsPanel;
    private Timer clockTimer, refreshTimer;
    private Integer currentEventId = null;

    public ScannerFrame() {
        setTitle("Attendance System — Scanner");
        setSize(880, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        startClock();
        loadEventOptions();
        loadSelectedEvent();
        startRefreshTimer();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Attendance Scanner");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_PRIMARY);
        lblDateTime = new JLabel("Loading...");
        lblDateTime.setFont(UITheme.FONT_BODY);
        lblDateTime.setForeground(UITheme.TEXT_MUTED);
        header.add(title, BorderLayout.WEST);
        header.add(lblDateTime, BorderLayout.EAST);

        // Event and status panels
        JPanel eventStatusRow = new JPanel(new GridLayout(1, 2, 16, 0));
        eventStatusRow.setOpaque(false);

        JPanel eventPanel = UITheme.cardPanel(new GridBagLayout());
        eventPanel.setBorder(new CompoundBorder(new LineBorder(UITheme.ACCENT, 1, true), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 8, 8, 8);
        g.weightx = 1;

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        JLabel eventHeader = new JLabel("Select Event");
        eventHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        eventHeader.setForeground(UITheme.TEXT_PRIMARY);
        eventPanel.add(eventHeader, g);

        g.gridy = 1; g.gridwidth = 1;
        eventPanel.add(UITheme.fieldLabel("Event"), g);
        g.gridx = 1;
        cmbEvent = UITheme.styledCombo(new String[]{"Loading events..."});
        cmbEvent.addActionListener(e -> {
            String sel = (String) cmbEvent.getSelectedItem();
            if (sel != null && sel.contains("|")) loadSelectedEvent();
        });
        eventPanel.add(cmbEvent, g);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        JButton btnRefreshEvent = UITheme.successButton("Refresh Events");
        btnRefreshEvent.addActionListener(e -> loadEventOptions());
        eventPanel.add(btnRefreshEvent, g);

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        eventPanel.add(new JSeparator(), g);

        g.gridy = 4; g.gridwidth = 1;
        eventPanel.add(UITheme.fieldLabel("Event"), g);
        g.gridx = 1;
        lblEvent = new JLabel("No event loaded");
        lblEvent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        eventPanel.add(lblEvent, g);

        g.gridx = 0; g.gridy = 5;
        eventPanel.add(UITheme.fieldLabel("College"), g);
        g.gridx = 1;
        lblCollege = new JLabel("—");
        lblCollege.setFont(new Font("Segoe UI", Font.BOLD, 13));
        eventPanel.add(lblCollege, g);

        g.gridx = 0; g.gridy = 6;
        eventPanel.add(UITheme.fieldLabel("Department"), g);
        g.gridx = 1;
        lblDept = new JLabel("—");
        lblDept.setFont(new Font("Segoe UI", Font.BOLD, 13));
        eventPanel.add(lblDept, g);

        g.gridx = 0; g.gridy = 7;
        eventPanel.add(UITheme.fieldLabel("Dates"), g);
        g.gridx = 1;
        lblDates = new JLabel("—");
        lblDates.setFont(new Font("Segoe UI", Font.BOLD, 13));
        eventPanel.add(lblDates, g);

        g.gridx = 0; g.gridy = 8;
        eventPanel.add(UITheme.fieldLabel("Time / Grace"), g);
        g.gridx = 1;
        lblTimeRange = new JLabel("—");
        lblTimeRange.setFont(new Font("Segoe UI", Font.BOLD, 13));
        eventPanel.add(lblTimeRange, g);

        JPanel statusPanel = UITheme.cardPanel(new GridBagLayout());
        statusPanel.setBorder(new CompoundBorder(new LineBorder(new Color(79, 70, 229), 1, true), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        GridBagConstraints s = new GridBagConstraints();
        s.fill = GridBagConstraints.HORIZONTAL;
        s.insets = new Insets(8, 8, 8, 8);
        s.weightx = 1;

        s.gridx = 0; s.gridy = 0; s.gridwidth = 1;
        JLabel lookupHeader = new JLabel("Student Status Lookup");
        lookupHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lookupHeader.setForeground(new Color(79, 70, 229));
        statusPanel.add(lookupHeader, s);

        s.gridy = 1;
        cmbStudentStatus = UITheme.styledCombo(new String[]{"— Pick a Student —"});
        cmbStudentStatus.addActionListener(e -> {
            String item = (String) cmbStudentStatus.getSelectedItem();
            if (item != null && item.contains("|")) {
                String studentIdNumber = item.split("\\|")[1].split(" - ")[0].trim();
                txtBarcode.setText(studentIdNumber);
                updateStatusLookup();
            } else {
                updateStatusLookup();
            }
        });
        statusPanel.add(cmbStudentStatus, s);

        s.gridy = 2;
        JButton btnRecordAttendance = new JButton("✔ Record Attendance");
        btnRecordAttendance.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRecordAttendance.setBackground(new Color(79, 70, 229));
        btnRecordAttendance.setForeground(Color.WHITE);
        btnRecordAttendance.setFocusPainted(false);
        btnRecordAttendance.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        btnRecordAttendance.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRecordAttendance.addActionListener(e -> {
            String item = (String) cmbStudentStatus.getSelectedItem();
            if (item == null || !item.contains("|")) {
                JOptionPane.showMessageDialog(this, "Please select a student first.", "No Student Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String studentIdNumber = item.split("\\|")[1].split(" - ")[0].trim();
            txtBarcode.setText(studentIdNumber);
            processScan();
            updateStatusLookup();
        });
        statusPanel.add(btnRecordAttendance, s);

        s.gridy = 3;
        statusPanel.add(new JSeparator(), s);

        s.gridy = 4;
        lblStatus = new JLabel("STATUS\n—");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStatus.setForeground(UITheme.TEXT_SECONDARY);
        statusPanel.add(lblStatus, s);

        s.gridy = 5;
        lblStatusTimeIn = new JLabel("TIME IN\n—");
        lblStatusTimeIn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblStatusTimeIn.setForeground(UITheme.TEXT_SECONDARY);
        statusPanel.add(lblStatusTimeIn, s);

        s.gridy = 6;
        lblStatusPenalty = new JLabel("PENALTY FEE\n₱0.00");
        lblStatusPenalty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblStatusPenalty.setForeground(UITheme.TEXT_SECONDARY);
        statusPanel.add(lblStatusPenalty, s);

        eventStatusRow.add(eventPanel);
        eventStatusRow.add(statusPanel);

        // Scanner and search panel
        JPanel scanPanel = UITheme.cardPanel(new BorderLayout(14, 14));
        scanPanel.setBorder(new CompoundBorder(new LineBorder(UITheme.ACCENT, 1, true), BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel scanTitle = new JLabel("Scan / Enter Student ID");
        scanTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        scanTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel scanInput = new JPanel(new BorderLayout(10, 0));
        scanInput.setOpaque(false);
        txtBarcode = new JTextField();
        txtBarcode.setFont(new Font("Monospace", Font.PLAIN, 20));
        txtBarcode.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));
        txtBarcode.addActionListener(e -> processScan());
        scanInput.add(txtBarcode, BorderLayout.CENTER);

        lblScanMessage = new JLabel("Load an event first, then scan a barcode or search for a student.");
        lblScanMessage.setFont(UITheme.FONT_SMALL);
        lblScanMessage.setForeground(UITheme.TEXT_MUTED);

        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setOpaque(false);
        txtSearch = UITheme.styledField();
        txtSearch.setToolTipText("Filter live attendance by student ID or name");
        txtSearch.addActionListener(e -> filterLiveAttendance());
        JButton btnSearch = UITheme.primaryButton("Search");
        btnSearch.addActionListener(e -> filterLiveAttendance());
        JButton btnClear = UITheme.outlineButton("Clear");
        btnClear.addActionListener(e -> { txtSearch.setText(""); loadRecentScans(); });
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchControls.setOpaque(false);
        searchControls.add(btnClear);
        searchControls.add(btnSearch);
        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(searchControls, BorderLayout.EAST);

        // Keep searchResultsPanel as hidden (never shown) for code compatibility
        searchResultsPanel = new JPanel(new BorderLayout());
        searchResultsPanel.setVisible(false);
        String[] searchCols = {"Student ID", "Student Name", "College", "Department", "Course"};
        searchModel = new DefaultTableModel(searchCols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        searchTable = new JTable(searchModel);

        JPanel scanCenter = new JPanel(new BorderLayout(0, 12));
        scanCenter.setOpaque(false);
        scanCenter.add(scanInput, BorderLayout.NORTH);
        scanCenter.add(lblScanMessage, BorderLayout.CENTER);

        scanPanel.add(scanTitle, BorderLayout.NORTH);
        scanPanel.add(scanCenter, BorderLayout.CENTER);
        scanPanel.add(searchBar, BorderLayout.SOUTH);

        // Recent scans
        String[] cols = {"#", "Student ID", "Student Name", "Status", "Time"};
        recentModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        recentTable = new JTable(recentModel);
        UITheme.styleTable(recentTable);
        recentTable.getColumnModel().getColumn(0).setMaxWidth(40);
        recentTable.getColumnModel().getColumn(3).setMaxWidth(90);
        recentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    String status = table.getValueAt(row, 3).toString();
                    if ("Present".equalsIgnoreCase(status)) c.setBackground(new Color(220, 252, 231));
                    else if ("Late".equalsIgnoreCase(status)) c.setBackground(new Color(254, 243, 199));
                    else if ("Absent".equalsIgnoreCase(status)) c.setBackground(new Color(254, 226, 226));
                    else c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return c;
            }
        });

        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBackground(Color.WHITE);
        recentPanel.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));
        JLabel recentTitle = new JLabel("Live Attendance (auto-refresh every 30 seconds — Green=Present Orange=Late Red=Absent)");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        recentTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        recentTitle.setOpaque(true);
        recentTitle.setBackground(new Color(249, 250, 251));
        recentPanel.add(recentTitle, BorderLayout.NORTH);
        recentPanel.add(UITheme.styledScrollPane(recentTable), BorderLayout.CENTER);

        JPanel content = new JPanel();
        content.setBackground(UITheme.BG_MAIN);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(eventStatusRow);
        content.add(Box.createVerticalStrut(16));
        content.add(scanPanel);
        content.add(Box.createVerticalStrut(16));
        content.add(recentPanel);

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void startClock() {
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy  HH:mm:ss");
        lblDateTime.setText(now.format(fmt));
    }

    private void loadEventOptions() {
        cmbEvent.removeAllItems();
        cmbEvent.addItem("— Select an event —");
        try (Connection c = DatabaseConnection.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                "SELECT e.id, e.event_name, DATE_FORMAT(e.start_date, '%Y-%m-%d') AS start_date " +
                "FROM events e ORDER BY e.start_date DESC")) {
            while (rs.next()) {
                cmbEvent.addItem(rs.getInt("id") + "|" + rs.getString("event_name") + " (" + rs.getString("start_date") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSelectedEvent() {
        String item = (String) cmbEvent.getSelectedItem();
        if (item == null || item.startsWith("—")) {
            currentEventId = null;
            lblEvent.setText("No event loaded");
            lblCollege.setText("—");
            lblDept.setText("—");
            lblDates.setText("—");
            lblTimeRange.setText("—");
            lblScanMessage.setText("Select an event to start scanning.");
            return;
        }
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT e.id, e.event_name, e.start_date, e.end_date, e.start_time, e.end_time, e.grace_period, " +
                 "e.department_id, d.department_name, d.college_id, col.college_name " +
                 "FROM events e " +
                 "JOIN departments d ON e.department_id=d.id " +
                 "JOIN colleges col ON d.college_id=col.id " +
                 "WHERE e.id = ?")) {
            ps.setInt(1, Integer.parseInt(item.split("\\|")[0]));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentEventId = rs.getInt("id");
                    int eventDeptId   = rs.getInt("department_id");
                    int eventCollegeId = rs.getInt("college_id");
                    lblEvent.setText(rs.getString("event_name"));
                    lblCollege.setText(rs.getString("college_name"));
                    lblDept.setText(rs.getString("department_name"));
                    lblDates.setText(rs.getString("start_date") + " to " + rs.getString("end_date"));
                    lblTimeRange.setText(rs.getString("start_time") + " - " + rs.getString("end_time") + " | Grace " + rs.getInt("grace_period") + "m");
                    lblScanMessage.setText("Event loaded — scan a student barcode or use the search box.");
                    loadStudentLookup(eventDeptId, eventCollegeId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblScanMessage.setText("Unable to load event.");
        }
        loadRecentScans();
    }

    private void loadStudentLookup(int deptId, int collegeId) {
        cmbStudentStatus.removeAllItems();
        cmbStudentStatus.addItem("— Pick a Student —");
        try (Connection c = DatabaseConnection.getConnection()) {
            // Try department-level first
            PreparedStatement ps = c.prepareStatement(
                "SELECT id, student_id_number, first_name, last_name " +
                "FROM students WHERE department_id = ? " +
                "ORDER BY last_name, first_name");
            ps.setInt(1, deptId);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                cmbStudentStatus.addItem(rs.getInt("id") + "|" + rs.getString("student_id_number") +
                    " - " + rs.getString("first_name") + " " + rs.getString("last_name"));
                count++;
            }
            rs.close(); ps.close();

            // Fallback: college-level
            if (count == 0) {
                PreparedStatement ps2 = c.prepareStatement(
                    "SELECT id, student_id_number, first_name, last_name " +
                    "FROM students WHERE college_id = ? " +
                    "ORDER BY last_name, first_name");
                ps2.setInt(1, collegeId);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    cmbStudentStatus.addItem(rs2.getInt("id") + "|" + rs2.getString("student_id_number") +
                        " - " + rs2.getString("first_name") + " " + rs2.getString("last_name"));
                    count++;
                }
                rs2.close(); ps2.close();
            }

            System.out.println("[Scanner] Loaded " + count + " students for dept=" + deptId + " college=" + collegeId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStatusLookup() {
        String item = (String) cmbStudentStatus.getSelectedItem();
        if (item == null || item.startsWith("—")) {
            lblStatus.setText("STATUS: —");
            lblStatusTimeIn.setText("TIME IN: —");
            lblStatusPenalty.setText("PENALTY FEE: ₱0.00");
            return;
        }
        int studentId = Integer.parseInt(item.split("\\|")[0]);
        if (currentEventId == null) {
            JOptionPane.showMessageDialog(this, "Load an event before checking student status.", "No Event", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT remarks, time_in, penalty_amount FROM attendance " +
                 "WHERE student_id = ? AND event_id = ? AND DATE(scan_date) = CURDATE() LIMIT 1")) {
            ps.setInt(1, studentId);
            ps.setInt(2, currentEventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblStatus.setText("STATUS: " + rs.getString("remarks"));
                lblStatusTimeIn.setText("TIME IN: " + rs.getTimestamp("time_in"));
                lblStatusPenalty.setText("PENALTY FEE: ₱" + String.format("%.2f", rs.getDouble("penalty_amount")));
            } else {
                lblStatus.setText("STATUS: Not scanned");
                lblStatusTimeIn.setText("TIME IN: —");
                lblStatusPenalty.setText("PENALTY FEE: ₱0.00");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String findStudentStatusItem(String studentId) {
        for (int i = 0; i < cmbStudentStatus.getItemCount(); i++) {
            String item = cmbStudentStatus.getItemAt(i);
            if (item != null && item.contains("|")) {
                String id = item.split("\\|")[0];
                if (id.equals(studentId)) return item;
            }
        }
        return null;
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer(30000, e -> loadRecentScans());
        refreshTimer.start();
    }

    private void processScan() {
        String barcode = txtBarcode.getText().trim();
        if (barcode.isEmpty()) return;

        if (currentEventId == null) {
            JOptionPane.showMessageDialog(this, "Please select and load an event before scanning.", "No Event Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            // Get student details
            String studentSql = "SELECT id, CONCAT(first_name, ' ', last_name) as name, student_id_number FROM students WHERE student_id_number=?";
            PreparedStatement studentStmt = DatabaseConnection.getConnection().prepareStatement(studentSql);
            studentStmt.setString(1, barcode);
            ResultSet studentRs = studentStmt.executeQuery();

            if (!studentRs.next()) {
                JOptionPane.showMessageDialog(this, "❌ Student not found", "Error", JOptionPane.ERROR_MESSAGE);
                txtBarcode.setText("");
                return;
            }

            int studentId = studentRs.getInt("id");
            String studentName = studentRs.getString("name");
            studentRs.close();
            studentStmt.close();

            int eventId = currentEventId;
            String eventSql = "SELECT start_time, end_time, grace_period FROM events WHERE id = ?";
            PreparedStatement eventStmt = DatabaseConnection.getConnection().prepareStatement(eventSql);
            eventStmt.setInt(1, eventId);
            ResultSet eventRs = eventStmt.executeQuery();

            if (!eventRs.next()) {
                JOptionPane.showMessageDialog(this, "⚠ Selected event not found", "Warning", JOptionPane.WARNING_MESSAGE);
                txtBarcode.setText("");
                return;
            }

            LocalTime eventStart = eventRs.getTime("start_time").toLocalTime();
            int graceMinutes = eventRs.getInt("grace_period");
            eventRs.close();
            eventStmt.close();

            // Check for duplicate attendance today
            String checkSql = "SELECT id FROM attendance WHERE student_id=? AND event_id=? AND DATE(scan_date)=CURDATE()";
            PreparedStatement checkStmt = DatabaseConnection.getConnection().prepareStatement(checkSql);
            checkStmt.setInt(1, studentId);
            checkStmt.setInt(2, eventId);
            ResultSet checkRs = checkStmt.executeQuery();

            if (checkRs.next()) {
                JOptionPane.showMessageDialog(this, "⚠ Duplicate scan - Already recorded", "Duplicate", JOptionPane.WARNING_MESSAGE);
                txtBarcode.setText("");
                return;
            }
            checkRs.close();
            checkStmt.close();

            // Determine if late
            LocalTime now = LocalTime.now();
            LocalTime lateTime = eventStart.plusMinutes(graceMinutes);
            String remarks = now.isAfter(lateTime) ? "Late" : "Present";

            // Record attendance
            String insertSql = "INSERT INTO attendance (student_id, event_id, time_in, remarks, scan_date) VALUES (?, ?, NOW(), ?, CURDATE())";
            PreparedStatement insertStmt = DatabaseConnection.getConnection().prepareStatement(insertSql);
            insertStmt.setInt(1, studentId);
            insertStmt.setInt(2, eventId);
            insertStmt.setString(3, remarks);
            insertStmt.executeUpdate();
            insertStmt.close();

            // Show result
            JOptionPane.showMessageDialog(this, "✓ " + studentName + "\n" + remarks.toUpperCase(), "Scanned", JOptionPane.INFORMATION_MESSAGE);

            txtBarcode.setText("");
            loadRecentScans();
            notifyAttendanceReport();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadRecentScans() {
        recentModel.setRowCount(0);
        try {
            String sql = "SELECT a.id, s.student_id_number, CONCAT(s.first_name, ' ', s.last_name) as name, a.remarks, a.time_in FROM attendance a JOIN students s ON a.student_id=s.id WHERE DATE(a.scan_date)=CURDATE() ORDER BY a.time_in DESC LIMIT 10";
            ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
            int count = 1;
            while (rs.next() && count <= 10) {
                recentModel.addRow(new Object[]{
                    count++,
                    rs.getString("student_id_number"),
                    rs.getString("name"),
                    rs.getString("remarks"),
                    rs.getTimestamp("time_in")
                });
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterLiveAttendance() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) { loadRecentScans(); return; }
        recentModel.setRowCount(0);
        try {
            String sql = "SELECT a.id, s.student_id_number, CONCAT(s.first_name, ' ', s.last_name) as name, a.remarks, a.time_in " +
                         "FROM attendance a JOIN students s ON a.student_id = s.id " +
                         "WHERE DATE(a.scan_date) = CURDATE() " +
                         "AND (s.student_id_number LIKE ? OR CONCAT(s.first_name, ' ', s.last_name) LIKE ?) " +
                         "ORDER BY a.time_in DESC";
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
            String term = "%" + query + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ResultSet rs = ps.executeQuery();
            int count = 1;
            while (rs.next()) {
                recentModel.addRow(new Object[]{
                    count++,
                    rs.getString("student_id_number"),
                    rs.getString("name"),
                    rs.getString("remarks"),
                    rs.getTimestamp("time_in")
                });
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearSearch() {
        txtSearch.setText("");
        searchModel.setRowCount(0);
        searchResultsPanel.setVisible(false);
    }

    /** Refreshes any open AttendanceReportFrame immediately after a scan. */
    private void notifyAttendanceReport() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            if (w instanceof AttendanceReportFrame && w.isShowing()) {
                ((AttendanceReportFrame) w).refresh();
            }
        }
    }

    @Override
    public void dispose() {
        if (clockTimer != null) clockTimer.stop();
        super.dispose();
    }
}
