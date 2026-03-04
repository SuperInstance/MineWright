package com.minewright.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.minewright.coordination.TaskRebalancingManager.*;

/**
 * Handles task reassignment to new agents.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Validating reassignment prerequisites</li>
 *   <li>Executing task reassignment</li>
 *   <li>Updating workload tracking</li>
 *   <li>Notifying listeners of reassignment events</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class TaskReassigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskReassigner.class);

    private final CapabilityRegistry capabilityRegistry;
    private final WorkloadTracker workloadTracker;
    private final ContractNetManager contractNetManager;
    private final int maxReassignments;

    // Statistics
    private final AtomicInteger reassignedSuccessfully = new AtomicInteger(0);
    private final AtomicInteger reassignedFailed = new AtomicInteger(0);
    private final AtomicLong totalRebalancingTime = new AtomicLong(0);

    public TaskReassigner(CapabilityRegistry capabilityRegistry,
                         WorkloadTracker workloadTracker,
                         ContractNetManager contractNetManager,
                         int maxReassignments) {
        this.capabilityRegistry = capabilityRegistry;
        this.workloadTracker = workloadTracker;
        this.contractNetManager = contractNetManager;
        this.maxReassignments = maxReassignments;
    }

    /**
     * Reassigns a task to a new agent.
     *
     * @param task The task to reassign
     * @param newAgent New agent to assign task to
     * @param listener Optional listener for reassignment events
     * @return true if reassignment was successful
     */
    public boolean reassignTask(MonitoredTask task, UUID newAgent, RebalancingListener listener) {
        if (task == null || newAgent == null) {
            LOGGER.warn("Cannot reassign task: null task or agent");
            reassignedFailed.incrementAndGet();
            return false;
        }

        if (task.getReassignedCount() >= maxReassignments) {
            LOGGER.warn("Cannot reassign task {}: already reassigned {} times (max: {})",
                task.getTaskId(), task.getReassignedCount(), maxReassignments);
            reassignedFailed.incrementAndGet();
            notifyReassignmentFailed(listener, task.getTaskId(), task.getAssignedAgent(),
                RebalancingReason.TIMEOUT, "Max reassignments exceeded");
            return false;
        }

        UUID oldAgent = task.getAssignedAgent();

        // Verify new agent is capable and available
        if (capabilityRegistry != null && workloadTracker != null) {
            AgentCapability newCapability = capabilityRegistry.getCapability(newAgent);
            if (newCapability == null || !newCapability.isActive() || !newCapability.isAvailable()) {
                LOGGER.warn("Cannot reassign task {} to {}: agent not available",
                    task.getTaskId(), newAgent.toString().substring(0, 8));
                reassignedFailed.incrementAndGet();
                notifyReassignmentFailed(listener, task.getTaskId(), oldAgent,
                    RebalancingReason.AGENT_UNAVAILABLE, "New agent not available");
                return false;
            }

            // Check workload capacity
            if (!workloadTracker.isAvailable(newAgent)) {
                LOGGER.warn("Cannot reassign task {} to {}: agent at capacity",
                    task.getTaskId(), newAgent.toString().substring(0, 8));
                reassignedFailed.incrementAndGet();
                notifyReassignmentFailed(listener, task.getTaskId(), oldAgent,
                    RebalancingReason.AGENT_OVERLOADED, "New agent at capacity");
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
                task.getTaskId(), oldAgent.toString().substring(0, 8),
                newAgent.toString().substring(0, 8), task.getReassignedCount(), elapsed / 1000);

            // Notify listeners
            if (listener != null) {
                try {
                    // Determine reason from task state
                    RebalancingReason reason = task.isTimedOut() ? RebalancingReason.TIMEOUT :
                        task.isStuck() ? RebalancingReason.STUCK :
                        RebalancingReason.AGENT_UNAVAILABLE;
                    listener.onTaskReassigned(task.getTaskId(), oldAgent, newAgent, reason);
                } catch (Exception e) {
                    LOGGER.warn("Listener error in onTaskReassigned", e);
                }
            }

            return true;
        } else {
            reassignedFailed.incrementAndGet();
            notifyReassignmentFailed(listener, task.getTaskId(), oldAgent,
                RebalancingReason.AGENT_UNAVAILABLE, "Reassignment failed");
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
     * Selects the best replacement agent from capable agents.
     */
    public UUID selectBestReplacementAgent(RebalancingAssessment assessment) {
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

    /**
     * Notifies listeners of reassignment failure.
     */
    private void notifyReassignmentFailed(RebalancingListener listener, String taskId,
                                         UUID agent, RebalancingReason reason, String cause) {
        if (listener != null) {
            try {
                listener.onReassignmentFailed(taskId, reason, cause);
            } catch (Exception e) {
                LOGGER.warn("Listener error in onReassignmentFailed", e);
            }
        }
    }

    /**
     * Gets the number of successful reassignments.
     */
    public int getReassignedSuccessfully() {
        return reassignedSuccessfully.get();
    }

    /**
     * Gets the number of failed reassignments.
     */
    public int getReassignedFailed() {
        return reassignedFailed.get();
    }

    /**
     * Gets the total rebalancing time in nanoseconds.
     */
    public long getTotalRebalancingTime() {
        return totalRebalancingTime.get();
    }

    /**
     * Gets the average rebalancing time in milliseconds.
     *
     * @param totalReassignments Total number of reassignments
     * @return Average time in milliseconds
     */
    public double getAverageRebalancingTime(int totalReassignments) {
        return totalReassignments > 0
            ? (double) totalRebalancingTime.get() / totalReassignments / 1_000_000.0
            : 0.0;
    }
}
