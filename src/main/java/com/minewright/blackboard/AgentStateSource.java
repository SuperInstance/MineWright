package com.minewright.blackboard;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Knowledge source for monitoring and posting agent status to the blackboard.
 *
 * <p><b>Purpose:</b></p>
 * <p>AgentStateSource tracks agent state changes and posts updates to
 * the {@link KnowledgeArea#AGENT_STATUS} area. This enables agents to
 * monitor each other's status for coordination and collaboration.</p>
 *
 * <p><b>Monitored Information:</b></p>
 * <ul>
 *   <li><b>Position:</b> Current coordinates and dimension</li>
 *   <li><b>Health:</b> Current and maximum health</li>
 *   <li><b>Inventory:</b> Items and quantities</li>
 *   <li><b>State:</b> IDLE, PLANNING, EXECUTING, WAITING, ERROR</li>
 *   <li><b>Current Task:</b> Task being executed</li>
 *   <li><b>Role:</b> SOLO, FOREMAN, WORKER in orchestration</li>
 * </ul>
 *
 * <p><b>Update Strategy:</b></p>
 * <p>Agent state is posted on significant changes (state transition, health change,
 * inventory change) rather than every tick to reduce blackboard traffic.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * AgentStateSource source = new AgentStateSource(foremanEntity);
 *
 * // Initial registration
 * source.registerAgent();
 *
 * // Update state when changed
 * source.postStateUpdate(AgentState.EXECUTING);
 * source.postHealthUpdate();
 * source.postInventoryUpdate();
 *
 * // Unregister when removed
 * source.unregisterAgent();
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Updates should be called from the server
 * thread to ensure state consistency.</p>
 *
 * @see KnowledgeArea
 * @see Blackboard
 * @see ForemanEntity
 * @since 1.0.0
 */
public class AgentStateSource {
    private static final Logger LOGGER = TestLogger.getLogger(AgentStateSource.class);
    /**
     * The agent being monitored.
     */
    private final ForemanEntity agent;

    /**
     * The blackboard for posting status updates.
     */
    private final Blackboard blackboard;

    /**
     * Base key for this agent's entries in the blackboard.
     */
    private final String baseKey;

    /**
     * Last posted state to avoid duplicate posts.
     */
    private AgentState lastPostedState = null;

    /**
     * Last posted health to avoid duplicate posts.
     */
    private double lastPostedHealth = -1.0;

    /**
     * Counter for status updates posted.
     */
    private int updatesPosted = 0;

    /**
     * Creates a new agent state source.
     *
     * @param agent The agent to monitor
     */
    public AgentStateSource(ForemanEntity agent) {
        this(agent, Blackboard.getInstance());
    }

    /**
     * Creates a new agent state source with a specific blackboard.
     *
     * @param agent The agent to monitor
     * @param blackboard The blackboard for posting updates
     */
    public AgentStateSource(ForemanEntity agent, Blackboard blackboard) {
        this.agent = agent;
        this.blackboard = blackboard;
        this.baseKey = agent.getUUID().toString().substring(0, 8);
    }

    /**
     * Registers this agent with the blackboard.
     *
     * <p>Posts initial status information including position, health,
     * state, and role. Call this when the agent spawns.</p>
     */
    public void registerAgent() {
        LOGGER.info("Registering agent {} with blackboard", agent.getEntityName());

        postFullStatus();
        postPosition();
        postHealth();
        postInventory();
        postRole();

        LOGGER.debug("Agent {} registered with blackboard", agent.getEntityName());
    }

    /**
     * Unregisters this agent from the blackboard.
     *
     * <p>Removes all agent-related entries. Call this when the agent is removed.</p>
     */
    public void unregisterAgent() {
        LOGGER.info("Unregistering agent {} from blackboard", agent.getEntityName());

        // Remove all agent entries
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey);
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey + "_position");
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey + "_health");
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey + "_inventory");
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey + "_role");
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey + "_state");
        blackboard.remove(KnowledgeArea.AGENT_STATUS, baseKey + "_task");

        LOGGER.debug("Agent {} unregistered from blackboard", agent.getEntityName());
    }

    /**
     * Posts a full status update for this agent.
     *
     * <p>Consolidates position, health, state, and task information into a single entry.</p>
     */
    public void postFullStatus() {
        AgentStatus status = new AgentStatus(
            agent.getEntityName(),
            agent.getUUID(),
            agent.blockPosition().toString(),
            agent.getHealth(),
            agent.getMaxHealth(),
            getCurrentState(),
            agent.getRole().name(),
            agent.getActionExecutor().getCurrentGoal()
        );

        BlackboardEntry<AgentStatus> entry = BlackboardEntry.createFact(baseKey, status, agent.getUUID());
        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        updatesPosted++;
    }

    /**
     * Posts the agent's current position to the blackboard.
     *
     * <p>Position is posted as a FACT with high confidence since it's directly observed.</p>
     */
    public void postPosition() {
        if (agent == null || agent.level() == null) {
            return;
        }

        String key = baseKey + "_position";
        PositionInfo pos = new PositionInfo(
            agent.blockPosition().getX(),
            agent.blockPosition().getY(),
            agent.blockPosition().getZ(),
            agent.level().dimension().location().toString()
        );

        BlackboardEntry<PositionInfo> entry = BlackboardEntry.createFact(key, pos, agent.getUUID());
        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        updatesPosted++;
    }

    /**
     * Posts the agent's health to the blackboard.
     *
     * <p>Only posts if health has changed significantly (more than 1 point)
     * to reduce blackboard traffic.</p>
     */
    public void postHealth() {
        if (agent == null) {
            return;
        }

        double currentHealth = agent.getHealth();

        // Only post if health changed by more than 1 point
        if (Math.abs(currentHealth - lastPostedHealth) < 1.0) {
            return;
        }

        String key = baseKey + "_health";
        HealthInfo health = new HealthInfo(
            currentHealth,
            agent.getMaxHealth(),
            currentHealth / agent.getMaxHealth()
        );

        BlackboardEntry<HealthInfo> entry = BlackboardEntry.createFact(key, health, agent.getUUID());
        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);

        lastPostedHealth = currentHealth;
        updatesPosted++;
    }

    /**
     * Posts the agent's inventory to the blackboard.
     *
     * <p>Inventory is posted as a summary with item counts rather than full
     * details to reduce blackboard size.</p>
     *
     * <p>Note: ForemanEntity does not have a player inventory, so this posts
     * a placeholder indicating no inventory.</p>
     */
    public void postInventory() {
        if (agent == null) {
            return;
        }

        String key = baseKey + "_inventory";
        // ForemanEntity does not have inventory, return empty info
        InventoryInfo inventory = new InventoryInfo(0, new HashMap<>());

        BlackboardEntry<InventoryInfo> entry = BlackboardEntry.createFact(key, inventory, agent.getUUID());
        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        updatesPosted++;
    }

    /**
     * Posts the agent's current execution state to the blackboard.
     *
     * <p>Only posts if the state has changed to avoid duplicates.</p>
     *
     * @param state Current agent state
     */
    public void postStateUpdate(AgentState state) {
        if (state == null || state == lastPostedState) {
            return;
        }

        String key = baseKey + "_state";

        BlackboardEntry<String> entry = new BlackboardEntry<>(
            key,
            state.name(),
            agent.getUUID(),
            1.0,
            BlackboardEntry.EntryType.FACT
        );

        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);

        lastPostedState = state;
        updatesPosted++;

        LOGGER.debug("Agent {} state updated: {}", agent.getEntityName(), state);
    }

    /**
     * Posts the agent's orchestration role to the blackboard.
     *
     * <p>Role changes are rare but important for coordination.</p>
     */
    public void postRole() {
        if (agent == null) {
            return;
        }

        String key = baseKey + "_role";

        BlackboardEntry<String> entry = BlackboardEntry.createFact(
            key,
            agent.getRole().name(),
            agent.getUUID()
        );

        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        updatesPosted++;
    }

    /**
     * Posts the agent's current task to the blackboard.
     *
     * <p>Updates when the agent starts, completes, or changes tasks.</p>
     *
     * @param taskDescription Description of the current task
     */
    public void postCurrentTask(String taskDescription) {
        if (taskDescription == null || taskDescription.isEmpty()) {
            return;
        }

        String key = baseKey + "_task";

        BlackboardEntry<String> entry = new BlackboardEntry<>(
            key,
            taskDescription,
            agent.getUUID(),
            0.9,
            BlackboardEntry.EntryType.GOAL
        );

        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        updatesPosted++;
    }

    /**
     * Posts an action completion event to the blackboard.
     *
     * <p>Other agents can use this to track progress and adjust their plans.</p>
     *
     * @param action Description of the completed action
     * @param success Whether the action succeeded
     */
    public void postActionCompletion(String action, boolean success) {
        String key = baseKey + "_action_" + System.currentTimeMillis();

        ActionCompletionEvent event = new ActionCompletionEvent(
            action,
            success,
            System.currentTimeMillis()
        );

        BlackboardEntry<ActionCompletionEvent> entry = BlackboardEntry.createFact(
            key,
            event,
            agent.getUUID()
        );

        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);
        updatesPosted++;
    }

    /**
     * Gets the number of updates posted by this source.
     *
     * @return Update count
     */
    public int getUpdatesPosted() {
        return updatesPosted;
    }

    /**
     * Gets the current agent state from the executor.
     *
     * @return Current state name
     */
    private String getCurrentState() {
        if (agent.getActionExecutor() != null &&
            agent.getActionExecutor().getStateMachine() != null) {
            return agent.getActionExecutor().getStateMachine().getCurrentState().name();
        }
        return "UNKNOWN";
    }

    /**
     * Summarizes the agent's inventory into a count map.
     *
     * <p>Note: ForemanEntity does not have inventory, so this returns
     * an empty map. This method is kept for potential future use if
     * inventory functionality is added.</p>
     *
     * @return Empty map (ForemanEntity has no inventory)
     */
    private Map<String, Integer> summarizeInventory() {
        // ForemanEntity does not have inventory
        // This method is kept for potential future use
        return new HashMap<>();
    }

    /**
     * Data class for full agent status.
     */
    public record AgentStatus(
        String name,
        UUID uuid,
        String position,
        double health,
        double maxHealth,
        String state,
        String role,
        String currentGoal
    ) {
        @Override
        public String toString() {
            return String.format("%s (%s) - %s - Health: %.1f/%.1f",
                name, role, state, health, maxHealth);
        }
    }

    /**
     * Data class for position information.
     */
    public record PositionInfo(
        int x,
        int y,
        int z,
        String dimension
    ) {
        @Override
        public String toString() {
            return String.format("(%d, %d, %d) in %s", x, y, z, dimension);
        }
    }

    /**
     * Data class for health information.
     */
    public record HealthInfo(
        double current,
        double maximum,
        double percentage
    ) {
        @Override
        public String toString() {
            return String.format("%.1f/%.1f (%.1f%%)", current, maximum, percentage * 100);
        }
    }

    /**
     * Data class for inventory information.
     */
    public record InventoryInfo(
        int slotCount,
        Map<String, Integer> itemSummary
    ) {
        @Override
        public String toString() {
            return String.format("%d slots, %d item types", slotCount, itemSummary.size());
        }
    }

    /**
     * Data class for action completion events.
     */
    public record ActionCompletionEvent(
        String action,
        boolean success,
        long completedAt
    ) {
        @Override
        public String toString() {
            return String.format("%s - %s at %d", action, success ? "SUCCESS" : "FAILURE", completedAt);
        }
    }
}
