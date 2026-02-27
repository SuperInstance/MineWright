# Siege Warfare AI for MineWright Minecraft Mod

**Version:** 1.0.0
**Forge Version:** 1.20.1
**Author:** MineWright Development Team
**Date:** 2025-02-27

---

## Table of Contents

1. [Overview](#overview)
2. [Siege Tactics](#siege-tactics)
3. [TNT Cannon Operation](#tnt-cannon-operation)
4. [Wall Breaching Tactics](#wall-breaching-tactics)
5. [Defense Building Patterns](#defense-building-patterns)
6. [Multi-Agent Siege Coordination](#multi-agent-siege-coordination)
7. [Code Examples](#code-examples)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Integration Guide](#integration-guide)
10. [Testing Strategies](#testing-strategies)

---

## Overview

The Siege Warfare AI system transforms MineWright agents into capable military commanders and siege engineers. This system enables coordinated assaults on pillager outposts, automated TNT cannon construction and operation, intelligent wall breaching, and defensive fortification building.

### Core Philosophy

- **Asynchronous Planning**: Siege operations are planned using LLM calls but executed tick-by-tick
- **Multi-Agent Coordination**: Leverage existing `OrchestratorService` for complex multi-unit maneuvers
- **Modular Actions**: Each siege capability is a separate action registered via the plugin system
- **Resilience**: Uses existing resilience patterns (circuit breakers, retries) for network operations
- **State Machine Integration**: Siege operations integrate with `AgentStateMachine` for state tracking

### Key Capabilities

| Capability | Description | Priority |
|------------|-------------|----------|
| Pillager Outpost Raids | Coordinated assaults on pillager outposts with tactical positioning | HIGH |
| TNT Cannon Automation | Build, load, and fire TNT cannons with precision targeting | HIGH |
| Wall Breaching | Intelligent weak point detection and explosive breach tactics | MEDIUM |
| Defense Construction | Automated fortification building with strategic placement | MEDIUM |
| Siege Coordination | Multi-agent formations and tactical communication | HIGH |

---

## Siege Tactics

### Pillager Outpost Raid Tactics

Pillager outposts are fort-like structures typically housing 20-30 pillagers with patrol capabilities. Successful raids require coordinated multi-agent approaches.

#### Phase 1: Reconnaissance

```java
// Action: scout_outpost
{
    "action": "scout_outpost",
    "parameters": {
        "radius": 64,
        "report_entities": true,
        "report_structure": true,
        "identify_threats": ["pillager", "evoker", "ravager", "vindicator"]
    }
}
```

**Implementation:**

```java
public class ScoutOutpostAction extends BaseAction {
    private static final int SCOUT_RADIUS = 64;
    private static final int SCAN_TICKS = 200; // 10 seconds

    private List<BlockPos> observedPositions;
    private Map<String, Integer> entityCounts;
    private BlockPos outpostCenter;
    private int scanTicks = 0;

    @Override
    protected void onStart() {
        observedPositions = new ArrayList<>();
        entityCounts = new ConcurrentHashMap<>();
        foreman.setFlying(true);
        foreman.setInvulnerableBuilding(true);

        MineWrightMod.LOGGER.info("Foreman '{}' beginning outpost reconnaissance",
            foreman.getSteveName());
    }

    @Override
    protected void onTick() {
        scanTicks++;

        if (scanTicks >= SCAN_TICKS) {
            // Complete reconnaissance
            SiegeIntelligence intel = analyzeScoutData();
            foreman.getMemory().storeSiegeIntelligence(intel);

            result = ActionResult.success(String.format(
                "Scout complete: %d pillagers, %d vindicators, %d evokers detected at %s",
                intel.getEntityCount("pillager"),
                intel.getEntityCount("vindicator"),
                intel.getEntityCount("evoker"),
                outpostCenter
            ));
            return;
        }

        // Spiral search pattern
        double angle = (scanTicks * 0.1) % (2 * Math.PI);
        double radius = (scanTicks * 0.5) % SCOUT_RADIUS;

        BlockPos scanPos = foreman.blockPosition().offset(
            (int)(Math.cos(angle) * radius),
            0,
            (int)(Math.sin(angle) * radius)
        );

        scanForHostiles(scanPos);
        scanForStructure(scanPos);
    }

    private void scanForHostiles(BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(16.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        for (Entity entity : entities) {
            if (entity instanceof Pillager) {
                entityCounts.merge("pillager", 1, Integer::sum);
            } else if (entity instanceof Vindicator) {
                entityCounts.merge("vindicator", 1, Integer::sum);
            } else if (entity instanceof Evoker) {
                entityCounts.merge("evoker", 1, Integer::sum);
                // Prioritize evokers - they're high-value targets
                if (outpostCenter == null) {
                    outpostCenter = entity.blockPosition();
                }
            } else if (entity instanceof Ravager) {
                entityCounts.merge("ravager", 1, Integer::sum);
            }
        }
    }

    private SiegeIntelligence analyzeScoutData() {
        return new SiegeIntelligence.Builder()
            .setLocation(outpostCenter)
            .setEntityCounts(entityCounts)
            .setThreatLevel(calculateThreatLevel())
            .setRecommendedTactics(determineTactics())
            .build();
    }
}
```

#### Phase 2: Tactical Positioning

```java
// Action: tactical_flank
{
    "action": "tactical_flank",
    "parameters": {
        "target": "outpost_center",
        "formation": "pincer",
        "distance": 20,
        "cover": true
    }
}
```

**Formation Types:**

| Formation | Description | Agent Count | Best For |
|-----------|-------------|-------------|----------|
| `pincer` | Two-pronged attack from opposite sides | 2+ | Flanking maneuvers |
| `surround` | Complete encirclement | 4+ | Preventing escape |
| `wedge` | V-shaped formation | 3+ | Breaking through |
| `line` | Linear formation | 2+ | Controlled advance |
| `scatter` | Dispersed positions | 3+ | Avoiding AOE |

#### Phase 3: Coordinated Assault

```java
// Action: assault_outpost
{
    "action": "assault_outpost",
    "parameters": {
        "strategy": "priority_elimination",
        "primary_targets": ["evoker", "ravager"],
        "secondary_targets": ["vindicator", "pillager"],
        "formation": "surround",
        "fallback_on_casualties": 0.5
    }
}
```

**Priority Target Elimination:**

1. **Evokers** (Highest Priority): Summon vexes, deal magic damage
2. **Ravagers**: High damage, can destroy blocks
3. **Vindicators**: Charge attacks, high DPS
4. **Pillagers**: Ranged attacks, but lower threat

### Tactical Doctrine

The siege AI follows these tactical principles:

1. **Intelligence First**: Never attack blindly. Scout first.
2. **Priority Targeting**: Eliminate the most dangerous threats first
3. **Formation Discipline**: Maintain positions for tactical advantage
4. **Adaptive Tactics**: Change strategy based on casualties and enemy response
5. **Escape Routes**: Always maintain a retreat path

---

## TNT Cannon Operation

TNT cannons are automated siege devices that use explosive force to launch TNT projectiles at enemy fortifications. The AI can construct, load, and fire these with precision.

### Cannon Architecture

```
┌─────────────────────────────────────────┐
│         FIRE CONTROL TOWER              │
│  [Dispenser] → [Redstone] → [Observer]  │
└─────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │   Firing Deck   │
        │  [TNT] [TNT]    │
        └────────┬────────┘
                 │
    ┌────────────┴────────────┐
    │     BARREL ASSEMBLY     │
    │  [Water] [Water] [Water]│
    │  [Blocks][Blocks][Block]│
    │  [TNT] → [TNT] → [TNT]  │
    └────────────┬────────────┘
                 │
        ┌────────┴────────┐
        │   AMMUNITION    │
        │  [Chest] [TNT]  │
        └─────────────────┘
```

### Cannon Types

#### 1. Basic Horizontal Cannon

```java
// Action: build_cannon
{
    "action": "build_cannon",
    "parameters": {
        "type": "horizontal",
        "barrel_length": 12,
        "projectile_count": 1,
        "power": "standard",
        "position": {
            "x": 0,
            "y": 0,
            "z": 0,
            "facing": "north"
        }
    }
}
```

**Implementation:**

```java
public class BuildCannonAction extends BaseAction {
    private CannonType cannonType;
    private BlockPos cannonPosition;
    private Direction facing;
    private List<BlockPlacement> buildPlan;
    private int currentBlockIndex;

    public enum CannonType {
        HORIZONTAL_STANDARD,    // Basic 1x1 cannon
        HORIZONTAL_POWER,       // Longer barrel, more TNT
       .VERTICAL_MORTAR,        // Indirect fire
        RAPID_FIRE,            // Auto-loading
        SAND_BREACHER          // Sand + TNT for water shielding
    }

    @Override
    protected void onStart() {
        cannonType = CannonType.valueOf(
            task.getStringParameter("type", "HORIZONTAL_STANDARD").toUpperCase()
        );

        int barrelLength = task.getIntParameter("barrel_length", 12);
        BlockPos startPos = foreman.blockPosition();
        String facingStr = task.getStringParameter("facing", "north");
        facing = Direction.byName(facingStr);

        cannonPosition = findOptimalCannonPosition(startPos, facing);
        buildPlan = generateCannonDesign(cannonType, barrelLength);

        foreman.setFlying(true);
        foreman.setInvulnerableBuilding(true);

        MineWrightMod.LOGGER.info("Foreman '{}' building {} cannon at {} facing {}",
            foreman.getSteveName(), cannonType, cannonPosition, facing);
    }

    @Override
    protected void onTick() {
        if (currentBlockIndex >= buildPlan.size()) {
            result = ActionResult.success("Cannon construction complete");
            return;
        }

        // Place 2 blocks per tick
        for (int i = 0; i < 2 && currentBlockIndex < buildPlan.size(); i++) {
            BlockPlacement placement = buildPlan.get(currentBlockIndex);

            foreman.getLevel().setBlock(
                placement.pos,
                placement.block.defaultBlockState(),
                3
            );

            currentBlockIndex++;
        }

        // Teleport to keep up with building
        if (currentBlockIndex % 10 == 0 && currentBlockIndex < buildPlan.size()) {
            BlockPos nextPos = buildPlan.get(currentBlockIndex).pos;
            foreman.teleportTo(nextPos.getX() + 2, nextPos.getY(), nextPos.getZ() + 2);
        }
    }

    private List<BlockPlacement> generateCannonDesign(CannonType type, int barrelLength) {
        List<BlockPlacement> blocks = new ArrayList<>();

        switch (type) {
            case HORIZONTAL_STANDARD -> {
                // Build base platform
                for (int x = -2; x <= barrelLength + 2; x++) {
                    for (int z = -1; z <= 1; z++) {
                        blocks.add(new BlockPlacement(
                            cannonPosition.offset(x, 0, z),
                            Blocks.COBBLESTONE
                        ));
                    }
                }

                // Build barrel walls
                for (int x = 0; x < barrelLength; x++) {
                    // Side walls
                    blocks.add(new BlockPlacement(
                        cannonPosition.offset(x, 1, -1),
                        Blocks.COBBLESTONE
                    ));
                    blocks.add(new BlockPlacement(
                        cannonPosition.offset(x, 1, 1),
                        Blocks.COBBLESTONE
                    ));
                }

                // Water channel for explosion protection
                for (int x = 0; x < barrelLength; x++) {
                    blocks.add(new BlockPlacement(
                        cannonPosition.offset(x, 1, 0),
                        Blocks.WATER
                    ));
                }

                // Loading chamber
                blocks.add(new BlockPlacement(
                    cannonPosition.offset(2, 2, 0),
                    Blocks.DISPENSER
                ));

                // Redstone wiring
                blocks.add(new BlockPlacement(
                    cannonPosition.offset(2, 3, 0),
                    Blocks.REDSTONE_WIRE
                ));
            }

            case VERTICAL_MORTAR -> {
                // Vertical mortar design
                int height = 8;

                // Base platform
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        blocks.add(new BlockPlacement(
                            cannonPosition.offset(x, 0, z),
                            Blocks.STONE_BRICKS
                        ));
                    }
                }

                // Tube structure
                for (int y = 1; y <= height; y++) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                                blocks.add(new BlockPlacement(
                                    cannonPosition.offset(x, y, z),
                                    Blocks.STONE_BRICKS
                                ));
                            } else {
                                blocks.add(new BlockPlacement(
                                    cannonPosition.offset(x, y, z),
                                    Blocks.WATER
                                ));
                            }
                        }
                    }
                }

                // Dispenser at top
                blocks.add(new BlockPlacement(
                    cannonPosition.offset(0, height + 1, 0),
                    Blocks.DISPENSER
                ));
            }
        }

        return blocks;
    }
}
```

#### 2. Automated Firing System

```java
// Action: fire_cannon
{
    "action": "fire_cannon",
    "parameters": {
        "cannon_id": "cannon_001",
        "target": {
            "x": 100,
            "y": 64,
            "z": -200
        },
        "salvo_size": 3,
        "delay_between_shots": 40
    }
}
```

**Implementation:**

```java
public class FireCannonAction extends BaseAction {
    private BlockPos cannonPosition;
    private BlockPos targetPosition;
    private int salvoSize;
    private int shotsFired;
    private int delayTicks;
    private CannonData cannonData;

    @Override
    protected void onStart() {
        // Get cannon data from memory
        cannonData = foreman.getMemory().getCannonData(
            task.getStringParameter("cannon_id")
        );

        if (cannonData == null) {
            result = ActionResult.failure("Cannon not found");
            return;
        }

        // Calculate firing solution
        targetPosition = new BlockPos(
            task.getIntParameter("target_x"),
            task.getIntParameter("target_y"),
            task.getIntParameter("target_z")
        );

        salvoSize = task.getIntParameter("salvo_size", 1);
        shotsFired = 0;
        delayTicks = 0;

        MineWrightMod.LOGGER.info("Foreman '{}' firing cannon at {}",
            foreman.getSteveName(), targetPosition);
    }

    @Override
    protected void onTick() {
        if (shotsFired >= salvoSize) {
            result = ActionResult.success("Fired " + salvoSize + " shots");
            return;
        }

        delayTicks++;
        int requiredDelay = task.getIntParameter("delay_between_shots", 40);

        if (delayTicks >= requiredDelay) {
            fireShot();
            delayTicks = 0;
            shotsFired++;
        }
    }

    private void fireShot() {
        ServerLevel level = (ServerLevel) foreman.level();

        // Calculate trajectory
        Vec3 velocity = calculateFiringSolution(
            cannonData.muzzlePosition,
            targetPosition,
            cannonData.projectileSpeed
        );

        // Spawn primed TNT at muzzle
        PrimedTnt tnt = EntityType.TNT.create(level);
        tnt.setPos(cannonData.muzzlePosition);
        tnt.setFuse(80); // 4 seconds

        // Apply velocity
        tnt.setDeltaMovement(velocity);

        level.addFreshEntity(tnt);

        // Trigger propulsion TNT
        level.explode(null,
            cannonData.propulsionPosition.x,
            cannonData.propulsionPosition.y,
            cannonData.propulsionPosition.z,
            3.0f, Level.ExplosionInteraction.TNT
        );

        MineWrightMod.LOGGER.info("Cannon fired shot {} of {} at target {}",
            shotsFired + 1, salvoSize, targetPosition);
    }

    private Vec3 calculateFiringSolution(BlockPos start, BlockPos end, double speed) {
        // Simple ballistic trajectory calculation
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = dy;

        // Calculate launch angle (45 degrees for max range)
        double angle = Math.toRadians(45);

        double vx = speed * Math.cos(angle) * (dx / horizontalDistance);
        double vy = speed * Math.sin(angle);
        double vz = speed * Math.cos(angle) * (dz / horizontalDistance);

        return new Vec3(vx, vy, vz);
    }
}
```

### Ballistic Calculations

The firing system uses projectile motion physics:

```
Range = (v² × sin(2θ)) / g

Where:
- v = initial velocity
- θ = launch angle
- g = gravity (0.05 blocks/tick² in Minecraft)
```

**Optimal Angles:**

| Target Situation | Optimal Angle | Notes |
|------------------|---------------|-------|
| Maximum range | 45° | Furthest distance |
| High arc (clear obstacles) | 60°+ | Indirect fire |
| Low arc (direct fire) | 20-30° | Flat trajectory |
| Minimum time | 30° | Fastest impact |

---

## Wall Breaching Tactics

Wall breaching involves identifying weak points in fortifications and using explosives to create entry points.

### Weak Point Detection

```java
public class WallAnalyzer {

    public static List<BreachPoint> analyzeWeakPoints(Level level, BlockPos wallCenter, int searchRadius) {
        List<BreachPoint> weakPoints = new ArrayList<>();

        // Scan wall surface
        for (BlockPos pos : BlockPos.betweenClosed(
            wallCenter.offset(-searchRadius, -10, -searchRadius),
            wallCenter.offset(searchRadius, 10, searchRadius)
        )) {
            BlockState state = level.getBlockState(pos);

            // Check for weak materials
            float blastResistance = state.getExplosionResistance(null);

            if (blastResistance < 6.0f) { // Non-blast resistant
                BreachPoint point = new BreachPoint();
                point.position = pos;
                point.material = state.getBlock();
                point.resistance = blastResistance;
                point.thickness = calculateWallThickness(level, pos);
                point.requiredTNT = calculateRequiredTNT(blastResistance, point.thickness);

                weakPoints.add(point);
            }
        }

        // Sort by least TNT required
        weakPoints.sort(Comparator.comparingInt(p -> p.requiredTNT));

        return weakPoints;
    }

    private static int calculateWallThickness(Level level, BlockPos pos) {
        int thickness = 0;
        Direction direction = getWallDirection(level, pos);

        // Measure through wall
        for (int i = 0; i < 20; i++) {
            BlockPos checkPos = pos.relative(direction, i);
            if (!level.getBlockState(checkPos).isAir()) {
                thickness++;
            } else {
                break;
            }
        }

        return thickness;
    }

    private static int calculateRequiredTNT(float resistance, int thickness) {
        // Empirical formula for TNT required
        return (int) Math.ceil((resistance * thickness) / 4.0f);
    }
}
```

### Breach Actions

#### Direct Breach

```java
// Action: breach_wall
{
    "action": "breach_wall",
    "parameters": {
        "method": "explosive",
        "target": {
            "x": 100,
            "y": 65,
            "z": -200
        },
        "breach_size": 3,
        "tnt_count": 5
    }
}
```

#### Undermining

```java
// Action: undermine_wall
{
    "action": "undermine_wall",
    "parameters": {
        "target_wall_base": {
            "x": 100,
            "y": 64,
            "z": -200
        },
        "tunnel_depth": 8,
        "explosive_charge": "large"
    }
}
```

**Implementation:**

```java
public class UndermineWallAction extends BaseAction {
    private BlockPos wallBase;
    private int tunnelDepth;
    private BlockPos tunnelEnd;
    private int blocksMined;

    @Override
    protected void onStart() {
        wallBase = new BlockPos(
            task.getIntParameter("target_x"),
            task.getIntParameter("target_y"),
            task.getIntParameter("target_z")
        );
        tunnelDepth = task.getIntParameter("tunnel_depth", 8);

        // Calculate tunnel end position
        Direction approachDirection = getApproachDirection();
        tunnelEnd = wallBase.relative(approachDirection, tunnelDepth);

        blocksMined = 0;

        MineWrightMod.LOGGER.info("Foreman '{}' undermining wall at {}, tunneling {} blocks",
            foreman.getSteveName(), wallBase, tunnelDepth);
    }

    @Override
    protected void onTick() {
        if (blocksMined >= tunnelDepth * 9) { // 1x3 tunnel
            placeExplosiveCharge();
            result = ActionResult.success("Undermining complete, explosive placed");
            return;
        }

        // Mine tunnel
        Direction approachDirection = getApproachDirection();
        int currentDepth = blocksMined / 9;
        BlockPos currentPos = wallBase.relative(approachDirection, currentDepth);

        // Mine 1x3 cross-section
        for (int y = -1; y <= 1; y++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                BlockPos minePos = currentPos.offset(0, y, 0);

                foreman.level().destroyBlock(minePos, true);
                blocksMined++;

                if (blocksMined % 10 == 0) {
                    foreman.teleportTo(minePos.getX(), minePos.getY(), minePos.getZ());
                }
            }
        }
    }

    private void placeExplosiveCharge() {
        ServerLevel level = (ServerLevel) foreman.level();

        // Place TNT at end of tunnel
        for (int i = 0; i < 3; i++) {
            BlockPos tntPos = tunnelEnd.relative(getApproachDirection(), -i);
            level.setBlock(tntPos, Blocks.TNT.defaultBlockState(), 3);
        }

        // Ignite
        PrimedTnt tnt = EntityType.TNT.create(level);
        tnt.setPos(Vec3.atCenterOf(tunnelEnd));
        tnt.setFuse(60); // 3 seconds to retreat
        level.addFreshEntity(tnt);

        // Retreat to safe distance
        retreatToSafety();
    }

    private Direction getApproachDirection() {
        // Determine direction from foreman to wall
        Vec3 diff = Vec3.atCenterOf(wallBase).subtract(foreman.position());
        return Direction.getNearest(diff.x, diff.y, diff.z);
    }
}
```

### Water Shield Counter

For bases protected by water (which absorbs explosions):

```java
// Action: counter_water_shield
{
    "action": "counter_water_shield",
    "parameters": {
        "target_area": {
            "min_x": 90,
            "max_x": 110,
            "y": 64,
            "min_z": -210,
            "max_z": -190
        },
        "method": "sand_cannon"
    }
}
```

The sand cannon converts water to cobblestone by firing sand with TNT:

```java
public class SandCannonAction extends BaseAction {
    @Override
    protected void onTick() {
        // Fire sand mixed with TNT
        // Sand solidifies water, TNT then explodes the cobblestone

        BlockPos muzzle = cannonData.muzzlePosition;
        ServerLevel level = (ServerLevel) foreman.level();

        // Spawn falling sand entity
        FallingBlockEntity sand = FallingBlockEntity.fall(level,
            muzzle,
            Blocks.SAND.defaultBlockState()
        );

        // Give it velocity toward target
        Vec3 target = getTargetPosition();
        Vec3 velocity = target.subtract(muzzle).normalize().scale(2.0);
        sand.setDeltaMovement(velocity);

        level.addFreshEntity(sand);

        // Follow with TNT
        PrimedTnt tnt = EntityType.TNT.create(level);
        tnt.setPos(muzzle);
        tnt.setDeltaMovement(velocity);
        level.addFreshEntity(tnt);
    }
}
```

---

## Defense Building Patterns

Automated construction of defensive fortifications to protect against raids and sieges.

### Fortification Types

#### 1. Perimeter Wall

```java
// Action: build_defense_wall
{
    "action": "build_defense_wall",
    "parameters": {
        "center": {"x": 0, "y": 64, "z": 0},
        "radius": 32,
        "height": 6,
        "thickness": 2,
        "material": "cobblestone",
        "features": ["towers", "battlements", "gate"]
    }
}
```

**Implementation:**

```java
public class BuildDefenseWallAction extends BaseAction {
    private BlockPos center;
    private int radius;
    private int height;
    private int thickness;
    private Block material;
    private List<String> features;
    private List<BlockPlacement> buildPlan;
    private int currentIndex;

    @Override
    protected void onStart() {
        center = new BlockPos(
            task.getIntParameter("center_x"),
            task.getIntParameter("center_y"),
            task.getIntParameter("center_z")
        );
        radius = task.getIntParameter("radius", 32);
        height = task.getIntParameter("height", 6);
        thickness = task.getIntParameter("thickness", 2);
        material = parseBlock(task.getStringParameter("material", "cobblestone"));

        Object featuresParam = task.getParameter("features");
        features = featuresParam instanceof List ?
            (List<String>) featuresParam : new ArrayList<String>();

        buildPlan = generateWallPlan();
        currentIndex = 0;

        foreman.setFlying(true);
        foreman.setInvulnerableBuilding(true);

        MineWrightMod.LOGGER.info("Foreman '{}' building defense wall: radius={}, height={}",
            foreman.getSteveName(), radius, height);
    }

    @Override
    protected void onTick() {
        if (currentIndex >= buildPlan.size()) {
            result = ActionResult.success("Defense wall complete");
            return;
        }

        // Place 3 blocks per tick
        for (int i = 0; i < 3 && currentIndex < buildPlan.size(); i++) {
            BlockPlacement placement = buildPlan.get(currentIndex);
            foreman.level().setBlock(placement.pos, placement.block.defaultBlockState(), 3);
            currentIndex++;
        }
    }

    private List<BlockPlacement> generateWallPlan() {
        List<BlockPlacement> plan = new ArrayList<>();

        // Generate circular wall
        double circumference = 2 * Math.PI * radius;
        int numSegments = (int) (circumference / 2); // Segment every 2 blocks

        for (int segment = 0; segment < numSegments; segment++) {
            double angle = (segment * 2 * Math.PI) / numSegments;

            int x = (int) Math.round(center.getX() + radius * Math.cos(angle));
            int z = (int) Math.round(center.getZ() + radius * Math.sin(angle));

            // Wall section
            for (int t = 0; t < thickness; t++) {
                for (int y = 0; y < height; y++) {
                    BlockPos pos = new BlockPos(x, center.getY() + y, z)
                        .relative(Direction.NORTH, t);
                    plan.add(new BlockPlacement(pos, material));
                }
            }

            // Add tower every 16 segments
            if (features.contains("towers") && segment % 16 == 0) {
                addTowerPlan(plan, x, center.getY(), z);
            }

            // Add battlements
            if (features.contains("battlements")) {
                addBattlementsPlan(plan, x, center.getY() + height, z);
            }
        }

        // Add gate
        if (features.contains("gate")) {
            addGatePlan(plan);
        }

        return plan;
    }

    private void addTowerPlan(List<BlockPlacement> plan, int x, int baseY, int z) {
        int towerHeight = height + 4;
        int towerRadius = 3;

        // Cylindrical tower
        for (int y = 0; y < towerHeight; y++) {
            for (int dx = -towerRadius; dx <= towerRadius; dx++) {
                for (int dz = -towerRadius; dz <= towerRadius; dz++) {
                    if (dx * dx + dz * dz <= towerRadius * towerRadius) {
                        // Hollow inside
                        if (dx * dx + dz * dz >= (towerRadius - 1) * (towerRadius - 1)) {
                            BlockPos pos = new BlockPos(x + dx, baseY + y, z + dz);
                            plan.add(new BlockPlacement(pos, material));
                        }
                    }
                }
            }
        }

        // Crenellations on top
        for (int angle = 0; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            int cx = (int) Math.round(towerRadius * Math.cos(radians));
            int cz = (int) Math.round(towerRadius * Math.sin(radians));

            BlockPos pos = new BlockPos(x + cx, baseY + towerHeight, z + cz);
            plan.add(new BlockPlacement(pos, material));
        }
    }

    private void addBattlementsPlan(List<BlockPlacement> plan, int x, int y, int z) {
        // Alternating crenellations
        BlockPos pos = new BlockPos(x, y, z);
        plan.add(new BlockPlacement(pos, material));
        plan.add(new BlockPlacement(pos.above(1), material)); // Merlon
    }

    private void addGatePlan(List<BlockPlacement> plan) {
        // Gate on south side
        int gateX = center.getX();
        int gateZ = center.getZ() + radius;

        // Gate opening
        for (int y = 0; y < 4; y++) {
            for (int t = 0; t < thickness + 1; t++) {
                BlockPos pos = new BlockPos(gateX, center.getY() + y, gateZ)
                    .relative(Direction.NORTH, t);
                plan.add(new BlockPlacement(pos, Blocks.AIR));
            }
        }

        // Gate house
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = 4; y < height + 2; y++) {
                    BlockPos pos = new BlockPos(gateX + dx, center.getY() + y, gateZ + dz);
                    plan.add(new BlockPlacement(pos, material));
                }
            }
        }

        // Portcullis (iron bars)
        for (int y = 0; y < 4; y++) {
            BlockPos pos = new BlockPos(gateX, center.getY() + y, gateZ);
            plan.add(new BlockPlacement(pos, Blocks.IRON_BARS));
        }
    }
}
```

#### 2. Bunker/Pillbox

```java
// Action: build_bunker
{
    "action": "build_bunker",
    "parameters": {
        "position": {"x": 0, "y": 64, "z": 0},
        "type": "observation",
        "orientation": "north",
        "weapon_ports": 3,
        "roof": "reinforced"
    }
}
```

**Bunker Types:**

| Type | Description | Use Case |
|------|-------------|----------|
| `observation` | Elevated viewing position | Sniper nests |
| `machine_gun` | Wide firing arc | Area denial |
| `mortar` | Indirect fire base | Artillery support |
| `command` | Central coordination | HQ with communication facilities |

#### 3. Trap Integration

```java
// Action: install_defenses
{
    "action": "install_defenses",
    "parameters": {
        "perimeter": "wall_outer",
        "traps": [
            {"type": "tnt_minecart", "spacing": 8},
            {"type": "lava_moat", "depth": 3},
            {"type": "arrow_dispenser", "trigger": "pressure_plate"}
        ]
    }
}
```

**Implementation:**

```java
public class InstallDefensesAction extends BaseAction {

    @Override
    protected void onStart() {
        // Parse trap configurations
        List<Map<String, Object>> traps = (List<Map<String, Object>>) task.getParameter("traps");

        for (Map<String, Object> trapConfig : traps) {
            String type = (String) trapConfig.get("type");

            switch (type) {
                case "tnt_minecart" -> installTNTMinecartTraps(trapConfig);
                case "lava_moat" -> installLavaMoat(trapConfig);
                case "arrow_dispenser" -> installArrowDispensers(trapConfig);
            }
        }
    }

    private void installTNTMinecartTraps(Map<String, Object> config) {
        int spacing = (Integer) config.getOrDefault("spacing", 8);

        // Place TNT minecarts at intervals along perimeter
        BlockPos[] perimeter = getPerimeterPositions();

        for (int i = 0; i < perimeter.length; i += spacing) {
            BlockPos pos = perimeter[i];

            // Place rail
            foreman.level().setBlock(pos, Blocks.RAIL.defaultBlockState(), 3);

            // Place TNT minecart
            ServerLevel level = (ServerLevel) foreman.level();
            MinecartTNT tntCart = EntityType.TNT_MINECART.create(level);
            tntCart.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            level.addFreshEntity(tntCart);

            // Activate with activator rail
            BlockPos activatorPos = pos.below();
            foreman.level().setBlock(activatorPos, Blocks.ACTIVATOR_RAIL.defaultBlockState(), 3);
        }
    }

    private void installLavaMoat(Map<String, Object> config) {
        int depth = (Integer) config.getOrDefault("depth", 3);

        BlockPos[] perimeter = getPerimeterPositions();

        // Dig moat
        for (BlockPos pos : perimeter) {
            for (int y = 0; y < depth; y++) {
                BlockPos moatPos = pos.below(y + 1);
                foreman.level().setBlock(moatPos, Blocks.AIR.defaultBlockState(), 3);
            }

            // Fill with lava
            BlockPos lavaPos = pos.below(depth);
            foreman.level().setBlock(lavaPos, Blocks.LAVA.defaultBlockState(), 3);
        }
    }

    private void installArrowDispensers(Map<String, Object> config) {
        String trigger = (String) config.getOrDefault("trigger", "pressure_plate");

        // Place dispensers facing outward
        BlockPos[] perimeter = getPerimeterPositions();

        for (BlockPos pos : perimeter) {
            // Dispenser
            BlockPos dispenserPos = pos.above(2);
            Direction facing = getOutwardDirection(pos);
            foreman.level().setBlock(dispenserPos,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing), 3);

            // Fill with arrows
            if (foreman.level().getBlockEntity(dispenserPos) instanceof DispenserBlockEntity dispenser) {
                for (int i = 0; i < 9; i++) {
                    dispenser.setItem(i, new ItemStack(Items.ARROW, 64));
                }
            }

            // Trigger mechanism
            if ("pressure_plate".equals(trigger)) {
                BlockPos platePos = pos.above(1);
                foreman.level().setBlock(platePos,
                    Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 3);

                // Redstone connection
                foreman.level().setBlock(pos.above(2).relative(facing.getOpposite()),
                    Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            }
        }
    }
}
```

---

## Multi-Agent Siege Coordination

Leveraging the existing `OrchestratorService` for complex multi-unit siege operations.

### Siege Formation System

```java
public class SiegeFormationManager {

    public enum FormationType {
        PHALANX,         // Tight defensive square
        SKIRMISH_LINE,   // Spread out for coverage
        PINCER,          // Two-pronged attack
        HAMMER_AND_ANVIL,// Frontal attack + flank
        SIEGE_RING       // Complete encirclement
    }

    public static void assignFormationPositions(
        List<ForemanEntity> agents,
        FormationType type,
        BlockPos target,
        double radius
    ) {
        switch (type) {
            case PHALANX -> {
                // Square formation
                int sideLength = (int) Math.ceil(Math.sqrt(agents.size()));
                for (int i = 0; i < agents.size(); i++) {
                    int row = i / sideLength;
                    int col = i % sideLength;

                    BlockPos pos = target.offset(
                        col * 3 - sideLength,
                        0,
                        row * 3 - sideLength
                    );

                    assignMoveOrder(agents.get(i), pos);
                }
            }

            case PINCER -> {
                // Split into two groups
                int midPoint = agents.size() / 2;

                // Left prong
                for (int i = 0; i < midPoint; i++) {
                    double angle = Math.PI + (i * 0.2);
                    BlockPos pos = target.offset(
                        (int)(Math.cos(angle) * radius),
                        0,
                        (int)(Math.sin(angle) * radius)
                    );
                    assignMoveOrder(agents.get(i), pos);
                }

                // Right prong
                for (int i = midPoint; i < agents.size(); i++) {
                    double angle = -Math.PI + ((i - midPoint) * 0.2);
                    BlockPos pos = target.offset(
                        (int)(Math.cos(angle) * radius),
                        0,
                        (int)(Math.sin(angle) * radius)
                    );
                    assignMoveOrder(agents.get(i), pos);
                }
            }

            case SIEGE_RING -> {
                // Circle around target
                for (int i = 0; i < agents.size(); i++) {
                    double angle = (i * 2 * Math.PI) / agents.size();
                    BlockPos pos = target.offset(
                        (int)(Math.cos(angle) * radius),
                        0,
                        (int)(Math.sin(angle) * radius)
                    );
                    assignMoveOrder(agents.get(i), pos);
                }
            }
        }
    }

    private static void assignMoveOrder(ForemanEntity agent, BlockPos pos) {
        Task pathfindTask = new Task("pathfind", Map.of(
            "x", pos.getX(),
            "y", pos.getY(),
            "z", pos.getZ()
        ));

        agent.getActionExecutor().queueTask(pathfindTask);
    }
}
```

### Coordinated Attack Sequence

```java
// Action: coordinated_siege
{
    "action": "coordinated_siege",
    "parameters": {
        "target": "outpost_at_100_64_-200",
        "phases": [
            {
                "phase": "reconnaissance",
                "agents": 1,
                "formation": "scatter"
            },
            {
                "phase": "encirclement",
                "agents": "all",
                "formation": "siege_ring",
                "radius": 40
            },
            {
                "phase": "assault",
                "agents": "all",
                "formation": "pincer",
                "target_priority": ["evoker", "ravager"]
            },
            {
                "phase": "cleanup",
                "agents": "all",
                "formation": "skirmish_line"
            }
        ]
    }
}
```

**Implementation:**

```java
public class CoordinatedSiegeAction extends BaseAction {
    private List<SiegePhase> phases;
    private int currentPhase;
    private String planId;
    private OrchestratorService orchestrator;

    @Override
    protected void onStart() {
        orchestrator = MineWrightMod.getOrchestratorService();

        // Parse phases
        phases = parsePhases((List<Map<String, Object>>) task.getParameter("phases"));
        currentPhase = 0;

        // Start first phase
        executePhase(phases.get(0));

        MineWrightMod.LOGGER.info("Starting coordinated siege with {} phases",
            phases.size());
    }

    @Override
    protected void onTick() {
        SiegePhase phase = phases.get(currentPhase);

        // Check if phase is complete
        if (isPhaseComplete(phase)) {
            currentPhase++;

            if (currentPhase >= phases.size()) {
                result = ActionResult.success("Siege complete");
                return;
            }

            // Start next phase
            executePhase(phases.get(currentPhase));
        }
    }

    private void executePhase(SiegePhase phase) {
        MineWrightMod.LOGGER.info("Executing siege phase {}: {}",
            currentPhase, phase.name);

        switch (phase.phaseType) {
            case RECONNAISSANCE -> executeReconnaissance(phase);
            case ENCIRCLEMENT -> executeEncirclement(phase);
            case ASSAULT -> executeAssault(phase);
            case CLEANUP -> executeCleanup(phase);
        }
    }

    private void executeReconnaissance(SiegePhase phase) {
        // Assign scout agent
        ForemanEntity scout = assignAgent(phase);

        Task scoutTask = new Task("scout_outpost", Map.of(
            "radius", 64,
            "report_entities", true
        ));

        scout.getActionExecutor().queueTask(scoutTask);
    }

    private void executeEncirclement(SiegePhase phase) {
        // Get all available agents
        List<ForemanEntity> agents = getAvailableAgents();

        // Form siege ring
        SiegeFormationManager.assignFormationPositions(
            agents,
            SiegeFormationManager.FormationType.SIEGE_RING,
            phase.target,
            phase.radius
        );

        // Wait for positioning
        phase.waitForAgents = true;
    }

    private void executeAssault(SiegePhase phase) {
        List<ForemanEntity> agents = getAvailableAgents();

        // Assign targets based on priority
        for (ForemanEntity agent : agents) {
            BlockPos target = findPriorityTarget(agent, phase.targetPriorities);

            Task assaultTask = new Task("attack", Map.of(
                "target", "hostile",
                "target_location", Map.of(
                    "x", target.getX(),
                    "y", target.getY(),
                    "z", target.getZ()
                )
            ));

            agent.getActionExecutor().queueTask(assaultTask);
        }
    }

    private void executeCleanup(SiegePhase phase) {
        // Sweep remaining enemies
        List<ForemanEntity> agents = getAvailableAgents();

        SiegeFormationManager.assignFormationPositions(
            agents,
            SiegeFormationManager.FormationType.SKIRMISH_LINE,
            phase.target,
            20
        );
    }

    private boolean isPhaseComplete(SiegePhase phase) {
        // Check all agents have completed their tasks
        return getAvailableAgents().stream()
            .allMatch(a -> a.getActionExecutor().isIdle() ||
                         a.getActionExecutor().getCurrentTask() == null);
    }

    private static class SiegePhase {
        PhaseType phaseType;
        String name;
        int agentCount;
        String formation;
        BlockPos target;
        double radius;
        List<String> targetPriorities;
        boolean waitForAgents;

        enum PhaseType {
            RECONNAISSANCE, ENCIRCLEMENT, ASSAULT, CLEANUP
        }
    }
}
```

### Inter-Agent Communication

Siege coordination uses the existing `AgentCommunicationBus`:

```java
public class SiegeCommunicationHandler {

    public static void broadcastOrder(AgentMessage.OrderType order, Object... params) {
        AgentMessage message = new AgentMessage.Builder()
            .type(AgentMessage.Type.SIEGE_ORDER)
            .sender("foreman", "Foreman")
            .recipient("*")
            .content(order.name())
            .payload("order", order.name())
            .payload("params", params)
            .priority(AgentMessage.Priority.HIGH)
            .build();

        MineWrightMod.getOrchestratorService()
            .getCommunicationBus()
            .publish(message);
    }

    public enum OrderType {
        HOLD_POSITION,
        ADVANCE,
        RETREAT,
        TARGET_PRIORITY,
        FORMATION_CHANGE,
        CEASE_FIRE
    }
}
```

---

## Code Examples

### Complete Siege Action Plugin

```java
package com.minewright.plugin;

import com.minewright.action.actions.*;
import com.minewright.di.ServiceContainer;
import com.minewright.plugin.siege.*;

/**
 * Siege Warfare plugin for MineWright.
 * Registers all siege-related actions.
 */
public class SiegeWarfarePlugin implements ActionPlugin {

    private static final String PLUGIN_ID = "siege-warfare";
    private static final String VERSION = "1.0.0";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        int priority = getPriority();

        // Reconnaissance
        registry.register("scout_outpost",
            (foreman, task, ctx) -> new ScoutOutpostAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("analyze_defenses",
            (foreman, task, ctx) -> new AnalyzeDefensesAction(foreman, task),
            priority, PLUGIN_ID);

        // Cannon operations
        registry.register("build_cannon",
            (foreman, task, ctx) -> new BuildCannonAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("fire_cannon",
            (foreman, task, ctx) -> new FireCannonAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("load_cannon",
            (foreman, task, ctx) -> new LoadCannonAction(foreman, task),
            priority, PLUGIN_ID);

        // Breaching
        registry.register("breach_wall",
            (foreman, task, ctx) -> new BreachWallAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("undermine_wall",
            (foreman, task, ctx) -> new UndermineWallAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("counter_water_shield",
            (foreman, task, ctx) -> new CounterWaterShieldAction(foreman, task),
            priority, PLUGIN_ID);

        // Defense
        registry.register("build_defense_wall",
            (foreman, task, ctx) -> new BuildDefenseWallAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("build_bunker",
            (foreman, task, ctx) -> new BuildBunkerAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("install_defenses",
            (foreman, task, ctx) -> new InstallDefensesAction(foreman, task),
            priority, PLUGIN_ID);

        // Coordinated siege
        registry.register("coordinated_siege",
            (foreman, task, ctx) -> new CoordinatedSiegeAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("siege_formation",
            (foreman, task, ctx) -> new SiegeFormationAction(foreman, task),
            priority, PLUGIN_ID);

        registry.register("tactical_flank",
            (foreman, task, ctx) -> new TacticalFlankAction(foreman, task),
            priority, PLUGIN_ID);
    }

    @Override
    public int getPriority() {
        return 500; // Load after core, before other plugins
    }

    @Override
    public String[] getDependencies() {
        return new String[]{"core-actions"};
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "Siege warfare actions: cannons, fortifications, coordinated assaults";
    }
}
```

### Enhanced Prompt Builder for Siege Operations

```java
public class SiegePromptBuilder {

    public static String buildSiegeSystemPrompt() {
        return basePrompt() + """

            SIEGE WARFARE ACTIONS:

            Reconnaissance:
            - scout_outpost: {"radius": 64, "report_entities": true}
            - analyze_defenses: {"target": "wall", "weak_points": true}

            Artillery:
            - build_cannon: {"type": "horizontal", "barrel_length": 12, "facing": "north"}
            - fire_cannon: {"cannon_id": "cannon_001", "target": {"x": 100, "y": 64, "z": -200}, "salvo_size": 3}
            - load_cannon: {"cannon_id": "cannon_001", "ammunition": "tnt", "quantity": 64}

            Breaching:
            - breach_wall: {"method": "explosive", "target": {...}, "breach_size": 3, "tnt_count": 5}
            - undermine_wall: {"target_wall_base": {...}, "tunnel_depth": 8}
            - counter_water_shield: {"target_area": {...}, "method": "sand_cannon"}

            Defense:
            - build_defense_wall: {"center": {...}, "radius": 32, "height": 6, "features": ["towers", "gate"]}
            - build_bunker: {"position": {...}, "type": "observation", "weapon_ports": 3}
            - install_defenses: {"perimeter": "wall_outer", "traps": [{"type": "tnt_minecart", "spacing": 8}]}

            Coordination:
            - coordinated_siege: {"target": "...", "phases": [...]}
            - siege_formation: {"formation": "pincer", "radius": 20}
            - tactical_flank: {"target": "...", "formation": "pincer"}

            SIEGE TACTICS:
            1. Always scout before attacking
            2. Eliminate evokers and ravagers first
            3. Use formations for tactical advantage
            4. Target weak points with calculated explosives
            5. Coordinate multi-agent attacks for effectiveness

            FORMATION TYPES: phalanx, skirmish_line, pincer, hammer_and_anvil, siege_ring

            CANNON TYPES: horizontal, vertical_mortar, rapid_fire, sand_breacher
            """;
    }

    public static String buildSiegeTacticalPrompt(
        ForemanEntity foreman,
        String objective,
        SiegeIntelligence intel
    ) {
        return String.format("""
            === SIEGE MISSION ===
            Objective: %s

            === INTELLIGENCE ===
            Enemy Strength: %s
            Pillagers: %d
            Vindicators: %d
            Evokers: %d
            Ravagers: %d

            Fortifications: %s
            Wall Height: %d blocks
            Material: %s
            Water Shield: %s

            === YOUR FORCES ===
            Available Agents: %d
            Formation: %s
            Ammunition: %d TNT

            === TACTICAL RECOMMENDATION ===
            """,
            objective,
            intel.getThreatLevel(),
            intel.getEntityCount("pillager"),
            intel.getEntityCount("vindicator"),
            intel.getEntityCount("evoker"),
            intel.getEntityCount("ravager"),
            intel.getFortificationType(),
            intel.getWallHeight(),
            intel.getWallMaterial(),
            intel.hasWaterShield() ? "DETECTED" : "None",
            getAvailableAgentCount(),
            getCurrentFormation(),
            getTNTInventory()
        );
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Core Infrastructure**

- [ ] Create `SiegeWarfarePlugin` class
- [ ] Implement `SiegeIntelligence` data class
- [ ] Add siege-specific memory storage to `ForemanMemory`
- [ ] Extend `AgentMessage` types for siege communication
- [ ] Add siege state machine transitions

**File Structure:**
```
src/main/java/com/minewright/
├── plugin/
│   └── SiegeWarfarePlugin.java
├── action/actions/siege/
│   ├── ScoutOutpostAction.java
│   ├── AnalyzeDefensesAction.java
│   └── package-info.java
├── siege/
│   ├── SiegeIntelligence.java
│   ├── SiegeFormationManager.java
│   └── TacticalComputer.java
└── llm/
    └── SiegePromptBuilder.java
```

### Phase 2: Reconnaissance (Week 2-3)

**Scouting System**

- [ ] `ScoutOutpostAction` - Spiral search pattern
- [ ] `AnalyzeDefensesAction` - Weak point detection
- [ ] `SiegeIntelligence` - Intel storage and retrieval
- [ ] Intelligence sharing via `AgentCommunicationBus`

**Testing:**
- Spawn test pillager outpost
- Verify scout detection accuracy
- Test intelligence propagation

### Phase 3: Artillery (Week 3-4)

**Cannon System**

- [ ] `BuildCannonAction` - All cannon types
- [ ] `FireCannonAction` - Automated targeting
- [ ] `LoadCannonAction` - Ammunition management
- [ ] Ballistic calculation library
- [ ] Cannon registration in memory

**Cannon Types:**
- [ ] Horizontal standard
- [ ] Vertical mortar
- [ ] Rapid-fire
- [ ] Sand breacher

### Phase 4: Breaching (Week 4-5)

**Wall Assault**

- [ ] `BreachWallAction` - Direct explosive breach
- [ ] `UndermineWallAction` - Tunnel under wall
- [ ] `CounterWaterShieldAction` - Sand cannon
- [ ] `WallAnalyzer` - Weak point finder
- [ ] Breach tactics selection AI

### Phase 5: Defense (Week 5-6)

**Fortifications**

- [ ] `BuildDefenseWallAction` - Perimeter walls
- [ ] `BuildBunkerAction` - Defensive structures
- [ ] `InstallDefensesAction` - Trap system
- [ ] Defense structure templates (NBT)
- [ ] Automatic defense activation

### Phase 6: Coordination (Week 6-7)

**Multi-Agent Operations**

- [ ] `CoordinatedSiegeAction` - Full siege orchestration
- [ ] `SiegeFormationAction` - Formation management
- [ ] `TacticalFlankAction` - Flanking maneuvers
- [ ] `SiegeFormationManager` - Position calculations
- [ ] Inter-agent tactical communication

### Phase 7: Integration & Testing (Week 7-8)

**Polish & Balance**

- [ ] Balance TNT damage values
- [ ] Optimize cannon accuracy
- [ ] Performance testing with 10+ agents
- [ ] Siege victory condition tuning
- [ ] Documentation completion

**Test Scenarios:**
1. Pillager outpost assault (4 agents)
2. Castle siege (8 agents)
3. Defense against raid (6 agents)
4. Multi-cannon battery (3 cannons, 6 loaders)
5. Coordinated pincer attack (6 agents)

### Phase 8: Advanced Features (Week 8+)

**Future Enhancements**

- [ ] Night operations with lighting
- [ ] Weather considerations
- [ ] Supply line management
- [ ] Siege camp construction
- [ ] Prisoner handling (capture mechanics)
- [ ] Loot collection automation
- [ ] Siege victory celebration

---

## Integration Guide

### Adding Siege Plugin

1. **Register the plugin** in `META-INF/services/com.minewright.plugin.ActionPlugin`:

```
com.minewright.plugin.CoreActionsPlugin
com.minewright.plugin.SiegeWarfarePlugin
```

2. **Update PromptBuilder** to include siege actions:

```java
public class PromptBuilder {
    public static String buildSystemPrompt() {
        return basePrompt + SiegePromptBuilder.buildSiegeSystemPrompt();
    }
}
```

3. **Extend ForemanMemory** with siege intelligence:

```java
public class ForemanMemory {
    private Map<String, SiegeIntelligence> siegeIntel = new ConcurrentHashMap<>();

    public void storeSiegeIntelligence(SiegeIntelligence intel) {
        siegeIntel.put(intel.getLocation().toString(), intel);
    }

    public SiegeIntelligence getSiegeIntelligence(BlockPos location) {
        return siegeIntel.get(location.toString());
    }
}
```

### Configuration

Add to `config/minewright-common.toml`:

```toml
[siege]
# Enable siege warfare features
enabled = true

# Artillery settings
max_cannon_range = 256
cannon_accuracy = 0.85
tnt_damage_multiplier = 1.0

# Defense settings
auto_defend = true
defense_trigger_distance = 32

# Coordination
max_siege_agents = 12
formation_change_speed = 20 # ticks

# Tactical AI
intelligence_sharing = true
tactical_planning = true
```

### Commands

Add siege commands to `ForemanCommands`:

```java
public class ForemanCommands {
    @CommandLine.Command(name = "siege")
    public void siegeCommand(
        CommandLine.ParseResult parseResult,
        String action,
        String target
    ) {
        switch (action) {
            case "scout" -> executeSiegeScout(target);
            case "attack" -> executeSiegeAttack(target);
            case "defend" -> executeSiegeDefend();
            case "cannon" -> executeCannonCommand(target);
        }
    }
}
```

---

## Testing Strategies

### Unit Tests

```java
class SiegeIntelligenceTest {
    @Test
    void testThreatLevelCalculation() {
        SiegeIntelligence intel = new SiegeIntelligence.Builder()
            .setEntityCounts(Map.of(
                "pillager", 20,
                "vindicator", 5,
                "evoker", 2,
                "ravager", 1
            ))
            .build();

        assertEquals(ThreatLevel.HIGH, intel.getThreatLevel());
    }

    @Test
    void testWeakPointDetection() {
        Level level = mockLevelWithWall();
        List<BreachPoint> points = WallAnalyzer.analyzeWeakPoints(level, ORIGIN, 32);

        assertFalse(points.isEmpty());
        assertTrue(points.get(0).requiredTNT <= 10);
    }
}
```

### Integration Tests

```java
class SiegeIntegrationTest {
    @Test
    void testFullOutpostSiege() {
        // Setup
        ForemanEntity[] agents = spawnAgents(6);
        BlockPos outpost = generatePillagerOutpost();

        // Execute
        String planId = agents[0].planTasks("siege the pillager outpost ahead");

        // Wait for completion
        waitForPlanCompletion(planId, 6000);

        // Verify
        assertEquals(0, countHostiles(outpost, 64));
        assertTrue(allAgentsAlive(agents));
    }
}
```

### Performance Tests

```java
class SiegePerformanceTest {
    @Test
    void testMultiAgentPerformance() {
        int[] agentCounts = {1, 2, 4, 8, 16};

        for (int count : agentCounts) {
            long start = System.nanoTime();

            executeCoordinatedSiege(count);

            long duration = System.nanoTime() - start;
            double tps = 20.0 / (duration / 1_000_000_000.0);

            assertTrue(tps > 15.0, "TPS too low with " + count + " agents: " + tps);
        }
    }
}
```

---

## API Reference

### Siege Intelligence

```java
public class SiegeIntelligence {
    public static class Builder {
        public Builder setLocation(BlockPos pos);
        public Builder setEntityCounts(Map<String, Integer> counts);
        public Builder setThreatLevel(ThreatLevel level);
        public Builder setFortificationType(FortType type);
        public Builder setWallHeight(int height);
        public Builder setWallMaterial(Block material);
        public Builder hasWaterShield(boolean hasShield);
        public SiegeIntelligence build();
    }

    public ThreatLevel getThreatLevel();
    public int getEntityCount(String type);
    public FortType getFortificationType();
    public List<BreachPoint> getWeakPoints();
    public List<String> getRecommendedTactics();
}
```

### Cannon Data

```java
public class CannonData {
    public enum CannonType {
        HORIZONTAL_STANDARD,
        VERTICAL_MORTAR,
        RAPID_FIRE,
        SAND_BREACHER
    }

    private String id;
    private CannonType type;
    private BlockPos position;
    private BlockPos muzzlePosition;
    private BlockPos propulsionPosition;
    private Direction facing;
    private double projectileSpeed;
    private int ammunition;

    public boolean canFire();
    public void reload(int amount);
    public Vec3 calculateTrajectory(BlockPos target);
}
```

### Formation Manager

```java
public class SiegeFormationManager {
    public enum FormationType {
        PHALANX,
        SKIRMISH_LINE,
        PINCER,
        HAMMER_AND_ANVIL,
        SIEGE_RING
    }

    public static void assignFormationPositions(
        List<ForemanEntity> agents,
        FormationType type,
        BlockPos target,
        double radius
    );

    public static BlockPos calculatePosition(
        FormationType type,
        int index,
        int total,
        BlockPos target,
        double radius
    );
}
```

---

## Troubleshooting

### Common Issues

**Issue: Cannons misfiring or self-destructing**

```
Solution: Check water channel integrity
- Ensure propulsion TNT is in water
- Verify dispenser timing (40 tick delay minimum)
- Check barrel alignment
```

**Issue: Agents not maintaining formation**

```
Solution: Verify pathfinding
- Clear obstacles in formation positions
- Increase formation spacing
- Check for conflicting navigation goals
```

**Issue: Low TPS during sieges**

```
Solution: Optimize entity counts
- Limit siege to 8-12 agents
- Reduce scan radius for reconnaissance
- Disable particle effects for cannons
```

**Issue: Intelligence not sharing between agents**

```
Solution: Check communication bus
- Verify orchestrator is initialized
- Check agent registration
- Ensure message subscriptions are active
```

---

## Credits

**Research Sources:**
- [Chinese Minecraft Wiki - PvP Tutorial](https://zh.minecraft.wiki/w/Tutorial:PvP?variant=zh-cn)
- [Chinese Minecraft Wiki - TNT Cannons](https://zh.minecraft.wiki/w/Tutorial:TNT%E5%A4%A7%E7%82%AE)
- [Minecraft Wiki - Pillager Behavior](https://minecraft-archive.fandom.com/wiki/Pillager)
- [Minecraft Forge Forums](https://www.minecraftforge.net/forum/)

**Special Thanks:**
- Minecraft siege warfare community
- TNT cannon engineering pioneers
- Testers and feedback providers

---

**Document Version:** 1.0.0
**Last Updated:** 2025-02-27
**For:** MineWright Mod v1.0.0+
**Minecraft Version:** 1.20.1 (Forge)
