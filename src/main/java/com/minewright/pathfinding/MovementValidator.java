package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Validates movement possibilities for pathfinding.
 *
 * <p>This class determines whether an agent can move from one position to another,
 * considering all Minecraft movement mechanics:</p>
 *
 * <h3>Validation Checks:</h3>
 * <ul>
 *   <li><b>Standability:</b> Is there solid ground to stand on?</li>
 *   <li><b>Clearance:</b> Is there enough headroom and space?</li>
 *   <li><b>Safety:</b> Would movement cause damage (lava, cactus, fall)?</li>
 *   <li><b>Support:</b> Are there blocks to jump on or climb?</li>
 *   <li><b>Fluids:</b> Is the agent in water or lava?</li>
 * </ul>
 *
 * <h3>Movement Types Validated:</h3>
 * <ul>
 *   <li><b>WALK:</b> Standard horizontal movement</li>
 *   <li><b>JUMP:</b> Jumping up one block</li>
 *   <li><b>FALL:</b> Falling down from height</li>
 *   <li><b>CLIMB:</b> Climbing ladders, vines, etc.</li>
 *   <li><b>SWIM:</b> Moving through water</li>
 *   <li><b>PARKOUR:</b> Jumping across gaps</li>
 * </ul>
 *
 * <p><b>Caching:</b> Validation results are cached by position to improve
 * performance during pathfinding.</p>
 *
 * @see PathfindingContext
 * @see MovementType
 * @see AStarPathfinder
 */
public class MovementValidator {
    /** Cache of validated positions to avoid repeated checks. */
    private final PositionCache positionCache;

    /** Entity bounding box width (default: 0.6 for most entities). */
    private static final double ENTITY_WIDTH = 0.6;

    /** Entity height (default: 1.8 for players/steve). */
    private static final double ENTITY_HEIGHT = 1.8;

    /** Eye height for looking up (default: 1.62). */
    private static final double EYE_HEIGHT = 1.62;

    /** Jump height in blocks (default: 1.0). */
    private static final double JUMP_HEIGHT = 1.0;

    /**
     * Creates a new movement validator with default cache settings.
     */
    public MovementValidator() {
        this.positionCache = new PositionCache(1000);
    }

    /**
     * Creates a new movement validator with custom cache size.
     *
     * @param cacheSize Maximum number of positions to cache
     */
    public MovementValidator(int cacheSize) {
        this.positionCache = new PositionCache(cacheSize);
    }

    /**
     * Checks if movement from one position to another is valid.
     *
     * <p>This is the main validation entry point. It dispatches to
     * specific validation methods based on movement type.</p>
     *
     * @param from     Starting position
     * @param to       Target position
     * @param movement Type of movement
     * @param context  Pathfinding context with agent capabilities
     * @return true if movement is valid
     */
    public boolean canMove(BlockPos from, BlockPos to, MovementType movement,
                           PathfindingContext context) {
        // Basic bounds check
        if (!isInBounds(to, context)) {
            return false;
        }

        // Check for dangerous blocks
        if (isDangerous(to, context) && !context.allowDangerousMovements()) {
            return false;
        }

        // Delegate to movement-specific validation
        switch (movement) {
            case WALK:
                return canWalkTo(from, to, context);

            case JUMP:
                return canJumpTo(from, to, context);

            case FALL:
                return canFallTo(from, to, context);

            case CLIMB:
                return canClimbTo(from, to, context);

            case SWIM:
            case WATER_WALK:
            case DESCEND_WATER:
                return canSwimTo(from, to, context);

            case PARKOUR:
                return canParkourTo(from, to, context);

            case SNEAK:
                return canSneakTo(from, to, context);

            case FLY:
                return canFlyTo(from, to, context);

            default:
                return false;
        }
    }

    /**
     * Checks if agent can walk to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if walking is valid
     */
    public boolean canWalkTo(BlockPos from, BlockPos to, PathfindingContext context) {
        Level level = context.getLevel();

        // Check if on same level or slightly different
        int dy = to.getY() - from.getY();
        if (Math.abs(dy) > 1) {
            return false;
        }

        // Check if there's solid ground at target
        if (!hasSolidGround(level, to)) {
            return false;
        }

        // Check if there's enough headroom
        if (!hasHeadroom(level, to)) {
            return false;
        }

        // Check if the space is not blocked
        if (isBlocked(level, to)) {
            return false;
        }

        // Check if stepping up requires jumping
        if (dy == 1) {
            return context.getJumpHeight() >= 1.0;
        }

        return true;
    }

    /**
     * Checks if agent can jump to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if jumping is valid
     */
    public boolean canJumpTo(BlockPos from, BlockPos to, PathfindingContext context) {
        Level level = context.getLevel();

        int dy = to.getY() - from.getY();

        // Check jump height
        if (dy > context.getJumpHeight()) {
            return false;
        }

        // Can only jump up
        if (dy < 0) {
            return false;
        }

        // Check if there's something to jump onto
        if (!hasSolidGround(level, to)) {
            return false;
        }

        // Check headroom at jump destination
        if (!hasHeadroom(level, to)) {
            return false;
        }

        // Check if jump path is clear
        BlockPos above = from.above();
        if (isBlocked(level, above)) {
            return false; // Can't jump if head hits ceiling
        }

        return true;
    }

    /**
     * Checks if agent can fall to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if falling is valid
     */
    public boolean canFallTo(BlockPos from, BlockPos to, PathfindingContext context) {
        Level level = context.getLevel();

        int dy = from.getY() - to.getY(); // Positive when falling down

        // Check fall distance
        if (dy > context.getMaxFallDistance() && !context.allowDangerousMovements()) {
            return false;
        }

        // Can only fall down
        if (dy <= 0) {
            return false;
        }

        // Check if landing spot is safe
        if (!hasSolidGround(level, to)) {
            return false;
        }

        // Check headroom at landing spot
        if (!hasHeadroom(level, to)) {
            return false;
        }

        // Check if fall path is clear (no blocks in the way)
        for (int y = from.getY() - 1; y > to.getY(); y--) {
            BlockPos checkPos = new BlockPos(to.getX(), y, to.getZ());
            if (isBlocked(level, checkPos)) {
                return false; // Path blocked
            }
        }

        return true;
    }

    /**
     * Checks if agent can climb to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if climbing is valid
     */
    public boolean canClimbTo(BlockPos from, BlockPos to, PathfindingContext context) {
        if (!context.canClimb()) {
            return false;
        }

        Level level = context.getLevel();

        // Check if there's a climbable block at the target
        if (!isClimbable(level, to)) {
            return false;
        }

        // Check if agent can reach the climbable block
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int dy = to.getY() - from.getY();

        // Can climb up or down along the climbable surface
        if ((dx <= 1 && dz <= 1) && Math.abs(dy) <= 3) {
            return true;
        }

        return false;
    }

    /**
     * Checks if agent can swim to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if swimming is valid
     */
    public boolean canSwimTo(BlockPos from, BlockPos to, PathfindingContext context) {
        if (!context.canSwim()) {
            return false;
        }

        Level level = context.getLevel();

        // Check if target is in water
        if (!isWater(level, to)) {
            return false;
        }

        // Check adjacent blocks for water (for horizontal movement)
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int dy = to.getY() - from.getY();

        // Can swim horizontally or vertically within water
        if ((dx <= 1 && dz <= 1 && Math.abs(dy) <= 1) || (dx == 0 && dz == 0 && Math.abs(dy) <= 2)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if agent can perform parkour to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if parkour is valid
     */
    public boolean canParkourTo(BlockPos from, BlockPos to, PathfindingContext context) {
        if (!context.canParkour()) {
            return false;
        }

        Level level = context.getLevel();

        // Check horizontal distance
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int distance = Math.max(dx, dz);

        // Parkour jumps can cover 2-3 blocks
        if (distance < 2 || distance > 3) {
            return false;
        }

        // Must be on same level or jumping up slightly
        int dy = to.getY() - from.getY();
        if (dy < 0 || dy > 1) {
            return false;
        }

        // Check if landing spot is valid
        if (!hasSolidGround(level, to)) {
            return false;
        }

        // Check headroom at landing
        if (!hasHeadroom(level, to)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if agent can sneak to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if sneaking is valid
     */
    public boolean canSneakTo(BlockPos from, BlockPos to, PathfindingContext context) {
        Level level = context.getLevel();

        // Sneaking is for low ceilings
        if (!hasLowCeiling(level, to)) {
            return false; // Normal walk is better
        }

        // Check if there's solid ground
        if (!hasSolidGround(level, to)) {
            return false;
        }

        // Check if the 1.5 block space is clear
        BlockPos eyeLevel = to.above();
        if (isBlocked(level, eyeLevel)) {
            // Check if it's a partial block (slab, stair, etc.)
            BlockState eyeState = level.getBlockState(eyeLevel);
            if (!eyeState.isSuffocating(level, eyeLevel)) {
                return true; // Can fit under partial block
            }
            return false;
        }

        return true;
    }

    /**
     * Checks if agent can fly to a position.
     *
     * @param from    Starting position
     * @param to      Target position
     * @param context Pathfinding context
     * @return true if flying is valid
     */
    public boolean canFlyTo(BlockPos from, BlockPos to, PathfindingContext context) {
        if (!context.canFly()) {
            return false;
        }

        // Flying bypasses most constraints
        // Just check that target isn't in a solid block
        Level level = context.getLevel();
        return !isBlocked(level, to);
    }

    // ========== Helper Methods ==========

    /**
     * Checks if a position is within world bounds.
     *
     * @param pos     Position to check
     * @param context Pathfinding context
     * @return true if position is valid
     */
    private boolean isInBounds(BlockPos pos, PathfindingContext context) {
        Level level = context.getLevel();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        return pos.getY() >= minY && pos.getY() < maxY;
    }

    /**
     * Checks if a position has a dangerous block (lava, cactus, etc.).
     *
     * @param pos     Position to check
     * @param context Pathfinding context
     * @return true if position is dangerous
     */
    private boolean isDangerous(BlockPos pos, PathfindingContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);

        // Check for lava
        if (state.getFluidState().is(Fluids.LAVA)) {
            return true;
        }

        // Check for cactus
        if (state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)) {
            return true;
        }

        // Check for magma block
        if (state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }

        // Check for campfire
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return true;
        }

        // Check for fire
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a position has solid ground to stand on.
     *
     * @param level The world level
     * @param pos   Position to check
     * @return true if there's solid ground below
     */
    public boolean hasSolidGround(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        // Check if the block below is solid
        if (belowState.isSolidRender(level, below)) {
            return true;
        }

        // Check for climbable blocks (can stand on them while climbing)
        if (isClimbable(level, pos)) {
            return true;
        }

        // Check for water (can swim)
        if (isWater(level, pos)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a position has enough headroom.
     *
     * @param level The world level
     * @param pos   Position to check
     * @return true if there's 2 blocks of clearance
     */
    public boolean hasHeadroom(Level level, BlockPos pos) {
        // Check current position and position above
        BlockPos above = pos.above();

        BlockState current = level.getBlockState(pos);
        BlockState aboveBlock = level.getBlockState(above);

        // Need at least 2 blocks of non-solid space
        return !current.isSolidRender(level, pos) && !aboveBlock.isSolidRender(level, above);
    }

    /**
     * Checks if a position has a low ceiling (requires sneaking).
     *
     * @param level The world level
     * @param pos   Position to check
     * @return true if ceiling is low
     */
    public boolean hasLowCeiling(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveBlock = level.getBlockState(above);

        // Check if block above is solid or partial
        return aboveBlock.isSolidRender(level, above) ||
               aboveBlock.isSuffocating(level, above);
    }

    /**
     * Checks if a position is blocked (solid block).
     *
     * @param level The world level
     * @param pos   Position to check
     * @return true if position is blocked
     */
    public boolean isBlocked(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos);
    }

    /**
     * Checks if a block at position is climbable.
     *
     * @param level The world level
     * @param pos   Position to check
     * @return true if block is climbable
     */
    public boolean isClimbable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isLadder(level, pos, null) || state.is(Blocks.VINE) || state.is(Blocks.SCAFFOLDING);
    }

    /**
     * Checks if a position is in water.
     *
     * @param level The world level
     * @param pos   Position to check
     * @return true if position is water
     */
    public boolean isWater(Level level, BlockPos pos) {
        return level.getBlockState(pos).getFluidState().is(Fluids.WATER);
    }

    /**
     * Clears the position cache.
     */
    public void clearCache() {
        positionCache.clear();
    }

    // ========== Inner Classes ==========

    /**
     * Simple cache for position validation results.
     */
    private static class PositionCache {
        private final int maxSize;
        private final java.util.Map<String, Boolean> cache = new java.util.LinkedHashMap<>(100, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, Boolean> eldest) {
                return size() > maxSize;
            }
        };

        PositionCache(int maxSize) {
            this.maxSize = maxSize;
        }

        Boolean get(BlockPos pos, String checkType) {
            String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + checkType;
            return cache.get(key);
        }

        void put(BlockPos pos, String checkType, boolean result) {
            String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + checkType;
            cache.put(key, result);
        }

        void clear() {
            cache.clear();
        }
    }
}
