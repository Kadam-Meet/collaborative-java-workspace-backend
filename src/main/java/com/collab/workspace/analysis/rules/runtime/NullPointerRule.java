package com.collab.workspace.analysis.rules.runtime;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NullPointerRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "NullPointerRule";
	}

	@Override
	public String getCategory() {
		return "runtime";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		Set<String> nullInitializedVariables = new HashSet<>();

		for (VariableDeclarator variable : context.compilationUnit().findAll(VariableDeclarator.class)) {
			if (variable.getInitializer().isPresent() && variable.getInitializer().get() instanceof NullLiteralExpr) {
				nullInitializedVariables.add(variable.getNameAsString());
			}
		}

		context.compilationUnit().findAll(MethodCallExpr.class).forEach(call -> {
			Expression scope = call.getScope().orElse(null);
			if (scope instanceof NameExpr nameExpr && nullInitializedVariables.contains(nameExpr.getNameAsString())) {
				issues.add(RuleSupport.issue(
					this,
					IssueType.WARNING,
					Severity.HIGH,
					context.filePath(),
					RuleSupport.lineOf(call),
					"Possible null dereference",
					"A variable initialized to null is dereferenced without an obvious guard.",
					"Check for null before use or initialize the value before dereferencing it.",
					"Immediate NullPointerException risk."
				));
			}
		});

		return issues;
	}
}
