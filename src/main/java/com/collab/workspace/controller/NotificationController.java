package com.collab.workspace.controller;

import com.collab.workspace.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listNotifications(
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @RequestParam(defaultValue = "20") int limit,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(notificationService.listNotifications(getEmail(request), unreadOnly, limit));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markRead(
        @PathVariable Long notificationId,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(notificationService.markRead(getEmail(request), notificationId));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(HttpServletRequest request) {
        return ResponseEntity.ok(notificationService.markAllRead(getEmail(request)));
    }

    private String getEmail(HttpServletRequest request) {
        Object email = request.getAttribute("authUserEmail");
        if (email == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Unauthenticated request"
            );
        }
        return email.toString();
    }
}
