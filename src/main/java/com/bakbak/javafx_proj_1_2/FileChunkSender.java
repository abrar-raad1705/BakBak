package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FileChunkSender {
    // Optimized for LAN: 5MB chunks (10x larger than before)
    public static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks
    private final String serverHost;
    private final int serverPort;

    public FileChunkSender(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public CompletableFuture<String> sendFile(File file, ProgressCallback progressCallback) {
        return sendFile(file, UUID.randomUUID().toString(), progressCallback);
    }

    public CompletableFuture<String> sendFile(File file, String fileID, ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long fileSize = file.length();
                int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

                System.out.println("Sending file: " + file.getName() + " (" + formatFileSize(fileSize) + ") in " + totalChunks + " chunks");
                System.out.println("Connecting to " + serverHost + ":" + serverPort + " for optimized LAN transfer...");

                try (Socket socket = new Socket(serverHost, serverPort)) {
                    // Optimize socket buffers for large files
                    socket.setSendBufferSize(1024 * 1024); // 1MB send buffer
                    socket.setReceiveBufferSize(1024 * 1024); // 1MB receive buffer
                    socket.setTcpNoDelay(true); // Disable Nagle's algorithm for better performance
                    socket.setSoTimeout(300000); // 5 minute timeout for large files
                    
                    try (BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024);
                         DataOutputStream dos = new DataOutputStream(bos);
                         BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), 64 * 1024)) {

                        System.out.println("Successfully connected to optimized transfer server");
                        
                        // Send file metadata first
                        dos.writeUTF("UPLOAD");
                        dos.writeUTF(fileID);
                        dos.writeUTF(file.getName());
                        dos.writeLong(fileSize);
                        dos.writeInt(totalChunks);
                        dos.flush();

                        byte[] buffer = new byte[CHUNK_SIZE];
                        int chunkNumber = 0;
                        int bytesRead;
                        long totalBytesProcessed = 0;

                        while ((bytesRead = bis.read(buffer)) != -1) {
                            chunkNumber++;
                            boolean isLastChunk = (chunkNumber == totalChunks);

                            // Send chunk header
                            dos.writeInt(chunkNumber);
                            dos.writeInt(bytesRead);
                            dos.writeBoolean(isLastChunk);
                            
                            // Send chunk data directly (no object serialization overhead)
                            dos.write(buffer, 0, bytesRead);
                            dos.flush();

                            totalBytesProcessed += bytesRead;
                            
                            // Update progress
                            if (progressCallback != null) {
                                progressCallback.onProgressUpdate(chunkNumber, totalChunks, totalBytesProcessed, fileSize);
                            }

                            System.out.println("Sent chunk " + chunkNumber + "/" + totalChunks + " (" + bytesRead + " bytes)" +
                                             " - Progress: " + String.format("%.1f%%", (double) totalBytesProcessed / fileSize * 100));
                            
                            // No artificial delays for LAN optimization
                        }

                        // Transfer complete
                        if (progressCallback != null) {
                            progressCallback.onTransferComplete();
                        }

                        System.out.println("File upload completed successfully");
                        return fileID;

                    } catch (Exception e) {
                        String errorMsg = "Error sending file: " + e.getMessage();
                        System.err.println(errorMsg);
                        e.printStackTrace();
                        
                        if (progressCallback != null) {
                            progressCallback.onTransferError(errorMsg);
                        }
                        
                        return null;
                    }
                } catch (Exception e) {
                    String errorMsg = "Error connecting to server: " + e.getMessage();
                    System.err.println(errorMsg);
                    e.printStackTrace();
                    
                    if (progressCallback != null) {
                        progressCallback.onTransferError(errorMsg);
                    }
                    
                    return null;
                }
            } catch (Exception e) {
                String errorMsg = "Error preparing file for upload: " + e.getMessage();
                System.err.println(errorMsg);
                e.printStackTrace();
                
                if (progressCallback != null) {
                    progressCallback.onTransferError(errorMsg);
                }
                
                return null;
            }
        });
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public CompletableFuture<String> sendFile(File file) {
        return sendFile(file, null);
    }

    public CompletableFuture<File> downloadFile(String fileID, String originalFileName, ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Requesting download for file: " + originalFileName + " (ID: " + fileID + ")");
                System.out.println("Connecting to " + serverHost + ":" + serverPort + " for optimized download...");

                try (Socket socket = new Socket(serverHost, serverPort)) {
                    // Optimize socket buffers for large files
                    socket.setSendBufferSize(1024 * 1024); // 1MB send buffer
                    socket.setReceiveBufferSize(1024 * 1024); // 1MB receive buffer
                    socket.setTcpNoDelay(true); // Disable Nagle's algorithm for better performance
                    socket.setSoTimeout(300000); // 5 minute timeout for large files
                    
                    try (BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024);
                         DataOutputStream dos = new DataOutputStream(bos);
                         BufferedInputStream bis = new BufferedInputStream(socket.getInputStream(), 64 * 1024);
                         DataInputStream dis = new DataInputStream(bis)) {

                        System.out.println("Successfully connected for download");
                        
                        // Send download request
                        dos.writeUTF("DOWNLOAD");
                        dos.writeUTF(fileID);
                        dos.flush();

                        // Read file metadata
                        String response = dis.readUTF();
                        if (response.equals("ERROR")) {
                            String errorMsg = dis.readUTF();
                            throw new IOException("Server error: " + errorMsg);
                        }

                        long fileSize = dis.readLong();
                        int totalChunks = dis.readInt();
                        
                        System.out.println("Downloading file: " + originalFileName + " (" + formatFileSize(fileSize) + ") in " + totalChunks + " chunks");

                        // Create downloads directory if it doesn't exist
                        File downloadsDir = new File("downloads");
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs();
                        }

                        // Check available disk space
                        long availableSpace = downloadsDir.getFreeSpace();
                        if (availableSpace < fileSize) {
                            throw new IOException("Insufficient disk space. Available: " + formatFileSize(availableSpace) + ", Required: " + formatFileSize(fileSize));
                        }

                        File outputFile = new File(downloadsDir, originalFileName);
                        
                        try (BufferedOutputStream fileBos = new BufferedOutputStream(new FileOutputStream(outputFile), 64 * 1024)) {
                            byte[] buffer = new byte[CHUNK_SIZE];
                            int chunkNumber = 0;
                            long totalBytesReceived = 0;

                            while (chunkNumber < totalChunks) {
                                // Read chunk header
                                int receivedChunkNumber = dis.readInt();
                                int chunkSize = dis.readInt();
                                boolean isLastChunk = dis.readBoolean();
                                
                                // Read chunk data
                                int bytesRead = 0;
                                int totalBytesRead = 0;
                                while (totalBytesRead < chunkSize) {
                                    bytesRead = dis.read(buffer, totalBytesRead, chunkSize - totalBytesRead);
                                    if (bytesRead == -1) {
                                        throw new IOException("Unexpected end of stream at chunk " + receivedChunkNumber);
                                    }
                                    totalBytesRead += bytesRead;
                                }

                                // Write to file
                                fileBos.write(buffer, 0, totalBytesRead);
                                fileBos.flush();

                                chunkNumber = receivedChunkNumber;
                                totalBytesReceived += totalBytesRead;
                                
                                // Update progress
                                if (progressCallback != null) {
                                    progressCallback.onProgressUpdate(chunkNumber, totalChunks, totalBytesReceived, fileSize);
                                }

                                System.out.println("Received chunk " + chunkNumber + "/" + totalChunks + " (" + totalBytesRead + " bytes)" +
                                                 " - Progress: " + String.format("%.1f%%", (double) totalBytesReceived / fileSize * 100));

                                if (isLastChunk) break;
                            }
                        }

                        // Verify file size
                        if (outputFile.length() != fileSize) {
                            outputFile.delete(); // Clean up incomplete file
                            throw new IOException("File size mismatch. Expected: " + fileSize + ", Actual: " + outputFile.length());
                        }

                        // Download complete
                        if (progressCallback != null) {
                            progressCallback.onTransferComplete();
                        }

                        System.out.println("File download completed successfully");
                        return outputFile;

                    } catch (Exception e) {
                        String errorMsg = "Error downloading file: " + e.getMessage();
                        System.err.println(errorMsg);
                        e.printStackTrace();
                        
                        if (progressCallback != null) {
                            progressCallback.onTransferError(errorMsg);
                        }
                        
                        throw new RuntimeException("Download failed", e);
                    }
                } catch (Exception e) {
                    String errorMsg = "Error connecting for download: " + e.getMessage();
                    System.err.println(errorMsg);
                    e.printStackTrace();
                    
                    if (progressCallback != null) {
                        progressCallback.onTransferError(errorMsg);
                    }
                    
                    throw new RuntimeException("Download connection failed", e);
                }
            } catch (Exception e) {
                String errorMsg = "Error preparing download: " + e.getMessage();
                System.err.println(errorMsg);
                e.printStackTrace();
                
                if (progressCallback != null) {
                    progressCallback.onTransferError(errorMsg);
                }
                
                throw new RuntimeException("Download preparation failed", e);
            }
        });
    }

    public CompletableFuture<File> downloadFile(String fileID, String originalFileName) {
        return downloadFile(fileID, originalFileName, null);
    }
} 