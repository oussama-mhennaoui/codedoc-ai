package com.codedoc.sourcefile;

import com.codedoc.sourcefile.dto.*;
import com.codedoc.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SourceFileController {

    private final SourceFileService sourceFileService;

    @GetMapping("/api/projects/{projectId}/files")
    public List<SourceFileDto> listFiles(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        return sourceFileService.listForProject(projectId, currentUser);
    }

    @PostMapping("/api/projects/{projectId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public SourceFileDetailDto createFile(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSourceFileRequest request,
            @AuthenticationPrincipal User currentUser) {
        return sourceFileService.create(projectId, request, currentUser);
    }

    @GetMapping("/api/files/{id}")
    public SourceFileDetailDto getFile(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return sourceFileService.get(id, currentUser);
    }

    @PatchMapping("/api/files/{id}")
    public SourceFileDetailDto updateFile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSourceFileRequest request,
            @AuthenticationPrincipal User currentUser) {
        return sourceFileService.update(id, request, currentUser);
    }

    @DeleteMapping("/api/files/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        sourceFileService.delete(id, currentUser);
    }
}
