package com.collab.workspace.repository;

import com.collab.workspace.entity.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    List<ActivityEvent> findTop30ByRoom_IdInOrderByCreatedAtDesc(List<Long> roomIds);

    List<ActivityEvent> findTop30ByRoom_IdOrderByCreatedAtDesc(Long roomId);
}
