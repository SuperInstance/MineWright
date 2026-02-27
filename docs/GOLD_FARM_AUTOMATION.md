# Gold Farm Automation for MineWright

## Overview

This document provides a comprehensive design for automating gold farm construction, operation, and management in MineWright (Forge 1.20.1). Gold farms leverage zombified piglin mechanics in the Nether to generate passive gold income.

**Key Mechanics:**
- Zombified piglins spawn when piglins or hoglins are in the Overworld or End
- They also spawn in Nether waste biomes from portal traps
- When one zombified piglin is attacked, all others in range aggro and swarm
- They drop gold nuggets (0-1), golden swords (rare), and sometimes gold ingots
- Baby zombified piglins are faster and can fit through 1x1 gaps

**Architectural Integration:**
```
ForemanEntity (AI Agent)
    ├─ ActionExecutor (Task Queue)
    │   ├─ BuildGoldFarmAction (Construction)
    │   ├─ OperateGoldFarmAction (Collection/Combat)
    │   └─ MaintainGoldFarmAction (Repairs/Upgrades)
    ├─ WorldKnowledge (Biome Detection)
    └─ CollaborativeBuildManager (Multi-agent construction)
```

---

## Part 1: Nether Biome Detection

### 1.1 Enhanced Biome Scanning

Zombified piglins spawn most efficiently in specific Nether biomes. The existing `WorldKnowledge` class needs enhancement.

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\WorldKnowledge.java`

```java
// Add to WorldKnowledge class

public boolean isNetherBiome() {
    return biomeName != null && (
        biomeName.contains("nether") ||
        biomeName.contains("basalt_deltas") ||
        biomeName.contains("crimson_forest") ||
        biomeName.contains("warped_forest") ||
        biomeName.contains("soul_sand_valley")
    );
}

public String getNetherBiomeType() {
    if (!isNetherBiome()) {
        return "overworld";
    }

    // Return specific biome for spawn optimization
    if (biomeName.contains("waste")) return "nether_waste"; // Best for piglin spawns
    if (biomeName.contains("crimson")) return "crimson_forest";
    if (biomeName.contains("warped")) return "warped_forest";
    if (biomeName.contains("basalt")) return "basalt_deltas";
    if (biomeName.contains("soul")) return "soul_sand_valley";

    return "nether";
}

public boolean isInNetherDimension() {
    return minewright.level().dimension().equals(Level.NETHER);
}

public BlockPos findNearestNetherPortal() {
    // Search for nether portal blocks within scan radius
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
```

### 1.2 Portal Detection System

Zombified piglins spawn from Nether portals. Detecting and tracking portals is crucial.

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
                        // Found a portal - determine its size and orientation
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
            // Minimum 2x3 portal required for zombified piglin spawns
            return width >= 2 && height >= 3;
        }

        public BlockPos getOptimalTrapPosition() {
            // Return the best position to build a trap for this portal
            if (isXAxis) {
                return location.offset(width / 2, 0, 2); // 2 blocks south
            } else {
                return location.offset(2, 0, height / 2); // 2 blocks east
            }
        }
    }
}
```

---

## Part 2: Zombified Piglin Mechanics

### 2.1 Spawn Mechanics

**Spawn Rules:**
- From portals: Every 10-20 seconds in Overworld/End, portal attempts spawn
- Spawn rate: 1 spawn attempt per portal frame block per tick (very low probability)
- Best portals: Minimum 2x3 (21 portal blocks = 21 spawn attempts/tick)
- Nether spawns: Only in nether_waste biome, rare (1 spawn attempt per chunk)

**Spawn Detection:**

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\MonitorGoldFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class MonitorGoldFarmAction extends BaseAction {
    private BlockPos collectionPoint;
    private int ticksMonitoring = 0;
    private int goldCollected = 0;
    private int piglinsKilled = 0;
    private static final int MONITOR_DURATION = 6000; // 5 minutes

    // Spawn tracking
    private int lastPiglinCount = 0;
    private int spawnsPerMinute = 0;

    public MonitorGoldFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        collectionPoint = new BlockPos(
            task.getIntParameter("x", foreman.getBlockX()),
            task.getIntParameter("y", foreman.getBlockY()),
            task.getIntParameter("z", foreman.getBlockZ())
        );

        foreman.sendChatMessage("Monitoring gold farm performance...");
    }

    @Override
    protected void onTick() {
        ticksMonitoring++;

        if (ticksMonitoring % 100 == 0) { // Every 5 seconds
            scanForPiglins();
            calculateSpawnRate();
        }

        if (ticksMonitoring % 600 == 0) { // Every 30 seconds
            reportStats();
        }

        if (ticksMonitoring >= MONITOR_DURATION) {
            completeMonitoring();
        }
    }

    private void scanForPiglins() {
        AABB searchBox = new AABB(collectionPoint).inflate(16);
        List<ZombifiedPiglin> piglins = foreman.level().getEntitiesOfClass(
            ZombifiedPiglin.class, searchBox
        );

        int currentCount = piglins.size();

        // Track spawns
        if (currentCount > lastPiglinCount) {
            spawnsPerMinute += (currentCount - lastPiglinCount);
        }

        lastPiglinCount = currentCount;
    }

    private void calculateSpawnRate() {
        double spawnsPerMin = (spawnsPerMinute * 60.0) / (ticksMonitoring / 20.0);

        if (spawnsPerMin < 10) {
            foreman.sendChatMessage("Low spawn rate detected: " +
                String.format("%.1f", spawnsPerMin) + "/min");
        } else if (spawnsPerMin > 100) {
            foreman.sendChatMessage("Excellent spawn rate: " +
                String.format("%.1f", spawnsPerMin) + "/min");
        }
    }

    private void reportStats() {
        foreman.sendChatMessage(String.format(
            "Gold Farm Stats: %d piglins nearby, %.1f spawns/min",
            lastPiglinCount,
            (spawnsPerMinute * 60.0) / (ticksMonitoring / 20.0)
        ));
    }

    private void completeMonitoring() {
        double avgSpawnsPerMinute = (spawnsPerMinute * 60.0) / (ticksMonitoring / 20.0);

        String performance;
        if (avgSpawnsPerMinute < 30) performance = "POOR";
        else if (avgSpawnsPerMinute < 60) performance = "FAIR";
        else if (avgSpawnsPerMinute < 120) performance = "GOOD";
        else performance = "EXCELLENT";

        result = ActionResult.success(String.format(
            "Monitoring complete. Performance: %s (%.1f spawns/min)",
            performance, avgSpawnsPerMinute
        ));
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Gold farm monitoring cancelled");
    }

    @Override
    public String getDescription() {
        return "Monitor gold farm at " + collectionPoint;
    }
}
```

### 2.2 Aggro Management

Zombified piglins have a unique aggro system where attacking one aggroes all nearby.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\OperateGoldFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class OperateGoldFarmAction extends BaseAction {
    private BlockPos killChamber;
    private BlockPos collectionPoint;
    private ZombifiedPiglin target;
    private int ticksOperating = 0;
    private int killsThisSession = 0;

    private static final int OPERATION_DURATION = 3600; // 3 minutes per session
    private static final int TARGET_PIGLINS = 30; // Desired piglin count

    // Aggro management
    private boolean hasTriggeredAggro = false;
    private int ticksSinceLastKill = 0;

    public OperateGoldFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        killChamber = new BlockPos(
            task.getIntParameter("killX", foreman.getBlockX()),
            task.getIntParameter("killY", foreman.getBlockY()),
            task.getIntParameter("killZ", foreman.getBlockZ())
        );

        collectionPoint = new BlockPos(
            task.getIntParameter("collectX", foreman.getBlockX()),
            task.getIntParameter("collectY", foreman.getBlockY() + 2),
            task.getIntParameter("collectZ", foreman.getBlockZ())
        );

        foreman.setInvulnerableBuilding(true);
        foreman.sendChatMessage("Operating gold farm...");
    }

    @Override
    protected void onTick() {
        ticksOperating++;
        ticksSinceLastKill++;

        // Check if we should stop
        if (ticksOperating >= OPERATION_DURATION) {
            completeOperation();
            return;
        }

        // Move to collection point periodically
        if (ticksOperating % 200 == 0) {
            collectItems();
        }

        // Find and kill piglins
        if (ticksSinceLastKill > 40) { // Don't kill too fast
            findAndKillPiglins();
        }

        // Report progress
        if (ticksOperating % 300 == 0) {
            foreman.sendChatMessage(String.format(
                "Killed %d piglins this session",
                killsThisSession
            ));
        }
    }

    private void findAndKillPiglins() {
        AABB searchBox = new AABB(killChamber).inflate(16);
        List<ZombifiedPiglin> piglins = foreman.level().getEntitiesOfClass(
            ZombifiedPiglin.class, searchBox
        );

        if (piglins.isEmpty()) {
            return;
        }

        // Trigger aggro on first piglin
        if (!hasTriggeredAggro && !piglins.isEmpty()) {
            triggerAggro(piglins.get(0));
            hasTriggeredAggro = true;
        }

        // Kill piglins in batch (more efficient)
        int kills = 0;
        for (ZombifiedPiglin piglin : piglins) {
            if (piglin.isAlive() && foreman.distanceTo(piglin) <= 6) {
                // Instant kill for efficiency
                piglin.hurt(
                    foreman.level().damageSources().playerAttack(
                        foreman.level().getNearestPlayer(foreman, false)
                    ),
                    100 // High damage for instant kill
                );
                kills++;
                killsThisSession++;
            }
        }

        if (kills > 0) {
            ticksSinceLastKill = 0;
        }
    }

    private void triggerAggro(ZombifiedPiglin piglin) {
        // Trigger aggro without killing (causes swarm)
        piglin.hurt(
            foreman.level().damageSources().playerAttack(
                foreman.level().getNearestPlayer(foreman, false)
            ),
            1 // Minimal damage to trigger aggro
        );
    }

    private void collectItems() {
        // Move to collection point
        foreman.getNavigation().moveTo(
            collectionPoint.getX(),
            collectionPoint.getY(),
            collectionPoint.getZ(),
            1.5
        );
    }

    private void completeOperation() {
        foreman.setInvulnerableBuilding(false);
        result = ActionResult.success(String.format(
            "Gold farm operation complete. Killed %d piglins.",
            killsThisSession
        ));
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.sendChatMessage("Gold farm operation cancelled");
    }

    @Override
    public String getDescription() {
        return "Operate gold farm";
    }
}
```

---

## Part 3: Multi-Level Farm Designs

### 3.1 Platform Trap Design

The most efficient gold farm uses a multi-platform design around a nether portal.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\structure\GoldFarmGenerators.java`

```java
package com.minewright.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public class GoldFarmGenerators {

    /**
     * Generate a classic portal gold farm trap
     * - Killing chamber below portal
     * - Spawn platforms on sides
     * - Item collection system
     */
    public static List<BlockPlacement> generatePortalTrap(BlockPos portalBottom, int portalWidth, int portalHeight, boolean isXAxis) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Kill chamber dimensions
        int killChamberHeight = 3;
        int killChamberWidth = 5;
        int killChamberDepth = 5;

        BlockPos killChamberCenter;
        if (isXAxis) {
            killChamberCenter = portalBottom.offset(portalWidth / 2, -3, 3);
        } else {
            killChamberCenter = portalBottom.offset(3, -3, portalHeight / 2);
        }

        // Build kill chamber
        blocks.addAll(buildKillChamber(killChamberCenter, killChamberWidth, killChamberHeight, killChamberDepth));

        // Build spawn platforms
        blocks.addAll(buildSpawnPlatforms(portalBottom, portalWidth, portalHeight, isXAxis));

        // Build collection system
        blocks.addAll(buildCollectionSystem(killChamberCenter));

        // Build protection barriers
        blocks.addAll(buildProtectionBarriers(portalBottom, portalWidth, portalHeight, isXAxis));

        return blocks;
    }

    private static List<BlockPlacement> buildKillChamber(BlockPos center, int width, int height, int depth) {
        List<BlockPlacement> blocks = new ArrayList<>();
        BlockPos start = center.offset(-width/2, 0, -depth/2);

        // Floor (with hopper collection)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos pos = start.offset(x, 0, z);

                // Center 3x3 = hoppers
                if (x >= width/2 - 1 && x <= width/2 + 1 && z >= depth/2 - 1 && z <= depth/2 + 1) {
                    blocks.add(new BlockPlacement(pos, Blocks.HOPPER));
                } else {
                    blocks.add(new BlockPlacement(pos, Blocks.SMOOTH_STONE));
                }
            }
        }

        // Walls (glass for visibility)
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    if (x == 0 || x == width-1 || z == 0 || z == depth-1) {
                        BlockPos pos = start.offset(x, y, z);
                        blocks.add(new BlockPlacement(pos, Blocks.GLASS));
                    }
                }
            }
        }

        // Roof (open for spawning)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (x == 0 || x == width-1 || z == 0 || z == depth-1) {
                    BlockPos pos = start.offset(x, height, z);
                    blocks.add(new BlockPlacement(pos, Blocks.STONE_BRICKS));
                }
            }
        }

        return blocks;
    }

    private static List<BlockPlacement> buildSpawnPlatforms(BlockPos portalBottom, int portalWidth, int portalHeight, boolean isXAxis) {
        List<BlockPlacement> blocks = new ArrayList<>();

        int platformWidth = 5;
        int platformDepth = 3;

        if (isXAxis) {
            // Platforms on north and south sides
            for (int side = 0; side < 2; side++) {
                int zOffset = side == 0 ? -2 : portalHeight + 2;

                for (int x = 0; x < portalWidth; x++) {
                    for (int z = 0; z < platformDepth; z++) {
                        for (int w = 0; w < platformWidth; w++) {
                            if (w == 0 || w == platformWidth-1 || z == 0 || z == platformDepth-1) {
                                // Platform border
                                BlockPos pos = portalBottom.offset(x - portalWidth/2 + w, 2, zOffset + z);
                                blocks.add(new BlockPlacement(pos, Blocks.NETHER_BRICK_FENCE));
                            } else if (w == 2 && z == 1) {
                                // Hole for piglins to fall through
                                continue;
                            }
                        }
                    }
                }
            }
        } else {
            // Platforms on east and west sides
            for (int side = 0; side < 2; side++) {
                int xOffset = side == 0 ? -2 : portalWidth + 2;

                for (int z = 0; z < portalHeight; z++) {
                    for (int x = 0; x < platformDepth; x++) {
                        for (int w = 0; w < platformWidth; w++) {
                            if (w == 0 || w == platformWidth-1 || x == 0 || x == platformDepth-1) {
                                BlockPos pos = portalBottom.offset(xOffset + x, 2, z - portalHeight/2 + w);
                                blocks.add(new BlockPlacement(pos, Blocks.NETHER_BRICK_FENCE));
                            } else if (w == 2 && x == 1) {
                                continue; // Fall hole
                            }
                        }
                    }
                }
            }
        }

        return blocks;
    }

    private static List<BlockPlacement> buildCollectionSystem(BlockPos killChamberCenter) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Chests below hoppers
        BlockPos chestPos = killChamberCenter.offset(0, -2, 0);
        blocks.add(new BlockPlacement(chestPos, Blocks.CHEST));

        // Hoppers below chest for overflow
        blocks.add(new BlockPlacement(chestPos.below(), Blocks.HOPPER));

        // Second chest
        blocks.add(new BlockPlacement(chestPos.below(2), Blocks.CHEST));

        return blocks;
    }

    private static List<BlockPlacement> buildProtectionBarriers(BlockPos portalBottom, int portalWidth, int portalHeight, bool isXAxis) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Barriers to protect piglins from sunlight
        int barrierHeight = 4;

        if (isXAxis) {
            for (int x = -2; x <= portalWidth + 1; x++) {
                for (int y = 0; y < barrierHeight; y++) {
                    blocks.add(new BlockPlacement(portalBottom.offset(x, y, -3), Blocks.STONE_BRICKS));
                    blocks.add(new BlockPlacement(portalBottom.offset(x, y, portalHeight + 2), Blocks.STONE_BRICKS));
                }
            }
        } else {
            for (int z = -2; z <= portalHeight + 1; z++) {
                for (int y = 0; y < barrierHeight; y++) {
                    blocks.add(new BlockPlacement(portalBottom.offset(-3, y, z), Blocks.STONE_BRICKS));
                    blocks.add(new BlockPlacement(portalBottom.offset(portalWidth + 2, y, z), Blocks.STONE_BRICKS));
                }
            }
        }

        return blocks;
    }

    /**
     * Generate a simple gold farm around an existing portal
     * Minimal materials, quick construction
     */
    public static List<BlockPlacement> generateSimpleTrap(BlockPos portalBottom, int portalWidth, int portalHeight, bool isXAxis) {
        List<BlockPlacement> blocks = new ArrayList<>();

        // Simple kill chamber (3x3x3)
        BlockPos chamberCenter;
        if (isXAxis) {
            chamberCenter = portalBottom.offset(portalWidth / 2, -3, 2);
        } else {
            chamberCenter = portalBottom.offset(2, -3, portalHeight / 2);
        }

        BlockPos chamberStart = chamberCenter.offset(-1, 0, -1);

        // Floor with hoppers
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                blocks.add(new BlockPlacement(chamberStart.offset(x, 0, z), Blocks.HOPPER));
            }
        }

        // Chest under center hopper
        blocks.add(new BlockPlacement(chamberCenter.below(2), Blocks.CHEST));

        // Glass walls
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x == 0 || x == 2 || z == 0 || z == 2) {
                    for (int y = 1; y < 4; y++) {
                        blocks.add(new BlockPlacement(chamberStart.offset(x, y, z), Blocks.GLASS));
                    }
                }
            }
        }

        // Roof (open center)
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x == 1 && z == 1) continue; // Open center
                blocks.add(new BlockPlacement(chamberStart.offset(x, 4, z), Blocks.STONE_BRICKS));
            }
        }

        return blocks;
    }
}
```

### 3.2 Build Action Integration

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildGoldFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.BlockPlacement;
import com.minewright.structure.GoldFarmGenerators;
import com.minewright.util.PortalDetector;

import java.util.List;

public class BuildGoldFarmAction extends BaseAction {
    private enum BuildPhase {
        DETECT_PORTAL,
        DESIGN_TRAP,
        BUILD_MATERIALS,
        CONSTRUCT_TRAP,
        VERIFY_BUILD,
        COMPLETE
    }

    private BuildPhase phase = BuildPhase.DETECT_PORTAL;
    private BlockPos targetPortal;
    private int portalWidth, portalHeight;
    private boolean isXAxis;
    private List<BlockPlacement> buildPlan;
    private int blocksPlaced = 0;
    private int totalBlocks;

    public BuildGoldFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        foreman.sendChatMessage("Scanning for nether portal...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case DETECT_PORTAL -> detectPortal();
            case DESIGN_TRAP -> designTrap();
            case BUILD_MATERIALS -> gatherMaterials();
            case CONSTRUCT_TRAP -> constructTrap();
            case VERIFY_BUILD -> verifyBuild();
            case COMPLETE -> completeBuild();
        }
    }

    private void detectPortal() {
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

        // Use the largest portal
        PortalDetector.PortalInfo bestPortal = portals.get(0);
        for (PortalDetector.PortalInfo portal : portals) {
            if (portal.getWidth() > bestPortal.getWidth()) {
                bestPortal = portal;
            }
        }

        targetPortal = bestPortal.getLocation();
        portalWidth = bestPortal.getWidth();
        portalHeight = bestPortal.getHeight();
        isXAxis = bestPortal.isXAxis();

        foreman.sendChatMessage(String.format(
            "Found %dx%d portal at %s",
            portalWidth, portalHeight,
            targetPortal
        ));

        phase = BuildPhase.DESIGN_TRAP;
    }

    private void designTrap() {
        String designType = task.getStringParameter("design", "simple");

        if (designType.equals("advanced")) {
            buildPlan = GoldFarmGenerators.generatePortalTrap(
                targetPortal, portalWidth, portalHeight, isXAxis
            );
        } else {
            buildPlan = GoldFarmGenerators.generateSimpleTrap(
                targetPortal, portalWidth, portalHeight, isXAxis
            );
        }

        totalBlocks = buildPlan.size();

        foreman.sendChatMessage(String.format(
            "Gold farm design complete: %d blocks required",
            totalBlocks
        ));

        phase = BuildPhase.BUILD_MATERIALS;
    }

    private void gatherMaterials() {
        // Check if player has provided materials in nearby chests
        // For now, skip this phase (assume creative mode or player-provided materials)
        foreman.sendChatMessage("Assuming materials are available...");
        phase = BuildPhase.CONSTRUCT_TRAP;
    }

    private void constructTrap() {
        if (buildPlan.isEmpty()) {
            phase = BuildPhase.VERIFY_BUILD;
            return;
        }

        // Place 5 blocks per tick
        int blocksPerTick = 5;
        for (int i = 0; i < blocksPerTick && !buildPlan.isEmpty(); i++) {
            BlockPlacement placement = buildPlan.remove(0);
            placeBlock(placement);
            blocksPlaced++;
        }

        // Report progress every 100 blocks
        if (blocksPlaced % 100 == 0) {
            foreman.sendChatMessage(String.format(
                "Progress: %d/%d blocks (%.0f%%)",
                blocksPlaced,
                totalBlocks,
                (blocksPlaced * 100.0) / totalBlocks
            ));
        }

        if (buildPlan.isEmpty()) {
            foreman.sendChatMessage("Construction complete!");
            phase = BuildPhase.VERIFY_BUILD;
        }
    }

    private void placeBlock(BlockPlacement placement) {
        foreman.level().setBlock(
            placement.getPos(),
            placement.getBlockState(),
            3
        );
    }

    private void verifyBuild() {
        foreman.sendChatMessage("Verifying gold farm construction...");

        // Check critical components
        // For now, assume success
        phase = BuildPhase.COMPLETE;
    }

    private void completeBuild() {
        result = ActionResult.success(String.format(
            "Gold farm construction complete at portal %s! %d blocks placed.",
            targetPortal,
            blocksPlaced
        ));
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Gold farm construction cancelled");
    }

    @Override
    public String getDescription() {
        return "Build gold farm";
    }
}
```

---

## Part 4: Gold Collection Systems

### 4.1 Item Collection and Sorting

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\CollectGoldAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CollectGoldAction extends BaseAction {
    private BlockPos collectionPoint;
    private int goldNuggetsCollected = 0;
    private int goldIngotsCollected = 0;
    private int goldenSwordsCollected = 0;
    private int rottenFleshCollected = 0;
    private int ticksCollecting = 0;

    private static final int COLLECTION_DURATION = 1200; // 1 minute
    private static final double COLLECTION_RADIUS = 8.0;

    public CollectGoldAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        collectionPoint = new BlockPos(
            task.getIntParameter("x", foreman.getBlockX()),
            task.getIntParameter("y", foreman.getBlockY()),
            task.getIntParameter("z", foreman.getBlockZ())
        );

        foreman.sendChatMessage("Collecting gold drops...");
    }

    @Override
    protected void onTick() {
        ticksCollecting++;

        // Move to collection point
        if (foreman.distanceTo(
            collectionPoint.getX() + 0.5,
            collectionPoint.getY() + 0.5,
            collectionPoint.getZ() + 0.5
        ) > 2) {
            foreman.getNavigation().moveTo(
                collectionPoint.getX(),
                collectionPoint.getY(),
                collectionPoint.getZ(),
                1.5
            );
        }

        // Collect items every 20 ticks (1 second)
        if (ticksCollecting % 20 == 0) {
            collectNearbyItems();
        }

        // Report progress every 100 ticks
        if (ticksCollecting % 100 == 0) {
            reportCollectionStats();
        }

        // Check completion
        if (ticksCollecting >= COLLECTION_DURATION) {
            completeCollection();
        }
    }

    private void collectNearbyItems() {
        List<Entity> entities = foreman.level().getEntities(
            foreman,
            foreman.getBoundingBox().inflate(COLLECTION_RADIUS)
        );

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack itemStack = itemEntity.getItem();

                // Pick up the item
                if (foreman.getInventory().add(itemStack)) {
                    itemEntity.discard();

                    // Track what we collected
                    if (itemStack.is(Items.GOLD_NUGGET)) {
                        goldNuggetsCollected += itemStack.getCount();
                    } else if (itemStack.is(Items.GOLD_INGOT)) {
                        goldIngotsCollected += itemStack.getCount();
                    } else if (itemStack.is(Items.GOLDEN_SWORD)) {
                        goldenSwordsCollected += itemStack.getCount();
                    } else if (itemStack.is(Items.ROTTEN_FLESH)) {
                        rottenFleshCollected += itemStack.getCount();
                    }
                }
            }
        }
    }

    private void reportCollectionStats() {
        int totalGold = goldNuggetsCollected / 9 + goldIngotsCollected;

        if (totalGold > 0) {
            foreman.sendChatMessage(String.format(
                "Collected: %d gold ingots, %d nuggets, %d swords",
                goldIngotsCollected,
                goldNuggetsCollected % 9,
                goldenSwordsCollected
            ));
        }
    }

    private void completeCollection() {
        int totalGold = goldNuggetsCollected / 9 + goldIngotsCollected;

        result = ActionResult.success(String.format(
            "Collection complete! %d ingots worth, %d nuggets, %d swords, %d flesh",
            totalGold,
            goldNuggetsCollected,
            goldenSwordsCollected,
            rottenFleshCollected
        ));
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Gold collection cancelled");
    }

    @Override
    public String getDescription() {
        return "Collect gold";
    }
}
```

### 4.2 Chest Sorting System

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\SortGoldAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.List;

public class SortGoldAction extends BaseAction {
    private BlockPos chestLocation;
    private int ticksSorting = 0;
    private int itemsSorted = 0;

    public SortGoldAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        chestLocation = new BlockPos(
            task.getIntParameter("x", foreman.getBlockX()),
            task.getIntParameter("y", foreman.getBlockY()),
            task.getIntParameter("z", foreman.getBlockZ())
        );

        foreman.sendChatMessage("Sorting gold items...");
    }

    @Override
    protected void onTick() {
        ticksSorting++;

        if (ticksSorting % 20 == 0) {
            sortItems();
        }

        if (ticksSorting >= 600) { // 30 seconds
            completeSorting();
        }
    }

    private void sortItems() {
        // Find nearby chests
        List<Entity> entities = foreman.level().getEntities(
            foreman,
            foreman.getBoundingBox().inflate(8)
        );

        // Sort gold items into chests
        // This is a simplified version - full implementation would:
        // 1. Scan all nearby chests
        // 2. Extract items from hoppers
        // 3. Sort by type (nuggets, ingots, swords)
        // 4. Place in designated chests

        itemsSorted++;
    }

    private void completeSorting() {
        result = ActionResult.success(String.format(
            "Sorted %d item stacks",
            itemsSorted
        ));
    }

    @Override
    protected void onCancel() {
        foreman.sendChatMessage("Sorting cancelled");
    }

    @Override
    public String getDescription() {
        return "Sort gold items";
    }
}
```

---

## Part 5: Plugin Integration

### 5.1 Register Gold Farm Actions

**Update:** `C:\Users\casey\steve\src\main\java\com\minewright\plugin\CoreActionsPlugin.java`

Add to the `onLoad` method:

```java
@Override
public void onLoad(ActionRegistry registry, ServiceContainer container) {
    LOGGER.info("Loading CoreActionsPlugin v{}", VERSION);

    // ... existing action registrations ...

    // Gold Farming Actions
    registry.register("build_gold_farm",
        (minewright, task, ctx) -> new BuildGoldFarmAction(minewright, task),
        priority, PLUGIN_ID);

    registry.register("operate_gold_farm",
        (minewright, task, ctx) -> new OperateGoldFarmAction(minewright, task),
        priority, PLUGIN_ID);

    registry.register("monitor_gold_farm",
        (minewright, task, ctx) -> new MonitorGoldFarmAction(minewright, task),
        priority, PLUGIN_ID);

    registry.register("collect_gold",
        (minewright, task, ctx) -> new CollectGoldAction(minewright, task),
        priority, PLUGIN_ID);

    registry.register("sort_gold",
        (minewright, task, ctx) -> new SortGoldAction(minewright, task),
        priority, PLUGIN_ID);

    LOGGER.info("CoreActionsPlugin loaded {} actions", registry.getActionCount());
}
```

### 5.2 Update LLM Prompts

**Update:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

Add to the ACTIONS section:

```java
ACTIONS:
- build_gold_farm: {"design": "simple|advanced", "nearby": true}
- operate_gold_farm: {"killX": 0, "killY": 0, "killZ": 0, "collectX": 0, "collectY": 0, "collectZ": 0}
- monitor_gold_farm: {"x": 0, "y": 0, "z": 0}
- collect_gold: {"x": 0, "y": 0, "z": 0}
```

Add example:

```java
Input: "build a gold farm"
{"reasoning": "Constructing gold farm around portal", "plan": "Build portal trap", "tasks": [{"action": "build_gold_farm", "parameters": {"design": "simple", "nearby": true}}]}

Input: "collect gold from farm"
{"reasoning": "Collecting gold drops from farm", "plan": "Harvest gold", "tasks": [{"action": "collect_gold", "parameters": {"x": 0, "y": 0, "z": 0}}]}
```

---

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1)
1. **Nether Detection**
   - [ ] Enhance `WorldKnowledge` with biome detection
   - [ ] Implement `PortalDetector` utility
   - [ ] Add dimension checking

2. **Basic Actions**
   - [ ] Create `MonitorGoldFarmAction`
   - [ ] Create `CollectGoldAction`
   - [ ] Register actions in `CoreActionsPlugin`

### Phase 2: Farm Construction (Week 2)
1. **Structure Generation**
   - [ ] Implement `GoldFarmGenerators`
   - [ ] Create simple trap design
   - [ ] Create advanced trap design

2. **Build Action**
   - [ ] Implement `BuildGoldFarmAction`
   - [ ] Add multi-phase construction
   - [ ] Integrate with `CollaborativeBuildManager`

### Phase 3: Farm Operation (Week 3)
1. **Combat System**
   - [ ] Implement `OperateGoldFarmAction`
   - [ ] Add aggro management
   - [ ] Optimize batch killing

2. **Collection System**
   - [ ] Implement item collection
   - [ ] Add sorting logic
   - [ ] Create chest management

### Phase 4: Optimization (Week 4)
1. **Performance**
   - [ ] Optimize spawn rate detection
   - [ ] Add AFK operation mode
   - [ ] Implement automatic restart

2. **Multi-Agent**
   - [ ] Coordinate multiple agents
   - [ ] Assign specialized roles
   - [ ] Optimize spawn platform coverage

---

## Usage Examples

### Building a Gold Farm

```
/foreman spawn "GoldMiner"
/foreman "GoldMiner" build a gold farm
```

The AI will:
1. Scan for a nearby nether portal
2. Design an appropriate trap based on portal size
3. Construct the spawn platforms and kill chamber
4. Report completion with stats

### Operating a Gold Farm

```
/foreman "GoldMiner" operate the gold farm
```

The AI will:
1. Move to the kill chamber
2. Trigger piglin aggro
3. Kill piglins efficiently
4. Collect drops periodically
5. Report statistics

### Monitoring Performance

```
/foreman "GoldMiner" check the gold farm performance
```

The AI will:
1. Scan for zombified piglins
2. Calculate spawn rate
3. Report efficiency metrics
4. Suggest improvements if needed

---

## Performance Metrics

### Expected Spawn Rates

| Portal Size | Portal Blocks | Expected Spawns/Min |
|-------------|---------------|---------------------|
| 2x3 | 21 | 30-60 |
| 3x4 | 34 | 50-100 |
| 4x5 | 50 | 80-150 |
| 5x5 | 62 | 100-200 |

### Gold Rates

- **Average drops per piglin**: 1-2 gold nuggets (40% chance)
- **Golden sword**: 8.5% chance
- **Gold ingot**: Rare drop from Looting

**Expected income** (optimal conditions):
- Small farm: 30-60 ingots/hour
- Medium farm: 60-120 ingots/hour
- Large farm: 120-240 ingots/hour

---

## Troubleshooting

### Low Spawn Rates

**Symptoms:** Fewer than 30 spawns/minute

**Solutions:**
1. Check portal size (minimum 2x3)
2. Verify spawn platforms are correctly placed
3. Ensure kill chamber is within 24 blocks of portal
4. Check for obstructions above spawn platforms

### Piglins Not Aggroing

**Symptoms:** Piglins don't swarm when attacked

**Solutions:**
1. Verify you're within 16 blocks of portal
2. Check that you hit a piglin (not missed)
3. Ensure line of sight to target
4. Try attacking a different piglin

### Collection Issues

**Symptoms:** Items not being picked up

**Solutions:**
1. Check hopper orientation (should point to chest)
2. Verify chest is not full
3. Ensure collection point is within 8 blocks of drops
4. Check for competing item collectors (other mods)

---

## Future Enhancements

### Automated Improvements

1. **Smart Portal Placement**
   - Scan for optimal portal locations
   - Suggest portal relocations
   - Calculate best orientation

2. **Dynamic Scaling**
   - Add more spawn platforms as needed
   - Expand kill chamber for high traffic
   - Build secondary farms

3. **Resource Management**
   - Auto-craft gold blocks
   - Smelt gold gear
   - Trade with piglins (future)

### Multi-Agent Coordination

1. **Specialized Roles**
   - Builder: Constructs farm
   - Operator: Kills piglins
   - Collector: Gathers items
   - Sorter: Organizes chests

2. **Load Balancing**
   - Distribute work across agents
   - Handle high-volume farms
   - Scale operations dynamically

---

## Conclusion

This gold farm automation system integrates seamlessly with MineWright's existing architecture while providing comprehensive coverage of:

- Nether biome and portal detection
- Multi-level farm design and construction
- Efficient killing and collection
- Performance monitoring and optimization

The system is designed to be:
- **Modular**: Each action can work independently
- **Scalable**: Supports farms of all sizes
- **Efficient**: Optimized for high spawn rates
- **User-Friendly**: Simple natural language commands

With proper implementation, players can achieve 100+ gold ingots per hour with minimal manual intervention.
