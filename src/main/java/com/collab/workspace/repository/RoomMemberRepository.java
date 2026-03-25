package com.collab.workspace.repository;

import com.collab.workspace.entity.RoomMember;
import com.collab.workspace.entity.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

	boolean existsByRoom_IdAndUser_Id(Long roomId, Long userId);

	Optional<RoomMember> findByRoom_IdAndUser_Id(Long roomId, Long userId);

	List<RoomMember> findAllByRoom_IdOrderByJoinedAtAsc(Long roomId);

	long countByRoom_Id(Long roomId);
}
