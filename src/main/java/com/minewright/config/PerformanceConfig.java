package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for performance tuning and tick budget management.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [performance]}</b> - Performance and budget settings</p>
 *
 * @since 3.0.0
 */
public class PerformanceConfig {
    private static final Logger LOGGER = TestLogger.getLogger(PerformanceConfig.class);

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Performance Configuration
    // ------------------------------------------------------------------------

    /**
     * AI tick budget in milliseconds.
     * <p>AI operations must complete within this time to prevent server lag.</p>
     * <p><b>Range:</b> 1 to 20</p>
     * <p><b>Default:</b> 5</p>
     * <p><b>Config key:</b> {@code performance.aiTickBudgetMs}</p>
     */
    public static final ForgeConfigSpec.IntValue AI_TICK_BUDGET_MS;

    /**
     * Warning threshold as percentage of budget.
     * <p>Warnings are logged when AI operations exceed this percentage of budget.</p>
     * <p><b>Range:</b> 50 to 95</p>
     * <p><b>Default:</b> 80</p>
     * <p><b>Config key:</b> {@code performance.budgetWarningThreshold}</p>
     */
    public static final ForgeConfigSpec.IntValue BUDGET_WARNING_THRESHOLD;

    /**
     * Enable strict budget enforcement.
     * <p>When enabled, operations defer work when budget is exceeded.</p>
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code performance.strictBudgetEnforcement}</p>
     */
    public static final ForgeConfigSpec.BooleanValue STRICT_BUDGET_ENFORCEMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Performance Configuration (Tick budget and enforcement)").push("performance");

        AI_TICK_BUDGET_MS = builder
            .comment("AI tick budget in milliseconds (must complete within this time)",
                     "Minecraft ticks are 50ms total, AI should use significantly less",
                     "Lower = more frequent operation yielding but smoother server performance",
                     "Recommended: 5ms for balanced performance (10% of tick budget)")
            .defineInRange("aiTickBudgetMs", 5, 1, 20);

        BUDGET_WARNING_THRESHOLD = builder
            .comment("Warning threshold as percentage of budget (50 to 95)",
                     "Warnings logged when AI operations exceed this percentage of budget",
                     "Lower = earlier warnings, more conservative operation",
                     "Recommended: 80 (warn when using 80%+ of budget)")
            .defineInRange("budgetWarningThreshold", 80, 50, 95);

        STRICT_BUDGET_ENFORCEMENT = builder
            .comment("Enable strict budget enforcement",
                     "When true, operations defer work when budget is exceeded",
                     "When false, budget is tracked but operations continue (not recommended)",
                     "Recommended: true for production servers")
            .define("strictBudgetEnforcement", true);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the performance configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating performance configuration...");
        int tickBudget = AI_TICK_BUDGET_MS.get();
        int budgetWarning = BUDGET_WARNING_THRESHOLD.get();
        boolean strictEnforcement = STRICT_BUDGET_ENFORCEMENT.get();
        LOGGER.info("Performance: tick_budget={}ms, warning_threshold={}%, strict_enforcement={}",
            tickBudget, budgetWarning, strictEnforcement);
        return true;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current performance configuration
     */
    public static String getConfigSummary() {
        return String.format(
            "PerformanceConfig[tickBudget=%dms, warning=%d%%, strict=%s]",
            AI_TICK_BUDGET_MS.get(),
            BUDGET_WARNING_THRESHOLD.get(),
            STRICT_BUDGET_ENFORCEMENT.get()
        );
    }
}
