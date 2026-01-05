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

public class AddLibrarianDialogController implements Initializable {

    // Form fields
    @FXML private TextField employeeIdField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    
    // Permissions
    @FXML private CheckBox canApproveRequestsCheckbox;
    @FXML private CheckBox canIssueReturnsCheckbox;
    @FXML private CheckBox canRevokeMembershipCheckbox;
    
    // Error labels
    @FXML private Label employeeIdError;
    @FXML private Label fullNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label generalMessage;
    
    // Buttons
    @FXML private Button cancelButton;
    @FXML private Button registerButton;
    
    private UserDAO userDAO;
    private Runnable onLibrarianAddedCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        setupListeners();
    }

    /**
     * Setup input field listeners to clear errors on typing
     */
    private void setupListeners() {
        employeeIdField.textProperty().addListener((obs, oldVal, newVal) -> 
            clearFieldError(employeeIdField, employeeIdError));
        fullNameField.textProperty().addListener((obs, oldVal, newVal) -> 
            clearFieldError(fullNameField, fullNameError));
        emailField.textProperty().addListener((obs, oldVal, newVal) -> 
            clearFieldError(emailField, emailError));
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> 
            clearFieldError(phoneField, phoneError));
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> 
            clearFieldError(passwordField, passwordError));
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> 
            clearFieldError(confirmPasswordField, confirmPasswordError));
    }

    /**
     * Set callback to execute when librarian is added
     */
    public void setOnLibrarianAdded(Runnable callback) {
        this.onLibrarianAddedCallback = callback;
    }

    /**
     * Handle register button click
     */
    @FXML
    private void handleRegister() {
        clearAllErrors();
        
        // Get input values
        String employeeId = employeeIdField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Get permissions
        boolean canApproveRequests = canApproveRequestsCheckbox.isSelected();
        boolean canIssueReturns = canIssueReturnsCheckbox.isSelected();
        boolean canRevokeMembership = canRevokeMembershipCheckbox.isSelected();
        
        // Validate inputs
        if (!validateInputs(employeeId, fullName, email, phone, password, confirmPassword)) {
            return;
        }
        
        // Check if employee ID or email already exists
        if (userDAO.usernameExists(employeeId)) {
            showError(employeeIdError, "Employee ID already exists. Please use a different one.");
            highlightError(employeeIdField);
            return;
        }
        
        if (userDAO.emailExists(email)) {
            showError(emailError, "Email already registered. Please use a different email.");
            highlightError(emailField);
            return;
        }
        
        // Confirm registration
        boolean confirmed = SceneManager.showConfirmation(
            "Register Librarian",
            "Register new librarian with the following details?\n\n" +
            "Employee ID: " + employeeId + "\n" +
            "Name: " + fullName + "\n" +
            "Email: " + email + "\n\n" +
            "Permissions:\n" +
            "• Approve Requests: " + (canApproveRequests ? "Yes" : "No") + "\n" +
            "• Issue/Return Books: " + (canIssueReturns ? "Yes" : "No") + "\n" +
            "• Revoke Memberships: " + (canRevokeMembership ? "Yes" : "No")
        );
        
        if (!confirmed) return;
        
        try {
            // Create librarian user
            User librarian = new User();
            librarian.setUsername(employeeId); // Employee ID is the username
            librarian.setPassword(password); // TODO: Hash in production
            librarian.setRole("LIBRARIAN");
            librarian.setFullName(fullName);
            librarian.setEmail(email);
            librarian.setPhone(phone);
            librarian.setAddress(address.isEmpty() ? null : address);
            librarian.setStatus("ACTIVE");
            librarian.setEmployeeId(employeeId);
            
            // Set permissions
            librarian.setCanApproveRequests(canApproveRequests);
            librarian.setCanIssueReturns(canIssueReturns);
            librarian.setCanRevokeMembership(canRevokeMembership);
            
            // Save to database
            boolean success = userDAO.createUser(librarian);
            
            if (success) {
                SceneManager.showInfo(
                    "Librarian Registered",
                    "Librarian registered successfully!\n\n" +
                    "Name: " + fullName + "\n" +
                    "Employee ID: " + employeeId + "\n" +
                    "Email: " + email + "\n\n" +
                    "The librarian can now login using their Employee ID as username."
                );
                
                // Trigger callback
                if (onLibrarianAddedCallback != null) {
                    onLibrarianAddedCallback.run();
                }
                
            } else {
                showError(generalMessage, "Failed to register librarian. Please try again.");
            }
            
        } catch (Exception e) {
            System.err.println("Error registering librarian: " + e.getMessage());
            e.printStackTrace();
            showError(generalMessage, "Error: " + e.getMessage());
        }
    }

    /**
     * Validate all form inputs
     */
    private boolean validateInputs(String employeeId, String fullName, String email, 
                                   String phone, String password, String confirmPassword) {
        boolean isValid = true;
        
        // Validate employee ID
        if (ValidationUtil.isEmpty(employeeId)) {
            showError(employeeIdError, "Employee ID is required");
            highlightError(employeeIdField);
            isValid = false;
        } else if (!ValidationUtil.isValidUsername(employeeId)) {
            showError(employeeIdError, "Employee ID must be 3-20 alphanumeric characters");
            highlightError(employeeIdField);
            isValid = false;
        }
        
        // Validate full name
        if (ValidationUtil.isEmpty(fullName)) {
            showError(fullNameError, "Full name is required");
            highlightError(fullNameField);
            isValid = false;
        } else if (!ValidationUtil.isAlphabetic(fullName)) {
            showError(fullNameError, "Full name should only contain letters and spaces");
            highlightError(fullNameField);
            isValid = false;
        } else if (!ValidationUtil.isValidLength(fullName, 3, 100)) {
            showError(fullNameError, "Full name must be between 3 and 100 characters");
            highlightError(fullNameField);
            isValid = false;
        }
        
        // Validate email
        if (ValidationUtil.isEmpty(email)) {
            showError(emailError, "Email is required");
            highlightError(emailField);
            isValid = false;
        } else if (!ValidationUtil.isValidEmail(email)) {
            showError(emailError, ValidationUtil.getEmailErrorMessage());
            highlightError(emailField);
            isValid = false;
        }
        
        // Validate phone
        if (ValidationUtil.isEmpty(phone)) {
            showError(phoneError, "Phone number is required");
            highlightError(phoneField);
            isValid = false;
        } else if (!ValidationUtil.isValidPhone(phone)) {
            showError(phoneError, ValidationUtil.getPhoneErrorMessage());
            highlightError(phoneField);
            isValid = false;
        }
        
        // Validate password
        if (ValidationUtil.isEmpty(password)) {
            showError(passwordError, "Password is required");
            highlightError(passwordField);
            isValid = false;
        } else if (!ValidationUtil.isValidPassword(password)) {
            showError(passwordError, ValidationUtil.getPasswordErrorMessage());
            highlightError(passwordField);
            isValid = false;
        }
        
        // Validate confirm password
        if (ValidationUtil.isEmpty(confirmPassword)) {
            showError(confirmPasswordError, "Please confirm the password");
            highlightError(confirmPasswordField);
            isValid = false;
        } else if (!ValidationUtil.passwordsMatch(password, confirmPassword)) {
            showError(confirmPasswordError, "Passwords do not match");
            highlightError(confirmPasswordField);
            isValid = false;
        }
        
        return isValid;
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        cancelButton.getScene().getWindow().hide();
    }

    // ==================== UI Helper Methods ====================
    
    /**
     * Show error message
     */
    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Clear specific error
     */
    private void clearError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Clear field error
     */
    private void clearFieldError(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("error-field");
        }
        clearError(errorLabel);
    }

    /**
     * Clear all errors
     */
    private void clearAllErrors() {
        clearError(employeeIdError);
        clearError(fullNameError);
        clearError(emailError);
        clearError(phoneError);
        clearError(passwordError);
        clearError(confirmPasswordError);
        clearError(generalMessage);
        
        employeeIdField.getStyleClass().remove("error-field");
        fullNameField.getStyleClass().remove("error-field");
        emailField.getStyleClass().remove("error-field");
        phoneField.getStyleClass().remove("error-field");
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

    // ==================== Getters/Setters for Testing ====================
    
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    /**
     * Get permission settings (for testing)
     */
    public boolean canApproveRequests() {
        return canApproveRequestsCheckbox.isSelected();
    }

    public boolean canIssueReturns() {
        return canIssueReturnsCheckbox.isSelected();
    }

    public boolean canRevokeMembership() {
        return canRevokeMembershipCheckbox.isSelected();
    }
}