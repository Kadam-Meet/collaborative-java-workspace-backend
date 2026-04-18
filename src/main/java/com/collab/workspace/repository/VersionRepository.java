package com.collab.workspace.repository;

import com.collab.workspace.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<Version, Long> {

	List<Version> findAllByFile_IdOrderByVersionNumberDesc(Long fileId);

	Optional<Version> findByIdAndFile_Id(Long id, Long fileId);

	void deleteByIdAndFile_Id(Long id, Long fileId);

	Optional<Version> findTopByFile_IdOrderByVersionNumberDesc(Long fileId);

	long countByFile_Id(Long fileId);

	long countByFile_Room_IdIn(List<Long> roomIds);

	void deleteByFile_Id(Long fileId);

	List<Version> findTop20ByFile_Room_IdInOrderByCreatedAtDesc(List<Long> roomIds);
}
