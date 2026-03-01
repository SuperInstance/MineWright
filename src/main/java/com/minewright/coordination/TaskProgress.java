package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks progress of tasks allocated through the Contract Net Protocol.
 *
 * <p><b>Progress Tracking:</b></p>
 * <ul>
 *   <li>Task state transitions through defined stages</li>
 *   <li>Percentage completion tracking</li>
 *   * <li>Checkpoint-based progress reporting</li>
 *   <li>Current step tracking</li>
 *   <li>Performance metrics collection</li>
 * </ul>
 *
 * <p><b>State Machine:</b></p>
 * <pre>
 * ANNOUNCED -> BIDDING -> ASSIGNED -> IN_PROGRESS -> COMPLETED
 *                                          |
 *                                          v
 *                                       FAILED
 * </pre>
 *
 * @see ContractNetManager
 * @since 1.3.0
 */
public class TaskProgress {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(TaskProgress.class);

    /**
     * Possible states for a task during execution.
     */
    public enum TaskStatus {
        /** Task has been announced but not yet bid on */
        ANNOUNCED,
        /** Collecting bids from agents */
        BIDDING,
        /** Contract awarded to an agent */
        ASSIGNED,
        /** Agent is actively working on the task */
        IN_PROGRESS,
        /** Task completed successfully */
        COMPLETED,
        /** Task failed or was cancelled */
        FAILED,
        /** Task timed out */
        TIMEOUT
    }

    /**
     * Represents a checkpoint in task execution.
     */
    public static class Checkpoint {
        private final String name;
        private final double progressPercentage;
        private final LocalDateTime reachedTime;
        private final String description;
        private final Map<String, Object> metadata;

        public Checkpoint(String name, double progressPercentage, String description) {
            this(name, progressPercentage, description, LocalDateTime.now(), Map.of());
        }

        public Checkpoint(String name, double progressPercentage, String description,
                         LocalDateTime reachedTime, Map<String, Object> metadata) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Checkpoint name cannot be null or blank");
            }
            if (progressPercentage < 0.0 || progressPercentage > 100.0) {
                throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
            }

            this.name = name;
            this.progressPercentage = progressPercentage;
            this.description = description;
            this.reachedTime = reachedTime;
            this.metadata = metadata != null ? new ConcurrentHashMap<>(metadata) : Map.of();
        }

        public String getName() {
            return name;
        }

        public double getProgressPercentage() {
            return progressPercentage;
        }

        public LocalDateTime getReachedTime() {
            return reachedTime;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }

        @Override
        public String toString() {
            return String.format("Checkpoint[%s: %.1f%% - %s]",
                name, progressPercentage, description);
        }
    }

    /**
     * Listener for task progress events.
     */
    public interface ProgressListener {
        /**
         * Called when task status changes.
         */
        default void onStatusChanged(String taskId, TaskStatus oldStatus, TaskStatus newStatus) {}

        /**
         * Called when progress percentage updates.
         */
        default void onProgressUpdate(String taskId, double oldProgress, double newProgress) {}

        /**
         * Called when a checkpoint is reached.
         */
        default void onCheckpointReached(String taskId, Checkpoint checkpoint) {}

        /**
         * Called when current step changes.
         */
        default void onStepChanged(String taskId, String oldStep, String newStep) {}

        /**
         * Called when task completes.
         */
        default void onTaskCompleted(String taskId, boolean success, String result) {}
    }

    private final String taskId;
    private final String announcementId;
    private final UUID assignedAgentId;
    private final AtomicReference<TaskStatus> status;
    private final AtomicDouble completionPercentage;
    private final List<Checkpoint> checkpoints;
    private final AtomicReference<String> currentStep;
    private final LocalDateTime createdTime;
    private final LocalDateTime assignedTime;
    private final AtomicReference<LocalDateTime> completedTime;
    private final String description;
    private final String failureReason;
    private final Map<String, Object> resultData;
    private final List<ProgressListener> listeners;
    private final AtomicInteger stepsCompleted;
    private final AtomicInteger totalSteps;

    /**
     * Creates a new task progress tracker.
     *
     * @param taskId Unique task identifier
     * @param announcementId Contract Net announcement ID
     * @param assignedAgentId ID of the agent assigned to the task
     */
    public TaskProgress(String taskId, String announcementId, UUID assignedAgentId) {
        this(taskId, announcementId, assignedAgentId, null);
    }

    /**
     * Creates a new task progress tracker with description.
     *
     * @param taskId Unique task identifier
     * @param announcementId Contract Net announcement ID
     * @param assignedAgentId ID of the agent assigned to the task
     * @param description Task description
     */
    public TaskProgress(String taskId, String announcementId, UUID assignedAgentId, String description) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        if (announcementId == null || announcementId.isBlank()) {
            throw new IllegalArgumentException("Announcement ID cannot be null or blank");
        }
        if (assignedAgentId == null) {
            throw new IllegalArgumentException("Assigned agent ID cannot be null");
        }

        this.taskId = taskId;
        this.announcementId = announcementId;
        this.assignedAgentId = assignedAgentId;
        this.status = new AtomicReference<>(TaskStatus.ASSIGNED);
        this.completionPercentage = new AtomicDouble(0.0);
        this.checkpoints = new CopyOnWriteArrayList<>();
        this.currentStep = new AtomicReference<>("Initializing");
        this.createdTime = LocalDateTime.now();
        this.assignedTime = LocalDateTime.now();
        this.completedTime = new AtomicReference<>(null);
        this.description = description;
        this.failureReason = null;
        this.resultData = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.stepsCompleted = new AtomicInteger(0);
        this.totalSteps = new AtomicInteger(0);

        LOGGER.debug("Created task progress tracker for {} (agent: {})",
            taskId, assignedAgentId.toString().substring(0, 8));
    }

    // ========== Accessors ==========

    public String getTaskId() {
        return taskId;
    }

    public String getAnnouncementId() {
        return announcementId;
    }

    public UUID getAssignedAgentId() {
        return assignedAgentId;
    }

    public TaskStatus getStatus() {
        return status.get();
    }

    public double getCompletionPercentage() {
        return completionPercentage.get();
    }

    public List<Checkpoint> getCheckpoints() {
        return Collections.unmodifiableList(checkpoints);
    }

    public String getCurrentStep() {
        return currentStep.get();
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getAssignedTime() {
        return assignedTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime.get();
    }

    public String getDescription() {
        return description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Map<String, Object> getResultData() {
        return Collections.unmodifiableMap(resultData);
    }

    public int getStepsCompleted() {
        return stepsCompleted.get();
    }

    public int getTotalSteps() {
        return totalSteps.get();
    }

    // ========== State Management ==========

    /**
     * Updates the task status.
     *
     * @param newStatus The new status
     * @return true if status was changed
     */
    public boolean setStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            return false;
        }

        TaskStatus oldStatus = status.getAndSet(newStatus);

        if (!oldStatus.equals(newStatus)) {
            LOGGER.info("Task {} status: {} -> {}",
                taskId, oldStatus, newStatus);

            if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED || newStatus == TaskStatus.TIMEOUT) {
                completedTime.set(LocalDateTime.now());
            }

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onStatusChanged(taskId, oldStatus, newStatus);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onStatusChanged", e);
                }
            });

            return true;
        }

        return false;
    }

    /**
     * Checks if the task is in a terminal state.
     *
     * @return true if task is complete, failed, or timed out
     */
    public boolean isTerminated() {
        TaskStatus s = status.get();
        return s == TaskStatus.COMPLETED || s == TaskStatus.FAILED || s == TaskStatus.TIMEOUT;
    }

    /**
     * Checks if the task is currently active.
     *
     * @return true if task is assigned or in progress
     */
    public boolean isActive() {
        TaskStatus s = status.get();
        return s == TaskStatus.ASSIGNED || s == TaskStatus.IN_PROGRESS;
    }

    // ========== Progress Tracking ==========

    /**
     * Updates the completion percentage.
     *
     * @param percentage New percentage (0.0-100.0)
     * @return true if percentage was changed
     */
    public boolean setCompletionPercentage(double percentage) {
        double clamped = Math.max(0.0, Math.min(100.0, percentage));
        double oldPercentage = completionPercentage.getAndSet(clamped);

        if (Math.abs(oldPercentage - clamped) > 0.01) {
            LOGGER.debug("Task {} progress: {:.1f}%", taskId, clamped);

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onProgressUpdate(taskId, oldPercentage, clamped);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onProgressUpdate", e);
                }
            });

            return true;
        }

        return false;
    }

    /**
     * Increments the completion percentage by a specified amount.
     *
     * @param amount Amount to increment (can be negative)
     * @return The new percentage
     */
    public double incrementProgress(double amount) {
        double newPercentage = completionPercentage.addAndGet(amount);
        newPercentage = Math.max(0.0, Math.min(100.0, newPercentage));
        completionPercentage.set(newPercentage);

        LOGGER.debug("Task {} progress: {:.1f}% ({:+.1f}%)",
            taskId, newPercentage, amount);

        return newPercentage;
    }

    // ========== Checkpoints ==========

    /**
     * Adds a checkpoint to the progress.
     *
     * @param checkpoint The checkpoint to add
     * @return true if checkpoint was added
     */
    public boolean addCheckpoint(Checkpoint checkpoint) {
        if (checkpoint == null) {
            return false;
        }

        checkpoints.add(checkpoint);
        setCompletionPercentage(checkpoint.getProgressPercentage());

        LOGGER.info("Task {} checkpoint: {}", taskId, checkpoint.getName());

        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onCheckpointReached(taskId, checkpoint);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onCheckpointReached", e);
            }
        });

        return true;
    }

    /**
     * Creates and adds a checkpoint.
     *
     * @param name Checkpoint name
     * @param progressPercentage Progress at this checkpoint (0-100)
     * @param description Checkpoint description
     * @return The created checkpoint
     */
    public Checkpoint addCheckpoint(String name, double progressPercentage, String description) {
        Checkpoint checkpoint = new Checkpoint(name, progressPercentage, description);
        addCheckpoint(checkpoint);
        return checkpoint;
    }

    /**
     * Gets the most recent checkpoint.
     *
     * @return The latest checkpoint, or null if none
     */
    public Checkpoint getLastCheckpoint() {
        return checkpoints.isEmpty() ? null : checkpoints.get(checkpoints.size() - 1);
    }

    // ========== Step Tracking ==========

    /**
     * Updates the current step.
     *
     * @param step New step description
     * @return true if step was changed
     */
    public boolean setCurrentStep(String step) {
        if (step == null) {
            return false;
        }

        String oldStep = currentStep.getAndSet(step);

        if (!step.equals(oldStep)) {
            LOGGER.info("Task {} step: {} -> {}", taskId, oldStep, step);

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onStepChanged(taskId, oldStep, step);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onStepChanged", e);
                }
            });

            return true;
        }

        return false;
    }

    /**
     * Completes the current step and increments the counter.
     *
     * @param nextStep The next step to begin
     * @return The number of completed steps
     */
    public int completeStep(String nextStep) {
        int completed = stepsCompleted.incrementAndGet();
        setCurrentStep(nextStep);
        return completed;
    }

    /**
     * Sets the total number of steps.
     *
     * @param total Total steps
     */
    public void setTotalSteps(int total) {
        if (total >= 0) {
            totalSteps.set(total);
        }
    }

    // ========== Completion ==========

    /**
     * Marks the task as completed successfully.
     *
     * @param result Completion result description
     * @return true if marked complete
     */
    public boolean markCompleted(String result) {
        if (isTerminated()) {
            return false;
        }

        setStatus(TaskStatus.COMPLETED);
        setCompletionPercentage(100.0);
        resultData.put("result", result);

        LOGGER.info("Task {} completed successfully: {}", taskId, result);

        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onTaskCompleted(taskId, true, result);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onTaskCompleted", e);
            }
        });

        return true;
    }

    /**
     * Marks the task as failed.
     *
     * @param reason Failure reason
     * @return true if marked failed
     */
    public boolean markFailed(String reason) {
        if (isTerminated()) {
            return false;
        }

        setStatus(TaskStatus.FAILED);
        resultData.put("failureReason", reason);

        LOGGER.warn("Task {} failed: {}", taskId, reason);

        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onTaskCompleted(taskId, false, reason);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onTaskCompleted", e);
            }
        });

        return true;
    }

    /**
     * Marks the task as timed out.
     *
     * @return true if marked timed out
     */
    public boolean markTimedOut() {
        if (isTerminated()) {
            return false;
        }

        setStatus(TaskStatus.TIMEOUT);
        resultData.put("timeout", true);

        LOGGER.warn("Task {} timed out", taskId);

        listeners.forEach(listener -> {
            try {
                listener.onTaskCompleted(taskId, false, "Timeout");
            } catch (Exception e) {
                LOGGER.warn("Listener error in onTaskCompleted", e);
            }
        });

        return true;
    }

    // ========== Result Data ==========

    /**
     * Adds result data.
     *
     * @param key Data key
     * @param value Data value
     */
    public void putResultData(String key, Object value) {
        resultData.put(key, value);
    }

    /**
     * Gets result data value.
     *
     * @param key Data key
     * @param <T> Expected type
     * @return Value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getResultData(String key) {
        return (T) resultData.get(key);
    }

    // ========== Listeners ==========

    /**
     * Adds a progress listener.
     *
     * @param listener The listener to add
     */
    public void addListener(ProgressListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a progress listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    // ========== Utility ==========

    /**
     * Gets the elapsed time since creation.
     *
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTime() {
        LocalDateTime end = completedTime.get() != null ? completedTime.get() : LocalDateTime.now();
        return java.time.Duration.between(createdTime, end).toMillis();
    }

    /**
     * Gets the execution time (time from assignment to completion).
     *
     * @return Execution time in milliseconds, or 0 if not complete
     */
    public long getExecutionTime() {
        if (completedTime.get() == null) {
            return 0;
        }
        return java.time.Duration.between(assignedTime, completedTime.get()).toMillis();
    }

    /**
     * Calculates progress rate (percentage per second).
     *
     * @return Progress rate, or 0 if insufficient data
     */
    public double getProgressRate() {
        double progress = completionPercentage.get();
        long elapsed = getElapsedTime();

        if (elapsed < 1000 || progress < 0.01) {
            return 0.0;
        }

        return (progress / elapsed) * 1000.0; // % per second
    }

    /**
     * Estimates time to completion based on current rate.
     *
     * @return Estimated milliseconds remaining, or -1 if cannot estimate
     */
    public long getEstimatedTimeRemaining() {
        double rate = getProgressRate();
        if (rate <= 0.0) {
            return -1L;
        }

        double remaining = 100.0 - completionPercentage.get();
        return (long) ((remaining / rate) * 1000.0);
    }

    @Override
    public String toString() {
        return String.format("TaskProgress[id=%s, agent=%s, status=%s, progress=%.1f%%, step='%s']",
            taskId, assignedAgentId.toString().substring(0, 8),
            status.get(), completionPercentage.get(), currentStep.get());
    }

    /**
     * Atomic double wrapper for thread-safe double operations.
     */
    private static class AtomicDouble {
        private volatile double value;

        public AtomicDouble(double initialValue) {
            this.value = initialValue;
        }

        public double get() {
            return value;
        }

        public double getAndSet(double newValue) {
            double oldValue = value;
            value = newValue;
            return oldValue;
        }

        public double addAndGet(double delta) {
            value += delta;
            return value;
        }

        public void set(double newValue) {
            value = newValue;
        }
    }

    /**
     * Creates a new builder for task progress.
     *
     * @param taskId Task ID
     * @param announcementId Announcement ID
     * @param agentId Assigned agent ID
     * @return New builder
     */
    public static Builder builder(String taskId, String announcementId, UUID agentId) {
        return new Builder(taskId, announcementId, agentId);
    }

    /**
     * Builder for creating TaskProgress instances.
     */
    public static class Builder {
        private final String taskId;
        private final String announcementId;
        private final UUID agentId;
        private String description;
        private int totalSteps;
        private final List<Checkpoint> initialCheckpoints = new ArrayList<>();

        private Builder(String taskId, String announcementId, UUID agentId) {
            this.taskId = taskId;
            this.announcementId = announcementId;
            this.agentId = agentId;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder totalSteps(int total) {
            this.totalSteps = total;
            return this;
        }

        public Builder addCheckpoint(String name, double progress, String description) {
            initialCheckpoints.add(new Checkpoint(name, progress, description));
            return this;
        }

        public TaskProgress build() {
            TaskProgress progress = new TaskProgress(taskId, announcementId, agentId, description);
            progress.setTotalSteps(totalSteps);
            initialCheckpoints.forEach(progress::addCheckpoint);
            return progress;
        }
    }
}
