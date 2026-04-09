package com.attendance.auth;


import com.attendance.db.DatabaseConnection;
import com.attendance.util.PasswordUtil;
import com.attendance.util.Session;
import com.attendance.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;

    public LoginFrame() {
        setTitle("Attendance System — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 580);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());

        // ── LEFT: dark decorative panel ──────────────────────────────────
        JPanel left = new JPanel(new GridBagLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.SIDEBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Teal glow circles
                g2.setColor(new Color(20, 184, 166, 30));
                g2.fillOval(-80, -80, 340, 340);
                g2.setColor(new Color(20, 184, 166, 18));
                g2.fillOval(getWidth() - 200, getHeight() - 180, 320, 320);
                g2.setColor(new Color(20, 184, 166, 12));
                g2.fillOval(30, getHeight() - 130, 200, 200);
                g2.dispose();
            }
        };
        left.setPreferredSize(new Dimension(420, 0));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        JLabel appIcon = new JLabel("◈", SwingConstants.CENTER);
        appIcon.setFont(new Font("Segoe UI", Font.BOLD, 52));
        appIcon.setForeground(UITheme.ACCENT);
        appIcon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel appName = new JLabel("Attendance System");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 36));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Smart Attendance Management");
        tagline.setFont(UITheme.FONT_BODY);
        tagline.setForeground(UITheme.SIDEBAR_TEXT);
        tagline.setAlignmentX(CENTER_ALIGNMENT);

        brand.add(appIcon);
        brand.add(Box.createVerticalStrut(10));
        brand.add(appName);
        brand.add(Box.createVerticalStrut(6));
        brand.add(tagline);
        brand.add(Box.createVerticalStrut(36));

        String[] pills = { "◉  Barcode & ID Scanning", "◉  Real-time Reports", "◉  Multi-College Support" };
        for (String p : pills) {
            JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            pill.setOpaque(false);
            pill.setMaximumSize(new Dimension(300, 32));
            JLabel lbl = new JLabel(p);
            lbl.setFont(UITheme.FONT_SMALL);
            lbl.setForeground(new Color(94, 234, 212));
            pill.add(lbl);
            brand.add(pill);
            brand.add(Box.createVerticalStrut(6));
        }
        left.add(brand);

        // ── RIGHT: login form ─────────────────────────────────────────────
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(UITheme.BG_MAIN);

        JPanel card = UITheme.cardPanel(new GridBagLayout());
        card.setPreferredSize(new Dimension(360, 420));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1; g.gridwidth = 1;
        g.insets = new Insets(4, 0, 4, 0);

        g.gridx = 0; g.gridy = 0;
        JLabel h1 = new JLabel("Login");
        h1.setFont(new Font("Segoe UI", Font.BOLD, 24));
        h1.setForeground(UITheme.TEXT_PRIMARY);
        card.add(h1, g);

        g.gridy = 1;
        JLabel h2 = new JLabel("Enter your credentials to access the system");
        h2.setFont(UITheme.FONT_SMALL);
        h2.setForeground(UITheme.TEXT_MUTED);
        card.add(h2, g);

        // Divider
        g.gridy = 2; g.insets = new Insets(12, 0, 12, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER_COLOR);
        card.add(sep, g);

        g.insets = new Insets(4, 0, 2, 0);
        g.gridy = 3; card.add(UITheme.fieldLabel("USERNAME"), g);
        g.gridy = 4; g.insets = new Insets(2, 0, 10, 0);
        txtUsername = UITheme.styledField();
        txtUsername.setPreferredSize(new Dimension(320, 40));
        card.add(txtUsername, g);

        g.insets = new Insets(4, 0, 2, 0);
        g.gridy = 5; card.add(UITheme.fieldLabel("PASSWORD"), g);
        g.gridy = 6; g.insets = new Insets(2, 0, 4, 0);
        txtPassword = UITheme.styledPasswordField();
        txtPassword.setPreferredSize(new Dimension(320, 40));
        card.add(txtPassword, g);

        g.gridy = 7; g.insets = new Insets(0, 0, 6, 0);
        lblError = new JLabel(" ");
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.DANGER);
        card.add(lblError, g);

        g.gridy = 8; g.insets = new Insets(0, 0, 16, 0);
        JButton btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(UITheme.ACCENT); btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false); btnLogin.setBorderPainted(false); btnLogin.setOpaque(true);
        btnLogin.setPreferredSize(new Dimension(320, 44));
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> doLogin());
        card.add(btnLogin, g);

        right.add(card);

        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());

        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void doLogin() {
        String u = txtUsername.getText().trim();
        String p = new String(txtPassword.getPassword());
        if (u.isEmpty() || p.isEmpty()) { lblError.setText("All fields are required."); return; }
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id,first_name,last_name,password,role,status FROM users WHERE username=?");
            ps.setString(1, u); ResultSet rs = ps.executeQuery();
            if (!rs.next() || !PasswordUtil.verifyPassword(p, rs.getString("password"))) {
                lblError.setText("Invalid credentials. Please try again.");
                txtPassword.setText(""); return;
            }
            if ("Inactive".equals(rs.getString("status"))) {
                lblError.setText("Account deactivated. Contact administrator."); return;
            }
            Session.login(rs.getInt("id"), u, rs.getString("role"),
                rs.getString("first_name") + " " + rs.getString("last_name"));
            dispose();
            final com.attendance.dashboard.DashboardFrame dash = new com.attendance.dashboard.DashboardFrame();
            SwingUtilities.invokeLater(new Runnable() { public void run() { dash.setVisible(true); } });
        } catch (SQLException ex) { lblError.setText("Database error."); ex.printStackTrace(); }
    }
}
