package com.collab.workspace.servlet;

import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.repository.ReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class LatestReportServlet extends HttpServlet {

    private final ReportRepository reportStore;
    private final ObjectMapper objectMapper;

    public LatestReportServlet(ReportRepository reportStore, ObjectMapper objectMapper) {
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
