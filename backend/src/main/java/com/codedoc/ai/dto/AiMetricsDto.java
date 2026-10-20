package com.codedoc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMetricsDto {
    private long totalCalls;
    private long successfulCalls;
    private long failedCalls;
    private double successRatePercent;
    private double avgLatencyMs;
    private long totalPromptTokens;
    private long totalCompletionTokens;
    private long totalTokens;
    private double estimatedCostUsd;
    private long piiRedactionsCount;
    private String activeModel;
    private List<EndpointMetricDto> endpointMetrics;
    private List<AiLogDto> recentLogs;
    private Map<String, Long> statusDistribution;
}
