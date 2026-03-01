package com.minewright.profile;

/**
 * Enumeration of supported task types in the profile system.
 *
 * <p>Each task type corresponds to a specific action that an agent can perform.
 * These map directly to the action system's task names.</p>
 *
 * <p><b>Task Types:</b></p>
 * <ul>
 *   <li><b>MINE:</b> Mine blocks of a specific type within a radius</li>
 *   <li><b>BUILD:</b> Build a structure from a template file</li>
 *   <li><b>GATHER:</b> Gather items/resources from the world</li>
 *   <li><b>CRAFT:</b> Craft items at a crafting table</li>
 *   <li><b>TRAVEL:</b> Travel to a specific location</li>
 *   <li><b>PLACE:</b> Place blocks in specific locations</li>
 *   <li><b>ATTACK:</b> Attack hostile entities</li>
 *   <li><b>FOLLOW:</b> Follow a target entity</li>
 *   <li><b>WAIT:</b> Wait for a specified duration</li>
 *   <li><b>PICKUP:</b> Pick up items from the ground</li>
 *   <li><b>DROP:</b> Drop items from inventory</li>
 *   <li><b>STORE:</b> Store items in a container</li>
 * </ul>
 *
 * @since 1.4.0
 */
public enum TaskType {
    /**
     * Mine blocks of a specific type.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Block type to mine (e.g., "iron_ore")</li>
     *   <li>quantity - Number of blocks to mine</li>
     *   <li>radius - Search radius (default: 32)</li>
     * </ul>
     */
    MINE("mine"),

    /**
     * Build a structure from a template.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Template file path (e.g., "structures/house.json")</li>
     *   <li>location - Build location (e.g., "${player_pos}")</li>
     * </ul>
     */
    BUILD("build"),

    /**
     * Gather resources from the world.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Resource type (e.g., "oak_log")</li>
     *   <li>quantity - Number to gather</li>
     *   <li>radius - Search radius (default: 50)</li>
     * </ul>
     */
    GATHER("gather"),

    /**
     * Craft items at a crafting table.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Item to craft (e.g., "iron_pickaxe")</li>
     *   <li>quantity - Number to craft (default: 1)</li>
     * </ul>
     */
    CRAFT("craft"),

    /**
     * Travel to a specific location.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Destination (e.g., "nearest_furnace", "x,y,z")</li>
     *   <li>distance - Acceptable distance threshold (default: 2)</li>
     * </ul>
     */
    TRAVEL("pathfind"),

    /**
     * Place blocks in the world.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Block type to place</li>
     *   <li>locations - Array of coordinates</li>
     * </ul>
     */
    PLACE("place"),

    /**
     * Attack hostile entities.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Entity type to attack (optional)</li>
     *   <li>radius - Search radius (default: 16)</li>
     * </ul>
     */
    ATTACK("attack"),

    /**
     * Follow a target entity.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Entity to follow (default: "nearest_player")</li>
     *   <li>distance - Follow distance (default: 3)</li>
     * </ul>
     */
    FOLLOW("follow"),

    /**
     * Wait for a specified duration.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>duration - Wait duration in ticks (20 ticks = 1 second)</li>
     * </ul>
     */
    WAIT("wait"),

    /**
     * Pick up items from the ground.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Item type to pick up (optional)</li>
     *   <li>radius - Search radius (default: 8)</li>
     *   <li>quantity - Maximum number to pick up (optional)</li>
     * </ul>
     */
    PICKUP("pickup"),

    /**
     * Drop items from inventory.
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Item type to drop</li>
     *   <li>quantity - Number to drop (default: all)</li>
     * </ul>
     */
    DROP("drop"),

    /**
     * Store items in a container (chest, barrel, etc.).
     *
     * <p><b>Parameters:</b></p>
     * <ul>
     *   <li>target - Container location or "nearest_chest"</li>
     *   <li>items - Items to store</li>
     * </ul>
     */
    STORE("store");

    private final String actionName;

    TaskType(String actionName) {
        this.actionName = actionName;
    }

    /**
     * Gets the action name used by the Task system.
     *
     * @return The action name (e.g., "mine", "build", "craft")
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * Parses a task type from string.
     *
     * @param value The string value (case-insensitive)
     * @return The task type, or null if not found
     */
    public static TaskType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TaskType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Checks if this task type requires a target.
     */
    public boolean requiresTarget() {
        return this != WAIT && this != FOLLOW;
    }
}
