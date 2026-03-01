package com.minewright.script;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Refines automation scripts based on execution feedback.
 *
 * <p>This class implements the learning component of the "One Abstraction Away" architecture.
 * It analyzes execution results and iteratively improves scripts through LLM-guided refinement.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *   <li>Analyze execution feedback to identify failure patterns</li>
 *   <li>Generate refined versions of scripts</li>
 *   <li>Track version history for scripts</li>
 *   <li>Learn from successful and failed executions</li>
 *   <li>Optimize scripts for better performance</li>
 * </ul>
 *
 * <p><b>Refinement Strategies:</b></p>
 * <ol>
 *   <li><b>Error Recovery:</b> Add fallback behaviors for common failure cases</li>
 *   <li><b>Performance Optimization:</b> Reduce redundant operations</li>
 *   <li><b>Resource Management:</b> Better inventory and tool usage</li>
 *   <li><b>Safety Improvements:</b> Add validation checks</li>
 *   <li><b>Path Optimization:</b> Improve movement patterns</li>
 * </ol>
 *
 * @see Script
 * @see ScriptGenerator
 * @see ExecutionFeedback
 * @since 1.3.0
 */
public class ScriptRefiner {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptRefiner.class);

    private final AsyncLLMClient llmClient;
    private final ScriptParser parser;
    private final ScriptValidator validator;
    private final RefinerConfig config;

    /**
     * Statistics for tracking refinement effectiveness.
     */
    private final RefinerStats stats = new RefinerStats();

    /**
     * Constructs a ScriptRefiner with required dependencies.
     *
     * @param llmClient The async LLM client for refinement
     * @param parser The script parser for parsing LLM responses
     * @param validator The script validator for safety checks
     */
    public ScriptRefiner(
            AsyncLLMClient llmClient,
            ScriptParser parser,
            ScriptValidator validator) {
        this(llmClient, parser, validator, new RefinerConfig());
    }

    /**
     * Constructs a ScriptRefiner with custom configuration.
     *
     * @param llmClient The async LLM client for refinement
     * @param parser The script parser for parsing LLM responses
     * @param validator The script validator for safety checks
     * @param config Configuration for refinement behavior
     */
    public ScriptRefiner(
            AsyncLLMClient llmClient,
            ScriptParser parser,
            ScriptValidator validator,
            RefinerConfig config) {
        this.llmClient = Objects.requireNonNull(llmClient, "LLM client cannot be null");
        this.parser = Objects.requireNonNull(parser, "Parser cannot be null");
        this.validator = Objects.requireNonNull(validator, "Validator cannot be null");
        this.config = config != null ? config : new RefinerConfig();
    }

    /**
     * Refines a script based on execution feedback.
     *
     * <p>This method analyzes the feedback, generates improvements via LLM,
     * and returns a refined version of the script with version tracking.</p>
     *
     * @param script The script to refine
     * @param feedback The execution feedback
     * @return CompletableFuture that completes with the refined script
     */
    public CompletableFuture<Script> refineAsync(
            Script script,
            ExecutionFeedback feedback) {

        Objects.requireNonNull(script, "Script cannot be null");
        Objects.requireNonNull(feedback, "Feedback cannot be null");

        LOGGER.info("[ScriptRefiner] Refining script '{}' based on {} execution",
            script.getName(), feedback.isSuccess() ? "successful" : "failed");

        // Check if refinement is needed
        if (!shouldRefine(script, feedback)) {
            LOGGER.debug("[ScriptRefiner] No refinement needed for script '{}'", script.getName());
            stats.recordSkipped();
            return CompletableFuture.completedFuture(script);
        }

        // Build the refinement prompt
        String prompt = buildRefinementPrompt(script, feedback);

        // Prepare LLM parameters
        Map<String, Object> llmParams = buildLLMParams();

        // Call LLM asynchronously
        long startTime = System.currentTimeMillis();
        return llmClient.sendAsync(prompt, llmParams)
            .thenApply(response -> {
                long latency = System.currentTimeMillis() - startTime;
                stats.recordLLMCall(response.getTokensUsed(), latency);

                LOGGER.debug("[ScriptRefiner] LLM response received in {}ms ({} tokens)",
                    latency, response.getTokensUsed());

                // Parse the refined script from LLM response
                return parseRefinedScript(response, script, feedback);
            })
            .exceptionally(throwable -> {
                LOGGER.error("[ScriptRefiner] LLM call failed for script refinement: {}",
                    script.getName(), throwable);
                stats.recordFailure();
                throw new ScriptRefinementException(
                    "Failed to refine script: " + throwable.getMessage(), throwable);
            });
    }

    /**
     * Checks if a script should be refined based on feedback.
     */
    private boolean shouldRefine(Script script, ExecutionFeedback feedback) {
        // Always refine failed scripts
        if (!feedback.isSuccess()) {
            return true;
        }

        // Refine if execution time exceeded threshold
        if (feedback.getExecutionTime().toMillis() > config.slowExecutionThresholdMs()) {
            return true;
        }

        // Refine if resource usage was high
        if (feedback.getResourceUsage() != null &&
            feedback.getResourceUsage().getScore() < config.minResourceEfficiencyScore()) {
            return true;
        }

        // Refine if user satisfaction was low
        if (feedback.getUserSatisfaction() != null &&
            feedback.getUserSatisfaction() < config.minSatisfactionScore()) {
            return true;
        }

        return false;
    }

    /**
     * Parses a refined script from the LLM response.
     */
    private Script parseRefinedScript(LLMResponse response, Script originalScript, ExecutionFeedback feedback) {
        String content = response.getContent();

        // Extract YAML script block if present
        String scriptSource = extractScriptContent(content);

        try {
            // Parse the refined script
            Script refinedScript = ScriptParser.parse(scriptSource);

            // Validate the refined script
            ScriptValidator.ValidationResult validation = validator.validate(refinedScript);
            if (!validation.isValid()) {
                LOGGER.warn("[ScriptRefiner] Refined script failed validation: {}",
                    validation.getSummary());
                stats.recordValidationFailure();
                // Return original script if refinement is invalid
                return originalScript;
            }

            // Update version and metadata
            String newVersion = incrementVersion(originalScript.getVersion());

            // Update metadata for refined script
            Script.ScriptMetadata originalMetadata = originalScript.getMetadata();
            List<String> originalTags = originalMetadata != null && originalMetadata.getTags() != null
                ? originalMetadata.getTags()
                : new ArrayList<>();

            Script.ScriptMetadata refinedMetadata = Script.ScriptMetadata.builder()
                .id(originalScript.getId())
                .name(originalScript.getName())
                .description((originalMetadata != null ? originalMetadata.getDescription() : "Script") + " (Refined)")
                .author(originalMetadata != null ? originalMetadata.getAuthor() : "unknown")
                .tags(originalTags)
                .addTag("refined")
                .createdAt(Instant.now())
                .build();

            refinedScript = refinedScript.toBuilder()
                .metadata(refinedMetadata)
                .version(newVersion)
                .build();

            stats.recordSuccess();
            LOGGER.info("[ScriptRefiner] Successfully refined script '{}' to version {}",
                originalScript.getName(), newVersion);

            return refinedScript;

        } catch (ScriptParser.ScriptParseException e) {
            LOGGER.error("[ScriptRefiner] Failed to parse refined script: {}", e.getMessage());
            stats.recordFailure();
            throw new ScriptRefinementException(
                "Failed to parse refined script: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the script content from the LLM response.
     */
    private String extractScriptContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String trimmed = content.trim();

        // Check for markdown code block with yaml
        if (trimmed.startsWith("```yaml")) {
            int start = trimmed.indexOf("```yaml") + 7;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        // Check for markdown code block without language
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n") + 1;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        // Return as-is
        return trimmed;
    }

    /**
     * Builds the prompt for script refinement.
     */
    private String buildRefinementPrompt(Script script, ExecutionFeedback feedback) {
        StringBuilder prompt = new StringBuilder(4096);

        // System instruction
        prompt.append("You are an expert Minecraft automation architect. Your task is to ");
        prompt.append("analyze a failed automation script and generate an improved version.\n\n");

        // DSL Grammar reference
        prompt.append("## Script DSL Grammar\n\n");
        prompt.append(getDSLGrammar());
        prompt.append("\n\n");

        // Original script
        prompt.append("## Original Script\n\n");
        prompt.append("```yaml\n");
        prompt.append(script.toDSL());
        prompt.append("\n```\n\n");

        // Execution feedback
        prompt.append("## Execution Feedback\n\n");
        prompt.append(feedback.toPromptSection());
        prompt.append("\n\n");

        // Refinement instructions
        prompt.append("## Refinement Instructions\n\n");
        prompt.append("Analyze the feedback and generate an improved version of the script. Focus on:\n\n");

        if (!feedback.isSuccess()) {
            prompt.append("1. **Fix the failure**: Address the specific error that occurred\n");
            if (feedback.getFailureReason() != null) {
                prompt.append("   - Failure reason: ").append(feedback.getFailureReason()).append("\n");
            }
        }

        if (feedback.getExecutionTime().toMillis() > config.slowExecutionThresholdMs()) {
            prompt.append("2. **Improve performance**: Reduce execution time by optimizing the script\n");
        }

        if (feedback.getResourceUsage() != null &&
            feedback.getResourceUsage().getScore() < config.minResourceEfficiencyScore()) {
            prompt.append("3. **Optimize resources**: Improve inventory/tool usage efficiency\n");
        }

        if (feedback.getErrorMessages() != null && !feedback.getErrorMessages().isEmpty()) {
            prompt.append("4. **Add error handling**: Add fallback behaviors for common errors\n");
        }

        prompt.append("\n## Requirements\n\n");
        prompt.append("1. Return ONLY the refined script in YAML DSL format (no markdown, no explanation)\n");
        prompt.append("2. Keep the same script ID and metadata structure\n");
        prompt.append("3. Add comments explaining key improvements\n");
        prompt.append("4. Ensure the script is safe and efficient\n");
        prompt.append("5. Use appropriate node types for better control flow\n");
        prompt.append("6. Add validation checks where appropriate\n");

        return prompt.toString();
    }

    /**
     * Returns the DSL grammar reference for the prompt.
     */
    private String getDSLGrammar() {
        return """
            Scripts use a YAML-based DSL format with behavior tree structure.

            **Node Types:**
            - `sequence`: Execute children in order, all must succeed
            - `selector`: Try children in order until one succeeds
            - `parallel`: Execute all children simultaneously
            - `action`: Execute a single atomic action
            - `condition`: Check if a condition is true
            - `loop`: Repeat child nodes N times
            - `if`: Execute if branch based on condition

            **Script Structure:**
            ```yaml
            metadata:
              id: "script-id"
              name: "Script Name"
              description: "Description"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "action_name"
                  params:
                    key: "value"

                - type: "selector"
                  steps:
                    - type: "condition"
                      condition: "condition_expression"
                    - type: "action"
                      action: "fallback_action"
            ```
            """;
    }

    /**
     * Builds LLM parameters from configuration.
     */
    private Map<String, Object> buildLLMParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("maxTokens", config.maxTokens());
        params.put("temperature", config.temperature());
        return params;
    }

    /**
     * Increments a version string.
     */
    private String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.0.1";
        }

        try {
            String[] parts = version.split("\\.");
            if (parts.length >= 3) {
                int patch = Integer.parseInt(parts[2]);
                return parts[0] + "." + parts[1] + "." + (patch + 1);
            } else if (parts.length == 2) {
                return version + ".1";
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("[ScriptRefiner] Failed to parse version '{}', using default", version);
        }

        return "1.0.1";
    }

    /**
     * Returns the current statistics.
     */
    public RefinerStats getStats() {
        return stats;
    }

    /**
     * Configuration for script refinement behavior.
     */
    public static class RefinerConfig {
        private final int maxTokens;
        private final double temperature;
        private final long slowExecutionThresholdMs;
        private final double minResourceEfficiencyScore;
        private final double minSatisfactionScore;

        public RefinerConfig() {
            this(2000, 0.7, 30000, 0.6, 0.7);
        }

        public RefinerConfig(
                int maxTokens,
                double temperature,
                long slowExecutionThresholdMs,
                double minResourceEfficiencyScore,
                double minSatisfactionScore) {
            this.maxTokens = maxTokens;
            this.temperature = temperature;
            this.slowExecutionThresholdMs = slowExecutionThresholdMs;
            this.minResourceEfficiencyScore = minResourceEfficiencyScore;
            this.minSatisfactionScore = minSatisfactionScore;
        }

        public int maxTokens() { return maxTokens; }
        public double temperature() { return temperature; }
        public long slowExecutionThresholdMs() { return slowExecutionThresholdMs; }
        public double minResourceEfficiencyScore() { return minResourceEfficiencyScore; }
        public double minSatisfactionScore() { return minSatisfactionScore; }
    }

    /**
     * Statistics for monitoring refiner performance.
     */
    public static class RefinerStats {
        private volatile int refinements = 0;
        private volatile int skipped = 0;
        private volatile int llmCalls = 0;
        private volatile int totalTokensUsed = 0;
        private volatile int totalLatencyMs = 0;
        private volatile int successes = 0;
        private volatile int failures = 0;
        private volatile int validationFailures = 0;

        public void recordRefinement() { refinements++; }
        public void recordSkipped() { skipped++; }
        public void recordLLMCall(int tokens, long latencyMs) {
            llmCalls++;
            totalTokensUsed += tokens;
            totalLatencyMs += latencyMs;
        }
        public void recordSuccess() { successes++; }
        public void recordFailure() { failures++; }
        public void recordValidationFailure() { validationFailures++; }

        public int getRefinements() { return refinements; }
        public int getSkipped() { return skipped; }
        public int getLLMCalls() { return llmCalls; }
        public int getTotalTokensUsed() { return totalTokensUsed; }
        public double getAvgTokensPerCall() {
            return llmCalls > 0 ? (double) totalTokensUsed / llmCalls : 0.0;
        }
        public double getAvgLatencyMs() {
            return llmCalls > 0 ? (double) totalLatencyMs / llmCalls : 0.0;
        }
        public int getSuccesses() { return successes; }
        public int getFailures() { return failures; }
        public int getValidationFailures() { return validationFailures; }

        @Override
        public String toString() {
            return String.format(
                "RefinerStats{refinements=%d, skipped=%d, avgTokens=%.0f, avgLatency=%.0fms, successes=%d, failures=%d}",
                refinements, skipped, getAvgTokensPerCall(), getAvgLatencyMs(), successes, failures
            );
        }
    }

    /**
     * Exception thrown when script refinement fails.
     */
    public static class ScriptRefinementException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ScriptRefinementException(String message) {
            super(message);
        }

        public ScriptRefinementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
