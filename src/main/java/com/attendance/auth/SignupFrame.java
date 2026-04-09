package com.attendance.auth;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.PasswordUtil;
import com.attendance.util.PasswordUtil.PasswordStrengthResult;
import com.attendance.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class SignupFrame extends JFrame {
    private JTextField txtFirst, txtMiddle, txtLast, txtEmail, txtUsername, txtSecA;
    private JPasswordField txtPassword, txtConfirm;
    private JComboBox<String> cmbRole, cmbSecQ;
    private JLabel lblStrength, lblError;

    public SignupFrame() {
        setTitle("Attendance System — Register");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 640);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_MAIN);

        // ── Header bar ───────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.SIDEBAR_BG);
        header.setPreferredSize(new Dimension(0, 56));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        JLabel logo = new JLabel("◈  Attendance System");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logo.setForeground(UITheme.ACCENT);
        JLabel subtitle = new JLabel("Create your account");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.SIDEBAR_TEXT);
        header.add(logo, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);

        // ── Two-column form ──────────────────────────────────────────────
        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setBackground(UITheme.BG_MAIN);
        formWrapper.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel card = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 8, 5, 8);

        // ── Section 1 ────────────────────────────────────────────────────
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4; g.weightx = 1;
        card.add(sectionDivider("Personal Details"), g);

        g.gridwidth = 1; g.weightx = 0.25;
        label(card, g, 0, 1, "FIRST NAME *");
        label(card, g, 1, 1, "MIDDLE NAME");
        label(card, g, 2, 1, "LAST NAME *"); g.gridwidth = 1;

        g.gridy = 2;
        g.gridx = 0; txtFirst = field(card, g);
        g.gridx = 1; txtMiddle = field(card, g);
        g.gridx = 2; g.gridwidth = 2; txtLast = field(card, g); g.gridwidth = 1;

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        card.add(UITheme.fieldLabel("EMAIL ADDRESS *"), g);
        g.gridx = 2; g.gridwidth = 2;
        card.add(UITheme.fieldLabel("USERNAME *"), g);

        g.gridy = 4; g.gridx = 0; g.gridwidth = 2;
        txtEmail = UITheme.styledField(); card.add(txtEmail, g);
        g.gridx = 2; g.gridwidth = 2;
        txtUsername = UITheme.styledField(); card.add(txtUsername, g);

        // ── Section 2 ────────────────────────────────────────────────────
        g.gridx = 0; g.gridy = 5; g.gridwidth = 4;
        card.add(sectionDivider("Security"), g);

        g.gridwidth = 2;
        g.gridx = 0; g.gridy = 6; card.add(UITheme.fieldLabel("PASSWORD *"), g);
        g.gridx = 2; card.add(UITheme.fieldLabel("CONFIRM PASSWORD *"), g);
        g.gridy = 7;
        g.gridx = 0; txtPassword = UITheme.styledPasswordField(); card.add(txtPassword, g);
        g.gridx = 2; txtConfirm = UITheme.styledPasswordField(); card.add(txtConfirm, g);

        g.gridx = 0; g.gridy = 8; g.gridwidth = 4;
        lblStrength = new JLabel(" ");
        lblStrength.setFont(UITheme.FONT_SMALL); card.add(lblStrength, g);

        // ── Section 3 ────────────────────────────────────────────────────
        g.gridy = 9; card.add(sectionDivider("Role & Recovery"), g);

        g.gridwidth = 2;
        g.gridx = 0; g.gridy = 10; card.add(UITheme.fieldLabel("ROLE *"), g);
        g.gridx = 2; card.add(UITheme.fieldLabel("SECURITY QUESTION *"), g);
        g.gridy = 11;
        g.gridx = 0;
        cmbRole = UITheme.styledCombo(new String[]{"Select Role","Super Admin","Dean","Program Head"});
        card.add(cmbRole, g);
        g.gridx = 2;
        cmbSecQ = UITheme.styledCombo(new String[]{"Select question",
            "What was the name of your first pet?", "What is your mother's maiden name?",
            "What city were you born in?", "Name of your elementary school?", "Favorite book?"});
        card.add(cmbSecQ, g);

        g.gridx = 0; g.gridy = 12; g.gridwidth = 4;
        card.add(UITheme.fieldLabel("SECURITY ANSWER *"), g);
        g.gridy = 13;
        txtSecA = UITheme.styledField(); card.add(txtSecA, g);

        // Error & buttons
        g.gridy = 14;
        lblError = new JLabel(" ");
        lblError.setFont(UITheme.FONT_SMALL); lblError.setForeground(UITheme.DANGER);
        card.add(lblError, g);

        g.gridy = 15;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton btnBack = UITheme.outlineButton("← Back to Login");
        btnBack.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        JButton btnSave = UITheme.primaryButton("Create Account →");
        btnSave.addActionListener(e -> doRegister());
        btnRow.add(btnBack); btnRow.add(btnSave);
        card.add(btnRow, g);

        txtPassword.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String pw = new String(txtPassword.getPassword());
                if (pw.isEmpty()) { lblStrength.setText(" "); return; }
                PasswordStrengthResult r = PasswordUtil.checkPasswordStrength(pw);
                lblStrength.setForeground(r.valid() ? UITheme.SUCCESS : UITheme.DANGER);
                lblStrength.setText((r.valid() ? "✔ " : "✘ ") + r.message());
            }
        });

        GridBagConstraints gw = new GridBagConstraints();
        gw.fill = GridBagConstraints.BOTH; gw.weightx = 1; gw.weighty = 1;
        formWrapper.add(new JScrollPane(card) {{
            setBorder(null); getVerticalScrollBar().setUnitIncrement(16);
        }}, gw);

        root.add(header, BorderLayout.NORTH);
        root.add(formWrapper, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel sectionDivider(String text) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(UITheme.ACCENT);
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.ACCENT_LIGHT);
        p.add(lbl, BorderLayout.WEST);
        p.add(sep, BorderLayout.CENTER);
        return p;
    }

    private void label(JPanel p, GridBagConstraints g, int x, int y, String t) {
        g.gridx = x; g.gridy = y; g.gridwidth = 1;
        p.add(UITheme.fieldLabel(t), g);
    }
    private JTextField field(JPanel p, GridBagConstraints g) {
        JTextField f = UITheme.styledField(); p.add(f, g); return f;
    }

    private void doRegister() {
        String fn = txtFirst.getText().trim(), ln = txtLast.getText().trim();
        String em = txtEmail.getText().trim(), un = txtUsername.getText().trim();
        String pw = new String(txtPassword.getPassword()), cf = new String(txtConfirm.getPassword());
        String rl = (String) cmbRole.getSelectedItem(), sq = (String) cmbSecQ.getSelectedItem();
        String sa = txtSecA.getText().trim();

        if (fn.isEmpty()||ln.isEmpty()||em.isEmpty()||un.isEmpty()||pw.isEmpty()||cf.isEmpty()||sa.isEmpty()) {
            lblError.setText("All required (*) fields must be filled."); return; }
        if ("Select Role".equals(rl)) { lblError.setText("Please select a role."); return; }
        if ("Select question".equals(sq)) { lblError.setText("Please select a security question."); return; }
        if (!PasswordUtil.isValidEmail(em)) { lblError.setText("Invalid email address."); return; }
        PasswordStrengthResult s = PasswordUtil.checkPasswordStrength(pw);
        if (!s.valid()) { lblError.setText(s.message()); return; }
        if (!pw.equals(cf)) { lblError.setText("Passwords do not match."); return; }

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement chk = conn.prepareStatement("SELECT id FROM users WHERE username=? OR email=?");
            chk.setString(1, un); chk.setString(2, em);
            if (chk.executeQuery().next()) { lblError.setText("Username or email already in use."); return; }
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users(first_name,middle_name,last_name,email,username,password,role,security_question,security_answer) VALUES(?,?,?,?,?,?,?,?,?)");
            ps.setString(1,fn); ps.setString(2,txtMiddle.getText().trim().isEmpty()?null:txtMiddle.getText().trim());
            ps.setString(3,ln); ps.setString(4,em); ps.setString(5,un);
            ps.setString(6,PasswordUtil.hashPassword(pw)); ps.setString(7,rl);
            ps.setString(8,sq); ps.setString(9,PasswordUtil.hashPassword(sa.toLowerCase()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Account created! You can now sign in.","Success",JOptionPane.INFORMATION_MESSAGE);
            dispose(); new LoginFrame().setVisible(true);
        } catch (SQLException ex) { lblError.setText("Database error."); ex.printStackTrace(); }
    }
}
