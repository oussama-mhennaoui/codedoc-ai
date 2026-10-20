package com.codedoc.testgen;

import com.codedoc.ai.*;
import com.codedoc.ai.dto.GenerateTestsRequest;
import com.codedoc.common.ForbiddenException;
import com.codedoc.common.NotFoundException;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codedoc.user.User;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestGenerationService {

    private final PromptLoader promptLoader;
    private final SchemaValidator schemaValidator;
    private final LlmClient llmClient;
    private final Anonymizer anonymizer;
    private final AILogRepository aiLogRepository;
    private final SourceFileRepository sourceFileRepository;
    private final TestGenerationRepository testGenerationRepository;

    private static final Map<String, String> DEFAULT_FRAMEWORK = Map.of(
            "java", "junit5",
            "javascript", "jest",
            "typescript", "jest",
            "python", "pytest"
    );

    private static final Map<String, String> FRAMEWORK_DISPLAY = Map.of(
            "junit5", "JUnit 5 + Mockito",
            "jest", "Jest",
            "vitest", "Vitest",
            "pytest", "pytest"
    );

    @Transactional
    public TestGenerationDto generateTests(GenerateTestsRequest request, User currentUser) {
        // Resolve source code and metadata
        String sourceCode;
        String language;
        String fileName;
        SourceFile sourceFile = null;

        if (request.getFileId() != null) {
            sourceFile = sourceFileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new NotFoundException("Source file not found: " + request.getFileId()));

            // Verify user owns the project
            if (!sourceFile.getProject().getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You do not have access to this file");
            }

            sourceCode = sourceFile.getContent();
            language = resolveLanguage(request.getLanguage(), sourceFile.getLanguage());
            fileName = sourceFile.getFilename();
        } else if (request.getSourceCode() != null && !request.getSourceCode().isBlank()) {
            sourceCode = request.getSourceCode();
            language = resolveLanguage(request.getLanguage(), null);
            fileName = "UserProvidedCode";
        } else {
            throw new IllegalArgumentException("Either fileId or sourceCode must be provided");
        }

        if (sourceCode.isBlank()) {
            throw new IllegalArgumentException("Source code is empty");
        }

        String testFramework = resolveTestFramework(request.getTestFramework(), language);

        // Create DB record
        TestGeneration gen = new TestGeneration();
        gen.setUser(currentUser);
        gen.setSourceFile(sourceFile);
        gen.setLanguage(language);
        gen.setTestFramework(testFramework);
        gen.setSourceCodeSnippet(sourceCode.length() > 5000 ? sourceCode.substring(0, 5000) : sourceCode);
        gen.setStatus(TestGenStatus.PENDING);
        gen.setPromptVersion("generate_tests_v1");
        gen.setModelUsed("openai/gpt-4o-mini");
        testGenerationRepository.save(gen);

        long startTime = System.currentTimeMillis();

        try {
            // Build the prompt
            String promptTemplate = promptLoader.load("generate_tests_v1");
            String prompt = promptTemplate
                    .replace("{{language}}", language)
                    .replace("{{testFramework}}", FRAMEWORK_DISPLAY.getOrDefault(testFramework, testFramework))
                    .replace("{{fileName}}", fileName)
                    .replace("{{sourceCode}}", anonymizer.clean(sourceCode));

            // Call the LLM with schema validation
            String rawSchema = schemaValidator.getRawSchema("generate_tests_schema");
            String finalPrompt = prompt + "\n\nEXPECTED JSON SCHEMA:\n```json\n" + rawSchema + "\n```\n";
            LlmResponse response = llmClient.call(finalPrompt);
            schemaValidator.validate(response.data(), "generate_tests_schema");

            // Update generation record
            gen.setResult(response.data());
            gen.setStatus(TestGenStatus.SUCCESS);
            testGenerationRepository.save(gen);

            logAiCall(currentUser, "generate_tests", "success", startTime, response.totalTokens());

            return TestGenerationDto.from(gen);

        } catch (AiException e) {
            log.error("AI error during test generation: {} - {}", e.getCode(), e.getMessage());
            gen.setStatus(TestGenStatus.FAILED);
            gen.setErrorMessage(e.getMessage());
            testGenerationRepository.save(gen);
            logAiCall(currentUser, "generate_tests", "failure:" + e.getCode(), startTime, null);
            throw e;
        } catch (IllegalArgumentException e) {
            gen.setStatus(TestGenStatus.FAILED);
            gen.setErrorMessage(e.getMessage());
            testGenerationRepository.save(gen);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during test generation", e);
            gen.setStatus(TestGenStatus.FAILED);
            gen.setErrorMessage("An unexpected error occurred");
            testGenerationRepository.save(gen);
            logAiCall(currentUser, "generate_tests", "failure:internal", startTime, null);
            throw new AiException("internal_error", "An unexpected error occurred during test generation", e);
        }
    }

    @Transactional(readOnly = true)
    public TestGenerationDto getById(Long id, User currentUser) {
        TestGeneration gen = testGenerationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test generation not found: " + id));

        if (!gen.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this test generation");
        }

        return TestGenerationDto.from(gen);
    }

    @Transactional(readOnly = true)
    public List<TestGenerationDto> getByProject(Long projectId, User currentUser) {
        // The repository query will only return records linked to files in that project
        return testGenerationRepository.findBySourceFileProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .filter(g -> g.getUser().getId().equals(currentUser.getId()))
                .map(TestGenerationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestGenerationDto> getHistory(User currentUser) {
        return testGenerationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(TestGenerationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestGeneration getEntityById(Long id, User currentUser) {
        TestGeneration gen = testGenerationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test generation not found: " + id));
        if (!gen.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this test generation");
        }
        return gen;
    }

    private String resolveLanguage(String requested, String fromFile) {
        if (requested != null && !requested.isBlank()) {
            return requested.toLowerCase().trim();
        }
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.toLowerCase().trim();
        }
        return "java"; // default
    }

    private String resolveTestFramework(String requested, String language) {
        if (requested != null && !requested.isBlank()) {
            return requested.toLowerCase().trim();
        }
        return DEFAULT_FRAMEWORK.getOrDefault(language, "junit5");
    }

    private void logAiCall(User user, String endpoint, String status, long startTime, Integer totalTokens) {
        try {
            long latency = System.currentTimeMillis() - startTime;
            String safeStatus = status != null && status.length() > 20 ? status.substring(0, 20) : status;
            Integer half = totalTokens != null ? totalTokens / 2 : null;
            Integer other = totalTokens != null ? totalTokens - half : null;
            AILog aiLog = new AILog(user, endpoint, safeStatus, half, other, (int) latency);
            aiLogRepository.save(aiLog);
        } catch (Exception ex) {
            log.warn("Failed to persist AI log for test generation: {}", ex.getMessage());
        }
    }
}
