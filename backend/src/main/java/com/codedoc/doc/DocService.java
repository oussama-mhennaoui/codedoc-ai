package com.codedoc.doc;

import com.codedoc.common.ForbiddenException;
import com.codedoc.common.NotFoundException;
import com.codedoc.doc.dto.*;
import com.codedoc.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocService {

    private final GeneratedDocRepository docRepository;
    private final DocSectionRepository sectionRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<GeneratedDocDto> listByProject(Long projectId, User user) {
        // Verify the user owns the project by trying to load any doc; if none exist, return empty
        return docRepository.findBySourceFileProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .filter(doc -> doc.getSourceFile().getProject().getOwner().getId().equals(user.getId()))
                .map(DocMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GeneratedDocDto getDoc(Long id, User user) {
        GeneratedDoc doc = getDocEntity(id, user);
        return DocMapper.toDto(doc);
    }

    @Transactional
    public GeneratedDocDto updateDoc(Long id, UpdateDocRequest request, User user) {
        GeneratedDoc doc = getDocEntity(id, user);
        if (request.status() != null) {
            doc.setStatus(request.status());
        }
        if (request.errorMessage() != null) {
            doc.setErrorMessage(request.errorMessage());
        }
        return DocMapper.toDto(docRepository.save(doc));
    }

    @Transactional
    public void deleteDoc(Long id, User user) {
        GeneratedDoc doc = getDocEntity(id, user);
        docRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public List<DocSectionDto> getSections(Long docId, User user) {
        GeneratedDoc doc = getDocEntity(docId, user);
        return doc.getSections().stream()
                .map(DocMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocSectionDto addSection(Long docId, CreateSectionRequest request, User user) {
        GeneratedDoc doc = getDocEntity(docId, user);
        
        DocSection section = new DocSection();
        section.setGeneratedDoc(doc);
        section.setTitle(request.title());
        section.setContent(request.content());
        section.setOrderIndex(request.orderIndex());
        section.setType(request.type());
        
        return DocMapper.toDto(sectionRepository.save(section));
    }

    @Transactional
    public DocSectionDto updateSection(Long sectionId, UpdateSectionRequest request, User user) {
        DocSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        
        getDocEntity(section.getGeneratedDoc().getId(), user);

        if (request.title() != null) {
            section.setTitle(request.title());
        }
        if (request.content() != null) {
            section.setContent(request.content());
        }
        if (request.orderIndex() != null) {
            section.setOrderIndex(request.orderIndex());
        }
        if (request.type() != null) {
            section.setType(request.type());
        }

        return DocMapper.toDto(sectionRepository.save(section));
    }

    @Transactional
    public void deleteSection(Long sectionId, User user) {
        DocSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        
        getDocEntity(section.getGeneratedDoc().getId(), user);
        sectionRepository.delete(section);
    }

    @Transactional
    public void reorderSections(Long docId, List<Long> sectionIds, User user) {
        getDocEntity(docId, user);
        
        List<DocSection> sections = sectionRepository.findAllById(sectionIds);
        Map<Long, DocSection> sectionMap = sections.stream()
                .collect(Collectors.toMap(DocSection::getId, Function.identity()));

        if (sectionIds.size() != sections.size() || !sectionMap.keySet().containsAll(sectionIds)) {
            throw new IllegalArgumentException("Invalid section IDs for reordering");
        }

        // Validate all sections belong to the document
        for (DocSection section : sections) {
            if (!section.getGeneratedDoc().getId().equals(docId)) {
                throw new IllegalArgumentException("Section " + section.getId() + " does not belong to document " + docId);
            }
        }

        for (int i = 0; i < sectionIds.size(); i++) {
            DocSection section = sectionMap.get(sectionIds.get(i));
            section.setOrderIndex(i);
        }
        
        sectionRepository.saveAll(sections);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(Long sectionId, User user) {
        DocSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        
        getDocEntity(section.getGeneratedDoc().getId(), user);
        
        return section.getComments().stream()
                .map(DocMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto addComment(Long sectionId, CreateCommentRequest request, User user) {
        DocSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        
        getDocEntity(section.getGeneratedDoc().getId(), user);

        Comment comment = new Comment();
        comment.setDocSection(section);
        comment.setAuthor(user);
        comment.setContent(request.content());
        
        return DocMapper.toDto(commentRepository.save(comment));
    }

    public GeneratedDoc getDocEntity(Long docId, User user) {
        GeneratedDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        
        // Ownership check: GeneratedDoc -> SourceFile -> Project -> Owner
        if (!doc.getSourceFile().getProject().getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to this document");
        }
        return doc;
    }
}
