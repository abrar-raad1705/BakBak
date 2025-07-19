package com.bakbak.javafx_proj_1_2;

import com.bakbak.javafx_proj_1_2.ChatClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatApplication extends Application {

    private static Stage primaryStage;
    private static ChatClient chatClient;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        chatClient = new ChatClient("localhost", 22222); // Configure server host/port
        try {
            chatClient.connect();
            showLoginView();
        } catch (IOException e) {
            System.err.println("Failed to connect to the server: " + e.getMessage());
            // Optionally show an alert to the user
        }
    }

    public static void showLoginView() throws IOException {
        FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("login.fxml"));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.initClient(chatClient);

        Scene scene = new Scene(root, 400, 600);
        // ADD THIS LINE to apply the stylesheet
        scene.getStylesheets().add(ChatApplication.class.getResource("style.css").toExternalForm());

        primaryStage.setTitle("Telegram - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showChatView(String username) throws IOException {
        FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("chat.fxml"));
        Parent root = loader.load();

        ChatController controller = loader.getController();
        controller.initData(username, chatClient);

        Scene scene = new Scene(root, 1000, 700);
        // ADD THIS LINE to apply the stylesheet
        scene.getStylesheets().add(ChatApplication.class.getResource("style.css").toExternalForm());

        primaryStage.setTitle("Telegram");
        primaryStage.setScene(scene);

        // Handle window close request to disconnect gracefully
        primaryStage.setOnCloseRequest(event -> {
            chatClient.disconnect();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}