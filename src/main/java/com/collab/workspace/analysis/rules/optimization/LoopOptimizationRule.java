package com.collab.workspace.analysis.rules.optimization;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.List;

public class LoopOptimizationRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "LoopOptimizationRule";
	}

	@Override
	public String getCategory() {
		return "optimization";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();

		context.compilationUnit().findAll(ForStmt.class).forEach(loop -> {
			if (containsExpensiveOperations(loop.findAll(MethodCallExpr.class))) {
				issues.add(buildIssue(context, loop));
			}
		});

		context.compilationUnit().findAll(WhileStmt.class).forEach(loop -> {
			if (containsExpensiveOperations(loop.findAll(MethodCallExpr.class))) {
				issues.add(buildIssue(context, loop));
			}
		});

		return issues;
	}

	private CodeIssue buildIssue(RuleContext context, com.github.javaparser.ast.Node loop) {
		return RuleSupport.issue(
			this,
			IssueType.PERFORMANCE,
			Severity.MEDIUM,
			context.filePath(),
			RuleSupport.lineOf(loop),
			"Expensive operation inside loop",
			"Method calls that look like I/O, waiting, or remote access were found inside a loop.",
			"Move the expensive work outside the loop, cache results, or batch the operation when possible.",
			"Unnecessary repeated work and slower runtime."
		);
	}

	private boolean containsExpensiveOperations(List<MethodCallExpr> calls) {
		return calls.stream().anyMatch(call -> {
			String name = call.getNameAsString().toLowerCase();
			return name.contains("sleep")
				|| name.contains("wait")
				|| name.contains("query")
				|| name.contains("connect")
				|| name.contains("read")
				|| name.contains("write");
		});
	}
}
