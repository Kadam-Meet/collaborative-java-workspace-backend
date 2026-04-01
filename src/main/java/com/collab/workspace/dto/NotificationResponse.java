package com.collab.workspace.dto;

import java.time.LocalDateTime;

public class NotificationResponse {

private Long id;
private String title;
private String message;
private boolean read;
private String roomCode;
private LocalDateTime createdAt;
private LocalDateTime readAt;

public NotificationResponse() {}

public NotificationResponse(Long id,
                            String title,
                            String message,
                            boolean read,
                            String roomCode,
                            LocalDateTime createdAt,
                            LocalDateTime readAt) {
    this.id = id;
    this.title = title;
    this.message = message;
    this.read = read;
    this.roomCode = roomCode;
    this.createdAt = createdAt;
    this.readAt = readAt;
}

// Getters
public Long getId() { return id; }
public String getTitle() { return title; }
public String getMessage() { return message; }
public boolean isRead() { return read; }
public String getRoomCode() { return roomCode; }
public LocalDateTime getCreatedAt() { return createdAt; }
public LocalDateTime getReadAt() { return readAt; }

// Setters
public void setId(Long id) { this.id = id; }
public void setTitle(String title) { this.title = title; }
public void setMessage(String message) { this.message = message; }
public void setRead(boolean read) { this.read = read; }
public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }


}
