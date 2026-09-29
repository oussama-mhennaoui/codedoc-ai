package com.codedoc.sourcefile.dto;

import java.time.Instant;

public record SourceFileDto(
    Long id,
    String filename,
    String language,
    Long sizeBytes,
    String checksum,
    Instant uploadedAt
) {}
