package com.collab.workspace.service;

import com.collab.workspace.dto.SoloWorkspaceRequest;
import com.collab.workspace.dto.SoloWorkspaceResponse;
import com.collab.workspace.dto.SoloWorkspaceSummary;
import com.collab.workspace.entity.SoloWorkspace;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.SoloWorkspaceRepository;
import com.collab.workspace.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class SoloWorkspaceService {

    private final SoloWorkspaceRepository soloWorkspaceRepository;
    private final UserRepository userRepository;

    public SoloWorkspaceService(SoloWorkspaceRepository soloWorkspaceRepository, UserRepository userRepository) {
        this.soloWorkspaceRepository = soloWorkspaceRepository;
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
        soloWorkspaceRepository.delete(workspace);
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