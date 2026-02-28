package com.minewright.llm.cascade;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Record capturing routing decisions for analytics and monitoring.
 *
 * <p>Each routing decision is logged to enable analysis of routing effectiveness,
 * cost optimization, and system behavior patterns.</p>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li><b>Analytics:</b> Track routing patterns and hit rates</li>
 *   <li><b>Cost Analysis:</b> Monitor LLM spending by tier</li>
 *   <li><b>Performance:</b> Identify latency bottlenecks</li>
 *   <li><b>Optimization:</b> Improve routing heuristics over time</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Immutable and thread-safe</p>
 *
 * @param command The original command
 * @param detectedComplexity The complexity level detected by analysis
 * @param selectedTier The tier selected by routing logic
 * @param actualTier The tier that actually handled the request (may differ due to fallback)
 * @param latencyMs Request latency in milliseconds
 * @param tokensUsed Total tokens consumed (prompt + completion)
 * @param success Whether the request completed successfully
 * @param timestamp When the routing decision was made
 * @param fromCache Whether response was served from cache
 * @param fallbackReason Reason for tier fallback, if applicable
 * @param errorMessage Error message if request failed
 *
 * @since 1.6.0
 */
public record RoutingDecision(
    String command,
    TaskComplexity detectedComplexity,
    LLMTier selectedTier,
    LLMTier actualTier,
    long latencyMs,
    int tokensUsed,
    boolean success,
    Instant timestamp,
    boolean fromCache,
    String fallbackReason,
    String errorMessage
) {

    /**
     * Creates a new RoutingDecision with required fields.
     * Optional fields are set to defaults.
     *
     * @param command The original command
     * @param detectedComplexity Detected complexity level
     * @param selectedTier Initially selected tier
     * @param actualTier Actual tier that handled request
     * @param latencyMs Request latency (ms)
     * @param tokensUsed Tokens consumed
     * @param success Whether request succeeded
     */
    public RoutingDecision(
        String command,
        TaskComplexity detectedComplexity,
        LLMTier selectedTier,
        LLMTier actualTier,
        long latencyMs,
        int tokensUsed,
        boolean success
    ) {
        this(
            command,
            detectedComplexity,
            selectedTier,
            actualTier,
            latencyMs,
            tokensUsed,
            success,
            Instant.now(),
            selectedTier == LLMTier.CACHE,
            selectedTier.equals(actualTier) ? null : inferFallbackReason(selectedTier, actualTier),
            success ? null : "Request failed"
        );
    }

    /**
     * Creates a RoutingDecision for a cache hit.
     *
     * @param command The original command
     * @param detectedComplexity Detected complexity level
     * @param latencyMs Cache lookup latency (ms)
     * @return RoutingDecision for cache hit
     */
    public static RoutingDecision cacheHit(String command, TaskComplexity detectedComplexity, long latencyMs) {
        return new RoutingDecision(
            command,
            detectedComplexity,
            LLMTier.CACHE,
            LLMTier.CACHE,
            latencyMs,
            0,
            true,
            Instant.now(),
            true,
            null,
            null
        );
    }

    /**
     * Creates a RoutingDecision for a failed request.
     *
     * @param command The original command
     * @param detectedComplexity Detected complexity level
     * @param selectedTier Selected tier
     * @param actualTier Actual tier that failed
     * @param latencyMs Latency before failure
     * @param errorMessage Error details
     * @return RoutingDecision for failed request
     */
    public static RoutingDecision failure(
        String command,
        TaskComplexity detectedComplexity,
        LLMTier selectedTier,
        LLMTier actualTier,
        long latencyMs,
        String errorMessage
    ) {
        return new RoutingDecision(
            command,
            detectedComplexity,
            selectedTier,
            actualTier,
            latencyMs,
            0,
            false,
            Instant.now(),
            false,
            selectedTier.equals(actualTier) ? null : inferFallbackReason(selectedTier, actualTier),
            errorMessage
        );
    }

    /**
     * Infers fallback reason from tier difference.
     *
     * @param selectedTier Originally selected tier
     * @param actualTier Actual tier used
     * @return Fallback reason description
     */
    private static String inferFallbackReason(LLMTier selectedTier, LLMTier actualTier) {
        if (selectedTier.equals(actualTier)) {
            return null;
        }

        if (actualTier == LLMTier.CACHE) {
            return "Cache hit after initial miss";
        }

        if (actualTier.ordinal() > selectedTier.ordinal()) {
            return String.format("Escalated from %s to %s (failure/retry)", selectedTier, actualTier);
        }

        if (actualTier.ordinal() < selectedTier.ordinal()) {
            return String.format("Downgraded from %s to %s (cost optimization)", selectedTier, actualTier);
        }

        return String.format("Tier changed from %s to %s", selectedTier, actualTier);
    }

    /**
     * Returns the estimated cost of this routing decision in USD.
     *
     * @return Estimated cost (0.0 for cache/free tiers)
     */
    public double getEstimatedCost() {
        return actualTier.estimateCost(tokensUsed);
    }

    /**
     * Checks if this was a successful routing decision.
     *
     * @return true if request succeeded
     */
    public boolean isSuccessful() {
        return success;
    }

    /**
     * Checks if a fallback occurred during routing.
     *
     * @return true if actual tier differs from selected tier
     */
    public boolean hasFallback() {
        return !selectedTier.equals(actualTier);
    }

    /**
     * Returns a human-readable summary of this routing decision.
     *
     * @return Summary string
     */
    public String getSummary() {
        return String.format(
            "RoutingDecision[%s -> %s, complexity=%s, %dms, %d tokens, $%.5f, cache=%s]",
            selectedTier.getTierId(),
            actualTier.getTierId(),
            detectedComplexity.name(),
            latencyMs,
            tokensUsed,
            getEstimatedCost(),
            fromCache
        );
    }

    @Override
    public String toString() {
        return String.format(
            "RoutingDecision{command='%s', complexity=%s, selected=%s, actual=%s, " +
            "latency=%dms, tokens=%d, cost=$%.5f, success=%s, cache=%s, fallback=%s}",
            truncate(command, 30),
            detectedComplexity,
            selectedTier,
            actualTier,
            latencyMs,
            tokensUsed,
            getEstimatedCost(),
            success,
            fromCache,
            hasFallback() ? fallbackReason : "none"
        );
    }

    /**
     * Truncates a string to a maximum length.
     *
     * @param str String to truncate
     * @param maxLength Maximum length
     * @return Truncated string
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Validates that required fields are present and valid.
     *
     * @return Optional containing validation error, or empty if valid
     */
    public Optional<String> validate() {
        if (command == null || command.trim().isEmpty()) {
            return Optional.of("command cannot be null or empty");
        }
        if (detectedComplexity == null) {
            return Optional.of("detectedComplexity cannot be null");
        }
        if (selectedTier == null) {
            return Optional.of("selectedTier cannot be null");
        }
        if (actualTier == null) {
            return Optional.of("actualTier cannot be null");
        }
        if (latencyMs < 0) {
            return Optional.of("latencyMs cannot be negative");
        }
        if (tokensUsed < 0) {
            return Optional.of("tokensUsed cannot be negative");
        }
        if (timestamp == null) {
            return Optional.of("timestamp cannot be null");
        }
        return Optional.empty();
    }

    /**
     * Creates a builder pattern for constructing RoutingDecision instances.
     *
     * @return A new RoutingDecisionBuilder
     */
    public static RoutingDecisionBuilder builder() {
        return new RoutingDecisionBuilder();
    }

    /**
     * Builder for constructing RoutingDecision instances.
     */
    public static class RoutingDecisionBuilder {
        private String command;
        private TaskComplexity detectedComplexity;
        private LLMTier selectedTier;
        private LLMTier actualTier;
        private long latencyMs;
        private int tokensUsed;
        private boolean success;
        private Instant timestamp = Instant.now();
        private boolean fromCache;
        private String fallbackReason;
        private String errorMessage;

        public RoutingDecisionBuilder command(String command) {
            this.command = command;
            return this;
        }

        public RoutingDecisionBuilder detectedComplexity(TaskComplexity detectedComplexity) {
            this.detectedComplexity = detectedComplexity;
            return this;
        }

        public RoutingDecisionBuilder selectedTier(LLMTier selectedTier) {
            this.selectedTier = selectedTier;
            return this;
        }

        public RoutingDecisionBuilder actualTier(LLMTier actualTier) {
            this.actualTier = actualTier;
            return this;
        }

        public RoutingDecisionBuilder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public RoutingDecisionBuilder tokensUsed(int tokensUsed) {
            this.tokensUsed = tokensUsed;
            return this;
        }

        public RoutingDecisionBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public RoutingDecisionBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RoutingDecisionBuilder fromCache(boolean fromCache) {
            this.fromCache = fromCache;
            return this;
        }

        public RoutingDecisionBuilder fallbackReason(String fallbackReason) {
            this.fallbackReason = fallbackReason;
            return this;
        }

        public RoutingDecisionBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public RoutingDecision build() {
            RoutingDecision decision = new RoutingDecision(
                command,
                detectedComplexity,
                selectedTier,
                actualTier,
                latencyMs,
                tokensUsed,
                success,
                timestamp,
                fromCache,
                fallbackReason,
                errorMessage
            );
            return decision;
        }
    }
}
