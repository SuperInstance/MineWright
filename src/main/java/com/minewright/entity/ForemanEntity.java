package com.minewright.entity;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionExecutor;
import com.minewright.behavior.ProcessManager;
import com.minewright.behavior.processes.FollowProcess;
import com.minewright.behavior.processes.IdleProcess;
import com.minewright.behavior.processes.SurvivalProcess;
import com.minewright.behavior.processes.TaskExecutionProcess;
import com.minewright.dialogue.ProactiveDialogueManager;
import com.minewright.humanization.HumanizationUtils;
import com.minewright.humanization.SessionManager;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ForemanMemory;
import com.minewright.orchestration.AgentMessage;
import com.minewright.orchestration.AgentRole;
import com.minewright.orchestration.OrchestratorService;
import com.minewright.recovery.RecoveryManager;
import com.minewright.recovery.StuckDetector;
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
 * <p><b>Architecture (Refactored Wave 46):</b></p>
 * <pre>
 * ForemanEntity (Main Entity Class - ~400 lines)
 * ├── EntityState - State management and data
 * ├── ActionCoordinator - Action execution and stuck recovery
 * ├── CommunicationHandler - Orchestration and dialogue
 * └── Minecraft Entity - PathfinderMob inheritance
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Entity operations are single-threaded on the Minecraft server thread</li>
 *   <li>Delegated classes use thread-safe collections for cross-thread communication</li>
 *   <li>LLM calls are async and execute on separate thread pools</li>
 * </ul>
 *
 * @see ActionExecutor
 * @see EntityState
 * @see ActionCoordinator
 * @see CommunicationHandler
 *
 * @since 1.0.0
 */
public class ForemanEntity extends PathfinderMob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForemanEntity.class);
    private static final EntityDataAccessor<String> ENTITY_NAME =
        SynchedEntityData.defineId(ForemanEntity.class, EntityDataSerializers.STRING);

    // ========== Delegated Subsystems ==========

    /**
     * Manages all state for this entity.
     * Encapsulates identity, memory, orchestration state, and movement capabilities.
     */
    private final EntityState state;

    /**
     * Coordinates action execution and behavior arbitration.
     * Manages tick-based execution, stuck detection, and recovery.
     */
    private final ActionCoordinator actionCoordinator;

    /**
     * Handles communication, orchestration, and dialogue.
     * Manages inter-agent messaging, task reporting, and proactive dialogue.
     */
    private final CommunicationHandler communicationHandler;

    // ========== Construction ==========

    /**
     * Constructs a new ForemanEntity.
     *
     * <p>Initializes all subsystems including state, action coordinator,
     * communication handler, and enables invulnerability by default.</p>
     *
     * @param entityType The entity type from registration
     * @param level The world/level this entity is spawned in
     */
    public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        // Initialize state first
        this.state = new EntityState(entityType, level, "Foreman");

        // Initialize subsystems with entity reference
        this.state.initializeSubsystems(this);
        this.actionCoordinator = new ActionCoordinator(this, this.state);
        this.communicationHandler = new CommunicationHandler(this, this.state);

        // Set up entity properties
        this.setCustomNameVisible(true);
        this.state.setInvulnerable(true);
        this.setInvulnerable(true);

        // Initialize behavior processes in the process manager
        initializeProcesses();
    }

    /**
     * Initializes all behavior processes for the process manager.
     * Processes are registered in priority order (highest first).
     */
    private void initializeProcesses() {
        ProcessManager processManager = state.getProcessManager();
        if (processManager == null) {
            LOGGER.warn("[{}] ProcessManager not initialized, cannot register processes",
                state.getEntityName());
            return;
        }

        try {
            // Register processes in priority order (highest first)
            processManager.registerProcess(new SurvivalProcess(this));
            processManager.registerProcess(new TaskExecutionProcess(this));
            processManager.registerProcess(new FollowProcess(this));
            processManager.registerProcess(new IdleProcess(this));

            LOGGER.info("[{}] Registered {} behavior processes",
                state.getEntityName(), processManager.getProcessCount());
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to initialize behavior processes",
                state.getEntityName(), e);
        }
    }

    // ========== Minecraft Entity Overrides ==========

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
     * <p>This method orchestrates all periodic operations by delegating to
     * specialized coordinators:</p>
     * <ul>
     *   <li>Register with orchestrator on first tick</li>
     *   <li>Check tactical situation via Hive Mind (if enabled)</li>
     *   <li>Sync state with edge service (if enabled)</li>
     *   <li>Process incoming inter-agent messages</li>
     *   <li>Update session state and fatigue</li>
     *   <li>Execute behaviors via ActionCoordinator</li>
     *   <li>Trigger proactive dialogue</li>
     *   <li>Report task progress to foreman</li>
     * </ul>
     */
    @Override
    public void tick() {
        try {
            super.tick();
        } catch (Exception e) {
            // Never let entity tick errors crash the game
            LOGGER.error("[{}] Critical error in parent entity tick, continuing anyway",
                state.getEntityName(), e);
        }

        if (!this.level().isClientSide) {
            long gameTime = this.level().getGameTime();

            // Wrap each subsystem in try-catch for graceful degradation

            // Register with orchestrator on first tick
            if (!state.getRegisteredWithOrchestrator().get() && state.getOrchestrator() != null) {
                try {
                    communicationHandler.registerWithOrchestrator();
                } catch (Exception e) {
                    LOGGER.error("[{}] Failed to register with orchestrator",
                        state.getEntityName(), e);
                    state.getRegisteredWithOrchestrator().set(true); // Don't keep trying
                }
            }

            // Hive Mind: Periodic tactical check (combat reflexes, hazards)
            if (state.getTacticalService().isEnabled() &&
                gameTime - state.getLastTacticalCheck() >= state.getTacticalService().getCheckInterval()) {
                state.setLastTacticalCheck(gameTime);
                try {
                    communicationHandler.checkTacticalSituation();
                } catch (Exception e) {
                    LOGGER.warn("[{}] Tactical check failed (continuing normally)",
                        state.getEntityName(), e);
                }
            }

            // Hive Mind: Periodic state sync with edge
            if (state.getTacticalService().isEnabled() &&
                gameTime - state.getLastStateSync() >= state.getTacticalService().getSyncInterval()) {
                state.setLastStateSync(gameTime);
                try {
                    state.getTacticalService().syncState(this);
                } catch (Exception e) {
                    LOGGER.warn("[{}] State sync failed (will retry later)",
                        state.getEntityName(), e);
                }
            }

            // Process incoming messages
            try {
                communicationHandler.processMessages();
            } catch (Exception e) {
                LOGGER.error("[{}] Error processing messages (continuing anyway)",
                    state.getEntityName(), e);
                // Clear potentially corrupted message queue
                state.getMessageQueue().clear();
            }

            // Update session state (fatigue, breaks)
            SessionManager sessionManager = state.getSessionManager();
            if (sessionManager != null) {
                try {
                    sessionManager.update();
                } catch (Exception e) {
                    LOGGER.warn("[{}] Session manager error (continuing without session tracking)",
                        state.getEntityName(), e);
                }
            }

            // Execute behaviors via ActionCoordinator
            try {
                actionCoordinator.tick();
            } catch (Exception e) {
                LOGGER.error("[{}] Critical error in action coordinator",
                    state.getEntityName(), e);
            }

            // Check for proactive dialogue triggers
            ProactiveDialogueManager dialogueManager = state.getDialogueManager();
            if (dialogueManager != null) {
                try {
                    dialogueManager.tick();
                } catch (Exception e) {
                    LOGGER.warn("[{}] Dialogue manager error (continuing without dialogue)",
                        state.getEntityName(), e);
                }
            }

            // Report progress if working on a task
            try {
                communicationHandler.reportTaskProgress();
            } catch (Exception e) {
                LOGGER.warn("[{}] Failed to report progress", state.getEntityName(), e);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        state.saveToNBT(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        state.loadFromNBT(tag);
        // Update synced entity data
        this.entityData.set(ENTITY_NAME, state.getEntityName());
        this.setCustomName(Component.literal(state.getEntityName()));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        return spawnData;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    }

    // ========== Public API - Identity ==========

    /**
     * Sets the display name of this crew member.
     * Updates both local state and synced entity data.
     *
     * @param name The new name for this entity
     */
    public void setEntityName(String name) {
        state.setEntityName(name);
        this.entityData.set(ENTITY_NAME, name);
        this.setCustomName(Component.literal(name));
    }

    /**
     * Gets the display name of this crew member.
     *
     * @return The entity's name
     */
    public String getEntityName() {
        return state.getEntityName();
    }

    /**
     * @deprecated Use {@link #getEntityName()} instead.
     */
    @Deprecated
    public String getSteveName() {
        return state.getEntityName();
    }

    // ========== Public API - Memory ==========

    /**
     * Gets the foreman memory system for this entity.
     *
     * @return The ForemanMemory instance
     */
    public ForemanMemory getMemory() {
        return state.getMemory();
    }

    /**
     * Gets the companion memory system for this entity.
     *
     * @return The CompanionMemory instance
     */
    public CompanionMemory getCompanionMemory() {
        return state.getCompanionMemory();
    }

    // ========== Public API - Action and Behavior Systems ==========

    /**
     * Gets the action executor for this entity.
     *
     * @return The ActionExecutor instance
     */
    public ActionExecutor getActionExecutor() {
        return state.getActionExecutor();
    }

    /**
     * Gets the process manager for behavior arbitration.
     *
     * @return ProcessManager instance, or null if not initialized
     */
    public ProcessManager getProcessManager() {
        return state.getProcessManager();
    }

    /**
     * Gets the name of the currently active behavior process.
     *
     * @return Active process name, or "IDLE" if no process is active
     */
    public String getActiveProcessName() {
        ProcessManager processManager = state.getProcessManager();
        return processManager != null ? processManager.getActiveProcessName() : "IDLE";
    }

    // ========== Public API - Dialogue ==========

    /**
     * Gets the dialogue manager for proactive commentary.
     *
     * @return The ProactiveDialogueManager instance
     */
    public ProactiveDialogueManager getDialogueManager() {
        return state.getDialogueManager();
    }

    // ========== Public API - Orchestration ==========

    /**
     * Gets the agent's role in the orchestration system.
     *
     * @return The agent's AgentRole
     */
    public AgentRole getRole() {
        return state.getRole();
    }

    /**
     * Sets the agent's role (used for promotion/demotion).
     *
     * @param newRole The new role for this agent
     */
    public void setRole(AgentRole newRole) {
        AgentRole oldRole = state.getRole();
        state.setRole(newRole);

        // Re-register with new role
        OrchestratorService orchestrator = state.getOrchestrator();
        if (orchestrator != null && state.getRegisteredWithOrchestrator().get()) {
            orchestrator.unregisterAgent(state.getEntityName());
            orchestrator.registerAgent(this, newRole);
            state.getRegisteredWithOrchestrator().set(true);
        }

        sendChatMessage(String.format("Role changed: %s -> %s",
            oldRole.getDisplayName(), newRole.getDisplayName()));
    }

    /**
     * Gets the current task ID being worked on.
     *
     * @return The current task ID, or null if no task
     */
    public String getCurrentTaskId() {
        return state.getCurrentTaskId();
    }

    /**
     * Sends a message to another agent or broadcasts to all.
     *
     * @param message The message to send
     */
    public void sendMessage(AgentMessage message) {
        communicationHandler.sendMessage(message);
    }

    /**
     * Marks current task as complete.
     *
     * @param result The task completion result
     */
    public void completeCurrentTask(String result) {
        communicationHandler.completeCurrentTask(result);
    }

    /**
     * Marks current task as failed.
     *
     * @param reason The failure reason
     */
    public void failCurrentTask(String reason) {
        communicationHandler.failCurrentTask(reason);
    }

    // ========== Public API - Proactive Dialogue Triggers ==========

    /**
     * Notifies the dialogue manager that a task was completed.
     *
     * @param taskDescription Description of the completed task
     */
    public void notifyTaskCompleted(String taskDescription) {
        communicationHandler.notifyTaskCompleted(taskDescription);
    }

    /**
     * Notifies the dialogue manager that a task failed.
     *
     * @param taskDescription Description of the failed task
     * @param reason Reason for failure
     */
    public void notifyTaskFailed(String taskDescription, String reason) {
        communicationHandler.notifyTaskFailed(taskDescription, reason);
    }

    /**
     * Notifies the dialogue manager that crew member is stuck on a task.
     *
     * @param taskDescription Description of the task crew member is stuck on
     */
    public void notifyTaskStuck(String taskDescription) {
        communicationHandler.notifyTaskStuck(taskDescription);
    }

    /**
     * Notifies the dialogue manager of a milestone achievement.
     *
     * @param milestone Description of the milestone
     */
    public void notifyMilestone(String milestone) {
        communicationHandler.notifyMilestone(milestone);
    }

    /**
     * Forces an immediate comment from crew member, bypassing cooldowns.
     *
     * @param triggerType The type of event (e.g., "danger", "discovery", "achievement")
     * @param context Description of what happened
     */
    public void forceComment(String triggerType, String context) {
        communicationHandler.forceComment(triggerType, context);
    }

    // ========== Public API - Recovery Systems ==========

    /**
     * Gets the stuck detector for monitoring agent position and progress.
     *
     * @return StuckDetector instance, or null if not initialized
     */
    public StuckDetector getStuckDetector() {
        return state.getStuckDetector();
    }

    /**
     * Gets the recovery manager for handling stuck conditions.
     *
     * @return RecoveryManager instance, or null if not initialized
     */
    public RecoveryManager getRecoveryManager() {
        return state.getRecoveryManager();
    }

    // ========== Public API - Session and Humanization ==========

    /**
     * Gets the session manager for fatigue and break tracking.
     *
     * @return SessionManager instance, or null if not initialized
     */
    public SessionManager getSessionManager() {
        return state.getSessionManager();
    }

    /**
     * Gets a humanized reaction delay based on session state.
     *
     * @return Reaction delay in ticks (20 ticks = 1 second)
     */
    public int getHumanizedReactionDelay() {
        SessionManager sessionManager = state.getSessionManager();
        if (sessionManager == null || !sessionManager.isEnabled()) {
            // Base reaction time: 150-600ms converted to ticks (3-12 ticks)
            int reactionMs = HumanizationUtils.humanReactionTime();
            return Math.max(3, Math.min(12, reactionMs / 50));
        }

        // Get session-based multipliers
        double fatigue = sessionManager.getFatigueLevel();
        double phaseMultiplier = sessionManager.getReactionMultiplier();

        // Generate contextual reaction time
        int reactionMs = HumanizationUtils.contextualReactionTime(fatigue, 0.0, 0.0);
        reactionMs = (int) (reactionMs * phaseMultiplier);

        // Convert to ticks (round up)
        return Math.max(3, Math.min(20, (reactionMs + 49) / 50));
    }

    /**
     * Checks if agent should make a mistake based on session state.
     *
     * @param baseRate Base mistake probability (0.0 to 1.0)
     * @return true if agent should make a mistake
     */
    public boolean shouldMakeMistake(double baseRate) {
        SessionManager sessionManager = state.getSessionManager();
        if (sessionManager == null || !sessionManager.isEnabled()) {
            return HumanizationUtils.shouldMakeMistake(baseRate);
        }

        // Apply session-based error multiplier
        double errorMultiplier = sessionManager.getErrorMultiplier();
        double adjustedRate = baseRate * errorMultiplier;

        return HumanizationUtils.shouldMakeMistake(adjustedRate);
    }

    // ========== Movement Capabilities ==========

    /**
     * Sets whether this entity is flying.
     * When flying, gravity is disabled and movement is unrestricted.
     *
     * @param flying true to enable flying mode
     */
    public void setFlying(boolean flying) {
        state.setFlying(flying);
        this.setNoGravity(flying);
        this.setInvulnerableBuilding(flying);
    }

    /**
     * Checks if this entity is currently flying.
     *
     * @return true if flying
     */
    public boolean isFlying() {
        return state.isFlying();
    }

    /**
     * Set invulnerability for building (immune to ALL damage: fire, lava, suffocation, fall, etc.)
     *
     * @param invulnerable true to make invulnerable
     */
    public void setInvulnerableBuilding(boolean invulnerable) {
        state.setInvulnerable(invulnerable);
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
        if (state.isFlying() && !this.level().isClientSide) {
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
        if (state.isFlying()) {
            return false;
        }
        return super.causeFallDamage(distance, damageMultiplier, source);
    }

    // ========== Communication ==========

    /**
     * Sends a chat message to all players.
     *
     * @param message The message to send
     */
    public void sendChatMessage(String message) {
        if (this.level().isClientSide || this.level() == null) return;

        Component chatComponent = Component.literal("<" + state.getEntityName() + "> " + message);
        this.level().players().forEach(player -> player.sendSystemMessage(chatComponent));
    }
}
