package com.codedoc.chat;

import com.codedoc.ai.AiException;
import com.codedoc.ai.LlmClient;
import com.codedoc.ai.LlmResponse;
import com.codedoc.ai.PromptLoader;
import com.codedoc.ai.SchemaValidator;
import com.codedoc.chat.dto.*;
import com.codedoc.common.NotFoundException;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codedoc.ai.AILog;
import com.codedoc.ai.AILogRepository;
import com.codedoc.ai.Anonymizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final PromptLoader promptLoader;
    private final SchemaValidator schemaValidator;
    private final LlmClient llmClient;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final SourceFileRepository sourceFileRepository;
    private final ObjectMapper objectMapper;
    private final AILogRepository aiLogRepository;
    private final Anonymizer anonymizer;

    // ─── Questions suggérées prédéfinies par défaut ──────────────────────────
    public static final List<String> SUGGESTED_QUESTIONS = List.of(
            "Quelles sont les principales fonctions de ce projet ?",
            "Explique l'architecture générale du projet.",
            "Où est définie la logique d'authentification ?",
            "Quelles sont les dépendances entre les modules ?",
            "Comment ajouter une nouvelle fonctionnalité ?",
            "Quels sont les points d'entrée (endpoints) de l'API ?"
    );

    // ─── Chat (RAG) ──────────────────────────────────────────────────────────
    @Transactional
    public ChatResponse chat(User user, ChatRequest request) {
        // 1. Récupère le projet
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + request.getProjectId()));

        // 2. Récupère ou crée la conversation
        ChatConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new NotFoundException("Conversation not found: " + request.getConversationId()));
        } else {
            conversation = new ChatConversation();
            conversation.setProject(project);
            conversation.setUser(user);
            // Titre automatique = les 60 premiers caractères de la 1ère question
            String title = request.getQuestion().length() > 60
                    ? request.getQuestion().substring(0, 60) + "…"
                    : request.getQuestion();
            conversation.setTitle(title);
            conversation = conversationRepository.save(conversation);
        }

        // 3. Sauvegarde le message utilisateur
        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setRole(ChatMessage.MessageRole.USER);
        userMessage.setContent(request.getQuestion());
        messageRepository.save(userMessage);

        // 4. Construit le contexte RAG à partir des fichiers du projet
        List<SourceFile> files = sourceFileRepository.findByProjectIdOrderByUploadedAtDesc(project.getId());
        String codeContext = buildCodeContext(files);

        // 5. Construit l'historique de conversation (5 derniers échanges max)
        List<ChatMessage> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        String conversationHistory = buildConversationHistory(history);

        // 6. Construit le prompt RAG
        String promptTemplate = promptLoader.load("chatbot_rag_v1");
        String prompt = promptTemplate
                .replace("{{projectName}}", project.getName())
                .replace("{{fileCount}}", String.valueOf(files.size()))
                .replace("{{totalLines}}", String.valueOf(countTotalLines(files)))
                .replace("{{codeContext}}", codeContext)
                .replace("{{conversationHistory}}", conversationHistory)
                .replace("{{question}}", request.getQuestion());

        // 7. Appel LLM avec validation de schéma
        String rawSchema = schemaValidator.getRawSchema("chatbot_schema");
        String finalPrompt = prompt + "\n\nEXPECTED JSON SCHEMA:\n```json\n" + rawSchema + "\n```\n";
        
        long startTime = System.currentTimeMillis();
        LlmResponse llmResponse;
        try {
            llmResponse = llmClient.call(finalPrompt);
            schemaValidator.validate(llmResponse.data(), "chatbot_schema");
            int totalTok = llmResponse.totalTokens();
            int pt = (int) (totalTok * 0.7);
            int ct = totalTok - pt;
            aiLogRepository.save(new AILog(user, "chat_rag", "SUCCESS", pt, ct, (int) (System.currentTimeMillis() - startTime)));
        } catch (Exception ex) {
            aiLogRepository.save(new AILog(user, "chat_rag", "FAILURE", 0, 0, (int) (System.currentTimeMillis() - startTime)));
            throw ex;
        }

        JsonNode data = llmResponse.data();
        String answer = data.path("answer").asText("");
        List<SourceCitationDto> sources = parseSources(data.path("sources"));

        // 8. Sauvegarde la réponse de l'assistant
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(ChatMessage.MessageRole.ASSISTANT);
        assistantMessage.setContent(answer);
        try {
            assistantMessage.setSources(objectMapper.valueToTree(sources));
        } catch (Exception e) {
            log.warn("Failed to serialize sources for message", e);
        }
        messageRepository.save(assistantMessage);

        // 9. Met à jour updatedAt de la conversation
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // 10. Retourne la réponse
        ChatResponse response = new ChatResponse();
        response.setConversationId(conversation.getId());
        response.setAnswer(answer);
        response.setSources(sources);
        response.setCreatedAt(assistantMessage.getCreatedAt());
        return response;
    }

    // ─── Liste des conversations d'un projet ─────────────────────────────────
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations(User user, Long projectId) {
        return conversationRepository
                .findByProjectIdAndUserIdOrderByUpdatedAtDesc(projectId, user.getId())
                .stream()
                .map(c -> {
                    ConversationSummaryDto dto = new ConversationSummaryDto();
                    dto.setId(c.getId());
                    dto.setTitle(c.getTitle());
                    dto.setCreatedAt(c.getCreatedAt());
                    dto.setUpdatedAt(c.getUpdatedAt());
                    dto.setMessageCount(c.getMessages().size());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ─── Historique d'une conversation ───────────────────────────────────────
    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> {
                    MessageDto dto = new MessageDto();
                    dto.setId(m.getId());
                    dto.setRole(m.getRole().name());
                    dto.setContent(m.getContent());
                    dto.setCreatedAt(m.getCreatedAt());
                    if (m.getSources() != null && m.getSources().isArray()) {
                        dto.setSources(parseSources(m.getSources()));
                    } else {
                        dto.setSources(List.of());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ─── Supprime une conversation ────────────────────────────────────────────
    @Transactional
    public void deleteConversation(Long conversationId) {
        conversationRepository.deleteById(conversationId);
    }

    // ─── Questions suggérées ─────────────────────────────────────────────────
    public List<String> getSuggestedQuestions() {
        return SUGGESTED_QUESTIONS;
    }

    // ─── Helpers privés ──────────────────────────────────────────────────────

    /**
     * Construit le contexte RAG : liste numérotée de tous les fichiers du projet
     * avec leur contenu ligne par ligne pour permettre les citations précises.
     */
    private String buildCodeContext(List<SourceFile> files) {
        StringBuilder sb = new StringBuilder();
        for (SourceFile file : files) {
            sb.append("=== FILE: ").append(file.getFilename())
              .append(" (").append(file.getLanguage()).append(") ===\n");
            String[] lines = file.getContent().split("\n");
            for (int i = 0; i < lines.length; i++) {
                sb.append(String.format("L%d: %s%n", i + 1, lines[i]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Construit l'historique de conversation (10 derniers messages max)
     * pour le contexte multi-turn.
     */
    private String buildConversationHistory(List<ChatMessage> messages) {
        if (messages.isEmpty()) return "(Début de la conversation)";
        // Prend les 10 derniers messages hors le dernier (qui vient d'être ajouté)
        int start = Math.max(0, messages.size() - 11);
        int end = messages.size() - 1; // exclut le dernier message (question courante)
        if (end <= start) return "(Début de la conversation)";

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            ChatMessage msg = messages.get(i);
            String role = msg.getRole() == ChatMessage.MessageRole.USER ? "Utilisateur" : "Assistant";
            sb.append(role).append(": ").append(msg.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private long countTotalLines(List<SourceFile> files) {
        return files.stream()
                .mapToLong(f -> f.getContent().split("\n").length)
                .sum();
    }

    private List<SourceCitationDto> parseSources(JsonNode sourcesNode) {
        List<SourceCitationDto> sources = new ArrayList<>();
        if (sourcesNode == null || !sourcesNode.isArray()) return sources;
        for (JsonNode node : sourcesNode) {
            SourceCitationDto dto = new SourceCitationDto();
            dto.setFilename(node.path("filename").asText(""));
            dto.setStartLine(node.path("startLine").asInt(0));
            dto.setEndLine(node.path("endLine").asInt(0));
            dto.setSnippet(node.path("snippet").asText(""));
            sources.add(dto);
        }
        return sources;
    }
}
