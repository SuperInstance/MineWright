package com.minewright.script;

import java.util.Objects;

/**
 * Represents a trigger that can initiate script execution.
 *
 * <p><b>Triggers:</b> Triggers define when a script should start executing.
 * They can be based on game events, conditions, time intervals, or player actions.</p>
 *
 * <p><b>Trigger Lifecycle:</b></p>
 * <ol>
 *   <li>Trigger condition is evaluated</li>
 *   <li>If condition is met, optional delay is applied</li>
 *   <li>Script execution begins</li>
 *   <li>Cooldown period begins (prevents re-triggering)</li>
 * </ol>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Event-based trigger
 * Trigger eventTrigger = Trigger.builder()
 *     .type(TriggerType.EVENT)
 *     .condition("block_broken == \"oak_log\"")
 *     .cooldown(100)
 *     .build();
 *
 * // Time-based trigger
 * Trigger timeTrigger = Trigger.builder()
 *     .type(TriggerType.TIME)
 *     .condition("time_of_day() == \"day\"")
 *     .delay(20)
 *     .build();
 *
 * // Condition-based trigger
 * Trigger conditionTrigger = Trigger.builder()
 *     .type(TriggerType.CONDITION)
 *     .condition("inventory_count(\"oak_log\") < 10")
 *     .cooldown(200)
 *     .build();
 * </pre>
 *
 * @see ScriptDSL.TriggerType
 * @see Script
 * @since 1.3.0
 */
public class Trigger {

    private final TriggerType type;
    private final String condition;
    private final Integer delay;
    private final Integer cooldown;
    private final String description;

    private Trigger(Builder builder) {
        this.type = builder.type;
        this.condition = builder.condition;
        this.delay = builder.delay;
        this.cooldown = builder.cooldown;
        this.description = builder.description;
    }

    /**
     * Creates a new builder for constructing triggers.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized from this trigger.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    // Getters

    /**
     * Gets the trigger type.
     *
     * @return The trigger type
     */
    public TriggerType getType() {
        return type;
    }

    /**
     * Gets the condition expression.
     *
     * @return The condition, or null if not set
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Gets the optional delay before script execution.
     *
     * @return The delay in ticks, or null if not set
     */
    public Integer getDelay() {
        return delay;
    }

    /**
     * Gets the cooldown period between triggers.
     *
     * @return The cooldown in ticks, or null if not set
     */
    public Integer getCooldown() {
        return cooldown;
    }

    /**
     * Gets the trigger description.
     *
     * @return The description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this trigger has a delay.
     */
    public boolean hasDelay() {
        return delay != null && delay > 0;
    }

    /**
     * Checks if this trigger has a cooldown.
     */
    public boolean hasCooldown() {
        return cooldown != null && cooldown > 0;
    }

    /**
     * Checks if the trigger is valid (has required fields).
     */
    public boolean isValid() {
        return type != null && condition != null && !condition.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return type == trigger.type &&
               Objects.equals(condition, trigger.condition) &&
               Objects.equals(delay, trigger.delay) &&
               Objects.equals(cooldown, trigger.cooldown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, condition, delay, cooldown);
    }

    @Override
    public String toString() {
        return "Trigger{" +
               "type=" + type +
               ", condition='" + condition + '\'' +
               ", delay=" + delay +
               ", cooldown=" + cooldown +
               '}';
    }

    /**
     * Converts trigger to DSL format.
     */
    public String toDSL() {
        StringBuilder sb = new StringBuilder();
        sb.append("  - type: \"").append(type).append("\"\n");
        sb.append("    condition: \"").append(condition).append("\"");

        if (delay != null) {
            sb.append("\n    delay: ").append(delay);
        }

        if (cooldown != null) {
            sb.append("\n    cooldown: ").append(cooldown);
        }

        if (description != null) {
            sb.append("\n    description: \"").append(description).append("\"");
        }

        return sb.toString();
    }

    /**
     * Builder for constructing Trigger instances.
     */
    public static class Builder {
        private TriggerType type;
        private String condition;
        private Integer delay;
        private Integer cooldown;
        private String description;

        private Builder() {}

        private Builder(Trigger existing) {
            this.type = existing.type;
            this.condition = existing.condition;
            this.delay = existing.delay;
            this.cooldown = existing.cooldown;
            this.description = existing.description;
        }

        /**
         * Sets the trigger type.
         *
         * @param type The trigger type
         * @return This builder
         */
        public Builder type(TriggerType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the condition expression.
         *
         * @param condition The condition to evaluate
         * @return This builder
         */
        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Sets the delay before script execution.
         *
         * @param delay The delay in ticks (20 ticks = 1 second)
         * @return This builder
         */
        public Builder delay(int delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Sets the delay before script execution.
         *
         * @param delay The delay in ticks, or null to clear
         * @return This builder
         */
        public Builder delay(Integer delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Sets the cooldown period between triggers.
         *
         * @param cooldown The cooldown in ticks (20 ticks = 1 second)
         * @return This builder
         */
        public Builder cooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        /**
         * Sets the cooldown period between triggers.
         *
         * @param cooldown The cooldown in ticks, or null to clear
         * @return This builder
         */
        public Builder cooldown(Integer cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        /**
         * Sets the trigger description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the trigger.
         *
         * @return A new Trigger instance
         * @throws IllegalStateException if type or condition is not set
         */
        public Trigger build() {
            if (type == null) {
                throw new IllegalStateException("Trigger type is required");
            }
            if (condition == null || condition.isEmpty()) {
                throw new IllegalStateException("Trigger condition is required");
            }
            return new Trigger(this);
        }
    }

    /**
     * Trigger type enumeration.
     */
    public enum TriggerType {
        /**
         * Triggered when a specific game event occurs.
         * Examples: block broken, entity spawned, item dropped
         */
        EVENT,

        /**
         * Triggered when a condition becomes true.
         * Continuously evaluated, triggers on state change.
         */
        CONDITION,

        /**
         * Triggered at regular time intervals.
         * Uses the delay field as the interval period.
         */
        TIME,

        /**
         * Triggered when the player performs an action.
         * Examples: player breaks block, player places block
         */
        PLAYER_ACTION
    }
}
