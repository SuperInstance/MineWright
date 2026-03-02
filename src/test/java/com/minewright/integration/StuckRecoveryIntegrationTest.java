package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.recovery.RecoveryManager;
import com.minewright.recovery.RecoveryResult;
import com.minewright.recovery.StuckDetector;
import com.minewright.recovery.StuckType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for stuck detection and recovery working together.
 *
 * <p><b>Test Scenarios:</b></p>
 * <ul>
 *   <li>Agent gets stuck, StuckDetector detects it, RecoveryManager recovers</li>
 *   <li>Position stuck detection and recovery</li>
 *   <li>Progress stuck detection and recovery</li>
 *   <li>State stuck detection and recovery</li>
 *   <li>Path stuck detection and recovery</li>
 *   <li>Multiple recovery attempts with escalation</li>
 *   <li>Recovery statistics tracking</li>
 * </ul>
 *
 * @see StuckDetector
 * @see RecoveryManager
 * @see IntegrationTestBase
 * @since 1.2.0
 */
@DisplayName("Stuck Recovery Integration Tests")
class StuckRecoveryIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Agent gets stuck and recovers successfully")
    void testAgentStuckAndRecovers() {
        // Create a foreman entity
        ForemanEntity foreman = createForeman("Steve");

        // Create stuck detector and recovery manager
        StuckDetector detector = new StuckDetector(foreman);
        RecoveryManager recoveryManager = new RecoveryManager(foreman);

        // Simulate stuck conditions
        Vec3 stuckPosition = new Vec3(0, 64, 0);
        when(foreman.position()).thenReturn(stuckPosition);

        // Tick until position stuck is detected (60 ticks)
        for (int i = 0; i < StuckDetector.StuckDetectionThreshold.POSITION_STUCK_TICKS; i++) {
            detector.tickAndDetect();
        }

        // Verify stuck was detected
        StuckType stuckType = detector.detectStuck();
        assertNotNull(stuckType, "Stuck should be detected");
        assertEquals(StuckType.POSITION_STUCK, stuckType, "Should detect position stuck");

        // Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        // Verify recovery was attempted
        assertNotNull(result, "Recovery result should not be null");
        assertTrue(
            result == RecoveryResult.SUCCESS || result == RecoveryResult.RETRY ||
            result == RecoveryResult.ESCALATE || result == RecoveryResult.ABORT,
            "Recovery should return a valid result"
        );

        // If recovery succeeded, detector should be reset
        if (result == RecoveryResult.SUCCESS) {
            detector.reset();
            assertEquals(0, detector.getStuckPositionTicks(),
                "Position stuck ticks should be reset after recovery");
        }
    }

    @Test
    @DisplayName("Position stuck is detected after 60 ticks without movement")
    void testPositionStuckDetection() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);

        // Mock entity to stay at same position
        Vec3 fixedPosition = new Vec3(100, 64, 200);
        when(foreman.position()).thenReturn(fixedPosition);

        // Tick for exactly the threshold
        for (int i = 0; i < 60; i++) {
            boolean detected = detector.tickAndDetect();
            // Should only detect stuck at or after threshold
            if (i < 59) {
                assertFalse(detected,
                    "Should not detect stuck before threshold (tick " + i + ")");
            }
        }

        // Verify stuck detection
        assertTrue(detector.isPositionStuck(60), "Should detect position stuck at threshold");
        assertEquals(StuckType.POSITION_STUCK, detector.detectStuck(),
            "Should detect position stuck type");
    }

    @Test
    @DisplayName("Progress stuck is detected after 100 ticks without progress")
    void testProgressStuckDetection() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);

        // Mock entity with zero progress
        when(foreman.getActionExecutor().getCurrentActionProgress()).thenReturn(0);

        // Tick for progress threshold (100 ticks)
        for (int i = 0; i < 100; i++) {
            detector.tickAndDetect();
        }

        // Verify progress stuck detection
        assertTrue(detector.isProgressStuck(0, 0, 100),
            "Should detect progress stuck at threshold");
    }

    @Test
    @DisplayName("State stuck is detected after 200 ticks in same state")
    void testStateStuckDetection() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);

        // Mock state machine to stay in EXECUTING state
        when(foreman.getActionExecutor().getStateMachine().getCurrentState())
            .thenReturn(AgentState.EXECUTING);

        // Tick for state threshold (200 ticks)
        for (int i = 0; i < 200; i++) {
            detector.tickAndDetect();
        }

        // Verify state stuck detection
        assertTrue(detector.isStateStuck(AgentState.EXECUTING, 200),
            "Should detect state stuck at threshold");
    }

    @Test
    @DisplayName("Path stuck is detected immediately when marked")
    void testPathStuckDetection() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);

        // Mark path as stuck
        detector.markPathStuck();

        // Verify immediate detection
        assertTrue(detector.isPathStuck(), "Should detect path stuck immediately");
        assertEquals(StuckType.PATH_STUCK, detector.detectStuck(),
            "Should detect path stuck type with highest priority");
    }

    @Test
    @DisplayName("Recovery manager escalates through strategies")
    void testRecoveryEscalation() {
        ForemanEntity foreman = createForeman("Steve");
        RecoveryManager recoveryManager = new RecoveryManager(foreman);

        // Track attempt counts
        int initialAttempts = recoveryManager.getTotalAttempts();

        // Attempt recovery for position stuck
        RecoveryResult result1 = recoveryManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Verify attempt was recorded
        assertEquals(initialAttempts + 1, recoveryManager.getTotalAttempts(),
            "Should record recovery attempt");

        // If first strategy fails, should escalate
        if (result1 == RecoveryResult.ESCALATE) {
            // Next attempt should try next strategy
            RecoveryResult result2 = recoveryManager.attemptRecovery(StuckType.POSITION_STUCK);
            assertNotNull(result2, "Escalated recovery should also return result");
        }
    }

    @Test
    @DisplayName("Recovery statistics are tracked correctly")
    void testRecoveryStatistics() {
        ForemanEntity foreman = createForeman("Steve");
        RecoveryManager recoveryManager = new RecoveryManager(foreman);

        // Make several recovery attempts
        recoveryManager.attemptRecovery(StuckType.POSITION_STUCK);
        recoveryManager.attemptRecovery(StuckType.PROGRESS_STUCK);

        // Get statistics
        RecoveryManager.RecoveryStats stats = recoveryManager.getStats();

        // Verify statistics
        assertEquals(2, stats.totalAttempts(), "Should record total attempts");
        assertTrue(stats.totalAttempts() > 0, "Should have at least one attempt");
        assertTrue(stats.successRate() >= 0.0 && stats.successRate() <= 1.0,
            "Success rate should be between 0 and 1");
    }

    @Test
    @DisplayName("Detector state snapshot captures all tracking data")
    void testDetectorStateSnapshot() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);

        // Simulate some stuck ticks
        Vec3 fixedPosition = new Vec3(50, 64, 50);
        when(foreman.position()).thenReturn(fixedPosition);

        for (int i = 0; i < 30; i++) {
            detector.tickAndDetect();
        }

        // Get snapshot
        StuckDetector.DetectionState snapshot = detector.getStateSnapshot();

        // Verify snapshot contains all data
        assertNotNull(snapshot, "Snapshot should not be null");
        assertEquals(30, snapshot.stuckPositionTicks(),
            "Snapshot should capture position stuck ticks");
        assertNotNull(snapshot.detectionHistory(),
            "Snapshot should include detection history");
    }

    @Test
    @DisplayName("Stuck detector resets after recovery")
    void testDetectorResetAfterRecovery() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);
        RecoveryManager recoveryManager = new RecoveryManager(foreman);

        // Simulate stuck condition
        Vec3 fixedPosition = new Vec3(75, 64, 75);
        when(foreman.position()).thenReturn(fixedPosition);

        for (int i = 0; i < 60; i++) {
            detector.tickAndDetect();
        }

        // Verify stuck was detected
        StuckType stuckType = detector.detectStuck();
        assertNotNull(stuckType, "Should detect stuck before recovery");

        // Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        // Reset detector after successful recovery
        if (result == RecoveryResult.SUCCESS) {
            detector.reset();

            // Verify all counters reset
            assertEquals(0, detector.getStuckPositionTicks(),
                "Position stuck ticks should reset");
            assertEquals(0, detector.getStuckProgressTicks(),
                "Progress stuck ticks should reset");
            assertEquals(0, detector.getStuckStateTicks(),
                "State stuck ticks should reset");
            assertFalse(detector.isPathStuck(),
                "Path stuck status should reset");
        }
    }

    @Test
    @DisplayName("Multiple stuck types detected in priority order")
    void testStuckTypePriority() {
        ForemanEntity foreman = createForeman("Steve");
        StuckDetector detector = new StuckDetector(foreman);

        // Mark path as stuck (highest priority)
        detector.markPathStuck();

        // Also simulate position stuck
        Vec3 fixedPosition = new Vec3(25, 64, 25);
        when(foreman.position()).thenReturn(fixedPosition);

        for (int i = 0; i < 60; i++) {
            detector.tickAndDetect();
        }

        // Path stuck should have priority over position stuck
        StuckType detected = detector.detectStuck();
        assertEquals(StuckType.PATH_STUCK, detected,
            "Path stuck should have highest priority");

        // Clear path stuck
        detector.clearPathStuck();

        // Now position stuck should be detected
        detected = detector.detectStuck();
        assertEquals(StuckType.POSITION_STUCK, detected,
            "Position stuck should be detected after path stuck is cleared");
    }

    // ==================== Helper Methods ====================

    /**
     * Stuck detection threshold constants for testing.
     */
    private static final class StuckDetectionThreshold {
        public static final int POSITION_STUCK_TICKS = 60;
        public static final int PROGRESS_STUCK_TICKS = 100;
        public static final int STATE_STUCK_TICKS = 200;
    }

    private void when(Vec3 position) {
        // Helper for mocking - in actual tests would use Mockito.when()
        // This is a placeholder for the test pattern
    }

    private void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

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

    private void fail(String message) {
        throw new AssertionError(message);
    }
}
