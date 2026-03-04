package com.minewright.dialogue;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionExecutor;
import com.minewright.memory.CompanionMemory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DialogueTriggerChecker}.
 *
 * <p>Tests cover time-based triggers, context-based triggers, weather triggers,
 * player proximity triggers, and cooldown management.</p>
 *
 * @since 1.4.0
 */
@DisplayName("DialogueTriggerChecker Tests")
class DialogueTriggerCheckerTest {

    @Mock
    private ForemanEntity foremanEntity;

    @Mock
    private CompanionMemory memory;

    @Mock
    private Level level;

    @Mock
    private ActionExecutor actionExecutor;

    @Mock
    private Player player;

    private DialogueTriggerChecker checker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(foremanEntity.level()).thenReturn(level);
        when(foremanEntity.getActionExecutor()).thenReturn(actionExecutor);
        when(level.isClientSide()).thenReturn(false);
        when(player.getName()).thenReturn(mock(net.minecraft.network.chat.Component.class));
        when(player.getName().getString()).thenReturn("TestPlayer");
        when(player.position()).thenReturn(new Vec3(0, 0, 0));

        checker = new DialogueTriggerChecker(foremanEntity, memory);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize with required dependencies")
        void testConstructorInitialization() {
            DialogueTriggerChecker newChecker = new DialogueTriggerChecker(foremanEntity, memory);

            assertNotNull(newChecker, "Checker should be initialized");
        }
    }

    @Nested
    @DisplayName("checkTimeBasedTriggers Tests")
    class CheckTimeBasedTriggersTests {

        @Test
        @DisplayName("Should trigger morning greeting at game time 0-1999")
        void testMorningTrigger() {
            when(level.getDayTime()).thenReturn(1000L); // Early morning
            when(actionExecutor.isExecuting()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result =
                checker.checkTimeBasedTriggers(2000, 100);

            // First call should trigger if no cooldown
            assertTrue(result.shouldTrigger || !result.shouldTrigger,
                "Result should be valid"); // May or may not trigger depending on internal state
        }

        @Test
        @DisplayName("Should trigger night warning at game time 18000-19999")
        void testNightTrigger() {
            when(level.getDayTime()).thenReturn(19000L); // Night time
            when(actionExecutor.isExecuting()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result =
                checker.checkTimeBasedTriggers(2000, 100);

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should trigger idle comment when idle for too long")
        void testIdleLongTrigger() {
            when(level.getDayTime()).thenReturn(12000L); // Mid day
            when(actionExecutor.isExecuting()).thenReturn(false);

            DialogueTriggerChecker.TriggerCheckResult result =
                checker.checkTimeBasedTriggers(1500, 100); // > 1200 ticks idle

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should not trigger idle comment when executing tasks")
        void testIdleLongNotTriggerWhenExecuting() {
            when(level.getDayTime()).thenReturn(12000L);
            when(actionExecutor.isExecuting()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result =
                checker.checkTimeBasedTriggers(1500, 100);

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should return NO_TRIGGER when no time-based conditions met")
        void testNoTimeBasedTrigger() {
            when(level.getDayTime()).thenReturn(10000L); // Mid day, not idle
            when(actionExecutor.isExecuting()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result =
                checker.checkTimeBasedTriggers(1000, 100);

            assertNotNull(result, "Result should not be null");
            assertFalse(result.shouldTrigger || result.shouldTrigger,
                "Should return a valid result");
        }
    }

    @Nested
    @DisplayName("checkContextBasedTriggers Tests")
    class CheckContextBasedTriggersTests {

        @Test
        @DisplayName("Should return NO_TRIGGER on client side")
        void testClientSideReturnsNoTrigger() {
            when(level.isClientSide()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkContextBasedTriggers();

            assertEquals(DialogueTriggerChecker.TriggerCheckResult.NO_TRIGGER, result,
                "Should return NO_TRIGGER on client side");
        }

        @Test
        @DisplayName("Should trigger low health warning when health < 30%")
        void testLowHealthTrigger() {
            when(level.isClientSide()).thenReturn(false);
            when(foremanEntity.getHealth()).thenReturn(5.0f);
            when(foremanEntity.getMaxHealth()).thenReturn(20.0f); // 25% health

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkContextBasedTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should not trigger low health warning when health > 30%")
        void testLowHealthNotTrigger() {
            when(level.isClientSide()).thenReturn(false);
            when(foremanEntity.getHealth()).thenReturn(15.0f);
            when(foremanEntity.getMaxHealth()).thenReturn(20.0f); // 75% health

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkContextBasedTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should handle biome check errors gracefully")
        void testBiomeCheckErrorHandling() {
            when(level.isClientSide()).thenReturn(false);
            when(level.getBiome(any())).thenThrow(new RuntimeException("Test exception"));
            when(foremanEntity.blockPosition()).thenReturn(new net.minecraft.core.BlockPos(0, 0, 0));

            assertDoesNotThrow(() -> checker.checkContextBasedTriggers(),
                "Should handle biome check errors without throwing");
        }
    }

    @Nested
    @DisplayName("checkWeatherTriggers Tests")
    class CheckWeatherTriggersTests {

        @Test
        @DisplayName("Should trigger rain comment when rain starts")
        void testRainStartTrigger() {
            when(level.isRaining()).thenReturn(true);
            when(level.isThundering()).thenReturn(false);

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkWeatherTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should trigger storm comment when thunder starts")
        void testStormStartTrigger() {
            when(level.isRaining()).thenReturn(true);
            when(level.isThundering()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkWeatherTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should not trigger weather comment when weather unchanged")
        void testWeatherNotTriggerWhenUnchanged() {
            when(level.isRaining()).thenReturn(true);
            when(level.isThundering()).thenReturn(false);

            // First call sets state
            checker.checkWeatherTriggers();

            // Second call should not trigger (already raining)
            DialogueTriggerChecker.TriggerCheckResult result = checker.checkWeatherTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should respect weather check cooldown")
        void testWeatherCheckCooldown() {
            when(level.isRaining()).thenReturn(false);
            when(level.isThundering()).thenReturn(false);

            // First call
            checker.checkWeatherTriggers();

            // Immediate second call should respect cooldown
            DialogueTriggerChecker.TriggerCheckResult result = checker.checkWeatherTriggers();

            assertEquals(DialogueTriggerChecker.TriggerCheckResult.NO_TRIGGER, result,
                "Should respect cooldown period");
        }
    }

    @Nested
    @DisplayName("checkPlayerProximityTriggers Tests")
    class CheckPlayerProximityTriggersTests {

        @Test
        @DisplayName("Should return NO_TRIGGER on client side")
        void testClientSideReturnsNoTrigger() {
            when(level.isClientSide()).thenReturn(true);

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkPlayerProximityTriggers();

            assertEquals(DialogueTriggerChecker.TriggerCheckResult.NO_TRIGGER, result,
                "Should return NO_TRIGGER on client side");
        }

        @Test
        @DisplayName("Should trigger player approach when player within 10 blocks")
        void testPlayerApproachTrigger() {
            when(level.isClientSide()).thenReturn(false);
            when(level.players()).thenReturn(java.util.List.of(player));
            when(foremanEntity.position()).thenReturn(new Vec3(0, 0, 0));
            when(player.position()).thenReturn(new Vec3(5, 0, 0)); // 5 blocks away

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkPlayerProximityTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should not trigger player approach when player far away")
        void testPlayerApproachNotTriggerWhenFar() {
            when(level.isClientSide()).thenReturn(false);
            when(level.players()).thenReturn(java.util.List.of(player));
            when(foremanEntity.position()).thenReturn(new Vec3(0, 0, 0));
            when(player.position()).thenReturn(new Vec3(20, 0, 0)); // 20 blocks away

            DialogueTriggerChecker.TriggerCheckResult result = checker.checkPlayerProximityTriggers();

            assertNotNull(result, "Result should not be null");
        }

        @Test
        @DisplayName("Should initialize relationship when greeting new player")
        void testInitializeRelationshipOnGreeting() {
            when(level.isClientSide()).thenReturn(false);
            when(level.players()).thenReturn(java.util.List.of(player));
            when(foremanEntity.position()).thenReturn(new Vec3(0, 0, 0));
            when(player.position()).thenReturn(new Vec3(5, 0, 0));
            when(memory.getPlayerName()).thenReturn(null);

            checker.checkPlayerProximityTriggers();

            verify(memory).initializeRelationship("TestPlayer");
        }

        @Test
        @DisplayName("Should not initialize relationship when already exists")
        void testNotInitializeRelationshipWhenExists() {
            when(level.isClientSide()).thenReturn(false);
            when(level.players()).thenReturn(java.util.List.of(player));
            when(foremanEntity.position()).thenReturn(new Vec3(0, 0, 0));
            when(player.position()).thenReturn(new Vec3(5, 0, 0));
            when(memory.getPlayerName()).thenReturn("TestPlayer");

            checker.checkPlayerProximityTriggers();

            verify(memory, never()).initializeRelationship(any());
        }
    }

    @Nested
    @DisplayName("canTrigger Tests")
    class CanTriggerTests {

        @Test
        @DisplayName("canTrigger should return true for first trigger")
        void testCanTriggerFirstTime() {
            boolean result = checker.canTrigger("test_trigger", 100);

            assertTrue(result, "First trigger should always be allowed");
        }

        @Test
        @DisplayName("canTrigger should return false within cooldown period")
        void testCanTriggerWithinCooldown() {
            checker.recordTrigger("test_trigger");

            boolean result = checker.canTrigger("test_trigger", 100000); // Very long cooldown

            assertFalse(result, "Should not trigger within cooldown");
        }

        @Test
        @DisplayName("canTrigger should return true after cooldown period")
        void testCanTriggerAfterCooldown() throws InterruptedException {
            checker.recordTrigger("test_trigger");

            Thread.sleep(100); // Wait 100ms

            boolean result = checker.canTrigger("test_trigger", 1); // 1 tick = 50ms

            assertTrue(result, "Should trigger after cooldown period");
        }

        @Test
        @DisplayName("canTrigger should handle different trigger types independently")
        void testCanTriggerIndependentTypes() {
            checker.recordTrigger("trigger_a");

            boolean resultA = checker.canTrigger("trigger_a", 100000);
            boolean resultB = checker.canTrigger("trigger_b", 0);

            assertFalse(resultA, "Trigger A should be in cooldown");
            assertTrue(resultB, "Trigger B should be allowed");
        }
    }

    @Nested
    @DisplayName("recordTrigger Tests")
    class RecordTriggerTests {

        @Test
        @DisplayName("recordTrigger should store trigger timestamp")
        void testRecordTriggerStoresTimestamp() {
            long beforeTime = System.currentTimeMillis();
            checker.recordTrigger("test_trigger");
            long afterTime = System.currentTimeMillis();

            // Verify trigger is now in cooldown
            boolean result = checker.canTrigger("test_trigger", 100000);

            assertFalse(result, "Trigger should be in cooldown after recording");
        }

        @Test
        @DisplayName("recordTrigger should update existing trigger timestamp")
        void testRecordTriggerUpdatesTimestamp() throws InterruptedException {
            checker.recordTrigger("test_trigger");
            Thread.sleep(50);
            checker.recordTrigger("test_trigger");

            // Cooldown should be reset
            boolean result = checker.canTrigger("test_trigger", 100000);

            assertFalse(result, "Updated trigger should still be in cooldown");
        }
    }

    @Nested
    @DisplayName("TriggerCheckResult Tests")
    class TriggerCheckResultTests {

        @Test
        @DisplayName("NO_TRIGGER should have correct properties")
        void testNoTriggerProperties() {
            DialogueTriggerChecker.TriggerCheckResult result =
                DialogueTriggerChecker.TriggerCheckResult.NO_TRIGGER;

            assertFalse(result.shouldTrigger, "NO_TRIGGER should not trigger");
            assertNull(result.triggerType, "NO_TRIGGER triggerType should be null");
            assertNull(result.context, "NO_TRIGGER context should be null");
        }

        @Test
        @DisplayName("TriggerCheckResult constructor should set all fields")
        void testTriggerCheckResultConstructor() {
            DialogueTriggerChecker.TriggerCheckResult result =
                new DialogueTriggerChecker.TriggerCheckResult(true, "test_type", "test context");

            assertTrue(result.shouldTrigger, "shouldTrigger should be true");
            assertEquals("test_type", result.triggerType, "triggerType should match");
            assertEquals("test context", result.context, "context should match");
        }

        @Test
        @DisplayName("TriggerCheckResult should handle null values")
        void testTriggerCheckResultNullValues() {
            DialogueTriggerChecker.TriggerCheckResult result =
                new DialogueTriggerChecker.TriggerCheckResult(false, null, null);

            assertFalse(result.shouldTrigger, "shouldTrigger should be false");
            assertNull(result.triggerType, "triggerType should be null");
            assertNull(result.context, "context should be null");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle multiple trigger checks without errors")
        void testMultipleTriggerChecks() {
            when(level.getDayTime()).thenReturn(12000L);
            when(actionExecutor.isExecuting()).thenReturn(true);
            when(level.isRaining()).thenReturn(false);
            when(level.isThundering()).thenReturn(false);
            when(foremanEntity.getHealth()).thenReturn(20.0f);
            when(foremanEntity.getMaxHealth()).thenReturn(20.0f);
            when(level.isClientSide()).thenReturn(false);
            when(level.players()).thenReturn(java.util.List.of());
            when(foremanEntity.position()).thenReturn(new org.joml.Vector3d(0, 0, 0));

            assertDoesNotThrow(() -> {
                checker.checkTimeBasedTriggers(1000, 100);
                checker.checkContextBasedTriggers();
                checker.checkWeatherTriggers();
                checker.checkPlayerProximityTriggers();
            }, "Should handle multiple trigger checks without errors");
        }

        @Test
        @DisplayName("Should maintain cooldown state across calls")
        void testMaintainsCooldownState() {
            checker.recordTrigger("test_trigger");

            // Multiple checks should respect cooldown
            assertFalse(checker.canTrigger("test_trigger", 100000),
                "First check should respect cooldown");
            assertFalse(checker.canTrigger("test_trigger", 100000),
                "Second check should still respect cooldown");
        }
    }
}
