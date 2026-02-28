package com.minewright.pathfinding;

import com.minewright.MineWrightMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes a pre-computed path tick-by-tick for an entity.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li><b>Path Following:</b> Guides entity along path nodes sequentially</li>
 *   <li><b>Progress Tracking:</b> Knows current position in path</li>
 *   <li><b>Interruption Handling:</b> Detects when path becomes invalid (block changes)</li>
 *   <li><b>Recovery:</b> Can request re-pathing when stuck</li>
 *   <li><b>Completion:</b> Notifies when destination is reached</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * // Find path
 * List<PathNode> path = pathfinder.findPath(start, goal, context).get();
 *
 * // Create executor
 * PathExecutor executor = new PathExecutor(entity, path);
 *
 * // Tick each game tick
 * executor.tick();
 *
 * // Check status
 * if (executor.isComplete()) { ... }
 * if (executor.isStuck()) { ... }
 * }</pre>
 *
 * <h3>Integration:</h3>
 * <p>This class is designed to work with PathfindAction. The action delegates
 * to PathExecutor for actual movement, allowing for rich pathfinding behavior
 * while maintaining the action-based architecture.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. It should be used
 * from a single thread (typically the Minecraft server thread).</p>
 *
 * @see PathNode
 * @see AStarPathfinder
 * @see com.minewright.action.actions.PathfindAction
 */
public class PathExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PathExecutor.class);

    /** Distance threshold to consider a node reached (in blocks). */
    private static final double NODE_REACHED_DISTANCE = 1.5;

    /** Distance threshold to consider final goal reached (in blocks). */
    private static final double GOAL_REACHED_DISTANCE = 2.0;

    /** Number of ticks without progress before considering entity stuck. */
    private static final int STUCK_TICK_THRESHOLD = 60; // 3 seconds

    /** Number of ticks to wait before requesting re-path. */
    private static final int REPATH_DELAY_TICKS = 20; // 1 second

    /** Entity being moved. */
    private final Mob entity;

    /** Path to follow. */
    private final List<PathNode> path;

    /** Current index in the path. */
    private int currentIndex;

    /** Current target node. */
    private PathNode currentNode;

    /** Whether execution is complete. */
    private boolean complete;

    /** Whether execution failed. */
    private boolean failed;

    /** Failure reason if failed. */
    private String failureReason;

    /** Entity position when last node was reached. */
    private Vec3 lastProgressPosition;

    /** Ticks since last made progress. */
    private int stuckTicks;

    /** Ticks since re-path was requested. */
    private int repathRequestTicks;

    /** Whether re-pathing has been requested. */
    private boolean repathRequested;

    /** Callback for when path is complete. */
    private Runnable onCompleteCallback;

    /** Callback for when path fails. */
    private Runnable onFailureCallback;

    /** Callback for when re-pathing is needed. */
    private Runnable repathCallback;

    /** Additional data attached to this executor. */
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * Creates a new path executor.
     *
     * @param entity Entity to move
     * @param path   Path to follow
     */
    public PathExecutor(Mob entity, List<PathNode> path) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        this.entity = entity;
        this.path = new ArrayList<>(path); // Defensive copy
        this.currentIndex = 0;
        this.currentNode = this.path.get(0);
        this.lastProgressPosition = entity.position();
    }

    /**
     * Main tick method - call each game tick.
     *
     * <p>This method:</p>
 * <ol>
     *   <li>Checks if entity has reached the current node</li>
     *   <li>Advances to next node if reached</li>
     *   <li>Checks if final goal is reached</li>
     *   <li>Detects if entity is stuck</li>
     *   <li>Updates entity navigation to target current node</li>
     * </ol>
     */
    public void tick() {
        if (complete || failed) {
            return; // Already done
        }

        // Check if entity is stuck
        checkStuck();

        // Check if we need to request re-path
        if (repathRequested) {
            repathRequestTicks++;
            if (repathRequestTicks >= REPATH_DELAY_TICKS && repathCallback != null) {
                repathCallback.run();
                repathRequested = false;
                repathRequestTicks = 0;
            }
            return; // Don't execute while waiting for re-path
        }

        // Check if reached current node
        if (isCurrentNodeReached()) {
            advanceToNextNode();
        }

        // Check if reached final goal
        if (currentIndex >= path.size() - 1 && isGoalReached()) {
            complete();
            return;
        }

        // Update navigation to move toward current node
        updateNavigation();
    }

    /**
     * Checks if the current node has been reached.
     *
     * @return true if entity is close enough to current node
     */
    private boolean isCurrentNodeReached() {
        if (currentNode == null) {
            return false;
        }

        BlockPos entityPos = entity.blockPosition();
        double distance = Math.sqrt(entityPos.distSqr(currentNode.pos));

        return distance <= NODE_REACHED_DISTANCE;
    }

    /**
     * Checks if the final goal has been reached.
     *
     * @return true if entity is close enough to goal
     */
    private boolean isGoalReached() {
        if (path.isEmpty()) {
            return true;
        }

        BlockPos goal = path.get(path.size() - 1).pos;
        BlockPos entityPos = entity.blockPosition();
        double distance = Math.sqrt(entityPos.distSqr(goal));

        return distance <= GOAL_REACHED_DISTANCE;
    }

    /**
     * Advances to the next node in the path.
     */
    private void advanceToNextNode() {
        currentIndex++;

        if (currentIndex >= path.size()) {
            // Path complete - verify goal reached
            if (isGoalReached()) {
                complete();
            } else {
                // Reached last node but not goal - should be close enough
                complete();
            }
            return;
        }

        currentNode = path.get(currentIndex);
        lastProgressPosition = entity.position();
        stuckTicks = 0;

        LOGGER.debug("[PathExecutor] {} advanced to node {}/{}: {}",
            entity.getName().getString(), currentIndex + 1, path.size(), currentNode.pos);
    }

    /**
     * Updates the entity's navigation to target the current node.
     */
    private void updateNavigation() {
        if (currentNode == null) {
            return;
        }

        // Use Minecraft's built-in navigation
        if (entity.getNavigation() != null) {
            BlockPos target = currentNode.pos;

            // Check if movement type affects speed
            double speedMultiplier = getSpeedMultiplier();
            entity.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speedMultiplier);
        }
    }

    /**
     * Gets speed multiplier based on current movement type.
     *
     * @return Speed multiplier (1.0 = normal)
     */
    private double getSpeedMultiplier() {
        if (currentNode == null || currentNode.movement == null) {
            return 1.0;
        }

        // Movement cost inversely relates to speed
        // Higher cost = slower movement
        return 1.0 / Math.max(0.5, currentNode.movement.getCost() * 0.5);
    }

    /**
     * Checks if entity is stuck and needs re-pathing.
     */
    private void checkStuck() {
        Vec3 currentPos = entity.position();
        double distanceMoved = lastProgressPosition.distanceTo(currentPos);

        if (distanceMoved < 0.1) { // Barely moved
            stuckTicks++;

            if (stuckTicks >= STUCK_TICK_THRESHOLD) {
                LOGGER.debug("[PathExecutor] {} appears stuck at {}",
                    entity.getName().getString(), entity.blockPosition());
                requestRepath("Entity stuck");
            }
        } else {
            // Making progress
            stuckTicks = 0;
            lastProgressPosition = currentPos;
        }
    }

    /**
     * Marks execution as complete.
     */
    private void complete() {
        complete = true;

        if (entity.getNavigation() != null) {
            entity.getNavigation().stop();
        }

        LOGGER.debug("[PathExecutor] {} reached destination after {} nodes",
            entity.getName().getString(), currentIndex + 1);

        if (onCompleteCallback != null) {
            try {
                onCompleteCallback.run();
            } catch (Exception e) {
                LOGGER.error("[PathExecutor] Error in complete callback", e);
            }
        }
    }

    /**
     * Marks execution as failed.
     *
     * @param reason Failure reason
     */
    public void fail(String reason) {
        failed = true;
        this.failureReason = reason;

        if (entity.getNavigation() != null) {
            entity.getNavigation().stop();
        }

        LOGGER.debug("[PathExecutor] {} failed: {}",
            entity.getName().getString(), reason);

        if (onFailureCallback != null) {
            try {
                onFailureCallback.run();
            } catch (Exception e) {
                LOGGER.error("[PathExecutor] Error in failure callback", e);
            }
        }
    }

    /**
     * Requests that the path be recomputed.
     *
     * @param reason Why re-pathing is needed
     */
    public void requestRepath(String reason) {
        LOGGER.debug("[PathExecutor] {} requesting re-path: {}",
            entity.getName().getString(), reason);

        repathRequested = true;
        repathRequestTicks = 0;

        if (repathCallback != null) {
            repathCallback.run();
        }
    }

    // ========== Public API ==========

    /**
     * Checks if execution is complete.
     *
     * @return true if path has been fully traversed
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Checks if execution has failed.
     *
     * @return true if path execution failed
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Checks if entity is stuck.
     *
     * @return true if entity hasn't made progress recently
     */
    public boolean isStuck() {
        return stuckTicks >= STUCK_TICK_THRESHOLD;
    }

    /**
     * Gets the current target node.
     *
     * @return Current node, or null if complete
     */
    public PathNode getCurrentNode() {
        return currentNode;
    }

    /**
     * Gets the current progress (0.0 to 1.0).
     *
     * @return Progress fraction
     */
    public double getProgress() {
        if (path.isEmpty()) {
            return 1.0;
        }
        return (double) currentIndex / path.size();
    }

    /**
     * Gets the failure reason if failed.
     *
     * @return Failure reason, or null if not failed
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Sets callback for completion.
     *
     * @param callback Callback to run when complete
     * @return this for chaining
     */
    public PathExecutor onComplete(Runnable callback) {
        this.onCompleteCallback = callback;
        return this;
    }

    /**
     * Sets callback for failure.
     *
     * @param callback Callback to run when failed
     * @return this for chaining
     */
    public PathExecutor onFailure(Runnable callback) {
        this.onFailureCallback = callback;
        return this;
    }

    /**
     * Sets callback for re-pathing request.
     *
     * @param callback Callback to run when re-pathing is needed
     * @return this for chaining
     */
    public PathExecutor onRepath(Runnable callback) {
        this.repathCallback = callback;
        return this;
    }

    /**
     * Gets metadata value.
     *
     * @param key Metadata key
     * @return Value, or null if not set
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Sets metadata value.
     *
     * @param key   Metadata key
     * @param value Metadata value
     * @return this for chaining
     */
    public PathExecutor setMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Stops path execution.
     */
    public void stop() {
        complete = true;

        if (entity.getNavigation() != null) {
            entity.getNavigation().stop();
        }
    }

    /**
     * Gets a snapshot of current execution state.
     *
     * @return State snapshot
     */
    public PathState getState() {
        return new PathState(
            currentIndex,
            getProgress(),
            isComplete(),
            isFailed(),
            isStuck(),
            currentNode != null ? currentNode.pos : null
        );
    }

    /**
     * Snapshot of path execution state.
     */
    public record PathState(
        int currentIndex,
        double progress,
        boolean complete,
        boolean failed,
        boolean stuck,
        BlockPos currentPosition
    ) {
        @Override
        public String toString() {
            return String.format("PathState[index=%d, progress=%.1f%%, %s%s%s]",
                currentIndex, progress * 100,
                complete ? "COMPLETE" : "",
                failed ? "FAILED" : "",
                stuck ? "STUCK" : "IN_PROGRESS");
        }
    }

    // ========== Static Utilities ==========

    /**
     * Path cache for sharing paths between entities.
     * Key: "startX,startY,startZ->goalX,goalY,goalZ"
     * Value: List of PathNodes
     */
    private static final Map<String, CachedPath> pathCache = new ConcurrentHashMap<>();

    /**
     * Maximum age of cached paths in milliseconds.
     */
    private static final long CACHE_MAX_AGE_MS = 30000; // 30 seconds

    /**
     * Gets a cached path if available and not expired.
     *
     * @param start Start position
     * @param goal  Goal position
     * @return Cached path, or null if not found or expired
     */
    public static List<PathNode> getCachedPath(BlockPos start, BlockPos goal) {
        String key = makeCacheKey(start, goal);
        CachedPath cached = pathCache.get(key);

        if (cached != null && !cached.isExpired()) {
            LOGGER.debug("[PathExecutor] Using cached path from {} to {}", start, goal);
            return cached.path;
        }

        // Remove expired entry
        if (cached != null) {
            pathCache.remove(key);
        }

        return null;
    }

    /**
     * Caches a path for future use.
     *
     * @param start Start position
     * @param goal  Goal position
     * @param path  Path to cache
     */
    public static void cachePath(BlockPos start, BlockPos goal, List<PathNode> path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        String key = makeCacheKey(start, goal);
        pathCache.put(key, new CachedPath(path, System.currentTimeMillis()));

        // Clean old entries if cache is too large
        if (pathCache.size() > 100) {
            pathCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    /**
     * Clears the path cache.
     */
    public static void clearPathCache() {
        pathCache.clear();
    }

    /**
     * Makes a cache key from two positions.
     */
    private static String makeCacheKey(BlockPos start, BlockPos goal) {
        return String.format("%d,%d,%d->%d,%d,%d",
            start.getX(), start.getY(), start.getZ(),
            goal.getX(), goal.getY(), goal.getZ());
    }

    /**
     * Cached path with timestamp.
     */
    private static class CachedPath {
        private final List<PathNode> path;
        private final long timestamp;

        CachedPath(List<PathNode> path, long timestamp) {
            this.path = path;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_MAX_AGE_MS;
        }

        List<PathNode> getPath() {
            return path;
        }

        long getTimestamp() {
            return timestamp;
        }
    }
}
