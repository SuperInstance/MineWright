package com.minewright.entity;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.dialogue.ProactiveDialogueManager;
import com.minewright.hivemind.CloudflareClient;
import com.minewright.orchestration.AgentMessage;
import com.minewright.orchestration.AgentRole;
import com.minewright.orchestration.OrchestratorService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles communication and orchestration for a ForemanEntity.
 *
 * <p>This class manages all communication aspects of a ForemanEntity:
 * <ul>
 *   <li>Orchestrator registration and role management</li>
 *   <li>Inter-agent messaging (task assignments, status queries)</li>
 *   <li>Task progress reporting</li>
 *   <li>Tactical decision execution (Hive Mind)</li>
 *   <li>Proactive dialogue triggers</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is designed to be called from the
 * Minecraft server thread only (single-threaded execution).</p>
 *
 * @since 1.0.0
 */
public class CommunicationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationHandler.class);

    private final ForemanEntity entity;
    private final EntityState state;

    /**
     * Tick counter used for periodic operations (e.g., progress reporting).
     */
    private int tickCounter = 0;

    /**
     * Creates a new CommunicationHandler.
     *
     * @param entity The ForemanEntity this handler belongs to
     * @param state The entity's state
     */
    public CommunicationHandler(ForemanEntity entity, EntityState state) {
        this.entity = entity;
        this.state = state;
    }

    /**
     * Registers this crew member with the orchestrator service.
     * First agent becomes FOREMAN, subsequent agents become WORKERS.
     */
    public void registerWithOrchestrator() {
        OrchestratorService orchestrator = state.getOrchestrator();
        if (orchestrator == null) {
            LOGGER.warn("[{}] Orchestrator service not available", state.getEntityName());
            return;
        }

        // Determine role based on existing agents
        int activeCount = MineWrightMod.getCrewManager().getActiveCount();

        if (activeCount == 1) {
            // First agent is the foreman
            state.setRole(AgentRole.FOREMAN);
            LOGGER.info("[{}] Registering as FOREMAN (first agent)", state.getEntityName());
        } else {
            // Subsequent agents are workers
            state.setRole(AgentRole.WORKER);
            LOGGER.info("[{}] Registering as WORKER (agent #{})", state.getEntityName(), activeCount);
        }

        orchestrator.registerAgent(entity, state.getRole());
        state.getRegisteredWithOrchestrator().set(true);

        // Send chat message about role
        if (state.getRole() == AgentRole.FOREMAN) {
            entity.sendChatMessage("I am the Foreman! Ready to coordinate.");
        } else {
            entity.sendChatMessage("Ready to work! Awaiting Foreman's instructions.");
        }
    }

    /**
     * Processes messages from the communication bus.
     */
    public void processMessages() {
        OrchestratorService orchestrator = state.getOrchestrator();
        if (orchestrator == null) return;

        // Poll for new messages
        AgentMessage message;
        while ((message = orchestrator.getCommunicationBus().poll(state.getEntityName())) != null) {
            handleMessage(message, orchestrator);
        }
    }

    /**
     * Handles a single message from the communication bus.
     *
     * @param message The message to handle
     * @param orchestrator The orchestrator service
     */
    private void handleMessage(AgentMessage message, OrchestratorService orchestrator) {
        LOGGER.debug("[{}] Received message: {} from {}",
            state.getEntityName(), message.getType(), message.getSenderName());

        switch (message.getType()) {
            case TASK_ASSIGNMENT:
                handleTaskAssignment(message, orchestrator);
                break;

            case PLAN_ANNOUNCEMENT:
                handlePlanAnnouncement(message);
                break;

            case BROADCAST:
                handleBroadcast(message);
                break;

            case STATUS_QUERY:
                handleStatusQuery(message, orchestrator);
                break;

            default:
                LOGGER.debug("[{}] Unhandled message type: {}", state.getEntityName(), message.getType());
        }
    }

    /**
     * Handles task assignment from the foreman.
     *
     * @param message The task assignment message
     * @param orchestrator The orchestrator service
     */
    private void handleTaskAssignment(AgentMessage message, OrchestratorService orchestrator) {
        if (state.getRole() == AgentRole.FOREMAN) {
            LOGGER.debug("[{}] Foreman ignoring task assignment", state.getEntityName());
            return;
        }

        String taskDescription = message.getPayloadValue("taskDescription", "Unknown task");
        LOGGER.info("[{}] Received task assignment: {}", state.getEntityName(), taskDescription);

        // Extract task parameters
        Task task = new Task(taskDescription, message.getPayload());

        // Execute the task
        entity.sendChatMessage("Accepting task: " + taskDescription);
        state.setCurrentTaskId(message.getMessageId());

        // Add to action queue
        ActionExecutor actionExecutor = state.getActionExecutor();
        actionExecutor.queueTask(task);

        // Send acknowledgment
        AgentMessage ack = AgentMessage.taskProgress(
            state.getEntityName(), state.getEntityName(),
            message.getSenderId(),
            message.getMessageId(),
            0,
            "Task accepted"
        );
        orchestrator.getCommunicationBus().publish(ack);
    }

    /**
     * Handles plan announcement from the foreman.
     *
     * @param message The plan announcement message
     */
    private void handlePlanAnnouncement(AgentMessage message) {
        String planId = message.getPayloadValue("planId", "?");
        int taskCount = message.getPayloadValue("taskCount", 0);
        LOGGER.info("[{}] Plan announced: {} ({} tasks)", state.getEntityName(), planId, taskCount);
    }

    /**
     * Handles broadcast messages.
     *
     * @param message The broadcast message
     */
    private void handleBroadcast(AgentMessage message) {
        LOGGER.info("[{}] Broadcast from {}: {}",
            state.getEntityName(), message.getSenderName(), message.getContent());
    }

    /**
     * Handles status query from foreman.
     *
     * @param message The status query message
     * @param orchestrator The orchestrator service
     */
    private void handleStatusQuery(AgentMessage message, OrchestratorService orchestrator) {
        ActionExecutor actionExecutor = state.getActionExecutor();
        String status = String.format("%s (%s) - %s",
            state.getEntityName(),
            state.getRole().getDisplayName(),
            actionExecutor.isExecuting() ? "Working" : "Idle"
        );

        AgentMessage response = new AgentMessage.Builder()
            .type(AgentMessage.Type.STATUS_REPORT)
            .sender(state.getEntityName(), state.getEntityName())
            .recipient(message.getSenderId())
            .content(status)
            .payload("idle", !actionExecutor.isExecuting())
            .priority(AgentMessage.Priority.NORMAL)
            .build();

        orchestrator.getCommunicationBus().publish(response);
    }

    /**
     * Reports task progress if working on a task.
     */
    public void reportTaskProgress() {
        String currentTaskId = state.getCurrentTaskId();
        ActionExecutor actionExecutor = state.getActionExecutor();

        if (currentTaskId == null || !actionExecutor.isExecuting()) {
            return;
        }

        // Report progress every 100 ticks (5 seconds)
        tickCounter++;
        if (tickCounter % 100 != 0) {
            return;
        }

        // Calculate progress based on action state
        int progress = actionExecutor.getCurrentActionProgress();
        if (progress > state.getCurrentTaskProgress()) {
            state.setCurrentTaskProgress(progress);

            // Send progress update to foreman
            OrchestratorService orchestrator = state.getOrchestrator();
            if (orchestrator != null && state.getRole() != AgentRole.FOREMAN) {
                AgentMessage progressMsg = AgentMessage.taskProgress(
                    state.getEntityName(), state.getEntityName(),
                    "foreman", // Send to foreman
                    currentTaskId,
                    progress,
                    "In progress"
                );
                orchestrator.getCommunicationBus().publish(progressMsg);
            }
        }
    }

    /**
     * Checks tactical situation using Cloudflare edge for fast reflexes.
     */
    public void checkTacticalSituation() {
        Level level = entity.level();
        if (level == null || entity.getBoundingBox() == null) {
            return;
        }

        // Get nearby entities
        List<Entity> nearbyEntities = level.getEntitiesOfClass(
            Entity.class,
            entity.getBoundingBox().inflate(16.0) // 16 block radius
        );

        // Get tactical decision from edge
        CloudflareClient.TacticalDecision decision =
            state.getTacticalService().checkTactical(entity, nearbyEntities);

        // Execute decision if action required
        if (decision != null && decision.requiresAction() && !decision.isFallback) {
            executeTacticalDecision(decision);
        }
    }

    /**
     * Executes a tactical decision from the edge.
     *
     * @param decision The tactical decision to execute
     */
    private void executeTacticalDecision(CloudflareClient.TacticalDecision decision) {
        LOGGER.debug("[{}] Executing tactical decision: {} ({})",
            state.getEntityName(), decision.action, decision.reasoning);

        ActionExecutor actionExecutor = state.getActionExecutor();
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();

        switch (decision.action) {
            case "flee" -> {
                // Stop current action and move away from threat
                if (actionExecutor != null && actionExecutor.isExecuting()) {
                    actionExecutor.stopCurrentAction();
                }
                // Trigger dialogue about danger
                if (dialogueManager != null) {
                    dialogueManager.forceComment("danger", decision.reasoning);
                }
            }
            case "attack" -> {
                // Combat is handled by existing combat action
                // This is more of an awareness/decision notification
            }
            case "dodge" -> {
                // Quick movement adjustment - handled by pathfinding
            }
            case "shield" -> {
                // Defensive posture (could activate shield if equipped)
            }
            case "stop" -> {
                // Emergency stop (lava, cliff, etc.)
                if (actionExecutor != null) {
                    actionExecutor.stopCurrentAction();
                }
                if (dialogueManager != null) {
                    dialogueManager.forceComment("danger", decision.reasoning);
                }
            }
            default -> {
                // Unknown action - log and continue
                LOGGER.debug("[{}] Unknown tactical action: {}",
                    state.getEntityName(), decision.action);
            }
        }
    }

    /**
     * Sends a message to another agent or broadcasts to all.
     *
     * @param message The message to send
     */
    public void sendMessage(AgentMessage message) {
        OrchestratorService orchestrator = state.getOrchestrator();
        if (orchestrator != null) {
            orchestrator.getCommunicationBus().publish(message);
        }
    }

    /**
     * Marks current task as complete.
     *
     * @param result The task completion result
     */
    public void completeCurrentTask(String result) {
        String currentTaskId = state.getCurrentTaskId();
        OrchestratorService orchestrator = state.getOrchestrator();
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();

        if (currentTaskId != null && orchestrator != null && state.getRole() != AgentRole.FOREMAN) {
            AgentMessage completeMsg = AgentMessage.taskComplete(
                state.getEntityName(), state.getEntityName(),
                "foreman",
                currentTaskId,
                true,
                result
            );
            orchestrator.getCommunicationBus().publish(completeMsg);
            entity.sendChatMessage("Task completed: " + result);
            state.setCurrentTaskId(null);
            state.setCurrentTaskProgress(0);

            // Trigger proactive dialogue for task completion
            if (dialogueManager != null) {
                dialogueManager.onTaskCompleted(result);
            }
        }
    }

    /**
     * Marks current task as failed.
     *
     * @param reason The failure reason
     */
    public void failCurrentTask(String reason) {
        String currentTaskId = state.getCurrentTaskId();
        OrchestratorService orchestrator = state.getOrchestrator();
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();

        if (currentTaskId != null && orchestrator != null && state.getRole() != AgentRole.FOREMAN) {
            AgentMessage failMsg = AgentMessage.taskComplete(
                state.getEntityName(), state.getEntityName(),
                "foreman",
                currentTaskId,
                false,
                reason
            );
            orchestrator.getCommunicationBus().publish(failMsg);
            entity.sendChatMessage("Task failed: " + reason);
            state.setCurrentTaskId(null);
            state.setCurrentTaskProgress(0);

            // Trigger proactive dialogue for task failure
            if (dialogueManager != null) {
                dialogueManager.onTaskFailed(currentTaskId, reason);
            }
        }
    }

    /**
     * Notifies the dialogue manager that a task was completed.
     *
     * @param taskDescription Description of the completed task
     */
    public void notifyTaskCompleted(String taskDescription) {
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();
        if (dialogueManager != null) {
            dialogueManager.onTaskCompleted(taskDescription);
        }
    }

    /**
     * Notifies the dialogue manager that a task failed.
     *
     * @param taskDescription Description of the failed task
     * @param reason Reason for failure
     */
    public void notifyTaskFailed(String taskDescription, String reason) {
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();
        if (dialogueManager != null) {
            dialogueManager.onTaskFailed(taskDescription, reason);
        }
    }

    /**
     * Notifies the dialogue manager that crew member is stuck on a task.
     *
     * @param taskDescription Description of the task crew member is stuck on
     */
    public void notifyTaskStuck(String taskDescription) {
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();
        if (dialogueManager != null) {
            dialogueManager.onTaskStuck(taskDescription);
        }
    }

    /**
     * Notifies the dialogue manager of a milestone achievement.
     *
     * @param milestone Description of the milestone
     */
    public void notifyMilestone(String milestone) {
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();
        if (dialogueManager != null) {
            dialogueManager.onMilestoneReached(milestone);
        }
    }

    /**
     * Forces an immediate comment from crew member, bypassing cooldowns.
     *
     * @param triggerType The type of event (e.g., "danger", "discovery", "achievement")
     * @param context Description of what happened
     */
    public void forceComment(String triggerType, String context) {
        ProactiveDialogueManager dialogueManager = state.getDialogueManager();
        if (dialogueManager != null) {
            dialogueManager.forceComment(triggerType, context);
        }
    }
}
