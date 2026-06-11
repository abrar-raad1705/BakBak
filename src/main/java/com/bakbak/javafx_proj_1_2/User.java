package com.bakbak.javafx_proj_1_2;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private boolean isOnline;
    private LocalDateTime lastSeenTimestamp;
    private Set<String> contacts;
    private Set<String> groups;
    private Set<String> blockedUsers;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.isOnline = false;
        this.lastSeenTimestamp = LocalDateTime.now(); // Initialize with creation time
        this.contacts = new HashSet<>();
        this.groups = new HashSet<>();
        this.blockedUsers = new HashSet<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
        if (!online) {
            // Update last seen timestamp when going offline
            this.lastSeenTimestamp = LocalDateTime.now();
        }
    }

    public LocalDateTime getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public void setLastSeenTimestamp(LocalDateTime lastSeenTimestamp) {
        this.lastSeenTimestamp = lastSeenTimestamp;
    }

    public void updateLastSeen() {
        this.lastSeenTimestamp = LocalDateTime.now();
    }

    public String getLastSeenStatus() {
        if (isOnline) {
            return "Online";
        }
        
        if (lastSeenTimestamp == null) {
            return "Last seen recently";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = ChronoUnit.MINUTES.between(lastSeenTimestamp, now);
        long hoursAgo = ChronoUnit.HOURS.between(lastSeenTimestamp, now);
        long daysAgo = ChronoUnit.DAYS.between(lastSeenTimestamp, now);
        
        if (minutesAgo < 5) {
            return "Last seen recently";
        } else if (minutesAgo < 60) {
            return "Last seen " + minutesAgo + " minute" + (minutesAgo == 1 ? "" : "s") + " ago";
        } else if (hoursAgo < 24) {
            return "Last seen " + hoursAgo + " hour" + (hoursAgo == 1 ? "" : "s") + " ago";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
            return "Last seen " + lastSeenTimestamp.format(formatter);
        }
    }

    public Set<String> getContacts() {
        return contacts;
    }

    public void setContacts(Set<String> contacts) {
        this.contacts = contacts;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public void addContact(String contactUsername) {
        this.contacts.add(contactUsername);
    }

    public void removeContact(String contactUsername) {
        this.contacts.remove(contactUsername);
    }

    public void joinGroup(String groupId) {
        this.groups.add(groupId);
    }

    public void leaveGroup(String groupId) {
        this.groups.remove(groupId);
    }

    public Set<String> getBlockedUsers() {
        if (blockedUsers == null) {
            blockedUsers = new HashSet<>();
        }
        return blockedUsers;
    }

    public void setBlockedUsers(Set<String> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    public void blockUser(String userToBlock) {
        if (blockedUsers == null) {
            blockedUsers = new HashSet<>();
        }
        this.blockedUsers.add(userToBlock);
    }

    public void unblockUser(String userToUnblock) {
        if (blockedUsers == null) {
            blockedUsers = new HashSet<>();
        }
        this.blockedUsers.remove(userToUnblock);
    }

    public boolean isUserBlocked(String targetUser) {
        if (blockedUsers == null) {
            return false;
        }
        return this.blockedUsers.contains(targetUser);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", isOnline=" + isOnline +
                ", lastSeen=" + (lastSeenTimestamp != null ? lastSeenTimestamp : "never") +
                ", contacts=" + contacts +
                ", groups=" + groups +
                '}';
    }
}