package com.library.controller;

import com.library.dao.UserDAO;
import com.library.model.User;
import com.library.util.SceneManager;
import com.library.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button loginButton;
    @FXML private Hyperlink applyMembershipLink;
    
    // Error labels
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label roleError;
    @FXML private Label generalError;
    
    private UserDAO userDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        setupListeners();
    }

    /**
     * Setup input field listeners to clear errors on typing
     */
    private void setupListeners() {
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(usernameField, usernameError));
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(passwordField, passwordError));
        roleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> clearComboError(roleComboBox, roleError));
    }

    /**
     * Handle login button click or Enter key press
     */
    @FXML
    private void handleLogin() {
        // Clear all previous errors
        clearAllErrors();
        
        // Get input values
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        
        // Validate inputs
        if (!validateInputs(username, password, role)) {
            return;
        }
        
        // Attempt authentication
        try {
            User user = authenticateUser(username, password, role);

            // Successful login - navigate to appropriate dashboard
            if (user != null) navigateToDashboard(user);
            else {
                // Authentication failed
                showError(generalError, "Invalid username, password, or role. Please try again.");
            }
            
        } catch (Exception e) {
            showError(generalError, "Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validate login form inputs
     */
    private boolean validateInputs(String username, String password, String role) {
        boolean isValid = true;
        
        // Validate username
        if (ValidationUtil.isEmpty(username)) {
            showError(usernameError, "Username is required");
            highlightError(usernameField);
            isValid = false;
        }
        
        // Validate password
        if (ValidationUtil.isEmpty(password)) {
            showError(passwordError, "Password is required");
            highlightError(passwordField);
            isValid = false;
        }
        
        // Validate role selection
        if (role == null || role.isEmpty()) {
            showError(roleError, "Please select a role");
            highlightError(roleComboBox);
            isValid = false;
        }
        
        return isValid;
    }

    /**
     * Authenticate user credentials against database
     */
    public static User authenticateUser(String username, String password, String role) {
        UserDAO userDAO = new UserDAO();
        
        // Normalize role to match database values
        String normalizedRole = normalizeRole(role);
        
        // Query database for user with matching credentials

        return userDAO.authenticate(username, password, normalizedRole);
    }

    /**
     * Navigate to appropriate dashboard based on user role
     */
    private void navigateToDashboard(User user) {
        try {
            switch (user.getRole().toLowerCase()) {
                case "member":
                    SceneManager.switchScene("MemberDashboard.fxml", "Member Dashboard", user);
                    break;
                    
                case "librarian":
                    SceneManager.switchScene("LibrarianDashboard.fxml", "Librarian Dashboard", user);
                    break;
                    
                case "admin":
                    SceneManager.switchScene("AdminDashboard.fxml", "Admin Dashboard", user);
                    break;
                    
                default:
                    showError(generalError, "Invalid user role");
            }
        } catch (Exception e) {
            showError(generalError, "Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle membership application link click
     */
    @FXML
    private void handleApplyMembership() {
        try {
            SceneManager.switchScene("MembershipApplication.fxml", "Apply for Membership", null);
        } catch (Exception e) {
            showError(generalError, "Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Normalize role string to match database format
     */
    private static String normalizeRole(String role) {
        if (role == null) return null;

        return switch (role.toLowerCase()) {
            case "member" -> "MEMBER";
            case "librarian" -> "LIBRARIAN";
            case "admin" -> "ADMIN";
            default -> role.toUpperCase();
        };
    }

    // ==================== UI Helper Methods ====================
    
    /**
     * Show error message and make label visible
     */
    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Clear error for a specific text field
     */
    private void clearFieldError(TextField field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("error-field");
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Clear error for a specific combo box
     */
    private void clearComboError(ComboBox<?> comboBox, Label errorLabel) {
        if (comboBox != null) {
            comboBox.getStyleClass().remove("error-field");
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Clear all error messages and highlights
     */
    private void clearAllErrors() {
        // Clear error labels
        if (usernameError != null) {
            usernameError.setVisible(false);
            usernameError.setManaged(false);
        }
        if (passwordError != null) {
            passwordError.setVisible(false);
            passwordError.setManaged(false);
        }
        if (roleError != null) {
            roleError.setVisible(false);
            roleError.setManaged(false);
        }
        if (generalError != null) {
            generalError.setVisible(false);
            generalError.setManaged(false);
        }
        
        // Clear field highlights
        usernameField.getStyleClass().remove("error-field");
        passwordField.getStyleClass().remove("error-field");
        roleComboBox.getStyleClass().remove("error-field");
    }

    /**
     * Highlight field with error styling
     */
    private void highlightError(Control control) {
        if (control != null && !control.getStyleClass().contains("error-field")) {
            control.getStyleClass().add("error-field");
        }
    }

    // ==================== Testing Support Methods ====================
    
    /**
     * Static test method for authentication logic
     * Can be called without instantiating the controller
     */
    public static boolean testAuthentication(String username, String password, String role) {
        User user = authenticateUser(username, password, role);
        return user != null;
    }

    /**
     * Get UserDAO instance for testing
     */
    public UserDAO getUserDAO() {
        return userDAO;
    }

    /**
     * Set UserDAO instance (useful for mocking in tests)
     */
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
}