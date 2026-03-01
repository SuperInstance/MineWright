package com.minewright.humanization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.List;

/**
 * Utility class for adding human-like variation to AI agent behavior.
 *
 * <p>This class provides methods for:
 * <ul>
 *   <li>Gaussian jitter for timing randomization</li>
 *   <li>Random delays with uniform distribution</li>
 *   <li>Bezier curve interpolation for smooth movement</li>
 *   <li>Probabilistic mistake triggering</li>
 *   <li>Human-like reaction time generation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Add Gaussian jitter to action delay
 * int baseDelay = 100; // ms
 * int jitteredDelay = HumanizationUtils.gaussianJitter(baseDelay, 0.3);
 *
 * // Generate human reaction time
 * int reactionTime = HumanizationUtils.humanReactionTime(); // 200-400ms
 *
 * // Check if agent should make a mistake
 * if (HumanizationUtils.shouldMakeMistake(0.03)) {
 *     // Simulate mistake
 * }
 *
 * // Get point along Bezier curve for smooth movement
 * Vec3 point = HumanizationUtils.bezierPoint(0.5, controlPoints);
 * }</pre>
 *
 * @see MistakeSimulator
 * @see IdleBehaviorController
 * @see SessionManager
 * @since 2.2.0
 */
public final class HumanizationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HumanizationUtils.class);

    /**
     * Random number generator for humanization calculations.
     * Uses a fixed seed for reproducibility in testing.
     */
    private static final Random RANDOM = new Random();

    /**
     * Minimum action delay in milliseconds to prevent unrealistically fast actions.
     * Human reaction time limit for simple tasks.
     */
    private static final int MIN_ACTION_DELAY_MS = 30;

    /**
     * Maximum action delay in milliseconds before actions become noticeably slow.
     */
    private static final int MAX_ACTION_DELAY_MS = 1000;

    /**
     * Mean human reaction time in milliseconds for visual stimulus.
     */
    private static final double MEAN_REACTION_TIME_MS = 300.0;

    /**
     * Standard deviation for human reaction time in milliseconds.
     */
    private static final double REACTION_TIME_STD_DEV_MS = 50.0;

    /**
     * Minimum realistic human reaction time in milliseconds.
     */
    private static final int MIN_REACTION_TIME_MS = 150;

    /**
     * Maximum realistic human reaction time in milliseconds.
     */
    private static final int MAX_REACTION_TIME_MS = 600;

    // Private constructor to prevent instantiation
    private HumanizationUtils() {
        throw new AssertionError("HumanizationUtils is a utility class and cannot be instantiated");
    }

    /**
     * Adds Gaussian (normal) distribution noise to a base value.
     *
     * <p>This method adds random jitter following a normal distribution,
     * which creates more natural variation than uniform random.</p>
     *
     * <p>Approximately 68% of values will fall within baseMs ± variancePercent,
     * and 95% within baseMs ± 2*variancePercent.</p>
     *
     * @param baseMs The base value in milliseconds
     * @param variancePercent Variance as a fraction of base value (e.g., 0.3 for ±30%)
     * @return Jittered value in milliseconds, clamped to realistic range
     *
     * @see #randomDelay(int, int)
     * @see #humanReactionTime()
     */
    public static int gaussianJitter(int baseMs, double variancePercent) {
        if (baseMs <= 0) {
            LOGGER.warn("Base delay must be positive, got: {}. Using default 100ms", baseMs);
            baseMs = 100;
        }
        if (variancePercent < 0 || variancePercent > 1.0) {
            LOGGER.warn("Variance percent out of range: {}. Clamping to [0, 1.0]", variancePercent);
            variancePercent = Math.max(0, Math.min(1.0, variancePercent));
        }

        // Calculate standard deviation as fraction of base value
        double stdDev = baseMs * variancePercent;

        // Generate Gaussian noise
        double jitter = RANDOM.nextGaussian() * stdDev;

        // Apply jitter and clamp to realistic range
        int jittered = (int) (baseMs + jitter);
        return Math.max(MIN_ACTION_DELAY_MS, Math.min(MAX_ACTION_DELAY_MS, jittered));
    }

    /**
     * Generates a random delay with uniform distribution between min and max.
     *
     * <p>Unlike {@link #gaussianJitter(int, double)}, this uses uniform distribution
     * which may be preferable for certain types of randomization.</p>
     *
     * @param minMs Minimum delay in milliseconds (inclusive)
     * @param maxMs Maximum delay in milliseconds (exclusive)
     * @return Random delay between min and max
     * @throws IllegalArgumentException if minMs >= maxMs or minMs < 0
     *
     * @see #gaussianJitter(int, double)
     */
    public static int randomDelay(int minMs, int maxMs) {
        if (minMs < 0) {
            throw new IllegalArgumentException("Minimum delay must be non-negative: " + minMs);
        }
        if (minMs >= maxMs) {
            throw new IllegalArgumentException(
                String.format("Minimum delay (%d) must be less than maximum (%d)", minMs, maxMs));
        }

        return minMs + RANDOM.nextInt(maxMs - minMs);
    }

    /**
     * Calculates a point along a quadratic Bezier curve.
     *
     * <p>Bezier curves create smooth, natural-looking movement paths.
     * A quadratic Bezier curve is defined by three points:
     * <ul>
     *   <li>P0: Start point</li>
     *   <li>P1: Control point (defines curvature)</li>
     *   <li>P2: End point</li>
     * </ul>
     *
     * <p>The formula is: B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2</p>
     *
     * @param t Progress along curve (0.0 to 1.0)
     * @param controlPoints List of 3 control points [P0, P1, P2]
     * @return Interpolated position as double array [x, y, z]
     * @throws IllegalArgumentException if t is not in [0, 1] or controlPoints size != 3
     *
     * @since 2.2.0
     */
    public static double[] bezierPoint(double t, List<double[]> controlPoints) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Parameter t must be in range [0, 1], got: " + t);
        }
        if (controlPoints == null || controlPoints.size() != 3) {
            throw new IllegalArgumentException(
                "Quadratic Bezier requires exactly 3 control points, got: " +
                (controlPoints == null ? "null" : controlPoints.size()));
        }

        double[] p0 = controlPoints.get(0);
        double[] p1 = controlPoints.get(1);
        double[] p2 = controlPoints.get(2);

        if (p0.length != 3 || p1.length != 3 || p2.length != 3) {
            throw new IllegalArgumentException("All control points must be 3D coordinates [x, y, z]");
        }

        // Quadratic Bezier formula: B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
        double x = Math.pow(1 - t, 2) * p0[0] + 2 * (1 - t) * t * p1[0] + Math.pow(t, 2) * p2[0];
        double y = Math.pow(1 - t, 2) * p0[1] + 2 * (1 - t) * t * p1[1] + Math.pow(t, 2) * p2[1];
        double z = Math.pow(1 - t, 2) * p0[2] + 2 * (1 - t) * t * p1[2] + Math.pow(t, 2) * p2[2];

        return new double[] {x, y, z};
    }

    /**
     * Calculates a point along a cubic Bezier curve (4 control points).
     *
     * <p>Cubic Bezier curves provide more control over curvature than quadratic.</p>
     *
     * <p>The formula is: B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3</p>
     *
     * @param t Progress along curve (0.0 to 1.0)
     * @param controlPoints List of 4 control points [P0, P1, P2, P3]
     * @return Interpolated position as double array [x, y, z]
     * @throws IllegalArgumentException if t is not in [0, 1] or controlPoints size != 4
     *
     * @since 2.2.0
     */
    public static double[] cubicBezierPoint(double t, List<double[]> controlPoints) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Parameter t must be in range [0, 1], got: " + t);
        }
        if (controlPoints == null || controlPoints.size() != 4) {
            throw new IllegalArgumentException(
                "Cubic Bezier requires exactly 4 control points, got: " +
                (controlPoints == null ? "null" : controlPoints.size()));
        }

        double[] p0 = controlPoints.get(0);
        double[] p1 = controlPoints.get(1);
        double[] p2 = controlPoints.get(2);
        double[] p3 = controlPoints.get(3);

        if (p0.length != 3 || p1.length != 3 || p2.length != 3 || p3.length != 3) {
            throw new IllegalArgumentException("All control points must be 3D coordinates [x, y, z]");
        }

        // Cubic Bezier formula: B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3
        double x = Math.pow(1 - t, 3) * p0[0]
                 + 3 * Math.pow(1 - t, 2) * t * p1[0]
                 + 3 * (1 - t) * Math.pow(t, 2) * p2[0]
                 + Math.pow(t, 3) * p3[0];
        double y = Math.pow(1 - t, 3) * p0[1]
                 + 3 * Math.pow(1 - t, 2) * t * p1[1]
                 + 3 * (1 - t) * Math.pow(t, 2) * p2[1]
                 + Math.pow(t, 3) * p3[1];
        double z = Math.pow(1 - t, 3) * p0[2]
                 + 3 * Math.pow(1 - t, 2) * t * p1[2]
                 + 3 * (1 - t) * Math.pow(t, 2) * p2[2]
                 + Math.pow(t, 3) * p3[2];

        return new double[] {x, y, z};
    }

    /**
     * Determines if an agent should make a mistake based on probability.
     *
     * <p>This uses a simple probability check - mistakes occur with the given rate.</p>
     *
     * <p>Example mistake rates:</p>
     * <ul>
     *   <li>0.01 (1%) - Expert level</li>
     *   <li>0.03 (3%) - Average/AI agent (recommended)</li>
     *   <li>0.05 (5%) - Distracted</li>
     *   <li>0.10 (10%) - Beginner/Fatigued</li>
     * </ul>
     *
     * @param mistakeRate Probability of mistake (0.0 to 1.0)
     * @return true if a mistake should occur
     * @throws IllegalArgumentException if mistakeRate is not in [0, 1]
     *
     * @see MistakeSimulator
     */
    public static boolean shouldMakeMistake(double mistakeRate) {
        if (mistakeRate < 0.0 || mistakeRate > 1.0) {
            throw new IllegalArgumentException(
                "Mistake rate must be in range [0, 1], got: " + mistakeRate);
        }
        return RANDOM.nextDouble() < mistakeRate;
    }

    /**
     * Generates a human-like reaction time using Gaussian distribution.
     *
     * <p>Reaction times follow a normal distribution with mean ~300ms and
     * standard deviation ~50ms for visual stimuli in healthy adults.</p>
     *
     * <p>The generated time is clamped to a realistic range [150ms, 600ms]
     * to prevent unrealistically fast or slow reactions.</p>
     *
     * <p>Typical human reaction times:</p>
     * <ul>
     *   <li>Visual stimulus: 250-500ms</li>
     *   <li>Auditory stimulus: 170-350ms</li>
     *   <li>Choice reaction: 400-800ms</li>
     *   <li>Minecraft actions: 200-400ms (experienced players)</li>
     * </ul>
     *
     * @return Reaction time in milliseconds (150-600ms range)
     *
     * @see #gaussianJitter(int, double)
     */
    public static int humanReactionTime() {
        // Generate reaction time using Gaussian distribution
        double reactionMs = RANDOM.nextGaussian() * REACTION_TIME_STD_DEV_MS + MEAN_REACTION_TIME_MS;

        // Clamp to realistic range
        return (int) Math.max(MIN_REACTION_TIME_MS, Math.min(MAX_REACTION_TIME_MS, reactionMs));
    }

    /**
     * Generates a reaction time adjusted for context (fatigue, complexity, etc.).
     *
     * <p>Reaction times are affected by:</p>
     * <ul>
     *   <li>Fatigue: +20-50% slower</li>
     *   <li>Complexity: +30-100% slower</li>
     *   <li>Familiarity: -20-30% faster</li>
     *   <li>Stress: ±20% variance</li>
     * </ul>
     *
     * @param fatigueLevel 0.0 to 1.0 (rested to exhausted)
     * @param complexity 0.0 to 1.0 (simple to complex)
     * @param familiarity 0.0 to 1.0 (unfamiliar to familiar)
     * @return Reaction time in milliseconds, adjusted for context
     *
     * @since 2.2.0
     */
    public static int contextualReactionTime(double fatigueLevel, double complexity, double familiarity) {
        // Validate inputs
        fatigueLevel = clamp(fatigueLevel, 0.0, 1.0);
        complexity = clamp(complexity, 0.0, 1.0);
        familiarity = clamp(familiarity, 0.0, 1.0);

        // Start with base reaction time
        double reactionMs = humanReactionTime();

        // Apply fatigue modifier (+20-50% slower)
        double fatigueMultiplier = 1.0 + (fatigueLevel * 0.5);
        reactionMs *= fatigueMultiplier;

        // Apply complexity modifier (+30-100% slower)
        double complexityMultiplier = 1.0 + (complexity * 1.0);
        reactionMs *= complexityMultiplier;

        // Apply familiarity modifier (-20-30% faster)
        double familiarityMultiplier = 1.0 - (familiarity * 0.3);
        reactionMs *= familiarityMultiplier;

        // Clamp to realistic range
        return (int) Math.max(MIN_REACTION_TIME_MS, Math.min(MAX_REACTION_TIME_MS * 2, reactionMs));
    }

    /**
     * Generates a random offset for micro-movements.
     *
     * <p>Humans make small, seemingly unnecessary movements that add realism.
     * This generates a small random offset suitable for micro-adjustments.</p>
     *
     * @param magnitude Maximum offset magnitude
     * @return Random offset with Gaussian distribution
     *
     * @since 2.2.0
     */
    public static double microMovementOffset(double magnitude) {
        double offset = RANDOM.nextGaussian() * magnitude;
        return Math.max(-magnitude, Math.min(magnitude, offset));
    }

    /**
     * Clamps a value between minimum and maximum.
     *
     * @param value The value to clamp
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Clamped value
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Sets the seed for the random number generator.
     *
     * <p>This is primarily useful for testing to get reproducible results.</p>
     *
     * @param seed Seed value for random number generation
     *
     * @since 2.2.0
     */
    public static void setSeed(long seed) {
        RANDOM.setSeed(seed);
        LOGGER.debug("Random seed set to: {}", seed);
    }
}
