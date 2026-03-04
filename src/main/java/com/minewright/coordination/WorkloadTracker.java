package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Tracks workload and task execution statistics for agents.
 *
 * <p><b>Workload Dimensions:</b></p>
 * <ul>
 *   <li><b>Active Tasks</b> - Number of tasks currently being executed</li>
 *   <li><b>Load Factor</b> - Computed load (0.0-1.0) based on active tasks and capacity</li>
 *   <li><b>Completed Tasks</b> - Total tasks completed by agent</li>
 *   <li><b>Average Duration</b> - Average time to complete tasks</li>
 *   <li><b>Success Rate</b> - Percentage of successful task completions</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses ConcurrentHashMap for thread-safe storage</li>
 *   <li>Atomic counters for statistics</li>
 *   <li>Safe for concurrent access from multiple agents</li>
 * </ul>
 *
 * @see AgentCapability
 * @see TaskBid
 * @since 1.3.0
 */
public class WorkloadTracker {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(WorkloadTracker.class);

    /**
     * Listener for workload change events.
     */
    public interface WorkloadListener {
        /**
         * Called when an agent's load changes.
         */
        default void onLoadChanged(UUID agentId, double oldLoad, double newLoad) {}

        /**
         * Called when a task is assigned to an agent.
         */
        default void onTaskAssigned(UUID agentId, String taskId) {}

        /**
         * Called when a task is completed by an agent.
         */
        default void onTaskCompleted(UUID agentId, String taskId, boolean success, long duration) {}
    }

    /**
     * Workload statistics for a single agent.
     */
    public static class AgentWorkload {
        private final UUID agentId;
        private final Map<String, Long> activeTasks; // task ID -> start time
        private final AtomicInteger totalCompleted;
        private final AtomicInteger totalFailed;
        private final AtomicLong totalCompletionTime;
        private final int maxConcurrentTasks;
        private volatile double currentLoad;

        public AgentWorkload(UUID agentId, int maxConcurrentTasks) {
            this.agentId = agentId;
            this.activeTasks = new ConcurrentHashMap<>();
            this.totalCompleted = new AtomicInteger(0);
            this.totalFailed = new AtomicInteger(0);
            this.totalCompletionTime = new AtomicLong(0);
            this.maxConcurrentTasks = maxConcurrentTasks;
            this.currentLoad = 0.0;
        }

        public UUID getAgentId() {
            return agentId;
        }

        public Map<String, Long> getActiveTasks() {
            return Collections.unmodifiableMap(activeTasks);
        }

        public int getActiveTaskCount() {
            return activeTasks.size();
        }

        public int getTotalCompleted() {
            return totalCompleted.get();
        }

        public int getTotalFailed() {
            return totalFailed.get();
        }

        public int getTotalTasks() {
            return totalCompleted.get() + totalFailed.get();
        }

        public long getTotalCompletionTime() {
            return totalCompletionTime.get();
        }

        public double getAverageCompletionTime() {
            int completed = totalCompleted.get();
            return completed > 0 ? (double) totalCompletionTime.get() / completed : 0.0;
        }

        public double getSuccessRate() {
            int total = totalCompleted.get() + totalFailed.get();
            return total > 0 ? (double) totalCompleted.get() / total : 0.5;
        }

        public int getMaxConcurrentTasks() {
            return maxConcurrentTasks;
        }

        public double getCurrentLoad() {
            return currentLoad;
        }

        /**
         * Assigns a task to this agent.
         *
         * @param taskId Task identifier
         * @return true if task was assigned, false if at capacity
         */
        public boolean assignTask(String taskId) {
            if (activeTasks.size() >= maxConcurrentTasks) {
                return false;
            }

            activeTasks.put(taskId, System.currentTimeMillis());
            updateLoad();
            return true;
        }

        /**
         * Completes a task for this agent.
         *
         * @param taskId Task identifier
         * @param success Whether the task succeeded
         * @return true if task was found and completed, false otherwise
         */
        public boolean completeTask(String taskId, boolean success) {
            Long startTime = activeTasks.remove(taskId);
            if (startTime == null) {
                return false;
            }

            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                totalCompleted.incrementAndGet();
                totalCompletionTime.addAndGet(duration);
            } else {
                totalFailed.incrementAndGet();
            }

            updateLoad();
            return true;
        }

        /**
         * Updates the current load based on active tasks.
         */
        private void updateLoad() {
            currentLoad = maxConcurrentTasks > 0
                ? (double) activeTasks.size() / maxConcurrentTasks
                : 0.0;
        }

        /**
         * Gets available capacity (number of additional tasks that can be assigned).
         */
        public int getAvailableCapacity() {
            return Math.max(0, maxConcurrentTasks - activeTasks.size());
        }

        /**
         * Checks if agent is at full capacity.
         */
        public boolean isAtCapacity() {
            return activeTasks.size() >= maxConcurrentTasks;
        }

        /**
         * Checks if agent is available for new tasks.
         */
        public boolean isAvailable() {
            return activeTasks.size() < maxConcurrentTasks;
        }

        @Override
        public String toString() {
            return String.format("AgentWorkload[agent=%s, active=%d/%d, load=%.2f, completed=%d, failed=%d, success=%.1f%%]",
                agentId.toString().substring(0, 8), activeTasks.size(), maxConcurrentTasks,
                currentLoad, totalCompleted.get(), totalFailed.get(), getSuccessRate() * 100);
        }
    }

    private final Map<UUID, AgentWorkload> workloads;
    private final List<WorkloadListener> listeners;
    private final int defaultMaxConcurrentTasks;

    /**
     * Creates a workload tracker with default capacity (5 concurrent tasks per agent).
     */
    public WorkloadTracker() {
        this(5);
    }

    /**
     * Creates a workload tracker with specified default capacity.
     *
     * @param defaultMaxConcurrentTasks Default maximum concurrent tasks per agent
     */
    public WorkloadTracker(int defaultMaxConcurrentTasks) {
        this.workloads = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.defaultMaxConcurrentTasks = defaultMaxConcurrentTasks;
    }

    // ========== Registration ==========

    /**
     * Registers an agent for workload tracking.
     *
     * @param agentId Agent UUID
     * @return true if registered, false if already registered
     */
    public boolean registerAgent(UUID agentId) {
        return registerAgent(agentId, defaultMaxConcurrentTasks);
    }

    /**
     * Registers an agent with custom capacity.
     *
     * @param agentId Agent UUID
     * @param maxConcurrentTasks Maximum concurrent tasks
     * @return true if registered, false if already registered
     */
    public boolean registerAgent(UUID agentId, int maxConcurrentTasks) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent ID cannot be null");
        }

        if (workloads.containsKey(agentId)) {
            return false;
        }

        AgentWorkload workload = new AgentWorkload(agentId, maxConcurrentTasks);
        workloads.put(agentId, workload);

        LOGGER.info("Registered agent for workload tracking: {} (max concurrent: {})",
            agentId.toString().substring(0, 8), maxConcurrentTasks);

        return true;
    }

    /**
     * Unregisters an agent from workload tracking.
     *
     * @param agentId Agent UUID
     * @return The removed workload, or null if not found
     */
    public AgentWorkload unregisterAgent(UUID agentId) {
        if (agentId == null) {
            return null;
        }

        AgentWorkload removed = workloads.remove(agentId);

        if (removed != null) {
            LOGGER.info("Unregistered agent from workload tracking: {} (had {} active tasks)",
                agentId.toString().substring(0, 8), removed.getActiveTaskCount());
        }

        return removed;
    }

    /**
     * Checks if an agent is registered.
     *
     * @param agentId Agent UUID
     * @return true if registered
     */
    public boolean isRegistered(UUID agentId) {
        return agentId != null && workloads.containsKey(agentId);
    }

    // ========== Task Assignment ==========

    /**
     * Assigns a task to an agent.
     *
     * @param agentId Agent UUID
     * @param taskId Task identifier
     * @return true if task was assigned, false if agent not registered or at capacity
     */
    public boolean assignTask(UUID agentId, String taskId) {
        if (agentId == null || taskId == null) {
            return false;
        }

        AgentWorkload workload = workloads.get(agentId);
        if (workload == null) {
            LOGGER.warn("Cannot assign task: agent {} not registered for workload tracking",
                agentId.toString().substring(0, 8));
            return false;
        }

        double oldLoad = workload.getCurrentLoad();
        boolean assigned = workload.assignTask(taskId);

        if (assigned) {
            double newLoad = workload.getCurrentLoad();

            LOGGER.debug("Assigned task {} to agent {} (load: {:.2f} -> {:.2f})",
                taskId, agentId.toString().substring(0, 8), oldLoad, newLoad);

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onLoadChanged(agentId, oldLoad, newLoad);
                    listener.onTaskAssigned(agentId, taskId);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in task assignment callbacks", e);
                }
            });
        } else {
            LOGGER.debug("Cannot assign task {} to agent {}: at capacity ({} active)",
                taskId, agentId.toString().substring(0, 8), workload.getActiveTaskCount());
        }

        return assigned;
    }

    /**
     * Completes a task for an agent.
     *
     * @param agentId Agent UUID
     * @param taskId Task identifier
     * @param success Whether the task succeeded
     * @return true if task was completed, false if agent not registered or task not found
     */
    public boolean completeTask(UUID agentId, String taskId, boolean success) {
        if (agentId == null || taskId == null) {
            return false;
        }

        AgentWorkload workload = workloads.get(agentId);
        if (workload == null) {
            LOGGER.warn("Cannot complete task: agent {} not registered for workload tracking",
                agentId.toString().substring(0, 8));
            return false;
        }

        double oldLoad = workload.getCurrentLoad();
        Long startTime = workload.getActiveTasks().get(taskId);
        boolean completed = workload.completeTask(taskId, success);

        if (completed) {
            double newLoad = workload.getCurrentLoad();
            long duration = startTime != null
                ? System.currentTimeMillis() - startTime
                : 0;

            LOGGER.debug("Completed task {} for agent {} (success={}, duration={}ms, load: {:.2f} -> {:.2f})",
                taskId, agentId.toString().substring(0, 8), success, duration, oldLoad, newLoad);

            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onLoadChanged(agentId, oldLoad, newLoad);
                    listener.onTaskCompleted(agentId, taskId, success, duration);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in task completion callbacks", e);
                }
            });
        } else {
            LOGGER.debug("Cannot complete task {} for agent {}: task not found in active tasks",
                taskId, agentId.toString().substring(0, 8));
        }

        return completed;
    }

    // ========== Queries ==========

    /**
     * Gets workload for an agent.
     *
     * @param agentId Agent UUID
     * @return Agent workload, or null if not registered
     */
    public AgentWorkload getWorkload(UUID agentId) {
        return workloads.get(agentId);
    }

    /**
     * Gets current load for an agent.
     *
     * @param agentId Agent UUID
     * @return Load factor (0.0-1.0), or 0.0 if not registered
     */
    public double getCurrentLoad(UUID agentId) {
        AgentWorkload workload = workloads.get(agentId);
        return workload != null ? workload.getCurrentLoad() : 0.0;
    }

    /**
     * Gets active task count for an agent.
     *
     * @param agentId Agent UUID
     * @return Number of active tasks, or 0 if not registered
     */
    public int getActiveTaskCount(UUID agentId) {
        AgentWorkload workload = workloads.get(agentId);
        return workload != null ? workload.getActiveTaskCount() : 0;
    }

    /**
     * Checks if an agent is available for new tasks.
     *
     * @param agentId Agent UUID
     * @return true if available, false if not registered or at capacity
     */
    public boolean isAvailable(UUID agentId) {
        AgentWorkload workload = workloads.get(agentId);
        return workload != null && workload.isAvailable();
    }

    /**
     * Gets all available agents.
     *
     * @return List of available agent IDs
     */
    public List<UUID> getAvailableAgents() {
        return workloads.values().stream()
            .filter(AgentWorkload::isAvailable)
            .map(AgentWorkload::getAgentId)
            .collect(Collectors.toList());
    }

    /**
     * Gets agents sorted by availability (lowest load first).
     *
     * @return List of agent IDs sorted by load
     */
    public List<UUID> getAgentsByAvailability() {
        return workloads.values().stream()
            .sorted(Comparator.comparingDouble(AgentWorkload::getCurrentLoad))
            .map(AgentWorkload::getAgentId)
            .collect(Collectors.toList());
    }

    /**
     * Gets agents with load below a threshold.
     *
     * @param maxLoad Maximum load threshold (0.0-1.0)
     * @return List of agent IDs below threshold
     */
    public List<UUID> getAgentsWithLoadBelow(double maxLoad) {
        return workloads.values().stream()
            .filter(w -> w.getCurrentLoad() < maxLoad)
            .map(AgentWorkload::getAgentId)
            .collect(Collectors.toList());
    }

    /**
     * Gets the least loaded agent.
     *
     * @return Agent ID with lowest load, or empty if no agents registered
     */
    public Optional<UUID> getLeastLoadedAgent() {
        return workloads.values().stream()
            .filter(AgentWorkload::isAvailable)
            .min(Comparator.comparingDouble(AgentWorkload::getCurrentLoad))
            .map(AgentWorkload::getAgentId);
    }

    // ========== Listeners ==========

    /**
     * Adds a workload listener.
     *
     * @param listener The listener to add
     */
    public void addListener(WorkloadListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a workload listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(WorkloadListener listener) {
        listeners.remove(listener);
    }

    // ========== Statistics ==========

    /**
     * Gets the number of registered agents.
     *
     * @return Agent count
     */
    public int getAgentCount() {
        return workloads.size();
    }

    /**
     * Gets the total number of active tasks across all agents.
     *
     * @return Total active tasks
     */
    public int getTotalActiveTasks() {
        return workloads.values().stream()
            .mapToInt(AgentWorkload::getActiveTaskCount)
            .sum();
    }

    /**
     * Gets the average load across all agents.
     *
     * @return Average load (0.0-1.0), or 0.0 if no agents
     */
    public double getAverageLoad() {
        if (workloads.isEmpty()) {
            return 0.0;
        }

        return workloads.values().stream()
            .mapToDouble(AgentWorkload::getCurrentLoad)
            .average()
            .orElse(0.0);
    }

    /**
     * Gets statistics summary.
     *
     * @return Map of statistic name to value
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("agentCount", getAgentCount());
        stats.put("totalActiveTasks", getTotalActiveTasks());
        stats.put("averageLoad", getAverageLoad());
        stats.put("availableAgents", getAvailableAgents().size());

        // Total completed/failed across all agents
        int totalCompleted = workloads.values().stream()
            .mapToInt(AgentWorkload::getTotalCompleted)
            .sum();
        int totalFailed = workloads.values().stream()
            .mapToInt(AgentWorkload::getTotalFailed)
            .sum();

        stats.put("totalCompleted", totalCompleted);
        stats.put("totalFailed", totalFailed);

        int total = totalCompleted + totalFailed;
        double overallSuccessRate = total > 0 ? (double) totalCompleted / total : 0.0;
        stats.put("overallSuccessRate", overallSuccessRate);

        return stats;
    }

    // ========== Cleanup ==========

    /**
     * Removes all workload data.
     */
    public void clear() {
        int count = workloads.size();
        workloads.clear();

        if (count > 0) {
            LOGGER.info("Cleared workload tracking for {} agents", count);
        }
    }

    @Override
    public String toString() {
        Map<String, Object> stats = getStatistics();
        return String.format("WorkloadTracker[agents=%d, active=%d, avgLoad=%.2f, available=%d]",
            stats.get("agentCount"), stats.get("totalActiveTasks"),
            stats.get("averageLoad"), stats.get("availableAgents"));
    }
}
