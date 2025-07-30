package com.bakbak.javafx_proj_1_2;

import java.io.Serializable;

public class FileChunk implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    
    private byte[] data;
    private int chunkNumber;
    private boolean isLastChunk;
    private String fileID;
    private String originalFileName;
    private long totalFileSize;
    private int totalChunks;

    public FileChunk(byte[] data, int chunkNumber, boolean isLastChunk, String fileID, 
                    String originalFileName, long totalFileSize, int totalChunks) {
        this.data = data;
        this.chunkNumber = chunkNumber;
        this.isLastChunk = isLastChunk;
        this.fileID = fileID;
        this.originalFileName = originalFileName;
        this.totalFileSize = totalFileSize;
        this.totalChunks = totalChunks;
    }

    // Getters
    public byte[] getData() { return data; }
    public int getChunkNumber() { return chunkNumber; }
    public boolean isLastChunk() { return isLastChunk; }
    public String getFileID() { return fileID; }
    public String getOriginalFileName() { return originalFileName; }
    public long getTotalFileSize() { return totalFileSize; }
    public int getTotalChunks() { return totalChunks; }

    // Setters
    public void setData(byte[] data) { this.data = data; }
    public void setChunkNumber(int chunkNumber) { this.chunkNumber = chunkNumber; }
    public void setLastChunk(boolean lastChunk) { isLastChunk = lastChunk; }
    public void setFileID(String fileID) { this.fileID = fileID; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public void setTotalFileSize(long totalFileSize) { this.totalFileSize = totalFileSize; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }

    @Override
    public String toString() {
        return "FileChunk{" +
                "chunkNumber=" + chunkNumber +
                ", isLastChunk=" + isLastChunk +
                ", fileID='" + fileID + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", dataSize=" + (data != null ? data.length : 0) + " bytes" +
                ", totalChunks=" + totalChunks +
                '}';
    }
} 