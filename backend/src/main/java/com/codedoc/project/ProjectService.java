package com.codedoc.project;

import com.codedoc.common.NotFoundException;
import com.codedoc.project.dto.CreateProjectRequest;
import com.codedoc.project.dto.ProjectDto;
import com.codedoc.project.dto.UpdateProjectRequest;
import com.codedoc.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<ProjectDto> listProjects(User owner) {
        return projectRepository.findByOwnerIdOrderByUpdatedAtDesc(owner.getId())
                .stream()
                .map(ProjectMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProjectDto createProject(CreateProjectRequest request, User owner) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setLanguage(request.language());
        project.setOwner(owner);
        project.setArchived(false);

        Project savedProject = projectRepository.save(project);
        return ProjectMapper.toDto(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProject(Long id, User owner) {
        return projectRepository.findByIdAndOwnerId(id, owner.getId())
                .map(ProjectMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));
    }

    public ProjectDto updateProject(Long id, UpdateProjectRequest request, User owner) {
        Project project = projectRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));

        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.language() != null) {
            project.setLanguage(request.language());
        }

        return ProjectMapper.toDto(projectRepository.save(project));
    }

    public void deleteProject(Long id, User owner) {
        if (!projectRepository.existsByIdAndOwnerId(id, owner.getId())) {
            throw new NotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    public ProjectDto archiveProject(Long id, User owner) {
        Project project = projectRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));

        project.setArchived(true);
        return ProjectMapper.toDto(projectRepository.save(project));
    }
}
