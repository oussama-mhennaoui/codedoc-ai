package com.codedoc.doc.dto;

import java.time.Instant;

public record CommentDto(
    Long id,
    Long docSectionId,
    Long authorId,
    String authorName,
    String content,
    Instant createdAt
) {}
