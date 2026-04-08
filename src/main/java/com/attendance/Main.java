package com.attendance;

import com.attendance.auth.LoginFrame;
import com.attendance.db.DatabaseConnection;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Use system look and feel for best cross-platform appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Initialize database and seed default admin
        DatabaseConnection.initializeDatabase();

        // Launch login window on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}
