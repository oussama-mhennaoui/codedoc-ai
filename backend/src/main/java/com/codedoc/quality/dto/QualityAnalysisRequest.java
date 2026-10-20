package com.codedoc.quality.dto;

import jakarta.validation.constraints.NotNull;

public record QualityAnalysisRequest(
        @NotNull(message = "fileId is required")
        Long fileId
) {}
