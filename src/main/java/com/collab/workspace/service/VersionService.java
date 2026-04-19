package com.collab.workspace.service;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.User;
import com.collab.workspace.entity.Version;
import com.collab.workspace.entity.WorkspaceFile;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.RoomRepository;
import com.collab.workspace.repository.UserRepository;
import com.collab.workspace.repository.VersionRepository;
import com.collab.workspace.repository.WorkspaceFileRepository;
import com.collab.workspace.socket.SocketEventServer;
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
public class VersionService {

	private final VersionRepository versionRepository;
	private final WorkspaceFileRepository workspaceFileRepository;
	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final UserRepository userRepository;
	private final ActivityEventService activityEventService;
	private final NotificationService notificationService;
	private final SocketEventServer socketEventServer;

	public VersionService(
		VersionRepository versionRepository,
		WorkspaceFileRepository workspaceFileRepository,
		RoomRepository roomRepository,
		RoomMemberRepository roomMemberRepository,
		UserRepository userRepository,
		ActivityEventService activityEventService,
		NotificationService notificationService,
		SocketEventServer socketEventServer
	) {
		this.versionRepository = versionRepository;
		this.workspaceFileRepository = workspaceFileRepository;
		this.roomRepository = roomRepository;
		this.roomMemberRepository = roomMemberRepository;
		this.userRepository = userRepository;
		this.activityEventService = activityEventService;
		this.notificationService = notificationService;
		this.socketEventServer = socketEventServer;
	}

	@Transactional
	public Map<String, Object> saveSnapshot(String currentUserEmail, Long roomId, Long fileId, WorkspaceRequest request) {
		User currentUser = getUserByEmail(currentUserEmail);
		Room room = getRoomById(roomId);
		ensureMember(room, currentUser);
		ensureCanSaveVersions(room, currentUser);
		WorkspaceFile file = getRoomFileById(roomId, fileId);

		if (request != null && request.getContent() != null) {
			file.setContent(request.getContent());
			file.setUpdatedBy(currentUser);
			file.setUpdatedAt(LocalDateTime.now());
			workspaceFileRepository.save(file);
		}

		int nextVersion = versionRepository.findTopByFile_IdOrderByVersionNumberDesc(fileId)
			.map(v -> v.getVersionNumber() + 1)
			.orElse(1);

		Version version = new Version();
		version.setFile(file);
		version.setVersionNumber(nextVersion);
		version.setContent(file.getContent() == null ? "" : file.getContent());
		if (request != null && request.getVersionMessage() != null && !request.getVersionMessage().isBlank()) {
			version.setMessage(request.getVersionMessage().trim());
		}
		version.setSavedBy(currentUser);
		version.setCreatedAt(LocalDateTime.now());
		version = versionRepository.save(version);
		activityEventService.record(
			room,
			currentUser,
			"VERSION_SAVED",
			"Version saved",
			file.getFilePath() + " saved as v" + version.getVersionNumber()
		);
		notificationService.notifyRoomMembers(
			room,
			currentUser,
			"VERSION_SAVED",
			"Version saved",
			currentUser.getName() + " saved " + file.getFilePath() + " as v" + version.getVersionNumber(),
			false
		);
		socketEventServer.broadcastRoomEvent(room, "VERSION_SAVED", Map.of(
			"fileId", file.getId(),
			"filePath", file.getFilePath(),
			"versionNumber", version.getVersionNumber(),
			"actorEmail", currentUser.getEmail(),
			"createdAt", version.getCreatedAt().toString()
		));

		return toVersionSummary(version);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listFileVersions(String currentUserEmail, Long roomId, Long fileId) {
		User currentUser = getUserByEmail(currentUserEmail);
		Room room = getRoomById(roomId);
		ensureMember(room, currentUser);
		getRoomFileById(roomId, fileId);

		return versionRepository.findAllByFile_IdOrderByVersionNumberDesc(fileId)
			.stream()
			.map(this::toVersionSummary)
			.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getVersionDetail(String currentUserEmail, Long roomId, Long fileId, Long versionId) {
		User currentUser = getUserByEmail(currentUserEmail);
		Room room = getRoomById(roomId);
		ensureMember(room, currentUser);
		getRoomFileById(roomId, fileId);

		Version version = versionRepository.findByIdAndFile_Id(versionId, fileId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

		Map<String, Object> response = toVersionSummary(version);
		response.put("content", version.getContent() == null ? "" : version.getContent());
		response.put("filePath", version.getFile() != null ? version.getFile().getFilePath() : null);
		return response;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> compareVersions(
		String currentUserEmail,
		Long roomId,
		Long fileId,
		Long fromVersionId,
		Long toVersionId
	) {
		User currentUser = getUserByEmail(currentUserEmail);
		Room room = getRoomById(roomId);
		ensureMember(room, currentUser);
		WorkspaceFile file = getRoomFileById(roomId, fileId);

		Version from = versionRepository.findByIdAndFile_Id(fromVersionId, fileId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "From version not found"));
		Version to = versionRepository.findByIdAndFile_Id(toVersionId, fileId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To version not found"));

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("fileId", fileId);
		response.put("filePath", file.getFilePath());
		response.put("fromVersionId", fromVersionId);
		response.put("toVersionId", toVersionId);
		response.put("fromLabel", "v" + from.getVersionNumber());
		response.put("toLabel", "v" + to.getVersionNumber());
		response.put("fromContent", from.getContent() == null ? "" : from.getContent());
		response.put("toContent", to.getContent() == null ? "" : to.getContent());
		return response;
	}

	@Transactional
	public Map<String, Object> revertToVersion(String currentUserEmail, Long roomId, Long fileId, Long versionId) {
		User currentUser = getUserByEmail(currentUserEmail);
		Room room = getRoomById(roomId);
		ensureCanRevertVersions(room, currentUser);
		WorkspaceFile file = getRoomFileById(roomId, fileId);

		Version version = versionRepository.findByIdAndFile_Id(versionId, fileId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

		file.setContent(version.getContent() == null ? "" : version.getContent());
		file.setUpdatedBy(currentUser);
		file.setUpdatedAt(LocalDateTime.now());
		workspaceFileRepository.save(file);

		Version snapshot = new Version();
		snapshot.setFile(file);
		int nextVersion = versionRepository.findTopByFile_IdOrderByVersionNumberDesc(fileId)
			.map(v -> v.getVersionNumber() + 1)
			.orElse(1);
		snapshot.setVersionNumber(nextVersion);
		snapshot.setContent(file.getContent());
		snapshot.setMessage("Reverted from v" + version.getVersionNumber());
		snapshot.setSavedBy(currentUser);
		snapshot.setCreatedAt(LocalDateTime.now());
		versionRepository.save(snapshot);
		activityEventService.record(
			room,
			currentUser,
			"VERSION_REVERTED",
			"Version reverted",
			file.getFilePath() + " reverted to v" + version.getVersionNumber()
		);
		notificationService.notifyRoomMembers(
			room,
			currentUser,
			"VERSION_REVERTED",
			"Version reverted",
			currentUser.getName() + " reverted " + file.getFilePath() + " to v" + version.getVersionNumber(),
			false
		);
		socketEventServer.broadcastRoomEvent(room, "VERSION_REVERTED", Map.of(
			"fileId", file.getId(),
			"filePath", file.getFilePath(),
			"revertedFromVersion", version.getVersionNumber(),
			"newVersion", snapshot.getVersionNumber(),
			"content", file.getContent(),
			"updatedAt", file.getUpdatedAt().toString(),
			"actorEmail", currentUser.getEmail()
		));

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("fileId", file.getId());
		response.put("filePath", file.getFilePath());
		response.put("content", file.getContent());
		response.put("revertedFromVersion", version.getVersionNumber());
		response.put("newVersion", snapshot.getVersionNumber());
		response.put("updatedAt", file.getUpdatedAt());
		response.put("updatedByEmail", file.getUpdatedBy() != null ? file.getUpdatedBy().getEmail() : null);
		return response;
	}

	@Transactional
	public Map<String, Object> deleteVersion(String currentUserEmail, Long roomId, Long fileId, Long versionId) {
		User currentUser = getUserByEmail(currentUserEmail);
		Room room = getRoomById(roomId);
		ensureCanSaveVersions(room, currentUser);
		getRoomFileById(roomId, fileId);

		Version version = versionRepository.findByIdAndFile_Id(versionId, fileId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

		versionRepository.delete(version);
		activityEventService.record(
			room,
			currentUser,
			"VERSION_DELETED",
			"Version deleted",
			"v" + version.getVersionNumber() + " deleted from file " + version.getFile().getFilePath()
		);
		notificationService.notifyRoomMembers(
			room,
			currentUser,
			"VERSION_DELETED",
			"Version deleted",
			currentUser.getName() + " deleted v" + version.getVersionNumber() + " from " + version.getFile().getFilePath(),
			false
		);
		socketEventServer.broadcastRoomEvent(room, "VERSION_DELETED", Map.of(
			"fileId", fileId,
			"versionId", versionId,
			"versionNumber", version.getVersionNumber(),
			"actorEmail", currentUser.getEmail()
		));

		return Map.of(
			"deleted", true,
			"fileId", fileId,
			"versionId", versionId,
			"versionNumber", version.getVersionNumber()
		);
	}

	private Map<String, Object> toVersionSummary(Version version) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", version.getId());
		response.put("versionNumber", version.getVersionNumber());
		response.put("createdAt", version.getCreatedAt());
		response.put("authorName", version.getSavedBy() != null ? version.getSavedBy().getName() : null);
		response.put("authorEmail", version.getSavedBy() != null ? version.getSavedBy().getEmail() : null);
		response.put("fileId", version.getFile() != null ? version.getFile().getId() : null);
		response.put("contentPreview", preview(version.getContent()));
		if (version.getMessage() != null && !version.getMessage().isBlank()) {
			response.put("message", version.getMessage());
		}
		return response;
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

	private WorkspaceFile getRoomFileById(Long roomId, Long fileId) {
		return workspaceFileRepository.findByIdAndRoom_Id(fileId, roomId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
	}

	private Room getRoomById(Long roomId) {
		return roomRepository.findById(roomId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
	}

	private User getUserByEmail(String email) {
		String normalized = required(email, "Authenticated user email is missing")
			.trim()
			.toLowerCase(Locale.ROOT);

		return userRepository.findByEmailIgnoreCase(normalized)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
	}

	private void ensureMember(Room room, User user) {
		boolean isOwner = room.getOwner() != null && room.getOwner().getId().equals(user.getId());
		boolean isMember = roomMemberRepository.existsByRoom_IdAndUser_Id(room.getId(), user.getId());

		if (!isOwner && !isMember) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room");
		}
	}

	private void ensureCanSaveVersions(Room room, User user) {
		if (room.getOwner() != null && room.getOwner().getId().equals(user.getId())) {
			return;
		}

		var member = roomMemberRepository.findByRoom_IdAndUser_Id(room.getId(), user.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room"));

		if (!member.isCanSaveVersions()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to save versions");
		}
	}

	private void ensureCanRevertVersions(Room room, User user) {
		if (room.getOwner() != null && room.getOwner().getId().equals(user.getId())) {
			return;
		}

		var member = roomMemberRepository.findByRoom_IdAndUser_Id(room.getId(), user.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room"));

		if (!member.isCanRevertVersions()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to revert versions");
		}
	}

	private String required(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return value;
	}
}
