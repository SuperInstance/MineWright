package com.minewright.skill;

import java.util.Map;
import java.util.Objects;

/**
 * Record of a single action execution within a sequence.
 *
 * <p><b>Purpose:</b></p>
 * <p>ActionRecord captures the details of each action execution for pattern analysis.
 * It stores the action type, parameters, execution time, success status, and any error
 * messages. These records are aggregated into ExecutionSequence objects for skill
 * extraction.</p>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li><b>Pattern Discovery:</b> Identify common action sequences across tasks</li>
 *   <li><b>Parameterization:</b> Extract variable parts for skill templates</li>
 *   <li><b>Performance Analysis:</b> Track execution times for optimization</li>
 *   <li><b>Failure Analysis:</b> Understand which actions commonly fail</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @see ExecutionSequence
 * @see PatternExtractor
 * @since 1.0.0
 */
public class ActionRecord {
    private final String actionType;
    private final Map<String, Object> parameters;
    private final long executionTime;
    private final boolean success;
    private final String errorMessage;
    private final long timestamp;

    /**
     * Creates a new action record.
     *
     * @param actionType    The type of action (e.g., "mine", "place", "craft")
     * @param parameters    The parameters passed to the action
     * @param executionTime Time taken to execute the action in milliseconds
     * @param success       Whether the action completed successfully
     * @param errorMessage  Error message if the action failed, null otherwise
     */
    public ActionRecord(String actionType, Map<String, Object> parameters,
                       long executionTime, boolean success, String errorMessage) {
        this.actionType = actionType;
        this.parameters = Map.copyOf(parameters); // Immutable copy
        this.executionTime = executionTime;
        this.success = success;
        this.errorMessage = errorMessage;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Creates a successful action record.
     *
     * @param actionType    The type of action
     * @param parameters    The parameters passed to the action
     * @param executionTime Time taken to execute the action in milliseconds
     * @return New ActionRecord for successful execution
     */
    public static ActionRecord success(String actionType, Map<String, Object> parameters, long executionTime) {
        return new ActionRecord(actionType, parameters, executionTime, true, null);
    }

    /**
     * Creates a failed action record.
     *
     * @param actionType    The type of action
     * @param parameters    The parameters passed to the action
     * @param executionTime Time taken before failure in milliseconds
     * @param errorMessage  Error message describing the failure
     * @return New ActionRecord for failed execution
     */
    public static ActionRecord failure(String actionType, Map<String, Object> parameters,
                                      long executionTime, String errorMessage) {
        return new ActionRecord(actionType, parameters, executionTime, false, errorMessage);
    }

    public String getActionType() {
        return actionType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this action record has a specific parameter.
     *
     * @param key Parameter key
     * @return true if the parameter exists
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Gets a string parameter value.
     *
     * @param key Parameter key
     * @return Parameter value as string, or null if not found
     */
    public String getStringParameter(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets an integer parameter value.
     *
     * @param key          Parameter key
     * @param defaultValue Default value if parameter not found
     * @return Parameter value as integer
     */
    public int getIntParameter(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Creates a normalized key for pattern matching.
     * Excludes variable parameters like coordinates and counts.
     *
     * @return Normalized key (e.g., "mine:block_type")
     */
    public String getNormalizedKey() {
        StringBuilder key = new StringBuilder(actionType);

        // Add only stable parameters (not coordinates or counts)
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramKey = entry.getKey().toLowerCase();
            // Skip variable parameters
            if (paramKey.contains("x") || paramKey.contains("y") || paramKey.contains("z") ||
                paramKey.contains("count") || paramKey.contains("amount") ||
                paramKey.contains("quantity") || paramKey.contains("radius")) {
                continue;
            }
            key.append(":").append(paramKey).append("=").append(entry.getValue());
        }

        return key.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionRecord that = (ActionRecord) o;
        return executionTime == that.executionTime &&
            success == that.success &&
            Objects.equals(actionType, that.actionType) &&
            Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionType, parameters, executionTime, success);
    }

    @Override
    public String toString() {
        return "ActionRecord{" +
            "actionType='" + actionType + '\'' +
            ", parameters=" + parameters +
            ", executionTime=" + executionTime +
            ", success=" + success +
            ", errorMessage='" + errorMessage + '\'' +
            ", timestamp=" + timestamp +
            '}';
    }
}
