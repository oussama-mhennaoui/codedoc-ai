package com.codedoc.ai;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class Anonymizer {

        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                        "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}",
                        Pattern.CASE_INSENSITIVE);

        private static final Pattern PHONE_PATTERN = Pattern.compile(
                        "(?:\\+?\\d{1,3}[\\s\\-.]?)?"
                                        + "(?:\\(?\\d{2,4}\\)?[\\s\\-.]?)?"
                                        + "\\d{3,4}[\\s\\-.]?\\d{3,4}");

        private static final Pattern API_KEY_GENERAL_PATTERN = Pattern.compile(
                        "(?i)(?:api[_-]?key|apikey|secret[_-]?key|secret|token|access[_-]?key|client[_-]?secret)"
                                        + "\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-\\.]{16,})[\"']?");

        private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
                        "(?i)bearer\\s+[A-Za-z0-9_\\-\\.]{20,}");

        private static final Pattern SK_KEY_PATTERN = Pattern.compile(
                        "(?:sk|pk|rk)-[A-Za-z0-9]{20,}");

        private static final Pattern JWT_PATTERN = Pattern.compile(
                        "eyJ[A-Za-z0-9_\\-]+\\.eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+");

        private final java.util.concurrent.atomic.AtomicLong redactionsCounter = new java.util.concurrent.atomic.AtomicLong(0);

        public long getRedactionsCount() {
                return redactionsCounter.get();
        }

        public String clean(String input) {
                if (input == null || input.isEmpty()) {
                        return input;
                }

                String result = input;
                int count = 0;
                var emailMatcher = EMAIL_PATTERN.matcher(result);
                while (emailMatcher.find()) count++;
                result = emailMatcher.replaceAll("[EMAIL]");

                var skMatcher = SK_KEY_PATTERN.matcher(result);
                while (skMatcher.find()) count++;
                result = skMatcher.replaceAll("[REDACTED]");

                var jwtMatcher = JWT_PATTERN.matcher(result);
                while (jwtMatcher.find()) count++;
                result = jwtMatcher.replaceAll("[REDACTED]");

                var bearerMatcher = BEARER_TOKEN_PATTERN.matcher(result);
                while (bearerMatcher.find()) count++;
                result = bearerMatcher.replaceAll("[REDACTED]");

                var apiKeyMatcher = API_KEY_GENERAL_PATTERN.matcher(result);
                while (apiKeyMatcher.find()) count++;
                result = apiKeyMatcher.replaceAll(
                                matcher -> matcher.group().replaceFirst(
                                                Pattern.quote(matcher.group(1)), "[REDACTED]"));

                var phoneMatcher = PHONE_PATTERN.matcher(result);
                while (phoneMatcher.find()) count++;
                result = phoneMatcher.replaceAll("[PHONE]");

                if (count > 0) {
                        redactionsCounter.addAndGet(count);
                }

                return result;
        }
}
