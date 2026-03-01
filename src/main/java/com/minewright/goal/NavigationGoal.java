package com.minewright.goal;

import net.minecraft.core.BlockPos;

/**
 * A navigation goal represents a target or objective for pathfinding.
 *
 * <p>Inspired by Baritone's goal system, this interface provides a flexible
 * way to specify navigation objectives that can be:</p>
 *
 * <ul>
 *   <li><b>Simple:</b> A specific block position</li>
 *   <li><b>Dynamic:</b> Nearest block of a type, nearest entity</li>
 *   <li><b>Composite:</b> Multiple goals with ANY/ALL logic</li>
 *   <li><b>Conditional:</b> Escape from danger, maintain safety</li>
 * </ul>
 *
 * <h3>Core Methods:</h3>
 * <ul>
 *   <li><b>isComplete:</b> Checks if a position satisfies this goal</li>
 *   <li><b>getTargetPosition:</b> Returns the best current target for pathfinding</li>
 *   <li><b>getHeuristic:</b> Estimates cost to reach goal (for A* sorting)</li>
 * </ul>
 *
 * <h3>Design Philosophy:</h3>
 * <p>Goals are <b>declarative</b> - they describe WHAT to achieve, not HOW.
 * The pathfinding system determines HOW to reach the goal.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Simple position goal
 * NavigationGoal goal = Goals.gotoPos(100, 64, 200);
 *
 * // Find any iron ore
 * NavigationGoal ironGoal = Goals.gotoBlock(Blocks.IRON_ORE);
 *
 * // Composite: reach ANY of multiple waypoints
 * NavigationGoal patrol = Goals.anyOf(
 *     Goals.gotoPos(100, 64, 100),
 *     Goals.gotoPos(200, 64, 100),
 *     Goals.gotoPos(150, 64, 200)
 * );
 *
 * // Run away from danger
 * NavigationGoal flee = Goals.runAway(dangerPos, 20);
 * }</pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>Implementations should be immutable for thread safety.
 * Goals are queried during pathfinding which may run on any thread.</p>
 *
 * @see Goals
 * @see CompositeNavigationGoal
 * @see GetToBlockGoal
 * @see GetToEntityGoal
 * @see RunAwayGoal
 *
 * @since 1.0.0
 */
public interface NavigationGoal {
    /**
     * Checks if the given position satisfies this goal.
     *
     * <p>This method is called by the pathfinder to determine if
     * pathfinding should stop. For composite goals, this typically
     * means checking if ANY sub-goal is satisfied.</p>
     *
     * @param pos The position to check
     * @return true if this position completes the goal
     */
    boolean isComplete(BlockPos pos);

    /**
     * Checks if the given position satisfies this goal.
     *
     * <p>This is a convenience method that accepts a WorldState
     * for goals that need world context (e.g., finding nearest block).</p>
     *
     * @param world The current world state
     * @return true if this position completes the goal
     */
    boolean isComplete(WorldState world);

    /**
     * Returns the best target position for this goal.
     *
     * <p>For simple goals, this is a fixed position.
     * For dynamic goals (block/entity search), this may change
     * as the world state changes.</p>
     *
     * <p>Goals that don't have a specific target position
     * (e.g., "escape") should return a suggested direction.</p>
     *
     * @param world The current world state
     * @return Target position, or null if no specific target
     */
    BlockPos getTargetPosition(WorldState world);

    /**
     * Calculates the heuristic cost to reach this goal.
     *
     * <p>This is used by A* pathfinding to prioritize nodes.
     * Lower heuristics should indicate closer/better targets.</p>
     *
     * <p>For composite goals with ANY logic, return the minimum
     * heuristic among all sub-goals.</p>
     *
     * @param world The current world state
     * @return Estimated cost (lower is better), or Double.MAX_VALUE if unreachable
     */
    double getHeuristic(WorldState world);

    /**
     * Checks if this goal is still valid.
     *
     * <p>Some goals may become invalid (e.g., entity dies, block destroyed).
     * Invalid goals should cause pathfinding to fail or replan.</p>
     *
     * @param world The current world state
     * @return true if goal is still achievable
     */
    default boolean isValid(WorldState world) {
        return true;
    }
}
