package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class AttendanceReportFrame extends JFrame {
    private JComboBox<String> cmbCollege, cmbDept, cmbCourse, cmbEvent, cmbStudent;
    private JTextField txtSearch;
    private JLabel lblPresent, lblAbsent, lblLate, lblPenalty;
    private JTable table;
    private DefaultTableModel tableModel;

    public AttendanceReportFrame() {
        setTitle("Attendance Report");
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadFilters();
        loadTable();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Stats cards
        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 0));
        stats.setOpaque(false);
        lblPresent = new JLabel("0"); lblAbsent = new JLabel("0");
        lblLate    = new JLabel("0"); lblPenalty = new JLabel("₱0.00");
        stats.add(statCard("✅ Present", lblPresent, UITheme.SUCCESS));
        stats.add(statCard("❌ Absent",  lblAbsent,  UITheme.DANGER));
        stats.add(statCard("⚠ Late",    lblLate,    UITheme.WARNING));
        stats.add(statCard("💰 Penalty", lblPenalty, new Color(96, 125, 139)));

        // Filter bar
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(UITheme.cardBorder());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6); g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 0.2;

        g.gridy = 0;
        g.gridx=0; filterPanel.add(UITheme.fieldLabel("College"), g);
        g.gridx=1; filterPanel.add(UITheme.fieldLabel("Department"), g);
        g.gridx=2; filterPanel.add(UITheme.fieldLabel("Course"), g);
        g.gridx=3; filterPanel.add(UITheme.fieldLabel("Event"), g);
        g.gridx=4; filterPanel.add(UITheme.fieldLabel("Student"), g);

        g.gridy = 1;
        g.gridx=0; cmbCollege = UITheme.styledCombo(new String[]{"All Colleges"}); filterPanel.add(cmbCollege, g);
        g.gridx=1; cmbDept    = UITheme.styledCombo(new String[]{"All Departments"}); filterPanel.add(cmbDept, g);
        g.gridx=2; cmbCourse  = UITheme.styledCombo(new String[]{"All Courses"}); filterPanel.add(cmbCourse, g);
        g.gridx=3; cmbEvent   = UITheme.styledCombo(new String[]{"All Events"}); filterPanel.add(cmbEvent, g);
        g.gridx=4; cmbStudent = UITheme.styledCombo(new String[]{"All Students"}); filterPanel.add(cmbStudent, g);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 5;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        txtSearch = UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(200, 32));
        JButton btnFilter = UITheme.primaryButton("Apply Filter");
        JButton btnExport = UITheme.successButton("Export CSV");
        btnFilter.addActionListener(e -> loadTable());
        btnExport.addActionListener(e -> exportCSV());
        btnRow.add(new JLabel("Search Student:")); btnRow.add(txtSearch);
        btnRow.add(btnFilter); btnRow.add(btnExport);
        filterPanel.add(btnRow, g);

        cmbCollege.addActionListener(e -> filterDepts());
        cmbDept.addActionListener(e -> filterCourses());

        // Table
        String[] cols = {"Student ID", "Student Name", "Department", "Course", "Event", "Date", "Time In", "Time Out", "Remarks"};
        tableModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(stats, BorderLayout.NORTH);
        top.add(filterPanel, BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(UITheme.styledScrollPane(table), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel statCard(String label, JLabel val, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(UITheme.cardBorder());
        JLabel l = new JLabel(label); l.setFont(UITheme.FONT_SMALL); l.setForeground(UITheme.TEXT_MUTED);
        val.setFont(new Font("Segoe UI", Font.BOLD, 26)); val.setForeground(color);
        p.add(l, BorderLayout.NORTH); p.add(val, BorderLayout.CENTER);
        return p;
    }

    private void loadFilters() {
        try (Connection c = DatabaseConnection.getConnection()) {
            ResultSet rs = c.createStatement().executeQuery("SELECT id,college_name FROM colleges ORDER BY college_name");
            while (rs.next()) cmbCollege.addItem(rs.getInt(1) + "|" + rs.getString(2));
            rs = c.createStatement().executeQuery("SELECT id,department_name FROM departments ORDER BY department_name");
            while (rs.next()) { cmbDept.addItem(rs.getInt(1)+"|"+rs.getString(2)); }
            rs = c.createStatement().executeQuery("SELECT id,course_name FROM courses ORDER BY course_name");
            while (rs.next()) cmbCourse.addItem(rs.getInt(1)+"|"+rs.getString(2));
            rs = c.createStatement().executeQuery("SELECT id,event_name FROM events ORDER BY start_date DESC");
            while (rs.next()) cmbEvent.addItem(rs.getInt(1)+"|"+rs.getString(2));
            rs = c.createStatement().executeQuery("SELECT id,first_name||' '||last_name FROM students ORDER BY last_name");
            while (rs.next()) cmbStudent.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterDepts() {
        String item = (String) cmbCollege.getSelectedItem();
        cmbDept.removeAllItems(); cmbDept.addItem("All Departments");
        if (item == null || item.startsWith("All")) return;
        int colId = Integer.parseInt(item.split("\\|")[0]);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id,department_name FROM departments WHERE college_id=? ORDER BY department_name")) {
            ps.setInt(1, colId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbDept.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterCourses() {
        String item = (String) cmbDept.getSelectedItem();
        cmbCourse.removeAllItems(); cmbCourse.addItem("All Courses");
        if (item == null || item.startsWith("All")) return;
        int dId = Integer.parseInt(item.split("\\|")[0]);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id,course_name FROM courses WHERE department_id=? ORDER BY course_name")) {
            ps.setInt(1, dId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbCourse.addItem(rs.getInt(1)+"|"+rs.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        StringBuilder sql = new StringBuilder(
            "SELECT s.student_id_number, s.first_name||' '||s.last_name, d.department_name, co.course_name, " +
            "e.event_name, e.start_date, " +
            "MAX(CASE WHEN a.scan_type='TIME_IN' THEN a.scan_time END) as time_in, " +
            "MAX(CASE WHEN a.scan_type='TIME_OUT' THEN a.scan_time END) as time_out, " +
            "MAX(a.remarks) as remarks, e.penalty_amount " +
            "FROM attendance a " +
            "JOIN students s ON s.id=a.student_id " +
            "JOIN events e ON e.id=a.event_id " +
            "LEFT JOIN departments d ON d.id=s.department_id " +
            "LEFT JOIN courses co ON co.id=s.course_id " +
            "WHERE 1=1");

        String si = (String) cmbCollege.getSelectedItem();
        if (si != null && !si.startsWith("All")) sql.append(" AND s.college_id=").append(si.split("\\|")[0]);
        si = (String) cmbDept.getSelectedItem();
        if (si != null && !si.startsWith("All")) sql.append(" AND s.department_id=").append(si.split("\\|")[0]);
        si = (String) cmbCourse.getSelectedItem();
        if (si != null && !si.startsWith("All")) sql.append(" AND s.course_id=").append(si.split("\\|")[0]);
        si = (String) cmbEvent.getSelectedItem();
        if (si != null && !si.startsWith("All")) sql.append(" AND a.event_id=").append(si.split("\\|")[0]);
        si = (String) cmbStudent.getSelectedItem();
        if (si != null && !si.startsWith("All")) sql.append(" AND a.student_id=").append(si.split("\\|")[0]);
        String search = txtSearch.getText().trim();
        if (!search.isEmpty()) sql.append(" AND (s.first_name LIKE '%").append(search).append("%' OR s.last_name LIKE '%").append(search).append("%')");

        sql.append(" GROUP BY a.event_id, a.student_id ORDER BY e.start_date DESC, s.last_name");

        int present = 0, late = 0; double penalty = 0;
        try (Connection c = DatabaseConnection.getConnection(); ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
            while (rs.next()) {
                String rem = rs.getString("remarks");
                tableModel.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rem});
                if ("Late".equals(rem)) { late++; penalty += rs.getDouble("penalty_amount"); }
                else present++;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        lblPresent.setText(String.valueOf(present));
        lblLate.setText(String.valueOf(late));
        lblAbsent.setText("N/A");
        lblPenalty.setText(String.format("₱%.2f", penalty));
    }

    private void exportCSV() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setSelectedFile(new java.io.File("attendance_report.csv"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                pw.print(tableModel.getColumnName(col));
                if (col < tableModel.getColumnCount()-1) pw.print(",");
            }
            pw.println();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object v = tableModel.getValueAt(row, col);
                    pw.print(v == null ? "" : "\"" + v + "\"");
                    if (col < tableModel.getColumnCount()-1) pw.print(",");
                }
                pw.println();
            }
            JOptionPane.showMessageDialog(this, "Exported successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
