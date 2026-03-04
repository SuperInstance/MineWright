package com.minewright.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.minewright.coordination.TaskRebalancingManager.*;

/**
 * Assesses whether tasks need rebalancing.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Evaluating rebalancing conditions (timeout, stuck, failure, etc.)</li>
 *   <li>Creating rebalancing assessments</li>
 *   <li>Finding capable replacement agents</li>
 *   <li>Determining rebalancing necessity</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class TaskRebalancingAssessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRebalancingAssessor.class);

    private final CapabilityRegistry capabilityRegistry;
    private final WorkloadTracker workloadTracker;
    private final ContractNetManager contractNetManager;

    public TaskRebalancingAssessor(CapabilityRegistry capabilityRegistry,
                                   WorkloadTracker workloadTracker,
                                   ContractNetManager contractNetManager) {
        this.capabilityRegistry = capabilityRegistry;
        this.workloadTracker = workloadTracker;
        this.contractNetManager = contractNetManager;
    }

    /**
     * Assesses a monitored task for rebalancing.
     *
     * @param task The task to assess
     * @param listener Optional listener for assessment events
     * @return Rebalancing assessment
     */
    public RebalancingAssessment assessTask(MonitoredTask task, RebalancingListener listener) {
        if (task == null) {
            return new RebalancingAssessment(null, null, null, false, null,
                "Task is null", List.of());
        }

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
        if (workloadTracker != null && workloadTracker.isRegistered(currentAgent)) {
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
        } else if (workloadTracker != null) {
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
                contractNetManager != null ? contractNetManager.getNegotiation(task.getAnnouncementId()) : null;

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
}
