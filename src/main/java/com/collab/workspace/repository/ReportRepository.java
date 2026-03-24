package com.collab.workspace.repository;

import org.springframework.stereotype.Component;

import com.collab.workspace.analysis.model.FullReviewResponse;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ReportRepository {

    private final AtomicReference<FullReviewResponse> latest = new AtomicReference<>();

    public void save(FullReviewResponse response) {
        latest.set(response);
    }

    public FullReviewResponse getLatest() {
        return latest.get();
    }
}
