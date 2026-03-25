package com.collab.workspace.service;

import com.collab.workspace.entity.Notification;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.RoomMember;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.NotificationRepository;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.UserRepository;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        RoomMemberRepository roomMemberRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @Transactional
    public void notifyUser(User recipient, String type, String title, String message, Room room) {
        if (recipient == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);

        if (room != null) {
            notification.setRoomId(room.getId());
            notification.setRoomCode(room.getRoomCode());
            notification.setRoomName(room.getRoomName());
        }

        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyRoomMembers(Room room, User actor, String type, String title, String message, boolean includeActor) {
        if (room == null) {
            return;
        }

        List<RoomMember> members = roomMemberRepository.findAllByRoom_IdOrderByJoinedAtAsc(room.getId());
        for (RoomMember member : members) {
            User recipient = member.getUser();
            if (recipient == null) {
                continue;
            }
            if (!includeActor && actor != null && actor.getId().equals(recipient.getId())) {
                continue;
            }
            notifyUser(recipient, type, title, message, room);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listNotifications(String currentUserEmail, boolean unreadOnly, int limit) {
        User user = getUserByEmail(currentUserEmail);
        List<Notification> notifications = unreadOnly
            ? notificationRepository.findTop50ByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(user.getId())
            : notificationRepository.findTop50ByRecipient_IdOrderByCreatedAtDesc(user.getId());

        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> summaries = notifications.stream()
            .limit(safeLimit)
            .map(this::toSummary)
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("unreadCount", notificationRepository.countByRecipient_IdAndReadAtIsNull(user.getId()));
        response.put("notifications", summaries);
        return response;
    }

    @Transactional
    public Map<String, Object> markRead(String currentUserEmail, Long notificationId) {
        User user = getUserByEmail(currentUserEmail);
        Notification notification = notificationRepository.findByIdAndRecipient_Id(notificationId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return toSummary(notification);
    }

    @Transactional
    public Map<String, Object> markAllRead(String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        List<Notification> unread = notificationRepository.findTop50ByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(user.getId());

        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : unread) {
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("updated", unread.size());
        response.put("unreadCount", 0);
        return response;
    }

    private Map<String, Object> toSummary(Notification notification) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getType());
        map.put("title", notification.getTitle());
        map.put("message", notification.getMessage());
        map.put("createdAt", notification.getCreatedAt());
        map.put("readAt", notification.getReadAt());
        map.put("read", notification.getReadAt() != null);
        map.put("roomId", notification.getRoomId());
        map.put("roomCode", notification.getRoomCode());
        map.put("roomName", notification.getRoomName());
        return map;
    }

    private User getUserByEmail(String email) {
        String normalized = required(email, "Authenticated user email is missing")
            .trim()
            .toLowerCase(Locale.ROOT);

        return userRepository.findByEmailIgnoreCase(normalized)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }
}
