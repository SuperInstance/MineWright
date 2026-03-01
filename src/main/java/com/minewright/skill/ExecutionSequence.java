package com.minewright.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a complete execution sequence of actions working toward a goal.
 *
 * <p><b>Purpose:</b></p>
 * <p>ExecutionSequence captures a full task execution from start to finish.
 * It stores the goal, all actions executed, total time, success status, and
 * contextual information. These sequences are analyzed by PatternExtractor
 * to discover reusable skill patterns.</p>
 *
 * <p><b>Voyager Pattern Integration:</b></p>
 * <p>In the Voyager architecture, successful execution sequences are the raw
 * material for skill learning. When an agent completes a task successfully,
 * the sequence is analyzed, parameterized, and stored as a reusable skill.</p>
 *
 * <p><b>Example Sequence:</b></p>
 * <pre>
 * Goal: "Dig a staircase for mining"
 * Actions:
 *   1. Pathfind to starting location
 *   2. Mine block at (x, y, z)
 *   3. Move to next position
 *   4. Place torch every 5 blocks
 *   5. Repeat until depth reached
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is immutable and thread-safe. Use the Builder to construct
 * instances incrementally during task execution.</p>
 *
 * @see ActionRecord
 * @see PatternExtractor
 * @see ExecutionTracker
 * @since 1.0.0
 */
public class ExecutionSequence {
    private final String id;
    private final String agentId;
    private final String goal;
    private final List<ActionRecord> actions;
    private final long totalExecutionTime;
    private final boolean successful;
    private final Map<String, Object> context;
    private final long startTime;
    private final long endTime;

    private ExecutionSequence(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.agentId = builder.agentId;
        this.goal = builder.goal;
        this.actions = List.copyOf(builder.actions);
        this.totalExecutionTime = builder.totalExecutionTime;
        this.successful = builder.successful;
        this.context = Map.copyOf(builder.context);
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
    }

    public String getId() {
        return id;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getGoal() {
        return goal;
    }

    public List<ActionRecord> getActions() {
        return actions;
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    /**
     * Gets the number of actions in this sequence.
     *
     * @return Action count
     */
    public int getActionCount() {
        return actions.size();
    }

    /**
     * Gets the average execution time per action.
     *
     * @return Average time in milliseconds
     */
    public long getAverageActionTime() {
        if (actions.isEmpty()) {
            return 0;
        }
        return totalExecutionTime / actions.size();
    }

    /**
     * Gets the count of successful actions in this sequence.
     *
     * @return Number of successful actions
     */
    public int getSuccessfulActionCount() {
        return (int) actions.stream().filter(ActionRecord::isSuccess).count();
    }

    /**
     * Gets the count of failed actions in this sequence.
     *
     * @return Number of failed actions
     */
    public int getFailedActionCount() {
        return (int) actions.stream().filter(a -> !a.isSuccess()).count();
    }

    /**
     * Checks if this sequence contains only successful actions.
     *
     * @return true if all actions succeeded
     */
    public boolean isAllActionsSuccessful() {
        return actions.stream().allMatch(ActionRecord::isSuccess);
    }

    /**
     * Gets a normalized signature of this sequence for pattern matching.
     * The signature excludes variable parameters like coordinates.
     *
     * @return Normalized signature string
     */
    public String getSignature() {
        StringBuilder signature = new StringBuilder();
        for (ActionRecord action : actions) {
            if (signature.length() > 0) {
                signature.append("->");
            }
            signature.append(action.getNormalizedKey());
        }
        return signature.toString();
    }

    /**
     * Checks if this sequence is similar to another sequence.
     * Similar sequences have the same action types in the same order.
     *
     * @param other The other sequence to compare
     * @return true if sequences are similar
     */
    public boolean isSimilarTo(ExecutionSequence other) {
        if (other == null || this.actions.size() != other.actions.size()) {
            return false;
        }

        for (int i = 0; i < actions.size(); i++) {
            if (!actions.get(i).getActionType().equals(other.actions.get(i).getActionType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a new builder for constructing an ExecutionSequence.
     *
     * @param agentId The agent executing this sequence
     * @param goal    The goal being pursued
     * @return New Builder instance
     */
    public static Builder builder(String agentId, String goal) {
        return new Builder(agentId, goal);
    }

    /**
     * Builder for incrementally constructing ExecutionSequence instances.
     */
    public static class Builder {
        private String id;
        private final String agentId;
        private final String goal;
        private final List<ActionRecord> actions = new ArrayList<>();
        private long totalExecutionTime = 0;
        private boolean successful = true;
        private final Map<String, Object> context = new java.util.HashMap<>();
        private long startTime = System.currentTimeMillis();
        private long endTime;

        private Builder(String agentId, String goal) {
            this.agentId = agentId;
            this.goal = goal;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder addAction(ActionRecord action) {
            this.actions.add(action);
            this.totalExecutionTime += action.getExecutionTime();
            if (!action.isSuccess()) {
                this.successful = false;
            }
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context.clear();
            this.context.putAll(context);
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            this.totalExecutionTime = endTime - startTime;
            return this;
        }

        /**
         * Builds the ExecutionSequence and sets end time to now.
         *
         * @return New ExecutionSequence instance
         */
        public ExecutionSequence build() {
            this.endTime = System.currentTimeMillis();
            this.totalExecutionTime = this.endTime - this.startTime;
            return new ExecutionSequence(this);
        }

        /**
         * Builds the ExecutionSequence with a specific success status.
         *
         * @param successful Whether the sequence was successful
         * @return New ExecutionSequence instance
         */
        public ExecutionSequence build(boolean successful) {
            this.successful = successful;
            return build();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionSequence that = (ExecutionSequence) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ExecutionSequence{" +
            "id='" + id + '\'' +
            ", agentId='" + agentId + '\'' +
            ", goal='" + goal + '\'' +
            ", actions=" + actions.size() +
            ", totalExecutionTime=" + totalExecutionTime +
            ", successful=" + successful +
            '}';
    }
}
