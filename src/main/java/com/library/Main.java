package com.library;

import com.library.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Root container that will hold all views
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Main.fxml")
        );

        StackPane root = loader.load();

        // Initialize SceneManager ONCE
        SceneManager.init(stage, root);

        // Load first view with animation
        SceneManager.switchView(
                "Login.fxml",
                "Library Management System"
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}

