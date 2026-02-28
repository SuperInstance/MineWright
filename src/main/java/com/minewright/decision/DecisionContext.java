package com.minewright.decision;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ForemanMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Provides context for utility-based decision making by MineWright agents.
 *
 * <p><b>Purpose:</b></p>
 * <p>DecisionContext encapsulates all relevant information about the current
 * situation that utility factors need to evaluate task desirability. This includes
 * agent state, world state, player preferences, available resources, and more.</p>
 *
 * <p><b>Context Categories:</b></p>
 * <ul>
 *   <li><b>Agent State:</b> Health, hunger, position, current task</li>
 *   <li><b>World State:</b> Time of day, weather, nearby threats</li>
 *   <li><b>Player Preferences:</b> Relationship level, historical choices</li>
 *   <li><b>Resources:</b> Available tools, materials, inventory</li>
 *   <li><b>Active Tasks:</b> Current queue and their priorities</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is immutable and thread-safe once created. All collections
 * are defensive copies or unmodifiable views.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * DecisionContext context = DecisionContext.of(foreman, tasks);
 * UtilityScore score = UtilityScore.calculate(task, context);
 * </pre>
 *
 * @see UtilityFactor
 * @see UtilityScore
 * @see TaskPrioritizer
 * @since 1.0.0
 */
public class DecisionContext {
    /**
     * The foreman entity making the decision.
     */
    private final ForemanEntity foreman;

    /**
     * The agent's current health level (0.0 to 1.0).
     */
    private final double healthLevel;

    /**
     * The agent's current food/hunger level (0.0 to 1.0).
     */
    private final double hungerLevel;

    /**
     * The agent's current position in the world.
     */
    private final BlockPos agentPosition;

    /**
     * The current game time in ticks.
     */
    private final long gameTime;

    /**
     * The current real-world date/time for time-sensitive decisions.
     */
    private final LocalDateTime realTime;

    /**
     * Whether it is currently daytime in the game.
     */
    private final boolean isDaytime;

    /**
     * Whether it is currently raining in the game.
     */
    private final boolean isRaining;

    /**
     * Whether there is a thunderstorm active.
     */
    private final boolean isThundering;

    /**
     * Collection of nearby hostile entities.
     */
    private final Collection<Entity> nearbyThreats;

    /**
     * Collection of nearby hostile entities.
     */
    private final Collection<BlockPos> nearbyResources;

    /**
     * The nearest player to the agent.
     */
    private final Player nearestPlayer;

    /**
     * Distance to the nearest player.
     */
    private final double distanceToPlayer;

    /**
     * Relationship/trust level with the player (0.0 to 1.0).
     */
    private final double relationshipLevel;

    /**
     * The agent's memory containing task history and conversation.
     */
    private final ForemanMemory memory;

    /**
     * Companion memory with learned player preferences.
     */
    private final CompanionMemory companionMemory;

    /**
     * Map of available resources and their quantities.
     */
    private final Map<String, Integer> availableResources;

    /**
     * Collection of available tools in the agent's inventory.
     */
    private final Collection<ItemStack> availableTools;

    /**
     * The currently executing task, if any.
     */
    private final Optional<Task> currentTask;

    /**
     * Collection of queued tasks pending execution.
     */
    private final Collection<Task> queuedTasks;

    /**
     * The prioritizer to use for scoring calculations.
     */
    private final TaskPrioritizer prioritizer;

    /**
     * Custom context values for extensibility.
     */
    private final Map<String, Object> customValues;

    /**
     * Private constructor - use builder pattern.
     */
    private DecisionContext(Builder builder) {
        this.foreman = builder.foreman;
        this.healthLevel = builder.healthLevel;
        this.hungerLevel = builder.hungerLevel;
        this.agentPosition = builder.agentPosition;
        this.gameTime = builder.gameTime;
        this.realTime = builder.realTime;
        this.isDaytime = builder.isDaytime;
        this.isRaining = builder.isRaining;
        this.isThundering = builder.isThundering;
        this.nearbyThreats = List.copyOf(builder.nearbyThreats);
        this.nearbyResources = List.copyOf(builder.nearbyResources);
        this.nearestPlayer = builder.nearestPlayer;
        this.distanceToPlayer = builder.distanceToPlayer;
        this.relationshipLevel = builder.relationshipLevel;
        this.memory = builder.memory;
        this.companionMemory = builder.companionMemory;
        this.availableResources = Map.copyOf(builder.availableResources);
        this.availableTools = List.copyOf(builder.availableTools);
        this.currentTask = Optional.ofNullable(builder.currentTask);
        this.queuedTasks = List.copyOf(builder.queuedTasks);
        this.prioritizer = builder.prioritizer;
        this.customValues = Map.copyOf(builder.customValues);
    }

    /**
     * Creates a decision context from a foreman entity and task queue.
     *
     * <p>This is the recommended way to create a context for most use cases.
     * It automatically extracts all relevant information from the entity.</p>
     *
     * @param foreman The foreman entity
     * @param tasks   The current task queue
     * @return A new DecisionContext with populated values
     */
    public static DecisionContext of(ForemanEntity foreman, Collection<Task> tasks) {
        Level level = foreman.level();
        BlockPos pos = foreman.blockPosition();

        // Find nearby threats (hostile mobs within 16 blocks)
        List<Entity> threats = new ArrayList<>();
        level.getEntitiesOfClass(Entity.class, foreman.getBoundingBox().inflate(16))
            .forEach(entity -> {
                if (!entity.equals(foreman) && isHostile(entity)) {
                    threats.add(entity);
                }
            });

        // Find nearest player
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double dist = foreman.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }

        // Get relationship level - use default since ForemanMemory doesn't track this yet
        double relationship = 0.5;

        return new Builder()
            .foreman(foreman)
            .healthLevel(foreman.getHealth() / foreman.getMaxHealth())
            .hungerLevel(1.0) // Mobs don't have hunger like players do
            .agentPosition(pos)
            .gameTime(level.getDayTime() % 24000)
            .realTime(LocalDateTime.now())
            .isDaytime(level.isDay())
            .isRaining(level.isRaining())
            .isThundering(level.isThundering())
            .nearbyThreats(threats)
            .nearestPlayer(nearest)
            .distanceToPlayer(nearest != null ? Math.sqrt(nearestDist) : Double.MAX_VALUE)
            .relationshipLevel(relationship)
            .memory(foreman.getMemory())
            .companionMemory(null) // Could be enhanced to include companion memory
            .queuedTasks(tasks)
            .build();
    }

    /**
     * Checks if an entity is hostile.
     */
    private static boolean isHostile(Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Monster
            || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
            || entity.getType().getClass().toString().contains("hostile");
    }

    /**
     * Creates a new builder for constructing DecisionContext instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public ForemanEntity getForeman() {
        return foreman;
    }

    public double getHealthLevel() {
        return healthLevel;
    }

    public double getHungerLevel() {
        return hungerLevel;
    }

    public BlockPos getAgentPosition() {
        return agentPosition;
    }

    public long getGameTime() {
        return gameTime;
    }

    public LocalDateTime getRealTime() {
        return realTime;
    }

    public boolean isDaytime() {
        return isDaytime;
    }

    public boolean isRaining() {
        return isRaining;
    }

    public boolean isThundering() {
        return isThundering;
    }

    public Collection<Entity> getNearbyThreats() {
        return nearbyThreats;
    }

    public Collection<BlockPos> getNearbyResources() {
        return nearbyResources;
    }

    public Player getNearestPlayer() {
        return nearestPlayer;
    }

    public double getDistanceToPlayer() {
        return distanceToPlayer;
    }

    public double getRelationshipLevel() {
        return relationshipLevel;
    }

    public ForemanMemory getMemory() {
        return memory;
    }

    public CompanionMemory getCompanionMemory() {
        return companionMemory;
    }

    public Map<String, Integer> getAvailableResources() {
        return availableResources;
    }

    public Collection<ItemStack> getAvailableTools() {
        return availableTools;
    }

    public Optional<Task> getCurrentTask() {
        return currentTask;
    }

    public Collection<Task> getQueuedTasks() {
        return queuedTasks;
    }

    public TaskPrioritizer getPrioritizer() {
        return prioritizer;
    }

    /**
     * Gets a custom value by key.
     *
     * @param key The key to look up
     * @return Optional containing the value, or empty if not present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCustomValue(String key) {
        Object value = customValues.get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((T) value);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    /**
     * Builder pattern for constructing DecisionContext instances.
     */
    public static class Builder {
        private ForemanEntity foreman;
        private double healthLevel = 1.0;
        private double hungerLevel = 1.0;
        private BlockPos agentPosition = BlockPos.ZERO;
        private long gameTime = 0;
        private LocalDateTime realTime = LocalDateTime.now();
        private boolean isDaytime = true;
        private boolean isRaining = false;
        private boolean isThundering = false;
        private final Collection<Entity> nearbyThreats = new ArrayList<>();
        private final Collection<BlockPos> nearbyResources = new ArrayList<>();
        private Player nearestPlayer;
        private double distanceToPlayer = Double.MAX_VALUE;
        private double relationshipLevel = 0.5;
        private ForemanMemory memory;
        private CompanionMemory companionMemory;
        private final Map<String, Integer> availableResources = new HashMap<>();
        private final Collection<ItemStack> availableTools = new ArrayList<>();
        private Task currentTask;
        private final Collection<Task> queuedTasks = new ArrayList<>();
        private TaskPrioritizer prioritizer;
        private final Map<String, Object> customValues = new HashMap<>();

        public Builder foreman(ForemanEntity foreman) {
            this.foreman = foreman;
            return this;
        }

        public Builder healthLevel(double healthLevel) {
            this.healthLevel = Math.max(0.0, Math.min(1.0, healthLevel));
            return this;
        }

        public Builder hungerLevel(double hungerLevel) {
            this.hungerLevel = Math.max(0.0, Math.min(1.0, hungerLevel));
            return this;
        }

        public Builder agentPosition(BlockPos agentPosition) {
            this.agentPosition = agentPosition;
            return this;
        }

        public Builder gameTime(long gameTime) {
            this.gameTime = gameTime;
            return this;
        }

        public Builder realTime(LocalDateTime realTime) {
            this.realTime = realTime;
            return this;
        }

        public Builder isDaytime(boolean isDaytime) {
            this.isDaytime = isDaytime;
            return this;
        }

        public Builder isRaining(boolean isRaining) {
            this.isRaining = isRaining;
            return this;
        }

        public Builder isThundering(boolean isThundering) {
            this.isThundering = isThundering;
            return this;
        }

        public Builder nearbyThreats(Collection<Entity> threats) {
            this.nearbyThreats.clear();
            if (threats != null) {
                this.nearbyThreats.addAll(threats);
            }
            return this;
        }

        public Builder nearbyResources(Collection<BlockPos> resources) {
            this.nearbyResources.clear();
            if (resources != null) {
                this.nearbyResources.addAll(resources);
            }
            return this;
        }

        public Builder nearestPlayer(Player player) {
            this.nearestPlayer = player;
            return this;
        }

        public Builder distanceToPlayer(double distance) {
            this.distanceToPlayer = distance;
            return this;
        }

        public Builder relationshipLevel(double level) {
            this.relationshipLevel = Math.max(0.0, Math.min(1.0, level));
            return this;
        }

        public Builder memory(ForemanMemory memory) {
            this.memory = memory;
            return this;
        }

        public Builder companionMemory(CompanionMemory memory) {
            this.companionMemory = memory;
            return this;
        }

        public Builder addResource(String resource, int quantity) {
            this.availableResources.put(resource, quantity);
            return this;
        }

        public Builder availableResources(Map<String, Integer> resources) {
            this.availableResources.clear();
            if (resources != null) {
                this.availableResources.putAll(resources);
            }
            return this;
        }

        public Builder availableTools(Collection<ItemStack> tools) {
            this.availableTools.clear();
            if (tools != null) {
                this.availableTools.addAll(tools);
            }
            return this;
        }

        public Builder addTool(ItemStack tool) {
            if (tool != null) {
                this.availableTools.add(tool);
            }
            return this;
        }

        public Builder addNearbyResource(BlockPos resource) {
            if (resource != null) {
                this.nearbyResources.add(resource);
            }
            return this;
        }

        public Builder currentTask(Task task) {
            this.currentTask = task;
            return this;
        }

        public Builder queuedTasks(Collection<Task> tasks) {
            this.queuedTasks.clear();
            if (tasks != null) {
                this.queuedTasks.addAll(tasks);
            }
            return this;
        }

        public Builder prioritizer(TaskPrioritizer prioritizer) {
            this.prioritizer = prioritizer;
            return this;
        }

        public Builder putCustomValue(String key, Object value) {
            this.customValues.put(key, value);
            return this;
        }

        public DecisionContext build() {
            return new DecisionContext(this);
        }
    }
}
