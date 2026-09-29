package com.codedoc.doc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedDocRepository extends JpaRepository<GeneratedDoc, Long> {
    java.util.List<GeneratedDoc> findBySourceFileProjectIdOrderByCreatedAtDesc(Long projectId);
}
