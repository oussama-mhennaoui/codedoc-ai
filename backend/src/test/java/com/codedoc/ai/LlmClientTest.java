package com.codedoc.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(value = LlmClient.class, properties = {
        "llm.base-url=https://api.openai.com/v1",
        "llm.api-key=test-key",
        "llm.model=gpt-4o-mini"
})
class LlmClientTest {

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String API_KEY = "test-key";
    private static final String MODEL = "gpt-4o-mini";

    @Test
    void call_ShouldReturnResponse_WhenSuccessful() throws Exception {
        // Prepare response
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("id", "chatcmpl-123");
        responseNode.putObject("usage").put("total_tokens", 50);
        
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseNode), MediaType.APPLICATION_JSON));

        LlmResponse response = llmClient.call("Hello AI");

        assertThat(response).isNotNull();
        assertThat(response.totalTokens()).isEqualTo(50);
        assertThat(response.data().get("id").asText()).isEqualTo("chatcmpl-123");
        server.verify();
    }

    @Test
    void call_ShouldThrowQuotaException_WhenStatus429() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiException exception = assertThrows(AiException.class, () -> llmClient.call("Hello AI"));
        assertThat(exception.getCode()).isEqualTo("quota");
        server.verify();
    }

    @Test
    void call_ShouldThrowTimeoutException_WhenTimeoutOccurs() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withException(new java.io.IOException("Read timed out")));

        AiException exception = assertThrows(AiException.class, () -> llmClient.call("Hello AI"));
        assertThat(exception.getCode()).isEqualTo("timeout");
        server.verify();
    }

    @Test
    void call_ShouldThrowGenericLlmError_WhenStatus500() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withServerError());

        AiException exception = assertThrows(AiException.class, () -> llmClient.call("Hello AI"));
        assertThat(exception.getCode()).isEqualTo("llm_error");
        server.verify();
    }

    @Test
    void call_ShouldThrowMissingApiKeyException_WhenApiKeyIsEmpty() {
        // We need a client with empty API key
        LlmClient clientWithNoKey = new LlmClient(
                org.springframework.web.client.RestClient.builder(),
                BASE_URL,
                "",
                MODEL,
                objectMapper
        );

        AiException exception = assertThrows(AiException.class, () -> clientWithNoKey.call("Hello AI"));
        assertThat(exception.getCode()).isEqualTo("missing_api_key");
    }
}
