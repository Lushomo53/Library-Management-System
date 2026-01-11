package com.library.controller;

import com.library.dao.UserDAO;
import com.library.model.User;
import com.library.util.PasswordUtil;
import com.library.util.SceneManager;
import com.library.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class MembershipApplicationController implements Initializable {

    // Form fields
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    
    // Error labels
    @FXML private Label fullNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label addressError;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label generalMessage;
    
    // Buttons
    @FXML private Button submitButton;
    @FXML private Button cancelButton;
    @FXML private Hyperlink backToLoginLink;
    
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
        fullNameField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(fullNameField, fullNameError));
        emailField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(emailField, emailError));
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(phoneField, phoneError));
        addressField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(addressField, addressError));
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(usernameField, usernameError));
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(passwordField, passwordError));
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> clearFieldError(confirmPasswordField, confirmPasswordError));
    }

    /**
     * Handle submit button click
     */
    @FXML
    private void handleSubmit() {
        // Clear all previous errors
        clearAllErrors();
        
        // Get input values
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate inputs
        if (!validateInputs(fullName, email, phone, address, username, password, confirmPassword)) {
            return;
        }
        
        // Check if username or email already exists
        if (userDAO.usernameExists(username)) {
            showError(usernameError, "Username already exists. Please choose another.");
            highlightError(usernameField);
            return;
        }
        
        if (userDAO.emailExists(email)) {
            showError(emailError, "Email already registered. Please use another email.");
            highlightError(emailField);
            return;
        }
        
        // Create new user object
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(PasswordUtil.hashPassword(password));
        newUser.setRole("MEMBER");
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setAddress(address);
        newUser.setStatus("PENDING"); // Requires admin approval
        
        // Save to database
        try {
            boolean success = userDAO.createUser(newUser);
            
            if (success) {
                // Show success message
                showSuccessMessage("Application submitted successfully! Your membership is pending approval. You will receive an email once approved.");
                
                // Disable form
                disableForm();
                
                // Navigate back to login after 3 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        javafx.application.Platform.runLater(this::handleBackToLogin);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                
            } else {
                showError(generalMessage, "Failed to submit application. Please try again.");
            }
            
        } catch (Exception e) {
            showError(generalMessage, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validate all form inputs (Submit-level validation)
     */
    private boolean validateInputs(String fullName, String email, String phone, String address,
                                   String username, String password, String confirmPassword) {

        boolean isValid = true;

        // Clear previous errors first (important for submit)
        clearAllErrors();

        if (!validateFullNameField()) {
            isValid = false;
        }

        if (!validateEmailField()) {
            isValid = false;
        }

        if (!validatePhoneField()) {
            isValid = false;
        }

        if (!validateAddressField()) {
            isValid = false;
        }

        if (!validateUsernameField()) {
            isValid = false;
        }

        if (!validatePasswordField()) {
            isValid = false;
        }

        if (!validateConfirmPasswordField()) {
            isValid = false;
        }

        return isValid;
    }


    private boolean validateFullNameField() {
        String fullName = fullNameField.getText().trim();

        if (ValidationUtil.isEmpty(fullName)) {
            showError(fullNameError, "Full name is required");
            highlightError(fullNameField);
            return false;
        }
        if (!ValidationUtil.isAlphabetic(fullName)) {
            showError(fullNameError, "Full name should only contain letters and spaces");
            highlightError(fullNameField);
            return false;
        }
        if (!ValidationUtil.isValidLength(fullName, 3, 100)) {
            showError(fullNameError, "Full name must be between 3 and 100 characters");
            highlightError(fullNameField);
            return false;
        }
        return true;
    }

    private boolean validateEmailField() {
        String email = emailField.getText().trim();

        if (ValidationUtil.isEmpty(email)) {
            showError(emailError, "Email is required");
            highlightError(emailField);
            return false;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            showError(emailError, ValidationUtil.getEmailErrorMessage());
            highlightError(emailField);
            return false;
        }
        return true;
    }

    private boolean validatePhoneField() {
        String phone = phoneField.getText().trim();

        if (ValidationUtil.isEmpty(phone)) {
            showError(phoneError, "Phone number is required");
            highlightError(phoneField);
            return false;
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            showError(phoneError, ValidationUtil.getPhoneErrorMessage());
            highlightError(phoneField);
            return false;
        }
        return true;
    }

    private boolean validateAddressField() {
        String address = addressField.getText().trim();

        if (ValidationUtil.isEmpty(address)) {
            showError(addressError, "Address is required");
            highlightError(addressField);
            return false;
        }
        if (!ValidationUtil.isValidLength(address, 10, 500)) {
            showError(addressError, "Address must be between 10 and 500 characters");
            highlightError(addressField);
            return false;
        }
        return true;
    }

    private boolean validateUsernameField() {
        String username = usernameField.getText().trim();

        if (ValidationUtil.isEmpty(username)) {
            showError(usernameError, "Username is required");
            highlightError(usernameField);
            return false;
        }
        if (!ValidationUtil.isValidUsername(username)) {
            showError(usernameError, ValidationUtil.getUsernameErrorMessage());
            highlightError(usernameField);
            return false;
        }
        return true;
    }

    private boolean validatePasswordField() {
        String password = passwordField.getText();

        if (ValidationUtil.isEmpty(password)) {
            showError(passwordError, "Password is required");
            highlightError(passwordField);
            return false;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            showError(passwordError, ValidationUtil.getPasswordErrorMessage());
            highlightError(passwordField);
            return false;
        }
        return true;
    }

    private boolean validateConfirmPasswordField() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (ValidationUtil.isEmpty(confirmPassword)) {
            showError(confirmPasswordError, "Please confirm your password");
            highlightError(confirmPasswordField);
            return false;
        }
        if (!ValidationUtil.passwordsMatch(password, confirmPassword)) {
            showError(confirmPasswordError, "Passwords do not match");
            highlightError(confirmPasswordField);
            return false;
        }
        return true;
    }

    @FXML
    private void handleFullNameField() {
        clearFieldError(fullNameField, fullNameError);
        if (validateFullNameField()) {
            emailField.requestFocus();
        }
    }

    @FXML
    private void handleEmailField() {
        clearFieldError(emailField, emailError);
        if (validateEmailField()) {
            phoneField.requestFocus();
        }
    }

    @FXML
    private void handlePhoneField() {
        clearFieldError(phoneField, phoneError);
        if (validatePhoneField()) {
            addressField.requestFocus();
        }
    }

    @FXML
    private void handleAddressField() {
        clearFieldError(addressField, addressError);
        if (validateAddressField()) {
            usernameField.requestFocus();
        }
    }

    @FXML
    private void handleUsernameField() {
        clearFieldError(usernameField, usernameError);
        if (validateUsernameField()) {
            passwordField.requestFocus();
        }
    }

    @FXML
    private void handlePasswordField() {
        clearFieldError(passwordField, passwordError);
        if (validatePasswordField()) {
            confirmPasswordField.requestFocus();
        }
    }

    @FXML
    private void handleConfirmPasswordField() {
        clearFieldError(confirmPasswordField, confirmPasswordError);
        if (validateConfirmPasswordField()) {
            submitButton.requestFocus();
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        if (SceneManager.showConfirmation("Cancel Application", 
            "Are you sure you want to cancel? All entered information will be lost.")) {
            handleBackToLogin();
        }
    }

    /**
     * Handle back to login link click
     */
    @FXML
    private void handleBackToLogin() {
        try {
            SceneManager.switchView("Login.fxml", "Library Management System - Login", null);
        } catch (Exception e) {
            showError(generalMessage, "Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Disable form after successful submission
     */
    private void disableForm() {
        fullNameField.setDisable(true);
        emailField.setDisable(true);
        phoneField.setDisable(true);
        addressField.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        confirmPasswordField.setDisable(true);
        submitButton.setDisable(true);
        cancelButton.setDisable(true);
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
            errorLabel.setStyle("-fx-text-fill: #FF4444;");
        }
    }

    /**
     * Show success message
     */
    private void showSuccessMessage(String message) {
        if (generalMessage != null) {
            generalMessage.setText(message);
            generalMessage.setVisible(true);
            generalMessage.setManaged(true);
            generalMessage.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    /**
     * Clear error for a specific text field
     */
    private void clearFieldError(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("error-field");
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
        // Clear all error labels
        Label[] errorLabels = {
            fullNameError, emailError, phoneError, addressError,
            usernameError, passwordError, confirmPasswordError, generalMessage
        };
        
        for (Label label : errorLabels) {
            if (label != null) {
                label.setVisible(false);
                label.setManaged(false);
            }
        }
        
        // Clear field highlights
        fullNameField.getStyleClass().remove("error-field");
        emailField.getStyleClass().remove("error-field");
        phoneField.getStyleClass().remove("error-field");
        addressField.getStyleClass().remove("error-field");
        usernameField.getStyleClass().remove("error-field");
        passwordField.getStyleClass().remove("error-field");
        confirmPasswordField.getStyleClass().remove("error-field");
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
     * Set UserDAO instance (useful for mocking in tests)
     */
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Static method to validate application data without UI
     */
    public static boolean validateApplicationData(String fullName, String email, String phone, 
                                                  String address, String username, String password) {
        boolean isValid = true;
        
        isValid &= ValidationUtil.isNotEmpty(fullName) && ValidationUtil.isAlphabetic(fullName);
        isValid &= ValidationUtil.isValidEmail(email);
        isValid &= ValidationUtil.isValidPhone(phone);
        isValid &= ValidationUtil.isNotEmpty(address) && ValidationUtil.isValidLength(address, 10, 500);
        isValid &= ValidationUtil.isValidUsername(username);
        isValid &= ValidationUtil.isValidPassword(password);
        
        return isValid;
    }

    /**
     * Get UserDAO instance for testing
     */
    public UserDAO getUserDAO() {
        return userDAO;
    }
}