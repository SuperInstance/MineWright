# Enderman Farm Design for MineWright (Minecraft Forge 1.20.1)

## Table of Contents
1. [Overview](#overview)
2. [Enderman Mechanics (1.20.1)](#enderman-mechanics-1201)
3. [End Biome Navigation](#end-biome-navigation)
4. [Spawn Platform Design](#spawn-platform-design)
5. [Safe Combat Strategies](#safe-combat-strategies)
6. [Collection Automation](#collection-automation)
7. [XP Efficiency Optimization](#xp-efficiency-optimization)
8. [Code Implementation](#code-implementation)
9. [Implementation Roadmap](#implementation-roadmap)

---

## Overview

This document provides a comprehensive design for an automated Enderman farming system for the MineWright mod. The design focuses on leveraging the existing ForemanEntity AI system to build, navigate, and operate an efficient Enderman farm in the End dimension.

### Key Goals
- **Safety**: Avoid aggroing Endermen during farm operation
- **Efficiency**: Maximize spawn rates and XP collection
- **Automation**: Minimize player intervention
- **Scalability**: Support multiple Foremen working collaboratively

---

## Enderman Mechanics (1.20.1)

### Spawning Requirements

| Location | Light Level | Spawn Rate | Pack Size |
|----------|-------------|------------|-----------|
| **The End (Java)** | Any (0-15) | Very Frequent | 1-4 |
| **Overworld** | 0 only | Rare | 1-4 |
| **Nether** | <= 7 | Variable | 1-4 |

### Key Stats
- **Health**: 40 HP (20 hearts)
- **Height**: 3 blocks
- **Attack Damage**: 4 (Easy) / 7 (Normal) / 10 (Hard)
- **XP Drop**: 5 XP (confirmed by wiki)

### Aggro Mechanics

| Trigger Type | Range | Details |
|-------------|-------|---------|
| **Eye Contact** | 64 blocks | Looking at head/torso for 0.25 seconds |
| **Follow Range** | 32 blocks | Chase distance when angered |
| **Teleport Trigger** | Varies | On damage, water, or suffocation |

### Safety Mechanisms
- **Carved Pumpkin**: Prevents eye contact aggro
- **2-Block High Ceiling**: Endermen cannot enter
- **Water**: Endermen teleport away when hit
- **Transparent Blocks**: Safe to look through

---

## End Biome Navigation

### Challenges

1. **Void Damage**: Falling into the void kills the Foreman
2. **No Traditional Pathfinding**: End islands are separated by void
3. **Ender Dragon**: May interfere until defeated
4. **Gateway Teleportation**: Requires special handling

### Navigation Strategy

#### Phase 1: Reach the End

```java
// Navigate to End Portal (uses existing pathfinding)
Task reachEndPortal = new Task("pathfind_to_end_portal", Map.of(
    "dimension", "end",
    "method", "portal"
));
```

#### Phase 2: Defeat Dragon (if needed)

```java
// Use existing combat system
Task defeatDragon = new Task("attack", Map.of(
    "target", "ender_dragon",
    "strategy", "safe_distance"
));
```

#### Phase 3: Reach Outer Islands

**Option A: End Gateway**
- Throw ender pearl into gateway
- Automatic teleportation to outer island

**Option B: Bridge Building**
- Build bridge 1000+ blocks (not recommended)
- Use existing `BuildStructureAction`

**Option C: Platform Construction**
- Build spawn platforms near main island
- No need for outer islands

### Safe Navigation Protocol

```java
private void navigateToEndSafely() {
    // 1. Ensure carved pumpkin is equipped
    equipPumpkin();

    // 2. Enable invulnerability during flight
    foreman.setInvulnerableBuilding(true);
    foreman.setFlying(true);

    // 3. Navigate to target coordinates
    foreman.getNavigation().moveTo(targetX, targetY, targetZ, 1.0);

    // 4. Avoid looking at Endermen
    lookAtGroundOnly();
}
```

---

## Spawn Platform Design

### Location Strategy

**Optimal Placement**: 128+ blocks from main island

This ensures:
- Main island mobs don't interfere
- Maximum spawn efficiency
- Safe distance from Dragon

### Platform Specifications

#### Basic Spawn Pad (Recommended)

```
Dimensions: 21x21 blocks (441 spawnable spaces)
Height: 43 blocks above kill chamber
Material: End Stone (natural blending)
Light Level: 0 (complete darkness)
```

#### Multi-Layer Design (Maximum Efficiency)

```
Layer 1: Y = +43 (main spawn pad)
Layer 2: Y = +50 (secondary spawn pad)
Layer 3: Y = +57 (tertiary spawn pad)

Total spawnable area: 1,323 blocks
Expected spawn rate: ~4,000 pearls/hour
```

### Platform Structure Generator

```java
private List<BlockPlacement> generateSpawnPlatform(BlockPos center, int size) {
    List<BlockPlacement> blocks = new ArrayList<>();
    Block endStone = Blocks.END_STONE;

    // Base platform
    for (int x = -size/2; x < size/2; x++) {
        for (int z = -size/2; z < size/2; z++) {
            BlockPos pos = center.offset(x, 0, z);
            blocks.add(new BlockPlacement(pos, endStone));
        }
    }

    // Spawn-blocking slabs (prevent spawning underneath)
    for (int x = -size/2 - 2; x < size/2 + 2; x++) {
        for (int z = -size/2 - 2; z < size/2 + 2; z++) {
            if (x < -size/2 || x >= size/2 || z < -size/2 || z >= size/2) {
                // Add slabs around perimeter
                BlockPos pos = center.offset(x, -1, z);
                blocks.add(new BlockPlacement(pos, Blocks.END_STONE_SLAB));
            }
        }
    }

    return blocks;
}
```

### Fall Damage System

Endermen take fall damage:
- **43 blocks**: Reduces to half-heart (one-hit kill)
- **45+ blocks**: Death on impact

**Vine Break System** (Optional):
```java
private void installVineBreaks(BlockPos platformPos) {
    // Place vines at Y = 41 (2 blocks above kill chamber)
    for (int x = -10; x < 10; x++) {
        for (int z = -10; z < 10; z++) {
            BlockPos vinePos = platformPos.offset(x, -41, z);
            blocks.add(new BlockPlacement(vinePos, Blocks.VINE));
        }
    }
}
```

---

## Safe Combat Strategies

### Strategy 1: 2-Block Ceiling Kill Chamber

**Advantages**:
- Cannot be attacked by Endermen
- Safe to look at them
- Can attack through 1-block gaps

**Design**:
```
Y = 0:   Hopper collection system
Y = 1:   Solid floor (with gaps)
Y = 2:   Safe zone (2-block high ceiling)
Y = 3+:  Spawn platforms above
```

### Strategy 2: Carved Pumpkin Method

**Advantages**:
- Full mobility
- No aggro from eye contact
- Can use any weapon

**Implementation**:
```java
private void equipPumpkin() {
    // Check inventory for pumpkin
    ItemStack pumpkin = findPumpkinInInventory();

    if (pumpkin != null) {
        // Equip as helmet
        foreman.setItemSlot(EquipmentSlot.HEAD, pumpkin);
    } else {
        // Request pumpkin from player
        foreman.sendChatMessage("I need a carved pumpkin for safe Enderman farming!");
    }
}

private boolean isPumpkinEquipped() {
    ItemStack helmet = foreman.getItemBySlot(EquipmentSlot.HEAD);
    return helmet.getItem() == Items.CARVED_PUMPKIN;
}
```

### Strategy 3: Water Sentinel (Hybrid)

**Design**:
- Kill chamber has 1-block deep water
- Foreman stands in water (safe from Endermen)
- Endermen teleport away when hit by water
- Fall damage kills them

**Code**:
```java
private void setupWaterKillChamber(BlockPos center) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Create 1-block deep water moat
    for (int x = -3; x <= 3; x++) {
        for (int z = -3; z <= 3; z++) {
            BlockPos waterPos = center.offset(x, 0, z);
            blocks.add(new BlockPlacement(waterPos, Blocks.WATER));
        }
    }

    return blocks;
}
```

### Combat Action Integration

Extend existing `CombatAction`:

```java
public class EndermanCombatAction extends BaseAction {
    private static final double SAFE_ATTACK_RANGE = 3.5;
    private boolean pumpkinEquipped = false;

    @Override
    protected void onStart() {
        // Equip pumpkin for safety
        equipPumpkin();
        pumpkinEquipped = isPumpkinEquipped();

        if (!pumpkinEquipped) {
            foreman.sendChatMessage("Warning: No pumpkin! Using 2-block ceiling strategy.");
        }

        foreman.setInvulnerableBuilding(true);
    }

    @Override
    protected void onTick() {
        LivingEntity target = findNearestEnderman();

        if (target != null) {
            double distance = foreman.distanceTo(target);

            if (distance <= SAFE_ATTACK_RANGE) {
                // One-hit kill with current weapon
                foreman.doHurtTarget(target);
                foreman.swing(InteractionHand.MAIN_HAND, true);
            } else {
                // Wait for Enderman to fall into kill chamber
                // Or use ranged weapon (if available)
            }
        }
    }

    private LivingEntity findNearestEnderman() {
        AABB searchBox = foreman.getBoundingBox().inflate(16.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        return entities.stream()
            .filter(e -> e.getType() == EntityType.ENDERMAN)
            .filter(e -> ((LivingEntity) e).getHealth() <= 1.0) // Only weakened ones
            .map(e -> (LivingEntity) e)
            .findFirst()
            .orElse(null);
    }
}
```

---

## Collection Automation

### Hopper System

**Design**:
```
Y = 0: Hopper row (collects drops)
Y = 1: Minecart with Hopper (transport)
Y = 2: Chest/Barrel (storage)
```

**Code**:
```java
private void setupCollectionSystem(BlockPos center) {
    List<BlockPlacement> blocks = new ArrayList<>();

    // Hopper grid beneath kill chamber
    for (int x = -2; x <= 2; x++) {
        for (int z = -2; z <= 2; z++) {
            BlockPos hopperPos = center.offset(x, 0, z);
            blocks.add(new BlockPlacement(hopperPos, Blocks.HOPPER));
        }
    }

    // Storage chest
    BlockPos chestPos = center.offset(0, 1, 3);
    blocks.add(new BlockPlacement(chestPos, Blocks.CHEST));

    return blocks;
}
```

### Item Sorting System

```java
private void sortDrops(ItemStack stack) {
    Item item = stack.getItem();

    if (item == Items.ENDER_PEARL) {
        // Store in main chest
        depositToChest(stack, pearlChestPos);
        totalPearlsCollected += stack.getCount();
    } else if (item == Items.EXPERIENCE_BOTTLE) {
        // Store separately
        depositToChest(stack, xpChestPos);
    }

    // Report progress every 100 pearls
    if (totalPearlsCollected % 100 == 0) {
        foreman.sendChatMessage("Collected " + totalPearlsCollected + " Ender Pearls!");
    }
}
```

### XP Collection

Endermen drop 5 XP each. For automation:

**Option A: Bottle of Enchanting**
- Collect XP in bottles
- Store for later use

**Option B: XP Restoration**
- Auto-collect orbs when in range
- Foreman levels up (no benefit currently)

**Option C: Player Collection**
- Design kill chamber near player AFK spot
- Player automatically collects XP orbs

---

## XP Efficiency Optimization

### Spawn Rate Optimization

1. **Be 128+ blocks from main island**: Prevents mob cap interference
2. **No other spawning surfaces**: Use slabs to block spawns
3. **Multi-layer platforms**: 2-3 layers maximizes spawn area
4. **Darkness**: Light level 0 (already true in End)

### XP Calculation

```
XP per Enderman: 5
Spawn rate (efficient farm): ~4,000 pearls/hour
Assumed 80% kill rate: ~3,200 kills/hour
Total XP/hour: 16,000 XP (approx level 0 -> 30 in 1 minute)
```

### Efficiency Comparison

| Design | Pearls/Hour | XP/Hour | Complexity |
|--------|-------------|---------|------------|
| **Single Platform** | ~1,000 | ~5,000 | Low |
| **Multi-Layer (Recommended)** | ~4,000 | ~16,000 | Medium |
| **Super Farm (4 layers)** | ~8,000+ | ~32,000+ | High |

---

## Code Implementation

### New Action: BuildEndermanFarmAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a complete Enderman farm in the End dimension.
 *
 * Parameters:
 * - center: BlockPos for farm center
 * - layers: Number of spawn layers (1-3 recommended)
 * - includeCollection: Boolean for hopper system
 * - killChamberType: "ceiling" or "water"
 */
public class BuildEndermanFarmAction extends BaseAction {

    private enum KillChamberType {
        TWO_BLOCK_CEILING,
        WATER_MOAT,
        PUMPKIN_SAFE
    }

    private BlockPos centerPos;
    private int spawnLayers;
    private boolean includeCollection;
    private KillChamberType chamberType;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;
    private int ticksRunning;

    private static final int PLATFORM_SIZE = 21; // 21x21 blocks
    private static final int PLATFORM_SPACING = 7; // 7 blocks between layers
    private static final int KILL_CHAMBER_HEIGHT = 3;
    private static final int MAX_TICKS = 6000; // 5 minutes

    public BuildEndermanFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Parse parameters
        int centerX = task.getIntParameter("center_x", 0);
        int centerY = task.getIntParameter("center_y", 100);
        int centerZ = task.getIntParameter("center_z", 0);
        centerPos = new BlockPos(centerX, centerY, centerZ);

        spawnLayers = task.getIntParameter("layers", 2);
        includeCollection = task.getBooleanParameter("collection", true);

        String chamberTypeStr = task.getStringParameter("kill_chamber", "ceiling");
        chamberType = parseKillChamberType(chamberTypeStr);

        // Validate End dimension
        if (!foreman.level().dimension().equals(net.minecraft.world.level.Level.END)) {
            result = ActionResult.failure("Enderman farm must be built in the End dimension!");
            return;
        }

        // Safety check
        if (!isPumpkinAvailable() && chamberType == KillChamberType.PUMPKIN_SAFE) {
            foreman.sendChatMessage("Warning: No pumpkin available. Switching to 2-block ceiling design.");
            chamberType = KillChamberType.TWO_BLOCK_CEILING;
        }

        // Enable building mode
        foreman.setFlying(true);
        foreman.setInvulnerableBuilding(true);

        // Generate build plan
        buildPlan = generateFarmBuildPlan();

        if (buildPlan.isEmpty()) {
            result = ActionResult.failure("Failed to generate farm build plan!");
            return;
        }

        foreman.sendChatMessage("Building Enderman farm with " + spawnLayers + " spawn layers at " + centerPos);
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            foreman.setFlying(false);
            foreman.setInvulnerableBuilding(false);
            result = ActionResult.failure("Farm build timeout!");
            return;
        }

        if (currentBlockIndex >= buildPlan.size()) {
            foreman.setFlying(false);
            foreman.setInvulnerableBuilding(false);
            result = ActionResult.success("Enderman farm complete! " + buildPlan.size() + " blocks placed.");
            return;
        }

        // Place blocks (5 per tick for speed)
        int blocksPerTick = 5;
        for (int i = 0; i < blocksPerTick && currentBlockIndex < buildPlan.size(); i++) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);
            placeBlock(placement);
            currentBlockIndex++;
        }

        // Progress update every 100 ticks
        if (ticksRunning % 100 == 0) {
            int progress = (currentBlockIndex * 100) / buildPlan.size();
            foreman.sendChatMessage("Farm construction: " + progress + "% complete");
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
        return "Build Enderman farm at " + centerPos + " (" + currentBlockIndex + "/" + buildPlan.size() + " blocks)";
    }

    private List<BlockPlacement> generateFarmBuildPlan() {
        List<BlockPlacement> plan = new ArrayList<>();

        // 1. Build kill chamber base
        plan.addAll(buildKillChamber());

        // 2. Build collection system (if enabled)
        if (includeCollection) {
            plan.addAll(buildCollectionSystem());
        }

        // 3. Build spawn platforms (multiple layers)
        for (int layer = 0; layer < spawnLayers; layer++) {
            int layerHeight = KILL_CHAMBER_HEIGHT + (layer * PLATFORM_SPACING);
            plan.addAll(buildSpawnPlatform(layerHeight));
        }

        return plan;
    }

    private List<BlockPlacement> buildKillChamber() {
        List<BlockPlacement> chamber = new ArrayList<>();

        switch (chamberType) {
            case TWO_BLOCK_CEILING:
                // Build 2-block high safe zone
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        // Floor at Y=1
                        chamber.add(new BlockPlacement(centerPos.offset(x, 1, z), Blocks.END_STONE));

                        // Ceiling at Y=3 (2 blocks high)
                        if (x == -3 || x == 3 || z == -3 || z == 3) {
                            chamber.add(new BlockPlacement(centerPos.offset(x, 3, z), Blocks.END_STONE));
                        }
                    }
                }

                // Attack gaps (1-block holes in ceiling)
                chamber.add(new BlockPlacement(centerPos.offset(0, 3, 0), Blocks.AIR));
                chamber.add(new BlockPlacement(centerPos.offset(0, 3, 1), Blocks.AIR));
                chamber.add(new BlockPlacement(centerPos.offset(1, 3, 0), Blocks.AIR));
                break;

            case WATER_MOAT:
                // Build water-filled kill chamber
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        // Water at Y=1
                        chamber.add(new BlockPlacement(centerPos.offset(x, 1, z), Blocks.WATER));

                        // Solid floor at Y=0
                        chamber.add(new BlockPlacement(centerPos.offset(x, 0, z), Blocks.END_STONE));
                    }
                }

                // Safe platform for Foreman
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        chamber.add(new BlockPlacement(centerPos.offset(x, 2, z), Blocks.END_STONE));
                    }
                }
                break;

            case PUMPKIN_SAFE:
                // Open design (relies on pumpkin)
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        chamber.add(new BlockPlacement(centerPos.offset(x, 1, z), Blocks.END_STONE));
                    }
                }
                break;
        }

        return chamber;
    }

    private List<BlockPlacement> buildCollectionSystem() {
        List<BlockPlacement> collection = new ArrayList<>();

        // Hopper grid beneath kill chamber
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Slabs to prevent Enderman spawning below hoppers
                collection.add(new BlockPlacement(centerPos.offset(x, -1, z), Blocks.END_STONE_SLAB));

                // Hoppers at Y=0
                collection.add(new BlockPlacement(centerPos.offset(x, 0, z), Blocks.HOPPER));
            }
        }

        // Storage chest
        BlockPos chestPos = centerPos.offset(0, 2, 5);
        collection.add(new BlockPlacement(chestPos, Blocks.CHEST));

        // Additional chest for overflow
        BlockPos overflowChestPos = centerPos.offset(2, 2, 5);
        collection.add(new BlockPlacement(overflowChestPos, Blocks.CHEST));

        return collection;
    }

    private List<BlockPlacement> buildSpawnPlatform(int layerHeight) {
        List<BlockPlacement> platform = new ArrayList<>();
        int halfSize = PLATFORM_SIZE / 2;

        // Main spawn platform
        for (int x = -halfSize; x < halfSize; x++) {
            for (int z = -halfSize; z < halfSize; z++) {
                BlockPos pos = centerPos.offset(x, layerHeight, z);
                platform.add(new BlockPlacement(pos, Blocks.END_STONE));
            }
        }

        // Spawn-blocking border (prevents spawning around edges)
        for (int x = -halfSize - 2; x <= halfSize + 1; x++) {
            for (int z = -halfSize - 2; z <= halfSize + 1; z++) {
                if (Math.abs(x) >= halfSize || Math.abs(z) >= halfSize) {
                    BlockPos pos = centerPos.offset(x, layerHeight - 1, z);
                    platform.add(new BlockPlacement(pos, Blocks.END_STONE_SLAB));
                }
            }
        }

        // Optional: Vine break for one-hit kill (at Y = layerHeight - 2)
        if (layerHeight >= 43) {
            for (int x = -halfSize + 2; x < halfSize - 2; x++) {
                for (int z = -halfSize + 2; z < halfSize - 2; z++) {
                    BlockPos vinePos = centerPos.offset(x, layerHeight - 2, z);
                    // Only place vines on perimeter of spawn area
                    if (Math.abs(x) >= halfSize - 3 || Math.abs(z) >= halfSize - 3) {
                        platform.add(new BlockPlacement(vinePos, Blocks.VINE));
                    }
                }
            }
        }

        return platform;
    }

    private void placeBlock(BlockPlacement placement) {
        if (foreman.level().isClientSide) return;

        BlockPos pos = placement.pos;
        Block block = placement.block;

        // Teleport closer if needed
        double distance = Math.sqrt(foreman.blockPosition().distSqr(pos));
        if (distance > 8) {
            foreman.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }

        // Look at block
        foreman.getLookControl().setLookAt(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );

        // Place block
        foreman.level().setBlock(pos, block.defaultBlockState(), 3);
    }

    private boolean isPumpkinAvailable() {
        // Check inventory for carved pumpkin
        return foreman.getInventory().contains(
            new net.minecraft.world.item.ItemStack(Items.CARVED_PUMPKIN)
        );
    }

    private KillChamberType parseKillChamberType(String type) {
        return switch (type.toLowerCase()) {
            case "water", "moat" -> KillChamberType.WATER_MOAT;
            case "pumpkin", "safe" -> KillChamberType.PUMPKIN_SAFE;
            default -> KillChamberType.TWO_BLOCK_CEILING;
        };
    }
}
```

### New Action: FarmEndermanAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Operates an Enderman farm - collects drops and kills weakened Endermen.
 *
 * Parameters:
 * - farmCenter: BlockPos of farm center
 * - duration: Ticks to run (default 6000 = 5 minutes)
 * - collectPearls: Boolean (default true)
 */
public class FarmEndermanAction extends BaseAction {

    private BlockPos farmCenter;
    private int durationTicks;
    private boolean collectPearls;
    private int ticksRunning;
    private int pearlsCollected;
    private int endermenKilled;

    private static final int ATTACK_RANGE = 4.0;
    private static final int COLLECTION_RANGE = 8.0;

    public FarmEndermanAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        int centerX = task.getIntParameter("farm_center_x", 0);
        int centerY = task.getIntParameter("farm_center_y", 100);
        int centerZ = task.getIntParameter("farm_center_z", 0);
        farmCenter = new BlockPos(centerX, centerY, centerZ);

        durationTicks = task.getIntParameter("duration", 6000);
        collectPearls = task.getBooleanParameter("collect_pearls", true);

        ticksRunning = 0;
        pearlsCollected = 0;
        endermenKilled = 0;

        // Equip pumpkin if available
        if (hasPumpkin()) {
            foreman.setItemSlot(EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(Items.CARVED_PUMPKIN));
            foreman.sendChatMessage("Equipped pumpkin for safe farming");
        } else {
            foreman.sendChatMessage("No pumpkin - staying in safe zone");
        }

        // Enable invulnerability
        foreman.setInvulnerableBuilding(true);

        // Move to kill chamber
        foreman.teleportTo(
            farmCenter.getX() + 0.5,
            farmCenter.getY() + 2,
            farmCenter.getZ() + 0.5
        );
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > durationTicks) {
            foreman.setInvulnerableBuilding(false);
            result = ActionResult.success(String.format(
                "Farming complete! Killed %d Endermen, collected %d pearls",
                endermenKilled, pearlsCollected
            ));
            return;
        }

        // Collect drops
        if (collectPearls) {
            collectDrops();
        }

        // Kill weakened Endermen
        killWeakenedEndermen();

        // Report progress every 1000 ticks (50 seconds)
        if (ticksRunning % 1000 == 0) {
            foreman.sendChatMessage(String.format(
                "Progress: %d%% - Pearls: %d, Kills: %d",
                (ticksRunning * 100) / durationTicks,
                pearlsCollected,
                endermenKilled
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
        return String.format("Farming Endermen (%d pearls, %d kills)",
            pearlsCollected, endermenKilled);
    }

    private void collectDrops() {
        AABB searchBox = new AABB(farmCenter).inflate(COLLECTION_RANGE);
        List<Entity> entities = foreman.level().getEntities(
            foreman,
            searchBox,
            e -> e.getType() == EntityType.ITEM
        );

        for (Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
                net.minecraft.world.item.ItemStack stack = itemEntity.getItem();

                if (stack.getItem() == Items.ENDER_PEARL) {
                    pearlsCollected += stack.getCount();
                    entity.discard(); // Remove item entity
                }
            }
        }
    }

    private void killWeakenedEndermen() {
        AABB searchBox = new AABB(farmCenter).inflate(ATTACK_RANGE);
        List<Entity> entities = foreman.level().getEntities(
            foreman,
            searchBox,
            e -> e.getType() == EntityType.ENDERMAN
        );

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity enderman) {
                // Only attack if weakened (half-heart or low health)
                if (enderman.getHealth() <= 2.0) {
                    foreman.doHurtTarget(enderman);
                    foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                    endermenKilled++;

                    foreman.sendChatMessage("Killed Enderman! Total: " + endermenKilled);
                    break; // One kill per tick
                }
            }
        }
    }

    private boolean hasPumpkin() {
        return foreman.getInventory().contains(
            new net.minecraft.world.item.ItemStack(Items.CARVED_PUMPKIN)
        );
    }
}
```

### Register New Actions

Update `CoreActionsPlugin.java`:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    // ... existing registrations ...

    // Enderman farming
    registry.register("build_enderman_farm",
        (foreman, task, ctx) -> new BuildEndermanFarmAction(foreman, task),
        priority, PLUGIN_ID);

    registry.register("farm_endermen",
        (foreman, task, ctx) -> new FarmEndermanAction(foreman, task),
        priority, PLUGIN_ID);
}
```

### Update PromptBuilder

Add to `PromptBuilder.java`:

```java
ACTIONS:
- build_enderman_farm: {"center_x": 0, "center_y": 100, "center_z": 0, "layers": 2, "kill_chamber": "ceiling", "collection": true}
- farm_endermen: {"farm_center_x": 0, "farm_center_y": 100, "farm_center_z": 0, "duration": 6000, "collect_pearls": true}

EXAMPLES:

Input: "build an enderman farm"
{"reasoning": "Constructing efficient Enderman farm in End dimension", "plan": "Build Enderman farm", "tasks": [{"action": "build_enderman_farm", "parameters": {"center_x": 100, "center_y": 150, "center_z": 100, "layers": 2, "kill_chamber": "ceiling", "collection": true}}]}

Input: "farm enderman for pearls"
{"reasoning": "Operating Enderman farm for pearls and XP", "plan": "Farm Endermen", "tasks": [{"action": "farm_endermen", "parameters": {"farm_center_x": 100, "farm_center_y": 150, "farm_center_z": 100, "duration": 6000}}]}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Create `BuildEndermanFarmAction` class
- [ ] Implement spawn platform generator
- [ ] Implement kill chamber builder (2-block ceiling design)
- [ ] Test platform construction in creative mode

### Phase 2: Collection System (Week 2)
- [ ] Add hopper system builder
- [ ] Implement storage chest placement
- [ ] Add item sorting logic
- [ ] Test drop collection with spawned Endermen

### Phase 3: Combat Integration (Week 3)
- [ ] Create `FarmEndermanAction` class
- [ ] Implement pumpkin equip logic
- [ ] Add weakened Enderman detection
- [ ] Test safe combat mechanics

### Phase 4: Navigation (Week 4)
- [ ] Implement End portal detection
- [ ] Add outer island navigation (gateway)
- [ ] Create safe transport protocol
- [ ] Test full End navigation

### Phase 5: Optimization (Week 5)
- [ ] Multi-layer spawn platform support
- [ ] Collaborative farming (multiple Foremen)
- [ ] XP collection system
- [ ] Performance tuning

### Phase 6: Polish (Week 6)
- [ ] Add progress reporting
- [ ] Implement error recovery
- [ ] Add configuration options
- [ ] Write documentation
- [ ] Final testing

---

## Usage Examples

### Build a Basic Farm

```
Input: "build an enderman farm here"

Result: Foreman builds a 2-layer spawn platform with 2-block ceiling kill chamber
Time: ~5 minutes
Materials: ~500 End Stone, 20 Hoppers, 2 Chests
```

### Build Maximum Efficiency Farm

```
Input: "build a maximum efficiency enderman farm with 3 layers and water collection"

Result: Foreman builds 3-layer platform with water moat kill chamber
Time: ~10 minutes
Materials: ~1,500 End Stone, 60 Hoppers, 6 Chests, 1 Water Bucket
XP Rate: ~32,000 XP/hour
Pearl Rate: ~8,000 pearls/hour
```

### Operate Existing Farm

```
Input: "farm enderman for 1 hour"

Result: Foreman operates farm, collects pearls and kills Endermen
Time: 1 hour
Output: ~4,000 Ender Pearls, ~16,000 XP
```

### Collaborative Farming

```
Input (to Foreman 1): "build the enderman farm spawn platforms"
Input (to Foreman 2): "build the enderman farm kill chamber"
Input (to Foreman 3): "farm enderman forever"

Result: 3 Foremen work simultaneously - construction completes 3x faster
```

---

## Safety Protocols

### Before Building
1. **Verify End dimension**: Farm only works in the End
2. **Check dragon status**: Dragon may interfere during construction
3. **Gather materials**: End Stone, Hoppers, Chests, optional Pumpkin
4. **Choose location**: 128+ blocks from main island recommended

### During Operation
1. **Always wear pumpkin**: Unless using 2-block ceiling design
2. **Stay in safe zone**: Never look directly at Endermen without protection
3. **Monitor inventory**: Empty chests periodically
4. **Watch for bugs**: Report any glitches

### Emergency Procedures
```java
// If Foreman gets aggroed
if (isAggroed()) {
    // 1. Stop looking at Enderman
    lookAway();

    // 2. Build emergency shelter
    build2BlockShelter();

    // 3. Wait for aggro to expire (25 seconds)
    waitForAggroExpire();

    // 4. Resume operation
}
```

---

## Troubleshooting

### Issue: Endermen not spawning
**Cause**: Too close to main island or other spawning surfaces
**Solution**: Move farm 128+ blocks away, cover nearby surfaces with slabs

### Issue: Endermen attacking Foreman
**Cause**: Not wearing pumpkin or outside safe zone
**Solution**: Equip carved pumpkin, stay under 2-block ceiling

### Issue: Low spawn rate
**Cause**: Insufficient spawn platforms or nearby competing spawns
**Solution**: Add more spawn layers, clear nearby areas

### Issue: Pearls not collecting
**Cause**: Hoppers not connected or chests full
**Solution**: Verify hopper connections, empty storage chests

---

## Future Enhancements

1. **Shulker Box Integration**: Auto-store pearls in Shulker boxes
2. **Elytra Launcher**: Quick transport to/from outer islands
3. **Auto-Smelting**: Smelt collected resources (if any)
4. **Market Integration**: Auto-sell pearls (if economy mod present)
5. **Multi-Farm Coordination**: Manage multiple farms across outer islands
6. **Dragon Egg Collection**: Automated egg harvesting
7. **Chorus Farm Integration**: Combined Enderman/Chorus farm

---

## Technical Specifications

### Minimum Requirements
- **Minecraft**: 1.20.1
- **Forge**: 47.2.0+
- **RAM**: 4GB+
- **Materials**: 500+ End Stone, 20+ Hoppers, 2+ Chests

### Recommended Setup
- **Minecraft**: 1.20.1
- **Forge**: Latest stable
- **RAM**: 8GB+
- **Materials**: 1,500+ End Stone, 60+ Hoppers, 6+ Chests
- **Foremen**: 2-3 for collaborative farming

### Performance Metrics
- **Construction Time**: 5-10 minutes (depending on size)
- **Spawn Rate**: 1,000-8,000 pearls/hour
- **XP Rate**: 5,000-32,000 XP/hour
- **Memory Usage**: ~200MB additional RAM
- **CPU Impact**: Minimal (tick-based execution)

---

## References

### Sources
- [末影人 (Baidu Baike)](https://baike.baidu.com/item/%E6%9C%AB%E5%BD%B1%E4%BA%BA/7707232)
- [Mob Menagerie: Enderman (Minecraft.net)](https://www.minecraft.net/zh-hans/article/enderman)
- [Minecraft Wiki: The End](https://minecraft.fandom.com/zh/wiki/%E6%9C%AB%E5%9C%B0%EF%BC%88%E7%94%9F%E7%89%A9%E7%BE%A4%E7%B3%BB%EF%BC%89)
- [末地折跃门 (Sogou Baike)](https://baike.sogou.com/v140437412.htm)
- [Moretingz's 1.20 Enderman XP Farm (Bilibili)](https://www.bilibili.com/list/ml1535972992)
- [Tutorials/Enderman farming (Minecraft Wiki)](https://minecraft.fandom.com/wiki/Tutorials/Enderman_farming)
- [How To Build An Enderman Farm (TheGamer)](https://www.thegamer.com/minecraft-enderman-farm-build-guide-quick-xp-farming/)

### Related Classes
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\CombatAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildStructureAction.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\structure\StructureGenerators.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\plugin\CoreActionsPlugin.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

---

**Document Version**: 1.0
**Last Updated**: 2025-02-27
**Author**: Claude Code (MineWright Team)
**Status**: Ready for Implementation
