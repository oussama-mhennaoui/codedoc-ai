package com.codedoc.project.dto;

public record UpdateProjectRequest(
    String name,
    String description,
    String language
) {}
