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
 * <p><b>PERFORMANCE NOTE:</b> Fields are public for direct access in performance-critical
 * pathfinding code. A* pathfinding is called every tick and processes thousands of nodes,
 * so getter/setter overhead would significantly impact performance. This is an intentional
 * trade-off for performance over encapsulation.</p>
 *
 * @see AStarPathfinder
 * @see MovementType
 * @see PathfindingContext
 */
public class PathNode implements Comparable<PathNode> {
    /** The block position this node represents. Mutable for object pooling. */
    public BlockPos pos;

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

    // === Getters and Setters ===

    /** Gets the block position this node represents. */
    public BlockPos getPos() { return pos; }

    /** Sets the block position (used for object pooling). */
    public void setPos(BlockPos pos) { this.pos = pos; }

    /** Gets the actual cost from start to this node. */
    public double getGCost() { return gCost; }

    /** Sets the actual cost from start to this node. */
    public void setGCost(double gCost) { this.gCost = gCost; }

    /** Gets the heuristic estimated cost from this node to goal. */
    public double getHCost() { return hCost; }

    /** Sets the heuristic estimated cost from this node to goal. */
    public void setHCost(double hCost) { this.hCost = hCost; }

    /** Gets the parent node in the path. */
    public PathNode getParent() { return parent; }

    /** Sets the parent node in the path. */
    public void setParent(PathNode parent) { this.parent = parent; }

    /** Gets the movement type used to reach this node. */
    public MovementType getMovement() { return movement; }

    /** Sets the movement type used to reach this node. */
    public void setMovement(MovementType movement) { this.movement = movement; }

    /** Gets the cost multiplier for this position. */
    public double getCostMultiplier() { return costMultiplier; }

    /** Sets the cost multiplier for this position (must be >= 0). */
    public void setCostMultiplier(double costMultiplier) {
        this.costMultiplier = Math.max(0, costMultiplier);
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

    /**
     * Resets this node's state for object pooling.
     *
     * <p>Called by AStarPathfinder when reusing pooled nodes
     * to avoid creating new objects during pathfinding.</p>
     *
     * @param newPos   New block position
     * @param parent   Parent node in path
     * @param gCost    Cost from start
     * @param hCost    Heuristic cost to goal
     * @param movement Movement type to reach this node
     */
    public void reset(BlockPos newPos, PathNode parent, double gCost, double hCost, MovementType movement) {
        this.pos = newPos;
        this.parent = parent;
        this.gCost = gCost;
        this.hCost = hCost;
        this.movement = movement;
        this.costMultiplier = 1.0;
    }
}
