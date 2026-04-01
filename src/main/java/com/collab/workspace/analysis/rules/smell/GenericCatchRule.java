package com.collab.workspace.analysis.rules.smell;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.stmt.CatchClause;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GenericCatchRule implements AnalysisRule {

	@Override
	public String getRuleName() {
		return "GenericCatchRule";
	}

	@Override
	public String getCategory() {
		return "smell";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(CatchClause.class).forEach(catchClause -> {
			String exceptionType = catchClause.getParameter().getType().asString().trim().toLowerCase(Locale.ROOT);
			if ("exception".equals(exceptionType) || "throwable".equals(exceptionType)) {
				issues.add(RuleSupport.issue(
					this,
					IssueType.MAINTAINABILITY,
					Severity.MEDIUM,
					context.filePath(),
					RuleSupport.lineOf(catchClause),
					"Broad exception catch",
					"Catching Exception or Throwable hides the real failure modes and makes recovery logic less precise.",
					"Catch the specific exception types you expect and handle each case intentionally.",
					"Harder debugging and weaker error handling."
				));
			}
		});
		return issues;
	}
}
