package com.codedoc.quality.dto;

public record CodeSmellDto(
        String type,
        String fichier,
        Integer ligne,
        String gravite,
        String description,
        String suggestionRefactoring,
        String codeOriginal,
        String codeRefactorise
) {
    public CodeSmellDto(
            String type,
            String fichier,
            Integer ligne,
            String gravite,
            String description,
            String suggestionRefactoring
    ) {
        this(type, fichier, ligne, gravite, description, suggestionRefactoring, null, null);
    }
}
