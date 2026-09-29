package com.codedoc.ai;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class Anonymizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[\\s\\-.]?)?"
                    + "(?:\\(?\\d{2,4}\\)?[\\s\\-.]?)?"
                    + "\\d{3,4}[\\s\\-.]?\\d{3,4}"
    );

    private static final Pattern API_KEY_GENERAL_PATTERN = Pattern.compile(
            "(?i)(?:api[_-]?key|apikey|secret[_-]?key|secret|token|access[_-]?key|client[_-]?secret)"
                    + "\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-\\.]{16,})[\"']?"
    );

    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[A-Za-z0-9_\\-\\.]{20,}"
    );

    private static final Pattern SK_KEY_PATTERN = Pattern.compile(
            "(?:sk|pk|rk)-[A-Za-z0-9]{20,}"
    );

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9_\\-]+\\.eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+"
    );

    public String clean(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL]");
        result = SK_KEY_PATTERN.matcher(result).replaceAll("[REDACTED]");
        result = JWT_PATTERN.matcher(result).replaceAll("[REDACTED]");
        result = BEARER_TOKEN_PATTERN.matcher(result).replaceAll("[REDACTED]");
        result = API_KEY_GENERAL_PATTERN.matcher(result).replaceAll(
                matcher -> matcher.group().replaceFirst(
                        Pattern.quote(matcher.group(1)), "[REDACTED]"
                )
        );
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE]");

        return result;
    }
}
