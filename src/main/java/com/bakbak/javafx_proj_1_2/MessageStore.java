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
    
    private void addMessageToUserHistory(String username, Message message) {
        userMessageHistory.computeIfAbsent(username, k -> new ArrayList<>()).add(message);
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
                .filter(msg -> (msg.getSender().equals(user1) && msg.getRecipient().equals(user2)) ||
                              (msg.getSender().equals(user2) && msg.getRecipient().equals(user1)))
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
        return String.join("|",
                message.getType().toString(),
                message.getSender() != null ? message.getSender() : "",
                message.getRecipient() != null ? message.getRecipient() : "",
                message.getContent() != null ? message.getContent() : "",
                message.getGroupId() != null ? message.getGroupId() : "",
                message.getTimestamp().format(formatter),
                String.valueOf(message.isSuccess())
/*                 message.getErrorMessage() != null ? message.getErrorMessage() : "" */
        );
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
                
                // if (parts.length > 7 && !parts[7].isEmpty()) message.setErrorMessage(parts[7]);
                
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
} 