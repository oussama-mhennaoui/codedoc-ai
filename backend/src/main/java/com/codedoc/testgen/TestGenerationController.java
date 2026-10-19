package com.codedoc.testgen;

import com.codedoc.ai.dto.GenerateTestsRequest;
import com.codedoc.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/test-generations")
@RequiredArgsConstructor
public class TestGenerationController {

    private final TestGenerationService testGenerationService;

    @PostMapping
    public ResponseEntity<TestGenerationDto> generate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GenerateTestsRequest request) {
        TestGenerationDto dto = testGenerationService.generateTests(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestGenerationDto> getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(testGenerationService.getById(id, user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TestGenerationDto>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(testGenerationService.getHistory(user));
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<List<TestGenerationDto>> getByProject(
            @AuthenticationPrincipal User user,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(testGenerationService.getByProject(projectId, user));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) throws IOException {
        TestGeneration gen = testGenerationService.getEntityById(id, user);

        if (gen.getStatus() != TestGenStatus.SUCCESS || gen.getResult() == null) {
            return ResponseEntity.badRequest().build();
        }

        JsonNode result = gen.getResult();
        JsonNode testFiles = result.path("testFiles");

        if (!testFiles.isArray() || testFiles.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Single file → return directly
        if (testFiles.size() == 1) {
            JsonNode file = testFiles.get(0);
            String filename = file.path("filename").asText("TestFile.java");
            String content = file.path("content").asText("");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content.getBytes(StandardCharsets.UTF_8));
        }

        // Multiple files → ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (JsonNode file : testFiles) {
                String filename = file.path("filename").asText("TestFile.java");
                String content = file.path("content").asText("");
                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"generated-tests.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(baos.toByteArray());
    }
}
