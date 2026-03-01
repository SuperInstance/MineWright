package com.minewright.skill;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Records successful action sequences for skill extraction.
 *
 * <p><b>Purpose:</b></p>
 * <p>ExecutionTracker captures action sequences as they execute, storing
 * them as ExecutionSequence objects for later analysis by PatternExtractor.
 * This is the foundation of the Voyager pattern's skill learning system.</p>
 *
 * <p><b>Tracking Flow:</b></p>
 * <ol>
 *   <li>Agent receives a goal (e.g., "Build a shelter")</li>
 *   <li>Tracker starts recording with startTracking()</li>
 *   <li>Each action execution is recorded via recordAction()</li>
 *   <li>When goal completes, endTracking() finalizes the sequence</li>
 *   <li>Successful sequences are stored for pattern extraction</li>
 * </ol>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>All operations are thread-safe for concurrent access from multiple agents.
 * Uses ConcurrentHashMap and CopyOnWriteArrayList for concurrent collections.</p>
 *
 * <p><b>Integration Point:</b></p>
 * <p>This class integrates with ActionExecutor to automatically track all
 * task executions without manual intervention.</p>
 *
 * @see ExecutionSequence
 * @see ActionRecord
 * @see PatternExtractor
 * @since 1.0.0
 */
public class ExecutionTracker {
    private static final Logger LOGGER = TestLogger.getLogger(ExecutionTracker.class);

    /**
     * Singleton instance for global access across all agents.
     */
    private static volatile ExecutionTracker instance;

    /**
     * Active tracking sessions by agent ID.
     * Maps agent ID to their current sequence builder.
     */
    private final Map<String, ExecutionSequence.Builder> activeSessions;

    /**
     * Completed sequences awaiting pattern extraction.
     * Only successful sequences are stored for learning.
     */
    private final List<ExecutionSequence> completedSequences;

    /**
     * Maximum number of sequences to keep in memory.
     * Older sequences are discarded when limit is exceeded.
     */
    private static final int MAX_SEQUENCES = 1000;

    private ExecutionTracker() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.completedSequences = new CopyOnWriteArrayList<>();
        LOGGER.info("ExecutionTracker initialized");
    }

    /**
     * Gets the singleton ExecutionTracker instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return Global ExecutionTracker instance
     */
    public static ExecutionTracker getInstance() {
        if (instance == null) {
            synchronized (ExecutionTracker.class) {
                if (instance == null) {
                    instance = new ExecutionTracker();
                }
            }
        }
        return instance;
    }

    /**
     * Starts tracking a new execution sequence for an agent.
     *
     * <p><b>Usage:</b></p>
     * <pre>
     * tracker.startTracking(agentId, "Build a shelter");
     * </pre>
     *
     * @param agentId The agent to track
     * @param goal    The goal being pursued
     */
    public void startTracking(String agentId, String goal) {
        if (activeSessions.containsKey(agentId)) {
            LOGGER.warn("Agent '{}' already has an active tracking session. Ending previous session.", agentId);
            endTracking(agentId, false);
        }

        ExecutionSequence.Builder builder = ExecutionSequence.builder(agentId, goal);
        activeSessions.put(agentId, builder);

        LOGGER.debug("Started tracking for agent '{}' with goal: {}", agentId, goal);
    }

    /**
     * Records an action execution for the agent's current sequence.
     *
     * <p><b>Usage:</b></p>
     * <pre>
     * tracker.recordAction(agentId, action, result);
     * </pre>
     *
     * @param agentId The agent executing the action
     * @param action  The action being executed
     * @param result  The result of the action
     */
    public void recordAction(String agentId, BaseAction action, ActionResult result) {
        ExecutionSequence.Builder builder = activeSessions.get(agentId);
        if (builder == null) {
            LOGGER.debug("No active tracking session for agent '{}'. Action not recorded.", agentId);
            return;
        }

        Task task = action.getTask();
        long executionTime = result.getAgeMs() > 0 ? result.getAgeMs() : 0;

        ActionRecord record;
        if (result.isSuccess()) {
            record = ActionRecord.success(
                task.getAction(),
                task.getParameters(),
                executionTime
            );
        } else {
            record = ActionRecord.failure(
                task.getAction(),
                task.getParameters(),
                executionTime,
                result.getMessage()
            );
        }

        builder.addAction(record);

        LOGGER.debug("Recorded action for agent '{}': {} (success: {})",
            agentId, task.getAction(), result.isSuccess());
    }

    /**
     * Records an action with explicit timing information.
     *
     * @param agentId       The agent executing the action
     * @param actionType    The type of action
     * @param parameters    The action parameters
     * @param executionTime Time taken in milliseconds
     * @param success       Whether the action succeeded
     * @param errorMessage  Error message if failed
     */
    public void recordAction(String agentId, String actionType, Map<String, Object> parameters,
                            long executionTime, boolean success, String errorMessage) {
        ExecutionSequence.Builder builder = activeSessions.get(agentId);
        if (builder == null) {
            LOGGER.debug("No active tracking session for agent '{}'. Action not recorded.", agentId);
            return;
        }

        ActionRecord record;
        if (success) {
            record = ActionRecord.success(actionType, parameters, executionTime);
        } else {
            record = ActionRecord.failure(actionType, parameters, executionTime, errorMessage);
        }

        builder.addAction(record);
    }

    /**
     * Adds contextual information to the current tracking session.
     *
     * @param agentId The agent to add context for
     * @param key     Context key
     * @param value   Context value
     */
    public void addContext(String agentId, String key, Object value) {
        ExecutionSequence.Builder builder = activeSessions.get(agentId);
        if (builder == null) {
            LOGGER.debug("No active tracking session for agent '{}'. Context not added.", agentId);
            return;
        }

        builder.addContext(key, value);
        LOGGER.debug("Added context for agent '{}': {} = {}", agentId, key, value);
    }

    /**
     * Ends tracking for an agent and finalizes the sequence.
     *
     * <p><b>Storage Policy:</b></p>
     * <ul>
     *   <li>Successful sequences are stored for pattern extraction</li>
     *   <li>Failed sequences are logged but discarded</li>
     *   <li>Older sequences are discarded when MAX_SEQUENCES is exceeded</li>
     * </ul>
     *
     * @param agentId  The agent to stop tracking
     * @param success Whether the overall sequence was successful
     * @return The completed sequence, or null if no active session
     */
    public ExecutionSequence endTracking(String agentId, boolean success) {
        ExecutionSequence.Builder builder = activeSessions.remove(agentId);
        if (builder == null) {
            LOGGER.debug("No active tracking session for agent '{}'", agentId);
            return null;
        }

        ExecutionSequence sequence = builder.build(success);

        if (success) {
            storeSequence(sequence);
            LOGGER.info("Completed tracking for agent '{}': {} (successful, {} actions, {}ms)",
                agentId, sequence.getGoal(), sequence.getActionCount(), sequence.getTotalExecutionTime());
        } else {
            LOGGER.info("Completed tracking for agent '{}': {} (failed, {} actions)",
                agentId, sequence.getGoal(), sequence.getActionCount());
        }

        return sequence;
    }

    /**
     * Stores a sequence in the completed list, enforcing max limit.
     *
     * @param sequence The sequence to store
     */
    private void storeSequence(ExecutionSequence sequence) {
        completedSequences.add(sequence);

        // Enforce max limit by removing oldest sequences
        while (completedSequences.size() > MAX_SEQUENCES) {
            ExecutionSequence removed = completedSequences.remove(0);
            LOGGER.debug("Removed old sequence: {}", removed.getId());
        }
    }

    /**
     * Gets all completed sequences available for pattern extraction.
     *
     * @return List of completed sequences
     */
    public List<ExecutionSequence> getSequences() {
        return List.copyOf(completedSequences);
    }

    /**
     * Gets only successful sequences for pattern extraction.
     *
     * @return List of successful sequences
     */
    public List<ExecutionSequence> getSuccessfulSequences() {
        return completedSequences.stream()
            .filter(ExecutionSequence::isSuccessful)
            .collect(Collectors.toList());
    }

    /**
     * Gets sequences by goal description.
     *
     * @param goal The goal to filter by (partial match)
     * @return List of matching sequences
     */
    public List<ExecutionSequence> getSequencesByGoal(String goal) {
        return completedSequences.stream()
            .filter(seq -> seq.getGoal().toLowerCase().contains(goal.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Gets sequences by agent ID.
     *
     * @param agentId The agent ID to filter by
     * @return List of matching sequences
     */
    public List<ExecutionSequence> getSequencesByAgent(String agentId) {
        return completedSequences.stream()
            .filter(seq -> seq.getAgentId().equals(agentId))
            .collect(Collectors.toList());
    }

    /**
     * Clears all stored sequences.
     * Useful for testing or memory management.
     */
    public void clear() {
        completedSequences.clear();
        activeSessions.clear();
        LOGGER.info("Cleared all sequences and active sessions");
    }

    /**
     * Clears only completed sequences, keeping active sessions.
     */
    public void clearSequences() {
        completedSequences.clear();
        LOGGER.info("Cleared all completed sequences");
    }

    /**
     * Gets statistics about tracked sequences.
     *
     * @return Statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalSequences", completedSequences.size());
        stats.put("successfulSequences", getSuccessfulSequences().size());
        stats.put("activeSessions", activeSessions.size());

        long totalActions = completedSequences.stream()
            .mapToLong(ExecutionSequence::getActionCount)
            .sum();
        stats.put("totalActions", totalActions);

        long averageActions = completedSequences.isEmpty() ? 0 :
            totalActions / completedSequences.size();
        stats.put("averageActionsPerSequence", averageActions);

        return stats;
    }

    /**
     * Gets the count of stored sequences.
     *
     * @return Number of sequences
     */
    public int getSequenceCount() {
        return completedSequences.size();
    }

    /**
     * Checks if an agent has an active tracking session.
     *
     * @param agentId The agent ID to check
     * @return true if agent is being tracked
     */
    public boolean isTracking(String agentId) {
        return activeSessions.containsKey(agentId);
    }

    /**
     * Cancels tracking for an agent without storing the sequence.
     *
     * @param agentId The agent to cancel tracking for
     */
    public void cancelTracking(String agentId) {
        ExecutionSequence.Builder builder = activeSessions.remove(agentId);
        if (builder != null) {
            LOGGER.info("Cancelled tracking for agent '{}'", agentId);
        }
    }
}
