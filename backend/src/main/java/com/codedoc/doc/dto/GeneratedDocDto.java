package com.codedoc.doc.dto;

import com.codedoc.doc.DocStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public record GeneratedDocDto(
    Long id,
    Long sourceFileId,
    String sourceFileName,
    String sourceFileLanguage,
    Long sourceFileSizeBytes,
    String modelUsed,
    String promptVersion,
    JsonNode rawResponse,
    DocStatus status,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt,
    List<DocSectionDto> sections
) {}
