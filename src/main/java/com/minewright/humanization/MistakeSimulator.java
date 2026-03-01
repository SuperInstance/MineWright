package com.minewright.humanization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Simulates human-like mistakes in AI agent behavior.
 *
 * <p>Humans are not perfectly consistent - they make mistakes at predictable rates.
 * This class simulates various types of mistakes to increase agent believability.</p>
 *
 * <h2>Mistake Categories</h2>
 * <ul>
 *   <li><b>WRONG_TARGET</b> - Selecting incorrect block/entity (2-5% rate)</li>
 *   <li><b>TIMING_ERROR</b> - Acting too early or late (5-10% rate)</li>
 *   <li><b>MOVEMENT_ERROR</b> - Slight navigation errors (3-7% rate)</li>
 *   <li><b>SELECTION_ERROR</b> - Choosing wrong tool/item (3-5% rate)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MistakeSimulator simulator = new MistakeSimulator(0.03); // 3% mistake rate
 *
 * // Check if mistake should occur
 * if (simulator.shouldMakeMistake()) {
 *     // Simulate targeting wrong block
 *     BlockPos mistaken = simulator.getWrongTarget(intendedPosition);
 * }
 *
 * // Get mistaken delay (occasional "spacing out")
 * int delay = simulator.getMistakenDelay(100); // May return extra delay
 *
 * // Get mistaken tool selection
 * Tool tool = simulator.getWrongTool(correctTool, availableTools);
 * }</pre>
 *
 * @see HumanizationUtils
 * @see IdleBehaviorController
 * @see SessionManager
 * @since 2.2.0
 */
public class MistakeSimulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MistakeSimulator.class);

    /**
     * Base mistake rate for average agent (3%).
     */
    private static final double BASE_MISTAKE_RATE = 0.03;

    /**
     * Random number generator for mistake simulation.
     */
    private final Random random;

    /**
     * Current mistake rate (can be adjusted by fatigue, complexity, etc.).
     */
    private double mistakeRate;

    /**
     * Types of mistakes that can be simulated.
     */
    public enum MistakeType {
        /**
         * Targeting wrong block or entity.
         */
        WRONG_TARGET,

        /**
         * Acting at wrong time (too early/late).
         */
        TIMING_ERROR,

        /**
         * Movement navigation errors (overshoot, wrong direction).
         */
        MOVEMENT_ERROR,

        /**
         * Selecting wrong tool or item.
         */
        SELECTION_ERROR
    }

    /**
     * Creates a mistake simulator with default mistake rate (3%).
     */
    public MistakeSimulator() {
        this(BASE_MISTAKE_RATE);
    }

    /**
     * Creates a mistake simulator with specified mistake rate.
     *
     * @param mistakeRate Probability of mistake (0.0 to 1.0)
     * @throws IllegalArgumentException if mistakeRate is not in [0, 1]
     */
    public MistakeSimulator(double mistakeRate) {
        if (mistakeRate < 0.0 || mistakeRate > 1.0) {
            throw new IllegalArgumentException(
                "Mistake rate must be in range [0, 1], got: " + mistakeRate);
        }
        this.mistakeRate = mistakeRate;
        this.random = new Random();
    }

    /**
     * Checks if a mistake should occur based on current mistake rate.
     *
     * @return true if a mistake should occur
     */
    public boolean shouldMakeMistake() {
        return random.nextDouble() < mistakeRate;
    }

    /**
     * Checks if a mistake of a specific type should occur.
     *
     * <p>Different mistake types have different base probabilities:</p>
     * <ul>
     *   <li>WRONG_TARGET: 80% of base rate</li>
     *   <li>TIMING_ERROR: 100% of base rate</li>
     *   <li>MOVEMENT_ERROR: 90% of base rate</li>
     *   <li>SELECTION_ERROR: 70% of base rate</li>
     * </ul>
     *
     * @param type Type of mistake to check
     * @return true if a mistake of this type should occur
     */
    public boolean shouldMakeMistake(MistakeType type) {
        double typeMultiplier = switch (type) {
            case WRONG_TARGET -> 0.8;
            case TIMING_ERROR -> 1.0;
            case MOVEMENT_ERROR -> 0.9;
            case SELECTION_ERROR -> 0.7;
        };
        return random.nextDouble() < (mistakeRate * typeMultiplier);
    }

    /**
     * Simulates a wrong target selection mistake.
     *
     * <p>Returns a position adjacent to the intended target, simulating
     * clicking on the wrong block.</p>
     *
     * @param intendedX Intended X coordinate
     * @param intendedY Intended Y coordinate
     * @param intendedZ Intended Z coordinate
     * @return Array [x, y, z] of mistaken target (adjacent block)
     */
    public int[] getWrongTarget(int intendedX, int intendedY, int intendedZ) {
        // Randomly choose a direction (6 possible directions)
        int direction = random.nextInt(6);

        int mistakenX = intendedX;
        int mistakenY = intendedY;
        int mistakenZ = intendedZ;

        // Offset in random direction
        switch (direction) {
            case 0 -> mistakenX += 1;  // East
            case 1 -> mistakenX -= 1;  // West
            case 2 -> mistakenY += 1;  // Up
            case 3 -> mistakenY -= 1;  // Down
            case 4 -> mistakenZ += 1;  // South
            case 5 -> mistakenZ -= 1;  // North
        }

        LOGGER.debug("Targeting mistake: intended ({}, {}, {}) -> mistaken ({}, {}, {})",
            intendedX, intendedY, intendedZ, mistakenX, mistakenY, mistakenZ);

        return new int[] {mistakenX, mistakenY, mistakenZ};
    }

    /**
     * Simulates a timing mistake by adding extra delay.
     *
     * <p>Represents "spacing out" or acting prematurely.</p>
     *
     * @param intendedDelayMs Intended delay in milliseconds
     * @return Delay with mistake applied (may be longer or shorter)
     */
    public int getMistakenDelay(int intendedDelayMs) {
        // 50% chance of extra delay (spacing out), 50% chance of acting too early
        if (random.nextBoolean()) {
            // Add 10-100ms extra delay
            int extraDelay = 10 + random.nextInt(90);
            LOGGER.debug("Timing mistake: added {}ms delay (intended: {}ms)", extraDelay, intendedDelayMs);
            return intendedDelayMs + extraDelay;
        } else {
            // Reduce delay by 10-30% (acting too early)
            double reductionFactor = 0.7 + (random.nextDouble() * 0.2); // 0.7-0.9
            int reducedDelay = (int) (intendedDelayMs * reductionFactor);
            LOGGER.debug("Timing mistake: reduced delay to {}ms (intended: {}ms)",
                reducedDelay, intendedDelayMs);
            return Math.max(10, reducedDelay);
        }
    }

    /**
     * Simulates a movement mistake by adding offset to target position.
     *
     * <p>Represents slight overshoot or misalignment when moving.</p>
     *
     * @param intendedX Intended X position
     * @param intendedY Intended Y position
     * @param intendedZ Intended Z position
     * @param magnitude Maximum offset magnitude (default 0.5 blocks)
     * @return Array [x, y, z] with small random offset applied
     */
    public double[] getMovementError(double intendedX, double intendedY, double intendedZ, double magnitude) {
        double offsetX = random.nextGaussian() * magnitude;
        double offsetY = random.nextGaussian() * magnitude;
        double offsetZ = random.nextGaussian() * magnitude;

        // Clamp offset magnitude
        offsetX = Math.max(-magnitude, Math.min(magnitude, offsetX));
        offsetY = Math.max(-magnitude, Math.min(magnitude, offsetY));
        offsetZ = Math.max(-magnitude, Math.min(magnitude, offsetZ));

        LOGGER.debug("Movement mistake: offset ({}, {}, {}) from intended ({}, {}, {})",
            offsetX, offsetY, offsetZ, intendedX, intendedY, intendedZ);

        return new double[] {
            intendedX + offsetX,
            intendedY + offsetY,
            intendedZ + offsetZ
        };
    }

    /**
     * Simulates a selection mistake by returning a wrong index.
     *
     * <p>Represents choosing the wrong tool or item from inventory.</p>
     *
     * @param correctIndex The correct index to select
     * @param maxIndex Maximum valid index
     * @return Wrong index (adjacent to correct index)
     */
    public int getWrongSelection(int correctIndex, int maxIndex) {
        if (maxIndex <= 0) {
            return 0;
        }

        // Choose adjacent index (if available)
        int wrongIndex;
        if (correctIndex > 0 && random.nextBoolean()) {
            // Offset to previous
            wrongIndex = correctIndex - 1;
        } else if (correctIndex < maxIndex) {
            // Offset to next
            wrongIndex = correctIndex + 1;
        } else if (correctIndex > 0) {
            // Can only go back
            wrongIndex = correctIndex - 1;
        } else {
            // Can only go forward
            wrongIndex = Math.min(1, maxIndex);
        }

        LOGGER.debug("Selection mistake: chose index {} instead of {}", wrongIndex, correctIndex);
        return wrongIndex;
    }

    /**
     * Possibly returns a mistaken value instead of the correct one.
     *
     * <p>This is a convenience method that combines mistake checking
     * with mistake generation.</p>
     *
     * @param correctValue The correct value
     * @param mistakeSupplier Function to generate mistaken value
     * @param <T> Type of value
     * @return Either correct value or mistaken value
     */
    public <T> T maybeIntroduceMistake(T correctValue, MistakeSupplier<T> mistakeSupplier) {
        if (shouldMakeMistake()) {
            T mistakenValue = mistakeSupplier.getMistake(correctValue);
            LOGGER.debug("Mistake introduced: {} -> {}", correctValue, mistakenValue);
            return mistakenValue;
        }
        return correctValue;
    }

    /**
     * Adjusts mistake rate based on context.
     *
     * <p>Mistake rates are affected by:</p>
     * <ul>
     *   <li>Fatigue: +50% mistake rate</li>
     *   <li>Complexity: +30% mistake rate</li>
     *   <li>Stress: +20% mistake rate</li>
     * </ul>
     *
     * @param fatigueLevel 0.0 to 1.0 (rested to exhausted)
     * @param complexity 0.0 to 1.0 (simple to complex)
     * @param stressLevel 0.0 to 1.0 (calm to stressed)
     */
    public void adjustMistakeRate(double fatigueLevel, double complexity, double stressLevel) {
        // Clamp inputs
        fatigueLevel = clamp(fatigueLevel, 0.0, 1.0);
        complexity = clamp(complexity, 0.0, 1.0);
        stressLevel = clamp(stressLevel, 0.0, 1.0);

        // Calculate adjustment
        double fatigueMultiplier = 1.0 + (fatigueLevel * 0.5);
        double complexityMultiplier = 1.0 + (complexity * 0.3);
        double stressMultiplier = 1.0 + (stressLevel * 0.2);

        double newRate = BASE_MISTAKE_RATE * fatigueMultiplier * complexityMultiplier * stressMultiplier;

        // Clamp to reasonable range
        this.mistakeRate = clamp(newRate, 0.0, 0.20); // Max 20% mistake rate

        LOGGER.debug("Mistake rate adjusted: {:.3f} (fatigue: {:.2f}, complexity: {:.2f}, stress: {:.2f})",
            this.mistakeRate, fatigueLevel, complexity, stressLevel);
    }

    /**
     * Resets mistake rate to base value.
     */
    public void resetMistakeRate() {
        this.mistakeRate = BASE_MISTAKE_RATE;
        LOGGER.debug("Mistake rate reset to base value: {:.3f}", BASE_MISTAKE_RATE);
    }

    /**
     * Gets current mistake rate.
     *
     * @return Current mistake probability (0.0 to 1.0)
     */
    public double getMistakeRate() {
        return mistakeRate;
    }

    /**
     * Sets a custom mistake rate.
     *
     * @param mistakeRate New mistake rate (0.0 to 1.0)
     * @throws IllegalArgumentException if mistakeRate is not in [0, 1]
     */
    public void setMistakeRate(double mistakeRate) {
        if (mistakeRate < 0.0 || mistakeRate > 1.0) {
            throw new IllegalArgumentException(
                "Mistake rate must be in range [0, 1], got: " + mistakeRate);
        }
        this.mistakeRate = mistakeRate;
        LOGGER.debug("Mistake rate set to: {:.3f}", mistakeRate);
    }

    /**
     * Functional interface for generating mistaken values.
     *
     * @param <T> Type of value to generate
     */
    @FunctionalInterface
    public interface MistakeSupplier<T> {
        /**
         * Generates a mistaken value based on the correct value.
         *
         * @param correctValue The correct value
         * @return A mistaken value
         */
        T getMistake(T correctValue);
    }

    /**
     * Clamps a value between minimum and maximum.
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
}
