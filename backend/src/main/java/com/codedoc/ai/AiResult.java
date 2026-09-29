package com.codedoc.ai;

import com.fasterxml.jackson.databind.JsonNode;

public record AiResult(
        boolean ok,
        JsonNode data,
        String error,
        String detail,
        Integer promptTokens,
        Integer completionTokens
) {
    public static AiResult ok(JsonNode data, int totalTokens) {
        // OpenRouter returns total_tokens; split approximately for logging
        int half = totalTokens / 2;
        return new AiResult(true, data, null, null, half, totalTokens - half);
    }

    public static AiResult ok(JsonNode data) {
        return new AiResult(true, data, null, null, null, null);
    }

    public static AiResult fail(String error, String detail) {
        return new AiResult(false, null, error, detail, null, null);
    }

    public AiResult {
        if (!ok && error == null) {
            throw new IllegalArgumentException("Error must be provided when ok is false");
        }
    }
}

