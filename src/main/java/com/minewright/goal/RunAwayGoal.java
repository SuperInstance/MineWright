package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A navigation goal to escape from a dangerous position.
 *
 * <p>This goal is satisfied when the agent is outside a specified
 * safe distance from the danger source. It's useful for:</p>
 *
 * <ul>
 *   <li><b>Fleeing combat:</b> Run away from hostile mobs</li>
 *   <li><b>Explosions:</b> Escape from creeping TNT or creepers</li>
 *   <li><b>Environmental hazards:</b> Avoid lava, cactus, etc.</li>
 *   <li><b>Safe zone:</b> Return to a safe area after danger</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Run away from a creeper
 * NavigationGoal fleeCreeper = Goals.runAway(creeperPos, 20);
 *
 * // Escape from lava
 * NavigationGoal fleeLava = Goals.runAway(lavaPos, 10);
 *
 * // Avoid multiple danger sources
 * NavigationGoal safe = Goals.runAwayFromAll(new BlockPos[]{danger1, danger2}, 15);
 * }</pre>
 *
 * <h3>Target Position:</h3>
 * <p>The target position is the furthest point from the danger
 * within the search radius. This provides a clear direction for
 * the pathfinder to follow.</p>
 *
 * <h3>Completion Criteria:</h3>
 * <p>The goal is complete when the agent is at or beyond the
 * safe distance from the danger source.</p>
 *
 * @see NavigationGoal
 * @see Goals#runAway(BlockPos, double)
 * @since 1.0.0
 */
public class RunAwayGoal implements NavigationGoal {
    /** The position of the danger source. */
    private final BlockPos dangerPos;

    /** Distance to be considered safe. */
    private final double safeDistance;

    /** Maximum search radius for finding safe positions. */
    private final int searchRadius;

    /**
     * Creates a goal to run away from a danger source.
     *
     * @param dangerPos The position of the danger
     * @param safeDistance Distance to be considered safe
     */
    public RunAwayGoal(BlockPos dangerPos, double safeDistance) {
        this(dangerPos, safeDistance, 64);
    }

    /**
     * Creates a goal with custom search radius.
     *
     * @param dangerPos The position of the danger
     * @param safeDistance Distance to be considered safe
     * @param searchRadius Maximum radius to search for safe positions
     */
    public RunAwayGoal(BlockPos dangerPos, double safeDistance, int searchRadius) {
        this.dangerPos = dangerPos;
        this.safeDistance = safeDistance;
        this.searchRadius = searchRadius;
    }

    @Override
    public boolean isComplete(BlockPos pos) {
        double dist = Math.sqrt(pos.distSqr(dangerPos));
        return dist >= safeDistance;
    }

    @Override
    public boolean isComplete(WorldState world) {
        BlockPos current = world.getCurrentPosition();
        return isComplete(current);
    }

    @Override
    public BlockPos getTargetPosition(WorldState world) {
        // Find a position that's safe (beyond safeDistance)
        BlockPos current = world.getCurrentPosition();
        double currentDist = Math.sqrt(current.distSqr(dangerPos));

        // If already safe, return current position
        if (currentDist >= safeDistance) {
            return current;
        }

        // Find a safe position by extending away from danger
        // Calculate direction vector from danger to current
        int dx = current.getX() - dangerPos.getX();
        int dy = current.getY() - dangerPos.getY();
        int dz = current.getZ() - dangerPos.getZ();

        // Normalize the direction
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) {
            // On top of danger - pick a random direction
            dx = 1;
            dy = 0;
            dz = 0;
            length = 1;
        }

        // Scale to safe distance
        double scale = safeDistance / length;
        int targetDx = (int) Math.round(dx * scale);
        int targetDy = (int) Math.round(dy * scale);
        int targetDz = (int) Math.round(dz * scale);

        // Target position
        BlockPos target = dangerPos.offset(targetDx, targetDy, targetDz);

        // Clamp to world bounds
        Level level = world.getLevel();
        target = new BlockPos(
            clamp(target.getX(), level.getMinBuildHeight(), level.getMaxBuildHeight()),
            clamp(target.getY(), level.getMinBuildHeight(), level.getMaxBuildHeight()),
            clamp(target.getZ(), level.getMinBuildHeight(), level.getMaxBuildHeight())
        );

        return target;
    }

    /**
     * Clamps a value between min and max.
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public double getHeuristic(WorldState world) {
        BlockPos current = world.getCurrentPosition();
        double dist = Math.sqrt(current.distSqr(dangerPos));

        // If already safe, heuristic is 0
        if (dist >= safeDistance) {
            return 0;
        }

        // Heuristic is distance we still need to travel
        return safeDistance - dist;
    }

    @Override
    public boolean isValid(WorldState world) {
        // Goal is always valid as long as the world is accessible
        return true;
    }

    /**
     * Gets the danger position.
     *
     * @return The position of the danger source
     */
    public BlockPos getDangerPos() {
        return dangerPos;
    }

    /**
     * Gets the safe distance.
     *
     * @return Distance to be considered safe
     */
    public double getSafeDistance() {
        return safeDistance;
    }

    /**
     * Gets the search radius.
     *
     * @return Maximum search radius for safe positions
     */
    public int getSearchRadius() {
        return searchRadius;
    }

    @Override
    public String toString() {
        return "RunAwayGoal{" +
                "danger=" + dangerPos +
                ", safeDistance=" + safeDistance +
                '}';
    }
}
