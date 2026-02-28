package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;

/**
 * Provides various heuristic functions for A* pathfinding.
 *
 * <p>Heuristics estimate the cost from a node to the goal, guiding the A* search
 * toward the goal efficiently. A good heuristic should:</p>
 * <ul>
 *   <li><b>Admissible:</b> Never overestimate the true cost (ensures optimal path)</li>
 *   <li><b>Consistent:</b> Satisfy triangle inequality (prevents re-expanding nodes)</li>
 *   <li><b>Fast:</b> Compute quickly (no complex operations)</li>
 * </ul>
 *
 * <h3>Heuristic Selection Guide:</h3>
 * <ul>
 *   <li><b>Manhattan:</b> Best for 4-directional movement on flat terrain</li>
 *   <li><b>Euclidean:</b> Best when diagonal movement is allowed at any angle</li>
 *   <li><b>Octile:</b> Best for 8-directional movement with equal diagonal cost</li>
 *   <li><b>Chebyshev:</b> Best for 8-directional with cheap diagonals</li>
 *   <li><b>Hierarchical:</b> Best for long-distance pathfinding (chunk-level)</li>
 * </ul>
 *
 * @see AStarPathfinder
 * @see HierarchicalPathfinder
 */
public final class Heuristics {

    private Heuristics() {
        // Utility class - prevent instantiation
    }

    /**
     * Manhattan distance heuristic.
     *
     * <p>Calculates distance as |dx| + |dy| + |dz|.</p>
     *
     * <p><b>Use when:</b> Movement is restricted to cardinal directions (no diagonals),
     * or when vertical movement has the same cost as horizontal.</p>
     *
     * <p><b>Properties:</b> Admissible, consistent, fast computation.</p>
     *
     * @param from Starting position
     * @param to   Goal position
     * @return Manhattan distance
     */
    public static double manhattanDistance(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        int dz = Math.abs(from.getZ() - to.getZ());
        return dx + dy + dz;
    }

    /**
     * Euclidean distance heuristic.
     *
     * <p>Calculates straight-line distance as sqrt(dx² + dy² + dz²).</p>
     *
     * <p><b>Use when:</b> Movement can be in any direction (flying, swimming),
     * or when diagonal movement costs the same as cardinal movement.</p>
     *
     * <p><b>Properties:</b> Admissible, consistent, requires sqrt calculation.</p>
     *
     * @param from Starting position
     * @param to   Goal position
     * @return Euclidean distance
     */
    public static double euclideanDistance(BlockPos from, BlockPos to) {
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Octile distance heuristic.
     *
     * <p>Estimates distance when diagonal movement costs sqrt(2) ≈ 1.414
     * times cardinal movement. This matches Minecraft's movement costs.</p>
     *
     * <p><b>Use when:</b> Diagonal movement is allowed with realistic cost.
     * This is the most commonly used heuristic for Minecraft pathfinding.</p>
     *
     * <p><b>Properties:</b> Admissible, consistent, good balance of accuracy and speed.</p>
     *
     * @param from Starting position
     * @param to   Goal position
     * @return Octile distance estimate
     */
    public static double octileDistance(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        int dz = Math.abs(from.getZ() - to.getZ());

        // Combine horizontal components (dx and dz)
        int horizontal = Math.max(dx, dz);
        int horizontalMin = Math.min(dx, dz);

        // Octile for horizontal plane
        double horizontalCost = horizontal + (Math.sqrt(2) - 1) * horizontalMin;

        // Add vertical component
        return horizontalCost + dy;
    }

    /**
     * Chebyshev distance heuristic.
     *
     * <p>Calculates distance as max(|dx|, |dy|, |dz|).</p>
     *
     * <p><b>Use when:</b> Diagonal movement costs the same as cardinal movement
     * (e.g., flying, swimming where movement speed is uniform).</p>
     *
     * <p><b>Properties:</b> Admissible (if diagonals cost 1), consistent, fastest computation.</p>
     *
     * @param from Starting position
     * @param to   Goal position
     * @return Chebyshev distance
     */
    public static double chebyshevDistance(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        int dz = Math.abs(from.getZ() - to.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    /**
     * Height-weighted heuristic.
     *
     * <p>Similar to Manhattan but gives extra weight to vertical movement,
     * since jumping/climbing is typically more expensive than walking.</p>
     *
     * <p><b>Use when:</b> Vertical movement is significantly more costly
     * than horizontal movement.</p>
     *
     * @param from           Starting position
     * @param to             Goal position
     * @param verticalWeight Multiplier for vertical distance (default: 2.0)
     * @return Height-weighted distance
     */
    public static double heightWeightedDistance(BlockPos from, BlockPos to, double verticalWeight) {
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        int dz = Math.abs(from.getZ() - to.getZ());
        return dx + dz + (dy * verticalWeight);
    }

    /**
     * Hierarchical heuristic.
     *
     * <p>Uses chunk-level distance estimation for long-range pathfinding.
     * Very fast but less accurate. Best used with HierarchicalPathfinder.</p>
     *
     * <p><b>Use when:</b> Pathfinding over long distances (> 64 blocks).</p>
     *
     * @param from           Starting position
     * @param to             Goal position
     * @param chunkSize      Size of chunks (default: 16)
     * @return Chunk-level distance estimate
     */
    public static double hierarchicalDistance(BlockPos from, BlockPos to, int chunkSize) {
        // Convert to chunk coordinates
        int fromChunkX = from.getX() / chunkSize;
        int fromChunkZ = from.getZ() / chunkSize;
        int toChunkX = to.getX() / chunkSize;
        int toChunkZ = to.getZ() / chunkSize;

        // Use octile distance at chunk level
        int dx = Math.abs(fromChunkX - toChunkX);
        int dz = Math.abs(fromChunkZ - toChunkZ);
        int horizontal = Math.max(dx, dz);
        int horizontalMin = Math.min(dx, dz);
        double chunkCost = horizontal + (Math.sqrt(2) - 1) * horizontalMin;

        // Convert back to block distance
        return chunkCost * chunkSize;
    }

    /**
     * Dynamic heuristic that adapts based on movement capabilities.
     *
     * <p>Selects the best heuristic based on the agent's capabilities
     * (flying, swimming, etc.) and the terrain context.</p>
     *
     * @param from    Starting position
     * @param to      Goal position
     * @param context Pathfinding context with agent capabilities
     * @return Adaptive heuristic estimate
     */
    public static double adaptiveHeuristic(BlockPos from, BlockPos to, PathfindingContext context) {
        // If agent can fly, use Euclidean (straight line is best)
        if (context.canFly()) {
            return euclideanDistance(from, to);
        }

        // If in water, use reduced vertical weight (buoyancy helps)
        if (context.canSwim() && isWaterPosition(from, context)) {
            return heightWeightedDistance(from, to, 1.2); // Lower vertical cost
        }

        // For normal movement, use octile (accounts for diagonals)
        return octileDistance(from, to);
    }

    /**
     * Direction-based heuristic that considers the terrain gradient.
     *
     * <p>Adjusts the estimate based on whether the path goes mostly uphill
     * or downhill, since climbing is more expensive than falling.</p>
     *
     * @param from    Starting position
     * @param to      Goal position
     * @param context Pathfinding context
     * @return Terrain-aware heuristic estimate
     */
    public static double terrainAwareHeuristic(BlockPos from, BlockPos to, PathfindingContext context) {
        double baseCost = octileDistance(from, to);

        int verticalDiff = to.getY() - from.getY();

        // If goal is significantly higher, add extra cost
        if (verticalDiff > 10) {
            baseCost += verticalDiff * 0.5; // Extra cost for climbing
        }
        // If goal is lower, reduce cost slightly (falling is cheap)
        else if (verticalDiff < -10) {
            baseCost += verticalDiff * 0.2; // Discount for descending
        }

        return baseCost;
    }

    /**
     * Checks if a position is in water.
     *
     * @param pos     Position to check
     * @param context Pathfinding context
     * @return true if position is water
     */
    private static boolean isWaterPosition(BlockPos pos, PathfindingContext context) {
        return context.getLevel().getBlockState(pos).getFluidState().isSource();
    }

    /**
     * Heuristic function interface for lambda usage.
     */
    @FunctionalInterface
    public interface HeuristicFunction {
        double estimate(BlockPos from, BlockPos to);
    }

    /**
     * Creates a heuristic function from the context.
     *
     * @param context Pathfinding context
     * @return Appropriate heuristic function
     */
    public static HeuristicFunction createHeuristic(PathfindingContext context) {
        if (context.useHierarchical()) {
            BlockPos currentGoal = context.getCurrentGoalPos();
            BlockPos entityPos = context.getEntity().blockPosition();

            if (currentGoal != null &&
                manhattanDistance(entityPos, currentGoal) > context.getHierarchicalThreshold()) {
                return (from, to) -> hierarchicalDistance(from, to, 16);
            }
        }

        if (context.canFly()) {
            return Heuristics::euclideanDistance;
        }

        if (context.shouldPreferWater()) {
            return (from, to) -> heightWeightedDistance(from, to, 1.2);
        }

        // Default: octile distance (best for Minecraft movement)
        return Heuristics::octileDistance;
    }
}
