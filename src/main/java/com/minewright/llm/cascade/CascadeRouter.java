package com.minewright.llm.cascade;

import com.minewright.llm.async.LLMCache;
import com.minewright.llm.async.LLMResponse;
import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.config.MineWrightConfig;
import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Intelligent router for LLM requests based on task complexity.
 *
 * <p><b>Routing Strategy:</b></p>
 * <ol>
 *   <li>Check cache first (for cacheable tasks)</li>
 *   <li>Analyze command complexity</li>
 *   <li>Select appropriate tier based on complexity</li>
 *   <li>Execute request with fallback on failure</li>
 *   <li>Log routing decision for analytics</li>
 * </ol>
 *
 * <p><b>Design Inspiration:</b> SmartCRDT/Aequor cascade routing patterns</p>
 * <p><b>Thread Safety:</b> All operations are thread-safe</p>
 *
 * @since 1.6.0
 */
public class CascadeRouter {

    private static final Logger LOGGER = TestLogger.getLogger(CascadeRouter.class);

    // ------------------------------------------------------------------------
    // Dependencies
    // ------------------------------------------------------------------------

    private final LLMCache cache;
    private final ComplexityAnalyzer analyzer;
    private final CascadeConfig config;
    private final Map<LLMTier, AsyncLLMClient> clients;

    // ------------------------------------------------------------------------
    // Metrics
    // ------------------------------------------------------------------------

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong fallbacks = new AtomicLong(0);
    private final AtomicLong failures = new AtomicLong(0);
    private final Map<LLMTier, AtomicLong> tierUsage = new ConcurrentHashMap<>();
    private final Map<LLMTier, AtomicDouble> tierCosts = new ConcurrentHashMap<>();
    private final List<RoutingDecision> recentDecisions = new ArrayList<>();

    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    private static final int MAX_RECENT_DECISIONS = 100;
    private static final int MAX_ESCALATION_ATTEMPTS = 3;

    /**
     * Creates a new CascadeRouter with the specified dependencies.
     *
     * @param cache LLM response cache
     * @param analyzer Task complexity analyzer
     * @param clients Map of tier to async clients
     */
    public CascadeRouter(LLMCache cache, ComplexityAnalyzer analyzer,
                         Map<LLMTier, AsyncLLMClient> clients) {
        this.cache = cache;
        this.analyzer = analyzer;
        this.clients = Map.copyOf(clients);
        this.config = CascadeConfig.getInstance();

        // Initialize metrics
        for (LLMTier tier : LLMTier.values()) {
            tierUsage.put(tier, new AtomicLong(0));
            tierCosts.put(tier, new AtomicDouble(0.0));
        }

        LOGGER.info("CascadeRouter initialized with config: {}", config.getSummary());
    }

    /**
     * Routes an LLM request to the appropriate tier based on complexity.
     *
     * <p><b>Routing Flow:</b></p>
     * <ol>
     *   <li>Check cache (if enabled and complexity allows)</li>
     *   <li>Analyze command complexity</li>
     *   <li>Select initial tier based on complexity</li>
     *   <li>Execute with fallback chain on failure</li>
     *   <li>Track metrics and log decision</li>
     * </ol>
     *
     * @param command The user command
     * @param context Additional context (foreman, world knowledge, etc.)
     * @return CompletableFuture with the LLM response
     */
    public CompletableFuture<LLMResponse> route(String command, Map<String, Object> context) {
        totalRequests.incrementAndGet();

        // Step 1: Check cache first (if enabled)
        if (config.isCachingEnabled()) {
            Optional<LLMResponse> cached = checkCache(command, context);
            if (cached.isPresent()) {
                return CompletableFuture.completedFuture(cached.get());
            }
        }

        // Step 2: Analyze complexity
        TaskComplexity complexity = analyzeComplexity(command, context);

        // Step 3: Select initial tier
        LLMTier selectedTier = selectTier(complexity);

        // Step 4: Execute with fallback
        return executeWithFallback(command, context, complexity, selectedTier, 0);
    }

    /**
     * Checks cache for a potential hit.
     *
     * @param command The command
     * @param context Request context
     * @return Cached response if present
     */
    private Optional<LLMResponse> checkCache(String command, Map<String, Object> context) {
        // Get model from context, or try config, or use default
        String model;
        if (context.containsKey("model")) {
            model = (String) context.get("model");
        } else {
            try {
                model = MineWrightConfig.OPENAI_MODEL.get();
            } catch (IllegalStateException e) {
                // Config not loaded (e.g., during tests), use default
                model = "gpt-4";
            }
        }
        String providerId = (String) context.getOrDefault("providerId", "cascade");

        Optional<LLMResponse> cached = cache.get(command, model, providerId);
        if (cached.isPresent()) {
            cacheHits.incrementAndGet();
            tierUsage.get(LLMTier.CACHE).incrementAndGet();

            // Log cache hit decision
            RoutingDecision decision = RoutingDecision.cacheHit(command,
                TaskComplexity.TRIVIAL, System.currentTimeMillis() - 1);

            LOGGER.debug("[Cascade] Cache hit for command: {}", truncate(command));
            recordDecision(decision);
        }

        return cached;
    }

    /**
     * Analyzes command to determine complexity level.
     *
     * @param command The command
     * @param context Request context
     * @return Task complexity
     */
    private TaskComplexity analyzeComplexity(String command, Map<String, Object> context) {
        // Get foreman entity from context if available
        Object foreman = context.get("foreman");

        // Get world knowledge from context if available
        Object worldKnowledge = context.get("worldKnowledge");

        // Analyze complexity (with simplified context if entities not available)
        if (foreman instanceof com.minewright.entity.ForemanEntity &&
            worldKnowledge instanceof com.minewright.memory.WorldKnowledge) {
            return analyzer.analyze(command,
                (com.minewright.entity.ForemanEntity) foreman,
                (com.minewright.memory.WorldKnowledge) worldKnowledge);
        }

        // Simplified analysis without entity context
        return analyzer.analyze(command, null, null);
    }

    /**
     * Selects the appropriate LLM tier for a given complexity level.
     *
     * @param complexity Task complexity
     * @return Recommended LLM tier
     */
    public LLMTier selectTier(TaskComplexity complexity) {
        LLMTier tier = config.getTierForComplexity(complexity);

        // Ensure selected tier is available
        if (!config.isTierAvailable(tier)) {
            LOGGER.warn("[Cascade] Selected tier {} not available, finding alternative", tier);
            tier = findNextAvailableTier(tier);
        }

        return tier;
    }

    /**
     * Executes a request with automatic fallback on failure.
     *
     * @param command The command
     * @param context Request context
     * @param complexity Task complexity
     * @param currentTier Current tier to try
     * @param attempt Current attempt number
     * @return CompletableFuture with the response
     */
    private CompletableFuture<LLMResponse> executeWithFallback(
        String command,
        Map<String, Object> context,
        TaskComplexity complexity,
        LLMTier currentTier,
        int attempt
    ) {
        // Check escalation limit
        if (attempt >= MAX_ESCALATION_ATTEMPTS) {
            failures.incrementAndGet();
            RoutingDecision decision = RoutingDecision.failure(
                command, complexity, currentTier, currentTier,
                0, "Max escalation attempts exceeded"
            );
            recordDecision(decision);
            return CompletableFuture.failedFuture(new RuntimeException("Max escalation attempts exceeded"));
        }

        // Get client for current tier
        AsyncLLMClient client = clients.get(currentTier);
        if (client == null) {
            // Tier not available, try fallback
            return tryFallbackTier(command, context, complexity, currentTier, attempt);
        }

        long startTime = System.currentTimeMillis();
        tierUsage.get(currentTier).incrementAndGet();

        return client.sendAsync(command, context)
            .thenApply(response -> {
                // Success - track metrics and log decision
                long latency = System.currentTimeMillis() - startTime;
                double cost = currentTier.estimateCost(response.getTokensUsed());
                tierCosts.get(currentTier).addAndGet(cost);

                // Store in cache if applicable
                if (config.isCachingEnabled() && complexity.isCacheable()) {
                    String model;
                    if (context.containsKey("model")) {
                        model = (String) context.get("model");
                    } else {
                        try {
                            model = MineWrightConfig.OPENAI_MODEL.get();
                        } catch (IllegalStateException e) {
                            // Config not loaded (e.g., during tests), use default
                            model = "gpt-4";
                        }
                    }
                    cache.put(command, model, currentTier.getTierId(), response);
                }

                RoutingDecision decision = new RoutingDecision(
                    command, complexity, currentTier, currentTier,
                    latency, response.getTokensUsed(), true
                );
                recordDecision(decision);

                LOGGER.info("[Cascade] Success: {} via {} ({}ms, ${:.5f})",
                    truncate(command), currentTier, latency, cost);

                return response;
            })
            .exceptionally(throwable -> {
                // Failure - try fallback tier
                LOGGER.warn("[Cascade] Tier {} failed: {}",
                    currentTier, throwable.getMessage());
                return tryFallbackTier(command, context, complexity, currentTier, attempt).join();
            });
    }

    /**
     * Attempts to use the next tier in the fallback chain.
     *
     * @param command The command
     * @param context Request context
     * @param complexity Task complexity
     * @param failedTier The tier that failed
     * @param attempt Current attempt number
     * @return CompletableFuture with response from fallback tier
     */
    private CompletableFuture<LLMResponse> tryFallbackTier(
        String command,
        Map<String, Object> context,
        TaskComplexity complexity,
        LLMTier failedTier,
        int attempt
    ) {
        fallbacks.incrementAndGet();

        List<LLMTier> fallbackChain = config.getFallbackChain(failedTier);
        if (fallbackChain.isEmpty()) {
            // No fallback available
            failures.incrementAndGet();
            RoutingDecision decision = RoutingDecision.failure(
                command, complexity, failedTier, failedTier,
                0, "No fallback available"
            );
            recordDecision(decision);
            return CompletableFuture.failedFuture(
                new RuntimeException("Tier " + failedTier + " failed and no fallback available"));
        }

        // Find first available fallback tier
        Optional<LLMTier> nextTier = config.getFirstAvailableTier(fallbackChain);
        if (nextTier.isEmpty()) {
            failures.incrementAndGet();
            RoutingDecision decision = RoutingDecision.failure(
                command, complexity, failedTier, failedTier,
                0, "No available fallback tiers"
            );
            recordDecision(decision);
            return CompletableFuture.failedFuture(
                new RuntimeException("No available fallback tiers for " + failedTier));
        }

        LOGGER.info("[Cascade] Falling back from {} to {}",
            failedTier, nextTier.get());

        return executeWithFallback(command, context, complexity, nextTier.get(), attempt + 1);
    }

    /**
     * Finds the next available tier after the given tier.
     *
     * @param tier Starting tier
     * @return Next available tier
     */
    private LLMTier findNextAvailableTier(LLMTier tier) {
        LLMTier next = tier.nextHigherTier();
        while (next != tier && !config.isTierAvailable(next)) {
            next = next.nextHigherTier();
        }
        return next;
    }

    /**
     * Records a routing decision for analytics.
     *
     * @param decision The routing decision
     */
    private void recordDecision(RoutingDecision decision) {
        synchronized (recentDecisions) {
            recentDecisions.add(decision);
            while (recentDecisions.size() > MAX_RECENT_DECISIONS) {
                recentDecisions.remove(0);
            }
        }
    }

    /**
     * Returns recent routing decisions for monitoring.
     *
     * @return List of recent decisions
     */
    public List<RoutingDecision> getRecentDecisions() {
        synchronized (recentDecisions) {
            return List.copyOf(recentDecisions);
        }
    }

    // ------------------------------------------------------------------------
    // Metrics and Monitoring
    // ------------------------------------------------------------------------

    /**
     * Returns the total number of requests routed.
     *
     * @return Total request count
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Returns the number of cache hits.
     *
     * @return Cache hit count
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * Returns the cache hit rate.
     *
     * @return Cache hit rate (0.0 to 1.0)
     */
    public double getCacheHitRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }

    /**
     * Returns the number of fallbacks that occurred.
     *
     * @return Fallback count
     */
    public long getFallbacks() {
        return fallbacks.get();
    }

    /**
     * Returns the number of failed requests.
     *
     * @return Failure count
     */
    public long getFailures() {
        return failures.get();
    }

    /**
     * Returns the usage count for a specific tier.
     *
     * @param tier LLM tier
     * @return Usage count
     */
    public long getTierUsage(LLMTier tier) {
        return tierUsage.getOrDefault(tier, new AtomicLong(0)).get();
    }

    /**
     * Returns the accumulated cost for a specific tier.
     *
     * @param tier LLM tier
     * @return Accumulated cost in USD
     */
    public double getTierCost(LLMTier tier) {
        return tierCosts.getOrDefault(tier, new AtomicDouble(0.0)).get();
    }

    /**
     * Returns the total accumulated cost across all tiers.
     *
     * @return Total cost in USD
     */
    public double getTotalCost() {
        return tierCosts.values().stream()
            .mapToDouble(AtomicDouble::get)
            .sum();
    }

    /**
     * Logs current routing statistics.
     */
    public void logStats() {
        LOGGER.info("=== Cascade Router Statistics ===");
        LOGGER.info("Total Requests: {}", getTotalRequests());
        LOGGER.info("Cache Hits: {} ({:.2f}%)", getCacheHits(), getCacheHitRate() * 100);
        LOGGER.info("Fallbacks: {}", getFallbacks());
        LOGGER.info("Failures: {}", getFailures());
        LOGGER.info("Total Cost: ${:.5f}", getTotalCost());

        LOGGER.info("Tier Usage:");
        for (LLMTier tier : LLMTier.values()) {
            if (tier.isAvailable()) {
                LOGGER.info("  {}: {} requests, ${:.5f}",
                    tier, getTierUsage(tier), getTierCost(tier));
            }
        }
    }

    /**
     * Resets all metrics.
     */
    public void resetMetrics() {
        totalRequests.set(0);
        cacheHits.set(0);
        fallbacks.set(0);
        failures.set(0);

        for (LLMTier tier : LLMTier.values()) {
            tierUsage.get(tier).set(0);
            tierCosts.get(tier).set(0.0);
        }

        synchronized (recentDecisions) {
            recentDecisions.clear();
        }

        LOGGER.info("Cascade router metrics reset");
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Truncates a string for logging.
     *
     * @param str String to truncate
     * @return Truncated string
     */
    private static String truncate(String str) {
        if (str == null) {
            return "null";
        }
        return str.length() > 50 ? str.substring(0, 47) + "..." : str;
    }

    /**
     * Thread-safe double wrapper for atomic operations.
     */
    private static class AtomicDouble {
        private volatile double value;

        AtomicDouble(double initialValue) {
            this.value = initialValue;
        }

        double get() {
            return value;
        }

        void set(double newValue) {
            value = newValue;
        }

        void addAndGet(double delta) {
            value += delta;
        }
    }
}
