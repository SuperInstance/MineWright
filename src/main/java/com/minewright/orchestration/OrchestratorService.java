package com.minewright.orchestration;

import com.minewright.MineWrightMod;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.ResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Central orchestration service for coordinating multiple MineWright agents.
 *
 * <p>Implements a hierarchical coordination model where one "Foreman" MineWright
 * receives high-level commands from the human player and distributes tasks
 * to worker agents.</p>
 *
 * <p><b>Architecture:</b></p>
 * <pre>
 * Human Player
 *      │
 *      ▼
 * ┌─────────────────┐
 * │  FOREMAN        │ ← OrchestratorService (this class)
 * │  MineWright     │
 * │   (Orchestrator)│
 * └────────┬────────┘
 *          │
 *    ┌─────┴─────┬─────────┐
 *    ▼           ▼         ▼
 * ┌──────┐   ┌──────┐   ┌──────┐
 * │Worker│   │Worker│   │Worker│
 * │Mine  │   │Mine  │   │Mine  │
 * │Wright│   │Wright│   │Wright│
 * └──────┘   └──────┘   └──────┘
 * </pre>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *   <li>Receive high-level commands from human player</li>
 *   <li>Decompose commands into distributable tasks</li>
 *   <li>Assign tasks to appropriate workers</li>
 *   <li>Monitor task progress and handle failures</li>
 *   <li>Rebalance work when workers finish or fail</li>
 *   <li>Report aggregate progress to human player</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class OrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorService.class);

    /**
     * Maximum time to wait for worker response before considering assignment failed.
     */
    private static final long ASSIGNMENT_TIMEOUT_MS = 30_000;

    /**
     * Maximum retries for a failed task before reporting overall failure.
     */
    private static final int MAX_TASK_RETRIES = 2;

    /**
     * Communication bus for inter-agent messaging.
     */
    private final AgentCommunicationBus communicationBus;

    /**
     * Map of plan IDs to their active task assignments.
     */
    private final Map<String, PlanExecution> activePlans;

    /**
     * Map of worker IDs to their current assignments.
     */
    private final Map<String, TaskAssignment> workerAssignments;

    /**
     * Registry of all available workers.
     */
    private final Map<String, WorkerInfo> workerRegistry;

    /**
     * Reference to the foreman entity (if any).
     */
    private volatile String foremanId;

    /**
     * Track message handlers for each agent to enable cleanup on unregister.
     */
    private final Map<String, java.util.function.Consumer<AgentMessage>> messageHandlers;

    /**
     * Creates a new OrchestratorService.
     */
    public OrchestratorService() {
        this.communicationBus = new AgentCommunicationBus();
        this.activePlans = new ConcurrentHashMap<>();
        this.workerAssignments = new ConcurrentHashMap<>();
        this.workerRegistry = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();

        LOGGER.info("OrchestratorService initialized");
    }

    /**
     * Registers a MineWright agent with the orchestration system.
     *
     * @param minewright The MineWright entity to register
     * @param role  The agent's role in the hierarchy
     */
    public void registerAgent(ForemanEntity minewright, AgentRole role) {
        String agentId = minewright.getEntityName();
        String agentName = minewright.getEntityName();

        // Register with communication bus
        communicationBus.registerAgent(agentId, agentName);

        if (role == AgentRole.FOREMAN) {
            // Only one foreman allowed
            if (foremanId != null && !foremanId.equals(agentId)) {
                LOGGER.warn("Replacing foreman: {} -> {}", foremanId, agentId);
            }
            this.foremanId = agentId;
            LOGGER.info("Registered FOREMAN: {}", agentName);
        } else {
            // Register as worker
            workerRegistry.put(agentId, new WorkerInfo(agentId, agentName, role));
            LOGGER.info("Registered WORKER: {} (role={})", agentName, role);
        }

        // Subscribe to messages from this agent and store handler for cleanup
        java.util.function.Consumer<AgentMessage> handler = message ->
            handleMessageFromAgent(agentId, message);
        communicationBus.subscribe(agentId, handler);
        messageHandlers.put(agentId, handler);
    }

    /**
     * Unregisters a Steve agent from the orchestration system.
     *
     * @param agentId Agent ID to unregister
     */
    public void unregisterAgent(String agentId) {
        // Clean up message handler subscription to prevent memory leak
        java.util.function.Consumer<AgentMessage> handler = messageHandlers.remove(agentId);
        if (handler != null) {
            communicationBus.unsubscribe(agentId, handler);
        }

        communicationBus.unregisterAgent(agentId);

        if (foremanId != null && foremanId.equals(agentId)) {
            foremanId = null;
            LOGGER.warn("Foreman unregistered: {}", agentId);
            // Elect new foreman from workers
            electNewForeman();
        } else {
            WorkerInfo removed = workerRegistry.remove(agentId);
            if (removed != null) {
                LOGGER.info("Worker unregistered: {}", agentId);
                // Reassign any tasks this worker had
                reassignWorkerTasks(agentId);
            }
        }
    }

    /**
     * Processes a high-level command from the human player.
     *
     * <p>The foreman receives this command, plans the work, and distributes
     * tasks to workers.</p>
     *
     * @param parsedResponse The LLM's parsed response containing tasks
     * @param availableSteves All available Steve agents
     * @return Plan ID for tracking progress
     */
    public String processHumanCommand(ResponseParser.ParsedResponse parsedResponse,
                                      Collection<ForemanEntity> availableSteves) {
        if (foremanId == null) {
            LOGGER.warn("No foreman available to process command");
            // Fall back to solo mode - assign to first available
            return processSoloCommand(parsedResponse, availableSteves);
        }

        String planId = UUID.randomUUID().toString().substring(0, 8);
        PlanExecution plan = new PlanExecution(
            planId,
            parsedResponse.getPlan(),
            parsedResponse.getTasks(),
            foremanId
        );

        activePlans.put(planId, plan);

        LOGGER.info("[Orchestrator] Processing command: {} ({} tasks, plan: {})",
            parsedResponse.getPlan(), parsedResponse.getTasks().size(), planId);

        // Distribute tasks to workers
        distributeTasks(plan, availableSteves);

        // Announce plan to all workers
        broadcastPlanAnnouncement(plan);

        return planId;
    }

    /**
     * Fallback for when no foreman is available.
     */
    private String processSoloCommand(ResponseParser.ParsedResponse parsedResponse,
                                      Collection<ForemanEntity> availableSteves) {
        LOGGER.info("[Orchestrator] No foreman - using solo mode");

        String planId = UUID.randomUUID().toString().substring(0, 8);
        PlanExecution plan = new PlanExecution(
            planId,
            parsedResponse.getPlan(),
            parsedResponse.getTasks(),
            "solo"
        );

        // Assign all tasks to first available worker
        if (!availableSteves.isEmpty()) {
            ForemanEntity firstSteve = availableSteves.iterator().next();
            for (Task task : parsedResponse.getTasks()) {
                TaskAssignment assignment = new TaskAssignment(
                    "solo", task, planId
                );
                assignment.assignTo(firstSteve.getEntityName());
                plan.addAssignment(assignment);
                workerAssignments.put(firstSteve.getEntityName(), assignment);
            }
        }

        activePlans.put(planId, plan);
        return planId;
    }

    /**
     * Distributes tasks from a plan to available workers.
     */
    private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
        List<Task> tasks = plan.getRemainingTasks();
        List<ForemanEntity> availableWorkers = availableSteves.stream()
            .filter(s -> !s.getEntityName().equals(foremanId))
            .filter(s -> !workerAssignments.containsKey(s.getEntityName()))
            .collect(Collectors.toList());

        LOGGER.info("[Orchestrator] Distributing {} tasks to {} available workers",
            tasks.size(), availableWorkers.size());

        int workerIndex = 0;
        for (Task task : tasks) {
            if (availableWorkers.isEmpty()) {
                // No more available workers - foreman takes remaining
                assignTaskToAgent(plan, task, foremanId);
            } else {
                // Round-robin assignment
                ForemanEntity worker = availableWorkers.get(workerIndex % availableWorkers.size());
                assignTaskToAgent(plan, task, worker.getEntityName());
                workerIndex++;
            }
        }
    }

    /**
     * Assigns a specific task to an agent.
     */
    private void assignTaskToAgent(PlanExecution plan, Task task, String agentId) {
        TaskAssignment assignment = new TaskAssignment(foremanId, task, plan.getPlanId());
        assignment.assignTo(agentId);

        plan.addAssignment(assignment);
        workerAssignments.put(agentId, assignment);

        // Send assignment message
        AgentMessage message = AgentMessage.taskAssignment(
            foremanId, "Foreman", agentId,
            task.getAction(), task.getParameters()
        );
        communicationBus.publish(message);

        LOGGER.info("[Orchestrator] Assigned task '{}' to {}", task.getAction(), agentId);
    }

    /**
     * Broadcasts plan announcement to all workers.
     */
    private void broadcastPlanAnnouncement(PlanExecution plan) {
        AgentMessage announcement = AgentMessage.broadcast(
            foremanId, "Foreman",
            String.format("New plan: %s (%d tasks)", plan.getDescription(), plan.getTotalTaskCount()),
            AgentMessage.Priority.NORMAL
        );
        announcement = new AgentMessage.Builder()
            .type(AgentMessage.Type.PLAN_ANNOUNCEMENT)
            .sender(foremanId, "Foreman")
            .recipient("*")
            .content(plan.getDescription())
            .payload("planId", plan.getPlanId())
            .payload("taskCount", plan.getTotalTaskCount())
            .priority(AgentMessage.Priority.NORMAL)
            .build();

        communicationBus.publish(announcement);
    }

    /**
     * Handles incoming messages from agents.
     */
    private void handleMessageFromAgent(String agentId, AgentMessage message) {
        LOGGER.debug("[Orchestrator] Message from {}: {}", agentId, message.getType());

        switch (message.getType()) {
            case TASK_PROGRESS:
                handleTaskProgress(agentId, message);
                break;

            case TASK_COMPLETE:
                handleTaskComplete(agentId, message);
                break;

            case TASK_FAILED:
                handleTaskFailed(agentId, message);
                break;

            case HELP_REQUEST:
                handleHelpRequest(agentId, message);
                break;

            case STATUS_REPORT:
                handleStatusReport(agentId, message);
                break;

            default:
                LOGGER.debug("[Orchestrator] Unhandled message type: {}", message.getType());
        }
    }

    /**
     * Handles task progress update from a worker.
     */
    private void handleTaskProgress(String workerId, AgentMessage message) {
        TaskAssignment assignment = workerAssignments.get(workerId);
        if (assignment != null) {
            int percent = message.getPayloadValue("percentComplete", 0);
            String status = message.getPayloadValue("status", "In progress");
            assignment.updateProgress(percent, status);

            LOGGER.debug("[Orchestrator] Task progress from {}: {}% - {}",
                workerId, percent, status);
        }
    }

    /**
     * Handles task completion from a worker.
     */
    private void handleTaskComplete(String workerId, AgentMessage message) {
        TaskAssignment assignment = workerAssignments.remove(workerId);
        if (assignment != null) {
            String result = message.getPayloadValue("result", "Completed");
            assignment.complete(result);

            // Update plan
            PlanExecution plan = activePlans.get(assignment.getParentPlanId());
            if (plan != null) {
                plan.markTaskComplete(assignment.getAssignmentId());
                checkPlanCompletion(plan);
            }

            LOGGER.info("[Orchestrator] Task completed by {}: {}", workerId, result);

            // Notify human player
            notifyHumanPlayer(String.format("%s completed: %s", workerId, result));
        }
    }

    /**
     * Handles task failure from a worker.
     */
    private void handleTaskFailed(String workerId, AgentMessage message) {
        TaskAssignment assignment = workerAssignments.remove(workerId);
        if (assignment != null) {
            String reason = message.getPayloadValue("result", "Unknown error");
            assignment.fail(reason);

            LOGGER.warn("[Orchestrator] Task failed by {}: {}", workerId, reason);

            // Check if we should retry
            if (assignment.getRetryCount() < MAX_TASK_RETRIES) {
                retryTask(assignment);
            } else {
                // Mark as failed in plan
                PlanExecution plan = activePlans.get(assignment.getParentPlanId());
                if (plan != null) {
                    plan.markTaskFailed(assignment.getAssignmentId());
                }

                notifyHumanPlayer(String.format("%s failed: %s", workerId, reason));
            }
        }
    }

    /**
     * Retries a failed task with a different worker.
     */
    private void retryTask(TaskAssignment assignment) {
        // Find a different available worker
        Optional<String> newWorker = workerRegistry.keySet().stream()
            .filter(id -> !id.equals(assignment.getAssignedWorkerId()))
            .filter(id -> !workerAssignments.containsKey(id))
            .findFirst();

        if (newWorker.isPresent()) {
            String newWorkerId = newWorker.get();
            assignment.reassign(newWorkerId, "Retry after failure");
            workerAssignments.put(newWorkerId, assignment);

            AgentMessage retryMessage = AgentMessage.taskAssignment(
                foremanId, "Foreman", newWorkerId,
                assignment.getTaskDescription(), assignment.getParameters()
            );
            communicationBus.publish(retryMessage);

            LOGGER.info("[Orchestrator] Retrying task with {} (attempt {})",
                newWorkerId, assignment.getRetryCount());
        } else {
            LOGGER.warn("[Orchestrator] No available worker for task retry");
        }
    }

    /**
     * Handles help request from a worker.
     */
    private void handleHelpRequest(String workerId, AgentMessage message) {
        String issue = message.getContent();
        LOGGER.warn("[Orchestrator] Help request from {}: {}", workerId, issue);

        // Notify human player
        notifyHumanPlayer(String.format("%s needs help: %s", workerId, issue));

        // Could implement automatic assistance here
    }

    /**
     * Handles status report from an agent.
     */
    private void handleStatusReport(String agentId, AgentMessage message) {
        LOGGER.debug("[Orchestrator] Status report from {}: {}", agentId, message.getContent());
        // Could track agent status for load balancing
    }

    /**
     * Checks if a plan is complete and notifies if so.
     */
    private void checkPlanCompletion(PlanExecution plan) {
        if (plan.isComplete()) {
            LOGGER.info("[Orchestrator] Plan complete: {}", plan.getDescription());

            String summary = plan.isSuccess()
                ? String.format("Plan completed successfully: %s", plan.getDescription())
                : String.format("Plan completed with failures: %d/%d tasks succeeded",
                    plan.getCompletedCount(), plan.getTotalTaskCount());

            notifyHumanPlayer(summary);
            activePlans.remove(plan.getPlanId());
        }
    }

    /**
     * Reassigns tasks when a worker is removed.
     */
    private void reassignWorkerTasks(String workerId) {
        TaskAssignment assignment = workerAssignments.remove(workerId);
        if (assignment != null && !assignment.isTerminal()) {
            LOGGER.info("[Orchestrator] Reassigning task from removed worker: {}", workerId);
            retryTask(assignment);
        }
    }

    /**
     * Elects a new foreman from available workers.
     */
    private void electNewForeman() {
        if (!workerRegistry.isEmpty()) {
            String newForeman = workerRegistry.keySet().iterator().next();
            WorkerInfo info = workerRegistry.remove(newForeman);
            this.foremanId = newForeman;

            LOGGER.info("[Orchestrator] Elected new foreman: {}", newForeman);

            // Notify all agents
            AgentMessage announcement = AgentMessage.broadcast(
                newForeman, newForeman,
                "I am now the foreman!",
                AgentMessage.Priority.HIGH
            );
            communicationBus.publish(announcement);
        }
    }

    /**
     * Notifies the human player of an event.
     *
     * <p>This sends a chat message to all players.</p>
     */
    private void notifyHumanPlayer(String message) {
        // Find foreman entity and use its chat method
        // This is a placeholder - actual implementation would need entity reference
        LOGGER.info("[Orchestrator → Human] {}", message);

        // Could also use Minecraft's chat system directly
        // For now, we log it and let the foreman handle in-game chat
    }

    /**
     * Gets the progress of a plan.
     *
     * @param planId Plan ID to check
     * @return Progress percentage (0-100), or -1 if plan not found
     */
    public int getPlanProgress(String planId) {
        PlanExecution plan = activePlans.get(planId);
        return plan != null ? plan.getProgressPercent() : -1;
    }

    /**
     * Gets all active plan IDs.
     */
    public Set<String> getActivePlanIds() {
        return Collections.unmodifiableSet(activePlans.keySet());
    }

    /**
     * Gets the communication bus for direct message access.
     */
    public AgentCommunicationBus getCommunicationBus() {
        return communicationBus;
    }

    /**
     * Shuts down the orchestrator service.
     */
    public void shutdown() {
        communicationBus.shutdown();
        activePlans.clear();
        workerAssignments.clear();
        workerRegistry.clear();
        messageHandlers.clear();
        foremanId = null;

        LOGGER.info("OrchestratorService shut down");
    }

    /**
     * Information about a registered worker.
     */
    private static class WorkerInfo {
        final String id;
        final String name;
        final AgentRole role;
        volatile Instant lastSeen;

        WorkerInfo(String id, String name, AgentRole role) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.lastSeen = Instant.now();
        }
    }

    /**
     * Represents an active plan execution with task tracking.
     */
    public static class PlanExecution {
        private final String planId;
        private final String description;
        private final List<Task> originalTasks;
        private final Map<String, TaskAssignment> assignments;
        private final String foremanId;
        private final Instant createdAt;

        public PlanExecution(String planId, String description, List<Task> tasks, String foremanId) {
            this.planId = planId;
            this.description = description;
            this.originalTasks = new ArrayList<>(tasks);
            this.assignments = new ConcurrentHashMap<>();
            this.foremanId = foremanId;
            this.createdAt = Instant.now();
        }

        public String getPlanId() { return planId; }
        public String getDescription() { return description; }
        public String getForemanId() { return foremanId; }
        public int getTotalTaskCount() { return originalTasks.size(); }

        public List<Task> getRemainingTasks() {
            return new ArrayList<>(originalTasks);
        }

        public void addAssignment(TaskAssignment assignment) {
            assignments.put(assignment.getAssignmentId(), assignment);
        }

        public void markTaskComplete(String assignmentId) {
            TaskAssignment assignment = assignments.get(assignmentId);
            if (assignment != null) {
                assignment.complete("Marked complete by orchestrator");
            }
        }

        public void markTaskFailed(String assignmentId) {
            TaskAssignment assignment = assignments.get(assignmentId);
            if (assignment != null) {
                assignment.fail("Marked failed by orchestrator");
            }
        }

        public int getCompletedCount() {
            return (int) assignments.values().stream()
                .filter(a -> a.getState() == TaskAssignment.State.COMPLETED)
                .count();
        }

        public int getFailedCount() {
            return (int) assignments.values().stream()
                .filter(a -> a.getState() == TaskAssignment.State.FAILED)
                .count();
        }

        public int getProgressPercent() {
            if (originalTasks.isEmpty()) return 100;
            int completed = getCompletedCount() + getFailedCount();
            return (completed * 100) / originalTasks.size();
        }

        public boolean isComplete() {
            return assignments.values().stream()
                .allMatch(a -> a.getState().isTerminal());
        }

        public boolean isSuccess() {
            return getFailedCount() == 0;
        }
    }
}
