package com.minewright.action;

import com.minewright.action.actions.BaseAction;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.voice.VoiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles errors and recovery for action execution.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Action result processing and error classification</li>
 *   <li>Automatic recovery strategy selection</li>
 *   <li>User notification via chat and GUI</li>
 *   <li>Recovery suggestion logging</li>
 * </ul>
 *
 * <p><b>Error Handling:</b></p>
 * <ul>
 *   <li><b>Structured Error Codes:</b> All errors categorized with specific error codes</li>
 *   <li><b>Recovery Strategies:</b> Automatic recovery based on error category</li>
 *   <li><b>User Feedback:</b> Clear error messages and recovery suggestions</li>
 *   <li><b>Structured Logging:</b> Detailed logging for debugging and monitoring</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ActionErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionErrorHandler.class);

    /**
     * The foreman entity for error notifications.
     */
    private final ForemanEntity foreman;

    /**
     * Creates a new action error handler.
     *
     * @param foreman The foreman entity to handle errors for
     */
    public ActionErrorHandler(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Handles action results with error recovery.
     *
     * @param result The action result
     * @param task   The task that was executed
     * @param currentAction The current action (for memory updates)
     */
    public void handleActionResult(ActionResult result, com.minewright.action.Task task,
                                   BaseAction currentAction) {
        if (result.isSuccess()) {
            handleSuccess(result, task, currentAction);
        } else {
            handleFailure(result, task);
        }
    }

    /**
     * Handles successful action completion.
     *
     * @param result The successful action result
     * @param task   The task that was executed
     * @param currentAction The current action (for memory updates)
     */
    private void handleSuccess(ActionResult result, com.minewright.action.Task task,
                              BaseAction currentAction) {
        LOGGER.info("[{}] Action completed successfully: {}",
            foreman.getEntityName(), result.getMessage());

        String description = (currentAction != null) ? currentAction.getDescription() :
            (task != null ? task.getAction() : "unknown");
        foreman.getMemory().addAction(description);
    }

    /**
     * Handles action failure with recovery strategy.
     *
     * @param result The failed action result
     * @param task   The task that was executed
     */
    private void handleFailure(ActionResult result, com.minewright.action.Task task) {
        // Handle failure with error recovery strategy
        ErrorRecoveryStrategy recoveryStrategy = ErrorRecoveryStrategy.fromResult(result);

        LOGGER.warn("[{}] Action failed [{}]: {}",
            foreman.getEntityName(), result.getErrorCode(), result.getMessage());

        // Attempt recovery
        boolean canRecover = recoveryStrategy.attemptRecovery(foreman, result);

        // Notify player via chat
        foreman.sendChatMessage("Job hit a snag: " + result.getMessage());

        // Show in GUI if enabled
        if (MineWrightConfig.ENABLE_CHAT_RESPONSES.get()) {
            sendToGUI(foreman.getEntityName(), "Problem: " + result.getMessage());
        }

        // Log recovery suggestion if available
        if (result.getRecoverySuggestion() != null) {
            LOGGER.info("[{}] Recovery suggestion: {}",
                foreman.getEntityName(), result.getRecoverySuggestion());
        }

        // Check if replanning is needed
        if (result.requiresReplanning() &&
            result.getErrorCode().getRecoveryCategory() !=
                ErrorRecoveryStrategy.RecoveryCategory.PERMANENT) {
            LOGGER.info("[{}] Task requires replanning due to recoverable error",
                foreman.getEntityName());
        }
    }

    /**
     * Handles action creation errors.
     *
     * @param task The task that failed
     */
    public void handleTaskCreationError(com.minewright.action.Task task) {
        String errorMsg = "Unknown action type: " + task.getAction();
        LOGGER.error("[{}] FAILED to create action for task: {}",
            foreman.getEntityName(), task);

        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.INVALID_ACTION_TYPE,
            errorMsg,
            true
        );

        handleActionResult(result, task, null);
    }

    /**
     * Handles unexpected execution errors.
     *
     * @param task The task being executed
     * @param e   The exception
     */
    public void handleExecutionError(com.minewright.action.Task task, Exception e) {
        LOGGER.error("[{}] Execution error for task {}: {}",
            foreman.getEntityName(), task.getAction(), e.getMessage(), e);

        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.EXECUTION_ERROR,
            "Execution error: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
            true
        );

        handleActionResult(result, task, null);
    }

    /**
     * Handles planning errors.
     *
     * @param command The command that failed to plan
     * @param e The exception
     */
    public void handlePlanningError(String command, Exception e) {
        LOGGER.error("Error starting async planning for command: {}", command, e);
        sendToGUI(foreman.getEntityName(),
            "Something went wrong with the planning! Try again in a moment.");
    }

    /**
     * Handles planning cancellation.
     *
     * @param command The command that was cancelled
     */
    public void handlePlanningCancellation(String command) {
        LOGGER.info("Planning was cancelled for command: {}", command);
        sendToGUI(foreman.getEntityName(), "Planning cancelled. Back to work!");
    }

    /**
     * Handles planning failure.
     *
     * @param command The command that failed
     * @param cause The cause of the failure
     */
    public void handlePlanningFailure(String command, Throwable cause) {
        LOGGER.error("Planning failed for command: {}", command, cause);
        sendToGUI(foreman.getEntityName(),
            "Something went wrong with the planning! Let's try that again.");
    }

    /**
     * Send a message to the GUI pane (client-side only, no chat spam).
     *
     * @param foremanName The name of the foreman
     * @param message The message to send
     */
    private void sendToGUI(String foremanName, String message) {
        if (foreman.level().isClientSide) {
            com.minewright.client.ForemanOfficeGUI.addCrewMessage(foremanName, message);
        }

        // Also speak the message if voice is enabled
        VoiceManager.getInstance().speakIfEnabled(message);
    }

    /**
     * Checks if an error is recoverable.
     *
     * @param errorCode The error code to check
     * @return true if recoverable
     */
    public boolean isRecoverable(ActionResult.ErrorCode errorCode) {
        return errorCode.getRecoveryCategory() !=
            ErrorRecoveryStrategy.RecoveryCategory.PERMANENT;
    }

    /**
     * Checks if an error requires replanning.
     *
     * @param result The action result to check
     * @return true if replanning is required
     */
    public boolean requiresReplanning(ActionResult result) {
        return result.requiresReplanning() &&
            result.getErrorCode().getRecoveryCategory() !=
                ErrorRecoveryStrategy.RecoveryCategory.PERMANENT;
    }
}
