package com.minewright.config;

import com.minewright.testutil.TestLogger;
import com.minewright.exception.ConfigException;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for Hive Mind (Cloudflare Edge) distributed AI system.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [hivemind]}</b> - Cloudflare Edge integration settings</p>
 *
 * <h2>Overview</h2>
 * <p>Hive Mind enables distributed AI with Cloudflare edge workers for tactical reflexes.
 * When enabled, agents get sub-20ms combat/hazard responses from edge workers.</p>
 *
 * @since 3.0.0
 */
public class HiveMindConfig {
    private static final Logger LOGGER = TestLogger.getLogger(HiveMindConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Hive Mind Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable Hive Mind - distributed AI for tactical reflexes.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code hivemind.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_ENABLED;

    /**
     * Cloudflare Worker URL for Hive Mind edge computing.
     * <p><b>Default:</b> {@code https://minecraft-agent-reflex.workers.dev}</p>
     * <p><b>Config key:</b> {@code hivemind.workerUrl}</p>
     */
    public static final ForgeConfigSpec.ConfigValue<String> HIVEMIND_WORKER_URL;

    /**
     * Connection timeout in milliseconds.
     * <p><b>Range:</b> 500 to 10000</p>
     * <p><b>Default:</b> 2000</p>
     * <p><b>Config key:</b> {@code hivemind.connectTimeoutMs}</p>
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_CONNECT_TIMEOUT;

    /**
     * Tactical decision timeout in milliseconds.
     * <p><b>Range:</b> 10 to 500</p>
     * <p><b>Default:</b> 50</p>
     * <p><b>Config key:</b> {@code hivemind.tacticalTimeoutMs}</p>
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_TIMEOUT;

    /**
     * State sync timeout in milliseconds.
     * <p><b>Range:</b> 100 to 5000</p>
     * <p><b>Default:</b> 1000</p>
     * <p><b>Config key:</b> {@code hivemind.syncTimeoutMs}</p>
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_TIMEOUT;

    /**
     * How often to check for tactical situations (in ticks).
     * <p><b>Range:</b> 5 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code hivemind.tacticalCheckInterval}</p>
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_CHECK_INTERVAL;

    /**
     * How often to sync state with edge (in ticks).
     * <p><b>Range:</b> 20 to 200</p>
     * <p><b>Default:</b> 100</p>
     * <p><b>Config key:</b> {@code hivemind.syncInterval}</p>
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_INTERVAL;

    /**
     * When edge is unavailable, fall back to local decision-making.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code hivemind.fallbackToLocal}</p>
     */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_FALLBACK_TO_LOCAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Hive Mind Configuration (Cloudflare Edge for tactical decisions)").push("hivemind");

        HIVEMIND_ENABLED = builder
            .comment("Enable Hive Mind - distributed AI with Cloudflare edge for tactical reflexes",
                     "When enabled, agents get sub-20ms combat/hazard responses from edge workers",
                     "When disabled, all decisions are made locally")
            .define("enabled", false);

        HIVEMIND_WORKER_URL = builder
            .comment("Cloudflare Worker URL (e.g., 'https://minecraft-agent-reflex.your-subdomain.workers.dev')")
            .define("workerUrl", "https://minecraft-agent-reflex.workers.dev");

        HIVEMIND_CONNECT_TIMEOUT = builder
            .comment("Connection timeout in milliseconds (for initial connection)")
            .defineInRange("connectTimeoutMs", 2000, 500, 10000);

        HIVEMIND_TACTICAL_TIMEOUT = builder
            .comment("Tactical decision timeout in milliseconds (target: sub-20ms, max: 100ms)")
            .defineInRange("tacticalTimeoutMs", 50, 10, 500);

        HIVEMIND_SYNC_TIMEOUT = builder
            .comment("State sync timeout in milliseconds (less time-critical)")
            .defineInRange("syncTimeoutMs", 1000, 100, 5000);

        HIVEMIND_TACTICAL_CHECK_INTERVAL = builder
            .comment("How often to check for tactical situations (in ticks, 20 ticks = 1 second)",
                     "Lower = faster reflexes but more API calls",
                     "Recommended: 20 (1 second) for balanced performance")
            .defineInRange("tacticalCheckInterval", 20, 5, 100);

        HIVEMIND_SYNC_INTERVAL = builder
            .comment("How often to sync state with edge (in ticks)",
                     "Higher = less network traffic but potentially stale state",
                     "Recommended: 100 (5 seconds)")
            .defineInRange("syncInterval", 100, 20, 200);

        HIVEMIND_FALLBACK_TO_LOCAL = builder
            .comment("When edge is unavailable, fall back to local decision-making",
                     "If false, agent will wait for edge response (not recommended)")
            .define("fallbackToLocal", true);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the Hive Mind configuration.
     *
     * @return true if configuration is valid
     * @throws ConfigException if critical configuration errors are found
     */
    public static boolean validate() throws ConfigException {
        LOGGER.info("Validating Hive Mind configuration...");

        // Validate Hive Mind URL if enabled
        if (HIVEMIND_ENABLED.get()) {
            String workerUrl = HIVEMIND_WORKER_URL.get();
            if (workerUrl != null && !workerUrl.trim().isEmpty()) {
                try {
                    new java.net.URL(workerUrl);
                } catch (java.net.MalformedURLException e) {
                    throw ConfigException.validationFailed("hivemind.workerUrl",
                        "Invalid URL: " + e.getMessage());
                }
            }
            LOGGER.info("Hive Mind: enabled (URL: {})", HIVEMIND_WORKER_URL.get());
        } else {
            LOGGER.info("Hive Mind: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current Hive Mind configuration
     */
    public static String getConfigSummary() {
        if (!HIVEMIND_ENABLED.get()) {
            return "HiveMindConfig[disabled]";
        }
        return String.format(
            "HiveMindConfig[enabled, URL=%s, connectTimeout=%dms, tacticalTimeout=%dms, fallback=%s]",
            HIVEMIND_WORKER_URL.get(),
            HIVEMIND_CONNECT_TIMEOUT.get(),
            HIVEMIND_TACTICAL_TIMEOUT.get(),
            HIVEMIND_FALLBACK_TO_LOCAL.get()
        );
    }
}
