package com.codedoc.sourcefile.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSourceFileRequest(
    @NotBlank(message = "Filename is required") String filename,
    @NotBlank(message = "Content is required") String content,
    String language
) {}
