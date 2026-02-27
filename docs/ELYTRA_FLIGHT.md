# Elytra Flight AI System for MineWright

## Executive Summary

This document describes a comprehensive elytra flight AI system for the MineWright Minecraft mod (Forge 1.20.1). The system enables Foreman entities to intelligently fly using elytra, manage durability, optimize firework rocket usage, and execute safe landing protocols.

**Version:** 1.0
**Date:** 2026-02-27
**Status:** Design Phase

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Elytra Mechanics Reference](#elytra-mechanics-reference)
3. [Flight Path Planning](#flight-path-planning)
4. [Rocket Timing Optimization](#rocket-timing-optimization)
5. [Landing Algorithms](#landing-algorithms)
6. [Emergency Strategies](#emergency-strategies)
7. [Implementation Guide](#implementation-guide)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)

---

## System Overview

### Design Goals

1. **Intelligent Flight Planning**: Calculate optimal flight paths considering terrain, obstacles, and efficiency
2. **Durability Management**: Monitor elytra durability and plan conservatively
3. **Rocket Conservation**: Use firework rockets optimally for maximum distance
4. **Safe Landings**: Always identify landing zones before starting flight
5. **Emergency Recovery**: Handle mid-air failures and unsafe situations
6. **Integration**: Seamlessly integrate with existing action system

### Core Components

```
ElytraFlightSystem
├── ElytraFlightController (main orchestration)
├── FlightPathPlanner (route calculation)
├── RocketTimingOptimizer (boost management)
├── LandingZoneEvaluator (landing site detection)
├── DurabilityMonitor (elytra health tracking)
├── HeightEstimator (altitude calculations)
├── EmergencyLandingSystem (crash recovery)
└── ElytraFlightAction (action implementation)
```

---

## Elytra Mechanics Reference

### Minecraft 1.20.1 Elytra Mechanics

Based on the 1.18+ rebalance that carries forward to 1.20.1:

#### Durability System

| Property | Value |
|----------|-------|
| **Maximum Durability** | 432 points |
| **Repair Material** | Phantom Membrane (108 durability each) |
| **Flight Time (unenchanted)** | ~7 minutes 12 seconds |
| **Gliding Drain** | **0 per second** (changed in 1.18!) |
| **Rocket Boost Drain** | 1 point per second |
| **With Unbreaking III** | ~1 point per 4 seconds (average) |

**Critical Change:** Elytra no longer lose durability during normal gliding - only when using firework rockets for acceleration. This makes them much more viable for long-distance exploration.

#### Flight Physics

| Property | Value |
|----------|-------|
| **Glide Ratio** | 9.47:1 (~10 blocks forward per 1 block down) |
| **Minimum Speed** | ~7.2 m/s (at 30° upward angle) |
| **Maximum Safe Landing Angle** | 38.6° |
| **Normal Glide Speed** | ~60 blocks/second maximum |
| **Firework Boost Speed** | 26 m/s (reduced from 30 m/s in 1.18) |
| **Minimum Altitude Loss** | 1.5 m/s at optimal glide angle (12-15°) |

#### Fall Damage & Landing

| Scenario | Damage |
|----------|--------|
| **Landing angle < 38.6° at low speed** | No damage |
| **Landing angle > 38.6°** | Damage proportional to gliding speed |
| **Standard fall damage** | Fall distance - 3 blocks (each block = 1 heart) |
| **Hay bale/Honey block** | 80% damage reduction |
| **Water landing** | No damage |

#### Firework Rocket Mechanics

| Property | Value |
|----------|-------|
| **Flight Duration NBT** | `Flight` tag (1-3 gunpowder = 1-3 seconds boost) |
| **Lifetime Formula** | `((Flight + 1) * 10 + random(0,5) + random(0,6))` ticks |
| **Damage with Firework Star** | 7 damage (3.5 hearts) |
| **Activation** | Right-click while gliding with elytra equipped |

---

## Flight Path Planning

### Path Planning Algorithm

The flight path planner calculates optimal routes considering:

1. **Terrain Analysis**: Elevation changes, obstacles, mountains
2. **Distance Estimation**: Horizontal and vertical components
3. **Durability Budget**: Rockets needed based on distance
4. **Landing Zones**: Pre-identified safe landing sites
5. **Emergency Stops**: Fallback landing zones along the route

### High-Level Flight Planning

```java
public class FlightPathPlanner {
    private static final double GLIDE_RATIO = 9.47; // blocks forward per block down
    private static final int MIN_SAFE_HEIGHT = 20; // blocks above ground
    private static final double MAX_GLIDE_SPEED = 60.0; // blocks/second

    public FlightPlan planFlight(BlockPos start, BlockPos destination,
                                  ServerLevel level, int durabilityRemaining) {
        // 1. Calculate direct distance
        double horizontalDistance = Math.sqrt(
            Math.pow(destination.getX() - start.getX(), 2) +
            Math.pow(destination.getZ() - start.getZ(), 2)
        );
        int verticalDistance = destination.getY() - start.getY();

        // 2. Estimate height requirements
        int requiredHeightLoss = (int) Math.ceil(horizontalDistance / GLIDE_RATIO);
        int requiredStartingHeight = destination.getY() + requiredHeightLoss + MIN_SAFE_HEIGHT;

        // 3. Check if we have enough height advantage
        int actualHeightLoss = start.getY() - destination.getY();
        boolean needsRocketBoosts = actualHeightLoss < requiredHeightLoss;

        // 4. Calculate rocket requirements
        int rocketsNeeded = calculateRocketsNeeded(
            horizontalDistance,
            verticalDistance,
            durabilityRemaining
        );

        // 5. Generate waypoints
        List<BlockPos> waypoints = generateWaypoints(
            start,
            destination,
            level,
            needsRocketBoosts,
            rocketsNeeded
        );

        // 6. Identify emergency landing zones
        List<LandingZone> emergencyLandings = findEmergencyLandingZones(
            waypoints,
            level
        );

        return new FlightPlan(
            start,
            destination,
            waypoints,
            rocketsNeeded,
            emergencyLandings,
            requiredStartingHeight
        );
    }

    private List<BlockPos> generateWaypoints(BlockPos start, BlockPos dest,
                                             ServerLevel level,
                                             boolean needsBoosts,
                                             int rocketsNeeded) {
        List<BlockPos> waypoints = new ArrayList<>();
        waypoints.add(start);

        if (!needsBoosts) {
            // Pure gliding flight - follow terrain contours
            waypoints.addAll(generateGlidingWaypoints(start, dest, level));
        } else {
            // Rocket-assisted flight - climb then glide
            waypoints.addAll(generateRocketWaypoints(start, dest, rocketsNeeded, level));
        }

        waypoints.add(dest);
        return waypoints;
    }

    private List<BlockPos> generateGlidingWaypoints(BlockPos start, BlockPos dest,
                                                    ServerLevel level) {
        List<BlockPos> waypoints = new ArrayList<>();

        // Intermediate waypoints for terrain following
        int segments = 5;
        for (int i = 1; i < segments; i++) {
            double t = (double) i / segments;
            int x = (int) Math.round(start.getX() + t * (dest.getX() - start.getX()));
            int z = (int) Math.round(start.getZ() + t * (dest.getZ() - start.getZ()));

            // Calculate height needed at this point
            int remainingDistance = (int) Math.sqrt(
                Math.pow(dest.getX() - x, 2) + Math.pow(dest.getZ() - z, 2)
            );
            int heightNeeded = (int) Math.ceil(remainingDistance / GLIDE_RATIO) +
                              dest.getY() + MIN_SAFE_HEIGHT;

            // Find highest terrain in area and add clearance
            int terrainHeight = getHighestTerrainBlock(level, x, z, 16);
            int waypointY = Math.max(heightNeeded, terrainHeight + MIN_SAFE_HEIGHT);

            waypoints.add(new BlockPos(x, waypointY, z));
        }

        return waypoints;
    }
}
```

### Terrain-Aware Route Planning

```java
public class TerrainAnalyzer {
    private static final int SCAN_RADIUS = 32; // blocks
    private static final int TERRAIN_SAMPLE_RATE = 4; // Check every N blocks

    public TerrainProfile analyzeTerrain(ServerLevel level, BlockPos start, BlockPos end) {
        TerrainProfile profile = new TerrainProfile();

        // Sample terrain along route
        int distance = (int) Math.sqrt(
            Math.pow(end.getX() - start.getX(), 2) +
            Math.pow(end.getZ() - start.getZ(), 2)
        );

        for (int d = 0; d <= distance; d += TERRAIN_SAMPLE_RATE) {
            double t = distance == 0 ? 0 : (double) d / distance;
            int x = (int) Math.round(start.getX() + t * (end.getX() - start.getX()));
            int z = (int) Math.round(start.getZ() + t * (end.getZ() - start.getZ()));

            // Find terrain height
            int maxY = getHighestTerrainBlock(level, x, z, SCAN_RADIUS);
            profile.addTerrainPoint(x, maxY, z);
        }

        // Identify obstacles
        profile.setObstacles(findObstacles(level, profile));

        // Identify climbing opportunities
        profile.setUpdrafts(findUpdrafts(level, profile));

        return profile;
    }

    private int getHighestTerrainBlock(ServerLevel level, int x, int z, int radius) {
        int maxY = level.getMinBuildHeight();

        // Scan in a spiral pattern
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = new BlockPos(x + dx, level.getMaxBuildHeight() - 1, z + dz);

                // Find first solid block from top
                while (pos.getY() > level.getMinBuildHeight() &&
                       level.getBlockState(pos).isAir()) {
                    pos = pos.below();
                }

                if (pos.getY() > maxY) {
                    maxY = pos.getY();
                }
            }
        }

        return maxY;
    }

    private List<Obstacle> findObstacles(ServerLevel level, TerrainProfile profile) {
        List<Obstacle> obstacles = new ArrayList<>();

        for (TerrainProfile.Point point : profile.getPoints()) {
            BlockPos pos = new BlockPos(point.x(), point.y(), point.z());

            // Check for dangerous blocks
            BlockState state = level.getBlockState(pos.above(20)); // Check flight altitude

            if (isDangerousBlock(state)) {
                obstacles.add(new Obstacle(pos, ObstacleType.BLOCK));
            }
        }

        return obstacles;
    }

    private boolean isDangerousBlock(BlockState state) {
        return state.is(Blocks.LAVA) ||
               state.is(Blocks.FIRE) ||
               state.is(BlockTags.FIRE) ||
               state.is(BlockTags.LEAVES); // Leaves cause slowdown
    }
}
```

---

## Rocket Timing Optimization

### Rocket Usage Strategy

Firework rockets are the primary method for:
1. **Gaining Altitude**: Climbing before a gliding descent
2. **Maintaining Speed**: Counteracting drag during long flights
3. **Emergency Recovery**: Recovering from stalls or unexpected situations

### Rocket Calculator

```java
public class RocketTimingOptimizer {
    private static final double BOOST_ACCELERATION = 26.0; // m/s per rocket
    private static final double GLIDE_DRAG = 0.5; // speed loss per second
    private static final int ROCKET_DURATION_TICKS = 20; // 1 second per gunpowder

    public RocketPlan calculateRocketPlan(FlightPlan flightPlan,
                                          int durabilityRemaining) {
        double totalDistance = flightPlan.getTotalDistance();
        int heightGainNeeded = flightPlan.getHeightGainNeeded();

        // Calculate rockets for altitude
        int altitudeRockets = calculateAltitudeRockets(heightGainNeeded);

        // Calculate rockets for maintaining glide
        int glideRockets = calculateGlideRockets(totalDistance);

        // Total rockets needed
        int totalRockets = Math.min(altitudeRockets + glideRockets,
                                    durabilityRemaining);

        // Schedule rocket usage
        List<RocketBurn> schedule = scheduleRocketBurns(
            flightPlan,
            totalRockets
        );

        return new RocketPlan(totalRockets, schedule);
    }

    private int calculateAltitudeRockets(int heightGainNeeded) {
        // Each rocket provides approximately 10-15 blocks of vertical boost
        // when aimed straight up at optimal angle
        final int BLOCKS_PER_ROCKET = 12;

        return (int) Math.ceil((double) heightGainNeeded / BLOCKS_PER_ROCKET);
    }

    private int calculateGlideRockets(double distance) {
        // Glide ratio of 9.47:1 means we lose 1 block height per 9.47 blocks forward
        // Rockets can extend this by providing speed

        double heightLossRequired = distance / 9.47;

        // Each rocket effectively "saves" ~8 blocks of height loss
        // when used at optimal intervals
        final int HEIGHT_SAVED_PER_ROCKET = 8;

        return (int) Math.ceil(heightLossRequired / HEIGHT_SAVED_PER_ROCKET);
    }

    private List<RocketBurn> scheduleRocketBurns(FlightPlan plan,
                                                  int rocketCount) {
        List<RocketBurn> schedule = new ArrayList<>();
        List<BlockPos> waypoints = plan.getWaypoints();

        if (waypoints.isEmpty()) return schedule;

        // Strategy: Use 30% of rockets at start for climb, 70% spaced for glide
        int climbRockets = (int) Math.ceil(rocketCount * 0.3);
        int glideRockets = rocketCount - climbRockets;

        // Schedule climb rockets at beginning
        for (int i = 0; i < climbRockets; i++) {
            schedule.add(new RocketBurn(
                waypoints.get(0), // At start position
                RocketPhase.CLIMB,
                i * 5 // Every 5 ticks
            ));
        }

        // Schedule glide rockets evenly along path
        if (waypoints.size() > 1 && glideRockets > 0) {
            int segments = waypoints.size() - 1;
            int rocketsPerSegment = Math.max(1, glideRockets / segments);

            for (int seg = 0; seg < segments && schedule.size() < rocketCount; seg++) {
                for (int r = 0; r < rocketsPerSegment && schedule.size() < rocketCount; r++) {
                    double t = (double) r / rocketsPerSegment;
                    BlockPos pos = interpolatePosition(
                        waypoints.get(seg),
                        waypoints.get(seg + 1),
                        t
                    );

                    schedule.add(new RocketBurn(
                        pos,
                        RocketPhase.GLIDE,
                        seg * 100 + r * 20 // Timing calculation
                    ));
                }
            }
        }

        return schedule;
    }

    private BlockPos interpolatePosition(BlockPos from, BlockPos to, double t) {
        return new BlockPos(
            (int) Math.round(from.getX() + t * (to.getX() - from.getX())),
            (int) Math.round(from.getY() + t * (to.getY() - from.getY())),
            (int) Math.round(from.getZ() + t * (to.getZ() - from.getZ()))
        );
    }
}
```

### Rocket Execution State Machine

```java
public enum RocketPhase {
    CLIMB,       // Initial ascent phase
    CRUISE,      // Level flight
    DESCENT,     // Final approach
    EMERGENCY    // Emergency boost
}

public class RocketBurn {
    private final BlockPos position;
    private final RocketPhase phase;
    private final int tickOffset;

    public RocketBurn(BlockPos position, RocketPhase phase, int tickOffset) {
        this.position = position;
        this.phase = phase;
        this.tickOffset = tickOffset;
    }

    // Getters...
}

public class RocketPlan {
    private final int totalRockets;
    private final List<RocketBurn> schedule;

    public RocketPlan(int totalRockets, List<RocketBurn> schedule) {
        this.totalRockets = totalRockets;
        this.schedule = schedule;
    }

    public RocketBurn getNextBurn(int currentTick, BlockPos currentPosition) {
        for (RocketBurn burn : schedule) {
            if (!burn.isExecuted() &&
                shouldExecuteBurn(burn, currentTick, currentPosition)) {
                return burn;
            }
        }
        return null;
    }

    private boolean shouldExecuteBurn(RocketBurn burn, int tick, BlockPos pos) {
        // Check if we're at the right time
        if (tick < burn.getTickOffset()) {
            return false;
        }

        // Check if we're close enough to the target position
        double distance = Math.sqrt(
            Math.pow(pos.getX() - burn.getPosition().getX(), 2) +
            Math.pow(pos.getZ() - burn.getPosition().getZ(), 2)
        );

        return distance < 10.0; // Within 10 blocks
    }
}
```

---

## Landing Algorithms

### Landing Zone Evaluation

```java
public class LandingZoneEvaluator {
    private static final int LANDING_SEARCH_RADIUS = 64; // blocks
    private static final int MIN_LANDING_SIZE = 5; // 5x5 minimum
    private static final double MAX_LANDING_SLOPE = 0.3; // Max slope angle
    private static final int MIN_APPROACH_CLEARANCE = 20; // blocks

    public Optional<LandingZone> findLandingZone(BlockPos destination,
                                                  ServerLevel level) {
        // 1. Check if destination itself is suitable
        if (isSuitableLandingZone(destination, level)) {
            return Optional.of(new LandingZone(destination, LandingQuality.EXCELLENT));
        }

        // 2. Search nearby for suitable zones
        List<BlockPos> candidates = findCandidateZones(destination, level);

        // 3. Evaluate each candidate
        LandingZone bestZone = null;
        double bestScore = 0.0;

        for (BlockPos candidate : candidates) {
            LandingZone zone = evaluateLandingZone(candidate, level);
            double score = calculateLandingScore(zone, destination);

            if (score > bestScore) {
                bestScore = score;
                bestZone = zone;
            }
        }

        return Optional.ofNullable(bestZone);
    }

    private List<BlockPos> findCandidateZones(BlockPos center, ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>();

        // Spiral search pattern
        int radius = 5;
        while (radius <= LANDING_SEARCH_RADIUS && candidates.isEmpty()) {
            for (int x = -radius; x <= radius; x += 5) {
                for (int z = -radius; z <= radius; z += 5) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        BlockPos candidate = new BlockPos(
                            center.getX() + x,
                            center.getY(),
                            center.getZ() + z
                        );

                        // Adjust Y to terrain height
                        candidate = getSurfacePosition(level, candidate);

                        if (candidate != null) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
            radius += 5;
        }

        return candidates;
    }

    private boolean isSuitableLandingZone(BlockPos pos, ServerLevel level) {
        // Check for flat surface
        if (!isFlatSurface(pos, level, MIN_LANDING_SIZE)) {
            return false;
        }

        // Check for safe approach
        if (!hasSafeApproach(pos, level)) {
            return false;
        }

        // Check for hazards
        if (hasNearbyHazards(pos, level)) {
            return false;
        }

        return true;
    }

    private boolean isFlatSurface(BlockPos center, ServerLevel level, int size) {
        int halfSize = size / 2;

        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                BlockPos pos = center.offset(x, 0, z);
                BlockState surface = level.getBlockState(pos);

                // Must be solid, walkable block
                if (!surface.isSolidRender(level, pos)) {
                    return false;
                }

                // Check for obstacles above
                BlockState above1 = level.getBlockState(pos.above());
                BlockState above2 = level.getBlockState(pos.above(2));

                if (!above1.isAir() || !above2.isAir()) {
                    return false; // Need clearance for landing
                }
            }
        }

        return true;
    }

    private boolean hasSafeApproach(BlockPos landingPos, ServerLevel level) {
        // Check approach vector from expected direction
        Direction approachDir = getApproachDirection(landingPos, level);

        for (int dist = 5; dist <= MIN_APPROACH_CLEARANCE; dist += 5) {
            BlockPos checkPos = landingPos.relative(approachDir, dist);

            // Check for obstacles in approach path
            if (!isPassable(level, checkPos)) {
                return false;
            }
        }

        return true;
    }

    private LandingZone evaluateLandingZone(BlockPos pos, ServerLevel level) {
        LandingQuality quality = LandingQuality.POOR;
        List<String> issues = new ArrayList<>();

        // Size assessment
        int size = assessZoneSize(pos, level);
        if (size >= MIN_LANDING_SIZE * 2) {
            quality = LandingQuality.EXCELLENT;
        } else if (size >= MIN_LANDING_SIZE) {
            quality = LandingQuality.GOOD;
        } else {
            issues.add("Too small");
        }

        // Surface quality
        if (!isFlatSurface(pos, level, size)) {
            quality = LandingQuality.POOR;
            issues.add("Uneven surface");
        }

        // Hazards
        if (hasNearbyHazards(pos, level)) {
            quality = LandingQuality.DANGEROUS;
            issues.add("Nearby hazards");
        }

        return new LandingZone(pos, quality, issues, size);
    }

    private boolean hasNearbyHazards(BlockPos pos, ServerLevel level) {
        int checkRadius = 10;

        for (BlockPos checkPos : BlockPos.betweenClosed(
            pos.offset(-checkRadius, -5, -checkRadius),
            pos.offset(checkRadius, 5, checkRadius)
        )) {
            BlockState state = level.getBlockState(checkPos);

            if (state.is(Blocks.LAVA) ||
                state.is(Blocks.FIRE) ||
                state.is(Blocks.CACTUS) ||
                state.is(Blocks.MAGMA_BLOCK)) {
                return true;
            }
        }

        return false;
    }

    private double calculateLandingScore(LandingZone zone, BlockPos destination) {
        double score = 0.0;

        // Quality score
        score += zone.getQuality().score * 50;

        // Distance score (closer to destination is better)
        double distance = Math.sqrt(
            Math.pow(zone.getPosition().getX() - destination.getX(), 2) +
            Math.pow(zone.getPosition().getZ() - destination.getZ(), 2)
        );
        score += Math.max(0, 100 - distance);

        // Size score
        score += zone.getSize() * 2;

        return score;
    }
}

public enum LandingQuality {
    DANGEROUS(0),
    POOR(25),
    GOOD(50),
    EXCELLENT(100);

    public final int score;

    LandingQuality(int score) {
        this.score = score;
    }
}

public class LandingZone {
    private final BlockPos position;
    private final LandingQuality quality;
    private final List<String> issues;
    private final int size;
    private final Direction approachDirection;

    public LandingZone(BlockPos position, LandingQuality quality) {
        this(position, quality, List.of(), 5, Direction.SOUTH);
    }

    public LandingZone(BlockPos position, LandingQuality quality,
                      List<String> issues, int size) {
        this(position, quality, issues, size, Direction.SOUTH);
    }

    public LandingZone(BlockPos position, LandingQuality quality,
                      List<String> issues, int size, Direction approachDirection) {
        this.position = position;
        this.quality = quality;
        this.issues = issues;
        this.size = size;
        this.approachDirection = approachDirection;
    }

    // Getters...
}
```

### Landing Execution

```java
public class LandingExecutor {
    private static final double MAX_SAFE_LANDING_SPEED = 15.0; // m/s
    private static final double MAX_SAFE_LANDING_ANGLE = 38.6; // degrees
    private static final int LANDING_APPROACH_DISTANCE = 30; // blocks
    private static final int FINAL_APPROACH_HEIGHT = 10; // blocks

    public enum LandingPhase {
        APPROACH,    // High altitude approach
        ALIGN,      // Align with landing zone
        FLARE,      // Flare to reduce speed
        TOUCHDOWN   // Final landing
    }

    private LandingPhase currentPhase = LandingPhase.APPROACH;
    private LandingZone targetZone;
    private int ticksInPhase = 0;

    public void tick(ForemanEntity foreman, Vec3 currentVelocity,
                     LandingZone zone) {
        this.targetZone = zone;
        ticksInPhase++;

        switch (currentPhase) {
            case APPROACH -> handleApproachPhase(foreman, currentVelocity);
            case ALIGN -> handleAlignPhase(foreman, currentVelocity);
            case FLARE -> handleFlarePhase(foreman, currentVelocity);
            case TOUCHDOWN -> handleTouchdownPhase(foreman);
        }
    }

    private void handleApproachPhase(ForemanEntity foreman, Vec3 velocity) {
        BlockPos pos = foreman.blockPosition();
        double distance = pos.distSqr(targetZone.getPosition());

        // Transition to align when close enough
        if (distance < LANDING_APPROACH_DISTANCE * LANDING_APPROACH_DISTANCE) {
            currentPhase = LandingPhase.ALIGN;
            ticksInPhase = 0;
            return;
        }

        // Adjust pitch for gradual descent
        double targetPitch = calculateApproachPitch(foreman, targetZone);
        adjustPitch(foreman, targetPitch);

        // Use rocket if needed to maintain approach path
        if (velocity.length() < 10.0) {
            useRocket(foreman);
        }
    }

    private void handleAlignPhase(ForemanEntity foreman, Vec3 velocity) {
        BlockPos pos = foreman.blockPosition();
        int heightAboveGround = pos.getY() - targetZone.getPosition().getY();

        // Transition to flare when at final approach height
        if (heightAboveGround <= FINAL_APPROACH_HEIGHT) {
            currentPhase = LandingPhase.FLARE;
            ticksInPhase = 0;
            return;
        }

        // Align with landing zone direction
        lookAtPosition(foreman, targetZone.getPosition());

        // Gradual descent
        double targetPitch = -15.0; // Slight nose-up for controlled descent
        adjustPitch(foreman, targetPitch);

        // Speed check
        double speed = velocity.length();
        if (speed > MAX_SAFE_LANDING_SPEED) {
            // Flare early to reduce speed
            adjustPitch(foreman, 30.0); // Nose up significantly
        }
    }

    private void handleFlarePhase(ForemanEntity foreman, Vec3 velocity) {
        BlockPos pos = foreman.blockPosition();
        int heightAboveGround = pos.getY() - targetZone.getPosition().getY();

        // Check for touchdown
        if (heightAboveGround <= 2) {
            currentPhase = LandingPhase.TOUCHDOWN;
            ticksInPhase = 0;
            return;
        }

        // Aggressive flare to reduce speed
        adjustPitch(foreman, 45.0); // Nose up 45 degrees

        // Speed check
        double speed = velocity.length();
        if (speed > 8.0 && heightAboveGround > 5) {
            // Still too fast - consider emergency rocket away
            if (ticksInPhase > 20) {
                initiateGoAround(foreman);
            }
        }
    }

    private void handleTouchdownPhase(ForemanEntity foreman) {
        // Close elytra
        foreman.setFlying(false);

        // Verify safe landing
        BlockPos pos = foreman.blockPosition();
        if (!isSafeLandingPosition(pos, foreman.level())) {
            // Landing failed - initiate emergency procedures
            initiateEmergencyLanding(foreman);
        }
    }

    private double calculateApproachPitch(ForemanEntity foreman, LandingZone zone) {
        BlockPos currentPos = foreman.blockPosition();
        double horizontalDistance = Math.sqrt(
            Math.pow(zone.getPosition().getX() - currentPos.getX(), 2) +
            Math.pow(zone.getPosition().getZ() - currentPos.getZ(), 2)
        );
        double verticalDistance = zone.getPosition().getY() - currentPos.getY();

        // Calculate glide angle
        double angle = Math.toDegrees(Math.atan2(verticalDistance, horizontalDistance));

        // Clamp to safe range
        return Math.max(-30, Math.min(15, angle));
    }

    private void adjustPitch(ForemanEntity foreman, double targetPitch) {
        // Set entity look direction
        Vec3 lookVec = foreman.getLookAngle();
        float currentYaw = (float) Math.toDegrees(Math.atan2(lookVec.z, lookVec.x));
        float currentPitch = (float) Math.toDegrees(Math.asin(lookVec.y));

        // Gradually adjust pitch
        float newPitch = (float) (currentPitch + (targetPitch - currentPitch) * 0.1);

        foreman.setYRot(currentYaw);
        foreman.setXRot(newPitch);
    }

    private void lookAtPosition(ForemanEntity foreman, BlockPos target) {
        Vec3 eyePos = foreman.getEyePosition(1.0f);
        Vec3 targetPos = Vec3.atCenterOf(target);

        Vec3 direction = targetPos.subtract(eyePos).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.asin(direction.y));

        foreman.setYRot(yaw);
        foreman.setXRot(pitch);
    }
}
```

---

## Emergency Strategies

### Emergency Landing System

```java
public class EmergencyLandingSystem {
    private static final int EMERGENCY_LANDING_HEIGHT = 15; // blocks
    private static final int CRITICAL_HEIGHT = 5; // blocks
    private static final double STALL_SPEED = 7.2; // m/s

    public enum EmergencyType {
        LOW_DURABILITY,
        OUT_OF_ROCKETS,
        STALL_IMMINENT,
        OBstacle_COLLISION,
        UNEXPECTED_TERRAIN,
        NIGHT_FALLING
    }

    public void handleEmergency(ForemanEntity foreman, EmergencyType type,
                                Vec3 currentVelocity) {
        BlockPos currentPos = foreman.blockPosition();
        int heightAboveGround = getHeightAboveGround(foreman);

        // Log emergency
        foreman.sendChatMessage("EMERGENCY: " + type + " at height " + heightAboveGround);

        switch (type) {
            case LOW_DURABILITY -> handleLowDurability(foreman, heightAboveGround);
            case OUT_OF_ROCKETS -> handleOutOfRockets(foreman, heightAboveGround, currentVelocity);
            case STALL_IMMINENT -> handleStall(foreman, currentVelocity);
            case OBstacle_COLLISION -> handleCollisionAvoidance(foreman);
            case UNEXPECTED_TERRAIN -> handleTerrainEmergency(foreman);
            case NIGHT_FALLING -> handleNightLanding(foreman);
        }
    }

    private void handleLowDurability(ForemanEntity foreman, int heightAboveGround) {
        if (heightAboveGround < EMERGENCY_LANDING_HEIGHT) {
            // Close enough to land immediately
            initiateImmediateLanding(foreman);
        } else {
            // Look for emergency landing zone
            Optional<LandingZone> zone = findEmergencyLandingZone(foreman);
            if (zone.isPresent()) {
                divertToEmergencyZone(foreman, zone.get());
            } else {
                // No zone found - prepare for rough landing
                prepareForRoughLanding(foreman);
            }
        }
    }

    private void handleOutOfRockets(ForemanEntity foreman, int height,
                                    Vec3 velocity) {
        if (velocity.length() < STALL_SPEED) {
            // About to stall - nose down to gain speed
            adjustPitch(foreman, -30.0);
        } else if (height > EMERGENCY_LANDING_HEIGHT) {
            // Enough height for gliding approach
            initiateGlideLanding(foreman);
        } else {
            // Too low - prepare for impact
            prepareForRoughLanding(foreman);
        }
    }

    private void handleStall(ForemanEntity foreman, Vec3 velocity) {
        // Immediate nose-down to recover speed
        adjustPitch(foreman, -45.0);

        // Check if we have rockets for recovery
        if (hasRockets(foreman)) {
            // Use rocket to boost speed
            useRocket(foreman);
        }

        // Monitor for recovery
        scheduleRecoveryCheck(foreman);
    }

    private void handleCollisionAvoidance(ForemanEntity foreman) {
        // Immediate turn away from obstacle
        Vec3 currentDir = foreman.getLookAngle();
        Vec3 avoidanceDir = findAvoidanceDirection(foreman);

        // Turn toward avoidance direction
        float targetYaw = (float) Math.toDegrees(Math.atan2(avoidanceDir.z, avoidanceDir.x));
        foreman.setYRot(targetYaw);

        // Climb to gain clearance
        adjustPitch(foreman, 20.0);

        if (hasRockets(foreman)) {
            useRocket(foreman);
        }
    }

    private void handleTerrainEmergency(ForemanEntity foreman) {
        // Unexpected terrain rising ahead
        int heightAboveGround = getHeightAboveGround(foreman);

        if (heightAboveGround < 20) {
            // Too low to climb - emergency landing
            initiateImmediateLanding(foreman);
        } else {
            // Climb to clear terrain
            adjustPitch(foreman, 30.0);

            if (hasRockets(foreman)) {
                // Use multiple rockets to climb
                for (int i = 0; i < 3 && hasRockets(foreman); i++) {
                    useRocket(foreman);
                }
            }
        }
    }

    private void handleNightLanding(ForemanEntity foreman) {
        // Night is falling - need to land soon
        Optional<LandingZone> zone = findEmergencyLandingZone(foreman);

        if (zone.isPresent()) {
            // Land at nearest suitable zone
            divertToEmergencyZone(foreman, zone.get());
        } else {
            // Build emergency shelter
            initiateEmergencyShelterBuild(foreman);
        }
    }

    private Optional<LandingZone> findEmergencyLandingZone(ForemanEntity foreman) {
        BlockPos currentPos = foreman.blockPosition();

        // Search in direction of travel first
        Vec3 lookDir = foreman.getLookAngle();
        BlockPos searchCenter = currentPos.offset(
            (int) (lookDir.x * 30),
            0,
            (int) (lookDir.z * 30)
        );

        LandingZoneEvaluator evaluator = new LandingZoneEvaluator();
        return evaluator.findLandingZone(searchCenter, foreman.level());
    }

    private void divertToEmergencyZone(ForemanEntity foreman, LandingZone zone) {
        // Recalculate flight path to emergency zone
        foreman.sendChatMessage("Diverting to emergency landing zone");

        // Update flight plan
        // (This would interact with the flight controller)
    }

    private void initiateImmediateLanding(ForemanEntity foreman) {
        foreman.sendChatMessage("Initiating immediate landing");

        // Flare aggressively
        adjustPitch(foreman, 60.0);

        // Close elytra when close to ground
        int height = getHeightAboveGround(foreman);
        if (height < 5) {
            foreman.setFlying(false);
        }
    }

    private void prepareForRoughLanding(ForemanEntity foreman) {
        foreman.sendChatMessage("Preparing for rough landing");

        // Minimize impact speed
        adjustPitch(foreman, 45.0);

        // Search for water or soft blocks
        BlockPos below = foreman.blockPosition().below();
        BlockState belowState = foreman.level().getBlockState(below);

        if (belowState.is(Blocks.WATER) ||
            belowState.is(Blocks.POWDER_SNOW) ||
            belowState.is(Blocks.HAY_BLOCK)) {
            // Good - soft landing
            return;
        }

        // Check if we can place water
        if (foreman.getInventory().contains(Items.WATER_BUCKET)) {
            // Place water below
            placeWaterBelow(foreman);
        }
    }

    private int getHeightAboveGround(ForemanEntity foreman) {
        BlockPos pos = foreman.blockPosition();
        ServerLevel level = (ServerLevel) foreman.level();

        // Raycast downward to find ground
        for (int y = pos.getY(); y >= level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState state = level.getBlockState(checkPos);

            if (state.isSolid()) {
                return pos.getY() - y;
            }
        }

        return pos.getY() - level.getMinBuildHeight();
    }

    private void placeWaterBelow(ForemanEntity foreman) {
        // Use water bucket to create safe landing
        BlockPos below = foreman.blockPosition().below(3);

        // Place water
        foreman.level().setBlock(below, Blocks.WATER.defaultBlockState(), 3);

        // Use bucket
        // (Inventory manipulation code would go here)
    }

    private boolean hasRockets(ForemanEntity foreman) {
        return foreman.getInventory().hasItem(Items.FIREWORK_ROCKET);
    }

    private void useRocket(ForemanEntity foreman) {
        // Find and use firework rocket
        // (Inventory and item usage code would go here)
    }

    private void adjustPitch(ForemanEntity foreman, double pitch) {
        // Set entity pitch
        foreman.setXRot((float) pitch);
    }
}
```

---

## Implementation Guide

### Package Structure

```
src/main/java/com/minewright/flight/
├── ElytraFlightController.java       # Main flight orchestration
├── planner/
│   ├── FlightPathPlanner.java        # Route calculation
│   ├── TerrainAnalyzer.java          # Terrain analysis
│   └── FlightPlan.java               # Flight plan model
├── rocket/
│   ├── RocketTimingOptimizer.java    # Rocket usage planning
│   ├── RocketPlan.java               # Rocket schedule model
│   └── RocketBurn.java               # Individual rocket burn
├── landing/
│   ├── LandingZoneEvaluator.java    # Landing site detection
│   ├── LandingExecutor.java          # Landing execution
│   └── LandingZone.java              # Landing zone model
├── emergency/
│   └── EmergencyLandingSystem.java   # Emergency procedures
├── durability/
│   └── DurabilityMonitor.java        # Elytra health tracking
└── action/
    └── ElytraFlightAction.java       # Action implementation
```

### Integration with Existing Systems

#### 1. Register the Action

```java
// In CoreActionsPlugin.java or new ElytraActionsPlugin.java
registry.register("fly_to",
    (foreman, task, ctx) -> new ElytraFlightAction(foreman, task),
    priority, PLUGIN_ID);

registry.register("land",
    (foreman, task, ctx) -> new LandAction(foreman, task),
    priority, PLUGIN_ID);

registry.register("emergency_land",
    (foreman, task, ctx) -> new EmergencyLandAction(foreman, task),
    priority, PLUGIN_ID);
```

#### 2. Update ForemanEntity

```java
public class ForemanEntity extends PathfinderMob {
    // Existing fields...
    private ElytraFlightController flightController;
    private DurabilityMonitor durabilityMonitor;

    public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        // Existing initialization...

        if (!level.isClientSide) {
            this.durabilityMonitor = new DurabilityMonitor(this);
            this.flightController = new ElytraFlightController(this);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Existing logic...

            // Tick flight controller if flying
            if (isFlying && flightController != null) {
                flightController.tick();
            }

            // Tick durability monitor
            if (durabilityMonitor != null) {
                durabilityMonitor.tick();
            }
        }
    }

    // Check if elytra is equipped
    public boolean hasElytra() {
        ItemStack chestSlot = this.getItemBySlot(EquipmentSlot.CHEST);
        return chestSlot.is(Items.ELYTRA);
    }

    public DurabilityMonitor getDurabilityMonitor() {
        return durabilityMonitor;
    }

    public ElytraFlightController getFlightController() {
        return flightController;
    }
}
```

#### 3. Add Inventory Access

```java
public class InventoryUtils {
    public static int countFireworkRockets(ForemanEntity foreman) {
        int count = 0;
        for (ItemStack stack : foreman.getInventory().items) {
            if (stack.is(Items.FIREWORK_ROCKET)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean consumeFireworkRocket(ForemanEntity foreman) {
        for (int i = 0; i < foreman.getInventory().getContainerSize(); i++) {
            ItemStack stack = foreman.getInventory().getItem(i);
            if (stack.is(Items.FIREWORK_ROCKET)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    public static int getElytraDurability(ForemanEntity foreman) {
        ItemStack chestSlot = foreman.getItemBySlot(EquipmentSlot.CHEST);
        if (chestSlot.is(Items.ELYTRA)) {
            return chestSlot.getMaxDamage() - chestSlot.getDamageValue();
        }
        return 0;
    }
}
```

---

## Code Examples

### Complete ElytraFlightAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.flight.*;
import com.minewright.flight.emergency.EmergencyLandingSystem;
import com.minewright.flight.landing.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ElytraFlightAction extends BaseAction {
    private final ElytraFlightController flightController;
    private final DurabilityMonitor durabilityMonitor;

    private BlockPos destination;
    private FlightPlan flightPlan;
    private RocketPlan rocketPlan;
    private LandingZone landingZone;

    private enum FlightState {
        PLANNING,
        TAKEOFF,
        CRUISING,
        LANDING_APPROACH,
        LANDING,
        COMPLETE,
        FAILED
    }

    private FlightState state = FlightState.PLANNING;
    private int ticksInState = 0;

    public ElytraFlightAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.flightController = foreman.getFlightController();
        this.durabilityMonitor = foreman.getDurabilityMonitor();
    }

    @Override
    protected void onStart() {
        // Validate prerequisites
        if (!validatePrerequisites()) {
            result = ActionResult.failure("Missing elytra or firework rockets");
            return;
        }

        // Get destination
        int destX = task.getIntParameter("x", 0);
        int destY = task.getIntParameter("y", foreman.blockPosition().getY());
        int destZ = task.getIntParameter("z", 0);
        destination = new BlockPos(destX, destY, destZ);

        state = FlightState.PLANNING;
    }

    @Override
    protected void onTick() {
        ticksInState++;

        switch (state) {
            case PLANNING -> planFlight();
            case TAKEOFF -> executeTakeoff();
            case CRUISING -> cruiseToDestination();
            case LANDING_APPROACH -> approachLanding();
            case LANDING -> executeLanding();
        }

        // Check for emergencies
        checkForEmergencies();
    }

    private boolean validatePrerequisites() {
        // Check for elytra
        if (!foreman.hasElytra()) {
            foreman.sendChatMessage("I need an elytra to fly!");
            return false;
        }

        // Check elytra durability
        int durability = durabilityMonitor.getDurability();
        if (durability < 50) {
            foreman.sendChatMessage("My elytra is too damaged to fly safely!");
            return false;
        }

        // Check for firework rockets
        int rocketCount = InventoryUtils.countFireworkRockets(foreman);
        if (rocketCount < 3) {
            foreman.sendChatMessage("I need at least 3 firework rockets for a safe flight!");
            return false;
        }

        return true;
    }

    private void planFlight() {
        foreman.sendChatMessage("Planning flight to " + destination);

        // Plan flight path
        FlightPathPlanner pathPlanner = new FlightPathPlanner();
        flightPlan = pathPlanner.planFlight(
            foreman.blockPosition(),
            destination,
            (ServerLevel) foreman.level(),
            durabilityMonitor.getDurability()
        );

        if (flightPlan == null) {
            result = ActionResult.failure("Could not plan flight path");
            state = FlightState.FAILED;
            return;
        }

        // Plan rocket usage
        RocketTimingOptimizer rocketOptimizer = new RocketTimingOptimizer();
        rocketPlan = rocketOptimizer.calculateRocketPlan(
            flightPlan,
            durabilityMonitor.getDurability()
        );

        // Find landing zone
        LandingZoneEvaluator landingEvaluator = new LandingZoneEvaluator();
        landingZone = landingEvaluator.findLandingZone(destination, foreman.level())
            .orElse(null);

        if (landingZone == null) {
            result = ActionResult.failure("No suitable landing zone found near destination");
            state = FlightState.FAILED;
            return;
        }

        // Check if plan is feasible
        if (rocketPlan.getTotalRockets() > InventoryUtils.countFireworkRockets(foreman)) {
            foreman.sendChatMessage("Not enough rockets for this flight! Need " +
                rocketPlan.getTotalRockets());
            result = ActionResult.failure("Insufficient firework rockets");
            state = FlightState.FAILED;
            return;
        }

        // Plan is good - proceed to takeoff
        foreman.sendChatMessage("Flight plan ready! " +
            rocketPlan.getTotalRockets() + " rockets needed. Landing at " +
            landingZone.getPosition());

        state = FlightState.TAKEOFF;
        ticksInState = 0;
    }

    private void executeTakeoff() {
        // Equip elytra if not already equipped
        if (!foreman.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            // Find elytra in inventory and equip
            equipElytra();
        }

        // Start flying
        foreman.setFlying(true);

        // Initial rocket boost for takeoff
        InventoryUtils.consumeFireworkRocket(foreman);
        foreman.sendChatMessage("Taking off!");

        state = FlightState.CRUISING;
        ticksInState = 0;
    }

    private void cruiseToDestination() {
        // Follow flight plan waypoints
        if (flightPlan.getWaypoints().isEmpty()) {
            state = FlightState.LANDING_APPROACH;
            ticksInState = 0;
            return;
        }

        // Get current waypoint
        BlockPos currentWaypoint = flightPlan.getCurrentWaypoint(ticksInState);
        if (currentWaypoint == null) {
            state = FlightState.LANDING_APPROACH;
            ticksInState = 0;
            return;
        }

        // Move toward waypoint
        moveToward(currentWaypoint);

        // Check if we need to use a rocket
        RocketBurn nextBurn = rocketPlan.getNextBurn(ticksInState, foreman.blockPosition());
        if (nextBurn != null && shouldExecuteRocket(nextBurn)) {
            InventoryUtils.consumeFireworkRocket(foreman);
            nextBurn.setExecuted(true);
            foreman.sendChatMessage("Using rocket for boost!");
        }

        // Check if we're close to landing zone
        double distanceToLanding = foreman.blockPosition().distSqr(landingZone.getPosition());
        if (distanceToLanding < 900) { // Within 30 blocks
            state = FlightState.LANDING_APPROACH;
            ticksInState = 0;
        }
    }

    private void approachLanding() {
        // Delegate to landing executor
        LandingExecutor landingExecutor = new LandingExecutor();
        landingExecutor.tick(foreman, foreman.getDeltaMovement(), landingZone);

        // Check if landed
        if (!foreman.isFlying()) {
            state = FlightState.COMPLETE;
            result = ActionResult.success("Successfully landed at " + landingZone.getPosition());
        }
    }

    private void executeLanding() {
        // Final landing checks
        foreman.setFlying(false);

        BlockPos pos = foreman.blockPosition();
        if (pos.distSqr(landingZone.getPosition()) < 25) {
            // Successful landing
            result = ActionResult.success("Landed at " + landingZone.getPosition());
            state = FlightState.COMPLETE;
            foreman.sendChatMessage("Touchdown!");
        } else {
            // Missed landing zone
            result = ActionResult.failure("Missed landing zone");
            state = FlightState.FAILED;
        }
    }

    private void checkForEmergencies() {
        // Check elytra durability
        if (durabilityMonitor.getDurability() < 20) {
            EmergencyLandingSystem emergencySystem = new EmergencyLandingSystem();
            emergencySystem.handleEmergency(foreman,
                EmergencyLandingSystem.EmergencyType.LOW_DURABILITY,
                foreman.getDeltaMovement());
            return;
        }

        // Check rocket count
        int rocketsLeft = InventoryUtils.countFireworkRockets(foreman);
        if (rocketsLeft == 0 && state == FlightState.CRUISING) {
            EmergencyLandingSystem emergencySystem = new EmergencyLandingSystem();
            emergencySystem.handleEmergency(foreman,
                EmergencyLandingSystem.EmergencyType.OUT_OF_ROCKETS,
                foreman.getDeltaMovement());
            return;
        }

        // Check for stall
        double speed = foreman.getDeltaMovement().length();
        if (speed < 7.2 && foreman.isFlying() && state != FlightState.LANDING) {
            EmergencyLandingSystem emergencySystem = new EmergencyLandingSystem();
            emergencySystem.handleEmergency(foreman,
                EmergencyLandingSystem.EmergencyType.STALL_IMMINENT,
                foreman.getDeltaMovement());
            return;
        }
    }

    private void moveToward(BlockPos target) {
        // Calculate direction
        Vec3 currentPos = foreman.position();
        Vec3 targetPos = Vec3.atCenterOf(target);
        Vec3 direction = targetPos.subtract(currentPos).normalize();

        // Set look direction
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.asin(direction.y));
        foreman.setYRot(yaw);
        foreman.setXRot(pitch);

        // Apply velocity in look direction
        Vec3 velocity = foreman.getDeltaMovement();
        double speed = velocity.length();

        // Maintain minimum glide speed
        if (speed < 10.0) {
            speed = 10.0;
        }

        foreman.setDeltaMovement(direction.scale(speed));
    }

    private boolean shouldExecuteRocket(RocketBurn burn) {
        BlockPos currentPos = foreman.blockPosition();
        double distance = Math.sqrt(
            Math.pow(currentPos.getX() - burn.getPosition().getX(), 2) +
            Math.pow(currentPos.getZ() - burn.getPosition().getZ(), 2)
        );
        return distance < 10.0;
    }

    private void equipElytra() {
        // Find elytra in inventory and equip it
        // (Implementation depends on inventory system)
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.sendChatMessage("Flight cancelled");
    }

    @Override
    public String getDescription() {
        return "Fly to " + destination;
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1)

**Tasks:**
- [ ] Create `flight` package structure
- [ ] Implement `DurabilityMonitor` for elytra health tracking
- [ ] Implement `InventoryUtils` for rocket management
- [ ] Create base models: `FlightPlan`, `RocketPlan`, `LandingZone`
- [ ] Add unit tests for durability monitoring

**Files:**
- `src/main/java/com/minewright/flight/durability/DurabilityMonitor.java`
- `src/main/java/com/minewright/flight/util/InventoryUtils.java`
- `src/main/java/com/minewright/flight/FlightPlan.java`
- `src/main/java/com/minewright/flight/rocket/RocketPlan.java`
- `src/main/java/com/minewright/flight/landing/LandingZone.java`

### Phase 2: Flight Planning (Week 2)

**Tasks:**
- [ ] Implement `FlightPathPlanner` with basic route calculation
- [ ] Implement `TerrainAnalyzer` for terrain profiling
- [ ] Create waypoint generation algorithms
- [ ] Add distance and duration estimation
- [ ] Test flight planning in various terrain types

**Files:**
- `src/main/java/com/minewright/flight/planner/FlightPathPlanner.java`
- `src/main/java/com/minewright/flight/planner/TerrainAnalyzer.java`
- `src/test/java/com/minewright/flight/FlightPlannerTest.java`

### Phase 3: Rocket Optimization (Week 3)

**Tasks:**
- [ ] Implement `RocketTimingOptimizer`
- [ ] Create rocket burn scheduling algorithm
- [ ] Add altitude boost calculations
- [ ] Implement glide extension formulas
- [ ] Test rocket efficiency

**Files:**
- `src/main/java/com/minewright/flight/rocket/RocketTimingOptimizer.java`
- `src/main/java/com/minewright/flight/rocket/RocketBurn.java`
- `src/test/java/com/minewright/flight/RocketOptimizerTest.java`

### Phase 4: Landing System (Week 4)

**Tasks:**
- [ ] Implement `LandingZoneEvaluator`
- [ ] Create landing zone detection algorithm
- [ ] Implement surface quality assessment
- [ ] Add approach clearance checking
- [ ] Test landing zone finding in various biomes

**Files:**
- `src/main/java/com/minewright/flight/landing/LandingZoneEvaluator.java`
- `src/main/java/com/minewright/flight/landing/LandingExecutor.java`
- `src/test/java/com/minewright/flight/LandingSystemTest.java`

### Phase 5: Emergency Systems (Week 5)

**Tasks:**
- [ ] Implement `EmergencyLandingSystem`
- [ ] Create emergency type handlers
- [ ] Add emergency zone finding
- [ ] Implement rough landing procedures
- [ ] Test emergency scenarios

**Files:**
- `src/main/java/com/minewright/flight/emergency/EmergencyLandingSystem.java`
- `src/test/java/com/minewright/flight/EmergencySystemTest.java`

### Phase 6: Action Integration (Week 6)

**Tasks:**
- [ ] Implement `ElytraFlightAction`
- [ ] Implement `LandAction`
- [ ] Implement `EmergencyLandAction`
- [ ] Register actions in plugin system
- [ ] Integrate with `ForemanEntity`
- [ ] Test action execution

**Files:**
- `src/main/java/com/minewright/flight/action/ElytraFlightAction.java`
- `src/main/java/com/minewright/flight/action/LandAction.java`
- `src/main/java/com/minewright/plugin/ElytraActionsPlugin.java`

### Phase 7: Flight Controller (Week 7)

**Tasks:**
- [ ] Implement `ElytraFlightController` as main orchestrator
- [ ] Integrate all subsystems
- [ ] Add flight state management
- [ ] Implement flight tick loop
- [ ] Add in-flight monitoring

**Files:**
- `src/main/java/com/minewright/flight/ElytraFlightController.java`

### Phase 8: Testing & Polish (Week 8-9)

**Tasks:**
- [ ] Comprehensive testing in all dimensions (Overworld, Nether, End)
- [ ] Performance optimization
- [ ] Edge case handling (mountains, oceans, caves)
- [ ] Multi-agent flight coordination
- [ ] Documentation completion
- [ ] User configuration options

### Phase 9: Release (Week 10)

**Tasks:**
- [ ] Final integration testing
- [ ] Performance regression testing
- [ ] Release notes
- [ ] Deployment

---

## Configuration

Add to `config/minewright-common.toml`:

```toml
[elytra_flight]
# Enable elytra flight AI
enabled = true

# Minimum elytra durability required for flight
min_durability = 50

# Minimum firework rockets required for flight
min_rockets = 3

# Safety factor for rocket calculations (higher = more conservative)
rocket_safety_factor = 1.2

# Landing zone search radius (blocks)
landing_search_radius = 64

# Minimum landing zone size (blocks)
min_landing_size = 5

# Emergency landing height (blocks)
emergency_landing_height = 15

# Glide ratio (blocks forward per block down)
glide_ratio = 9.47

# Maximum safe landing speed (m/s)
max_landing_speed = 15.0

# Maximum safe landing angle (degrees)
max_landing_angle = 38.6

# Enable emergency landing systems
enable_emergency_systems = true

# Log flight details
log_flight_plans = true
log_rocket_usage = true
log_landings = true
```

---

## Performance Considerations

### Optimization Strategies

1. **Lazy Terrain Analysis**: Only analyze terrain when planning flights
2. **Cached Landing Zones**: Remember good landing zones
3. **Path Reuse**: Cache common flight paths
4. **Async Planning**: Plan flights in background threads
5. **Sparse Updates**: Check landing conditions every 2 seconds, not every tick

### Memory Usage

| Component | Memory per Agent |
|-----------|------------------|
| Flight Plan | ~2 KB |
| Rocket Plan | ~1 KB |
| Landing Zones | ~5 KB (cached) |
| Terrain Profile | ~10 KB (temporary) |
| **Total** | ~18 KB per flying agent |

### CPU Impact

| Operation | Time (ms) | Frequency |
|-----------|-----------|-----------|
| Flight Planning | 50-200 | Per flight |
| Rocket Calculation | 5-10 | Per flight |
| Landing Zone Search | 20-100 | Per flight |
| Flight Tick | <1 | Per tick |
| Emergency Check | <1 | Per tick |

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testFlightPlanningWithHeightAdvantage() {
    // Plan flight from high point to low point
    BlockPos start = new BlockPos(0, 200, 0);
    BlockPos dest = new BlockPos(100, 64, 100);

    FlightPlan plan = planner.planFlight(start, dest, level, 432);

    assertNotNull(plan);
    assertTrue(plan.getRocketsNeeded() < 5); // Should need few rockets
}

@Test
public void testLandingZoneDetection() {
    // Create test landing zone
    createFlatArea(new BlockPos(100, 64, 100), 10);

    Optional<LandingZone> zone = evaluator.findLandingZone(
        new BlockPos(100, 64, 100), level
    );

    assertTrue(zone.isPresent());
    assertEquals(LandingQuality.GOOD, zone.get().getQuality());
}

@Test
public void testEmergencyLandingAtLowDurability() {
    ForemanEntity foreman = spawnForeman();
    foreman.setFlying(true);

    // Simulate low durability
    when(durabilityMonitor.getDurability()).thenReturn(15);

    emergencySystem.handleEmergency(foreman,
        EmergencyType.LOW_DURABILITY, Vec3.ZERO);

    assertFalse(foreman.isFlying()); // Should have landed
}
```

### Integration Tests

```java
@Test
public void testCompleteFlight() {
    // Spawn foreman with elytra and rockets
    ForemanEntity foreman = spawnForemanWithElytra();
    giveItem(foreman, Items.FIREWORK_ROCKET, 64);

    // Issue flight command
    Task task = new Task("fly_to", Map.of(
        "x", 500,
        "y", 100,
        "z", 500
    ));

    foreman.getActionExecutor().queueTask(task);

    // Simulate flight
    for (int i = 0; i < 6000; i++) { // 5 minutes max
        foreman.tick();

        if (!foreman.getActionExecutor().isExecuting()) {
            break;
        }
    }

    // Verify successful landing
    BlockPos finalPos = foreman.blockPosition();
    assertTrue(finalPos.getX() >= 450 && finalPos.getX() <= 550);
    assertFalse(foreman.isFlying());
}
```

---

## Troubleshooting

### Common Issues

**Issue**: Agent crashes into terrain
- **Solution**: Improve terrain analysis, add more waypoints

**Issue**: Agent runs out of rockets mid-flight
- **Solution**: Increase rocket safety factor, add emergency landing

**Issue**: Agent can't find landing zone
- **Solution**: Increase search radius, lower quality threshold

**Issue**: Agent takes fall damage on landing
- **Solution**: Improve landing flare timing, check landing angle

**Issue**: Poor flight performance
- **Solution**: Optimize terrain scanning, cache flight paths

---

## Future Enhancements

### Phase 10+ Features

1. **Advanced Maneuvers**
   - Barrel rolls for speed adjustment
   - S-turns for descent control
   - Thermal soaring in desert/mesa biomes

2. **Multi-Agent Coordination**
   - Formation flying
   - Drafting for efficiency
   - Coordinated landing patterns

3. **Weather-Aware Flight**
   - Use updrafts in thunderstorms
   - Avoid rain-related slowdown
   - Crosswind compensation

4. **Combat Flight**
   - Evasive maneuvers
   - Aerial attack patterns
   - Bombing runs

5. **Autonomous Exploration**
   - Automated survey flights
   - Photo mapping
   - Resource spotting from air

6. **Flight Network**
   - Establish air routes
   - Waypoint beacons
   - Flight paths between bases

---

## Sources

- [Elytra Mechanics - Minecraft Wiki (Fandom)](https://minecraft.fandom.com/zh/wiki/%E9%9E%98%E7%BF%85?variant=zh-cn)
- [Firework Rocket Mechanics - Minecraft Wiki](https://minecraft.fandom.com/wiki/Firework_Rocket)
- [Easy Elytra Takeoff Mod - MCMod](https://www.mcmod.cn/class/11112.html)
- [How to Rocket Propel Elytra - DigMinecraft](https://www.digminecraft.com/getting_started/how_to_rocket_propel_elytra.php)
- [Fall Damage Tutorial - Bilibili Wiki](https://wiki.biligame.com/mc/Tutorial:%E5%87%8F%E5%B0%91%E6%91%94%E8%90%BD%E4%BC%A4%E5%AE%B3)
- [No Fall-Damage Mod - MCMod](https://www.mcmod.cn/class/23752.html)
- [Elytra HUD Display Mod - MCMod](https://www.mcmod.cn/class/12802.html)
- [Elytra Flight Duration Tutorial - TheSpike.gg](https://www.thespike.gg/minecraft/recipes/firework-recipes)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Claude Code (Elytra Flight AI Design)

---

## Notes

This design document provides a complete blueprint for implementing intelligent elytra flight in MineWright. The system is designed to:

1. **Plan safe flights** with proper terrain analysis
2. **Optimize rocket usage** for maximum efficiency
3. **Execute safe landings** with proper flare and touchdown
4. **Handle emergencies** with multiple fallback strategies
5. **Integrate seamlessly** with existing action and orchestration systems

The modular design allows for incremental implementation following the 10-week roadmap. The plugin-based action registration ensures compatibility with the existing action system, while the flight controller provides a clean abstraction for all flight operations.
