package com.collab.workspace.service;

import com.collab.workspace.dto.NotificationResponse;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class NotificationService {


private final NotificationRepository notificationRepository;
private final UserRepository userRepository;
private final RoomMemberRepository roomMemberRepository;

public NotificationService(NotificationRepository notificationRepository,
                                                   UserRepository userRepository,
                                                   RoomMemberRepository roomMemberRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
}

public Map<String, Object> listNotifications(String email, boolean unreadOnly, int limit) {

    User user = getUser(email);

    List<Notification> notifications = unreadOnly
            ? notificationRepository.findTop50ByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(user.getId())
            : notificationRepository.findTop50ByRecipient_IdOrderByCreatedAtDesc(user.getId());

    // 🔥 limit handling
    if (limit > 0 && notifications.size() > limit) {
        notifications = notifications.subList(0, limit);
    }

    // 🔥 Convert to DTO
    List<NotificationResponse> responseList = notifications.stream()
            .map(this::toDto)
            .toList();

        long unreadCount = notificationRepository.countByRecipient_IdAndReadAtIsNull(user.getId());

    return Map.of(
            "notifications", responseList,
            "unreadCount", unreadCount
    );
}

public NotificationResponse markRead(String email, Long notificationId) {
    User user = getUser(email);

    Notification notification = notificationRepository
            .findByIdAndRecipient_Id(notificationId, user.getId())
            .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found")
            );

    if (notification.getReadAt() == null) {
        notification.setReadAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
    }

    return toDto(notification);
}

public Map<String, Object> markAllRead(String email) {
    User user = getUser(email);

    List<Notification> unread = notificationRepository
            .findTop50ByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(user.getId());

    unread.forEach(n -> {
        n.setReadAt(java.time.LocalDateTime.now());
    });

    notificationRepository.saveAll(unread);

    return Map.of(
            "updated", unread.size(),
            "unreadCount", 0
    );
}

// =========================
// 🔥 Helper Methods
// =========================

private NotificationResponse toDto(Notification n) {
    return new NotificationResponse(
            n.getId(),
                        n.getType(),
            n.getTitle(),
            n.getMessage(),
                        n.getReadAt() != null,
                        n.getRoomId(),
                        n.getRoomCode(),
                        n.getRoomName(),
                        n.getActionType(),
                        n.getActionToken(),
            n.getCreatedAt(),
            n.getReadAt()
    );
}

private User getUser(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmailIgnoreCase(normalized)
            .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
            );
}

public Notification notifyUser(User recipient, String type, String title, String message, Room room) {
        return notifyUser(recipient, type, title, message, room, null, null);
}

public Notification notifyUser(
        User recipient,
        String type,
        String title,
        String message,
        Room room,
        String actionType,
        String actionToken
) {
        if (recipient == null) {
                return null;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type == null || type.isBlank() ? "GENERAL" : type.trim());
        notification.setTitle(title == null ? "Notification" : title.trim());
        notification.setMessage(message == null ? "" : message.trim());
        if (room != null) {
                notification.setRoomId(room.getId());
                notification.setRoomCode(room.getRoomCode());
                notification.setRoomName(room.getRoomName());
        }
        notification.setActionType(actionType);
        notification.setActionToken(actionToken);
        return notificationRepository.save(notification);
}

public int notifyRoomMembers(
        Room room,
        User actor,
        String type,
        String title,
        String message,
        boolean includeActor
) {
        if (room == null) {
                return 0;
        }

        List<RoomMember> members = roomMemberRepository.findAllByRoom_IdOrderByJoinedAtAsc(room.getId());
        int sent = 0;
        for (RoomMember member : members) {
                User recipient = member.getUser();
                if (recipient == null) {
                        continue;
                }
                if (!includeActor && actor != null && actor.getId().equals(recipient.getId())) {
                        continue;
                }
                if (notifyUser(recipient, type, title, message, room) != null) {
                        sent++;
                }
        }

        return sent;
}


}
