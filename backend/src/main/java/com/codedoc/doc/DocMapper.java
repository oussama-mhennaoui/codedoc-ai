package com.codedoc.doc;

import com.codedoc.doc.dto.CommentDto;
import com.codedoc.doc.dto.DocSectionDto;
import com.codedoc.doc.dto.GeneratedDocDto;

import java.util.stream.Collectors;

public class DocMapper {

    public static GeneratedDocDto toDto(GeneratedDoc doc) {
        return new GeneratedDocDto(
            doc.getId(),
            doc.getSourceFile().getId(),
            doc.getSourceFile().getFilename(),
            doc.getSourceFile().getLanguage(),
            doc.getSourceFile().getSizeBytes(),
            doc.getModelUsed(),
            doc.getPromptVersion(),
            doc.getRawResponse(),
            doc.getStatus(),
            doc.getErrorMessage(),
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            doc.getSections().stream()
                .map(DocMapper::toDto)
                .collect(Collectors.toList())
        );
    }

    public static DocSectionDto toDto(DocSection section) {
        return new DocSectionDto(
            section.getId(),
            section.getGeneratedDoc().getId(),
            section.getTitle(),
            section.getContent(),
            section.getOrderIndex(),
            section.getType(),
            section.getCreatedAt(),
            section.getComments().stream()
                .map(DocMapper::toDto)
                .collect(Collectors.toList())
        );
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(
            comment.getId(),
            comment.getDocSection().getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getUsername(),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
