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
    private JLabel lblDateTime, lblEvent, lblDept, lblTimeRange;
    private JTextField txtBarcode;
    private JTable recentTable;
    private DefaultTableModel recentModel;
    private Timer clockTimer;

    public ScannerFrame() {
        setTitle("AttendX — Scanner");
        setSize(800, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        startClock();
        loadEventInfo();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel title = new JLabel("◈  Barcode Scanner");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UITheme.TEXT_PRIMARY);

        // Info Panel
        JPanel infoPanel = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(10, 15, 10, 15);
        g.weightx = 1;

        int row = 0;
        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        lblDateTime = new JLabel("Loading...");
        lblDateTime.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDateTime.setForeground(UITheme.ACCENT);
        infoPanel.add(lblDateTime, g); row++;

        g.gridy = row; g.gridwidth = 1;
        infoPanel.add(new JLabel("Event:") {{ setFont(UITheme.FONT_SMALL); setForeground(UITheme.TEXT_MUTED); }}, g);
        g.gridx = 1;
        lblEvent = new JLabel("No active event");
        lblEvent.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoPanel.add(lblEvent, g); row++;

        g.gridx = 0; g.gridy = row;
        infoPanel.add(new JLabel("Department:") {{ setFont(UITheme.FONT_SMALL); setForeground(UITheme.TEXT_MUTED); }}, g);
        g.gridx = 1;
        lblDept = new JLabel("—");
        lblDept.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoPanel.add(lblDept, g); row++;

        g.gridx = 0; g.gridy = row;
        infoPanel.add(new JLabel("Time:") {{ setFont(UITheme.FONT_SMALL); setForeground(UITheme.TEXT_MUTED); }}, g);
        g.gridx = 1;
        lblTimeRange = new JLabel("—");
        lblTimeRange.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoPanel.add(lblTimeRange, g);

        // Scanner Input Panel
        JPanel scanPanel = UITheme.cardPanel(new BorderLayout(10, 10));
        scanPanel.setBorder(new CompoundBorder(
            new LineBorder(UITheme.ACCENT, 2, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblScanLabel = new JLabel("Scan Student Barcode:");
        lblScanLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblScanLabel.setForeground(UITheme.TEXT_PRIMARY);

        txtBarcode = new JTextField();
        txtBarcode.setFont(new Font("Monospace", Font.PLAIN, 16));
        txtBarcode.setPreferredSize(new Dimension(0, 45));
        txtBarcode.setBorder(new LineBorder(UITheme.BORDER_COLOR, 2, true));
        txtBarcode.addActionListener(e -> processScan());

        scanPanel.add(lblScanLabel, BorderLayout.NORTH);
        scanPanel.add(txtBarcode, BorderLayout.CENTER);

        // Recent Scans Table
        String[] cols = {"#", "Student ID", "Student Name", "Status", "Time"};
        recentModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        recentTable = new JTable(recentModel);
        UITheme.styleTable(recentTable);
        recentTable.getColumnModel().getColumn(0).setMaxWidth(40);
        recentTable.getColumnModel().getColumn(3).setMaxWidth(80);

        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBackground(Color.WHITE);
        recentPanel.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));
        JLabel recentTitle = new JLabel("Recent Scans (Last 10)");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        recentTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        recentTitle.setOpaque(true);
        recentTitle.setBackground(new Color(249, 250, 251));
        recentPanel.add(recentTitle, BorderLayout.NORTH);
        recentPanel.add(UITheme.styledScrollPane(recentTable), BorderLayout.CENTER);

        // Layout
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(infoPanel, BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(scanPanel, BorderLayout.CENTER);
        root.add(recentPanel, BorderLayout.SOUTH);

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

    private void loadEventInfo() {
        try {
            // Get today's active event
            String sql = "SELECT e.id, e.event_name, d.department_name, e.start_time, e.end_time, e.grace_period FROM events e JOIN departments d ON e.department_id=d.id WHERE CURDATE() BETWEEN e.start_date AND e.end_date LIMIT 1";
            ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
            if (rs.next()) {
                lblEvent.setText(rs.getString("event_name"));
                lblDept.setText(rs.getString("department_name"));
                lblTimeRange.setText(rs.getString("start_time") + " - " + rs.getString("end_time"));
            } else {
                lblEvent.setText("No active event today");
                lblDept.setText("—");
                lblTimeRange.setText("—");
            }
            rs.close();
            loadRecentScans();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void processScan() {
        String barcode = txtBarcode.getText().trim();
        if (barcode.isEmpty()) return;

        try {
            // Get student details
            String studentSql = "SELECT id, CONCAT(first_name, ' ', last_name) as name, student_id_number, department_id FROM students WHERE student_id_number=?";
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

            // Get today's active event
            String eventSql = "SELECT id, start_time, end_time, grace_period FROM events WHERE CURDATE() BETWEEN start_date AND end_date LIMIT 1";
            ResultSet eventRs = DatabaseConnection.getConnection().createStatement().executeQuery(eventSql);

            if (!eventRs.next()) {
                JOptionPane.showMessageDialog(this, "⚠ No active event today", "Warning", JOptionPane.WARNING_MESSAGE);
                txtBarcode.setText("");
                return;
            }

            int eventId = eventRs.getInt("id");
            LocalTime eventStart = eventRs.getTime("start_time").toLocalTime();
            int graceMinutes = eventRs.getInt("grace_period");
            eventRs.close();

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

    @Override
    public void dispose() {
        if (clockTimer != null) clockTimer.stop();
        super.dispose();
    }
}
