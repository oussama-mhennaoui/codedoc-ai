package com.codedoc.sourcefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourceFileRepository extends JpaRepository<SourceFile, Long> {

    List<SourceFile> findByProjectIdOrderByUploadedAtDesc(Long projectId);

    Optional<SourceFile> findByIdAndProjectOwnerId(Long id, Long ownerId);

    long countByProjectId(Long projectId);
}
