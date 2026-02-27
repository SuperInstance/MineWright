package com.minewright.action;

import com.minewright.exception.ActionException;

/**
 * Result of an action execution.
 *
 * <p>ActionResult provides structured feedback about action completion:
 * <ul>
 *   <li>Success/failure status</li>
 *   <li>Descriptive message</li>
 *   <li>Whether replanning is needed</li>
 *   <li>Optional exception for detailed error context</li>
 *   <li>Recovery suggestions for users</li>
 * </ul>
 *
 * <p><b>Error Recovery:</b> When an action fails, the recovery suggestion
 * can be displayed to users to help them understand what went wrong
 * and how to fix it.</p>
 *
 * @since 1.0.0
 */
public class ActionResult {
    private final boolean success;
    private final String message;
    private final boolean requiresReplanning;
    private final ActionException exception;
    private final String recoverySuggestion;

    public ActionResult(boolean success, String message) {
        this(success, message, !success);
    }

    public ActionResult(boolean success, String message, boolean requiresReplanning) {
        this(success, message, requiresReplanning, null, null);
    }

    public ActionResult(boolean success, String message, boolean requiresReplanning,
                       ActionException exception, String recoverySuggestion) {
        this.success = success;
        this.message = message;
        this.requiresReplanning = requiresReplanning;
        this.exception = exception;
        this.recoverySuggestion = recoverySuggestion;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public boolean requiresReplanning() {
        return requiresReplanning;
    }

    /**
     * Returns the exception that caused this action to fail, if any.
     *
     * @return The exception, or null if the action succeeded or failed without an exception
     */
    public ActionException getException() {
        return exception;
    }

    /**
     * Returns a recovery suggestion for this failure.
     *
     * @return Recovery suggestion, or null if none available
     */
    public String getRecoverySuggestion() {
        return recoverySuggestion;
    }

    /**
     * Returns a user-friendly message including recovery suggestion.
     *
     * @return Formatted message
     */
    public String getFormattedMessage() {
        if (success || recoverySuggestion == null) {
            return message;
        }
        return message + "\n\n" + recoverySuggestion;
    }

    public static ActionResult success(String message) {
        return new ActionResult(true, message, false);
    }

    public static ActionResult failure(String message) {
        return failure(message, true);
    }

    public static ActionResult failure(String message, boolean requiresReplanning) {
        return new ActionResult(false, message, requiresReplanning, null, null);
    }

    /**
     * Creates a failure result from an ActionException.
     *
     * @param exception The exception that caused the failure
     * @return ActionResult with error details
     */
    public static ActionResult fromException(ActionException exception) {
        return new ActionResult(
            false,
            exception.getMessage(),
            exception.requiresReplanning(),
            exception,
            exception.getRecoverySuggestion()
        );
    }

    /**
     * Creates a failure result with a recovery suggestion.
     *
     * @param message            Error message
     * @param requiresReplanning Whether replanning is needed
     * @param recoverySuggestion Suggestion for recovery
     * @return ActionResult with recovery information
     */
    public static ActionResult failureWithRecovery(String message, boolean requiresReplanning,
                                                   String recoverySuggestion) {
        return new ActionResult(false, message, requiresReplanning, null, recoverySuggestion);
    }

    @Override
    public String toString() {
        return "ActionResult{" +
            "success=" + success +
            ", message='" + message + '\'' +
            ", requiresReplanning=" + requiresReplanning +
            ", hasException=" + (exception != null) +
            '}';
    }
}

