package com.collab.workspace.repository;

import com.collab.workspace.entity.RoomInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {

    Optional<RoomInvitation> findByTokenHash(String tokenHash);

    Optional<RoomInvitation> findByRoom_IdAndInviteeEmailIgnoreCaseAndStatus(Long roomId, String inviteeEmail, String status);

    java.util.List<RoomInvitation> findAllByRoom_IdAndStatusOrderByCreatedAtDesc(Long roomId, String status);

    java.util.List<RoomInvitation> findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String inviteeEmail, String status);

    void deleteByRoom_Id(Long roomId);

    void deleteByRoom_IdAndInviteeEmailIgnoreCaseAndStatus(Long roomId, String inviteeEmail, String status);
}