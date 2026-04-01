package com.collab.workspace.analysis.rules.type;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.expr.CastExpr;

import java.util.ArrayList;
import java.util.List;

public class UnsafeCastRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "UnsafeCastRule";
	}

	@Override
	public String getCategory() {
		return "type";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(CastExpr.class).forEach(castExpr -> {
			issues.add(RuleSupport.issue(
				this,
				IssueType.WARNING,
				Severity.MEDIUM,
				context.filePath(),
				RuleSupport.lineOf(castExpr),
				"Manual type cast detected",
				"Explicit casts can hide type mismatches and may fail at runtime if the value is not of the expected type.",
				"Prefer generics, polymorphism, or `instanceof` guards before casting.",
				"Runtime ClassCastException risk."
			));
		});
		return issues;
	}
}
