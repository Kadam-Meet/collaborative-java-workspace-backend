package com.collab.workspace.analysis;

import com.collab.workspace.analysis.complexity.ComplexityCalculator;
import com.collab.workspace.analysis.issue.IssueDetector;
import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.ComplexitySummary;
import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.parser.JavaParserService;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.engine.JavaCompilerSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class AnalysisEngine {

	private final JavaParserService parserService = new JavaParserService();
	private final ComplexityCalculator complexityCalculator = new ComplexityCalculator();
	private final IssueDetector issueDetector = new IssueDetector();
	private final JavaCompilerSupport compilerSupport = new JavaCompilerSupport();

	public OptimizationResult optimize(WorkspaceRequest request) {
		OptimizationResult optimization = new OptimizationResult();
		optimization.setWorkspaceName(request.getWorkspaceName());
		optimization.setAnalyzedAt(Instant.now());
		optimization.setOptimizedFiles(new LinkedHashMap<>(request.getFiles()));

		List<CodeIssue> issues = new ArrayList<>(collectCompilerIssues(request));
		Set<String> existingKeys = new HashSet<>();
		for (CodeIssue issue : issues) {
			existingKeys.add(issueKey(issue));
		}

		for (var entry : request.getFiles().entrySet()) {
			for (CodeIssue issue : issueDetector.detect(entry.getKey(), entry.getValue())) {
				String key = issueKey(issue);
				if (existingKeys.add(key)) {
					issues.add(issue);
				}
			}
		}

		optimization.setIssues(issues);
		optimization.setCompilationSuccessful(issues.stream().noneMatch(issue -> issue.getSeverity() == Severity.HIGH));
		optimization.setSummary(buildSummary(issues));
		return optimization;
	}

	public AnalysisResult analyze(WorkspaceRequest request, OptimizationResult optimization) {
		AnalysisResult result = new AnalysisResult();
		result.setWorkspaceName(request.getWorkspaceName());
		result.setAnalyzedAt(Instant.now());

		ComplexitySummary summary = new ComplexitySummary();
		summary.setTotalFiles(request.getFiles().size());

		int totalMethodLength = 0;
		for (var entry : request.getFiles().entrySet()) {
			JavaParserService.FileStructure structure = parserService.parse(entry.getValue());

			ComplexitySummary fileSummary = new ComplexitySummary();
			fileSummary.setTotalFiles(1);
			fileSummary.setTotalLines(structure.totalLines());
			fileSummary.setBlankLines(structure.blankLines());
			fileSummary.setCommentLines(structure.commentLines());
			fileSummary.setCodeLines(structure.totalLines() - structure.blankLines());
			fileSummary.setClassCount(structure.classCount());
			fileSummary.setMethodCount(structure.methodCount());
			fileSummary.setLoopCount(structure.loopCount());
			fileSummary.setConditionalCount(structure.conditionalCount());
			fileSummary.setTryCatchCount(structure.tryCatchCount());
			fileSummary.setIoOperationCount(structure.ioOperationCount());
			fileSummary.setNetworkOperationCount(structure.networkOperationCount());
			fileSummary.setCyclomaticComplexity(1 + structure.loopCount() + structure.conditionalCount() + structure.tryCatchCount());
			fileSummary.setMaxNestingDepth(structure.maxNestingDepth());
			fileSummary.setMaxMethodLength(structure.maxMethodLength());

			totalMethodLength += structure.totalMethodLength();
			complexityCalculator.accumulate(summary, fileSummary);
		}

		summary.setAverageMethodLength(
			summary.getMethodCount() == 0 ? 0.0 : round((double) totalMethodLength / summary.getMethodCount())
		);
		complexityCalculator.finalizeMetrics(summary, optimization.getIssues().size());
		result.setComplexity(summary);

		result.getObservations().add("Detected " + summary.getLoopCount() + " loop constructs and " + summary.getConditionalCount() + " branch points.");
		result.getObservations().add("I/O operations: " + summary.getIoOperationCount() + ", networking calls: " + summary.getNetworkOperationCount() + ".");
		result.getObservations().add("Total issues detected: " + optimization.getIssues().size() + ".");

		if (summary.getMaxNestingDepth() >= 4) {
			result.getRecommendations().add("Split deeply nested logic into smaller private methods or strategy classes.");
		}
		if (summary.getAverageMethodLength() > 25) {
			result.getRecommendations().add("Reduce long methods to improve readability and testability.");
		}
		if (summary.getIoOperationCount() > 0 || summary.getNetworkOperationCount() > 0) {
			result.getRecommendations().add("Wrap external I/O and networking in dedicated service classes with retries and timeouts.");
		}
		if (optimization.getIssues().stream().anyMatch(issue -> issue.getType().name().equals("PERFORMANCE"))) {
			result.getRecommendations().add("Address the flagged performance smells before deeper algorithmic tuning.");
		}

		return result;
	}

	public FullReviewResponse fullReview(WorkspaceRequest request) {
		OptimizationResult optimization = optimize(request);
		AnalysisResult analysis = analyze(request, optimization);
		return new FullReviewResponse(optimization, analysis);
	}

	private List<CodeIssue> collectCompilerIssues(WorkspaceRequest request) {
		Path workspaceRoot = null;
		try {
			workspaceRoot = Files.createTempDirectory("analysis-workspace-");
			for (var entry : request.getFiles().entrySet()) {
				Path filePath = workspaceRoot.resolve(entry.getKey());
				Path parent = filePath.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				Files.writeString(filePath, entry.getValue());
			}

			return new ArrayList<>(compilerSupport.inspect(workspaceRoot).issues());
		} catch (IOException exception) {
			return new ArrayList<>();
		} finally {
			deleteQuietly(workspaceRoot);
		}
	}

	private void deleteQuietly(Path root) {
		if (root == null) {
			return;
		}

		try (var paths = Files.walk(root)) {
			paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException ignored) {
						// Ignore temp cleanup failures.
					}
				});
		} catch (IOException ignored) {
			// Ignore temp cleanup failures.
		}
	}

	private String issueKey(CodeIssue issue) {
		return String.join(
			"::",
			issue.getFilePath() == null ? "" : issue.getFilePath(),
			String.valueOf(issue.getLine()),
			issue.getType() == null ? "" : issue.getType().name(),
			issue.getTitle() == null ? "" : issue.getTitle(),
			issue.getExplanation() == null ? "" : issue.getExplanation()
		);
	}

	private String buildSummary(List<CodeIssue> issues) {
		long high = issues.stream().filter(issue -> issue.getSeverity() == Severity.HIGH).count();
		long medium = issues.stream().filter(issue -> issue.getSeverity() == Severity.MEDIUM).count();
		long low = issues.stream().filter(issue -> issue.getSeverity() == Severity.LOW).count();
		return "Detected " + issues.size() + " issues: " + high + " high, " + medium + " medium, " + low + " low.";
	}

	private double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
