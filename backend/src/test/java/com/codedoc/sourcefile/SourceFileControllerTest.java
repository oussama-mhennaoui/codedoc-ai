package com.codedoc.sourcefile;

import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.security.JwtService;
import com.codedoc.sourcefile.dto.CreateSourceFileRequest;
import com.codedoc.sourcefile.dto.UpdateSourceFileRequest;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SourceFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SourceFileRepository sourceFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private Project projectA;
    private Project projectB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
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

        projectA = new Project();
        projectA.setName("Project A");
        projectA.setOwner(userA);
        projectA = projectRepository.save(projectA);

        projectB = new Project();
        projectB.setName("Project B");
        projectB.setOwner(userB);
        projectB = projectRepository.save(projectB);
    }

    @Test
    void shouldCreateSourceFile() throws Exception {
        String content = "public class Hello {}";
        CreateSourceFileRequest request = new CreateSourceFileRequest("Hello.java", content, "Java");

        mockMvc.perform(post("/api/projects/" + projectA.getId() + "/files")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("Hello.java"))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.sizeBytes").value(content.getBytes(StandardCharsets.UTF_8).length))
                .andExpect(jsonPath("$.checksum").value(calculateChecksum(content)));
    }

    @Test
    void shouldFailOnDuplicateFilename() throws Exception {
        SourceFile file = new SourceFile();
        file.setFilename("Hello.java");
        file.setContent("Content 1");
        file.setProject(projectA);
        sourceFileRepository.save(file);

        CreateSourceFileRequest request = new CreateSourceFileRequest("Hello.java", "Content 2", "Java");

        mockMvc.perform(post("/api/projects/" + projectA.getId() + "/files")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("File with name Hello.java already exists in this project"));
    }

    @Test
    void shouldListFilesForProject() throws Exception {
        SourceFile file = new SourceFile();
        file.setFilename("Hello.java");
        file.setContent("Content");
        file.setProject(projectA);
        sourceFileRepository.save(file);

        mockMvc.perform(get("/api/projects/" + projectA.getId() + "/files")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].filename").value("Hello.java"));
    }

    @Test
    void shouldGetSourceFileDetail() throws Exception {
        SourceFile file = new SourceFile();
        file.setFilename("Hello.java");
        file.setContent("Content");
        file.setProject(projectA);
        file = sourceFileRepository.save(file);

        mockMvc.perform(get("/api/files/" + file.getId())
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("Hello.java"))
                .andExpect(jsonPath("$.content").value("Content"));
    }

    @Test
    void shouldUpdateSourceFile() throws Exception {
        SourceFile file = new SourceFile();
        file.setFilename("Hello.java");
        file.setContent("Content");
        file.setProject(projectA);
        file = sourceFileRepository.save(file);

        String newContent = "Updated Content";
        UpdateSourceFileRequest request = new UpdateSourceFileRequest("Updated.java", newContent, "Java");

        mockMvc.perform(patch("/api/files/" + file.getId())
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("Updated.java"))
                .andExpect(jsonPath("$.content").value(newContent))
                .andExpect(jsonPath("$.checksum").value(calculateChecksum(newContent)));
    }

    @Test
    void shouldDeleteSourceFile() throws Exception {
        SourceFile file = new SourceFile();
        file.setFilename("Hello.java");
        file.setContent("Content");
        file.setProject(projectA);
        file = sourceFileRepository.save(file);

        mockMvc.perform(delete("/api/files/" + file.getId())
                .header("Authorization", tokenA))
                .andExpect(status().isNoContent());

        assert(!sourceFileRepository.existsById(file.getId()));
    }

    @Test
    void shouldNotAccessOtherUsersFile() throws Exception {
        SourceFile fileB = new SourceFile();
        fileB.setFilename("Secret.java");
        fileB.setContent("Secret Content");
        fileB.setProject(projectB);
        fileB = sourceFileRepository.save(fileB);

        // Try to get B's file with A's token
        mockMvc.perform(get("/api/files/" + fileB.getId())
                .header("Authorization", tokenA))
                .andExpect(status().isNotFound());
        
        // Try to list B's project files with A's token
        mockMvc.perform(get("/api/projects/" + projectB.getId() + "/files")
                .header("Authorization", tokenA))
                .andExpect(status().isForbidden());
    }

    private String calculateChecksum(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
