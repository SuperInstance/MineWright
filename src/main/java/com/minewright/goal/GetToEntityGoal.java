package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * A navigation goal to reach the nearest entity of a specific type.
 *
 * <p>This goal dynamically searches for entities of the target type
 * within the search radius and navigates to the closest one.</p>
 *
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li><b>Following:</b> Follow the nearest player</li>
 *   <li><b>Combat:</b> Attack nearest hostile mob</li>
 *   <li><b>Trading:</b> Find a villager</li>
 *   <li><b>Transport:</b> Locate a boat or mount</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Follow nearest player
 * NavigationGoal followPlayer = Goals.gotoEntity(EntityType.PLAYER);
 *
 * // Attack zombies
 * NavigationGoal attackZombie = Goals.gotoEntity(EntityType.ZOMBIE);
 *
 * // Find a villager
 * NavigationGoal findVillager = Goals.gotoEntity(EntityType.VILLAGER);
 * }</pre>
 *
 * <h3>Dynamic Behavior:</h3>
 * <p>The target entity can change as entities move, die, or despawn.
 * The goal is re-evaluated each time the pathfinder queries it.</p>
 *
 * <h3>Completion Criteria:</h3>
 * <p>The goal is complete when the agent is within 2 blocks
 * of the target entity.</p>
 *
 * @param <T> The entity type
 * @see NavigationGoal
 * @see Goals#gotoEntity(Class)
 * @since 1.0.0
 */
public class GetToEntityGoal<T extends Entity> implements NavigationGoal {
    /** The entity type class to search for. */
    private final Class<T> entityType;

    /** Maximum search radius for entities. */
    private final int searchRadius;

    /** Completion distance (blocks). */
    private final double completionDistance;

    /** Cached nearest entity position (for efficiency). */
    private BlockPos cachedNearest;
    /** Cached entity reference (for validity checks). */
    private Entity cachedEntity;
    /** Timestamp of cache validity. */
    private long cacheTime;

    /**
     * Creates a goal to find the nearest entity of a type.
     *
     * @param entityType The entity type class
     */
    public GetToEntityGoal(Class<T> entityType) {
        this(entityType, 64, 2.0);
    }

    /**
     * Creates a goal with custom parameters.
     *
     * @param entityType The entity type class
     * @param searchRadius Maximum search radius
     * @param completionDistance Distance to consider goal reached
     */
    public GetToEntityGoal(Class<T> entityType, int searchRadius, double completionDistance) {
        this.entityType = entityType;
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
        T nearest = findNearestEntity(world);
        if (nearest == null) {
            return false; // No entities found
        }

        BlockPos entityPos = nearest.blockPosition();
        return world.isWithin(entityPos, completionDistance);
    }

    @Override
    public BlockPos getTargetPosition(WorldState world) {
        T nearest = findNearestEntity(world);
        return nearest != null ? nearest.blockPosition() : null;
    }

    @Override
    public double getHeuristic(WorldState world) {
        T nearest = findNearestEntity(world);
        if (nearest == null) {
            return Double.MAX_VALUE; // No entities found
        }

        return world.distanceTo(nearest.blockPosition());
    }

    @Override
    public boolean isValid(WorldState world) {
        return findNearestEntity(world) != null;
    }

    /**
     * Finds the nearest entity of the target type.
     *
     * @param world The world state
     * @return Nearest entity, or null if not found
     */
    private T findNearestEntity(WorldState world) {
        // Use cache if still valid
        long currentTime = world.getLevel().getGameTime();
        if (cachedEntity != null && cachedEntity.isAlive() && cacheTime == currentTime) {
            if (entityType.isInstance(cachedEntity)) {
                @SuppressWarnings("unchecked")
                T result = (T) cachedEntity;
                return result;
            }
        }

        // Search for nearest entity
        T nearest = world.findNearestEntity(entityType, searchRadius);

        // Update cache
        cachedEntity = nearest;
        cacheTime = currentTime;
        cachedNearest = nearest != null ? nearest.blockPosition() : null;

        return nearest;
    }

    /**
     * Gets the entity type class.
     *
     * @return The entity type being searched for
     */
    public Class<T> getEntityType() {
        return entityType;
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
     * Clears the cached entity position.
     *
     * <p>Call this if the world changes significantly
     * (e.g., entities died/despawned).</p>
     */
    public void clearCache() {
        cachedNearest = null;
        cachedEntity = null;
    }

    @Override
    public String toString() {
        return "GetToEntityGoal{" +
                "type=" + entityType.getSimpleName() +
                ", searchRadius=" + searchRadius +
                '}';
    }
}
