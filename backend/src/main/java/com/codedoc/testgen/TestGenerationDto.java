package com.codedoc.testgen;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record TestGenerationDto(
        Long id,
        Long sourceFileId,
        String sourceFileName,
        String language,
        String testFramework,
        String status,
        JsonNode result,
        String errorMessage,
        String modelUsed,
        Instant createdAt
) {
    public static TestGenerationDto from(TestGeneration entity) {
        return new TestGenerationDto(
                entity.getId(),
                entity.getSourceFile() != null ? entity.getSourceFile().getId() : null,
                entity.getSourceFile() != null ? entity.getSourceFile().getFilename() : null,
                entity.getLanguage(),
                entity.getTestFramework(),
                entity.getStatus().name(),
                entity.getResult(),
                entity.getErrorMessage(),
                entity.getModelUsed(),
                entity.getCreatedAt()
        );
    }
}
