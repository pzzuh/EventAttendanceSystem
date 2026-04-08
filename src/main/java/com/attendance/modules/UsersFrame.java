package com.attendance.modules;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.PasswordUtil;
import com.attendance.util.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class UsersFrame extends JFrame {
    private JTextField txtFirst, txtMiddle, txtLast, txtUsername, txtSearch;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole, cmbStatus;
    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedId = -1;
    private Runnable onClose;

    public UsersFrame() {
        setTitle("AttendX — Users");
        setSize(960, 660);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        loadTable();
    }

    private void buildUI() {
        JPanel root = UITheme.moduleRoot();

        // ── Top bar ───────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        JLabel title = new JLabel("◈  Users");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);
        txtSearch = UITheme.styledField();
        txtSearch.setPreferredSize(new Dimension(220, 34));
        JButton btnSearch = UITheme.primaryButton("Search");
        btnSearch.addActionListener(e -> loadTable());
        txtSearch.addActionListener(e -> loadTable());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBox.setOpaque(false);
        searchBox.add(new JLabel("Search:") {{ setFont(UITheme.FONT_SMALL); setForeground(UITheme.TEXT_MUTED); }});
        searchBox.add(txtSearch); searchBox.add(btnSearch);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(searchBox, BorderLayout.EAST);

        // ── Form card ─────────────────────────────────────────────────────
        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 8, 5, 8);

        // Row labels
        int row = 0;
        g.gridx=0; g.gridy=row; g.gridwidth=1; g.weightx=0.25; form.add(UITheme.fieldLabel("FIRST NAME *"), g);
        g.gridx=1; form.add(UITheme.fieldLabel("MIDDLE NAME"), g);
        g.gridx=2; g.gridwidth=2; form.add(UITheme.fieldLabel("LAST NAME *"), g); row++;

        g.gridwidth=1; g.gridy=row;
        g.gridx=0; txtFirst=UITheme.styledField(); form.add(txtFirst,g);
        g.gridx=1; txtMiddle=UITheme.styledField(); form.add(txtMiddle,g);
        g.gridx=2; g.gridwidth=2; txtLast=UITheme.styledField(); form.add(txtLast,g); row++;

        g.gridwidth=2; g.gridx=0; g.gridy=row; form.add(UITheme.fieldLabel("USERNAME *"),g);
        g.gridx=2; form.add(UITheme.fieldLabel("PASSWORD (leave blank to keep)"),g); row++;

        g.gridy=row;
        g.gridx=0; txtUsername=UITheme.styledField(); form.add(txtUsername,g);
        g.gridx=2; txtPassword=UITheme.styledPasswordField(); form.add(txtPassword,g); row++;

        g.gridwidth=2; g.gridx=0; g.gridy=row; form.add(UITheme.fieldLabel("ROLE *"),g);
        g.gridx=2; form.add(UITheme.fieldLabel("STATUS"),g); row++;

        g.gridy=row;
        g.gridx=0; cmbRole=UITheme.styledCombo(new String[]{"Select Role","Super Admin","Program Head","Department President","Department Secretary","Department Treasurer"});
        form.add(cmbRole,g);
        g.gridx=2; cmbStatus=UITheme.styledCombo(new String[]{"Active","Inactive"});
        form.add(cmbStatus,g); row++;

        g.gridx=0; g.gridy=row; g.gridwidth=4;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton btnSave   = UITheme.primaryButton("Add User");
        JButton btnUpdate = UITheme.successButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear  = UITheme.outlineButton("Clear");
        btnSave.addActionListener(e -> doSave());
        btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> clearForm());
        btnRow.add(btnSave); btnRow.add(btnUpdate); btnRow.add(btnDelete); btnRow.add(btnClear);
        form.add(btnRow, g);

        // ── Table ─────────────────────────────────────────────────────────
        String[] cols = {"ID","First Name","Middle Name","Last Name","Username","Role","Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                boolean active = "Active".equals(v);
                l.setText(active ? "● Active" : "○ Inactive");
                l.setForeground(sel ? UITheme.ACCENT_DARK : (active ? UITheme.SUCCESS : UITheme.DANGER));
                l.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
                return l;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new LineBorder(UITheme.BORDER_COLOR, 1, true));
        tableCard.add(UITheme.styledScrollPane(table));

        root.add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(form, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void doSave() {
        String first = txtFirst.getText().trim(), last = txtLast.getText().trim();
        String uname = txtUsername.getText().trim(), pw = new String(txtPassword.getPassword());
        String role  = (String) cmbRole.getSelectedItem();
        if (first.isEmpty()||last.isEmpty()||uname.isEmpty()||pw.isEmpty()||"Select Role".equals(role)) {
            JOptionPane.showMessageDialog(this,"Fill all required fields.","Validation",JOptionPane.WARNING_MESSAGE); return;
        }
        PasswordUtil.PasswordStrengthResult s = PasswordUtil.checkPasswordStrength(pw);
        if (!s.valid()) { JOptionPane.showMessageDialog(this,s.message(),"Password",JOptionPane.WARNING_MESSAGE); return; }
        try (Connection c = DatabaseConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(first_name,middle_name,last_name,username,password,role,status) VALUES(?,?,?,?,?,?,?)");
            ps.setString(1,first); ps.setString(2,txtMiddle.getText().trim());
            ps.setString(3,last); ps.setString(4,uname);
            ps.setString(5,PasswordUtil.hashPassword(pw)); ps.setString(6,role); ps.setString(7,"Active");
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"User added successfully.");
            clearForm(); loadTable();
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
    }

    private void doUpdate() {
        if (selectedId<0) { JOptionPane.showMessageDialog(this,"Select a user first."); return; }
        String first=txtFirst.getText().trim(), last=txtLast.getText().trim(), uname=txtUsername.getText().trim();
        if (first.isEmpty()||last.isEmpty()||uname.isEmpty()) { JOptionPane.showMessageDialog(this,"Required fields missing."); return; }
        try (Connection c = DatabaseConnection.getConnection()) {
            String pw = new String(txtPassword.getPassword());
            if (!pw.isEmpty()) {
                PasswordUtil.PasswordStrengthResult s = PasswordUtil.checkPasswordStrength(pw);
                if (!s.valid()) { JOptionPane.showMessageDialog(this,s.message()); return; }
                PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET first_name=?,middle_name=?,last_name=?,username=?,password=?,role=?,status=? WHERE id=?");
                ps.setString(1,first); ps.setString(2,txtMiddle.getText().trim()); ps.setString(3,last);
                ps.setString(4,uname); ps.setString(5,PasswordUtil.hashPassword(pw));
                ps.setString(6,(String)cmbRole.getSelectedItem()); ps.setString(7,(String)cmbStatus.getSelectedItem()); ps.setInt(8,selectedId);
                ps.executeUpdate();
            } else {
                PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET first_name=?,middle_name=?,last_name=?,username=?,role=?,status=? WHERE id=?");
                ps.setString(1,first); ps.setString(2,txtMiddle.getText().trim()); ps.setString(3,last);
                ps.setString(4,uname); ps.setString(5,(String)cmbRole.getSelectedItem());
                ps.setString(6,(String)cmbStatus.getSelectedItem()); ps.setInt(7,selectedId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this,"User updated."); clearForm(); loadTable();
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
    }

    private void doDelete() {
        if (selectedId<0) { JOptionPane.showMessageDialog(this,"Select a user first."); return; }
        if (JOptionPane.showConfirmDialog(this,"Delete this user?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try (Connection c = DatabaseConnection.getConnection()) {
            c.createStatement().executeUpdate("DELETE FROM users WHERE id="+selectedId);
            JOptionPane.showMessageDialog(this,"User deleted."); clearForm(); loadTable();
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
    }

    private void loadSelected() {
        int row = table.getSelectedRow(); if (row<0) return;
        selectedId = (int) tableModel.getValueAt(row,0);
        txtFirst.setText((String)tableModel.getValueAt(row,1));
        txtMiddle.setText(tableModel.getValueAt(row,2)==null?"":(String)tableModel.getValueAt(row,2));
        txtLast.setText((String)tableModel.getValueAt(row,3));
        txtUsername.setText((String)tableModel.getValueAt(row,4));
        txtPassword.setText("");
        cmbRole.setSelectedItem(tableModel.getValueAt(row,5));
        cmbStatus.setSelectedItem(tableModel.getValueAt(row,6));
    }

    private void clearForm() {
        selectedId=-1; txtFirst.setText(""); txtMiddle.setText(""); txtLast.setText("");
        txtUsername.setText(""); txtPassword.setText(""); cmbRole.setSelectedIndex(0); cmbStatus.setSelectedIndex(0);
        table.clearSelection();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        String search = txtSearch.getText().trim();
        String sql = "SELECT id,first_name,middle_name,last_name,username,role,status FROM users" +
            (search.isEmpty() ? "" : " WHERE first_name LIKE '%"+search+"%' OR last_name LIKE '%"+search+"%' OR username LIKE '%"+search+"%'") +
            " ORDER BY first_name";
        try (Connection c = DatabaseConnection.getConnection(); ResultSet rs = c.createStatement().executeQuery(sql)) {
            while (rs.next()) tableModel.addRow(new Object[]{
                rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)});
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
