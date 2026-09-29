package com.codedoc.ai;

import com.codedoc.ai.dto.ComplexityRequest;
import com.codedoc.ai.dto.ExamplesRequest;
import com.codedoc.ai.dto.GenerateDocRequest;
import com.codedoc.ai.dto.SummarizeRequest;
import com.codedoc.common.NotFoundException;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.User;
import com.codedoc.doc.GeneratedDoc;
import com.codedoc.doc.GeneratedDocRepository;
import com.codedoc.doc.DocSection;
import com.codedoc.doc.DocSectionRepository;
import com.codedoc.doc.DocStatus;
import com.codedoc.doc.SectionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final PromptLoader promptLoader;
    private final SchemaValidator schemaValidator;
    private final LlmClient llmClient;
    private final AILogRepository aiLogRepository;
    private final Anonymizer anonymizer;
    private final SourceFileRepository sourceFileRepository;
    private final ProjectRepository projectRepository;
    private final GeneratedDocRepository docRepository;
    private final DocSectionRepository sectionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiResult generateDoc(GenerateDocRequest request) {
        return executeAiTask("generate_doc", () -> {
            SourceFile file = sourceFileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new NotFoundException("Source file not found: " + request.getFileId()));

            String promptTemplate = promptLoader.load("generate_doc_v1");
            String prompt = promptTemplate
                    .replace("{{projectName}}", file.getProject().getName())
                    .replace("{{fileName}}", file.getFilename())
                    .replace("{{language}}", file.getLanguage())
                    .replace("{{sourceCode}}", anonymizer.clean(file.getContent()));

            AiResult result = processLlmCall(prompt, "generate_doc_schema");
            
            // Save document and sections
            GeneratedDoc doc = new GeneratedDoc();
            doc.setSourceFile(file);
            doc.setModelUsed("openai/gpt-4o-mini");
            doc.setPromptVersion("generate_doc_v1");
            doc.setStatus(DocStatus.SUCCESS);
            doc.setRawResponse(result.data());
            
            docRepository.save(doc);
            
            JsonNode data = result.data();
            int order = 0;
            
            if (data.has("overview")) {
                DocSection sec = new DocSection();
                sec.setGeneratedDoc(doc);
                sec.setTitle("Overview");
                sec.setContent(data.get("overview").asText());
                sec.setOrderIndex(order++);
                sec.setType(SectionType.SUMMARY);
                sectionRepository.save(sec);
            }
            
            if (data.has("architecture")) {
                DocSection sec = new DocSection();
                sec.setGeneratedDoc(doc);
                sec.setTitle("Architecture");
                sec.setContent(data.get("architecture").asText());
                sec.setOrderIndex(order++);
                sec.setType(SectionType.SUMMARY);
                sectionRepository.save(sec);
            }
            
            if (data.has("components") && data.get("components").isArray()) {
                StringBuilder content = new StringBuilder();
                for (JsonNode comp : data.get("components")) {
                    content.append("### ").append(comp.path("name").asText("")).append(" (").append(comp.path("type").asText("")).append(")\n");
                    content.append(comp.path("responsibility").asText("")).append("\n\n");
                }
                DocSection sec = new DocSection();
                sec.setGeneratedDoc(doc);
                sec.setTitle("Components");
                sec.setContent(content.toString());
                sec.setOrderIndex(order++);
                sec.setType(SectionType.CLASS);
                sectionRepository.save(sec);
            }
            
            if (data.has("publicApi") && data.get("publicApi").isArray()) {
                StringBuilder content = new StringBuilder();
                for (JsonNode api : data.get("publicApi")) {
                    content.append("### `").append(api.path("signature").asText("")).append("`\n");
                    content.append(api.path("description").asText("")).append("\n\n");
                    content.append("**Returns:** ").append(api.path("returnType").asText("")).append("\n\n");
                }
                DocSection sec = new DocSection();
                sec.setGeneratedDoc(doc);
                sec.setTitle("Public API");
                sec.setContent(content.toString());
                sec.setOrderIndex(order++);
                sec.setType(SectionType.FUNCTION);
                sectionRepository.save(sec);
            }

            // Inject docId into the result so frontend can redirect to it
            ObjectNode modifiableData = (ObjectNode) result.data();
            modifiableData.put("docId", doc.getId());
            
            return result;
        });
    }

    @Transactional
    public AiResult summarizeProject(SummarizeRequest request) {
        return executeAiTask("summarize_project", () -> {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new NotFoundException("Project not found: " + request.getProjectId()));

            List<SourceFile> files = sourceFileRepository.findByProjectIdOrderByUploadedAtDesc(project.getId());
            
            StringBuilder filesContent = new StringBuilder();
            long totalLines = 0;
            for (SourceFile file : files) {
                String cleanContent = anonymizer.clean(file.getContent());
                int lineCount = cleanContent.split("\n").length;
                totalLines += lineCount;
                
                filesContent.append("--- FILE: ").append(file.getFilename())
                        .append(" (").append(file.getLanguage()).append(", ")
                        .append(lineCount).append(" lines) ---\n")
                        .append("```").append(file.getLanguage()).append("\n")
                        .append(cleanContent).append("\n```\n\n");
            }

            String promptTemplate = promptLoader.load("summarize_project_v1");
            String prompt = promptTemplate
                    .replace("{{projectName}}", project.getName())
                    .replace("{{fileCount}}", String.valueOf(files.size()))
                    .replace("{{totalLines}}", String.valueOf(totalLines))
                    .replace("{{files}}", filesContent.toString());

            return processLlmCall(prompt, "summarize_project_schema");
        });
    }

    @Transactional
    public AiResult detectComplexity(ComplexityRequest request) {
        return executeAiTask("detect_complexity", () -> {
            SourceFile file = sourceFileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new NotFoundException("Source file not found: " + request.getFileId()));

            String promptTemplate = promptLoader.load("detect_complexity_v1");
            String prompt = promptTemplate
                    .replace("{{projectName}}", file.getProject().getName())
                    .replace("{{fileName}}", file.getFilename())
                    .replace("{{language}}", file.getLanguage())
                    .replace("{{sourceCode}}", anonymizer.clean(file.getContent()));

            return processLlmCall(prompt, "detect_complexity_schema");
        });
    }

    @Transactional
    public AiResult generateExamples(ExamplesRequest request) {
        return executeAiTask("generate_examples", () -> {
            SourceFile file = sourceFileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new NotFoundException("Source file not found: " + request.getFileId()));

            String promptTemplate = promptLoader.load("generate_examples_v1");
            String prompt = promptTemplate
                    .replace("{{projectName}}", file.getProject().getName())
                    .replace("{{fileName}}", file.getFilename())
                    .replace("{{language}}", file.getLanguage())
                    .replace("{{sourceCode}}", anonymizer.clean(file.getContent()));

            return processLlmCall(prompt, "generate_examples_schema");
        });
    }

    private AiResult processLlmCall(String prompt, String schemaName) {
        String rawSchema = schemaValidator.getRawSchema(schemaName);
        String finalPrompt = prompt + "\n\nEXPECTED JSON SCHEMA:\n```json\n" + rawSchema + "\n```\n";
        LlmResponse response = llmClient.call(finalPrompt);
        schemaValidator.validate(response.data(), schemaName);
        return AiResult.ok(response.data(), response.totalTokens());
    }

    private AiResult executeAiTask(String endpoint, Supplier<AiResult> task) {
        long startTime = System.currentTimeMillis();
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        try {
            AiResult result = task.get();
            tryLogAiCall(currentUser, endpoint, "success", startTime, result.promptTokens(), result.completionTokens());
            return result;
        } catch (AiException e) {
            log.error("AI service error for {}: {} - {}", endpoint, e.getCode(), e.getMessage());
            tryLogAiCall(currentUser, endpoint, "failure:" + e.getCode(), startTime, null, null);
            return AiResult.fail(e.getCode(), e.getMessage());
        } catch (NotFoundException e) {
            log.error("Resource not found for AI task {}: {}", endpoint, e.getMessage());
            return AiResult.fail("not_found", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in AI service for {}: ", endpoint, e);
            tryLogAiCall(currentUser, endpoint, "failure:internal", startTime, null, null);
            return AiResult.fail("internal_error", "An unexpected error occurred during AI processing");
        }
    }

    private void tryLogAiCall(User user, String endpoint, String status, long startTime, Integer promptTokens, Integer completionTokens) {
        try {
            logAiCall(user, endpoint, status, startTime, promptTokens, completionTokens);
        } catch (Exception ex) {
            log.warn("Failed to persist AI log for endpoint {}: {}", endpoint, ex.getMessage());
        }
    }

    private void logAiCall(User user, String endpoint, String status, long startTime, Integer promptTokens, Integer completionTokens) {
        long latency = System.currentTimeMillis() - startTime;
        // Truncate to 20 chars to fit VARCHAR(20) in ai_logs
        String safeStatus = status != null && status.length() > 20 ? status.substring(0, 20) : status;

        AILog aiLog = new AILog(user, endpoint, safeStatus, promptTokens, completionTokens, (int) latency);
        aiLogRepository.save(aiLog);
    }
}
