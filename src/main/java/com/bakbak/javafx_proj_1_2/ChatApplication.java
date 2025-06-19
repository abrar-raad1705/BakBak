package com.bakbak.javafx_proj_1_2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("chat-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
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