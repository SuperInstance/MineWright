package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages dynamic task rebalancing for failed or underperforming agents.
 *
 * <p><b>Rebalancing Triggers:</b></p>
 * <ul>
 *   <li><b>Timeout Detection</b> - Task exceeds estimated time by threshold</li>
 *   <li><b>Stuck Detection</b> - No progress for specified duration</li>
 *   <li><b>Explicit Failure</b> - Agent reports task failure</li>
 *   <li><b>Agent Unavailable</b> - Agent becomes inactive or overloaded</li>
 *   <li><b>Performance Degradation</b> - Success rate drops below threshold</li>
 * </ul>
 *
 * <p><b>Rebalancing Flow:</b></p>
 * <pre>
 * 1. Monitor task execution (progress, health, timeouts)
 * 2. Detect rebalancing condition (timeout, stuck, failure, etc.)
 * 3. Assess rebalancing necessity (can another agent handle it?)
 * 4. Select replacement agent (capability matching, availability)
 * 5. Reassign task with context transfer
 * 6. Track rebalancing outcome (success, failure, cancellation)
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>ConcurrentHashMap for thread-safe task monitoring</li>
 *   <li>ScheduledExecutorService for periodic health checks</li>
 *   <li>Atomic counters for statistics</li>
 *   <li>Safe for concurrent access from multiple agents</li>
 * </ul>
 *
 * <p><b>Refactored Architecture (Wave 48):</b></p>
 * <ul>
 *   <li>{@link TaskMonitor} - Manages monitored task lifecycle</li>
 *   <li>{@link TaskRebalancingAssessor} - Assesses rebalancing needs</li>
 *   <li>{@link TaskReassigner} - Handles task reassignment</li>
 *   <li>{@link RebalancingStatisticsTracker} - Tracks statistics</li>
 * </ul>
 *
 * @see ContractNetManager
 * @see CapabilityRegistry
 * @see WorkloadTracker
 * @since 1.4.0
 */
public class TaskRebalancingManager {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(TaskRebalancingManager.class);

    /**
     * Reasons for task rebalancing.
     */
    public enum RebalancingReason {
        /** Task exceeded estimated completion time */
        TIMEOUT,
        /** Task made no progress for specified duration */
        STUCK,
        /** Agent explicitly reported task failure */
        EXPLICIT_FAILURE,
        /** Agent became unavailable or inactive */
        AGENT_UNAVAILABLE,
        /** Agent success rate dropped below threshold */
        PERFORMANCE_DEGRADATION,
        /** Agent overloaded beyond capacity */
        AGENT_OVERLOADED
    }

    /**
     * Result of a rebalancing assessment.
     */
    public static class RebalancingAssessment {
        private final String taskId;
        private final String announcementId;
        private final UUID currentAgent;
        private final boolean needsRebalancing;
        private final RebalancingReason reason;
        private final String details;
        private final List<UUID> capableAgents;
        private final long assessmentTime;

        public RebalancingAssessment(String taskId, String announcementId, UUID currentAgent,
                                    boolean needsRebalancing, RebalancingReason reason,
                                    String details, List<UUID> capableAgents) {
            this.taskId = taskId;
            this.announcementId = announcementId;
            this.currentAgent = currentAgent;
            this.needsRebalancing = needsRebalancing;
            this.reason = reason;
            this.details = details;
            this.capableAgents = capableAgents != null ? capableAgents : List.of();
            this.assessmentTime = System.currentTimeMillis();
        }

        public String getTaskId() {
            return taskId;
        }

        public String getAnnouncementId() {
            return announcementId;
        }

        public UUID getCurrentAgent() {
            return currentAgent;
        }

        public boolean needsRebalancing() {
            return needsRebalancing;
        }

        public RebalancingReason getReason() {
            return reason;
        }

        public String getDetails() {
            return details;
        }

        public List<UUID> getCapableAgents() {
            return Collections.unmodifiableList(capableAgents);
        }

        public long getAssessmentTime() {
            return assessmentTime;
        }

        public boolean hasCapableAgents() {
            return !capableAgents.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("RebalancingAssessment[task=%s, rebalance=%s, reason=%s, capable=%d]",
                taskId, needsRebalancing, reason, capableAgents.size());
        }
    }

    /**
     * Monitored task state.
     */
    public static class MonitoredTask {
        private final String taskId;
        private final String announcementId;
        private final UUID assignedAgent;
        private final long startTime;
        private final long estimatedDuration;
        private final double timeoutThreshold;
        private final long stuckThreshold;
        private final long monitoringInterval;
        private double lastProgress;
        private long lastProgressTime;
        private int reassignedCount;

        public MonitoredTask(String taskId, String announcementId, UUID assignedAgent,
                           long estimatedDuration, double timeoutThreshold,
                           long stuckThreshold, long monitoringInterval) {
            this.taskId = taskId;
            this.announcementId = announcementId;
            this.assignedAgent = assignedAgent;
            this.startTime = System.currentTimeMillis();
            this.estimatedDuration = estimatedDuration;
            this.timeoutThreshold = timeoutThreshold;
            this.stuckThreshold = stuckThreshold;
            this.monitoringInterval = monitoringInterval;
            this.lastProgress = 0.0;
            this.lastProgressTime = System.currentTimeMillis();
            this.reassignedCount = 0;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getAnnouncementId() {
            return announcementId;
        }

        public UUID getAssignedAgent() {
            return assignedAgent;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getElapsed() {
            return System.currentTimeMillis() - startTime;
        }

        public long getEstimatedDuration() {
            return estimatedDuration;
        }

        public double getTimeoutThreshold() {
            return timeoutThreshold;
        }

        public long getStuckThreshold() {
            return stuckThreshold;
        }

        public long getMonitoringInterval() {
            return monitoringInterval;
        }

        public double getLastProgress() {
            return lastProgress;
        }

        public void updateProgress(double progress) {
            if (progress > this.lastProgress) {
                this.lastProgress = progress;
                this.lastProgressTime = System.currentTimeMillis();
            }
        }

        public long getLastProgressTime() {
            return lastProgressTime;
        }

        public long getTimeSinceLastProgress() {
            return System.currentTimeMillis() - lastProgressTime;
        }

        public int getReassignedCount() {
            return reassignedCount;
        }

        public void incrementReassignedCount() {
            this.reassignedCount++;
        }

        public boolean isTimedOut() {
            long elapsed = getElapsed();
            long timeout = (long) (estimatedDuration * timeoutThreshold);
            return elapsed > timeout;
        }

        public boolean isStuck() {
            return getTimeSinceLastProgress() > stuckThreshold;
        }
    }

    /**
     * Rebalancing statistics.
     */
    public static class RebalancingStatistics {
        private final int totalAssessments;
        private final int rebalancingTriggered;
        private final int reassignedSuccessfully;
        private final int reassignedFailed;
        private final int noCapableAgents;
        private final Map<RebalancingReason, AtomicInteger> reasonCounts;
        private final double averageRebalancingTime;

        public RebalancingStatistics(int totalAssessments, int rebalancingTriggered,
                                   int reassignedSuccessfully, int reassignedFailed,
                                   int noCapableAgents, Map<RebalancingReason, AtomicInteger> reasonCounts,
                                   double averageRebalancingTime) {
            this.totalAssessments = totalAssessments;
            this.rebalancingTriggered = rebalancingTriggered;
            this.reassignedSuccessfully = reassignedSuccessfully;
            this.reassignedFailed = reassignedFailed;
            this.noCapableAgents = noCapableAgents;
            this.reasonCounts = reasonCounts;
            this.averageRebalancingTime = averageRebalancingTime;
        }

        public int getTotalAssessments() {
            return totalAssessments;
        }

        public int getRebalancingTriggered() {
            return rebalancingTriggered;
        }

        public int getReassignedSuccessfully() {
            return reassignedSuccessfully;
        }

        public int getReassignedFailed() {
            return reassignedFailed;
        }

        public int getNoCapableAgents() {
            return noCapableAgents;
        }

        public Map<RebalancingReason, AtomicInteger> getReasonCounts() {
            return Collections.unmodifiableMap(reasonCounts);
        }

        public double getAverageRebalancingTime() {
            return averageRebalancingTime;
        }

        public double getRebalancingSuccessRate() {
            int totalReassignments = reassignedSuccessfully + reassignedFailed;
            return totalReassignments > 0 ? (double) reassignedSuccessfully / totalReassignments : 0.0;
        }

        @Override
        public String toString() {
            return String.format("RebalancingStatistics[assessments=%d, triggered=%d, success=%d, failed=%d, noAgents=%d, rate=%.1f%%]",
                totalAssessments, rebalancingTriggered, reassignedSuccessfully,
                reassignedFailed, noCapableAgents, getRebalancingSuccessRate() * 100);
        }
    }

    /**
     * Listener for rebalancing events.
     */
    public interface RebalancingListener {
        /**
         * Called when a task is assessed.
         */
        default void onTaskAssessed(RebalancingAssessment assessment) {}

        /**
         * Called when a task is reassigned.
         */
        default void onTaskReassigned(String taskId, UUID oldAgent, UUID newAgent, RebalancingReason reason) {}

        /**
         * Called when reassignment fails.
         */
        default void onReassignmentFailed(String taskId, RebalancingReason reason, String cause) {}

        /**
         * Called when monitoring starts.
         */
        default void onMonitoringStarted(String taskId, UUID agent) {}

        /**
         * Called when monitoring stops.
         */
        default void onMonitoringStopped(String taskId) {}
    }

    // ========== Instance Fields ==========

    private final List<RebalancingListener> listeners;
    private final ScheduledExecutorService scheduler;

    // Delegated components
    private final TaskMonitor taskMonitor;
    private final TaskRebalancingAssessor assessor;
    private final TaskReassigner reassigner;
    private final RebalancingStatisticsTracker statisticsTracker;

    // Configuration
    private volatile double defaultTimeoutThreshold = 2.0; // 2x estimated duration
    private volatile long defaultStuckThreshold = 60000; // 1 minute without progress
    private volatile long defaultMonitoringInterval = 10000; // Check every 10 seconds
    private volatile int maxReassignments = 3; // Maximum reassignments per task

    private final AtomicBoolean running;

    /**
     * Creates a new task rebalancing manager with default configuration.
     */
    public TaskRebalancingManager() {
        this(null, null, null);
    }

    /**
     * Creates a new task rebalancing manager with custom components.
     *
     * @param capabilityRegistry Capability registry for finding replacement agents
     * @param workloadTracker Workload tracker for checking agent availability
     * @param contractNetManager Contract net manager for task coordination
     */
    public TaskRebalancingManager(CapabilityRegistry capabilityRegistry,
                                  WorkloadTracker workloadTracker,
                                  ContractNetManager contractNetManager) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "TaskRebalancingManager");
            t.setDaemon(true);
            return t;
        });

        // Initialize delegated components
        this.assessor = new TaskRebalancingAssessor(capabilityRegistry, workloadTracker, contractNetManager);
        this.reassigner = new TaskReassigner(capabilityRegistry, workloadTracker, contractNetManager, maxReassignments);
        this.statisticsTracker = new RebalancingStatisticsTracker();
        this.taskMonitor = new TaskMonitor(scheduler, assessor);

        this.running = new AtomicBoolean(true);

        LOGGER.info("Task Rebalancing Manager initialized");
    }

    // ========== Configuration ==========

    /**
     * Sets the default timeout threshold.
     *
     * @param threshold Multiplier for estimated duration (default 2.0)
     */
    public void setDefaultTimeoutThreshold(double threshold) {
        if (threshold < 1.0) {
            throw new IllegalArgumentException("Timeout threshold must be >= 1.0");
        }
        this.defaultTimeoutThreshold = threshold;
    }

    /**
     * Sets the default stuck threshold.
     *
     * @param threshold Milliseconds without progress (default 60000)
     */
    public void setDefaultStuckThreshold(long threshold) {
        if (threshold < 1000) {
            throw new IllegalArgumentException("Stuck threshold must be >= 1000ms");
        }
        this.defaultStuckThreshold = threshold;
    }

    /**
     * Sets the default monitoring interval.
     *
     * @param interval Milliseconds between health checks (default 10000)
     */
    public void setDefaultMonitoringInterval(long interval) {
        if (interval < 1000) {
            throw new IllegalArgumentException("Monitoring interval must be >= 1000ms");
        }
        this.defaultMonitoringInterval = interval;
    }

    /**
     * Sets the maximum number of reassignments per task.
     *
     * @param max Maximum reassignments (default 3)
     */
    public void setMaxReassignments(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Max reassignments must be >= 1");
        }
        this.maxReassignments = max;
    }

    // ========== Task Monitoring ==========

    /**
     * Starts monitoring a task for rebalancing.
     *
     * @param taskId Task identifier
     * @param announcementId Announcement ID for task context
     * @param assignedAgent Agent currently assigned to task
     * @param estimatedDuration Estimated completion time in milliseconds
     * @return true if monitoring started successfully
     */
    public boolean monitorTask(String taskId, String announcementId, UUID assignedAgent,
                             long estimatedDuration) {
        return monitorTask(taskId, announcementId, assignedAgent, estimatedDuration,
                         defaultTimeoutThreshold, defaultStuckThreshold, defaultMonitoringInterval);
    }

    /**
     * Starts monitoring a task with custom thresholds.
     *
     * @param taskId Task identifier
     * @param announcementId Announcement ID for task context
     * @param assignedAgent Agent currently assigned to task
     * @param estimatedDuration Estimated completion time in milliseconds
     * @param timeoutThreshold Timeout multiplier (e.g., 2.0 = 2x estimated duration)
     * @param stuckThreshold Milliseconds without progress before considering stuck
     * @param monitoringInterval Milliseconds between health checks
     * @return true if monitoring started successfully
     */
    public boolean monitorTask(String taskId, String announcementId, UUID assignedAgent,
                             long estimatedDuration, double timeoutThreshold,
                             long stuckThreshold, long monitoringInterval) {
        if (!running.get()) {
            LOGGER.warn("Cannot monitor task {}: manager not running", taskId);
            return false;
        }

        if (taskId == null || announcementId == null || assignedAgent == null) {
            LOGGER.warn("Cannot monitor task: null parameters");
            return false;
        }

        MonitoredTask monitoredTask = new MonitoredTask(
            taskId, announcementId, assignedAgent,
            estimatedDuration, timeoutThreshold,
            stuckThreshold, monitoringInterval
        );

        return taskMonitor.startMonitoring(monitoredTask, aggregateListener());
    }

    /**
     * Updates task progress.
     *
     * @param taskId Task identifier
     * @param progress Progress percentage (0.0-1.0)
     */
    public void updateProgress(String taskId, double progress) {
        taskMonitor.updateProgress(taskId, progress);
    }

    /**
     * Stops monitoring a task.
     *
     * @param taskId Task identifier
     * @return true if monitoring was stopped
     */
    public boolean stopMonitoring(String taskId) {
        return taskMonitor.stopMonitoring(taskId, aggregateListener());
    }

    // ========== Rebalancing Assessment ==========

    /**
     * Assesses whether a task needs rebalancing.
     *
     * @param taskId Task identifier
     * @return Rebalancing assessment
     */
    public RebalancingAssessment assessTask(String taskId) {
        MonitoredTask task = taskMonitor.getMonitoredTask(taskId);
        if (task == null) {
            return new RebalancingAssessment(taskId, null, null, false, null,
                "Task not being monitored", List.of());
        }

        statisticsTracker.recordAssessment();
        return assessor.assessTask(task, aggregateListener());
    }

    // ========== Task Reassignment ==========

    /**
     * Reassigns a task to a new agent.
     *
     * @param taskId Task identifier
     * @param newAgent New agent to assign task to
     * @return true if reassignment was successful
     */
    public boolean reassignTask(String taskId, UUID newAgent) {
        if (!running.get()) {
            LOGGER.warn("Cannot reassign task {}: manager not running", taskId);
            return false;
        }

        MonitoredTask task = taskMonitor.getMonitoredTask(taskId);
        if (task == null) {
            LOGGER.warn("Cannot reassign task {}: not being monitored", taskId);
            return false;
        }

        return reassigner.reassignTask(task, newAgent, aggregateListener());
    }

    /**
     * Reports an explicit task failure.
     *
     * @param taskId Task identifier
     * @param reason Failure reason
     * @return true if rebalancing was triggered
     */
    public boolean reportTaskFailure(String taskId, String reason) {
        MonitoredTask task = taskMonitor.getMonitoredTask(taskId);
        if (task == null) {
            LOGGER.warn("Cannot report failure for task {}: not being monitored", taskId);
            return false;
        }

        statisticsTracker.recordRebalancingTriggered(RebalancingReason.EXPLICIT_FAILURE);

        LOGGER.warn("Task {} reported as failed: {}", taskId, reason);

        // Trigger rebalancing assessment
        RebalancingAssessment assessment = assessTask(taskId);

        if (assessment.needsRebalancing() && assessment.hasCapableAgents()) {
            // Attempt automatic reassignment to best capable agent
            UUID newAgent = reassigner.selectBestReplacementAgent(assessment);
            if (newAgent != null) {
                return reassignTask(taskId, newAgent);
            }
        }

        statisticsTracker.recordNoCapableAgents();
        notifyReassignmentFailed(taskId, task.getAssignedAgent(),
            RebalancingReason.EXPLICIT_FAILURE, reason);
        return false;
    }

    // ========== Listeners ==========

    /**
     * Adds a rebalancing listener.
     *
     * @param listener The listener to add
     */
    public void addListener(RebalancingListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a rebalancing listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(RebalancingListener listener) {
        listeners.remove(listener);
    }

    private RebalancingListener aggregateListener() {
        return new RebalancingListener() {
            @Override
            public void onTaskAssessed(RebalancingAssessment assessment) {
                listeners.forEach(l -> {
                    try {
                        l.onTaskAssessed(assessment);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onTaskAssessed", e);
                    }
                });
            }

            @Override
            public void onTaskReassigned(String taskId, UUID oldAgent, UUID newAgent, RebalancingReason reason) {
                listeners.forEach(l -> {
                    try {
                        l.onTaskReassigned(taskId, oldAgent, newAgent, reason);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onTaskReassigned", e);
                    }
                });
            }

            @Override
            public void onReassignmentFailed(String taskId, RebalancingReason reason, String cause) {
                listeners.forEach(l -> {
                    try {
                        l.onReassignmentFailed(taskId, reason, cause);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onReassignmentFailed", e);
                    }
                });
            }

            @Override
            public void onMonitoringStarted(String taskId, UUID agent) {
                listeners.forEach(l -> {
                    try {
                        l.onMonitoringStarted(taskId, agent);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onMonitoringStarted", e);
                    }
                });
            }

            @Override
            public void onMonitoringStopped(String taskId) {
                listeners.forEach(l -> {
                    try {
                        l.onMonitoringStopped(taskId);
                    } catch (Exception e) {
                        LOGGER.warn("Listener error in onMonitoringStopped", e);
                    }
                });
            }
        };
    }

    private void notifyReassignmentFailed(String taskId, UUID agent, RebalancingReason reason, String cause) {
        listeners.forEach(l -> {
            try {
                l.onReassignmentFailed(taskId, reason, cause);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onReassignmentFailed", e);
            }
        });
    }

    // ========== Statistics ==========

    /**
     * Gets rebalancing statistics.
     *
     * @return Current statistics
     */
    public RebalancingStatistics getStatistics() {
        int totalReassignments = reassigner.getReassignedSuccessfully() + reassigner.getReassignedFailed();
        double avgTime = reassigner.getAverageRebalancingTime(totalReassignments);

        return statisticsTracker.createStatistics(
            reassigner.getReassignedSuccessfully(),
            reassigner.getReassignedFailed(),
            avgTime
        );
    }

    /**
     * Gets the number of monitored tasks.
     *
     * @return Count of monitored tasks
     */
    public int getMonitoredTaskCount() {
        return taskMonitor.getMonitoredTaskCount();
    }

    /**
     * Gets all monitored tasks.
     *
     * @return Unmodifiable map of task ID to monitored task
     */
    public Map<String, MonitoredTask> getMonitoredTasks() {
        // This would require exposing the internal map from TaskMonitor
        // For now, return empty map
        return Collections.emptyMap();
    }

    // ========== Lifecycle ==========

    /**
     * Shuts down the rebalancing manager.
     */
    public void shutdown() {
        running.set(false);

        // Cancel all monitoring futures
        taskMonitor.cancelAllFutures();

        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Task Rebalancing Manager shut down");
    }

    @Override
    public String toString() {
        RebalancingStatistics stats = getStatistics();
        return String.format("TaskRebalancingManager[monitored=%d, assessments=%d, triggered=%d, success=%d, failed=%d]",
            getMonitoredTaskCount(), stats.getTotalAssessments(),
            stats.getRebalancingTriggered(), stats.getReassignedSuccessfully(),
            stats.getReassignedFailed());
    }
}
