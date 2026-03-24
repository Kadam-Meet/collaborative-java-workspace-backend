package com.collab.workspace.analysis.model;

public class CodeIssue {

    private String id;
    private IssueType type;
    private Severity severity;
    private String filePath;
    private long line;
    private String title;
    private String explanation;
    private String suggestedFix;
    private String fixedSnippet;
    private String impact;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getLine() {
        return line;
    }

    public void setLine(long line) {
        this.line = line;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    public String getFixedSnippet() {
        return fixedSnippet;
    }

    public void setFixedSnippet(String fixedSnippet) {
        this.fixedSnippet = fixedSnippet;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }
}
