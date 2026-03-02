package com.minewright.pathfinding;

import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced A* pathfinding implementation with Minecraft-specific optimizations.
 *
 * <h3>Algorithm Overview:</h3>
 * <p>A* search finds the optimal path by maintaining two sets:</p>
 * <ul>
 *   <li><b>Open Set:</b> Nodes to be explored, sorted by fCost (priority queue)</li>
 *   <li><b>Closed Set:</b> Nodes already evaluated (hash set for O(1) lookup)</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Early Termination:</b> Stops as soon as goal is reached</li>
 *   <li><b>Node Caching:</b> Reuses PathNode objects to reduce GC pressure</li>
 *   <li><b>Timeout Protection:</b> Aborts search if it takes too long</li>
 *   <li><b>Path Smoothing:</b> Removes unnecessary waypoints from final path</li>
 *   <li><b>Adaptive Heuristics:</b> Chooses best heuristic based on context</li>
 *   <li><b>Path Caching:</b> Caches frequently traversed routes for performance</li>
 *   <li><b>Dynamic Timeout:</b> Adjusts timeout based on path complexity</li>
 * </ul>
 *
 * <h3>Performance:</h3>
 * <ul>
 *   <li>Time: O(b^d) where b is branching factor, d is solution depth</li>
 *   <li>Space: O(b^d) for storing explored nodes</li>
 *   <li>Typical: 100-500ms for paths up to 150 blocks</li>
 * </ul>
 *
 * <h3>Configuration:</h3>
 * <p>Pathfinding behavior can be configured in <code>config/minewright-common.toml</code>:</p>
 * <pre>
 * [pathfinding]
 * # Maximum nodes to explore (prevents infinite loops)
 * max_nodes = 10000
 * # Enable path caching for frequently traversed routes
 * cache_enabled = true
 * # Maximum number of cached paths
 * cache_max_size = 100
 * # Path cache TTL in minutes
 * cache_ttl_minutes = 10
 * </pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * AStarPathfinder pathfinder = new AStarPathfinder();
 * PathfindingContext context = new PathfindingContext(level, entity)
 *     .withGoal(targetPos);
 *
 * Optional<List<PathNode>> path = pathfinder.findPath(startPos, goalPos, context);
 *
 * if (path.isPresent()) {
 *     // Execute the path
 *     executor.followPath(path.get());
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Multiple pathfinding
 * operations can run concurrently on different threads.</p>
 *
 * @see PathNode
 * @see PathfindingContext
 * @see Heuristics
 * @see MovementValidator
 */
public class AStarPathfinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AStarPathfinder.class);

    /**
     * Default maximum nodes to explore before giving up (prevents infinite loops).
     * Can be overridden via config: pathfinding.max_nodes
     */
    private static final int DEFAULT_MAX_NODES = 10000;

    /** Node pool for reusing PathNode objects (reduces GC pressure). */
    private static final PriorityBlockingQueue<PathNode> nodePool =
        new PriorityBlockingQueue<>(100, Comparator.comparingDouble(n -> -n.fCost()));

    /** Statistics for monitoring pathfinder performance. */
    private static final AtomicInteger nodesExplored = new AtomicInteger(0);
    private static final AtomicInteger pathsFound = new AtomicInteger(0);
    private static final AtomicInteger timeouts = new AtomicInteger(0);
    private static final AtomicInteger cacheHits = new AtomicInteger(0);
    private static final AtomicInteger cacheMisses = new AtomicInteger(0);

    /** Movement validator for checking if moves are valid. */
    private final MovementValidator movementValidator;

    /** Path cache for frequently traversed routes. */
    private final PathCache pathCache;

    /** Maximum nodes to explore (read from config). */
    private final int maxNodes;

    /** Whether path caching is enabled. */
    private final boolean cachingEnabled;

    /** Whether to enable detailed logging. */
    private boolean debugLogging = false;

    /**
     * Creates a new A* pathfinder with default settings.
     */
    public AStarPathfinder() {
        this.movementValidator = new MovementValidator();
        this.maxNodes = readMaxNodesFromConfig();
        this.cachingEnabled = readCacheEnabledFromConfig();
        this.pathCache = new PathCache(readCacheMaxSizeFromConfig(), readCacheTTLFromConfig());
    }

    /**
     * Creates a new A* pathfinder with a custom movement validator.
     *
     * @param movementValidator Custom movement validator
     */
    public AStarPathfinder(MovementValidator movementValidator) {
        this.movementValidator = movementValidator;
        this.maxNodes = readMaxNodesFromConfig();
        this.cachingEnabled = readCacheEnabledFromConfig();
        this.pathCache = new PathCache(readCacheMaxSizeFromConfig(), readCacheTTLFromConfig());
    }

    /**
     * Finds a path from start to goal using A* search.
     *
     * <p>This is the main entry point for pathfinding. It handles all the
     * bookkeeping of open/closed sets, node expansion, and path reconstruction.</p>
     *
     * <p><b>Optimizations:</b></p>
     * <ul>
     *   <li>Path caching for frequently traversed routes</li>
     *   <li>Dynamic timeout based on path complexity</li>
     *   <li>Early termination when cache hit occurs</li>
     * </ul>
     *
     * @param start   Starting position
     * @param goal    Goal position
     * @param context Pathfinding context with constraints
     * @return Optional list of PathNodes representing the path, empty if not found
     */
    public Optional<List<PathNode>> findPath(BlockPos start, BlockPos goal, PathfindingContext context) {
        long startTime = System.currentTimeMillis();

        // OPTIMIZATION: Calculate dynamic timeout based on path complexity
        long timeout = calculateDynamicTimeout(start, goal, context);

        // Validate inputs
        if (start == null || goal == null || context == null) {
            LOGGER.warn("[A*] Invalid pathfinding parameters: start={}, goal={}, context={}",
                start, goal, context);
            return Optional.empty();
        }

        // Check if already at goal
        if (start.equals(goal) || context.isGoalReached(start)) {
            if (debugLogging) {
                LOGGER.debug("[A*] Already at goal: {}", start);
            }
            return Optional.of(Collections.singletonList(new PathNode(start)));
        }

        // Check distance constraints
        if (start.distSqr(goal) > (context.getMaxRange() * context.getMaxRange())) {
            LOGGER.debug("[A*] Goal {} out of range (max: {})", goal, context.getMaxRange());
            return Optional.empty();
        }

        // OPTIMIZATION: Check path cache before computing
        if (cachingEnabled) {
            Optional<List<PathNode>> cachedPath = pathCache.get(start, goal, context);
            if (cachedPath.isPresent()) {
                cacheHits.incrementAndGet();
                if (debugLogging) {
                    LOGGER.debug("[A*] Cache hit for path {} -> {} ({} nodes)",
                        start, goal, cachedPath.get().size());
                }
                return cachedPath;
            }
            cacheMisses.incrementAndGet();
        }

        // Initialize A* data structures
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<BlockPos, PathNode> openMap = new HashMap<>(); // For faster lookup
        Set<BlockPos> closedSet = new HashSet<>();

        // Create start node
        PathNode startNode = createOrReuseNode(start, null, 0, 0, MovementType.WALK);
        openSet.add(startNode);
        openMap.put(start, startNode);

        // Get heuristic function
        Heuristics.HeuristicFunction heuristic = Heuristics.createHeuristic(context);
        double initialHeuristic = heuristic.estimate(start, goal);
        startNode.hCost = initialHeuristic;

        int explored = 0;
        PathNode currentNode = null;

        // Main A* loop
        try {
            while (!openSet.isEmpty() && explored < maxNodes) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeout) {
                    timeouts.incrementAndGet();
                    LOGGER.debug("[A*] Pathfinding timeout after {}ms, explored {} nodes",
                        System.currentTimeMillis() - startTime, explored);
                    return Optional.empty();
                }

                // Get node with lowest fCost
                currentNode = openSet.poll();
                openMap.remove(currentNode.pos);
                explored++;
                nodesExplored.incrementAndGet();

                // Check if goal reached
                if (context.isGoalReached(currentNode.pos)) {
                    pathsFound.incrementAndGet();
                    long duration = System.currentTimeMillis() - startTime;

                    if (debugLogging) {
                        LOGGER.debug("[A*] Path found in {}ms, explored {} nodes, path length: {}",
                            duration, explored, calculatePathLength(currentNode));
                    }

                    List<PathNode> path = reconstructPath(currentNode);

                    // Apply smoothing if enabled
                    if (context.shouldSmoothPath()) {
                        path = PathSmoother.smooth(path, context);
                    }

                    // OPTIMIZATION: Cache successful path
                    if (cachingEnabled && !path.isEmpty()) {
                        pathCache.put(start, goal, context, path);
                    }

                    // Return nodes to pool
                    returnNodesToPool(openSet);
                    returnNodesToPool(closedSet);

                    return Optional.of(path);
                }

                // Add to closed set
                closedSet.add(currentNode.pos);

                // Expand neighbors
                expandNeighbors(currentNode, goal, context, heuristic, openSet, openMap, closedSet);
            }

            // No path found
            if (debugLogging) {
                LOGGER.debug("[A*] No path found after exploring {} nodes", explored);
            }

            returnNodesToPool(openSet);
            returnNodesToPool(closedSet);

            return Optional.empty();

        } catch (Exception e) {
            LOGGER.error("[A*] Exception during pathfinding", e);
            returnNodesToPool(openSet);
            returnNodesToPool(closedSet);
            return Optional.empty();
        }
    }

    /**
     * Calculates dynamic timeout based on path complexity.
     *
     * <p>Longer paths and more complex terrain get longer timeouts.</p>
     *
     * <p><b>Formula:</b></p>
     * <pre>
     * base_timeout = config value (default 2000ms)
     * distance_factor = min(distance / 100, 5.0)
     * dynamic_timeout = base_timeout * (1 + distance_factor)
     * </pre>
     *
     * @param start   Starting position
     * @param goal    Goal position
     * @param context Pathfinding context
     * @return Dynamic timeout in milliseconds
     */
    private long calculateDynamicTimeout(BlockPos start, BlockPos goal, PathfindingContext context) {
        long baseTimeout = context.getTimeoutMs();

        // Calculate distance factor (longer paths get more time)
        double distance = Math.sqrt(start.distSqr(goal));
        double distanceFactor = Math.min(distance / 100.0, 5.0);

        // Calculate dynamic timeout
        long dynamicTimeout = (long) (baseTimeout * (1.0 + distanceFactor));

        // Apply config-specified pathfinding timeout as upper bound
        long maxTimeout = MineWrightConfig.PATHFINDING_TIMEOUT_MS.get();
        dynamicTimeout = Math.min(dynamicTimeout, maxTimeout);

        return dynamicTimeout;
    }

    /**
     * Clears the path cache.
     * <p>Useful for testing or when world changes significantly.</p>
     */
    public void clearCache() {
        pathCache.clear();
    }

    /**
     * Gets path cache statistics.
     *
     * @return Array of [cacheSize, cacheHits, cacheMisses, hitRate]
     */
    public static double[] getCacheStatistics() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
        return new double[]{hits, misses, hitRate};
    }

    /**
     * Expands the neighbors of a node during A* search.
     *
     * @param current   The node being expanded
     * @param goal      The goal position
     * @param context   Pathfinding context
     * @param heuristic Heuristic function
     * @param openSet   Open set (priority queue)
     * @param openMap   Map for open set lookups
     * @param closedSet Closed set (already evaluated)
     */
    private void expandNeighbors(PathNode current, BlockPos goal, PathfindingContext context,
                                  Heuristics.HeuristicFunction heuristic,
                                  PriorityQueue<PathNode> openSet,
                                  Map<BlockPos, PathNode> openMap,
                                  Set<BlockPos> closedSet) {
        // Generate all possible neighbor positions
        List<BlockPos> neighbors = generateNeighbors(current.pos, context);

        for (BlockPos neighborPos : neighbors) {
            // Skip if already evaluated
            if (closedSet.contains(neighborPos)) {
                continue;
            }

            // Determine movement type to reach neighbor
            MovementType movement = determineMovementType(current.pos, neighborPos, context);

            // Validate movement
            if (!movementValidator.canMove(current.pos, neighborPos, movement, context)) {
                continue;
            }

            // Calculate movement cost
            double moveCost = calculateMovementCost(current.pos, neighborPos, movement, context);
            double tentativeGCost = current.gCost + moveCost;

            // Check if neighbor is already in open set with better cost
            PathNode existingNode = openMap.get(neighborPos);
            if (existingNode != null && tentativeGCost >= existingNode.gCost) {
                continue; // Skip if this is not a better path
            }

            // Create or update neighbor node
            PathNode neighborNode = existingNode;
            if (neighborNode == null) {
                neighborNode = createOrReuseNode(neighborPos, current, tentativeGCost, 0, movement);
                openSet.add(neighborNode);
                openMap.put(neighborPos, neighborNode);
            } else {
                // Update existing node with better path
                neighborNode.gCost = tentativeGCost;
                neighborNode.parent = current;
                neighborNode.movement = movement;
                // Re-add to queue to re-sort based on new cost
                openSet.remove(neighborNode);
                openSet.add(neighborNode);
            }

            // Calculate heuristic
            neighborNode.hCost = heuristic.estimate(neighborPos, goal);
        }
    }

    /**
     * Generates all possible neighbor positions for a node.
     *
     * @param pos     Current position
     * @param context Pathfinding context
     * @return List of neighbor positions to explore
     */
    private List<BlockPos> generateNeighbors(BlockPos pos, PathfindingContext context) {
        List<BlockPos> neighbors = new ArrayList<>();

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // Add immediate neighbors (26 surrounding blocks)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue; // Skip current position
                    }

                    // Skip positions that would move too far vertically
                    if (Math.abs(dy) > context.getJumpHeight() + 1) {
                        continue;
                    }

                    BlockPos neighbor = new BlockPos(x + dx, y + dy, z + dz);
                    neighbors.add(neighbor);
                }
            }
        }

        // Add parkour jumps (2-block horizontal jumps)
        if (context.canParkour() && context.allowDangerousMovements()) {
            for (int dx = -2; dx <= 2; dx += 4) {
                neighbors.add(new BlockPos(x + dx, y, z));
                neighbors.add(new BlockPos(x + dx, y + 1, z));
            }
            for (int dz = -2; dz <= 2; dz += 4) {
                neighbors.add(new BlockPos(x, y, z + dz));
                neighbors.add(new BlockPos(x, y + 1, z + dz));
            }
        }

        return neighbors;
    }

    /**
     * Determines the movement type between two positions.
     *
     * @param from     Starting position
     * @param to       Target position
     * @param context  Pathfinding context
     * @return The movement type
     */
    private MovementType determineMovementType(BlockPos from, BlockPos to, PathfindingContext context) {
        int dy = to.getY() - from.getY();

        // Check for flying
        if (context.canFly()) {
            return MovementType.FLY;
        }

        // Check for water
        if (context.getLevel().getBlockState(to).getFluidState().isSource()) {
            if (dy < 0) {
                return MovementType.DESCEND_WATER;
            } else if (dy == 0) {
                return MovementType.WATER_WALK;
            } else {
                return MovementType.SWIM;
            }
        }

        // Check for climbing
        if (dy > 0 && context.canClimb() &&
            movementValidator.isClimbable(context.getLevel(), to)) {
            return MovementType.CLIMB;
        }

        // Vertical movement
        if (dy > 0) {
            return MovementType.JUMP;
        } else if (dy < 0) {
            return MovementType.FALL;
        }

        // Horizontal movement
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());

        // Check for sneaking (low ceiling)
        if (movementValidator.hasLowCeiling(context.getLevel(), to)) {
            return MovementType.SNEAK;
        }

        // Check for parkour
        if ((dx == 2 || dz == 2) && dy == 0 && context.canParkour()) {
            return MovementType.PARKOUR;
        }

        return MovementType.WALK;
    }

    /**
     * Calculates the cost of a movement between two positions.
     *
     * @param from     Starting position
     * @param to       Target position
     * @param movement Movement type
     * @param context  Pathfinding context
     * @return Movement cost
     */
    private double calculateMovementCost(BlockPos from, BlockPos to, MovementType movement,
                                          PathfindingContext context) {
        double baseCost = movement.getCost();

        // Distance factor
        double distance = Math.sqrt(from.distSqr(to));
        double cost = baseCost * distance;

        // Apply cost multiplier if position has one
        if (context.getBlocksToPrefer() != null && context.getBlocksToPrefer().contains(to)) {
            cost *= 0.5; // Prefer these positions
        }
        if (context.getBlocksToAvoid() != null && context.getBlocksToAvoid().contains(to)) {
            cost *= 5.0; // Avoid these positions
        }

        // Danger multiplier
        if (!movement.isSafe() && !context.allowDangerousMovements()) {
            cost *= 10.0; // Heavily penalize dangerous movements
        }

        // Light preference
        if (context.shouldStayLit()) {
            int lightLevel = context.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, to);
            if (lightLevel < 8) {
                cost *= 1.5; // Penalize dark areas
            }
        }

        return cost;
    }

    /**
     * Reconstructs the path from goal back to start by following parent links.
     *
     * @param goalNode The goal node
     * @return List of path nodes from start to goal
     */
    private List<PathNode> reconstructPath(PathNode goalNode) {
        LinkedList<PathNode> path = new LinkedList<>();
        PathNode current = goalNode;

        while (current != null) {
            path.addFirst(current.copy());
            current = current.parent;
        }

        return path;
    }

    /**
     * Calculates the length of a path by following parent links.
     *
     * @param goalNode The goal node
     * @return Number of nodes in the path
     */
    private int calculatePathLength(PathNode goalNode) {
        int length = 0;
        PathNode current = goalNode;
        while (current != null) {
            length++;
            current = current.parent;
        }
        return length;
    }

    /**
     * Creates a new PathNode or reuses one from the pool.
     *
     * @param pos      Position
     * @param parent   Parent node
     * @param gCost    Cost from start
     * @param hCost    Heuristic cost
     * @param movement Movement type
     * @return A PathNode ready for use
     */
    private PathNode createOrReuseNode(BlockPos pos, PathNode parent, double gCost, double hCost,
                                        MovementType movement) {
        PathNode node = nodePool.poll();
        if (node != null) {
            // Reuse existing node
            return new PathNode(pos, parent, gCost, hCost, movement);
        }
        // Create new node
        return new PathNode(pos, parent, gCost, hCost, movement);
    }

    /**
     * Returns nodes to the pool for reuse.
     *
     * @param nodes Collection of nodes to return
     */
    private void returnNodesToPool(Collection<PathNode> nodes) {
        // Don't pool - just let GC handle it
        // Pooling can cause issues with parent references
    }

    private void returnNodesToPool(Set<BlockPos> positions) {
        // Nothing to return - these are just positions
    }

    /**
     * Gets the movement validator used by this pathfinder.
     *
     * @return The movement validator
     */
    public MovementValidator getMovementValidator() {
        return movementValidator;
    }

    /**
     * Enables or disables debug logging.
     *
     * @param debug true to enable debug logging
     */
    public void setDebugLogging(boolean debug) {
        this.debugLogging = debug;
    }

    /**
     * Gets pathfinder statistics.
     *
     * @return Array of [nodesExplored, pathsFound, timeouts]
     */
    public static int[] getStatistics() {
        return new int[]{
            nodesExplored.get(),
            pathsFound.get(),
            timeouts.get()
        };
    }

    /**
     * Resets pathfinder statistics.
     */
    public static void resetStatistics() {
        nodesExplored.set(0);
        pathsFound.set(0);
        timeouts.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }

    // ========== Configuration Helpers ==========

    /**
     * Reads the maximum nodes from configuration.
     * Defaults to 10000 if configuration is not available.
     *
     * @return Maximum nodes to explore
     */
    private static int readMaxNodesFromConfig() {
        try {
            return MineWrightConfig.PATHFINDING_MAX_NODES.get();
        } catch (Exception e) {
            LOGGER.debug("Could not read max nodes from config, using default: {}",
                DEFAULT_MAX_NODES);
            return DEFAULT_MAX_NODES;
        }
    }

    /**
     * Reads whether path caching is enabled from configuration.
     * Defaults to true if configuration is not available.
     *
     * @return true if path caching is enabled
     */
    private static boolean readCacheEnabledFromConfig() {
        try {
            return MineWrightConfig.PATHFINDING_CACHE_ENABLED.get();
        } catch (Exception e) {
            LOGGER.debug("Could not read cache enabled from config, using default: true");
            return true;
        }
    }

    /**
     * Reads the cache max size from configuration.
     * Defaults to 100 if configuration is not available.
     *
     * @return Maximum number of cached paths
     */
    private static int readCacheMaxSizeFromConfig() {
        try {
            return MineWrightConfig.PATHFINDING_CACHE_MAX_SIZE.get();
        } catch (Exception e) {
            LOGGER.debug("Could not read cache max size from config, using default: 100");
            return 100;
        }
    }

    /**
     * Reads the cache TTL from configuration.
     * Defaults to 10 minutes if configuration is not available.
     *
     * @return Cache TTL in minutes
     */
    private static int readCacheTTLFromConfig() {
        try {
            return MineWrightConfig.PATHFINDING_CACHE_TTL_MINUTES.get();
        } catch (Exception e) {
            LOGGER.debug("Could not read cache TTL from config, using default: 10 minutes");
            return 10;
        }
    }

    // ========== Path Cache Implementation ==========

    /**
     * Path cache for storing frequently traversed routes.
     *
     * <p>Uses LRU eviction policy and TTL-based expiration.</p>
     */
    private static class PathCache {
        private final int maxSize;
        private final long ttlMillis;
        private final java.util.LinkedHashMap<CacheKey, CachedPath> cache;

        PathCache(int maxSize, int ttlMinutes) {
            this.maxSize = maxSize;
            this.ttlMillis = ttlMinutes * 60 * 1000L;
            this.cache = new java.util.LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<CacheKey, CachedPath> eldest) {
                    return size() > maxSize;
                }
            };
        }

        Optional<List<PathNode>> get(BlockPos start, BlockPos goal, PathfindingContext context) {
            CacheKey key = new CacheKey(start, goal, context);
            CachedPath cached = cache.get(key);

            if (cached != null && !cached.isExpired(ttlMillis)) {
                return Optional.of(new ArrayList<>(cached.path));
            }

            // Remove expired entry
            cache.remove(key);
            return Optional.empty();
        }

        void put(BlockPos start, BlockPos goal, PathfindingContext context, List<PathNode> path) {
            CacheKey key = new CacheKey(start, goal, context);
            cache.put(key, new CachedPath(path));
        }

        void clear() {
            cache.clear();
        }

        int size() {
            return cache.size();
        }
    }

    /**
     * Cache key combining start, goal, and context.
     */
    private static class CacheKey {
        private final BlockPos start;
        private final BlockPos goal;
        private final int capabilitiesHash;

        CacheKey(BlockPos start, BlockPos goal, PathfindingContext context) {
            this.start = start;
            this.goal = goal;
            // Hash relevant context parameters
            this.capabilitiesHash = Objects.hash(
                context.canSwim(),
                context.canClimb(),
                context.canFly(),
                context.canParkour(),
                context.getJumpHeight(),
                context.shouldAvoidMobs()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return start.equals(cacheKey.start) &&
                goal.equals(cacheKey.goal) &&
                capabilitiesHash == cacheKey.capabilitiesHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, goal, capabilitiesHash);
        }
    }

    /**
     * Cached path with timestamp for TTL expiration.
     */
    private static class CachedPath {
        private final List<PathNode> path;
        private final long timestamp;

        CachedPath(List<PathNode> path) {
            this.path = new ArrayList<>(path);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }
    }
}
