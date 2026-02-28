package com.minewright.pathfinding;

/**
 * Represents the type of movement used to reach a pathfinding node.
 *
 * <p>Different movement types have different costs and constraints,
 * which affects both pathfinding decisions and path execution.</p>
 *
 * <p><b>Cost Hierarchy:</b> Generally, simpler movements (WALK) have lower costs
 * than complex movements (FALL, CLIMB). This encourages the pathfinder to find
 * paths using the most natural movements.</p>
 *
 * <p><b>Validation:</b> Each movement type requires specific world conditions
 * to be valid. For example, JUMP requires a solid block beneath, while SWIM
 * requires water at the agent's position.</p>
 *
 * @see MovementValidator
 * @see PathfindingContext
 */
public enum MovementType {
    /**
     * Standard walking on solid ground.
     * Cost: 1.0 (baseline)
     * Requirements: Solid block beneath, 2-block clearance above
     */
    WALK(1.0, "Walking"),

    /**
     * Jumping up one block.
     * Cost: 1.5 (slightly more expensive than walking)
     * Requirements: Solid block to jump onto, head clearance
     */
    JUMP(1.5, "Jumping"),

    /**
     * Falling down from a height.
     * Cost: 0.5 (cheaper, but may cause fall damage)
     * Requirements: Drop is within safe fall distance
     */
    FALL(0.5, "Falling"),

    /**
     * Climbing up a ladder, vine, or similar climbable block.
     * Cost: 1.2 (slightly slower than walking)
     * Requirements: Climbable block at position
     */
    CLIMB(1.2, "Climbing"),

    /**
     * Swimming through water.
     * Cost: 2.0 (significantly slower)
     * Requirements: Water source block at agent's feet level
     */
    SWIM(2.0, "Swimming"),

    /**
     * Moving through water with floating at surface.
     * Cost: 1.8 (slightly faster than submerged swimming)
     * Requirements: Water with air above at head level
     */
    WATER_WALK(1.8, "Water Walking"),

    /**
     * Descending through water (swimming down).
     * Cost: 2.5 (slowest movement, requires swimming against buoyancy)
     * Requirements: Water below current position
     */
    DESCEND_WATER(2.5, "Descending Water"),

    /**
     * Parkour jump - jumping across a gap without landing on intermediate blocks.
     * Cost: 3.0 (expensive, used as last resort)
     * Requirements: Clear trajectory to landing spot
     */
    PARKOUR(3.0, "Parkour"),

    /**
     * Traversing through a 1.5-block high space (sneaking).
     * Cost: 1.3 (slower than normal walking)
     * Requirements: Upper block is slab, stair, or partial
     */
    SNEAK(1.3, "Sneaking"),

    /**
     * Flying movement (when agent has flight capability).
     * Cost: 0.8 (faster than walking, ignores terrain)
     * Requirements: Agent has flight enabled
     */
    FLY(0.8, "Flying");

    private final double cost;
    private final String displayName;

    MovementType(double cost, String displayName) {
        this.cost = cost;
        this.displayName = displayName;
    }

    /**
     * Gets the base movement cost multiplier.
     *
     * @return Cost multiplier for this movement type
     */
    public double getCost() {
        return cost;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return Display name for this movement type
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this movement type involves vertical ascent.
     *
     * @return true if movement goes upward
     */
    public boolean isAscending() {
        return this == JUMP || this == CLIMB || this == PARKOUR;
    }

    /**
     * Checks if this movement type involves vertical descent.
     *
     * @return true if movement goes downward
     */
    public boolean isDescending() {
        return this == FALL || this == DESCEND_WATER;
    }

    /**
     * Checks if this movement type requires water.
     *
     * @return true if movement requires water
     */
    public boolean isWaterMovement() {
        return this == SWIM || this == WATER_WALK || this == DESCEND_WATER;
    }

    /**
     * Checks if this movement type bypasses normal terrain constraints.
     *
     * @return true if movement ignores standard collision
     */
    public boolean bypassesTerrain() {
        return this == FLY || this == PARKOUR;
    }

    /**
     * Checks if this movement type is safe (won't cause damage).
     *
     * @return true if movement is inherently safe
     */
    public boolean isSafe() {
        return this != FALL && this != PARKOUR;
    }
}
