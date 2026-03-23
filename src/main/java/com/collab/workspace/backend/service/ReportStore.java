package com.collab.workspace.backend.service;

import com.collab.workspace.backend.model.FullReviewResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ReportStore {

    private final AtomicReference<FullReviewResponse> latest = new AtomicReference<>();

    public void save(FullReviewResponse response) {
        latest.set(response);
    }

    public FullReviewResponse getLatest() {
        return latest.get();
    }
}
