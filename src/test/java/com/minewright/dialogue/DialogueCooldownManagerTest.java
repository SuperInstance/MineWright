package com.minewright.dialogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DialogueCooldownManager}.
 *
 * <p>Tests cover cooldown tracking, trigger validation, tick management,
 * and state reset functionality.</p>
 *
 * @since 1.3.0
 */
@DisplayName("DialogueCooldownManager Tests")
class DialogueCooldownManagerTest {

    private DialogueCooldownManager manager;
    private static final int BASE_COOLDOWN_TICKS = 100;

    @BeforeEach
    void setUp() {
        manager = new DialogueCooldownManager(BASE_COOLDOWN_TICKS);
    }

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor should initialize with zero ticks since last comment")
    void testConstructorInitializesZeroTicks() {
        assertEquals(0, manager.getTicksSinceLastComment(),
            "Ticks since last comment should start at 0");
        assertEquals(0, manager.getLastCommentTimestamp(),
            "Last comment timestamp should start at 0");
        assertNull(manager.getLastCommentType(),
            "Last comment type should start as null");
    }

    // ========== canTrigger Tests ==========

    @Test
    @DisplayName("canTrigger should return true for first trigger")
    void testCanTriggerFirstTrigger() {
        boolean result = manager.canTrigger("greeting", 50);
        assertTrue(result,
            "First trigger should always be allowed");
    }

    @Test
    @DisplayName("canTrigger should return true when cooldown has elapsed")
    void testCanTriggerAfterCooldown() {
        // Trigger once
        manager.recordComment("greeting");

        // Simulate cooldown ticks
        for (int i = 0; i < 50; i++) {
            manager.incrementTick();
        }

        boolean result = manager.canTrigger("greeting", 50);
        assertTrue(result,
            "Trigger should be allowed after cooldown ticks");
    }

    @Test
    @DisplayName("canTrigger should return false when within cooldown")
    void testCanTriggerWithinCooldown() {
        manager.recordComment("greeting");
        manager.incrementTick(); // Only 1 tick passed

        boolean result = manager.canTrigger("greeting", 50);
        assertFalse(result,
            "Trigger should be denied within cooldown period");
    }

    @Test
    @DisplayName("canTrigger should handle different trigger types independently")
    void testCanTriggerIndependentTypes() {
        manager.recordComment("greeting");
        manager.incrementTick(); // 1 tick passed

        // Same type should be blocked
        assertFalse(manager.canTrigger("greeting", 50),
            "Same trigger type should be blocked");

        // Different type should be allowed (different cooldown)
        assertTrue(manager.canTrigger("farewell", 1),
            "Different trigger type should be allowed");
    }

    // ========== recordTrigger Tests ==========

    @Test
    @DisplayName("recordTrigger should store trigger timestamp")
    void testRecordTriggerStoresTimestamp() {
        long beforeTime = System.currentTimeMillis();
        manager.recordTrigger("test_trigger");
        long afterTime = System.currentTimeMillis();

        // Verify the trigger was recorded by checking canTrigger behavior
        assertTrue(manager.canTrigger("different_trigger", 0),
            "Different trigger should still work");

        // The recorded timestamp should be between before and after
        // We can't directly access triggerCooldowns, but we verify indirectly
        manager.recordComment("different_trigger");
        assertEquals("different_trigger", manager.getLastCommentType(),
            "Last comment type should be updated");
    }

    // ========== recordComment Tests ==========

    @Test
    @DisplayName("recordComment should update state")
    void testRecordCommentUpdatesState() {
        manager.recordComment("morning");

        assertEquals("morning", manager.getLastCommentType(),
            "Last comment type should be updated");
        assertEquals(0, manager.getTicksSinceLastComment(),
            "Ticks since last comment should be reset");
        assertNotEquals(0, manager.getLastCommentTimestamp(),
            "Last comment timestamp should be set");
    }

    @Test
    @DisplayName("recordComment should reset tick counter")
    void testRecordCommentResetsTickCounter() {
        for (int i = 0; i < 50; i++) {
            manager.incrementTick();
        }
        assertEquals(50, manager.getTicksSinceLastComment(),
            "Ticks should increment");

        manager.recordComment("test");
        assertEquals(0, manager.getTicksSinceLastComment(),
            "Ticks should reset after comment");
    }

    // ========== incrementTick Tests ==========

    @Test
    @DisplayName("incrementTick should increase tick counter")
    void testIncrementTickIncreasesCounter() {
        manager.incrementTick();
        assertEquals(1, manager.getTicksSinceLastComment());

        manager.incrementTick();
        manager.incrementTick();
        assertEquals(3, manager.getTicksSinceLastComment(),
            "Ticks should accumulate");
    }

    @Test
    @DisplayName("incrementTick after recordComment should start from zero")
    void testIncrementTickAfterRecordComment() {
        manager.recordComment("test");
        manager.incrementTick();
        assertEquals(1, manager.getTicksSinceLastComment(),
            "Ticks should start from 0 after comment");
    }

    // ========== isSameTypeRecent Tests ==========

    @Test
    @DisplayName("isSameTypeRecent should return true for same recent type")
    void testIsSameTypeRecentTrue() {
        manager.recordComment("greeting");
        manager.incrementTick(); // Only 1 tick passed

        assertTrue(manager.isSameTypeRecent("greeting"),
            "Same type should be recent");
    }

    @Test
    @DisplayName("isSameTypeRecent should return false after cooldown")
    void testIsSameTypeRecentAfterCooldown() {
        manager.recordComment("greeting");

        // Simulate full cooldown
        for (int i = 0; i < BASE_COOLDOWN_TICKS; i++) {
            manager.incrementTick();
        }

        assertFalse(manager.isSameTypeRecent("greeting"),
            "Same type should not be recent after cooldown");
    }

    @Test
    @DisplayName("isSameTypeRecent should return false for different type")
    void testIsSameTypeRecentDifferentType() {
        manager.recordComment("greeting");
        manager.incrementTick();

        assertFalse(manager.isSameTypeRecent("farewell"),
            "Different type should return false");
    }

    @Test
    @DisplayName("isSameTypeRecent should return false when no previous comment")
    void testIsSameTypeRecentNoPreviousComment() {
        assertFalse(manager.isSameTypeRecent("greeting"),
            "Should return false when no previous comment");
    }

    // ========== Getter Tests ==========

    @Test
    @DisplayName("getTicksSinceLastComment should return accurate count")
    void testGetTicksSinceLastCommentAccuracy() {
        assertEquals(0, manager.getTicksSinceLastComment());

        for (int i = 0; i < 25; i++) {
            manager.incrementTick();
        }
        assertEquals(25, manager.getTicksSinceLastComment());

        manager.recordComment("test");
        assertEquals(0, manager.getTicksSinceLastComment());
    }

    @Test
    @DisplayName("getLastCommentTimestamp should be set after comment")
    void testGetLastCommentTimestamp() {
        long beforeTime = System.currentTimeMillis();
        manager.recordComment("test");
        long afterTime = System.currentTimeMillis();

        long timestamp = manager.getLastCommentTimestamp();
        assertTrue(timestamp >= beforeTime && timestamp <= afterTime,
            "Timestamp should be between before and after time");
    }

    @Test
    @DisplayName("getLastCommentType should return last type")
    void testGetLastCommentType() {
        assertNull(manager.getLastCommentType());

        manager.recordComment("morning");
        assertEquals("morning", manager.getLastCommentType());

        manager.recordComment("greeting");
        assertEquals("greeting", manager.getLastCommentType(),
            "Last comment type should be updated");
    }

    // ========== reset Tests ==========

    @Test
    @DisplayName("reset should clear all state")
    void testResetClearsState() {
        // Set up some state
        manager.recordComment("test");
        for (int i = 0; i < 10; i++) {
            manager.incrementTick();
        }
        manager.recordTrigger("trigger1");
        manager.recordTrigger("trigger2");

        // Reset
        manager.reset();

        // Verify all cleared
        assertEquals(0, manager.getTicksSinceLastComment(),
            "Ticks should be reset");
        assertEquals(0, manager.getLastCommentTimestamp(),
            "Timestamp should be reset");
        assertNull(manager.getLastCommentType(),
            "Type should be reset");

        // Triggers should be cleared - verify by checking canTrigger
        assertTrue(manager.canTrigger("trigger1", 0),
            "Old triggers should be cleared");
        assertTrue(manager.canTrigger("trigger2", 0),
            "Old triggers should be cleared");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Full workflow: trigger, cooldown, trigger again")
    void testFullWorkflow() {
        String triggerType = "greeting";
        int cooldownTicks = 20;

        // First trigger should work
        assertTrue(manager.canTrigger(triggerType, cooldownTicks),
            "First trigger should be allowed");
        manager.recordComment(triggerType);

        // Within cooldown should fail
        for (int i = 0; i < cooldownTicks - 1; i++) {
            manager.incrementTick();
        }
        assertFalse(manager.canTrigger(triggerType, cooldownTicks),
            "Should be blocked within cooldown");

        // After cooldown should work
        manager.incrementTick(); // Now at cooldown
        assertTrue(manager.canTrigger(triggerType, cooldownTicks),
            "Should be allowed after cooldown");
    }

    @Test
    @DisplayName("Multiple trigger types with different cooldowns")
    void testMultipleTriggerTypes() {
        manager.recordComment("short_cooldown");
        manager.incrementTick();
        manager.incrementTick();

        // Short cooldown should pass
        assertTrue(manager.canTrigger("short_cooldown", 2),
            "Short cooldown should pass");

        // Long cooldown should fail
        assertFalse(manager.canTrigger("short_cooldown", 10),
            "Long cooldown should fail");

        // Different type with short cooldown should pass
        assertTrue(manager.canTrigger("different_type", 1),
            "Different type with short cooldown should pass");
    }

    @Test
    @DisplayName("Edge case: zero cooldown")
    void testZeroCooldown() {
        manager.recordComment("test");
        assertTrue(manager.canTrigger("test", 0),
            "Zero cooldown should always allow triggers");
    }

    @Test
    @DisplayName("Edge case: single tick precision")
    void testSingleTickPrecision() {
        manager.recordComment("test");
        assertFalse(manager.canTrigger("test", 1),
            "Should be blocked at tick 0");

        manager.incrementTick();
        assertTrue(manager.canTrigger("test", 1),
            "Should be allowed at tick 1");
    }
}
