package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.nio.file.*;
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
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        String username = parts[0];
                        String password = parts[1];
                        User user = new User(username, password);
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
                    writer.println(user.getUsername() + "|" + user.getPassword());
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
        return true;
    }
    
    public void logoutUser(String username) {
        User user = users.get(username);
        if (user != null) {
            user.setOnline(false);
            onlineUsers.remove(username);
        }
    }
    
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }
    
    public ClientHandler getClientHandler(String username) {
        return onlineUsers.get(username);
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers.keySet());
    }
    
    public Set<String> getAllUsers() {
        return new HashSet<>(users.keySet());
    }
    
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
    
    public void addContact(String username, String contactUsername) {
        User user = users.get(username);
        if (user != null && users.containsKey(contactUsername)) {
            user.addContact(contactUsername);
        }
    }
    
    public void removeContact(String username, String contactUsername) {
        User user = users.get(username);
        if (user != null) {
            user.removeContact(contactUsername);
        }
    }
    
    public Set<String> getUserContacts(String username) {
        User user = users.get(username);
        return user != null ? user.getContacts() : new HashSet<>();
    }
} 