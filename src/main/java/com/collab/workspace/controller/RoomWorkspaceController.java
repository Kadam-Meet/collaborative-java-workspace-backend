package com.collab.workspace.controller;

import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.RoomWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/rooms/{roomId}/files")
    public ResponseEntity<List<Map<String, Object>>> roomFiles(
        @PathVariable Long roomId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(roomWorkspaceService.getRoomFiles(getEmail(httpRequest), roomId));
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
