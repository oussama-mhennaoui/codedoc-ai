package com.codedoc.sourcefile;

import com.codedoc.sourcefile.dto.SourceFileDetailDto;
import com.codedoc.sourcefile.dto.SourceFileDto;

public class SourceFileMapper {

    public static SourceFileDto toDto(SourceFile file) {
        return new SourceFileDto(
            file.getId(),
            file.getFilename(),
            file.getLanguage(),
            file.getSizeBytes(),
            file.getChecksum(),
            file.getUploadedAt()
        );
    }

    public static SourceFileDetailDto toDetailDto(SourceFile file) {
        return new SourceFileDetailDto(
            file.getId(),
            file.getFilename(),
            file.getLanguage(),
            file.getSizeBytes(),
            file.getChecksum(),
            file.getUploadedAt(),
            file.getContent(),
            file.getProject().getId()
        );
    }
}
