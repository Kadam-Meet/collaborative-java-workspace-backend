package com.collab.workspace.controller;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.RoomWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = "*")
@RequestMapping("/api/workspaces")
public class RoomWorkspaceController {

    private final RoomWorkspaceService roomWorkspaceService;

    public RoomWorkspaceController(RoomWorkspaceService roomWorkspaceService) {
        this.roomWorkspaceService = roomWorkspaceService;
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

    @GetMapping(value = "/rooms/{roomId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter roomEvents(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return roomWorkspaceService.subscribeRoomEvents(getEmail(httpRequest), roomId);
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
