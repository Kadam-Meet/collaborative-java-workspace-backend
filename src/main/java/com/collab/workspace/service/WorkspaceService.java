package com.collab.workspace.service;

import com.collab.workspace.analysis.AnalysisEngine;
import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.analysis.model.OptimizationResult;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.engine.rules.RuleRegistry;
import com.collab.workspace.repository.ReportRepository;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WorkspaceService {

    private final AnalysisEngine analysisEngine = new AnalysisEngine();
    private final RuleRegistry ruleRegistry = new RuleRegistry();
    private final ReportRepository reportStore;
    private final EventPublisher eventPublisher;

    public WorkspaceService(ReportRepository reportStore, EventPublisher eventPublisher) {
        this.reportStore = reportStore;
        this.eventPublisher = eventPublisher;
    }

    public OptimizationResult optimize(WorkspaceRequest request) {
        validate(request);
        OptimizationResult result = analysisEngine.optimize(request);
        eventPublisher.publish("OPTIMIZATION", "Optimization completed for " + request.getWorkspaceName());
        return result;
    }

    public AnalysisResult analyze(WorkspaceRequest request) {
        return analyze(request, optimize(request));
    }

    public AnalysisResult analyze(WorkspaceRequest request, OptimizationResult optimizationResult) {
        AnalysisResult result = analysisEngine.analyze(request, optimizationResult);
        eventPublisher.publish("ANALYSIS", "Analysis completed for " + request.getWorkspaceName());
        return result;
    }

    public FullReviewResponse fullReview(WorkspaceRequest request) {
        OptimizationResult optimization = optimize(request);
        AnalysisResult analysis = analyze(request, optimization);
        FullReviewResponse response = new FullReviewResponse(optimization, analysis);
        reportStore.save(response);
        eventPublisher.publish("REPORT", "Full review ready for " + request.getWorkspaceName());
        return response;
    }

    public Map<String, Object> rules() {
        return Map.of(
            "supportedLanguage", "java",
            "optimizerRules", ruleRegistry.describeRules(),
            "analysisMetrics", java.util.List.of(
                "cyclomatic complexity",
                "nesting depth",
                "method length",
                "estimated time complexity",
                "maintainability index",
                "performance score",
                "I/O operation count",
                "network operation count"
            )
        );
    }

    private void validate(WorkspaceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getWorkspaceName() == null || request.getWorkspaceName().isBlank()) {
            throw new IllegalArgumentException("workspaceName is required.");
        }
        if (request.getEntryFile() == null || request.getEntryFile().isBlank()) {
            throw new IllegalArgumentException("entryFile is required.");
        }
        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new IllegalArgumentException("At least one Java source file is required.");
        }
    }
}
