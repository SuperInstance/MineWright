# Scaffolding Build AI for MineWright Minecraft Mod

**Research Document:** MineWright Scaffolding Build System
**Date:** 2026-02-27
**Status:** Design Complete - Implementation Roadmap Provided
**Target:** Forge 1.20.1, Java 17

---

## Executive Summary

This document outlines the design and implementation strategy for an intelligent scaffolding build AI system for the MineWright Minecraft mod. The system enables Foreman entities to safely build tall structures by automatically placing and removing scaffolding, implementing water bucket fall breaks, and optimizing material usage.

**Key Design Principles:**
- **Safety First**: Agents never take fall damage during building operations
- **Automatic Cleanup**: All scaffolding is removed after build completion
- **Height Awareness**: Dynamic scaffold placement based on build height requirements
- **Material Efficiency**: Reusable scaffolding patterns minimize material waste
- **Integration Ready**: Builds on existing `BuildStructureAction` and `CollaborativeBuildManager`

**Recommendation Priority:**
1. **Critical**: Implement `ScaffoldingManager` core system
2. **High**: Add height-aware scaffold placement algorithm
3. **High**: Implement automatic scaffold cleanup
4. **Medium**: Add water bucket MLG fall break integration
5. **Medium**: Optimize material usage with reusable scaffolds
6. **Low**: Add scaffold inventory management

---

## Table of Contents

1. [System Architecture](#1-system-architecture)
2. [Scaffolding Algorithms](#2-scaffolding-algorithms)
3. [Safety Protocols](#3-safety-protocols)
4. [Cleanup Strategies](#4-cleanup-strategies)
5. [Water Bucket Integration](#5-water-bucket-integration)
6. [Material Optimization](#6-material-optimization)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Code Examples](#8-code-examples)
9. [Testing Strategy](#9-testing-strategy)

---

## 1. System Architecture

### 1.1 Core Components

```
ScaffoldingBuildSystem
├── ScaffoldingManager (core coordinator)
│   ├── ScaffoldPlacer (placement logic)
│   ├── ScaffoldRemover (cleanup logic)
│   ├── HeightCalculator (dynamic height analysis)
│   └── SafetyMonitor (fall protection)
├── ScaffoldPatternLibrary (predefined patterns)
├── WaterBucketFallBreak (MLG mechanic)
└── ScaffoldInventoryManager (material tracking)
```

### 1.2 Integration Points

**Existing Systems to Leverage:**
- `BuildStructureAction` (C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildStructureAction.java)
  - Lines 40-182: Current build initialization and execution
  - Lines 274-277: Build plan generation via `StructureGenerators`

- `CollaborativeBuildManager` (C:\Users\casey\steve\src\main\java\com\minewright\action\CollaborativeBuildManager.java)
  - Spatial partitioning for multi-agent coordination
  - Block claiming system for concurrent building

- `ForemanEntity` (C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java)
  - Lines 211-267: Flying mode and fall damage handling
  - Can be extended for scaffolding-specific behavior

- `BaseAction` (C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java)
  - Lines 19-34: Action lifecycle management
  - Template for new scaffolding actions

### 1.3 New Package Structure

```
com.minewright.scaffolding
├── ScaffoldingManager.java
├── ScaffoldPattern.java
├── ScaffoldPlacer.java
├── ScaffoldRemover.java
├── HeightCalculator.java
├── SafetyMonitor.java
├── WaterBucketFallBreak.java
└── ScaffoldInventoryManager.java

com.minewright.action.actions
├── BuildWithScaffoldingAction.java
└── RemoveScaffoldingAction.java
```

---

## 2. Scaffolding Algorithms

### 2.1 Height-Aware Scaffold Placement

**Algorithm Overview:**

The scaffolding system dynamically calculates scaffold requirements based on:
- Maximum build height
- Agent reach distance (5 blocks standard)
- Safety buffer zones
- Structure footprint

**Height Calculation Formula:**

```java
public class HeightCalculator {
    private static final int AGENT_REACH = 5;        // Blocks agent can reach
    private static final int SAFETY_BUFFER = 3;      // Extra blocks for safety
    private static final int SCAFFOLD_INTERVAL = 4;  // Place scaffold every N blocks

    /**
     * Calculate scaffold tower height requirements
     *
     * @param baseY Starting ground Y level
     * @param maxHeight Highest block Y level in structure
     * @return List of Y levels requiring scaffolding
     */
    public static List<Integer> calculateScaffoldLevels(int baseY, int maxHeight) {
        List<Integer> levels = new ArrayList<>();

        // Start scaffolding just above agent's maximum standing reach
        int currentY = baseY + AGENT_REACH + 1;

        while (currentY < maxHeight + SAFETY_BUFFER) {
            levels.add(currentY);
            currentY += SCAFFOLD_INTERVAL;
        }

        // Always add a top platform for final blocks
        if (!levels.contains(maxHeight)) {
            levels.add(maxHeight);
        }

        return levels;
    }

    /**
     * Calculate scaffold tower position (center of structure)
     *
     * @param start Structure start position
     * @param width Structure width
     * @param depth Structure depth
     * @return Center position for scaffold tower
     */
    public static BlockPos calculateTowerCenter(BlockPos start, int width, int depth) {
        int centerX = start.getX() + width / 2;
        int centerZ = start.getZ() + depth / 2;
        int baseY = start.getY();

        return new BlockPos(centerX, baseY, centerZ);
    }
}
```

### 2.2 Scaffold Pattern Types

**Pattern 1: Single Tower (Small Structures)**

```
Best for: Width/Depth < 7 blocks
Material cost: Low
Construction speed: Fast

   [Top Platform]
        ||
        ||
   [Scaffold Block]
        ||
        ||
   [Scaffold Block]
        ||
        ||
   [Base Platform]
```

**Pattern 2: Double Tower (Medium Structures)**

```
Best for: Width/Depth 7-15 blocks
Material cost: Medium
Construction speed: Medium

[Top Platform]-----[Top Platform]
      ||                 ||
      ||                 ||
[Scaffold Block]-----[Scaffold Block]
      ||                 ||
      ||                 ||
[Scaffold Block]-----[Scaffold Block]
      ||                 ||
      ||                 ||
[Base Platform]-----[Base Platform]
```

**Pattern 3: Corner Towers (Large Structures)**

```
Best for: Width/Depth > 15 blocks
Material cost: High
Construction speed: Slow (parallelizable)

[TP]                 [TP]
 ||                   ||
 ||                   ||
[SB]                 [SB]
 ||                   ||
 ||                   ||
[BP]                 [BP]

(Same pattern at all 4 corners)
```

### 2.3 Scaffold Placement Algorithm

```java
public class ScaffoldPlacer {
    private final ServerLevel level;
    private final ForemanEntity foreman;
    private final Block scaffoldBlock;

    public ScaffoldPlacer(ServerLevel level, ForemanEntity foreman) {
        this.level = level;
        this.foreman = foreman;
        // Use dirt as default scaffolding (always available, cheap)
        this.scaffoldBlock = Blocks.DIRT;
    }

    /**
     * Place scaffolding for a structure
     *
     * @param structureBounds Structure bounding box
     * @param pattern Scaffold pattern to use
     * @return Set of placed scaffold positions
     */
    public Set<BlockPos> placeScaffolding(BoundingBox structureBounds,
                                         ScaffoldPattern pattern) {
        Set<BlockPos> placedScaffolds = new HashSet<>();

        // Calculate base Y level (ground under structure)
        int baseY = findGroundLevel(structureBounds);

        // Calculate required scaffold levels
        List<Integer> levels = HeightCalculator.calculateScaffoldLevels(
            baseY,
            structureBounds.maxY()
        );

        // Place scaffolding according to pattern
        switch (pattern.getType()) {
            case SINGLE_TOWER:
                placedScaffolds.addAll(placeSingleTower(structureBounds, levels));
                break;
            case DOUBLE_TOWER:
                placedScaffolds.addAll(placeDoubleTower(structureBounds, levels));
                break;
            case CORNER_TOWERS:
                placedScaffolds.addAll(placeCornerTowers(structureBounds, levels));
                break;
        }

        return placedScaffolds;
    }

    private Set<BlockPos> placeSingleTower(BoundingBox bounds,
                                           List<Integer> levels) {
        Set<BlockPos> scaffolds = new HashSet<>();
        BlockPos center = HeightCalculator.calculateTowerCenter(
            new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
            bounds.getXSpan(),
            bounds.getZSpan()
        );

        // Place base platform (3x3)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, 0, z);
                placeScaffoldBlock(pos);
                scaffolds.add(pos);
            }
        }

        // Place vertical tower
        for (int level : levels) {
            BlockPos pos = new BlockPos(center.getX(), level, center.getZ());
            placeScaffoldBlock(pos);
            scaffolds.add(pos);

            // Add small platform at each level (2x2)
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    BlockPos platformPos = pos.offset(x, 0, z);
                    placeScaffoldBlock(platformPos);
                    scaffolds.add(platformPos);
                }
            }
        }

        return scaffolds;
    }

    private void placeScaffoldBlock(BlockPos pos) {
        BlockState current = level.getBlockState(pos);

        // Only place if air or replaceable
        if (current.isAir() || current.getMaterial().isReplaceable()) {
            level.setBlock(pos, scaffoldBlock.defaultBlockState(), 3);

            // Play sound effect
            level.playSound(null, pos,
                scaffoldBlock.defaultBlockState().getSoundType(level, pos, foreman).getPlaceSound(),
                SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }

    private int findGroundLevel(BoundingBox bounds) {
        // Scan down from min Y to find solid ground
        for (int y = bounds.minY(); y >= bounds.minY() - 20; y--) {
            BlockPos testPos = new BlockPos(
                (bounds.minX() + bounds.maxX()) / 2,
                y,
                (bounds.minZ() + bounds.maxZ()) / 2
            );

            BlockState state = level.getBlockState(testPos);
            if (state.isSolid() && !state.isAir()) {
                return y + 1; // Return first air block above ground
            }
        }

        return bounds.minY(); // Fallback to structure min Y
    }
}
```

---

## 3. Safety Protocols

### 3.1 Fall Damage Prevention

**Current State Analysis:**

From `ForemanEntity.java` (lines 261-267):
```java
@Override
public boolean causeFallDamage(float distance, float damageMultiplier,
                              DamageSource source) {
    // No fall damage when flying
    if (this.isFlying) {
        return false;
    }
    return super.causeFallDamage(distance, damageMultiplier, source);
}
```

**Enhanced Safety Protocol:**

```java
public class SafetyMonitor {
    private final ForemanEntity foreman;
    private final ServerLevel level;
    private BlockPos lastSafePosition;
    private int ticksSinceSafePosition;

    // Safety thresholds
    private static final int MAX_FALL_DISTANCE = 3;  // Max safe fall without scaffolding
    private static final int WARNING_HEIGHT = 5;     // Height to trigger safety measures
    private static final double POSITION_CHECK_INTERVAL = 20; // Check every second

    /**
     * Monitor agent safety during building
     * Call this every tick during building operations
     */
    public void tick() {
        BlockPos currentPos = foreman.blockPosition();
        int currentHeight = currentPos.getY();
        int groundHeight = findGroundHeight(currentPos);
        int heightAboveGround = currentHeight - groundHeight;

        // Update last safe position if on ground or scaffold
        if (isSafePosition(currentPos)) {
            lastSafePosition = currentPos;
            ticksSinceSafePosition = 0;
        } else {
            ticksSinceSafePosition++;
        }

        // Trigger safety measures if too high without scaffolding
        if (heightAboveGround > WARNING_HEIGHT && !hasScaffoldingBelow(currentPos)) {
            triggerSafetyProtocol(currentPos);
        }

        // Emergency teleport if fallen too far without scaffolding
        if (heightAboveGround > MAX_FALL_DISTANCE && ticksSinceSafePosition > 10) {
            emergencyRecovery();
        }
    }

    /**
     * Check if position is safe (ground or scaffold)
     */
    private boolean isSafePosition(BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState at = level.getBlockState(pos);

        // Safe if standing on solid block or scaffolding
        if (below.isSolid() || isScaffoldBlock(below)) {
            return true;
        }

        // Safe if in water (emergency fallback)
        if (at.is(Blocks.WATER)) {
            return true;
        }

        return false;
    }

    /**
     * Check if scaffolding exists below position
     */
    private boolean hasScaffoldingBelow(BlockPos pos) {
        // Check up to 5 blocks below for scaffolding
        for (int i = 1; i <= 5; i++) {
            BlockPos checkPos = pos.below(i);
            if (isScaffoldBlock(level.getBlockState(checkPos))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trigger safety protocol when agent is at dangerous height
     */
    private void triggerSafetyProtocol(BlockPos currentPos) {
        // Pause current action
        foreman.getActionExecutor().pauseCurrentAction();

        // Place emergency scaffolding below
        BlockPos scaffoldPos = currentPos.below(2);
        placeEmergencyScaffold(scaffoldPos);

        // Resume action
        foreman.getActionExecutor().resumeCurrentAction();
    }

    /**
     * Emergency recovery when agent has fallen
     */
    private void emergencyRecovery() {
        if (lastSafePosition != null) {
            // Teleport back to last safe position
            foreman.teleportTo(
                lastSafePosition.getX() + 0.5,
                lastSafePosition.getY(),
                lastSafePosition.getZ() + 0.5
            );

            foreman.sendChatMessage("Emergency recovery activated!");
        }
    }

    private boolean isScaffoldBlock(BlockState state) {
        // Check if block is a scaffold (dirt, cobblestone, or actual scaffolding)
        Block block = state.getBlock();
        return block == Blocks.DIRT ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.SCAFFOLDING;
    }

    private void placeEmergencyScaffold(BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (current.isAir()) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.GRASS_PLACE,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private int findGroundHeight(BlockPos pos) {
        for (int y = pos.getY(); y >= pos.getY() - 32; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(checkPos).isSolid()) {
                return y;
            }
        }
        return pos.getY() - 32;
    }
}
```

### 3.2 Height Safety Zones

**Zone Classification:**

```java
public enum SafetyZone {
    GROUND_LEVEL(0, 2, "Safe - No protection needed"),
    LOW_HEIGHT(2, 5, "Caution - Monitor position"),
    MEDIUM_HEIGHT(5, 12, "Danger - Scaffolding required"),
    HIGH_HEIGHT(12, Integer.MAX_VALUE, "Critical - Full safety protocol");

    private final int minHeight;
    private final int maxHeight;
    private final String description;

    SafetyZone(int minHeight, int maxHeight, String description) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.description = description;
    }

    public static SafetyZone fromHeight(int heightAboveGround) {
        for (SafetyZone zone : values()) {
            if (heightAboveGround >= zone.minHeight && heightAboveGround < zone.maxHeight) {
                return zone;
            }
        }
        return GROUND_LEVEL;
    }
}
```

### 3.3 Scaffold Safety Checks

```java
public class ScaffoldSafetyChecker {
    /**
     * Verify scaffold integrity before agent climbs
     *
     * @param scaffolds Set of scaffold positions
     * @return true if scaffolding is safe to use
     */
    public static boolean verifyScaffoldIntegrity(ServerLevel level,
                                                  Set<BlockPos> scaffolds) {
        for (BlockPos pos : scaffolds) {
            BlockState state = level.getBlockState(pos);

            // Check if scaffold still exists
            if (state.isAir()) {
                return false; // Missing scaffold block!
            }

            // Check if scaffold is supported (has something below)
            if (!hasSupport(level, pos)) {
                return false; // Floating scaffold!
            }
        }

        return true;
    }

    private static boolean hasSupport(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isSolid() || belowState.getBlock() == Blocks.SCAFFOLDING;
    }

    /**
     * Check if scaffold path is clear for climbing
     */
    public static boolean isPathClear(ServerLevel level, List<BlockPos> path) {
        for (BlockPos pos : path) {
            // Check for obstructions at head level
            BlockPos headPos = pos.above(1);
            BlockState headState = level.getBlockState(headPos);

            if (!headState.isAir() && !headState.is(Blocks.SCAFFOLDING)) {
                return false; // Path blocked!
            }
        }
        return true;
    }
}
```

---

## 4. Cleanup Strategies

### 4.1 Automatic Scaffold Removal

**Design Philosophy:**
- Remove scaffolding from top to bottom (safe order)
- Verify structure completion before cleanup
- Handle partial failures gracefully
- Support manual cancellation

**Removal Algorithm:**

```java
public class ScaffoldRemover {
    private final ServerLevel level;
    private final ForemanEntity foreman;
    private final Set<BlockPos> scaffolds;

    public ScaffoldRemover(ServerLevel level, ForemanEntity, Set<BlockPos> scaffolds) {
        this.level = level;
        this.foreman = foreman;
        this.scaffolds = new TreeSet<>((a, b) ->
            Integer.compare(b.getY(), a.getY())); // Sort by Y descending
        this.scaffolds.addAll(scaffolds);
    }

    /**
     * Remove all scaffolding safely
     *
     * @return ActionResult with cleanup status
     */
    public ActionResult removeAll() {
        int removed = 0;
        int failed = 0;

        // Remove from top to bottom
        for (BlockPos pos : scaffolds) {
            if (removeScaffold(pos)) {
                removed++;
            } else {
                failed++;
            }
        }

        String message = String.format("Scaffold cleanup complete: %d removed, %d failed",
                                     removed, failed);

        if (failed == 0) {
            return ActionResult.success(message);
        } else {
            return ActionResult.partial(message);
        }
    }

    /**
     * Remove a single scaffold block
     */
    private boolean removeScaffold(BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);

        // Verify it's still a scaffold
        if (!isScaffoldBlock(currentState)) {
            return false; // Already removed or replaced
        }

        // Check if anything important is above
        if (hasImportantBlockAbove(pos)) {
            return false; // Don't remove if supporting structure
        }

        // Remove the block
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        // Play sound
        level.playSound(null, pos, SoundEvents.GRAVEL_BREAK,
            SoundSource.BLOCKS, 0.5f, 1.0f);

        return true;
    }

    private boolean hasImportantBlockAbove(BlockPos pos) {
        // Check up to 3 blocks above
        for (int i = 1; i <= 3; i++) {
            BlockPos above = pos.above(i);
            BlockState state = level.getBlockState(above);

            // If there's a non-scaffold, non-air block, don't remove
            if (!state.isAir() && !isScaffoldBlock(state)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScaffoldBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.SCAFFOLDING;
    }
}
```

### 4.2 Post-Build Verification

```java
public class BuildVerifier {
    /**
     * Verify structure is complete before removing scaffolding
     *
     * @param buildPlan Original build plan
     * @param structureBounds Structure bounding box
     * @return true if structure is complete
     */
    public static boolean verifyBuildComplete(ServerLevel level,
                                             List<BlockPlacement> buildPlan,
                                             BoundingBox structureBounds) {
        // Check all blocks in build plan are placed
        for (BlockPlacement placement : buildPlan) {
            BlockState current = level.getBlockState(placement.pos);
            Block expected = placement.block;

            if (current.getBlock() != expected) {
                // Block not placed correctly!
                return false;
            }
        }

        return true;
    }

    /**
     * Find missing blocks in structure
     *
     * @return List of missing block positions
     */
    public static List<BlockPos> findMissingBlocks(ServerLevel level,
                                                   List<BlockPlacement> buildPlan) {
        List<BlockPos> missing = new ArrayList<>();

        for (BlockPlacement placement : buildPlan) {
            BlockState current = level.getBlockState(placement.pos);
            if (current.isAir() || current.getBlock() != placement.block) {
                missing.add(placement.pos);
            }
        }

        return missing;
    }
}
```

### 4.3 Partial Cleanup Strategy

```java
public class PartialCleanupStrategy {
    /**
     * Remove only scaffolding that's safe to remove
     * (not supporting incomplete structure parts)
     */
    public static Set<BlockPos> findRemovableScaffolds(ServerLevel level,
                                                       Set<BlockPos> allScaffolds,
                                                       List<BlockPlacement> buildPlan) {
        Set<BlockPos> removable = new HashSet<>();

        for (BlockPos scaffoldPos : allScaffolds) {
            if (isSafeToRemove(level, scaffoldPos, buildPlan)) {
                removable.add(scaffoldPos);
            }
        }

        return removable;
    }

    private static boolean isSafeToRemove(ServerLevel level,
                                         BlockPos scaffoldPos,
                                         List<BlockPlacement> buildPlan) {
        // Check if any build block is directly above this scaffold
        for (BlockPlacement placement : buildPlan) {
            if (isDirectlyAbove(placement.pos, scaffoldPos)) {
                // Check if build block is actually placed
                BlockState state = level.getBlockState(placement.pos);
                if (state.isAir()) {
                    return false; // Build block not placed, keep scaffold
                }
            }
        }

        // Check if any scaffold above depends on this one
        BlockPos above = scaffoldPos.above();
        while (true) {
            BlockState state = level.getBlockState(above);
            if (isScaffoldBlock(state)) {
                // Found dependent scaffold above
                return false;
            }
            if (!state.isAir()) {
                break; // Hit non-scaffold block
            }
            above = above.above();
        }

        return true;
    }

    private static boolean isDirectlyAbove(BlockPos blockPos, BlockPos scaffoldPos) {
        return blockPos.getX() == scaffoldPos.getX() &&
               blockPos.getZ() == scaffoldPos.getZ() &&
               blockPos.getY() > scaffoldPos.getY();
    }

    private static boolean isScaffoldBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.SCAFFOLDING;
    }
}
```

---

## 5. Water Bucket Integration

### 5.1 MLG Fall Break Mechanic

**MLG (Major League Gaming) Water Bucket Technique:**
- Place water bucket just before landing to cancel fall damage
- Requires precise timing (within 2-3 ticks of impact)
- Works for falls up to ~23 blocks in vanilla

**Implementation:**

```java
public class WaterBucketFallBreak {
    private final ForemanEntity foreman;
    private final ServerLevel level;
    private boolean isFalling = false;
    private int fallTicks = 0;
    private BlockPos fallStartPos;
    private static final int MLG_TRIGGER_DISTANCE = 20; // Blocks
    private static final int WATER_PLACE_TICKS_BEFORE_LANDING = 3;

    public WaterBucketFallBreak(ForemanEntity foreman) {
        this.foreman = foreman;
        this.level = (ServerLevel) foreman.level();
    }

    /**
     * Monitor fall state and attempt MLG water bucket placement
     * Call this every tick
     */
    public void tick() {
        BlockPos currentPos = foreman.blockPosition();
        double verticalMotion = foreman.getDeltaMovement().y;

        // Detect start of fall
        if (!isFalling && verticalMotion < -0.5) {
            isFalling = true;
            fallTicks = 0;
            fallStartPos = currentPos;
            MineWrightMod.LOGGER.debug("{} started falling at {}",
                foreman.getSteveName(), currentPos);
        }

        // Track fall progress
        if (isFalling) {
            fallTicks++;
            int fallDistance = fallStartPos.getY() - currentPos.getY();

            // Predict landing position
            BlockPos predictedLanding = predictLandingPosition();

            // Check if we should place water (MLG timing)
            if (shouldPlaceWater(fallDistance, predictedLanding)) {
                attemptMLGWaterBucket(predictedLanding);
            }

            // Check if landed
            if (verticalMotion >= -0.1 || isOnGround()) {
                isFalling = false;
                fallTicks = 0;
            }
        }
    }

    /**
     * Predict where agent will land
     */
    private BlockPos predictLandingPosition() {
        BlockPos currentPos = foreman.blockPosition();
        Vec3 velocity = foreman.getDeltaMovement();

        // Simple prediction: scan down until we hit solid block
        for (int y = currentPos.getY() - 1; y >= currentPos.getY() - 32; y--) {
            BlockPos checkPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());
            BlockState state = level.getBlockState(checkPos);

            if (state.isSolid() || isWater(state)) {
                return checkPos.above(); // Return position above ground/water
            }
        }

        return currentPos;
    }

    /**
     * Determine if water should be placed for MLG
     */
    private boolean shouldPlaceWater(int fallDistance, BlockPos landingPos) {
        // Only attempt for dangerous falls
        if (fallDistance < MLG_TRIGGER_DISTANCE) {
            return false;
        }

        // Check if landing position is safe (not in water already)
        BlockState landingState = level.getBlockState(landingPos);
        if (isWater(landingState)) {
            return false; // Already landing in water
        }

        // Check if we're at the right height for placement
        int currentY = foreman.blockPosition().getY();
        int landingY = landingPos.getY();
        int blocksAboveLanding = currentY - landingY;

        return blocksAboveLanding == WATER_PLACE_TICKS_BEFORE_LANDING;
    }

    /**
     * Attempt to place water bucket for MLG
     */
    private void attemptMLGWaterBucket(BlockPos landingPos) {
        // Check if agent has water bucket
        if (!hasWaterBucket()) {
            MineWrightMod.LOGGER.warn("{} no water bucket for MLG!",
                foreman.getSteveName());
            return;
        }

        // Check if position is valid for water placement
        BlockState targetState = level.getBlockState(landingPos);
        if (!targetState.isAir() && !canWaterlog(targetState)) {
            return; // Can't place water here
        }

        // Place water
        level.setBlock(landingPos, Blocks.WATER.defaultBlockState(), 3);

        // Play sound
        level.playSound(null, landingPos, SoundEvents.BUCKET_EMPTY,
            SoundSource.BLOCKS, 1.0f, 1.0f);

        MineWrightMod.LOGGER.info("{} MLG water bucket at {}",
            foreman.getSteveName(), landingPos);

        // Schedule water cleanup (remove after 5 seconds)
        scheduleWaterCleanup(landingPos, 100); // 100 ticks = 5 seconds
    }

    private boolean hasWaterBucket() {
        // Check if agent has water bucket in inventory
        // For now, assume always available (creative mode or infinite bucket)
        // TODO: Implement proper inventory checking
        return true;
    }

    private boolean isWater(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.SEAGRASS) ||
               state.is(Blocks.TALL_SEAGRASS);
    }

    private boolean canWaterlog(BlockState state) {
        // Check if block can be waterlogged
        return state.hasProperty(BlockStateProperties.WATERLOGGED);
    }

    private boolean isOnGround() {
        return foreman.isOnGround();
    }

    private void scheduleWaterCleanup(BlockPos pos, int delayTicks) {
        // Schedule a task to remove water after delay
        MineWrightMod.getServer().ifPresent(server -> {
            server.tell(new TickDelayedTask(delayTicks, () -> {
                BlockState currentState = level.getBlockState(pos);
                if (isWater(currentState)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    MineWrightMod.LOGGER.debug("Cleaned up MLG water at {}", pos);
                }
            }));
        });
    }
}
```

### 5.2 Enhanced Fall Damage Event Handling

**Integration with Forge Event System:**

```java
@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScaffoldingEventHandler {
    /**
     * Handle fall damage event for agents
     * Provides last-resort fall damage cancellation
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof ForemanEntity foreman)) {
            return;
        }

        // Check if scaffolding was present during fall
        if (wasUsingScaffolding(foreman)) {
            event.setCanceled(true);
            event.setDistance(0);
            MineWrightMod.LOGGER.info("Fall damage cancelled for {} (using scaffolding)",
                foreman.getSteveName());
            return;
        }

        // Check if MLG water bucket was used
        if (wasMLGSuccessful(foreman)) {
            event.setCanceled(true);
            event.setDistance(0);
            MineWrightMod.LOGGER.info("Fall damage cancelled for {} (MLG water bucket)",
                foreman.getSteveName());
        }
    }

    private static boolean wasUsingScaffolding(ForemanEntity foreman) {
        // Check if agent recently interacted with scaffolding
        // This would be set by ScaffoldPlacer when scaffolding is active
        return foreman.getPersistentData().getBoolean("UsingScaffolding");
    }

    private static boolean wasMLGSuccessful(ForemanEntity foreman) {
        // Check if MLG water bucket was placed recently
        return foreman.getPersistentData().getLong("LastMLGTime") >
               System.currentTimeMillis() - 1000; // Within last second
    }
}
```

---

## 6. Material Optimization

### 6.1 Reusable Scaffolding System

```java
public class ScaffoldInventoryManager {
    private final ForemanEntity foreman;
    private final Map<Block, Integer> scaffoldInventory;

    public ScaffoldInventoryManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.scaffoldInventory = new HashMap<>();
    }

    /**
     * Calculate required scaffolding materials for a structure
     *
     * @param bounds Structure bounding box
     * @param pattern Scaffold pattern
     * @return Material requirements
     */
    public MaterialRequirements calculateRequirements(BoundingBox bounds,
                                                     ScaffoldPattern pattern) {
        int baseY = findGroundLevel(bounds);
        List<Integer> levels = HeightCalculator.calculateScaffoldLevels(
            baseY, bounds.maxY()
        );

        int totalBlocks = 0;

        switch (pattern.getType()) {
            case SINGLE_TOWER:
                // Base platform (3x3 = 9 blocks)
                totalBlocks += 9;
                // Each level has 2x2 platform + 1 tower block = 5 blocks
                totalBlocks += levels.size() * 5;
                break;

            case DOUBLE_TOWER:
                // Two base platforms (9 * 2 = 18 blocks)
                totalBlocks += 18;
                // Each level has two sets of 5 blocks = 10 blocks
                totalBlocks += levels.size() * 10;
                break;

            case CORNER_TOWERS:
                // Four base platforms (9 * 4 = 36 blocks)
                totalBlocks += 36;
                // Each level has four sets of 5 blocks = 20 blocks
                totalBlocks += levels.size() * 20;
                break;
        }

        return new MaterialRequirements(totalBlocks, estimateCost(totalBlocks));
    }

    /**
     * Collect scaffolding after build completion
     *
     * @param scaffoldPositions Set of scaffold positions
     * @return Number of blocks collected
     */
    public int collectScaffolding(Set<BlockPos> scaffoldPositions) {
        int collected = 0;

        for (BlockPos pos : scaffoldPositions) {
            BlockState state = foreman.level().getBlockState(pos);

            if (isScaffoldBlock(state)) {
                // Break the block
                foreman.level().destroyBlock(pos, true); // true = drop items
                collected++;
            }
        }

        // Update inventory
        Block scaffoldType = Blocks.DIRT; // Default
        scaffoldInventory.put(scaffoldType,
            scaffoldInventory.getOrDefault(scaffoldType, 0) + collected);

        return collected;
    }

    /**
     * Estimate material cost for scaffolding
     */
    private int estimateCost(int blockCount) {
        // Dirt is cheap (0 cost, infinite)
        // Cobblestone is medium (1 per block)
        // Actual scaffolding is expensive (4 per block)
        return blockCount * 1; // Using dirt as default
    }

    private boolean isScaffoldBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.SCAFFOLDING;
    }

    private int findGroundLevel(BoundingBox bounds) {
        // Implementation from ScaffoldPlacer
        return bounds.minY();
    }

    public static class MaterialRequirements {
        public final int blockCount;
        public final int estimatedCost;

        public MaterialRequirements(int blockCount, int estimatedCost) {
            this.blockCount = blockCount;
            this.estimatedCost = estimatedCost;
        }
    }
}
```

### 6.2 Material Selection Strategy

```java
public class ScaffoldMaterialSelector {
    public enum ScaffoldMaterial {
        DIRT(Blocks.DIRT, 0, "Infinite, cheap, always available"),
        COBBLESTONE(Blocks.COBBLESTONE, 1, "Cheap, decent durability"),
        WOOD(Blocks.OAK_PLANKS, 2, "Medium cost, aesthetic"),
        STONE(Blocks.STONE, 3, "Durable, realistic"),
        SCAFFOLDING(Blocks.SCAFFOLDING, 4, "Expensive, best functionality");

        private final Block block;
        private final int costPerBlock;
        private final String description;

        ScaffoldMaterial(Block block, int costPerBlock, String description) {
            this.block = block;
            this.costPerBlock = costPerBlock;
            this.description = description;
        }

        public Block getBlock() { return block; }
        public int getCostPerBlock() { return costPerBlock; }
        public String getDescription() { return description; }
    }

    /**
     * Select optimal scaffolding material based on structure
     */
    public static ScaffoldMaterial selectMaterial(BoundingBox bounds,
                                                 int budget) {
        int height = bounds.getYSpan();

        // Use dirt for small structures (height < 10)
        if (height < 10) {
            return ScaffoldMaterial.DIRT;
        }

        // Use cobblestone for medium structures (height 10-20)
        if (height < 20) {
            return ScaffoldMaterial.COBBLESTONE;
        }

        // Use better materials for tall structures
        if (budget >= 100) {
            return ScaffoldMaterial.SCAFFOLDING;
        } else if (budget >= 50) {
            return ScaffoldMaterial.STONE;
        } else {
            return ScaffoldMaterial.WOOD;
        }
    }
}
```

---

## 7. Implementation Roadmap

### Phase 1: Core Scaffolding System (Week 1-2)

**Tasks:**
1. Create `ScaffoldingManager` class
2. Implement `HeightCalculator` with basic height analysis
3. Create `ScaffoldPattern` enum and pattern selection logic
4. Implement single tower scaffold placement
5. Add basic safety monitoring

**Deliverables:**
- `ScaffoldingManager.java`
- `HeightCalculator.java`
- `ScaffoldPattern.java`
- `ScaffoldPlacer.java`
- `SafetyMonitor.java` (basic version)

**Testing:**
- Unit tests for height calculation
- Integration test for single tower placement
- Safety test for fall prevention

### Phase 2: Advanced Patterns & Cleanup (Week 3-4)

**Tasks:**
1. Implement double tower pattern
2. Implement corner towers pattern
3. Create `ScaffoldRemover` with top-to-bottom cleanup
4. Add build verification before cleanup
5. Implement partial cleanup for failed builds

**Deliverables:**
- Enhanced `ScaffoldPlacer` with all patterns
- `ScaffoldRemover.java`
- `BuildVerifier.java`
- `PartialCleanupStrategy.java`

**Testing:**
- Pattern placement tests for all types
- Cleanup test with complete structure
- Partial cleanup test with incomplete structure

### Phase 3: Water Bucket MLG (Week 5)

**Tasks:**
1. Implement `WaterBucketFallBreak` system
2. Add fall detection and landing prediction
3. Create MLG timing algorithm
4. Add water cleanup system
5. Integrate with `LivingFallEvent`

**Deliverables:**
- `WaterBucketFallBreak.java`
- `ScaffoldingEventHandler.java`
- Integration with existing `ForemanEntity`

**Testing:**
- MLG fall break tests at various heights
- Water cleanup verification
- Event handler integration tests

### Phase 4: Material Optimization & Inventory (Week 6)

**Tasks:**
1. Implement `ScaffoldInventoryManager`
2. Create material selection strategy
3. Add scaffolding collection system
4. Implement material tracking
5. Add cost estimation

**Deliverables:**
- `ScaffoldInventoryManager.java`
- `ScaffoldMaterialSelector.java`
- Integration with inventory system

**Testing:**
- Material calculation accuracy
- Collection system tests
- Cost estimation verification

### Phase 5: Action Integration (Week 7)

**Tasks:**
1. Create `BuildWithScaffoldingAction`
2. Create `RemoveScaffoldingAction`
3. Register actions in `CoreActionsPlugin`
4. Update `PromptBuilder` with scaffolding commands
5. Add LLM task planning integration

**Deliverables:**
- `BuildWithScaffoldingAction.java`
- `RemoveScaffoldingAction.java`
- Updated `CoreActionsPlugin.java`
- Updated `PromptBuilder.java`

**Testing:**
- End-to-end build with scaffolding
- LLM command parsing tests
- Multi-agent coordination tests

### Phase 6: Polish & Optimization (Week 8)

**Tasks:**
1. Performance profiling and optimization
2. Add visual feedback (particles, sounds)
3. Implement configuration options
4. Add progress reporting
5. Documentation and examples

**Deliverables:**
- Configuration file updates
- Performance improvements
- User documentation
- Code examples

**Testing:**
- Performance benchmarks
- User acceptance testing
- Multi-player compatibility tests

---

## 8. Code Examples

### 8.1 Complete Build with Scaffolding Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.scaffolding.*;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.StructureGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/**
 * Build a structure with automatic scaffolding placement and removal
 */
public class BuildWithScaffoldingAction extends BaseAction {
    private String structureType;
    private int width;
    private int height;
    private int depth;

    private BuildPhase currentPhase = BuildPhase.INITIALIZATION;
    private ScaffoldingManager scaffoldingManager;
    private Set<BlockPos> placedScaffolds;
    private List<BlockPlacement> buildPlan;
    private int blocksPlaced = 0;

    private static final int BLOCKS_PER_TICK = 2;

    public BuildWithScaffoldingAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Parse structure parameters
        structureType = task.getStringParameter("structure", "house");
        width = task.getIntParameter("width", 9);
        height = task.getIntParameter("height", 6);
        depth = task.getIntParameter("depth", 9);

        // Find build position
        BlockPos buildPos = findBuildPosition();
        if (buildPos == null) {
            result = ActionResult.failure("Cannot find suitable build position");
            return;
        }

        // Initialize scaffolding manager
        scaffoldingManager = new ScaffoldingManager(foreman);

        // Generate build plan
        buildPlan = StructureGenerators.generate(
            structureType, buildPos, width, height, depth,
            List.of(Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.GLASS_PANE)
        );

        if (buildPlan.isEmpty()) {
            result = ActionResult.failure("Cannot generate build plan");
            return;
        }

        foreman.sendChatMessage("Building " + structureType +
            " (" + width + "x" + height + "x" + depth + ") with scaffolding!");
    }

    @Override
    protected void onTick() {
        switch (currentPhase) {
            case INITIALIZATION:
                phaseInitialization();
                break;

            case PLACING_SCAFFOLDING:
                phasePlacingScaffolding();
                break;

            case BUILDING_STRUCTURE:
                phaseBuildingStructure();
                break;

            case VERIFYING_BUILD:
                phaseVerifyingBuild();
                break;

            case REMOVING_SCAFFOLDING:
                phaseRemovingScaffolding();
                break;

            case COMPLETE:
                result = ActionResult.success("Build complete with scaffolding!");
                break;
        }
    }

    private void phaseInitialization() {
        // Calculate structure bounds
        AABB bounds = new AABB(
            buildPlan.get(0).pos,
            buildPlan.get(buildPlan.size() - 1).pos
        );

        // Select appropriate scaffold pattern
        ScaffoldPattern pattern = scaffoldingManager.selectPattern(
            width, depth, height
        );

        foreman.sendChatMessage("Using scaffold pattern: " + pattern.name());

        currentPhase = BuildPhase.PLACING_SCAFFOLDING;
    }

    private void phasePlacingScaffolding() {
        if (placedScaffolds == null) {
            // Calculate structure bounds
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (BlockPlacement placement : buildPlan) {
                minX = Math.min(minX, placement.pos.getX());
                minY = Math.min(minY, placement.pos.getY());
                minZ = Math.min(minZ, placement.pos.getZ());
                maxX = Math.max(maxX, placement.pos.getX());
                maxY = Math.max(maxY, placement.pos.getY());
                maxZ = Math.max(maxZ, placement.pos.getZ());
            }

            BoundingBox bounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

            // Place scaffolding
            ScaffoldPattern pattern = scaffoldingManager.selectPattern(width, depth, height);
            placedScaffolds = scaffoldingManager.placeScaffolding(bounds, pattern);

            foreman.sendChatMessage("Placed " + placedScaffolds.size() + " scaffold blocks");
        }

        // Enable flying for safe building
        foreman.setFlying(true);

        currentPhase = BuildPhase.BUILDING_STRUCTURE;
    }

    private void phaseBuildingStructure() {
        // Place multiple blocks per tick for faster building
        for (int i = 0; i < BLOCKS_PER_TICK; i++) {
            if (blocksPlaced >= buildPlan.size()) {
                break;
            }

            BlockPlacement placement = buildPlan.get(blocksPlaced);
            placeBlock(placement);
            blocksPlaced++;
        }

        // Update progress
        if (blocksPlaced % 20 == 0) {
            int percent = (blocksPlaced * 100) / buildPlan.size();
            foreman.sendChatMessage("Building progress: " + percent + "%");
        }

        // Check if building is complete
        if (blocksPlaced >= buildPlan.size()) {
            currentPhase = BuildPhase.VERIFYING_BUILD;
        }
    }

    private void phaseVerifyingBuild() {
        foreman.sendChatMessage("Verifying build completion...");

        // Verify all blocks are placed
        boolean allPlaced = true;
        for (BlockPlacement placement : buildPlan) {
            BlockState state = foreman.level().getBlockState(placement.pos);
            if (state.getBlock() != placement.block) {
                allPlaced = false;
                break;
            }
        }

        if (allPlaced) {
            currentPhase = BuildPhase.REMOVING_SCAFFOLDING;
        } else {
            foreman.sendChatMessage("Build verification failed - some blocks missing");
            // Attempt to place missing blocks
            currentPhase = BuildPhase.BUILDING_STRUCTURE;
        }
    }

    private void phaseRemovingScaffolding() {
        foreman.sendChatMessage("Removing scaffolding...");

        ScaffoldRemover remover = new ScaffoldRemover(
            (ServerLevel) foreman.level(), foreman, placedScaffolds
        );

        ActionResult removalResult = remover.removeAll();

        if (removalResult.isSuccess()) {
            foreman.sendChatMessage("Scaffolding removed successfully!");
            currentPhase = BuildPhase.COMPLETE;
        } else {
            result = ActionResult.failure("Scaffold removal failed: " +
                removalResult.getMessage());
        }

        // Disable flying after cleanup
        foreman.setFlying(false);
    }

    private void placeBlock(BlockPlacement placement) {
        // Teleport close if needed
        BlockPos pos = placement.pos;
        if (!foreman.blockPosition().closerThan(pos, 5.0)) {
            foreman.teleportTo(pos.getX() + 2.0, pos.getY(), pos.getZ() + 2.0);
        }

        // Place the block
        foreman.level().setBlock(pos, placement.block.defaultBlockState(), 3);

        // Play sound
        foreman.level().playSound(null, pos,
            placement.block.defaultBlockState().getSoundType(foreman.level(), pos, foreman).getPlaceSound(),
            net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private BlockPos findBuildPosition() {
        // Use existing ground finding logic from BuildStructureAction
        BlockPos playerPos = findNearestPlayerPosition();
        if (playerPos == null) {
            playerPos = foreman.blockPosition();
        }

        return findGroundLevel(playerPos.offset(5, 0, 5));
    }

    private BlockPos findNearestPlayerPosition() {
        var players = foreman.level().players();
        return players.isEmpty() ? null : players.get(0).blockPosition();
    }

    private BlockPos findGroundLevel(BlockPos startPos) {
        for (int y = startPos.getY(); y >= startPos.getY() - 20; y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (foreman.level().getBlockState(checkPos.below()).isSolid()) {
                return new BlockPos(startPos.getX(), y, startPos.getZ());
            }
        }
        return startPos;
    }

    @Override
    protected void onCancel() {
        // Clean up scaffolding on cancel
        if (placedScaffolds != null && !placedScaffolds.isEmpty()) {
            ScaffoldRemover remover = new ScaffoldRemover(
                (ServerLevel) foreman.level(), foreman, placedScaffolds
            );
            remover.removeAll();
        }

        foreman.setFlying(false);
    }

    @Override
    public String getDescription() {
        return "Building " + structureType + " with scaffolding (Phase: " + currentPhase + ")";
    }

    private enum BuildPhase {
        INITIALIZATION,
        PLACING_SCAFFOLDING,
        BUILDING_STRUCTURE,
        VERIFYING_BUILD,
        REMOVING_SCAFFOLDING,
        COMPLETE
    }
}
```

### 8.2 Integration Example

```java
// Register in CoreActionsPlugin.java
@Override
public void registerActions(ActionRegistry registry) {
    // ... existing registrations ...

    registry.register("build_with_scaffolding", (foreman, task, context) ->
        new BuildWithScaffoldingAction(foreman, task));

    registry.register("remove_scaffolding", (foreman, task, context) ->
        new RemoveScaffoldingAction(foreman, task));
}

// Update PromptBuilder.java to include scaffolding commands
private String getScaffoldingActionsPrompt() {
    return """
        **Scaffolding Actions:**
        - build_with_scaffolding: Build a structure with automatic scaffolding
          Parameters: structure (string), width (int), height (int), depth (int)
          Example: "build_with_scaffolding structure=tower width=5 height=15 depth=5"

        - remove_scaffolding: Remove all scaffolding from current build
          Parameters: none
          Example: "remove_scaffolding"
        """;
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

**HeightCalculator Tests:**
```java
@Test
void testCalculateScaffoldLevels_SmallStructure() {
    List<Integer> levels = HeightCalculator.calculateScaffoldLevels(64, 70);
    assertFalse(levels.isEmpty());
    assertTrue(levels.get(0) > 64); // Above base
    assertTrue(levels.get(levels.size() - 1) >= 70); // Reaches max height
}

@Test
void testCalculateScaffoldLevels_LargeStructure() {
    List<Integer> levels = HeightCalculator.calculateScaffoldLevels(64, 100);
    assertTrue(levels.size() > 5); // More levels for taller structure
}
```

**ScaffoldPlacer Tests:**
```java
@Test
void testPlaceSingleTower_PlacesCorrectNumberOfBlocks() {
    ScaffoldPlacer placer = new ScaffoldPlacer(testLevel, testForeman);
    BoundingBox bounds = new BoundingBox(0, 64, 0, 10, 80, 10);

    Set<BlockPos> scaffolds = placer.placeSingleTower(bounds,
        List.of(70, 75, 80));

    assertFalse(scaffolds.isEmpty());
    assertTrue(scaffolds.size() >= 9); // Base platform (3x3)
}
```

**SafetyMonitor Tests:**
```java
@Test
void testSafetyMonitor_DetectsDangerousHeight() {
    SafetyMonitor monitor = new SafetyMonitor(testForeman);
    testForeman.teleportTo(0, 80, 0); // High above ground

    // Simulate safety check
    boolean safe = monitor.isSafePosition(testForeman.blockPosition());
    assertFalse(safe); // Should detect danger
}
```

### 9.2 Integration Tests

**End-to-End Build Test:**
```java
@Test
void testBuildWithScaffolding_CompleteFlow() {
    // Create task
    Task task = new Task("build_with_scaffolding");
    task.setParameter("structure", "tower");
    task.setParameter("width", 7);
    task.setParameter("height", 15);
    task.setParameter("depth", 7);

    // Create action
    BuildWithScaffoldingAction action =
        new BuildWithScaffoldingAction(testForeman, task);

    // Run action
    action.start();
    for (int i = 0; i < 1000 && !action.isComplete(); i++) {
        action.tick();
    }

    // Verify success
    assertTrue(action.isComplete());
    assertTrue(action.getResult().isSuccess());

    // Verify scaffolding was removed
    // (check that no dirt/cobblestone scaffold blocks remain)
}
```

### 9.3 Performance Tests

**Scaffold Placement Performance:**
```java
@Test
void testScaffoldPlacement_Performance_LargeStructure() {
    ScaffoldPlacer placer = new ScaffoldPlacer(testLevel, testForeman);
    BoundingBox largeBounds = new BoundingBox(0, 64, 0, 50, 150, 50);

    long startTime = System.nanoTime();
    Set<BlockPos> scaffolds = placer.placeScaffolding(largeBounds,
        ScaffoldPattern.CORNER_TOWERS);
    long duration = System.nanoTime() - startTime;

    // Should complete within 100ms
    assertTrue(duration < 100_000_000);
    assertFalse(scaffolds.isEmpty());
}
```

### 9.4 Safety Tests

**Fall Damage Prevention Test:**
```java
@Test
void testFallDamagePrevention_WithScaffolding() {
    // Place scaffolding
    ScaffoldPlacer placer = new ScaffoldPlacer(testLevel, testForeman);
    Set<BlockPos> scaffolds = placer.placeSingleTower(testBounds, testLevels);

    // Teleport agent high up
    testForeman.teleportTo(0, 80, 0);

    // Run safety monitor
    SafetyMonitor monitor = new SafetyMonitor(testForeman);
    for (int i = 0; i < 100; i++) {
        monitor.tick();
    }

    // Verify agent didn't take damage
    assertEquals(testForeman.getHealth(), testForeman.getMaxHealth());
}
```

**MLG Water Bucket Test:**
```java
@Test
void testMLGWaterBucket_SuccessfulFallBreak() {
    WaterBucketFallBreak mlg = new WaterBucketFallBreak(testForeman);

    // Teleport agent high up
    testForeman.teleportTo(0, 100, 0);
    testForeman.setDeltaMovement(new Vec3(0, -2.0, 0)); // Start falling

    // Simulate fall
    for (int i = 0; i < 50; i++) {
        mlg.tick();
        testForeman.tick();
    }

    // Verify water was placed
    BlockState landingState = testLevel.getBlockState(new BlockPos(0, 64, 0));
    assertTrue(landingState.is(Blocks.WATER));

    // Verify no fall damage
    assertEquals(testForeman.getHealth(), testForeman.getMaxHealth());
}
```

---

## Conclusion

This scaffolding build AI system provides a comprehensive solution for safe, efficient building in the MineWright mod. The design prioritizes agent safety through multiple layers of protection:

1. **Preventive**: Height-aware scaffolding placement before building
2. **Active**: Real-time safety monitoring during construction
3. **Emergency**: Water bucket MLG fall breaks as last resort
4. **Cleanup**: Automatic scaffold removal after build completion

The modular architecture allows for incremental implementation, with each phase building on the previous one. The system integrates seamlessly with existing MineWright components like `BuildStructureAction`, `CollaborativeBuildManager`, and `ForemanEntity`.

**Next Steps:**
1. Review and approve this design document
2. Begin Phase 1 implementation (Core Scaffolding System)
3. Set up unit test framework for scaffolding components
4. Create configuration schema for scaffolding options
5. Implement progress tracking and reporting

**Estimated Timeline:** 8 weeks for full implementation
**Team Size:** 1-2 developers
**Risk Level:** Medium (involves entity movement and fall damage mechanics)

---

## Sources

- [Forge Scaffold API Documentation](https://download.csdn.net/download/m0_74337424/90594518)
- [Structure Gel API](https://www.curseforge.com/minecraft/search?class=mc-mods&page=1&pageSize=20&search=structure&sortBy=relevance)
- [Scaffolding Backported Mod](https://www.mcmod.cn/class/19562.html)
- [Baritone Client Mod (Auto-Build Reference)](http://www.9minecraft.net/baritone-client-mod/)
- [No Fall Damage Mod](https://www.mcmod.cn/class/23752.html)
- [Minecraft Wiki: Fall Damage Reduction Tutorial](https://wiki.biligame.com/mc/Tutorial:%E5%87%8F%E5%B0%91%E6%91%94%E8%90%BD%E4%BC%A4%E5%AE%B3)
- [Configurable Falls Mod](https://m.mcmod.cn/class/12564.html)
