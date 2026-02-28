package com.minewright.blackboard;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.action.ActionResult;
import com.minewright.action.CollaborativeBuildManager;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Integration layer connecting the blackboard system with existing MineWright systems.
 *
 * <p><b>Purpose:</b></p>
 * <p>BlackboardIntegration hooks the blackboard into ForemanEntity, ActionExecutor,
 * CollaborativeBuildManager, and other systems to enable automatic knowledge sharing
 * without requiring explicit blackboard calls throughout the codebase.</p>
 *
 * <p><b>Integration Points:</b></p>
 * <ul>
 *   <li><b>ForemanEntity:</b> Automatic agent state updates</li>
 *   <li><b>ActionExecutor:</b> Task lifecycle events</li>
 *   <li><b>World Scanning:</b> Periodic environment observations</li>
 *   <li><b>Collaborative Build:</b> Shared build progress</li>
 *   <li><b>Threat Detection:</b> Hostile entity notifications</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Initialize on server startup
 * BlackboardIntegration.initialize();
 *
 * // Agent registration happens automatically
 * // Subscribers are notified when knowledge is posted
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Initialization can be called from any thread.</p>
 *
 * @see Blackboard
 * @see AgentStateSource
 * @see WorldKnowledgeSource
 * @see TaskResultSource
 * @since 1.0.0
 */
public class BlackboardIntegration {
    private static final Logger LOGGER = TestLogger.getLogger(BlackboardIntegration.class);
    /**
     * Singleton instance for the integration layer.
     */
    private static volatile BlackboardIntegration instance;

    /**
     * The blackboard instance.
     */
    private final Blackboard blackboard;

    /**
     * Flag indicating whether integration is enabled.
     */
    private volatile boolean enabled = false;

    /**
     * Tick counter for periodic operations.
     */
    private int tickCounter = 0;

    /**
     * Interval for world state scans (in ticks).
     * 100 ticks = 5 seconds.
     */
    private static final int WORLD_SCAN_INTERVAL = 100;

    /**
     * Interval for blackboard cleanup (in ticks).
     * 1200 ticks = 1 minute.
     */
    private static final int CLEANUP_INTERVAL = 1200;

    /**
     * Private constructor for singleton pattern.
     */
    private BlackboardIntegration() {
        this.blackboard = Blackboard.getInstance();
    }

    /**
     * Gets the singleton instance of the integration layer.
     *
     * @return BlackboardIntegration instance
     */
    public static BlackboardIntegration getInstance() {
        if (instance == null) {
            synchronized (BlackboardIntegration.class) {
                if (instance == null) {
                    instance = new BlackboardIntegration();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the blackboard integration.
     *
     * <p>Sets up automatic knowledge sources and subscribers.
     * Call this on server startup.</p>
     */
    public void initialize() {
        if (enabled) {
            LOGGER.warn("Blackboard integration already initialized");
            return;
        }

        LOGGER.info("Initializing blackboard integration...");

        // Register global subscribers
        registerGlobalSubscribers();

        enabled = true;
        LOGGER.info("Blackboard integration enabled");
    }

    /**
     * Registers a ForemanEntity with the blackboard system.
     *
     * <p>This method:</p>
 * <ul>
     *   <li>Creates an AgentStateSource for the entity</li>
     *   <li>Posts initial agent status</li>
     *   <li>Subscribes the entity to relevant knowledge areas</li>
     * </ul>
     *
     * @param entity The ForemanEntity to register
     */
    public void registerAgent(ForemanEntity entity) {
        if (!enabled || entity == null) {
            return;
        }

        try {
            // Create and register agent state source
            AgentStateSource stateSource = new AgentStateSource(entity);
            stateSource.registerAgent();

            // Subscribe agent to relevant knowledge areas
            subscribeAgentToAreas(entity);

            LOGGER.info("Agent {} registered with blackboard", entity.getEntityName());

        } catch (Exception e) {
            LOGGER.error("Failed to register agent {} with blackboard",
                entity.getEntityName(), e);
        }
    }

    /**
     * Unregisters a ForemanEntity from the blackboard system.
     *
     * @param entity The ForemanEntity to unregister
     */
    public void unregisterAgent(ForemanEntity entity) {
        if (!enabled || entity == null) {
            return;
        }

        try {
            AgentStateSource stateSource = new AgentStateSource(entity);
            stateSource.unregisterAgent();

            LOGGER.info("Agent {} unregistered from blackboard", entity.getEntityName());

        } catch (Exception e) {
            LOGGER.error("Failed to unregister agent {} from blackboard",
                entity.getEntityName(), e);
        }
    }

    /**
     * Subscribes an agent to relevant knowledge areas.
     *
     * <p>Agents subscribe to:</p>
     * <ul>
     *   <li><b>THREATS:</b> For danger awareness</li>
     *   <li><b>AGENT_STATUS:</b> For coordination</li>
     *   <li><b>BUILD_PLANS:</b> For collaborative building</li>
     * </ul>
     *
     * @param entity The agent to subscribe
     */
    private void subscribeAgentToAreas(ForemanEntity entity) {
        BlackboardSubscriber subscriber = new AgentBlackboardSubscriber(entity);

        blackboard.subscribe(KnowledgeArea.THREATS, subscriber);
        blackboard.subscribe(KnowledgeArea.AGENT_STATUS, subscriber);
        blackboard.subscribe(KnowledgeArea.BUILD_PLANS, subscriber);

        LOGGER.debug("Agent {} subscribed to knowledge areas", entity.getEntityName());
    }

    /**
     * Posts a task started event to the blackboard.
     *
     * <p>Called by ActionExecutor when a task begins.</p>
     *
     * @param entity The agent executing the task
     * @param task The task being started
     */
    public void onTaskStarted(ForemanEntity entity, Task task) {
        if (!enabled || entity == null || task == null) {
            return;
        }

        try {
            TaskResultSource resultSource = new TaskResultSource(entity.getUUID());
            resultSource.postTaskStarted(task);

            // Also update agent's current task in status
            AgentStateSource stateSource = new AgentStateSource(entity);
            stateSource.postCurrentTask(task.getAction());

        } catch (Exception e) {
            LOGGER.error("Failed to post task started event", e);
        }
    }

    /**
     * Posts a task completed event to the blackboard.
     *
     * <p>Called by ActionExecutor when a task succeeds.</p>
     *
     * @param entity The agent that executed the task
     * @param task The completed task
     * @param result Action result
     */
    public void onTaskCompleted(ForemanEntity entity, Task task, ActionResult result) {
        if (!enabled || entity == null || task == null) {
            return;
        }

        try {
            TaskResultSource resultSource = new TaskResultSource(entity.getUUID());
            resultSource.postTaskCompleted(task, result);

            // Update agent state
            AgentStateSource stateSource = new AgentStateSource(entity);
            stateSource.postStateUpdate(AgentState.IDLE);

        } catch (Exception e) {
            LOGGER.error("Failed to post task completed event", e);
        }
    }

    /**
     * Posts a task failed event to the blackboard.
     *
     * <p>Called by ActionExecutor when a task fails.</p>
     *
     * @param entity The agent that executed the task
     * @param task The failed task
     * @param result Action result with failure details
     */
    public void onTaskFailed(ForemanEntity entity, Task task, ActionResult result) {
        if (!enabled || entity == null || task == null) {
            return;
        }

        try {
            TaskResultSource resultSource = new TaskResultSource(entity.getUUID());
            resultSource.postTaskFailed(task, result);

            // Update agent state
            AgentStateSource stateSource = new AgentStateSource(entity);
            stateSource.postActionCompletion(task.getAction(), false);

        } catch (Exception e) {
            LOGGER.error("Failed to post task failed event", e);
        }
    }

    /**
     * Updates collaborative build progress on the blackboard.
     *
     * <p>Called by BuildStructureAction during collaborative building.</p>
     *
     * @param buildId The build ID
     * @param blocksPlaced Number of blocks placed
     * @param totalBlocks Total blocks in the build
     * @param agentUUID UUID of the agent placing blocks
     */
    public void updateBuildProgress(String buildId, int blocksPlaced, int totalBlocks,
                                    UUID agentUUID) {
        if (!enabled) {
            return;
        }

        try {
            String key = "build_" + buildId + "_progress";
            BuildProgress progress = new BuildProgress(
                buildId,
                blocksPlaced,
                totalBlocks,
                (blocksPlaced * 100) / totalBlocks,
                System.currentTimeMillis()
            );

            BlackboardEntry<BuildProgress> entry = new BlackboardEntry<>(
                key,
                progress,
                agentUUID,
                1.0,
                BlackboardEntry.EntryType.FACT
            );

            blackboard.post(KnowledgeArea.BUILD_PLANS, entry);

        } catch (Exception e) {
            LOGGER.error("Failed to update build progress", e);
        }
    }

    /**
     * Scans the world around an agent and posts observations.
     *
     * <p>Called periodically by ForemanEntity tick.</p>
     *
     * @param entity The agent to scan around
     * @param radius Scan radius in blocks
     */
    public void scanWorldForAgent(ForemanEntity entity, int radius) {
        if (!enabled || entity == null) {
            return;
        }

        try {
            Level level = entity.level();
            if (level == null || level.isClientSide) {
                return;
            }

            WorldKnowledgeSource worldSource = new WorldKnowledgeSource(level);
            int observations = worldSource.scanAround(entity.blockPosition(), radius, entity.getUUID());

            if (observations > 0) {
                LOGGER.debug("World scan for {}: {} observations posted",
                    entity.getEntityName(), observations);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to scan world for agent {}", entity.getEntityName(), e);
        }
    }

    /**
     * Detects and posts threats in the area around an agent.
     *
     * <p>Scans for hostile entities and posts them to the THREATS area.</p>
     *
     * @param entity The agent to scan around
     * @param radius Detection radius in blocks
     */
    public void detectThreatsForAgent(ForemanEntity entity, int radius) {
        if (!enabled || entity == null) {
            return;
        }

        try {
            Level level = entity.level();
            if (level == null || level.isClientSide) {
                return;
            }

            WorldKnowledgeSource worldSource = new WorldKnowledgeSource(level);

            // Scan for hostile entities
            level.getEntitiesOfClass(Monster.class, entity.getBoundingBox().inflate(radius))
                .stream()
                .limit(5) // Limit to 5 threats per scan
                .forEach(monster -> worldSource.postThreat(monster, entity.getUUID()));

        } catch (Exception e) {
            LOGGER.error("Failed to detect threats for agent {}", entity.getEntityName(), e);
        }
    }

    /**
     * Performs periodic maintenance on the blackboard.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Evicts stale entries</li>
     *   <li>Cleans up completed builds</li>
     *   <li>Logs statistics</li>
     * </ul>
     *
     * <p>Call this periodically (e.g., every minute) from the server thread.</p>
     */
    public void performMaintenance() {
        if (!enabled) {
            return;
        }

        try {
            tickCounter++;

            // Evict stale entries every minute
            if (tickCounter % CLEANUP_INTERVAL == 0) {
                int evicted = blackboard.evictStale(60000); // 1 minute
                if (evicted > 0) {
                    LOGGER.info("Evicted {} stale entries from blackboard", evicted);
                }
            }

            // Log statistics every 5 minutes
            if (tickCounter % (CLEANUP_INTERVAL * 5) == 0) {
                LOGGER.info("\n{}", blackboard.getStatistics());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to perform blackboard maintenance", e);
        }
    }

    /**
     * Registers global subscribers for monitoring and logging.
     */
    private void registerGlobalSubscribers() {
        // Add a logging subscriber for all knowledge areas
        BlackboardSubscriber loggingSubscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                LOGGER.debug("[Blackboard] {} posted to {}: {}",
                    entry.getKey(), area.getId(), entry.getValue());
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {
                LOGGER.debug("[Blackboard] {} removed from {}", key, area.getId());
            }
        };

        blackboard.subscribeAll(loggingSubscriber);
    }

    /**
     * Checks if the integration is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the integration.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Blackboard integration {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Data class for build progress updates.
     */
    public record BuildProgress(
        String buildId,
        int blocksPlaced,
        int totalBlocks,
        int progressPercentage,
        long timestamp
    ) {
        @Override
        public String toString() {
            return String.format("%s: %d/%d blocks (%d%%)", buildId, blocksPlaced, totalBlocks, progressPercentage);
        }
    }

    /**
     * Blackboard subscriber implementation for ForemanEntity.
     *
     * <p>Reacts to knowledge updates and triggers agent behavior.</p>
     */
    private static class AgentBlackboardSubscriber implements BlackboardSubscriber {
        private final ForemanEntity entity;

        AgentBlackboardSubscriber(ForemanEntity entity) {
            this.entity = entity;
        }

        @Override
        public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
            if (entity == null || !entity.isAlive()) {
                return;
            }

            switch (area) {
                case THREATS:
                    handleThreatPosted(entry);
                    break;

                case AGENT_STATUS:
                    handleAgentStatusPosted(entry);
                    break;

                case BUILD_PLANS:
                    handleBuildPlanPosted(entry);
                    break;

                default:
                    // Ignore other areas
                    break;
            }
        }

        @Override
        public void onEntryRemoved(KnowledgeArea area, String key) {
            // Handle removals if needed
            LOGGER.debug("[{}] Entry removed from {}: {}",
                entity.getEntityName(), area.getId(), key);
        }

        /**
         * Handles threat notifications.
         */
        private void handleThreatPosted(BlackboardEntry<?> entry) {
            if (entry.getType() == BlackboardEntry.EntryType.FACT &&
                entry.getKey().startsWith("hostile_")) {

                // Check if threat is near this agent
                // Could trigger evasion or combat response
                LOGGER.debug("[{}] Threat detected: {}",
                    entity.getEntityName(), entry.getKey());
            }
        }

        /**
         * Handles agent status updates.
         */
        private void handleAgentStatusPosted(BlackboardEntry<?> entry) {
            // Could be used for coordination
            // e.g., "if another agent needs help, offer assistance"
        }

        /**
         * Handles build plan updates.
         */
        private void handleBuildPlanPosted(BlackboardEntry<?> entry) {
            // Could be used for collaborative building
            // e.g., "if a build is in progress, offer to help"
        }
    }
}
