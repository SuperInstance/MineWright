package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for multi-agent coordination features.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [multi_agent]}</b> - Coordination and communication settings</p>
 *
 * @since 3.0.0
 */
public class MultiAgentConfig {
    private static final Logger LOGGER = TestLogger.getLogger(MultiAgentConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Multi-Agent Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable multi-agent coordination features.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code multi_agent.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue MULTI_AGENT_ENABLED;

    /**
     * Maximum time to wait for agent bids in milliseconds.
     * <p><b>Range:</b> 100 to 5000</p>
     * <p><b>Default:</b> 1000</p>
     * <p><b>Config key:</b> {@code multi_agent.max_bid_wait_ms}</p>
     */
    public static final ForgeConfigSpec.IntValue MAX_BID_WAIT_MS;

    /**
     * Time-to-live for blackboard entries in seconds.
     * <p><b>Range:</b> 60 to 3600</p>
     * <p><b>Default:</b> 300</p>
     * <p><b>Config key:</b> {@code multi_agent.blackboard_ttl_seconds}</p>
     */
    public static final ForgeConfigSpec.IntValue BLACKBOARD_TTL_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Multi-Agent Configuration (Coordination features)").push("multi_agent");

        MULTI_AGENT_ENABLED = builder
            .comment("Enable multi-agent coordination features",
                     "When enabled, agents can collaborate and coordinate tasks",
                     "When disabled, each agent operates independently")
            .define("enabled", true);

        MAX_BID_WAIT_MS = builder
            .comment("Maximum time to wait for agent bids in milliseconds",
                     "Lower = faster coordination but may miss capable agents",
                     "Recommended: 1000 for balanced performance")
            .defineInRange("max_bid_wait_ms", 1000, 100, 5000);

        BLACKBOARD_TTL_SECONDS = builder
            .comment("Time-to-live for blackboard entries in seconds",
                     "Lower = less stale data but more frequent updates",
                     "Recommended: 300 (5 minutes)")
            .defineInRange("blackboard_ttl_seconds", 300, 60, 3600);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the multi-agent configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating multi-agent configuration...");

        if (MULTI_AGENT_ENABLED.get()) {
            int maxBidWait = MAX_BID_WAIT_MS.get();
            int ttl = BLACKBOARD_TTL_SECONDS.get();

            if (maxBidWait < 100 || maxBidWait > 5000) {
                LOGGER.warn("Max bid wait out of range: {}. Should be 100-5000ms", maxBidWait);
            }
            if (ttl < 60 || ttl > 3600) {
                LOGGER.warn("Blackboard TTL out of range: {}. Should be 60-3600 seconds", ttl);
            }

            LOGGER.info("Multi-Agent: enabled (max_bid_wait: {}ms, blackboard_ttl: {}s)",
                maxBidWait, ttl);
        } else {
            LOGGER.info("Multi-Agent: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current multi-agent configuration
     */
    public static String getConfigSummary() {
        if (!MULTI_AGENT_ENABLED.get()) {
            return "MultiAgentConfig[disabled]";
        }
        return String.format(
            "MultiAgentConfig[enabled, maxBidWait=%dms, blackboardTTL=%ds]",
            MAX_BID_WAIT_MS.get(),
            BLACKBOARD_TTL_SECONDS.get()
        );
    }
}
