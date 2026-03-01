package com.minewright.security;

import java.util.regex.Pattern;

/**
 * Security utility for sanitizing user input before sending to LLMs or other systems.
 *
 * <p><b>Attack Vectors Prevented:</b></p>
 * <ul>
 *   <li><b>Prompt Injection:</b> Attempts to override system instructions</li>
 *   <li><b>Jailbreak Attempts:</b> Patterns like "ignore previous instructions"</li>
 *   <li><b>Special Character Exploits:</b> Control characters, escape sequences</li>
 *   <li><b>Length Attacks:</b> Extremely long inputs that could cause issues</li>
 *   <li><b>Repetition Attacks:</b> Repeated characters that could confuse models</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Sanitize user command before sending to LLM
 * String cleanCommand = InputSanitizer.forLLM(userCommand);
 *
 * // Sanitize with custom max length
 * String clean = InputSanitizer.forLLM(input, 1000);
 *
 * // Check if input contains suspicious patterns
 * if (InputSanitizer.containsSuspiciousPatterns(input)) {
 *     logger.warn("Suspicious input detected");
 * }
 * }</pre>
 *
 * @since 2.2.0
 */
public class InputSanitizer {

    // Maximum input lengths
    private static final int DEFAULT_MAX_LENGTH = 2000;
    private static final int MAX_COMMAND_LENGTH = 500;
    private static final int MAX_USERNAME_LENGTH = 50;

    // Patterns for detecting prompt injection attempts
    private static final Pattern[] INJECTION_PATTERNS = {
        // Common jailbreak phrases
        Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|above|the)\\s+(instructions?|prompts?|commands?|text)"),
        Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|above|the)\\s+(instructions?|prompts?|commands?|text)"),
        Pattern.compile("(?i)forget\\s+(all\\s+)?(previous|above|the)\\s+(instructions?|prompts?|commands?|text)"),
        Pattern.compile("(?i)(do\\s+not|don't)\\s+follow\\s+(the\\s+)?(instructions?|prompts?|rules?)"),
        Pattern.compile("(?i)act\\s+as\\s+a\\s+different"),
        Pattern.compile("(?i)pretend\\s+(to\\s+be|you\\s+are)"),
        Pattern.compile("(?i)you\\s+are\\s+now\\s+a"),
        Pattern.compile("(?i)override\\s+(the\\s+)?(system|default)"),
        Pattern.compile("(?i)new\\s+(role|character|persona)"),

        // JSON/YAML termination attempts
        Pattern.compile("(?i)\\}\\s*\\]\\s*\\}\\s*\""),
        Pattern.compile("(?i)\"\\s*\\}\\s*\\]\\s*\\}"),

        // Code execution attempts
        Pattern.compile("(?i)(exec|eval|system)\\s*\\("),
        Pattern.compile("(?i)```\\s*(javascript|python|java|bash|shell)"),

        // System prompt extraction attempts
        Pattern.compile("(?i)print\\s+(the\\s+)?(system|above|previous)\\s+(prompt|instruction|text)"),
        Pattern.compile("(?i)repeat\\s+(the\\s+)?(system|above|previous)\\s+(prompt|instruction|text)"),
        Pattern.compile("(?i)show\\s+(the\\s+)?(system|above|previous)\\s+(prompt|instruction|text)"),
        Pattern.compile("(?i)what\\s+(are|were)\\s+(the\\s+)?(system|above|previous)\\s+(instructions?|prompts?)"),

        // DAN and similar jailbreak patterns
        Pattern.compile("(?i)\\bDAN\\b"),
        Pattern.compile("(?i)developer\\s+mode"),
        Pattern.compile("(?i)unrestricted\\s+mode"),
        Pattern.compile("(?i)no\\s+limitations"),
        Pattern.compile("(?i)bypass\\s+(safety|security|filters)"),
        Pattern.compile("(?i)(ignore|disable)\\s+(safety|security|ethical|moral)"),

        // Role hijacking attempts
        Pattern.compile("(?i)you\\s+are\\s+(no\\s+longer|not)\\s+(bound\\s+by|constrained\\s+by)"),
        Pattern.compile("(?i)from\\s+now\\s+on\\s+you\\s+are"),
        Pattern.compile("(?i)starting\\s+now\\s+you\\s+will"),

        // delimiter manipulation
        Pattern.compile("(?i)(<\\|[Ee][Nn][Dd]|<[Ee][Nn][Dd]|\\[Ee[ Nn][Dd]\\]|\\[\\[Ee[ Nn][Dd]\\]\\]|\"\"\"|'''|```)")
    };

    // Patterns for detecting repeated characters (potential DoS)
    private static final Pattern EXCESSIVE_REPETITION = Pattern.compile("(.)\\1{30,}");

    // Control characters that should be stripped
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B-\\x0C\\x0E-\\x1F\\x7F]");

    // Dangerous escape sequences
    private static final Pattern DANGEROUS_ESCAPES = Pattern.compile("\\\\[nrtvfb\\\\'\"]{2,}");

    private InputSanitizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Sanitizes user input for use in LLM prompts with default max length.
     *
     * @param input The raw user input
     * @return Sanitized input safe for LLM consumption
     * @throws IllegalArgumentException if input contains critical injection patterns
     */
    public static String forLLM(String input) {
        return forLLM(input, DEFAULT_MAX_LENGTH);
    }

    /**
     * Sanitizes user input for use in LLM prompts with custom max length.
     *
     * <p>Sanitization steps:</p>
     * <ol>
     *   <li>Check for critical injection patterns (throws exception if found)</li>
     *   <li>Remove control characters</li>
     *   <li>Collapse dangerous escape sequences</li>
     *   <li>Limit excessive character repetition</li>
     *   <li>Trim and limit length</li>
     * </ol>
     *
     * @param input The raw user input
     * @param maxLength Maximum allowed length
     * @return Sanitized input safe for LLM consumption
     * @throws IllegalArgumentException if input contains critical injection patterns
     */
    public static String forLLM(String input, int maxLength) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Check for critical injection patterns first
        String sanitized = checkForCriticalPatterns(input);

        // Remove control characters
        sanitized = CONTROL_CHARS.matcher(sanitized).replaceAll("");

        // Collapse dangerous escape sequences
        sanitized = DANGEROUS_ESCAPES.matcher(sanitized).replaceAll("");

        // Limit excessive repetition
        sanitized = EXCESSIVE_REPETITION.matcher(sanitized).replaceAll("$1$1$1");

        // Trim whitespace
        sanitized = sanitized.trim();

        // Enforce max length
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
            // Try to end at a word boundary
            int lastSpace = sanitized.lastIndexOf(' ');
            if (lastSpace > maxLength * 3 / 4) {
                sanitized = sanitized.substring(0, lastSpace);
            }
        }

        return sanitized;
    }

    /**
     * Sanitizes a command with stricter length limits.
     *
     * @param command The raw command
     * @return Sanitized command
     * @throws IllegalArgumentException if command contains injection patterns
     */
    public static String forCommand(String command) {
        return forLLM(command, MAX_COMMAND_LENGTH);
    }

    /**
     * Sanitizes a username with strict character and length limits.
     *
     * @param username The raw username
     * @return Sanitized username
     */
    public static String forUsername(String username) {
        if (username == null || username.isEmpty()) {
            return username;
        }

        // Allow only alphanumeric, spaces, underscores, hyphens, and periods
        String sanitized = username.replaceAll("[^a-zA-Z0-9 _\\.-]", "");
        sanitized = sanitized.trim();

        // Limit length
        if (sanitized.length() > MAX_USERNAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_USERNAME_LENGTH);
        }

        return sanitized;
    }

    /**
     * Checks if input contains suspicious patterns without modifying it.
     *
     * @param input The input to check
     * @return true if suspicious patterns are detected
     */
    public static boolean containsSuspiciousPatterns(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(lowerInput).find()) {
                return true;
            }
        }

        // Check for excessive repetition
        if (EXCESSIVE_REPETITION.matcher(input).find()) {
            return true;
        }

        return false;
    }

    /**
     * Gets a description of detected suspicious patterns.
     *
     * @param input The input to analyze
     * @return Description of suspicious patterns found, or empty string if none
     */
    public static String getSuspiciousPatternDescription(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String lowerInput = input.toLowerCase();

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(lowerInput).find()) {
                return "Potential prompt injection or jailbreak attempt detected";
            }
        }

        if (EXCESSIVE_REPETITION.matcher(input).find()) {
            return "Excessive character repetition detected";
        }

        if (CONTROL_CHARS.matcher(input).find()) {
            return "Control characters detected";
        }

        return "";
    }

    /**
     * Checks input for critical injection patterns and throws exception if found.
     *
     * @param input The input to check
     * @return The original input if safe
     * @throws IllegalArgumentException if critical patterns are detected
     */
    private static String checkForCriticalPatterns(String input) {
        // High-confidence injection patterns that should be rejected
        String[] criticalPatterns = {
            "(?i)ignore\\s+(all\\s+)?(previous|above)\\s+instructions?.*\\n.*\\n",
            "(?i)<\\|[Ee][Nn][Dd]|```\\s*(javascript|python)",
            "(?i)\\bDAN\\b.*\\bmode\\b",
            "(?i)(exec|eval)\\s*\\(.*\\)"
        };

        for (String pattern : criticalPatterns) {
            if (Pattern.compile(pattern).matcher(input).find()) {
                throw new IllegalArgumentException(
                    "Input contains potentially malicious patterns and was rejected"
                );
            }
        }

        return input;
    }

    /**
     * Validates input and returns detailed validation result.
     *
     * @param input The input to validate
     * @param maxLength Maximum allowed length
     * @return Validation result with details
     */
    public static ValidationResult validate(String input, int maxLength) {
        ValidationResult result = new ValidationResult();

        if (input == null) {
            result.valid = false;
            result.reason = "Input is null";
            return result;
        }

        if (input.isEmpty()) {
            result.valid = true;
            result.reason = "Input is empty (valid)";
            return result;
        }

        // Check length
        if (input.length() > maxLength) {
            result.valid = false;
            result.reason = String.format("Input exceeds maximum length: %d > %d", input.length(), maxLength);
            return result;
        }

        // Check for suspicious patterns
        String suspiciousDesc = getSuspiciousPatternDescription(input);
        if (!suspiciousDesc.isEmpty()) {
            result.valid = false;
            result.hasSuspiciousPatterns = true;
            result.reason = suspiciousDesc;
            return result;
        }

        result.valid = true;
        result.reason = "Input is safe";
        return result;
    }

    /**
     * Result of input validation.
     */
    public static class ValidationResult {
        private boolean valid = true;
        private String reason = "";
        private boolean hasSuspiciousPatterns = false;

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        public boolean hasSuspiciousPatterns() {
            return hasSuspiciousPatterns;
        }
    }
}
