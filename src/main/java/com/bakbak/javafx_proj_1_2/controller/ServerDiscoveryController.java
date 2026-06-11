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
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.Scene;

import static com.bakbak.javafx_proj_1_2.ChatServer.PORT;

public class ServerDiscoveryController implements Initializable {

    @FXML private Button scanButton;
    @FXML private Label statusLabel;
    @FXML private ListView<String> serverListView;
    @FXML private TextField ipField;
    @FXML private Button connectButton;
    @FXML private ToggleButton darkModeToggle;
    @FXML private ImageView darkModeIcon;

    private ChatClient chatClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupButtonHoverScale(scanButton);
        setupButtonHoverScale(connectButton);
        
        if (darkModeToggle != null) {
            boolean isDark = ChatApplication.isDarkModeEnabled();
            darkModeToggle.setSelected(isDark);
            updateDarkModeIcon(isDark);
            darkModeToggle.setOnAction(e -> handleDarkModeToggle());
        }
    }

    private void setupButtonHoverScale(Button button) {
        // Zoom/scale animation disabled
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
        statusLabel.setStyle(""); // Reset error coloring
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
    private void handleConnect() {
        String serverIP = ipField.getText().trim();
        if (!serverIP.isEmpty()) {
            connectToServer(serverIP);
        }
    }

    private void connectToServer(String serverAddress) {
        connectButton.setDisable(true);
        scanButton.setDisable(true);
        statusLabel.setStyle(""); // Reset error coloring
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
        statusLabel.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
        statusLabel.setText("IP doesn't exist or can't be reached. Please check the address.");
        connectButton.setDisable(false);
        scanButton.setDisable(false);
        ChatApplication.showToast("Connection failed: IP doesn't exist or can't be reached.");
    }

    private void showError(String message) {
        ChatApplication.showToast(message);
        
        statusLabel.setStyle("");
        statusLabel.setText("Ready to scan for servers");
        connectButton.setDisable(false);
        scanButton.setDisable(false);
    }

    private void handleDarkModeToggle() {
        if (darkModeToggle == null) return;
        boolean enabled = darkModeToggle.isSelected();
        ChatApplication.setDarkModeEnabled(enabled);
        
        Scene scene = darkModeToggle.getScene();
        if (scene != null) {
            if (enabled) {
                if (!scene.getRoot().getStyleClass().contains("dark-mode")) {
                    scene.getRoot().getStyleClass().add("dark-mode");
                }
            } else {
                scene.getRoot().getStyleClass().remove("dark-mode");
            }
        }
        updateDarkModeIcon(enabled);
    }

    private void updateDarkModeIcon(boolean isDark) {
        if (darkModeIcon != null) {
            String imagePath = isDark ? "/com/bakbak/javafx_proj_1_2/icons/night2.png" : "/com/bakbak/javafx_proj_1_2/icons/sun.png";
            try {
                darkModeIcon.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Failed to update dark mode icon: " + e.getMessage());
            }
        }
    }


}
