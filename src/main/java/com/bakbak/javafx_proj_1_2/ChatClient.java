package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private MessageListener messageListener;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());

        // Start a new thread to listen for incoming messages from the server
        Thread listenerThread = new Thread(this::listenForMessages);
        listenerThread.setDaemon(true); // Ensures the thread exits when the app closes
        listenerThread.start();
    }

    private void listenForMessages() {
        try {
            while (!socket.isClosed()) {
                Message messageFromServer = (Message) ois.readObject();
                if (messageListener != null) {
                    // Notify the active controller about the new message
                    messageListener.onMessageReceived(messageFromServer);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Disconnected from server: " + e.getMessage());
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (oos != null) {
            oos.writeObject(message);
            oos.flush();
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
}