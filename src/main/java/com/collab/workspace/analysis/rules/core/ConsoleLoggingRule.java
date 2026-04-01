package com.collab.workspace.analysis.rules.core;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.ArrayList;
import java.util.List;

public class ConsoleLoggingRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "ConsoleLoggingRule";
	}

	@Override
	public String getCategory() {
		return "core";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(MethodCallExpr.class).stream()
			.filter(call -> call.getScope().isPresent())
			.filter(call -> {
				String scope = call.getScope().map(Expression::toString).orElse("");
				return "System.out".equals(scope) || "System.err".equals(scope);
			})
			.forEach(call -> issues.add(RuleSupport.issue(
				this,
				IssueType.STYLE,
				Severity.LOW,
				context.filePath(),
				RuleSupport.lineOf(call),
				"Console logging in application code",
				"Direct console output is difficult to filter, test, and route in production environments.",
				"Use a structured logger such as SLF4J with levels and appenders.",
				"Noisy logs and weaker observability."
			)));
		return issues;
	}
}
