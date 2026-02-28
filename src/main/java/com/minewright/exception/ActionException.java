package com.minewright.exception;

/**
 * Exception thrown when action execution fails.
 *
 * <p>Action exceptions can occur due to:
 * <ul>
 *   <li>Unknown or unsupported action types</li>
 *   <li>Invalid action parameters</li>
 *   <li>Execution failures during action tick</li>
 *   <li>Action timeouts or cancellations</li>
 * </ul>
 *
 * <p><b>Recovery:</b> Most action failures can be recovered by:
 * <ul>
 *   <li>Replanning the task with the LLM</li>
 *   <li>Adjusting action parameters</li>
 *   <li>Skipping the failed action if non-critical</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class ActionException extends MineWrightException {
    private static final long serialVersionUID = 1L;

    private final String actionType;
    private final boolean requiresReplanning;

    /**
     * Constructs a new ActionException.
     *
     * @param message            Error message
     * @param actionType         The type of action that failed
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     */
    public ActionException(String message, String actionType, ErrorCode errorCode,
                          String recoverySuggestion) {
        super(message, errorCode, recoverySuggestion);
        this.actionType = actionType;
        this.requiresReplanning = true;
    }

    /**
     * Constructs a new ActionException with cause.
     *
     * @param message            Error message
     * @param actionType         The type of action that failed
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param cause              Underlying cause
     */
    public ActionException(String message, String actionType, ErrorCode errorCode,
                          String recoverySuggestion, Throwable cause) {
        super(message, errorCode, recoverySuggestion, null, cause);
        this.actionType = actionType;
        this.requiresReplanning = true;
    }

    /**
     * Constructs a new ActionException with full control.
     *
     * @param message            Error message
     * @param actionType         The type of action that failed
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param requiresReplanning Whether the task needs replanning
     * @param cause              Underlying cause
     */
    public ActionException(String message, String actionType, ErrorCode errorCode,
                          String recoverySuggestion, boolean requiresReplanning, Throwable cause) {
        super(message, errorCode, recoverySuggestion, null, cause);
        this.actionType = actionType;
        this.requiresReplanning = requiresReplanning;
    }

    /**
     * Returns the action type that failed.
     *
     * @return Action type (e.g., "mine", "place", "pathfind")
     */
    public String getActionType() {
        return actionType;
    }

    /**
     * Returns whether this action failure requires task replanning.
     *
     * @return true if replanning is recommended
     */
    public boolean requiresReplanning() {
        return requiresReplanning;
    }

    // Static factory methods for common errors

    /**
     * Creates an exception for unknown action types.
     *
     * @param actionType The unknown action type
     * @return ActionException instance
     */
    public static ActionException unknownAction(String actionType) {
        return new ActionException(
            "Unknown action type: " + actionType,
            actionType,
            ErrorCode.ACTION_UNKNOWN_TYPE,
            "The action '" + actionType + "' is not recognized. " +
            "Available actions: mine, place, pathfind, craft, attack, follow, gather, build. " +
            "The task should be replanned with valid action types.",
            true,
            null
        );
    }

    /**
     * Creates an exception for invalid action parameters.
     *
     * @param actionType The action type
     * @param param      The invalid parameter
     * @param reason     Why it's invalid
     * @return ActionException instance
     */
    public static ActionException invalidParameter(String actionType, String param, String reason) {
        return new ActionException(
            "Invalid parameter '" + param + "' for action '" + actionType + "': " + reason,
            actionType,
            ErrorCode.ACTION_INVALID_PARAMS,
            "Check the action parameters and ensure they match the expected format. " +
            "The task should be replanned with correct parameters.",
            true,
            null
        );
    }

    /**
     * Creates an exception for action execution failures.
     *
     * @param actionType The action type
     * @param reason     The failure reason
     * @param cause      Underlying cause
     * @return ActionException instance
     */
    public static ActionException executionFailed(String actionType, String reason, Throwable cause) {
        return new ActionException(
            "Action '" + actionType + "' execution failed: " + reason,
            actionType,
            ErrorCode.ACTION_EXECUTION_FAILED,
            "The action encountered an error during execution. " +
            "Try replanning the task or adjusting the action parameters.",
            true,
            cause
        );
    }

    /**
     * Creates an exception for action timeouts.
     *
     * @param actionType The action type that timed out
     * @param duration   How long it ran before timeout
     * @return ActionException instance
     */
    public static ActionException timeout(String actionType, String duration) {
        return new ActionException(
            "Action '" + actionType + "' timed out after " + duration,
            actionType,
            ErrorCode.ACTION_TIMEOUT,
            "The action took too long to complete. " +
            "This could be due to the target being unreachable, blocked, or too far away. " +
            "Try replanning with a different target or approach.",
            true,
            null
        );
    }

    /**
     * Creates an exception for blocked actions (e.g., can't place block).
     *
     * @param actionType The action type
     * @param reason     Why it's blocked
     * @return ActionException instance
     */
    public static ActionException blocked(String actionType, String reason) {
        return new ActionException(
            "Action '" + actionType + "' is blocked: " + reason,
            actionType,
            ErrorCode.ACTION_BLOCKED,
            "The action cannot proceed due to an obstruction. " +
            "Clear the obstruction or try a different location/target. " +
            "Non-critical actions can sometimes be skipped.",
            false,
            null
        );
    }

    @Override
    public String toString() {
        return String.format("ActionException[actionType='%s', requiresReplanning=%s, code=%d, message='%s']",
            actionType, requiresReplanning, getErrorCode().getCode(), getMessage());
    }
}
