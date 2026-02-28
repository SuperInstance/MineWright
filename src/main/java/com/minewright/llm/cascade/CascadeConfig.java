package com.minewright.llm.cascade;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import com.minewright.config.MineWrightConfig;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for cascade routing system.
 *
 * <p>Controls tier availability, cost limits, fallback chains, and routing rules.
 * Loaded from MineWright configuration with cascade-specific defaults.</p>
 *
 * <p><b>Configuration Sections:</b></p>
 * <ul>
 *   <li><b>Tier Availability:</b> Which tiers are enabled</li>
 *   <li><b>Cost Limits:</b> Maximum cost per tier per session</li>
 *   <li><b>Fallback Chains:</b> Tier escalation on failure</li>
 *   <li><b>Routing Rules:</b> Complexity-to-tier mappings</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Immutable after initialization, thread-safe</p>
 *
 * @since 1.6.0
 */
public class CascadeConfig {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(CascadeConfig.class);


    /**
     * Default mapping from task complexity to LLM tier.
     */
    private static final Map<TaskComplexity, LLMTier> DEFAULT_COMPLEXITY_TIERS;

    static {
        DEFAULT_COMPLEXITY_TIERS = new EnumMap<>(TaskComplexity.class);
        DEFAULT_COMPLEXITY_TIERS.put(TaskComplexity.TRIVIAL, LLMTier.CACHE);
        DEFAULT_COMPLEXITY_TIERS.put(TaskComplexity.SIMPLE, LLMTier.FAST);
        DEFAULT_COMPLEXITY_TIERS.put(TaskComplexity.MODERATE, LLMTier.BALANCED);
        DEFAULT_COMPLEXITY_TIERS.put(TaskComplexity.COMPLEX, LLMTier.SMART);
        DEFAULT_COMPLEXITY_TIERS.put(TaskComplexity.NOVEL, LLMTier.SMART);
    }

    /**
     * Default fallback chain (tier to try next on failure).
     */
    private static final Map<LLMTier, List<LLMTier>> DEFAULT_FALLBACK_CHAINS;

    static {
        DEFAULT_FALLBACK_CHAINS = new EnumMap<>(LLMTier.class);
        DEFAULT_FALLBACK_CHAINS.put(LLMTier.CACHE, Arrays.asList(LLMTier.FAST));
        DEFAULT_FALLBACK_CHAINS.put(LLMTier.LOCAL, Arrays.asList(LLMTier.FAST));
        DEFAULT_FALLBACK_CHAINS.put(LLMTier.FAST, Arrays.asList(LLMTier.BALANCED, LLMTier.SMART));
        DEFAULT_FALLBACK_CHAINS.put(LLMTier.BALANCED, Arrays.asList(LLMTier.SMART));
        DEFAULT_FALLBACK_CHAINS.put(LLMTier.SMART, Arrays.asList()); // No fallback for SMART
    }

    // ------------------------------------------------------------------------
    // Configuration State
    // ------------------------------------------------------------------------

    private final Map<TaskComplexity, LLMTier> complexityTierMap;
    private final Map<LLMTier, List<LLMTier>> fallbackChains;
    private final Map<LLMTier, Double> costLimits;
    private final Map<LLMTier, Boolean> tierAvailability;
    private final boolean enableCaching;
    private final boolean enableCostTracking;
    private final int maxCacheSize;
    private final long cacheTtlMs;
    private final boolean enableAutoEscalation;
    private final int maxEscalationAttempts;

    // ------------------------------------------------------------------------
    // Singleton Instance
    // ------------------------------------------------------------------------

    private static volatile CascadeConfig instance;

    /**
     * Gets the singleton CascadeConfig instance.
     *
     * @return The cascade configuration
     */
    public static CascadeConfig getInstance() {
        if (instance == null) {
            synchronized (CascadeConfig.class) {
                if (instance == null) {
                    instance = loadConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Reloads configuration from MineWright config.
     */
    public static void reload() {
        synchronized (CascadeConfig.class) {
            instance = loadConfig();
            LOGGER.info("Cascade configuration reloaded");
        }
    }

    /**
     * Loads configuration from MineWright settings.
     */
    private static CascadeConfig loadConfig() {
        LOGGER.info("Loading cascade routing configuration");

        // Build complexity tier map (currently using defaults)
        Map<TaskComplexity, LLMTier> complexityMap = new EnumMap<>(DEFAULT_COMPLEXITY_TIERS);

        // Build fallback chains (currently using defaults)
        Map<LLMTier, List<LLMTier>> fallbacks = new EnumMap<>(DEFAULT_FALLBACK_CHAINS);

        // Build cost limits (per tier, per session in USD)
        Map<LLMTier, Double> limits = new EnumMap<>(LLMTier.class);
        limits.put(LLMTier.CACHE, 0.0);
        limits.put(LLMTier.LOCAL, 0.0);
        limits.put(LLMTier.FAST, 0.50);  // $0.50 per session for FAST tier
        limits.put(LLMTier.BALANCED, 2.00); // $2.00 per session for BALANCED tier
        limits.put(LLMTier.SMART, 10.00);  // $10.00 per session for SMART tier

        // Build tier availability (all tiers available by default)
        Map<LLMTier, Boolean> availability = new EnumMap<>(LLMTier.class);
        availability.put(LLMTier.CACHE, true);
        availability.put(LLMTier.LOCAL, false); // LOCAL not yet implemented
        availability.put(LLMTier.FAST, true);
        availability.put(LLMTier.BALANCED, true);
        availability.put(LLMTier.SMART, true);

        return new CascadeConfig(
            complexityMap,
            fallbacks,
            limits,
            availability,
            true,  // enable caching
            true,  // enable cost tracking
            500,   // max cache size
            5 * 60 * 1000, // cache TTL: 5 minutes
            true,  // enable auto-escalation on failure
            2      // max escalation attempts
        );
    }

    /**
     * Private constructor for configuration.
     */
    private CascadeConfig(
        Map<TaskComplexity, LLMTier> complexityTierMap,
        Map<LLMTier, List<LLMTier>> fallbackChains,
        Map<LLMTier, Double> costLimits,
        Map<LLMTier, Boolean> tierAvailability,
        boolean enableCaching,
        boolean enableCostTracking,
        int maxCacheSize,
        long cacheTtlMs,
        boolean enableAutoEscalation,
        int maxEscalationAttempts
    ) {
        this.complexityTierMap = Map.copyOf(complexityTierMap);
        this.fallbackChains = Map.copyOf(fallbackChains);
        this.costLimits = Map.copyOf(costLimits);
        this.tierAvailability = Map.copyOf(tierAvailability);
        this.enableCaching = enableCaching;
        this.enableCostTracking = enableCostTracking;
        this.maxCacheSize = maxCacheSize;
        this.cacheTtlMs = cacheTtlMs;
        this.enableAutoEscalation = enableAutoEscalation;
        this.maxEscalationAttempts = maxEscalationAttempts;
    }

    // ------------------------------------------------------------------------
    // Configuration Accessors
    // ------------------------------------------------------------------------

    /**
     * Returns the LLM tier for a given complexity level.
     *
     * @param complexity Task complexity
     * @return Recommended LLM tier
     */
    public LLMTier getTierForComplexity(TaskComplexity complexity) {
        return complexityTierMap.getOrDefault(complexity, LLMTier.BALANCED);
    }

    /**
     * Returns the fallback chain for a given tier.
     *
     * @param tier Starting tier
     * @return List of tiers to try in order (may be empty)
     */
    public List<LLMTier> getFallbackChain(LLMTier tier) {
        return fallbackChains.getOrDefault(tier, List.of());
    }

    /**
     * Returns the cost limit for a given tier.
     *
     * @param tier LLM tier
     * @return Maximum cost per session in USD
     */
    public double getCostLimit(LLMTier tier) {
        return costLimits.getOrDefault(tier, 0.0);
    }

    /**
     * Checks if a tier is available for use.
     *
     * @param tier LLM tier
     * @return true if tier is enabled and available
     */
    public boolean isTierAvailable(LLMTier tier) {
        return tierAvailability.getOrDefault(tier, tier.isAvailable());
    }

    /**
     * Checks if caching is enabled.
     *
     * @return true if caching is enabled
     */
    public boolean isCachingEnabled() {
        return enableCaching;
    }

    /**
     * Checks if cost tracking is enabled.
     *
     * @return true if cost tracking is enabled
     */
    public boolean isCostTrackingEnabled() {
        return enableCostTracking;
    }

    /**
     * Returns the maximum cache size.
     *
     * @return Maximum number of cache entries
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Returns the cache TTL in milliseconds.
     *
     * @return Cache time-to-live in milliseconds
     */
    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    /**
     * Checks if auto-escalation is enabled on failure.
     *
     * @return true if auto-escalation is enabled
     */
    public boolean isAutoEscalationEnabled() {
        return enableAutoEscalation;
    }

    /**
     * Returns the maximum number of escalation attempts.
     *
     * @return Maximum escalation attempts
     */
    public int getMaxEscalationAttempts() {
        return maxEscalationAttempts;
    }

    /**
     * Returns the first available tier from a list.
     * <p>Useful for finding a valid fallback tier.</p>
     *
     * @param tiers List of tiers to check (in order)
     * @return First available tier, or empty if none available
     */
    public java.util.Optional<LLMTier> getFirstAvailableTier(List<LLMTier> tiers) {
        return tiers.stream()
            .filter(this::isTierAvailable)
            .findFirst();
    }

    /**
     * Returns configuration summary for logging.
     *
     * @return Configuration summary string
     */
    public String getSummary() {
        return String.format(
            "CascadeConfig[caching=%s, costTracking=%s, autoEscalate=%s, maxEscalations=%d, " +
            "cacheSize=%d, cacheTtl=%dms]",
            enableCaching,
            enableCostTracking,
            enableAutoEscalation,
            maxEscalationAttempts,
            maxCacheSize,
            cacheTtlMs
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CascadeConfig{\n");
        sb.append("  complexityTierMap={\n");
        for (Map.Entry<TaskComplexity, LLMTier> entry : complexityTierMap.entrySet()) {
            sb.append(String.format("    %s -> %s\n", entry.getKey(), entry.getValue()));
        }
        sb.append("  },\n");
        sb.append("  tierAvailability=").append(tierAvailability).append(",\n");
        sb.append("  enableCaching=").append(enableCaching).append(",\n");
        sb.append("  enableCostTracking=").append(enableCostTracking).append(",\n");
        sb.append("  enableAutoEscalation=").append(enableAutoEscalation).append(",\n");
        sb.append("  maxEscalationAttempts=").append(maxEscalationAttempts).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
