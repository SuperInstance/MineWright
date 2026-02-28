package com.minewright.exception;

/**
 * Base exception for all MineWright-specific errors.
 *
 * <p>All MineWright exceptions should extend this class to provide:
 * <ul>
 *   <li>Consistent error categorization</li>
 *   <li>Error codes for programmatic handling</li>
 *   <li>Recovery suggestions for user-friendly messages</li>
 *   <li>Context information for debugging</li>
 * </ul>
 *
 * <p><b>Error Recovery:</b> Each exception can provide a recovery suggestion
 * that can be displayed to users to help them resolve the issue.</p>
 *
 * @since 1.2.0
 */
public class MineWrightException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Error codes for categorizing exceptions.
     * Each code represents a specific category of failure.
     */
    public enum ErrorCode {
        // LLM Errors (1000-1999)
        LLM_PROVIDER_ERROR(1000, "LLM provider error"),
        LLM_RATE_LIMIT(1001, "LLM rate limit exceeded"),
        LLM_TIMEOUT(1002, "LLM request timeout"),
        LLM_INVALID_RESPONSE(1003, "Invalid LLM response"),
        LLM_AUTH_ERROR(1004, "LLM authentication failed"),
        LLM_NETWORK_ERROR(1005, "LLM network error"),
        LLM_CONFIG_ERROR(1006, "LLM configuration error"),

        // Action Errors (2000-2999)
        ACTION_UNKNOWN_TYPE(2000, "Unknown action type"),
        ACTION_INVALID_PARAMS(2001, "Invalid action parameters"),
        ACTION_EXECUTION_FAILED(2002, "Action execution failed"),
        ACTION_TIMEOUT(2003, "Action timeout"),
        ACTION_CANCELLED(2004, "Action cancelled"),
        ACTION_BLOCKED(2005, "Action blocked"),

        // Entity Errors (3000-3999)
        ENTITY_SPAWN_FAILED(3000, "Entity spawn failed"),
        ENTITY_TICK_ERROR(3001, "Entity tick error"),
        ENTITY_INVALID_STATE(3002, "Entity invalid state"),
        ENTITY_NOT_FOUND(3003, "Entity not found"),

        // Configuration Errors (4000-4999)
        CONFIG_MISSING_KEY(4000, "Missing configuration key"),
        CONFIG_INVALID_VALUE(4001, "Invalid configuration value"),
        CONFIG_VALIDATION_FAILED(4002, "Configuration validation failed"),

        // Plugin Errors (5000-5999)
        PLUGIN_LOAD_FAILED(5000, "Plugin load failed"),
        PLUGIN_NOT_FOUND(5001, "Plugin not found"),
        PLUGIN_VERSION_MISMATCH(5002, "Plugin version mismatch"),

        // Voice Errors (6000-6999)
        VOICE_INIT_FAILED(6000, "Voice system initialization failed"),
        VOICE_RECOGNITION_ERROR(6001, "Voice recognition error"),
        VOICE_SYNTHESIS_ERROR(6002, "Voice synthesis error");

        private final int code;
        private final String description;

        ErrorCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorCode errorCode;
    private final String recoverySuggestion;
    private final String context;

    /**
     * Constructs a new MineWrightException.
     *
     * @param message            Error message
     * @param errorCode          Error code for categorization
     * @param recoverySuggestion Suggestion for resolving the issue (can be null)
     */
    public MineWrightException(String message, ErrorCode errorCode, String recoverySuggestion) {
        super(message);
        this.errorCode = errorCode;
        this.recoverySuggestion = recoverySuggestion;
        this.context = null;
    }

    /**
     * Constructs a new MineWrightException with a cause.
     *
     * @param message            Error message
     * @param errorCode          Error code for categorization
     * @param recoverySuggestion Suggestion for resolving the issue (can be null)
     * @param cause              Underlying cause
     */
    public MineWrightException(String message, ErrorCode errorCode, String recoverySuggestion, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recoverySuggestion = recoverySuggestion;
        this.context = null;
    }

    /**
     * Constructs a new MineWrightException with full context.
     *
     * @param message            Error message
     * @param errorCode          Error code for categorization
     * @param recoverySuggestion Suggestion for resolving the issue (can be null)
     * @param context            Additional context information (can be null)
     * @param cause              Underlying cause (can be null)
     */
    public MineWrightException(String message, ErrorCode errorCode, String recoverySuggestion,
                               String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recoverySuggestion = recoverySuggestion;
        this.context = context;
    }

    /**
     * Returns the error code for this exception.
     *
     * @return Error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a recovery suggestion for this error.
     *
     * @return Recovery suggestion, or null if none available
     */
    public String getRecoverySuggestion() {
        return recoverySuggestion;
    }

    /**
     * Returns additional context for this error.
     *
     * @return Context information, or null if none available
     */
    public String getContext() {
        return context;
    }

    /**
     * Returns a user-friendly error message including recovery suggestion.
     *
     * @return Formatted error message
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        if (recoverySuggestion != null && !recoverySuggestion.isEmpty()) {
            sb.append("\n\nRecovery: ").append(recoverySuggestion);
        }
        if (context != null && !context.isEmpty()) {
            sb.append("\n\nContext: ").append(context);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("MineWrightException[code=%d, type=%s, message='%s']",
            errorCode.getCode(), errorCode.name(), getMessage());
    }
}
