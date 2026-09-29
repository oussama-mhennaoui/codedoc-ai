package com.codedoc.ai;

public class AiSchemaException extends AiException {

    public AiSchemaException(String message) {
        super("invalid_schema", message);
    }

    public AiSchemaException(String message, Throwable cause) {
        super("invalid_schema", message, cause);
    }
}
