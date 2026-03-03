package com.minewright.action;

/**
 * Exception thrown when a FunctionCallingTool execution fails.
 *
 * <p>This exception provides structured error information that can be
 * captured and fed back to the LLM for context-aware error recovery.</p>
 *
 * @see FunctionCallingTool
 * @see ToolResult
 * @since 1.8.0
 */
public class ToolExecutionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String toolName;
    private final String recoveryHint;
    private final boolean retryable;

    /**
     * Creates a new ToolExecutionException.
     *
     * @param toolName The name of the tool that failed
     * @param message Error message
     */
    public ToolExecutionException(String toolName, String message) {
        super(message);
        this.toolName = toolName;
        this.recoveryHint = null;
        this.retryable = false;
    }

    /**
     * Creates a new ToolExecutionException with cause.
     *
     * @param toolName The name of the tool that failed
     * @param message Error message
     * @param cause The underlying cause
     */
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
        this.recoveryHint = null;
        this.retryable = false;
    }

    /**
     * Creates a new ToolExecutionException with all options.
     *
     * @param toolName The name of the tool that failed
     * @param message Error message
     * @param cause The underlying cause
     * @param recoveryHint Suggestion for how to recover
     * @param retryable Whether the operation can be retried
     */
    public ToolExecutionException(String toolName, String message, Throwable cause,
                                  String recoveryHint, boolean retryable) {
        super(message, cause);
        this.toolName = toolName;
        this.recoveryHint = recoveryHint;
        this.retryable = retryable;
    }

    /**
     * Returns the name of the tool that failed.
     *
     * @return Tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns a hint for how to recover from this error.
     *
     * @return Recovery hint, or null if none
     */
    public String getRecoveryHint() {
        return recoveryHint;
    }

    /**
     * Returns whether the operation can be retried.
     *
     * <p>Retryable errors are typically transient issues like
     * network timeouts or temporary resource unavailability.</p>
     *
     * @return true if the operation can be retried
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Creates a builder for constructing ToolExecutionExceptions.
     *
     * @param toolName The name of the tool that failed
     * @return New builder
     */
    public static Builder builder(String toolName) {
        return new Builder(toolName);
    }

    /**
     * Builder for ToolExecutionException.
     */
    public static class Builder {
        private final String toolName;
        private String message;
        private Throwable cause;
        private String recoveryHint;
        private boolean retryable;

        public Builder(String toolName) {
            this.toolName = toolName;
        }

        /**
         * Sets the error message.
         *
         * @param message Error message
         * @return This builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the underlying cause.
         *
         * @param cause The cause
         * @return This builder
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Sets the recovery hint.
         *
         * @param hint Suggestion for recovery
         * @return This builder
         */
        public Builder recoveryHint(String hint) {
            this.recoveryHint = hint;
            return this;
        }

        /**
         * Marks the error as retryable.
         *
         * @return This builder
         */
        public Builder retryable() {
            this.retryable = true;
            return this;
        }

        /**
         * Builds the exception.
         *
         * @return New ToolExecutionException
         */
        public ToolExecutionException build() {
            return new ToolExecutionException(toolName, message, cause, recoveryHint, retryable);
        }
    }
}
