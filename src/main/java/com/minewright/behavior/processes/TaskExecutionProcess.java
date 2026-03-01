package com.minewright.behavior.processes;

import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Medium-priority process for executing assigned tasks from the task queue.
 *
 * <p>This process is responsible for normal work operations - executing tasks
 * that have been planned by the LLM or assigned by the orchestration system.
 * It has medium priority (50) and will run when survival is not needed.</p>
 *
 * <p><b>Task Execution Conditions (canRun):</b></p>
 * <ul>
 *   <li>ActionExecutor has tasks in queue</li>
 *   <li>ActionExecutor is currently executing an action</li>
 *   <li>Agent has a current goal set</li>
 * </ul>
 *
 * <p><b>Task Execution Actions (tick):</b></p>
 * <ul>
 *   <li>Delegate to ActionExecutor.tick() for actual execution</li>
 *   <li>Monitor task progress</li>
 *   <li>Handle task completion or failure</li>
 * </ul>
 *
 * @see BehaviorProcess
 * @see com.minewright.action.ActionExecutor
 * @since 1.2.0
 */
public class TaskExecutionProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionProcess.class);

    /**
     * Priority level for task execution (medium - normal work).
     */
    private static final int PRIORITY = 50;

    /**
     * The foreman entity this process is managing.
     */
    private final ForemanEntity foreman;

    /**
     * The action executor that handles actual task execution.
     */
    private final ActionExecutor actionExecutor;

    /**
     * Whether this process is currently active.
     */
    private boolean active = false;

    /**
     * Ticks since process activation.
     */
    private int ticksActive = 0;

    /**
     * Creates a new TaskExecutionProcess for the given foreman.
     *
     * @param foreman The foreman entity to manage task execution for
     */
    public TaskExecutionProcess(ForemanEntity foreman) {
        this.foreman = foreman;
        this.actionExecutor = foreman.getActionExecutor();
    }

    @Override
    public String getName() {
        return "TaskExecution";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean canRun() {
        // Can run if there's work to do
        return hasTasks() || isExecuting();
    }

    @Override
    public void tick() {
        ticksActive++;

        // Delegate to ActionExecutor for actual execution
        // The ActionExecutor handles task queue, action lifecycle, etc.
        actionExecutor.tick();

        // Log progress every 100 ticks (5 seconds)
        if (ticksActive % 100 == 0) {
            String currentGoal = actionExecutor.getCurrentGoal();
            LOGGER.info("[{}] Task execution progress (ticks: {}, goal: {})",
                foreman.getEntityName(), ticksActive,
                currentGoal != null ? currentGoal : "none");
        }
    }

    @Override
    public void onActivate() {
        active = true;
        ticksActive = 0;

        LOGGER.info("[{}] Task execution activated",
            foreman.getEntityName());

        // Notify player of work in progress
        String currentGoal = actionExecutor.getCurrentGoal();
        if (currentGoal != null) {
            // Only log if there's meaningful work
            LOGGER.debug("[{}] Working on: {}",
                foreman.getEntityName(), currentGoal);
        }
    }

    @Override
    public void onDeactivate() {
        active = false;

        LOGGER.info("[{}] Task execution deactivated (was active for {} ticks)",
            foreman.getEntityName(), ticksActive);

        // Don't stop current action - let it complete naturally
        // The process can be reactivated if more tasks arrive
    }

    /**
     * Checks if there are tasks queued or currently executing.
     *
     * @return true if there is work to do
     */
    private boolean hasTasks() {
        // Check if ActionExecutor has tasks or is executing
        return actionExecutor.isExecuting();
    }

    /**
     * Checks if an action is currently being executed.
     *
     * @return true if an action is in progress
     */
    private boolean isExecuting() {
        return actionExecutor.getCurrentAction() != null;
    }

    /**
     * Gets the current goal being worked toward.
     *
     * @return The current goal description, or null if none
     */
    public String getCurrentGoal() {
        return actionExecutor.getCurrentGoal();
    }

    /**
     * Gets the number of ticks this process has been active.
     *
     * @return Ticks active
     */
    public int getTicksActive() {
        return ticksActive;
    }

    /**
     * Resets the active tick counter.
     */
    public void resetTicksActive() {
        ticksActive = 0;
    }
}
