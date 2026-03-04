package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for MineWright AI agent behavior settings.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [behavior]}</b> - Crew behavior settings</p>
 *
 * @since 3.0.0
 */
public class BehaviorConfig {
    private static final Logger LOGGER = TestLogger.getLogger(BehaviorConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Behavior Configuration
    // ------------------------------------------------------------------------

    /**
     * Ticks between action checks.
     * <p><b>Range:</b> 1 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code behavior.actionTickDelay}</p>
     */
    public static final ForgeConfigSpec.IntValue ACTION_TICK_DELAY;

    /**
     * Allow crew members to respond in chat.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code behavior.enableChatResponses}</p>
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHAT_RESPONSES;

    /**
     * Maximum number of crew members that can be active simultaneously.
     * <p><b>Range:</b> 1 to 50</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code behavior.maxActiveCrewMembers}</p>
     */
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_CREW_MEMBERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("MineWright Behavior Configuration").push("behavior");

        ACTION_TICK_DELAY = builder
            .comment("Ticks between action checks (20 ticks = 1 second)")
            .defineInRange("actionTickDelay", 20, 1, 100);

        ENABLE_CHAT_RESPONSES = builder
            .comment("Allow crew members to respond in chat")
            .define("enableChatResponses", true);

        MAX_ACTIVE_CREW_MEMBERS = builder
            .comment("Maximum number of crew members that can be active simultaneously")
            .defineInRange("maxActiveCrewMembers", 10, 1, 50);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the behavior configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating behavior configuration...");
        LOGGER.info("Max active crew members: {}", MAX_ACTIVE_CREW_MEMBERS.get());
        LOGGER.info("Action tick delay: {} ticks ({} seconds)",
            ACTION_TICK_DELAY.get(), ACTION_TICK_DELAY.get() / 20.0);
        LOGGER.info("Chat responses: {}", ENABLE_CHAT_RESPONSES.get() ? "enabled" : "disabled");
        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current behavior configuration
     */
    public static String getConfigSummary() {
        return String.format(
            "BehaviorConfig[actionTickDelay=%d, maxCrew=%d, chatResponses=%s]",
            ACTION_TICK_DELAY.get(),
            MAX_ACTIVE_CREW_MEMBERS.get(),
            ENABLE_CHAT_RESPONSES.get()
        );
    }
}
