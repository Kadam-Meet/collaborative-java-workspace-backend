package com.collab.workspace.analysis.rules.smell;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.List;

public class DeepNestingRule implements AnalysisRule {

	private static final int MAX_RECOMMENDED_DEPTH = 4;

	@Override
	public String getRuleName() {
		return "DeepNestingRule";
	}

	@Override
	public String getCategory() {
		return "smell";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(Node.class).stream()
			.filter(this::isNestingNode)
			.forEach(node -> {
				int depth = nestingDepth(node);
				if (depth > MAX_RECOMMENDED_DEPTH) {
					issues.add(RuleSupport.issue(
						this,
						IssueType.MAINTAINABILITY,
						Severity.MEDIUM,
						context.filePath(),
						RuleSupport.lineOf(node),
						"Deep nesting increases complexity",
						"Nested control flow makes the code harder to reason about, test, and modify safely.",
						"Extract nested logic into smaller methods, return early, or invert conditions to flatten the flow.",
						"Lower readability and a higher chance of missing edge cases."
					));
				}
			});
		return issues;
	}

	private boolean isNestingNode(Node node) {
		return node instanceof IfStmt
			|| node instanceof ForStmt
			|| node instanceof ForEachStmt
			|| node instanceof WhileStmt
			|| node instanceof DoStmt
			|| node instanceof SwitchStmt
			|| node instanceof TryStmt;
	}

	private int nestingDepth(Node node) {
		int depth = 0;
		Node current = node.getParentNode().orElse(null);
		while (current != null) {
			if (isNestingNode(current)) {
				depth++;
			}
			current = current.getParentNode().orElse(null);
		}
		return depth + 1;
	}
}
