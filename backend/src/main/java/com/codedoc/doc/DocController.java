package com.codedoc.doc;

import com.codedoc.doc.dto.*;
import com.codedoc.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocController {

    private final DocService docService;
    private final DocExportService docExportService;

    @GetMapping("/projects/{projectId}/docs")
    public ResponseEntity<List<GeneratedDocDto>> listDocsByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(docService.listByProject(projectId, user));
    }

    @GetMapping("/docs/{id}")
    public ResponseEntity<GeneratedDocDto> getDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(docService.getDoc(id, user));
    }

    @PatchMapping("/docs/{id}")
    public ResponseEntity<GeneratedDocDto> updateDoc(
            @PathVariable Long id,
            @RequestBody UpdateDocRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(docService.updateDoc(id, request, user));
    }

    @DeleteMapping("/docs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        docService.deleteDoc(id, user);
    }

    @GetMapping("/docs/{id}/sections")
    public ResponseEntity<List<DocSectionDto>> getSections(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(docService.getSections(id, user));
    }

    @PostMapping("/docs/{id}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<DocSectionDto> addSection(
            @PathVariable Long id,
            @Valid @RequestBody CreateSectionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(docService.addSection(id, request, user));
    }

    @PatchMapping("/sections/{id}")
    public ResponseEntity<DocSectionDto> updateSection(
            @PathVariable Long id,
            @RequestBody UpdateSectionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(docService.updateSection(id, request, user));
    }

    @DeleteMapping("/sections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        docService.deleteSection(id, user);
    }

    @PostMapping("/docs/{id}/sections/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderSections(
            @PathVariable Long id,
            @RequestBody List<Long> sectionIds,
            @AuthenticationPrincipal User user) {
        docService.reorderSections(id, sectionIds, user);
    }

    @GetMapping("/sections/{id}/comments")
    public ResponseEntity<List<CommentDto>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(docService.getComments(id, user));
    }

    @PostMapping("/sections/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(docService.addComment(id, request, user));
    }
    @GetMapping("/docs/{id}/export")
    public ResponseEntity<byte[]> exportDoc(
            @PathVariable Long id,
            @RequestParam(defaultValue = "md") String format,
            @AuthenticationPrincipal User user) {
        
        GeneratedDoc docEntity = docService.getDocEntity(id, user);
        
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdfBytes = docExportService.toPdf(docEntity);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"" + docEntity.getSourceFile().getFilename() + ".pdf\"")
                    .body(pdfBytes);
        } else {
            String markdown = docExportService.toMarkdown(docEntity);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/markdown")
                    .header("Content-Disposition", "attachment; filename=\"" + docEntity.getSourceFile().getFilename() + ".md\"")
                    .body(markdown.getBytes());
        }
    }
}
