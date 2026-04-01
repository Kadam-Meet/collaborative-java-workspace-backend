package com.collab.workspace.dto;

import java.time.LocalDateTime;

public class RoomSummary {

private Long id;
private String roomCode;
private String roomName;
private LocalDateTime createdAt;
private String ownerEmail;
private long memberCount;
private long fileCount;

public RoomSummary(Long id, String roomCode, String roomName,
                   LocalDateTime createdAt, String ownerEmail,
                   long memberCount, long fileCount) {
    this.id = id;
    this.roomCode = roomCode;
    this.roomName = roomName;
    this.createdAt = createdAt;
    this.ownerEmail = ownerEmail;
    this.memberCount = memberCount;
    this.fileCount = fileCount;
}

public Long getId() { return id; }
public String getRoomCode() { return roomCode; }
public String getRoomName() { return roomName; }
public LocalDateTime getCreatedAt() { return createdAt; }
public String getOwnerEmail() { return ownerEmail; }
public long getMemberCount() { return memberCount; }
public long getFileCount() { return fileCount; }

}
