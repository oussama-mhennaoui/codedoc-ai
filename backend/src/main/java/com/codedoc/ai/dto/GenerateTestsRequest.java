package com.codedoc.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GenerateTestsRequest {

    /** Optional: generate tests for a file already uploaded to a project */
    private Long fileId;

    /** Optional: paste source code directly */
    @Size(max = 100_000, message = "Source code must not exceed 100 000 characters")
    private String sourceCode;

    /** Programming language (java, javascript, typescript, python) — auto-detected if omitted */
    private String language;

    /** Test framework (junit5, jest, vitest, pytest) — auto-detected from language if omitted */
    private String testFramework;
}
