package com.codedoc.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotNull(message = "projectId is required")
    private Long projectId;

    private Long conversationId; // null = nouvelle conversation

    @NotBlank(message = "question is required")
    private String question;
}
