package com.collab.workspace.service;

import com.collab.workspace.entity.ActivityEvent;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.ActivityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class ActivityEventService {

    private final ActivityEventRepository activityEventRepository;

    public ActivityEventService(ActivityEventRepository activityEventRepository) {
        this.activityEventRepository = activityEventRepository;
    }

    @Transactional
    public void record(Room room, User actor, String eventType, String title, String description) {
        if (room == null || actor == null) {
            return;
        }

        ActivityEvent event = new ActivityEvent();
        event.setRoom(room);
        event.setActor(actor);
        event.setEventType(eventType);
        event.setTitle(title);
        event.setDescription(description);
        activityEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRoomActivity(Long roomId) {
        return activityEventRepository.findTop30ByRoom_IdOrderByCreatedAtDesc(roomId)
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRoomActivityFiltered(
        Long roomId,
        String actorEmail,
        String eventType,
        LocalDateTime from,
        LocalDateTime to
    ) {
        String normalizedActor = actorEmail == null ? null : actorEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedType = eventType == null ? null : eventType.trim().toUpperCase(Locale.ROOT);

        return activityEventRepository.findTop30ByRoom_IdOrderByCreatedAtDesc(roomId)
            .stream()
            .filter(event -> normalizedActor == null
                || (event.getActor() != null && event.getActor().getEmail() != null
                    && event.getActor().getEmail().trim().toLowerCase(Locale.ROOT).equals(normalizedActor)))
            .filter(event -> normalizedType == null
                || (event.getEventType() != null && event.getEventType().trim().toUpperCase(Locale.ROOT).equals(normalizedType)))
            .filter(event -> from == null || (event.getCreatedAt() != null && !event.getCreatedAt().isBefore(from)))
            .filter(event -> to == null || (event.getCreatedAt() != null && !event.getCreatedAt().isAfter(to)))
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUserActivity(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }
        return activityEventRepository.findTop30ByRoom_IdInOrderByCreatedAtDesc(roomIds)
            .stream()
            .map(this::toSummary)
            .toList();
    }

    private Map<String, Object> toSummary(ActivityEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("type", event.getEventType());
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("createdAt", event.getCreatedAt());
        map.put("roomId", event.getRoom() != null ? event.getRoom().getId() : null);
        map.put("roomName", event.getRoom() != null ? event.getRoom().getRoomName() : null);
        map.put("actorName", event.getActor() != null ? event.getActor().getName() : null);
        map.put("actorEmail", event.getActor() != null ? event.getActor().getEmail() : null);
        return map;
    }
}
