package com.bakbak.javafx_proj_1_2.controller;

import com.bakbak.javafx_proj_1_2.ChatApplication;
import com.bakbak.javafx_proj_1_2.ChatClient;
import com.bakbak.javafx_proj_1_2.Message;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ChatController implements Initializable {

    @FXML private ToggleButton darkModeToggle;
    @FXML private Button settingsButton;
    @FXML private TextField messageInput1; // Search field
    @FXML private ListView<ChatItem> userListView;
    @FXML private Button logoutButton;
    @FXML private Label chatNameLabel;
    @FXML private Label chatStatusLabel;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Button chatMenuButton;

    private ChatClient chatClient;
    private String currentUsername;
    private ChatItem selectedChat;
    private Set<String> onlineUsers = new HashSet<>();
    private Map<String, String> userGroups = new HashMap<>(); // groupId -> groupName
    private Map<String, ChatItem> allChatItems = new HashMap<>();
    private CompletableFuture<Message> pendingResponse;
    private Timeline lastSeenUpdateTimer;
    
    // Image-based emoji system
    private static final String EMOJI_RESOURCES_PATH = "/com/bakbak/javafx_proj_1_2/emojis/";
    private static final String RECENT_EMOJIS_FILE = "chat_data/recent_emojis.txt";
    private List<String> recentlyUsedEmojis = new ArrayList<>();
    private Map<String, String> emojiNameMap = new HashMap<>(); // filename -> display name

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatClient = ChatApplication.getChatClient();
        setupUI();
        setupMessageHandler();
        loadRecentlyUsedEmojis(); // Load recent emojis
        initializeEmojiSystem(); // Initialize image-based emojis
        startLastSeenUpdateTimer(); // Start timer for last seen updates
        
        // Note: loadContacts() is now called after username is set in setCurrentUsername()
        
        // Initially show "Select a chat" message
        showSelectChatMessage();
    }
    
    private void initializeEmojiSystem() {
        // Initialize emoji name mappings for available emojis
        emojiNameMap.put("1f004.png", "🀄 Mahjong");
        emojiNameMap.put("1f0cf.png", "🃏 Joker");
        emojiNameMap.put("1f170.png", "🅰 A Button");
        emojiNameMap.put("1f171.png", "🅱 B Button");
        emojiNameMap.put("1f17e.png", "🅾 O Button");
        emojiNameMap.put("1f17f.png", "🅿 P Button");
        emojiNameMap.put("1f18e.png", "🆎 AB Button");
        emojiNameMap.put("1f191.png", "🆑 CL Button");
        emojiNameMap.put("1f192.png", "🆒 Cool");
        emojiNameMap.put("1f193.png", "🆓 Free");
        emojiNameMap.put("1f194.png", "🆔 ID Button");
        emojiNameMap.put("1f195.png", "🆕 New");
        emojiNameMap.put("1f196.png", "🆖 NG Button");
        emojiNameMap.put("1f197.png", "🆗 OK Button");
        emojiNameMap.put("1f198.png", "🆘 SOS");
        emojiNameMap.put("1f199.png", "🆙 UP");
        emojiNameMap.put("1f19a.png", "🆚 VS");
        
        // Common symbols that were in the original list
        emojiNameMap.put("2764.png", "❤ Heart");
        emojiNameMap.put("270d.png", "✍ Writing");
        emojiNameMap.put("270c.png", "✌ Peace");
        emojiNameMap.put("270b.png", "✋ Hand");
        emojiNameMap.put("270a.png", "✊ Fist");
        emojiNameMap.put("2709.png", "✉ Envelope");
        emojiNameMap.put("2708.png", "✈ Airplane");
        emojiNameMap.put("2705.png", "✅ Check");
        emojiNameMap.put("2702.png", "✂ Scissors");
        emojiNameMap.put("26fa.png", "⛺ Tent");
        emojiNameMap.put("26fd.png", "⛽ Gas");
        emojiNameMap.put("2b50.png", "⭐ Star");
        emojiNameMap.put("2b55.png", "⭕ Circle");
        emojiNameMap.put("2754.png", "❔ Question");
        emojiNameMap.put("2755.png", "❕ Exclamation");
        emojiNameMap.put("2757.png", "❗ Exclamation Mark");
        emojiNameMap.put("2795.png", "➕ Plus");
        emojiNameMap.put("2796.png", "➖ Minus");
        emojiNameMap.put("2797.png", "➗ Divide");
        emojiNameMap.put("27a1.png", "➡ Right Arrow");
        emojiNameMap.put("2b05.png", "⬅ Left Arrow");
        emojiNameMap.put("2b06.png", "⬆ Up Arrow");
        emojiNameMap.put("2b07.png", "⬇ Down Arrow");
    }
    
    private void startLastSeenUpdateTimer() {
        // Update last seen status every minute
        lastSeenUpdateTimer = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            updateLastSeenStatusForContacts();
        }));
        lastSeenUpdateTimer.setCycleCount(Timeline.INDEFINITE);
        lastSeenUpdateTimer.play();
    }
    
    private void updateLastSeenStatusForContacts() {
        boolean needsUpdate = false;
        for (ChatItem contact : allChatItems.values()) {
            if (contact.getType() == ChatItem.Type.USER && !contact.isOnline()) {
                // Request updated status from server
                try {
                    Message statusRequest = new Message(Message.MessageType.USER_LIST, currentUsername);
                    statusRequest.setContent(contact.getName());
                    chatClient.sendMessage(statusRequest);
                    needsUpdate = true;
                } catch (IOException ex) {
                    System.err.println("Failed to request status update: " + ex.getMessage());
                }
            }
        }
        
        if (needsUpdate) {
            Platform.runLater(() -> updateChatList());
        }
    }

    private void setupUI() {
        // Setup search functionality
        messageInput1.textProperty().addListener((observable, oldValue, newValue) -> filterChatList(newValue));
        
        // Setup chat list selection
        userListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectChat(newValue);
            }
        });

        // Setup send message functionality
        sendButton.setOnAction(e -> handleSendMessage());
        messageInput.setOnAction(e -> handleSendMessage());
        
        // Disable message input initially
        messageInput.setDisable(true);
        sendButton.setDisable(true);
    }

    private void setupMessageHandler() {
        if (chatClient != null) {
            chatClient.setMessageHandler(this::handleServerMessage);
        }
    }

    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case ACKNOWLEDGMENT:
                    if (pendingResponse != null) {
                        pendingResponse.complete(message);
                        pendingResponse = null;
                    }
                    break;
                case PRIVATE_MESSAGE:
                    handleIncomingPrivateMessage(message);
                    break;
                case GROUP_MESSAGE:
                    handleIncomingGroupMessage(message);
                    break;
                case USER_LIST:
                    handleUserListResponse(message);
                    break;
                case USER_SEARCH:
                    handleUserSearchResponse(message);
                    break;
                case GROUP_LIST:
                    handleGroupListResponse(message);
                    break;
                case USER_STATUS_UPDATE:
                    handleUserStatusUpdate(message);
                    break;
            }
        });
    }

    private void loadContacts() {
        // Load contacts from message history instead of all users
        loadContactsFromHistory();
        
        // Still request group list
        requestGroupList();
        
        // Request online status for known contacts
        requestOnlineStatusForContacts();
    }
    
    private void loadContactsFromHistory() {
        allChatItems.clear();
        Set<String> contacts = new HashSet<>();
        Map<String, String> lastMessages = new HashMap<>();
        Map<String, String> lastMessageTimes = new HashMap<>();
        
        // Read message history file for current user
        String messagesFile = "chat_data/messages/" + currentUsername + ".txt";
        System.out.println("DEBUG: Attempting to load contacts from file: " + messagesFile);
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(messagesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    String sender = parts[1];
                    String recipient = parts[2];
                    String messageContent = parts[3];
                    String timestamp = parts[5];
                    
                    // Add contact if they're not the current user
                    String contactName = null;
                    if (!sender.equals(currentUsername)) {
                        contacts.add(sender);
                        contactName = sender;
                    }
                    if (!recipient.equals(currentUsername)) {
                        contacts.add(recipient);
                        contactName = recipient;
                    }
                    
                    // Track last message for this contact (most recent one wins due to file order)
                    if (contactName != null) {
                        // Format message with sender info
                        String displayMessage;
                        String processedContent = convertEmojiPlaceholdersToDisplay(messageContent);
                        if (sender.equals(currentUsername)) {
                            displayMessage = "You: " + processedContent;
                        } else {
                            displayMessage = processedContent;
                        }
                        
                        lastMessages.put(contactName, displayMessage);
                        
                        // Extract time from timestamp
                        try {
                            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(timestamp, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            String timeStr = dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                            lastMessageTimes.put(contactName, timeStr);
                        } catch (Exception e) {
                            lastMessageTimes.put(contactName, "");
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.out.println("DEBUG: No message history found for " + currentUsername + " or error reading file: " + e.getMessage());
            // This is normal for new users
        }
        
        // Create ChatItems for all contacts with their last messages
        for (String contact : contacts) {
            ChatItem contactItem = new ChatItem(contact, ChatItem.Type.USER, false);
            
            // Set last message and timestamp
            if (lastMessages.containsKey(contact)) {
                contactItem.setLastMessage(lastMessages.get(contact));
                contactItem.setLastMessageTimestamp(lastMessageTimes.get(contact));
            }
            
            allChatItems.put(contact, contactItem);
        }
        
        updateChatList();
        System.out.println("DEBUG: Loaded " + contacts.size() + " contacts from message history for " + currentUsername);
        if (!contacts.isEmpty()) {
            System.out.println("DEBUG: Contacts found: " + contacts);
        }
    }
    
    private void requestOnlineStatusForContacts() {
        // Request online status only for known contacts
        if (!allChatItems.isEmpty()) {
            try {
                // Send a message to get online status for specific users
                StringBuilder contactList = new StringBuilder();
                for (String contact : allChatItems.keySet()) {
                    if (contactList.length() > 0) contactList.append(",");
                    contactList.append(contact);
                }
                
                Message statusMessage = new Message(Message.MessageType.USER_LIST, currentUsername);
                statusMessage.setContent(contactList.toString());
                chatClient.sendMessage(statusMessage);
            } catch (IOException e) {
                System.err.println("Failed to request contact status: " + e.getMessage());
            }
        }
    }

    private void requestUserList() {
        try {
            Message userListMessage = new Message(Message.MessageType.USER_LIST, currentUsername);
            chatClient.sendMessage(userListMessage);
        } catch (IOException e) {
            System.err.println("Failed to request user list: " + e.getMessage());
        }
    }

    private void requestGroupList() {
        try {
            Message groupListMessage = new Message(Message.MessageType.GROUP_LIST, currentUsername);
            chatClient.sendMessage(groupListMessage);
        } catch (IOException e) {
            System.err.println("Failed to request group list: " + e.getMessage());
        }
    }

    private void handleUserListResponse(Message message) {
        // Update online status for existing contacts only
        String[] users = message.getContent().replace("Online users: ", "").split(", ");
        onlineUsers.clear();
        
        // Track who is online
        Set<String> currentlyOnline = new HashSet<>();
        for (String user : users) {
            if (!user.trim().isEmpty() && !user.equals(currentUsername)) {
                currentlyOnline.add(user.trim());
                onlineUsers.add(user.trim());
            }
        }
        
        // Update online status for existing contacts
        for (ChatItem contact : allChatItems.values()) {
            if (contact.getType() == ChatItem.Type.USER) {
                boolean isOnline = currentlyOnline.contains(contact.getName());
                contact.setOnline(isOnline);
                if (isOnline) {
                    contact.setLastSeenStatus("Online");
                }
                // Note: If offline, the last seen status will be updated via USER_STATUS_UPDATE messages
            }
        }
        
        updateChatList();
    }

    private void handleUserSearchResponse(Message message) {
        String response = message.getContent();
        if (!response.isEmpty()) {
            String[] users = response.split("\\|");
            for (String userInfo : users) {
                if (!userInfo.trim().isEmpty()) {
                    String[] parts = userInfo.split(" \\(");
                    if (parts.length > 0) {
                        String username = parts[0].trim();
                        boolean isOnline = userInfo.contains("(online)");
                        
                        if (!username.equals(currentUsername)) {
                            ChatItem userItem = new ChatItem(username, ChatItem.Type.USER, isOnline);
                            allChatItems.put(username, userItem);
                        }
                    }
                }
            }
            updateChatList();
        }
    }

    private void handleGroupListResponse(Message message) {
        String response = message.getContent();
        
        // Handle group notifications (when added to a group)
        if (response.startsWith("GROUP_ADDED:")) {
            handleGroupAddedNotification(response);
            return;
        }
        
        // Handle detailed group information
        if (response.startsWith("GROUP_DETAILS:")) {
            handleDetailedGroupInfo(response, message.getGroupId());
            return;
        }
        
        // Handle regular group list response
        response = response.replace("Your groups: ", "");
        if (!response.trim().isEmpty()) {
            String[] groups = response.split(", ");
            for (String groupInfo : groups) {
                if (!groupInfo.trim().isEmpty()) {
                    // Parse group name and ID from format: "GroupName (ID: groupId)"
                    int idIndex = groupInfo.lastIndexOf(" (ID: ");
                    if (idIndex > 0) {
                        String groupName = groupInfo.substring(0, idIndex);
                        String groupId = groupInfo.substring(idIndex + 6, groupInfo.length() - 1);
                        
                        userGroups.put(groupId, groupName);
                        ChatItem groupItem = new ChatItem(groupName, ChatItem.Type.GROUP, true);
                        groupItem.setGroupId(groupId);
                        allChatItems.put(groupName, groupItem);
                        
                        // Request detailed info for this group
                        requestDetailedGroupInfo(groupId);
                    }
                }
            }
            updateChatList();
        }
    }
    
    private void handleGroupAddedNotification(String response) {
        // Format: GROUP_ADDED:groupName|groupId|creator
        String[] parts = response.substring(12).split("\\|"); // Remove "GROUP_ADDED:"
        if (parts.length >= 3) {
            String groupName = parts[0];
            String groupId = parts[1];
            String creator = parts[2];
            
            System.out.println("DEBUG: Added to group: " + groupName + " (ID: " + groupId + ") by " + creator);
            
            // Add to user groups
            userGroups.put(groupId, groupName);
            
            // Create ChatItem for the group
            ChatItem groupItem = new ChatItem(groupName, ChatItem.Type.GROUP, true);
            groupItem.setGroupId(groupId);
            groupItem.setGroupCreator(creator);
            allChatItems.put(groupName, groupItem);
            
            // Request detailed group information
            requestDetailedGroupInfo(groupId);
            
            updateChatList();
        }
    }
    
    private void handleDetailedGroupInfo(String response, String groupId) {
        // Format: GROUP_DETAILS:groupName|groupId|creator|MEMBERS:member1,member2,|ADMINS:admin1,admin2,|ONLINE:online1,online2,
        String[] parts = response.substring(14).split("\\|"); // Remove "GROUP_DETAILS:"
        if (parts.length >= 6) {
            String groupName = parts[0];
            String creator = parts[2];
            
            // Parse members
            Set<String> members = new HashSet<>();
            if (parts[3].startsWith("MEMBERS:")) {
                String membersStr = parts[3].substring(8); // Remove "MEMBERS:"
                if (!membersStr.trim().isEmpty()) {
                    String[] memberArray = membersStr.split(",");
                    for (String member : memberArray) {
                        if (!member.trim().isEmpty()) {
                            members.add(member.trim());
                        }
                    }
                }
            }
            
            // Parse admins
            Set<String> admins = new HashSet<>();
            if (parts[4].startsWith("ADMINS:")) {
                String adminsStr = parts[4].substring(7); // Remove "ADMINS:"
                if (!adminsStr.trim().isEmpty()) {
                    String[] adminArray = adminsStr.split(",");
                    for (String admin : adminArray) {
                        if (!admin.trim().isEmpty()) {
                            admins.add(admin.trim());
                        }
                    }
                }
            }
            
            // Parse online members
            Set<String> onlineMembers = new HashSet<>();
            if (parts[5].startsWith("ONLINE:")) {
                String onlineStr = parts[5].substring(7); // Remove "ONLINE:"
                if (!onlineStr.trim().isEmpty()) {
                    String[] onlineArray = onlineStr.split(",");
                    for (String online : onlineArray) {
                        if (!online.trim().isEmpty()) {
                            onlineMembers.add(online.trim());
                        }
                    }
                }
            }
            
            // Find and update the ChatItem
            ChatItem groupItem = allChatItems.get(groupName);
            if (groupItem != null && groupItem.getType() == ChatItem.Type.GROUP) {
                groupItem.setGroupMembers(members);
                groupItem.setGroupAdmins(admins);
                groupItem.setOnlineMembers(onlineMembers);
                groupItem.setGroupCreator(creator);
                
                System.out.println("DEBUG: Updated group " + groupName + " - Members: " + members.size() + 
                                 ", Online: " + onlineMembers.size() + ", Creator: " + creator + ", Admins: " + admins);
                
                updateChatList();
                
                // If this group is currently selected, update the display
                if (selectedChat != null && selectedChat.getName().equals(groupName)) {
                    updateGroupChatDisplay();
                }
            }
        }
    }
    
    private void requestDetailedGroupInfo(String groupId) {
        try {
            Message groupInfoRequest = new Message(Message.MessageType.GROUP_LIST, currentUsername);
            groupInfoRequest.setGroupId(groupId);
            chatClient.sendMessage(groupInfoRequest);
        } catch (IOException e) {
            System.err.println("Failed to request detailed group info: " + e.getMessage());
        }
    }
    
    private void updateGroupChatDisplay() {
        if (selectedChat != null && selectedChat.getType() == ChatItem.Type.GROUP) {
            // Update the status label with current member counts
            String statusText = selectedChat.getMemberCount() + " members, " + selectedChat.getOnlineCount() + " online";
            chatStatusLabel.setText(statusText);
        }
    }

    private void updateChatList() {
        userListView.getItems().clear();
        userListView.getItems().addAll(allChatItems.values());
        
        // Sort by unread status first, then by last message time, then by type and name
        userListView.getItems().sort((a, b) -> {
            // Unread messages first
            if (a.hasUnreadMessages() != b.hasUnreadMessages()) {
                return a.hasUnreadMessages() ? -1 : 1;
            }
            
            // Then by type (groups first)
            if (a.getType() != b.getType()) {
                return a.getType() == ChatItem.Type.GROUP ? -1 : 1;
            }
            
            // Then by whether they have messages
            boolean aHasMessage = a.getLastMessage() != null && !a.getLastMessage().trim().isEmpty();
            boolean bHasMessage = b.getLastMessage() != null && !b.getLastMessage().trim().isEmpty();
            
            if (aHasMessage != bHasMessage) {
                return aHasMessage ? -1 : 1;
            }
            
            // Finally by name
            return a.getName().compareToIgnoreCase(b.getName());
        });
        
        // Set custom cell factory for better display
        userListView.setCellFactory(listView -> new ChatItemCell());
    }

    private void filterChatList(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            updateChatList();
            return;
        }
        
        // If searching, also request search from server
        if (searchText.length() >= 2) {
            try {
                Message searchMessage = new Message(Message.MessageType.USER_SEARCH, currentUsername);
                searchMessage.setContent(searchText.toLowerCase());
                chatClient.sendMessage(searchMessage);
            } catch (IOException e) {
                System.err.println("Failed to search users: " + e.getMessage());
            }
        }
        
        // Filter local results
        String searchLower = searchText.toLowerCase();
        userListView.getItems().clear();
        allChatItems.values().stream()
                .filter(item -> item.getName().toLowerCase().contains(searchLower))
                .sorted((a, b) -> {
                    if (a.getType() != b.getType()) {
                        return a.getType() == ChatItem.Type.GROUP ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .forEach(item -> userListView.getItems().add(item));
    }

    private void selectChat(ChatItem chatItem) {
        selectedChat = chatItem;
        
        // Mark as read
        chatItem.setHasUnreadMessages(false);
        updateChatList();
        
        // Update header
        chatNameLabel.setText(chatItem.getName());
        
        if (chatItem.getType() == ChatItem.Type.GROUP) {
            // Show group information
            String statusText = chatItem.getMemberCount() + " members, " + chatItem.getOnlineCount() + " online";
            chatStatusLabel.setText(statusText);
            
            // Make the status label interactive for hover effects
            chatStatusLabel.setOnMouseEntered(e -> showGroupMemberPopup(chatItem, e));
            chatStatusLabel.setOnMouseExited(e -> hideGroupMemberPopup());
            
            // Style the status label to indicate it's interactive
            chatStatusLabel.setStyle("-fx-text-fill: #007bff; -fx-cursor: hand;");
            
        } else {
            // Show user status
            if (chatItem.isOnline()) {
                chatStatusLabel.setText("Online");
            } else {
                chatStatusLabel.setText(chatItem.getLastSeenStatus());
            }
            
            // Remove group-specific interactions
            chatStatusLabel.setOnMouseEntered(null);
            chatStatusLabel.setOnMouseExited(null);
            chatStatusLabel.setStyle("-fx-text-fill: #666666;");
        }
        
        // Clear current messages
        messagesContainer.getChildren().clear();
        
        if (chatItem.getType() == ChatItem.Type.USER) {
            showPrivateChat(chatItem.getName());
        } else if (chatItem.getType() == ChatItem.Type.GROUP) {
            showGroupChat(chatItem.getGroupId());
        }
    }

    private Popup memberPopup;
    
    private void showGroupMemberPopup(ChatItem groupItem, javafx.scene.input.MouseEvent event) {
        if (memberPopup != null) {
            memberPopup.hide();
        }
        
        memberPopup = new Popup();
        memberPopup.setAutoHide(true);
        
        VBox popupContent = new VBox();
        popupContent.setSpacing(5);
        popupContent.setPadding(new Insets(10));
        popupContent.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        popupContent.setPrefWidth(250);
        popupContent.setMaxHeight(300);
        
        // Title
        Label titleLabel = new Label("Group Members (" + groupItem.getMemberCount() + ")");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Members list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox membersList = new VBox();
        membersList.setSpacing(3);
        
        for (String member : groupItem.getGroupMembers()) {
            HBox memberBox = new HBox();
            memberBox.setSpacing(8);
            memberBox.setAlignment(Pos.CENTER_LEFT);
            memberBox.setPadding(new Insets(3, 5, 3, 5));
            
            // Online indicator
            Label onlineIndicator = new Label(groupItem.getOnlineMembers().contains(member) ? "🟢" : "🔴");
            onlineIndicator.setStyle("-fx-font-size: 8px;");
            
            // Member name
            Label memberName = new Label(member);
            memberName.setStyle("-fx-font-size: 12px;");
            
            // Role indicator
            Label roleLabel = new Label();
            if (groupItem.isGroupCreator(member)) {
                roleLabel.setText("owner");
                roleLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else if (groupItem.isGroupAdmin(member)) {
                roleLabel.setText("admin");
                roleLabel.setStyle("-fx-text-fill: #4ecdc4; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            memberBox.getChildren().addAll(onlineIndicator, memberName, spacer, roleLabel);
            
            // Add right-click context menu for member management (only for admins/owner)
            if (groupItem.isGroupAdmin(currentUsername) || groupItem.isGroupCreator(currentUsername)) {
                memberBox.setOnContextMenuRequested(contextEvent -> {
                    showMemberContextMenu(groupItem, member, contextEvent);
                });
                memberBox.setStyle("-fx-cursor: hand;");
            }
            
            membersList.getChildren().add(memberBox);
        }
        
        scrollPane.setContent(membersList);
        popupContent.getChildren().addAll(titleLabel, scrollPane);
        
        memberPopup.getContent().add(popupContent);
        
        // Show popup near the mouse
        memberPopup.show(chatStatusLabel, event.getScreenX() + 10, event.getScreenY() + 10);
    }
    
    private void hideGroupMemberPopup() {
        // Add a small delay before hiding to allow mouse to move to popup
        Timeline hideTimer = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            if (memberPopup != null && !memberPopup.isShowing()) {
                // Popup already hidden or mouse moved to popup area
                return;
            }
            if (memberPopup != null) {
                memberPopup.hide();
            }
        }));
        hideTimer.play();
    }
    
    private void showMemberContextMenu(ChatItem groupItem, String targetMember, javafx.scene.input.ContextMenuEvent event) {
        if (targetMember.equals(currentUsername)) {
            return; // Don't show context menu for self
        }
        
        ContextMenu contextMenu = new ContextMenu();
        
        // Promote/Demote admin option
        if (groupItem.isGroupCreator(currentUsername)) {
            MenuItem adminAction;
            if (groupItem.isGroupAdmin(targetMember)) {
                adminAction = new MenuItem("Remove as admin");
                adminAction.setOnAction(e -> removeAdmin(groupItem.getGroupId(), targetMember));
            } else {
                adminAction = new MenuItem("Promote to admin");
                adminAction.setOnAction(e -> promoteToAdmin(groupItem.getGroupId(), targetMember));
            }
            contextMenu.getItems().add(adminAction);
        }
        
        // Remove from group option (for admins and owner, but owner can't be removed)
        if ((groupItem.isGroupAdmin(currentUsername) || groupItem.isGroupCreator(currentUsername)) 
            && !groupItem.isGroupCreator(targetMember)) {
            MenuItem removeAction = new MenuItem("Remove from group");
            removeAction.setOnAction(e -> removeMemberFromGroup(groupItem.getGroupId(), targetMember));
            contextMenu.getItems().add(removeAction);
        }
        
        if (!contextMenu.getItems().isEmpty()) {
            contextMenu.show(chatStatusLabel, event.getScreenX(), event.getScreenY());
        }
    }
    
    private void promoteToAdmin(String groupId, String username) {
        try {
            // Send promote message to server
            Message promoteMessage = new Message(Message.MessageType.JOIN_GROUP, currentUsername); // Reuse message type
            promoteMessage.setGroupId(groupId);
            promoteMessage.setRecipient(username);
            promoteMessage.setContent("PROMOTE_ADMIN"); // Special content to indicate admin promotion
            chatClient.sendMessage(promoteMessage);
            
            System.out.println("Promoting " + username + " to admin in group " + groupId);
            
        } catch (IOException e) {
            System.err.println("Failed to promote user to admin: " + e.getMessage());
        }
    }
    
    private void removeAdmin(String groupId, String username) {
        try {
            // Send demote message to server
            Message demoteMessage = new Message(Message.MessageType.LEAVE_GROUP, currentUsername); // Reuse message type
            demoteMessage.setGroupId(groupId);
            demoteMessage.setRecipient(username);
            demoteMessage.setContent("REMOVE_ADMIN"); // Special content to indicate admin removal
            chatClient.sendMessage(demoteMessage);
            
            System.out.println("Removing " + username + " as admin from group " + groupId);
            
        } catch (IOException e) {
            System.err.println("Failed to remove user as admin: " + e.getMessage());
        }
    }
    
    private void removeMemberFromGroup(String groupId, String username) {
        try {
            // Send remove member message to server
            Message removeMessage = new Message(Message.MessageType.LEAVE_GROUP, currentUsername);
            removeMessage.setGroupId(groupId);
            removeMessage.setRecipient(username);
            removeMessage.setContent("REMOVE_MEMBER"); // Special content to indicate member removal
            chatClient.sendMessage(removeMessage);
            
            System.out.println("Removing " + username + " from group " + groupId);
            
        } catch (IOException e) {
            System.err.println("Failed to remove member from group: " + e.getMessage());
        }
    }

    private void loadConversationHistory(ChatItem chatItem) {
        // Request conversation history from server
        try {
            Message syncMessage = new Message(Message.MessageType.SYNC_HISTORY, currentUsername);
            if (chatItem.getType() == ChatItem.Type.USER) {
                syncMessage.setContent(chatItem.getName());
            } else {
                syncMessage.setGroupId(chatItem.getGroupId());
            }
            chatClient.sendMessage(syncMessage);
        } catch (IOException e) {
            System.err.println("Failed to request conversation history: " + e.getMessage());
        }
    }

    private void showSelectChatMessage() {
        messagesContainer.getChildren().clear();
        
        Label selectLabel = new Label("Select a chat to start messaging");
        selectLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 16px;");
        
        VBox centerBox = new VBox(selectLabel);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPrefHeight(400);
        
        messagesContainer.getChildren().add(centerBox);
    }

    private void showPrivateChat(String contactName) {
        // Enable message input
        messageInput.setDisable(false);
        sendButton.setDisable(false);
        
        // Load conversation history for private chat
        loadConversationHistory(selectedChat);
    }
    
    private void showGroupChat(String groupId) {
        // Enable message input
        messageInput.setDisable(false);
        sendButton.setDisable(false);
        
        // Load conversation history for group chat
        loadConversationHistory(selectedChat);
        
        // Always request updated group information when entering a group chat
        requestDetailedGroupInfo(groupId);
        
        // Update display with current information (will be updated again when detailed info arrives)
        updateGroupChatDisplay();
    }

    @FXML
    private void handleSendMessage() {
        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty() || selectedChat == null) return;
        
        try {
            Message message;
            if (selectedChat.getType() == ChatItem.Type.GROUP) {
                message = new Message(Message.MessageType.GROUP_MESSAGE, currentUsername);
                message.setGroupId(selectedChat.getName());
            } else {
                message = new Message(Message.MessageType.PRIVATE_MESSAGE, currentUsername);
                message.setRecipient(selectedChat.getName());
            }
            
            // Keep the emoji placeholders in the message content
            message.setContent(messageText);
            
            chatClient.sendMessage(message);
            
            // Optimistic UI update
            addMessageToUI(message, true);
            
            // Update contact's last message (convert emoji placeholders to display format for contact list)
            String displayMessage = convertEmojiPlaceholdersToDisplay(messageText);
            selectedChat.setLastMessage("You: " + displayMessage);
            selectedChat.setHasUnreadMessages(false);
            updateChatList();
            
            messageInput.clear();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }
    
    private String convertEmojiPlaceholdersToDisplay(String messageText) {
        if (messageText == null || !messageText.contains("[EMOJI:")) {
            return messageText;
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < messageText.length()) {
            if (i < messageText.length() - 6 && messageText.substring(i, i + 7).equals("[EMOJI:")) {
                int endIndex = messageText.indexOf("]", i);
                if (endIndex != -1) {
                    String emojiPlaceholder = messageText.substring(i, endIndex + 1);
                    String emojiFilename = emojiPlaceholder.substring(7, emojiPlaceholder.length() - 1);
                    
                    // Get emoji display name or show a generic emoji symbol
                    String displayName = emojiNameMap.getOrDefault(emojiFilename, "📷");
                    if (displayName.contains(" ")) {
                        displayName = displayName.split(" ")[0]; // Just the emoji part
                    } else {
                        displayName = "📷"; // Generic emoji symbol
                    }
                    result.append(displayName);
                    i = endIndex + 1;
                } else {
                    result.append(messageText.charAt(i));
                    i++;
                }
            } else {
                result.append(messageText.charAt(i));
                i++;
            }
        }
        
        return result.toString();
    }

    private void addMessageToUI(Message message, boolean isSentByMe) {
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);
        messageBox.setPadding(new Insets(5, 10, 5, 10));
        
        VBox messageContent = new VBox();
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(400);
        
        // Create TextFlow with mixed text and emoji content
        TextFlow textFlow = createTextFlowWithEmojis(message.getContent());
        
        // Timestamp in bottom right corner
        String timeStr = message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        // Add timestamp aligned to the right
        HBox timestampBox = new HBox();
        timestampBox.setAlignment(Pos.CENTER_RIGHT);
        timestampBox.getChildren().add(timeLabel);
        
        messageContent.getChildren().addAll(textFlow, timestampBox);
        
        if (isSentByMe) {
            // Sent messages - align right
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageContent.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 18; -fx-padding: 12;");
        } else {
            // Received messages - align left, no sender name
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageContent.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 18; -fx-padding: 12; -fx-border-color: #E0E0E0; -fx-border-radius: 18; -fx-border-width: 1;");
        }
        
        messageBox.getChildren().add(messageContent);
        messagesContainer.getChildren().add(messageBox);
        
        // Auto-scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    private TextFlow createTextFlowWithEmojis(String messageContent) {
        TextFlow textFlow = new TextFlow();
        
        if (messageContent == null || messageContent.isEmpty()) {
            return textFlow;
        }
        
        // Parse message content for emoji placeholders
        String[] parts = messageContent.split("(\\[EMOJI:[^\\]]+\\])");
        String[] emojis = messageContent.split("[^\\[EMOJI:[^\\]]+\\]]+");
        
        // Handle text and emojis alternately
        int textIndex = 0;
        int emojiIndex = 0;
        
        // More robust parsing
        StringBuilder currentText = new StringBuilder();
        int i = 0;
        
        while (i < messageContent.length()) {
            if (i < messageContent.length() - 6 && messageContent.substring(i, i + 7).equals("[EMOJI:")) {
                // Add any accumulated text first
                if (currentText.length() > 0) {
                    Text textNode = new Text(currentText.toString());
                    textNode.setFont(Font.font("System", 14));
                    textFlow.getChildren().add(textNode);
                    currentText.setLength(0);
                }
                
                // Find the end of the emoji placeholder
                int endIndex = messageContent.indexOf("]", i);
                if (endIndex != -1) {
                    String emojiPlaceholder = messageContent.substring(i, endIndex + 1);
                    String emojiFilename = emojiPlaceholder.substring(7, emojiPlaceholder.length() - 1); // Remove [EMOJI: and ]
                    
                    // Create emoji ImageView
                    try {
                        Image emojiImage = new Image(getClass().getResourceAsStream(EMOJI_RESOURCES_PATH + emojiFilename));
                        ImageView emojiView = new ImageView(emojiImage);
                        emojiView.setFitWidth(20);
                        emojiView.setFitHeight(20);
                        emojiView.setPreserveRatio(true);
                        textFlow.getChildren().add(emojiView);
                    } catch (Exception e) {
                        // If emoji image fails to load, show the filename as text
                        Text fallbackText = new Text("[" + emojiFilename + "]");
                        fallbackText.setFont(Font.font("System", 14));
                        textFlow.getChildren().add(fallbackText);
                    }
                    
                    i = endIndex + 1;
                } else {
                    // Malformed emoji placeholder, treat as regular text
                    currentText.append(messageContent.charAt(i));
                    i++;
                }
            } else {
                currentText.append(messageContent.charAt(i));
                i++;
            }
        }
        
        // Add any remaining text
        if (currentText.length() > 0) {
            Text textNode = new Text(currentText.toString());
            textNode.setFont(Font.font("System", 14));
            textFlow.getChildren().add(textNode);
        }
        
        // If no content was added, add empty text to prevent layout issues
        if (textFlow.getChildren().isEmpty()) {
            Text emptyText = new Text("");
            textFlow.getChildren().add(emptyText);
        }
        
        return textFlow;
    }

    private void handleIncomingPrivateMessage(Message message) {
        // Automatically add sender to contacts if not already present
        String contactName = message.getSender();
        if (!contactName.equals(currentUsername) && !allChatItems.containsKey(contactName)) {
            ChatItem newContact = new ChatItem(contactName, ChatItem.Type.USER, true); // Assume online since they just sent a message
            allChatItems.put(contactName, newContact);
            System.out.println("Added new contact from incoming message: " + contactName);
        }
        
        // Update last message for the contact (always show the most recent message)
        if (allChatItems.containsKey(contactName)) {
            ChatItem contact = allChatItems.get(contactName);
            String displayMessage = convertEmojiPlaceholdersToDisplay(message.getContent());
            contact.setLastMessage(displayMessage); // Don't prefix with sender name for incoming
            
            // Mark as unread if not currently in this chat
            if (selectedChat == null || !selectedChat.getName().equals(contactName)) {
                contact.setHasUnreadMessages(true);
            }
            
            updateChatList();
        }
        
        // Add to UI if it's for the current conversation
        if (selectedChat != null && 
            selectedChat.getType() == ChatItem.Type.USER &&
            (selectedChat.getName().equals(message.getSender()) || 
             selectedChat.getName().equals(message.getRecipient()))) {
            addMessageToUI(message, message.getSender().equals(currentUsername));
        }
    }

    private void handleIncomingGroupMessage(Message message) {
        // Add to UI if it's for the current conversation
        if (selectedChat != null && 
            selectedChat.getType() == ChatItem.Type.GROUP &&
            selectedChat.getGroupId().equals(message.getGroupId())) {
            addMessageToUI(message, message.getSender().equals(currentUsername));
        }
    }

    private void handleUserStatusUpdate(Message message) {
        String[] parts = message.getContent().split("\\|");
        if (parts.length >= 3) {
            String username = parts[0].trim();
            boolean isOnline = Boolean.parseBoolean(parts[1].trim());
            String statusText = parts[2].trim();
            
            System.out.println("DEBUG: Received status update for " + username + " - online: " + isOnline + ", status: " + statusText);
            
            if (allChatItems.containsKey(username)) {
                ChatItem userItem = allChatItems.get(username);
                userItem.setOnline(isOnline);
                userItem.setLastSeenStatus(statusText);
                updateChatList();
                
                // Update chat header if this is the currently selected conversation
                if (selectedChat != null && selectedChat.getName().equals(username) && 
                    selectedChat.getType() == ChatItem.Type.USER) {
                    chatStatusLabel.setText(statusText);
                }
                
                System.out.println("DEBUG: Updated status for contact " + username + " to: " + statusText);
            } else {
                System.out.println("DEBUG: Contact " + username + " not found in allChatItems for status update");
            }
        } else {
            System.out.println("DEBUG: Invalid status update message format: " + message.getContent());
        }
    }

    @FXML
    private void handleSettings() {
        // Create context menu for settings
        ContextMenu settingsMenu = new ContextMenu();
        
        MenuItem newGroup = new MenuItem("New Group");
        newGroup.setOnAction(e -> showGroupCreationWorkflow());
        
        settingsMenu.getItems().add(newGroup);
        settingsMenu.show(settingsButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showGroupCreationWorkflow() {
        // Step 1: Group name input
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("New Group");
        nameDialog.setHeaderText("Create a new group");
        nameDialog.setContentText("Group name:");
        
        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
            String groupName = nameResult.get().trim();
            // Step 2: Show member selection popup
            showMemberSelectionPopup(groupName, false, null); // false = creating new group
        }
    }

    private void showMemberSelectionPopup(String groupName, boolean isAddingMembers, String existingGroupId) {
        Stage memberSelectionStage = new Stage();
        memberSelectionStage.initModality(Modality.APPLICATION_MODAL);
        memberSelectionStage.setTitle(isAddingMembers ? "Add Members" : "Select Members");
        memberSelectionStage.setResizable(false);
        
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setPrefSize(400, 500);
        
        // Title
        Label titleLabel = new Label(isAddingMembers ? "Add Members to Group" : "Select Group Members");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search contacts...");
        searchField.setPrefHeight(35);
        
        // Contact list with checkboxes
        ListView<CheckBox> contactListView = new ListView<>();
        contactListView.setPrefHeight(350);
        
        // Load contacts into the list
        ObservableList<CheckBox> contactCheckBoxes = FXCollections.observableArrayList();
        for (String contactName : allChatItems.keySet()) {
            if (!contactName.equals(currentUsername)) { // Don't show current user
                CheckBox contactCheckBox = new CheckBox(contactName);
                contactCheckBox.setStyle("-fx-font-size: 14px;");
                contactCheckBoxes.add(contactCheckBox);
            }
        }
        contactListView.setItems(contactCheckBoxes);
        
        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                contactListView.setItems(contactCheckBoxes);
            } else {
                String searchText = newValue.toLowerCase();
                ObservableList<CheckBox> filteredList = FXCollections.observableArrayList();
                for (CheckBox checkBox : contactCheckBoxes) {
                    if (checkBox.getText().toLowerCase().contains(searchText)) {
                        filteredList.add(checkBox);
                    }
                }
                contactListView.setItems(filteredList);
            }
        });
        
        // Buttons
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(80);
        cancelButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333;");
        cancelButton.setOnAction(e -> memberSelectionStage.close());
        
        Button createButton = new Button(isAddingMembers ? "Add" : "Create");
        createButton.setPrefWidth(80);
        createButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        createButton.setOnAction(e -> {
            Set<String> selectedMembers = new HashSet<>();
            for (CheckBox checkBox : contactCheckBoxes) {
                if (checkBox.isSelected()) {
                    selectedMembers.add(checkBox.getText());
                }
            }
            
            if (isAddingMembers) {
                addMembersToGroup(existingGroupId, selectedMembers);
            } else {
                createGroupWithMembers(groupName, selectedMembers);
            }
            memberSelectionStage.close();
        });
        
        buttonBox.getChildren().addAll(cancelButton, createButton);
        
        mainContainer.getChildren().addAll(titleLabel, searchField, contactListView, buttonBox);
        
        Scene scene = new Scene(mainContainer);
        memberSelectionStage.setScene(scene);
        memberSelectionStage.show();
    }

    private void createGroupWithMembers(String groupName, Set<String> selectedMembers) {
        try {
            // Create the group with selected members
            Message createGroupMessage = new Message(Message.MessageType.CREATE_GROUP, currentUsername);
            createGroupMessage.setContent(groupName);
            
            // Convert selected members to a comma-separated string
            String membersString = String.join(",", selectedMembers);
            createGroupMessage.setRecipient(membersString); // Use recipient field to pass members
            
            chatClient.sendMessage(createGroupMessage);
            
            System.out.println("Creating group '" + groupName + "' with members: " + selectedMembers);
            
            // Refresh group list after creation
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500); // Small delay to let server process
                    loadContacts(); // Reload contacts to show the new group
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
        } catch (IOException e) {
            System.err.println("Failed to create group: " + e.getMessage());
        }
    }

    private void addMembersToGroup(String groupId, Set<String> selectedMembers) {
        try {
            for (String member : selectedMembers) {
                Message addMemberMessage = new Message(Message.MessageType.JOIN_GROUP, currentUsername);
                addMemberMessage.setGroupId(groupId);
                addMemberMessage.setRecipient(member); // Member to add
                chatClient.sendMessage(addMemberMessage);
            }
            
            System.out.println("Adding members to group " + groupId + ": " + selectedMembers);
            
            // Refresh group information to get updated member counts
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500); // Small delay to let server process
                    requestDetailedGroupInfo(groupId); // Refresh current group info
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
        } catch (IOException e) {
            System.err.println("Failed to add members to group: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Stop the timer
            if (lastSeenUpdateTimer != null) {
                lastSeenUpdateTimer.stop();
            }
            
            // Send logout message
            Message logoutMessage = new Message(Message.MessageType.LOGOUT, currentUsername);
            chatClient.sendMessage(logoutMessage);
            
            // Disconnect and return to login
            chatClient.disconnect();
            ChatApplication.showLoginView();
            
        } catch (IOException e) {
            System.err.println("Failed to logout properly: " + e.getMessage());
            // Force close anyway
            chatClient.disconnect();
            System.exit(0);
        }
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        
        // Now that username is set, load contacts from message history
        System.out.println("DEBUG: Username set to " + username + ", loading contacts...");
        loadContacts();
    }

    // Inner classes for chat item management
    public static class ChatItem {
        public enum Type { USER, GROUP }
        
        private String name;
        private Type type;
        private boolean online;
        private String groupId;
        private String lastMessage;
        private String lastMessageTimestamp;
        private boolean hasUnreadMessages;
        private String lastSeenStatus;
        
        // Group-specific fields
        private Set<String> groupMembers;
        private Set<String> groupAdmins;
        private Set<String> onlineMembers;
        private String groupCreator;
        
        public ChatItem(String name, Type type, boolean online) {
            this.name = name;
            this.type = type;
            this.online = online;
            this.lastSeenStatus = online ? "Online" : "Last seen recently";
            this.hasUnreadMessages = false;
            this.lastMessage = "";
            this.lastMessageTimestamp = "";
            
            // Initialize group-specific fields
            if (type == Type.GROUP) {
                this.groupMembers = new HashSet<>();
                this.groupAdmins = new HashSet<>();
                this.onlineMembers = new HashSet<>();
            }
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Type getType() { return type; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { 
            this.online = online;
            if (online && (lastSeenStatus == null || lastSeenStatus.startsWith("Last seen"))) {
                this.lastSeenStatus = "Online";
            }
        }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { 
            this.lastMessage = lastMessage;
            this.lastMessageTimestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }
        public String getLastMessageTimestamp() { return lastMessageTimestamp; }
        public void setLastMessageTimestamp(String timestamp) { this.lastMessageTimestamp = timestamp; }
        public boolean hasUnreadMessages() { return hasUnreadMessages; }
        public void setHasUnreadMessages(boolean hasUnread) { this.hasUnreadMessages = hasUnread; }
        public String getLastSeenStatus() { return lastSeenStatus; }
        public void setLastSeenStatus(String lastSeenStatus) { this.lastSeenStatus = lastSeenStatus; }
        
        // Group-specific getters and setters
        public Set<String> getGroupMembers() { return groupMembers != null ? groupMembers : new HashSet<>(); }
        public void setGroupMembers(Set<String> groupMembers) { this.groupMembers = groupMembers; }
        public Set<String> getGroupAdmins() { return groupAdmins != null ? groupAdmins : new HashSet<>(); }
        public void setGroupAdmins(Set<String> groupAdmins) { this.groupAdmins = groupAdmins; }
        public Set<String> getOnlineMembers() { return onlineMembers != null ? onlineMembers : new HashSet<>(); }
        public void setOnlineMembers(Set<String> onlineMembers) { this.onlineMembers = onlineMembers; }
        public String getGroupCreator() { return groupCreator; }
        public void setGroupCreator(String groupCreator) { this.groupCreator = groupCreator; }
        
        public int getMemberCount() { return groupMembers != null ? groupMembers.size() : 0; }
        public int getOnlineCount() { return onlineMembers != null ? onlineMembers.size() : 0; }
        
        public boolean isGroupAdmin(String username) {
            return groupAdmins != null && groupAdmins.contains(username);
        }
        
        public boolean isGroupCreator(String username) {
            return groupCreator != null && groupCreator.equals(username);
        }
    }

    private static class ChatItemCell extends ListCell<ChatItem> {
        @Override
        protected void updateItem(ChatItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
            } else {
                VBox content = new VBox();
                content.setSpacing(3);
                content.setPadding(new Insets(8, 10, 8, 10));
                
                HBox nameBox = new HBox();
                nameBox.setAlignment(Pos.CENTER_LEFT);
                nameBox.setSpacing(8);
                
                Label nameLabel = new Label(item.getName());
                
                // Make name bold if there are unread messages
                if (item.hasUnreadMessages()) {
                    nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                } else {
                    nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                }
                
                if (item.getType() == ChatItem.Type.GROUP) {
                    Label groupIcon = new Label("👥");
                    nameBox.getChildren().addAll(groupIcon, nameLabel);
                } else {
                    Label statusIcon = new Label(item.isOnline() ? "🟢" :"");
                    statusIcon.setStyle("-fx-font-size: 8px;");
                    nameBox.getChildren().addAll(nameLabel, statusIcon);
                }
                
                content.getChildren().add(nameBox);
                
                // Show last message for all types instead of last seen status
                if (item.getLastMessage() != null && !item.getLastMessage().trim().isEmpty()) {
                    HBox messageBox = new HBox();
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageBox.setSpacing(8);
                    
                    Label messageLabel = new Label(item.getLastMessage());
                    if (item.hasUnreadMessages()) {
                        messageLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                        messageLabel.setStyle("-fx-text-fill: #333333;");
                    } else {
                        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                        messageLabel.setStyle("-fx-text-fill: #666666;");
                    }
                    
                    // Truncate long messages
                    if (messageLabel.getText().length() > 30) {
                        messageLabel.setText(messageLabel.getText().substring(0, 30) + "...");
                    }
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Label timeLabel = new Label(item.getLastMessageTimestamp());
                    timeLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
                    
                    messageBox.getChildren().addAll(messageLabel, spacer, timeLabel);
                    content.getChildren().add(messageBox);
                } else {
                    // Show "No messages yet" for contacts without messages
                    Label noMessageLabel = new Label("No messages yet");
                    noMessageLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px; -fx-font-style: italic;");
                    content.getChildren().add(noMessageLabel);
                }
                
                setGraphic(content);
            }
        }
    }

    // Emoji picker functionality
    private void loadRecentlyUsedEmojis() {
        try {
            Path recentFile = Paths.get(RECENT_EMOJIS_FILE);
            if (Files.exists(recentFile)) {
                recentlyUsedEmojis = Files.readAllLines(recentFile);
                // Keep only existing emoji files
                recentlyUsedEmojis = recentlyUsedEmojis.stream()
                    .filter(this::emojiFileExists)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            System.err.println("Error loading recent emojis: " + e.getMessage());
        }
    }
    
    private boolean emojiFileExists(String filename) {
        try {
            return getClass().getResourceAsStream(EMOJI_RESOURCES_PATH + filename) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void saveRecentlyUsedEmojis() {
        try {
            Path recentFile = Paths.get(RECENT_EMOJIS_FILE);
            Files.createDirectories(recentFile.getParent());
            Files.write(recentFile, recentlyUsedEmojis);
        } catch (IOException e) {
            System.err.println("Error saving recent emojis: " + e.getMessage());
        }
    }
    
    private void addToRecentEmojis(String emojiFilename) {
        recentlyUsedEmojis.remove(emojiFilename); // Remove if already exists
        recentlyUsedEmojis.add(0, emojiFilename); // Add to beginning
        
        // Keep only the last 20 recent emojis
        if (recentlyUsedEmojis.size() > 20) {
            recentlyUsedEmojis = recentlyUsedEmojis.subList(0, 20);
        }
        
        saveRecentlyUsedEmojis();
    }
    
    @FXML
    private void handleEmojiPicker() {
        showEmojiPickerPopup();
    }
    
    private void showEmojiPickerPopup() {
        Popup emojiPopup = new Popup();
        emojiPopup.setAutoHide(true);
        emojiPopup.setHideOnEscape(true);
        
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(5);
        mainContainer.setPadding(new Insets(10));
        mainContainer.setPrefSize(400, 350);
        mainContainer.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; " +
                              "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search emojis...");
        searchField.setPrefHeight(30);
        
        // Tabs
        TabPane emojiTabs = new TabPane();
        emojiTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        emojiTabs.setPrefHeight(280);
        
        // Recent tab
        Tab recentTab = new Tab("Recent");
        ScrollPane recentScroll = createEmojiScrollPane(recentlyUsedEmojis, emojiPopup);
        recentTab.setContent(recentScroll);
        emojiTabs.getTabs().add(recentTab);
        
        // All emojis tab
        Tab allTab = new Tab("All");
        List<String> allEmojis = getAllAvailableEmojis();
        ScrollPane allScroll = createEmojiScrollPane(allEmojis, emojiPopup);
        allTab.setContent(allScroll);
        emojiTabs.getTabs().add(allTab);
        
        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                return;
            }
            
            List<String> searchResults = searchEmojis(newValue);
            ScrollPane searchScroll = createEmojiScrollPane(searchResults, emojiPopup);
            
            // Add or update search results tab
            Tab searchTab = emojiTabs.getTabs().stream()
                    .filter(tab -> "Search".equals(tab.getText()))
                    .findFirst()
                    .orElse(null);
            
            if (searchTab == null) {
                searchTab = new Tab("Search");
                emojiTabs.getTabs().add(searchTab);
            }
            
            searchTab.setContent(searchScroll);
            emojiTabs.getSelectionModel().select(searchTab);
        });
        
        mainContainer.getChildren().addAll(searchField, emojiTabs);
        emojiPopup.getContent().add(mainContainer);
        
        // Show popup relative to emoji button
        Button emojiButton = (Button) messageInput.getParent().lookup("#emojiButton");
        if (emojiButton != null) {
            emojiPopup.show(emojiButton, 
                emojiButton.localToScreen(emojiButton.getBoundsInLocal()).getMinX(),
                emojiButton.localToScreen(emojiButton.getBoundsInLocal()).getMinY() - 360
            );
        }
    }
    
    private List<String> getAllAvailableEmojis() {
        List<String> emojis = new ArrayList<>();
        
        // Add basic Unicode symbol emojis that we know exist
        emojis.addAll(Arrays.asList(
            "2764.png", "270d.png", "270c.png", "270b.png", "270a.png", "2709.png", "2708.png",
            "2705.png", "2702.png", "26fa.png", "26fd.png", "2b50.png", "2b55.png", "2754.png",
            "2755.png", "2757.png", "2795.png", "2796.png", "2797.png", "27a1.png", "2b05.png",
            "2b06.png", "2b07.png", "2b1c.png", "2b1b.png", "2934.png", "2935.png", "27bf.png",
            "2728.png", "2733.png", "2734.png", "2744.png", "2747.png", "274c.png", "274e.png",
            "2753.png", "2763.png", "2721.png", "3030.png", "303d.png", "3297.png", "3299.png",
            "a9.png", "ae.png", "e50a.png", "30-20e3.png", "31-20e3.png", "32-20e3.png",
            "33-20e3.png", "34-20e3.png", "35-20e3.png", "36-20e3.png", "37-20e3.png",
            "38-20e3.png", "39-20e3.png", "2a-20e3.png"
        ));
        
        // Add the new emojis that we found
        emojis.addAll(Arrays.asList(
            "1f004.png", "1f0cf.png", "1f170.png", "1f171.png", "1f17e.png", "1f17f.png",
            "1f18e.png", "1f191.png", "1f192.png", "1f193.png", "1f194.png", "1f195.png",
            "1f196.png", "1f197.png", "1f198.png", "1f199.png", "1f19a.png"
        ));
        
        // Filter to only include emojis that actually exist
        return emojis.stream()
                .filter(this::emojiFileExists)
                .collect(Collectors.toList());
    }
    
    private List<String> searchEmojis(String query) {
        String lowerQuery = query.toLowerCase();
        return emojiNameMap.entrySet().stream()
                .filter(entry -> entry.getValue().toLowerCase().contains(lowerQuery))
                .map(Map.Entry::getKey)
                .filter(this::emojiFileExists)
                .collect(Collectors.toList());
    }
    
    private ScrollPane createEmojiScrollPane(List<String> emojiFiles, Popup emojiPopup) {
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));
        
        int col = 0;
        int row = 0;
        int itemsPerRow = 8;
        
        for (String emojiFile : emojiFiles) {
            try {
                Image emojiImage = new Image(getClass().getResourceAsStream(EMOJI_RESOURCES_PATH + emojiFile));
                ImageView imageView = new ImageView(emojiImage);
                imageView.setFitWidth(32);
                imageView.setFitHeight(32);
                imageView.setPreserveRatio(true);
                
                Button emojiButton = new Button();
                emojiButton.setGraphic(imageView);
                emojiButton.setPrefSize(40, 40);
                emojiButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                
                String emojiName = emojiNameMap.getOrDefault(emojiFile, emojiFile);
                emojiButton.setTooltip(new Tooltip(emojiName));
                
                emojiButton.setOnMouseEntered(e -> emojiButton.setStyle("-fx-background-color: #e0e0e0; -fx-border-radius: 3;"));
                emojiButton.setOnMouseExited(e -> emojiButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
                
                emojiButton.setOnAction(e -> {
                    insertEmojiIntoMessage(emojiFile);
                    addToRecentEmojis(emojiFile);
                    emojiPopup.hide();
                });
                
                grid.add(emojiButton, col, row);
                
                col++;
                if (col >= itemsPerRow) {
                    col = 0;
                    row++;
                }
            } catch (Exception e) {
                System.err.println("Error loading emoji: " + emojiFile + " - " + e.getMessage());
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(240);
        return scrollPane;
    }
    
    private void insertEmojiIntoMessage(String emojiFilename) {
        String currentText = messageInput.getText();
        int caretPosition = messageInput.getCaretPosition();
        
        // Insert emoji placeholder in text
        String emojiPlaceholder = "[EMOJI:" + emojiFilename + "]";
        String newText = currentText.substring(0, caretPosition) + emojiPlaceholder + currentText.substring(caretPosition);
        messageInput.setText(newText);
        messageInput.positionCaret(caretPosition + emojiPlaceholder.length());
        messageInput.requestFocus();
    }

    @FXML
    private void handleChatMenu() {
        if (selectedChat == null) return;
        
        ContextMenu chatMenu = new ContextMenu();
        
        if (selectedChat.getType() == ChatItem.Type.GROUP) {
            // Debug logging
            System.out.println("DEBUG: Chat menu for group " + selectedChat.getName());
            System.out.println("DEBUG: Current user: " + currentUsername);
            System.out.println("DEBUG: Group creator: " + selectedChat.getGroupCreator());
            System.out.println("DEBUG: Group admins: " + selectedChat.getGroupAdmins());
            System.out.println("DEBUG: Is current user admin: " + selectedChat.isGroupAdmin(currentUsername));
            System.out.println("DEBUG: Is current user creator: " + selectedChat.isGroupCreator(currentUsername));
            
            // Group-specific options
            MenuItem addMembersItem = new MenuItem("Add members");
            addMembersItem.setOnAction(e -> showMemberSelectionPopup("", true, selectedChat.getGroupId()));
            
            MenuItem changeNameItem = new MenuItem("Change group name");
            changeNameItem.setOnAction(e -> showChangeGroupNameDialog());
            
            // Only show these options to admins and owner
            if (selectedChat.isGroupAdmin(currentUsername) || selectedChat.isGroupCreator(currentUsername)) {
                chatMenu.getItems().addAll(addMembersItem, changeNameItem);
                System.out.println("DEBUG: Added admin/owner menu items");
            } else {
                System.out.println("DEBUG: User is not admin/owner, not showing admin menu items");
            }
            
            // Leave group option (everyone except owner)
            if (!selectedChat.isGroupCreator(currentUsername)) {
                MenuItem leaveGroupItem = new MenuItem("Leave group");
                leaveGroupItem.setOnAction(e -> leaveGroup());
                chatMenu.getItems().add(leaveGroupItem);
                System.out.println("DEBUG: Added leave group option");
            } else {
                System.out.println("DEBUG: User is creator, not showing leave group option");
            }
            
        } else {
            // User-specific options
            MenuItem blockUserItem = new MenuItem("Block user");
            MenuItem clearChatItem = new MenuItem("Clear chat");
            chatMenu.getItems().addAll(blockUserItem, clearChatItem);
        }
        
        if (!chatMenu.getItems().isEmpty()) {
            chatMenu.show(chatMenuButton, javafx.geometry.Side.BOTTOM, 0, 0);
        } else {
            System.out.println("DEBUG: No menu items to show");
        }
    }
    
    private void showChangeGroupNameDialog() {
        if (selectedChat == null || selectedChat.getType() != ChatItem.Type.GROUP) return;
        
        // Check permissions
        if (!selectedChat.isGroupAdmin(currentUsername) && !selectedChat.isGroupCreator(currentUsername)) {
            showAlert("Permission Denied", "Only group admins and the owner can change the group name.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog(selectedChat.getName());
        dialog.setTitle("Change Group Name");
        dialog.setHeaderText("Enter new group name");
        dialog.setContentText("Group name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newGroupName = result.get().trim();
            if (!newGroupName.equals(selectedChat.getName())) {
                changeGroupName(newGroupName);
            }
        }
    }
    
    private void changeGroupName(String newGroupName) {
        try {
            Message changeNameMessage = new Message(Message.MessageType.GROUP_MESSAGE, currentUsername);
            changeNameMessage.setGroupId(selectedChat.getGroupId());
            changeNameMessage.setContent("CHANGE_GROUP_NAME:" + newGroupName);
            chatClient.sendMessage(changeNameMessage);
            
            System.out.println("Changing group name to: " + newGroupName);
            
        } catch (IOException e) {
            System.err.println("Failed to change group name: " + e.getMessage());
            showAlert("Error", "Failed to change group name. Please try again.");
        }
    }
    
    private void leaveGroup() {
        if (selectedChat == null || selectedChat.getType() != ChatItem.Type.GROUP) return;
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Leave Group");
        confirmDialog.setHeaderText("Are you sure you want to leave this group?");
        confirmDialog.setContentText("You will no longer receive messages from this group.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Message leaveMessage = new Message(Message.MessageType.LEAVE_GROUP, currentUsername);
                leaveMessage.setGroupId(selectedChat.getGroupId());
                chatClient.sendMessage(leaveMessage);
                
                System.out.println("Leaving group: " + selectedChat.getName());
                
                // Remove from local contacts and refresh
                allChatItems.remove(selectedChat.getName());
                updateChatList();
                showSelectChatMessage(); // Clear chat area
                
            } catch (IOException e) {
                System.err.println("Failed to leave group: " + e.getMessage());
                showAlert("Error", "Failed to leave group. Please try again.");
            }
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
