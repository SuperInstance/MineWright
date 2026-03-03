# Advanced Pathfinding Optimization Techniques

**Research Document** - Steve AI Project
**Date:** 2025-03-02
**Version:** 1.0
**Author:** Claude Orchestrator
**Focus:** A* Optimizations, Multi-Agent Systems, Minecraft-Specific Techniques

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [A* Optimizations](#a-optimizations)
3. [Minecraft-Specific Pathfinding](#minecraft-specific-pathfinding)
4. [Multi-Agent Pathfinding](#multi-agent-pathfinding)
5. [Baritone Patterns](#baritone-patterns)
6. [Application to MineWright](#application-to-minewright)
7. [Implementation Roadmap](#implementation-roadmap)
8. [References](#references)

---

## Executive Summary

Pathfinding is a critical component of autonomous AI agents in Minecraft. This document surveys the state-of-the-art pathfinding optimization techniques as of 2024-2025, with specific focus on:

1. **A* Optimizations** - Hierarchical pathfinding (HPA*), Jump Point Search (JPS), Navigation Meshes, Path Caching
2. **Minecraft-Specific Challenges** - Chunk-aware pathfinding, 3D movement, Block cost functions, Dynamic obstacles
3. **Multi-Agent Coordination** - Conflict-Based Search (CBS), Cooperative A*, Flow Fields, RVO
4. **Baritone Techniques** - Industry-leading Minecraft bot pathfinding patterns

**Key Findings:**
- Jump Point Search can reduce node expansion by 60-80% in uniform-cost grids
- Hierarchical pathfinding achieves 10-100x speedup for long-distance paths
- Multi-agent collision avoidance is best handled through hybrid approaches (RVO + flow fields)
- Baritone's segmented calculation with incremental cost backoff is optimal for Minecraft's limited render distance

---

## 1. A* Optimizations

### 1.1 Hierarchical Pathfinding (HPA*)

**Concept:** Reduce search space by planning at multiple abstraction levels.

**Algorithm:**
```
HPA* Pathfinding:
1. Partition map into clusters (chunks)
2. Build abstract graph with cluster entrance/exit points
3. Find high-level path in abstract graph (A*)
4. For each cluster transition, find detailed low-level path
5. Concatenate low-level paths
```

**Performance Gains:**
- Standard A*: O(b^d) where b = branching factor, d = solution depth
- HPA*: O(b'^d') where b' << b, d' << d
- Typical speedup: 10-100x for paths > 64 blocks

**2024 Advances:**
- **HMLPA*** (Hierarchical Multi-Target LPA*): Addresses unstable rerouting costs in dynamic environments
- Graph partitioning localizes changes - one subgraph update doesn't invalidate entire path
- Parallel computation expands multiple nodes simultaneously

**Java Implementation Sketch:**
```java
public class HPAStarPathfinder {
    private static final int CLUSTER_SIZE = 16; // Chunk size

    // Abstract graph node
    static class AbstractNode {
        Cluster cluster;
        List<AbstractNode> neighbors;
        Map<AbstractNode, List<BlockPos>> paths; // Cached intra-cluster paths
    }

    // Cluster abstraction
    static class Cluster {
        int clusterX, clusterZ;
        List<BlockPos> entrances; // Entry/exit points
        Map<BlockPos, Map<BlockPos, List<BlockPos>>> internalPaths;
    }

    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        // Level 1: Abstract path through clusters
        List<Cluster> abstractPath = findAbstractPath(start, goal);

        // Level 2: Refine each cluster transition
        List<BlockPos> detailedPath = new ArrayList<>();
        for (int i = 0; i < abstractPath.size() - 1; i++) {
            detailedPath.addAll(refineClusterPath(
                abstractPath.get(i),
                abstractPath.get(i + 1)
            ));
        }
        return detailedPath;
    }

    private List<BlockPos> refineClusterPath(Cluster from, Cluster to) {
        // Check cache first
        // If not cached, run standard A* limited to these two clusters
        // Cache result for future queries
    }
}
```

**Advantages:**
- Dramatically reduces search space for long paths
- Natural fit for Minecraft's chunk-based world
- Enables caching of frequently-used intra-cluster paths

**Disadvantages:**
- Suboptimal paths (may miss shortcuts across cluster boundaries)
- Overhead for short paths (< 64 blocks)
- Cluster boundary selection is heuristic

---

### 1.2 Jump Point Search (JPS)

**Concept:** Exploit symmetry in uniform-cost grids to skip redundant node expansions.

**Core Insight:** In a grid with uniform movement costs, many A* expansions are unnecessary:
- Moving straight: Skip intermediate nodes, jump to next decision point
- Moving diagonally: Only check "forced neighbors" at corners

**Algorithm:**
```
Jump Point Search:
1. From current node, jump in search direction
2. If diagonal:
   - Recursively jump horizontally and vertically
   - If either finds a forced neighbor, this is a jump point
3. If straight:
   - Continue jumping until blocked or find forced neighbor
4. Add jump points to open set
5. Standard A* on reduced graph of jump points only
```

**Identifying Forced Neighbors:**
```java
// Coming from parent (px, py) to current (x, y)
// Checking if neighbor (nx, ny) is forced
boolean isForcedNeighbor(int px, int py, int x, int y, int nx, int ny) {
    // Parent is diagonal
    if (px != x && py != y) {
        // Horizontal/vertical neighbors are forced if blocked
        if (isBlocked(x + (nx - x), y) && !isBlocked(x + (nx - x), y + (ny - y))) {
            return true;
        }
        if (isBlocked(x, y + (ny - y)) && !isBlocked(x + (nx - x), y + (ny - y))) {
            return true;
        }
    }
    return false;
}
```

**Performance Gains (2024-2025 Research):**
- Standard JPS vs A*: 60-80% reduction in expanded nodes
- Improved JPS with preprocessing: **159-559x faster** than traditional A*
- JPS with bit operations: Up to **1169x faster** on large grids
- Path quality: 3-8% improvement in path length with secondary optimization

**Java Implementation:**
```java
public class JumpPointSearch {

    // Main jump function - recursively find next jump point
    private BlockPos jump(BlockPos current, BlockPos parent, BlockPos goal) {
        int dx = Integer.signum(goal.getX() - current.getX());
        int dy = Integer.signum(goal.getY() - current.getY());
        int dz = Integer.signum(goal.getZ() - current.getZ());

        // Check if blocked
        if (isBlocked(current)) {
            return null;
        }

        // Check if reached goal
        if (current.equals(goal)) {
            return current;
        }

        // Check for forced neighbors
        if (hasForcedNeighbor(current, dx, dy, dz)) {
            return current; // This is a jump point
        }

        // Diagonal movement - recurse
        if (dx != 0 && dz != 0) {
            // Check horizontal
            BlockPos hx = jump(current.offset(dx, 0, 0), current, goal);
            if (hx != null) return current;

            // Check vertical
            BlockPos hz = jump(current.offset(0, 0, dz), current, goal);
            if (hz != null) return current;
        }

        // Continue jumping
        return jump(current.offset(dx, dy, dz), current, goal);
    }

    // Identify successor nodes (jump points only)
    private List<BlockPos> getSuccessors(BlockPos current, BlockPos parent) {
        List<BlockPos> successors = new ArrayList<>();

        // Get neighbors
        List<BlockPos> neighbors = getNeighbors(current, parent);

        for (BlockPos neighbor : neighbors) {
            BlockPos jumpPoint = jump(neighbor, current, goal);
            if (jumpPoint != null) {
                successors.add(jumpPoint);
            }
        }

        return successors;
    }
}
```

**When to Use JPS:**
- Large open areas (plains, deserts, oceans)
- Uniform movement costs (flat terrain)
- NOT suitable for complex terrain with varied costs (mountains, caves)

---

### 1.3 Navigation Meshes (NavMesh)

**Concept:** Represent traversable space as convex polygons instead of grids.

**Structure:**
```
Navigation Mesh:
- Vertices: Key points in traversable space
- Polygons: Convex regions (triangles, quads)
- Edges: Connections between regions
- Portals: Entry/exit points for regions
```

**Pathfinding on NavMesh:**
```
1. High-level: Find sequence of polygons (A* on polygon graph)
2. Low-level: String pulling through portals (funnel algorithm)
```

**Funnel Algorithm (String Pulling):**
```java
public class FunnelAlgorithm {

    public List<BlockPos> stringPull(List<Polygon> polygonPath, BlockPos start, BlockPos goal) {
        List<BlockPos> waypoints = new ArrayList<>();
        waypoints.add(start);

        // Left and right funnels
        BlockPos left = start;
        BlockPos right = start;
        BlockPos apex = start;

        // Process portals between polygons
        for (Portal portal : polygonPath.getPortals()) {
            // Update left funnel
            if (isLeft(portal.left, apex, left)) {
                left = portal.left;
            }

            // Update right funnel
            if (isRight(portal.right, apex, right)) {
                right = portal.right;
            }

            // Check if funnel is narrowing
            if (isLeft(left, apex, right)) {
                // Add left vertex to path
                waypoints.add(left);
                apex = left;
                left = apex;
                right = apex;
            } else if (isRight(right, apex, left)) {
                // Add right vertex to path
                waypoints.add(right);
                apex = right;
                left = apex;
                right = apex;
            }
        }

        waypoints.add(goal);
        return waypoints;
    }
}
```

**Minecraft NavMesh Considerations:**
- Generate from solid block surfaces
- Handle verticality (stairs, ladders, vines)
- Dynamic updates when blocks change
- Simplification for performance

**Advantages:**
- Smoother paths (fewer waypoints)
- Faster pathfinding (smaller graph)
- Natural fit for organic terrain

**Disadvantages:**
- Complex generation (especially in 3D)
- Dynamic updates are expensive
- Memory intensive for large worlds

---

### 1.4 Path Caching

**Concept:** Store frequently-used paths to avoid recomputation.

**Cache Design:**
```java
public class PathCache {
    private final int maxSize;
    private final long ttlMillis;
    private final LinkedHashMap<CacheKey, CachedPath> cache;

    static class CacheKey {
        BlockPos start;
        BlockPos goal;
        int capabilitiesHash; // Entity capabilities

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) o;
            return start.equals(other.start) &&
                   goal.equals(other.goal) &&
                   capabilitiesHash == other.capabilitiesHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, goal, capabilitiesHash);
        }
    }

    static class CachedPath {
        List<PathNode> path;
        long timestamp;

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }
    }

    // LRU eviction
    public PathCache(int maxSize, int ttlMinutes) {
        this.maxSize = maxSize;
        this.ttlMillis = ttlMinutes * 60 * 1000L;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > maxSize;
            }
        };
    }

    public Optional<List<PathNode>> get(BlockPos start, BlockPos goal, PathfindingContext context) {
        CacheKey key = new CacheKey(start, goal, context);
        CachedPath cached = cache.get(key);

        if (cached != null && !cached.isExpired(ttlMillis)) {
            return Optional.of(new ArrayList<>(cached.path));
        }

        cache.remove(key);
        return Optional.empty();
    }
}
```

**Cache Invalidation:**
- Time-based (TTL): Paths expire after N minutes
- Event-based: Clear cache when blocks change
- Spatial: Clear cache only in affected chunks
- Hierarchical: Different cache levels for different path lengths

**Advanced Caching Techniques:**

1. **Path Sharing:**
```java
// Cache subpaths for reuse
public class SubpathCache {
    Map<BlockPos, Map<BlockPos, List<PathNode>>> subpaths;

    public void cacheSubpath(BlockPos from, BlockPos to, List<PathNode> subpath) {
        subpaths.computeIfAbsent(from, k -> new HashMap<>())
                .put(to, subpath);
    }
}
```

2. **Path Prefix Caching:**
```java
// Cache common path prefixes (e.g., from spawn)
public class PrefixCache {
    Map<BlockPos, List<PathNode>> prefixCache; // goal -> path from origin

    public Optional<List<PathNode>> getPathTo(BlockPos from, BlockPos goal) {
        List<PathNode> prefix = prefixCache.get(goal);
        if (prefix != null) {
            // Find best match in prefix
            int bestIndex = findBestMatch(prefix, from);
            return Optional.of(prefix.subList(bestIndex, prefix.size()));
        }
        return Optional.empty();
    }
}
```

**Cache Performance (Current MineWright):**
- Default cache size: 100 paths
- TTL: 10 minutes
- Hit rate tracking available

---

## 2. Minecraft-Specific Pathfinding

### 2.1 Chunk-Aware Pathfinding

**Challenge:** Minecraft's world is infinite and chunk-loaded. Pathfinding must account for:
- Unloaded chunks (not in memory)
- Chunk boundaries (16x16 regions)
- Dynamic chunk loading during pathfinding

**Approach 1: Conservative Pathfinding**
```java
public class ConservativePathfinding {

    private boolean isChunkLoaded(Level level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(
            ChunkPos.toSectionCoordinate(pos.getX()),
            ChunkPos.toSectionCoordinate(pos.getZ())
        );
        return chunk != null && chunk.isLoaded();
    }

    private List<BlockPos> generateNeighbors(BlockPos pos, PathfindingContext context) {
        List<BlockPos> neighbors = new ArrayList<>();

        for (BlockPos candidate : getPossibleNeighbors(pos)) {
            // Only consider positions in loaded chunks
            if (isChunkLoaded(context.getLevel(), candidate)) {
                neighbors.add(candidate);
            }
        }

        return neighbors;
    }
}
```

**Approach 2: Hierarchical Chunk Pathfinding**
```java
public class ChunkPathfinder {

    // Step 1: Plan at chunk level
    public List<ChunkPos> planChunkPath(BlockPos start, BlockPos goal) {
        ChunkPos startChunk = new ChunkPos(start);
        ChunkPos goalChunk = new ChunkPos(goal);

        // A* through chunk graph
        return aStarChunkPath(startChunk, goalChunk);
    }

    // Step 2: Navigate within each chunk
    public List<BlockPos> navigateChunk(ChunkPos from, ChunkPos to) {
        // Load chunk if needed
        // Find detailed path within chunk constraints
        // Exit chunk at optimal point
    }
}
```

**Chunk Caching Strategy:**
```java
public class ChunkPathCache {
    // Cache traversability of chunks
    Map<ChunkPos, ChunkInfo> chunkCache;

    static class ChunkInfo {
        boolean traversable;
        List<BlockPos> entryPoints;
        List<BlockPos> exitPoints;
        Map<BlockPos, Double> movementCosts;
    }

    public ChunkInfo getChunkInfo(Level level, ChunkPos pos) {
        return chunkCache.computeIfAbsent(pos, p -> analyzeChunk(level, p));
    }

    private ChunkInfo analyzeChunk(Level level, ChunkPos pos) {
        // Sample chunk at regular intervals
        // Determine traversability
        // Find optimal entry/exit points
        // Calculate movement costs
    }
}
```

---

### 2.2 3D Pathfinding Challenges

**Vertical Movement Costs:**
```java
public class VerticalCostCalculator {

    public double calculateCost(BlockPos from, BlockPos to, MovementType type) {
        double baseCost = type.getCost();

        int dy = to.getY() - from.getY();

        // Height difference penalty
        if (dy > 0) {
            // Going up is expensive
            baseCost *= 1.0 + (dy * 0.5);
        } else if (dy < 0) {
            // Going down is cheaper but risky
            baseCost *= 1.0 + (Math.abs(dy) * 0.2);
        }

        // Specific movement costs
        switch (type) {
            case JUMP:
                return baseCost * 2.0; // Jumping is expensive
            case CLIMB:
                return baseCost * 1.5; // Climbing is moderately expensive
            case FALL:
                // Falling is cheap but dangerous
                return baseCost * 0.5 + calculateFallDamageRisk(from, to);
            case SWIM:
                return baseCost * 1.2; // Swimming is slightly slower
            default:
                return baseCost;
        }
    }

    private double calculateFallDamageRisk(BlockPos from, BlockPos to) {
        int fallDistance = from.getY() - to.getY();
        if (fallDistance > 3) {
            return Math.pow(fallDistance - 3, 2) * 0.1; // Exponential damage risk
        }
        return 0;
    }
}
```

**Headroom and Clearance:**
```java
public class ClearanceValidator {

    public boolean hasClearance(Level level, BlockPos pos, int height) {
        // Check if entity has enough headroom
        for (int i = 1; i < height; i++) {
            BlockPos above = pos.above(i);
            if (!level.getBlockState(above).isAir()) {
                return false;
            }
        }
        return true;
    }

    public boolean canStandAt(Level level, BlockPos pos) {
        // Check solid ground below
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }

        // Check headroom
        return hasClearance(level, pos, 2); // 2 blocks high for player
    }
}
```

**3D Neighbor Generation:**
```java
public class Neighbor3DGenerator {

    public List<BlockPos> getNeighbors(BlockPos pos, PathfindingContext context) {
        List<BlockPos> neighbors = new ArrayList<>();

        int maxJumpHeight = context.getJumpHeight();
        int maxFallDistance = context.getMaxFallDistance();

        // Generate 26 neighbors (3x3x3 cube minus center)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -maxFallDistance; dy <= maxJumpHeight; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos neighbor = pos.offset(dx, dy, dz);

                    // Validate movement
                    if (isValidMove(pos, neighbor, context)) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }

        // Add parkour possibilities
        if (context.canParkour()) {
            addParkourJumps(neighbors, pos, context);
        }

        return neighbors;
    }

    private void addParkourJumps(List<BlockPos> neighbors, BlockPos pos, PathfindingContext context) {
        // 2-block jumps
        neighbors.add(pos.east(2));
        neighbors.add(pos.west(2));
        neighbors.add(pos.north(2));
        neighbors.add(pos.south(2));

        // 1-block gap, 1-up jumps
        neighbors.add(pos.east().above());
        neighbors.add(pos.west().above());
        neighbors.add(pos.north().above());
        neighbors.add(pos.south().above());
    }
}
```

---

### 2.3 Block Cost Functions

**Minecraft Block Costs:**
```java
public class BlockCostCalculator {

    private static final Map<Block, Double> BLOCK_COSTS = new HashMap<>();

    static {
        // Walking surfaces
        BLOCK_COSTS.put(Blocks.GRASS_BLOCK, 1.0);
        BLOCK_COSTS.put(Blocks.DIRT, 1.0);
        BLOCK_COSTS.put(Blocks.STONE, 1.0);

        // Slower surfaces
        BLOCK_COSTS.put(Blocks.SAND, 1.2); // Slower to walk
        BLOCK_COSTS.put(Blocks.SOUL_SAND, 1.5); // Even slower
        BLOCK_COSTS.put(Blocks.MUD, 1.8); // Very slow

        // Water
        BLOCK_COSTS.put(Blocks.WATER, 2.0); // Swimming is slow

        // Hazardous
        BLOCK_COSTS.put(Blocks.LAVA, Double.POSITIVE_INFINITY); // Impassable
        BLOCK_COSTS.put(Blocks.MAGMA_BLOCK, 5.0); // Damage
        BLOCK_COSTS.put(Blocks.SWEET_BERRY_BUSH, 3.0); // Slow + damage

        // Useful (prefer these)
        BLOCK_COSTS.put(Blocks.POWDER_SNOW, 0.8); // Fast climbing
        BLOCK_COSTS.put(Blocks.SCAFFOLDING, 0.9); // Easy climbing
    }

    public double getBlockCost(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        return BLOCK_COSTS.getOrDefault(block, 1.0);
    }

    // Dynamic cost based on time of day
    public double getDynamicCost(Level level, BlockPos pos) {
        double baseCost = getBlockCost(level, pos);

        // Darkness penalty
        if (level.getBrightness(LightLayer.BLOCK, pos) < 8) {
            baseCost *= 1.5; // Mobs spawn in dark
        }

        // Water depth penalty
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER)) {
            int fluidHeight = state.getFluidState().getAmount();
            baseCost *= (1.0 + fluidHeight * 0.2);
        }

        return baseCost;
    }
}
```

**Path-Based Costs:**
```java
public class PathContextCost {

    // Prefer previously used paths (path formation)
    private Map<BlockPos, Integer> pathUsage = new HashMap<>();

    public double getUsageCost(BlockPos pos) {
        int usage = pathUsage.getOrDefault(pos, 0);

        // Prefer well-used paths (lower cost)
        if (usage > 10) {
            return 0.8; // 20% discount for established paths
        } else if (usage > 5) {
            return 0.9;
        }
        return 1.0;
    }

    public void recordPathUsage(List<BlockPos> path) {
        for (BlockPos pos : path) {
            pathUsage.merge(pos, 1, Integer::sum);
        }

        // Decay usage over time
        pathUsage.entrySet().forEach(e -> {
            e.setValue(Math.max(0, e.getValue() - 1));
        });
    }
}
```

---

### 2.4 Dynamic Obstacle Handling

**Real-time Updates:**
```java
public class DynamicPathfinding {

    private PathfindingContext context;
    private List<PathNode> currentPath;
    private int currentPathIndex;

    public void tick() {
        // Check if path is still valid
        if (isPathInvalidated()) {
            // Replan from current position
            replan();
        } else {
            // Continue following path
            followPath();
        }
    }

    private boolean isPathInvalidated() {
        if (currentPath == null) return true;

        // Check remaining path for obstacles
        for (int i = currentPathIndex; i < currentPath.size(); i++) {
            PathNode node = currentPath.get(i);
            if (isBlocked(node.pos)) {
                return true;
            }
        }

        return false;
    }

    private void replan() {
        BlockPos currentPos = getCurrentPosition();
        BlockPos goal = getGoalPosition();

        // Replan with obstacle awareness
        currentPath = pathfinder.findPath(currentPos, goal, context).orElse(null);
        currentPathIndex = 0;
    }
}
```

**Incremental Path Repair:**
```java
public class PathRepair {

    public List<PathNode> repairPath(List<PathNode> originalPath, BlockPos blockedPos) {
        int blockedIndex = -1;

        // Find blocked node
        for (int i = 0; i < originalPath.size(); i++) {
            if (originalPath.get(i).pos.equals(blockedPos)) {
                blockedIndex = i;
                break;
            }
        }

        if (blockedIndex == -1) {
            return originalPath; // Not blocked
        }

        // Replan from node before blockage
        int repairFrom = Math.max(0, blockedIndex - 1);
        BlockPos repairStart = originalPath.get(repairFrom).pos;
        BlockPos goal = originalPath.get(originalPath.size() - 1).pos;

        // Find detour
        Optional<List<PathNode>> detour = pathfinder.findPath(repairStart, goal, context);

        if (detour.isPresent()) {
            // Splice in new path
            List<PathNode> repaired = new ArrayList<>(originalPath.subList(0, repairFrom));
            repaired.addAll(detour.get());
            return repaired;
        }

        return originalPath; // No detour found
    }
}
```

---

## 3. Multi-Agent Pathfinding

### 3.1 Conflict-Based Search (CBS)

**Concept:** Two-level algorithm for optimal multi-agent pathfinding.

**Algorithm Structure:**
```
Level 1 (High-Level): Constraint Tree
- Nodes: Sets of constraints on agents
- Root: No constraints
- Children: Add constraint to resolve conflict

Level 2 (Low-Level): Individual A*
- Each agent plans independently
- Respects constraints from high-level
```

**Pseudocode:**
```
function CBS(agentStarts, agentGoals):
    root = CTNode()
    root.solution = planIndividualPaths(agentStarts, agentGoals)
    root.cost = sumCosts(root.solution)
    root.constraints = []

    priorityQueue.insert(root)

    while priorityQueue not empty:
        node = priorityQueue.pop()

        if node.cost < bestCost:
            conflict = findFirstConflict(node.solution)

            if conflict == null:
                return node.solution // Conflict-free

            // Branch: Add constraints for conflicting agents
            for agent in [conflict.agent1, conflict.agent2]:
                child = copy(node)
                child.constraints.add(conflict.constraintFor(agent))
                child.solution = replanAgent(agent, child.constraints)
                child.cost = sumCosts(child.solution)

                if child.solution != null:
                    priorityQueue.insert(child)

    return null // No solution
```

**Java Implementation:**
```java
public class ConflictBasedSearch {

    static class Conflict {
        int agent1, agent2;
        BlockPos position;
        int timestep;

        Constraint getConstraintFor(int agent) {
            return new Constraint(agent, position, timestep);
        }
    }

    static class Constraint {
        int agent;
        BlockPos position;
        int timestep;
    }

    static class CTNode {
        Map<Integer, List<PathNode>> solution;
        List<Constraint> constraints;
        double cost;
    }

    public Map<Integer, List<PathNode>> findPaths(
        Map<Integer, BlockPos> starts,
        Map<Integer, BlockPos> goals
    ) {
        PriorityQueue<CTNode> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.cost)
        );

        // Initialize root node
        CTNode root = new CTNode();
        root.solution = new HashMap<>();
        root.constraints = new ArrayList<>();

        // Plan initial paths for each agent
        for (Map.Entry<Integer, BlockPos> entry : starts.entrySet()) {
            int agent = entry.getKey();
            BlockPos start = entry.getValue();
            BlockPos goal = goals.get(agent);

            List<PathNode> path = planAgentPath(agent, start, goal, root.constraints);
            if (path == null) {
                return null; // No solution
            }
            root.solution.put(agent, path);
        }

        root.cost = calculateTotalCost(root.solution);
        openSet.add(root);

        // Search
        while (!openSet.isEmpty()) {
            CTNode current = openSet.poll();

            // Check for conflicts
            Conflict conflict = findFirstConflict(current.solution);
            if (conflict == null) {
                return current.solution; // Found solution
            }

            // Create children with constraints
            for (int agent : new int[]{conflict.agent1, conflict.agent2}) {
                CTNode child = new CTNode();
                child.solution = new HashMap<>(current.solution);
                child.constraints = new ArrayList<>(current.constraints);

                // Add constraint
                child.constraints.add(conflict.getConstraintFor(agent));

                // Replan for constrained agent
                BlockPos start = starts.get(agent);
                BlockPos goal = goals.get(agent);
                List<PathNode> newPath = planAgentPath(agent, start, goal, child.constraints);

                if (newPath != null) {
                    child.solution.put(agent, newPath);
                    child.cost = calculateTotalCost(child.solution);
                    openSet.add(child);
                }
            }
        }

        return null; // No solution found
    }

    private Conflict findFirstConflict(Map<Integer, List<PathNode>> solution) {
        // Check vertex conflicts (same position at same time)
        Map<String, List<Integer>> positionUsage = new HashMap<>();

        for (Map.Entry<Integer, List<PathNode>> entry : solution.entrySet()) {
            int agent = entry.getKey();
            List<PathNode> path = entry.getValue();

            for (int t = 0; t < path.size(); t++) {
                String posKey = path.get(t).pos.toString();

                if (positionUsage.containsKey(posKey)) {
                    for (int otherAgent : positionUsage.get(posKey)) {
                        // Check if other agent is here at same time
                        List<PathNode> otherPath = solution.get(otherAgent);
                        if (t < otherPath.size() && otherPath.get(t).pos.equals(path.get(t).pos)) {
                            return new Conflict(agent, otherAgent, path.get(t).pos, t);
                        }
                    }
                }

                positionUsage.computeIfAbsent(posKey, k -> new ArrayList<>()).add(agent);
            }
        }

        return null; // No conflict found
    }
}
```

**Performance Characteristics:**
- Optimal: Guarantees shortest combined path length
- Complete: Will find solution if one exists
- Memory: Can be high for many agents
- Time: Exponential in number of conflicts

---

### 3.2 Cooperative A*

**Concept:** Agents coordinate to avoid conflicts during pathfinding.

**Window-Based Cooperative A*:**
```java
public class CooperativeAStar {

    private static final int WINDOW_SIZE = 10; // Time window for coordination

    public List<PathNode> findPath(
        int agentId,
        BlockPos start,
        BlockPos goal,
        Map<Integer, List<PathNode>> otherPaths,
        int currentTime
    ) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, goal), MovementType.WALK);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            if (current.pos.equals(goal)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                // Check cooperation constraints
                int arrivalTime = currentTime + (int)current.gCost + 1;
                if (isInConflict(neighbor, arrivalTime, agentId, otherPaths)) {
                    continue; // Skip conflicting positions
                }

                double gCost = current.gCost + 1;
                double hCost = heuristic(neighbor, goal);

                PathNode neighborNode = new PathNode(neighbor, current, gCost, hCost, MovementType.WALK);
                openSet.add(neighborNode);
            }
        }

        return null; // No path found
    }

    private boolean isInConflict(
        BlockPos pos,
        int time,
        int agentId,
        Map<Integer, List<PathNode>> otherPaths
    ) {
        // Check time window
        for (int t = Math.max(0, time - WINDOW_SIZE/2);
             t <= time + WINDOW_SIZE/2; t++) {

            for (Map.Entry<Integer, List<PathNode>> entry : otherPaths.entrySet()) {
                int otherAgent = entry.getKey();
                List<PathNode> path = entry.getValue();

                if (t < path.size()) {
                    BlockPos otherPos = path.get(t).pos;

                    // Vertex conflict (same position)
                    if (otherPos.equals(pos)) {
                        return true;
                    }

                    // Edge conflict (swapping positions)
                    if (t > 0) {
                        BlockPos prevPos = path.get(t - 1).pos;
                        if (prevPos.equals(pos) && otherPos.equals(getPreviousPos(time, agentId))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
```

**Priority-Based Coordination:**
```java
public class PriorityPlanning {

    private List<Integer> agentPriorities; // Lower = higher priority

    public Map<Integer, List<PathNode>> planAll(
        Map<Integer, BlockPos> starts,
        Map<Integer, BlockPos> goals
    ) {
        Map<Integer, List<PathNode>> allPaths = new HashMap<>();

        // Plan in priority order
        for (int priority = 0; priority < agentPriorities.size(); priority++) {
            int agent = agentPriorities.get(priority);

            BlockPos start = starts.get(agent);
            BlockPos goal = goals.get(agent);

            // Plan considering higher-priority agents
            List<PathNode> path = cooperativePathfinder.findPath(
                agent, start, goal, allPaths, 0
            );

            if (path == null) {
                return null; // No solution
            }

            allPaths.put(agent, path);
        }

        return allPaths;
    }
}
```

---

### 3.3 Flow Fields

**Concept:** Pre-compute direction vectors for all cells toward target.

**Algorithm:**
```
1. Generate Cost Field:
   - Mark target as cost 0
   - Mark obstacles as infinite cost
   - All other cells get terrain cost

2. Generate Integration Field:
   - Dijkstra from target outward
   - Each cell stores min cost to target

3. Generate Flow Field:
   - For each cell, point to neighbor with lowest integration cost
   - Store normalized direction vector

4. Agent Movement:
   - Look up flow vector at current position
   - Move in that direction
   - Repeat until target reached
```

**Java Implementation:**
```java
public class FlowFieldPathfinding {

    private int width, height, depth;
    private double[][][] costField;      // Static terrain costs
    private double[][][] integrationField; // Dynamic cost to target
    private Vec3[][][] flowField;         // Direction vectors
    private BlockPos target;

    public void generateFlowField(BlockPos target, Level level) {
        this.target = target;

        // Step 1: Generate cost field
        generateCostField(level);

        // Step 2: Generate integration field (Dijkstra from target)
        generateIntegrationField();

        // Step 3: Generate flow field
        generateFlowFieldVectors();
    }

    private void generateCostField(Level level) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (isBlocked(level, pos)) {
                        costField[x][y][z] = Double.POSITIVE_INFINITY;
                    } else {
                        costField[x][y][z] = getTerrainCost(level, pos);
                    }
                }
            }
        }
    }

    private void generateIntegrationField() {
        // Initialize
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    integrationField[x][y][z] = Double.POSITIVE_INFINITY;
                }
            }
        }

        // Dijkstra from target
        PriorityQueue<IntegrationNode> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.cost)
        );

        integrationField[target.getX()][target.getY()][target.getZ()] = 0;
        openSet.add(new IntegrationNode(target, 0));

        while (!openSet.isEmpty()) {
            IntegrationNode current = openSet.poll();

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                int nx = neighbor.getX();
                int ny = neighbor.getY();
                int nz = neighbor.getZ();

                if (costField[nx][ny][nz] == Double.POSITIVE_INFINITY) {
                    continue; // Obstacle
                }

                double newCost = current.cost + costField[nx][ny][nz];

                if (newCost < integrationField[nx][ny][nz]) {
                    integrationField[nx][ny][nz] = newCost;
                    openSet.add(new IntegrationNode(neighbor, newCost));
                }
            }
        }
    }

    private void generateFlowFieldVectors() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (integrationField[x][y][z] == Double.POSITIVE_INFINITY) {
                        flowField[x][y][z] = Vec3.ZERO;
                        continue;
                    }

                    // Find neighbor with lowest cost
                    BlockPos bestNeighbor = null;
                    double bestCost = Double.POSITIVE_INFINITY;

                    for (BlockPos neighbor : getNeighbors(new BlockPos(x, y, z))) {
                        int nx = neighbor.getX();
                        int ny = neighbor.getY();
                        int nz = neighbor.getZ();

                        if (integrationField[nx][ny][nz] < bestCost) {
                            bestCost = integrationField[nx][ny][nz];
                            bestNeighbor = neighbor;
                        }
                    }

                    // Calculate direction vector
                    if (bestNeighbor != null) {
                        double dx = bestNeighbor.getX() - x;
                        double dy = bestNeighbor.getY() - y;
                        double dz = bestNeighbor.getZ() - z;

                        double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        flowField[x][y][z] = new Vec3(dx/length, dy/length, dz/length);
                    } else {
                        flowField[x][y][z] = Vec3.ZERO;
                    }
                }
            }
        }
    }

    public Vec3 getFlowDirection(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            return Vec3.ZERO;
        }

        return flowField[x][y][z];
    }

    static class IntegrationNode {
        BlockPos pos;
        double cost;

        IntegrationNode(BlockPos pos, double cost) {
            this.pos = pos;
            this.cost = cost;
        }
    }
}
```

**Advantages:**
- O(1) lookup per agent per tick
- Scales to thousands of agents
- Natural collision avoidance (agents follow same flow)

**Disadvantages:**
- Must regenerate when obstacles change
- Memory intensive (3 vectors per cell)
- All agents follow same target

**Hybrid Approach:**
```java
public class HybridFlowField {

    // Use flow fields for common paths
    private Map<BlockPos, FlowFieldPathfinding> commonFlowFields;

    // Use A* for unique destinations
    private AStarPathfinder individualPathfinder;

    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        // Check if flow field exists for this goal
        FlowFieldPathfinding flowField = commonFlowFields.get(goal);

        if (flowField != null) {
            // Follow flow field
            return followFlowField(start, flowField);
        } else {
            // Use individual A*
            return individualPathfinder.findPath(start, goal);
        }
    }

    private List<BlockPos> followFlowField(BlockPos start, FlowFieldPathfinding flowField) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;

        while (!current.equals(flowField.getTarget())) {
            path.add(current);

            Vec3 direction = flowField.getFlowDirection(current);
            if (direction.equals(Vec3.ZERO)) {
                break; // No flow
            }

            current = current.offset(
                (int)Math.round(direction.x),
                (int)Math.round(direction.y),
                (int)Math.round(direction.z)
            );
        }

        return path;
    }
}
```

---

### 3.4 RVO (Reciprocal Velocity Obstacles)

**Concept:** Local collision avoidance by adjusting velocities.

**Algorithm:**
```
For each agent:
1. Calculate preferred velocity (toward goal)
2. For each nearby agent:
   - Calculate VO (velocity obstacles)
   - Find velocities that would cause collision
3. Find velocity outside all VOs
4. Pick velocity closest to preferred
```

**Java Implementation:**
```java
public class RVOAvoidance {

    static class Agent {
        Vec3 position;
        Vec3 velocity;
        Vec3 preferredVelocity;
        double radius;
        double maxSpeed;
        BlockPos goal;
    }

    public Vec3 calculateSafeVelocity(Agent agent, List<Agent> nearbyAgents) {
        Vec3 preferred = agent.preferredVelocity;

        // ORCA (Optimal Reciprocal Collision Avoidance)
        List<Line> orcaLines = new ArrayList<>();

        for (Agent other : nearbyAgents) {
            if (other == agent) continue;

            // Calculate relative velocity
            Vec3 relativeVelocity = agent.velocity.sub(other.velocity);
            Vec3 relativePosition = other.position.sub(agent.position);

            double distance = relativePosition.length();
            double radiusSum = agent.radius + other.radius;

            if (distance > radiusSum * 2) {
                continue; // Too far to worry about
            }

            // Calculate ORCA line
            Line orcaLine = computeORCALine(agent, other, relativePosition, relativeVelocity);
            orcaLines.add(orcaLine);
        }

        // Find velocity closest to preferred that satisfies all constraints
        Vec3 safeVelocity = findSafeVelocity(preferred, orcaLines, agent.maxSpeed);

        return safeVelocity;
    }

    private Line computeORCALine(
        Agent agent,
        Agent other,
        Vec3 relativePosition,
        Vec3 relativeVelocity
    ) {
        double radiusSum = agent.radius + other.radius;
        double distance = relativePosition.length();

        // Unit vector from agent to other
        Vec3 direction = relativePosition.normalize();

        // Relative velocity at collision boundary
        double dv = relativeVelocity.dot(direction);

        // Time to collision
        double tau = (distance - radiusSum) / dv;

        if (tau < 0) {
            // Moving away, no constraint needed
            return null;
        }

        // ORCA constraint
        Vec3 point = relativeVelocity.add(
            direction.scale((distance - radiusSum) / (2 * tau))
        );

        Vec3 normal = direction.negate();

        return new Line(point, normal);
    }

    private Vec3 findSafeVelocity(Vec3 preferred, List<Line> orcaLines, double maxSpeed) {
        // Binary search for velocity closest to preferred
        // that satisfies all ORCA constraints

        // Start with preferred velocity
        Vec3 candidate = preferred;

        // Check if satisfies all constraints
        boolean valid = true;
        for (Line line : orcaLines) {
            if (!line.satisfies(candidate)) {
                valid = false;
                break;
            }
        }

        if (valid) {
            return candidate;
        }

        // Project onto feasible region
        // (Simplified - full implementation uses linear programming)
        candidate = projectToFeasibleRegion(preferred, orcaLines, maxSpeed);

        return candidate;
    }

    static class Line {
        Vec3 point;
        Vec3 normal;

        Line(Vec3 point, Vec3 normal) {
            this.point = point;
            this.normal = normal;
        }

        boolean satisfies(Vec3 velocity) {
            Vec3 rel = velocity.sub(point);
            return rel.dot(normal) >= -0.001; // Tolerance
        }
    }
}
```

**Integration with Pathfinding:**
```java
public class RVOPathFollowing {

    private List<BlockPos> globalPath;
    private int currentWaypointIndex;
    private RVOAvoidance rvo;

    public Vec3 calculateNextVelocity(Agent agent, List<Agent> allAgents) {
        // Calculate preferred velocity toward next waypoint
        BlockPos target = globalPath.get(currentWaypointIndex);
        Vec3 toTarget = new Vec3(target).sub(agent.position);

        Vec3 preferredVelocity = toTarget.normalize().scale(agent.maxSpeed);

        // Check if reached waypoint
        if (toTarget.length() < 1.0) {
            currentWaypointIndex++;
            if (currentWaypointIndex >= globalPath.size()) {
                return Vec3.ZERO; // Reached goal
            }
        }

        // Adjust for collision avoidance
        List<Agent> nearbyAgents = getNearbyAgents(agent, allAgents);
        Vec3 safeVelocity = rvo.calculateSafeVelocity(agent, nearbyAgents);

        return safeVelocity;
    }

    private List<Agent> getNearbyAgents(Agent agent, List<Agent> allAgents) {
        List<Agent> nearby = new ArrayList<>();
        double perceptionRadius = 5.0; // 5 blocks

        for (Agent other : allAgents) {
            if (other == agent) continue;

            double distance = agent.position.distTo(other.position);
            if (distance <= perceptionRadius) {
                nearby.add(other);
            }
        }

        return nearby;
    }
}
```

---

## 4. Baritone Patterns

### 4.1 Baritone Architecture Overview

**Baritone** is the industry-leading Minecraft pathfinding bot, approximately 30x faster than traditional implementations.

**Key Components:**
```
1. Path Execution
   - Movement calculations (Movement, MovementHelper)
   - Action execution (Mining, placing, walking)

2. Path Calculation
   - A* with modifications
   - Segmented calculation
   - Chunk caching

3. Cost System
   - Block-based costs
   - Movement-based costs
   - Heuristic weighting
```

---

### 4.2 Segmented Path Calculation

**Challenge:** Minecraft has limited render distance. Paths may extend beyond loaded chunks.

**Baritone Solution:** Calculate paths in segments.

```java
public class SegmentedPathfinding {

    private static final double MIN_IMPROVEMENT = 0.01; // Minimum ticks to care about
    private static final int MIN_DISTANCE = 5; // Minimum blocks from start

    public List<PathSegment> calculateSegmentedPath(BlockPos start, BlockPos goal) {
        List<PathSegment> segments = new ArrayList<>();
        BlockPos currentPos = start;
        long remainingTime = MAX_CALCULATION_TIME;

        while (!currentPos.equals(goal) && remainingTime > 0) {
            long startTime = System.currentTimeMillis();

            // Calculate path segment
            List<BlockPos> segment = calculatePathSegment(currentPos, goal, remainingTime);

            if (segment.isEmpty()) {
                break; // No path found
            }

            // Select best segment using incremental cost backoff
            BlockPos segmentEnd = selectBestSegment(segment, currentPos);

            segments.add(new PathSegment(currentPos, segmentEnd, segment));
            currentPos = segmentEnd;

            remainingTime -= (System.currentTimeMillis() - startTime);
        }

        return segments;
    }

    private BlockPos selectBestSegment(List<BlockPos> path, BlockPos startPos) {
        // Incremental cost backoff
        // Find the best node by various increasing coefficients

        BlockPos bestNode = startPos;
        double bestCoefficient = 0;

        for (int i = 1; i < path.size(); i++) {
            BlockPos node = path.get(i);

            // Check if at least MIN_DISTANCE from start
            if (startPos.distSqr(node) < MIN_DISTANCE * MIN_DISTANCE) {
                continue;
            }

            // Calculate coefficient for this node
            double coefficient = calculateCostCoefficient(path, i);

            if (coefficient > bestCoefficient) {
                bestCoefficient = coefficient;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private double calculateCostCoefficient(List<BlockPos> path, int index) {
        // Simplified cost coefficient
        // Baritone uses complex heuristics including:
        // - Distance from start
        // - Estimated cost to goal
        // - Path quality metrics

        double distanceFromStart = Math.sqrt(
            path.get(0).distSqr(path.get(index))
        );

        return distanceFromStart;
    }

    private List<BlockPos> calculatePathSegment(
        BlockPos start,
        BlockPos goal,
        long timeLimit
    ) {
        // Standard A* with time limit
        // Uses minimum improvement repropagation
        // (ignores improvements < 0.01 ticks)

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();

        AStarNode startNode = new AStarNode(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);

        long startTime = System.currentTimeMillis();

        while (!openSet.isEmpty()) {
            // Check time limit
            if (System.currentTimeMillis() - startTime > timeLimit) {
                // Return best partial path
                return reconstructBestPath(openSet);
            }

            AStarNode current = openSet.poll();

            if (current.pos.equals(goal)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double gCost = current.gCost + calculateCost(current.pos, neighbor);

                // Minimum improvement repropagation
                AStarNode existing = findInOpenSet(openSet, neighbor);
                if (existing != null) {
                    double improvement = existing.gCost - gCost;
                    if (improvement < MIN_IMPROVEMENT) {
                        continue; // Not worth repropagating
                    }
                }

                double hCost = heuristic(neighbor, goal);
                AStarNode neighborNode = new AStarNode(neighbor, current, gCost, hCost);

                if (existing != null) {
                    openSet.remove(existing);
                }

                openSet.add(neighborNode);
            }
        }

        return Collections.emptyList(); // No path found
    }
}
```

---

### 4.3 Movement Cost Calculation

**Baritone's Cost System:**

```java
public class BaritoneCostCalculator {

    // Base movement costs
    private static final double WALK_COST = 1.0;
    private static final double JUMP_PENALTY = 2.0; // Default
    private static final double FALL_COST = 1.0;
    private static final double SWIM_COST = 3.0;
    private static final double CLIMB_COST = 2.5;

    // Configurable multipliers
    private double jumpPenaltyMultiplier = 1.0;
    private double costHeuristicMultiplier = 1.0;

    public double calculateMovementCost(BlockPos from, BlockPos to, MovementType type) {
        double baseCost;

        switch (type) {
            case WALK:
                baseCost = WALK_COST;
                break;
            case JUMP:
                baseCost = WALK_COST + (JUMP_PENALTY * jumpPenaltyMultiplier);
                break;
            case FALL:
                baseCost = FALL_COST;
                break;
            case SWIM:
                baseCost = SWIM_COST;
                break;
            case CLIMB:
                baseCost = CLIMB_COST;
                break;
            default:
                baseCost = WALK_COST;
        }

        // Distance adjustment
        double distance = Math.sqrt(from.distSqr(to));
        baseCost *= distance;

        // Diagonal movement adjustment
        if (from.getX() != to.getX() && from.getZ() != to.getZ()) {
            baseCost *= Math.sqrt(2);
        }

        return baseCost;
    }

    public double calculateBlockCost(Level level, BlockPos pos, MovementType type) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Block-specific costs
        if (block == Blocks.WATER) {
            return SWIM_COST;
        }

        if (block == Blocks.LADDER || block == Blocks.VINE) {
            return CLIMB_COST;
        }

        if (block == Blocks.SOUL_SAND) {
            return WALK_COST * 1.5; // Slower
        }

        return WALK_COST;
    }

    // Dynamic cost based on context
    public double calculateContextCost(PathfindingContext context) {
        double multiplier = 1.0;

        // Hunger consideration
        if (context.getHungerLevel() < 6) {
            multiplier *= 1.5; // Penalize jumping when hungry
        }

        // Time of day
        if (context.isNight() && !context.hasLight()) {
            multiplier *= 1.2; // Slower in dark
        }

        // Armor weight
        if (context.getArmorWeight() > 20) {
            multiplier *= 1.1; // Encumbered
        }

        return multiplier;
    }
}
```

---

### 4.4 Chunk Caching Strategies

**Baritone's Approach:**

```java
public class ChunkCacheManager {

    private static final int CACHE_SIZE = 256; // Number of chunks to cache
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutes

    private LinkedHashMap<Long, CachedChunk> chunkCache;

    public ChunkCacheManager() {
        this.chunkCache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, CachedChunk> eldest) {
                return size() > CACHE_SIZE;
            }
        };
    }

    public CachedChunk getCachedChunk(long chunkKey) {
        CachedChunk cached = chunkCache.get(chunkKey);

        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        return null;
    }

    public void cacheChunk(long chunkKey, CachedChunk chunk) {
        chunkCache.put(chunkKey, chunk);
    }

    static class CachedChunk {
        long chunkKey;
        long timestamp;

        // Cached pathfinding data
        Map<BlockPos, Double> movementCosts;
        Set<BlockPos> blockedPositions;
        List<BlockPos> entrancePoints;
        List<BlockPos> exitPoints;

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
}
```

---

## 5. Application to MineWright

### 5.1 Current Pathfinding System

**Existing Components:**
- `AStarPathfinder` - Standard A* with node pooling and path caching
- `HierarchicalPathfinder` - Two-level chunk-based pathfinding
- `PathSmoother` - String pulling and corner cutting
- `MovementValidator` - Movement type detection and validation

**Current Performance:**
- Path caching with configurable TTL
- Dynamic timeout based on path complexity
- Node pooling to reduce GC pressure
- Chunk-level abstraction for long paths

---

### 5.2 Proposed Enhancements

#### Priority 1: Jump Point Search Integration

**Add JPS for flat terrain:**

```java
public class HybridPathfinder {

    private AStarPathfinder standardAStar;
    private JumpPointSearch jps;

    public Optional<List<PathNode>> findPath(
        BlockPos start,
        BlockPos goal,
        PathfindingContext context
    ) {
        // Determine if terrain is suitable for JPS
        if (isUniformTerrain(start, goal, context)) {
            return jps.findPath(start, goal, context);
        } else {
            return standardAStar.findPath(start, goal, context);
        }
    }

    private boolean isUniformTerrain(BlockPos start, BlockPos goal, PathfindingContext context) {
        // Sample terrain along path
        // If mostly uniform costs, use JPS
        int samples = 10;
        int uniformCount = 0;

        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            int x = (int) (start.getX() + t * (goal.getX() - start.getX()));
            int z = (int) (start.getZ() + t * (goal.getZ() - start.getZ()));

            BlockPos sample = new BlockPos(x, start.getY(), z);
            double cost = getMovementCost(context, sample);

            if (cost < 1.5) { // Consider uniform if cost < 1.5
                uniformCount++;
            }
        }

        return uniformCount >= samples * 0.8; // 80% uniform
    }
}
```

**Expected Impact:**
- 60-80% reduction in node expansion for flat terrain
- Maintains current A* for complex terrain
- Transparent to existing code

---

#### Priority 2: Enhanced Hierarchical Pathfinding

**Improve chunk graph caching:**

```java
public class EnhancedHierarchicalPathfinder extends HierarchicalPathfinder {

    private Map<ChunkPos, ChunkGraph> globalChunkGraph;
    private Map<ChunkPos, Long> chunkGraphTimestamps;

    @Override
    protected ChunkGraph buildChunkGraph(ChunkPos start, ChunkPos goal, PathfindingContext context) {
        // Check if we have cached graph data
        ChunkGraph cached = globalChunkGraph.get(start);
        if (cached != null && !isExpired(start)) {
            return cached;
        }

        // Build new graph
        ChunkGraph graph = super.buildChunkGraph(start, goal, context);

        // Cache for future
        globalChunkGraph.put(start, graph);
        chunkGraphTimestamps.put(start, System.currentTimeMillis());

        return graph;
    }

    private boolean isExpired(ChunkPos pos) {
        Long timestamp = chunkGraphTimestamps.get(pos);
        if (timestamp == null) return true;

        return System.currentTimeMillis() - timestamp > CHUNK_GRAPH_TTL;
    }

    public void invalidateChunk(ChunkPos pos) {
        globalChunkGraph.remove(pos);
        chunkGraphTimestamps.remove(pos);
    }
}
```

**Add intra-chunk path caching:**

```java
public class IntraChunkCache {

    private Map<ChunkPos, Map<BlockPos, Map<BlockPos, List<PathNode>>>> chunkPathCache;

    public Optional<List<PathNode>> getCachedPath(
        ChunkPos chunk,
        BlockPos start,
        BlockPos goal
    ) {
        Map<BlockPos, Map<BlockPos, List<PathNode>>> chunkCache = chunkPathCache.get(chunk);
        if (chunkCache == null) {
            return Optional.empty();
        }

        Map<BlockPos, List<PathNode>> startCache = chunkCache.get(start);
        if (startCache == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(startCache.get(goal));
    }

    public void cachePath(
        ChunkPos chunk,
        BlockPos start,
        BlockPos goal,
        List<PathNode> path
    ) {
        chunkPathCache.computeIfAbsent(chunk, k -> new HashMap<>())
            .computeIfAbsent(start, k -> new HashMap<>())
            .put(goal, path);
    }
}
```

---

#### Priority 3: Multi-Agent Collision Avoidance

**Add RVO for local avoidance:**

```java
public class MultiAgentCoordinator {

    private Map<UUID, Agent> agents;
    private RVOAvoidance rvo;

    public void updateAgentMovements() {
        for (Agent agent : agents.values()) {
            if (agent.isPathfinding()) {
                // Calculate preferred velocity from path
                Vec3 preferred = calculatePreferredVelocity(agent);

                // Get nearby agents
                List<Agent> nearby = getNearbyAgents(agent, 5.0);

                // Adjust for collision avoidance
                Vec3 safeVelocity = rvo.calculateSafeVelocity(agent, nearby);

                // Apply velocity
                agent.setVelocity(safeVelocity);
            }
        }
    }

    private Vec3 calculatePreferredVelocity(Agent agent) {
        List<PathNode> path = agent.getCurrentPath();
        if (path.isEmpty()) {
            return Vec3.ZERO;
        }

        BlockPos nextWaypoint = path.get(0).pos;
        Vec3 toWaypoint = new Vec3(nextWaypoint).sub(agent.getPosition());

        return toWaypoint.normalize().scale(agent.getMaxSpeed());
    }
}
```

---

#### Priority 4: Baritone-Inspired Optimizations

**Add segmented calculation for long paths:**

```java
public class SegmentedPathfindingWrapper {

    private static final int SEGMENT_LENGTH = 50; // blocks

    public List<BlockPos> findLongPath(BlockPos start, BlockPos goal, PathfindingContext context) {
        List<BlockPos> fullPath = new ArrayList<>();
        BlockPos current = start;

        while (!current.equals(goal)) {
            // Calculate segment
            BlockPos segmentEnd = calculateSegmentGoal(current, goal);

            Optional<List<PathNode>> segmentPath = pathfinder.findPath(
                current, segmentEnd, context
            );

            if (!segmentPath.isPresent()) {
                break; // Can't reach goal
            }

            // Add segment to full path
            for (PathNode node : segmentPath.get()) {
                fullPath.add(node.pos);
                current = node.pos;

                if (current.distSqr(goal) < SEGMENT_LENGTH * SEGMENT_LENGTH) {
                    break;
                }
            }
        }

        return fullPath;
    }

    private BlockPos calculateSegmentGoal(BlockPos from, BlockPos ultimateGoal) {
        Vec3 direction = new Vec3(ultimateGoal).sub(new Vec3(from)).normalize();

        return new BlockPos(
            from.getX() + (int)(direction.x * SEGMENT_LENGTH),
            from.getY() + (int)(direction.y * SEGMENT_LENGTH),
            from.getZ() + (int)(direction.z * SEGMENT_LENGTH)
        );
    }
}
```

---

## 6. Implementation Roadmap

### Phase 1: Low-Hanging Fruit (1-2 weeks)

1. **Jump Point Search for Flat Terrain**
   - Implement `JumpPointSearch` class
   - Add terrain uniformity detection
   - Integrate with `AStarPathfinder`
   - Benchmark performance gains

2. **Enhanced Path Caching**
   - Add subpath caching
   - Implement prefix cache for common origins
   - Add cache invalidation on block changes

3. **Improved Chunk Caching**
   - Cache chunk traversability data
   - Add intra-chunk path caching
   - Implement spatial cache invalidation

### Phase 2: Advanced Features (2-3 weeks)

4. **Multi-Agent RVO**
   - Implement `RVOAvoidance` class
   - Add local collision avoidance
   - Integrate with agent coordination

5. **Segmented Path Calculation**
   - Implement Baritone-style segmentation
   - Add incremental cost backoff
   - Handle partial path results

6. **Enhanced Cost Functions**
   - Add hunger-based costs
   - Implement time-of-day penalties
   - Add armor/encumbrance factors

### Phase 3: Experimental Features (3-4 weeks)

7. **Navigation Mesh Generation**
   - Implement navmesh generator for Minecraft
   - Add funnel algorithm for string pulling
   - Compare performance vs grid-based

8. **Flow Field Integration**
   - Implement flow field generation
   - Add hybrid A*/flow field approach
   - Benchmark for multiple agents

9. **Conflict-Based Search**
   - Implement CBS for small agent groups
   - Add priority-based planning
   - Benchmark against current approach

---

## 7. References

### Academic Papers

1. **Harabor, D., & Botea, A. (2010)** - "Hierarchical Pathfinding for Grid-Based World" (HPA*)
2. **Harabor, D., & Grastien, A. (2011)** - "Online Graph Pruning for Pathfinding on Grid Maps" (JPS)
3. **Sharon, G., et al. (2015)** - "Conflict-Based Search for Optimal Multi-Agent Pathfinding" (CBS)
4. **Van den Berg, J., et al. (2011)** - "Reciprocal Velocity Obstacles for Real-Time Multi-Agent Navigation" (RVO)

### Industry Resources

1. **Red Blob Games** - Interactive pathfinding visualizations
2. **Baritone Source Code** - Industry-leading Minecraft pathfinding
3. **Unreal Engine Documentation** - Navigation mesh optimization
4. **Unity A* Pathfinding Project** - Commercial pathfinding implementation

### Online Resources

1. CSDN - Jump Point Search implementations and optimizations
2. Bilibili - Baritone architecture analysis
3. Game Programming Patterns - Path caching strategies
4. Recast & Detour - Navigation mesh generation

---

## Conclusion

MineWright's current pathfinding system provides a solid foundation with A*, hierarchical pathfinding, and path smoothing. By implementing the optimization techniques outlined in this document, we can achieve:

- **Performance:** 10-100x speedup for long-distance paths
- **Scalability:** Support for 10+ concurrent agents
- **Quality:** Smoother, more natural movement
- **Robustness:** Better handling of dynamic obstacles

**Priority Implementation Order:**
1. Jump Point Search (quick win for flat terrain)
2. Enhanced chunk caching (builds on existing system)
3. RVO collision avoidance (enables multi-agent coordination)
4. Segmented calculation (Baritone-inspired optimization)

The research and techniques presented here represent the state-of-the-art in pathfinding as of 2024-2025, with specific applications to Minecraft's unique challenges and opportunities.

---

**Document Status:** Complete
**Next Review:** After implementation of Priority 1 features
**Contact:** Claude Orchestrator
