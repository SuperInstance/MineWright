package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.pathfinding.*;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Action for pathfinding to a target position using enhanced A* algorithm.
 *
 * <p>This action has been significantly enhanced with:</p>
 * <ul>
 *   <li><b>Baritone-inspired A*:</b> Advanced pathfinding with multiple heuristics</li>
 *   <li><b>Hierarchical Pathfinding:</b> Chunk-level planning for long distances</li>
 *   <li><b>Path Smoothing:</b> Removes unnecessary waypoints for efficient movement</li>
 *   <li><b>Movement Validation:</b> Considers all Minecraft movement mechanics</li>
 *   <li><b>Path Caching:</b> Shares paths between agents for efficiency</li>
 *   <li><b>Stuck Detection:</b> Automatically re-paths when blocked</li>
 * </ul>
 *
 * <h3>Task Parameters:</h3>
 * <ul>
 *   <li><b>x, y, z:</b> Target coordinates (required unless using area goal)</li>
 *   <li><b>goalAreaX, goalAreaY, goalAreaZ, goalAreaRadius:</b> Alternative area goal</li>
 *   <li><b>maxRange:</b> Maximum pathfinding distance (default: 150)</li>
 *   <li><b>timeout:</b> Pathfinding timeout in milliseconds (default: 5000)</li>
 *   <li><b>allowDangerous:</b> Allow dangerous movements (default: false)</li>
 *   <li><b>useCache:</b> Use cached paths when available (default: true)</li>
 * </ul>
 *
 * <h3>Integration:</h3>
 * <p>This action integrates with the new pathfinding system while maintaining
 * compatibility with the existing action-based architecture.</p>
 *
 * @see com.minewright.pathfinding.AStarPathfinder
 * @see com.minewright.pathfinding.HierarchicalPathfinder
 * @see com.minewright.pathfinding.PathExecutor
 */
public class PathfindAction extends BaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(PathfindAction.class);

    /** Default maximum pathfinding range in blocks. */
    private static final int DEFAULT_MAX_RANGE = 150;

    /** Default pathfinding timeout in milliseconds. */
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    /** Maximum ticks before giving up (30 seconds). */
    private static final int MAX_TICKS = 600;

    /** Target position. */
    private BlockPos targetPos;

    /** Path executor for following the computed path. */
    private PathExecutor pathExecutor;

    /** Ticks since action started. */
    private int ticksRunning;

    /** Whether enhanced pathfinding is enabled. */
    private boolean useEnhancedPathfinding = true;

    /** Pathfinding context. */
    private PathfindingContext pathfindingContext;

    /**
     * Creates a new PathfindAction.
     *
     * @param foreman The entity executing this action
     * @param task    The task with target position
     */
    public PathfindAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null) {
            fail("Foreman entity is null", false);
            return;
        }

        // Parse target position from task
        if (!parseTargetPosition()) {
            fail("Invalid target position parameters", true);
            return;
        }

        ticksRunning = 0;

        // Check if we should use enhanced pathfinding
        if (useEnhancedPathfinding) {
            startEnhancedPathfinding();
        } else {
            startLegacyPathfinding();
        }
    }

    /**
     * Parses the target position from task parameters.
     *
     * @return true if position was successfully parsed
     */
    private boolean parseTargetPosition() {
        // Try direct x, y, z parameters first
        if (task.hasParameter("x") && task.hasParameter("y") && task.hasParameter("z")) {
            int x = task.getIntParameter("x", 0);
            int y = task.getIntParameter("y", 0);
            int z = task.getIntParameter("z", 0);
            targetPos = new BlockPos(x, y, z);
            return true;
        }

        // Try goal area parameters
        if (task.hasParameter("goalAreaX") && task.hasParameter("goalAreaY") &&
            task.hasParameter("goalAreaZ")) {
            int x = task.getIntParameter("goalAreaX", 0);
            int y = task.getIntParameter("goalAreaY", 0);
            int z = task.getIntParameter("goalAreaZ", 0);
            targetPos = new BlockPos(x, y, z);
            return true;
        }

        // Try string position parameter (e.g., "100,64,-200")
        String posStr = task.getStringParameter("position");
        if (posStr != null) {
            String[] parts = posStr.split(",");
            if (parts.length == 3) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int z = Integer.parseInt(parts[2].trim());
                    targetPos = new BlockPos(x, y, z);
                    return true;
                } catch (NumberFormatException e) {
                    LOGGER.warn("[PathfindAction] Failed to parse position: {}", posStr);
                }
            }
        }

        return false;
    }

    /**
     * Starts enhanced pathfinding using the new A* system.
     */
    private void startEnhancedPathfinding() {
        LOGGER.info("[PathfindAction] {} using enhanced pathfinding to {}",
            foreman.getEntityName(), targetPos);

        // Create pathfinding context
        pathfindingContext = createPathfindingContext();

        // Check cache first
        if (task.getBooleanParameter("useCache", true)) {
            List<PathNode> cachedPath = PathExecutor.getCachedPath(
                foreman.blockPosition(), targetPos);

            if (cachedPath != null) {
                LOGGER.debug("[PathfindAction] Using cached path");
                createPathExecutor(cachedPath);
                return;
            }
        }

        // Find path using hierarchical pathfinder
        findPath();
    }

    /**
     * Creates the pathfinding context from task parameters and entity capabilities.
     *
     * @return Configured pathfinding context
     */
    private PathfindingContext createPathfindingContext() {
        PathfindingContext context = new PathfindingContext(
            foreman.level(),
            foreman
        );

        // Set goal
        context.withGoal(targetPos);

        // Set max range
        int maxRange = task.getIntParameter("maxRange", DEFAULT_MAX_RANGE);
        context.withMaxRange(maxRange);

        // Set timeout
        long timeout = task.getLongParameter("timeout", DEFAULT_TIMEOUT_MS);
        context.withTimeout(timeout);

        // Agent capabilities
        context.setCanSwim(true); // Most entities can swim
        context.setCanClimb(true); // Can use ladders
        context.setCanFly(foreman.isFlying()); // Some entities can fly
        context.setCanParkour(task.getBooleanParameter("allowParkour", false));

        // Constraints
        context.setAllowDangerousMovements(task.getBooleanParameter("allowDangerous", false));
        context.setAvoidMobs(task.getBooleanParameter("avoidMobs", false));
        context.setStayLit(task.getBooleanParameter("stayLit", false));

        // Performance settings
        context.setUseHierarchical(true);
        context.setSmoothPath(true);

        return context;
    }

    /**
     * Finds a path using the hierarchical pathfinder.
     */
    private void findPath() {
        BlockPos start = foreman.blockPosition();

        LOGGER.debug("[PathfindAction] Finding path from {} to {}", start, targetPos);

        long startTime = System.currentTimeMillis();

        // Use hierarchical pathfinder for long distances
        HierarchicalPathfinder pathfinder = new HierarchicalPathfinder();
        Optional<List<PathNode>> pathResult = pathfinder.findPath(start, targetPos, pathfindingContext);

        long duration = System.currentTimeMillis() - startTime;

        if (pathResult.isPresent() && !pathResult.get().isEmpty()) {
            List<PathNode> path = pathResult.get();

            LOGGER.info("[PathfindAction] Path found in {}ms with {} nodes", duration, path.size());

            // Cache the path for future use
            if (task.getBooleanParameter("useCache", true)) {
                PathExecutor.cachePath(start, targetPos, path);
            }

            createPathExecutor(path);
        } else {
            LOGGER.warn("[PathfindAction] No path found to {} in {}ms", targetPos, duration);
            fail("No path found to target", true);
        }
    }

    /**
     * Creates a path executor with callbacks.
     *
     * @param path Path to follow
     */
    private void createPathExecutor(List<PathNode> path) {
        pathExecutor = new PathExecutor(foreman, path)
            .onComplete(this::onPathComplete)
            .onFailure(() -> onPathFailed("Path execution failed"))
            .onRepath(this::onRepathRequested);
    }

    /**
     * Called when path is successfully completed.
     */
    private void onPathComplete() {
        succeed("Reached destination: " + targetPos);
    }

    /**
     * Called when path execution fails.
     *
     * @param reason Failure reason
     */
    private void onPathFailed(String reason) {
        fail(reason, true);
    }

    /**
     * Called when re-pathing is requested (e.g., stuck).
     */
    private void onRepathRequested() {
        LOGGER.info("[PathfindAction] {} requesting re-path to {}",
            foreman.getEntityName(), targetPos);

        // Try to find a new path from current position
        findPath();
    }

    /**
     * Starts legacy pathfinding using vanilla Minecraft navigation.
     */
    private void startLegacyPathfinding() {
        LOGGER.info("[PathfindAction] {} using legacy pathfinding to {}",
            foreman.getEntityName(), targetPos);

        if (foreman.getNavigation() != null) {
            foreman.getNavigation().moveTo(
                targetPos.getX(),
                targetPos.getY(),
                targetPos.getZ(),
                1.0
            );
        } else {
            fail("Navigation not available", false);
        }
    }

    @Override
    protected void onTick() {
        if (foreman == null) {
            fail("Foreman entity became null", false);
            return;
        }

        ticksRunning++;

        // Check timeout
        if (ticksRunning > MAX_TICKS) {
            fail("Pathfinding timeout after " + MAX_TICKS + " ticks", true);
            return;
        }

        // Tick the path executor if using enhanced pathfinding
        if (useEnhancedPathfinding && pathExecutor != null) {
            tickEnhanced();
        } else {
            tickLegacy();
        }
    }

    /**
     * Ticks enhanced pathfinding execution.
     */
    private void tickEnhanced() {
        if (pathExecutor == null) {
            return;
        }

        // Check if already complete
        if (pathExecutor.isComplete()) {
            // Path executor should have called the callback
            return;
        }

        // Check if failed
        if (pathExecutor.isFailed()) {
            fail(pathExecutor.getFailureReason(), true);
            return;
        }

        // Tick the executor
        pathExecutor.tick();

        // Log progress periodically
        if (ticksRunning % 100 == 0) {
            PathExecutor.PathState state = pathExecutor.getState();
            LOGGER.debug("[PathfindAction] Progress: {} - {}",
                String.format("%.1f%%", state.progress() * 100),
                state.currentPosition());
        }
    }

    /**
     * Ticks legacy pathfinding execution.
     */
    private void tickLegacy() {
        if (targetPos == null) {
            return;
        }

        // Check if reached
        if (foreman.blockPosition().closerThan(targetPos, 2.0)) {
            succeed("Reached target position: " + targetPos);
            return;
        }

        // Check if navigation is done but not at target
        if (foreman.getNavigation() != null &&
            foreman.getNavigation().isDone() &&
            !foreman.blockPosition().closerThan(targetPos, 2.0)) {

            // Retry navigation
            foreman.getNavigation().moveTo(
                targetPos.getX(),
                targetPos.getY(),
                targetPos.getZ(),
                1.0
            );
        }
    }

    @Override
    protected void onCancel() {
        if (pathExecutor != null) {
            pathExecutor.stop();
        }

        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    public String getDescription() {
        if (targetPos != null) {
            return String.format("Pathfind to [%d, %d, %d]",
                targetPos.getX(), targetPos.getY(), targetPos.getZ());
        }
        return "Pathfind to unknown location";
    }

    /**
     * Gets the current path executor state.
     *
     * @return Current path state, or null if not using enhanced pathfinding
     */
    public PathExecutor.PathState getPathState() {
        return pathExecutor != null ? pathExecutor.getState() : null;
    }

    /**
     * Gets the current progress (0.0 to 1.0).
     *
     * @return Progress fraction
     */
    public double getProgress() {
        if (pathExecutor != null) {
            return pathExecutor.getProgress();
        }
        // Legacy mode: estimate by distance
        if (targetPos != null) {
            double totalDist = Math.sqrt(foreman.blockPosition().distSqr(targetPos));
            return Math.max(0, Math.min(1, 1.0 - totalDist / 50.0)); // Rough estimate
        }
        return 0;
    }
}
