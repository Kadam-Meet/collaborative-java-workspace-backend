package com.collab.workspace.dto;

import java.time.LocalDateTime;

public class NotificationResponse {

private Long id;
private String type;
private String title;
private String message;
private boolean read;
private Long roomId;
private String roomCode;
private String roomName;
private String actionType;
private String actionToken;
private LocalDateTime createdAt;
private LocalDateTime readAt;

public NotificationResponse() {}

public NotificationResponse(Long id,
                            String type,
                            String title,
                            String message,
                            boolean read,
                            Long roomId,
                            String roomCode,
                            String roomName,
                            String actionType,
                            String actionToken,
                            LocalDateTime createdAt,
                            LocalDateTime readAt) {
    this.id = id;
    this.type = type;
    this.title = title;
    this.message = message;
    this.read = read;
    this.roomId = roomId;
    this.roomCode = roomCode;
    this.roomName = roomName;
    this.actionType = actionType;
    this.actionToken = actionToken;
    this.createdAt = createdAt;
    this.readAt = readAt;
}

// Getters
public Long getId() { return id; }
public String getType() { return type; }
public String getTitle() { return title; }
public String getMessage() { return message; }
public boolean isRead() { return read; }
public Long getRoomId() { return roomId; }
public String getRoomCode() { return roomCode; }
public String getRoomName() { return roomName; }
public String getActionType() { return actionType; }
public String getActionToken() { return actionToken; }
public LocalDateTime getCreatedAt() { return createdAt; }
public LocalDateTime getReadAt() { return readAt; }

// Setters
public void setId(Long id) { this.id = id; }
public void setType(String type) { this.type = type; }
public void setTitle(String title) { this.title = title; }
public void setMessage(String message) { this.message = message; }
public void setRead(boolean read) { this.read = read; }
public void setRoomId(Long roomId) { this.roomId = roomId; }
public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
public void setRoomName(String roomName) { this.roomName = roomName; }
public void setActionType(String actionType) { this.actionType = actionType; }
public void setActionToken(String actionToken) { this.actionToken = actionToken; }
public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }


}
