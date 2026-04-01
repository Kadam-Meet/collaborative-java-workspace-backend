package com.collab.workspace.engine;

import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.ComplexitySummary;
import com.collab.workspace.analysis.model.IssueType;

import java.util.List;

public class JavaHeuristicAnalyzer {

	public List<String> buildObservations(ComplexitySummary summary, List<CodeIssue> issues) {
		return List.of(
			"Detected " + summary.getLoopCount() + " loop constructs and " + summary.getConditionalCount() + " branch points.",
			"I/O operations: " + summary.getIoOperationCount() + ", networking calls: " + summary.getNetworkOperationCount() + ".",
			"Total issues detected: " + issues.size() + "."
		);
	}

	public List<String> buildRecommendations(ComplexitySummary summary, List<CodeIssue> issues) {
		List<String> recommendations = new java.util.ArrayList<>();
		if (summary.getMaxNestingDepth() >= 4) {
			recommendations.add("Split deeply nested logic into smaller private methods or strategy classes.");
		}
		if (summary.getAverageMethodLength() > 25) {
			recommendations.add("Reduce long methods to improve readability and testability.");
		}
		if (summary.getIoOperationCount() > 0 || summary.getNetworkOperationCount() > 0) {
			recommendations.add("Wrap external I/O and networking in dedicated service classes with retries and timeouts.");
		}
		if (issues.stream().anyMatch(issue -> issue.getType() == IssueType.PERFORMANCE)) {
			recommendations.add("Address the flagged performance issues before deeper algorithmic tuning.");
		}
		return recommendations;
	}

	public void enrichResult(AnalysisResult result, ComplexitySummary summary, List<CodeIssue> issues) {
		result.setObservations(buildObservations(summary, issues));
		result.setRecommendations(buildRecommendations(summary, issues));
	}
}
