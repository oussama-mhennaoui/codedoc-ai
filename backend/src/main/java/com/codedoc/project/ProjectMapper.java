package com.codedoc.project;

import com.codedoc.project.dto.ProjectDto;

public class ProjectMapper {

    public static ProjectDto toDto(Project project) {
        return new ProjectDto(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getLanguage(),
            project.getCreatedAt(),
            project.getUpdatedAt(),
            project.isArchived(),
            0L // fileCount set to 0 as ProjectFile entity is not yet implemented
        );
    }
}
