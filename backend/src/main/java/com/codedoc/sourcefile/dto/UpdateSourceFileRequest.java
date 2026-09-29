package com.codedoc.sourcefile.dto;

public record UpdateSourceFileRequest(
    String filename,
    String content,
    String language
) {}
