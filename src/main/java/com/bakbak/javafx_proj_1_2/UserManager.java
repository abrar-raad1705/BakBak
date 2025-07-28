package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class UserManager {
    private static UserManager instance;
    private Map<String, User> users;
    private Map<String, ClientHandler> onlineUsers;
    private final String DATA_DIR = "chat_data";
    private final String USERS_FILE = DATA_DIR + "/users.txt";
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private UserManager() {
        System.out.println("UserManager constructor called");
        this.users = new ConcurrentHashMap<>();
        this.onlineUsers = new ConcurrentHashMap<>();
        initializeDataDirectory();
        loadUsers();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }
    
    private void initializeDataDirectory() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }
    
    private void loadUsers() {
        try {
            Path usersPath = Paths.get(USERS_FILE);
            if (Files.exists(usersPath)) {
                for (String line : Files.readAllLines(usersPath)) {
                    if (line.trim().isEmpty()) continue;
                    
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        String username = parts[0];
                        String password = parts[1];
                        User user = new User(username, password);
                        
                        // Load last seen timestamp if available
                        if (parts.length >= 3 && !parts[2].isEmpty()) {
                            try {
                                LocalDateTime lastSeen = LocalDateTime.parse(parts[2], formatter);
                                user.setLastSeenTimestamp(lastSeen);
                            } catch (Exception e) {
                                System.err.println("Error parsing last seen time for user " + username + ": " + e.getMessage());
                            }
                        }
                        
                        users.put(username, user);
                    }
                }
                System.out.println("Loaded " + users.size() + " users from file");
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }
    
    private void saveUsers() {
        try {
            Path usersPath = Paths.get(USERS_FILE);
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(usersPath))) {
                for (User user : users.values()) {
                    String lastSeenStr = user.getLastSeenTimestamp() != null ? 
                        user.getLastSeenTimestamp().format(formatter) : "";
                    writer.println(user.getUsername() + "|" + user.getPassword() + "|" + lastSeenStr);
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }
        
        User newUser = new User(username, password);
        users.put(username, newUser);
        saveUsers();
        System.out.println("User " + username + " registered and saved to file");
        return true;  
    }
    
    public boolean loginUser(String username, String password, ClientHandler clientHandler) {
        System.out.println("Login attempt for user: " + username);
        
        User user = users.get(username);
        if (user == null) {
            System.out.println("Users loaded: " + users.keySet());
            System.out.println("User not found: " + username);
            return false;
        }
        
        if (!user.getPassword().equals(password)) {
            System.out.println("Password mismatch for user: " + username);
            return false;
        }
        
        if (onlineUsers.containsKey(username)) {
            System.out.println("User already online: " + username);
            return false;
        }
        
        user.setOnline(true);
        onlineUsers.put(username, clientHandler);
        saveUsers();
        
        // Broadcast login status to all connected clients
        broadcastUserStatusUpdate(username, true);
        
        System.out.println("User " + username + " logged in successfully");
        return true;
    }
    
    public void logoutUser(String username) {
        User user = users.get(username);
        if (user != null) {
            user.setOnline(false);
            user.updateLastSeen(); // Update logout timestamp
            onlineUsers.remove(username);
            saveUsers();
            
            // Broadcast logout status to all connected clients
            broadcastUserStatusUpdate(username, false);
            
            System.out.println("User " + username + " logged out at " + user.getLastSeenTimestamp());
        }
    }
    
    private void broadcastUserStatusUpdate(String username, boolean isOnline) {
        User user = users.get(username);
        if (user == null) return;
        
        Message statusUpdate = new Message(Message.MessageType.USER_STATUS_UPDATE, "SERVER");
        statusUpdate.setContent(username + "|" + isOnline + "|" + user.getLastSeenStatus());
        
        // Send to all connected clients except the user who just logged in/out
        for (Map.Entry<String, ClientHandler> entry : onlineUsers.entrySet()) {
            if (!entry.getKey().equals(username)) {
                entry.getValue().sendMessage(statusUpdate);
            }
        }
        
        System.out.println("DEBUG: Broadcasted status update for " + username + " (online: " + isOnline + ") to " + 
                          (onlineUsers.size() - (isOnline ? 1 : 0)) + " clients");
    }
    
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }
    
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers.keySet());
    }
    
    public Set<String> getAllUsers() {
        return new HashSet<>(users.keySet());
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public ClientHandler getClientHandler(String username) {
        return onlineUsers.get(username);
    }
    
    public String getUserStatus(String username) {
        User user = users.get(username);
        return user != null ? user.getLastSeenStatus() : "User not found";
    }
} 