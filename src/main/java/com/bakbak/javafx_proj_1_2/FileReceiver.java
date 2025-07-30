package com.bakbak.javafx_proj_1_2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class FileReceiver implements Runnable {
    public static final int FILE_PORT = 12346;
    private final String fileDirectory;

    public FileReceiver(String fileDirectory) {
        this.fileDirectory = fileDirectory;
        // Ensure the directory exists
        try {
            Files.createDirectories(Paths.get(fileDirectory));
            System.out.println("FileReceiver: Created directory " + fileDirectory);
        } catch (IOException e) {
            System.err.println("FileReceiver: Error creating directory " + fileDirectory + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(FILE_PORT)) {
            System.out.println("FileReceiver started on port " + FILE_PORT);
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleFileTransfer(clientSocket)).start();
                } catch (Exception e) {
                    System.err.println("Error accepting file transfer connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("FileReceiver could not start: " + e.getMessage());
        }
    }

    private void handleFileTransfer(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Read the first message to determine if it's an upload or download request
            bytesRead = in.read(buffer);
            String firstMessage = new String(buffer, 0, bytesRead);
            
            if (firstMessage.startsWith("DOWNLOAD:")) {
                // Handle download request
                handleDownloadRequest(firstMessage.substring(9), out);
            } else {
                // Handle upload request
                handleUploadRequest(firstMessage, in);
            }
            
        } catch (Exception e) {
            System.err.println("Error during file transfer: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void handleUploadRequest(String fileName, InputStream in) {
        try {
            // Read file content
            try (FileOutputStream fos = new FileOutputStream(fileDirectory + "/" + fileName)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("Received file: " + fileName);
        } catch (Exception e) {
            System.err.println("Error during file upload: " + e.getMessage());
        }
    }

    private void handleDownloadRequest(String uuidFileName, OutputStream out) {
        try {
            File file = new File(fileDirectory + "/" + uuidFileName);
            if (!file.exists()) {
                System.err.println("File not found: " + uuidFileName);
                return;
            }

            // Send file content
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
            System.out.println("Sent file: " + uuidFileName);
        } catch (Exception e) {
            System.err.println("Error during file download: " + e.getMessage());
        }
    }
} 