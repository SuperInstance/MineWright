package com.minewright.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single task within a TaskProfile.
 *
 * <p>Profile tasks define what action to perform, on what target, with what parameters.
 * They are the building blocks of profiles and can be converted to Task objects for execution.</p>
 *
 * <p><b>Supported Task Types:</b></p>
 * <ul>
 *   <li><b>MINE:</b> Mine blocks of a specific type</li>
 *   <li><b>BUILD:</b> Build a structure from a template</li>
 *   <li><b>GATHER:</b> Gather resources from the world</li>
 *   <li><b>CRAFT:</b> Craft items at a crafting table</li>
 *   <li><b>TRAVEL:</b> Travel to a specific location</li>
 *   <li><b>PLACE:</b> Place blocks in the world</li>
 *   <li><b>ATTACK:</b> Attack hostile entities</li>
 *   <li><b>FOLLOW:</b> Follow a target entity</li>
 * </ul>
 *
 * <p><b>Conditions:</b></p>
 * <p>Tasks can have conditional execution requirements that must be met before
 * the task executes. Examples:</p>
 * <ul>
 *   <li>"inventory_has_space" - Only execute if inventory has room</li>
 *   <li>"daytime" - Only execute during day</li>
 *   <li>"health_above_50" - Only execute if health > 50%</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * ProfileTask task = ProfileTask.builder()
 *     .type(TaskType.MINE)
 *     .target("iron_ore")
 *     .quantity(64)
 *     .addCondition("inventory_has_space", true)
 *     .addParameter("radius", 32)
 *     .build();
 * }</pre>
 *
 * @see TaskProfile
 * @see TaskType
 * @since 1.4.0
 */
public class ProfileTask {

    private final TaskType type;
    private final String target;
    private final int quantity;
    private final Map<String, Object> conditions;
    private final Map<String, Object> parameters;
    private final String description;
    private final boolean optional;

    private ProfileTask(Builder builder) {
        this.type = builder.type;
        this.target = builder.target;
        this.quantity = builder.quantity;
        this.conditions = Collections.unmodifiableMap(new HashMap<>(builder.conditions));
        this.parameters = Collections.unmodifiableMap(new HashMap<>(builder.parameters));
        this.description = builder.description;
        this.optional = builder.optional;
    }

    /**
     * Creates a new builder for constructing profile tasks.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public TaskType getType() { return type; }
    public String getTarget() { return target; }
    public int getQuantity() { return quantity; }
    public Map<String, Object> getConditions() { return conditions; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getDescription() { return description; }
    public boolean isOptional() { return optional; }

    /**
     * Gets a condition value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCondition(String key, Class<T> type) {
        Object value = conditions.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets a condition value with default.
     */
    public boolean getConditionBoolean(String key, boolean defaultValue) {
        Object value = conditions.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Gets a parameter value by key.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets a parameter value as string.
     */
    public String getParameterString(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets a parameter value as int.
     */
    public int getParameterInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Checks if this task has all specified conditions.
     */
    public boolean hasConditions(String... keys) {
        for (String key : keys) {
            if (!conditions.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts this task to JSON format.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\": \"").append(type).append("\"");

        if (target != null) {
            sb.append(", \"target\": \"").append(escapeJson(target)).append("\"");
        }

        if (quantity > 0) {
            sb.append(", \"quantity\": ").append(quantity);
        }

        if (!conditions.isEmpty()) {
            sb.append(", \"conditions\": {");
            boolean first = true;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
                sb.append(formatValue(entry.getValue()));
            }
            sb.append("}");
        }

        if (!parameters.isEmpty()) {
            sb.append(", \"parameters\": {");
            boolean first = true;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
                sb.append(formatValue(entry.getValue()));
            }
            sb.append("}");
        }

        if (description != null) {
            sb.append(", \"description\": \"").append(escapeJson(description)).append("\"");
        }

        if (optional) {
            sb.append(", \"optional\": true");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts this profile task to a Task for execution.
     */
    public com.minewright.action.Task toTask() {
        Map<String, Object> params = new HashMap<>(parameters);

        // Add standard parameters
        if (target != null) {
            params.put("target", target);
        }
        if (quantity > 0) {
            params.put("quantity", quantity);
        }

        return new com.minewright.action.Task(type.getActionName(), params);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return value.toString();
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    /**
     * Builder for constructing ProfileTask instances.
     */
    public static class Builder {
        private TaskType type;
        private String target;
        private int quantity = 0;
        private Map<String, Object> conditions = new HashMap<>();
        private Map<String, Object> parameters = new HashMap<>();
        private String description;
        private boolean optional = false;

        public Builder() {}

        public Builder type(TaskType type) { this.type = type; return this; }
        public Builder target(String target) { this.target = target; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }

        public Builder addCondition(String key, Object value) {
            this.conditions.put(key, value);
            return this;
        }

        public Builder conditions(Map<String, Object> conditions) {
            this.conditions = new HashMap<>(conditions);
            return this;
        }

        public Builder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }

        public Builder description(String description) { this.description = description; return this; }
        public Builder optional(boolean optional) { this.optional = optional; return this; }

        public ProfileTask build() {
            if (type == null) {
                throw new IllegalStateException("Task type cannot be null");
            }
            return new ProfileTask(this);
        }
    }

    @Override
    public String toString() {
        return "ProfileTask{" +
                "type=" + type +
                ", target='" + target + '\'' +
                ", quantity=" + quantity +
                ", description='" + description + '\'' +
                '}';
    }
}
