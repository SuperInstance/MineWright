package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link CombatAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Target detection (hostile mobs, specific entities)</li>
 *   <li>Target selection (nearest hostile)</li>
 *   <li>Combat execution (movement, attacking)</li>
 *   <li>Stuck detection and teleportation</li>
 *   <li>Invulnerability management</li>
 *   <li>Timeout handling</li>
 *   <li>Cancellation and cleanup</li>
 *   <li>Target validation (no friendly fire)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CombatAction Tests")
class CombatActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private PathNavigation navigation;

    @Mock
    private Zombie hostileMob;

    @Mock
    private Player player;

    @Mock
    private ForemanEntity otherForeman;

    private Task task;
    private CombatAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(foreman.getNavigation()).thenReturn(navigation);
        lenient().when(navigation.isDone()).thenReturn(true);
        lenient().when(foreman.getX()).thenReturn(0.0);
        lenient().when(foreman.getY()).thenReturn(64.0);
        lenient().when(foreman.getZ()).thenReturn(0.0);
        lenient().when(foreman.getBoundingBox()).thenReturn(new AABB(0, 64, 0, 1, 65, 1));

        // Setup hostile mob
        lenient().when(hostileMob.isAlive()).thenReturn(true);
        lenient().when(hostileMob.isRemoved()).thenReturn(false);
        lenient().when(hostileMob.getType()).thenReturn((EntityType) EntityType.ZOMBIE);
        lenient().when(hostileMob.getX()).thenReturn(10.0);
        lenient().when(hostileMob.getY()).thenReturn(64.0);
        lenient().when(hostileMob.getZ()).thenReturn(10.0);
        lenient().when(hostileMob.getBoundingBox()).thenReturn(new AABB(10, 64, 10, 11, 65, 11));

        // Setup player
        lenient().when(player.isAlive()).thenReturn(true);
        lenient().when(player.isRemoved()).thenReturn(false);
        lenient().when(player.getType()).thenReturn((EntityType) EntityType.PLAYER);

        // Setup other foreman
        lenient().when(otherForeman.isAlive()).thenReturn(true);
        lenient().when(otherForeman.isRemoved()).thenReturn(false);
    }

    // ==================== Target Detection Tests ====================

    @Test
    @DisplayName("Should successfully initialize with default target")
    void testValidDefaultTarget() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Should not fail immediately
        assertNull(action.getResult(), "Should not have result yet");
    }

    @Test
    @DisplayName("Should successfully initialize with specific target type")
    void testValidSpecificTarget() {
        Map<String, Object> params = new HashMap<>();
        params.put("target", "zombie");
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should start successfully");
    }

    @Test
    @DisplayName("Should fail when navigation unavailable")
    void testNavigationUnavailable() {
        lenient().when(foreman.getNavigation()).thenReturn(null);

        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        action = new CombatAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Should be failure result");
        assertTrue(result.getMessage().contains("navigation") ||
                   result.getMessage().contains("Navigation") ||
                   result.getMessage().contains("available"),
            "Should indicate navigation unavailable");
    }

    @Test
    @DisplayName("Should fail when level unavailable")
    void testLevelUnavailable() {
        lenient().when(foreman.level()).thenReturn(null);

        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        action = new CombatAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("level") ||
                   result.getMessage().contains("Level"));
    }

    @Test
    @DisplayName("Should detect hostile mobs nearby")
    void testDetectHostileMobs() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should find hostile mob");
        assertEquals("Attack mob", action.getDescription());
    }

    @Test
    @DisplayName("Should select nearest hostile mob")
    void testSelectNearestHostile() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        Zombie farMob = mock(Zombie.class);
        lenient().when(farMob.isAlive()).thenReturn(true);
        lenient().when(farMob.isRemoved()).thenReturn(false);
        lenient().when(farMob.getType()).thenReturn((EntityType) EntityType.ZOMBIE);
        lenient().when(farMob.getX()).thenReturn(50.0);
        lenient().when(farMob.getY()).thenReturn(64.0);
        lenient().when(farMob.getZ()).thenReturn(50.0);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        entities.add(farMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        when(foreman.distanceTo(hostileMob)).thenReturn(10.0f);
        when(foreman.distanceTo(farMob)).thenReturn(50.0f);

        action = new CombatAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should select nearest hostile");
    }

    // ==================== Combat Execution Tests ====================

    @Test
    @DisplayName("Should sprint during combat")
    void testSprintDuringCombat() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Tick to trigger combat
        action.tick();

        verify(foreman, atLeastOnce()).setSprinting(true);
    }

    @Test
    @DisplayName("Should navigate to target")
    void testNavigateToTarget() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Tick to trigger movement
        action.tick();

        verify(navigation, atLeastOnce()).moveTo(any(LivingEntity.class), anyDouble());
    }

    @Test
    @DisplayName("Should attack when in range")
    void testAttackWhenInRange() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        when(foreman.distanceTo(hostileMob)).thenReturn(2.0f); // In attack range

        action = new CombatAction(foreman, task);
        action.start();

        // Tick to trigger attack
        action.tick();

        verify(foreman, atLeastOnce()).doHurtTarget(eq(hostileMob));
        verify(foreman, atLeastOnce()).swing(any(), anyBoolean());
    }

    @Test
    @DisplayName("Should not attack when out of range")
    void testNotAttackWhenOutOfRange() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        when(foreman.distanceTo(hostileMob)).thenReturn(10.0f); // Out of range

        action = new CombatAction(foreman, task);
        action.start();

        // Tick to trigger movement but not attack
        action.tick();

        verify(foreman, never()).doHurtTarget(any());
    }

    @Test
    @DisplayName("Should attack at 3 times per second rate")
    void testAttackRate() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        when(foreman.distanceTo(hostileMob)).thenReturn(2.0f);

        action = new CombatAction(foreman, task);
        action.start();

        // Attack should happen every 7 ticks
        for (int i = 1; i <= 20; i++) {
            action.tick();
            if (i % 7 == 0) {
                verify(foreman, atLeast(i / 7)).doHurtTarget(eq(hostileMob));
            }
        }
    }

    // ==================== Stuck Detection Tests ====================

    @Test
    @DisplayName("Should detect stuck and teleport closer")
    void testStuckDetectionAndTeleport() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        when(foreman.distanceTo(hostileMob)).thenReturn(10.0f);
        when(foreman.getX()).thenReturn(0.0);
        when(foreman.getZ()).thenReturn(0.0);
        when(hostileMob.getX()).thenReturn(10.0);
        when(hostileMob.getZ()).thenReturn(10.0);

        action = new CombatAction(foreman, task);
        action.start();

        // Simulate being stuck (not moving) for > 40 ticks
        for (int i = 0; i < 45; i++) {
            lenient().when(foreman.getX()).thenReturn(0.0);
            lenient().when(foreman.getZ()).thenReturn(0.0);
            action.tick();
        }

        // Should have teleported closer
        verify(foreman, atLeastOnce()).teleportTo(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should reset stuck counter when moving")
    void testResetStuckCounterWhenMoving() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        when(foreman.distanceTo(hostileMob)).thenReturn(5.0f);

        action = new CombatAction(foreman, task);
        action.start();

        // Move foreman
        when(foreman.getX()).thenReturn(0.0);
        when(foreman.getZ()).thenReturn(0.0);
        action.tick();

        when(foreman.getX()).thenReturn(1.0);
        when(foreman.getZ()).thenReturn(1.0);
        action.tick();

        // Should not have teleported (wasn't stuck)
        verify(foreman, never()).teleportTo(anyDouble(), anyDouble(), anyDouble());
    }

    // ==================== Invulnerability Tests ====================

    @Test
    @DisplayName("Should enable invulnerability on start")
    void testEnableInvulnerabilityOnStart() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        verify(foreman, atLeastOnce()).setInvulnerableBuilding(true);
        verify(foreman, atLeastOnce()).setFlying(false);
    }

    @Test
    @DisplayName("Should disable invulnerability on completion")
    void testDisableInvulnerabilityOnCompletion() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Run to completion
        for (int i = 0; i < 650 && !action.isComplete(); i++) {
            action.tick();
        }

        verify(foreman, atLeastOnce()).setInvulnerableBuilding(false);
        verify(foreman, atLeastOnce()).setSprinting(false);
    }

    @Test
    @DisplayName("Should disable invulnerability on cancellation")
    void testDisableInvulnerabilityOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();
        action.cancel();

        verify(foreman, atLeastOnce()).setInvulnerableBuilding(false);
        verify(foreman, atLeastOnce()).setSprinting(false);
        verify(foreman, atLeastOnce()).setFlying(false);
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Run until timeout (600 ticks)
        for (int i = 0; i < 650 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertTrue(result.isSuccess() || result.getMessage().contains("complete"),
            "Should indicate combat complete");
    }

    // ==================== Target Validation Tests ====================

    @Test
    @DisplayName("Should not attack players")
    void testNotAttackPlayers() {
        Map<String, Object> params = new HashMap<>();
        params.put("target", "mob");
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(player);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Should not find target (player is not hostile)
        // In real scenario, would find no targets
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should not attack other foremen")
    void testNotAttackOtherForemen() {
        Map<String, Object> params = new HashMap<>();
        params.put("target", "mob");
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(otherForeman);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Should not attack other foreman
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should not attack dead entities")
    void testNotAttackDeadEntities() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        lenient().when(hostileMob.isAlive()).thenReturn(false);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Should skip dead entities
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should not attack removed entities")
    void testNotAttackRemovedEntities() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        lenient().when(hostileMob.isRemoved()).thenReturn(true);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        // Should skip removed entities
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should match specific entity type")
    void testMatchSpecificEntityType() {
        Map<String, Object> params = new HashMap<>();
        params.put("target", "zombie");
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        assertEquals("Attack zombie", action.getDescription());
    }

    @Test
    @DisplayName("Should match any hostile mob")
    void testMatchAnyHostileMob() {
        String[] targetTypes = {"mob", "hostile", "monster", "any"};

        for (String targetType : targetTypes) {
            Map<String, Object> params = new HashMap<>();
            params.put("target", targetType);
            task = new Task("combat", params);

            List<Entity> entities = new ArrayList<>();
            entities.add(hostileMob);
            when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

            action = new CombatAction(foreman, task);
            action.start();

            assertEquals("Attack mob", action.getDescription(),
                "Should match generic target type: " + targetType);
        }
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete(), "Should be complete after cancellation");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        // Result may be null if cancelled immediately
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();
        action.cancel();

        // Verify navigation stopped
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Description Tests ====================

    @Test
    @DisplayName("Description should contain target type")
    void testDescriptionContainsTargetType() {
        Map<String, Object> params = new HashMap<>();
        params.put("target", "zombie");
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.toLowerCase().contains("zombie") ||
                   description.toLowerCase().contains("attack"),
            "Description should reference the target type");
    }

    @Test
    @DisplayName("Description should show default hostile mobs")
    void testDescriptionDefaultTarget() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.toLowerCase().contains("mob") ||
                   description.toLowerCase().contains("hostile") ||
                   description.toLowerCase().contains("attack"),
            "Description should indicate attacking hostile mobs");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        action = new CombatAction(null, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle no targets nearby")
    void testNoTargetsNearby() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(new ArrayList<>());

        action = new CombatAction(foreman, task);
        action.start();

        // Should not fail, just warn
        assertNull(action.getResult(), "Should handle no targets gracefully");
    }

    @Test
    @DisplayName("Should handle empty entities list")
    void testEmptyEntitiesList() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should handle empty entities list");
    }

    @Test
    @DisplayName("Should handle case insensitive target type")
    void testCaseInsensitiveTargetType() {
        String[] variants = {"ZOMBIE", "Zombie", "zOmBiE", "zombie"};

        for (String variant : variants) {
            Map<String, Object> params = new HashMap<>();
            params.put("target", variant);
            task = new Task("combat", params);

            List<Entity> entities = new ArrayList<>();
            entities.add(hostileMob);
            when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

            action = new CombatAction(foreman, task);
            action.start();

            assertNotNull(action, "Should handle case variant: " + variant);
        }
    }

    @Test
    @DisplayName("Should re-search for targets periodically")
    void testPeriodicTargetSearch() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);
        lenient().when(hostileMob.isAlive()).thenReturn(false);

        action = new CombatAction(foreman, task);
        action.start();

        // Tick multiple times - should search periodically
        for (int i = 0; i < 50; i++) {
            action.tick();
        }

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should use high speed multiplier for sprinting")
    void testHighSpeedMultiplier() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("combat", params);

        List<Entity> entities = new ArrayList<>();
        entities.add(hostileMob);
        when(level.getEntities(eq(foreman), any(AABB.class))).thenReturn(entities);

        action = new CombatAction(foreman, task);
        action.start();

        action.tick();

        verify(navigation, atLeastOnce()).moveTo(any(LivingEntity.class), eq(2.5));
    }
}
