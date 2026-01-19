package com.library.util;

import com.library.model.User;
import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

public final class SceneManager {

    private static Stage primaryStage;
    private static StackPane root;
    private static User currentUser;

    private static final Duration FADE_OUT = Duration.millis(180);
    private static final Duration FADE_IN  = Duration.millis(220);
    private static final Duration SLIDE    = Duration.millis(220);

    private SceneManager() {}

    /* =========================
       INIT
       ========================= */

    public static void init(Stage stage, StackPane rootContainer) {
        primaryStage = stage;
        root = rootContainer;

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        SceneManager.class.getResource("/styles.css")
                ).toExternalForm()
        );

        primaryStage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                SceneManager.class.getResourceAsStream("/images/logo.jpg")
                        )
                )
        );

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    /* =========================
       USER CONTEXT
       ========================= */

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    /* =========================
       VIEW SWITCHING (ANIMATED)
       ========================= */

    public static void switchView(String fxml, String title, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("/fxml/" + fxml)
            );

            Parent newView = loader.load();

            Object controller = loader.getController();
            if (controller != null && user != null) {
                try {
                    controller.getClass()
                            .getMethod("setUser", User.class)
                            .invoke(controller, user);
                } catch (Exception ignored) {}
            }

            setCurrentUser(user);
            animateTransition(newView);
            primaryStage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void switchView(String fxml, String title) {
        switchView(fxml, title, null);
    }

    private static void animateTransition(Parent newView) {

        newView.setOpacity(0);
        newView.setTranslateX(30);

        if (!root.getChildren().isEmpty()) {
            Parent oldView = (Parent) root.getChildren().getFirst();

            FadeTransition fadeOut = new FadeTransition(FADE_OUT, oldView);
            fadeOut.setToValue(0);

            TranslateTransition slideOut = new TranslateTransition(SLIDE, oldView);
            slideOut.setToX(-30);

            ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
            exit.setOnFinished(e -> {
                root.getChildren().setAll(newView);
                playEnterAnimation(newView);
            });
            exit.play();

        } else {
            root.getChildren().add(newView);
            playEnterAnimation(newView);
        }
    }

    private static void playEnterAnimation(Parent view) {

        FadeTransition fadeIn = new FadeTransition(FADE_IN, view);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(SLIDE, view);
        slideIn.setFromX(30);
        slideIn.setToX(0);

        new ParallelTransition(fadeIn, slideIn).play();
    }

    /* =========================
       DIALOGS (YOUR CODE — KEPT)
       ========================= */

    public static DialogResult openDialog(String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxmlFile));
        Parent root = loader.load();

        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(primaryStage);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(SceneManager.class.getResource("/styles.css")).toExternalForm()
        );

        primaryStage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                SceneManager.class.getResourceAsStream("/images/logo.jpg")
                        )
                )
        );

        dialogStage.setScene(scene);

        dialogStage.setOnShown(event -> {
            dialogStage.setX(primaryStage.getX() + (primaryStage.getWidth() - dialogStage.getWidth()) / 2);
            dialogStage.setY(primaryStage.getY() + (primaryStage.getHeight() - dialogStage.getHeight()) / 2);
        });

        return new DialogResult(dialogStage, loader.getController());
    }

    public static Object openDialogAndWait(String fxmlFile, String title) throws IOException {
        DialogResult result = openDialog(fxmlFile, title);
        result.getStage().showAndWait();
        return result.getController();
    }

    /* =========================
       ALERTS (YOUR CODE — KEPT)
       ========================= */

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        configureAlert(alert, title, message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        configureAlert(alert, title, message);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        configureAlert(alert, title, message);
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        configureAlert(alert, title, message);
        alert.showAndWait();
    }

    private static void configureAlert(Alert alert, String title, String message) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(primaryStage);
        alert.initModality(Modality.APPLICATION_MODAL);


        DialogPane pane = alert.getDialogPane();

        pane.getStylesheets().add(
                Objects.requireNonNull(
                        SceneManager.class.getResource("/styles.css")
                ).toExternalForm()
        );

        pane.getStyleClass().add("dialog-pane");

        // IMPORTANT: style buttons
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.getStyleClass().add("dialog-primary-button");
        }

        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancel != null) {
            cancel.getStyleClass().add("dialog-cancel-button");
        }
    }

    /* =========================
       LOGOUT
       ========================= */

    public static void logout() {
        setCurrentUser(null);
        switchView("Login.fxml", "Library Management System");
    }

    /* =========================
       DIALOG RESULT
       ========================= */

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
