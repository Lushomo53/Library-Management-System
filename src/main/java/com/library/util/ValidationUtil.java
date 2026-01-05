package com.library.util;

import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtil {

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,9}$"
    );
    
    private static final Pattern ISBN_PATTERN = Pattern.compile(
        "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$"
    );

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validate ISBN format
     */
    public static boolean isValidISBN(String isbn) {
        if (isEmpty(isbn)) {
            return false;
        }
        return ISBN_PATTERN.matcher(isbn.trim().replaceAll("[- ]", "")).matches();
    }

    /**
     * Validate password strength
     * At least 8 characters
     */
    public static boolean isValidPassword(String password) {
        if (isEmpty(password)) {
            return false;
        }
        return password.length() >= 8;
    }

    /**
     * Validate password strength with requirements
     * At least 8 characters, one uppercase, one lowercase, one digit
     */
    public static boolean isStrongPassword(String password) {
        if (isEmpty(password) || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            
            if (hasUpper && hasLower && hasDigit) return true;
        }
        
        return false;
    }

    /**
     * Check if passwords match
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        if (isEmpty(password) || isEmpty(confirmPassword)) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String value, int minLength, int maxLength) {
        if (isEmpty(value)) {
            return false;
        }
        int length = value.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validate numeric string
     */
    public static boolean isNumeric(String value) {
        if (isEmpty(value)) {
            return false;
        }
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate positive integer
     */
    public static boolean isPositiveInteger(String value) {
        if (isEmpty(value)) {
            return false;
        }
        try {
            int num = Integer.parseInt(value.trim());
            return num > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate integer in range
     */
    public static boolean isIntegerInRange(String value, int min, int max) {
        if (isEmpty(value)) {
            return false;
        }
        try {
            int num = Integer.parseInt(value.trim());
            return num >= min && num <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate double in range
     */
    public static boolean isDoubleInRange(String value, double min, double max) {
        if (isEmpty(value)) {
            return false;
        }
        try {
            double num = Double.parseDouble(value.trim());
            return num >= min && num <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Sanitize string (remove special characters)
     */
    public static String sanitize(String value) {
        if (isEmpty(value)) {
            return "";
        }
        return value.trim().replaceAll("[<>\"';]", "");
    }

    /**
     * Validate username format
     * Only alphanumeric and underscore, 3-20 characters
     */
    public static boolean isValidUsername(String username) {
        if (isEmpty(username)) {
            return false;
        }
        String trimmed = username.trim();
        return trimmed.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    /**
     * Validate that string contains only letters and spaces
     */
    public static boolean isAlphabetic(String value) {
        if (isEmpty(value)) {
            return false;
        }
        return value.trim().matches("^[a-zA-Z\\s]+$");
    }

    /**
     * Get validation error message for email
     */
    public static String getEmailErrorMessage() {
        return "Please enter a valid email address (e.g., user@example.com)";
    }

    /**
     * Get validation error message for phone
     */
    public static String getPhoneErrorMessage() {
        return "Please enter a valid phone number";
    }

    /**
     * Get validation error message for password
     */
    public static String getPasswordErrorMessage() {
        return "Password must be at least 8 characters long";
    }

    /**
     * Get validation error message for strong password
     */
    public static String getStrongPasswordErrorMessage() {
        return "Password must be at least 8 characters with uppercase, lowercase, and digit";
    }

    /**
     * Get validation error message for username
     */
    public static String getUsernameErrorMessage() {
        return "Username must be 3-20 characters (letters, numbers, underscore only)";
    }

    /**
     * Get validation error message for required field
     */
    public static String getRequiredFieldMessage(String fieldName) {
        return fieldName + " is required";
    }
}