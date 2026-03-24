package com.collab.workspace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "versions")
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int versionNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "file_id")
    private WorkspaceFile file;

    @ManyToOne
    @JoinColumn(name = "saved_by")
    private User savedBy;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public WorkspaceFile getFile() { return file; }
    public void setFile(WorkspaceFile file) { this.file = file; }

    public User getSavedBy() { return savedBy; }
    public void setSavedBy(User savedBy) { this.savedBy = savedBy; }

    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version version = (Version) o;
        return id != null && id.equals(version.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}