package com.collab.workspace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "code_issues")
public class CodeIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String issueType;
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long lineNumber;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "report_id")
    private AnalysisReport report;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private WorkspaceFile file;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getLineNumber() { return lineNumber; }
    public void setLineNumber(Long lineNumber) { this.lineNumber = lineNumber; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public AnalysisReport getReport() { return report; }
    public void setReport(AnalysisReport report) { this.report = report; }

    public WorkspaceFile getFile() { return file; }
    public void setFile(WorkspaceFile file) { this.file = file; }

    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodeIssue)) return false;
        CodeIssue that = (CodeIssue) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}