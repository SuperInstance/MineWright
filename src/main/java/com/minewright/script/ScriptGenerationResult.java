package com.minewright.script;

import java.time.Instant;
import java.util.*;

/**
 * Result wrapper for script generation operations.
 *
 * <p>This class encapsulates the results of script generation, including the generated
 * script, confidence scores, alternative options, explanations, and validation errors.
 * It provides comprehensive feedback about the generation process.</p>
 *
 * <p><b>Result Components:</b></p>
 * <ul>
 *   <li><b>Generated Script:</b> The primary script output</li>
 *   <li><b>Confidence Score:</b> LLM's confidence in the script (0.0-1.0)</li>
 *   <li><b>Alternative Scripts:</b> Other viable options for the same command</li>
 *   <li><b>Explanation:</b> Natural language description of what the script does</li>
 *   <li><b>Validation Errors:</b> Any validation issues found</li>
 *   <li><b>Generation Metadata:</b> Tokens used, latency, LLM model info</li>
 * </ul>
 *
 * @see ScriptGenerator
 * @see Script
 * @since 1.3.0
 */
public class ScriptGenerationResult {

    private final Script script;
    private final Double confidenceScore;
    private final List<Script> alternativeScripts;
    private final String explanation;
    private final List<String> validationErrors;
    private final List<String> validationWarnings;
    private final GenerationMetadata metadata;
    private final boolean success;
    private final String errorMessage;

    private ScriptGenerationResult(Builder builder) {
        this.script = builder.script;
        this.confidenceScore = builder.confidenceScore;
        this.alternativeScripts = Collections.unmodifiableList(new ArrayList<>(builder.alternativeScripts));
        this.explanation = builder.explanation;
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(builder.validationErrors));
        this.validationWarnings = Collections.unmodifiableList(new ArrayList<>(builder.validationWarnings));
        this.metadata = builder.metadata;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    /**
     * Creates a new builder for constructing ScriptGenerationResult.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a successful result with a script.
     */
    public static ScriptGenerationResult success(Script script) {
        return builder()
            .script(script)
            .success(true)
            .build();
    }

    /**
     * Creates a successful result with script and metadata.
     */
    public static ScriptGenerationResult success(Script script, GenerationMetadata metadata) {
        return builder()
            .script(script)
            .metadata(metadata)
            .success(true)
            .build();
    }

    /**
     * Creates a failed result with an error message.
     */
    public static ScriptGenerationResult failure(String errorMessage) {
        return builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Creates a failed result with validation errors.
     */
    public static ScriptGenerationResult validationFailure(Script script, List<String> errors) {
        return builder()
            .script(script)
            .success(false)
            .validationErrors(errors)
            .errorMessage("Script validation failed")
            .build();
    }

    /**
     * Checks if the generation was successful.
     */
    public boolean isSuccess() {
        return success && script != null;
    }

    /**
     * Checks if there are validation errors.
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * Checks if there are validation warnings.
     */
    public boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }

    /**
     * Gets the best script (primary or first alternative if primary failed).
     */
    public Script getBestScript() {
        if (script != null) {
            return script;
        }
        if (!alternativeScripts.isEmpty()) {
            return alternativeScripts.get(0);
        }
        return null;
    }

    /**
     * Gets a summary of the generation result.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder(256);

        if (success) {
            sb.append("Generation SUCCESS");
            if (script != null) {
                sb.append(" - Script: '").append(script.getName()).append("'");
            }
            if (confidenceScore != null) {
                sb.append(" (confidence: ").append(String.format("%.2f", confidenceScore)).append(")");
            }
        } else {
            sb.append("Generation FAILED");
            if (errorMessage != null) {
                sb.append(" - ").append(errorMessage);
            }
        }

        if (hasValidationErrors()) {
            sb.append("\nErrors: ").append(validationErrors.size());
        }

        if (hasValidationWarnings()) {
            sb.append("\nWarnings: ").append(validationWarnings.size());
        }

        if (!alternativeScripts.isEmpty()) {
            sb.append("\nAlternatives: ").append(alternativeScripts.size());
        }

        if (metadata != null) {
            sb.append("\nMetadata: ").append(metadata.getSummary());
        }

        return sb.toString();
    }

    // Getters

    public Script getScript() { return script; }
    public Double getConfidenceScore() { return confidenceScore; }
    public List<Script> getAlternativeScripts() { return alternativeScripts; }
    public String getExplanation() { return explanation; }
    public List<String> getValidationErrors() { return validationErrors; }
    public List<String> getValidationWarnings() { return validationWarnings; }
    public GenerationMetadata getMetadata() { return metadata; }
    public boolean getSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    /**
     * Builder for constructing ScriptGenerationResult instances.
     */
    public static class Builder {
        private Script script;
        private Double confidenceScore;
        private List<Script> alternativeScripts = new ArrayList<>();
        private String explanation;
        private List<String> validationErrors = new ArrayList<>();
        private List<String> validationWarnings = new ArrayList<>();
        private GenerationMetadata metadata;
        private boolean success = true;
        private String errorMessage;

        public Builder script(Script script) {
            this.script = script;
            return this;
        }

        public Builder confidenceScore(Double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder addAlternative(Script alternative) {
            this.alternativeScripts.add(alternative);
            return this;
        }

        public Builder alternativeScripts(List<Script> alternatives) {
            this.alternativeScripts = new ArrayList<>(alternatives);
            return this;
        }

        public Builder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public Builder addValidationError(String error) {
            this.validationErrors.add(error);
            return this;
        }

        public Builder validationErrors(List<String> errors) {
            this.validationErrors = new ArrayList<>(errors);
            return this;
        }

        public Builder addValidationWarning(String warning) {
            this.validationWarnings.add(warning);
            return this;
        }

        public Builder validationWarnings(List<String> warnings) {
            this.validationWarnings = new ArrayList<>(warnings);
            return this;
        }

        public Builder metadata(GenerationMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ScriptGenerationResult build() {
            return new ScriptGenerationResult(this);
        }
    }

    /**
     * Metadata about the script generation process.
     */
    public static class GenerationMetadata {
        private final String llmProvider;
        private final String model;
        private final int tokensUsed;
        private final long latencyMs;
        private final Instant generatedAt;
        private final String originalCommand;
        private final int attemptNumber;
        private final boolean fromCache;

        private GenerationMetadata(Builder builder) {
            this.llmProvider = builder.llmProvider;
            this.model = builder.model;
            this.tokensUsed = builder.tokensUsed;
            this.latencyMs = builder.latencyMs;
            this.generatedAt = builder.generatedAt != null ? builder.generatedAt : Instant.now();
            this.originalCommand = builder.originalCommand;
            this.attemptNumber = builder.attemptNumber;
            this.fromCache = builder.fromCache;
        }

        /**
         * Creates a new builder for GenerationMetadata.
         */
        public static Builder builder() {
            return new Builder();
        }

        public String getLLMProvider() { return llmProvider; }
        public String getModel() { return model; }
        public int getTokensUsed() { return tokensUsed; }
        public long getLatencyMs() { return latencyMs; }
        public Instant getGeneratedAt() { return generatedAt; }
        public String getOriginalCommand() { return originalCommand; }
        public int getAttemptNumber() { return attemptNumber; }
        public boolean isFromCache() { return fromCache; }

        /**
         * Gets a summary of the metadata.
         */
        public String getSummary() {
            return String.format(
                "provider=%s, model=%s, tokens=%d, latency=%dms, cached=%s",
                llmProvider, model, tokensUsed, latencyMs, fromCache
            );
        }

        /**
         * Builder for constructing GenerationMetadata instances.
         */
        public static class Builder {
            private String llmProvider;
            private String model;
            private int tokensUsed;
            private long latencyMs;
            private Instant generatedAt;
            private String originalCommand;
            private int attemptNumber = 1;
            private boolean fromCache = false;

            public Builder llmProvider(String llmProvider) {
                this.llmProvider = llmProvider;
                return this;
            }

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder tokensUsed(int tokensUsed) {
                this.tokensUsed = tokensUsed;
                return this;
            }

            public Builder latencyMs(long latencyMs) {
                this.latencyMs = latencyMs;
                return this;
            }

            public Builder generatedAt(Instant generatedAt) {
                this.generatedAt = generatedAt;
                return this;
            }

            public Builder originalCommand(String originalCommand) {
                this.originalCommand = originalCommand;
                return this;
            }

            public Builder attemptNumber(int attemptNumber) {
                this.attemptNumber = attemptNumber;
                return this;
            }

            public Builder fromCache(boolean fromCache) {
                this.fromCache = fromCache;
                return this;
            }

            public GenerationMetadata build() {
                return new GenerationMetadata(this);
            }
        }
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Creates a result from a validation result.
     */
    public static ScriptGenerationResult fromValidation(
            Script script,
            ScriptValidator.ValidationResult validation) {

        Builder builder = builder()
            .script(script)
            .success(validation.isValid());

        if (!validation.getErrors().isEmpty()) {
            builder.validationErrors(validation.getErrors());
        }

        if (!validation.getWarnings().isEmpty()) {
            builder.validationWarnings(validation.getWarnings());
        }

        if (!validation.isValid()) {
            builder.errorMessage("Script validation failed with " +
                validation.getErrors().size() + " errors");
        }

        return builder.build();
    }
}
