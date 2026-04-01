package com.collab.workspace.engine;

import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.Severity;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JavaCompilerService {

	public List<CodeIssue> inspect(Map<String, String> files) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			return List.of(systemIssue("compiler-missing", "Java compiler unavailable", "The backend is not running on a full JDK, so compiler diagnostics are unavailable.", "Start the service with a JDK installation.", "Compiler-grade validation is skipped."));
		}

		if (files == null || files.isEmpty()) {
			return List.of(systemIssue("no-java-sources", "No Java source files detected", "The request did not include any `.java` files.", "Send at least one Java source file.", "No meaningful analysis can be run."));
		}

		Path workspaceRoot = null;
		try {
			workspaceRoot = Files.createTempDirectory("compiler-inspection-");
			for (var entry : files.entrySet()) {
				Path target = workspaceRoot.resolve(entry.getKey());
				Path parent = target.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				Files.writeString(target, entry.getValue());
			}

			List<Path> sourceFiles;
			try (var stream = Files.walk(workspaceRoot)) {
				sourceFiles = stream.filter(path -> path.toString().endsWith(".java")).toList();
			}

			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
			try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
				var units = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
				compiler.getTask(null, fileManager, diagnostics, List.of("-Xlint:all", "-proc:none"), null, units).call();

				List<CodeIssue> issues = new ArrayList<>();
				diagnostics.getDiagnostics().forEach(diagnostic -> {
					CodeIssue issue = new CodeIssue();
					issue.setId("compiler-" + issues.size());
					issue.setRuleName("JavaCompilerService");
					issue.setCategory("compiler");
					issue.setType(diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR ? IssueType.COMPILER_ERROR : IssueType.WARNING);
					issue.setSeverity(diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR ? Severity.HIGH : Severity.MEDIUM);
					issue.setFilePath(diagnostic.getSource() == null ? "unknown" : Path.of(diagnostic.getSource().toUri()).getFileName().toString());
					issue.setLine(diagnostic.getLineNumber());
					issue.setTitle(diagnostic.getKind().name());
					String message = diagnostic.getMessage(null);
					issue.setExplanation(message);
					issue.setFixSuggestion(suggestedFixForDiagnostic(message));
					issue.setImpact("Compilation failures block execution and lower analysis confidence.");
					issues.add(issue);
				});
				return issues;
			}
		} catch (IOException exception) {
			return List.of(systemIssue("compiler-io", "Compiler inspection failed", "The analyzer could not prepare the temporary workspace for compiler diagnostics.", "Check filesystem permissions and retry.", "Compiler diagnostics are unavailable for this run."));
		} finally {
			deleteQuietly(workspaceRoot);
		}
	}

	private CodeIssue systemIssue(String id, String title, String explanation, String fixSuggestion, String impact) {
		CodeIssue issue = new CodeIssue();
		issue.setId(id);
		issue.setRuleName("JavaCompilerService");
		issue.setCategory("compiler");
		issue.setType(IssueType.WARNING);
		issue.setSeverity(Severity.MEDIUM);
		issue.setFilePath("system");
		issue.setLine(0);
		issue.setTitle(title);
		issue.setExplanation(explanation);
		issue.setFixSuggestion(fixSuggestion);
		issue.setImpact(impact);
		return issue;
	}

	private String suggestedFixForDiagnostic(String message) {
		if (message == null || message.isBlank()) {
			return "Fix the reported syntax or type problem and run the analyzer again.";
		}

		String normalized = message.toLowerCase(Locale.ROOT);
		if (normalized.contains("cannot find symbol")) {
			return "Check variable, method, and class names plus required imports; ensure the symbol is declared and in scope.";
		}
		if (normalized.contains("';' expected") || normalized.contains("')' expected") || normalized.contains("'}' expected")) {
			return "Fix the syntax near the reported line by balancing brackets/parentheses and adding missing separators.";
		}
		if (normalized.contains("incompatible types")) {
			return "Align assigned and target types, or add a safe conversion where appropriate.";
		}
		if (normalized.contains("might not have been initialized")) {
			return "Initialize the variable on all code paths before first use.";
		}
		return "Fix the reported syntax or type problem and run the analyzer again.";
	}

	private void deleteQuietly(Path root) {
		if (root == null) {
			return;
		}
		try (var paths = Files.walk(root)) {
			paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException ignored) {
						// Ignore temp cleanup failures.
					}
				});
		} catch (IOException ignored) {
			// Ignore temp cleanup failures.
		}
	}
}
