package com.codedoc.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {
    List<SecurityAudit> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<SecurityAudit> findBySourceFileIdOrderByCreatedAtDesc(Long sourceFileId);
}
