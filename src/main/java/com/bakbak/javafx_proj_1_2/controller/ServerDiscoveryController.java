package com.bakbak.javafx_proj_1_2.controller;

import com.bakbak.javafx_proj_1_2.ChatApplication;
import com.bakbak.javafx_proj_1_2.ChatClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.bakbak.javafx_proj_1_2.ChatServer.PORT;

public class ServerDiscoveryController implements Initializable {

    @FXML private Button scanButton;
    @FXML private Label statusLabel;
    @FXML private ListView<String> serverListView;
    @FXML private Button localhostButton;
    @FXML private TextField ipField;
    @FXML private Button connectButton;

    private ChatClient chatClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
    }

    private void setupUI() {
        // Configure server list view
        serverListView.setCellFactory(TextFieldListCell.forListView());
        serverListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedServer = serverListView.getSelectionModel().getSelectedItem();
                if (selectedServer != null) {
                    connectToServer(selectedServer);
                }
            }
        });

        // Setup IP field validation
        ipField.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        connectButton.setDisable(true);
    }

    @FXML
    private void handleScan() {
        scanButton.setDisable(true);
        statusLabel.setText("Scanning for servers...");
        serverListView.getItems().clear();
        serverListView.setVisible(false);
        serverListView.setManaged(false);

        // Run server discovery in background
        Task<List<String>> discoveryTask = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                return ChatClient.discoverServers(3000); // 3 second timeout
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<String> servers = getValue();
                    if (servers.isEmpty()) {
                        statusLabel.setText("No servers found on LAN. Try manual connection.");
                    } else {
                        statusLabel.setText("Found " + servers.size() + " server(s). Double-click to connect.");
                        serverListView.getItems().addAll(servers);
                        serverListView.setVisible(true);
                        serverListView.setManaged(true);
                        
                        // Auto-resize window to accommodate the server list
                        resizeWindowForServerList();
                    }
                    scanButton.setDisable(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Scan failed. Try manual connection.");
                    scanButton.setDisable(false);
                });
            }
        };

        new Thread(discoveryTask).start();
    }

    @FXML
    private void handleLocalhost() {
        connectToServer("localhost");
    }

    @FXML
    private void handleConnect() {
        String serverIP = ipField.getText().trim();
        if (!serverIP.isEmpty()) {
            connectToServer(serverIP);
        }
    }

    private void connectToServer(String serverAddress) {
        connectButton.setDisable(true);
        localhostButton.setDisable(true);
        scanButton.setDisable(true);
        statusLabel.setText("Connecting to " + serverAddress + "...");

        Task<Boolean> connectionTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                chatClient = new ChatClient(serverAddress, PORT);
                return chatClient.connect();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean connected = getValue();
                    if (connected) {
                        statusLabel.setText("Connected successfully! Redirecting to login...");
                        
                        // Store the client instance in the application
                        ChatApplication.setChatClient(chatClient);
                        
                        // Navigate to login screen
                        try {
                            ChatApplication.showLoginView();
                        } catch (IOException e) {
                            showError("Failed to load login screen: " + e.getMessage());
                        }
                    } else {
                        showConnectionError("Failed to connect to server at " + serverAddress);
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showConnectionError("Connection failed: " + getException().getMessage());
                });
            }
        };

        new Thread(connectionTask).start();
    }

    private void showConnectionError(String message) {
        statusLabel.setText(message);
        connectButton.setDisable(false);
        localhostButton.setDisable(false);
        scanButton.setDisable(false);
        
        // Show alert
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("Failed to connect to server");
        alert.setContentText(message + "\n\nPlease check:\n• Server is running\n• IP address is correct\n• Network connectivity");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
        
        statusLabel.setText("Ready to scan for servers");
        connectButton.setDisable(false);
        localhostButton.setDisable(false);
        scanButton.setDisable(false);
    }

    private void resizeWindowForServerList() {
        // Get the stage (window) from any scene node
        Stage stage = (Stage) serverListView.getScene().getWindow();
        
        // Store current dimensions
        double currentWidth = stage.getWidth();
        double currentHeight = stage.getHeight();
        
        // Calculate required height based on number of servers
        int serverCount = serverListView.getItems().size();
        double listItemHeight = 24; // Approximate height per list item
        double listPadding = 40; // Padding and borders
        double requiredListHeight = Math.min(serverCount * listItemHeight + listPadding, 150); // Cap at 150px
        
        // Calculate new window height (add extra space for the list)
        double additionalHeight = requiredListHeight + 20; // Extra margin
        double newHeight = Math.max(currentHeight + additionalHeight, 500); // Minimum height of 500
        
        // Smoothly resize the window
        stage.setHeight(newHeight);
        
        // Center the window on screen after resize
        stage.centerOnScreen();
    }
}
