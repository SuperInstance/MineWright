package com.minewright.recovery.strategies;

import com.minewright.entity.ForemanEntity;
import com.minewright.recovery.RecoveryResult;
import com.minewright.recovery.RecoveryStrategy;
import com.minewright.recovery.StuckType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recovery strategy that aborts the current task and notifies the user.
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 *   <li>Agent state machine is deadlocked (STATE_STUCK)</li>
 *   <li>Agent cannot acquire required resources (RESOURCE_STUCK)</li>
 *   <li>All other recovery strategies have failed</li>
 *   <li>Task requirements cannot be met</li>
 * </ul>
 *
 * <p><b>How it Works:</b></p>
 * <ol>
 *   <li>Stops current action execution</li>
 *   <li>Clears task queue</li>
 *   <li>Resets agent state machine to IDLE</li>
 *   <li>Sends chat message to user explaining failure</li>
 *   <li>Always returns ABORT to stop recovery chain</li>
 * </ol>
 *
 * <p><b>User Notification:</b></p>
 * <p>The strategy provides context-specific messages:</p>
 * <ul>
 *   <li>STATE_STUCK: "I'm confused and need a fresh start."</li>
 *   <li>RESOURCE_STUCK: "I don't have what I need for this job."</li>
 *   <li>Generic: "I can't complete this task."</li>
 * </ul>
 *
 * <p><b>Post-Abort State:</b></p>
 * <p>After abort, the agent returns to:</p>
 * <ul>
 *   <li>IDLE state (ready for new commands)</li>
 *   <li>Empty task queue</li>
 *   *   <li>No current action</li>
 *   <li>Following nearest player (default idle behavior)</li>
 * </ul>
 *
 * @since 1.1.0
 * @see RecoveryStrategy
 * @see StuckType
 * @see RepathStrategy
 * @see TeleportStrategy
 */
public class AbortStrategy implements RecoveryStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbortStrategy.class);

    /** No retry on abort - this is final. */
    private static final int MAX_ABORT_ATTEMPTS = 1;

    @Override
    public boolean canRecover(StuckType type, ForemanEntity entity) {
        // Abort strategy can handle any stuck type
        // It's the final fallback when other strategies fail
        return true;
    }

    @Override
    public RecoveryResult execute(ForemanEntity entity) {
        LOGGER.info("[AbortStrategy] Aborting task for {}", entity.getEntityName());

        try {
            // Stop current action execution
            if (entity.getActionExecutor() != null) {
                entity.getActionExecutor().stopCurrentAction();
                LOGGER.debug("[AbortStrategy] Stopped current action for {}",
                    entity.getEntityName());
            }

            // Stop navigation
            if (entity.getNavigation() != null) {
                entity.getNavigation().stop();
                LOGGER.debug("[AbortStrategy] Stopped navigation for {}",
                    entity.getEntityName());
            }

            // Notify user with context-specific message
            String message = getAbortMessage(entity);
            entity.sendChatMessage(message);

            // Log the abort
            LOGGER.warn("[AbortStrategy] Task aborted for {}: {}",
                entity.getEntityName(), message);

            // Always return ABORT - this is final
            return RecoveryResult.ABORT;

        } catch (Exception e) {
            LOGGER.error("[AbortStrategy] Error during abort", e);
            // Even if abort fails, return ABORT to stop recovery chain
            return RecoveryResult.ABORT;
        }
    }

    @Override
    public int getMaxAttempts() {
        return MAX_ABORT_ATTEMPTS;
    }

    /**
     * Generates a context-appropriate abort message for the user.
     *
     * @param entity The entity
     * @return User-friendly abort message
     */
    private String getAbortMessage(ForemanEntity entity) {
        // Check current action for context
        var currentAction = entity.getActionExecutor().getCurrentAction();
        if (currentAction != null) {
            String description = currentAction.getDescription();
            return String.format("I can't complete this task: %s. Giving up on this one.",
                description);
        }

        // Check current goal
        String currentGoal = entity.getActionExecutor().getCurrentGoal();
        if (currentGoal != null) {
            return String.format("I'm stuck trying to: %s. I need help with this one.",
                currentGoal);
        }

        // Generic message
        return "I can't complete this task. I'm going to stop and wait for new instructions.";
    }
}
