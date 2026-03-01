package com.minewright.htn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a decomposition method for compound tasks in Hierarchical Task Network (HTN) planning.
 *
 * <p><b>HTN Methods:</b></p>
 * <p>A method defines one possible way to decompose a compound task into primitive actions.
 * A compound task may have multiple methods, providing alternative approaches based on
 * preconditions (world state).</p>
 *
 * <p><b>Method Components:</b></p>
 * <ul>
 *   <li><b>Task Name:</b> The compound task this method decomposes (e.g., "build_house")</li>
 *   <li><b>Preconditions:</b> Predicate that must be satisfied for this method to apply</li>
 *   <li><b>Subtasks:</b> Ordered list of tasks that achieve the compound task</li>
 *   <li><b>Priority:</b> Numeric preference for method selection (higher = preferred)</li>
 * </ul>
 *
 * <p><b>Method Selection:</b></p>
 * <pre>
 * Compound Task: "build_house"
 *     │
 *     ├─► Method 1 (priority: 100): "build_with_materials"
 *     │   ├── Precondition: Has all materials in inventory
 *     │   └── Subtasks: [pathfind_to_site, clear_area, build_walls, add_roof]
 *     │
 *     └─► Method 2 (priority: 50): "build_with_gathering"
 *         ├── Precondition: Always true (fallback)
 *         └── Subtasks: [gather_wood, craft_planks, pathfind_to_site, build]
 *
 * If Method 1 preconditions are met, use Method 1 (higher priority).
 * Otherwise, try Method 2 (fallback).
 * </pre>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * HTNMethod buildHouseBasic = HTNMethod.builder("build_house_basic", "build_house")
 *     .precondition(state -> state.hasMaterials("oak_planks", 192))
 *     .subtask(HTNTask.primitive("pathfind")
 *         .parameter("target", "build_site")
 *         .build())
 *     .subtask(HTNTask.primitive("clear_area")
 *         .parameter("width", 5)
 *         .parameter("depth", 5)
 *         .build())
 *     .subtask(HTNTask.compound("construct_walls")
 *         .parameter("material", "oak_planks")
 *         .parameter("height", 3)
 *         .build())
 *     .priority(100)
 *     .build();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe after construction.
 * Preconditions are evaluated during planning and should be thread-safe.</p>
 *
 * @see HTNTask
 * @see HTNDomain
 * @see HTNWorldState
 *
 * @since 1.0.0
 */
public class HTNMethod {
    /**
     * Unique identifier for this method (e.g., "build_house_basic").
     * Used for debugging and method tracking.
     */
    private final String methodName;

    /**
     * The compound task this method decomposes.
     * Must match the task name in domain lookups.
     */
    private final String taskName;

    /**
     * Preconditions that must be satisfied for this method to apply.
     * Evaluated against the current world state during planning.
     * Null means no preconditions (always applicable).
     */
    private final Predicate<HTNWorldState> preconditions;

    /**
     * Ordered list of subtasks that decompose the compound task.
     * Executed sequentially during planning.
     */
    private final List<HTNTask> subtasks;

    /**
     * Priority for method selection when multiple methods are applicable.
     * Higher priority methods are tried first.
     * Default is 0.
     */
    private final int priority;

    /**
     * Optional description of this method for debugging and logging.
     */
    private final String description;

    /**
     * Builder for creating HTNMethod instances.
     */
    public static class Builder {
        private String methodName;
        private String taskName;
        private Predicate<HTNWorldState> preconditions = state -> true; // Default: always applicable
        private final List<HTNTask> subtasks = new ArrayList<>();
        private int priority = 0;
        private String description;

        /**
         * Sets the method name.
         *
         * @param methodName Unique method identifier
         * @return This builder for chaining
         */
        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        /**
         * Sets the task name this method decomposes.
         *
         * @param taskName Compound task name
         * @return This builder for chaining
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * Sets preconditions for this method.
         * The predicate is tested against the world state during planning.
         *
         * @param preconditions Predicate that returns true if method is applicable
         * @return This builder for chaining
         */
        public Builder precondition(Predicate<HTNWorldState> preconditions) {
            this.preconditions = preconditions != null ? preconditions : state -> true;
            return this;
        }

        /**
         * Sets a simple boolean precondition checking a world state property.
         *
         * @param propertyKey   Property to check
         * @param propertyValue Required value
         * @return This builder for chaining
         */
        public Builder precondition(String propertyKey, Object propertyValue) {
            this.preconditions = state -> Objects.equals(state.getProperty(propertyKey), propertyValue);
            return this;
        }

        /**
         * Adds a subtask to this method's decomposition.
         *
         * @param subtask The subtask to add
         * @return This builder for chaining
         */
        public Builder subtask(HTNTask subtask) {
            if (subtask != null) {
                this.subtasks.add(subtask);
            }
            return this;
        }

        /**
         * Adds multiple subtasks at once.
         *
         * @param tasks List of subtasks to add
         * @return This builder for chaining
         */
        public Builder subtasks(List<HTNTask> tasks) {
            if (tasks != null) {
                this.subtasks.addAll(tasks);
            }
            return this;
        }

        /**
         * Sets the priority for this method.
         * Higher priority methods are tried first during decomposition.
         *
         * @param priority Priority value (higher = preferred)
         * @return This builder for chaining
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets a description for this method.
         *
         * @param description Method description
         * @return This builder for chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the HTNMethod instance.
         *
         * @return A new HTNMethod with the configured properties
         * @throws IllegalArgumentException if methodName or taskName is null/empty
         * @throws IllegalArgumentException if no subtasks are defined
         */
        public HTNMethod build() {
            if (methodName == null || methodName.trim().isEmpty()) {
                throw new IllegalArgumentException("Method name cannot be null or empty");
            }
            if (taskName == null || taskName.trim().isEmpty()) {
                throw new IllegalArgumentException("Task name cannot be null or empty");
            }
            if (subtasks.isEmpty()) {
                throw new IllegalArgumentException("Method must have at least one subtask: " + methodName);
            }

            return new HTNMethod(
                methodName,
                taskName,
                preconditions,
                List.copyOf(subtasks), // Immutable copy
                priority,
                description
            );
        }
    }

    /**
     * Creates a new builder for the specified method and task name.
     * Convenience method for fluent API.
     *
     * @param methodName Method identifier
     * @param taskName   Compound task to decompose
     * @return A new Builder
     */
    public static Builder builder(String methodName, String taskName) {
        return new Builder()
            .methodName(methodName)
            .taskName(taskName);
    }

    /**
     * Private constructor. Use Builder to create instances.
     */
    private HTNMethod(String methodName, String taskName, Predicate<HTNWorldState> preconditions,
                      List<HTNTask> subtasks, int priority, String description) {
        this.methodName = methodName;
        this.taskName = taskName;
        this.preconditions = preconditions;
        this.subtasks = subtasks;
        this.priority = priority;
        this.description = description;
    }

    /**
     * Gets the method name.
     *
     * @return Method identifier
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the task name this method decomposes.
     *
     * @return Compound task name
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Gets the subtasks in this method's decomposition.
     *
     * @return Immutable list of subtasks
     */
    public List<HTNTask> getSubtasks() {
        return subtasks;
    }

    /**
     * Gets the priority of this method.
     *
     * @return Priority value (higher = preferred)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the method description.
     *
     * @return Description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this method's preconditions are satisfied in the given world state.
     *
     * @param worldState Current world state
     * @return true if this method is applicable
     */
    public boolean checkPreconditions(HTNWorldState worldState) {
        if (worldState == null) {
            return false;
        }
        try {
            return preconditions.test(worldState);
        } catch (Exception e) {
            // If precondition evaluation fails, consider it not satisfied
            return false;
        }
    }

    /**
     * Checks if this method has any preconditions.
     *
     * @return true if preconditions are defined
     */
    public boolean hasPreconditions() {
        return preconditions != null;
    }

    /**
     * Gets the number of subtasks in this method.
     *
     * @return Subtask count
     */
    public int getSubtaskCount() {
        return subtasks.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTNMethod htnMethod = (HTNMethod) o;
        return Objects.equals(methodName, htnMethod.methodName) &&
               Objects.equals(taskName, htnMethod.taskName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, taskName);
    }

    @Override
    public String toString() {
        return "HTNMethod{" +
               "methodName='" + methodName + '\'' +
               ", taskName='" + taskName + '\'' +
               ", priority=" + priority +
               ", subtasks=" + subtasks.size() +
               ", description='" + description + '\'' +
               '}';
    }

    /**
     * Creates a detailed string representation including all subtasks.
     *
     * @return Detailed method description
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTNMethod{");
        sb.append("methodName='").append(methodName).append('\'');
        sb.append(", taskName='").append(taskName).append('\'');
        sb.append(", priority=").append(priority);
        if (description != null) {
            sb.append(", description='").append(description).append('\'');
        }
        sb.append(", subtasks=[");
        for (int i = 0; i < subtasks.size(); i++) {
            if (i > 0) sb.append(", ");
            HTNTask task = subtasks.get(i);
            sb.append(task.getName()).append("(").append(task.getType()).append(")");
        }
        sb.append("]}");
        return sb.toString();
    }
}
