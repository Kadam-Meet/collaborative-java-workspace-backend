package com.collab.workspace.dto;

public class AnalysisResponse {

private int performanceScore;
private String riskLevel;
private String suggestions;
private String optimizedCode;

public AnalysisResponse() {}

public AnalysisResponse(int performanceScore, String riskLevel,
                        String suggestions, String optimizedCode) {
    this.performanceScore = performanceScore;
    this.riskLevel = riskLevel;
    this.suggestions = suggestions;
    this.optimizedCode = optimizedCode;
}

public int getPerformanceScore() { return performanceScore; }
public String getRiskLevel() { return riskLevel; }
public String getSuggestions() { return suggestions; }
public String getOptimizedCode() { return optimizedCode; }

public void setPerformanceScore(int performanceScore) { this.performanceScore = performanceScore; }
public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
public void setSuggestions(String suggestions) { this.suggestions = suggestions; }
public void setOptimizedCode(String optimizedCode) { this.optimizedCode = optimizedCode; }


}
