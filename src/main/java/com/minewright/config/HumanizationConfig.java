package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for humanization features - making AI agents behave more naturally.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [humanization]}</b> - Natural agent behavior settings</p>
 *
 * <h2>Overview</h2>
 * <p>When enabled, agents exhibit human-like timing, mistakes, and behaviors
 * including reaction times, fatigue, warm-up periods, and break intervals.</p>
 *
 * @since 3.0.0
 */
public class HumanizationConfig {
    private static final Logger LOGGER = TestLogger.getLogger(HumanizationConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Humanization Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable humanization features for more natural agent behavior.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue HUMANIZATION_ENABLED;

    /**
     * Timing variance as fraction of base value.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.3 (30% variance)</p>
     * <p><b>Config key:</b> {@code humanization.timing_variance}</p>
     */
    public static final ForgeConfigSpec.DoubleValue TIMING_VARIANCE;

    /**
     * Minimum action delay in ticks.
     * <p><b>Range:</b> 1 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 2</p>
     * <p><b>Config key:</b> {@code humanization.min_action_delay_ticks}</p>
     */
    public static final ForgeConfigSpec.IntValue MIN_ACTION_DELAY_TICKS;

    /**
     * Maximum action delay in ticks.
     * <p><b>Range:</b> 1 to 200 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code humanization.max_action_delay_ticks}</p>
     */
    public static final ForgeConfigSpec.IntValue MAX_ACTION_DELAY_TICKS;

    /**
     * Movement speed variance as fraction of base speed.
     * <p><b>Range:</b> 0.0 to 0.5</p>
     * <p><b>Default:</b> 0.1 (10% variance)</p>
     * <p><b>Config key:</b> {@code humanization.speed_variance}</p>
     */
    public static final ForgeConfigSpec.DoubleValue SPEED_VARIANCE;

    /**
     * Chance per tick of micro-movement.
     * <p><b>Range:</b> 0.0 to 0.2</p>
     * <p><b>Default:</b> 0.05 (5% chance)</p>
     * <p><b>Config key:</b> {@code humanization.micro_movement_chance}</p>
     */
    public static final ForgeConfigSpec.DoubleValue MICRO_MOVEMENT_CHANCE;

    /**
     * Enable smooth look transitions.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.smooth_look}</p>
     */
    public static final ForgeConfigSpec.BooleanValue SMOOTH_LOOK;

    /**
     * Base mistake rate for agent actions.
     * <p><b>Range:</b> 0.0 to 0.2</p>
     * <p><b>Default:</b> 0.03 (3% mistake rate)</p>
     * <p><b>Config key:</b> {@code humanization.mistake_rate}</p>
     */
    public static final ForgeConfigSpec.DoubleValue MISTAKE_RATE;

    /**
     * Minimum agent reaction time in milliseconds.
     * <p>Agents wait at least this long before responding to stimuli.</p>
     * <p><b>Range:</b> 50 to 1000</p>
     * <p><b>Default:</b> 150</p>
     * <p><b>Config key:</b> {@code humanization.reaction_time_min_ms}</p>
     */
    public static final ForgeConfigSpec.IntValue REACTION_TIME_MIN_MS;

    /**
     * Maximum agent reaction time in milliseconds.
     * <p>Agents wait at most this long before responding to stimuli.</p>
     * <p><b>Range:</b> 100 to 5000</p>
     * <p><b>Default:</b> 500</p>
     * <p><b>Config key:</b> {@code humanization.reaction_time_max_ms}</p>
     */
    public static final ForgeConfigSpec.IntValue REACTION_TIME_MAX_MS;

    /**
     * Idle action chance per tick.
     * <p><b>Range:</b> 0.0 to 0.1</p>
     * <p><b>Default:</b> 0.02 (2% chance)</p>
     * <p><b>Config key:</b> {@code humanization.idle_action_chance}</p>
     */
    public static final ForgeConfigSpec.DoubleValue IDLE_ACTION_CHANCE;

    /**
     * Enable personality-driven idle behaviors.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.personality_affects_idle}</p>
     */
    public static final ForgeConfigSpec.BooleanValue PERSONALITY_AFFECTS_IDLE;

    /**
     * Enable session modeling (warm-up, fatigue, breaks).
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code humanization.session_modeling_enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue SESSION_MODELING_ENABLED;

    /**
     * Warm-up duration in minutes.
     * <p><b>Range:</b> 1 to 60</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code humanization.warmup_duration_minutes}</p>
     */
    public static final ForgeConfigSpec.IntValue WARMUP_DURATION_MINUTES;

    /**
     * Fatigue onset time in minutes.
     * <p><b>Range:</b> 15 to 180</p>
     * <p><b>Default:</b> 60</p>
     * <p><b>Config key:</b> {@code humanization.fatigue_start_minutes}</p>
     */
    public static final ForgeConfigSpec.IntValue FATIGUE_START_MINUTES;

    /**
     * Minimum break interval in minutes.
     * <p><b>Range:</b> 5 to 120</p>
     * <p><b>Default:</b> 30</p>
     * <p><b>Config key:</b> {@code humanization.break_interval_minutes}</p>
     */
    public static final ForgeConfigSpec.IntValue BREAK_INTERVAL_MINUTES;

    /**
     * Break duration in minutes.
     * <p><b>Range:</b> 1 to 10</p>
     * <p><b>Default:</b> 2</p>
     * <p><b>Config key:</b> {@code humanization.break_duration_minutes}</p>
     */
    public static final ForgeConfigSpec.IntValue BREAK_DURATION_MINUTES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Humanization Configuration (Natural agent behavior)").push("humanization");

        HUMANIZATION_ENABLED = builder
            .comment("Enable humanization features for more natural agent behavior",
                     "When enabled, agents exhibit human-like timing, mistakes, and behaviors",
                     "When disabled, agents act with perfect consistency (robotic)",
                     "Recommended: true for immersive gameplay")
            .define("enabled", true);

        TIMING_VARIANCE = builder
            .comment("Timing variance as fraction of base value (0.0 to 1.0)",
                     "Higher = more variation in action delays",
                     "30% variance means delays vary by ±30% from base",
                     "Recommended: 0.3 for natural behavior")
            .defineInRange("timing_variance", 0.3, 0.0, 1.0);

        MIN_ACTION_DELAY_TICKS = builder
            .comment("Minimum action delay in ticks (20 ticks = 1 second)",
                     "Prevents unrealistically fast actions",
                     "Recommended: 2 ticks (100ms minimum)")
            .defineInRange("min_action_delay_ticks", 2, 1, 100);

        MAX_ACTION_DELAY_TICKS = builder
            .comment("Maximum action delay in ticks (20 ticks = 1 second)",
                     "Prevents excessively slow actions",
                     "Recommended: 20 ticks (1 second maximum)")
            .defineInRange("max_action_delay_ticks", 20, 1, 200);

        SPEED_VARIANCE = builder
            .comment("Movement speed variance as fraction of base speed (0.0 to 0.5)",
                     "Higher = more variation in movement speed",
                     "10% variance means speed varies by ±10% from base",
                     "Recommended: 0.1 for subtle variation")
            .defineInRange("speed_variance", 0.1, 0.0, 0.5);

        MICRO_MOVEMENT_CHANCE = builder
            .comment("Chance per tick of micro-movement (0.0 to 0.2)",
                     "Small fidgeting movements when idle or moving",
                     "5% chance = 1 in 20 ticks",
                     "Recommended: 0.05 for realistic micro-movements")
            .defineInRange("micro_movement_chance", 0.05, 0.0, 0.2);

        SMOOTH_LOOK = builder
            .comment("Enable smooth look transitions",
                     "When true, agents gradually turn instead of snapping",
                     "When false, instant direction changes",
                     "Recommended: true for natural movement")
            .define("smooth_look", true);

        MISTAKE_RATE = builder
            .comment("Base mistake rate for agent actions (0.0 to 0.2)",
                     "3% = 1 mistake per 33 actions (average human level)",
                     "Higher = more mistakes (beginner/fatigued)",
                     "Lower = fewer mistakes (expert)",
                     "Recommended: 0.03 for realistic behavior")
            .defineInRange("mistake_rate", 0.03, 0.0, 0.2);

        REACTION_TIME_MIN_MS = builder
            .comment("Minimum agent reaction time in milliseconds",
                     "Agents wait at least this long before responding to stimuli",
                     "Lower = faster responses (less realistic)",
                     "Higher = more realistic but potentially frustrating",
                     "Recommended: 150 for realistic human reaction time")
            .defineInRange("reaction_time_min_ms", 150, 50, 1000);

        REACTION_TIME_MAX_MS = builder
            .comment("Maximum agent reaction time in milliseconds",
                     "Agents wait at most this long before responding to stimuli",
                     "Lower = more consistent reaction times",
                     "Higher = more variance (can simulate fatigue/distraction)",
                     "Recommended: 500 for typical human variance")
            .defineInRange("reaction_time_max_ms", 500, 100, 5000);

        IDLE_ACTION_CHANCE = builder
            .comment("Idle action chance per tick (0.0 to 0.1)",
                     "2% = agent performs idle action ~2.4 times per second",
                     "Actions include: look around, fidget, stretch",
                     "Recommended: 0.02 for lively but not chaotic behavior")
            .defineInRange("idle_action_chance", 0.02, 0.0, 0.1);

        PERSONALITY_AFFECTS_IDLE = builder
            .comment("Enable personality-driven idle behaviors",
                     "When true, idle actions vary by personality traits",
                     "When false, all agents use same idle pattern",
                     "Recommended: true for diverse agent behaviors")
            .define("personality_affects_idle", true);

        SESSION_MODELING_ENABLED = builder
            .comment("Enable session modeling (warm-up, fatigue, breaks)",
                     "When true, agent performance changes over session",
                     "When false, consistent performance throughout",
                     "Recommended: true for long-term realism")
            .define("session_modeling_enabled", true);

        WARMUP_DURATION_MINUTES = builder
            .comment("Warm-up duration in minutes",
                     "Agents slower/more mistake-prone during warm-up",
                     "Recommended: 10 minutes for realistic warm-up")
            .defineInRange("warmup_duration_minutes", 10, 1, 60);

        FATIGUE_START_MINUTES = builder
            .comment("Fatigue onset time in minutes",
                     "Agents begin degrading after this time",
                     "Recommended: 60 minutes for typical play session")
            .defineInRange("fatigue_start_minutes", 60, 15, 180);

        BREAK_INTERVAL_MINUTES = builder
            .comment("Minimum break interval in minutes",
                     "Agents may take breaks after this interval",
                     "10% chance per check after minimum, forced at 2 hours",
                     "Recommended: 30 minutes for realistic break pattern")
            .defineInRange("break_interval_minutes", 30, 5, 120);

        BREAK_DURATION_MINUTES = builder
            .comment("Break duration in minutes",
                     "Agents pause actions during breaks",
                     "Recommended: 2 minutes for short breaks")
            .defineInRange("break_duration_minutes", 2, 1, 10);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the humanization configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating humanization configuration...");

        if (HUMANIZATION_ENABLED.get()) {
            LOGGER.info("Humanization: enabled");
            LOGGER.info("  Reaction time: {}-{}ms", REACTION_TIME_MIN_MS.get(), REACTION_TIME_MAX_MS.get());
            LOGGER.info("  Mistake rate: {:.3f}", MISTAKE_RATE.get());
            LOGGER.info("  Session modeling: {}", SESSION_MODELING_ENABLED.get() ? "enabled" : "disabled");
        } else {
            LOGGER.info("Humanization: disabled");
        }

        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current humanization configuration
     */
    public static String getConfigSummary() {
        if (!HUMANIZATION_ENABLED.get()) {
            return "HumanizationConfig[disabled]";
        }
        return String.format(
            "HumanizationConfig[enabled, reactionTime=%d-%dms, mistakeRate=%.3f, session=%s]",
            REACTION_TIME_MIN_MS.get(),
            REACTION_TIME_MAX_MS.get(),
            MISTAKE_RATE.get(),
            SESSION_MODELING_ENABLED.get() ? "enabled" : "disabled"
        );
    }
}
