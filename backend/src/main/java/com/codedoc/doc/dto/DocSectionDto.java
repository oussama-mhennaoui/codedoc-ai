package com.codedoc.doc.dto;

import com.codedoc.doc.SectionType;
import java.time.Instant;
import java.util.List;

public record DocSectionDto(
    Long id,
    Long generatedDocId,
    String title,
    String content,
    Integer orderIndex,
    SectionType type,
    Instant createdAt,
    List<CommentDto> comments
) {}
