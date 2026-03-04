package com.minewright.config;

import com.minewright.testutil.TestLogger;
import com.minewright.exception.ConfigException;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for Utility AI decision-making system.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [utility_ai]}</b> - Decision-making weights and settings</p>
 *
 * <h2>Overview</h2>
 * <p>When enabled, uses weighted scoring for action selection based on
 * urgency, proximity, and safety factors.</p>
 *
 * @since 3.0.0
 */
public class UtilityAIConfig {
    private static final Logger LOGGER = TestLogger.getLogger(UtilityAIConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Utility AI Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable utility AI for decision-making.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code utility_ai.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue UTILITY_AI_ENABLED;

    /**
     * Weight for urgency in utility calculations.
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code utility_ai.urgency_weight}</p>
     */
    public static final ForgeConfigSpec.DoubleValue URGENCY_WEIGHT;

    /**
     * Weight for proximity in utility calculations.
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 0.8</p>
     * <p><b>Config key:</b> {@code utility_ai.proximity_weight}</p>
     */
    public static final ForgeConfigSpec.DoubleValue PROXIMITY_WEIGHT;

    /**
     * Weight for safety in utility calculations.
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 1.2</p>
     * <p><b>Config key:</b> {@code utility_ai.safety_weight}</p>
     */
    public static final ForgeConfigSpec.DoubleValue SAFETY_WEIGHT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Utility AI Configuration (Decision-making weights)").push("utility_ai");

        UTILITY_AI_ENABLED = builder
            .comment("Enable utility AI for decision-making",
                     "When enabled, uses weighted scoring for action selection",
                     "When disabled, uses simple priority-based selection")
            .define("enabled", true);

        URGENCY_WEIGHT = builder
            .comment("Weight for urgency in utility calculations (0.0 to 2.0)",
                     "Higher = prioritizes time-sensitive actions more",
                     "Recommended: 1.0 for balanced behavior")
            .defineInRange("urgency_weight", 1.0, 0.0, 2.0);

        PROXIMITY_WEIGHT = builder
            .comment("Weight for proximity in utility calculations (0.0 to 2.0)",
                     "Higher = prioritizes nearby tasks more",
                     "Recommended: 0.8 for balanced behavior")
            .defineInRange("proximity_weight", 0.8, 0.0, 2.0);

        SAFETY_WEIGHT = builder
            .comment("Weight for safety in utility calculations (0.0 to 2.0)",
                     "Higher = prioritizes safe actions over risky ones",
                     "Recommended: 1.2 for safety-focused behavior")
            .defineInRange("safety_weight", 1.2, 0.0, 2.0);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the utility AI configuration.
     *
     * @return true if configuration is valid
     * @throws ConfigException if critical configuration errors are found
     */
    public static boolean validate() throws ConfigException {
        LOGGER.info("Validating utility AI configuration...");

        if (UTILITY_AI_ENABLED.get()) {
            double urgency = URGENCY_WEIGHT.get();
            double proximity = PROXIMITY_WEIGHT.get();
            double safety = SAFETY_WEIGHT.get();
            double totalWeight = urgency + proximity + safety;

            if (urgency < 0.0 || urgency > 2.0) {
                throw ConfigException.validationFailed("utility_ai.urgency_weight",
                    "Weight must be between 0.0 and 2.0, got: " + urgency);
            }
            if (proximity < 0.0 || proximity > 2.0) {
                throw ConfigException.validationFailed("utility_ai.proximity_weight",
                    "Weight must be between 0.0 and 2.0, got: " + proximity);
            }
            if (safety < 0.0 || safety > 2.0) {
                throw ConfigException.validationFailed("utility_ai.safety_weight",
                    "Weight must be between 0.0 and 2.0, got: " + safety);
            }

            LOGGER.info("Utility AI: enabled (urgency: {}, proximity: {}, safety: {}, total: {})",
                urgency, proximity, safety, totalWeight);

            if (totalWeight > 5.0) {
                LOGGER.warn("Utility AI weights sum to {}, which is high. Consider balancing weights.", totalWeight);
            }
        } else {
            LOGGER.info("Utility AI: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current utility AI configuration
     */
    public static String getConfigSummary() {
        if (!UTILITY_AI_ENABLED.get()) {
            return "UtilityAIConfig[disabled]";
        }
        return String.format(
            "UtilityAIConfig[enabled, urgency=%.2f, proximity=%.2f, safety=%.2f]",
            URGENCY_WEIGHT.get(),
            PROXIMITY_WEIGHT.get(),
            SAFETY_WEIGHT.get()
        );
    }
}
