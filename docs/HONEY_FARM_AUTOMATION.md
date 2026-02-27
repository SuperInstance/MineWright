# Honey Farm Automation for MineWright

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document
**Target Platform:** Minecraft Forge 1.20.1

## Table of Contents

1. [Overview](#overview)
2. [Bee Mechanics Reference](#bee-mechanics-reference)
3. [Architecture](#architecture)
4. [Bee Nest Detection](#bee-nest-detection)
5. [Bee Population Management](#bee-population-management)
6. [Honey Collection Automation](#honey-collection-automation)
7. [Honeycomb Farming](#honeycomb-farming)
8. [Crop Pollination Optimization](#crop-pollination-optimization)
9. [Code Examples](#code-examples)
10. [Implementation Roadmap](#implementation-roadmap)
11. [Integration Guide](#integration-guide)
12. [Testing Checklist](#testing-checklist)

---

## Overview

The Honey Farm Automation system enables MineWright Foreman entities to autonomously manage bee colonies for honey and honeycomb production while leveraging bee pollination to accelerate crop growth.

### Key Features

- **Smart Detection**: Locate bee nests and hives within operational range
- **Population Management**: Monitor bee counts, breed new bees, and relocate colonies
- **Automated Harvesting**: Collect honey bottles and honeycombs using dispensers or manual collection
- **Pollination Optimization**: Position hives to maximize crop growth acceleration
- **Safety Protocols**: Use campfires to prevent bee aggression during harvest
- **Hive Relocation**: Transport bees using silk touch and lead mechanics

### Value Proposition

Bee automation provides three major benefits:

1. **Sustainable Food Source**: Honey bottles restore 6 hunger points + 2.4 saturation
2. **Resource Production**: Honeycombs for crafting hives, candles, and copper preservation
3. **Crop Acceleration**: Bee pollination acts as automatic bonemeal for nearby crops

---

## Bee Mechanics Reference

### Vanilla Minecraft Bee Behavior

| Aspect | Detail |
|--------|--------|
| **Spawn Biomes** | Meadow (100%), Plains (5%), Flower Forest (2%) |
| **Spawn Location** | Oak/Birch trees naturally |
| **Bees per Hive** | Maximum 3 bees |
| **Hive Capacity** | Up to 3 bees resident |
| **Honey Levels** | 0-5 (harvestable at 5) |
| **Honey Production Time** | ~2 minutes per level (10 minutes full cycle) |
| **Pollination Range** | Up to 22 blocks from hive |
| **Pollination Effect** | Same as bonemeal - advances growth stage |
| **Night Behavior** | Bees return to hive at night/rain |
| **Aggression Trigger** | Breaking hive or harvesting without campfire |
| **Aggression Duration** | Bee stings poison, dies after stinging |

### Block States and Properties

**BeehiveBlock Properties:**
```java
// Block state property for honey level
IntegerProperty HONEY_LEVEL = IntegerProperty.create("honey_level", 0, 5);

// Block state property for facing
DirectionProperty FACING = DirectionProperty.create("facing",
    Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
```

**BeehiveBlockEntity Data:**
```java
// NBT structure stored in block entity
{
  "Bees": [
    {
      "EntityData": { ... }, // Bee entity NBT
      "MinOccupationTicks": 400, // Minimum ticks to stay
      "TicksInHive": 1200 // Current ticks in hive
    }
  ],
  "FlowerPos": { // Cached flower position
    "X": 100,
    "Y": 64,
    "Z": 200
  }
}
```

### Harvesting Methods

| Method | Tool | Result | Notes |
|--------|------|--------|-------|
| **Shears** | Shears | 3 Honeycombs | Honey level resets to 0 |
| **Glass Bottle** | Glass Bottle | Honey Bottle | Honey level resets to 0 |
| **Campfire + Tool** | Any | Safe Harvest | No bee aggression |

### Pollination Mechanics

**Crops Affected by Pollination:**
- Wheat, Carrots, Potatoes, Beetroots
- Melons, Pumpkins, Sweet Berries
- Cocoa Beans, Bamboo, Sugar Cane
- Nether Wart, Torchflower, Pitcher Pod

**Pollination Process:**
1. Bee exits hive and finds flower (within 22 blocks)
2. Bee collects pollen (visible pollen particles on bee)
3. Bee returns to hive (drops pollen particles en route)
4. Pollen particles contacting crops = bonemeal effect
5. Bee deposits pollen in hive (increases honey level)

---

## Architecture

### Package Structure

```
com.minewright.bee/
├── BeehiveDetector.java           # Scan for bee nests/hives
├── BeeColony.java                 # Track bee colony data
├── BeePopulationManager.java      # Monitor and manage bee counts
├── HoneyHarvestManager.java       # Coordinate honey collection
├── PollinationOptimizer.java      # Optimize hive placement for crops
├── BeeSafetyManager.java          # Handle campfire placement and safety
└── actions/
    ├── FindBeehiveAction.java     # Locate nearby bee nests
    ├── HarvestHoneyAction.java    # Collect honey bottles
    ├── HarvestHoneycombAction.java # Collect honeycombs
    ├── BreedBeesAction.java       # Breed bees with flowers
    ├── RelocateHiveAction.java    # Move hive with silk touch
    ├── BuildBeehiveAction.java    # Craft and place new hive
    ├── PlaceCampfireAction.java   # Safe harvest setup
    └── PollinateCropsAction.java  # Position hives for pollination
```

### Class Diagram

```
┌─────────────────────────┐       ┌──────────────────┐
│    BeehiveDetector      │◄──────│  BeeColony       │
├─────────────────────────┤       ├──────────────────┤
│ + scan(radius)          │       │ + hivePos        │
│ + findNearest()         │       │ + beeCount       │
│ + countBees()           │       │ + honeyLevel     │
│ + isHarvestable()       │       │ + flowerPos      │
└─────────────────────────┘       └──────────────────┘
           ▲                                ▲
           │                                │
┌─────────────────────────┐       ┌──────────────────┐
│  HoneyHarvestManager    │◄──────│PollinationOptimizer│
├─────────────────────────┤       ├──────────────────┤
│ + harvestHoney()        │       │ + findBestSpot() │
│ + harvestHoneycombs()   │       │ + layoutHives()  │
│ + setupCampfire()       │       │ + calculateCoverage()│
└─────────────────────────┘       └──────────────────┘
           ▲
           │
┌─────────────────────────┐       ┌──────────────────┐
│ BeePopulationManager    │◄──────│ BeeSafetyManager │
├─────────────────────────┤       ├──────────────────┤
│ + getBeeCount()         │       │ + placeCampfire()│
│ + breedBees()           │       │ + isSafeToHarvest()│
│ + relocateBees()        │       │ + clearCampfire()│
└─────────────────────────┘       └──────────────────┘
```

---

## Bee Nest Detection

### BeehiveDetector

```java
package com.minewright.bee;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects and analyzes bee nests and beehives within range.
 */
public class BeehiveDetector {
    private final ForemanEntity foreman;
    private final Level level;
    private final int defaultRadius = 64;

    public BeehiveDetector(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = foreman.level();
    }

    /**
     * Scan for all bee nests and hives within radius
     */
    public List<BeeColony> scan(int radius) {
        AABB searchBox = foreman.getBoundingBox().inflate(radius);
        List<BeeColony> colonies = new ArrayList<>();

        // Iterate through all blocks in range
        BlockPos.betweenClosedStream(
            searchBox.minX, searchBox.minY, searchBox.minZ,
            searchBox.maxX, searchBox.maxY, searchBox.maxZ
        ).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) {
                colonies.add(analyzeColony(pos));
            }
        });

        return colonies;
    }

    /**
     * Find nearest bee nest/hive
     */
    public BeeColony findNearest() {
        List<BeeColony> colonies = scan(defaultRadius);
        if (colonies.isEmpty()) return null;

        return colonies.stream()
            .min(Comparator.comparingDouble(c -> foreman.distanceToSqr(c.hivePos())))
            .orElse(null);
    }

    /**
     * Find all harvestable hives (honey level >= 5)
     */
    public List<BeeColony> findHarvestable() {
        return scan(defaultRadius).stream()
            .filter(BeeColony::isHarvestable)
            .collect(Collectors.toList());
    }

    /**
     * Count total bees across all hives
     */
    public int countTotalBees() {
        return scan(defaultRadius).stream()
            .mapToInt(BeeColony::beeCount)
            .sum();
    }

    /**
     * Analyze a single colony at the given position
     */
    private BeeColony analyzeColony(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean isNest = state.is(Blocks.BEE_NEST);

        // Get honey level from block state
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);

        // Get bee count from block entity
        int beeCount = 0;
        BlockPos flowerPos = null;

        if (level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehive) {
            beeCount = beehive.getOccupantCount();
            // Note: Flower position is stored internally but not directly accessible
        }

        return new BeeColony(
            pos,
            isNest ? ColonyType.NEST : ColonyType.HIVE,
            honeyLevel,
            beeCount,
            honeyLevel >= 5
        );
    }

    /**
     * Find optimal location for new hive placement
     * Considers: flower proximity, crop proximity, space
     */
    public BlockPos findOptimalHiveLocation(BlockPos center, int searchRadius) {
        BlockPos bestLocation = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int x = -searchRadius; x <= searchRadius; x += 4) {
            for (int z = -searchRadius; z <= searchRadius; z += 4) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos candidate = center.offset(x, y, z);
                    double score = scoreHiveLocation(candidate);

                    if (score > bestScore) {
                        bestScore = score;
                        bestLocation = candidate;
                    }
                }
            }
        }

        return bestLocation;
    }

    /**
     * Score a potential hive location
     * Higher is better
     */
    private double scoreHiveLocation(BlockPos pos) {
        double score = 0.0;

        // Check if location is valid (air, solid below)
        if (!level.getBlockState(pos).isAir()) {
            return Double.NEGATIVE_INFINITY;
        }
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            return Double.NEGATIVE_INFINITY;
        }

        // Proximity to flowers (within 22 blocks)
        long flowerCount = BlockPos.betweenClosedStream(
            pos.getX() - 22, pos.getY() - 3, pos.getZ() - 22,
            pos.getX() + 22, pos.getY() + 3, pos.getZ() + 22
        ).filter(p -> {
            var block = level.getBlockState(p).getBlock();
            return block.getRegistryName() != null &&
                   block.getRegistryName().getPath().contains("flower");
        }).count();

        score += flowerCount * 10.0;

        // Proximity to crops (for pollination bonus)
        long cropCount = BlockPos.betweenClosedStream(
            pos.getX() - 22, pos.getY() - 3, pos.getZ() - 22,
            pos.getX() + 22, pos.getY() + 3, pos.getZ() + 22
        ).filter(p -> {
            var block = level.getBlockState(p).getBlock();
            return block == Blocks.WHEAT ||
                   block == Blocks.CARROTS ||
                   block == Blocks.POTATOES ||
                   block == Blocks.BEETROOTS;
        }).count();

        score += cropCount * 5.0;

        // Avoid existing hives (space them out)
        double distanceToNearest = scan(defaultRadius).stream()
            .mapToDouble(c -> c.hivePos().distSqr(pos))
            .min()
            .orElse(Double.MAX_VALUE);

        if (distanceToNearest < 16) {
            score -= 100.0; // Too close to existing hive
        }

        return score;
    }
}
```

### BeeColony Record

```java
package com.minewright.bee;

import net.minecraft.core.BlockPos;

/**
 * Represents a bee colony (nest or hive)
 */
public record BeeColony(
    BlockPos hivePos,          // Position of the hive/nest
    ColonyType type,           // NEST (natural) or HIVE (crafted)
    int honeyLevel,            // 0-5
    int beeCount,              // 0-3 bees
    boolean isHarvestable      // true if honeyLevel >= 5
) {
    public enum ColonyType {
        NEST,   // Natural bee nest
        HIVE    // Crafted beehive
    }

    /**
     * Get time until harvestable (in ticks)
     * Assumes ~2400 ticks (2 minutes) per honey level
     */
    public int ticksUntilHarvestable() {
        if (isHarvestable) return 0;
        return (5 - honeyLevel) * 2400;
    }

    /**
     * Check if colony is at full capacity (3 bees)
     */
    public boolean isFull() {
        return beeCount >= 3;
    }
}
```

---

## Bee Population Management

### BeePopulationManager

```java
package com.minewright.bee;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages bee population across colonies
 */
public class BeePopulationManager {
    private final ForemanEntity foreman;
    private final Level level;
    private final Map<BlockPos, ColonyStatus> trackedColonies = new ConcurrentHashMap<>();

    public BeePopulationManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = foreman.level();
    }

    /**
     * Breed bees at a specific colony
     * Requires: 2 bees, flowers nearby
     */
    public boolean breedBees(BlockPos hivePos) {
        BeeColony colony = new BeehiveDetector(foreman).scan(16).stream()
            .filter(c -> c.hivePos().equals(hivePos))
            .findFirst()
            .orElse(null);

        if (colony == null) {
            foreman.sendChatMessage("No hive found at " + hivePos);
            return false;
        }

        if (colony.isFull()) {
            foreman.sendChatMessage("Hive is already full (3 bees)");
            return false;
        }

        // Find nearby bees
        List<Bee> nearbyBees = findNearbyBees(hivePos, 16);
        if (nearbyBees.size() < 2) {
            foreman.sendChatMessage("Need at least 2 bees to breed");
            return false;
        }

        // Find flowers for breeding
        Optional<BlockPos> flower = findNearestFlower(hivePos);
        if (flower.isEmpty()) {
            foreman.sendChatMessage("No flowers nearby for breeding");
            return false;
        }

        // Feed flowers to two bees
        int bredCount = 0;
        for (Bee bee : nearbyBees) {
            if (bredCount >= 2) break;

            // In Minecraft, bees enter love mode when fed flowers
            // We need to simulate this
            bee.setInLoveTime(600);
            bredCount++;
        }

        foreman.sendChatMessage("Breeding bees at hive...");
        return true;
    }

    /**
     * Relocate bees from one hive to another
     * Uses silk touch to break hive with bees inside
     */
    public boolean relocateColony(BlockPos fromHive, BlockPos toHive) {
        // This requires:
        // 1. Silk touch tool
        // 2. Break hive with bees inside
        // 3. Place hive at new location
        // 4. Bees remain inside

        // Note: This is complex and requires silk touch enchantment
        foreman.sendChatMessage("Hive relocation requires silk touch tool");
        return false;
    }

    /**
     * Track population across all colonies
     */
    public PopulationStats getPopulationStats() {
        BeehiveDetector detector = new BeehiveDetector(foreman);
        List<BeeColony> colonies = detector.scan(64);

        int totalHives = colonies.size();
        int totalBees = colonies.stream().mapToInt(BeeColony::beeCount).sum();
        int harvestableHives = (int) colonies.stream().filter(BeeColony::isHarvestable).count();

        return new PopulationStats(
            totalHives,
            totalBees,
            harvestableHives,
            colonies.stream().filter(c -> c.type() == BeeColony.ColonyType.NEST).count(),
            colonies.stream().filter(c -> c.type() == BeeColony.ColonyType.HIVE).count()
        );
    }

    /**
     * Find all free bees (not in a hive)
     */
    public List<Bee> findFreeBees(int radius) {
        AABB searchBox = foreman.getBoundingBox().inflate(radius);

        return level.getEntitiesOfClass(Bee.class, searchBox).stream()
            .filter(bee -> !bee.isHiveValid()) // Bee not in a hive
            .toList();
    }

    /**
     * Guide free bees to a specific hive
     * Uses lead or flower lure mechanic
     */
    public boolean guideBeesToHive(BlockPos targetHive, int targetCount) {
        List<Bee> freeBees = findFreeBees(32);

        if (freeBees.isEmpty()) {
            foreman.sendChatMessage("No free bees found");
            return false;
        }

        // Bees follow players holding flowers
        // We need to simulate this behavior
        foreman.sendChatMessage("Guiding " + Math.min(targetCount, freeBees.size()) + " bees to hive");

        // In practice: move foreman near hive while "holding" flowers
        // Bees will follow and can enter the hive
        return true;
    }

    private List<Bee> findNearbyBees(BlockPos pos, int radius) {
        AABB searchBox = new AABB(
            pos.getX() - radius, pos.getY() - 4, pos.getZ() - radius,
            pos.getX() + radius, pos.getY() + 4, pos.getZ() + radius
        );

        return level.getEntitiesOfClass(Bee.class, searchBox);
    }

    private Optional<BlockPos> findNearestFlower(BlockPos center) {
        return BlockPos.findClosestMatch(
            center, 22, 16,
            pos -> {
                var block = level.getBlockState(pos).getBlock();
                var name = block.getRegistryName();
                return name != null && name.getPath().contains("flower");
            }
        );
    }

    public record PopulationStats(
        int totalHives,
        int totalBees,
        int harvestableHives,
        int naturalNests,
        int craftedHives
    ) {
        public double getAverageBeesPerHive() {
            return totalHives > 0 ? (double) totalBees / totalHives : 0;
        }
    }

    public static class ColonyStatus {
        private final BlockPos hivePos;
        private int lastKnownBeeCount;
        private long lastCheckTime;

        public ColonyStatus(BlockPos hivePos, int beeCount) {
            this.hivePos = hivePos;
            this.lastKnownBeeCount = beeCount;
            this.lastCheckTime = System.currentTimeMillis();
        }

        public void update(int beeCount) {
            this.lastKnownBeeCount = beeCount;
            this.lastCheckTime = System.currentTimeMillis();
        }

        public boolean needsUpdate() {
            return System.currentTimeMillis() - lastCheckTime > 60000; // 1 minute
        }
    }
}
```

---

## Honey Collection Automation

### HoneyHarvestManager

```java
package com.minewright.bee;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages automated honey and honeycomb harvesting
 */
public class HoneyHarvestManager {
    private final ForemanEntity foreman;
    private final Level level;

    public HoneyHarvestManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = foreman.level();
    }

    /**
     * Harvest all harvestable hives for honey bottles
     */
    public CompletableFuture<HarvestResult> harvestAllHoney() {
        BeehiveDetector detector = new BeehiveDetector(foreman);
        List<BeeColony> harvestable = detector.findHarvestable();

        if (harvestable.isEmpty()) {
            foreman.sendChatMessage("No hives ready for harvest");
            return CompletableFuture.completedFuture(
                new HarvestResult(0, 0, List.of())
            );
        }

        foreman.sendChatMessage("Harvesting " + harvestable.size() + " hives for honey");

        int totalBottles = 0;
        List<String> messages = List.of();

        for (BeeColony colony : harvestable) {
            int collected = harvestHoneyBottle(colony.hivePos());
            if (collected > 0) {
                totalBottles += collected;
                messages = List.of("Collected " + collected + " honey bottles from hive at " + colony.hivePos());
            }
        }

        return CompletableFuture.completedFuture(
            new HarvestResult(totalBottles, 0, messages)
        );
    }

    /**
     * Harvest all harvestable hives for honeycombs
     */
    public CompletableFuture<HarvestResult> harvestAllHoneycombs() {
        BeehiveDetector detector = new BeehiveDetector(foreman);
        List<BeeColony> harvestable = detector.findHarvestable();

        if (harvestable.isEmpty()) {
            foreman.sendChatMessage("No hives ready for harvest");
            return CompletableFuture.completedFuture(
                new HarvestResult(0, 0, List.of())
            );
        }

        foreman.sendChatMessage("Harvesting " + harvestable.size() + " hives for honeycombs");

        int totalCombs = 0;
        List<String> messages = List.of();

        for (BeeColony colony : harvestable) {
            int collected = harvestHoneycombs(colony.hivePos());
            if (collected > 0) {
                totalCombs += collected;
                messages = List.of("Collected " + collected + " honeycombs from hive at " + colony.hivePos());
            }
        }

        return CompletableFuture.completedFuture(
            new HarvestResult(0, totalCombs, messages)
        );
    }

    /**
     * Harvest honey from a single hive using glass bottle
     * Requires: campfire underneath for safety
     */
    private int harvestHoneyBottle(BlockPos hivePos) {
        // Ensure campfire is present for safety
        if (!hasCampfire(hivePos)) {
            placeCampfire(hivePos);
            // Wait for campfire smoke to calm bees
            waitForTicks(20);
        }

        // Find glass bottle in inventory
        ItemStack bottleStack = findItem(Items.GLASS_BOTTLE);
        if (bottleStack.isEmpty()) {
            foreman.sendChatMessage("No glass bottles available");
            return 0;
        }

        // Navigate to hive
        if (!navigateTo(hivePos)) {
            return 0;
        }

        // Use bottle on hive
        BlockState hiveState = level.getBlockState(hivePos);
        if (hiveState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) >= 5) {
            // Simulate right-click with bottle
            // In Forge, this would use the item's interaction method
            bottleStack.shrink(1); // Consume bottle

            // Get honey bottle
            ItemStack honeyBottle = new ItemStack(Items.HONEY_BOTTLE);

            // Add to inventory
            if (!foreman.getInventory().add(honeyBottle)) {
                // Drop if inventory full
                foreman.spawnAtLocation(honeyBottle);
            }

            // Reset honey level
            level.setBlock(hivePos, hiveState.setValue(
                net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 0
            ), 3);

            foreman.notifyTaskCompleted("Harvested honey bottle");
            return 1;
        }

        return 0;
    }

    /**
     * Harvest honeycombs from a single hive using shears
     * Requires: campfire underneath for safety
     */
    private int harvestHoneycombs(BlockPos hivePos) {
        // Ensure campfire is present for safety
        if (!hasCampfire(hivePos)) {
            placeCampfire(hivePos);
            waitForTicks(20);
        }

        // Find shears in inventory
        ItemStack shearsStack = findItem(Items.SHEARS);
        if (shearsStack.isEmpty()) {
            foreman.sendChatMessage("No shears available");
            return 0;
        }

        // Navigate to hive
        if (!navigateTo(hivePos)) {
            return 0;
        }

        // Use shears on hive
        BlockState hiveState = level.getBlockState(hivePos);
        if (hiveState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) >= 5) {
            // Simulate right-click with shears
            // Drops 3 honeycombs
            for (int i = 0; i < 3; i++) {
                ItemStack honeycomb = new ItemStack(Items.HONEYCOMB);
                if (!foreman.getInventory().add(honeycomb)) {
                    foreman.spawnAtLocation(honeycomb);
                }
            }

            // Damage shears slightly
            if (shearsStack.isDamageableItem()) {
                shearsStack.hurtAndBreak(1, foreman, (entity) -> {
                    entity.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                });
            }

            // Reset honey level
            level.setBlock(hivePos, hiveState.setValue(
                net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 0
            ), 3);

            foreman.notifyTaskCompleted("Harvested 3 honeycombs");
            return 3;
        }

        return 0;
    }

    /**
     * Place campfire under hive for safe harvesting
     */
    private void placeCampfire(BlockPos hivePos) {
        BlockPos campfirePos = hivePos.below();

        // Check if space is valid
        if (!level.getBlockState(campfirePos).isAir()) {
            foreman.sendChatMessage("Cannot place campfire - space occupied");
            return;
        }

        // Find campfire in inventory
        ItemStack campfireStack = findItem(Items.CAMPFIRE);
        if (campfireStack.isEmpty()) {
            foreman.sendChatMessage("No campfire available");
            return;
        }

        // Place campfire
        level.setBlock(campfirePos, Blocks.CAMPFIRE.defaultBlockState()
            .setValue(CampfireBlock.FACING, foreman.getDirection())
            .setValue(CampfireBlock.LIT, true), 3);

        campfireStack.shrink(1);

        foreman.sendChatMessage("Placed campfire for safe harvest");
    }

    private boolean hasCampfire(BlockPos hivePos) {
        BlockState below = level.getBlockState(hivePos.below());
        return below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE);
    }

    private ItemStack findItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < foreman.getInventory().getContainerSize(); i++) {
            ItemStack stack = foreman.getInventory().getItem(i);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean navigateTo(BlockPos pos) {
        foreman.getNavigation().moveTo(
            pos.getX() + 0.5,
            pos.getY(),
            pos.getZ() + 0.5,
            1.0
        );

        // Wait until arrived or timeout
        int timeout = 200; // 10 seconds
        int waited = 0;
        while (foreman.getNavigation().isInProgress() && waited < timeout) {
            waitForTicks(1);
            waited++;
        }

        double distance = foreman.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return distance < 9.0; // Within 3 blocks
    }

    private void waitForTicks(int ticks) {
        // This would be called from the action's tick loop
        // Placeholder for demonstration
    }

    public record HarvestResult(
        int honeyBottlesCollected,
        int honeycombsCollected,
        List<String> messages
    ) {
        public int totalItems() {
            return honeyBottlesCollected + honeycombsCollected;
        }
    }
}
```

---

## Honeycomb Farming

### Automated Honeycomb Action

```java
package com.minewright.bee.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.bee.HoneyHarvestManager;
import com.minewright.entity.ForemanEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Action to harvest honeycombs from all ready hives
 */
public class HarvestHoneycombAction extends BaseAction {
    private HoneyHarvestManager harvestManager;
    private CompletableFuture<HoneyHarvestManager.HarvestResult> harvestFuture;
    private boolean started = false;

    public HarvestHoneycombAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        harvestManager = new HoneyHarvestManager(foreman);

        // Start async harvest
        harvestFuture = harvestManager.harvestAllHoneycombs();
        started = true;

        foreman.sendChatMessage("Starting honeycomb harvest");
    }

    @Override
    protected void onTick() {
        if (!started) {
            result = ActionResult.failure("Harvest not started");
            return;
        }

        if (harvestFuture.isDone()) {
            try {
                HoneyHarvestManager.HarvestResult harvestResult = harvestFuture.get();

                if (harvestResult.totalItems() > 0) {
                    result = ActionResult.success(
                        "Harvested " + harvestResult.honeycombsCollected() + " honeycombs"
                    );
                    foreman.notifyMilestone("Harvested " + harvestResult.honeycombsCollected() + " honeycombs!");
                } else {
                    result = ActionResult.failure("No hives ready for harvest", false);
                }
            } catch (Exception e) {
                result = ActionResult.failure("Harvest failed: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Harvest honeycombs from ready hives";
    }
}
```

### Build Beehive Action

```java
package com.minewright.bee.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

/**
 * Action to craft and place a new beehive
 */
public class BuildBeehiveAction extends BaseAction {
    private BlockPos targetPos;
    private int phase = 0; // 0: gather resources, 1: craft, 2: place
    private int planksNeeded = 6;
    private int combsNeeded = 3;

    public BuildBeehiveAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        targetPos = new BlockPos(
            task.getIntParameter("x", foreman.getBlockX()),
            task.getIntParameter("y", foreman.getBlockY()),
            task.getIntParameter("z", foreman.getBlockZ())
        );

        foreman.sendChatMessage("Building beehive at " + targetPos);
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case 0 -> checkResources();
            case 1 -> craftHive();
            case 2 -> placeHive();
        }
    }

    private void checkResources() {
        int planksCount = countItem(Items.OAK_PLANKS); // Simplified - any planks
        int combsCount = countItem(Items.HONEYCOMB);

        if (planksCount >= planksNeeded && combsCount >= combsNeeded) {
            phase = 1;
            foreman.sendChatMessage("Resources ready, crafting beehive");
        } else {
            result = ActionResult.failure(
                "Need " + planksNeeded + " planks and " + combsNeeded + " honeycombs. " +
                "Have: " + planksCount + " planks, " + combsCount + " honeycombs"
            );
        }
    }

    private void craftHive() {
        // In practice, this would use a crafting table
        // Simplified: consume items and "craft" the hive
        consumeItems(Items.OAK_PLANKS, planksNeeded);
        consumeItems(Items.HONEYCOMB, combsNeeded);

        phase = 2;
        foreman.sendChatMessage("Beehive crafted");
    }

    private void placeHive() {
        if (!foreman.getNavigation().isInProgress()) {
            foreman.getNavigation().moveTo(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                1.0
            );
        }

        double distance = foreman.distanceToSqr(
            targetPos.getX() + 0.5,
            targetPos.getY(),
            targetPos.getZ() + 0.5
        );

        if (distance < 9.0) {
            // Place beehive
            foreman.level().setBlock(
                targetPos,
                net.minecraft.world.level.block.Blocks.BEEHIVE.defaultBlockState(),
                3
            );

            result = ActionResult.success("Beehive placed at " + targetPos);
            foreman.notifyMilestone("Built new beehive!");
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build beehive at " + targetPos;
    }

    private int countItem(net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < foreman.getInventory().getContainerSize(); i++) {
            var stack = foreman.getInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeItems(net.minecraft.world.item.Item item, int count) {
        int remaining = count;
        for (int i = 0; i < foreman.getInventory().getContainerSize() && remaining > 0; i++) {
            var stack = foreman.getInventory().getItem(i);
            if (stack.is(item)) {
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                remaining -= toTake;
            }
        }
    }
}
```

---

## Crop Pollination Optimization

### PollinationOptimizer

```java
package com.minewright.bee;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * Optimizes hive placement for maximum crop pollination
 */
public class PollinationOptimizer {
    private final ForemanEntity foreman;
    private final Level level;

    public PollinationOptimizer(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = foreman.level();
    }

    /**
     * Calculate optimal hive positions for a farm
     * Layout: Grid pattern covering all crops
     */
    public List<BlockPos> optimizeHiveLayout(BlockPos farmCenter, int farmWidth, int farmDepth) {
        List<BlockPos> hivePositions = new ArrayList<>();

        // Bee pollination range is ~22 blocks horizontally
        // Place hives in grid pattern to cover entire farm
        int spacing = 30; // Leave some overlap for complete coverage

        for (int x = -farmWidth / 2; x <= farmWidth / 2; x += spacing) {
            for (int z = -farmDepth / 2; z <= farmDepth / 2; z += spacing) {
                BlockPos hivePos = farmCenter.offset(x, 2, z);

                // Position hive 2 blocks above crops (bees fly downward)
                if (isValidHiveLocation(hivePos)) {
                    hivePositions.add(hivePos);
                }
            }
        }

        foreman.sendChatMessage("Optimized layout: " + hivePositions.size() + " hives for farm coverage");
        return hivePositions;
    }

    /**
     * Calculate pollination coverage for existing hives
     */
    public PollinationCoverage calculateCoverage(BlockPos areaCenter, int areaRadius) {
        BeehiveDetector detector = new BeehiveDetector(foreman);
        List<BeeColony> hives = detector.scan(areaRadius * 2);

        int totalBlocks = (int) Math.pow(areaRadius * 2, 2);
        int coveredBlocks = 0;
        Set<BlockPos> coveredPositions = new HashSet<>();

        for (BeeColony hive : hives) {
            // Mark all blocks within 22 blocks as covered
            for (int x = -22; x <= 22; x++) {
                for (int z = -22; z <= 22; z++) {
                    BlockPos checkPos = hive.hivePos().offset(x, 0, z);
                    if (checkPos.distSqr(areaCenter) <= areaRadius * areaRadius) {
                        coveredPositions.add(checkPos);
                    }
                }
            }
        }

        coveredBlocks = coveredPositions.size();
        double coverage = (double) coveredBlocks / totalBlocks * 100.0;

        return new PollinationCoverage(
            hives.size(),
            coveredBlocks,
            totalBlocks,
            coverage
        );
    }

    /**
     * Find best location to add a hive for better coverage
     */
    public BlockPos findBestNewHiveLocation(BlockPos areaCenter, int areaRadius) {
        PollinationCoverage current = calculateCoverage(areaCenter, areaRadius);

        if (current.coverage() >= 95.0) {
            foreman.sendChatMessage("Farm already has " + String.format("%.1f", current.coverage()) + "% coverage");
            return null;
        }

        // Find uncovered areas
        Set<BlockPos> covered = new HashSet<>();
        BeehiveDetector detector = new BeehiveDetector(foreman);
        List<BeeColony> hives = detector.scan(areaRadius * 2);

        for (BeeColony hive : hives) {
            for (int x = -22; x <= 22; x++) {
                for (int z = -22; z <= 22; z++) {
                    covered.add(hive.hivePos().offset(x, 0, z));
                }
            }
        }

        // Find position that covers most uncovered blocks
        BlockPos bestPos = null;
        int maxNewCoverage = 0;

        for (int x = -areaRadius; x <= areaRadius; x += 4) {
            for (int z = -areaRadius; z <= areaRadius; z += 4) {
                BlockPos candidate = areaCenter.offset(x, 2, z);
                if (!isValidHiveLocation(candidate)) continue;

                int newCoverage = 0;
                for (int dx = -22; dx <= 22; dx++) {
                    for (int dz = -22; dz <= 22; dz++) {
                        BlockPos coveredPos = candidate.offset(dx, 0, dz);
                        if (!covered.contains(coveredPos)) {
                            newCoverage++;
                        }
                    }
                }

                if (newCoverage > maxNewCoverage) {
                    maxNewCoverage = newCoverage;
                    bestPos = candidate;
                }
            }
        }

        return bestPos;
    }

    /**
     * Estimate crop growth acceleration from pollination
     * Bee pollination = ~1 bonemeal per bee pass
     */
    public int estimateGrowthBonus(BlockPos farmCenter, int farmRadius) {
        PollinationCoverage coverage = calculateCoverage(farmCenter, farmRadius);
        int activeHives = coverage.hiveCount();
        int avgBeesPerHive = 2; // Assume average

        // Each bee makes ~1 trip per 2 minutes
        // Each trip pollinates ~5-10 crop blocks
        // So: hives * bees * trips * crops_per_trip / time_period

        int beesPerTick = activeHives * avgBeesPerHive / 2400; // 2400 ticks = 2 minutes
        int cropsPerTrip = 7;
        int ticksPerGameDay = 24000;

        // Crops accelerated per game day
        return beesPerTick * cropsPerTrip * ticksPerGameDay;
    }

    private boolean isValidHiveLocation(BlockPos pos) {
        // Must be air
        if (!level.getBlockState(pos).isAir()) return false;

        // Must have solid block below
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) return false;

        // Must have space above for bees
        if (!level.getBlockState(pos.above()).isAir()) return false;

        // Must be within world bounds
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight() - 1) {
            return false;
        }

        return true;
    }

    public record PollinationCoverage(
        int hiveCount,
        int coveredBlocks,
        int totalBlocks,
        double coverage
    ) {
        public String getFormattedCoverage() {
            return String.format("%.1f%%", coverage);
        }
    }
}
```

---

## Code Examples

### Example 1: Finding and Harvesting Beehives

```java
// Find all harvestable hives
BeehiveDetector detector = new BeehiveDetector(foreman);
List<BeeColony> harvestable = detector.findHarvestable();

foreman.sendChatMessage("Found " + harvestable.size() + " hives ready for harvest");

// Harvest honey
HoneyHarvestManager harvestManager = new HoneyHarvestManager(foreman);
var result = harvestManager.harvestAllHoney().get();

foreman.sendChatMessage("Collected " + result.honeyBottlesCollected() + " honey bottles");
```

### Example 2: Building New Beehive

```java
// Create task to build beehive
Task task = new Task("build_beehive", Map.of(
    "x", 100,
    "y", 64,
    "z", 200
));

BuildBeehiveAction action = new BuildBeehiveAction(foreman, task);
action.start();

while (!action.isComplete()) {
    action.tick();
    Thread.sleep(50); // Tick loop
}

ActionResult result = action.getResult();
if (result.success()) {
    System.out.println("Beehive built: " + result.message());
}
```

### Example 3: Optimizing Pollination for Farm

```java
// Calculate current coverage
PollinationOptimizer optimizer = new PollinationOptimizer(foreman);
BlockPos farmCenter = new BlockPos(0, 64, 0);
int farmRadius = 32;

PollinationCoverage coverage = optimizer.calculateCoverage(farmCenter, farmRadius);
foreman.sendChatMessage("Current coverage: " + coverage.getFormattedCoverage());

// Find optimal hive positions
List<BlockPos> optimalHives = optimizer.optimizeHiveLayout(farmCenter, 64, 64);
foreman.sendChatMessage("Recommended " + optimalHives.size() + " hives for full coverage");

// Find best location for new hive
BlockPos bestSpot = optimizer.findBestNewHiveLocation(farmCenter, farmRadius);
if (bestSpot != null) {
    foreman.sendChatMessage("Best new hive location: " + bestSpot);
}
```

### Example 4: Managing Bee Population

```java
// Get population stats
BeePopulationManager popManager = new BeePopulationManager(foreman);
PopulationStats stats = popManager.getPopulationStats();

foreman.sendChatMessage("Managing " + stats.totalHives() + " hives with " +
    stats.totalBees() + " bees");
foreman.sendChatMessage("Average: " + String.format("%.1f", stats.getAverageBeesPerHive()) +
    " bees per hive");

// Breed bees at specific hive
BlockPos hivePos = new BlockPos(100, 64, 100);
boolean success = popManager.breedBees(hivePos);

if (success) {
    foreman.sendChatMessage("Bees breeding successfully");
}
```

### Example 5: Complete Honey Farm Setup

```java
// 1. Find optimal location
BeehiveDetector detector = new BeehiveDetector(foreman);
BlockPos optimalSpot = detector.findOptimalHiveLocation(foreman.blockPosition(), 32);

// 2. Build hive
Task buildTask = new Task("build_beehive", Map.of(
    "x", optimalSpot.getX(),
    "y", optimalSpot.getY(),
    "z", optimalSpot.getZ()
));

BuildBeehiveAction buildAction = new BuildBeehiveAction(foreman, buildTask);
buildAction.start();

// 3. Breed bees to fill hive
BeePopulationManager popManager = new BeePopulationManager(foreman);
popManager.breedBees(optimalSpot);

// 4. Position for pollination
PollinationOptimizer optimizer = new PollinationOptimizer(foreman);
List<BlockPos> hives = optimizer.optimizeHiveLayout(optimalSpot, 64, 64);

// 5. Set up automated harvesting
HoneyHarvestManager harvestManager = new HoneyHarvestManager(foreman);
// This would be called periodically from a tick handler
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Create `BeeColony` record class
- [ ] Implement `BeehiveDetector` for nest/hive scanning
- [ ] Add bee detection to `WorldKnowledge`
- [ ] Create basic hive location tracking

### Phase 2: Population Management (Week 2-3)
- [ ] Implement `BeePopulationManager`
- [ ] Add bee breeding logic
- [ ] Create population statistics tracking
- [ ] Implement free bee detection and guiding

### Phase 3: Honey Harvesting (Week 3-4)
- [ ] Implement `HoneyHarvestManager`
- [ ] Add campfire safety mechanics
- [ ] Create `HarvestHoneyAction`
- [ ] Create `HarvestHoneycombAction`
- [ ] Add inventory integration for bottles/shears

### Phase 4: Hive Building (Week 4-5)
- [ ] Implement `BuildBeehiveAction`
- [ ] Add crafting integration
- [ ] Create hive placement validation
- [ ] Implement nest relocation with silk touch

### Phase 5: Pollination Optimization (Week 5-6)
- [ ] Implement `PollinationOptimizer`
- [ ] Add coverage calculation
- [ ] Create optimal layout algorithm
- [ ] Implement growth bonus estimation

### Phase 6: Integration (Week 6-7)
- [ ] Register all bee actions in `CoreActionsPlugin`
- [ ] Update `PromptBuilder` with bee commands
- [ ] Add bee context to `WorldKnowledge`
- [ ] Create periodic harvest scheduler

### Phase 7: Advanced Features (Week 7-8)
- [ ] Implement automated dispenser-based harvesting
- [ ] Add flower planting for bee attraction
- [ ] Create bee health monitoring
- [ ] Implement crop growth tracking with pollination

---

## Integration Guide

### 1. Register Bee Actions

Add to `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Bee and honey farming actions
    registry.register("find_beehive",
        (foreman, task, ctx) -> new FindBeehiveAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("harvest_honey",
        (foreman, task, ctx) -> new HarvestHoneyAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("harvest_honeycomb",
        (foreman, task, ctx) -> new HarvestHoneycombAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("build_beehive",
        (foreman, task, ctx) -> new BuildBeehiveAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("breed_bees",
        (foreman, task, ctx) -> new BreedBeesAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("optimize_pollination",
        (foreman, task, ctx) -> new OptimizePollinationAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### 2. Update PromptBuilder

Add bee-related actions to system prompt:

```java
String beeActions = """
ACTIONS:
- find_beehive: {} (finds nearest bee nest or hive)
- harvest_honey: {} (collects honey bottles from ready hives)
- harvest_honeycomb: {} (collects honeycombs from ready hives)
- build_beehive: {"x": 100, "y": 64, "z": 200} (crafts and places new hive)
- breed_bees: {"hive_pos": [x, y, z]} (breeds bees at specific hive)
- optimize_pollination: {"farm_center": [x, y, z], "radius": 32} (optimizes hive layout)
""";
```

### 3. Add Bee Context to WorldKnowledge

```java
public class WorldKnowledge {
    // ... existing code ...

    private PopulationStats beeStats;
    private PollinationCoverage pollinationCoverage;

    public String getBeeSummary() {
        if (beeStats == null) {
            return "no bee colonies detected";
        }

        return String.format("%d hives with %d bees (%.1f%% pollination coverage)",
            beeStats.totalHives(),
            beeStats.totalBees(),
            pollinationCoverage != null ? pollinationCoverage.coverage() : 0.0);
    }

    public void updateBeeData(PopulationStats stats, PollinationCoverage coverage) {
        this.beeStats = stats;
        this.pollinationCoverage = coverage;
    }

    public boolean hasHarvestableHives() {
        return beeStats != null && beeStats.harvestableHives() > 0;
    }
}
```

### 4. Update ForemanEntity

Add bee managers to entity:

```java
public class ForemanEntity extends PathfinderMob {
    private BeePopulationManager beeManager;
    private HoneyHarvestManager honeyHarvestManager;
    private PollinationOptimizer pollinationOptimizer;

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing code ...

            // Update bee data periodically
            if (tickCounter % 200 == 0) { // Every 10 seconds
                updateBeeKnowledge();
            }
        }
    }

    private void updateBeeKnowledge() {
        BeePopulationManager popManager = new BeePopulationManager(this);
        var stats = popManager.getPopulationStats();

        var center = this.blockPosition();
        PollinationOptimizer optimizer = new PollinationOptimizer(this);
        var coverage = optimizer.calculateCoverage(center, 32);

        memory.getWorldKnowledge().updateBeeData(stats, coverage);
    }

    // Getters
    public BeePopulationManager getBeeManager() { return beeManager; }
    public HoneyHarvestManager getHoneyHarvestManager() { return honeyHarvestManager; }
}
```

### 5. Example Commands for Users

```
"find beehives"
-> {"action": "find_beehive", "parameters": {}}

"harvest honey from all hives"
-> {"action": "harvest_honey", "parameters": {}}

"collect honeycombs"
-> {"action": "harvest_honeycomb", "parameters": {}}

"build a beehive over there"
-> {"action": "build_beehive", "parameters": {"x": 100, "y": 64, "z": 200}}

"breed bees at the hive"
-> {"action": "breed_bees", "parameters": {}}

"optimize pollination for my farm"
-> {"action": "optimize_pollination", "parameters": {"farm_center": [0, 64, 0], "radius": 32}}

"how many bees do we have?"
-> Queries WorldKnowledge for bee summary
```

---

## Testing Checklist

### Detection
- [ ] Can find bee nests within 64 blocks
- [ ] Can find crafted beehives
- [ ] Correctly counts bees in each hive
- [ ] Accurately detects honey level (0-5)
- [ ] Identifies harvestable hives correctly

### Population Management
- [ ] Can breed bees with flowers
- [ ] Tracks population statistics accurately
- [ ] Detects free (un-hived) bees
- [ ] Can guide bees to hives
- [ ] Handles full hives (3 bees) correctly

### Harvesting
- [ ] Harvests honey bottles correctly
- [ ] Harvests honeycombs correctly
- [ ] Uses glass bottles from inventory
- [ ] Uses shears from inventory
- [ ] Places campfire for safety
- [ ] Does not anger bees when campfire present

### Hive Building
- [ ] Crafts beehives with correct recipe
- [ ] Consumes correct materials (6 planks + 3 honeycombs)
- [ ] Places hives at valid locations
- [ ] Validates placement (solid block below, air above)
- [ ] Handles inventory correctly

### Pollination
- [ ] Calculates coverage accurately
- [ ] Optimizes hive layout correctly
- [ ] Recommends best new hive locations
- [ ] Estimates growth bonus reasonably
- [ ] Accounts for 22-block pollination range

### Integration
- [ ] Actions registered in plugin
- [ ] LLM can invoke bee actions
- [ ] WorldKnowledge tracks bee data
- [ ] Chat messages are informative
- [ ] Progress tracking works

---

## Future Enhancements

### 1. Advanced Automation
- **Dispenser Systems**: Build redstone circuits with dispensers for automatic harvesting
- **Hopper Integration**: Auto-collect honey bottles/honeycombs into chests
- **Timer Circuits**: Harvest on schedule without foreman intervention

### 2. Bee Breeding Programs
- **Trait Selection**: Breed bees for specific behaviors (faster pollination, more honey)
- **Genetic Tracking**: Track bee lineages and traits across generations
- **Cross-breeding**: Hybrid bee types with modded bee systems

### 3. Crop Integration
- **Smart Farming**: Coordinate bee hives with specific crop types
- **Growth Tracking**: Measure actual growth acceleration from pollination
- **Seasonal Optimization**: Adjust hive layout based on crop cycles

### 4. Resource Processing
- **Honey Bottling**: Auto-convert honeycombs to honey bottles
- **Candle Production**: Craft candles from honeycombs for lighting
- **Copper Preservation**: Auto-apply honeycombs to prevent copper oxidation

### 5. Multi-Agent Coordination
- **Distributed Pollination**: Multiple agents manage different farm sections
- **Hive Networks**: Share bee population data across agents
- **Centralized Harvesting**: One agent manages all hives while others tend crops

### 6. Safety and Protection
- **Bee Health**: Monitor and heal injured bees
- **Hive Protection**: Protect hives from mobs and environmental damage
- **Weather Shelters**: Provide cover for hives during storms

---

## References and Sources

This design document incorporates information about vanilla Minecraft bee mechanics and Forge 1.20.1 API usage. The following sources were referenced:

### Bee Mechanics Documentation
- [Minecraft Bees: Spawning, Behavior, Attacking, Breeding & More](https://wiki.sportskeeda.com/minecraft/bees) - Comprehensive vanilla bee behavior
- [How to get Bees in Minecraft: Beehive and Bee Farm explained](https://www.rockpapershotgun.com/minecraft-bee-how-to-get-bees-in-minecraft-beehive-bee-farm-honeycomb) - Bee farming best practices

### Mod Development Resources
- [Productive Bees Mod for Forge 1.20.1](https://www.9minecraft.cn/productive-bees-mod/) - Advanced bee system reference
- [MoreBeeInfo Mod](https://www.9minecraft.net/) - Hive information display implementation

### Community Knowledge
- [Minecraft Bee Farming Guide (Chinese)](https://baijiahao.baidu.com/s?id=1674274349704156004) - Advanced farming techniques
- [Minecraft Bee Mechanics (Chinese)](https://www.bilibili.com/read/cv7064772/) - Pollination mechanics deep dive

---

**Document End**

This comprehensive design document provides a complete foundation for implementing honey farm automation in MineWright. The modular architecture supports incremental implementation and easy extension with advanced features.

Key implementation notes:
- Bee mechanics in vanilla Minecraft are well-documented and stable
- Forge 1.20.1 provides full access to beehive block states and entities
- The pollination bonus is a powerful incentive for integrating bees into crop farming
- Safety (campfires) is critical for sustainable honey harvesting
- Population management ensures long-term colony viability
