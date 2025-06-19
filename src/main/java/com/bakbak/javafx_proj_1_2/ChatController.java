package com.bakbak.javafx_proj_1_2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ChatController {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    private NetworkService networkService;
    private String username;

    @FXML
    public void initialize() {
        try {
            // Create a simple username based on the computer name
            this.username = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            this.username = "User"; // Fallback username
        }

        try {
            // The lambda expression (message) -> ... is the callback.
            // It gets executed every time the network service receives a message.
            this.networkService = new NetworkService(message -> {
                // UI updates must be run on the JavaFX Application Thread.
                Platform.runLater(() -> {
                    chatArea.appendText(message + "\n");
                });
            });
            // Start listening for messages
            networkService.startListening();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle initialization error (e.g., show an alert to the user)
            chatArea.appendText("Error: Could not initialize network service.");
        }
    }

    @FXML
    protected void onSendMessage() {
        String messageText = messageField.getText();
        if (messageText != null && !messageText.trim().isEmpty()) {
            // Prepend username to the message
            String fullMessage = username + ": " + messageText;
            networkService.sendMessage(fullMessage);
            messageField.clear();
        }
    }

    // Method to be called to cleanly shut down the network listener
    public void shutdown() {
        if (networkService != null) {
            networkService.stopListening();
        }
    }
}