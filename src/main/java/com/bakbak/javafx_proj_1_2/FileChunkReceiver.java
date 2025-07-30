package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileChunkReceiver implements Runnable {
    public static final int CHUNK_PORT = 12347;
    private final String fileDirectory;
    private final Map<String, FileAssembly> fileAssemblies = new ConcurrentHashMap<>();

    public FileChunkReceiver(String fileDirectory) {
        this.fileDirectory = fileDirectory;
        // Ensure the directory exists
        try {
            Files.createDirectories(Paths.get(fileDirectory));
            System.out.println("FileChunkReceiver: Created directory " + fileDirectory);
        } catch (IOException e) {
            System.err.println("FileChunkReceiver: Error creating directory " + fileDirectory + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(CHUNK_PORT)) {
            System.out.println("FileChunkReceiver started on port " + CHUNK_PORT);
            System.out.println("FileChunkReceiver is listening for connections...");
            while (true) {
                try {
                    System.out.println("FileChunkReceiver waiting for client connection...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("FileChunkReceiver accepted connection from: " + clientSocket.getInetAddress());
                    new Thread(() -> handleChunkedTransfer(clientSocket)).start();
                } catch (Exception e) {
                    System.err.println("Error accepting chunked transfer connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("FileChunkReceiver could not start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChunkedTransfer(Socket clientSocket) {
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {

            Object firstObject = ois.readObject();
            
            if (firstObject instanceof String && ((String) firstObject).startsWith("DOWNLOAD:")) {
                // Handle download request
                String fileID = ((String) firstObject).substring(9);
                handleChunkedDownload(fileID, oos);
            } else if (firstObject instanceof FileChunk) {
                // Handle upload request
                handleChunkedUpload((FileChunk) firstObject, ois, oos);
            }

        } catch (Exception e) {
            System.err.println("Error during chunked transfer: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void handleChunkedUpload(FileChunk firstChunk, ObjectInputStream ois, ObjectOutputStream oos) {
        try {
            String fileID = firstChunk.getFileID();
            System.out.println("FileChunkReceiver: Starting upload for file ID: " + fileID);
            System.out.println("FileChunkReceiver: Original filename: " + firstChunk.getOriginalFileName());
            System.out.println("FileChunkReceiver: Total chunks expected: " + firstChunk.getTotalChunks());
            
            FileAssembly assembly = new FileAssembly(fileID, firstChunk.getOriginalFileName(), 
                                                   firstChunk.getTotalFileSize(), firstChunk.getTotalChunks());
            
            // Store assembly in map for concurrent access
            fileAssemblies.put(fileID, assembly);
            
            // Add first chunk
            assembly.addChunk(firstChunk);
            System.out.println("FileChunkReceiver: Added first chunk. Assembly complete: " + assembly.isComplete());
            
            // Continue receiving chunks
            while (!assembly.isComplete()) {
                Object obj = ois.readObject();
                if (obj instanceof FileChunk) {
                    FileChunk chunk = (FileChunk) obj;
                    assembly.addChunk(chunk);
                    
                    System.out.println("Received chunk " + chunk.getChunkNumber() + "/" + chunk.getTotalChunks() + 
                                     " for file: " + chunk.getOriginalFileName());
                    System.out.println("FileChunkReceiver: Assembly complete: " + assembly.isComplete());
                }
            }
            
            // Save completed file
            System.out.println("FileChunkReceiver: Saving file to directory: " + fileDirectory);
            File outputFile = assembly.saveToFile(fileDirectory);
            System.out.println("File assembly completed: " + outputFile.getName() + " at " + outputFile.getAbsolutePath());
            System.out.println("FileChunkReceiver: File exists after save: " + outputFile.exists());
            System.out.println("FileChunkReceiver: File size: " + outputFile.length() + " bytes");
            
            // Clean up
            fileAssemblies.remove(fileID);
            
        } catch (Exception e) {
            System.err.println("Error during chunked upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChunkedDownload(String fileID, ObjectOutputStream oos) {
        try {
            File file = new File(fileDirectory + "/" + fileID);
            if (!file.exists()) {
                oos.writeObject("ERROR:File not found");
                oos.flush();
                return;
            }

            // Read file and send as chunks
            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / FileChunkSender.CHUNK_SIZE);
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[FileChunkSender.CHUNK_SIZE];
                int chunkNumber = 0;
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    chunkNumber++;
                    boolean isLastChunk = (chunkNumber == totalChunks);

                    // Create chunk with actual data size
                    byte[] chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

                    FileChunk chunk = new FileChunk(
                        chunkData, chunkNumber, isLastChunk, fileID,
                        file.getName(), fileSize, totalChunks
                    );

                    oos.writeObject(chunk);
                    oos.flush();

                    System.out.println("Sent chunk " + chunkNumber + "/" + totalChunks + " for download");
                    
                    // Small delay to prevent overwhelming the client
                    Thread.sleep(10);
                }
            }

            System.out.println("Chunked download completed for file: " + file.getName());

        } catch (Exception e) {
            try {
                oos.writeObject("ERROR:" + e.getMessage());
                oos.flush();
            } catch (IOException ioException) {
                System.err.println("Error sending error response: " + ioException.getMessage());
            }
            System.err.println("Error during chunked download: " + e.getMessage());
        }
    }

    // Inner class to manage file assembly
    private static class FileAssembly {
        private final String fileID;
        private final String originalFileName;
        private final long totalFileSize;
        private final int totalChunks;
        private final Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();
        private volatile boolean complete = false;

        public FileAssembly(String fileID, String originalFileName, long totalFileSize, int totalChunks) {
            this.fileID = fileID;
            this.originalFileName = originalFileName;
            this.totalFileSize = totalFileSize;
            this.totalChunks = totalChunks;
        }

        public void addChunk(FileChunk chunk) {
            chunks.put(chunk.getChunkNumber(), chunk.getData());
            
            if (chunk.isLastChunk() || chunks.size() == totalChunks) {
                complete = true;
            }
        }

        public boolean isComplete() {
            return complete && chunks.size() == totalChunks;
        }

        public File saveToFile(String directory) throws IOException {
            File outputFile = new File(directory, fileID);
            System.out.println("FileAssembly: Saving file to: " + outputFile.getAbsolutePath());
            System.out.println("FileAssembly: Directory exists: " + new File(directory).exists());
            System.out.println("FileAssembly: Directory is writable: " + new File(directory).canWrite());
            System.out.println("FileAssembly: Total chunks to write: " + totalChunks);
            System.out.println("FileAssembly: Chunks in memory: " + chunks.size());
            
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                for (int i = 1; i <= totalChunks; i++) {
                    byte[] chunkData = chunks.get(i);
                    if (chunkData != null) {
                        fos.write(chunkData);
                        System.out.println("FileAssembly: Wrote chunk " + i + " (" + chunkData.length + " bytes)");
                    } else {
                        System.err.println("FileAssembly: Missing chunk " + i);
                        throw new IOException("Missing chunk " + i);
                    }
                }
            }
            
            System.out.println("FileAssembly: File saved successfully. Size: " + outputFile.length() + " bytes");
            return outputFile;
        }
    }
} 