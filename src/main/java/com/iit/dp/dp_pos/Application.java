package com.iit.dp.dp_pos;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                Application.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        // Set the stage on the MainController
        com.iit.dp.dp_pos.controller.MainController mainController = fxmlLoader.getController();
        mainController.setStage(stage);
        stage.setTitle("Supershop App");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}