package com.collab.workspace.repository;

import com.collab.workspace.entity.SoloWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoloWorkspaceRepository extends JpaRepository<SoloWorkspace, Long> {

    List<SoloWorkspace> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<SoloWorkspace> findByIdAndUser_Id(Long id, Long userId);

    Optional<SoloWorkspace> findTopByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<SoloWorkspace> findByUser_IdAndFileNameIgnoreCase(Long userId, String fileName);

    boolean existsByUser_IdAndFileNameIgnoreCase(Long userId, String fileName);
}