package com.codedoc.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AILogRepository extends JpaRepository<AILog, Long> {

    List<AILog> findTop50ByOrderByCreatedAtDesc();
}
