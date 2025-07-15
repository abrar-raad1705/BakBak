import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 8080;
    private static final int MAX_CLIENTS = 100;
    
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private boolean isRunning;
    
    public ChatServer() {
        this.clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.isRunning = false;
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        isRunning = true;
        
        System.out.println("Chat Server started on port " + PORT);
        System.out.println("Waiting for client connections...");
        
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                // Create a new client handler for each connection
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientThreadPool.submit(clientHandler);
                
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    public void stop() {
        isRunning = false;
        
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        if (clientThreadPool != null) {
            clientThreadPool.shutdown();
        }
        
        System.out.println("Chat Server stopped");
    }
    
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
} 