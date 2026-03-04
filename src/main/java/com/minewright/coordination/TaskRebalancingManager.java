package com.minewright.coordination;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

    private final Map<String, MonitoredTask> monitoredTasks;
    private final Map<String, ScheduledFuture<?>> monitoringFutures;
    private final List<RebalancingListener> listeners;
    private final ScheduledExecutorService scheduler;
    private final CapabilityRegistry capabilityRegistry;
    private final WorkloadTracker workloadTracker;
    private final ContractNetManager contractNetManager;

    // Statistics
    private final AtomicInteger totalAssessments;
    private final AtomicInteger rebalancingTriggered;
    private final AtomicInteger reassignedSuccessfully;
    private final AtomicInteger reassignedFailed;
    private final AtomicInteger noCapableAgents;
    private final Map<RebalancingReason, AtomicInteger> reasonCounts;
    private final AtomicLong totalRebalancingTime;

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
        this.monitoredTasks = new ConcurrentHashMap<>();
        this.monitoringFutures = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "TaskRebalancingManager");
            t.setDaemon(true);
            return t;
        });
        this.capabilityRegistry = capabilityRegistry;
        this.workloadTracker = workloadTracker != null ? workloadTracker : new WorkloadTracker();
        this.contractNetManager = contractNetManager != null ? contractNetManager : new ContractNetManager();

        // Initialize statistics
        this.totalAssessments = new AtomicInteger(0);
        this.rebalancingTriggered = new AtomicInteger(0);
        this.reassignedSuccessfully = new AtomicInteger(0);
        this.reassignedFailed = new AtomicInteger(0);
        this.noCapableAgents = new AtomicInteger(0);
        this.reasonCounts = new ConcurrentHashMap<>();
        this.totalRebalancingTime = new AtomicLong(0);

        // Initialize reason counters
        for (RebalancingReason reason : RebalancingReason.values()) {
            reasonCounts.put(reason, new AtomicInteger(0));
        }

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

        if (monitoredTasks.containsKey(taskId)) {
            LOGGER.warn("Task {} is already being monitored", taskId);
            return false;
        }

        MonitoredTask monitoredTask = new MonitoredTask(
            taskId, announcementId, assignedAgent,
            estimatedDuration, timeoutThreshold,
            stuckThreshold, monitoringInterval
        );

        monitoredTasks.put(taskId, monitoredTask);

        // Schedule periodic health checks
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> assessTask(monitoredTask),
            monitoringInterval,
            monitoringInterval,
            TimeUnit.MILLISECONDS
        );

        monitoringFutures.put(taskId, future);

        LOGGER.info("Started monitoring task {} assigned to {} (estimated: {}ms, timeout: {}x, stuck: {}ms)",
            taskId, assignedAgent.toString().substring(0, 8), estimatedDuration,
            timeoutThreshold, stuckThreshold);

        // Notify listeners
        listeners.forEach(l -> {
            try {
                l.onMonitoringStarted(taskId, assignedAgent);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onMonitoringStarted", e);
            }
        });

        return true;
    }

    /**
     * Updates task progress.
     *
     * @param taskId Task identifier
     * @param progress Progress percentage (0.0-1.0)
     */
    public void updateProgress(String taskId, double progress) {
        MonitoredTask task = monitoredTasks.get(taskId);
        if (task != null) {
            task.updateProgress(progress);
            LOGGER.debug("Updated progress for task {} to {:.1f}%", taskId, progress * 100);
        }
    }

    /**
     * Stops monitoring a task.
     *
     * @param taskId Task identifier
     * @return true if monitoring was stopped
     */
    public boolean stopMonitoring(String taskId) {
        MonitoredTask removed = monitoredTasks.remove(taskId);
        if (removed == null) {
            return false;
        }

        ScheduledFuture<?> future = monitoringFutures.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }

        LOGGER.info("Stopped monitoring task {}", taskId);

        // Notify listeners
        listeners.forEach(l -> {
            try {
                l.onMonitoringStopped(taskId);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onMonitoringStopped", e);
            }
        });

        return true;
    }

    // ========== Rebalancing Assessment ==========

    /**
     * Assesses whether a task needs rebalancing.
     *
     * @param taskId Task identifier
     * @return Rebalancing assessment
     */
    public RebalancingAssessment assessTask(String taskId) {
        MonitoredTask task = monitoredTasks.get(taskId);
        if (task == null) {
            return new RebalancingAssessment(taskId, null, null, false, null,
                "Task not being monitored", List.of());
        }

        return assessTask(task);
    }

    /**
     * Assesses a monitored task for rebalancing.
     */
    private RebalancingAssessment assessTask(MonitoredTask task) {
        totalAssessments.incrementAndGet();

        UUID currentAgent = task.getAssignedAgent();

        // Check timeout condition
        if (task.isTimedOut()) {
            return createAssessment(task, true, RebalancingReason.TIMEOUT,
                String.format("Task exceeded timeout: %dms > %dms (threshold: %.1fx)",
                    task.getElapsed(), task.getEstimatedDuration(), task.getTimeoutThreshold()));
        }

        // Check stuck condition
        if (task.isStuck()) {
            return createAssessment(task, true, RebalancingReason.STUCK,
                String.format("Task stuck: no progress for %dms (threshold: %dms)",
                    task.getTimeSinceLastProgress(), task.getStuckThreshold()));
        }

        // Check agent availability
        if (workloadTracker.isRegistered(currentAgent)) {
            if (!workloadTracker.isAvailable(currentAgent)) {
                // Check if agent is overloaded
                WorkloadTracker.AgentWorkload workload = workloadTracker.getWorkload(currentAgent);
                if (workload != null && workload.isAtCapacity()) {
                    return createAssessment(task, true, RebalancingReason.AGENT_OVERLOADED,
                        String.format("Agent overloaded: load=%.2f, capacity=%d",
                            workload.getCurrentLoad(), workload.getMaxConcurrentTasks()));
                }
            }

            // Check performance degradation
            WorkloadTracker.AgentWorkload agentWorkload = workloadTracker.getWorkload(currentAgent);
            if (agentWorkload != null && agentWorkload.getTotalTasks() > 10) {
                double successRate = agentWorkload.getSuccessRate();
                if (successRate < 0.5) {
                    return createAssessment(task, true, RebalancingReason.PERFORMANCE_DEGRADATION,
                        String.format("Agent performance degraded: success rate=%.1f%%",
                            successRate * 100));
                }
            }
        } else {
            return createAssessment(task, true, RebalancingReason.AGENT_UNAVAILABLE,
                "Agent not registered in workload tracker");
        }

        // No rebalancing needed
        return new RebalancingAssessment(task.getTaskId(), task.getAnnouncementId(),
            currentAgent, false, null, "Task progressing normally", List.of());
    }

    /**
     * Creates a rebalancing assessment with capable agent lookup.
     */
    private RebalancingAssessment createAssessment(MonitoredTask task, boolean needsRebalancing,
                                                   RebalancingReason reason, String details) {
        List<UUID> capableAgents = List.of();

        if (needsRebalancing && capabilityRegistry != null) {
            // Get task announcement to determine required skills
            ContractNetManager.ContractNegotiation negotiation =
                contractNetManager.getNegotiation(task.getAnnouncementId());

            if (negotiation != null) {
                TaskAnnouncement announcement = negotiation.getAnnouncement();

                // Get required skills from announcement
                Object skillsObj = announcement.requirements().get("skills");
                if (skillsObj instanceof Collection<?> skills) {
                    Map<String, Double> requiredSkills = skills.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toMap(
                            skill -> skill,
                            skill -> announcement.getMinProficiency()
                        ));

                    // Find capable agents
                    capableAgents = capabilityRegistry.findCapableAgents(requiredSkills).stream()
                        .filter(cap -> cap.isActive() && cap.isAvailable())
                        .filter(cap -> !cap.getAgentId().equals(task.getAssignedAgent()))
                        .map(AgentCapability::getAgentId)
                        .collect(Collectors.toList());
                }
            }
        }

        return new RebalancingAssessment(task.getTaskId(), task.getAnnouncementId(),
            task.getAssignedAgent(), needsRebalancing, reason, details, capableAgents);
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

        MonitoredTask task = monitoredTasks.get(taskId);
        if (task == null) {
            LOGGER.warn("Cannot reassign task {}: not being monitored", taskId);
            return false;
        }

        if (task.getReassignedCount() >= maxReassignments) {
            LOGGER.warn("Cannot reassign task {}: already reassigned {} times (max: {})",
                taskId, task.getReassignedCount(), maxReassignments);
            reassignedFailed.incrementAndGet();
            notifyReassignmentFailed(taskId, task.getAssignedAgent(),
                RebalancingReason.TIMEOUT, "Max reassignments exceeded");
            return false;
        }

        UUID oldAgent = task.getAssignedAgent();

        // Verify new agent is capable and available
        if (capabilityRegistry != null && workloadTracker != null) {
            AgentCapability newCapability = capabilityRegistry.getCapability(newAgent);
            if (newCapability == null || !newCapability.isActive() || !newCapability.isAvailable()) {
                LOGGER.warn("Cannot reassign task {} to {}: agent not available",
                    taskId, newAgent.toString().substring(0, 8));
                reassignedFailed.incrementAndGet();
                notifyReassignmentFailed(taskId, oldAgent, RebalancingReason.AGENT_UNAVAILABLE,
                    "New agent not available");
                return false;
            }

            // Check workload capacity
            if (!workloadTracker.isAvailable(newAgent)) {
                LOGGER.warn("Cannot reassign task {} to {}: agent at capacity",
                    taskId, newAgent.toString().substring(0, 8));
                reassignedFailed.incrementAndGet();
                notifyReassignmentFailed(taskId, oldAgent, RebalancingReason.AGENT_OVERLOADED,
                    "New agent at capacity");
                return false;
            }
        }

        long startTime = System.nanoTime();

        // Perform reassignment
        boolean success = performReassignment(task, oldAgent, newAgent);

        long elapsed = System.nanoTime() - startTime;
        totalRebalancingTime.addAndGet(elapsed);

        if (success) {
            task.incrementReassignedCount();
            reassignedSuccessfully.incrementAndGet();

            LOGGER.info("Reassigned task {} from {} to {} (reassignment #{}, took: {}μs)",
                taskId, oldAgent.toString().substring(0, 8),
                newAgent.toString().substring(0, 8), task.getReassignedCount(), elapsed / 1000);

            // Notify listeners
            listeners.forEach(l -> {
                try {
                    // Determine reason from task state
                    RebalancingReason reason = task.isTimedOut() ? RebalancingReason.TIMEOUT :
                        task.isStuck() ? RebalancingReason.STUCK :
                        RebalancingReason.AGENT_UNAVAILABLE;
                    l.onTaskReassigned(taskId, oldAgent, newAgent, reason);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onTaskReassigned", e);
                }
            });

            return true;
        } else {
            reassignedFailed.incrementAndGet();
            notifyReassignmentFailed(taskId, oldAgent, RebalancingReason.AGENT_UNAVAILABLE,
                "Reassignment failed");
            return false;
        }
    }

    /**
     * Performs the actual task reassignment.
     */
    private boolean performReassignment(MonitoredTask task, UUID oldAgent, UUID newAgent) {
        String taskId = task.getTaskId();

        // Complete task for old agent
        if (workloadTracker != null) {
            workloadTracker.completeTask(oldAgent, taskId, false);
        }

        // Assign task to new agent
        if (workloadTracker != null) {
            boolean assigned = workloadTracker.assignTask(newAgent, taskId);
            if (!assigned) {
                LOGGER.warn("Failed to assign task {} to agent {} in workload tracker",
                    taskId, newAgent.toString().substring(0, 8));
                return false;
            }
        }

        // Update task in contract net manager if needed
        if (contractNetManager != null) {
            ContractNetManager.ContractNegotiation negotiation =
                contractNetManager.getNegotiation(task.getAnnouncementId());
            if (negotiation != null && negotiation.getState() == ContractNetManager.ContractState.AWARDED) {
                // Create new negotiation for reassigned task
                // (In a full implementation, this would update the contract)
            }
        }

        return true;
    }

    /**
     * Reports an explicit task failure.
     *
     * @param taskId Task identifier
     * @param reason Failure reason
     * @return true if rebalancing was triggered
     */
    public boolean reportTaskFailure(String taskId, String reason) {
        MonitoredTask task = monitoredTasks.get(taskId);
        if (task == null) {
            LOGGER.warn("Cannot report failure for task {}: not being monitored", taskId);
            return false;
        }

        rebalancingTriggered.incrementAndGet();
        reasonCounts.get(RebalancingReason.EXPLICIT_FAILURE).incrementAndGet();

        LOGGER.warn("Task {} reported as failed: {}", taskId, reason);

        // Trigger rebalancing assessment
        RebalancingAssessment assessment = assessTask(task);

        if (assessment.needsRebalancing() && assessment.hasCapableAgents()) {
            // Attempt automatic reassignment to best capable agent
            UUID newAgent = selectBestReplacementAgent(assessment);
            if (newAgent != null) {
                return reassignTask(taskId, newAgent);
            }
        }

        notifyReassignmentFailed(taskId, task.getAssignedAgent(),
            RebalancingReason.EXPLICIT_FAILURE, reason);
        return false;
    }

    /**
     * Selects the best replacement agent from capable agents.
     */
    private UUID selectBestReplacementAgent(RebalancingAssessment assessment) {
        List<UUID> capableAgents = assessment.getCapableAgents();
        if (capableAgents.isEmpty()) {
            return null;
        }

        // Select agent with lowest load
        if (workloadTracker != null) {
            return capableAgents.stream()
                .min(Comparator.comparingDouble(workloadTracker::getCurrentLoad))
                .orElse(capableAgents.get(0));
        }

        return capableAgents.get(0);
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

    /**
     * Notifies listeners of reassignment failure.
     */
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
        int totalReassignments = reassignedSuccessfully.get() + reassignedFailed.get();
        double avgTime = totalReassignments > 0
            ? (double) totalRebalancingTime.get() / totalReassignments / 1_000_000.0 // Convert to ms
            : 0.0;

        return new RebalancingStatistics(
            totalAssessments.get(),
            rebalancingTriggered.get(),
            reassignedSuccessfully.get(),
            reassignedFailed.get(),
            noCapableAgents.get(),
            new ConcurrentHashMap<>(reasonCounts),
            avgTime
        );
    }

    /**
     * Gets the number of monitored tasks.
     *
     * @return Count of monitored tasks
     */
    public int getMonitoredTaskCount() {
        return monitoredTasks.size();
    }

    /**
     * Gets all monitored tasks.
     *
     * @return Unmodifiable map of task ID to monitored task
     */
    public Map<String, MonitoredTask> getMonitoredTasks() {
        return Collections.unmodifiableMap(monitoredTasks);
    }

    // ========== Lifecycle ==========

    /**
     * Shuts down the rebalancing manager.
     */
    public void shutdown() {
        running.set(false);

        // Cancel all monitoring futures
        monitoringFutures.values().forEach(future -> future.cancel(false));
        monitoringFutures.clear();

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
