package com.collab.workspace.controller;

import com.collab.workspace.dto.SoloWorkspaceRequest;
import com.collab.workspace.dto.SoloWorkspaceResponse;
import com.collab.workspace.dto.SoloWorkspaceSummary;
import com.collab.workspace.service.SoloWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces/solo")
public class SoloWorkspaceController {

    private final SoloWorkspaceService soloWorkspaceService;

    public SoloWorkspaceController(SoloWorkspaceService soloWorkspaceService) {
        this.soloWorkspaceService = soloWorkspaceService;
    }

    @GetMapping
    public ResponseEntity<List<SoloWorkspaceSummary>> listSoloWorkspaces(HttpServletRequest request) {
        return ResponseEntity.ok(soloWorkspaceService.getMySoloWorkspaces(getEmail(request)));
    }

    @GetMapping("/latest")
    public ResponseEntity<SoloWorkspaceResponse> latestSoloWorkspace(HttpServletRequest request) {
        return ResponseEntity.ok(soloWorkspaceService.getLatestSoloWorkspace(getEmail(request)));
    }

    @GetMapping("/{soloWorkspaceId}")
    public ResponseEntity<SoloWorkspaceResponse> soloWorkspace(
        @PathVariable Long soloWorkspaceId,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.getSoloWorkspace(getEmail(request), soloWorkspaceId));
    }

    @PostMapping
    public ResponseEntity<SoloWorkspaceResponse> createSoloWorkspace(
        @RequestBody SoloWorkspaceRequest soloWorkspaceRequest,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.createSoloWorkspace(getEmail(request), soloWorkspaceRequest));
    }

    @PutMapping("/{soloWorkspaceId}")
    public ResponseEntity<SoloWorkspaceResponse> updateSoloWorkspace(
        @PathVariable Long soloWorkspaceId,
        @RequestBody SoloWorkspaceRequest soloWorkspaceRequest,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.updateSoloWorkspace(getEmail(request), soloWorkspaceId, soloWorkspaceRequest));
    }

    @DeleteMapping("/{soloWorkspaceId}")
    public ResponseEntity<Void> deleteSoloWorkspace(
        @PathVariable Long soloWorkspaceId,
        HttpServletRequest request
    ) {
        soloWorkspaceService.deleteSoloWorkspace(getEmail(request), soloWorkspaceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{soloWorkspaceId}/versions")
    public ResponseEntity<Map<String, Object>> saveVersionSnapshot(
        @PathVariable Long soloWorkspaceId,
        @RequestBody SoloWorkspaceRequest soloWorkspaceRequest,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.saveVersionSnapshot(getEmail(request), soloWorkspaceId, soloWorkspaceRequest));
    }

    @GetMapping("/{soloWorkspaceId}/versions")
    public ResponseEntity<List<Map<String, Object>>> listVersions(
        @PathVariable Long soloWorkspaceId,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.listSoloVersions(getEmail(request), soloWorkspaceId));
    }

    @GetMapping("/{soloWorkspaceId}/versions/{versionId}")
    public ResponseEntity<Map<String, Object>> versionDetail(
        @PathVariable Long soloWorkspaceId,
        @PathVariable Long versionId,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.getSoloVersionDetail(getEmail(request), soloWorkspaceId, versionId));
    }

    @PostMapping("/{soloWorkspaceId}/versions/{versionId}/revert")
    public ResponseEntity<Map<String, Object>> revertVersion(
        @PathVariable Long soloWorkspaceId,
        @PathVariable Long versionId,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.revertToSoloVersion(getEmail(request), soloWorkspaceId, versionId));
    }

    @DeleteMapping("/{soloWorkspaceId}/versions/{versionId}")
    public ResponseEntity<Map<String, Object>> deleteVersion(
        @PathVariable Long soloWorkspaceId,
        @PathVariable Long versionId,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(soloWorkspaceService.deleteSoloVersion(getEmail(request), soloWorkspaceId, versionId));
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