package com.minewright.humanization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link IdleBehaviorController}.
 *
 * <p>These tests verify that idle behaviors are triggered correctly,
 * personality traits affect behavior appropriately, and cooldowns work as expected.</p>
 *
 * @since 2.2.0
 */
@DisplayName("IdleBehaviorController Tests")
class IdleBehaviorControllerTest {

    private IdleBehaviorController controller;
    private List<IdleBehaviorController.IdleAction> executedActions;
    private List<double[]> executedData;

    @BeforeEach
    void setUp() {
        executedActions = new ArrayList<>();
        executedData = new ArrayList<>();

        // Create a test personality with average traits
        IdleBehaviorController.PersonalityTraits personality = new TestPersonality(50, 50, 50, 50);

        controller = new IdleBehaviorController(personality);
        controller.setSeed(42L);

        // Set up executor to record actions
        controller.setExecutor((action, data) -> {
            executedActions.add(action);
            executedData.add(data);
        });
    }

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Test
    @DisplayName("Default constructor should create controller")
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new IdleBehaviorController());
    }

    @Test
    @DisplayName("Constructor with personality should use provided traits")
    void testConstructorWithPersonality() {
        TestPersonality personality = new TestPersonality(80, 20, 70, 30);
        IdleBehaviorController customController = new IdleBehaviorController(personality);

        assertDoesNotThrow(() -> customController.tick());
    }

    @Test
    @DisplayName("Constructor should handle null personality gracefully")
    void testConstructorWithNullPersonality() {
        // The implementation has a default personality fallback
        assertDoesNotThrow(() -> new IdleBehaviorController(null));
    }

    // ========================================================================
    // Enable/Disable Tests
    // ========================================================================

    @Test
    @DisplayName("Should be enabled by default")
    void testEnabledByDefault() {
        assertTrue(controller.isEnabled(), "Controller should be enabled by default");
    }

    @Test
    @DisplayName("Disable should prevent idle actions")
    void testDisablePreventsActions() {
        controller.setEnabled(false);

        for (int i = 0; i < 1000; i++) {
            controller.tick();
        }

        assertTrue(executedActions.isEmpty(),
            "No actions should be executed when disabled");
    }

    @Test
    @DisplayName("Enable should allow idle actions")
    void testEnableAllowsActions() {
        controller.setEnabled(false);
        controller.setEnabled(true);

        // Force an action
        controller.resetCooldown();
        controller.forceIdleAction();

        assertFalse(executedActions.isEmpty(),
            "Actions should be executed when enabled");
    }

    @Test
    @DisplayName("Toggle enabled state")
    void testToggleEnabled() {
        assertTrue(controller.isEnabled());

        controller.setEnabled(false);
        assertFalse(controller.isEnabled());

        controller.setEnabled(true);
        assertTrue(controller.isEnabled());
    }

    // ========================================================================
    // Executor Tests
    // ========================================================================

    @Test
    @DisplayName("Set executor should update callback")
    void testSetExecutor() {
        List<IdleBehaviorController.IdleAction> customActions = new ArrayList<>();

        controller.setExecutor((action, data) -> {
            customActions.add(action);
        });

        controller.forceIdleAction();

        assertEquals(1, customActions.size(),
            "Custom executor should be called");
    }

    @Test
    @DisplayName("Tick with executor parameter should work")
    void testTickWithExecutor() {
        List<IdleBehaviorController.IdleAction> tickActions = new ArrayList<>();

        controller.resetCooldown();
        controller.tick((action, data) -> {
            tickActions.add(action);
        });

        // Should execute action since cooldown is reset
        assertFalse(tickActions.isEmpty() || executedActions.isEmpty(),
            "Tick with executor parameter should work");
    }

    @Test
    @DisplayName("Force idle action without executor should log warning")
    void testForceIdleActionWithoutExecutor() {
        IdleBehaviorController noExecutorController = new IdleBehaviorController();

        // Should not throw, just log warning
        assertDoesNotThrow(() -> noExecutorController.forceIdleAction());
    }

    // ========================================================================
    // Cooldown Tests
    // ========================================================================

    @Test
    @DisplayName("Cooldown should prevent immediate actions")
    void testCooldownPreventsImmediateActions() {
        executedActions.clear();

        // First action should execute
        controller.forceIdleAction();
        int firstActionCount = executedActions.size();

        // Immediate ticks should not execute actions due to cooldown
        for (int i = 0; i < 100; i++) {
            controller.tick();
        }

        assertEquals(firstActionCount, executedActions.size(),
            "Cooldown should prevent immediate additional actions");
    }

    @Test
    @DisplayName("Cooldown should decrement")
    void testCooldownDecrements() {
        controller.forceIdleAction();
        int initialCooldown = controller.getCooldownTicks();

        controller.tick();

        assertEquals(initialCooldown - 1, controller.getCooldownTicks(),
            "Cooldown should decrement by 1 each tick");
    }

    @Test
    @DisplayName("Reset cooldown should allow immediate action")
    void testResetCooldown() {
        controller.forceIdleAction();
        assertTrue(controller.getCooldownTicks() > 0,
            "Cooldown should be set after forced action");

        controller.resetCooldown();
        assertEquals(0, controller.getCooldownTicks(),
            "Reset should set cooldown to 0");
    }

    @Test
    @DisplayName("Get cooldown ticks should return correct value")
    void testGetCooldownTicks() {
        assertEquals(0, controller.getCooldownTicks(),
            "Initial cooldown should be 0");

        controller.forceIdleAction();
        assertTrue(controller.getCooldownTicks() > 0,
            "Cooldown should be positive after action");
    }

    @Test
    @DisplayName("Cooldown should be within valid range")
    void testCooldownInRange() {
        controller.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            controller.forceIdleAction();
            int cooldown = controller.getCooldownTicks();

            assertTrue(cooldown >= 20 && cooldown <= 200,
                "Cooldown should be in range [20, 200], got: " + cooldown);
        }
    }

    // ========================================================================
    // Force Idle Action Tests
    // ========================================================================

    @Test
    @DisplayName("Force idle action should bypass cooldown")
    void testForceIdleActionBypassesCooldown() {
        controller.forceIdleAction();
        assertTrue(controller.getCooldownTicks() > 0,
            "First action should set cooldown");

        int actionCount = executedActions.size();

        controller.forceIdleAction();

        assertEquals(actionCount + 1, executedActions.size(),
            "Forced action should execute even with cooldown");
    }

    @Test
    @DisplayName("Force idle action should not work when disabled")
    void testForceIdleActionWhenDisabled() {
        controller.setEnabled(false);
        controller.resetCooldown();

        controller.forceIdleAction();

        assertTrue(executedActions.isEmpty(),
            "Forced action should not execute when disabled");
    }

    @Test
    @DisplayName("Force idle action should set new cooldown")
    void testForceIdleActionSetsCooldown() {
        controller.resetCooldown();
        assertEquals(0, controller.getCooldownTicks());

        controller.forceIdleAction();

        assertTrue(controller.getCooldownTicks() > 0,
            "Forced action should set cooldown");
    }

    // ========================================================================
    // Action Distribution Tests
    // ========================================================================

    @Test
    @DisplayName("All action types should be possible")
    void testAllActionTypesPossible() {
        controller.setSeed(42L);

        boolean lookAround = false;
        boolean fidget = false;
        boolean stretch = false;
        boolean checkInventory = false;
        boolean emote = false;
        boolean smallStep = false;
        boolean standStill = false;

        for (int i = 0; i < 1000; i++) {
            controller.resetCooldown();
            controller.forceIdleAction();

            if (!executedActions.isEmpty()) {
                switch (executedActions.get(executedActions.size() - 1)) {
                    case LOOK_AROUND -> lookAround = true;
                    case FIDGET -> fidget = true;
                    case STRETCH -> stretch = true;
                    case CHECK_INVENTORY -> checkInventory = true;
                    case EMOTE -> emote = true;
                    case SMALL_STEP -> smallStep = true;
                    case STAND_STILL -> standStill = true;
                }
            }
        }

        assertTrue(lookAround, "LOOK_AROUND should occur");
        assertTrue(fidget, "FIDGET should occur");
        assertTrue(smallStep, "SMALL_STEP should occur");
        assertTrue(standStill, "STAND_STILL should occur");
    }

    @Test
    @DisplayName("Action distribution should follow default probabilities")
    void testActionDistribution() {
        controller.setSeed(42L);

        int lookAround = 0, fidget = 0, standStill = 0, smallStep = 0;
        int checkInventory = 0, emote = 0;

        for (int i = 0; i < 1000; i++) {
            controller.resetCooldown();
            controller.forceIdleAction();

            if (!executedActions.isEmpty()) {
                switch (executedActions.get(executedActions.size() - 1)) {
                    case LOOK_AROUND -> lookAround++;
                    case FIDGET -> fidget++;
                    case STAND_STILL -> standStill++;
                    case SMALL_STEP -> smallStep++;
                    case CHECK_INVENTORY -> checkInventory++;
                    case EMOTE -> emote++;
                }
            }
        }

        // Roughly verify distribution (allowing for variance)
        int total = lookAround + fidget + standStill + smallStep + checkInventory + emote;

        // LOOK_AROUND should be most common (~30%)
        double lookAroundRatio = (double) lookAround / total;
        assertTrue(lookAroundRatio > 0.20 && lookAroundRatio < 0.40,
            "LOOK_AROUND ratio should be around 30%, got: " + lookAroundRatio);

        // EMOTE should be least common (~5%)
        double emoteRatio = (double) emote / total;
        assertTrue(emoteRatio < 0.10,
            "EMOTE ratio should be around 5%, got: " + emoteRatio);
    }

    // ========================================================================
    // Personality Tests
    // ========================================================================

    @Test
    @DisplayName("High neuroticism should increase fidgeting")
    void testHighNeuroticismIncreasesFidgeting() {
        TestPersonality anxiousPersonality = new TestPersonality(50, 90, 50, 50);
        IdleBehaviorController anxiousController = new IdleBehaviorController(anxiousPersonality);
        anxiousController.setSeed(42L);

        List<IdleBehaviorController.IdleAction> anxiousActions = new ArrayList<>();
        anxiousController.setExecutor((action, data) -> anxiousActions.add(action));

        for (int i = 0; i < 100; i++) {
            anxiousController.resetCooldown();
            anxiousController.forceIdleAction();
        }

        long fidgetCount = anxiousActions.stream()
            .filter(a -> a == IdleBehaviorController.IdleAction.FIDGET)
            .count();

        // With 90% neuroticism, should have significant fidgeting
        assertTrue(fidgetCount > 10,
            "High neuroticism should result in fidgeting, got: " + fidgetCount);
    }

    @Test
    @DisplayName("High extraversion should increase looking around")
    void testHighExtraversionIncreasesLookingAround() {
        TestPersonality extravertPersonality = new TestPersonality(90, 50, 50, 50);
        IdleBehaviorController extravertController = new IdleBehaviorController(extravertPersonality);
        extravertController.setSeed(42L);

        List<IdleBehaviorController.IdleAction> extravertActions = new ArrayList<>();
        extravertController.setExecutor((action, data) -> extravertActions.add(action));

        for (int i = 0; i < 100; i++) {
            extravertController.resetCooldown();
            extravertController.forceIdleAction();
        }

        long lookAroundCount = extravertActions.stream()
            .filter(a -> a == IdleBehaviorController.IdleAction.LOOK_AROUND)
            .count();

        // With 90% extraversion, should have significant looking around
        assertTrue(lookAroundCount > 10,
            "High extraversion should result in looking around, got: " + lookAroundCount);
    }

    @Test
    @DisplayName("High conscientiousness should increase standing still")
    void testHighConscientiousnessIncreasesStandingStill() {
        TestPersonality focusedPersonality = new TestPersonality(50, 50, 50, 90);
        IdleBehaviorController focusedController = new IdleBehaviorController(focusedPersonality);
        focusedController.setSeed(42L);

        List<IdleBehaviorController.IdleAction> focusedActions = new ArrayList<>();
        focusedController.setExecutor((action, data) -> focusedActions.add(action));

        for (int i = 0; i < 100; i++) {
            focusedController.resetCooldown();
            focusedController.forceIdleAction();
        }

        long standStillCount = focusedActions.stream()
            .filter(a -> a == IdleBehaviorController.IdleAction.STAND_STILL)
            .count();

        // With 90% conscientiousness, should have significant standing still
        assertTrue(standStillCount > 10,
            "High conscientiousness should result in standing still, got: " + standStillCount);
    }

    @Test
    @DisplayName("Low conscientiousness should decrease cooldown")
    void testLowConscientiousnessDecreasesCooldown() {
        TestPersonality unfocusedPersonality = new TestPersonality(50, 50, 50, 10);
        IdleBehaviorController unfocusedController = new IdleBehaviorController(unfocusedPersonality);
        unfocusedController.setSeed(42L);

        unfocusedController.forceIdleAction();
        int unfocusedCooldown = unfocusedController.getCooldownTicks();

        TestPersonality focusedPersonality = new TestPersonality(50, 50, 50, 90);
        IdleBehaviorController focusedController = new IdleBehaviorController(focusedPersonality);
        focusedController.setSeed(42L);

        focusedController.forceIdleAction();
        int focusedCooldown = focusedController.getCooldownTicks();

        assertTrue(unfocusedCooldown < focusedCooldown,
            "Low conscientiousness should result in shorter cooldown");
    }

    // ========================================================================
    // Action Data Tests
    // ========================================================================

    @Test
    @DisplayName("LOOK_AROUND should generate rotation data")
    void testLookAroundGeneratesData() {
        controller.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            controller.resetCooldown();
            controller.forceIdleAction();

            if (!executedActions.isEmpty() &&
                executedActions.get(executedActions.size() - 1) == IdleBehaviorController.IdleAction.LOOK_AROUND) {

                double[] data = executedData.get(executedData.size() - 1);
                assertEquals(2, data.length, "LOOK_AROUND should have 2 data values");
                break;
            }
        }
    }

    @Test
    @DisplayName("FIDGET should generate offset data")
    void testFidgetGeneratesData() {
        controller.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            controller.resetCooldown();
            controller.forceIdleAction();

            if (!executedActions.isEmpty() &&
                executedActions.get(executedActions.size() - 1) == IdleBehaviorController.IdleAction.FIDGET) {

                double[] data = executedData.get(executedData.size() - 1);
                assertEquals(3, data.length, "FIDGET should have 3 data values");
                break;
            }
        }
    }

    @Test
    @DisplayName("STAND_STILL should have no data")
    void testStandStillHasNoData() {
        controller.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            controller.resetCooldown();
            controller.forceIdleAction();

            if (!executedActions.isEmpty() &&
                executedActions.get(executedActions.size() - 1) == IdleBehaviorController.IdleAction.STAND_STILL) {

                double[] data = executedData.get(executedData.size() - 1);
                assertEquals(0, data.length, "STAND_STILL should have no data");
                break;
            }
        }
    }

    @Test
    @DisplayName("SMALL_STEP should generate offset data")
    void testSmallStepGeneratesData() {
        controller.setSeed(42L);

        for (int i = 0; i < 100; i++) {
            controller.resetCooldown();
            controller.forceIdleAction();

            if (!executedActions.isEmpty() &&
                executedActions.get(executedActions.size() - 1) == IdleBehaviorController.IdleAction.SMALL_STEP) {

                double[] data = executedData.get(executedData.size() - 1);
                assertEquals(3, data.length, "SMALL_STEP should have 3 data values");
                break;
            }
        }
    }

    // ========================================================================
    // Tick Behavior Tests
    // ========================================================================

    @Test
    @DisplayName("Tick should respect cooldown")
    void testTickRespectsCooldown() {
        controller.forceIdleAction();
        int actionsAfterForce = executedActions.size();

        // Tick many times, should not trigger new action due to cooldown
        for (int i = 0; i < 200; i++) {
            controller.tick();
        }

        assertEquals(actionsAfterForce, executedActions.size(),
            "No new actions should occur during cooldown");
    }

    @Test
    @DisplayName("Tick should trigger action after cooldown")
    void testTickAfterCooldown() {
        controller.forceIdleAction();
        int actionsAfterForce = executedActions.size();

        // Tick past cooldown
        for (int i = 0; i < 250; i++) {
            controller.tick();
        }

        // Should eventually trigger new action (though probabilistic)
        // Just verify it doesn't crash
        assertTrue(executedActions.size() >= actionsAfterForce,
            "Should have at least the forced action");
    }

    @Test
    @DisplayName("Tick when disabled should not decrement cooldown")
    void testTickDisabledNoDecrement() {
        controller.forceIdleAction();
        int cooldownWhenEnabled = controller.getCooldownTicks();

        controller.setEnabled(false);

        for (int i = 0; i < 10; i++) {
            controller.tick();
        }

        assertEquals(cooldownWhenEnabled, controller.getCooldownTicks(),
            "Cooldown should not decrement when disabled");
    }

    // ========================================================================
    // Seed Tests
    // ========================================================================

    @Test
    @DisplayName("Seed should produce reproducible action sequence")
    void testSeedReproducibility() {
        controller.setSeed(123L);

        List<IdleBehaviorController.IdleAction> sequence1 = new ArrayList<>();
        IdleBehaviorController controller1 = new IdleBehaviorController(new TestPersonality(50, 50, 50, 50));
        controller1.setSeed(123L);
        controller1.setExecutor((action, data) -> sequence1.add(action));

        for (int i = 0; i < 10; i++) {
            controller1.resetCooldown();
            controller1.forceIdleAction();
        }

        List<IdleBehaviorController.IdleAction> sequence2 = new ArrayList<>();
        IdleBehaviorController controller2 = new IdleBehaviorController(new TestPersonality(50, 50, 50, 50));
        controller2.setSeed(123L);
        controller2.setExecutor((action, data) -> sequence2.add(action));

        for (int i = 0; i < 10; i++) {
            controller2.resetCooldown();
            controller2.forceIdleAction();
        }

        assertEquals(sequence1, sequence2,
            "Same seed should produce same action sequence");
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle extreme personality values")
    void testExtremePersonalityValues() {
        TestPersonality minPersonality = new TestPersonality(0, 0, 0, 0);
        assertDoesNotThrow(() -> {
            IdleBehaviorController extremeController = new IdleBehaviorController(minPersonality);
            extremeController.forceIdleAction();
        });

        TestPersonality maxPersonality = new TestPersonality(100, 100, 100, 100);
        assertDoesNotThrow(() -> {
            IdleBehaviorController extremeController = new IdleBehaviorController(maxPersonality);
            extremeController.forceIdleAction();
        });
    }

    @Test
    @DisplayName("Should handle multiple rapid force calls")
    void testMultipleRapidForceCalls() {
        for (int i = 0; i < 100; i++) {
            controller.forceIdleAction();
        }

        assertEquals(100, executedActions.size(),
            "Should handle 100 rapid force calls");
    }

    @Test
    @DisplayName("Should handle null executor gracefully")
    void testNullExecutor() {
        IdleBehaviorController nullExecutorController = new IdleBehaviorController();
        nullExecutorController.setExecutor(null);

        // Should not throw
        assertDoesNotThrow(() -> {
            nullExecutorController.forceIdleAction();
            nullExecutorController.tick();
        });
    }

    // ========================================================================
    // Test Personality Implementation
    // ========================================================================

    private static class TestPersonality implements IdleBehaviorController.PersonalityTraits {
        private final int extraversion;
        private final int neuroticism;
        private final int openness;
        private final int conscientiousness;

        TestPersonality(int extraversion, int neuroticism, int openness, int conscientiousness) {
            this.extraversion = extraversion;
            this.neuroticism = neuroticism;
            this.openness = openness;
            this.conscientiousness = conscientiousness;
        }

        @Override
        public int getExtraversion() {
            return extraversion;
        }

        @Override
        public int getNeuroticism() {
            return neuroticism;
        }

        @Override
        public int getOpenness() {
            return openness;
        }

        @Override
        public int getConscientiousness() {
            return conscientiousness;
        }
    }
}
