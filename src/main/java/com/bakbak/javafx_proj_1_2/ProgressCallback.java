package com.bakbak.javafx_proj_1_2;

/**
 * Callback interface for tracking file transfer progress
 */
public interface ProgressCallback {
    /**
     * Called when progress is updated during file transfer
     * @param currentChunk Current chunk number being processed
     * @param totalChunks Total number of chunks
     * @param bytesProcessed Total bytes processed so far
     * @param totalBytes Total file size in bytes
     */
    void onProgressUpdate(int currentChunk, int totalChunks, long bytesProcessed, long totalBytes);
    
    /**
     * Called when the transfer is completed
     */
    void onTransferComplete();
    
    /**
     * Called when an error occurs during transfer
     * @param error Error message
     */
    void onTransferError(String error);
} 