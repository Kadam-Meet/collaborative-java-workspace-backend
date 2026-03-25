package com.collab.workspace.repository;

import com.collab.workspace.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByRoomCodeIgnoreCase(String roomCode);

	boolean existsByRoomCodeIgnoreCase(String roomCode);

	@Query("""
		select distinct r from Room r
		left join r.members m
		where r.owner.id = :userId or m.user.id = :userId
		order by r.createdAt desc
		""")
	List<Room> findAllByParticipantUserId(@Param("userId") Long userId);
}
