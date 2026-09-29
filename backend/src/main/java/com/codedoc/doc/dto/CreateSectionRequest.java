package com.codedoc.doc.dto;

import com.codedoc.doc.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSectionRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotNull Integer orderIndex,
    @NotNull SectionType type
) {}
