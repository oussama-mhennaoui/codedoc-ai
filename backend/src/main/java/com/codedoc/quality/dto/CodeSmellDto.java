package com.codedoc.quality.dto;

public record CodeSmellDto(
        String type,
        String fichier,
        Integer ligne,
        String gravite,
        String description,
        String suggestionRefactoring
) {}
