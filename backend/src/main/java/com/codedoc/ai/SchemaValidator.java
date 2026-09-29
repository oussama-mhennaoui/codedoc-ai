package com.codedoc.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SchemaValidator {

    private static final String SCHEMAS_DIR = "schemas/";

    private final ConcurrentHashMap<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidator() {
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    public void validate(JsonNode jsonNode, String schemaName) {
        JsonSchema schema = schemaCache.computeIfAbsent(schemaName, this::loadSchema);
        Set<ValidationMessage> errors = schema.validate(jsonNode);

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Schema validation failed for '");
            sb.append(schemaName).append("': ");
            boolean first = true;
            for (ValidationMessage error : errors) {
                if (!first) {
                    sb.append("; ");
                }
                sb.append(error.getMessage());
                first = false;
            }
            log.warn("Schema validation errors for schema '{}': {}", schemaName, errors);
            throw new AiSchemaException(sb.toString());
        }
    }

    private JsonSchema loadSchema(String schemaName) {
        String resourcePath = SCHEMAS_DIR + schemaName + ".json";
        log.debug("Loading schema from classpath: {}", resourcePath);

        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new AiException("missing_schema", "Schema not found: " + resourcePath);
        }

        try (InputStream is = resource.getInputStream()) {
            JsonSchema schema = schemaFactory.getSchema(is);
            log.debug("Loaded schema '{}'", schemaName);
            return schema;
        } catch (com.networknt.schema.JsonSchemaException | IOException e) {
            throw new AiException("schema_load_error",
                    "Failed to read or parse schema: " + resourcePath, e);
        }
    }

    public String getRawSchema(String schemaName) {
        String resourcePath = SCHEMAS_DIR + schemaName + ".json";
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new AiException("missing_schema", "Schema not found: " + resourcePath);
        }
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AiException("schema_load_error", "Failed to read schema: " + resourcePath, e);
        }
    }

    public void invalidateCache(String schemaName) {
        schemaCache.remove(schemaName);
    }

    public void invalidateAll() {
        schemaCache.clear();
    }
}
