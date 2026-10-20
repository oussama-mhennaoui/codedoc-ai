package com.codedoc.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointMetricDto {
    private String endpoint;
    private long totalCalls;
    private long successCount;
    private long errorCount;
    private double successRatePercent;
    private double avgLatencyMs;
    private long totalTokens;
}
