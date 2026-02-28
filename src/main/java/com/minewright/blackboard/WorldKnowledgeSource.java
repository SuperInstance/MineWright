package com.minewright.blackboard;

import com.minewright.testutil.TestLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Knowledge source for scanning and posting world state information to the blackboard.
 *
 * <p><b>Purpose:</b></p>
 * <p>WorldKnowledgeSource monitors the Minecraft world and posts observations to
 * the {@link KnowledgeArea#WORLD_STATE} area of the blackboard. This enables
 * agents to share environmental awareness without redundant scanning.</p>
 *
 * <p><b>Monitored Information:</b></p>
 * <ul>
 *   <li><b>Block States:</b> Block types and positions at coordinates</li>
 *   <li><b>Entities:</b> Hostile mobs, passive mobs, players</li>
 *   <li><b>Biomes:</b> Biome types and characteristics</li>
 *   <li><b>Structures:</b> Detected structures (buildings, caves, etc.)</li>
 * </ul>
 *
 * <p><b>Update Strategy:</b></p>
 * <p>WorldKnowledgeSource scans on demand or at intervals to balance information
 * freshness with performance. High-frequency scanning can impact server performance.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * WorldKnowledgeSource source = new WorldKnowledgeSource(level);
 *
 * // Scan area around a position
 * source.scanAround(agentPosition, 32, agentUUID);
 *
 * // Post a specific block observation
 * source.postBlock(blockPos, agentUUID);
 *
 * // Post entity detection
 * source.postEntity(entity, agentUUID);
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Scan operations should be called from the server
 * thread to ensure world access safety.</p>
 *
 * @see KnowledgeArea
 * @see Blackboard
 * @since 1.0.0
 */
public class WorldKnowledgeSource {
    private static final Logger LOGGER = TestLogger.getLogger(WorldKnowledgeSource.class);

    /**
     * The world level being monitored.
     */
    private final Level level;

    /**
     * The blackboard for posting observations.
     */
    private final Blackboard blackboard;

    /**
     * Counter for tracking observations posted.
     * Useful for debugging and statistics.
     */
    private int observationsPosted = 0;

    /**
     * Creates a new world knowledge source.
     *
     * @param level The world level to monitor
     */
    public WorldKnowledgeSource(Level level) {
        this(level, Blackboard.getInstance());
    }

    /**
     * Creates a new world knowledge source with a specific blackboard.
     *
     * @param level The world level to monitor
     * @param blackboard The blackboard for posting observations
     */
    public WorldKnowledgeSource(Level level, Blackboard blackboard) {
        this.level = level;
        this.blackboard = blackboard;
    }

    /**
     * Scans the area around a position and posts observations to the blackboard.
     *
     * <p>This method detects blocks, entities, and structures within the radius
     * and posts them as FACT entries to the WORLD_STATE area.</p>
     *
     * <p><b>Performance:</b> O(n) where n is the number of blocks/entities scanned.
     * Large radii can impact performance.</p>
     *
     * @param center Center position of the scan
     * @param radius Scan radius in blocks
     * @param sourceAgent UUID of the agent performing the scan
     * @return Number of observations posted
     */
    public int scanAround(BlockPos center, int radius, UUID sourceAgent) {
        if (level == null || center == null) {
            return 0;
        }

        int posted = 0;
        int scanCount = 0;

        // Scan blocks in horizontal slices for efficiency
        int yMin = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int yMax = Math.min(level.getMaxBuildHeight(), center.getY() + radius);

        for (int y = yMin; y <= yMax; y += 4) { // Sample every 4 blocks vertically
            for (int x = center.getX() - radius; x <= center.getX() + radius; x += 2) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += 2) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    // Skip common blocks to reduce noise
                    if (!isInterestingBlock(state)) {
                        continue;
                    }

                    postBlock(pos, sourceAgent);
                    posted++;
                    scanCount++;

                    // Limit posts per scan to prevent flooding
                    if (posted >= 50) {
                        LOGGER.debug("World scan reached post limit (50) at {}",
                            center);
                        return posted;
                    }
                }
            }
        }

        // Scan for entities
        posted += scanEntities(center, radius, sourceAgent);

        LOGGER.debug("World scan around {}: {} blocks scanned, {} observations posted",
            center, scanCount, posted);

        observationsPosted += posted;
        return posted;
    }

    /**
     * Scans for entities around a position.
     *
     * @param center Center position
     * @param radius Scan radius
     * @param sourceAgent UUID of the agent performing the scan
     * @return Number of entity observations posted
     */
    public int scanEntities(BlockPos center, int radius, UUID sourceAgent) {
        AABB scanBox = new AABB(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );

        List<Entity> entities = level.getEntities(null, scanBox);
        int posted = 0;

        for (Entity entity : entities) {
            // Skip players and non-living entities for now
            if (!entity.isAlive() || entity instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }

            postEntity(entity, sourceAgent);
            posted++;

            if (posted >= 20) {
                break; // Limit entity posts
            }
        }

        return posted;
    }

    /**
     * Posts a block observation to the blackboard.
     *
     * @param pos Position of the block
     * @param sourceAgent UUID of the agent posting this observation
     */
    public void postBlock(BlockPos pos, UUID sourceAgent) {
        if (level == null || pos == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Create entry key
        String key = String.format("block_%d_%d_%d", pos.getX(), pos.getY(), pos.getZ());

        // Create value object
        BlockObservation observation = new BlockObservation(
            pos,
            block.getDescriptionId(),
            state.toString()
        );

        // Post as FACT with high confidence
        BlackboardEntry<BlockObservation> entry = BlackboardEntry.createFact(key, observation, sourceAgent);
        blackboard.post(KnowledgeArea.WORLD_STATE, entry);
    }

    /**
     * Posts an entity observation to the blackboard.
     *
     * @param entity The entity being observed
     * @param sourceAgent UUID of the agent posting this observation
     */
    public void postEntity(Entity entity, UUID sourceAgent) {
        if (entity == null) {
            return;
        }

        String key = String.format("entity_%s_%d",
            entity.getType().getDescriptionId().replace("entity.", "").replace(".", "_"),
            entity.getId());

        EntityObservation observation = new EntityObservation(
            entity.getType().getDescriptionId(),
            entity.blockPosition(),
            entity.isAlive(),
            entity.hasCustomName() ? entity.getCustomName().getString() : null
        );

        // Post as FACT with high confidence
        BlackboardEntry<EntityObservation> entry = BlackboardEntry.createFact(key, observation, sourceAgent);
        blackboard.post(KnowledgeArea.WORLD_STATE, entry);
    }

    /**
     * Posts a hostile entity as a threat to the blackboard.
     *
     * <p>This posts to both WORLD_STATE and THREATS areas for quick access.</p>
     *
     * @param entity The hostile entity
     * @param sourceAgent UUID of the agent posting this threat
     */
    public void postThreat(Entity entity, UUID sourceAgent) {
        if (entity == null) {
            return;
        }

        String key = String.format("hostile_%s_%d",
            entity.getType().getDescriptionId().replace("entity.", "").replace(".", "_"),
            entity.getId());

        ThreatObservation observation = new ThreatObservation(
            entity.getType().getDescriptionId(),
            entity.blockPosition(),
            calculateDistance(entity),
            System.currentTimeMillis()
        );

        // Post to THREATS area
        BlackboardEntry<ThreatObservation> threatEntry =
            BlackboardEntry.createFact(key, observation, sourceAgent);
        blackboard.post(KnowledgeArea.THREATS, threatEntry);

        // Also post to WORLD_STATE
        postEntity(entity, sourceAgent);
    }

    /**
     * Posts a biome observation to the blackboard.
     *
     * @param pos Position to check biome
     * @param sourceAgent UUID of the agent posting this observation
     */
    public void postBiome(BlockPos pos, UUID sourceAgent) {
        if (level == null || pos == null) {
            return;
        }

        try {
            // getBiome returns a Holder, use registry to get the biome name
            Biome biome = level.getBiome(pos).value();
            var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
            var biomeKey = biomeRegistry.getKey(biome);

            if (biomeKey != null) {
                String key = String.format("biome_%d_%d", pos.getX(), pos.getZ());
                String biomeName = biomeKey.getPath();

                BlackboardEntry<String> entry = BlackboardEntry.createFact(key, biomeName, sourceAgent);
                blackboard.post(KnowledgeArea.WORLD_STATE, entry);
            }
        } catch (Exception e) {
            // Biome lookup failed - log and continue
            LOGGER.debug("Failed to post biome observation at {}: {}", pos, e.getMessage());
        }
    }

    /**
     * Checks if a block is interesting enough to post.
     *
     * <p>Filters out common blocks (air, dirt, stone, grass) to reduce noise.</p>
     *
     * @param state Block state to check
     * @return true if the block should be posted
     */
    private boolean isInterestingBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        var block = state.getBlock();
        String descriptionId = block.getDescriptionId();

        // Filter common blocks
        return !descriptionId.contains("dirt") &&
               !descriptionId.contains("grass_block") &&
               !descriptionId.contains("stone") &&
               !descriptionId.contains("sand") &&
               !descriptionId.contains("gravel");
    }

    /**
     * Calculates distance from this source agent to an entity.
     *
     * @param entity Target entity
     * @return Distance in blocks
     */
    private double calculateDistance(Entity entity) {
        // This is a simplified calculation
        // In practice, you'd need the agent's position
        return 0.0;
    }

    /**
     * Gets the number of observations posted by this source.
     *
     * @return Observation count
     */
    public int getObservationsPosted() {
        return observationsPosted;
    }

    /**
     * Data class for block observations.
     */
    public record BlockObservation(
        BlockPos position,
        String blockId,
        String blockState
    ) {
        @Override
        public String toString() {
            return String.format("%s at %s", blockId, position);
        }
    }

    /**
     * Data class for entity observations.
     */
    public record EntityObservation(
        String entityId,
        BlockPos position,
        boolean isAlive,
        String customName
    ) {
        @Override
        public String toString() {
            return String.format("%s at %s (alive: %s)", entityId, position, isAlive);
        }
    }

    /**
     * Data class for threat observations.
     */
    public record ThreatObservation(
        String entityId,
        BlockPos position,
        double distance,
        long detectedAt
    ) {
        @Override
        public String toString() {
            return String.format("%s at %s (distance: %.1f)", entityId, position, distance);
        }
    }
}
