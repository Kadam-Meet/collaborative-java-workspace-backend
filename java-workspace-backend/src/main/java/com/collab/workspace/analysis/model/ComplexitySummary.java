package com.collab.workspace.analysis.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComplexitySummary {

    private int totalFiles;
    private int totalLines;
    private int codeLines;
    private int blankLines;
    private int commentLines;
    private int classCount;
    private int methodCount;
    private int loopCount;
    private int conditionalCount;
    private int tryCatchCount;
    private int ioOperationCount;
    private int networkOperationCount;
    private int cyclomaticComplexity;
    private int maxNestingDepth;
    private double averageMethodLength;
    private int maxMethodLength;
    private String estimatedTimeComplexity;
    private String riskLevel;
    private double maintainabilityIndex;
    private int performanceScore;
    private Map<String, String> methodComplexities = new LinkedHashMap<>();

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getCodeLines() {
        return codeLines;
    }

    public void setCodeLines(int codeLines) {
        this.codeLines = codeLines;
    }

    public int getBlankLines() {
        return blankLines;
    }

    public void setBlankLines(int blankLines) {
        this.blankLines = blankLines;
    }

    public int getCommentLines() {
        return commentLines;
    }

    public void setCommentLines(int commentLines) {
        this.commentLines = commentLines;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public int getConditionalCount() {
        return conditionalCount;
    }

    public void setConditionalCount(int conditionalCount) {
        this.conditionalCount = conditionalCount;
    }

    public int getTryCatchCount() {
        return tryCatchCount;
    }

    public void setTryCatchCount(int tryCatchCount) {
        this.tryCatchCount = tryCatchCount;
    }

    public int getIoOperationCount() {
        return ioOperationCount;
    }

    public void setIoOperationCount(int ioOperationCount) {
        this.ioOperationCount = ioOperationCount;
    }

    public int getNetworkOperationCount() {
        return networkOperationCount;
    }

    public void setNetworkOperationCount(int networkOperationCount) {
        this.networkOperationCount = networkOperationCount;
    }

    public int getCyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    public void setCyclomaticComplexity(int cyclomaticComplexity) {
        this.cyclomaticComplexity = cyclomaticComplexity;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        this.maxNestingDepth = maxNestingDepth;
    }

    public double getAverageMethodLength() {
        return averageMethodLength;
    }

    public void setAverageMethodLength(double averageMethodLength) {
        this.averageMethodLength = averageMethodLength;
    }

    public int getMaxMethodLength() {
        return maxMethodLength;
    }

    public void setMaxMethodLength(int maxMethodLength) {
        this.maxMethodLength = maxMethodLength;
    }

    public String getEstimatedTimeComplexity() {
        return estimatedTimeComplexity;
    }

    public void setEstimatedTimeComplexity(String estimatedTimeComplexity) {
        this.estimatedTimeComplexity = estimatedTimeComplexity;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getMaintainabilityIndex() {
        return maintainabilityIndex;
    }

    public void setMaintainabilityIndex(double maintainabilityIndex) {
        this.maintainabilityIndex = maintainabilityIndex;
    }

    public int getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(int performanceScore) {
        this.performanceScore = performanceScore;
    }

    public Map<String, String> getMethodComplexities() {
        return methodComplexities;
    }

    public void setMethodComplexities(Map<String, String> methodComplexities) {
        this.methodComplexities = methodComplexities;
    }
}
