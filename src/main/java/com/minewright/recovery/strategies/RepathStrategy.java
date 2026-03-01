package com.minewright.recovery.strategies;

import com.minewright.entity.ForemanEntity;
import com.minewright.pathfinding.AStarPathfinder;
import com.minewright.pathfinding.HierarchicalPathfinder;
import com.minewright.pathfinding.MovementValidator;
import com.minewright.pathfinding.PathNode;
import com.minewright.pathfinding.PathfindingContext;
import com.minewright.recovery.RecoveryResult;
import com.minewright.recovery.RecoveryStrategy;
import com.minewright.recovery.StuckType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Recovery strategy that attempts to re-path the agent.
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 *   <li>Agent is physically blocked (POSITION_STUCK)</li>
 *   <li>Pathfinding cannot find path (PATH_STUCK)</li>
 *   <li>Agent moving in wrong direction (PROGRESS_STUCK)</li>
 * </ul>
 *
 * <p><b>How it Works:</b></p>
 * <ol>
 *   <li>Stops current navigation</li>
 *   <li>Attempts to find new path using hierarchical pathfinding</li>
 *   <li>Falls back to A* if hierarchical fails</li>
 *   <li>If successful, sets new path on entity navigation</li>
 *   <li>If failed, returns ESCALATE to try next strategy</li>
 * </ol>
 *
 * <p><b>Best Practices:</b></p>
 * <ul>
 *   <li>Always check reachability before setting path</li>
 *   <li>Log pathfinding failures for debugging</li>
 *   <li>Notify user if repathing fails multiple times</li>
 *   <li>Clear path stuck status on success</li>
 * </ul>
 *
 * @since 1.1.0
 * @see RecoveryStrategy
 * @see StuckType
 */
public class RepathStrategy implements RecoveryStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepathStrategy.class);

    /** Maximum repath attempts before escalating. */
    private static final int MAX_REPATH_ATTEMPTS = 3;

    /** Maximum pathfinding timeout in milliseconds. */
    private static final long PATHFINDING_TIMEOUT_MS = 5000;

    /** Maximum path distance for direct A* (blocks). */
    private static final int DIRECT_PATH_MAX_DISTANCE = 100;

    @Override
    public boolean canRecover(StuckType type, ForemanEntity entity) {
        return type == StuckType.POSITION_STUCK
            || type == StuckType.PATH_STUCK
            || type == StuckType.PROGRESS_STUCK;
    }

    @Override
    public RecoveryResult execute(ForemanEntity entity) {
        LOGGER.info("[RepathStrategy] Attempting to re-path {}",
            entity.getEntityName());

        try {
            // Get current target from navigation
            BlockPos target = getCurrentTarget(entity);
            if (target == null) {
                LOGGER.warn("[RepathStrategy] No current target to re-path to");
                return RecoveryResult.ESCALATE;
            }

            BlockPos start = entity.blockPosition();
            Level level = entity.level();

            // Check if target is reachable
            if (!isReachable(entity, target)) {
                LOGGER.warn("[RepathStrategy] Target {} is unreachable from {}",
                    target, start);
                entity.sendChatMessage("I can't find a way there, boss!");
                return RecoveryResult.ESCALATE;
            }

            // Try to find new path
            Optional<List<PathNode>> newPath = findNewPath(entity, start, target);

            if (newPath.isPresent()) {
                // Set new path on navigation
                setNewPath(entity, newPath.get(), target);

                LOGGER.info("[RepathStrategy] Successfully re-pathed {} to {} ({} nodes)",
                    entity.getEntityName(), target, newPath.get().size());

                entity.sendChatMessage("Found a new way!");
                return RecoveryResult.SUCCESS;
            } else {
                LOGGER.warn("[RepathStrategy] Could not find new path for {}",
                    entity.getEntityName());
                return RecoveryResult.ESCALATE;
            }

        } catch (Exception e) {
            LOGGER.error("[RepathStrategy] Error during repathing", e);
            return RecoveryResult.RETRY;
        }
    }

    @Override
    public int getMaxAttempts() {
        return MAX_REPATH_ATTEMPTS;
    }

    /**
     * Gets the current navigation target from entity.
     *
     * @param entity The entity
     * @return Current target position, or null if none
     */
    private BlockPos getCurrentTarget(ForemanEntity entity) {
        // Try to get target from current action
        var currentAction = entity.getActionExecutor().getCurrentAction();
        if (currentAction != null) {
            // Check if action has a target position
            BlockPos target = extractTargetFromAction(currentAction);
            if (target != null) {
                return target;
            }
        }

        // Fall back to navigation target
        if (entity.getNavigation() != null && entity.getNavigation().isInProgress()) {
            net.minecraft.world.level.pathfinder.Path path = entity.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                return path.getTarget();
            }
        }

        return null;
    }

    /**
     * Extracts target position from current action if available.
     *
     * @param action The current action
     * @return Target position, or null if action doesn't have one
     */
    private BlockPos extractTargetFromAction(com.minewright.action.actions.BaseAction action) {
        // This would need to be implemented based on action structure
        // For now, return null - let navigation handle it
        return null;
    }

    /**
     * Checks if target is reachable from current position.
     *
     * @param entity The entity
     * @param target Target position
     * @return true if reachable
     */
    private boolean isReachable(ForemanEntity entity, BlockPos target) {
        Level level = entity.level();
        BlockPos start = entity.blockPosition();

        // Quick distance check
        double distance = Math.sqrt(start.distSqr(target));
        if (distance > DIRECT_PATH_MAX_DISTANCE * 2) {
            // Too far - may be unreachable
            return false;
        }

        // Use movement validator to check reachability
        MovementValidator validator = new MovementValidator();
        PathfindingContext context = new PathfindingContext(level, entity);

        // Check if target is valid for movement (has solid ground and headroom)
        return validator.hasSolidGround(level, target) && validator.hasHeadroom(level, target);
    }

    /**
     * Finds a new path using hierarchical or A* pathfinding.
     *
     * @param entity The entity
     * @param start  Start position
     * @param target Target position
     * @return New path, or empty if not found
     */
    private Optional<List<PathNode>> findNewPath(ForemanEntity entity, BlockPos start, BlockPos target) {
        Level level = entity.level();
        PathfindingContext context = new PathfindingContext(level, entity);

        // Calculate distance
        double distance = Math.sqrt(start.distSqr(target));

        // Try hierarchical pathfinding first for long distances
        if (distance > DIRECT_PATH_MAX_DISTANCE) {
            LOGGER.debug("[RepathStrategy] Using hierarchical pathfinding for distance {}",
                distance);

            HierarchicalPathfinder hierarchical = new HierarchicalPathfinder();
            Optional<List<PathNode>> path = hierarchical.findPath(start, target, context);

            if (path.isPresent()) {
                return path;
            }

            LOGGER.debug("[RepathStrategy] Hierarchical pathfinding failed, falling back to A*");
        }

        // Fall back to A* pathfinding
        LOGGER.debug("[RepathStrategy] Using A* pathfinding");
        AStarPathfinder pathfinder = new AStarPathfinder();
        return pathfinder.findPath(start, target, context);
    }

    /**
     * Sets the new path on entity navigation.
     *
     * @param entity The entity
     * @param path   New path
     * @param target Target position
     */
    private void setNewPath(ForemanEntity entity, List<PathNode> path, BlockPos target) {
        // Stop current navigation
        if (entity.getNavigation() != null) {
            entity.getNavigation().stop();
        }

        // Set new path
        if (entity.getNavigation() != null && !path.isEmpty()) {
            // Use first node as immediate target
            PathNode firstNode = path.get(0);
            entity.getNavigation().moveTo(
                firstNode.pos.getX(),
                firstNode.pos.getY(),
                firstNode.pos.getZ(),
                1.0 // Normal speed
            );

            LOGGER.debug("[RepathStrategy] Set navigation to first node: {}", firstNode.pos);
        }
    }
}
