package com.collab.workspace.controller;

import com.collab.workspace.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

private final DashboardService dashboardService;

public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
}

@GetMapping
public ResponseEntity<Map<String, Object>> getDashboard(HttpServletRequest request) {

    // ✅ Extract email from JwtFilter
    String email = (String) request.getAttribute("authUserEmail");

    // 🔥 Defensive check (better error clarity)
    if (email == null || email.isBlank()) {
        return ResponseEntity.status(401).body(Map.of(
                "error", "Unauthorized: Missing authentication context"
        ));
    }

    Map<String, Object> response = dashboardService.getDashboard(email);

    return ResponseEntity.ok(response);
}

}
