# XP Farming Automation for MineWright Minecraft Mod

**Version:** 1.0.0
**Last Updated:** 2026-02-27
**Minecraft Version:** Forge 1.20.1
**Mod:** MineWright (Autonomous AI Agents)

---

## Table of Contents

1. [Overview](#overview)
2. [Farm Type Comparison](#farm-type-comparison)
3. [Automation Patterns](#automation-patterns)
4. [Safety Protocols](#safety-protocols)
5. [Multi-Agent Coordination](#multi-agent-coordination)
6. [Code Examples](#code-examples)
7. [Implementation Roadmap](#implementation-roadmap)
8. [Configuration](#configuration)
9. [Performance Metrics](#performance-metrics)

---

## Overview

This document outlines the design and implementation of automated XP (Experience) farming systems for the MineWright mod. XP farming is essential for:

- **Enchanting** - High-level enchantments require Level 30
- **Mending Gear** - Repair equipment with collected XP
- **Skill Progression** - Vanilla Minecraft progression

### Key Design Principles

1. **AFK-Capable** - Systems that work without player intervention
2. **Multi-Agent Coordination** - Multiple Foremen working in parallel
3. **Safety First** - Invulnerability during farming operations
4. **Efficiency** - Optimal XP per hour rates
5. **Scalability** - Easy to expand and modify

---

## Farm Type Comparison

### Tier 1: Early Game Farms (Recommended for Starting)

#### 1.1 Spider Spawner Farm
```
XP Rate: ★★★☆☆ (Low-Medium)
Setup Difficulty: ★☆☆☆☆ (Easy)
AFK Friendly: ★★★★☆ (Yes)
Resources: String, Spider Eyes, XP
Time to Level 30: ~24 minutes
```

**Design:**
- Water channels push spiders into kill chamber
- Magma block or suffocation damage (1-hit kills)
- Foreman stands at XP collection point

**Pros:**
- Easy to build in abandoned mineshafts
- Consistent spawn rate
- Good early game resource

**Cons:**
- Spider poison can be annoying
- Slower than dedicated farms
- Requires spawner dungeon

#### 1.2 Smelter Array (XP Bank)
```
XP Rate: ★★★☆☆ (Passive)
Setup Difficulty: ★☆☆☆☆ (Easy)
AFK Friendly: ★★★★★ (100% AFK)
Resources: Smelted items, XP
Time to Level 30: Varies by fuel
```

**Design:**
- Bamboo/Cacti auto-smelter (self-fueling)
- Iron golem farm + smelters
- Nether quartz smelting (3.5 XP per ore)

**Pros:**
- Completely AFK
- XP stored until collection
- Generates resources too

**Cons:**
- Requires initial setup
- Slower than mob farms
- Fuel management needed

---

### Tier 2: Mid Game Farms

#### 2.1 Gold Farm (Nether Portal)
```
XP Rate: ★★★★☆ (High)
Setup Difficulty: ★★☆☆☆ (Medium)
AFK Friendly: ★★★★☆ (Yes)
Resources: Gold Ingots, Gold Nuggets, XP, Rods
Time to Level 30: ~15 minutes
Time to Level 100: ~1 hour
```

**Design:**
- Nether portal in spawning floor
- Magma block kill chamber
- Minecart with hopper collection
- Foreman stands at collection point

**Pros:**
- Excellent XP rates
- Gold for golden apples, beacons
- Piglin bartering potential

**Cons:**
- Requires Nether access
- Portal placement critical
- Zombified Piglins can group up

#### 2.2 Raid Farm
```
XP Rate: ★★★★★ (Very High)
Setup Difficulty: ★★★☆☆ (Hard)
AFK Friendly: ★★★★★ (100% AFK)
Resources: Emeralds, Totems, Enchanted Books, XP
Time to Level 30: ~10 minutes
```

**Design:**
- Raid triggering system
- Transport minecart or boat
- One-block ceiling trap
- Lava blade or fall damage

**Pros:**
- Highest vanilla XP rates
- Totems of Undying (very valuable)
- Emerald income

**Cons:**
- Complex build (requires Bad Omen management)
- Requires villager access
- High resource investment

---

### Tier 3: Late Game Farms

#### 3.1 Enderman Farm (End Dimension)
```
XP Rate: ★★★★★ (Extreme)
Setup Difficulty: ★★★★☆ (Very Hard)
AFK Friendly: ★★★☆☆ (Requires attention)
Resources: Ender Pearls, XP
Time to Level 30: <1 minute
```

**Design:**
- Build at Y=1 in the End
- 2-block high ceiling (players can't fit, endermen can't teleport)
- Platform for hitting feet
- Lava for finishing off

**Pros:**
- Fastest XP in the game
- Infinite ender pearls
- Ender chest material

**Cons:**
- Requires End access
- Void death risk
- Building at Y=1 is dangerous
- Not fully AFK (need to hit)

#### 3.2 Guardian Temple Farm
```
XP Rate: ★★★★★ (Extreme)
Setup Difficulty: ★★★★★ (Expert)
AFK Friendly: ★★★★☆ (Mostly)
Resources: Prismarine, Sea Lanterns, Fish, XP
Time to Level 30: ~5 minutes
```

**Design:**
- Drain ocean monument
- Spawn-proof living quarters
- Flowing water to kill chamber
- Hopper minecart collection

**Pros:**
- Extremely fast XP
- Valuable prismarine
- Consistent spawns

**Cons:**
- Massive resource investment
- Draining monument is tedious
- Requires sponges (Elder Guardian)

---

## Summary Comparison Table

| Farm Type | XP/Hour | Setup Time | AFK | Multi-Agent | Early Game |
|-----------|---------|------------|-----|-------------|------------|
| Spider Spawner | 5,000 | 1 hour | Yes | Good | ★★★★★ |
| Smelter Array | 3,000 | 30 min | Yes | Excellent | ★★★★★ |
| Gold Farm | 15,000 | 2 hours | Yes | Good | ★★★☆☆ |
| Raid Farm | 30,000 | 4 hours | Yes | Fair | ★★☆☆☆ |
| Enderman | 50,000 | 3 hours | Partial | Poor | ★☆☆☆☆ |
| Guardian | 40,000 | 8 hours | Yes | Fair | ★☆☆☆☆ |

**Recommendation for MineWright:**
1. **Start** with Smelter Array (passive, always useful)
2. **Expand** to Spider Spawner (easy dungeon find)
3. **Scale** to Gold Farm (best mid-game investment)
4. **Endgame** with Raid Farm (maximum efficiency)

---

## Automation Patterns

### Pattern 1: AFK Station Keeping

**Use Case:** Agent stands at XP collection point indefinitely

```java
/**
 * Action for standing at XP farm collection point
 * Agent enters invulnerable mode and maintains position
 */
public class FarmAFKAction extends BaseAction {
    private final BlockPos stationPoint;
    private final int durationTicks;
    private int ticksElapsed;
    private static final int CHECK_INTERVAL = 100; // Check every 5 seconds

    public FarmAFKAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.stationPoint = task.getBlockPosParameter("stationPoint");
        this.durationTicks = task.getIntParameter("durationTicks", -1); // -1 = infinite
    }

    @Override
    protected void onStart() {
        foreman.setInvulnerableBuilding(true);
        foreman.setFlying(false);

        // Pathfind to station point
        foreman.getNavigation().moveTo(
            stationPoint.getX(),
            stationPoint.getY(),
            stationPoint.getZ(),
            1.0
        );

        ticksElapsed = 0;
        foreman.sendChatMessage("Taking position at XP farm...");
    }

    @Override
    protected void onTick() {
        ticksElapsed++;

        // Check if duration reached
        if (durationTicks > 0 && ticksElapsed >= durationTicks) {
            result = ActionResult.success("Farming session complete");
            return;
        }

        // Maintain position (return to station if moved)
        double distance = foreman.blockPosition().distSqr(stationPoint);
        if (distance > 4.0) {
            foreman.getNavigation().moveTo(
                stationPoint.getX(),
                stationPoint.getY(),
                stationPoint.getZ(),
                1.0
            );
        }

        // Periodically announce status
        if (ticksElapsed % CHECK_INTERVAL == 0) {
            int levels = foreman.experienceLevel;
            foreman.sendChatMessage(String.format(
                "Farming... Current level: %d | Time: %ds",
                levels,
                ticksElapsed / 20
            ));
        }
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "AFK at XP farm collection point";
    }
}
```

### Pattern 2: Resource Processing (Smelter Farm)

**Use Case:** Agent manages smelter array, refueling and collecting output

```java
/**
 * Action for managing automated smelter array
 * Handles fueling, input loading, and output collection
 */
public class ManageSmelterAction extends BaseAction {
    private final BlockPos smelterCenter;
    private final List<BlockPos> smelters;
    private final List<BlockPos> chests;
    private int phase; // 0=fuel, 1=input, 2=collect
    private static final int SMELTER_TICKS = 200; // 10 seconds per cycle

    public ManageSmelterAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.smelterCenter = task.getBlockPosParameter("center");
        // Assume 3x3 grid of smelters
        this.smelters = new ArrayList<>();
        this.chests = new ArrayList<>();
        initializeSmelterPositions();
    }

    private void initializeSmelterPositions() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                smelters.add(smelterCenter.offset(x, 0, z));
            }
        }
        // Chest at top and bottom
        chests.add(smelterCenter.above(2));
        chests.add(smelterCenter.below(2));
    }

    @Override
    protected void onStart() {
        foreman.setFlying(true); // Fly for easy access
        foreman.setInvulnerableBuilding(true);
        phase = 0;
        foreman.sendChatMessage("Managing smelter array...");
    }

    @Override
    protected void onTick() {
        int cycle = (int)(foreman.tickCount / SMELTER_TICKS) % 3;

        switch (cycle) {
            case 0:
                refuelSmelters();
                break;
            case 1:
                loadInputs();
                break;
            case 2:
                collectOutput();
                break;
        }
    }

    private void refuelSmelters() {
        // Check each smelter for fuel
        for (BlockPos pos : smelters) {
            // Logic to check fuel slot and add fuel if needed
            // Use bamboo/coal bucket from storage chest
        }
    }

    private void loadInputs() {
        // Load smeltables from input chest
        // Auto-balance across all smelters
    }

    private void collectOutput() {
        // Collect smelted items to output chest
        // XP orbs automatically collected by Foreman proximity
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.setInvulnerableBuilding(false);
    }

    @Override
    public String getDescription() {
        return "Manage smelter array";
    }
}
```

### Pattern 3: Mob Farm Killer (Active)

**Use Case:** Agent kills mobs at spawn point for XP

```java
/**
 * Action for automated mob farm killing
 * Agent stands at kill point and attacks mobs pushed into range
 */
public class MobFarmKillerAction extends BaseAction {
    private final BlockPos killPosition;
    private final int searchRadius;
    private int ticksSinceLastKill;
    private int mobsKilled;
    private static final int KILL_RANGE = 4;

    public MobFarmKillerAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.killPosition = task.getBlockPosParameter("killPosition");
        this.searchRadius = task.getIntParameter("radius", 16);
    }

    @Override
    protected void onStart() {
        foreman.setInvulnerableBuilding(true);
        foreman.setFlying(false);

        // Pathfind to kill position
        foreman.getNavigation().moveTo(
            killPosition.getX(),
            killPosition.getY(),
            killPosition.getZ(),
            1.0
        );

        ticksSinceLastKill = 0;
        mobsKilled = 0;
        foreman.sendChatMessage("Taking position at mob farm...");
    }

    @Override
    protected void onTick() {
        ticksSinceLastKill++;

        // Maintain position
        if (foreman.blockPosition().distSqr(killPosition) > 4.0) {
            foreman.getNavigation().moveTo(
                killPosition.getX(),
                killPosition.getY(),
                killPosition.getZ(),
                1.0
            );
            return;
        }

        // Attack nearby mobs
        AABB searchBox = new AABB(killPosition).inflate(searchRadius);
        List<net.minecraft.world.entity.Entity> entities = foreman.level().getEntities(
            foreman,
            searchBox
        );

        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living &&
                entity instanceof net.minecraft.world.entity.monster.Monster) {

                double distance = foreman.distanceTo(living);
                if (distance <= KILL_RANGE) {
                    // Attack the mob
                    foreman.doHurtTarget(living);
                    foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                    mobsKilled++;
                    ticksSinceLastKill = 0;
                }
            }
        }

        // Announce progress every minute
        if (ticksSinceLastKill % 1200 == 0 && mobsKilled > 0) {
            int xpGained = estimateXPGained(mobsKilled);
            foreman.sendChatMessage(String.format(
                "Killed %d mobs (~%d XP) | Level: %d",
                mobsKilled,
                xpGained,
                foreman.experienceLevel
            ));
            mobsKilled = 0;
        }
    }

    private int estimateXPGained(int mobCount) {
        // Average 5 XP per mob
        return mobCount * 5;
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Kill mobs at farm";
    }
}
```

---

## Safety Protocols

### 1. Invulnerability Management

**Critical:** All farming actions must enable invulnerability to prevent agent death.

```java
// Enable invulnerability at start
foreman.setInvulnerableBuilding(true);
foreman.setInvulnerable(true); // Double-set for safety

// Disable on completion
foreman.setInvulnerableBuilding(false);
```

### 2. Hazard Protection

| Hazard | Protection Level | Notes |
|--------|-----------------|-------|
| Lava | Full | Invulnerable agents ignore lava damage |
| Suffocation | Full | Agents can be buried in blocks |
| Fall Damage | Full | Agents take no fall damage |
| Void | Partial | Still kills even when invulnerable |
| Mobs | Full | Agents take no mob damage |

### 3. Void Safety (Critical for End/Enderman Farms)

```java
/**
 * Action extension with void safety
 * Prevents agents from falling into the void
 */
public abstract class VoidSafeAction extends BaseAction {
    protected final int safeYLevel;
    private static final int VOID_THRESHOLD = -64;

    protected VoidSafeAction(ForemanEntity foreman, Task task, int safeY) {
        super(foreman, task);
        this.safeYLevel = safeY;
    }

    @Override
    protected void onTick() {
        // Check for void danger
        if (foreman.getY() < safeYLevel) {
            foreman.sendChatMessage("VOID DANGER! Teleporting to safety!");
            foreman.teleportTo(
                foreman.getX(),
                safeYLevel + 2,
                foreman.getZ()
            );
            result = ActionResult.failure("Void escape triggered");
            return;
        }

        // Normal tick logic
        executeTick();
    }

    protected abstract void executeTick();
}
```

### 4. Equipment Management

**Mending Gear Setup:**
- All agents should have Mending enchantment on armor/tools
- XP collected automatically repairs gear
- Prevents equipment loss during farming

```java
/**
 * Check and equip best gear for farming
 */
public class EquipFarmingGearAction extends BaseAction {
    @Override
    protected void onStart() {
        // Ensure Mending gear is equipped
        // Priority: Mending > Unbreaking > Protection
        foreman.sendChatMessage("Equipping farming gear...");

        // Logic to swap inventory items
        // Check armor slots, weapon slot
        // Equip best available gear

        result = ActionResult.success("Gear equipped");
    }

    @Override
    protected void onTick() {
        // Instant action
    }

    @Override
    protected void onCancel() {
        // Nothing to clean up
    }

    @Override
    public String getDescription() {
        return "Equip farming gear";
    }
}
```

---

## Multi-Agent Coordination

### Architecture Overview

MineWright's `OrchestratorService` coordinates multiple agents for efficient farming:

```
                    Human Player
                         │
                         ▼
              ┌──────────────────┐
              │   FOREMAN        │
              │   (Level 30+)    │
              └────────┬─────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │ Worker  │   │ Worker  │   │ Worker  │
   │ Farm 1  │   │ Farm 2  │   │ Farm 3  │
   └─────────┘   └─────────┘   └─────────┘
```

### Coordination Patterns

#### 1. Spatial Partitioning (Already Implemented)

Used in `CollaborativeBuildManager`, can be adapted for farming:

```java
/**
 * Divide farm zones among multiple agents
 */
public class FarmZoneManager {
    public static class FarmZone {
        public final String zoneId;
        public final BlockPos stationPoint;
        public final String farmType;
        private volatile String assignedAgent;

        public FarmZone(String id, BlockPos pos, String type) {
            this.zoneId = id;
            this.stationPoint = pos;
            this.farmType = type;
        }

        public synchronized boolean claim(String agentName) {
            if (assignedAgent == null) {
                assignedAgent = agentName;
                return true;
            }
            return false;
        }

        public synchronized void release() {
            assignedAgent = null;
        }
    }

    private static final Map<String, FarmZone> activeZones = new ConcurrentHashMap<>();

    public static FarmZone assignZone(String agentName, String farmType) {
        // Find unassigned zone of matching type
        for (FarmZone zone : activeZones.values()) {
            if (zone.farmType.equals(farmType) && zone.claim(agentName)) {
                return zone;
            }
        }

        // Create new zone if none available
        String zoneId = farmType + "_" + activeZones.size();
        BlockPos pos = calculateZonePosition(farmType);
        FarmZone newZone = new FarmZone(zoneId, pos, farmType);
        newZone.claim(agentName);
        activeZones.put(zoneId, newZone);

        return newZone;
    }

    private static BlockPos calculateZonePosition(String farmType) {
        // Calculate position based on farm type
        // Each farm type has different optimal spacing
        return new BlockPos(0, 64, 0); // Placeholder
    }
}
```

#### 2. Task Distribution

Foreman distributes farming tasks via `OrchestratorService`:

```java
/**
 * XP Farming coordination via Orchestrator
 */
public class XPFarmingOrchestrator {
    private final OrchestratorService orchestrator;

    public XPFarmingOrchestrator(OrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Assign multiple agents to different farm types
     */
    public String assignFarmingDuties(List<ForemanEntity> agents) {
        String planId = UUID.randomUUID().toString().substring(0, 8);

        List<Task> tasks = new ArrayList<>();

        // Task 1: Smelter management (1 agent)
        tasks.add(new Task("manage_smelter", Map.of(
            "center", "0, 64, 0",
            "duration", "3600" // 3 minutes
        )));

        // Task 2: Mob farm killer (1 agent)
        tasks.add(new Task("kill_mobs", Map.of(
            "killPosition", "100, 64, 100",
            "radius", "16"
        )));

        // Task 3: AFK station at gold farm (remaining agents)
        for (int i = 2; i < agents.size(); i++) {
            tasks.add(new Task("farm_afk", Map.of(
                "stationPoint", "200, 64, 200",
                "duration", "-1" // Infinite
            )));
        }

        // Submit to orchestrator
        ResponseParser.ParsedResponse response = new ResponseParser.ParsedResponse(
            "XP farming operation",
            tasks
        );

        return orchestrator.processHumanCommand(response, agents);
    }
}
```

#### 3. Load Balancing

Agents can request help when overwhelmed:

```java
/**
 * Agent requests help when farm is too productive
 */
public class HelpRequestSystem {
    public static void requestHelp(ForemanEntity requester, String reason) {
        AgentMessage helpRequest = new AgentMessage.Builder()
            .type(AgentMessage.Type.HELP_REQUEST)
            .sender(requester.getSteveName(), requester.getSteveName())
            .recipient("foreman")
            .content(String.format("Help needed at %s: %s",
                requester.blockPosition().toString(),
                reason))
            .payload("position", requester.blockPosition().toString())
            .priority(AgentMessage.Priority.HIGH)
            .build();

        requester.sendMessage(helpRequest);
    }

    public static void handleHelpRequest(AgentMessage message, OrchestratorService orchestrator) {
        // Find idle agent
        String idleAgent = findIdleAgent(orchestrator);

        if (idleAgent != null) {
            // Send idle agent to help
            AgentMessage assignment = AgentMessage.taskAssignment(
                "foreman",
                "Foreman",
                idleAgent,
                "assist_farming",
                Map.of(
                    "assistTarget", message.getSenderId(),
                    "position", message.getPayloadValue("position", "")
                )
            );

            orchestrator.getCommunicationBus().publish(assignment);
        }
    }

    private static String findIdleAgent(OrchestratorService orchestrator) {
        // Query all workers for status
        // Return first idle agent
        return null; // Simplified
    }
}
```

---

## Code Examples

### Example 1: Complete XP Farm Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Complete XP farming action for mob farms
 * Handles positioning, combat, and XP collection
 */
public class XPFarmAction extends BaseAction {
    private final BlockPos stationPoint;
    private final String farmType;
    private final int maxDurationTicks;

    private int ticksRunning;
    private int mobsKilled;
    private int lastAnnouncementTick;

    private static final int ANNOUNCE_INTERVAL = 1200; // Every minute
    private static final int ATTACK_RANGE = 4;
    private static final int SEARCH_RADIUS = 16;

    public XPFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.stationPoint = task.getBlockPosParameter("stationPoint");
        this.farmType = task.getStringParameter("farmType", "mob");
        this.maxDurationTicks = task.getIntParameter("duration", -1);
    }

    @Override
    protected void onStart() {
        foreman.setInvulnerableBuilding(true);
        foreman.setFlying(false);

        // Navigate to station point
        if (stationPoint != null) {
            foreman.getNavigation().moveTo(
                stationPoint.getX(),
                stationPoint.getY(),
                stationPoint.getZ(),
                1.0
            );
        }

        ticksRunning = 0;
        mobsKilled = 0;
        lastAnnouncementTick = 0;

        foreman.sendChatMessage(String.format(
            "Starting %s farm at %s",
            farmType,
            stationPoint != null ? stationPoint.toString() : "current location"
        ));
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        // Check duration
        if (maxDurationTicks > 0 && ticksRunning >= maxDurationTicks) {
            result = ActionResult.success(String.format(
                "Farming complete. Killed %d mobs in %ds",
                mobsKilled,
                ticksRunning / 20
            ));
            return;
        }

        // Maintain position
        if (stationPoint != null && !isAtStation()) {
            returnToStation();
            return;
        }

        // Farm based on type
        switch (farmType.toLowerCase()) {
            case "mob":
                farmMobs();
                break;
            case "afk":
                // Just stand there, XP comes to us
                break;
            case "enderman":
                farmEnderman();
                break;
            default:
                farmMobs();
        }

        // Announce progress
        if (ticksRunning - lastAnnouncementTick >= ANNOUNCE_INTERVAL) {
            announceProgress();
            lastAnnouncementTick = ticksRunning;
        }
    }

    private boolean isAtStation() {
        if (stationPoint == null) return true;
        return foreman.blockPosition().distSqr(stationPoint) <= 9.0; // 3 blocks
    }

    private void returnToStation() {
        foreman.getNavigation().moveTo(
            stationPoint.getX(),
            stationPoint.getY(),
            stationPoint.getZ(),
            1.0
        );
    }

    private void farmMobs() {
        AABB searchBox = foreman.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        for (Entity entity : entities) {
            if (entity instanceof Monster monster && monster.isAlive()) {
                double distance = foreman.distanceTo(monster);

                if (distance <= ATTACK_RANGE) {
                    // Kill the mob
                    attackMob(monster);
                    mobsKilled++;
                }
            }
        }
    }

    private void farmEnderman() {
        // Specialized enderman farming
        // Hit feet, avoid eye contact
        AABB searchBox = foreman.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        for (Entity entity : entities) {
            String entityType = entity.getType().toString().toLowerCase();
            if (entityType.contains("enderman") && entity instanceof LivingEntity enderman) {
                double distance = foreman.distanceTo(enderman);

                if (distance <= ATTACK_RANGE + 2) {
                    // Attack enderman (look at feet to avoid teleport)
                    attackMob(enderman);
                    mobsKilled++;
                }
            }
        }
    }

    private void attackMob(LivingEntity mob) {
        foreman.doHurtTarget(mob);
        foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private void announceProgress() {
        int currentLevel = foreman.experienceLevel;
        int xpPerMinute = (mobsKilled * 5) / (ticksRunning / 1200 + 1);

        foreman.sendChatMessage(String.format(
            "Farm Status: %d mobs killed | ~%d XP/min | Level: %d | Time: %dm",
            mobsKilled,
            xpPerMinute,
            currentLevel,
            ticksRunning / 1200
        ));
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();

        foreman.sendChatMessage(String.format(
            "Farming cancelled. Killed %d mobs",
            mobsKilled
        ));
    }

    @Override
    public String getDescription() {
        return String.format("XP farming at %s",
            stationPoint != null ? stationPoint.toString() : "current location");
    }
}
```

### Example 2: Smelter Array Manager

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages automated smelter array for XP farming
 * Handles fueling, input loading, and XP collection
 */
public class SmelterArrayAction extends BaseAction {
    private final BlockPos centerPos;
    private final int radius;
    private final List<BlockPos> smelters;

    private int ticksRunning;
    private int cycleTicks;
    private static final int CYCLE_DURATION = 200; // 10 seconds per cycle
    private static final int FUEL_SLOT = 2;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    public SmelterArrayAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.centerPos = task.getBlockPosParameter("center");
        this.radius = task.getIntParameter("radius", 1);
        this.smelters = new ArrayList<>();
        initializeSmelters();
    }

    private void initializeSmelters() {
        // Scan area for furnaces
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    if (foreman.level().getBlockState(pos).is(Blocks.FURNACE) ||
                        foreman.level().getBlockState(pos).is(Blocks.SMOKER)) {
                        smelters.add(pos);
                    }
                }
            }
        }

        foreman.sendChatMessage(String.format(
            "Found %d smelters to manage",
            smelters.size()
        ));
    }

    @Override
    protected void onStart() {
        foreman.setFlying(true);
        foreman.setInvulnerableBuilding(true);
        ticksRunning = 0;
        cycleTicks = 0;
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        cycleTicks++;

        // Run management cycle
        if (cycleTicks >= CYCLE_DURATION) {
            cycleTicks = 0;
            runManagementCycle();
        }

        // Announce every 5 minutes
        if (ticksRunning % 6000 == 0) {
            announceStatus();
        }
    }

    private void runManagementCycle() {
        int refueled = 0;
        int loaded = 0;
        int collected = 0;

        for (BlockPos pos : smelters) {
            if (foreman.level().getBlockEntity(pos) instanceof FurnaceBlockEntity furnace) {
                // Check fuel
                if (needsFuel(furnace)) {
                    addFuel(furnace);
                    refueled++;
                }

                // Collect output
                if (hasOutput(furnace)) {
                    collectOutput(furnace);
                    collected++;
                }

                // Load input (would need inventory access)
                // This is simplified - real implementation would interact with chests
            }
        }

        // Log activity
        if (refueled + loaded + collected > 0) {
            foreman.sendChatMessage(String.format(
                "Smelter cycle: %d refueled, %d collected",
                refueled,
                collected
            ));
        }
    }

    private boolean needsFuel(FurnaceBlockEntity furnace) {
        return furnace.getItem(FUEL_SLOT).isEmpty() ||
               furnace.getItem(FUEL_SLOT).getCount() < 4;
    }

    private void addFuel(FurnaceBlockEntity furnace) {
        // Would pull from fuel chest and insert
        // Simplified for example
    }

    private boolean hasOutput(FurnaceBlockEntity furnace) {
        return !furnace.getItem(OUTPUT_SLOT).isEmpty();
    }

    private void collectOutput(FurnaceBlockEntity furnace) {
        // Would extract to output chest
        // XP is automatically collected by Foreman proximity
    }

    private void announceStatus() {
        int currentLevel = foreman.experienceLevel;
        foreman.sendChatMessage(String.format(
            "Smelter array running | Level: %d | Time: %dm",
            currentLevel,
            ticksRunning / 1200
        ));
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.setInvulnerableBuilding(false);
    }

    @Override
    public String getDescription() {
        return "Manage smelter array";
    }
}
```

### Example 3: Register XP Farm Actions

```java
package com.minewright.plugin;

import com.minewright.action.actions.*;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;

/**
 * XP Farming plugin for MineWright
 * Registers all XP farming actions
 */
public class XPFarmingPlugin implements ActionPlugin {
    @Override
    public void registerActions(ActionRegistry registry) {
        // AFK farming (for smelters, gold farms, etc.)
        registry.register("farm_afk", (foreman, task, context) ->
            new FarmAFKAction(foreman, task));

        // Active mob farming
        registry.register("farm_xp", (foreman, task, context) ->
            new XPFarmAction(foreman, task));

        // Smelter array management
        registry.register("manage_smelter", (foreman, task, context) ->
            new SmelterArrayAction(foreman, task));

        // Enderman farming (specialized)
        registry.register("farm_enderman", (foreman, task, context) ->
            new EndermanFarmAction(foreman, task));

        // Raid farm participation
        registry.register("farm_raid", (foreman, task, context) ->
            new RaidFarmAction(foreman, task));
    }

    @Override
    public String getPluginName() {
        return "XP Farming";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Basic XP farming capability

- [ ] Create `XPFarmAction` class
- [ ] Implement AFK station keeping
- [ ] Add invulnerability safety checks
- [ ] Test with simple mob spawner
- [ ] Create `XPFarmingPlugin` to register actions

**Deliverables:**
- Working mob farm action
- Agents can stand at collection point
- Basic safety systems in place

### Phase 2: Smelter Integration (Week 3)

**Goal:** Passive XP generation

- [ ] Create `SmelterArrayAction` class
- [ ] Implement fuel management logic
- [ ] Add input/output handling
- [ ] Test with bamboo/cacti auto-smelter
- [ ] Optimize fuel efficiency

**Deliverables:**
- Working smelter manager
- Agents can maintain 3x3 smelter array
- Passive XP while AFK

### Phase 3: Multi-Agent Coordination (Week 4-5)

**Goal:** Multiple agents farming simultaneously

- [ ] Implement `FarmZoneManager`
- [ ] Add zone assignment logic
- [ ] Create help request system
- [ ] Test with 3+ agents
- [ ] Balance load distribution

**Deliverables:**
- Agents can claim farm zones
- Help requests work correctly
- No interference between agents

### Phase 4: Advanced Farms (Week 6-7)

**Goal:** High-efficiency farms

- [ ] Implement `EndermanFarmAction`
- [ ] Add void safety systems
- [ ] Create `RaidFarmAction`
- [ ] Implement raid triggering logic
- [ ] Test at Y=1 in End

**Deliverables:**
- Working enderman farm support
- Raid farm automation
- Maximum XP rates achieved

### Phase 5: Optimization & Polish (Week 8)

**Goal:** Production-ready system

- [ ] Performance profiling
- [ ] Memory optimization
- [ ] Error handling improvements
- [ ] User documentation
- [ ] Configuration options

**Deliverables:**
- Optimized codebase
- Complete documentation
- Configuration system
- Production-ready release

---

## Configuration

### config/minewright-common.toml

```toml
[xp_farming]
# Enable/disable XP farming features
enabled = true

# Maximum farming duration (ticks, -1 = infinite)
max_duration = -1

# Announce interval (ticks)
announce_interval = 1200

# Safety settings
[xp_farming.safety]
# Always use invulnerability mode
invulnerable = true

# Void safety Y level (agents teleport above this)
void_safe_y = -60

# Enable void safety checks
void_protection = true

# Farm-specific settings
[xp_farming.farms]
# Mob farm settings
[xp_farming.farms.mob]
attack_range = 4
search_radius = 16
max_targets_per_tick = 3

# Smelter settings
[xp_farming.farms.smelter]
cycle_duration = 200
fuel_threshold = 4
auto_refuel = true

# Enderman farm settings
[xp_farming.farms.enderman]
attack_range = 6
avoid_eye_contact = true
void_safe_y = 5

# Multi-agent settings
[xp_farming.multi_agent]
# Enable zone management
zone_management = true

# Minimum agents for zone system
min_agents_for_zones = 2

# Help request timeout (ms)
help_timeout = 30000

# Load balancing
[xp_farming.load_balancing]
# Enable automatic load balancing
auto_balance = true

# Check interval (ticks)
check_interval = 100

# Idle threshold (ticks)
idle_threshold = 600
```

---

## Performance Metrics

### Expected XP Rates

| Farm Type | XP/Hour (Single Agent) | XP/Hour (3 Agents) | Notes |
|-----------|----------------------|-------------------|-------|
| Spider Spawner | 5,000 | 15,000 | Easy to set up |
| Smelter Array | 3,000 | 9,000 | Passive |
| Gold Farm | 15,000 | 45,000 | Best mid-game |
| Raid Farm | 30,000 | 60,000 | Complex build |
| Enderman | 50,000 | 50,000 | Single agent only |

### Leveling Times

| Target Level | Spider (1 agent) | Gold (1 agent) | Gold (3 agents) |
|--------------|------------------|----------------|-----------------|
| Level 30 | 24 min | 8 min | 3 min |
| Level 50 | 40 min | 13 min | 4 min |
| Level 100 | 80 min | 27 min | 9 min |

### Resource Requirements

| Farm Type | Blocks | Redstone | Time | Skill |
|-----------|--------|----------|------|-------|
| Spider | 500 | 50 | 1 hr | Easy |
| Smelter | 300 | 0 | 30 min | Easy |
| Gold | 1,000 | 100 | 2 hr | Medium |
| Raid | 2,000 | 500 | 4 hr | Hard |
| Enderman | 500 | 0 | 3 hr | Hard |

---

## Safety Checklist

Before deploying agents to XP farms:

- [ ] Invulnerability enabled
- [ ] Mending gear equipped
- [ ] Void safety enabled (for End)
- [ ] Station point verified
- [ ] Emergency exit planned
- [ ] Communication bus active
- [ ] Orchestrator registered
- [ ] Help requests configured

---

## Troubleshooting

### Issue: Agent not collecting XP

**Solution:**
1. Verify agent is within 8 blocks of XP orb source
2. Check invulnerability is enabled
3. Ensure agent is not flying (XP collection works better on ground)

### Issue: Agent dying in void

**Solution:**
1. Enable void protection in config
2. Set safe Y level above -60
3. Check agent is not in flying mode near void

### Issue: Low XP rates

**Solution:**
1. Verify farm is built correctly
2. Check spawn conditions (light level for mob farms)
3. Ensure agent is positioned optimally
4. Consider multiple agents for parallel farming

### Issue: Agents interfering with each other

**Solution:**
1. Enable zone management
2. Increase spacing between farm stations
3. Use Orchestrator for task distribution
4. Assign specific roles per agent

---

## Additional Resources

### Research Sources

Based on research from:
- [Minecraft Wiki - Experience Farming Tutorials](https://minecraft.fandom.com/wiki/Tutorials/Experience_farming)
- [Screen Rant - 16 Best XP Farms for Minecraft 1.21](https://screenrant.com/best-xp-farms-minecraft/)
- [TheGamer - Best Ways to Get XP Fast](https://www.thegamer.com/minecraft-fast-easy-level/)
- [YuvDwi/Steve - Autonomous AI Agent for Minecraft](https://github.com/YuvDwi/Steve)
- [MineStudio - Minecraft AI Agent Development Toolkit](https://github.com/CraftJarvis/MineStudio)

### Recommended Mods for XP Farming (Forge 1.20.1)

- **Better Than Mending** - Enhanced mending mechanics
- **Easy Auto Cycler** - Auto-find Mending books from villagers
- **Experience Jars** - Store and manage XP
- **Create: Integrated Farming** - Automation with Create mod

---

## Appendix: Quick Reference

### Action Names (for commands)

```
/foreman <name> farm_afk stationPoint:<x,y,z> duration:<ticks>
/foreman <name> farm_xp stationPoint:<x,y,z> farmType:<type> duration:<ticks>
/foreman <name> manage_smelter center:<x,y,z> radius:<n>
/foreman <name> farm_enderman stationPoint:<x,y,z> duration:<ticks>
/foreman <name> farm_raid stationPoint:<x,y,z> duration:<ticks>
```

### Example Commands

```
# Send agent to AFK at gold farm
/foreman Steve1 farm_afk stationPoint:200,64,200 duration:-1

# Active mob farming
/foreman Steve2 farm_xp stationPoint:100,70,100 farmType:mob duration:3600

# Manage smelter array
/foreman Steve3 manage_smelter center:0,64,0 radius:1

# Enderman farming (End dimension)
/foreman Steve4 farm_enderman stationPoint:0,1,0 duration:1800
```

---

**Document Version:** 1.0.0
**Author:** MineWright Development Team
**License:** MIT

---

## Changelog

### v1.0.0 (2026-02-27)
- Initial release
- Farm comparison matrix
- Basic action implementations
- Multi-agent coordination patterns
- Safety protocols
- Configuration options
- Implementation roadmap
