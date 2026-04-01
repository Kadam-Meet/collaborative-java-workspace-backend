package com.collab.workspace.analysis.issue;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class IssueDetector {

	private final JavaParser javaParser = new JavaParser();

	public List<CodeIssue> detect(String filePath, String content) {
		List<CodeIssue> issues = new ArrayList<>();

		ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
		for (Problem problem : parseResult.getProblems()) {
			issues.add(parseProblemIssue(filePath, problem));
		}

		if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
			return issues;
		}

		CompilationUnit compilationUnit = parseResult.getResult().orElseThrow();

		detectConsoleLogging(filePath, compilationUnit, issues);
		detectBroadAndEmptyCatch(filePath, compilationUnit, issues);
		detectAssignmentInConditions(filePath, compilationUnit, issues);
		detectStringConcatenationInLoops(filePath, compilationUnit, issues);
		detectDivisionByZero(filePath, compilationUnit, issues);
		detectRawCollections(filePath, compilationUnit, issues);
		detectDeepNesting(filePath, compilationUnit, issues);
		detectSuspiciousLoopDirection(filePath, compilationUnit, issues);

		return issues;
	}

	private void detectConsoleLogging(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(MethodCallExpr.class).stream()
			.filter(call -> call.getScope().isPresent())
			.filter(call -> {
				String scope = call.getScope().map(Expression::toString).orElse("");
				return "System.out".equals(scope) || "System.err".equals(scope);
			})
			.forEach(call -> issues.add(issue(
				"stdout-" + filePath + "-" + lineOf(call) + "-" + columnOf(call),
				IssueType.STYLE,
				Severity.LOW,
				filePath,
				lineOf(call),
				"Console logging in application code",
				"Direct console output is difficult to filter, test, and route in production environments.",
				"Use a structured logger such as SLF4J with levels and appenders.",
				null,
				"Noisy logs and weaker observability."
			)));
	}

	private void detectBroadAndEmptyCatch(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(CatchClause.class).forEach(catchClause -> {
			String exceptionType = catchClause.getParameter().getType().asString();
			String normalized = exceptionType.trim().toLowerCase(Locale.ROOT);

			if ("exception".equals(normalized) || "throwable".equals(normalized)) {
				issues.add(issue(
					"broad-catch-" + filePath + "-" + lineOf(catchClause),
					IssueType.MAINTAINABILITY,
					Severity.MEDIUM,
					filePath,
					lineOf(catchClause),
					"Broad exception catch",
					"Catching Exception or Throwable hides the real failure modes and makes recovery logic less precise.",
					"Catch the specific exception types you expect and handle each case intentionally.",
					null,
					"Harder debugging and weaker error handling."
				));
			}

			if (catchClause.getBody().getStatements().isEmpty()) {
				issues.add(issue(
					"empty-catch-" + filePath + "-" + lineOf(catchClause),
					IssueType.SECURITY,
					Severity.HIGH,
					filePath,
					lineOf(catchClause),
					"Empty catch block",
					"Ignoring an exception entirely can hide data corruption, failed I/O, or broken business logic.",
					"Log the exception, rethrow it, or translate it into a domain-specific error.",
					null,
					"Silent failures and inconsistent behavior."
				));
			}
		});
	}

	private void detectAssignmentInConditions(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(AssignExpr.class).stream()
			.filter(assignExpr -> isInsideCondition(assignExpr))
			.forEach(assignExpr -> issues.add(issue(
				"assignment-condition-" + filePath + "-" + lineOf(assignExpr),
				IssueType.WARNING,
				Severity.HIGH,
				filePath,
				lineOf(assignExpr),
				"Assignment used in conditional expression",
				"Assignments inside conditions are often accidental and make branching behavior harder to reason about.",
				"Separate the assignment from the condition or use an explicit comparison when that was intended.",
				null,
				"Bug-prone control flow and reduced readability."
			)));
	}

	private void detectStringConcatenationInLoops(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(BinaryExpr.class).stream()
			.filter(binaryExpr -> binaryExpr.getOperator() == BinaryExpr.Operator.PLUS)
			.filter(this::looksLikeStringConcatenation)
			.filter(this::isInsideLoop)
			.forEach(binaryExpr -> issues.add(issue(
				"string-concat-loop-" + filePath + "-" + lineOf(binaryExpr),
				IssueType.PERFORMANCE,
				Severity.MEDIUM,
				filePath,
				lineOf(binaryExpr),
				"String concatenation inside loop",
				"Repeated string concatenation inside loops creates many intermediate objects and unnecessary allocations.",
				"Accumulate text with StringBuilder and convert to String once after the loop.",
				"StringBuilder builder = new StringBuilder();",
				"Avoidable memory churn and slower runtime."
			)));
	}

	private void detectDivisionByZero(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(BinaryExpr.class).stream()
			.filter(binaryExpr -> binaryExpr.getOperator() == BinaryExpr.Operator.DIVIDE)
			.filter(binaryExpr -> binaryExpr.getRight() instanceof IntegerLiteralExpr)
			.filter(binaryExpr -> ((IntegerLiteralExpr) binaryExpr.getRight()).asInt() == 0)
			.forEach(binaryExpr -> issues.add(issue(
				"divide-zero-" + filePath + "-" + lineOf(binaryExpr),
				IssueType.WARNING,
				Severity.HIGH,
				filePath,
				lineOf(binaryExpr),
				"Division by zero expression",
				"The divisor is the constant zero, which will always fail at runtime.",
				"Validate the divisor or guard against zero before performing the division.",
				null,
				"Immediate ArithmeticException."
			)));
	}

	private void detectRawCollections(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(VariableDeclarator.class).forEach(variable -> {
			if (isRawCollectionType(variable.getType())) {
				issues.add(issue(
					"raw-collection-type-" + filePath + "-" + lineOf(variable),
					IssueType.WARNING,
					Severity.MEDIUM,
					filePath,
					lineOf(variable),
					"Raw collection type used without generics",
					"Raw collections disable compile-time type checking and make misuse easier to miss.",
					"Use parameterized types such as List<String>, Map<K, V>, or Set<MyType>.",
					null,
					"Runtime ClassCastException risk and weaker static analysis."
				));
			}

			if (variable.getInitializer().isPresent() && variable.getInitializer().get() instanceof ObjectCreationExpr creationExpr) {
				if (isRawCollectionType(creationExpr.getType())) {
					issues.add(issue(
						"raw-collection-creation-" + filePath + "-" + lineOf(creationExpr),
						IssueType.WARNING,
						Severity.MEDIUM,
						filePath,
						lineOf(creationExpr),
						"Raw collection instantiated without generics",
						"Instantiating a raw collection loses element type information and weakens type safety.",
						"Use the diamond operator or explicit type arguments, for example new ArrayList<String>() or new ArrayList<>().",
						null,
						"Unsafe casts and weaker IDE/compiler assistance."
					));
				}
			}
		});
	}

	private void detectDeepNesting(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(IfStmt.class).stream()
			.filter(ifStmt -> nestingDepth(ifStmt, IfStmt.class) >= 4)
			.forEach(ifStmt -> issues.add(issue(
				"deep-nesting-" + filePath + "-" + lineOf(ifStmt),
				IssueType.MAINTAINABILITY,
				Severity.HIGH,
				filePath,
				lineOf(ifStmt),
				"Deep nesting detected",
				"The control flow has many nested conditional branches, which makes the method harder to read and change safely.",
				"Extract nested branches into smaller methods, use guard clauses, or flatten the decision logic.",
				null,
				"Higher bug risk and lower maintainability."
			)));
	}

	private void detectSuspiciousLoopDirection(String filePath, CompilationUnit compilationUnit, List<CodeIssue> issues) {
		compilationUnit.findAll(ForStmt.class).forEach(forStmt -> {
			if (forStmt.getCompare().isEmpty() || forStmt.getUpdate().isEmpty()) {
				return;
			}

			Expression compare = forStmt.getCompare().orElseThrow();
			String compareText = compare.toString();

			boolean decrements = forStmt.getUpdate().stream().anyMatch(this::isDecrement);
			boolean increments = forStmt.getUpdate().stream().anyMatch(this::isIncrement);

			if ((compareText.contains("<") && decrements) || (compareText.contains(">") && increments)) {
				issues.add(issue(
					"loop-direction-" + filePath + "-" + lineOf(forStmt),
					IssueType.WARNING,
					Severity.MEDIUM,
					filePath,
					lineOf(forStmt),
					"Suspicious loop update direction",
					"The loop update appears to move away from the termination condition.",
					"Check that the comparison operator and update step move the loop variable toward termination.",
					null,
					"Infinite loop or unreachable termination risk."
				));
			}
		});
	}

	private boolean isInsideCondition(AssignExpr assignExpr) {
		Optional<IfStmt> ifAncestor = assignExpr.findAncestor(IfStmt.class);
		if (ifAncestor.isPresent() && ifAncestor.get().getCondition().isAncestorOf(assignExpr)) {
			return true;
		}

		return assignExpr.findAncestor(com.github.javaparser.ast.stmt.WhileStmt.class)
			.filter(whileStmt -> whileStmt.getCondition().isAncestorOf(assignExpr))
			.isPresent()
			|| assignExpr.findAncestor(com.github.javaparser.ast.stmt.DoStmt.class)
				.filter(doStmt -> doStmt.getCondition().isAncestorOf(assignExpr))
				.isPresent();
	}

	private boolean looksLikeStringConcatenation(BinaryExpr binaryExpr) {
		return containsStringOperand(binaryExpr.getLeft()) || containsStringOperand(binaryExpr.getRight());
	}

	private boolean isInsideLoop(BinaryExpr binaryExpr) {
		return binaryExpr.findAncestor(ForStmt.class).isPresent()
			|| binaryExpr.findAncestor(ForEachStmt.class).isPresent()
			|| binaryExpr.findAncestor(WhileStmt.class).isPresent()
			|| binaryExpr.findAncestor(DoStmt.class).isPresent();
	}

	private boolean containsStringOperand(Expression expression) {
		if (expression.isStringLiteralExpr()) {
			return true;
		}
		return expression.findAll(BinaryExpr.class).stream()
			.anyMatch(inner -> inner.getLeft().isStringLiteralExpr() || inner.getRight().isStringLiteralExpr());
	}

	private boolean isRawCollectionType(com.github.javaparser.ast.type.Type type) {
		if (!(type instanceof ClassOrInterfaceType classType)) {
			return false;
		}

		String name = classType.getNameAsString();
		boolean collectionLike = List.of("List", "ArrayList", "LinkedList", "Set", "HashSet", "Map", "HashMap", "Collection")
			.contains(name);
		return collectionLike && classType.getTypeArguments().isEmpty();
	}

	private int nestingDepth(Node node, Class<? extends Node> type) {
		int depth = 0;
		Optional<Node> current = node.getParentNode();
		while (current.isPresent()) {
			if (type.isInstance(current.get())) {
				depth++;
			}
			current = current.get().getParentNode();
		}
		return depth;
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

	private CodeIssue parseProblemIssue(String filePath, Problem problem) {
		Optional<Position> position = problem.getLocation()
			.flatMap(location -> location.getBegin().getRange())
			.map(range -> range.begin);
		long line = position.map(pos -> (long) pos.line).orElse(1L);
		String message = problem.getMessage() == null || problem.getMessage().isBlank()
			? "Invalid Java syntax"
			: problem.getMessage();

		return issue(
			"parse-problem-" + filePath + "-" + line + "-" + message.hashCode(),
			IssueType.COMPILER_ERROR,
			Severity.HIGH,
			filePath,
			line,
			"Java syntax or parse error",
			message,
			"Fix the syntax issue so the file can be parsed before deeper analysis runs.",
			null,
			"Compilation failure and incomplete static analysis."
		);
	}

	private long lineOf(Node node) {
		return node.getBegin().map(position -> (long) position.line).orElse(1L);
	}

	private int columnOf(Node node) {
		return node.getBegin().map(position -> position.column).orElse(1);
	}

	private CodeIssue issue(
		String id,
		IssueType type,
		Severity severity,
		String filePath,
		long line,
		String title,
		String explanation,
		String suggestedFix,
		String fixedSnippet,
		String impact
	) {
		CodeIssue issue = new CodeIssue();
		issue.setId(id);
		issue.setType(type);
		issue.setSeverity(severity);
		issue.setFilePath(filePath);
		issue.setLine(line);
		issue.setTitle(title);
		issue.setExplanation(explanation);
		issue.setSuggestedFix(suggestedFix);
		issue.setFixedSnippet(fixedSnippet);
		issue.setImpact(impact);
		return issue;
	}
}
