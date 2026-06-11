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
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
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
import javafx.scene.control.OverrunStyle;
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
    private VBox skeletonContainer;
    private PauseTransition skeletonTimeout;
    private Timeline shimmerTimeline;
    private PauseTransition scrollDebouncer;
    private Map<String, String> unicodeToEmojiFilenameMap = new HashMap<>();
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
    private BorderPane mainChatArea;
    @FXML
    private StackPane chatAreaStack;
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
    private final Map<String, List<Message>> pendingGroupMessages = new HashMap<>();
    private CompletableFuture<Message> pendingResponse;
    private Timeline lastSeenUpdateTimer;
    private String lastMessageSender = null; // Track last sender for message grouping
    private Set<String> blockedUsers = new HashSet<>();
    private Set<String> blockedByUsers = new HashSet<>();

    private boolean isBlockedByUser(String name) {
        if (name == null) return false;
        for (String u : blockedByUsers) {
            if (name.equalsIgnoreCase(u)) return true;
        }
        return false;
    }

    private boolean isUserBlockedByMe(String name) {
        if (name == null) return false;
        for (String u : blockedUsers) {
            if (name.equalsIgnoreCase(u)) return true;
        }
        return false;
    }
    private VBox activeOptionsPanel = null;
    private Region activeDismissOverlay = null;
    private VBox activeEmojiPanel = null;
    private Region activeEmojiDismissOverlay = null;
    private ListView<HBox> managementMemberListView;
    private ChatItem managementGroupItem;
    private final Map<String, String> unicodeToFilename = new HashMap<>();
    // Store the selection listener to enable/disable it
    private javafx.beans.value.ChangeListener<ChatItem> chatSelectionListener;
    private boolean isSelectingChat = false; // Prevent multiple rapid selections
    private long lastChatSelectTime = 0;

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
        initializeUnicodeToEmojiFilenameMap();
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
        private int marginLeft = 15; // Equal left/right margin to center grid in popup
        private Tooltip tooltip = new Tooltip();

        public EmojiCanvas() {
            this.setWidth(columns * (emojiSize + padding) + marginLeft * 2); // Grid + equal margins

            // Handle hover events to highlight emojis
            this.setOnMouseMoved(e -> {
                int col = (int) ((e.getX() - marginLeft) / (emojiSize + padding));
                int row = (int) (e.getY() / (emojiSize + padding));
                int index = row * columns + col;

                if (e.getX() >= marginLeft && emojis != null && index >= 0 && index < emojis.size() && col >= 0 && col < columns) {
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
                int col = (int) ((e.getX() - marginLeft) / (emojiSize + padding));
                int row = (int) (e.getY() / (emojiSize + padding));
                int index = row * columns + col;

                if (e.getX() >= marginLeft && emojis != null && index >= 0 && index < emojis.size() && col >= 0 && col < columns) {
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
                // Calculate position for this emoji (with left margin offset)
                int x = marginLeft + col * (emojiSize + padding);
                int y = row * (emojiSize + padding);

                // Draw background/highlight if hovered
                if (emoji == hoveredEmoji) {
                    if (com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled()) {
                        gc.setFill(javafx.scene.paint.Color.valueOf("#2b5278"));
                    } else {
                        gc.setFill(javafx.scene.paint.Color.valueOf("#f0f0f0"));
                    }
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
        darkModeToggle.setSelected(com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled());
        Platform.runLater(this::updateIconsForCurrentMode);

        // Disable message input initially
        messageInput.setDisable(true);
        sendButton.setDisable(true);
        fileButton.setDisable(true);

        // Setup hover effects for icon buttons
        setupIconButtonHoverEffects();

        // Setup chat screen containers with CSS classes for dark mode
    }

    private void setupIconButtonHoverEffects() {
        // Setup hover effects for settings button
        setupButtonHoverEffect(settingsButton, "group.png", null);

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

        boolean isDarkMode = com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled();
        String imageName = normalImage;
        if (isDarkMode) {
            String baseName = normalImage;
            if (normalImage.endsWith(".png")) {
                baseName = normalImage.substring(0, normalImage.length() - 4);
                if (baseName.endsWith("2")) {
                    baseName = baseName.substring(0, baseName.length() - 1);
                }
            }
            String path2 = "/com/bakbak/javafx_proj_1_2/icons/" + baseName + "2.png";
            if (getClass().getResource(path2) != null) {
                imageName = baseName + "2.png";
            }
        }

        Image normalImg = loadResourceImage("/com/bakbak/javafx_proj_1_2/icons/" + imageName);
        imageView.setImage(normalImg);
    }

    private void setupButtonHoverScaleOnly(Button button) {
        // Zoom/scale animation disabled
    }

    private void setupToggleButtonHoverEffects() {
        if (darkModeToggle == null || darkModeIcon == null)
            return;

        // Load sun and night images
        boolean isDarkMode = com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled();
        String sunName = isDarkMode ? "sun2.png" : "sun.png";
        String nightName = isDarkMode ? "night2.png" : "night.png";
        Image sunImg = loadResourceImage("/com/bakbak/javafx_proj_1_2/icons/" + sunName);
        Image nightImg = loadResourceImage("/com/bakbak/javafx_proj_1_2/icons/" + nightName);

        // Set initial state
        if (darkModeToggle.isSelected()) {
            darkModeIcon.setImage(nightImg);
        } else {
            darkModeIcon.setImage(sunImg);
        }

        // Update icon when toggle state changes
        darkModeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean currentDark = newValue;
            String sName = currentDark ? "sun2.png" : "sun.png";
            String nName = currentDark ? "night2.png" : "night.png";
            Image sImg = loadResourceImage("/com/bakbak/javafx_proj_1_2/icons/" + sName);
            Image nImg = loadResourceImage("/com/bakbak/javafx_proj_1_2/icons/" + nName);

            if (newValue) {
                // Dark mode activated, show night icon
                darkModeIcon.setImage(nImg);
            } else {
                // Light mode activated, show sun icon
                darkModeIcon.setImage(sImg);
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
                if (!scene.getRoot().getStyleClass().contains("dark-mode")) {
                    scene.getRoot().getStyleClass().add("dark-mode");
                }
                com.bakbak.javafx_proj_1_2.ChatApplication.setDarkModeEnabled(true);
            } else {
                // Disable dark mode
                scene.getRoot().getStyleClass().remove("dark-mode");
                com.bakbak.javafx_proj_1_2.ChatApplication.setDarkModeEnabled(false);
            }
            updateIconsForCurrentMode();
        }
    }

    private void updateIconsForCurrentMode() {
        boolean isDark = com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled();
        if (darkModeToggle != null && darkModeToggle.getScene() != null) {
            updateIconsColor(darkModeToggle.getScene().getRoot(), isDark);
        }
    }

    private void updateIconsColor(Node node, boolean isDarkMode) {
        if (node instanceof ImageView) {
            ImageView iv = (ImageView) node;
            if (iv.getImage() != null) {
                String url = iv.getImage().getUrl();
                if (url != null) {
                    // Extract filename from URL (e.g. "/icons/settings.png" -> "settings.png")
                    int lastSlash = url.lastIndexOf('/');
                    if (lastSlash != -1) {
                        String filename = url.substring(lastSlash + 1);
                        if (filename.endsWith(".png")) {
                            String baseName = filename.substring(0, filename.length() - 4); // Remove ".png"
                            
                            // Normalize baseName by removing trailing "2" if it exists
                            if (baseName.endsWith("2")) {
                                baseName = baseName.substring(0, baseName.length() - 1);
                            }
                            
                            String targetImageName = baseName + ".png";
                            boolean use2Version = false;
                            
                            if (isDarkMode) {
                                // Check if a "2" version exists in resource folder
                                String path2 = "/com/bakbak/javafx_proj_1_2/icons/" + baseName + "2.png";
                                if (getClass().getResource(path2) != null) {
                                    targetImageName = baseName + "2.png";
                                    use2Version = true;
                                }
                            }
                            
                            // Load and apply the target image if it is different
                            if (!filename.equals(targetImageName)) {
                                String resourcePath = "/com/bakbak/javafx_proj_1_2/icons/" + targetImageName;
                                try {
                                    Image newImg = loadResourceImage(resourcePath);
                                    iv.setImage(newImg);
                                } catch (Exception e) {
                                    System.err.println("Failed to load icon: " + resourcePath + " - " + e.getMessage());
                                }
                            }
                            
                            // For icons using the native dark mode "2" version, set effect to null.
                            // For other UI icons (including those falling back or not having a "2" version),
                            // apply the ColorAdjust brightness effect in dark mode.
                            if (use2Version) {
                                iv.setEffect(null);
                            } else {
                                // Only apply ColorAdjust to non-media UI icons (skip emoji/voice/file icons that are already colored)
                                boolean isUIIcon = baseName.equals("group")
                                                || baseName.equals("send")
                                                || baseName.equals("play")
                                                || baseName.equals("pause")
                                                || baseName.equals("symbols");
                                
                                if (isUIIcon) {
                                    if (isDarkMode) {
                                        javafx.scene.effect.ColorAdjust ca = new javafx.scene.effect.ColorAdjust();
                                        ca.setBrightness(0.9);
                                        iv.setEffect(ca);
                                    } else {
                                        iv.setEffect(null);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                updateIconsColor(child, isDarkMode);
            }
        }
    }

    private Image loadResourceImage(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                return new Image(url.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Failed to load image via URL: " + path + " - " + e.getMessage());
        }
        
        // Fallback to safe default file icon if resource is missing
        try {
            java.net.URL fallbackUrl = getClass().getResource("/com/bakbak/javafx_proj_1_2/icons/file_icon.png");
            if (fallbackUrl != null) {
                return new Image(fallbackUrl.toExternalForm());
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return new Image(getClass().getResourceAsStream("/com/bakbak/javafx_proj_1_2/icons/file_icon.png"));
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
                    } else {
                        if (!message.isSuccess() && message.getContent() != null) {
                            showAlert("Error", message.getContent());
                        }
                    }
                    break;
                case BLOCK_USER:
                    handleBlockedUsersList(message);
                    break;
                case BLOCKED_BY_USER:
                    handleBlockedByUsersMessage(message);
                    break;
                case UNBLOCKED_BY_USER:
                    handleUnblockedByUsersMessage(message);
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
                        } else if ("System".equals(sender)) {
                            displayMessage = processedContent;
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

                        // Process any pending messages for this group that arrived before group list was loaded
                        List<Message> pending = pendingGroupMessages.remove(groupId);
                        if (pending != null) {
                            for (Message pendingMsg : pending) {
                                handleIncomingGroupMessage(pendingMsg);
                            }
                        }
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
        // Format: REMOVED_FROM_GROUP:groupName|groupId|isDeleted
        String[] parts = response.substring("REMOVED_FROM_GROUP:".length()).split("\\|");
        if (parts.length >= 2) {
            String groupName = parts[0];
            String groupId = parts[1];
            boolean isDeleted = parts.length > 2 && "deleted".equals(parts[2]);

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
            if (isDeleted) {
                showAlert("Group Deleted", "The group '" + groupName + "' has been deleted by its creator.");
            } else {
                showAlert("Removed from Group", "You have been removed from the group: " + groupName);
            }
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
            groupItem.setLastMessage("👥 Group created", LocalDateTime.now());
            groupItem.setHasUnreadMessages(true);
            allChatItems.put(groupName, groupItem);

            // Request detailed group information
            requestDetailedGroupInfo(groupId);

            // Process any pending messages for this group that arrived before group list was loaded
            List<Message> pending = pendingGroupMessages.remove(groupId);
            if (pending != null) {
                for (Message pendingMsg : pending) {
                    handleIncomingGroupMessage(pendingMsg);
                }
            }

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

                // Live update the management window if open
                if (isManagementWindowOpen() && managementMemberListView != null && managementGroupItem != null && managementGroupItem.getGroupId().equals(groupId)) {
                    refreshManagementWindow(managementMemberListView, groupItem);
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
        if (isRecording) {
            cancelRecording();
        }
        selectedChat = chatItem;
        closeOptionsPanel();
        closeEmojiPanel();

        // Make chat header and input visible
        chatHeader.setVisible(true);

        if (chatItem.getType() == ChatItem.Type.USER && isUserBlockedByMe(chatItem.getName())) {
            HBox unblockBar = createUnblockBar(chatItem.getName());
            if (mainChatArea != null) {
                mainChatArea.setBottom(unblockBar);
            }
        } else if (chatItem.getType() == ChatItem.Type.USER && isBlockedByUser(chatItem.getName())) {
            HBox blockedBanner = createBlockedBanner(chatItem.getName());
            if (mainChatArea != null) {
                mainChatArea.setBottom(blockedBanner);
            }
        } else {
            if (mainChatArea != null) {
                mainChatArea.setBottom(messageInputArea);
            }
            messageInputArea.setVisible(true);
            messageInputArea.setManaged(true);
        }

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

        // Reset file progress indicators in UI
        fileIconStacks.clear();
        fileIconImageViews.clear();
        fileDownloadButtons.clear();
        fileOpenButtons.clear();
        activeSendProgressIndicators.clear();
        activeSendingMessageNodes.clear();

        showSkeletonLoading();

        lastChatSelectTime = System.currentTimeMillis();

        // Show messages immediately without animation
        messagesContainer.setOpacity(1.0);
        messagesContainer.setTranslateY(0.0);

        if (chatItem.getType() == ChatItem.Type.USER) {
            showPrivateChat(chatItem.getName());
        } else if (chatItem.getType() == ChatItem.Type.GROUP) {
            showGroupChat(chatItem.getGroupId());
        }

        // Re-add active sending messages for the selected chat
        for (Map.Entry<String, Message> entry : activeSendingMessages.entrySet()) {
            Message msg = entry.getValue();
            boolean matches = false;
            if (chatItem.getType() == ChatItem.Type.GROUP) {
                matches = chatItem.getGroupId() != null && chatItem.getGroupId().equals(msg.getGroupId());
            } else {
                matches = chatItem.getName() != null && chatItem.getName().equals(msg.getRecipient());
            }

            if (matches) {
                try {
                    addMessageToUI(msg, true);
                } catch (Exception e) {
                    System.err.println("Error re-adding active sending message to UI: " + e.getMessage());
                }
            }
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
        popupContent.getStyleClass().add("group-members-popup");
        popupContent.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
        popupContent.setSpacing(5);
        popupContent.setPadding(new Insets(10));
        popupContent.setPrefWidth(250);
        popupContent.setMaxHeight(300);
        // Title
        Label titleLabel = new Label("Group Members (" + groupItem.getMemberCount() + ")");
        titleLabel.getStyleClass().add("group-member-popup-title");

        // Members list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("group-members-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox membersList = new VBox();
        membersList.setSpacing(3);

        for (String member : groupItem.getGroupMembers()) {
            HBox memberBox = new HBox();
            memberBox.getStyleClass().add("group-member-row");
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
        if (mainChatArea != null) {
            mainChatArea.setBottom(messageInputArea);
        }
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

        if (selectedChat.getType() == ChatItem.Type.USER && isBlockedByUser(selectedChat.getName())) {
            showAlert("Blocked", "You cannot send messages to this user because they have blocked you.");
            return;
        }
        if (selectedChat.getType() == ChatItem.Type.USER && isUserBlockedByMe(selectedChat.getName())) {
            showAlert("Blocked", "You have blocked this user. Please unblock them to send messages.");
            return;
        }

        // Convert Unicode emojis back to [EMOJI:filename] placeholders for the server
        messageText = convertUnicodeToEmojiPlaceholders(messageText);

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
        removeSkeletonLoading();
        if ("System".equals(message.getSender())) {
            HBox centerRow = new HBox();
            centerRow.setAlignment(Pos.CENTER);
            centerRow.setPadding(new Insets(6, 10, 6, 10));
            
            Label systemLabel = new Label(message.getContent());
            systemLabel.getStyleClass().add("system-message-label");
            
            centerRow.getChildren().add(systemLabel);
            messagesContainer.getChildren().add(centerRow);
            
            if (scrollDebouncer != null) {
                scrollDebouncer.stop();
            }
            scrollDebouncer = new PauseTransition(Duration.millis(50));
            scrollDebouncer.setOnFinished(e -> {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            });
            scrollDebouncer.play();
            return;
        }

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
            if (isVoiceMessage(fileData)) {
                Node audioPlayer = createAudioPreview(fileData);
                messageContent.getChildren().add(audioPlayer);
                messageContent.setMaxWidth(270);
                messageContent.setPrefWidth(270);
            } else {
                VBox fileBox = new VBox(6);
                fileBox.getStyleClass().add("file-message-box");

            // Standard file message styling for all file types (including images)
            fileBox.setPadding(new Insets(8));
            fileBox.setMaxWidth(380);

            // File info row: icon + name/size
            HBox fileInfo = new HBox(8);
            fileInfo.setAlignment(Pos.CENTER_LEFT);
            fileInfo.setPadding(new Insets(4, 0, 4, 0));

            StackPane iconStack = new StackPane();
            iconStack.setPrefSize(32, 32);
            iconStack.setMinSize(32, 32);
            iconStack.setMaxSize(32, 32);

            String iconPath = getFileTypeIcon(fileData.getMimeType(), fileData.getOriginalName());
            System.out.println("DEBUG FILE_ICON: file=" + fileData.getOriginalName() 
                + ", mime=" + fileData.getMimeType() 
                + ", path=" + iconPath 
                + ", isDark=" + com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled());
            ImageView fileIcon = new ImageView(loadResourceImage(iconPath));
            fileIcon.setFitWidth(32);
            fileIcon.setFitHeight(32);
            // Let the generic file icon render with its original details (light-grey fill, dark outline)
            // which is highly visible on both light and dark backgrounds.

            fileIconStacks.put(fileData.getUuidName(), iconStack);
            fileIconImageViews.put(fileData.getUuidName(), fileIcon);

            boolean isSending = activeSendingFileIds.contains(fileData.getUuidName());
            boolean isDownloading = activeDownloadingFileIds.contains(fileData.getUuidName());

            VBox fileDetails = new VBox(2);

            Label fileName = new Label(fileData.getOriginalName());
            fileName.getStyleClass().add("file-name-label");
            fileName.setWrapText(true);
            fileName.setMaxWidth(200);

            HBox fileMetaInfo = new HBox(4);
            fileMetaInfo.setAlignment(Pos.CENTER_LEFT);

            Label fileSizeLabel = new Label(fileData.getFormattedFileSize());
            fileSizeLabel.getStyleClass().add("file-size-label");

            Label fileTypeLabel = new Label(getFileTypeDisplay(fileData.getMimeType()));
            fileTypeLabel.getStyleClass().add("file-type-info-label");
            fileTypeLabel.getStyleClass().add("file-type-label");
            fileTypeLabel.getStyleClass().add(getFileTypeStyleClass(fileData.getMimeType()));

            fileMetaInfo.getChildren().addAll(fileSizeLabel, fileTypeLabel);
            fileDetails.getChildren().addAll(fileName, fileMetaInfo);
            fileInfo.getChildren().addAll(iconStack, fileDetails);

            // Download and Open buttons
            Button downloadButton = new Button("Download");
            downloadButton.getStyleClass().add("download-button");
            downloadButton.setOnAction(e -> downloadFile(fileData));

            Button openButton = new Button("Open");
            openButton.getStyleClass().add("open-button");
            openButton.setOnAction(e -> openFile(fileData));

            if (isSending || isDownloading) {
                CircularProgressRing progressIndicator = new CircularProgressRing();
                if (isSending) {
                    activeSendProgressIndicators.put(fileData.getUuidName(), progressIndicator);
                } else {
                    activeDownloadProgressIndicators.put(fileData.getUuidName(), progressIndicator);
                }
                iconStack.getChildren().add(progressIndicator);
                downloadButton.setDisable(true);
                openButton.setDisable(true);
            } else {
                iconStack.getChildren().add(fileIcon);
            }

            fileDownloadButtons.put(fileData.getUuidName(), downloadButton);
            fileOpenButtons.put(fileData.getUuidName(), openButton);

            HBox buttonContainer = new HBox(6);
            buttonContainer.setAlignment(Pos.CENTER_RIGHT);
            buttonContainer.getChildren().addAll(downloadButton, openButton);

            VBox compactInfoSection = new VBox(4);
            compactInfoSection.setPrefWidth(280);
            compactInfoSection.setMaxWidth(Double.MAX_VALUE);
            compactInfoSection.getChildren().addAll(fileInfo, buttonContainer);
            fileBox.getChildren().add(compactInfoSection);

            messageContent.getChildren().add(fileBox);
            }

        } else {
            // Create TextFlow with mixed text and emoji content
            TextFlow textFlow = createTextFlowWithEmojis(message.getContent(), isSentByMe);
            
            messageContent.getChildren().add(textFlow);
        }

        // Apply message bubble styles using CSS classes
        messageContent.getStyleClass().add("message-bubble");
        if (isSentByMe) {
            messageContent.getStyleClass().add("message-bubble-sent");
        } else {
            messageContent.getStyleClass().add("message-bubble-received");
        }

        // Create timestamp without checkmarks, always right-aligned
        String timeStr = message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(timeStr);
        timeLabel.getStyleClass().add("message-time-internal");

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

        if (message.getType() == Message.MessageType.FILE_MESSAGE && message.getFileMessageData() != null) {
            String fileID = message.getFileMessageData().getUuidName();
            if (activeSendingFileIds.contains(fileID)) {
                activeSendingMessageNodes.put(fileID, fullMessageContainer);
            }
        }

        // Before adding the new message node, if there are active sending message nodes in the container,
        // temporarily remove them so they remain at the bottom
        boolean isCurrentMsgSending = false;
        if (message.getType() == Message.MessageType.FILE_MESSAGE && message.getFileMessageData() != null) {
            String fileID = message.getFileMessageData().getUuidName();
            isCurrentMsgSending = activeSendingFileIds.contains(fileID);
        }

        List<VBox> activeNodesToMove = new ArrayList<>();
        if (!isCurrentMsgSending) {
            for (String fileID : activeSendingFileIds) {
                VBox node = activeSendingMessageNodes.get(fileID);
                if (node != null && messagesContainer.getChildren().contains(node)) {
                    messagesContainer.getChildren().remove(node);
                    activeNodesToMove.add(node);
                }
            }
        }

        fullMessageContainer.getChildren().add(messageRow);

        // Apply dark mode coloring/icons immediately if dark mode is enabled
        updateIconsColor(fullMessageContainer, com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled());

        // Add the complete message container to the messages container
        messagesContainer.getChildren().add(fullMessageContainer);

        // Re-append active sending message nodes to the bottom
        if (!isCurrentMsgSending) {
            for (VBox node : activeNodesToMove) {
                messagesContainer.getChildren().add(node);
            }
        }

        // No entrance animation for messages

        // Update last message sender for grouping
        lastMessageSender = message.getSender();

        // Auto-scroll to bottom (debounced to prevent jumpy layout during batch message loads)
        if (scrollDebouncer != null) {
            scrollDebouncer.stop();
        }
        scrollDebouncer = new PauseTransition(Duration.millis(50));
        scrollDebouncer.setOnFinished(e -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
        scrollDebouncer.play();
    }

    private void showSkeletonLoading() {
        if (skeletonContainer == null) {
            skeletonContainer = new VBox(10);
            skeletonContainer.setPadding(new Insets(10));
            
            // Create some mock skeleton bubbles
            HBox bubble1 = createSkeletonBubble(false, 200, 2);
            HBox bubble2 = createSkeletonBubble(true, 150, 1);
            HBox bubble3 = createSkeletonBubble(false, 250, 3);
            HBox bubble4 = createSkeletonBubble(true, 100, 1);
            
            skeletonContainer.getChildren().addAll(bubble1, bubble2, bubble3, bubble4);
        }
        
        // Add to messagesContainer
        messagesContainer.getChildren().add(skeletonContainer);
        
        // Start moving gradient shimmer timeline
        if (shimmerTimeline != null) {
            shimmerTimeline.stop();
        }
        
        DoubleProperty gradientOffset = new SimpleDoubleProperty(0.0);
        shimmerTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(gradientOffset, 0.0)),
            new KeyFrame(Duration.millis(1200), new KeyValue(gradientOffset, 2.0))
        );
        shimmerTimeline.setCycleCount(Timeline.INDEFINITE);
        
        gradientOffset.addListener((obs, oldVal, newVal) -> {
            double offset = newVal.doubleValue();
            if (skeletonContainer != null) {
                for (Node child : skeletonContainer.getChildren()) {
                    if (child instanceof HBox) {
                        for (Node inner : ((HBox) child).getChildren()) {
                            if (inner.getStyleClass().contains("skeleton-bubble")) {
                                boolean isSent = inner.getStyleClass().contains("skeleton-bubble-sent");
                                applyShimmerBackground((VBox) inner, isSent, offset);
                            }
                        }
                    }
                }
            }
        });
        
        shimmerTimeline.play();
        
        // Start a timeout of 800ms to clear the skeleton in case no messages arrive
        if (skeletonTimeout != null) {
            skeletonTimeout.stop();
        }
        skeletonTimeout = new PauseTransition(Duration.millis(800));
        skeletonTimeout.setOnFinished(e -> removeSkeletonLoading());
        skeletonTimeout.play();
    }
    
    private void applyShimmerBackground(VBox bubble, boolean isSent, double offset) {
        boolean isDarkMode = darkModeToggle != null && darkModeToggle.isSelected();
        Color baseColor;
        Color highlightColor;
        
        if (isDarkMode) {
            if (isSent) {
                baseColor = Color.web("#2b5278");
                highlightColor = Color.web("#3b6a99");
            } else {
                baseColor = Color.web("#182533");
                highlightColor = Color.web("#25374a");
            }
        } else {
            if (isSent) {
                baseColor = Color.web("#d8f5b0");
                highlightColor = Color.web("#eafce0");
            } else {
                baseColor = Color.web("#ffffff");
                highlightColor = Color.web("#f3f7fa");
            }
        }
        
        LinearGradient gradient = new LinearGradient(
            -1.0 + offset, 0, offset, 0,
            true, CycleMethod.NO_CYCLE,
            new Stop(0.0, baseColor),
            new Stop(0.5, highlightColor),
            new Stop(1.0, baseColor)
        );
        
        bubble.setBackground(new Background(new BackgroundFill(gradient, new CornerRadii(12), Insets.EMPTY)));
    }
    
    private HBox createSkeletonBubble(boolean isSent, double width, int lines) {
        HBox row = new HBox();
        row.setPadding(new Insets(4, 10, 4, 10));
        
        VBox bubble = new VBox(6);
        bubble.setPrefWidth(width);
        bubble.setMaxWidth(width);
        bubble.getStyleClass().add("skeleton-bubble");
        if (isSent) {
            bubble.getStyleClass().add("skeleton-bubble-sent");
        } else {
            bubble.getStyleClass().add("skeleton-bubble-received");
        }
        
        applyShimmerBackground(bubble, isSent, 0.0);
        
        for (int i = 0; i < lines; i++) {
            Region line = new Region();
            line.setPrefHeight(10);
            line.getStyleClass().add("skeleton-line");
            if (i == lines - 1 && lines > 1) {
                line.setPrefWidth(width * 0.6);
                line.setMaxWidth(width * 0.6);
            } else {
                line.setPrefWidth(width * 0.85);
                line.setMaxWidth(width * 0.85);
            }
            bubble.getChildren().add(line);
        }
        
        // Add timestamp skeleton
        Region timeLine = new Region();
        timeLine.setPrefHeight(8);
        timeLine.setPrefWidth(30);
        timeLine.setMaxWidth(30);
        timeLine.getStyleClass().add("skeleton-line");
        HBox timeContainer = new HBox(timeLine);
        timeContainer.setAlignment(Pos.CENTER_RIGHT);
        bubble.getChildren().add(timeContainer);
        
        if (isSent) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(spacer, bubble);
        } else {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(bubble, spacer);
        }
        
        return row;
    }
    
    private void removeSkeletonLoading() {
        if (shimmerTimeline != null) {
            shimmerTimeline.stop();
            shimmerTimeline = null;
        }
        if (skeletonContainer != null && messagesContainer.getChildren().contains(skeletonContainer)) {
            messagesContainer.getChildren().remove(skeletonContainer);
        }
        if (skeletonTimeout != null) {
            skeletonTimeout.stop();
        }
    }

    private TextFlow createTextFlowWithEmojis(String messageContent) {
        return createTextFlowWithEmojis(messageContent, false);
    }
    
    private TextFlow createTextFlowWithEmojis(String messageContent, boolean isSentByMe) {
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
                    textNode.getStyleClass().add("chat-message-text");
                    textFlow.getChildren().add(textNode);
                    currentText.setLength(0);
                }

                // Find the end of the emoji placeholder
                int endIndex = messageContent.indexOf("]", i);
                if (endIndex != -1) {
                    String emojiPlaceholder = messageContent.substring(i, endIndex + 1);
                    String emojiFilename = emojiPlaceholder.substring(7, emojiPlaceholder.length() - 1); // Remove [EMOJI: and ]

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
                                fallbackEmoji.getStyleClass().add("chat-message-text");
                                textFlow.getChildren().add(fallbackEmoji);
                            }
                        } else {
                            // Fallback to text emoji
                            Text fallbackEmoji = new Text("📷");
                            fallbackEmoji.getStyleClass().add("chat-message-text");
                            textFlow.getChildren().add(fallbackEmoji);
                        }
                    } catch (Exception e) {
                        // If emoji image fails to load, show the filename as text
                        Text fallbackText = new Text("[" + emojiFilename + "]");
                        fallbackText.getStyleClass().add("emoji-fallback-text");
                        fallbackText.getStyleClass().add("chat-message-text");
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
            textNode.getStyleClass().add("chat-message-text");
            textFlow.getChildren().add(textNode);
        }

        // If no content was added, add empty text to prevent layout issues
        if (textFlow.getChildren().isEmpty()) {
            Text emptyText = new Text("");
            emptyText.getStyleClass().add("chat-message-text");
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

        if (groupItem == null) {
            pendingGroupMessages.computeIfAbsent(groupId, k -> new java.util.ArrayList<>()).add(message);
            return;
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
            } else if ("System".equals(sender)) {
                // Keep the displayMessage as is (no prefix)
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
        showGroupCreationWorkflow();
    }

    private void showGroupCreationWorkflow() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Create New Group");
        dialogStage.setResizable(false);

        VBox root = new VBox(10);
        root.setPrefSize(450, 580);
        root.getStyleClass().add("group-creation-root");
        if (darkModeToggle != null && darkModeToggle.isSelected()) {
            root.getStyleClass().add("dark-mode");
        }

        StackPane pagesContainer = new StackPane();
        pagesContainer.setPrefHeight(500);

        // Page Indicator Dots
        HBox indicators = new HBox(8);
        indicators.setAlignment(Pos.CENTER);
        indicators.setPadding(new Insets(0, 0, 20, 0));
        Circle dot1 = new Circle(4, Color.web("#3390ec"));
        Circle dot2 = new Circle(4, Color.web("#9baab8"));
        indicators.getChildren().addAll(dot1, dot2);

        // --- PAGE 1: GROUP NAME ---
        VBox page1 = new VBox(20);
        page1.setPadding(new Insets(25));
        page1.getStyleClass().add("group-creation-page");

        Label titleLabel1 = new Label("Create New Group");
        titleLabel1.getStyleClass().add("dialog-header");
        
        Label subLabel1 = new Label("Step 1: Choose a name for your group");
        subLabel1.getStyleClass().add("dialog-subheader");
        subLabel1.setStyle("-fx-text-fill: #6b7c93; -fx-font-size: 13.5px; -fx-font-family: 'Inter';");

        Label nameLabel = new Label("Group Name:");
        nameLabel.getStyleClass().add("dialog-label");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter group name...");
        nameField.setPrefHeight(40);
        nameField.getStyleClass().add("dialog-text-field");

        Region spacer1 = new Region();
        VBox.setVgrow(spacer1, Priority.ALWAYS);

        HBox buttonBox1 = new HBox(15);
        buttonBox1.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton1 = new Button("Cancel");
        cancelButton1.setPrefWidth(100);
        cancelButton1.getStyleClass().add("dialog-cancel-button");
        cancelButton1.setOnAction(e -> dialogStage.close());

        Button nextButton = new Button("Next  →");
        nextButton.setPrefWidth(100);
        nextButton.getStyleClass().add("dialog-create-button");

        buttonBox1.getChildren().addAll(cancelButton1, nextButton);
        page1.getChildren().addAll(titleLabel1, subLabel1, nameLabel, nameField, spacer1, buttonBox1);

        // --- PAGE 2: MEMBER SELECTION ---
        VBox page2 = new VBox(20);
        page2.setPadding(new Insets(25));
        page2.getStyleClass().add("group-creation-page");
        page2.setVisible(false);

        Label titleLabel2 = new Label("Select Members");
        titleLabel2.getStyleClass().add("dialog-header");

        Label subLabel2 = new Label("Step 2: Choose members to add");
        subLabel2.getStyleClass().add("dialog-subheader");
        subLabel2.setStyle("-fx-text-fill: #6b7c93; -fx-font-size: 13.5px; -fx-font-family: 'Inter';");

        TextField searchField = new TextField();
        searchField.setPromptText("Search contacts...");
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("member-selection-search-field");

        ListView<CheckBox> contactListView = new ListView<>();
        contactListView.setPrefHeight(280);

        ObservableList<CheckBox> contactCheckBoxes = FXCollections.observableArrayList();
        for (ChatItem item : allChatItems.values()) {
            if (item.getType() == ChatItem.Type.USER) {
                String contactName = item.getName();
                if (!contactName.equals(currentUsername)) {
                    CheckBox contactCheckBox = new CheckBox(contactName);
                    contactCheckBox.getStyleClass().add("member-selection-checkbox");
                    contactCheckBoxes.add(contactCheckBox);
                }
            }
        }
        contactListView.setItems(contactCheckBoxes);

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

        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        HBox buttonBox2 = new HBox(15);
        buttonBox2.setAlignment(Pos.CENTER_RIGHT);

        Button backButton = new Button("←  Back");
        backButton.setPrefWidth(100);
        backButton.getStyleClass().add("dialog-cancel-button");

        Button createButton = new Button("Create Group");
        createButton.setPrefWidth(120);
        createButton.getStyleClass().add("dialog-create-button");

        buttonBox2.getChildren().addAll(backButton, createButton);
        page2.getChildren().addAll(titleLabel2, subLabel2, searchField, contactListView, spacer2, buttonBox2);

        // Add both pages to stack
        pagesContainer.getChildren().addAll(page2, page1);

        // Next Button Actions
        nextButton.setOnAction(e -> {
            String groupName = nameField.getText().trim();
            if (!groupName.isEmpty()) {
                slideTransition(page1, page2, true);
                dot1.setFill(Color.web("#9baab8"));
                dot2.setFill(Color.web("#3390ec"));
            } else {
                nameField.setStyle("-fx-border-color: #e06c75; -fx-border-width: 1px; -fx-border-radius: 4px;");
            }
        });

        // Back Button Actions
        backButton.setOnAction(e -> {
            slideTransition(page2, page1, false);
            dot1.setFill(Color.web("#3390ec"));
            dot2.setFill(Color.web("#9baab8"));
        });

        // Create Button Action
        createButton.setOnAction(e -> {
            Set<String> selectedMembers = new HashSet<>();
            for (CheckBox checkBox : contactCheckBoxes) {
                if (checkBox.isSelected()) {
                    selectedMembers.add(checkBox.getText());
                }
            }
            createGroupWithMembers(nameField.getText().trim(), selectedMembers);
            dialogStage.close();
        });

        root.getChildren().addAll(pagesContainer, indicators);

        Scene dialogScene = new Scene(root);
        dialogScene.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
        if (darkModeToggle != null && darkModeToggle.isSelected()) {
            dialogScene.getRoot().getStyleClass().add("dark-mode");
            dialogScene.setFill(Color.web("#17212b"));
        } else {
            dialogScene.setFill(Color.WHITE);
        }
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    private void slideTransition(Pane fromPage, Pane toPage, boolean forward) {
        toPage.setVisible(true);

        double width = 450;
        double startXFrom = 0;
        double endXFrom = forward ? -width : width;

        double startXTo = forward ? width : -width;
        double endXTo = 0;

        fromPage.setTranslateX(startXFrom);
        toPage.setTranslateX(startXTo);

        TranslateTransition outTransition = new TranslateTransition(Duration.millis(250), fromPage);
        outTransition.setToX(endXFrom);

        TranslateTransition inTransition = new TranslateTransition(Duration.millis(250), toPage);
        inTransition.setToX(endXTo);

        ParallelTransition parallelTransition = new ParallelTransition(outTransition, inTransition);
        parallelTransition.setOnFinished(e -> {
            fromPage.setVisible(false);
            fromPage.setTranslateX(0); // Reset
        });
        parallelTransition.play();
    }

    private void showMemberSelectionPopup(String groupName, boolean isAddingMembers, String existingGroupId) {
        Stage memberSelectionStage = new Stage();
        memberSelectionStage.initModality(Modality.APPLICATION_MODAL);
        memberSelectionStage.setTitle("Add Members");
        memberSelectionStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setPrefSize(450, 550);
        mainContainer.getStyleClass().add("member-selection-dialog");

        Label titleLabel = new Label("Add Members to Group");
        titleLabel.getStyleClass().add("dialog-header");

        TextField searchField = new TextField();
        searchField.setPromptText("Search contacts...");
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("member-selection-search-field");

        ListView<CheckBox> contactListView = new ListView<>();
        contactListView.setPrefHeight(350);

        Set<String> existingMembers = new HashSet<>();
        if (existingGroupId != null) {
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

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(100);
        cancelButton.getStyleClass().add("dialog-cancel-button");
        cancelButton.setOnAction(e -> memberSelectionStage.close());

        Button addButton = new Button("Add Members");
        addButton.setPrefWidth(120);
        addButton.getStyleClass().add("dialog-create-button");
        addButton.setOnAction(e -> {
            Set<String> selectedMembers = new HashSet<>();
            for (CheckBox checkBox : contactCheckBoxes) {
                if (checkBox.isSelected()) {
                    selectedMembers.add(checkBox.getText());
                }
            }
            addMembersToGroup(existingGroupId, selectedMembers);
            memberSelectionStage.close();
        });

        buttonBox.getChildren().addAll(cancelButton, addButton);
        mainContainer.getChildren().addAll(titleLabel, searchField, contactListView, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
        if (darkModeToggle != null && darkModeToggle.isSelected()) {
            scene.getRoot().getStyleClass().add("dark-mode");
            scene.setFill(Color.web("#17212b"));
        } else {
            scene.setFill(Color.WHITE);
        }
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

        // Request blocked users and blockers list from server
        try {
            Message req = new Message(Message.MessageType.BLOCK_USER, currentUsername);
            req.setRecipient("GET_LISTS");
            chatClient.sendMessage(req);
        } catch (IOException e) {
            System.err.println("Failed to request block lists: " + e.getMessage());
        }
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
                getStyleClass().remove("cell-unread");
            } else {
                if (item.hasUnreadMessages()) {
                    if (!getStyleClass().contains("cell-unread")) {
                        getStyleClass().add("cell-unread");
                    }
                } else {
                    getStyleClass().remove("cell-unread");
                }

                VBox content = new VBox();
                content.setSpacing(3);
                content.setPadding(new Insets(8, 10, 8, 10));
                content.getStyleClass().add("contact-cell");
                if (item.hasUnreadMessages()) {
                    content.getStyleClass().add("contact-cell-unread");
                }

                HBox nameBox = new HBox();
                nameBox.setAlignment(Pos.CENTER_LEFT);
                nameBox.setSpacing(8);

                Label nameLabel = new Label(item.getName());
                nameLabel.getStyleClass().add("contact-name");
                nameLabel.setStyle("");
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
                    if (isUserBlockedByMe(item.getName())) {
                        Label blockedBadge = new Label("🚫");
                        blockedBadge.getStyleClass().add("blocked-indicator");
                        blockedBadge.setStyle("-fx-text-fill: #e06c75; -fx-font-size: 14px;");
                        nameBox.getChildren().add(blockedBadge);
                    }
                }

                content.getChildren().add(nameBox);

                // Show last message for all types instead of last seen status
                if (item.getLastMessage() != null && !item.getLastMessage().trim().isEmpty()) {
                    HBox messageBox = new HBox();
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageBox.setSpacing(8);
                    messageBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    messageBox.setMaxWidth(Double.MAX_VALUE);

                    Label messageLabel = new Label(item.getLastMessage());
                    messageLabel.getStyleClass()
                            .add(item.hasUnreadMessages() ? "contact-message-unread" : "contact-message");
                    messageLabel.setStyle("");
                    
                    // Set max width to ensure timestamp is always visible (reserve ~50px for timestamp)
                    messageLabel.setMaxWidth(150); // Reduced from unlimited to ensure timestamp space
                    messageLabel.setWrapText(false);
                    messageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label timeLabel = new Label(item.getLastMessageTimestamp());
                    timeLabel.getStyleClass().add("contact-time");
                    timeLabel.setStyle("");
                    timeLabel.setMinWidth(Region.USE_PREF_SIZE); // Prevent shrinking
                    timeLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);

                    if (item.hasUnreadMessages()) {
                        Circle unreadDot = new Circle(4, Color.web("#3390ec"));
                        unreadDot.getStyleClass().add("unread-indicator-dot");
                        HBox rightSideBox = new HBox(5);
                        rightSideBox.setAlignment(Pos.CENTER_RIGHT);
                        rightSideBox.getChildren().addAll(timeLabel, unreadDot);
                        messageBox.getChildren().addAll(messageLabel, spacer, rightSideBox);
                    } else {
                        messageBox.getChildren().addAll(messageLabel, spacer, timeLabel);
                    }
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
        if (activeEmojiPanel != null) {
            closeEmojiPanel();
        } else {
            closeOptionsPanel();
            showModernEmojiPicker();
        }
    }

    private void showModernEmojiPicker() {
        // Close if already open
        if (activeEmojiPanel != null) {
            closeEmojiPanel();
            return;
        }

        // Main container with modern styling
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(0);
        mainContainer.setPrefSize(390, 400); // Fixed size bounds
        mainContainer.getStyleClass().addAll("emoji-picker-popup", "modern-emoji-picker");

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
        categoryTabs.setSpacing(2);
        categoryTabs.setPadding(new Insets(6, 6, 6, 6));
        categoryTabs.setAlignment(Pos.CENTER); // Center the tabs
        categoryTabs.setFillHeight(false);
        HBox.setHgrow(categoryTabs, javafx.scene.layout.Priority.ALWAYS);

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
        // Use emoji icons for category display labels
        String[] shortLabels = { "\uD83D\uDE00", "\uD83D\uDC64", "\uD83D\uDC3B", "\uD83C\uDF55", "\u26BD", "\uD83C\uDF0D", "\uD83D\uDCE6", "\uD83D\uDD2E",
                "\uD83C\uDFC1" };

        // Add ALL category buttons to the tab bar (all fit in one row with emoji icons)
        for (int i = 0; i < categories.length; i++) {
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
                    showSearchResults(newValue, emojiCanvas);
                }
            }));
            searchDebouncer.play();
        });

        // Add categories directly to the container for proper layout
        categoryContainer.getChildren().add(categoryTabs);

        // Assemble panel content
        mainContainer.getChildren().addAll(searchField, categoryContainer, emojiScrollPane);

        activeEmojiPanel = mainContainer;

        // Position emoji panel at the bottom right of the messages area
        mainContainer.setMinWidth(390);
        mainContainer.setPrefWidth(390);
        mainContainer.setMaxWidth(390);
        mainContainer.setMinHeight(400);
        mainContainer.setPrefHeight(400);
        mainContainer.setMaxHeight(400);
        StackPane.setAlignment(mainContainer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(mainContainer, new Insets(0, 10, 10, 0));

        // Create transparent dismiss overlay to cover the chatAreaStack
        activeEmojiDismissOverlay = new Region();
        activeEmojiDismissOverlay.setStyle("-fx-background-color: transparent;");
        activeEmojiDismissOverlay.setMaxWidth(Double.MAX_VALUE);
        activeEmojiDismissOverlay.setMaxHeight(Double.MAX_VALUE);
        activeEmojiDismissOverlay.setOnMouseClicked(e -> closeEmojiPanel());

        // Keyboard support: Escape closes the picker
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closeEmojiPanel();
            }
        });
        mainContainer.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closeEmojiPanel();
            }
        });

        // Add to stack pane
        if (chatAreaStack != null) {
            chatAreaStack.getChildren().addAll(activeEmojiDismissOverlay, mainContainer);
        }

        // Apply animations: slide up and fade in
        mainContainer.setOpacity(0.0);
        mainContainer.setTranslateY(30);

        activeEmojiDismissOverlay.setOpacity(0.0);

        FadeTransition fadeInOverlay = new FadeTransition(Duration.millis(250), activeEmojiDismissOverlay);
        fadeInOverlay.setToValue(1.0);

        FadeTransition fadeInPanel = new FadeTransition(Duration.millis(250), mainContainer);
        fadeInPanel.setToValue(1.0);

        TranslateTransition slideInPanel = new TranslateTransition(Duration.millis(250), mainContainer);
        slideInPanel.setToY(0);

        ParallelTransition openAnim = new ParallelTransition(fadeInOverlay, fadeInPanel, slideInPanel);
        openAnim.play();
    }

    private Button createCategoryButton(String label, String category) {
        Button btn = new Button(label);

        // Store the actual category in the user data
        btn.setUserData(category);

        // Configure button as a square emoji icon tab
        btn.setPrefHeight(34);
        btn.setPrefWidth(38);
        btn.setMinHeight(34);
        btn.setMinWidth(38);
        // Ensure the emoji character renders large
        btn.setStyle("-fx-font-size: 16px;");
        btn.getStyleClass().add("emoji-category-button");
        setupButtonHoverScaleOnly(btn);

        // Always show full category name as tooltip
        btn.setTooltip(new Tooltip(category));

        return btn;
    }

    private void selectCategory(String category, Map<String, Button> categoryButtons, EmojiCanvas emojiCanvas) {
        System.out.println("DEBUG: Selecting category: " + category);
        activeCategory = category;

        // Update button styles using CSS classes instead of inline styles
        for (Map.Entry<String, Button> entry : categoryButtons.entrySet()) {
            Button btn = entry.getValue();
            if (entry.getKey().equals(category)) {
                if (!btn.getStyleClass().contains("selected")) {
                    btn.getStyleClass().add("selected");
                }
            } else {
                btn.getStyleClass().remove("selected");
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

    private void showSearchResults(String query, EmojiCanvas emojiCanvas) {
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

        // Convert filename to Unicode emoji character
        String unicodeEmoji = getUnicodeFromEmojiFilename(emojiFilename);
        if (unicodeEmoji == null || unicodeEmoji.isEmpty()) {
            unicodeEmoji = "[EMOJI:" + emojiFilename + "]";
        }

        String newText = currentText + unicodeEmoji;

        messageInput.setText(newText);

        // Set caret position after the inserted emoji and request focus
        Platform.runLater(() -> {
            messageInput.requestFocus();
            messageInput.positionCaret(newText.length());
        });

        System.out.println("DEBUG: New text: '" + newText + "'");
        System.out.println("DEBUG: Caret positioned at end: " + newText.length());
    }

    private String getUnicodeFromEmojiFilename(String filename) {
        try {
            String hex = filename;
            if (hex.endsWith(".png")) {
                hex = hex.substring(0, hex.length() - 4);
            }
            String[] parts = hex.split("-");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                int codePoint = Integer.parseInt(part, 16);
                sb.appendCodePoint(codePoint);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void initializeUnicodeToEmojiFilenameMap() {
        if (unicodeToEmojiFilenameMap.isEmpty() && allEmojis != null) {
            for (String filename : allEmojis.keySet()) {
                String unicode = getUnicodeFromEmojiFilename(filename);
                if (unicode != null && !unicode.isEmpty()) {
                    unicodeToEmojiFilenameMap.put(unicode, filename);
                }
            }
        }
    }

    private String convertUnicodeToEmojiPlaceholders(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        initializeUnicodeToEmojiFilenameMap();
        if (unicodeToEmojiFilenameMap.isEmpty()) {
            return text;
        }

        // Sort keys by length in descending order to avoid partial replacement of sequences
        List<String> sortedUnicodes = new ArrayList<>(unicodeToEmojiFilenameMap.keySet());
        sortedUnicodes.sort((a, b) -> Integer.compare(b.length(), a.length()));

        String result = text;
        for (String unicode : sortedUnicodes) {
            if (result.contains(unicode)) {
                String filename = unicodeToEmojiFilenameMap.get(unicode);
                result = result.replace(unicode, "[EMOJI:" + filename + "]");
            }
        }

        return result;
    }

    @FXML
    private void handleChatMenu() {
        if (selectedChat == null)
            return;

        if (activeOptionsPanel != null) {
            closeOptionsPanel();
            return;
        }

        // Create active dismiss overlay
        activeDismissOverlay = new Region();
        activeDismissOverlay.setStyle("-fx-background-color: transparent;");
        activeDismissOverlay.setOnMouseClicked(e -> closeOptionsPanel());

        // Create options panel
        VBox optionsPanel = new VBox();
        optionsPanel.getStyleClass().add("chat-options-panel");
        optionsPanel.setMaxWidth(200);
        optionsPanel.setMaxHeight(Region.USE_PREF_SIZE);

        if (selectedChat.getType() == ChatItem.Type.GROUP) {
            HBox addMembersItem = createOptionItem("Add members", e -> {
                closeOptionsPanel();
                showMemberSelectionPopup("", true, selectedChat.getGroupId());
            });
            optionsPanel.getChildren().add(addMembersItem);

            if (selectedChat.isGroupAdmin(currentUsername) || selectedChat.isGroupCreator(currentUsername)) {
                HBox changeNameItem = createOptionItem("Change group name", e -> {
                    closeOptionsPanel();
                    showChangeGroupNameDialog();
                });
                optionsPanel.getChildren().add(changeNameItem);
            }

            if (!selectedChat.isGroupCreator(currentUsername)) {
                HBox leaveGroupItem = createOptionItem("Leave group", e -> {
                    closeOptionsPanel();
                    leaveGroup();
                });
                optionsPanel.getChildren().add(leaveGroupItem);
            }

            if (selectedChat.isGroupCreator(currentUsername)) {
                HBox deleteGroupItem = createOptionItem("Delete group", e -> {
                    closeOptionsPanel();
                    showDeleteGroupConfirmationDialog(selectedChat);
                });
                optionsPanel.getChildren().add(deleteGroupItem);
            }

            HBox deleteChatItem = createOptionItem("Delete chat", e -> {
                closeOptionsPanel();
                promptToDeleteChat(selectedChat);
            });
            optionsPanel.getChildren().add(deleteChatItem);
        } else {
            String targetUser = selectedChat.getName();
            boolean isBlocked = isUserBlockedByMe(targetUser);
            HBox blockUserItem = createOptionItem(isBlocked ? "Unblock user" : "Block user", e -> {
                closeOptionsPanel();
                if (isBlocked) {
                    unblockUser(targetUser);
                } else {
                    blockUser(targetUser);
                }
            });
            optionsPanel.getChildren().add(blockUserItem);

            HBox deleteChatItem = createOptionItem("Delete chat", e -> {
                closeOptionsPanel();
                promptToDeleteChat(selectedChat);
            });
            optionsPanel.getChildren().add(deleteChatItem);
        }

        activeOptionsPanel = optionsPanel;

        // Position options panel - set a wider layout so text is not truncated
        optionsPanel.setMinWidth(180);
        optionsPanel.setPrefWidth(220);
        StackPane.setAlignment(optionsPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsPanel, new Insets(10, 20, 0, 0));

        // Add to stack pane
        if (chatAreaStack != null) {
            chatAreaStack.getChildren().addAll(activeDismissOverlay, optionsPanel);
        }

        // Slide down animation
        optionsPanel.setTranslateY(-20);
        optionsPanel.setOpacity(0.0);

        TranslateTransition translate = new TranslateTransition(Duration.millis(150), optionsPanel);
        translate.setToY(0);

        FadeTransition fade = new FadeTransition(Duration.millis(150), optionsPanel);
        fade.setToValue(1.0);

        ParallelTransition parallel = new ParallelTransition(translate, fade);
        parallel.play();
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
        com.bakbak.javafx_proj_1_2.ChatApplication.showToast(message);
    }

    private void showInWindowConfirmationDialog(String title, String description, Runnable onConfirm) {
        closeEmojiPanel();
        closeOptionsPanel();

        Region dimOverlay = new Region();
        dimOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
        dimOverlay.setMaxWidth(Double.MAX_VALUE);
        dimOverlay.setMaxHeight(Double.MAX_VALUE);

        VBox dialogCard = new VBox(15);
        dialogCard.setPadding(new Insets(20));
        dialogCard.setMaxWidth(350);
        dialogCard.setMaxHeight(Region.USE_PREF_SIZE);
        dialogCard.setAlignment(Pos.TOP_LEFT);

        boolean isDark = com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled();
        String cardStyle = "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 1; "
                + (isDark 
                    ? "-fx-background-color: #17212b; -fx-border-color: #101921;" 
                    : "-fx-background-color: #ffffff; -fx-border-color: #dde5f0;");
        dialogCard.setStyle(cardStyle);
        dialogCard.setEffect(new DropShadow(15, 0, 5, Color.rgb(0, 0, 0, 0.25)));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;" 
                + (isDark ? "-fx-text-fill: #ffffff;" : "-fx-text-fill: #1a1a2e;"));

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(310);
        descLabel.setStyle("-fx-font-size: 14px;" 
                + (isDark ? "-fx-text-fill: #a0acba;" : "-fx-text-fill: #5c6c7d;"));

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: " + (isDark ? "#2c3847;" : "#e9ecef;") 
                + "-fx-text-fill: " + (isDark ? "#a0acba;" : "#495057;") 
                + "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");

        Button confirmButton = new Button("Delete");
        confirmButton.setStyle("-fx-background-color: #e06c75; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        dialogCard.getChildren().addAll(titleLabel, descLabel, buttonBox);

        StackPane alignmentWrapper = new StackPane(dialogCard);
        alignmentWrapper.setAlignment(Pos.CENTER);
        alignmentWrapper.setMaxWidth(Double.MAX_VALUE);
        alignmentWrapper.setMaxHeight(Double.MAX_VALUE);

        StackPane overlayContainer = new StackPane();
        overlayContainer.getChildren().addAll(dimOverlay, alignmentWrapper);
        overlayContainer.setMaxWidth(Double.MAX_VALUE);
        overlayContainer.setMaxHeight(Double.MAX_VALUE);

        Runnable closeDialog = () -> {
            FadeTransition fadeOutOverlay = new FadeTransition(Duration.millis(150), overlayContainer);
            fadeOutOverlay.setToValue(0.0);
            
            ScaleTransition scaleOutCard = new ScaleTransition(Duration.millis(150), dialogCard);
            scaleOutCard.setFromX(1.0);
            scaleOutCard.setFromY(1.0);
            scaleOutCard.setToX(0.9);
            scaleOutCard.setToY(0.9);

            ParallelTransition closeAnim = new ParallelTransition(fadeOutOverlay, scaleOutCard);
            closeAnim.setOnFinished(ev -> {
                if (chatAreaStack != null) {
                    chatAreaStack.getChildren().remove(overlayContainer);
                }
            });
            closeAnim.play();
        };

        cancelButton.setOnAction(ev -> closeDialog.run());
        confirmButton.setOnAction(ev -> {
            closeDialog.run();
            onConfirm.run();
        });

        overlayContainer.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closeDialog.run();
            }
        });

        if (chatAreaStack != null) {
            chatAreaStack.getChildren().add(overlayContainer);
            overlayContainer.requestFocus();
        }

        overlayContainer.setOpacity(0.0);
        dialogCard.setScaleX(0.9);
        dialogCard.setScaleY(0.9);

        FadeTransition fadeInOverlay = new FadeTransition(Duration.millis(200), overlayContainer);
        fadeInOverlay.setToValue(1.0);

        ScaleTransition scaleInCard = new ScaleTransition(Duration.millis(200), dialogCard);
        scaleInCard.setFromX(0.9);
        scaleInCard.setFromY(0.9);
        scaleInCard.setToX(1.0);
        scaleInCard.setToY(1.0);

        ParallelTransition openAnim = new ParallelTransition(fadeInOverlay, scaleInCard);
        openAnim.play();
    }

    private void promptToDeleteChat(ChatItem item) {
        showInWindowConfirmationDialog(
            "Delete Chat",
            "Are you sure you want to delete this chat history? This action cannot be undone.",
            () -> deleteChatHistory(item)
        );
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

        managementGroupItem = groupItem;

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setPrefSize(450, 600);
        layout.getStyleClass().add("group-management-dialog");

        Label title = new Label("Members (" + groupItem.getMemberCount() + ")");
        title.getStyleClass().add("group-management-title");

        managementMemberListView = new ListView<>();
        managementMemberListView.getStyleClass().add("dialog-list-view");
        refreshManagementWindow(managementMemberListView, groupItem);

        Button addMemberButton = new Button("Add Member");
        addMemberButton.getStyleClass().add("dialog-create-button");
        addMemberButton.setOnAction(e -> showMemberSelectionPopup("", true, groupItem.getGroupId()));

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setSpacing(10);
        HBox.setHgrow(bottomBar, Priority.ALWAYS);

        bottomBar.getChildren().add(addMemberButton);

        layout.getChildren().addAll(title, managementMemberListView, bottomBar);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
        if (darkModeToggle != null && darkModeToggle.isSelected()) {
            scene.getRoot().getStyleClass().add("dark-mode");
            scene.setFill(Color.web("#17212b"));
        } else {
            scene.setFill(Color.WHITE);
        }
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
            memberRow.setPadding(new Insets(6, 8, 6, 8));
            memberRow.getStyleClass().add("group-management-row");

            Label nameLabel = new Label(member);
            nameLabel.getStyleClass().add("group-management-member-name");

            Label roleLabel = new Label();
            if (groupItem.isGroupCreator(member)) {
                roleLabel.setText("Owner");
                roleLabel.getStyleClass().add("group-member-owner");
            } else if (groupItem.isGroupAdmin(member)) {
                roleLabel.setText("Admin");
                roleLabel.getStyleClass().add("group-member-admin");
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            memberRow.getChildren().addAll(nameLabel, roleLabel, spacer);

            if (isCurrentUserAdmin && !member.equals(currentUsername)) {
                // Add management buttons for admins
                if (groupItem.isGroupCreator(currentUsername) && !groupItem.isGroupCreator(member)) {
                    if (groupItem.isGroupAdmin(member)) {
                        Button demoteButton = new Button("Demote");
                        demoteButton.getStyleClass().addAll("dialog-btn-sm", "dialog-btn-danger");
                        demoteButton.setOnAction(e -> removeAdmin(groupItem.getGroupId(), member));
                        memberRow.getChildren().add(demoteButton);
                    } else {
                        Button promoteButton = new Button("Promote");
                        promoteButton.getStyleClass().addAll("dialog-btn-sm", "dialog-btn-primary");
                        promoteButton.setOnAction(e -> promoteToAdmin(groupItem.getGroupId(), member));
                        memberRow.getChildren().add(promoteButton);
                    }
                }

                if (!groupItem.isGroupCreator(member)) {
                    Button removeButton = new Button("Remove");
                    removeButton.getStyleClass().addAll("dialog-btn-sm", "dialog-btn-danger");
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

        fileChooser.getExtensionFilters().addAll(allFiles, imageFiles, videoFiles, audioFiles, documentFiles);

        // Show the file chooser dialog
        File selectedFile = fileChooser.showOpenDialog(ChatApplication.getPrimaryStage());

        if (selectedFile != null && selectedFile.exists()) {
            sendFile(selectedFile);
        }
    }

    private void sendFile(File file) {
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
        } catch (Exception e) {
            mimeType = "application/octet-stream";
        }

        String originalName = file.getName();
        String fileID = java.util.UUID.randomUUID().toString();
        long fileSize = file.length();
        String finalMimeType = mimeType;

        FileMessageData fileMessageData = new FileMessageData(originalName, fileID, fileSize, finalMimeType);

        // Create the file message
        Message fileMessage = new Message(Message.MessageType.FILE_MESSAGE, currentUsername);
        if (selectedChat.getType() == ChatItem.Type.GROUP) {
            fileMessage.setGroupId(selectedChat.getGroupId()); // Use groupId for group messages
        } else {
            fileMessage.setRecipient(selectedChat.getName()); // Use recipient for private messages
        }
        fileMessage.setFileMessageData(fileMessageData);
        fileMessage.setContent(fileMessageData.toString()); // Set the content field for serialization
        fileMessage.setTimestamp(LocalDateTime.now());

        // Cache the local file immediately so playback/actions know it's available locally
        cacheDownloadedFile(fileID, file);

        // Mark this file ID as actively sending
        activeSendingFileIds.add(fileID);
        activeSendingMessages.put(fileID, fileMessage);

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
                System.err.println("Error updating UI before file send: " + e.getMessage());
            }
        });

        // Start background thread for file upload
        new Thread(() -> {
            try {
                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);

                ProgressCallback progressCallback = new ProgressCallback() {
                    @Override
                    public void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed, long totalBytes) {
                        double progress = (double) bytesProcessed / totalBytes;
                        Platform.runLater(() -> {
                            CircularProgressRing pi = activeSendProgressIndicators.get(fileID);
                            if (pi != null) {
                                pi.setProgress(progress);
                            }
                        });
                    }

                    @Override
                    public void onTransferComplete() {
                        Platform.runLater(() -> {
                            activeSendingFileIds.remove(fileID);
                            activeSendingMessages.remove(fileID);
                            activeSendProgressIndicators.remove(fileID);

                            // Send the message to server now that file is fully uploaded
                            new Thread(() -> {
                                try {
                                    chatClient.sendMessage(fileMessage);
                                } catch (Exception e) {
                                    System.err.println("Error sending message to server: " + e.getMessage());
                                }
                            }).start();

                            // Restore the normal icon in the UI
                            restoreFileIcon(fileID);
                        });
                    }

                    @Override
                    public void onTransferError(String error) {
                        Platform.runLater(() -> {
                            activeSendingFileIds.remove(fileID);
                            activeSendingMessages.remove(fileID);
                            activeSendProgressIndicators.remove(fileID);
                            showAlert("Upload Error", "Failed to upload file: " + error);
                            restoreFileIcon(fileID);
                        });
                    }
                };

                CompletableFuture<String> uploadFuture = chunkSender.sendFile(file, fileID, progressCallback);
                String uploadedFileID = uploadFuture.get(); // Wait for completion

                if (uploadedFileID == null) {
                    Platform.runLater(() -> {
                        resetSendButton();
                        showAlert("Error", "Failed to upload file.");
                        activeSendingFileIds.remove(fileID);
                        activeSendingMessages.remove(fileID);
                        activeSendProgressIndicators.remove(fileID);
                        restoreFileIcon(fileID);
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    resetSendButton();
                    showAlert("Error", "Failed to send file: " + e.getMessage());
                    activeSendingFileIds.remove(fileID);
                    activeSendingMessages.remove(fileID);
                    activeSendProgressIndicators.remove(fileID);
                    restoreFileIcon(fileID);
                });
            }
        }).start();
    }

    private void restoreFileIcon(String fileID) {
        Platform.runLater(() -> {
            StackPane iconStack = fileIconStacks.get(fileID);
            ImageView fileIcon = fileIconImageViews.get(fileID);
            if (iconStack != null && fileIcon != null) {
                iconStack.getChildren().clear();
                iconStack.getChildren().add(fileIcon);
            }
            Button downloadBtn = fileDownloadButtons.remove(fileID);
            if (downloadBtn != null) {
                downloadBtn.setDisable(false);
            }
            Button openBtn = fileOpenButtons.remove(fileID);
            if (openBtn != null) {
                openBtn.setDisable(false);
            }
        });
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

    private String getFileTypeIcon(String mimeType, String filename) {
        String baseIconPath = "/com/bakbak/javafx_proj_1_2/icons/";
        
        // Normalize mimeType and filename
        String mime = (mimeType != null) ? mimeType.toLowerCase() : "";
        String file = (filename != null) ? filename.toLowerCase() : "";
        
        // 1. Check specific document types FIRST to avoid matching generic XML
        if (mime.contains("word") || mime.contains("document") || file.endsWith(".doc") || file.endsWith(".docx") || file.endsWith(".odt")) {
            return baseIconPath + "doc_file_icon.png";
        }
        
        // PDF
        if (mime.contains("pdf") || file.endsWith(".pdf")) {
            return baseIconPath + "pdf_file_icon.png";
        }
        
        // Spreadsheet / Excel
        if (mime.contains("excel") || mime.contains("spreadsheet") || file.endsWith(".xls") || file.endsWith(".xlsx") || file.endsWith(".ods")) {
            return baseIconPath + "excel_file_icon.png";
        }
        
        // Presentation / PowerPoint
        if (mime.contains("powerpoint") || mime.contains("presentation") || file.endsWith(".ppt") || file.endsWith(".pptx") || file.endsWith(".odp")) {
            return baseIconPath + "ppt_file_icon.png";
        }
        
        // Archives
        if (mime.contains("zip") || mime.contains("rar") || mime.contains("7z") || mime.contains("tar") || mime.contains("gzip") || mime.contains("archive")
                || file.endsWith(".zip") || file.endsWith(".rar") || file.endsWith(".7z") || file.endsWith(".tar") || file.endsWith(".gz") || file.endsWith(".bz2") || file.endsWith(".xz")) {
            return baseIconPath + "archive_file_icon.png";
        }
        
        // Images
        if (mime.startsWith("image/") || file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".jpeg") || file.endsWith(".gif") || file.endsWith(".bmp") || file.endsWith(".webp") || file.endsWith(".svg")) {
            return baseIconPath + "image_file_icon.png";
        }
        
        // Video
        if (mime.startsWith("video/") || file.endsWith(".mp4") || file.endsWith(".avi") || file.endsWith(".mov") || file.endsWith(".mkv") || file.endsWith(".wmv") || file.endsWith(".webm") || file.endsWith(".flv") || file.endsWith(".3gp")) {
            return baseIconPath + "video_file_icon.png";
        }
        
        // Audio
        if (mime.startsWith("audio/") || file.endsWith(".mp3") || file.endsWith(".wav") || file.endsWith(".flac") || file.endsWith(".ogg") || file.endsWith(".m4a") || file.endsWith(".aac") || file.endsWith(".wma")) {
            return baseIconPath + "audio_file_icon.png";
        }
        
        // Text / Data (Map missing XML/JSON/HTML/CSV/EXE icons to existing colored/gray icons)
        if (mime.startsWith("text/plain") || mime.startsWith("text/html") || mime.startsWith("application/json") || mime.contains("xml") || mime.contains("csv")
                || file.endsWith(".txt") || file.endsWith(".log") || file.endsWith(".ini") || file.endsWith(".conf") || file.endsWith(".html") || file.endsWith(".htm") || file.endsWith(".json") || file.endsWith(".xml") || file.endsWith(".csv")) {
            return baseIconPath + "txt_file_icon.png";
        }
        
        // Default / Generic (Also matches exe/dll/bat/sh/deb since no exe_file_icon.png exists)
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

            // Always use fit scale - image should resize with window (both up and down)
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
        HBox audioContainer = new HBox(10);
        audioContainer.setAlignment(Pos.CENTER_LEFT);
        audioContainer.getStyleClass().add("audio-preview");
        audioContainer.setPadding(new Insets(2, 6, 2, 6));
        audioContainer.setMaxWidth(250);
        audioContainer.setPrefWidth(250);
        audioContainer.setMaxHeight(40);
        audioContainer.setPrefHeight(40);

        // Play/Pause button with custom PNG
        Button playPauseBtn = createPlayPauseButton();

        // Progress container
        VBox progressContainer = new VBox(4);
        progressContainer.setAlignment(Pos.CENTER);

        // Create a StackPane to hold the progress bar and the drag handle
        StackPane progressStack = new StackPane();
        progressStack.setPrefWidth(150);
        progressStack.setPrefHeight(14); // Make it a bit taller to accommodate the drag handle

        // Progress bar with custom styling
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150);
        progressBar.setPrefHeight(6);
        progressBar.getStyleClass().add("custom-progress-bar");

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
        playPauseBtn.setMinSize(32, 32);
        playPauseBtn.setMaxSize(32, 32);
        playPauseBtn.setPrefSize(32, 32);

        // Set play icon initially
        setButtonIcon(playPauseBtn, PLAY_ICON_PATH, 14);

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
                if (com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled()) {
                    javafx.scene.effect.ColorAdjust ca = new javafx.scene.effect.ColorAdjust();
                    ca.setBrightness(0.9);
                    imageView.setEffect(ca);
                }
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
                if (isFileDownloaded(fileData.getUuidName())) {
                    audioFile[0] = getDownloadedFile(fileData.getUuidName());
                } else {
                    // Download the audio file
                    FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);
                    CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                            fileData.getOriginalName());
                    audioFile[0] = downloadFuture.get();
                    cacheDownloadedFile(fileData.getUuidName(), audioFile[0]);
                }

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
                String fileID = fileData.getUuidName();
                // Check if file is already downloaded
                if (isFileDownloaded(fileID)) {
                    // File already downloaded, no popup needed
                    return;
                }

                // Add to active downloading set
                activeDownloadingFileIds.add(fileID);

                // Update UI: show spinner on the file icon, disable buttons
                Platform.runLater(() -> {
                    StackPane iconStack = fileIconStacks.get(fileID);
                    if (iconStack != null) {
                        CircularProgressRing progressIndicator = new CircularProgressRing();
                        activeDownloadProgressIndicators.put(fileID, progressIndicator);
                        iconStack.getChildren().clear();
                        iconStack.getChildren().add(progressIndicator);
                    }
                    Button downloadBtn = fileDownloadButtons.get(fileID);
                    if (downloadBtn != null) {
                        downloadBtn.setDisable(true);
                    }
                    Button openBtn = fileOpenButtons.get(fileID);
                    if (openBtn != null) {
                        openBtn.setDisable(true);
                    }
                });

                FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(), FileChunkReceiver.CHUNK_PORT);

                // Check if it's a video file to show progress
                boolean isVideo = fileData.getMimeType() != null && fileData.getMimeType().startsWith("video/");
                ProgressCallback progressCallback = new ProgressCallback() {
                    @Override
                    public void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed,
                            long totalBytes) {
                        double progress = (double) bytesProcessed / totalBytes;
                        Platform.runLater(() -> {
                            CircularProgressRing pi = activeDownloadProgressIndicators.get(fileID);
                            if (pi != null) {
                                pi.setProgress(progress);
                            }
                        });
                    }

                    @Override
                    public void onTransferComplete() {
                        Platform.runLater(() -> {
                            activeDownloadingFileIds.remove(fileID);
                            activeDownloadProgressIndicators.remove(fileID);
                            restoreFileIcon(fileID);
                        });
                    }

                    @Override
                    public void onTransferError(String error) {
                        Platform.runLater(() -> {
                            activeDownloadingFileIds.remove(fileID);
                            activeDownloadProgressIndicators.remove(fileID);
                            restoreFileIcon(fileID);
                            showAlert("Download Error", "Failed to download file: " + error);
                        });
                    }
                };

                CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                        fileData.getOriginalName(), progressCallback);
                File downloadedFile = downloadFuture.get(); // Wait for completion

                // Cache the downloaded file
                cacheDownloadedFile(fileData.getUuidName(), downloadedFile);

            } catch (Exception e) {
                String fileID = fileData.getUuidName();
                Platform.runLater(() -> {
                    activeDownloadingFileIds.remove(fileID);
                    activeDownloadProgressIndicators.remove(fileID);
                    restoreFileIcon(fileID);
                    showAlert("Download Error", "Failed to download file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void openFile(FileMessageData fileData) {
        new Thread(() -> {
            try {
                File fileToOpen;
                String fileID = fileData.getUuidName();

                // Check if file is already downloaded
                if (isFileDownloaded(fileID)) {
                    fileToOpen = getDownloadedFile(fileID);
                    System.out.println("Using cached file: " + fileToOpen.getAbsolutePath());
                } else {
                    // Add to active downloading set
                    activeDownloadingFileIds.add(fileID);

                    // Update UI: show spinner on the file icon, disable buttons
                    Platform.runLater(() -> {
                        StackPane iconStack = fileIconStacks.get(fileID);
                        if (iconStack != null) {
                            CircularProgressRing progressIndicator = new CircularProgressRing();
                            activeDownloadProgressIndicators.put(fileID, progressIndicator);
                            iconStack.getChildren().clear();
                            iconStack.getChildren().add(progressIndicator);
                        }
                        Button downloadBtn = fileDownloadButtons.get(fileID);
                        if (downloadBtn != null) {
                            downloadBtn.setDisable(true);
                        }
                        Button openBtn = fileOpenButtons.get(fileID);
                        if (openBtn != null) {
                            openBtn.setDisable(true);
                        }
                    });

                    // File not cached, need to download
                    FileChunkSender chunkSender = new FileChunkSender(chatClient.getHost(),
                            FileChunkReceiver.CHUNK_PORT);

                    // Check if it's a video file to show progress
                    boolean isVideo = fileData.getMimeType() != null && fileData.getMimeType().startsWith("video/");
                    ProgressCallback progressCallback = new ProgressCallback() {
                        @Override
                        public void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed,
                                long totalBytes) {
                            double progress = (double) bytesProcessed / totalBytes;
                            Platform.runLater(() -> {
                                CircularProgressRing pi = activeDownloadProgressIndicators.get(fileID);
                                if (pi != null) {
                                    pi.setProgress(progress);
                                }
                            });
                        }

                        @Override
                        public void onTransferComplete() {
                            Platform.runLater(() -> {
                                activeDownloadingFileIds.remove(fileID);
                                activeDownloadProgressIndicators.remove(fileID);
                                restoreFileIcon(fileID);
                            });
                        }

                        @Override
                        public void onTransferError(String error) {
                            Platform.runLater(() -> {
                                activeDownloadingFileIds.remove(fileID);
                                activeDownloadProgressIndicators.remove(fileID);
                                restoreFileIcon(fileID);
                                showAlert("Download Error", "Failed to download file for opening: " + error);
                            });
                        }
                    };

                    CompletableFuture<File> downloadFuture = chunkSender.downloadFile(fileData.getUuidName(),
                            fileData.getOriginalName(), progressCallback);
                    fileToOpen = downloadFuture.get(); // Wait for completion

                    // Cache the downloaded file for future use
                    cacheDownloadedFile(fileData.getUuidName(), fileToOpen);
                }

                // Open the file with system default application
                final File finalFile = fileToOpen;
                Platform.runLater(() -> {
                    try {
                        openFileHelper(finalFile);
                    } catch (Exception e) {
                        showAlert("Open Error", "Failed to open file: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                String fileID = fileData.getUuidName();
                Platform.runLater(() -> {
                    activeDownloadingFileIds.remove(fileID);
                    activeDownloadProgressIndicators.remove(fileID);
                    restoreFileIcon(fileID);
                    showAlert("Download Error", "Failed to download file for opening: " + e.getMessage());
                });
            }
        }).start();
    }

    private void openFileHelper(File file) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", file.getAbsolutePath()).start();
        } else if (os.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
        } else {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                throw new Exception("Desktop operations not supported");
            }
        }
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

            progressContainer.getStyleClass().add("dialog-pane");
            Scene scene = new Scene(progressContainer);
            scene.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
            if (darkModeToggle != null && darkModeToggle.isSelected()) {
                scene.getRoot().getStyleClass().add("dark-mode");
                scene.setFill(Color.web("#17212b"));
            } else {
                scene.setFill(Color.WHITE);
            }
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
        unifiedProgressContainer.managedProperty().bind(unifiedProgressContainer.visibleProperty());

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

    // Active sending file fields
    private final Set<String> activeSendingFileIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, Message> activeSendingMessages = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, CircularProgressRing> activeSendProgressIndicators = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<String> activeDownloadingFileIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, CircularProgressRing> activeDownloadProgressIndicators = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, StackPane> fileIconStacks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, ImageView> fileIconImageViews = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Button> fileDownloadButtons = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Button> fileOpenButtons = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, VBox> activeSendingMessageNodes = new java.util.concurrent.ConcurrentHashMap<>();

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
    private Timeline inWindowTimerTimeline;
    private Timeline inWindowPulseTimeline;

    @FXML
    private void handleVoiceMessage() {
        if (selectedChat == null) {
            showAlert("Error", "Please select a chat to send a voice message.");
            return;
        }
        
        // Build recording bar UI
        HBox recordingBar = createVoiceRecordingBar();
        
        // Get references to components from recording bar
        VBox footerContainer = (VBox) recordingBar.getChildren().get(0);
        HBox innerRow = (HBox) footerContainer.getChildren().get(0);
        Circle indicator = (Circle) innerRow.getChildren().get(0);
        Label timerLabel = (Label) innerRow.getChildren().get(1);
        Label statusLabel = (Label) innerRow.getChildren().get(2);
        
        // Swap UI
        if (mainChatArea != null) {
            mainChatArea.setBottom(recordingBar);
        }
        
        // Start recording
        startInWindowRecording(indicator, timerLabel, statusLabel);
    }

    private HBox createVoiceRecordingBar() {
        HBox recordingBar = new HBox();
        recordingBar.setAlignment(Pos.CENTER);
        
        VBox footerContainer = new VBox();
        footerContainer.setStyle("-fx-padding: 15 20 20 20;");
        footerContainer.getStyleClass().add("chatBoxFooter");
        footerContainer.getStylesheets().add(getClass().getResource("/com/bakbak/javafx_proj_1_2/fxml/ChatWindowStyle.css").toExternalForm());
        HBox.setHgrow(footerContainer, Priority.ALWAYS);
        
        HBox innerRow = new HBox(15);
        innerRow.setAlignment(Pos.CENTER_LEFT);
        
        // Pulse red circle indicator
        Circle indicator = new Circle(6, Color.RED);
        indicator.getStyleClass().add("recording-indicator");
        
        // Timer label
        Label timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        
        // Status text
        Label statusLabel = new Label("Recording...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7c93; -fx-font-family: 'Outfit';");
        if (com.bakbak.javafx_proj_1_2.ChatApplication.isDarkModeEnabled()) {
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a0acba; -fx-font-family: 'Outfit';");
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("voice-control-button", "voice-cancel-button");
        cancelButton.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold; -fx-font-family: 'Outfit';");
        cancelButton.setOnAction(e -> cancelRecording());
        
        // Send button
        Button sendButton = new Button("Send");
        sendButton.getStyleClass().addAll("voice-control-button", "voice-send-button");
        sendButton.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold; -fx-font-family: 'Outfit';");
        sendButton.setOnAction(e -> stopAndSendRecording());
        
        innerRow.getChildren().addAll(indicator, timerLabel, statusLabel, spacer, cancelButton, sendButton);
        footerContainer.getChildren().add(innerRow);
        recordingBar.getChildren().add(footerContainer);
        
        // Timelines
        inWindowTimerTimeline = new Timeline(new KeyFrame(Duration.seconds(0.1), e -> updateRecordingTimer(timerLabel)));
        inWindowTimerTimeline.setCycleCount(Timeline.INDEFINITE);
        
        inWindowPulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(indicator.scaleXProperty(), 1.0),
                new KeyValue(indicator.scaleYProperty(), 1.0),
                new KeyValue(indicator.opacityProperty(), 1.0)
            ),
            new KeyFrame(Duration.seconds(0.5), 
                new KeyValue(indicator.scaleXProperty(), 1.3),
                new KeyValue(indicator.scaleYProperty(), 1.3),
                new KeyValue(indicator.opacityProperty(), 0.5)
            ),
            new KeyFrame(Duration.seconds(1.0), 
                new KeyValue(indicator.scaleXProperty(), 1.0),
                new KeyValue(indicator.scaleYProperty(), 1.0),
                new KeyValue(indicator.opacityProperty(), 1.0)
            )
        );
        inWindowPulseTimeline.setCycleCount(Timeline.INDEFINITE);
        
        return recordingBar;
    }

    private void startInWindowRecording(Circle indicator, Label timerLabel, Label statusLabel) {
        try {
            // Audio format configuration
            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000.0f,  // Sample rate
                16,        // Sample size in bits
                1,         // Channels (mono)
                2,         // Frame size
                16000.0f,  // Frame rate
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

            // Start animations
            inWindowPulseTimeline.play();
            inWindowTimerTimeline.play();

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
                            cancelRecording();
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

    private void cancelRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        if (targetDataLine != null) {
            try {
                targetDataLine.stop();
                targetDataLine.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (inWindowTimerTimeline != null) {
            inWindowTimerTimeline.stop();
        }
        if (inWindowPulseTimeline != null) {
            inWindowPulseTimeline.stop();
        }
        
        // Clean up audio output stream
        audioOutputStream = null;
        
        // Restore input bar
        Platform.runLater(() -> {
            if (mainChatArea != null) {
                mainChatArea.setBottom(messageInputArea);
            }
        });
    }

    private void stopAndSendRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        if (targetDataLine != null) {
            try {
                targetDataLine.stop();
                targetDataLine.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (inWindowTimerTimeline != null) {
            inWindowTimerTimeline.stop();
        }
        if (inWindowPulseTimeline != null) {
            inWindowPulseTimeline.stop();
        }
        
        // Restore input bar
        Platform.runLater(() -> {
            if (mainChatArea != null) {
                mainChatArea.setBottom(messageInputArea);
            }
        });

        // Send
        sendVoiceMessage();
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

    private boolean isVoiceMessage(FileMessageData fileData) {
        if (fileData == null || fileData.getOriginalName() == null) {
            return false;
        }
        String name = fileData.getOriginalName();
        return name.startsWith("voice_message_") && name.endsWith(".wav");
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
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            
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

    private void handleBlockedUsersList(Message message) {
        String content = message.getContent();
        blockedUsers.clear();
        if (content != null && !content.trim().isEmpty()) {
            String[] users = content.split(",");
            for (String u : users) {
                if (!u.trim().isEmpty()) {
                    blockedUsers.add(u.trim());
                }
            }
        }
        System.out.println("DEBUG: Loaded blocked users: " + blockedUsers);
        Platform.runLater(() -> {
            updateChatList();
            if (selectedChat != null && selectedChat.getType() == ChatItem.Type.USER) {
                selectChat(selectedChat);
            }
        });
    }

    private void handleBlockedByUsersMessage(Message message) {
        String blocker = message.getSender();
        if ("SERVER".equals(blocker)) {
            blockedByUsers.clear();
            String content = message.getContent();
            if (content != null && !content.trim().isEmpty()) {
                for (String u : content.split(",")) {
                    blockedByUsers.add(u.trim());
                }
            }
        } else {
            if (blocker != null && !blocker.isEmpty()) {
                // Remove existing entry case-insensitively first to avoid duplicates with different casing
                blockedByUsers.removeIf(u -> u.equalsIgnoreCase(blocker));
                blockedByUsers.add(blocker);
            }
        }
        Platform.runLater(() -> {
            updateChatList();
            if (selectedChat != null && selectedChat.getType() == ChatItem.Type.USER && selectedChat.getName().equalsIgnoreCase(blocker)) {
                selectChat(selectedChat);
            }
        });
    }

    private void handleUnblockedByUsersMessage(Message message) {
        String unblocker = message.getSender();
        if (unblocker != null) {
            blockedByUsers.removeIf(u -> u.equalsIgnoreCase(unblocker));
        }
        Platform.runLater(() -> {
            updateChatList();
            if (selectedChat != null && selectedChat.getType() == ChatItem.Type.USER && selectedChat.getName().equalsIgnoreCase(unblocker)) {
                selectChat(selectedChat);
            }
        });
    }

    private HBox createUnblockBar(String username) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER);
        bar.setSpacing(15);
        bar.setStyle("-fx-padding: 15 20 20 20;");
        bar.getStyleClass().add("unblock-button-bar");

        Label label = new Label("You have blocked this user.");
        label.setStyle("-fx-text-fill: #e06c75; -fx-font-size: 14px; -fx-font-family: 'Outfit'; -fx-font-weight: bold;");

        Button unblockButton = new Button("Unblock");
        unblockButton.getStyleClass().add("unblock-action-button");
        unblockButton.setStyle("-fx-background-color: #e06c75; -fx-text-fill: white; -fx-font-family: 'Outfit'; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
        unblockButton.setOnAction(e -> {
            unblockUser(username);
            blockedUsers.removeIf(u -> u.equalsIgnoreCase(username));
            selectChat(selectedChat);
        });

        bar.getChildren().addAll(label, unblockButton);
        return bar;
    }

    private HBox createBlockedBanner(String username) {
        HBox banner = new HBox();
        banner.setAlignment(Pos.CENTER);
        banner.setStyle("-fx-padding: 20;");
        banner.getStyleClass().add("blocked-banner");

        Label label = new Label("You cannot send messages to this user because they have blocked you.");
        label.setStyle("-fx-text-fill: #abb2bf; -fx-font-size: 14px; -fx-font-family: 'Outfit'; -fx-font-weight: bold;");

        banner.getChildren().add(label);
        return banner;
    }

    private HBox createOptionItem(String text, javafx.event.EventHandler<javafx.scene.input.MouseEvent> onHandler) {
        HBox item = new HBox();
        item.getStyleClass().add("chat-option-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("chat-option-text");

        item.getChildren().add(textLabel);
        item.setOnMouseClicked(onHandler);
        return item;
    }

    private void closeOptionsPanel() {
        if (activeOptionsPanel != null && chatAreaStack != null) {
            chatAreaStack.getChildren().removeAll(activeDismissOverlay, activeOptionsPanel);
            activeOptionsPanel = null;
            activeDismissOverlay = null;
        }
    }

    private void closeEmojiPanel() {
        if (activeEmojiPanel != null && chatAreaStack != null) {
            final VBox panelToClose = activeEmojiPanel;
            final Region overlayToClose = activeEmojiDismissOverlay;
            activeEmojiPanel = null;
            activeEmojiDismissOverlay = null;

            // Fade out overlay
            FadeTransition fadeOverlay = new FadeTransition(Duration.millis(200), overlayToClose);
            fadeOverlay.setToValue(0.0);

            // Slide down and fade out panel
            FadeTransition fadePanel = new FadeTransition(Duration.millis(200), panelToClose);
            fadePanel.setToValue(0.0);

            TranslateTransition slidePanel = new TranslateTransition(Duration.millis(200), panelToClose);
            slidePanel.setToY(30);

            ParallelTransition closeAnim = new ParallelTransition(fadeOverlay, fadePanel, slidePanel);
            closeAnim.setOnFinished(e -> {
                chatAreaStack.getChildren().removeAll(overlayToClose, panelToClose);
            });
            closeAnim.play();
        }
    }

    private void blockUser(String usernameToBlock) {
        try {
            Message message = new Message(Message.MessageType.BLOCK_USER, currentUsername);
            message.setRecipient(usernameToBlock);
            chatClient.sendMessage(message);
            showAlert("User Blocked", "You have blocked " + usernameToBlock);
        } catch (IOException e) {
            System.err.println("Failed to send block request: " + e.getMessage());
        }
    }

    private void unblockUser(String usernameToUnblock) {
        try {
            Message message = new Message(Message.MessageType.UNBLOCK_USER, currentUsername);
            message.setRecipient(usernameToUnblock);
            chatClient.sendMessage(message);
            showAlert("User Unblocked", "You have unblocked " + usernameToUnblock);
        } catch (IOException e) {
            System.err.println("Failed to send unblock request: " + e.getMessage());
        }
    }

    private void deleteGroup(String groupId) {
        try {
            Message message = new Message(Message.MessageType.DELETE_GROUP, currentUsername);
            message.setGroupId(groupId);
            chatClient.sendMessage(message);
        } catch (IOException e) {
            System.err.println("Failed to send delete group request: " + e.getMessage());
        }
    }

    private void showDeleteGroupConfirmationDialog(ChatItem groupItem) {
        showInWindowConfirmationDialog(
            "Delete Group",
            "Are you sure you want to permanently delete this group? This action cannot be undone.",
            () -> deleteGroup(groupItem.getGroupId())
        );
    }

    // Custom circular progress ring showing hollow border spinner
    private static class CircularProgressRing extends StackPane {
        private final javafx.scene.shape.Circle track;
        private final javafx.scene.shape.Arc progressArc;

        public CircularProgressRing() {
            track = new javafx.scene.shape.Circle();
            track.setRadius(12);
            track.setFill(Color.TRANSPARENT);
            track.setStrokeWidth(2.5);
            track.getStyleClass().add("track-circle");
            track.setManaged(false);
            track.setCenterX(16.0);
            track.setCenterY(16.0);

            progressArc = new javafx.scene.shape.Arc();
            progressArc.setRadiusX(12.0f);
            progressArc.setRadiusY(12.0f);
            progressArc.setStartAngle(90.0f);
            progressArc.setLength(0.0f);
            progressArc.setType(javafx.scene.shape.ArcType.OPEN);
            progressArc.setFill(Color.TRANSPARENT);
            progressArc.setStrokeWidth(2.5);
            progressArc.getStyleClass().add("progress-arc");
            progressArc.setManaged(false);
            progressArc.setCenterX(16.0);
            progressArc.setCenterY(16.0);

            getChildren().addAll(track, progressArc);
            getStyleClass().add("circular-progress-ring");
            
            setPrefSize(32, 32);
            setMinSize(32, 32);
            setMaxSize(32, 32);
        }

        public void setProgress(double progress) {
            progressArc.setLength(-360.0 * Math.max(0.0, Math.min(1.0, progress)));
        }
    }
}
