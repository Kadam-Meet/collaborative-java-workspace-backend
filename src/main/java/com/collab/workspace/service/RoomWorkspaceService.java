package com.collab.workspace.service;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.RoomMember;
import com.collab.workspace.entity.RoomMemberId;
import com.collab.workspace.entity.User;
import com.collab.workspace.entity.WorkspaceFile;
import com.collab.workspace.exception.CustomException;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.RoomRepository;
import com.collab.workspace.repository.UserRepository;
import com.collab.workspace.repository.WorkspaceFileRepository;
import com.collab.workspace.socket.SocketEventServer;
import com.collab.workspace.util.FileUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomWorkspaceService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceFileRepository workspaceFileRepository;
    private final ActivityEventService activityEventService;
    private final NotificationService notificationService;
    private final SocketEventServer socketEventServer;

    public RoomWorkspaceService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        UserRepository userRepository,
        WorkspaceFileRepository workspaceFileRepository,
        ActivityEventService activityEventService,
        NotificationService notificationService,
        SocketEventServer socketEventServer
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.workspaceFileRepository = workspaceFileRepository;
        this.activityEventService = activityEventService;
        this.notificationService = notificationService;
        this.socketEventServer = socketEventServer;
    }

    @Transactional
    public Map<String, Object> createRoom(String currentUserEmail, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        String roomName = required(request.getRoomName(), "roomName is required");

        Room room = new Room();
        room.setRoomName(roomName.trim());
        room.setRoomCode(generateUniqueRoomCode());
        room.setOwner(currentUser);
        room.setCreatedAt(LocalDateTime.now());
        room = roomRepository.save(room);

        addMemberIfMissing(room, currentUser);
        createDefaultFile(room, currentUser);
        activityEventService.record(
            room,
            currentUser,
            "ROOM_CREATED",
            "Room created",
            "Created room " + room.getRoomName() + " (" + room.getRoomCode() + ")"
        );

        return toRoomSummary(room);
    }

    @Transactional
    public Map<String, Object> joinRoom(String currentUserEmail, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        String roomCode = required(request.getRoomCode(), "roomCode is required");

        Room room = roomRepository.findByRoomCodeIgnoreCase(roomCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean wasMember = roomMemberRepository.existsByRoom_IdAndUser_Id(room.getId(), currentUser.getId());
        addMemberIfMissing(room, currentUser);
        if (!wasMember) {
            activityEventService.record(
                room,
                currentUser,
                "ROOM_JOINED",
                "Joined room",
                currentUser.getName() + " joined " + room.getRoomName()
            );
            socketEventServer.broadcastRoomEvent(room, "ROOM_JOINED", Map.of(
                "actorEmail", currentUser.getEmail(),
                "actorName", currentUser.getName(),
                "roomId", room.getId(),
                "roomCode", room.getRoomCode()
            ));
            socketEventServer.broadcastPresence(room);
        }
        return toRoomSummary(room);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyRooms(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        return roomRepository.findAllByParticipantUserId(currentUser.getId())
            .stream()
            .map(this::toRoomSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoomByCode(String currentUserEmail, String roomCode) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = roomRepository.findByRoomCodeIgnoreCase(roomCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        ensureMember(room, currentUser);
        return toRoomSummary(room);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoomMembers(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        return roomMemberRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId)
            .stream()
            .map(member -> toMemberSummary(room, member))
            .toList();
    }

    @Transactional
    public Map<String, Object> addMember(String currentUserEmail, Long roomId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureOwner(room, currentUser);

        String memberEmail = required(request.getMemberEmail(), "memberEmail is required");
        User member = getUserByEmail(memberEmail);
        addMemberIfMissing(room, member);
        activityEventService.record(
            room,
            currentUser,
            "MEMBER_ADDED",
            "Member added",
            member.getEmail() + " added to " + room.getRoomName()
        );
        notificationService.notifyUser(
            member,
            "ROOM_INVITE",
            "Added to room",
            "You were added to " + room.getRoomName() + " by " + currentUser.getName(),
            room
        );
        socketEventServer.broadcastRoomEvent(room, "MEMBER_ADDED", Map.of(
            "actorEmail", currentUser.getEmail(),
            "memberEmail", member.getEmail(),
            "memberName", member.getName()
        ));
        socketEventServer.broadcastPresence(room);

        return toRoomSummary(room);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoomActivity(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        return activityEventService.listRoomActivity(roomId);
    }

    @Transactional
    public Map<String, Object> updateMemberPermissions(
        String currentUserEmail,
        Long roomId,
        Long memberUserId,
        WorkspaceRequest request
    ) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureOwner(room, currentUser);

        if (room.getOwner() != null && room.getOwner().getId().equals(memberUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner permissions cannot be changed");
        }

        RoomMember member = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, memberUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room member not found"));

        if (request.getCanEditFiles() != null) {
            member.setCanEditFiles(request.getCanEditFiles());
        }
        if (request.getCanSaveVersions() != null) {
            member.setCanSaveVersions(request.getCanSaveVersions());
        }
        if (request.getCanRevertVersions() != null) {
            member.setCanRevertVersions(request.getCanRevertVersions());
        }

        roomMemberRepository.save(member);
        activityEventService.record(
            room,
            currentUser,
            "MEMBER_PERMISSIONS_UPDATED",
            "Member permissions updated",
            member.getUser().getEmail() + " permissions were updated"
        );
        notificationService.notifyUser(
            member.getUser(),
            "MEMBER_PERMISSIONS_UPDATED",
            "Permissions updated",
            "Your room permissions were updated in " + room.getRoomName(),
            room
        );
        socketEventServer.broadcastRoomEvent(room, "MEMBER_PERMISSIONS_UPDATED", Map.of(
            "actorEmail", currentUser.getEmail(),
            "memberEmail", member.getUser().getEmail(),
            "canEditFiles", member.isCanEditFiles(),
            "canSaveVersions", member.isCanSaveVersions(),
            "canRevertVersions", member.isCanRevertVersions()
        ));
        return toMemberSummary(room, member);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoomFiles(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        return workspaceFileRepository.findAllByRoom_IdOrderByUpdatedAtDesc(roomId)
            .stream()
            .map(this::toFileSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoomFile(String currentUserEmail, Long roomId, Long fileId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        WorkspaceFile file = getRoomFileById(roomId, fileId);

        Map<String, Object> response = toFileSummary(file);
        response.put("content", file.getContent() == null ? "" : file.getContent());
        return response;
    }

    @Transactional
    public Map<String, Object> createRoomFile(String currentUserEmail, Long roomId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        String filePath = required(request.getFilePath(), "filePath is required").trim();
        if (workspaceFileRepository.existsByRoom_IdAndFilePathIgnoreCase(roomId, filePath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File already exists in room");
        }

        WorkspaceFile file = new WorkspaceFile();
        file.setRoom(room);
        file.setFilePath(filePath);
        file.setLanguage(resolveLanguage(request.getLanguage(), filePath));
        file.setContent(request.getContent() == null ? "" : request.getContent());
        file.setUpdatedBy(currentUser);
        file.setUpdatedAt(LocalDateTime.now());
        workspaceFileRepository.save(file);
        activityEventService.record(
            room,
            currentUser,
            "FILE_CREATED",
            "File created",
            file.getFilePath() + " was created"
        );
        socketEventServer.broadcastRoomEvent(room, "FILE_CREATED", Map.of(
            "fileId", file.getId(),
            "filePath", file.getFilePath(),
            "content", file.getContent(),
            "updatedAt", file.getUpdatedAt().toString(),
            "actorEmail", currentUser.getEmail()
        ));

        Map<String, Object> response = toFileSummary(file);
        response.put("content", file.getContent());
        return response;
    }

    @Transactional
    public Map<String, Object> saveRoomFile(String currentUserEmail, Long roomId, Long fileId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        WorkspaceFile file = getRoomFileById(roomId, fileId);
        ensureNoEditConflict(file, request.getExpectedUpdatedAt());

        if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
            String filePath = request.getFilePath().trim();
            boolean exists = workspaceFileRepository.existsByRoom_IdAndFilePathIgnoreCase(roomId, filePath);
            if (exists && !filePath.equalsIgnoreCase(file.getFilePath())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another file with this path already exists");
            }
            file.setFilePath(filePath);
        }

        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            file.setLanguage(request.getLanguage().trim().toLowerCase(Locale.ROOT));
        } else if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
            file.setLanguage(FileUtil.detectLanguage(file.getFilePath()));
        }

        file.setContent(request.getContent() == null ? "" : request.getContent());
        file.setUpdatedBy(currentUser);
        file.setUpdatedAt(LocalDateTime.now());
        workspaceFileRepository.save(file);
        activityEventService.record(
            room,
            currentUser,
            "FILE_UPDATED",
            "File updated",
            file.getFilePath() + " was updated"
        );
        socketEventServer.broadcastRoomEvent(room, "FILE_UPDATED", Map.of(
            "fileId", file.getId(),
            "filePath", file.getFilePath(),
            "content", file.getContent(),
            "updatedAt", file.getUpdatedAt().toString(),
            "actorEmail", currentUser.getEmail()
        ));

        Map<String, Object> response = toFileSummary(file);
        response.put("content", file.getContent());
        return response;
    }

    private void ensureNoEditConflict(WorkspaceFile file, String expectedUpdatedAt) {
        if (expectedUpdatedAt == null || expectedUpdatedAt.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expectedUpdatedAt is required for save operation");
        }

        LocalDateTime expected;
        try {
            expected = LocalDateTime.parse(expectedUpdatedAt);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expectedUpdatedAt must be ISO-8601 LocalDateTime");
        }

        LocalDateTime current = file.getUpdatedAt();
        if (current != null && !current.equals(expected)) {
            throw new CustomException(
                HttpStatus.CONFLICT,
                "EDIT_CONFLICT",
                "File changed by another user. Refresh and merge your changes."
            );
        }
    }

    @Transactional
    public Map<String, Object> uploadJavaFile(String currentUserEmail, Long roomId, MultipartFile multipartFile) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        FileUtil.validateJavaUpload(multipartFile);

        String filePath = FileUtil.sanitizeDownloadFileName(multipartFile.getOriginalFilename());
        String baseName = filePath;
        int suffix = 1;
        while (workspaceFileRepository.existsByRoom_IdAndFilePathIgnoreCase(roomId, filePath)) {
            filePath = baseName.replace(".java", "") + "_" + suffix + ".java";
            suffix++;
        }

        String content;
        try {
            content = new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file");
        }

        WorkspaceFile file = new WorkspaceFile();
        file.setRoom(room);
        file.setFilePath(filePath);
        file.setLanguage("java");
        file.setContent(content);
        file.setUpdatedBy(currentUser);
        file.setUpdatedAt(LocalDateTime.now());
        workspaceFileRepository.save(file);
        activityEventService.record(
            room,
            currentUser,
            "FILE_UPLOADED",
            "File uploaded",
            file.getFilePath() + " was uploaded"
        );
        socketEventServer.broadcastRoomEvent(room, "FILE_UPLOADED", Map.of(
            "fileId", file.getId(),
            "filePath", file.getFilePath(),
            "content", file.getContent(),
            "updatedAt", file.getUpdatedAt().toString(),
            "actorEmail", currentUser.getEmail()
        ));

        Map<String, Object> response = toFileSummary(file);
        response.put("content", file.getContent());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDownloadPayload(String currentUserEmail, Long roomId, Long fileId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        WorkspaceFile file = getRoomFileById(roomId, fileId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fileName", FileUtil.sanitizeDownloadFileName(file.getFilePath()));
        response.put("content", file.getContent() == null ? "" : file.getContent());
        return response;
    }

    private void addMemberIfMissing(Room room, User user) {
        boolean exists = roomMemberRepository.existsByRoom_IdAndUser_Id(room.getId(), user.getId());
        if (exists) {
            return;
        }

        RoomMember member = new RoomMember();
        member.setId(new RoomMemberId(room.getId(), user.getId()));
        member.setRoom(room);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        roomMemberRepository.save(member);
    }

    private void createDefaultFile(Room room, User user) {
        WorkspaceFile file = new WorkspaceFile();
        file.setRoom(room);
        file.setFilePath("Main.java");
        file.setLanguage("java");
        file.setContent("public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from " + sanitizeClassSuffix(room.getRoomName()) + "\");\n    }\n}\n");
        file.setUpdatedBy(user);
        file.setUpdatedAt(LocalDateTime.now());
        workspaceFileRepository.save(file);
    }

    private String sanitizeClassSuffix(String input) {
        String cleaned = input == null ? "Room" : input.replaceAll("[^A-Za-z0-9 ]", "").trim();
        if (cleaned.isBlank()) {
            return "Room";
        }
        return cleaned;
    }

    private Map<String, Object> toRoomSummary(Room room) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", room.getId());
        response.put("roomCode", room.getRoomCode());
        response.put("roomName", room.getRoomName());
        response.put("createdAt", room.getCreatedAt());
        response.put("ownerEmail", room.getOwner().getEmail());
        response.put("memberCount", roomMemberRepository.countByRoom_Id(room.getId()));
        response.put("fileCount", workspaceFileRepository.countByRoom_Id(room.getId()));
        return response;
    }

    private Map<String, Object> toMemberSummary(Room room, RoomMember member) {
        Map<String, Object> response = new LinkedHashMap<>();
        boolean isOwner = room.getOwner().getId().equals(member.getUser().getId());
        response.put("id", member.getUser().getId());
        response.put("name", member.getUser().getName());
        response.put("email", member.getUser().getEmail());
        response.put("joinedAt", member.getJoinedAt());
        response.put("owner", isOwner);
        response.put("canEditFiles", isOwner || member.isCanEditFiles());
        response.put("canSaveVersions", isOwner || member.isCanSaveVersions());
        response.put("canRevertVersions", isOwner || member.isCanRevertVersions());
        return response;
    }

    private Map<String, Object> toFileSummary(WorkspaceFile file) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", file.getId());
        response.put("filePath", file.getFilePath());
        response.put("language", file.getLanguage());
        response.put("updatedAt", file.getUpdatedAt());
        response.put("updatedByEmail", file.getUpdatedBy() != null ? file.getUpdatedBy().getEmail() : null);
        return response;
    }

    private WorkspaceFile getRoomFileById(Long roomId, Long fileId) {
        return workspaceFileRepository.findByIdAndRoom_Id(fileId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private String resolveLanguage(String requestedLanguage, String filePath) {
        if (requestedLanguage != null && !requestedLanguage.isBlank()) {
            return requestedLanguage.trim().toLowerCase(Locale.ROOT);
        }
        return FileUtil.detectLanguage(filePath);
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

    private void ensureOwner(Room room, User user) {
        if (room.getOwner() == null || !room.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only room owner can manage members");
        }
    }

    private void ensureCanEditFiles(Room room, User user) {
        if (room.getOwner() != null && room.getOwner().getId().equals(user.getId())) {
            return;
        }

        RoomMember member = roomMemberRepository.findByRoom_IdAndUser_Id(room.getId(), user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room"));

        if (!member.isCanEditFiles()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit files in this room");
        }
    }

    private String generateUniqueRoomCode() {
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            if (!roomRepository.existsByRoomCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate room code");
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }
}
