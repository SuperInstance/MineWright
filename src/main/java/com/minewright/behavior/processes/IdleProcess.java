package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
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
     * The foreman entity this process is managing.
     */
    private final ForemanEntity foreman;

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
        // TODO: Implement looking at random nearby targets
        // This would involve setting the entity's look rotation
    }

    /**
     * Wander randomly in a small radius.
     */
    private void wander() {
        // TODO: Implement random wandering
        // This would involve pathfinding to a random nearby position
    }

    /**
     * Stretch animation (characterful idle).
     */
    private void stretch() {
        // TODO: Implement stretch animation
        // This would involve playing an animation or particle effect
    }

    /**
     * Yawn animation (characterful idle).
     */
    private void yawn() {
        // TODO: Implement yawn animation
        // This would involve playing an animation or sound
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
        // TODO: Implement follow behavior
        // This would involve pathfinding to stay near the player
        // For now, this is handled by IdleFollowAction in ActionExecutor
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
