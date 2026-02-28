package com.minewright.pathfinding;

import com.minewright.MineWrightMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Optimizes paths by removing unnecessary waypoints and smoothing movement.
 *
 * <h3>Purpose:</h3>
 * <p>A* pathfinding produces paths that are optimal but often inefficient to follow.
 * PathSmoother post-processes paths to:</p>
 * <ul>
 *   <li><b>Remove Redundancy:</b> Eliminate waypoints that don't change direction</li>
 *   <li><b>Straighten:</b> Convert zig-zags to straight diagonals where possible</li>
 *   <li><b>Simplify:</b> Reduce the number of points the agent must track</li>
 *   <li><b>Add Curve:</b> Insert intermediate points for smooth turning</li>
 * </ul>
 *
 * <h3>Smoothing Techniques:</h3>
 * <ol>
 *   <li><b>Line of Sight Check:</b> Skip intermediate nodes if direct path is clear</li>
 *   <li><b>String Pulling:</b> Iteratively remove nodes that don't affect path validity</li>
 *   <li><b>Corner Cutting:</b> Replace 90° turns with diagonal when safe</li>
 *   <li><b>Subdivision:</b> Add points for smooth curvature at turns</li>
 * </ol>
 *
 * <h3>Example:</h3>
 * <pre>
 * Original:  A -> B -> C -> D -> E
 * After:     A -> C -> E  (B and D removed)
 * </pre>
 *
 * @see PathNode
 * @see PathfindingContext
 */
public class PathSmoother {
    private static final Logger LOGGER = LoggerFactory.getLogger(PathSmoother.class);

    /** Maximum distance for line-of-sight skipping. */
    private static final int MAX_SKIP_DISTANCE = 16;

    /** Minimum angle change to warrant keeping a waypoint (in radians). */
    private static final double MIN_ANGLE_CHANGE = 0.2; // ~11 degrees

    /** Movement validator for checking if smoothed paths are valid. */
    private static final MovementValidator validator = new MovementValidator();

    /**
     * Private constructor - utility class.
     */
    private PathSmoother() {
    }

    /**
     * Smooths a path by removing unnecessary waypoints.
     *
     * <p>This is the main entry point for path smoothing. It applies multiple
     * smoothing techniques in sequence to produce the most efficient path.</p>
     *
     * @param path    Original path from pathfinder
     * @param context Pathfinding context
     * @return Smoothed path (may be shorter than input)
     */
    public static List<PathNode> smooth(List<PathNode> path, PathfindingContext context) {
        if (path == null || path.size() <= 2) {
            return path; // Nothing to smooth
        }

        try {
            // Step 1: String pulling (remove redundant nodes)
            List<PathNode> pulled = stringPulling(path, context);

            // Step 2: Corner cutting (diagonal shortcuts)
            List<PathNode> cut = cutCorners(pulled, context);

            // Step 3: Subdivide for smooth turning
            List<PathNode> subdivided = subdivideForTurning(cut, context);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[PathSmoother] Reduced path from {} to {} nodes",
                    path.size(), subdivided.size());
            }

            return subdivided;

        } catch (Exception e) {
            LOGGER.warn("[PathSmoother] Error during smoothing, returning original path", e);
            return path;
        }
    }

    /**
     * String pulling: removes nodes that don't contribute to path shape.
     *
     * <p>Iteratively checks if a path can skip intermediate nodes by maintaining
     * line of sight. If node A can directly reach node C, then node B is removed.</p>
     *
     * @param path    Original path
     * @param context Pathfinding context
     * @return Simplified path
     */
    private static List<PathNode> stringPulling(List<PathNode> path, PathfindingContext context) {
        if (path.size() <= 2) {
            return new ArrayList<>(path);
        }

        List<PathNode> result = new ArrayList<>();
        result.add(path.get(0)); // Always keep start

        int currentIndex = 0;

        while (currentIndex < path.size() - 1) {
            int furthestVisible = currentIndex;

            // Find the furthest node we can reach directly
            for (int i = currentIndex + 1; i < path.size(); i++) {
                if (hasLineOfSight(path.get(currentIndex), path.get(i), context)) {
                    furthestVisible = i;
                } else {
                    break; // Can't see past this point
                }
            }

            // Add the furthest visible node
            result.add(path.get(furthestVisible));
            currentIndex = furthestVisible;
        }

        return result;
    }

    /**
     * Cuts corners by replacing 90° turns with diagonals where safe.
     *
     * <p>Looks for patterns like A -> B -> C where B is a corner that can be
     * skipped if the diagonal path is valid.</p>
     *
     * @param path    Path after string pulling
     * @param context Pathfinding context
     * @return Path with cut corners
     */
    private static List<PathNode> cutCorners(List<PathNode> path, PathfindingContext context) {
        if (path.size() <= 2) {
            return new ArrayList<>(path);
        }

        List<PathNode> result = new ArrayList<>();
        result.add(path.get(0)); // Keep start

        for (int i = 1; i < path.size() - 1; i++) {
            PathNode prev = result.get(result.size() - 1);
            PathNode current = path.get(i);
            PathNode next = path.get(i + 1);

            // Check if this is a corner that can be cut
            if (canCutCorner(prev, current, next, context)) {
                // Skip the corner node
                continue;
            }

            result.add(current);
        }

        result.add(path.get(path.size() - 1)); // Keep end

        return result;
    }

    /**
     * Subdivides the path to add intermediate points for smooth turning.
     *
     * <p>At sharp turns, adds intermediate points to create a more natural
     * turning arc. This is especially important for entities that can't
     * make instant 90° turns.</p>
     *
     * @param path    Path after corner cutting
     * @param context Pathfinding context
     * @return Path with turn subdivision
     */
    private static List<PathNode> subdivideForTurning(List<PathNode> path, PathfindingContext context) {
        if (path.size() <= 2) {
            return new ArrayList<>(path);
        }

        List<PathNode> result = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            PathNode current = path.get(i);
            result.add(current);

            // Check if there's a next node
            if (i < path.size() - 1) {
                PathNode next = path.get(i + 1);

                // Calculate angle change
                double angle = calculateAngleChange(
                    i > 0 ? path.get(i - 1) : current,
                    current,
                    next
                );

                // If angle is sharp, add intermediate point
                if (Math.abs(angle) > MIN_ANGLE_CHANGE) {
                    PathNode intermediate = createIntermediateNode(current, next, context);
                    if (intermediate != null) {
                        result.add(intermediate);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks if there's a clear line of sight between two nodes.
     *
     * @param from    Starting node
     * @param to      Target node
     * @param context Pathfinding context
     * @return true if direct path is clear
     */
    private static boolean hasLineOfSight(PathNode from, PathNode to, PathfindingContext context) {
        BlockPos startPos = from.pos;
        BlockPos endPos = to.pos;

        // Check distance
        double distance = Math.sqrt(startPos.distSqr(endPos));
        if (distance > MAX_SKIP_DISTANCE) {
            return false; // Too far to skip
        }

        Level level = context.getLevel();

        // Raycast from start to end
        // Simple implementation: check blocks along the line
        int dx = endPos.getX() - startPos.getX();
        int dy = endPos.getY() - startPos.getY();
        int dz = endPos.getZ() - startPos.getZ();

        int steps = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));

        if (steps == 0) {
            return true;
        }

        double stepX = (double) dx / steps;
        double stepY = (double) dy / steps;
        double stepZ = (double) dz / steps;

        // Check each point along the line
        for (int i = 1; i < steps; i++) {
            BlockPos checkPos = new BlockPos(
                startPos.getX() + (int) (stepX * i),
                startPos.getY() + (int) (stepY * i),
                startPos.getZ() + (int) (stepZ * i)
            );

            // Check if this position is blocked
            if (validator.isBlocked(level, checkPos)) {
                return false; // Path blocked
            }

            // Check headroom
            if (!validator.hasHeadroom(level, checkPos)) {
                return false; // Not enough space
            }
        }

        return true; // Path is clear
    }

    /**
     * Checks if a corner can be cut (replaced with diagonal).
     *
     * @param prev    Node before corner
     * @param corner  Corner node
     * @param next    Node after corner
     * @param context Pathfinding context
     * @return true if corner can be cut
     */
    private static boolean canCutCorner(PathNode prev, PathNode corner, PathNode next,
                                        PathfindingContext context) {
        // Check if movement forms a 90° turn in horizontal plane
        int dx1 = corner.pos.getX() - prev.pos.getX();
        int dz1 = corner.pos.getZ() - prev.pos.getZ();
        int dx2 = next.pos.getX() - corner.pos.getX();
        int dz2 = next.pos.getZ() - corner.pos.getZ();

        // Check if perpendicular (dot product = 0)
        boolean isTurn = (dx1 * dx2 + dz1 * dz2) == 0;

        if (!isTurn) {
            return false;
        }

        // Check if diagonal is valid
        MovementType diagonalType = MovementType.WALK;
        return validator.canMove(prev.pos, next.pos, diagonalType, context);
    }

    /**
     * Calculates the angle change between three consecutive nodes.
     *
     * @param prev    Previous node
     * @param current Current node
     * @param next    Next node
     * @return Angle change in radians
     */
    private static double calculateAngleChange(PathNode prev, PathNode current, PathNode next) {
        // Calculate direction vectors
        double dx1 = current.pos.getX() - prev.pos.getX();
        double dz1 = current.pos.getZ() - prev.pos.getZ();
        double dx2 = next.pos.getX() - current.pos.getX();
        double dz2 = next.pos.getZ() - current.pos.getZ();

        // Normalize
        double len1 = Math.sqrt(dx1 * dx1 + dz1 * dz1);
        double len2 = Math.sqrt(dx2 * dx2 + dz2 * dz2);

        if (len1 == 0 || len2 == 0) {
            return 0;
        }

        dx1 /= len1;
        dz1 /= len1;
        dx2 /= len2;
        dz2 /= len2;

        // Calculate angle using dot product
        double dot = dx1 * dx2 + dz1 * dz2;
        return Math.acos(Math.max(-1, Math.min(1, dot)));
    }

    /**
     * Creates an intermediate node between two nodes for smooth turning.
     *
     * @param from    Starting node
     * @param to      Target node
     * @param context Pathfinding context
     * @return Intermediate node, or null if not possible
     */
    private static PathNode createIntermediateNode(PathNode from, PathNode to,
                                                   PathfindingContext context) {
        // Calculate midpoint
        int midX = (from.pos.getX() + to.pos.getX()) / 2;
        int midY = (from.pos.getY() + to.pos.getY()) / 2;
        int midZ = (from.pos.getZ() + to.pos.getZ()) / 2;

        BlockPos midPos = new BlockPos(midX, midY, midZ);

        // Check if midpoint is valid
        if (validator.hasSolidGround(context.getLevel(), midPos)) {
            return new PathNode(midPos, from, (from.gCost + to.gCost) / 2,
                (from.hCost + to.hCost) / 2, MovementType.WALK);
        }

        return null;
    }

    /**
     * Removes duplicate consecutive nodes from the path.
     *
     * @param path Path to clean
     * @return Path with duplicates removed
     */
    public static List<PathNode> removeDuplicates(List<PathNode> path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        List<PathNode> result = new ArrayList<>();
        BlockPos lastPos = null;

        for (PathNode node : path) {
            if (!node.pos.equals(lastPos)) {
                result.add(node);
                lastPos = node.pos;
            }
        }

        return result;
    }

    /**
     * Merges very close nodes into single waypoints.
     *
     * @param path       Path to merge
     * @param minDistance Minimum distance between nodes
     * @return Path with merged nodes
     */
    public static List<PathNode> mergeCloseNodes(List<PathNode> path, double minDistance) {
        if (path == null || path.size() <= 2) {
            return path;
        }

        List<PathNode> result = new ArrayList<>();
        result.add(path.get(0)); // Keep start

        PathNode lastKept = path.get(0);

        for (int i = 1; i < path.size(); i++) {
            PathNode current = path.get(i);

            double distance = Math.sqrt(lastKept.pos.distSqr(current.pos));

            if (distance >= minDistance || i == path.size() - 1) {
                result.add(current);
                lastKept = current;
            }
        }

        return result;
    }
}
