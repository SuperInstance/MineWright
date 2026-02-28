package com.minewright.blackboard;

import com.minewright.testutil.TestLogger;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Knowledge source for posting task results to the blackboard.
 *
 * <p><b>Purpose:</b></p>
 * <p>TaskResultSource posts task execution outcomes to the {@link KnowledgeArea#TASKS}
 * area. This enables agents to track task progress, learn from failures, and
 * coordinate work distribution without central coordination.</p>
 *
 * <p><b>Posted Information:</b></p>
 * <ul>
 *   <li><b>Task Started:</b> When a task begins execution</li>
 *   <li><b>Task Progress:</b> Periodic progress updates</li>
 *   <li><b>Task Completed:</b> Successful completion with result</li>
 *   <li><b>Task Failed:</b> Failure with reason and replanning flag</li>
 *   <li><b>Task Cancelled:</b> Cancellation before completion</li>
 * </ul>
 *
 * <p><b>Update Strategy:</b></p>
 * <p>Task events are posted immediately when they occur. Progress updates are
 * throttled to avoid flooding the blackboard during long-running tasks.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * TaskResultSource source = new TaskResultSource(agentUUID);
 *
 * // Task started
 * source.postTaskStarted(task);
 *
 * // Task completed
 * source.postTaskCompleted(task, ActionResult.success("Built structure"));
 *
 * // Task failed
 * source.postTaskFailed(task, ActionResult.failure("No materials available"));
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Task events can be posted from any thread.</p>
 *
 * @see KnowledgeArea
 * @see Blackboard
 * @see Task
 * @since 1.0.0
 */
public class TaskResultSource {
    private static final Logger LOGGER = TestLogger.getLogger(TaskResultSource.class);

    /**
     * UUID of the agent this source is reporting for.
     */
    private final UUID agentUUID;

    /**
     * The blackboard for posting task results.
     */
    private final Blackboard blackboard;

    /**
     * Counter for results posted.
     */
    private int resultsPosted = 0;

    /**
     * Timestamp of last progress update for throttling.
     */
    private long lastProgressUpdate = 0;

    /**
     * Minimum milliseconds between progress updates.
     */
    private static final long PROGRESS_UPDATE_INTERVAL = 1000; // 1 second

    /**
     * Creates a new task result source.
     *
     * @param agentUUID UUID of the agent to report for
     */
    public TaskResultSource(UUID agentUUID) {
        this(agentUUID, Blackboard.getInstance());
    }

    /**
     * Creates a new task result source with a specific blackboard.
     *
     * @param agentUUID UUID of the agent to report for
     * @param blackboard The blackboard for posting results
     */
    public TaskResultSource(UUID agentUUID, Blackboard blackboard) {
        this.agentUUID = agentUUID;
        this.blackboard = blackboard;
    }

    /**
     * Posts a task started event to the blackboard.
     *
     * <p>Notifies other agents that a task has begun execution.
     * This helps with coordination and prevents duplicate work.</p>
     *
     * @param task The task being started
     */
    public void postTaskStarted(Task task) {
        if (task == null) {
            return;
        }

        String key = generateTaskKey(task, "started");
        TaskEvent event = new TaskEvent(
            task.getAction(),
            "STARTED",
            task.toString(),
            System.currentTimeMillis()
        );

        BlackboardEntry<TaskEvent> entry = BlackboardEntry.createFact(key, event, agentUUID);
        blackboard.post(KnowledgeArea.TASKS, entry);

        resultsPosted++;
        LOGGER.debug("Task started posted to blackboard: {}", task.getAction());
    }

    /**
     * Posts a task completed event to the blackboard.
     *
     * <p>Notifies other agents that a task has completed successfully.
     * The result message can contain useful information for other agents.</p>
     *
     * @param task The completed task
     * @param result Action result with outcome details
     */
    public void postTaskCompleted(Task task, ActionResult result) {
        if (task == null || result == null) {
            return;
        }

        String key = generateTaskKey(task, "completed");
        TaskEvent event = new TaskEvent(
            task.getAction(),
            "COMPLETED",
            result.getMessage(),
            System.currentTimeMillis()
        );

        BlackboardEntry<TaskEvent> entry = BlackboardEntry.createFact(key, event, agentUUID);
        blackboard.post(KnowledgeArea.TASKS, entry);

        resultsPosted++;
        LOGGER.debug("Task completed posted to blackboard: {} - {}",
            task.getAction(), result.getMessage());
    }

    /**
     * Posts a task failed event to the blackboard.
     *
     * <p>Notifies other agents that a task has failed. Other agents can use
     * this information to avoid repeating the same failure, or to retry
     * with different parameters.</p>
     *
     * @param task The failed task
     * @param result Action result with failure details
     */
    public void postTaskFailed(Task task, ActionResult result) {
        if (task == null || result == null) {
            return;
        }

        String key = generateTaskKey(task, "failed");
        TaskEvent event = new TaskEvent(
            task.getAction(),
            "FAILED",
            result.getMessage(),
            System.currentTimeMillis()
        );

        // Post as FACT but with slightly lower confidence for failures
        BlackboardEntry<TaskEvent> entry = new BlackboardEntry<>(
            key,
            event,
            agentUUID,
            0.8,
            BlackboardEntry.EntryType.FACT
        );

        blackboard.post(KnowledgeArea.TASKS, entry);

        resultsPosted++;
        LOGGER.debug("Task failed posted to blackboard: {} - {}",
            task.getAction(), result.getMessage());

        // If replanning is required, post a HYPOTHESIS for alternative approaches
        if (result.requiresReplanning()) {
            postReplanningSuggestion(task, result);
        }
    }

    /**
     * Posts a task cancelled event to the blackboard.
     *
     * <p>Notifies other agents that a task was cancelled before completion.
     * This is different from failure - it was intentionally stopped.</p>
     *
     * @param task The cancelled task
     * @param reason Reason for cancellation
     */
    public void postTaskCancelled(Task task, String reason) {
        if (task == null) {
            return;
        }

        String key = generateTaskKey(task, "cancelled");
        TaskEvent event = new TaskEvent(
            task.getAction(),
            "CANCELLED",
            reason != null ? reason : "No reason provided",
            System.currentTimeMillis()
        );

        BlackboardEntry<TaskEvent> entry = BlackboardEntry.createFact(key, event, agentUUID);
        blackboard.post(KnowledgeArea.TASKS, entry);

        resultsPosted++;
        LOGGER.debug("Task cancelled posted to blackboard: {} - {}",
            task.getAction(), reason);
    }

    /**
     * Posts a task progress update to the blackboard.
     *
     * <p>Progress updates are throttled to avoid flooding the blackboard
     * during long-running tasks. Updates are only posted if at least
     * {@link #PROGRESS_UPDATE_INTERVAL} milliseconds have passed since the
     * last update.</p>
     *
     * @param task The task in progress
     * @param progress Progress percentage (0-100)
     * @param message Optional progress message
     * @return true if the update was posted, false if throttled
     */
    public boolean postTaskProgress(Task task, int progress, String message) {
        if (task == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastProgressUpdate < PROGRESS_UPDATE_INTERVAL) {
            return false; // Throttled
        }

        String key = generateTaskKey(task, "progress");
        TaskProgressEvent event = new TaskProgressEvent(
            task.getAction(),
            progress,
            message,
            now
        );

        BlackboardEntry<TaskProgressEvent> entry = new BlackboardEntry<>(
            key,
            event,
            agentUUID,
            0.9,
            BlackboardEntry.EntryType.FACT
        );

        blackboard.post(KnowledgeArea.TASKS, entry);

        lastProgressUpdate = now;
        resultsPosted++;

        return true;
    }

    /**
     * Posts a replanning suggestion when a task fails.
     *
     * <p>This creates a HYPOTHESIS entry that other agents can use to
     * suggest alternative approaches or avoid similar failures.</p>
     *
     * @param task The failed task
     * @param result The failure result
     */
    private void postReplanningSuggestion(Task task, ActionResult result) {
        String key = "replan_" + task.getAction() + "_" + System.currentTimeMillis();
        String suggestion = String.format(
            "Task '%s' failed: %s. Consider alternative approaches or prerequisites.",
            task.getAction(),
            result.getMessage()
        );

        BlackboardEntry<String> entry = BlackboardEntry.createHypothesis(
            key,
            suggestion,
            agentUUID,
            0.6 // Moderate confidence for suggestions
        );

        blackboard.post(KnowledgeArea.TASKS, entry);

        LOGGER.debug("Replanning suggestion posted: {}", suggestion);
    }

    /**
     * Posts a dependency observation to the blackboard.
     *
     * <p>Other agents can use dependency information to plan tasks in the
     * correct order (e.g., gather materials before building).</p>
     *
     * @param task The task with dependencies
     * @param dependency Description of what this task depends on
     */
    public void postTaskDependency(Task task, String dependency) {
        if (task == null || dependency == null) {
            return;
        }

        String key = "dependency_" + task.getAction();
        DependencyEvent event = new DependencyEvent(
            task.getAction(),
            dependency,
            System.currentTimeMillis()
        );

        BlackboardEntry<DependencyEvent> entry = new BlackboardEntry<>(
            key,
            event,
            agentUUID,
            0.8,
            BlackboardEntry.EntryType.FACT
        );

        blackboard.post(KnowledgeArea.TASKS, entry);

        resultsPosted++;
        LOGGER.debug("Task dependency posted: {} depends on {}",
            task.getAction(), dependency);
    }

    /**
     * Posts a resource requirement to the blackboard.
     *
     * <p>Helps with resource planning and allocation across agents.</p>
     *
     * @param task The task requiring resources
     * @param resource Description of required resources
     * @param quantity Amount needed
     */
    public void postResourceRequirement(Task task, String resource, int quantity) {
        if (task == null || resource == null) {
            return;
        }

        String key = "resource_" + resource.replace(" ", "_");
        ResourceRequirementEvent event = new ResourceRequirementEvent(
            task.getAction(),
            resource,
            quantity,
            System.currentTimeMillis()
        );

        BlackboardEntry<ResourceRequirementEvent> entry = BlackboardEntry.createFact(
            key,
            event,
            agentUUID
        );

        blackboard.post(KnowledgeArea.RESOURCES, entry);

        resultsPosted++;
        LOGGER.debug("Resource requirement posted: {} needs {} {}",
            task.getAction(), quantity, resource);
    }

    /**
     * Generates a unique key for a task event.
     *
     * @param task The task
     * @param eventType Type of event (started, completed, failed, etc.)
     * @return Unique key string
     */
    private String generateTaskKey(Task task, String eventType) {
        String agentPart = agentUUID.toString().substring(0, 8);
        String taskPart = task.getAction().replace(" ", "_").toLowerCase();
        return String.format("%s_%s_%s_%d", agentPart, taskPart, eventType, System.currentTimeMillis());
    }

    /**
     * Gets the number of results posted by this source.
     *
     * @return Result count
     */
    public int getResultsPosted() {
        return resultsPosted;
    }

    /**
     * Data class for task events.
     */
    public record TaskEvent(
        String action,
        String status,
        String details,
        long timestamp
    ) {
        @Override
        public String toString() {
            return String.format("%s - %s: %s", action, status, details);
        }
    }

    /**
     * Data class for task progress events.
     */
    public record TaskProgressEvent(
        String action,
        int progress,
        String message,
        long timestamp
    ) {
        @Override
        public String toString() {
            return String.format("%s - %d%%: %s", action, progress, message);
        }
    }

    /**
     * Data class for dependency events.
     */
    public record DependencyEvent(
        String task,
        String dependency,
        long timestamp
    ) {
        @Override
        public String toString() {
            return String.format("%s depends on %s", task, dependency);
        }
    }

    /**
     * Data class for resource requirement events.
     */
    public record ResourceRequirementEvent(
        String task,
        String resource,
        int quantity,
        long timestamp
    ) {
        @Override
        public String toString() {
            return String.format("%s needs %d of %s", task, quantity, resource);
        }
    }
}
