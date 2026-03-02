package com.minewright.integration;

import com.minewright.humanization.HumanizationUtils;
import com.minewright.humanization.MistakeSimulator;
import com.minewright.humanization.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for humanization features working together.
 *
 * <p><b>Test Scenarios:</b></p>
 * <ul>
 *   <li>Actions have timing jitter applied</li>
 *   <li>Mistakes occur at configured rate</li>
 *   <li>Session affects performance (warmup, performance, fatigue phases)</li>
 *   <li>Mistake rate adjusts based on fatigue and complexity</li>
 *   <li>Reaction times follow human-like distribution</li>
 *   <li>Bezier curves create smooth movement paths</li>
 *   <li>Session manager handles breaks correctly</li>
 * </ul>
 *
 * @see HumanizationUtils
 * @see MistakeSimulator
 * @see SessionManager
 * @see IntegrationTestBase
 * @since 1.2.0
 */
@DisplayName("Humanization Integration Tests")
class HumanizationIntegrationTest extends IntegrationTestBase {

    private MistakeSimulator mistakeSimulator;
    private SessionManager sessionManager;
    private static final long TEST_WARMUP_MS = 100; // 100ms for testing
    private static final long TEST_FATIGUE_MS = 500; // 500ms for testing

    @BeforeEach
    void setUpHumanizationTest() {
        // Create mistake simulator with default rate (3%)
        mistakeSimulator = new MistakeSimulator(0.03);

        // Create session manager with shortened phases for testing
        sessionManager = new SessionManager(TEST_WARMUP_MS, TEST_FATIGUE_MS);
    }

    @AfterEach
    void tearDownHumanizationTest() {
        // Reset any modified state
        HumanizationUtils.setSeed(System.currentTimeMillis());
        if (sessionManager != null) {
            sessionManager.setEnabled(false);
        }
    }

    @Test
    @DisplayName("Actions have Gaussian timing jitter applied")
    void testTimingJitterApplied() {
        int baseDelay = 100; // 100ms base delay
        double variancePercent = 0.3; // 30% variance

        // Generate multiple jittered delays
        List<Integer> delays = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int jittered = HumanizationUtils.gaussianJitter(baseDelay, variancePercent);
            delays.add(jittered);
        }

        // Verify all delays are within realistic bounds
        for (int delay : delays) {
            assertTrue(delay >= 30,
                "Jittered delay should be at least minimum (30ms), got: " + delay);
            assertTrue(delay <= 1000,
                "Jittered delay should not exceed maximum (1000ms), got: " + delay);
        }

        // Verify variance exists (not all delays are the same)
        long uniqueCount = delays.stream().distinct().count();
        assertTrue(uniqueCount > 50,
            "Gaussian jitter should produce variety in delays");
    }

    @Test
    @DisplayName("Mistakes occur at configured rate")
    void testMistakesAtConfiguredRate() {
        double mistakeRate = 0.05; // 5% mistake rate
        int trials = 1000;

        MistakeSimulator simulator = new MistakeSimulator(mistakeRate);
        int mistakeCount = 0;

        for (int i = 0; i < trials; i++) {
            if (simulator.shouldMakeMistake()) {
                mistakeCount++;
            }
        }

        // Verify mistake count is close to expected rate
        double actualRate = (double) mistakeCount / trials;
        double tolerance = 0.02; // Allow 2% variance

        assertTrue(Math.abs(actualRate - mistakeRate) <= tolerance,
            String.format("Mistake rate should be close to configured rate. " +
                "Expected: %.2f, Actual: %.2f", mistakeRate, actualRate));
    }

    @Test
    @DisplayName("Session affects performance through different phases")
    void testSessionPhasesAffectPerformance() throws InterruptedException {
        // Enable session modeling
        sessionManager.setEnabled(true);

        // Initially in WARMUP phase
        assertEquals(SessionManager.SessionPhase.WARMUP, sessionManager.getCurrentPhase(),
            "Should start in warmup phase");
        assertEquals(1.3, sessionManager.getReactionMultiplier(),
            "Warmup phase should have slower reaction multiplier");
        assertEquals(1.5, sessionManager.getErrorMultiplier(),
            "Warmup phase should have higher error multiplier");

        // Wait for warmup to complete
        Thread.sleep(TEST_WARMUP_MS + 50);
        sessionManager.update();

        // Now in PERFORMANCE phase
        assertEquals(SessionManager.SessionPhase.PERFORMANCE, sessionManager.getCurrentPhase(),
            "Should be in performance phase after warmup");
        assertEquals(1.0, sessionManager.getReactionMultiplier(),
            "Performance phase should have normal reaction multiplier");
        assertEquals(1.0, sessionManager.getErrorMultiplier(),
            "Performance phase should have normal error multiplier");

        // Wait for fatigue to set in
        Thread.sleep(TEST_FATIGUE_MS - TEST_WARMUP_MS + 50);
        sessionManager.update();

        // Now in FATIGUE phase
        assertEquals(SessionManager.SessionPhase.FATIGUE, sessionManager.getCurrentPhase(),
            "Should be in fatigue phase after time elapsed");
        assertEquals(1.5, sessionManager.getReactionMultiplier(),
            "Fatigue phase should have slower reaction multiplier");
        assertEquals(2.0, sessionManager.getErrorMultiplier(),
            "Fatigue phase should have higher error multiplier");
    }

    @Test
    @DisplayName("Mistake rate adjusts based on fatigue and complexity")
    void testMistakeRateAdjustsForContext() {
        double baseRate = 0.03;
        MistakeSimulator simulator = new MistakeSimulator(baseRate);

        // Base rate
        assertEquals(baseRate, simulator.getMistakeRate(),
            "Should start with base rate");

        // Adjust for high fatigue
        simulator.adjustMistakeRate(0.8, 0.0, 0.0); // 80% fatigue
        assertTrue(simulator.getMistakeRate() > baseRate,
            "Mistake rate should increase with fatigue");

        // Adjust for high complexity
        simulator.resetMistakeRate();
        simulator.adjustMistakeRate(0.0, 0.9, 0.0); // 90% complexity
        assertTrue(simulator.getMistakeRate() > baseRate,
            "Mistake rate should increase with complexity");

        // Adjust for high stress
        simulator.resetMistakeRate();
        simulator.adjustMistakeRate(0.0, 0.0, 0.7); // 70% stress
        assertTrue(simulator.getMistakeRate() > baseRate,
            "Mistake rate should increase with stress");

        // Adjust for all factors
        simulator.resetMistakeRate();
        simulator.adjustMistakeRate(0.7, 0.6, 0.5);
        double highContextRate = simulator.getMistakeRate();
        assertTrue(highContextRate > baseRate * 1.5,
            "Mistake rate should increase significantly with multiple factors");

        // Verify rate doesn't exceed maximum (20%)
        assertTrue(simulator.getMistakeRate() <= 0.20,
            "Mistake rate should not exceed maximum of 20%");
    }

    @Test
    @DisplayName("Reaction times follow human-like Gaussian distribution")
    void testReactionTimeDistribution() {
        // Generate many reaction times
        List<Integer> reactionTimes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            reactionTimes.add(HumanizationUtils.humanReactionTime());
        }

        // Calculate mean and standard deviation
        double mean = reactionTimes.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);

        // Mean should be close to 300ms
        assertTrue(mean >= 250 && mean <= 350,
            "Mean reaction time should be close to 300ms, got: " + mean);

        // All times should be within realistic range
        for (int time : reactionTimes) {
            assertTrue(time >= 150 && time <= 600,
                "Reaction time should be within realistic range [150, 600]ms, got: " + time);
        }

        // Verify distribution (most values within 1 standard deviation)
        long withinOneStdDev = reactionTimes.stream()
            .filter(time -> time >= 250 && time <= 350)
            .count();

        double percentage = (double) withinOneStdDev / reactionTimes.size();
        assertTrue(percentage >= 0.60 && percentage <= 0.75,
            "Approximately 68% of values should be within 1 standard deviation, got: " + percentage);
    }

    @Test
    @DisplayName("Bezier curves create smooth movement paths")
    void testBezierCurveSmoothness() {
        // Create control points for quadratic Bezier
        List<double[]> controlPoints = List.of(
            new double[]{0, 0, 0},    // Start
            new double[]{5, 10, 0},   // Control
            new double[]{10, 0, 0}    // End
        );

        // Sample points along the curve
        List<double[]> pathPoints = new ArrayList<>();
        for (double t = 0.0; t <= 1.0; t += 0.1) {
            double[] point = HumanizationUtils.bezierPoint(t, controlPoints);
            pathPoints.add(point);
        }

        // Verify we got 11 points (0.0 to 1.0 in 0.1 increments)
        assertEquals(11, pathPoints.size(),
            "Should generate 11 points along the curve");

        // Verify start and end points match control points
        assertArrayEquals(new double[]{0, 0, 0}, pathPoints.get(0), 0.001,
            "Curve should start at first control point");
        assertArrayEquals(new double[]{10, 0, 0}, pathPoints.get(10), 0.001,
            "Curve should end at last control point");

        // Verify intermediate point is elevated (curved)
        double[] midPoint = pathPoints.get(5); // t = 0.5
        assertTrue(midPoint[1] > 0,
            "Middle of curve should be elevated (y > 0)");
        assertTrue(midPoint[0] > 0 && midPoint[0] < 10,
            "Middle of curve should be between start and end x");
    }

    @Test
    @DisplayName("Session manager handles breaks correctly")
    void testSessionBreakHandling() throws InterruptedException {
        sessionManager.setEnabled(true);

        // Initially not on break
        assertFalse(sessionManager.isOnBreak(),
            "Should not be on break initially");
        assertEquals(0, sessionManager.getBreakTimeRemaining(),
            "Break time remaining should be 0 when not on break");

        // Start a break
        sessionManager.startBreak(200); // 200ms break

        assertTrue(sessionManager.isOnBreak(),
            "Should be on break after startBreak()");
        assertTrue(sessionManager.getBreakTimeRemaining() > 0,
            "Break time remaining should be positive");

        // Wait for break to end
        Thread.sleep(250);
        sessionManager.update();

        assertFalse(sessionManager.isOnBreak(),
            "Should no longer be on break after break duration");
    }

    @Test
    @DisplayName("Different mistake types have different probabilities")
    void testMistakeTypeProbabilities() {
        int trials = 10000;
        double baseRate = 0.10; // Use higher rate for faster testing

        MistakeSimulator simulator = new MistakeSimulator(baseRate);

        // Count each mistake type
        int wrongTargetCount = 0;
        int timingErrorCount = 0;
        int movementErrorCount = 0;
        int selectionErrorCount = 0;

        for (int i = 0; i < trials; i++) {
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.WRONG_TARGET)) {
                wrongTargetCount++;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.TIMING_ERROR)) {
                timingErrorCount++;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.MOVEMENT_ERROR)) {
                movementErrorCount++;
            }
            if (simulator.shouldMakeMistake(MistakeSimulator.MistakeType.SELECTION_ERROR)) {
                selectionErrorCount++;
            }
        }

        // Verify relative probabilities
        // TIMING_ERROR: 100% of base rate
        // MOVEMENT_ERROR: 90% of base rate
        // WRONG_TARGET: 80% of base rate
        // SELECTION_ERROR: 70% of base rate

        assertTrue(timingErrorCount >= movementErrorCount,
            "Timing errors should be most frequent");
        assertTrue(movementErrorCount >= wrongTargetCount,
            "Movement errors should be second most frequent");
        assertTrue(wrongTargetCount >= selectionErrorCount,
            "Wrong target errors should be third most frequent");
    }

    @Test
    @DisplayName("Contextual reaction time adjusts for factors")
    void testContextualReactionTime() {
        // Base reaction time
        int baseReaction = HumanizationUtils.contextualReactionTime(0.0, 0.0, 0.0);

        // Fatigue should slow reaction
        int fatiguedReaction = HumanizationUtils.contextualReactionTime(0.8, 0.0, 0.0);
        assertTrue(fatiguedReaction >= baseReaction,
            "Fatigued reaction time should be >= base");

        // Complexity should slow reaction
        int complexReaction = HumanizationUtils.contextualReactionTime(0.0, 0.9, 0.0);
        assertTrue(complexReaction >= baseReaction,
            "Complex task reaction time should be >= base");

        // Familiarity should speed up reaction
        int familiarReaction = HumanizationUtils.contextualReactionTime(0.0, 0.0, 0.8);
        assertTrue(familiarReaction <= baseReaction,
            "Familiar task reaction time should be <= base");

        // All factors combined
        int combinedReaction = HumanizationUtils.contextualReactionTime(0.5, 0.5, 0.3);
        assertTrue(combinedReaction >= 150 && combinedReaction <= 2000,
            "Combined reaction time should be within realistic range");
    }

    @Test
    @DisplayName("Mistake simulators produce correct wrong targets")
    void testWrongTargetGeneration() {
        int intendedX = 10;
        int intendedY = 64;
        int intendedZ = 20;

        int[] wrongTarget = mistakeSimulator.getWrongTarget(intendedX, intendedY, intendedZ);

        // Verify wrong target is adjacent to intended
        int dx = Math.abs(wrongTarget[0] - intendedX);
        int dy = Math.abs(wrongTarget[1] - intendedY);
        int dz = Math.abs(wrongTarget[2] - intendedZ);

        int totalDistance = dx + dy + dz;
        assertEquals(1, totalDistance,
            "Wrong target should be exactly 1 block away from intended");
    }

    @Test
    @DisplayName("Mistake simulators produce timing delays")
    void testMistakenDelayGeneration() {
        int intendedDelay = 100;

        int mistakenDelay = mistakeSimulator.getMistakenDelay(intendedDelay);

        // Delay should be different (either longer or shorter)
        assertTrue(Math.abs(mistakenDelay - intendedDelay) >= 10,
            "Mistaken delay should differ from intended by at least 10ms");

        // But still within reasonable bounds
        assertTrue(mistakenDelay >= 10,
            "Mistaken delay should not be less than 10ms");
        assertTrue(mistakenDelay <= intendedDelay + 100,
            "Mistaken delay should not add more than 100ms");
    }

    @Test
    @DisplayName("Session fatigue level increases over time")
    void testSessionFatigueProgression() throws InterruptedException {
        sessionManager.setEnabled(true);

        // Initially no fatigue
        assertEquals(0.0, sessionManager.getFatigueLevel(),
            "Should have no fatigue initially");

        // Fatigue should still be 0 before onset
        Thread.sleep(TEST_WARMUP_MS);
        sessionManager.update();
        assertEquals(0.0, sessionManager.getFatigueLevel(),
            "Should have no fatigue before onset time");

        // Wait for fatigue onset
        Thread.sleep(TEST_FATIGUE_MS - TEST_WARMUP_MS + 100);
        sessionManager.update();

        // Now fatigue should be > 0
        double fatigueLevel = sessionManager.getFatigueLevel();
        assertTrue(fatigueLevel > 0.0,
            "Fatigue level should be positive after onset time");
        assertTrue(fatigueLevel <= 1.0,
            "Fatigue level should not exceed 1.0");
    }

    // ==================== Helper Methods ====================

    private void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private void assertArrayEquals(double[] expected, double[] actual, double delta, String message) {
        if (expected.length != actual.length) {
            throw new AssertionError(message + " (array lengths differ)");
        }
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > delta) {
                throw new AssertionError(message + " (arrays differ at index " + i +
                    ": expected=" + expected[i] + ", actual=" + actual[i] + ")");
            }
        }
    }
}
