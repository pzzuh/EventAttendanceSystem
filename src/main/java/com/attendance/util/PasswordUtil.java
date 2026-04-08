package com.attendance.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class PasswordUtil {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT     = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL   = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern EMAIL     = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static class PasswordStrengthResult {
        private final boolean valid;
        private final String message;
        public PasswordStrengthResult(boolean valid, String message) {
            this.valid = valid; this.message = message;
        }
        public boolean valid()   { return valid; }
        public String  message() { return message; }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        return hashPassword(rawPassword).equals(hashedPassword);
    }

    public static PasswordStrengthResult checkPasswordStrength(String password) {
        if (password == null || password.length() < 8)
            return new PasswordStrengthResult(false, "Password must be at least 8 characters long.");
        if (!UPPERCASE.matcher(password).find())
            return new PasswordStrengthResult(false, "Password must contain at least one uppercase letter.");
        if (!LOWERCASE.matcher(password).find())
            return new PasswordStrengthResult(false, "Password must contain at least one lowercase letter.");
        if (!DIGIT.matcher(password).find())
            return new PasswordStrengthResult(false, "Password must contain at least one number.");
        if (!SPECIAL.matcher(password).find())
            return new PasswordStrengthResult(false, "Password must contain at least one special character.");
        return new PasswordStrengthResult(true, "Password is strong.");
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }
}
