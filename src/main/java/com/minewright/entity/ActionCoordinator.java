package com.minewright.entity;

import com.minewright.action.ActionExecutor;
import com.minewright.behavior.ProcessManager;
import com.minewright.recovery.RecoveryManager;
import com.minewright.recovery.RecoveryResult;
import com.minewright.recovery.StuckDetector;
import com.minewright.recovery.StuckType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates action execution for a ForemanEntity.
 *
 * <p>This class manages the tick-based execution of actions and behaviors:
 * <ul>
 *   <li>Process arbitration via ProcessManager</li>
 *   <li>Stuck detection and recovery</li>
 *   <li>Error recovery with graceful degradation</li>
 *   <li>Fallback to legacy ActionExecutor when needed</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is designed to be called from the
 * Minecraft server thread only (single-threaded execution).</p>
 *
 * @since 1.0.0
 */
public class ActionCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionCoordinator.class);

    private final ForemanEntity entity;
    private final EntityState state;

    /**
     * Counter for consecutive errors in action executor - used for recovery.
     */
    private int errorRecoveryTicks = 0;

    /**
     * Creates a new ActionCoordinator.
     *
     * @param entity The ForemanEntity this coordinator belongs to
     * @param state The entity's state
     */
    public ActionCoordinator(ForemanEntity entity, EntityState state) {
        this.entity = entity;
        this.state = state;
    }

    /**
     * Main tick method for action coordination.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Executes behaviors via ProcessManager (preferred)</li>
     *   <li>Falls back to ActionExecutor if ProcessManager unavailable</li>
     *   <li>Detects and recovers from stuck conditions</li>
     *   <li>Handles errors with graceful degradation</li>
     * </ol>
     */
    public void tick() {
        ProcessManager processManager = state.getProcessManager();
        ActionExecutor actionExecutor = state.getActionExecutor();

        // Execute behaviors via ProcessManager - NEW approach
        if (processManager != null) {
            try {
                processManager.tick();
                errorRecoveryTicks = 0; // Reset error counter on success
            } catch (Exception e) {
                LOGGER.error("[{}] Critical error in process manager",
                    state.getEntityName(), e);
                errorRecoveryTicks++;

                // Only send chat message once per error burst (not every tick)
                if (errorRecoveryTicks == 1) {
                    try {
                        entity.sendChatMessage("Hit a snag there boss. Working on it...");
                    } catch (Exception ignored) {
                        // If chat fails too, just log and continue
                    }
                }

                // After 3 consecutive errors, reset the process manager to recover
                if (errorRecoveryTicks >= 3) {
                    LOGGER.warn("[{}] Too many errors, resetting process manager",
                        state.getEntityName());
                    try {
                        processManager.forceDeactivate();
                        ProcessManager newManager = new ProcessManager(entity);
                        initializeProcesses(newManager);
                        state.setProcessManager(newManager);
                        errorRecoveryTicks = 0;
                        entity.sendChatMessage("Alright, I'm back on track now.");
                    } catch (Exception resetError) {
                        LOGGER.error("[{}] Failed to reset process manager",
                            state.getEntityName(), resetError);
                    }
                }
            }
        } else {
            // Fallback to old action executor if process manager not available
            try {
                actionExecutor.tick();
            } catch (Exception e) {
                LOGGER.error("[{}] Critical error in action executor (fallback)",
                    state.getEntityName(), e);
            }
        }

        // Stuck detection and recovery - NEW feature
        StuckDetector stuckDetector = state.getStuckDetector();
        RecoveryManager recoveryManager = state.getRecoveryManager();

        if (stuckDetector != null && recoveryManager != null) {
            try {
                detectAndRecoverFromStuck(stuckDetector, recoveryManager, actionExecutor);
            } catch (Exception e) {
                LOGGER.warn("[{}] Stuck detection/recovery error (continuing anyway)",
                    state.getEntityName(), e);
            }
        }
    }

    /**
     * Initializes all behavior processes for the process manager.
     *
     * @param processManager The process manager to initialize
     */
    private void initializeProcesses(ProcessManager processManager) {
        String entityName = state.getEntityName();

        if (processManager == null) {
            LOGGER.warn("[{}] ProcessManager not initialized, cannot register processes", entityName);
            return;
        }

        try {
            // Import process classes to avoid fully qualified names
            com.minewright.behavior.processes.SurvivalProcess survivalProcess =
                new com.minewright.behavior.processes.SurvivalProcess(entity);
            com.minewright.behavior.processes.TaskExecutionProcess taskExecutionProcess =
                new com.minewright.behavior.processes.TaskExecutionProcess(entity);
            com.minewright.behavior.processes.FollowProcess followProcess =
                new com.minewright.behavior.processes.FollowProcess(entity);
            com.minewright.behavior.processes.IdleProcess idleProcess =
                new com.minewright.behavior.processes.IdleProcess(entity);

            // Register processes in priority order (highest first)
            processManager.registerProcess(survivalProcess);
            processManager.registerProcess(taskExecutionProcess);
            processManager.registerProcess(followProcess);
            processManager.registerProcess(idleProcess);

            LOGGER.info("[{}] Registered {} behavior processes",
                entityName, processManager.getProcessCount());
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to initialize behavior processes", entityName, e);
        }
    }

    /**
     * Detects stuck conditions and attempts recovery.
     *
     * @param stuckDetector The stuck detector to use
     * @param recoveryManager The recovery manager to use
     * @param actionExecutor The action executor for stopping actions
     */
    private void detectAndRecoverFromStuck(StuckDetector stuckDetector,
                                          RecoveryManager recoveryManager,
                                          ActionExecutor actionExecutor) {
        // Update stuck detection
        boolean detected = stuckDetector.tickAndDetect();

        if (!detected) {
            return; // Not stuck
        }

        // Determine stuck type
        StuckType stuckType = stuckDetector.detectStuck();
        if (stuckType == null) {
            return; // No specific stuck type detected
        }

        LOGGER.warn("[{}] Stuck detected: {} at position {}",
            state.getEntityName(), stuckType, entity.blockPosition());

        // Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        switch (result) {
            case SUCCESS:
                LOGGER.info("[{}] Recovery successful from {}",
                    state.getEntityName(), stuckType);
                stuckDetector.reset();
                entity.sendChatMessage("Phew, got unstuck!");
                break;

            case RETRY:
                // Will retry next tick
                LOGGER.debug("[{}] Recovery retrying for {}",
                    state.getEntityName(), stuckType);
                break;

            case ESCALATE:
                LOGGER.info("[{}] Recovery escalating for {}",
                    state.getEntityName(), stuckType);
                break;

            case ABORT:
                LOGGER.warn("[{}] Recovery aborted for {}, giving up",
                    state.getEntityName(), stuckType);
                stuckDetector.reset();
                actionExecutor.stopCurrentAction();
                entity.sendChatMessage("I'm stuck and can't complete this task.");
                break;
        }
    }

    /**
     * Gets the error recovery tick counter.
     *
     * @return Current error recovery tick count
     */
    public int getErrorRecoveryTicks() {
        return errorRecoveryTicks;
    }

    /**
     * Resets the error recovery tick counter.
     */
    public void resetErrorRecoveryTicks() {
        this.errorRecoveryTicks = 0;
    }
}
