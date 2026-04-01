package com.collab.workspace.analysis.issue;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IssueDetector {

	public List<CodeIssue> run(RuleContext context, List<AnalysisRule> rules) {
		Map<String, CodeIssue> issues = new LinkedHashMap<>();
		for (AnalysisRule rule : rules) {
			for (CodeIssue issue : rule.analyze(context)) {
				issues.putIfAbsent(issueKey(issue), issue);
			}
		}
		return new ArrayList<>(issues.values());
	}

	private String issueKey(CodeIssue issue) {
		return String.join(
			"::",
			issue.getFilePath() == null ? "" : issue.getFilePath(),
			String.valueOf(issue.getLine()),
			issue.getRuleName() == null ? "" : issue.getRuleName(),
			issue.getTitle() == null ? "" : issue.getTitle(),
			issue.getExplanation() == null ? "" : issue.getExplanation()
		);
	}
}
