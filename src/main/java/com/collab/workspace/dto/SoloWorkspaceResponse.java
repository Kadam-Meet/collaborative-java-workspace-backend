package com.collab.workspace.dto;

import java.time.LocalDateTime;

public class SoloWorkspaceResponse {

    private Long id;
    private String fileName;
    private String content;
    private String contentPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SoloWorkspaceResponse(Long id, String fileName, String content, String contentPreview, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fileName = fileName;
        this.content = content;
        this.contentPreview = contentPreview;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getContent() { return content; }
    public String getContentPreview() { return contentPreview; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}