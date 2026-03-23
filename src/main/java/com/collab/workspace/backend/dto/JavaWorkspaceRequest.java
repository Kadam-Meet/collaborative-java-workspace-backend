package com.collab.workspace.backend.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class JavaWorkspaceRequest {

    private String workspaceName;
    private String entryFile;
    private Map<String, String> files = new LinkedHashMap<>();
    private boolean applySuggestedFixes;

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
}
