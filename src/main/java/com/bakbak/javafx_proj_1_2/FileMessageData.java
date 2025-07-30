package com.bakbak.javafx_proj_1_2;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

public class FileMessageData implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private static final String FILE_REFERENCE_PREFIX = "FILE_REFERENCE::";

    private final String originalName;
    private final String uuidName;
    private final long fileSize;
    private final String mimeType;

    public FileMessageData(String originalName, String uuidName, long fileSize, String mimeType) {
        this.originalName = originalName;
        this.uuidName = uuidName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getUuidName() {
        return uuidName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String toString() {
        return FILE_REFERENCE_PREFIX + String.join("::", originalName, uuidName, String.valueOf(fileSize), mimeType);
    }

    public static boolean isAFileReference(String content) {
        return content != null && content.startsWith(FILE_REFERENCE_PREFIX);
    }

    public static FileMessageData fromString(String content) {
        if (!isAFileReference(content)) {
            throw new IllegalArgumentException("Content is not a valid file reference.");
        }
        String[] parts = content.substring(FILE_REFERENCE_PREFIX.length()).split("::");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid file reference format: " + content);
        }
        String originalName = parts[0];
        String uuidName = parts[1];
        long fileSize = Long.parseLong(parts[2]);
        String mimeType = parts[3];
        return new FileMessageData(originalName, uuidName, fileSize, mimeType);
    }

    public static FileMessageData fromFile(File file) {
        try {
            String originalName = file.getName();
            String extension = "";
            int i = originalName.lastIndexOf('.');
            if (i > 0) {
                extension = originalName.substring(i);
            }
            String uuidName = UUID.randomUUID() + extension;
            long fileSize = file.length();
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            return new FileMessageData(originalName, uuidName, fileSize, mimeType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMessageData that = (FileMessageData) o;
        return fileSize == that.fileSize &&
                originalName.equals(that.originalName) &&
                uuidName.equals(that.uuidName) &&
                Objects.equals(mimeType, that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalName, uuidName, fileSize, mimeType);
    }
} 