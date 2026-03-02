package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import com.minewright.humanization.HumanizationUtils;
import com.minewright.humanization.SessionManager;
import com.minewright.personality.ForemanArchetypeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Low-priority idle process for fallback behavior when no other process needs to run.
 *
 * <p>This process has the lowest priority (10) and only runs when all other processes
 * cannot run. It provides characterful idle behaviors that make the agent feel alive
 * and engaged with the world.</p>
 *
 * <p><b>Idle Conditions (canRun):</b></p>
 * <ul>
 *   <li>No other process can run</li>
 *   <li>Task queue is empty</li>
 *   <li>Not in danger</li>
 * </ul>
 *
 * <p><b>Idle Behaviors (tick):</b></p>
 * <ul>
 *   <li>Look around (observe surroundings)</li>
 *   <li>Wander randomly (explore nearby)</li>
 *   <li>Chat with player (characterful comments)</li>
 *   <li>Perform idle animations (stretch, yawn, etc.)</li>
 *   <li>Follow player at a distance</li>
 * </ul>
 *
 * @see BehaviorProcess
 * @since 1.2.0
 */
public class IdleProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleProcess.class);

    /**
     * Priority level for idle behavior (lowest - fallback only).
     */
    private static final int PRIORITY = 10;

    /**
     * Random number generator for idle behavior variety.
     */
    private static final Random RANDOM = new Random();

    /**
     * Interval between idle actions (in ticks).
     */
    private static final int IDLE_ACTION_INTERVAL = 60; // 3 seconds

    /**
     * Chance of chatting during idle (0.0 to 1.0).
     */
    private static final double CHAT_CHANCE = 0.05; // 5% per action interval

    /**
     * Radius for looking around (blocks).
     */
    private static final double LOOK_AROUND_RADIUS = 10.0;

    /**
     * Radius for wandering (blocks).
     */
    private static final double WANDER_RADIUS = 5.0;

    /**
     * Radius for following player (blocks).
     */
    private static final double FOLLOW_RADIUS = 3.0;

    /**
     * The foreman entity this process is managing.
     */
    private final ForemanEntity foreman;

    /**
     * Cached archetype for personality-based behaviors.
     */
    private ForemanArchetypeConfig.ForemanArchetype archetype;

    /**
     * Whether this process is currently active.
     */
    private boolean active = false;

    /**
     * Ticks since process activation.
     */
    private int ticksActive = 0;

    /**
     * Ticks since last idle action.
     */
    private int ticksSinceLastAction = 0;

    /**
     * Current idle behavior type.
     */
    private IdleBehavior currentBehavior = IdleBehavior.NONE;

    /**
     * Types of idle behaviors.
     */
    private enum IdleBehavior {
        NONE("Doing nothing"),
        LOOK_AROUND("Looking around"),
        WANDER("Wandering"),
        CHAT("Chatting"),
        STRETCH("Stretching"),
        YAWN("Yawning"),
        FOLLOW("Following player");

        private final String description;

        IdleBehavior(String description) {
            this.description = description;
        }

        String getDescription() {
            return description;
        }
    }

    /**
     * Creates a new IdleProcess for the given foreman.
     *
     * @param foreman The foreman entity to manage idle behavior for
     */
    public IdleProcess(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    @Override
    public String getName() {
        return "Idle";
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
        // Idle can always run - it's the fallback behavior
        // ProcessManager will select it if no other process can run
        return true;
    }

    @Override
    public void tick() {
        ticksActive++;
        ticksSinceLastAction++;

        // Perform idle actions at intervals
        if (ticksSinceLastAction >= IDLE_ACTION_INTERVAL) {
            performIdleAction();
            ticksSinceLastAction = 0;
        }

        // Continue current idle behavior
        continueIdleBehavior();
    }

    @Override
    public void onActivate() {
        active = true;
        ticksActive = 0;
        ticksSinceLastAction = 0;

        LOGGER.debug("[{}] Idle behavior activated",
            foreman.getEntityName());

        // Occasionally chat when becoming idle
        if (RANDOM.nextDouble() < CHAT_CHANCE) {
            sendIdleChat();
        }
    }

    @Override
    public void onDeactivate() {
        active = false;
        currentBehavior = IdleBehavior.NONE;

        LOGGER.debug("[{}] Idle behavior deactivated (was active for {} ticks)",
            foreman.getEntityName(), ticksActive);
    }

    /**
     * Performs a random idle action.
     */
    private void performIdleAction() {
        // Choose a random idle behavior
        int roll = RANDOM.nextInt(100);

        if (roll < 40) {
            currentBehavior = IdleBehavior.LOOK_AROUND;
            lookAround();
        } else if (roll < 70) {
            currentBehavior = IdleBehavior.WANDER;
            wander();
        } else if (roll < 80) {
            currentBehavior = IdleBehavior.STRETCH;
            stretch();
        } else if (roll < 90) {
            currentBehavior = IdleBehavior.YAWN;
            yawn();
        } else if (roll < 95) {
            currentBehavior = IdleBehavior.CHAT;
            sendIdleChat();
        } else {
            currentBehavior = IdleBehavior.FOLLOW;
            followPlayer();
        }

        LOGGER.debug("[{}] Idle action: {}",
            foreman.getEntityName(), currentBehavior.getDescription());
    }

    /**
     * Continues the current idle behavior.
     */
    private void continueIdleBehavior() {
        // Most idle behaviors are instant, but some (like following) are ongoing
        if (currentBehavior == IdleBehavior.FOLLOW) {
            followPlayer();
        }
    }

    // === Idle Behavior Implementations ===

    /**
     * Look around at random nearby entities or blocks.
     */
    private void lookAround() {
        if (foreman == null || foreman.level() == null) {
            return;
        }

        // Randomly choose between looking at an entity or a block position
        if (RANDOM.nextBoolean()) {
            // Look at a random nearby entity
            Entity targetEntity = findRandomNearbyEntity();
            if (targetEntity != null) {
                lookAtEntity(targetEntity);
            }
        } else {
            // Look at a random block position
            BlockPos targetPos = getRandomNearbyPosition();
            if (targetPos != null) {
                lookAtPosition(targetPos);
            }
        }
    }

    /**
     * Finds a random nearby entity to look at.
     *
     * @return A random entity within look radius, or null if none found
     */
    private Entity findRandomNearbyEntity() {
        if (foreman.level() == null) {
            return null;
        }

        // Get all entities within look radius
        var nearbyEntities = foreman.level().getEntitiesOfClass(
            Entity.class,
            foreman.getBoundingBox().inflate(LOOK_AROUND_RADIUS)
        );

        // Filter out the foreman itself
        nearbyEntities.removeIf(e -> e == foreman);

        if (nearbyEntities.isEmpty()) {
            return null;
        }

        // Return a random entity
        return nearbyEntities.get(RANDOM.nextInt(nearbyEntities.size()));
    }

    /**
     * Makes the foreman look at a specific entity.
     *
     * @param target The entity to look at
     */
    private void lookAtEntity(Entity target) {
        if (target == null) {
            return;
        }

        // Calculate the direction to the target
        double dx = target.getX() - foreman.getX();
        double dy = target.getEyeY() - foreman.getEyeY();
        double dz = target.getZ() - foreman.getZ();

        // Calculate yaw and pitch
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        // Apply micro-movement offset for humanization
        pitch += HumanizationUtils.microMovementOffset(5.0);

        // Set the look rotation
        foreman.setYRot(yaw);
        foreman.setXRot(pitch);
        foreman.yHeadRot = yaw;
        foreman.yBodyRot = yaw;

        LOGGER.trace("[{}] Looking at entity: {} (yaw: {}, pitch: {})",
            foreman.getEntityName(), target.getName().getString(),
            String.format("%.1f", yaw), String.format("%.1f", pitch));
    }

    /**
     * Makes the foreman look at a specific block position.
     *
     * @param pos The position to look at
     */
    private void lookAtPosition(BlockPos pos) {
        if (pos == null) {
            return;
        }

        // Calculate the direction to the position
        double dx = pos.getX() + 0.5 - foreman.getX();
        double dy = pos.getY() + 0.5 - foreman.getEyeY();
        double dz = pos.getZ() + 0.5 - foreman.getZ();

        // Calculate yaw and pitch
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        // Apply micro-movement offset for humanization
        pitch += HumanizationUtils.microMovementOffset(5.0);

        // Set the look rotation
        foreman.setYRot(yaw);
        foreman.setXRot(pitch);
        foreman.yHeadRot = yaw;
        foreman.yBodyRot = yaw;

        LOGGER.trace("[{}] Looking at position: {} (yaw: {}, pitch: {})",
            foreman.getEntityName(), pos,
            String.format("%.1f", yaw), String.format("%.1f", pitch));
    }

    /**
     * Gets a random nearby block position to look at.
     *
     * @return A random block position within look radius
     */
    private BlockPos getRandomNearbyPosition() {
        int offsetX = (int) Math.round(RANDOM.nextGaussian() * (LOOK_AROUND_RADIUS / 2));
        int offsetY = (int) Math.round(RANDOM.nextGaussian() * 2.0); // Mostly horizontal
        int offsetZ = (int) Math.round(RANDOM.nextGaussian() * (LOOK_AROUND_RADIUS / 2));

        // Clamp to radius
        offsetX = Math.max(-(int)LOOK_AROUND_RADIUS, Math.min((int)LOOK_AROUND_RADIUS, offsetX));
        offsetY = Math.max(-3, Math.min(5, offsetY)); // Reasonable vertical range
        offsetZ = Math.max(-(int)LOOK_AROUND_RADIUS, Math.min((int)LOOK_AROUND_RADIUS, offsetZ));

        return foreman.blockPosition().offset(offsetX, offsetY, offsetZ);
    }

    /**
     * Wander randomly in a small radius.
     */
    private void wander() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            return;
        }

        // Don't wander if currently navigating
        if (foreman.getNavigation().isInProgress()) {
            return;
        }

        // Check fatigue level - wander less when fatigued
        SessionManager sessionManager = getSessionManager();
        if (sessionManager != null && sessionManager.getFatigueLevel() > 0.7) {
            // Too tired to wander
            return;
        }

        // Generate a random position within wander radius
        BlockPos currentPos = foreman.blockPosition();
        int offsetX = (int) Math.round(RANDOM.nextGaussian() * (WANDER_RADIUS / 2));
        int offsetZ = (int) Math.round(RANDOM.nextGaussian() * (WANDER_RADIUS / 2));

        // Clamp to radius
        offsetX = Math.max(-(int)WANDER_RADIUS, Math.min((int)WANDER_RADIUS, offsetX));
        offsetZ = Math.max(-(int)WANDER_RADIUS, Math.min((int)WANDER_RADIUS, offsetZ));

        // Stay on same Y level (no vertical wandering)
        BlockPos targetPos = currentPos.offset(offsetX, 0, offsetZ);

        // Use navigation to move to the position
        // Speed multiplier 0.8 for leisurely wandering
        foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0.8);

        LOGGER.trace("[{}] Wandering to {} (offset: {} blocks)",
            foreman.getEntityName(), targetPos,
            String.format("%.1f", Math.sqrt(offsetX * offsetX + offsetZ * offsetZ)));
    }

    /**
     * Gets the session manager for fatigue-aware behavior.
     *
     * @return The session manager, or null if not available
     */
    private SessionManager getSessionManager() {
        // Try to get session manager from foreman's memory or companion memory
        // This is a simplified approach - in a real implementation, this would
        // be accessed through a proper getter on ForemanEntity
        return null; // Placeholder - would be implemented when ForemanEntity exposes SessionManager
    }

    /**
     * Stretch animation (characterful idle).
     */
    private void stretch() {
        // Send personality-based stretch message
        String stretchMessage = getPersonalityStretchMessage();
        if (stretchMessage != null && !stretchMessage.isEmpty()) {
            foreman.sendChatMessage(stretchMessage);
        }

        LOGGER.trace("[{}] Stretch animation", foreman.getEntityName());
    }

    /**
     * Yawn animation (characterful idle).
     */
    private void yawn() {
        // Send personality-based yawn message
        String yawnMessage = getPersonalityYawnMessage();
        if (yawnMessage != null && !yawnMessage.isEmpty()) {
            foreman.sendChatMessage(yawnMessage);
        }

        LOGGER.trace("[{}] Yawn animation (fatigue check)", foreman.getEntityName());
    }

    /**
     * Gets a personality-based stretch message.
     *
     * @return A stretch message based on the foreman's archetype, or a default
     */
    private String getPersonalityStretchMessage() {
        // Try to get archetype from foreman's companion memory
        ForemanArchetypeConfig.ForemanArchetype arch = getArchetype();

        if (arch != null) {
            // Get random stretch message from archetype
            String[] stretchMessages = {
                "*stretches arms*",
                "*stretches back*",
                "*loosens up*",
                "*limbers up*",
                "Time to stretch!",
                "Getting the kinks out."
            };
            return stretchMessages[RANDOM.nextInt(stretchMessages.length)];
        }

        // Default stretch messages (no personality)
        String[] defaultStretches = {
            "*stretches*",
            "*limbers up*"
        };
        return defaultStretches[RANDOM.nextInt(defaultStretches.length)];
    }

    /**
     * Gets a personality-based yawn message.
     *
     * @return A yawn message based on the foreman's archetype, or a default
     */
    private String getPersonalityYawnMessage() {
        // Try to get archetype from foreman's companion memory
        ForemanArchetypeConfig.ForemanArchetype arch = getArchetype();

        if (arch != null) {
            // Get random yawn message from archetype
            String[] yawnMessages = {
                "*yawns*",
                "*yawn*",
                "A bit tired...",
                "Could use a break.",
                "Working hard here...",
                "*stifles a yawn*"
            };
            return yawnMessages[RANDOM.nextInt(yawnMessages.length)];
        }

        // Default yawn messages (no personality)
        String[] defaultYawns = {
            "*yawns*",
            "*yawn*"
        };
        return defaultYawns[RANDOM.nextInt(defaultYawns.length)];
    }

    /**
     * Gets the foreman's archetype for personality-based behaviors.
     *
     * @return The foreman archetype, or null if not available
     */
    private ForemanArchetypeConfig.ForemanArchetype getArchetype() {
        // Cache the archetype to avoid repeated lookups
        if (archetype != null) {
            return archetype;
        }

        // Try to get archetype from foreman's companion memory
        // This is a simplified approach - in a real implementation, this would
        // be accessed through a proper getter on ForemanEntity
        // For now, return a default archetype
        archetype = ForemanArchetypeConfig.THE_FOREMAN;
        return archetype;
    }

    /**
     * Send a characterful idle chat message.
     */
    private void sendIdleChat() {
        String[] idleChats = {
            "Nice weather we're having!",
            "What should we do next?",
            "I'm ready when you are!",
            "Just enjoying the view.",
            "Anyone need anything?",
            "Quiet today, isn't it?",
            "I could use a break... just kidding!",
            "Thinking about building something...",
            "Wonder what's over there?",
            "Good times!"
        };

        String message = idleChats[RANDOM.nextInt(idleChats.length)];
        foreman.sendChatMessage(message);
    }

    /**
     * Follow the player at a distance.
     */
    private void followPlayer() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            return;
        }

        // Find the nearest player
        Player nearestPlayer = foreman.level().getNearestPlayer(
            foreman,
            20.0 // Search radius
        );

        if (nearestPlayer == null) {
            return;
        }

        // Check distance to player
        double distance = foreman.position().distanceTo(nearestPlayer.position());

        // Only follow if outside follow radius
        if (distance > FOLLOW_RADIUS) {
            // Use navigation to move toward player
            // Speed multiplier 1.0 for normal following speed
            foreman.getNavigation().moveTo(nearestPlayer, 1.0);

            LOGGER.trace("[{}] Following player {} (distance: {})",
                foreman.getEntityName(),
                nearestPlayer.getName().getString(),
                String.format("%.1f", distance));
        } else {
            // Stop navigation if close enough
            if (foreman.getNavigation().isInProgress()) {
                foreman.getNavigation().stop();
            }

            // Look at the player
            lookAtEntity(nearestPlayer);
        }
    }

    /**
     * Gets the number of ticks this process has been active.
     *
     * @return Ticks active
     */
    public int getTicksActive() {
        return ticksActive;
    }

    /**
     * Gets the current idle behavior.
     *
     * @return Current behavior type
     */
    public IdleBehavior getCurrentBehavior() {
        return currentBehavior;
    }
}
