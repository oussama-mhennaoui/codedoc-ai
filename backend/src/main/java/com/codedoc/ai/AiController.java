package com.codedoc.ai;

import com.codedoc.ai.dto.ComplexityRequest;
import com.codedoc.ai.dto.ExamplesRequest;
import com.codedoc.ai.dto.GenerateDocRequest;
import com.codedoc.ai.dto.SummarizeRequest;
import com.codedoc.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate-doc")
    public ResponseEntity<AiResult> generateDoc(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GenerateDocRequest request) {
        return handleResult(aiService.generateDoc(request));
    }

    @PostMapping("/summarize-project")
    public ResponseEntity<AiResult> summarizeProject(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SummarizeRequest request) {
        return handleResult(aiService.summarizeProject(request));
    }

    @PostMapping("/detect-complexity")
    public ResponseEntity<AiResult> detectComplexity(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ComplexityRequest request) {
        return handleResult(aiService.detectComplexity(request));
    }

    @PostMapping("/generate-examples")
    public ResponseEntity<AiResult> generateExamples(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ExamplesRequest request) {
        return handleResult(aiService.generateExamples(request));
    }

    private ResponseEntity<AiResult> handleResult(AiResult result) {
        if (result.ok()) {
            return ResponseEntity.ok(result);
        }

        HttpStatus status = switch (result.error()) {
            case "missing_api_key" -> HttpStatus.INTERNAL_SERVER_ERROR;
            case "quota" -> HttpStatus.TOO_MANY_REQUESTS;
            case "timeout" -> HttpStatus.GATEWAY_TIMEOUT;
            case "invalid_schema", "llm_error" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(status).body(result);
    }
}
