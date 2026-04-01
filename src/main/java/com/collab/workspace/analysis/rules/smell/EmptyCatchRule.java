package com.collab.workspace.analysis.rules.smell;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.stmt.CatchClause;

import java.util.ArrayList;
import java.util.List;

public class EmptyCatchRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "EmptyCatchRule";
	}

	@Override
	public String getCategory() {
		return "smell";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(CatchClause.class).stream()
			.filter(catchClause -> catchClause.getBody().getStatements().isEmpty())
			.forEach(catchClause -> issues.add(RuleSupport.issue(
				this,
				IssueType.SECURITY,
				Severity.HIGH,
				context.filePath(),
				RuleSupport.lineOf(catchClause),
				"Empty catch block",
				"Ignoring an exception entirely can hide data corruption, failed I/O, or broken business logic.",
				"Log the exception, rethrow it, or translate it into a domain-specific error.",
				"Silent failures and inconsistent behavior."
			)));
		return issues;
	}
}
