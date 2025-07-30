package com.bakbak.javafx_proj_1_2;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FileChunkSender {
    public static final int CHUNK_SIZE = 512 * 1024; // 512KB chunks
    private final String serverHost;
    private final int serverPort;

    public FileChunkSender(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public CompletableFuture<String> sendFile(File file, ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String fileID = UUID.randomUUID().toString();
                long fileSize = file.length();
                int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

                System.out.println("Sending file: " + file.getName() + " (" + fileSize + " bytes) in " + totalChunks + " chunks");
                System.out.println("Connecting to " + serverHost + ":" + serverPort + " for chunked transfer...");

                try (Socket socket = new Socket(serverHost, serverPort);
                     ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                     FileInputStream fis = new FileInputStream(file)) {

                    System.out.println("Successfully connected to chunked transfer server");
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int chunkNumber = 0;
                    int bytesRead;
                    long totalBytesProcessed = 0;

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

                        totalBytesProcessed += bytesRead;
                        
                        // Update progress
                        if (progressCallback != null) {
                            progressCallback.onProgressUpdate(chunkNumber, totalChunks, totalBytesProcessed, fileSize);
                        }

                        System.out.println("Sent chunk " + chunkNumber + "/" + totalChunks + " (" + bytesRead + " bytes)");

                        // Small delay to prevent overwhelming the server
                        Thread.sleep(10);
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

    // Overloaded method for backward compatibility
    public CompletableFuture<String> sendFile(File file) {
        return sendFile(file, null);
    }

    public CompletableFuture<File> downloadFile(String fileID, String originalFileName, ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Requesting download for file: " + originalFileName + " (ID: " + fileID + ")");

                try (Socket socket = new Socket(serverHost, serverPort);
                     ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    // Send download request
                    oos.writeObject("DOWNLOAD:" + fileID);
                    oos.flush();

                    // Create downloads directory
                    File downloadsDir = new File("downloads");
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }

                    File outputFile = new File(downloadsDir, originalFileName);
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {

                        long totalBytesProcessed = 0;
                        long totalFileSize = 0;
                        int totalChunks = 0;

                        // Receive chunks
                        while (true) {
                            Object obj = ois.readObject();
                            if (obj instanceof FileChunk) {
                                FileChunk chunk = (FileChunk) obj;
                                
                                // Get file size and total chunks from first chunk
                                if (chunk.getChunkNumber() == 1) {
                                    totalFileSize = chunk.getTotalFileSize();
                                    totalChunks = chunk.getTotalChunks();
                                }
                                
                                // Write chunk data to file
                                fos.write(chunk.getData());
                                fos.flush();

                                totalBytesProcessed += chunk.getData().length;
                                
                                // Update progress
                                if (progressCallback != null) {
                                    progressCallback.onProgressUpdate(chunk.getChunkNumber(), totalChunks, totalBytesProcessed, totalFileSize);
                                }

                                System.out.println("Received chunk " + chunk.getChunkNumber() + "/" + chunk.getTotalChunks());

                                if (chunk.isLastChunk()) {
                                    break;
                                }
                            } else if (obj instanceof String && ((String) obj).startsWith("ERROR:")) {
                                String errorMsg = "Server error: " + obj;
                                if (progressCallback != null) {
                                    progressCallback.onTransferError(errorMsg);
                                }
                                throw new IOException(errorMsg);
                            }
                        }
                    }

                    // Transfer complete
                    if (progressCallback != null) {
                        progressCallback.onTransferComplete();
                    }

                    System.out.println("File download completed: " + outputFile.getAbsolutePath());
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

    // Overloaded method for backward compatibility
    public CompletableFuture<File> downloadFile(String fileID, String originalFileName) {
        return downloadFile(fileID, originalFileName, null);
    }
} 