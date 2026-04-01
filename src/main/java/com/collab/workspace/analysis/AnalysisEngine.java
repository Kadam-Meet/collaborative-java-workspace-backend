package com.collab.workspace.analysis;

import com.collab.workspace.analysis.complexity.ComplexityCalculator;
import com.collab.workspace.analysis.issue.IssueDetector;
import com.collab.workspace.analysis.model.AnalysisResult;
import com.collab.workspace.analysis.model.CodeIssue;
import com.collab.workspace.analysis.model.ComplexitySummary;
import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.analysis.optimization.OptimizationEngine;
import com.collab.workspace.analysis.parser.JavaParserService;
import com.collab.workspace.analysis.rules.RuleContext;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.engine.JavaCompilerService;
import com.collab.workspace.engine.JavaHeuristicAnalyzer;
import com.collab.workspace.engine.rules.RuleRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AnalysisEngine {

	private final JavaParserService parserService = new JavaParserService();
	private final IssueDetector issueDetector = new IssueDetector();
	private final ComplexityCalculator complexityCalculator = new ComplexityCalculator();
	private final OptimizationEngine optimizationEngine = new OptimizationEngine();
	private final RuleRegistry ruleRegistry = new RuleRegistry();
	private final JavaCompilerService compilerService = new JavaCompilerService();
	private final JavaHeuristicAnalyzer heuristicAnalyzer = new JavaHeuristicAnalyzer();

	public OptimizationResult optimize(WorkspaceRequest request) {
		List<JavaParserService.ParsedJavaSource> parsedSources = parseAll(request);
		List<CodeIssue> issues = new ArrayList<>(compilerService.inspect(request.getFiles()));

		for (JavaParserService.ParsedJavaSource parsedSource : parsedSources) {
			if (parsedSource.compilationUnit().isEmpty()) {
				continue;
			}

			RuleContext context = new RuleContext(
				parsedSource.filePath(),
				parsedSource.content(),
				parsedSource.compilationUnit().orElseThrow(),
				parsedSource.structure()
			);
			issues.addAll(issueDetector.run(context, ruleRegistry.getAllRules()));
		}

		List<CodeIssue> dedupedIssues = deduplicateIssues(issues);
		return optimizationEngine.aggregate(request, dedupedIssues);
	}

	public AnalysisResult analyze(WorkspaceRequest request, OptimizationResult optimization) {
		List<JavaParserService.ParsedJavaSource> parsedSources = parseAll(request);
		ComplexitySummary summary = new ComplexitySummary();
		summary.setTotalFiles(parsedSources.size());

		int totalMethodLength = 0;
		for (JavaParserService.ParsedJavaSource parsedSource : parsedSources) {
			JavaParserService.FileStructure structure = parsedSource.structure();

			ComplexitySummary fileSummary = new ComplexitySummary();
			fileSummary.setTotalFiles(1);
			fileSummary.setTotalLines(structure.totalLines());
			fileSummary.setBlankLines(structure.blankLines());
			fileSummary.setCommentLines(structure.commentLines());
			fileSummary.setCodeLines(structure.totalLines() - structure.blankLines());
			fileSummary.setClassCount(structure.classCount());
			fileSummary.setMethodCount(structure.methodCount());
			fileSummary.setLoopCount(structure.loopCount());
			fileSummary.setConditionalCount(structure.conditionalCount());
			fileSummary.setTryCatchCount(structure.tryCatchCount());
			fileSummary.setIoOperationCount(structure.ioOperationCount());
			fileSummary.setNetworkOperationCount(structure.networkOperationCount());
			fileSummary.setCyclomaticComplexity(1 + structure.loopCount() + structure.conditionalCount() + structure.tryCatchCount());
			fileSummary.setMaxNestingDepth(structure.maxNestingDepth());
			fileSummary.setMaxMethodLength(structure.maxMethodLength());
			fileSummary.setMethodComplexities(new LinkedHashMap<>());

			totalMethodLength += structure.totalMethodLength();
			complexityCalculator.accumulate(summary, fileSummary);
		}

		summary.setAverageMethodLength(
			summary.getMethodCount() == 0 ? 0.0 : round((double) totalMethodLength / summary.getMethodCount())
		);
		complexityCalculator.finalizeMetrics(summary, optimization.getIssues().size());

		AnalysisResult result = new AnalysisResult();
		result.setWorkspaceName(request.getWorkspaceName());
		result.setAnalyzedAt(java.time.Instant.now());
		result.setComplexity(summary);
		heuristicAnalyzer.enrichResult(result, summary, optimization.getIssues());
		return result;
	}

	public FullReviewResponse fullReview(WorkspaceRequest request) {
		OptimizationResult optimization = optimize(request);
		AnalysisResult analysis = analyze(request, optimization);
		return new FullReviewResponse(optimization, analysis);
	}

	private List<JavaParserService.ParsedJavaSource> parseAll(WorkspaceRequest request) {
		return request.getFiles().entrySet().stream()
			.map(entry -> parserService.parseSource(entry.getKey(), entry.getValue()))
			.toList();
	}

	private List<CodeIssue> deduplicateIssues(List<CodeIssue> issues) {
		LinkedHashMap<String, CodeIssue> deduped = new LinkedHashMap<>();
		for (CodeIssue issue : issues) {
			deduped.putIfAbsent(issueKey(issue), issue);
		}
		return new ArrayList<>(deduped.values());
	}

	private String issueKey(CodeIssue issue) {
		return String.join(
			"::",
			issue.getFilePath() == null ? "" : issue.getFilePath(),
			String.valueOf(issue.getLine()),
			issue.getRuleName() == null ? "" : issue.getRuleName(),
			issue.getTitle() == null ? "" : issue.getTitle(),
			issue.getExplanation() == null ? "" : issue.getExplanation()
		);
	}

	private double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
