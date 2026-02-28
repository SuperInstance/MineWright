package com.minewright.llm.cascade;

/**
 * Classification of LLM tiers for cascade routing.
 *
 * <p>Tiers represent different LLM providers/models with varying cost,
 * latency, and capability characteristics. The cascade router selects
 * the appropriate tier based on task complexity.</p>
 *
 * <p><b>Tier Hierarchy:</b></p>
 * <pre>
 * CACHE (fastest, free) → LOCAL (fast, no API cost) → FAST (cheap, fast)
 *   → BALANCED (moderate cost, good quality) → SMART (expensive, best quality)
 * </pre>
 *
 * <p><b>Cost vs Quality Trade-off:</b></p>
 * <ul>
 *   <li>CACHE: $0.00/1K tokens, ~1ms latency</li>
 *   <li>LOCAL: $0.00/1K tokens, ~50-200ms latency</li>
 *   <li>FAST: ~$0.00001/1K tokens, ~100-300ms latency</li>
 *   <li>BALANCED: ~$0.00020/1K tokens, ~300-800ms latency</li>
 *   <li>SMART: ~$0.01000/1K tokens, ~1000-3000ms latency</li>
 * </ul>
 *
 * <p><b>Design Inspiration:</b> SmartCRDT/Aequor tiered routing patterns</p>
 *
 * @since 1.6.0
 */
public enum LLMTier {
    /**
     * No LLM needed - served from cache.
     * <p><b>Cost:</b> Free</p>
     * <p><b>Latency:</b> ~1ms</p>
     * <p><b>Use Case:</b> Previously seen commands with cached responses</p>
     * <p><b>Hit Rate:</b> 40-60% overall, higher for TRIVIAL tasks</p>
     */
    CACHE("cache", 0.0, 1, 0, "cached response", "no LLM call"),

    /**
     * Local model execution (future: Ollama).
     * <p><b>Cost:</b> Free (local compute)</p>
     * <p><b>Latency:</b> ~50-200ms</p>
     * <p><b>Use Case:</b> Simple tasks when GPU is available</p>
     * <p><b>Note:</b> Not yet implemented, falls back to FAST tier</p>
     */
    LOCAL("local", 0.0, 100, 1, "local model", "future: Ollama"),

    /**
     * Fast cloud model for simple tasks.
     * <p><b>Model:</b> Groq llama-3.1-8b-instant</p>
     * <p><b>Cost:</b> ~$0.00001/1K tokens</p>
     * <p><b>Latency:</b> ~100-300ms</p>
     * <p><b>Use Case:</b> SIMPLE tasks with straightforward execution</p>
     */
    FAST("fast", 0.00001, 200, 8, "llama-3.1-8b-instant", "Groq"),

    /**
     * Balanced model for moderate complexity.
     * <p><b>Model:</b> Groq llama-3.3-70b or OpenAI gpt-3.5-turbo</p>
     * <p><b>Cost:</b> ~$0.00020/1K tokens</p>
     * <p><b>Latency:</b> ~300-800ms</p>
     * <p><b>Use Case:</b> MODERATE tasks requiring some reasoning</p>
     */
    BALANCED("balanced", 0.00020, 500, 70, "llama-3.3-70b/gpt-3.5", "Groq/OpenAI"),

    /**
     * Smart model for complex reasoning.
     * <p><b>Model:</b> OpenAI gpt-4 or Anthropic claude-3-opus</p>
     * <p><b>Cost:</b> ~$0.01000/1K tokens</p>
     * <p><b>Latency:</b> ~1000-3000ms</p>
     * <p><b>Use Case:</b> COMPLEX and NOVEL tasks requiring full reasoning</p>
     */
    SMART("smart", 0.01000, 2000, 1000, "gpt-4/claude-3", "OpenAI/Anthropic");

    private final String tierId;
    private final double costPer1kTokens;
    private final long expectedLatencyMs;
    private final int parameterCountBillions;
    private final String description;
    private final String provider;

    LLMTier(String tierId, double costPer1kTokens, long expectedLatencyMs,
             int parameterCountBillions, String description, String provider) {
        this.tierId = tierId;
        this.costPer1kTokens = costPer1kTokens;
        this.expectedLatencyMs = expectedLatencyMs;
        this.parameterCountBillions = parameterCountBillions;
        this.description = description;
        this.provider = provider;
    }

    /**
     * Returns the tier identifier for logging and configuration.
     *
     * @return Tier ID (e.g., "cache", "fast", "smart")
     */
    public String getTierId() {
        return tierId;
    }

    /**
     * Returns the approximate cost per 1K tokens in USD.
     *
     * @return Cost per 1K tokens (0.0 for free tiers)
     */
    public double getCostPer1kTokens() {
        return costPer1kTokens;
    }

    /**
     * Returns the expected latency in milliseconds.
     *
     * @return Expected latency (ms)
     */
    public long getExpectedLatencyMs() {
        return expectedLatencyMs;
    }

    /**
     * Returns the model parameter count in billions.
     *
     * @return Parameter count (billions)
     */
    public int getParameterCountBillions() {
        return parameterCountBillions;
    }

    /**
     * Returns the tier description (model information).
     *
     * @return Tier description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the provider name for this tier.
     *
     * @return Provider name (e.g., "Groq", "OpenAI", "Anthropic")
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Checks if this tier requires an API call (not cached/local).
     *
     * @return true if API call is required
     */
    public boolean requiresApiCall() {
        return this != CACHE && this != LOCAL;
    }

    /**
     * Checks if this tier is available for use.
     * <p>LOCAL tier returns false until Ollama integration is complete.</p>
     *
     * @return true if tier is currently available
     */
    public boolean isAvailable() {
        // LOCAL tier not yet implemented
        return this != LOCAL;
    }

    /**
     * Calculates estimated cost for a given token count.
     *
     * @param tokens Number of tokens used
     * @return Estimated cost in USD
     */
    public double estimateCost(int tokens) {
        return (tokens / 1000.0) * costPer1kTokens;
    }

    @Override
    public String toString() {
        return String.format("%s[%s, $%.5f/1K, ~%dms, %dB params]",
            name(),
            description,
            costPer1kTokens,
            expectedLatencyMs,
            parameterCountBillions
        );
    }

    /**
     * Parses a tier ID string to LLMTier enum.
     *
     * @param tierId Tier identifier (case-insensitive)
     * @return Matching LLMTier, or BALANCED if not found
     */
    public static LLMTier fromId(String tierId) {
        if (tierId == null || tierId.trim().isEmpty()) {
            return BALANCED;
        }
        try {
            return valueOf(tierId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BALANCED;
        }
    }

    /**
     * Returns the next higher tier for fallback purposes.
     * <p>SMART has no higher tier and returns itself.</p>
     *
     * @return Next higher tier
     */
    public LLMTier nextHigherTier() {
        return switch (this) {
            case CACHE -> FAST;
            case LOCAL -> FAST;
            case FAST -> BALANCED;
            case BALANCED -> SMART;
            case SMART -> SMART;
        };
    }

    /**
     * Returns the next lower tier for degradation purposes.
     * <p>CACHE has no lower tier and returns itself.</p>
     *
     * @return Next lower tier
     */
    public LLMTier nextLowerTier() {
        return switch (this) {
            case CACHE -> CACHE;
            case LOCAL -> CACHE;
            case FAST -> CACHE;
            case BALANCED -> FAST;
            case SMART -> BALANCED;
        };
    }
}
