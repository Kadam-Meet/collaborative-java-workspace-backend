package com.collab.workspace.controller;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.RoomRepository;
import com.collab.workspace.repository.UserRepository;
import com.collab.workspace.socket.SocketEventServer;
import com.collab.workspace.service.RoomWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
public class RoomWorkspaceController {

    private final RoomWorkspaceService roomWorkspaceService;
    private final SocketEventServer socketEventServer;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    public RoomWorkspaceController(
        RoomWorkspaceService roomWorkspaceService,
        SocketEventServer socketEventServer,
        RoomRepository roomRepository,
        UserRepository userRepository,
        RoomMemberRepository roomMemberRepository
    ) {
        this.roomWorkspaceService = roomWorkspaceService;
        this.socketEventServer = socketEventServer;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @GetMapping(value = "/rooms/{roomId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeRoomEvents(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        String email = getEmail(httpRequest);
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "User not found"
            ));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Room not found"
            ));

        boolean owner = room.getOwner() != null && room.getOwner().getId().equals(user.getId());
        boolean member = roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, user.getId());
        if (!owner && !member) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "You are not a member of this room"
            );
        }

        return socketEventServer.subscribe(room, user);
    }

    @PostMapping("/rooms/{roomId}/presence")
    public ResponseEntity<Map<String, Object>> publishPresence(
        @PathVariable Long roomId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.accepted().body(
            roomWorkspaceService.publishRealtimePresence(getEmail(httpRequest), roomId, request)
        );
    }

    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Object>> createRoom(
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.createRoom(getEmail(httpRequest), request));
    }

    @PostMapping("/rooms/join")
    public ResponseEntity<Map<String, Object>> joinRoom(
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.joinRoom(getEmail(httpRequest), request));
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> updateRoom(
        @PathVariable Long roomId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.updateRoom(getEmail(httpRequest), roomId, request));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> deleteRoom(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.deleteRoom(getEmail(httpRequest), roomId));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> myRooms(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(roomWorkspaceService.getMyRooms(getEmail(httpRequest)));
    }

    @GetMapping("/rooms/by-code/{roomCode}")
    public ResponseEntity<Map<String, Object>> roomByCode(
        @PathVariable String roomCode,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.getRoomByCode(getEmail(httpRequest), roomCode));
    }

    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<List<Map<String, Object>>> roomMembers(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.getRoomMembers(getEmail(httpRequest), roomId));
    }

    @PostMapping("/rooms/{roomId}/members")
    public ResponseEntity<Map<String, Object>> addMember(
        @PathVariable Long roomId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.addMember(getEmail(httpRequest), roomId, request));
    }

    @GetMapping("/invitations/preview")
    public ResponseEntity<Map<String, Object>> previewInvitation(@RequestParam String token) {
        return ResponseEntity.ok(roomWorkspaceService.previewInvitation(token));
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.acceptInvitation(getEmail(httpRequest), request));
    }

    @DeleteMapping("/rooms/{roomId}/members/{memberUserId}")
    public ResponseEntity<Map<String, Object>> removeMember(
        @PathVariable Long roomId,
        @PathVariable Long memberUserId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.removeMember(getEmail(httpRequest), roomId, memberUserId));
    }

    @PutMapping("/rooms/{roomId}/members/{memberUserId}/permissions")
    public ResponseEntity<Map<String, Object>> updateMemberPermissions(
        @PathVariable Long roomId,
        @PathVariable Long memberUserId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
            roomWorkspaceService.updateMemberPermissions(getEmail(httpRequest), roomId, memberUserId, request)
        );
    }

    @GetMapping("/rooms/{roomId}/files")
    public ResponseEntity<List<Map<String, Object>>> roomFiles(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.getRoomFiles(getEmail(httpRequest), roomId));
    }

    @GetMapping("/rooms/{roomId}/activity")
    public ResponseEntity<List<Map<String, Object>>> roomActivity(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.getRoomActivity(getEmail(httpRequest), roomId));
    }

    @GetMapping("/rooms/{roomId}/files/{fileId}")
    public ResponseEntity<Map<String, Object>> roomFile(
        @PathVariable Long roomId,
        @PathVariable Long fileId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.getRoomFile(getEmail(httpRequest), roomId, fileId));
    }

    @PostMapping("/rooms/{roomId}/files")
    public ResponseEntity<Map<String, Object>> createRoomFile(
        @PathVariable Long roomId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.createRoomFile(getEmail(httpRequest), roomId, request));
    }

    @PutMapping("/rooms/{roomId}/files/{fileId}")
    public ResponseEntity<Map<String, Object>> saveRoomFile(
        @PathVariable Long roomId,
        @PathVariable Long fileId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.saveRoomFile(getEmail(httpRequest), roomId, fileId, request));
    }

    @DeleteMapping("/rooms/{roomId}/files/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteRoomFile(
        @PathVariable Long roomId,
        @PathVariable Long fileId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.deleteRoomFile(getEmail(httpRequest), roomId, fileId));
    }

    @GetMapping("/rooms/{roomId}/folders")
    public ResponseEntity<List<Map<String, String>>> listFolders(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.listFolders(getEmail(httpRequest), roomId));
    }

    @PostMapping("/rooms/{roomId}/folders")
    public ResponseEntity<Map<String, Object>> createFolder(
        @PathVariable Long roomId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.createFolder(getEmail(httpRequest), roomId, request));
    }

    @PutMapping("/rooms/{roomId}/folders")
    public ResponseEntity<Map<String, Object>> renameFolder(
        @PathVariable Long roomId,
        @RequestBody WorkspaceRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.renameFolder(getEmail(httpRequest), roomId, request));
    }

    @DeleteMapping("/rooms/{roomId}/folders")
    public ResponseEntity<Map<String, Object>> deleteFolder(
        @PathVariable Long roomId,
        @RequestParam("folderPath") String folderPath,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.deleteFolder(getEmail(httpRequest), roomId, folderPath));
    }

    @PostMapping(value = "/rooms/{roomId}/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadRoomFile(
        @PathVariable Long roomId,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.uploadJavaFile(getEmail(httpRequest), roomId, file));
    }

    @GetMapping("/rooms/{roomId}/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadRoomFile(
        @PathVariable Long roomId,
        @PathVariable Long fileId,
        HttpServletRequest httpRequest
    ) {
        Map<String, Object> fileData = roomWorkspaceService.getDownloadPayload(getEmail(httpRequest), roomId, fileId);
        String content = (String) fileData.get("content");
        String filename = (String) fileData.get("fileName");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
