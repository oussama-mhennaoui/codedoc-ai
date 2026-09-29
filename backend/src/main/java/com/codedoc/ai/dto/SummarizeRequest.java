package com.codedoc.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SummarizeRequest {
    @NotNull(message = "Project ID is required")
    private Long projectId;
}
