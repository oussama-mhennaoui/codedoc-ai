package com.codedoc.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GenerateDocRequest {
    @NotNull(message = "File ID is required")
    private Long fileId;
}
