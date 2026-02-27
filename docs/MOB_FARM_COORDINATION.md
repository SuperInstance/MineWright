# Mob Farm Coordination Design

**Author:** MineWright Development Team
**Version:** 1.0.0
**Date:** 2025-02-27
**Status:** Design Document

## Executive Summary

This document outlines the design and implementation roadmap for multi-agent mob farm coordination in MineWright. The system enables autonomous Foreman agents to operate, optimize, and manage hostile mob farms with minimal human intervention, leveraging Minecraft's spawning mechanics and the existing orchestration architecture.

### Key Features
- Automated spawn platform monitoring and optimization
- Intelligent collection system management with hopper minecart networks
- AFK spot positioning for maximum spawn rates
- Multi-agent role specialization (killers, collectors, monitors)
- Item collection automation with sorting and storage
- Real-time spawn rate analytics and optimization

---

## Table of Contents

1. [Farm Design Patterns](#farm-design-patterns)
2. [Collection System Design](#collection-system-design)
3. [AFK Spot Positioning](#afk-spot-positioning)
4. [Multi-Agent Roles](#multi-agent-roles)
5. [Spawn Platform Optimization](#spawn-platform-optimization)
6. [Item Collection Automation](#item-collection-automation)
7. [Integration with Existing Systems](#integration-with-existing-systems)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Configuration and Tuning](#configuration-and-tuning)

---

## Farm Design Patterns

### 1. Traditional Tower Farm

```
┌─────────────────────────────────┐
│     AFK Platform (Y=200)         │ ← Foreman positions here
├─────────────────────────────────┤
│     Spawn Platforms (Y=150-180)  │ ← 2-3 levels
├──────────────────────────────────┤
│     Dropper Funnel               │ ← Water streams push mobs
├─────────────────────────────────┤
│     Killing Chamber (Y=128)      │ ← Fall damage + finish
├─────────────────────────────────┤
│     Collection System (Y=120)    │ ← Hoppers + Minecarts
└─────────────────────────────────┘
```

**Specifications:**
- Spawn platforms: 21x21 or 21x21 x 4 layers
- Distance requirement: 24-128 blocks from AFK spot
- Dark rooms: Light level 0
- Water streams: Every 4 blocks to push mobs to center hole

### 2. Spider/Cave Spider Farm

```
┌─────────────────────────────────┐
│     Spawner Room (3x3x3)         │ ← Around monster spawner
├─────────────────────────────────┤
│     Water Current Channels       │ ← Push spiders to collection
├─────────────────────────────────┤
│     Suffocation Trap/Kill Shaft  │ ← Fall damage mechanism
├─────────────────────────────────┤
│     Item Collection (Hoppers)    │ ← Auto-collect drops
└─────────────────────────────────┘
```

**Specifications:**
- Spawner activation range: 16 blocks
- Optimal Y-level: Spawner's natural Y
- Flow rate: Water source every 8 blocks
- Collection: Hopper minecart under kill chamber

### 3. Nether Portal Farm

```
┌─────────────────────────────────┐
│     Teleportation Trap (AFK)     │ ← Player stands here
├─────────────────────────────────┤
│     Nether Side (Spawn Pad)      │ ← Mobs spawn on 4x4 pad
├─────────────────────────────────┤
│     Overworld Side (Kill Room)   │ ← Mobs teleport here
├─────────────────────────────────┤
│     Collection System            │ ← Hopper under kill chamber
└─────────────────────────────────┘
```

**Specifications:**
- Spawn pad: 4x4 (gold farm) or 21x21 (general)
- Portal alignment: Exact coordinate match
- Kill method: Fall damage (23 blocks for pigmen, 43 for general)

---

## Collection System Design

### Hopper Network Architecture

```java
/**
 * Collection system using hopper minecarts for maximum throughput
 */
public class MobFarmCollectionSystem {

    private final BlockPos collectionPoint;
    private final List<BlockPos> hoppers;
    private final List<BlockPos> chests;

    // Throughput: ~1 item/tick per hopper, ~4 items/tick with minecart
    private static final int MAX_HOPPERS = 16;
    private static final int ITEMS_PER_TICK = 64; // Stack size
}
```

### Optimal Collection Setup

```
Top View (Collection Layer):
┌─────────────────────────────────┐
│  [C][C][C][C][C][C][C][C]      │ ← Double chests (8 total)
│  [C][C][C][C][C][C][C][C]      │
│  [H][H][H][H][H][H][H][H]      │ ← Hopper line (16 hoppers)
│  [H][H][H][H][H][H][H][H]      │
│  ══════════════════════════   │ → Hopper minecart track
│  [M][M][M][M][M][M][M][M]      │ → Minecarts (8 carts)
└─────────────────────────────────┘
```

**Collection Rate Calculation:**
- Single hopper: 8 items/2.5 seconds = 3.2 items/sec
- Hopper minecart: 1 item/tick = 20 items/sec (6x faster!)
- Recommended: 8 hopper minecarts for 160 items/sec throughput

---

## AFK Spot Positioning

### Optimal Distance Calculation

```java
/**
 * Calculates optimal AFK position for mob spawning
 *
 * Spawn Rules (Minecraft 1.20.1):
 * - Horizontal distance: 24-128 blocks from player
 * - Vertical distance: Any Y level (no limit)
 * - Sphere distance: 24-128 blocks (spherical radius)
 */
public class AFKSpotCalculator {

    private static final double MIN_SPAWN_DIST = 24.0;
    private static final double MAX_SPAWN_DIST = 128.0;

    public BlockPos calculateOptimalAFKSpot(BlockPos farmCenter) {
        // Ideal: ~32 blocks away (maximizes spawnable area while keeping close)
        double optimalDistance = 32.0;

        // Position at Y=200 or higher (above spawn platforms)
        int afkY = 200;
        int afkX = farmCenter.getX() + (int)optimalDistance;
        int afkZ = farmCenter.getZ();

        return new BlockPos(afkX, afkY, afkZ);
    }
}
```

### AFK Platform Requirements

```
Minimal AFK Platform (2x2):
┌─────┐
│ ■ ■ │ ← Safe platform, fenced
│ ■ ■ │ ← AFK spot (center)
└─────┘

Recommended AFK Platform (5x5):
┌─────────┐
│ ██████ │ ← Fence walls (prevent mob aggro)
│ ■■■■■■ │ ← Center spot
│ ██████ │ ← Torches (prevent local spawns)
└─────────┘
```

**Key Considerations:**
1. **Light level 7+** at AFK spot (prevent local spawns)
2. **Fenced enclosure** (prevent ranged mob attacks)
3. **24-32 blocks** horizontally from spawn center
4. **Y+200** or above spawn platforms
5. **Chunk loading**: Must remain in spawn chunks or use chunk loader

---

## Multi-Agent Roles

### Role Specialization

```java
/**
 * Mob farm operation roles for multi-agent coordination
 */
public enum FarmRole {

    /**
     * Primary combat agent - kills mobs in kill chamber
     * Position: Inside kill chamber or at AFK spot
     * Equipment: Sword (Looting III), optional knockback stick
     */
    KILLER("Killer", "Combat specialist"),

    /**
     * Collection management - monitors hoppers, sorts items
     * Position: Near collection chests
     * Equipment: None (inventory management only)
     */
    COLLECTOR("Collector", "Item gathering specialist"),

    /**
     * System monitor - tracks spawn rates, efficiency
     * Position: AFK spot (to maintain spawns)
     * Equipment: None (observation only)
     */
    MONITOR("Monitor", "Analytics and optimization"),

    /**
     * Maintenance - repairs, restocks, handles issues
     * Position: Roaming (responds to issues)
     * Equipment: Building materials, replacement parts
     */
    MAINTENANCE("Maintenance", "Repairs and restocking"),

    /**
     * Farm foreman - coordinates all agents
     * Position: Command center or AFK spot
     * Equipment: None (coordination only)
     */
    FOREMAN("Farm Foreman", "Overall coordination");
}
```

### Role Responsibilities

| Role | Primary Tasks | Secondary Tasks | Equipment |
|------|---------------|-----------------|-----------|
| **Killer** | Attack mobs in kill chamber | Looting sweeps, finishing weakened mobs | Looting sword |
| **COLLECTOR** | Empty hoppers to chests | Sort items, manage storage | Empty hands |
| **MONITOR** | Count spawns, track rates | Report efficiency, detect issues | None |
| **MAINTENANCE** | Replace broken blocks | Restock materials, repair lighting | Building blocks |
| **FOREMAN** | Assign tasks, coordinate | Report status, optimize operations | None |

---

## Spawn Platform Optimization

### Platform Design Principles

```java
/**
 * Spawn platform optimization for maximum spawn rates
 *
 * Spawn Mechanics (1.20.1):
 * - 1 spawn attempt per chunk per 10 ticks (0.5 seconds)
 * - Max 70 hostile mobs in 128-block radius
 * - Each 2-block tall mob needs 2x1x1 space
 * - Light level 0 required
 */
public class SpawnPlatformOptimizer {

    // Optimal dimensions for 2-block tall mobs
    private static final int PLATFORM_WIDTH = 21;  // Max efficient width
    private static final int PLATFORM_LENGTH = 21; // Max efficient length
    private static final int LAYER_COUNT = 4;      // 4 layers = max spawns

    /**
     * Optimizes spawn platform layout
     *
     * Strategy:
     * 1. Use 21x21 platforms (spans 2x2 chunks)
     * 2. 4 vertical layers (Y=150, 160, 170, 180)
     * 3. Half-slab floor (prevents spider spawns if not wanted)
     * 4. Water streams every 4 blocks (push mobs to center)
     */
    public List<BlockPlacement> optimizePlatform(BlockPos center) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Build 4 layers
        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            int y = center.getY() + (layer * 10);
            buildLayer(blocks, center, y);
        }

        // Add water streams
        addWaterStreams(blocks, center);

        // Add center drop hole
        addDropHole(blocks, center);

        return blocks;
    }

    private void buildLayer(List<BlockPlacement> blocks, BlockPos center, int y) {
        // 21x21 platform with half-slab floor (prevents spider spawns)
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y, center.getZ() + z),
                    "stone_slab"
                ));
            }
        }

        // Enclose with walls (keep mobs in)
        buildWalls(blocks, center, y);
    }
}
```

### Spawn Rate Calculation

```
Theoretical Maximum Spawn Rate:
- Chunks: 2x2 = 4 chunks
- Spawn attempts: 1 per chunk per 10 ticks = 0.4 attempts/sec per chunk
- Total attempts: 4 chunks × 0.4 = 1.6 attempts/sec
- Per layer: 1.6 attempts/sec
- 4 layers: 1.6 × 4 = 6.4 attempts/sec

Realistic Rate (with mob cap of 70):
- Spawn efficiency: ~80% (mob cap, light level, space constraints)
- Effective spawns: 6.4 × 0.8 = ~5 spawns/sec
- Per minute: 5 × 60 = 300 spawns/min
- Per hour: 300 × 60 = 18,000 spawns/hour
```

---

## Item Collection Automation

### Sorting System Design

```java
/**
 * Automatic item sorting and storage system
 */
public class ItemSorter {

    /**
     * Sorts collected mob drops into categories
     *
     * Categories:
     * - Rotten flesh (compost/fuel)
     * - Bones (bone meal, wolves)
     * - Gunpowder (fireworks, TNT)
     * - String (bows, wool)
     * - Ender pearls (teleportation)
     * - Rare drops (wither skulls, ingots)
     */
    public void sortItems(Collection<ItemStack> drops) {
        Map<String, List<ItemStack>> sorted = new HashMap<>();

        for (ItemStack drop : drops) {
            String category = categorize(drop);
            sorted.computeIfAbsent(category, k -> new ArrayList<>()).add(drop);
        }

        // Distribute to appropriate chests
        for (Map.Entry<String, List<ItemStack>> entry : sorted.entrySet()) {
            depositToChest(entry.getKey(), entry.getValue());
        }
    }

    private String categorize(ItemStack stack) {
        return switch (stack.getItem().toString()) {
            case "rotten_flesh" -> "common_compost";
            case "bone" -> "bone_meal";
            case "gunpowder" -> "explosives";
            case "string" -> "crafting";
            case "ender_pearl" -> "teleportation";
            default -> "rare_drops";
        };
    }
}
```

### Storage Layout

```
Chest Room Layout (Under farm):
┌─────────────────────────────────┐
│  [RF][RF][RF][RF]  (Rotten Flesh)│
│  [B ][B ][B ][B ]  (Bones)      │
│  [GP][GP][GP][GP]  (Gunpowder)  │
│  [S ][S ][S ][S ]  (String)     │
│  [EP][EP]        (Ender Pearls) │
│  [RD][RD][RD]    (Rare Drops)   │
└─────────────────────────────────┘
Legend: RF=Rotten Flesh, B=Bones, GP=Gunpowder, S=String, EP=Ender Pearls, RD=Rare
```

---

## Integration with Existing Systems

### 1. Action Registration

```java
/**
 * Mob farm actions plugin
 * Registers farm-specific actions with the plugin system
 */
public class MobFarmActionsPlugin implements ActionPlugin {

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        int priority = 500; // Lower than core, higher than custom

        // Farm operation actions
        registry.register("operate_farm",
            (foreman, task, ctx) -> new OperateFarmAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("collect_drops",
            (foreman, task, ctx) -> new CollectDropsAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("monitor_spawns",
            (foreman, task, ctx) -> new MonitorSpawnsAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("kill_mobs",
            (foreman, task, ctx) -> new KillMobsAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("optimize_platform",
            (foreman, task, ctx) -> new OptimizePlatformAction(foreman, task),
            priority, PLUGIN_ID);
    }
}
```

### 2. Orchestrator Integration

```java
/**
 * Farm operation coordination through OrchestratorService
 */
public class FarmCoordinator {

    private final OrchestratorService orchestrator;

    /**
     * Coordinates multi-agent farm operation
     */
    public void startFarmOperation(String farmId, Collection<ForemanEntity> agents) {
        // Assign roles
        Map<FarmRole, ForemanEntity> roles = assignRoles(agents);

        // Create operation plan
        List<Task> tasks = createFarmTasks(farmId);

        // Submit to orchestrator
        ResponseParser.ParsedResponse plan = new ResponseParser.ParsedResponse(
            "Operating mob farm: " + farmId,
            tasks
        );

        orchestrator.processHumanCommand(plan, agents);
    }

    private Map<FarmRole, ForemanEntity> assignRoles(Collection<ForemanEntity> agents) {
        Map<FarmRole, ForemanEntity> roles = new EnumMap<>(FarmRole.class);

        int i = 0;
        for (ForemanEntity agent : agents) {
            FarmRole role = FarmRole.values()[i % FarmRole.values().length];
            agent.setRole(AgentRole.WORKER);
            roles.put(role, agent);
            i++;
        }

        return roles;
    }
}
```

### 3. Memory Integration

```java
/**
 * Farm statistics tracking in ForemanMemory
 */
public class FarmStatistics {

    private int totalSpawns = 0;
    private int totalKills = 0;
    private int totalItemsCollected = 0;
    private double averageSpawnRate = 0.0;
    private long lastUpdateTick = 0;

    /**
     * Records spawn event
     */
    public void recordSpawn(EntityType<?> mobType) {
        totalSpawns++;
        updateSpawnRate();
    }

    /**
     * Calculates current spawn rate (spawns per minute)
     */
    private void updateSpawnRate() {
        long currentTick = MinecraftServer.currentTick;
        long ticksSinceUpdate = currentTick - lastUpdateTick;

        if (ticksSinceUpdate > 0) {
            double recentRate = (totalSpawns / (double)ticksSinceUpdate) * 1200; // per minute
            averageSpawnRate = (averageSpawnRate * 0.9) + (recentRate * 0.1); // EMA
        }

        lastUpdateTick = currentTick;
    }

    /**
     * Gets efficiency report
     */
    public String getEfficiencyReport() {
        return String.format(
            "Spawn Rate: %.1f/min | Kills: %d | Items: %d | Efficiency: %.1f%%",
            averageSpawnRate,
            totalKills,
            totalItemsCollected,
            (totalItemsCollected / (double)totalSpawns) * 100
        );
    }
}
```

---

## Code Examples

### Example 1: Operate Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Action for operating a mob farm
 *
 * Parameters:
 * - farmId: Identifier for the farm
 * - role: Agent's role (killer, collector, monitor)
 * - afkPosition: AFK spot coordinates
 */
public class OperateFarmAction extends BaseAction {

    private final String farmId;
    private final String role;
    private final BlockPos afkPosition;
    private final BlockPos killChamber;

    private int ticksAtAFK = 0;
    private int mobsKilled = 0;
    private static final int OPERATION_DURATION = 6000; // 5 minutes

    public OperateFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.farmId = task.getStringParameter("farmId", "default_farm");
        this.role = task.getStringParameter("role", "monitor");
        this.afkPosition = parseBlockPos(task.getStringParameter("afkPosition", "0,200,0"));
        this.killChamber = parseBlockPos(task.getStringParameter("killChamber", "0,128,0"));
    }

    @Override
    protected void onStart() {
        // Navigate to AFK position or kill chamber based on role
        BlockPos target = "killer".equals(role) ? killChamber : afkPosition;

        foreman.getNavigation().moveTo(
            target.getX(), target.getY(), target.getZ(),
            1.5 // Speed
        );

        foreman.setInvulnerableBuilding(true);
    }

    @Override
    protected void onTick() {
        ticksAtAFK++;

        // Check operation duration
        if (ticksAtAFK >= OPERATION_DURATION) {
            result = ActionResult.success(
                String.format("Farm operation complete: %d mobs killed", mobsKilled)
            );
            return;
        }

        // Role-specific behavior
        switch (role) {
            case "killer" -> performKillerDuties();
            case "collector" -> performCollectorDuties();
            case "monitor" -> performMonitorDuties();
        }
    }

    private void performKillerDuties() {
        // Find and attack mobs in kill chamber
        AABB searchBox = new AABB(killChamber).inflate(16.0);
        List<Entity> mobs = foreman.level().getEntities(
            foreman, searchBox, e -> e instanceof Monster
        );

        for (Entity mob : mobs) {
            if (mob instanceof Monster monster && monster.isAlive()) {
                double distance = foreman.distanceTo(mob);

                if (distance <= 3.5) {
                    // Attack mob
                    foreman.doHurtTarget(monster);
                    foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

                    if (!monster.isAlive()) {
                        mobsKilled++;
                    }
                } else {
                    // Move towards mob
                    foreman.getNavigation().moveTo(mob, 2.0);
                }

                break; // Attack one mob per tick
            }
        }
    }

    private void performCollectorDuties() {
        // Check hopper levels and empty if needed
        BlockPos collectionPoint = killChamber.below(8);

        // Scan for nearby hoppers
        AABB searchBox = new AABB(collectionPoint).inflate(8.0);
        List<BlockPos> hoppers = findHoppers(searchBox);

        for (BlockPos hopperPos : hoppers) {
            // Check hopper contents and transfer to storage
            // (Implementation depends on Minecraft API for hopper inspection)
        }
    }

    private void performMonitorDuties() {
        // Count spawns, track efficiency
        if (ticksAtAFK % 100 == 0) { // Every 5 seconds
            AABB farmArea = new AABB(afkPosition).inflate(32.0);
            List<Entity> mobs = foreman.level().getEntities(
                foreman, farmArea, e -> e instanceof Monster
            );

            int mobCount = mobs.size();
            MineWrightMod.LOGGER.info("[Farm Monitor] {} mobs in farm area", mobCount);

            // Report to foreman if spawn rate is low
            if (mobCount < 10) {
                foreman.sendChatMessage(
                    String.format("Low spawn rate detected: %d mobs", mobCount)
                );
            }
        }
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return String.format("Operating farm '%s' as %s", farmId, role);
    }

    private BlockPos parseBlockPos(String str) {
        String[] parts = str.split(",");
        return new BlockPos(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        );
    }

    private List<BlockPos> findHoppers(AABB area) {
        // Find all hopper blocks in area
        // (Implementation depends on block scanning API)
        return List.of();
    }
}
```

### Example 2: Build Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Action for constructing a mob farm
 *
 * Parameters:
 * - farmType: tower, spider, nether
 * - center: Center position of farm
 * - height: Number of spawn layers
 */
public class BuildFarmAction extends BaseAction {

    private final String farmType;
    private final BlockPos center;
    private final int height;

    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex = 0;

    public BuildFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.farmType = task.getStringParameter("farmType", "tower");
        this.center = parseBlockPos(task.getStringParameter("center", "0,150,0"));
        this.height = task.getIntParameter("height", 4);
    }

    @Override
    protected void onStart() {
        // Generate build plan based on farm type
        buildPlan = switch (farmType) {
            case "tower" -> buildTowerFarm();
            case "spider" -> buildSpiderFarm();
            case "nether" -> buildNetherFarm();
            default -> buildTowerFarm();
        };

        foreman.setFlying(true);
    }

    @Override
    protected void onTick() {
        if (currentBlockIndex >= buildPlan.size()) {
            result = ActionResult.success(
                String.format("Built %s farm at %s", farmType, center)
            );
            foreman.setFlying(false);
            return;
        }

        // Place up to 5 blocks per tick for efficiency
        int blocksThisTick = 0;
        while (currentBlockIndex < buildPlan.size() && blocksThisTick < 5) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);
            placeBlock(placement);
            currentBlockIndex++;
            blocksThisTick++;
        }
    }

    private List<BlockPlacement> buildTowerFarm() {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Build spawn layers
        for (int layer = 0; layer < height; layer++) {
            int y = center.getY() + (layer * 10);
            buildSpawnLayer(blocks, y);
        }

        // Build water streams and drop hole
        buildWaterSystem(blocks, center.getY() - 10);

        // Build kill chamber
        buildKillChamber(blocks, center.getY() - 20);

        // Build collection system
        buildCollectionSystem(blocks, center.getY() - 30);

        // Build AFK platform
        buildAFKPlatform(blocks, center.getY() + 50);

        return blocks;
    }

    private void buildSpawnLayer(List<BlockPlacement> blocks, int y) {
        // 21x21 platform
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y, center.getZ() + z),
                    "stone_slab" // Prevents spider spawns
                ));
            }
        }

        // Walls
        for (int x = -10; x <= 10; x++) {
            for (int dy = 1; dy <= 3; dy++) {
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y + dy, center.getZ() - 11),
                    "cobblestone_wall"
                ));
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y + dy, center.getZ() + 11),
                    "cobblestone_wall"
                ));
            }
        }

        for (int z = -10; z <= 10; z++) {
            for (int dy = 1; dy <= 3; dy++) {
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() - 11, y + dy, center.getZ() + z),
                    "cobblestone_wall"
                ));
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + 11, y + dy, center.getZ() + z),
                    "cobblestone_wall"
                ));
            }
        }
    }

    private void buildWaterSystem(List<BlockPlacement> blocks, int y) {
        // Water streams pushing to center
        for (int x = -10; x <= 10; x++) {
            // North-south streams
            if (x % 4 == 0) { // Water every 4 blocks
                for (int z = -10; z <= 10; z++) {
                    blocks.add(new BlockPlacement(
                        new BlockPos(center.getX() + x, y, center.getZ() + z),
                        "water"
                    ));
                }
            }
        }

        // Center drop hole (2x2)
        blocks.add(new BlockPlacement(new BlockPos(center.getX(), y, center.getZ()), "air"));
        blocks.add(new BlockPlacement(new BlockPos(center.getX() + 1, y, center.getZ()), "air"));
        blocks.add(new BlockPlacement(new BlockPos(center.getX(), y, center.getZ() + 1), "air"));
        blocks.add(new BlockPlacement(new BlockPos(center.getX() + 1, y, center.getZ() + 1), "air"));
    }

    private void buildKillChamber(List<BlockPlacement> blocks, int y) {
        // 9x9 kill chamber with 3-block fall
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                // Floor
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + x, y, center.getZ() + z),
                    "stone"
                ));

                // Walls (glass for visibility)
                for (int dy = 1; dy <= 3; dy++) {
                    blocks.add(new BlockPlacement(
                        new BlockPos(center.getX() + x, y + dy, center.getZ() + z),
                        "glass"
                    ));
                }
            }
        }
    }

    private void buildCollectionSystem(List<BlockPlacement> blocks, int y) {
        // Hopper line under kill chamber
        for (int x = -4; x <= 4; x++) {
            blocks.add(new BlockPlacement(
                new BlockPos(center.getX() + x, y, center.getZ()),
                "hopper"
            ));
        }

        // Chests below hoppers
        for (int x = -4; x <= 4; x += 2) {
            blocks.add(new BlockPlacement(
                new BlockPos(center.getX() + x, y - 1, center.getZ()),
                "chest"
            ));
        }
    }

    private void buildAFKPlatform(List<BlockPlacement> blocks, int y) {
        // 5x5 AFK platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                blocks.add(new BlockPlacement(
                    new BlockPos(center.getX() + x + 32, y, center.getZ() + z),
                    "oak_planks"
                ));
            }
        }

        // Fences
        for (int x = -2; x <= 2; x++) {
            blocks.add(new BlockPlacement(
                new BlockPos(center.getX() + x + 32, y, center.getZ() - 3),
                "oak_fence"
            ));
            blocks.add(new BlockPlacement(
                new BlockPos(center.getX() + x + 32, y, center.getZ() + 3),
                "oak_fence"
            ));
        }

        // Torches for light
        blocks.add(new BlockPlacement(
            new BlockPos(center.getX() + 32, y + 1, center.getZ()),
            "torch"
        ));
    }

    private void placeBlock(BlockPlacement placement) {
        // Place block at position (implementation depends on Minecraft API)
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
    }

    @Override
    public String getDescription() {
        return String.format("Building %s farm at %s", farmType, center);
    }

    private BlockPos parseBlockPos(String str) {
        String[] parts = str.split(",");
        return new BlockPos(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        );
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Basic farm building and operation

- [ ] Create `MobFarmActionsPlugin` with basic actions
- [ ] Implement `BuildFarmAction` for tower farm construction
- [ ] Implement `OperateFarmAction` with monitor role
- [ ] Add farm coordinates to memory system
- [ ] Create basic spawn rate tracking

**Deliverables:**
- Agents can build a 4-layer tower farm
- Agents can monitor spawn rates
- Basic statistics logging

### Phase 2: Multi-Agent Coordination (Week 3-4)

**Goal:** Multiple agents operating farm simultaneously

- [ ] Implement `FarmCoordinator` for role assignment
- [ ] Create killer agent behavior (combat in kill chamber)
- [ ] Create collector agent behavior (hopper management)
- [ ] Add inter-agent communication for farm status
- [ ] Implement farm foreman role

**Deliverables:**
- 3+ agents can operate farm together
- Role-based task distribution
- Real-time coordination

### Phase 3: Optimization (Week 5-6)

**Goal:** Maximize spawn rates and efficiency

- [ ] Implement spawn platform optimization
- [ ] Add AFK spot calculator
- [ ] Create hopper minecart collection system
- [ ] Implement automatic item sorting
- [ ] Add efficiency metrics and reporting

**Deliverables:**
- Optimized spawn rates (18k+ spawns/hour)
- Automated collection and sorting
- Performance analytics

### Phase 4: Advanced Features (Week 7-8)

**Goal:** Specialized farm types and maintenance

- [ ] Implement spider farm design
- [ ] Implement nether portal farm
- [ ] Create maintenance agent (repairs, restocking)
- [ ] Add farm diagnostics and issue detection
- [ ] Implement adaptive optimization

**Deliverables:**
- Support for multiple farm types
- Self-maintaining farms
- Predictive issue detection

### Phase 5: Polish and Testing (Week 9-10)

**Goal:** Production-ready system

- [ ] Comprehensive testing
- [ ] Performance optimization
- [ ] Documentation completion
- [ ] UI/UX improvements
- [ ] Configurable settings

**Deliverables:**
- Stable, production-ready system
- Complete documentation
- User-friendly configuration

---

## Configuration and Tuning

### Config Options

```toml
[mob_farm]
# Farm operation settings
max_operation_duration_ticks = 6000  # 5 minutes
spawn_check_interval_ticks = 100     # 5 seconds
mob_cap_threshold = 70               # Minecraft mob cap

# AFK spot settings
default_afk_distance = 32            # blocks from spawn center
default_afk_height = 200             # Y level
min_spawn_distance = 24
max_spawn_distance = 128

# Platform settings
platform_width = 21
platform_length = 21
platform_layers = 4
layer_spacing = 10                   # blocks between layers

# Collection settings
hopper_count = 16
minecart_count = 8
collection_interval_ticks = 200      # 10 seconds

# Spawn rate targets
target_spawns_per_minute = 300
low_spawn_rate_threshold = 10       # mobs in farm area
efficiency_reporting_interval = 600 # 30 seconds

# Role assignment
auto_assign_roles = true
killer_agents = 1
collector_agents = 1
monitor_agents = 1
```

### Performance Tuning

```
Spawn Rate Optimization:
┌─────────────────────────────────────────────────────────────┐
│ Factor                │ Impact    │ Optimized Value         │
├─────────────────────────────────────────────────────────────┤
│ Distance from AFK     │ High     │ 32 blocks (optimal)      │
│ Platform layers       │ High     │ 4 layers (max efficient) │
│ Light level           │ Critical  │ 0 (complete darkness)    │
│ Water flow rate       │ Medium   │ Every 4 blocks           │
│ Chunk loading         │ Critical  │ All chunks must load     │
│ Mob cap management    │ High     | Kill mobs at cap (70)    │
│ Collection throughput  │ Medium   │ 8 hopper minecarts       │
└─────────────────────────────────────────────────────────────┘
```

---

## Conclusion

This design provides a comprehensive framework for multi-agent mob farm coordination in MineWright. The system leverages:

1. **Existing Architecture:** Builds on ActionExecutor, OrchestratorService, and plugin system
2. **Multi-Agent Synergy:** Role-based coordination for maximum efficiency
3. **Minecraft Mechanics:** Optimized for 1.20.1 spawning rules and collection systems
4. **Scalability:** Supports multiple farm types and agent counts
5. **Maintainability:** Modular design with clear separation of concerns

### Expected Performance

- **Spawn Rate:** 18,000+ mobs/hour (optimized tower farm)
- **Collection Rate:** 160+ items/second (hopper minecart system)
- **Agent Efficiency:** 3 agents can fully operate farm (killer, collector, monitor)
- **Automation:** Near-fully autonomous operation after initial build

### Next Steps

1. Review and approve design
2. Begin Phase 1 implementation
3. Regular testing and iteration
4. Community feedback integration
5. Continuous optimization

---

**Document Version:** 1.0.0
**Last Updated:** 2025-02-27
**Status:** Ready for Implementation
