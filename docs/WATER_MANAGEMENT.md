# Water Management AI for MineWright Minecraft Mod

## Executive Summary

This document outlines the design and implementation strategy for a comprehensive water management AI system for MineWright (Forge 1.20.1). The system enables Foreman entities to detect, create, manage, and navigate water sources for farming, irrigation, transportation, and resource collection.

**Version:** 1.0
**Date:** 2026-02-27
**Status:** Design Phase

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Water Source Detection](#water-source-detection)
3. [Irrigation Systems](#irrigation-systems)
4. [Water Flow Mechanics](#water-flow-mechanics)
5. [Infinite Water Sources](#infinite-water-sources)
6. [Underwater Navigation](#underwater-navigation)
7. [Integration Architecture](#integration-architecture)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Code Examples](#code-examples)
10. [Testing Strategy](#testing-strategy)

---

## System Overview

### Design Goals

1. **Smart Water Detection**: Efficiently locate and catalog water sources within scanning range
2. **Automated Irrigation**: Design and build irrigation systems for farms with optimal hydration
3. **Flow Understanding**: Leverage Minecraft's water physics for transportation and mechanisms
4. **Source Management**: Create and maintain infinite water sources for sustainable operations
5. **Underwater Operations**: Navigate and work underwater for resource gathering and construction

### Core Components

```
WaterManagementSystem
├── WaterDetector (source detection and cataloging)
├── IrrigationPlanner (farm hydration design)
├── WaterFlowSimulator (flow mechanics and prediction)
├── InfiniteSourceBuilder (source creation)
├── UnderwaterNavigator (submerged pathfinding)
├── WaterMemory (water source cache)
└── WaterActionPlugin (action registration)
```

---

## Water Source Detection

### Detection Algorithms

Water sources must be detected efficiently without impacting tick performance. The system uses spatial scanning with caching to maintain awareness of nearby water.

#### 1. Radial Water Scanner

```java
package com.minewright.water;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

/**
 * Detects and catalogs water sources within scanning range.
 * Uses efficient radial scanning with caching to minimize performance impact.
 */
public class WaterDetector {
    private final ForemanEntity foreman;
    private final int scanRadius;
    private Map<BlockPos, WaterSourceInfo> detectedSources;
    private int lastScanTick = 0;
    private static final int SCAN_COOLDOWN = 100; // 5 seconds between scans

    public WaterDetector(ForemanEntity foreman, int scanRadius) {
        this.foreman = foreman;
        this.scanRadius = scanRadius;
        this.detectedSources = new HashMap<>();
    }

    /**
     * Scans for water sources if cooldown has elapsed.
     * Returns cached results if scan was recent.
     */
    public Map<BlockPos, WaterSourceInfo> detectWaterSources() {
        int currentTick = foreman.tickCount;

        if (currentTick - lastScanTick < SCAN_COOLDOWN && !detectedSources.isEmpty()) {
            return detectedSources; // Return cached results
        }

        lastScanTick = currentTick;
        detectedSources.clear();

        Level level = foreman.level();
        BlockPos center = foreman.blockPosition();

        // Scan in expanding cubes for efficiency
        for (int y = -scanRadius; y <= scanRadius; y += 2) {
            for (int x = -scanRadius; x <= scanRadius; x += 2) {
                for (int z = -scanRadius; z <= scanRadius; z += 2) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (isWaterBlock(state)) {
                        WaterSourceInfo info = analyzeWaterSource(checkPos, state);
                        if (info != null) {
                            detectedSources.put(checkPos, info);
                        }
                    }
                }
            }
        }

        return detectedSources;
    }

    /**
     * Analyzes a water block to determine its type and properties.
     */
    private WaterSourceInfo analyzeWaterSource(BlockPos pos, BlockState state) {
        Level level = foreman.level();

        // Check if this is a source block (level 0)
        boolean isSource = state.getFluidState().isSource();

        // Determine water body type
        WaterBodyType type = classifyWaterBody(pos, isSource);

        // Calculate volume estimate
        int volume = estimateWaterVolume(pos, type);

        // Check for nearby infinite source setup
        boolean isInfinitePool = isInfinitePoolCandidate(pos);

        return new WaterSourceInfo(pos, isSource, type, volume, isInfinitePool);
    }

    /**
     * Classifies the type of water body (lake, river, ocean, pool).
     */
    private WaterBodyType classifyWaterBody(BlockPos pos, boolean isSource) {
        Level level = foreman.level();
        int adjacentWater = 0;
        int depth = 0;

        // Check adjacent blocks
        for (BlockPos offset : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            if (level.getFluidState(offset).is(Fluids.WATER)) {
                adjacentWater++;
            }
        }

        // Measure depth
        BlockPos belowPos = pos.below();
        while (level.getFluidState(belowPos).is(Fluids.WATER)) {
            depth++;
            belowPos = belowPos.below();
        }

        // Classification logic
        if (adjacentWater >= 8 && depth >= 5) {
            return WaterBodyType.OCEAN;
        } else if (adjacentWater >= 5) {
            return WaterBodyType.RIVER;
        } else if (depth >= 3) {
            return WaterBodyType.LAKE;
        } else if (adjacentWater >= 2) {
            return WaterBodyType.POOL;
        } else {
            return WaterBodyType.SINGLE_BLOCK;
        }
    }

    /**
     * Estimates the volume of a water body in blocks.
     */
    private int estimateWaterVolume(BlockPos pos, WaterBodyType type) {
        return switch (type) {
            case OCEAN -> 1000; // Large, don't count precisely
            case RIVER -> estimateConnectedVolume(pos, 100);
            case LAKE -> estimateConnectedVolume(pos, 50);
            case POOL -> estimateConnectedVolume(pos, 20);
            case SINGLE_BLOCK -> 1;
        };
    }

    /**
     * Estimates volume by flood-filling connected water blocks.
     */
    private int estimateConnectedVolume(BlockPos start, int maxBlocks) {
        Level level = foreman.level();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);
        int count = 0;

        while (!queue.isEmpty() && count < maxBlocks) {
            BlockPos current = queue.poll();
            count++;

            for (BlockPos offset : BlockPos.betweenClosed(
                current.offset(-1, -1, -1),
                current.offset(1, 1, 1)
            )) {
                if (!visited.contains(offset) &&
                    level.getFluidState(offset).is(Fluids.WATER)) {
                    visited.add(offset);
                    queue.add(offset);
                }
            }
        }

        return count;
    }

    /**
     * Checks if position is a candidate for infinite water source.
     * An infinite pool requires at least 2 adjacent source blocks.
     */
    private boolean isInfinitePoolCandidate(BlockPos pos) {
        Level level = foreman.level();
        int adjacentSources = 0;

        // Check 4 horizontal neighbors
        BlockPos[] neighbors = {
            pos.east(), pos.west(), pos.north(), pos.south()
        };

        for (BlockPos neighbor : neighbors) {
            BlockState state = level.getBlockState(neighbor);
            if (state.getFluidState().isSource() &&
                state.getFluidState().is(Fluids.WATER)) {
                adjacentSources++;
            }
        }

        return adjacentSources >= 2;
    }

    private boolean isWaterBlock(BlockState state) {
        return state.is(Blocks.WATER) || state.getFluidState().is(Fluids.WATER);
    }

    /**
     * Finds the nearest water source of a given type.
     */
    public Optional<BlockPos> findNearestWater(WaterBodyType preferredType) {
        detectWaterSources(); // Ensure fresh scan

        return detectedSources.entrySet().stream()
            .filter(e -> e.getValue().type() == preferredType || preferredType == WaterBodyType.ANY)
            .min(Comparator.comparing(e -> e.getKey().distSqr(foreman.blockPosition())))
            .map(Map.Entry::getKey);
    }

    /**
     * Gets all infinite water sources within range.
     */
    public List<BlockPos> getInfiniteSources() {
        detectWaterSources();

        return detectedSources.entrySet().stream()
            .filter(e -> e.getValue().isInfinitePool())
            .map(Map.Entry::getKey)
            .toList();
    }
}

/**
 * Information about a detected water source.
 */
record WaterSourceInfo(
    BlockPos position,
    boolean isSourceBlock,
    WaterBodyType type,
    int estimatedVolume,
    boolean isInfinitePool
) {}

/**
 * Classification of water bodies.
 */
enum WaterBodyType {
    OCEAN,      // Large deep water bodies
    RIVER,      // Flowing water channels
    LAKE,       // Static medium-sized bodies
    POOL,       // Small collections of water blocks
    SINGLE_BLOCK,
    ANY         // Wildcard for searches
}
```

#### 2. Water Memory Cache

```java
package com.minewright.water;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent memory of water sources across sessions.
 * Helps Foreman remember where water is located for future reference.
 */
public class WaterMemory {
    private Map<String, WaterSourceMemory> memories = new HashMap<>();

    public void rememberSource(String dimensionKey, BlockPos pos, WaterBodyType type) {
        memories.computeIfAbsent(dimensionKey, k -> new WaterSourceMemory())
            .addSource(pos, type);
    }

    public WaterSourceMemory getMemory(String dimensionKey) {
        return memories.get(dimensionKey);
    }

    public void saveToNBT(CompoundTag tag) {
        for (Map.Entry<String, WaterSourceMemory> entry : memories.entrySet()) {
            CompoundTag memoryTag = new CompoundTag();
            entry.getValue().saveToNBT(memoryTag);
            tag.put(entry.getKey(), memoryTag);
        }
    }

    public void loadFromNBT(CompoundTag tag) {
        memories.clear();
        for (String key : tag.getAllKeys()) {
            WaterSourceMemory memory = new WaterSourceMemory();
            memory.loadFromNBT(tag.getCompound(key));
            memories.put(key, memory);
        }
    }

    public static class WaterSourceMemory {
        private Map<BlockPos, WaterBodyType> sources = new HashMap<>();
        private Map<BlockPos, Integer> visitCount = new HashMap<>();

        public void addSource(BlockPos pos, WaterBodyType type) {
            sources.put(pos, type);
            visitCount.put(pos, visitCount.getOrDefault(pos, 0) + 1);
        }

        public Map<BlockPos, WaterBodyType> getSources() {
            return sources;
        }

        public BlockPos getMostVisitedSource() {
            return visitCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }

        public void saveToNBT(CompoundTag tag) {
            // Serialize sources to NBT
        }

        public void loadFromNBT(CompoundTag tag) {
            // Deserialize from NBT
        }
    }
}
```

---

## Irrigation Systems

### Irrigation Pattern Design

The irrigation system uses efficient hydration patterns to maximize farmland hydration while minimizing water usage.

#### 1. Irrigation Planner

```java
package com.minewright.water;

import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * Plans efficient irrigation systems for farms.
 * Maximizes hydrated farmland while minimizing water channeling.
 */
public class IrrigationPlanner {
    private final ForemanEntity foreman;

    public IrrigationPlanner(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Designs an irrigation layout for a rectangular farm area.
     * Returns a list of water channel placements.
     */
    public IrrigationDesign designIrrigation(BlockPos start, int width, int depth, IrrigationPattern pattern) {
        return switch (pattern) {
            case CENTRAL_SPRINKLER -> designCentralSprinkler(start, width, depth);
            case GRID_CHANNELS -> designGridChannels(start, width, depth);
            case PERIMETER_CHANNEL -> designPerimeterChannel(start, width, depth);
            case OPTIMAL_SPACING -> designOptimalSpacing(start, width, depth);
        };
    }

    /**
     * Central sprinkler design - single water source in center.
     * Best for small farms (9x9 or smaller).
     */
    private IrrigationDesign designCentralSprinkler(BlockPos start, int width, int depth) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Calculate center position
        int centerX = start.getX() + width / 2;
        int centerZ = start.getZ() + depth / 2;
        BlockPos centerPos = new BlockPos(centerX, start.getY(), centerZ);

        // Place water at center
        placements.add(new BlockPlacement(centerPos, Blocks.WATER));

        // Calculate hydrated farmland count
        int hydratedRadius = 4; // Water hydrates 4 blocks in all directions
        int hydratedBlocks = Math.min(width * depth, (int) Math.PI * hydratedRadius * hydratedRadius);

        return new IrrigationDesign(
            placements,
            "Central Sprinkler",
            hydratedBlocks,
            1
        );
    }

    /**
     * Grid channels design - water channels spaced every 8 blocks.
     * Best for large farms requiring uniform hydration.
     */
    private IrrigationDesign designGridChannels(BlockPos start, int width, int depth) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Spacing: water hydrates 4 blocks, so place channels every 8 blocks
        final int CHANNEL_SPACING = 8;

        // Create horizontal channels
        for (int z = 0; z < depth; z += CHANNEL_SPACING) {
            for (int x = 0; x < width; x++) {
                placements.add(new BlockPlacement(
                    start.offset(x, 0, z),
                    Blocks.WATER
                ));
            }
        }

        // Create vertical channels
        for (int x = 0; x < width; x += CHANNEL_SPACING) {
            for (int z = 0; z < depth; z++) {
                placements.add(new BlockPlacement(
                    start.offset(x, 0, z),
                    Blocks.WATER
                ));
            }
        }

        // Calculate efficiency
        int totalBlocks = width * depth;
        int waterBlocks = placements.size();
        int farmlandBlocks = totalBlocks - waterBlocks;

        return new IrrigationDesign(
            placements,
            "Grid Channels",
            farmlandBlocks,
            waterBlocks
        );
    }

    /**
     * Perimeter channel design - water around the edges.
     * Creates a moat-like irrigation system.
     */
    private IrrigationDesign designPerimeterChannel(BlockPos start, int width, int depth) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Top edge
        for (int x = 0; x < width; x++) {
            placements.add(new BlockPlacement(start.offset(x, 0, 0), Blocks.WATER));
        }

        // Bottom edge
        for (int x = 0; x < width; x++) {
            placements.add(new BlockPlacement(start.offset(x, 0, depth - 1), Blocks.WATER));
        }

        // Left edge
        for (int z = 1; z < depth - 1; z++) {
            placements.add(new BlockPlacement(start.offset(0, 0, z), Blocks.WATER));
        }

        // Right edge
        for (int z = 1; z < depth - 1; z++) {
            placements.add(new BlockPlacement(start.offset(width - 1, 0, z), Blocks.WATER));
        }

        // Calculate hydrated farmland (all blocks within 4 of edge)
        int hydratedBlocks = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int distToEdge = Math.min(
                    Math.min(x, width - 1 - x),
                    Math.min(z, depth - 1 - z)
                );
                if (distToEdge <= 4) {
                    hydratedBlocks++;
                }
            }
        }

        return new IrrigationDesign(
            placements,
            "Perimeter Channel",
            hydratedBlocks,
            placements.size()
        );
    }

    /**
     * Optimal spacing design - algorithmically determines best placement.
     * Uses hexagonal spacing for maximum coverage.
     */
    private IrrigationDesign designOptimalSpacing(BlockPos start, int width, int depth) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Hexagonal packing for optimal coverage
        final double SPACING = 7.0; // Slightly less than 8 for overlap
        final double HEX_OFFSET = SPACING * Math.sqrt(3) / 2;

        int row = 0;
        for (double z = 0; z < depth; z += HEX_OFFSET) {
            double xOffset = (row % 2 == 0) ? 0 : SPACING / 2;

            for (double x = xOffset; x < width; x += SPACING) {
                int ix = (int) Math.round(x);
                int iz = (int) Math.round(z);

                if (ix < width && iz < depth) {
                    placements.add(new BlockPlacement(
                        start.offset(ix, 0, iz),
                        Blocks.WATER
                    ));
                }
            }
            row++;
        }

        // Estimate hydrated blocks (conservative estimate)
        int hydratedBlocks = (int) (width * depth * 0.85); // ~85% coverage

        return new IrrigationDesign(
            placements,
            "Optimal Hexagonal Spacing",
            hydratedBlocks,
            placements.size()
        );
    }

    /**
     * Analyzes an existing farm and suggests irrigation improvements.
     */
    public IrrigationSuggestion analyzeFarm(BlockPos start, int width, int depth) {
        // Check current hydration status
        int totalBlocks = width * depth;
        int hydratedBlocks = countHydratedBlocks(start, width, depth);
        double hydrationRatio = (double) hydratedBlocks / totalBlocks;

        if (hydrationRatio >= 0.95) {
            return new IrrigationSuggestion(
                "Farm is well-hydrated!",
                IrrigationPattern.NONE,
                true
            );
        }

        // Suggest best pattern based on size
        IrrigationPattern suggested;
        if (width <= 9 && depth <= 9) {
            suggested = IrrigationPattern.CENTRAL_SPRINKLER;
        } else if (width > 20 || depth > 20) {
            suggested = IrrigationPattern.GRID_CHANNELS;
        } else {
            suggested = IrrigationPattern.OPTIMAL_SPACING;
        }

        return new IrrigationSuggestion(
            String.format("Farm is only %.1f%% hydrated. Consider %s.",
                hydrationRatio * 100, suggested),
            suggested,
            false
        );
    }

    private int countHydratedBlocks(BlockPos start, int width, int depth) {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos checkPos = start.offset(x, 0, z);
                if (isHydrated(checkPos)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isHydrated(BlockPos pos) {
        // Check if position is within 4 blocks of a water source
        return foreman.level().getBlockState(pos.above())
            .hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE);
    }
}

/**
 * Represents a complete irrigation design.
 */
record IrrigationDesign(
    List<BlockPlacement> waterPlacements,
    String patternName,
    int hydratedFarmlandCount,
    int waterBlockCount
) {
    public double getEfficiency() {
        return waterBlockCount == 0 ? 0 :
            (double) hydratedFarmlandCount / waterBlockCount;
    }
}

/**
 * Suggestion for farm irrigation.
 */
record IrrigationSuggestion(
    String message,
    IrrigationPattern suggestedPattern,
    boolean isOptimal
) {}

/**
 * Irrigation pattern types.
 */
enum IrrigationPattern {
    NONE,                // No irrigation needed
    CENTRAL_SPRINKLER,   // Single central water source
    GRID_CHANNELS,       // Grid of water channels
    PERIMETER_CHANNEL,   // Water around edges
    OPTIMAL_SPACING      // Hexagonal optimal spacing
}
```

#### 2. Automated Irrigation Builder Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.water.IrrigationDesign;
import com.minewright.water.IrrigationPattern;
import com.minewright.water.IrrigationPlanner;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Action that builds an irrigation system for a farm.
 */
public class IrrigateAction extends BaseAction {
    private IrrigationPlanner planner;
    private BlockPos farmStart;
    private int farmWidth;
    private int farmDepth;
    private IrrigationPattern pattern;

    private IrrigationDesign design;
    private List<BlockPlacement> placementsToBuild;
    private int currentIndex = 0;
    private int ticksRunning = 0;
    private static final int MAX_TICKS = 1200; // 1 minute timeout

    public IrrigateAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.planner = new IrrigationPlanner(foreman);
    }

    @Override
    protected void onStart() {
        // Parse task parameters
        farmStart = new BlockPos(
            task.getIntParameter("startX", foreman.blockPosition().getX()),
            task.getIntParameter("startY", foreman.blockPosition().getY()),
            task.getIntParameter("startZ", foreman.blockPosition().getZ())
        );
        farmWidth = task.getIntParameter("width", 9);
        farmDepth = task.getIntParameter("depth", 9);

        String patternStr = task.getStringParameter("pattern", "optimal");
        pattern = parsePattern(patternStr);

        // Design the irrigation system
        design = planner.designIrrigation(farmStart, farmWidth, farmDepth, pattern);
        placementsToBuild = design.waterPlacements();

        foreman.sendChatMessage(String.format(
            "Building %s irrigation: %d water blocks for %d hydrated farmland",
            design.patternName(),
            design.waterBlockCount(),
            design.hydratedFarmlandCount()
        ));
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Irrigation construction timeout");
            return;
        }

        if (currentIndex >= placementsToBuild.size()) {
            result = ActionResult.success(String.format(
                "Built irrigation system: %d water blocks placed",
                placementsToBuild.size()
            ));
            return;
        }

        // Place a few blocks per tick (2-3 for balance)
        int blocksThisTick = 0;
        while (currentIndex < placementsToBuild.size() && blocksThisTick < 2) {
            BlockPlacement placement = placementsToBuild.get(currentIndex);

            // Navigate to position if needed
            if (!foreman.blockPosition().closerThan(placement.pos(), 5.0)) {
                foreman.getNavigation().moveTo(
                    placement.pos().getX(),
                    placement.pos().getY(),
                    placement.pos().getZ(),
                    1.0
                );
                return;
            }

            // Place water block
            foreman.level().setBlock(
                placement.pos(),
                placement.block().defaultBlockState(),
                3
            );

            currentIndex++;
            blocksThisTick++;
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Irrigate farm (%dx%d) with %s pattern",
            farmWidth, farmDepth, pattern);
    }

    private IrrigationPattern parsePattern(String patternStr) {
        return switch (patternStr.toLowerCase()) {
            case "central", "sprinkler" -> IrrigationPattern.CENTRAL_SPRINKLER;
            case "grid", "channels" -> IrrigationPattern.GRID_CHANNELS;
            case "perimeter", "moat" -> IrrigationPattern.PERIMETER_CHANNEL;
            case "optimal", "hexagonal" -> IrrigationPattern.OPTIMAL_SPACING;
            default -> IrrigationPattern.OPTIMAL_SPACING;
        };
    }
}
```

---

## Water Flow Mechanics

### Flow Simulation and Prediction

Understanding water flow is essential for building mechanisms, transportation systems, and redstone contraptions.

#### 1. Water Flow Simulator

```java
package com.minewright.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

/**
 * Simulates and predicts water flow patterns.
 * Useful for designing water elevators, item transporters, and mechanisms.
 */
public class WaterFlowSimulator {
    private final Level level;

    public WaterFlowSimulator(Level level) {
        this.level = level;
    }

    /**
     * Predicts where water will flow from a source position.
     * Returns a set of positions that will contain water.
     */
    public Set<BlockPos> predictFlow(BlockPos sourcePos, int maxDepth) {
        Set<BlockPos> waterPositions = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(sourcePos);
        waterPositions.add(sourcePos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            // Check all 6 directions
            for (Direction direction : Direction.values()) {
                BlockPos nextPos = current.relative(direction);

                // Skip if already visited
                if (waterPositions.contains(nextPos)) {
                    continue;
                }

                // Check if water can flow there
                if (canWaterFlowTo(current, nextPos, direction)) {
                    waterPositions.add(nextPos);

                    // Only continue flowing if not at max depth
                    if (waterPositions.size() < maxDepth) {
                        queue.add(nextPos);
                    }
                }
            }
        }

        return waterPositions;
    }

    /**
     * Determines if water can flow from currentPos to nextPos.
     */
    private boolean canWaterFlowTo(BlockPos currentPos, BlockPos nextPos, Direction direction) {
        BlockState targetState = level.getBlockState(nextPos);

        // Water can flow into air
        if (targetState.isAir()) {
            return true;
        }

        // Water can flow into replaceable blocks
        if (targetState.getMaterial().isReplaceable()) {
            return true;
        }

        // Water can flow downward into waterlogged blocks
        if (direction == Direction.DOWN &&
            targetState.canBeReplaced(Fluids.WATER)) {
            return true;
        }

        // Water can flow horizontally if there's a solid block below
        if (direction.getAxis().isHorizontal()) {
            BlockState belowTarget = level.getBlockState(nextPos.below());
            if (belowTarget.isSolidRender(level, nextPos.below())) {
                // Check if target is air or waterlogged
                return targetState.isAir() ||
                       targetState.getFluidState().isEmpty();
            }
        }

        return false;
    }

    /**
     * Designs a water elevator for vertical transportation.
     */
    public WaterElevatorDesign designElevator(BlockPos base, int height) {
        List<BlockPos> waterBlocks = new ArrayList<>();
        List<BlockPos> supportingBlocks = new ArrayList<>();

        // Choose water elevator style based on height
        if (height <= 20) {
            return designSimpleElevator(base, height);
        } else {
            return designSoulSandElevator(base, height);
        }
    }

    /**
     * Simple water elevator using bubble column mechanics.
     */
    private WaterElevatorDesign designSimpleElevator(BlockPos base, int height) {
        List<BlockPos> waterBlocks = new ArrayList<>();
        List<BlockPos> soulSand = new ArrayList<>();
        List<BlockPos> glassWalls = new ArrayList<>();

        // Create the water column
        for (int y = 0; y < height; y++) {
            waterBlocks.add(base.above(y));

            // Add glass walls for containment (optional)
            glassWalls.add(base.above(y).east());
            glassWalls.add(base.above(y).west());
            glassWalls.add(base.above(y).north());
            glassWalls.add(base.above(y).south());
        }

        // Add soul sand at bottom for upward bubbles
        soulSand.add(base.below());

        return new WaterElevatorDesign(
            waterBlocks,
            soulSand,
            glassWalls,
            "Simple Bubble Elevator"
        );
    }

    /**
     * Soul sand elevator with multiple columns for taller heights.
     */
    private WaterElevatorDesign designSoulSandElevator(BlockPos base, int height) {
        List<BlockPos> waterBlocks = new ArrayList<>();
        List<BlockPos> soulSand = new ArrayList<>();
        List<BlockPos> glassWalls = new ArrayList<>();

        // Create a 2x2 water column for stability
        for (int y = 0; y < height; y++) {
            waterBlocks.add(base.above(y));
            waterBlocks.add(base.above(y).east());
            waterBlocks.add(base.above(y).south());
            waterBlocks.add(base.above(y).east().south());

            // Glass walls around the column
            for (BlockPos corner : List.of(
                base.above(y).east().east(),
                base.above(y).south().south(),
                base.above(y).west(),
                base.above(y).north()
            )) {
                glassWalls.add(corner);
            }
        }

        // Soul sand at bottom of each column
        for (BlockPos corner : List.of(base, base.east(), base.south(), base.east().south())) {
            soulSand.add(corner.below());
        }

        return new WaterElevatorDesign(
            waterBlocks,
            soulSand,
            glassWalls,
            "Multi-Column Bubble Elevator"
        );
    }

    /**
     * Designs an item transport system using water flow.
     */
    public ItemTransportDesign designItemTransport(BlockPos start, BlockPos end) {
        List<BlockPos> waterChannel = new ArrayList<>();
        List<BlockPos> signs = new ArrayList<>();
        List<BlockPos> hopperCollectors = new ArrayList<>();

        // Determine direction
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();

        // Create a simple L-shaped channel
        Direction primaryDir = Math.abs(dx) > Math.abs(dz) ?
            (dx > 0 ? Direction.EAST : Direction.WEST) :
            (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        BlockPos current = start;
        while (!current.equals(end)) {
            waterChannel.add(current.above());

            // Add signs to hold water in place every 8 blocks
            if (waterChannel.size() % 8 == 0) {
                signs.add(current.above(2));
            }

            // Move towards end
            if (current.getX() != end.getX()) {
                current = current.relative(primaryDir.getAxis() == Direction.Axis.X ?
                    primaryDir : Direction.EAST);
            } else if (current.getZ() != end.getZ()) {
                current = current.relative(Direction.SOUTH);
            } else {
                break;
            }
        }

        // Add hopper at end for collection
        hopperCollectors.add(end.above());

        return new ItemTransportDesign(
            waterChannel,
            signs,
            hopperCollectors,
            "Water Item Transport"
        );
    }

    /**
     * Calculates the power of water flow at a position.
     * Useful for determining if items can be pushed.
     */
    public int calculateFlowStrength(BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.getFluidState().is(Fluids.WATER)) {
            return 0;
        }

        // Source blocks have full strength
        if (state.getFluidState().isSource()) {
            return 8;
        }

        // Flowing water strength decreases with distance
        return 8 - state.getFluidState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL);
    }
}

/**
 * Design for a water elevator.
 */
record WaterElevatorDesign(
    List<BlockPos> waterBlocks,
    List<BlockPos> soulSandBlocks,
    List<BlockPos> glassWalls,
    String designType
) {}

/**
 * Design for item transport system.
 */
record ItemTransportDesign(
    List<BlockPos> waterChannel,
    List<BlockPos> supportSigns,
    List<BlockPos> collectionHoppers,
    String designType
) {}
```

---

## Infinite Water Sources

### Source Creation and Management

Infinite water sources are essential for sustainable operations. The system creates and manages these sources.

#### 1. Infinite Source Builder

```java
package com.minewright.water;

import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates infinite water sources for sustainable water supply.
 * An infinite source requires at least 2x2 or 1x3 arrangement of source blocks.
 */
public class InfiniteSourceBuilder {
    private final ForemanEntity foreman;

    public InfiniteSourceBuilder(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Designs an infinite water source at the given location.
     */
    public InfiniteSourceDesign designSource(BlockPos location, InfiniteSourceType type) {
        return switch (type) {
            case POOL_2X2 -> design2x2Pool(location);
            case TRENCH_1X3 -> design1x3Trench(location);
            case WELL_3X3 -> design3x3Well(location);
            case AUTO_REFILLING -> designAutoRefillingSource(location);
        };
    }

    /**
     * 2x2 pool - simplest infinite source.
     */
    private InfiniteSourceDesign design2x2Pool(BlockPos location) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Create 2x2 water source
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                placements.add(new BlockPlacement(
                    location.offset(x, 0, z),
                    Blocks.WATER
                ));
            }
        }

        // Add solid blocks around edges to prevent spreading
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 2; z++) {
                if (x < 0 || x >= 2 || z < 0 || z >= 2) {
                    placements.add(new BlockPlacement(
                        location.offset(x, -1, z),
                        Blocks.COBBLESTONE
                    ));
                }
            }
        }

        return new InfiniteSourceDesign(
            placements,
            "2x2 Infinite Pool",
            4,
            true
        );
    }

    /**
     * 1x3 trench - efficient infinite source.
     */
    private InfiniteSourceDesign design1x3Trench(BlockPos location) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Create 1x3 water source
        for (int z = 0; z < 3; z++) {
            placements.add(new BlockPlacement(
                location.offset(0, 0, z),
                Blocks.WATER
            ));
        }

        // Add solid blocks below and on sides
        for (int z = -1; z <= 3; z++) {
            placements.add(new BlockPlacement(
                location.offset(0, -1, z),
                Blocks.COBBLESTONE
            ));
            placements.add(new BlockPlacement(
                location.offset(-1, 0, z),
                Blocks.COBBLESTONE
            ));
            placements.add(new BlockPlacement(
                location.offset(1, 0, z),
                Blocks.COBBLESTONE
            ));
        }

        return new InfiniteSourceDesign(
            placements,
            "1x3 Infinite Trench",
            3,
            true
        );
    }

    /**
     * 3x3 well - larger infinite source with access.
     */
    private InfiniteSourceDesign design3x3Well(BlockPos location) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Create 3x3 water source
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                placements.add(new BlockPlacement(
                    location.offset(x, 0, z),
                    Blocks.WATER
                ));
            }
        }

        // Add stone walls
        for (int x = -1; x <= 3; x++) {
            for (int z = -1; z <= 3; z++) {
                if (x < 0 || x >= 3 || z < 0 || z >= 3) {
                    placements.add(new BlockPlacement(
                        location.offset(x, -1, z),
                        Blocks.STONE_BRICKS
                    ));
                    placements.add(new BlockPlacement(
                        location.offset(x, 0, z),
                        Blocks.STONE_BRICKS
                    ));
                }
            }
        }

        // Add fence for safety (optional)
        for (int x = -1; x <= 3; x++) {
            placements.add(new BlockPlacement(
                location.offset(x, 1, -1),
                Blocks.OAK_FENCE
            ));
            placements.add(new BlockPlacement(
                location.offset(x, 1, 3),
                Blocks.OAK_FENCE
            ));
        }

        return new InfiniteSourceDesign(
            placements,
            "3x3 Stone Well",
            9,
            true
        );
    }

    /**
     * Auto-refilling source with hoppers and dispensers.
     * Advanced design for automated water collection.
     */
    private InfiniteSourceDesign designAutoRefillingSource(BlockPos location) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Create 1x3 water source
        for (int z = 0; z < 3; z++) {
            placements.add(new BlockPlacement(
                location.offset(0, 0, z),
                Blocks.WATER
            ));
        }

        // Add container blocks (could be cauldrons later)
        placements.add(new BlockPlacement(
            location.offset(-2, 0, 1),
            Blocks.CHEST
        ));

        // Add hoppers to move water items
        placements.add(new BlockPlacement(
            location.offset(-1, 0, 1),
            Blocks.HOPPER
        ));

        return new InfiniteSourceDesign(
            placements,
            "Auto-Refilling Source",
            3,
            true
        );
    }

    /**
     * Validates if an existing water source is infinite.
     */
    public boolean isInfiniteSource(BlockPos pos) {
        // Check for 2x2 arrangement
        boolean has2x2 = true;
        for (int dx = 0; dx < 2 && has2x2; dx++) {
            for (int dz = 0; dz < 2 && has2x2; dz++) {
                BlockPos checkPos = pos.offset(dx, 0, dz);
                has2x2 &= isWaterSource(checkPos);
            }
        }

        if (has2x2) return true;

        // Check for 1x3 arrangement
        boolean has1x3 = true;
        for (int dz = 0; dz < 3 && has1x3; dz++) {
            has1x3 &= isWaterSource(pos.offset(0, 0, dz));
        }

        if (has1x3) return true;

        // Check for 3x1 arrangement
        boolean has3x1 = true;
        for (int dx = 0; dx < 3 && has3x1; dx++) {
            has3x1 &= isWaterSource(pos.offset(dx, 0, 0));
        }

        return has3x1;
    }

    private boolean isWaterSource(BlockPos pos) {
        return foreman.level().getBlockState(pos).getFluidState().isSourceOfType(net.minecraft.world.level.material.Fluids.WATER);
    }
}

/**
 * Design for an infinite water source.
 */
record InfiniteSourceDesign(
    List<BlockPlacement> blockPlacements,
    String designType,
    int sourceBlockCount,
    boolean isInfinite
) {}

/**
 * Types of infinite water sources.
 */
enum InfiniteSourceType {
    POOL_2X2,        // Simple 2x2 pool
    TRENCH_1X3,      // Efficient 1x3 trench
    WELL_3X3,        // Decorative 3x3 well
    AUTO_REFILLING   // Automated with hoppers/chests
}
```

#### 2. Create Infinite Source Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.water.InfiniteSourceBuilder;
import com.minewright.water.InfiniteSourceDesign;
import com.minewright.water.InfiniteSourceType;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Action that creates an infinite water source.
 */
public class CreateInfiniteWaterAction extends BaseAction {
    private InfiniteSourceBuilder builder;
    private BlockPos location;
    private InfiniteSourceType type;

    private InfiniteSourceDesign design;
    private List<BlockPlacement> placementsToBuild;
    private int currentIndex = 0;
    private int ticksRunning = 0;
    private static final int MAX_TICKS = 600; // 30 seconds

    public CreateInfiniteWaterAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.builder = new InfiniteSourceBuilder(foreman);
    }

    @Override
    protected void onStart() {
        location = new BlockPos(
            task.getIntParameter("x", foreman.blockPosition().getX()),
            task.getIntParameter("y", foreman.blockPosition().getY()),
            task.getIntParameter("z", foreman.blockPosition().getZ())
        );

        String typeStr = task.getStringParameter("type", "pool");
        type = parseSourceType(typeStr);

        design = builder.designSource(location, type);
        placementsToBuild = design.blockPlacements();

        foreman.sendChatMessage(String.format(
            "Building %s infinite water source (%d blocks)",
            design.designType(),
            placementsToBuild.size()
        ));
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Infinite source creation timeout");
            return;
        }

        if (currentIndex >= placementsToBuild.size()) {
            result = ActionResult.success(String.format(
                "Created %s infinite water source",
                design.designType()
            ));
            return;
        }

        // Place blocks
        int blocksThisTick = 0;
        while (currentIndex < placementsToBuild.size() && blocksThisTick < 3) {
            BlockPlacement placement = placementsToBuild.get(currentIndex);

            if (!foreman.blockPosition().closerThan(placement.pos(), 5.0)) {
                foreman.getNavigation().moveTo(
                    placement.pos().getX(),
                    placement.pos().getY(),
                    placement.pos().getZ(),
                    1.0
                );
                return;
            }

            foreman.level().setBlock(
                placement.pos(),
                placement.block().defaultBlockState(),
                3
            );

            currentIndex++;
            blocksThisTick++;
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Create infinite water source: %s at %s",
            type, location);
    }

    private InfiniteSourceType parseSourceType(String typeStr) {
        return switch (typeStr.toLowerCase()) {
            case "pool", "2x2" -> InfiniteSourceType.POOL_2X2;
            case "trench", "1x3" -> InfiniteSourceType.TRENCH_1X3;
            case "well", "3x3" -> InfiniteSourceType.WELL_3X3;
            case "auto", "refilling" -> InfiniteSourceType.AUTO_REFILLING;
            default -> InfiniteSourceType.POOL_2X2;
        };
    }
}
```

---

## Underwater Navigation

### Submerged Pathfinding and Operations

Underwater operations require specialized pathfinding and movement strategies.

#### 1. Underwater Navigator

```java
package com.minewright.water;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Specialized navigator for underwater movement and operations.
 * Handles 3D pathfinding, breathing management, and underwater hazards.
 */
public class UnderwaterNavigator {
    private final ForemanEntity foreman;
    private static final int MAX_UNDERWATER_DISTANCE = 64; // blocks
    private static final int AIR_SEARCH_RADIUS = 32; // blocks

    public UnderwaterNavigator(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Plans a path underwater with consideration for air pockets.
     */
    public UnderwaterPath planUnderwaterPath(BlockPos start, BlockPos target) {
        List<BlockPos> waypoints = new ArrayList<>();
        List<BlockPos> airPocketStops = new ArrayList<>();
        boolean requiresBreathing = false;

        // Check if direct path is underwater
        if (isUnderwater(start) || isUnderwater(target)) {
            requiresBreathing = true;

            // Find path with air pocket stops
            waypoints = calculateUnderwaterPath(start, target);
            airPocketStops = findAirPocketsAlongPath(waypoints);
        } else {
            // Direct path on land
            waypoints.add(target);
        }

        return new UnderwaterPath(
            waypoints,
            airPocketStops,
            requiresBreathing,
            estimateTravelTime(waypoints, airPocketStops)
        );
    }

    /**
     * Calculates an efficient underwater path using A*.
     */
    private List<BlockPos> calculateUnderwaterPath(BlockPos start, BlockPos target) {
        // Simplified pathfinding - in production would use proper A*
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;

        while (!current.equals(target)) {
            // Move towards target, prioritizing horizontal movement
            int dx = Integer.signum(target.getX() - current.getX());
            int dz = Integer.signum(target.getZ() - current.getZ());
            int dy = Integer.signum(target.getY() - current.getY());

            // Prefer horizontal movement
            if (dx != 0 || dz != 0) {
                BlockPos next = current.offset(dx, 0, dz);
                if (canPassThrough(next)) {
                    path.add(next);
                    current = next;
                    continue;
                }
            }

            // Then vertical
            if (dy != 0) {
                BlockPos next = current.offset(0, dy, 0);
                if (canPassThrough(next)) {
                    path.add(next);
                    current = next;
                    continue;
                }
            }

            // Try diagonal
            if (dx != 0) {
                BlockPos next = current.offset(dx, dy != 0 ? dy : 1, 0);
                if (canPassThrough(next)) {
                    path.add(next);
                    current = next;
                    continue;
                }
            }

            break; // Can't progress
        }

        return path;
    }

    /**
     * Finds air pockets along a path for breathing.
     */
    private List<BlockPos> findAirPocketsAlongPath(List<BlockPos> path) {
        List<BlockPos> airPockets = new ArrayList<>();
        BlockPos lastAirPocket = null;

        for (BlockPos pos : path) {
            // Check if we're far enough from last air pocket
            if (lastAirPocket != null &&
                pos.distSqr(lastAirPocket) < 100) { // 10 blocks
                continue;
            }

            // Check for air above
            if (hasAirAbove(pos)) {
                airPockets.add(pos);
                lastAirPocket = pos;
            }
        }

        return airPockets;
    }

    /**
     * Finds the nearest air pocket within search radius.
     */
    public Optional<BlockPos> findNearestAirPocket(BlockPos center) {
        Level level = foreman.level();

        // Search in expanding spheres
        for (int radius = 1; radius <= AIR_SEARCH_RADIUS; radius++) {
            for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)
            )) {
                if (hasAirAbove(pos) && pos.distSqr(center) <= radius * radius) {
                    return Optional.of(pos);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if there's air above the position.
     */
    private boolean hasAirAbove(BlockPos pos) {
        Level level = foreman.level();

        // Check blocks above
        for (int y = 1; y <= 3; y++) {
            BlockPos above = pos.above(y);
            if (level.getBlockState(above).isAir()) {
                return true;
            }
            if (level.getBlockState(above).isSolidRender(level, above)) {
                return false; // Blocked by solid block
            }
        }

        return false;
    }

    /**
     * Checks if position is underwater.
     */
    private boolean isUnderwater(BlockPos pos) {
        return foreman.level().getBlockState(pos).getFluidState().is(
            net.minecraft.world.level.material.Fluids.WATER
        );
    }

    /**
     * Checks if entity can pass through position.
     */
    private boolean canPassThrough(BlockPos pos) {
        Level level = foreman.level();
        return !level.getBlockState(pos).isSolidRender(level, pos) ||
               level.getBlockState(pos).getFluidState().is(
                   net.minecraft.world.level.material.Fluids.WATER
               );
    }

    /**
     * Estimates travel time in ticks.
     */
    private int estimateTravelTime(List<BlockPos> path, List<BlockPos> airStops) {
        // Underwater movement is slower (0.5x speed)
        int distance = path.size();
        int baseTime = distance * 10; // 10 ticks per block underwater

        // Add time for air stops
        int airStopTime = airStops.size() * 20; // 1 second per air stop

        return baseTime + airStopTime;
    }

    /**
     * Prepares the entity for underwater operation.
     */
    public void prepareForUnderwater() {
        // Could equip diving helmet, etc. in future
        foreman.sendChatMessage("Preparing for underwater operation...");
    }

    /**
     * Monitors underwater status and alerts if air is low.
     */
    public void monitorUnderwaterStatus() {
        int airSupply = foreman.getAirSupply();
        int maxAir = foreman.getMaxAirSupply();

        if (airSupply < maxAir * 0.3) { // Less than 30% air
            foreman.sendChatMessage("Air supply critical! Seeking surface...");

            // Try to find nearby air
            findNearestAirPocket(foreman.blockPosition()).ifPresent(airPos -> {
                foreman.getNavigation().moveTo(
                    airPos.getX(),
                    airPos.getY(),
                    airPos.getZ(),
                    1.5 // Higher speed when desperate
                );
            });
        }
    }
}

/**
 * Represents a planned underwater path.
 */
record UnderwaterPath(
    List<BlockPos> waypoints,
    List<BlockPos> airPocketStops,
    boolean requiresBreathing,
    int estimatedTicks
) {}
```

#### 2. Underwater Operation Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.water.UnderwaterNavigator;
import com.minewright.water.UnderwaterPath;
import net.minecraft.core.BlockPos;

/**
 * Action for performing underwater operations.
 */
public class UnderwaterAction extends BaseAction {
    private UnderwaterNavigator navigator;
    private BlockPos targetPosition;
    private String operationType;

    private UnderwaterPath plannedPath;
    private int currentWaypointIndex = 0;
    private boolean hasReachedTarget = false;
    private int ticksRunning = 0;
    private static final int MAX_TICKS = 2400; // 2 minutes

    public UnderwaterAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.navigator = new UnderwaterNavigator(foreman);
    }

    @Override
    protected void onStart() {
        targetPosition = new BlockPos(
            task.getIntParameter("x"),
            task.getIntParameter("y"),
            task.getIntParameter("z")
        );
        operationType = task.getStringParameter("operation", "navigate");

        // Plan the path
        plannedPath = navigator.planUnderwaterPath(
            foreman.blockPosition(),
            targetPosition
        );

        foreman.sendChatMessage(String.format(
            "Planning underwater path to %s - %d waypoints, %d air stops",
            targetPosition,
            plannedPath.waypoints().size(),
            plannedPath.airPocketStops().size()
        ));

        navigator.prepareForUnderwater();
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Underwater operation timeout");
            return;
        }

        // Monitor air supply
        navigator.monitorUnderwaterStatus();

        if (hasReachedTarget) {
            performOperation();
            return;
        }

        // Navigate to next waypoint
        if (currentWaypointIndex < plannedPath.waypoints().size()) {
            BlockPos nextWaypoint = plannedPath.waypoints().get(currentWaypointIndex);

            if (foreman.blockPosition().closerThan(nextWaypoint, 2.0)) {
                currentWaypointIndex++;

                // Check if this is an air pocket stop
                if (plannedPath.airPocketStops().contains(nextWaypoint)) {
                    foreman.sendChatMessage("Reaching air pocket...");
                    // Wait for air replenishment
                    try {
                        Thread.sleep(1000); // Brief pause
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                // Move toward waypoint
                foreman.getNavigation().moveTo(
                    nextWaypoint.getX(),
                    nextWaypoint.getY(),
                    nextWaypoint.getZ(),
                    0.8 // Slower speed underwater
                );
            }
        } else {
            // Reached target
            hasReachedTarget = true;
        }
    }

    private void performOperation() {
        // Perform the requested operation at target
        result = ActionResult.success(String.format(
            "Completed underwater %s at %s",
            operationType,
            targetPosition
        ));
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Underwater %s to %s",
            operationType, targetPosition);
    }
}
```

---

## Integration Architecture

### Plugin Registration

```java
package com.minewright.water;

import com.minewright.di.ServiceContainer;
import com.minewright.plugin.ActionPlugin;
import com.minewright.plugin.ActionRegistry;
import com.minewright.action.actions.*;

/**
 * Plugin that registers all water management actions.
 */
public class WaterManagementPlugin implements ActionPlugin {

    @Override
    public String getPluginId() {
        return "water-management";
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        // Register water detection
        registry.register("detect_water",
            (foreman, task, ctx) -> new DetectWaterAction(foreman, task),
            100, getPluginId());

        // Register irrigation
        registry.register("irrigate",
            (foreman, task, ctx) -> new IrrigateAction(foreman, task),
            100, getPluginId());

        // Register infinite source creation
        registry.register("create_infinite_water",
            (foreman, task, ctx) -> new CreateInfiniteWaterAction(foreman, task),
            100, getPluginId());

        // Register underwater navigation
        registry.register("underwater",
            (foreman, task, ctx) -> new UnderwaterAction(foreman, task),
            100, getPluginId());
    }

    @Override
    public void onUnload() {
        // Cleanup
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    public String[] getDependencies() {
        return new String[0];
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Water management: detection, irrigation, infinite sources, and underwater navigation";
    }
}
```

### Prompt Builder Updates

```java
// Add to PromptBuilder.java system prompt:

"""
ACTIONS:
- detect_water: {"radius": 32} - Scans for water sources within radius
- irrigate: {"startX": 0, "startY": 64, "startZ": 0, "width": 9, "depth": 9, "pattern": "optimal"} - Builds irrigation system
- create_infinite_water: {"x": 0, "y": 64, "z": 0, "type": "pool"} - Creates infinite water source
- underwater: {"x": 0, "y": 60, "z": 0, "operation": "navigate"} - Underwater navigation and operations

IRRIGATION PATTERNS:
- "central" - Single central water source (best for 9x9 farms)
- "grid" - Grid of channels (best for large farms)
- "optimal" - Hexagonal spacing (maximum coverage)
- "perimeter" - Edge channels (moat-style)

INFINITE SOURCE TYPES:
- "pool" - Simple 2x2 pool
- "trench" - Efficient 1x3 trench
- "well" - Decorative 3x3 well
- "auto" - Auto-refilling with hoppers

WATER OPERATIONS:
- "navigate" - Move to underwater position
- "collect" - Gather items underwater
- "build" - Place blocks underwater

EXAMPLES:
Input: "irrigate my farm"
{"reasoning": "Designing irrigation for farm hydration", "plan": "Build irrigation system", "tasks": [{"action": "irrigate", "parameters": {"startX": 0, "startY": 64, "startZ": 0, "width": 9, "depth": 9, "pattern": "optimal"}}]}

Input: "create an infinite water source"
{"reasoning": "Building infinite water source for sustainable operations", "plan": "Create 2x2 water pool", "tasks": [{"action": "create_infinite_water", "parameters": {"x": 0, "y": 64, "z": 0, "type": "pool"}}]}

Input: "find water nearby"
{"reasoning": "Scanning area for water sources", "plan": "Detect water within 32 blocks", "tasks": [{"action": "detect_water", "parameters": {"radius": 32}}]}
"""
```

---

## Implementation Roadmap

### Phase 1: Core Water Detection (Week 1-2)
- [ ] Implement `WaterDetector` with radial scanning
- [ ] Create `WaterMemory` for caching water sources
- [ ] Add `DetectWaterAction` with command integration
- [ ] Write tests for water detection accuracy

### Phase 2: Irrigation Systems (Week 3-4)
- [ ] Implement `IrrigationPlanner` with all patterns
- [ ] Add `IrrigateAction` for building irrigation
- [ ] Create farm hydration analysis
- [ ] Test irrigation efficiency on different farm sizes

### Phase 3: Infinite Sources (Week 5)
- [ ] Implement `InfiniteSourceBuilder` with all types
- [ ] Add `CreateInfiniteWaterAction`
- [ ] Create validation for infinite source detection
- [ ] Test source sustainability

### Phase 4: Water Flow Mechanics (Week 6)
- [ ] Implement `WaterFlowSimulator`
- [ ] Add elevator and transport designs
- [ ] Create flow prediction algorithms
- [ ] Test with redstone contraptions

### Phase 5: Underwater Navigation (Week 7-8)
- [ ] Implement `UnderwaterNavigator`
- [ ] Add `UnderwaterAction` with breathing management
- [ ] Create air pocket detection
- [ ] Test pathfinding in underwater environments

### Phase 6: Integration and Polish (Week 9-10)
- [ ] Register `WaterManagementPlugin`
- [ ] Update `PromptBuilder` with water actions
- [ ] Add comprehensive logging
- [ ] Performance optimization and testing
- [ ] Documentation and examples

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testWaterDetector_ScanForSources() {
    // Create test world with water sources
    Level level = createTestLevel();
    ForemanEntity foreman = createTestForeman(level);

    WaterDetector detector = new WaterDetector(foreman, 16);
    Map<BlockPos, WaterSourceInfo> sources = detector.detectWaterSources();

    assertFalse(sources.isEmpty());
    assertTrue(sources.values().stream().anyMatch(s -> s.isInfinitePool()));
}

@Test
public void testIrrigationPlanner_CentralSprinkler() {
    ForemanEntity foreman = createTestForeman();
    IrrigationPlanner planner = new IrrigationPlanner(foreman);

    IrrigationDesign design = planner.designIrrigation(
        new BlockPos(0, 64, 0), 9, 9, IrrigationPattern.CENTRAL_SPRINKLER
    );

    assertEquals(1, design.waterBlockCount());
    assertTrue(design.hydratedFarmlandCount() > 40);
}

@Test
public void testInfiniteSourceBuilder_2x2Pool() {
    ForemanEntity foreman = createTestForeman();
    InfiniteSourceBuilder builder = new InfiniteSourceBuilder(foreman);

    InfiniteSourceDesign design = builder.designSource(
        new BlockPos(0, 64, 0), InfiniteSourceType.POOL_2X2
    );

    assertTrue(design.isInfinite());
    assertEquals(4, design.sourceBlockCount());
}

@Test
public void testUnderwaterNavigator_PathPlanning() {
    ForemanEntity foreman = createTestForeman();
    UnderwaterNavigator navigator = new UnderwaterNavigator(foreman);

    UnderwaterPath path = navigator.planUnderwaterPath(
        new BlockPos(0, 60, 0), new BlockPos(20, 60, 20)
    );

    assertTrue(path.requiresBreathing());
    assertFalse(path.airPocketStops().isEmpty());
}
```

### Integration Tests

```java
@Test
public void testWaterManagement_FullWorkflow() {
    // 1. Detect water sources
    DetectWaterAction detect = new DetectWaterAction(foreman, task);
    detect.start();
    detect.tick();
    ActionResult detectResult = detect.getResult();
    assertTrue(detectResult.isSuccess());

    // 2. Create infinite source
    Task createTask = new Task("create_infinite_water", Map.of(
        "x", 100, "y", 64, "z", 100, "type", "pool"
    ));
    CreateInfiniteWaterAction create = new CreateInfiniteWaterAction(foreman, createTask);
    create.start();
    while (!create.isComplete()) {
        create.tick();
    }
    assertTrue(create.getResult()..isSuccess());

    // 3. Build irrigation
    Task irrigateTask = new Task("irrigate", Map.of(
        "startX", 0, "startY", 64, "startZ", 0,
        "width", 9, "depth", 9, "pattern", "optimal"
    ));
    IrrigateAction irrigate = new IrrigateAction(foreman, irrigateTask);
    irrigate.start();
    while (!irrigate.isComplete()) {
        irrigate.tick();
    }
    assertTrue(irrigate.getResult().isSuccess());
}
```

### Performance Tests

```java
@Test
public void testWaterDetector_Performance() {
    ForemanEntity foreman = createTestForeman();
    WaterDetector detector = new WaterDetector(foreman, 32);

    long startTime = System.nanoTime();
    Map<BlockPos, WaterSourceInfo> sources = detector.detectWaterSources();
    long duration = System.nanoTime() - startTime;

    // Should complete within 50ms
    assertTrue(duration < 50_000_000);
}

@Test
public void testFlowSimulation_Complexity() {
    Level level = createTestLevel();
    WaterFlowSimulator simulator = new WaterFlowSimulator(level);

    long startTime = System.nanoTime();
    Set<BlockPos> flow = simulator.predictFlow(new BlockPos(0, 64, 0), 1000);
    long duration = System.nanoTime() - startTime;

    // Should handle 1000 blocks efficiently
    assertTrue(duration < 100_000_000); // 100ms max
}
```

---

## Configuration

### Config File Updates

Add to `config/minewright-common.toml`:

```toml
[water]
# Maximum radius for water detection scans (default: 32)
detection_radius = 32

# Scan cooldown in ticks (default: 100 = 5 seconds)
scan_cooldown = 100

# Preferred irrigation pattern (default: optimal)
# Options: central, grid, perimeter, optimal
preferred_irrigation = "optimal"

# Enable infinite source creation (default: true)
allow_infinite_sources = true

# Enable underwater navigation (default: true)
allow_underwater = true

# Maximum underwater distance in blocks (default: 64)
max_underwater_distance = 64

# Air pocket warning threshold (default: 0.3 = 30%)
air_warning_threshold = 0.3

# Default infinite source type (default: pool)
# Options: pool, trench, well, auto
default_source_type = "pool"
```

---

## Conclusion

The Water Management AI system provides comprehensive water handling capabilities for MineWright, enabling Foreman entities to:

1. **Detect and catalog** water sources efficiently with spatial scanning
2. **Design and build** optimized irrigation systems for farms
3. **Simulate and predict** water flow for mechanisms
4. **Create infinite water sources** for sustainable operations
5. **Navigate underwater** safely with breathing management

The system integrates seamlessly with the existing architecture through the plugin system and follows MineWright's established patterns for actions, memory, and pathfinding.

### Key Benefits

- **Performance**: Cached water detection with configurable cooldowns
- **Flexibility**: Multiple irrigation patterns for different farm sizes
- **Safety**: Breathing management for underwater operations
- **Sustainability**: Infinite source creation for long-term operations
- **Intelligence**: Flow simulation for mechanism design

### Future Enhancements

- Water-based redstone contraption building
- Automated crop harvesting with water collection
- Underwater base construction
- Boat transportation and docking
- Custom water feature generation (fountains, waterfalls)
