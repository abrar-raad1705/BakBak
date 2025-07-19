package com.bakbak.javafx_proj_1_2;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ConversationCell extends ListCell<Conversation> {
    private HBox cellBox;
    private StackPane avatarContainer;
    private Circle avatar;
    private Label avatarLabel;
    private Circle onlineIndicator;
    private VBox contentBox;
    private Label nameLabel;
    private Label messageLabel;
    private Label timeLabel;
    private Label unreadBadge;

    public ConversationCell() {
        createCell();
    }

    private void createCell() {
        // Main container
        cellBox = new HBox();
        cellBox.setAlignment(Pos.CENTER_LEFT);
        cellBox.setPadding(new Insets(8, 12, 8, 12));
        cellBox.setSpacing(12);

        // Avatar container
        avatarContainer = new StackPane();
        avatar = new Circle(20);
        avatar.setFill(Color.web("#4299e1"));
        
        avatarLabel = new Label();
        avatarLabel.setTextFill(Color.WHITE);
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        onlineIndicator = new Circle(6);
        onlineIndicator.setFill(Color.web("#38a169"));
        onlineIndicator.setStroke(Color.WHITE);
        onlineIndicator.setStrokeWidth(2);
        StackPane.setAlignment(onlineIndicator, Pos.BOTTOM_RIGHT);
        
        avatarContainer.getChildren().addAll(avatar, avatarLabel, onlineIndicator);

        // Content box
        contentBox = new VBox();
        contentBox.setSpacing(2);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Name and time row
        HBox nameTimeRow = new HBox();
        nameTimeRow.setAlignment(Pos.CENTER_LEFT);
        
        nameLabel = new Label();
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.getStyleClass().add("conversation-name");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        timeLabel = new Label();
        timeLabel.setFont(Font.font("System", 11));
        timeLabel.getStyleClass().add("conversation-time");
        
        nameTimeRow.getChildren().addAll(nameLabel, spacer, timeLabel);

        // Message and unread row
        HBox messageUnreadRow = new HBox();
        messageUnreadRow.setAlignment(Pos.CENTER_LEFT);
        
        messageLabel = new Label();
        messageLabel.setFont(Font.font("System", 12));
        messageLabel.getStyleClass().add("conversation-message");
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
        
        unreadBadge = new Label();
        unreadBadge.getStyleClass().add("unread-badge");
        unreadBadge.setMinSize(18, 18);
        unreadBadge.setAlignment(Pos.CENTER);
        unreadBadge.setVisible(false);
        
        messageUnreadRow.getChildren().addAll(messageLabel, unreadBadge);

        contentBox.getChildren().addAll(nameTimeRow, messageUnreadRow);
        cellBox.getChildren().addAll(avatarContainer, contentBox);
    }

    @Override
    protected void updateItem(Conversation conversation, boolean empty) {
        super.updateItem(conversation, empty);

        if (empty || conversation == null) {
            setGraphic(null);
            getStyleClass().removeAll("conversation-cell");
        } else {
            updateConversationDisplay(conversation);
            setGraphic(cellBox);
            getStyleClass().add("conversation-cell");
        }
    }

    private void updateConversationDisplay(Conversation conversation) {
        // Update avatar
        String username = conversation.getContactUsername();
        String initials = username.length() >= 2 ? 
            username.substring(0, 2).toUpperCase() : 
            username.toUpperCase();
        avatarLabel.setText(initials);

        // Update online status
        onlineIndicator.setVisible(conversation.isOnline());

        // Update name
        nameLabel.setText(username);

        // Update time
        timeLabel.setText(formatTime(conversation.getLastActivity()));

        // Update message preview
        messageLabel.setText(conversation.getLastMessagePreview());

        // Update unread badge
        int unreadCount = conversation.getUnreadCount();
        if (unreadCount > 0) {
            unreadBadge.setText(String.valueOf(unreadCount));
            unreadBadge.setVisible(true);
            nameLabel.getStyleClass().add("unread-name");
            messageLabel.getStyleClass().add("unread-message");
        } else {
            unreadBadge.setVisible(false);
            nameLabel.getStyleClass().remove("unread-name");
            messageLabel.getStyleClass().remove("unread-message");
        }
    }

    private String formatTime(java.time.LocalDateTime dateTime) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(dateTime, now);
        
        if (duration.toMinutes() < 1) {
            return "now";
        } else if (duration.toHours() < 1) {
            return duration.toMinutes() + "m";
        } else if (duration.toDays() < 1) {
            return duration.toHours() + "h";
        } else if (duration.toDays() < 7) {
            return duration.toDays() + "d";
        } else {
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));
        }
    }
} 