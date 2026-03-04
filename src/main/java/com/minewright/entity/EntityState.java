package com.minewright.entity;

import com.minewright.action.ActionExecutor;
import com.minewright.dialogue.ProactiveDialogueManager;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ForemanMemory;
import com.minewright.orchestration.AgentRole;
import com.minewright.orchestration.OrchestratorService;
import com.minewright.recovery.RecoveryManager;
import com.minewright.recovery.StuckDetector;
import com.minewright.behavior.ProcessManager;
import com.minewright.humanization.SessionManager;
import com.minewright.hivemind.TacticalDecisionService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the state of a ForemanEntity.
 *
 * <p>This class encapsulates all state-related aspects of a ForemanEntity including:
 * <ul>
 *   <li>Identity (name, role)</li>
 *   <li>Memory systems (foreman memory, companion memory)</li>
 *   <li>Subsystem references (action executor, dialogue manager, process manager)</li>
 *   <li>Orchestration state (role, registration, messaging)</li>
 *   <li>Movement capabilities (flying, invulnerability)</li>
 *   <li>Session and humanization state</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class uses volatile fields and atomic types
 * for cross-thread visibility where needed (e.g., role changes during orchestration).</p>
 *
 * @since 1.0.0
 */
public class EntityState {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityState.class);

    // ========== Identity State ==========

    /**
     * The display name of this crew member.
     * Marked volatile for visibility across threads.
     */
    private volatile String entityName;

    /**
     * This agent's role in the orchestration hierarchy.
     * SOLO - Working alone without coordination
     * FOREMAN - Coordinating other agents
     * WORKER - Taking tasks from foreman
     * Marked volatile for visibility across threads during role changes.
     */
    private volatile AgentRole role = AgentRole.SOLO;

    // ========== Memory Systems ==========

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

    // ========== Action and Behavior Systems ==========

    /**
     * Action executor responsible for queuing and executing tasks.
     * Implements tick-based execution to prevent server freezing.
     */
    private ActionExecutor actionExecutor;

    /**
     * Process manager for behavior arbitration.
     * Coordinates competing behaviors via priority-based selection.
     */
    private ProcessManager processManager;

    /**
     * Dialogue manager for proactive commentary about task progress,
     * discoveries, and contextual observations.
     */
    private ProactiveDialogueManager dialogueManager;

    // ========== Orchestration State ==========

    /**
     * Reference to the orchestration service for multi-agent coordination.
     * Marked volatile for visibility across threads (lazy initialization).
     */
    private volatile OrchestratorService orchestrator;

    /**
     * Thread-safe queue for incoming messages from other agents.
     */
    private final ConcurrentLinkedQueue<com.minewright.orchestration.AgentMessage> messageQueue = new ConcurrentLinkedQueue<>();

    /**
     * Flag indicating whether this agent has registered with the orchestrator.
     * Uses AtomicBoolean for thread-safe initialization on first tick.
     */
    private final AtomicBoolean registeredWithOrchestrator = new AtomicBoolean(false);

    /**
     * ID of the currently assigned task from the foreman.
     * Marked volatile for visibility across threads.
     */
    private volatile String currentTaskId = null;

    /**
     * Current progress percentage (0-100) for the active task.
     * Marked volatile for visibility across threads during progress updates.
     */
    private volatile int currentTaskProgress = 0;

    // ========== Movement Capabilities ==========

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

    // ========== Stuck Detection and Recovery ==========

    /**
     * Detects when the agent is stuck (position, progress, state, or path).
     */
    private StuckDetector stuckDetector;

    /**
     * Manages recovery strategies for stuck conditions.
     */
    private RecoveryManager recoveryManager;

    // ========== Session and Humanization ==========

    /**
     * Manages session state and fatigue modeling.
     */
    private SessionManager sessionManager;

    /**
     * Tactical decision service for fast reflex decisions using Cloudflare Edge.
     */
    private final TacticalDecisionService tacticalService;

    /**
     * Last game time when tactical situation was checked.
     */
    private long lastTacticalCheck = 0;

    /**
     * Last game time when state was synced with edge service.
     */
    private long lastStateSync = 0;

    // ========== Construction ==========

    /**
     * Creates a new EntityState instance.
     *
     * @param entityType The entity type from registration
     * @param level The world/level this entity is spawned in
     * @param entityName The initial name for this entity
     */
    public EntityState(EntityType<? extends net.minecraft.world.entity.PathfinderMob> entityType, Level level, String entityName) {
        this.entityName = entityName;
        this.tacticalService = TacticalDecisionService.getInstance();

        // Initialize orchestrator reference
        if (!level.isClientSide) {
            this.orchestrator = com.minewright.MineWrightMod.getOrchestratorService();
        }
    }

    /**
     * Initializes all subsystems after construction.
     * Called after the entity is fully constructed to avoid circular dependencies.
     *
     * @param entity The ForemanEntity this state belongs to
     */
    public void initializeSubsystems(ForemanEntity entity) {
        this.memory = new ForemanMemory(entity);
        this.companionMemory = new CompanionMemory();
        this.actionExecutor = new ActionExecutor(entity);
        this.dialogueManager = new ProactiveDialogueManager(entity);
        this.processManager = new ProcessManager(entity);
        this.stuckDetector = new StuckDetector(entity);
        this.recoveryManager = new RecoveryManager(entity);
        this.sessionManager = new SessionManager();
    }

    // ========== NBT Persistence ==========

    /**
     * Saves state data to NBT format for world saving.
     *
     * @param tag The compound tag to save data to
     */
    public void saveToNBT(CompoundTag tag) {
        tag.putString("CrewName", this.entityName);

        CompoundTag memoryTag = new CompoundTag();
        this.memory.saveToNBT(memoryTag);
        tag.put("Memory", memoryTag);

        CompoundTag companionMemoryTag = new CompoundTag();
        this.companionMemory.saveToNBT(companionMemoryTag);
        tag.put("CompanionMemory", companionMemoryTag);
    }

    /**
     * Loads state data from NBT format after world load.
     *
     * @param tag The compound tag to load data from
     */
    public void loadFromNBT(CompoundTag tag) {
        // Try new key first, fall back to old key for backwards compatibility
        if (tag.contains("CrewName")) {
            setEntityName(tag.getString("CrewName"));
        } else if (tag.contains("SteveName")) {
            setEntityName(tag.getString("SteveName"));
        }

        if (tag.contains("Memory")) {
            this.memory.loadFromNBT(tag.getCompound("Memory"));
        }

        if (tag.contains("CompanionMemory")) {
            this.companionMemory.loadFromNBT(tag.getCompound("CompanionMemory"));
        }
    }

    // ========== Getters and Setters ==========

    public String getEntityName() {
        return this.entityName;
    }

    public void setEntityName(String name) {
        this.entityName = name;
    }

    public AgentRole getRole() {
        return role;
    }

    public void setRole(AgentRole role) {
        this.role = role;
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

    public ProcessManager getProcessManager() {
        return this.processManager;
    }

    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public ProactiveDialogueManager getDialogueManager() {
        return this.dialogueManager;
    }

    public OrchestratorService getOrchestrator() {
        return this.orchestrator;
    }

    public ConcurrentLinkedQueue<com.minewright.orchestration.AgentMessage> getMessageQueue() {
        return this.messageQueue;
    }

    public AtomicBoolean getRegisteredWithOrchestrator() {
        return this.registeredWithOrchestrator;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(String taskId) {
        this.currentTaskId = taskId;
    }

    public int getCurrentTaskProgress() {
        return currentTaskProgress;
    }

    public void setCurrentTaskProgress(int progress) {
        this.currentTaskProgress = progress;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public void setFlying(boolean flying) {
        this.isFlying = flying;
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
    }

    public StuckDetector getStuckDetector() {
        return stuckDetector;
    }

    public RecoveryManager getRecoveryManager() {
        return recoveryManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public TacticalDecisionService getTacticalService() {
        return tacticalService;
    }

    public long getLastTacticalCheck() {
        return lastTacticalCheck;
    }

    public void setLastTacticalCheck(long time) {
        this.lastTacticalCheck = time;
    }

    public long getLastStateSync() {
        return lastStateSync;
    }

    public void setLastStateSync(long time) {
        this.lastStateSync = time;
    }
}
