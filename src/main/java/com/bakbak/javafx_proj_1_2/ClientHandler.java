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

    public ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
        ois = new ObjectInputStream(clientSocket.getInputStream());
        isLoggedIn = false;
    }

    @Override
    public void run() {
        try {
            Message message;
            while ((message = (Message) ois.readObject()) != null) {
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
            case FILE_MESSAGE:
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
            case DELETE_HISTORY:
                handleDeleteHistory(message, messageStore);
                break;
            case USER_STATUS_UPDATE:
                // This message type is only sent from server to clients
                // No handling needed in server-side ClientHandler
                break;
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

            for (Message offlineMessage : offlineMessages) {
                sendMessage(offlineMessage);
            }

            System.out.println("Delivered " + offlineMessages.size() + " offline messages");
        }
    }

    private void handleLogout(Message message, UserManager userManager) {
        if (isLoggedIn) {
            userManager.logoutUser(username);
            isLoggedIn = false;
            System.out.println("User " + username + " logged out");
        }
        disconnect();
    }

    private void handlePrivateMessage(Message message, UserManager userManager, MessageStore messageStore) {
        String recipient = message.getRecipient();
        messageStore.storeMessage(message);

        if (userManager.isUserOnline(recipient)) {
            ClientHandler recipientHandler = userManager.getClientHandler(recipient);
            if (recipientHandler != null) {
                recipientHandler.sendMessage(message);
            }

            //! Send 'delivered' ack to sender
            // Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
            // ack.setSuccess(true);
            // ack.setContent("Message delivered to " + recipient);
            // sendMessage(ack);
        } else {
            messageStore.queueOfflineMessage(recipient, message);

            //! Send 'sent' ack to sender
            // Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
            // ack.setSuccess(true);
            // ack.setContent("Message queued for " + recipient + " (offline)");
            // sendMessage(ack);
        }
    }

    private void handleGroupMessage(Message message, GroupManager groupManager) {
        if (message.getType() == Message.MessageType.FILE_MESSAGE) {
            handleFileMessage(message);
            return;
        }
        String groupId = message.getGroupId();
        String content = message.getContent();
        Set<String> members = groupManager.getGroupMembers(groupId);
        UserManager userManager = UserManager.getInstance();
        MessageStore messageStore = MessageStore.getInstance();

        // Handle group name change
        if (content != null && content.startsWith("CHANGE_GROUP_NAME:")) {
            String newGroupName = content.substring("CHANGE_GROUP_NAME:".length());
            Group group = groupManager.getGroup(groupId);
            if (group != null) {
                group.setGroupName(newGroupName);
                groupManager.saveGroups(); // Persist the change

                // Notify all group members (online) about the new group name
                for (String member : members) {
                    if (userManager.isUserOnline(member)) {
                        ClientHandler memberHandler = userManager.getClientHandler(member);
                        if (memberHandler != null) {
                            Message groupNotification = new Message(Message.MessageType.GROUP_LIST, "SERVER");
                            groupNotification.setContent("GROUP_ADDED:" + newGroupName + "|" + groupId + "|" + group.getCreator());
                            memberHandler.sendMessage(groupNotification);
                        }
                    }
                }
            }
            return; // Do not treat as a normal group message
        }

        /* if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        } */

        /* if (!groupManager.isGroupMember(groupId, username)) {
            sendErrorMessage("You are not a member of this group");
            return;
        } */

        // Store the group message for all members (including sender)
        messageStore.storeGroupMessage(message, members);

        // Send message to online members (excluding sender)
        for (String member : members) {
            if (!member.equals(username)) {
                if (userManager.isUserOnline(member)) {
                    ClientHandler memberHandler = userManager.getClientHandler(member);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(message);
                    }
                } else {
                    // Queue message for offline members
                    messageStore.queueOfflineMessage(member, message);
                }
            }
        }

        //! Send sent to sender
        /* Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        ack.setSuccess(true);
        ack.setContent("Message delivered to " + deliveredCount + " group members");
        sendMessage(ack); */
    }

    private void handleFileMessage(Message message) {
        MessageStore.getInstance().storeMessage(message);

        if (message.getGroupId() != null && !message.getGroupId().isEmpty()) {
            // Group file message
            Set<String> members = GroupManager.getInstance().getGroupMembers(message.getGroupId());
            for (String member : members) {
                if (!member.equals(username)) {
                    ClientHandler handler = UserManager.getInstance().getClientHandler(member);
                    if (handler != null) {
                        handler.sendMessage(message);
                    } else {
                        MessageStore.getInstance().queueOfflineMessage(member, message);
                    }
                }
            }
        } else {
            // Private file message
            String recipient = message.getRecipient();
            ClientHandler handler = UserManager.getInstance().getClientHandler(recipient);
            if (handler != null) {
                handler.sendMessage(message);
            } else {
                MessageStore.getInstance().queueOfflineMessage(recipient, message);
            }
        }
    }

    private void handleCreateGroup(Message message, GroupManager groupManager) {
/*         if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        } */
        String groupName = message.getContent();
        String groupId = groupManager.createGroup(groupName, username);
        
        // Process selected members if provided
        String selectedMembersString = message.getRecipient();
        if (selectedMembersString != null && !selectedMembersString.trim().isEmpty()) {
            String[] selectedMembers = selectedMembersString.split(",");
            for (String member : selectedMembers) {
                String memberName = member.trim();
                if (!memberName.isEmpty() && !memberName.equals(username)) {
                    groupManager.joinGroup(groupId, memberName);
                    System.out.println("Added " + memberName + " to group " + groupName);
                }
            }
        }

        // Send acknowledgment to group creator
        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(true);
        response.setContent("Group created successfully");
        response.setGroupId(groupId);
        sendMessage(response);
        
        // Notify all group members (including creator) about the new group
        Group group = groupManager.getGroup(groupId);
        if (group != null) {
            UserManager userManager = UserManager.getInstance();
            for (String member : group.getMembers()) {
                if (userManager.isUserOnline(member)) {
                    ClientHandler memberHandler = userManager.getClientHandler(member);
                    if (memberHandler != null) {
                        // Send group notification to member
                        Message groupNotification = new Message(Message.MessageType.GROUP_LIST, "SERVER");
                        groupNotification.setContent("GROUP_ADDED:" + groupName + "|" + groupId + "|" + group.getCreator());
                        memberHandler.sendMessage(groupNotification);
                    }
                }
            }
        }
    }

    private void handleJoinGroup(Message message, GroupManager groupManager) {
/*         if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        } */
        String groupId = message.getGroupId();
        String targetUser = message.getRecipient(); // User to add to group (if specified)
        String userToAdd = (targetUser != null && !targetUser.trim().isEmpty()) ? targetUser.trim() : username;
        
        // Handle admin promotion
        if ("PROMOTE_ADMIN".equals(message.getContent())) {
            handlePromoteAdmin(message, groupManager);
            return;
        }

        // Check if this is an admin adding another user
        if (!userToAdd.equals(username)) {
            Group group = groupManager.getGroup(groupId);
            if (group != null && !group.isAdmin(username)) {
                // Only admins can add other users
                Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
                response.setSuccess(false);
                response.setContent("Only admins can add members to the group");
                sendMessage(response);
                return;
            }
        }
        
        boolean success = groupManager.joinGroup(groupId, userToAdd);

        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(success);
        response.setContent(success ? 
            (userToAdd.equals(username) ? "Joined group successfully" : "Member added successfully") : 
            "Failed to add member to group");
        sendMessage(response);
        
        // If successful and adding another user, notify that user about the new group
        if (success && !userToAdd.equals(username)) {
            Group group = groupManager.getGroup(groupId);
            if (group != null) {
                UserManager userManager = UserManager.getInstance();
                if (userManager.isUserOnline(userToAdd)) {
                    ClientHandler memberHandler = userManager.getClientHandler(userToAdd);
                    if (memberHandler != null) {
                        // Send group notification to the newly added member
                        Message groupNotification = new Message(Message.MessageType.GROUP_LIST, "SERVER");
                        groupNotification.setContent("GROUP_ADDED:" + group.getGroupName() + "|" + groupId + "|" + group.getCreator());
                        memberHandler.sendMessage(groupNotification);
                    }
                }
            }
        }
    }

    private void handleLeaveGroup(Message message, GroupManager groupManager) {
/*         if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        } */
        String groupId = message.getGroupId();
        String targetUser = message.getRecipient(); // User to be removed by an admin
        String specialContent = message.getContent();

        // Handle admin demotion
        if ("REMOVE_ADMIN".equals(specialContent)) {
            handleDemoteAdmin(message, groupManager);
            return;
        }

        // Handle member removal by admin
        if ("REMOVE_MEMBER".equals(specialContent)) {
            handleRemoveMember(message, groupManager);
            return;
        }

        // Regular user leaving the group
        boolean success = groupManager.leaveGroup(groupId, username);

        Message response = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        response.setSuccess(success);
        response.setContent(success ? "Left group successfully" : "Failed to leave group");
        sendMessage(response);
    }
    
    private void handlePromoteAdmin(Message message, GroupManager groupManager) {
        String groupId = message.getGroupId();
        String requestingUser = message.getSender();
        String targetUser = message.getRecipient();

        boolean success = groupManager.promoteToAdmin(groupId, requestingUser, targetUser);
        if (success) {
            notifyGroupMembersOfChange(groupId, groupManager);
        }
    }

    private void handleDemoteAdmin(Message message, GroupManager groupManager) {
        String groupId = message.getGroupId();
        String requestingUser = message.getSender();
        String targetUser = message.getRecipient();

        boolean success = groupManager.demoteAdmin(groupId, requestingUser, targetUser);
        if (success) {
            notifyGroupMembersOfChange(groupId, groupManager);
        }
    }

    private void handleRemoveMember(Message message, GroupManager groupManager) {
        String groupId = message.getGroupId();
        String requestingUser = message.getSender();
        String targetUser = message.getRecipient();

        boolean success = groupManager.removeMember(groupId, requestingUser, targetUser);
        if (success) {
            notifyGroupMembersOfChange(groupId, groupManager);
            
            // Also notify the removed user that they've been removed
            UserManager userManager = UserManager.getInstance();
            if (userManager.isUserOnline(targetUser)) {
                ClientHandler removedUserHandler = userManager.getClientHandler(targetUser);
                if (removedUserHandler != null) {
                    Group group = groupManager.getGroup(groupId);
                    Message removalNotification = new Message(Message.MessageType.GROUP_LIST, "SERVER");
                    removalNotification.setContent("REMOVED_FROM_GROUP:" + group.getGroupName() + "|" + groupId);
                    removedUserHandler.sendMessage(removalNotification);
                }
            }
        }
    }
    
    private void notifyGroupMembersOfChange(String groupId, GroupManager groupManager) {
        Group group = groupManager.getGroup(groupId);
        if (group != null) {
            UserManager userManager = UserManager.getInstance();
            for (String member : group.getMembers()) {
                if (userManager.isUserOnline(member)) {
                    ClientHandler memberHandler = userManager.getClientHandler(member);
                    if (memberHandler != null) {
                        sendDetailedGroupInfo(group, groupManager);
                    }
                }
            }
        }
    }

    private void handleUserList(Message message, UserManager userManager) {
/*         if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        } */
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
        /* if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        } */
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
        // if (!isLoggedIn) {
        //     sendErrorMessage("Please login first");
        //     return;
        // }
        
        // Check if this is a request for specific group info
        String requestedGroupId = message.getGroupId();
        if (requestedGroupId != null && !requestedGroupId.trim().isEmpty()) {
            // Send detailed info for specific group
            Group group = groupManager.getGroup(requestedGroupId);
            if (group != null && group.isMember(username)) {
                sendDetailedGroupInfo(group, groupManager);
            }
            return;
        }
        
        // Send list of all user's groups with basic info
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
    
    private void sendDetailedGroupInfo(Group group, GroupManager groupManager) {
        UserManager userManager = UserManager.getInstance();
        
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
            if (userManager.isUserOnline(member)) {
                groupInfo.append(member).append(",");
            }
        }
        
        Message response = new Message(Message.MessageType.GROUP_LIST, "SERVER");
        response.setContent(groupInfo.toString());
        response.setGroupId(group.getGroupId());
        sendMessage(response);
    }

    private void handleSyncHistory(Message message, MessageStore messageStore) {
        String withUser = message.getContent();
        String groupId = message.getGroupId();

        if (groupId != null && !groupId.isEmpty()) {
            // Sync group chat history
            List<Message> groupHistory = messageStore.getGroupConversationHistory(username, groupId);
            sendMessagesWithDelay(groupHistory);
        } else if (withUser != null && !withUser.isEmpty()) {
            // Sync private chat history
            List<Message> conversationHistory = messageStore.getConversationHistory(username, withUser);
            sendMessagesWithDelay(conversationHistory);
        }
    }
    
    private void handleDeleteHistory(Message message, MessageStore messageStore) {
        String requestingUser = message.getSender();
        String targetUser = message.getRecipient();
        String groupId = message.getGroupId();

        if (targetUser != null && !targetUser.isEmpty()) {
            // It's a private chat history deletion
            messageStore.deletePrivateChatHistory(requestingUser, targetUser);
            System.out.println("Deleted chat history between " + requestingUser + " and " + targetUser);
        } else if (groupId != null && !groupId.isEmpty()) {
            // It's a group chat history deletion for a single user
            // Note: This only removes it from the requesting user's view in a shared file scenario
            // A more robust implementation might have per-user group message files.
            messageStore.deleteGroupChatHistoryForUser(requestingUser, groupId);
            System.out.println("Deleted group chat history for user " + requestingUser + " in group " + groupId);
        }

        // No response is sent back to the client, as it's a fire-and-forget action
    }

    private void sendMessagesWithDelay(List<Message> messages) {
        // Limit to last 50 messages to prevent overwhelming the connection
        final List<Message> messagesToSend;
        if (messages.size() > 50) {
            messagesToSend = messages.subList(messages.size() - 50, messages.size());
        } else {
            messagesToSend = messages;
        }
        
        // Send messages directly
        for (Message historyMessage : messagesToSend) {
            sendMessage(historyMessage);
        }
    }

    /* private String serializeMessage(Message message) {
        return message.getSender() + "|" + message.getRecipient() + "|" +
                message.getContent() + "|" + message.getTimestamp().toString();
    } */

    public synchronized void sendMessage(Message message) {
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
        }
    }

    /* private void sendErrorMessage(String errorMessage) {
        Message error = new Message(Message.MessageType.ERROR, "SERVER");
        error.setSuccess(false);
        error.setErrorMessage(errorMessage);
        sendMessage(error);
    } */

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