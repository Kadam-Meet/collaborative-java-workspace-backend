package com.collab.workspace.analysis.rules;

import com.collab.workspace.analysis.parser.JavaParserService;
import com.github.javaparser.ast.CompilationUnit;

public record RuleContext(
	String filePath,
	String content,
	CompilationUnit compilationUnit,
	JavaParserService.FileStructure structure
) {
}
