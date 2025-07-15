import java.io.*;
import java.net.Socket;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String username;
    private boolean isLoggedIn;
    
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isLoggedIn = false;
        
        try {
            this.objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error creating streams: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            Message message;
            while ((message = (Message) objectInputStream.readObject()) != null) {
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
        
        switch (message.getType()) {
            case REGISTER:
                handleRegister(message, userManager);
                break;
            case LOGIN:
                handleLogin(message, userManager);
                break;
            case LOGOUT:
                handleLogout(message, userManager);
                break;
            case PRIVATE_MESSAGE:
                handlePrivateMessage(message, userManager);
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
            case GROUP_LIST:
                handleGroupList(message, groupManager);
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
    
    private void handleLogin(Message message, UserManager userManager) {
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
        } else {
            response.setContent("Login failed - invalid credentials or user already online");
        }
        
        sendMessage(response);
    }
    
    private void handleLogout(Message message, UserManager userManager) {
        if (isLoggedIn) {
            userManager.logoutUser(username);
            isLoggedIn = false;
            System.out.println("User " + username + " logged out");
        }
        disconnect();
    }
    
    private void handlePrivateMessage(Message message, UserManager userManager) {
        if (!isLoggedIn) {
            sendErrorMessage("Please login first");
            return;
        }
        
        String recipient = message.getRecipient();
        
        if (!userManager.isUserOnline(recipient)) {
            sendErrorMessage("User " + recipient + " is not online");
            return;
        }
        
        ClientHandler recipientHandler = userManager.getClientHandler(recipient);
        if (recipientHandler != null) {
            recipientHandler.sendMessage(message);
        }
        
        // Send acknowledgment to sender
        Message ack = new Message(Message.MessageType.ACKNOWLEDGMENT, "SERVER");
        ack.setSuccess(true);
        ack.setContent("Message delivered to " + recipient);
        sendMessage(ack);
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
    
    public void sendMessage(Message message) {
        try {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
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
            
            if (objectInputStream != null) objectInputStream.close();
            if (objectOutputStream != null) objectOutputStream.close();
            if (clientSocket != null) clientSocket.close();
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