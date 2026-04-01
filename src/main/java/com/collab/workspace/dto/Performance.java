package com.collab.workspace.dto;

public class Performance {

private double averageScore;
private int bestScore;
private String latestRiskLevel;

public Performance(double averageScore, int bestScore, String latestRiskLevel) {
    this.averageScore = averageScore;
    this.bestScore = bestScore;
    this.latestRiskLevel = latestRiskLevel;
}

public double getAverageScore() { return averageScore; }
public int getBestScore() { return bestScore; }
public String getLatestRiskLevel() { return latestRiskLevel; }

}
