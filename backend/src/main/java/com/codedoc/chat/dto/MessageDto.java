package com.codedoc.chat.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class MessageDto {
    private Long id;
    private String role; // "USER" | "ASSISTANT"
    private String content;
    private List<SourceCitationDto> sources;
    private Instant createdAt;
}
