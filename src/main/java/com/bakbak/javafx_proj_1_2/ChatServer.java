package com.bakbak.javafx_proj_1_2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static final int PORT = 12345;
    public static final int MAX_CLIENTS = 100;

    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private ExecutorService fileThreadPool;
    private boolean isRunning;

    public ChatServer() {
        clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        // Optimized for LAN: Allow more concurrent file transfers
        fileThreadPool = Executors.newFixedThreadPool(10); // Increased from 2 to 10 for better concurrency
        isRunning = false;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        isRunning = true;

        System.out.println("Chat Server started on port " + PORT);
        showAvailableIP();
        startUdpBroadcastListener();
        
        // Start FileReceiver
        System.out.println("Starting FileReceiver service...");
        fileThreadPool.submit(new FileReceiver("chat_data/shared_files"));
        
        // Start FileChunkReceiver
        System.out.println("Starting FileChunkReceiver service...");
        fileThreadPool.submit(new FileChunkReceiver("chat_data/shared_files"));
        
        System.out.println("All file services started successfully");
        System.out.println("Waiting for client connections...");

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientThreadPool.submit(clientHandler);

            } catch (IOException e) {
                if (isRunning) {
                    e.printStackTrace();
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
            e.printStackTrace();
        }

        if (clientThreadPool != null) {
            clientThreadPool.shutdown();
        }
        
        if (fileThreadPool != null) {
            fileThreadPool.shutdown();
        }

        System.out.println("Chat Server stopped");
    }

    public void showAvailableIP() {
        try {
            System.out.println("Available LAN IP addresses for clients:");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            System.out.println("  -> " + addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startUdpBroadcastListener() {
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(8888)) {
                byte[] buffer = new byte[1024];
                System.out.println("UDP Broadcast listener started...");

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String request = new String(packet.getData(), 0, packet.getLength());
                    if ("DISCOVER_CHAT_SERVER".equals(request)) {
                        String response = InetAddress.getLocalHost().getHostAddress();
                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseData, responseData.length, packet.getAddress(), packet.getPort());
                        udpSocket.send(responsePacket);
                    }
                }
            } catch (IOException e) {
                System.err.println("UDP Error: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}