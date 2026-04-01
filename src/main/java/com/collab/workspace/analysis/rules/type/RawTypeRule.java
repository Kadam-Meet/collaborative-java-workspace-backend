package com.collab.workspace.analysis.rules.type;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.analysis.rules.RuleSupport;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.List;

public class RawTypeRule implements AnalysisRule {

	private static final List<String> COLLECTION_TYPES = List.of(
		"List", "ArrayList", "LinkedList", "Set", "HashSet", "Map", "HashMap", "Collection"
	);

	@Override
	public String getRuleName() {
		return "RawTypeRule";
	}

	@Override
	public String getCategory() {
		return "type";
	}

	@Override
	public List<CodeIssue> analyze(RuleContext context) {
		List<CodeIssue> issues = new ArrayList<>();
		context.compilationUnit().findAll(VariableDeclarator.class).forEach(variable -> {
			if (isRawCollectionType(variable.getType())) {
				issues.add(RuleSupport.issue(
					this,
					IssueType.WARNING,
					Severity.MEDIUM,
					context.filePath(),
					RuleSupport.lineOf(variable),
					"Raw collection type used without generics",
					"Raw collections disable compile-time type checking and make misuse easier to miss.",
					"Use parameterized types such as List<String>, Map<K, V>, or Set<MyType>.",
					"Runtime ClassCastException risk and weaker static analysis."
				));
			}

			if (variable.getInitializer().isPresent() && variable.getInitializer().get() instanceof ObjectCreationExpr creationExpr) {
				if (isRawCollectionType(creationExpr.getType())) {
					issues.add(RuleSupport.issue(
						this,
						IssueType.WARNING,
						Severity.MEDIUM,
						context.filePath(),
						RuleSupport.lineOf(creationExpr),
						"Raw collection instantiated without generics",
						"Instantiating a raw collection loses element type information and weakens type safety.",
						"Use the diamond operator or explicit type arguments, for example new ArrayList<>().",
						"Unsafe casts and weaker IDE/compiler assistance."
					));
				}
			}
		});
		return issues;
	}

	private boolean isRawCollectionType(com.github.javaparser.ast.type.Type type) {
		if (!(type instanceof ClassOrInterfaceType classType)) {
			return false;
		}
		return COLLECTION_TYPES.contains(classType.getNameAsString()) && classType.getTypeArguments().isEmpty();
	}
}
