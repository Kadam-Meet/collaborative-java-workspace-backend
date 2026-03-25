package com.collab.workspace.socket;

import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocketEventServer {

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, ClientSession>> roomSessions = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Room room, User user) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = UUID.randomUUID().toString();

        roomSessions
            .computeIfAbsent(room.getId(), ignored -> new ConcurrentHashMap<>())
            .put(sessionId, new ClientSession(emitter, user));

        emitter.onCompletion(() -> removeSession(room.getId(), sessionId));
        emitter.onTimeout(() -> removeSession(room.getId(), sessionId));
        emitter.onError(error -> removeSession(room.getId(), sessionId));

        sendToEmitter(emitter, "CONNECTED", Map.of(
            "type", "CONNECTED",
            "roomId", room.getId(),
            "roomCode", room.getRoomCode(),
            "sessionId", sessionId,
            "createdAt", OffsetDateTime.now().toString()
        ));

        broadcastPresence(room);
        return emitter;
    }

    public void broadcastRoomEvent(Room room, String eventType, Map<String, Object> payload) {
        if (room == null) {
            return;
        }

        ConcurrentHashMap<String, ClientSession> sessions = roomSessions.get(room.getId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", eventType);
        envelope.put("roomId", room.getId());
        envelope.put("createdAt", OffsetDateTime.now().toString());
        envelope.put("payload", payload == null ? Map.of() : payload);

        for (ClientSession session : sessions.values()) {
            sendToEmitter(session.emitter, eventType, envelope);
        }
    }

    public void broadcastPresence(Room room) {
        if (room == null) {
            return;
        }
        List<Map<String, Object>> activeUsers = getActiveUsers(room.getId());
        broadcastRoomEvent(room, "ACTIVE_USERS", Map.of("users", activeUsers));
    }

    private List<Map<String, Object>> getActiveUsers(Long roomId) {
        ConcurrentHashMap<String, ClientSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<String, Object>> uniqueUsers = new LinkedHashMap<>();
        for (ClientSession session : sessions.values()) {
            User user = session.user;
            if (user == null || user.getId() == null) {
                continue;
            }
            uniqueUsers.put(user.getId(), Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail()
            ));
        }
        return new ArrayList<>(uniqueUsers.values());
    }

    private void removeSession(Long roomId, String sessionId) {
        ConcurrentHashMap<String, ClientSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            roomSessions.remove(roomId);
            return;
        }

        List<Map<String, Object>> activeUsers = getActiveUsers(roomId);
        for (ClientSession session : sessions.values()) {
            sendToEmitter(session.emitter, "ACTIVE_USERS", Map.of(
                "type", "ACTIVE_USERS",
                "roomId", roomId,
                "createdAt", OffsetDateTime.now().toString(),
                "payload", Map.of("users", activeUsers)
            ));
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private static class ClientSession {
        private final SseEmitter emitter;
        private final User user;

        private ClientSession(SseEmitter emitter, User user) {
            this.emitter = emitter;
            this.user = user;
        }
    }
}
