package com.collab.workspace.socket;

import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.User;
import com.collab.workspace.service.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class SocketEventServer {

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int port;
    private final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private final Map<Long, Set<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Integer>> roomUserConnections = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, String>> roomUserNames = new ConcurrentHashMap<>();
    private final Consumer<String> broadcaster = this::broadcast;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public SocketEventServer(
        EventPublisher eventPublisher,
        ObjectMapper objectMapper,
        @Value("${backend.events.enabled:true}") boolean enabled,
        @Value("${backend.events.port:9091}") int port
    ) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.port = port;
    }

    public SseEmitter subscribe(Room room, User user) {
        long roomId = room.getId();
        String email = user.getEmail();

        SseEmitter emitter = new SseEmitter(0L);
        roomEmitters.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        roomUserConnections.computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>())
            .merge(email, 1, Integer::sum);
        roomUserNames.computeIfAbsent(roomId, ignored -> new ConcurrentHashMap<>())
            .put(email, user.getName());

        emitter.onCompletion(() -> removeEmitter(roomId, email, emitter));
        emitter.onTimeout(() -> removeEmitter(roomId, email, emitter));
        emitter.onError(ignored -> removeEmitter(roomId, email, emitter));

        sendEvent(emitter, buildEvent("CONNECTED", roomId, Map.of(
            "actorEmail", email,
            "actorName", user.getName()
        )));
        broadcastActiveUsers(roomId);

        return emitter;
    }

    public void broadcastRoomEvent(Room room, String type, Map<String, Object> payload) {
        Long roomId = room.getId();
        broadcastRoomEvent(roomId, type, payload);
    }

    private void broadcastRoomEvent(Long roomId, String type, Map<String, Object> payload) {
        Map<String, Object> event = buildEvent(type, roomId, payload);
        Set<SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        ArrayList<SseEmitter> stale = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            if (!sendEvent(emitter, event)) {
                stale.add(emitter);
            }
        }
        emitters.removeAll(stale);
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            return;
        }

        eventPublisher.register(broadcaster);
        acceptThread = new Thread(this::runServer, "java-workspace-event-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void runServer() {
        try (ServerSocket socket = new ServerSocket(port)) {
            this.serverSocket = socket;
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = socket.accept();
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                clients.add(writer);
                writer.println("CONNECTED|java-workspace-events");
            }
        } catch (IOException ignored) {
        }
    }

    private void broadcast(String payload) {
        clients.removeIf(PrintWriter::checkError);
        clients.forEach(writer -> writer.println(payload));
    }

    @PreDestroy
    public void stop() {
        eventPublisher.unregister(broadcaster);
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        clients.forEach(PrintWriter::close);
        clients.clear();

        roomEmitters.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        roomEmitters.clear();
        roomUserConnections.clear();
        roomUserNames.clear();
    }

    private void removeEmitter(long roomId, String email, SseEmitter emitter) {
        Set<SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                roomEmitters.remove(roomId);
            }
        }

        Map<String, Integer> counts = roomUserConnections.get(roomId);
        if (counts != null) {
            counts.computeIfPresent(email, (ignored, value) -> value > 1 ? value - 1 : null);
            if (counts.isEmpty()) {
                roomUserConnections.remove(roomId);
                roomUserNames.remove(roomId);
            } else {
                Map<String, String> names = roomUserNames.get(roomId);
                if (names != null && !counts.containsKey(email)) {
                    names.remove(email);
                }
            }
        }

        broadcastActiveUsers(roomId);
    }

    private void broadcastActiveUsers(long roomId) {
        Map<String, String> names = roomUserNames.getOrDefault(roomId, Map.of());
        ArrayList<Map<String, Object>> users = new ArrayList<>();
        names.forEach((email, name) -> users.add(Map.of("email", email, "name", name)));
        broadcastRoomEvent(roomId, "ACTIVE_USERS", Map.of("users", users));
    }

    private Map<String, Object> buildEvent(String type, Long roomId, Map<String, Object> payload) {
        HashMap<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("roomId", roomId);
        event.put("createdAt", Instant.now().toString());
        event.put("payload", payload == null ? Map.of() : payload);
        return event;
    }

    private boolean sendEvent(SseEmitter emitter, Map<String, Object> event) {
        String type = String.valueOf(event.get("type"));
        try {
            emitter.send(SseEmitter.event().name(type).data(objectMapper.writeValueAsString(event)));
            return true;
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return false;
        }
    }
}
