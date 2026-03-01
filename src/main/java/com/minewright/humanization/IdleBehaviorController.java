package com.minewright.humanization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Controls idle behaviors for AI agents to make them feel more alive.
 *
 * <p>Humans are rarely completely still - they fidget, look around, shift weight,
 * and perform small actions even when not actively working on tasks. This class
 * simulates those idle behaviors.</p>
 *
 * <h2>Idle Behavior Types</h2>
 * <ul>
 *   <li><b>LOOK_AROUND</b> - Rotate head to survey surroundings</li>
 *   <li><b>FIDGET</b> - Small positional shifts and movements</li>
 *   <li><b>STRETCH</b> - Occasional stretching animation</li>
 *   <li><b>CHECK_INVENTORY</b> - Brief inventory inspection</li>
 *   <li><b>EMOTE</b> - Personality-driven emotes or sounds</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IdleBehaviorController controller = new IdleBehaviorController(personality);
 *
 * // In agent tick() method:
 * controller.tick(() -> {
 *     // Perform idle action if triggered
 *     // This callback will execute the actual idle behavior
 * });
 * }</pre>
 *
 * @see HumanizationUtils
 * @see MistakeSimulator
 * @see SessionManager
 * @since 2.2.0
 */
public class IdleBehaviorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleBehaviorController.class);

    /**
     * Default chance per tick of performing an idle action (2%).
     */
    private static final double DEFAULT_IDLE_CHANCE = 0.02;

    /**
     * Minimum cooldown between idle actions in ticks.
     */
    private static final int MIN_COOLDOWN_TICKS = 20; // 1 second

    /**
     * Maximum cooldown between idle actions in ticks.
     */
    private static final int MAX_COOLDOWN_TICKS = 200; // 10 seconds

    /**
     * Random number generator for idle behavior calculations.
     */
    private final Random random;

    /**
     * Personality traits affecting idle behavior.
     */
    private final PersonalityTraits personality;

    /**
     * Current cooldown until next idle action can occur.
     */
    private int cooldownTicks;

    /**
     * Whether idle behaviors are enabled.
     */
    private boolean enabled;

    /**
     * Callback for executing idle actions.
     */
    private IdleActionExecutor executor;

    /**
     * Types of idle behaviors.
     */
    public enum IdleAction {
        /**
         * Rotate head to look around at surroundings.
         */
        LOOK_AROUND,

        /**
         * Small fidgeting movements (shift weight, small steps).
         */
        FIDGET,

        /**
         * Stretching animation/pose.
         */
        STRETCH,

        /**
         * Brief inventory check gesture.
         */
        CHECK_INVENTORY,

        /**
         * Personality-driven emote or sound.
         */
        EMOTE,

        /**
         * Small movement in random direction.
         */
        SMALL_STEP,

        /**
         * Stand still (no action).
         */
        STAND_STILL
    }

    /**
     * Simple personality traits interface.
     */
    public interface PersonalityTraits {
        /**
         * Gets extraversion level (0-100).
         * Higher = more social, more idle actions.
         */
        int getExtraversion();

        /**
         * Gets neuroticism level (0-100).
         * Higher = more anxious, more fidgeting.
         */
        int getNeuroticism();

        /**
         * Gets openness level (0-100).
         * Higher = more varied idle behaviors.
         */
        int getOpenness();

        /**
         * Gets conscientiousness level (0-100).
         * Higher = fewer idle actions, more focused.
         */
        int getConscientiousness();
    }

    /**
     * Functional interface for executing idle actions.
     */
    @FunctionalInterface
    public interface IdleActionExecutor {
        /**
         * Executes an idle action.
         *
         * @param action The action to execute
         * @param data Optional data for the action (e.g., rotation amounts)
         */
        void execute(IdleAction action, double[] data);
    }

    /**
     * Creates an idle behavior controller with default personality.
     */
    public IdleBehaviorController() {
        this(new DefaultPersonality());
    }

    /**
     * Creates an idle behavior controller with specified personality traits.
     *
     * @param personality Personality traits affecting idle behavior
     */
    public IdleBehaviorController(PersonalityTraits personality) {
        this.personality = personality;
        this.random = new Random();
        this.enabled = true;
        this.cooldownTicks = 0;
    }

    /**
     * Sets the executor callback for idle actions.
     *
     * @param executor Callback to execute idle actions
     */
    public void setExecutor(IdleActionExecutor executor) {
        this.executor = executor;
    }

    /**
     * Enables or disables idle behaviors.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.debug("Idle behaviors {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Checks if idle behaviors are enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Updates the controller (should be called each tick).
     *
     * <p>This decrements cooldown and may trigger an idle action.</p>
     */
    public void tick() {
        if (!enabled) {
            return;
        }

        // Decrement cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        // Check if should perform idle action
        double idleChance = getIdleActionChance();
        if (random.nextDouble() < idleChance) {
            IdleAction action = selectAction();
            performAction(action);
            setNextCooldown();
        }
    }

    /**
     * Updates the controller with custom executor (convenience method).
     *
     * @param executor Callback to execute idle actions
     */
    public void tick(IdleActionExecutor executor) {
        setExecutor(executor);
        tick();
    }

    /**
     * Gets the probability of performing an idle action based on personality.
     *
     * <p>Personality affects idle action chance:</p>
     * <ul>
     *   <li>High extraversion: +50% more actions</li>
     *   <li>High neuroticism: +100% more fidgeting</li>
     *   <li>High openness: +30% more varied actions</li>
     *   <li>High conscientiousness: -50% fewer actions</li>
     * </ul>
     *
     * @return Probability of idle action per tick (0.0 to 1.0)
     */
    private double getIdleActionChance() {
        double baseChance = DEFAULT_IDLE_CHANCE;

        // Personality modifiers
        double extraversion = personality.getExtraversion() / 100.0; // 0.0 to 1.0
        double neuroticism = personality.getNeuroticism() / 100.0;
        double openness = personality.getOpenness() / 100.0;
        double conscientiousness = personality.getConscientiousness() / 100.0;

        // Apply modifiers
        baseChance *= (1.0 + extraversion * 0.5); // Extraverts act more
        baseChance *= (1.0 + neuroticism * 1.0); // Anxious types fidget more
        baseChance *= (1.0 + openness * 0.3); // Open to varied actions
        baseChance *= (1.0 - conscientiousness * 0.5); // Focused types act less

        // Clamp to reasonable range
        return Math.max(0.001, Math.min(0.10, baseChance));
    }

    /**
     * Selects an idle action based on personality.
     *
     * @return Selected idle action
     */
    private IdleAction selectAction() {
        double extraversion = personality.getExtraversion() / 100.0;
        double neuroticism = personality.getNeuroticism() / 100.0;
        double openness = personality.getOpenness() / 100.0;
        double conscientiousness = personality.getConscientiousness() / 100.0;

        double roll = random.nextDouble();

        // Personality affects action distribution
        if (neuroticism > 0.7 && roll < 0.4) {
            // Anxious personalities fidget more (40% chance)
            return IdleAction.FIDGET;
        } else if (extraversion > 0.7 && roll < 0.5) {
            // Extraverts look around more (50% chance)
            return IdleAction.LOOK_AROUND;
        } else if (openness > 0.7 && roll < 0.3) {
            // Open personalities try varied actions (30% chance)
            return getRandomVariedAction();
        } else if (conscientiousness > 0.7 && roll < 0.6) {
            // Focused personalities mostly stand still (60% chance)
            return IdleAction.STAND_STILL;
        } else {
            // Default distribution
            return getDefaultAction();
        }
    }

    /**
     * Gets a default idle action from standard distribution.
     */
    private IdleAction getDefaultAction() {
        double roll = random.nextDouble();

        if (roll < 0.30) {
            return IdleAction.LOOK_AROUND; // 30%
        } else if (roll < 0.55) {
            return IdleAction.FIDGET; // 25%
        } else if (roll < 0.75) {
            return IdleAction.STAND_STILL; // 20%
        } else if (roll < 0.85) {
            return IdleAction.SMALL_STEP; // 10%
        } else if (roll < 0.95) {
            return IdleAction.CHECK_INVENTORY; // 10%
        } else {
            return IdleAction.EMOTE; // 5%
        }
    }

    /**
     * Gets a varied idle action for open personalities.
     */
    private IdleAction getRandomVariedAction() {
        IdleAction[] variedActions = {
            IdleAction.STRETCH,
            IdleAction.EMOTE,
            IdleAction.SMALL_STEP,
            IdleAction.CHECK_INVENTORY
        };
        return variedActions[random.nextInt(variedActions.length)];
    }

    /**
     * Performs the selected idle action.
     *
     * @param action Action to perform
     */
    private void performAction(IdleAction action) {
        if (executor == null) {
            LOGGER.warn("No executor set for idle action: {}", action);
            return;
        }

        double[] data = generateActionData(action);
        executor.execute(action, data);

        LOGGER.debug("Performed idle action: {}", action);
    }

    /**
     * Generates data for an idle action (e.g., rotation amounts, offsets).
     *
     * @param action Action to generate data for
     * @return Array of action-specific data
     */
    private double[] generateActionData(IdleAction action) {
        return switch (action) {
            case LOOK_AROUND -> new double[] {
                random.nextGaussian() * 30, // Yaw rotation (degrees)
                random.nextGaussian() * 15  // Pitch rotation (degrees)
            };
            case FIDGET -> new double[] {
                random.nextGaussian() * 0.2, // X offset
                0.0,                         // Y offset
                random.nextGaussian() * 0.2  // Z offset
            };
            case SMALL_STEP -> new double[] {
                random.nextGaussian() * 0.5, // X offset
                0.0,                         // Y offset
                random.nextGaussian() * 0.5  // Z offset
            };
            case STRETCH, CHECK_INVENTORY, EMOTE, STAND_STILL -> new double[0];
        };
    }

    /**
     * Sets cooldown until next idle action.
     */
    private void setNextCooldown() {
        // Base cooldown with personality adjustment
        double conscientiousness = personality.getConscientiousness() / 100.0;
        double cooldownMultiplier = 1.0 - (conscientiousness * 0.3); // Focused = longer cooldown

        int minCooldown = (int) (MIN_COOLDOWN_TICKS * cooldownMultiplier);
        int maxCooldown = (int) (MAX_COOLDOWN_TICKS * cooldownMultiplier);

        cooldownTicks = minCooldown + random.nextInt(maxCooldown - minCooldown);

        LOGGER.debug("Set idle cooldown: {} ticks", cooldownTicks);
    }

    /**
     * Forces an idle action to occur immediately (bypassing cooldown).
     *
     * <p>Useful for triggering idle actions in response to events.</p>
     */
    public void forceIdleAction() {
        if (!enabled) {
            return;
        }

        IdleAction action = selectAction();
        performAction(action);
        setNextCooldown();
    }

    /**
     * Resets cooldown to zero, allowing next idle check immediately.
     */
    public void resetCooldown() {
        cooldownTicks = 0;
    }

    /**
     * Gets remaining cooldown ticks.
     *
     * @return Ticks until next idle action can occur
     */
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /**
     * Sets the seed for the random number generator.
     *
     * <p>Useful for testing to get reproducible results.</p>
     *
     * @param seed Seed value for random number generation
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
        LOGGER.debug("Random seed set to: {}", seed);
    }

    /**
     * Default personality implementation with average traits.
     */
    private static class DefaultPersonality implements PersonalityTraits {
        @Override
        public int getExtraversion() {
            return 50;
        }

        @Override
        public int getNeuroticism() {
            return 50;
        }

        @Override
        public int getOpenness() {
            return 50;
        }

        @Override
        public int getConscientiousness() {
            return 50;
        }
    }
}
