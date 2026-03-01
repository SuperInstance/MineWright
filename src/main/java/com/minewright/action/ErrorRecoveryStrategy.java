package com.minewright.action;

import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy for recovering from action execution errors.
 *
 * <p>Error recovery strategies define how the system should respond
 * to different types of failures, enabling automatic recovery from
 * transient issues while gracefully handling permanent failures.</p>
 *
 * <p><b>Recovery Categories:</b></p>
 * <ul>
 *   <li><b>TRANSIENT:</b> Temporary issues (network, temporary blocks) - retry</li>
 *   <li><b>RECOVERABLE:</b> Can be fixed with state changes - attempt recovery</li>
 *   <li><b>PERMANENT:</b> Cannot be fixed - fail gracefully</li>
 *   <li><b>CRITICAL:</b> System-level failure - immediate abort</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ErrorRecoveryStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorRecoveryStrategy.class);

    private final RecoveryCategory category;
    private final RetryPolicy retryPolicy;
    private final String description;

    private ErrorRecoveryStrategy(RecoveryCategory category, RetryPolicy retryPolicy, String description) {
        this.category = category;
        this.retryPolicy = retryPolicy;
        this.description = description;
    }

    /**
     * Gets the recovery category.
     *
     * @return Recovery category
     */
    public RecoveryCategory getCategory() {
        return category;
    }

    /**
     * Gets the retry policy for this error type.
     *
     * @return Retry policy (null if no retries)
     */
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Gets the description of this recovery strategy.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this error should be retried.
     *
     * @return true if retry is allowed
     */
    public boolean shouldRetry() {
        return retryPolicy != null && retryPolicy.getMaxAttempts() > 1;
    }

    /**
     * Creates a recovery strategy from an ActionResult.
     *
     * @param result The action result
     * @return Recovery strategy
     */
    public static ErrorRecoveryStrategy fromResult(ActionResult result) {
        if (result.isSuccess()) {
            return TRANSIENT_SUCCESS;
        }

        // Determine category based on result message
        String message = result.getMessage().toLowerCase();

        if (message.contains("timeout") || message.contains("timed out")) {
            return TIMEOUT;
        } else if (message.contains("blocked") || message.contains("obstruction")) {
            return BLOCKED;
        } else if (message.contains("not found") || message.contains("unknown")) {
            return NOT_FOUND;
        } else if (message.contains("invalid") || message.contains("parameter")) {
            return INVALID_PARAMS;
        } else if (message.contains("navigation") || message.contains("path")) {
            return NAVIGATION_FAILURE;
        } else {
            return DEFAULT_FAILURE;
        }
    }

    /**
     * Attempts to recover from an error.
     *
     * @param foreman The foreman entity
     * @param result  The error result
     * @return true if recovery was attempted
     */
    public boolean attemptRecovery(ForemanEntity foreman, ActionResult result) {
        LOGGER.debug("[{}] Attempting recovery for {}: {}",
            foreman.getEntityName(), category, result.getMessage());

        switch (category) {
            case TRANSIENT:
            case RECOVERABLE:
                // Log the error and suggest retry
                LOGGER.info("[{}] Recoverable error, will retry: {}",
                    foreman.getEntityName(), result.getMessage());
                return true;

            case PERMANENT:
                // Log and notify user
                LOGGER.error("[{}] Permanent failure: {}",
                    foreman.getEntityName(), result.getMessage());
                return false;

            case CRITICAL:
                // Log critical error
                LOGGER.error("[{}] CRITICAL ERROR: {}",
                    foreman.getEntityName(), result.getMessage());
                return false;

            default:
                return false;
        }
    }

    /**
     * Recovery category for errors.
     */
    public enum RecoveryCategory {
        /** Temporary error that will resolve itself - retry immediately */
        TRANSIENT,

        /** Error that can be fixed with intervention - attempt recovery */
        RECOVERABLE,

        /** Permanent error that cannot be fixed - fail gracefully */
        PERMANENT,

        /** Critical system error - abort immediately */
        CRITICAL
    }

    // Predefined recovery strategies

    /** Transient success - no recovery needed. */
    public static final ErrorRecoveryStrategy TRANSIENT_SUCCESS =
        new ErrorRecoveryStrategy(RecoveryCategory.TRANSIENT, null, "Success - no recovery needed");

    /** Timeout errors - retry with standard policy. */
    public static final ErrorRecoveryStrategy TIMEOUT =
        new ErrorRecoveryStrategy(RecoveryCategory.TRANSIENT, RetryPolicy.QUICK_RETRY,
            "Operation timed out - retrying");

    /** Blocked by temporary obstacle - retry with quick policy. */
    public static final ErrorRecoveryStrategy BLOCKED =
        new ErrorRecoveryStrategy(RecoveryCategory.RECOVERABLE, RetryPolicy.QUICK_RETRY,
            "Blocked by obstacle - will retry");

    /** Resource not found - may require replanning. */
    public static final ErrorRecoveryStrategy NOT_FOUND =
        new ErrorRecoveryStrategy(RecoveryCategory.PERMANENT, RetryPolicy.NO_RETRIES,
            "Resource not found - replanning required");

    /** Invalid parameters - user error, no retry. */
    public static final ErrorRecoveryStrategy INVALID_PARAMS =
        new ErrorRecoveryStrategy(RecoveryCategory.PERMANENT, RetryPolicy.NO_RETRIES,
            "Invalid parameters - check task configuration");

    /** Navigation failure - retry with alternative path. */
    public static final ErrorRecoveryStrategy NAVIGATION_FAILURE =
        new ErrorRecoveryStrategy(RecoveryCategory.RECOVERABLE, RetryPolicy.STANDARD,
            "Navigation failed - retrying with alternative path");

    /** Default failure - standard retry. */
    public static final ErrorRecoveryStrategy DEFAULT_FAILURE =
        new ErrorRecoveryStrategy(RecoveryCategory.RECOVERABLE, RetryPolicy.STANDARD,
            "Operation failed - retrying");

    /**
     * Creates a custom recovery strategy.
     *
     * @param category    Recovery category
     * @param retryPolicy Retry policy
     * @param description Description
     * @return New recovery strategy
     */
    public static ErrorRecoveryStrategy create(RecoveryCategory category, RetryPolicy retryPolicy, String description) {
        return new ErrorRecoveryStrategy(category, retryPolicy, description);
    }
}
