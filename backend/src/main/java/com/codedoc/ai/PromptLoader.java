package com.codedoc.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PromptLoader {

    private static final String PROMPTS_DIR = "prompts/";

    private final ConcurrentHashMap<String, String> promptCache = new ConcurrentHashMap<>();

    @Autowired
    public PromptLoader() {
    }

    public String load(String promptName) {
        return promptCache.computeIfAbsent(promptName, this::readPromptFromClasspath);
    }

    private String readPromptFromClasspath(String promptName) {
        String resourcePath = PROMPTS_DIR + promptName + ".txt";
        log.debug("Loading prompt from classpath: {}", resourcePath);

        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new AiException("missing_prompt", "Prompt not found: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            log.debug("Loaded prompt '{}' ({} chars)", promptName, content.length());
            return content;
        } catch (IOException e) {
            throw new AiException("prompt_load_error",
                    "Failed to read prompt: " + resourcePath, e);
        }
    }

    public void invalidateCache(String promptName) {
        promptCache.remove(promptName);
    }

    public void invalidateAll() {
        promptCache.clear();
    }
}
