package com.minewright.config;

import com.minewright.testutil.TestLogger;
import com.minewright.exception.ConfigException;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for Cascade Router - intelligent LLM selection system.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [cascade_router]}</b> - Intelligent LLM selection settings</p>
 *
 * <h2>Overview</h2>
 * <p>When enabled, routes tasks to appropriate LLM based on complexity.
 * Implements semantic similarity matching for task classification.</p>
 *
 * @since 3.0.0
 */
public class CascadeRouterConfig {
    private static final Logger LOGGER = TestLogger.getLogger(CascadeRouterConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Cascade Router Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable cascade router for intelligent LLM selection.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code cascade_router.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue CASCADE_ROUTER_ENABLED;

    /**
     * Semantic similarity threshold for cascade routing decisions.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.85</p>
     * <p><b>Config key:</b> {@code cascade_router.similarity_threshold}</p>
     */
    public static final ForgeConfigSpec.DoubleValue SEMANTIC_SIMILARITY_THRESHOLD;

    /**
     * Use local LLM for cascade router fallback.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code cascade_router.use_local_llm}</p>
     */
    public static final ForgeConfigSpec.BooleanValue USE_LOCAL_LLM;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Cascade Router Configuration (Intelligent LLM selection)").push("cascade_router");

        CASCADE_ROUTER_ENABLED = builder
            .comment("Enable cascade router for intelligent LLM selection",
                     "When enabled, routes tasks to appropriate LLM based on complexity",
                     "When disabled, always uses primary LLM")
            .define("enabled", true);

        SEMANTIC_SIMILARITY_THRESHOLD = builder
            .comment("Semantic similarity threshold for cascade routing decisions (0.0 to 1.0)",
                     "Tasks with similarity above this threshold use cached/local LLM",
                     "Higher = more local processing, less API usage")
            .defineInRange("similarity_threshold", 0.85, 0.0, 1.0);

        USE_LOCAL_LLM = builder
            .comment("Use local LLM for cascade router fallback",
                     "When true, falls back to local LLM for similar tasks",
                     "When false, always uses primary API LLM")
            .define("use_local_llm", false);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the cascade router configuration.
     *
     * @return true if configuration is valid
     * @throws ConfigException if critical configuration errors are found
     */
    public static boolean validate() throws ConfigException {
        LOGGER.info("Validating cascade router configuration...");

        if (CASCADE_ROUTER_ENABLED.get()) {
            double similarity = SEMANTIC_SIMILARITY_THRESHOLD.get();
            if (similarity < 0.0 || similarity > 1.0) {
                throw ConfigException.validationFailed("cascade_router.similarity_threshold",
                    "Similarity must be between 0.0 and 1.0, got: " + similarity);
            }
            LOGGER.info("Cascade Router: enabled (similarity: {}, use_local_llm: {})",
                similarity, USE_LOCAL_LLM.get());
        } else {
            LOGGER.info("Cascade Router: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current cascade router configuration
     */
    public static String getConfigSummary() {
        if (!CASCADE_ROUTER_ENABLED.get()) {
            return "CascadeRouterConfig[disabled]";
        }
        return String.format(
            "CascadeRouterConfig[enabled, similarity=%.2f, useLocalLLM=%s]",
            SEMANTIC_SIMILARITY_THRESHOLD.get(),
            USE_LOCAL_LLM.get()
        );
    }
}
