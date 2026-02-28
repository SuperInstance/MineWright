package com.minewright.llm.cascade;

/**
 * Classification of task complexity for intelligent LLM routing.
 *
 * <p>Complexity levels determine which LLM tier should handle a request,
 * enabling cost optimization by using cheaper/faster models for simple tasks
 * while reserving powerful models for complex reasoning.</p>
 *
 * <p><b>Routing Strategy:</b></p>
 * <ul>
 *   <li><b>TRIVIAL:</b> Cache hit or precomputed response</li>
 *   <li><b>SIMPLE:</b> Fast model (Groq llama-3.1-8b-instant)</li>
 *   <li><b>MODERATE:</b> Balanced model (Groq llama-3.3-70b or gpt-3.5)</li>
 *   <li><b>COMPLEX:</b> Smart model (gpt-4, claude) with full reasoning</li>
 *   <li><b>NOVEL:</b> Smart model with maximum context</li>
 * </ul>
 *
 * <p><b>Design Inspiration:</b> SmartCRDT/Aequor cascade routing patterns</p>
 *
 * @since 1.6.0
 */
public enum TaskComplexity {
    /**
     * Single action, well-known task.
     * <p>Examples: "mine 10 stone", "follow me", "stop"</p>
     * <p>Expected cache hit rate: 60-80%</p>
     * <p>Routing: CACHE or LOCAL (if available)</p>
     */
    TRIVIAL("single-action", "well-known pattern", 0.7),

    /**
     * 1-2 actions, straightforward execution.
     * <p>Examples: "mine 10 stone and craft a furnace", "go to x,y,z and place torch"</p>
     * <p>Expected cache hit rate: 30-50%</p>
     * <p>Routing: FAST (Groq llama-3.1-8b-instant)</p>
     */
    SIMPLE("few-actions", "straightforward", 0.5),

    /**
     * 3-5 actions, some reasoning required.
     * <p>Examples: "build a small house", "gather resources for crafting"</p>
     * <p>Expected cache hit rate: 10-20%</p>
     * <p>Routing: BALANCED (Groq llama-3.3-70b or gpt-3.5)</p>
     */
    MODERATE("multi-step", "reasoning-needed", 0.2),

    /**
     * Multiple actions, coordination between agents.
     * <p>Examples: "coordinate crew to build a castle", "setup automated farm"</p>
     * <p>Expected cache hit rate: 5-10%</p>
     * <p>Routing: SMART (gpt-4, claude) with full reasoning</p>
     */
    COMPLEX("coordinated", "multi-agent", 0.05),

    /**
     * Never seen before, needs maximum reasoning.
     * <p>Examples: Novel strategies, complex problem-solving, creative tasks</p>
     * <p>Expected cache hit rate: 0%</p>
     * <p>Routing: SMART with maximum context and low temperature</p>
     */
    NOVEL("unknown", "first-seen", 0.0);

    private final String description;
    private final String category;
    private final double expectedCacheHitRate;

    TaskComplexity(String category, String description, double expectedCacheHitRate) {
        this.category = category;
        this.description = description;
        this.expectedCacheHitRate = expectedCacheHitRate;
    }

    /**
     * Returns the complexity category.
     *
     * @return Category description
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the human-readable description.
     *
     * @return Complexity description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the expected cache hit rate for this complexity level.
     *
     * @return Expected cache hit rate (0.0 to 1.0)
     */
    public double getExpectedCacheHitRate() {
        return expectedCacheHitRate;
    }

    /**
     * Checks if this complexity level can potentially be served from cache.
     *
     * @return true if caching is beneficial for this level
     */
    public boolean isCacheable() {
        return this != NOVEL;
    }

    /**
     * Checks if this complexity level requires fast response time.
     *
     * @return true if low latency is critical
     */
    public boolean requiresLowLatency() {
        return this == TRIVIAL || this == SIMPLE;
    }

    /**
     * Checks if this complexity level requires high-quality reasoning.
     *
     * @return true if full reasoning capabilities are needed
     */
    public boolean requiresFullReasoning() {
        return this == COMPLEX || this == NOVEL;
    }

    @Override
    public String toString() {
        return String.format("%s[%s, cache=%.0f%%]", name(), description, expectedCacheHitRate * 100);
    }
}
