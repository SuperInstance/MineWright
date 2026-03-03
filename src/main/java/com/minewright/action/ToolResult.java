package com.minewright.action;

import com.google.gson.JsonObject;

/**
 * Result of a FunctionCallingTool execution.
 *
 * <p>This class encapsulates the outcome of tool execution, providing
 * structured success/failure information that can be fed back to the LLM
 * for context-aware decision making.</p>
 *
 * <h3>Design Philosophy</h3>
 * <p>Research from AutoGPT and CrewAI shows that structured error context
 * helps LLMs make better recovery decisions. This class provides rich
 * error information while maintaining a simple success/failure dichotomy.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Success case
 * public ToolResult execute(JsonObject params) {
 *     int blocksMined = mineBlocks(params);
 *     return ToolResult.success("Mined " + blocksMined + " blocks");
 * }
 *
 * // Failure case with recovery hint
 * public ToolResult execute(JsonObject params) {
 *     if (!hasRequiredTool()) {
 *         return ToolResult.failure("No pickaxe equipped")
 *             .withRecoveryHint("Equip a pickaxe first using equip_item");
 *     }
 *     // ...
 * }
 *
 * // Partial success
 * public ToolResult execute(JsonObject params) {
 *     int target = params.get("count").getAsInt();
 *     int actual = mineBlocks(target);
 *     if (actual < target) {
 *         return ToolResult.partial(actual, target,
 *             "Only found " + actual + " of " + target + " requested blocks");
 *     }
 *     return ToolResult.success("Mined " + actual + " blocks");
 * }
 * }</pre>
 *
 * @see FunctionCallingTool
 * @since 1.8.0
 */
public class ToolResult {

    /**
     * Result status indicating execution outcome.
     */
    public enum Status {
        /**
         * Tool executed successfully.
         */
        SUCCESS,

        /**
         * Tool execution failed completely.
         */
        FAILURE,

        /**
         * Tool executed partially (some but not all objectives met).
         */
        PARTIAL
    }

    private final Status status;
    private final String message;
    private final JsonObject data;
    private final String recoveryHint;
    private final Throwable error;
    private final int completedSteps;
    private final int totalSteps;

    private ToolResult(Status status, String message, JsonObject data,
                       String recoveryHint, Throwable error,
                       int completedSteps, int totalSteps) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.recoveryHint = recoveryHint;
        this.error = error;
        this.completedSteps = completedSteps;
        this.totalSteps = totalSteps;
    }

    // ========== Factory Methods ==========

    /**
     * Creates a successful result with a message.
     *
     * @param message Success message
     * @return Successful ToolResult
     */
    public static ToolResult success(String message) {
        return new ToolResult(Status.SUCCESS, message, null, null, null, 0, 0);
    }

    /**
     * Creates a successful result with message and data.
     *
     * @param message Success message
     * @param data Additional result data
     * @return Successful ToolResult with data
     */
    public static ToolResult success(String message, JsonObject data) {
        return new ToolResult(Status.SUCCESS, message, data, null, null, 0, 0);
    }

    /**
     * Creates a failure result with a message.
     *
     * @param message Failure message
     * @return Failed ToolResult
     */
    public static ToolResult failure(String message) {
        return new ToolResult(Status.FAILURE, message, null, null, null, 0, 0);
    }

    /**
     * Creates a failure result with message and error.
     *
     * @param message Failure message
     * @param error The exception that caused the failure
     * @return Failed ToolResult with error
     */
    public static ToolResult failure(String message, Throwable error) {
        return new ToolResult(Status.FAILURE, message, null, null, error, 0, 0);
    }

    /**
     * Creates a partial success result.
     *
     * <p>Use this when some but not all objectives were achieved.</p>
     *
     * @param completed Number of completed steps
     * @param total Total number of steps attempted
     * @param message Description of partial outcome
     * @return Partial ToolResult
     */
    public static ToolResult partial(int completed, int total, String message) {
        return new ToolResult(Status.PARTIAL, message, null, null, null, completed, total);
    }

    // ========== Builder Methods ==========

    /**
     * Returns a new result with the specified data.
     *
     * @param data Additional result data
     * @return New ToolResult with data
     */
    public ToolResult withData(JsonObject data) {
        return new ToolResult(status, message, data, recoveryHint, error, completedSteps, totalSteps);
    }

    /**
     * Returns a new result with a recovery hint.
     *
     * <p>Recovery hints help LLMs understand what to try next
     * when a tool fails. This improves autonomous error recovery.</p>
     *
     * @param hint Suggestion for how to recover from failure
     * @return New ToolResult with recovery hint
     */
    public ToolResult withRecoveryHint(String hint) {
        return new ToolResult(status, message, data, hint, error, completedSteps, totalSteps);
    }

    // ========== Accessors ==========

    /**
     * Returns the execution status.
     *
     * @return Status (SUCCESS, FAILURE, or PARTIAL)
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the result message.
     *
     * @return Human-readable message describing the outcome
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns additional result data, if any.
     *
     * @return JsonObject with result data, or null if none
     */
    public JsonObject getData() {
        return data;
    }

    /**
     * Returns the recovery hint, if any.
     *
     * @return Suggestion for recovery, or null if none
     */
    public String getRecoveryHint() {
        return recoveryHint;
    }

    /**
     * Returns the error that caused failure, if any.
     *
     * @return Throwable error, or null if none
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Returns the number of completed steps (for partial results).
     *
     * @return Completed steps, or 0 if not applicable
     */
    public int getCompletedSteps() {
        return completedSteps;
    }

    /**
     * Returns the total number of steps (for partial results).
     *
     * @return Total steps, or 0 if not applicable
     */
    public int getTotalSteps() {
        return totalSteps;
    }

    // ========== Convenience Methods ==========

    /**
     * Returns true if execution was successful.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Returns true if execution failed.
     *
     * @return true if status is FAILURE
     */
    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    /**
     * Returns true if execution was partially successful.
     *
     * @return true if status is PARTIAL
     */
    public boolean isPartial() {
        return status == Status.PARTIAL;
    }

    /**
     * Returns completion percentage for partial results.
     *
     * @return Percentage complete (0-100), or 100 if not partial
     */
    public int getCompletionPercentage() {
        if (totalSteps == 0) {
            return isSuccess() ? 100 : 0;
        }
        return (int) ((completedSteps * 100.0) / totalSteps);
    }

    /**
     * Returns a JSON representation suitable for LLM context.
     *
     * <p>This format is designed to be easily understood by LLMs
     * for context-aware decision making.</p>
     *
     * @return JsonObject representation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.name().toLowerCase());
        json.addProperty("message", message);

        if (data != null) {
            json.add("data", data);
        }

        if (recoveryHint != null) {
            json.addProperty("recovery_hint", recoveryHint);
        }

        if (error != null) {
            json.addProperty("error_type", error.getClass().getSimpleName());
            json.addProperty("error_message", error.getMessage());
        }

        if (status == Status.PARTIAL) {
            json.addProperty("completed", completedSteps);
            json.addProperty("total", totalSteps);
            json.addProperty("percentage", getCompletionPercentage());
        }

        return json;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ToolResult[status=").append(status);
        sb.append(", message=\"").append(message).append("\"");

        if (recoveryHint != null) {
            sb.append(", hint=\"").append(recoveryHint).append("\"");
        }

        if (status == Status.PARTIAL) {
            sb.append(", progress=").append(completedSteps).append("/").append(totalSteps);
        }

        sb.append("]");
        return sb.toString();
    }
}
