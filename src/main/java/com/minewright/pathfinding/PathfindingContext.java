package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Set;

/**
 * Provides context and constraints for pathfinding operations.
 *
 * <p>This class encapsulates all the information needed by the pathfinder
 * to make intelligent decisions about route selection:</p>
 *
 * <h3>Core Components:</h3>
 * <ul>
 *   <li><b>World Access:</b> Level reference for block queries and validation</li>
 *   <li><b>Agent Capabilities:</b> What the agent can do (jump height, swim, climb, etc.)</li>
 *   <li><b>Goal Specification:</b> Where the agent needs to go (block, entity, area)</li>
 *   <li><b>Constraints:</b> Restrictions on path selection (avoid mobs, max range, etc.)</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * PathfindingContext context = new PathfindingContext(level, agent)
 *     .withGoal(targetPos)
 *     .withJumpHeight(1.0)
 *     .withMaxRange(200)
 *     .setCanSwim(true)
 *     .setAvoidMobs(true);
 *
 * List<PathNode> path = pathfinder.findPath(startPos, goalPos, context);
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is mutable and not thread-safe.
 * Create a new context for each pathfinding operation.</p>
 *
 * @see AStarPathfinder
 * @see MovementValidator
 */
public class PathfindingContext {
    /** The world level for block validation. */
    private final Level level;

    /** The entity performing the pathfinding (for capability queries). */
    private final Entity entity;

    /** Goal position - primary destination. */
    private BlockPos goalPos;

    /** Goal entity - dynamic target (moving entity). */
    private Entity goalEntity;

    /** Goal area - any position within this radius is acceptable. */
    private BlockPos goalAreaCenter;
    private double goalAreaRadius;

    /** Maximum pathfinding range in blocks. */
    private int maxRange = 150;

    /** Agent capabilities */
    private boolean canSwim = false;
    private boolean canClimb = false;
    private boolean canFly = false;
    private boolean canParkour = false;
    private double jumpHeight = 1.0;
    private int maxFallDistance = 3;

    /** Path constraints */
    private boolean avoidMobs = false;
    private boolean stayLit = false;
    private boolean preferWater = false;
    private boolean allowDangerousMovements = false;

    /** Blocks to avoid during pathfinding. */
    private Set<BlockPos> blocksToAvoid;

    /** Blocks to prefer during pathfinding. */
    private Set<BlockPos> blocksToPrefer;

    /** Whether to use hierarchical pathfinding for long distances. */
    private boolean useHierarchical = true;

    /** Hierarchical threshold - use chunks for distances greater than this. */
    private int hierarchicalThreshold = 64;

    /** Timeout in milliseconds for pathfinding operation. */
    private long timeoutMs = 5000;

    /** Whether to apply path smoothing after finding a route. */
    private boolean smoothPath = true;

    /** Cache key for memoization of repeated pathfinding queries. */
    private String cacheKey;

    /**
     * Creates a new pathfinding context with required parameters.
     *
     * @param level  The world level
     * @param entity The entity that will perform the movement
     */
    public PathfindingContext(Level level, Entity entity) {
        this.level = level;
        this.entity = entity;
    }

    // ========== Goal Specification ==========

    /**
     * Sets the goal position.
     *
     * @param pos The destination position
     * @return this context for chaining
     */
    public PathfindingContext withGoal(BlockPos pos) {
        this.goalPos = pos;
        this.goalEntity = null;
        this.goalAreaCenter = null;
        return this;
    }

    /**
     * Sets the goal to a moving entity.
     * The pathfinder will track the entity's current position.
     *
     * @param entity The target entity
     * @return this context for chaining
     */
    public PathfindingContext withGoalEntity(Entity entity) {
        this.goalEntity = entity;
        this.goalPos = null;
        this.goalAreaCenter = null;
        return this;
    }

    /**
     * Sets the goal to an area around a center point.
     * Any position within the radius is acceptable.
     *
     * @param center Center of the goal area
     * @param radius Radius of acceptable positions
     * @return this context for chaining
     */
    public PathfindingContext withGoalArea(BlockPos center, double radius) {
        this.goalAreaCenter = center;
        this.goalAreaRadius = radius;
        this.goalPos = null;
        this.goalEntity = null;
        return this;
    }

    // ========== Agent Capabilities ==========

    /**
     * Sets whether the agent can swim through water.
     *
     * @param canSwim true if agent can swim
     * @return this context for chaining
     */
    public PathfindingContext setCanSwim(boolean canSwim) {
        this.canSwim = canSwim;
        return this;
    }

    /**
     * Sets whether the agent can climb ladders and vines.
     *
     * @param canClimb true if agent can climb
     * @return this context for chaining
     */
    public PathfindingContext setCanClimb(boolean canClimb) {
        this.canClimb = canClimb;
        return this;
    }

    /**
     * Sets whether the agent can fly (ignores terrain).
     *
     * @param canFly true if agent can fly
     * @return this context for chaining
     */
    public PathfindingContext setCanFly(boolean canFly) {
        this.canFly = canFly;
        return this;
    }

    /**
     * Sets whether the agent can perform parkour jumps.
     *
     * @param canParkour true if agent can parkour
     * @return this context for chaining
     */
    public PathfindingContext setCanParkour(boolean canParkour) {
        this.canParkour = canParkour;
        return this;
    }

    /**
     * Sets the agent's jump height in blocks.
     *
     * @param height Maximum jump height
     * @return this context for chaining
     */
    public PathfindingContext withJumpHeight(double height) {
        this.jumpHeight = height;
        return this;
    }

    /**
     * Sets the maximum safe fall distance in blocks.
     *
     * @param distance Maximum fall distance without damage
     * @return this context for chaining
     */
    public PathfindingContext withMaxFallDistance(int distance) {
        this.maxFallDistance = distance;
        return this;
    }

    // ========== Path Constraints ==========

    /**
     * Sets whether to avoid areas with hostile mobs.
     *
     * @param avoid true if mobs should be avoided
     * @return this context for chaining
     */
    public PathfindingContext setAvoidMobs(boolean avoid) {
        this.avoidMobs = avoid;
        return this;
    }

    /**
     * Sets whether to prefer well-lit areas (light level > 7).
     *
     * @param stayLit true if should stay lit
     * @return this context for chaining
     */
    public PathfindingContext setStayLit(boolean stayLit) {
        this.stayLit = stayLit;
        return this;
    }

    /**
     * Sets whether to prefer water routes.
     *
     * @param prefer true if water is preferred
     * @return this context for chaining
     */
    public PathfindingContext setPreferWater(boolean prefer) {
        this.preferWater = prefer;
        return this;
    }

    /**
     * Sets whether to allow dangerous movements (long falls, parkour).
     *
     * @param allow true if dangerous movements allowed
     * @return this context for chaining
     */
    public PathfindingContext setAllowDangerousMovements(boolean allow) {
        this.allowDangerousMovements = allow;
        return this;
    }

    /**
     * Sets specific blocks to avoid during pathfinding.
     *
     * @param blocks Set of positions to avoid
     * @return this context for chaining
     */
    public PathfindingContext withBlocksToAvoid(Set<BlockPos> blocks) {
        this.blocksToAvoid = blocks;
        return this;
    }

    /**
     * Sets specific blocks to prefer during pathfinding.
     *
     * @param blocks Set of positions to prefer
     * @return this context for chaining
     */
    public PathfindingContext withBlocksToPrefer(Set<BlockPos> blocks) {
        this.blocksToPrefer = blocks;
        return this;
    }

    // ========== Performance Settings ==========

    /**
     * Sets whether to use hierarchical pathfinding.
     * Reduces search space for long-distance paths.
     *
     * @param use true if hierarchical pathfinding should be used
     * @return this context for chaining
     */
    public PathfindingContext setUseHierarchical(boolean use) {
        this.useHierarchical = use;
        return this;
    }

    /**
     * Sets the distance threshold for hierarchical pathfinding.
     * Distances greater than this use chunk-level planning.
     *
     * @param threshold Distance in blocks
     * @return this context for chaining
     */
    public PathfindingContext withHierarchicalThreshold(int threshold) {
        this.hierarchicalThreshold = threshold;
        return this;
    }

    /**
     * Sets the maximum pathfinding range.
     *
     * @param range Maximum range in blocks
     * @return this context for chaining
     */
    public PathfindingContext withMaxRange(int range) {
        this.maxRange = range;
        return this;
    }

    /**
     * Sets the timeout for pathfinding operation.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return this context for chaining
     */
    public PathfindingContext withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /**
     * Sets whether to smooth the path after finding it.
     *
     * @param smooth true if path should be smoothed
     * @return this context for chaining
     */
    public PathfindingContext setSmoothPath(boolean smooth) {
        this.smoothPath = smooth;
        return this;
    }

    /**
     * Sets a cache key for memoization.
     *
     * @param key Unique cache key
     * @return this context for chaining
     */
    public PathfindingContext withCacheKey(String key) {
        this.cacheKey = key;
        return this;
    }

    // ========== Getters ==========

    public Level getLevel() {
        return level;
    }

    public Entity getEntity() {
        return entity;
    }

    public BlockPos getGoalPos() {
        return goalPos;
    }

    public Entity getGoalEntity() {
        return goalEntity;
    }

    public BlockPos getGoalAreaCenter() {
        return goalAreaCenter;
    }

    public double getGoalAreaRadius() {
        return goalAreaRadius;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public boolean canSwim() {
        return canSwim;
    }

    public boolean canClimb() {
        return canClimb;
    }

    public boolean canFly() {
        return canFly;
    }

    public boolean canParkour() {
        return canParkour;
    }

    public double getJumpHeight() {
        return jumpHeight;
    }

    public int getMaxFallDistance() {
        return maxFallDistance;
    }

    public boolean shouldAvoidMobs() {
        return avoidMobs;
    }

    public boolean shouldStayLit() {
        return stayLit;
    }

    public boolean shouldPreferWater() {
        return preferWater;
    }

    public boolean allowDangerousMovements() {
        return allowDangerousMovements;
    }

    public Set<BlockPos> getBlocksToAvoid() {
        return blocksToAvoid;
    }

    public Set<BlockPos> getBlocksToPrefer() {
        return blocksToPrefer;
    }

    public boolean useHierarchical() {
        return useHierarchical;
    }

    public int getHierarchicalThreshold() {
        return hierarchicalThreshold;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean shouldSmoothPath() {
        return smoothPath;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    /**
     * Checks if this context has a valid goal specification.
     *
     * @return true if a goal (position, entity, or area) is set
     */
    public boolean hasValidGoal() {
        return goalPos != null || goalEntity != null || goalAreaCenter != null;
    }

    /**
     * Gets the current goal position, accounting for entity tracking.
     *
     * @return Current goal position, or null if no goal
     */
    public BlockPos getCurrentGoalPos() {
        if (goalPos != null) {
            return goalPos;
        }
        if (goalEntity != null && goalEntity.isAlive()) {
            return goalEntity.blockPosition();
        }
        if (goalAreaCenter != null) {
            return goalAreaCenter;
        }
        return null;
    }

    /**
     * Checks if a position is within the goal area.
     *
     * @param pos Position to check
     * @return true if position is acceptable as goal
     */
    public boolean isGoalReached(BlockPos pos) {
        if (goalPos != null) {
            return pos.equals(goalPos);
        }
        if (goalEntity != null && goalEntity.isAlive()) {
            return pos.distSqr(goalEntity.blockPosition()) <= 4.0; // Within 2 blocks
        }
        if (goalAreaCenter != null) {
            return pos.distSqr(goalAreaCenter) <= (goalAreaRadius * goalAreaRadius);
        }
        return false;
    }
}
