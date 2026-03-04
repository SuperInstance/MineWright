package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for pathfinding and navigation algorithms.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [pathfinding]}</b> - Navigation algorithm settings</p>
 *
 * @since 3.0.0
 */
public class PathfindingConfig {
    private static final Logger LOGGER = TestLogger.getLogger(PathfindingConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Pathfinding Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable enhanced pathfinding algorithms.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code pathfinding.enhanced}</p>
     */
    public static final ForgeConfigSpec.BooleanValue ENHANCED_PATHFINDING;

    /**
     * Maximum nodes to search in pathfinding algorithms.
     * <p><b>Range:</b> 1000 to 50000</p>
     * <p><b>Default:</b> 10000</p>
     * <p><b>Config key:</b> {@code pathfinding.max_search_nodes}</p>
     */
    public static final ForgeConfigSpec.IntValue MAX_PATH_SEARCH_NODES;

    /**
     * Maximum nodes to explore before giving up (prevents infinite loops).
     * <p><b>Range:</b> 1000 to 50000</p>
     * <p><b>Default:</b> 10000</p>
     * <p><b>Config key:</b> {@code pathfinding.max_nodes}</p>
     */
    public static final ForgeConfigSpec.IntValue PATHFINDING_MAX_NODES;

    /**
     * Enable path caching for frequently traversed routes.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code pathfinding.cache_enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue PATHFINDING_CACHE_ENABLED;

    /**
     * Maximum number of cached paths.
     * <p><b>Range:</b> 10 to 1000</p>
     * <p><b>Default:</b> 100</p>
     * <p><b>Config key:</b> {@code pathfinding.cache_max_size}</p>
     */
    public static final ForgeConfigSpec.IntValue PATHFINDING_CACHE_MAX_SIZE;

    /**
     * Path cache TTL in minutes.
     * <p><b>Range:</b> 1 to 60</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code pathfinding.cache_ttl_minutes}</p>
     */
    public static final ForgeConfigSpec.IntValue PATHFINDING_CACHE_TTL_MINUTES;

    /**
     * Pathfinding timeout threshold in milliseconds.
     * <p><b>Range:</b> 100 to 10000</p>
     * <p><b>Default:</b> 2000</p>
     * <p><b>Config key:</b> {@code performance.pathfindingTimeoutMs}</p>
     */
    public static final ForgeConfigSpec.IntValue PATHFINDING_TIMEOUT_MS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Pathfinding Configuration (Navigation algorithms)").push("pathfinding");

        ENHANCED_PATHFINDING = builder
            .comment("Enable enhanced pathfinding algorithms",
                     "When enabled, uses advanced pathfinding with obstacle avoidance",
                     "When disabled, uses basic pathfinding")
            .define("enhanced", true);

        MAX_PATH_SEARCH_NODES = builder
            .comment("Maximum nodes to search in pathfinding algorithms",
                     "Higher = can find longer paths but uses more CPU",
                     "Recommended: 10000 for balanced performance")
            .defineInRange("max_search_nodes", 10000, 1000, 50000);

        PATHFINDING_MAX_NODES = builder
            .comment("A* pathfinder maximum node exploration limit",
                     "Higher = can find longer paths but uses more CPU",
                     "Recommended: 10000 for balanced performance",
                     "Lower = faster pathfinding but may fail to find long paths")
            .defineInRange("max_nodes", 10000, 1000, 50000);

        PATHFINDING_CACHE_ENABLED = builder
            .comment("Enable path caching for frequently traversed routes",
                     "When enabled, caches successful paths to reduce CPU overhead",
                     "Significantly improves performance for repeated routes",
                     "When disabled, computes all paths from scratch")
            .define("cache_enabled", true);

        PATHFINDING_CACHE_MAX_SIZE = builder
            .comment("Maximum number of cached paths",
                     "Higher = more cache hits but more memory usage",
                     "Each cached path ~1-5KB depending on length",
                     "Recommended: 100 for balanced performance")
            .defineInRange("cache_max_size", 100, 10, 1000);

        PATHFINDING_CACHE_TTL_MINUTES = builder
            .comment("Path cache time-to-live in minutes",
                     "Cached paths older than this are evicted",
                     "Lower = fresher paths but more cache misses",
                     "Higher = more cache hits but potentially stale routes",
                     "Recommended: 10 minutes for typical gameplay")
            .defineInRange("cache_ttl_minutes", 10, 1, 60);

        PATHFINDING_TIMEOUT_MS = builder
            .comment("Pathfinding timeout threshold in milliseconds",
                     "Pathfinding operations exceeding this timeout are aborted",
                     "Higher = can find longer paths but may cause lag",
                     "Lower = faster abort but may fail to find paths",
                     "Recommended: 2000 for balanced performance")
            .defineInRange("pathfindingTimeoutMs", 2000, 100, 10000);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the pathfinding configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating pathfinding configuration...");

        if (ENHANCED_PATHFINDING.get()) {
            int maxNodes = MAX_PATH_SEARCH_NODES.get();
            int timeout = PATHFINDING_TIMEOUT_MS.get();
            if (maxNodes < 1000 || maxNodes > 50000) {
                LOGGER.warn("Max path search nodes out of range: {}. Should be 1000-50000", maxNodes);
            }
            if (timeout < 100 || timeout > 10000) {
                LOGGER.warn("Pathfinding timeout out of range: {}. Should be 100-10000ms", timeout);
            }
            LOGGER.info("Pathfinding: enhanced (max_search_nodes: {}, timeout: {}ms)", maxNodes, timeout);
        } else {
            LOGGER.info("Pathfinding: basic");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current pathfinding configuration
     */
    public static String getConfigSummary() {
        if (!ENHANCED_PATHFINDING.get()) {
            return "PathfindingConfig[basic]";
        }
        return String.format(
            "PathfindingConfig[enhanced, maxNodes=%d, cache=%s, timeout=%dms]",
            PATHFINDING_MAX_NODES.get(),
            PATHFINDING_CACHE_ENABLED.get() ? "enabled" : "disabled",
            PATHFINDING_TIMEOUT_MS.get()
        );
    }
}
