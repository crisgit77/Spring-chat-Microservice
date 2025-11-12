package com.example.demo.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime timestamp;
    private String messageType; // "CHAT", "JOIN", "LEAVE"

    public ChatMessage() {}

    public ChatMessage(String sender, String receiver, String content, String messageType) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
