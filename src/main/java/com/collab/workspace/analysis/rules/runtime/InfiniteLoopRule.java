package com.collab.workspace.analysis.rules.runtime;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ForStmt;

import java.util.ArrayList;
import java.util.List;

public class InfiniteLoopRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "InfiniteLoopRule";
	}

	@Override
	public String getCategory() {
		return "runtime";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(ForStmt.class).forEach(forStmt -> {
			if (forStmt.getCompare().isEmpty() || forStmt.getUpdate().isEmpty()) {
				return;
			}

			String compareText = forStmt.getCompare().orElseThrow().toString();
			boolean decrements = forStmt.getUpdate().stream().anyMatch(this::isDecrement);
			boolean increments = forStmt.getUpdate().stream().anyMatch(this::isIncrement);

			if ((compareText.contains("<") && decrements) || (compareText.contains(">") && increments)) {
				issues.add(RuleSupport.issue(
					this,
					IssueType.WARNING,
					Severity.MEDIUM,
					context.filePath(),
					RuleSupport.lineOf(forStmt),
					"Suspicious loop update direction",
					"The loop update appears to move away from the termination condition.",
					"Check that the comparison operator and update step move the loop variable toward termination.",
					"Infinite loop or unreachable termination risk."
				));
			}
		});
		return issues;
	}

	private boolean isIncrement(Expression expression) {
		if (expression instanceof UnaryExpr unaryExpr) {
			return unaryExpr.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
				|| unaryExpr.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT;
		}
		if (expression instanceof AssignExpr assignExpr) {
			return assignExpr.getOperator() == AssignExpr.Operator.PLUS;
		}
		return expression.toString().contains("++");
	}

	private boolean isDecrement(Expression expression) {
		if (expression instanceof UnaryExpr unaryExpr) {
			return unaryExpr.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
				|| unaryExpr.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT;
		}
		if (expression instanceof AssignExpr assignExpr) {
			return assignExpr.getOperator() == AssignExpr.Operator.MINUS;
		}
		return expression.toString().contains("--");
	}
}
