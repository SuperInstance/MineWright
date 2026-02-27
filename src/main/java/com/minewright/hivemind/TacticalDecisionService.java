package com.minewright.hivemind;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for fast tactical decisions at the edge.
 *
 * <p>Provides sub-20ms tactical reflexes for combat and hazard avoidance.
 * Falls back gracefully when the edge is unavailable.</p>
 *
 * <p><b>Decision Types:</b></p>
 * <ul>
 *   <li><b>Emergency:</b> Lava, cliffs, immediate danger → instant stop</li>
 *   <li><b>Combat:</b> Hostile mobs nearby → fight/flight decision</li>
 *   <li><b>Hazard:</b> Environmental dangers → path adjustment</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> All methods are thread-safe and designed for
 * non-blocking tick-based execution.</p>
 *
 * @since 1.2.0
 */
public class TacticalDecisionService {
    private static final TacticalDecisionService INSTANCE = new TacticalDecisionService();

    private final CloudflareClient client;
    private final int checkInterval;
    private final int syncInterval;

    // Per-agent state tracking - CRITICAL: Must be per-agent to avoid race conditions
    private final ConcurrentHashMap<String, AtomicReference<CloudflareClient.TacticalDecision>> lastDecisions = new ConcurrentHashMap<>();

    private TacticalDecisionService() {
        this.client = new CloudflareClient();
        this.checkInterval = MineWrightConfig.HIVEMIND_TACTICAL_CHECK_INTERVAL.get();
        this.syncInterval = MineWrightConfig.HIVEMIND_SYNC_INTERVAL.get();
    }

    public static TacticalDecisionService getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if Hive Mind is enabled.
     */
    public boolean isEnabled() {
        return client.isEnabled();
    }

    /**
     * Performs a tactical check for an agent.
     *
     * <p>This method is designed to be called every N ticks (configurable).
     * It's non-blocking and returns immediately with cached results from
     * previous async calls.</p>
     *
     * @param foreman The foreman entity
     * @param nearbyEntities List of nearby entities
     * @return Tactical decision (may be from cache or fallback)
     */
    public CloudflareClient.TacticalDecision checkTactical(ForemanEntity foreman, List<Entity> nearbyEntities) {
        if (!client.isEnabled()) {
            return CloudflareClient.TacticalDecision.fallback("Hive Mind disabled");
        }

        String agentId = foreman.getUUID().toString();
        BlockPos pos = foreman.blockPosition();
        float health = foreman.getHealth();

        // Build entity array for request
        JsonArray entities = buildEntityArray(nearbyEntities);

        // Build blocks array (check for hazards)
        JsonArray blocks = buildHazardBlocks(foreman, pos);

        // Calculate combat score
        float combatScore = calculateCombatScore(foreman, nearbyEntities);

        // Create request
        CloudflareClient.TacticalRequest request;
        if (hasHostileMobs(nearbyEntities)) {
            request = CloudflareClient.TacticalRequest.combatReflex(
                pos.getX(), pos.getY(), pos.getZ(), health, entities, combatScore
            );
        } else {
            request = CloudflareClient.TacticalRequest.emergencyCheck(
                pos.getX(), pos.getY(), pos.getZ(), health, entities, blocks
            );
        }

        // Make async call with caching callback
        CompletableFuture<CloudflareClient.TacticalDecision> future =
            client.getTacticalDecision(agentId, request);

        // CRITICAL: Set up callback to cache the result when it arrives
        future.thenAccept(decision -> updateLastDecision(agentId, decision));

        // Try to get result without blocking (for immediate needs)
        if (future.isDone()) {
            try {
                return future.getNow(CloudflareClient.TacticalDecision.fallback("Not ready"));
            } catch (Exception e) {
                MineWrightMod.LOGGER.debug("Tactical decision error: {}", e.getMessage());
            }
        }

        // Return per-agent cached decision or fallback
        AtomicReference<CloudflareClient.TacticalDecision> agentCache = lastDecisions.get(agentId);
        CloudflareClient.TacticalDecision cached = agentCache != null ? agentCache.get() : null;
        return cached != null ? cached : CloudflareClient.TacticalDecision.fallback("Waiting for response");
    }

    /**
     * Syncs agent state with the edge.
     *
     * <p>Called periodically (every N ticks) to keep edge state fresh.</p>
     *
     * @param foreman The foreman entity
     */
    public void syncState(ForemanEntity foreman) {
        if (!client.isEnabled()) return;

        BlockPos pos = foreman.blockPosition();
        String status = foreman.getActionExecutor().isExecuting() ? "working" : "idle";
        float health = foreman.getHealth();
        int hunger = 20;  // Default to full hunger (ForemanEntity doesn't have hunger mechanics)
        String task = foreman.getActionExecutor().getCurrentGoal();
        if (task == null) task = "none";

        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            pos.getX(), pos.getY(), pos.getZ(),
            status, health, hunger, task
        );

        client.syncState(foreman.getUUID().toString(), state)
            .thenAccept(result -> {
                if (result.synced && result.mission != null) {
                    // New mission available - would trigger mission handling
                    MineWrightMod.LOGGER.debug("Mission available from edge: {}", result.mission);
                }
            })
            .exceptionally(e -> {
                MineWrightMod.LOGGER.debug("State sync failed for {}: {}", foreman.getUUID(), e.getMessage());
                return null;
            });
    }

    /**
     * Gets the tactical check interval in ticks.
     */
    public int getCheckInterval() {
        return checkInterval;
    }

    /**
     * Gets the sync interval in ticks.
     */
    public int getSyncInterval() {
        return syncInterval;
    }

    /**
     * Checks worker health.
     */
    public CompletableFuture<Boolean> checkHealth() {
        return client.checkHealth();
    }

    // ==================== HELPERS ====================

    private JsonArray buildEntityArray(List<Entity> entities) {
        JsonArray array = new JsonArray();
        for (Entity entity : entities) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", getEntityType(entity));
            obj.addProperty("x", (int) entity.getX());
            obj.addProperty("y", (int) entity.getY());
            obj.addProperty("z", (int) entity.getZ());
            // Only LivingEntity has health
            if (entity instanceof LivingEntity living) {
                obj.addProperty("health", (int) living.getHealth());
            } else {
                obj.addProperty("health", -1);
            }
            obj.addProperty("isHostile", entity instanceof Monster);
            array.add(obj);
        }
        return array;
    }

    private JsonArray buildHazardBlocks(ForemanEntity foreman, BlockPos center) {
        JsonArray array = new JsonArray();
        int radius = 3;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    Block block = foreman.level().getBlockState(pos).getBlock();

                    String hazardType = getHazardType(block);
                    if (hazardType != null) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("type", hazardType);
                        obj.addProperty("x", pos.getX());
                        obj.addProperty("y", pos.getY());
                        obj.addProperty("z", pos.getZ());
                        array.add(obj);
                    }
                }
            }
        }
        return array;
    }

    private String getEntityType(Entity entity) {
        String typeName = entity.getType().toString().toLowerCase();
        // Extract just the entity name (e.g., "entity.minecraft.zombie" -> "zombie")
        if (typeName.contains(".")) {
            typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
        }
        return typeName;
    }

    private String getHazardType(Block block) {
        String blockName = block.toString().toLowerCase();
        if (blockName.contains("lava")) return "lava";
        if (blockName.contains("fire")) return "fire";
        if (blockName.contains("cactus")) return "cactus";
        if (blockName.contains("sweet_berry")) return "berries";
        if (blockName.contains("wither_rose")) return "wither_rose";
        if (blockName.contains("magma")) return "magma";
        return null;
    }

    private boolean hasHostileMobs(List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof Monster) {
                return true;
            }
        }
        return false;
    }

    private float calculateCombatScore(ForemanEntity foreman, List<Entity> nearbyEntities) {
        float score = 0.5f; // Base score

        // Health bonus
        score += (foreman.getHealth() / 20.0f) * 0.3f;

        // Count hostiles
        int hostileCount = 0;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Monster) {
                hostileCount++;
            }
        }

        // Penalty for multiple hostiles
        if (hostileCount > 3) {
            score -= 0.3f;
        } else if (hostileCount > 1) {
            score -= 0.1f;
        }

        return Math.max(0, Math.min(1, score));
    }

    /**
     * Updates the cached decision for a specific agent (called by async callback).
     *
     * @param agentId The agent's UUID
     * @param decision The tactical decision to cache
     */
    public void updateLastDecision(String agentId, CloudflareClient.TacticalDecision decision) {
        lastDecisions.computeIfAbsent(agentId, k -> new AtomicReference<>()).set(decision);
    }
}
