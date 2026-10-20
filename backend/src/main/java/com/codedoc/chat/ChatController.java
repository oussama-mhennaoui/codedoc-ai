package com.codedoc.chat;

import com.codedoc.chat.dto.*;
import com.codedoc.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat
     * Envoie une question au chatbot RAG.
     * Si conversationId est null → crée une nouvelle conversation.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.chat(user, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/chat/conversations?projectId=1
     * Liste toutes les conversations de l'utilisateur pour un projet.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations(
            @AuthenticationPrincipal User user,
            @RequestParam Long projectId) {
        return ResponseEntity.ok(chatService.getConversations(user, projectId));
    }

    /**
     * GET /api/chat/conversations/{id}/messages
     * Récupère tous les messages d'une conversation (historique).
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }

    /**
     * DELETE /api/chat/conversations/{id}
     * Supprime une conversation et ses messages.
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        chatService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/chat/suggested-questions
     * Retourne les questions prédéfinies suggérées.
     */
    @GetMapping("/suggested-questions")
    public ResponseEntity<List<String>> getSuggestedQuestions() {
        return ResponseEntity.ok(chatService.getSuggestedQuestions());
    }
}
