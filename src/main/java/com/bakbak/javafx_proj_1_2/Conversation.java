package com.bakbak.javafx_proj_1_2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private String contactUsername;
    private List<Message> messages;
    private int unreadCount;
    private LocalDateTime lastActivity;
    private boolean isOnline;

    public Conversation(String contactUsername) {
        this.contactUsername = contactUsername;
        this.messages = new ArrayList<>();
        this.unreadCount = 0;
        this.lastActivity = LocalDateTime.now();
        this.isOnline = false;
    }

    public void addMessage(Message message) {
        messages.add(message);
        lastActivity = message.getTimestamp();
        
        // If it's an incoming message (not from us), increment unread count
        if (!message.getSender().equals(getCurrentUser())) {
            unreadCount++;
        }
    }

    public String getContactUsername() {
        return contactUsername;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void markAsRead() {
        unreadCount = 0;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public String getLastMessagePreview() {
        if (messages.isEmpty()) {
            return "No messages yet";
        }
        Message lastMessage = messages.get(messages.size() - 1);
        String content = lastMessage.getContent();
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    // This should be set by the conversation manager
    private String currentUser;
    
    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }
    
    private String getCurrentUser() {
        return currentUser;
    }
} 