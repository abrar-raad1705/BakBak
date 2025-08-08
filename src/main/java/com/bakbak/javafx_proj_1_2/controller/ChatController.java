package com.bakbak.javafx_proj_1_2.controller;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.bakbak.javafx_proj_1_2.ChatApplication;
import com.bakbak.javafx_proj_1_2.ChatClient;
import com.bakbak.javafx_proj_1_2.FileChunkReceiver;
import com.bakbak.javafx_proj_1_2.FileChunkSender;
import com.bakbak.javafx_proj_1_2.FileMessageData;
import com.bakbak.javafx_proj_1_2.Message;
import com.bakbak.javafx_proj_1_2.ProgressCallback;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Window;

public class ChatController implements Initializable {

    @FXML
    private ToggleButton darkModeToggle;
    @FXML
    private Button settingsButton;
    @FXML
    private TextField messageInput1; // Search field
    @FXML
    private ListView<ChatItem> userListView;
    @FXML
    private Button logoutButton;
    @FXML
    private Label chatNameLabel;
    @FXML
    private Label chatStatusLabel;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox messagesContainer;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendButton;
    @FXML
    private Button chatMenuButton;
    @FXML
    private VBox chatHeader;
    @FXML
    private HBox messageInputArea;
    @FXML
    private Button fileButton;
    @FXML
    private Button voiceButton;
    @FXML
    private ImageView darkModeIcon;

    private ChatClient chatClient;
    private String currentUsername;
    private ChatItem selectedChat;
    private Set<String> onlineUsers = new HashSet<>();
    private Map<String, String> userGroups = new HashMap<>(); // groupId -> groupName
    private Map<String, ChatItem> allChatItems = new HashMap<>();
    private CompletableFuture<Message> pendingResponse;
    private Timeline lastSeenUpdateTimer;
    private String lastMessageSender = null; // Track last sender for message grouping

    // Store the selection listener to enable/disable it
    private javafx.beans.value.ChangeListener<ChatItem> chatSelectionListener;
    private boolean isSelectingChat = false; // Prevent multiple rapid selections

    // Modern sprite-based emoji system
    private static final String EMOJI_SPRITES_PATH = "/com/bakbak/javafx_proj_1_2/emoji_sprites/";

    private Map<String, List<EmojiData>> emojiCategories = new LinkedHashMap<>();
    private Map<String, EmojiData> allEmojis = new HashMap<>();

    // Cache for sprite sheet images to prevent memory issues
    private Map<String, Image> spriteSheetCache = new HashMap<>();

    // Active emoji category for canvas rendering
    private String activeCategory = "Smileys";
    // Currently hovered emoji in canvas
    private EmojiData hoveredEmoji = null;
    // Store caret position when emoji picker opens
    private int savedCaretPosition = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatClient = ChatApplication.getChatClient();
        setupUI();
        setupMessageHandler();
        initializeEmojiSystem(); // Initialize sprite-based emojis
        startLastSeenUpdateTimer(); // Start timer for last seen updates

        // Note: loadContacts() is now called after username is set in
        // setCurrentUsername()

        // Initially show "Select a chat" message
        showSelectChatMessage();
    }

    private void initializeEmojiSystem() {
        System.out.println("DEBUG: Initializing emoji system...");
        // Load all emoji categories from sprite sheets
        loadEmojiCategories();
        System.out.println("DEBUG: Emoji system initialization complete. Total categories: " + emojiCategories.size()
                + ", Total emojis: " + allEmojis.size());
    }

    // Data class for emoji information
    public static class EmojiData {
        private final String filename;
        private final String name;
        private final int x, y, width, height;
        private final String category;

        public EmojiData(String filename, String name, int x, int y, int width, int height, String category) {
            this.filename = filename;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.category = category;
        }

        // Getters
        public String getFilename() {
            return filename;
        }

        public String getName() {
            return name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getCategory() {
            return category;
        }
    }

    // Canvas-based emoji renderer class
    private class EmojiCanvas extends Canvas {
        private List<EmojiData> emojis;
        private Image spriteSheet;
        private int columns = 9;
        private int emojiSize = 32; // Size of each emoji button
        private int emojiImageSize = 24; // Actual emoji image size within button
        private int padding = 8;
        private Tooltip tooltip = new Tooltip();

        public EmojiCanvas() {
            this.setWidth(columns * (emojiSize + padding)); // Remove padding subtraction to give more space

            // Handle hover events to highlight emojis
            this.setOnMouseMoved(e -> {
                int col = (int) (e.getX() / (emojiSize + padding));
                int row = (int) (e.getY() / (emojiSize + padding));
                int index = row * columns + col;

                if (emojis != null && index >= 0 && index < emojis.size() && col < columns) {
                    EmojiData emoji = emojis.get(index);
                    if (hoveredEmoji != emoji) {
                        hoveredEmoji = emoji;
                        if (emoji.getName() != null) {
                            tooltip.setText(emoji.getName());
                            tooltip.show(this, e.getScreenX() + 15, e.getScreenY() + 15);
                        }
                        redraw();
                    }
                } else {
                    if (hoveredEmoji != null) {
                        hoveredEmoji = null;
                        tooltip.hide();
                        redraw();
                    }
                }
            });

            // Handle mouse exit to clear hover state
            this.setOnMouseExited(e -> {
                hoveredEmoji = null;
                tooltip.hide();
                redraw();
            });

            // Handle click events to select emoji
            this.setOnMouseClicked(e -> {
                int col = (int) (e.getX() / (emojiSize + padding));
                int row = (int) (e.getY() / (emojiSize + padding));
                int index = row * columns + col;

                if (emojis != null && index >= 0 && index < emojis.size() && col < columns) {
                    EmojiData emoji = emojis.get(index);
                    insertEmojiIntoMessage(emoji.getFilename());
                    // Keep popup open for multiple emoji selection
                }
            });
        }

        public void setEmojis(List<EmojiData> emojis, Image spriteSheet) {
            this.emojis = emojis;
            this.spriteSheet = spriteSheet;

            // Adjust canvas height based on number of emojis
            int rows = (int) Math.ceil((double) emojis.size() / columns);
            this.setHeight(rows * (emojiSize + padding) - padding);

            redraw();
        }

        public void redraw() {
            if (emojis == null || spriteSheet == null)
                return;

            GraphicsContext gc = this.getGraphicsContext2D();
            gc.clearRect(0, 0, this.getWidth(), this.getHeight());

            int col = 0;
            int row = 0;

            for (EmojiData emoji : emojis) {
                // Calculate position for this emoji
                int x = col * (emojiSize + padding);
                int y = row * (emojiSize + padding);

                // Draw background/highlight if hovered
                if (emoji == hoveredEmoji) {
                    gc.setFill(javafx.scene.paint.Color.valueOf("#f0f0f0"));
                    gc.fillRoundRect(x, y, emojiSize, emojiSize, 6, 6);
                }

                // Calculate centering offset for the emoji within the button area
                int offsetX = (emojiSize - emojiImageSize) / 2;
                int offsetY = (emojiSize - emojiImageSize) / 2;

                // Draw the emoji from sprite sheet
                gc.drawImage(
                        spriteSheet,
                        emoji.getX(), emoji.getY(), emoji.getWidth(), emoji.getHeight(),
                        x + offsetX, y + offsetY, emojiImageSize, emojiImageSize);

                // Update position for next emoji
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private void loadEmojiCategories() {
        // Define categories with their corresponding files
        String[] categories = {
                "Smileys", "People", "Animals & Nature", "Food",
                "Activities", "Places", "Objects", "Symbols", "Flags", "Tone & Style"
        };

        for (String category : categories) {
            try {
                String jsonPath = EMOJI_SPRITES_PATH + category + ".json";
                InputStream jsonStream = getClass().getResourceAsStream(jsonPath);

                if (jsonStream != null) {
                    List<EmojiData> categoryEmojis = parseEmojiJson(jsonStream, category);
                    if (!categoryEmojis.isEmpty()) {
                        emojiCategories.put(category, categoryEmojis);

                        // Add to global emoji map for search
                        for (EmojiData emoji : categoryEmojis) {
                            if (emoji.getName() != null && !emoji.getName().isEmpty()) {
                                allEmojis.put(emoji.getFilename(), emoji);
                            }
                        }

                        System.out.println("Loaded " + categoryEmojis.size() + " emojis for category: " + category);
                    }
                } else {
                    System.err.println("Could not find emoji JSON for category: " + category);
                }
            } catch (Exception e) {
                System.err.println("Error loading emoji category " + category + ": " + e.getMessage());
            }
        }

        System.out.println(
                "Total emojis loaded: " + allEmojis.size() + " across " + emojiCategories.size() + " categories");
    }

    private List<EmojiData> parseEmojiJson(InputStream jsonStream, String category) {
        List<EmojiData> emojis = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jsonStream))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            // Simple JSON parsing (you could use a JSON library here for more robust
            // parsing)
            String json = jsonContent.toString();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1); // Remove outer braces

                // Split by top-level commas (simple approach)
                String[] entries = parseJsonEntries(json);

                for (String entry : entries) {
                    try {
                        EmojiData emoji = parseEmojiEntry(entry.trim(), category);
                        if (emoji != null) {
                            emojis.add(emoji);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing emoji entry: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading emoji JSON: " + e.getMessage());
        }

        return emojis;
    }

    private String[] parseJsonEntries(String json) {
        List<String> entries = new ArrayList<>();
        int braceLevel = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                braceLevel++;
            } else if (c == '}') {
                braceLevel--;
            } else if (c == ',' && braceLevel == 0) {
                entries.add(json.substring(start, i));
                start = i + 1;
            }
        }

        // Add the last entry
        if (start < json.length()) {
            entries.add(json.substring(start));
        }

        return entries.toArray(new String[0]);
    }

    private EmojiData parseEmojiEntry(String entry, String category) {
        try {
            // Parse entry like: "1f479.png": { "x": 72, "y": 0, "width": 72, "height": 72,
            // "name": "japanese_ogre" }
            int colonIndex = entry.indexOf(':');
            if (colonIndex == -1)
                return null;

            String filename = entry.substring(0, colonIndex).trim();
            filename = filename.replace("\"", "");

            String data = entry.substring(colonIndex + 1).trim();
            if (data.startsWith("{") && data.endsWith("}")) {
                data = data.substring(1, data.length() - 1);

                // Parse x, y, width, height, name
                int x = 0, y = 0, width = 72, height = 72;
                String name = null;

                String[] properties = data.split(",");
                for (String prop : properties) {
                    prop = prop.trim();
                    if (prop.contains("\"x\"") && prop.contains(":")) {
                        x = extractIntValue(prop);
                    } else if (prop.contains("\"y\"") && prop.contains(":")) {
                        y = extractIntValue(prop);
                    } else if (prop.contains("\"width\"") && prop.contains(":")) {
                        width = extractIntValue(prop);
                    } else if (prop.contains("\"height\"") && prop.contains(":")) {
                        height = extractIntValue(prop);
                    } else if (prop.contains("\"name\"") && prop.contains(":")) {
                        name = extractStringValue(prop);
                    }
                }

                return new EmojiData(filename, name, x, y, width, height, category);
            }
        } catch (Exception e) {
            System.err.println("Error parsing emoji entry: " + entry + " - " + e.getMessage());
        }

        return null;
    }

    private int extractIntValue(String property) {
        try {
            int colonIndex = property.indexOf(':');
            if (colonIndex != -1) {
                String value = property.substring(colonIndex + 1).trim();
                // Remove any quotes and non-numeric characters
                value = value.replaceAll("[^0-9]", "");
                if (!value.isEmpty()) {
                    return Integer.parseInt(value);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing int value: " + property);
        }
        return 0;
    }

    private String extractStringValue(String property) {
        try {
            int colonIndex = property.indexOf(':');
            if (colonIndex != -1) {
                String value = property.substring(colonIndex + 1).trim();
                value = value.replace("\"", "");
                if ("null".equals(value)) {
                    return null;
                }
                return value;
            }
        } catch (Exception e) {
            System.err.println("Error parsing string value: " + property);
        }
        return null;
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
        chatSelectionListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectChat(newValue);
            }
        };
        userListView.getSelectionModel().selectedItemProperty().addListener(chatSelectionListener);

        // Setup send message functionality
        sendButton.setOnAction(e -> handleSendMessage());
        messageInput.setOnAction(e -> handleSendMessage());
        fileButton.setOnAction(e -> handleSendFile());

        // Setup dark mode toggle
        darkModeToggle.setOnAction(e -> handleDarkModeToggle());

        // Disable message input initially
        messageInput.setDisable(true);
        sendButton.setDisable(true);
        fileButton.setDisable(true);

        // Setup unified progress bar below chat header
        setupUnifiedProgressBar();

        // Setup hover effects for icon buttons
        setupIconButtonHoverEffects();

        // Setup chat screen containers with CSS classes for dark mode
        messagesContainer.getStyleClass().add("chatBoxScreen");
        chatScrollPane.getStyleClass().add("chatBoxScreen");
    }

    private void setupIconButtonHoverEffects() {
        // Setup hover effects for settings button
        setupButtonHoverEffect(settingsButton, "settings.png", "settings2.png");

        // Setup hover effects for more button
        setupButtonHoverEffect(chatMenuButton, "more.png", "more2.png");

        // Setup hover effects for attach button
        setupButtonHoverEffect(fileButton, "attach.png", "attach2.png");

        // Setup hover effects for send button (no "2" version, just opacity change)
        setupButtonHoverEffect(sendButton, "send.png", null);

        // Setup hover effects for toggle button
        setupToggleButtonHoverEffects();

        // Setup hover effects for other buttons with a delay to ensure scene is ready
        Platform.runLater(() -> {
            // Retry mechanism for buttons that might not be ready immediately
            setupHoverEffectsWithRetry();
        });
    }

    private void setupHoverEffectsWithRetry() {
        // Setup hover effects for call button
        Button callButton = findButtonByImage("call.png");
        if (callButton != null) {
            setupButtonHoverEffect(callButton, "call.png", "call2.png");
        } else {
            // Retry after a short delay
            Timeline retryTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                Button retryButton = findButtonByImage("call.png");
                if (retryButton != null) {
                    setupButtonHoverEffect(retryButton, "call.png", "call2.png");
                }
            }));
            retryTimer.play();
        }

        // Setup hover effects for video button
        Button videoButton = findButtonByImage("video.png");
        if (videoButton != null) {
            setupButtonHoverEffect(videoButton, "video.png", "video2.png");
        } else {
            Timeline retryTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                Button retryButton = findButtonByImage("video.png");
                if (retryButton != null) {
                    setupButtonHoverEffect(retryButton, "video.png", "video2.png");
                }
            }));
            retryTimer.play();
        }

        // Setup hover effects for emoji button
        Button emojiButton = findButtonByImage("emoji.png");
        if (emojiButton != null) {
            setupButtonHoverEffect(emojiButton, "emoji.png", "emoji2.png");
        } else {
            Timeline retryTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                Button retryButton = findButtonByImage("emoji.png");
                if (retryButton != null) {
                    setupButtonHoverEffect(retryButton, "emoji.png", "emoji2.png");
                }
            }));
            retryTimer.play();
        }

        // Setup hover effects for voice button
        Button voiceButton = findButtonByImage("voice.png");
        if (voiceButton != null) {
            setupButtonHoverEffect(voiceButton, "voice.png", "voice2.png");
        } else {
            Timeline retryTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                Button retryButton = findButtonByImage("voice.png");
                if (retryButton != null) {
                    setupButtonHoverEffect(retryButton, "voice.png", "voice2.png");
                }
            }));
            retryTimer.play();
        }
    }

    private void setupButtonHoverEffect(Button button, String normalImage, String hoverImage) {
        if (button == null)
            return;

        ImageView imageView = (ImageView) button.getGraphic();
        if (imageView == null)
            return;

        Image normalImg = new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/" + normalImage));
        Image hoverImg = hoverImage != null
                ? new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/" + hoverImage))
                : null;

        button.setOnMouseEntered(e -> {
            if (hoverImg != null) {
                imageView.setImage(hoverImg);
            } else {
                imageView.setOpacity(0.8);
            }
        });

        button.setOnMouseExited(e -> {
            imageView.setImage(normalImg);
            imageView.setOpacity(1.0);
        });
    }

    private void setupToggleButtonHoverEffects() {
        if (darkModeToggle == null || darkModeIcon == null)
            return;

        // Load all the images
        Image sunImg = new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/sun.png"));
        Image sun2Img = new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/sun2.png"));
        Image nightImg = new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/night.png"));
        Image night2Img = new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/night2.png"));

        // Set initial state (sun icon)
        darkModeIcon.setImage(sunImg);

        // Setup hover effects
        darkModeToggle.setOnMouseEntered(e -> {
            if (darkModeToggle.isSelected()) {
                // Dark mode is on, show night2 on hover
                darkModeIcon.setImage(night2Img);
            } else {
                // Light mode is on, show sun2 on hover
                darkModeIcon.setImage(sun2Img);
            }
        });

        darkModeToggle.setOnMouseExited(e -> {
            if (darkModeToggle.isSelected()) {
                // Dark mode is on, show night
                darkModeIcon.setImage(nightImg);
            } else {
                // Light mode is on, show sun
                darkModeIcon.setImage(sunImg);
            }
        });

        // Update icon when toggle state changes
        darkModeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Dark mode activated, show night icon
                darkModeIcon.setImage(nightImg);
            } else {
                // Light mode activated, show sun icon
                darkModeIcon.setImage(sunImg);
            }
        });
    }

    private Button findButtonByImageRecursive(Node node, String imageName) {
        if (node instanceof Button) {
            Button button = (Button) node;
            if (button.getGraphic() instanceof ImageView) {
                ImageView imageView = (ImageView) button.getGraphic();
                if (imageView.getImage() != null) {
                    String imageUrl = imageView.getImage().getUrl();
                    if (imageUrl != null && imageUrl.contains(imageName)) {
                        return button;
                    }
                }
            }
        }

        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                Button result = findButtonByImageRecursive(child, imageName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private Button findButtonByImage(String imageName) {
        // Search through all buttons in the scene to find the one with the specified
        // image
        if (settingsButton.getScene() == null) {
            System.out.println("Scene not ready yet for " + imageName);
            return null;
        }

        Button found = findButtonByImageRecursive(settingsButton.getScene().getRoot(), imageName);
        if (found != null) {
            System.out.println("Found button for " + imageName);
        } else {
            System.out.println("Could not find button for " + imageName);
        }
        return found;
    }

    @FXML
    private void handleDarkModeToggle() {
        Scene scene = darkModeToggle.getScene();
        if (scene != null) {
            if (darkModeToggle.isSelected()) {
                // Enable dark mode
                scene.getRoot().getStyleClass().add("dark-mode");
            } else {
                // Disable dark mode
                scene.getRoot().getStyleClass().remove("dark-mode");
            }
        }
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
                case FILE_MESSAGE:
                    // Check if it's a group file message or private file message
                    if (message.getGroupId() != null && !message.getGroupId().isEmpty()) {
                        handleIncomingGroupMessage(message);
                    } else {
                        handleIncomingPrivateMessage(message);
                    }
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
        Map<String, LocalDateTime> lastMessageDateTimes = new HashMap<>();
        Map<String, String> groupLastMessages = new HashMap<>(); // groupId -> lastMessage
        Map<String, LocalDateTime> groupLastMessageDateTimes = new HashMap<>(); // groupId -> lastMessageDateTime

        // Read message history file for current user
        String messagesFile = "chat_data/messages/" + currentUsername + ".txt";
        System.out.println("DEBUG: Attempting to load contacts from file: " + messagesFile);

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(messagesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    String messageType = parts[0];
                    String sender = parts[1];
                    String recipient = parts[2];
                    String messageContent = parts[3];
                    String groupId = parts[4];
                    String timestamp = parts[5];

                    if (("GROUP_MESSAGE".equals(messageType) || "FILE_MESSAGE".equals(messageType)) && groupId != null
                            && !groupId.isEmpty()) {
                        // Handle group message
                        String displayMessage;
                        String processedContent = convertEmojiPlaceholdersToDisplay(messageContent);
                        if (sender.equals(currentUsername)) {
                            displayMessage = "You: " + processedContent;
                        } else {
                            displayMessage = sender + ": " + processedContent;
                        }

                        groupLastMessages.put(groupId, displayMessage);

                        // Extract time from timestamp
                        try {
                            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(timestamp,
                                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            groupLastMessageDateTimes.put(groupId, dateTime);
                        } catch (Exception e) {
                            // Ignore parse errors
                        }
                    } else {
                        // Handle private message
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
                                displayMessage = sender + ": " + processedContent;
                            }

                            lastMessages.put(contactName, displayMessage);

                            // Extract time from timestamp
                            try {
                                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(timestamp,
                                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                lastMessageDateTimes.put(contactName, dateTime);
                            } catch (Exception e) {
                                // Ignore parse errors
                            }
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.out.println("DEBUG: No message history found for " + currentUsername + " or error reading file: "
                    + e.getMessage());
            // This is normal for new users
        }

        // Create ChatItem objects for private contacts
        for (String contact : contacts) {
            ChatItem contactItem = new ChatItem(contact, ChatItem.Type.USER, false);

            // Set last message and timestamp
            if (lastMessages.containsKey(contact)) {
                contactItem.setLastMessage(lastMessages.get(contact), lastMessageDateTimes.get(contact));
            }

            allChatItems.put(contact, contactItem);
        }

        // Update existing group items with last messages from history
        for (Map.Entry<String, String> entry : groupLastMessages.entrySet()) {
            String groupId = entry.getKey();
            String lastMessage = entry.getValue();

            // Find existing group item by ID
            for (ChatItem item : allChatItems.values()) {
                if (item.getType() == ChatItem.Type.GROUP && groupId.equals(item.getGroupId())) {
                    item.setLastMessage(lastMessage, groupLastMessageDateTimes.get(groupId));
                    break;
                }
            }
        }

        updateChatList();
        System.out.println("DEBUG: Loaded " + contacts.size()
                + " contacts and updated group messages from message history for " + currentUsername);
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
                    if (contactList.length() > 0)
                        contactList.append(",");
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
                // Note: If offline, the last seen status will be updated via USER_STATUS_UPDATE
                // messages
            }
        }

        updateChatList();
    }

    private void handleUserSearchResponse(Message message) {
        String response = message.getContent();
        if (response.isEmpty()) {
            return;
        }

        String currentSearchText = messageInput1.getText().toLowerCase();
        if (currentSearchText.isEmpty()) {
            return; // Search was cleared, ignore late response
        }

        Platform.runLater(() -> {
            Set<String> displayedUsernames = userListView.getItems().stream()
                    .map(ChatItem::getName)
                    .collect(Collectors.toSet());

            String[] users = response.split("\\|");
            boolean listChanged = false;
            for (String userInfo : users) {
                if (userInfo.trim().isEmpty())
                    continue;

                String[] parts = userInfo.split(" \\(");
                if (parts.length > 0) {
                    String username = parts[0].trim();

                    // Check if the result is relevant to the current search and not already
                    // displayed
                    if (!username.equals(currentUsername) &&
                            username.toLowerCase().contains(currentSearchText) &&
                            !displayedUsernames.contains(username)) {

                        boolean isOnline = userInfo.contains("(online)");
                        ChatItem userItem = new ChatItem(username, ChatItem.Type.USER, isOnline);
                        userListView.getItems().add(userItem);
                        listChanged = true;
                    }
                }
            }

            if (listChanged) {
                // Re-sort the list with the new items
                userListView.getItems().sort((a, b) -> {
                    if (a.getType() != b.getType()) {
                        return a.getType() == ChatItem.Type.GROUP ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                });
            }
        });
    }

    private void handleGroupListResponse(Message message) {
        String response = message.getContent();

        // Handle group notifications (when added to a group)
        if (response.startsWith("GROUP_ADDED:")) {
            handleGroupAddedNotification(response);
            return;
        }

        if (response.startsWith("REMOVED_FROM_GROUP:")) {
            handleGroupRemovedNotification(response);
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
            updateGroupLastMessagesFromHistory();
        }
    }

    private void updateGroupLastMessagesFromHistory() {
        Map<String, String> groupLastMessages = new HashMap<>();
        Map<String, LocalDateTime> groupLastMessageDateTimes = new HashMap<>();
        String messagesFile = "chat_data/messages/" + currentUsername + ".txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(messagesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 6 && "GROUP_MESSAGE".equals(parts[0])) {
                    String sender = parts[1];
                    String messageContent = parts[3];
                    String groupId = parts[4];
                    String timestamp = parts[5];

                    String processedContent = convertEmojiPlaceholdersToDisplay(messageContent);
                    String displayMessage = sender.equals(currentUsername) ? "You: " + processedContent
                            : sender + ": " + processedContent;

                    groupLastMessages.put(groupId, displayMessage);
                    try {
                        groupLastMessageDateTimes.put(groupId,
                                LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } catch (Exception e) {
                        /* ignore */
                    }
                }
            }
        } catch (IOException e) {
            /* ignore */
        }

        for (Map.Entry<String, String> entry : groupLastMessages.entrySet()) {
            String groupId = entry.getKey();
            for (ChatItem item : allChatItems.values()) {
                if (item.getType() == ChatItem.Type.GROUP && groupId.equals(item.getGroupId())) {
                    item.setLastMessage(entry.getValue(), groupLastMessageDateTimes.get(groupId));
                    break;
                }
            }
        }

        updateChatList();
    }

    private void handleGroupRemovedNotification(String response) {
        // Format: REMOVED_FROM_GROUP:groupName|groupId
        String[] parts = response.substring("REMOVED_FROM_GROUP:".length()).split("\\|");
        if (parts.length >= 2) {
            String groupName = parts[0];
            String groupId = parts[1];

            // Remove the group from the local list
            allChatItems.remove(groupName);
            userGroups.remove(groupId);

            // If the removed group was selected, clear the chat view
            if (selectedChat != null && groupId.equals(selectedChat.getGroupId())) {
                selectedChat = null;
                userListView.getSelectionModel().clearSelection();
                showSelectChatMessage();
                messageInput.setDisable(true);
                sendButton.setDisable(true);
                fileButton.setDisable(true);
                chatNameLabel.setText("Chat");
                chatStatusLabel.setText("");
            }

            updateChatList();
            showAlert("Removed from Group", "You have been removed from the group: " + groupName);
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

            // Remove any previous group name entry for this groupId
            String oldGroupName = null;
            for (Map.Entry<String, ChatItem> entry : allChatItems.entrySet()) {
                ChatItem item = entry.getValue();
                if (item.getType() == ChatItem.Type.GROUP && groupId.equals(item.getGroupId())
                        && !item.getName().equals(groupName)) {
                    oldGroupName = item.getName();
                    break;
                }
            }
            if (oldGroupName != null) {
                allChatItems.remove(oldGroupName);
            }
            // Remove old group name from userGroups if present
            for (Iterator<Map.Entry<String, String>> it = userGroups.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> entry = it.next();
                if (entry.getKey().equals(groupId) && !entry.getValue().equals(groupName)) {
                    it.remove();
                }
            }

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
        // Format:
        // GROUP_DETAILS:groupName|groupId|creator|MEMBERS:member1,member2,|ADMINS:admin1,admin2,|ONLINE:online1,online2,
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
            String statusText = selectedChat.getMemberCount() + " members, " + selectedChat.getOnlineCount()
                    + " online";
            chatStatusLabel.setText(statusText);
        }
    }

    private void updateChatList() {
        userListView.getItems().clear();
        userListView.getItems().addAll(allChatItems.values());

        // Sort by unread status first, then by last message time
        userListView.getItems().sort((a, b) -> {
            // Unread messages first
            if (a.hasUnreadMessages() != b.hasUnreadMessages()) {
                return a.hasUnreadMessages() ? -1 : 1;
            }

            // Then by last message time (most recent first)
            LocalDateTime timeA = a.getLastMessageDateTime();
            LocalDateTime timeB = b.getLastMessageDateTime();

            if (timeA != null && timeB != null) {
                return timeB.compareTo(timeA); // Descending order
            } else if (timeA != null) {
                return -1; // a has a time, b doesn't, so a comes first
            } else if (timeB != null) {
                return 1; // b has a time, a doesn't, so b comes first
            }

            // Finally by name for those without messages
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

        // Filter local results and display them
        String searchLower = searchText.toLowerCase();
        List<ChatItem> filteredContacts = allChatItems.values().stream()
                .filter(item -> item.getName().toLowerCase().contains(searchLower))
                .sorted((a, b) -> {
                    if (a.getType() != b.getType()) {
                        return a.getType() == ChatItem.Type.GROUP ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());
        userListView.getItems().setAll(filteredContacts);

        // If searching, also request search from server
        // Using length >= 1 to allow searching single characters
        if (searchText.length() >= 1) {
            try {
                Message searchMessage = new Message(Message.MessageType.USER_SEARCH, currentUsername);
                searchMessage.setContent(searchText.toLowerCase());
                chatClient.sendMessage(searchMessage);
            } catch (IOException e) {
                System.err.println("Failed to search users: " + e.getMessage());
            }
        }
    }

    private void selectChat(ChatItem chatItem) {
        selectedChat = chatItem;

        // Make chat header and input visible
        chatHeader.setVisible(true);
        messageInputArea.setVisible(true);

        // Reset last message sender for proper grouping in new chat
        lastMessageSender = null;

        // Mark as read
        chatItem.setHasUnreadMessages(false);
        Platform.runLater(this::updateChatList);

        // Update header
        chatNameLabel.setText(chatItem.getName());

        if (chatItem.getType() == ChatItem.Type.GROUP) {
            // Show group information
            String statusText = chatItem.getMemberCount() + " members, " + chatItem.getOnlineCount() + " online";
            chatStatusLabel.setText(statusText);

            // Make the status label interactive for hover effects
            chatStatusLabel.setOnMouseEntered(e -> showGroupMemberPopup(chatItem, e));
            chatStatusLabel.setOnMouseExited(e -> hideGroupMemberPopup());
            chatStatusLabel.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    showGroupManagementWindow(chatItem);
                }
            });

            // Style the status label to indicate it's interactive
            chatStatusLabel.getStyleClass().add("chat-status-clickable");

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
            chatStatusLabel.setOnMouseClicked(null);
            chatStatusLabel.getStyleClass().add("chat-status-normal");
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
        titleLabel.getStyleClass().add("group-member-popup-title");

        // Members list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox membersList = new VBox();
        membersList.setSpacing(3);

        for (String member : groupItem.getGroupMembers()) {
            HBox memberBox = new HBox();
            memberBox.setSpacing(5);
            memberBox.setAlignment(Pos.CENTER_LEFT);
            memberBox.setPadding(new Insets(3, 5, 3, 5));

            // Member name
            Label memberName = new Label(member);
            memberName.getStyleClass().add("group-member-name");

            memberBox.getChildren().add(memberName);

            // Online indicator - only shown if online, and to the right of the name
            if (groupItem.getOnlineMembers().contains(member)) {
                Circle onlineIndicator = new Circle(4, Color.LIMEGREEN);
                memberBox.getChildren().add(onlineIndicator);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Role indicator
            Label roleLabel = new Label();
            if (groupItem.isGroupCreator(member)) {
                roleLabel.setText("owner");
                roleLabel.getStyleClass().add("group-member-owner");
            } else if (groupItem.isGroupAdmin(member)) {
                roleLabel.setText("admin");
                roleLabel.getStyleClass().add("group-member-admin");
            }

            memberBox.getChildren().addAll(spacer, roleLabel);

            // Add right-click context menu for member management (only for admins/owner)
            if (groupItem.isGroupAdmin(currentUsername) || groupItem.isGroupCreator(currentUsername)) {
                memberBox.setOnContextMenuRequested(contextEvent -> {
                    showMemberContextMenu(groupItem, member, contextEvent);
                });
                memberBox.getStyleClass().add("group-member-item");
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

    private void showMemberContextMenu(ChatItem groupItem, String targetMember,
            javafx.scene.input.ContextMenuEvent event) {
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

        // Hide chat-specific controls
        chatHeader.setVisible(false);
        messageInputArea.setVisible(false);

        Label selectLabel = new Label("Select a chat to start messaging");
        selectLabel.getStyleClass().add("select-chat-message");

        VBox centerBox = new VBox(selectLabel);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPrefHeight(400);
        centerBox.getStyleClass().add("chatBoxScreen"); // Add CSS class for dark mode

        messagesContainer.getChildren().add(centerBox);
        fileButton.setDisable(true);
    }

    private void showPrivateChat(String contactName) {
        // Enable message input
        messageInput.setDisable(false);
        sendButton.setDisable(false);
        fileButton.setDisable(false);

        // Load conversation history for private chat
        loadConversationHistory(selectedChat);
    }

    private void showGroupChat(String groupId) {
        // Enable message input
        messageInput.setDisable(false);
        sendButton.setDisable(false);
        fileButton.setDisable(false);

        // Load conversation history for group chat
        loadConversationHistory(selectedChat);

        // Always request updated group information when entering a group chat
        requestDetailedGroupInfo(groupId);

        // Update display with current information (will be updated again when detailed
        // info arrives)
        updateGroupChatDisplay();
    }

    @FXML
    private void handleSendMessage() {
        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty() || selectedChat == null)
            return;

        // When sending a message to a user from search results, add them to contacts
        if (selectedChat.getType() == ChatItem.Type.USER && !allChatItems.containsKey(selectedChat.getName())) {
            allChatItems.put(selectedChat.getName(), selectedChat);
        }

        try {
            Message message;
            if (selectedChat.getType() == ChatItem.Type.GROUP) {
                message = new Message(Message.MessageType.GROUP_MESSAGE, currentUsername);
                message.setGroupId(selectedChat.getGroupId()); // Use groupId instead of name
            } else {
                message = new Message(Message.MessageType.PRIVATE_MESSAGE, currentUsername);
                message.setRecipient(selectedChat.getName());
            }

            // Keep the emoji placeholders in the message content
            message.setContent(messageText);

            chatClient.sendMessage(message);

            // Optimistic UI update
            addMessageToUI(message, true);

            // Update contact's last message (convert emoji placeholders to display format
            // for contact list)
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
        if (FileMessageData.isAFileReference(messageText)) {
            FileMessageData fileData = FileMessageData.fromString(messageText);
            return "File: " + fileData.getOriginalName();
        }
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
                    EmojiData emoji = allEmojis.get(emojiFilename);
                    String displayName = emoji != null && emoji.getName() != null ? "📷" : "📷";
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
        VBox fullMessageContainer = new VBox();
        fullMessageContainer.setSpacing(2);
        fullMessageContainer.setPadding(new Insets(2, 10, 2, 10));

        // Determine if we should show sender name for group chats
        boolean showSenderName = false;
        if (selectedChat != null && selectedChat.getType() == ChatItem.Type.GROUP && !isSentByMe) {
            // Show sender name if it's a different sender than the last message
            showSenderName = !message.getSender().equals(lastMessageSender);
        }

        // Add sender name label for group chats (if needed)
        if (showSenderName) {
            Label senderLabel = new Label(message.getSender());
            senderLabel.getStyleClass().add("sender-name-label");
            fullMessageContainer.getChildren().add(senderLabel);
        }

        // Create the message bubble
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);
        messageBox.setPadding(new Insets(3, 0, 3, 0));

        VBox messageContent = new VBox();
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(300); // Compact width like demo (280px CSS + padding)

        if (FileMessageData.isAFileReference(message.getContent())) {
            message.setFileMessageData(FileMessageData.fromString(message.getContent()));
        }

        if (message.getType() == Message.MessageType.FILE_MESSAGE && message.getFileMessageData() != null) {
            FileMessageData fileData = message.getFileMessageData();
            VBox fileBox = new VBox(6);
            fileBox.getStyleClass().add("file-message-box"); // Add a wrapper class for specificity

            // Check if this is an image file
            boolean isImageFile = fileData.getMimeType() != null && fileData.getMimeType().startsWith("image/");
            boolean isPng = fileData.getMimeType() != null &&
                    (fileData.getMimeType().equals("image/png") ||
                            fileData.getOriginalName().toLowerCase().endsWith(".png"));

            if (isImageFile) {
                // For image files, use completely transparent style - no border, no background
                fileBox.setPadding(new Insets(0));
                fileBox.setMaxWidth(360); // Slightly smaller width for a more elegant look

                // Apply completely transparent styling - no border at all
                fileBox.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-background-radius: 16;" +
                                "-fx-padding: 0;" +
                                "-fx-cursor: hand;" +
                                "-fx-max-width: 320px;" +
                                "-fx-max-height: 320px;");
            } else {
                // Standard file message styling for non-image files
                fileBox.setPadding(new Insets(8));
                fileBox.setMaxWidth(380); // 85% of 450 = ~380

                // Apply file message box styling directly with premium white background
                fileBox.setStyle(
                        "-fx-background-color: #FAF9F6;" + // Nice whitish background
                                "-fx-background-radius: 18;" +
                                "-fx-border-color: rgba(0,0,0,0.08);" +
                                "-fx-border-radius: 18;" +
                                "-fx-border-width: 1;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);" +
                                "-fx-padding: 12;");

                // Removed Platform.runLater to avoid CSS binding conflicts with text fill
            }

            // Add media preview if supported (takes most space)
            Node mediaPreview = createMediaPreview(fileData);
            if (mediaPreview != null) {
                fileBox.getChildren().add(mediaPreview);
            }

            // For image files, no buttons in chat bubble - keep it clean
            if (isImageFile) {
                // Just add the image file box without any buttons
                messageContent.getChildren().add(fileBox);
            } else {

            // Compact file info section (only for non-image files)
            HBox fileInfo = new HBox(8);
            fileInfo.setAlignment(Pos.CENTER_LEFT);
            fileInfo.setPadding(new Insets(4, 0, 4, 0));

            // Get appropriate file type icon with larger size
            String iconPath = getFileTypeIcon(fileData.getMimeType());
            ImageView fileIcon = new ImageView(
                    new Image(getClass().getResourceAsStream(iconPath)));
            fileIcon.setFitWidth(32); // Increased from 24
            fileIcon.setFitHeight(32); // Increased from 24

            VBox fileDetails = new VBox(2);

            // File name with larger, bold styling for better hierarchy
            Label fileName = new Label(fileData.getOriginalName());
            // Use CSS class for font styling and also add fallback inline styles
            fileName.getStyleClass().add("file-name-label");
            fileName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #000000;");
            fileName.setWrapText(true);
            fileName.setMaxWidth(200);

            // Compact file size with color-coded file type label
            HBox fileMetaInfo = new HBox(4);
            fileMetaInfo.setAlignment(Pos.CENTER_LEFT);

            // File size info with smaller font for details
            Label fileSizeLabel = new Label(fileData.getFormattedFileSize());
            // Use CSS class for font styling and also add fallback inline styles
            fileSizeLabel.getStyleClass().add("file-size-label");
            fileSizeLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: normal; -fx-text-fill: #666666;");

            // File type label with smaller font and color accent
            Label fileTypeLabel = new Label(getFileTypeDisplay(fileData.getMimeType()));
            // Use CSS class for font styling and also add fallback inline styles
            fileTypeLabel.getStyleClass().add("file-type-info-label");
            fileTypeLabel.getStyleClass().add("file-type-label"); // Add base class for padding/radius
            fileTypeLabel.setStyle("-fx-font-size: 7px; -fx-font-weight: normal; -fx-text-fill: #666666;");

            // Apply color-specific styling based on file type
            fileTypeLabel.getStyleClass().add(getFileTypeStyleClass(fileData.getMimeType()));

            fileMetaInfo.getChildren().addAll(fileSizeLabel, fileTypeLabel);
            fileDetails.getChildren().addAll(fileName, fileMetaInfo);
            fileInfo.getChildren().addAll(fileIcon, fileDetails);

            // Modern download button
            Button downloadButton = new Button("Download");
            downloadButton.getStyleClass().add("download-button");
            downloadButton.setStyle("-fx-text-fill: white;"); // Ensure text is white on blue background
            downloadButton.setOnAction(e -> downloadFile(fileData));

            Button openButton = new Button("Open");
            openButton.getStyleClass().add("open-button");
            openButton.setStyle("-fx-text-fill: white;"); // Ensure text is white on green background
            openButton.setOnAction(e -> openFile(fileData));

            // Create compact HBox for buttons
            HBox buttonContainer = new HBox(6);
            buttonContainer.setAlignment(Pos.CENTER_RIGHT); // Align buttons to the right
            buttonContainer.setPadding(new Insets(0, 0, 0, 0)); // Remove left padding
            buttonContainer.getChildren().addAll(downloadButton, openButton);

                // Add file info and buttons together in a compact layout
                VBox compactInfoSection = new VBox(4);
                compactInfoSection.setPrefWidth(280); // Set preferred width to ensure buttons can align right
                compactInfoSection.setMaxWidth(Double.MAX_VALUE); // Allow it to fill available space
                compactInfoSection.getChildren().addAll(fileInfo, buttonContainer);
                fileBox.getChildren().add(compactInfoSection);

                messageContent.getChildren().add(fileBox);
            }

            // No additional file box styling needed - already handled in styling above
        } else {
            // Create TextFlow with mixed text and emoji content
            TextFlow textFlow = createTextFlowWithEmojis(message.getContent());
            
            // Set text color based on message type (sent vs received)
            if (isSentByMe) {
                textFlow.setStyle("-fx-text-fill: white;");
            } else {
                textFlow.setStyle("-fx-text-fill: #2c3e50;");
            }
            
            messageContent.getChildren().add(textFlow);
        }

        // Apply message bubble styles directly in Java (no CSS classes)
        if (isSentByMe) {
            // Sent message bubble - Blue gradient with rounded corners
            messageContent.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #4568DC, #3498db);" +
                            "-fx-background-radius: 22 22 4 22;" +
                            "-fx-padding: 12 16 12 16;" +
                            "-fx-font-family: 'Inter', 'Segoe UI', 'Roboto', sans-serif;" +
                            "-fx-font-size: 15px;" +
                            "-fx-font-weight: 600;" +
                            "-fx-text-fill: white;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);" +
                            "-fx-max-width: 300px;" +
                            "-fx-border-width: 0;");

            // Force all text nodes to be white - REMOVED due to CSS binding conflicts
            // Platform.runLater(() -> {
            // messageContent.lookupAll(".text").forEach(node -> {
            // if (node instanceof Text) {
            // ((Text) node).setFill(Color.WHITE);
            // }
            // });
            // });
        } else {
            // Received message bubble - White background with rounded corners
            messageContent.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 22 22 22 4;" +
                            "-fx-padding: 12 16 12 16;" +
                            "-fx-font-family: 'Inter', 'Segoe UI', 'Roboto', sans-serif;" +
                            "-fx-font-size: 15px;" +
                            "-fx-font-weight: 600;" +
                            "-fx-text-fill: #2c3e50;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);" +
                            "-fx-max-width: 300px;" +
                            "-fx-border-color: rgba(0,0,0,0.05);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 22 22 22 4;");

            // Force all text nodes to be dark - REMOVED due to CSS binding conflicts
            // Platform.runLater(() -> {
            // messageContent.lookupAll(".text").forEach(node -> {
            // if (node instanceof Text) {
            // ((Text) node).setFill(Color.web("#2c3e50"));
            // }
            // });
            // });
        }

        // Create timestamp without checkmarks, always right-aligned
        String timeStr = message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(timeStr);
        // Apply JetBrains Mono font styling directly in Java
        timeLabel.setStyle(
                "-fx-font-family: 'Inter', 'Monaco', monospace;" +
                        "-fx-font-size: 8px;" +
                        "-fx-font-weight: normal;" +
                        "-fx-text-fill: inherit;" +
                        "-fx-padding: 2 0 0 0;" +
                        "-fx-opacity: 0.8;" +
                        "-fx-alignment: center-right;");

        // Add timestamp inside the message content (always right-aligned)
        messageContent.getChildren().add(timeLabel);

        // Set alignment for timestamp within the message content - always right-aligned
        VBox.setMargin(timeLabel, new Insets(2, 0, 0, 0)); // Small top margin
        // Ensure timestamp is always right-aligned by adding it to a right-aligned
        // container
        HBox timestampContainer = new HBox();
        timestampContainer.setAlignment(Pos.CENTER_RIGHT);
        timestampContainer.getChildren().add(timeLabel);

        // Remove the standalone timeLabel and add the container instead
        messageContent.getChildren().remove(timeLabel);
        messageContent.getChildren().add(timestampContainer);

        // Create message row with proper alignment (like in demo)
        HBox messageRow = new HBox();
        messageRow.setPadding(new Insets(2, 10, 2, 10));

        if (isSentByMe) {
            // Sent message: push to right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            messageRow.getChildren().addAll(spacer, messageContent);
        } else {
            // Received message: align to left with spacer on right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            messageRow.getChildren().addAll(messageContent, spacer);
        }

        fullMessageContainer.getChildren().add(messageRow);

        // Add the complete message container to the messages container
        messagesContainer.getChildren().add(fullMessageContainer);

        // Update last message sender for grouping
        lastMessageSender = message.getSender();

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
                    // Remove direct font setting to use CSS parent class styling
                    textFlow.getChildren().add(textNode);
                    currentText.setLength(0);
                }

                // Find the end of the emoji placeholder
                int endIndex = messageContent.indexOf("]", i);
                if (endIndex != -1) {
                    String emojiPlaceholder = messageContent.substring(i, endIndex + 1);
                    String emojiFilename = emojiPlaceholder.substring(7, emojiPlaceholder.length() - 1); // Remove
                    // [EMOJI: and
                    // ]

                    // Create emoji ImageView using sprite sheet
                    try {
                        EmojiData emojiData = allEmojis.get(emojiFilename);
                        if (emojiData != null) {
                            Image spriteSheet = loadSpriteSheet(emojiData.getCategory());
                            if (spriteSheet != null) {
                                ImageView emojiView = new ImageView(spriteSheet);
                                emojiView.setViewport(new javafx.geometry.Rectangle2D(
                                        emojiData.getX(), emojiData.getY(),
                                        emojiData.getWidth(), emojiData.getHeight()));
                                emojiView.setFitWidth(18);
                                emojiView.setFitHeight(18);
                                emojiView.setPreserveRatio(true);
                                // Ensure the emoji aligns with text baseline
                                emojiView.setTranslateY(2); // Move emoji down to align with text baseline
                                textFlow.getChildren().add(emojiView);
                            } else {
                                // Fallback to text emoji
                                Text fallbackEmoji = new Text("📷");
                                textFlow.getChildren().add(fallbackEmoji);
                            }
                        } else {
                            // Fallback to text emoji
                            Text fallbackEmoji = new Text("📷");
                            textFlow.getChildren().add(fallbackEmoji);
                        }
                    } catch (Exception e) {
                        // If emoji image fails to load, show the filename as text
                        Text fallbackText = new Text("[" + emojiFilename + "]");
                        fallbackText.getStyleClass().add("emoji-fallback-text");
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
            // Remove direct font setting to use CSS parent class styling
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
        String sender = message.getSender();
        String recipient = message.getRecipient();
        String processedContent = convertEmojiPlaceholdersToDisplay(message.getContent());
        String displayMessage;

        if (sender.equals(currentUsername)) {
            displayMessage = "You: " + processedContent;
        } else {
            displayMessage = sender + ": " + processedContent;
        }

        // Update chat item for the contact
        ChatItem contactItem = null;
        String contactName = sender.equals(currentUsername) ? recipient : sender;

        if (allChatItems.containsKey(contactName)) {
            contactItem = allChatItems.get(contactName);
            contactItem.setLastMessage(displayMessage, message.getTimestamp());
        } else {
            // Create new contact item if it doesn't exist
            contactItem = new ChatItem(contactName, ChatItem.Type.USER, false);
            contactItem.setLastMessage(displayMessage, message.getTimestamp());
            allChatItems.put(contactName, contactItem);
        }

        if (selectedChat == null || !selectedChat.getName().equals(contactName)) {
            contactItem.setHasUnreadMessages(true);
        }
        updateChatList();

        // Add to UI if it's for the current conversation
        if (selectedChat != null &&
                selectedChat.getType() == ChatItem.Type.USER &&
                (selectedChat.getName().equals(message.getSender()) ||
                        selectedChat.getName().equals(message.getRecipient()))) {

            // Handle FILE_MESSAGE types - ensure FileMessageData is properly set
            if (message.getType() == Message.MessageType.FILE_MESSAGE) {
                System.out.println("DEBUG: Processing FILE_MESSAGE in handleIncomingPrivateMessage");
                System.out.println("DEBUG: Message content: " + message.getContent());

                if (FileMessageData.isAFileReference(message.getContent())) {
                    try {
                        FileMessageData fileData = FileMessageData.fromString(message.getContent());
                        message.setFileMessageData(fileData);
                        System.out.println("DEBUG: Successfully parsed FileMessageData: " + fileData.getOriginalName());
                    } catch (Exception e) {
                        System.err.println("DEBUG: Error parsing FileMessageData: " + e.getMessage());
                    }
                } else {
                    System.err.println("DEBUG: Message content is not a valid file reference");
                }
            }
            addMessageToUI(message, message.getSender().equals(currentUsername));
        }
    }

    private void handleIncomingGroupMessage(Message message) {
        String groupId = message.getGroupId();
        ChatItem groupItem = null;
        for (ChatItem item : allChatItems.values()) {
            if (item.getType() == ChatItem.Type.GROUP && groupId.equals(item.getGroupId())) {
                groupItem = item;
                break;
            }
        }

        if (groupItem != null) {
            String sender = message.getSender();
            String displayMessage;

            if (message.getType() == Message.MessageType.FILE_MESSAGE) {
                FileMessageData fileData = FileMessageData.fromString(message.getContent());
                displayMessage = "File: " + fileData.getOriginalName();
            } else {
                displayMessage = convertEmojiPlaceholdersToDisplay(message.getContent());
            }

            if (sender.equals(currentUsername)) {
                displayMessage = "You: " + displayMessage;
            } else {
                displayMessage = sender + ": " + displayMessage;
            }
            groupItem.setLastMessage(displayMessage, message.getTimestamp());

            if (selectedChat == null || !selectedChat.getGroupId().equals(groupId)) {
                groupItem.setHasUnreadMessages(true);
            }
            updateChatList();
        }

        // Add to UI if it's for the current conversation
        if (selectedChat != null &&
                selectedChat.getType() == ChatItem.Type.GROUP &&
                selectedChat.getGroupId().equals(message.getGroupId())) {

            if (message.getType() == Message.MessageType.FILE_MESSAGE) {
                if (FileMessageData.isAFileReference(message.getContent())) {
                    message.setFileMessageData(FileMessageData.fromString(message.getContent()));
                }
            }
            addMessageToUI(message, message.getSender().equals(currentUsername));
        }
    }

    private void handleUserStatusUpdate(Message message) {
        String[] parts = message.getContent().split("\\|");
        if (parts.length >= 3) {
            String username = parts[0].trim();
            boolean isOnline = Boolean.parseBoolean(parts[1].trim());
            String statusText = parts[2].trim();

            System.out.println("DEBUG: Received status update for " + username + " - online: " + isOnline + ", status: "
                    + statusText);

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

        // Apply CSS class instead of inline styling
        settingsMenu.getStyleClass().add("settings-context-menu");

        settingsMenu.show(settingsButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showGroupCreationWorkflow() {
        // Create a custom dialog for group creation
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Create New Group");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setPrefSize(400, 200);
        mainContainer.getStyleClass().add("group-creation-dialog");

        // Header
        Label headerLabel = new Label("Create New Group");
        headerLabel.getStyleClass().add("dialog-header");

        // Group name input
        Label nameLabel = new Label("Group Name:");
        nameLabel.getStyleClass().add("dialog-label");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter group name...");
        nameField.setPrefHeight(35);
        nameField.getStyleClass().add("dialog-text-field");

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(80);
        cancelButton.getStyleClass().add("dialog-cancel-button");
        cancelButton.setOnAction(e -> dialogStage.close());

        Button createButton = new Button("Create Group");
        createButton.setPrefWidth(100);
        createButton.getStyleClass().add("dialog-create-button");
        createButton.setOnAction(e -> {
            String groupName = nameField.getText().trim();
            if (!groupName.isEmpty()) {
                dialogStage.close();
                showMemberSelectionPopup(groupName, false, null);
            }
        });

        buttonBox.getChildren().addAll(cancelButton, createButton);

        mainContainer.getChildren().addAll(headerLabel, nameLabel, nameField, buttonBox);

        Scene dialogScene = new Scene(mainContainer);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    private void showMemberSelectionPopup(String groupName, boolean isAddingMembers, String existingGroupId) {
        Stage memberSelectionStage = new Stage();
        memberSelectionStage.initModality(Modality.APPLICATION_MODAL);
        memberSelectionStage.setTitle(isAddingMembers ? "Add Members" : "Select Members");
        memberSelectionStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setPrefSize(450, 550);
        mainContainer.getStyleClass().add("member-selection-dialog");

        // Title
        Label titleLabel = new Label(isAddingMembers ? "Add Members to Group" : "Select Group Members");
        titleLabel.getStyleClass().add("dialog-header");

        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search contacts...");
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("member-selection-search-field");

        // Contact list with checkboxes
        ListView<CheckBox> contactListView = new ListView<>();
        contactListView.setPrefHeight(350);

        // Load contacts into the list, excluding existing group members
        Set<String> existingMembers = new HashSet<>();
        if (isAddingMembers && existingGroupId != null) {
            for (ChatItem item : allChatItems.values()) {
                if (item.getType() == ChatItem.Type.GROUP && existingGroupId.equals(item.getGroupId())) {
                    existingMembers.addAll(item.getGroupMembers());
                    break;
                }
            }
        }

        ObservableList<CheckBox> contactCheckBoxes = FXCollections.observableArrayList();
        for (ChatItem item : allChatItems.values()) {
            if (item.getType() == ChatItem.Type.USER) {
                String contactName = item.getName();
                if (!contactName.equals(currentUsername) && !existingMembers.contains(contactName)) {
                    CheckBox contactCheckBox = new CheckBox(contactName);
                    contactCheckBox.getStyleClass().add("member-selection-checkbox");
                    contactCheckBoxes.add(contactCheckBox);
                }
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
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(100);
        cancelButton.getStyleClass().add("dialog-cancel-button");
        cancelButton.setOnAction(e -> memberSelectionStage.close());

        Button createButton = new Button(isAddingMembers ? "Add Members" : "Create Group");
        createButton.setPrefWidth(120);
        createButton.getStyleClass().add("dialog-create-button");
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

            // Clear sprite sheet cache to free memory
            if (spriteSheetCache != null) {
                spriteSheetCache.clear();
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
        public enum Type {
            USER, GROUP
        }

        private String name;
        private Type type;
        private boolean online;
        private String groupId;
        private String lastMessage;
        private String lastMessageTimestamp;
        private LocalDateTime lastMessageDateTime;
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
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
            if (online && (lastSeenStatus == null || lastSeenStatus.startsWith("Last seen"))) {
                this.lastSeenStatus = "Online";
            }
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public void setLastMessage(String lastMessage) {
            setLastMessage(lastMessage, LocalDateTime.now());
        }

        public void setLastMessage(String lastMessage, LocalDateTime timestamp) {
            this.lastMessage = lastMessage;
            this.lastMessageDateTime = timestamp;
            if (timestamp != null) {
                this.lastMessageTimestamp = timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                this.lastMessageTimestamp = "";
            }
        }

        public String getLastMessageTimestamp() {
            return lastMessageTimestamp;
        }

        public void setLastMessageTimestamp(String timestamp) {
            this.lastMessageTimestamp = timestamp;
        }

        public LocalDateTime getLastMessageDateTime() {
            return lastMessageDateTime;
        }

        public void setLastMessageDateTime(LocalDateTime dateTime) {
            this.lastMessageDateTime = dateTime;
        }

        public boolean hasUnreadMessages() {
            return hasUnreadMessages;
        }

        public void setHasUnreadMessages(boolean hasUnread) {
            this.hasUnreadMessages = hasUnread;
        }

        public String getLastSeenStatus() {
            return lastSeenStatus;
        }

        public void setLastSeenStatus(String lastSeenStatus) {
            this.lastSeenStatus = lastSeenStatus;
        }

        // Group-specific getters and setters
        public Set<String> getGroupMembers() {
            return groupMembers != null ? groupMembers : new HashSet<>();
        }

        public void setGroupMembers(Set<String> groupMembers) {
            this.groupMembers = groupMembers;
        }

        public Set<String> getGroupAdmins() {
            return groupAdmins != null ? groupAdmins : new HashSet<>();
        }

        public void setGroupAdmins(Set<String> groupAdmins) {
            this.groupAdmins = groupAdmins;
        }

        public Set<String> getOnlineMembers() {
            return onlineMembers != null ? onlineMembers : new HashSet<>();
        }

        public void setOnlineMembers(Set<String> onlineMembers) {
            this.onlineMembers = onlineMembers;
        }

        public String getGroupCreator() {
            return groupCreator;
        }

        public void setGroupCreator(String groupCreator) {
            this.groupCreator = groupCreator;
        }

        public int getMemberCount() {
            return groupMembers != null ? groupMembers.size() : 0;
        }

        public int getOnlineCount() {
            return onlineMembers != null ? onlineMembers.size() : 0;
        }

        public boolean isGroupAdmin(String username) {
            return groupAdmins != null && groupAdmins.contains(username);
        }

        public boolean isGroupCreator(String username) {
            return groupCreator != null && groupCreator.equals(username);
        }
    }

    private class ChatItemCell extends ListCell<ChatItem> {
        @Override
        protected void updateItem(ChatItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
            } else {
                VBox content = new VBox();
                content.setSpacing(3);
                content.setPadding(new Insets(8, 10, 8, 10));
                content.getStyleClass().add("contact-cell");

                HBox nameBox = new HBox();
                nameBox.setAlignment(Pos.CENTER_LEFT);
                nameBox.setSpacing(8);

                Label nameLabel = new Label(item.getName());
                nameLabel.getStyleClass().add("contact-name");
                if (!item.hasUnreadMessages()) {
                    nameLabel.getStyleClass().add("contact-name-normal");
                }

                if (item.getType() == ChatItem.Type.GROUP) {
                    Label groupIcon = new Label("👥");
                    groupIcon.getStyleClass().add("group-icon");
                    nameBox.getChildren().addAll(groupIcon, nameLabel);
                } else {
                    nameBox.getChildren().add(nameLabel);
                    if (item.isOnline()) {
                        Circle statusCircle = new Circle(4, Color.web("#a3be8c"));
                        nameBox.getChildren().add(statusCircle);
                    }
                }

                content.getChildren().add(nameBox);

                // Show last message for all types instead of last seen status
                if (item.getLastMessage() != null && !item.getLastMessage().trim().isEmpty()) {
                    HBox messageBox = new HBox();
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageBox.setSpacing(8);

                    Label messageLabel = new Label(item.getLastMessage());
                    messageLabel.getStyleClass()
                            .add(item.hasUnreadMessages() ? "contact-message-unread" : "contact-message");

                    // Truncate long messages
                    if (messageLabel.getText().length() > 30) {
                        messageLabel.setText(messageLabel.getText().substring(0, 30) + "...");
                    }

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label timeLabel = new Label(item.getLastMessageTimestamp());
                    timeLabel.getStyleClass().add("contact-time");

                    messageBox.getChildren().addAll(messageLabel, spacer, timeLabel);
                    content.getChildren().add(messageBox);
                } else {
                    // Show "No messages yet" for contacts without messages
                    Label noMessageLabel = new Label("No messages yet");
                    noMessageLabel.getStyleClass().add("contact-no-message");
                    content.getChildren().add(noMessageLabel);
                }

                setGraphic(content);

                // Add context menu for deleting chat
                ContextMenu contextMenu = new ContextMenu();
                MenuItem deleteMenuItem = new MenuItem("Delete chat");
                deleteMenuItem.setOnAction(e -> promptToDeleteChat(item));
                contextMenu.getItems().add(deleteMenuItem);
                setContextMenu(contextMenu);
            }
        }
    }

    // Emoji picker functionality
    @FXML
    private void handleEmojiPicker() {
        showModernEmojiPicker();
    }

    private void showModernEmojiPicker() {
        // Create main popup
        Popup emojiPopup = new Popup();
        emojiPopup.setAutoHide(false); // Prevent auto-hiding so multiple emojis can be selected
        emojiPopup.setHideOnEscape(true); // Allow closing with Escape key

        // Main container with modern styling
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(0);
        mainContainer.setPrefSize(390, 400); // Increased width to accommodate 6 categories
        mainContainer.getStyleClass().addAll("emoji-picker-popup", "modern-emoji-picker");
        // Remove inline styles to allow CSS to control styling

        // Search bar
        TextField searchField = new TextField();
        searchField.setPromptText("Search emojis...");
        searchField.setPrefHeight(36);
        searchField.getStyleClass().add("emoji-search-field");
        VBox.setMargin(searchField, new Insets(12, 12, 8, 12));

        // Category tabs container with main visible categories
        HBox categoryContainer = new HBox();
        categoryContainer.setPrefHeight(46);
        categoryContainer.setAlignment(Pos.CENTER);
        categoryContainer.setPadding(new Insets(0));
        categoryContainer.getStyleClass().add("emoji-category-container");

        // Main category tabs - will show the first few categories
        HBox categoryTabs = new HBox();
        categoryTabs.setSpacing(4); // Slightly larger spacing for better readability
        categoryTabs.setPadding(new Insets(8, 8, 8, 16)); // More padding on the left side
        categoryTabs.setAlignment(Pos.CENTER_LEFT); // Left align buttons
        categoryTabs.setPrefWidth(390); // Use full available width for better distribution

        // More categories dropdown button with context menu
        Button moreButton = new Button("More");
        moreButton.getStyleClass().add("emoji-category-button");
        // Style to match other buttons
        moreButton.getStyleClass().add("emoji-more-button");
        moreButton.setPrefHeight(30);
        moreButton.setPrefWidth(55); // Width for "More" text
        moreButton.setTooltip(new Tooltip("More categories"));

        // Create context menu for dropdown functionality
        ContextMenu moreMenu = new ContextMenu();

        // Emoji canvas container
        ScrollPane emojiScrollPane = new ScrollPane();
        emojiScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        emojiScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        emojiScrollPane.setFitToWidth(true);
        emojiScrollPane.setPrefHeight(350);
        emojiScrollPane.getStyleClass().add("emoji-scroll-pane");

        // Create canvas for emoji rendering
        EmojiCanvas emojiCanvas = new EmojiCanvas();
        emojiScrollPane.setContent(emojiCanvas);

        // Create category buttons and populate initial view
        Map<String, Button> categoryButtons = new HashMap<>();
        String[] categories = { "Smileys", "People", "Animals & Nature", "Food", "Activities", "Places", "Objects",
                "Symbols", "Flags" };
        // Use abbreviated labels for display to save space
        String[] shortLabels = { "Smileys", "People", "Animals", "Food", "Activities", "Places", "Objects", "Symbols",
                "Flags" };

        // Show first 5 categories in the main tab bar and add a "More" button
        int visibleCategories = 5;
        // Add Places, Objects, Symbols, Flags to dropdown menu
        String[] additionalCategories = { "Places", "Objects", "Symbols", "Flags" };

        // Add the visible category buttons
        for (int i = 0; i < visibleCategories; i++) {
            String category = categories[i];
            String label = shortLabels[i];
            Button categoryBtn = createCategoryButton(label, category);
            categoryButtons.put(category, categoryBtn);

            // Set up click handler for category buttons
            categoryBtn.setOnAction(e -> {
                String actualCategory = (String) categoryBtn.getUserData();
                selectCategory(actualCategory, categoryButtons, emojiCanvas);
            });

            categoryTabs.getChildren().add(categoryBtn);
        }

        // Populate the More menu with the remaining categories
        for (int i = 0; i < additionalCategories.length; i++) {
            String additionalCategory = additionalCategories[i];
            String additionalLabel = shortLabels[visibleCategories + i];

            MenuItem menuItem = new MenuItem(additionalLabel);
            menuItem.setUserData(additionalCategory);
            menuItem.setOnAction(e -> {
                String category = (String) menuItem.getUserData();
                selectCategory(category, categoryButtons, emojiCanvas);
            });
            moreMenu.getItems().add(menuItem);
        }

        // Set the action to show the context menu
        moreButton.setOnAction(e -> {
            moreMenu.show(moreButton, Side.BOTTOM, 0, 0);
        });

        // Add the remaining categories to the buttons map
        for (int i = visibleCategories; i < categories.length; i++) {
            String category = categories[i];
            String label = shortLabels[i];
            Button categoryBtn = createCategoryButton(label, category);
            categoryButtons.put(category, categoryBtn);
            // These buttons won't be added to the UI, but will be used for styling
        }

        // Add the More button at the end
        categoryTabs.getChildren().add(moreButton);

        // Load initial category (Smileys by default)
        selectCategory("Smileys", categoryButtons, emojiCanvas);

        // Search functionality
        Timeline searchDebouncer = new Timeline();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDebouncer.stop();
            searchDebouncer.getKeyFrames().clear();
            searchDebouncer.getKeyFrames().add(new KeyFrame(Duration.millis(300), event -> {
                if (newValue.trim().isEmpty()) {
                    // Return to Smileys category
                    selectCategory("Smileys", categoryButtons, emojiCanvas);
                } else {
                    // Show search results
                    showSearchResults(newValue, emojiCanvas, emojiPopup);
                }
            }));
            searchDebouncer.play();
        });

        // Add categories directly to the container for proper layout
        categoryContainer.getChildren().add(categoryTabs);

        // Assemble popup
        mainContainer.getChildren().addAll(searchField, categoryContainer, emojiScrollPane);

        // Register the popup with the canvas for later access
        emojiCanvas.setUserData(emojiPopup);
        emojiPopup.getContent().add(mainContainer);

        // Add event filter to handle clicking outside the popup (since autoHide is
        // disabled)
        Scene scene = messageInput.getScene();
        if (scene != null) {
            // Create a event handler for mouse clicks outside the popup
            javafx.event.EventHandler<MouseEvent> clickOutsideHandler = new javafx.event.EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (emojiPopup.isShowing()) {
                        Node content = emojiPopup.getContent().get(0);
                        Bounds contentBounds = content.localToScreen(content.getBoundsInLocal());

                        // If click is outside the popup's content bounds, hide it
                        if (contentBounds == null ||
                                event.getScreenX() < contentBounds.getMinX() ||
                                event.getScreenX() > contentBounds.getMaxX() ||
                                event.getScreenY() < contentBounds.getMinY() ||
                                event.getScreenY() > contentBounds.getMaxY()) {
                            emojiPopup.hide();
                            scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, this);
                        }
                    }
                }
            };

            scene.addEventFilter(MouseEvent.MOUSE_PRESSED, clickOutsideHandler);
        }

        // Position popup
        Button emojiButton = (Button) messageInput.getParent().lookup("#emojiButton");
        if (emojiButton != null) {
            emojiPopup.show(emojiButton,
                    emojiButton.localToScreen(emojiButton.getBoundsInLocal()).getMinX() - 50,
                    emojiButton.localToScreen(emojiButton.getBoundsInLocal()).getMinY() - 410);
        }
    }

    private Button createCategoryButton(String label, String category) {
        Button btn = new Button(label);

        // Store the actual category in the user data
        btn.setUserData(category);

        // Configure button properties with text labels
        btn.setPrefHeight(30);
        btn.setMinHeight(30);
        btn.setPrefWidth(55); // Fixed width for uniform appearance
        btn.setMinWidth(55);
        // Make buttons more visually appealing
        btn.getStyleClass().add("emoji-button");
        // Remove programmatic font setting to allow CSS to control font size

        // Set tooltip to show full category name
        if (!label.equals(category)) {
            btn.setTooltip(new Tooltip(category));
        }

        return btn;
    }

    private void selectCategory(String category, Map<String, Button> categoryButtons, EmojiCanvas emojiCanvas) {
        System.out.println("DEBUG: Selecting category: " + category);
        activeCategory = category;

        // Update button styles
        for (Map.Entry<String, Button> entry : categoryButtons.entrySet()) {
            Button btn = entry.getValue();
            if (entry.getKey().equals(category)) {
                btn.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 1; " +
                        "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 2 4; -fx-font-weight: bold;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                        "-fx-background-radius: 6; -fx-padding: 2 4; -fx-font-weight: normal;");
            }
        }

        // Load emojis for category
        List<EmojiData> emojis = getCategoryEmojis(category);
        System.out.println("DEBUG: Found " + emojis.size() + " emojis for category: " + category);

        // Load the sprite sheet for this category
        Image spriteSheet = loadSpriteSheet(category);
        if (spriteSheet != null) {
            // Set emojis and sprite sheet for the canvas
            emojiCanvas.setEmojis(emojis, spriteSheet);
        }
    }

    private List<EmojiData> getCategoryEmojis(String category) {
        return emojiCategories.getOrDefault(category, new ArrayList<>());
    }

    private void showSearchResults(String query, EmojiCanvas emojiCanvas, Popup popup) {
        List<EmojiData> searchResults = searchEmojis(query);
        // We need to choose an appropriate sprite sheet for the search results
        // Using a general category or "Smileys" as default
        String category = "Smileys"; // Default

        // Try to get a specific category if all results belong to the same category
        if (!searchResults.isEmpty()) {
            boolean singleCategory = true;
            String firstCategory = searchResults.get(0).getCategory();
            for (EmojiData emoji : searchResults) {
                if (!emoji.getCategory().equals(firstCategory)) {
                    singleCategory = false;
                    break;
                }
            }
            if (singleCategory) {
                category = firstCategory;
            }
        }

        // Load sprite sheet and set emojis
        Image spriteSheet = loadSpriteSheet(category);
        if (spriteSheet != null) {
            emojiCanvas.setEmojis(searchResults, spriteSheet);
        }
    }

    private List<EmojiData> searchEmojis(String query) {
        String lowerQuery = query.toLowerCase();
        return allEmojis.values().stream()
                .filter(emoji -> emoji.getName() != null &&
                        emoji.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // We've replaced the old GridPane-based emoji grid with a Canvas-based
    // implementation
    // that draws emojis directly from sprite sheets without creating hundreds of
    // nodes

    private Image loadSpriteSheet(String category) {
        // Check cache first
        if (spriteSheetCache.containsKey(category)) {
            return spriteSheetCache.get(category);
        }

        try {
            String imagePath = EMOJI_SPRITES_PATH + category + ".png";
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            if (imageStream != null) {
                Image spriteSheet = new Image(imageStream);
                // Cache the loaded image
                spriteSheetCache.put(category, spriteSheet);
                System.out.println("Loaded and cached sprite sheet for category: " + category);
                return spriteSheet;
            }
        } catch (Exception e) {
            System.err.println("Error loading sprite sheet for category " + category + ": " + e.getMessage());
        }
        return null;
    }

    private void insertEmojiIntoMessage(String emojiFilename) {
        String currentText = messageInput.getText();

        // Simple approach: always insert at the end of current text
        // This is more reliable than trying to track caret position with focus issues
        int insertPosition = currentText.length();

        // Debug output
        System.out.println("DEBUG: Current text: '" + currentText + "'");
        System.out.println("DEBUG: Inserting at position: " + insertPosition);

        // Insert emoji placeholder in text
        String emojiPlaceholder = "[EMOJI:" + emojiFilename + "]";
        String newText = currentText + emojiPlaceholder;

        messageInput.setText(newText);

        // Set caret position after the inserted emoji and request focus
        Platform.runLater(() -> {
            messageInput.requestFocus();
            messageInput.positionCaret(newText.length());
        });

        System.out.println("DEBUG: New text: '" + newText + "'");
        System.out.println("DEBUG: Caret positioned at end: " + newText.length());
    }

    @FXML
    private void handleChatMenu() {
        if (selectedChat == null)
            return;

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
            chatMenu.getItems().add(addMembersItem); // <-- Always add for group members

            // Only show 'Change group name' to admins and owner
            if (selectedChat.isGroupAdmin(currentUsername) || selectedChat.isGroupCreator(currentUsername)) {
                MenuItem changeNameItem = new MenuItem("Change group name");
                changeNameItem.setOnAction(e -> showChangeGroupNameDialog());
                chatMenu.getItems().add(changeNameItem);
                System.out.println("DEBUG: Added admin/owner menu items");
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
            // MenuItem clearChatItem = new MenuItem("Clear chat"); // Removed as per new
            // implementation
            chatMenu.getItems().addAll(blockUserItem);
        }

        if (!chatMenu.getItems().isEmpty()) {
            // Apply CSS styling to the context menu
            chatMenu.setStyle(
                    "-fx-background-color: #eceff4; -fx-background-radius: 8; -fx-border-color: #d8dee9; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 8 0; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 10, 0, 0, 2);");

            chatMenu.show(chatMenuButton, javafx.geometry.Side.BOTTOM, 0, 0);
        } else {
            System.out.println("DEBUG: No menu items to show");
        }
    }

    private void showChangeGroupNameDialog() {
        if (selectedChat == null || selectedChat.getType() != ChatItem.Type.GROUP)
            return;

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
        if (selectedChat == null || selectedChat.getType() != ChatItem.Type.GROUP)
            return;

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

    private void promptToDeleteChat(ChatItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Chat");
        alert.setHeaderText("Delete chat with " + item.getName());
        alert.setContentText("Are you sure you want to delete this chat history? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteChatHistory(item);
        }
    }

    private void deleteChatHistory(ChatItem item) {
        String messagesFilePath = "chat_data/messages/" + currentUsername + ".txt";
        File messagesFile = new File(messagesFilePath);
        if (!messagesFile.exists()) {
            return; // Nothing to delete
        }

        List<String> linesToKeep = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(messagesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 6) {
                    linesToKeep.add(line); // Keep malformed lines
                    continue;
                }

                boolean keepLine = true;
                if (item.getType() == ChatItem.Type.USER) {
                    String messageType = parts[0];
                    if ("PRIVATE_MESSAGE".equals(messageType)) {
                        String sender = parts[1];
                        String recipient = parts[2];
                        if ((sender.equals(currentUsername) && recipient.equals(item.getName())) ||
                                (sender.equals(item.getName()) && recipient.equals(currentUsername))) {
                            keepLine = false;
                        }
                    }
                } else if (item.getType() == ChatItem.Type.GROUP) {
                    String messageType = parts[0];
                    if ("GROUP_MESSAGE".equals(messageType)) {
                        String groupId = parts[4];
                        if (item.getGroupId().equals(groupId)) {
                            keepLine = false;
                        }
                    }
                }

                if (keepLine) {
                    linesToKeep.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not read chat history to delete messages.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(messagesFile, false))) { // false to overwrite
            for (String line : linesToKeep) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not update chat history.");
            return;
        }

        // Refresh UI
        if (selectedChat != null && selectedChat.getName().equals(item.getName())) {
            selectedChat = null;
            userListView.getSelectionModel().clearSelection();
            showSelectChatMessage();
            messageInput.setDisable(true);
            sendButton.setDisable(true);
            fileButton.setDisable(true);
            chatNameLabel.setText("Chat");
            chatStatusLabel.setText("");
        }

        // Send a message to the server to delete the history there as well
        try {
            Message deleteHistoryMessage = new Message(Message.MessageType.DELETE_HISTORY, currentUsername);
            if (item.getType() == ChatItem.Type.USER) {
                deleteHistoryMessage.setRecipient(item.getName());
            } else if (item.getType() == ChatItem.Type.GROUP) {
                deleteHistoryMessage.setGroupId(item.getGroupId());
            }
            chatClient.sendMessage(deleteHistoryMessage);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not send delete request to the server.");
        }

        loadContacts();
    }

    private Stage managementStage;

    private boolean isManagementWindowOpen() {
        return managementStage != null && managementStage.isShowing();
    }

    private void showGroupManagementWindow(ChatItem groupItem) {
        if (isManagementWindowOpen()) {
            managementStage.toFront();
            return;
        }

        managementStage = new Stage();
        managementStage.initModality(Modality.WINDOW_MODAL);
        managementStage.initOwner(chatStatusLabel.getScene().getWindow());
        managementStage.setTitle("Manage Group: " + groupItem.getName());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setPrefSize(450, 600);

        Label title = new Label("Members (" + groupItem.getMemberCount() + ")");
        title.getStyleClass().add("group-management-title");

        ListView<HBox> memberListView = new ListView<>();
        refreshManagementWindow(memberListView, groupItem);

        Button addMemberButton = new Button("Add Member");
        addMemberButton.setOnAction(e -> showMemberSelectionPopup("", true, groupItem.getGroupId()));

        HBox bottomBar = new HBox(addMemberButton);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(title, memberListView, bottomBar);

        Scene scene = new Scene(layout);
        managementStage.setScene(scene);
        managementStage.show();
    }

    private void refreshManagementWindow(ListView<HBox> memberListView, ChatItem groupItem) {
        memberListView.getItems().clear();
        boolean isCurrentUserAdmin = groupItem.isGroupAdmin(currentUsername)
                || groupItem.isGroupCreator(currentUsername);

        for (String member : groupItem.getGroupMembers()) {
            HBox memberRow = new HBox(10);
            memberRow.setAlignment(Pos.CENTER_LEFT);
            memberRow.setPadding(new Insets(5));

            Label nameLabel = new Label(member);
            Label roleLabel = new Label();
            if (groupItem.isGroupCreator(member)) {
                roleLabel.setText("(Owner)");
                roleLabel.setStyle("-fx-text-fill: #d9534f;");
            } else if (groupItem.isGroupAdmin(member)) {
                roleLabel.setText("(Admin)");
                roleLabel.setStyle("-fx-text-fill: #5cb85c;");
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            memberRow.getChildren().addAll(nameLabel, roleLabel, spacer);

            if (isCurrentUserAdmin && !member.equals(currentUsername)) {
                // Add management buttons for admins
                if (groupItem.isGroupCreator(currentUsername) && !groupItem.isGroupCreator(member)) {
                    if (groupItem.isGroupAdmin(member)) {
                        Button demoteButton = new Button("Demote");
                        demoteButton.setOnAction(e -> removeAdmin(groupItem.getGroupId(), member));
                        memberRow.getChildren().add(demoteButton);
                    } else {
                        Button promoteButton = new Button("Promote");
                        promoteButton.setOnAction(e -> promoteToAdmin(groupItem.getGroupId(), member));
                        memberRow.getChildren().add(promoteButton);
                    }
                }

                if (!groupItem.isGroupCreator(member)) {
                    Button removeButton = new Button("Remove");
                    removeButton.setOnAction(e -> removeMemberFromGroup(groupItem.getGroupId(), member));
                    memberRow.getChildren().add(removeButton);
                }
            }
            memberListView.getItems().add(memberRow);
        }
    }

    @FXML
    private void handleSendFile() {
        if (selectedChat == null) {
            return;
        }

        // Use JavaFX FileChooser for better integration
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");

        // Set initial directory to user's home directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // Add file filters for common file types
        FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("All Files", "*.*");
        FileChooser.ExtensionFilter imageFiles = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg",
                "*.jpeg", "*.gif", "*.bmp");
        FileChooser.ExtensionFilter videoFiles = new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi",
                "*.mov", "*.mkv", "*.wmv");
        FileChooser.ExtensionFilter audioFiles = new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav",
                "*.flac", "*.aac", "*.ogg");
        FileChooser.ExtensionFilter documentFiles = new FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.doc",
                "*.docx", "*.txt", "*.rtf");

        fileChooser.getExtensionFilters().addAll(imageFiles, videoFiles, audioFiles, documentFiles, allFiles);

        // Show the file chooser dialog
        File selectedFile = fileChooser.showOpenDialog(ChatApplication.getPrimaryStage());

        if (selectedFile != null && selectedFile.exists()) {
            sendFile(selectedFile);
        }
    }

    private void sendFile(File file) {
        new Thread(() -> {
            try {
                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);

                // Check if it's a video file to show progress
                String mimeType = null;
                try {
                    mimeType = Files.probeContentType(file.toPath());
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }
                } catch (Exception e) {
                    mimeType = "application/octet-stream";
                }

                boolean isVideo = mimeType != null && mimeType.startsWith("video/");
                ProgressCallback progressCallback = null;

                if (isVideo) {
                    // Create progress callback for video files
                    progressCallback = new ProgressCallback() {
                        @Override
                        public void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed,
                                long totalBytes) {
                            double progress = (double) bytesProcessed / totalBytes;
                            Platform.runLater(() -> {
                                // Use unified progress bar
                                showUnifiedProgress(progress,
                                        "Sending video... " + String.format("%d%%", (int) (progress * 100)));
                            });
                        }

                        @Override
                        public void onTransferComplete() {
                            Platform.runLater(() -> {
                                // Hide unified progress bar
                                hideUnifiedProgress();
                            });
                        }

                        @Override
                        public void onTransferError(String error) {
                            Platform.runLater(() -> {
                                hideUnifiedProgress();
                                showAlert("Upload Error", "Failed to upload video: " + error);
                            });
                        }
                    };
                }

                CompletableFuture<String> uploadFuture = chunkSender.sendFile(file, progressCallback);
                String fileID = uploadFuture.get(); // Wait for completion

                if (fileID == null) {
                    Platform.runLater(() -> {
                        resetSendButton();
                        showAlert("Error", "Failed to upload file.");
                    });
                    return;
                }

                String originalName = file.getName();
                String extension = "";
                int i = originalName.lastIndexOf('.');
                if (i > 0) {
                    extension = originalName.substring(i);
                }
                String uuidName = fileID; // Use just the UUID without extension
                long fileSize = file.length();
                String finalMimeType = mimeType;

                FileMessageData fileMessageData = new FileMessageData(originalName, uuidName, fileSize, finalMimeType);

                // Create and send the file message
                Message fileMessage = new Message(Message.MessageType.FILE_MESSAGE, currentUsername);
                if (selectedChat.getType() == ChatItem.Type.GROUP) {
                    fileMessage.setGroupId(selectedChat.getGroupId()); // Use groupId for group messages
                } else {
                    fileMessage.setRecipient(selectedChat.getName()); // Use recipient for private messages
                }
                fileMessage.setFileMessageData(fileMessageData);
                fileMessage.setContent(fileMessageData.toString()); // Set the content field for serialization
                fileMessage.setTimestamp(LocalDateTime.now());

                // Send the message to server
                chatClient.sendMessage(fileMessage);

                // Add to UI immediately (optimistic update)
                Platform.runLater(() -> {
                    try {
                        addMessageToUI(fileMessage, true);

                        // Update the chat list with file message info
                        String displayMessage = "File: " + fileMessageData.getOriginalName();
                        selectedChat.setLastMessage("You: " + displayMessage);
                        selectedChat.setHasUnreadMessages(false);
                        updateChatList();

                    } catch (Exception e) {
                        System.err.println("Error updating UI after file send: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    resetSendButton();
                    showAlert("Error", "Failed to send file: " + e.getMessage());
                });
            }
        }).start();
    }

    private String getFileTypeDisplay(String mimeType) {
        if (mimeType == null)
            return "Unknown";

        if (mimeType.startsWith("image/"))
            return "Image";
        if (mimeType.startsWith("video/"))
            return "Video";
        if (mimeType.startsWith("audio/"))
            return "Audio";
        if (mimeType.startsWith("text/"))
            return "Text";
        if (mimeType.equals("application/pdf"))
            return "PDF";
        if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mimeType.startsWith("application/msword"))
            return "Word";
        if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                mimeType.startsWith("application/vnd.ms-excel"))
            return "Excel";
        if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
                mimeType.startsWith("application/vnd.ms-powerpoint"))
            return "PowerPoint";
        if (mimeType.startsWith("application/zip") ||
                mimeType.startsWith("application/x-rar") ||
                mimeType.startsWith("application/x-7z"))
            return "Archive";
        if (mimeType.startsWith("application/x-msdownload") ||
                mimeType.startsWith("application/x-executable") ||
                mimeType.endsWith("exe") ||
                mimeType.endsWith("dll") ||
                mimeType.endsWith("bat") ||
                mimeType.endsWith("cmd") ||
                mimeType.endsWith("sh"))
            return "Program";
        if (mimeType.startsWith("application/"))
            return "Document";

        return "File";
    }

    private String getFileTypeStyleClass(String mimeType) {
        if (mimeType == null)
            return "file-type-label-other";

        if (mimeType.startsWith("image/"))
            return "file-type-label-image";
        if (mimeType.startsWith("audio/"))
            return "file-type-label-audio";
        if (mimeType.startsWith("video/"))
            return "file-type-label-video";
        if (mimeType.startsWith("text/"))
            return "file-type-label-text";
        if (mimeType.equals("application/pdf"))
            return "file-type-label-pdf";
        if (mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z"))
            return "file-type-label-archive";
        if (mimeType.contains("word"))
            return "file-type-label-document";
        if (mimeType.contains("excel") || mimeType.contains("spreadsheet"))
            return "file-type-label-spreadsheet";
        if (mimeType.contains("powerpoint") || mimeType.contains("presentation"))
            return "file-type-label-presentation";
        if (mimeType.startsWith("application/"))
            return "file-type-label-program";

        return "file-type-label-other";
    }

    private String getFileTypeColor(String mimeType) {
        if (mimeType == null)
            return "#6B7280"; // Default gray

        String baseType = mimeType.split("/")[0];
        switch (baseType) {
            case "image":
                return "#10B981"; // Green for images
            case "video":
                return "#3B82F6"; // Blue for videos
            case "audio":
                return "#8B5CF6"; // Purple for audio
            case "text":
                return "#F59E0B"; // Amber for text files
            case "application":
                if (mimeType.contains("pdf"))
                    return "#EF4444"; // Red for PDF
                if (mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("rar")) {
                    return "#6366F1"; // Indigo for archives
                }
                if (mimeType.contains("json") || mimeType.contains("xml")) {
                    return "#F59E0B"; // Amber for data files
                }
                return "#6B7280"; // Gray for other applications
            default:
                return "#6B7280"; // Default gray
        }
    }

    private String getFileTypeIcon(String mimeType) {
        String baseIconPath = "/com/bakbak/javafx_proj_1_2/icons/";

        if (mimeType == null)
            return baseIconPath + "file_icon.png";

        if (mimeType.startsWith("image/"))
            return baseIconPath + "image_file_icon.png";
        if (mimeType.startsWith("video/"))
            return baseIconPath + "video_file_icon.png";
        if (mimeType.startsWith("audio/"))
            return baseIconPath + "audio_file_icon.png";
        if (mimeType.startsWith("text/plain"))
            return baseIconPath + "txt_file_icon.png";
        if (mimeType.startsWith("text/html"))
            return baseIconPath + "html_file_icon.png";
        if (mimeType.startsWith("application/json"))
            return baseIconPath + "json_file_icon.png";
        if (mimeType.contains("xml"))
            return baseIconPath + "xml_file_icon.png";
        if (mimeType.contains("csv"))
            return baseIconPath + "csv_file_icon.png";
        if (mimeType.equals("application/pdf"))
            return baseIconPath + "pdf_file_icon.png";
        if (mimeType.contains("word") || mimeType.contains("document"))
            return baseIconPath + "doc_file_icon.png";
        if (mimeType.contains("excel") || mimeType.contains("spreadsheet"))
            return baseIconPath + "excel_file_icon.png";
        if (mimeType.contains("powerpoint") || mimeType.contains("presentation"))
            return baseIconPath + "ppt_file_icon.png";
        if (mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z")
                || mimeType.contains("archive"))
            return baseIconPath + "archive_file_icon.png";
        if (mimeType.contains("exe") || mimeType.contains("dll") || mimeType.contains("bat") ||
                mimeType.contains("cmd") || mimeType.contains("sh") || mimeType.contains("executable"))
            return baseIconPath + "exe_file_icon.png";

        return baseIconPath + "file_icon.png";
    }

    /**
     * Get the JavaFX Color object for a given file type style class
     */
    private Color getColorForFileType(String styleClass) {
        switch (styleClass) {
            case "file-type-image":
                return Color.valueOf("#e83e8c"); // Pink
            case "file-type-audio":
                return Color.valueOf("#6f42c1"); // Purple
            case "file-type-video":
                return Color.valueOf("#fd7e14"); // Orange
            case "file-type-text":
                return Color.valueOf("#6c757d"); // Gray
            case "file-type-json":
            case "file-type-xml":
                return Color.valueOf("#17a2b8"); // Teal
            case "file-type-html":
                return Color.valueOf("#e83e8c"); // Pink
            case "file-type-csv":
                return Color.valueOf("#28a745"); // Green
            case "file-type-pdf":
                return Color.valueOf("#dc3545"); // Red
            case "file-type-archive":
                return Color.valueOf("#6610f2"); // Deep Purple
            case "file-type-document":
                return Color.valueOf("#007bff"); // Blue
            case "file-type-spreadsheet":
                return Color.valueOf("#28a745"); // Green
            case "file-type-presentation":
                return Color.valueOf("#fd7e14"); // Orange
            case "file-type-program":
                return Color.valueOf("#0d6efd"); // Bright Blue
            default:
                return Color.valueOf("#6c757d"); // Gray
        }
    }

    /**
     * Get the RGBA color string for CSS styling
     */
    private String getColorRgba(String styleClass) {
        Color color = getColorForFileType(styleClass);
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return r + ", " + g + ", " + b;
    }

    private Node createMediaPreview(FileMessageData fileData) {
        String mimeType = fileData.getMimeType();

        if (mimeType == null)
            return null;

        if (mimeType.startsWith("image/")) {
            return createImagePreview(fileData);
        } else if (mimeType.startsWith("audio/")) {
            return createAudioPreview(fileData);
        } else if (mimeType.startsWith("video/")) {
            return createVideoPreview(fileData);
        }

        return null;
    }

    private Node createImagePreview(FileMessageData fileData) {
        VBox imageContainer = new VBox(3);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setMaxWidth(340);
        imageContainer.setPadding(new Insets(0));

        // Create the image view with modern styling - clean, minimal border with
        // rounded corners
        ImageView imageView = new ImageView();
        imageView.setFitWidth(340);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Create a clip for rounded corners
        Rectangle clip = new Rectangle(340, 240);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);

        // Bind the clip size to the actual image size
        imageView.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
        });

        // Create loading/status indicator (smaller)
        Label loadingLabel = new Label("Loading image...");
        loadingLabel.getStyleClass().add("file-loading-label");

        imageContainer.getChildren().addAll(imageView, loadingLabel);

        // Set cursor style to indicate it's clickable
        imageContainer.setStyle("-fx-cursor: hand;");

        // Automatically download and display the image thumbnail
        downloadAndDisplayImageThumbnail(fileData, imageView, loadingLabel);

        return imageContainer;
    }

    private void downloadAndDisplayImageThumbnail(FileMessageData fileData, ImageView imageView, Label loadingLabel) {
        new Thread(() -> {
            try {
                // Download the image file
                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);
                CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                        fileData.getOriginalName());
                File imageFile = downloadFuture.get();

                // Display the image thumbnail
                Platform.runLater(() -> {
                    try {
                        Image image = new Image(imageFile.toURI().toString());
                        imageView.setImage(image);
                        loadingLabel.setVisible(false); // Hide the label completely

                        // No special handling needed for PNG transparency in thumbnails
                        // The message bubble background will show through transparent areas

                        // Add click handler to open full-size image
                        imageView.setOnMouseClicked(e -> openImageInFullSize(fileData));

                        // Modern subtle hover effect - just a slight brightness change
                        imageView.setOnMouseEntered(e -> {
                            imageView.setOpacity(0.9);
                            imageView.setEffect(new javafx.scene.effect.ColorAdjust(0, 0, 0.1, 0));
                        });
                        imageView.setOnMouseExited(e -> {
                            imageView.setOpacity(1.0);
                            imageView.setEffect(null);
                        });

                    } catch (Exception e) {
                        loadingLabel.setText("Error loading image");
                        loadingLabel.getStyleClass().clear();
                        loadingLabel.getStyleClass().add("file-error-label");
                        System.err.println("Error loading image: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingLabel.setText("Error downloading image");
                    loadingLabel.getStyleClass().clear();
                    loadingLabel.getStyleClass().add("file-error-label");
                    System.err.println("Error downloading image: " + e.getMessage());
                });
            }
        }).start();
    }

    private void openImageInFullSize(FileMessageData fileData) {
        new Thread(() -> {
            try {
                // Download the image file if not already cached
                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);
                CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                        fileData.getOriginalName());
                File imageFile = downloadFuture.get();

                Platform.runLater(() -> {
                    try {
                        createModernImageViewer(imageFile, fileData);
                    } catch (Exception e) {
                        showAlert("Error", "Failed to open image: " + e.getMessage());
                        System.err.println("Error opening image: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to download image: " + e.getMessage());
                    System.err.println("Error downloading image for full view: " + e.getMessage());
                });
            }
        }).start();
    }

    private void createModernImageViewer(File imageFile, FileMessageData fileData) {
        // Create stage - start with chat window size
        Stage imageStage = new Stage();
        imageStage.setTitle(fileData.getOriginalName());
        imageStage.initModality(Modality.APPLICATION_MODAL);

        // Get chat window dimensions (adjust these to your actual chat window size)
        double chatWindowWidth = 800;
        double chatWindowHeight = 650; // Slightly taller to accommodate toolbar
        double toolbarHeight = 60; // Height of the toolbar

        // Load image
        Image image = new Image(imageFile.toURI().toString());

        // Create zoomable and pannable image view
        ZoomableImageView zoomableImageView = new ZoomableImageView(image, fileData);
        
        // Set preferred height to exclude toolbar space
        zoomableImageView.setPrefHeight(chatWindowHeight - toolbarHeight);

        // Create toolbar with download and open buttons (fixed at bottom)
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(12));
        toolbar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 1 0 0 0;");
        toolbar.setPrefHeight(60);
        toolbar.setMinHeight(60);
        toolbar.setMaxHeight(60);

        // Download button
        Button downloadButton = new Button("Download");
        downloadButton.getStyleClass().add("download-button");
        downloadButton.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-color: #3498db; -fx-background-radius: 6; -fx-cursor: hand;");
        downloadButton.setOnMouseEntered(e -> downloadButton.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-color: #2980b9; -fx-background-radius: 6; -fx-cursor: hand;"));
        downloadButton.setOnMouseExited(e -> downloadButton.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-color: #3498db; -fx-background-radius: 6; -fx-cursor: hand;"));
        downloadButton.setOnAction(e -> {
            // Create a copy of the file in downloads folder
            try {
                File downloadsDir = new File("downloads");
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File destFile = new File(downloadsDir, fileData.getOriginalName());
                Files.copy(imageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert("Download Complete", "Image saved to: " + destFile.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("Download Error", "Failed to save image: " + ex.getMessage());
            }
        });

        // Open button
        Button openButton = new Button("Open");
        openButton.getStyleClass().add("open-button");
        openButton.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-color: #27ae60; -fx-background-radius: 6; -fx-cursor: hand;");
        openButton.setOnMouseEntered(e -> openButton.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-color: #229954; -fx-background-radius: 6; -fx-cursor: hand;"));
        openButton.setOnMouseExited(e -> openButton.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-color: #27ae60; -fx-background-radius: 6; -fx-cursor: hand;"));
        openButton.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(imageFile);
            } catch (Exception ex) {
                showAlert("Open Error", "Failed to open image: " + ex.getMessage());
            }
        });

        toolbar.getChildren().addAll(downloadButton, openButton);

        // Create main container with image viewer and toolbar
        VBox mainContainer = new VBox();
        mainContainer.getChildren().addAll(zoomableImageView, toolbar);
        VBox.setVgrow(zoomableImageView, Priority.ALWAYS);

        // Create scene with chat window size
        Scene scene = new Scene(mainContainer, chatWindowWidth, chatWindowHeight);

        // Add keyboard shortcuts for zoom
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                switch (e.getCode()) {
                    case PLUS:
                    case EQUALS: // For + without shift
                        zoomableImageView.zoomIn();
                        e.consume();
                        break;
                    case MINUS:
                        zoomableImageView.zoomOut();
                        e.consume();
                        break;
                    case DIGIT0:
                        zoomableImageView.resetZoom();
                        e.consume();
                        break;
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                imageStage.close();
            }
        });

        imageStage.setScene(scene);
        imageStage.centerOnScreen();
        imageStage.show();

        // Focus on scene to enable keyboard shortcuts
        scene.getRoot().requestFocus();
    }

    // Custom zoomable and pannable image view
    private static class ZoomableImageView extends StackPane {
        private final ImageView imageView;
        private final Group imageGroup;
        private double zoomFactor = 1.0;
        private final double MIN_ZOOM = 0.1;
        private final double MAX_ZOOM = 10.0;
        private final double ZOOM_STEP = 0.1;

        // For dragging
        private double lastMouseX, lastMouseY;
        private double translateX = 0, translateY = 0;

        // Original image size
        private double originalImageWidth, originalImageHeight;

        public ZoomableImageView(Image image, FileMessageData fileData) {
            this.imageView = new ImageView(image);
            this.imageGroup = new Group();

            originalImageWidth = image.getWidth();
            originalImageHeight = image.getHeight();

            setupImageView();
            setupBackground(fileData);
            setupMouseHandlers();

            imageGroup.getChildren().add(imageView);
            getChildren().add(imageGroup);

            // Listen for size changes to recenter image
            widthProperty().addListener((obs, oldVal, newVal) -> updateImageDisplay());
            heightProperty().addListener((obs, oldVal, newVal) -> updateImageDisplay());

            // Initial setup
            Platform.runLater(this::updateImageDisplay);
        }

        private void setupImageView() {
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
        }

        private void setupBackground(FileMessageData fileData) {
            // Set background for transparent PNGs
            boolean isPng = fileData.getMimeType() != null &&
                    (fileData.getMimeType().equals("image/png") ||
                            fileData.getOriginalName().toLowerCase().endsWith(".png"));

            if (isPng) {
                setStyle(
                        "-fx-background-color: #f0f0f0;" +
                                "-fx-background-image: " +
                                "linear-gradient(45deg, #e0e0e0 25%, transparent 25%), " +
                                "linear-gradient(-45deg, #e0e0e0 25%, transparent 25%), " +
                                "linear-gradient(45deg, transparent 75%, #e0e0e0 75%), " +
                                "linear-gradient(-45deg, transparent 75%, #e0e0e0 75%);" +
                                "-fx-background-size: 20px 20px;" +
                                "-fx-background-position: 0 0, 0 10px, 10px -10px, -10px 0px;");
            } else {
                setStyle("-fx-background-color: #2b2b2b;");
            }
        }

        private void setupMouseHandlers() {
            // Mouse wheel zoom
            setOnScroll(e -> {
                if (e.isControlDown()) {
                    e.consume();

                    // Get mouse position relative to image for zoom center
                    double mouseX = e.getX();
                    double mouseY = e.getY();

                    if (e.getDeltaY() > 0) {
                        zoomAt(mouseX, mouseY, ZOOM_STEP);
                    } else {
                        zoomAt(mouseX, mouseY, -ZOOM_STEP);
                    }
                }
            });

            // Mouse drag for panning
            setOnMousePressed(e -> {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                setCursor(Cursor.CLOSED_HAND);
                e.consume();
            });

            setOnMouseDragged(e -> {
                double deltaX = e.getX() - lastMouseX;
                double deltaY = e.getY() - lastMouseY;

                translateX += deltaX;
                translateY += deltaY;

                updateImagePosition();

                lastMouseX = e.getX();
                lastMouseY = e.getY();
                e.consume();
            });

            setOnMouseReleased(e -> {
                setCursor(Cursor.DEFAULT);
            });
        }

        private void updateImageDisplay() {
            if (getWidth() <= 0 || getHeight() <= 0)
                return;

            double containerWidth = getWidth();
            double containerHeight = getHeight();

            // Calculate the scale to fit image in container while preserving aspect ratio
            double scaleX = containerWidth / originalImageWidth;
            double scaleY = containerHeight / originalImageHeight;
            double fitScale = Math.min(scaleX, scaleY);

            // Always use fit scale as the base scale (this makes image fit to window initially)
            double baseScale = fitScale;

            // Apply zoom on top of base scale
            double finalScale = baseScale * zoomFactor;

            imageView.setFitWidth(originalImageWidth * finalScale);
            imageView.setFitHeight(originalImageHeight * finalScale);

            // Reset translation when zoom changes to recenter
            if (zoomFactor == 1.0) {
                translateX = 0;
                translateY = 0;
            }

            updateImagePosition();
        }

        private void updateImagePosition() {
            double containerWidth = getWidth();
            double containerHeight = getHeight();
            double imageWidth = imageView.getFitWidth();
            double imageHeight = imageView.getFitHeight();

            // Calculate center position (this centers the image in the container)
            double centerX = (containerWidth - imageWidth) / 2;
            double centerY = (containerHeight - imageHeight) / 2;

            // Apply centering plus any translation from dragging
            double finalX = centerX + translateX;
            double finalY = centerY + translateY;

            // Constrain translation to prevent image from going too far off screen
            if (imageWidth > containerWidth) {
                double maxTranslateX = (imageWidth - containerWidth) / 2 + 50; // 50px margin
                double minTranslateX = -(imageWidth - containerWidth) / 2 - 50;
                translateX = Math.max(minTranslateX, Math.min(maxTranslateX, translateX));
                finalX = centerX + translateX;
            } else {
                // If image is smaller than container, don't allow panning
                translateX = 0;
                finalX = centerX;
            }

            if (imageHeight > containerHeight) {
                double maxTranslateY = (imageHeight - containerHeight) / 2 + 50; // 50px margin
                double minTranslateY = -(imageHeight - containerHeight) / 2 - 50;
                translateY = Math.max(minTranslateY, Math.min(maxTranslateY, translateY));
                finalY = centerY + translateY;
            } else {
                // If image is smaller than container, don't allow panning
                translateY = 0;
                finalY = centerY;
            }

            // Position the image group at the calculated position
            imageGroup.setLayoutX(finalX);
            imageGroup.setLayoutY(finalY);
        }

        private void zoomAt(double mouseX, double mouseY, double zoomDelta) {
            double newZoom = zoomFactor + zoomDelta;
            newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

            if (newZoom != zoomFactor) {
                // Calculate zoom center relative to current image position
                double imageX = imageGroup.getTranslateX();
                double imageY = imageGroup.getTranslateY();

                // Adjust translation to zoom towards mouse position
                double zoomRatio = newZoom / zoomFactor;
                translateX = (translateX + mouseX - getWidth() / 2) * zoomRatio - (mouseX - getWidth() / 2);
                translateY = (translateY + mouseY - getHeight() / 2) * zoomRatio - (mouseY - getHeight() / 2);

                zoomFactor = newZoom;
                updateImageDisplay();
            }
        }

        public void zoomIn() {
            zoomAt(getWidth() / 2, getHeight() / 2, ZOOM_STEP);
        }

        public void zoomOut() {
            zoomAt(getWidth() / 2, getHeight() / 2, -ZOOM_STEP);
        }

        public void resetZoom() {
            zoomFactor = 1.0;
            translateX = 0;
            translateY = 0;
            updateImageDisplay();
        }
    }

    // Audio preview design starts
    private final String PLAY_ICON_PATH = "/com/bakbak/javafx_proj_1_2/icons/play.png"; // Add your play button PNG path
                                                                                        // here
    private final String PAUSE_ICON_PATH = "/com/bakbak/javafx_proj_1_2/icons/pause.png"; // Add your pause button PNG
                                                                                          // path here

    // Keep track of currently playing media player to pause it when another one
    // starts
    private MediaPlayer currentlyPlayingMediaPlayer;

    private Node createAudioPreview(FileMessageData fileData) {
        HBox audioContainer = new HBox(12);
        audioContainer.setAlignment(Pos.CENTER_LEFT);
        audioContainer.getStyleClass().add("audio-preview");
        audioContainer.setPadding(new Insets(12));
        audioContainer.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e9ecef;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);" +
                        "-fx-text-fill: black !important;");
        audioContainer.setMaxWidth(280);
        audioContainer.setPrefWidth(280);
        audioContainer.setMaxHeight(60);
        audioContainer.setPrefHeight(60);

        // Play/Pause button with custom PNG
        Button playPauseBtn = createPlayPauseButton();

        // Progress container
        VBox progressContainer = new VBox(4);
        progressContainer.setAlignment(Pos.CENTER);

        // Create a StackPane to hold the progress bar and the drag handle
        StackPane progressStack = new StackPane();
        progressStack.setPrefWidth(180);
        progressStack.setPrefHeight(14); // Make it a bit taller to accommodate the drag handle

        // Progress bar with custom styling
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180);
        progressBar.setPrefHeight(6);
        // Make sure JavaFX applies our CSS properly
        progressBar.setStyle(""); // Clear any default inline styles
        progressBar.getStyleClass().clear(); // Clear default style classes
        progressBar.getStyleClass().add("progress-bar"); // Re-add the base class
        progressBar.getStyleClass().add("custom-progress-bar");
        progressBar.getStyleClass().add("progress-bar-normal");

        // Create the drag handle (circle)
        Circle dragHandle = new Circle(5);
        dragHandle.setFill(Color.WHITE);
        dragHandle.setStroke(Color.valueOf("#3498db"));
        dragHandle.setStrokeWidth(1.5);
        dragHandle.setCursor(Cursor.HAND);
        dragHandle.setVisible(false); // Initially hidden, will show on hover
        dragHandle.getStyleClass().add("progress-drag-handle");
        // Add subtle shadow effect
        dragHandle.setEffect(new DropShadow(4, 0, 1, Color.rgb(0, 0, 0, 0.2)));

        // Position the drag handle at the progress point (initially at 0)
        StackPane.setAlignment(dragHandle, Pos.CENTER_LEFT);
        StackPane.setMargin(dragHandle, new Insets(0, 0, 0, 0));

        // Add both to the stack
        progressStack.getChildren().addAll(progressBar, dragHandle);

        // Time labels
        HBox timeBox = new HBox();
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setSpacing(8);

        Label currentTime = new Label("0:00");
        Label separator = new Label("/");
        Label totalTime = new Label("0:00");

        currentTime.getStyleClass().add("audio-time-label");
        separator.getStyleClass().add("audio-time-separator");
        totalTime.getStyleClass().add("audio-time-label");

        timeBox.getChildren().addAll(currentTime, separator, totalTime);
        progressContainer.getChildren().addAll(progressStack, timeBox);

        audioContainer.getChildren().addAll(playPauseBtn, progressContainer);

        // Setup audio player with drag animation
        setupAudioPlayerWithDragAnimation(fileData, playPauseBtn, progressStack, currentTime, totalTime);

        return audioContainer;
    }

    private Button createPlayPauseButton() {
        Button playPauseBtn = new Button();
        playPauseBtn.getStyleClass().add("audio-control-btn");
        playPauseBtn.setStyle(
                "-fx-background-color: #3498db;" +
                        "-fx-background-radius: 50;" +
                        "-fx-min-width: 32;" +
                        "-fx-min-height: 32;" +
                        "-fx-max-width: 32;" +
                        "-fx-max-height: 32;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 0;");

        // Set play icon initially
        setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);

        // Hover effect
        playPauseBtn.setOnMouseEntered(
                e -> playPauseBtn.setStyle(playPauseBtn.getStyle() + "-fx-background-color: #2980b9;"));
        playPauseBtn.setOnMouseExited(e -> playPauseBtn.setStyle(
                playPauseBtn.getStyle().replace("-fx-background-color: #2980b9;", "-fx-background-color: #3498db;")));

        return playPauseBtn;
    }

    private void setButtonIcon(Button button, String iconPath, int size) {
        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                Image icon = new Image(iconPath);
                ImageView imageView = new ImageView(icon);
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                button.setGraphic(imageView);
                button.setText("");
            } catch (Exception e) {
                // Fallback to text if image fails
                button.setGraphic(null);
                button.setText(iconPath.contains("play") ? "▶" : "⏸");
                button.getStyleClass().add("audio-control-fallback-text");
            }
        } else {
            // Fallback when no path is set
            button.setGraphic(null);
            button.setText("▶");
            button.getStyleClass().add("audio-control-fallback-text");
        }
    }

    private void setupAudioPlayerWithDragAnimation(FileMessageData fileData, Button playPauseBtn,
            StackPane progressStack, Label currentTime, Label totalTime) {

        final MediaPlayer[] mediaPlayer = { null };
        final boolean[] isPlaying = { false };
        final boolean[] isDragging = { false };
        final File[] audioFile = { null };
        final Timeline[] progressTimeline = { null };

        // Get the progress bar from the stack
        ProgressBar progressBar = null;
        for (Node node : progressStack.getChildren()) {
            if (node instanceof ProgressBar) {
                progressBar = (ProgressBar) node;
                break;
            }
        }

        // Play/Pause button action
        playPauseBtn.setOnAction(e -> {
            if (audioFile[0] == null) {
                downloadAndPrepareAudio(fileData, playPauseBtn, progressStack, currentTime, totalTime,
                        mediaPlayer, isPlaying, audioFile, isDragging, progressTimeline);
            } else {
                togglePlayPause(mediaPlayer[0], playPauseBtn, isPlaying, progressTimeline[0]);
            }
        });

        // Get reference to the drag handle
        Circle dragHandle = null;
        for (Node node : progressStack.getChildren()) {
            if (node instanceof Circle) {
                dragHandle = (Circle) node;
                break;
            }
        }

        // Enhanced drag functionality with live animation
        setupProgressBarDragAnimation(progressStack, progressBar, dragHandle, mediaPlayer, currentTime, totalTime,
                isDragging, progressTimeline);
    }

    private void setupProgressBarDragAnimation(StackPane progressStack, ProgressBar progressBar, Circle dragHandle,
            MediaPlayer[] mediaPlayer, Label currentTime, Label totalTime, boolean[] isDragging,
            Timeline[] progressTimeline) {

        // Update drag handle position when progress changes
        progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (dragHandle != null) {
                double width = progressStack.getWidth() - dragHandle.getRadius() * 2;
                double newX = width * newVal.doubleValue();
                StackPane.setMargin(dragHandle, new Insets(0, 0, 0, newX));
            }
        });

        // Show drag handle on hover
        progressStack.setOnMouseEntered(event -> {
            if (dragHandle != null && mediaPlayer[0] != null) {
                dragHandle.setVisible(true);
            }
        });

        progressStack.setOnMouseExited(event -> {
            if (dragHandle != null && !isDragging[0]) {
                dragHandle.setVisible(false);
            }
        });

        // Setup both the progress bar and drag handle for interaction
        progressStack.setOnMousePressed(event -> {
            if (mediaPlayer[0] != null && mediaPlayer[0].getTotalDuration() != null) {
                isDragging[0] = true;
                // Pause the automatic progress updates
                if (progressTimeline[0] != null) {
                    progressTimeline[0].pause();
                }

                // Make the drag handle visible while dragging
                if (dragHandle != null) {
                    dragHandle.setVisible(true);
                }

                // Update position immediately
                double clickX = event.getX();
                double width = progressStack.getWidth() - (dragHandle != null ? dragHandle.getRadius() * 2 : 0);
                double progress = Math.max(0, Math.min(1, clickX / width));

                // Live update progress bar
                progressBar.setProgress(progress);

                // Update drag handle position
                if (dragHandle != null) {
                    double newX = width * progress;
                    StackPane.setMargin(dragHandle, new Insets(0, 0, 0, newX));
                }

                // Live update time label
                if (mediaPlayer[0].getTotalDuration() != null) {
                    Duration totalDuration = mediaPlayer[0].getTotalDuration();
                    Duration newTime = Duration.seconds(progress * totalDuration.toSeconds());
                    currentTime.setText(formatDuration(newTime));
                }

                // Smooth animation effect
                createProgressAnimation(progressBar, progress);
            }
        });

        progressStack.setOnMouseDragged(event -> {
            if (isDragging[0] && mediaPlayer[0] != null && mediaPlayer[0].getTotalDuration() != null) {
                double clickX = event.getX();
                double width = progressStack.getWidth() - (dragHandle != null ? dragHandle.getRadius() * 2 : 0);
                double progress = Math.max(0, Math.min(1, clickX / width));

                // Live update progress bar
                progressBar.setProgress(progress);

                // Update drag handle position
                if (dragHandle != null) {
                    double newX = width * progress;
                    StackPane.setMargin(dragHandle, new Insets(0, 0, 0, newX));
                }

                // Live update time label
                Duration totalDuration = mediaPlayer[0].getTotalDuration();
                Duration newTime = Duration.seconds(progress * totalDuration.toSeconds());
                currentTime.setText(formatDuration(newTime));

                // Smooth animation effect
                createProgressAnimation(progressBar, progress);
            }
        });

        progressStack.setOnMouseReleased(event -> {
            if (isDragging[0] && mediaPlayer[0] != null && mediaPlayer[0].getTotalDuration() != null) {
                double clickX = event.getX();
                double width = progressStack.getWidth() - (dragHandle != null ? dragHandle.getRadius() * 2 : 0);
                double progress = Math.max(0, Math.min(1, clickX / width));

                // Seek to new position
                Duration totalDuration = mediaPlayer[0].getTotalDuration();
                Duration seekTime = Duration.seconds(progress * totalDuration.toSeconds());
                mediaPlayer[0].seek(seekTime);

                isDragging[0] = false;

                // Hide drag handle if mouse is not over the progress bar
                if (dragHandle != null && !progressStack.isHover()) {
                    dragHandle.setVisible(false);
                }
            }
        });

        // Handle click (not drag)
        progressStack.setOnMouseClicked(event -> {
            if (!isDragging[0] && mediaPlayer[0] != null && mediaPlayer[0].getTotalDuration() != null) {
                double clickX = event.getX();
                double width = progressStack.getWidth() - (dragHandle != null ? dragHandle.getRadius() * 2 : 0);
                double progress = Math.max(0, Math.min(1, clickX / width));

                Duration totalDuration = mediaPlayer[0].getTotalDuration();
                Duration seekTime = Duration.seconds(progress * totalDuration.toSeconds());
                mediaPlayer[0].seek(seekTime);
            }
        });
    }

    private void createProgressAnimation(ProgressBar progressBar, double targetProgress) {
        // Create a subtle pulse effect during drag by toggling classes
        Timeline pulseAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    // Remove any inline styling first
                    progressBar.setStyle("");
                    progressBar.getStyleClass().remove("progress-bar-normal");
                    progressBar.getStyleClass().add("progress-bar-pulse");
                }),
                new KeyFrame(Duration.millis(200), e -> {
                    // Remove any inline styling first
                    progressBar.setStyle("");
                    progressBar.getStyleClass().remove("progress-bar-pulse");
                    progressBar.getStyleClass().add("progress-bar-normal");
                }));
        pulseAnimation.setCycleCount(2);
        pulseAnimation.play();
    }

    private void downloadAndPrepareAudio(FileMessageData fileData, Button playPauseBtn,
            StackPane progressStack, Label currentTime, Label totalTime,
            MediaPlayer[] mediaPlayer, boolean[] isPlaying, File[] audioFile,
            boolean[] isDragging, Timeline[] progressTimeline) {

        // Get the progress bar from the stack
        ProgressBar progressBar = null;
        for (Node node : progressStack.getChildren()) {
            if (node instanceof ProgressBar) {
                progressBar = (ProgressBar) node;
                break;
            }
        }

        // Create a final copy of progressBar for use in lambda expressions
        final ProgressBar finalProgressBar = progressBar;

        // Loading state
        playPauseBtn.setDisable(true);
        setButtonIcon(playPauseBtn, "", 0); // This will show loading text
        playPauseBtn.setText("⏳");

        new Thread(() -> {
            try {
                // Download the audio file
                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);
                CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                        fileData.getOriginalName());
                audioFile[0] = downloadFuture.get();

                Platform.runLater(() -> {
                    try {
                        // Create MediaPlayer
                        String audioPath = audioFile[0].toURI().toString();
                        Media media = new Media(audioPath);
                        mediaPlayer[0] = new MediaPlayer(media);
                        
                        // Ensure audio plays only once (no looping)
                        mediaPlayer[0].setCycleCount(1);

                        // Setup media player listeners
                        setupMediaPlayerListeners(mediaPlayer[0], playPauseBtn, finalProgressBar,
                                currentTime, totalTime, isPlaying, isDragging, progressTimeline);

                        // Pause any currently playing audio before starting a new one
                        if (currentlyPlayingMediaPlayer != null && currentlyPlayingMediaPlayer != mediaPlayer[0]) {
                            currentlyPlayingMediaPlayer.pause();
                        }

                        // Update the currently playing media player
                        currentlyPlayingMediaPlayer = mediaPlayer[0];

                        // Start playing
                        mediaPlayer[0].play();
                        isPlaying[0] = true;
                        setButtonIcon(playPauseBtn, PAUSE_ICON_PATH, 14);
                        playPauseBtn.setDisable(false);

                    } catch (Exception ex) {
                        showAlert("Playback Error", "Failed to play audio: " + ex.getMessage());
                        setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);
                        playPauseBtn.setDisable(false);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showAlert("Download Error", "Failed to download audio: " + ex.getMessage());
                    setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);
                    playPauseBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void setupMediaPlayerListeners(MediaPlayer mediaPlayer, Button playPauseBtn,
            ProgressBar progressBar, Label currentTime, Label totalTime,
            boolean[] isPlaying, boolean[] isDragging, Timeline[] progressTimeline) {

        mediaPlayer.setOnReady(() -> {
            Duration duration = mediaPlayer.getTotalDuration();
            totalTime.setText(formatDuration(duration));
        });

        // Use the built-in currentTimeProperty listener instead of Timeline
        // Find the drag handle in the progressBar's parent
        Circle dragHandle = null;
        if (progressBar.getParent() instanceof StackPane) {
            StackPane progressStack = (StackPane) progressBar.getParent();
            for (Node node : progressStack.getChildren()) {
                if (node instanceof Circle) {
                    dragHandle = (Circle) node;
                    break;
                }
            }
        }

        final Circle finalDragHandle = dragHandle;

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isDragging[0]) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && total.toSeconds() > 0) {
                    double progress = newTime.toSeconds() / total.toSeconds();
                    progressBar.setProgress(progress);

                    // Apply pulsing style when playing - ensure style is set properly
                    if (isPlaying[0]) {
                        // First clear any inline style that could be overriding our CSS
                        progressBar.setStyle("");

                        // Then update style classes
                        if (!progressBar.getStyleClass().contains("custom-progress-bar")) {
                            progressBar.getStyleClass().add("custom-progress-bar");
                        }

                        progressBar.getStyleClass().remove("progress-bar-normal");
                        if (!progressBar.getStyleClass().contains("progress-bar-pulse")) {
                            progressBar.getStyleClass().add("progress-bar-pulse");
                        }
                    }
                }
                currentTime.setText(formatDuration(newTime));
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            isPlaying[0] = false;
            setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);
            progressBar.setProgress(0);
            currentTime.setText("0:00");
            
            // Stop the media player completely instead of seeking to zero
            mediaPlayer.stop();

            // Clear the currently playing reference if this is the one that was playing
            if (currentlyPlayingMediaPlayer == mediaPlayer) {
                currentlyPlayingMediaPlayer = null;
            }

            // Reset to normal style when ended
            progressBar.getStyleClass().remove("progress-bar-pulse");
            progressBar.getStyleClass().add("progress-bar-normal");
        });

        mediaPlayer.setOnError(() -> {
            MediaException error = mediaPlayer.getError();
            System.err.println("MediaPlayer error: " + error.getMessage());
            Platform.runLater(() -> {
                showAlert("Media Error", "Cannot play audio: " + error.getMessage());
                setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);
                playPauseBtn.setDisable(false);
            });
        });
    }

    private void togglePlayPause(MediaPlayer mediaPlayer, Button playPauseBtn, boolean[] isPlaying,
            Timeline progressTimeline) {
        // Get the parent of playPauseBtn to find progressBar
        Parent parent = playPauseBtn.getParent();
        while (parent != null && !(parent instanceof VBox)) {
            if (parent.getParent() == null)
                break;
            parent = parent.getParent();
        }

        ProgressBar progressBar = null;
        if (parent != null && parent instanceof VBox) {
            // Find the progress bar in the container
            for (Node node : ((VBox) parent).getChildren()) {
                if (node instanceof HBox) {
                    for (Node innerNode : ((HBox) node).getChildren()) {
                        if (innerNode instanceof ProgressBar) {
                            progressBar = (ProgressBar) innerNode;
                            break;
                        }
                    }
                }
            }
        }

        if (isPlaying[0]) {
            mediaPlayer.pause();
            setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);
            isPlaying[0] = false;

            // Clear the currently playing reference if this is the one that was playing
            if (currentlyPlayingMediaPlayer == mediaPlayer) {
                currentlyPlayingMediaPlayer = null;
            }

            // Change to normal style when paused
            if (progressBar != null) {
                progressBar.getStyleClass().remove("progress-bar-pulse");
                progressBar.getStyleClass().add("progress-bar-normal");
            }
        } else {
            // Pause any currently playing audio before starting a new one
            if (currentlyPlayingMediaPlayer != null && currentlyPlayingMediaPlayer != mediaPlayer) {
                currentlyPlayingMediaPlayer.pause();
            }

            // Update the currently playing media player
            currentlyPlayingMediaPlayer = mediaPlayer;

            mediaPlayer.play();
            setButtonIcon(playPauseBtn, PAUSE_ICON_PATH, 14);
            isPlaying[0] = true;

            // Change to pulse style when playing
            if (progressBar != null) {
                progressBar.getStyleClass().remove("progress-bar-normal");
                progressBar.getStyleClass().add("progress-bar-pulse");
            }
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null)
            return "0:00";

        int minutes = (int) duration.toMinutes();
        int seconds = (int) (duration.toSeconds() % 60);
        return String.format("%d:%02d", minutes, seconds);
    }
    // Audio preview design ends

    private Node createVideoPreview(FileMessageData fileData) {
        // Return null to display video files like regular files without media preview
        // This removes the red-bordered media preview area and shows only file info +
        // download/open buttons
        return null;
    }

    private void downloadAndPlayVideo(FileMessageData fileData, Button playButton, Button pauseButton,
            VBox videoContainer) {
        new Thread(() -> {
            try {
                // Download the video file
                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);
                CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                        fileData.getOriginalName());
                File videoFile = downloadFuture.get();

                // Show success message
                Platform.runLater(() -> {
                    showAlert("Video Downloaded", "Video file '" + fileData.getOriginalName()
                            + "' has been downloaded.\nLocation: " + videoFile.getAbsolutePath());
                    playButton.setDisable(true);
                    pauseButton.setDisable(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error downloading video: " + e.getMessage());
                    showAlert("Download Error", "Failed to download video file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void pauseAudio(Button pauseButton, Button playButton) {
        // Simplified pause functionality - just show a message
        showAlert("Audio Playback",
                "Audio playback controls are simplified.\nUse the Download button to save the file and play it with your system's media player.");
    }

    private void pauseVideo(Button pauseButton, Button playButton) {
        // Simplified pause functionality - just show a message
        showAlert("Video Playback",
                "Video playback controls are simplified.\nUse the Download button to save the file and play it with your system's media player.");
    }

    private void downloadFile(FileMessageData fileData) {
        new Thread(() -> {
            try {
                // Check if file is already downloaded
                if (isFileDownloaded(fileData.getUuidName())) {
                    // File already downloaded, no popup needed
                    return;
                }

                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);

                // Check if it's a video file to show progress
                boolean isVideo = fileData.getMimeType() != null && fileData.getMimeType().startsWith("video/");
                ProgressCallback progressCallback = null;

                if (isVideo) {
                    // Create progress callback for video files
                    progressCallback = new ProgressCallback() {
                        @Override
                        public void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed,
                                long totalBytes) {
                            double progress = (double) bytesProcessed / totalBytes;
                            Platform.runLater(() -> {
                                // Use unified progress bar
                                showUnifiedProgress(progress,
                                        "Downloading video... " + String.format("%d%%", (int) (progress * 100)));
                            });
                        }

                        @Override
                        public void onTransferComplete() {
                            Platform.runLater(() -> {
                                hideUnifiedProgress();
                            });
                        }

                        @Override
                        public void onTransferError(String error) {
                            Platform.runLater(() -> {
                                hideUnifiedProgress();
                                showAlert("Download Error", "Failed to download video: " + error);
                            });
                        }
                    };
                }

                CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                        fileData.getOriginalName(), progressCallback);
                File downloadedFile = downloadFuture.get(); // Wait for completion

                // Cache the downloaded file
                cacheDownloadedFile(fileData.getUuidName(), downloadedFile);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideUnifiedProgress();
                    showAlert("Download Error", "Failed to download file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void openFile(FileMessageData fileData) {
        new Thread(() -> {
            try {
                File fileToOpen;

                // Check if file is already downloaded
                if (isFileDownloaded(fileData.getUuidName())) {
                    fileToOpen = getDownloadedFile(fileData.getUuidName());
                    System.out.println("Using cached file: " + fileToOpen.getAbsolutePath());
                } else {
                    // File not cached, need to download
                    FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(),
                            FileChunkReceiver.CHUNK_PORT);

                    // Check if it's a video file to show progress
                    boolean isVideo = fileData.getMimeType() != null && fileData.getMimeType().startsWith("video/");
                    ProgressCallback progressCallback = null;

                    if (isVideo) {
                        // Create progress callback for video files
                        progressCallback = new ProgressCallback() {
                            @Override
                            public void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed,
                                    long totalBytes) {
                                double progress = (double) bytesProcessed / totalBytes;
                                Platform.runLater(() -> {
                                    // Use unified progress bar
                                    showUnifiedProgress(progress,
                                            "Preparing video... " + String.format("%d%%", (int) (progress * 100)));
                                });
                            }

                            @Override
                            public void onTransferComplete() {
                                Platform.runLater(() -> {
                                    hideUnifiedProgress();
                                });
                            }

                            @Override
                            public void onTransferError(String error) {
                                Platform.runLater(() -> {
                                    hideUnifiedProgress();
                                    showAlert("Download Error", "Failed to download video for opening: " + error);
                                });
                            }
                        };
                    }

                    CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                            fileData.getOriginalName(), progressCallback);
                    fileToOpen = downloadFuture.get(); // Wait for completion

                    // Cache the downloaded file for future use
                    cacheDownloadedFile(fileData.getUuidName(), fileToOpen);
                }

                // Open the file with system default application
                Platform.runLater(() -> {
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop desktop = Desktop.getDesktop();
                            if (desktop.isSupported(Desktop.Action.OPEN)) {
                                desktop.open(fileToOpen);
                            } else {
                                showAlert("Open Error", "Opening files is not supported on this system.");
                            }
                        } else {
                            showAlert("Open Error", "Desktop operations are not supported on this system.");
                        }
                    } catch (Exception e) {
                        showAlert("Open Error", "Failed to open file: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideUnifiedProgress();
                    showAlert("Download Error", "Failed to download file for opening: " + e.getMessage());
                });
            }
        }).start();
    }

    // Progress bar methods for video file transfers
    private void updateSendButtonProgress(double progress, String text) {
        if (sendButton != null) {
            sendButton.setText(text);
            sendButton.setDisable(true);

            // Create a simple progress bar effect by changing button style
            String progressStyle = String.format(
                    "-fx-background-color: linear-gradient(to right, #007bff %.1f%%, #e9ecef %.1f%%); " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;",
                    progress * 100, progress * 100);
            sendButton.setStyle(progressStyle);
        }
    }

    private void resetSendButton() {
        if (sendButton != null) {
            sendButton.setText("Send");
            sendButton.setDisable(false);
            sendButton.setStyle(
                    "-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        }
    }

    // Download progress methods
    private ProgressBar downloadProgressBar;
    private Label downloadProgressLabel;
    private Stage downloadProgressStage;

    private void showDownloadProgress(double progress, String text) {
        if (downloadProgressStage == null) {
            // Create progress dialog
            downloadProgressStage = new Stage();
            downloadProgressStage.setTitle("Download Progress");
            downloadProgressStage.initModality(Modality.NONE);
            downloadProgressStage.setResizable(false);

            VBox progressContainer = new VBox(10);
            progressContainer.setPadding(new Insets(20));
            progressContainer.setAlignment(Pos.CENTER);

            downloadProgressLabel = new Label(text);
            downloadProgressLabel.getStyleClass().add("download-progress-label");

            downloadProgressBar = new ProgressBar(progress);
            downloadProgressBar.setPrefWidth(300);
            downloadProgressBar.setStyle("-fx-accent: #007bff;");

            progressContainer.getChildren().addAll(downloadProgressLabel, downloadProgressBar);

            Scene scene = new Scene(progressContainer);
            downloadProgressStage.setScene(scene);
            downloadProgressStage.show();
        } else {
            // Update existing progress
            downloadProgressBar.setProgress(progress);
            downloadProgressLabel.setText(text);
        }
    }

    private void hideDownloadProgress() {
        if (downloadProgressStage != null) {
            downloadProgressStage.close();
            downloadProgressStage = null;
            downloadProgressBar = null;
            downloadProgressLabel = null;
        }
    }

    // Unified progress bar setup
    private void setupUnifiedProgressBar() {
        // Create unified progress bar container
        unifiedProgressContainer = new HBox(10);
        unifiedProgressContainer.setPadding(new Insets(5, 10, 5, 10));
        unifiedProgressContainer.setAlignment(Pos.CENTER_LEFT);
        unifiedProgressContainer
                .setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0;");
        unifiedProgressContainer.setVisible(false);

        // Create progress bar
        unifiedProgressBar = new ProgressBar(0);
        unifiedProgressBar.setPrefWidth(200);
        unifiedProgressBar.setStyle("-fx-accent: #007bff;");

        // Create progress label
        unifiedProgressLabel = new Label("");
        unifiedProgressLabel.getStyleClass().add("unified-progress-label");

        unifiedProgressContainer.getChildren().addAll(unifiedProgressBar, unifiedProgressLabel);

        // Add to chat header (assuming chatHeader is a VBox)
        if (chatHeader != null) {
            chatHeader.getChildren().add(unifiedProgressContainer);
        }
    }

    // Smart file caching methods
    private boolean isFileDownloaded(String fileID) {
        return downloadedFilesCache.containsKey(fileID) &&
                downloadedFilesCache.get(fileID).exists();
    }

    private File getDownloadedFile(String fileID) {
        return downloadedFilesCache.get(fileID);
    }

    private void cacheDownloadedFile(String fileID, File file) {
        downloadedFilesCache.put(fileID, file);
    }

    // Unified progress bar methods
    private void showUnifiedProgress(double progress, String text) {
        Platform.runLater(() -> {
            if (unifiedProgressContainer != null) {
                unifiedProgressBar.setProgress(progress);
                unifiedProgressLabel.setText(text);
                unifiedProgressContainer.setVisible(true);
            }
        });
    }

    private void hideUnifiedProgress() {
        Platform.runLater(() -> {
            if (unifiedProgressContainer != null) {
                unifiedProgressContainer.setVisible(false);
                unifiedProgressBar.setProgress(0);
                unifiedProgressLabel.setText("");
            }
        });
    }

    // File cache to avoid redundant downloads
    private Map<String, File> downloadedFilesCache = new HashMap<>();

    // Unified progress bar below title bar
    private ProgressBar unifiedProgressBar;
    private Label unifiedProgressLabel;
    private HBox unifiedProgressContainer;

    // Voice recording fields
    private TargetDataLine targetDataLine;
    private Thread recordingThread;
    private ByteArrayOutputStream audioOutputStream;
    private boolean isRecording = false;
    private Stage voiceRecorderStage;
    private long recordingStartTime;

    @FXML
    private void handleVoiceMessage() {
        if (selectedChat == null) {
            showAlert("Error", "Please select a chat to send a voice message.");
            return;
        }
        showVoiceRecorderDialog();
    }

    private void showVoiceRecorderDialog() {
        if (voiceRecorderStage != null && voiceRecorderStage.isShowing()) {
            voiceRecorderStage.toFront();
            return;
        }

        voiceRecorderStage = new Stage();
        voiceRecorderStage.initModality(Modality.APPLICATION_MODAL);
        voiceRecorderStage.setTitle("Voice Recorder");
        voiceRecorderStage.setResizable(false);

        // Create the main container
        VBox mainContainer = new VBox();
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setSpacing(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.getStyleClass().add("voice-recorder-dialog");

        // Title
        Label titleLabel = new Label("Voice Message");
        titleLabel.getStyleClass().add("voice-recorder-title");

        // Recording status container
        VBox statusContainer = new VBox();
        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.setSpacing(15);

        // Recording indicator (animated circle)
        Circle recordingIndicator = new Circle(8);
        recordingIndicator.getStyleClass().add("recording-indicator");
        recordingIndicator.setVisible(false);

        // Timer label
        Label timerLabel = new Label("00:00");
        timerLabel.getStyleClass().add("voice-recorder-timer");

        // Status text
        Label statusLabel = new Label("Tap to start recording");
        statusLabel.getStyleClass().add("voice-recorder-status");

        statusContainer.getChildren().addAll(recordingIndicator, timerLabel, statusLabel);

        // Main recording button
        Button recordButton = new Button();
        recordButton.setPrefSize(80, 80);
        recordButton.getStyleClass().add("voice-record-button");

        // Microphone icon for record button
        ImageView micIcon = new ImageView();
        try {
            Image micImage = new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/voice.png"));
            micIcon.setImage(micImage);
            micIcon.setFitWidth(35);
            micIcon.setFitHeight(35);
            micIcon.setPreserveRatio(true);
        } catch (Exception e) {
            // Fallback text if image not found
            recordButton.setText("🎤");
            recordButton.setStyle(recordButton.getStyle() + "-fx-font-size: 30px;");
        }
        recordButton.setGraphic(micIcon);

        // Control buttons container
        HBox controlsContainer = new HBox();
        controlsContainer.setAlignment(Pos.CENTER);
        controlsContainer.setSpacing(20);

        // Cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefSize(100, 40);
        cancelButton.getStyleClass().addAll("voice-control-button", "voice-cancel-button");

        // Send button
        Button sendButton = new Button("Send");
        sendButton.setPrefSize(100, 40);
        sendButton.getStyleClass().addAll("voice-control-button", "voice-send-button");
        sendButton.setDisable(true);

        controlsContainer.getChildren().addAll(cancelButton, sendButton);

        // Add all components to main container
        mainContainer.getChildren().addAll(titleLabel, statusContainer, recordButton, controlsContainer);

        // Create timeline for timer update
        Timeline timerTimeline = new Timeline();
        timerTimeline.setCycleCount(Timeline.INDEFINITE);

        // Create pulse animation for recording indicator
        Timeline pulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(recordingIndicator.scaleXProperty(), 1.0),
                new KeyValue(recordingIndicator.scaleYProperty(), 1.0),
                new KeyValue(recordingIndicator.opacityProperty(), 1.0)
            ),
            new KeyFrame(Duration.seconds(0.5), 
                new KeyValue(recordingIndicator.scaleXProperty(), 1.3),
                new KeyValue(recordingIndicator.scaleYProperty(), 1.3),
                new KeyValue(recordingIndicator.opacityProperty(), 0.7)
            ),
            new KeyFrame(Duration.seconds(1.0), 
                new KeyValue(recordingIndicator.scaleXProperty(), 1.0),
                new KeyValue(recordingIndicator.scaleYProperty(), 1.0),
                new KeyValue(recordingIndicator.opacityProperty(), 1.0)
            )
        );
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);

        // Button event handlers
        recordButton.setOnAction(e -> {
            if (!isRecording) {
                startRecording(recordButton, recordingIndicator, timerLabel, statusLabel, 
                             sendButton, timerTimeline, pulseTimeline);
            } else {
                stopRecording(recordButton, recordingIndicator, timerLabel, statusLabel, 
                            sendButton, timerTimeline, pulseTimeline);
            }
        });

        cancelButton.setOnAction(e -> {
            if (isRecording) {
                stopRecording(recordButton, recordingIndicator, timerLabel, statusLabel, 
                            sendButton, timerTimeline, pulseTimeline);
            }
            voiceRecorderStage.close();
        });

        sendButton.setOnAction(e -> {
            sendVoiceMessage();
            voiceRecorderStage.close();
        });

        // Add hover effects to buttons
        addButtonHoverEffect(cancelButton, "#95a5a6", "#7f8c8d");
        addButtonHoverEffect(sendButton, "#27ae60", "#219a52");
        addButtonHoverEffect(recordButton, "#3498db", "#2980b9");

        Scene scene = new Scene(mainContainer, 350, 400);
        scene.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
        
        voiceRecorderStage.setScene(scene);
        voiceRecorderStage.show();

        // Center the stage on the main window
        voiceRecorderStage.setX((voiceRecorderStage.getOwner().getX() + voiceRecorderStage.getOwner().getWidth() / 2) - 175);
        voiceRecorderStage.setY((voiceRecorderStage.getOwner().getY() + voiceRecorderStage.getOwner().getHeight() / 2) - 200);
    }

    private void addButtonHoverEffect(Button button, String normalColor, String hoverColor) {
        String originalStyle = button.getStyle();
        button.setOnMouseEntered(e -> 
            button.setStyle(originalStyle.replace(normalColor, hoverColor))
        );
        button.setOnMouseExited(e -> 
            button.setStyle(originalStyle)
        );
    }

    private void startRecording(Button recordButton, Circle indicator, Label timerLabel, 
                              Label statusLabel, Button sendButton, Timeline timerTimeline, Timeline pulseTimeline) {
        try {
            // Audio format configuration
            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,  // Sample rate
                16,        // Sample size in bits
                2,         // Channels (stereo)
                4,         // Frame size
                44100.0f,  // Frame rate
                false      // Big endian
            );

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                showAlert("Error", "Audio recording is not supported on this system.");
                return;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            audioOutputStream = new ByteArrayOutputStream();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // Update UI
            recordButton.getStyleClass().remove("voice-record-button");
            recordButton.getStyleClass().add("voice-record-button-recording");
            
            indicator.setVisible(true);
            statusLabel.setText("Recording... Tap to stop");
            pulseTimeline.play();

            // Start timer update
            timerTimeline.getKeyFrames().clear();
            timerTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(0.1), e -> updateRecordingTimer(timerLabel))
            );
            timerTimeline.play();

            // Start recording thread
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording && !Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            audioOutputStream.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert("Recording Error", "An error occurred while recording: " + e.getMessage());
                            stopRecording(recordButton, indicator, timerLabel, statusLabel, sendButton, timerTimeline, pulseTimeline);
                        });
                        break;
                    }
                }
            });
            recordingThread.setDaemon(true);
            recordingThread.start();

        } catch (Exception e) {
            showAlert("Recording Error", "Failed to start recording: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopRecording(Button recordButton, Circle indicator, Label timerLabel,
                             Label statusLabel, Button sendButton, Timeline timerTimeline, Timeline pulseTimeline) {
        isRecording = false;
        
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }

        // Stop animations
        timerTimeline.stop();
        pulseTimeline.stop();

        // Update UI
        recordButton.getStyleClass().remove("voice-record-button-recording");
        recordButton.getStyleClass().add("voice-record-button");
        
        indicator.setVisible(false);
        statusLabel.setText("Recording completed");
        sendButton.setDisable(false);
    }

    private void updateRecordingTimer(Label timerLabel) {
        if (isRecording) {
            long elapsedTime = System.currentTimeMillis() - recordingStartTime;
            long seconds = (elapsedTime / 1000) % 60;
            long minutes = (elapsedTime / (1000 * 60)) % 60;
            
            Platform.runLater(() -> {
                timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
            });
        }
    }

    private void sendVoiceMessage() {
        if (audioOutputStream == null || audioOutputStream.size() == 0) {
            showAlert("Error", "No audio recorded. Please record a voice message first.");
            return;
        }

        try {
            // Create temporary WAV file
            File tempFile = File.createTempFile("voice_message_", ".wav");
            tempFile.deleteOnExit();

            // Write audio data to WAV file
            byte[] audioData = audioOutputStream.toByteArray();
            AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, tempFile);
            
            // Send the file using existing file sharing mechanism
            Platform.runLater(() -> {
                sendFile(tempFile);
            });

        } catch (Exception e) {
            showAlert("Error", "Failed to send voice message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
