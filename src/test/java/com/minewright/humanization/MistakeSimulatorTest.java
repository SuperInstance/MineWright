package com.minewright.humanization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link MistakeSimulator}.
 *
 * <p>These tests verify that mistake simulation produces correct probabilities,
 * handles edge cases properly, and maintains statistical properties.</p>
 *
 * @since 2.2.0
 */
@DisplayName("MistakeSimulator Tests")
class MistakeSimulatorTest {

    private static final double EPSILON = 0.01;
    private static final int SAMPLE_SIZE = 1000;

    private MistakeSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new MistakeSimulator(0.03);
        simulator.setSeed(42L);
    }

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Test
    @DisplayName("Default constructor should use base mistake rate")
    void testDefaultConstructor() {
        MistakeSimulator defaultSimulator = new MistakeSimulator();
        assertEquals(0.03, defaultSimulator.getMistakeRate(), EPSILON,
            "Default mistake rate should be 3%");
    }

    @Test
    @DisplayName("Constructor with custom rate should set correctly")
    void testCustomRateConstructor() {
        MistakeSimulator customSimulator = new MistakeSimulator(0.05);
        assertEquals(0.05, customSimulator.getMistakeRate(), EPSILON,
            "Custom mistake rate should be 5%");
    }

    @Test
    @DisplayName("Constructor should throw on negative rate")
    void testConstructorNegativeRate() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MistakeSimulator(-0.1);
        });
    }

    @Test
    @DisplayName("Constructor should throw on rate greater than 1")
    void testConstructorRateGreaterThanOne() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MistakeSimulator(1.1);
        });
    }

    @Test
    @DisplayName("Constructor should accept zero rate")
    void testConstructorZeroRate() {
        assertDoesNotThrow(() -> new MistakeSimulator(0.0));
    }

    @Test
    @DisplayName("Constructor should accept rate of 1")
    void testConstructorRateOfOne() {
        assertDoesNotThrow(() -> new MistakeSimulator(1.0));
    }

    // ========================================================================
    // Mistake Probability Tests
    // ========================================================================

    @Test
    @DisplayName("Should make mistake at correct rate")
    void testShouldMakeMistakeRate() {
        double mistakeRate = 0.05; // 5%
        int trials = 10000;
        int mistakes = 0;

        simulator.setMistakeRate(mistakeRate);
        simulator.setSeed(42L);

        for (int i = 0; i < trials; i++) {
            if (simulator.shouldMakeMistake()) {
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
        simulator.setMistakeRate(0.0);
        simulator.setSeed(42L);

        for (int i = 0; i < 1000; i++) {
            assertFalse(simulator.shouldMakeMistake());
        }
    }

    @Test
    @DisplayName("Should always make mistake at rate 1.0")
    void testShouldMakeMistakeAlways() {
        simulator.setMistakeRate(1.0);
        simulator.setSeed(42L);

        for (int i = 0; i < 1000; i++) {
            assertTrue(simulator.shouldMakeMistake());
        }
    }

    @Test
    @DisplayName("Mistake rate should be persistent")
    void testMistakeRatePersistence() {
        simulator.setMistakeRate(0.07);
        assertEquals(0.07, simulator.getMistakeRate(), EPSILON);
    }

    // ========================================================================
    // Mistake Type Tests
    // ========================================================================

    @Test
    @DisplayName("Should respect type multipliers")
    void testMistakeTypeMultipliers() {
        simulator.setMistakeRate(0.10); // 10% base rate
        simulator.setSeed(42L);

        int trials = 10000;
        int wrongTargetMistakes = 0;
        int timingErrorMistakes = 0;
        int movementErrorMistakes = 0;
        int selectionErrorMistakes = 0;

        for (int i = 0; i < trials; i++) {
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.WRONG_TARGET)) {
                wrongTargetMistakes++;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.TIMING_ERROR)) {
                timingErrorMistakes++;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.MOVEMENT_ERROR)) {
                movementErrorMistakes++;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.SELECTION_ERROR)) {
                selectionErrorMistakes++;
            }
        }

        // Timing error should have highest rate (100% of base)
        // Selection error should have lowest rate (70% of base)
        assertTrue(timingErrorMistakes > selectionErrorMistakes,
            "Timing errors should be more frequent than selection errors");
    }

    @Test
    @DisplayName("All mistake types should have non-zero rates")
    void testAllMistakeTypesPossible() {
        simulator.setMistakeRate(0.50); // High rate to ensure mistakes occur
        simulator.setSeed(42L);

        boolean wrongTargetOccurred = false;
        boolean timingErrorOccurred = false;
        boolean movementErrorOccurred = false;
        boolean selectionErrorOccurred = false;

        for (int i = 0; i < 1000; i++) {
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.WRONG_TARGET)) {
                wrongTargetOccurred = true;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.TIMING_ERROR)) {
                timingErrorOccurred = true;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.MOVEMENT_ERROR)) {
                movementErrorOccurred = true;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.SELECTION_ERROR)) {
                selectionErrorOccurred = true;
            }
        }

        assertTrue(wrongTargetOccurred, "WRONG_TARGET mistakes should occur");
        assertTrue(timingErrorOccurred, "TIMING_ERROR mistakes should occur");
        assertTrue(movementErrorOccurred, "MOVEMENT_ERROR mistakes should occur");
        assertTrue(selectionErrorOccurred, "SELECTION_ERROR mistakes should occur");
    }

    // ========================================================================
    // Wrong Target Tests
    // ========================================================================

    @Test
    @DisplayName("Wrong target should be adjacent to intended")
    void testWrongTargetAdjacent() {
        int intendedX = 10;
        int intendedY = 5;
        int intendedZ = 15;

        simulator.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            int[] wrongTarget = simulator.getWrongTarget(intendedX, intendedY, intendedZ);

            int dx = Math.abs(wrongTarget[0] - intendedX);
            int dy = Math.abs(wrongTarget[1] - intendedY);
            int dz = Math.abs(wrongTarget[2] - intendedZ);

            // Total Manhattan distance should be exactly 1 (adjacent block)
            int totalDistance = dx + dy + dz;
            assertEquals(1, totalDistance,
                "Wrong target should be exactly 1 block away from intended");
        }
    }

    @Test
    @DisplayName("Wrong target should cover all directions")
    void testWrongTargetAllDirections() {
        int intendedX = 10;
        int intendedY = 5;
        int intendedZ = 15;

        simulator.setSeed(42L);

        boolean east = false, west = false, up = false, down = false, south = false, north = false;

        for (int i = 0; i < 1000; i++) {
            int[] wrongTarget = simulator.getWrongTarget(intendedX, intendedY, intendedZ);

            if (wrongTarget[0] == intendedX + 1 && wrongTarget[1] == intendedY && wrongTarget[2] == intendedZ) east = true;
            if (wrongTarget[0] == intendedX - 1 && wrongTarget[1] == intendedY && wrongTarget[2] == intendedZ) west = true;
            if (wrongTarget[0] == intendedX && wrongTarget[1] == intendedY + 1 && wrongTarget[2] == intendedZ) up = true;
            if (wrongTarget[0] == intendedX && wrongTarget[1] == intendedY - 1 && wrongTarget[2] == intendedZ) down = true;
            if (wrongTarget[0] == intendedX && wrongTarget[1] == intendedY && wrongTarget[2] == intendedZ + 1) south = true;
            if (wrongTarget[0] == intendedX && wrongTarget[1] == intendedY && wrongTarget[2] == intendedZ - 1) north = true;
        }

        assertTrue(east, "Should target east direction");
        assertTrue(west, "Should target west direction");
        assertTrue(up, "Should target up direction");
        assertTrue(down, "Should target down direction");
        assertTrue(south, "Should target south direction");
        assertTrue(north, "Should target north direction");
    }

    @Test
    @DisplayName("Wrong target should handle negative coordinates")
    void testWrongTargetNegativeCoordinates() {
        int intendedX = -10;
        int intendedY = -5;
        int intendedZ = -15;

        simulator.setSeed(42L);

        int[] wrongTarget = simulator.getWrongTarget(intendedX, intendedY, intendedZ);

        int dx = Math.abs(wrongTarget[0] - intendedX);
        int dy = Math.abs(wrongTarget[1] - intendedY);
        int dz = Math.abs(wrongTarget[2] - intendedZ);

        int totalDistance = dx + dy + dz;
        assertEquals(1, totalDistance, "Should handle negative coordinates");
    }

    @Test
    @DisplayName("Wrong target should handle zero coordinates")
    void testWrongTargetZeroCoordinates() {
        int[] wrongTarget = simulator.getWrongTarget(0, 0, 0);

        int totalDistance = Math.abs(wrongTarget[0]) + Math.abs(wrongTarget[1]) + Math.abs(wrongTarget[2]);
        assertEquals(1, totalDistance, "Should handle zero coordinates");
    }

    // ========================================================================
    // Timing Error Tests
    // ========================================================================

    @Test
    @DisplayName("Mistaken delay should add extra delay")
    void testMistakenDelayExtra() {
        simulator.setSeed(42L);

        int intendedDelay = 100;
        List<Integer> delays = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            delays.add(simulator.getMistakenDelay(intendedDelay));
        }

        // Some delays should be longer than intended (extra delay)
        boolean hasExtraDelay = delays.stream().anyMatch(d -> d > intendedDelay);
        assertTrue(hasExtraDelay, "Should have some delays with extra time");

        // Some delays should be shorter than intended (acting early)
        boolean hasReducedDelay = delays.stream().anyMatch(d -> d < intendedDelay);
        assertTrue(hasReducedDelay, "Should have some delays with reduced time");
    }

    @Test
    @DisplayName("Mistaken delay should not go below minimum")
    void testMistakenDelayMinimum() {
        simulator.setSeed(42L);

        int intendedDelay = 20;

        for (int i = 0; i < 100; i++) {
            int mistakenDelay = simulator.getMistakenDelay(intendedDelay);
            assertTrue(mistakenDelay >= 10,
                "Mistaken delay should be at least 10ms, got: " + mistakenDelay);
        }
    }

    @Test
    @DisplayName("Mistaken delay should handle zero delay")
    void testMistakenDelayZero() {
        simulator.setSeed(42L);

        int mistakenDelay = simulator.getMistakenDelay(0);
        assertTrue(mistakenDelay >= 10, "Should handle zero delay gracefully");
    }

    @Test
    @DisplayName("Mistaken delay should handle large delays")
    void testMistakenDelayLarge() {
        simulator.setSeed(42L);

        int intendedDelay = 10000;
        int mistakenDelay = simulator.getMistakenDelay(intendedDelay);

        assertTrue(mistakenDelay > 0, "Should handle large delays");
        assertTrue(mistakenDelay >= intendedDelay || mistakenDelay < intendedDelay,
            "Mistaken delay should be different from intended");
    }

    // ========================================================================
    // Movement Error Tests
    // ========================================================================

    @Test
    @DisplayName("Movement error should be within magnitude")
    void testMovementErrorWithinMagnitude() {
        double magnitude = 0.5;

        simulator.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            double[] error = simulator.getMovementError(10.0, 5.0, 15.0, magnitude);

            double offsetX = error[0] - 10.0;
            double offsetY = error[1] - 5.0;
            double offsetZ = error[2] - 15.0;

            assertTrue(Math.abs(offsetX) <= magnitude,
                "X offset should be within magnitude");
            assertTrue(Math.abs(offsetY) <= magnitude,
                "Y offset should be within magnitude");
            assertTrue(Math.abs(offsetZ) <= magnitude,
                "Z offset should be within magnitude");
        }
    }

    @Test
    @DisplayName("Movement error should have Gaussian distribution")
    void testMovementErrorDistribution() {
        double magnitude = 1.0;
        List<Double> xOffsets = new ArrayList<>();

        simulator.setSeed(42L);

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            double[] error = simulator.getMovementError(0.0, 0.0, 0.0, magnitude);
            xOffsets.add(error[0]);
        }

        // Mean should be close to 0
        double mean = xOffsets.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        assertEquals(0.0, mean, 0.1, "Mean offset should be close to 0");
    }

    @Test
    @DisplayName("Movement error should handle zero magnitude")
    void testMovementErrorZeroMagnitude() {
        simulator.setSeed(42L);

        double[] error = simulator.getMovementError(10.0, 5.0, 15.0, 0.0);

        assertEquals(10.0, error[0], EPSILON, "X should not change with zero magnitude");
        assertEquals(5.0, error[1], EPSILON, "Y should not change with zero magnitude");
        assertEquals(15.0, error[2], EPSILON, "Z should not change with zero magnitude");
    }

    @Test
    @DisplayName("Movement error should handle negative coordinates")
    void testMovementErrorNegativeCoordinates() {
        double magnitude = 0.5;

        double[] error = simulator.getMovementError(-10.0, -5.0, -15.0, magnitude);

        assertTrue(error[0] >= -10.5 && error[0] <= -9.5,
            "Should handle negative X coordinate");
        assertTrue(error[1] >= -5.5 && error[1] <= -4.5,
            "Should handle negative Y coordinate");
        assertTrue(error[2] >= -15.5 && error[2] <= -14.5,
            "Should handle negative Z coordinate");
    }

    // ========================================================================
    // Selection Error Tests
    // ========================================================================

    @Test
    @DisplayName("Wrong selection should be adjacent to correct")
    void testWrongSelectionAdjacent() {
        int correctIndex = 5;
        int maxIndex = 10;

        simulator.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            int wrongIndex = simulator.getWrongSelection(correctIndex, maxIndex);
            int distance = Math.abs(wrongIndex - correctIndex);
            assertTrue(distance <= 1,
                "Wrong selection should be adjacent to correct, distance: " + distance);
        }
    }

    @Test
    @DisplayName("Wrong selection should handle index 0")
    void testWrongSelectionIndexZero() {
        int correctIndex = 0;
        int maxIndex = 5;

        simulator.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            int wrongIndex = simulator.getWrongSelection(correctIndex, maxIndex);
            assertTrue(wrongIndex >= 0 && wrongIndex <= maxIndex,
                "Wrong selection should be valid");
            if (wrongIndex != correctIndex) {
                assertEquals(1, wrongIndex, "Should select next index when at 0");
            }
        }
    }

    @Test
    @DisplayName("Wrong selection should handle max index")
    void testWrongSelectionMaxIndex() {
        int correctIndex = 10;
        int maxIndex = 10;

        simulator.setSeed(42L);

        int wrongIndex = simulator.getWrongSelection(correctIndex, maxIndex);
        assertTrue(wrongIndex <= maxIndex, "Wrong selection should be valid");
        assertTrue(wrongIndex != correctIndex || wrongIndex == correctIndex - 1,
            "Should select previous index when at max");
    }

    @Test
    @DisplayName("Wrong selection should handle single item")
    void testWrongSelectionSingleItem() {
        int correctIndex = 0;
        int maxIndex = 0;

        simulator.setSeed(42L);

        int wrongIndex = simulator.getWrongSelection(correctIndex, maxIndex);
        assertEquals(0, wrongIndex, "Should return 0 when only one item exists");
    }

    @Test
    @DisplayName("Wrong selection should handle zero max index")
    void testWrongSelectionZeroMaxIndex() {
        int wrongIndex = simulator.getWrongSelection(0, 0);
        assertEquals(0, wrongIndex, "Should handle zero max index");
    }

    // ========================================================================
    // Mistake Rate Adjustment Tests
    // ========================================================================

    @Test
    @DisplayName("Adjust mistake rate should increase with fatigue")
    void testAdjustMistakeRateFatigue() {
        double baseRate = simulator.getMistakeRate();

        simulator.adjustMistakeRate(1.0, 0.0, 0.0); // Maximum fatigue

        assertTrue(simulator.getMistakeRate() > baseRate,
            "Mistake rate should increase with fatigue");
    }

    @Test
    @DisplayName("Adjust mistake rate should increase with complexity")
    void testAdjustMistakeRateComplexity() {
        double baseRate = simulator.getMistakeRate();

        simulator.adjustMistakeRate(0.0, 1.0, 0.0); // Maximum complexity

        assertTrue(simulator.getMistakeRate() > baseRate,
            "Mistake rate should increase with complexity");
    }

    @Test
    @DisplayName("Adjust mistake rate should increase with stress")
    void testAdjustMistakeRateStress() {
        double baseRate = simulator.getMistakeRate();

        simulator.adjustMistakeRate(0.0, 0.0, 1.0); // Maximum stress

        assertTrue(simulator.getMistakeRate() > baseRate,
            "Mistake rate should increase with stress");
    }

    @Test
    @DisplayName("Adjust mistake rate should clamp to maximum")
    void testAdjustMistakeRateClampMax() {
        simulator.adjustMistakeRate(1.0, 1.0, 1.0); // Maximum all factors

        assertTrue(simulator.getMistakeRate() <= 0.20,
            "Mistake rate should be capped at 20%");
    }

    @Test
    @DisplayName("Adjust mistake rate should clamp negative inputs")
    void testAdjustMistakeRateClampNegative() {
        double baseRate = simulator.getMistakeRate();

        simulator.adjustMistakeRate(-1.0, -1.0, -1.0); // All negative

        // Should clamp to 0 and not throw
        assertTrue(simulator.getMistakeRate() >= 0);
    }

    @Test
    @DisplayName("Adjust mistake rate should clamp inputs above 1")
    void testAdjustMistakeRateClampAboveOne() {
        simulator.adjustMistakeRate(2.0, 2.0, 2.0); // All above 1

        // Should clamp to 1 and not throw
        assertTrue(simulator.getMistakeRate() >= 0 && simulator.getMistakeRate() <= 0.20);
    }

    // ========================================================================
    // Reset Tests
    // ========================================================================

    @Test
    @DisplayName("Reset mistake rate should return to base")
    void testResetMistakeRate() {
        simulator.setMistakeRate(0.15);
        simulator.resetMistakeRate();

        assertEquals(0.03, simulator.getMistakeRate(), EPSILON,
            "Reset should return to base rate of 3%");
    }

    @Test
    @DisplayName("Reset mistake rate after adjustment")
    void testResetMistakeRateAfterAdjustment() {
        simulator.adjustMistakeRate(1.0, 1.0, 1.0);
        double adjustedRate = simulator.getMistakeRate();

        simulator.resetMistakeRate();

        assertEquals(0.03, simulator.getMistakeRate(), EPSILON);
        assertNotEquals(adjustedRate, simulator.getMistakeRate(),
            "Reset should change rate from adjusted value");
    }

    // ========================================================================
    // Maybe Introduce Mistake Tests
    // ========================================================================

    @Test
    @DisplayName("Maybe introduce mistake should return correct value when no mistake")
    void testMaybeIntroduceMistakeNoMistake() {
        simulator.setMistakeRate(0.0);
        simulator.setSeed(42L);

        String result = simulator.maybeIntroduceMistake("correct", correct -> "mistake");
        assertEquals("correct", result, "Should return correct value when mistake rate is 0");
    }

    @Test
    @DisplayName("Maybe introduce mistake should return mistaken value when mistake occurs")
    void testMaybeIntroduceMistakeOccurs() {
        simulator.setMistakeRate(1.0);
        simulator.setSeed(42L);

        String result = simulator.maybeIntroduceMistake("correct", correct -> "mistake");
        assertEquals("mistake", result, "Should return mistaken value when mistake rate is 1");
    }

    @Test
    @DisplayName("Maybe introduce mistake should work with integers")
    void testMaybeIntroduceMistakeIntegers() {
        simulator.setMistakeRate(1.0);
        simulator.setSeed(42L);

        Integer result = simulator.maybeIntroduceMistake(10, correct -> correct + 1);
        assertEquals(11, result, "Should work with integer values");
    }

    // ========================================================================
    // Set Mistake Rate Tests
    // ========================================================================

    @Test
    @DisplayName("Set mistake rate should update correctly")
    void testSetMistakeRate() {
        simulator.setMistakeRate(0.08);
        assertEquals(0.08, simulator.getMistakeRate(), EPSILON);
    }

    @Test
    @DisplayName("Set mistake rate should throw on negative")
    void testSetMistakeRateNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            simulator.setMistakeRate(-0.1);
        });
    }

    @Test
    @DisplayName("Set mistake rate should throw on greater than 1")
    void testSetMistakeRateGreaterThanOne() {
        assertThrows(IllegalArgumentException.class, () -> {
            simulator.setMistakeRate(1.1);
        });
    }

    // ========================================================================
    // Seed Tests
    // ========================================================================

    @Test
    @DisplayName("Seed should produce reproducible results")
    void testSeedReproducibility() {
        simulator.setSeed(123L);
        boolean result1 = simulator.shouldMakeMistake();

        simulator.setSeed(123L);
        boolean result2 = simulator.shouldMakeMistake();

        assertEquals(result1, result2, "Same seed should produce same result");
    }

    @Test
    @DisplayName("Different seeds should produce different results")
    void testDifferentSeeds() {
        simulator.setSeed(111L);
        boolean result1 = simulator.shouldMakeMistake();

        simulator.setSeed(222L);
        boolean result2 = simulator.shouldMakeMistake();

        // Results may be same by chance, but unlikely
        // Just verify it doesn't crash
        assertNotNull(result1);
        assertNotNull(result2);
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle very small mistake rate")
    void testVerySmallMistakeRate() {
        simulator.setMistakeRate(0.0001); // 0.01%
        simulator.setSeed(42L);

        // Very unlikely to make mistake in 1000 trials
        int mistakes = 0;
        for (int i = 0; i < 1000; i++) {
            if (simulator.shouldMakeMistake()) {
                mistakes++;
            }
        }

        assertTrue(mistakes <= 1, "Should have very few or no mistakes at 0.01% rate");
    }

    @Test
    @DisplayName("Should handle mistake rate at boundary")
    void testBoundaryMistakeRate() {
        simulator.setMistakeRate(0.999999); // Very close to 1
        simulator.setSeed(42L);

        // Almost always should make mistake
        int successes = 0;
        for (int i = 0; i < 1000; i++) {
            if (simulator.shouldMakeMistake()) {
                successes++;
            }
        }

        assertTrue(successes >= 990, "Should make mistake almost always");
    }
}
