package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String username;
    private boolean isLoggedIn;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        isLoggedIn = false;

        try {
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error creating streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            Message message = (Message) ois.readObject();
            while (message != null) {
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(Message message) {
        UserManager userManager = UserManager.getInstance();
        GroupManager groupManager = GroupManager.getInstance();
        MessageStore messageStore = MessageStore.getInstance();

        switch (message.getType()) {
            case REGISTER:
                handleRegister(message, userManager);
                break;
            case LOGIN:
                handleLogin(message, userManager, messageStore);
                break;
            case LOGOUT:
                handleLogout(message, userManager);
                break;
            case PRIVATE_MESSAGE:
                handlePrivateMessage(message, userManager, messageStore);
                break;
            case GROUP_MESSAGE:
                handleGroupMessage(message, groupManager);
                break;
            case CREATE_GROUP:
                handleCreateGroup(message, groupManager);
                break;
            case JOIN_GROUP:
                handleJoinGroup(message, groupManager);
                break;
            case LEAVE_GROUP:
                handleLeaveGroup(message, groupManager);
                break;
            case USER_LIST:
                handleUserList(message, userManager);
                break;
            case USER_SEARCH:
                handleUserSearch(message, userManager);
                break;
            case GROUP_LIST:
                handleGroupList(message, groupManager);
                break;
            case SYNC_HISTORY:
                handleSyncHistory(message, messageStore);
                break;
            default:
                sendErrorMessage("Unknown message type");
        }
    }

    private void handleRegister(Message message, UserManager userManager) {
        String username = message.getSender();
        String password = message.getContent();

        boolean success = userManager.registerUser(username, password);
        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(success);

        if (success) {
            response.setContent("Registration successful");
        } else {
            response.setContent("Registration failed - username already exists");
        }

        sendMessage(response);
    }

    private void handleLogin(Message message, UserManager userManager, MessageStore messageStore) {
        String username = message.getSender();
        String password = message.getContent();

        boolean success = userManager.loginUser(username, password, this);
        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(success);

        if (success) {
            this.username = username;
            this.isLoggedIn = true;
            response.setContent("Login successful");
            System.out.println("User " + username + " logged in");

            sendMessage(response);

            sendOfflineMessages(username, messageStore);
        } else {
            response.setContent("Login failed - invalid credentials or user already online");
            sendMessage(response);
        }
    }

    private void sendOfflineMessages(String username, MessageStore messageStore) {
        List<Message> offlineMessages = messageStore.getOfflineMessages(username);

        if (!offlineMessages.isEmpty()) {
            System.out.println("Delivering " + offlineMessages.size() + " offline messages to " + username);

            // Send each offline message
            for (Message offlineMessage : offlineMessages) {
                sendMessage(offlineMessage);
            }

            // Send notification about offline messages delivered
            Message notification = new Message(Message.MessageType.OFFLINE_MESSAGES, "SERVER");
            notification.setContent("Delivered " + offlineMessages.size() + " offline messages");
            sendMessage(notification);
        }
    }

    private void handleLogout(Message message, UserManager userManager) {
        if(isLoggedIn) {
            userManager.logoutUser(username);
            isLoggedIn = false;
            System.out.println("User " + username + " logged out");
        }
        disconnect();
    }

    private void handlePrivateMessage(Message message, UserManager userManager, MessageStore messageStore) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String recipient = message.getRecipient();

        // Always store the message first
        messageStore.storeMessage(message);

        // Check if recipient is online
        if (userManager.isUserOnline(recipient)) {
            // Deliver immediately if online
            ClientHandler recipientHandler = userManager.getClientHandler(recipient);
            if (recipientHandler != null) {
                recipientHandler.sendMessage(message);
            }

            // Send acknowledgment to sender
            Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
            ack.setSuccess(true);
            ack.setContent("Message delivered to " + recipient);
            sendMessage(ack);
        } else {
            // Queue for offline delivery if recipient is offline
            messageStore.queueOfflineMessage(recipient, message);

            // Send acknowledgment to sender
            Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
            ack.setSuccess(true);
            ack.setContent("Message queued for " + recipient + " (offline)");
            sendMessage(ack);
        }
    }

    private void handleGroupMessage(Message message, GroupManager groupManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String groupId = message.getGroupId();

        if (!groupManager.isGroupMember(groupId, username)) {
            sendErrorMessage("You are not a member of this group");
            return;
        }

        Set<String> members = groupManager.getGroupMembers(groupId);
        UserManager userManager = UserManager.getInstance();

        int deliveredCount = 0;
        for (String member : members) {
            if (!member.equals(username) && userManager.isUserOnline(member)) {
                ClientHandler memberHandler = userManager.getClientHandler(member);
                if (memberHandler != null) {
                    memberHandler.sendMessage(message);
                    deliveredCount++;
                }
            }
        }

        // Send acknowledgment to sender
        Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        ack.setSuccess(true);
        ack.setContent("Message delivered to " + deliveredCount + " group members");
        sendMessage(ack);
    }

    private void handleCreateGroup(Message message, GroupManager groupManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String groupName = message.getContent();
        String groupId = groupManager.createGroup(groupName, username);

        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(true);
        response.setContent("Group created successfully");
        response.setGroupId(groupId);
        sendMessage(response);
    }

    private void handleJoinGroup(Message message, GroupManager groupManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String groupId = message.getGroupId();
        boolean success = groupManager.joinGroup(groupId, username);

        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(success);
        response.setContent(success ? "Joined group successfully" : "Failed to join group");
        sendMessage(response);
    }

    private void handleLeaveGroup(Message message, GroupManager groupManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String groupId = message.getGroupId();
        boolean success = groupManager.leaveGroup(groupId, username);

        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(success);
        response.setContent(success ? "Left group successfully" : "Failed to leave group");
        sendMessage(response);
    }

    private void handleUserList(Message message, UserManager userManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        Set<String> onlineUsers = userManager.getOnlineUsers();
        StringBuilder userList = new StringBuilder("Online users: ");
        for (String user : onlineUsers) {
            if (!user.equals(username)) {
                userList.append(user).append(", ");
            }
        }

        Message response = new Message(Message.MessageType.USER_LIST, "SERVER");
        response.setContent(userList.toString());
        sendMessage(response);
    }

    private void handleUserSearch(Message message, UserManager userManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String searchQuery = message.getContent().toLowerCase();
        Set<String> allUsers = userManager.getAllUsers();
        StringBuilder searchResults = new StringBuilder();

        for (String user : allUsers) {
            if (!user.equals(username) && user.toLowerCase().contains(searchQuery)) {
                boolean isOnline = userManager.isUserOnline(user);
                searchResults.append(user).append(isOnline ? " (online)" : " (offline)").append("|");
            }
        }

        Message response = new Message(Message.MessageType.USER_SEARCH, "SERVER");
        response.setContent(searchResults.toString());
        sendMessage(response);
    }

    private void handleGroupList(Message message, GroupManager groupManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        Set<String> userGroups = groupManager.getUserGroups(username);
        StringBuilder groupList = new StringBuilder("Your groups: ");
        for (String groupId : userGroups) {
            Group group = groupManager.getGroup(groupId);
            if (group != null) {
                groupList.append(group.getGroupName()).append(" (ID: ").append(groupId).append("), ");
            }
        }

        Message response = new Message(Message.MessageType.GROUP_LIST, "SERVER");
        response.setContent(groupList.toString());
        sendMessage(response);
    }

    private void handleSyncHistory(Message message, MessageStore messageStore) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }

        String withUser = message.getContent(); // The user to sync conversation history with

        if (withUser != null && !withUser.isEmpty()) {
            List<Message> conversationHistory = messageStore.getConversationHistory(username, withUser);

            // Send the conversation history
            for (Message historyMessage : conversationHistory) {
                Message historyResponse = new Message(Message.MessageType.MESSAGE_HISTORY, "SERVER");
                historyResponse.setContent(serializeMessage(historyMessage));
                sendMessage(historyResponse);
            }
        }
    }

    private String serializeMessage(Message message) {
        // Simple serialization for message history
        return message.getSender() + "|" + message.getRecipient() + "|" +
                message.getContent() + "|" + message.getTimestamp().toString();
    }

    public void sendMessage(Message message) {
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
        }
    }

    private void sendErrorMessage(String errorMessage) {
        Message error = new Message(Message.MessageType.ERROR, "SERVER");
        error.setSuccess(false);
        error.setErrorMessage(errorMessage);
        sendMessage(error);
    }

    private void disconnect() {
        try {
            if (isLoggedIn) {
                UserManager.getInstance().logoutUser(username);
                System.out.println("User " + username + " disconnected");
            }

            if (ois != null)
                ois.close();
            if (oos != null)
                oos.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}