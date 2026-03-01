package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A navigation goal to reach the nearest block of a specific type.
 *
 * <p>This goal dynamically searches for blocks of the target type
 * within the search radius and navigates to the closest one.</p>
 *
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li><b>Mining:</b> Find the nearest ore vein</li>
 *   <li><b>Gathering:</b> Locate trees for wood</li>
 *   <li><b>Crafting:</b> Find a crafting table</li>
 *   <li><b>Storage:</b> Locate a chest</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Find any iron ore
 * NavigationGoal ironGoal = Goals.gotoBlock(Blocks.IRON_ORE);
 *
 * // Find a crafting table
 * NavigationGoal craftGoal = Goals.gotoBlock(Blocks.CRAFTING_TABLE);
 *
 * // Find ANY of several options (nearest one)
 * NavigationGoal woodGoal = Goals.gotoAnyBlock(
 *     Blocks.OAK_LOG,
 *     Blocks.BIRCH_LOG,
 *     Blocks.SPRUCE_LOG
 * );
 * }</pre>
 *
 * <h3>Dynamic Behavior:</h3>
 * <p>The target position can change as blocks are mined/placed.
 * The goal is re-evaluated each time the pathfinder queries it.</p>
 *
 * <h3>Completion Criteria:</h3>
 * <p>The goal is complete when the agent is within 2 blocks
 * (adjacent or standing on) of the target block type.</p>
 *
 * @see NavigationGoal
 * @see Goals#gotoBlock(net.minecraft.world.level.block.Block)
 * @since 1.0.0
 */
public class GetToBlockGoal implements NavigationGoal {
    /** The block state to search for. */
    private final BlockState targetBlockState;

    /** Maximum search radius for blocks. */
    private final int searchRadius;

    /** Completion distance (blocks). */
    private final double completionDistance;

    /** Cached nearest block position (for efficiency). */
    private BlockPos cachedNearest;
    /** Timestamp of cache validity. */
    private long cacheTime;

    /**
     * Creates a goal to find the nearest block of a type.
     *
     * @param targetBlockState The block state to search for
     */
    public GetToBlockGoal(BlockState targetBlockState) {
        this(targetBlockState, 64, 2.0);
    }

    /**
     * Creates a goal with custom parameters.
     *
     * @param targetBlockState The block state to search for
     * @param searchRadius Maximum search radius
     * @param completionDistance Distance to consider goal reached
     */
    public GetToBlockGoal(BlockState targetBlockState, int searchRadius, double completionDistance) {
        this.targetBlockState = targetBlockState;
        this.searchRadius = searchRadius;
        this.completionDistance = completionDistance;
    }

    @Override
    public boolean isComplete(BlockPos pos) {
        // Cannot determine without world context
        return false;
    }

    @Override
    public boolean isComplete(WorldState world) {
        // Check if we're near any block of the target type
        BlockPos current = world.getCurrentPosition();

        // Check nearby blocks (within completion distance)
        int checkRadius = (int) Math.ceil(completionDistance);
        for (int dy = -checkRadius; dy <= checkRadius; dy++) {
            for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                for (int dx = -checkRadius; dx <= checkRadius; dx++) {
                    BlockPos checkPos = current.offset(dx, dy, dz);
                    if (!world.isLoaded(checkPos)) {
                        continue;
                    }

                    if (world.getBlockState(checkPos).equals(targetBlockState)) {
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist <= completionDistance) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public BlockPos getTargetPosition(WorldState world) {
        // Update cache if expired
        if (cachedNearest == null || cacheTime != world.getLevel().getGameTime()) {
            cachedNearest = world.findNearestBlock(targetBlockState, searchRadius);
            cacheTime = world.getLevel().getGameTime();
        }

        return cachedNearest;
    }

    @Override
    public double getHeuristic(WorldState world) {
        BlockPos nearest = getTargetPosition(world);
        if (nearest == null) {
            return Double.MAX_VALUE; // No blocks found
        }

        return world.distanceTo(nearest);
    }

    @Override
    public boolean isValid(WorldState world) {
        // Goal is valid if at least one target block exists
        return getTargetPosition(world) != null;
    }

    /**
     * Gets the target block state.
     *
     * @return The block state being searched for
     */
    public BlockState getTargetBlockState() {
        return targetBlockState;
    }

    /**
     * Gets the search radius.
     *
     * @return Maximum search radius in blocks
     */
    public int getSearchRadius() {
        return searchRadius;
    }

    /**
     * Gets the completion distance.
     *
     * @return Distance to consider goal reached
     */
    public double getCompletionDistance() {
        return completionDistance;
    }

    /**
     * Clears the cached nearest block position.
     *
     * <p>Call this if the world changes significantly
     * (e.g., blocks mined/placed).</p>
     */
    public void clearCache() {
        cachedNearest = null;
    }

    @Override
    public String toString() {
        return "GetToBlockGoal{" +
                "block=" + targetBlockState.getBlock() +
                ", searchRadius=" + searchRadius +
                '}';
    }
}
