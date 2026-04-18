package com.collab.workspace.controller;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.VersionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
public class VersionController {

	private final VersionService versionService;

	public VersionController(VersionService versionService) {
		this.versionService = versionService;
	}

	@PostMapping("/rooms/{roomId}/files/{fileId}/versions")
	public ResponseEntity<Map<String, Object>> saveSnapshot(
		@PathVariable Long roomId,
		@PathVariable Long fileId,
		@RequestBody(required = false) WorkspaceRequest request,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(versionService.saveSnapshot(getEmail(httpRequest), roomId, fileId, request));
	}

	@GetMapping("/rooms/{roomId}/files/{fileId}/versions")
	public ResponseEntity<List<Map<String, Object>>> versions(
		@PathVariable Long roomId,
		@PathVariable Long fileId,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(versionService.listFileVersions(getEmail(httpRequest), roomId, fileId));
	}

	@GetMapping("/rooms/{roomId}/files/{fileId}/versions/{versionId}")
	public ResponseEntity<Map<String, Object>> versionDetail(
		@PathVariable Long roomId,
		@PathVariable Long fileId,
		@PathVariable Long versionId,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(versionService.getVersionDetail(getEmail(httpRequest), roomId, fileId, versionId));
	}

	@GetMapping("/rooms/{roomId}/files/{fileId}/versions/compare")
	public ResponseEntity<Map<String, Object>> compare(
		@PathVariable Long roomId,
		@PathVariable Long fileId,
		@RequestParam("fromVersionId") Long fromVersionId,
		@RequestParam("toVersionId") Long toVersionId,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(versionService.compareVersions(getEmail(httpRequest), roomId, fileId, fromVersionId, toVersionId));
	}

	@PostMapping("/rooms/{roomId}/files/{fileId}/versions/{versionId}/revert")
	public ResponseEntity<Map<String, Object>> revert(
		@PathVariable Long roomId,
		@PathVariable Long fileId,
		@PathVariable Long versionId,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(versionService.revertToVersion(getEmail(httpRequest), roomId, fileId, versionId));
	}

	@DeleteMapping("/rooms/{roomId}/files/{fileId}/versions/{versionId}")
	public ResponseEntity<Map<String, Object>> deleteVersion(
		@PathVariable Long roomId,
		@PathVariable Long fileId,
		@PathVariable Long versionId,
		HttpServletRequest httpRequest
	) {
		return ResponseEntity.ok(versionService.deleteVersion(getEmail(httpRequest), roomId, fileId, versionId));
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
