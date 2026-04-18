package com.collab.workspace.analysis.rules.core;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentInConditionRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "AssignmentInConditionRule";
	}

	@Override
	public String getCategory() {
		return "core";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(AssignExpr.class).stream()
			.filter(this::isInsideCondition)
			.forEach(assignExpr -> issues.add(RuleSupport.issue(
				this,
				IssueType.WARNING,
				Severity.HIGH,
				context.filePath(),
				RuleSupport.lineOf(assignExpr),
				"Assignment used in conditional expression",
				"Assignments inside conditions are often accidental and make branching behavior harder to reason about.",
				"Separate the assignment from the condition or use an explicit comparison when that was intended.",
				"Bug-prone control flow and reduced readability."
			)));
		return issues;
	}

	private boolean isInsideCondition(AssignExpr assignExpr) {
		Optional<IfStmt> ifAncestor = findAncestor(assignExpr, IfStmt.class);
		if (ifAncestor.isPresent() && ifAncestor.get().getCondition().isAncestorOf(assignExpr)) {
			return true;
		}

		return findAncestor(assignExpr, WhileStmt.class)
			.filter(whileStmt -> whileStmt.getCondition().isAncestorOf(assignExpr))
			.isPresent()
			|| findAncestor(assignExpr, DoStmt.class)
				.filter(doStmt -> doStmt.getCondition().isAncestorOf(assignExpr))
				.isPresent();
	}

	private <T extends Node> Optional<T> findAncestor(Node node, Class<T> type) {
		Node current = node.getParentNode().orElse(null);
		while (current != null) {
			if (type.isInstance(current)) {
				return Optional.of(type.cast(current));
			}
			current = current.getParentNode().orElse(null);
		}
		return Optional.empty();
	}
}
