package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class GroupManager {
    private static GroupManager instance;
    private Map<String, Group> groups;
    private final String DATA_DIR = "chat_data";
    private final String GROUPS_FILE = DATA_DIR + "/groups.txt";

    private GroupManager() {
        this.groups = new ConcurrentHashMap<>();
        initializeDataDirectory();
        loadGroups();
    }

    public static synchronized GroupManager getInstance() {
        if (instance == null) {
            instance = new GroupManager();
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

    private void loadGroups() {
        try {
            Path groupsPath = Paths.get(GROUPS_FILE);
            if (Files.exists(groupsPath)) {
                for (String line : Files.readAllLines(groupsPath)) {
                    if (line.trim().isEmpty()) continue;
                    
                    Group group = stringToGroup(line);
                    if (group != null) {
                        groups.put(group.getGroupId(), group);
                        System.out.println("DEBUG: Loaded group: " + group.getGroupName() + " (ID: " + group.getGroupId() + ")");
                    }
                }
                System.out.println("Loaded " + groups.size() + " groups from file");
            }
        } catch (IOException e) {
            System.err.println("Error loading groups: " + e.getMessage());
        }
    }

    public void saveGroups() {
        try {
            Path groupsFile = Paths.get(GROUPS_FILE);
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(groupsFile, 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
                for (Group group : groups.values()) {
                    writer.println(groupToString(group));
                }
                writer.flush();
            }
            System.out.println("DEBUG: Saved " + groups.size() + " groups to file");
        } catch (IOException e) {
            System.err.println("Error saving groups: " + e.getMessage());
        }
    }

    private String groupToString(Group group) {
        StringBuilder sb = new StringBuilder();
        sb.append(group.getGroupId()).append("|");
        sb.append(group.getGroupName()).append("|");
        sb.append(group.getCreator()).append("|");
        
        // Members
        sb.append("MEMBERS:");
        for (String member : group.getMembers()) {
            sb.append(member).append(",");
        }
        sb.append("|");
        
        // Admins
        sb.append("ADMINS:");
        for (String admin : group.getAdmins()) {
            sb.append(admin).append(",");
        }
        
        return sb.toString();
    }

    private Group stringToGroup(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length >= 5) {
                String groupId = parts[0];
                String groupName = parts[1];
                String creator = parts[2];
                
                // Create a new group without auto-adding creator (we'll add members explicitly)
                Group group = new Group(groupId, groupName, creator);
                // Clear the auto-added creator from constructor
                group.getMembers().clear();
                group.getAdmins().clear();
                
                // Parse members
                if (parts[3].startsWith("MEMBERS:")) {
                    String membersStr = parts[3].substring(8); // Remove "MEMBERS:"
                    if (!membersStr.trim().isEmpty()) {
                        String[] memberArray = membersStr.split(",");
                        for (String member : memberArray) {
                            if (!member.trim().isEmpty()) {
                                group.addMember(member.trim());
                            }
                        }
                    }
                }
                
                // Parse admins
                if (parts[4].startsWith("ADMINS:")) {
                    String adminsStr = parts[4].substring(7); // Remove "ADMINS:"
                    if (!adminsStr.trim().isEmpty()) {
                        String[] adminArray = adminsStr.split(",");
                        for (String admin : adminArray) {
                            if (!admin.trim().isEmpty()) {
                                group.addAdmin(admin.trim());
                            }
                        }
                    }
                }
                
                return group;
            }
        } catch (Exception e) {
            System.err.println("Error parsing group line: " + line + " - " + e.getMessage());
        }
        return null;
    }

    public String createGroup(String groupName, String creator) {
        String groupId = UUID.randomUUID().toString();
        Group newGroup = new Group(groupId, groupName, creator);
        groups.put(groupId, newGroup);
        saveGroups(); // Save immediately after creating a group

        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(creator);
        if (user != null) {
            user.joinGroup(groupId);
            userManager.saveUsers();
        }
        return groupId;
    }

    public boolean joinGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group == null) {
            return false;
        }

        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(username);
        /*
         * if (user == null) {
         * return false; // User doesn't exist
         * }
         */
        group.addMember(username);
        if (user != null) {
            user.joinGroup(groupId);
            userManager.saveUsers();
        }
        saveGroups(); // Save immediately after joining a group
        return true;
    }

    public boolean leaveGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        /*
         * if (group == null || !group.isMember(username)) {
         * return false;
         * }
         */
        if (group.getCreator().equals(username)) {
            return false;
        }
        group.removeMember(username);

        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(username);
        if (user != null) {
            user.leaveGroup(groupId);
            userManager.saveUsers();
        }
        saveGroups(); // Save immediately after leaving a group
        return true;
    }

    public boolean deleteGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (/* group == null || */!group.getCreator().equals(username)) {
            return false;
        }

        UserManager userManager = UserManager.getInstance();
        for (String member : group.getMembers()) {
            User user = userManager.getUser(member);
            if (user != null) {
                user.leaveGroup(groupId);
            }
        }
        userManager.saveUsers();

        groups.remove(groupId);
        saveGroups(); // Save immediately after deleting a group
        return true;
    }

    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public Set<String> getGroupMembers(String groupId) {
        Group group = groups.get(groupId);
        return group != null ? group.getMembers() : new HashSet<>();
    }

    public Set<String> getUserGroups(String username) {
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(username);
        return user != null ? user.getGroups() : new HashSet<>();
    }

    public Set<String> getAllGroups() {
        return new HashSet<>(groups.keySet());
    }

    public boolean isGroupMember(String groupId, String username) {
        Group group = groups.get(groupId);
        return group != null && group.isMember(username);
    }

    public boolean isGroupAdmin(String groupId, String username) {
        Group group = groups.get(groupId);
        return group != null && group.isAdmin(username);
    }

    public boolean addAdmin(String groupId, String adminUsername, String newAdminUsername) {
        Group group = groups.get(groupId);
        if (!group.isAdmin(adminUsername)) {
            return false;
        }

        if (!group.isMember(newAdminUsername)) {
            return false;
        }

        group.addAdmin(newAdminUsername);
        saveGroups(); // Save immediately after adding an admin
        return true;
    }

    public boolean removeAdmin(String groupId, String adminUsername, String targetUsername) {
        Group group = groups.get(groupId);
        if (!group.isAdmin(adminUsername)) {
            return false;
        }

        // !As only creator should be able to remove other admins
        if (!group.getCreator().equals(adminUsername)) {
            return false;
        }

        group.removeAdmin(targetUsername);
        saveGroups(); // Save immediately after removing an admin
        return true;
    }

    public boolean removeMember(String groupId, String adminUsername, String targetMember){
        Group group = groups.get(groupId);
        if(!group.isAdmin(adminUsername)){
            return false;
        }

        group.removeMember(targetMember);
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(targetMember);
        if (user != null) {
            user.leaveGroup(groupId);
            userManager.saveUsers();
        }
        saveGroups(); // Save immediately after removing a member
        return true;
    }

    public boolean addMember(String groupId, String memberUsername, String targetMember){
        Group group = groups.get(groupId);
        if(!group.isMember(memberUsername)){
            return false;
        }

        group.addMember(targetMember);
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(targetMember);
        if (user != null) {
            user.joinGroup(groupId);
            userManager.saveUsers();
        }
        saveGroups(); // Save immediately after adding a member
        return true;
    }
    
    public boolean promoteToAdmin(String groupId, String requestingUser, String targetUser) {
        Group group = groups.get(groupId);
        if (group == null || !group.isCreator(requestingUser) || !group.isMember(targetUser)) {
            return false;
        }
        group.addAdmin(targetUser);
        saveGroups();
        return true;
    }

    public boolean demoteAdmin(String groupId, String requestingUser, String targetUser) {
        Group group = groups.get(groupId);
        if (group == null || !group.isCreator(requestingUser) || !group.isAdmin(targetUser) || group.isCreator(targetUser)) {
            return false;
        }
        group.removeAdmin(targetUser);
        saveGroups();
        return true;
    }
}