package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class CollegesFrame extends JFrame {
    private JTextField txtCollegeName, txtDeanFirst, txtDeanMiddle, txtDeanLast, txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;

    public CollegesFrame() {
        setTitle("AttendX — Colleges");
        setSize(1020, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();
        JLabel title = new JLabel("◈  Colleges");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 8, 5, 8);
        g.weightx = 0.33;

        int row = 0;
        // College name
        g.gridx = 0; g.gridy = row; g.gridwidth = 3; form.add(UITheme.fieldLabel("COLLEGE NAME *"), g); row++;
        g.gridy = row; txtCollegeName = UITheme.styledField(); form.add(txtCollegeName, g); row++;

        // Dean info header
        g.gridy = row;
        JLabel deanHdr = new JLabel("College Dean (automatically creates user account)");
        deanHdr.setFont(UITheme.FONT_SMALL); deanHdr.setForeground(UITheme.TEXT_MUTED);
        form.add(deanHdr, g); row++;

        // Dean names
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = row; form.add(UITheme.fieldLabel("FIRST NAME *"), g);
        g.gridx = 1; form.add(UITheme.fieldLabel("MIDDLE NAME"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("LAST NAME *"), g); row++;

        g.gridy = row;
        g.gridx = 0; txtDeanFirst = UITheme.styledField(); form.add(txtDeanFirst, g);
        g.gridx = 1; txtDeanMiddle = UITheme.styledField(); form.add(txtDeanMiddle, g);
        g.gridx = 2; txtDeanLast = UITheme.styledField(); form.add(txtDeanLast, g); row++;

        // Buttons
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); btnRow.setOpaque(false);
        JButton btnSave = UITheme.primaryButton("Add College");
        JButton btnUpdate = UITheme.successButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear = UITheme.outlineButton("Clear");
        btnSave.addActionListener(e -> doSave());
        btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow, g);

        // Search bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(new Color(249, 250, 251));
        filterBar.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        txtSearch = UITheme.styledField(); txtSearch.setPreferredSize(new Dimension(220, 32));
        JButton btnSearch = UITheme.primaryButton("Search");
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        txtSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }});
        filterBar.add(txtSearch); filterBar.add(btnSearch);

        // Table
        String[] cols = {"ID", "College Name", "Dean", "Status"};
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

    private void doSave() {
        if (!validateForm()) return;
        try {
            // Insert college
            String collegeSql = "INSERT INTO colleges (college_name, dean_first_name, dean_middle_name, dean_last_name) VALUES (?, ?, ?, ?)";
            PreparedStatement colPst = DatabaseConnection.getConnection().prepareStatement(collegeSql, Statement.RETURN_GENERATED_KEYS);
            colPst.setString(1, txtCollegeName.getText().trim());
            colPst.setString(2, txtDeanFirst.getText().trim());
            colPst.setString(3, txtDeanMiddle.getText().trim());
            colPst.setString(4, txtDeanLast.getText().trim());
            colPst.executeUpdate();
            ResultSet rs = colPst.getGeneratedKeys();
            rs.next();
            int collegeId = rs.getInt(1);
            rs.close();
            colPst.close();

            // Create dean user
            String username = txtDeanFirst.getText().toLowerCase() + "." + txtDeanLast.getText().toLowerCase();
            String userSql = "INSERT INTO users (first_name, middle_name, last_name, username, password, role, status) VALUES (?, ?, ?, ?, ?, 'Dean', 'Active')";
            PreparedStatement userPst = DatabaseConnection.getConnection().prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userPst.setString(1, txtDeanFirst.getText().trim());
            userPst.setString(2, txtDeanMiddle.getText().trim());
            userPst.setString(3, txtDeanLast.getText().trim());
            userPst.setString(4, username);
            userPst.setString(5, com.attendance.util.PasswordUtil.hashPassword("123456"));
            userPst.executeUpdate();
            rs = userPst.getGeneratedKeys();
            rs.next();
            int userId = rs.getInt(1);
            rs.close();
            userPst.close();

            // Update college with dean user id
            String updateSql = "UPDATE colleges SET dean_user_id = ? WHERE id = ?";
            PreparedStatement updatePst = DatabaseConnection.getConnection().prepareStatement(updateSql);
            updatePst.setInt(1, userId);
            updatePst.setInt(2, collegeId);
            updatePst.executeUpdate();
            updatePst.close();

            JOptionPane.showMessageDialog(this, "College added and dean account created successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doUpdate() {
        if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a college to update"); return; }
        if (!validateForm()) return;
        try {
            String sql = "UPDATE colleges SET college_name=?, dean_first_name=?, dean_middle_name=?, dean_last_name=? WHERE id=?";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setString(1, txtCollegeName.getText().trim());
            pst.setString(2, txtDeanFirst.getText().trim());
            pst.setString(3, txtDeanMiddle.getText().trim());
            pst.setString(4, txtDeanLast.getText().trim());
            pst.setInt(5, selectedId);
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "College updated successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDelete() {
        if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a college to delete"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this college?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            String sql = "DELETE FROM colleges WHERE id=?";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setInt(1, selectedId);
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "College deleted successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        try {
            String sql = "SELECT c.id, c.college_name, COALESCE(NULLIF(CONCAT(c.dean_first_name, ' ', c.dean_last_name), ' '), CONCAT(u.first_name, ' ', u.last_name)) AS dean, COALESCE(u.status, 'N/A') AS status " +
                "FROM colleges c LEFT JOIN users u ON c.dean_user_id = u.id";
            if (!search.isEmpty()) {
                sql += " WHERE CAST(c.id AS CHAR) LIKE ? OR c.college_name LIKE ? OR c.dean_first_name LIKE ? OR c.dean_last_name LIKE ? OR CONCAT(c.dean_first_name, ' ', c.dean_last_name) LIKE ? " +
                       "OR u.first_name LIKE ? OR u.last_name LIKE ? OR CONCAT(u.first_name, ' ', u.last_name) LIKE ? OR u.username LIKE ? OR u.status LIKE ?";
            }
            sql += " ORDER BY c.college_name ASC";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            if (!search.isEmpty()) {
                String pattern = "%" + search + "%";
                pst.setString(1, pattern);
                pst.setString(2, pattern);
                pst.setString(3, pattern);
                pst.setString(4, pattern);
                pst.setString(5, pattern);
                pst.setString(6, pattern);
                pst.setString(7, pattern);
                pst.setString(8, pattern);
                pst.setString(9, pattern);
                pst.setString(10, pattern);
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("college_name"), rs.getString("dean"),
                    rs.getString("status")
                });
            }
            rs.close();
            pst.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading colleges: " + e.getMessage());
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { clearForm(); return; }
        selectedId = (int) tableModel.getValueAt(row, 0);
        txtCollegeName.setText((String) tableModel.getValueAt(row, 1));
        String dean = (String) tableModel.getValueAt(row, 2);
        if (dean != null && !dean.isEmpty()) {
            String[] deanParts = dean.split(" ");
            txtDeanFirst.setText(deanParts.length > 0 ? deanParts[0] : "");
            txtDeanLast.setText(deanParts.length > 1 ? deanParts[1] : "");
        }
    }

    private void clearForm() {
        txtCollegeName.setText("");
        txtDeanFirst.setText("");
        txtDeanMiddle.setText("");
        txtDeanLast.setText("");
        selectedId = -1;
        table.clearSelection();
    }

    private boolean validateForm() {
        if (txtCollegeName.getText().trim().isEmpty() || txtDeanFirst.getText().trim().isEmpty() || txtDeanLast.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields");
            return false;
        }
        return true;
    }
}
