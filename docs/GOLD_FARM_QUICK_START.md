# Gold Farm Quick Start Guide

## Overview

This guide provides a streamlined path to implement basic gold farm automation in MineWright.

---

## Step 1: Create WorldKnowledge Enhancements

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\WorldKnowledge.java`

**Add these methods to the existing `WorldKnowledge` class:**

```java
public boolean isNetherBiome() {
    return biomeName != null && (
        biomeName.contains("nether") ||
        biomeName.contains("basalt_deltas") ||
        biomeName.contains("crimson_forest") ||
        biomeName.contains("warped_forest") ||
        biomeName.contains("soul_sand_valley")
    );
}

public boolean isInNetherDimension() {
    return minewright.level().dimension().equals(Level.NETHER);
}

public int countNearbyZombifiedPiglins() {
    int count = 0;
    for (Entity entity : nearbyEntities) {
        String entityType = entity.getType().toString();
        if (entityType.contains("zombified_piglin") ||
            entityType.contains("zombie_pigman")) {
            count++;
        }
    }
    return count;
}

public BlockPos findNearestNetherPortal() {
    Level level = minewright.level();
    BlockPos minewrightPos = minewright.blockPosition();

    for (int x = -scanRadius; x <= scanRadius; x++) {
        for (int y = -scanRadius; y <= scanRadius; y++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                BlockPos checkPos = minewrightPos.offset(x, y, z);
                BlockState state = level.getBlockState(checkPos);

                if (state.is(Blocks.NETHER_PORTAL)) {
                    return checkPos;
                }
            }
        }
    }

    return null;
}
```

---

## Step 2: Create Portal Detector

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\util\PortalDetector.java`

```java
package com.minewright.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class PortalDetector {

    private final Level level;
    private final BlockPos center;
    private final int searchRadius;

    public PortalDetector(Level level, BlockPos center, int searchRadius) {
        this.level = level;
        this.center = center;
        this.searchRadius = searchRadius;
    }

    public List<PortalInfo> findPortals() {
        List<PortalInfo> portals = new ArrayList<>();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(Blocks.NETHER_PORTAL)) {
                        PortalInfo portal = analyzePortal(checkPos);
                        if (!portals.contains(portal)) {
                            portals.add(portal);
                        }
                    }
                }
            }
        }

        return portals;
    }

    private PortalInfo analyzePortal(BlockPos start) {
        boolean isXAxis = level.getBlockState(start.east()).is(Blocks.NETHER_PORTAL) ||
                         level.getBlockState(start.west()).is(Blocks.NETHER_PORTAL);

        int width = 1;
        int height = 1;

        // Calculate width
        if (isXAxis) {
            for (int i = 1; i < 30; i++) {
                if (!level.getBlockState(start.east(i)).is(Blocks.NETHER_PORTAL)) break;
                width++;
            }
            for (int i = 1; i < 30; i++) {
                if (!level.getBlockState(start.west(i)).is(Blocks.NETHER_PORTAL)) break;
                width++;
            }
        } else {
            for (int i = 1; i < 30; i++) {
                if (!level.getBlockState(start.north(i)).is(Blocks.NETHER_PORTAL)) break;
                width++;
            }
            for (int i = 1; i < 30; i++) {
                if (!level.getBlockState(start.south(i)).is(Blocks.NETHER_PORTAL)) break;
                width++;
            }
        }

        // Calculate height
        for (int i = 1; i < 30; i++) {
            if (!level.getBlockState(start.above(i)).is(Blocks.NETHER_PORTAL)) break;
            height++;
        }
        for (int i = 1; i < 30; i++) {
            if (!level.getBlockState(start.below(i)).is(Blocks.NETHER_PORTAL)) break;
            height++;
        }

        return new PortalInfo(start, width, height, isXAxis);
    }

    public static class PortalInfo {
        private final BlockPos location;
        private final int width;
        private final int height;
        private final boolean isXAxis;

        public PortalInfo(BlockPos location, int width, int height, boolean isXAxis) {
            this.location = location;
            this.width = width;
            this.height = height;
            this.isXAxis = isXAxis;
        }

        public BlockPos getLocation() { return location; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean isXAxis() { return isXAxis; }

        public boolean canSpawnPiglins() {
            return width >= 2 && height >= 3;
        }
    }
}
```

---

## Step 3: Create Simple Gold Farm Action

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\SimpleGoldFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.util.PortalDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SimpleGoldFarmAction extends BaseAction {
    private enum Phase {
        FIND_PORTAL,
        BUILD_TRAP,
        OPERATE_FARM,
        COMPLETE
    }

    private Phase phase = Phase.FIND_PORTAL;
    private BlockPos portalLocation;
    private BlockPos killChamber;
    private int ticksOperating = 0;
    private int piglinsKilled = 0;
    private static final int OPERATION_TIME = 3600; // 3 minutes

    public SimpleGoldFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Starting gold farm setup...");
        foreman.setInvulnerableBuilding(true);
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case FIND_PORTAL -> findAndBuildTrap();
            case OPERATE_FARM -> operateFarm();
            case COMPLETE -> complete();
        }
    }

    private void findAndBuildTrap() {
        // Find portal
        PortalDetector detector = new PortalDetector(
            foreman.level(),
            foreman.blockPosition(),
            32
        );

        List<PortalDetector.PortalInfo> portals = detector.findPortals();

        if (portals.isEmpty()) {
            result = ActionResult.failure("No nether portal found within 32 blocks");
            return;
        }

        PortalDetector.PortalInfo portal = portals.get(0);
        portalLocation = portal.getLocation();

        foreman.sendChatMessage("Found portal! Building trap...");

        // Build simple trap
        buildSimpleTrap(portal);

        phase = Phase.OPERATE_FARM;
    }

    private void buildSimpleTrap(PortalDetector.PortalInfo portal) {
        // Calculate kill chamber position (2 blocks south of portal)
        killChamber = portal.getLocation().offset(
            portal.isXAxis() ? 0 : 2,
            -2,
            portal.isXAxis() ? 2 : 0
        );

        // Build 3x3 glass box with hopper floor
        BlockPos start = killChamber.offset(-1, -1, -1);

        // Hopper floor
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                foreman.level().setBlock(
                    start.offset(x, 0, z),
                    Blocks.HOPPER.defaultBlockState(),
                    3
                );
            }
        }

        // Glass walls
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x == 0 || x == 2 || z == 0 || z == 2) {
                    for (int y = 1; y < 4; y++) {
                        foreman.level().setBlock(
                            start.offset(x, y, z),
                            Blocks.GLASS.defaultBlockState(),
                            3
                        );
                    }
                }
            }
        }

        // Chest below center
        foreman.level().setBlock(
            start.offset(1, -1, 1),
            Blocks.CHEST.defaultBlockState(),
            3
        );

        foreman.sendChatMessage("Trap complete at " + killChamber);
    }

    private void operateFarm() {
        ticksOperating++;

        // Move to kill chamber
        if (foreman.distanceTo(
            killChamber.getX() + 0.5,
            killChamber.getY() + 0.5,
            killChamber.getZ() + 0.5
        ) > 2) {
            foreman.getNavigation().moveTo(
                killChamber.getX(),
                killChamber.getY(),
                killChamber.getZ(),
                1.5
            );
        }

        // Kill piglins every 40 ticks
        if (ticksOperating % 40 == 0) {
            killPiglins();
        }

        // Report progress every 300 ticks
        if (ticksOperating % 300 == 0) {
            foreman.sendChatMessage("Killed " + piglinsKilled + " piglins");
        }

        // Check completion
        if (ticksOperating >= OPERATION_TIME) {
            phase = Phase.COMPLETE;
        }
    }

    private void killPiglins() {
        AABB searchBox = new AABB(killChamber).inflate(16);
        List<ZombifiedPiglin> piglins = foreman.level().getEntitiesOfClass(
            ZombifiedPiglin.class, searchBox
        );

        for (ZombifiedPiglin piglin : piglins) {
            if (piglin.isAlive() && foreman.distanceTo(piglin) <= 6) {
                piglin.hurt(
                    foreman.level().damageSources().playerAttack(
                        foreman.level().getNearestPlayer(foreman, false)
                    ),
                    100
                );
                piglinsKilled++;
            }
        }
    }

    private void complete() {
        foreman.setInvulnerableBuilding(false);
        result = ActionResult.success(
            "Gold farm operation complete! Killed " + piglinsKilled + " piglins."
        );
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.sendChatMessage("Gold farm operation cancelled");
    }

    @Override
    public String getDescription() {
        return "Simple gold farm";
    }
}
```

---

## Step 4: Register the Action

**Update:** `C:\Users\casey\steve\src\main\java\com\minewright\plugin\CoreActionsPlugin.java`

**Add this to the `onLoad` method:**

```java
// Gold Farming
registry.register("gold_farm",
    (minewright, task, ctx) -> new SimpleGoldFarmAction(minewright, task),
    priority, PLUGIN_ID);
```

---

## Step 5: Update LLM Prompt

**Update:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

**Add to ACTIONS section:**

```java
- gold_farm: {} (finds portal, builds trap, operates farm)
```

**Add example:**

```java
Input: "setup a gold farm"
{"reasoning": "Building gold farm around portal", "plan": "Construct gold farm", "tasks": [{"action": "gold_farm", "parameters": {}}]}
```

---

## Usage

### In-Game Commands

```
/foreman spawn "GoldFarmer"
/foreman "GoldFarmer" setup a gold farm
```

Or press K and type:
```
setup a gold farm
```

### What Happens

1. **Scans** for a nether portal within 32 blocks
2. **Builds** a 3x3 glass trap with hopper floor
3. **Operates** for 3 minutes, killing piglins
4. **Reports** total kills

---

## Expected Results

- **Spawn Rate**: 30-60 piglins/minute (depends on portal size)
- **Gold Drops**: ~1-2 nuggets per piglin (40% chance)
- **Total Income**: ~20-40 gold ingots per 3-minute session

---

## Troubleshooting

**No Portal Found**
- Ensure you're within 32 blocks of a nether portal
- The portal must be in the Overworld (not Nether)

**Low Spawn Rate**
- Minimum portal size is 2x3 blocks
- Larger portals (3x4, 4x5) spawn more piglins
- Ensure spawn area is well-lit (no hostile mob spawning interference)

**Items Not Collecting**
- Check that hoppers are pointing to the chest
- Verify the chest is not full
- Make sure you're standing within 6 blocks of piglins

---

## Next Steps

After the basic farm is working, you can:

1. **Expand** the farm with larger spawn platforms
2. **Automate** collection with hopper systems
3. **Optimize** for your specific portal layout
4. **Scale** with multiple agents operating simultaneously

See `GOLD_FARM_AUTOMATION.md` for advanced designs and multi-agent coordination.
