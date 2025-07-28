package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
                        
                        // Load groups if available
                        if (parts.length >= 4 && !parts[3].isEmpty() && !parts[3].equals("GROUPS:")) {
                            String groupsStr = parts[3];
                            if (groupsStr.startsWith("GROUPS:")) {
                                groupsStr = groupsStr.substring(7); // Remove "GROUPS:"
                                if (!groupsStr.trim().isEmpty()) {
                                    String[] groupArray = groupsStr.split(",");
                                    for (String groupId : groupArray) {
                                        if (!groupId.trim().isEmpty()) {
                                            user.joinGroup(groupId.trim());
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Load contacts if available
                        if (parts.length >= 5 && !parts[4].isEmpty() && !parts[4].equals("CONTACTS:")) {
                            String contactsStr = parts[4];
                            if (contactsStr.startsWith("CONTACTS:")) {
                                contactsStr = contactsStr.substring(9); // Remove "CONTACTS:"
                                if (!contactsStr.trim().isEmpty()) {
                                    String[] contactArray = contactsStr.split(",");
                                    for (String contact : contactArray) {
                                        if (!contact.trim().isEmpty()) {
                                            user.addContact(contact.trim());
                                        }
                                    }
                                }
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
                    StringBuilder userLine = new StringBuilder();
                    userLine.append(user.getUsername()).append("|");
                    userLine.append(user.getPassword()).append("|");
                    
                    // Last seen timestamp
                    String lastSeenStr = user.getLastSeenTimestamp() != null ? 
                        user.getLastSeenTimestamp().format(formatter) : "";
                    userLine.append(lastSeenStr).append("|");
                    
                    // Groups
                    userLine.append("GROUPS:");
                    for (String groupId : user.getGroups()) {
                        userLine.append(groupId).append(",");
                    }
                    userLine.append("|");
                    
                    // Contacts
                    userLine.append("CONTACTS:");
                    for (String contact : user.getContacts()) {
                        userLine.append(contact).append(",");
                    }
                    
                    writer.println(userLine.toString());
                }
            }
            System.out.println("DEBUG: Saved " + users.size() + " users to file with group memberships");
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
        
        // Also broadcast group status updates for all groups this user belongs to
        broadcastGroupStatusUpdates(username);
        
        System.out.println("DEBUG: Broadcasted status update for " + username + " (online: " + isOnline + ") to " + 
                          (onlineUsers.size() - (isOnline ? 1 : 0)) + " clients");
    }
    
    private void broadcastGroupStatusUpdates(String username) {
        GroupManager groupManager = GroupManager.getInstance();
        Set<String> userGroups = groupManager.getUserGroups(username);
        
        for (String groupId : userGroups) {
            Group group = groupManager.getGroup(groupId);
            if (group != null) {
                // Send updated group info to all online group members
                for (String member : group.getMembers()) {
                    if (isUserOnline(member)) {
                        ClientHandler memberHandler = getClientHandler(member);
                        if (memberHandler != null) {
                            sendDetailedGroupInfo(group, memberHandler);
                        }
                    }
                }
            }
        }
    }
    
    private void sendDetailedGroupInfo(Group group, ClientHandler clientHandler) {
        // Collect detailed group information
        StringBuilder groupInfo = new StringBuilder();
        groupInfo.append("GROUP_DETAILS:");
        groupInfo.append(group.getGroupName()).append("|");
        groupInfo.append(group.getGroupId()).append("|");
        groupInfo.append(group.getCreator()).append("|");
        
        // Add members list
        Set<String> members = group.getMembers();
        groupInfo.append("MEMBERS:");
        for (String member : members) {
            groupInfo.append(member).append(",");
        }
        groupInfo.append("|");
        
        // Add admins list
        Set<String> admins = group.getAdmins();
        groupInfo.append("ADMINS:");
        for (String admin : admins) {
            groupInfo.append(admin).append(",");
        }
        groupInfo.append("|");
        
        // Add online members list
        groupInfo.append("ONLINE:");
        for (String member : members) {
            if (isUserOnline(member)) {
                groupInfo.append(member).append(",");
            }
        }
        
        Message response = new Message(Message.MessageType.GROUP_LIST, "SERVER");
        response.setContent(groupInfo.toString());
        response.setGroupId(group.getGroupId());
        clientHandler.sendMessage(response);
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