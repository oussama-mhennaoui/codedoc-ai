package com.codedoc.audit;

import com.codedoc.ai.*;
import com.codedoc.audit.dto.SecurityAuditDto;
import com.codedoc.audit.dto.SecurityAuditRequest;
import com.codedoc.audit.dto.VulnerabilityDto;
import com.codedoc.common.NotFoundException;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditRepository auditRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ProjectRepository projectRepository;
    private final SourceFileRepository sourceFileRepository;
    private final AiService aiService;
    private final PromptLoader promptLoader;
    private final Anonymizer anonymizer;
    private final ObjectMapper objectMapper;

    @Transactional
    public SecurityAuditDto performAudit(SecurityAuditRequest request) {
        AiResult aiResult = aiService.executeAiTask("security_audit", () -> {
            SourceFile file = sourceFileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new NotFoundException("Source file not found: " + request.getFileId()));

            String fileName = file.getFilename().toLowerCase();
            String promptName = "security_audit_v1";

            if (fileName.equals("pom.xml") || fileName.equals("package.json") || fileName.equals("requirements.txt")
                    || fileName.equals("go.mod")) {
                promptName = "dependency_audit_v1";
            }

            String promptTemplate = promptLoader.load(promptName);
            String prompt = promptTemplate
                    .replace("{{projectName}}", file.getProject().getName())
                    .replace("{{fileName}}", file.getFilename())
                    .replace("{{language}}", file.getLanguage())
                    .replace("{{sourceCode}}", anonymizer.clean(file.getContent()));

            return aiService.processLlmCall(prompt, "security_audit_schema");
        });

        if (!aiResult.ok()) {
            throw new RuntimeException("AI Audit failed: " + aiResult.error());
        }

        SourceFile file = sourceFileRepository.findById(request.getFileId()).orElseThrow();
        String fileName = file.getFilename().toLowerCase();
        String promptVersion = "security_audit_v1";
        if (fileName.equals("pom.xml") || fileName.equals("package.json") || fileName.equals("requirements.txt")
                || fileName.equals("go.mod")) {
            promptVersion = "dependency_audit_v1";
        }

        SecurityAudit audit = new SecurityAudit();
        audit.setSourceFile(file);
        audit.setProject(file.getProject());
        audit.setStatus(AuditStatus.SUCCESS);
        audit.setModelUsed("openai/gpt-4o-mini");
        audit.setPromptVersion(promptVersion);
        audit.setRawResponse(aiResult.data().toString());

        JsonNode data = aiResult.data();
        audit.setSecurityScore(data.path("score_securite").asText("N/A"));
        audit.setSummary(data.path("summary").asText(""));

        SecurityAudit savedAudit = auditRepository.save(audit);

        List<Vulnerability> vulnerabilities = new ArrayList<>();
        JsonNode vulnsNode = data.path("vulnerabilites");
        if (vulnsNode.isArray()) {
            for (JsonNode vNode : vulnsNode) {
                Vulnerability v = new Vulnerability();
                v.setAudit(savedAudit);
                v.setType(vNode.path("type").asText());

                String severityStr = vNode.path("severite").asText("INFO").toUpperCase();
                try {
                    v.setSeverity(Severity.valueOf(severityStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown severity level: {}. Defaulting to INFO.", severityStr);
                    v.setSeverity(Severity.INFO);
                }

                v.setFileName(vNode.path("fichier").asText(file.getFilename()));
                v.setLine(vNode.path("ligne").asInt(0));
                v.setDescription(vNode.path("description").asText());
                v.setConsequences(vNode.path("consequences").asText());
                v.setFix(vNode.path("correctif").asText());
                v.setCweId(vNode.path("cwe_id").asText());
                v.setCorrectedCode(vNode.path("code_corrige").asText());
                vulnerabilities.add(v);
            }
        }
        vulnerabilityRepository.saveAll(vulnerabilities);
        savedAudit.setVulnerabilities(vulnerabilities);

        return mapToDto(savedAudit);
    }

    @Transactional(readOnly = true)
    public List<SecurityAuditDto> getAuditsByProject(Long projectId) {
        return auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SecurityAuditDto> getAuditsByFile(Long fileId) {
        return auditRepository.findBySourceFileIdOrderByCreatedAtDesc(fileId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SecurityAuditDto getAudit(Long auditId) {
        SecurityAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("Audit not found: " + auditId));
        return mapToDto(audit);
    }

    private SecurityAuditDto mapToDto(SecurityAudit audit) {
        SecurityAuditDto dto = new SecurityAuditDto();
        dto.setId(audit.getId());
        dto.setProjectId(audit.getProject() != null ? audit.getProject().getId() : null);
        dto.setSourceFileId(audit.getSourceFile() != null ? audit.getSourceFile().getId() : null);
        dto.setFileName(audit.getSourceFile() != null ? audit.getSourceFile().getFilename() : "Project Audit");
        dto.setStatus(audit.getStatus());
        dto.setSecurityScore(audit.getSecurityScore());
        dto.setSummary(audit.getSummary());
        dto.setCreatedAt(audit.getCreatedAt());
        dto.setVulnerabilities(audit.getVulnerabilities().stream()
                .map(this::mapVulnToDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private VulnerabilityDto mapVulnToDto(Vulnerability v) {
        VulnerabilityDto dto = new VulnerabilityDto();
        dto.setId(v.getId());
        dto.setType(v.getType());
        dto.setSeverity(v.getSeverity());
        dto.setFileName(v.getFileName());
        dto.setLine(v.getLine());
        dto.setDescription(v.getDescription());
        dto.setConsequences(v.getConsequences());
        dto.setFix(v.getFix());
        dto.setCweId(v.getCweId());
        dto.setCorrectedCode(v.getCorrectedCode());
        return dto;
    }
}
