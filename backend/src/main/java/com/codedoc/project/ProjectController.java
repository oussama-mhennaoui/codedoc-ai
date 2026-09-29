package com.codedoc.project;

import com.codedoc.project.dto.CreateProjectRequest;
import com.codedoc.project.dto.ProjectDto;
import com.codedoc.project.dto.UpdateProjectRequest;
import com.codedoc.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectDto> list(@AuthenticationPrincipal User currentUser) {
        return projectService.listProjects(currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return projectService.createProject(request, currentUser);
    }

    @GetMapping("/{id}")
    public ProjectDto get(
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        return projectService.getProject(id, currentUser);
    }

    @PatchMapping("/{id}")
    public ProjectDto update(
        @PathVariable Long id,
        @RequestBody UpdateProjectRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return projectService.updateProject(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        projectService.deleteProject(id, currentUser);
    }

    @PostMapping("/{id}/archive")
    public ProjectDto archive(
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        return projectService.archiveProject(id, currentUser);
    }
}
