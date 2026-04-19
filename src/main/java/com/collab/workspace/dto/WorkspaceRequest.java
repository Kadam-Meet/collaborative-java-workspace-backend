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
    private String invitationToken;
    private String filePath;
    private String folderPath;
    private String newFolderPath;
    private String language;
    private String content;
    private String name;
    private String password;
    private String headline;
    private String bio;
    private String location;
    private String accentColor;

    private String versionMessage;
    private String expectedUpdatedAt;
    private Boolean collaborativeSave;
    private Long fileId;
    private Integer startLine;
    private Integer startColumn;
    private Integer endLine;
    private Integer endColumn;
    private Boolean typing;
    private Boolean notifyOnCompletion;
    private Boolean canEditFiles;
    private Boolean canSaveVersions;
    private Boolean canRevertVersions;
    private String memberRole;
    private String searchQuery;
    private String activityType;
    private String activityActorEmail;
    private String activityFrom;
    private String activityTo;
    private Boolean resolved;
    private Boolean profilePublic;
    private Boolean emailNotifications;
    private Boolean workspaceDigest;
    private Boolean focusModeEnabled;

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

    public String getInvitationToken() {
        return invitationToken;
    }

    public void setInvitationToken(String invitationToken) {
        this.invitationToken = invitationToken;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getNewFolderPath() {
        return newFolderPath;
    }

    public void setNewFolderPath(String newFolderPath) {
        this.newFolderPath = newFolderPath;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
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

    public Boolean getCollaborativeSave() {
        return collaborativeSave;
    }

    public void setCollaborativeSave(Boolean collaborativeSave) {
        this.collaborativeSave = collaborativeSave;
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

    public Boolean getNotifyOnCompletion() {
        return notifyOnCompletion;
    }

    public void setNotifyOnCompletion(Boolean notifyOnCompletion) {
        this.notifyOnCompletion = notifyOnCompletion;
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

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityActorEmail() {
        return activityActorEmail;
    }

    public void setActivityActorEmail(String activityActorEmail) {
        this.activityActorEmail = activityActorEmail;
    }

    public String getActivityFrom() {
        return activityFrom;
    }

    public void setActivityFrom(String activityFrom) {
        this.activityFrom = activityFrom;
    }

    public String getActivityTo() {
        return activityTo;
    }

    public void setActivityTo(String activityTo) {
        this.activityTo = activityTo;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public Boolean getProfilePublic() {
        return profilePublic;
    }

    public void setProfilePublic(Boolean profilePublic) {
        this.profilePublic = profilePublic;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getWorkspaceDigest() {
        return workspaceDigest;
    }

    public void setWorkspaceDigest(Boolean workspaceDigest) {
        this.workspaceDigest = workspaceDigest;
    }

    public Boolean getFocusModeEnabled() {
        return focusModeEnabled;
    }

    public void setFocusModeEnabled(Boolean focusModeEnabled) {
        this.focusModeEnabled = focusModeEnabled;
    }
}
