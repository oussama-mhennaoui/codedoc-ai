package com.codedoc.ai;

import com.codedoc.ai.dto.AiLogDto;
import com.codedoc.ai.dto.AiMetricsDto;
import com.codedoc.ai.dto.EndpointMetricDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMetricsService {

    private final AILogRepository aiLogRepository;
    private final Anonymizer anonymizer;

    @Value("${ai.model:openai/gpt-4o-mini}")
    private String activeModel;

    @Transactional(readOnly = true)
    public AiMetricsDto getMetrics() {
        List<AILog> allLogs = aiLogRepository.findTop100ByOrderByCreatedAtDesc();

        long totalCalls = allLogs.size();
        long successfulCalls = allLogs.stream()
                .filter(l -> l.getStatus() != null && l.getStatus().toLowerCase().startsWith("success"))
                .count();
        long failedCalls = totalCalls - successfulCalls;

        double successRate = totalCalls == 0 ? 100.0 : Math.round((successfulCalls * 100.0 / totalCalls) * 10.0) / 10.0;

        double avgLatency = allLogs.stream()
                .filter(l -> l.getLatencyMs() != null)
                .mapToInt(AILog::getLatencyMs)
                .average()
                .orElse(0.0);
        avgLatency = Math.round(avgLatency);

        long totalPromptTokens = allLogs.stream()
                .filter(l -> l.getPromptTokens() != null)
                .mapToLong(AILog::getPromptTokens)
                .sum();

        long totalCompletionTokens = allLogs.stream()
                .filter(l -> l.getCompletionTokens() != null)
                .mapToLong(AILog::getCompletionTokens)
                .sum();

        long totalTokens = totalPromptTokens + totalCompletionTokens;

        // Pricing estimate for gpt-4o-mini: $0.15/1M input, $0.60/1M output
        double estimatedCost = (totalPromptTokens * 0.00000015) + (totalCompletionTokens * 0.00000060);
        estimatedCost = Math.round(estimatedCost * 10000.0) / 10000.0;

        // Endpoint grouping
        Map<String, List<AILog>> byEndpoint = allLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getEndpoint() != null ? l.getEndpoint() : "unknown"));

        List<EndpointMetricDto> endpointMetrics = byEndpoint.entrySet().stream()
                .map(entry -> {
                    String ep = entry.getKey();
                    List<AILog> logs = entry.getValue();
                    long epTotal = logs.size();
                    long epSuccess = logs.stream()
                            .filter(l -> l.getStatus() != null && l.getStatus().toLowerCase().startsWith("success"))
                            .count();
                    long epError = epTotal - epSuccess;
                    double epRate = epTotal == 0 ? 100.0 : Math.round((epSuccess * 100.0 / epTotal) * 10.0) / 10.0;
                    double epAvgLat = logs.stream()
                            .filter(l -> l.getLatencyMs() != null)
                            .mapToInt(AILog::getLatencyMs)
                            .average()
                            .orElse(0.0);
                    long epTokens = logs.stream()
                            .mapToLong(l -> (l.getPromptTokens() != null ? l.getPromptTokens() : 0) +
                                    (l.getCompletionTokens() != null ? l.getCompletionTokens() : 0))
                            .sum();

                    return EndpointMetricDto.builder()
                            .endpoint(ep)
                            .totalCalls(epTotal)
                            .successCount(epSuccess)
                            .errorCount(epError)
                            .successRatePercent(epRate)
                            .avgLatencyMs(Math.round(epAvgLat))
                            .totalTokens(epTokens)
                            .build();
                })
                .sorted(Comparator.comparingLong(EndpointMetricDto::getTotalCalls).reversed())
                .collect(Collectors.toList());

        // Status distribution
        Map<String, Long> statusDist = allLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getStatus() != null ? l.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        // Recent logs
        List<AiLogDto> recentLogs = allLogs.stream()
                .limit(50)
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return AiMetricsDto.builder()
                .totalCalls(totalCalls)
                .successfulCalls(successfulCalls)
                .failedCalls(failedCalls)
                .successRatePercent(successRate)
                .avgLatencyMs(avgLatency)
                .totalPromptTokens(totalPromptTokens)
                .totalCompletionTokens(totalCompletionTokens)
                .totalTokens(totalTokens)
                .estimatedCostUsd(estimatedCost)
                .piiRedactionsCount(anonymizer.getRedactionsCount())
                .activeModel(activeModel)
                .endpointMetrics(endpointMetrics)
                .recentLogs(recentLogs)
                .statusDistribution(statusDist)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AiLogDto> getRecentLogs() {
        return aiLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AiLogDto mapToDto(AILog log) {
        String username = null;
        try {
            if (log.getUser() != null) {
                username = log.getUser().getUsername();
            }
        } catch (Exception ignored) {}

        int pt = log.getPromptTokens() != null ? log.getPromptTokens() : 0;
        int ct = log.getCompletionTokens() != null ? log.getCompletionTokens() : 0;

        return AiLogDto.builder()
                .id(log.getId())
                .endpoint(log.getEndpoint())
                .status(log.getStatus())
                .promptTokens(log.getPromptTokens())
                .completionTokens(log.getCompletionTokens())
                .totalTokens(pt + ct)
                .latencyMs(log.getLatencyMs())
                .createdAt(log.getCreatedAt())
                .username(username != null ? username : "system")
                .build();
    }
}
