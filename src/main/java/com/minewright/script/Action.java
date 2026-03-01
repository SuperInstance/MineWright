package com.minewright.script;

import java.util.*;

/**
 * Represents an action in a script's behavior tree.
 *
 * <p><b>Actions:</b> Actions define what the agent should do. They form a tree structure
 * where composite actions (SEQUENCE, PARALLEL, CONDITIONAL, LOOP) contain child actions,
 * and atomic actions (ATOMIC) represent actual game commands.</p>
 *
 * <p><b>Action Types:</b></p>
 * <ul>
 *   <li><b>SEQUENCE:</b> Execute children in order, stops at first failure</li>
 *   <li><b>PARALLEL:</b> Execute all children simultaneously, all must succeed</li>
 *   <li><b>CONDITIONAL:</b> Execute children if condition is true</li>
 *   <li><b>LOOP:</b> Repeat children N times</li>
 *   <li><b>ATOMIC:</b> Execute a single game command (mine, place, move, etc.)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Atomic action
 * Action mineAction = Action.builder()
 *     .type(ActionType.ATOMIC)
 *     .command("mine")
 *     .parameter("block", "oak_log")
 *     .build();
 *
 * // Composite action
 * Action sequence = Action.builder()
 *     .type(ActionType.SEQUENCE)
 *     .addChild(mineAction)
 *     .addChild(Action.builder()
 *         .type(ActionType.ATOMIC)
 *         .command("place")
 *         .parameter("block", "oak_planks")
 *         .build())
 *     .build();
 *
 * // Loop action
 * Action loop = Action.builder()
 *     .type(ActionType.LOOP)
 *     .iterations(10)
 *     .addChild(mineAction)
 *     .build();
 * </pre>
 *
 * @see ScriptDSL.ActionType
 * @see Script
 * @see ScriptNode
 * @since 1.3.0
 */
public class Action {

    private final ActionType type;
    private final String command;
    private final Map<String, Object> parameters;
    private final String condition;
    private final Integer iterations;
    private final List<Action> children;

    private Action(Builder builder) {
        this.type = builder.type;
        this.command = builder.command;
        this.parameters = Collections.unmodifiableMap(new HashMap<>(builder.parameters));
        this.condition = builder.condition;
        this.iterations = builder.iterations;
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
    }

    /**
     * Creates a new builder for constructing actions.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized from this action.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    // Getters

    /**
     * Gets the action type.
     *
     * @return The action type
     */
    public ActionType getType() {
        return type;
    }

    /**
     * Gets the command name (for ATOMIC actions).
     *
     * @return The command, or null if not set
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets the parameters map.
     *
     * @return Unmodifiable map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets a parameter value by key.
     *
     * @param key The parameter key
     * @return The parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Gets a string parameter value.
     *
     * @param key The parameter key
     * @return The parameter value as string, or null if not found
     */
    public String getStringParameter(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets an integer parameter value.
     *
     * @param key          The parameter key
     * @param defaultValue The default value if not found
     * @return The parameter value as integer
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
     * @param key          The parameter key
     * @param defaultValue The default value if not found
     * @return The parameter value as boolean
     */
    public boolean getBooleanParameter(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Gets the condition expression (for CONDITIONAL actions).
     *
     * @return The condition, or null if not set
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Gets the iteration count (for LOOP actions).
     *
     * @return The number of iterations, or null if not set
     */
    public Integer getIterations() {
        return iterations;
    }

    /**
     * Gets the child actions.
     *
     * @return Unmodifiable list of children
     */
    public List<Action> getChildren() {
        return children;
    }

    /**
     * Checks if this is a composite action (has children).
     */
    public boolean isComposite() {
        return type == ActionType.SEQUENCE ||
               type == ActionType.PARALLEL ||
               type == ActionType.CONDITIONAL ||
               type == ActionType.LOOP;
    }

    /**
     * Checks if this is an atomic action.
     */
    public boolean isAtomic() {
        return type == ActionType.ATOMIC;
    }

    /**
     * Checks if the action is valid.
     */
    public boolean isValid() {
        // Type must be set
        if (type == null) {
            return false;
        }

        // Atomic actions must have a command
        if (type == ActionType.ATOMIC && (command == null || command.isEmpty())) {
            return false;
        }

        // Conditional actions must have a condition
        if (type == ActionType.CONDITIONAL && (condition == null || condition.isEmpty())) {
            return false;
        }

        // Loop actions must have iteration count
        if (type == ActionType.LOOP && iterations == null) {
            return false;
        }

        // Composite actions must have children
        if (isComposite() && children.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Counts the total number of nodes in this action tree.
     */
    public int countNodes() {
        int count = 1;
        for (Action child : children) {
            count += child.countNodes();
        }
        return count;
    }

    /**
     * Calculates the depth of this action tree.
     */
    public int calculateDepth() {
        if (children.isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (Action child : children) {
            maxChildDepth = Math.max(maxChildDepth, child.calculateDepth());
        }

        return maxChildDepth + 1;
    }

    /**
     * Creates a deep copy of this action.
     */
    public Action copy() {
        return this.toBuilder().build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return type == action.type &&
               Objects.equals(command, action.command) &&
               Objects.equals(parameters, action.parameters) &&
               Objects.equals(condition, action.condition) &&
               Objects.equals(iterations, action.iterations) &&
               Objects.equals(children, action.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, command, parameters, condition, iterations, children);
    }

    @Override
    public String toString() {
        return "Action{" +
               "type=" + type +
               (command != null ? ", command='" + command + '\'' : "") +
               (!parameters.isEmpty() ? ", parameters=" + parameters.size() : "") +
               (condition != null ? ", condition='" + condition + '\'' : "") +
               (iterations != null ? ", iterations=" + iterations : "") +
               (!children.isEmpty() ? ", children=" + children.size() : "") +
               '}';
    }

    /**
     * Converts action to DSL format.
     */
    public String toDSL(int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);

        sb.append(indentStr).append("- type: \"").append(type).append("\"\n");

        if (command != null) {
            sb.append(indentStr).append("  command: \"").append(command).append("\"\n");
        }

        if (condition != null) {
            sb.append(indentStr).append("  condition: \"").append(condition).append("\"\n");
        }

        if (iterations != null) {
            sb.append(indentStr).append("  iterations: ").append(iterations).append("\n");
        }

        if (!parameters.isEmpty()) {
            sb.append(indentStr).append("  parameters:\n");
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                sb.append(indentStr).append("    ").append(entry.getKey()).append(": ");
                sb.append(formatValue(entry.getValue())).append("\n");
            }
        }

        if (!children.isEmpty()) {
            sb.append(indentStr).append("  children:\n");
            for (Action child : children) {
                sb.append(child.toDSL(indent + 2));
            }
        }

        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Builder for constructing Action instances.
     */
    public static class Builder {
        private ActionType type;
        private String command;
        private Map<String, Object> parameters = new HashMap<>();
        private String condition;
        private Integer iterations;
        private List<Action> children = new ArrayList<>();

        private Builder() {}

        private Builder(Action existing) {
            this.type = existing.type;
            this.command = existing.command;
            this.parameters = new HashMap<>(existing.parameters);
            this.condition = existing.condition;
            this.iterations = existing.iterations;
            this.children = new ArrayList<>(existing.children);
        }

        /**
         * Sets the action type.
         *
         * @param type The action type
         * @return This builder
         */
        public Builder type(ActionType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the command name (for ATOMIC actions).
         *
         * @param command The command to execute
         * @return This builder
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Adds a parameter.
         *
         * @param key   The parameter key
         * @param value The parameter value
         * @return This builder
         */
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        /**
         * Sets all parameters.
         *
         * @param parameters The parameter map
         * @return This builder
         */
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }

        /**
         * Sets the condition expression (for CONDITIONAL actions).
         *
         * @param condition The condition to evaluate
         * @return This builder
         */
        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Sets the iteration count (for LOOP actions).
         *
         * @param iterations The number of iterations
         * @return This builder
         */
        public Builder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        /**
         * Adds a child action.
         *
         * @param child The child action
         * @return This builder
         */
        public Builder addChild(Action child) {
            if (child != null) {
                this.children.add(child);
            }
            return this;
        }

        /**
         * Adds multiple child actions.
         *
         * @param children The child actions
         * @return This builder
         */
        public Builder addChildren(Action... children) {
            for (Action child : children) {
                addChild(child);
            }
            return this;
        }

        /**
         * Sets all children.
         *
         * @param children The list of children
         * @return This builder
         */
        public Builder children(List<Action> children) {
            this.children = new ArrayList<>(children);
            return this;
        }

        /**
         * Builds the action.
         *
         * @return A new Action instance
         * @throws IllegalStateException if type is not set
         */
        public Action build() {
            if (type == null) {
                throw new IllegalStateException("Action type is required");
            }
            return new Action(this);
        }
    }

    /**
     * Action type enumeration.
     */
    public enum ActionType {
        /**
         * Execute children in sequential order.
         * All children must succeed for sequence to succeed.
         * Stops at first failure.
         */
        SEQUENCE,

        /**
         * Execute all children simultaneously.
         * Succeeds if all children succeed.
         * Fails if any child fails.
         */
        PARALLEL,

        /**
         * Execute children only if condition is true.
         * Uses the condition field to determine execution.
         */
        CONDITIONAL,

        /**
         * Repeat children a specified number of times.
         * Uses the iterations field for count.
         */
        LOOP,

        /**
         * Execute a single atomic game command.
         * These are leaf nodes that interact with the game.
         */
        ATOMIC
    }
}
