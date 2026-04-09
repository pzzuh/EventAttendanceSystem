package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.Base64;

public class StudentsFrame extends JFrame {
    private JComboBox<String> cmbCollege, cmbDept, cmbCourse, cmbYearLevel, cmbFilterDept, cmbFilterCourse;
    private JTextField txtStudentId, txtFirstName, txtMiddleName, txtLastName, txtSearch;
    private JLabel lblPhotoPreview;
    private String base64Photo = "";
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;

    public StudentsFrame() {
        setTitle("AttendX — Students");
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadColleges();
        loadTable("");
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();
        JLabel title = new JLabel("◈  Students");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 8, 5, 8);
        g.weightx = 0.33;

        int row = 0;
        // Row 1: Dropdowns
        g.gridx = 0; g.gridy = row; form.add(UITheme.fieldLabel("COLLEGE *"), g);
        g.gridx = 1; form.add(UITheme.fieldLabel("DEPARTMENT *"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("COURSE *"), g); row++;

        g.gridy = row;
        g.gridx = 0; cmbCollege = UITheme.styledCombo(new String[]{"Select College"}); form.add(cmbCollege, g);
        cmbCollege.addActionListener(e -> loadDepartments());
        g.gridx = 1; cmbDept = UITheme.styledCombo(new String[]{"Select Department"}); form.add(cmbDept, g);
        cmbDept.addActionListener(e -> loadCourses());
        g.gridx = 2; cmbCourse = UITheme.styledCombo(new String[]{"Select Course"}); form.add(cmbCourse, g); row++;

        // Row 2: Student ID
        g.gridx = 0; g.gridy = row; g.gridwidth = 3; form.add(UITheme.fieldLabel("STUDENT ID *"), g); row++;
        g.gridy = row; txtStudentId = UITheme.styledField(); form.add(txtStudentId, g); row++;

        // Row 3: Names
        g.gridy = row; g.gridwidth = 1;
        g.gridx = 0; form.add(UITheme.fieldLabel("FIRST NAME *"), g);
        g.gridx = 1; form.add(UITheme.fieldLabel("MIDDLE NAME"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("LAST NAME *"), g); row++;

        g.gridy = row;
        g.gridx = 0; txtFirstName = UITheme.styledField(); form.add(txtFirstName, g);
        g.gridx = 1; txtMiddleName = UITheme.styledField(); form.add(txtMiddleName, g);
        g.gridx = 2; txtLastName = UITheme.styledField(); form.add(txtLastName, g); row++;

        // Row 4: Photo and Year Level
        g.gridy = row; g.gridwidth = 2;
        g.gridx = 0; form.add(UITheme.fieldLabel("PHOTO"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("YEAR LEVEL *"), g); row++;

        g.gridy = row;
        g.gridx = 0; g.gridwidth = 2;
        JPanel photoPanel = new JPanel(new BorderLayout(5, 0));
        photoPanel.setOpaque(false);
        lblPhotoPreview = new JLabel("No photo"); lblPhotoPreview.setFont(UITheme.FONT_SMALL); lblPhotoPreview.setForeground(Color.GRAY);
        JButton btnUpload = UITheme.outlineButton("Upload Photo");
        btnUpload.addActionListener(e -> uploadPhoto());
        photoPanel.add(btnUpload, BorderLayout.WEST);
        photoPanel.add(lblPhotoPreview, BorderLayout.CENTER);
        form.add(photoPanel, g);
        g.gridx = 2; g.gridwidth = 1;
        cmbYearLevel = UITheme.styledCombo(new String[]{"First Year", "Second Year", "Third Year", "Fourth Year"});
        form.add(cmbYearLevel, g); row++;

        // Row 5: Buttons
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); btnRow.setOpaque(false);
        JButton btnSave = UITheme.primaryButton("Add Student");
        JButton btnUpdate = UITheme.successButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear = UITheme.outlineButton("Clear");
        btnSave.addActionListener(e -> doSave());
        btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow, g);

        // Filters and Search
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(new Color(249, 250, 251));
        filterBar.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }});
        txtSearch = UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(150, 32));
        txtSearch.addActionListener(e -> loadTable(txtSearch.getText().trim(), (String) cmbFilterDept.getSelectedItem(), (String) cmbFilterCourse.getSelectedItem()));
        JButton btnSearch = UITheme.primaryButton("Search");
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim(), (String) cmbFilterDept.getSelectedItem(), (String) cmbFilterCourse.getSelectedItem()));
        filterBar.add(txtSearch); filterBar.add(btnSearch);
        
        filterBar.add(Box.createHorizontalStrut(20));
        filterBar.add(new JLabel("Dept:") {{ setFont(UITheme.FONT_SMALL); }});
        cmbFilterDept = UITheme.styledCombo(new String[]{"All Departments"});
        cmbFilterDept.setPreferredSize(new Dimension(150, 32));
        cmbFilterDept.addActionListener(e -> loadTable("", (String) cmbFilterDept.getSelectedItem(), (String) cmbFilterCourse.getSelectedItem()));
        filterBar.add(cmbFilterDept);
        
        filterBar.add(new JLabel("Course:") {{ setFont(UITheme.FONT_SMALL); }});
        cmbFilterCourse = UITheme.styledCombo(new String[]{"All Courses"});
        cmbFilterCourse.setPreferredSize(new Dimension(150, 32));
        cmbFilterCourse.addActionListener(e -> loadTable("", (String) cmbFilterDept.getSelectedItem(), (String) cmbFilterCourse.getSelectedItem()));
        filterBar.add(cmbFilterCourse);

        // Table
        String[] cols = {"ID", "Student ID", "First Name", "Last Name", "Department", "Course", "Year Level"};
        tableModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getSelectionModel().addListSelectionListener(e -> {if (!e.getValueIsAdjusting()) loadSelected();});

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));
        tableCard.add(filterBar, BorderLayout.NORTH);
        tableCard.add(UITheme.styledScrollPane(table), BorderLayout.CENTER);

        root.add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);
        center.add(form, BorderLayout.NORTH); center.add(tableCard, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void uploadPhoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                FileInputStream fis = new FileInputStream(file);
                byte[] bytes = new byte[(int) file.length()];
                fis.read(bytes);
                fis.close();
                base64Photo = Base64.getEncoder().encodeToString(bytes);
                lblPhotoPreview.setText("✓ Photo: " + file.getName());
                lblPhotoPreview.setForeground(UITheme.SUCCESS);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error uploading photo: " + e.getMessage());
            }
        }
    }

    private void loadColleges() {
        cmbCollege.removeAllItems();
        cmbCollege.addItem("Select College");
        try {
            ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery("SELECT id,college_name FROM colleges ORDER BY college_name");
            while (rs.next()) cmbCollege.addItem(rs.getInt(1) + "|" + rs.getString(2));
            rs.close();
        } catch (SQLException e) { e.printStackTrace(); }
        loadFilterDepartments();
    }

    private void loadDepartments() {
        cmbDept.removeAllItems();
        cmbDept.addItem("Select Department");
        String colItem = (String) cmbCollege.getSelectedItem();
        if (colItem == null || colItem.equals("Select College")) return;
        int colId = Integer.parseInt(colItem.split("\\|")[0]);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id,department_name FROM departments WHERE college_id=? ORDER BY department_name");
            ps.setInt(1, colId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbDept.addItem(rs.getInt(1) + "|" + rs.getString(2));
            rs.close();
            ps.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadCourses() {
        cmbCourse.removeAllItems();
        cmbCourse.addItem("Select Course");
        String deptItem = (String) cmbDept.getSelectedItem();
        if (deptItem == null || deptItem.equals("Select Department")) return;
        int deptId = Integer.parseInt(deptItem.split("\\|")[0]);
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id,course_name FROM courses WHERE department_id=? ORDER BY course_name");
            ps.setInt(1, deptId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbCourse.addItem(rs.getInt(1) + "|" + rs.getString(2));
            rs.close();
            ps.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadFilterDepartments() {
        cmbFilterDept.removeAllItems();
        cmbFilterDept.addItem("All Departments");
        try {
            ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery("SELECT DISTINCT department_name FROM departments ORDER BY department_name");
            while (rs.next()) cmbFilterDept.addItem(rs.getString(1));
            rs.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void doSave() {
        if (!validateForm()) return;
        try {
            String colItem = (String) cmbCollege.getSelectedItem();
            String deptItem = (String) cmbDept.getSelectedItem();
            String courseItem = (String) cmbCourse.getSelectedItem();
            int colId = Integer.parseInt(colItem.split("\\|")[0]);
            int deptId = Integer.parseInt(deptItem.split("\\|")[0]);
            int courseId = Integer.parseInt(courseItem.split("\\|")[0]);

            String sql = "INSERT INTO students (college_id, department_id, course_id, student_id_number, first_name, middle_name, last_name, photo, year_level) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setInt(1, colId);
            pst.setInt(2, deptId);
            pst.setInt(3, courseId);
            pst.setString(4, txtStudentId.getText().trim());
            pst.setString(5, txtFirstName.getText().trim());
            pst.setString(6, txtMiddleName.getText().trim());
            pst.setString(7, txtLastName.getText().trim());
            pst.setString(8, base64Photo.isEmpty() ? null : base64Photo);
            pst.setString(9, (String) cmbYearLevel.getSelectedItem());
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "Student added successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doUpdate() {
        if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a student to update"); return; }
        if (!validateForm()) return;
        try {
            String deptItem = (String) cmbDept.getSelectedItem();
            String courseItem = (String) cmbCourse.getSelectedItem();
            int deptId = Integer.parseInt(deptItem.split("\\|")[0]);
            int courseId = Integer.parseInt(courseItem.split("\\|")[0]);

            String sql = "UPDATE students SET student_id_number=?, first_name=?, middle_name=?, last_name=?, photo=?, year_level=?, department_id=?, course_id=? WHERE id=?";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setString(1, txtStudentId.getText().trim());
            pst.setString(2, txtFirstName.getText().trim());
            pst.setString(3, txtMiddleName.getText().trim());
            pst.setString(4, txtLastName.getText().trim());
            pst.setString(5, base64Photo.isEmpty() ? null : base64Photo);
            pst.setString(6, (String) cmbYearLevel.getSelectedItem());
            pst.setInt(7, deptId);
            pst.setInt(8, courseId);
            pst.setInt(9, selectedId);
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "Student updated successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDelete() {
        if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a student to delete"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this student?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            String sql = "DELETE FROM students WHERE id=?";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setInt(1, selectedId);
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "Student deleted successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        loadTable(search, null, null);
    }

    private void loadTable(String search, String filterDept, String filterCourse) {
        tableModel.setRowCount(0);
        try {
            String sql = "SELECT s.id, s.student_id_number, s.first_name, s.last_name, d.department_name, c.course_name, s.year_level FROM students s JOIN departments d ON s.department_id=d.id JOIN courses c ON s.course_id=c.id WHERE 1=1";
            if (!search.isEmpty()) sql += " AND (CAST(s.id AS CHAR) LIKE '%" + search + "%' OR s.student_id_number LIKE '%" + search + "%' OR s.first_name LIKE '%" + search + "%' OR s.last_name LIKE '%" + search + "%' OR d.department_name LIKE '%" + search + "%' OR c.course_name LIKE '%" + search + "%' OR s.year_level LIKE '%" + search + "%')";
            if (filterDept != null && !filterDept.equals("All Departments")) sql += " AND d.department_name='" + filterDept + "'";
            if (filterCourse != null && !filterCourse.equals("All Courses")) sql += " AND c.course_name='" + filterCourse + "'";
            sql += " ORDER BY s.first_name ASC";
            
            ResultSet rs = DatabaseConnection.getConnection().createStatement().executeQuery(sql);
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
            }
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { clearForm(); return; }
        selectedId = (int) tableModel.getValueAt(row, 0);
        txtStudentId.setText((String) tableModel.getValueAt(row, 1));
        txtFirstName.setText((String) tableModel.getValueAt(row, 2));
        txtLastName.setText((String) tableModel.getValueAt(row, 3));
        cmbYearLevel.setSelectedItem((String) tableModel.getValueAt(row, 6));
    }

    private void clearForm() {
        txtStudentId.setText("");
        txtFirstName.setText("");
        txtMiddleName.setText("");
        txtLastName.setText("");
        cmbCollege.setSelectedIndex(0);
        cmbDept.setSelectedIndex(0);
        cmbCourse.setSelectedIndex(0);
        cmbYearLevel.setSelectedIndex(0);
        base64Photo = "";
        lblPhotoPreview.setText("No photo");
        lblPhotoPreview.setForeground(Color.GRAY);
        selectedId = -1;
        table.clearSelection();
    }

    private boolean validateForm() {
        String colItem = (String) cmbCollege.getSelectedItem();
        String deptItem = (String) cmbDept.getSelectedItem();
        String courseItem = (String) cmbCourse.getSelectedItem();
        if ("Select College".equals(colItem) || "Select Department".equals(deptItem) || "Select Course".equals(courseItem) ||
            txtStudentId.getText().trim().isEmpty() || txtFirstName.getText().trim().isEmpty() || txtLastName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields");
            return false;
        }
        return true;
    }
}
