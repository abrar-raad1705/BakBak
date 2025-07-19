package com.bakbak.javafx_proj_1_2;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SearchResultCell extends ListCell<String> {
    private HBox cellBox;
    private StackPane avatarContainer;
    private Circle avatar;
    private Label avatarLabel;
    private Circle onlineIndicator;
    private VBox contentBox;
    private Label nameLabel;
    private Label statusLabel;

    public SearchResultCell() {
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
        avatar = new Circle(18);
        avatar.setFill(Color.web("#667eea"));
        
        avatarLabel = new Label();
        avatarLabel.setTextFill(Color.WHITE);
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        onlineIndicator = new Circle(5);
        onlineIndicator.setFill(Color.web("#38a169"));
        onlineIndicator.setStroke(Color.WHITE);
        onlineIndicator.setStrokeWidth(2);
        StackPane.setAlignment(onlineIndicator, Pos.BOTTOM_RIGHT);
        
        avatarContainer.getChildren().addAll(avatar, avatarLabel, onlineIndicator);

        // Content box
        contentBox = new VBox();
        contentBox.setSpacing(1);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        nameLabel = new Label();
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.getStyleClass().add("search-result-name");
        
        statusLabel = new Label();
        statusLabel.setFont(Font.font("System", 11));
        statusLabel.getStyleClass().add("search-result-status");
        
        contentBox.getChildren().addAll(nameLabel, statusLabel);
        cellBox.getChildren().addAll(avatarContainer, contentBox);
    }

    @Override
    protected void updateItem(String userInfo, boolean empty) {
        super.updateItem(userInfo, empty);

        if (empty || userInfo == null) {
            setGraphic(null);
            getStyleClass().removeAll("search-result-cell");
        } else {
            updateSearchResultDisplay(userInfo);
            setGraphic(cellBox);
            getStyleClass().add("search-result-cell");
        }
    }

    private void updateSearchResultDisplay(String userInfo) {
        // Parse the user info (format: "username (online)" or "username (offline)")
        String[] parts = userInfo.split(" \\(");
        String username = parts[0];
        boolean isOnline = parts.length > 1 && parts[1].startsWith("online");

        // Update avatar
        String initials = username.length() >= 2 ? 
            username.substring(0, 2).toUpperCase() : 
            username.toUpperCase();
        avatarLabel.setText(initials);

        // Update online status
        onlineIndicator.setVisible(isOnline);
        if (isOnline) {
            onlineIndicator.setFill(Color.web("#38a169"));
        } else {
            onlineIndicator.setFill(Color.web("#e53e3e"));
        }

        // Update name
        nameLabel.setText(username);

        // Update status
        statusLabel.setText(isOnline ? "Online" : "Offline");
        statusLabel.setTextFill(isOnline ? Color.web("#38a169") : Color.web("#718096"));
    }
} 