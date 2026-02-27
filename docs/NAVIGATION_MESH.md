# Navigation Mesh System for MineWright

## Executive Summary

This document describes a comprehensive navigation mesh (navmesh) system for the MineWright Minecraft mod (Forge 1.20.1). The navmesh system enables intelligent pathfinding for AI crew members, supporting dynamic terrain, multi-height navigation, and efficient region-based pathfinding.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Navmesh Generation](#navmesh-generation)
3. [Pathfinding Algorithm](#pathfinding-algorithm)
4. [Dynamic Updates](#dynamic-updates)
5. [Multi-Height Navigation](#multi-height-navigation)
6. [Obstacle Avoidance](#obstacle-avoidance)
7. [Integration Guide](#integration-guide)
8. [Implementation Roadmap](#implementation-roadmap)

---

## Architecture Overview

### Current State Analysis

The existing codebase uses Minecraft's built-in `PathfinderMob` navigation system:
- `ForemanEntity` extends `PathfinderMob` (line 34, ForemanEntity.java)
- `PathfindAction` uses `foreman.getNavigation().moveTo()` for basic pathfinding
- `FollowPlayerAction` similarly relies on vanilla pathfinding

**Limitations of current approach:**
- No awareness of dynamic terrain changes
- Inefficient for long-distance pathfinding
- No support for complex movement (jumping, climbing)
- No multi-agent path coordination
- No persistent world knowledge for pathfinding

### Navmesh System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     NavMeshManager                           │
│  - Global navmesh registry per dimension                     │
│  - Region lifecycle management                               │
│  - Update scheduling and optimization                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ├─────────────────────────────────────┐
                     │                                     │
┌────────────────────▼──────────────────┐  ┌────────────────▼─────────────────┐
│           NavMeshRegion               │  │      NavMeshUpdateScheduler      │
│  - Spatially partitioned navmesh       │  │  - Chunk-based update scheduling │
│  - A* pathfinding within region       │  │  - Priority-based invalidation   │
│  - Neighbor region links              │  │  - Deferred update batching      │
└────────────────────┬──────────────────┘  └──────────────────────────────────┘
                     │
                     ├─────────────────────────────────────┐
                     │                                     │
┌────────────────────▼──────────────────┐  ┌────────────────▼─────────────────┐
│           NavPolygon                  │  │      NavMeshCollisionSystem      │
│  - Walkable surface representation    │  │  - Dynamic obstacle detection    │
│  - Height information (floors/ceiling)│  │  - Block change listeners        │
│  - Connectivity graph                 │  │  - AABB collision queries        │
└───────────────────────────────────────┘  └──────────────────────────────────┘
```

### Core Components

| Component | Responsibility | Key Methods |
|-----------|----------------|-------------|
| `NavMeshManager` | Global navmesh registry, region management | `getRegion()`, `invalidateRegion()`, `scheduleUpdate()` |
| `NavMeshRegion` | 16x16 chunk-based navmesh partition | `findPath()`, `getPolygons()`, `getNeighbors()` |
| `NavPolygon` | Walkable surface representation | `isWalkable()`, `getNeighbors()`, `getCenter()` |
| `NavMeshUpdateScheduler` | Manages deferred navmesh updates | `onBlockChange()`, `processUpdates()` |
| `NavMeshCollisionSystem` | Collision detection and obstacle avoidance | `canStandAt()`, `getObstaclesBetween()` |
| `NavPathExecutor` | Executes computed paths with movement actions | `followPath()`, `handleObstacles()` |

---

## Navmesh Generation

### Overview

Navmesh generation converts Minecraft's voxel world into a polygon mesh representing walkable surfaces. This approach dramatically reduces the search space compared to voxel-based pathfinding.

### Generation Algorithm

#### Phase 1: Surface Detection

```java
/**
 * Scans a chunk region to identify walkable surfaces.
 * A surface is walkable if:
 * 1. There's a solid block below (y-1)
 * 2. The space above (y+1, y+2) is passable
 * 3. The block itself is passable (AIR, SLAB, CARPET, etc.)
 */
public List<NavPolygon> detectWalkableSurface(ServerLevel level, ChunkPos chunkPos) {
    List<NavPolygon> polygons = new ArrayList<>();

    // Scan vertical columns within the chunk
    for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
        for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
            int maxY = getHighestBlockY(level, x, z);

            for (int y = level.getMinBuildHeight(); y <= maxY; y++) {
                if (isWalkableSurface(level, new BlockPos(x, y, z))) {
                    NavPolygon polygon = createPolygonFromColumn(level, x, y, z);
                    if (polygon != null) {
                        polygons.add(polygon);
                    }
                }
            }
        }
    }

    return polygons;
}

private boolean isWalkableSurface(ServerLevel level, BlockPos pos) {
    BlockState current = level.getBlockState(pos);
    BlockState below = level.getBlockState(pos.below());
    BlockState above1 = level.getBlockState(pos.above());
    BlockState above2 = level.getBlockState(pos.above(2));

    // Must have solid footing
    if (!isSolid(below) && !isPartialSolid(below)) {
        return false;
    }

    // Current position must be passable
    if (!isPassable(current)) {
        return false;
    }

    // Need 2 blocks of clearance for entity
    if (!isPassable(above1) || !isPassable(above2)) {
        return false;
    }

    return true;
}
```

#### Phase 2: Polygon Merging

```java
/**
 * Merges adjacent walkable columns into larger polygons.
 * Uses a flood-fill approach to group contiguous walkable surfaces
 * at the same Y-level.
 */
public List<NavPolygon> mergePolygons(List<NavPolygon> rawPolygons) {
    List<NavPolygon> mergedPolygons = new ArrayList<>();
    Set<NavPolygon> visited = new HashSet<>();

    for (NavPolygon start : rawPolygons) {
        if (visited.contains(start)) continue;

        // Find all connected polygons at this level
        Set<NavPolygon> component = new HashSet<>();
        Queue<NavPolygon> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            NavPolygon current = queue.poll();
            if (visited.contains(current)) continue;

            visited.add(current);
            component.add(current);

            // Add orthogonal neighbors at same Y-level
            for (NavPolygon neighbor : getOrthogonalNeighbors(current, rawPolygons)) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        // Create merged polygon from component
        if (!component.isEmpty()) {
            mergedPolygons.add(createMergedPolygon(component));
        }
    }

    return mergedPolygons;
}
```

#### Phase 3: Connectivity Graph

```java
/**
 * Builds a connectivity graph between polygons.
 * Polygons are connected if:
 * 1. They're adjacent horizontally (same Y)
 * 2. They're reachable vertically (jump/climb height)
 */
public void buildConnectivityGraph(NavMeshRegion region) {
    for (NavPolygon polygon : region.getPolygons()) {
        // Horizontal neighbors (same Y-level)
        for (NavPolygon potentialNeighbor : region.getPolygons()) {
            if (polygon == potentialNeighbor) continue;

            if (areHorizontallyAdjacent(polygon, potentialNeighbor)) {
                polygon.addNeighbor(polygon, NavEdgeType.HORIZONTAL);
            }

            // Vertical neighbors (within jump/climb range)
            int yDiff = potentialNeighbor.getCenterY() - polygon.getCenterY();
            if (Math.abs(yDiff) <= MAX_JUMP_HEIGHT) {
                if (areVerticallyReachable(polygon, potentialNeighbor)) {
                    NavEdgeType edgeType = yDiff > 0 ?
                        NavEdgeType.JUMP_UP : NavEdgeType.FALL_DOWN;
                    polygon.addNeighbor(polygon, edgeType);
                }
            }

            // Stair/ladder connections
            if (canClimb(polygon, potentialNeighbor)) {
                polygon.addNeighbor(potentialNeighbor, NavEdgeType.CLIMB);
            }
        }
    }
}
```

### Generation Performance Optimization

**Lazy Generation Strategy:**
- Generate navmesh on-demand when crew member enters chunk radius
- Cache generated regions in memory with LRU eviction
- Use Caffeine cache with 100-region limit

**Parallel Generation:**
- Use Minecraft's worker threads for parallel region generation
- Each chunk processed independently
- Merge results on main thread

```java
// Lazy generation with caching
private final LoadingCache<ChunkPos, NavMeshRegion> regionCache = Caffeine.newBuilder()
    .maximumSize(100)
    .expireAfterAccess(Duration.ofMinutes(10))
    .build(this::generateRegionSync);

public CompletableFuture<NavMeshRegion> getRegionAsync(ChunkPos pos) {
    return CompletableFuture.supplyAsync(() -> regionCache.get(pos),
        MinecraftServer.getInstance());
}
```

---

## Pathfinding Algorithm

### Region-Based A* Pathfinding

The navmesh system uses a hierarchical A* approach:

1. **Inter-region pathfinding** (high-level)
   - Find sequence of regions to traverse
   - Use region centers as waypoints
   - Cache common routes

2. **Intra-region pathfinding** (detailed)
   - Standard A* within each region
   - Polygon-to-polygon navigation
   - Consider movement costs (jumping, climbing)

### Implementation

```java
/**
 * Finds a path from start to end using navmesh-based A*.
 */
public NavPath findPath(BlockPos start, BlockPos end, ForemanEntity entity) {
    // Get start and end regions
    ChunkPos startChunk = new ChunkPos(start);
    ChunkPos endChunk = new ChunkPos(end);

    NavMeshRegion startRegion = regionCache.get(startChunk);
    NavMeshRegion endRegion = regionCache.get(endChunk);

    if (startRegion == null || endRegion == null) {
        return null; // Path not possible
    }

    // High-level region pathfinding
    List<NavMeshRegion> regionPath = findRegionPath(startRegion, endRegion);

    if (regionPath.isEmpty()) {
        return null;
    }

    // Low-level polygon pathfinding
    NavPolygon startPoly = startRegion.getPolygonAt(start);
    NavPolygon endPoly = endRegion.getPolygonAt(end);

    List<NavPolygon> polygonPath = findPolygonPath(startPoly, endPoly, regionPath);

    // Convert to waypoints
    return createWaypointsFromPolygons(polygonPath);
}

/**
 * High-level A* across regions.
 */
private List<NavMeshRegion> findRegionPath(NavMeshRegion start, NavMeshRegion end) {
    PriorityQueue<RegionNode> openSet = new PriorityQueue<>();
    Map<NavMeshRegion, RegionNode> allNodes = new HashMap<>();

    RegionNode startNode = new RegionNode(start, null, 0,
        heuristicDistance(start, end));
    openSet.add(startNode);
    allNodes.put(start, startNode);

    while (!openSet.isEmpty()) {
        RegionNode current = openSet.poll();

        if (current.region == end) {
            return reconstructRegionPath(current);
        }

        for (NavMeshRegion neighbor : current.region.getNeighbors()) {
            double tentativeG = current.g +
                current.region.getCenter().distSqr(neighbor.getCenter());

            RegionNode neighborNode = allNodes.get(neighbor);
            if (neighborNode == null || tentativeG < neighborNode.g) {
                neighborNode = new RegionNode(neighbor, current, tentativeG,
                    tentativeG + heuristicDistance(neighbor, end));
                allNodes.put(neighbor, neighborNode);
                openSet.add(neighborNode);
            }
        }
    }

    return Collections.emptyList(); // No path found
}

/**
 * Low-level A* within regions using polygons.
 */
private List<NavPolygon> findPolygonPath(NavPolygon start, NavPolygon end,
                                         List<NavMeshRegion> regions) {
    PriorityQueue<PolygonNode> openSet = new PriorityQueue<>();
    Map<NavPolygon, PolygonNode> allNodes = new HashMap<>();

    PolygonNode startNode = new PolygonNode(start, null, 0,
        heuristicDistance(start, end));
    openSet.add(startNode);
    allNodes.put(start, startNode);

    while (!openSet.isEmpty()) {
        PolygonNode current = openSet.poll();

        if (current.polygon == end) {
            return reconstructPolygonPath(current);
        }

        for (NavEdge edge : current.polygon.getEdges()) {
            NavPolygon neighbor = edge.getTarget();
            if (!regions.contains(neighbor.getRegion())) continue;

            // Cost depends on edge type (jumping costs more than walking)
            double edgeCost = getEdgeCost(edge.getType());
            double tentativeG = current.g + edgeCost;

            PolygonNode neighborNode = allNodes.get(neighbor);
            if (neighborNode == null || tentativeG < neighborNode.g) {
                neighborNode = new PolygonNode(neighbor, current, tentativeG,
                    tentativeG + heuristicDistance(neighbor, end));
                allNodes.put(neighbor, neighborNode);
                openSet.add(neighborNode);
            }
        }
    }

    return Collections.emptyList();
}
```

### Path Cost Heuristics

```java
private double getEdgeCost(NavEdgeType edgeType) {
    return switch (edgeType) {
        case HORIZONTAL -> 1.0;        // Normal walking
        case JUMP_UP -> 2.0;           // Jumping costs 2x
        case FALL_DOWN -> 1.5;         // Falling costs 1.5x
        case CLIMB -> 3.0;             // Climbing ladders/vines
        case SWIM -> 2.5;              // Swimming is slower
        case FLY -> 0.5;               // Flying is fastest (for creative mode)
    };
}
```

### Path Smoothing

```java
/**
 * Post-processes path to remove unnecessary waypoints.
 * Uses line-of-sight checks to skip intermediate points.
 */
public List<BlockPos> smoothPath(List<BlockPos> originalPath, ServerLevel level) {
    if (originalPath.size() <= 2) return originalPath;

    List<BlockPos> smoothed = new ArrayList<>();
    smoothed.add(originalPath.get(0));

    int currentIndex = 0;
    while (currentIndex < originalPath.size() - 1) {
        int furthestVisible = currentIndex;

        // Find furthest visible point
        for (int i = originalPath.size() - 1; i > currentIndex; i--) {
            if (hasLineOfSight(level, originalPath.get(currentIndex),
                              originalPath.get(i))) {
                furthestVisible = i;
                break;
            }
        }

        smoothed.add(originalPath.get(furthestVisible));
        currentIndex = furthestVisible;
    }

    return smoothed;
}
```

---

## Dynamic Updates

### Block Change Monitoring

The navmesh must update when the world changes. This requires listening to block change events and efficiently updating affected regions.

### Event-Based Invalidation

```java
/**
 * Listens for block change events and invalidates affected navmesh regions.
 */
@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NavMeshUpdateHandler {

    private static final NavMeshUpdateScheduler scheduler =
        NavMeshUpdateScheduler.getInstance();

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockPos pos = event.getPos();
        ChunkPos chunkPos = new ChunkPos(pos);

        // Invalidate affected region
        scheduler.invalidateRegion(chunkPos);

        // Also invalidate adjacent chunks if near boundary
        if (isNearChunkBoundary(pos)) {
            for (ChunkPos neighbor : getNeighborChunks(chunkPos)) {
                scheduler.invalidateRegion(neighbor);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        ChunkPos chunkPos = new ChunkPos(pos);
        scheduler.invalidateRegion(chunkPos);

        if (isNearChunkBoundary(pos)) {
            for (ChunkPos neighbor : getNeighborChunks(chunkPos)) {
                scheduler.invalidateRegion(neighbor);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent event) {
        // Explosions affect large areas - invalidate all nearby chunks
        Vec3 center = event.getExplosion().getPosition();
        BlockPos centerPos = BlockPos.containing(center);
        double radius = event.getExplosion().getRadius();

        int chunkRadius = (int) Math.ceil(radius / 16);
        ChunkPos centerChunk = new ChunkPos(centerPos);

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                ChunkPos chunkPos = new ChunkPos(
                    centerChunk.x + x,
                    centerChunk.z + z
                );
                scheduler.invalidateRegion(chunkPos);
            }
        }
    }
}
```

### Update Scheduler

```java
/**
 * Manages deferred navmesh updates to avoid performance spikes.
 * Processes updates in batches during tick time.
 */
public class NavMeshUpdateScheduler {
    private static final NavMeshUpdateScheduler INSTANCE =
        new NavMeshUpdateScheduler();

    private final Set<ChunkPos> pendingUpdates = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkPos> updateQueue = new ConcurrentLinkedQueue<>();
    private final NavMeshManager navMeshManager = new NavMeshManager();

    private int updatesThisTick = 0;
    private static final int MAX_UPDATES_PER_TICK = 2;

    public static NavMeshUpdateScheduler getInstance() {
        return INSTANCE;
    }

    public void invalidateRegion(ChunkPos chunkPos) {
        if (pendingUpdates.add(chunkPos)) {
            updateQueue.add(chunkPos);
        }
    }

    /**
     * Called every server tick to process pending updates.
     */
    public void tick(ServerLevel level) {
        updatesThisTick = 0;

        while (updatesThisTick < MAX_UPDATES_PER_TICK && !updateQueue.isEmpty()) {
            ChunkPos chunkPos = updateQueue.poll();
            if (chunkPos == null) break;

            pendingUpdates.remove(chunkPos);

            // Regenerate navmesh for this region
            navMeshManager.regenerateRegion(level, chunkPos);

            updatesThisTick++;
        }
    }

    /**
     * Bulk invalidate multiple chunks at once.
     */
    public void invalidateBulk(Collection<ChunkPos> chunkPositions) {
        pendingUpdates.addAll(chunkPositions);
        updateQueue.addAll(chunkPositions);
    }
}
```

### Incremental Updates

For small changes (single block placement/breaking), use incremental updates:

```java
/**
 * Updates navmesh incrementally for small changes.
 * Only updates polygons directly affected by the block change.
 */
public void incrementallyUpdate(ServerLevel level, BlockPos changedBlock) {
    ChunkPos chunkPos = new ChunkPos(changedBlock);
    NavMeshRegion region = regionCache.getIfPresent(chunkPos);

    if (region == null) return;

    // Find polygons affected by this block
    List<NavPolygon> affectedPolygons = region.getPolygonsNear(changedBlock, 2);

    if (affectedPolygons.isEmpty()) {
        // No polygons affected - might need to add new ones
        if (isWalkableSurface(level, changedBlock)) {
            NavPolygon newPolygon = createPolygonFromColumn(level, changedBlock);
            if (newPolygon != null) {
                region.addPolygon(newPolygon);
                rebuildConnectivity(region, newPolygon);
            }
        }
    } else {
        // Remove affected polygons and regenerate
        for (NavPolygon polygon : affectedPolygons) {
            region.removePolygon(polygon);
        }

        // Regenerate polygons in affected area
        regeneratePolygonsInArea(level, region, changedBlock, 2);
    }
}
```

---

## Multi-Height Navigation

### Vertical Movement Capabilities

The navmesh supports various vertical movement types:

| Movement Type | Max Height | Cost | Requirements |
|---------------|------------|------|--------------|
| Walk | Same Y | 1.0x | Flat or gradual slope |
| Jump | +1 block | 2.0x | 2 blocks clearance above |
| Fall | -3 blocks | 1.5x | Safe landing required |
| Climb | Unlimited | 3.0x | Ladder, vine, scaffold |
| Stairs | +1 per step | 1.2x | Stairs block |
| Slab | +0.5 | 1.1x | Slab block |

### Jump Detection

```java
/**
 * Determines if an entity can jump from one polygon to another.
 */
public boolean canJumpBetween(NavPolygon from, NavPolygon to, ForemanEntity entity) {
    int yDiff = to.getCenterY() - from.getCenterY();

    // Can only jump up 1 block normally
    if (yDiff > 1) return false;

    // Can fall up to 3 blocks safely
    if (yDiff < -3) return false;

    // Check horizontal distance
    double horizDist = horizontalDistance(from, to);
    if (horizDist > MAX_JUMP_DISTANCE) return false;

    // Check clearance at landing
    if (!hasClearance(to, entity)) return false;

    // Check for obstacles in jump arc
    if (!hasClearJumpPath(from, to, entity)) return false;

    return true;
}

private boolean hasClearJumpPath(NavPolygon from, NavPolygon to, ForemanEntity entity) {
    // Sample points along the jump trajectory
    Vec3 start = Vec3.atCenterOf(from.getCenterBlock());
    Vec3 end = Vec3.atCenterOf(to.getCenterBlock());

    int samples = 5;
    for (int i = 1; i < samples; i++) {
        float t = (float) i / samples;

        // Parabolic jump arc
        float height = (float) (4 * t * (1 - t)); // Peak at t=0.5

        Vec3 sample = start.lerp(end, t).add(0, height, 0);
        BlockPos samplePos = BlockPos.containing(sample);

        // Check if obstacle at sample point
        if (!isPassable(entity.level(), samplePos)) {
            return false;
        }
    }

    return true;
}
```

### Climbing Support

```java
/**
 * Detects climbable surfaces (ladders, vines, scaffolding).
 */
public boolean isClimbable(ServerLevel level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    Block block = state.getBlock();

    return block instanceof LadderBlock ||
           block instanceof VineBlock ||
           block instanceof ScaffoldingBlock ||
           block == Blocks.POWDER_SNOW;
}

/**
 * Finds climbable connections between polygons.
 */
public List<NavEdge> findClimbableEdges(NavPolygon polygon) {
    List<NavEdge> edges = new ArrayList<>();

    // Check above for climbable blocks
    BlockPos above = polygon.getCenterBlock().above();
    if (isClimbable(polygon.getRegion().getLevel(), above)) {
        // Find polygon above
        NavPolygon abovePoly = findPolygonAbove(polygon);
        if (abovePoly != null) {
            edges.add(new NavEdge(abovePoly, NavEdgeType.CLIMB));
        }
    }

    // Check below for climbable blocks
    BlockPos below = polygon.getCenterBlock().below();
    if (isClimbable(polygon.getRegion().getLevel(), below)) {
        NavPolygon belowPoly = findPolygonBelow(polygon);
        if (belowPoly != null) {
            edges.add(new NavEdge(belowPoly, NavEdgeType.CLIMB));
        }
    }

    return edges;
}
```

### Stair and Slab Handling

```java
/**
 * Detects and handles stairs/slabs for smooth navigation.
 */
public boolean canTraverseStairs(NavPolygon from, NavPolygon to) {
    BlockPos fromBlock = from.getCenterBlock();
    BlockPos toBlock = to.getCenterBlock();

    BlockState toState = to.getRegion().getLevel().getBlockState(toBlock);
    Block toBlock = toState.getBlock();

    // Handle stairs
    if (toBlock instanceof StairBlock) {
        // Check if stairs face the right direction
        Direction stairDirection = toState.getValue(StairBlock.FACING);
        BlockPos expectedPos = fromBlock.relative(stairDirection.getOpposite());

        return expectedPos.equals(toBlock);
    }

    // Handle slabs
    if (toBlock instanceof SlabBlock) {
        SlabType slabType = toState.getValue(SlabBlock.TYPE);

        // Can only walk up top slab, down from top slab
        if (slabType == SlabType.TOP) {
            return from.getCenterY() <= to.getCenterY();
        }

        // Bottom slab acts like normal block
        return true;
    }

    return false;
}
```

---

## Obstacle Avoidance

### Dynamic Obstacle Detection

```java
/**
 * Detects dynamic obstacles (entities, doors, gates) in path.
 */
public class NavMeshCollisionSystem {

    /**
     * Checks if a position is currently blocked by a dynamic obstacle.
     */
    public boolean isPositionBlocked(ServerLevel level, BlockPos pos, ForemanEntity entity) {
        // Check for entities
        AABB searchBox = new AABB(pos).inflate(0.5);
        List<Entity> entities = level.getEntities(entity, searchBox);

        for (Entity other : entities) {
            if (other instanceof ForemanEntity) {
                return true; // Blocked by another crew member
            }
        }

        // Check for closed doors/gates
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            if (!state.getValue(DoorBlock.OPEN)) {
                return true; // Closed door
            }
        }

        if (state.getBlock() instanceof FenceGateBlock) {
            if (!state.getValue(FenceGateBlock.OPEN)) {
                return true; // Closed gate
            }
        }

        return false;
    }

    /**
     * Gets all obstacles along a path segment.
     */
    public List<BlockPos> getObstaclesAlongPath(ServerLevel level, List<BlockPos> path,
                                                ForemanEntity entity) {
        List<BlockPos> obstacles = new ArrayList<>();

        for (BlockPos pos : path) {
            if (isPositionBlocked(level, pos, entity)) {
                obstacles.add(pos);
            }
        }

        return obstacles;
    }
}
```

### Local Obstacle Avoidance

When following a path, crew members need to avoid dynamic obstacles:

```java
/**
 * Executes path with local obstacle avoidance.
 */
public class NavPathExecutor {
    private static final double OBSTACLE_AVOID_RADIUS = 2.0;
    private static final double PATH_DEVIATION_THRESHOLD = 1.5;

    private final ForemanEntity entity;
    private final List<BlockPos> path;
    private int currentPathIndex = 0;
    private final NavMeshCollisionSystem collisionSystem;

    public void tick() {
        if (currentPathIndex >= path.size()) {
            // Path complete
            return;
        }

        BlockPos nextWaypoint = path.get(currentPathIndex);

        // Check if next waypoint is blocked
        if (collisionSystem.isPositionBlocked(entity.level(), nextWaypoint, entity)) {
            // Try to find alternative route
            List<BlockPos> alternative = findAlternativePath(nextWaypoint);

            if (alternative != null && !alternative.isEmpty()) {
                // Insert alternative waypoints
                path.addAll(currentPathIndex, alternative);
            } else {
                // Wait for obstacle to clear
                return;
            }
        }

        // Check for nearby obstacles (local avoidance)
        List<BlockPos> nearbyObstacles = findNearbyObstacles();
        if (!nearbyObstacles.isEmpty()) {
            Vec3 avoidanceVector = calculateAvoidanceVector(nearbyObstacles);
            Vec3 adjustedTarget = applyAvoidance(nextWaypoint, avoidanceVector);

            entity.getNavigation().moveTo(
                adjustedTarget.x,
                adjustedTarget.y,
                adjustedTarget.z,
                1.0
            );
        } else {
            // Normal path following
            entity.getNavigation().moveTo(nextWaypoint.getX(),
                                         nextWaypoint.getY(),
                                         nextWaypoint.getZ(), 1.0);
        }

        // Advance to next waypoint if close enough
        if (entity.blockPosition().closerThan(nextWaypoint, 1.5)) {
            currentPathIndex++;
        }
    }

    private Vec3 calculateAvoidanceVector(List<BlockPos> obstacles) {
        Vec3 avoidance = Vec3.ZERO;

        for (BlockPos obstacle : obstacles) {
            Vec3 toObstacle = Vec3.atCenterOf(obstacle).subtract(entity.position());
            double distance = toObstacle.length();

            if (distance < OBSTACLE_AVOID_RADIUS && distance > 0) {
                // Add repulsion vector (inversely proportional to distance)
                Vec3 repulsion = toObstacle.normalize().scale(
                    (OBSTACLE_AVOID_RADIUS - distance) / OBSTACLE_AVOID_RADIUS
                );
                avoidance = avoidance.add(repulsion);
            }
        }

        return avoidance.normalize();
    }

    private Vec3 applyAvoidance(BlockPos target, Vec3 avoidanceVector) {
        Vec3 targetVec = Vec3.atCenterOf(target);
        Vec3 toTarget = targetVec.subtract(entity.position()).normalize();

        // Blend target direction with avoidance
        Vec3 blended = toTarget.add(avoidanceVector.scale(0.7)).normalize();

        return entity.position().add(blended.scale(2.0));
    }
}
```

---

## Integration Guide

### Integrating with Existing Actions

#### 1. Replace PathfindAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.navigation.*;
import net.minecraft.core.BlockPos;

public class PathfindAction extends BaseAction {
    private NavPath navPath;
    private NavPathExecutor pathExecutor;
    private int ticksRunning;
    private static final int MAX_TICKS = 600;

    public PathfindAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);

        BlockPos targetPos = new BlockPos(x, y, z);
        ticksRunning = 0;

        // Use navmesh system
        NavMeshManager navMeshManager = MineWrightMod.getNavMeshManager();

        navPath = navMeshManager.findPath(
            foreman.blockPosition(),
            targetPos,
            foreman
        );

        if (navPath == null || navPath.isEmpty()) {
            result = ActionResult.failure("No path found to target");
            return;
        }

        pathExecutor = new NavPathExecutor(foreman, navPath);
        pathExecutor.start();
    }

    @Override
    protected void onTick() {
        ticksRunning++;

        if (pathExecutor != null) {
            pathExecutor.tick();

            if (pathExecutor.isComplete()) {
                if (pathExecutor.reachedDestination()) {
                    result = ActionResult.success("Reached target position");
                } else {
                    result = ActionResult.failure("Failed to reach destination");
                }
                return;
            }
        }

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Pathfinding timeout");
            pathExecutor.cancel();
        }
    }

    @Override
    protected void onCancel() {
        if (pathExecutor != null) {
            pathExecutor.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Navigate to " + (navPath != null ? navPath.getDestination() : "unknown");
    }
}
```

#### 2. Update ForemanEntity

```java
package com.minewright.entity;

import com.minewright.navigation.NavMeshManager;
// ... other imports

public class ForemanEntity extends PathfinderMob {
    // ... existing fields

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing tick logic

            // Tick navmesh update scheduler
            NavMeshUpdateScheduler.getInstance().tick((ServerLevel) level());
        }
    }
}
```

#### 3. Register with Main Mod Class

```java
package com.minewright;

import com.minewright.navigation.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public class MineWrightMod {
    // ... existing fields

    private static NavMeshManager navMeshManager;
    private static NavMeshUpdateScheduler updateScheduler;

    public MineWrightMod() {
        // Initialize navmesh system
        navMeshManager = new NavMeshManager();
        updateScheduler = NavMeshUpdateScheduler.getInstance();

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new NavMeshUpdateHandler());

        // Register tick handler
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            updateScheduler.tick();
        }
    }

    public static NavMeshManager getNavMeshManager() {
        return navMeshManager;
    }

    public static NavMeshUpdateScheduler getNavMeshUpdateScheduler() {
        return updateScheduler;
    }
}
```

### Configuration

Add to `config/minewright-common.toml`:

```toml
[navigation]
# Enable navmesh system (false = use vanilla pathfinding)
enabled = true

# Maximum navmesh regions to cache in memory
max_cached_regions = 100

# Maximum regions to update per server tick
max_updates_per_tick = 2

# Jump height (in blocks) for pathfinding
jump_height = 1

# Maximum safe fall distance (in blocks)
max_fall_distance = 3

# Enable path smoothing
smooth_paths = true

# Enable dynamic obstacle avoidance
obstacle_avoidance = true

# Debug rendering (client-side)
debug_render_navmesh = false
debug_render_path = false
```

---

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1)

**Tasks:**
- [ ] Create base package structure: `com.minewright.navigation`
- [ ] Implement `NavPolygon` class with basic properties
- [ ] Implement `NavMeshRegion` for 16x16 chunk areas
- [ ] Create `NavMeshManager` with region caching
- [ ] Implement basic walkable surface detection
- [ ] Add unit tests for surface detection

**Files to create:**
- `src/main/java/com/minewright/navigation/NavPolygon.java`
- `src/main/java/com/minewright/navigation/NavMeshRegion.java`
- `src/main/java/com/minewright/navigation/NavMeshManager.java`
- `src/main/java/com/minewright/navigation/NavEdgeType.java` (enum)
- `src/main/java/com/minewright/navigation/NavEdge.java`

### Phase 2: Pathfinding (Week 2)

**Tasks:**
- [ ] Implement region-based A* pathfinding
- [ ] Implement polygon-level pathfinding within regions
- [ ] Create `NavPath` class to represent computed paths
- [ ] Add path smoothing algorithm
- [ ] Implement `NavPathExecutor` for following paths
- [ ] Add integration tests for pathfinding

**Files to create:**
- `src/main/java/com/minewright/navigation/NavPath.java`
- `src/main/java/com/minewright/navigation/NavPathfinder.java`
- `src/main/java/com/minewright/navigation/NavPathExecutor.java`
- `src/test/java/com/minewright/navigation/NavPathfinderTest.java`

### Phase 3: Dynamic Updates (Week 3)

**Tasks:**
- [ ] Implement `NavMeshUpdateScheduler`
- [ ] Create `NavMeshUpdateHandler` for block change events
- [ ] Add incremental update logic
- [ ] Implement bulk update for explosions
- [ ] Add performance monitoring and metrics
- [ ] Create update strategy tests

**Files to create:**
- `src/main/java/com/minewright/navigation/NavMeshUpdateScheduler.java`
- `src/main/java/com/minewright/navigation/NavMeshUpdateHandler.java`
- `src/test/java/com/minewright/navigation/NavMeshUpdateTest.java`

### Phase 4: Multi-Height Navigation (Week 4)

**Tasks:**
- [ ] Implement jump detection and validation
- [ ] Add climbing support (ladders, vines)
- [ ] Handle stairs and slab navigation
- [ ] Implement fall damage prediction
- [ ] Add vertical movement cost heuristics
- [ ] Create vertical navigation tests

**Files to create:**
- `src/main/java/com/minewright/navigation/VerticalNavigation.java`
- `src/main/java/com/minewright/navigation/JumpValidator.java`
- `src/test/java/com/minewright/navigation/VerticalNavigationTest.java`

### Phase 5: Obstacle Avoidance (Week 5)

**Tasks:**
- [ ] Implement `NavMeshCollisionSystem`
- [ ] Add dynamic obstacle detection (entities, doors)
- [ ] Create local avoidance algorithm
- [ ] Implement alternative path finding
- [ ] Add multi-agent collision avoidance
- [ ] Create collision system tests

**Files to create:**
- `src/main/java/com/minewright/navigation/NavMeshCollisionSystem.java`
- `src/main/java/com/minewright/navigation/ObstacleAvoidance.java`
- `src/test/java/com/minewright/navigation/CollisionSystemTest.java`

### Phase 6: Integration & Optimization (Week 6)

**Tasks:**
- [ ] Update `PathfindAction` to use navmesh
- [ ] Update `FollowPlayerAction` to use navmesh
- [ ] Integrate with existing action system
- [ ] Add configuration options
- [ ] Performance profiling and optimization
- [ ] Memory usage optimization
- [ ] Add fallback to vanilla pathfinding
- [ ] Create integration tests

### Phase 7: Testing & Polish (Week 7-8)

**Tasks:**
- [ ] Comprehensive testing in various biomes
- [ ] Edge case testing (caves, nether, end)
- [ ] Multi-agent testing
- [ ] Performance benchmarking
- [ ] Documentation completion
- [ ] Code review and refinement
- [ ] Debug visualization tools (optional)

### Phase 8: Release Preparation (Week 9)

**Tasks:**
- [ ] Final integration testing
- [ ] Performance regression testing
- [ ] User documentation
- [ ] Release notes preparation
- [ ] Deployment to production

---

## Appendix: Code Examples

### Example 1: Complete NavPolygon Class

```java
package com.minewright.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class NavPolygon {
    private final UUID id;
    private final NavMeshRegion region;
    private final AABB bounds;
    private final int centerY;
    private final List<NavEdge> edges;
    private final Set<BlockPos> blocks;

    public NavPolygon(NavMeshRegion region, Collection<BlockPos> blocks) {
        this.id = UUID.randomUUID();
        this.region = region;
        this.blocks = new HashSet<>(blocks);
        this.bounds = calculateBounds();
        this.centerY = calculateCenterY();
        this.edges = new ArrayList<>();
    }

    private AABB calculateBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double maxZ = Double.MIN_VALUE;

        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private int calculateCenterY() {
        int sum = 0;
        for (BlockPos pos : blocks) {
            sum += pos.getY();
        }
        return sum / blocks.size();
    }

    public void addEdge(NavPolygon target, NavEdgeType type) {
        edges.add(new NavEdge(target, type));
    }

    public boolean isWalkable() {
        Level level = region.getLevel();

        // Check if all blocks in this polygon are still walkable
        for (BlockPos pos : blocks) {
            if (!isWalkableSurface(level, pos)) {
                return false;
            }
        }

        return true;
    }

    private boolean isWalkableSurface(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState current = level.getBlockState(pos);
        BlockState above1 = level.getBlockState(pos.above());
        BlockState above2 = level.getBlockState(pos.above(2));

        return below.isSolid() &&
               current.isAir() &&
               above1.isAir() &&
               above2.isAir();
    }

    public Vec3 getCenter() {
        return bounds.getCenter();
    }

    public BlockPos getCenterBlock() {
        Vec3 center = getCenter();
        return new BlockPos(
            (int) Math.floor(center.x),
            centerY,
            (int) Math.floor(center.z)
        );
    }

    // Getters
    public UUID getId() { return id; }
    public NavMeshRegion getRegion() { return region; }
    public AABB getBounds() { return bounds; }
    public int getCenterY() { return centerY; }
    public List<NavEdge> getEdges() { return Collections.unmodifiableList(edges); }
    public Set<BlockPos> getBlocks() { return Collections.unmodifiableSet(blocks); }
}
```

### Example 2: Complete NavPath Class

```java
package com.minewright.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class NavPath {
    private final List<BlockPos> waypoints;
    private final BlockPos destination;
    private final double totalDistance;
    private final int estimatedTicks;
    private boolean isSmoothed;

    public NavPath(List<BlockPos> waypoints) {
        this.waypoints = new ArrayList<>(waypoints);
        this.destination = waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1);
        this.totalDistance = calculateTotalDistance();
        this.estimatedTicks = (int) (totalDistance * 10); // Rough estimate
        this.isSmoothed = false;
    }

    private double calculateTotalDistance() {
        if (waypoints.size() < 2) return 0;

        double distance = 0;
        Vec3 previous = Vec3.atCenterOf(waypoints.get(0));

        for (int i = 1; i < waypoints.size(); i++) {
            Vec3 current = Vec3.atCenterOf(waypoints.get(i));
            distance += previous.distanceTo(current);
            previous = current;
        }

        return distance;
    }

    public BlockPos getNextWaypoint(int currentIndex) {
        if (currentIndex >= waypoints.size()) {
            return null;
        }
        return waypoints.get(currentIndex);
    }

    public void smooth(List<BlockPos> smoothed) {
        waypoints.clear();
        waypoints.addAll(smoothed);
        isSmoothed = true;
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public int size() {
        return waypoints.size();
    }

    // Getters
    public List<BlockPos> getWaypoints() {
        return new ArrayList<>(waypoints);
    }
    public BlockPos getDestination() { return destination; }
    public double getTotalDistance() { return totalDistance; }
    public int getEstimatedTicks() { return estimatedTicks; }
    public boolean isSmoothed() { return isSmoothed; }
}
```

---

## Performance Considerations

### Memory Usage

| Component | Memory per Unit | Max Units | Total Memory |
|-----------|-----------------|-----------|--------------|
| NavPolygon | ~200 bytes | 10,000 | ~2 MB |
| NavMeshRegion | ~50 KB | 100 | ~5 MB |
| Cached Paths | ~1 KB | 50 | ~50 KB |

**Estimated total memory usage:** ~10-15 MB for loaded areas

### CPU Usage

| Operation | Time (ms) | Frequency |
|-----------|-----------|-----------|
| Region Generation | 50-200 | On chunk load |
| Pathfinding (short) | 1-5 | Per action |
| Pathfinding (long) | 10-50 | Per action |
| Region Update | 20-100 | On block change |
| Collision Check | <1 | Per tick |

### Optimization Strategies

1. **Spatial Partitioning:** Use chunk-based regions to limit search space
2. **Path Caching:** Cache frequently used routes
3. **Lazy Updates:** Batch navmesh updates over multiple ticks
4. **Level of Detail:** Use simpler pathfinding for distant targets
5. **Early Exit:** Stop pathfinding if target is unreachable

---

## Troubleshooting

### Common Issues

**Issue:** Crew members getting stuck on obstacles
- **Solution:** Increase obstacle avoidance radius or improve local avoidance

**Issue:** Pathfinding lag on block changes
- **Solution:** Reduce `max_updates_per_tick` or increase update batching

**Issue:** Crew members falling from heights
- **Solution:** Adjust `max_fall_distance` or improve fall damage prediction

**Issue:** High memory usage
- **Solution:** Reduce `max_cached_regions` or implement more aggressive eviction

### Debug Mode

Enable debug rendering in config:
```toml
[navigation]
debug_render_navmesh = true
debug_render_path = true
```

This will:
- Highlight navmesh polygons in blue
- Draw active paths in yellow
- Show blocked polygons in red
- Display waypoint markers

---

## Conclusion

This navmesh system provides MineWright with advanced navigation capabilities that far exceed vanilla Minecraft pathfinding. The region-based approach ensures scalability while the dynamic update system keeps the navmesh current with world changes.

The modular design allows for incremental implementation and easy extension with new features. Integration with the existing action system is straightforward and backward-compatible.

**Next Steps:**
1. Review and approve this design
2. Begin Phase 1 implementation
3. Set up weekly progress reviews
4. Establish testing criteria for each phase

**Estimated Timeline:** 9 weeks for full implementation and testing.
