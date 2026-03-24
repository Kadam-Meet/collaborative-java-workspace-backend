package com.collab.workspace.config;

import com.collab.workspace.repository.ReportRepository;
import com.collab.workspace.servlet.LatestReportServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean
    public ServletRegistrationBean<LatestReportServlet> latestReportServlet(ReportRepository reportStore, ObjectMapper objectMapper) {
        ServletRegistrationBean<LatestReportServlet> registration =
            new ServletRegistrationBean<>(new LatestReportServlet(reportStore, objectMapper), "/reports/latest");
        registration.setName("latestReportServlet");
        return registration;
    }
}
