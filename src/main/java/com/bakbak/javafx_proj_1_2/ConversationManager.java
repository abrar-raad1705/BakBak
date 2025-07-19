package com.bakbak.javafx_proj_1_2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConversationManager {
    private Map<String, Conversation> conversations;
    private String currentUser;

    public ConversationManager(String currentUser) {
        this.conversations = new HashMap<>();
        this.currentUser = currentUser;
    }

    public Conversation getOrCreateConversation(String contactUsername) {
        return conversations.computeIfAbsent(contactUsername, username -> {
            Conversation conv = new Conversation(username);
            conv.setCurrentUser(currentUser);
            return conv;
        });
    }

    public void addMessage(Message message) {
        String contactUsername;
        
        // Determine which conversation this message belongs to
        if (message.getSender().equals(currentUser)) {
            // Outgoing message - conversation is with the recipient
            contactUsername = message.getRecipient();
        } else {
            // Incoming message - conversation is with the sender
            contactUsername = message.getSender();
        }

        Conversation conversation = getOrCreateConversation(contactUsername);
        conversation.addMessage(message);
    }

    public Conversation getConversation(String contactUsername) {
        return conversations.get(contactUsername);
    }

    public List<Conversation> getAllConversations() {
        return conversations.values().stream()
                .sorted((c1, c2) -> c2.getLastActivity().compareTo(c1.getLastActivity()))
                .collect(Collectors.toList());
    }

    public void updateContactOnlineStatus(String contactUsername, boolean isOnline) {
        Conversation conversation = conversations.get(contactUsername);
        if (conversation != null) {
            conversation.setOnline(isOnline);
        }
    }

    public void markConversationAsRead(String contactUsername) {
        Conversation conversation = conversations.get(contactUsername);
        if (conversation != null) {
            conversation.markAsRead();
        }
    }

    public int getTotalUnreadCount() {
        return conversations.values().stream()
                .mapToInt(Conversation::getUnreadCount)
                .sum();
    }

    public boolean hasConversation(String contactUsername) {
        return conversations.containsKey(contactUsername);
    }
} 