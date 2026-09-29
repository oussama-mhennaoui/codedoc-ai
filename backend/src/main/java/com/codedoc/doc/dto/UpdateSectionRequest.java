package com.codedoc.doc.dto;

import com.codedoc.doc.SectionType;

public record UpdateSectionRequest(
    String title,
    String content,
    Integer orderIndex,
    SectionType type
) {}
