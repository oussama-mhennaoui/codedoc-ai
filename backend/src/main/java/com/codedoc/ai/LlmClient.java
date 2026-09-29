package com.codedoc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmClient {

    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public LlmClient(
            RestClient.Builder restClientBuilder,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public LlmResponse call(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException("missing_api_key", "LLM API key is not configured");
        }

        long startTime = System.currentTimeMillis();
        
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "response_format", Map.of("type", "json_object"),
                            "messages", List.of(
                                    Map.of("role", "user", "content", prompt)
                            )
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        if (clientResponse.getStatusCode().value() == 429) {
                            throw new AiException("quota", "LLM API quota exceeded or rate limited");
                        }
                        throw new AiException("llm_error", "LLM API client error: " + clientResponse.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        throw new AiException("llm_error", "LLM API server error: " + clientResponse.getStatusCode());
                    })
                    .body(JsonNode.class);

            long latencyMs = System.currentTimeMillis() - startTime;
            int totalTokens = response.path("usage").path("total_tokens").asInt(0);

            log.info("LLM call completed | model: {} | latency: {}ms | tokens: {}", 
                    model, latencyMs, totalTokens);

            // Extract the model's reply text from choices[0].message.content
            String rawContent = response
                    .path("choices").path(0)
                    .path("message").path("content")
                    .asText("");

            if (rawContent.isBlank()) {
                throw new AiException("llm_error", "LLM returned an empty response content");
            }

            // Strip markdown code fences if the model wrapped the JSON in ```json ... ```
            String jsonContent = rawContent.strip();
            if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("```\\s*$", "").strip();
            }

            JsonNode contentNode = objectMapper.readTree(jsonContent);
            return new LlmResponse(contentNode, latencyMs, totalTokens);

        } catch (ResourceAccessException e) {
            // Spring RestClient throws ResourceAccessException for timeouts
            throw new AiException("timeout", "LLM API request timed out", e);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException("llm_error", "Unexpected error during LLM call", e);
        }
    }
}
