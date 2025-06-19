package com.bakbak.javafx_proj_1_2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.function.Consumer;

public class NetworkService {

    private static final int PORT = 8888; // Port to broadcast and listen on
    private static final String BROADCAST_ADDRESS = "255.255.255.255"; // Broadcast to all devices on the network

    private DatagramSocket socket;
    private final Consumer<String> onMessageReceived;
    private volatile boolean listening;
    private Thread listenerThread;

    public NetworkService(Consumer<String> onMessageReceived) throws IOException {
        this.onMessageReceived = onMessageReceived;
        // Create a socket that can listen on our port
        this.socket = new DatagramSocket(PORT);
        this.socket.setBroadcast(true);
    }

    public void startListening() {
        listenerThread = new Thread(() -> {
            listening = true;
            byte[] buffer = new byte[1024];
            while (listening) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // This is a blocking call

                    String message = new String(packet.getData(), 0, packet.getLength());

                    // Pass the received message to the controller via the callback
                    onMessageReceived.accept(message);

                } catch (IOException e) {
                    if (listening) {
                        System.err.println("Socket error: " + e.getMessage());
                    }
                    // The loop will terminate if the socket is closed.
                }
            }
        });
        listenerThread.setDaemon(true); // Allows the app to exit without explicitly stopping the thread
        listenerThread.start();
    }

    public void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(BROADCAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopListening() {
        listening = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // This will interrupt the blocking socket.receive() call
        }
    }
}