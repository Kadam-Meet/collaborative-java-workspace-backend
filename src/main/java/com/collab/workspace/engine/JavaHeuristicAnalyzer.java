package com.collab.workspace.engine;

import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.ComplexitySummary;
import com.collab.workspace.analysis.model.IssueType;
import com.collab.workspace.analysis.model.OptimizationResult;
import com.collab.workspace.analysis.model.Severity;
import com.collab.workspace.dto.JavaWorkspaceRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaHeuristicAnalyzer {

    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(public|protected|private|static|final|synchronized|native|abstract|\\s)+[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{"
    );

    public OptimizationResult optimize(WorkspaceRequest request, List<CodeIssue> compilerIssues) {
        OptimizationResult result = new OptimizationResult();
        result.setWorkspaceName(request.getWorkspaceName());
        result.setAnalyzedAt(Instant.now());
        result.setCompilationSuccessful(compilerIssues.stream().noneMatch(issue -> issue.getSeverity() == Severity.HIGH));
        result.setOptimizedFiles(new LinkedHashMap<>(request.getFiles()));

        List<CodeIssue> issues = new ArrayList<>(compilerIssues);
        for (var entry : request.getFiles().entrySet()) {
            analyzeFileForIssues(entry.getKey(), entry.getValue(), issues);
        }

        result.setIssues(issues);
        result.setSummary(buildOptimizationSummary(issues));
        return result;
    }

    public AnalysisResult analyze(WorkspaceRequest request, OptimizationResult optimizationResult) {
        AnalysisResult result = new AnalysisResult();
        result.setWorkspaceName(request.getWorkspaceName());
        result.setAnalyzedAt(Instant.now());

        ComplexitySummary summary = new ComplexitySummary();
        summary.setTotalFiles(request.getFiles().size());

        int totalLines = 0;
        int blankLines = 0;
        int commentLines = 0;
        int classCount = 0;
        int methodCount = 0;
        int loopCount = 0;
        int conditionalCount = 0;
        int tryCatchCount = 0;
        int ioOps = 0;
        int networkOps = 0;
        int maxNestingDepth = 0;
        int totalMethodLength = 0;
        int maxMethodLength = 0;
        int cyclomatic = 1;
        Map<String, String> methodComplexities = new LinkedHashMap<>();

        for (var entry : request.getFiles().entrySet()) {
            String file = entry.getKey();
            String content = entry.getValue();
            String[] lines = content.split("\\R", -1);
            totalLines += lines.length;
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isBlank()) {
                    blankLines++;
                } else {
                    summary.setCodeLines(summary.getCodeLines() + 1);
                }
                if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                    commentLines++;
                }
            }

            int fileLoops = countMatches(content, "\\b(for|while|do)\\b");
            int fileConditionals = countMatches(content, "\\b(if|switch|case)\\b");
            int fileTryCatch = countMatches(content, "\\b(try|catch)\\b");
            loopCount += fileLoops;
            conditionalCount += fileConditionals;
            tryCatchCount += fileTryCatch;
            classCount += countMatches(content, "\\b(class|interface|record|enum)\\b");
            ioOps += countMatches(content, "\\b(Files\\.|FileInputStream|FileOutputStream|BufferedReader|BufferedWriter|read\\(|write\\()");
            networkOps += countMatches(content, "\\b(Socket|ServerSocket|HttpClient|URLConnection|DatagramSocket|connect\\()");
            cyclomatic += fileLoops + fileConditionals + fileTryCatch;
            maxNestingDepth = Math.max(maxNestingDepth, computeMaxNestingDepth(content));

            Matcher matcher = METHOD_PATTERN.matcher(content);
            while (matcher.find()) {
                methodCount++;
                String methodName = matcher.group(2);
                int startBraceIndex = content.indexOf('{', matcher.end() - 1);
                int methodLength = estimateBlockLength(content, startBraceIndex);
                totalMethodLength += methodLength;
                maxMethodLength = Math.max(maxMethodLength, methodLength);
                methodComplexities.put(file + "#" + methodName, estimateMethodComplexity(content, matcher.start(), startBraceIndex));
            }
        }

        summary.setTotalLines(totalLines);
        summary.setBlankLines(blankLines);
        summary.setCommentLines(commentLines);
        summary.setClassCount(classCount);
        summary.setMethodCount(methodCount);
        summary.setLoopCount(loopCount);
        summary.setConditionalCount(conditionalCount);
        summary.setTryCatchCount(tryCatchCount);
        summary.setIoOperationCount(ioOps);
        summary.setNetworkOperationCount(networkOps);
        summary.setCyclomaticComplexity(cyclomatic);
        summary.setMaxNestingDepth(maxNestingDepth);
        summary.setAverageMethodLength(methodCount == 0 ? 0.0 : round((double) totalMethodLength / methodCount));
        summary.setMaxMethodLength(maxMethodLength);
        summary.setEstimatedTimeComplexity(estimateTimeComplexity(loopCount, maxNestingDepth, methodComplexities));
        summary.setRiskLevel(determineRiskLevel(cyclomatic, optimizationResult.getIssues().size(), maxNestingDepth));
        summary.setMaintainabilityIndex(estimateMaintainability(summary, optimizationResult.getIssues().size()));
        summary.setPerformanceScore(estimatePerformanceScore(summary, optimizationResult.getIssues().size()));
        summary.setMethodComplexities(methodComplexities);
        result.setComplexity(summary);

        result.getObservations().add("Compiler issues: " + optimizationResult.getIssues().stream()
            .filter(issue -> issue.getType() == IssueType.COMPILER_ERROR).count());
        result.getObservations().add("Detected " + loopCount + " loop constructs and " + conditionalCount + " branch points.");
        result.getObservations().add("I/O operations: " + ioOps + ", networking calls: " + networkOps + ".");

        if (summary.getMaxNestingDepth() >= 4) {
            result.getRecommendations().add("Split deeply nested logic into smaller private methods or strategy classes.");
        }
        if (summary.getAverageMethodLength() > 25) {
            result.getRecommendations().add("Reduce long methods to improve readability and testability.");
        }
        if (summary.getIoOperationCount() > 0 || summary.getNetworkOperationCount() > 0) {
            result.getRecommendations().add("Wrap external I/O and networking in dedicated service classes with retries and timeouts.");
        }
        if (optimizationResult.getIssues().stream().anyMatch(issue -> issue.getType() == IssueType.PERFORMANCE)) {
            result.getRecommendations().add("Address the flagged performance smells before deeper algorithmic tuning.");
        }

        return result;
    }

    private void analyzeFileForIssues(String filePath, String content, List<CodeIssue> issues) {
        String[] lines = content.split("\\R");
        int nesting = 0;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String trimmed = line.trim();

            if (trimmed.contains("{")) {
                nesting += countMatches(trimmed, "\\{");
            }
            if (trimmed.contains("}")) {
                nesting -= countMatches(trimmed, "}");
                nesting = Math.max(0, nesting);
            }

            if (trimmed.matches(".*for\\s*\\(.*\\).*") && index + 1 < lines.length && lines[index + 1].contains("+=")) {
                issues.add(issue("perf-string-concat-" + filePath + "-" + index, IssueType.PERFORMANCE, Severity.MEDIUM, filePath,
                    index + 1, "Potential expensive concatenation in loop",
                    "String concatenation inside loops creates many intermediate objects.",
                    "Use StringBuilder and append values inside the loop.",
                    "StringBuilder builder = new StringBuilder();",
                    "Avoidable memory churn and slower runtime."));
            }

            if (trimmed.matches("catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\).*")) {
                issues.add(issue("broad-catch-" + filePath + "-" + index, IssueType.MAINTAINABILITY, Severity.MEDIUM, filePath,
                    index + 1, "Broad exception catch",
                    "Catching generic Exception hides the actual failure modes.",
                    "Catch specific checked or runtime exceptions instead.",
                    null,
                    "Harder debugging and weaker error handling."));
            }

            if (trimmed.matches("catch\\s*\\(.*\\)\\s*\\{\\s*}")) {
                issues.add(issue("empty-catch-" + filePath + "-" + index, IssueType.SECURITY, Severity.HIGH, filePath,
                    index + 1, "Empty catch block",
                    "Ignoring exceptions can hide corrupted state or failed I/O.",
                    "Log the exception or translate it into a domain-specific failure.",
                    null,
                    "Silent failures and inconsistent behavior."));
            }

            if (trimmed.contains("System.out.println")) {
                issues.add(issue("stdout-" + filePath + "-" + index, IssueType.STYLE, Severity.LOW, filePath,
                    index + 1, "Console logging in application code",
                    "Direct console output is difficult to filter and test in production.",
                    "Use a structured logger such as SLF4J.",
                    null,
                    "Noisy logs and limited observability."));
            }

            if (nesting >= 5 && trimmed.contains("if")) {
                issues.add(issue("nesting-" + filePath + "-" + index, IssueType.MAINTAINABILITY, Severity.HIGH, filePath,
                    index + 1, "Deep nesting detected",
                    "The control flow is becoming hard to follow.",
                    "Extract nested branches into smaller methods or return early.",
                    null,
                    "Higher bug risk and lower readability."));
            }
        }

        Matcher matcher = METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            int startBraceIndex = content.indexOf('{', matcher.end() - 1);
            int methodLength = estimateBlockLength(content, startBraceIndex);
            if (methodLength > 40) {
                issues.add(issue("long-method-" + filePath + "-" + matcher.start(), IssueType.MAINTAINABILITY, Severity.MEDIUM,
                    filePath, lineNumberAt(content, matcher.start()), "Long method",
                    "Large methods often mix multiple responsibilities.",
                    "Split the method into smaller units with descriptive names.",
                    null,
                    "Reduced maintainability and testing difficulty."));
            }
        }
    }

    private CodeIssue issue(String id, IssueType type, Severity severity, String filePath, long line, String title,
                            String explanation, String suggestedFix, String fixedSnippet, String impact) {
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

    private String buildOptimizationSummary(List<CodeIssue> issues) {
        long high = issues.stream().filter(issue -> issue.getSeverity() == Severity.HIGH).count();
        long medium = issues.stream().filter(issue -> issue.getSeverity() == Severity.MEDIUM).count();
        long low = issues.stream().filter(issue -> issue.getSeverity() == Severity.LOW).count();
        return "Detected " + issues.size() + " issues: " + high + " high, " + medium + " medium, " + low + " low.";
    }

    private int countMatches(String content, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private int computeMaxNestingDepth(String content) {
        int depth = 0;
        int max = 0;
        for (char value : content.toCharArray()) {
            if (value == '{') {
                depth++;
                max = Math.max(max, depth);
            } else if (value == '}') {
                depth = Math.max(0, depth - 1);
            }
        }
        return max;
    }

    private int estimateBlockLength(String content, int openingBraceIndex) {
        if (openingBraceIndex < 0 || openingBraceIndex >= content.length()) {
            return 0;
        }
        int depth = 0;
        int endIndex = content.length() - 1;
        for (int i = openingBraceIndex; i < content.length(); i++) {
            char value = content.charAt(i);
            if (value == '{') {
                depth++;
            } else if (value == '}') {
                depth--;
                if (depth == 0) {
                    endIndex = i;
                    break;
                }
            }
        }
        return lineNumberAt(content, endIndex) - lineNumberAt(content, openingBraceIndex) + 1;
    }

    private String estimateMethodComplexity(String content, int methodStart, int blockStart) {
        int blockLength = estimateBlockLength(content, blockStart);
        int sliceEnd = Math.min(content.length(), blockStart + Math.max(blockLength * 8, 1));
        String methodBody = content.substring(Math.max(0, methodStart), sliceEnd);
        int loops = countMatches(methodBody, "\\b(for|while|do)\\b");
        int branches = countMatches(methodBody, "\\b(if|case|catch)\\b");
        if (loops >= 2) {
            return "O(n^2) or higher candidate";
        }
        if (loops == 1 && branches > 1) {
            return "O(n) with branching";
        }
        if (methodBody.contains("return ") && methodBody.contains(methodNameFrom(methodBody) + "(")) {
            return "Recursive candidate";
        }
        return "O(1) to O(n)";
    }

    private String methodNameFrom(String methodBody) {
        Matcher matcher = METHOD_PATTERN.matcher(methodBody);
        return matcher.find() ? matcher.group(2) : "method";
    }

    private int lineNumberAt(String content, int index) {
        int line = 1;
        for (int i = 0; i < Math.min(index, content.length()); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String estimateTimeComplexity(int loopCount, int maxNestingDepth, Map<String, String> methodComplexities) {
        if (methodComplexities.values().stream().anyMatch(value -> value.contains("n^2"))) {
            return "O(n^2)";
        }
        if (loopCount > 0 && maxNestingDepth > 2) {
            return "O(n log n) to O(n^2)";
        }
        if (loopCount > 0) {
            return "O(n)";
        }
        return "O(1)";
    }

    private String determineRiskLevel(int cyclomatic, int issueCount, int maxNestingDepth) {
        if (cyclomatic > 25 || issueCount > 8 || maxNestingDepth >= 5) {
            return "High";
        }
        if (cyclomatic > 12 || issueCount > 3 || maxNestingDepth >= 3) {
            return "Medium";
        }
        return "Low";
    }

    private double estimateMaintainability(ComplexitySummary summary, int issueCount) {
        double raw = 100
            - (summary.getCyclomaticComplexity() * 1.4)
            - (summary.getAverageMethodLength() * 0.8)
            - (summary.getMaxNestingDepth() * 4.0)
            - (issueCount * 2.5);
        return round(Math.max(5, Math.min(100, raw)));
    }

    private int estimatePerformanceScore(ComplexitySummary summary, int issueCount) {
        double score = 100
            - (summary.getLoopCount() * 3.5)
            - (summary.getIoOperationCount() * 2.0)
            - (summary.getNetworkOperationCount() * 2.5)
            - (issueCount * 1.8);
        return Math.max(1, Math.min(100, (int) Math.round(score)));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
