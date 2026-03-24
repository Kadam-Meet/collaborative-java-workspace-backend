package com.collab.workspace.analysis;

import com.collab.workspace.analysis.model.CodeIssue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OptimizationResult {

    private String workspaceName;
    private Instant analyzedAt;
    private boolean compilationSuccessful;
    private String summary;
    private List<CodeIssue> issues = new ArrayList<>();
    private Map<String, String> optimizedFiles = new LinkedHashMap<>();

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public boolean isCompilationSuccessful() {
        return compilationSuccessful;
    }

    public void setCompilationSuccessful(boolean compilationSuccessful) {
        this.compilationSuccessful = compilationSuccessful;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<CodeIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<CodeIssue> issues) {
        this.issues = issues;
    }

    public Map<String, String> getOptimizedFiles() {
        return optimizedFiles;
    }

    public void setOptimizedFiles(Map<String, String> optimizedFiles) {
        this.optimizedFiles = optimizedFiles;
    }
}
