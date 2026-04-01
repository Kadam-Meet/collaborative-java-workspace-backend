package com.collab.workspace.analysis.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.List;
import java.util.Optional;

public class JavaParserService {

	private final JavaParser javaParser = new JavaParser();

	public ParsedJavaSource parseSource(String filePath, String content) {
		ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
		Optional<CompilationUnit> compilationUnit = parseResult.getResult();
		FileStructure structure = buildFileStructure(content, compilationUnit);
		List<String> parserMessages = parseResult.getProblems().stream()
			.map(Problem::getMessage)
			.toList();

		return new ParsedJavaSource(filePath, content, compilationUnit, structure, parserMessages);
	}

	private FileStructure buildFileStructure(String content, Optional<CompilationUnit> compilationUnit) {
		String[] lines = content.split("\\R", -1);
		int blankLines = 0;
		int commentLines = 0;
		for (String raw : lines) {
			String line = raw.trim();
			if (line.isBlank()) {
				blankLines++;
			}
			if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
				commentLines++;
			}
		}

		if (compilationUnit.isEmpty()) {
			return new FileStructure(
				lines.length,
				blankLines,
				commentLines,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0
			);
		}

		CompilationUnit unit = compilationUnit.orElseThrow();
		int classCount = unit.findAll(TypeDeclaration.class).size();
		int methodCount = unit.findAll(CallableDeclaration.class).size();
		int loopCount = unit.findAll(ForStmt.class).size()
			+ unit.findAll(ForEachStmt.class).size()
			+ unit.findAll(WhileStmt.class).size()
			+ unit.findAll(DoStmt.class).size();
		int conditionalCount = unit.findAll(IfStmt.class).size() + unit.findAll(SwitchEntry.class).size();
		int tryCatchCount = unit.findAll(TryStmt.class).size() + unit.findAll(CatchClause.class).size();
		int ioOperationCount = unit.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).stream()
			.map(methodCallExpr -> methodCallExpr.getNameAsString().toLowerCase())
			.filter(name -> name.equals("read") || name.equals("write") || name.equals("readallbytes") || name.equals("readstring"))
			.toList()
			.size();
		int networkOperationCount = unit.findAll(com.github.javaparser.ast.expr.ObjectCreationExpr.class).stream()
			.map(objectCreationExpr -> objectCreationExpr.getType().getNameAsString())
			.filter(name -> name.contains("Socket") || name.contains("HttpClient") || name.contains("URLConnection"))
			.toList()
			.size();

		int totalMethodLength = 0;
		int maxMethodLength = 0;
		for (CallableDeclaration<?> callable : unit.findAll(CallableDeclaration.class)) {
			if (callable.getBegin().isEmpty() || callable.getEnd().isEmpty()) {
				continue;
			}
			int methodLength = callable.getEnd().get().line - callable.getBegin().get().line + 1;
			totalMethodLength += methodLength;
			maxMethodLength = Math.max(maxMethodLength, methodLength);
		}

		return new FileStructure(
			lines.length,
			blankLines,
			commentLines,
			classCount,
			methodCount,
			loopCount,
			conditionalCount,
			tryCatchCount,
			ioOperationCount,
			networkOperationCount,
			computeMaxNestingDepth(unit),
			totalMethodLength,
			maxMethodLength
		);
	}

	private int computeMaxNestingDepth(CompilationUnit unit) {
		int max = 0;
		for (com.github.javaparser.ast.stmt.Statement statement : unit.findAll(com.github.javaparser.ast.stmt.Statement.class)) {
			int depth = 0;
			Optional<com.github.javaparser.ast.Node> current = statement.getParentNode();
			while (current.isPresent()) {
				com.github.javaparser.ast.Node node = current.get();
				if (node instanceof IfStmt
					|| node instanceof ForStmt
					|| node instanceof ForEachStmt
					|| node instanceof WhileStmt
					|| node instanceof DoStmt
					|| node instanceof TryStmt) {
					depth++;
				}
				current = node.getParentNode();
			}
			max = Math.max(max, depth);
		}
		return max;
	}

	public record ParsedJavaSource(
		String filePath,
		String content,
		Optional<CompilationUnit> compilationUnit,
		FileStructure structure,
		List<String> parserMessages
	) {
	}

	public record FileStructure(
		int totalLines,
		int blankLines,
		int commentLines,
		int classCount,
		int methodCount,
		int loopCount,
		int conditionalCount,
		int tryCatchCount,
		int ioOperationCount,
		int networkOperationCount,
		int maxNestingDepth,
		int totalMethodLength,
		int maxMethodLength
	) {
	}
}
