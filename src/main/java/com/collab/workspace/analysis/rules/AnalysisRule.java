package com.collab.workspace.analysis.rules;

import com.collab.workspace.analysis.model.CodeIssue;

import java.util.List;

public interface AnalysisRule {

	String getRuleName();

	String getCategory();

	List<CodeIssue> analyze(RuleContext context);
}
