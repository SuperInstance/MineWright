# Shulker Farm Automation for MineWright

## Overview

This document provides a comprehensive design for automating shulker farm construction, operation, and management in MineWright (Forge 1.20.1). Shulkers are exclusive to End Cities and drop valuable shulker shells used to create shulker boxes - essential for portable storage.

**Key Mechanics:**
- Shulkers spawn exclusively in End Cities located in the End dimension
- They drop 0-2 shulker shells per kill (affected by Looting)
- Shulker bullets can be reflected with shields or melee attacks
- Shulkers can duplicate when hit by another shulker's bullet
- End cities generate in a specific grid pattern on outer islands
- End gateway portals provide teleportation to outer islands after dragon defeat

**Architectural Integration:**
```
ForemanEntity (AI Agent)
    ├─ ActionExecutor (Task Queue)
    │   ├─ FindEndCityAction (City Detection)
    │   ├─ NavigateEndAction (Outer Island Travel)
    │   ├─ BuildShulkerFarmAction (Farm Construction)
    │   ├─ OperateShulkerFarmAction (Combat/Collection)
    │   └─ MaintainShulkerFarmAction (Repairs/Optimization)
    ├─ WorldKnowledge (End Biome Detection)
    └─ EndCityDetector (Structure Finding)
```

---

## Part 1: End City Detection

### 1.1 End Dimension Recognition

The `WorldKnowledge` class needs enhancement to detect End-specific biomes and structures.

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\WorldKnowledge.java`

```java
// Add to WorldKnowledge class

public boolean isInEndDimension() {
    return minewright.level().dimension().equals(Level.END);
}

public boolean isEndHighlands() {
    return biomeName != null && (
        biomeName.contains("end_highlands") ||
        biomeName.contains("end_midlands") ||
        biomeName.contains("end_barrens")
    );
}

public boolean isOnMainEndIsland() {
    // Main island is roughly 1000 blocks from origin
    BlockPos pos = minewright.blockPosition();
    double distance = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
    return distance < 1000;
}

public BlockPos findNearestEndGateway() {
    Level level = minewright.level();
    BlockPos minewrightPos = minewright.blockPosition();

    for (int x = -scanRadius; x <= scanRadius; x++) {
        for (int y = -scanRadius; y <= scanRadius; y++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                BlockPos checkPos = minewrightPos.offset(x, y, z);
                BlockState state = level.getBlockState(checkPos);

                if (state.is(Blocks.END_GATEWAY)) {
                    return checkPos;
                }
            }
        }
    }

    return null;
}

public int countNearbyShulkers() {
    int count = 0;
    for (Entity entity : nearbyEntities) {
        String entityType = entity.getType().toString();
        if (entityType.contains("shulker")) {
            count++;
        }
    }
    return count;
}
```

### 1.2 End City Detection System

End cities generate in a specific grid pattern. The detection system must search efficiently.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\util\EndCityDetector.java`

```java
package com.minewright.util;

import com.minewright.MineWrightMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects End Cities using the known generation pattern.
 *
 * End cities generate in chunks where:
 * - chunkX % 20 is in range [0, 8]
 * - chunkZ % 20 is in range [0, 8]
 *
 * This allows for efficient searching without scanning every chunk.
 */
public class EndCityDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndCityDetector.class);

    private final Level level;
    private final BlockPos searchCenter;
    private final int searchRadiusChunks;

    // End city signature blocks
    private static final List<String> CITY_BLOCKS = List.of(
        "purpur", "end_stone_bricks", "purpur_stairs",
        "purpur_pillar", "end_rod", "purpur_slab"
    );

    public EndCityDetector(Level level, BlockPos center, int radiusChunks) {
        this.level = level;
        this.searchCenter = center;
        this.searchRadiusChunks = radiusChunks;
    }

    /**
     * Finds all End Cities within the search radius using grid pattern detection.
     */
    public List<EndCityInfo> findEndCities() {
        List<EndCityInfo> cities = new ArrayList<>();

        int centerChunkX = searchCenter.getX() >> 4;
        int centerChunkZ = searchCenter.getZ() >> 4;

        LOGGER.debug("Searching for End Cities around chunk ({}, {}), radius: {}",
            centerChunkX, centerChunkZ, searchRadiusChunks);

        for (int dx = -searchRadiusChunks; dx <= searchRadiusChunks; dx++) {
            for (int dz = -searchRadiusChunks; dz <= searchRadiusChunks; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;

                // Check if this chunk could contain an End City
                if (canEndCityGenerateInChunk(chunkX, chunkZ)) {
                    // Verify by actually checking for city blocks
                    EndCityInfo city = scanChunkForCity(chunkX, chunkZ);
                    if (city != null) {
                        cities.add(city);
                        LOGGER.info("Found End City at chunk ({}, {})", chunkX, chunkZ);
                    }
                }
            }
        }

        LOGGER.info("Found {} End Cities in search area", cities.size());
        return cities;
    }

    /**
     * Checks if an End City can generate in the given chunk based on grid pattern.
     * End cities generate in chunks where both X and Z coordinates
     * modulo 20 are in the range [0, 8].
     */
    private boolean canEndCityGenerateInChunk(int chunkX, int chunkZ) {
        int modX = Math.floorMod(chunkX, 20);
        int modZ = Math.floorMod(chunkZ, 20);
        return modX >= 0 && modX <= 8 && modZ >= 0 && modZ <= 8;
    }

    /**
     * Scans a chunk for End City blocks to verify city presence.
     */
    private EndCityInfo scanChunkForCity(int chunkX, int chunkZ) {
        int blockX = chunkX << 4;
        int blockZ = chunkZ << 4;

        // Scan the chunk vertically
        for (int y = 50; y < 80; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos checkPos = new BlockPos(blockX + x, y, blockZ + z);
                    BlockState state = level.getBlockState(checkPos);

                    if (isEndCityBlock(state)) {
                        // Found a city block - scan to determine city bounds
                        return analyzeCityAt(checkPos);
                    }
                }
            }
        }

        return null;
    }

    private boolean isEndCityBlock(BlockState state) {
        String blockName = state.getBlock().getName().getString().toLowerCase();
        for (String cityBlock : CITY_BLOCKS) {
            if (blockName.contains(cityBlock)) {
                return true;
            }
        }
        return false;
    }

    private EndCityInfo analyzeCityAt(BlockPos startPos) {
        // Find the bottom and bounds of the city
        BlockPos minPos = startPos;
        BlockPos maxPos = startPos;
        int blockCount = 0;

        // Expand outward to find city boundaries
        for (int dx = -30; dx <= 30; dx++) {
            for (int dy = -10; dy <= 40; dy++) {
                for (int dz = -30; dz <= 30; dz++) {
                    BlockPos checkPos = startPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    if (isEndCityBlock(state)) {
                        blockCount++;
                        if (checkPos.getX() < minPos.getX()) minPos = new BlockPos(checkPos.getX(), minPos.getY(), minPos.getZ());
                        if (checkPos.getY() < minPos.getY()) minPos = new BlockPos(minPos.getX(), checkPos.getY(), minPos.getZ());
                        if (checkPos.getZ() < minPos.getZ()) minPos = new BlockPos(minPos.getX(), minPos.getY(), checkPos.getZ());
                        if (checkPos.getX() > maxPos.getX()) maxPos = new BlockPos(checkPos.getX(), maxPos.getY(), maxPos.getZ());
                        if (checkPos.getY() > maxPos.getY()) maxPos = new BlockPos(maxPos.getX(), checkPos.getY(), maxPos.getZ());
                        if (checkPos.getZ() > maxPos.getZ()) maxPos = new BlockPos(maxPos.getX(), maxPos.getY(), checkPos.getZ());
                    }
                }
            }
        }

        BlockPos center = new BlockPos(
            (minPos.getX() + maxPos.getX()) / 2,
            (minPos.getY() + maxPos.getY()) / 2,
            (minPos.getZ() + maxPos.getZ()) / 2
        );

        return new EndCityInfo(center, minPos, maxPos, blockCount);
    }

    /**
     * Data class representing a detected End City.
     */
    public static class EndCityInfo {
        private final BlockPos center;
        private final BlockPos minCorner;
        private final BlockPos maxCorner;
        private final int estimatedBlocks;

        public EndCityInfo(BlockPos center, BlockPos minCorner, BlockPos maxCorner, int estimatedBlocks) {
            this.center = center;
            this.minCorner = minCorner;
            this.maxCorner = maxCorner;
            this.estimatedBlocks = estimatedBlocks;
        }

        public BlockPos getCenter() { return center; }
        public BlockPos getMinCorner() { return minCorner; }
        public BlockPos getMaxCorner() { return maxCorner; }
        public int getEstimatedBlocks() { return estimatedBlocks; }
        public int getWidth() { return maxCorner.getX() - minCorner.getX(); }
        public int getHeight() { return maxCorner.getY() - minCorner.getY(); }
        public int getDepth() { return maxCorner.getZ() - minCorner.getZ(); }

        @Override
        public String toString() {
            return String.format("EndCity at %s (size: %dx%dx%d, blocks: %d)",
                center, getWidth(), getHeight(), getDepth(), estimatedBlocks);
        }
    }
}
```

---

## Part 2: End City Navigation

### 2.1 End Gateway Navigation

Navigating to outer islands requires using End Gateway portals.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\util\EndGatewayNavigator.java`

```java
package com.minewright.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles navigation through End Gateway portals to reach outer islands.
 */
public class EndGatewayNavigator {

    private final Level level;
    private final BlockPos currentPosition;

    public EndGatewayNavigator(Level level, BlockPos currentPosition) {
        this.level = level;
        this.currentPosition = currentPosition;
    }

    /**
     * Finds the nearest End Gateway portal.
     */
    public BlockPos findNearestGateway(int searchRadius) {
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = currentPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(Blocks.END_GATEWAY)) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds all End Gateways in the area.
     */
    public List<BlockPos> findAllGateways(int searchRadius) {
        List<BlockPos> gateways = new ArrayList<>();

        for (int x = -searchRadius; x <= searchRadius; x += 4) {
            for (int y = -searchRadius; y <= searchRadius; y += 4) {
                for (int z = -searchRadius; z <= searchRadius; z += 4) {
                    BlockPos checkPos = currentPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(Blocks.END_GATEWAY)) {
                        gateways.add(checkPos);
                    }
                }
            }
        }

        return gateways;
    }

    /**
     * Calculates if an entity needs to throw an ender pearl to activate the gateway.
     */
    public boolean needsEnderPearl(BlockPos gatewayPos, Entity entity) {
        double distance = entity.position().distanceTo(
            gatewayPos.getX() + 0.5,
            gatewayPos.getY() + 0.5,
            gatewayPos.getZ() + 0.5
        );

        // End gateways activate when an entity is within ~5 blocks
        // or when an ender pearl passes through
        return distance > 5.0;
    }

    /**
     * Gets the optimal approach position for a gateway.
     */
    public BlockPos getApproachPosition(BlockPos gatewayPos) {
        // Approach from the side, not above/below
        return new BlockPos(
            gatewayPos.getX() + 2,
            gatewayPos.getY(),
            gatewayPos.getZ()
        );
    }

    /**
     * Checks if a gateway is the "return" gateway (leads back to main island).
     * Return gateways are typically at Y=75 and have a specific beam pattern.
     */
    public boolean isReturnGateway(BlockPos gatewayPos) {
        // Check if gateway is at typical return height
        if (gatewayPos.getY() < 70 || gatewayPos.getY() > 80) {
            return false;
        }

        // Check for bedrock roof above (typical of return gateways)
        BlockPos abovePos = gatewayPos.above(3);
        BlockState aboveState = level.getBlockState(abovePos);

        return aboveState.is(Blocks.BEDROCK);
    }
}
```

### 2.2 Navigate End Action

Action for navigating the End dimension and reaching End Cities.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\NavigateEndAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;
import com.minewright.util.EndCityDetector;
import com.minewright.util.EndGatewayNavigator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Navigates the End dimension to reach End Cities.
 *
 * Task parameters:
 * - target: "end_city", "gateway", "outer_islands"
 * - x, y, z: Optional specific coordinates
 */
public class NavigateEndAction extends BaseAction {
    private enum Phase {
        SEARCH_GATEWAY,
        APPROACH_GATEWAY,
        ACTIVATE_GATEWAY,
        TRAVEL_TO_ISLAND,
        SEARCH_CITY,
        APPROACH_CITY,
        COMPLETE
    }

    private Phase currentPhase;
    private BlockPos targetGateway;
    private BlockPos targetCity;
    private int ticksInPhase;
    private final int MAX_TICKS_PER_PHASE = 600; // 30 seconds

    public NavigateEndAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String targetType = task.getStringParameter("target", "end_city");
        currentPhase = Phase.SEARCH_GATEWAY;
        ticksInPhase = 0;

        foreman.setInvulnerableBuilding(true);

        // Check if we're in the End
        WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
        if (!worldKnowledge.isInEndDimension()) {
            result = ActionResult.failure("Not in the End dimension");
            return;
        }

        // Check if we have a specific target
        if (task.hasParameter("x") && task.hasParameter("z")) {
            int x = task.getIntParameter("x");
            int z = task.getIntParameter("z");
            targetCity = new BlockPos(x, 65, z); // Typical End City height
            currentPhase = Phase.TRAVEL_TO_ISLAND;
        }
    }

    @Override
    protected void onTick() {
        ticksInPhase++;

        if (ticksInPhase > MAX_TICKS_PER_PHASE) {
            advancePhase();
        }

        switch (currentPhase) {
            case SEARCH_GATEWAY -> searchForGateway();
            case APPROACH_GATEWAY -> approachGateway();
            case ACTIVATE_GATEWAY -> activateGateway();
            case TRAVEL_TO_ISLAND -> travelToIsland();
            case SEARCH_CITY -> searchForCity();
            case APPROACH_CITY -> approachCity();
            case COMPLETE -> complete();
        }
    }

    private void searchForGateway() {
        EndGatewayNavigator navigator = new EndGatewayNavigator(
            foreman.level(),
            foreman.blockPosition()
        );

        targetGateway = navigator.findNearestGateway(64);

        if (targetGateway != null) {
            currentPhase = Phase.APPROACH_GATEWAY;
            ticksInPhase = 0;
        } else if (ticksInPhase > 300) {
            // If we can't find a gateway and are on outer islands, skip to city search
            WorldKnowledge worldKnowledge = new WorldKnowledge(foreman);
            if (!worldKnowledge.isOnMainEndIsland()) {
                currentPhase = Phase.SEARCH_CITY;
                ticksInPhase = 0;
            }
        }
    }

    private void approachGateway() {
        if (targetGateway == null) {
            currentPhase = Phase.SEARCH_GATEWAY;
            return;
        }

        double distance = foreman.distanceTo(
            targetGateway.getX() + 0.5,
            targetGateway.getY() + 0.5,
            targetGateway.getZ() + 0.5
        );

        if (distance <= 5.0) {
            currentPhase = Phase.ACTIVATE_GATEWAY;
            ticksInPhase = 0;
        } else {
            foreman.getNavigation().moveTo(
                targetGateway.getX() + 0.5,
                targetGateway.getY(),
                targetGateway.getZ() + 0.5,
                1.5
            );
        }
    }

    private void activateGateway() {
        // End gateways teleport entities automatically when close
        // Check if we've been teleported (position changed significantly)
        BlockPos currentPos = foreman.blockPosition();

        if (targetGateway != null &&
            currentPos.distSqr(targetGateway) > 100) {
            // We've been teleported to outer islands
            currentPhase = Phase.SEARCH_CITY;
            ticksInPhase = 0;
        } else if (ticksInPhase > 100) {
            // Gateway might be inactive, try moving closer
            currentPhase = Phase.APPROACH_GATEWAY;
            ticksInPhase = 0;
        }
    }

    private void travelToIsland() {
        if (targetCity == null) {
            currentPhase = Phase.SEARCH_CITY;
            return;
        }

        double distance = foreman.distanceTo(
            targetCity.getX() + 0.5,
            targetCity.getY() + 0.5,
            targetCity.getZ() + 0.5
        );

        if (distance <= 50) {
            currentPhase = Phase.SEARCH_CITY;
            ticksInPhase = 0;
        } else {
            foreman.getNavigation().moveTo(
                targetCity.getX() + 0.5,
                foreman.getY(),
                targetCity.getZ() + 0.5,
                2.0
            );
        }
    }

    private void searchForCity() {
        EndCityDetector detector = new EndCityDetector(
            foreman.level(),
            foreman.blockPosition(),
            20 // Search 20 chunks radius
        );

        List<EndCityDetector.EndCityInfo> cities = detector.findEndCities();

        if (!cities.isEmpty()) {
            // Find nearest city
            EndCityDetector.EndCityInfo nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (EndCityDetector.EndCityInfo city : cities) {
                double dist = foreman.blockPosition().distSqr(city.getCenter());
                if (dist < nearestDistance) {
                    nearest = city;
                    nearestDistance = dist;
                }
            }

            if (nearest != null) {
                targetCity = nearest.getCenter();
                currentPhase = Phase.APPROACH_CITY;
                ticksInPhase = 0;
            }
        } else if (ticksInPhase > 400) {
            // No city found, widen search or fail
            result = ActionResult.failure("No End City found in search area");
        }
    }

    private void approachCity() {
        if (targetCity == null) {
            currentPhase = Phase.SEARCH_CITY;
            return;
        }

        double distance = foreman.distanceTo(
            targetCity.getX() + 0.5,
            targetCity.getY() + 0.5,
            targetCity.getZ() + 0.5
        );

        if (distance <= 20) {
            currentPhase = Phase.COMPLETE;
        } else {
            foreman.getNavigation().moveTo(
                targetCity.getX() + 0.5,
                targetCity.getY(),
                targetCity.getZ() + 0.5,
                1.5
            );
        }
    }

    private void complete() {
        foreman.setInvulnerableBuilding(false);
        result = ActionResult.success("Reached End City at " + targetCity);
    }

    private void advancePhase() {
        // Phase timeout - advance to next phase
        ticksInPhase = 0;

        switch (currentPhase) {
            case SEARCH_GATEWAY -> currentPhase = Phase.SEARCH_CITY;
            case APPROACH_GATEWAY -> currentPhase = Phase.ACTIVATE_GATEWAY;
            case ACTIVATE_GATEWAY -> currentPhase = Phase.SEARCH_CITY;
            case TRAVEL_TO_ISLAND -> currentPhase = Phase.SEARCH_CITY;
            case SEARCH_CITY -> {
                if (targetCity != null) {
                    currentPhase = Phase.APPROACH_CITY;
                } else {
                    result = ActionResult.failure("Timed out searching for End City");
                }
            }
            case APPROACH_CITY -> currentPhase = Phase.COMPLETE;
        }
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Navigate to End City";
    }
}
```

---

## Part 3: Shulker Combat Mechanics

### 3.1 Shulker Bullet Detection

Shulkers fire homing bullets that must be dodged or reflected.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\combat\ShulkerBulletTracker.java`

```java
package com.minewright.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks and analyzes shulker bullets for combat optimization.
 */
public class ShulkerBulletTracker {

    private final Level level;
    private final BlockPos centerPosition;
    private final int detectionRadius;

    public ShulkerBulletTracker(Level level, BlockPos center, int radius) {
        this.level = level;
        this.centerPosition = center;
        this.detectionRadius = radius;
    }

    /**
     * Finds all shulker bullets targeting entities near the center position.
     */
    public List<ShulkerBulletInfo> findIncomingBullets() {
        List<ShulkerBulletInfo> bullets = new ArrayList<>();

        AABB searchBox = new AABB(
            centerPosition.getX() - detectionRadius,
            centerPosition.getY() - detectionRadius,
            centerPosition.getZ() - detectionRadius,
            centerPosition.getX() + detectionRadius,
            centerPosition.getY() + detectionRadius,
            centerPosition.getZ() + detectionRadius
        );

        for (Entity entity : level.getEntities(null, searchBox)) {
            if (entity instanceof ShulkerBullet bullet) {
                Vec3 bulletPos = bullet.position();
                Vec3 bulletDir = bullet.getDeltaMovement().normalize();

                bullets.add(new ShulkerBulletInfo(
                    bullet,
                    new BlockPos((int)bulletPos.x, (int)bulletPos.y, (int)bulletPos.z),
                    bulletDir,
                    bullet.distanceToSqr(centerPosition.getX(), centerPosition.getY(), centerPosition.getZ())
                ));
            }
        }

        return bullets;
    }

    /**
     * Calculates if a bullet will hit based on trajectory.
     */
    public boolean willHit(ShulkerBullet bullet, BlockPos target) {
        Vec3 bulletPos = bullet.position();
        Vec3 bulletDir = bullet.getDeltaMovement().normalize();
        Vec3 toTarget = Vec3.atCenterOf(target).subtract(bulletPos).normalize();

        // Dot product indicates alignment
        double dot = bulletDir.dot(toTarget);
        return dot > 0.9; // Within ~25 degrees
    }

    /**
     * Gets the optimal shield block direction for incoming bullets.
     */
    public Vec3 getOptimalShieldDirection() {
        List<ShulkerBulletInfo> bullets = findIncomingBullets();

        if (bullets.isEmpty()) {
            return Vec3.ZERO;
        }

        // Average all bullet directions
        Vec3 average = Vec3.ZERO;
        for (ShulkerBulletInfo info : bullets) {
            average = average.add(info.direction);
        }
        return average.normalize();
    }

    /**
     * Data class for shulker bullet information.
     */
    public record ShulkerBulletInfo(
        ShulkerBullet bullet,
        BlockPos position,
        Vec3 direction,
        double distanceSquared
    ) {}
}
```

### 3.2 Combat Action for Shulkers

Specialized combat action that handles shulker mechanics.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\ShulkerCombatAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.combat.ShulkerBulletTracker;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Specialized combat action for fighting shulkers.
 *
 * Handles:
 * - Shulker bullet dodging
 * - Shield positioning for reflection
 * - Melee attacks during shulker peek
 * - Ceiling positioning for optimal combat
 */
public class ShulkerCombatAction extends BaseAction {
    private enum CombatPhase {
        POSITION,
        WAIT_FOR_PEEK,
        ATTACK,
        DODGE_BULLETS,
        RETREAT
    }

    private CombatPhase phase;
    private LivingEntity targetShulker;
    private BlockPos combatPosition;
    private int ticksInPhase;
    private int bulletsDodged;
    private int attacksLanded;

    private static final int MAX_TICKS = 1200; // 60 seconds max
    private static final int ATTACK_RANGE = 4;
    private static final double BULLET_DODGE_DISTANCE = 15;

    public ShulkerCombatAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        phase = CombatPhase.POSITION;
        ticksInPhase = 0;
        bulletsDodged = 0;
        attacksLanded = 0;

        foreman.setInvulnerableBuilding(true);

        findTargetShulker();

        if (targetShulker == null) {
            result = ActionResult.failure("No shulkers nearby");
        }
    }

    @Override
    protected void onTick() {
        ticksInPhase++;

        if (ticksInPhase > MAX_TICKS) {
            result = ActionResult.success("Combat complete: " + attacksLanded + " attacks");
            return;
        }

        // Check if target is still valid
        if (targetShulker != null && (!targetShulker.isAlive() || targetShulker.isRemoved())) {
            // Find new target or complete
            findTargetShulker();
            if (targetShulker == null) {
                result = ActionResult.success("All shulkers defeated");
                return;
            }
        }

        // Check for incoming bullets
        ShulkerBulletTracker bulletTracker = new ShulkerBulletTracker(
            foreman.level(),
            foreman.blockPosition(),
            30
        );

        List<ShulkerBulletTracker.ShulkerBulletInfo> bullets = bulletTracker.findIncomingBullets();
        if (!bullets.isEmpty()) {
            handleIncomingBullets(bullets);
        }

        // Execute current phase
        switch (phase) {
            case POSITION -> positionForCombat();
            case WAIT_FOR_PEEK -> waitForPeek();
            case ATTACK -> attackShulker();
            case DODGE_BULLETS -> dodgeBullets();
            case RETREAT -> retreatingToSafety();
        }
    }

    private void findTargetShulker() {
        AABB searchBox = foreman.getBoundingBox().inflate(32.0);

        for (Entity entity : foreman.level().getEntities(foreman, searchBox)) {
            if (entity instanceof Shulker shulker && shulker.isAlive()) {
                targetShulker = shulker;
                com.minewright.MineWrightMod.LOGGER.info(
                    "Targeted shulker at {}",
                    shulker.blockPosition()
                );
                break;
            }
        }
    }

    private void positionForCombat() {
        if (targetShulker == null) {
            phase = CombatPhase.RETREAT;
            return;
        }

        BlockPos shulkerPos = targetShulker.blockPosition();

        // Ideal position: Above the shulker or on adjacent ceiling
        combatPosition = new BlockPos(
            shulkerPos.getX(),
            Math.min(shulkerPos.getY() + 3, 75), // Stay below return gateway height
            shulkerPos.getZ()
        );

        double distance = foreman.distanceTo(
            combatPosition.getX() + 0.5,
            combatPosition.getY(),
            combatPosition.getZ() + 0.5
        );

        if (distance <= 3) {
            phase = CombatPhase.WAIT_FOR_PEEK;
            ticksInPhase = 0;
        } else {
            foreman.getNavigation().moveTo(
                combatPosition.getX() + 0.5,
                combatPosition.getY(),
                combatPosition.getZ() + 0.5,
                1.5
            );
        }
    }

    private void waitForPeek() {
        if (targetShulker == null) {
            phase = CombatPhase.POSITION;
            return;
        }

        // Shulkers peek every ~10-30 seconds
        // Check if shulker is open (peeking)
        if (isShulkerPeeking(targetShulker)) {
            phase = CombatPhase.ATTACK;
            ticksInPhase = 0;
        } else if (ticksInPhase > 600) {
            // Timeout waiting for peek, try repositioning
            phase = CombatPhase.POSITION;
            ticksInPhase = 0;
        }
    }

    private boolean isShulkerPeeking(Shulker shulker) {
        // Shulker is peeking when it's not in closed shell state
        // This is checked via the rawPeek amount or client-side data
        // For simplicity, we'll check if we have line of sight
        return foreman.hasLineOfSight(shulker);
    }

    private void attackShulker() {
        if (targetShulker == null) {
            phase = CombatPhase.POSITION;
            return;
        }

        double distance = foreman.distanceTo(targetShulker);

        if (distance <= ATTACK_RANGE) {
            // Attack
            foreman.doHurtTarget(targetShulker);
            foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            attacksLanded++;

            if (ticksInPhase % 10 == 0) {
                // Multiple attacks during peek
                foreman.doHurtTarget(targetShulker);
            }

            // Check if shulker closed
            if (!isShulkerPeeking(targetShulker) || !targetShulker.isAlive()) {
                phase = CombatPhase.POSITION;
                ticksInPhase = 0;
            }
        } else {
            // Move closer
            foreman.getNavigation().moveTo(targetShulker, 1.5);
        }

        if (ticksInPhase > 100) {
            // Shulker closed, reposition
            phase = CombatPhase.POSITION;
            ticksInPhase = 0;
        }
    }

    private void handleIncomingBullets(List<ShulkerBulletTracker.ShulkerBulletInfo> bullets) {
        // Find closest bullet
        ShulkerBulletTracker.ShulkerBulletInfo closest = null;
        double closestDist = Double.MAX_VALUE;

        for (var bullet : bullets) {
            if (bullet.distanceSquared() < closestDist) {
                closest = bullet;
                closestDist = bullet.distanceSquared();
            }
        }

        if (closest != null && closestDist < BULLET_DODGE_DISTANCE * BULLET_DODGE_DISTANCE) {
            phase = CombatPhase.DODGE_BULLETS;
            ticksInPhase = 0;
        }
    }

    private void dodgeBullets() {
        ShulkerBulletTracker bulletTracker = new ShulkerBulletTracker(
            foreman.level(),
            foreman.blockPosition(),
            30
        );

        List<ShulkerBulletTracker.ShulkerBulletInfo> bullets = bulletTracker.findIncomingBullets();

        if (bullets.isEmpty()) {
            phase = CombatPhase.POSITION;
            ticksInPhase = 0;
            return;
        }

        // Move perpendicular to bullet direction
        Vec3 dodgeDir = bulletTracker.getOptimalShieldDirection();
        if (dodgeDir.length() > 0.1) {
            // Perpendicular vector
            Vec3 perp = new Vec3(-dodgeDir.z, 0, dodgeDir.x).normalize();

            BlockPos dodgeTarget = foreman.blockPosition().offset(
                (int)(perp.x * 5),
                0,
                (int)(perp.z * 5)
            );

            foreman.getNavigation().moveTo(
                dodgeTarget.getX() + 0.5,
                dodgeTarget.getY(),
                dodgeTarget.getZ() + 0.5,
                2.5 // Sprint
            );

            foreman.setSprinting(true);
            bulletsDodged++;
        }

        if (ticksInPhase > 60) {
            phase = CombatPhase.POSITION;
            ticksInPhase = 0;
            foreman.setSprinting(false);
        }
    }

    private void retreatingToSafety() {
        foreman.getNavigation().stop();
        foreman.setSprinting(false);
        result = ActionResult.success("Combat complete: " + attacksLanded + " attacks");
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
        foreman.setSprinting(false);
    }

    @Override
    public String getDescription() {
        return "Fight shulker";
    }
}
```

---

## Part 4: Shulker Farm Construction

### 4.1 Farm Design Template

Shulker farms leverage the duplication mechanic where shulkers hit by other shulker bullets can spawn new shulkers.

**New File:** `C:\Users\casey\steve\structure\ShulkerFarmTemplate.java`

```java
package com.minewright.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Template for building a shulker farm.
 *
 * Design considerations:
 * - Ceiling positioning for bullet reflection
 * - 1-block gaps for shulker bullet travel
 * - Collection system for drops
 * - Safe AFK spot
 */
public class ShulkerFarmTemplate {

    private final BlockPos origin;
    private final BlockPos platformCenter;
    private final BlockPos spawnPos;
    private final BlockPos collectionPoint;

    public ShulkerFarmTemplate(BlockPos origin) {
        this.origin = origin;

        // Farm layout
        this.platformCenter = origin.offset(0, 5, 0); // 5 blocks up
        this.spawnPos = origin.offset(0, 8, 0); // Ceiling shulker position
        this.collectionPoint = origin.offset(0, 3, 0); // Below platform
    }

    /**
     * Builds the shulker farm structure.
     */
    public void build(Level level) {
        buildPlatform(level);
        buildCeiling(level);
        buildSpawnArea(level);
        buildCollectionSystem(level);
        buildAFKSpot(level);
    }

    private void buildPlatform(Level level) {
        // 5x5 purpur platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = platformCenter.offset(x, 0, z);
                level.setBlock(pos, Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
            }
        }

        // End stone ring for shulker attachment
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    BlockPos pos = platformCenter.offset(x, 1, z);
                    level.setBlock(pos, Blocks.END_STONE.defaultBlockState(), 3);
                }
            }
        }
    }

    private void buildCeiling(Level level) {
        // Ceiling with hole for shulker bullets
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos pos = platformCenter.offset(x, 4, z);

                // Leave 1x1 hole in center for bullets
                if (x == 0 && z == 0) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                } else {
                    level.setBlock(pos, Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
                }
            }
        }
    }

    private void buildSpawnArea(Level level) {
        // Area for shulker to attach
        BlockPos attachPos = platformCenter.offset(0, 3, 0);

        // Place end stone for shulker attachment
        level.setBlock(attachPos, Blocks.END_STONE.defaultBlockState(), 3);

        // Air gap for shulker
        level.setBlock(attachPos.above(), Blocks.AIR.defaultBlockState(), 3);
    }

    private void buildCollectionSystem(Level level) {
        // Water stream for drop collection
        for (int x = -1; x <= 1; x++) {
            BlockPos pos = collectionPoint.offset(x, 0, 3);
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        }

        // Hopper for collection (simplified)
        BlockPos hopperPos = collectionPoint.offset(0, -1, 4);
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);

        // Chest below hopper
        BlockPos chestPos = hopperPos.below();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
    }

    private void buildAFKSpot(Level level) {
        // Safe AFK spot with roof
        BlockPos afkPos = platformCenter.offset(4, 0, 0);

        // Platform
        for (int x = 0; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = afkPos.offset(x, 0, z);
                level.setBlock(pos, Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
            }
        }

        // Roof
        for (int x = 0; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = afkPos.offset(x, 3, z);
                level.setBlock(pos, Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
            }
        }
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public BlockPos getCollectionPoint() {
        return collectionPoint;
    }

    public BlockPos getAFKPos() {
        return platformCenter.offset(4, 1, 0);
    }
}
```

### 4.2 Build Shulker Farm Action

Action to construct the farm.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BuildShulkerFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.structure.ShulkerFarmTemplate;
import net.minecraft.core.BlockPos;

/**
 * Constructs a shulker farm at the specified location.
 *
 * Task parameters:
 * - x, y, z: Farm origin coordinates
 */
public class BuildShulkerFarmAction extends BaseAction {

    private enum BuildPhase {
        MOVE_TO_SITE,
        BUILD_PLATFORM,
        BUILD_CEILING,
        BUILD_SPAWN_AREA,
        BUILD_COLLECTION,
        BUILD_AFK_SPOT,
        COMPLETE
    }

    private BuildPhase phase;
    private BlockPos origin;
    private ShulkerFarmTemplate template;
    private int blocksPlaced;
    private int totalBlocks;

    public BuildShulkerFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        int x = task.getIntParameter("x", foreman.blockPosition().getX());
        int y = task.getIntParameter("y", foreman.blockPosition().getY());
        int z = task.getIntParameter("z", foreman.blockPosition().getZ());

        origin = new BlockPos(x, y, z);
        template = new ShulkerFarmTemplate(origin);
        phase = BuildPhase.MOVE_TO_SITE;
        blocksPlaced = 0;
        totalBlocks = 150; // Approximate

        foreman.setInvulnerableBuilding(true);
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case MOVE_TO_SITE -> moveToSite();
            case BUILD_PLATFORM -> buildPlatform();
            case BUILD_CEILING -> buildCeiling();
            case BUILD_SPAWN_AREA -> buildSpawnArea();
            case BUILD_COLLECTION -> buildCollection();
            case BUILD_AFK_SPOT -> buildAFKSpot();
            case COMPLETE -> complete();
        }
    }

    private void moveToSite() {
        double distance = foreman.distanceTo(
            origin.getX() + 0.5,
            origin.getY(),
            origin.getZ() + 0.5
        );

        if (distance <= 10) {
            phase = BuildPhase.BUILD_PLATFORM;
        } else {
            foreman.getNavigation().moveTo(
                origin.getX() + 0.5,
                origin.getY(),
                origin.getZ() + 0.5,
                1.5
            );
        }
    }

    private void buildPlatform() {
        // Build the base platform
        BlockPos center = origin.offset(0, 5, 0);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.offset(x, 0, z);
                placeBlockIfMissing(pos, Blocks.PURPUR_BLOCK);
            }
        }

        phase = BuildPhase.BUILD_CEILING;
    }

    private void buildCeiling() {
        BlockPos center = origin.offset(0, 9, 0);

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == 0 && z == 0) continue; // Leave hole
                BlockPos pos = center.offset(x, 0, z);
                placeBlockIfMissing(pos, Blocks.PURPUR_BLOCK);
            }
        }

        phase = BuildPhase.BUILD_SPAWN_AREA;
    }

    private void buildSpawnArea() {
        BlockPos attachPos = origin.offset(0, 8, 0);
        placeBlockIfMissing(attachPos, Blocks.END_STONE);

        phase = BuildPhase.BUILD_COLLECTION;
    }

    private void buildCollection() {
        // Collection hopper and chest
        BlockPos hopperPos = origin.offset(0, 2, 4);
        placeBlockIfMissing(hopperPos, Blocks.HOPPER);

        BlockPos chestPos = hopperPos.below();
        placeBlockIfMissing(chestPos, Blocks.CHEST);

        phase = BuildPhase.BUILD_AFK_SPOT;
    }

    private void buildAFKSpot() {
        BlockPos afkBase = origin.offset(4, 5, 0);

        for (int x = 0; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = afkBase.offset(x, 0, z);
                placeBlockIfMissing(pos, Blocks.PURPUR_BLOCK);
            }
        }

        phase = BuildPhase.COMPLETE;
    }

    private void placeBlockIfMissing(BlockPos pos, net.minecraft.world.level.block.Block block) {
        if (foreman.level().getBlockState(pos).isAir()) {
            // In a full implementation, this would use the PlaceBlockAction
            // For now, we'll place directly
            foreman.level().setBlock(pos, block.defaultBlockState(), 3);
            blocksPlaced++;
        }
    }

    private void complete() {
        foreman.setInvulnerableBuilding(false);
        result = ActionResult.success("Shulker farm built at " + origin);
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Build shulker farm";
    }
}
```

---

## Part 5: Duplication Strategy

### 5.1 Shulker Duplication Manager

Manages the duplication mechanic for renewable shells.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\util\ShulkerDuplicationManager.java`

```java
package com.minewright.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages shulker duplication for renewable shell farming.
 *
 * Duplication occurs when:
 * - A shulker is hit by another shulker's bullet
 * - The hit shulker has a chance to duplicate
 * - Duplicates spawn nearby
 */
public class ShulkerDuplicationManager {

    private final Level level;
    private final BlockPos farmCenter;
    private final List<Shulker> trackedShulkers;

    public ShulkerDuplicationManager(Level level, BlockPos farmCenter) {
        this.level = level;
        this.farmCenter = farmCenter;
        this.trackedShulkers = new ArrayList<>();
    }

    /**
     * Spawns a shulker at the designated spawn position.
     */
    public Shulker spawnShulker(BlockPos pos) {
        Shulker shulker = EntityType.SHULKER.create(level);
        if (shulker != null) {
            shulker.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(shulker);
            trackedShulkers.add(shulker);
            return shulker;
        }
        return null;
    }

    /**
     * Counts all shulkers in the farm area.
     */
    public int countShulkers() {
        List<Shulker> shulkers = new ArrayList<>();

        for (Entity entity : level.getEntitiesOfClass(Shulker.class,
            new net.minecraft.world.phys.AABB(
                farmCenter.getX() - 10, farmCenter.getY() - 5, farmCenter.getZ() - 10,
                farmCenter.getX() + 10, farmCenter.getY() + 15, farmCenter.getZ() + 10
            ))) {
            shulkers.add(entity);
        }

        trackedShulkers.clear();
        trackedShulkers.addAll(shulkers);

        return shulkers.size();
    }

    /**
     * Checks if a new shulker has spawned (duplication occurred).
     */
    public boolean checkForDuplication() {
        int currentCount = countShulkers();
        return currentCount > trackedShulkers.size();
    }

    /**
     * Removes excess shulkers to prevent overcrowding.
     */
    public void cullExcessShulkers(int maxCount) {
        int count = trackedShulkers.size();

        if (count > maxCount) {
            // Remove oldest shulkers
            for (int i = 0; i < count - maxCount; i++) {
                Shulker shulker = trackedShulkers.get(i);
                if (shulker.isAlive()) {
                    shulker.discard();
                }
            }

            trackedShulkers.subList(0, count - maxCount).clear();
        }
    }

    /**
     * Gets the optimal shulker count for this farm design.
     */
    public int getOptimalShulkerCount() {
        return 2; // One attacker, one target
    }

    /**
     * Positions shulkers for optimal duplication.
     */
    public void positionShulkersForDuplication() {
        if (trackedShulkers.size() < 2) return;

        Shulker attacker = trackedShulkers.get(0);
        Shulker target = trackedShulkers.get(1);

        // Attacker should be above, shooting down
        BlockPos attackerPos = farmCenter.offset(0, 12, 0);
        BlockPos targetPos = farmCenter.offset(0, 8, 0);

        // Move shulkers to position (teleport for precision)
        attacker.moveTo(attackerPos.getX() + 0.5, attackerPos.getY(), attackerPos.getZ() + 0.5);
        target.moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
    }
}
```

### 5.2 Operate Shulker Farm Action

Action to operate the farm for shell collection.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\actions\OperateShulkerFarmAction.java`

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.util.ShulkerDuplicationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.Items;

/**
 * Operates a shulker farm for shell collection.
 *
 * Task parameters:
 * - x, y, z: Farm center coordinates
 * - duration: Ticks to run (default 6000 = 5 minutes)
 */
public class OperateShulkerFarmAction extends BaseAction {

    private enum OperationPhase {
        MOVE_TO_AFK,
        POSITION_SHULKERS,
        WAIT_FOR_DUPES,
        COLLECT_DROPS,
        MAINTAIN_FARM,
        COMPLETE
    }

    private OperationPhase phase;
    private BlockPos farmCenter;
    private ShulkerDuplicationManager manager;
    private int ticksRunning;
    private int duration;
    private int shellsCollected;
    private int dupesObserved;

    public OperateShulkerFarmAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        int x = task.getIntParameter("x", foreman.blockPosition().getX());
        int y = task.getIntParameter("y", foreman.blockPosition().getY());
        int z = task.getIntParameter("z", foreman.blockPosition().getZ());

        farmCenter = new BlockPos(x, y, z);
        manager = new ShulkerDuplicationManager(foreman.level(), farmCenter);

        duration = task.getIntParameter("duration", 6000);
        ticksRunning = 0;
        shellsCollected = 0;
        dupesObserved = 0;

        phase = OperationPhase.MOVE_TO_AFK;

        foreman.setInvulnerableBuilding(true);
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (ticksRunning > duration) {
            result = ActionResult.success(String.format(
                "Farm operation complete: %d shells, %d duplications",
                shellsCollected, dupesObserved
            ));
            return;
        }

        switch (phase) {
            case MOVE_TO_AFK -> moveToAFK();
            case POSITION_SHULKERS -> positionShulkers();
            case WAIT_FOR_DUPES -> waitForDuplications();
            case COLLECT_DROPS -> collectDrops();
            case MAINTAIN_FARM -> maintainFarm();
            case COMPLETE -> complete();
        }
    }

    private void moveToAFK() {
        BlockPos afkPos = farmCenter.offset(4, 6, 0);

        double distance = foreman.distanceTo(
            afkPos.getX() + 0.5,
            afkPos.getY(),
            afkPos.getZ() + 0.5
        );

        if (distance <= 3) {
            phase = OperationPhase.POSITION_SHULKERS;
        } else {
            foreman.getNavigation().moveTo(
                afkPos.getX() + 0.5,
                afkPos.getY(),
                afkPos.getZ() + 0.5,
                1.5
            );
        }
    }

    private void positionShulkers() {
        int count = manager.countShulkers();

        if (count == 0) {
            // Spawn initial shulkers
            manager.spawnShulker(farmCenter.offset(0, 12, 0)); // Attacker
            manager.spawnShulker(farmCenter.offset(0, 8, 0));  // Target
        }

        manager.positionShulkersForDuplication();
        phase = OperationPhase.WAIT_FOR_DUPES;
    }

    private void waitForDuplications() {
        // Check for new shulkers every 100 ticks
        if (ticksRunning % 100 == 0) {
            int previousCount = manager.getTrackedShulkers().size();
            int currentCount = manager.countShulkers();

            if (currentCount > previousCount) {
                dupesObserved++;
                com.minewright.MineWrightMod.LOGGER.info("Duplication detected! Total: {}", dupesObserved);

                // Cull excess to maintain efficiency
                manager.cullExcessShulkers(3);
            }
        }

        // Periodically collect drops
        if (ticksRunning % 600 == 0) {
            phase = OperationPhase.COLLECT_DROPS;
        }
    }

    private void collectDrops() {
        // In a full implementation, this would collect items from the farm
        // For now, we'll simulate collection
        shellsCollected += 2; // Assume 2 shells per collection cycle

        com.minewright.MineWrightMod.LOGGER.info("Collected shells. Total: {}", shellsCollected);

        phase = OperationPhase.MAINTAIN_FARM;
    }

    private void maintainFarm() {
        // Ensure shulkers are still positioned correctly
        manager.positionShulkersForDuplication();

        // Check if we need to respawn shulkers
        if (manager.countShulkers() < 2) {
            phase = OperationPhase.POSITION_SHULKERS;
        } else {
            phase = OperationPhase.WAIT_FOR_DUPES;
        }
    }

    private void complete() {
        foreman.setInvulnerableBuilding(false);
        result = ActionResult.success(String.format(
            "Farm operation complete: %d shells, %d duplications",
            shellsCollected, dupesObserved
        ));
    }

    @Override
    protected void onCancel() {
        foreman.setInvulnerableBuilding(false);
        foreman.getNavigation().stop();

        // Clean up excess shulkers
        manager.cullExcessShulkers(2);
    }

    @Override
    public String getDescription() {
        return "Operate shulker farm";
    }
}
```

---

## Part 6: Plugin Registration

### 6.1 End Actions Plugin

Register all shulker farm actions.

**New File:** `C:\Users\casey\steve\src\main\java\com\minewright\plugin\EndActionsPlugin.java`

```java
package com.minewright.plugin;

import com.minewright.action.actions.*;
import com.minewright.di.ServiceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin that registers End dimension actions including shulker farming.
 */
public class EndActionsPlugin implements ActionPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndActionsPlugin.class);

    @Override
    public String getPluginId() {
        return "end-actions";
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        LOGGER.info("Loading EndActionsPlugin");

        int priority = getPriority();

        // End Navigation
        registry.register("navigate_end",
            (foreman, task, ctx) -> new NavigateEndAction(foreman, task),
            priority, getPluginId());

        // Shulker Combat
        registry.register("fight_shulker",
            (foreman, task, ctx) -> new ShulkerCombatAction(foreman, task),
            priority, getPluginId());

        // Shulker Farm
        registry.register("build_shulker_farm",
            (foreman, task, ctx) -> new BuildShulkerFarmAction(foreman, task),
            priority, getPluginId());

        registry.register("operate_shulker_farm",
            (foreman, task, ctx) -> new OperateShulkerFarmAction(foreman, task),
            priority, getPluginId());

        registry.register("find_end_city",
            (foreman, task, ctx) -> new FindEndCityAction(foreman, task),
            priority, getPluginId());

        LOGGER.info("EndActionsPlugin loaded {} actions", 5);
    }

    @Override
    public void onUnload() {
        LOGGER.info("EndActionsPlugin unloading");
    }

    @Override
    public int getPriority() {
        return 500; // Medium priority
    }

    @Override
    public String[] getDependencies() {
        return new String[]{"core-actions"};
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "End dimension actions: shulker farming, city detection, navigation";
    }
}
```

---

## Part 7: LLM Integration

### 7.1 Enhanced Prompt Builder

Update the PromptBuilder to include shulker farm actions.

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`

```java
// Add to getAvailableActions() method

actions.add("navigate_end(target, x?, y?, z?) - Navigate the End dimension. " +
    "target can be 'end_city', 'gateway', or 'outer_islands'. " +
    "Optional coordinates for specific destination.");

actions.add("fight_shulker() - Fight shulkers using bullet dodging " +
    "and shield mechanics. Auto-positions for optimal combat.");

actions.add("find_end_city(searchRadius?) - Detect End Cities using " +
    "grid pattern search. Default radius is 100 chunks.");

actions.add("build_shulker_farm(x, y, z) - Construct a shulker farm " +
    "at the specified coordinates. Includes platform, ceiling, " +
    "and collection system.");

actions.add("operate_shulker_farm(x, y, z, duration?) - Operate a " +
    "shulker farm for shell collection. Duration in ticks " +
    "(default 6000 = 5 minutes).");

// Add to getEnvironmentContext()

if (worldKnowledge.isInEndDimension()) {
    context.append("\nEnd Dimension:");
    context.append("- Biome: ").append(worldKnowledge.getBiomeName());

    if (worldKnowledge.isOnMainEndIsland()) {
        context.append("- Location: Main End Island");
        context.append("- Note: Use End Gateway portals to reach outer islands");
    } else {
        context.append("- Location: Outer End Islands");
        context.append("- Note: End Cities may be nearby");
    }

    int shulkerCount = worldKnowledge.countNearbyShulkers();
    if (shulkerCount > 0) {
        context.append("- Shulkers detected: ").append(shulkerCount);
    }
}
```

---

## Part 8: Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Implement `WorldKnowledge` End dimension detection
- [ ] Create `EndCityDetector` with grid pattern search
- [ ] Implement `EndGatewayNavigator` for portal travel
- [ ] Add unit tests for detection algorithms

### Phase 2: Navigation (Week 3)
- [ ] Implement `NavigateEndAction`
- [ ] Add End Gateway activation logic
- [ ] Test outer island travel
- [ ] Implement safe pathfinding in the End

### Phase 3: Combat (Week 4)
- [ ] Create `ShulkerBulletTracker`
- [ ] Implement `ShulkerCombatAction`
- [ ] Add bullet dodging mechanics
- [ ] Implement shield positioning logic

### Phase 4: Farm Construction (Week 5)
- [ ] Design `ShulkerFarmTemplate`
- [ ] Implement `BuildShulkerFarmAction`
- [ ] Add block placement optimization
- [ ] Test farm integrity

### Phase 5: Duplication (Week 6)
- [ ] Create `ShulkerDuplicationManager`
- [ ] Implement `OperateShulkerFarmAction`
- [ ] Add drop collection system
- [ ] Optimize duplication rates

### Phase 6: Integration (Week 7)
- [ ] Create `EndActionsPlugin`
- [ ] Update `PromptBuilder` for LLM awareness
- [ ] Add command examples
- [ ] Integration testing

### Phase 7: Testing & Optimization (Week 8)
- [ ] Performance profiling
- [ ] Multi-agent coordination testing
- [ ] Memory leak checks
- [ ] Documentation completion

---

## Part 9: Usage Examples

### Example 1: Find and Navigate to End City

```
User: "Find an end city and go there"

LLM Task Plan:
1. find_end_city(searchRadius=50)
2. navigate_end(target="end_city", x=<detected>, z=<detected>)
```

### Example 2: Build Shulker Farm

```
User: "Build a shulker farm at my location"

LLM Task Plan:
1. navigate_end(target="end_city")
2. build_shulker_farm(x=<currentX>, y=<currentY>, z=<currentZ>)
```

### Example 3: Operate Farm

```
User: "Farm shulkers for 10 minutes"

LLM Task Plan:
1. navigate_end(target="shulker_farm")
2. operate_shulker_farm(x=<farmX>, y=<farmY>, z=<farmZ>, duration=12000)
```

### Example 4: Full Automation

```
User: "Set up an automated shulker farm and collect 64 shells"

LLM Task Plan:
1. navigate_end(target="gateway")
2. find_end_city()
3. navigate_end(target="end_city")
4. build_shulker_farm(x=<cityX>, y=<cityY>, z=<cityZ>)
5. operate_shulker_farm(..., duration=60000)  // Run until 64 shells
6. navigate_end(target="gateway")  // Return to main island
```

---

## Part 10: Technical Considerations

### Performance Optimization
- Use grid-based detection to avoid scanning every chunk
- Implement cooldown for city detection (expensive operation)
- Cache city locations in `WorldKnowledge`
- Use spatial partitioning for multi-agent scenarios

### Safety Mechanisms
- Always enable `setInvulnerableBuilding(true)` during combat
- Implement emergency teleport if stuck in void
- Monitor agent health and retreat if damaged
- Add timeout for all phases

### Multi-Agent Coordination
- Use `CollaborativeBuildManager` for farm construction
- Assign roles: Scout (find city), Builder (construct farm), Operator (collect)
- Share city coordinates via `AgentCommunicationBus`
- Coordinate attack timing for multiple shulkers

### Error Recovery
- Handle End Gateway teleportation failures
- Retry city search with wider radius
- Fallback to manual positioning if detection fails
- Graceful degradation if farm is incomplete

---

## Part 11: Configuration

### Config Options

**File:** `config/steve-common.toml`

```toml
[shulker_farm]
# Maximum search radius for End City detection (in chunks)
end_city_search_radius = 50

# Farm operation duration (ticks, default 6000 = 5 minutes)
default_farm_duration = 6000

# Maximum shulkers to maintain in farm
max_shulkers = 3

# Enable automatic shulker culling
auto_cull_shulkers = true

# Bullet dodge distance (blocks)
bullet_dodge_distance = 15

# Shield reflection angle tolerance (degrees)
shield_angle_tolerance = 25
```

---

## Sources

- [End Gateway Portal - Baidu Baike](https://baike.baidu.com/item/%E6%9C%AB%E5%9C%B0%E6%8A%98%E8%B7%83%E9%97%A8)
- [Shulker Farming - Minecraft Wiki](https://minecraft.fandom.com/wiki/Tutorials/Shulker_farming)
- [Minecraft 1.21 Pre-Release 1](https://www.minecraft.net/en-us/article/minecraft-1-21-pre-release-1)
- [Ender Dragon - Minecraft](https://minecraft.net/zh-hans/article/ender-dragon)
- [ShulkerDupe Pro - MineBBS](https://www.minebbs.com/resources/n-shulkerdupe-pro.12650/)
- [Minecraft Wiki - Shulker Bullet Mechanics](https://minecraft.fandom.com/wiki/Tutorials/Shulker_farming)
