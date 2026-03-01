package com.minewright.htn;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a task in Hierarchical Task Network (HTN) planning.
 *
 * <p><b>HTN Task Types:</b></p>
 * <ul>
 *   <li><b>Primitive Tasks:</b> Directly executable actions that map to {@link com.minewright.action.Task}</li>
 *   <li><b>Compound Tasks:</b> High-level goals that require decomposition via {@link HTNMethod}</li>
 * </ul>
 *
 * <p><b>HTN Planning Flow:</b></p>
 * <pre>
 * Compound Task (e.g., "build_house")
 *     ↓
 * HTNMethod decomposes into subtasks
 *     ↓
 * [Compound: gather_materials, Primitive: pathfind, Compound: construct_walls]
 *     ↓
 * Recursive decomposition until all tasks are primitive
 *     ↓
 * Primitive tasks execute as actions
 * </pre>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Primitive task - directly executable
 * HTNTask mineOak = new HTNTask.Builder("mine")
 *     .type(HTNTask.Type.PRIMITIVE)
 *     .parameter("blockType", "oak_log")
 *     .parameter("count", 64)
 *     .build();
 *
 * // Compound task - requires decomposition
 * HTNTask buildHouse = new HTNTask.Builder("build_house")
 *     .type(HTNTask.Type.COMPOUND)
 *     .parameter("material", "oak_planks")
 *     .parameter("width", 5)
 *     .parameter("height", 3)
 *     .build();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 * Clone operations create independent copies for safe manipulation.</p>
 *
 * @see HTNMethod
 * @see HTNDomain
 * @see HTNPlanner
 *
 * @since 1.0.0
 */
public class HTNTask {
    /**
     * The task name/identifier (e.g., "mine", "build_house", "craft_item").
     * Primitive tasks map directly to action names in the action system.
     */
    private final String name;

    /**
     * The task type determining whether this task can be executed directly
     * or requires decomposition.
     */
    private final Type type;

    /**
     * Task parameters providing context for execution or decomposition.
     * Examples: blockType, count, material, dimensions.
     */
    private final Map<String, Object> parameters;

    /**
     * Unique identifier for this task instance, used for tracking during planning.
     * Generated automatically if not provided.
     */
    private final String taskId;

    /**
     * Task types in HTN planning.
     */
    public enum Type {
        /**
         * Primitive tasks are directly executable actions.
         * They map to {@link com.minewright.action.Task} in the action system.
         * Examples: "mine", "place", "pathfind", "craft".
         */
        PRIMITIVE,

        /**
         * Compound tasks require decomposition into subtasks.
         * They represent high-level goals that are achieved through methods.
         * Examples: "build_house", "gather_resources", "establish_base".
         */
        COMPOUND
    }

    /**
     * Builder for creating HTNTask instances.
     */
    public static class Builder {
        private String name;
        private Type type = Type.COMPOUND; // Default to compound
        private final Map<String, Object> parameters = new java.util.HashMap<>();
        private String taskId;

        /**
         * Sets the task name.
         *
         * @param name The task name/identifier
         * @return This builder for chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the task type.
         *
         * @param type The task type (PRIMITIVE or COMPOUND)
         * @return This builder for chaining
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Adds a parameter to this task.
         *
         * @param key   Parameter key
         * @param value Parameter value (must be serializable)
         * @return This builder for chaining
         */
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        /**
         * Sets all parameters at once.
         *
         * @param parameters Map of parameters
         * @return This builder for chaining
         */
        public Builder parameters(Map<String, Object> parameters) {
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            return this;
        }

        /**
         * Sets a specific task ID (for tracking during planning).
         * If not set, a unique ID will be generated.
         *
         * @param taskId The task ID
         * @return This builder for chaining
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Builds the HTNTask instance.
         *
         * @return A new HTNTask with the configured properties
         * @throws IllegalArgumentException if name is null or empty
         */
        public HTNTask build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Task name cannot be null or empty");
            }

            // Generate task ID if not provided
            String finalTaskId = this.taskId;
            if (finalTaskId == null) {
                finalTaskId = generateTaskId();
            }

            return new HTNTask(name, type, java.util.Map.copyOf(parameters), finalTaskId);
        }
    }

    /**
     * Creates a new primitive task builder.
     * Convenience method for primitive task creation.
     *
     * @param name The primitive task name
     * @return A Builder configured for primitive tasks
     */
    public static Builder primitive(String name) {
        return new Builder().name(name).type(Type.PRIMITIVE);
    }

    /**
     * Creates a new compound task builder.
     * Convenience method for compound task creation.
     *
     * @param name The compound task name
     * @return A Builder configured for compound tasks
     */
    public static Builder compound(String name) {
        return new Builder().name(name).type(Type.COMPOUND);
    }

    /**
     * Private constructor. Use Builder to create instances.
     */
    private HTNTask(String name, Type type, Map<String, Object> parameters, String taskId) {
        this.name = name;
        this.type = type;
        this.parameters = parameters;
        this.taskId = taskId;
    }

    /**
     * Gets the task name.
     *
     * @return Task name/identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the task type.
     *
     * @return Task type (PRIMITIVE or COMPOUND)
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets all task parameters.
     *
     * @return Immutable map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets a parameter value by key.
     *
     * @param key Parameter key
     * @return Parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Gets a string parameter value.
     *
     * @param key          Parameter key
     * @param defaultValue Default value if not found
     * @return String parameter value
     */
    public String getStringParameter(String key, String defaultValue) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Gets an integer parameter value.
     *
     * @param key          Parameter key
     * @param defaultValue Default value if not found or not a number
     * @return Integer parameter value
     */
    public int getIntParameter(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Gets a boolean parameter value.
     *
     * @param key          Parameter key
     * @param defaultValue Default value if not found or not a boolean
     * @return Boolean parameter value
     */
    public boolean getBooleanParameter(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Gets the unique task ID.
     *
     * @return Task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Returns whether this is a primitive task.
     *
     * @return true if primitive, false if compound
     */
    public boolean isPrimitive() {
        return type == Type.PRIMITIVE;
    }

    /**
     * Returns whether this is a compound task.
     *
     * @return true if compound, false if primitive
     */
    public boolean isCompound() {
        return type == Type.COMPOUND;
    }

    /**
     * Checks if this task has a specific parameter.
     *
     * @param key Parameter key
     * @return true if parameter exists
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Creates a deep copy of this task with a new unique ID.
     * Useful for safe manipulation during planning.
     *
     * @return A cloned HTNTask with the same properties but new ID
     */
    public HTNTask clone() {
        return new HTNTask(name, type, new java.util.HashMap<>(parameters), generateTaskId());
    }

    /**
     * Creates a modified copy of this task with overridden parameters.
     *
     * @param additionalParameters Parameters to add or override
     * @return A new HTNTask with merged parameters
     */
    public HTNTask withParameters(Map<String, Object> additionalParameters) {
        Map<String, Object> newParams = new java.util.HashMap<>(this.parameters);
        if (additionalParameters != null) {
            newParams.putAll(additionalParameters);
        }
        return new HTNTask(name, type, java.util.Map.copyOf(newParams), generateTaskId());
    }

    /**
     * Generates a unique task ID.
     */
    private static String generateTaskId() {
        return "task_" + System.nanoTime() + "_" + (int)(Math.random() * 10000);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTNTask htnTask = (HTNTask) o;
        return Objects.equals(name, htnTask.name) &&
               type == htnTask.type &&
               Objects.equals(parameters, htnTask.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, parameters);
    }

    @Override
    public String toString() {
        return "HTNTask{" +
               "name='" + name + '\'' +
               ", type=" + type +
               ", parameters=" + parameters +
               ", taskId='" + taskId + '\'' +
               '}';
    }

    /**
     * Converts this HTNTask to an executable Task for the action system.
     * Only valid for primitive tasks.
     *
     * @return A Task instance for execution
     * @throws IllegalStateException if called on a compound task
     */
    public com.minewright.action.Task toActionTask() {
        if (!isPrimitive()) {
            throw new IllegalStateException("Cannot convert compound task to action task: " + name);
        }
        return new com.minewright.action.Task(name, parameters);
    }
}
