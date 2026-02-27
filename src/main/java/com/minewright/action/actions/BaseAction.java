package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.exception.ActionException;

/**
 * Base class for all actions executed by crew members.
 *
 * <p>Actions follow a lifecycle:
 * <ol>
 *   <li>Created with ForemanEntity and Task</li>
 *   <li>start() called to initialize</li>
 *   <li>tick() called each game tick until complete</li>
 *   <li>cancel() called if action needs to stop early</li>
 * </ol>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Exceptions during tick are caught and converted to ActionResult</li>
 *   <li>ActionException provides detailed error context and recovery suggestions</li>
 *   <li>Errors never crash the game - graceful degradation</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Actions are ticked on the main game thread.
 * Do not block in onTick() - all long-running operations must be async.</p>
 */
public abstract class BaseAction {
    protected final ForemanEntity foreman;
    protected final Task task;
    protected ActionResult result;
    protected boolean started = false;
    protected boolean cancelled = false;
    protected String errorContext;

    public BaseAction(ForemanEntity foreman, Task task) {
        this.foreman = foreman;
        this.task = task;
    }

    /**
     * Starts the action. Should be called once after creation.
     * Validates parameters and initializes any required state.
     */
    public void start() {
        if (started) {
            MineWrightMod.LOGGER.warn("[{}] Action already started: {}", foreman.getEntityName(), getDescription());
            return;
        }
        started = true;
        MineWrightMod.LOGGER.debug("[{}] Starting action: {}", foreman.getEntityName(), getDescription());
        try {
            onStart();
        } catch (ActionException e) {
            handleActionException(e);
        } catch (Exception e) {
            handleUnexpectedException(e, "action start");
        }
    }

    /**
     * Ticks the action. Called each game tick.
     * Errors are caught and converted to failure results.
     */
    public void tick() {
        if (!started || isComplete()) return;

        try {
            onTick();
        } catch (ActionException e) {
            handleActionException(e);
        } catch (Exception e) {
            handleUnexpectedException(e, "action tick");
        }
    }

    /**
     * Cancels the action. Stops execution and marks as failed.
     */
    public void cancel() {
        if (cancelled) {
            MineWrightMod.LOGGER.debug("[{}] Action already cancelled: {}", foreman.getEntityName(), getDescription());
            return;
        }
        cancelled = true;
        result = ActionResult.failure("Action cancelled", false);
        MineWrightMod.LOGGER.info("[{}] Cancelling action: {}", foreman.getEntityName(), getDescription());
        try {
            onCancel();
        } catch (Exception e) {
            MineWrightMod.LOGGER.warn("[{}] Error during action cancellation: {}",
                foreman.getEntityName(), getDescription(), e);
        }
    }

    /**
     * Returns whether the action has completed (successfully or failed).
     */
    public boolean isComplete() {
        return result != null || cancelled;
    }

    /**
     * Returns the result of the action, or null if not yet complete.
     */
    public ActionResult getResult() {
        return result;
    }

    /**
     * Returns the error context if an error occurred.
     *
     * @return Error context, or null if no error
     */
    public String getErrorContext() {
        return errorContext;
    }

    /**
     * Handles an ActionException by converting it to an ActionResult.
     *
     * @param e The exception to handle
     */
    protected void handleActionException(ActionException e) {
        MineWrightMod.LOGGER.error("[{}] Action error [{}]: {}",
            foreman.getEntityName(), e.getActionType(), e.getMessage());

        this.result = ActionResult.fromException(e);
        this.errorContext = e.getContext();

        // Notify entity of the failure
        if (foreman != null) {
            foreman.notifyTaskFailed(getDescription(), e.getMessage());
        }
    }

    /**
     * Handles an unexpected exception by wrapping it in an ActionException.
     *
     * @param e         The unexpected exception
     * @param phase     The action phase where it occurred
     */
    protected void handleUnexpectedException(Exception e, String phase) {
        MineWrightMod.LOGGER.error("[{}] Unexpected error during {}: {}",
            foreman.getEntityName(), phase, e.getMessage(), e);

        String actionType = task != null ? task.getAction() : "unknown";
        ActionException wrapped = ActionException.executionFailed(
            actionType,
            "Unexpected error during " + phase + ": " + e.getClass().getSimpleName(),
            e
        );

        this.result = ActionResult.fromException(wrapped);
    }

    /**
     * Handles action cancellation due to interruption.
     *
     * @param reason The cancellation reason
     */
    protected void handleCancellation(String reason) {
        MineWrightMod.LOGGER.info("[{}] Action cancelled: {}", foreman.getEntityName(), reason);
        this.result = ActionResult.failure(reason, false);
        this.cancelled = true;
    }

    /**
     * Sets a successful result for this action.
     *
     * @param message Success message
     */
    protected void succeed(String message) {
        this.result = ActionResult.success(message);
    }

    /**
     * Sets a failed result for this action.
     *
     * @param message            Failure message
     * @param requiresReplanning Whether replanning is needed
     */
    protected void fail(String message, boolean requiresReplanning) {
        this.result = ActionResult.failure(message, requiresReplanning);
    }

    /**
     * Sets a failed result with recovery suggestion.
     *
     * @param message            Failure message
     * @param requiresReplanning Whether replanning is needed
     * @param recoverySuggestion Recovery suggestion for the user
     */
    protected void failWithRecovery(String message, boolean requiresReplanning, String recoverySuggestion) {
        this.result = ActionResult.failureWithRecovery(message, requiresReplanning, recoverySuggestion);
    }

    /**
     * Validates a required parameter from the task.
     *
     * @param param     Parameter name
     * @param paramName Display name for error messages
     * @throws ActionException if parameter is missing
     */
    protected void requireParameter(String param, String paramName) throws ActionException {
        if (task == null || task.getStringParameter(param) == null) {
            throw ActionException.invalidParameter(
                task != null ? task.getAction() : "unknown",
                param,
                "Missing required parameter: " + paramName
            );
        }
    }

    /**
     * Gets the action type for error reporting.
     *
     * @return Action type string
     */
    protected String getActionType() {
        return task != null ? task.getAction() : "unknown";
    }

    protected abstract void onStart();
    protected abstract void onTick();
    protected abstract void onCancel();

    public abstract String getDescription();
}

