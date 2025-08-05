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
            System.out.println("FileChunkReceiver is listening for optimized LAN connections...");
            while (true) {
                try {
                    System.out.println("FileChunkReceiver waiting for client connection...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("FileChunkReceiver accepted connection from: " + clientSocket.getInetAddress());
                    new Thread(() -> handleOptimizedTransfer(clientSocket)).start();
                } catch (Exception e) {
                    System.err.println("Error accepting optimized transfer connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("FileChunkReceiver could not start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleOptimizedTransfer(Socket clientSocket) {
        try (BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream(), 64 * 1024);
             DataInputStream dis = new DataInputStream(bis);
             BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream(), 64 * 1024);
             DataOutputStream dos = new DataOutputStream(bos)) {

            String command = dis.readUTF();
            
            if (command.equals("DOWNLOAD")) {
                // Handle download request
                String fileID = dis.readUTF();
                handleOptimizedDownload(fileID, dos);
            } else if (command.equals("UPLOAD")) {
                // Handle upload request
                handleOptimizedUpload(dis, dos);
            }

        } catch (Exception e) {
            System.err.println("Error during optimized transfer: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void handleOptimizedUpload(DataInputStream dis, DataOutputStream dos) {
        try {
            // Read file metadata
            String fileID = dis.readUTF();
            String originalFileName = dis.readUTF();
            long fileSize = dis.readLong();
            int totalChunks = dis.readInt();
            
            System.out.println("FileChunkReceiver: Starting optimized upload for file ID: " + fileID);
            System.out.println("FileChunkReceiver: Original filename: " + originalFileName);
            System.out.println("FileChunkReceiver: Total chunks expected: " + totalChunks);
            System.out.println("FileChunkReceiver: File size: " + formatFileSize(fileSize));
            
            // Check available disk space
            File outputFile = new File(fileDirectory, fileID);
            long availableSpace = new File(fileDirectory).getFreeSpace();
            if (availableSpace < fileSize) {
                String errorMsg = "Insufficient disk space. Available: " + formatFileSize(availableSpace) + ", Required: " + formatFileSize(fileSize);
                System.err.println("FileChunkReceiver: " + errorMsg);
                dos.writeUTF("ERROR");
                dos.writeUTF(errorMsg);
                dos.flush();
                return;
            }
            
            // Stream directly to file instead of storing in memory
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                long totalBytesReceived = 0;
                
                // Receive all chunks and write directly to file
                for (int i = 0; i < totalChunks; i++) {
                    // Read chunk header
                    int chunkNumber = dis.readInt();
                    int chunkSize = dis.readInt();
                    boolean isLastChunk = dis.readBoolean();
                    
                    // Read chunk data
                    byte[] chunkData = new byte[chunkSize];
                    int totalBytesRead = 0;
                    while (totalBytesRead < chunkSize) {
                        int bytesRead = dis.read(chunkData, totalBytesRead, chunkSize - totalBytesRead);
                        if (bytesRead == -1) {
                            throw new IOException("Unexpected end of stream at chunk " + chunkNumber);
                        }
                        totalBytesRead += bytesRead;
                    }
                    
                    // Write chunk data directly to file
                    fos.write(chunkData, 0, totalBytesRead);
                    fos.flush();
                    
                    totalBytesReceived += totalBytesRead;
                    
                    System.out.println("Received optimized chunk " + chunkNumber + "/" + totalChunks + 
                                     " for file: " + originalFileName + " (" + totalBytesRead + " bytes)" +
                                     " - Progress: " + String.format("%.1f%%", (double) totalBytesReceived / fileSize * 100));
                    
                    if (isLastChunk) break;
                }
            }
            
            // Verify file size
            if (outputFile.length() != fileSize) {
                String errorMsg = "File size mismatch. Expected: " + fileSize + ", Actual: " + outputFile.length();
                System.err.println("FileChunkReceiver: " + errorMsg);
                outputFile.delete(); // Clean up incomplete file
                dos.writeUTF("ERROR");
                dos.writeUTF(errorMsg);
                dos.flush();
                return;
            }
            
            System.out.println("File saved successfully: " + outputFile.getAbsolutePath());
            System.out.println("FileChunkReceiver: Upload completed successfully for file: " + originalFileName);

        } catch (Exception e) {
            System.err.println("Error during optimized upload: " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("ERROR");
                dos.writeUTF("Upload failed: " + e.getMessage());
                dos.flush();
            } catch (IOException ex) {
                // ignore
            }
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void handleOptimizedDownload(String fileID, DataOutputStream dos) {
        try {
            System.out.println("FileChunkReceiver: Handling optimized download for file ID: " + fileID);
            
            // Find the file
            File file = new File(fileDirectory, fileID);
            if (!file.exists()) {
                dos.writeUTF("ERROR");
                dos.writeUTF("File not found");
                dos.flush();
                return;
            }

            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / FileChunkSender.CHUNK_SIZE);
            
            System.out.println("FileChunkReceiver: Sending file: " + file.getName() + " (" + fileSize + " bytes) in " + totalChunks + " chunks");
            
            // Send file metadata
            dos.writeUTF("OK");
            dos.writeLong(fileSize);
            dos.writeInt(totalChunks);
            dos.flush();

            // Send file in chunks
            try (BufferedInputStream fileBis = new BufferedInputStream(new FileInputStream(file), 64 * 1024)) {
                byte[] buffer = new byte[FileChunkSender.CHUNK_SIZE];
                int chunkNumber = 0;
                int bytesRead;

                while ((bytesRead = fileBis.read(buffer)) != -1) {
                    chunkNumber++;
                    boolean isLastChunk = (chunkNumber == totalChunks);

                    // Send chunk header
                    dos.writeInt(chunkNumber);
                    dos.writeInt(bytesRead);
                    dos.writeBoolean(isLastChunk);
                    
                    // Send chunk data
                    dos.write(buffer, 0, bytesRead);
                    dos.flush();

                    System.out.println("Sent optimized chunk " + chunkNumber + "/" + totalChunks + " (" + bytesRead + " bytes)");
                }
            }

            System.out.println("Optimized download completed successfully");

        } catch (Exception e) {
            System.err.println("Error during optimized download: " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("ERROR");
                dos.writeUTF("Download failed: " + e.getMessage());
                dos.flush();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    // Legacy method for backward compatibility (if needed)
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
            System.out.println("FileChunkReceiver: Starting legacy upload for file ID: " + fileID);
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
                    
                    System.out.println("Received legacy chunk " + chunk.getChunkNumber() + "/" + chunk.getTotalChunks() + 
                                     " for file: " + chunk.getOriginalFileName());
                }
            }
            
            // Save file when complete
            if (assembly.isComplete()) {
                File savedFile = assembly.saveToFile(fileDirectory);
                System.out.println("File saved successfully: " + savedFile.getAbsolutePath());
                
                // Clean up assembly
                fileAssemblies.remove(fileID);
            } else {
                System.err.println("File assembly incomplete for file ID: " + fileID);
            }

        } catch (Exception e) {
            System.err.println("Error during legacy upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChunkedDownload(String fileID, ObjectOutputStream oos) {
        try {
            System.out.println("FileChunkReceiver: Handling legacy download for file ID: " + fileID);
            
            // Find the file
            File file = new File(fileDirectory, fileID);
            if (!file.exists()) {
                oos.writeObject("ERROR:File not found");
                oos.flush();
                return;
            }

            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / FileChunkSender.CHUNK_SIZE);
            
            System.out.println("FileChunkReceiver: Sending file: " + file.getName() + " (" + fileSize + " bytes) in " + totalChunks + " chunks");

            // Send file in chunks
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

                    // Send chunk
                    oos.writeObject(chunk);
                    oos.flush();

                    System.out.println("Sent legacy chunk " + chunkNumber + "/" + totalChunks + " (" + bytesRead + " bytes)");
                }
            }

            System.out.println("Legacy download completed successfully");

        } catch (Exception e) {
            System.err.println("Error during legacy download: " + e.getMessage());
            e.printStackTrace();
            try {
                oos.writeObject("ERROR:" + e.getMessage());
                oos.flush();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

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
            if (chunks.size() == totalChunks) {
                complete = true;
            }
        }

        public boolean isComplete() {
            return complete;
        }

        public File saveToFile(String directory) throws IOException {
            File outputFile = new File(directory, fileID);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                for (int i = 1; i <= totalChunks; i++) {
                    byte[] chunkData = chunks.get(i);
                    if (chunkData != null) {
                        fos.write(chunkData);
                    }
                }
            }
            return outputFile;
        }
    }
} 