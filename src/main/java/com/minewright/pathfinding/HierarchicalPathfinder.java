package com.minewright.pathfinding;

import com.minewright.MineWrightMod;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hierarchical pathfinding implementation using two-level pathfinding.
 *
 * <h3>Concept:</h3>
 * <p>Hierarchical pathfinding reduces the search space for long-distance paths
 * by planning at two levels:</p>
 * <ol>
 *   <li><b>Chunk Level:</b> Find a route through chunks (16x16 areas)</li>
 *   <li><b>Local Level:</b> Find detailed paths within each chunk</li>
 * </ol>
 *
 * <p>This approach dramatically reduces computation time for long paths because
 * the chunk-level graph has much fewer nodes than the full block-level graph.</p>
 *
 * <h3>Performance Comparison:</h3>
 * <ul>
 *   <li><b>Standard A*:</b> O(n²) where n = number of blocks in path</li>
 *   <li><b>Hierarchical:</b> O((n/16)² + k²) where k = chunk size (typically 16)</li>
 *   <li><b>Speedup:</b> ~10-100x for paths > 64 blocks</li>
 * </ul>
 *
 * <h3>When to Use:</h3>
 * <ul>
 *   <li>Paths longer than 64 blocks</li>
 *   <li>Open terrain with few obstacles</li>
 *   <li>When pathfinding time is critical</li>
 *   <li>Not suitable for: tight corridors, complex indoor environments</li>
 * </ul>
 *
 * <h3>Algorithm:</h3>
 * <pre>
 * 1. Convert start/goal to chunk coordinates
 * 2. Build chunk graph (connect adjacent traversable chunks)
 * 3. Find chunk path using A*
 * 4. For each chunk transition, find local path
 * 5. Concatenate local paths into final route
 * </pre>
 *
 * @see AStarPathfinder
 * @see PathfindingContext
 */
public class HierarchicalPathfinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalPathfinder.class);

    /** Size of a chunk in blocks (standard Minecraft chunk size). */
    private static final int CHUNK_SIZE = 16;

    /** Maximum chunk distance before refusing to pathfind. */
    private static final int MAX_CHUNK_DISTANCE = 50; // 800 blocks

    /** Low-level pathfinder for local paths. */
    private final AStarPathfinder localPathfinder;

    /** Chunk graph cache for repeated pathfinding. */
    private final Map<String, ChunkNode> chunkCache = new HashMap<>();

    /** Whether chunk graph caching is enabled. */
    private boolean cachingEnabled = true;

    /**
     * Creates a new hierarchical pathfinder.
     */
    public HierarchicalPathfinder() {
        this.localPathfinder = new AStarPathfinder();
    }

    /**
     * Creates a new hierarchical pathfinder with a custom local pathfinder.
     *
     * @param localPathfinder The pathfinder for local routes
     */
    public HierarchicalPathfinder(AStarPathfinder localPathfinder) {
        this.localPathfinder = localPathfinder;
    }

    /**
     * Finds a path using hierarchical pathfinding.
     *
     * <p>For long distances, uses chunk-level planning. For short distances,
     * delegates to standard A* for simplicity.</p>
     *
     * @param start   Starting position
     * @param goal    Goal position
     * @param context Pathfinding context
     * @return Optional list of path nodes, empty if not found
     */
    public Optional<List<PathNode>> findPath(BlockPos start, BlockPos goal, PathfindingContext context) {
        // Check if hierarchical pathfinding should be used
        if (!shouldUseHierarchical(start, goal, context)) {
            // Use standard A* for short paths
            return localPathfinder.findPath(start, goal, context);
        }

        LOGGER.debug("[Hierarchical] Using hierarchical pathfinding from {} to {}", start, goal);

        try {
            // Step 1: Find chunk-level path
            List<BlockPos> chunkWaypoints = findChunkPath(start, goal, context);
            if (chunkWaypoints.isEmpty()) {
                LOGGER.debug("[Hierarchical] No chunk path found");
                return Optional.empty();
            }

            // Step 2: Connect waypoints with local paths
            List<PathNode> fullPath = new ArrayList<>();
            BlockPos currentPos = start;

            for (int i = 0; i < chunkWaypoints.size(); i++) {
                BlockPos waypoint = chunkWaypoints.get(i);

                // Skip if waypoint is same as current position
                if (waypoint.equals(currentPos)) {
                    continue;
                }

                // Find local path to waypoint
                Optional<List<PathNode>> localPath = findLocalPath(currentPos, waypoint, context);

                if (localPath.isPresent()) {
                    // Add local path to full path (skip first node to avoid duplication)
                    List<PathNode> localNodes = localPath.get();
                    if (!fullPath.isEmpty() && localNodes.size() > 1) {
                        fullPath.addAll(localNodes.subList(1, localNodes.size()));
                    } else {
                        fullPath.addAll(localNodes);
                    }
                    currentPos = waypoint;
                } else {
                    LOGGER.warn("[Hierarchical] Failed to find local path to waypoint {}", waypoint);
                    // Try to recover by finding path to next waypoint
                    if (i < chunkWaypoints.size() - 1) {
                        currentPos = chunkWaypoints.get(i + 1);
                    }
                }
            }

            // Step 3: Add final path to exact goal
            Optional<List<PathNode>> finalPath = findLocalPath(currentPos, goal, context);
            if (finalPath.isPresent()) {
                List<PathNode> finalNodes = finalPath.get();
                if (!fullPath.isEmpty() && finalNodes.size() > 1) {
                    fullPath.addAll(finalNodes.subList(1, finalNodes.size()));
                } else {
                    fullPath.addAll(finalNodes);
                }
            }

            if (fullPath.isEmpty()) {
                return Optional.empty();
            }

            // Step 4: Apply path smoothing
            if (context.shouldSmoothPath()) {
                fullPath = PathSmoother.smooth(fullPath, context);
            }

            LOGGER.debug("[Hierarchical] Path found with {} nodes", fullPath.size());
            return Optional.of(fullPath);

        } catch (Exception e) {
            LOGGER.error("[Hierarchical] Exception during hierarchical pathfinding", e);
            return Optional.empty();
        }
    }

    /**
     * Determines if hierarchical pathfinding should be used.
     *
     * @param start   Starting position
     * @param goal    Goal position
     * @param context Pathfinding context
     * @return true if hierarchical pathfinding is appropriate
     */
    private boolean shouldUseHierarchical(BlockPos start, BlockPos goal, PathfindingContext context) {
        if (!context.useHierarchical()) {
            return false;
        }

        // Calculate distance
        double distance = Math.sqrt(start.distSqr(goal));

        // Use hierarchical for distances above threshold
        return distance >= context.getHierarchicalThreshold();
    }

    /**
     * Finds a path at the chunk level.
     *
     * @param start   Starting position
     * @param goal    Goal position
     * @param context Pathfinding context
     * @return List of chunk waypoint positions
     */
    private List<BlockPos> findChunkPath(BlockPos start, BlockPos goal, PathfindingContext context) {
        // Convert to chunk coordinates
        ChunkPos startChunk = new ChunkPos(start);
        ChunkPos goalChunk = new ChunkPos(goal);

        // Check if in same chunk
        if (startChunk.equals(goalChunk)) {
            return Collections.singletonList(goal);
        }

        // Build chunk graph
        ChunkGraph graph = buildChunkGraph(startChunk, goalChunk, context);

        // Find path through chunks
        List<ChunkPos> chunkPath = findChunkRoute(startChunk, goalChunk, graph, context);

        if (chunkPath.isEmpty()) {
            return Collections.emptyList();
        }

        // Convert chunk path to waypoints
        List<BlockPos> waypoints = new ArrayList<>();
        for (ChunkPos chunk : chunkPath) {
            BlockPos waypoint = chunk.getCenter();
            waypoints.add(waypoint);
        }

        // Ensure goal is included
        if (!waypoints.get(waypoints.size() - 1).equals(goal)) {
            waypoints.add(goal);
        }

        return waypoints;
    }

    /**
     * Builds a graph of traversable chunks.
     *
     * @param start  Start chunk
     * @param goal   Goal chunk
     * @param context Pathfinding context
     * @return Chunk graph
     */
    private ChunkGraph buildChunkGraph(ChunkPos start, ChunkPos goal, PathfindingContext context) {
        ChunkGraph graph = new ChunkGraph();

        // Use BFS to discover reachable chunks
        Queue<ChunkPos> queue = new LinkedList<>();
        Set<ChunkPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            ChunkPos current = queue.poll();

            // Create chunk node
            ChunkNode node = getOrCreateChunkNode(current, context);
            graph.addNode(node);

            // Stop if we've reached the goal and explored enough around it
            if (current.equals(goal) && visited.size() > 25) {
                break;
            }

            // Don't explore too far from the direct path
            if (current.distanceTo(start) > MAX_CHUNK_DISTANCE) {
                continue;
            }

            // Check adjacent chunks
            for (ChunkPos neighbor : getAdjacentChunks(current)) {
                if (!visited.contains(neighbor)) {
                    // Check if neighbor is traversable
                    if (isChunkTraversable(neighbor, context)) {
                        visited.add(neighbor);
                        queue.add(neighbor);

                        ChunkNode neighborNode = getOrCreateChunkNode(neighbor, context);
                        graph.addNode(neighborNode);

                        // Add edge between chunks
                        double cost = current.distanceTo(neighbor);
                        graph.addEdge(current, neighbor, cost);
                    }
                }
            }
        }

        return graph;
    }

    /**
     * Gets or creates a chunk node for the given position.
     *
     * @param chunkPos Chunk position
     * @param context  Pathfinding context
     * @return Chunk node
     */
    private ChunkNode getOrCreateChunkNode(ChunkPos chunkPos, PathfindingContext context) {
        String key = chunkPos.toString();

        if (cachingEnabled && chunkCache.containsKey(key)) {
            return chunkCache.get(key);
        }

        ChunkNode node = new ChunkNode(chunkPos);

        // Estimate chunk traversability
        BlockPos center = chunkPos.getCenter();
        node.traversable = isPositionTraversable(center, context);
        node.cost = node.traversable ? 1.0 : 100.0;

        if (cachingEnabled) {
            chunkCache.put(key, node);
        }

        return node;
    }

    /**
     * Checks if a chunk is traversable.
     *
     * @param chunkPos Chunk position
     * @param context  Pathfinding context
     * @return true if chunk can be traversed
     */
    private boolean isChunkTraversable(ChunkPos chunkPos, PathfindingContext context) {
        BlockPos center = chunkPos.getCenter();
        return isPositionTraversable(center, context);
    }

    /**
     * Checks if a specific position is traversable.
     *
     * @param pos     Position to check
     * @param context Pathfinding context
     * @return true if position can be traversed
     */
    private boolean isPositionTraversable(BlockPos pos, PathfindingContext context) {
        // Simple check: is there solid ground to stand on?
        BlockPos below = pos.below();
        return context.getLevel().getBlockState(below).isSolid();
    }

    /**
     * Finds a route through the chunk graph using A*.
     *
     * @param start   Start chunk
     * @param goal    Goal chunk
     * @param graph   Chunk graph
     * @param context Pathfinding context
     * @return List of chunk positions
     */
    private List<ChunkPos> findChunkRoute(ChunkPos start, ChunkPos goal, ChunkGraph graph,
                                           PathfindingContext context) {
        // A* search through chunks
        Map<ChunkPos, ChunkNode> nodes = graph.getNodes();
        if (!nodes.containsKey(start) || !nodes.containsKey(goal)) {
            return Collections.emptyList();
        }

        PriorityQueue<ChunkNode> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.gCost + n.hCost));
        Set<ChunkPos> closedSet = new HashSet<>();
        Map<ChunkPos, ChunkPos> cameFrom = new HashMap<>();
        Map<ChunkPos, Double> gScore = new HashMap<>();

        // Initialize
        ChunkNode startNode = nodes.get(start);
        startNode.gCost = 0;
        startNode.hCost = start.distanceTo(goal);
        openSet.add(startNode);
        gScore.put(start, 0.0);

        while (!openSet.isEmpty()) {
            ChunkNode current = openSet.poll();

            if (current.pos.equals(goal)) {
                // Reconstruct path
                return reconstructChunkPath(cameFrom, current.pos);
            }

            closedSet.add(current.pos);

            // Explore neighbors
            for (Map.Entry<ChunkPos, Double> entry : current.neighbors.entrySet()) {
                ChunkPos neighborPos = entry.getKey();
                double edgeCost = entry.getValue();

                if (closedSet.contains(neighborPos)) {
                    continue;
                }

                if (!nodes.containsKey(neighborPos)) {
                    continue;
                }

                ChunkNode neighbor = nodes.get(neighborPos);
                if (!neighbor.traversable) {
                    continue;
                }

                double tentativeGScore = gScore.get(current.pos) + edgeCost;

                if (tentativeGScore < gScore.getOrDefault(neighborPos, Double.MAX_VALUE)) {
                    cameFrom.put(neighborPos, current.pos);
                    gScore.put(neighborPos, tentativeGScore);
                    neighbor.gCost = tentativeGScore;
                    neighbor.hCost = neighborPos.distanceTo(goal);

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Reconstructs the path through chunks.
     *
     * @param cameFrom Map of chunk to its predecessor
     * @param current  Current chunk position
     * @return List of chunk positions from start to goal
     */
    private List<ChunkPos> reconstructChunkPath(Map<ChunkPos, ChunkPos> cameFrom, ChunkPos current) {
        LinkedList<ChunkPos> path = new LinkedList<>();
        path.addFirst(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.addFirst(current);
        }

        return path;
    }

    /**
     * Gets adjacent chunks (including diagonals).
     *
     * @param chunk Current chunk
     * @return List of adjacent chunk positions
     */
    private List<ChunkPos> getAdjacentChunks(ChunkPos chunk) {
        List<ChunkPos> adjacent = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                adjacent.add(new ChunkPos(chunk.x + dx, chunk.z + dz));
            }
        }

        return adjacent;
    }

    /**
     * Finds a local path between two waypoints.
     *
     * @param start   Start position
     * @param goal    Goal position
     * @param context Pathfinding context
     * @return Optional local path
     */
    private Optional<List<PathNode>> findLocalPath(BlockPos start, BlockPos goal,
                                                    PathfindingContext context) {
        // Create a limited context for local pathfinding
        PathfindingContext localContext = new PathfindingContext(context.getLevel(), context.getEntity())
            .withGoal(goal)
            .withMaxRange(32) // Limit local path range
            .setUseHierarchical(false); // Don't recurse

        return localPathfinder.findPath(start, goal, localContext);
    }

    /**
     * Clears the chunk cache.
     */
    public void clearCache() {
        chunkCache.clear();
    }

    /**
     * Enables or disables chunk caching.
     *
     * @param enabled true to enable caching
     */
    public void setCachingEnabled(boolean enabled) {
        this.cachingEnabled = enabled;
        if (!enabled) {
            clearCache();
        }
    }

    // ========== Inner Classes ==========

    /**
     * Represents a chunk position.
     */
    private static class ChunkPos {
        final int x;
        final int z;

        ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }

        ChunkPos(BlockPos blockPos) {
            this.x = blockPos.getX() / CHUNK_SIZE;
            this.z = blockPos.getZ() / CHUNK_SIZE;
        }

        BlockPos getCenter() {
            return new BlockPos(x * CHUNK_SIZE + CHUNK_SIZE / 2, 64, z * CHUNK_SIZE + CHUNK_SIZE / 2);
        }

        double distanceTo(ChunkPos other) {
            return Math.sqrt((x - other.x) * (x - other.x) + (z - other.z) * (z - other.z));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ChunkPos)) return false;
            ChunkPos other = (ChunkPos) obj;
            return x == other.x && z == other.z;
        }

        @Override
        public int hashCode() {
            return 31 * x + z;
        }

        @Override
        public String toString() {
            return "Chunk[" + x + "," + z + "]";
        }
    }

    /**
     * Node in the chunk graph.
     */
    private static class ChunkNode implements Comparable<ChunkNode> {
        final ChunkPos pos;
        boolean traversable;
        double cost;
        double gCost;
        double hCost;
        Map<ChunkPos, Double> neighbors = new HashMap<>();

        ChunkNode(ChunkPos pos) {
            this.pos = pos;
        }

        void addNeighbor(ChunkPos neighbor, double cost) {
            neighbors.put(neighbor, cost);
        }

        @Override
        public int compareTo(ChunkNode other) {
            return Double.compare((gCost + hCost), (other.gCost + other.hCost));
        }
    }

    /**
     * Graph of chunks for pathfinding.
     */
    private static class ChunkGraph {
        private final Map<ChunkPos, ChunkNode> nodes = new HashMap<>();

        void addNode(ChunkNode node) {
            nodes.put(node.pos, node);
        }

        void addEdge(ChunkPos from, ChunkPos to, double cost) {
            ChunkNode fromNode = nodes.get(from);
            if (fromNode != null) {
                fromNode.addNeighbor(to, cost);
            }

            // Add reverse edge for undirected graph
            ChunkNode toNode = nodes.get(to);
            if (toNode != null) {
                toNode.addNeighbor(from, cost);
            }
        }

        Map<ChunkPos, ChunkNode> getNodes() {
            return nodes;
        }
    }
}
