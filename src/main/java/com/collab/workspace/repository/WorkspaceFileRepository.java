package com.collab.workspace.repository;

import com.collab.workspace.entity.WorkspaceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceFileRepository extends JpaRepository<WorkspaceFile, Long> {

	List<WorkspaceFile> findAllByRoom_IdOrderByUpdatedAtDesc(Long roomId);

	long countByRoom_Id(Long roomId);
}
