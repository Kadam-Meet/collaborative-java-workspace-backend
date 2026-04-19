package com.collab.workspace.service;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.RoomInvitation;
import com.collab.workspace.entity.RoomMember;
import com.collab.workspace.entity.RoomMemberId;
import com.collab.workspace.entity.User;
import com.collab.workspace.entity.WorkspaceComment;
import com.collab.workspace.entity.WorkspaceFile;
import com.collab.workspace.exception.CustomException;
import com.collab.workspace.repository.RoomInvitationRepository;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.RoomRepository;
import com.collab.workspace.repository.UserRepository;
import com.collab.workspace.repository.WorkspaceFileRepository;
import com.collab.workspace.repository.WorkspaceCommentRepository;
import com.collab.workspace.repository.VersionRepository;
import com.collab.workspace.repository.AnalysisReportRepository;
import com.collab.workspace.repository.CodeIssueRepository;
import com.collab.workspace.repository.ActivityEventRepository;
import com.collab.workspace.repository.NotificationRepository;
import com.collab.workspace.socket.SocketEventServer;
import com.collab.workspace.util.FileUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomWorkspaceService {

    private static final String INVITE_STATUS_PENDING = "PENDING";
    private static final String INVITE_STATUS_ACCEPTED = "ACCEPTED";
    private static final String INVITE_STATUS_EXPIRED = "EXPIRED";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter INVITE_EXPIRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_FRONTEND_BASE_URL = "https://collaborative-java-workspace-frontend.onrender.com";
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Long, FileLockState>> ROOM_FILE_LOCKS = new ConcurrentHashMap<>();

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceFileRepository workspaceFileRepository;
    private final WorkspaceCommentRepository workspaceCommentRepository;
    private final ActivityEventService activityEventService;
    private final NotificationService notificationService;
    private final SocketEventServer socketEventServer;
    private final VersionRepository versionRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final CodeIssueRepository codeIssueRepository;
    private final ActivityEventRepository activityEventRepository;
    private final NotificationRepository notificationRepository;
    private final RoomInvitationRepository roomInvitationRepository;
    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final long invitationExpirationHours;

    public RoomWorkspaceService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        UserRepository userRepository,
        WorkspaceFileRepository workspaceFileRepository,
        WorkspaceCommentRepository workspaceCommentRepository,
        ActivityEventService activityEventService,
        NotificationService notificationService,
        SocketEventServer socketEventServer,
        VersionRepository versionRepository,
        AnalysisReportRepository analysisReportRepository,
        CodeIssueRepository codeIssueRepository,
        ActivityEventRepository activityEventRepository,
        NotificationRepository notificationRepository,
        RoomInvitationRepository roomInvitationRepository,
        JavaMailSender mailSender,
        @Value("${app.frontend.base-url:https://collaborative-java-workspace-frontend.onrender.com}") String frontendBaseUrl,
        @Value("${app.invitation.expiration-hours:72}") long invitationExpirationHours
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.workspaceFileRepository = workspaceFileRepository;
        this.workspaceCommentRepository = workspaceCommentRepository;
        this.activityEventService = activityEventService;
        this.notificationService = notificationService;
        this.socketEventServer = socketEventServer;
        this.versionRepository = versionRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.codeIssueRepository = codeIssueRepository;
        this.activityEventRepository = activityEventRepository;
        this.notificationRepository = notificationRepository;
        this.roomInvitationRepository = roomInvitationRepository;
        this.mailSender = mailSender;
        this.frontendBaseUrl = frontendBaseUrl;
        this.invitationExpirationHours = invitationExpirationHours;
    }

    @Transactional
    public Map<String, Object> updateRoom(String currentUserEmail, Long roomId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureOwner(room, currentUser);

        String roomName = required(request.getRoomName(), "roomName is required").trim();
        room.setRoomName(roomName);
        roomRepository.save(room);

        activityEventService.record(room, currentUser, "ROOM_UPDATED", "Room updated", "Room renamed to " + roomName);
        socketEventServer.broadcastRoomEvent(room, "ROOM_UPDATED", Map.of(
            "roomId", room.getId(),
            "roomName", room.getRoomName(),
            "actorEmail", currentUser.getEmail()
        ));

        return toRoomSummary(room);
    }

    @Transactional
    public Map<String, Object> deleteRoom(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureOwner(room, currentUser);

        List<WorkspaceFile> files = workspaceFileRepository.findAllByRoom_Id(roomId);
        for (WorkspaceFile file : files) {
            deleteFileDependencies(file.getId());
            workspaceFileRepository.delete(file);
        }

        List<RoomMember> members = roomMemberRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        roomMemberRepository.deleteAll(members);
        activityEventRepository.deleteByRoom_Id(roomId);
        notificationRepository.deleteByRoomId(roomId);
        roomInvitationRepository.deleteByRoom_Id(roomId);

        socketEventServer.broadcastRoomEvent(room, "ROOM_DELETED", Map.of(
            "roomId", roomId,
            "actorEmail", currentUser.getEmail()
        ));
        ROOM_FILE_LOCKS.remove(roomId);
        roomRepository.delete(room);

        return Map.of("status", "OK", "roomId", roomId);
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
    public SseEmitter subscribeRoomEvents(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        return socketEventServer.subscribe(room, currentUser);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> publishRealtimePresence(String currentUserEmail, Long roomId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        if (request.getFileId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileId is required for realtime presence");
        }

        socketEventServer.broadcastRoomEvent(room, "CURSOR_UPDATE", Map.of(
            "actorEmail", currentUser.getEmail(),
            "actorName", currentUser.getName(),
            "fileId", request.getFileId(),
            "startLine", request.getStartLine() == null ? 1 : request.getStartLine(),
            "startColumn", request.getStartColumn() == null ? 1 : request.getStartColumn(),
            "endLine", request.getEndLine() == null ? 1 : request.getEndLine(),
            "endColumn", request.getEndColumn() == null ? 1 : request.getEndColumn(),
            "typing", request.getTyping() != null && request.getTyping()
        ));

        return Map.of("status", "OK");
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

        String memberEmail = normalizeEmail(required(request.getMemberEmail(), "memberEmail is required"));
        if (memberEmail.equalsIgnoreCase(currentUser.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner is already part of this room");
        }

        User member = userRepository.findByEmailIgnoreCase(memberEmail).orElse(null);
        if (member != null && roomMemberRepository.existsByRoom_IdAndUser_Id(room.getId(), member.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this room");
        }

        String token = generateInvitationToken();
        RoomInvitation invitation = roomInvitationRepository
            .findByRoom_IdAndInviteeEmailIgnoreCaseAndStatus(room.getId(), memberEmail, INVITE_STATUS_PENDING)
            .orElseGet(RoomInvitation::new);

        invitation.setRoom(room);
        invitation.setInviter(currentUser);
        invitation.setInviteeEmail(memberEmail);
        invitation.setTokenHash(sha256(token));
        invitation.setStatus(INVITE_STATUS_PENDING);
        invitation.setAcceptedAt(null);
        invitation.setAcceptedBy(null);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationExpirationHours));
        roomInvitationRepository.save(invitation);

        activityEventService.record(
            room,
            currentUser,
            "ROOM_INVITED",
            "Invitation sent",
            memberEmail + " invited to " + room.getRoomName()
        );

        if (member != null) {
            notificationService.notifyUser(
                member,
                "ROOM_INVITE",
                "Workspace invitation",
                currentUser.getName() + " invited you to join " + room.getRoomName(),
                room,
                "INVITE_ACCEPT",
                token
            );
        }

        sendRoomInvitationEmail(room, currentUser, memberEmail, token, member == null);

        socketEventServer.broadcastRoomEvent(room, "MEMBER_ADDED", Map.of(
            "actorEmail", currentUser.getEmail(),
            "memberEmail", memberEmail,
            "memberName", member != null ? member.getName() : memberEmail,
            "invitationSent", true
        ));

        return Map.of(
            "status", "INVITED",
            "memberEmail", memberEmail,
            "roomCode", room.getRoomCode(),
            "roomName", room.getRoomName()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewInvitation(String token) {
        RoomInvitation invitation = findInvitationByToken(token);
        boolean expired = invitation.getExpiresAt() == null || invitation.getExpiresAt().isBefore(LocalDateTime.now());
        boolean accepted = INVITE_STATUS_ACCEPTED.equalsIgnoreCase(invitation.getStatus());

        User invitedUser = userRepository.findByEmailIgnoreCase(invitation.getInviteeEmail()).orElse(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valid", !expired && !accepted);
        response.put("expired", expired);
        response.put("accepted", accepted);
        response.put("inviteeEmail", invitation.getInviteeEmail());
        response.put("inviterEmail", invitation.getInviter() != null ? invitation.getInviter().getEmail() : null);
        response.put("roomCode", invitation.getRoom().getRoomCode());
        response.put("roomName", invitation.getRoom().getRoomName());
        response.put("inviterName", invitation.getInviter() != null ? invitation.getInviter().getName() : null);
        response.put("requiresSignup", invitedUser == null);
        response.put("expiresAt", invitation.getExpiresAt());
        return response;
    }

    @Transactional
    public Map<String, Object> acceptInvitation(String currentUserEmail, WorkspaceRequest request) {
        String token = required(request.getInvitationToken(), "invitationToken is required");
        RoomInvitation invitation = findInvitationByToken(token);
        if (!INVITE_STATUS_PENDING.equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer active");
        }
        if (invitation.getExpiresAt() == null || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(INVITE_STATUS_EXPIRED);
            roomInvitationRepository.save(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation has expired");
        }

        String normalizedCurrentUserEmail = normalizeEmail(currentUserEmail);
        if (!normalizedCurrentUserEmail.equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This invitation belongs to a different email");
        }

        User currentUser = getUserByEmail(normalizedCurrentUserEmail);
        Room room = invitation.getRoom();
        addMemberIfMissing(room, currentUser);

        invitation.setStatus(INVITE_STATUS_ACCEPTED);
        invitation.setAcceptedBy(currentUser);
        invitation.setAcceptedAt(LocalDateTime.now());
        roomInvitationRepository.save(invitation);

        activityEventService.record(
            room,
            currentUser,
            "INVITATION_ACCEPTED",
            "Invitation accepted",
            currentUser.getEmail() + " joined " + room.getRoomName() + " via invitation"
        );

        if (invitation.getInviter() != null) {
            notificationService.notifyUser(
                invitation.getInviter(),
                "ROOM_INVITE_ACCEPTED",
                "Invitation accepted",
                currentUser.getName() + " accepted your invitation to " + room.getRoomName(),
                room
            );
        }

        socketEventServer.broadcastRoomEvent(room, "ROOM_JOINED", Map.of(
            "actorEmail", currentUser.getEmail(),
            "actorName", currentUser.getName(),
            "roomId", room.getId(),
            "roomCode", room.getRoomCode()
        ));
        socketEventServer.broadcastPresence(room);

        return Map.of(
            "status", "ACCEPTED",
            "inviteeEmail", currentUser.getEmail(),
            "inviterEmail", invitation.getInviter() != null ? invitation.getInviter().getEmail() : null,
            "roomCode", room.getRoomCode(),
            "roomName", room.getRoomName(),
            "roomId", room.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPendingInvitations(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        // Non-owners can open/read a workspace but should not see invitation management data.
        if (!isOwner(room, currentUser)) {
            return List.of();
        }

        return roomInvitationRepository.findAllByRoom_IdAndStatusOrderByCreatedAtDesc(roomId, INVITE_STATUS_PENDING)
            .stream()
            .map(this::toInvitationSummary)
            .toList();
    }

    @Transactional
    public Map<String, Object> revokeInvitation(String currentUserEmail, Long roomId, String inviteeEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureOwner(room, currentUser);

        String normalizedInviteeEmail = normalizeEmail(inviteeEmail);
        RoomInvitation invitation = roomInvitationRepository
            .findByRoom_IdAndInviteeEmailIgnoreCaseAndStatus(roomId, normalizedInviteeEmail, INVITE_STATUS_PENDING)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending invitation not found"));

        invitation.setStatus("REVOKED");
        invitation.setRevokedAt(LocalDateTime.now());
        roomInvitationRepository.save(invitation);

        notificationService.notifyUser(
            userRepository.findByEmailIgnoreCase(normalizedInviteeEmail).orElse(null),
            "ROOM_INVITE_REVOKED",
            "Invitation revoked",
            currentUser.getName() + " revoked your invitation to " + room.getRoomName(),
            room
        );

        activityEventService.record(
            room,
            currentUser,
            "INVITATION_REVOKED",
            "Invitation revoked",
            normalizedInviteeEmail + " invitation revoked from " + room.getRoomName()
        );

        return toInvitationSummary(invitation);
    }

    @Transactional
    public Map<String, Object> declineInvitation(String currentUserEmail, WorkspaceRequest request) {
        String token = required(request.getInvitationToken(), "invitationToken is required");
        User currentUser = getUserByEmail(currentUserEmail);
        RoomInvitation invitation = findInvitationByToken(token);

        if (!normalizeEmail(currentUser.getEmail()).equalsIgnoreCase(normalizeEmail(invitation.getInviteeEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This invitation belongs to a different email");
        }
        if (!INVITE_STATUS_PENDING.equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer active");
        }

        invitation.setStatus("DECLINED");
        invitation.setDeclinedAt(LocalDateTime.now());
        roomInvitationRepository.save(invitation);

        if (invitation.getInviter() != null) {
            notificationService.notifyUser(
                invitation.getInviter(),
                "ROOM_INVITE_DECLINED",
                "Invitation declined",
                currentUser.getName() + " declined your invitation to " + invitation.getRoom().getRoomName(),
                invitation.getRoom()
            );
        }

        activityEventService.record(
            invitation.getRoom(),
            currentUser,
            "INVITATION_DECLINED",
            "Invitation declined",
            currentUser.getEmail() + " declined invitation to " + invitation.getRoom().getRoomName()
        );

        Map<String, Object> response = toInvitationSummary(invitation);
        response.put("status", invitation.getStatus());
        return response;
    }

    @Transactional
    public Map<String, Object> removeMember(String currentUserEmail, Long roomId, Long memberUserId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureOwner(room, currentUser);

        if (room.getOwner() != null && room.getOwner().getId().equals(memberUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot be removed from room");
        }

        RoomMember member = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, memberUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room member not found"));

        roomMemberRepository.deleteByRoom_IdAndUser_Id(roomId, memberUserId);

        activityEventService.record(
            room,
            currentUser,
            "MEMBER_REMOVED",
            "Member removed",
            member.getUser().getEmail() + " removed from " + room.getRoomName()
        );
        socketEventServer.broadcastRoomEvent(room, "MEMBER_REMOVED", Map.of(
            "actorEmail", currentUser.getEmail(),
            "memberEmail", member.getUser().getEmail()
        ));
        socketEventServer.broadcastPresence(room);

        return Map.of("status", "OK", "memberUserId", memberUserId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoomActivity(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        return activityEventService.listRoomActivity(roomId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoomActivity(
        String currentUserEmail,
        Long roomId,
        String actorEmail,
        String eventType,
        String from,
        String to
    ) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        return activityEventService.listRoomActivityFiltered(
            roomId,
            isBlank(actorEmail) ? null : actorEmail,
            isBlank(eventType) ? null : eventType,
            parseLocalDateTimeOrNull(from),
            parseLocalDateTimeOrNull(to)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchRoom(String currentUserEmail, Long roomId, String query) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        String q = required(query, "query is required").trim().toLowerCase(Locale.ROOT);

        List<Map<String, Object>> files = workspaceFileRepository.findAllByRoom_IdOrderByUpdatedAtDesc(roomId)
            .stream()
            .filter(file -> containsIgnoreCase(file.getFilePath(), q) || containsIgnoreCase(file.getContent(), q))
            .limit(25)
            .map(this::toFileSummary)
            .toList();

        List<Map<String, Object>> versionHits = workspaceFileRepository.findAllByRoom_IdOrderByUpdatedAtDesc(roomId)
            .stream()
            .flatMap(file -> versionRepository.findAllByFile_IdOrderByVersionNumberDesc(file.getId()).stream())
            .filter(version -> containsIgnoreCase(version.getContent(), q)
                || containsIgnoreCase(version.getFile() != null ? version.getFile().getFilePath() : null, q))
            .limit(25)
            .map(version -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", version.getId());
                map.put("versionNumber", version.getVersionNumber());
                map.put("createdAt", version.getCreatedAt());
                map.put("authorName", version.getSavedBy() != null ? version.getSavedBy().getName() : null);
                map.put("authorEmail", version.getSavedBy() != null ? version.getSavedBy().getEmail() : null);
                map.put("fileId", version.getFile() != null ? version.getFile().getId() : null);
                map.put("filePath", version.getFile() != null ? version.getFile().getFilePath() : null);
                String content = version.getContent() == null ? "" : version.getContent().replace("\r", "").replace("\n", " ").trim();
                map.put("contentPreview", content.length() <= 100 ? content : content.substring(0, 100) + "...");
                return map;
            })
            .toList();

        List<Map<String, Object>> activity = activityEventService.listRoomActivity(roomId)
            .stream()
            .filter(event -> containsIgnoreCase((String) event.get("title"), q)
                || containsIgnoreCase((String) event.get("description"), q)
                || containsIgnoreCase((String) event.get("actorEmail"), q))
            .limit(25)
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", q);
        response.put("files", files);
        response.put("versions", versionHits);
        response.put("activity", activity);
        return response;
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
        if (!isBlank(request.getMemberRole())) {
            String role = request.getMemberRole().trim().toUpperCase(Locale.ROOT);
            switch (role) {
                case "VIEWER" -> {
                    member.setCanEditFiles(false);
                    member.setCanSaveVersions(false);
                    member.setCanRevertVersions(false);
                }
                case "REVIEWER" -> {
                    member.setCanEditFiles(false);
                    member.setCanSaveVersions(false);
                    member.setCanRevertVersions(true);
                }
                case "EDITOR" -> {
                    member.setCanEditFiles(true);
                    member.setCanSaveVersions(true);
                    member.setCanRevertVersions(false);
                }
                case "ADMIN" -> {
                    member.setCanEditFiles(true);
                    member.setCanSaveVersions(true);
                    member.setCanRevertVersions(true);
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role: " + role);
            }
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

    @Transactional
    public Map<String, Object> deleteRoomFile(String currentUserEmail, Long roomId, Long fileId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        WorkspaceFile file = getRoomFileById(roomId, fileId);
        String filePath = file.getFilePath();

        deleteFileDependencies(fileId);
        workspaceFileRepository.delete(file);

        activityEventService.record(room, currentUser, "FILE_DELETED", "File deleted", filePath + " was deleted");
        socketEventServer.broadcastRoomEvent(room, "FILE_DELETED", Map.of(
            "fileId", fileId,
            "filePath", filePath,
            "actorEmail", currentUser.getEmail()
        ));

        return Map.of("status", "OK", "fileId", fileId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> listFolders(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        LinkedHashSet<String> folders = new LinkedHashSet<>();
        for (WorkspaceFile file : workspaceFileRepository.findAllByRoom_IdOrderByUpdatedAtDesc(roomId)) {
            String path = file.getFilePath() == null ? "" : file.getFilePath().trim();
            if (!path.contains("/")) {
                continue;
            }
            String[] segments = path.split("/");
            String current = "";
            for (int i = 0; i < segments.length - 1; i++) {
                current = current.isBlank() ? segments[i] : current + "/" + segments[i];
                folders.add(current);
            }
        }

        return folders.stream().sorted().map(path -> Map.of("path", path)).toList();
    }

    @Transactional
    public Map<String, Object> createFolder(String currentUserEmail, Long roomId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        String folderPath = normalizeFolderPath(required(request.getFolderPath(), "folderPath is required"));
        return Map.of("status", "OK", "path", folderPath);
    }

    @Transactional
    public Map<String, Object> renameFolder(String currentUserEmail, Long roomId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        String oldPath = normalizeFolderPath(required(request.getFolderPath(), "folderPath is required"));
        String newPath = normalizeFolderPath(required(request.getNewFolderPath(), "newFolderPath is required"));
        if (oldPath.equalsIgnoreCase(newPath)) {
            return Map.of("status", "OK", "updated", 0);
        }

        List<WorkspaceFile> files = workspaceFileRepository.findAllByRoom_IdOrderByUpdatedAtDesc(roomId).stream()
            .filter(file -> file.getFilePath() != null && (file.getFilePath().equalsIgnoreCase(oldPath) || file.getFilePath().toLowerCase(Locale.ROOT).startsWith(oldPath.toLowerCase(Locale.ROOT) + "/")))
            .toList();

        for (WorkspaceFile file : files) {
            String currentPath = file.getFilePath();
            String suffix = currentPath.length() > oldPath.length() ? currentPath.substring(oldPath.length()) : "";
            String nextPath = (newPath + suffix).replace("//", "/");
            boolean exists = workspaceFileRepository.existsByRoom_IdAndFilePathIgnoreCase(roomId, nextPath);
            if (exists && !nextPath.equalsIgnoreCase(currentPath)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "File path conflict while renaming folder: " + nextPath);
            }
        }

        int updated = 0;
        for (WorkspaceFile file : files) {
            String currentPath = file.getFilePath();
            String suffix = currentPath.length() > oldPath.length() ? currentPath.substring(oldPath.length()) : "";
            String nextPath = (newPath + suffix).replace("//", "/");
            file.setFilePath(nextPath);
            file.setUpdatedAt(LocalDateTime.now());
            file.setUpdatedBy(currentUser);
            workspaceFileRepository.save(file);
            updated++;
        }

        activityEventService.record(room, currentUser, "FOLDER_RENAMED", "Folder renamed", oldPath + " renamed to " + newPath);
        socketEventServer.broadcastRoomEvent(room, "FOLDER_RENAMED", Map.of(
            "from", oldPath,
            "to", newPath,
            "updated", updated,
            "actorEmail", currentUser.getEmail()
        ));

        return Map.of("status", "OK", "updated", updated);
    }

    @Transactional
    public Map<String, Object> deleteFolder(String currentUserEmail, Long roomId, String folderPathRaw) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        ensureCanEditFiles(room, currentUser);

        String folderPath = normalizeFolderPath(required(folderPathRaw, "folderPath is required"));

        List<WorkspaceFile> files = workspaceFileRepository.findAllByRoom_IdOrderByUpdatedAtDesc(roomId).stream()
            .filter(file -> file.getFilePath() != null && (file.getFilePath().equalsIgnoreCase(folderPath) || file.getFilePath().toLowerCase(Locale.ROOT).startsWith(folderPath.toLowerCase(Locale.ROOT) + "/")))
            .toList();

        int deleted = 0;
        for (WorkspaceFile file : files) {
            deleteFileDependencies(file.getId());
            workspaceFileRepository.delete(file);
            deleted++;
        }

        activityEventService.record(room, currentUser, "FOLDER_DELETED", "Folder deleted", folderPath + " deleted with " + deleted + " files");
        socketEventServer.broadcastRoomEvent(room, "FOLDER_DELETED", Map.of(
            "path", folderPath,
            "deleted", deleted,
            "actorEmail", currentUser.getEmail()
        ));

        return Map.of("status", "OK", "deleted", deleted);
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFileLocks(String currentUserEmail, Long roomId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        ConcurrentHashMap<Long, FileLockState> locks = ROOM_FILE_LOCKS.getOrDefault(roomId, new ConcurrentHashMap<>());
        return locks.entrySet().stream().map(entry -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fileId", entry.getKey());
            map.put("lockedByEmail", entry.getValue().lockedByEmail);
            map.put("lockedByName", entry.getValue().lockedByName);
            map.put("lockedAt", entry.getValue().lockedAt);
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> acquireFileLock(String currentUserEmail, Long roomId, Long fileId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        WorkspaceFile file = getRoomFileById(roomId, fileId);

        ROOM_FILE_LOCKS.computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, FileLockState> locks = ROOM_FILE_LOCKS.get(roomId);
        FileLockState existing = locks.get(fileId);
        if (existing != null && !existing.lockedByEmail.equalsIgnoreCase(currentUser.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File is locked by " + existing.lockedByEmail);
        }

        FileLockState lockState = new FileLockState(currentUser.getEmail(), currentUser.getName(), LocalDateTime.now());
        locks.put(fileId, lockState);

        socketEventServer.broadcastRoomEvent(room, "FILE_LOCKED", Map.of(
            "fileId", fileId,
            "filePath", file.getFilePath(),
            "lockedByEmail", currentUser.getEmail(),
            "lockedByName", currentUser.getName(),
            "lockedAt", lockState.lockedAt.toString()
        ));

        return Map.of(
            "fileId", fileId,
            "filePath", file.getFilePath(),
            "lockedByEmail", currentUser.getEmail(),
            "lockedByName", currentUser.getName(),
            "lockedAt", lockState.lockedAt
        );
    }

    @Transactional
    public Map<String, Object> releaseFileLock(String currentUserEmail, Long roomId, Long fileId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        WorkspaceFile file = getRoomFileById(roomId, fileId);

        ConcurrentHashMap<Long, FileLockState> locks = ROOM_FILE_LOCKS.get(roomId);
        if (locks == null) {
            return Map.of("fileId", fileId, "released", false);
        }
        FileLockState existing = locks.get(fileId);
        if (existing == null) {
            return Map.of("fileId", fileId, "released", false);
        }
        if (!existing.lockedByEmail.equalsIgnoreCase(currentUser.getEmail()) && !isOwner(room, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lock owner or room owner can release lock");
        }

        locks.remove(fileId);
        socketEventServer.broadcastRoomEvent(room, "FILE_UNLOCKED", Map.of(
            "fileId", fileId,
            "filePath", file.getFilePath(),
            "unlockedByEmail", currentUser.getEmail()
        ));

        return Map.of("fileId", fileId, "released", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFileComments(String currentUserEmail, Long roomId, Long fileId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        getRoomFileById(roomId, fileId);

        List<WorkspaceComment> comments = workspaceCommentRepository.findAllByRoom_IdAndFile_IdOrderByCreatedAtAsc(roomId, fileId);
        return comments.stream().map(this::toCommentSummary).toList();
    }

    @Transactional
    public Map<String, Object> addFileComment(String currentUserEmail, Long roomId, Long fileId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);
        WorkspaceFile file = getRoomFileById(roomId, fileId);

        WorkspaceComment comment = new WorkspaceComment();
        comment.setRoom(room);
        comment.setFile(file);
        comment.setAuthor(currentUser);
        comment.setContent(required(request.getContent(), "content is required").trim());
        comment.setStartLine(request.getStartLine());
        comment.setStartColumn(request.getStartColumn());
        comment.setEndLine(request.getEndLine());
        comment.setEndColumn(request.getEndColumn());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment = workspaceCommentRepository.save(comment);

        socketEventServer.broadcastRoomEvent(room, "COMMENT_CREATED", Map.of(
            "commentId", comment.getId(),
            "fileId", fileId,
            "actorEmail", currentUser.getEmail()
        ));

        return toCommentSummary(comment);
    }

    @Transactional
    public Map<String, Object> replyToComment(String currentUserEmail, Long roomId, Long commentId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        WorkspaceComment parent = workspaceCommentRepository.findByIdAndRoom_Id(commentId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        WorkspaceComment reply = new WorkspaceComment();
        reply.setRoom(room);
        reply.setFile(parent.getFile());
        reply.setAuthor(currentUser);
        reply.setParent(parent);
        reply.setContent(required(request.getContent(), "content is required").trim());
        reply.setStartLine(parent.getStartLine());
        reply.setStartColumn(parent.getStartColumn());
        reply.setEndLine(parent.getEndLine());
        reply.setEndColumn(parent.getEndColumn());
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        reply = workspaceCommentRepository.save(reply);

        socketEventServer.broadcastRoomEvent(room, "COMMENT_REPLY_CREATED", Map.of(
            "commentId", reply.getId(),
            "parentCommentId", parent.getId(),
            "fileId", parent.getFile() != null ? parent.getFile().getId() : null,
            "actorEmail", currentUser.getEmail()
        ));

        return toCommentSummary(reply);
    }

    @Transactional
    public Map<String, Object> resolveComment(String currentUserEmail, Long roomId, Long commentId, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Room room = getRoomById(roomId);
        ensureMember(room, currentUser);

        WorkspaceComment comment = workspaceCommentRepository.findByIdAndRoom_Id(commentId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean resolved = request.getResolved() == null || request.getResolved();
        comment.setResolved(resolved);
        comment.setResolvedBy(resolved ? currentUser : null);
        comment.setResolvedAt(resolved ? LocalDateTime.now() : null);
        comment.setUpdatedAt(LocalDateTime.now());
        workspaceCommentRepository.save(comment);

        socketEventServer.broadcastRoomEvent(room, "COMMENT_RESOLVED", Map.of(
            "commentId", comment.getId(),
            "resolved", resolved,
            "fileId", comment.getFile() != null ? comment.getFile().getId() : null,
            "actorEmail", currentUser.getEmail()
        ));

        return toCommentSummary(comment);
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
        response.put("memberRole", resolveRoleLabel(isOwner, member));
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

    private Map<String, Object> toInvitationSummary(RoomInvitation invitation) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", invitation.getId());
        response.put("status", invitation.getStatus());
        response.put("roomId", invitation.getRoom() != null ? invitation.getRoom().getId() : null);
        response.put("roomCode", invitation.getRoom() != null ? invitation.getRoom().getRoomCode() : null);
        response.put("roomName", invitation.getRoom() != null ? invitation.getRoom().getRoomName() : null);
        response.put("inviteeEmail", invitation.getInviteeEmail());
        response.put("inviterEmail", invitation.getInviter() != null ? invitation.getInviter().getEmail() : null);
        response.put("acceptedByEmail", invitation.getAcceptedBy() != null ? invitation.getAcceptedBy().getEmail() : null);
        response.put("createdAt", invitation.getCreatedAt());
        response.put("expiresAt", invitation.getExpiresAt());
        response.put("acceptedAt", invitation.getAcceptedAt());
        response.put("declinedAt", invitation.getDeclinedAt());
        response.put("revokedAt", invitation.getRevokedAt());
        return response;
    }

    private Map<String, Object> toCommentSummary(WorkspaceComment comment) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", comment.getId());
        response.put("roomId", comment.getRoom() != null ? comment.getRoom().getId() : null);
        response.put("fileId", comment.getFile() != null ? comment.getFile().getId() : null);
        response.put("parentId", comment.getParent() != null ? comment.getParent().getId() : null);
        response.put("content", comment.getContent());
        response.put("startLine", comment.getStartLine());
        response.put("startColumn", comment.getStartColumn());
        response.put("endLine", comment.getEndLine());
        response.put("endColumn", comment.getEndColumn());
        response.put("resolved", comment.isResolved());
        response.put("resolvedByEmail", comment.getResolvedBy() != null ? comment.getResolvedBy().getEmail() : null);
        response.put("resolvedAt", comment.getResolvedAt());
        response.put("createdAt", comment.getCreatedAt());
        response.put("updatedAt", comment.getUpdatedAt());
        response.put("authorEmail", comment.getAuthor() != null ? comment.getAuthor().getEmail() : null);
        response.put("authorName", comment.getAuthor() != null ? comment.getAuthor().getName() : null);
        return response;
    }

    private WorkspaceFile getRoomFileById(Long roomId, Long fileId) {
        return workspaceFileRepository.findByIdAndRoom_Id(fileId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private String normalizeFolderPath(String value) {
        return value.trim()
            .replace("\\", "/")
            .replaceAll("^/+", "")
            .replaceAll("/+$", "")
            .replaceAll("/{2,}", "/");
    }

    private void deleteFileDependencies(Long fileId) {
        workspaceCommentRepository.deleteByFile_Id(fileId);
        versionRepository.deleteByFile_Id(fileId);
        List<com.collab.workspace.entity.AnalysisReport> reports = analysisReportRepository.findAllByFile_Id(fileId);
        for (com.collab.workspace.entity.AnalysisReport report : reports) {
            codeIssueRepository.deleteByReport_Id(report.getId());
        }
        analysisReportRepository.deleteAll(reports);
    }

    private String resolveRoleLabel(boolean isOwner, RoomMember member) {
        if (isOwner) {
            return "OWNER";
        }
        if (!member.isCanEditFiles() && !member.isCanSaveVersions() && !member.isCanRevertVersions()) {
            return "VIEWER";
        }
        if (member.isCanEditFiles() && member.isCanSaveVersions() && member.isCanRevertVersions()) {
            return "ADMIN";
        }
        if (member.isCanEditFiles() && member.isCanSaveVersions()) {
            return "EDITOR";
        }
        return "REVIEWER";
    }

    private String resolveLanguage(String requestedLanguage, String filePath) {
        if (requestedLanguage != null && !requestedLanguage.isBlank()) {
            return requestedLanguage.trim().toLowerCase(Locale.ROOT);
        }
        return FileUtil.detectLanguage(filePath);
    }

    private String normalizeEmail(String email) {
        return required(email, "email is required").trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime parseLocalDateTimeOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid datetime format: " + value);
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && query != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isOwner(Room room, User user) {
        return room.getOwner() != null && user != null && room.getOwner().getId().equals(user.getId());
    }

    private RoomInvitation findInvitationByToken(String rawToken) {
        String token = required(rawToken, "invitation token is required").trim();
        String tokenHash = sha256(token);
        return roomInvitationRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
    }

    private String generateInvitationToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash invitation token", ex);
        }
    }

    private String buildInvitationLink(String token) {
        String baseUrl = normalizeFrontendBaseUrl(frontendBaseUrl);
        return baseUrl + "/invite?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String normalizeFrontendBaseUrl(String raw) {
        String baseUrl = raw == null ? "" : raw.trim();
        if (baseUrl.isEmpty()) {
            baseUrl = DEFAULT_FRONTEND_BASE_URL;
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private void sendRoomInvitationEmail(Room room, User inviter, String inviteeEmail, String token, boolean requiresSignup) {
        String invitationLink = buildInvitationLink(token);
        String title = requiresSignup ? "You are invited to join Collaborative Java Workspace" : "Workspace invitation";
        String expiryText = LocalDateTime.now().plusHours(invitationExpirationHours).format(INVITE_EXPIRY_FORMAT);
        String actionText = requiresSignup
            ? "Create your account and accept invitation"
            : "Accept invitation";

        String html = """
            <p>Hello,</p>
            <p><strong>%s</strong> invited you to collaborate in room <strong>%s</strong> (%s).</p>
            <p><strong>Room Code:</strong> %s</p>
            <p><a href="%s">%s</a></p>
            <p>If the button does not open, copy and paste this link in your browser:</p>
            <p><a href="%s">%s</a></p>
            <p>This invitation expires at %s.</p>
            """.formatted(
            inviter != null ? inviter.getName() : "A teammate",
            room.getRoomName(),
            room.getRoomCode(),
            room.getRoomCode(),
            invitationLink,
            actionText,
            invitationLink,
            invitationLink,
            expiryText
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(inviteeEmail);
            helper.setSubject(title);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send invitation email", ex);
        }
    }

    private static class FileLockState {
        private final String lockedByEmail;
        private final String lockedByName;
        private final LocalDateTime lockedAt;

        private FileLockState(String lockedByEmail, String lockedByName, LocalDateTime lockedAt) {
            this.lockedByEmail = lockedByEmail;
            this.lockedByName = lockedByName;
            this.lockedAt = lockedAt;
        }
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
