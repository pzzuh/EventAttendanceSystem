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

public class CollegesFrame extends JFrame {

    private JTextField txtCollegeName, txtDeanFirst, txtDeanMiddle, txtDeanLast, txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;
    private Runnable onClose;

    public CollegesFrame() {
        this(null);
    }

    public CollegesFrame(Runnable onClose) {
        this.onClose = onClose;
        setTitle("AttendX — Colleges");
        setSize(940, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadTable("");
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                if (onClose != null) onClose.run();
            }
        });
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();

        // ── Title ─────────────────────────────────────────────────────────
        JLabel title = new JLabel("◈  Colleges");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        // ── Form card ─────────────────────────────────────────────────────
        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 8, 5, 8);
        g.weightx = 0.33;

        int row = 0;
        // College name — full width
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        form.add(UITheme.fieldLabel("COLLEGE NAME *"), g); row++;
        g.gridy = row;
        txtCollegeName = UITheme.styledField();
        form.add(txtCollegeName, g); row++;

        // Dean section label
        g.gridy = row;
        JLabel deanHdr = new JLabel("Dean Information  (automatically creates a user account)");
        deanHdr.setFont(UITheme.FONT_SMALL); deanHdr.setForeground(UITheme.TEXT_MUTED);
        form.add(deanHdr, g); row++;

        // Dean name fields
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = row; form.add(UITheme.fieldLabel("FIRST NAME *"), g);
        g.gridx = 1; form.add(UITheme.fieldLabel("MIDDLE NAME"), g);
        g.gridx = 2; form.add(UITheme.fieldLabel("LAST NAME *"), g); row++;

        g.gridy = row;
        g.gridx = 0; txtDeanFirst  = UITheme.styledField(); form.add(txtDeanFirst, g);
        g.gridx = 1; txtDeanMiddle = UITheme.styledField(); form.add(txtDeanMiddle, g);
        g.gridx = 2; txtDeanLast   = UITheme.styledField(); form.add(txtDeanLast, g); row++;

        // Buttons
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton btnSave   = UITheme.primaryButton("Add College");
        JButton btnUpdate = UITheme.successButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear  = UITheme.outlineButton("Clear");
        btnSave.addActionListener(e -> doSave());
        btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow, g);

        // ── Search + Table ────────────────────────────────────────────────
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(new Color(249, 250, 251));
        filterBar.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        txtSearch = UITheme.styledField();
        txtSearch.setPreferredSize(new Dimension(260, 32));
        JButton btnSearch = UITheme.primaryButton("Search");
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        txtSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        filterBar.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); }});
        filterBar.add(txtSearch);
        filterBar.add(btnSearch);

        String[] cols = {"ID", "College Name", "Dean First", "Dean Middle", "Dean Last"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));
        tableCard.add(filterBar, BorderLayout.NORTH);
        tableCard.add(UITheme.styledScrollPane(table), BorderLayout.CENTER);

        root.add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void doSave() {
        String name   = txtCollegeName.getText().trim();
        String dFirst = txtDeanFirst.getText().trim();
        String dLast  = txtDeanLast.getText().trim();
        if (name.isEmpty() || dFirst.isEmpty() || dLast.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all required fields."); return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            String uname = (dFirst.toLowerCase().charAt(0) + dLast.toLowerCase()).replaceAll("\\s+", "");
            PreparedStatement uPs = conn.prepareStatement(
                "INSERT INTO users (first_name,middle_name,last_name,username,password,role) VALUES (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            uPs.setString(1, dFirst); uPs.setString(2, txtDeanMiddle.getText().trim());
            uPs.setString(3, dLast);  uPs.setString(4, uname);
            uPs.setString(5, PasswordUtil.hashPassword("Dean@1234")); uPs.setString(6, "Super Admin");
            uPs.executeUpdate();
            ResultSet keys = uPs.getGeneratedKeys();
            int deanId = keys.next() ? keys.getInt(1) : -1;

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO colleges (college_name,dean_first_name,dean_middle_name,dean_last_name,dean_user_id) VALUES (?,?,?,?,?)");
            ps.setString(1, name); ps.setString(2, dFirst);
            ps.setString(3, txtDeanMiddle.getText().trim()); ps.setString(4, dLast); ps.setInt(5, deanId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "College saved!\nDean account: " + uname + " / Dean@1234");
            clearForm(); loadTable("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doUpdate() {
        if (selectedId < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
        String name = txtCollegeName.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "College name required."); return; }
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE colleges SET college_name=? WHERE id=?");
            ps.setString(1, name); ps.setInt(2, selectedId); ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Updated."); clearForm(); loadTable("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDelete() {
        if (selectedId < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this college?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.createStatement().executeUpdate("DELETE FROM colleges WHERE id=" + selectedId);
            JOptionPane.showMessageDialog(this, "Deleted."); clearForm(); loadTable("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow(); if (row < 0) return;
        selectedId = (int) tableModel.getValueAt(row, 0);
        txtCollegeName.setText((String) tableModel.getValueAt(row, 1));
        txtDeanFirst.setText(tableModel.getValueAt(row, 2) == null ? "" : (String) tableModel.getValueAt(row, 2));
        txtDeanMiddle.setText(tableModel.getValueAt(row, 3) == null ? "" : (String) tableModel.getValueAt(row, 3));
        txtDeanLast.setText(tableModel.getValueAt(row, 4) == null ? "" : (String) tableModel.getValueAt(row, 4));
    }

    private void clearForm() {
        selectedId = -1; txtCollegeName.setText(""); txtDeanFirst.setText("");
        txtDeanMiddle.setText(""); txtDeanLast.setText(""); table.clearSelection();
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = "SELECT id,college_name,dean_first_name,dean_middle_name,dean_last_name FROM colleges" +
            (search.isEmpty() ? "" : " WHERE college_name LIKE '%" + search + "%'") + " ORDER BY college_name";
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
