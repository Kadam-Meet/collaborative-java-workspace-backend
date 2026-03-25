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
}
