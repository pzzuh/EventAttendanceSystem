package com.attendance.util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

public class UITheme {
    // ── Teal/Charcoal palette ──────────────────────────────────────────────
    public static final Color SIDEBAR_BG     = new Color(28, 35, 49);
    public static final Color SIDEBAR_HOVER  = new Color(38, 47, 65);
    public static final Color SIDEBAR_ACTIVE = new Color(20, 184, 166);   // teal-500
    public static final Color SIDEBAR_TEXT   = new Color(156, 163, 175);
    public static final Color SIDEBAR_HEAD   = new Color(17, 24, 39);

    public static final Color BG_MAIN        = new Color(243, 244, 246);
    public static final Color TOPBAR_BG      = new Color(28, 35, 49);
    public static final Color CARD_BG        = Color.WHITE;

    public static final Color ACCENT         = new Color(20, 184, 166);   // teal
    public static final Color ACCENT_DARK    = new Color(13, 148, 136);
    public static final Color ACCENT_LIGHT   = new Color(204, 251, 241);
    public static final Color SUCCESS        = new Color(34, 197, 94);
    public static final Color DANGER         = new Color(239, 68, 68);
    public static final Color WARNING        = new Color(234, 179, 8);
    public static final Color INFO           = new Color(59, 130, 246);

    public static final Color TEXT_PRIMARY   = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY = new Color(75, 85, 99);
    public static final Color TEXT_MUTED     = new Color(156, 163, 175);
    public static final Color BORDER_COLOR   = new Color(229, 231, 235);

    // Legacy aliases
    public static final Color PRIMARY      = ACCENT;
    public static final Color PRIMARY_DARK = ACCENT_DARK;
    public static final Color BG_LIGHT     = BG_MAIN;
    public static final Color CARD_WHITE   = CARD_BG;
    public static final Color TEXT_DARK    = TEXT_PRIMARY;

    // ── Fonts ─────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SIDEBAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.BOLD, 11);

    // ── Buttons ──────────────────────────────────────────────────────────
    public static JButton primaryButton(String text) {
        return makeBtn(text, ACCENT, Color.WHITE);
    }
    public static JButton dangerButton(String text) {
        return makeBtn(text, DANGER, Color.WHITE);
    }
    public static JButton successButton(String text) {
        return makeBtn(text, SUCCESS, Color.WHITE);
    }
    public static JButton warningButton(String text) {
        return makeBtn(text, WARNING, TEXT_PRIMARY);
    }
    public static JButton outlineButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BUTTON); b.setForeground(ACCENT);
        b.setBackground(Color.WHITE); b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(ACCENT, 1, true),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JButton makeBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(FONT_BUTTON); b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Inputs ───────────────────────────────────────────────────────────
    public static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(FONT_BODY); f.setForeground(TEXT_PRIMARY);
        f.setBackground(new Color(249, 250, 251));
        f.setCaretColor(ACCENT);
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        return f;
    }
    public static JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(FONT_BODY); f.setForeground(TEXT_PRIMARY);
        f.setBackground(new Color(249, 250, 251));
        f.setCaretColor(ACCENT);
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        return f;
    }
    public static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(FONT_BODY); c.setBackground(new Color(249, 250, 251));
        c.setForeground(TEXT_PRIMARY);
        return c;
    }
    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL); l.setForeground(TEXT_SECONDARY);
        return l;
    }
    public static Border cardBorder() {
        return new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    // ── Table ────────────────────────────────────────────────────────────
    public static void styleTable(JTable t) {
        t.setFont(FONT_BODY); t.setRowHeight(34);
        t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(CARD_BG); t.setForeground(TEXT_PRIMARY);
        t.setSelectionBackground(ACCENT_LIGHT);
        t.setSelectionForeground(ACCENT_DARK);
        JTableHeader h = t.getTableHeader();
        h.setFont(FONT_LABEL);
        h.setBackground(new Color(249, 250, 251));
        h.setForeground(TEXT_SECONDARY);
        h.setBorder(new MatteBorder(0, 0, 2, 0, BORDER_COLOR));
        h.setReorderingAllowed(false);
        // Alternating row renderer
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
    }
    public static JScrollPane styledScrollPane(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(BORDER_COLOR, 1));
        sp.getViewport().setBackground(CARD_BG);
        return sp;
    }

    // ── Module frame wrapper ──────────────────────────────────────────────
    /** Returns a standard content panel used in every module frame */
    public static JPanel moduleRoot() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG_MAIN);
        p.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return p;
    }
    /** White card panel */
    public static JPanel cardPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(CARD_BG);
        p.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        return p;
    }
    /** Teal pill badge */
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(fg); l.setBackground(bg); l.setOpaque(true);
        l.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return l;
    }
}
