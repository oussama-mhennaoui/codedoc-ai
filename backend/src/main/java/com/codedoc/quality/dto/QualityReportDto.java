package com.codedoc.quality.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public record QualityReportDto(
        Long id,
        Long sourceFileId,
        String filename,
        Integer scoreGlobal,
        Integer technicalDebtMinutes,
        String technicalDebtRating,
        List<CodeSmellDto> codeSmells,
        JsonNode respectSolid,
        JsonNode duplication,
        JsonNode complexite,
        JsonNode detteTechnique,
        Instant createdAt,
        Instant updatedAt
) {}
