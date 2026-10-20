package com.codedoc.chat.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ConversationSummaryDto {
    private Long id;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private int messageCount;
}
