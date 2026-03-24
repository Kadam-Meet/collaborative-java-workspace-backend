package com.collab.workspace.service;

import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.analysis.model.OptimizationResult;
import com.collab.workspace.dto.JavaWorkspaceRequest;
import com.collab.workspace.engine.JavaCompilerSupport;
import com.collab.workspace.engine.JavaHeuristicAnalyzer;
import com.collab.workspace.engine.WorkspaceMaterializer;
import com.collab.workspace.repository.ReportRepository;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Service
public class WorkspaceService {

    private final WorkspaceMaterializer materializer = new WorkspaceMaterializer();
    private final JavaCompilerSupport compilerSupport = new JavaCompilerSupport();
    private final JavaHeuristicAnalyzer analyzer = new JavaHeuristicAnalyzer();
    private final ReportRepository reportStore;
    private final EventPublisher eventPublisher;

    public WorkspaceService(ReportRepository reportStore, EventPublisher eventPublisher) {
        this.reportStore = reportStore;
        this.eventPublisher = eventPublisher;
    }

    public OptimizationResult optimize(WorkspaceRequest request) {
        validate(request);
        Path workspaceRoot = null;
        try {
            workspaceRoot = materializer.materialize(request);
            var compileResult = compilerSupport.inspect(workspaceRoot);
            OptimizationResult result = analyzer.optimize(request, compileResult.issues());
            eventPublisher.publish("OPTIMIZATION", "Optimization completed for " + request.getWorkspaceName());
            return result;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to materialize Java workspace for optimization", ex);
        } finally {
            materializer.deleteQuietly(workspaceRoot);
        }
    }

    public AnalysisResult analyze(WorkspaceRequest request) {
        return analyze(request, optimize(request));
    }

    public AnalysisResult analyze(WorkspaceRequest request, OptimizationResult optimizationResult) {
        AnalysisResult result = analyzer.analyze(request, optimizationResult);
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
            "optimizerRules", java.util.List.of(
                "compiler error detection",
                "deep nesting detection",
                "long method detection",
                "broad catch detection",
                "empty catch detection",
                "console logging hints"
            ),
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
