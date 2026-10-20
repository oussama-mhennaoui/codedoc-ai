package com.codedoc.quality.dto;

public record FileQualitySummaryDto(
        Long fileId,
        String filename,
        Integer scoreGlobal,
        Integer technicalDebtMinutes,
        String technicalDebtRating,
        Integer codeSmellsCount,
        Integer criticalSmellsCount,
        boolean analyzed
) {}
