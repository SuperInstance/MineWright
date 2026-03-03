package com.minewright.skill;

import com.minewright.action.Task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Context for skill validation containing execution state and results.
 *
 * <p><b>Purpose:</b></p>
 * <p>Provides all necessary information for the CriticAgent to validate
 * whether a skill execution was successful.</p>
 *
 * @see CriticAgent
 * @see SkillRefinementLoop
 * @since 1.0.0
 */
public class ValidationContext {
    private final Task task;
    private final Map<String, Object> state;
    private final Set<String> collectedItems;
    private final Set<String> errors;
    private final long executionTimeMs;
    private final boolean taskCompleted;

    /**
     * Creates a new ValidationContext.
     *
     * @param task The task being executed
     * @param taskCompleted Whether the task completed successfully
     */
    public ValidationContext(Task task, boolean taskCompleted) {
        this.task = task;
        this.taskCompleted = taskCompleted;
        this.state = new HashMap<>();
        this.collectedItems = new HashSet<>();
        this.errors = new HashSet<>();
        this.executionTimeMs = 0;
    }

    /**
     * Creates a new ValidationContext with full details.
     *
     * @param task The task being executed
     * @param taskCompleted Whether the task completed successfully
     * @param state The world state after execution
     * @param collectedItems Items collected during execution
     * @param errors Errors that occurred during execution
     * @param executionTimeMs Execution time in milliseconds
     */
    public ValidationContext(Task task, boolean taskCompleted,
                            Map<String, Object> state,
                            Set<String> collectedItems,
                            Set<String> errors,
                            long executionTimeMs) {
        this.task = task;
        this.taskCompleted = taskCompleted;
        this.state = state != null ? new HashMap<>(state) : new HashMap<>();
        this.collectedItems = collectedItems != null ? new HashSet<>(collectedItems) : new HashSet<>();
        this.errors = errors != null ? new HashSet<>(errors) : new HashSet<>();
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Gets the task being executed.
     *
     * @return The task
     */
    public Task getTask() {
        return task;
    }

    /**
     * Checks if the task was completed.
     *
     * @return true if task completed successfully
     */
    public boolean taskCompleted() {
        return taskCompleted;
    }

    /**
     * Checks if any errors occurred during execution.
     *
     * @return true if errors occurred
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets the errors that occurred during execution.
     *
     * @return Set of error messages
     */
    public String getErrors() {
        return String.join(", ", errors);
    }

    /**
     * Gets a value from the execution state.
     *
     * @param key The state key
     * @return The state value, or null if not present
     */
    public Object getState(String key) {
        return state.get(key);
    }

    /**
     * Gets all state keys.
     *
     * @return Set of state keys
     */
    public Set<String> getStateKeys() {
        return state.keySet();
    }

    /**
     * Gets the current world state as a string.
     *
     * @return State description
     */
    public String getCurrentState() {
        return state.toString();
    }

    /**
     * Checks if an item was collected during execution.
     *
     * @param itemName The item name
     * @return true if the item was collected
     */
    public boolean hasCollectedItem(String itemName) {
        return collectedItems.contains(itemName);
    }

    /**
     * Gets the execution time in milliseconds.
     *
     * @return Execution time
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Checks if the context has a specific state key.
     *
     * @param key The state key
     * @return true if the key exists
     */
    public boolean hasState(String key) {
        return state.containsKey(key);
    }

    /**
     * Checks if the context has a specific item.
     *
     * @param itemName The item name
     * @return true if the item is available
     */
    public boolean hasItem(String itemName) {
        return collectedItems.contains(itemName) || hasState(itemName);
    }

    /**
     * Builder for creating ValidationContext instances.
     */
    public static class Builder {
        private Task task;
        private boolean taskCompleted;
        private Map<String, Object> state = new HashMap<>();
        private Set<String> collectedItems = new HashSet<>();
        private Set<String> errors = new HashSet<>();
        private long executionTimeMs;

        public Builder task(Task task) {
            this.task = task;
            return this;
        }

        public Builder taskCompleted(boolean completed) {
            this.taskCompleted = completed;
            return this;
        }

        public Builder state(String key, Object value) {
            this.state.put(key, value);
            return this;
        }

        public Builder state(Map<String, Object> state) {
            this.state.putAll(state);
            return this;
        }

        public Builder collectedItem(String item) {
            this.collectedItems.add(item);
            return this;
        }

        public Builder collectedItems(Set<String> items) {
            this.collectedItems.addAll(items);
            return this;
        }

        public Builder error(String error) {
            this.errors.add(error);
            return this;
        }

        public Builder errors(Set<String> errors) {
            this.errors.addAll(errors);
            return this;
        }

        public Builder executionTimeMs(long timeMs) {
            this.executionTimeMs = timeMs;
            return this;
        }

        public ValidationContext build() {
            return new ValidationContext(task, taskCompleted, state, collectedItems, errors, executionTimeMs);
        }
    }
}
