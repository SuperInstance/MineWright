package com.minewright.recovery.strategies;

import com.minewright.entity.ForemanEntity;
import com.minewright.pathfinding.MovementValidator;
import com.minewright.pathfinding.PathfindingContext;
import com.minewright.recovery.RecoveryResult;
import com.minewright.recovery.RecoveryStrategy;
import com.minewright.recovery.StuckType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recovery strategy that teleports the agent to a safe location.
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 *   <li>Agent is physically stuck and cannot move (POSITION_STUCK)</li>
 *   <li>Repathing has failed multiple times</li>
 *   <li>Agent is in a dangerous location (lava, suffocation, etc.)</li>
 *   <li>Pathfinding is completely impossible (PATH_STUCK)</li>
 * </ul>
 *
 * <p><b>How it Works:</b></p>
 * <ol>
 *   <li>Finds nearest safe position near current location</li>
 *   <li>Checks if safe position is valid for standing</li>
 *   <li>Teleports entity to safe position</li>
 *   <li>Stops current navigation to prevent getting stuck again</li>
 *   <li>If no safe position found, returns ESCALATE</li>
 * </ol>
 *
 * <p><b>Safety Checks:</b></p>
 * <ul>
 *   <li>Never teleport into solid blocks</li>
 *   <li>Never teleport into hazardous blocks (lava, fire, cactus)</li>
 *   <li>Always ensure ground is solid under feet</li>
 *   <li>Always ensure head space is clear</li>
 *   <li>Prefer teleporting toward target, not away</li>
 * </ul>
 *
 * <p><b>Best Practices:</b></p>
 * <ul>
 *   <li>Use as last resort before aborting</li>
 *   <li>Always notify user when teleporting</li>
 *   <li>Log teleport location for debugging</li>
 *   <li>Reset path stuck status after teleport</li>
 * </ul>
 *
 * @since 1.1.0
 * @see RecoveryStrategy
 * @see StuckType
 * @see RepathStrategy
 */
public class TeleportStrategy implements RecoveryStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportStrategy.class);

    /** Maximum teleport attempts before escalating. */
    private static final int MAX_TELEPORT_ATTEMPTS = 2;

    /** Maximum search radius for safe position (blocks). */
    private static final int SAFE_POSITION_SEARCH_RADIUS = 10;

    /** Maximum vertical search distance (blocks). */
    private static final int VERTICAL_SEARCH_RANGE = 5;

    @Override
    public boolean canRecover(StuckType type, ForemanEntity entity) {
        return type == StuckType.POSITION_STUCK || type == StuckType.PATH_STUCK;
    }

    @Override
    public RecoveryResult execute(ForemanEntity entity) {
        LOGGER.info("[TeleportStrategy] Attempting to teleport {}",
            entity.getEntityName());

        try {
            BlockPos currentPos = entity.blockPosition();
            BlockPos safePos = findSafePosition(entity, currentPos);

            if (safePos == null) {
                LOGGER.warn("[TeleportStrategy] No safe position found for {}",
                    entity.getEntityName());
                entity.sendChatMessage("I'm trapped! Can't find a way out!");
                return RecoveryResult.ESCALATE;
            }

            // Perform teleport
            teleportTo(entity, safePos);

            LOGGER.info("[TeleportStrategy] Teleported {} from {} to {}",
                entity.getEntityName(), currentPos, safePos);

            entity.sendChatMessage("Had to blink out of there!");
            return RecoveryResult.SUCCESS;

        } catch (Exception e) {
            LOGGER.error("[TeleportStrategy] Error during teleport", e);
            return RecoveryResult.RETRY;
        }
    }

    @Override
    public int getMaxAttempts() {
        return MAX_TELEPORT_ATTEMPTS;
    }

    /**
     * Finds a safe position near the given location.
     *
     * <p>Search strategy:</p>
     * <ol>
     *   <li>Check current position (maybe just stuck in wall)</li>
     *   <li>Check positions around current at same Y level</li>
     *   <li>Check positions above current (stuck in hole)</li>
     *   <li>Check positions below current (stuck on ceiling)</li>
     *   <li>Expand search radius if needed</li>
     * </ol>
     *
     * @param entity     The entity
     * @param currentPos Current position
     * @return Safe position, or null if none found
     */
    private BlockPos findSafePosition(ForemanEntity entity, BlockPos currentPos) {
        Level level = entity.level();
        MovementValidator validator = new MovementValidator();
        PathfindingContext context = new PathfindingContext(level, entity);

        // Strategy 1: Check if current position is actually safe
        // (might just be pathfinding stuck, not physically stuck)
        if (validator.hasSolidGround(level, currentPos) && validator.hasHeadroom(level, currentPos)) {
            LOGGER.debug("[TeleportStrategy] Current position {} is safe, "
                + "agent may just be pathfinding stuck", currentPos);
            return currentPos;
        }

        // Strategy 2: Check positions at same Y level in spiral pattern
        BlockPos sameLevelPos = findSafePositionAtLevel(
            entity, currentPos, currentPos.getY(), validator, context
        );
        if (sameLevelPos != null) {
            return sameLevelPos;
        }

        // Strategy 3: Check positions above (might be in a hole)
        for (int y = 1; y <= VERTICAL_SEARCH_RANGE; y++) {
            BlockPos abovePos = findSafePositionAtLevel(
                entity, currentPos, currentPos.getY() + y, validator, context
            );
            if (abovePos != null) {
                LOGGER.debug("[TeleportStrategy] Found safe position above at {}",
                    abovePos);
                return abovePos;
            }
        }

        // Strategy 4: Check positions below (might be on ceiling)
        for (int y = 1; y <= VERTICAL_SEARCH_RANGE; y++) {
            BlockPos belowPos = findSafePositionAtLevel(
                entity, currentPos, currentPos.getY() - y, validator, context
            );
            if (belowPos != null) {
                LOGGER.debug("[TeleportStrategy] Found safe position below at {}",
                    belowPos);
                return belowPos;
            }
        }

        // No safe position found
        LOGGER.warn("[TeleportStrategy] No safe position found near {}", currentPos);
        return null;
    }

    /**
     * Finds a safe position at a specific Y level.
     * Searches in a spiral pattern from center.
     *
     * @param entity    The entity
     * @param center    Center position (X and Z used)
     * @param y         Y level to search at
     * @param validator Movement validator
     * @param context   Pathfinding context
     * @return Safe position, or null if none found at this level
     */
    private BlockPos findSafePositionAtLevel(
        ForemanEntity entity,
        BlockPos center,
        int y,
        MovementValidator validator,
        PathfindingContext context
    ) {
        Level level = entity.level();

        // Check center position first
        BlockPos centerPos = new BlockPos(center.getX(), y, center.getZ());
        if (validator.hasSolidGround(level, centerPos) && validator.hasHeadroom(level, centerPos)) {
            return centerPos;
        }

        // Spiral search pattern
        int radius = 1;
        while (radius <= SAFE_POSITION_SEARCH_RADIUS) {
            // Check all positions at this radius
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                        // Only check perimeter of spiral
                        BlockPos checkPos = new BlockPos(
                            center.getX() + dx,
                            y,
                            center.getZ() + dz
                        );

                        if (validator.hasSolidGround(level, checkPos) && validator.hasHeadroom(level, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
            radius++;
        }

        return null;
    }

    /**
     * Teleports the entity to the given position.
     *
     * @param entity  The entity to teleport
     * @param target  Target position
     */
    private void teleportTo(ForemanEntity entity, BlockPos target) {
        // Stop current navigation
        if (entity.getNavigation() != null) {
            entity.getNavigation().stop();
        }

        // Stop current action to prevent getting stuck again
        if (entity.getActionExecutor() != null) {
            entity.getActionExecutor().stopCurrentAction();
        }

        // Teleport to position
        // Use teleportTo for direct position setting
        entity.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        LOGGER.debug("[TeleportStrategy] Teleported {} to {}",
            entity.getEntityName(), target);
    }
}
