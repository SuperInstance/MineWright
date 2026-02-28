package com.minewright.exception;

/**
 * Exception thrown when entity operations fail.
 *
 * <p>Entity exceptions can occur due to:
 * <ul>
 *   <li>Spawn failures (invalid location, world not ready)</li>
 *   <li>Tick errors (invalid state, null references)</li>
 *   <li>State inconsistencies</li>
 *   <li>Entity not found errors</li>
 * </ul>
 *
 * <p><b>Recovery:</b> Entity errors typically require:
 * <ul>
 *   <li>Removing and respawning the entity</li>
 *   <li>Resetting entity state</li>
 *   <li>Checking world/server conditions</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class EntityException extends MineWrightException {
    private static final long serialVersionUID = 1L;

    private final String entityId;
    private final String entityName;

    /**
     * Constructs a new EntityException.
     *
     * @param message            Error message
     * @param entityId           Entity UUID or identifier
     * @param entityName         Entity display name
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     */
    public EntityException(String message, String entityId, String entityName,
                          ErrorCode errorCode, String recoverySuggestion) {
        super(message, errorCode, recoverySuggestion);
        this.entityId = entityId;
        this.entityName = entityName;
    }

    /**
     * Constructs a new EntityException with cause.
     *
     * @param message            Error message
     * @param entityId           Entity UUID or identifier
     * @param entityName         Entity display name
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param cause              Underlying cause
     */
    public EntityException(String message, String entityId, String entityName,
                          ErrorCode errorCode, String recoverySuggestion, Throwable cause) {
        super(message, errorCode, recoverySuggestion,
            String.format("Entity: %s (ID: %s)", entityName, entityId), cause);
        this.entityId = entityId;
        this.entityName = entityName;
    }

    /**
     * Returns the entity ID.
     *
     * @return Entity ID
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Returns the entity name.
     *
     * @return Entity name
     */
    public String getEntityName() {
        return entityName;
    }

    // Static factory methods for common errors

    /**
     * Creates an exception for spawn failures.
     *
     * @param entityName The entity name that failed to spawn
     * @param reason     Why spawn failed
     * @return EntityException instance
     */
    public static EntityException spawnFailed(String entityName, String reason) {
        return new EntityException(
            "Failed to spawn entity '" + entityName + "': " + reason,
            "unknown",
            entityName,
            ErrorCode.ENTITY_SPAWN_FAILED,
            "Could not spawn the crew member. " +
            "Ensure there is valid spawn space and the world is fully loaded. " +
            "Try removing any entities in the way and respawning."
        );
    }

    /**
     * Creates an exception for tick errors.
     *
     * @param entityId   Entity ID
     * @param entityName Entity name
     * @param reason     Why the tick failed
     * @param cause      Underlying cause
     * @return EntityException instance
     */
    public static EntityException tickError(String entityId, String entityName,
                                           String reason, Throwable cause) {
        return new EntityException(
            "Entity tick error for '" + entityName + "': " + reason,
            entityId,
            entityName,
            ErrorCode.ENTITY_TICK_ERROR,
            "The entity encountered an error during its tick update. " +
            "This is usually recoverable - the entity will continue functioning. " +
            "If errors persist, try removing and respawning the entity.",
            cause
        );
    }

    /**
     * Creates an exception for invalid state.
     *
     * @param entityId   Entity ID
     * @param entityName Entity name
     * @param state      The invalid state
     * @return EntityException instance
     */
    public static EntityException invalidState(String entityId, String entityName, String state) {
        return new EntityException(
            "Entity '" + entityName + "' in invalid state: " + state,
            entityId,
            entityName,
            ErrorCode.ENTITY_INVALID_STATE,
            "The entity has entered an invalid state. " +
            "Try using commands to reset the entity state or remove and respawn it."
        );
    }

    /**
     * Creates an exception for entity not found.
     *
     * @param entityName The entity name that wasn't found
     * @return EntityException instance
     */
    public static EntityException notFound(String entityName) {
        return new EntityException(
            "Entity not found: " + entityName,
            "unknown",
            entityName,
            ErrorCode.ENTITY_NOT_FOUND,
            "The specified crew member could not be found. " +
            "Use /crew list to see active members and check the name is correct."
        );
    }

    @Override
    public String toString() {
        return String.format("EntityException[entityName='%s', entityId='%s', code=%d, message='%s']",
            entityName, entityId, getErrorCode().getCode(), getMessage());
    }
}
