package com.collab.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "solo_workspace_versions")
public class SoloWorkspaceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "solo_workspace_id", nullable = false)
    private SoloWorkspace soloWorkspace;

    @ManyToOne
    @JoinColumn(name = "saved_by", nullable = false)
    private User savedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public SoloWorkspace getSoloWorkspace() { return soloWorkspace; }
    public void setSoloWorkspace(SoloWorkspace soloWorkspace) { this.soloWorkspace = soloWorkspace; }

    public User getSavedBy() { return savedBy; }
    public void setSavedBy(User savedBy) { this.savedBy = savedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoloWorkspaceVersion)) return false;
        SoloWorkspaceVersion that = (SoloWorkspaceVersion) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}