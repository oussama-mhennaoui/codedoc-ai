package com.codedoc.quality.dto;

import java.util.List;

public record ProjectQualitySummaryDto(
        Long projectId,
        String projectName,
        Double averageScore,
        Integer totalTechnicalDebtMinutes,
        Integer totalCodeSmells,
        Integer totalCriticalSmells,
        List<FileQualitySummaryDto> fileSummaries
) {}
