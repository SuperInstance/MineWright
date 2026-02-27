# Drowned Farm Design for MineWright (Minecraft Forge 1.20.1)

## Overview

This document provides a comprehensive design for building automated drowned farms in Minecraft 1.20.1 (Java Edition) using the MineWright mod. Drowned farms provide valuable resources including tridents, nautilus shells, copper ingots, and experience points.

**Key Resources:**
- **Tridents** - Rare throwing weapons (3.75% base drop chance)
- **Nautilus Shells** - For conduits (8% of drowned carry them)
- **Copper Ingots** - From drowned drops
- **Experience** - Sustainable XP source

---

## Table of Contents

1. [Spawning Mechanics](#spawning-mechanics)
2. [Drop Rate Optimization](#drop-rate-optimization)
3. [Farm Designs](#farm-designs)
4. [Collection Systems](#collection-systems)
5. [MineWright Integration](#minewright-integration)
6. [Implementation Roadmap](#implementation-roadmap)

---

## Spawning Mechanics

### Natural Spawning Requirements

Drowned spawn naturally in water under these conditions:

| Requirement | Value | Notes |
|-------------|-------|-------|
| Light Level | 0 | Must be completely dark |
| Water Depth | 2+ blocks | Flowing or source water |
| Biome | All ocean biomes, rivers | Prefer ocean for maximum spawn area |
| Y-Level (Ocean) | Y < 58 | Or 6+ blocks below sea level |
| Y-Level (River) | Any | No height restriction in rivers |
| Player Distance | 24-128 blocks | Must be within range but not too close |

### Critical Spawning Rules

**Java Edition 1.20.1:**
- Drowned spawn individually in water that is 2+ blocks deep
- Can spawn in flowing water OR source water
- Cannot spawn on glass, transparent blocks, or non-solid blocks
- Spawn attempts reduced if other mobs exist nearby (mob cap)
- Ocean biome drowned only spawn below Y=58

**Trident-Wielding Drowned:**
- Only **15% of naturally spawned drowned** spawn with tridents
- **Converted zombies NEVER spawn with tridents** (Java Edition)
- This makes natural spawning essential for trident farms

### Zombie Conversion Mechanics

Regular zombies convert to drowned after:
- **30 seconds (600 ticks)** of continuous submersion
- Shakes and emits bubbles during conversion
- Retains held items (Java Edition)
- Can spawn with fishing rod after conversion

**Conversion Farm Strategy:**
- More consistent spawns (zombie spawners)
- Better for copper, nautilus shells, rotten flesh
- **Cannot produce tridents** in Java Edition

---

## Drop Rate Optimization

### Drop Chances Table

| Item | Drop Chance | With Looting III | Notes |
|------|-------------|------------------|-------|
| Trident (from trident drowned) | 25% base | 37% | Only from naturally spawned drowned with tridents |
| Trident (overall per drowned) | 3.75% | 5.55% | 15% spawn with trident × 25% drop rate |
| Nautilus Shell | 8% (held) | 11.5% | Both natural and converted drowned |
| Copper Ingot | 11% | 15.25% | From natural drowned only |
| Rotten Flesh | 0-2 | 0-5 | Common drop |
| Gold Ingots | 0-1 | 0-4 | Rare drop |

### Optimization Strategies

#### 1. Looting Enchantment
- Use **Looting III** on killing weapon
- Increases trident drop chance from 25% → 37%
- Increases nautilus shell from 8% → 11.5%
- Increases copper from 11% → 15.25%

#### 2. Spawn Platform Design
- **Maximize spawning area**: Use largest possible platform (up to 128×128)
- **Optimal height**: Build spawning platform at Y=50-55 in ocean biomes
- **Light control**: Ensure complete darkness (light level 0)
- **Water depth**: 2 blocks minimum for spawning

#### 3. Spawn Rate Optimization
- Remove all other spawning spaces within 128 blocks
- Use AFK platform at correct distance (24-128 blocks from spawn)
- Clear nearby caves to reduce competing mob spawns
- Consider Nether portal method to bypass mob cap

#### 4. Multi-Layer Spawning
```java
// Spawning efficiency increases with more layers
// Each layer must be:
// - At least 2 blocks below the layer above
// - Within 128 blocks of player
// - Properly lit (light level 0)

// Optimal spacing: 3-4 blocks between layers
int layerSpacing = 3;
int maxLayers = 4;
int platformsPerLayer = 4; // 2x2 grid per layer
```

---

## Farm Designs

### Design 1: Aerial Platform Farm (Best for Tridents)

**Overview:** Build above ocean, drowned spawn on platform and fall into collection.

**Advantages:**
- Produces tridents (natural spawning only)
- High spawn rates in ocean biome
- Clean, expandable design
- Multi-layer friendly

**Disadvantages:**
- Complex initial build
- Requires water management
- Height restrictions (Y < 58 in ocean)

#### Structure Layout

```
Top View (Spawning Platform - 80×80):
┌────────────────────────────────────────────────────────────────┐
│  Glass Water Layer (light reduction)                           │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Water (2 blocks deep)                                │    │
│  │  ┌────────────────────────────────────────────┐      │    │
│  │  │  Spawnable Solid Blocks (stone/cobble)      │      │    │
│  │  │  ████████████████████████████████████████   │      │    │
│  │  │  █░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░█   │      │    │
│  │  │  █░░░░░░░░░HOLE░░░░░░░░HOLE░░░░░░░░░░░░░░█   │      │    │
│  │  │  █░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░█   │      │    │
│  │  │  ████████████████████████████████████████   │      │    │
│  │  └────────────────────────────────────────────┘      │    │
│  └──────────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────────┘

Side View:
    ┌─────────────────────────────┐ Y=200+
    │ AFK Platform (Player)       │
    └─────────────────────────────┘
              │ 24+ blocks
    ┌─────────────────────────────┐ Y=120-150
    │ Killing Chamber (1×1)       │ ← Collect items here
    │ Water / Signs for survival  │
    └─────────────────────────────┘
              │ 23 blocks (non-lethal)
    ┌─────────────────────────────┐ Y=97-99
    │ Intermediate Platform       │ ← Prevents fall damage
    │ (optional, can skip)        │
    └─────────────────────────────┘
              │ 3+ blocks
    ┌─────────────────────────────┐ Y=50-55
    │ Spawning Platform (80×80)   │ ← Drowned spawn here
    │ Water channels (2 deep)     │
    │ Holes every 4 blocks        │
    └─────────────────────────────┘
              │
    ┌─────────────────────────────┐ Y=48
    │ Collection Water Stream     │
    │ (pushes to center)          │
    └─────────────────────────────┘
```

#### Building Steps

1. **Find Ocean Location**
   - Deep ocean or frozen ocean biome
   - At least 80×80 blocks of open water
   - Sea level around Y=62

2. **Build Spawning Platform at Y=50**
   ```
   Dimensions: 80×80 blocks minimum
   Materials: Stone, cobblestone, or any solid block
   Water: 2 blocks deep covering entire platform
   Holes: Every 4 blocks in a grid pattern
   ```

3. **Create Water Channels**
   ```java
   // Channel pattern (every 4 blocks)
   for (int x = 0; x < 80; x += 4) {
       for (int z = 0; z < 80; z += 4) {
           // Create 2×2 hole
           clearArea(x, Y=50, z, 2, 1, 2);
           // Place water to flow toward center
       }
   }
   ```

4. **Build Collection Shaft**
   - Central drop shaft: 2×2 blocks
   - Height: 45-50 blocks up
   - Prevents fall damage with intermediate platform

5. **Killing Chamber**
   - 1×1 or 2×2 chamber
   - Signs or water to prevent drowning yourself
   - Hopppers for item collection
   - AFK spot directly above

6. **Light Control**
   - Ensure light level 0 on spawning platform
   - Use torches to light caves below
   - Remove all spawnable surfaces nearby

---

### Design 2: River Conversion Farm (Best for Nautilus Shells)

**Overview:** Spawn zombies in dungeon/natural spawner, convert to drowned in water.

**Advantages:**
- Consistent spawn rate (spawner-based)
- Good for nautilus shells and copper
- Simpler to build
- Works in any biome

**Disadvantages:**
- **No tridents** (converted drowned don't spawn with them)
- Requires zombie spawner
- Lower overall value

#### Structure Layout

```
┌────────────────────────────────────────┐
│ Zombie Spawner (dungeon)               │
│ ┌──────────────────────────────────┐  │
│ │  Water Conversion Chamber         │  │
│ │  (zombies become drowned)         │  │
│ │  → Flow to collection              │  │
│ └──────────────────────────────────┘  │
└────────────────────────────────────────┘
           │ Water flow
    ┌──────────────┐
    │ Killing Area │
    │ (AFK spot)   │
    └──────────────┘
```

#### Building Steps

1. **Find Zombie Spawner**
   - In dungeons (naturally generated)
   - Abandoned mineshaft
   - Or build with zombie spawn eggs

2. **Create Water Chamber**
   - 9×9×9 room minimum
   - Fill bottom with water (2 blocks deep)
   - Zombies will convert after 30 seconds

3. **Flow System**
   - Use water currents to push drowned
   - Single collection point
   - Sign-based elevator (optional)

4. **Killing Area**
   - 1×1 chamber
   - Looting III sword
   - Hopper collection system

---

### Design 3: Multi-Layer Farm (Maximum Efficiency)

**Overview:** Stack multiple spawning layers vertically for maximum spawn rates.

#### Specifications

| Layer Count | Spawning Area | Expected Rate (drowns/hr) |
|-------------|---------------|---------------------------|
| 1 Layer | 80×80 | ~900 |
| 2 Layers | 160×80 | ~1,600 |
| 3 Layers | 240×80 | ~2,200 |
| 4 Layers | 320×80 | ~2,700 |

#### Vertical Layout

```
Y=200:  ┌─────────┐ AFK Platform
         │ Player  │
Y=150:  ┌─────────┐ Killing Chamber
         │ Kill    │
Y=100:  ┌─────────┐ Intermediate (optional)
Y=60:   ┌─────────┐ Spawn Layer 3
Y=55:   ┌─────────┐ Spawn Layer 2
Y=50:   ┌─────────┐ Spawn Layer 1 (Main)
         │ Ocean   │
Y=62:   ┌─────────┘ Sea Level
```

#### Implementation Notes

```java
// Multi-layer spawning configuration
public class MultiLayerDrownedFarm {
    private static final int LAYER_COUNT = 3;
    private static final int LAYER_SPACING = 5; // blocks between layers
    private static final int PLATFORM_SIZE = 80;

    public static void buildLayer(int layerIndex, BlockPos basePos) {
        int yOffset = layerIndex * LAYER_SPACING;
        BlockPos layerPos = basePos.offset(0, -yOffset, 0);

        // Each layer must be independent
        // - Own water source
        // - Own collection funnel
        // - Feed into main drop shaft
    }
}
```

---

## Collection Systems

### System 1: Basic Drop Collection

**Simplest method:** Drowneds fall into a 1×1 chamber, you kill them manually.

**Requirements:**
- Drop shaft: 23 blocks (prevents fall damage)
- Killing chamber: 1×1 or 2×2
- Hoppers: 1-4 for item collection
- Chest: Storage below hoppers

**Pros:** Simple, effective
**Cons:** Manual killing required

---

### System 2: Bubble Column Transport

**Advanced method:** Use bubble columns to transport drowned.

#### Components

| Component | Material | Function |
|-----------|----------|----------|
| Upward Column | Soul Sand + Water | Push drowned up |
| Downward Column | Magma Block + Water | Pull drowned down |
| Horizontal Flow | Water + Signs | Move sideways |
| Collection | Hopper Minecart | Collect items |

#### Design Pattern

```java
// Bubble column elevator design
public class BubbleColumnElevator {
    private static final Block SOUL_SAND = Blocks.SOUL_SAND;
    private static final Block MAGMA_BLOCK = Blocks.MAGMA_BLOCK;
    private static final Block WATER = Blocks.WATER;

    /**
     * Creates an upward bubble column elevator
     * @param basePos Base position
     * @param height Height of elevator in blocks
     */
    public static void buildUpwardElevator(BlockPos basePos, int height) {
        for (int y = 0; y < height; y++) {
            // Place soul sand at bottom
            if (y == 0) {
                level.setBlock(basePos, SOUL_SAND.defaultBlockState(), 3);
            }
            // Place water source blocks above
            else {
                level.setBlock(basePos.above(y), WATER.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Creates a downward bubble column
     * Useful for bringing items to collection point
     */
    public static void buildDownwardElevator(BlockPos basePos, int depth) {
        // Magma at top, water below
        level.setBlock(basePos, MAGMA_BLOCK.defaultBlockState(), 3);

        for (int y = 1; y <= depth; y++) {
            level.setBlock(basePos.below(y), WATER.defaultBlockState(), 3);
        }
    }
}
```

---

### System 3: Water Stream Collection

**Efficient method:** Water streams push items to hoppers.

#### Layout

```
Spawning Platform
    │ (drowned spawn here)
    ↓
Water Channels (every 4 blocks)
    │ (push drowned to center)
    ↓
Central Collection Shaft
    │ (drowned fall)
    ↓
Killing Chamber
    │ (you kill them)
    ↓
Hopper Collection System
    │ (items flow)
    ↓
┌─────────────────┐
│  ←  →  ←  →     │ Water streams
│  H   H   H   H  │ Hoppers
│  └───┴───┴───┘  │
│     CHESTS      │ Storage
└─────────────────┘
```

#### Implementation

```java
// Water stream collection system
public class WaterCollectionSystem {
    /**
     * Creates water streams flowing to center
     */
    public static void buildWaterStreams(BlockPos center, int radius) {
        // Create 4 streams from cardinal directions
        for (int direction = 0; direction < 4; direction++) {
            for (int i = 1; i <= radius; i++) {
                BlockPos streamPos;

                switch (direction) {
                    case 0: streamPos = center.east(i); break;   // East
                    case 1: streamPos = center.west(i); break;   // West
                    case 2: streamPos = center.south(i); break;  // South
                    case 3: streamPos = center.north(i); break;  // North
                    default: continue;
                }

                // Place water source block
                level.setBlock(streamPos, Blocks.WATER.defaultBlockState(), 3);

                // Place signs below to contain water
                level.setBlock(streamPos.below(),
                    Blocks.OAK_SIGN.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Build hopper collection array
     */
    public static void buildHopperArray(BlockPos centerPos) {
        // 2×2 hopper array centered on position
        BlockPos[] hopperPositions = {
            centerPos,
            centerPos.east(),
            centerPos.south(),
            centerPos.south().east()
        };

        for (BlockPos pos : hopperPositions) {
            level.setBlock(pos, Blocks.HOPPER.defaultBlockState()
                .setValue(HopperBlock.FACING, Direction.DOWN), 3);

            // Chest below each hopper
            level.setBlock(pos.below(), Blocks.CHEST.defaultBlockState(), 3);
        }
    }
}
```

---

### System 4: Minecart with Hopper Collection

**Automatic method:** Minecart with hopper collects items continuously.

#### Design

```java
public class MinecartCollectionSystem {
    /**
     * Creates a circular track for hopper minecart
     */
    public static void buildCollectionTrack(BlockPos center, int radius) {
        // Build rail circle
        for (int angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            int x = (int) Math.round(radius * Math.cos(radians));
            int z = (int) Math.round(radius * Math.sin(radians));

            BlockPos railPos = center.offset(x, 0, z);
            level.setBlock(railPos, Blocks.POWERED_RAIL.defaultBlockState(), 3);

            // Powered rail every 5th block
            if (angle % 30 == 0) {
                level.setBlock(railPos, Blocks.POWERED_RAIL.defaultBlockState()
                    .setValue(PoweredRailBlock.POWERED, Boolean.TRUE), 3);
            }
        }

        // Spawn hopper minecart
        Minecart minecart = EntityType.HOPPER_MINECART.create(level);
        minecart.setPos(center.getX(), center.getY(), center.getZ());
        level.addFreshEntity(minecart);

        // Storage minecart at station
        EntityType.MINECART.create(level);
    }
}
```

---

## MineWright Integration

### New Action: BuildDrownedFarmAction

Create a new action that allows Foremen to build drowned farms autonomously.

```java
package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Action that builds a drowned farm automatically.
 *
 * Task Parameters:
 * - "design": "aerial", "river", or "multi_layer"
 * - "size": platform size (default: 80)
 * - "layers": number of layers (for multi_layer, default: 3)
 * - "collection": "drop", "bubble", or "stream"
 */
public class BuildDrownedFarmAction extends BaseAction {
    private String designType;
    private int platformSize;
    private int layerCount;
    private String collectionType;

    private BlockPos buildLocation;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex = 0;
    private int ticksRunning = 0;
    private static final int MAX_TICKS = 60000; // 50 minutes max

    // Farm components
    private static final int WATER_DEPTH = 2;
    private static final int DROP_SHAFT_HEIGHT = 45;
    private static final int KILL_CHAMBER_OFFSET = 50;

    public BuildDrownedFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Parse parameters
        designType = task.getStringParameter("design", "aerial").toLowerCase();
        platformSize = task.getIntParameter("size", 80);
        layerCount = task.getIntParameter("layers", 3);
        collectionType = task.getStringParameter("collection", "stream").toLowerCase();

        // Validate parameters
        if (platformSize < 20 || platformSize > 128) {
            result = ActionResult.failure("Platform size must be between 20 and 128");
            return;
        }

        if (layerCount < 1 || layerCount > 8) {
            result = ActionResult.failure("Layer count must be between 1 and 8");
            return;
        }

        // Enable flying for construction
        foreman.setFlying(true);
        foreman.setInvulnerableBuilding(true);

        // Find suitable location
        buildLocation = findSuitableLocation();
        if (buildLocation == null) {
            foreman.setFlying(false);
            foreman.setInvulnerableBuilding(false);
            result = ActionResult.failure("Could not find suitable location for drowned farm");
            return;
        }

        // Generate build plan
        buildPlan = generateBuildPlan();
        if (buildPlan == null || buildPlan.isEmpty()) {
            foreman.setFlying(false);
            foreman.setInvulnerableBuilding(false);
            result = ActionResult.failure("Failed to generate build plan");
            return;
        }

        MineWrightMod.LOGGER.info("Foreman '{}' building {} drowned farm at {} with {} blocks",
            foreman.getSteveName(), designType, buildLocation, buildPlan.size());
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            foreman.setFlying(false);
            foreman.setInvulnerableBuilding(false);
            result = ActionResult.failure("Build timeout - farm too complex");
            return;
        }

        // Place multiple blocks per tick for efficiency
        int blocksPerTick = 5;

        for (int i = 0; i < blocksPerTick; i++) {
            if (currentBlockIndex >= buildPlan.size()) {
                // Build complete
                foreman.setFlying(false);
                foreman.setInvulnerableBuilding(false);
                result = ActionResult.success("Drowned farm complete! " +
                    designType + " design, " + platformSize + "x" + platformSize +
                    (layerCount > 1 ? ", " + layerCount + " layers" : ""));
                return;
            }

            placeNextBlock();
        }

        // Log progress periodically
        if (ticksRunning % 100 == 0) {
            int progress = (currentBlockIndex * 100) / buildPlan.size();
            MineWrightMod.LOGGER.info("Drowned farm progress: {}%", progress);
        }
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build " + designType + " drowned farm (" + currentBlockIndex + "/" + buildPlan.size() + " blocks)";
    }

    /**
     * Place the next block in the build plan
     */
    private void placeNextBlock() {
        BlockPlacement placement = buildPlan.get(currentBlockIndex);
        BlockPos pos = placement.pos;
        BlockState blockState = placement.block.defaultBlockState();

        // Teleport if too far
        double distance = Math.sqrt(foreman.blockPosition().distSqr(pos));
        if (distance > 10) {
            foreman.teleportTo(pos.getX() + 2.0, pos.getY(), pos.getZ() + 2.0);
        }

        // Look at the block
        foreman.getLookControl().setLookAt(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
        );

        // Place the block
        foreman.level().setBlock(pos, blockState, 3);
        currentBlockIndex++;
    }

    /**
     * Find a suitable location for the drowned farm
     */
    private BlockPos findSuitableLocation() {
        // Check if we're in an ocean biome
        BlockPos playerPos = foreman.blockPosition();

        if (designType.equals("aerial")) {
            // For aerial farms, we need ocean biome
            // Find ocean within 100 blocks
            for (int radius = 10; radius <= 100; radius += 10) {
                for (int angle = 0; angle < 360; angle += 30) {
                    double radians = Math.toRadians(angle);
                    int x = (int) (playerPos.getX() + radius * Math.cos(radians));
                    int z = (int) (playerPos.getZ() + radius * Math.sin(radians));

                    BlockPos testPos = new BlockPos(x, 62, z);
                    if (isOceanBiome(testPos)) {
                        MineWrightMod.LOGGER.info("Found ocean biome at {}", testPos);
                        // Build spawning platform at Y=50
                        return new BlockPos(x - platformSize/2, 50, z - platformSize/2);
                    }
                }
            }

            // If no ocean found, use current location (may be less efficient)
            MineWrightMod.LOGGER.warn("No ocean biome found, building at current location");
            return new BlockPos(
                playerPos.getX() - platformSize/2,
                50,
                playerPos.getZ() - platformSize/2
            );
        } else {
            // For river farms, just use current location
            return playerPos;
        }
    }

    /**
     * Check if position is in an ocean biome
     */
    private boolean isOceanBiome(BlockPos pos) {
        var biome = foreman.level().getBiome(pos);
        String biomeName = biome.unwrapKey().get().location().toString();
        return biomeName.contains("ocean");
    }

    /**
     * Generate the complete build plan based on design type
     */
    private List<BlockPlacement> generateBuildPlan() {
        return switch (designType) {
            case "aerial" -> buildAerialFarm();
            case "river" -> buildRiverFarm();
            case "multi_layer" -> buildMultiLayerFarm();
            default -> buildAerialFarm(); // Default to aerial
        };
    }

    /**
     * Build plan for aerial platform farm
     */
    private List<BlockPlacement> buildAerialFarm() {
        List<BlockPlacement> plan = new ArrayList<>();
        int halfSize = platformSize / 2;
        int centerY = buildLocation.getY();
        int centerX = buildLocation.getX() + halfSize;
        int centerZ = buildLocation.getZ() + halfSize;

        // 1. Spawning platform (solid blocks with water on top)
        for (int x = 0; x < platformSize; x++) {
            for (int z = 0; z < platformSize; z++) {
                BlockPos basePos = buildLocation.offset(x, 0, z);

                // Solid base
                plan.add(new BlockPlacement(basePos, Blocks.STONE));

                // Water layer 1
                plan.add(new BlockPlacement(basePos.above(1),
                    Blocks.WATER.defaultBlockState()
                        .setValue(FluidBlock.LEVEL, 0)));

                // Water layer 2
                plan.add(new BlockPlacement(basePos.above(2),
                    Blocks.WATER.defaultBlockState()
                        .setValue(FluidBlock.LEVEL, 0)));

                // Add holes every 4 blocks for drowned to fall through
                if (x % 4 == 0 && z % 4 == 0) {
                    // Replace solid with air (hole)
                    plan.remove(plan.size() - 3); // Remove stone
                    plan.add(new BlockPlacement(basePos, Blocks.AIR));

                    // Add water funnel below
                    plan.add(new BlockPlacement(basePos.below(1), Blocks.WATER));
                    plan.add(new BlockPlacement(basePos.below(2), Blocks.WATER));
                }
            }
        }

        // 2. Collection shaft (central column)
        BlockPos shaftTop = new BlockPos(centerX, centerY + KILL_CHAMBER_OFFSET, centerZ);
        for (int y = 3; y < DROP_SHAFT_HEIGHT; y++) {
            BlockPos shaftPos = new BlockPos(centerX, centerY + y, centerZ);
            plan.add(new BlockPlacement(shaftPos, Blocks.AIR));

            // Shaft walls (glass)
            plan.add(new BlockPlacement(shaftPos.east(), Blocks.GLASS));
            plan.add(new BlockPlacement(shaftPos.west(), Blocks.GLASS));
            plan.add(new BlockPlacement(shaftPos.south(), Blocks.GLASS));
            plan.add(new BlockPlacement(shaftPos.north(), Blocks.GLASS));
        }

        // 3. Killing chamber
        BlockPos killFloor = shaftTop.below(2);
        plan.add(new BlockPlacement(killFloor, Blocks.HOPPER));
        plan.add(new BlockPlacement(killFloor.below(), Blocks.CHEST));

        // 4. AFK platform
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos afkPos = shaftTop.above(3).offset(dx, 0, dz);
                plan.add(new BlockPlacement(afkPos, Blocks.OAK_PLANKS));
            }
        }

        // 5. Collection system (water streams)
        if (collectionType.equals("stream")) {
            buildWaterStreamCollection(plan, centerX, centerY, centerZ);
        }

        return plan;
    }

    /**
     * Build plan for river conversion farm
     */
    private List<BlockPlacement> buildRiverFarm() {
        List<BlockPlacement> plan = new ArrayList<>();

        // Simpler design for river farms
        // 1. Water conversion chamber (9x9x9)
        int chamberSize = 9;
        for (int x = 0; x < chamberSize; x++) {
            for (int y = 0; y < chamberSize; y++) {
                for (int z = 0; z < chamberSize; z++) {
                    BlockPos pos = buildLocation.offset(x, y, z);

                    // Walls
                    if (x == 0 || x == chamberSize-1 ||
                        y == 0 || y == chamberSize-1 ||
                        z == 0 || z == chamberSize-1) {
                        plan.add(new BlockPlacement(pos, Blocks.STONE_BRICKS));
                    }
                    // Water
                    else if (y < 3) {
                        plan.add(new BlockPlacement(pos, Blocks.WATER));
                    }
                    // Air above
                    else {
                        plan.add(new BlockPlacement(pos, Blocks.AIR));
                    }
                }
            }
        }

        // 2. Collection tunnel
        BlockPos tunnelStart = buildLocation.offset(chamberSize/2, 1, chamberSize);
        for (int z = 0; z < 20; z++) {
            BlockPos pos = tunnelStart.offset(0, 0, z);
            plan.add(new BlockPlacement(pos, Blocks.WATER));
        }

        // 3. Hopper collection
        BlockPos collectionPoint = tunnelStart.offset(0, 0, 20);
        plan.add(new BlockPlacement(collectionPoint.below(), Blocks.HOPPER));
        plan.add(new BlockPlacement(collectionPoint.below(2), Blocks.CHEST));

        return plan;
    }

    /**
     * Build plan for multi-layer farm
     */
    private List<BlockPlacement> buildMultiLayerFarm() {
        List<BlockPlacement> plan = new ArrayList<>();
        int layerSpacing = 5;

        // Build each layer
        for (int layer = 0; layer < layerCount; layer++) {
            int yOffset = layer * layerSpacing;
            BlockPos layerPos = buildLocation.offset(0, -yOffset, 0);

            // Each layer is a smaller version of aerial farm
            for (int x = 0; x < platformSize; x++) {
                for (int z = 0; z < platformSize; z++) {
                    BlockPos pos = layerPos.offset(x, 0, z);

                    // Solid base with holes
                    if (x % 4 == 0 && z % 4 == 0) {
                        plan.add(new BlockPlacement(pos, Blocks.AIR));
                        plan.add(new BlockPlacement(pos.below(1), Blocks.WATER));
                    } else {
                        plan.add(new BlockPlacement(pos, Blocks.STONE));
                        plan.add(new BlockPlacement(pos.above(1), Blocks.WATER));
                        plan.add(new BlockPlacement(pos.above(2), Blocks.WATER));
                    }
                }
            }
        }

        // Central collection shaft (goes through all layers)
        int centerX = buildLocation.getX() + platformSize/2;
        int centerZ = buildLocation.getZ() + platformSize/2;

        for (int y = -layerCount * layerSpacing; y < KILL_CHAMBER_OFFSET; y++) {
            BlockPos pos = new BlockPos(centerX, buildLocation.getY() + y, centerZ);
            plan.add(new BlockPlacement(pos, Blocks.AIR));

            // Walls
            plan.add(new BlockPlacement(pos.east(), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.west(), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.south(), Blocks.GLASS));
            plan.add(new BlockPlacement(pos.north(), Blocks.GLASS));
        }

        // Killing chamber (same as aerial)
        BlockPos shaftTop = new BlockPos(centerX, buildLocation.getY() + KILL_CHAMBER_OFFSET, centerZ);
        BlockPos killFloor = shaftTop.below(2);
        plan.add(new BlockPlacement(killFloor, Blocks.HOPPER));
        plan.add(new BlockPlacement(killFloor.below(), Blocks.CHEST));

        return plan;
    }

    /**
     * Add water stream collection system to build plan
     */
    private void buildWaterStreamCollection(List<BlockPlacement> plan,
            int centerX, int centerY, int centerZ) {

        // Create 4 water streams flowing to center
        int streamLength = platformSize / 2;

        for (int i = 0; i < 4; i++) {
            for (int dist = 2; dist < streamLength; dist++) {
                BlockPos streamPos;

                switch (i) {
                    case 0: streamPos = new BlockPos(centerX + dist, centerY, centerZ); break;
                    case 1: streamPos = new BlockPos(centerX - dist, centerY, centerZ); break;
                    case 2: streamPos = new BlockPos(centerX, centerY, centerZ + dist); break;
                    case 3: streamPos = new BlockPos(centerX, centerY, centerZ - dist); break;
                    default: continue;
                }

                // Water source
                plan.add(new BlockPlacement(streamPos, Blocks.WATER));
                plan.add(new BlockPlacement(streamPos.above(1), Blocks.WATER));
            }
        }

        // Hopper array at bottom
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos hopperPos = new BlockPos(
                    centerX + dx,
                    centerY + DROP_SHAFT_HEIGHT - 5,
                    centerZ + dz
                );
                plan.add(new BlockPlacement(hopperPos, Blocks.HOPPER));
                plan.add(new BlockPlacement(hopperPos.below(), Blocks.CHEST));
            }
        }
    }

    /**
     * Helper class for block placement
     */
    private static class BlockPlacement {
        final BlockPos pos;
        final BlockState block;

        BlockPlacement(BlockPos pos, Block block) {
            this.pos = pos;
            this.block = block.defaultBlockState();
        }

        BlockPlacement(BlockPos pos, BlockState block) {
            this.pos = pos;
            this.block = block;
        }
    }
}
```

---

### Register the Action

Update `CoreActionsPlugin.java` to register the new action:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Mob farms
    registry.register("build_drowned_farm",
        (minewright, task, ctx) -> new BuildDrownedFarmAction(minewright, task),
        priority, PLUGIN_ID);

    LOGGER.info("CoreActionsPlugin loaded {} actions", registry.getActionCount());
}
```

---

### Update PromptBuilder

Add drowned farm capabilities to the LLM prompt:

```java
// In PromptBuilder.java, add to buildActionDescriptions()

private String buildActionDescriptions() {
    return """
        ...
        build_drowned_farm: Build an automated drowned farm
          - design: "aerial" (best for tridents), "river" (simple), "multi_layer" (maximum efficiency)
          - size: Platform size in blocks (20-128, default 80)
          - layers: Number of layers for multi_layer (1-8, default 3)
          - collection: "drop", "bubble", or "stream" (default "stream")

        Examples:
        - "Build an aerial drowned farm in the ocean"
        - "Make a multi-layer drowned farm with 4 layers"
        - "Build a river conversion farm for nautilus shells"
        """;
}
```

---

## Implementation Roadmap

### Phase 1: Core Structure (Week 1)

**Tasks:**
1. Create `BuildDrownedFarmAction` class
2. Implement basic aerial platform design
3. Add registration in `CoreActionsPlugin`
4. Test spawning platform generation

**Success Criteria:**
- Foreman can build basic spawning platform
- Platform correctly places water blocks
- Proper hole pattern for drowned to fall

**Commands to Test:**
```
/minewright spawn Builder
K: "Build an aerial drowned farm"
```

---

### Phase 2: Collection System (Week 2)

**Tasks:**
1. Implement drop shaft construction
2. Add killing chamber with hoppers
3. Create water stream collection
4. Test item collection

**Success Criteria:**
- Items properly collected in chests
- Drowned fall correctly to killing chamber
- Non-lethal drop height maintained

**Commands to Test:**
```
K: "Build a drowned farm with water stream collection"
```

---

### Phase 3: Multiple Designs (Week 3)

**Tasks:**
1. Implement river conversion farm
2. Add multi-layer farm support
3. Create bubble column collection option
4. Biome detection for optimal placement

**Success Criteria:**
- All three farm types buildable
- Multi-layer farms stack correctly
- Ocean biome detection works

**Commands to Test:**
```
K: "Build a river drowned farm"
K: "Make a 4-layer drowned farm"
K: "Find an ocean and build a trident farm"
```

---

### Phase 4: Optimization & Automation (Week 4)

**Tasks:**
1. Add spawn rate monitoring
2. Implement AFK timer/action
3. Create resource collection automation
4. Add farm status reporting

**Success Criteria:**
- Foreman can monitor farm performance
- Automated resource collection
- Status updates on farm efficiency

**Commands to Test:**
```
K: "Check drowned farm status"
K: "Collect items from drowned farm"
```

---

### Phase 5: Advanced Features (Optional - Week 5+)

**Tasks:**
1. Nether portal bypass system
2. Sorting system for drops
3. Multi-farm coordination
4. Resource counting and goal tracking

**Success Criteria:**
- Portal-optimized farms
- Automatic sorting of tridents/shells/copper
- Multiple farms run simultaneously

---

## Usage Examples

### Example 1: Basic Trident Farm

```
Player: /minewright spawn Farmer
Player: K: "Build an aerial drowned farm in the ocean for tridents"

Foreman: "Searching for ocean biome... Found at (1234, 50, -567)"
Foreman: "Building 80x80 aerial drowned farm with water stream collection"
[Foreman begins construction]
```

### Example 2: Maximum Efficiency Farm

```
Player: K: "Build a 4-layer drowned farm for maximum production"

Foreman: "Constructing multi-layer drowned farm"
Foreman: "Platform size: 80x80, Layers: 4"
Foreman: "Expected production: ~2,200 drowned/hour"
[Construction begins]
```

### Example 3: Simple River Farm

```
Player: K: "Make a simple drowned farm near the river"

Foreman: "Building river conversion farm at current location"
Foreman: "Design: zombie conversion chamber with water flow"
[Builds simpler design]
```

### Example 4: Collection-Only Action

```
Player: K: "Go collect items from the drowned farm"

Foreman: "Traveling to drowned farm..."
[Foreman pathfinds to farm]
Foreman: "Collected: 3 copper, 12 rotten flesh, 1 nautilus shell"
Foreman: "Depositing items in storage chest"
```

---

## Performance Metrics

### Expected Spawn Rates

| Farm Type | Platform Size | Layers | Drowned/Hour | Trident/Hour |
|-----------|---------------|--------|--------------|--------------|
| Basic Aerial | 80x80 | 1 | ~900 | ~34 |
| Multi-Layer | 80x80 | 3 | ~2,200 | ~82 |
| Maximum | 128x128 | 4 | ~4,500 | ~169 |

### Resource Estimates

**Per Hour (Optimal 3-Layer Farm):**
- Tridents: ~82 (with Looting III)
- Nautilus Shells: ~180
- Copper Ingots: ~330
- Experience: ~22,000 XP
- Rotten Flesh: ~2,000+

---

## Troubleshooting

### Issue: No Drowned Spawning

**Causes:**
- Light level too high (must be 0)
- Wrong Y-level for ocean (must be Y<58)
- Player too close/far (24-128 blocks required)
- Other mobs consuming spawn cap

**Solutions:**
```
/minewright spawn Builder
K: "Check light levels on the drowned farm platform"
K: "Light up all caves within 128 blocks"
```

---

### Issue: Low Trident Drop Rate

**Causes:**
- Not using Looting III
- Farm too small
- Converted zombies (no tridents)

**Solutions:**
- Ensure natural spawning (no zombie conversion)
- Use Looting III weapon
- Increase platform size

---

### Issue: Drowneds Not Falling

**Causes:**
- Holes not placed correctly
- Water depth wrong
- Flow direction incorrect

**Solutions:**
```
K: "Check the water flow on the drowned farm"
K: "Repair any blocked holes in the platform"
```

---

## Material Requirements

### Aerial Farm (80x80, 1 layer)

| Material | Quantity | Notes |
|----------|----------|-------|
| Stone / Cobblestone | ~6,400 | Platform base |
| Glass | ~200 | Shaft walls |
| Water Buckets | ~100 | Fill platform |
| Hoppers | 4-9 | Collection system |
| Chests | 4-9 | Item storage |
| Signs | ~50 | Water control (optional) |
| Oak Planks | 25 | AFK platform |

### Multi-Layer Farm (80x80, 3 layers)

| Material | Quantity | Notes |
|----------|----------|-------|
| Stone / Cobblestone | ~19,200 | 3 platforms |
| Glass | ~600 | 3 shafts |
| Water Buckets | ~300 | 3 layers |
| Hoppers | 9 | 3x3 array |
| Chests | 9 | 3x3 array |

---

## Future Enhancements

### 1. Nether Portal Bypass
- Push drowned through Nether portal
- Bypass overworld mob cap
- Dramatically increase spawn rates

### 2. Automatic Sorting
- Separate tridents, shells, copper
- Filter into specific chests
- Auto-smelt copper with furnaces

### 3. Multi-Farm Network
- Coordinate multiple farms
- Load balancing between Foremen
- Centralized resource collection

### 4. XP Collection
- XP storage system
- Auto-enchanting integration
- Mending repair station

### 5. Monitoring Dashboard
- Real-time spawn rate tracking
- Drop statistics
- Efficiency reports

---

## References

### External Resources

- [Minecraft Wiki - Drowned](https://minecraft.fandom.com/wiki/Drowned)
- [Minecraft Wiki - Drowned Farming Tutorial](https://minecraft.fandom.com/wiki/Tutorials/Drowned_farming)
- [Minecraft Wiki - Bubble Columns](https://minecraft.fandom.com/wiki/Bubble_Column)
- [Bilibili - MCBE Drowned Farm Tutorial](https://www.bilibili.com/video/BV1a54y1f7VW)

### MineWright Documentation

- `docs/MOB_FARM_COORDINATION.md` - Multi-farm management
- `docs/WATER_MANAGEMENT.md` - Water handling in builds
- `docs/ACTION_SYSTEM_IMPROVEMENTS.md` - Action system architecture

### Code References

- `src/main/java/com/minewright/action/actions/BuildStructureAction.java` - Building system
- `src/main/java/com/minewright/action/actions/CombatAction.java` - Combat for killing
- `src/main/java/com/minewright/structure/StructureGenerators.java` - Procedural generation

---

## Changelog

**v1.0.0 (2025-02-27)**
- Initial drowned farm design document
- Three farm designs (aerial, river, multi-layer)
- Collection system options
- MineWright integration code
- Implementation roadmap

---

**Document Status:** Ready for Implementation
**Last Updated:** 2025-02-27
**Maintained By:** MineWright Development Team
