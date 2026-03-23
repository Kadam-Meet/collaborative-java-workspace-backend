package com.collab.workspace.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {

    private String workspaceName;
    private Instant analyzedAt;
    private ComplexitySummary complexity = new ComplexitySummary();
    private List<String> observations = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

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

    public ComplexitySummary getComplexity() {
        return complexity;
    }

    public void setComplexity(ComplexitySummary complexity) {
        this.complexity = complexity;
    }

    public List<String> getObservations() {
        return observations;
    }

    public void setObservations(List<String> observations) {
        this.observations = observations;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}
