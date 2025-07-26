package com.bakbak.javafx_proj_1_2;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    public enum MessageType {
        LOGIN,
        LOGOUT,
        REGISTER,
        PRIVATE_MESSAGE,
        GROUP_MESSAGE,
        JOIN_GROUP,
        LEAVE_GROUP,
        CREATE_GROUP,
        USER_LIST,
        USER_SEARCH,
        GROUP_LIST,
        SYNC_HISTORY,
        OFFLINE_MESSAGES,
        ACKNOWLEDGMENT,
        /* ERROR */
    }

    private MessageType type;
    private String sender;
    private String recipient;
    private String content;
    private String groupId;
    private LocalDateTime timestamp;
    private boolean isSuccess;
    private String errorMessage;

    public Message(MessageType type, String sender) {
        this.type = type;
        this.sender = sender;
        timestamp = LocalDateTime.now();
        isSuccess = true;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    /* public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    } */

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", recipient='" + recipient + '\'' +
                ", content='" + content + '\'' +
                ", groupId='" + groupId + '\'' +
                ", timestamp=" + timestamp +
                ", isSuccess=" + isSuccess +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}