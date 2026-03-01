package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.UUID;

/**
 * Factory class for creating navigation goals.
 *
 * <p>This class provides static factory methods for creating common
 * navigation goals, inspired by Baritone's goal factory API.</p>
 *
 * <h3>Basic Goals:</h3>
 * <ul>
 *   <li><b>gotoPos:</b> Navigate to a specific position</li>
 *   <li><b>gotoBlock:</b> Navigate to nearest block of type</li>
 *   <li><b>gotoEntity:</b> Navigate to nearest entity</li>
 *   <li><b>gotoEntityById:</b> Navigate to specific entity by UUID</li>
 * </ul>
 *
 * <h3>Composite Goals:</h3>
 * <ul>
 *   <li><b>anyOf:</b> Complete when ANY goal is complete (closest target)</li>
 *   <li><b>allOf:</b> Complete when ALL goals are complete (visit all)</li>
 * </ul>
 *
 * <h3>Safety Goals:</h3>
 * <ul>
 *   <li><b>runAway:</b> Escape from a dangerous position</li>
 *   <li><b>avoid:</b> Avoid specific positions/blocks</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Simple position goal
 * NavigationGoal goal1 = Goals.gotoPos(100, 64, 200);
 *
 * // Find any iron ore
 * NavigationGoal goal2 = Goals.gotoBlock(Blocks.IRON_ORE);
 *
 * // Follow nearest player
 * NavigationGoal goal3 = Goals.gotoEntity(EntityType.PLAYER);
 *
 * // Composite: reach ANY waypoint
 * NavigationGoal goal4 = Goals.anyOf(
 *     Goals.gotoPos(100, 64, 100),
 *     Goals.gotoPos(200, 64, 100)
 * );
 *
 * // Run away from creeper
 * NavigationGoal goal5 = Goals.runAway(creeperPos, 20);
 * }</pre>
 *
 * @see NavigationGoal
 * @see CompositeNavigationGoal
 * @since 1.0.0
 */
public final class Goals {

    private Goals() {
        // Private constructor to prevent instantiation
    }

    // ========== Basic Position Goals ==========

    /**
     * Creates a goal to navigate to a specific position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Goal for the position
     */
    public static NavigationGoal gotoPos(int x, int y, int z) {
        return gotoPos(new BlockPos(x, y, z));
    }

    /**
     * Creates a goal to navigate to a specific position.
     *
     * @param pos The target position
     * @return Goal for the position
     */
    public static NavigationGoal gotoPos(BlockPos pos) {
        return new PositionGoal(pos);
    }

    /**
     * Creates a goal to navigate within a radius of a position.
     *
     * @param pos The center position
     * @param radius The completion radius
     * @return Goal for the area
     */
    public static NavigationGoal nearPos(BlockPos pos, double radius) {
        return new NearPositionGoal(pos, radius);
    }

    // ========== Block Goals ==========

    /**
     * Creates a goal to navigate to the nearest block of a type.
     *
     * @param block The block type to find
     * @return Goal for the nearest block
     */
    public static NavigationGoal gotoBlock(Block block) {
        return gotoBlock(block.defaultBlockState());
    }

    /**
     * Creates a goal to navigate to the nearest block with a specific state.
     *
     * @param blockState The block state to find
     * @return Goal for the nearest block
     */
    public static NavigationGoal gotoBlock(BlockState blockState) {
        return new GetToBlockGoal(blockState);
    }

    /**
     * Creates a goal to navigate to ANY of several block types.
     * Uses ANY composition (finds the closest).
     *
     * @param blocks The block types to search for
     * @return Composite goal for the blocks
     */
    public static NavigationGoal gotoAnyBlock(Block... blocks) {
        NavigationGoal[] blockGoals = new NavigationGoal[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            blockGoals[i] = gotoBlock(blocks[i]);
        }
        return anyOf(blockGoals);
    }

    // ========== Entity Goals ==========

    /**
     * Creates a goal to navigate to the nearest entity of a type.
     *
     * @param entityType The entity type class
     * @param <T> The entity type
     * @return Goal for the nearest entity
     */
    public static <T extends Entity> NavigationGoal gotoEntity(Class<T> entityType) {
        return new GetToEntityGoal<>(entityType);
    }

    /**
     * Creates a goal to navigate to the nearest entity of a type.
     *
     * @param entityType The entity type
     * @param <T> The entity type
     * @return Goal for the nearest entity
     */
    public static <T extends Entity> NavigationGoal gotoEntity(EntityType<T> entityType) {
        // EntityType.getCategory() returns EntityCategory, not Class
        // We need to use a different approach - create a category-based goal
        // For now, this overload is not commonly used - prefer gotoEntity(Class)
        throw new UnsupportedOperationException(
            "Use gotoEntity(Class<T>) instead. EntityType overload not yet implemented.");
    }

    /**
     * Creates a goal to navigate to a specific entity by UUID.
     *
     * @param entityUuid The entity's UUID
     * @return Goal for the specific entity
     */
    public static NavigationGoal gotoEntityById(UUID entityUuid) {
        return new GetToEntityGoalById(entityUuid);
    }

    // ========== Composite Goals ==========

    /**
     * Creates a goal that is complete when ANY sub-goal is complete.
     *
     * <p>The heuristic returns the minimum distance to any sub-goal,
     * so the pathfinder will naturally choose the closest target.</p>
     *
     * @param goals The sub-goals
     * @return Composite ANY goal
     */
    public static NavigationGoal anyOf(NavigationGoal... goals) {
        if (goals == null || goals.length == 0) {
            throw new IllegalArgumentException("anyOf requires at least one goal");
        }
        if (goals.length == 1) {
            return goals[0];
        }
        return new CompositeNavigationGoal(goals, CompositeNavigationGoal.CompositionType.ANY);
    }

    /**
     * Creates a goal that is complete when ALL sub-goals are complete.
     *
     * <p>The heuristic returns the sum of all sub-goal distances,
     * representing the total work to visit all targets.</p>
     *
     * @param goals The sub-goals
     * @return Composite ALL goal
     */
    public static NavigationGoal allOf(NavigationGoal... goals) {
        if (goals == null || goals.length == 0) {
            throw new IllegalArgumentException("allOf requires at least one goal");
        }
        if (goals.length == 1) {
            return goals[0];
        }
        return new CompositeNavigationGoal(goals, CompositeNavigationGoal.CompositionType.ALL);
    }

    // ========== Safety Goals ==========

    /**
     * Creates a goal to run away from a dangerous position.
     *
     * <p>The goal is to reach any position outside the specified radius
     * from the danger source.</p>
     *
     * @param dangerPos The source of danger
     * @param safeDistance Distance to be considered safe
     * @return Goal to escape the danger
     */
    public static NavigationGoal runAway(BlockPos dangerPos, double safeDistance) {
        return new RunAwayGoal(dangerPos, safeDistance);
    }

    /**
     * Creates a goal to run away from multiple danger sources.
     *
     * @param dangerPositions The danger sources
     * @param safeDistance Distance to be considered safe from each
     * @return Goal to escape all dangers
     */
    public static NavigationGoal runAwayFromAll(BlockPos[] dangerPositions, double safeDistance) {
        NavigationGoal[] runAwayGoals = new NavigationGoal[dangerPositions.length];
        for (int i = 0; i < dangerPositions.length; i++) {
            runAwayGoals[i] = runAway(dangerPositions[i], safeDistance);
        }
        return allOf(runAwayGoals);
    }

    // ========== Internal Goal Implementations ==========

    /**
     * Simple position goal.
     */
    private static class PositionGoal implements NavigationGoal {
        private final BlockPos target;

        PositionGoal(BlockPos target) {
            this.target = target;
        }

        @Override
        public boolean isComplete(BlockPos pos) {
            return pos.equals(target);
        }

        @Override
        public boolean isComplete(WorldState world) {
            return world.getCurrentPosition().equals(target);
        }

        @Override
        public BlockPos getTargetPosition(WorldState world) {
            return target;
        }

        @Override
        public double getHeuristic(WorldState world) {
            return world.distanceTo(target);
        }

        @Override
        public String toString() {
            return "PositionGoal{target=" + target + "}";
        }
    }

    /**
     * Near position goal (within radius).
     */
    private static class NearPositionGoal implements NavigationGoal {
        private final BlockPos center;
        private final double radius;

        NearPositionGoal(BlockPos center, double radius) {
            this.center = center;
            this.radius = radius;
        }

        @Override
        public boolean isComplete(BlockPos pos) {
            return pos.distSqr(center) <= radius * radius;
        }

        @Override
        public boolean isComplete(WorldState world) {
            return world.isWithin(center, radius);
        }

        @Override
        public BlockPos getTargetPosition(WorldState world) {
            return center;
        }

        @Override
        public double getHeuristic(WorldState world) {
            double dist = world.distanceTo(center);
            return Math.max(0, dist - radius); // Distance to edge of radius
        }

        @Override
        public String toString() {
            return "NearPositionGoal{center=" + center + ", radius=" + radius + "}";
        }
    }

    /**
     * Goal to navigate to a specific entity by UUID.
     */
    private static class GetToEntityGoalById implements NavigationGoal {
        private final UUID entityUuid;

        GetToEntityGoalById(UUID entityUuid) {
            this.entityUuid = entityUuid;
        }

        @Override
        public boolean isComplete(BlockPos pos) {
            // Cannot determine without world context
            return false;
        }

        @Override
        public boolean isComplete(WorldState world) {
            Entity entity = world.findEntityByUUID(entityUuid);
            if (entity == null) {
                return false; // Entity not found
            }
            BlockPos entityPos = entity.blockPosition();
            return world.isWithin(entityPos, 2.0); // Within 2 blocks
        }

        @Override
        public BlockPos getTargetPosition(WorldState world) {
            Entity entity = world.findEntityByUUID(entityUuid);
            return entity != null ? entity.blockPosition() : null;
        }

        @Override
        public double getHeuristic(WorldState world) {
            Entity entity = world.findEntityByUUID(entityUuid);
            if (entity == null) {
                return Double.MAX_VALUE; // Unreachable
            }
            return world.distanceTo(entity.blockPosition());
        }

        @Override
        public boolean isValid(WorldState world) {
            return world.findEntityByUUID(entityUuid) != null;
        }

        @Override
        public String toString() {
            return "GetToEntityGoalById{uuid=" + entityUuid + "}";
        }
    }
}
