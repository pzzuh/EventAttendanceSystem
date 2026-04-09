package com.attendance.dashboard;

import com.attendance.db.DatabaseConnection;
import com.attendance.modules.*;
import com.attendance.util.Session;
import com.attendance.util.UITheme;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardFrame extends JFrame {

    private JPanel contentArea;
    private JLabel lblClock, lblTopTitle;
    private JLabel lblColleges, lblDepts, lblStudents, lblCourses, lblUsers, lblEvents;
    private JButton activeSideBtn = null;
    private DefaultCategoryDataset collegeDataset;
    private DefaultPieDataset<String> overviewDataset;
    private Timer chartRefreshTimer;

    // Sidebar collapse state
    private JPanel sidebar;
    private boolean sidebarExpanded = true;
    private static final int SIDEBAR_W = 220;
    private static final int SIDEBAR_COLLAPSED_W = 56;

    // Sidebar component refs for show/hide text
    private JPanel logoPane, avatarStrip;
    private JPanel[] navGroupPanels = new JPanel[3];
    private JButton[] allNavBtns;
    private JLabel logoLbl;
    private JLabel nameL, roleL;

    public DashboardFrame() {
        setTitle("Attendance System — Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 730);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        buildUI();
        startClock();
        startChartRefresh();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));

        // ════════════════════════════════════════════════════════════════
        //  SIDEBAR
        // ════════════════════════════════════════════════════════════════
        sidebar = new JPanel();
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_W, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // ── Logo + toggle row ──────────────────────────────────────────
        logoPane = new JPanel(new BorderLayout(0, 0));
        logoPane.setBackground(UITheme.SIDEBAR_HEAD);
        logoPane.setMaximumSize(new Dimension(SIDEBAR_W, 56));
        logoPane.setMinimumSize(new Dimension(SIDEBAR_COLLAPSED_W, 56));
        logoPane.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 6));

        logoLbl = new JLabel("◈  Attendance System");
        logoLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logoLbl.setForeground(UITheme.ACCENT);

        JButton btnToggle = new JButton("◀");
        btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnToggle.setForeground(UITheme.ACCENT);
        btnToggle.setBackground(new Color(30, 40, 60));
        btnToggle.setBorderPainted(false);
        btnToggle.setFocusPainted(false);
        btnToggle.setOpaque(true);
        btnToggle.setPreferredSize(new Dimension(32, 32));
        btnToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggle.setToolTipText("Collapse sidebar");
        btnToggle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnToggle.setBackground(UITheme.ACCENT);
                btnToggle.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnToggle.setBackground(new Color(30, 40, 60));
                btnToggle.setForeground(UITheme.ACCENT);
            }
        });
        btnToggle.addActionListener(e -> {
            toggleSidebar();
            if (sidebarExpanded) {
                btnToggle.setText("◀");
                btnToggle.setToolTipText("Collapse sidebar");
            } else {
                btnToggle.setText("▶");
                btnToggle.setToolTipText("Expand sidebar");
            }
        });

        logoPane.add(logoLbl, BorderLayout.CENTER);
        logoPane.add(btnToggle, BorderLayout.EAST);
        sidebar.add(logoPane);

        // ── Avatar strip ───────────────────────────────────────────────
        avatarStrip = new JPanel(new BorderLayout(10, 0));
        avatarStrip.setBackground(new Color(22, 28, 42));
        avatarStrip.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        avatarStrip.setMaximumSize(new Dimension(SIDEBAR_W, 58));

        JLabel av = new JLabel("◉");
        av.setFont(new Font("Segoe UI", Font.BOLD, 22));
        av.setForeground(UITheme.ACCENT);

        JPanel nameBox = new JPanel(new GridLayout(2, 1, 0, 1));
        nameBox.setOpaque(false);
        nameL = new JLabel(Session.getFullName());
        nameL.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameL.setForeground(Color.WHITE);
        roleL = new JLabel(Session.getRole());
        roleL.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleL.setForeground(UITheme.SIDEBAR_TEXT);
        nameBox.add(nameL);
        nameBox.add(roleL);

        avatarStrip.add(av, BorderLayout.WEST);
        avatarStrip.add(nameBox, BorderLayout.CENTER);
        sidebar.add(avatarStrip);
        sidebar.add(Box.createVerticalStrut(6));

        // ── Nav sections ───────────────────────────────────────────────
        navGroupPanels[0] = navGroup("OVERVIEW");
        navGroupPanels[1] = navGroup("MANAGEMENT");
        navGroupPanels[2] = navGroup("EVENTS & REPORTS");

        JButton btnDashboard   = navBtn("⊞", "Dashboard",   null);
        JButton btnUsers       = navBtn("◉", "Users",        () -> openFrame(new UsersFrame()));
        JButton btnColleges    = navBtn("◉", "Colleges",     () -> openFrame(new CollegesFrame()));
        JButton btnDepts       = navBtn("◉", "Departments",  () -> openFrame(new DepartmentsFrame()));
        JButton btnCourses     = navBtn("◉", "Courses",      () -> openFrame(new CoursesFrame()));
        JButton btnStudents    = navBtn("◉", "Students",     () -> openFrame(new StudentsFrame()));
        JButton btnEvents      = navBtn("◉", "Events",       () -> openFrame(new EventsFrame()));
        JButton btnScanner     = navBtn("◉", "Scanner",      () -> openFrame(new ScannerFrame()));
        JButton btnAttendance  = navBtn("◉", "Attendance",   () -> openFrame(new AttendanceReportFrame()));

        allNavBtns = new JButton[]{
            btnDashboard, btnUsers, btnColleges, btnDepts,
            btnCourses, btnStudents, btnEvents, btnScanner, btnAttendance
        };

        sidebar.add(navGroupPanels[0]);
        sidebar.add(btnDashboard);
        sidebar.add(navGroupPanels[1]);
        sidebar.add(btnUsers);
        sidebar.add(btnColleges);
        sidebar.add(btnDepts);
        sidebar.add(btnCourses);
        sidebar.add(btnStudents);
        sidebar.add(navGroupPanels[2]);
        sidebar.add(btnEvents);
        sidebar.add(btnScanner);
        sidebar.add(btnAttendance);

        sidebar.add(Box.createVerticalGlue());

        setActive(btnDashboard);

        // ════════════════════════════════════════════════════════════════
        //  MAIN CONTENT
        // ════════════════════════════════════════════════════════════════
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(UITheme.BG_MAIN);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.TOPBAR_BG);
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(40, 52, 70)));
        topBar.setPreferredSize(new Dimension(0, 52));

        lblTopTitle = new JLabel("  Dashboard");
        lblTopTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTopTitle.setForeground(Color.WHITE);

        lblClock = new JLabel();
        lblClock.setFont(UITheme.FONT_SMALL);
        lblClock.setForeground(UITheme.SIDEBAR_TEXT);
        lblClock.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));

        topBar.add(lblTopTitle, BorderLayout.WEST);

        // ── Top-right: clock + sign out ────────────────────────────────
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        topRight.setOpaque(false);

        topRight.add(lblClock);

        JButton btnSignOut = new JButton("⏻  Sign Out");
        btnSignOut.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSignOut.setForeground(Color.WHITE);
        btnSignOut.setBackground(UITheme.DANGER);
        btnSignOut.setBorderPainted(false);
        btnSignOut.setFocusPainted(false);
        btnSignOut.setOpaque(true);
        btnSignOut.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btnSignOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSignOut.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnSignOut.setBackground(new Color(185, 28, 28));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnSignOut.setBackground(UITheme.DANGER);
            }
        });
        btnSignOut.addActionListener(e -> doLogout());
        topRight.add(btnSignOut);

        topBar.add(topRight, BorderLayout.EAST);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(UITheme.BG_MAIN);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(null);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);

        main.add(topBar, BorderLayout.NORTH);
        main.add(contentScroll, BorderLayout.CENTER);

        root.add(sidebar, BorderLayout.WEST);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
        showHome();
    }

    // ── Sidebar toggle ───────────────────────────────────────────────────
    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        int w = sidebarExpanded ? SIDEBAR_W : SIDEBAR_COLLAPSED_W;
        sidebar.setPreferredSize(new Dimension(w, 0));

        // Show / hide text labels
        logoLbl.setVisible(sidebarExpanded);
        nameL.setVisible(sidebarExpanded);
        roleL.setVisible(sidebarExpanded);
        for (JPanel p : navGroupPanels) p.setVisible(sidebarExpanded);

        // Update nav button text
        String[] icons  = {"⊞", "◉", "◉", "◉", "◉", "◉", "◉", "◉", "◉"};
        String[] labels = {"Dashboard","Users","Colleges","Departments","Courses","Students","Events","Scanner","Attendance"};
        for (int i = 0; i < allNavBtns.length; i++) {
            allNavBtns[i].setText(sidebarExpanded ? icons[i] + "   " + labels[i] : icons[i]);
            allNavBtns[i].setHorizontalAlignment(sidebarExpanded ? SwingConstants.LEFT : SwingConstants.CENTER);
            allNavBtns[i].setBorder(sidebarExpanded
                ? BorderFactory.createEmptyBorder(0, 22, 0, 0)
                : BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        sidebar.revalidate();
        sidebar.repaint();
        revalidate();
        repaint();
    }

    // ── Sidebar helpers ──────────────────────────────────────────────────
    private JPanel navGroup(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.SIDEBAR_BG);
        p.setMaximumSize(new Dimension(SIDEBAR_W, 26));
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(new Color(55, 65, 81));
        l.setBorder(BorderFactory.createEmptyBorder(8, 8, 2, 0));
        p.add(l);
        return p;
    }

    private JButton navBtn(String icon, String label, Runnable action) {
        JButton b = new JButton(icon + "   " + label);
        b.setFont(UITheme.FONT_SIDEBAR);
        b.setForeground(UITheme.SIDEBAR_TEXT);
        b.setBackground(UITheme.SIDEBAR_BG);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        b.setMaximumSize(new Dimension(SIDEBAR_W, 40));
        b.setPreferredSize(new Dimension(SIDEBAR_W, 40));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b != activeSideBtn) b.setBackground(UITheme.SIDEBAR_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (b != activeSideBtn) b.setBackground(UITheme.SIDEBAR_BG);
            }
        });
        b.addActionListener(e -> {
            setActive(b);
            lblTopTitle.setText("  " + label);
            if (action != null) action.run();
            else showHome();
        });
        return b;
    }

    private void setActive(JButton btn) {
        if (activeSideBtn != null) {
            activeSideBtn.setBackground(UITheme.SIDEBAR_BG);
            activeSideBtn.setForeground(UITheme.SIDEBAR_TEXT);
        }
        activeSideBtn = btn;
        btn.setBackground(UITheme.SIDEBAR_ACTIVE);
        btn.setForeground(UITheme.SIDEBAR_HEAD);
    }

    private void openFrame(JFrame frame) {
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { refreshStats(); }
        });
    }

    // ── Dashboard home ───────────────────────────────────────────────────
    private void showHome() {
        contentArea.removeAll();
        JPanel home = new JPanel();
        home.setBackground(UITheme.BG_MAIN);
        home.setLayout(new BoxLayout(home, BoxLayout.Y_AXIS));
        home.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        // Greeting
        JLabel greet = new JLabel("Hello, " + Session.getFullName().split(" ")[0] + " ◈");
        greet.setFont(new Font("Segoe UI", Font.BOLD, 24));
        greet.setForeground(UITheme.TEXT_PRIMARY);
        greet.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Here's your attendance system overview");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        home.add(greet);
        home.add(Box.createVerticalStrut(4));
        home.add(sub);
        home.add(Box.createVerticalStrut(20));

        // Stat cards
        JPanel stats = new JPanel(new GridLayout(1, 6, 12, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));
        stats.setAlignmentX(LEFT_ALIGNMENT);

        lblColleges = new JLabel("0");
        lblDepts    = new JLabel("0");
        lblStudents = new JLabel("0");
        lblCourses  = new JLabel("0");
        lblUsers    = new JLabel("0");
        lblEvents   = new JLabel("0");

        stats.add(statCard("Colleges",    lblColleges, UITheme.ACCENT,                  "🏫"));
        stats.add(statCard("Departments", lblDepts,    UITheme.SUCCESS,                 "🏢"));
        stats.add(statCard("Students",    lblStudents, new Color(139, 92, 246),         "🎓"));
        stats.add(statCard("Courses",     lblCourses,  UITheme.WARNING,                 "📚"));
        stats.add(statCard("Users",       lblUsers,    UITheme.INFO,                    "👥"));
        stats.add(statCard("Events",      lblEvents,   UITheme.DANGER,                  "📅"));
        home.add(stats);
        home.add(Box.createVerticalStrut(22));

        JPanel chartRow = new JPanel(new GridLayout(1, 2, 12, 0));
        chartRow.setOpaque(false);
        chartRow.setAlignmentX(LEFT_ALIGNMENT);
        chartRow.add(chartCard("Student Population per College", createCollegePopulationChart()));
        chartRow.add(chartCard("System Overview", createSystemOverviewChart()));
        home.add(chartRow);
        home.add(Box.createVerticalStrut(22));

        // Quick access
        JLabel qaLbl = new JLabel("Quick Access");
        qaLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        qaLbl.setForeground(UITheme.TEXT_PRIMARY);
        qaLbl.setAlignmentX(LEFT_ALIGNMENT);
        home.add(qaLbl);
        home.add(Box.createVerticalStrut(10));

        JPanel qa = new JPanel(new GridLayout(2, 4, 12, 12));
        qa.setOpaque(false);
        qa.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        qa.setAlignmentX(LEFT_ALIGNMENT);
        qa.add(qaCard("◉  Users",       new Color(238, 242, 255), UITheme.ACCENT,                  () -> openFrame(new UsersFrame())));
        qa.add(qaCard("◉  Colleges",    new Color(209, 250, 229), UITheme.SUCCESS,                 () -> openFrame(new CollegesFrame())));
        qa.add(qaCard("◉  Departments", new Color(237, 233, 254), new Color(124, 58, 237),         () -> openFrame(new DepartmentsFrame())));
        qa.add(qaCard("◉  Courses",     new Color(254, 243, 199), new Color(180, 83, 9),           () -> openFrame(new CoursesFrame())));
        qa.add(qaCard("◉  Students",    new Color(219, 234, 254), new Color(29, 78, 216),          () -> openFrame(new StudentsFrame())));
        qa.add(qaCard("◉  Events",      new Color(254, 226, 226), UITheme.DANGER,                  () -> openFrame(new EventsFrame())));
        qa.add(qaCard("◉  Scanner",     new Color(209, 250, 229), new Color(5, 150, 105),          () -> openFrame(new ScannerFrame())));
        qa.add(qaCard("◉  Reports",     new Color(243, 244, 246), UITheme.TEXT_SECONDARY,          () -> openFrame(new AttendanceReportFrame())));
        home.add(qa);

        contentArea.add(home, BorderLayout.NORTH);
        contentArea.revalidate();
        contentArea.repaint();
        refreshStats();
    }

    private JPanel statCard(String label, JLabel val, Color color, String icon) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(Color.WHITE);
        c.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 3, 0, color),
            new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14))));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel lbl = new JLabel(label); lbl.setFont(UITheme.FONT_SMALL); lbl.setForeground(UITheme.TEXT_MUTED);
        JLabel ico = new JLabel(icon);  ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        top.add(lbl, BorderLayout.WEST); top.add(ico, BorderLayout.EAST);
        val.setFont(new Font("Segoe UI", Font.BOLD, 28)); val.setForeground(UITheme.TEXT_PRIMARY);
        c.add(top, BorderLayout.NORTH); c.add(val, BorderLayout.CENTER);
        return c;
    }

    private JPanel qaCard(String text, Color bg, Color fg, Runnable action) {
        JPanel c = new JPanel(new GridBagLayout());
        c.setBackground(bg);
        c.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(fg);
        c.add(l);
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
            public void mouseEntered(java.awt.event.MouseEvent e) { c.setBackground(bg.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e)  { c.setBackground(bg); }
        });
        return c;
    }

    private JPanel chartCard(String title, ChartPanel chartPanel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(UITheme.BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(lbl, BorderLayout.NORTH);
        chartPanel.setPreferredSize(new Dimension(0, 300));
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setPopupMenu(null);
        chartPanel.setMouseWheelEnabled(false);
        card.add(chartPanel, BorderLayout.CENTER);
        return card;
    }

    private ChartPanel createCollegePopulationChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        collegeDataset = dataset;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT c.college_name, COUNT(s.id) AS total " +
                 "FROM colleges c LEFT JOIN students s ON s.college_id = c.id " +
                 "GROUP BY c.id, c.college_name ORDER BY total DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String collegeName = rs.getString("college_name");
                    String category = abbreviateCollegeName(collegeName);
                    dataset.addValue(rs.getInt("total"), "Students", category);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (dataset.getColumnCount() == 0) {
            dataset.addValue(0, "Students", "No data");
        }
        JFreeChart chart = ChartFactory.createBarChart(
            null,
            "College",
            "Students",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(UITheme.BORDER_COLOR);
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        BarRenderer renderer = new BarRenderer();
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("Segoe UI", Font.BOLD, 11));
        plot.setRenderer(renderer);

        chart.setBackgroundPaint(UITheme.BG_MAIN);
        return new ChartPanel(chart, false);
    }

    private ChartPanel createSystemOverviewChart() {
        overviewDataset = new DefaultPieDataset<>();
        try (Connection c = DatabaseConnection.getConnection()) {
            refreshOverviewData(c);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (overviewDataset.getItemCount() == 0) {
            overviewDataset.setValue("No data", 1);
        }
        JFreeChart chart = ChartFactory.createPieChart(
            null,
            overviewDataset,
            true,
            true,
            false);
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelBackgroundPaint(new Color(255,255,255,200));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        chart.setBackgroundPaint(UITheme.BG_MAIN);
        return new ChartPanel(chart, false);
    }

    private int cntInt(Connection c, String t) throws SQLException {
        ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + t);
        return rs.next() ? rs.getInt(1) : 0;
    }

    public void refreshStats() {
        try (Connection c = DatabaseConnection.getConnection()) {
            if (lblColleges != null) lblColleges.setText(cnt(c, "colleges"));
            if (lblDepts    != null) lblDepts.setText(cnt(c, "departments"));
            if (lblStudents != null) lblStudents.setText(cnt(c, "students"));
            if (lblCourses  != null) lblCourses.setText(cnt(c, "courses"));
            if (lblUsers    != null) lblUsers.setText(cnt(c, "users"));
            if (lblEvents   != null) lblEvents.setText(cnt(c, "events"));
            refreshCollegePopulationData(c);
            refreshOverviewData(c);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void refreshCollegePopulationData(Connection c) throws SQLException {
        if (collegeDataset == null) return;
        collegeDataset.clear();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT c.college_name, COUNT(s.id) AS total " +
                "FROM colleges c LEFT JOIN students s ON s.college_id = c.id " +
                "GROUP BY c.id, c.college_name ORDER BY total DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String collegeName = rs.getString("college_name");
                    String category = abbreviateCollegeName(collegeName);
                    collegeDataset.addValue(rs.getInt("total"), "Students", category);
                }
            }
        }
        if (collegeDataset.getColumnCount() == 0) {
            collegeDataset.addValue(0, "Students", "No data");
        }
    }

    private void refreshOverviewData(Connection c) throws SQLException {
        if (overviewDataset == null) return;
        overviewDataset.clear();
        overviewDataset.setValue("Students", cntInt(c, "students"));
        overviewDataset.setValue("Users", cntInt(c, "users"));
        overviewDataset.setValue("Events", cntInt(c, "events"));
        overviewDataset.setValue("Colleges", cntInt(c, "colleges"));
        overviewDataset.setValue("Departments", cntInt(c, "departments"));
        overviewDataset.setValue("Courses", cntInt(c, "courses"));
    }

    private String abbreviateCollegeName(String name) {
        if (name == null || name.isBlank()) return "Unknown";
        List<String> parts = new ArrayList<>();
        for (String word : name.split("\\s+")) {
            String clean = word.replaceAll("[^A-Za-z0-9]", "");
            if (clean.isBlank()) continue;
            String lower = clean.toLowerCase();
            if (Arrays.asList("of", "and", "&", "the", "for").contains(lower)) continue;
            if (parts.isEmpty() && lower.equals("college")) continue;
            parts.add(clean);
        }
        if (parts.isEmpty()) {
            return name.length() <= 8 ? name : name.substring(0, 8).toUpperCase();
        }
        if (parts.size() == 1) {
            String single = parts.get(0).toUpperCase();
            return single.length() <= 6 ? single : single.substring(0, 6);
        }
        StringBuilder abbr = new StringBuilder();
        for (String part : parts) {
            abbr.append(Character.toUpperCase(part.charAt(0)));
        }
        return abbr.toString();
    }

    private String cnt(Connection c, String t) throws SQLException {
        ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + t);
        return rs.next() ? String.valueOf(rs.getInt(1)) : "0";
    }

    private void startClock() {
        Timer t = new Timer(1000, e -> lblClock.setText(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE MMM d  HH:mm:ss  "))));
        t.setInitialDelay(0);
        t.start();
    }

    private void startChartRefresh() {
        chartRefreshTimer = new Timer(5000, e -> refreshStats());
        chartRefreshTimer.setInitialDelay(5000);
        chartRefreshTimer.start();
    }

    private void doLogout() {
        int c = JOptionPane.showConfirmDialog(this, "Sign out of Attendance System?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            Session.logout();
            dispose();
            new com.attendance.auth.LoginFrame().setVisible(true);
        }
    }
}
