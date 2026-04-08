package com.attendance.auth;

import com.attendance.db.DatabaseConnection;
import com.attendance.util.PasswordUtil;
import com.attendance.util.PasswordUtil.PasswordStrengthResult;
import com.attendance.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ForgotPasswordFrame extends JDialog {
    private JTextField txtUsername, txtEmail, txtSecAnswer;
    private JLabel lblSecQ, lblErr1;
    private JPasswordField txtNewPw, txtConfirm;
    private JLabel lblStrength;
    private JPanel stepPanel; private CardLayout cardLayout;
    private int foundId = -1;

    public ForgotPasswordFrame(Frame parent) {
        super(parent, "Reset Password", true);
        setSize(460, 420); setLocationRelativeTo(parent); setResizable(false); buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_MAIN);
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(UITheme.SIDEBAR_BG); hdr.setPreferredSize(new Dimension(0,52));
        hdr.setBorder(BorderFactory.createEmptyBorder(0,22,0,22));
        JLabel t = new JLabel("◈  Reset Password");
        t.setFont(new Font("Segoe UI",Font.BOLD,15)); t.setForeground(UITheme.ACCENT);
        hdr.add(t, BorderLayout.WEST);
        cardLayout = new CardLayout(); stepPanel = new JPanel(cardLayout);
        stepPanel.setBackground(UITheme.BG_MAIN);
        stepPanel.add(step1(), "s1"); stepPanel.add(step2(), "s2"); stepPanel.add(step3(), "s3");
        root.add(hdr, BorderLayout.NORTH); root.add(stepPanel, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel wrap(JPanel card) {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(UITheme.BG_MAIN);
        p.setBorder(BorderFactory.createEmptyBorder(20,30,20,30)); p.add(card); return p;
    }

    private JPanel step1() {
        JPanel c = UITheme.cardPanel(new GridBagLayout());
        c.setPreferredSize(new Dimension(400, 300));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(5,0,5,0);
        g.gridx=0;
        g.gridy=0; JLabel h=new JLabel("Find your account"); h.setFont(new Font("Segoe UI",Font.BOLD,17)); h.setForeground(UITheme.TEXT_PRIMARY); c.add(h,g);
        g.gridy=1; JLabel sub=new JLabel("Enter your username and registered email"); sub.setFont(UITheme.FONT_SMALL); sub.setForeground(UITheme.TEXT_MUTED); c.add(sub,g);
        g.gridy=2; g.insets=new Insets(12,0,3,0); c.add(UITheme.fieldLabel("USERNAME"),g);
        g.gridy=3; g.insets=new Insets(0,0,8,0); txtUsername=UITheme.styledField(); c.add(txtUsername,g);
        g.gridy=4; g.insets=new Insets(4,0,3,0); c.add(UITheme.fieldLabel("EMAIL ADDRESS"),g);
        g.gridy=5; g.insets=new Insets(0,0,8,0); txtEmail=UITheme.styledField(); c.add(txtEmail,g);
        g.gridy=6; lblErr1=new JLabel(" "); lblErr1.setFont(UITheme.FONT_SMALL); lblErr1.setForeground(UITheme.DANGER); c.add(lblErr1,g);
        g.gridy=7; g.insets=new Insets(4,0,0,0);
        JPanel br=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); br.setOpaque(false);
        JButton cancel=UITheme.outlineButton("Cancel"); cancel.addActionListener(e->dispose());
        JButton next=UITheme.primaryButton("Continue →"); next.addActionListener(e->doStep1());
        br.add(cancel); br.add(next); c.add(br,g);
        return wrap(c);
    }

    private JPanel step2() {
        JPanel c = UITheme.cardPanel(new GridBagLayout());
        c.setPreferredSize(new Dimension(400, 280));
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1; g.insets=new Insets(5,0,5,0); g.gridx=0;
        g.gridy=0; JLabel h=new JLabel("Security check"); h.setFont(new Font("Segoe UI",Font.BOLD,17)); h.setForeground(UITheme.TEXT_PRIMARY); c.add(h,g);
        g.gridy=1; lblSecQ=new JLabel(" "); lblSecQ.setFont(new Font("Segoe UI",Font.ITALIC,12)); lblSecQ.setForeground(UITheme.ACCENT); c.add(lblSecQ,g);
        g.gridy=2; g.insets=new Insets(10,0,3,0); c.add(UITheme.fieldLabel("YOUR ANSWER"),g);
        g.gridy=3; g.insets=new Insets(0,0,8,0); txtSecAnswer=UITheme.styledField(); c.add(txtSecAnswer,g);
        final JLabel err2=new JLabel(" "); err2.setFont(UITheme.FONT_SMALL); err2.setForeground(UITheme.DANGER);
        g.gridy=4; g.insets=new Insets(0,0,4,0); c.add(err2,g);
        g.gridy=5; g.insets=new Insets(4,0,0,0);
        JPanel br=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); br.setOpaque(false);
        JButton back=UITheme.outlineButton("← Back"); back.addActionListener(e->cardLayout.show(stepPanel,"s1"));
        JButton next=UITheme.primaryButton("Verify →"); next.addActionListener(e->doStep2(err2));
        br.add(back); br.add(next); c.add(br,g);
        return wrap(c);
    }

    private JPanel step3() {
        JPanel c = UITheme.cardPanel(new GridBagLayout());
        c.setPreferredSize(new Dimension(400, 310));
        GridBagConstraints g = new GridBagConstraints();
        g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1; g.insets=new Insets(5,0,5,0); g.gridx=0;
        g.gridy=0; JLabel h=new JLabel("New password"); h.setFont(new Font("Segoe UI",Font.BOLD,17)); h.setForeground(UITheme.TEXT_PRIMARY); c.add(h,g);
        g.gridy=1; g.insets=new Insets(10,0,3,0); c.add(UITheme.fieldLabel("NEW PASSWORD"),g);
        g.gridy=2; g.insets=new Insets(0,0,4,0); txtNewPw=UITheme.styledPasswordField(); c.add(txtNewPw,g);
        g.gridy=3; lblStrength=new JLabel(" "); lblStrength.setFont(UITheme.FONT_SMALL); c.add(lblStrength,g);
        g.gridy=4; g.insets=new Insets(6,0,3,0); c.add(UITheme.fieldLabel("CONFIRM PASSWORD"),g);
        g.gridy=5; g.insets=new Insets(0,0,8,0); txtConfirm=UITheme.styledPasswordField(); c.add(txtConfirm,g);
        final JLabel err3=new JLabel(" "); err3.setFont(UITheme.FONT_SMALL); err3.setForeground(UITheme.DANGER);
        g.gridy=6; c.add(err3,g);
        g.gridy=7; g.insets=new Insets(4,0,0,0);
        JPanel br=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); br.setOpaque(false);
        JButton reset=UITheme.successButton("Reset Password ✔"); reset.addActionListener(e->doStep3(err3));
        br.add(reset); c.add(br,g);
        txtNewPw.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String pw=new String(txtNewPw.getPassword());
                if(pw.isEmpty()){lblStrength.setText(" ");return;}
                PasswordStrengthResult r=PasswordUtil.checkPasswordStrength(pw);
                lblStrength.setForeground(r.valid()?UITheme.SUCCESS:UITheme.DANGER);
                lblStrength.setText(r.message());
            }
        });
        return wrap(c);
    }

    private void doStep1() {
        String u=txtUsername.getText().trim(), e=txtEmail.getText().trim();
        if(u.isEmpty()||e.isEmpty()){lblErr1.setText("All fields required.");return;}
        if(!PasswordUtil.isValidEmail(e)){lblErr1.setText("Invalid email.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("SELECT id,security_question FROM users WHERE username=? AND email=?");
            ps.setString(1,u); ps.setString(2,e); ResultSet rs=ps.executeQuery();
            if(!rs.next()){lblErr1.setText("No matching account found.");return;}
            foundId=rs.getInt("id"); String q=rs.getString("security_question");
            if(q==null||q.trim().isEmpty()){lblErr1.setText("No security question on file.");return;}
            lblSecQ.setText("<html>"+q+"</html>"); cardLayout.show(stepPanel,"s2");
        } catch(SQLException ex){lblErr1.setText("Database error.");ex.printStackTrace();}
    }
    private void doStep2(JLabel err) {
        String a=txtSecAnswer.getText().trim();
        if(a.isEmpty()){err.setText("Answer required.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("SELECT security_answer FROM users WHERE id=?");
            ps.setInt(1,foundId); ResultSet rs=ps.executeQuery();
            if(!rs.next()||!PasswordUtil.verifyPassword(a.toLowerCase(),rs.getString("security_answer"))){
                err.setText("Incorrect answer.");return;}
            cardLayout.show(stepPanel,"s3");
        } catch(SQLException ex){err.setText("Database error.");ex.printStackTrace();}
    }
    private void doStep3(JLabel err) {
        String pw=new String(txtNewPw.getPassword()), cf=new String(txtConfirm.getPassword());
        if(pw.isEmpty()||cf.isEmpty()){err.setText("All fields required.");return;}
        PasswordStrengthResult s=PasswordUtil.checkPasswordStrength(pw);
        if(!s.valid()){err.setText(s.message());return;}
        if(!pw.equals(cf)){err.setText("Passwords do not match.");return;}
        try(Connection c=DatabaseConnection.getConnection()){
            PreparedStatement ps=c.prepareStatement("UPDATE users SET password=? WHERE id=?");
            ps.setString(1,PasswordUtil.hashPassword(pw)); ps.setInt(2,foundId); ps.executeUpdate();
            JOptionPane.showMessageDialog(this,"Password reset successfully!","Done",JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch(SQLException ex){err.setText("Database error.");ex.printStackTrace();}
    }
}
