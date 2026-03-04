package com.minewright.action;

import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.skill.ExecutionTracker;
import com.minewright.util.TickProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors execution progress and tracks metrics.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Progress tracking for current action</li>
 *   <li>Tick budget enforcement via {@link TickProfiler}</li>
 *   <li>Execution sequence tracking for skill learning</li>
 *   <li>Metrics collection and reporting</li>
 * </ul>
 *
 * <p><b>Tick Budget Enforcement:</b></p>
 * <p>Ensures AI operations complete within configured budget (default 5ms)
 * to prevent server lag. Uses consolidated budget checking to reduce
 * overhead from multiple isOverBudget() calls.</p>
 *
 * @since 1.0.0
 */
public class ExecutionMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionMonitor.class);

    /**
     * Tick profiler for enforcing AI operation budget constraints.
     */
    private final TickProfiler tickProfiler;

    /**
     * Execution tracker for recording action sequences for skill learning.
     */
    private final ExecutionTracker executionTracker;

    /**
     * The foreman entity being monitored.
     */
    private final ForemanEntity foreman;

    /**
     * Current goal description for execution tracking.
     */
    private volatile String currentGoal;

    /**
     * Creates a new execution monitor.
     *
     * @param foreman The foreman entity to monitor
     */
    public ExecutionMonitor(ForemanEntity foreman) {
        this.foreman = foreman;
        this.tickProfiler = new TickProfiler();
        this.executionTracker = ExecutionTracker.getInstance();
        this.currentGoal = null;
    }

    /**
     * Starts tick profiling for budget enforcement.
     * Call this at the beginning of each tick.
     */
    public void startTick() {
        tickProfiler.startTick();
    }

    /**
     * Checks the tick budget and yields if over budget.
     *
     * <p>This is a consolidated budget check that replaces multiple isOverBudget()
     * calls throughout tick(). This optimization reduces the overhead of budget
     * checking from 6 calls per tick to a single entry point.</p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>If over budget and strict enforcement is enabled: returns false (yield)</li>
     *   <li>If over budget and strict enforcement is disabled: logs warning, returns true (continue)</li>
     *   <li>If under budget: returns true (continue)</li>
     * </ul>
     *
     * @return true if within budget or strict enforcement disabled, false if should yield
     */
    public boolean checkBudgetAndYield() {
        if (!tickProfiler.isRunning()) {
            return true; // Not tracking, continue
        }

        boolean overBudget = tickProfiler.isOverBudget();
        if (overBudget) {
            // Log warning if exceeded
            tickProfiler.logWarningIfExceeded();

            // Check if we should enforce strictly
            if (tickProfiler.isStrictEnforcement()) {
                // Defer remaining work to next tick
                return false;
            }
            // Strict enforcement disabled, continue but log warning
        }

        return true;
    }

    /**
     * Ends tick profiling and logs warning if budget was exceeded.
     * Call this at the end of each tick.
     */
    public void endTick() {
        tickProfiler.logWarningIfExceeded();
    }

    /**
     * Gets the elapsed time for the current tick in milliseconds.
     *
     * @return Elapsed time in milliseconds
     */
    public long getElapsedMs() {
        return tickProfiler.getElapsedMs();
    }

    /**
     * Starts tracking an execution sequence for skill learning.
     *
     * @param goal The goal description
     */
    public void startTracking(String goal) {
        this.currentGoal = goal;
        executionTracker.startTracking(foreman.getEntityName(), goal);
        LOGGER.debug("Started execution tracking for agent '{}' with goal: {}",
            foreman.getEntityName(), goal);
    }

    /**
     * Ends tracking an execution sequence.
     *
     * @param success Whether the sequence completed successfully
     */
    public void endTracking(boolean success) {
        if (executionTracker.isTracking(foreman.getEntityName())) {
            executionTracker.endTracking(foreman.getEntityName(), success);
            LOGGER.debug("Ended execution tracking for agent '{}' (success: {})",
                foreman.getEntityName(), success);
        }
        this.currentGoal = null;
    }

    /**
     * Records an action execution for skill learning.
     *
     * @param action The action that was executed
     * @param result The result of the action
     */
    public void recordAction(BaseAction action, ActionResult result) {
        executionTracker.recordAction(foreman.getEntityName(), action, result);
    }

    /**
     * Checks if currently tracking an execution sequence.
     *
     * @return true if tracking
     */
    public boolean isTracking() {
        return executionTracker.isTracking(foreman.getEntityName());
    }

    /**
     * Gets the current goal being tracked.
     *
     * @return Current goal, or null if not tracking
     */
    public String getCurrentGoal() {
        return currentGoal;
    }

    /**
     * Sets the current goal.
     *
     * @param goal The goal description
     */
    public void setCurrentGoal(String goal) {
        this.currentGoal = goal;
        foreman.getMemory().setCurrentGoal(goal);
    }

    /**
     * Gets the current action progress percentage (0-100).
     *
     * @param currentAction The currently executing action
     * @param isQueueEmpty Whether the task queue is empty
     * @return Progress percentage
     */
    public int getCurrentActionProgress(BaseAction currentAction, boolean isQueueEmpty) {
        if (currentAction == null) {
            return isQueueEmpty ? 100 : 0;
        }

        // Try to get progress from the action if it supports it
        if (currentAction instanceof com.minewright.action.actions.BuildStructureAction) {
            com.minewright.action.actions.BuildStructureAction buildAction =
                (com.minewright.action.actions.BuildStructureAction) currentAction;
            return buildAction.getProgressPercent();
        }

        // Default to 50% if action is in progress
        return currentAction.isComplete() ? 100 : 50;
    }

    /**
     * Gets the tick profiler for external access.
     *
     * @return TickProfiler instance
     */
    public TickProfiler getTickProfiler() {
        return tickProfiler;
    }

    /**
     * Gets the execution tracker for external access.
     *
     * @return ExecutionTracker instance
     */
    public ExecutionTracker getExecutionTracker() {
        return executionTracker;
    }
}
