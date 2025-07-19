package com.bakbak.javafx_proj_1_2;

import com.bakbak.javafx_proj_1_2.ChatClient; // <-- IMPORTANT: Change packages
import com.bakbak.javafx_proj_1_2.Message;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class ChatController implements MessageListener {

    @FXML private Label userNameLabel;
    @FXML private TextField searchField;
    @FXML private ListView chatListView;
    @FXML private Button newChatButton;
    @FXML private Button newGroupButton;
    @FXML private Button settingsButton;
    @FXML private Label chatTitleLabel;
    @FXML private Label chatSubtitleLabel;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private String username;
    private ChatClient chatClient;
    private String currentChatRecipient; // Track who we're currently chatting with
    private ConversationManager conversationManager;
    private boolean isSearchMode = false;
    private ObservableList<Conversation> conversationList;
    private ObservableList<String> searchResultsList;

    public void initData(String username, ChatClient client) {
        this.username = username;
        this.userNameLabel.setText(username);
        this.chatClient = client;
        this.chatClient.setMessageListener(this);
        
        // Initialize conversation manager
        this.conversationManager = new ConversationManager(username);
        
        // Initialize observable lists
        this.conversationList = FXCollections.observableArrayList();
        this.searchResultsList = FXCollections.observableArrayList();
        
        // Set default to conversation mode
        switchToConversationMode();
    }

    @FXML
    public void initialize() {
        // Automatically scroll to the bottom when a new message is added
        messagesContainer.heightProperty().addListener(observable -> messagesScrollPane.setVvalue(1D));

        sendButton.setOnAction(event -> handleSendMessage());
        messageField.setOnAction(event -> handleSendMessage()); // Allow sending with Enter key

        newGroupButton.setOnAction(event -> handleCreateGroup());
        
        // Add search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                switchToSearchMode();
                handleUserSearch(newValue.trim());
            } else {
                switchToConversationMode();
            }
        });
        
        // Handle chat list selection for starting conversations
        chatListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double click
                Object selectedItem = chatListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    if (isSearchMode) {
                        handleStartConversationFromSearch((String) selectedItem);
                    } else {
                        handleStartConversationFromList((Conversation) selectedItem);
                    }
                }
            }
        });
        
        // Settings button
        if (settingsButton != null) {
            settingsButton.setOnAction(event -> showSettingsDialog());
        }
    }

    private void handleSendMessage() {
        String content = messageField.getText();
        if (content.isEmpty()) {
            return;
        }

        if (currentChatRecipient == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a user to chat with first.").show();
            return;
        }

        Message message = new Message(Message.MessageType.PRIVATE_MESSAGE, this.username);
        message.setRecipient(currentChatRecipient);
        message.setContent(content);

        try {
            chatClient.sendMessage(message);
            
            // Add message to conversation manager
            conversationManager.addMessage(message);
            
            // Add our own message to the view
            addMessageToView(message, true);
            
            // Refresh conversation list
            if (!isSearchMode) {
                refreshConversationList();
            }
            
            messageField.clear();
        } catch (IOException e) {
            // Show an error, maybe in a dialog
            new Alert(Alert.AlertType.ERROR, "Failed to send message.").show();
        }
    }

    private void handleCreateGroup() {
        TextInputDialog dialog = new TextInputDialog("New Group");
        dialog.setTitle("Create New Group");
        dialog.setHeaderText("Enter a name for your new group.");
        dialog.setContentText("Group Name:");

        dialog.showAndWait().ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                Message createGroupMsg = new Message(Message.MessageType.CREATE_GROUP, this.username);
                createGroupMsg.setContent(groupName);
                try {
                    chatClient.sendMessage(createGroupMsg);
                } catch (IOException e) {
                    new Alert(Alert.AlertType.ERROR, "Failed to create group.").show();
                }
            }
        });
    }

    private void handleUserSearch(String searchQuery) {
        Message searchMessage = new Message(Message.MessageType.USER_SEARCH, this.username);
        searchMessage.setContent(searchQuery);
        
        try {
            chatClient.sendMessage(searchMessage);
        } catch (IOException e) {
            System.err.println("Failed to send search request: " + e.getMessage());
        }
    }
    
    private void switchToSearchMode() {
        isSearchMode = true;
        chatListView.setItems(searchResultsList);
        chatListView.setCellFactory(listView -> new SearchResultCell());
    }
    
    private void switchToConversationMode() {
        isSearchMode = false;
        chatListView.setItems(conversationList);
        chatListView.setCellFactory(listView -> new ConversationCell());
        refreshConversationList();
    }
    
    private void refreshConversationList() {
        List<Conversation> conversations = conversationManager.getAllConversations();
        conversationList.clear();
        conversationList.addAll(conversations);
    }
    
    private void handleStartConversationFromSearch(String selectedItem) {
        // Extract username from the selected item (remove status info)
        String targetUser = selectedItem.split(" \\(")[0]; // Remove "(online)" or "(offline)"
        
        // Create or get conversation
        Conversation conversation = conversationManager.getOrCreateConversation(targetUser);
        
        // Switch back to conversation mode and select this conversation
        searchField.clear(); // This will trigger switchToConversationMode
        
        // Open the conversation
        openConversation(conversation);
    }
    
    private void handleStartConversationFromList(Conversation conversation) {
        openConversation(conversation);
    }
    
    private void openConversation(Conversation conversation) {
        // Set this user as the current chat recipient
        currentChatRecipient = conversation.getContactUsername();
        
        // Mark conversation as read
        conversationManager.markConversationAsRead(currentChatRecipient);
        
        // Update chat header
        chatTitleLabel.setText(currentChatRecipient);
        chatSubtitleLabel.setText(conversation.isOnline() ? "Online" : "Last seen recently");
        
        // Load local message history first
        loadMessageHistory(conversation);
        
        // Request conversation history from server to sync any missed messages
        requestConversationHistory(currentChatRecipient);
        
        // Focus the message input field
        messageField.requestFocus();
        
        // Refresh conversation list to update unread counts
        refreshConversationList();
    }
    
    private void requestConversationHistory(String withUser) {
        try {
            Message syncRequest = new Message(Message.MessageType.SYNC_HISTORY, username);
            syncRequest.setContent(withUser);
            chatClient.sendMessage(syncRequest);
        } catch (IOException e) {
            System.err.println("Failed to request conversation history: " + e.getMessage());
        }
    }
    
    private void loadMessageHistory(Conversation conversation) {
        messagesContainer.getChildren().clear();
        
        for (Message message : conversation.getMessages()) {
            boolean isOwnMessage = message.getSender().equals(username);
            addMessageToView(message, isOwnMessage);
        }
    }
    
    private void handleSearchResults(String searchResults) {
        searchResultsList.clear();
        
        if (searchResults != null && !searchResults.trim().isEmpty()) {
            String[] users = searchResults.split("\\|");
            for (String user : users) {
                if (!user.trim().isEmpty()) {
                    searchResultsList.add(user.trim());
                }
            }
        }
    }
    
    private void handleOfflineMessagesNotification(String content) {
        System.out.println("Offline messages notification: " + content);
        // Refresh conversation list to show new messages
        if (!isSearchMode) {
            refreshConversationList();
        }
    }
    
    private void handleMessageHistory(String serializedMessage) {
        // Parse the serialized message from server
        String[] parts = serializedMessage.split("\\|");
        if (parts.length >= 4) {
            // Create a message object from the serialized data
            Message historyMessage = new Message(Message.MessageType.PRIVATE_MESSAGE, parts[0]);
            historyMessage.setRecipient(parts[1]);
            historyMessage.setContent(parts[2]);
            
            // Add to conversation manager
            conversationManager.addMessage(historyMessage);
        }
    }
    
    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Application Settings");
        
        // Create the content
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        
        // Dark mode toggle
        HBox darkModeRow = new HBox(10);
        darkModeRow.setAlignment(Pos.CENTER_LEFT);
        
        Label darkModeLabel = new Label("Dark Mode:");
        darkModeLabel.setFont(javafx.scene.text.Font.font("System", 14));
        
        CheckBox darkModeToggle = new CheckBox();
        darkModeToggle.setSelected(isDarkModeEnabled());
        darkModeToggle.setOnAction(e -> toggleDarkMode(darkModeToggle.isSelected()));
        
        darkModeRow.getChildren().addAll(darkModeLabel, darkModeToggle);
        
        content.getChildren().addAll(darkModeRow);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        dialog.showAndWait();
    }
    
    private boolean isDarkModeEnabled() {
        return chatListView.getScene().getRoot().getStyleClass().contains("dark-mode");
    }
    
    private void toggleDarkMode(boolean enable) {
        javafx.scene.Scene scene = chatListView.getScene();
        if (enable) {
            if (!scene.getRoot().getStyleClass().contains("dark-mode")) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark-mode");
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case PRIVATE_MESSAGE:
                case GROUP_MESSAGE:
                    handleIncomingMessage(message);
                    break;
                case USER_SEARCH:
                    handleSearchResults(message.getContent());
                    break;
                case OFFLINE_MESSAGES:
                    handleOfflineMessagesNotification(message.getContent());
                    break;
                case MESSAGE_HISTORY:
                    handleMessageHistory(message.getContent());
                    break;
                case ACKNOWLEDGMENT:
                    // You could show a small status update for acknowledgments
                    System.out.println("Server ACK: " + message.getContent());
                    break;
                case ERROR:
                    new Alert(Alert.AlertType.ERROR, message.getErrorMessage()).show();
                    break;
                // Add cases for USER_LIST, GROUP_LIST etc. to update the UI
            }
        });
    }
    
    private void handleIncomingMessage(Message message) {
        // Add message to conversation manager
        conversationManager.addMessage(message);
        
        // If this message is for the currently open conversation, show it
        if (currentChatRecipient != null && currentChatRecipient.equals(message.getSender())) {
            addMessageToView(message, false);
        }
        
        // Refresh conversation list to show new message and update unread counts
        if (!isSearchMode) {
            refreshConversationList();
        }
        
        // Show notification for new message if not in current conversation
        if (currentChatRecipient == null || !currentChatRecipient.equals(message.getSender())) {
            showMessageNotification(message.getSender(), message.getContent());
        }
    }
    
    private void showMessageNotification(String sender, String content) {
        // Simple notification - in a real app you might use system notifications
        String preview = content.length() > 30 ? content.substring(0, 30) + "..." : content;
        System.out.println("New message from " + sender + ": " + preview);
    }

    private void addMessageToView(Message message, boolean isOwnMessage) {
        HBox messageWrapper = new HBox();
        Label messageLabel = new Label(message.getContent());
        messageLabel.setWrapText(true);

        if (isOwnMessage) {
            messageLabel.getStyleClass().add("message-bubble-own");
            messageWrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // For group messages, you might want a sender label
            VBox otherMessageContainer = new VBox(5);
            Label senderLabel = new Label(message.getSender());
            senderLabel.getStyleClass().add("message-sender");

            messageLabel.getStyleClass().add("message-bubble-other");
            otherMessageContainer.getChildren().addAll(senderLabel, messageLabel);
            messageWrapper.setAlignment(Pos.CENTER_LEFT);
            messageWrapper.getChildren().add(otherMessageContainer);
        }

        if(isOwnMessage) {
            messageWrapper.getChildren().add(messageLabel);
        }

        messagesContainer.getChildren().add(messageWrapper);
    }
}