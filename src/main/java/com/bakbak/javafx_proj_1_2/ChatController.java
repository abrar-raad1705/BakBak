package com.bakbak.javafx_proj_1_2;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatController {

    // Existing FXML elements
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> contactsListView;
    @FXML private Label chatPartnerNameLabel;
    @FXML private Button sendButton;

    // New beautiful UI elements
    @FXML private Label userAvatarLabel;
    @FXML private Label statusLabel;
    @FXML private Button callButton;
    @FXML private Button videoButton;
    @FXML private Button moreButton;
    @FXML private HBox typingIndicator;
    @FXML private Label typingDots;
    @FXML private Button attachButton;
    @FXML private Button emojiButton;

    // Existing functionality
    private NetworkService networkService;
    private String username;

    // New UI functionality
    private Timeline typingAnimation;
    private boolean isTyping = false;

    @FXML
    public void initialize() {
        // Existing network initialization
        try {
            this.username = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            this.username = "User";
        }

        try {
            this.networkService = new NetworkService(message -> {
                Platform.runLater(() -> {
                    // Enhanced message display with timestamp
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String formattedMessage = "[" + timestamp + "] " + message;
                    chatArea.appendText(formattedMessage + "\n");

                    // Auto-scroll to bottom
                    chatArea.setScrollTop(Double.MAX_VALUE);

                    // Show typing indicator briefly for incoming messages
                    if (!message.startsWith(username + ":")) {
                        showTypingIndicator();
                        Timeline hideTyping = new Timeline(new KeyFrame(Duration.seconds(1), e -> hideTypingIndicator()));
                        hideTyping.play();
                    }
                });
            });
            networkService.startListening();

            // Update UI status
            Platform.runLater(() -> {
                statusLabel.setText("Connected");
                chatPartnerNameLabel.setText("BakBak Chat - " + username);
                updateAvatar();
            });

        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Error: Could not initialize network service.\n");
            statusLabel.setText("Connection Failed");
        }

        // Initialize new UI features
        setupTypingAnimation();
        setupChatArea();

        // Set focus to message input
        Platform.runLater(() -> messageField.requestFocus());
    }

    private void setupTypingAnimation() {
        typingAnimation = new Timeline(
            new KeyFrame(Duration.seconds(0.5), e -> typingDots.setText(".")),
            new KeyFrame(Duration.seconds(1.0), e -> typingDots.setText("..")),
            new KeyFrame(Duration.seconds(1.5), e -> typingDots.setText("..."))
        );
        typingAnimation.setCycleCount(Timeline.INDEFINITE);
    }

    private void setupChatArea() {
        // Enhanced chat area setup
        chatArea.setStyle(chatArea.getStyle() + "; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Welcome message
        Platform.runLater(() -> {
            String welcomeMsg = "=== Welcome to BakBak Chat ===\n";
            welcomeMsg += "Connected as: " + username + "\n";
            welcomeMsg += "Ready to chat!\n\n";
            chatArea.appendText(welcomeMsg);
        });
    }

    private void updateAvatar() {
        // Create avatar initials from username
        if (username.length() >= 2) {
            String[] parts = username.split("\\s+");
            if (parts.length >= 2) {
                userAvatarLabel.setText(parts[0].substring(0, 1).toUpperCase() +
                                       parts[1].substring(0, 1).toUpperCase());
            } else {
                userAvatarLabel.setText(username.substring(0, Math.min(2, username.length())).toUpperCase());
            }
        } else {
            userAvatarLabel.setText(username.toUpperCase());
        }
    }

    @FXML
    protected void onSendMessage() {
        String messageText = messageField.getText();
        if (messageText != null && !messageText.trim().isEmpty()) {
            String fullMessage = username + ": " + messageText;

            System.out.println("DEBUG: Attempting to send message: '" + fullMessage + "'");

            networkService.sendMessage(fullMessage);
            messageField.clear();

            // Focus back to input field
            messageField.requestFocus();
        }
    }

    // New UI Action Methods
    @FXML
    private void onCallAction() {
        showAlert("Voice Call", "Voice call feature coming soon!\nThis would integrate with your network service for voice communication.");
    }

    @FXML
    private void onVideoAction() {
        showAlert("Video Call", "Video call feature coming soon!\nThis would integrate with your network service for video communication.");
    }

    @FXML
    private void onMoreAction() {
        showAlert("More Options",
            "Additional features:\n" +
            "• File sharing\n" +
            "• Chat history\n" +
            "• User settings\n" +
            "• Network status\n\n" +
            "Current Network Status: " + (networkService != null ? "Connected" : "Disconnected"));
    }

    @FXML
    private void onAttachAction() {
        showAlert("File Attachment", "File sharing feature coming soon!\nThis would allow sending files through your network service.");
    }

    @FXML
    private void onEmojiAction() {
        // Insert a random emoji
        String[] emojis = {"😊", "😂", "❤️", "👍", "🎉", "🔥", "✨", "💯", "🚀", "💪"};
        String emoji = emojis[(int) (Math.random() * emojis.length)];

        // Insert emoji at current cursor position
        String currentText = messageField.getText();
        int caretPos = messageField.getCaretPosition();
        String newText = currentText.substring(0, caretPos) + emoji + currentText.substring(caretPos);
        messageField.setText(newText);
        messageField.positionCaret(caretPos + emoji.length());
        messageField.requestFocus();
    }

    private void showTypingIndicator() {
        if (!isTyping) {
            isTyping = true;
            typingIndicator.setVisible(true);
            typingIndicator.setManaged(true);
            typingAnimation.play();
        }
    }

    private void hideTypingIndicator() {
        if (isTyping) {
            isTyping = false;
            typingIndicator.setVisible(false);
            typingIndicator.setManaged(false);
            typingAnimation.stop();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Existing shutdown method
    public void shutdown() {
        if (networkService != null) {
            networkService.stopListening();
        }
        if (typingAnimation != null) {
            typingAnimation.stop();
        }
    }

    // Public methods for external interaction (if needed)
    public void setStatus(String status) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(status));
        }
    }

    public void addSystemMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            chatArea.appendText("[" + timestamp + "] SYSTEM: " + message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}