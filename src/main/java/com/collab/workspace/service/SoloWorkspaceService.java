package com.collab.workspace.service;

import com.collab.workspace.dto.SoloWorkspaceRequest;
import com.collab.workspace.dto.SoloWorkspaceResponse;
import com.collab.workspace.dto.SoloWorkspaceSummary;
import com.collab.workspace.entity.SoloWorkspace;
import com.collab.workspace.entity.SoloWorkspaceVersion;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.SoloWorkspaceRepository;
import com.collab.workspace.repository.SoloWorkspaceVersionRepository;
import com.collab.workspace.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SoloWorkspaceService {

    private final SoloWorkspaceRepository soloWorkspaceRepository;
    private final SoloWorkspaceVersionRepository soloWorkspaceVersionRepository;
    private final UserRepository userRepository;

    public SoloWorkspaceService(
        SoloWorkspaceRepository soloWorkspaceRepository,
        SoloWorkspaceVersionRepository soloWorkspaceVersionRepository,
        UserRepository userRepository
    ) {
        this.soloWorkspaceRepository = soloWorkspaceRepository;
        this.soloWorkspaceVersionRepository = soloWorkspaceVersionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SoloWorkspaceSummary> getMySoloWorkspaces(String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        return soloWorkspaceRepository.findAllByUser_IdOrderByUpdatedAtDesc(user.getId()).stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public SoloWorkspaceResponse getSoloWorkspace(String currentUserEmail, Long soloWorkspaceId) {
        User user = getUserByEmail(currentUserEmail);
        SoloWorkspace workspace = soloWorkspaceRepository.findByIdAndUser_Id(soloWorkspaceId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo workspace not found"));
        return toResponse(workspace);
    }

    @Transactional(readOnly = true)
    public SoloWorkspaceResponse getLatestSoloWorkspace(String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        SoloWorkspace workspace = soloWorkspaceRepository.findTopByUser_IdOrderByUpdatedAtDesc(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No solo workspaces found"));
        return toResponse(workspace);
    }

    @Transactional
    public SoloWorkspaceResponse createSoloWorkspace(String currentUserEmail, SoloWorkspaceRequest request) {
        User user = getUserByEmail(currentUserEmail);
        String fileName = normalizeFileName(request.getFileName());
        String content = request.getContent() == null ? "" : request.getContent();

        SoloWorkspace workspace = soloWorkspaceRepository.findByUser_IdAndFileNameIgnoreCase(user.getId(), fileName)
            .orElseGet(SoloWorkspace::new);

        if (workspace.getId() == null) {
            workspace.setUser(user);
            workspace.setFileName(fileName);
        } else {
            workspace.setFileName(fileName);
        }
        workspace.setContent(content);
        soloWorkspaceRepository.save(workspace);
        return toResponse(workspace);
    }

    @Transactional
    public SoloWorkspaceResponse updateSoloWorkspace(String currentUserEmail, Long soloWorkspaceId, SoloWorkspaceRequest request) {
        User user = getUserByEmail(currentUserEmail);
        SoloWorkspace workspace = soloWorkspaceRepository.findByIdAndUser_Id(soloWorkspaceId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo workspace not found"));

        String fileName = normalizeFileName(request.getFileName());
        if (!workspace.getFileName().equalsIgnoreCase(fileName)) {
            boolean exists = soloWorkspaceRepository.existsByUser_IdAndFileNameIgnoreCase(user.getId(), fileName);
            if (exists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A solo workspace with this name already exists");
            }
            workspace.setFileName(fileName);
        }
        workspace.setContent(request.getContent() == null ? "" : request.getContent());
        soloWorkspaceRepository.save(workspace);
        return toResponse(workspace);
    }

    @Transactional
    public void deleteSoloWorkspace(String currentUserEmail, Long soloWorkspaceId) {
        User user = getUserByEmail(currentUserEmail);
        SoloWorkspace workspace = soloWorkspaceRepository.findByIdAndUser_Id(soloWorkspaceId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo workspace not found"));
        soloWorkspaceVersionRepository.deleteAllBySoloWorkspace_Id(workspace.getId());
        soloWorkspaceRepository.delete(workspace);
    }

    @Transactional
    public Map<String, Object> saveVersionSnapshot(String currentUserEmail, Long soloWorkspaceId, SoloWorkspaceRequest request) {
        User user = getUserByEmail(currentUserEmail);
        SoloWorkspace workspace = getSoloWorkspaceEntity(currentUserEmail, soloWorkspaceId);

        if (request != null && request.getContent() != null) {
            workspace.setContent(request.getContent());
            soloWorkspaceRepository.save(workspace);
        }

        int nextVersion = soloWorkspaceVersionRepository.findTopBySoloWorkspace_IdOrderByVersionNumberDesc(workspace.getId())
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);

        SoloWorkspaceVersion version = new SoloWorkspaceVersion();
        version.setSoloWorkspace(workspace);
        version.setVersionNumber(nextVersion);
        version.setFileName(workspace.getFileName());
        version.setContent(workspace.getContent() == null ? "" : workspace.getContent());
        if (request != null && request.getVersionLabel() != null && !request.getVersionLabel().isBlank()) {
            version.setVersionLabel(request.getVersionLabel().trim());
        }
        version.setSavedBy(user);
        version.setCreatedAt(LocalDateTime.now());
        version = soloWorkspaceVersionRepository.save(version);
        pruneSoloVersions(workspace.getId());
        return toVersionSummary(version);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSoloVersions(String currentUserEmail, Long soloWorkspaceId) {
        getSoloWorkspaceEntity(currentUserEmail, soloWorkspaceId);
        return soloWorkspaceVersionRepository.findAllBySoloWorkspace_IdOrderByVersionNumberDesc(soloWorkspaceId)
            .stream()
            .map(this::toVersionSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSoloVersionDetail(String currentUserEmail, Long soloWorkspaceId, Long versionId) {
        getSoloWorkspaceEntity(currentUserEmail, soloWorkspaceId);
        SoloWorkspaceVersion version = soloWorkspaceVersionRepository.findByIdAndSoloWorkspace_Id(versionId, soloWorkspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo version not found"));
        Map<String, Object> response = toVersionSummary(version);
        response.put("content", version.getContent() == null ? "" : version.getContent());
        response.put("filePath", version.getFileName());
        return response;
    }

    @Transactional
    public Map<String, Object> revertToSoloVersion(String currentUserEmail, Long soloWorkspaceId, Long versionId) {
        User user = getUserByEmail(currentUserEmail);
        SoloWorkspace workspace = getSoloWorkspaceEntity(currentUserEmail, soloWorkspaceId);
        SoloWorkspaceVersion version = soloWorkspaceVersionRepository.findByIdAndSoloWorkspace_Id(versionId, soloWorkspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo version not found"));

        workspace.setContent(version.getContent() == null ? "" : version.getContent());
        soloWorkspaceRepository.save(workspace);

        SoloWorkspaceVersion snapshot = new SoloWorkspaceVersion();
        snapshot.setSoloWorkspace(workspace);
        snapshot.setVersionNumber(
            soloWorkspaceVersionRepository.findTopBySoloWorkspace_IdOrderByVersionNumberDesc(workspace.getId())
                .map(currentVersion -> currentVersion.getVersionNumber() + 1)
                .orElse(1)
        );
        snapshot.setFileName(workspace.getFileName());
        snapshot.setContent(workspace.getContent() == null ? "" : workspace.getContent());
        snapshot.setSavedBy(user);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot = soloWorkspaceVersionRepository.save(snapshot);
        pruneSoloVersions(workspace.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fileId", workspace.getId());
        response.put("filePath", workspace.getFileName());
        response.put("content", workspace.getContent());
        response.put("revertedFromVersion", version.getVersionNumber());
        response.put("newVersion", snapshot.getVersionNumber());
        response.put("updatedAt", workspace.getUpdatedAt());
        response.put("updatedByEmail", workspace.getUser() != null ? workspace.getUser().getEmail() : null);
        return response;
    }

    @Transactional
    public Map<String, Object> deleteSoloVersion(String currentUserEmail, Long soloWorkspaceId, Long versionId) {
        getSoloWorkspaceEntity(currentUserEmail, soloWorkspaceId);
        SoloWorkspaceVersion version = soloWorkspaceVersionRepository.findByIdAndSoloWorkspace_Id(versionId, soloWorkspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo version not found"));
        soloWorkspaceVersionRepository.delete(version);
        return Map.of(
            "deleted", true,
            "fileId", soloWorkspaceId,
            "versionId", versionId,
            "versionNumber", version.getVersionNumber()
        );
    }

    private SoloWorkspace getSoloWorkspaceEntity(String currentUserEmail, Long soloWorkspaceId) {
        User user = getUserByEmail(currentUserEmail);
        return soloWorkspaceRepository.findByIdAndUser_Id(soloWorkspaceId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solo workspace not found"));
    }

    private void pruneSoloVersions(Long soloWorkspaceId) {
        List<SoloWorkspaceVersion> versions = soloWorkspaceVersionRepository.findAllBySoloWorkspace_IdOrderByVersionNumberDesc(soloWorkspaceId);
        if (versions.size() <= 5) {
            return;
        }

        for (int i = 5; i < versions.size(); i += 1) {
            soloWorkspaceVersionRepository.delete(versions.get(i));
        }
    }

    private Map<String, Object> toVersionSummary(SoloWorkspaceVersion version) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", version.getId());
        response.put("versionNumber", version.getVersionNumber());
        response.put("createdAt", version.getCreatedAt());
        response.put("authorName", version.getSavedBy() != null ? version.getSavedBy().getName() : null);
        response.put("authorEmail", version.getSavedBy() != null ? version.getSavedBy().getEmail() : null);
        response.put("fileId", version.getSoloWorkspace() != null ? version.getSoloWorkspace().getId() : null);
        response.put("contentPreview", preview(version.getContent()));
        response.put("filePath", version.getFileName());
        if (version.getVersionLabel() != null && !version.getVersionLabel().isBlank()) {
            response.put("versionLabel", version.getVersionLabel());
        }
        return response;
    }

    private SoloWorkspaceSummary toSummary(SoloWorkspace workspace) {
        return new SoloWorkspaceSummary(
            workspace.getId(),
            workspace.getFileName(),
            buildPreview(workspace.getContent()),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }

    private SoloWorkspaceResponse toResponse(SoloWorkspace workspace) {
        return new SoloWorkspaceResponse(
            workspace.getId(),
            workspace.getFileName(),
            workspace.getContent(),
            buildPreview(workspace.getContent()),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }

    private String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "Unsaved solo workspace";
        }

        String normalizedPreview = content.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .findFirst()
            .orElse(content.trim())
            .replaceAll("\\s+", " ");

        int end = Math.min(120, normalizedPreview.length());
        return normalizedPreview.substring(0, end);
    }

    private String preview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.replace("\r", "").replace("\n", " ").trim();
        if (normalized.length() <= 100) {
            return normalized;
        }
        return normalized.substring(0, 100) + "...";
    }

    private String normalizeFileName(String rawFileName) {
        String fileName = rawFileName == null ? "" : rawFileName.trim();
        if (fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName is required");
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".java")) {
            fileName = fileName + ".java";
        }
        return fileName;
    }

    private User getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authenticated user email is missing");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmailIgnoreCase(normalized)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}