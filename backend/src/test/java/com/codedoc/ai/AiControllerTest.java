package com.codedoc.ai;

import com.codedoc.ai.dto.GenerateDocRequest;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.security.JwtService;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.Role;
import com.codedoc.user.User;
import com.codedoc.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LlmClient llmClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SourceFileRepository sourceFileRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String token;
    private SourceFile testFile;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        projectRepository.deleteAll();
        sourceFileRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);

        token = "Bearer " + jwtService.generateToken(testUser);

        Project project = new Project();
        project.setName("Test Project");
        project.setOwner(testUser);
        project = projectRepository.save(project);

        testFile = new SourceFile();
        testFile.setFilename("Test.java");
        testFile.setContent("public class Test {}");
        testFile.setLanguage("java");
        testFile.setProject(project);
        testFile = sourceFileRepository.save(testFile);
    }

    @Test
    void generateDoc_ShouldReturn200_WhenSuccessful() throws Exception {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("overview", "Test overview");
        data.put("architecture", "Test arch");
        data.putArray("components");
        data.putArray("publicApi");
        data.putArray("dependencies");
        data.putArray("usageExamples");
        data.putArray("edgeCasesAndLimitations");
        data.putArray("performanceConsiderations");

        LlmResponse llmResponse = new LlmResponse(data, 100L, 50);
        when(llmClient.call(anyString())).thenReturn(llmResponse);

        GenerateDocRequest request = new GenerateDocRequest();
        request.setFileId(testFile.getId());

        mockMvc.perform(post("/api/ai/generate-doc")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.overview").value("Test overview"));
    }

    @Test
    void generateDoc_ShouldReturn429_WhenQuotaExceeded() throws Exception {
        when(llmClient.call(anyString())).thenThrow(new AiException("quota", "Quota exceeded"));

        GenerateDocRequest request = new GenerateDocRequest();
        request.setFileId(testFile.getId());

        mockMvc.perform(post("/api/ai/generate-doc")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("quota"));
    }

    @Test
    void generateDoc_ShouldReturn500_WhenApiKeyMissing() throws Exception {
        when(llmClient.call(anyString())).thenThrow(new AiException("missing_api_key", "API key missing"));

        GenerateDocRequest request = new GenerateDocRequest();
        request.setFileId(testFile.getId());

        mockMvc.perform(post("/api/ai/generate-doc")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("missing_api_key"));
    }

    @Test
    void generateDoc_ShouldReturn502_WhenInvalidSchema() throws Exception {
        // LlmClient returns data that fails validation (missing required fields)
        ObjectNode invalidData = objectMapper.createObjectNode();
        invalidData.put("invalid", "data");

        LlmResponse llmResponse = new LlmResponse(invalidData, 100L, 50);
        when(llmClient.call(anyString())).thenReturn(llmResponse);

        GenerateDocRequest request = new GenerateDocRequest();
        request.setFileId(testFile.getId());

        mockMvc.perform(post("/api/ai/generate-doc")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("invalid_schema"));
    }

    @Test
    void generateDoc_ShouldReturn401_WhenUnauthenticated() throws Exception {
        GenerateDocRequest request = new GenerateDocRequest();
        request.setFileId(testFile.getId());

        mockMvc.perform(post("/api/ai/generate-doc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
