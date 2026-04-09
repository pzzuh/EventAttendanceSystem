
package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class UsersFrame extends JFrame {
    private JTextField txtFirstName, txtMiddleName, txtLastName, txtUsername, txtPassword, txtSearch;
    private JComboBox<String> cmbRole, cmbStatus;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;

    public UsersFrame() {
        setTitle("AttendX — Users");
        setSize(1020, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();
        JLabel title = new JLabel("◈  Users");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 8, 5, 8);
        g.weightx = 0.33;

        int row = 0;
        // Names row
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; form.add(UITheme.fieldLabel("FIRST NAME *"), g);
        g.gridx = 1; form.add(UITheme.fieldLabel("MIDDLE NAME"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("LAST NAME *"), g); row++;

        g.gridy = row;
        g.gridx = 0; txtFirstName = UITheme.styledField(); form.add(txtFirstName, g);
        g.gridx = 1; txtMiddleName = UITheme.styledField(); form.add(txtMiddleName, g);
        g.gridx = 2; txtLastName = UITheme.styledField(); form.add(txtLastName, g); row++;

        // Username row
        g.gridy = row; g.gridwidth = 2;
        g.gridx = 0; form.add(UITheme.fieldLabel("USERNAME *"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("PASSWORD *"), g); row++;

        g.gridy = row; g.gridwidth = 1;
        g.gridx = 0; g.gridwidth = 2; txtUsername = UITheme.styledField(); form.add(txtUsername, g);
        g.gridx = 2; g.gridwidth = 1; txtPassword = UITheme.styledField(); form.add(txtPassword, g); row++;

        // Role & Status row
        g.gridy = row; g.gridwidth = 1;
        g.gridx = 0; g.gridwidth = 2; form.add(UITheme.fieldLabel("ROLE *"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("STATUS *"), g); row++;

        g.gridy = row; g.gridwidth = 1;
        g.gridx = 0; g.gridwidth = 2;
        cmbRole = UITheme.styledCombo(new String[]{"Super Admin", "Dean", "Program Head"});
        form.add(cmbRole, g);
        g.gridx = 2; g.gridwidth = 1;
        cmbStatus = UITheme.styledCombo(new String[]{"Active", "Inactive"});
        form.add(cmbStatus, g); row++;

        // Buttons
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); btnRow.setOpaque(false);
        JButton btnSave = UITheme.primaryButton("Add User");
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
        String[] cols = {"ID", "First Name", "Last Name", "Username", "Role", "Status"};
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
            String sql = "INSERT INTO users (first_name, middle_name, last_name, username, password, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setString(1, txtFirstName.getText().trim());
            pst.setString(2, txtMiddleName.getText().trim());
            pst.setString(3, txtLastName.getText().trim());
            pst.setString(4, txtUsername.getText().trim());
            pst.setString(5, txtPassword.getText().trim());
            pst.setString(6, (String) cmbRole.getSelectedItem());
            pst.setString(7, (String) cmbStatus.getSelectedItem());
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "User added successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doUpdate() {
        if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a user to update"); return; }
        if (!validateForm()) return;
        try {
            String sql = "UPDATE users SET first_name=?, middle_name=?, last_name=?, username=?, password=?, role=?, status=? WHERE id=?";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setString(1, txtFirstName.getText().trim());
            pst.setString(2, txtMiddleName.getText().trim());
            pst.setString(3, txtLastName.getText().trim());
            pst.setString(4, txtUsername.getText().trim());
            pst.setString(5, txtPassword.getText().trim());
            pst.setString(6, (String) cmbRole.getSelectedItem());
            pst.setString(7, (String) cmbStatus.getSelectedItem());
            pst.setInt(8, selectedId);
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "User updated successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDelete() {
        if (selectedId == -1) { JOptionPane.showMessageDialog(this, "Select a user to delete"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this user?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            String sql = "DELETE FROM users WHERE id=?";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            pst.setInt(1, selectedId);
            pst.executeUpdate();
            pst.close();
            JOptionPane.showMessageDialog(this, "User deleted successfully");
            clearForm();
            loadTable("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        try {
            String sql = "SELECT * FROM users";
            if (!search.isEmpty()) {
                sql += " WHERE CAST(id AS CHAR) LIKE ? OR first_name LIKE ? OR last_name LIKE ? OR username LIKE ? OR role LIKE ? OR status LIKE ?";
            }
            sql += " ORDER BY first_name ASC";
            PreparedStatement pst = DatabaseConnection.getConnection().prepareStatement(sql);
            if (!search.isEmpty()) {
                String pattern = "%" + search + "%";
                pst.setString(1, pattern);
                pst.setString(2, pattern);
                pst.setString(3, pattern);
                pst.setString(4, pattern);
                pst.setString(5, pattern);
                pst.setString(6, pattern);
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("username"), rs.getString("role"), rs.getString("status")
                });
            }
            rs.close();
            pst.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { clearForm(); return; }
        selectedId = (int) tableModel.getValueAt(row, 0);
        txtFirstName.setText((String) tableModel.getValueAt(row, 1));
        txtLastName.setText((String) tableModel.getValueAt(row, 2));
        txtUsername.setText((String) tableModel.getValueAt(row, 3));
        cmbRole.setSelectedItem((String) tableModel.getValueAt(row, 4));
        cmbStatus.setSelectedItem((String) tableModel.getValueAt(row, 5));
    }

    private void clearForm() {
        txtFirstName.setText("");
        txtMiddleName.setText("");
        txtLastName.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        cmbRole.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        selectedId = -1;
        table.clearSelection();
    }

    private boolean validateForm() {
        if (txtFirstName.getText().trim().isEmpty() || txtLastName.getText().trim().isEmpty() ||
            txtUsername.getText().trim().isEmpty() || txtPassword.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields");
            return false;
        }
        return true;
    }
}
