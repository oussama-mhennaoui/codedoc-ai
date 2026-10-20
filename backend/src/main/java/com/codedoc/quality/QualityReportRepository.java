package com.codedoc.quality;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QualityReportRepository extends JpaRepository<QualityReport, Long> {

    Optional<QualityReport> findBySourceFileId(Long sourceFileId);

    List<QualityReport> findBySourceFileProjectId(Long projectId);

    boolean existsBySourceFileId(Long sourceFileId);

    void deleteBySourceFileId(Long sourceFileId);
}
