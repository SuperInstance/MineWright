package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;

/**
 * Represents a node in the A* pathfinding search space.
 *
 * <p>Each PathNode corresponds to a specific block position in the world
 * and contains cost information for A* algorithm traversal:</p>
 * <ul>
 *   <li><b>gCost:</b> Actual cost from the start position to this node</li>
 *   <li><b>hCost:</b> Heuristic estimated cost from this node to the goal</li>
 *   <li><b>fCost:</b> Total estimated cost (gCost + hCost) for path sorting</li>
 * </ul>
 *
 * <p><b>Movement Type:</b> Each node tracks how the agent arrived at this position
 * (walking, jumping, climbing, swimming, falling), which affects cost calculation
 * and subsequent movement validation.</p>
 *
 * <p><b>Parent Link:</b> Forms a linked chain from goal back to start, allowing
 * path reconstruction once the goal is reached.</p>
 *
 * <p><b>Thread Safety:</b> This class is mutable and not thread-safe.
 * Pathfinding operations should be confined to a single thread.</p>
 *
 * @see AStarPathfinder
 * @see MovementType
 * @see PathfindingContext
 */
public class PathNode implements Comparable<PathNode> {
    /** The block position this node represents. Immutable. */
    public final BlockPos pos;

    /** Actual cost from start to this node. */
    public double gCost;

    /** Heuristic estimated cost from this node to goal. */
    public double hCost;

    /** Parent node in the path (null for start node). */
    public PathNode parent;

    /** Movement type used to reach this node. */
    public MovementType movement;

    /** Additional cost multiplier for this position (e.g., danger, slow terrain). */
    public double costMultiplier = 1.0;

    /**
     * Creates a new PathNode at the specified position.
     *
     * @param pos The block position for this node
     */
    public PathNode(BlockPos pos) {
        this.pos = pos;
        this.movement = MovementType.WALK;
    }

    /**
     * Creates a new PathNode with full initialization.
     *
     * @param pos      The block position for this node
     * @param parent   The parent node in the path
     * @param gCost    Cost from start to this node
     * @param hCost    Heuristic cost to goal
     * @param movement Movement type used to reach this node
     */
    public PathNode(BlockPos pos, PathNode parent, double gCost, double hCost, MovementType movement) {
        this.pos = pos;
        this.parent = parent;
        this.gCost = gCost;
        this.hCost = hCost;
        this.movement = movement;
    }

    /**
     * Returns the total estimated cost for this node (gCost + hCost).
     * Used by A* to prioritize node expansion.
     *
     * @return Total cost f = g + h
     */
    public double fCost() {
        return gCost + hCost;
    }

    /**
     * Compares nodes by their fCost for priority queue ordering.
     * Lower fCost values have higher priority.
     *
     * @param other The node to compare with
     * @return Negative if this node has lower cost, positive if higher
     */
    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost(), other.fCost());
    }

    /**
     * Checks equality based on position only.
     * Two nodes are equal if they represent the same block position.
     *
     * @param obj Object to compare with
     * @return true if positions are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathNode)) return false;
        PathNode other = (PathNode) obj;
        return pos.equals(other.pos);
    }

    /**
     * Hash code based on position for use in HashMap/HashSet.
     *
     * @return Hash code of the position
     */
    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    /**
     * Returns a string representation for debugging.
     *
     * @return String with position, costs, and movement type
     */
    @Override
    public String toString() {
        return String.format("PathNode[%s, g=%.1f, h=%.1f, f=%.1f, %s]",
            pos, gCost, hCost, fCost(), movement);
    }

    /**
     * Creates a deep copy of this node without the parent link.
     * Useful for path reconstruction when you don't want the full chain.
     *
     * @return A new PathNode with the same position and costs
     */
    public PathNode copy() {
        PathNode copy = new PathNode(pos);
        copy.gCost = this.gCost;
        copy.hCost = this.hCost;
        copy.movement = this.movement;
        copy.costMultiplier = this.costMultiplier;
        return copy;
    }

    /**
     * Checks if this node is the goal position.
     *
     * @param goal The goal position to check against
     * @return true if this node's position equals the goal
     */
    public boolean isGoal(BlockPos goal) {
        return pos.equals(goal);
    }

    /**
     * Gets the Manhattan distance to another position.
     * Useful for quick distance estimates.
     *
     * @param other The other position
     * @return Manhattan distance (dx + dy + dz)
     */
    public int manhattanDistanceTo(BlockPos other) {
        return Math.abs(pos.getX() - other.getX()) +
               Math.abs(pos.getY() - other.getY()) +
               Math.abs(pos.getZ() - other.getZ());
    }
}
