package com.codedoc.chat.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ChatResponse {
    private Long conversationId;
    private String answer;
    private List<SourceCitationDto> sources;
    private Instant createdAt;
}
