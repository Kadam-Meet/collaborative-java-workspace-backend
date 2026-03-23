package com.collab.workspace.backend.model;

public class FullReviewResponse {

    private OptimizationResult optimization;
    private AnalysisResult analysis;

    public FullReviewResponse() {
    }

    public FullReviewResponse(OptimizationResult optimization, AnalysisResult analysis) {
        this.optimization = optimization;
        this.analysis = analysis;
    }

    public OptimizationResult getOptimization() {
        return optimization;
    }

    public void setOptimization(OptimizationResult optimization) {
        this.optimization = optimization;
    }

    public AnalysisResult getAnalysis() {
        return analysis;
    }

    public void setAnalysis(AnalysisResult analysis) {
        this.analysis = analysis;
    }
}
