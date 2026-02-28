package com.minewright.llm.cache;

import com.minewright.llm.async.LLMCache;
import com.minewright.llm.async.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Integrates semantic caching with the existing exact-match LLM cache.
 *
 * <p>This integration layer provides a tiered caching strategy:</p>
 *
 * <ol>
 *   <li><b>Exact Match (Fast Path):</b> Check existing LLMCache for exact prompt match</li>
 *   <li><b>Semantic Match (Smart Path):</b> Check SemanticLLMCache for similar prompts</li>
 *   <li><b>Cache Miss (Slow Path):</b> Execute the LLM request</li>
 * </ol>
 *
 * <p>Both caches are updated on cache misses, ensuring gradual learning of
 * common query patterns.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * SemanticCacheIntegration integration = new SemanticCacheIntegration(llmCache);
 *
 * // Get with automatic fallback
 * Optional&lt;LLMResponse&gt; response = integration.get(
 *     prompt, model, providerId,
 *     () -> llmClient.sendAsync(prompt, params).get()
 * );
 * </pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe and can be used
 * concurrently from multiple threads.</p>
 *
 * @since 1.6.0
 */
public class SemanticCacheIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticCacheIntegration.class);

    private final LLMCache exactMatchCache;
    private final SemanticLLMCache semanticCache;
    private final boolean enabled;

    // Statistics for integration layer
    private volatile long exactMatchHits = 0;
    private volatile long semanticMatchHits = 0;
    private volatile long misses = 0;
    private volatile long llmCalls = 0;

    /**
     * Creates a new integration with default settings.
     *
     * @param exactMatchCache The existing exact-match LLM cache
     */
    public SemanticCacheIntegration(LLMCache exactMatchCache) {
        this(exactMatchCache, true, 0.85, 500, 5 * 60 * 1000);
    }

    /**
     * Creates a new integration with custom settings.
     *
     * @param exactMatchCache     The existing exact-match LLM cache
     * @param enabled             Whether semantic caching is enabled
     * @param similarityThreshold Minimum similarity for semantic hits
     * @param maxCacheSize        Maximum semantic cache size
     * @param maxAgeMs            Maximum entry age in milliseconds
     */
    public SemanticCacheIntegration(LLMCache exactMatchCache, boolean enabled,
                                    double similarityThreshold, int maxCacheSize,
                                    long maxAgeMs) {
        this.exactMatchCache = exactMatchCache;
        this.semanticCache = new SemanticLLMCache(
            new SimpleTextEmbedder(), similarityThreshold, maxCacheSize, maxAgeMs
        );
        this.enabled = enabled;

        LOGGER.info("Initialized SemanticCacheIntegration: enabled={}, threshold={}",
            enabled, similarityThreshold);
    }

    /**
     * Retrieves a cached response using tiered caching strategy.
     *
     * <p>Search order:</p>
     * <ol>
     *   <li>Exact match cache (fastest)</li>
     *   <li>Semantic cache (if enabled)</li>
     *   <li>Cache miss - returns empty</li>
     * </ol>
     *
     * @param prompt     The query prompt
     * @param model      The LLM model
     * @param providerId The LLM provider ID
     * @return Optional containing cached response, or empty if not found
     */
    public Optional<LLMResponse> get(String prompt, String model, String providerId) {
        // Tier 1: Exact match cache
        Optional<LLMResponse> exactResponse = exactMatchCache.get(prompt, model, providerId);
        if (exactResponse.isPresent()) {
            exactMatchHits++;
            LOGGER.debug("Exact match cache hit for prompt='{}'", truncate(prompt, 40));
            return exactResponse;
        }

        // Tier 2: Semantic cache (if enabled)
        if (enabled) {
            Optional<String> semanticResponse = semanticCache.get(prompt, model, providerId);
            if (semanticResponse.isPresent()) {
                semanticMatchHits++;

                // Convert string response to LLMResponse
                LLMResponse llmResponse = LLMResponse.builder()
                    .content(semanticResponse.get())
                    .model(model)
                    .providerId(providerId)
                    .tokensUsed(0) // tokens unknown from semantic cache
                    .fromCache(true)
                    .build();

                // Also populate exact cache for future fast access
                exactMatchCache.put(prompt, model, providerId, llmResponse);

                LOGGER.debug("Semantic cache hit for prompt='{}'", truncate(prompt, 40));
                return Optional.of(llmResponse);
            }
        }

        // Cache miss
        misses++;
        LOGGER.debug("Cache miss for prompt='{}'", truncate(prompt, 40));
        return Optional.empty();
    }

    /**
     * Gets a response, executing the supplier on cache miss.
     *
     * <p>This method provides a convenient get-or-compute pattern:</p>
     * <ul>
     *   <li>Check both caches</li>
     *   <li>If miss, execute supplier to get response</li>
     *   <li>Populate both caches with the response</li>
     * </ul>
     *
     * @param prompt     The query prompt
     * @param model      The LLM model
     * @param providerId The LLM provider ID
     * @param supplier   The supplier to execute on cache miss
     * @return The LLM response (cached or fresh)
     */
    public LLMResponse getOrCompute(String prompt, String model, String providerId,
                                     Supplier<LLMResponse> supplier) {
        // Try caches first
        Optional<LLMResponse> cached = get(prompt, model, providerId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Cache miss - execute LLM call
        llmCalls++;
        LOGGER.debug("Cache miss - executing LLM call for prompt='{}'", truncate(prompt, 40));

        LLMResponse response = supplier.get();

        // Populate both caches
        exactMatchCache.put(prompt, model, providerId, response);
        semanticCache.put(prompt, model, providerId, response.getContent(),
                         response.getTokensUsed());

        LOGGER.debug("Cached new response for prompt='{}', tokens={}",
            truncate(prompt, 40), response.getTokensUsed());

        return response;
    }

    /**
     * Puts a response into both caches.
     *
     * @param prompt     The prompt
     * @param model      The LLM model
     * @param providerId The LLM provider ID
     * @param response   The response to cache
     */
    public void put(String prompt, String model, String providerId, LLMResponse response) {
        exactMatchCache.put(prompt, model, providerId, response);
        semanticCache.put(prompt, model, providerId, response.getContent(),
                         response.getTokensUsed());
    }

    /**
     * Clears both caches.
     */
    public void clear() {
        exactMatchCache.clear();
        semanticCache.clear();
        resetStats();
        LOGGER.info("Cleared both exact and semantic caches");
    }

    /**
     * Resets statistics counters.
     */
    public void resetStats() {
        exactMatchHits = 0;
        semanticMatchHits = 0;
        misses = 0;
        llmCalls = 0;
    }

    /**
     * Returns whether semantic caching is enabled.
     *
     * @return true if semantic caching is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the exact-match cache.
     *
     * @return The exact-match cache
     */
    public LLMCache getExactMatchCache() {
        return exactMatchCache;
    }

    /**
     * Returns the semantic cache.
     *
     * @return The semantic cache
     */
    public SemanticLLMCache getSemanticCache() {
        return semanticCache;
    }

    /**
     * Returns integration statistics.
     *
     * @return Integration stats
     */
    public IntegrationStats getStats() {
        long totalRequests = exactMatchHits + semanticMatchHits + misses;
        double hitRate = totalRequests > 0
            ? (double) (exactMatchHits + semanticMatchHits) / totalRequests
            : 0.0;
        double semanticContribution = (exactMatchHits + semanticMatchHits) > 0
            ? (double) semanticMatchHits / (exactMatchHits + semanticMatchHits)
            : 0.0;

        return new IntegrationStats(
            exactMatchHits,
            semanticMatchHits,
            misses,
            llmCalls,
            hitRate,
            semanticContribution,
            semanticCache.size(),
            exactMatchCache.size()
        );
    }

    /**
     * Logs current statistics.
     */
    public void logStats() {
        IntegrationStats stats = getStats();
        LOGGER.info(
            "SemanticCacheIntegration - Exact hits: {}, Semantic hits: {}, " +
            "Misses: {}, LLM calls: {}, Hit rate: {:.2f}%, Semantic contrib: {:.2f}%",
            stats.exactMatchHits,
            stats.semanticMatchHits,
            stats.misses,
            stats.llmCalls,
            stats.hitRate * 100,
            stats.semanticContribution * 100
        );

        // Log semantic cache stats separately
        semanticCache.logStats();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Statistics for the integration layer.
     */
    public static class IntegrationStats {
        public final long exactMatchHits;
        public final long semanticMatchHits;
        public final long misses;
        public final long llmCalls;
        public final double hitRate;
        public final double semanticContribution;
        public final int semanticCacheSize;
        public final long exactCacheSize;

        public IntegrationStats(long exactMatchHits, long semanticMatchHits,
                               long misses, long llmCalls, double hitRate,
                               double semanticContribution, int semanticCacheSize,
                               long exactCacheSize) {
            this.exactMatchHits = exactMatchHits;
            this.semanticMatchHits = semanticMatchHits;
            this.misses = misses;
            this.llmCalls = llmCalls;
            this.hitRate = hitRate;
            this.semanticContribution = semanticContribution;
            this.semanticCacheSize = semanticCacheSize;
            this.exactCacheSize = exactCacheSize;
        }

        @Override
        public String toString() {
            return String.format(
                "IntegrationStats[exactHits=%d, semanticHits=%d, misses=%d, " +
                "llmCalls=%d, hitRate=%.2f%%, semanticContrib=%.2f%%, " +
                "semanticSize=%d, exactSize=%d]",
                exactMatchHits, semanticMatchHits, misses, llmCalls,
                hitRate * 100, semanticContribution * 100,
                semanticCacheSize, exactCacheSize
            );
        }
    }
}
