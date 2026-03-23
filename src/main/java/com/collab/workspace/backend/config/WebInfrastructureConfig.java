package com.collab.workspace.backend.config;

import com.collab.workspace.backend.service.ReportStore;
import com.collab.workspace.backend.servlet.LatestReportServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class WebInfrastructureConfig {

    @Bean
    public ServletRegistrationBean<LatestReportServlet> latestReportServlet(ReportStore reportStore, ObjectMapper objectMapper) {
        ServletRegistrationBean<LatestReportServlet> registration =
            new ServletRegistrationBean<>(new LatestReportServlet(reportStore, objectMapper), "/reports/latest");
        registration.setName("latestReportServlet");
        return registration;
    }
}
