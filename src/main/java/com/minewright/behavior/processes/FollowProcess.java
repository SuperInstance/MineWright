package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import com.minewright.goal.Goals;
import com.minewright.goal.NavigationGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Medium-low priority process for following the player or another entity.
 *
 * <p>This process has medium-low priority (25) and runs when survival is not needed
 * and no tasks are queued. It causes the agent to follow the player or a designated
 * target entity at a comfortable distance.</p>
 *
 * <p><b>Follow Conditions (canRun):</b></p>
 * <ul>
 *   <li>No survival threats</li>
 *   <li>No tasks to execute</li>
 *   <li>Player or target entity exists</li>
 *   <li>Target is far enough to warrant movement</li>
 * </ul>
 *
 * <p><b>Follow Actions (tick):</b></p>
 * <ul>
 *   <li>Pathfind toward target entity</li>
 *   <li>Maintain follow distance (not too close, not too far)</li>
 *   <li>Stop if target is too close</li>
 *   <li>Teleport if target is too far (server-configured)</li>
 * </ul>
 *
 * @see BehaviorProcess
 * @since 1.2.0
 */
public class FollowProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(FollowProcess.class);

    /**
     * Priority level for following (medium-low - below tasks, above idle).
     */
    private static final int PRIORITY = 25;

    /**
     * Minimum distance to start following (blocks).
     */
    private static final double MIN_FOLLOW_DISTANCE = 3.0;

    /**
     * Maximum distance before teleporting (blocks).
     */
    private static final double MAX_TELEPORT_DISTANCE = 64.0;

    /**
     * The foreman entity this process is managing.
     */
    private final ForemanEntity foreman;

    /**
     * The entity to follow (usually the player).
     */
    private Entity followTarget;

    /**
     * The UUID of the owner (player who spawned this foreman).
     * Used for owner tracking and prioritizing who to follow.
     */
    private UUID ownerUUID;

    /**
     * Whether this process is currently active.
     */
    private boolean active = false;

    /**
     * Ticks since process activation.
     */
    private int ticksActive = 0;

    /**
     * Ticks since last position update (for preventing excessive pathfinding).
     */
    private int ticksSinceLastUpdate = 0;

    /**
     * Creates a new FollowProcess for the given foreman.
     *
     * @param foreman The foreman entity to manage following for
     */
    public FollowProcess(ForemanEntity foreman) {
        this.foreman = foreman;
        this.followTarget = null;
        this.ownerUUID = null;
    }

    @Override
    public String getName() {
        return "Follow";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean canRun() {
        // Find follow target
        followTarget = findFollowTarget();

        if (followTarget == null) {
            return false;
        }

        // Check if target is far enough to warrant following
        double distance = distanceTo(followTarget);
        return distance > MIN_FOLLOW_DISTANCE;
    }

    @Override
    public void tick() {
        ticksActive++;
        ticksSinceLastUpdate++;

        // Check if target still exists and is valid
        if (followTarget == null || !followTarget.isAlive()) {
            followTarget = findFollowTarget();
            if (followTarget == null) {
                LOGGER.debug("[{}] Follow target lost, deactivating",
                    foreman.getEntityName());
                return;
            }
        }

        double distance = distanceTo(followTarget);

        // Teleport if target is too far
        if (distance > MAX_TELEPORT_DISTANCE) {
            LOGGER.info("[{}] Teleporting to follow target (distance: {})",
                foreman.getEntityName(), String.format("%.1f", distance));
            teleportToTarget();
            ticksSinceLastUpdate = 0;
            return;
        }

        // Move toward target if far enough
        if (distance > MIN_FOLLOW_DISTANCE && ticksSinceLastUpdate >= 10) {
            moveTowardTarget();
            ticksSinceLastUpdate = 0;
        }

        // Log follow status every 100 ticks
        if (ticksActive % 100 == 0) {
            LOGGER.debug("[{}] Following {} (distance: {})",
                foreman.getEntityName(),
                followTarget.getName().getString(),
                String.format("%.1f", distance));
        }
    }

    @Override
    public void onActivate() {
        active = true;
        ticksActive = 0;
        ticksSinceLastUpdate = 0;

        LOGGER.info("[{}] Follow activated (target: {})",
            foreman.getEntityName(),
            followTarget != null ? followTarget.getName().getString() : "none");

        // Notify player
        foreman.sendChatMessage("I'll follow you!");
    }

    @Override
    public void onDeactivate() {
        active = false;

        LOGGER.debug("[{}] Follow deactivated (was active for {} ticks)",
            foreman.getEntityName(), ticksActive);

        // Clear target
        followTarget = null;
    }

    /**
     * Finds the entity to follow.
     *
     * <p>Priority order:
     * <ol>
     *   <li>Owner (if set and within range)</li>
     *   <li>Nearest player</li>
     *   <li>Null if no valid target</li>
     * </ol>
     *
     * @return The entity to follow, or null if none
     */
    private Entity findFollowTarget() {
        if (foreman.level() == null) {
            return null;
        }

        // Priority 1: Follow owner if set
        if (ownerUUID != null) {
            Player owner = findPlayerByUUID(ownerUUID);
            if (owner != null && distanceTo(owner) <= MAX_TELEPORT_DISTANCE) {
                return owner;
            }
        }

        // Priority 2: Find nearest player
        Player nearestPlayer = foreman.level().getNearestPlayer(
            foreman,
            MAX_TELEPORT_DISTANCE
        );

        if (nearestPlayer != null) {
            return nearestPlayer;
        }

        return null;
    }

    /**
     * Finds a player by their UUID.
     *
     * @param playerUuid The UUID of the player to find
     * @return The player entity, or null if not found
     */
    private Player findPlayerByUUID(UUID playerUuid) {
        if (foreman.level() == null || playerUuid == null) {
            return null;
        }

        // Search for player by UUID in the level
        for (Player player : foreman.level().players()) {
            if (player.getUUID().equals(playerUuid)) {
                return player;
            }
        }

        return null;
    }

    /**
     * Calculates distance to the target entity.
     *
     * @param target The target entity
     * @return Distance in blocks
     */
    private double distanceTo(Entity target) {
        return foreman.position().distanceTo(target.position());
    }

    /**
     * Moves toward the follow target.
     */
    private void moveTowardTarget() {
        if (followTarget == null || foreman.getNavigation() == null) {
            return;
        }

        // Check if already navigating
        if (foreman.getNavigation().isInProgress()) {
            // Check if we're navigating toward the right target
            // If not, stop and restart navigation
            BlockPos currentTarget = foreman.getNavigation().getTargetPos();
            BlockPos targetPos = followTarget.blockPosition();

            if (currentTarget != null && currentTarget.distSqr(targetPos) > 100) {
                // Target moved significantly, re-path
                foreman.getNavigation().stop();
            } else {
                // Still navigating correctly
                return;
            }
        }

        // Use Minecraft's built-in navigation to follow the entity
        // Speed multiplier 1.0 for normal following speed
        foreman.getNavigation().moveTo(followTarget, 1.0);

        LOGGER.debug("[{}] Moving toward follow target {} (distance: {})",
            foreman.getEntityName(),
            followTarget.getName().getString(),
            String.format("%.1f", distanceTo(followTarget)));
    }

    /**
     * Teleports to the follow target.
     */
    private void teleportToTarget() {
        if (followTarget == null) {
            return;
        }

        // Teleport near the target (not exactly on them)
        double offsetX = (Math.random() - 0.5) * 4.0;
        double offsetZ = (Math.random() - 0.5) * 4.0;

        foreman.teleportTo(
            followTarget.getX() + offsetX,
            followTarget.getY(),
            followTarget.getZ() + offsetZ
        );
    }

    /**
     * Gets the current follow target.
     *
     * @return The entity being followed, or null if none
     */
    public Entity getFollowTarget() {
        return followTarget;
    }

    /**
     * Sets the follow target explicitly.
     *
     * <p>Use this to make the foreman follow a specific entity.</p>
     *
     * @param target The entity to follow
     */
    public void setFollowTarget(Entity target) {
        this.followTarget = target;
        LOGGER.debug("[{}] Follow target set to: {}",
            foreman.getEntityName(),
            target != null ? target.getName().getString() : "none");
    }

    /**
     * Gets the number of ticks this process has been active.
     *
     * @return Ticks active
     */
    public int getTicksActive() {
        return ticksActive;
    }

    // ========== Owner Tracking ==========

    /**
     * Gets the owner's UUID.
     *
     * @return The owner's UUID, or null if no owner is set
     */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Sets the owner's UUID.
     *
     * <p>The owner is prioritized when choosing who to follow.
     * If the owner is within range, the foreman will follow them
     * instead of other nearby players.</p>
     *
     * @param ownerUuid The UUID of the owner (usually the player who spawned the foreman)
     */
    public void setOwnerUUID(UUID ownerUuid) {
        this.ownerUUID = ownerUuid;
        LOGGER.info("[{}] Owner UUID set to: {}",
            foreman.getEntityName(),
            ownerUuid != null ? ownerUuid.toString() : "none");
    }

    /**
     * Clears the owner UUID.
     *
     * <p>After clearing, the foreman will follow the nearest player
     * instead of a specific owner.</p>
     */
    public void clearOwner() {
        this.ownerUUID = null;
        LOGGER.debug("[{}] Owner cleared", foreman.getEntityName());
    }

    /**
     * Checks if the foreman has an owner set.
     *
     * @return true if an owner UUID is set
     */
    public boolean hasOwner() {
        return ownerUUID != null;
    }

    /**
     * Gets the owner entity if they are in the current level.
     *
     * @return The owner player entity, or null if not found/online
     */
    public Player getOwnerEntity() {
        if (ownerUUID == null) {
            return null;
        }
        return findPlayerByUUID(ownerUUID);
    }
}
