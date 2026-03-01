package com.minewright.goal;

import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A composite navigation goal that combines multiple sub-goals.
 *
 * <p>Inspired by Baritone's GoalComposite, this class implements
 * two composition strategies:</p>
 *
 * <ul>
 *   <li><b>ANY:</b> Goal is complete when ANY sub-goal is complete</li>
 *   <li><b>ALL:</b> Goal is complete when ALL sub-goals are complete</li>
 * </ul>
 *
 * <h3>ANY Composition (Most Common):</h3>
 * <p>Useful when you want to reach the closest of multiple targets:</p>
 * <pre>{@code
 * // Patrol between waypoints - reach any one
 * NavigationGoal patrol = Goals.anyOf(
 *     Goals.gotoPos(100, 64, 100),
 *     Goals.gotoPos(200, 64, 100),
 *     Goals.gotoPos(150, 64, 200)
 * );
 *
 * // Find any crafting table
 * NavigationGoal crafting = Goals.anyOf(
 *     Goals.gotoBlock(Blocks.CRAFTING_TABLE),
 *     Goals.gotoBlock(Blocks.CARTOGRAPHY_TABLE)  // Alternative
 * );
 * }</pre>
 *
 * <h3>ALL Composition:</h3>
 * <p>Useful when you need to visit multiple locations:</p>
 * <pre>{@code
 * // Visit all waypoints in any order
 * NavigationGoal tour = Goals.allOf(
 *     Goals.gotoPos(100, 64, 100),
 *     Goals.gotoPos(200, 64, 100),
 *     Goals.gotoPos(150, 64, 200)
 * );
 * }</pre>
 *
 * <h3>Heuristic Calculation:</h3>
 * <ul>
 *   <li><b>ANY:</b> Returns minimum heuristic (closest goal)</li>
 *   <li><b>ALL:</b> Returns sum of all heuristics (total distance)</li>
 * </ul>
 *
 * <h3>Target Selection:</h3>
 * <p>For ANY composition, returns the position of the closest sub-goal.
 * For ALL composition, returns the position of the furthest sub-goal.</p>
 *
 * @see NavigationGoal
 * @see Goals#anyOf(NavigationGoal...)
 * @see Goals#allOf(NavigationGoal...)
 * @since 1.0.0
 */
public class CompositeNavigationGoal implements NavigationGoal {
    /** The sub-goals that compose this goal. */
    private final List<NavigationGoal> goals;

    /** Composition strategy: ANY or ALL. */
    private final CompositionType type;

    /**
     * Creates a new composite goal.
     *
     * @param goals The sub-goals to compose
     * @param type The composition type (ANY or ALL)
     */
    public CompositeNavigationGoal(List<NavigationGoal> goals, CompositionType type) {
        if (goals == null || goals.isEmpty()) {
            throw new IllegalArgumentException("Composite goal requires at least one sub-goal");
        }
        this.goals = List.copyOf(goals); // Immutable copy
        this.type = type;
    }

    /**
     * Creates a new composite goal from an array of goals.
     *
     * @param goals The sub-goals to compose
     * @param type The composition type (ANY or ALL)
     */
    public CompositeNavigationGoal(NavigationGoal[] goals, CompositionType type) {
        this(Arrays.asList(goals), type);
    }

    @Override
    public boolean isComplete(BlockPos pos) {
        return switch (type) {
            case ANY -> goals.stream().anyMatch(goal -> goal.isComplete(pos));
            case ALL -> goals.stream().allMatch(goal -> goal.isComplete(pos));
        };
    }

    @Override
    public boolean isComplete(WorldState world) {
        return switch (type) {
            case ANY -> goals.stream().anyMatch(goal -> goal.isComplete(world));
            case ALL -> goals.stream().allMatch(goal -> goal.isComplete(world));
        };
    }

    @Override
    public BlockPos getTargetPosition(WorldState world) {
        return switch (type) {
            case ANY -> {
                // Return closest goal's target
                BlockPos closest = null;
                double closestDist = Double.MAX_VALUE;

                for (NavigationGoal goal : goals) {
                    BlockPos target = goal.getTargetPosition(world);
                    if (target != null) {
                        double dist = world.distanceTo(target);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = target;
                        }
                    }
                }
                yield closest;
            }
            case ALL -> {
                // Return furthest goal's target
                BlockPos furthest = null;
                double furthestDist = Double.MIN_VALUE;

                for (NavigationGoal goal : goals) {
                    BlockPos target = goal.getTargetPosition(world);
                    if (target != null) {
                        double dist = world.distanceTo(target);
                        if (dist > furthestDist) {
                            furthestDist = dist;
                            furthest = target;
                        }
                    }
                }
                yield furthest;
            }
        };
    }

    @Override
    public double getHeuristic(WorldState world) {
        return switch (type) {
            case ANY -> {
                // Return minimum heuristic (best option)
                double min = Double.MAX_VALUE;
                for (NavigationGoal goal : goals) {
                    double h = goal.getHeuristic(world);
                    if (h < min) {
                        min = h;
                    }
                }
                yield min;
            }
            case ALL -> {
                // Return sum of all heuristics (total work)
                double sum = 0;
                for (NavigationGoal goal : goals) {
                    sum += goal.getHeuristic(world);
                }
                yield sum;
            }
        };
    }

    @Override
    public boolean isValid(WorldState world) {
        return switch (type) {
            case ANY -> goals.stream().anyMatch(goal -> goal.isValid(world));
            case ALL -> goals.stream().allMatch(goal -> goal.isValid(world));
        };
    }

    /**
     * Gets the sub-goals.
     *
     * @return Immutable list of sub-goals
     */
    public List<NavigationGoal> getGoals() {
        return goals;
    }

    /**
     * Gets the composition type.
     *
     * @return ANY or ALL
     */
    public CompositionType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositeNavigationGoal that = (CompositeNavigationGoal) o;
        return Objects.equals(goals, that.goals) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(goals, type);
    }

    @Override
    public String toString() {
        return "CompositeNavigationGoal{" +
                "type=" + type +
                ", goals=" + goals +
                '}';
    }

    /**
     * Composition type for composite goals.
     */
    public enum CompositionType {
        /**
         * Goal is complete when ANY sub-goal is complete.
         * Heuristic returns minimum (closest goal).
         */
        ANY,

        /**
         * Goal is complete when ALL sub-goals are complete.
         * Heuristic returns sum of all (total distance).
         */
        ALL
    }
}
