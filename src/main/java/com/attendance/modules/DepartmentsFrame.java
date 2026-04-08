package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.PasswordUtil;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;

public class DepartmentsFrame extends JFrame {
    private JComboBox<String> cmbCollege;
    private JTextField txtDeptName, txtCoordFirst, txtCoordMiddle, txtCoordLast, txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;
    private Runnable onClose;

    public DepartmentsFrame() { this(null); }

    public DepartmentsFrame(Runnable onClose) {
        this.onClose = onClose;
        setTitle("AttendX — Departments");
        setSize(960, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadColleges();
        loadTable("");
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { if (onClose != null) onClose.run(); }
        });
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();
        JLabel title = new JLabel("◈  Departments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 8, 5, 8);
        g.weightx = 0.33;

        int row = 0;
        // College dropdown — full width
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        form.add(UITheme.fieldLabel("COLLEGE *"), g); row++;
        g.gridy = row;
        cmbCollege = UITheme.styledCombo(new String[]{"Select College"});
        form.add(cmbCollege, g); row++;

        // Department name — full width
        g.gridy = row; form.add(UITheme.fieldLabel("DEPARTMENT NAME *"), g); row++;
        g.gridy = row; txtDeptName = UITheme.styledField(); form.add(txtDeptName, g); row++;

        // Coordinator section
        g.gridy = row;
        JLabel cHdr = new JLabel("Program Coordinator  (automatically creates a user account)");
        cHdr.setFont(UITheme.FONT_SMALL); cHdr.setForeground(UITheme.TEXT_MUTED);
        form.add(cHdr, g); row++;

        g.gridwidth = 1;
        g.gridx = 0; g.gridy = row; form.add(UITheme.fieldLabel("FIRST NAME *"), g);
        g.gridx = 1; form.add(UITheme.fieldLabel("MIDDLE NAME"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("LAST NAME *"), g); row++;

        g.gridy = row;
        g.gridx = 0; txtCoordFirst  = UITheme.styledField(); form.add(txtCoordFirst, g);
        g.gridx = 1; txtCoordMiddle = UITheme.styledField(); form.add(txtCoordMiddle, g);
        g.gridx = 2; txtCoordLast   = UITheme.styledField(); form.add(txtCoordLast, g); row++;

        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); btnRow.setOpaque(false);
        JButton btnSave   = UITheme.primaryButton("Add Department");
        JButton btnUpdate = UITheme.successButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear  = UITheme.outlineButton("Clear");
        btnSave.addActionListener(e -> doSave()); btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete()); btnClear.addActionListener(e -> clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow, g);

        // Search bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(new Color(249, 250, 251));
        filterBar.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        txtSearch = UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(260, 32));
        JButton btnSearch = UITheme.primaryButton("Search");
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        txtSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }});
        filterBar.add(txtSearch); filterBar.add(btnSearch);

        String[] cols = {"ID", "College", "Department Name", "Coordinator"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadSelected(); });

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

    private void loadColleges() {
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                 "SELECT id,college_name FROM colleges ORDER BY college_name")) {
            while (rs.next()) cmbCollege.addItem(rs.getInt(1) + "|" + rs.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void doSave() {
        String dept   = txtDeptName.getText().trim();
        String cFirst = txtCoordFirst.getText().trim();
        String cLast  = txtCoordLast.getText().trim();
        String colItem = (String) cmbCollege.getSelectedItem();
        if ("Select College".equals(colItem) || dept.isEmpty() || cFirst.isEmpty() || cLast.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all required fields."); return;
        }
        int colId = Integer.parseInt(colItem.split("\\|")[0]);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String uname = (cFirst.toLowerCase().charAt(0) + cLast.toLowerCase()).replaceAll("\\s+", "");
            PreparedStatement uPs = conn.prepareStatement(
                "INSERT INTO users (first_name,middle_name,last_name,username,password,role) VALUES (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            uPs.setString(1, cFirst); uPs.setString(2, txtCoordMiddle.getText().trim());
            uPs.setString(3, cLast);  uPs.setString(4, uname);
            uPs.setString(5, PasswordUtil.hashPassword("Coord@1234")); uPs.setString(6, "Program Head");
            uPs.executeUpdate();
            ResultSet keys = uPs.getGeneratedKeys();
            int cId = keys.next() ? keys.getInt(1) : -1;

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO departments (college_id,department_name,coordinator_first_name,coordinator_middle_name,coordinator_last_name,coordinator_user_id) VALUES (?,?,?,?,?,?)");
            ps.setInt(1, colId); ps.setString(2, dept); ps.setString(3, cFirst);
            ps.setString(4, txtCoordMiddle.getText().trim()); ps.setString(5, cLast); ps.setInt(6, cId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Department saved!\nCoordinator account: " + uname + " / Coord@1234");
            clearForm(); loadTable("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doUpdate() {
        if (selectedId < 0) { JOptionPane.showMessageDialog(this, "Select a row."); return; }
        String dept = txtDeptName.getText().trim();
        if (dept.isEmpty()) { JOptionPane.showMessageDialog(this, "Name required."); return; }
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE departments SET department_name=? WHERE id=?");
            ps.setString(1, dept); ps.setInt(2, selectedId); ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated."); clearForm(); loadTable("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDelete() {
        if (selectedId < 0) { JOptionPane.showMessageDialog(this, "Select a row."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this department?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.createStatement().executeUpdate("DELETE FROM departments WHERE id=" + selectedId);
            JOptionPane.showMessageDialog(this, "Deleted."); clearForm(); loadTable("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow(); if (row < 0) return;
        selectedId = (int) tableModel.getValueAt(row, 0);
        txtDeptName.setText((String) tableModel.getValueAt(row, 2));
    }

    private void clearForm() {
        selectedId = -1; txtDeptName.setText("");
        txtCoordFirst.setText(""); txtCoordMiddle.setText(""); txtCoordLast.setText("");
        cmbCollege.setSelectedIndex(0); table.clearSelection();
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = "SELECT d.id, c.college_name, d.department_name, " +
            "COALESCE(d.coordinator_first_name,'') || ' ' || COALESCE(d.coordinator_last_name,'') " +
            "FROM departments d JOIN colleges c ON c.id=d.college_id" +
            (search.isEmpty() ? "" : " WHERE d.department_name LIKE '%" + search + "%' OR c.college_name LIKE '%" + search + "%'") +
            " ORDER BY c.college_name, d.department_name";
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
