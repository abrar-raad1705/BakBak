package com.bakbak.javafx_proj_1_2;

import com.bakbak.javafx_proj_1_2.ChatClient; // <-- IMPORTANT: Change packages
import com.bakbak.javafx_proj_1_2.Message;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class LoginController implements MessageListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private ChatClient chatClient;

    public void initClient(ChatClient client) {
        this.chatClient = client;
        this.chatClient.setMessageListener(this);
    }

    @FXML
    public void initialize() {
        loginButton.setOnAction(event -> handleLogin());
        registerButton.setOnAction(event -> handleRegister());
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password cannot be empty.", true);
            return;
        }

        setLoading(true);
        Message loginMessage = new Message(Message.MessageType.LOGIN, username);
        loginMessage.setContent(password);

        try {
            chatClient.sendMessage(loginMessage);
        } catch (IOException e) {
            setLoading(false);
            showStatus("Error: Could not connect to server.", true);
        }
    }

    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password cannot be empty.", true);
            return;
        }

        setLoading(true);
        Message registerMessage = new Message(Message.MessageType.REGISTER, username);
        registerMessage.setContent(password);
        try {
            chatClient.sendMessage(registerMessage);
        } catch (IOException e) {
            setLoading(false);
            showStatus("Error: Could not connect to server.", true);
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            setLoading(false);
            if (message.getType() == Message.MessageType.ACKNOWLEDGMENT) {
                if (message.isSuccess()) {
                    // Check if this was a login success
                    if (message.getContent().startsWith("Login successful")) {
                        try {
                            ChatApplication.showChatView(usernameField.getText());
                        } catch (IOException e) {
                            showStatus("Error loading chat view.", true);
                        }
                    } else {
                        // Registration success
                        showStatus(message.getContent(), false);
                    }
                } else {
                    // Login or Register failure
                    showStatus(message.getContent(), true);
                }
            } else if (message.getType() == Message.MessageType.ERROR) {
                showStatus(message.getErrorMessage(), true);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        loginButton.setDisable(isLoading);
        registerButton.setDisable(isLoading);
    }

    private void showStatus(String text, boolean isError) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("success-label", "error-label");
        statusLabel.getStyleClass().add(isError ? "error-label" : "success-label");
        statusLabel.setVisible(true);
    }
}