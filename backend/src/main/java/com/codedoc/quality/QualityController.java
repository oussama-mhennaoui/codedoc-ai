package com.codedoc.quality;

import com.codedoc.quality.dto.ProjectQualitySummaryDto;
import com.codedoc.quality.dto.QualityAnalysisRequest;
import com.codedoc.quality.dto.QualityReportDto;
import com.codedoc.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;
    private final QualityExportService qualityExportService;

    @PostMapping("/analyze")
    public ResponseEntity<QualityReportDto> analyzeQuality(
            @Valid @RequestBody QualityAnalysisRequest request,
            @AuthenticationPrincipal User user) {
        QualityReportDto result = qualityService.analyzeQuality(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<QualityReportDto> getReportByFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(qualityService.getReportByFile(fileId, user));
    }

    @GetMapping("/projects/{projectId}/summary")
    public ResponseEntity<ProjectQualitySummaryDto> getProjectSummary(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(qualityService.getProjectSummary(projectId, user));
    }

    @GetMapping("/reports/{id}/export")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String format,
            @AuthenticationPrincipal User user) {

        QualityReport report = qualityService.getReportEntity(id, user);
        String filename = report.getSourceFile() != null ? report.getSourceFile().getFilename() : "file";
        String baseName = filename.replaceAll("\\.[^.]+$", "");

        byte[] content;
        String contentType;
        String extension;

        if ("md".equalsIgnoreCase(format) || "markdown".equalsIgnoreCase(format)) {
            content = qualityExportService.toMarkdown(report).getBytes(StandardCharsets.UTF_8);
            contentType = "text/markdown;charset=UTF-8";
            extension = "md";
        } else {
            content = qualityExportService.toPdf(report);
            contentType = MediaType.APPLICATION_PDF_VALUE;
            extension = "pdf";
        }

        String downloadName = "quality-report-" + baseName + "." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(content);
    }
}
