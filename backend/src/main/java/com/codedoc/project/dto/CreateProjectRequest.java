package com.codedoc.project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
    @NotBlank String name,
    String description,
    String language
) {}
