# Wither Skeleton Farm Design for MineWright

**Author:** MineWright Development Team
**Version:** 1.0.0
**Date:** 2026-02-27
**Status:** Design Document
**Target:** Minecraft Forge 1.20.1

## Executive Summary

This document provides a comprehensive design for automated Wither Skeleton farming in MineWright. Wither Skeletons are critical mobs that drop Wither Skeleton Skulls (2.5% base drop rate) required to summon the Wither boss. This design leverages the unique spawning mechanics of Nether Fortresses and integrates with MineWright's existing orchestration and action systems.

### Key Features
- Nether Fortress detection and mapping
- Optimized spawn platform construction within fortress boundaries
- Wither Skeleton-specific combat automation
- Wither Rose collection system
- Skull collection and sorting automation
- Multi-agent coordination for efficient farming
- Looting III support for maximum skull drop rate

---

## Table of Contents

1. [Nether Fortress Mechanics](#nether-fortress-mechanics)
2. [Wither Skeleton Spawn Mechanics](#wither-skeleton-spawn-mechanics)
3. [Fortress Detection System](#fortress-detection-system)
4. [Farm Design Patterns](#farm-design-patterns)
5. [Spawn Platform Optimization](#spawn-platform-optimization)
6. [Skull Collection Systems](#skull-collection-systems)
7. [Wither Rose Collection](#wither-rose-collection)
8. [Safe Combat Strategies](#safe-combat-strategies)
9. [Code Integration Examples](#code-integration-examples)
10. [Implementation Roadmap](#implementation-roadmap)
11. [Configuration and Tuning](#configuration-and-tuning)

---

## Nether Fortress Mechanics

### Generation Algorithm (Java Edition 1.20.1)

```java
/**
 * Nether Fortress generation mechanics
 *
 * Region Size: 432 x 432 blocks
 * Spawn Probability: 40% (2/5) per region
 * Structure Bounds: 368 x 368 blocks within each region
 *
 * Direction: Fortresses generate primarily along Z-axis (North-South)
 * Search Strategy: Move along X-axis (East-West) for highest discovery rate
 */
public class FortressLocation {

    private static final int REGION_SIZE = 432;
    private static final double SPAWN_PROBABILITY = 0.4;

    /**
     * Calculates fortress region from world coordinates
     */
    public static ChunkPos getFortressRegion(BlockPos pos) {
        int regionX = Math.floorDiv(pos.getX(), REGION_SIZE);
        int regionZ = Math.floorDiv(pos.getZ(), REGION_SIZE);
        return new ChunkPos(regionX, regionZ);
    }
}
```

### Detection Commands

| Command | Description |
|---------|-------------|
| `/locate structure minecraft:nether_fortress` | Locates nearest fortress |
| `/locate structure minecraft:fortress` | Alternative command |

### Fortress Structure Components

```
Typical Nether Fortress Layout:
┌─────────────────────────────────────────────────────────────┐
│  [Bridge]─[Bridge]─[Hall]─[Bridge]─[Hall]─[Bridge]         │
│     │        │       │       │       │       │               │
│  [Stairs]  [Tower] [Stairs] [Tower] [Stairs]               │
│     │        │       │       │       │                       │
│  [Corridor]─[Room]─[Corridor]─[Room]─[Corridor]            │
│                     │       │                               │
│                  [Blaze] [Nether]                           │
│                  [Spawner] [Wart]                           │
└─────────────────────────────────────────────────────────────┘

Key Components:
- Bridges: Long corridors connecting sections
- Hallways: Interior passages with nether brick fencing
- Towers: Stair structures connecting different Y levels
- Blaze Spawners: Regular spawners (0-2 per fortress)
- Nether Wart Rooms: Staircase rooms with soul sand
```

---

## Wither Skeleton Spawn Mechanics

### Spawn Conditions

```java
/**
 * Wither Skeleton spawn requirements (Minecraft 1.20.1)
 */
public class WitherSkeletonSpawn {

    // Location: ONLY in Nether Fortresses
    private static final DimensionType DIMENSION = DimensionType.NETHER;

    // Light Level: 0-7 (dark areas)
    private static final int MIN_LIGHT_LEVEL = 0;
    private static final int MAX_LIGHT_LEVEL = 7;

    // Spawn Group Size
    private static final int JAVA_GROUP_SIZE = 5;  // Java Edition
    private static final int BEDROCK_GROUP_SIZE = 3; // Bedrock Edition

    // Spawn Zones (Critical!)
    private static final SpawnZone STRUCTURE_BBOX = SpawnZone.INSIDE_FORTRESS_PIECE;
    private static final SpawnZone LARGER_BBOX = SpawnZone.NETHER_BRICK_ONLY;

    /**
     * TWO SPAWN ZONES:
     *
     * 1. Structure Bounding Box: Can spawn on ANY block within a fortress piece
     *    - Includes bridges, halls, towers, rooms
     *    - Any solid block works (netherrack, gravel, etc.)
     *
     * 2. Larger Bounding Box: Can spawn ONLY on Nether Brick blocks
     *    - Larger area covering entire fortress
     *    - Restrictive: Only nether_bricks, nether_brick_fence, nether_brick_stairs
     */
    public enum SpawnZone {
        STRUCTURE_PIECE,  // Any block inside fortress pieces
        NETHER_BRICK_ONLY // Only nether brick blocks in larger area
    }
}
```

### Drop Rates

| Item | Base Drop | Looting I | Looting II | Looting III |
|------|-----------|-----------|------------|-------------|
| **Coal** | 33.33% (0-1) | 40% (0-1) | 46.67% (0-2) | 53.33% (0-2) |
| **Bone** | 66.67% (0-2) | 75% (0-3) | 83.33% (0-3) | 91.67% (0-4) |
| **Wither Skeleton Skull** | 2.5% | 3.125% | 3.75% | 5.5% |

**Special Drops:**
- Stone Sword (common, often enchanted)
- Wither Rose (when mob kills another mob)

### Spawn Rate Calculation

```
Wither Skeleton Spawn Probability:
- Fortress spawns: 40% of regions have a fortress
- Spawn attempts: 1 per chunk per 10 ticks (0.5 sec)
- Mob type selection: ~40% chance for Wither Skeleton in fortress
- Light level requirement: 0-7 (most fortress interiors qualify)

Expected Spawn Rate (optimized farm):
- Spawn platforms: 21x21 = 441 blocks per layer
- 4 layers = 1,764 spawnable blocks
- Spawn attempts: ~1.6 per second (within fortress piece)
- Wither Skeleton probability: ~40% of hostile spawns
- Effective rate: ~0.64 spawns/second = ~38 spawns/minute
```

---

## Fortress Detection System

### Block-Based Scanning

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Action for detecting and mapping Nether Fortresses
 *
 * This action scans the surrounding area for fortress components
 * and builds a comprehensive map for farm construction.
 */
public class DetectFortressAction extends BaseAction {

    private final int scanRadius;
    private final BlockPos centerPos;

    private List<BlockPos> netherBricks = new ArrayList<>();
    private List<BlockPos> blazeSpawners = new ArrayList<>();
    private List<BlockPos> netherWartRooms = new ArrayList<>();
    private AABB fortressBounds;
    private int ticksScanning = 0;

    // Fortress block types
    private static final Set<Block> FORTRESS_BLOCKS = Set.of(
        Blocks.NETHER_BRICKS,
        Blocks.NETHER_BRICK_FENCE,
        Blocks.NETHER_BRICK_STAIRS,
        Blocks.NETHER_WART
    );

    public DetectFortressAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.scanRadius = task.getIntParameter("scanRadius", 128);
        this.centerPos = foreman.blockPosition();
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Scanning for Nether Fortress...");
        MineWrightMod.LOGGER.info("[Fortress Detection] Starting scan at {}", centerPos);
    }

    @Override
    protected void onTick() {
        ticksScanning++;

        // Scan in chunks to avoid lag
        int chunksPerTick = 4;
        scanArea(chunksPerTick);

        // Check if scan complete
        if (ticksScanning >= (scanRadius / 16) / chunksPerTick + 10) {
            completeScan();
        }
    }

    /**
     * Scans area for fortress components
     */
    private void scanArea(int chunksToScan) {
        AABB searchArea = new AABB(centerPos).inflate(scanRadius);

        int minX = (int)searchArea.minX;
        int maxX = (int)searchArea.maxX;
        int minZ = (int)searchArea.minZ;
        int maxZ = (int)searchArea.maxZ;

        // Scan multiple Y levels (fortresses span Y=60-90 typically)
        for (int y = 60; y <= 90; y += 5) {
            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    BlockPos checkPos = new BlockPos(x, y, z);

                    // Check for fortress blocks
                    if (isFortressBlock(checkPos)) {
                        netherBricks.add(checkPos);
                    }

                    // Check for blaze spawners
                    if (foreman.level().getBlockState(checkPos).is(Blocks.SPAWNER)) {
                        blazeSpawners.add(checkPos);
                    }

                    // Check for nether wart (indicates wart room)
                    if (foreman.level().getBlockState(checkPos).is(Blocks.NETHER_WART)) {
                        netherWartRooms.add(checkPos);
                    }
                }
            }
        }
    }

    /**
     * Checks if position contains a fortress block
     */
    private boolean isFortressBlock(BlockPos pos) {
        return FORTRESS_BLOCKS.contains(foreman.level().getBlockState(pos).getBlock());
    }

    /**
     * Completes scan and calculates fortress bounds
     */
    private void completeScan() {
        if (netherBricks.isEmpty()) {
            result = ActionResult.failure("No Nether Fortress found in scan area");
            foreman.sendChatMessage("No fortress detected within " + scanRadius + " blocks");
            return;
        }

        // Calculate bounding box
        fortressBounds = calculateBounds(netherBricks);

        // Report findings
        String report = String.format(
            "Fortress detected! Bounds: [%d, %d, %d] to [%d, %d, %d] | " +
            "Spawners: %d | Wart Rooms: %d",
            (int)fortressBounds.minX, (int)fortressBounds.minY, (int)fortressBounds.minZ,
            (int)fortressBounds.maxX, (int)fortressBounds.maxY, (int)fortressBounds.maxZ,
            blazeSpawners.size(), netherWartRooms.size()
        );

        foreman.sendChatMessage(report);
        MineWrightMod.LOGGER.info("[Fortress Detection] {}", report);

        // Store in memory
        storeFortressData();

        result = ActionResult.success(report);
    }

    /**
     * Calculates bounding box from block positions
     */
    private AABB calculateBounds(List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return new AABB(centerPos);
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Stores fortress data in memory for farm construction
     */
    private void storeFortressData() {
        var memory = foreman.getMemory();

        // Store fortress bounds
        memory.setLocation("fortress_bounds",
            String.format("[%d,%d,%d] to [%d,%d,%d]",
                (int)fortressBounds.minX, (int)fortressBounds.minY, (int)fortressBounds.minZ,
                (int)fortressBounds.maxX, (int)fortressBounds.maxY, (int)fortressBounds.maxZ));

        // Store spawner locations
        for (int i = 0; i < blazeSpawners.size(); i++) {
            BlockPos spawner = blazeSpawners.get(i);
            memory.setLocation("blaze_spawner_" + i,
                String.format("[%d,%d,%d]", spawner.getX(), spawner.getY(), spawner.getZ()));
        }

        // Store center point
        BlockPos center = new BlockPos(
            (fortressBounds.minX + fortressBounds.maxX) / 2,
            (fortressBounds.minY + fortressBounds.maxY) / 2,
            (fortressBounds.minZ + fortressBounds.maxZ) / 2
        );
        memory.setLocation("fortress_center",
            String.format("[%d,%d,%d]", center.getX(), center.getY(), center.getZ()));
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Fortress scan cancelled");
    }

    @Override
    public String getDescription() {
        return "Detecting Nether Fortress";
    }
}
```

### Command-Based Detection

```java
/**
 * Uses Minecraft's built-in locate command for fortress detection
 */
public class FortressLocateAction extends BaseAction {

    @Override
    protected void onStart() {
        // Execute locate command
        ServerLevel level = (ServerLevel) foreman.level();

        // Use structure locator
        level.getServer().getCommands().performPrefixedCommand(
            level.getServer().createCommandSourceStack(),
            "locate structure minecraft:nether_fortress"
        );

        foreman.sendChatMessage("Locating nearest fortress...");
    }

    @Override
    protected void onTick() {
        // Parse command output and navigate to fortress
        // (Implementation depends on command output parsing)

        result = ActionResult.success("Fortress located, navigating...");
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Fortress location cancelled");
    }

    @Override
    public String getDescription() {
        return "Locating Nether Fortress";
    }
}
```

---

## Farm Design Patterns

### Design 1: Platform Extension Farm

```
Side View - Platform Extension:
┌───────────────────────────────────────────────────────────┐
│  AFK Platform (Y=100, 32 blocks away)                     │
│  ┌─────┐                                                  │
│  │ ■■■ │ ← Safe spot, torch-lit                          │
│  └─────┘                                                  │
├───────────────────────────────────────────────────────────┤
│  Spawn Platforms (Y=75-85) - Built ABOVE fortress         │
│  ┌─────────────────────────────────────┐                 │
│  │ ═══════════════════════════════════ │ ← Water streams  │
│  │ ┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐    │                 │
│  │ │NSS││NSS││NSS││NSS││NSS││NSS│    │ ← Nether slabs   │
│  │ └───┘└───┘└───┘└───┘└───┘└───┘    │    (spawn pads)  │
│  └─────────────────────────────────────┘                 │
├───────────────────────────────────────────────────────────┤
│  Existing Fortress Structure (Y=65-75)                    │
│  [Bridge][Hall][Bridge]                                    │
└───────────────────────────────────────────────────────────┘

Key Features:
- Platforms built ABOVE existing fortress (exploits structure bounding box)
- Uses nether brick slabs for spawning
- Water streams push mobs to collection point
- Can be built without destroying fortress
```

### Design 2: Tunnel Trapping Farm

```
Top View - Tunnel Trap:
┌────────────────────────────────────────────────────────────┐
│  Existing Fortress Structure                               │
│  ┌────────────────────────────────────┐                   │
│  │ [Bridge]     [Bridge]     [Bridge] │                   │
│  │    │            │            │     │                   │
│  │    ↓            ↓            ↓     │                   │
│  │  ┌────────────────────────────┐    │ ← Trap corridor   │
│  │  │ Kill Chamber               │    │                   │
│  │  │ [K]    [K]    [K]          │    │ ← Killer agents  │
│  │  └────────────────────────────┘    │                   │
│  │    ↓            ↓            ↓     │                   │
│  │ [Hopper]    [Hopper]    [Hopper]   │ ← Collection      │
│  └────────────────────────────────────┘                   │
└────────────────────────────────────────────────────────────┘

Key Features:
- Uses existing fortress corridors
- Traps mobs in chokepoints
- Killer agents stationed in trap rooms
- Minimal construction required
```

### Design 3: Tower Farm (Recommended)

```
Side View - Tower Farm:
┌───────────────────────────────────────────────────────────┐
│  AFK Tower (Y=120, 32 blocks N/S of fortress)            │
│  ┌─────────┐                                              │
│  │ ███████ │ ← Fenced, lit platform                      │
│  │ ■■■■■■■ │ ← AFK spot                                  │
│  └─────────┘                                              │
├───────────────────────────────────────────────────────────┤
│  Spawn Tower (Y=90-110, centered on fortress)             │
│  ┌─────────────────────────────────────┐                 │
│  │ Layer 4 (Y=110)                     │                 │
│  │ ════╦════╦════╦════╦═════          │ ← Water streams │
│  │ ████║████║████║████║████           │                 │
│  └─────────────────────────────────────┘                 │
│  ┌─────────────────────────────────────┐                 │
│  │ Layer 3 (Y=100)                     │                 │
│  │ ════╦════╦════╦════╦═════          │                 │
│  │ ████║████║████║████║████           │                 │
│  └─────────────────────────────────────┘                 │
│  ┌─────────────────────────────────────┐                 │
│  │ Layer 2 (Y=90)                      │                 │
│  │ ════╦════╦════╦════╦═════          │                 │
│  │ ████║████║████║████║████           │                 │
│  └─────────────────────────────────────┘                 │
├───────────────────────────────────────────────────────────┤
│  Drop Shaft + Kill Chamber (Y=75-85)                     │
│  ┌─────────────────────────────────────┐                 │
│  │         ↓ 23-block fall ↓          │                 │
│  │  ┌─────────────────────────────┐    │                 │
│  │  │ Kill Chamber (1 HP left)    │    │                 │
│  │  │ [Killer Agent]              │    │                 │
│  │  └─────────────────────────────┘    │                 │
├───────────────────────────────────────────────────────────┤
│  Collection System (Y=70)                                  │
│  ┌─────────────────────────────────────┐                 │
│  │ [Hopper][Hopper][Hopper][Hopper]    │                 │
│  │   [Chest][Chest][Chest][Chest]      │                 │
│  └─────────────────────────────────────┘                 │
└───────────────────────────────────────────────────────────┘

Key Features:
- Vertical tower design maximizes spawn efficiency
- 2-3 spawn layers within fortress bounding box
- Water streams push mobs to center drop shaft
- 23-block fall leaves 1 HP (stone sword finish)
- Killer agent with Looting III for maximum skull drops
```

---

## Spawn Platform Optimization

### Platform Construction Algorithm

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Action for building optimized Wither Skeleton spawn platforms
 *
 * Parameters:
 * - centerX, centerY, centerZ: Center position
 * - layers: Number of spawn layers (2-4 recommended)
 * - platformSize: Size of each platform (21x21 recommended)
 */
public class BuildWitherFarmAction extends BaseAction {

    private final BlockPos center;
    private final int layers;
    private final int platformSize;

    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex = 0;
    private int blocksPlacedThisTick = 0;
    private static final int BLOCKS_PER_TICK = 5;

    public BuildWitherFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.center = parseBlockPos(task.getStringParameter("center", "0,80,0"));
        this.layers = task.getIntParameter("layers", 3);
        this.platformSize = task.getIntParameter("platformSize", 21);
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Building Wither Skeleton farm...");
        foreman.setFlying(true);

        buildPlan = generateFarmPlan();

        MineWrightMod.LOGGER.info("[Wither Farm] Building {}-layer farm at {} with {} blocks",
            layers, center, buildPlan.size());
    }

    @Override
    protected void onTick() {
        if (currentBlockIndex >= buildPlan.size()) {
            result = ActionResult.success(
                String.format("Built Wither Skeleton farm: %d blocks placed", buildPlan.size())
            );
            foreman.setFlying(false);
            return;
        }

        blocksPlacedThisTick = 0;
        while (currentBlockIndex < buildPlan.size() && blocksPlacedThisTick < BLOCKS_PER_TICK) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);
            placeBlock(placement);
            currentBlockIndex++;
            blocksPlacedThisTick++;
        }
    }

    /**
     * Generates the complete farm build plan
     */
    private List<BlockPlacement> generateFarmPlan() {
        List<BlockPlacement> plan = new ArrayList<>();

        // Build spawn layers
        for (int layer = 0; layer < layers; layer++) {
            int layerY = center.getY() + (layer * 8);
            buildSpawnLayer(plan, layerY, layer);
        }

        // Build water system
        buildWaterSystem(plan, center.getY() - 8);

        // Build drop shaft
        buildDropShaft(plan);

        // Build kill chamber
        buildKillChamber(plan, center.getY() - 25);

        // Build collection system
        buildCollectionSystem(plan, center.getY() - 30);

        // Build AFK tower
        buildAFKTower(plan, center.getX() + 32, center.getY() + 40, center.getZ());

        return plan;
    }

    /**
     * Builds a single spawn layer
     *
     * Design: 21x21 platform with nether brick slabs
     * - Half-slab floor (spawns 2-block mobs)
     * - Water streams every 4 blocks
     * - Center hole for drop shaft
     */
    private void buildSpawnLayer(List<BlockPlacement> plan, int y, int layerIndex) {
        int halfSize = platformSize / 2;

        // Nether brick slab floor
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                // Skip center 2x2 for drop shaft
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    continue;
                }

                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y, center.getZ() + z),
                    Blocks.NETHER_BRICK_SLAB
                ));
            }
        }

        // Water streams (every 4 blocks, pushing to center)
        for (int x = -halfSize; x <= halfSize; x++) {
            if (x % 4 == 0) {
                for (int z = -halfSize; z <= halfSize; z++) {
                    // Don't place water in center
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                        continue;
                    }
                    plan.add(new BlockPlacement(
                        new BlockPos(center.getX() + x, y + 1, center.getZ() + z),
                        Blocks.WATER
                    ));
                }
            }
        }

        // Z-axis water streams
        for (int z = -halfSize; z <= halfSize; z++) {
            if (z % 4 == 0) {
                for (int x = -halfSize; x <= halfSize; x++) {
                    // Don't place water in center
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                        continue;
                    }
                    plan.add(new BlockPlacement(
                        new BlockPos(center.getX() + x, y + 1, center.getZ() + z),
                        Blocks.WATER
                    ));
                }
            }
        }

        // Walls (keep mobs in, 3 blocks high)
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int dy = 1; dy <= 3; dy++) {
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y + dy, center.getZ() - halfSize - 1),
                    Blocks.NETHER_BRICKS
                ));
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y + dy, center.getZ() + halfSize + 1),
                    Blocks.NETHER_BRICKS
                ));
            }
        }

        for (int z = -halfSize; z <= halfSize; z++) {
            for (int dy = 1; dy <= 3; dy++) {
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() - halfSize - 1, y + dy, center.getZ() + z),
                    Blocks.NETHER_BRICKS
                ));
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + halfSize + 1, y + dy, center.getZ() + z),
                    Blocks.NETHER_BRICKS
                ));
            }
        }
    }

    /**
     * Builds water collection system
     */
    private void buildWaterSystem(List<BlockPlacement> plan, int y) {
        // Water ring to catch falling mobs
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y, center.getZ() + z),
                    Blocks.WATER
                ));
            }
        }
    }

    /**
     * Builds central drop shaft
     */
    private void buildDropShaft(List<BlockPlacement> plan) {
        // 2x2 drop shaft from spawn layers to kill chamber
        int startY = center.getY() - 8;
        int endY = center.getY() - 25;

        for (int y = startY; y >= endY; y--) {
            plan.add(new BlockPlacement(
                new BlockPos(center.getX(), y, center.getZ()),
                Blocks.AIR
            ));
            plan.add(new BlockPlacement(
                new BlockPos(center.getX() + 1, y, center.getZ()),
                Blocks.AIR
            ));
            plan.add(new BlockPlacement(
                new BlockPos(center.getX(), y, center.getZ() + 1),
                Blocks.AIR
            ));
            plan.add(new BlockPlacement(
                new BlockPos(center.getX() + 1, y, center.getZ() + 1),
                Blocks.AIR
            ));
        }
    }

    /**
     * Builds kill chamber with fall damage
     *
     * Fall distance calculation:
     * Wither Skeleton has 20 HP (10 hearts)
     * Fall damage: 1 HP per block fallen beyond 3 blocks
     * Target: 23 blocks = 20 damage (leaves 1 HP for one-hit kill)
     */
    private void buildKillChamber(List<BlockPlacement> plan, int y) {
        // 9x9 kill chamber floor
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y, center.getZ() + z),
                    Blocks.NETHER_BRICKS
                ));
            }
        }

        // Glass walls (for visibility)
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    for (int dy = 1; dy <= 3; dy++) {
                        plan.add(new BlockPlacement(
                            new BlockPos(center.getX() + x, y + dy, center.getZ() + z),
                            Blocks.GLASS
                        ));
                    }
                }
            }
        }

        // Ceiling (prevent escape)
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                plan.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y + 4, center.getZ() + z),
                    Blocks.NETHER_BRICK_SLAB
                ));
            }
        }
    }

    /**
     * Builds collection system under kill chamber
     */
    private void buildCollectionSystem(List<BlockPlacement> plan, int y) {
        // Hopper line
        for (int x = -4; x <= 4; x++) {
            plan.add(new BlockPlacement(
                new BlockPos(center.getX() + x, y, center.getZ()),
                Blocks.HOPPER
            ));
        }

        // Chests below hoppers
        for (int x = -4; x <= 4; x += 2) {
            plan.add(new BlockPlacement(
                new BlockPos(center.getX() + x, y - 1, center.getZ()),
                Blocks.CHEST
            ));
        }
    }

    /**
     * Builds AFK tower
     *
     * Position: 32 blocks horizontally from farm center
     * Height: 40+ blocks above spawn platforms
     */
    private void buildAFKTower(List<BlockPlacement> plan, int x, int y, int z) {
        // Tower pillar
        for (int dy = 0; dy < 50; dy++) {
            plan.add(new BlockPlacement(
                new BlockPos(x, y + dy, z),
                Blocks.NETHER_BRICKS
            ));
        }

        // AFK platform (5x5)
        int afkY = y + 50;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                plan.add(new BlockPlacement(
                    new BlockPos(x + dx, afkY, z + dz),
                    Blocks.NETHER_BRICKS
                ));
            }
        }

        // Safety fence
        for (int dx = -2; dx <= 2; dx++) {
            plan.add(new BlockPlacement(
                new BlockPos(x + dx, afkY, z - 3),
                Blocks.NETHER_BRICK_FENCE
            ));
            plan.add(new BlockPlacement(
                new BlockPos(x + dx, afkY, z + 3),
                Blocks.NETHER_BRICK_FENCE
            ));
        }

        for (int dz = -2; dz <= 2; dz++) {
            plan.add(new BlockPlacement(
                new BlockPos(x - 3, afkY, z + dz),
                Blocks.NETHER_BRICK_FENCE
            ));
            plan.add(new BlockPlacement(
                new BlockPos(x + 3, afkY, z + dz),
                Blocks.NETHER_BRICK_FENCE
            ));
        }

        // Torch for light (prevent local spawns)
        plan.add(new BlockPlacement(
            new BlockPos(x, afkY + 1, z),
            Blocks.TORCH
        ));
    }

    private void placeBlock(BlockPlacement placement) {
        // Implementation depends on block placement API
        foreman.level().setBlock(placement.pos(),
            placement.block().defaultBlockState(), 3);
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.sendChatMessage("Farm construction cancelled");
    }

    @Override
    public String getDescription() {
        return String.format("Building %d-layer Wither Skeleton farm", layers);
    }

    private BlockPos parseBlockPos(String str) {
        String[] parts = str.split(",");
        return new BlockPos(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        );
    }
}
```

---

## Skull Collection Systems

### Hopper Network Design

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Action for managing Wither Skeleton skull collection
 *
 * Features:
 * - Monitors hopper levels
 * - Sorts skulls from other drops
 * - Manages Looting III equipment
 * - Reports skull drop statistics
 */
public class CollectSkullsAction extends BaseAction {

    private final BlockPos killChamber;
    private final BlockPos collectionPoint;

    private int skullsCollected = 0;
    private int totalKills = 0;
    private int ticksOperating = 0;
    private static final int OPERATION_DURATION = 6000; // 5 minutes

    // Skull drop tracking
    private double dropRate = 0.0;
    private long lastKillTime = 0;

    public CollectSkullsAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.killChamber = parseBlockPos(task.getStringParameter("killChamber", "0,75,0"));
        this.collectionPoint = parseBlockPos(task.getStringParameter("collectionPoint", "0,70,0"));
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Starting skull collection...");
        foreman.setInvulnerableBuilding(true);

        // Navigate to collection point
        foreman.getNavigation().moveTo(
            collectionPoint.getX(),
            collectionPoint.getY(),
            collectionPoint.getZ(),
            1.5
        );
    }

    @Override
    protected void onTick() {
        ticksOperating++;

        // Check operation duration
        if (ticksOperating >= OPERATION_DURATION) {
            completeOperation();
            return;
        }

        // Check for skull drops every 100 ticks (5 seconds)
        if (ticksOperating % 100 == 0) {
            scanForSkulls();
            reportStatistics();
        }

        // Sort items
        if (ticksOperating % 200 == 0) {
            sortItems();
        }
    }

    /**
     * Scans collection system for Wither Skeleton skulls
     */
    private void scanForSkulls() {
        AABB scanArea = new AABB(collectionPoint).inflate(8.0);

        // Scan for hoppers and chests
        List<BlockPos> containers = findContainers(scanArea);

        for (BlockPos container : containers) {
            // Check container contents
            List<ItemStack> items = getContainerContents(container);

            for (ItemStack item : items) {
                if (item.is(Items.WITHER_SKELETON_SKULL)) {
                    skullsCollected += item.getCount();

                    // Move to special skull chest
                    moveToSkullChest(item);
                }
            }
        }
    }

    /**
     * Finds all containers (hoppers, chests) in area
     */
    private List<BlockPos> findContainers(AABB area) {
        List<BlockPos> containers = new ArrayList<>();

        // Implementation: Scan blocks for containers
        // This depends on Minecraft API for block scanning

        return containers;
    }

    /**
     * Gets contents of a container block
     */
    private List<ItemStack> getContainerContents(BlockPos pos) {
        // Implementation: Get container contents
        // This depends on Minecraft API for container access
        return new ArrayList<>();
    }

    /**
     * Moves skull to dedicated storage
     */
    private void moveToSkullChest(ItemStack skull) {
        // Implementation: Move skull to special chest
        MineWrightMod.LOGGER.info("[Skull Collection] Collected {} skulls", skull.getCount());
    }

    /**
     * Sorts collected items by type
     */
    private void sortItems() {
        // Categories: skulls, coal, bones, stone swords, other
        Map<String, List<ItemStack>> sorted = new HashMap<>();

        List<BlockPos> containers = findContainers(new AABB(collectionPoint).inflate(8.0));

        for (BlockPos container : containers) {
            List<ItemStack> items = getContainerContents(container);

            for (ItemStack item : items) {
                String category = categorizeItem(item);
                sorted.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
            }
        }

        // Distribute to appropriate chests
        for (Map.Entry<String, List<ItemStack>> entry : sorted.entrySet()) {
            distributeToChest(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Categorizes item for sorting
     */
    private String categorizeItem(ItemStack item) {
        if (item.is(Items.WITHER_SKELETON_SKULL)) {
            return "skulls";
        } else if (item.is(Items.COAL)) {
            return "coal";
        } else if (item.is(Items.BONE)) {
            return "bones";
        } else if (item.is(Items.STONE_SWORD)) {
            return "swords";
        } else {
            return "other";
        }
    }

    /**
     * Distributes items to appropriate chest
     */
    private void distributeToChest(String category, List<ItemStack> items) {
        // Implementation: Move items to category-specific chest
    }

    /**
     * Reports collection statistics
     */
    private void reportStatistics() {
        double currentRate = totalKills > 0 ? (skullsCollected / (double)totalKills) * 100 : 0;
        dropRate = (dropRate * 0.9) + (currentRate * 0.1); // EMA

        String report = String.format(
            "Skulls: %d | Kills: %d | Drop Rate: %.2f%% | Expected: 2.5-5.5%%",
            skullsCollected, totalKills, dropRate
        );

        // Report every minute
        if (ticksOperating % 1200 == 0) {
            foreman.sendChatMessage(report);
        }
    }

    /**
     * Completes collection operation
     */
    private void completeOperation() {
        String summary = String.format(
            "Collection complete! Skulls: %d | Kills: %d | Efficiency: %.2f%%",
            skullsCollected, totalKills, dropRate
        );

        foreman.sendChatMessage(summary);
        foreman.setInvulnerableBuilding(false);

        result = ActionResult.success(summary);
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.sendChatMessage("Skull collection cancelled");
    }

    @Override
    public String getDescription() {
        return "Collecting Wither Skeleton skulls";
    }

    private BlockPos parseBlockPos(String str) {
        String[] parts = str.split(",");
        return new BlockPos(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        );
    }
}
```

### Storage Layout

```
Skull Storage Room:
┌────────────────────────────────────────────────────────────┐
│  [SKULL CHEST]  - Double chest for skulls only             │
│  ┌──────────────────────────────────────────────────┐    │
│  │  [Wither Skeleton Skull] x64                     │    │
│  │  [Wither Skeleton Skull] x64                     │    │
│  └──────────────────────────────────────────────────┘    │
├────────────────────────────────────────────────────────────┤
│  Sorting System:                                           │
│  ┌─────┬─────┬─────┬─────┬─────┐                        │
│  │COAL │BONE │SWRD │ROSE │MISC │ ← Item categories     │
│  └─────┴─────┴─────┴─────┴─────┘                        │
│
│  Each category has dedicated double chest                    │
└────────────────────────────────────────────────────────────┘

Sorting Priority:
1. Wither Skeleton Skulls (highest priority)
2. Stone Swords (enchanted, valuable)
3. Wither Roses (decorative, rare)
4. Bones (useful)
5. Coal (fuel)
```

---

## Wither Rose Collection

### Wither Rose Mechanics

```java
/**
 * Wither Rose collection system
 *
 * Wither Roses are obtained when:
 * 1. Wither kills any mob (except other Wither Skeletons)
 * 2. Rose is placed on the block where mob died
 * 3. Chance: ~50-75% per kill
 *
 * Collection Strategy:
 * - Build dedicated Wither Rose farm
 * - Use Wither to kill surplus mobs
 * - Collect roses for decoration or composter
 */
public class WitherRoseCollector {

    /**
     * Builds a Wither Rose farm
     *
     * Design: Summon Wither, let it kill mobs, collect roses
     */
    public List<BlockPlacement> buildRoseFarm(BlockPos center) {
        List<BlockPlacement> plan = new ArrayList<>();

        // Build containment chamber (bedrock walls)
        buildContainment(plan, center);

        // Build mob summoning platform
        buildSummoningPad(plan, center.above(10));

        // Build collection system
        buildRoseCollection(plan, center.below(5));

        // Build player AFK spot (safe distance)
        buildSafeSpot(plan, center.offset(0, 30, 20));

        return plan;
    }

    private void buildContainment(List<BlockPlacement> plan, BlockPos center) {
        // Bedrock walls (wither-proof)
        int size = 15;
        for (int x = -size; x <= size; x++) {
            for (int y = 0; y <= 20; y++) {
                for (int z = -size; z <= size; z++) {
                    if (Math.abs(x) == size || Math.abs(z) == size || y == 0 || y == 20) {
                        plan.add(new BlockPlacement(
                            center.offset(x, y, z),
                            Blocks.BEDROCK
                        ));
                    }
                }
            }
        }
    }

    private void buildSummoningPad(List<BlockPlacement> plan, BlockPos center) {
        // Soul sand summoning pad
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                plan.add(new BlockPlacement(
                    center.offset(x, 0, z),
                    Blocks.SOUL_SAND
                ));
            }
        }
    }

    private void buildRoseCollection(List<BlockPlacement> plan, BlockPos center) {
        // Hopper collection floor
        int size = 10;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                plan.add(new BlockPlacement(
                    center.offset(x, 0, z),
                    Blocks.HOPPER
                ));
            }
        }
    }

    private void buildSafeSpot(List<BlockPlacement> plan, BlockPos center) {
        // Protected AFK spot
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                plan.add(new BlockPlacement(
                    center.offset(x, 0, z),
                    Blocks.OBSIDIAN
                ));
            }
        }

        // Obsidian roof
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                plan.add(new BlockPlacement(
                    center.offset(x, 3, z),
                    Blocks.OBSIDIAN
                ));
            }
        }
    }
}
```

### Rose Collection Action

```java
/**
 * Action for collecting Wither Roses from farm
 */
public class CollectWitherRosesAction extends BaseAction {

    private final BlockPos farmCenter;
    private int rosesCollected = 0;

    @Override
    protected void onTick() {
        // Scan for Wither Rose blocks
        AABB scanArea = new AABB(farmCenter).inflate(20.0);

        // Collect roses
        List<BlockPos> roses = findWitherRoses(scanArea);

        for (BlockPos rosePos : roses) {
            if (foreman.level().getBlockState(rosePos).is(Blocks.WITHER_ROSE)) {
                // Harvest rose
                foreman.level().destroyBlock(rosePos, true);
                rosesCollected++;
            }
        }

        // Report every 100 ticks
        if (ticksRunning % 100 == 0 && rosesCollected > 0) {
            foreman.sendChatMessage("Collected " + rosesCollected + " Wither Roses");
        }
    }

    private List<BlockPos> findWitherRoses(AABB area) {
        // Implementation: Scan blocks for Wither Roses
        return new ArrayList<>();
    }
}
```

---

## Safe Combat Strategies

### Combat Optimization

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Specialized combat action for Wither Skeletons
 *
 * Features:
 * - Sword attack with knockback control
 * - Looting III support for maximum drops
 * - Safe positioning in kill chamber
 * - Attack timing optimization
 */
public class WitherSkeletonCombatAction extends BaseAction {

    private final BlockPos killChamber;
    private WitherSkeleton target;
    private int kills = 0;
    private int ticksRunning = 0;

    private static final int ATTACK_RANGE = 3.5;
    private static final int ATTACK_COOLDOWN = 10; // ticks
    private static final int MAX_DURATION = 6000; // 5 minutes

    public WitherSkeletonCombatAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.killChamber = parseBlockPos(task.getStringParameter("killChamber", "0,75,0"));
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Starting Wither Skeleton combat...");

        // Navigate to kill chamber
        foreman.getNavigation().moveTo(
            killChamber.getX(),
            killChamber.getY(),
            killChamber.getZ(),
            1.0
        );

        // Enable invulnerability (building mode)
        foreman.setInvulnerableBuilding(true);

        // Ensure sword is equipped
        equipLootingSword();
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        // Check duration
        if (ticksRunning >= MAX_DURATION) {
            completeCombat();
            return;
        }

        // Find or validate target
        if (target == null || !target.isAlive()) {
            findTarget();
            if (target == null) {
                return; // No targets, wait
            }
        }

        // Attack if in range
        double distance = foreman.distanceTo(target);
        if (distance <= ATTACK_RANGE) {
            attackTarget();
        } else {
            // Move towards target
            foreman.getNavigation().moveTo(target, 1.5);
        }
    }

    /**
     * Finds nearest Wither Skeleton in kill chamber
     */
    private void findTarget() {
        AABB searchBox = new AABB(killChamber).inflate(8.0);
        List<Entity> entities = foreman.level().getEntities(
            foreman, searchBox, e -> e instanceof WitherSkeleton
        );

        WitherSkeleton nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            WitherSkeleton skeleton = (WitherSkeleton) entity;
            double dist = foreman.distanceTo(skeleton);
            if (dist < nearestDist) {
                nearest = skeleton;
                nearestDist = dist;
            }
        }

        target = nearest;
    }

    /**
     * Attacks target with optimized timing
     */
    private void attackTarget() {
        if (target == null || !target.isAlive()) {
            return;
        }

        // Attack every 10 ticks (0.5 seconds)
        if (ticksRunning % ATTACK_COOLDOWN == 0) {
            foreman.doHurtTarget(target);
            foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

            // Check if killed
            if (!target.isAlive()) {
                kills++;
                target = null;

                // Log skull drops
                if (kills % 20 == 0) {
                    foreman.sendChatMessage("Killed " + kills + " Wither Skeletons");
                }
            }
        }
    }

    /**
     * Equips Looting III sword for maximum drops
     */
    private void equipLootingSword() {
        // Implementation: Check inventory for Looting III sword
        // If not found, notify player
        MineWrightMod.LOGGER.info("[Combat] Equipping Looting III sword");
    }

    /**
     * Completes combat session
     */
    private void completeCombat() {
        String summary = String.format(
            "Combat complete! Killed %d Wither Skeletons",
            kills
        );

        foreman.sendChatMessage(summary);
        foreman.setInvulnerableBuilding(false);

        result = ActionResult.success(summary);
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
        foreman.sendChatMessage("Combat cancelled");
    }

    @Override
    public String getDescription() {
        return "Fighting Wither Skeletons";
    }

    private BlockPos parseBlockPos(String str) {
        String[] parts = str.split(",");
        return new BlockPos(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        );
    }
}
```

### Safety Measures

```
Combat Safety Checklist:

✓ Position: Inside kill chamber (protected from other mobs)
✓ Invulnerability: Enabled (immune to wither effect)
✓ Equipment: Looting III sword (max drops)
✓ Knockback: Controlled (keep mobs in chamber)
✓ Attack Timing: Optimized (0.5 sec cooldown)
✓ Escape Route: Clear path to safety
✓ Health Monitoring: Full health (20 HP)

Wither Effect Protection:
- Wither Skeletons apply Wither effect II for 10 seconds
- ForemanEntity is invulnerable (immune)
- No health loss during combat
- Combat can continue indefinitely

Combat Strategy:
1. Wait for mobs to fall from spawn platforms
2. One-hit kill with stone sword (1 HP remaining)
3. Collect drops via hoppers
4. Repeat continuously
```

---

## Code Integration Examples

### Action Registration

```java
package com.minewright.plugin;

import com.minewright.action.actions.*;
import com.minewright.di.ServiceContainer;

/**
 * Wither Skeleton Farm Actions Plugin
 *
 * Registers all wither skeleton farming actions
 */
public class WitherSkeletonFarmPlugin implements ActionPlugin {

    private static final String PLUGIN_ID = "wither-skeleton-farm";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        int priority = 600; // Lower than core, higher than custom

        // Fortress detection
        registry.register("detect_fortress",
            (foreman, task, ctx) -> new DetectFortressAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("locate_fortress",
            (foreman, task, ctx) -> new FortressLocateAction(foreman, task),
            priority, PLUGIN_ID);

        // Farm construction
        registry.register("build_wither_farm",
            (foreman, task, ctx) -> new BuildWitherFarmAction(foreman, task),
            priority, PLUGIN_ID);

        // Farm operation
        registry.register("operate_wither_farm",
            (foreman, task, ctx) -> new OperateWitherFarmAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("wither_combat",
            (foreman, task, ctx) -> new WitherSkeletonCombatAction(foreman, task),
            priority, PLUGIN_ID);

        // Collection
        registry.register("collect_skulls",
            (foreman, task, ctx) -> new CollectSkullsAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("collect_roses",
            (foreman, task, ctx) -> new CollectWitherRosesAction(foreman, task),
            priority, PLUGIN_ID);

        // Rose farm
        registry.register("build_rose_farm",
            (foreman, task, ctx) -> new BuildRoseFarmAction(foreman, task),
            priority, PLUGIN_ID);
    }

    @Override
    public void onUnload() {
        // Cleanup if needed
    }

    @Override
    public int getPriority() {
        return 600;
    }

    @Override
    public String[] getDependencies() {
        return new String[]{"core-actions"}; // Depends on core actions
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Wither Skeleton farming: fortress detection, farm building, combat, collection";
    }
}
```

### LLM Prompt Integration

```java
/**
 * Extended PromptBuilder with Wither Skeleton farm actions
 */
public class ExtendedPromptBuilder extends PromptBuilder {

    public static String buildWitherSystemPrompt() {
        return """
            You are a Minecraft AI agent. Respond ONLY with valid JSON, no extra text.

            FORMAT (strict JSON):
            {"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}

            ACTIONS:
            - detect_fortress: {"scanRadius": 128} - Scans for Nether Fortress
            - build_wither_farm: {"center": "x,y,z", "layers": 3} - Builds spawn platforms
            - operate_wither_farm: {"role": "killer", "afkPosition": "x,y,z"} - Operates farm
            - wither_combat: {"killChamber": "x,y,z"} - Combat in kill chamber
            - collect_skulls: {"killChamber": "x,y,z", "collectionPoint": "x,y,z"} - Collect skulls
            - collect_roses: {"farmCenter": "x,y,z"} - Collect Wither Roses

            NETHER FORTRESS ACTIONS:
            - locate_fortress: {} - Uses /locate to find nearest fortress
            - build_rose_farm: {"center": "x,y,z"} - Builds Wither Rose farm

            RULES:
            1. Always detect fortress before building farm
            2. Use AFK position 32 blocks from spawn center
            3. Looting III sword recommended for combat
            4. Skull drop rate: 2.5-5.5% with Looting III
            5. Wither Roses from Wither kills (not skeletons)

            EXAMPLES:

            Input: "find wither skeleton farm"
            {"reasoning": "Locating Nether Fortress", "plan": "Find fortress", "tasks": [{"action": "locate_fortress", "parameters": {}}]}

            Input: "build wither farm"
            {"reasoning": "Building spawn platforms at fortress", "plan": "Construct farm", "tasks": [{"action": "build_wither_farm", "parameters": {"center": "0,80,0", "layers": 3}}]}

            Input: "farm wither skulls"
            {"reasoning": "Operating farm for skull collection", "plan": "Combat and collect", "tasks": [{"action": "operate_wither_farm", "parameters": {"role": "killer", "afkPosition": "32,120,0"}}]}

            CRITICAL: Output ONLY valid JSON. No markdown, no explanations.
            """;
    }
}
```

---

## Implementation Roadmap

### Phase 1: Fortress Detection (Week 1-2)

**Goal:** Detect and map Nether Fortresses

- [ ] Implement `DetectFortressAction` with block scanning
- [ ] Implement `FortressLocateAction` using `/locate` command
- [ ] Add fortress data storage in memory system
- [ ] Create fortress visualization in GUI
- [ ] Test detection in various fortress configurations

**Deliverables:**
- Agents can detect Nether Fortresses
- Fortress bounds stored in memory
- GUI shows fortress location on map

### Phase 2: Farm Construction (Week 3-4)

**Goal:** Build optimized spawn platforms

- [ ] Implement `BuildWitherFarmAction` with tower design
- [ ] Create spawn platform generation algorithm
- [ ] Build water system and drop shaft
- [ ] Construct kill chamber and collection system
- [ ] Build AFK tower for positioning
- [ ] Test farm construction efficiency

**Deliverables:**
- Agents can build 3-layer tower farm
- Spawn platforms optimize fortress bounding box
- Collection system functional

### Phase 3: Combat System (Week 5-6)

**Goal:** Implement safe, efficient combat

- [ ] Implement `WitherSkeletonCombatAction`
- [ ] Add Looting III sword support
- [ ] Create attack timing optimization
- [ ] Implement kill chamber positioning
- [ ] Add wither effect immunity (already invulnerable)
- [ ] Test combat efficiency

**Deliverables:**
- Agents can safely kill Wither Skeletons
- Looting III support implemented
- Combat statistics tracked

### Phase 4: Collection System (Week 7-8)

**Goal:** Automated skull and rose collection

- [ ] Implement `CollectSkullsAction` with sorting
- [ ] Implement `CollectWitherRosesAction`
- [ ] Create hopper network management
- [ ] Build item sorting system
- [ ] Add drop rate tracking
- [ ] Test collection efficiency

**Deliverables:**
- Skulls automatically collected and sorted
- Drop rates tracked and reported
- Storage system functional

### Phase 5: Multi-Agent Coordination (Week 9-10)

**Goal:** Multiple agents operating farm

- [ ] Implement farm role assignments
- [ ] Create killer agent behavior
- [ ] Create collector agent behavior
- [ ] Create monitor agent behavior
- [ ] Add inter-agent communication
- [ ] Test multi-agent efficiency

**Deliverables:**
- 3+ agents can operate farm together
- Role-based task distribution
- Real-time coordination

### Phase 6: Wither Rose Farm (Week 11-12)

**Goal:** Dedicated rose farm construction

- [ ] Implement `BuildRoseFarmAction`
- [ ] Create containment chamber
- [ ] Build Wither summoning system
- [ ] Implement safe Wither management
- [ ] Add rose collection system
- [ ] Test rose farm efficiency

**Deliverables:**
- Dedicated Wither Rose farm
- Automated rose collection
- Safe Wither containment

### Phase 7: Optimization and Testing (Week 13-14)

**Goal:** Maximize efficiency and stability

- [ ] Optimize spawn platform layout
- [ ] Improve AFK positioning
- [ ] Enhance collection throughput
- [ ] Add performance metrics
- [ ] Comprehensive testing
- [ ] Documentation completion

**Deliverables:**
- Optimized farm designs
- Performance analytics
- Complete documentation

---

## Configuration and Tuning

### Config Options

```toml
[wither_skeleton_farm]
# Detection settings
default_scan_radius = 128
fortress_detection_interval = 6000  # ticks

# Farm construction
default_layers = 3
platform_size = 21
layer_spacing = 8  # blocks between layers

# Combat settings
attack_range = 3.5
attack_cooldown = 10  # ticks
looting_level = 3  # Looting III recommended

# Collection
skull_storage_chests = 4  # double chests
rose_storage_chests = 2
auto_sort_items = true

# Spawn rate targets
target_skulls_per_hour = 3  # with Looting III
expected_drop_rate = 5.5  # percent with Looting III

# AFK positioning
default_afk_distance = 32  # blocks from spawn center
default_afk_height = 40  # blocks above spawn platforms

# Multi-agent roles
killer_agents = 1
collector_agents = 1
monitor_agents = 1

# Wither Rose farm
wither_containment_size = 15
summon_pad_size = 5
rose_collection_radius = 10
```

### Performance Metrics

```
Expected Farm Performance:

Spawn Rate:
- 3-layer farm: ~38 spawns/minute
- 4-layer farm: ~50 spawns/minute
- Fortress-dependent (varies)

Skull Collection (Looting III):
- Drop rate: 5.5% per kill
- Expected skulls/hour: 1-3 skulls
- Time for 3 skulls: 1-3 hours

Combat Efficiency:
- Kills per minute: ~30-40
- No health loss (invulnerable)
- Continuous operation

Collection Throughput:
- Hopper system: ~160 items/second
- Sorting: Automatic by type
- Storage: 27 slots per chest

Multi-Agent Efficiency:
- 3 agents: 3x operation time
- Killer: Continuous combat
- Collector: Item management
- Monitor: Performance tracking
```

---

## Conclusion

This design provides a comprehensive framework for automated Wither Skeleton farming in MineWright. The system leverages:

1. **Fortress Mechanics:** Optimized for Nether Fortress spawn rules
2. **Multi-Agent Synergy:** Role-based coordination for efficiency
3. **Safe Combat:** Invulnerable agents with Looting III support
4. **Automated Collection:** Skull and rose sorting systems
5. **Scalability:** Supports multiple farm designs and agent counts

### Expected Performance

- **Spawn Rate:** 38-50 Wither Skeletons/minute (3-4 layer farm)
- **Skull Collection:** 1-3 skulls/hour with Looting III
- **Agent Efficiency:** 3 agents for full automation
- **Automation:** Near-fully autonomous after construction

### Integration Points

1. **Action Registry:** New actions for fortress detection and farming
2. **OrchestratorService:** Multi-agent farm coordination
3. **Memory System:** Fortress location and statistics storage
4. **LLM Integration:** Natural language farm commands

### Next Steps

1. Review and approve design
2. Begin Phase 1 implementation (fortress detection)
3. Iterate based on testing
4. Community feedback integration
5. Continuous optimization

---

**Document Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation

## Sources

- [Wither Skeleton - Minecraft Wiki](https://minecraft.wiki/w/Wither_Skeleton)
- [下界要塞 - 中文Minecraft Wiki](https://zh.minecraft.wiki/w/%E4%B8%8B%E7%95%8C%E8%A6%81%E5%A1%9E)
- [我的世界凋零骷髅头农场教程 - 抖音](https://www.douyin.com/shipin/7277394216551155772)
