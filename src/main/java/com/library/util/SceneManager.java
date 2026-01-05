package com.library.util;

import com.library.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utility class for managing scene transitions and window operations
 */
public class SceneManager {

    private static Stage primaryStage;
    private static User currentUser;

    /**
     * Set the primary stage (call this from Main/Application class)
     */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Set the current logged-in user
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Get the current logged-in user
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Switch to a new scene
     * 
     * @param fxmlFile The FXML file name (must be in resources folder)
     * @param title The window title
     * @param user The user object to pass to the controller (can be null)
     */
    public static void switchScene(String fxmlFile, String title, User user) throws IOException {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage not initialized. Call setPrimaryStage() first.");
        }

        // Update current user
        setCurrentUser(user);

        // Load FXML
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxmlFile));
        Parent root = loader.load();

        // Pass user to controller if it has a setUser method
        Object controller = loader.getController();
        if (controller != null && user != null) {
            try {
                controller.getClass().getMethod("setUser", User.class).invoke(controller, user);
            } catch (Exception e) {
                // Controller doesn't have setUser method, that's okay
            }
        }

        // Create and set scene
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
        primaryStage.show();
    }

    /**
     * Switch to a new scene without user parameter
     */
    public static void switchScene(String fxmlFile, String title) throws IOException {
        switchScene(fxmlFile, title, null);
    }

    /**
     * Open a new modal dialog window
     * 
     * @param fxmlFile The FXML file name
     * @param title The dialog title
     * @return The controller of the loaded FXML
     */
    public static Object openDialog(String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxmlFile));
        Parent root = loader.load();

        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(primaryStage);
        
        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        
        // Don't show yet - let caller configure controller first
        // dialogStage.showAndWait();
        
        // Return both stage and controller
        return new DialogResult(dialogStage, loader.getController());
    }

    /**
     * Open a modal dialog and wait for it to close
     */
    public static Object openDialogAndWait(String fxmlFile, String title) throws IOException {
        DialogResult result = (DialogResult) openDialog(fxmlFile, title);
        result.getStage().showAndWait();
        return result.getController();
    }

    /**
     * Logout and return to login screen
     */
    public static void logout() {
        try {
            setCurrentUser(null);
            switchScene("Login.fxml", "Library Management System - Login");
        } catch (IOException e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show error alert dialog
     */
    public static void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show information alert dialog
     */
    public static void showInfo(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show confirmation dialog
     * 
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirmation(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        return alert.showAndWait()
            .filter(response -> response == javafx.scene.control.ButtonType.OK)
            .isPresent();
    }

    /**
     * Show warning alert dialog
     */
    public static void showWarning(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.WARNING
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class to hold dialog stage and controller
     */
    public static class DialogResult {
        private final Stage stage;
        private final Object controller;

        public DialogResult(Stage stage, Object controller) {
            this.stage = stage;
            this.controller = controller;
        }

        public Stage getStage() {
            return stage;
        }

        public Object getController() {
            return controller;
        }
    }
}