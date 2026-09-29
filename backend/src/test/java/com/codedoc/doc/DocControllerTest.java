package com.codedoc.doc;

import com.codedoc.doc.dto.*;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.security.JwtService;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.Role;
import com.codedoc.user.User;
import com.codedoc.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SourceFileRepository sourceFileRepository;

    @Autowired
    private GeneratedDocRepository docRepository;

    @Autowired
    private DocSectionRepository sectionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private GeneratedDoc docA;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        sectionRepository.deleteAll();
        docRepository.deleteAll();
        sourceFileRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setUsername("userA");
        userA.setEmail("userA@example.com");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setRole(Role.USER);
        userA = userRepository.save(userA);
        tokenA = "Bearer " + jwtService.generateToken(userA);

        userB = new User();
        userB.setUsername("userB");
        userB.setEmail("userB@example.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);
        tokenB = "Bearer " + jwtService.generateToken(userB);

        Project projectA = new Project();
        projectA.setName("Project A");
        projectA.setOwner(userA);
        projectA = projectRepository.save(projectA);

        SourceFile fileA = new SourceFile();
        fileA.setFilename("FileA.java");
        fileA.setContent("content");
        fileA.setProject(projectA);
        fileA = sourceFileRepository.save(fileA);

        docA = new GeneratedDoc();
        docA.setSourceFile(fileA);
        docA.setStatus(DocStatus.SUCCESS);
        docA.setModelUsed("gpt-4");
        docA = docRepository.save(docA);
    }

    @Test
    void getDoc_ShouldReturn200_WhenOwner() throws Exception {
        mockMvc.perform(get("/api/docs/" + docA.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docA.getId()));
    }

    @Test
    void getDoc_ShouldReturn403_WhenNotOwner() throws Exception {
        mockMvc.perform(get("/api/docs/" + docA.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDoc_ShouldReturn200_WhenOwner() throws Exception {
        UpdateDocRequest request = new UpdateDocRequest(DocStatus.FAILED, "error");
        mockMvc.perform(patch("/api/docs/" + docA.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("error"));
    }

    @Test
    void deleteDoc_ShouldReturn204_WhenOwner() throws Exception {
        mockMvc.perform(delete("/api/docs/" + docA.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isNoContent());
    }

    @Test
    void addSection_ShouldReturn201_WhenOwner() throws Exception {
        CreateSectionRequest request = new CreateSectionRequest("Title", "Content", 1, SectionType.SUMMARY);
        mockMvc.perform(post("/api/docs/" + docA.getId() + "/sections")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void updateSection_ShouldReturn200_WhenOwner() throws Exception {
        DocSection section = new DocSection();
        section.setGeneratedDoc(docA);
        section.setTitle("Old Title");
        section.setContent("Old Content");
        section.setOrderIndex(0);
        section.setType(SectionType.SUMMARY);
        section = sectionRepository.save(section);

        UpdateSectionRequest request = new UpdateSectionRequest("New Title", null, null, null);
        mockMvc.perform(patch("/api/sections/" + section.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void deleteSection_ShouldReturn204_WhenOwner() throws Exception {
        DocSection section = new DocSection();
        section.setGeneratedDoc(docA);
        section.setTitle("Title");
        section.setContent("Content");
        section.setOrderIndex(0);
        section.setType(SectionType.SUMMARY);
        section = sectionRepository.save(section);

        mockMvc.perform(delete("/api/sections/" + section.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isNoContent());
    }

    @Test
    void reorderSections_ShouldReturn204_WhenOwner() throws Exception {
        DocSection s1 = new DocSection();
        s1.setGeneratedDoc(docA);
        s1.setTitle("S1");
        s1.setContent("C1");
        s1.setOrderIndex(0);
        s1.setType(SectionType.SUMMARY);
        s1 = sectionRepository.save(s1);

        DocSection s2 = new DocSection();
        s2.setGeneratedDoc(docA);
        s2.setTitle("S2");
        s2.setContent("C2");
        s2.setOrderIndex(1);
        s2.setType(SectionType.SUMMARY);
        s2 = sectionRepository.save(s2);

        mockMvc.perform(post("/api/docs/" + docA.getId() + "/sections/reorder")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(s2.getId(), s1.getId()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void addComment_ShouldReturn201_WhenOwner() throws Exception {
        DocSection section = new DocSection();
        section.setGeneratedDoc(docA);
        section.setTitle("Title");
        section.setContent("Content");
        section.setOrderIndex(0);
        section.setType(SectionType.SUMMARY);
        section = sectionRepository.save(section);

        CreateCommentRequest request = new CreateCommentRequest("Comment Content");
        mockMvc.perform(post("/api/sections/" + section.getId() + "/comments")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Comment Content"))
                .andExpect(jsonPath("$.authorName").value("userA"));
    }

    @Test
    void getComments_ShouldReturn200_WhenOwner() throws Exception {
        DocSection section = new DocSection();
        section.setGeneratedDoc(docA);
        section.setTitle("Title");
        section.setContent("Content");
        section.setOrderIndex(0);
        section.setType(SectionType.SUMMARY);
        section = sectionRepository.save(section);

        mockMvc.perform(get("/api/sections/" + section.getId() + "/comments")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk());
    }
}
