package com.bakbak.javafx_proj_1_2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Get the visual bounds of the primary screen
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Calculate 60% of the screen width and 70% of the screen height
        double sceneWidth = screenBounds.getWidth() * 0.6;
        double sceneHeight = screenBounds.getHeight() * 0.7;

        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("chat-view.fxml"));

        // Use the calculated dimensions for the scene
        Scene scene = new Scene(fxmlLoader.load(), sceneWidth, sceneHeight);

        stage.setTitle("LAN Messenger");
        stage.setScene(scene);

        // Ensure the network service is shut down when the app closes
        ChatController controller = fxmlLoader.getController();
        stage.setOnHidden(e -> controller.shutdown());

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}