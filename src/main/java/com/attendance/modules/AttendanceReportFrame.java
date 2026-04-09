package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.io.*;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

        // Auto-refresh every 5 seconds to pick up new scans
        Timer autoRefresh = new Timer(5000, e -> loadTable());
        autoRefresh.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent we) { autoRefresh.stop(); }
        });
    }

    /** Called externally (e.g. from ScannerFrame) to immediately reload data. */
    public void refresh() {
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
        JButton btnExportCsv = UITheme.successButton("📊 CSV");
        JButton btnExportPdf = UITheme.successButton("📄 PDF");
        JButton btnExportExcel = UITheme.successButton("📈 Excel");
        btnFilter.addActionListener(e -> loadTable());
        btnExportCsv.addActionListener(e -> exportCSV());
        btnExportPdf.addActionListener(e -> exportPDF());
        btnExportExcel.addActionListener(e -> exportExcel());
        btnRow.add(new JLabel("Search Student:")); btnRow.add(txtSearch);
        btnRow.add(btnFilter); btnRow.add(btnExportCsv); btnRow.add(btnExportPdf); btnRow.add(btnExportExcel);
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
            "SELECT s.student_id_number, CONCAT(s.first_name,' ',s.last_name) as name, d.department_name, c.course_name, " +
            "e.event_name, a.scan_date, a.time_in, a.time_out, a.remarks, e.penalty_amount " +
            "FROM attendance a " +
            "JOIN students s ON s.id=a.student_id " +
            "JOIN events e ON e.id=a.event_id " +
            "JOIN departments d ON d.id=s.department_id " +
            "JOIN courses c ON c.id=s.course_id " +
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

        sql.append(" ORDER BY a.scan_date DESC, s.last_name ASC");

        int present = 0, late = 0, absent = 0; double penalty = 0;
        try (Connection c = DatabaseConnection.getConnection(); ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
            while (rs.next()) {
                String rem = rs.getString("remarks");
                tableModel.addRow(new Object[]{
                    rs.getString("student_id_number"),
                    rs.getString("name"),
                    rs.getString("department_name"),
                    rs.getString("course_name"),
                    rs.getString("event_name"),
                    rs.getDate("scan_date"),
                    rs.getTimestamp("time_in"),
                    rs.getTimestamp("time_out"),
                    rem
                });
                if ("Late".equals(rem)) { late++; penalty += rs.getDouble("penalty_amount"); }
                else if ("Present".equals(rem)) present++;
                else if ("Absent".equals(rem)) absent++;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        lblPresent.setText(String.valueOf(present));
        lblLate.setText(String.valueOf(late));
        lblAbsent.setText(String.valueOf(absent));
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
            JOptionPane.showMessageDialog(this, "Exported to CSV successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportPDF() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setSelectedFile(new java.io.File("attendance_report.pdf"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try {
            PdfWriter writer = new PdfWriter(fc.getSelectedFile());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Title
            Paragraph title = new Paragraph("Attendance Report").setTextAlignment(TextAlignment.CENTER).setFontSize(18).setBold();
            document.add(title);
            document.add(new Paragraph("\n"));
            
            // Stats summary
            Paragraph stats = new Paragraph(
                "Present: " + lblPresent.getText() + " | Late: " + lblLate.getText() + " | " +
                "Absent: " + lblAbsent.getText() + " | Penalty: " + lblPenalty.getText()
            ).setTextAlignment(TextAlignment.CENTER).setFontSize(10);
            document.add(stats);
            document.add(new Paragraph("\n"));
            
            // Table
            Table pdfTable = new Table(tableModel.getColumnCount());
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                Cell h = new Cell().add(new Paragraph(tableModel.getColumnName(col)).setBold());
                pdfTable.addCell(h);
            }
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object v = tableModel.getValueAt(row, col);
                    pdfTable.addCell(new Cell().add(new Paragraph(v == null ? "" : v.toString())));
                }
            }
            document.add(pdfTable);
            document.close();
            
            JOptionPane.showMessageDialog(this, "Exported to PDF successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportExcel() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setSelectedFile(new java.io.File("attendance_report.xlsx"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance");
            
            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(col);
                cell.setCellValue(tableModel.getColumnName(col));
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(row + 1);
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object v = tableModel.getValueAt(row, col);
                    org.apache.poi.ss.usermodel.Cell cell = excelRow.createCell(col);
                    if (v != null) {
                        if (v instanceof Number) {
                            cell.setCellValue(((Number) v).doubleValue());
                        } else {
                            cell.setCellValue(v.toString());
                        }
                    }
                }
            }
            
            // Auto-size columns
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                sheet.autoSizeColumn(col);
            }
            
            // Stats sheet
            Sheet statsSheet = workbook.createSheet("Statistics");
            org.apache.poi.ss.usermodel.Row row1 = statsSheet.createRow(0);
            row1.createCell(0).setCellValue("Metric");
            row1.createCell(1).setCellValue("Value");
            
            org.apache.poi.ss.usermodel.Row row2 = statsSheet.createRow(1);
            row2.createCell(0).setCellValue("Present");
            row2.createCell(1).setCellValue(lblPresent.getText());
            
            org.apache.poi.ss.usermodel.Row row3 = statsSheet.createRow(2);
            row3.createCell(0).setCellValue("Late");
            row3.createCell(1).setCellValue(lblLate.getText());
            
            org.apache.poi.ss.usermodel.Row row4 = statsSheet.createRow(3);
            row4.createCell(0).setCellValue("Absent");
            row4.createCell(1).setCellValue(lblAbsent.getText());
            
            org.apache.poi.ss.usermodel.Row row5 = statsSheet.createRow(4);
            row5.createCell(0).setCellValue("Penalty");
            row5.createCell(1).setCellValue(lblPenalty.getText());
            
            try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                workbook.write(fos);
            }
            
            JOptionPane.showMessageDialog(this, "Exported to Excel successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
