package com.codedoc.testgen;

import com.codedoc.ai.*;
import com.codedoc.ai.dto.GenerateTestsRequest;
import com.codedoc.common.NotFoundException;
import com.codedoc.project.Project;
import com.codedoc.sourcefile.SourceFile;
import com.codedoc.sourcefile.SourceFileRepository;
import com.codedoc.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestGenerationServiceTest {

    @Mock private PromptLoader promptLoader;
    @Mock private SchemaValidator schemaValidator;
    @Mock private LlmClient llmClient;
    @Mock private Anonymizer anonymizer;
    @Mock private AILogRepository aiLogRepository;
    @Mock private SourceFileRepository sourceFileRepository;
    @Mock private TestGenerationRepository testGenerationRepository;

    @InjectMocks
    private TestGenerationService service;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void generateTests_withSourceCode_shouldSucceed() throws Exception {
        // Arrange
        GenerateTestsRequest request = new GenerateTestsRequest();
        request.setSourceCode("public class Hello { public String greet() { return \"Hello\"; } }");
        request.setLanguage("java");

        String validResponse = """
            {
              "language": "java",
              "testFramework": "JUnit 5",
              "testFiles": [{
                "filename": "HelloTest.java",
                "content": "import org.junit.jupiter.api.Test;",
                "tests": [{"name": "testGreet", "description": "Tests greet method", "category": "happy_path"}]
              }],
              "summary": "Tests for Hello class"
            }
            """;
        JsonNode responseData = objectMapper.readTree(validResponse);

        when(promptLoader.load("generate_tests_v1")).thenReturn("prompt {{language}} {{testFramework}} {{fileName}} {{sourceCode}}");
        when(schemaValidator.getRawSchema("generate_tests_schema")).thenReturn("{}");
        when(anonymizer.clean(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(llmClient.call(anyString())).thenReturn(new LlmResponse(responseData, 1000L, 500));
        when(testGenerationRepository.save(any(TestGeneration.class))).thenAnswer(inv -> {
            TestGeneration g = inv.getArgument(0);
            if (g.getId() == null) g.setId(1L);
            return g;
        });

        // Act
        TestGenerationDto result = service.generateTests(request, testUser);

        // Assert
        assertNotNull(result);
        assertEquals("java", result.language());
        assertEquals("junit5", result.testFramework());
        assertEquals("SUCCESS", result.status());
        assertNotNull(result.result());
        verify(llmClient, times(1)).call(anyString());
        verify(testGenerationRepository, times(2)).save(any(TestGeneration.class)); // once PENDING, once SUCCESS
    }

    @Test
    void generateTests_withFileId_shouldSucceed() throws Exception {
        // Arrange
        Project project = new Project();
        project.setId(10L);
        project.setOwner(testUser);
        project.setName("TestProject");

        SourceFile file = new SourceFile();
        file.setId(5L);
        file.setProject(project);
        file.setFilename("Calculator.java");
        file.setLanguage("java");
        file.setContent("public class Calculator { public int add(int a, int b) { return a + b; } }");

        GenerateTestsRequest request = new GenerateTestsRequest();
        request.setFileId(5L);

        String validResponse = """
            {
              "language": "java",
              "testFramework": "JUnit 5",
              "testFiles": [{
                "filename": "CalculatorTest.java",
                "content": "import org.junit.jupiter.api.Test;",
                "tests": [{"name": "testAdd", "description": "Tests add method", "category": "happy_path"}]
              }],
              "summary": "Tests for Calculator class"
            }
            """;
        JsonNode responseData = objectMapper.readTree(validResponse);

        when(sourceFileRepository.findById(5L)).thenReturn(Optional.of(file));
        when(promptLoader.load("generate_tests_v1")).thenReturn("prompt {{language}} {{testFramework}} {{fileName}} {{sourceCode}}");
        when(schemaValidator.getRawSchema("generate_tests_schema")).thenReturn("{}");
        when(anonymizer.clean(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(llmClient.call(anyString())).thenReturn(new LlmResponse(responseData, 1000L, 500));
        when(testGenerationRepository.save(any(TestGeneration.class))).thenAnswer(inv -> {
            TestGeneration g = inv.getArgument(0);
            if (g.getId() == null) g.setId(1L);
            return g;
        });

        // Act
        TestGenerationDto result = service.generateTests(request, testUser);

        // Assert
        assertNotNull(result);
        assertEquals("java", result.language());
        assertEquals("SUCCESS", result.status());
        assertEquals(5L, result.sourceFileId());
    }

    @Test
    void generateTests_noSourceCodeNoFile_shouldThrow() {
        GenerateTestsRequest request = new GenerateTestsRequest();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateTests(request, testUser));
    }

    @Test
    void generateTests_emptySourceCode_shouldThrow() {
        GenerateTestsRequest request = new GenerateTestsRequest();
        request.setSourceCode("   ");

        assertThrows(IllegalArgumentException.class,
                () -> service.generateTests(request, testUser));
    }

    @Test
    void generateTests_fileNotFound_shouldThrow() {
        GenerateTestsRequest request = new GenerateTestsRequest();
        request.setFileId(999L);

        when(sourceFileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.generateTests(request, testUser));
    }

    @Test
    void generateTests_aiError_shouldMarkFailed() throws Exception {
        GenerateTestsRequest request = new GenerateTestsRequest();
        request.setSourceCode("some code");
        request.setLanguage("java");

        when(promptLoader.load("generate_tests_v1")).thenReturn("prompt {{language}} {{testFramework}} {{fileName}} {{sourceCode}}");
        when(schemaValidator.getRawSchema("generate_tests_schema")).thenReturn("{}");
        when(anonymizer.clean(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(llmClient.call(anyString())).thenThrow(new AiException("quota", "Rate limited"));
        when(testGenerationRepository.save(any(TestGeneration.class))).thenAnswer(inv -> {
            TestGeneration g = inv.getArgument(0);
            if (g.getId() == null) g.setId(1L);
            return g;
        });

        assertThrows(AiException.class,
                () -> service.generateTests(request, testUser));

        // Verify the generation was saved as FAILED
        verify(testGenerationRepository, atLeast(2)).save(argThat(gen ->
                gen.getStatus() == TestGenStatus.FAILED || gen.getStatus() == TestGenStatus.PENDING
        ));
    }

    @Test
    void generateTests_defaultFramework_python() throws Exception {
        GenerateTestsRequest request = new GenerateTestsRequest();
        request.setSourceCode("def hello(): return 'hello'");
        request.setLanguage("python");

        String validResponse = """
            {
              "language": "python",
              "testFramework": "pytest",
              "testFiles": [{
                "filename": "test_hello.py",
                "content": "def test_hello(): assert hello() == 'hello'",
                "tests": [{"name": "test_hello", "description": "Tests hello", "category": "happy_path"}]
              }],
              "summary": "Tests for hello"
            }
            """;
        JsonNode responseData = objectMapper.readTree(validResponse);

        when(promptLoader.load("generate_tests_v1")).thenReturn("prompt {{language}} {{testFramework}} {{fileName}} {{sourceCode}}");
        when(schemaValidator.getRawSchema("generate_tests_schema")).thenReturn("{}");
        when(anonymizer.clean(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(llmClient.call(anyString())).thenReturn(new LlmResponse(responseData, 1000L, 300));
        when(testGenerationRepository.save(any(TestGeneration.class))).thenAnswer(inv -> {
            TestGeneration g = inv.getArgument(0);
            if (g.getId() == null) g.setId(1L);
            return g;
        });

        TestGenerationDto result = service.generateTests(request, testUser);

        assertEquals("python", result.language());
        assertEquals("pytest", result.testFramework());
        assertEquals("SUCCESS", result.status());
    }
}
