import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private Scanner scanner;
    private String username;
    private boolean isLoggedIn;
    
    public ChatClient() {
        this.scanner = new Scanner(System.in);
        this.isLoggedIn = false;
    }
    
    public void start() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("Connected to chat server");
            
            // Start listening for messages from server
            Thread messageListener = new Thread(this::listenForMessages);
            messageListener.start();
            
            // Main menu loop
            showMainMenu();
            
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
    
    private void listenForMessages() {
        try {
            Message message;
            while ((message = (Message) objectInputStream.readObject()) != null) {
                handleServerMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Connection to server lost: " + e.getMessage());
        }
    }
    
    private void handleServerMessage(Message message) {
        switch (message.getType()) {
            case PRIVATE_MESSAGE:
                System.out.println("\n[Private] " + message.getSender() + ": " + message.getContent());
                break;
            case GROUP_MESSAGE:
                System.out.println("\n[Group] " + message.getSender() + ": " + message.getContent());
                break;
            case ACKNOWLEDGMENT:
                System.out.println("\nServer: " + message.getContent());
                break;
            case ERROR:
                System.out.println("\nError: " + message.getErrorMessage());
                break;
            case USER_LIST:
                System.out.println("\n" + message.getContent());
                break;
            case GROUP_LIST:
                System.out.println("\n" + message.getContent());
                break;
            default:
                System.out.println("\nReceived: " + message.getContent());
        }
        System.out.print("> ");
    }
    
    private void showMainMenu() {
        while (true) {
            if (!isLoggedIn) {
                System.out.println("\n=== Chat Client ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                
                String choice = scanner.nextLine();
                
                switch (choice) {
                    case "1":
                        register();
                        break;
                    case "2":
                        login();
                        break;
                    case "3":
                        disconnect();
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } else {
                showChatMenu();
            }
        }
    }
    
    private void showChatMenu() {
        System.out.println("\n=== Chat Menu ===");
        System.out.println("1. Send private message");
        System.out.println("2. Send group message");
        System.out.println("3. Create group");
        System.out.println("4. Join group");
        System.out.println("5. Leave group");
        System.out.println("6. List online users");
        System.out.println("7. List my groups");
        System.out.println("8. Logout");
        System.out.print("Choose an option: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                sendPrivateMessage();
                break;
            case "2":
                sendGroupMessage();
                break;
            case "3":
                createGroup();
                break;
            case "4":
                joinGroup();
                break;
            case "5":
                leaveGroup();
                break;
            case "6":
                listOnlineUsers();
                break;
            case "7":
                listMyGroups();
                break;
            case "8":
                logout();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
    
    private void register() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.REGISTER, username);
        message.setContent(password);
        sendMessage(message);
    }
    
    private void login() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.LOGIN, username);
        message.setContent(password);
        sendMessage(message);
        
        // Wait a bit to see if login was successful
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        this.username = username;
        this.isLoggedIn = true;
    }
    
    private void sendPrivateMessage() {
        System.out.print("Enter recipient username: ");
        String recipient = scanner.nextLine();
        System.out.print("Enter message: ");
        String content = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.PRIVATE_MESSAGE, username);
        message.setRecipient(recipient);
        message.setContent(content);
        sendMessage(message);
    }
    
    private void sendGroupMessage() {
        System.out.print("Enter group ID: ");
        String groupId = scanner.nextLine();
        System.out.print("Enter message: ");
        String content = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.GROUP_MESSAGE, username);
        message.setGroupId(groupId);
        message.setContent(content);
        sendMessage(message);
    }
    
    private void createGroup() {
        System.out.print("Enter group name: ");
        String groupName = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.CREATE_GROUP, username);
        message.setContent(groupName);
        sendMessage(message);
    }
    
    private void joinGroup() {
        System.out.print("Enter group ID: ");
        String groupId = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.JOIN_GROUP, username);
        message.setGroupId(groupId);
        sendMessage(message);
    }
    
    private void leaveGroup() {
        System.out.print("Enter group ID: ");
        String groupId = scanner.nextLine();
        
        Message message = new Message(Message.MessageType.LEAVE_GROUP, username);
        message.setGroupId(groupId);
        sendMessage(message);
    }
    
    private void listOnlineUsers() {
        Message message = new Message(Message.MessageType.USER_LIST, username);
        sendMessage(message);
    }
    
    private void listMyGroups() {
        Message message = new Message(Message.MessageType.GROUP_LIST, username);
        sendMessage(message);
    }
    
    private void logout() {
        Message message = new Message(Message.MessageType.LOGOUT, username);
        sendMessage(message);
        isLoggedIn = false;
        username = null;
    }
    
    private void sendMessage(Message message) {
        try {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
    
    private void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
} 