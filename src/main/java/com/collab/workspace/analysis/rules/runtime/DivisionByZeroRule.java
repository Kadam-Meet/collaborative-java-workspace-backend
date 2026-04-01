package com.collab.workspace.analysis.rules.runtime;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;

import java.util.ArrayList;
import java.util.List;

public class DivisionByZeroRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "DivisionByZeroRule";
	}

	@Override
	public String getCategory() {
		return "runtime";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(BinaryExpr.class).stream()
			.filter(binaryExpr -> binaryExpr.getOperator() == BinaryExpr.Operator.DIVIDE)
			.filter(binaryExpr -> binaryExpr.getRight() instanceof IntegerLiteralExpr)
			.filter(binaryExpr -> ((IntegerLiteralExpr) binaryExpr.getRight()).asInt() == 0)
			.forEach(binaryExpr -> issues.add(RuleSupport.issue(
				this,
				IssueType.WARNING,
				Severity.HIGH,
				context.filePath(),
				RuleSupport.lineOf(binaryExpr),
				"Division by zero expression",
				"The divisor is the constant zero, which will always fail at runtime.",
				"Validate the divisor or guard against zero before performing the division.",
				"Immediate ArithmeticException."
			)));
		return issues;
	}
}
