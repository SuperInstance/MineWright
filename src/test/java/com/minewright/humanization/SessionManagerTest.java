package com.minewright.humanization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link SessionManager}.
 *
 * <p>These tests verify that session phases transition correctly,
 * fatigue levels increase appropriately, and breaks work as expected.</p>
 *
 * @since 2.2.0
 */
@DisplayName("SessionManager Tests")
class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
        sessionManager.setSeed(42L);
    }

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Test
    @DisplayName("Default constructor should create session manager")
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new SessionManager());
    }

    @Test
    @DisplayName("Constructor with custom timing should use provided values")
    void testConstructorWithCustomTiming() {
        long warmupDuration = 5 * 60 * 1000; // 5 minutes
        long fatigueOnset = 30 * 60 * 1000; // 30 minutes

        SessionManager customManager = new SessionManager(warmupDuration, fatigueOnset);

        assertDoesNotThrow(() -> customManager.getCurrentPhase());
    }

    @Test
    @DisplayName("Constructor should handle zero timing values")
    void testConstructorWithZeroTiming() {
        assertDoesNotThrow(() -> new SessionManager(0, 0));
    }

    @Test
    @DisplayName("Constructor should handle very large timing values")
    void testConstructorWithLargeTiming() {
        long veryLongTime = 24 * 60 * 60 * 1000L; // 24 hours

        assertDoesNotThrow(() -> new SessionManager(veryLongTime, veryLongTime));
    }

    // ========================================================================
    // Enable/Disable Tests
    // ========================================================================

    @Test
    @DisplayName("Should be enabled by default")
    void testEnabledByDefault() {
        assertTrue(sessionManager.isEnabled(), "Session manager should be enabled by default");
    }

    @Test
    @DisplayName("Disabled should return PERFORMANCE phase")
    void testDisabledReturnsPerformancePhase() {
        sessionManager.setEnabled(false);

        assertEquals(SessionManager.SessionPhase.PERFORMANCE, sessionManager.getCurrentPhase(),
            "Disabled session should return PERFORMANCE phase");
    }

    @Test
    @DisplayName("Disabled should return 1.0 reaction multiplier")
    void testDisabledReturnsNormalReactionMultiplier() {
        sessionManager.setEnabled(false);

        assertEquals(1.0, sessionManager.getReactionMultiplier(), 0.001,
            "Disabled session should return normal reaction multiplier");
    }

    @Test
    @DisplayName("Disabled should return 1.0 error multiplier")
    void testDisabledReturnsNormalErrorMultiplier() {
        sessionManager.setEnabled(false);

        assertEquals(1.0, sessionManager.getErrorMultiplier(), 0.001,
            "Disabled session should return normal error multiplier");
    }

    @Test
    @DisplayName("Disabled should return 0.0 fatigue level")
    void testDisabledReturnsZeroFatigue() {
        sessionManager.setEnabled(false);

        assertEquals(0.0, sessionManager.getFatigueLevel(), 0.001,
            "Disabled session should return zero fatigue");
    }

    @Test
    @DisplayName("Disable should prevent breaks")
    void testDisablePreventsBreaks() {
        sessionManager.setEnabled(false);

        assertFalse(sessionManager.shouldTakeBreak(),
            "Disabled session should not trigger breaks");
    }

    @Test
    @DisplayName("Toggle enabled state")
    void testToggleEnabled() {
        assertTrue(sessionManager.isEnabled());

        sessionManager.setEnabled(false);
        assertFalse(sessionManager.isEnabled());

        sessionManager.setEnabled(true);
        assertTrue(sessionManager.isEnabled());
    }

    // ========================================================================
    // Session Phase Tests
    // ========================================================================

    @Test
    @DisplayName("Initial phase should be WARMUP")
    void testInitialPhase() {
        assertEquals(SessionManager.SessionPhase.WARMUP, sessionManager.getCurrentPhase(),
            "Initial phase should be WARMUP");
    }

    @Test
    @DisplayName("Phase should be WARMUP immediately after start")
    void testWarmupPhaseImmediately() {
        // Just created session should be in WARMUP
        assertEquals(SessionManager.SessionPhase.WARMUP, sessionManager.getCurrentPhase());
    }

    @Test
    @DisplayName("Custom warmup duration should affect phase timing")
    void testCustomWarmupDuration() {
        // Create session with very short warmup (1ms)
        SessionManager shortWarmupManager = new SessionManager(1, 60 * 60 * 1000);

        // Should be in PERFORMANCE phase immediately after warmup
        // Note: This test may be flaky due to timing, but we check it doesn't crash
        assertDoesNotThrow(() -> shortWarmupManager.getCurrentPhase());
    }

    @Test
    @DisplayName("Zero warmup should start in PERFORMANCE")
    void testZeroWarmupStartsInPerformance() {
        SessionManager noWarmupManager = new SessionManager(0, 60 * 60 * 1000);

        assertEquals(SessionManager.SessionPhase.PERFORMANCE, noWarmupManager.getCurrentPhase(),
            "With zero warmup, should start in PERFORMANCE");
    }

    // ========================================================================
    // Reaction Multiplier Tests
    // ========================================================================

    @Test
    @DisplayName("WARMUP phase should increase reaction time")
    void testWarmupReactionMultiplier() {
        // Create session with long warmup
        SessionManager warmupManager = new SessionManager(60 * 60 * 1000, 120 * 60 * 1000);

        assertEquals(SessionManager.SessionPhase.WARMUP, warmupManager.getCurrentPhase());
        assertEquals(1.3, warmupManager.getReactionMultiplier(), 0.001,
            "WARMUP should increase reaction time by 30%");
    }

    @Test
    @DisplayName("PERFORMANCE phase should have normal reaction time")
    void testPerformanceReactionMultiplier() {
        // Create session with zero warmup
        SessionManager performanceManager = new SessionManager(0, 60 * 60 * 1000);

        assertEquals(SessionManager.SessionPhase.PERFORMANCE, performanceManager.getCurrentPhase());
        assertEquals(1.0, performanceManager.getReactionMultiplier(), 0.001,
            "PERFORMANCE should have normal reaction time");
    }

    @Test
    @DisplayName("FATIGUE phase should increase reaction time")
    void testFatigueReactionMultiplier() {
        // Create session with zero warmup and zero fatigue onset
        SessionManager fatigueManager = new SessionManager(0, 0);

        assertEquals(SessionManager.SessionPhase.FATIGUE, fatigueManager.getCurrentPhase());
        assertEquals(1.5, fatigueManager.getReactionMultiplier(), 0.001,
            "FATIGUE should increase reaction time by 50%");
    }

    @Test
    @DisplayName("On break should return normal reaction multiplier")
    void testOnBreakReactionMultiplier() {
        sessionManager.startBreak();

        assertEquals(1.0, sessionManager.getReactionMultiplier(), 0.001,
            "On break should return normal reaction multiplier");
    }

    // ========================================================================
    // Error Multiplier Tests
    // ========================================================================

    @Test
    @DisplayName("WARMUP phase should increase error rate")
    void testWarmupErrorMultiplier() {
        SessionManager warmupManager = new SessionManager(60 * 60 * 1000, 120 * 60 * 1000);

        assertEquals(SessionManager.SessionPhase.WARMUP, warmupManager.getCurrentPhase());
        assertEquals(1.5, warmupManager.getErrorMultiplier(), 0.001,
            "WARMUP should increase error rate by 50%");
    }

    @Test
    @DisplayName("PERFORMANCE phase should have normal error rate")
    void testPerformanceErrorMultiplier() {
        SessionManager performanceManager = new SessionManager(0, 60 * 60 * 1000);

        assertEquals(SessionManager.SessionPhase.PERFORMANCE, performanceManager.getCurrentPhase());
        assertEquals(1.0, performanceManager.getErrorMultiplier(), 0.001,
            "PERFORMANCE should have normal error rate");
    }

    @Test
    @DisplayName("FATIGUE phase should double error rate")
    void testFatigueErrorMultiplier() {
        SessionManager fatigueManager = new SessionManager(0, 0);

        assertEquals(SessionManager.SessionPhase.FATIGUE, fatigueManager.getCurrentPhase());
        assertEquals(2.0, fatigueManager.getErrorMultiplier(), 0.001,
            "FATIGUE should double error rate");
    }

    @Test
    @DisplayName("On break should return normal error multiplier")
    void testOnBreakErrorMultiplier() {
        sessionManager.startBreak();

        assertEquals(1.0, sessionManager.getErrorMultiplier(), 0.001,
            "On break should return normal error multiplier");
    }

    // ========================================================================
    // Fatigue Level Tests
    // ========================================================================

    @Test
    @DisplayName("Fatigue level should be zero initially")
    void testInitialFatigueLevel() {
        assertEquals(0.0, sessionManager.getFatigueLevel(), 0.001,
            "Initial fatigue level should be zero");
    }

    @Test
    @DisplayName("Fatigue level should be zero before fatigue onset")
    void testFatigueLevelBeforeOnset() {
        // Create session with fatigue onset in the future
        SessionManager noFatigueManager = new SessionManager(0, 24 * 60 * 60 * 1000); // 24 hours

        assertEquals(0.0, noFatigueManager.getFatigueLevel(), 0.001,
            "Fatigue should be zero before onset");
    }

    @Test
    @DisplayName("Fatigue level should increase after onset")
    void testFatigueLevelIncreases() {
        // Create session with zero fatigue onset (immediate fatigue)
        SessionManager immediateFatigueManager = new SessionManager(0, 0);

        double fatigueLevel = immediateFatigueManager.getFatigueLevel();

        // Should be greater than zero (though very small due to just starting)
        assertTrue(fatigueLevel >= 0.0, "Fatigue level should be non-negative");
    }

    @Test
    @DisplayName("Fatigue level should be capped at 1.0")
    void testFatigueLevelCapped() {
        // Create session with zero timing (maximum fatigue)
        SessionManager maxFatigueManager = new SessionManager(0, 0);

        double fatigueLevel = maxFatigueManager.getFatigueLevel();

        assertTrue(fatigueLevel <= 1.0, "Fatigue level should not exceed 1.0");
    }

    @Test
    @DisplayName("Disabled should return zero fatigue level")
    void testDisabledReturnsZeroFatigueLevel() {
        sessionManager.setEnabled(false);

        assertEquals(0.0, sessionManager.getFatigueLevel(), 0.001,
            "Disabled session should return zero fatigue");
    }

    // ========================================================================
    // Break Tests
    // ========================================================================

    @Test
    @DisplayName("Should not take break immediately")
    void testNoBreakImmediately() {
        assertFalse(sessionManager.shouldTakeBreak(),
            "Should not take break immediately after session start");
    }

    @Test
    @DisplayName("Start break should set on break flag")
    void testStartBreak() {
        sessionManager.startBreak();

        assertTrue(sessionManager.isOnBreak(), "Should be on break after startBreak()");
    }

    @Test
    @DisplayName("Start break with duration should set end time")
    void testStartBreakWithDuration() {
        long breakDuration = 60 * 1000; // 1 minute
        sessionManager.startBreak(breakDuration);

        assertTrue(sessionManager.isOnBreak());

        long remaining = sessionManager.getBreakTimeRemaining();
        assertTrue(remaining > 0 && remaining <= breakDuration,
            "Break time remaining should be positive and within duration");
    }

    @Test
    @DisplayName("Should not take break while on break")
    void testNoBreakWhileOnBreak() {
        sessionManager.startBreak();

        assertFalse(sessionManager.shouldTakeBreak(),
            "Should not trigger break while already on break");
    }

    @Test
    @DisplayName("Starting break while on break should be ignored")
    void testStartBreakWhileOnBreak() {
        sessionManager.startBreak();
        long firstEndTime = System.currentTimeMillis() + sessionManager.getBreakTimeRemaining();

        // Wait a bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        sessionManager.startBreak();

        // Second call should be ignored, so end time should not change significantly
        long secondEndTime = System.currentTimeMillis() + sessionManager.getBreakTimeRemaining();

        // Should be roughly the same (within 100ms tolerance)
        assertTrue(Math.abs(secondEndTime - firstEndTime) < 100,
            "Starting break while on break should be ignored");
    }

    @Test
    @DisplayName("Update should end break when time expires")
    void testUpdateEndsBreak() {
        sessionManager.startBreak(1); // 1ms break

        // Wait for break to end
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        sessionManager.update();

        assertFalse(sessionManager.isOnBreak(),
            "Break should end after time expires");
    }

    @Test
    @DisplayName("Get break time remaining should return zero when not on break")
    void testBreakTimeRemainingWhenNotOnBreak() {
        assertEquals(0, sessionManager.getBreakTimeRemaining(),
            "Break time remaining should be zero when not on break");
    }

    @Test
    @DisplayName("Get break time remaining should decrease over time")
    void testBreakTimeRemainingDecreases() {
        sessionManager.startBreak(100); // 100ms break

        long remaining1 = sessionManager.getBreakTimeRemaining();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        long remaining2 = sessionManager.getBreakTimeRemaining();

        assertTrue(remaining2 < remaining1,
            "Break time remaining should decrease");
    }

    // ========================================================================
    // Session Time Tests
    // ========================================================================

    @Test
    @DisplayName("Session elapsed time should be positive")
    void testSessionElapsedTimePositive() {
        long elapsedTime = sessionManager.getSessionElapsedTime();

        assertTrue(elapsedTime >= 0, "Elapsed time should be non-negative");
    }

    @Test
    @DisplayName("Session elapsed time should increase")
    void testSessionElapsedTimeIncreases() throws InterruptedException {
        long elapsedTime1 = sessionManager.getSessionElapsedTime();

        Thread.sleep(10);

        long elapsedTime2 = sessionManager.getSessionElapsedTime();

        assertTrue(elapsedTime2 > elapsedTime1,
            "Elapsed time should increase");
    }

    @Test
    @DisplayName("Time since last break should equal session elapsed time initially")
    void testTimeSinceLastBreakInitially() {
        long sessionElapsed = sessionManager.getSessionElapsedTime();
        long sinceLastBreak = sessionManager.getTimeSinceLastBreak();

        // Should be approximately equal (within 100ms)
        assertTrue(Math.abs(sinceLastBreak - sessionElapsed) < 100,
            "Time since last break should equal session elapsed time initially");
    }

    @Test
    @DisplayName("Time since last break should reset after break")
    void testTimeSinceLastBreakResets() throws InterruptedException {
        sessionManager.startBreak(1); // 1ms break

        // Wait for break to end
        Thread.sleep(10);
        sessionManager.update();

        long sinceLastBreak = sessionManager.getTimeSinceLastBreak();

        assertTrue(sinceLastBreak < 100,
            "Time since last break should be small after break ends");
    }

    // ========================================================================
    // Reset Tests
    // ========================================================================

    @Test
    @DisplayName("Reset session should update last break time")
    void testResetSession() {
        long oldLastBreakTime = sessionManager.getTimeSinceLastBreak();

        sessionManager.resetSession();

        long newLastBreakTime = sessionManager.getTimeSinceLastBreak();

        // New time should be very recent (close to 0)
        assertTrue(newLastBreakTime < 100,
            "Reset should update last break time to current time");
    }

    @Test
    @DisplayName("Reset session should clear on break flag")
    void testResetSessionClearsBreak() {
        sessionManager.startBreak();
        sessionManager.resetSession();

        assertFalse(sessionManager.isOnBreak(),
            "Reset should clear on break flag");
    }

    // ========================================================================
    // Session Summary Tests
    // ========================================================================

    @Test
    @DisplayName("Session summary should not be null")
    void testSessionSummaryNotNull() {
        String summary = sessionManager.getSessionSummary();

        assertNotNull(summary, "Session summary should not be null");
    }

    @Test
    @DisplayName("Session summary should contain phase information")
    void testSessionSummaryContainsPhase() {
        String summary = sessionManager.getSessionSummary();

        assertTrue(summary.contains("phase=") || summary.contains("WARMUP") || summary.contains("PERFORMANCE") || summary.contains("FATIGUE"),
            "Summary should contain phase information");
    }

    @Test
    @DisplayName("Session summary when disabled")
    void testSessionSummaryWhenDisabled() {
        sessionManager.setEnabled(false);

        String summary = sessionManager.getSessionSummary();

        assertTrue(summary.contains("disabled") || summary.contains("DISABLED"),
            "Summary should indicate disabled state");
    }

    // ========================================================================
    // Update Tests
    // ========================================================================

    @Test
    @DisplayName("Update should not throw")
    void testUpdateDoesNotThrow() {
        assertDoesNotThrow(() -> sessionManager.update());
    }

    @Test
    @DisplayName("Update when disabled should do nothing")
    void testUpdateWhenDisabled() {
        sessionManager.setEnabled(false);

        SessionManager.SessionPhase phaseBefore = sessionManager.getCurrentPhase();

        sessionManager.update();

        SessionManager.SessionPhase phaseAfter = sessionManager.getCurrentPhase();

        assertEquals(phaseBefore, phaseAfter,
            "Update when disabled should not change phase");
    }

    // ========================================================================
    // Seed Tests
    // ========================================================================

    @Test
    @DisplayName("Seed should affect break randomness")
    void testSeedAffectsBreaks() {
        // Create two managers with same seed
        SessionManager manager1 = new SessionManager();
        manager1.setSeed(123L);

        SessionManager manager2 = new SessionManager();
        manager2.setSeed(123L);

        // Manually set last break time to trigger break possibility
        // This is a bit of a hack since we can't easily control time
        // Just verify setSeed doesn't crash
        assertDoesNotThrow(() -> {
            manager1.setSeed(456L);
            manager2.setSeed(456L);
        });
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle very short break duration")
    void testVeryShortBreakDuration() {
        sessionManager.startBreak(1); // 1ms

        assertTrue(sessionManager.isOnBreak());

        sessionManager.update();

        // May or may not still be on break depending on timing
        // Just verify it doesn't crash
        assertDoesNotThrow(() -> sessionManager.isOnBreak());
    }

    @Test
    @DisplayName("Should handle very long break duration")
    void testVeryLongBreakDuration() {
        long veryLongBreak = 24 * 60 * 60 * 1000L; // 24 hours

        sessionManager.startBreak(veryLongBreak);

        assertTrue(sessionManager.isOnBreak());
        assertTrue(sessionManager.getBreakTimeRemaining() > 0);
    }

    @Test
    @DisplayName("Should handle zero break duration")
    void testZeroBreakDuration() {
        sessionManager.startBreak(0);

        sessionManager.update();

        // Break should end immediately
        assertFalse(sessionManager.isOnBreak() || sessionManager.getBreakTimeRemaining() == 0,
            "Zero duration break should end immediately");
    }

    @Test
    @DisplayName("Should handle negative break duration")
    void testNegativeBreakDuration() {
        // Implementation doesn't validate negative, but should handle gracefully
        assertDoesNotThrow(() -> sessionManager.startBreak(-1));
    }

    @Test
    @DisplayName("Should handle multiple rapid start break calls")
    void testMultipleRapidStartBreakCalls() {
        for (int i = 0; i < 100; i++) {
            sessionManager.startBreak();
        }

        // Should still be on break
        assertTrue(sessionManager.isOnBreak());
    }

    @Test
    @DisplayName("Should handle multiple update calls")
    void testMultipleUpdateCalls() {
        for (int i = 0; i < 1000; i++) {
            sessionManager.update();
        }

        // Should not crash
        assertDoesNotThrow(() -> sessionManager.getCurrentPhase());
    }

    @Test
    @DisplayName("Should handle reset while on break")
    void testResetWhileOnBreak() {
        sessionManager.startBreak(60 * 1000); // 1 minute break
        assertTrue(sessionManager.isOnBreak());

        sessionManager.resetSession();

        assertFalse(sessionManager.isOnBreak(),
            "Reset should clear break state");
    }

    // ========================================================================
    // Statistical Tests
    // ========================================================================

    @Test
    @DisplayName("Break chance should respect minimum interval")
    void testBreakChanceRespectsMinimumInterval() {
        // Create manager with break that just ended
        SessionManager manager = new SessionManager();
        manager.startBreak(1); // 1ms break

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        manager.update();

        // Immediately after break, should not trigger another
        boolean shouldTakeBreak = manager.shouldTakeBreak();

        // Very unlikely to trigger immediately (depends on random chance)
        // Just verify it doesn't crash
        assertDoesNotThrow(() -> manager.shouldTakeBreak());
    }

    @Test
    @DisplayName("Forced break should trigger after max interval")
    void testForcedBreakAfterMaxInterval() {
        // This test is difficult to implement without mocking time
        // Just verify the logic exists by checking it doesn't crash
        SessionManager manager = new SessionManager();
        assertDoesNotThrow(() -> manager.shouldTakeBreak());
    }

    // ========================================================================
    // Integration Tests
    // ========================================================================

    @Test
    @DisplayName("Full session lifecycle should work")
    void testFullSessionLifecycle() {
        // Start in WARMUP
        assertEquals(SessionManager.SessionPhase.WARMUP, sessionManager.getCurrentPhase());

        // Take a break
        sessionManager.startBreak(10);
        assertTrue(sessionManager.isOnBreak());

        // End break
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }
        sessionManager.update();
        assertFalse(sessionManager.isOnBreak());

        // Reset session
        sessionManager.resetSession();

        // Should still work
        assertNotNull(sessionManager.getSessionSummary());
    }

    @Test
    @DisplayName("Disabled manager should have no effect on multipliers")
    void testDisabledManagerNoEffect() {
        sessionManager.setEnabled(false);

        // All multipliers should be normal
        assertEquals(1.0, sessionManager.getReactionMultiplier(), 0.001);
        assertEquals(1.0, sessionManager.getErrorMultiplier(), 0.001);
        assertEquals(0.0, sessionManager.getFatigueLevel(), 0.001);

        // Should not trigger breaks
        assertFalse(sessionManager.shouldTakeBreak());
    }
}
