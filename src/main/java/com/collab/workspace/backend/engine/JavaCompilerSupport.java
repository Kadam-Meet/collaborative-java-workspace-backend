package com.collab.workspace.backend.engine;

import com.collab.workspace.backend.model.CodeIssue;
import com.collab.workspace.backend.model.IssueType;
import com.collab.workspace.backend.model.Severity;

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

public class JavaCompilerSupport {

    public CompilerInspectionResult inspect(Path workspaceRoot) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompilerInspectionResult(false, List.of(compilerMissingIssue()));
        }

        List<Path> sourceFiles;
        try (var stream = Files.walk(workspaceRoot)) {
            sourceFiles = stream.filter(path -> path.toString().endsWith(".java")).toList();
        }

        if (sourceFiles.isEmpty()) {
            return new CompilerInspectionResult(false, List.of(noSourcesIssue()));
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var units = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
            boolean success = Boolean.TRUE.equals(
                compiler.getTask(null, fileManager, diagnostics, List.of("-Xlint:all", "-proc:none"), null, units).call()
            );

            List<CodeIssue> issues = new ArrayList<>();
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                CodeIssue issue = new CodeIssue();
                issue.setId("compiler-" + issues.size());
                issue.setType(diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR ? IssueType.COMPILER_ERROR : IssueType.WARNING);
                issue.setSeverity(diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR ? Severity.HIGH : Severity.MEDIUM);
                issue.setFilePath(diagnostic.getSource() == null ? "unknown" : Path.of(diagnostic.getSource().toUri()).getFileName().toString());
                issue.setLine(diagnostic.getLineNumber());
                issue.setTitle(diagnostic.getKind().name());
                issue.setExplanation(diagnostic.getMessage(null));
                issue.setSuggestedFix("Fix the reported syntax or type problem and run the optimizer again.");
                issue.setImpact("Compilation failures block execution and lower analysis confidence.");
                issues.add(issue);
            });
            return new CompilerInspectionResult(success, issues);
        }
    }

    private CodeIssue compilerMissingIssue() {
        CodeIssue issue = new CodeIssue();
        issue.setId("compiler-missing");
        issue.setType(IssueType.WARNING);
        issue.setSeverity(Severity.MEDIUM);
        issue.setFilePath("system");
        issue.setLine(0);
        issue.setTitle("Java compiler unavailable");
        issue.setExplanation("The backend is not running on a full JDK, so compiler diagnostics are unavailable.");
        issue.setSuggestedFix("Start the service with a JDK installation.");
        issue.setImpact("Compiler-grade validation is skipped.");
        return issue;
    }

    private CodeIssue noSourcesIssue() {
        CodeIssue issue = new CodeIssue();
        issue.setId("no-java-sources");
        issue.setType(IssueType.WARNING);
        issue.setSeverity(Severity.MEDIUM);
        issue.setFilePath("workspace");
        issue.setLine(0);
        issue.setTitle("No Java source files detected");
        issue.setExplanation("The request did not include any `.java` files.");
        issue.setSuggestedFix("Send at least one Java source file.");
        issue.setImpact("No meaningful optimization can be run.");
        return issue;
    }

    public record CompilerInspectionResult(boolean successful, List<CodeIssue> issues) {
    }
}
