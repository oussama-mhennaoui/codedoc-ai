package com.codedoc.project;

import com.codedoc.security.JwtService;
import com.codedoc.user.Role;
import com.codedoc.user.User;
import com.codedoc.user.UserRepository;
import com.codedoc.project.dto.CreateProjectRequest;
import com.codedoc.project.dto.UpdateProjectRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void shouldCreateProject() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Project 1", "Description 1", "Java");

        mockMvc.perform(post("/api/projects")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Project 1"))
                .andExpect(jsonPath("$.description").value("Description 1"))
                .andExpect(jsonPath("$.language").value("Java"))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void shouldListProjects() throws Exception {
        Project project = new Project();
        project.setName("Project A");
        project.setOwner(userA);
        projectRepository.save(project);

        mockMvc.perform(get("/api/projects")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Project A"));
    }

    @Test
    void shouldGetProject() throws Exception {
        Project project = new Project();
        project.setName("Project A");
        project.setOwner(userA);
        project = projectRepository.save(project);

        mockMvc.perform(get("/api/projects/" + project.getId())
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project A"));
    }

    @Test
    void shouldUpdateProject() throws Exception {
        Project project = new Project();
        project.setName("Project A");
        project.setOwner(userA);
        project = projectRepository.save(project);

        UpdateProjectRequest request = new UpdateProjectRequest("Updated Name", null, null);

        mockMvc.perform(patch("/api/projects/" + project.getId())
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void shouldDeleteProject() throws Exception {
        Project project = new Project();
        project.setName("Project A");
        project.setOwner(userA);
        project = projectRepository.save(project);

        mockMvc.perform(delete("/api/projects/" + project.getId())
                .header("Authorization", tokenA))
                .andExpect(status().isNoContent());

        assert(!projectRepository.existsById(project.getId()));
    }

    @Test
    void shouldArchiveProject() throws Exception {
        Project project = new Project();
        project.setName("Project A");
        project.setOwner(userA);
        project.setArchived(false);
        project = projectRepository.save(project);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/archive")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void shouldNotAccessOtherUsersProject() throws Exception {
        Project projectB = new Project();
        projectB.setName("Project B");
        projectB.setOwner(userB);
        projectB = projectRepository.save(projectB);

        mockMvc.perform(get("/api/projects/" + projectB.getId())
                .header("Authorization", tokenA))
                .andExpect(status().isNotFound());
    }
}
