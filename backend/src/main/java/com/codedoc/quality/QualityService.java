package com.codedoc.quality;

import com.codedoc.ai.*;
import com.codedoc.common.ForbiddenException;
import com.codedoc.common.NotFoundException;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.quality.dto.*;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.Role;
import com.codedoc.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityService {

    private final QualityReportRepository qualityReportRepository;
    private final SourceFileRepository sourceFileRepository;
    private final ProjectRepository projectRepository;
    private final LlmClient llmClient;
    private final PromptLoader promptLoader;
    private final SchemaValidator schemaValidator;
    private final Anonymizer anonymizer;
    private final AILogRepository aiLogRepository;

    @Transactional
    public QualityReportDto analyzeQuality(QualityAnalysisRequest request, User currentUser) {
        SourceFile file = sourceFileRepository.findById(request.fileId())
                .orElseThrow(() -> new NotFoundException("Source file not found: " + request.fileId()));

        checkProjectAccess(file.getProject(), currentUser);

        long startTime = System.currentTimeMillis();
        String endpoint = "analyze_quality";

        try {
            String promptTemplate = promptLoader.load("analyze_quality_v1");
            String cleanedCode = anonymizer.clean(file.getContent());
            String prompt = promptTemplate
                    .replace("{{projectName}}", file.getProject().getName())
                    .replace("{{fileName}}", file.getFilename())
                    .replace("{{language}}", file.getLanguage() != null ? file.getLanguage() : "unknown")
                    .replace("{{sourceCode}}", cleanedCode);

            String rawSchema = schemaValidator.getRawSchema("analyze_quality_schema");
            String finalPrompt = prompt + "\n\nEXPECTED JSON SCHEMA:\n```json\n" + rawSchema + "\n```\n";

            LlmResponse response = llmClient.call(finalPrompt);
            schemaValidator.validate(response.data(), "analyze_quality_schema");

            int totalTokens = response.totalTokens();
            int promptTokens = totalTokens / 2;
            int completionTokens = totalTokens - promptTokens;
            tryLogAiCall(currentUser, endpoint, "success", startTime, promptTokens, completionTokens);

            JsonNode data = response.data();
            int scoreGlobal = data.path("score_global").asInt(70);
            JsonNode detteNode = data.path("dette_technique");
            int debtMinutes = detteNode.path("estimation_minutes").asInt(0);
            String debtRating = detteNode.path("niveau").asText("faible");

            QualityReport report = qualityReportRepository.findBySourceFileId(file.getId())
                    .orElseGet(() -> {
                        QualityReport newReport = new QualityReport();
                        newReport.setSourceFile(file);
                        return newReport;
                    });

            report.setScoreGlobal(scoreGlobal);
            report.setTechnicalDebtMinutes(debtMinutes);
            report.setTechnicalDebtRating(debtRating);
            report.setRawAnalysis(data);

            QualityReport saved = qualityReportRepository.save(report);
            return toDto(saved);

        } catch (AiException e) {
            log.error("AI service error for {}: {} - {}", endpoint, e.getCode(), e.getMessage());
            tryLogAiCall(currentUser, endpoint, "failure:" + e.getCode(), startTime, null, null);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in QualityService for {}: ", endpoint, e);
            tryLogAiCall(currentUser, endpoint, "failure:internal", startTime, null, null);
            throw new AiException("internal_error", "An unexpected error occurred during quality analysis: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public QualityReportDto getReportByFile(Long fileId, User currentUser) {
        SourceFile file = sourceFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Source file not found: " + fileId));
        checkProjectAccess(file.getProject(), currentUser);

        QualityReport report = qualityReportRepository.findBySourceFileId(fileId)
                .orElseThrow(() -> new NotFoundException("Quality report not found for file: " + fileId));

        return toDto(report);
    }

    @Transactional(readOnly = true)
    public QualityReport getReportEntity(Long reportId, User currentUser) {
        QualityReport report = qualityReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Quality report not found: " + reportId));
        checkProjectAccess(report.getSourceFile().getProject(), currentUser);
        return report;
    }

    @Transactional(readOnly = true)
    public ProjectQualitySummaryDto getProjectSummary(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        checkProjectAccess(project, currentUser);

        List<SourceFile> files = sourceFileRepository.findByProjectIdOrderByUploadedAtDesc(projectId);
        List<QualityReport> reports = qualityReportRepository.findBySourceFileProjectId(projectId);
        Map<Long, QualityReport> reportMap = reports.stream()
                .collect(Collectors.toMap(r -> r.getSourceFile().getId(), r -> r, (r1, r2) -> r1));

        int totalDebtMinutes = 0;
        int totalSmells = 0;
        int totalCriticalSmells = 0;
        int totalScore = 0;
        int analyzedCount = 0;

        List<FileQualitySummaryDto> fileSummaries = new ArrayList<>();
        for (SourceFile f : files) {
            QualityReport rep = reportMap.get(f.getId());
            if (rep != null) {
                analyzedCount++;
                totalScore += rep.getScoreGlobal();
                totalDebtMinutes += rep.getTechnicalDebtMinutes();

                int smellsCount = 0;
                int criticalCount = 0;
                JsonNode smellsNode = rep.getRawAnalysis().path("code_smells");
                if (smellsNode.isArray()) {
                    smellsCount = smellsNode.size();
                    for (JsonNode s : smellsNode) {
                        if ("critique".equalsIgnoreCase(s.path("gravite").asText(""))) {
                            criticalCount++;
                        }
                    }
                }
                totalSmells += smellsCount;
                totalCriticalSmells += criticalCount;

                fileSummaries.add(new FileQualitySummaryDto(
                        f.getId(),
                        f.getFilename(),
                        rep.getScoreGlobal(),
                        rep.getTechnicalDebtMinutes(),
                        rep.getTechnicalDebtRating(),
                        smellsCount,
                        criticalCount,
                        true
                ));
            } else {
                fileSummaries.add(new FileQualitySummaryDto(
                        f.getId(),
                        f.getFilename(),
                        null,
                        0,
                        "non_analyse",
                        0,
                        0,
                        false
                ));
            }
        }

        double averageScore = analyzedCount > 0
                ? Math.round((double) totalScore / analyzedCount * 10.0) / 10.0
                : 0.0;

        return new ProjectQualitySummaryDto(
                project.getId(),
                project.getName(),
                averageScore,
                totalDebtMinutes,
                totalSmells,
                totalCriticalSmells,
                fileSummaries
        );
    }

    public QualityReportDto toDto(QualityReport report) {
        List<CodeSmellDto> smells = new ArrayList<>();
        JsonNode analysis = report.getRawAnalysis();

        if (analysis != null) {
            JsonNode smellsNode = analysis.path("code_smells");
            if (smellsNode.isArray()) {
                for (JsonNode s : smellsNode) {
                    smells.add(new CodeSmellDto(
                            s.path("type").asText(""),
                            s.path("fichier").asText(report.getSourceFile() != null ? report.getSourceFile().getFilename() : ""),
                            s.path("ligne").asInt(1),
                            s.path("gravite").asText("faible"),
                            s.path("description").asText(""),
                            s.path("suggestion_refactoring").asText(""),
                            s.hasNonNull("code_original") ? s.path("code_original").asText("") : null,
                            s.hasNonNull("code_refactorise") ? s.path("code_refactorise").asText("") : null
                    ));
                }
            }
        }

        // Sort smells: critique first, then moyen, then faible
        smells.sort((a, b) -> Integer.compare(graviteRank(b.gravite()), graviteRank(a.gravite())));

        return new QualityReportDto(
                report.getId(),
                report.getSourceFile() != null ? report.getSourceFile().getId() : null,
                report.getSourceFile() != null ? report.getSourceFile().getFilename() : null,
                report.getScoreGlobal(),
                report.getTechnicalDebtMinutes(),
                report.getTechnicalDebtRating(),
                smells,
                analysis != null ? analysis.path("respect_solid") : null,
                analysis != null ? analysis.path("duplication") : null,
                analysis != null ? analysis.path("complexite") : null,
                analysis != null ? analysis.path("dette_technique") : null,
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private int graviteRank(String gravite) {
        if (gravite == null) return 0;
        return switch (gravite.toLowerCase()) {
            case "critique" -> 3;
            case "moyen" -> 2;
            case "faible" -> 1;
            default -> 0;
        };
    }

    private void checkProjectAccess(Project project, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to this project");
        }
    }

    private void tryLogAiCall(User user, String endpoint, String status, long startTime, Integer promptTokens, Integer completionTokens) {
        try {
            long latency = System.currentTimeMillis() - startTime;
            String safeStatus = status != null && status.length() > 20 ? status.substring(0, 20) : status;
            AILog aiLog = new AILog(user, endpoint, safeStatus, promptTokens, completionTokens, (int) latency);
            aiLogRepository.save(aiLog);
        } catch (Exception ex) {
            log.warn("Failed to persist AI log for endpoint {}: {}", endpoint, ex.getMessage());
        }
    }
}
