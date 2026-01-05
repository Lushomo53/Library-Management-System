package com.library;

import com.library.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        stage.sizeToScene();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setHeight(screenBounds.getHeight());
        if(stage.getWidth() > screenBounds.getWidth()) stage.setWidth(screenBounds.getWidth() * 0.7);

        SceneManager.setPrimaryStage(stage);
        stage.setTitle("Library Login Test");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
