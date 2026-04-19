package com.collab.workspace.repository;

import com.collab.workspace.entity.SoloWorkspaceVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoloWorkspaceVersionRepository extends JpaRepository<SoloWorkspaceVersion, Long> {

    List<SoloWorkspaceVersion> findAllBySoloWorkspace_IdOrderByVersionNumberDesc(Long soloWorkspaceId);

    Optional<SoloWorkspaceVersion> findByIdAndSoloWorkspace_Id(Long id, Long soloWorkspaceId);

    Optional<SoloWorkspaceVersion> findTopBySoloWorkspace_IdOrderByVersionNumberDesc(Long soloWorkspaceId);

    void deleteByIdAndSoloWorkspace_Id(Long id, Long soloWorkspaceId);

    void deleteBySoloWorkspace_IdAndVersionNumberLessThan(Long soloWorkspaceId, int versionNumber);

    void deleteAllBySoloWorkspace_Id(Long soloWorkspaceId);
}