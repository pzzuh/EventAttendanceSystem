package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ScannerFrame extends JFrame {

    private JLabel lblDateTime, lblEventName, lblDeptName, lblTimeRange;
    private JTextField txtStudentId;
    private JLabel lblStatus;
    private JTable recentTable;
    private DefaultTableModel recentModel;
    private int currentEventId = -1;
    private Timer clockTimer;

    public ScannerFrame() {
        setTitle("AttendX — Scanner");
        setSize(900, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadTodayEvent();
        startClock();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (clockTimer != null) clockTimer.stop();
            }
        });
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_MAIN);

        // ── Top: dark event info bar ──────────────────────────────────────
        JPanel infoBar = new JPanel(new GridLayout(1, 4, 0, 0));
        infoBar.setBackground(UITheme.SIDEBAR_BG);
        infoBar.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        lblDateTime  = infoChip("--:--:--");
        lblEventName = infoChip("No active event");
        lblDeptName  = infoChip("--");
        lblTimeRange = infoChip("--");

        infoBar.add(infoGroup("CURRENT TIME", lblDateTime));
        infoBar.add(infoGroup("EVENT", lblEventName));
        infoBar.add(infoGroup("DEPARTMENT", lblDeptName));
        infoBar.add(infoGroup("SCHEDULE", lblTimeRange));

        // ── Center: scan panel + table ────────────────────────────────────
        JPanel centerArea = new JPanel(new BorderLayout(0, 14));
        centerArea.setBackground(UITheme.BG_MAIN);
        centerArea.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Scanner input card
        JPanel scanCard = UITheme.cardPanel(new BorderLayout(0, 12));

        JLabel scanTitle = new JLabel("◈  ID / Barcode Scanner");
        scanTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        scanTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);

        JLabel inputLabel = new JLabel("ENTER STUDENT ID:");
        inputLabel.setFont(UITheme.FONT_LABEL);
        inputLabel.setForeground(UITheme.TEXT_SECONDARY);

        txtStudentId = UITheme.styledField();
        txtStudentId.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtStudentId.setPreferredSize(new Dimension(0, 48));

        JButton btnScan = new JButton("SCAN  ▶");
        btnScan.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnScan.setBackground(UITheme.ACCENT);
        btnScan.setForeground(Color.WHITE);
        btnScan.setFocusPainted(false);
        btnScan.setBorderPainted(false);
        btnScan.setOpaque(true);
        btnScan.setPreferredSize(new Dimension(120, 48));
        btnScan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnScan.addActionListener(e -> doScan());
        txtStudentId.addActionListener(e -> doScan());

        inputRow.add(txtStudentId, BorderLayout.CENTER);
        inputRow.add(btnScan, BorderLayout.EAST);

        lblStatus = new JLabel("  Ready — waiting for scan...");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        lblStatus.setPreferredSize(new Dimension(0, 38));
        lblStatus.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        lblStatus.setBackground(new Color(249, 250, 251));
        lblStatus.setOpaque(true);

        JPanel scanTop = new JPanel(new BorderLayout(0, 6));
        scanTop.setOpaque(false);
        scanTop.add(inputLabel, BorderLayout.NORTH);
        scanTop.add(inputRow, BorderLayout.CENTER);

        scanCard.add(scanTitle, BorderLayout.NORTH);
        scanCard.add(scanTop, BorderLayout.CENTER);
        scanCard.add(lblStatus, BorderLayout.SOUTH);

        // Recent scans table
        JPanel tableCard = new JPanel(new BorderLayout(0, 0));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(UITheme.SIDEBAR_BG);
        tableHeader.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel tableTitle = new JLabel("Recent ID Scans  (latest 10)");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tableTitle.setForeground(Color.WHITE);
        JButton btnRefresh = new JButton("↻ Refresh");
        btnRefresh.setFont(UITheme.FONT_SMALL); btnRefresh.setForeground(UITheme.ACCENT);
        btnRefresh.setBackground(UITheme.SIDEBAR_BG); btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false); btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadRecentScans());
        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(btnRefresh, BorderLayout.EAST);

        String[] cols = {"#", "Student ID", "Student Name", "Department", "Time", "Type", "Remarks"};
        recentModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        recentTable = new JTable(recentModel);
        UITheme.styleTable(recentTable);
        recentTable.getColumnModel().getColumn(0).setMaxWidth(40);
        recentTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                boolean in = "TIME_IN".equals(v);
                l.setText(in ? "▶ Time In" : "◀ Time Out");
                l.setForeground(sel ? UITheme.ACCENT_DARK : (in ? UITheme.INFO : UITheme.SUCCESS));
                l.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
                return l;
            }
        });
        recentTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                boolean late = "Late".equals(v);
                l.setForeground(sel ? UITheme.ACCENT_DARK : (late ? UITheme.DANGER : UITheme.SUCCESS));
                l.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
                return l;
            }
        });

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(UITheme.styledScrollPane(recentTable), BorderLayout.CENTER);

        centerArea.add(scanCard, BorderLayout.NORTH);
        centerArea.add(tableCard, BorderLayout.CENTER);

        root.add(infoBar, BorderLayout.NORTH);
        root.add(centerArea, BorderLayout.CENTER);
        setContentPane(root);

        // Focus the input field on open
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { txtStudentId.requestFocusInWindow(); }
        });
    }

    private JLabel infoChip(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(Color.WHITE);
        return l;
    }

    private JPanel infoGroup(String label, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lbl.setForeground(UITheme.SIDEBAR_TEXT);
        p.add(lbl, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private void loadTodayEvent() {
        String today = LocalDate.now().toString();
        try (Connection c = DatabaseConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT e.id,e.event_name,d.department_name,e.start_time,e.end_time,e.grace_period " +
                "FROM events e LEFT JOIN departments d ON d.id=e.department_id " +
                "WHERE e.start_date<=? AND e.end_date>=? LIMIT 1");
            ps.setString(1, today); ps.setString(2, today);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentEventId = rs.getInt("id");
                lblEventName.setText(rs.getString("event_name"));
                lblDeptName.setText(rs.getString("department_name") == null ? "--" : rs.getString("department_name"));
                lblTimeRange.setText(rs.getString("start_time") + " – " + rs.getString("end_time"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        loadRecentScans();
    }

    private void doScan() {
        String sid = txtStudentId.getText().trim();
        txtStudentId.setText("");
        txtStudentId.requestFocusInWindow();
        if (sid.isEmpty()) return;

        if (currentEventId < 0) {
            showStatus("⚠  No active event scheduled for today.", UITheme.WARNING); return;
        }

        try (Connection c = DatabaseConnection.getConnection()) {
            // Lookup student
            PreparedStatement ps = c.prepareStatement(
                "SELECT s.id,s.first_name,s.last_name,s.department_id FROM students s WHERE s.student_id_number=?");
            ps.setString(1, sid); ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                showStatus("✘  Student ID not found: " + sid, UITheme.DANGER); return;
            }
            int stuId = rs.getInt("id");
            String stuName = rs.getString("first_name") + " " + rs.getString("last_name");
            int stuDeptId = rs.getInt("department_id");

            // Check event dept
            PreparedStatement evPs = c.prepareStatement(
                "SELECT department_id,start_time,end_time,grace_period FROM events WHERE id=?");
            evPs.setInt(1, currentEventId); ResultSet evRs = evPs.executeQuery();
            if (!evRs.next()) { showStatus("✘  Event not found.", UITheme.DANGER); return; }
            int evDept = evRs.getInt("department_id");
            if (evDept > 0 && evDept != stuDeptId) {
                showStatus("⚠  " + stuName + " is NOT in this event's department.", UITheme.WARNING); return;
            }
            String evStart = evRs.getString("start_time");
            int grace = evRs.getInt("grace_period");

            // Check existing records
            PreparedStatement chk = c.prepareStatement(
                "SELECT scan_type FROM attendance WHERE event_id=? AND student_id=?");
            chk.setInt(1, currentEventId); chk.setInt(2, stuId);
            ResultSet chkRs = chk.executeQuery();
            boolean hasIn = false, hasOut = false;
            while (chkRs.next()) {
                if ("TIME_IN".equals(chkRs.getString(1))) hasIn = true;
                else hasOut = true;
            }

            if (hasIn && hasOut) {
                showStatus("◈  " + stuName + " — Already fully checked in/out.", UITheme.ACCENT); return;
            }

            String scanType = hasIn ? "TIME_OUT" : "TIME_IN";
            LocalTime now = LocalTime.now();
            String remarks = "Present";
            if ("TIME_IN".equals(scanType)) {
                try {
                    LocalTime start = LocalTime.parse(evStart);
                    if (now.isAfter(start.plusMinutes(grace))) remarks = "Late";
                } catch (Exception ex) { /* ignore parse error */ }
            }

            PreparedStatement ins = c.prepareStatement(
                "INSERT INTO attendance(event_id,student_id,scan_time,scan_type,remarks) VALUES(?,?,?,?,?)");
            ins.setInt(1, currentEventId); ins.setInt(2, stuId);
            ins.setString(3, now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            ins.setString(4, scanType); ins.setString(5, remarks);
            ins.executeUpdate();

            String icon = "Late".equals(remarks) ? "⚠" : "✔";
            Color col = "Late".equals(remarks) ? UITheme.WARNING : UITheme.SUCCESS;
            showStatus(icon + "  " + stuName + "  |  " + scanType.replace("_"," ") + "  |  " + remarks + "  |  " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), col);
            loadRecentScans();

        } catch (SQLException ex) {
            showStatus("✘  Database error: " + ex.getMessage(), UITheme.DANGER); ex.printStackTrace();
        }
    }

    private void showStatus(String msg, Color color) {
        lblStatus.setText("  " + msg);
        lblStatus.setForeground(color);
        lblStatus.setBackground(color == UITheme.SUCCESS ? new Color(240, 253, 244) :
                                color == UITheme.WARNING ? new Color(255, 251, 235) :
                                color == UITheme.DANGER  ? new Color(254, 242, 242) :
                                new Color(240, 249, 255));
    }

    private void loadRecentScans() {
        recentModel.setRowCount(0);
        if (currentEventId < 0) return;
        try (Connection c = DatabaseConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT s.student_id_number, s.first_name||' '||s.last_name, " +
                "COALESCE(d.department_name,'--'), a.scan_time, a.scan_type, a.remarks " +
                "FROM attendance a " +
                "JOIN students s ON s.id=a.student_id " +
                "LEFT JOIN departments d ON d.id=s.department_id " +
                "WHERE a.event_id=? ORDER BY a.id DESC LIMIT 10");
            ps.setInt(1, currentEventId); ResultSet rs = ps.executeQuery();
            int num = 1;
            while (rs.next()) {
                recentModel.addRow(new Object[]{
                    num++,
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void startClock() {
        clockTimer = new Timer(1000, e -> {
            lblDateTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }
}
