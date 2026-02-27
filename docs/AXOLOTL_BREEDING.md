# Axolotl Breeding AI for MineWright

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document
**Target:** Minecraft Forge 1.20.1

## Table of Contents

1. [Overview](#overview)
2. [Axolotl Mechanics](#axolotl-mechanics)
3. [Lush Cave Detection](#lush-cave-detection)
4. [Breeding Automation](#breeding-automation)
5. [Color Variant Management](#color-variant-management)
6. [Combat Assistance System](#combat-assistance-system)
7. [Architecture](#architecture)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Integration Guide](#integration-guide)

---

## Overview

The Axolotl Breeding AI system enables MineWright Foreman entities to automatically locate lush caves, breed axolotls for rare blue variants, manage tropical fish resources, and deploy axolotls as aquatic combat assistants.

### Key Features

- **Smart Cave Detection**: Locate lush caves using biome scanning and clay detection
- **Blue Breeding Program**: Automated breeding targeting the 1/1200 blue variant chance
- **Color Lineage Tracking**: Track inheritance patterns across generations
- **Tropical Fish Management**: Harvest and breed tropical fish for food
- **Combat Deployment**: Use axolotls against underwater guardians and drowned
- **Habitat Construction**: Build optimal axolotl breeding tanks

### Why Axolotls?

Axolotls are unique among Minecraft mobs:
- Only aquatic tamable mob
- Provide Regeneration I when helping kill mobs
- Can be picked up in buckets (preserves health/variant/age)
- Blue variant is highly sought after (cannot spawn naturally)
- Excellent for underwater combat (drowned farms, ocean monuments)

---

## Axolotl Mechanics

### Color Variants

| Color | Name | Spawn Chance | Breeding Chance |
|-------|------|--------------|-----------------|
| Pink | Lucy | 25% | Inheritable |
| Brown | Wild | 25% | Inheritable |
| Gold | Gold | 25% | Inheritable |
| Cyan | Cyan | 25% | Inheritable |
| **Blue** | **Blue** | **0% (mutation only)** | **1/1200 (0.083%)** |

### Breeding Requirements

**Food:** Bucket of Tropical Fish (not raw tropical fish items)
- Must use bucket on tropical fish to catch it
- Feed bucket to two adult axolotls
- Bucket becomes empty after feeding

**Breeding Process:**
1. Find two adult axolotls
2. Feed each a bucket of tropical fish
3. Axolotls enter love mode (hearts)
4. Baby spawns after a few seconds
5. Parents have 5-minute cooldown (Java Edition)
6. Baby takes 20 minutes to grow up (can be accelerated)

**Color Inheritance:**
```java
// Blue axolotl breeding logic
if (random.nextInt(1200) == 0) {
    // 0.083% chance of blue mutation
    babyColor = BLUE;
} else {
    // 99.917% chance - inherit from one parent
    babyColor = random.nextBoolean() ? parent1.color : parent2.color;
}

// Once you have a blue axolotl:
// Breeding blue with any color = 50% blue babies
```

### Spawn Conditions

Axolotls spawn naturally in:
- **Lush Caves** (primary habitat)
- Water bodies with clay blocks within 5 blocks below
- Dark underwater areas (light level 0)
- Y-levels: Typically below Y=0 (after Caves & Cliffs update)

### Combat Mechanics

**Attacked Mobs:**
- Drowned
- Guardians and Elder Guardians
- Fish (cod, salmon, tropical fish, pufferfish)
- Squid and Glow Squid
- Turtles

**Special Abilities:**
- **Play Dead**: When damaged below 50% health, may play dead for 10 seconds
  - While playing dead: Regenerates health, mobs stop attacking
  - Cannot play dead again until cooldown expires
- **Regeneration Boost**: When player kills axolotl's target, player gets Regeneration I

### Water Survival

- Can survive on land for ~5 minutes (6000 ticks)
- After 5 minutes: Takes 1 damage per second
- Must return to water to survive long-term
- Can be carried in buckets indefinitely

---

## Lush Cave Detection

### LushCaveDetector

Detecting lush caves requires scanning for characteristic blocks and biome markers.

```java
package com.minewright.detector;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;

import java.util.*;

public class LushCaveDetector {
    private final ForemanEntity foreman;
    private final Level level;

    // Lush cave indicator blocks
    private static final Set<CharacteristicBlock> INDICATORS = Set.of(
        new CharacteristicBlock(Blocks.AZALEA, 3),
        new CharacteristicBlock(Blocks.FLOWERING_AZALEA, 5),
        new CharacteristicBlock(Blocks.AZALEA_LEAVES, 2),
        new CharacteristicBlock(Blocks.SPORE_BLOSSOM, 10),
        new CharacteristicBlock(Blocks.MOSS_BLOCK, 1),
        new CharacteristicBlock(Blocks.MOSS_CARPET, 1),
        new CharacteristicBlock(Blocks.CLAY, 0.5f), // Axolotl spawn marker
        new CharacteristicBlock(Blocks.GLOW_LICHEN, 1),
        new CharacteristicBlock(Blocks.CAVE_VINES, 1),
        new CharacteristicBlock(Blocks.BIG_DRIPLEAF, 2)
    );

    public LushCaveDetector(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = foreman.level();
    }

    /**
     * Scan for lush caves within radius
     * Returns a list of potential cave centers sorted by confidence
     */
    public List<LushCaveLocation> findLushCaves(int radius) {
        List<LushCaveLocation> locations = new ArrayList<>();
        BlockPos center = foreman.blockPosition();

        // Horizontal scanning at different Y levels
        for (int y = -64; y < 64; y += 8) {
            for (int x = -radius; x <= radius; x += 16) {
                for (int z = -radius; z <= radius; z += 16) {
                    BlockPos checkPos = center.offset(x, y - center.getY(), z);

                    if (!level.isLoaded(checkPos)) continue;

                    double confidence = scanArea(checkPos, 16);
                    if (confidence > 0.3) {
                        locations.add(new LushCaveLocation(checkPos, confidence));
                    }
                }
            }
        }

        // Sort by confidence (highest first)
        locations.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return locations;
    }

    /**
     * Scan area around position for lush cave indicators
     * Returns confidence score (0.0 to 1.0)
     */
    private double scanArea(BlockPos center, int radius) {
        double score = 0;
        int blocksScanned = 0;

        BlockPos minPos = center.offset(-radius, -radius/2, -radius);
        BlockPos maxPos = center.offset(radius, radius/2, radius);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);

            for (CharacteristicBlock indicator : INDICATORS) {
                if (state.is(indicator.block())) {
                    score += indicator.weight();
                    blocksScanned++;
                    break;
                }
            }

            // Check for clay specifically (axolotl spawn requirement)
            if (state.is(Blocks.CLAY)) {
                // Check if water above clay
                BlockPos above = pos.above();
                if (level.getBlockState(above).getFluidState().isSource()) {
                    score += 2.0; // High confidence for axolotl habitat
                }
            }
        }

        // Normalize score
        if (blocksScanned == 0) return 0;
        return Math.min(1.0, score / (radius * radius * 0.5));
    }

    /**
     * Find optimal axolotl breeding location within a lush cave
     * Looks for water with clay below
     */
    public Optional<BlockPos> findBreedingLocation(LushCaveLocation cave) {
        BlockPos center = cave.position();
        int radius = 24;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -8, -radius),
            center.offset(radius, 8, radius)
        )) {
            // Check for water at foot level
            BlockState waterState = level.getBlockState(pos);
            if (!waterState.getFluidState().isSource()) continue;

            // Check for clay below (within 5 blocks)
            boolean hasClayBelow = false;
            for (int y = 1; y <= 5; y++) {
                if (level.getBlockState(pos.below(y)).is(Blocks.CLAY)) {
                    hasClayBelow = true;
                    break;
                }
            }

            if (hasClayBelow) {
                // Verify enough space for breeding
                if (hasBreedingSpace(pos, 5)) {
                    return Optional.of(pos);
                }
            }
        }

        return Optional.empty();
    }

    private boolean hasBreedingSpace(BlockPos center, int radius) {
        int waterBlocks = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -1, -radius),
            center.offset(radius, 2, radius)
        )) {
            if (level.getBlockState(pos).getFluidState().isSource()) {
                waterBlocks++;
            }
        }

        // Need at least 10 water blocks for comfortable breeding
        return waterBlocks >= 10;
    }

    /**
     * Check if current biome is lush caves
     */
    public boolean isInLushCaves() {
        var biome = level.getBiome(foreman.blockPosition());
        return biome.value() == Biomes.LUSH_CAVES;
    }

    /**
     * Count axolotls in nearby area
     */
    public int countNearbyAxolotls(int radius) {
        return (int) level.getEntitiesOfClass(
            net.minecraft.world.entity.animal.Axolotl.class,
            foreman.getBoundingBox().inflate(radius)
        ).stream()
        .filter(axolotl -> axolotl.isAlive())
        .count();
    }

    public record CharacteristicBlock(net.minecraft.world.level.block.Block block, float weight) {}

    public record LushCaveLocation(BlockPos position, double confidence) {
        public String getDescription() {
            if (confidence > 0.8) return "Definite lush cave";
            if (confidence > 0.6) return "Likely lush cave";
            if (confidence > 0.4) return "Possible lush cave";
            return "Weak lush cave indicator";
        }
    }
}
```

### Cave Navigation Strategy

```java
package com.minewright.navigation;

import com.minewright.detector.LushCaveDetector;
import com.minewright.detector.LushCaveDetector.LushCaveLocation;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public class LushCaveNavigator {
    private final ForemanEntity foreman;
    private final LushCaveDetector detector;

    public LushCaveNavigator(ForemanEntity foreman) {
        this.foreman = foreman;
        this.detector = new LushCaveDetector(foreman);
    }

    /**
     * Navigate to the nearest lush cave
     */
    public NavigationResult navigateToNearestCave() {
        foreman.sendChatMessage("Scanning for lush caves...");

        var caves = detector.findLushCaves(64);

        if (caves.isEmpty()) {
            return NavigationResult.failure("No lush caves detected within 64 blocks");
        }

        LushCaveLocation target = caves.get(0);
        foreman.sendChatMessage(String.format(
            "Found %s at %s (confidence: %.0f%%)",
            target.getDescription(),
            target.position(),
            target.confidence() * 100
        ));

        return navigateToPosition(target.position());
    }

    /**
     * Navigate to optimal breeding location
     */
    public NavigationResult navigateToBreedingSpot() {
        var caves = detector.findLushCaves(64);

        for (var cave : caves) {
            Optional<BlockPos> breedingSpot = detector.findBreedingLocation(cave);
            if (breedingSpot.isPresent()) {
                foreman.sendChatMessage("Found suitable breeding location");
                return navigateToPosition(breedingSpot.get());
            }
        }

        return NavigationResult.failure("No suitable breeding locations found");
    }

    private NavigationResult navigateToPosition(BlockPos target) {
        // Use foreman's navigation
        foreman.getNavigation().moveTo(
            target.getX(),
            target.getY(),
            target.getZ(),
            1.2
        );

        return NavigationResult.success("Navigating to " + target);
    }

    public record NavigationResult(boolean success, String message) {
        public static NavigationResult success(String message) {
            return new NavigationResult(true, message);
        }

        public static NavigationResult failure(String message) {
            return new NavigationResult(false, message);
        }
    }
}
```

---

## Breeding Automation

### AxolotlBreedingManager

Manages the breeding program with intelligent pair selection and blue variant tracking.

```java
package com.minewright.animal;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AxolotlBreedingManager {
    private final ForemanEntity foreman;

    // Breeding program state
    private final Map<UUID, AxolotlData> trackedAxolotls = new ConcurrentHashMap<>();
    private final Queue<BreedingPair> breedingQueue = new ConcurrentLinkedQueue<>();
    private AxolotlData blueAxolotl = null; // Track if we have a blue

    // Statistics
    private int totalBreedings = 0;
    private int blueMutations = 0;
    private int generations = 0;

    // Color variants
    public enum AxolotlColor {
        LUCY(0, "Lucy", "Pink"),
        WILD(1, "Wild", "Brown"),
        GOLD(2, "Gold", "Gold"),
        CYAN(3, "Cyan", "Cyan"),
        BLUE(4, "Blue", "Blue");

        final int id;
        final String name;
        final String displayName;

        AxolotlColor(int id, String name, String displayName) {
            this.id = id;
            this.name = name;
            this.displayName = displayName;
        }

        public static AxolotlColor fromId(int id) {
            return values()[Math.min(id, values().length - 1)];
        }
    }

    public AxolotlBreedingManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Scan and track all nearby axolotls
     */
    public void scanNearbyAxolotls(int radius) {
        var axolotls = foreman.level().getEntitiesOfClass(
            Axolotl.class,
            foreman.getBoundingBox().inflate(radius)
        );

        for (Axolotl axolotl : axolotls) {
            if (!trackedAxolotls.containsKey(axolotl.getUUID())) {
                AxolotlData data = new AxolotlData(
                    axolotl.getUUID(),
                    AxolotlColor.fromId(axolotl.getVariant().getId()),
                    !axolotl.isBaby(),
                    axolotl.isInLove(),
                    System.currentTimeMillis()
                );
                trackedAxolotls.put(axolotl.getUUID(), data);

                if (data.color() == AxolotlColor.BLUE && blueAxolotl == null) {
                    blueAxolotl = data;
                    foreman.sendChatMessage("Found a BLUE axolotl! " +
                        "Breeding this will give 50% blue babies.");
                }
            }
        }
    }

    /**
     * Find optimal breeding pair
     * Priority: Blue x Any > Same Color > Different Colors
     */
    public Optional<BreedingPair> findOptimalPair() {
        List<AxolotlData> adults = trackedAxolotls.values().stream()
            .filter(a -> a.isAdult() && !a.isInLove())
            .toList();

        if (adults.size() < 2) {
            return Optional.empty();
        }

        // Strategy 1: If we have a blue axolotl, breed it
        if (blueAxolotl != null && blueAxolotl.isAdult() && !blueAxolotl.isInLove()) {
            for (AxolotlData partner : adults) {
                if (!partner.uuid().equals(blueAxolotl.uuid())) {
                    return Optional.of(new BreedingPair(blueAxolotl, partner));
                }
            }
        }

        // Strategy 2: Breed for blue mutation (any pair works)
        // Prefer non-blue pairs to avoid redundancy
        List<AxolotlData> nonBlues = adults.stream()
            .filter(a -> a.color() != AxolotlColor.BLUE)
            .toList();

        if (nonBlues.size() >= 2) {
            // Try same-color pairs first (better tracking)
            Map<AxolotlColor, List<AxolotlData>> byColor = new HashMap<>();
            for (AxolotlData axolotl : nonBlues) {
                byColor.computeIfAbsent(axolotl.color(), k -> new ArrayList<>()).add(axolotl);
            }

            for (List<AxolotlData> sameColor : byColor.values()) {
                if (sameColor.size() >= 2) {
                    return Optional.of(new BreedingPair(sameColor.get(0), sameColor.get(1)));
                }
            }

            // No same-color pairs, use any pair
            return Optional.of(new BreedingPair(nonBlues.get(0), nonBlues.get(1)));
        }

        // Fallback: Use any two adults
        return Optional.of(new BreedingPair(adults.get(0), adults.get(1)));
    }

    /**
     * Execute breeding for a pair
     */
    public boolean breedPair(BreedingPair pair) {
        Axolotl axolotl1 = getAxolotlEntity(pair.parent1().uuid());
        Axolotl axolotl2 = getAxolotlEntity(pair.parent2().uuid());

        if (axolotl1 == null || axolotl2 == null) {
            return false; // Axolotls disappeared
        }

        if (!axolotl1.isAlive() || !axolotl2.isAlive()) {
            return false; // One died
        }

        // Check if we have tropical fish buckets
        int bucketsNeeded = 2;
        int bucketsAvailable = countTropicalFishBuckets();

        if (bucketsAvailable < bucketsNeeded) {
            foreman.sendChatMessage(String.format(
                "Need %d tropical fish buckets, have %d. Fishing...",
                bucketsNeeded, bucketsAvailable
            ));
            return false;
        }

        // Feed both axolotls
        boolean success1 = feedAxolotl(axolotl1);
        boolean success2 = feedAxolotl(axolotl2);

        if (success1 && success2) {
            totalBreedings++;
            pair.setBreedingTime(System.currentTimeMillis());

            foreman.sendChatMessage(String.format(
                "Bred %s x %s. Baby due in 20 minutes. (Breeding #%d, Blues: %d)",
                pair.parent1().color().displayName(),
                pair.parent2().color().displayName(),
                totalBreedings,
                blueMutations
            ));

            return true;
        }

        return false;
    }

    /**
     * Feed a tropical fish bucket to an axolotl
     */
    private boolean feedAxolotl(Axolotl axolotl) {
        // In a real implementation, we'd check foreman's inventory
        // for tropical fish buckets and use them

        // For now, simulate the feeding
        axolotl.setInLoveTime(600); // Put in love mode

        // Remove bucket from inventory (pseudo-code)
        // foreman.getInventory().removeItem(new ItemStack(Items.TROPICAL_FISH_BUCKET));

        return true;
    }

    /**
     * Check for and track new babies
     */
    public void checkForBabies() {
        var nearby = foreman.level().getEntitiesOfClass(
            Axolotl.class,
            foreman.getBoundingBox().inflate(16)
        );

        for (Axolotl axolotl : nearby) {
            UUID uuid = axolotl.getUUID();

            if (axolotl.isBaby() && !trackedAxolotls.containsKey(uuid)) {
                // New baby detected!
                AxolotlColor color = AxolotlColor.fromId(axolotl.getVariant().getId());

                AxolotlData babyData = new AxolotlData(
                    uuid,
                    color,
                    false, // isAdult
                    false,
                    System.currentTimeMillis()
                );

                trackedAxolotls.put(uuid, babyData);

                if (color == AxolotlColor.BLUE) {
                    blueMutations++;
                    blueAxolotl = babyData;

                    foreman.sendChatMessage(String.format(
                        "BLUE AXOLOTL BORN! After %d breedings! (Total blues: %d)",
                        totalBreedings,
                        blueMutations
                    ));

                    // Celebrate!
                    foreman.sendChatMessage("Now we can breed 50% blue babies!");
                }
            }
        }
    }

    /**
     * Main tick method for breeding manager
     */
    public void tick() {
        // Rescan periodically
        if (foreman.tickCount % 100 == 0) {
            scanNearbyAxolotls(32);
        }

        // Check for babies
        if (foreman.tickCount % 20 == 0) {
            checkForBabies();
        }

        // Process breeding queue
        if (!breedingQueue.isEmpty() && foreman.tickCount % 60 == 0) {
            BreedingPair pair = breedingQueue.peek();

            // Check cooldown (5 minutes = 6000 ticks)
            if (System.currentTimeMillis() - pair.breedingTime() > 300000) {
                breedingQueue.remove();

                if (breedPair(pair)) {
                    // Success, keep pair in queue for next breeding
                    breedingQueue.add(pair);
                }
            }
        }
    }

    private Axolotl getAxolotlEntity(UUID uuid) {
        return foreman.level().getEntitiesOfClass(
            Axolotl.class,
            foreman.getBoundingBox().inflate(64)
        ).stream()
        .filter(a -> a.getUUID().equals(uuid))
        .findFirst()
        .orElse(null);
    }

    private int countTropicalFishBuckets() {
        // Check foreman's inventory
        int count = 0;
        for (ItemStack stack : foreman.getInventory().items) {
            if (stack.is(Items.TROPICAL_FISH_BUCKET)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // Data classes
    public record AxolotlData(
        UUID uuid,
        AxolotlColor color,
        boolean isAdult,
        boolean isInLove,
        long discoveredTime
    ) {
        public boolean isAdult() {
            return isAdult;
        }

        public boolean isInLove() {
            return isInLove;
        }
    }

    public static class BreedingPair {
        private final AxolotlData parent1;
        private final AxolotlData parent2;
        private long breedingTime = 0;
        private int babiesProduced = 0;

        public BreedingPair(AxolotlData parent1, AxolotlData parent2) {
            this.parent1 = parent1;
            this.parent2 = parent2;
        }

        public AxolotlData parent1() { return parent1; }
        public AxolotlData parent2() { return parent2; }
        public long breedingTime() { return breedingTime; }
        public int babiesProduced() { return babiesProduced; }

        public void setBreedingTime(long time) {
            this.breedingTime = time;
        }

        public void recordBaby() {
            this.babiesProduced++;
        }

        public double getBlueChance() {
            if (parent1.color() == AxolotlColor.BLUE || parent2.color() == AxolotlColor.BLUE) {
                return 0.5; // 50% if one parent is blue
            }
            return 1.0 / 1200.0; // 0.083% mutation chance
        }
    }
}
```

### Tropical Fish Supply System

```java
package com.minewright.animal;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.concurrent.atomic.AtomicInteger;

public class TropicalFishSupplier {
    private final ForemanEntity foreman;
    private final AtomicInteger bucketsCollected = new AtomicInteger(0);

    public TropicalFishSupplier(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Collect tropical fish by fishing
     */
    public boolean collectTropicalFish(int targetCount) {
        int current = bucketsCollected.get();

        if (current >= targetCount) {
            return true; // Already have enough
        }

        foreman.sendChatMessage(String.format(
            "Fishing for tropical fish... (%d/%d)",
            current, targetCount
        ));

        // Find water nearby
        Optional<BlockPos> waterSpot = findNearestWater();
        if (waterSpot.isEmpty()) {
            foreman.sendChatMessage("No water found for fishing");
            return false;
        }

        // Move to fishing spot
        foreman.getNavigation().moveTo(
            waterSpot.get().getX(),
            waterSpot.get().getY(),
            waterSpot.get().getZ(),
            1.2
        );

        // Fish (simplified - in reality, would use fishing rod)
        return fishForTropicalFish(targetCount - current);
    }

    /**
     * Catch tropical fish with water buckets
     */
    public boolean catchWildTropicalFish(int targetCount) {
        var fish = foreman.level().getEntitiesOfClass(
            TropicalFish.class,
            foreman.getBoundingBox().inflate(32)
        );

        if (fish.isEmpty()) {
            foreman.sendChatMessage("No tropical fish nearby to catch");
            return false;
        }

        int caught = 0;
        for (TropicalFish f : fish) {
            if (caught >= targetCount) break;
            if (!f.isAlive()) continue;

            // In a real implementation, use a bucket on the fish
            // This simulates catching it
            bucketsCollected.incrementAndGet();
            caught++;
            f.discard();
        }

        if (caught > 0) {
            foreman.sendChatMessage(String.format(
                "Caught %d tropical fish with buckets",
                caught
            ));
            return true;
        }

        return false;
    }

    private boolean fishForTropicalFish(int needed) {
        // Simulated fishing - in reality, use fishing rod and wait for bites
        // Tropical fish are relatively rare from fishing

        double catchRate = 0.02; // 2% chance per tick (very generous)
        int attempts = 0;
        int maxAttempts = 600; // 30 seconds

        while (bucketsCollected.get() < needed && attempts < maxAttempts) {
            if (foreman.getRandom().nextDouble() < catchRate) {
                bucketsCollected.incrementAndGet();
                foreman.sendChatMessage(String.format(
                    "Caught a tropical fish! (%d/%d)",
                    bucketsCollected.get(),
                    needed
                ));
            }
            attempts++;
        }

        return bucketsCollected.get() >= needed;
    }

    private Optional<BlockPos> findNearestWater() {
        BlockPos pos = foreman.blockPosition();

        for (int x = -16; x <= 16; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -16; z <= 16; z++) {
                    BlockPos check = pos.offset(x, y, z);
                    if (foreman.level().getBlockState(check).getFluidState().isSource()) {
                        return Optional.of(check);
                    }
                }
            }
        }

        return Optional.empty();
    }

    public int getBucketCount() {
        return bucketsCollected.get();
    }
}
```

---

## Color Variant Management

### AxolotlLineageTracker

Tracks breeding history and color inheritance patterns.

```java
package com.minewright.animal;

import java.util.*;

public class AxolotlLineageTracker {
    private final Map<UUID, AxolotlGenealogy> genealogy = new HashMap<>();

    public void registerBreeding(UUID parent1, UUID parent2, UUID baby, AxolotlBreedingManager.AxolotlColor color) {
        AxolotlGenealogy p1 = genealogy.getOrDefault(parent1, new AxolotlGenealogy(parent1));
        AxolotlGenealogy p2 = genealogy.getOrDefault(parent2, new AxolotlGenealogy(parent2));

        AxolotlGenealogy child = new AxolotlGenealogy(baby);
        child.color = color;
        child.parent1 = parent1;
        child.parent2 = parent2;
        child.generation = Math.max(p1.generation, p2.generation) + 1;

        genealogy.put(baby, child);

        p1.children.add(baby);
        p2.children.add(baby);
        genealogy.put(parent1, p1);
        genealogy.put(parent2, p2);
    }

    public String getLineageSummary(UUID axolotl) {
        AxolotlGenealogy gene = genealogy.get(axolotl);
        if (gene == null) return "Unknown axolotl";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Generation %d %s axolotl\n",
            gene.generation, gene.color.displayName()));

        if (gene.parent1 != null) {
            sb.append("  Parents: ");
            AxolotlGenealogy p1 = genealogy.get(gene.parent1);
            AxolotlGenealogy p2 = genealogy.get(gene.parent2);
            if (p1 != null && p2 != null) {
                sb.append(String.format("%s x %s",
                    p1.color.displayName(), p2.color.displayName()));
            }
        }

        if (!gene.children.isEmpty()) {
            sb.append(String.format("\n  Offspring: %d babies", gene.children.size()));
        }

        return sb.toString();
    }

    public int getGeneration(UUID axolotl) {
        AxolotlGenealogy gene = genealogy.get(axolotl);
        return gene != null ? gene.generation : 0;
    }

    public List<UUID> getLineageToBlue(UUID axolotl) {
        List<UUID> path = new ArrayList<>();
        findPathToBlue(axolotl, path, new HashSet<>());
        return path;
    }

    private boolean findPathToBlue(UUID current, List<UUID> path, Set<UUID> visited) {
        if (visited.contains(current)) return false;
        visited.add(current);

        AxolotlGenealogy gene = genealogy.get(current);
        if (gene == null) return false;

        if (gene.color == AxolotlBreedingManager.AxolotlColor.BLUE) {
            path.add(current);
            return true;
        }

        if (gene.parent1 != null && findPathToBlue(gene.parent1, path, visited)) {
            path.add(current);
            return true;
        }

        if (gene.parent2 != null && findPathToBlue(gene.parent2, path, visited)) {
            path.add(current);
            return true;
        }

        return false;
    }

    public static class AxolotlGenealogy {
        final UUID uuid;
        AxolotlBreedingManager.AxolotlColor color = AxolotlBreedingManager.AxolotlColor.LUCY;
        UUID parent1 = null;
        UUID parent2 = null;
        List<UUID> children = new ArrayList<>();
        int generation = 0;

        public AxolotlGenealogy(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
```

---

## Combat Assistance System

### AxolotlSquadManager

Manages a squad of axolotls for underwater combat operations.

```java
package com.minewright.combat;

import com.minewright.entity.ForemanEntity;
import com.minewright.animal.AxolotlBreedingManager;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.Axolotl;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AxolotlSquadManager {
    private final ForemanEntity foreman;

    // Squad composition
    private final Map<UUID, AxolotlSoldier> squad = new ConcurrentHashMap<>();
    private SquadMode mode = SquadMode.DEFENSIVE;

    // Combat targets
    private final Set<UUID> prioritizedTargets = ConcurrentHashMap.newKeySet();

    public enum SquadMode {
        FOLLOW("Follow"),         // Axolotls follow foreman
        PATROL("Patrol"),         // Axolotls patrol area
        GUARD("Guard"),           // Axolotls guard specific location
        ASSAULT("Assault"),       // Axolotls attack nearby hostile mobs
        PASSIVE("Passive");       // Axolotls don't attack

        final String displayName;
        SquadMode(String displayName) {
            this.displayName = displayName;
        }
    }

    public AxolotlSquadManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Add an axolotl to the combat squad
     */
    public boolean recruitToSquad(Axolotl axolotl) {
        if (squad.containsKey(axolotl.getUUID())) {
            return false; // Already recruited
        }

        AxolotlSoldier soldier = new AxolotlSoldier(
            axolotl.getUUID(),
            axolotl.getVariant().getId(),
            System.currentTimeMillis()
        );

        squad.put(axolotl.getUUID(), soldier);

        foreman.sendChatMessage(String.format(
            "Recruited %s axolotl to squad! (Squad size: %d)",
            getVariantName(axolotl.getVariant().getId()),
            squad.size()
        ));

        return true;
    }

    /**
     * Deploy squad for assault mode
     */
    public void deployAssault(int searchRadius) {
        mode = SquadMode.ASSAULT;

        foreman.sendChatMessage(String.format(
            "Squad deployed in ASSAULT mode! (%d axolotls)",
            squad.size()
        ));

        // Find targets and assign to squad members
        findAndAssignTargets(searchRadius);
    }

    /**
     * Find hostile mobs and assign to axolotls
     */
    private void findAndAssignTargets(int radius) {
        // Targets axolotls are good against
        List<Drowned> drowned = foreman.level().getEntitiesOfClass(
            Drowned.class,
            foreman.getBoundingBox().inflate(radius)
        );

        List<Guardian> guardians = foreman.level().getEntitiesOfClass(
            Guardian.class,
            foreman.getBoundingBox().inflate(radius)
        );

        // Assign targets to squad members
        int squadIndex = 0;
        List<UUID> squadMembers = new ArrayList<>(squad.keySet());

        for (Drowned target : drowned) {
            if (squadIndex >= squadMembers.size()) break;

            assignTarget(squadMembers.get(squadIndex), target.getUUID());
            squadIndex++;
        }

        for (Guardian target : guardians) {
            if (squadIndex >= squadMembers.size()) break;

            assignTarget(squadMembers.get(squadIndex), target.getUUID());
            squadIndex++;
        }

        foreman.sendChatMessage(String.format(
            "Assigned %d targets to squad",
            drowned.size() + guardians.size()
        ));
    }

    /**
     * Assign a specific target to an axolotl
     */
    private void assignTarget(UUID axolotlUuid, UUID targetUuid) {
        Axolotl axolotl = getAxolotlEntity(axolotlUuid);
        if (axolotl == null) return;

        // Find target entity
        var target = foreman.level().getEntities(
            null,
            foreman.getBoundingBox().inflate(32)
        ).stream()
        .filter(e -> e.getUUID().equals(targetUuid))
        .findFirst()
        .orElse(null);

        if (target != null && axolotl instanceof Axolotl) {
            // Set axolotl's target
            axolotl.setTarget((net.minecraft.world.entity.LivingEntity) target);

            AxolotlSoldier soldier = squad.get(axolotlUuid);
            if (soldier != null) {
                soldier.currentTarget = targetUuid;
                soldier.targetsEliminated++;
            }
        }
    }

    /**
     * Tick the squad - update AI and track kills
     */
    public void tick() {
        for (Map.Entry<UUID, AxolotlSoldier> entry : squad.entrySet()) {
            Axolotl axolotl = getAxolotlEntity(entry.getKey());
            AxolotlSoldier soldier = entry.getValue();

            if (axolotl == null || !axolotl.isAlive()) {
                // Axolotl died or despawned
                if (soldier.deathTime == 0) {
                    soldier.deathTime = System.currentTimeMillis();
                    foreman.sendChatMessage("Squad member lost in combat!");
                }
                continue;
            }

            // Check if target is still alive
            if (soldier.currentTarget != null) {
                var target = foreman.level().getEntity(soldier.currentTarget);
                if (target == null || !target.isAlive()) {
                    // Target defeated
                    soldier.currentTarget = null;
                    soldier.killsConfirmed++;

                    foreman.sendChatMessage(String.format(
                        "Target eliminated! (Squad kills: %d)",
                        soldier.killsConfirmed
                    ));

                    // Find new target if in assault mode
                    if (mode == SquadMode.ASSAULT) {
                        findAndAssignTargets(24);
                    }
                }
            }

            // Update axolotl behavior based on mode
            updateAxolotlBehavior(axolotl, soldier);
        }
    }

    /**
     * Update axolotl AI based on squad mode
     */
    private void updateAxolotlBehavior(Axolotl axolotl, AxolotlSoldier soldier) {
        switch (mode) {
            case FOLLOW -> {
                // Follow the foreman
                double distance = axolotl.distanceTo(foreman);
                if (distance > 8.0) {
                    axolotl.getNavigation().moveTo(foreman, 1.0);
                }
            }

            case GUARD -> {
                // Stay in guard position
                if (soldier.guardPosition != null) {
                    double distance = axolotl.blockPosition().distSqr(soldier.guardPosition);
                    if (distance > 16.0) {
                        axolotl.getNavigation().moveTo(
                            soldier.guardPosition.getX(),
                            soldier.guardPosition.getY(),
                            soldier.guardPosition.getZ(),
                            0.8
                        );
                    }
                }
            }

            case ASSAULT -> {
                // Already handled by target assignment
                // Axolotls will attack their assigned targets
            }
        }

        // Ensure axolotls don't wander too far on land
        if (!axolotl.isUnderWater()) {
            // Guide back to water
            var nearestWater = findNearestWater(axolotl);
            if (nearestWater.isPresent()) {
                axolotl.getNavigation().moveTo(
                    nearestWater.get().getX(),
                    nearestWater.get().getY(),
                    nearestWater.get().getZ(),
                    1.2
                );
            }
        }
    }

    /**
     * Heal squad members (when they play dead)
     */
    public void healSquad() {
        for (UUID uuid : squad.keySet()) {
            Axolotl axolotl = getAxolotlEntity(uuid);
            if (axolotl != null && axolotl.isAlive()) {
                // Axolotls heal when playing dead
                // Just ensure they're in water
                if (!axolotl.isUnderWater()) {
                    var nearestWater = findNearestWater(axolotl);
                    if (nearestWater.isPresent()) {
                        axolotl.getNavigation().moveTo(
                            nearestWater.get().getX(),
                            nearestWater.get().getY(),
                            nearestWater.get().getZ(),
                            1.2
                        );
                    }
                }
            }
        }
    }

    /**
     * Set squad mode
     */
    public void setSquadMode(SquadMode mode) {
        this.mode = mode;
        foreman.sendChatMessage("Squad mode: " + mode.displayName);
    }

    /**
     * Get squad statistics
     */
    public String getSquadStats() {
        int active = 0;
        int kills = 0;

        for (AxolotlSoldier soldier : squad.values()) {
            if (soldier.deathTime == 0) {
                active++;
            }
            kills += soldier.killsConfirmed;
        }

        return String.format(
            "Squad: %d active, %d total, %d kills",
            active, squad.size(), kills
        );
    }

    private Axolotl getAxolotlEntity(UUID uuid) {
        return foreman.level().getEntitiesOfClass(
            Axolotl.class,
            foreman.getBoundingBox().inflate(64)
        ).stream()
        .filter(a -> a.getUUID().equals(uuid))
        .findFirst()
        .orElse(null);
    }

    private Optional<net.minecraft.core.BlockPos> findNearestWater(Axolotl axolotl) {
        // Implementation similar to earlier examples
        return Optional.empty();
    }

    private String getVariantName(int variantId) {
        return switch (variantId) {
            case 0 -> "Lucy (Pink)";
            case 1 -> "Wild (Brown)";
            case 2 -> "Gold";
            case 3 -> "Cyan";
            case 4 -> "Blue";
            default -> "Unknown";
        };
    }

    public static class AxolotlSoldier {
        final UUID uuid;
        final int variantId;
        final long recruitmentTime;

        UUID currentTarget = null;
        net.minecraft.core.BlockPos guardPosition = null;
        int killsConfirmed = 0;
        int targetsEliminated = 0;
        long deathTime = 0;

        public AxolotlSoldier(UUID uuid, int variantId, long recruitmentTime) {
            this.uuid = uuid;
            this.variantId = variantId;
            this.recruitmentTime = recruitmentTime;
        }
    }
}
```

---

## Architecture

### Package Structure

```
com.minewright.axolotl/
├── detection/
│   ├── LushCaveDetector.java       # Detect lush caves
│   └── AxolotlScanner.java         # Scan for axolotls
├── breeding/
│   ├── AxolotlBreedingManager.java # Main breeding logic
│   ├── TropicalFishSupplier.java   # Manage fish buckets
│   └── AxolotlLineageTracker.java  # Track breeding history
├── combat/
│   ├── AxolotlSquadManager.java    # Combat squad management
│   └── AxolotlTactics.java         # Combat strategy
├── habitat/
│   ├── AxolotlHabitatBuilder.java  # Build breeding tanks
│   └── WaterFlowManager.java       # Manage water systems
└── actions/
    ├── FindLushCaveAction.java     # Locate caves
    ├── BreedAxolotlAction.java     # Execute breeding
    ├── CollectTropicalFishAction.java # Gather fish
    ├── AxolotlAssaultAction.java   # Combat deployment
    └── BuildAxolotlHabitatAction.java # Build tanks
```

### Class Diagram

```
┌─────────────────────────────────┐
│    LushCaveDetector             │
├─────────────────────────────────┤
│ + findLushCaves(radius)         │
│ + findBreedingLocation(cave)    │
│ + isInLushCaves()               │
│ + countNearbyAxolotls()         │
└─────────────────────────────────┘
             ▲
             │
┌─────────────────────────────────┐
│ AxolotlBreedingManager          │
├─────────────────────────────────┤
│ + scanNearbyAxolotls()          │
│ + findOptimalPair()             │
│ + breedPair(pair)               │
│ + checkForBabies()              │
│ + tick()                        │
└─────────────────────────────────┘
             ▲
             │
┌─────────────────────────────────┐
│   AxolotlLineageTracker         │
├─────────────────────────────────┤
│ + registerBreeding(p1, p2, baby)│
│ + getLineageSummary(uuid)       │
│ + getGeneration(uuid)           │
└─────────────────────────────────┘
             ▲
             │
┌─────────────────────────────────┐
│  AxolotlSquadManager            │
├─────────────────────────────────┤
│ + recruitToSquad(axolotl)       │
│ + deployAssault(radius)         │
│ + setSquadMode(mode)            │
│ + tick()                        │
└─────────────────────────────────┘
```

---

## Code Examples

### Example 1: Finding Lush Caves

```java
// User command: "find a lush cave"
Task task = new Task("find_lush_cave", Map.of(
    "radius", 64
));

FindLushCaveAction action = new FindLushCaveAction(foreman, task);
action.start();

while (!action.isComplete()) {
    action.tick();
    Thread.sleep(50); // Tick loop
}

ActionResult result = action.getResult();
if (result.success()) {
    System.out.println("Found lush cave: " + result.message());
}
```

### Example 2: Breeding for Blue

```java
// Initialize breeding manager
AxolotlBreedingManager breedingManager = new AxolotlBreedingManager(foreman);

// Scan for axolotls
breedingManager.scanNearbyAxolotls(32);

// Find optimal pair
Optional<BreedingPair> pair = breedingManager.findOptimalPair();
if (pair.isPresent()) {
    // Breed them
    if (breedingManager.breedPair(pair.get())) {
        System.out.println("Breeding started! Wait 20 minutes for baby.");
    }
}

// Run breeding loop
while (true) {
    breedingManager.tick();
    Thread.sleep(50); // One tick

    // Check every 5 minutes
    if (foreman.tickCount % 6000 == 0) {
        breedingManager.checkForBabies();
    }
}
```

### Example 3: Combat Deployment

```java
// Create squad manager
AxolotlSquadManager squad = new AxolotlSquadManager(foreman);

// Recruit nearby axolotls
List<Axolotl> nearby = foreman.level().getEntitiesOfClass(
    Axolotl.class,
    foreman.getBoundingBox().inflate(32)
);

for (Axolotl axolotl : nearby) {
    if (axolotl.isAlive() && !axolotl.isBaby()) {
        squad.recruitToSquad(axolotl);
    }
}

// Deploy for assault
squad.deployAssault(24);

// Run combat loop
while (squad.getSquadStats().contains("active")) {
    squad.tick();
    Thread.sleep(50);

    // Heal squad periodically
    if (foreman.tickCount % 200 == 0) {
        squad.healSquad();
    }
}
```

### Example 4: Building Breeding Habitat

```java
// Build axolotl breeding tank
Task buildTask = new Task("build_axolotl_habitat", Map.of(
    "centerX", 100,
    "centerY", 64,
    "centerZ", 100,
    "size", 12,
    "capacity", 10
));

BuildAxolotlHabitatAction buildAction =
    new BuildAxolotlHabitatAction(foreman, buildTask);

buildAction.start();

while (!buildAction.isComplete()) {
    buildAction.tick();
    Thread.sleep(50);
}

System.out.println("Habitat complete: " + buildAction.getResult().message());
```

---

## Implementation Roadmap

### Phase 1: Detection & Navigation (Week 1)

**Tasks:**
- [ ] Implement `LushCaveDetector`
- [ ] Add clay detection for axolotl habitats
- [ ] Create `LushCaveNavigator` for pathfinding
- [ ] Implement `FindLushCaveAction`
- [ ] Add lush cave location to `WorldKnowledge`

**Testing:**
- [ ] Can detect lush caves within 64 blocks
- [ ] Can identify axolotl spawning locations
- [ ] Navigation reaches target correctly

### Phase 2: Breeding System (Week 2-3)

**Tasks:**
- [ ] Implement `AxolotlBreedingManager`
- [ ] Create `TropicalFishSupplier`
- [ ] Add `BreedAxolotlAction`
- [ ] Implement color variant tracking
- [ ] Create breeding statistics

**Testing:**
- [ ] Can detect and track axolotls
- [ ] Finds optimal breeding pairs
- [ ] Successfully feeds axolotls
- [ ] Tracks baby axolotls correctly
- [ ] Detects blue mutations

### Phase 3: Lineage & Genetics (Week 3-4)

**Tasks:**
- [ ] Implement `AxolotlLineageTracker`
- [ ] Add generation tracking
- [ ] Create breeding history display
- [ ] Implement blue chance calculator
- [ ] Add lineage visualization

**Testing:**
- [ ] Tracks parent-child relationships
- [ ] Calculates generations correctly
- [ ] Shows breeding history
- [ ] Identifies paths to blue axolotls

### Phase 4: Combat System (Week 4-5)

**Tasks:**
- [ ] Implement `AxolotlSquadManager`
- [ ] Create squad mode system
- [ ] Add target assignment logic
- [ ] Implement kill tracking
- [ ] Create `AxolotlAssaultAction`

**Testing:**
- [ ] Can recruit axolotls to squad
- [ ] Follow mode works correctly
- [ ] Assault mode targets enemies
- [ ] Guard mode maintains position
- [ ] Tracks combat statistics

### Phase 5: Habitat Construction (Week 5-6)

**Tasks:**
- [ ] Implement `AxolotlHabitatBuilder`
- [ ] Create breeding tank designs
- [ ] Add water flow management
- [ ] Implement `BuildAxolotlHabitatAction`
- [ ] Add automatic water refill

**Testing:**
- [ ] Tanks hold water correctly
- [ ] Axolotls can spawn in tanks
- [ ] Breeding works in constructed habitats
- [ ] Water level management works

### Phase 6: Integration (Week 6-7)

**Tasks:**
- [ ] Register all axolotl actions in `CoreActionsPlugin`
- [ ] Update `PromptBuilder` with axolotl commands
- [ ] Add axolotl context to `WorldKnowledge`
- [ ] Integrate with existing animal handling
- [ ] Create comprehensive tests

**Testing:**
- [ ] All actions registered correctly
- [ ] LLM understands axolotl commands
- [ ] Integration with other systems works
- [ ] No conflicts with existing code

### Phase 7: Polish & Optimization (Week 7-8)

**Tasks:**
- [ ] Optimize cave scanning performance
- [ ] Add GUI for breeding stats
- [ ] Implement blue axolotl celebration
- [ ] Add progress notifications
- [ ] Create documentation and tutorials

**Testing:**
- [ ] Performance is acceptable
- [ ] GUI displays correctly
- [ ] Notifications work
- [ ] Documentation is complete

---

## Integration Guide

### 1. Register Axolotl Actions

Add to `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    LOGGER.info("Loading CoreActionsPlugin v{}", VERSION);

    // ... existing registrations ...

    // Axolotl Actions
    registry.register("find_lush_cave",
        (foreman, task, ctx) -> new FindLushCaveAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("breed_axolotl",
        (foreman, task, ctx) -> new BreedAxolotlAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("collect_tropical_fish",
        (foreman, task, ctx) -> new CollectTropicalFishAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("axolotl_assault",
        (foreman, task, ctx) -> new AxolotlAssaultAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("build_axolotl_habitat",
        (foreman, task, ctx) -> new BuildAxolotlHabitatAction(foreman, task),
        priority, PLUGIN_ID);

    LOGGER.info("CoreActionsPlugin loaded {} actions", registry.getActionCount());
}
```

### 2. Update PromptBuilder

Add axolotl-related actions to system prompt:

```java
String axolotlActions = """
ACTIONS:
- find_lush_cave: {"radius": 64} - Find lush caves for axolotl habitats
- breed_axolotl: {"target": "blue", "pairs": 1} - Breed axolotls for blue variant
- collect_tropical_fish: {"amount": 10} - Gather tropical fish buckets
- axolotl_assault: {"mode": "assault", "radius": 24} - Deploy axolotl squad
- build_axolotl_habitat: {"size": 12, "capacity": 10} - Build breeding tank

STRATEGIES:
- Blue axolotls have 1/1200 mutation chance from normal breeding
- Once you have a blue axolotl, breeding it gives 50% blue babies
- Axolotls require buckets of tropical fish to breed (not raw fish)
- Axolotls are excellent against drowned, guardians, and underwater mobs
- Axolotls provide Regeneration I when helping kill mobs
""";
```

### 3. Add to WorldKnowledge

```java
public class WorldKnowledge {
    // ... existing code ...

    private boolean nearLushCave = false;
    private int nearbyAxolotlCount = 0;
    private boolean hasBlueAxolotl = false;

    public void updateAxolotlKnowledge(LushCaveDetector detector) {
        this.nearLushCave = detector.isInLushCaves();
        this.nearbyAxolotlCount = detector.countNearbyAxolotls(32);
    }

    public String getAxolotlSummary() {
        if (nearbyAxolotlCount == 0) {
            return "No axolotls nearby";
        }
        return String.format("%d axolotls near %s lush cave",
            nearbyAxolotlCount,
            nearLushCave ? "a" : "no");
    }

    public boolean isNearLushCave() { return nearLushCave; }
    public int getNearbyAxolotlCount() { return nearbyAxolotlCount; }
    public void setHasBlueAxolotl(boolean has) { this.hasBlueAxolotl = has; }
}
```

### 4. Example Commands

```
"find a lush cave"
-> {"reasoning": "Scanning for lush cave biome", "plan": "Locate axolotl habitat", "tasks": [{"action": "find_lush_cave", "parameters": {"radius": 64}}]}

"breed axolotls for a blue one"
-> {"reasoning": "Blue axolotls require breeding", "plan": "Start blue breeding program", "tasks": [{"action": "collect_tropical_fish", "parameters": {"amount": 20}}, {"action": "breed_axolotl", "parameters": {"target": "blue", "pairs": 2}}]}

"deploy axolotls against drowned"
-> {"reasoning": "Axolotls effective against drowned", "plan": "Combat deployment", "tasks": [{"action": "axolotl_assault", "parameters": {"mode": "assault", "radius": 24}}]}

"build an axolotl breeding tank"
-> {"reasoning": "Need habitat for breeding", "plan": "Construct breeding tank", "tasks": [{"action": "build_axolotl_habitat", "parameters": {"size": 12, "capacity": 10}}]}
```

---

## Testing Checklist

### Lush Cave Detection
- [ ] Can detect lush caves within 64 blocks
- [ ] Correctly identifies axolotl spawning locations
- [ ] Finds clay blocks underwater
- [ ] Calculates confidence scores accurately
- [ ] Navigation reaches detected caves

### Breeding System
- [ ] Detects and tracks all nearby axolotls
- [ ] Correctly identifies color variants
- [ ] Finds optimal breeding pairs
- [ ] Prioritizes blue axolotl breeding
- [ ] Successfully feeds axolotls with tropical fish buckets
- [ ] Tracks baby axolotls when spawned
- [ ] Detects blue mutations correctly
- [ ] Respects 5-minute breeding cooldown

### Lineage Tracking
- [ ] Records parent-child relationships
- [ ] Calculates generations correctly
- [ ] Shows breeding history
- [ ] Identifies paths to blue axolotls
- [ ] Handles axolotl deaths/despawning

### Combat System
- [ ] Can recruit axolotls to squad
- [ ] Follow mode works correctly
- [ ] Guard mode maintains position
- [ ] Assault mode targets hostile mobs
- [ ] Tracks combat statistics
- [ ] Heals injured squad members

### Habitat Construction
- [ ] Builds water-tight tanks
- [ ] Creates optimal breeding conditions
- [ ] Includes proper water flow
- [ ] Axolotls can spawn and breed in tanks
- [ ] Water level management works

---

## Performance Considerations

### Optimization Strategies

1. **Lazy Scanning**: Don't scan every tick
   - Scan for axolotls every 5 seconds (100 ticks)
   - Check for babies every 1 second (20 ticks)
   - Process breeding queue every 3 seconds (60 ticks)

2. **Spatial Partitioning**: Use chunk-based tracking
   - Only scan loaded chunks
   - Cache scan results per chunk
   - Invalidate cache when chunks unload

3. **Batch Processing**: Handle multiple axolotls together
   - Feed multiple pairs in same tick
   - Collect all tropical fish at once
   - Build entire habitats in phases

4. **Memory Management**: Limit tracking scope
   - Only track axolotls within 64 blocks
   - Remove old lineage data after 10 generations
   - Purge data for unloaded entities

### Expected Performance

| Operation | Frequency | Duration | Impact |
|-----------|-----------|----------|--------|
| Cave Scan | Once | 2-5 seconds | High (one-time) |
| Axolotl Scan | Every 5s | <100ms | Low |
| Breeding | Every 5m | <50ms | Negligible |
| Baby Check | Every 1s | <50ms | Negligible |
| Combat Tick | Every tick | <200ms | Medium |

---

## Future Enhancements

### 1. Advanced Genetics
- Track specific traits (speed, health, damage)
- Breed for optimal stats
- Create "perfect" axolotl bloodlines
- Export breeding data to NBT

### 2. Automated Farms
- Build drowned farms for combat XP
- Create guardian farms for prizes
- Automated tropical fish farming
- Resource collection systems

### 3. Multi-Agent Coordination
- Multiple foremen coordinate breeding
- Share blue axolotls across agents
- Distributed breeding for faster blues
- Centralized lineage database

### 4. Trading Integration
- Sell blue axolotls to players
- Trade axolotls for rare items
- Axolotl marketplace system
- Reputation for quality breeds

### 5. Custom Habitats
- Underwater dome construction
- Naturalistic cave environments
- Automated feeding systems
- Decorative breeding displays

---

## Conclusion

The Axolotl Breeding AI system provides comprehensive coverage of:
- Lush cave detection and navigation
- Automated breeding targeting rare blue variants
- Color lineage tracking and genetics
- Combat squad deployment for underwater threats
- Habitat construction and management

**Key Benefits:**
- **Blue Axolotl Acquisition**: Average 1/1200 chance becomes automated process
- **Combat Advantage**: Axolotl squads excel against drowned and guardians
- **Resource Efficiency**: Smart breeding minimizes tropical fish waste
- **Educational**: Teaches players about axolotl mechanics
- **Collectible**: Blue axolotls are highly valuable trading items

**Expected Timeline:**
- Casual players: ~40-60 hours for first blue (manual)
- With AI assistance: ~20-30 hours for first blue (automated)
- After first blue: Unlimited 50% blue breeding

---

**Sources:**
- [Tutorials/Axolotl farming - Minecraft Wiki](https://minecraft.fandom.com/wiki/Tutorials/Axolotl_farming)
- [Mob Menagerie: Axolotl - Minecraft.net](https://www.minecraft.net/en-us/article/axolotl)
- [Axolotl - Minecraft Wiki](https://minecraft.fandom.com/wiki/Axolotl)

**Document End**

This design document provides a complete foundation for implementing axolotl breeding AI in MineWright, integrating seamlessly with the existing animal handling architecture while adding specialized features for these unique aquatic mobs.
