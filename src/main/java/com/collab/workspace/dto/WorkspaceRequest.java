package com.collab.workspace.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkspaceRequest {

    private String workspaceName;
    private String entryFile;
    private Map<String, String> files = new LinkedHashMap<>();
    private boolean applySuggestedFixes;

    // Room/workspace management fields
    private String roomName;
    private String roomCode;
    private String memberEmail;
    private String filePath;
    private String language;
    private String content;

    private String versionMessage;
    private String expectedUpdatedAt;
    private Long fileId;
    private Integer startLine;
    private Integer startColumn;
    private Integer endLine;
    private Integer endColumn;
    private Boolean typing;
    private Boolean canEditFiles;
    private Boolean canSaveVersions;
    private Boolean canRevertVersions;

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getEntryFile() {
        return entryFile;
    }

    public void setEntryFile(String entryFile) {
        this.entryFile = entryFile;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    public boolean isApplySuggestedFixes() {
        return applySuggestedFixes;
    }

    public void setApplySuggestedFixes(boolean applySuggestedFixes) {
        this.applySuggestedFixes = applySuggestedFixes;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVersionMessage() {
        return versionMessage;
    }

    public void setVersionMessage(String versionMessage) {
        this.versionMessage = versionMessage;
    }

    public String getExpectedUpdatedAt() {
        return expectedUpdatedAt;
    }

    public void setExpectedUpdatedAt(String expectedUpdatedAt) {
        this.expectedUpdatedAt = expectedUpdatedAt;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getStartColumn() {
        return startColumn;
    }

    public void setStartColumn(Integer startColumn) {
        this.startColumn = startColumn;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Integer getEndColumn() {
        return endColumn;
    }

    public void setEndColumn(Integer endColumn) {
        this.endColumn = endColumn;
    }

    public Boolean getTyping() {
        return typing;
    }

    public void setTyping(Boolean typing) {
        this.typing = typing;
    }

    public Boolean getCanEditFiles() {
        return canEditFiles;
    }

    public void setCanEditFiles(Boolean canEditFiles) {
        this.canEditFiles = canEditFiles;
    }

    public Boolean getCanSaveVersions() {
        return canSaveVersions;
    }

    public void setCanSaveVersions(Boolean canSaveVersions) {
        this.canSaveVersions = canSaveVersions;
    }

    public Boolean getCanRevertVersions() {
        return canRevertVersions;
    }

    public void setCanRevertVersions(Boolean canRevertVersions) {
        this.canRevertVersions = canRevertVersions;
    }
}
