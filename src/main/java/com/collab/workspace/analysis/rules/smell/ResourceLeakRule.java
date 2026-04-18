package com.collab.workspace.analysis.rules.smell;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceLeakRule implements AnalysisRule {

	private static final Set<String> TRACKED_TYPES = Set.of("Scanner", "BufferedReader", "BufferedWriter", "InputStream", "OutputStream");

	@Override
	public String getRuleName() {
		return "ResourceLeakRule";
	}

	@Override
	public String getCategory() {
		return "smell";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(VariableDeclarator.class).forEach(variable -> {
			if (!looksLikeTrackedResource(variable.getType())) {
				return;
			}

			boolean closed = context.compilationUnit().findAll(MethodCallExpr.class).stream()
				.anyMatch(call -> call.getNameAsString().equals("close")
					&& call.getScope().filter(scope -> scope instanceof NameExpr && ((NameExpr) scope).getNameAsString().equals(variable.getNameAsString())).isPresent());

			if (!closed) {
				issues.add(RuleSupport.issue(
					this,
					IssueType.MAINTAINABILITY,
					Severity.MEDIUM,
					context.filePath(),
					RuleSupport.lineOf(variable),
					"Resource may not be closed",
					"The resource is created without an obvious close call or try-with-resources block.",
					"Use try-with-resources or ensure the resource is closed in a finally block.",
					"Resource leak and handle exhaustion risk."
				));
			}
		});
		return issues;
	}

	private boolean looksLikeTrackedResource(com.github.javaparser.ast.type.Type type) {
		if (!(type instanceof ClassOrInterfaceType classType)) {
			return false;
		}
		return TRACKED_TYPES.contains(classType.getNameAsString());
	}
}
