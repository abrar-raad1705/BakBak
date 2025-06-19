package com.bakbak.javafx_proj_1_2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress; // Import this class
import java.util.function.Consumer;

public class NetworkService {

    private static final int PORT = 8888;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";

    private DatagramSocket socket;
    private final Consumer<String> onMessageReceived;
    private volatile boolean listening;
    private Thread listenerThread;

    public NetworkService(Consumer<String> onMessageReceived) throws IOException {
        this.onMessageReceived = onMessageReceived;

        // --- START OF CHANGES ---
        // 1. Create an unbound socket
        this.socket = new DatagramSocket(null);
        // 2. Set the "reuse address" option
        this.socket.setReuseAddress(true);
        // 3. Now, bind the socket to the port
        this.socket.bind(new InetSocketAddress(PORT));
        // --- END OF CHANGES ---

        this.socket.setBroadcast(true);
    }

    public void startListening() {
        listenerThread = new Thread(() -> {
            listening = true;
            byte[] buffer = new byte[1024];
            while (listening) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());

                    onMessageReceived.accept(message);

                } catch (IOException e) {
                    if (listening) {
                        System.err.println("Socket error: " + e.getMessage());
                    }
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(BROADCAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
            socket.send(packet);
            System.out.println("DEBUG: Message sent successfully via socket.");
        } catch (IOException e) {
            System.err.println("!!!!!! DEBUG: FAILED TO SEND MESSAGE. IOException occurred !!!!!!");
            e.printStackTrace();
        }
    }

    public void stopListening() {
        listening = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}