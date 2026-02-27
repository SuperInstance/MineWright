# Iron Golem Farming for MineWright (Minecraft Forge 1.20.1)

## Executive Summary

This document provides a comprehensive design for implementing **automated iron golem farming** in the MineWright Minecraft mod. Iron golem farms leverage Minecraft's village mechanics to automatically spawn and harvest iron golems for a renewable source of iron ingots.

**Key Features:**
- Village-based spawning using vanilla Minecraft mechanics
- Multi-village coordination for maximum efficiency
- Automated collection and sorting systems
- Integration with MineWright's existing action system
- Scalable designs from single-village to industrial-scale farms

---

## Table of Contents

1. [Village Mechanics Overview](#1-village-mechanics-overview)
2. [Village Setup Patterns](#2-village-setup-patterns)
3. [Kill Mechanism Designs](#3-kill-mechanism-designs)
4. [Collection Systems](#4-collection-systems)
5. [Multi-Village Coordination](#5-multi-village-coordination)
6. [Efficiency Optimization](#6-efficiency-optimization)
7. [Code Integration](#7-code-integration)
8. [Implementation Roadmap](#8-implementation-roadmap)

---

## 1. Village Mechanics Overview

### 1.1 Java Edition Iron Golem Spawning (1.20.1)

Iron golems spawn naturally when villagers meet specific conditions:

| Requirement | Value |
|-------------|-------|
| **Minimum Villagers** | 10 villagers |
| **Minimum Beds** | 20 beds (2 per villager) |
| **Bed Linking** | Each villager must be linked to a bed |
| **Work Requirement** | 75% of villagers must have worked in the past day |
| **Sleep Requirement** | Villagers must have slept in the last 20 minutes |
| **Golem Detection** | Villagers check for golems within 16 blocks every 5 seconds |
| **Spawn Volume** | 16×13×16 blocks around gossiping villager |
| **Spawn Cap** | 1 golem per 10 villagers (rounded down) |
| **Spawn Attempt Rate** | Every ~35 seconds (600 ticks) when conditions met |

### 1.2 Spawning Process

1. **Gossip Phase**: Villagers periodically spread "golem spawning intention" gossip
2. **Aggregation**: When 5 villagers with golem intention are within 10 blocks
3. **Spawn Check**: System checks for valid spawn location (2×4×2 space with solid block below)
4. **Cooldown**: After successful spawn, 30-second cooldown for villagers within 16 blocks
5. **Cap Check**: If golem cap reached, no further spawns until golem leaves or dies

### 1.3 Village Center Detection

The village center is determined by the **last claimed bed** (not last placed). This is critical for spawn platform positioning:

```java
// Village center is the centroid of all claimed beds
// Spawn platform must be within 6 blocks vertically of center
```

---

## 2. Village Setup Patterns

### 2.1 Single-Village Design (Basic)

**Layout:**
```
Top View (Level 1 - Villager Housing):
+---------------------+
| B = Bed             |
| W = Workstation     |
| V = Villager        |
| . = Air             |
+---------------------+
  B W B W B W B W
  W . . . . . . . W
  B W B W B W B W
  W . . . . . . . W
  B W B W B W B W
  W . . . . . . . W
  B W B W B W B W
  W . . . . . . . W
  B W B W B W B W
+---------------------+

Side View:
Level 5: Kill chamber (lava/magma block)
Level 4: Air (golem spawn zone)
Level 3: Villager housing
Level 2: Collection system (hoppers/minecart)
Level 1: Base platform
```

**Specifications:**
- Villagers: 10-20 (1-2 golem cap)
- Beds: 20-40 (2 per villager)
- Spawn Platform: 8×8 blocks, 4 blocks above villager level
- Villager Height: Y+3 from base
- Spawn Height: Y+7 from base

### 2.2 Compact Single-Village Design

**Space-Efficient Layout (9×9 footprint):**

```java
/**
 * Generates a compact iron golem farm structure.
 * Dimensions: 9×9 base, 8 blocks tall
 * Villagers: 10
 * Beds: 20
 * Spawn platform: 7×7
 */
public static List<BlockPlacement> buildCompactIronFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Level 0: Foundation (solid base)
    for (int x = 0; x < 9; x++) {
        for (int z = 0; z < 9; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.STONE_BRICKS));
        }
    }

    // Level 1: Collection system (hoppers)
    for (int x = 1; x < 8; x++) {
        for (int z = 1; z < 8; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.HOPPER));
        }
    }

    // Level 2: Villager platform walls
    for (int x = 0; x < 9; x++) {
        for (int z = 0; z < 9; z++) {
            if (x == 0 || x == 8 || z == 0 || z == 8) {
                blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.STONE_BRICKS));
            }
        }
    }

    // Level 2-3: Villager housing with beds and workstations
    // Place 10 beds and 10 workstations in alternating pattern
    int bedIndex = 0;
    int workstationIndex = 0;
    for (int x = 1; x < 8; x += 2) {
        for (int z = 1; z < 8; z += 2) {
            if (bedIndex < 10) {
                // Bed (occupies 2 blocks)
                blocks.add(new BlockPlacement(start.offset(x, 3, z), Blocks.WHITE_BED));
                blocks.add(new BlockPlacement(start.offset(x + 1, 3, z), Blocks.WHITE_BED));
                bedIndex += 2;
            }
            if (workstationIndex < 10) {
                // Workstation (composter for simplicity)
                blocks.add(new BlockPlacement(start.offset(x, 3, z + 1), Blocks.COMPOSTER));
                workstationIndex++;
            }
        }
    }

    // Level 4: Spawn platform (solid blocks)
    for (int x = 1; x < 8; x++) {
        for (int z = 1; z < 8; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 4, z), Blocks.SMOOTH_STONE));
        }
    }

    // Level 5-6: Spawn chamber (air - golems spawn here)
    // Level 7: Kill chamber (lava or magma block)
    for (int x = 2; x < 7; x++) {
        for (int z = 2; z < 7; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 7, z), Blocks.MAGMA_BLOCK));
        }
    }

    // Level 8: Water collection (pushes drops to center)
    for (int x = 1; x < 8; x++) {
        for (int z = 1; z < 8; z++) {
            if (x == 1 || x == 7 || z == 1 || z == 7) {
                blocks.add(new BlockPlacement(start.offset(x, 8, z), Blocks.WATER));
            }
        }
    }

    return blocks;
}
```

### 2.3 Multi-Village Stacking Design

**Principle:** Multiple independent villages stacked vertically or horizontally, each with its own spawn platform.

**Vertical Stack (3 villages):**

```
Side View:
+----------------+
| Village 3      | Y+20 to Y+28
| Spawn: Y+24    |
+----------------+
| Buffer (16 bl) | Y+12 to Y+19 (prevents cross-village interference)
+----------------+
| Village 2      | Y+4 to Y+12
| Spawn: Y+8     |
+----------------+
| Buffer (16 bl) | Y-8 to Y+3
+----------------+
| Village 1      | Y-16 to Y-8
| Spawn: Y-12    |
+----------------+
```

**Key Points:**
- Villages must be separated by at least 66 blocks (center-to-center)
- Each village needs independent villager housing
- Spawn platforms must be >16 blocks apart vertically
- Collection systems can be combined

---

## 3. Kill Mechanism Designs

### 3.1 Lava Blade (Classic Design)

**Concept:** Golems are pushed into a lava blade, instantly killing them and dropping iron.

```java
/**
 * Creates a lava blade kill mechanism.
 * Golems are pushed by water into flowing lava.
 */
public static List<BlockPlacement> createLavaBlade(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Channel: 1 wide, 3 long
    int centerX = 4;
    int centerZ = 4;

    // Base layer (solid)
    for (int x = centerX - 1; x <= centerX + 1; x++) {
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));
        }
    }

    // Walls (glass for visibility)
    for (int y = 1; y <= 3; y++) {
        blocks.add(new BlockPlacement(start.offset(centerX - 1, y, centerZ), Blocks.GLASS));
        blocks.add(new BlockPlacement(start.offset(centerX + 1, y, centerZ), Blocks.GLASS));
    }

    // Water source (pushes golems toward lava)
    blocks.add(new BlockPlacement(start.offset(centerX, 1, centerZ - 1), Blocks.WATER));

    // Lava source (kill blade)
    blocks.add(new BlockPlacement(start.offset(centerX, 1, centerZ), Blocks.LAVA));

    // Collection point (hopper under lava)
    blocks.add(new BlockPlacement(start.offset(centerX, 0, centerZ), Blocks.HOPPER));

    return blocks;
}
```

**Pros:**
- Instant kills
- Compact design
- High efficiency

**Cons:**
- Golems may take damage before dying (pop mechanism)
- Requires careful water flow setup

### 3.2 Magma Block + Suffocation (Modern Design)

**Concept:** Golems stand on magma blocks for damage, finished by suffocation or player intervention.

```java
/**
 * Creates a magma block kill mechanism.
 * Magma blocks deal 2 damage/second (1 damage/10 ticks).
 * Golems die in ~50 seconds.
 */
public static List<BlockPlacement> createMagmaKillChamber(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // 3×3 magma platform
    for (int x = -1; x <= 1; x++) {
        for (int z = -1; z <= 1; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.MAGMA_BLOCK));
        }
    }

    // Glass walls (contain golems, allow visibility)
    for (int y = 1; y <= 3; y++) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                    blocks.add(new BlockPlacement(start.offset(x, y, z), Blocks.GLASS));
                }
            }
        }
    }

    // Trapdoor floor (optional - for manual collection)
    blocks.add(new BlockPlacement(start.offset(0, 0, 0), Blocks.IRON_TRAPDOOR));

    return blocks;
}
```

**Pros:**
- No water flow issues
- Can collect XP manually
- Golem stays intact (no "pop" animation issues)

**Cons:**
- Slower kill time
- Requires manual intervention for XP

### 3.3 Fall Damage + Player Kill

**Concept:** Golems fall from spawn platform to near-death, then finished by player.

```java
/**
 * Creates a fall damage kill mechanism.
 * Fall distance: 23 blocks (leaves golem at 1 HP)
 * Player can then kill with one hit for XP.
 */
public static List<BlockPlacement> createFallKillChamber(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Spawn platform (solid)
    for (int x = -3; x <= 3; x++) {
        for (int z = -3; z <= 3; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));
        }
    }

    // Water elevator (pushes golems off edge)
    for (int x = -4; x <= 4; x++) {
        for (int z = -4; z <= 4; z++) {
            if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));
            }
        }
    }

    // Drop shaft (23 blocks deep)
    for (int y = -1; y >= -23; y--) {
        blocks.add(new BlockPlacement(start.offset(0, y, 0), Blocks.AIR));
    }

    // Landing platform (hay bale - reduces fall damage slightly)
    for (int x = -1; x <= 1; x++) {
        for (int z = -1; z <= 1; z++) {
            blocks.add(new BlockPlacement(start.offset(x, -23, z), Blocks.HAY_BLOCK));
        }
    }

    // Collection hopper
    blocks.add(new BlockPlacement(start.offset(0, -24, 0), Blocks.HOPPER));

    return blocks;
}
```

**Pros:**
- Player collects XP
- No lava needed
- Can be automated with dispensers

**Cons:**
- Requires player presence for optimal XP
- Taller structure

---

## 4. Collection Systems

### 4.1 Hopper-Based Collection

**Simple Hopper Grid:**

```java
/**
 * Creates a hopper-based collection system.
 * Iron drops flow into hopper minecarts or chests.
 */
public static List<BlockPlacement> createHopperCollection(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // 3×3 hopper grid
    for (int x = -1; x <= 1; x++) {
        for (int z = -1; z <= 1; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.HOPPER));
        }
    }

    // Central chest (receiving from all hoppers)
    blocks.add(new BlockPlacement(start.offset(0, -1, 0), Blocks.CHEST));

    // Optional: Hopper minecart for high-throughput
    // Rails underneath hoppers for minecart collection

    return blocks;
}
```

### 4.2 Minecart Collection System

**High-Volume Design:**

```java
/**
 * Creates a minecart-based collection system.
 * Hopper minecarts collect drops and move them to storage.
 */
public static List<BlockPlacement> createMinecartCollection(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Rail line
    for (int z = 0; z < 20; z++) {
        blocks.add(new BlockPlacement(start.offset(0, 0, z), Blocks.POWERED_RAIL));
    }

    // Storage chests at end
    blocks.add(new BlockPlacement(start.offset(0, 0, 20), Blocks.CHEST));
    blocks.add(new BlockPlacement(start.offset(1, 0, 20), Blocks.CHEST));
    blocks.add(new BlockPlacement(start.offset(-1, 0, 20), Blocks.CHEST));

    // Hopper minecart (spawned separately)
    // EntityMinecartContainer with hopper minecart type

    return blocks;
}
```

### 4.3 Water Stream Collection

**Passive Collection:**

```java
/**
 * Creates a water stream collection system.
 * Water flows carrying drops to collection point.
 */
public static List<BlockPlacement> createWaterCollection(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Create water streams from all corners to center
    for (int x = -10; x <= 10; x++) {
        for (int z = -10; z <= 10; z++) {
            // Base
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));

            // Walls
            if (Math.abs(x) == 10 || Math.abs(z) == 10) {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.STONE_BRICKS));
            }

            // Water (creates flow toward center)
            if (x > 0 && z > 0) {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));
            }
        }
    }

    // Central collection hopper
    blocks.add(new BlockPlacement(start.offset(0, 1, 0), Blocks.HOPPER));
    blocks.add(new BlockPlacement(start.offset(0, 0, 0), Blocks.CHEST));

    return blocks;
}
```

---

## 5. Multi-Village Coordination

### 5.1 Village Separation Requirements

For independent villages, ensure proper spacing:

```java
/**
 * Calculates minimum separation for independent villages.
 * Village centers must be >66 blocks apart to prevent merging.
 */
public class VillageSeparationCalculator {

    private static final int VILLAGE_RADIUS = 32;
    private static final int MIN_SEPARATION = 66;
    private static final int GOLEM_DETECTION_RANGE = 16;

    /**
     * Checks if two village centers are far enough apart.
     */
    public static boolean areVillagesIndependent(BlockPos center1, BlockPos center2) {
        double distance = Math.sqrt(
            Math.pow(center2.getX() - center1.getX(), 2) +
            Math.pow(center2.getZ() - center1.getZ(), 2)
        );
        return distance >= MIN_SEPARATION;
    }

    /**
     * Finds optimal positions for multiple stacked villages.
     */
    public static List<BlockPos> calculateStackedVillagePositions(BlockPos baseCenter, int count) {
        List<BlockPos> positions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int yOffset = i * MIN_SEPARATION;
            positions.add(baseCenter.offset(0, yOffset, 0));
        }

        return positions;
    }
}
```

### 5.2 Multi-Village Manager

```java
package com.minewright.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages multiple iron golem farms as independent villages.
 * Ensures proper separation and coordinates spawning.
 */
public class MultiVillageManager {

    private final List<IronGolemFarm> farms;
    private final Level level;

    public MultiVillageManager(Level level) {
        this.level = level;
        this.farms = new ArrayList<>();
    }

    /**
     * Adds a new farm village.
     * Validates separation from existing farms.
     */
    public boolean addFarm(IronGolemFarm farm) {
        // Check separation
        for (IronGolemFarm existing : farms) {
            if (!VillageSeparationCalculator.areVillagesIndependent(
                farm.getVillageCenter(),
                existing.getVillageCenter()
            )) {
                return false;
            }
        }

        farms.add(farm);
        return true;
    }

    /**
     * Gets total iron production rate (ingots per hour).
     */
    public double getTotalIronRate() {
        return farms.stream()
            .mapToDouble(IronGolemFarm::getIronRate)
            .sum();
    }

    /**
     * Cycles through farms to balance villager work schedules.
     */
    public void balanceVillagerWork() {
        // Implement work cycle to maintain 75% work requirement
    }
}
```

### 5.3 Horizontal Multi-Village Layout

**Ground-Level Spreading:**

```
Top View (3 villages in triangle formation):
       V1
      /|\
     / | \
  70b  |  70b
     \ | /
      \|/
       V2 ----- 70b ----- V3

V1, V2, V3 = Village centers
70b = Minimum separation (66 blocks + buffer)
```

**Code for Layout Calculation:**

```java
/**
 * Generates positions for triangular village layout.
 */
public static List<BlockPos> generateTriangularLayout(BlockPos center, int separation) {
    List<BlockPos> positions = new ArrayList<>();

    // Village 1: Top of triangle
    positions.add(center.offset(0, 0, -separation));

    // Village 2: Bottom left
    double angle60 = Math.toRadians(60);
    int x2 = (int) Math.round(separation * Math.cos(angle60));
    int z2 = (int) Math.round(separation * Math.sin(angle60));
    positions.add(center.offset(-x2, 0, z2));

    // Village 3: Bottom right
    positions.add(center.offset(x2, 0, z2));

    return positions;
}
```

---

## 6. Efficiency Optimization

### 6.1 Spawn Rate Optimization

**Key Factors:**

1. **Villager Count**: 10-20 villagers per village (optimal balance)
2. **Sleep Cycle**: Ensure all villagers sleep within 20 minutes
3. **Work Station Access**: 75% must work daily
4. **Golem Cap Management**: Remove golems quickly to reset spawn timer

**Optimization Code:**

```java
/**
 * Monitors and optimizes iron golem spawn rates.
 */
public class SpawnRateOptimizer {

    private static final int OPTIMAL_VILLAGER_COUNT = 16;
    private static final int GOLEM_SPAWN_COOLDOWN_TICKS = 600; // 30 seconds
    private static final int GOLEM_DETECTION_RANGE = 16;

    /**
     * Checks if farm conditions are optimal for spawning.
     */
    public boolean isOptimal(IronGolemFarm farm) {
        // Check villager count
        if (farm.getVillagerCount() < OPTIMAL_VILLAGER_COUNT) {
            return false;
        }

        // Check bed links
        if (farm.getUnlinkedBedCount() > 0) {
            return false;
        }

        // Check golem presence (resets spawn timer)
        if (farm.getGolemInRange() != null) {
            return false;
        }

        return true;
    }

    /**
     * Calculates expected spawn rate (golems per hour).
     */
    public double calculateSpawnRate(IronGolemFarm farm) {
        double baseRate = 3600.0 / 35.0; // ~103 spawns/hour per village
        int golemCap = farm.getVillagerCount() / 10;

        // Adjust for actual conditions
        double efficiency = calculateEfficiency(farm);

        return baseRate * golemCap * efficiency;
    }

    private double calculateEfficiency(IronGolemFarm farm) {
        double efficiency = 1.0;

        // Reduce efficiency if villagers haven't slept
        if (farm.getTimeSinceLastSleep() > 12000) { // 10 minutes
            efficiency *= 0.5;
        }

        // Reduce efficiency if not enough villagers worked
        double workPercentage = farm.getWorkPercentage();
        if (workPercentage < 0.75) {
            efficiency *= (workPercentage / 0.75);
        }

        return efficiency;
    }
}
```

### 6.2 Production Rate Calculations

**Theoretical Maximum:**

| Configuration | Villagers | Villages | Golem Cap | Iron/Hour |
|--------------|-----------|----------|-----------|-----------|
| Basic | 10 | 1 | 1 | ~320 |
| Optimized | 16 | 1 | 1 | ~320 |
| Double | 16 | 2 | 2 | ~640 |
| Triple Stack | 16 | 3 | 3 | ~960 |
| Industrial | 16 | 6 | 6 | ~1,920 |

**Note:** Each iron golem drops 3-5 iron ingots (average 4). With ~35 second spawn rate, theoretical maximum is ~320 ingots/hour per golem cap.

### 6.3 Efficiency Tips

1. **Spawn Chunks**: Build in spawn chunks for 24/7 operation
2. **Nether Portal Reset**: Use nether portal to instantly teleport golems away (resets spawn timer)
3. **Immediate Kill**: Kill golems instantly upon spawning (lava blade)
4. **Villager Curing**: Cured zombie villagers have higher work tendency
5. **Work Station Optimization**: Use lecterns for easy villager workstation access

---

## 7. Code Integration

### 7.1 Iron Golem Farm Structure Generator

Add to `StructureGenerators.java`:

```java
/**
 * Generates an iron golem farm structure.
 *
 * @param start Base position
 * @param villageCount Number of stacked villages (1-3 recommended)
 * @return List of block placements for the farm
 */
public static List<BlockPlacement> buildIronGolemFarm(BlockPos start, int villageCount) {
    List<BlockPlacement> blocks = new ArrayList<>();

    for (int i = 0; i < villageCount; i++) {
        int yOffset = i * 66; // 66 block separation
        BlockPos villageStart = start.offset(0, yOffset, 0);
        blocks.addAll(buildSingleVillageFarm(villageStart));
    }

    // Add central collection system
    blocks.addAll(buildCentralCollection(start));

    return blocks;
}

/**
 * Builds a single village iron farm unit.
 */
private static List<BlockPlacement> buildSingleVillageFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Level 0: Foundation
    for (int x = 0; x < 11; x++) {
        for (int z = 0; z < 11; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.STONE_BRICKS));
        }
    }

    // Level 1: Villager housing
    buildVillagerHousing(blocks, start.offset(0, 1, 0));

    // Level 5: Spawn platform
    buildSpawnPlatform(blocks, start.offset(0, 5, 0));

    // Level 8: Kill chamber
    buildKillChamber(blocks, start.offset(0, 8, 0));

    return blocks;
}

private static void buildVillagerHousing(List<BlockPlacement> blocks, BlockPos start) {
    // Build walls
    for (int y = 0; y < 4; y++) {
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                if (x == 0 || x == 10 || z == 0 || z == 10) {
                    blocks.add(new BlockPlacement(start.offset(x, y, z), Blocks.STONE_BRICKS));
                }
            }
        }
    }

    // Place beds and workstations
    int villagerCount = 16;
    for (int i = 0; i < villagerCount; i++) {
        int x = 1 + (i % 4) * 2;
        int z = 1 + (i / 4) * 2;

        // Bed
        blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.WHITE_BED));
        blocks.add(new BlockPlacement(start.offset(x + 1, 0, z), Blocks.WHITE_BED));

        // Workstation (composter)
        blocks.add(new BlockPlacement(start.offset(x, 0, z + 1), Blocks.COMPOSTER));
    }
}

private static void buildSpawnPlatform(List<BlockPlacement> blocks, BlockPos start) {
    // Solid spawn platform
    for (int x = 1; x < 10; x++) {
        for (int z = 1; z < 10; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));
        }
    }

    // Walls to contain golems
    for (int y = 1; y < 3; y++) {
        for (int x = 1; x < 10; x++) {
            blocks.add(new BlockPlacement(start.offset(x, y, 1), Blocks.GLASS));
            blocks.add(new BlockPlacement(start.offset(x, y, 9), Blocks.GLASS));
        }
        for (int z = 1; z < 10; z++) {
            blocks.add(new BlockPlacement(start.offset(1, y, z), Blocks.GLASS));
            blocks.add(new BlockPlacement(start.offset(9, y, z), Blocks.GLASS));
        }
    }
}

private static void buildKillChamber(List<BlockPlacement> blocks, BlockPos start) {
    // Magma block floor
    for (int x = 4; x < 7; x++) {
        for (int z = 4; z < 7; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.MAGMA_BLOCK));
        }
    }

    // Collection hopper under center
    blocks.add(new BlockPlacement(start.offset(5, -1, 5), Blocks.HOPPER));

    // Chest below hopper
    blocks.add(new BlockPlacement(start.offset(5, -2, 5), Blocks.CHEST));
}

private static List<BlockPlacement> buildCentralCollection(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Vertical collection shaft
    for (int y = -30; y < 0; y++) {
        blocks.add(new BlockPlacement(start.offset(5, y, 5), Blocks.HOPPER));
    }

    // Storage room at bottom
    for (int x = 3; x < 8; x++) {
        for (int z = 3; z < 8; z++) {
            blocks.add(new BlockPlacement(start.offset(x, -31, z), Blocks.CHEST));
        }
    }

    return blocks;
}
```

### 7.2 Build Iron Farm Action

Create new action: `BuildIronFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.StructureGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Action for building an iron golem farm.
 * Automatically generates and places the farm structure.
 */
public class BuildIronFarmAction extends BaseAction {

    private final int villageCount;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;
    private static final int BLOCKS_PER_TICK = 4;

    public BuildIronFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.villageCount = task.getIntParameter("villages", 1);
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Building iron golem farm with " + villageCount + " village(es)...");

        // Find suitable location
        BlockPos buildPos = findBuildLocation();
        if (buildPos == null) {
            result = ActionResult.failure("Could not find suitable location for iron farm");
            return;
        }

        // Generate build plan
        buildPlan = StructureGenerators.buildIronGolemFarm(buildPos, villageCount);

        if (buildPlan.isEmpty()) {
            result = ActionResult.failure("Failed to generate iron farm structure");
            return;
        }

        foreman.sendChatMessage("Iron farm will require " + buildPlan.size() + " blocks to build.");
        foreman.setFlying(true);
    }

    @Override
    protected void onTick() {
        if (buildPlan == null) return;

        int blocksThisTick = 0;
        while (currentBlockIndex < buildPlan.size() && blocksThisTick < BLOCKS_PER_TICK) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);

            // Place block
            foreman.level().setBlock(
                placement.pos(),
                placement.block().defaultBlockState(),
                3
            );

            currentBlockIndex++;
            blocksThisTick++;

            // Teleport if far away
            double distance = Math.sqrt(foreman.blockPosition().distSqr(placement.pos()));
            if (distance > 8) {
                foreman.teleportTo(
                    placement.pos().getX() + 0.5,
                    placement.pos().getY(),
                    placement.pos().getZ() + 0.5
                );
            }
        }

        // Report progress
        if (currentBlockIndex % 100 == 0) {
            int percentComplete = (currentBlockIndex * 100) / buildPlan.size();
            foreman.sendChatMessage("Iron farm progress: " + percentComplete + "%");
        }

        // Check completion
        if (currentBlockIndex >= buildPlan.size()) {
            foreman.setFlying(false);
            result = ActionResult.success("Iron golem farm completed! " +
                "Expected production: ~" + (villageCount * 320) + " iron/hour");
        }
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build iron golem farm (" + currentBlockIndex + "/" +
            (buildPlan != null ? buildPlan.size() : 0) + " blocks)";
    }

    private BlockPos findBuildLocation() {
        // Find flat area near foreman
        BlockPos startPos = foreman.blockPosition();

        // Check 50 blocks in each direction
        for (int radius = 5; radius < 50; radius += 5) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = startPos.getX() + (int) (Math.cos(radians) * radius);
                int z = startPos.getZ() + (int) (Math.sin(radians) * radius);
                BlockPos checkPos = new BlockPos(x, startPos.getY(), z);

                if (isAreaSuitable(checkPos)) {
                    return checkPos;
                }
            }
        }

        return null;
    }

    private boolean isAreaSuitable(BlockPos pos) {
        // Check 15x15 area for flat terrain
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                if (!foreman.level().getBlockState(checkPos.below()).isSolid()) {
                    return false;
                }
                if (!foreman.level().getBlockState(checkPos).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }
}
```

### 7.3 Register the Action

Update `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Farming
    registry.register("build_iron_farm",
        (foreman, task, ctx) -> new BuildIronFarmAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### 7.4 Update Prompt Builder

Add to `PromptBuilder.java` to inform the LLM about the new action:

```java
private String getActionDescriptions() {
    return """
        ...

        Farming Actions:
        - build_iron_farm: Build an automated iron golem farm
          * Parameters: villages (integer, 1-3, default: 1)
          * Builds: Complete iron farm with villager housing, spawn platform, kill chamber
          * Production: ~320 iron ingots/hour per village
          * Usage: "build an iron farm", "create a double village iron farm"

        ...
        """;
}
```

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1)

**Tasks:**
1. Create `IronGolemFarm` class to track village state
2. Implement village center detection logic
3. Add villager spawning and bed linking utilities
4. Test basic village spawning mechanics

**Deliverables:**
- `IronGolemFarm.java`
- `VillageUtils.java`
- Unit tests for village detection

### Phase 2: Structure Generation (Week 2)

**Tasks:**
1. Implement `buildIronGolemFarm()` in `StructureGenerators`
2. Add compact and stacked variants
3. Implement kill mechanism designs
4. Add collection system variants

**Deliverables:**
- Complete structure generation code
- Visual testing of generated structures
- Performance benchmarks

### Phase 3: Action Integration (Week 3)

**Tasks:**
1. Create `BuildIronFarmAction`
2. Register in `CoreActionsPlugin`
3. Update `PromptBuilder`
4. Test LLM integration

**Deliverables:**
- Working action accessible via natural language
- Integration tests
- Documentation

### Phase 4: Multi-Village Coordination (Week 4)

**Tasks:**
1. Implement `MultiVillageManager`
2. Add village separation calculation
3. Implement spawn rate optimization
4. Add monitoring and reporting

**Deliverables:**
- Multi-village support
- Efficiency monitoring tools
- Production rate calculator

### Phase 5: Optimization & Polish (Week 5)

**Tasks:**
1. Performance profiling
2. Memory usage optimization
3. Add configuration options
4. Create documentation and tutorials

**Deliverables:**
- Optimized codebase
- User documentation
- Tutorial videos/guides

---

## Appendix A: Configuration

Add to `config/steve-common.toml`:

```toml
[iron_farming]
# Maximum number of villages per farm
max_villages = 6

# Default village count when building iron farm
default_villages = 1

# Enable auto-cleaning of excess iron golems
auto_clean = true

# Iron collection method: "hopper", "minecart", "water"
collection_method = "hopper"

# Kill mechanism: "lava", "magma", "fall"
kill_mechanism = "magma"

# Enable spawn chunk optimization
spawn_chunks = true
```

---

## Appendix B: Troubleshooting

### Issue: Golems Not Spawning

**Possible Causes:**
1. Villagers haven't slept in 20 minutes
2. Golem already within 16 blocks
3. Spawn platform not within valid range
4. Not enough villagers with workstations

**Solutions:**
- Place beds and wait for night cycle
- Push existing golem away with boat
- Verify spawn platform height (Y+4 to Y+6 from villager level)
- Add workstations (composters, lecterns, etc.)

### Issue: Low Production Rate

**Possible Causes:**
1. Villagers not meeting work requirement
2. Golem cap reached
3. Villages merging (too close together)
4. Spawn timer not resetting

**Solutions:**
- Ensure 75% of villagers access workstations daily
- Kill golems faster (lava blade)
- Separate village centers by 66+ blocks
- Use nether portal to instantly remove golems

### Issue: Iron Not Collecting

**Possible Causes:**
1. Hoppers not properly aligned
2. Chests full
3. Water flow incorrect
4. Minecart not moving

**Solutions:**
- Verify hopper connections (use debug stick)
- Add more chest storage
- Check water source blocks
- Power rails with redstone

---

## References

### Sources

- [Minecraft Wiki - Iron Golem](https://minecraft-archive.fandom.com/wiki/Iron_Golem)
- [Minecraft Wiki - Iron Golem Farming Tutorials](https://minecraft.fandom.com/wiki/Tutorials/Iron_golem_farming)
- [Minecraft Wiki - Door-based Iron Golem Farming](https://minecraft.fandom.com/wiki/Tutorials/Door-based_iron_golem_farming)
- [MCMOD - Iron Golem Item Details](https://www.mcmod.cn/item/65598.html)
- [1.20 Iron Farm Tutorial (Bilibili)](http://www.bilibili.com/video/BV1JN4y167DV)

### Minecraft 1.20.1 Forge API

- `net.minecraft.world.entity.animal.IronGolem`
- `net.minecraft.world.entity.npc.Villager`
- `net.minecraft.world.level.block.entity.BedBlockEntity`
- `net.minecraft.world.entity.ai.village.VillageSiege`

### Related MineWright Code

- `C:\Users\casey\steve\src\main\java\com\minewright\structure\StructureGenerators.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildStructureAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\plugin\CoreActionsPlugin.java`

---

## Summary

This design provides a complete implementation plan for iron golem farming in MineWright, including:

1. **Village Setup**: Single and multi-village patterns with proper spacing
2. **Kill Mechanisms**: Lava blade, magma block, and fall damage designs
3. **Collection Systems**: Hopper, minecart, and water stream variants
4. **Multi-Village Coordination**: Independent village management and optimization
5. **Code Integration**: Complete implementation with structure generators and actions
6. **Efficiency Optimization**: Spawn rate calculations and production targets

The expected production rate is **~320 iron ingots per hour per village**, with scalable designs supporting up to 6+ villages for industrial-scale iron farming (1,900+ ingots/hour).

---

*Document Version: 1.0*
*Last Updated: 2026-02-27*
*Author: Orchestrator (MineWright AI Team)*
