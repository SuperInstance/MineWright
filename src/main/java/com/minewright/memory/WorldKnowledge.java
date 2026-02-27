package com.minewright.memory;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches world knowledge (blocks, entities, biome) with TTL to avoid repeated expensive scans.
 * Uses a static cache shared across all ForemanEntity instances since world data is relatively static.
 *
 * Enhanced with priority filtering to reduce prompt token usage by only including relevant information.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class WorldKnowledge {
    private final ForemanEntity minewright;
    private final int scanRadius = 16;
    private Map<Block, Integer> nearbyBlocks;
    private List<Entity> nearbyEntities;
    private String biomeName;

    // Priority lists for filtering relevant info
    private static final List<String> VALUABLE_BLOCKS = List.of(
        "diamond_ore", "deepslate_diamond_ore", "iron_ore", "deepslate_iron_ore",
        "gold_ore", "deepslate_gold_ore", "copper_ore", "deepslate_copper_ore",
        "coal_ore", "deepslate_coal_ore", "emerald_ore", "deepslate_emerald_ore",
        "redstone_ore", "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore",
        "chest", "ender_chest", "barrel", "shulker_box"
    );

    private static final List<String> HOSTILE_MOBS = List.of(
        "zombie", "skeleton", "creeper", "spider", "enderman",
        "witch", "slime", "phantom", "drowned", "pillager"
    );

    // Cache configuration - thread-safe with ConcurrentHashMap
    private static final long CACHE_TTL_MS = 2000; // 2 seconds TTL
    private static final Map<Integer, CachedWorldData> staticCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    /**
     * Cached world data with timestamp for TTL-based expiration.
     */
    private static class CachedWorldData {
        final Map<Block, Integer> blocks;
        final List<Entity> entities;
        final String biome;
        final long timestamp;

        CachedWorldData(Map<Block, Integer> blocks, List<Entity> entities, String biome) {
            this.blocks = blocks;
            this.entities = entities;
            this.biome = biome;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public WorldKnowledge(ForemanEntity minewright) {
        this.minewright = minewright;
        scan();
    }

    private void scan() {
        // Generate cache key based on position (block coordinates to reduce cache misses from small movements)
        int cacheKey = generateCacheKey();

        // Check cache first
        CachedWorldData cached = staticCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            // Cache hit - use cached data
            this.nearbyBlocks = cached.blocks;
            this.nearbyEntities = cached.entities;
            this.biomeName = cached.biome;
            return;
        }

        // Cache miss or expired - perform full scan
        scanBiome();
        scanBlocks();
        scanEntities();

        // Cache the results
        cacheResults(cacheKey);
    }

    /**
     * Generates a cache key based on the Foreman's chunk position.
     * Using chunk coordinates (>> 4) instead of block coordinates reduces cache misses
     * when the Foreman moves small distances.
     */
    private int generateCacheKey() {
        BlockPos pos = minewright.blockPosition();
        // Use chunk coordinates for the key (blocks within same chunk share cache)
        int chunkX = pos.getX() >> 4;
        int chunkY = pos.getY() >> 4;
        int chunkZ = pos.getZ() >> 4;
        // Combine into a single hash
        return (chunkX * 31 + chunkY) * 31 + chunkZ;
    }

    /**
     * Caches the current scan results.
     * Evicts oldest entries if cache is too large.
     * Thread-safe implementation.
     */
    private void cacheResults(int cacheKey) {
        // Create immutable copies for thread safety
        Map<Block, Integer> blocksCopy = Collections.unmodifiableMap(new HashMap<>(nearbyBlocks));
        List<Entity> entitiesCopy = List.copyOf(nearbyEntities);

        // Clean up expired entries first (thread-safe iteration)
        staticCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            return expired;
        });

        // Evict oldest if cache is too large (thread-safe check)
        if (staticCache.size() >= MAX_CACHE_SIZE) {
            // Remove first entry (simple FIFO eviction)
            Iterator<Map.Entry<Integer, CachedWorldData>> it = staticCache.entrySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        // Add to cache with immutable data
        staticCache.put(cacheKey, new CachedWorldData(blocksCopy, entitiesCopy, biomeName));
    }

    private void scanBiome() {
        if (minewright == null || minewright.level() == null) {
            biomeName = "unknown";
            return;
        }

        Level level = minewright.level();
        BlockPos pos = minewright.blockPosition();

        try {
            Biome biome = level.getBiome(pos).value();
            var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
            var biomeKey = biomeRegistry.getKey(biome);

            if (biomeKey != null) {
                biomeName = biomeKey.getPath();
            } else {
                biomeName = "unknown";
            }
        } catch (Exception e) {
            biomeName = "unknown";
        }
    }

    private void scanBlocks() {
        nearbyBlocks = new HashMap<>();

        if (minewright == null || minewright.level() == null) {
            return;
        }

        Level level = minewright.level();
        BlockPos minewrightPos = minewright.blockPosition();

        for (int x = -scanRadius; x <= scanRadius; x += 2) {
            for (int y = -scanRadius; y <= scanRadius; y += 2) {
                for (int z = -scanRadius; z <= scanRadius; z += 2) {
                    BlockPos checkPos = minewrightPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();

                    if (block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR) {
                        nearbyBlocks.put(block, nearbyBlocks.getOrDefault(block, 0) + 1);
                    }
                }
            }
        }
    }

    private void scanEntities() {
        nearbyEntities = new ArrayList<>();

        if (minewright == null || minewright.level() == null || minewright.getBoundingBox() == null) {
            return;
        }

        Level level = minewright.level();
        AABB searchBox = minewright.getBoundingBox().inflate(scanRadius);
        List<Entity> entities = level.getEntities(minewright, searchBox);

        if (entities != null) {
            nearbyEntities = entities;
        }
    }

    public String getBiomeName() {
        return biomeName;
    }

    public String getNearbyBlocksSummary() {
        if (nearbyBlocks.isEmpty()) {
            return "none";
        }
        
        List<Map.Entry<Block, Integer>> sorted = nearbyBlocks.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .toList();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(", ");
            Map.Entry<Block, Integer> entry = sorted.get(i);
            sb.append(entry.getKey().getName().getString());
        }
        
        return sb.toString();
    }

    public String getNearbyEntitiesSummary() {
        if (nearbyEntities.isEmpty()) {
            return "none";
        }
        
        Map<String, Integer> entityCounts = new HashMap<>();
        for (Entity entity : nearbyEntities) {
            String name = entity.getType().toString();
            entityCounts.put(name, entityCounts.getOrDefault(name, 0) + 1);
        }
        
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : entityCounts.entrySet()) {
            if (count > 0) sb.append(", ");
            sb.append(entry.getValue()).append(" ").append(entry.getKey());
            count++;
            if (count >= 5) break;
        }
        
        return sb.toString();
    }

    public Map<Block, Integer> getNearbyBlocks() {
        return nearbyBlocks;
    }

    public List<Entity> getNearbyEntities() {
        return nearbyEntities;
    }

    public String getNearbyPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player player) {
                playerNames.add(player.getName().getString());
            }
        }
        
        if (playerNames.isEmpty()) {
            return "none";
        }
        
        return String.join(", ", playerNames);
    }
}

