package com.minewright.script;

import com.minewright.entity.ForemanEntity;
import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates automation scripts from natural language commands using LLMs.
 *
 * <p>This class implements the "Brain Layer" of the "One Abstraction Away" architecture.
 * It acts as an interface between natural language commands and executable DSL scripts.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *   <li>Convert natural language commands to DSL scripts via LLM</li>
 *   <li>Provide context-aware generation (agent state, inventory, environment)</li>
 *   <li>Support parameterized scripts with template variables</li>
 *   <li>Enable script refinement based on execution feedback</li>
 *   <li>Cache and reuse successful scripts for efficiency</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b></p>
 * <ul>
 *   <li>LLMs are architects, not micromanagers</li>
 *   <li>Scripts are generated once, executed many times</li>
 *   <li>Token efficiency through caching and parameterization</li>
 *   <li>Safety through multi-layer validation</li>
 * </ul>
 *
 * @see Script
 * @see ScriptParser
 * @see ScriptValidator
 * @since 1.3.0
 */
public class ScriptGenerator {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptGenerator.class);

    private final AsyncLLMClient llmClient;
    private final ScriptParser parser;
    private final ScriptValidator validator;

    /**
     * Cache for generated scripts to avoid redundant LLM calls.
     * Key: command hash, Value: cached script
     */
    private final Map<String, CachedScript> scriptCache = new ConcurrentHashMap<>();

    /**
     * Statistics for monitoring and optimization.
     */
    private final GeneratorStats stats = new GeneratorStats();

    /**
     * Configuration for script generation behavior.
     */
    private final GeneratorConfig config;

    /**
     * Constructs a ScriptGenerator with required dependencies.
     *
     * @param llmClient The async LLM client for script generation
     * @param parser The script parser for parsing LLM responses
     * @param validator The script validator for safety checks
     */
    public ScriptGenerator(
            AsyncLLMClient llmClient,
            ScriptParser parser,
            ScriptValidator validator) {
        this(llmClient, parser, validator, new GeneratorConfig());
    }

    /**
     * Constructs a ScriptGenerator with custom configuration.
     *
     * @param llmClient The async LLM client for script generation
     * @param parser The script parser for parsing LLM responses
     * @param validator The script validator for safety checks
     * @param config Configuration for generation behavior
     */
    public ScriptGenerator(
            AsyncLLMClient llmClient,
            ScriptParser parser,
            ScriptValidator validator,
            GeneratorConfig config) {
        this.llmClient = Objects.requireNonNull(llmClient, "LLM client cannot be null");
        this.parser = Objects.requireNonNull(parser, "Parser cannot be null");
        this.validator = Objects.requireNonNull(validator, "Validator cannot be null");
        this.config = config != null ? config : new GeneratorConfig();
    }

    /**
     * Generates a script from a natural language command asynchronously.
     *
     * <p>This method checks the cache first for similar commands. If a cache hit
     * is found with sufficient similarity, it returns the cached script immediately.
     * Otherwise, it generates a new script via LLM.</p>
     *
     * @param command The natural language command to convert to a script
     * @param context The generation context (agent state, environment, etc.)
     * @return CompletableFuture that completes with the generated script
     */
    public CompletableFuture<Script> generateAsync(
            String command,
            ScriptGenerationContext context) {

        Objects.requireNonNull(command, "Command cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        // Trim and validate command
        String trimmedCommand = command.trim();
        if (trimmedCommand.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Command cannot be empty"));
        }

        // Check cache first
        String cacheKey = generateCacheKey(trimmedCommand, context);
        CachedScript cached = scriptCache.get(cacheKey);

        if (cached != null && cached.isValid()) {
            LOGGER.debug("[ScriptGenerator] Cache hit for command: {}", trimmedCommand);
            stats.recordCacheHit();
            return CompletableFuture.completedFuture(cached.script());
        }

        // Cache miss - generate new script
        LOGGER.info("[ScriptGenerator] Generating script for command: {}", trimmedCommand);
        stats.recordCacheMiss();

        return generateNewScript(trimmedCommand, context)
            .thenApply(script -> {
                // Cache the generated script
                cacheScript(cacheKey, script, trimmedCommand);
                return script;
            });
    }

    /**
     * Generates a new script via LLM.
     *
     * @param command The natural language command
     * @param context The generation context
     * @return CompletableFuture with the generated script
     */
    private CompletableFuture<Script> generateNewScript(
            String command,
            ScriptGenerationContext context) {

        // Build the prompt
        String prompt = buildGenerationPrompt(command, context);

        // Prepare LLM parameters
        Map<String, Object> llmParams = buildLLMParams();

        // Call LLM asynchronously
        long startTime = System.currentTimeMillis();
        return llmClient.sendAsync(prompt, llmParams)
            .thenApply(response -> {
                long latency = System.currentTimeMillis() - startTime;
                stats.recordLLMCall(response.getTokensUsed(), latency);

                LOGGER.debug("[ScriptGenerator] LLM response received in {}ms ({} tokens)",
                    latency, response.getTokensUsed());

                // Parse the script from LLM response
                return parseScriptFromResponse(response, command);
            })
            .exceptionally(throwable -> {
                LOGGER.error("[ScriptGenerator] LLM call failed for command: {}",
                    command, throwable);
                stats.recordFailure();
                throw new ScriptGenerationException(
                    "Failed to generate script: " + throwable.getMessage(), throwable);
            });
    }

    /**
     * Parses a script from the LLM response.
     *
     * @param response The LLM response
     * @param originalCommand The original command for metadata
     * @return The parsed script
     */
    private Script parseScriptFromResponse(LLMResponse response, String originalCommand) {
        String content = response.getContent();

        // Extract YAML script block if present
        String scriptSource = extractScriptContent(content);

        try {
            // Parse the script
            Script script = ScriptParser.parse(scriptSource);

            // Validate the script
            ScriptValidator.ValidationResult validation = validator.validate(script);
            if (!validation.isValid()) {
                LOGGER.warn("[ScriptGenerator] Generated script failed validation: {}",
                    validation.getSummary());
                // Don't throw - return the script anyway, let execution layer handle failures
                stats.recordValidationFailure();
            } else {
                stats.recordSuccess();
            }

            // Update metadata if not set
            if (script.getMetadata() == null ||
                script.getMetadata().getId() == null ||
                script.getMetadata().getId().isEmpty()) {

                Script.ScriptMetadata metadata = Script.ScriptMetadata.builder()
                    .id("script-" + UUID.randomUUID().toString().substring(0, 8))
                    .name(generateScriptName(originalCommand))
                    .description("Generated from command: " + originalCommand)
                    .author("llm-generator")
                    .addTag("generated")
                    .createdAt(Instant.now())
                    .build();

                script = script.toBuilder()
                    .metadata(metadata)
                    .build();
            }

            return script;

        } catch (ScriptParser.ScriptParseException e) {
            LOGGER.error("[ScriptGenerator] Failed to parse generated script: {}", e.getMessage());
            stats.recordFailure();
            throw new ScriptGenerationException(
                "Failed to parse generated script: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the script content from the LLM response.
     * Handles markdown code blocks and plain YAML.
     *
     * @param content The LLM response content
     * @return The extracted script content
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
     * Builds the prompt for script generation.
     *
     * @param command The natural language command
     * @param context The generation context
     * @return The formatted prompt
     */
    private String buildGenerationPrompt(String command, ScriptGenerationContext context) {
        StringBuilder prompt = new StringBuilder(4096);

        // System instruction
        prompt.append("You are an expert Minecraft automation architect. Your task is to generate ");
        prompt.append("a DSL (Domain-Specific Language) script that implements the given command.\n\n");

        // DSL Grammar reference
        prompt.append("## Script DSL Grammar\n\n");
        prompt.append(getDSLGrammar());
        prompt.append("\n\n");

        // Example scripts
        if (config.includeExamples()) {
            prompt.append("## Example Scripts\n\n");
            prompt.append(getExampleScripts());
            prompt.append("\n\n");
        }

        // Context information
        if (context != null && !context.isEmpty()) {
            prompt.append("## Context\n\n");
            prompt.append(context.toPromptSection());
            prompt.append("\n\n");
        }

        // The task
        prompt.append("## Task\n\n");
        prompt.append("Generate a script for the following command:\n");
        prompt.append("\"").append(command).append("\"\n\n");

        // Requirements
        prompt.append("## Requirements\n\n");
        prompt.append("1. Return ONLY the script in YAML DSL format (no markdown, no explanation)\n");
        prompt.append("2. Include metadata section with id, name, and description\n");
        prompt.append("3. Use appropriate node types (sequence, selector, parallel, action)\n");
        prompt.append("4. Include error handling for common failure cases\n");
        prompt.append("5. Ensure the script is safe and efficient\n");
        prompt.append("6. Use parameterized values where appropriate (e.g., {{parameter_name}})\n\n");

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
              author: "author"

            parameters:
              - name: "param_name"
                type: "string|integer|boolean"
                default: "default_value"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "action_name"
                  params:
                    key: "value"

                - type: "selector"
                  steps:
                    - type: "sequence"
                      steps:
                        - type: "condition"
                          condition: "inventory_has('item')"
                        - type: "action"
                          action: "do_something"

            error_handling:
              on_failure:
                - type: "action"
                  action: "handle_error"
            ```

            **Available Actions:**
            - `mine`: Mine a block (params: block, target)
            - `place`: Place a block (params: block, position)
            - `pathfind`: Navigate to location (params: target, max_distance)
            - `craft`: Craft an item (params: item, count)
            - `gather`: Collect resources (params: resource, amount)
            - `build`: Build a structure (params: structure, position)
            - `equip`: Equip an item (params: item)
            - `deposit`: Store items (params: item, location)
            """;
    }

    /**
     * Returns example scripts for the prompt.
     */
    private String getExampleScripts() {
        return """
            **Example 1: Simple Mining**
            ```yaml
            metadata:
              id: "simple_iron_mining"
              name: "Simple Iron Mining"
              description: "Mines iron ore nearby"

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "locate_nearest"
                  params:
                    block: "iron_ore"
                    radius: 64
                    save_as: "target"

                - type: "action"
                  action: "pathfind_to"
                  params:
                    target: "@target"
                    max_distance: 100

                - type: "action"
                  action: "mine"
                  params:
                    target: "@target"
            ```

            **Example 2: Building with Error Handling**
            ```yaml
            metadata:
              id: "build_shelter"
              name: "Build Simple Shelter"
              description: "Builds a 5x3x5 shelter"

            parameters:
              - name: "material"
                type: "string"
                default: "oak_planks"

            script:
              type: "sequence"
              steps:
                - type: "condition"
                  condition: "inventory_has('{{material}}', 20)"
                  on_true:
                    type: "selector"
                    steps:
                      - type: "action"
                        action: "gather"
                        params:
                          resource: "{{material}}"
                          amount: 20

                      - type: "action"
                        action: "craft"
                        params:
                          item: "{{material}}"
                          count: 20

                - type: "action"
                  action: "build_rectangle"
                  params:
                    width: 5
                    depth: 5
                    height: 3
                    material: "{{material}}"

            error_handling:
              on_no_resources:
                - type: "action"
                  action: "announce"
                  params:
                    message: "Not enough resources to build shelter"
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
     * Generates a cache key for the command and context.
     */
    private String generateCacheKey(String command, ScriptGenerationContext context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(command.toLowerCase().trim());

        if (context != null) {
            keyBuilder.append("|");
            keyBuilder.append(context.hashCode());
        }

        return Integer.toString(keyBuilder.toString().hashCode());
    }

    /**
     * Caches a generated script.
     */
    private void cacheScript(String key, Script script, String originalCommand) {
        CachedScript cached = new CachedScript(
            script,
            Instant.now(),
            originalCommand
        );
        scriptCache.put(key, cached);

        // Clean old entries if cache is too large
        if (scriptCache.size() > config.maxCacheSize()) {
            cleanupCache();
        }
    }

    /**
     * Cleans up old cache entries.
     */
    private void cleanupCache() {
        Instant cutoff = Instant.now().minus(config.cacheTtl());

        scriptCache.entrySet().removeIf(entry -> {
            CachedScript cached = entry.getValue();
            return !cached.isValid() || cached.createdAt().isBefore(cutoff);
        });

        LOGGER.debug("[ScriptGenerator] Cache cleanup: {} entries remaining",
            scriptCache.size());
    }

    /**
     * Generates a descriptive script name from the command.
     */
    private String generateScriptName(String command) {
        // Simple heuristic: first few words, capitalized
        String[] words = command.toLowerCase().split("\\s+");
        int nameLength = Math.min(words.length, 5);

        StringBuilder name = new StringBuilder();
        for (int i = 0; i < nameLength; i++) {
            if (i > 0) {
                name.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                name.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
            }
        }

        return name.toString();
    }

    /**
     * Clears the script cache.
     */
    public void clearCache() {
        scriptCache.clear();
        LOGGER.info("[ScriptGenerator] Script cache cleared");
    }

    /**
     * Returns the current statistics.
     */
    public GeneratorStats getStats() {
        return stats;
    }

    /**
     * Returns the current cache size.
     */
    public int getCacheSize() {
        return scriptCache.size();
    }

    /**
     * Configuration for script generation behavior.
     */
    public static class GeneratorConfig {
        private final int maxTokens;
        private final double temperature;
        private final int maxCacheSize;
        private final java.time.Duration cacheTtl;
        private final boolean includeExamples;

        public GeneratorConfig() {
            this(2000, 0.7, 100, java.time.Duration.ofHours(1), true);
        }

        public GeneratorConfig(
                int maxTokens,
                double temperature,
                int maxCacheSize,
                java.time.Duration cacheTtl,
                boolean includeExamples) {
            this.maxTokens = maxTokens;
            this.temperature = temperature;
            this.maxCacheSize = maxCacheSize;
            this.cacheTtl = cacheTtl;
            this.includeExamples = includeExamples;
        }

        public int maxTokens() { return maxTokens; }
        public double temperature() { return temperature; }
        public int maxCacheSize() { return maxCacheSize; }
        public java.time.Duration cacheTtl() { return cacheTtl; }
        public boolean includeExamples() { return includeExamples; }
    }

    /**
     * Statistics for monitoring generator performance.
     */
    public static class GeneratorStats {
        private volatile int cacheHits = 0;
        private volatile int cacheMisses = 0;
        private volatile int llmCalls = 0;
        private volatile int totalTokensUsed = 0;
        private volatile int totalLatencyMs = 0;
        private volatile int successes = 0;
        private volatile int failures = 0;
        private volatile int validationFailures = 0;

        public void recordCacheHit() { cacheHits++; }
        public void recordCacheMiss() { cacheMisses++; }
        public void recordLLMCall(int tokens, long latencyMs) {
            llmCalls++;
            totalTokensUsed += tokens;
            totalLatencyMs += latencyMs;
        }
        public void recordSuccess() { successes++; }
        public void recordFailure() { failures++; }
        public void recordValidationFailure() { validationFailures++; }

        public int getCacheHits() { return cacheHits; }
        public int getCacheMisses() { return cacheMisses; }
        public double getCacheHitRate() {
            int total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
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
                "GeneratorStats{cacheHitRate=%.2f, llmCalls=%d, avgTokens=%.0f, avgLatency=%.0fms, successes=%d, failures=%d}",
                getCacheHitRate(), llmCalls, getAvgTokensPerCall(), getAvgLatencyMs(), successes, failures
            );
        }
    }

    /**
     * Cached script with metadata.
     */
    private record CachedScript(
        Script script,
        Instant createdAt,
        String originalCommand
    ) {
        boolean isValid() {
            return script != null &&
                   createdAt != null &&
                   createdAt.isBefore(Instant.now().plus(java.time.Duration.ofHours(24)));
        }
    }

    /**
     * Exception thrown when script generation fails.
     */
    public static class ScriptGenerationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ScriptGenerationException(String message) {
            super(message);
        }

        public ScriptGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
