package com.minewright.orchestration;

import com.minewright.action.Task;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a task assigned from the orchestrator to a worker agent.
 *
 * <p>Tracks the full lifecycle of a task from assignment through completion,
 * including progress updates and failure handling.</p>
 *
 * <p><b>Task States:</b></p>
 * <ul>
 *   <li>PENDING - Task created but not yet sent to worker</li>
 *   <li>ASSIGNED - Task sent to worker, awaiting acknowledgment</li>
 *   <li>ACCEPTED - Worker acknowledged and started task</li>
 *   <li>IN_PROGRESS - Worker actively executing task</li>
 *   <li>COMPLETED - Task successfully completed</li>
 *   <li>FAILED - Task failed</li>
 *   <li>CANCELLED - Task cancelled before completion</li>
 *   <li>REASSIGNED - Task reassigned to different worker</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class TaskAssignment {

    /**
     * State of an assigned task.
     */
    public enum State {
        PENDING("pending", false),
        ASSIGNED("assigned", false),
        ACCEPTED("accepted", false),
        IN_PROGRESS("in_progress", false),
        COMPLETED("completed", true),
        FAILED("failed", true),
        CANCELLED("cancelled", true),
        REASSIGNED("reassigned", false);

        private final String code;
        private final boolean terminal;

        State(String code, boolean terminal) {
            this.code = code;
            this.terminal = terminal;
        }

        public String getCode() {
            return code;
        }

        public boolean isTerminal() {
            return terminal;
        }

        public boolean isActive() {
            return this == ACCEPTED || this == IN_PROGRESS;
        }
    }

    /**
     * Priority levels for task assignments.
     */
    public enum Priority {
        LOW(1),
        NORMAL(5),
        HIGH(10),
        URGENT(20),
        CRITICAL(50);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    // Task identification
    private final String assignmentId;
    private final String parentPlanId; // Links to overall plan
    private final Task originalTask;

    // Assignment details
    private final String foremanId;
    private String assignedWorkerId;
    private final String taskDescription;
    private final Map<String, Object> parameters;
    private final Priority priority;

    // State tracking
    private volatile State state;
    private volatile int progressPercent;
    private volatile String statusMessage;
    private volatile int retryCount;

    // Timestamps
    private final Instant createdAt;
    private volatile Instant assignedAt;
    private volatile Instant startedAt;
    private volatile Instant completedAt;

    // Results
    private volatile boolean success;
    private volatile String result;
    private volatile String failureReason;

    // Dependencies
    private final Map<String, TaskAssignment> dependencies;
    private volatile boolean dependenciesMet;

    /**
     * Creates a new task assignment.
     */
    public TaskAssignment(String foremanId, Task task, String parentPlanId) {
        this.assignmentId = UUID.randomUUID().toString().substring(0, 8);
        this.parentPlanId = parentPlanId;
        this.originalTask = task;

        this.foremanId = foremanId;
        this.taskDescription = task.getAction();
        this.parameters = new ConcurrentHashMap<>(task.getParameters());
        this.priority = Priority.NORMAL;

        this.state = State.PENDING;
        this.progressPercent = 0;
        this.statusMessage = "Pending assignment";
        this.retryCount = 0;

        this.createdAt = Instant.now();
        this.dependencies = new ConcurrentHashMap<>();
        this.dependenciesMet = true; // No dependencies by default
    }

    // Getters

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getParentPlanId() {
        return parentPlanId;
    }

    public Task getOriginalTask() {
        return originalTask;
    }

    public String getForemanId() {
        return foremanId;
    }

    public String getAssignedWorkerId() {
        return assignedWorkerId;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Priority getPriority() {
        return priority;
    }

    public State getState() {
        return state;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public boolean isTerminal() {
        return state.isTerminal();
    }

    public boolean isActive() {
        return state.isActive();
    }

    public Map<String, TaskAssignment> getDependencies() {
        return dependencies;
    }

    public boolean areDependenciesMet() {
        return dependenciesMet;
    }

    // State transitions (called by OrchestratorService)

    /**
     * Assigns the task to a worker.
     */
    public void assignTo(String workerId) {
        if (state != State.PENDING && state != State.REASSIGNED) {
            throw new IllegalStateException("Cannot assign task in state: " + state);
        }
        this.assignedWorkerId = workerId;
        this.state = State.ASSIGNED;
        this.assignedAt = Instant.now();
        this.statusMessage = "Assigned to " + workerId;
    }

    /**
     * Worker accepts the task.
     */
    public void accept() {
        if (state != State.ASSIGNED) {
            throw new IllegalStateException("Cannot accept task in state: " + state);
        }
        this.state = State.ACCEPTED;
        this.startedAt = Instant.now();
        this.statusMessage = "Accepted by worker";
    }

    /**
     * Worker starts executing the task.
     */
    public void start() {
        if (state != State.ACCEPTED) {
            throw new IllegalStateException("Cannot start task in state: " + state);
        }
        this.state = State.IN_PROGRESS;
        this.statusMessage = "Execution started";
    }

    /**
     * Updates task progress.
     */
    public void updateProgress(int percent, String message) {
        if (state != State.IN_PROGRESS && state != State.ACCEPTED) {
            return; // Ignore progress for non-active tasks
        }
        this.progressPercent = Math.min(100, Math.max(0, percent));
        this.statusMessage = message;
    }

    /**
     * Marks task as successfully completed.
     */
    public void complete(String result) {
        this.state = State.COMPLETED;
        this.progressPercent = 100;
        this.success = true;
        this.result = result;
        this.completedAt = Instant.now();
        this.statusMessage = "Completed successfully";
    }

    /**
     * Marks task as failed.
     */
    public void fail(String reason) {
        this.state = State.FAILED;
        this.success = false;
        this.failureReason = reason;
        this.completedAt = Instant.now();
        this.statusMessage = "Failed: " + reason;
    }

    /**
     * Cancels the task.
     */
    public void cancel(String reason) {
        this.state = State.CANCELLED;
        this.statusMessage = "Cancelled: " + reason;
        this.completedAt = Instant.now();
    }

    /**
     * Reassigns the task to a different worker.
     */
    public void reassign(String newWorkerId, String reason) {
        this.state = State.REASSIGNED;
        this.assignedWorkerId = newWorkerId;
        this.retryCount++;
        this.statusMessage = "Reassigned: " + reason;
        this.assignedAt = Instant.now();
    }

    /**
     * Adds a dependency on another task.
     */
    public void addDependency(TaskAssignment dependency) {
        dependencies.put(dependency.getAssignmentId(), dependency);
        dependenciesMet = false;
    }

    /**
     * Checks if all dependencies are satisfied.
     */
    public boolean checkDependencies() {
        if (dependencies.isEmpty()) {
            dependenciesMet = true;
            return true;
        }

        dependenciesMet = dependencies.values().stream()
            .allMatch(d -> d.getState() == State.COMPLETED && d.isSuccess());

        return dependenciesMet;
    }

    /**
     * Calculates duration in milliseconds.
     */
    public long getDurationMs() {
        if (startedAt == null) return 0;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return end.toEpochMilli() - startedAt.toEpochMilli();
    }

    /**
     * Returns a summary string for logging.
     */
    public String getSummary() {
        return String.format("TaskAssignment[%s] %s -> %s: %s (%d%%) [%s]",
            assignmentId, taskDescription,
            assignedWorkerId != null ? assignedWorkerId : "unassigned",
            statusMessage, progressPercent, state);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Builder for creating task assignments with custom settings.
     */
    public static class Builder {
        private String foremanId;
        private Task task;
        private String parentPlanId;
        private Priority priority = Priority.NORMAL;
        private String assignedWorkerId;

        public Builder foremanId(String foremanId) {
            this.foremanId = foremanId;
            return this;
        }

        public Builder task(Task task) {
            this.task = task;
            return this;
        }

        public Builder parentPlanId(String parentPlanId) {
            this.parentPlanId = parentPlanId;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder assignedWorkerId(String workerId) {
            this.assignedWorkerId = workerId;
            return this;
        }

        public TaskAssignment build() {
            if (foremanId == null || task == null) {
                throw new IllegalStateException("foremanId and task are required");
            }
            TaskAssignment assignment = new TaskAssignment(foremanId, task, parentPlanId);
            if (assignedWorkerId != null) {
                assignment.assignTo(assignedWorkerId);
            }
            return assignment;
        }
    }
}
