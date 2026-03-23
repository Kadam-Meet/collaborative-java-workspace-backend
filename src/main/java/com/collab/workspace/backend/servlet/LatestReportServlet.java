package com.collab.workspace.backend.servlet;

import com.collab.workspace.backend.model.FullReviewResponse;
import com.collab.workspace.backend.service.ReportStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class LatestReportServlet extends HttpServlet {

    private final ReportStore reportStore;
    private final ObjectMapper objectMapper;

    public LatestReportServlet(ReportStore reportStore, ObjectMapper objectMapper) {
        this.reportStore = reportStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FullReviewResponse report = reportStore.getLatest();
        if (report == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"message\":\"No report has been generated yet.\"}");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setHeader("Content-Disposition", "attachment; filename=\"latest-java-analysis-report.json\"");
        objectMapper.writeValue(resp.getOutputStream(), report);
    }
}
