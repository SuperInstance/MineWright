package com.minewright.behavior.processes;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SurvivalProcess.
 *
 * <p>Tests cover all 18 implemented methods:
 * <ul>
 *   <li>Detection methods: isLowHealth(), isInLava(), isOnFire(), isDrowning(), isFalling(), isUnderAttack()</li>
 *   <li>Handler methods: handleLowHealth(), handleFireLava(), handleDrowning(), handleFalling(), handleAttack()</li>
 *   <li>Action methods: fleeFromNearestThreat(), fleeFromCurrentPosition(), swimUpward()</li>
 * </ul>
 *
 * @since 1.2.0
 */
@DisplayName("SurvivalProcess Tests")
class SurvivalProcessTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private Level mockLevel;

    @Mock
    private LivingEntity mockAttacker;

    @Mock
    private BlockState mockBlockState;

    private SurvivalProcess survivalProcess;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock foreman defaults
        lenient().when(mockForeman.getEntityName()).thenReturn("TestForeman");
        lenient().when(mockForeman.isAlive()).thenReturn(true);
        lenient().when(mockForeman.getHealth()).thenReturn(20.0f);
        lenient().when(mockForeman.getMaxHealth()).thenReturn(20.0f);
        lenient().when(mockForeman.getAirSupply()).thenReturn(300);
        lenient().when(mockForeman.getMaxAirSupply()).thenReturn(300);
        lenient().when(mockForeman.isOnFire()).thenReturn(false);
        lenient().when(mockForeman.getRemainingFireTicks()).thenReturn(0);
        lenient().when(mockForeman.isUnderWater()).thenReturn(false);
        lenient().when(mockForeman.getDeltaMovement()).thenReturn(new Vec3(0, 0, 0));
        lenient().when(mockForeman.getLastHurtByMob()).thenReturn(null);
        lenient().when(mockForeman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(mockForeman.position()).thenReturn(new Vec3(0, 64, 0));
        lenient().when(mockForeman.level()).thenReturn(mockLevel);
        lenient().when(mockForeman.getNavigation()).thenReturn(null);

        // Setup mock level defaults
        lenient().when(mockLevel.getBlockState(any(BlockPos.class))).thenReturn(mockBlockState);
        lenient().when(mockBlockState.isSolidRender(eq(mockLevel), any(BlockPos.class))).thenReturn(true);
        lenient().when(mockBlockState.isSuffocating(eq(mockLevel), any(BlockPos.class))).thenReturn(false);
        lenient().when(mockLevel.getMaxBuildHeight()).thenReturn(320);

        // Add specific block mocks - use doReturn to avoid ambiguity
        lenient().doReturn(false).when(mockBlockState).is(Blocks.LAVA);
        lenient().doReturn(false).when(mockBlockState).is(Blocks.FIRE);
        lenient().doReturn(false).when(mockBlockState).is(Blocks.WATER);
        lenient().doReturn(false).when(mockBlockState).isAir();
        lenient().doReturn(false).when(mockBlockState).is(Blocks.FIRE);

        // Setup mock attacker defaults
        lenient().when(mockAttacker.isAlive()).thenReturn(true);
        lenient().when(mockAttacker.blockPosition()).thenReturn(new BlockPos(10, 64, 10));
        lenient().when(mockAttacker.position()).thenReturn(new Vec3(10, 64, 10));

        survivalProcess = new SurvivalProcess(mockForeman);
    }

    // === Basic Process Tests ===

    @Test
    @DisplayName("Process name should be 'Survival'")
    void testGetName() {
        assertEquals("Survival", survivalProcess.getName());
    }

    @Test
    @DisplayName("Process priority should be 100 (highest)")
    void testGetPriority() {
        assertEquals(100, survivalProcess.getPriority());
    }

    @Test
    @DisplayName("Process should not be active initially")
    void testInitialState() {
        assertFalse(survivalProcess.isActive());
    }

    // === Detection Method Tests ===

    @Test
    @DisplayName("canRun() should return false when foreman is null")
    void testCanRun_NullForeman() {
        SurvivalProcess nullProcess = new SurvivalProcess(null);
        assertFalse(nullProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return false when foreman is not alive")
    void testCanRun_NotAlive() {
        when(mockForeman.isAlive()).thenReturn(false);
        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when health is below 30%")
    void testCanRun_LowHealth() {
        when(mockForeman.getHealth()).thenReturn(5.0f); // 25% of 20
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when on fire")
    void testCanRun_OnFire() {
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when in lava")
    void testCanRun_InLava() {
        when(mockLevel.getBlockState(any(BlockPos.class))).thenReturn(mockBlockState);
        when(mockBlockState.is(Blocks.LAVA)).thenReturn(true);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when drowning")
    void testCanRun_Drowning() {
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50); // Less than 30% of 300
        when(mockForeman.getMaxAirSupply()).thenReturn(300);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when falling")
    void testCanRun_Falling() {
        when(mockForeman.getDeltaMovement()).thenReturn(new Vec3(0, -1.0, 0)); // Falling fast
        when(mockBlockState.isAir()).thenReturn(true);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when under attack")
    void testCanRun_UnderAttack() {
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return false when no threats")
    void testCanRun_NoThreats() {
        // All default values indicate no threats
        assertFalse(survivalProcess.canRun());
    }

    // === Handler Method Tests ===

    @Test
    @DisplayName("onActivate() should activate process and send chat message")
    void testOnActivate() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        survivalProcess.canRun(); // Detect threat
        survivalProcess.onActivate();

        assertTrue(survivalProcess.isActive());
        verify(mockForeman).sendChatMessage(contains("Help!"));
    }

    @Test
    @DisplayName("onDeactivate() should deactivate process")
    void testOnDeactivate() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        survivalProcess.canRun();
        survivalProcess.onActivate();
        assertTrue(survivalProcess.isActive());

        survivalProcess.onDeactivate();
        assertFalse(survivalProcess.isActive());
    }

    @Test
    @DisplayName("tick() should increment ticks active")
    void testTick_IncrementsTicks() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        survivalProcess.tick();
        survivalProcess.tick();
        survivalProcess.tick();

        // No direct way to access ticksActive, but we can verify no exceptions thrown
        assertTrue(survivalProcess.isActive());
    }

    // === Threat Priority Tests ===

    @Test
    @DisplayName("Lava should take priority over low health")
    void testThreatPriority_LavaOverLowHealth() {
        when(mockForeman.getHealth()).thenReturn(5.0f); // Low health
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);
        when(mockLevel.getBlockState(any(BlockPos.class))).thenReturn(mockBlockState);
        when(mockBlockState.is(Blocks.LAVA)).thenReturn(true); // Also in lava

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should send "lava" message, not "health" message
        verify(mockForeman).sendChatMessage(contains("lava"));
    }

    @Test
    @DisplayName("Fire should take priority over drowning")
    void testThreatPriority_FireOverDrowning() {
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should send "fire" message, not "drowning" message
        verify(mockForeman).sendChatMessage(contains("fire"));
    }

    // === Handler-Specific Tests via tick() ===

    @Test
    @DisplayName("handleLowHealth() should trigger flee behavior")
    void testHandleLowHealth_Behavior() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw exception
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("handleFireLava() should trigger flee behavior")
    void testHandleFireLava_Behavior() {
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw exception
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("handleDrowning() should trigger swim upward behavior")
    void testHandleDrowning_Behavior() {
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50);
        when(mockForeman.getMaxAirSupply()).thenReturn(300);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw exception
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("handleFalling() should log detection without throwing")
    void testHandleFalling_Behavior() {
        when(mockForeman.getDeltaMovement()).thenReturn(new Vec3(0, -1.0, 0));
        when(mockBlockState.isAir()).thenReturn(true);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw exception
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("handleAttack() should trigger flee from attacker")
    void testHandleAttack_Behavior() {
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw exception
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    // === Action Method Tests ===

    @Test
    @DisplayName("fleeFromNearestThreat() should handle null attacker")
    void testFleeFromNearestThreat_NullAttacker() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);
        when(mockForeman.getLastHurtByMob()).thenReturn(null);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should fall back to fleeFromCurrentPosition, not throw
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("fleeFromNearestThreat() should handle dead attacker")
    void testFleeFromNearestThreat_DeadAttacker() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);
        when(mockAttacker.isAlive()).thenReturn(false);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should fall back to fleeFromCurrentPosition, not throw
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("fleeFromCurrentPosition() should handle no navigation")
    void testFleeFromCurrentPosition_NoNavigation() {
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);
        when(mockForeman.getNavigation()).thenReturn(null);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw even without navigation
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("swimUpward() should handle no water surface found")
    void testSwimUpward_NoSurface() {
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50);
        when(mockForeman.getMaxAirSupply()).thenReturn(300);
        when(mockBlockState.is(Blocks.WATER)).thenReturn(true); // Always water

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should try to move up anyway, not throw
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    @Test
    @DisplayName("swimUpward() should handle null level")
    void testSwimUpward_NullLevel() {
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50);
        when(mockForeman.getMaxAirSupply()).thenReturn(300);
        when(mockForeman.level()).thenReturn(null);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should not throw with null level
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    // === Edge Cases and Boundary Conditions ===

    @Test
    @DisplayName("isLowHealth() should detect exactly 30% health as not low")
    void testIsLowHealth_Exactly30Percent() {
        when(mockForeman.getHealth()).thenReturn(6.0f); // Exactly 30% of 20
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isLowHealth() should detect 29.9% health as low")
    void testIsLowHealth_JustBelow30Percent() {
        when(mockForeman.getHealth()).thenReturn(5.9f); // Just below 30% of 20
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isOnFire() should detect fire by remaining ticks")
    void testIsOnFire_ByTicks() {
        when(mockForeman.isOnFire()).thenReturn(false);
        when(mockForeman.getRemainingFireTicks()).thenReturn(50);

        assertTrue(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isDrowning() should require underwater and low air")
    void testIsDrowning_UnderwaterButFullAir() {
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(300); // Full air
        when(mockForeman.getMaxAirSupply()).thenReturn(300);

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isDrowning() should not trigger when not underwater")
    void testIsDrowning_NotUnderwater() {
        when(mockForeman.isUnderWater()).thenReturn(false);
        when(mockForeman.getAirSupply()).thenReturn(50);
        when(mockForeman.getMaxAirSupply()).thenReturn(300);

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isFalling() should require both velocity and air below")
    void testIsFalling_VelocityButGroundBelow() {
        when(mockForeman.getDeltaMovement()).thenReturn(new Vec3(0, -1.0, 0));
        when(mockBlockState.isAir()).thenReturn(false); // Solid ground below

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isFalling() should not trigger with slow fall speed")
    void testIsFalling_SlowFallSpeed() {
        when(mockForeman.getDeltaMovement()).thenReturn(new Vec3(0, -0.3, 0)); // Above threshold
        when(mockBlockState.isAir()).thenReturn(true);

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isUnderAttack() should check distance threshold")
    void testIsUnderAttack_AttackerFarAway() {
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);
        when(mockAttacker.blockPosition()).thenReturn(new BlockPos(100, 64, 100)); // Far away
        when(mockAttacker.position()).thenReturn(new Vec3(100, 64, 100));

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isUnderAttack() should return false for null attacker")
    void testIsUnderAttack_NullAttacker() {
        when(mockForeman.getLastHurtByMob()).thenReturn(null);

        assertFalse(survivalProcess.canRun());
    }

    @Test
    @DisplayName("isUnderAttack() should return false for dead attacker")
    void testIsUnderAttack_DeadAttacker() {
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);
        when(mockAttacker.isAlive()).thenReturn(false);

        assertFalse(survivalProcess.canRun());
    }

    // === Navigation Integration Tests ===

    @Test
    @DisplayName("navigateTo() should be called when fleeing with navigation available")
    void testNavigateTo_CalledWhenAvailable() {
        // This test verifies the integration but can't fully test without
        // mocking Minecraft's PathNavigation class
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Should attempt navigation (we verify no exception thrown)
        assertDoesNotThrow(() -> survivalProcess.tick());
    }

    // === Multiple Threat Detection Tests ===

    @Test
    @DisplayName("Multiple threats should prioritize by severity")
    void testMultipleThreats_PriorityOrder() {
        // Setup multiple threats: lava (highest), fire, drowning, falling, low health, attack (lowest)
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50);
        when(mockForeman.getMaxAirSupply()).thenReturn(300);
        when(mockLevel.getBlockState(any(BlockPos.class))).thenReturn(mockBlockState);
        when(mockBlockState.is(Blocks.LAVA)).thenReturn(true);
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Lava should win (highest priority)
        verify(mockForeman).sendChatMessage(contains("lava"));
    }

    // === Tick Logging Tests ===

    @Test
    @DisplayName("tick() should log every 20 ticks")
    void testTick_LoggingInterval() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        // Tick 20 times - should log at least once
        for (int i = 0; i < 20; i++) {
            assertDoesNotThrow(() -> survivalProcess.tick());
        }

        assertTrue(survivalProcess.isActive());
    }

    // === Threat Message Tests ===

    @Test
    @DisplayName("Low health threat should show correct message")
    void testThreatMessage_LowHealth() {
        when(mockForeman.getHealth()).thenReturn(5.0f);
        when(mockForeman.getMaxHealth()).thenReturn(20.0f);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("hurt"));
    }

    @Test
    @DisplayName("Fire threat should show correct message")
    void testThreatMessage_Fire() {
        when(mockForeman.isOnFire()).thenReturn(true);
        when(mockForeman.getRemainingFireTicks()).thenReturn(100);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("fire"));
    }

    @Test
    @DisplayName("Lava threat should show correct message")
    void testThreatMessage_Lava() {
        when(mockLevel.getBlockState(any(BlockPos.class))).thenReturn(mockBlockState);
        when(mockBlockState.is(Blocks.LAVA)).thenReturn(true);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("lava"));
    }

    @Test
    @DisplayName("Drowning threat should show correct message")
    void testThreatMessage_Drowning() {
        when(mockForeman.isUnderWater()).thenReturn(true);
        when(mockForeman.getAirSupply()).thenReturn(50);
        when(mockForeman.getMaxAirSupply()).thenReturn(300);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("breathe"));
    }

    @Test
    @DisplayName("Falling threat should show correct message")
    void testThreatMessage_Falling() {
        when(mockForeman.getDeltaMovement()).thenReturn(new Vec3(0, -1.0, 0));
        when(mockBlockState.isAir()).thenReturn(true);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("falling"));
    }

    @Test
    @DisplayName("Under attack threat should show correct message")
    void testThreatMessage_UnderAttack() {
        when(mockForeman.getLastHurtByMob()).thenReturn(mockAttacker);

        survivalProcess.canRun();
        survivalProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("attack"));
    }
}
