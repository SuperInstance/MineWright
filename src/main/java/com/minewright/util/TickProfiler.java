package com.minewright.util;

import com.minewright.config.MineWrightConfig;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Utility class for profiling and enforcing tick budget constraints for AI operations.
 *
 * <p><b>Purpose:</b></p>
 * <p>Minecraft servers have a strict tick budget of 50ms per tick (20 ticks per second).
 * AI operations must complete in significantly less time to prevent server lag.
 * This profiler tracks AI operation time and enforces budget limits.</p>
 *
 * <p><b>Design Constraints (from MINECRAFT_CONSTRAINTS_DISSERTATION.md):</b></p>
 * <ul>
 *   <li>AI operations must complete in <5ms per tick</li>
 *   <li>Server tick budget is 50ms total</li>
 *   <li>Exceeding budget causes server lag and degraded player experience</li>
 * </ul>
 *
 * <p><b>Usage Pattern:</b></p>
 * <pre>
 * // At start of tick processing
 * TickProfiler profiler = new TickProfiler();
 * profiler.startTick();
 *
 * // During AI operations
 * if (profiler.isOverBudget()) {
 *     // Defer work to next tick
 *     return;
 * }
 *
 * // At end of tick (optional)
 * long elapsed = profiler.getElapsedMs();
 * if (elapsed > profiler.getWarningThreshold()) {
 *     LOGGER.warn("AI operation took {}ms (budget: {}ms)", elapsed, profiler.getBudgetMs());
 * }
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is NOT thread-safe and should be used by a single thread.
 * In Minecraft, tick() methods are called on the server thread only.</p>
 *
 * <p><b>Configuration:</b></p>
 * <p>Budget limits can be configured in <code>config/minewright-common.toml</code>:</p>
 * <pre>
 * [performance]
 * # AI tick budget in milliseconds (default: 5)
 * ai_tick_budget_ms = 5
 *
 * # Warning threshold as percentage of budget (default: 80)
 * budget_warning_threshold = 80
 *
 * # Enable strict budget enforcement (default: true)
 * strict_budget_enforcement = true
 * </pre>
 *
 * @see com.minewright.action.ActionExecutor
 * @since 2.1.0
 */
public class TickProfiler {
    private static final Logger LOGGER = TestLogger.getLogger(TickProfiler.class);

    /**
     * Default AI tick budget in milliseconds.
     * AI operations must complete within this time to prevent server lag.
     */
    private static final long DEFAULT_AI_BUDGET_MS = 5L;

    /**
     * Default warning threshold as percentage of budget.
     * Warnings are logged when usage exceeds this percentage.
     */
    private static final double DEFAULT_WARNING_THRESHOLD = 0.8;

    /**
     * System time when the tick started.
     * Used to calculate elapsed time and remaining budget.
     */
    private long tickStartTime;

    /**
     * Whether budget profiling is currently active.
     */
    private boolean isRunning;

    /**
     * The AI tick budget in milliseconds.
     * Read from config, defaults to 5ms if not configured.
     */
    private final long budgetMs;

    /**
     * The warning threshold in milliseconds.
     * Warnings logged when elapsed time exceeds this threshold.
     */
    private final long warningThresholdMs;

    /**
     * Whether strict budget enforcement is enabled.
     * When true, operations should defer work when over budget.
     */
    private final boolean strictEnforcement;

    /**
     * Creates a new TickProfiler with default settings.
     * Budget is read from configuration or defaults to 5ms.
     */
    public TickProfiler() {
        this.budgetMs = readBudgetFromConfig();
        this.warningThresholdMs = (long) (this.budgetMs * readWarningThresholdFromConfig());
        this.strictEnforcement = readStrictEnforcementFromConfig();
        this.isRunning = false;
    }

    /**
     * Creates a new TickProfiler with a specific budget.
     * Useful for testing or operations with different budget requirements.
     *
     * @param budgetMs The tick budget in milliseconds
     */
    public TickProfiler(long budgetMs) {
        this(budgetMs, DEFAULT_WARNING_THRESHOLD, true);
    }

    /**
     * Creates a new TickProfiler with full configuration.
     * Useful for testing and special cases.
     *
     * @param budgetMs The tick budget in milliseconds
     * @param warningThreshold The warning threshold (0.0 to 1.0)
     * @param strictEnforcement Whether to enforce budget strictly
     */
    public TickProfiler(long budgetMs, double warningThreshold, boolean strictEnforcement) {
        if (budgetMs <= 0) {
            throw new IllegalArgumentException("Budget must be positive: " + budgetMs);
        }
        if (warningThreshold < 0.0 || warningThreshold > 1.0) {
            throw new IllegalArgumentException("Warning threshold must be 0.0-1.0: " + warningThreshold);
        }

        this.budgetMs = budgetMs;
        this.warningThresholdMs = (long) (budgetMs * warningThreshold);
        this.strictEnforcement = strictEnforcement;
        this.isRunning = false;
    }

    /**
     * Starts profiling a new tick.
     * Must be called before using other methods.
     */
    public void startTick() {
        this.tickStartTime = System.nanoTime();
        this.isRunning = true;
    }

    /**
     * Checks if the current operation has exceeded the tick budget.
     *
     * <p><b>Usage:</b></p>
     * <pre>
     * if (profiler.isOverBudget()) {
     *     // Defer remaining work to next tick
     *     return;
     * }
     * </pre>
     *
     * @return true if budget has been exceeded
     * @throws IllegalStateException if startTick() was not called
     */
    public boolean isOverBudget() {
        if (!isRunning) {
            throw new IllegalStateException("TickProfiler not started. Call startTick() first.");
        }
        return getElapsedMs() > budgetMs;
    }

    /**
     * Checks if the current operation has exceeded the warning threshold.
     *
     * <p>This allows for early warning before budget is fully exceeded.</p>
     *
     * @return true if warning threshold has been exceeded
     * @throws IllegalStateException if startTick() was not called
     */
    public boolean isOverWarningThreshold() {
        if (!isRunning) {
            throw new IllegalStateException("TickProfiler not started. Call startTick() first.");
        }
        return getElapsedMs() > warningThresholdMs;
    }

    /**
     * Gets the elapsed time since tick start in milliseconds.
     *
     * @return Elapsed time in milliseconds
     * @throws IllegalStateException if startTick() was not called
     */
    public long getElapsedMs() {
        if (!isRunning) {
            throw new IllegalStateException("TickProfiler not started. Call startTick() first.");
        }
        long elapsedNanos = System.nanoTime() - tickStartTime;
        return elapsedNanos / 1_000_000L; // Convert to milliseconds
    }

    /**
     * Gets the remaining budget in milliseconds.
     *
     * <p>Returns 0 if budget has been exceeded.</p>
     *
     * @return Remaining budget in milliseconds, or 0 if exceeded
     * @throws IllegalStateException if startTick() was not called
     */
    public long getRemainingBudget() {
        if (!isRunning) {
            throw new IllegalStateException("TickProfiler not started. Call startTick() first.");
        }
        long remaining = budgetMs - getElapsedMs();
        return Math.max(0L, remaining);
    }

    /**
     * Gets the tick budget in milliseconds.
     *
     * @return Budget in milliseconds
     */
    public long getBudgetMs() {
        return budgetMs;
    }

    /**
     * Gets the warning threshold in milliseconds.
     *
     * @return Warning threshold in milliseconds
     */
    public long getWarningThreshold() {
        return warningThresholdMs;
    }

    /**
     * Checks if strict budget enforcement is enabled.
     *
     * @return true if strict enforcement is enabled
     */
    public boolean isStrictEnforcement() {
        return strictEnforcement;
    }

    /**
     * Checks if the profiler is currently running.
     *
     * @return true if profiling is active
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Stops profiling and returns the total elapsed time.
     *
     * @return Total elapsed time in milliseconds
     * @throws IllegalStateException if startTick() was not called
     */
    public long stopTick() {
        long elapsed = getElapsedMs();
        this.isRunning = false;

        // Log warning if budget was exceeded
        if (elapsed > budgetMs) {
            LOGGER.warn("Tick budget exceeded: {}ms / {}ms ({}% over budget)",
                elapsed, budgetMs,
                String.format("%.1f", ((elapsed - budgetMs) * 100.0 / budgetMs)));
        } else if (elapsed > warningThresholdMs) {
            LOGGER.debug("Tick usage high: {}ms / {}ms ({}% of budget)",
                elapsed, budgetMs,
                String.format("%.1f", (elapsed * 100.0 / budgetMs)));
        }

        return elapsed;
    }

    /**
     * Logs a warning if the budget has been exceeded.
     * This is a convenience method that combines checking and logging.
     *
     * @return true if budget was exceeded (warning was logged)
     */
    public boolean logWarningIfExceeded() {
        if (!isRunning) {
            return false;
        }

        long elapsed = getElapsedMs();
        if (elapsed > budgetMs) {
            LOGGER.warn("AI tick budget exceeded: {}ms used out of {}ms budget ({}% over)",
                elapsed, budgetMs,
                String.format("%.1f", ((elapsed - budgetMs) * 100.0 / budgetMs)));
            return true;
        } else if (elapsed > warningThresholdMs) {
            LOGGER.debug("AI tick usage approaching limit: {}ms used out of {}ms budget ({}% used)",
                elapsed, budgetMs,
                String.format("%.1f", (elapsed * 100.0 / budgetMs)));
            return false;
        }

        return false;
    }

    /**
     * Creates a string representation of the current profiling state.
     *
     * @return String representation with elapsed time and budget info
     */
    @Override
    public String toString() {
        if (!isRunning) {
            return "TickProfiler[not started]";
        }

        long elapsed = getElapsedMs();
        long remaining = getRemainingBudget();
        double percentage = budgetMs > 0 ? (elapsed * 100.0 / budgetMs) : 0.0;

        return String.format("TickProfiler[elapsed=%dms, budget=%dms, remaining=%dms, usage=%.1f%%]",
            elapsed, budgetMs, remaining, percentage);
    }

    /**
     * Reads the AI tick budget from configuration.
     * Defaults to 5ms if configuration is not available.
     *
     * @return Budget in milliseconds
     */
    private static long readBudgetFromConfig() {
        try {
            return MineWrightConfig.AI_TICK_BUDGET_MS.get();
        } catch (Exception e) {
            LOGGER.debug("Could not read AI tick budget from config, using default: {}ms",
                DEFAULT_AI_BUDGET_MS);
            return DEFAULT_AI_BUDGET_MS;
        }
    }

    /**
     * Reads the warning threshold from configuration.
     * Defaults to 80% if configuration is not available.
     *
     * @return Warning threshold as a percentage (0.0 to 1.0)
     */
    private static double readWarningThresholdFromConfig() {
        try {
            int threshold = MineWrightConfig.BUDGET_WARNING_THRESHOLD.get();
            return threshold / 100.0;
        } catch (Exception e) {
            LOGGER.debug("Could not read warning threshold from config, using default: {}",
                DEFAULT_WARNING_THRESHOLD);
            return DEFAULT_WARNING_THRESHOLD;
        }
    }

    /**
     * Reads the strict enforcement flag from configuration.
     * Defaults to true if configuration is not available.
     *
     * @return true if strict enforcement is enabled
     */
    private static boolean readStrictEnforcementFromConfig() {
        try {
            return MineWrightConfig.STRICT_BUDGET_ENFORCEMENT.get();
        } catch (Exception e) {
            LOGGER.debug("Could not read strict enforcement from config, using default: true");
            return true;
        }
    }

    /**
     * Checks if the profiler has been configured for strict budget enforcement.
     * This is a static helper for checking global configuration.
     *
     * @return true if strict enforcement is globally enabled
     */
    public static boolean isGlobalStrictEnforcement() {
        return readStrictEnforcementFromConfig();
    }

    /**
     * Gets the global AI tick budget from configuration.
     * This is a static helper for checking global configuration.
     *
     * @return Budget in milliseconds
     */
    public static long getGlobalBudget() {
        return readBudgetFromConfig();
    }
}
