# Sculk Farm Automation for MineWright

**Version:** 1.20.1
**Author:** MineWright Development Team
**Last Updated:** 2025-01-10

## Table of Contents

1. [Overview](#overview)
2. [Sculk Spreading Mechanics](#sculk-spreading-mechanics)
3. [Deep Dark Biome Detection](#deep-dark-biome-detection)
4. [Sculk Catalyst Mechanics](#sculk-catalyst-mechanics)
5. [Warden Safety Protocols](#warden-safety-protocols)
6. [Farm Design Patterns](#farm-design-patterns)
7. [XP Collection Systems](#xp-collection-systems)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Testing Strategy](#testing-strategy)

---

## Overview

Sculk farms represent a unique XP farming method in Minecraft 1.19+ that converts mob deaths into mineable sculk blocks. Unlike traditional XP farms that rely on XP orb collection, sculk farms store experience in blocks that can be mined on-demand.

### Key Advantages

- **AFK-Friendly**: XP stored in blocks, no despawn concerns
- **High Density**: 1 XP per sculk block mined
- **Low Lag**: No entity processing for XP orbs
- **Automatable**: Compatible with mining automation
- **Safe**: No direct combat required

### MineWright Integration

The Foreman AI agents can:
- Locate and navigate to Deep Dark biomes
- Construct sculk farm infrastructure
- Manage mob spawning and killing cycles
- Mine sculk blocks efficiently with hoes
- Maintain Warden safety protocols

---

## Sculk Spreading Mechanics

### The Charge System

When a mob dies near a Sculk Catalyst, it generates a "charge" based on the mob's XP value:

```
Charge = Mob's XP Drop Value
- Zombie: 5 XP
- Skeleton: 5 XP
- Spider: 5 XP
- Enderman: 5 XP
- Blaze: 10 XP
- Wither Skeleton: 5 XP
```

### Spread Rules

| Aspect | Value | Notes |
|--------|-------|-------|
| **Detection Range** | 8 blocks (Java) | Spherical radius from catalyst |
| **Max Spread Distance** | 24+ blocks | Charge decays over distance |
| **XP per Conversion** | 1 XP | Each block consumes 1 charge |
| **Decay Rate** | Distance-based | Faster decay further from catalyst |
| **Conversion Chance** | Variable | 90% skip chance on sculk blocks |

### Block Conversion Logic

```
1. On mob death within 8 blocks:
   - Catalyst blooms with soul particles
   - Charge created at death location
   - XP orbs NOT dropped (absorbed)

2. Charge spreads randomly:
   - Travels through sculk and sculk veins
   - Cannot spread through fire or soul fire
   - Decays based on distance from catalyst

3. Block conversion:
   - Non-sculk, replaceable blocks → Sculk
   - Edges → Sculk Vein (no XP consumed)
   - Near catalyst (>4 blocks) → Sculk Sensor (90%) / Shrieker (10%)
   - Within 4 blocks → Sculk Block only
```

### Convertible Blocks

Blocks tagged as `sculk_replaceable`:
- Stone, Deepslate, Granite, Diorite, Andesite
- Dirt, Grass Block, Podzol
- Sand, Gravel
- Netherrack, End Stone
- Mud, Muddy Mangrove Roots

**Non-convertible:**
- Obsidian, Crying Obsidian
- Ancient Debris
- Ores
- Logs, Wood
- Fluids

---

## Deep Dark Biome Detection

### Forge 1.20.1 API

```java
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.BiomeTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;

public class DeepDarkDetector {

    /**
     * Check if position is in Deep Dark biome
     */
    public static boolean isInDeepDark(Level level, BlockPos pos) {
        // Method 1: Using BiomeTags (recommended)
        return level.getBiome(pos).is(BiomeTags.IS_DEEP_DARK);
    }

    /**
     * Check if position is in Deep Dark biome using ResourceKey
     */
    public static boolean isInDeepDarkExplicit(Level level, BlockPos pos) {
        // Method 2: Direct biome comparison
        ResourceKey<Biome> biomeKey = level.getBiome(pos).unwrapKey().orElse(null);
        return biomeKey != null && biomeKey.equals(Biomes.DEEP_DARK);
    }

    /**
     * Find nearest Deep Dark biome from position
     */
    public static BlockPos findNearestDeepDark(Level level, BlockPos startPos, int searchRadius) {
        // Spiral search pattern
        int x = 0, z = 0;
        int dx = 0, dz = -1;

        for (int i = 0; i < searchRadius * searchRadius; i++) {
            BlockPos checkPos = new BlockPos(
                startPos.getX() + x,
                startPos.getY(),
                startPos.getZ() + z
            );

            if (isInDeepDark(level, checkPos)) {
                // Find appropriate Y level (usually below Y=0)
                return findValidDeepDarkY(level, checkPos);
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }

            x += dx;
            z += dz;
        }

        return null;
    }

    /**
     * Find valid Y level in Deep Dark (typically Y=-40 to Y=-60)
     */
    public static BlockPos findValidDeepDarkY(Level level, BlockPos pos) {
        for (int y = 0; y >= -64; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (isInDeepDark(level, checkPos)) {
                return checkPos;
            }
        }
        return null;
    }

    /**
     * Check if area contains Ancient City structures
     */
    public static boolean isNearAncientCity(Level level, BlockPos pos, int radius) {
        return level.structureManager()
            .getStructureWithPieceAt(pos,
                net.minecraft.world.level.levelgen.structure.StructureTypes.ANCIENT_CITY)
            .isValid();
    }
}
```

### Foreman Action for Deep Dark Detection

```java
package com.minewright.action.actions;

import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;
import com.minewright.action.ActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class LocateDeepDarkAction extends BaseAction {

    private int searchRadius = 1000;
    private BlockPos targetPos = null;
    private int searchTicks = 0;

    public LocateDeepDarkAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    public void tick() {
        Level level = foreman.level();
        BlockPos currentPos = foreman.blockPosition();

        // Search incrementally to avoid lag
        if (searchTicks % 20 == 0) { // Check once per second
            targetPos = DeepDarkDetector.findNearestDeepDark(
                level, currentPos, searchRadius
            );

            if (targetPos != null) {
                // Found Deep Dark - report to memory
                foreman.getMemory().recordLocation(
                    "deep_dark_biome",
                    targetPos
                );
                complete();
                return;
            }
        }

        searchTicks++;

        // Timeout after 5 minutes
        if (searchTicks > 6000) {
            fail("Could not locate Deep Dark biome within search radius");
        }
    }

    @Override
    public boolean isComplete() {
        return targetPos != null;
    }

    @Override
    public void onCancel() {
        // Cleanup if needed
    }
}
```

---

## Sculk Catalyst Mechanics

### Block Properties

| Property | Value |
|----------|-------|
| **Registry Name** | `minecraft:sculk_catalyst` |
| **Block Entity** | `minecraft:sculk_catalyst` |
| **Light Level** | 6 |
| **Hardness** | 3.0 |
| **Best Tool** | Hoe |
| **Flammability** | No |
| **Spawn** | Deep Dark biome, Ancient City chests |

### Block Entity Data (NBT)

```nbt
{
    "cursor": [
        {
            "pos": [I; x, y, z],
            "charge": 100,
            "decay_delay": 0,
            "update_delay": 0
        }
    ]
}
```

### Catalyst Detection and Collection

```java
package com.minewright.action.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public class SculkCatalystHelper {

    /**
     * Check if block is a Sculk Catalyst
     */
    public static boolean isCatalyst(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.SCULK_CATALYST);
    }

    /**
     * Check if agent has Silk Touch (required to collect catalyst)
     */
    public static boolean hasSilkTouch(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            net.minecraft.world.entity.LivingEntity living =
                (net.minecraft.world.entity.LivingEntity) entity;

            ItemStack mainHand = living.getMainHandItem();
            return mainHand.getEnchantmentLevel(
                Enchantments.SILK_TOUCH
            ) > 0;
        }
        return false;
    }

    /**
     * Calculate catalyst coverage area
     */
    public static List<BlockPos> getCoverageArea(BlockPos catalystPos) {
        List<BlockPos> coverage = new ArrayList<>();
        int radius = 8;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z <= radius*radius) {
                        coverage.add(catalystPos.offset(x, y, z));
                    }
                }
            }
        }

        return coverage;
    }

    /**
     * Count sculk blocks in coverage area
     */
    public static int countSculkBlocks(Level level, BlockPos catalystPos) {
        int count = 0;
        for (BlockPos pos : getCoverageArea(catalystPos)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SCULK) ||
                state.is(Blocks.SCULK_SENSOR) ||
                state.is(Blocks.SCULK_SHRIEKER)) {
                count++;
            }
        }
        return count;
    }
}
```

---

## Warden Safety Protocols

### Warden Detection System

```java
package com.minewright.sculk;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class WardenSafetySystem {

    /**
     * Detect Wardens in range
     */
    public static List<Warden> detectWardens(Level level, BlockPos center, int radius) {
        AABB searchBox = new AABB(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );

        return level.getEntitiesOfClass(Warden.class, searchBox);
    }

    /**
     * Check if any Warden is aggro toward agent
     */
    public static boolean isWardenAngry(List<Warden> wardens, Entity target) {
        for (Warden warden : wardens) {
            if (warden.getTarget() == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if position is safe from Warden detection
     */
    public static boolean isSafeFromWarden(Level level, BlockPos pos) {
        List<Warden> wardens = detectWardens(level, pos, 32);

        for (Warden warden : wardens) {
            double distance = warden.blockPosition().distSqr(pos);

            // Wardens can detect from ~20 blocks when calm
            if (distance < 400) { // 20 blocks squared
                // Check if Warden is suspicious
                if (warden.getSuspect() != null) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculate safe path avoiding Warden detection
     */
    public static List<BlockPos> calculateSafePath(
        Level level,
        BlockPos start,
        BlockPos end,
        List<Warden> wardens
    ) {
        // Implementation using A* pathfinding with Warden avoidance
        // Weight cells near Warden positions heavily
        // Prefer sneaking and staying on wool/wool carpets (silent)

        return PathfindingUtils.findSafePath(
            level, start, end,
            pos -> {
                double penalty = 0;
                for (Warden warden : wardens) {
                    double dist = pos.distSqr(warden.blockPosition());
                    if (dist < 900) { // 30 blocks
                        penalty += (900 - dist) / 100.0;
                    }
                }
                return penalty;
            }
        );
    }
}
```

### Stealth Action Implementation

```java
package com.minewright.action.actions;

import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;
import com.minewright.action.ActionContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class StealthMoveAction extends BaseAction {

    private BlockPos destination;
    private List<BlockPos> path;
    private int currentPathIndex = 0;
    private boolean isSneaking = true;

    public StealthMoveAction(ForemanEntity foreman, Task task, BlockPos destination) {
        super(foreman, task);
        this.destination = destination;
    }

    @Override
    public void tick() {
        Level level = foreman.level();

        // Check for Wardens before moving
        var wardens = WardenSafetySystem.detectWardens(
            level, foreman.blockPosition(), 32
        );

        if (!wardens.isEmpty()) {
            // Recalculate path avoiding Wardens
            if (currentPathIndex % 20 == 0) { // Recalculate periodically
                path = WardenSafetySystem.calculateSafePath(
                    level,
                    foreman.blockPosition(),
                    destination,
                    wardens
                );
            }

            // Check if Warden is targeting us
            if (WardenSafetySystem.isWardenAngry(wardens, foreman)) {
                // EMERGENCY: Distract and flee
                executeDistraction Maneuver();
                return;
            }
        }

        // Move along path with sneaking
        if (path != null && currentPathIndex < path.size()) {
            BlockPos nextPos = path.get(currentPathIndex);

            // Move silently (avoid vibrations)
            foreman.setSneaking(true);
            moveToPosition(nextPos);

            // Place wool carpet for silent movement
            if (level.getBlockState(nextPos.below()).isAir()) {
                level.setBlock(
                    nextPos.below(),
                    Blocks.WHITE_WOOL.defaultBlockState(),
                    3
                );
            }

            currentPathIndex++;
        } else {
            complete();
        }
    }

    private void executeDistractionManeuver() {
        // Throw snowball/arrow away from position
        // Hide in nearby location
        // Wait for Warden to calm down

        // Throw projectile
        var projectile = new net.minecraft.world.entity.projectile.Snowball(
            foreman.level(),
            foreman
        );
        projectile.shoot(
            foreman.getX() + (Math.random() - 0.5) * 20,
            foreman.getY(),
            foreman.getZ() + (Math.random() - 0.5) * 20,
            1.0f, 0.1f
        );
        foreman.level().addFreshEntity(projectile);

        // Hide
        foreman.setSneaking(true);
        task.updateStatus("Hiding from Warden");
    }

    @Override
    public boolean isComplete() {
        return foreman.blockPosition().distSqr(destination) < 4;
    }
}
```

### Warden Prevention Strategies

1. **Avoid Sculk Shriekers**
   - Destroy shriekers before they can summon
   - Sneak within 8 blocks of shriekers
   - Never activate shrieker intentionally

2. **Minimize Vibrations**
   - Always sneak when moving
   - Use wool carpets for flooring
   - Avoid placing/breaking blocks
   - Don't use projectiles near catalysts

3. **Sound Dampening**
   - Place wool blocks around work area
   - Use snow blocks for silent construction
   - Avoid bells, note blocks, and other sound sources

4. **Safe Farm Design**
   - Build farm at least 32 blocks from Ancient Cities
   - Use water streams for mob transport (silent)
   - Build killing chamber with solid walls (contain sounds)
   - Ensure no shriekers exist in spawn area

---

## Farm Design Patterns

### Pattern 1: Classic Mob Farm Conversion

```
┌─────────────────────────────────────┐
│      SPAWNING PLATFORM (Y=0)        │
│  ┌───────────────────────────────┐  │
│  │    Dark Room (24x24)          │  │
│  │    Water Streams              │  │
│  │    → ↓ ↓ ↓ ←                  │  │
│  └───────────────────────────────┘  │
│              ↓ ↓ ↓                   │
┌─────────────────────────────────────┐
│    KILLING CHAMBER (Y=-10)          │
│  ┌───────────────────────────────┐  │
│  │   [SCULK CATALYST]            │  │
│  │   [SCULK CATALYST]            │  │
│  │   (4x4 grid for coverage)     │  │
│  │                               │  │
│  │   Mobs fall into:             │  │
│  │   - Lava blade (instant)      │  │
│  │   - Iron golem (slow)         │  │
│  │   - Trapdoor + drop (22+)     │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
│    SCULK GROWTH AREA                │
│  (Radius 8 from each catalyst)      │
│  Replace floor with stone/dirt      │
└─────────────────────────────────────┘
```

### Pattern 2: Spreader Farm (High Efficiency)

```
         Spawning Platform
                 ↓
    [Water Stream System]
         ↓     ↓     ↓
    ┌─────────────────────┐
    │  Catalyst Grid      │
    │  [C] [C] [C] [C]    │  ← 4 Catalysts
    │  [C] [C] [C] [C]    │  (4x4 grid)
    │                     │
    │  Growth Medium:     │
    │  - Stone (primary)  │
    │  - Deepslate (alt)  │
    │  - Dirt (backup)    │
    └─────────────────────┘
         ↓     ↓     ↓
    [Collection Hoppers]
         ↓     ↓     ↓
    [Storage System]
```

### Pattern 3: Underground Ancient City Raid

```
        [Deep Dark Biome]
                ↓
    ┌─────────────────────┐
    │  Ancient City       │
    │  Detection Tunnel   │  ← Sneak through wool
    │  (wool carpet)      │
    └─────────────────────┘
                ↓
    ┌─────────────────────┐
    │  Loot Scavenging    │  ← Collect catalysts
    │  - Chests (16%)     │  from chests
    │  - Natural spawns   │
    └─────────────────────┘
                ↓
    ┌─────────────────────┐
    │  Farm Construction  │  ← Build farm on-site
    │  - Use city walls   │  using existing catalysts
    │  - Clear shriekers  │
    └─────────────────────┘
```

### Pattern 4: Nether Portal-Based (High Value)

```
    [Overworld]                    [Nether]
        ↓                              ↓
    [Spawning Pad]              [Zombie Piglin Farm]
        ↓                              ↓
    [Portal Edge]              → [Sculk Catalyst]
        ↓                              ↓
    [Catalyst Array]        ← [High XP Conversion]
        ↓                              ↓
    [Sculk Growth]                  [Teleport]
        ↓                              ↓
    [Collection System]     [Back to Overworld]
```

---

## XP Collection Systems

### Manual Mining (Basic)

```java
package com.minewright.action.actions;

public class MineSculkAction extends BaseAction {

    private BlockPos center;
    private int radius;
    private int blocksMined = 0;
    private int targetBlocks;

    public MineSculkAction(ForemanEntity foreman, Task task,
                          BlockPos center, int radius) {
        super(foreman, task);
        this.center = center;
        this.radius = radius;
        this.targetBlocks = countSculkInRadius(center, radius);
    }

    @Override
    public void tick() {
        // Equip hoe (fastest mining tool)
        equipTool(Items.DIAMOND_HOE);

        // Find nearest sculk block
        BlockPos target = findNearestSculk(center, radius);

        if (target != null) {
            // Mine the block
            mineBlock(target);
            blocksMined++;

            // 1 XP per block
            task.updateProgress(blocksMined, targetBlocks);
        } else {
            complete(); // All sculk mined
        }
    }

    @Override
    public boolean isComplete() {
        return blocksMined >= targetBlocks;
    }
}
```

### Automated Mining (Advanced)

```java
package com.minewright.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;

public class SculkMiningAutomation {

    /**
     * Setup automated mining system using:
     * - Hoppers for collection
     * - Minecarts with hoppers
     * - Redstone clocks for timing
     */
    public static void buildMiningSystem(Level level, BlockPos catalystPos) {
        // Clear area around catalyst
        int radius = 8;
        for (BlockPos pos : getCoverageArea(catalystPos)) {
            if (pos.distSqr(catalystPos) > 4) { // Keep 2 blocks around catalyst
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // Build hopper grid
        buildHopperGrid(level, catalystPos);

        // Create collection minecart track
        buildMinecartTrack(level, catalystPos);
    }

    private static void buildHopperGrid(Level level, BlockPos center) {
        // Build hoppers every 4 blocks
        for (int x = -8; x <= 8; x += 4) {
            for (int z = -8; z <= 8; z += 4) {
                BlockPos hopperPos = center.offset(x, -1, z);
                level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);

                // Connect to chest below
                BlockPos chestPos = hopperPos.below();
                level.setBlock(chestPos,
                    Blocks.CHEST.defaultBlockState(), 3);
            }
        }
    }

    private static void buildMinecartTrack(Level level, BlockPos center) {
        // Build rail loop for hopper minecarts
        int radius = 10;
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            int x = (int) Math.round(radius * Math.cos(rad));
            int z = (int) Math.round(radius * Math.sin(rad));

            BlockPos railPos = center.offset(x, 0, z);
            level.setBlock(railPos,
                Blocks.POWERED_RAIL.defaultBlockState(), 3);

            // Place hopper minecart every 8 rails
            if (angle % 90 == 0) {
                var minecart = EntityType.MINECART_HOPPER.create(
                    level, null, null, null,
                    railPos, MobSpawnType.SPAWN_EGG
                );
                level.addFreshEntity(minecart);
            }
        }
    }
}
```

### XP Storage System

```java
package com.minewright.storage;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;

public class XPStorageSystem {

    private final SimpleContainer sculkStorage;
    private int totalXPStored;

    public XPStorageSystem(int slots) {
        this.sculkStorage = new SimpleContainer(slots);
        this.totalXPStored = 0;
    }

    /**
     * Store sculk blocks (each = 1 XP)
     */
    public void storeSculk(ItemStack sculkStack) {
        int xpAmount = sculkStack.getCount();
        totalXPStored += xpAmount;

        // Add to storage
        for (int i = 0; i < sculkStorage.getContainerSize(); i++) {
            ItemStack existing = sculkStorage.getItem(i);
            if (existing.isEmpty()) {
                sculkStorage.setItem(i, sculkStack.copy());
                break;
            } else if (existing.is(sculkStack.getItem())) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, sculkStack.getCount());
                existing.grow(toAdd);
                sculkStack.shrink(toAdd);
                if (sculkStack.isEmpty()) break;
            }
        }
    }

    /**
     * Withdraw XP (by mining sculk from storage)
     */
    public int withdrawXP(int amount) {
        int withdrawn = 0;

        for (int i = 0; i < sculkStorage.getContainerSize(); i++) {
            ItemStack stack = sculkStorage.getItem(i);
            if (!stack.isEmpty()) {
                int toRemove = Math.min(stack.getCount(), amount - withdrawn);
                stack.shrink(toRemove);
                withdrawn += toRemove;
                totalXPStored -= toRemove;

                if (withdrawn >= amount) break;
            }
        }

        return withdrawn;
    }

    public int getTotalXPStored() {
        return totalXPStored;
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("TotalXP", totalXPStored);
        // Save container contents
        ListTag items = new ListTag();
        for (int i = 0; i < sculkStorage.getContainerSize(); i++) {
            ItemStack stack = sculkStorage.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                items.add(itemTag);
            }
        }
        tag.put("Items", items);
    }
}
```

---

## Code Examples

### Complete Sculk Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;
import com.minewright.action.ActionContext;
import com.minewright.sculk.WardenSafetySystem;
import com.minewright.sculk.DeepDarkDetector;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.monster.warden.Warden;

import java.util.List;

public class SculkFarmAction extends BaseAction {

    private enum Phase {
        DETECT_BIOME,
        ASSESS_SAFETY,
        SETUP_FARM,
        RUN_FARM_CYCLE,
        MINE_SCULK,
        COMPLETE
    }

    private Phase currentPhase = Phase.DETECT_BIOME;
    private BlockPos farmLocation;
    private BlockPos catalystLocation;
    private int farmCycles = 0;
    private int maxCycles = 10;
    private int sculkMined = 0;

    public SculkFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    public void tick() {
        switch (currentPhase) {
            case DETECT_BIOME:
                detectAndNavigateToBiome();
                break;

            case ASSESS_SAFETY:
                assessWardenThreat();
                break;

            case SETUP_FARM:
                setupFarmInfrastructure();
                break;

            case RUN_FARM_CYCLE:
                runFarmingCycle();
                break;

            case MINE_SCULK:
                mineSculkBlocks();
                break;

            case COMPLETE:
                complete();
                break;
        }
    }

    private void detectAndNavigateToBiome() {
        task.updateStatus("Locating Deep Dark biome...");

        BlockPos nearestDeepDark = DeepDarkDetector.findNearestDeepDark(
            foreman.level(),
            foreman.blockPosition(),
            2000
        );

        if (nearestDeepDark != null) {
            farmLocation = nearestDeepDark;
            foreman.getMemory().recordLocation("sculk_farm", farmLocation);
            task.updateStatus("Found Deep Dark at " + farmLocation);
            currentPhase = Phase.ASSESS_SAFETY;
        } else {
            fail("Could not locate Deep Dark biome");
        }
    }

    private void assessWardenThreat() {
        task.updateStatus("Assessing Warden threat level...");

        List<Warden> wardens = WardenSafetySystem.detectWardens(
            foreman.level(), farmLocation, 64
        );

        if (!wardens.isEmpty()) {
            task.updateStatus("Warning: " + wardens.size() + " Wardens nearby");
            // Implement stealth navigation
            executeStealthNavigation();
        } else {
            task.updateStatus("Area appears safe");
            currentPhase = Phase.SETUP_FARM;
        }
    }

    private void executeStealthNavigation() {
        // Navigate using wool carpet path
        // Sneak at all times
        // Avoid vibrations

        foreman.setSneaking(true);
        task.updateStatus("Moving stealthily...");

        // Check if arrived
        if (foreman.blockPosition().distSqr(farmLocation) < 25) {
            currentPhase = Phase.SETUP_FARM;
        }
    }

    private void setupFarmInfrastructure() {
        task.updateStatus("Building sculk farm infrastructure...");

        Level level = foreman.level();
        BlockPos buildPos = farmLocation;

        // Check for existing catalyst
        catalystLocation = findNearbyCatalyst(buildPos);

        if (catalystLocation == null) {
            // Need to place catalyst (should have from inventory)
            if (hasCatalystInInventory()) {
                placeCatalyst(buildPos);
                catalystLocation = buildPos;
            } else {
                fail("No Sculk Catalyst available");
                return;
            }
        }

        // Build spawning platform
        buildSpawningPlatform(catalystLocation.above(10));

        // Build killing chamber
        buildKillingChamber(catalystLocation);

        // Clear growth area
        clearGrowthArea(catalystLocation);

        task.updateStatus("Farm infrastructure complete");
        currentPhase = Phase.RUN_FARM_CYCLE;
    }

    private BlockPos findNearbyCatalyst(BlockPos center) {
        Level level = foreman.level();
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-16, -16, -16),
            center.offset(16, 16, 16)
        )) {
            if (level.getBlockState(pos).is(Blocks.SCULK_CATALYST)) {
                return pos;
            }
        }
        return null;
    }

    private boolean hasCatalystInInventory() {
        return foreman.getInventory().hasItem(
            Items.SCULK_CATALYST
        );
    }

    private void placeCatalyst(BlockPos pos) {
        Level level = foreman.level();
        level.setBlock(pos, Blocks.SCULK_CATALYST.defaultBlockState(), 3);
        task.updateStatus("Placed Sculk Catalyst");
    }

    private void buildSpawningPlatform(BlockPos pos) {
        Level level = foreman.level();
        int size = 24;

        // Build dark room
        for (int x = -size/2; x <= size/2; x++) {
            for (int z = -size/2; z <= size/2; z++) {
                // Floor
                level.setBlock(
                    pos.offset(x, 0, z),
                    Blocks.STONE.defaultBlockState(),
                    3
                );
            }
        }

        // Build walls (dark room)
        for (int y = 1; y <= 3; y++) {
            for (int x = -size/2; x <= size/2; x++) {
                level.setBlock(pos.offset(x, y, -size/2),
                    Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos.offset(x, y, size/2),
                    Blocks.STONE.defaultBlockState(), 3);
            }
            for (int z = -size/2; z <= size/2; z++) {
                level.setBlock(pos.offset(-size/2, y, z),
                    Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos.offset(size/2, y, z),
                    Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private void buildKillingChamber(BlockPos catalystPos) {
        Level level = foreman.level();

        // Build funnel to direct mobs to catalyst
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    // Walls
                    for (int y = 0; y <= 5; y++) {
                        level.setBlock(
                            catalystPos.offset(x, y, z),
                            Blocks.STONE.defaultBlockState(),
                            3
                        );
                    }
                }
            }
        }

        // Place trapdoor system for drop kill
        level.setBlock(catalystPos.above(6),
            Blocks.IRON_TRAPDOOR.defaultBlockState(), 3);
    }

    private void clearGrowthArea(BlockPos center) {
        Level level = foreman.level();
        int radius = 8;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius)
        )) {
            if (pos.distSqr(center) <= radius * radius) {
                BlockState state = level.getBlockState(pos);
                // Replace air/replaceable blocks with stone
                if (state.isAir() || isReplaceable(state)) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
    }

    private boolean isReplaceable(BlockState state) {
        return state.getMaterial().isReplaceable();
    }

    private void runFarmingCycle() {
        task.updateStatus("Running farm cycle " + (farmCycles + 1) + "/" + maxCycles);

        // Wait for mobs to spawn and die (simulated)
        // In reality, this would wait for actual mob farm operation

        farmCycles++;

        if (farmCycles >= maxCycles) {
            task.updateStatus("Farm cycles complete");
            currentPhase = Phase.MINE_SCULK;
        }
    }

    private void mineSculkBlocks() {
        task.updateStatus("Mining sculk blocks...");

        Level level = foreman.level();

        // Equip hoe
        equipBestHoe();

        // Mine sculk in radius
        int radius = 8;
        for (BlockPos pos : BlockPos.betweenClosed(
            catalystLocation.offset(-radius, -1, -radius),
            catalystLocation.offset(radius, 1, radius)
        )) {
            BlockState state = level.getBlockState(pos);

            if (state.is(Blocks.SCULK)) {
                // Mine the block
                level.destroyBlock(pos, true); // Drop XP
                sculkMined++;

                task.updateProgress(sculkMined, 100); // Target 100 sculk

                if (sculkMined >= 100) {
                    task.updateStatus("Mined " + sculkMined + " sculk blocks");
                    currentPhase = Phase.COMPLETE;
                    return;
                }
            }
        }

        // If no more sculk, complete
        if (sculkMined == 0) {
            task.updateStatus("No sculk blocks to mine");
            currentPhase = Phase.COMPLETE;
        }
    }

    private void equipBestHoe() {
        // Find best hoe in inventory
        // Priority: Netherite > Diamond > Iron > Stone > Wooden
    }

    @Override
    public boolean isComplete() {
        return currentPhase == Phase.COMPLETE;
    }

    @Override
    public void onCancel() {
        task.updateStatus("Farm operation cancelled");
        // Cleanup: remove temporary blocks
    }
}
```

### LLM Prompt Integration

```java
// Add to PromptBuilder.java

public class PromptBuilder {

    public String buildPromptWithSculkFarming(ForemanMemory memory,
                                             WorldKnowledge world) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(BASE_SYSTEM_PROMPT).append("\n\n");

        prompt.append("# Sculk Farming Capabilities\n\n");
        prompt.append("You can automate sculk farms for XP production:\n\n");

        prompt.append("## Sculk Mechanics\n");
        prompt.append("- Sculk Catalysts convert mob deaths to sculk blocks\n");
        prompt.append("- Each sculk block = 1 XP when mined\n");
        prompt.append("- Catalyst range: 8 block radius\n");
        prompt.append("- Best XP mobs: Endermen (5), Blazes (10)\n\n");

        prompt.append("## Available Actions\n");
        prompt.append("1. `locate_deep_dark` - Find nearest Deep Dark biome\n");
        prompt.append("2. `build_sculk_farm` - Construct sculk farm\n");
        prompt.append("3. `mine_sculk` - Harvest sculk blocks for XP\n");
        prompt.append("4. `warden_safety_check` - Assess Warden threat\n");
        prompt.append("5. `stealth_move` - Navigate without detection\n\n");

        prompt.append("## Warden Safety\n");
        prompt.append("- ALWAYS check for Wardens before building\n");
        prompt.append("- Sneak at all times in Deep Dark\n");
        prompt.append("- Use wool carpets for silent movement\n");
        prompt.append("- Avoid Sculk Shriekers (destroy them first)\n");
        prompt.append("- If Warden detected: retreat and reassess\n\n");

        prompt.append("## Farm Design Principles\n");
        prompt.append("- Build spawning platform 20+ blocks above catalyst\n");
        prompt.append("- Use water streams or drop for killing\n");
        prompt.append("- Place growth blocks (stone/deepslate) in radius\n");
        prompt.append("- Mine with hoes for fastest collection\n\n");

        // Add world context
        prompt.append(buildWorldContext(world));

        // Add memory context
        prompt.append(buildMemoryContext(memory));

        return prompt.toString();
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Mechanics (Week 1-2)

**Tasks:**
- [ ] Create `DeepDarkDetector` utility class
- [ ] Create `SculkCatalystHelper` utility class
- [ ] Create `WardenSafetySystem` utility class
- [ ] Implement biome detection API integration
- [ ] Add sculk block constants and registries

**Deliverables:**
- `com.minewright.sculk` package with core utilities
- Unit tests for detection systems
- Documentation for API usage

### Phase 2: Action Implementation (Week 2-3)

**Tasks:**
- [ ] Implement `LocateDeepDarkAction`
- [ ] Implement `StealthMoveAction`
- [ ] Implement `SculkFarmAction` (complete farm cycle)
- [ ] Implement `MineSculkAction`
- [ ] Add sculk-related tasks to ActionRegistry

**Deliverables:**
- All sculk farming actions
- Integration with plugin system
- Action completion handlers

### Phase 3: Warden Safety (Week 3-4)

**Tasks:**
- [ ] Implement Warden detection (entities, sounds)
- [ ] Create stealth pathfinding algorithm
- [ ] Implement distraction mechanics
- [ ] Add Warden avoidance to state machine
- [ ] Create emergency evacuation protocols

**Deliverables:**
- Complete Warden safety system
- Stealth navigation action
- Emergency response handlers

### Phase 4: Infrastructure (Week 4-5)

**Tasks:**
- [ ] Create structure generators for farms
- [ ] Implement automated mining system
- [ ] Create XP storage system
- [ ] Add farm maintenance actions
- [ ] Implement collection system (hoppers, minecarts)

**Deliverables:**
- Farm structure templates
- Automation components
- Storage and collection systems

### Phase 5: LLM Integration (Week 5-6)

**Tasks:**
- [ ] Extend PromptBuilder for sculk farming
- [ ] Add sculk context to memory system
- [ ] Create specialized prompts for Deep Dark
- [ ] Implement task decomposition for farms
- [ ] Add safety prompts for Warden areas

**Deliverables:**
- Updated prompt templates
- Context-aware sculk planning
- Safety-focused LLM responses

### Phase 6: Testing & Optimization (Week 6-7)

**Tasks:**
- [ ] Unit tests for all components
- [ ] Integration tests in test world
- [ ] Performance optimization (chunk loading)
- [ ] Warden AI testing
- [ ] Farm efficiency metrics

**Deliverables:**
- Complete test suite
- Performance benchmarks
- Bug fixes and optimizations

### Phase 7: Documentation (Week 7-8)

**Tasks:**
- [ ] Complete this documentation
- [ ] Create quick-start guide
- [ ] Add in-game tutorial
- [ ] Create video demonstration
- [ ] Write troubleshooting guide

**Deliverables:**
- User documentation
- Developer documentation
- Tutorial materials

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testDeepDarkDetection() {
    // Create test level with Deep Dark biome
    Level level = createTestLevel();
    BlockPos deepDarkPos = new BlockPos(0, -50, 0);

    assertTrue(DeepDarkDetector.isInDeepDark(level, deepDarkPos));
    assertFalse(DeepDarkDetector.isInDeepDark(level, new BlockPos(0, 100, 0)));
}

@Test
 public void testWardenDetection() {
    Level level = createTestLevel();
    BlockPos playerPos = new BlockPos(0, 0, 0);
    BlockPos wardenPos = new BlockPos(10, 0, 0);

    // Spawn Warden
    Warden warden = new Warden(EntityType.WARDEN, level);
    warden.setPos(wardenPos);
    level.addFreshEntity(warden);

    List<Warden> detected = WardenSafetySystem.detectWardens(level, playerPos, 32);
    assertEquals(1, detected.size());
    assertEquals(warden, detected.get(0));
}

@Test
public void testSculkCoverage() {
    BlockPos catalystPos = new BlockPos(0, 0, 0);
    List<BlockPos> coverage = SculkCatalystHelper.getCoverageArea(catalystPos);

    // Should cover 8-block radius sphere
    assertTrue(coverage.size() > 2000); // Approximate sphere volume
    assertTrue(coverage.contains(catalystPos.offset(8, 0, 0)));
    assertFalse(coverage.contains(catalystPos.offset(9, 0, 0)));
}
```

### Integration Tests

```java
@Test
public void testCompleteFarmCycle() {
    // Setup test environment
    Level level = createTestLevel();
    ForemanEntity foreman = spawnTestForeman(level);

    // Create sculk farm action
    SculkFarmAction action = new SculkFarmAction(foreman,
        new Task("Build sculk farm"));

    // Run action (simulate ticks)
    for (int i = 0; i < 10000; i++) {
        action.tick();
        if (action.isComplete()) break;
    }

    // Verify results
    assertTrue(action.isComplete());
    assertNotNull(action.farmLocation);
    assertTrue(action.sculkMined > 0);
}
```

### Performance Benchmarks

```java
@Test
public void benchmarkBiomeSearch() {
    Level level = createLargeTestLevel();

    long start = System.nanoTime();
    BlockPos result = DeepDarkDetector.findNearestDeepDark(
        level, new BlockPos(0, 0, 0), 5000
    );
    long duration = System.nanoTime() - start;

    // Should complete in under 100ms
    assertTrue(duration < 100_000_000);
    System.out.println("Search took: " + (duration / 1_000_000) + "ms");
}
```

---

## Troubleshooting

### Common Issues

**Issue: Catalyst not activating**
- Cause: Mobs dying outside 8-block radius
- Fix: Adjust killing chamber positioning
- Verify: Use debug stick to show radius

**Issue: No sculk generating**
- Cause: Mobs not dropping XP (e.g., player kills)
- Fix: Use natural death methods (fall, lava)
- Verify: Check catalyst has "bloom" animation

**Issue: Warden spawning frequently**
- Cause: Sculk Shriekers activated too often
- Fix: Destroy all shriekers in area
- Prevent: Sneak at all times

**Issue: Low XP yield**
- Cause: Low-value mobs or inefficient killing
- Fix: Target high-XP mobs (Enderman, Blaze)
- Optimize: Increase spawn rate

**Issue: Lag during farming**
- Cause: Too many entities or block updates
- Fix: Use redstone clocks to limit spawn rate
- Optimize: Use chunk loading strategically

---

## References and Sources

### Official Minecraft Documentation
- [Minecraft Wiki - Sculk Catalyst](https://minecraft.fandom.com/wiki/Sculk_Catalyst)
- [Minecraft Wiki - Sculk](https://minecraft.fandom.com/wiki/Sculk)
- [Minecraft Wiki - Warden](https://minecraft.fandom.com/wiki/Warden)
- [Minecraft Snapshot 22w11a](https://www.minecraft.net/en-us/article/minecraft-snapshot-22w11a)

### Community Resources
- [Beebom - Minecraft Sculk Guide](https://beebom.com/minecraft-sculk-blocks/)
- [MCMOD.cn - Sculk Catalyst (Chinese)](http://www.mcmod.cn/item/598207.html)
- [TheGamer - Warden Mechanics Guide](https://www.thegamer.com/minecraft-warden-mechanics-guide-tips/)

### Forge Modding References
- [SeedFinding - MC Biome Java Library](https://github.com/SeedFinding/mc_biome_java)
- [CSDN - Forge 1.20.1 Biome Tutorial](https://m.blog.csdn.net/qq_24992377/article/details/146298791)
- [Microsoft - BiomeTypes Script API](https://learn.microsoft.com/zh-cn/minecraft/creator/scriptapi/microsoft/server/biometypes?view=minecraft-bedrock-experimental)

### Farm Design Inspiration
- [Avomance - No Spawner Sculk XP Farm](https://avomance.com/minecraft-world-downloads/)
- [Game Rant - Every Sculk Block Explained](https://gamerant.com/minecraft-every-sculk-block-what-they-do/)

---

**Document Version:** 1.0
**Last Modified:** 2025-01-10
**Status:** Ready for Implementation
