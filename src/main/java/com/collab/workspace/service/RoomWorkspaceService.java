package com.collab.workspace.service;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.RoomMember;
import com.collab.workspace.entity.RoomMemberId;
import com.collab.workspace.entity.User;
import com.collab.workspace.entity.WorkspaceFile;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.RoomRepository;
import com.collab.workspace.repository.UserRepository;
import com.collab.workspace.repository.WorkspaceFileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

    public RoomWorkspaceService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        UserRepository userRepository,
        WorkspaceFileRepository workspaceFileRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.workspaceFileRepository = workspaceFileRepository;
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

        return toRoomSummary(room);
    }

    @Transactional
    public Map<String, Object> joinRoom(String currentUserEmail, WorkspaceRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        String roomCode = required(request.getRoomCode(), "roomCode is required");

        Room room = roomRepository.findByRoomCodeIgnoreCase(roomCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        addMemberIfMissing(room, currentUser);
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

        return toRoomSummary(room);
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
        response.put("id", member.getUser().getId());
        response.put("name", member.getUser().getName());
        response.put("email", member.getUser().getEmail());
        response.put("joinedAt", member.getJoinedAt());
        response.put("owner", room.getOwner().getId().equals(member.getUser().getId()));
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
