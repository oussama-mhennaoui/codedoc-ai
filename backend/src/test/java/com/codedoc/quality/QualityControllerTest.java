package com.codedoc.quality;

import com.codedoc.ai.LlmClient;
import com.codedoc.ai.LlmResponse;
import com.codedoc.project.Project;
import com.codedoc.project.ProjectRepository;
import com.codedoc.quality.dto.QualityAnalysisRequest;
import com.codedoc.security.JwtService;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.Role;
import com.codedoc.user.User;
import com.codedoc.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QualityControllerTest {

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
    private QualityReportRepository qualityReportRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private LlmClient llmClient;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private SourceFile fileA;
    private Project projectA;

    @BeforeEach
    void setUp() throws Exception {
        qualityReportRepository.deleteAll();
        sourceFileRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setUsername("sirine");
        userA.setEmail("sirine@codedoc.test");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setRole(Role.USER);
        userA = userRepository.save(userA);
        tokenA = "Bearer " + jwtService.generateToken(userA);

        userB = new User();
        userB.setUsername("other");
        userB.setEmail("other@codedoc.test");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);
        tokenB = "Bearer " + jwtService.generateToken(userB);

        projectA = new Project();
        projectA.setName("Demo Quality Project");
        projectA.setOwner(userA);
        projectA = projectRepository.save(projectA);

        fileA = new SourceFile();
        fileA.setProject(projectA);
        fileA.setFilename("UserService.java");
        fileA.setLanguage("java");
        fileA.setContent("public class UserService { public void save() { System.out.println(\"saving\"); } }");
        fileA = sourceFileRepository.save(fileA);

        // Mock LLM Response strictly conforming to analyze_quality_schema.json
        String mockLlmJson = """
        {
          "score_global": 82,
          "respect_solid": {
            "srp": "Bonne séparation des responsabilités",
            "ocp": "Extensible via interfaces",
            "lsp": "Respecté",
            "isp": "Interfaces fines",
            "dip": "Dépendances injectées",
            "resume": "Architecture globalement propre"
          },
          "duplication": {
            "pourcentage_estime": 5,
            "details": "Très peu de duplication constatée"
          },
          "complexite": {
            "score_cyclomatique": 3,
            "score_cognitif": 2,
            "details": "Complexité faible, flux linéaire"
          },
          "dette_technique": {
            "estimation_minutes": 45,
            "niveau": "faible",
            "resume": "Quelques refactorings mineurs recommandés"
          },
          "code_smells": [
            {
              "type": "Magic Number",
              "fichier": "UserService.java",
              "ligne": 12,
              "gravite": "faible",
              "description": "Valeur en dur non extraite en constante",
              "suggestion_refactoring": "Extraire dans private static final int TIMEOUT_SECONDS"
            },
            {
              "type": "Long Method",
              "fichier": "UserService.java",
              "ligne": 35,
              "gravite": "critique",
              "description": "Méthode de 65 lignes avec logique métier et mapping",
              "suggestion_refactoring": "Déléguer le mapping à un UserMapper"
            }
          ]
        }
        """;
        JsonNode mockNode = objectMapper.readTree(mockLlmJson);
        when(llmClient.call(anyString())).thenReturn(new LlmResponse(mockNode, 150, 420));
    }

    @Test
    void analyzeQuality_returnsCreatedAndCorrectSchema() throws Exception {
        QualityAnalysisRequest request = new QualityAnalysisRequest(fileA.getId());

        mockMvc.perform(post("/api/quality/analyze")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreGlobal").value(82))
                .andExpect(jsonPath("$.technicalDebtRating").value("faible"))
                .andExpect(jsonPath("$.technicalDebtMinutes").value(45))
                .andExpect(jsonPath("$.codeSmells.length()").value(2))
                .andExpect(jsonPath("$.codeSmells[0].gravite").value("critique"))
                .andExpect(jsonPath("$.codeSmells[1].gravite").value("faible"));
    }

    @Test
    void getReportByFile_returnsExistingReport() throws Exception {
        // Run analysis first
        QualityAnalysisRequest request = new QualityAnalysisRequest(fileA.getId());
        mockMvc.perform(post("/api/quality/analyze")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get report
        mockMvc.perform(get("/api/quality/files/" + fileA.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(82))
                .andExpect(jsonPath("$.filename").value("UserService.java"));
    }

    @Test
    void getProjectSummary_returnsAggregatedMetrics() throws Exception {
        // Run analysis
        QualityAnalysisRequest request = new QualityAnalysisRequest(fileA.getId());
        mockMvc.perform(post("/api/quality/analyze")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Check project summary
        mockMvc.perform(get("/api/quality/projects/" + projectA.getId() + "/summary")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(82.0))
                .andExpect(jsonPath("$.totalTechnicalDebtMinutes").value(45))
                .andExpect(jsonPath("$.totalCodeSmells").value(2))
                .andExpect(jsonPath("$.totalCriticalSmells").value(1))
                .andExpect(jsonPath("$.fileSummaries.length()").value(1));
    }

    @Test
    void exportReport_markdownAndPdf() throws Exception {
        // Analyze first
        QualityAnalysisRequest request = new QualityAnalysisRequest(fileA.getId());
        String responseContent = mockMvc.perform(post("/api/quality/analyze")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseContent);
        long reportId = jsonNode.get("id").asLong();

        // Export Markdown
        mockMvc.perform(get("/api/quality/reports/" + reportId + "/export?format=md")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/markdown")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rapport d'Analyse")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UserService.java")));

        // Export PDF
        mockMvc.perform(get("/api/quality/reports/" + reportId + "/export?format=pdf")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void forbidden_whenAnotherUserAccessesReport() throws Exception {
        QualityAnalysisRequest request = new QualityAnalysisRequest(fileA.getId());

        mockMvc.perform(post("/api/quality/analyze")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
