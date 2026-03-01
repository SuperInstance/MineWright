package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents the current state of the world for goal evaluation.
 *
 * <p>This class provides a simplified view of the world for navigation goals.
 * It abstracts away Minecraft-specific details and provides a clean interface
 * for goal evaluation.</p>
 *
 * <h3>Design Purpose:</h3>
 * <p>Goals need to query the world to determine:</p>
 * <ul>
 *   <li>Where are the nearest blocks of a type?</li>
 *   <li>Where is a specific entity?</li>
 *   <li>What is the agent's current position?</li>
 *   <li>Is a position safe or dangerous?</li>
 * </ul>
 *
 * <p>WorldState wraps the Minecraft Level and provides efficient queries.</p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Create world state
 * WorldState world = new WorldState(level, agentEntity, currentPos);
 *
 * // Find nearest iron ore
 * BlockPos nearestIron = world.findNearestBlock(Blocks.IRON_ORE, 50);
 *
 * // Find specific entity
 * Entity player = world.findEntityByUUID(playerId);
 *
 * // Check safety
 * boolean safe = world.isSafe(currentPos);
 * }</pre>
 *
 * <p><b>Note:</b> This is a simplified facade. For complex queries,
 * goals can access the underlying Level directly.</p>
 *
 * @see NavigationGoal
 * @since 1.0.0
 */
public class WorldState {
    /** The Minecraft world level. */
    private final Level level;

    /** The entity performing navigation (for position tracking). */
    private final Entity entity;

    /** The agent's current position. */
    private final BlockPos currentPosition;

    /** Cached search radius for block/entity queries. */
    private final int searchRadius;

    /**
     * Creates a new world state.
     *
     * @param level The world level
     * @param entity The navigating entity
     * @param currentPosition Current position of the entity
     */
    public WorldState(Level level, Entity entity, BlockPos currentPosition) {
        this(level, entity, currentPosition, 64);
    }

    /**
     * Creates a new world state with custom search radius.
     *
     * @param level The world level
     * @param entity The navigating entity
     * @param currentPosition Current position of the entity
     * @param searchRadius Maximum search radius for block/entity queries
     */
    public WorldState(Level level, Entity entity, BlockPos currentPosition, int searchRadius) {
        this.level = level;
        this.entity = entity;
        this.currentPosition = currentPosition;
        this.searchRadius = searchRadius;
    }

    /**
     * Gets the Minecraft level.
     *
     * @return The world level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Gets the navigating entity.
     *
     * @return The entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Gets the current position.
     *
     * @return Current position
     */
    public BlockPos getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Gets the search radius.
     *
     * @return Maximum search radius
     */
    public int getSearchRadius() {
        return searchRadius;
    }

    /**
     * Gets the block state at a position.
     *
     * @param pos The position to check
     * @return Block state at position
     */
    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    /**
     * Checks if a position is loaded.
     *
     * @param pos The position to check
     * @return true if the chunk at this position is loaded
     */
    public boolean isLoaded(BlockPos pos) {
        return level.hasChunkAt(pos);
    }

    /**
     * Checks if a block is solid.
     *
     * @param pos The position to check
     * @return true if the block is solid
     */
    public boolean isSolid(BlockPos pos) {
        return isLoaded(pos) && level.getBlockState(pos).isSolidRender(level, pos);
    }

    /**
     * Finds the nearest block of a specific type.
     *
     * @param blockState The block state to search for
     * @return Nearest block position, or null if not found
     */
    public BlockPos findNearestBlock(BlockState blockState) {
        return findNearestBlock(blockState, searchRadius);
    }

    /**
     * Finds the nearest block of a specific type within radius.
     *
     * @param blockState The block state to search for
     * @param radius Maximum search radius
     * @return Nearest block position, or null if not found
     */
    public BlockPos findNearestBlock(BlockState blockState, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        int startX = currentPosition.getX() - radius;
        int startY = Math.max(level.getMinBuildHeight(), currentPosition.getY() - radius);
        int startZ = currentPosition.getZ() - radius;

        int endX = currentPosition.getX() + radius;
        int endY = Math.min(level.getMaxBuildHeight(), currentPosition.getY() + radius);
        int endZ = currentPosition.getZ() + radius;

        for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int x = startX; x <= endX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!isLoaded(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.equals(blockState)) {
                        double dist = currentPosition.distSqr(pos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Finds all blocks of a specific type within search radius.
     *
     * @param blockState The block state to search for
     * @return Collection of block positions
     */
    public Collection<BlockPos> findAllBlocks(BlockState blockState) {
        return findAllBlocks(blockState, searchRadius);
    }

    /**
     * Finds all blocks of a specific type within radius.
     *
     * @param blockState The block state to search for
     * @param radius Maximum search radius
     * @return Collection of block positions
     */
    public Collection<BlockPos> findAllBlocks(BlockState blockState, int radius) {
        java.util.List<BlockPos> found = new java.util.ArrayList<>();

        int startX = currentPosition.getX() - radius;
        int startY = Math.max(level.getMinBuildHeight(), currentPosition.getY() - radius);
        int startZ = currentPosition.getZ() - radius;

        int endX = currentPosition.getX() + radius;
        int endY = Math.min(level.getMaxBuildHeight(), currentPosition.getY() + radius);
        int endZ = currentPosition.getZ() + radius;

        for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int x = startX; x <= endX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!isLoaded(pos)) {
                        continue;
                    }

                    if (level.getBlockState(pos).equals(blockState)) {
                        found.add(pos);
                    }
                }
            }
        }

        return found;
    }

    /**
     * Finds an entity by its UUID.
     *
     * @param uuid The entity UUID
     * @return The entity, or null if not found
     */
    public Entity findEntityByUUID(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        // Use getEntities with a large bounding box to search for the entity
        AABB searchBox = new AABB(currentPosition).inflate(searchRadius);
        for (Entity entity : level.getEntities((Entity) null, searchBox)) {
            if (uuid.equals(entity.getUUID())) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Finds the nearest entity of a type.
     *
     * @param entityType The entity type class
     * @param <T> The entity type
     * @return Nearest entity, or null if not found
     */
    public <T extends Entity> T findNearestEntity(Class<T> entityType) {
        return findNearestEntity(entityType, searchRadius);
    }

    /**
     * Finds the nearest entity of a type within radius.
     *
     * @param entityType The entity type class
     * @param radius Maximum search radius
     * @param <T> The entity type
     * @return Nearest entity, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> T findNearestEntity(Class<T> entityType, int radius) {
        T nearest = null;
        double nearestDist = Double.MAX_VALUE;

        // Create search box around current position
        AABB searchBox = new AABB(currentPosition).inflate(radius);

        for (Entity entity : level.getEntitiesOfClass(entityType, searchBox)) {
            double dist = currentPosition.distSqr(entity.blockPosition());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = (T) entity;
            }
        }

        return nearest;
    }

    /**
     * Calculates distance to a position.
     *
     * @param pos The target position
     * @return Euclidean distance
     */
    public double distanceTo(BlockPos pos) {
        return Math.sqrt(currentPosition.distSqr(pos));
    }

    /**
     * Checks if a position is within a certain distance.
     *
     * @param pos The position to check
     * @param radius The radius
     * @return true if position is within radius
     */
    public boolean isWithin(BlockPos pos, double radius) {
        return currentPosition.distSqr(pos) <= radius * radius;
    }
}
