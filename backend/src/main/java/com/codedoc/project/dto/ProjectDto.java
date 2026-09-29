package com.codedoc.project.dto;

import java.time.Instant;

public record ProjectDto(
    Long id,
    String name,
    String description,
    String language,
    Instant createdAt,
    Instant updatedAt,
    boolean archived,
    long fileCount
) {}
