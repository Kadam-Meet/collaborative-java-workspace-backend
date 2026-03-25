package com.collab.workspace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "room_members")
public class RoomMember {

    @EmbeddedId
    private RoomMemberId id;

    @ManyToOne
    @MapsId("roomId")
    private Room room;

    @ManyToOne
    @MapsId("userId")
    private User user;

    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean canEditFiles = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean canSaveVersions = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canRevertVersions = false;

    public RoomMemberId getId() { return id; }
    public void setId(RoomMemberId id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public boolean isCanEditFiles() { return canEditFiles; }
    public void setCanEditFiles(boolean canEditFiles) { this.canEditFiles = canEditFiles; }

    public boolean isCanSaveVersions() { return canSaveVersions; }
    public void setCanSaveVersions(boolean canSaveVersions) { this.canSaveVersions = canSaveVersions; }

    public boolean isCanRevertVersions() { return canRevertVersions; }
    public void setCanRevertVersions(boolean canRevertVersions) { this.canRevertVersions = canRevertVersions; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomMember)) return false;
        RoomMember that = (RoomMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}