package com.minewright.entity;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.dialogue.ProactiveDialogueManager;
import com.minewright.hivemind.CloudflareClient;
import com.minewright.hivemind.TacticalDecisionService;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ForemanMemory;
import com.minewright.orchestration.AgentMessage;
import com.minewright.orchestration.AgentRole;
import com.minewright.orchestration.OrchestratorService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A MineWright crew member entity that autonomously executes tasks in Minecraft.
 *
 * <p><b>Overview:</b></p>
 * <p>The ForemanEntity represents an AI-controlled crew member that can plan and execute
 * complex tasks based on natural language commands from the player. These entities
 * use Large Language Models (LLMs) to understand commands, break them down into
 * actionable steps, and coordinate with other crew members through the orchestration system.</p>
 *
 * <p><b>Key Capabilities:</b></p>
 * <ul>
 *   <li>Execute natural language commands via LLM-powered planning</li>
 *   <li>Perform actions: mining, building, pathfinding, combat, crafting, gathering</li>
 *   <li>Coordinate with multiple crew members through orchestration</li>
 *   <li>Provide proactive dialogue about task progress and discoveries</li>
 *   <li>Use fast tactical decision-making via Cloudflare Edge (Hive Mind)</li>
 *   <li>Fly and become invulnerable for building tasks</li>
 * </ul>
 *
 * <p><b>Architecture:</b></p>
 * <pre>
 * ForemanEntity
 * ├── ActionExecutor - Queue and execute tasks tick-by-tick
 * ├── TaskPlanner - Async LLM calls for planning
 * ├── ForemanMemory - Task history and conversation context
 * ├── CompanionMemory - Player preferences and learning
 * ├── ProactiveDialogueManager - Contextual commentary
 * ├── OrchestratorService - Multi-agent coordination
 * └── TacticalDecisionService - Fast reflex decisions via edge
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Entity operations are single-threaded on the Minecraft server thread</li>
 *   <li>{@code ConcurrentLinkedQueue} for thread-safe message passing</li>
 *   <li>{@code AtomicBoolean} for orchestrator registration flag</li>
 *   <li>{@code volatile} for task progress visibility across threads</li>
 *   <li>LLM calls are async and execute on separate thread pools</li>
 * </ul>
 *
 * <p><b>State Management:</b></p>
 * <p>The entity maintains state through:</p>
 * <ul>
 *   <li>{@link AgentRole} - SOLO, FOREMAN, or WORKER in orchestration</li>
 *   <li>{@link ActionExecutor} state machine - IDLE, PLANNING, EXECUTING, etc.</li>
 *   <li>Current task tracking with progress reporting</li>
 *   <li>Memory systems that persist across server restarts</li>
 * </ul>
 *
 * <p><b>Lifecycle:</b></p>
 * <ol>
 *   <li>Spawned via command or GUI</li>
 *   <li>Registers with orchestrator on first tick</li>
 *   <li>Receives commands from player (GUI or chat)</li>
 *   <li>Plans tasks asynchronously (non-blocking)</li>
 *   <li>Executes actions tick-by-tick</li>
 *   <li>Reports progress and completion</li>
 *   <li>Persists state to NBT on world save</li>
 * </ol>
 *
 * @see ActionExecutor
 * @see TaskPlanner
 * @see OrchestratorService
 * @see com.minewright.action.actions.BaseAction
 *
 * @since 1.0.0
 */
public class ForemanEntity extends PathfinderMob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForemanEntity.class);
    private static final EntityDataAccessor<String> ENTITY_NAME =
        SynchedEntityData.defineId(ForemanEntity.class, EntityDataSerializers.STRING);

    /**
     * The display name of this crew member.
     * Synchronized to clients via entity data.
     */
    private String entityName;

    /**
     * Long-term memory storing task history, conversation context,
     * and knowledge learned from experience.
     */
    private ForemanMemory memory;

    /**
     * Companion memory storing player preferences, learning from interactions,
     * and personalization data.
     */
    private CompanionMemory companionMemory;

    /**
     * Action executor responsible for queuing and executing tasks.
     * Implements tick-based execution to prevent server freezing.
     */
    private ActionExecutor actionExecutor;

    /**
     * Dialogue manager for proactive commentary about task progress,
     * discoveries, and contextual observations.
     */
    private ProactiveDialogueManager dialogueManager;

    /**
     * Tick counter used for periodic operations (e.g., progress reporting).
     */
    private int tickCounter = 0;

    /**
     * Counter for consecutive errors in action executor - used for recovery.
     */
    private int errorRecoveryTicks = 0;

    /**
     * Whether this entity is currently in flying mode.
     * When flying, gravity is disabled and movement is unrestricted.
     */
    private boolean isFlying = false;

    /**
     * Whether this entity is immune to all damage.
     * Used during building to prevent suffocation, fall damage, etc.
     */
    private boolean isInvulnerable = false;

    // ========== Orchestration Support ==========

    /**
     * This agent's role in the orchestration hierarchy.
     * SOLO - Working alone without coordination
     * FOREMAN - Coordinating other agents
     * WORKER - Taking tasks from foreman
     */
    private AgentRole role = AgentRole.SOLO;

    /**
     * Reference to the orchestration service for multi-agent coordination.
     * Used for task distribution, progress tracking, and inter-agent communication.
     */
    private OrchestratorService orchestrator;

    /**
     * Thread-safe queue for incoming messages from other agents.
     * Messages are polled and processed during each tick.
     */
    private final ConcurrentLinkedQueue<AgentMessage> messageQueue = new ConcurrentLinkedQueue<>();

    /**
     * Flag indicating whether this agent has registered with the orchestrator.
     * Uses AtomicBoolean for thread-safe initialization on first tick.
     */
    private final AtomicBoolean registeredWithOrchestrator = new AtomicBoolean(false);

    /**
     * ID of the currently assigned task from the foreman.
     * Used for progress reporting and completion notification.
     */
    private String currentTaskId = null;

    /**
     * Current progress percentage (0-100) for the active task.
     * Marked volatile for visibility across threads during progress updates.
     */
    private volatile int currentTaskProgress = 0;

    // ========== Hive Mind (Cloudflare Edge) Support ==========

    /**
     * Tactical decision service for fast reflex decisions using Cloudflare Edge.
     * Provides sub-20ms response for combat and hazard detection.
     */
    private final TacticalDecisionService tacticalService;

    /**
     * Last game time when tactical situation was checked.
     * Used to limit check frequency to performance costs.
     */
    private long lastTacticalCheck = 0;

    /**
     * Last game time when state was synced with edge service.
     * Used to limit sync frequency for bandwidth efficiency.
     */
    private long lastStateSync = 0;

    /**
     * Constructs a new ForemanEntity.
     *
     * <p>Initializes all subsystems including memory, action executor,
     * dialogue manager, and tactical service. The entity starts in
     * invulnerable mode and registers with the orchestrator on first tick.</p>
     *
     * @param entityType The entity type from registration
     * @param level The world/level this entity is spawned in
     */
    public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.entityName = "Foreman";
        this.memory = new ForemanMemory(this);
        this.companionMemory = new CompanionMemory();
        this.actionExecutor = new ActionExecutor(this);
        this.dialogueManager = new ProactiveDialogueManager(this);
        this.tacticalService = TacticalDecisionService.getInstance();
        this.setCustomNameVisible(true);

        this.isInvulnerable = true;
        this.setInvulnerable(true);

        // Initialize orchestrator reference
        if (!level.isClientSide) {
            this.orchestrator = MineWrightMod.getOrchestratorService();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ENTITY_NAME, "Foreman");
    }

    /**
     * Main update loop called every game tick (20 times per second).
     *
     * <p>This method orchestrates all periodic operations:</p>
     * <ul>
     *   <li>Register with orchestrator on first tick</li>
     *   <li>Check tactical situation via Hive Mind (if enabled)</li>
     *   <li>Sync state with edge service (if enabled)</li>
     *   <li>Process incoming inter-agent messages</li>
     *   <li>Execute queued actions via ActionExecutor</li>
     *   <li>Trigger proactive dialogue</li>
     *   <li>Report task progress to foreman</li>
     * </ul>
     *
     * <p><b>Important:</b> This method must return quickly to avoid server lag.
     * Long-running operations (LLM calls) are handled asynchronously.</p>
     *
     * <p><b>Thread Safety:</b> Called on the Minecraft server thread only.
     * All state updates are thread-safe due to single-threaded execution.</p>
     *
     * <p><b>Error Handling:</b> Each subsystem is wrapped in try-catch to ensure
     * errors never crash the game. Failed subsystems log warnings but the entity
     * continues operating (graceful degradation).</p>
     */
    @Override
    public void tick() {
        try {
            super.tick();
        } catch (Exception e) {
            // Never let entity tick errors crash the game
            LOGGER.error("[{}] Critical error in parent entity tick, continuing anyway",
                entityName, e);
        }

        if (!this.level().isClientSide) {
            long gameTime = this.level().getGameTime();

            // Wrap each subsystem in try-catch for graceful degradation

            // Register with orchestrator on first tick
            if (!registeredWithOrchestrator.get() && orchestrator != null) {
                try {
                    registerWithOrchestrator();
                } catch (Exception e) {
                    LOGGER.error("[{}] Failed to register with orchestrator", entityName, e);
                    registeredWithOrchestrator.set(true); // Don't keep trying
                }
            }

            // Hive Mind: Periodic tactical check (combat reflexes, hazards)
            if (tacticalService.isEnabled() &&
                gameTime - lastTacticalCheck >= tacticalService.getCheckInterval()) {
                lastTacticalCheck = gameTime;
                try {
                    checkTacticalSituation();
                } catch (Exception e) {
                    LOGGER.warn("[{}] Tactical check failed (continuing normally)", entityName, e);
                    // Continue without tactical decisions - graceful degradation
                }
            }

            // Hive Mind: Periodic state sync with edge
            if (tacticalService.isEnabled() &&
                gameTime - lastStateSync >= tacticalService.getSyncInterval()) {
                lastStateSync = gameTime;
                try {
                    tacticalService.syncState(this);
                } catch (Exception e) {
                    LOGGER.warn("[{}] State sync failed (will retry later)", entityName, e);
                    // Continue without sync - not critical
                }
            }

            // Process incoming messages
            try {
                processMessages();
            } catch (Exception e) {
                LOGGER.error("[{}] Error processing messages (continuing anyway)", entityName, e);
                // Clear potentially corrupted message queue
                messageQueue.clear();
            }

            // Execute actions - most critical, wrap carefully
            try {
                actionExecutor.tick();
                errorRecoveryTicks = 0; // Reset error counter on success
            } catch (Exception e) {
                LOGGER.error("[{}] Critical error in action executor", entityName, e);
                errorRecoveryTicks++;

                // Only send chat message once per error burst (not every tick)
                if (errorRecoveryTicks == 1) {
                    try {
                        sendChatMessage("Hit a snag there boss. Working on it...");
                    } catch (Exception ignored) {
                        // If chat fails too, just log and continue
                    }
                }

                // After 3 consecutive errors, reset the action executor to recover
                if (errorRecoveryTicks >= 3) {
                    LOGGER.warn("[{}] Too many errors, resetting action executor", entityName);
                    try {
                        actionExecutor.stopCurrentAction();
                        actionExecutor = new ActionExecutor(this);
                        errorRecoveryTicks = 0;
                        sendChatMessage("Alright, I'm back on track now.");
                    } catch (Exception resetError) {
                        LOGGER.error("[{}] Failed to reset action executor", entityName, resetError);
                    }
                }
            }

            // Check for proactive dialogue triggers
            if (dialogueManager != null) {
                try {
                    dialogueManager.tick();
                } catch (Exception e) {
                    LOGGER.warn("[{}] Dialogue manager error (continuing without dialogue)", entityName, e);
                    // Dialogue is not critical - continue without it
                }
            }

            // Report progress if working on a task
            try {
                reportTaskProgress();
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed to report progress", entityName, e);
                // Progress reporting is not critical
            }
        }
    }

    public void setEntityName(String name) {
        this.entityName = name;
        this.entityData.set(ENTITY_NAME, name);
        this.setCustomName(Component.literal(name));
    }

    public String getEntityName() {
        return this.entityName;
    }

    /**
     * @deprecated Use {@link #getEntityName()} instead.
     */
    @Deprecated
    public String getSteveName() {
        return this.entityName;
    }

    public ForemanMemory getMemory() {
        return this.memory;
    }

    public CompanionMemory getCompanionMemory() {
        return this.companionMemory;
    }

    public ActionExecutor getActionExecutor() {
        return this.actionExecutor;
    }

    public ProactiveDialogueManager getDialogueManager() {
        return this.dialogueManager;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CrewName", this.entityName);

        CompoundTag memoryTag = new CompoundTag();
        this.memory.saveToNBT(memoryTag);
        tag.put("Memory", memoryTag);

        CompoundTag companionMemoryTag = new CompoundTag();
        this.companionMemory.saveToNBT(companionMemoryTag);
        tag.put("CompanionMemory", companionMemoryTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Try new key first, fall back to old key for backwards compatibility
        if (tag.contains("CrewName")) {
            this.setEntityName(tag.getString("CrewName"));
        } else if (tag.contains("SteveName")) {
            this.setEntityName(tag.getString("SteveName"));
        }

        if (tag.contains("Memory")) {
            this.memory.loadFromNBT(tag.getCompound("Memory"));
        }

        if (tag.contains("CompanionMemory")) {
            this.companionMemory.loadFromNBT(tag.getCompound("CompanionMemory"));
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        return spawnData;
    }

    public void sendChatMessage(String message) {
        if (this.level().isClientSide || this.level() == null) return;

        Component chatComponent = Component.literal("<" + this.entityName + "> " + message);
        this.level().players().forEach(player -> player.sendSystemMessage(chatComponent));
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    }

    public void setFlying(boolean flying) {
        this.isFlying = flying;
        this.setNoGravity(flying);
        this.setInvulnerableBuilding(flying);
    }

    public boolean isFlying() {
        return this.isFlying;
    }

    /**
     * Set invulnerability for building (immune to ALL damage: fire, lava, suffocation, fall, etc.)
     */
    public void setInvulnerableBuilding(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
        this.setInvulnerable(invulnerable); // Minecraft's built-in invulnerability
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isFlying && !this.level().isClientSide) {
            double motionY = this.getDeltaMovement().y;

            if (this.getNavigation() != null && this.getNavigation().isInProgress()) {
                super.travel(travelVector);

                // But add ability to move vertically freely
                if (Math.abs(motionY) < 0.1) {
                    // Small upward force to prevent falling
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
                }
            } else {
                super.travel(travelVector);
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, net.minecraft.world.damagesource.DamageSource source) {
        // No fall damage when flying
        if (this.isFlying) {
            return false;
        }
        return super.causeFallDamage(distance, damageMultiplier, source);
    }

    // ========== Orchestration Support ==========

    /**
     * Registers this crew member with the orchestrator service.
     * First agent becomes FOREMAN, subsequent agents become WORKERS.
     */
    private void registerWithOrchestrator() {
        if (orchestrator == null) {
            LOGGER.warn("[{}] Orchestrator service not available", entityName);
            return;
        }

        // Determine role based on existing agents
        int activeCount = MineWrightMod.getCrewManager().getActiveCount();

        if (activeCount == 1) {
            // First agent is the foreman
            this.role = AgentRole.FOREMAN;
            LOGGER.info("[{}] Registering as FOREMAN (first agent)", entityName);
        } else {
            // Subsequent agents are workers
            this.role = AgentRole.WORKER;
            LOGGER.info("[{}] Registering as WORKER (agent #{})", entityName, activeCount);
        }

        orchestrator.registerAgent(this, role);
        registeredWithOrchestrator.set(true);

        // Send chat message about role
        if (role == AgentRole.FOREMAN) {
            sendChatMessage("I am the Foreman! Ready to coordinate.");
        } else {
            sendChatMessage("Ready to work! Awaiting Foreman's instructions.");
        }
    }

    /**
     * Processes messages from the communication bus.
     */
    private void processMessages() {
        if (orchestrator == null) return;

        // Poll for new messages
        AgentMessage message;
        while ((message = orchestrator.getCommunicationBus().poll(entityName)) != null) {
            handleMessage(message);
        }
    }

    /**
     * Handles a single message from the communication bus.
     */
    private void handleMessage(AgentMessage message) {
        LOGGER.debug("[{}] Received message: {} from {}",
            entityName, message.getType(), message.getSenderName());

        switch (message.getType()) {
            case TASK_ASSIGNMENT:
                handleTaskAssignment(message);
                break;

            case PLAN_ANNOUNCEMENT:
                handlePlanAnnouncement(message);
                break;

            case BROADCAST:
                handleBroadcast(message);
                break;

            case STATUS_QUERY:
                handleStatusQuery(message);
                break;

            default:
                LOGGER.debug("[{}] Unhandled message type: {}", entityName, message.getType());
        }
    }

    /**
     * Handles task assignment from the foreman.
     */
    private void handleTaskAssignment(AgentMessage message) {
        if (role == AgentRole.FOREMAN) {
            LOGGER.debug("[{}] Foreman ignoring task assignment", entityName);
            return;
        }

        String taskDescription = message.getPayloadValue("taskDescription", "Unknown task");
        LOGGER.info("[{}] Received task assignment: {}", entityName, taskDescription);

        // Extract task parameters
        Task task = new Task(taskDescription, message.getPayload());

        // Execute the task
        sendChatMessage("Accepting task: " + taskDescription);
        currentTaskId = message.getMessageId();

        // Add to action queue
        actionExecutor.queueTask(task);

        // Send acknowledgment
        AgentMessage ack = AgentMessage.taskProgress(
            entityName, entityName,
            message.getSenderId(),
            message.getMessageId(),
            0,
            "Task accepted"
        );
        orchestrator.getCommunicationBus().publish(ack);
    }

    /**
     * Handles plan announcement from the foreman.
     */
    private void handlePlanAnnouncement(AgentMessage message) {
        String planId = message.getPayloadValue("planId", "?");
        int taskCount = message.getPayloadValue("taskCount", 0);
        LOGGER.info("[{}] Plan announced: {} ({} tasks)", entityName, planId, taskCount);
    }

    /**
     * Handles broadcast messages.
     */
    private void handleBroadcast(AgentMessage message) {
        LOGGER.info("[{}] Broadcast from {}: {}",
            entityName, message.getSenderName(), message.getContent());
    }

    /**
     * Handles status query from foreman.
     */
    private void handleStatusQuery(AgentMessage message) {
        String status = String.format("%s (%s) - %s",
            entityName,
            role.getDisplayName(),
            actionExecutor.isExecuting() ? "Working" : "Idle"
        );

        AgentMessage response = new AgentMessage.Builder()
            .type(AgentMessage.Type.STATUS_REPORT)
            .sender(entityName, entityName)
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
    private void reportTaskProgress() {
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
        if (progress > currentTaskProgress) {
            currentTaskProgress = progress;

            // Send progress update to foreman
            if (orchestrator != null && role != AgentRole.FOREMAN) {
                AgentMessage progressMsg = AgentMessage.taskProgress(
                    entityName, entityName,
                    "foreman", // Send to foreman
                    currentTaskId,
                    progress,
                    "In progress"
                );
                orchestrator.getCommunicationBus().publish(progressMsg);
            }
        }
    }

    // ========== Hive Mind (Cloudflare Edge) Support ==========

    /**
     * Checks tactical situation using Cloudflare edge for fast reflexes.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Collects nearby entities (hostile mobs, etc.)</li>
     *   <li>Sends async request to Cloudflare Worker</li>
     *   <li>Executes immediate reflex actions (flee, dodge, etc.)</li>
     * </ul>
     *
     * <p>Designed for sub-20ms response time. Falls back gracefully when edge unavailable.</p>
     */
    private void checkTacticalSituation() {
        if (this.level() == null || this.getBoundingBox() == null) {
            return;
        }

        // Get nearby entities
        List<Entity> nearbyEntities = this.level().getEntitiesOfClass(
            Entity.class,
            this.getBoundingBox().inflate(16.0) // 16 block radius
        );

        // Get tactical decision from edge
        CloudflareClient.TacticalDecision decision = tacticalService.checkTactical(this, nearbyEntities);

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
            entityName, decision.action, decision.reasoning);

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
                LOGGER.debug("[{}] Unknown tactical action: {}", entityName, decision.action);
            }
        }
    }

    /**
     * Sends a message to another agent or broadcasts to all.
     */
    public void sendMessage(AgentMessage message) {
        if (orchestrator != null) {
            orchestrator.getCommunicationBus().publish(message);
        }
    }

    /**
     * Gets the agent's role in the orchestration system.
     */
    public AgentRole getRole() {
        return role;
    }

    /**
     * Sets the agent's role (used for promotion/demotion).
     */
    public void setRole(AgentRole newRole) {
        AgentRole oldRole = this.role;
        this.role = newRole;

        // Re-register with new role
        if (orchestrator != null && registeredWithOrchestrator.get()) {
            orchestrator.unregisterAgent(entityName);
            orchestrator.registerAgent(this, newRole);
            registeredWithOrchestrator.set(true);
        }

        sendChatMessage(String.format("Role changed: %s -> %s",
            oldRole.getDisplayName(), newRole.getDisplayName()));
    }

    /**
     * Gets the current task ID being worked on.
     */
    public String getCurrentTaskId() {
        return currentTaskId;
    }

    /**
     * Marks current task as complete.
     */
    public void completeCurrentTask(String result) {
        if (currentTaskId != null && orchestrator != null && role != AgentRole.FOREMAN) {
            AgentMessage completeMsg = AgentMessage.taskComplete(
                entityName, entityName,
                "foreman",
                currentTaskId,
                true,
                result
            );
            orchestrator.getCommunicationBus().publish(completeMsg);
            sendChatMessage("Task completed: " + result);
            currentTaskId = null;
            currentTaskProgress = 0;

            // Trigger proactive dialogue for task completion
            if (dialogueManager != null) {
                dialogueManager.onTaskCompleted(result);
            }
        }
    }

    /**
     * Marks current task as failed.
     */
    public void failCurrentTask(String reason) {
        if (currentTaskId != null && orchestrator != null && role != AgentRole.FOREMAN) {
            AgentMessage failMsg = AgentMessage.taskComplete(
                entityName, entityName,
                "foreman",
                currentTaskId,
                false,
                reason
            );
            orchestrator.getCommunicationBus().publish(failMsg);
            sendChatMessage("Task failed: " + reason);
            currentTaskId = null;
            currentTaskProgress = 0;

            // Trigger proactive dialogue for task failure
            if (dialogueManager != null) {
                dialogueManager.onTaskFailed(currentTaskId != null ? currentTaskId : "task", reason);
            }
        }
    }

    // ========== Proactive Dialogue Triggers ==========

    /**
     * Notifies the dialogue manager that a task was completed.
     * Can be called by ActionExecutor when actions finish successfully.
     *
     * @param taskDescription Description of the completed task
     */
    public void notifyTaskCompleted(String taskDescription) {
        if (dialogueManager != null) {
            dialogueManager.onTaskCompleted(taskDescription);
        }
    }

    /**
     * Notifies the dialogue manager that a task failed.
     * Can be called by ActionExecutor when actions fail.
     *
     * @param taskDescription Description of the failed task
     * @param reason Reason for failure
     */
    public void notifyTaskFailed(String taskDescription, String reason) {
        if (dialogueManager != null) {
            dialogueManager.onTaskFailed(taskDescription, reason);
        }
    }

    /**
     * Notifies the dialogue manager that crew member is stuck on a task.
     * Can be called by ActionExecutor when actions can't progress.
     *
     * @param taskDescription Description of the task crew member is stuck on
     */
    public void notifyTaskStuck(String taskDescription) {
        if (dialogueManager != null) {
            dialogueManager.onTaskStuck(taskDescription);
        }
    }

    /**
     * Notifies the dialogue manager of a milestone achievement.
     * Use this for significant events that deserve celebration.
     *
     * @param milestone Description of the milestone
     */
    public void notifyMilestone(String milestone) {
        if (dialogueManager != null) {
            dialogueManager.onMilestoneReached(milestone);
        }
    }

    /**
     * Forces an immediate comment from crew member, bypassing cooldowns.
     * Use this for important events that should always be commented on.
     *
     * @param triggerType The type of event (e.g., "danger", "discovery", "achievement")
     * @param context Description of what happened
     */
    public void forceComment(String triggerType, String context) {
        if (dialogueManager != null) {
            dialogueManager.forceComment(triggerType, context);
        }
    }
}

