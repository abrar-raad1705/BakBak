package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageStore {
    private static MessageStore instance;
    private final String DATA_DIR = "chat_data";
    private final String MESSAGES_DIR = DATA_DIR + "/messages";
    // private final String USERS_FILE = DATA_DIR + "/users.txt";
    private final String OFFLINE_QUEUE_DIR = DATA_DIR + "/offline_queue";
    
    private Map<String, List<Message>> userMessageHistory;
    private Map<String, Queue<Message>> offlineMessageQueue;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private MessageStore() {
        this.userMessageHistory = new ConcurrentHashMap<>();
        this.offlineMessageQueue = new ConcurrentHashMap<>();
        initializeDataDirectories();
        loadAllData();
    }
    
    public static synchronized MessageStore getInstance() {
        if (instance == null) {
            instance = new MessageStore();
        }
        return instance;
    }
    
    private void initializeDataDirectories() {
        try {
            Files.createDirectories(Paths.get(MESSAGES_DIR));
            Files.createDirectories(Paths.get(OFFLINE_QUEUE_DIR));
            Files.createDirectories(Paths.get(DATA_DIR + "/shared_files"));
        } catch (IOException e) {
            System.err.println("Error creating data directories: " + e.getMessage());
        }
    }
    
    public synchronized void storeMessage(Message message) {
        // Store message for both sender and recipient
        String sender = message.getSender();
        String recipient = message.getRecipient();
        
        System.out.println("DEBUG: Storing message from " + sender + " to " + recipient + ": " + message.getContent());
        
        if (sender != null && recipient != null) {
            // Add to sender's history
            addMessageToUserHistory(sender, message);
            
            // Add to recipient's history
            addMessageToUserHistory(recipient, message);
            
            System.out.println("DEBUG: Sender " + sender + " now has " + 
                userMessageHistory.get(sender).size() + " messages in memory");
            System.out.println("DEBUG: Recipient " + recipient + " now has " + 
                userMessageHistory.get(recipient).size() + " messages in memory");
            
            // Save to files
            saveUserMessages(sender);
            saveUserMessages(recipient);
        }
    }
    
    public synchronized void storeGroupMessage(Message message, Set<String> groupMembers) {
        // Store group message for all group members
        String sender = message.getSender();
        String groupId = message.getGroupId();
        
        System.out.println("DEBUG: Storing group message from " + sender + " to group " + groupId + ": " + message.getContent());
        
        if (sender != null && groupId != null && groupMembers != null) {
            for (String member : groupMembers) {
                // Add to each member's history (including sender)
                addMessageToUserHistory(member, message);
                
                // Save to files
                saveUserMessages(member);
            }
            
            System.out.println("DEBUG: Stored group message for " + groupMembers.size() + " members");
        }
    }
    
    private void addMessageToUserHistory(String username, Message message) {
        try {
            if (message.getType() == Message.MessageType.FILE_MESSAGE) {
                // Ensure the content is properly set for file messages
                if (message.getContent() == null || message.getContent().isEmpty()) {
                    if (message.getFileMessageData() != null) {
                        message.setContent(message.getFileMessageData().toString());
                        System.out.println("DEBUG: Fixed empty content for FILE_MESSAGE using FileMessageData");
                    } else {
                        System.err.println("WARNING: FILE_MESSAGE has no content and no FileMessageData for user " + username);
                        return; // Skip saving corrupted file messages
                    }
                }
            }
            
            userMessageHistory.computeIfAbsent(username, k -> new ArrayList<>()).add(message);
            saveUserMessages(username);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to add message to user history for " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public synchronized void queueOfflineMessage(String recipient, Message message) {
        offlineMessageQueue.computeIfAbsent(recipient, k -> new LinkedList<>()).offer(message);
        saveOfflineQueue(recipient);
    }
    
    public synchronized List<Message> getOfflineMessages(String username) {
        Queue<Message> queue = offlineMessageQueue.get(username);
        if (queue == null || queue.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>(queue);
        queue.clear();
        offlineMessageQueue.remove(username);
        
        // Delete the offline queue file
        deleteOfflineQueueFile(username);
        
        return messages;
    }
    
    public synchronized List<Message> getUserMessageHistory(String username) {
        List<Message> history = userMessageHistory.get(username);
        return history != null ? new ArrayList<>(history) : new ArrayList<>();
    }
    
    public synchronized List<Message> getConversationHistory(String user1, String user2) {
        List<Message> user1Messages = getUserMessageHistory(user1);
        return user1Messages.stream()
                .filter(msg -> (msg.getType() == Message.MessageType.PRIVATE_MESSAGE || 
                               msg.getType() == Message.MessageType.FILE_MESSAGE) &&
                              msg.getRecipient() != null && msg.getSender() != null && // Add null checks
                              ((msg.getSender().equals(user1) && msg.getRecipient().equals(user2)) ||
                               (msg.getSender().equals(user2) && msg.getRecipient().equals(user1))))
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .toList();
    }
    
    public synchronized List<Message> getGroupConversationHistory(String username, String groupId) {
        List<Message> userMessages = getUserMessageHistory(username);
        return userMessages.stream()
                .filter(msg -> (msg.getType() == Message.MessageType.GROUP_MESSAGE || 
                               msg.getType() == Message.MessageType.FILE_MESSAGE) && 
                              msg.getGroupId() != null && msg.getSender() != null && // Add null checks
                              groupId.equals(msg.getGroupId()))
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .toList();
    }
    
    private void saveUserMessages(String username) {
        try {
            Path userFile = Paths.get(MESSAGES_DIR, username + ".txt");
            List<Message> messages = userMessageHistory.get(username);
            
            if (messages != null && !messages.isEmpty()) {
                // Sort messages by timestamp to ensure chronological order
                messages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
                
                // Write all messages (atomic operation)
                Path tempFile = Paths.get(MESSAGES_DIR, username + ".tmp");
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(tempFile, 
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    for (Message message : messages) {
                        writer.println(messageToString(message));
                    }
                    writer.flush();
                } 
                
                // Atomic move from temp to final file
                Files.move(tempFile, userFile, StandardCopyOption.REPLACE_EXISTING);
                
                System.out.println("DEBUG: Saved " + messages.size() + " messages for user " + username);
            } else {
                System.out.println("DEBUG: No messages to save for user " + username);
            }
        } catch (IOException e) {
            System.err.println("Error saving messages for user " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveOfflineQueue(String username) {
        try {
            Path queueFile = Paths.get(OFFLINE_QUEUE_DIR, username + ".txt");
            Queue<Message> queue = offlineMessageQueue.get(username);
            
            if (queue != null && !queue.isEmpty()) {
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(queueFile))) {
                    for (Message message : queue) {
                        writer.println(messageToString(message));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving offline queue for user " + username + ": " + e.getMessage());
        }
    }
    
    private void loadAllData() {
        System.out.println("DEBUG: MessageStore loading all data from files...");
        loadUserMessages();
        loadOfflineQueues();
        System.out.println("DEBUG: MessageStore finished loading data. Total users with messages: " + userMessageHistory.size());
    }
    
    private void loadUserMessages() {
        try {
            Path messagesPath = Paths.get(MESSAGES_DIR);
            if (Files.exists(messagesPath)) {
                Files.list(messagesPath)
                        .filter(path -> path.toString().endsWith(".txt"))
                        .forEach(this::loadUserMessagesFromFile);
            }
        } catch (IOException e) {
            System.err.println("Error loading user messages: " + e.getMessage());
        }
    }
    
    private void loadUserMessagesFromFile(Path userFile) {
        try {
            String username = userFile.getFileName().toString().replace(".txt", "");
            List<Message> messages = new ArrayList<>();
            
            List<String> lines = Files.readAllLines(userFile);
            System.out.println("DEBUG: Loading " + lines.size() + " lines from " + userFile.getFileName());
            
            for (String line : lines) {
                if (line.trim().isEmpty()) continue; // Skip empty lines
                
                Message message = stringToMessage(line);
                if (message != null) {
                    messages.add(message);
                } else {
                    System.err.println("DEBUG: Failed to parse message line: " + line);
                }
            }
            
            // Sort messages by timestamp to ensure chronological order
            messages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
            
            userMessageHistory.put(username, messages);
            System.out.println("DEBUG: Loaded " + messages.size() + " messages for user " + username);
            
        } catch (IOException e) {
            System.err.println("Error loading messages from file " + userFile + ": " + e.getMessage());
        }
    }
    
    private void loadOfflineQueues() {
        try {
            Path offlinePath = Paths.get(OFFLINE_QUEUE_DIR);
            if (Files.exists(offlinePath)) {
                Files.list(offlinePath)
                        .filter(path -> path.toString().endsWith(".txt"))
                        .forEach(this::loadOfflineQueueFromFile);
            }
        } catch (IOException e) {
            System.err.println("Error loading offline queues: " + e.getMessage());
        }
    }
    
    private void loadOfflineQueueFromFile(Path queueFile) {
        try {
            String username = queueFile.getFileName().toString().replace(".txt", "");
            Queue<Message> queue = new LinkedList<>();
            
            List<String> lines = Files.readAllLines(queueFile);
            for (String line : lines) {
                Message message = stringToMessage(line);
                if (message != null) {
                    queue.offer(message);
                }
            }
            
            if (!queue.isEmpty()) {
                offlineMessageQueue.put(username, queue);
            }
        } catch (IOException e) {
            System.err.println("Error loading offline queue from file " + queueFile + ": " + e.getMessage());
        }
    }
    
    private void deleteOfflineQueueFile(String username) {
        try {
            Path queueFile = Paths.get(OFFLINE_QUEUE_DIR, username + ".txt");
            Files.deleteIfExists(queueFile);
        } catch (IOException e) {
            System.err.println("Error deleting offline queue file for " + username + ": " + e.getMessage());
        }
    }
    
    private String messageToString(Message message) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("|",
                message.getType().toString(),
                message.getSender() != null ? message.getSender() : "",
                message.getRecipient() != null ? message.getRecipient() : "",
                message.getContent() != null ? message.getContent() : "",
                message.getGroupId() != null ? message.getGroupId() : "",
                message.getTimestamp().format(formatter),
                String.valueOf(message.isSuccess())
        ));
        
        // For FILE_MESSAGE types, the FileMessageData is already in the content field
        // No need to add it again as a separate field
        
        return sb.toString();
    }
    
    private Message stringToMessage(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length >= 7) { // Changed from 8 to 7 since we have 7 parts
                Message message = new Message(Message.MessageType.valueOf(parts[0]), parts[1]);
                
                if (!parts[2].isEmpty()) message.setRecipient(parts[2]);
                if (!parts[3].isEmpty()) message.setContent(parts[3]);
                if (!parts[4].isEmpty()) message.setGroupId(parts[4]);
                
                message.setTimestamp(LocalDateTime.parse(parts[5], formatter));
                message.setSuccess(Boolean.parseBoolean(parts[6]));
                
                // Handle FILE_MESSAGE types - parse FileMessageData from content
                if (message.getType() == Message.MessageType.FILE_MESSAGE && !parts[3].isEmpty()) {
                    try {
                        System.out.println("DEBUG: Parsing FILE_MESSAGE content: " + parts[3]);
                        FileMessageData fileData = FileMessageData.fromString(parts[3]);
                        message.setFileMessageData(fileData);
                        System.out.println("DEBUG: Successfully parsed FileMessageData: " + fileData.getOriginalName());
                    } catch (Exception e) {
                        System.err.println("Error parsing FileMessageData from content: " + parts[3] + " - " + e.getMessage());
                        return null; // Skip corrupted file messages
                    }
                } else if (message.getType() == Message.MessageType.FILE_MESSAGE && parts[3].isEmpty()) {
                    System.err.println("ERROR: FILE_MESSAGE with empty content found: " + line);
                    return null; // Skip corrupted file messages
                }
                
                return message;
            } else {
                System.err.println("DEBUG: Invalid message format - expected at least 7 parts, got " + parts.length + ": " + line);
            }
        } catch (Exception e) {
            System.err.println("Error parsing message from string: " + line + " - " + e.getMessage());
        }
        return null;
    }
    
    public synchronized boolean hasOfflineMessages(String username) {
        Queue<Message> queue = offlineMessageQueue.get(username);
        return queue != null && !queue.isEmpty();
    }
    
    public synchronized int getOfflineMessageCount(String username) {
        Queue<Message> queue = offlineMessageQueue.get(username);
        return queue != null ? queue.size() : 0;
    }
    
    public synchronized void deletePrivateChatHistory(String user1, String user2) {
        // This will delete the conversation from both users' history files
        removeMessagesFromUser(user1, msg -> 
            msg.getType() == Message.MessageType.PRIVATE_MESSAGE &&
            ((msg.getSender().equals(user1) && msg.getRecipient().equals(user2)) ||
             (msg.getSender().equals(user2) && msg.getRecipient().equals(user1)))
        );
        removeMessagesFromUser(user2, msg -> 
            msg.getType() == Message.MessageType.PRIVATE_MESSAGE &&
            ((msg.getSender().equals(user1) && msg.getRecipient().equals(user2)) ||
             (msg.getSender().equals(user2) && msg.getRecipient().equals(user1)))
        );
        System.out.println("DEBUG: Deleted private chat history between " + user1 + " and " + user2);
    }

    public synchronized void deleteGroupChatHistoryForUser(String username, String groupId) {
        // This only removes group messages from a specific user's history file
        removeMessagesFromUser(username, msg ->
            msg.getType() == Message.MessageType.GROUP_MESSAGE &&
            groupId.equals(msg.getGroupId())
        );
        System.out.println("DEBUG: Deleted group chat history for user " + username + " in group " + groupId);
    }

    private synchronized void removeMessagesFromUser(String username, java.util.function.Predicate<Message> filter) {
        if (userMessageHistory.containsKey(username)) {
            List<Message> userMessages = userMessageHistory.get(username);
            userMessages.removeIf(filter);
            
            // After removing from memory, save the updated list to the file
            saveUserMessages(username);
        }
    }
} 