package com.collab.workspace.analysis.optimization;

import com.collab.workspace.analysis.OptimizationResult;
import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.dto.WorkspaceRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

public class OptimizationEngine {

	public OptimizationResult aggregate(WorkspaceRequest request, List<CodeIssue> issues) {
		OptimizationResult optimization = new OptimizationResult();
		optimization.setWorkspaceName(request.getWorkspaceName());
		optimization.setAnalyzedAt(Instant.now());
		optimization.setOptimizedFiles(new LinkedHashMap<>(request.getFiles()));
		optimization.setIssues(issues);
		optimization.setCompilationSuccessful(issues.stream().noneMatch(issue -> issue.getSeverity() == Severity.HIGH));
		optimization.setSummary(buildSummary(issues));
		return optimization;
	}

	private String buildSummary(List<CodeIssue> issues) {
		long high = issues.stream().filter(issue -> issue.getSeverity() == Severity.HIGH).count();
		long medium = issues.stream().filter(issue -> issue.getSeverity() == Severity.MEDIUM).count();
		long low = issues.stream().filter(issue -> issue.getSeverity() == Severity.LOW).count();
		return "Detected " + issues.size() + " issues: " + high + " high, " + medium + " medium, " + low + " low.";
	}
}
