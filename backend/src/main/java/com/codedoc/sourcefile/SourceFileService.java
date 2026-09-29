package com.codedoc.sourcefile;

import com.codedoc.common.ConflictException;
import com.codedoc.common.ForbiddenException;
import com.codedoc.common.NotFoundException;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.sourcefile.dto.CreateSourceFileRequest;
import com.codedoc.sourcefile.dto.SourceFileDetailDto;
import com.codedoc.sourcefile.dto.SourceFileDto;
import com.codedoc.sourcefile.dto.UpdateSourceFileRequest;
import com.codedoc.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceFileService {

    private final SourceFileRepository sourceFileRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<SourceFileDto> listForProject(Long projectId, User currentUser) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this project");
        }
        return sourceFileRepository.findByProjectIdOrderByUploadedAtDesc(projectId)
                .stream()
                .map(SourceFileMapper::toDto)
                .toList();
    }

    @Transactional
    public SourceFileDetailDto create(Long projectId, CreateSourceFileRequest request, User currentUser) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You do not have access to this project"));

        if (sourceFileRepository.countByProjectId(projectId) >= 100) { // Example limit, can be adjusted
             // No specific limit mentioned in prompt, but good practice. 
             // Let's stick to prompt requirements.
        }

        // Check for duplicate filename in project
        sourceFileRepository.findByProjectIdOrderByUploadedAtDesc(projectId).stream()
            .filter(f -> f.getFilename().equals(request.filename()))
            .findFirst()
            .ifPresent(f -> {
                throw new ConflictException("File with name " + request.filename() + " already exists in this project");
            });

        SourceFile file = new SourceFile();
        file.setProject(project);
        file.setFilename(request.filename());
        file.setContent(request.content());
        file.setLanguage(request.language());
        file.setSizeBytes((long) request.content().getBytes(StandardCharsets.UTF_8).length);
        file.setChecksum(calculateChecksum(request.content()));

        return SourceFileMapper.toDetailDto(sourceFileRepository.save(file));
    }

    @Transactional(readOnly = true)
    public SourceFileDetailDto get(Long id, User currentUser) {
        SourceFile file = sourceFileRepository.findByIdAndProjectOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Source file not found"));
        return SourceFileMapper.toDetailDto(file);
    }

    @Transactional
    public SourceFileDetailDto update(Long id, UpdateSourceFileRequest request, User currentUser) {
        SourceFile file = sourceFileRepository.findByIdAndProjectOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Source file not found"));

        if (request.filename() != null && !request.filename().equals(file.getFilename())) {
            // Check for duplicate filename if name is changing
            boolean duplicate = sourceFileRepository.findByProjectIdOrderByUploadedAtDesc(file.getProject().getId()).stream()
                .anyMatch(f -> f.getFilename().equals(request.filename()) && !f.getId().equals(id));
            if (duplicate) {
                throw new ConflictException("File with name " + request.filename() + " already exists in this project");
            }
            file.setFilename(request.filename());
        }

        if (request.content() != null) {
            file.setContent(request.content());
            file.setSizeBytes((long) request.content().getBytes(StandardCharsets.UTF_8).length);
            file.setChecksum(calculateChecksum(request.content()));
        }

        if (request.language() != null) {
            file.setLanguage(request.language());
        }

        return SourceFileMapper.toDetailDto(sourceFileRepository.save(file));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        SourceFile file = sourceFileRepository.findByIdAndProjectOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Source file not found"));
        sourceFileRepository.delete(file);
    }

    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
