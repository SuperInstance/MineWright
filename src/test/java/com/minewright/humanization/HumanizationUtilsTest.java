package com.minewright.humanization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for humanization utilities.
 *
 * <p>These tests verify that humanization utilities produce correct distributions,
 * handle edge cases properly, and maintain statistical properties.</p>
 *
 * @since 2.2.0
 */
@DisplayName("Humanization Utilities Tests")
class HumanizationUtilsTest {

    private static final double EPSILON = 0.01;
    private static final int SAMPLE_SIZE = 1000;

    @BeforeEach
    void setUp() {
        // Set seed for reproducible tests
        HumanizationUtils.setSeed(42L);
    }

    // ========================================================================
    // Gaussian Jitter Tests
    // ========================================================================

    @Test
    @DisplayName("Gaussian jitter should be within valid range")
    void testGaussianJitterRange() {
        int baseMs = 100;
        double variance = 0.3;

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            int jittered = HumanizationUtils.gaussianJitter(baseMs, variance);
            assertTrue(jittered >= 30 && jittered <= 1000,
                "Jittered value should be in range [30, 1000], got: " + jittered);
        }
    }

    @Test
    @DisplayName("Gaussian jitter should have correct variance")
    void testGaussianJitterVariance() {
        int baseMs = 100;
        double variancePercent = 0.3;
        double expectedStdDev = baseMs * variancePercent;

        List<Integer> samples = new ArrayList<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            samples.add(HumanizationUtils.gaussianJitter(baseMs, variancePercent));
        }

        // Calculate sample standard deviation
        double mean = samples.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = samples.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0);
        double sampleStdDev = Math.sqrt(variance);

        // Sample std dev should be close to expected (within 20%)
        assertEquals(expectedStdDev, sampleStdDev, expectedStdDev * 0.2);
    }

    @Test
    @DisplayName("Gaussian jitter should handle zero variance")
    void testGaussianJitterZeroVariance() {
        int baseMs = 100;
        double variance = 0.0;

        // With zero variance, values should be close to base (due to clamping)
        int result = HumanizationUtils.gaussianJitter(baseMs, variance);
        // May vary slightly due to clamping but should be close to base
        assertTrue(Math.abs(result - baseMs) < 50,
            "With zero variance, result should be close to base");
    }

    @Test
    @DisplayName("Gaussian jitter should handle invalid base value")
    void testGaussianJitterInvalidBase() {
        int result = HumanizationUtils.gaussianJitter(-10, 0.3);
        // Should default to 100ms base
        assertTrue(result >= 30 && result <= 1000);
    }

    // ========================================================================
    // Random Delay Tests
    // ========================================================================

    @Test
    @DisplayName("Random delay should be within range")
    void testRandomDelayRange() {
        int minMs = 50;
        int maxMs = 150;

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            int delay = HumanizationUtils.randomDelay(minMs, maxMs);
            assertTrue(delay >= minMs && delay < maxMs,
                "Delay should be in range [" + minMs + ", " + maxMs + "), got: " + delay);
        }
    }

    @Test
    @DisplayName("Random delay should have uniform distribution")
    void testRandomDelayDistribution() {
        int minMs = 0;
        int maxMs = 100;
        int bins = 10;
        int[] histogram = new int[bins];

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            int delay = HumanizationUtils.randomDelay(minMs, maxMs);
            int bin = delay / (maxMs / bins);
            if (bin >= bins) bin = bins - 1;
            histogram[bin]++;
        }

        // Each bin should have approximately equal samples
        int expectedPerBin = SAMPLE_SIZE / bins;
        for (int i = 0; i < bins; i++) {
            // Allow 30% deviation from expected
            assertTrue(histogram[i] >= expectedPerBin * 0.7 && histogram[i] <= expectedPerBin * 1.3,
                "Bin " + i + " has " + histogram[i] + " samples, expected ~" + expectedPerBin);
        }
    }

    @Test
    @DisplayName("Random delay should throw on invalid range")
    void testRandomDelayInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.randomDelay(100, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.randomDelay(-10, 50);
        });
    }

    // ========================================================================
    // Bezier Curve Tests
    // ========================================================================

    @Test
    @DisplayName("Quadratic Bezier should interpolate correctly")
    void testQuadraticBezierInterpolation() {
        List<double[]> controlPoints = Arrays.asList(
            new double[]{0, 0, 0},   // P0: Start
            new double[]{5, 5, 0},   // P1: Control
            new double[]{10, 0, 0}   // P2: End
        );

        // Test start point (t=0)
        double[] start = HumanizationUtils.bezierPoint(0.0, controlPoints);
        assertArrayEquals(new double[]{0, 0, 0}, start, EPSILON);

        // Test end point (t=1)
        double[] end = HumanizationUtils.bezierPoint(1.0, controlPoints);
        assertArrayEquals(new double[]{10, 0, 0}, end, EPSILON);

        // Test midpoint (t=0.5)
        double[] mid = HumanizationUtils.bezierPoint(0.5, controlPoints);
        // At t=0.5: B(0.5) = 0.25*P0 + 0.5*P1 + 0.25*P2
        // x = 0.25*0 + 0.5*5 + 0.25*10 = 2.5 + 2.5 = 5
        // y = 0.25*0 + 0.5*5 + 0.25*0 = 2.5
        assertEquals(5.0, mid[0], EPSILON);
        assertEquals(2.5, mid[1], EPSILON);
        assertEquals(0.0, mid[2], EPSILON);
    }

    @Test
    @DisplayName("Cubic Bezier should interpolate correctly")
    void testCubicBezierInterpolation() {
        List<double[]> controlPoints = Arrays.asList(
            new double[]{0, 0, 0},    // P0: Start
            new double[]{2.5, 10, 0}, // P1: Control 1
            new double[]{7.5, 10, 0}, // P2: Control 2
            new double[]{10, 0, 0}    // P3: End
        );

        // Test start point (t=0)
        double[] start = HumanizationUtils.cubicBezierPoint(0.0, controlPoints);
        assertArrayEquals(new double[]{0, 0, 0}, start, EPSILON);

        // Test end point (t=1)
        double[] end = HumanizationUtils.cubicBezierPoint(1.0, controlPoints);
        assertArrayEquals(new double[]{10, 0, 0}, end, EPSILON);
    }

    @Test
    @DisplayName("Bezier should throw on invalid t parameter")
    void testBezierInvalidT() {
        List<double[]> controlPoints = Arrays.asList(
            new double[]{0, 0, 0},
            new double[]{5, 5, 0},
            new double[]{10, 0, 0}
        );

        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.bezierPoint(-0.1, controlPoints);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.bezierPoint(1.1, controlPoints);
        });
    }

    @Test
    @DisplayName("Bezier should throw on wrong number of control points")
    void testBezierWrongControlPoints() {
        List<double[]> wrongCount = Arrays.asList(
            new double[]{0, 0, 0},
            new double[]{5, 5, 0}
        );

        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.bezierPoint(0.5, wrongCount);
        });
    }

    // ========================================================================
    // Mistake Tests
    // ========================================================================

    @Test
    @DisplayName("Should make mistake at correct rate")
    void testShouldMakeMistakeRate() {
        double mistakeRate = 0.05; // 5%
        int trials = 10000;
        int mistakes = 0;

        HumanizationUtils.setSeed(42L);
        for (int i = 0; i < trials; i++) {
            if (HumanizationUtils.shouldMakeMistake(mistakeRate)) {
                mistakes++;
            }
        }

        double actualRate = (double) mistakes / trials;
        // Allow 5% absolute deviation from expected rate
        assertEquals(mistakeRate, actualRate, 0.05);
    }

    @Test
    @DisplayName("Should never make mistake at zero rate")
    void testShouldMakeMistakeZeroRate() {
        HumanizationUtils.setSeed(42L);
        for (int i = 0; i < 1000; i++) {
            assertFalse(HumanizationUtils.shouldMakeMistake(0.0));
        }
    }

    @Test
    @DisplayName("Should always make mistake at rate 1.0")
    void testShouldMakeMistakeAlways() {
        HumanizationUtils.setSeed(42L);
        for (int i = 0; i < 1000; i++) {
            assertTrue(HumanizationUtils.shouldMakeMistake(1.0));
        }
    }

    @Test
    @DisplayName("Should throw on invalid mistake rate")
    void testShouldMakeMistakeInvalidRate() {
        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.shouldMakeMistake(-0.1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.shouldMakeMistake(1.1);
        });
    }

    // ========================================================================
    // Reaction Time Tests
    // ========================================================================

    @Test
    @DisplayName("Human reaction time should be in valid range")
    void testHumanReactionTimeRange() {
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            int reactionTime = HumanizationUtils.humanReactionTime();
            assertTrue(reactionTime >= 150 && reactionTime <= 600,
                "Reaction time should be in range [150, 600]ms, got: " + reactionTime);
        }
    }

    @Test
    @DisplayName("Human reaction time should have correct distribution")
    void testHumanReactionTimeDistribution() {
        List<Integer> samples = new ArrayList<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            samples.add(HumanizationUtils.humanReactionTime());
        }

        // Calculate mean
        double mean = samples.stream().mapToInt(Integer::intValue).average().orElse(0);

        // Mean should be close to 300ms (within 10%)
        assertEquals(300.0, mean, 30.0);
    }

    @Test
    @DisplayName("Contextual reaction time should adjust for factors")
    void testContextualReactionTime() {
        // High fatigue should increase reaction time
        int fatiguedReaction = HumanizationUtils.contextualReactionTime(1.0, 0.0, 0.0);
        int normalReaction = HumanizationUtils.contextualReactionTime(0.0, 0.0, 0.0);

        assertTrue(fatiguedReaction > normalReaction,
            "Fatigued reaction time should be longer than normal");

        // High familiarity should decrease reaction time
        int familiarReaction = HumanizationUtils.contextualReactionTime(0.0, 0.0, 1.0);
        assertTrue(familiarReaction < normalReaction,
            "Familiar reaction time should be shorter than normal");
    }

    @Test
    @DisplayName("Contextual reaction time should clamp inputs")
    void testContextualReactionTimeClamp() {
        // Extreme values should be clamped
        int result = HumanizationUtils.contextualReactionTime(-1.0, 2.0, 1.5);
        // Should not throw and should be in valid range
        assertTrue(result >= 150 && result <= 1200); // Extended upper limit for fatigue
    }

    // ========================================================================
    // Micro-Movement Tests
    // ========================================================================

    @Test
    @DisplayName("Micro movement offset should be in range")
    void testMicroMovementOffsetRange() {
        double magnitude = 1.0;

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            double offset = HumanizationUtils.microMovementOffset(magnitude);
            assertTrue(offset >= -magnitude && offset <= magnitude,
                "Offset should be in range [-" + magnitude + ", " + magnitude + "], got: " + offset);
        }
    }

    @Test
    @DisplayName("Micro movement offset should have Gaussian distribution")
    void testMicroMovementOffsetDistribution() {
        double magnitude = 1.0;
        List<Double> samples = new ArrayList<>();

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            samples.add(HumanizationUtils.microMovementOffset(magnitude));
        }

        // Calculate mean (should be close to 0)
        double mean = samples.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // Mean should be close to 0 (within 0.1)
        assertEquals(0.0, mean, 0.1);
    }

    // ========================================================================
    // Edge Cases and Error Handling
    // ========================================================================

    @Test
    @DisplayName("Should handle null control points gracefully")
    void testNullControlPoints() {
        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.bezierPoint(0.5, null);
        });
    }

    @Test
    @DisplayName("Should handle wrong dimension control points")
    void testWrongDimensionControlPoints() {
        List<double[]> wrongDim = Arrays.asList(
            new double[]{0, 0},        // 2D
            new double[]{5, 5},
            new double[]{10, 0}
        );

        assertThrows(IllegalArgumentException.class, () -> {
            HumanizationUtils.bezierPoint(0.5, wrongDim);
        });
    }

    @Test
    @DisplayName("Seed should produce reproducible results")
    void testSeedReproducibility() {
        HumanizationUtils.setSeed(123L);
        int result1 = HumanizationUtils.humanReactionTime();

        HumanizationUtils.setSeed(123L);
        int result2 = HumanizationUtils.humanReactionTime();

        assertEquals(result1, result2, "Same seed should produce same result");
    }
}
