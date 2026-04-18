package com.collab.workspace.analysis.rules.optimization;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.List;

public class StringConcatRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "StringConcatRule";
	}

	@Override
	public String getCategory() {
		return "optimization";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(BinaryExpr.class).stream()
			.filter(binaryExpr -> binaryExpr.getOperator() == BinaryExpr.Operator.PLUS)
			.filter(this::looksLikeStringConcatenation)
			.filter(this::isInsideLoop)
			.forEach(binaryExpr -> issues.add(RuleSupport.issue(
				this,
				IssueType.PERFORMANCE,
				Severity.MEDIUM,
				context.filePath(),
				RuleSupport.lineOf(binaryExpr),
				"String concatenation inside loop",
				"Repeated string concatenation inside loops creates many intermediate objects and unnecessary allocations.",
				"Accumulate text with StringBuilder and convert to String once after the loop.",
				"Avoidable memory churn and slower runtime."
			)));
		return issues;
	}

	private boolean looksLikeStringConcatenation(BinaryExpr binaryExpr) {
		return containsStringOperand(binaryExpr.getLeft()) || containsStringOperand(binaryExpr.getRight());
	}

	private boolean containsStringOperand(Expression expression) {
		if (expression.isStringLiteralExpr()) {
			return true;
		}
		return expression.findAll(BinaryExpr.class).stream()
			.anyMatch(inner -> inner.getLeft().isStringLiteralExpr() || inner.getRight().isStringLiteralExpr());
	}

	private boolean isInsideLoop(BinaryExpr binaryExpr) {
		return hasAncestor(binaryExpr, ForStmt.class)
			|| hasAncestor(binaryExpr, ForEachStmt.class)
			|| hasAncestor(binaryExpr, WhileStmt.class)
			|| hasAncestor(binaryExpr, DoStmt.class);
	}

	private boolean hasAncestor(Node node, Class<? extends Node> ancestorType) {
		Node current = node.getParentNode().orElse(null);
		while (current != null) {
			if (ancestorType.isInstance(current)) {
				return true;
			}
			current = current.getParentNode().orElse(null);
		}
		return false;
	}
}
