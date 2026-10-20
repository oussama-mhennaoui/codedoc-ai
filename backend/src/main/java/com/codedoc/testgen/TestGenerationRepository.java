package com.codedoc.testgen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestGenerationRepository extends JpaRepository<TestGeneration, Long> {

    List<TestGeneration> findBySourceFileProjectIdOrderByCreatedAtDesc(Long projectId);

    List<TestGeneration> findByUserIdOrderByCreatedAtDesc(Long userId);
}
