package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for Skill Library system - learning and storing successful action patterns.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [skill_library]}</b> - Skill learning and storage settings</p>
 *
 * <h2>Overview</h2>
 * <p>When enabled, agents learn from successful executions and reuse patterns.
 * This implements the Voyager-style skill composition system.</p>
 *
 * @since 3.0.0
 */
public class SkillLibraryConfig {
    private static final Logger LOGGER = TestLogger.getLogger(SkillLibraryConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Skill Library Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable skill library for learning and storing successful action patterns.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code skill_library.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue SKILL_LIBRARY_ENABLED;

    /**
     * Maximum number of skills to store in the library.
     * <p><b>Range:</b> 10 to 1000</p>
     * <p><b>Default:</b> 100</p>
     * <p><b>Config key:</b> {@code skill_library.max_skills}</p>
     */
    public static final ForgeConfigSpec.IntValue MAX_SKILLS_STORED;

    /**
     * Success threshold for considering a skill as learned.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.7</p>
     * <p><b>Config key:</b> {@code skill_library.success_threshold}</p>
     */
    public static final ForgeConfigSpec.DoubleValue SKILL_SUCCESS_THRESHOLD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Skill Library Configuration (Learning and pattern storage)").push("skill_library");

        SKILL_LIBRARY_ENABLED = builder
            .comment("Enable skill library for learning and storing successful action patterns",
                     "When enabled, agents learn from successful executions and reuse patterns",
                     "When disabled, each action is planned from scratch")
            .define("enabled", true);

        MAX_SKILLS_STORED = builder
            .comment("Maximum number of skills to store in the library",
                     "Higher = more learned patterns but more memory usage",
                     "Recommended: 100 for balanced performance")
            .defineInRange("max_skills", 100, 10, 1000);

        SKILL_SUCCESS_THRESHOLD = builder
            .comment("Success threshold for considering a skill as learned (0.0 to 1.0)",
                     "Skills with success rate above this threshold are stored",
                     "Higher = fewer but more reliable skills")
            .defineInRange("success_threshold", 0.7, 0.0, 1.0);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the skill library configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating skill library configuration...");

        if (SKILL_LIBRARY_ENABLED.get()) {
            double threshold = SKILL_SUCCESS_THRESHOLD.get();
            if (threshold < 0.0 || threshold > 1.0) {
                LOGGER.warn("Skill Library success threshold out of range: {}. Should be 0.0-1.0", threshold);
            }
            int maxSkills = MAX_SKILLS_STORED.get();
            if (maxSkills < 10 || maxSkills > 1000) {
                LOGGER.warn("Max skills stored out of range: {}. Should be 10-1000", maxSkills);
            }
            LOGGER.info("Skill Library: enabled (threshold: {}, max skills: {})", threshold, maxSkills);
        } else {
            LOGGER.info("Skill Library: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current skill library configuration
     */
    public static String getConfigSummary() {
        if (!SKILL_LIBRARY_ENABLED.get()) {
            return "SkillLibraryConfig[disabled]";
        }
        return String.format(
            "SkillLibraryConfig[enabled, threshold=%.2f, maxSkills=%d]",
            SKILL_SUCCESS_THRESHOLD.get(),
            MAX_SKILLS_STORED.get()
        );
    }
}
