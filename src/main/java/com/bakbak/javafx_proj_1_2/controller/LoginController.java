package com.bakbak.javafx_proj_1_2.controller;

import com.bakbak.javafx_proj_1_2.ChatApplication;
import com.bakbak.javafx_proj_1_2.ChatClient;
import com.bakbak.javafx_proj_1_2.Message;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LoginController implements Initializable {

    @FXML private Button backButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    private ChatClient chatClient;
    private CompletableFuture<Message> pendingResponse;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatClient = ChatApplication.getChatClient();
        setupUI();
        setupMessageHandler();
    }

    private void setupUI() {
        // Enable/disable buttons based on input
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> updateButtonStates());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> updateButtonStates());
        
        updateButtonStates();
        
        // Enter key handling
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
    }

    private void updateButtonStates() {
        boolean hasInput = !usernameField.getText().trim().isEmpty() && 
                          !passwordField.getText().trim().isEmpty();
        loginButton.setDisable(!hasInput);
        registerButton.setDisable(!hasInput);
    }

    private void setupMessageHandler() {
        if (chatClient != null) {
            chatClient.setMessageHandler(this::handleServerMessage);
        }
    }

    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            if (message.getType() == Message.MessageType.ACKNOWLEDGMENT && pendingResponse != null) {
                pendingResponse.complete(message);
                pendingResponse = null;
            }
        });
    }

    @FXML
    private void handleBack() {
        if (chatClient != null) {
            chatClient.disconnect();
        }
        try {
            ChatApplication.showServerDiscoveryView();
        } catch (IOException e) {
            showError("Failed to return to server discovery: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        performLogin(username, password);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters long");
            return;
        }

        if (password.length() < 4) {
            showError("Password must be at least 4 characters long");
            return;
        }

        performRegistration(username, password);
    }

    private void performLogin(String username, String password) {
        setUIEnabled(false);
        statusLabel.setText("Logging in...");

        Task<Boolean> loginTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Check if we need to reconnect (socket might be closed after logout)
                if (chatClient == null || !chatClient.isConnected()) {
                    // Get the server host from the existing client or default to localhost
                    String serverHost = (chatClient != null) ? chatClient.getHost() : "localhost";
                    
                    // Create new connection
                    chatClient = new ChatClient(serverHost, 12345); // Use default port
                    if (!chatClient.connect()) {
                        throw new Exception("Failed to reconnect to server");
                    }
                    
                    // Update the application's chat client reference
                    ChatApplication.setChatClient(chatClient);
                    
                    // Setup message handler for this controller
                    chatClient.setMessageHandler(LoginController.this::handleServerMessage);
                }
                
                Message loginMessage = new Message(Message.MessageType.LOGIN, username);
                loginMessage.setContent(password);

                pendingResponse = new CompletableFuture<>();
                chatClient.sendMessage(loginMessage);

                // Wait for response with timeout
                try {
                    Message response = pendingResponse.get(10, TimeUnit.SECONDS);
                    return response.isSuccess();
                } catch (Exception e) {
                    System.err.println("Login timeout or error: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean success = getValue();
                    if (success) {
                        statusLabel.setText("Login successful! Opening chat...");
                        try {
                            ChatApplication.showChatView(username);
                        } catch (IOException e) {
                            showError("Failed to open chat window: " + e.getMessage());
                            setUIEnabled(true);
                        }
                    } else {
                        showError("Login failed. Please check your credentials.");
                        setUIEnabled(true);
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("Login failed: " + getException().getMessage());
                    setUIEnabled(true);
                });
            }
        };

        new Thread(loginTask).start();
    }

    private void performRegistration(String username, String password) {
        setUIEnabled(false);
        statusLabel.setText("Creating account...");

        Task<Boolean> registerTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Message registerMessage = new Message(Message.MessageType.REGISTER, username);
                registerMessage.setContent(password);

                pendingResponse = new CompletableFuture<>();
                chatClient.sendMessage(registerMessage);

                // Wait for response with timeout
                try {
                    Message response = pendingResponse.get(10, TimeUnit.SECONDS);
                    return response.isSuccess();
                } catch (Exception e) {
                    System.err.println("Registration timeout or error: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean success = getValue();
                    if (success) {
                        statusLabel.setText("Account created successfully! You can now log in.");
                        showInfo("Registration Successful", "Your account has been created. You can now log in with your credentials.");
                    } else {
                        showError("Registration failed. Username may already exist.");
                    }
                    setUIEnabled(true);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("Registration failed: " + getException().getMessage());
                    setUIEnabled(true);
                });
            }
        };

        new Thread(registerTask).start();
    }

    private void setUIEnabled(boolean enabled) {
        loginButton.setDisable(!enabled);
        registerButton.setDisable(!enabled);
        usernameField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        backButton.setDisable(!enabled);
        
        if (enabled) {
            updateButtonStates();
        }
    }

    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
