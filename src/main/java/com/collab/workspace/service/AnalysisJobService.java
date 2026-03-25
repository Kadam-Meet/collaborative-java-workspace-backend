package com.collab.workspace.service;

import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AnalysisJobService {

    private final AnalysisService analysisService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ConcurrentHashMap<String, JobRecord> jobs = new ConcurrentHashMap<>();

    public AnalysisJobService(
        AnalysisService analysisService,
        UserRepository userRepository,
        NotificationService notificationService
    ) {
        this.analysisService = analysisService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Map<String, Object> enqueue(String requesterEmail, WorkspaceRequest request) {
        String jobId = UUID.randomUUID().toString();
        JobRecord record = new JobRecord(
            jobId,
            JobStatus.PENDING,
            OffsetDateTime.now(),
            requesterEmail,
            request != null ? request.getWorkspaceName() : null,
            request == null || request.getNotifyOnCompletion() == null || request.getNotifyOnCompletion()
        );
        jobs.put(jobId, record);

        executor.submit(() -> runJob(record, request));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", jobId);
        response.put("status", record.status.name());
        response.put("createdAt", record.createdAt.toString());
        return response;
    }

    public Map<String, Object> getJobStatus(String requesterEmail, String jobId) {
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis job not found");
        }
        if (requesterEmail == null || record.requesterEmail == null || !requesterEmail.equalsIgnoreCase(record.requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this analysis job");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", record.jobId);
        response.put("status", record.status.name());
        response.put("createdAt", record.createdAt.toString());
        response.put("startedAt", record.startedAt != null ? record.startedAt.toString() : null);
        response.put("completedAt", record.completedAt != null ? record.completedAt.toString() : null);

        if (record.status == JobStatus.COMPLETED && record.result != null) {
            response.put("result", record.result);
        }
        if (record.status == JobStatus.FAILED) {
            response.put("error", record.errorMessage == null ? "Analysis failed" : record.errorMessage);
        }
        return response;
    }

    private void runJob(JobRecord record, WorkspaceRequest request) {
        record.startedAt = OffsetDateTime.now();
        record.status = JobStatus.RUNNING;

        try {
            FullReviewResponse result = analysisService.fullReview(request);
            record.result = result;
            record.status = JobStatus.COMPLETED;
            record.completedAt = OffsetDateTime.now();
            notifyResult(record, true);
        } catch (Exception ex) {
            record.status = JobStatus.FAILED;
            record.errorMessage = ex.getMessage();
            record.completedAt = OffsetDateTime.now();
            notifyResult(record, false);
        }
    }

    private void notifyResult(JobRecord record, boolean success) {
        if (!record.notifyOnCompletion) {
            return;
        }
        if (record.requesterEmail == null || record.requesterEmail.isBlank()) {
            return;
        }

        User recipient = userRepository.findByEmailIgnoreCase(record.requesterEmail.trim())
            .orElse(null);
        if (recipient == null) {
            return;
        }

        if (success) {
            notificationService.notifyUser(
                recipient,
                "ANALYSIS_COMPLETED",
                "Analysis complete",
                "Background analysis finished successfully for " + record.workspaceName,
                null
            );
            return;
        }

        notificationService.notifyUser(
            recipient,
            "ANALYSIS_FAILED",
            "Analysis failed",
            "Background analysis failed for " + record.workspaceName,
            null
        );
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private static class JobRecord {
        private final String jobId;
        private final String requesterEmail;
        private final String workspaceName;
        private final boolean notifyOnCompletion;
        private volatile JobStatus status;
        private final OffsetDateTime createdAt;
        private volatile OffsetDateTime startedAt;
        private volatile OffsetDateTime completedAt;
        private volatile String errorMessage;
        private volatile FullReviewResponse result;

        private JobRecord(
            String jobId,
            JobStatus status,
            OffsetDateTime createdAt,
            String requesterEmail,
            String workspaceName,
            boolean notifyOnCompletion
        ) {
            this.jobId = jobId;
            this.status = status;
            this.createdAt = createdAt;
            this.requesterEmail = requesterEmail;
            this.workspaceName = workspaceName == null || workspaceName.isBlank() ? "workspace" : workspaceName;
            this.notifyOnCompletion = notifyOnCompletion;
        }
    }
}
