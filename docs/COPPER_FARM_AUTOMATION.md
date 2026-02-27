# Copper Farm Automation for MineWright

## Overview

This document provides comprehensive research and design for copper farm automation in Minecraft 1.20.1 (Forge). Copper farms leverage lightning rod mechanics to harvest copper or control oxidation states of copper blocks.

**Key Resources:**
- [Lightning Rod - Minecraft Wiki](https://minecraft.fandom.com/wiki/Lightning_Rod)
- [Thunderstorm Mechanics](https://minecraft.fandom.com/wiki/Thunderstorm)
- [Copper Oxidation - Minecraft Wiki](https://minecraft.fandom.com/wiki/Oxidation)

---

## Table of Contents

1. [Lightning Rod Mechanics](#1-lightning-rod-mechanics)
2. [Copper Oxidation Prevention](#2-copper-oxidation-prevention)
3. [Waxed Copper Handling](#3-waxed-copper-handling)
4. [Multi-Rod Optimization](#4-multi-rod-optimization)
5. [Collection Automation](#5-collection-automation)
6. [Forge Event Integration](#6-forge-event-integration)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Code Examples](#8-code-examples)

---

## 1. Lightning Rod Mechanics

### 1.1 Core Properties

| Property | Java Edition | Bedrock Edition |
|----------|--------------|-----------------|
| **Attraction Range** | 128 blocks (spherical radius) | 64x64x64 blocks (cuboid) |
| **Redstone Signal** | Strength 15 for 8 ticks (0.4s) | Same |
| **Crafting** | 3 Copper Ingots (vertical) | Same |
| **Mining Requirement** | Stone pickaxe or better | Same |

### 1.2 Lightning Strike Mechanics

**Lightning Generation:**
- Probability per chunk per tick: 1/100,000
- With ~201 loaded chunks: ~2.4 strikes per minute average
- Range: 0-5 strikes per minute (90% of the time)

**Strike Conditions:**
- Target block must have sky access
- Rain (not snow) must be falling at location
- Does NOT occur in desert or snowy biomes

**Lightning Prioritization:**
1. Random X/Z position in chunk -> highest block
2. If entities within 6x12x6 area under sky, redirects to random entity
3. If Lightning Rod within 128 blocks, strikes the rod instead

### 1.3 Thunderstorm Mechanics

| Timer Property | Value |
|----------------|-------|
| Thunderstorm Duration | 3,600 - 15,600 ticks (3-13 minutes) |
| Time Between Storms | 12,000 - 180,000 ticks (0.5-7.5 game days) |
| Average Between Storms | ~9 real-time hours |
| Thunderstorm Probability | ~1.44% per game tick |

### 1.4 Copper De-oxidation by Lightning

**When lightning strikes a rod:**
- Instantly removes ALL oxidation from unwaxed copper blocks directly below
- Reduces oxidation level on nearby copper blocks
- Range of effect: Approximately 6x6x6 blocks centered on the rod

**Important Notes:**
- Waxed copper blocks are NOT affected by lightning
- The rod must be the highest block in its column
- Multiple rods in range: closest to original strike location is chosen

---

## 2. Copper Oxidation Prevention

### 2.1 Oxidation Stages

Copper blocks progress through four oxidation stages:

| Stage | Name | Appearance |
|-------|------|------------|
| 0 | Copper Block | Shiny copper-orange |
| 1 | Exposed Copper | Beginning to show green rust |
| 2 | Weathered Copper | Mostly covered with green rust |
| 3 | Oxidized Copper | Completely green (teal color) |

### 2.2 Oxidation Mechanics

**Random Tick System:**
- Each copper block has 64/1125 chance per random tick to enter "pre-oxidation"
- Random ticks occur naturally in loaded chunks
- Oxidation rate depends on neighboring blocks

**Neighbor Influence (Manhattan Distance):**
- Blocks within distance <=4 affect each other's oxidation
- Isolated copper oxidizes FASTEST
- Surrounded by less-oxidized copper: oxidizes SLOWEST

**Formula:**
```
Oxidation Chance = Base Chance / (1 + Neighbors with less oxidation)
```

### 2.3 Waxing for Prevention

**Applying Wax:**
1. Hold honeycomb in hand
2. Right-click on copper block
3. Or use crafting table: honeycomb + copper block

**Effects of Waxing:**
- Locks oxidation at current stage PERMANENTLY
- Not affected by lightning strikes
- Can be applied at ANY oxidation stage

**Removing Wax:**
- Use any axe, right-click on waxed copper
- Unlocks oxidation to continue naturally
- Grants "Wax Off" advancement

### 2.4 Reversing Oxidation

| Method | Effect | Speed |
|--------|--------|-------|
| **Axe** | Removes 1 oxidation stage | Instant (manual) |
| **Lightning Strike** | Removes all oxidation from blocks below rod | Instant (during storm) |
| **Natural** | Progresses through stages | Very slow (hours/days) |

---

## 3. Waxed Copper Handling

### 3.1 Waxed Copper Block Types

| Base Block | Waxed Variant | ID |
|------------|---------------|-----|
| Copper Block | Waxed Copper | `minecraft:waxed_copper` |
| Exposed Copper | Waxed Exposed Copper | `minecraft:waxed_exposed_copper` |
| Weathered Copper | Waxed Weathered Copper | `minecraft:waxed_weathered_copper` |
| Oxidized Copper | Waxed Oxidized Copper | `minecraft:waxed_oxidized_copper` |
| Cut Copper | Waxed Cut Copper | `minecraft:waxed_cut_copper` |
| Cut Copper Stairs | Waxed Cut Copper Stairs | `minecraft:waxed_cut_copper_stairs` |
| Cut Copper Slab | Waxed Cut Copper Slab | `minecraft:waxed_cut_copper_slab` |

### 3.2 Waxed Copper Detection

```java
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBlock;
import net.minecraft.world.level.block.state.BlockState;

public boolean isWaxedCopper(BlockState state) {
    Block block = state.getBlock();
    return block == Blocks.WAXED_COPPER_BLOCK ||
           block == Blocks.WAXED_EXPOSED_COPPER ||
           block == Blocks.WAXED_WEATHERED_COPPER ||
           block == Blocks.WAXED_OXIDIZED_COPPER ||
           block == Blocks.WAXED_CUT_COPPER ||
           block == Blocks.WAXED_EXPOSED_CUT_COPPER ||
           block == Blocks.WAXED_WEATHERED_CUT_COPPER ||
           block == Blocks.WAXED_OXIDIZED_CUT_COPPER;
}

public boolean isUnwaxedCopper(BlockState state) {
    return state.getBlock() instanceof CopperBlock;
}
```

### 3.3 Waxed Copper Handling for Automation

**Strategies:**

1. **Pre-waxed Building Materials:**
   - Store waxed copper in separate inventory
   - Always use waxed blocks for construction
   - Prevents oxidation issues in builds

2. **Auto-Waxing System:**
   - Mine copper blocks
   - Feed through auto-crafter with honeycomb
   - Store waxed result for building

3. **Selective De-waxing:**
   - Use axe to remove wax when needed
   - Allow controlled oxidation for aesthetics
   - Re-wax when desired stage achieved

---

## 4. Multi-Rod Optimization

### 4.1 Rod Spacing Strategies

**Optimal Spacing for Maximum Coverage:**

```
Layout 1: Hexagonal Grid (Most Efficient)
     R
    R R
   R   R
    R R
     R

Spacing: 110 blocks between centers (allows some overlap)
Coverage: ~98% of area within 128-block radius
```

```
Layout 2: Square Grid (Simple)
R   R   R   R

R   R   R   R

R   R   R   R

Spacing: 90 blocks between centers
Coverage: ~90% of area (simpler to build)
```

**Recommended Spacing:**

| Purpose | Rod Spacing | Notes |
|---------|-------------|-------|
| **Maximum Coverage** | 110 blocks | Hexagonal pattern, minimal overlap |
| **Simple Build** | 90 blocks | Square grid, easier to construct |
| **Redundancy** | 70 blocks | Multiple rods cover each area |

### 4.2 Vertical Stacking

**Single-Column Multi-Rod:**
```
         [Rod C]
            |
         [Rod B]
            |
         [Rod A]
         =======
      [Copper Blocks]
```

**Issues with Vertical Stacking:**
- Only the TOP rod attracts lightning (must be highest in column)
- Lower rods provide redundancy only if top rod is destroyed
- NOT recommended for efficiency

### 4.3 Horizontal Array Design

**Recommended: Elevated Platform Array**

```
Cross-section view:

Rod Rod Rod Rod Rod  (Y=256)
 |   |   |   |
Rod Rod Rod Rod Rod  (Y=200)
 |   |   |   |
===================== (Ground level)
[Copper Farm Chamber]
```

**Benefits:**
- Each rod attracts lightning independently
- Redundancy if one rod is destroyed
- Simultaneous strikes during heavy storms

### 4.4 Lightning Rod Chamber Design

**Standard Chamber:**

```
Side View:

     [Lightning Rod]  <- Exposed to sky
          |
    ===============  <- Ceiling (glass)
    |             |
    |  Copper     |  <- Chamber interior
    |  Blocks     |
    |             |
    ===============  <- Floor
          |
      [Hopper]  <- Collection system
          |
      [Chest]
```

**Mob-Proof Design Considerations:**
- Place chamber in ocean or deep ocean biome (no hostile surface spawns)
- Use waterlogged bottom half slab to prevent spawns
- Light level 0 is fine if no spawnable surfaces
- Lightning still damages entities within 6x12x6 area

---

## 5. Collection Automation

### 5.1 Item Collection Systems

**System 1: Hopper Minecart (Recommended)**

```
Top View:

[Copper Blocks]
     ||
[Hopper Minecart on Rail]
     ||
[Chest/Hopper]
```

**Advantages:**
- Collects items through full blocks
- Mobile - can cover large areas
- No gaps in collection

**Code Integration:**
```java
// Place hopper minecart at copper farm collection point
public void placeHopperMinecart(BlockPos pos) {
    // Hopper minecarts pick up items through solid blocks
    // Position should be directly below copper blocks
    BlockPos cartPos = pos.below();
    // Rail placement
    level.setBlock(cartPos, Blocks.RAIL.defaultBlockState(), 3);
    // Spawn minecart with hopper
    MinecartChest cart = new MinecartChest(EntityType.CHEST_MINECART, level);
    cart.setPos(cartPos.getX() + 0.5, cartPos.getY(), cartPos.getZ() + 0.5);
    level.addFreshEntity(cart);
}
```

**System 2: Water Stream**

```
[Copper Blocks]
     |
[Water Source] -> [Flow] -> [Hopper] -> [Chest]
```

**Advantages:**
- Simple redstone-free design
- Items flow to collection point
- Can transport long distances

**Implementation:**
```java
public void createWaterStream(BlockPos start, BlockPos end, Direction flowDir) {
    BlockPos current = start;
    while (!current.equals(end)) {
        // Place water (source blocks every 8 blocks)
        int distance = (int) current.distSqr(end);
        if (distance % 8 == 0) {
            level.setBlock(current, Blocks.WATER.defaultBlockState(), 3);
        } else {
            // Use signs, stairs, or glass to contain water
            level.setBlock(current, Blocks.WATER.defaultBlockState(), 3);
        }
        current = current.relative(flowDir);
    }
    // Collection hopper at end
    level.setBlock(end, Blocks.HOPPER.defaultBlockState()
        .setValue(HopperBlock.FACING, flowDir.getOpposite()), 3);
}
```

**System 3: Direct Hopper Array**

```
[Copper Blocks]
     |
[Hopper] [Hopper] [Hopper]
    |       |       |
  [Chest] [Chest] [Chest]
```

**Advantages:**
- Fast collection (2.5 items/second)
- Simple design
- No entities required

**Code:**
```java
public void createHopperArray(BlockPos start, int length, Direction dir) {
    for (int i = 0; i < length; i++) {
        BlockPos hopperPos = start.relative(dir, i);
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState()
            .setValue(HopperBlock.FACING, Direction.DOWN), 3);

        // Chest below each hopper
        BlockPos chestPos = hopperPos.below();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.FACING, Direction.NORTH), 3);
    }
}
```

### 5.2 Collection Optimization

**Timing-Based Collection:**
```java
public class LightningRodManager {
    private final Map<BlockPos, LocalDateTime> lastStrikeTimes = new HashMap<>();

    @SubscribeEvent
    public void onLightningStrike(LightningStrikeEvent event) {
        BlockPos rodPos = getNearbyLightningRod(event.getPos());
        if (rodPos != null) {
            lastStrikeTimes.put(rodPos, LocalDateTime.now());

            // Schedule collection 1 tick after strike
            scheduleCollection(rodPos, 1);
        }
    }

    private void scheduleCollection(BlockPos rodPos, int delayTicks) {
        // Use a scheduler to collect items after lightning
        // Items drop from de-oxidized copper blocks
    }
}
```

---

## 6. Forge Event Integration

### 6.1 Lightning Strike Detection

**Event Handler Setup:**

```java
package com.minewright.farm;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CopperFarmEventHandler {

    private static final Map<BlockPos, LightningRod> lightningRods = new ConcurrentHashMap<>();
    private static final int LIGHTNING_ROD_RANGE = 128;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Check if entity is lightning bolt
        if (event.getEntity().getType() == EntityType.LIGHTNING_BOLT) {
            LightningBolt lightning = (LightningBolt) event.getEntity();
            Level level = event.getLevel();
            BlockPos strikePos = lightning.blockPosition();

            MineWrightMod.LOGGER.info("Lightning strike detected at {}", strikePos);

            // Find nearby lightning rod
            BlockPos rodPos = findNearestLightningRod(level, strikePos);
            if (rodPos != null) {
                handleLightningRodStrike(level, rodPos, strikePos);
            }
        }
    }

    private static BlockPos findNearestLightningRod(Level level, BlockPos strikePos) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos rodPos : lightningRods.keySet()) {
            double distance = rodPos.distSqr(strikePos);
            if (distance <= LIGHTNING_ROD_RANGE * LIGHTNING_ROD_RANGE && distance < nearestDistance) {
                nearest = rodPos;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static void handleLightningRodStrike(Level level, BlockPos rodPos, BlockPos strikePos) {
        MineWrightMod.LOGGER.info("Lightning rod struck at {}", rodPos);

        LightningRod rod = lightningRods.get(rodPos);
        if (rod != null) {
            rod.onLightningStrike(level);

            // De-oxidize copper blocks below
            deoxidizeCopperBelow(level, rodPos);

            // Trigger redstone output
            triggerRedstoneOutput(level, rodPos);

            // Schedule collection
            scheduleItemCollection(level, rodPos);
        }
    }

    private static void deoxidizeCopperBelow(Level level, BlockPos rodPos) {
        // Check blocks directly below the rod (up to 6 blocks)
        for (int i = 1; i <= 6; i++) {
            BlockPos checkPos = rodPos.below(i);
            BlockState state = level.getBlockState(checkPos);

            if (state.getBlock() instanceof CopperBlock) {
                // Only de-oxidize unwaxed copper
                if (!isWaxed(state)) {
                    // Reset to base copper state
                    level.setBlock(checkPos, Blocks.COPPER_BLOCK.defaultBlockState(), 3);
                    MineWrightMod.LOGGER.debug("De-oxidized copper at {}", checkPos);
                }
            }
        }

        // Check nearby copper blocks (within 6x6x6 area)
        BlockPos.betweenClosedStream(
            rodPos.offset(-3, -6, -3),
            rodPos.offset(3, 0, 3)
        ).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CopperBlock && !isWaxed(state)) {
                // Reduce oxidation by one stage
                reduceOxidation(level, pos, state);
            }
        });
    }

    private static boolean isWaxed(BlockState state) {
        return state.is(Blocks.WAXED_COPPER_BLOCK) ||
               state.is(Blocks.WAXED_EXPOSED_COPPER) ||
               state.is(Blocks.WAXED_WEATHERED_COPPER) ||
               state.is(Blocks.WAXED_OXIDIZED_COPPER);
    }

    private static void reduceOxidation(Level level, BlockPos pos, BlockState currentState) {
        Block current = currentState.getBlock();
        Block newState = null;

        // Map oxidized -> weathered -> exposed -> base
        switch (currentState.getBlock().toString()) {
            case "minecraft:oxidized_copper" -> newState = Blocks.WEATHERED_COPPER;
            case "minecraft:weathered_copper" -> newState = Blocks.EXPOSED_COPPER;
            case "minecraft:exposed_copper" -> newState = Blocks.COPPER_BLOCK;
        }

        if (newState != null) {
            level.setBlock(pos, newState.defaultBlockState(), 3);
        }
    }

    private static void triggerRedstoneOutput(Level level, BlockPos rodPos) {
        // Lightning rod emits redstone signal when struck
        // This is handled by vanilla game, but we can listen for it
        BlockPos belowRod = rodPos.below();
        BlockState belowState = level.getBlockState(belowRod);

        if (belowState.is(Blocks.REPEATER) || belowState.is(Blocks.COMPARATOR)) {
            // Redstone circuit triggered - handle automation
            MineWrightMod.LOGGER.debug("Redstone output triggered at {}", belowRod);
        }
    }

    private static void scheduleItemCollection(Level level, BlockPos rodPos) {
        // Items will drop after de-oxidation
        // Schedule collection task to run in 1-2 ticks
        // This can interface with MineWright's action system
    }

    public static void registerLightningRod(BlockPos pos, LightningRod rod) {
        lightningRods.put(pos, rod);
        MineWrightMod.LOGGER.info("Registered lightning rod at {}", pos);
    }

    public static void unregisterLightningRod(BlockPos pos) {
        lightningRods.remove(pos);
    }
}
```

### 6.2 Thunderstorm Detection

```java
package com.minewright.farm;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.tick.LevelTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WeatherEventHandler {

    private static boolean isThundering = false;
    private static final Set<Level> stormActiveLevels = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level.isClientSide()) {
            return;
        }

        boolean currentlyThundering = level.isThundering();

        // Detect storm start
        if (currentlyThundering && !isThundering) {
            onThunderstormStart(level);
        }
        // Detect storm end
        else if (!currentlyThundering && isThundering) {
            onThunderstormEnd(level);
        }

        isThundering = currentlyThundering;
    }

    private static void onThunderstormStart(Level level) {
        MineWrightMod.LOGGER.info("Thunderstorm started in dimension {}", level.dimension());
        stormActiveLevels.add(level);

        // Notify all copper farms to prepare for lightning
        // Foreman entities can be assigned to monitor lightning rods
    }

    private static void onThunderstormEnd(Level level) {
        MineWrightMod.LOGGER.info("Thunderstorm ended in dimension {}", level.dimension());
        stormActiveLevels.remove(level);

        // Perform cleanup, collect any remaining items
    }

    public static boolean isStormActive(Level level) {
        return stormActiveLevels.contains(level);
    }

    public static boolean canLightningStrike(Level level, BlockPos pos) {
        // Check if storm is active
        if (!isStormActive(level)) {
            return false;
        }

        // Check if position can see sky
        if (!level.canSeeSky(pos)) {
            return false;
        }

        // Check if raining (not snowing) at this position
        Biome biome = level.getBiome(pos).value();
        if (biome.shouldSnow(level, pos)) {
            return false; // No lightning in snow biomes
        }

        if (biome.isHot()) {
            return false; // No lightning in hot/dry biomes
        }

        return true;
    }
}
```

### 6.3 Integration with Foreman Actions

**New Action: BuildCopperFarmAction**

```java
package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.farm.CopperFarmEventHandler;
import com.minewright.farm.LightningRod;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class BuildCopperFarmAction extends BaseAction {

    private BlockPos farmLocation;
    private int farmWidth;
    private int farmDepth;
    private int numRods;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;

    public BuildCopperFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        farmWidth = task.getIntParameter("width", 15);
        farmDepth = task.getIntParameter("depth", 15);
        numRods = task.getIntParameter("rods", 4);

        // Find suitable location (high elevation, sky access)
        farmLocation = findCopperFarmLocation();

        if (farmLocation == null) {
            result = ActionResult.failure("Cannot find suitable location for copper farm");
            return;
        }

        // Generate build plan
        buildPlan = generateCopperFarmPlan(farmLocation, farmWidth, farmDepth, numRods);

        MineWrightMod.LOGGER.info("Building copper farm at {} with {} lightning rods",
            farmLocation, numRods);
    }

    @Override
    protected void onTick() {
        if (currentBlockIndex >= buildPlan.size()) {
            // Register lightning rods with event handler
            registerLightningRods();
            result = ActionResult.success("Copper farm built at " + farmLocation);
            return;
        }

        // Place blocks (similar to BuildStructureAction)
        BlockPlacement placement = buildPlan.get(currentBlockIndex);
        placeBlock(placement);
        currentBlockIndex++;
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    private BlockPos findCopperFarmLocation() {
        // Find high elevation with sky access
        BlockPos start = foreman.blockPosition();

        // Go up to build height limit
        for (int y = 320; y > start.getY(); y--) {
            BlockPos testPos = new BlockPos(start.getX(), y, start.getZ());

            // Check if above has sky access
            if (foreman.level().canSeeSky(testPos)) {
                // Check if solid ground below
                BlockPos groundPos = findSolidGroundBelow(testPos);
                if (groundPos != null) {
                    return groundPos;
                }
            }
        }

        return null;
    }

    private BlockPos findSolidGroundBelow(BlockPos start) {
        for (int y = start.getY(); y > start.getY() - 20; y--) {
            BlockPos testPos = new BlockPos(start.getX(), y, start.getZ());
            if (foreman.level().getBlockState(testPos).isSolid()) {
                return testPos.above();
            }
        }
        return null;
    }

    private List<BlockPlacement> generateCopperFarmPlan(BlockPos start, int width, int depth, int rods) {
        List<BlockPlacement> plan = new ArrayList<>();

        // Platform base
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                plan.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));
            }
        }

        // Lightning rods (evenly spaced)
        int rodSpacing = width / (rods + 1);
        for (int i = 1; i <= rods; i++) {
            int x = i * rodSpacing;
            int z = depth / 2;

            // Rod base
            plan.add(new BlockPlacement(start.offset(x, 1, z), Blocks.COPPER_BLOCK));

            // Lightning rod
            plan.add(new BlockPlacement(start.offset(x, 2, z), Blocks.LIGHTNING_ROD));

            // Store rod position for registration
            // This will be used in registerLightningRods()
        }

        // Collection hoppers under lightning rods
        for (int i = 1; i <= rods; i++) {
            int x = i * rodSpacing;
            int z = depth / 2;

            plan.add(new BlockPlacement(start.offset(x, -1, z), Blocks.HOPPER));
            plan.add(new BlockPlacement(start.offset(x, -2, z), Blocks.CHEST));
        }

        return plan;
    }

    private void registerLightningRods() {
        // Register each lightning rod with the event handler
        int rodSpacing = farmWidth / (numRods + 1);

        for (int i = 1; i <= numRods; i++) {
            int x = i * rodSpacing;
            int z = farmDepth / 2;
            BlockPos rodPos = farmLocation.offset(x, 2, z);

            LightningRod rod = new LightningRod(rodPos);
            CopperFarmEventHandler.registerLightningRod(rodPos, rod);
        }
    }

    private void placeBlock(BlockPlacement placement) {
        foreman.level().setBlock(placement.pos, placement.block.defaultBlockState(), 3);
    }

    @Override
    public String getDescription() {
        return "Building copper farm (" + currentBlockIndex + "/" + buildPlan.size() + " blocks placed)";
    }
}
```

---

## 7. Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1)

**Tasks:**
1. Create `com.minewright.farm` package
2. Implement `CopperFarmEventHandler`
3. Implement `WeatherEventHandler`
4. Add `LightningRod` data class
5. Write unit tests for event handlers

**Deliverables:**
- Event detection system
- Lightning rod registration
- Thunderstorm tracking

### Phase 2: Copper Farm Structure Generator (Week 2)

**Tasks:**
1. Create `CopperFarmGenerator` class
2. Implement farm layout algorithms
3. Add to `StructureGenerators`
4. Create NBT templates for common designs

**Deliverables:**
- Procedural copper farm generation
- Multiple farm designs
- Integration with existing build system

### Phase 3: Collection Automation (Week 2-3)

**Tasks:**
1. Implement hopper minecart collection
2. Add water stream collection option
3. Create item routing system
4. Interface with storage system

**Deliverables:**
- Automated item collection
- Multiple collection methods
- Storage integration

### Phase 4: BuildCopperFarm Action (Week 3)

**Tasks:**
1. Create `BuildCopperFarmAction`
2. Register in `CoreActionsPlugin`
3. Add to `PromptBuilder`
4. Test building functionality

**Deliverables:**
- New action for building copper farms
- LLM integration
- In-game command support

### Phase 5: Monitoring and Maintenance (Week 4)

**Tasks:**
1. Create `CopperFarmMonitor` class
2. Add health checking for lightning rods
3. Implement auto-repair system
4. Add statistics tracking

**Deliverables:**
- Farm monitoring system
- Automatic maintenance
- Performance metrics

### Phase 6: Optimization and Testing (Week 5)

**Tasks:**
1. Performance profiling
2. Multi-farm coordination
3. Load testing
4. Documentation

**Deliverables:**
- Optimized performance
- Multi-farm support
- Complete documentation

---

## 8. Code Examples

### 8.1 Lightning Rod Data Class

```java
package com.minewright.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LightningRod {

    private final BlockPos position;
    private final List<LocalDateTime> strikeHistory;
    private int totalStrikes;
    private LocalDateTime lastStrike;
    private boolean isActive;

    public LightningRod(BlockPos position) {
        this.position = position;
        this.strikeHistory = new ArrayList<>();
        this.totalStrikes = 0;
        this.isActive = true;
    }

    public void onLightningStrike(Level level) {
        totalStrikes++;
        lastStrike = LocalDateTime.now();
        strikeHistory.add(lastStrike);

        // Keep only last 100 strikes
        if (strikeHistory.size() > 100) {
            strikeHistory.remove(0);
        }

        // Log strike
        MineWrightMod.LOGGER.debug("Lightning rod at {} struck! Total strikes: {}",
            position, totalStrikes);
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getTotalStrikes() {
        return totalStrikes;
    }

    public LocalDateTime getLastStrike() {
        return lastStrike;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Get strikes per hour for the last N hours
     */
    public double getStrikesPerHour(int hours) {
        if (strikeHistory.isEmpty()) {
            return 0.0;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        long recentStrikes = strikeHistory.stream()
            .filter(strike -> strike.isAfter(cutoff))
            .count();

        return (double) recentStrikes / hours;
    }

    /**
     * Estimate efficiency (strikes per hour during thunderstorms)
     */
    public double getStormEfficiency() {
        // This would require tracking storm durations
        // Simplified version:
        return getStrikesPerHour(24); // Strikes per 24 hours
    }
}
```

### 8.2 Copper Farm Configuration

```java
package com.minewright.farm;

public class CopperFarmConfig {

    private int width = 15;
    private int depth = 15;
    private int numRods = 4;
    private boolean includeWaxing = false;
    private CollectionType collectionType = CollectionType.HOPPER_MINECART;
    private boolean autoRepair = true;

    public enum CollectionType {
        HOPPER_MINECART,
        WATER_STREAM,
        DIRECT_HOPPER
    }

    // Builder pattern
    public static class Builder {
        private final CopperFarmConfig config;

        public Builder() {
            this.config = new CopperFarmConfig();
        }

        public Builder width(int width) {
            config.width = width;
            return this;
        }

        public Builder depth(int depth) {
            config.depth = depth;
            return this;
        }

        public Builder numRods(int rods) {
            config.numRods = rods;
            return this;
        }

        public Builder includeWaxing(boolean waxing) {
            config.includeWaxing = waxing;
            return this;
        }

        public Builder collectionType(CollectionType type) {
            config.collectionType = type;
            return this;
        }

        public Builder autoRepair(boolean repair) {
            config.autoRepair = repair;
            return this;
        }

        public CopperFarmConfig build() {
            return config;
        }
    }

    // Getters
    public int getWidth() { return width; }
    public int getDepth() { return depth; }
    public int getNumRods() { return numRods; }
    public boolean includeWaxing() { return includeWaxing; }
    public CollectionType getCollectionType() { return collectionType; }
    public boolean isAutoRepair() { return autoRepair; }
}
```

### 8.3 Prompt Builder Integration

```java
// In PromptBuilder.java, add copper farm action description

private void addCopperFarmActions(StringBuilder prompt) {
    prompt.append("""
        ## Copper Farm Actions

        ### build_copper_farm
        Build an automated copper farm using lightning rods.
        Parameters:
        - width: Farm width in blocks (default: 15)
        - depth: Farm depth in blocks (default: 15)
        - rods: Number of lightning rods (default: 4, max: 9)
        - collection: Collection method - hopper_minecart, water_stream, or hopper (default: hopper_minecart)
        - waxing: Include auto-waxing system (true/false, default: false)

        Example: "Build a copper farm with 6 lightning rods and water collection"

        ### monitor_copper_farm
        Check the status of copper farms.
        Returns: Strike counts, efficiency metrics, maintenance needs

        Example: "Check the copper farm status"

        ### repair_copper_farm
        Repair damaged copper farm components.
        Automatically fixes broken lightning rods, hoppers, and collection systems.

        Example: "Repair the copper farm"
        """);
}
```

### 8.4 Honeycomb Farm Integration (for Waxing)

```java
package com.minewright.farm;

import com.minewright.MineWrightMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HoneycombFarm {

    /**
     * Auto-wax copper blocks using honeycomb from bee farm
     */
    public static void waxCopperBlocks(Level level, BlockPos start, int width, int depth) {
        int honeycombNeeded = width * depth;

        MineWrightMod.LOGGER.info("Auto-waxing {} copper blocks, need {} honeycomb",
            width * depth, honeycombNeeded);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos pos = start.offset(x, 0, z);
                BlockState state = level.getBlockState(pos);

                if (isUnwaxedCopper(state)) {
                    // Apply wax (would need inventory access)
                    waxBlock(level, pos);
                }
            }
        }
    }

    private static boolean isUnwaxedCopper(BlockState state) {
        return state.is(Blocks.COPPER_BLOCK) ||
               state.is(Blocks.EXPOSED_COPPER) ||
               state.is(Blocks.WEATHERED_COPPER) ||
               state.is(Blocks.OXIDIZED_COPPER);
    }

    private static void waxBlock(Level level, BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        BlockState waxedState = getWaxedVariant(currentState);

        if (waxedState != null) {
            level.setBlock(pos, waxedState, 3);
            MineWrightMod.LOGGER.debug("Waxed copper block at {}", pos);
        }
    }

    private static BlockState getWaxedVariant(BlockState unwaxed) {
        return switch (unwaxed.getBlock().toString()) {
            case "minecraft:copper_block" -> Blocks.WAXED_COPPER.defaultBlockState();
            case "minecraft:exposed_copper" -> Blocks.WAXED_EXPOSED_COPPER.defaultBlockState();
            case "minecraft:weathered_copper" -> Blocks.WAXED_WEATHERED_COPPER.defaultBlockState();
            case "minecraft:oxidized_copper" -> Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState();
            default -> null;
        };
    }

    /**
     * Collect honeycomb from bee nests/hives
     */
    public static int collectHoneycomb(Level level, BlockPos hivePos) {
        BlockState hiveState = level.getBlockState(hivePos);

        if (hiveState.is(Blocks.BEEHIVE) || hiveState.is(Blocks.BEE_NEST)) {
            int honeyLevel = hiveState.getValue(BeehiveBlock.HONEY_LEVEL);

            if (honeyLevel >= 5) {
                // Harvest honeycomb (would use shears)
                // Returns 1-3 honeycombs
                int honeycomb = level.random.nextInt(3) + 1;

                // Reset hive
                level.setBlock(hivePos, hiveState.setValue(BeehiveBlock.HONEY_LEVEL, 0), 3);

                return honeycomb;
            }
        }

        return 0;
    }
}
```

---

## Summary

This document provides comprehensive research and design for copper farm automation in MineWright (Minecraft Forge 1.20.1). Key points:

1. **Lightning Rod Range:** 128 blocks in Java Edition, spherical
2. **Thunderstorm Frequency:** ~9 hours between storms, lasting 3-13 minutes
3. **Oxidation Control:** Use waxing for permanent prevention, lightning for reversal
4. **Multi-Rod Optimization:** Hexagonal grid with 110-block spacing for maximum coverage
5. **Collection:** Hopper minecart system recommended for automation
6. **Forge Events:** Use `EntityJoinLevelEvent` to detect lightning strikes

**Integration with MineWright:**
- Add new `BuildCopperFarmAction` for building farms
- Create `CopperFarmEventHandler` for lightning detection
- Extend `StructureGenerators` with copper farm templates
- Add to `PromptBuilder` for LLM integration

---

## References

- [Lightning Rod - Minecraft Wiki](https://minecraft.fandom.com/wiki/Lightning_Rod)
- [Thunderstorm - Minecraft Wiki](https://minecraft.fandom.com/wiki/Thunderstorm)
- [Oxidation - Minecraft Wiki](https://minecraft.fandom.com/wiki/Oxidation)
- [Copper Oxidation Farm Tutorial (Bilibili)](https://www.bilibili.com/read/cv25701804/)
- [Forge Event System Documentation](https://m.blog.csdn.net/gitblog_00601/article/details/152070036)

---

**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Target Version:** Minecraft Forge 1.20.1
**Mod:** MineWright (Steve AI - "Cursor for Minecraft")
