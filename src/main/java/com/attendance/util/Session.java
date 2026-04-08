package com.attendance.util;

public class Session {
    private static int userId;
    private static String username;
    private static String role;
    private static String fullName;

    public static void login(int id, String uname, String rl, String name) {
        userId = id; username = uname; role = rl; fullName = name;
    }

    public static void logout() { userId = 0; username = null; role = null; fullName = null; }

    public static int getUserId()    { return userId; }
    public static String getUsername() { return username; }
    public static String getRole()   { return role; }
    public static String getFullName() { return fullName; }
    public static boolean isLoggedIn() { return username != null; }
}
