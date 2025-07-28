package com.bakbak.javafx_proj_1_2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatClient {
    private String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Consumer<Message> messageHandler;
    private Thread listenerThread;
    private boolean isConnected = false;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setMessageHandler(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            isConnected = true;

            listenerThread = new Thread(() -> {
                while (isConnected && !socket.isClosed()) {
                    try {
                        Message msgFromServer = (Message) ois.readObject();
                        if (messageHandler != null) {
                            messageHandler.accept(msgFromServer);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        if (isConnected) {
                            System.err.println("Error receiving message: " + e.getMessage());
                        }
                        break;
                    }
                }
            });
            listenerThread.start();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (oos != null && isConnected) {
            oos.writeObject(message);
            oos.flush();
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    // UDP Broadcasting methods for server discovery
    public static List<String> discoverServers(int timeoutMs) {
        List<String> discoveredServers = new ArrayList<>();
        
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setBroadcast(true);
            udpSocket.setSoTimeout(timeoutMs);
            
            // Send broadcast message
            String broadcastMessage = "DISCOVER_CHAT_SERVER";
            byte[] sendData = broadcastMessage.getBytes();
            
            // Try broadcast to multiple common broadcast addresses
            String[] broadcastAddresses = {
                "255.255.255.255",
                "192.168.255.255", 
                "10.255.255.255",
                "172.31.255.255"
            };
            
            for (String broadcastAddr : broadcastAddresses) {
                try {
                    InetAddress broadcastIP = InetAddress.getByName(broadcastAddr);
                    DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, broadcastIP, 8888
                    );
                    udpSocket.send(sendPacket);
                } catch (Exception e) {
                    // Continue with next broadcast address
                }
            }
            
            // Also try subnet broadcast
            try {
                NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(ni -> {
                    try {
                        if (ni.isUp() && !ni.isLoopback()) {
                            ni.getInterfaceAddresses().forEach(addr -> {
                                if (addr.getBroadcast() != null) {
                                    try {
                                        DatagramPacket sendPacket = new DatagramPacket(
                                            sendData, sendData.length, addr.getBroadcast(), 8888
                                        );
                                        udpSocket.send(sendPacket);
                                    } catch (IOException e) {
                                        // Continue with next address
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        // Continue with next interface
                    }
                });
            } catch (Exception e) {
                // Continue with basic broadcast
            }
            
            // Listen for responses
            byte[] buffer = new byte[1024];
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(receivePacket);
                    
                    String serverIP = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    if (!discoveredServers.contains(serverIP)) {
                        discoveredServers.add(serverIP);
                        System.out.println("Discovered server at: " + serverIP);
                    }
                } catch (SocketTimeoutException e) {
                    // Expected timeout, continue listening until total timeout
                    break;
                } catch (IOException e) {
                    System.err.println("Error during server discovery: " + e.getMessage());
                    break;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in server discovery: " + e.getMessage());
        }
        
        return discoveredServers;
    }
}