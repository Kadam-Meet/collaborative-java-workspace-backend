package com.collab.workspace.repository;

import com.collab.workspace.entity.WorkspaceComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceCommentRepository extends JpaRepository<WorkspaceComment, Long> {

    List<WorkspaceComment> findAllByRoom_IdAndFile_IdOrderByCreatedAtAsc(Long roomId, Long fileId);

    List<WorkspaceComment> findAllByParent_IdOrderByCreatedAtAsc(Long parentId);

    Optional<WorkspaceComment> findByIdAndRoom_Id(Long id, Long roomId);

    void deleteByFile_Id(Long fileId);
}
