package com.collab.workspace.repository;

import com.collab.workspace.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findTop50ByRecipient_IdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);
}
