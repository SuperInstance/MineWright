package com.minewright.action;

import com.minewright.exception.ActionException;

/**
 * Result of an action execution.
 *
 * <p>ActionResult provides structured feedback about action completion:
 * <ul>
 *   <li>Success/failure status</li>
 *   <li>Error code for categorization</li>
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
    private final ErrorCode errorCode;
    private final String message;
    private final boolean requiresReplanning;
    private final ActionException exception;
    private final String recoverySuggestion;
    private final long timestamp;

    public ActionResult(boolean success, String message) {
        this(success, ErrorCode.UNKNOWN, message, !success);
    }

    public ActionResult(boolean success, String message, boolean requiresReplanning) {
        this(success, ErrorCode.UNKNOWN, message, requiresReplanning);
    }

    public ActionResult(boolean success, ErrorCode errorCode, String message, boolean requiresReplanning) {
        this(success, errorCode, message, requiresReplanning, null, null);
    }

    public ActionResult(boolean success, ErrorCode errorCode, String message, boolean requiresReplanning,
                       ActionException exception, String recoverySuggestion) {
        this.success = success;
        this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN;
        this.message = message;
        this.requiresReplanning = requiresReplanning;
        this.exception = exception;
        this.recoverySuggestion = recoverySuggestion;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Gets the error code for this result.
     *
     * @return Error code, or SUCCESS if successful
     */
    public ErrorCode getErrorCode() {
        return success ? ErrorCode.SUCCESS : errorCode;
    }

    public boolean requiresReplanning() {
        return requiresReplanning;
    }

    /**
     * Gets the timestamp when this result was created.
     *
     * @return Milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the age of this result in milliseconds.
     *
     * @return Age in milliseconds
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
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
        return new ActionResult(true, ErrorCode.SUCCESS, message, false);
    }

    public static ActionResult failure(String message) {
        return failure(ErrorCode.UNKNOWN, message, true);
    }

    public static ActionResult failure(String message, boolean requiresReplanning) {
        return failure(ErrorCode.UNKNOWN, message, requiresReplanning);
    }

    public static ActionResult failure(ErrorCode errorCode, String message, boolean requiresReplanning) {
        return new ActionResult(false, errorCode, message, requiresReplanning, null, null);
    }

    /**
     * Creates a failure result from an ActionException.
     *
     * @param exception The exception that caused the failure
     * @return ActionResult with error details
     */
    public static ActionResult fromException(ActionException exception) {
        return fromException(exception, true);
    }

    /**
     * Creates a failure result from an ActionException with custom replanning flag.
     *
     * @param exception          The exception that caused the failure
     * @param requiresReplanning Whether replanning is needed
     * @return ActionResult with error details
     */
    public static ActionResult fromException(ActionException exception, boolean requiresReplanning) {
        ErrorCode errorCode = ErrorCode.fromExceptionErrorCode(exception.getErrorCode());
        return new ActionResult(
            false,
            errorCode,
            exception.getMessage(),
            requiresReplanning,
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
        return new ActionResult(false, ErrorCode.UNKNOWN, message, requiresReplanning,
            null, recoverySuggestion);
    }

    /**
     * Creates a timeout failure result.
     *
     * @param actionType The type of action that timed out
     * @param duration   The timeout duration
     * @return ActionResult with timeout error
     */
    public static ActionResult timeout(String actionType, String duration) {
        return new ActionResult(false, ErrorCode.TIMEOUT,
            "Action '" + actionType + "' timed out after " + duration,
            true,
            null,
            "The action took too long to complete. Try replanning with a different target or approach.");
    }

    /**
     * Creates a blocked failure result.
     *
     * @param actionType The type of action that was blocked
     * @param reason     Why it's blocked
     * @return ActionResult with blocked error
     */
    public static ActionResult blocked(String actionType, String reason) {
        return new ActionResult(false, ErrorCode.BLOCKED,
            "Action '" + actionType + "' is blocked: " + reason,
            false,
            null,
            "Clear the obstruction or try a different location/target.");
    }

    @Override
    public String toString() {
        return "ActionResult{" +
            "success=" + success +
            ", errorCode=" + errorCode +
            ", message='" + message + '\'' +
            ", requiresReplanning=" + requiresReplanning +
            ", hasException=" + (exception != null) +
            ", timestamp=" + timestamp +
            '}';
    }

    /**
     * Error codes for action results.
     *
     * <p>These codes categorize different types of failures to enable
     * appropriate recovery strategies.</p>
     */
    public enum ErrorCode {
        /** Action completed successfully. */
        SUCCESS(0, "success", ErrorRecoveryStrategy.RecoveryCategory.TRANSIENT),

        /** Unknown or uncategorized error. */
        UNKNOWN(1, "unknown", ErrorRecoveryStrategy.RecoveryCategory.PERMANENT),

        /** Action timed out. */
        TIMEOUT(2, "timeout", ErrorRecoveryStrategy.RecoveryCategory.TRANSIENT),

        /** Action blocked by obstacle. */
        BLOCKED(3, "blocked", ErrorRecoveryStrategy.RecoveryCategory.RECOVERABLE),

        /** Required resource not found. */
        NOT_FOUND(4, "not_found", ErrorRecoveryStrategy.RecoveryCategory.PERMANENT),

        /** Invalid action parameters. */
        INVALID_PARAMS(5, "invalid_params", ErrorRecoveryStrategy.RecoveryCategory.PERMANENT),

        /** Navigation or pathfinding failure. */
        NAVIGATION_FAILURE(6, "navigation_failure", ErrorRecoveryStrategy.RecoveryCategory.RECOVERABLE),

        /** Invalid action type. */
        INVALID_ACTION_TYPE(7, "invalid_action", ErrorRecoveryStrategy.RecoveryCategory.PERMANENT),

        /** Execution error (runtime exception). */
        EXECUTION_ERROR(8, "execution_error", ErrorRecoveryStrategy.RecoveryCategory.RECOVERABLE),

        /** Resource unavailable (temporarily). */
        RESOURCE_UNAVAILABLE(9, "resource_unavailable", ErrorRecoveryStrategy.RecoveryCategory.TRANSIENT),

        /** Permission denied. */
        PERMISSION_DENIED(10, "permission_denied", ErrorRecoveryStrategy.RecoveryCategory.PERMANENT),

        /** State corruption or invalid state. */
        INVALID_STATE(11, "invalid_state", ErrorRecoveryStrategy.RecoveryCategory.CRITICAL);

        private final int code;
        private final String name;
        private final ErrorRecoveryStrategy.RecoveryCategory recoveryCategory;

        ErrorCode(int code, String name, ErrorRecoveryStrategy.RecoveryCategory recoveryCategory) {
            this.code = code;
            this.name = name;
            this.recoveryCategory = recoveryCategory;
        }

        /**
         * Gets the numeric error code.
         *
         * @return Error code number
         */
        public int getCode() {
            return code;
        }

        /**
         * Gets the error name.
         *
         * @return Error name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the recovery category for this error code.
         *
         * @return Recovery category
         */
        public ErrorRecoveryStrategy.RecoveryCategory getRecoveryCategory() {
            return recoveryCategory;
        }

        /**
         * Maps an ActionException error code to ActionResult error code.
         *
         * @param exceptionErrorCode ActionException error code
         * @return Mapped ActionResult error code
         */
        public static ErrorCode fromExceptionErrorCode(com.minewright.exception.MineWrightException.ErrorCode exceptionErrorCode) {
            if (exceptionErrorCode == null) {
                return UNKNOWN;
            }

            // Map by error code number
            return switch (exceptionErrorCode.getCode()) {
                case 2003 -> TIMEOUT;          // ACTION_TIMEOUT
                case 2005 -> BLOCKED;          // ACTION_BLOCKED
                case 2000 -> INVALID_ACTION_TYPE; // ACTION_UNKNOWN_TYPE
                case 2001 -> INVALID_PARAMS;   // ACTION_INVALID_PARAMS
                case 2002 -> EXECUTION_ERROR;  // ACTION_EXECUTION_FAILED
                default -> UNKNOWN;
            };
        }
    }
}

