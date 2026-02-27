# Snow and Ice Farming for MineWright (Minecraft Forge 1.20.1)

## Executive Summary

This document provides a comprehensive design for implementing **automated snow and ice farming** in the MineWright Minecraft mod. Snow and ice farms leverage vanilla Minecraft mechanics to produce renewable snow blocks, ice, packed ice, and blue ice for building and transportation purposes.

**Key Features:**
- Snow golem-based snow layer generation
- Ice formation through biome temperature mechanics
- Automated collection and sorting systems
- Multi-biome support with climate detection
- Packed ice and blue ice production automation
- Integration with MineWright's existing action system

---

## Table of Contents

1. [Snow Golem Mechanics](#1-snow-golem-mechanics)
2. [Ice Formation Mechanics](#2-ice-formation-mechanics)
3. [Snow Farm Designs](#3-snow-farm-designs)
4. [Ice Farm Designs](#4-ice-farm-designs)
5. [Packed Ice Production](#5-packed-ice-production)
6. [Climate Control](#6-climate-control)
7. [Code Integration](#7-code-integration)
8. [Implementation Roadmap](#8-implementation-roadmap)

---

## 1. Snow Golem Mechanics

### 1.1 Snow Golem Behavior (Java Edition 1.20.1)

| Attribute | Value |
|-----------|-------|
| **Health** | 4 hearts (8 HP) |
| **Snow Trail** | Generates snow layers in appropriate biomes |
| **Attack** | Throws snowballs (0 damage, knockback to Blaze: 3 damage) |
| **Creation** | 2 snow blocks + 1 carved pumpkin/jack o' lantern |

### 1.2 Snow Trail Generation

Snow golems leave a trail of single-layer snow blocks as they move, but with important biome restrictions:

**Biomes Where Snow Trails Work (Java Edition):**
| Biome Category | Examples |
|----------------|----------|
| Snowy | Snowy Plains, Ice Spikes, Snowy Taiga, Snowy Slopes, Frozen River |
| Cold | Cold Taiga, Mountain Grove, Taiga |
| Temperate | Plains (some variants), Forest |

**Biomes Where Snow Trails FAIL:**
| Biome Type | Examples |
|------------|----------|
| Hot/Dry | Desert, Savanna, Badlands, Nether, Desert variants |
| Jungle | Jungle, Bamboo Jungle |
| Swamp | Swampland |

### 1.3 Snow Layer Properties

| Property | Value |
|----------|-------|
| **Block ID** | `minecraft:snow` |
| **Layers** | 1-8 layers possible (snow golem creates layer 1) |
| **Harvest** | Shovel yields 1-4 snowballs depending on layer thickness |
| **Melting** | Light level > 11 causes melting |
| **Physics** | Subject to gravity (falls if unsupported below layer 8) |

### 1.4 Snow Golem Spawning

**Creation Pattern:**
```
[Y] Carved Pumpkin / Jack o' Lantern (placed LAST)
[Y] Snow Block
[Y] Snow Block
```

**Programmatic Spawning:**

```java
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Spawns a snow golem at the specified position.
 */
public void spawnSnowGolem(Level level, BlockPos pos) {
    if (!level.isClientSide) {
        SnowGolem snowGolem = EntityType.SNOW_GOLEM.create(level);
        if (snowGolem != null) {
            snowGolem.moveTo(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                0.0F, 0.0F
            );
            level.addFreshEntity(snowGolem);
        }
    }
}

/**
 * Creates a snow golem from blocks (like a player would).
 */
public void createSnowGolemFromBlocks(Level level, BlockPos pos) {
    // Place two snow blocks
    level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
    level.setBlock(pos.above(), Blocks.SNOW_BLOCK.defaultBlockState(), 3);

    // Place pumpkin on top (triggers golem creation)
    level.setBlock(pos.above(2), Blocks.CARVED_PUMPKIN.defaultBlockState(), 3);
}
```

---

## 2. Ice Formation Mechanics

### 2.1 Water Freezing Requirements

For water to freeze into ice, the following conditions must be met:

| Condition | Requirement |
|-----------|-------------|
| **Biome Temperature** | Must be **< 0.15** |
| **Sky Access** | Water must be **directly exposed to sky** (no blocks above) |
| **Light Level** | Adjacent block brightness must be **<= 12** |
| **Adjacent Block** | Must have at least **one non-water block** horizontally adjacent |
| **Height Factor** | Temperature decreases by 1/600 per block above Y=63 |

### 2.2 Biome Temperature Scale

**Freezing Biomes (Temperature <= 0.15):**

| Biome | Temperature | Notes |
|-------|-------------|-------|
| Snow Capped Peaks | -0.7 | Always freezes |
| Lofty Peaks | -0.7 | Always freezes |
| Ice Spikes | 0.0 | Always freezes |
| Frozen River | 0.0 | Always freezes |
| Snowy Plains | 0.0 | Always freezes |
| Snowy Slopes | -0.3 | Always freezes |
| Cold Taiga | -0.5 | Always freezes |
| Snowy Beach | 0.05 | Always freezes |

**Non-Freezing Examples:**
- Plains: 0.8
- Desert: 2.0
- Jungle: 0.95

### 2.3 Height-Based Freezing

**Formula for freezing at altitude:**
```
Temperature at height Y = Base Temperature - ((Y - 63) / 600)
```

**Example:** In an Extreme Hills biome (temp 0.2):
- At Y=63: 0.2 (no freeze)
- At Y=93: 0.2 - (30/600) = 0.15 (borderline freeze)
- At Y=123: 0.2 - (60/600) = 0.1 (freezes!)

This means you can build ice farms at high altitude even in non-snowy biomes!

### 2.4 Ice Melting Mechanics

| Condition | Effect |
|-----------|--------|
| **Light Level > 11** | Ice melts (from torches, lanterns, etc.) |
| **Sunlight** | Does NOT melt ice (despite high light level) |

### 2.5 Ice Types

| Type | Source | Harvest Method |
|------|--------|----------------|
| **Ice** | Water freezing in cold biomes | Silk Touch tool |
| **Packed Ice** | Ice spikes biomes, or 9 ice crafted | Silk Touch or crafting |
| **Blue Ice** | Frozen oceans, or 9 packed ice crafted | Silk Touch or crafting |

---

## 3. Snow Farm Designs

### 3.1 Basic Piston Snow Farm

**Concept:** Snow golem walks in circles, piston breaks snow layers into snowballs.

```
Top View:
+------------------+
| P = Piston       |
| G = Snow Golem   |
| H = Hopper       |
| C = Chest        |
| . = Snow Layer   |
+------------------+
  P P P P P P P
  P . . . . . P
  P . G . . . P
  P . . . . . P
  P P H P P P P
      C
```

**Code:**

```java
/**
 * Generates a basic piston snow farm.
 * Dimensions: 7x7 base, 4 blocks tall
 */
public static List<BlockPlacement> buildPistonSnowFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Level 0: Foundation and collection
    for (int x = 0; x < 7; x++) {
        for (int z = 0; z < 7; z++) {
            // Stone base
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));

            // Hopper in center
            if (x == 3 && z == 3) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.HOPPER));
            }
        }
    }

    // Chest under hopper
    blocks.add(new BlockPlacement(start.offset(3, -1, 3), Blocks.CHEST));

    // Level 1: Walls (contain snow golem)
    for (int x = 0; x < 7; x++) {
        for (int z = 0; z < 7; z++) {
            if (x == 0 || x == 6 || z == 0 || z == 5) {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.STONE_BRICKS));
            }
        }
    }

    // Level 2: Pistons (break snow)
    for (int x = 1; x < 6; x++) {
        for (int z = 1; z < 5; z++) {
            // Pistons facing inward
            blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.PISTON));
        }
    }

    // Level 3: Glass roof (prevent snow golem escape)
    for (int x = 0; x < 7; x++) {
        for (int z = 0; z < 7; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 3, z), Blocks.GLASS));
        }
    }

    return blocks;
}
```

### 3.2 Walking Path Snow Farm

**Concept:** Snow golem walks on a contained path, leaving snow that is periodically collected.

```
Top View (figure-8 pattern):
+------------------------+
| W = Wall (glass)       |
| G = Golem walking path |
| H = Hopper collection  |
+------------------------+
  W W W W W W W W
  W G G G G G G W
  W G H H H G G W
  W G G G G G G W
  W W W W W W W W
```

**Code:**

```java
/**
 * Generates a walking path snow farm.
 * Snow golem patrols a figure-8 path.
 */
public static List<BlockPlacement> buildWalkingSnowFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Level 0: Base with hoppers along path
    for (int x = 0; x < 8; x++) {
        for (int z = 0; z < 7; z++) {
            // Base
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));

            // Hoppers in middle row
            if (z == 2 || z == 3 || z == 4) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.HOPPER));
            }
        }
    }

    // Storage under hoppers
    for (int x = 2; x < 6; x++) {
        blocks.add(new BlockPlacement(start.offset(x, -1, 3), Blocks.CHEST));
    }

    // Level 1: Walls (glass for visibility)
    for (int x = 0; x < 8; x++) {
        blocks.add(new BlockPlacement(start.offset(x, 1, 0), Blocks.GLASS));
        blocks.add(new BlockPlacement(start.offset(x, 1, 6), Blocks.GLASS));
    }
    for (int z = 0; z < 7; z++) {
        blocks.add(new BlockPlacement(start.offset(0, 1, z), Blocks.GLASS));
        blocks.add(new BlockPlacement(start.offset(7, 1, z), Blocks.GLASS));
    }

    // Level 2: Roof (half slabs - allows snow placement but contains golem)
    for (int x = 0; x < 8; x++) {
        for (int z = 0; z < 7; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.SMOOTH_STONE_SLAB));
        }
    }

    return blocks;
}
```

### 3.3 Multi-Golem Snow Farm

**Concept:** Multiple snow golems for higher production.

```java
/**
 * Generates a multi-golem snow farm.
 * Supports 2-4 snow golems simultaneously.
 */
public static List<BlockPlacement> buildMultiGolemSnowFarm(BlockPos start, int golemCount) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Calculate size based on golem count
    int size = 6 + (golemCount * 3);

    // Level 0: Foundation with hopper grid
    for (int x = 0; x < size; x++) {
        for (int z = 0; z < size; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));

            // Hopper grid pattern
            if ((x + z) % 2 == 0) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.HOPPER));
            }
        }
    }

    // Level 1-3: Enclosure with dividers
    for (int y = 1; y <= 3; y++) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                // Outer walls
                if (x == 0 || x == size - 1 || z == 0 || z == size - 1) {
                    blocks.add(new BlockPlacement(start.offset(x, y, z), Blocks.GLASS));
                }

                // Dividers between golem sections
                if (x % 4 == 0 && y < 3) {
                    blocks.add(new BlockPlacement(start.offset(x, y, z), Blocks.IRON_BARS));
                }
            }
        }
    }

    // Central collection chest
    blocks.add(new BlockPlacement(start.offset(size/2, -1, size/2), Blocks.CHEST));

    return blocks;
}
```

---

## 4. Ice Farm Designs

### 4.1 Basic Ice Farm

**Concept:** Exposed water in cold biome freezes into ice.

```
Top View:
+------------------+
| S = Source block (protected) |
| I = Ice formation zone |
| . = Water (freezes) |
+------------------+
  S I S I S I S
  I . I . I . I
  S I S I S I S
  I . I . I . I
  S I S I S I S
```

**Code:**

```java
/**
 * Generates a basic ice farm.
 * Water freezes in cold biomes when exposed to sky.
 * Source blocks are protected to maintain infinite water.
 */
public static List<BlockPlacement> buildBasicIceFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    int size = 7;

    for (int x = 0; x < size; x++) {
        for (int z = 0; z < size; z++) {
            // Base (kelp or other solid block)
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));

            // Checkerboard pattern: protected source vs freezing zone
            boolean isSource = (x % 2 == 0) && (z % 2 == 0);

            if (isSource) {
                // Protected source block (prevents freezing)
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));
                // Cover above source to prevent freezing
                blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.SLAB));
            } else {
                // Freezing zone (exposed to sky - no blocks above)
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));
                // Intentionally no blocks above - sky access required
            }
        }
    }

    return blocks;
}
```

### 4.2 Automatic Ice Collection Farm

**Concept:** Pistons automatically break ice when it forms.

```java
/**
 * Generates an automatic ice collection farm.
 * Uses pistons to break ice and collect it.
 */
public static List<BlockPlacement> buildAutoIceFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Level 0: Base with collection system
    for (int x = 0; x < 9; x++) {
        for (int z = 0; z < 9; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));

            // Hopper grid for collection
            if (x % 2 == 1 && z % 2 == 1) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.HOPPER));
            }
        }
    }

    // Storage chests under hoppers
    blocks.add(new BlockPlacement(start.offset(4, -1, 4), Blocks.CHEST));

    // Level 1: Water with protected sources
    for (int x = 0; x < 9; x++) {
        for (int z = 0; z < 9; z++) {
            boolean isSource = (x % 3 == 0) && (z % 3 == 0);

            if (isSource) {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));
                // Protect source from freezing
                blocks.add(new BlockPlacement(start.offset(x, 3, z), Blocks.SLAB));
            } else {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));
            }
        }
    }

    // Level 2: Pistons (facing down) to break ice
    for (int x = 1; x < 8; x += 2) {
        for (int z = 1; z < 8; z += 2) {
            blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.PISTON));
        }
    }

    return blocks;
}
```

### 4.3 High-Altitude Ice Farm

**Concept:** Built at high altitude for freezing in any biome.

```java
/**
 * Generates a high-altitude ice farm.
 * Works in non-snowy biomes due to altitude-based temperature reduction.
 * Best built at Y=150+ for maximum freezing potential.
 */
public static List<BlockPlacement> buildHighAltitudeIceFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Ensure we're building at high altitude
    if (start.getY() < 120) {
        // Adjust to minimum effective altitude
        start = new BlockPos(start.getX(), 150, start.getZ());
    }

    int size = 11;

    // Level 0: Foundation
    for (int x = 0; x < size; x++) {
        for (int z = 0; z < size; z++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));
        }
    }

    // Level 1: Water array with protected sources
    for (int x = 0; x < size; x++) {
        for (int z = 0; z < size; z++) {
            boolean isSource = (x % 4 == 0) || (z % 4 == 0);

            blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.WATER));

            if (isSource) {
                // Protect source blocks
                blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.SLAB));
            }
            // Non-source blocks exposed to sky for freezing
        }
    }

    // Collection system
    blocks.add(new BlockPlacement(start.offset(size/2, -1, size/2), Blocks.HOPPER));
    blocks.add(new BlockPlacement(start.offset(size/2, -2, size/2), Blocks.CHEST));

    return blocks;
}
```

### 4.4 Climate Detection Utility

```java
/**
 * Utility class for detecting if ice farming is viable at a location.
 */
public class ClimateDetection {

    private static final float FREEZING_THRESHOLD = 0.15f;
    private static final int HIGH_ALTITUDE_THRESHOLD = 120;

    /**
     * Checks if water will freeze at the given position.
     */
    public static boolean canWaterFreeze(Level level, BlockPos pos) {
        if (level.isClientSide) return false;

        // Get biome temperature at position
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();
        float temperature = biome.getTemperature(pos);

        // Check if temperature is below freezing threshold
        return temperature < FREEZING_THRESHOLD;
    }

    /**
     * Checks if snow golems will leave snow trails in this biome.
     * Java Edition: Only in cold/snowy/temperate biomes.
     */
    public static boolean canSnowGolemTrail(Level level, BlockPos pos) {
        if (level.isClientSide) return false;

        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();
        float temperature = biome.getTemperature(pos);

        // Snow golems leave trails in biomes with temp < 0.95
        // Excluding hot/dry biomes (desert, savanna, badlands, nether)
        return temperature < 0.95f;
    }

    /**
     * Finds the nearest suitable location for an ice farm.
     */
    public static BlockPos findIceFarmLocation(Level level, BlockPos center, int searchRadius) {
        BlockPos bestPos = null;
        float lowestTemp = Float.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x += 16) {
            for (int z = -searchRadius; z <= searchRadius; z += 16) {
                BlockPos checkPos = center.offset(x, 0, z);

                // Check surface level
                checkPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, checkPos);

                if (canWaterFreeze(level, checkPos)) {
                    float temp = level.getBiome(checkPos).value().getTemperature(checkPos);
                    if (temp < lowestTemp) {
                        lowestTemp = temp;
                        bestPos = checkPos;
                    }
                }
            }
        }

        return bestPos;
    }

    /**
     * Calculates the minimum altitude for ice farming in a given biome.
     */
    public static int getMinFreezingAltitude(Level level, BlockPos basePos) {
        Biome biome = level.getBiome(basePos).value();
        float baseTemp = biome.getTemperature(basePos);

        if (baseTemp < FREEZING_THRESHOLD) {
            return basePos.getY(); // Already freezes at ground level
        }

        // Calculate altitude needed for freezing
        // Formula: temp - ((Y - 63) / 600) < 0.15
        // Solving for Y: Y > 63 + (temp - 0.15) * 600
        int minAltitude = (int) Math.ceil(63 + (baseTemp - FREEZING_THRESHOLD) * 600);

        return Math.max(minAltitude, HIGH_ALTITUDE_THRESHOLD);
    }
}
```

---

## 5. Packed Ice Production

### 5.1 Packed Ice Mechanics

| Property | Value |
|----------|-------|
| **Source** | Ice spikes biomes, or crafted |
| **Crafting Recipe** | 9 ice = 1 packed ice |
| **Harvest** | Silk Touch required |
| **Melting** | Does not melt |
| **Friction** | Slippery (entities slide) |

### 5.2 Packed Ice Farm with Auto-Crafting

```java
/**
 * Generates a packed ice production facility.
 * Includes ice farm + auto-crafting system.
 */
public static List<BlockPlacement> buildPackedIceFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Ice farm section (left side)
    blocks.addAll(buildBasicIceFarm(start));

    // Auto-crafting section (right side)
    BlockPos craftStart = start.offset(15, 0, 0);

    // Level 0: Storage and crafting
    for (int x = 0; x < 5; x++) {
        for (int z = 0; z < 5; z++) {
            blocks.add(new BlockPlacement(craftStart.offset(x, 0, z), Blocks.SMOOTH_STONE));
        }
    }

    // Storage chest for ice
    blocks.add(new BlockPlacement(craftStart.offset(2, 1, 2), Blocks.CHEST));

    // Crafting table (arranged for auto-crafting)
    blocks.add(new BlockPlacement(craftStart.offset(1, 1, 1), Blocks.CRAFTING_TABLE));

    // Dispenser system for crafting (3x3 grid)
    for (int x = 0; x < 3; x++) {
        for (int z = 0; z < 3; z++) {
            blocks.add(new BlockPlacement(craftStart.offset(x, 2, z), Blocks.DISPENSER));
        }
    }

    // Output chest for packed ice
    blocks.add(new BlockPlacement(craftStart.offset(3, 1, 3), Blocks.CHEST));

    return blocks;
}
```

### 5.3 Blue Ice Production

```java
/**
 * Generates a blue ice production facility.
 * Requires packed ice as input.
 */
public static List<BlockPlacement> buildBlueIceFarm(BlockPos start) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Similar to packed ice, but needs 9 packed ice = 1 blue ice
    // Two-stage crafting: Ice -> Packed Ice -> Blue Ice

    // Stage 1: Ice to Packed Ice
    blocks.addAll(buildPackedIceFarm(start));

    // Stage 2: Packed Ice to Blue Ice
    BlockPos blueStart = start.offset(25, 0, 0);

    // Storage for packed ice
    blocks.add(new BlockPlacement(blueStart.offset(0, 1, 0), Blocks.CHEST));

    // Crafting array for blue ice
    for (int x = 0; x < 3; x++) {
        for (int z = 0; z < 3; z++) {
            blocks.add(new BlockPlacement(blueStart.offset(x, 1, z + 2), Blocks.CRAFTING_TABLE));
        }
    }

    // Output for blue ice
    blocks.add(new BlockPlacement(blueStart.offset(1, 1, 6), Blocks.CHEST));

    return blocks;
}
```

---

## 6. Climate Control

### 6.1 Biome Temperature Modification

While vanilla Minecraft doesn't allow biome temperature modification, MineWright can provide tools for working within climate constraints:

```java
/**
 * Climate utilities for snow and ice farming.
 */
public class ClimateUtilities {

    /**
     * Gets detailed climate information for a position.
     */
    public static ClimateInfo getClimateInfo(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();

        float temperature = biome.getTemperature(pos);
        boolean canFreeze = temperature < 0.15f;
        boolean snowGolemTrail = temperature < 0.95f;

        return new ClimateInfo(
            biome,
            temperature,
            canFreeze,
            snowGolemTrail,
            pos.getY()
        );
    }

    /**
     * Record class for climate information.
     */
    public record ClimateInfo(
        Biome biome,
        float temperature,
        boolean canWaterFreeze,
        boolean snowGolemCanTrail,
        int altitude
    ) {
        public String getRecommendation() {
            if (canWaterFreeze) {
                return "Ideal for ice farming. Temperature: " + temperature;
            } else if (altitude > 120) {
                return "High altitude may enable freezing. Consider building higher.";
            } else if (snowGolemCanTrail) {
                return "Suitable for snow farming with snow golems.";
            } else {
                return "Not suitable for snow/ice farming. Biome too warm: " + biome;
            }
        }
    }

    /**
     * Finds the best nearby location for snow farming.
     */
    public static BlockPos findSnowFarmLocation(Level level, BlockPos center, int radius) {
        BlockPos bestPos = null;
        float bestTemp = Float.MAX_VALUE;

        for (int x = -radius; x <= radius; x += 8) {
            for (int z = -radius; z <= radius; z += 8) {
                BlockPos checkPos = center.offset(x, 0, z);
                checkPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, checkPos);

                float temp = level.getBiome(checkPos).value().getTemperature(checkPos);
                if (temp < bestTemp && temp < 0.95f) {
                    bestTemp = temp;
                    bestPos = checkPos;
                }
            }
        }

        return bestPos != null ? bestPos : center;
    }
}
```

---

## 7. Code Integration

### 7.1 Build Snow Farm Action

Create new action: `BuildSnowFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.farming.ClimateDetection;
import com.minewright.farming.ClimateUtilities;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.StructureGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Action for building a snow farm.
 * Detects climate and builds appropriate design.
 */
public class BuildSnowFarmAction extends BaseAction {

    private final int golemCount;
    private final String farmType;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;
    private static final int BLOCKS_PER_TICK = 4;

    public BuildSnowFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.golemCount = task.getIntParameter("golems", 1);
        this.farmType = task.getStringParameter("type", "piston");
    }

    @Override
    protected void onStart() {
        Level level = foreman.level();
        BlockPos currentPos = foreman.blockPosition();

        // Check climate suitability
        ClimateUtilities.ClimateInfo climate = ClimateUtilities.getClimateInfo(level, currentPos);

        if (!climate.snowGolemCanTrail()) {
            foreman.sendChatMessage("Warning: This biome may not support snow golem trails.");
            foreman.sendChatMessage("Climate: " + climate.getRecommendation());
        }

        // Select farm design
        switch (farmType.toLowerCase()) {
            case "walking":
                buildPlan = StructureGenerators.buildWalkingSnowFarm(currentPos);
                break;
            case "multi":
                buildPlan = StructureGenerators.buildMultiGolemSnowFarm(currentPos, golemCount);
                break;
            case "piston":
            default:
                buildPlan = StructureGenerators.buildPistonSnowFarm(currentPos);
                break;
        }

        if (buildPlan.isEmpty()) {
            result = ActionResult.failure("Failed to generate snow farm structure");
            return;
        }

        foreman.sendChatMessage("Building snow farm (" + farmType + ") with " +
            golemCount + " golem(s)...");
        foreman.sendChatMessage("Farm requires " + buildPlan.size() + " blocks.");
        foreman.setFlying(true);
    }

    @Override
    protected void onTick() {
        if (buildPlan == null) return;

        int blocksThisTick = 0;
        while (currentBlockIndex < buildPlan.size() && blocksThisTick < BLOCKS_PER_TICK) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);

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
        if (currentBlockIndex % 50 == 0) {
            int percentComplete = (currentBlockIndex * 100) / buildPlan.size();
            foreman.sendChatMessage("Snow farm progress: " + percentComplete + "%");
        }

        // Check completion
        if (currentBlockIndex >= buildPlan.size()) {
            foreman.setFlying(false);

            // Spawn snow golems
            spawnSnowGolems();

            result = ActionResult.success("Snow farm completed with " + golemCount +
                " snow golem(s)! Producing snowballs automatically.");
        }
    }

    private void spawnSnowGolems() {
        // Spawn snow golems at designated positions
        for (int i = 0; i < golemCount; i++) {
            BlockPos spawnPos = findGolemSpawnPosition(i);
            spawnSnowGolem(foreman.level(), spawnPos);
        }
    }

    private BlockPos findGolemSpawnPosition(int index) {
        // Calculate spawn position based on farm type and index
        BlockPos base = foreman.blockPosition();
        return base.offset(index * 3, 2, 0);
    }

    private void spawnSnowGolem(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            net.minecraft.world.entity.EntityType<?> snowGolemType =
                net.minecraft.world.entity.EntityType.SNOW_GOLEM;
            var snowGolem = snowGolemType.create(level);
            if (snowGolem != null) {
                snowGolem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                level.addFreshEntity(snowGolem);
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build snow farm (" + currentBlockIndex + "/" +
            (buildPlan != null ? buildPlan.size() : 0) + " blocks)";
    }
}
```

### 7.2 Build Ice Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.farming.ClimateUtilities;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.StructureGenerators;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Action for building an ice farm.
 * Detects climate and builds at appropriate altitude if needed.
 */
public class BuildIceFarmAction extends BaseAction {

    private final String farmType;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;
    private static final int BLOCKS_PER_TICK = 4;

    public BuildIceFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.farmType = task.getStringParameter("type", "basic");
    }

    @Override
    protected void onStart() {
        var level = foreman.level();
        BlockPos currentPos = foreman.blockPosition();

        // Check climate
        ClimateUtilities.ClimateInfo climate = ClimateUtilities.getClimateInfo(level, currentPos);

        BlockPos buildPos = currentPos;

        if (!climate.canWaterFreeze()) {
            // Find suitable altitude
            int minAltitude = ClimateDetection.getMinFreezingAltitude(level, currentPos);
            buildPos = new BlockPos(currentPos.getX(), minAltitude, currentPos.getZ());

            foreman.sendChatMessage("Current location too warm for ice farming.");
            foreman.sendChatMessage("Building at altitude Y=" + minAltitude + " for freezing.");
        } else {
            foreman.sendChatMessage("Climate suitable for ice farming! Temperature: " +
                climate.temperature());
        }

        // Generate farm structure
        switch (farmType.toLowerCase()) {
            case "auto":
                buildPlan = StructureGenerators.buildAutoIceFarm(buildPos);
                break;
            case "high_altitude":
                buildPlan = StructureGenerators.buildHighAltitudeIceFarm(buildPos);
                break;
            case "basic":
            default:
                buildPlan = StructureGenerators.buildBasicIceFarm(buildPos);
                break;
        }

        if (buildPlan.isEmpty()) {
            result = ActionResult.failure("Failed to generate ice farm structure");
            return;
        }

        foreman.sendChatMessage("Building " + farmType + " ice farm...");
        foreman.sendChatMessage("Farm requires " + buildPlan.size() + " blocks.");
        foreman.setFlying(true);
    }

    @Override
    protected void onTick() {
        if (buildPlan == null) return;

        int blocksThisTick = 0;
        while (currentBlockIndex < buildPlan.size() && blocksThisTick < BLOCKS_PER_TICK) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);

            foreman.level().setBlock(
                placement.pos(),
                placement.block().defaultBlockState(),
                3
            );

            currentBlockIndex++;
            blocksThisTick++;

            double distance = Math.sqrt(foreman.blockPosition().distSqr(placement.pos()));
            if (distance > 8) {
                foreman.teleportTo(
                    placement.pos().getX() + 0.5,
                    placement.pos().getY(),
                    placement.pos().getZ() + 0.5
                );
            }
        }

        if (currentBlockIndex % 50 == 0) {
            int percentComplete = (currentBlockIndex * 100) / buildPlan.size();
            foreman.sendChatMessage("Ice farm progress: " + percentComplete + "%");
        }

        if (currentBlockIndex >= buildPlan.size()) {
            foreman.setFlying(false);
            result = ActionResult.success("Ice farm completed! Ice will form automatically in cold conditions.");
        }
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build ice farm (" + currentBlockIndex + "/" +
            (buildPlan != null ? buildPlan.size() : 0) + " blocks)";
    }
}
```

### 7.3 Register the Actions

Update `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Snow and Ice farming
    registry.register("build_snow_farm",
        (foreman, task, ctx) -> new BuildSnowFarmAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("build_ice_farm",
        (foreman, task, ctx) -> new BuildIceFarmAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### 7.4 Update Prompt Builder

Add to `PromptBuilder.java`:

```java
private String getActionDescriptions() {
    return """
        ...

        Farming Actions:
        - build_snow_farm: Build an automated snow farm using snow golems
          * Parameters: golems (integer, 1-4, default: 1), type (piston/walking/multi)
          * Produces: Snowballs automatically collected in chests
          * Climate: Works best in cold/temperate biomes

        - build_ice_farm: Build an automated ice farm
          * Parameters: type (basic/auto/high_altitude)
          * Produces: Ice blocks through natural freezing
          * Climate: Automatically detects suitable location or builds at altitude

        ...
        """;
}
```

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1)

**Tasks:**
1. Create `ClimateDetection` utility class
2. Create `ClimateUtilities` for climate analysis
3. Implement basic snow farm structure generation
4. Implement basic ice farm structure generation
5. Test biome temperature detection

**Deliverables:**
- `ClimateDetection.java`
- `ClimateUtilities.java`
- Basic structure generators in `StructureGenerators.java`
- Unit tests for climate detection

### Phase 2: Snow Farm Implementation (Week 2)

**Tasks:**
1. Implement piston snow farm design
2. Implement walking path snow farm design
3. Implement multi-golem snow farm design
4. Add snow golem spawning logic
5. Create collection system variants

**Deliverables:**
- Complete snow farm structure generators
- `BuildSnowFarmAction` class
- Integration tests with snow golems

### Phase 3: Ice Farm Implementation (Week 3)

**Tasks:**
1. Implement basic ice farm with protected sources
2. Implement automatic ice collection with pistons
3. Implement high-altitude ice farm design
4. Add altitude-based freezing calculation
5. Test ice formation in various biomes

**Deliverables:**
- Complete ice farm structure generators
- `BuildIceFarmAction` class
- Climate-aware building logic

### Phase 4: Packed Ice & Blue Ice (Week 4)

**Tasks:**
1. Implement packed ice auto-crafting system
2. Implement blue ice production (two-stage crafting)
3. Add inventory management for crafting
4. Optimize production rates

**Deliverables:**
- Packed ice farm generator
- Blue ice farm generator
- Auto-crafting integration

### Phase 5: Action Integration & Polish (Week 5)

**Tasks:**
1. Register actions in `CoreActionsPlugin`
2. Update `PromptBuilder` with farm actions
3. Add configuration options
4. Performance optimization
5. Documentation and tutorials

**Deliverables:**
- Fully integrated farming actions
- LLM integration for natural language commands
- User documentation
- Tutorial videos/guides

---

## Appendix A: Configuration

Add to `config/steve-common.toml`:

```toml
[snow_farming]
# Maximum number of snow golems per farm
max_golems = 4

# Default snow farm type: "piston", "walking", "multi"
default_farm_type = "piston"

# Enable auto-spawning of snow golems
auto_spawn_golems = true

[ice_farming]
# Minimum altitude for high-altitude ice farms
min_altitude = 150

# Ice collection method: "manual", "piston", "auto"
collection_method = "piston"

# Enable climate-based location detection
auto_detect_climate = true

# Search radius for finding suitable ice farm locations
search_radius = 500

[production]
# Enable auto-crafting of packed ice
auto_craft_packed = true

# Enable auto-crafting of blue ice
auto_craft_blue = true

# Storage chest size multiplier
chest_capacity = 27
```

---

## Appendix B: Troubleshooting

### Issue: Snow Golems Not Leaving Snow Trails

**Possible Causes:**
1. Biome is too hot (desert, savanna, nether)
2. Snow golem is in a dry/hot biome variant

**Solutions:**
- Check biome temperature with `/forge track` or debug tools
- Relocate farm to colder biome
- Use climate detection to find suitable location

### Issue: Ice Not Forming

**Possible Causes:**
1. Biome temperature too high (> 0.15)
2. Water not exposed to sky (blocks above)
3. Light level too high (> 12 from artificial sources)
4. Altitude not high enough for warm biomes

**Solutions:**
- Verify sky access (remove blocks above water)
- Remove nearby light sources
- Build at higher altitude (Y=150+)
- Use `/forge biome` command to check biome temperature

### Issue: Snow/Ice Not Collecting

**Possible Causes:**
1. Hoppers not properly connected
2. Chests full
3. Pistons not breaking blocks
4. Water flow incorrect

**Solutions:**
- Verify hopper connections with debug stick
- Add more chest storage
- Check redstone power to pistons
- Verify water source blocks

---

## References

### Sources

- [Minecraft Wiki - Snow Golem](https://minecraft.fandom.com/wiki/Snow_Golem)
- [Minecraft Wiki - Snow](https://minecraft.fandom.com/wiki/Snow)
- [Tutorials/Ice farming - Minecraft Wiki](https://minecraft.fandom.com/wiki/Tutorials/Ice_farming)
- [Ranking coldest to warmest Minecraft biomes - Sportskeeda](https://www.sportskeeda.com/minecraft/ranking-coldest-warmest-minecraft-biomes)
- [Biomes - Bedrock Wiki](https://wiki.bedrock.dev/world-generation/biomes)
- [Minecraft Forge Weather Control System](https://m.blog.csdn.net/gitblog_00284/article/details/152066910)
- [Minecraft Mod Development: Snow Ball Monster Tutorial](https://m.blog.csdn.net/qq_52247089/article/details/119150028)
- [Snow Golem Entity (Microsoft Learn)](https://learn.microsoft.com/zh-cn/minecraft/creator/reference/source/vanillabehaviorpack_snippets/entities/snow_golem?view=minecraft-bedrock-stable)

### Minecraft 1.20.1 Forge API

- `net.minecraft.world.entity.animal.SnowGolem`
- `net.minecraft.world.level.block.SnowBlock`
- `net.minecraft.world.level.block.IceBlock`
- `net.minecraft.world.level.biome.Biome`
- `net.minecraft.world.level.Level`

### Related MineWright Code

- `C:\Users\casey\steve\src\main\java\com\minewright\structure\StructureGenerators.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildStructureAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\plugin\CoreActionsPlugin.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

---

## Summary

This design provides a complete implementation plan for snow and ice farming in MineWright, including:

1. **Snow Golem Mechanics**: Trail generation, biome restrictions, spawning methods
2. **Ice Formation Mechanics**: Temperature requirements, altitude effects, freezing conditions
3. **Snow Farm Designs**: Piston-based, walking path, and multi-golem variants
4. **Ice Farm Designs**: Basic, automatic collection, and high-altitude variants
5. **Packed Ice Production**: Auto-crafting systems for packed ice and blue ice
6. **Climate Control**: Biome detection, temperature analysis, location finding
7. **Code Integration**: Complete implementation with structure generators and actions

The snow and ice farming system provides:
- **Automatic snowball production** through snow golem trails
- **Renewable ice blocks** through climate-aware farming
- **Packed ice and blue ice** production via auto-crafting
- **Climate-smart building** that detects optimal locations
- **Scalable designs** from small to industrial-scale production

---

*Document Version: 1.0*
*Last Updated: 2026-02-27*
*Author: Orchestrator (MineWright AI Team)*
