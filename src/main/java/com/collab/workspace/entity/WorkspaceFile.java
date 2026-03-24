package com.collab.workspace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "workspace_files",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "file_path"}))
public class WorkspaceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filePath;
    private String language;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "file")
    private List<Version> versions;

    @OneToMany(mappedBy = "file")
    private List<AnalysisReport> reports;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }

    public List<Version> getVersions() { return versions; }
    public void setVersions(List<Version> versions) { this.versions = versions; }

    public List<AnalysisReport> getReports() { return reports; }
    public void setReports(List<AnalysisReport> reports) { this.reports = reports; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceFile)) return false;
        WorkspaceFile that = (WorkspaceFile) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}