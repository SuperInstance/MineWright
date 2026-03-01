package com.minewright.recovery;

/**
 * Categories of stuck conditions that can affect AI agents.
 *
 * <p><b>Stuck Types:</b></p>
 * <ul>
 *   <li><b>POSITION_STUCK:</b> Agent hasn't moved from its current position
 *       despite attempts to move. Typically indicates physical blockage.</li>
 *   <li><b>PROGRESS_STUCK:</b> Agent is moving but not making progress toward
 *       the goal. Task progress counter hasn't increased.</li>
 *   <li><b>STATE_STUCK:</b> Agent state machine hasn't transitioned in an
 *       unusually long time. Indicates logic deadlock.</li>
 *   <li><b>PATH_STUCK:</b> Pathfinding is unable to find a valid path to
 *       the target. Navigation system failure.</li>
 *   <li><b>RESOURCE_STUCK:</b> Agent cannot acquire required resources
 *       (blocks, items, tools) to complete the task.</li>
 * </ul>
 *
 * <p><b>Recovery Strategy Selection:</b></p>
 * <p>Each stuck type maps to appropriate recovery strategies:</p>
 * <ul>
 *   <li>POSITION_STUCK → TeleportStrategy, RepathStrategy</li>
 *   <li>PROGRESS_STUCK → RepathStrategy, AbortStrategy</li>
 *   <li>STATE_STUCK → AbortStrategy (requires state machine reset)</li>
 *   <li>PATH_STUCK → RepathStrategy, TeleportStrategy</li>
 *   <li>RESOURCE_STUCK → AbortStrategy (requires user intervention)</li>
 * </ul>
 *
 * @since 1.1.0
 * @see StuckDetector
 * @see RecoveryStrategy
 */
public enum StuckType {

    /**
     * Agent is physically stuck - not moving despite movement commands.
     * Common causes: walls, obstacles, getting stuck in blocks.
     */
    POSITION_STUCK("Position Stuck", "Agent hasn't moved from current position"),

    /**
     * Agent is not making task progress - moving in circles or wrong direction.
     * Common causes: incorrect path, navigation errors, goal confusion.
     */
    PROGRESS_STUCK("Progress Stuck", "Task progress hasn't increased"),

    /**
     * Agent state machine is stuck - no state transitions occurring.
     * Common causes: logic deadlock, waiting on unavailable resource.
     */
    STATE_STUCK("State Stuck", "Agent state hasn't transitioned"),

    /**
     * Pathfinding cannot find a valid path to target.
     * Common causes: unreachable goal, terrain obstacles, path calculation failure.
     */
    PATH_STUCK("Path Stuck", "No valid path to target"),

    /**
     * Agent cannot acquire required resources.
     * Common causes: missing blocks, insufficient tools, items not available.
     */
    RESOURCE_STUCK("Resource Stuck", "Required resources unavailable");

    private final String displayName;
    private final String description;

    StuckType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the stuck type description.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this stuck type can be recovered via re-pathing.
     *
     * @return true if re-pathing is a viable recovery strategy
     */
    public boolean canRepath() {
        return this == POSITION_STUCK || this == PATH_STUCK;
    }

    /**
     * Checks if this stuck type can be recovered via teleportation.
     *
     * @return true if teleportation is a viable recovery strategy
     */
    public boolean canTeleport() {
        return this == POSITION_STUCK || this == PATH_STUCK;
    }

    /**
     * Checks if this stuck type requires task abortion.
     *
     * @return true if abortion is the recommended recovery strategy
     */
    public boolean shouldAbort() {
        return this == STATE_STUCK || this == RESOURCE_STUCK;
    }

    @Override
    public String toString() {
        return displayName + ": " + description;
    }
}
