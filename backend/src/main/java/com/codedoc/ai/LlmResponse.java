package com.codedoc.ai;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmResponse(
        JsonNode data,
        long latencyMs,
        int totalTokens
) {
}
