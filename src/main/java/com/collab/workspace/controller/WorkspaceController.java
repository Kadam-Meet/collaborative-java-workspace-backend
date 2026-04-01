package com.collab.workspace.controller;

import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.analysis.model.OptimizationResult;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {

    private final WorkspaceService reviewService;
    
    public WorkspaceController(WorkspaceService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/optimizer/java")
    public ResponseEntity<OptimizationResult> optimize(@RequestBody WorkspaceRequest request) {
        return ResponseEntity.ok(reviewService.optimize(request));
    }

    @PostMapping("/analyzer/java")
    public ResponseEntity<AnalysisResult> analyze(@RequestBody WorkspaceRequest request) {
        return ResponseEntity.ok(reviewService.analyze(request));
    }

    @PostMapping("/analyzer/java/full")
    public ResponseEntity<FullReviewResponse> fullReview(@RequestBody WorkspaceRequest request) {
        return ResponseEntity.ok(reviewService.fullReview(request));
    }

    @GetMapping("/meta/rules")
    public ResponseEntity<Map<String, Object>> rules() {
        return ResponseEntity.ok(reviewService.rules());
    }

    @GetMapping("/meta/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "java-workspace-review"));
    }
}
