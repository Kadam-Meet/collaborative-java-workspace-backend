package com.collab.workspace.controller;

import com.collab.workspace.analysis.OptimizationResult;
import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.AnalysisJobService;
import com.collab.workspace.service.AnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
public class AnalysisController {

	private final AnalysisService analysisService;
	private final AnalysisJobService analysisJobService;

	public AnalysisController(AnalysisService analysisService, AnalysisJobService analysisJobService) {
		this.analysisService = analysisService;
		this.analysisJobService = analysisJobService;
	}

	@PostMapping("/optimizer/java")
	public ResponseEntity<OptimizationResult> optimize(@RequestBody WorkspaceRequest request) {
		return ResponseEntity.ok(analysisService.optimize(request));
	}

	@PostMapping("/analyzer/java")
	public ResponseEntity<AnalysisResult> analyze(@RequestBody WorkspaceRequest request) {
		return ResponseEntity.ok(analysisService.analyze(request));
	}

	@PostMapping("/analyzer/java/full")
	public ResponseEntity<FullReviewResponse> fullReview(@RequestBody WorkspaceRequest request) {
		return ResponseEntity.ok(analysisService.fullReview(request));
	}

	@PostMapping("/analyzer/java/jobs")
	public ResponseEntity<Map<String, Object>> queueFullReview(@RequestBody WorkspaceRequest request, HttpServletRequest httpRequest) {
		return ResponseEntity.accepted().body(analysisJobService.enqueue(getEmail(httpRequest), request));
	}

	@GetMapping("/analyzer/java/jobs/{jobId}")
	public ResponseEntity<Map<String, Object>> getJobStatus(
		@org.springframework.web.bind.annotation.PathVariable String jobId,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(analysisJobService.getJobStatus(getEmail(httpRequest), jobId));
	}

	private String getEmail(HttpServletRequest request) {
		Object email = request.getAttribute("authUserEmail");
		if (email == null) {
			throw new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.UNAUTHORIZED,
				"Unauthenticated request"
			);
		}
		return email.toString();
	}

	@GetMapping("/meta/rules")
	public ResponseEntity<Map<String, Object>> rules() {
		return ResponseEntity.ok(Map.of(
			"supportedLanguage", "java",
			"optimizerRules", List.of(
				"deep nesting detection",
				"broad catch detection",
				"empty catch detection",
				"console logging hints",
				"loop concatenation warnings"
			),
			"analysisMetrics", List.of(
				"cyclomatic complexity",
				"nesting depth",
				"method length",
				"estimated time complexity",
				"maintainability index",
				"performance score",
				"I/O operation count",
				"network operation count"
			)
		));
	}

	@GetMapping("/meta/health")
	public ResponseEntity<Map<String, Object>> health() {
		return ResponseEntity.ok(Map.of("status", "UP", "service", "analysis"));
	}
}
