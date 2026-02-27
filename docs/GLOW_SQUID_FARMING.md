# Glow Squid Farming AI for MineWright Minecraft Mod

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Design Document

## Table of Contents

1. [Overview](#overview)
2. [Glow Squid Spawning Mechanics](#glow-squid-spawning-mechanics)
3. [Farm Architecture](#farm-architecture)
4. [Dark Underwater Area Creation](#dark-underwater-area-creation)
5. [Glow Ink Sac Collection](#glow-ink-sac-collection)
6. [Underwater Navigation](#underwater-navigation)
7. [Light Level Management](#light-level-management)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Integration Guide](#integration-guide)

---

## Overview

Glow squids are unique aquatic mobs that spawn in dark underwater environments below Y=30. They drop valuable glow ink sacs used for creating glowing text on signs and item frames. This document outlines the design and implementation of an automated glow squid farming system for MineWright.

### Key Features

- **Dark underwater chamber construction** below Y=30
- **Spawn platform optimization** with water source blocks
- **Automated collection** using water flow and hopper systems
- **Underwater navigation** for AI agents to build and maintain farms
- **Light level management** ensuring complete darkness (light level 0)
- **Multi-agent coordination** for efficient farm operation

### Use Cases

- **Glow ink sac production** for decorative signs
- **XP farming** from glow squid kills
- **Automated resource collection** with minimal player interaction
- **Underwater base integration** with existing structures

---

## Glow Squid Spawning Mechanics

### Minecraft 1.20.1 Requirements

Glow squids have specific spawning requirements in Java Edition 1.20.1:

| Requirement | Details |
|-------------|---------|
| **Y-Level** | Must spawn below **Y=30** in the Overworld |
| **Light Level** | Complete darkness (**light level 0**) |
| **Water Type** | Water source blocks (not flowing water) |
| **Group Size** | Spawns in groups of **2-4** glow squids |
| **Biome** | Any Overworld biome (except deep dark) |
| **Block Below** | Stone-type blocks within 5 blocks below (tag: `base_stone_overworld`) |

### Stone Block Requirement

Within **5 blocks below** the spawning space, there must be a block with the `base_stone_overworld` tag, including:
- Stone
- Deepslate
- Andesite
- Granite
- Diorite
- Tuff

### Spawn Rate Mechanics

```
Spawn Rules (Java Edition 1.20.1):
- Spawn attempts: 1 per chunk per 10 ticks (0.5 seconds)
- Pack size: 2-4 glow squids per spawn
- Max hostile mobs: 70 within 128-block radius
- Despawn range: 128 blocks from player
```

---

## Farm Architecture

### Overall Farm Design

```
Y-Level Cross-Section:
┌─────────────────────────────────────────┐
│  Y=32      │ Access Tunnel (Dry)        │ ← Agent enters here
├─────────────────────────────────────────┤
│  Y=30      │ Ceiling (Stone)             │ ← Must be below Y=30
├─────────────────────────────────────────┤
│  Y=29      │ Spawn Platform (Water)      │ ← Glow squids spawn here
├─────────────────────────────────────────┤
│  Y=25      │ Stone Floor (Deepslate)     │ ← Base stone requirement
├─────────────────────────────────────────┤
│  Y=24      │ Water Flow Channels         │ ← Push squid to collection
├─────────────────────────────────────────┤
│  Y=20      │ Collection System           │ ← Hoppers + Chests
├─────────────────────────────────────────┘
```

### Spawn Chamber Design

```
Top View (Spawn Platform - Y=29):
┌──────────────────────────────────────┐
│  ╔═══════════════════════════════════╗ │
│  ║ [█][█][█][█][█][█][█][█][█][█][█] ║ │ ← Stone walls
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │ ← Water sources
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][≈][≈][≈][≈][≈][≈][≈][≈][≈][█] ║ │
│  ║ [█][█][█][█][█][█][█][█][█][█][█] ║ │
│  ╚═══════════════════════════════════╝ │
│      11x11 spawn platform               │
│      Light level 0 required             │
└──────────────────────────────────────┘

Legend:
█ = Stone wall (light-tight)
≈ = Water source block
```

### Multi-Layer Design

For optimal spawn rates, use multiple spawn layers:

```
4-Layer Stack (Y=20-29):
┌─────────────────────────┐
│  Y=29  │ Spawn Layer 1  │ ← Highest spawning level
├─────────────────────────┤
│  Y=27  │ Spawn Layer 2  │
├─────────────────────────┤
│  Y=25  │ Spawn Layer 3  │
├─────────────────────────┤
│  Y=23  │ Spawn Layer 4  │ ← Lowest spawning level
├─────────────────────────┤
│  Y=22  │ Water Funnel   │ ← Directs squid to collection
├─────────────────────────┤
│  Y=20  │ Collection     │ ← Hoppers + chests
└─────────────────────────┘
```

---

## Dark Underwater Area Creation

### 1. Location Selection

```java
package com.minewright.squid;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Selects optimal locations for glow squid farms
 */
public class SquidFarmLocationFinder {

    private final ForemanEntity foreman;

    public SquidFarmLocationFinder(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Finds a suitable location for glow squid farm
     * Requirements:
     * - Below Y=30
     * - Can be underground or underwater
     * - Has space for 11x11x9 chamber
     * - Near base stone blocks
     */
    public BlockPos findOptimalLocation() {
        Level level = foreman.level();
        BlockPos currentPos = foreman.blockPosition();

        // Start from current Y and go down
        int targetY = Math.min(currentPos.getY() - 10, 25);

        // Find underground cave or create new chamber
        for (int y = targetY; y >= 10; y--) {
            BlockPos checkPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());

            // Check if area is clear
            if (isAreaClear(checkPos, 11, 9, 11)) {
                return checkPos;
            }
        }

        // If no clear area, return best location for excavation
        return new BlockPos(currentPos.getX(), 25, currentPos.getZ());
    }

    /**
     * Checks if area is clear for farm construction
     */
    private boolean isAreaClear(BlockPos center, int width, int height, int depth) {
        Level level = foreman.level();

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-width/2, 0, -depth/2),
            center.offset(width/2, height-1, depth/2)
        )) {
            // Air or water is acceptable
            if (!level.getBlockState(pos).isAir() &&
                !level.getFluidState(pos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates that location meets all spawning requirements
     */
    public boolean validateLocation(BlockPos location) {
        // Must be below Y=30
        if (location.getY() >= 30) {
            return false;
        }

        // Check for stone blocks below (base_stone_overworld tag requirement)
        Level level = foreman.level();
        boolean hasStoneBelow = false;

        for (int y = 1; y <= 5; y++) {
            BlockPos below = location.below(y);
            if (level.getBlockState(below).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)) {
                hasStoneBelow = true;
                break;
            }
        }

        return hasStoneBelow;
    }
}
```

### 2. Chamber Construction

```java
package com.minewright.squid;

import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates glow squid farm structures
 */
public class SquidFarmGenerator {

    /**
     * Generates a complete glow squid farm
     */
    public static List<BlockPlacement> generateGlowSquidFarm(BlockPos center, int layers) {
        List<BlockPlacement> placements = new ArrayList<>();

        int width = 11;
        int depth = 11;
        int layerHeight = 2; // Water + walking space

        // Build each spawn layer
        for (int layer = 0; layer < layers; layer++) {
            int layerY = center.getY() + (layer * layerHeight);
            buildSpawnLayer(placements, center, layerY, width, depth);
        }

        // Build collection system
        int collectionY = center.getY() - 2;
        buildCollectionSystem(placements, center, collectionY, width, depth);

        // Build access tunnel
        buildAccessTunnel(placements, center, center.getY() + layers * layerHeight + 1);

        return placements;
    }

    /**
     * Builds a single spawn layer
     */
    private static void buildSpawnLayer(List<BlockPlacement> placements,
                                       BlockPos center, int y,
                                       int width, int depth) {
        // Stone ceiling (light-tight)
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -depth/2; z <= depth/2; z++) {
                placements.add(new BlockPlacement(
                    center.offset(x, y + 1, z),
                    Blocks.STONE
                ));
            }
        }

        // Stone walls (light-tight)
        for (int x = -width/2; x <= width/2; x++) {
            for (int dy = 0; dy <= 1; dy++) {
                placements.add(new BlockPlacement(
                    center.offset(x, y + dy, -depth/2),
                    Blocks.STONE
                ));
                placements.add(new BlockPlacement(
                    center.offset(x, y + dy, depth/2),
                    Blocks.STONE
                ));
            }
        }

        for (int z = -depth/2; z <= depth/2; z++) {
            for (int dy = 0; dy <= 1; dy++) {
                placements.add(new BlockPlacement(
                    center.offset(-width/2, y + dy, z),
                    Blocks.STONE
                ));
                placements.add(new BlockPlacement(
                    center.offset(width/2, y + dy, z),
                    Blocks.STONE
                ));
            }
        }

        // Stone/deepslate floor (base_stone_overworld requirement)
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -depth/2; z <= depth/2; z++) {
                placements.add(new BlockPlacement(
                    center.offset(x, y - 1, z),
                    Blocks.DEEPSLATE
                ));
            }
        }

        // Water source blocks (fill the layer)
        for (int x = -width/2 + 1; x < width/2; x++) {
            for (int z = -depth/2 + 1; z < depth/2; z++) {
                placements.add(new BlockPlacement(
                    center.offset(x, y, z),
                    Blocks.WATER
                ));
            }
        }
    }

    /**
     * Builds collection system with hoppers
     */
    private static void buildCollectionSystem(List<BlockPlacement> placements,
                                            BlockPos center, int y,
                                            int width, int depth) {
        // Water funnel pushing to center
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -depth/2; z <= depth/2; z++) {
                // Create funnel toward center
                if (Math.abs(x) > 2 || Math.abs(z) > 2) {
                    // Directional water flowing to center
                    placements.add(new BlockPlacement(
                        center.offset(x, y, z),
                        Blocks.WATER
                    ));
                }
            }
        }

        // Hopper line at center
        for (int x = -2; x <= 2; x++) {
            placements.add(new BlockPlacement(
                center.offset(x, y, 0),
                Blocks.HOPPER
            ));
        }

        // Chests below hoppers
        placements.add(new BlockPlacement(
            center.offset(0, y - 1, 0),
            Blocks.CHEST
        ));
    }

    /**
     * Builds access tunnel for agent entry
     */
    private static void buildAccessTunnel(List<BlockPlacement> placements,
                                         BlockPos center, int tunnelY) {
        // Horizontal tunnel to surface
        for (int x = 0; x < 10; x++) {
            // Clear tunnel
            placements.add(new BlockPlacement(
                center.offset(x, tunnelY, 0),
                Blocks.AIR
            ));
            placements.add(new BlockPlacement(
                center.offset(x, tunnelY + 1, 0),
                Blocks.AIR
            ));
        }

        // Stone stairs
        for (int x = 0; x < 10; x++) {
            placements.add(new BlockPlacement(
                center.offset(x, tunnelY - 1, 0),
                Blocks.STONE_STAIRS
            ));
        }
    }
}
```

---

## Glow Ink Sac Collection

### Collection System Design

```java
package com.minewright.squid;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.GlowSquid;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Action for collecting glow ink sacs from glow squids
 */
public class CollectGlowInkAction extends BaseAction {

    private BlockPos farmCenter;
    private int collectionRadius;
    private int inkSacsCollected = 0;
    private int ticksOperating = 0;
    private static final int OPERATION_DURATION = 1200; // 1 minute

    public CollectGlowInkAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        farmCenter = new BlockPos(
            task.getIntParameter("farmX", foreman.blockPosition().getX()),
            task.getIntParameter("farmY", foreman.blockPosition().getY()),
            task.getIntParameter("farmZ", foreman.blockPosition().getZ())
        );
        collectionRadius = task.getIntParameter("radius", 16);

        foreman.sendChatMessage("Collecting glow ink sacs at farm");
    }

    @Override
    protected void onTick() {
        ticksOperating++;

        if (ticksOperating >= OPERATION_DURATION) {
            result = ActionResult.success(
                String.format("Collected %d glow ink sacs", inkSacsCollected)
            );
            return;
        }

        // Find glow squids in farm area
        AABB searchArea = new AABB(farmCenter).inflate(collectionRadius);
        List<GlowSquid> squids = foreman.level().getEntitiesOfClass(
            GlowSquid.class,
            searchArea
        );

        // Kill glow squids for ink sacs
        for (GlowSquid squid : squids) {
            if (foreman.distanceTo(squid) <= 5.0) {
                // Attack squid
                foreman.doHurtTarget(squid);
                foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

                if (!squid.isAlive()) {
                    inkSacsCollected += 1 + foreman.getRandom().nextInt(3); // 1-3 sacs
                }

                break; // One attack per tick
            }
        }

        // Move towards nearest squid if any exist
        if (!squids.isEmpty()) {
            GlowSquid nearest = squids.stream()
                .min((a, b) -> Double.compare(
                    foreman.distanceTo(a),
                    foreman.distanceTo(b)
                ))
                .orElse(null);

            if (nearest != null && foreman.distanceTo(nearest) > 5.0) {
                foreman.getNavigation().moveTo(nearest, 1.2);
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Collecting glow ink sacs";
    }
}
```

### Automated Hopper Collection

```java
package com.minewright.squid;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Manages glow ink sac collection from hoppers
 */
public class SquidCollectionManager {

    private final ForemanEntity foreman;
    private final BlockPos farmCenter;

    public SquidCollectionManager(ForemanEntity foreman, BlockPos farmCenter) {
        this.foreman = foreman;
        this.farmCenter = farmCenter;
    }

    /**
     * Checks collection chests and retrieves glow ink sacs
     */
    public int collectGlowInkSacs() {
        BlockPos chestPos = farmCenter.below(2); // Chest below farm

        if (!foreman.level().getBlockState(chestPos).is(Blocks.CHEST)) {
            return 0;
        }

        // Check chest contents
        if (foreman.level().getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            int collected = 0;

            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);

                if (stack.getItem() == Items.GLOW_INK_SAC) {
                    collected += stack.getCount();
                    chest.setItem(i, ItemStack.EMPTY);
                }
            }

            if (collected > 0) {
                foreman.sendChatMessage(String.format(
                    "Collected %d glow ink sacs from chest",
                    collected
                ));
            }

            return collected;
        }

        return 0;
    }

    /**
     * Estimates collection rate (sacs per minute)
     */
    public double estimateCollectionRate() {
        // Based on spawn rate and kill efficiency
        // Typical spawn rate: ~2-4 squids per 30 seconds in good conditions
        // Each drops 1-3 sacs
        // Average: 3 squids * 2 sacs = 6 sacs per 30 seconds = 12 sacs/min
        return 12.0;
    }
}
```

---

## Underwater Navigation

### Underwater Pathfinding

```java
package com.minewright.squid;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

/**
 * Specialized navigator for underwater glow squid farm operations
 */
public class UnderwaterSquidNavigator {

    private final ForemanEntity foreman;
    private static final int UNDERWATER_SPEED_PENALTY = 2; // 2x slower underwater

    public UnderwaterSquidNavigator(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Plans a path to a location, handling underwater movement
     */
    public UnderwaterPath planPath(BlockPos start, BlockPos target) {
        List<BlockPos> waypoints = new ArrayList<>();
        List<BlockPos> airPockets = new ArrayList<>();
        boolean requiresDiving = isUnderwater(target);

        // Check if we need to dive underwater
        if (requiresDiving) {
            // Find path with air pocket stops
            waypoints = calculateUnderwaterPath(start, target);
            airPockets = findAirPockets(waypoints);
        } else {
            // Direct path
            waypoints.add(target);
        }

        return new UnderwaterPath(
            waypoints,
            airPockets,
            requiresDiving,
            estimateTravelTime(waypoints)
        );
    }

    /**
     * Calculates underwater path using modified A*
     */
    private List<BlockPos> calculateUnderwaterPath(BlockPos start, BlockPos target) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;

        while (!current.equals(target)) {
            int dx = Integer.signum(target.getX() - current.getX());
            int dy = Integer.signum(target.getY() - current.getY());
            int dz = Integer.signum(target.getZ() - current.getZ());

            // Prioritize horizontal movement, then vertical
            BlockPos next;

            if (dx != 0 || dz != 0) {
                next = current.offset(dx, 0, dz);
                if (canTraverse(next)) {
                    path.add(next);
                    current = next;
                    continue;
                }
            }

            if (dy != 0) {
                next = current.offset(0, dy, 0);
                if (canTraverse(next)) {
                    path.add(next);
                    current = next;
                    continue;
                }
            }

            // Try diagonal if direct path blocked
            next = current.offset(dx != 0 ? dx : 0, dy != 0 ? dy : 0, dz != 0 ? dz : 0);
            if (canTraverse(next)) {
                path.add(next);
                current = next;
            } else {
                break; // Path blocked
            }
        }

        return path;
    }

    /**
     * Finds air pockets along path for breathing
     */
    private List<BlockPos> findAirPockets(List<BlockPos> path) {
        List<BlockPos> airPockets = new ArrayList<>();
        BlockPos lastAirPocket = null;

        for (BlockPos pos : path) {
            // Check if we're far from last air pocket
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
     * Checks if there's air above position
     */
    private boolean hasAirAbove(BlockPos pos) {
        Level level = foreman.level();

        for (int y = 1; y <= 3; y++) {
            BlockPos above = pos.above(y);
            if (level.getBlockState(above).isAir()) {
                return true;
            }
            if (level.getBlockState(above).isSolidRender(level, above)) {
                return false; // Blocked
            }
        }

        return false;
    }

    /**
     * Checks if position is underwater
     */
    private boolean isUnderwater(BlockPos pos) {
        return foreman.level().getFluidState(pos).is(Fluids.WATER);
    }

    /**
     * Checks if entity can traverse position
     */
    private boolean canTraverse(BlockPos pos) {
        Level level = foreman.level();
        return !level.getBlockState(pos).isSolidRender(level, pos) ||
               level.getFluidState(pos).is(Fluids.WATER);
    }

    /**
     * Estimates travel time in ticks
     */
    private int estimateTravelTime(List<BlockPos> path) {
        int distance = path.size();
        return distance * 10 * UNDERWATER_SPEED_PENALTY; // 10 ticks per block, 2x slower
    }
}

/**
 * Represents a planned underwater path
 */
record UnderwaterPath(
    List<BlockPos> waypoints,
    List<BlockPos> airPocketStops,
    boolean requiresDiving,
    int estimatedTicks
) {}
```

### Breathing Management

```java
package com.minewright.squid;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;

/**
 * Manages breathing for underwater operations
 */
public class BreathingManager {

    private final ForemanEntity foreman;
    private static final int AIR_WARNING_THRESHOLD = 60; // 3 seconds of air
    private static final int AIR_CRITICAL_THRESHOLD = 30; // 1.5 seconds of air

    public BreathingManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Checks if foreman needs to surface for air
     */
    public boolean needsAir() {
        int currentAir = foreman.getAirSupply();
        return currentAir < AIR_WARNING_THRESHOLD;
    }

    /**
     * Checks if air is critical (immediate action needed)
     */
    public boolean isAirCritical() {
        int currentAir = foreman.getAirSupply();
        return currentAir < AIR_CRITICAL_THRESHOLD;
    }

    /**
     * Finds nearest air pocket
     */
    public BlockPos findNearestAirPocket(BlockPos center) {
        // Search in expanding sphere
        for (int radius = 1; radius <= 16; radius++) {
            for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)
            )) {
                if (hasAir(pos) && pos.distSqr(center) <= radius * radius) {
                    return pos;
                }
            }
        }

        return null;
    }

    /**
     * Checks if position has breathable air
     */
    private boolean hasAir(BlockPos pos) {
        return foreman.level().getBlockState(pos).isAir();
    }

    /**
     * Navigates to nearest air pocket
     */
    public void goToAirPocket(BlockPos currentPos) {
        BlockPos airPocket = findNearestAirPocket(currentPos);

        if (airPocket != null) {
            foreman.getNavigation().moveTo(
                airPocket.getX(),
                airPocket.getY(),
                airPocket.getZ(),
                1.5 // Urgent speed
            );
        }
    }

    /**
     * Monitors breathing and alerts if needed
     */
    public void monitorBreathing() {
        int currentAir = foreman.getAirSupply();
        int maxAir = foreman.getMaxAirSupply();
        double airPercent = (double) currentAir / maxAir;

        if (airPercent < 0.3) {
            foreman.sendChatMessage(String.format(
                "Air supply at %.0f%% - seeking surface",
                airPercent * 100
            ));
        }
    }
}
```

---

## Light Level Management

### Light Level Verification

```java
package com.minewright.squid;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * Manages light levels for glow squid spawning
 */
public class LightLevelManager {

    private final ForemanEntity foreman;

    public LightLevelManager(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Checks if position is completely dark (light level 0)
     */
    public boolean isCompletelyDark(BlockPos pos) {
        Level level = foreman.level();
        int lightLevel = level.getLightEmission(pos);

        // Check both block and sky light
        int blockLight = level.getBrightness(LightEngine.Layer.BLOCK, pos);
        int skyLight = level.getBrightness(LightEngine.Layer.SKY, pos);

        return lightLevel == 0 && blockLight == 0 && skyLight == 0;
    }

    /**
     * Scans area for light leaks
     */
    public List<BlockPos> findLightLeaks(BlockPos center, int radius) {
        List<BlockPos> leaks = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius)
        )) {
            if (!isCompletelyDark(pos)) {
                leaks.add(pos);
            }
        }

        return leaks;
    }

    /**
     * Verifies spawn area meets light level requirements
     */
    public boolean verifySpawnArea(BlockPos center, int width, int height, int depth) {
        // Check all blocks in spawn area
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-width/2, 0, -depth/2),
            center.offset(width/2, height-1, depth/2)
        )) {
            if (!isCompletelyDark(pos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets light level report for area
     */
    public String getLightLevelReport(BlockPos center, int radius) {
        List<BlockPos> leaks = findLightLeaks(center, radius);

        if (leaks.isEmpty()) {
            return "Spawn area is completely dark - perfect for glow squids!";
        }

        return String.format(
            "Found %d light leak(s) in spawn area. Glow squids will not spawn.",
            leaks.size()
        );
    }
}
```

### Light-Tight Construction

```java
package com.minewright.squid;

import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates light-tight structures for glow squid farms
 */
public class LightTightBuilder {

    /**
     * Creates a light-tight chamber
     */
    public static List<BlockPlacement> buildLightTightChamber(BlockPos center,
                                                             int width,
                                                             int height,
                                                             int depth) {
        List<BlockPlacement> placements = new ArrayList<>();

        // Build solid stone enclosure (no gaps for light)
        for (int x = -width/2 - 1; x <= width/2 + 1; x++) {
            for (int y = -1; y <= height; y++) {
                for (int z = -depth/2 - 1; z <= depth/2 + 1; z++) {
                    // Only place on perimeter
                    if (x == -width/2 - 1 || x == width/2 + 1 ||
                        y == -1 || y == height ||
                        z == -depth/2 - 1 || z == depth/2 + 1) {

                        placements.add(new BlockPlacement(
                            center.offset(x, y, z),
                            Blocks.STONE // Stone blocks all light
                        ));
                    }
                }
            }
        }

        return placements;
    }

    /**
     * Seals any light leaks in existing structure
     */
    public static List<BlockPlacement> sealLightLeaks(List<BlockPos> leaks) {
        List<BlockPlacement> seals = new ArrayList<>();

        for (BlockPos leak : leaks) {
            seals.add(new BlockPlacement(leak, Blocks.STONE));
        }

        return seals;
    }
}
```

---

## Code Examples

### Example 1: Build Glow Squid Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.action.actions.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.squid.*;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Action for building a glow squid farm
 */
public class BuildGlowSquidFarmAction extends BaseAction {

    private BlockPos farmLocation;
    private int numLayers;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex = 0;
    private int ticksBuilding = 0;
    private static final int MAX_BUILD_TIME = 6000; // 5 minutes

    public BuildGlowSquidFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Find optimal location
        SquidFarmLocationFinder finder = new SquidFarmLocationFinder(foreman);
        farmLocation = finder.findOptimalLocation();

        // Validate location
        if (!finder.validateLocation(farmLocation)) {
            result = ActionResult.failure(
                "Could not find valid location below Y=30"
            );
            return;
        }

        numLayers = task.getIntParameter("layers", 2);

        // Generate build plan
        buildPlan = SquidFarmGenerator.generateGlowSquidFarm(
            farmLocation,
            numLayers
        );

        foreman.sendChatMessage(String.format(
            "Building glow squid farm at Y=%d with %d layers (%d blocks)",
            farmLocation.getY(),
            numLayers,
            buildPlan.size()
        ));
    }

    @Override
    protected void onTick() {
        ticksBuilding++;

        if (ticksBuilding > MAX_BUILD_TIME) {
            result = ActionResult.failure("Farm construction timeout");
            return;
        }

        if (currentBlockIndex >= buildPlan.size()) {
            // Verify light levels
            LightLevelManager lightManager = new LightLevelManager(foreman);
            if (lightManager.verifySpawnArea(farmLocation, 11, numLayers * 2, 11)) {
                result = ActionResult.success(String.format(
                    "Glow squid farm built at Y=%d - verified completely dark",
                    farmLocation.getY()
                ));
            } else {
                result = ActionResult.success(String.format(
                    "Glow squid farm built at Y=%d - WARNING: Light leaks detected!",
                    farmLocation.getY()
                ));
            }
            return;
        }

        // Place blocks (5 per tick for efficiency)
        int blocksThisTick = 0;
        while (currentBlockIndex < buildPlan.size() && blocksThisTick < 5) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);

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

            // Place block
            foreman.level().setBlock(
                placement.pos(),
                placement.block().defaultBlockState(),
                3
            );

            currentBlockIndex++;
            blocksThisTick++;
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Building glow squid farm at Y=%d",
            farmLocation != null ? farmLocation.getY() : 0);
    }
}
```

### Example 2: Operate Glow Squid Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.squid.BreathingManager;
import com.minewright.squid.SquidCollectionManager;
import com.minewright.squid.UnderwaterSquidNavigator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.GlowSquid;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Action for operating a glow squid farm
 */
public class OperateGlowSquidFarmAction extends BaseAction {

    private BlockPos farmCenter;
    private BreathingManager breathingManager;
    private SquidCollectionManager collectionManager;
    private UnderwaterSquidNavigator navigator;

    private int inkSacsCollected = 0;
    private int squidsKilled = 0;
    private int ticksOperating = 0;
    private static final int OPERATION_DURATION = 6000; // 5 minutes

    private boolean atCollectionPoint = false;

    public OperateGlowSquidFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        farmCenter = new BlockPos(
            task.getIntParameter("farmX", foreman.blockPosition().getX()),
            task.getIntParameter("farmY", foreman.blockPosition().getY()),
            task.getIntParameter("farmZ", foreman.blockPosition().getZ())
        );

        breathingManager = new BreathingManager(foreman);
        collectionManager = new SquidCollectionManager(foreman, farmCenter);
        navigator = new UnderwaterSquidNavigator(foreman);

        foreman.sendChatMessage("Operating glow squid farm");
    }

    @Override
    protected void onTick() {
        ticksOperating++;

        // Check operation duration
        if (ticksOperating >= OPERATION_DURATION) {
            result = ActionResult.success(String.format(
                "Farm operation complete: %d glow squids killed, %d ink sacs collected",
                squidsKilled,
                inkSacsCollected
            ));
            return;
        }

        // Monitor breathing
        breathingManager.monitorBreathing();
        if (breathingManager.isAirCritical()) {
            breathingManager.goToAirPocket(foreman.blockPosition());
            return;
        }

        // Navigate to farm if not there
        if (!foreman.blockPosition().closerThan(farmCenter, 16.0)) {
            foreman.getNavigation().moveTo(
                farmCenter.getX(),
                farmCenter.getY(),
                farmCenter.getZ(),
                1.0
            );
            return;
        }

        // Kill glow squids in farm
        killGlowSquids();

        // Collect from chests periodically
        if (ticksOperating % 200 == 0) { // Every 10 seconds
            int collected = collectionManager.collectGlowInkSacs();
            inkSacsCollected += collected;
        }
    }

    private void killGlowSquids() {
        AABB farmArea = new AABB(farmCenter).inflate(8.0);
        List<GlowSquid> squids = foreman.level().getEntitiesOfClass(
            GlowSquid.class,
            farmArea
        );

        for (GlowSquid squid : squids) {
            double distance = foreman.distanceTo(squid);

            if (distance <= 4.0) {
                // Attack squid
                foreman.doHurtTarget(squid);
                foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

                if (!squid.isAlive()) {
                    squidsKilled++;
                }

                break; // One attack per tick
            } else {
                // Move towards squid
                foreman.getNavigation().moveTo(squid, 1.2);
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Operating glow squid farm";
    }
}
```

### Example 3: Verify Glow Squid Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.squid.LightLevelManager;
import com.minewright.squid.SquidFarmLocationFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

/**
 * Action for verifying glow squid farm setup
 */
public class VerifyGlowSquidFarmAction extends BaseAction {

    private BlockPos farmCenter;

    public VerifyGlowSquidFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        farmCenter = new BlockPos(
            task.getIntParameter("farmX", foreman.blockPosition().getX()),
            task.getIntParameter("farmY", foreman.blockPosition().getY()),
            task.getIntParameter("farmZ", foreman.blockPosition().getZ())
        );
    }

    @Override
    protected void onTick() {
        Level level = foreman.level();
        StringBuilder report = new StringBuilder();
        report.append("=== Glow Squid Farm Verification ===\n");

        // 1. Check Y-level
        boolean correctY = farmCenter.getY() < 30;
        report.append(String.format("Y-Level: %d (%s)\n",
            farmCenter.getY(),
            correctY ? "✓ Below 30" : "✗ MUST be below 30"
        ));

        // 2. Check light levels
        LightLevelManager lightManager = new LightLevelManager(foreman);
        boolean isDark = lightManager.verifySpawnArea(farmCenter, 11, 2, 11);
        report.append(String.format("Light Level: %s\n",
            isDark ? "✓ Completely dark" : "✗ Light leaks detected"
        ));

        // 3. Check stone below
        boolean hasStone = false;
        for (int y = 1; y <= 5; y++) {
            if (level.getBlockState(farmCenter.below(y))
                .is(BlockTags.BASE_STONE_OVERWORLD)) {
                hasStone = true;
                break;
            }
        }
        report.append(String.format("Stone Base: %s\n",
            hasStone ? "✓ Found within 5 blocks" : "✗ Not found"
        ));

        // 4. Check water sources
        int waterCount = 0;
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (level.getFluidState(farmCenter.offset(x, 0, z))
                    .is(net.minecraft.world.level.material.Fluids.WATER)) {
                    waterCount++;
                }
            }
        }
        report.append(String.format("Water Blocks: %d\n", waterCount));

        // 5. Overall verdict
        boolean allGood = correctY && isDark && hasStone && waterCount > 50;
        report.append("\n").append(allGood ? "✓ FARM READY" : "✗ ISSUES FOUND");

        foreman.sendChatMessage(report.toString());

        result = allGood ?
            ActionResult.success("Farm verified - glow squids will spawn!") :
            ActionResult.failure("Farm has issues - fix reported problems");
    }

    @Override
    protected void onCancel() {
        // Nothing to cancel
    }

    @Override
    public String getDescription() {
        return "Verifying glow squid farm setup";
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Basic glow squid farm building and verification

- [ ] Create `SquidFarmLocationFinder` for location selection
- [ ] Implement `SquidFarmGenerator` for structure generation
- [ ] Add `BuildGlowSquidFarmAction` for farm construction
- [ ] Implement `LightLevelManager` for darkness verification
- [ ] Add `VerifyGlowSquidFarmAction` for farm validation

**Deliverables:**
- Agents can build basic glow squid farm
- Light level verification system
- Location validation

### Phase 2: Collection System (Week 3)

**Goal:** Automated glow ink sac collection

- [ ] Implement `CollectGlowInkAction` for manual collection
- [ ] Add `SquidCollectionManager` for hopper/chest management
- [ ] Create water flow collection system
- [ ] Add inventory management for glow ink sacs

**Deliverables:**
- Automatic ink sac collection
- Hopper-based collection system
- Inventory tracking

### Phase 3: Underwater Navigation (Week 4)

**Goal:** Specialized underwater pathfinding

- [ ] Implement `UnderwaterSquidNavigator` for 3D pathfinding
- [ ] Add `BreathingManager` for air supply management
- [ ] Create air pocket detection system
- [ ] Add underwater movement optimization

**Deliverables:**
- Safe underwater navigation
- Breathing management
- Air pocket finding

### Phase 4: Farm Operation (Week 5)

**Goal:** Automated farm operation

- [ ] Implement `OperateGlowSquidFarmAction`
- [ ] Add spawn rate monitoring
- [ ] Create multi-agent coordination
- [ ] Add efficiency metrics

**Deliverables:**
- Fully automated operation
- Spawn rate tracking
- Multi-agent support

### Phase 5: Optimization (Week 6)

**Goal:** Maximum spawn rates

- [ ] Optimize spawn platform design
- [ ] Add multi-layer support
- [ ] Implement AFK positioning
- [ ] Add spawn cap management

**Deliverables:**
- Optimized spawn rates
- Multi-layer farms
- Performance metrics

### Phase 6: Integration (Week 7)

**Goal:** Full system integration

- [ ] Register `GlowSquidFarmPlugin`
- [ ] Update `PromptBuilder` with squid actions
- [ ] Add configuration options
- [ ] Create comprehensive tests

**Deliverables:**
- Plugin registration
- LLM integration
- Config system
- Test suite

### Phase 7: Polish (Week 8)

**Goal:** Production-ready system

- [ ] Performance optimization
- [ ] Documentation completion
- [ ] UI/UX improvements
- [ ] Bug fixes and refinement

**Deliverables:**
- Stable, production-ready system
- Complete documentation
- User-friendly interface

---

## Integration Guide

### 1. Register Glow Squid Farm Actions

Add to `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Glow squid farm actions
    registry.register("build_glow_squid_farm",
        (foreman, task, ctx) -> new BuildGlowSquidFarmAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("operate_glow_squid_farm",
        (foreman, task, ctx) -> new OperateGlowSquidFarmAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("collect_glow_ink",
        (foreman, task, ctx) -> new CollectGlowInkAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("verify_glow_squid_farm",
        (foreman, task, ctx) -> new VerifyGlowSquidFarmAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### 2. Update PromptBuilder

Add glow squid farm actions to system prompt:

```java
String glowSquidActions = """
ACTIONS:
- build_glow_squid_farm: {"layers": 2} - Builds glow squid farm below Y=30
- operate_glow_squid_farm: {"farmX": 0, "farmY": 25, "farmZ": 0} - Operates farm
- collect_glow_ink: {} - Collects glow ink sacs from squids
- verify_glow_squid_farm: {"farmX": 0, "farmY": 25, "farmZ": 0} - Verifies farm setup

GLOW SQUID REQUIREMENTS:
- Y-Level: Must be below Y=30
- Light: Complete darkness (light level 0)
- Water: Water source blocks
- Stone Base: Stone/deepslate within 5 blocks below

EXAMPLES:
Input: "build a glow squid farm"
Output: {"action": "build_glow_squid_farm", "parameters": {"layers": 2}}

Input: "collect glow ink sacs"
Output: {"action": "collect_glow_ink", "parameters": {}}

Input: "verify my glow squid farm"
Output: {"action": "verify_glow_squid_farm", "parameters": {"farmX": 100, "farmY": 25, "farmZ": 100}}
""";
```

### 3. Configuration

Add to `config/steve-common.toml`:

```toml
[glow_squid_farm]
# Farm settings
default_layers = 2
default_width = 11
default_depth = 11
layer_spacing = 2

# Spawn requirements
min_y_level = 30  # Must be below this
required_light_level = 0

# Collection settings
collection_interval_ticks = 200  # 10 seconds
auto_collect = true

# Operation settings
operation_duration_ticks = 6000  # 5 minutes
kill_radius = 8.0

# Navigation
underwater_speed_penalty = 2
air_warning_threshold = 60  # ticks
air_critical_threshold = 30  # ticks
```

### 4. Testing

```java
@Test
public void testGlowSquidFarmBuilding() {
    ForemanEntity foreman = createTestForeman();
    Task task = new Task("build_glow_squid_farm", Map.of("layers", 2));

    BuildGlowSquidFarmAction action = new BuildGlowSquidFarmAction(foreman, task);
    action.start();

    while (!action.isComplete()) {
        action.tick();
    }

    ActionResult result = action.getResult();
    assertTrue(result.isSuccess());
    assertTrue(foreman.blockPosition().getY() < 30);
}

@Test
public void testLightLevelVerification() {
    ForemanEntity foreman = createTestForeman();
    LightLevelManager manager = new LightLevelManager(foreman);

    // Build test chamber
    BlockPos testCenter = new BlockPos(0, 25, 0);
    buildDarkChamber(foreman.level(), testCenter);

    assertTrue(manager.verifySpawnArea(testCenter, 11, 2, 11));
}

@Test
public void testUnderwaterNavigation() {
    ForemanEntity foreman = createTestForeman();
    UnderwaterSquidNavigator navigator = new UnderwaterSquidNavigator(foreman);

    BlockPos start = new BlockPos(0, 60, 0);
    BlockPos target = new BlockPos(10, 25, 10); // Underwater

    UnderwaterPath path = navigator.planPath(start, target);

    assertTrue(path.requiresDiving());
    assertFalse(path.waypoints().isEmpty());
}
```

---

## Conclusion

The Glow Squid Farming AI system provides comprehensive automation for glow squid farming in MineWright. Key features include:

### Benefits

1. **Automated Construction**: Agents build complete farms below Y=30
2. **Light Management**: Ensures complete darkness for spawning
3. **Underwater Navigation**: Safe pathfinding with breathing management
4. **Automated Collection**: Hopper-based glow ink sac collection
5. **Multi-Agent Coordination**: Multiple agents can operate together

### Expected Performance

- **Spawn Rate**: 6-12 glow squids per minute (2-layer farm)
- **Collection Rate**: 12-24 glow ink sacs per minute
- **Construction Time**: ~5 minutes for 2-layer farm
- **Automation**: Fully autonomous after construction

### Future Enhancements

- Axolotl-assisted farming (axolotls hunt glow squids)
- Multi-biome farm designs
- Advanced spawn optimization
- Glow ink sac processing (sign creation, etc.)
- Underwater base integration

---

**Document End**

Sources:
- [Glow Squid - Minecraft Wiki](https://minecraft.fandom.com/wiki/Glow_Squid)
- [Tutorials/Squid farming - Minecraft Wiki](https://minecraft.fandom.com/wiki/Tutorials/Squid_farming)
- [Tutorials/Axolotl farming - Minecraft Wiki](https://minecraft.fandom.com/wiki/Tutorials/Axolotl_farming)
