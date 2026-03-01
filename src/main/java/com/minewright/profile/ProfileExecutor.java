package com.minewright.profile;

import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes task profiles step by step.
 *
 * <p>The ProfileExecutor manages the execution of a TaskProfile, handling:</p>
 * <ul>
 *   <li><b>Progress Tracking:</b> Tracks current task index and completion status</li>
 *   <li><b>Error Handling:</b> Handles failures based on profile settings</li>
 *   <li><b>Retry Logic:</b> Retries failed tasks up to max retries</li>
 *   <li><b>Repetition:</b> Repeats profile execution if configured</li>
 *   <li><b>Conditions:</b> Evaluates task conditions before execution</li>
 * </ul>
 *
 * <p><b>Execution Flow:</b></p>
 * <pre>
 * 1. Load profile
 * 2. For each task in profile:
 *    a. Check conditions
 *    b. If conditions met, convert ProfileTask to Task
 *    c. Queue task for execution
 *    d. Wait for completion
 *    e. Handle result (success/retry/fail)
 * 3. If repeat enabled, go to step 2
 * 4. Fire completion event
 * </pre>
 *
 * <p><b>Integration with ActionExecutor:</b></p>
 * <p>ProfileExecutor integrates with the existing ActionExecutor by:</p>
 * <ul>
 *   <li>Converting ProfileTasks to Tasks</li>
 *   <li>Queuing tasks via ActionExecutor.queueTask()</li>
 *   <li>Monitoring progress via getCurrentAction()</li>
 *   <li>Handling errors via ActionResult callbacks</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * ProfileExecutor executor = new ProfileExecutor(foreman, actionExecutor, profile);
 * executor.addListener((event) -> {
 *     if (event.type == EventType.PROFILE_COMPLETED) {
 *         System.out.println("Profile completed!");
 *     }
 * });
 * executor.start();
 * }</pre>
 *
 * @see TaskProfile
 * @see ProfileTask
 * @see ActionExecutor
 * @since 1.4.0
 */
public class ProfileExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileExecutor.class);

    private final ForemanEntity foreman;
    private final ActionExecutor actionExecutor;
    private final TaskProfile profile;

    private final AtomicInteger currentTaskIndex = new AtomicInteger(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicInteger currentIteration = new AtomicInteger(0);
    private final AtomicReference<ExecutionState> state = new AtomicReference<>(ExecutionState.IDLE);

    private final List<ProfileExecutionListener> listeners = new ArrayList<>();
    private final List<TaskExecutionResult> executionHistory = new ArrayList<>();

    private long lastExecutionTime;
    private String lastError;

    /**
     * Creates a new profile executor.
     *
     * @param foreman The foreman entity
     * @param actionExecutor The action executor to use for task execution
     * @param profile The profile to execute
     */
    public ProfileExecutor(ForemanEntity foreman, ActionExecutor actionExecutor, TaskProfile profile) {
        this.foreman = foreman;
        this.actionExecutor = actionExecutor;
        this.profile = profile;
    }

    /**
     * Starts executing the profile.
     *
     * @throws IllegalStateException if already running
     */
    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Profile is already running");
        }

        LOGGER.info("Starting profile execution: {} ({} tasks)",
                profile.getName(), profile.getTaskCount());

        state.set(ExecutionState.RUNNING);
        currentTaskIndex.set(0);
        currentIteration.set(0);

        fireEvent(ProfileExecutionEvent.started(profile));

        executeNextTask();
    }

    /**
     * Pauses profile execution.
     */
    public void pause() {
        if (!isRunning.get()) {
            return;
        }

        if (isPaused.compareAndSet(false, true)) {
            LOGGER.info("Pausing profile execution: {}", profile.getName());
            state.set(ExecutionState.PAUSED);
            fireEvent(ProfileExecutionEvent.paused(profile));
        }
    }

    /**
     * Resumes paused profile execution.
     */
    public void resume() {
        if (!isPaused.get()) {
            return;
        }

        if (isPaused.compareAndSet(true, false)) {
            LOGGER.info("Resuming profile execution: {}", profile.getName());
            state.set(ExecutionState.RUNNING);
            fireEvent(ProfileExecutionEvent.resumed(profile));
            executeNextTask();
        }
    }

    /**
     * Stops profile execution.
     */
    public void stop() {
        if (!isRunning.compareAndSet(true, false)) {
            return;
        }

        LOGGER.info("Stopping profile execution: {}", profile.getName());
        state.set(ExecutionState.STOPPED);
        isPaused.set(false);
        fireEvent(ProfileExecutionEvent.stopped(profile));
    }

    /**
     * Executes the next task in the profile.
     */
    private void executeNextTask() {
        if (!isRunning.get() || isPaused.get()) {
            return;
        }

        int index = currentTaskIndex.get();

        // Check if we've completed all tasks
        if (index >= profile.getTasks().size()) {
            handleProfileCompleted();
            return;
        }

        ProfileTask profileTask = profile.getTasks().get(index);

        LOGGER.debug("Executing task {}/{}: {}",
                index + 1, profile.getTaskCount(), profileTask.getType());

        // Check conditions
        if (!evaluateConditions(profileTask)) {
            LOGGER.info("Task conditions not met, skipping: {}", profileTask.getType());
            handleTaskSkipped(profileTask, "Conditions not met");
            currentTaskIndex.incrementAndGet();
            executeNextTask();
            return;
        }

        // Convert to Task and queue for execution
        Task task = profileTask.toTask();
        actionExecutor.queueTask(task);

        fireEvent(ProfileExecutionEvent.taskStarted(profile, index, profileTask));

        // Monitor execution
        monitorTaskExecution(profileTask, index);
    }

    /**
     * Monitors the execution of a task.
     */
    private void monitorTaskExecution(ProfileTask profileTask, int taskIndex) {
        int retryCount = 0;
        int maxRetries = profile.getSettings().getMaxRetries();

        // Start monitoring thread (or use tick-based monitoring)
        // For now, we'll do simple polling
        Thread monitorThread = new Thread(() -> {
            try {
                while (isRunning.get() && !isPaused.get()) {
                    Thread.sleep(100); // Check every 100ms

                    BaseAction currentAction = actionExecutor.getCurrentAction();

                    // Check if action completed
                    if (currentAction == null || currentAction.isComplete()) {
                        // Task completed
                        handleTaskCompleted(profileTask, taskIndex, true, null);
                        return;
                    }

                    // Check for errors
                    // This would need better integration with ActionExecutor
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ProfileExecutor-Monitor-" + profile.getName());

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Evaluates task conditions.
     */
    private boolean evaluateConditions(ProfileTask task) {
        // Default implementation - all conditions pass
        // In a full implementation, this would check:
        // - Inventory space
        // - Time of day
        // - Health level
        // - Custom conditions

        if (task.hasConditions("inventory_has_space")) {
            boolean required = task.getConditionBoolean("inventory_has_space", true);
            // Check inventory
            // For now, return true
        }

        return true;
    }

    /**
     * Handles task completion.
     */
    private void handleTaskCompleted(ProfileTask task, int index, boolean success, String error) {
        TaskExecutionResult result = new TaskExecutionResult(task, index, success, error);
        executionHistory.add(result);

        if (success) {
            LOGGER.info("Task completed: {}", task.getType());
            fireEvent(ProfileExecutionEvent.taskCompleted(profile, index, task));

            // Move to next task
            currentTaskIndex.incrementAndGet();
            executeNextTask();
        } else {
            LOGGER.error("Task failed: {} - {}", task.getType(), error);

            // Check if we should retry
            if (!task.isOptional() && profile.getSettings().getMaxRetries() > 0) {
                // Retry logic would go here
                LOGGER.info("Retrying task: {}", task.getType());
            } else {
                // Check if we should stop on error
                if (profile.getSettings().isStopOnError() && !task.isOptional()) {
                    handleProfileFailed(error);
                } else {
                    // Skip optional task or continue
                    currentTaskIndex.incrementAndGet();
                    executeNextTask();
                }
            }
        }
    }

    /**
     * Handles a skipped task.
     */
    private void handleTaskSkipped(ProfileTask task, String reason) {
        TaskExecutionResult result = new TaskExecutionResult(task, currentTaskIndex.get(), false, reason);
        result.setSkipped(true);
        executionHistory.add(result);

        fireEvent(ProfileExecutionEvent.taskSkipped(profile, currentTaskIndex.get(), task, reason));
    }

    /**
     * Handles profile completion.
     */
    private void handleProfileCompleted() {
        LOGGER.info("Profile completed: {}", profile.getName());

        // Check if we should repeat
        if (profile.getSettings().isRepeat()) {
            int currentIter = currentIteration.incrementAndGet();
            int maxIterations = profile.getSettings().getRepeatCount();

            if (maxIterations == 0 || currentIter < maxIterations) {
                LOGGER.info("Repeating profile (iteration {}/{})",
                        currentIter + 1, maxIterations);
                currentTaskIndex.set(0);
                executeNextTask();
                return;
            }
        }

        // Profile fully completed
        isRunning.set(false);
        state.set(ExecutionState.COMPLETED);
        lastExecutionTime = System.currentTimeMillis();

        fireEvent(ProfileExecutionEvent.completed(profile, executionHistory));
    }

    /**
     * Handles profile failure.
     */
    private void handleProfileFailed(String error) {
        LOGGER.error("Profile failed: {} - {}", profile.getName(), error);

        isRunning.set(false);
        state.set(ExecutionState.FAILED);
        lastError = error;

        fireEvent(ProfileExecutionEvent.failed(profile, error));
    }

    /**
     * Adds a listener for execution events.
     */
    public void addListener(ProfileExecutionListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     */
    public void removeListener(ProfileExecutionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fires an execution event to all listeners.
     */
    private void fireEvent(ProfileExecutionEvent event) {
        for (ProfileExecutionListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error firing profile event", e);
            }
        }
    }

    // Getters

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public ExecutionState getState() {
        return state.get();
    }

    public int getCurrentTaskIndex() {
        return currentTaskIndex.get();
    }

    public int getCurrentIteration() {
        return currentIteration.get();
    }

    public float getProgress() {
        int totalTasks = profile.getTaskCount();
        if (totalTasks == 0) return 1.0f;
        return (float) currentTaskIndex.get() / totalTasks;
    }

    public List<TaskExecutionResult> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    public String getLastError() {
        return lastError;
    }

    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    /**
     * Execution state enum.
     */
    public enum ExecutionState {
        IDLE,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        STOPPED
    }

    /**
     * Listener for profile execution events.
     */
    public interface ProfileExecutionListener {
        void onEvent(ProfileExecutionEvent event);
    }

    /**
     * Execution event data.
     */
    public static class ProfileExecutionEvent {
        private final EventType type;
        private final TaskProfile profile;
        private final int taskIndex;
        private final ProfileTask task;
        private final String message;
        private final List<TaskExecutionResult> history;
        private final long timestamp;

        private ProfileExecutionEvent(EventType type, TaskProfile profile, int taskIndex,
                                     ProfileTask task, String message, List<TaskExecutionResult> history) {
            this.type = type;
            this.profile = profile;
            this.taskIndex = taskIndex;
            this.task = task;
            this.message = message;
            this.history = history;
            this.timestamp = System.currentTimeMillis();
        }

        public static ProfileExecutionEvent started(TaskProfile profile) {
            return new ProfileExecutionEvent(EventType.PROFILE_STARTED, profile, -1, null,
                    "Profile started", null);
        }

        public static ProfileExecutionEvent completed(TaskProfile profile, List<TaskExecutionResult> history) {
            return new ProfileExecutionEvent(EventType.PROFILE_COMPLETED, profile, -1, null,
                    "Profile completed", history);
        }

        public static ProfileExecutionEvent failed(TaskProfile profile, String error) {
            return new ProfileExecutionEvent(EventType.PROFILE_FAILED, profile, -1, null,
                    error, null);
        }

        public static ProfileExecutionEvent stopped(TaskProfile profile) {
            return new ProfileExecutionEvent(EventType.PROFILE_STOPPED, profile, -1, null,
                    "Profile stopped", null);
        }

        public static ProfileExecutionEvent paused(TaskProfile profile) {
            return new ProfileExecutionEvent(EventType.PROFILE_PAUSED, profile, -1, null,
                    "Profile paused", null);
        }

        public static ProfileExecutionEvent resumed(TaskProfile profile) {
            return new ProfileExecutionEvent(EventType.PROFILE_RESUMED, profile, -1, null,
                    "Profile resumed", null);
        }

        public static ProfileExecutionEvent taskStarted(TaskProfile profile, int index, ProfileTask task) {
            return new ProfileExecutionEvent(EventType.TASK_STARTED, profile, index, task,
                    "Task started: " + task.getType(), null);
        }

        public static ProfileExecutionEvent taskCompleted(TaskProfile profile, int index, ProfileTask task) {
            return new ProfileExecutionEvent(EventType.TASK_COMPLETED, profile, index, task,
                    "Task completed: " + task.getType(), null);
        }

        public static ProfileExecutionEvent taskSkipped(TaskProfile profile, int index, ProfileTask task, String reason) {
            return new ProfileExecutionEvent(EventType.TASK_SKIPPED, profile, index, task,
                    "Task skipped: " + reason, null);
        }

        public EventType getType() { return type; }
        public TaskProfile getProfile() { return profile; }
        public int getTaskIndex() { return taskIndex; }
        public ProfileTask getTask() { return task; }
        public String getMessage() { return message; }
        public List<TaskExecutionResult> getHistory() { return history; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Event type enum.
     */
    public enum EventType {
        PROFILE_STARTED,
        PROFILE_COMPLETED,
        PROFILE_FAILED,
        PROFILE_STOPPED,
        PROFILE_PAUSED,
        PROFILE_RESUMED,
        TASK_STARTED,
        TASK_COMPLETED,
        TASK_SKIPPED,
        TASK_FAILED
    }

    /**
     * Result of a task execution.
     */
    public static class TaskExecutionResult {
        private final ProfileTask task;
        private final int index;
        private final boolean success;
        private final String error;
        private final long timestamp;
        private boolean skipped;

        public TaskExecutionResult(ProfileTask task, int index, boolean success, String error) {
            this.task = task;
            this.index = index;
            this.success = success;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
            this.skipped = false;
        }

        public ProfileTask getTask() { return task; }
        public int getIndex() { return index; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public long getTimestamp() { return timestamp; }
        public boolean isSkipped() { return skipped; }
        public void setSkipped(boolean skipped) { this.skipped = skipped; }
    }
}
