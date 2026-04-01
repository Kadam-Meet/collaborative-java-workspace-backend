package com.collab.workspace.analysis.rules;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.github.javaparser.ast.Node;

public final class RuleSupport {

	private RuleSupport() {
	}

	public static CodeIssue issue(
		AnalysisRule rule,
		IssueType type,
		Severity severity,
		String filePath,
		long line,
		String title,
		String explanation,
		String fixSuggestion,
		String impact
	) {
		CodeIssue issue = new CodeIssue();
		issue.setId(rule.getRuleName() + "-" + filePath + "-" + line + "-" + title.hashCode());
		issue.setRuleName(rule.getRuleName());
		issue.setCategory(rule.getCategory());
		issue.setType(type);
		issue.setSeverity(severity);
		issue.setFilePath(filePath);
		issue.setLine(line);
		issue.setTitle(title);
		issue.setExplanation(explanation);
		issue.setFixSuggestion(fixSuggestion);
		issue.setImpact(impact);
		return issue;
	}

	public static long lineOf(Node node) {
		return node.getBegin().map(position -> (long) position.line).orElse(1L);
	}
}
