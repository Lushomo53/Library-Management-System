package com.library.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    // Cost factor: 10–12 is standard (higher = more secure, slower)
    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {
        // Prevent instantiation
    }

    /**
     * Hash a plain-text password using bcrypt.
     * Automatically generates and embeds a salt.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verify a plain-text password against a stored bcrypt hash.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }

        return BCrypt.checkpw(plainPassword, storedHash);
    }

    public static void main(String[] args) {
        System.out.println(hashPassword("admin123"));
        System.out.println(hashPassword("member123"));
        System.out.println(hashPassword("librarian123"));
    }
}


