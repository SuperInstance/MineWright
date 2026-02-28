package com.minewright.integration;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.BlackboardEntry;
import com.minewright.blackboard.KnowledgeArea;
import com.minewright.communication.AgentMessage;
import com.minewright.communication.CommunicationBus;
import com.minewright.entity.ForemanEntity;
import com.minewright.event.EventBus;
import com.minewright.llm.ResponseParser;
import com.minewright.llm.TaskPlanner;
import com.minewright.orchestration.OrchestratorService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Integration hooks for connecting new systems to existing code.
 *
 * <p><b>Purpose:</b></p>
 * <p>This class provides integration points for hooking the new integration
 * layer into existing code without breaking existing functionality. All hooks
 * are optional and gracefully degrade when systems are not available.</p>
 *
 * <p><b>Design Philosophy:</b></p>
 * <ul>
 *   <li><b>Non-Breaking:</b> All hooks are optional and safe to call</li>
 *   <li><b>Backward Compatible:</b> Existing code continues to work unchanged</li>
 *   <li><b>Graceful Degradation:</b> Systems fail safely when unavailable</li>
 *   <li><b>Minimal Changes:</b> Small, focused integration points</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // In ForemanEntity
 * IntegrationHooks.onForemanSpawned(this);
 *
 * // In ActionExecutor
 * IntegrationHooks.onTaskStarted(task, foreman);
 * IntegrationHooks.onTaskCompleted(task, true, foreman);
 *
 * // In TaskPlanner
 * List&lt;Task&gt; tasks = IntegrationHooks.planTasksWithOrchestrator(foreman, command);
 * </pre>
 *
 * @since 1.6.0
 */
public final class IntegrationHooks {

    private static final Logger LOGGER = TestLogger.getLogger(IntegrationHooks.class);

    // ------------------------------------------------------------------------
    // Singleton Orchestrator Reference
    // ------------------------------------------------------------------------

    private static volatile SteveOrchestrator orchestrator;
    private static volatile boolean initialized = false;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------

    /**
     * Initializes the integration layer.
     *
     * <p>This should be called once during mod initialization to set up
     * all subsystems. Safe to call multiple times.</p>
     *
     * @return The initialized orchestrator
     */
    public static SteveOrchestrator initialize() {
        if (initialized) {
            return getOrchestrator().orElse(null);
        }

        synchronized (IntegrationHooks.class) {
            if (initialized) {
                return getOrchestrator().orElse(null);
            }

            LOGGER.info("Initializing MineWright integration layer...");

            orchestrator = SystemFactory.createOrchestrator();
            initialized = true;

            LOGGER.info("Integration layer initialized successfully");

            return orchestrator;
        }
    }

    /**
     * Gets the orchestrator if initialized.
     *
     * @return Optional containing the orchestrator, or empty
     */
    public static Optional<SteveOrchestrator> getOrchestrator() {
        return Optional.ofNullable(orchestrator);
    }

    /**
     * Checks if the integration layer is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized && orchestrator != null;
    }

    // ------------------------------------------------------------------------
    // ForemanEntity Hooks
    // ------------------------------------------------------------------------

    /**
     * Called when a ForemanEntity is spawned.
     *
     * <p>Registers the agent with subsystems:</p>
     * <ul>
     *   <li>CommunicationBus for message routing</li>
     *   <li>Blackboard for knowledge sharing</li>
     *   <li>Orchestrator for task coordination</li>
     * </ul>
     *
     * @param foreman The spawned foreman entity
     */
    public static void onForemanSpawned(ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        UUID agentId = foreman.getUUID();
        String entityName = foreman.getEntityName();

        LOGGER.debug("[IntegrationHooks] Foreman spawned: {}", entityName);

        // Register with communication bus
        CommunicationBus commBus = orchestrator.getCommunicationBus();
        if (commBus != null) {
            commBus.register(agentId, message -> {
                // Handle incoming messages
                handleAgentMessage(foreman, message);
            });
            LOGGER.debug("[IntegrationHooks] Registered {} with communication bus", entityName);
        }

        // Post spawn event to blackboard
        Blackboard blackboard = orchestrator.getBlackboard();
        if (blackboard != null) {
            BlackboardEntry<String> entry = BlackboardEntry.createFact(
                "agent_spawned_" + agentId,
                entityName,
                agentId
            );
            blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        }

        // The existing OrchestratorService handles registration
        // This is just additional integration setup
    }

    /**
     * Called when a ForemanEntity is removed.
     *
     * @param foreman The foreman entity being removed
     */
    public static void onForemanRemoved(ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        UUID agentId = foreman.getUUID();
        String entityName = foreman.getEntityName();

        LOGGER.debug("[IntegrationHooks] Foreman removed: {}", entityName);

        // Unregister from communication bus
        CommunicationBus commBus = orchestrator.getCommunicationBus();
        if (commBus != null) {
            commBus.unregister(agentId);
        }

        // Post removal event to blackboard
        Blackboard blackboard = orchestrator.getBlackboard();
        if (blackboard != null) {
            blackboard.remove(KnowledgeArea.AGENT_STATUS, "agent_spawned_" + agentId);
        }
    }

    /**
     * Called each tick for the foreman entity.
     *
     * <p>Processes messages and updates subsystems.</p>
     *
     * @param foreman The foreman entity
     */
    public static void onForemanTick(ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        // Tick the communication bus to process messages
        CommunicationBus commBus = orchestrator.getCommunicationBus();
        if (commBus != null) {
            commBus.tick();
        }

        // Update blackboard with current status
        updateForemanStatus(foreman);
    }

    // ------------------------------------------------------------------------
    // ActionExecutor Hooks
    // ------------------------------------------------------------------------

    /**
     * Called when a task is queued for execution.
     *
     * @param task    The task being queued
     * @param foreman The foreman entity
     */
    public static void onTaskQueued(Task task, ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        LOGGER.debug("[IntegrationHooks] Task queued: {} for {}",
            task.getAction(), foreman.getEntityName());

        // Post to blackboard
        Blackboard blackboard = orchestrator.getBlackboard();
        if (blackboard != null) {
            BlackboardEntry<Task> entry = BlackboardEntry.createFact(
                "task_queued_" + task.hashCode(),
                task,
                foreman.getUUID()
            );
            blackboard.post(KnowledgeArea.TASKS, entry);
        }
    }

    /**
     * Called when a task starts execution.
     *
     * @param task    The task starting
     * @param foreman The foreman entity
     */
    public static void onTaskStarted(Task task, ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        LOGGER.debug("[IntegrationHooks] Task started: {} for {}",
            task.getAction(), foreman.getEntityName());

        // Update blackboard
        Blackboard blackboard = orchestrator.getBlackboard();
        if (blackboard != null) {
            blackboard.post(KnowledgeArea.TASKS,
                BlackboardEntry.createFact("task_active_" + task.hashCode(), task, foreman.getUUID()));
        }
    }

    /**
     * Called when a task completes (success or failure).
     *
     * <p>Updates skill library with learning data and posts to blackboard.</p>
     *
     * @param task    The completed task
     * @param success Whether the task succeeded
     * @param foreman The foreman entity
     */
    public static void onTaskCompleted(Task task, boolean success, ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        LOGGER.debug("[IntegrationHooks] Task completed: {} for {} - {}",
            task.getAction(), foreman.getEntityName(), success ? "SUCCESS" : "FAILED");

        // Update skill library
        orchestrator.onTaskComplete(task, success);

        // Update blackboard
        Blackboard blackboard = orchestrator.getBlackboard();
        if (blackboard != null) {
            blackboard.remove(KnowledgeArea.TASKS, "task_active_" + task.hashCode());

            BlackboardEntry<Boolean> entry = BlackboardEntry.createFact(
                "task_complete_" + task.hashCode(),
                success,
                foreman.getUUID()
            );
            blackboard.post(KnowledgeArea.TASKS, entry);
        }

        // Notify foreman for dialogue
        if (success) {
            foreman.notifyTaskCompleted(task.getAction());
        } else {
            foreman.notifyTaskFailed(task.getAction(), "Execution failed");
        }
    }

    /**
     * Called when a task fails with an error.
     *
     * @param task    The failed task
     * @param error   The error message
     * @param foreman The foreman entity
     */
    public static void onTaskFailed(Task task, String error, ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        LOGGER.warn("[IntegrationHooks] Task failed: {} for {} - {}",
            task.getAction(), foreman.getEntityName(), error);

        orchestrator.onTaskFailed(task, error);

        // Notify foreman
        foreman.notifyTaskFailed(task.getAction(), error);
    }

    // ------------------------------------------------------------------------
    // TaskPlanner Hooks
    // ------------------------------------------------------------------------

    /**
     * Plans tasks using the orchestrator if available.
     *
     * <p>This hook allows the orchestrator to intercept planning requests
     * and apply skill learning, cascade routing, and prioritization.</p>
     *
     * @param foreman The foreman entity
     * @param command The user command
     * @return CompletableFuture with planned tasks, or empty if orchestrator not available
     */
    public static Optional<CompletableFuture<List<Task>>> planTasksWithOrchestrator(
        ForemanEntity foreman,
        String command
    ) {
        if (!isInitialized()) {
            return Optional.empty();
        }

        LOGGER.debug("[IntegrationHooks] Planning tasks via orchestrator: {}", command);

        return Optional.of(orchestrator.processCommand(foreman, command));
    }

    /**
     * Legacy planning fallback when orchestrator is not available.
     *
     * @param foreman The foreman entity
     * @param command The user command
     * @return Planned tasks using legacy TaskPlanner
     */
    public static ResponseParser.ParsedResponse planTasksLegacy(ForemanEntity foreman, String command) {
        TaskPlanner planner = new TaskPlanner();
        return planner.planTasks(foreman, command);
    }

    // ------------------------------------------------------------------------
    // Communication Hooks
    // ------------------------------------------------------------------------

    /**
     * Handles an incoming message for a foreman entity.
     *
     * @param foreman The target foreman
     * @param message The incoming message
     */
    private static void handleAgentMessage(ForemanEntity foreman, AgentMessage message) {
        String entityName = foreman.getEntityName();

        LOGGER.debug("[IntegrationHooks] Message for {}: {} from {}",
            entityName, message.type(), message.senderId());

        // Handle task requests
        if (message.type() == AgentMessage.MessageType.TASK_REQUEST) {
            handleTaskAssignment(foreman, message);
        }

        // Handle queries
        else if (message.type() == AgentMessage.MessageType.QUERY) {
            handleStatusQuery(foreman, message);
        }

        // Handle coordination messages (broadcast-like)
        else if (message.type() == AgentMessage.MessageType.COORDINATION) {
            handleBroadcast(foreman, message);
        }

        // Default handler for other message types
        else {
            LOGGER.debug("[IntegrationHooks] Unhandled message type: {}", message.type());
        }
    }

    /**
     * Handles a task assignment message.
     */
    private static void handleTaskAssignment(ForemanEntity foreman, AgentMessage message) {
        String taskDescription = message.content();
        Map<String, Object> payload = message.payload();

        Task task = new Task(taskDescription, payload);

        LOGGER.info("[IntegrationHooks] Task assigned to {}: {}",
            foreman.getEntityName(), taskDescription);

        // Queue the task
        ActionExecutor executor = foreman.getActionExecutor();
        if (executor != null) {
            executor.queueTask(task);

            // Send acknowledgment
            if (isInitialized()) {
                AgentMessage ack = new AgentMessage.Builder()
                    .sender(foreman.getUUID())
                    .recipient(message.senderId())
                    .type(AgentMessage.MessageType.RESPONSE)
                    .content("Task accepted: " + taskDescription)
                    .build();

                orchestrator.sendMessage(ack);
            }
        }
    }

    /**
     * Handles a status query message.
     */
    private static void handleStatusQuery(ForemanEntity foreman, AgentMessage message) {
        ActionExecutor executor = foreman.getActionExecutor();
        boolean isExecuting = executor != null && executor.isExecuting();

        String status = String.format("%s - %s",
            foreman.getEntityName(),
            isExecuting ? "Working" : "Idle"
        );

        AgentMessage response = new AgentMessage.Builder()
            .sender(foreman.getUUID())
            .recipient(message.senderId())
            .type(AgentMessage.MessageType.STATUS_UPDATE)
            .content(status)
            .payload("idle", !isExecuting)
            .build();

        if (isInitialized()) {
            orchestrator.sendMessage(response);
        }
    }

    /**
     * Handles a broadcast message.
     */
    private static void handleBroadcast(ForemanEntity foreman, AgentMessage message) {
        LOGGER.info("[IntegrationHooks] Broadcast from {}: {}",
            message.senderId(), message.content());

        // Could trigger dialogue or other reactions here
        foreman.forceComment("broadcast", message.content());
    }

    // ------------------------------------------------------------------------
    // Blackboard Hooks
    // ------------------------------------------------------------------------

    /**
     * Updates the blackboard with foreman status.
     *
     * @param foreman The foreman entity
     */
    private static void updateForemanStatus(ForemanEntity foreman) {
        if (!isInitialized()) {
            return;
        }

        Blackboard blackboard = orchestrator.getBlackboard();
        if (blackboard == null) {
            return;
        }

        UUID agentId = foreman.getUUID();
        ActionExecutor executor = foreman.getActionExecutor();

        // Update current status
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("entityName", foreman.getEntityName());
        status.put("isExecuting", executor != null && executor.isExecuting());
        status.put("blockX", foreman.getBlockX());
        status.put("blockY", foreman.getBlockY());
        status.put("blockZ", foreman.getBlockZ());
        status.put("health", foreman.getHealth());
        status.put("role", foreman.getRole().name());

        BlackboardEntry<Map<String, Object>> entry = BlackboardEntry.createFact(
            "agent_status_" + agentId,
            status,
            agentId
        );

        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
    }

    // ------------------------------------------------------------------------
    // Event Bus Hooks
    // ------------------------------------------------------------------------

    /**
     * Subscribes orchestrator to event bus if available.
     *
     * @param eventBus The event bus
     */
    public static void subscribeToEvents(EventBus eventBus) {
        if (!isInitialized() || eventBus == null) {
            return;
        }

        LOGGER.debug("[IntegrationHooks] Subscribing orchestrator to event bus");

        // Subscribe to action completion events
        eventBus.subscribe(com.minewright.event.ActionCompletedEvent.class, event -> {
            onActionCompletedEvent(event);
        });

        // Subscribe to action started events
        eventBus.subscribe(com.minewright.event.ActionStartedEvent.class, event -> {
            onActionStartedEvent(event);
        });
    }

    /**
     * Handles action completion events from the event bus.
     */
    private static void onActionCompletedEvent(com.minewright.event.ActionCompletedEvent event) {
        LOGGER.debug("[IntegrationHooks] Action completed: {}", event.getActionName());

        // Update skill library with action outcome
        // This enables learning from action-level feedback
    }

    /**
     * Handles action started events from the event bus.
     */
    private static void onActionStartedEvent(com.minewright.event.ActionStartedEvent event) {
        LOGGER.debug("[IntegrationHooks] Action started: {}", event.getActionName());
    }

    // ------------------------------------------------------------------------
    // Shutdown
    // ------------------------------------------------------------------------

    /**
     * Shuts down the integration layer.
     *
     * <p>Cleans up resources and stops all subsystems.</p>
     */
    public static void shutdown() {
        LOGGER.info("Shutting down integration layer...");

        if (orchestrator != null) {
            CommunicationBus commBus = orchestrator.getCommunicationBus();
            if (commBus != null) {
                commBus.shutdown();
            }

            TaskPlanner planner = orchestrator.getTaskPlanner();
            if (planner != null) {
                planner.shutdown();
            }
        }

        initialized = false;
        orchestrator = null;

        LOGGER.info("Integration layer shut down");
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Gets system health if initialized.
     *
     * @return Optional health report, or empty if not initialized
     */
    public static Optional<String> getHealthReport() {
        if (!isInitialized()) {
            return Optional.empty();
        }

        SystemHealthMonitor monitor = new SystemHealthMonitor(orchestrator);
        return Optional.of(monitor.generateHealthReport());
    }
}
