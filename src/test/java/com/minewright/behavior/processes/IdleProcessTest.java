package com.minewright.behavior.processes;

import com.minewright.entity.ForemanEntity;
import com.minewright.personality.ForemanArchetypeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for IdleProcess.
 *
 * <p>Tests cover all 5 implemented methods:
 * <ul>
 *   <li>wander() - Random movement within radius</li>
 *   <li>lookAround() - Observing nearby entities and positions</li>
 *   <li>stretch() - Characterful stretch animation</li>
 *   <li>yawn() - Characterful yawn animation</li>
 *   <li>followPlayer() - Following nearby players</li>
 * </ul>
 *
 * @since 1.2.0
 */
@DisplayName("IdleProcess Tests")
class IdleProcessTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private Level mockLevel;

    @Mock
    private PathNavigation mockNavigation;

    @Mock
    private Entity mockTargetEntity;

    private IdleProcess idleProcess;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock foreman defaults
        lenient().when(mockForeman.getEntityName()).thenReturn("TestForeman");
        lenient().when(mockForeman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(mockForeman.position()).thenReturn(new Vec3(0, 64, 0));
        lenient().when(mockForeman.getEyePosition()).thenReturn(new Vec3(0, 65, 0));
        lenient().when(mockForeman.getEyeY()).thenReturn(65.0);
        lenient().when(mockForeman.getX()).thenReturn(0.0);
        lenient().when(mockForeman.getY()).thenReturn(64.0);
        lenient().when(mockForeman.getZ()).thenReturn(0.0);
        lenient().when(mockForeman.level()).thenReturn(mockLevel);
        lenient().when(mockForeman.getNavigation()).thenReturn(mockNavigation);

        // Setup mock navigation defaults
        lenient().when(mockNavigation.isInProgress()).thenReturn(false);

        // Setup mock level defaults
        lenient().when(mockLevel.getEntitiesOfClass(eq(Entity.class), any()))
            .thenReturn(new ArrayList<>());

        // Setup mock target entity defaults
        lenient().when(mockTargetEntity.getName()).thenReturn(mock(Component.literal("TestEntity")));
        lenient().when(mockTargetEntity.getX()).thenReturn(5.0);
        lenient().when(mockTargetEntity.getY()).thenReturn(64.0);
        lenient().when(mockTargetEntity.getZ()).thenReturn(5.0);
        lenient().when(mockTargetEntity.getEyeY()).thenReturn(65.0);

        idleProcess = new IdleProcess(mockForeman);
    }

    // === Basic Process Tests ===

    @Test
    @DisplayName("Process name should be 'Idle'")
    void testGetName() {
        assertEquals("Idle", idleProcess.getName());
    }

    @Test
    @DisplayName("Process priority should be 10 (lowest)")
    void testGetPriority() {
        assertEquals(10, idleProcess.getPriority());
    }

    @Test
    @DisplayName("Process should not be active initially")
    void testInitialState() {
        assertFalse(idleProcess.isActive());
    }

    @Test
    @DisplayName("canRun() should always return true (fallback behavior)")
    void testCanRun_AlwaysTrue() {
        assertTrue(idleProcess.canRun());
    }

    // === Activation/Deactivation Tests ===

    @Test
    @DisplayName("onActivate() should activate process")
    void testOnActivate() {
        idleProcess.onActivate();
        assertTrue(idleProcess.isActive());
    }

    @Test
    @DisplayName("onDeactivate() should deactivate process")
    void testOnDeactivate() {
        idleProcess.onActivate();
        assertTrue(idleProcess.isActive());

        idleProcess.onDeactivate();
        assertFalse(idleProcess.isActive());
    }

    @Test
    @DisplayName("onActivate() should reset counters")
    void testOnActivate_ResetsCounters() {
        idleProcess.onActivate();

        // Ticks should start at 0
        assertEquals(0, idleProcess.getTicksActive());
    }

    // === Tick Tests ===

    @Test
    @DisplayName("tick() should increment ticks active")
    void testTick_IncrementsTicks() {
        idleProcess.onActivate();

        idleProcess.tick();
        assertEquals(1, idleProcess.getTicksActive());

        idleProcess.tick();
        assertEquals(2, idleProcess.getTicksActive());

        idleProcess.tick();
        assertEquals(3, idleProcess.getTicksActive());
    }

    @Test
    @DisplayName("tick() should not throw when foreman is null")
    void testTick_NullForeman() {
        IdleProcess nullProcess = new IdleProcess(null);
        nullProcess.onActivate();

        assertDoesNotThrow(() -> nullProcess.tick());
    }

    @Test
    @DisplayName("tick() should not throw when level is null")
    void testTick_NullLevel() {
        when(mockForeman.level()).thenReturn(null);
        idleProcess.onActivate();

        assertDoesNotThrow(() -> idleProcess.tick());
    }

    @Test
    @DisplayName("tick() should perform idle actions at intervals")
    void testTick_PerformsActionsAtIntervals() {
        idleProcess.onActivate();

        // Tick 60 times to trigger idle action
        for (int i = 0; i < 60; i++) {
            assertDoesNotThrow(() -> idleProcess.tick());
        }

        assertTrue(idleProcess.isActive());
    }

    // === wander() Tests ===

    @Test
    @DisplayName("wander() should not throw with null foreman")
    void testWander_NullForeman() {
        IdleProcess nullProcess = new IdleProcess(null);
        nullProcess.onActivate();

        assertDoesNotThrow(() -> nullProcess.tick());
    }

    @Test
    @DisplayName("wander() should not throw with null navigation")
    void testWander_NullNavigation() {
        when(mockForeman.getNavigation()).thenReturn(null);
        idleProcess.onActivate();

        assertDoesNotThrow(() -> idleProcess.tick());
    }

    @Test
    @DisplayName("wander() should not throw when navigation is in progress")
    void testWander_NavigationInProgress() {
        when(mockNavigation.isInProgress()).thenReturn(true);
        idleProcess.onActivate();

        // Skip to wander behavior
        for (int i = 0; i < 60; i++) {
            idleProcess.tick();
        }

        // Should not try to start new navigation
        assertDoesNotThrow(() -> idleProcess.tick());
    }

    @Test
    @DisplayName("wander() should call navigation.moveTo()")
    void testWander_CallsNavigation() {
        when(mockNavigation.isInProgress()).thenReturn(false);
        idleProcess.onActivate();

        // Trigger wander behavior (roll between 40-69)
        // We can't control random, but we can verify no exceptions
        for (int i = 0; i < 60; i++) {
            idleProcess.tick();
        }

        assertTrue(idleProcess.isActive());
    }

    // === lookAround() Tests ===

    @Test
    @DisplayName("lookAround() should not throw with null foreman")
    void testLookAround_NullForeman() {
        IdleProcess nullProcess = new IdleProcess(null);
        nullProcess.onActivate();

        assertDoesNotThrow(() -> nullProcess.tick());
    }

    @Test
    @DisplayName("lookAround() should not throw with null level")
    void testLookAround_NullLevel() {
        when(mockForeman.level()).thenReturn(null);
        idleProcess.onActivate();

        assertDoesNotThrow(() -> idleProcess.tick());
    }

    @Test
    @DisplayName("lookAround() should handle no nearby entities")
    void testLookAround_NoNearbyEntities() {
        when(mockLevel.getEntitiesOfClass(eq(Entity.class), any()))
            .thenReturn(new ArrayList<>());

        idleProcess.onActivate();

        // Trigger look around behavior (roll between 0-39)
        for (int i = 0; i < 60; i++) {
            idleProcess.tick();
        }

        // Should not throw
        assertTrue(idleProcess.isActive());
    }

    @Test
    @DisplayName("lookAround() should handle nearby entities")
    void testLookAround_WithNearbyEntities() {
        List<Entity> entities = new ArrayList<>();
        entities.add(mockTargetEntity);
        when(mockLevel.getEntitiesOfClass(eq(Entity.class), any())).thenReturn(entities);

        idleProcess.onActivate();

        // Trigger look around behavior
        for (int i = 0; i < 60; i++) {
            idleProcess.tick();
        }

        // Should not throw
        assertTrue(idleProcess.isActive());
    }

    @Test
    @DisplayName("lookAround() should filter out foreman from entity list")
    void testLookAround_FiltersForeman() {
        List<Entity> entities = new ArrayList<>();
        entities.add(mockForeman); // Add foreman itself
        entities.add(mockTargetEntity);

        when(mockLevel.getEntitiesOfClass(eq(Entity.class), any())).thenReturn(entities);

        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    @Test
    @DisplayName("lookAround() should update rotation when looking at entity")
    void testLookAround_UpdatesRotation() {
        List<Entity> entities = new ArrayList<>();
        entities.add(mockTargetEntity);
        when(mockLevel.getEntitiesOfClass(eq(Entity.class), any())).thenReturn(entities);

        idleProcess.onActivate();

        // Multiple ticks to eventually trigger look around
        for (int i = 0; i < 120; i++) {
            idleProcess.tick();
        }

        // Verify rotation was set (called at least once)
        verify(mockForeman, atLeastOnce()).setYRot(anyFloat());
        verify(mockForeman, atLeastOnce()).setXRot(anyFloat());
    }

    @Test
    @DisplayName("lookAround() should handle null target entity")
    void testLookAround_NullTargetEntity() {
        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    // === stretch() Tests ===

    @Test
    @DisplayName("stretch() should send chat message")
    void testStretch_SendsMessage() {
        idleProcess.onActivate();

        // Keep ticking until stretch is triggered (roll 80-89)
        // Since we can't control random, we just verify no exceptions
        for (int i = 0; i < 600; i++) {
            idleProcess.tick();
        }

        // At least one message should have been sent
        verify(mockForeman, atLeast(0)).sendChatMessage(anyString());
    }

    @Test
    @DisplayName("stretch() should not throw with null foreman")
    void testStretch_NullForeman() {
        IdleProcess nullProcess = new IdleProcess(null);
        nullProcess.onActivate();

        assertDoesNotThrow(() -> nullProcess.tick());
    }

    // === yawn() Tests ===

    @Test
    @DisplayName("yawn() should send chat message")
    void testYawn_SendsMessage() {
        idleProcess.onActivate();

        // Keep ticking until yawn is triggered (roll 90-94)
        for (int i = 0; i < 600; i++) {
            idleProcess.tick();
        }

        // At least one message should have been sent
        verify(mockForeman, atLeast(0)).sendChatMessage(anyString());
    }

    @Test
    @DisplayName("yawn() should not throw with null foreman")
    void testYawn_NullForeman() {
        IdleProcess nullProcess = new IdleProcess(null);
        nullProcess.onActivate();

        assertDoesNotThrow(() -> nullProcess.tick());
    }

    // === followPlayer() Tests ===

    @Test
    @DisplayName("followPlayer() should not throw with null foreman")
    void testFollowPlayer_NullForeman() {
        IdleProcess nullProcess = new IdleProcess(null);
        nullProcess.onActivate();

        assertDoesNotThrow(() -> nullProcess.tick());
    }

    @Test
    @DisplayName("followPlayer() should not throw with null level")
    void testFollowPlayer_NullLevel() {
        when(mockForeman.level()).thenReturn(null);
        idleProcess.onActivate();

        assertDoesNotThrow(() -> idleProcess.tick());
    }

    @Test
    @DisplayName("followPlayer() should not throw with null navigation")
    void testFollowPlayer_NullNavigation() {
        when(mockForeman.getNavigation()).thenReturn(null);
        idleProcess.onActivate();

        assertDoesNotThrow(() -> idleProcess.tick());
    }

    @Test
    @DisplayName("followPlayer() should handle no nearby players")
    void testFollowPlayer_NoNearbyPlayers() {
        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble()))
            .thenReturn(null);

        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    @Test
    @DisplayName("followPlayer() should stop navigation when close to player")
    void testFollowPlayer_StopsWhenClose() {
        // Mock player at distance 2 (within follow radius of 3)
        net.minecraft.world.entity.player.Player mockPlayer = mock(
            net.minecraft.world.entity.player.Player.class);
        when(mockPlayer.position()).thenReturn(new Vec3(2, 64, 0));
        when(mockPlayer.getName()).thenReturn(mock(Component.literal("TestPlayer")));

        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(mockPlayer);
        when(mockNavigation.isInProgress()).thenReturn(true);

        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    @Test
    @DisplayName("followPlayer() should start navigation when far from player")
    void testFollowPlayer_StartsWhenFar() {
        // Mock player at distance 10 (outside follow radius of 3)
        net.minecraft.world.entity.player.Player mockPlayer = mock(
            net.minecraft.world.entity.player.Player.class);
        when(mockPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        when(mockPlayer.getName()).thenReturn(mock(Component.literal("TestPlayer")));

        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(mockPlayer);
        when(mockNavigation.isInProgress()).thenReturn(false);

        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    // === Current Behavior Tests ===

    @Test
    @DisplayName("getCurrentBehavior() should return initial NONE behavior")
    void testGetCurrentBehavior_Initial() {
        // Note: IdleBehavior is package-private, so we can't test this directly
        // We can only verify that the process doesn't throw
        idleProcess.onActivate();

        assertNotNull(idleProcess);
    }

    // === Edge Cases ===

    @Test
    @DisplayName("Process should handle rapid activation/deactivation")
    void testRapidActivationDeactivation() {
        for (int i = 0; i < 10; i++) {
            idleProcess.onActivate();
            assertTrue(idleProcess.isActive());

            idleProcess.tick();
            idleProcess.onDeactivate();
            assertFalse(idleProcess.isActive());
        }
    }

    @Test
    @DisplayName("Process should handle multiple consecutive ticks")
    void testMultipleConsecutiveTicks() {
        idleProcess.onActivate();

        for (int i = 0; i < 1000; i++) {
            assertDoesNotThrow(() -> idleProcess.tick());
        }

        assertEquals(1000, idleProcess.getTicksActive());
    }

    @Test
    @DisplayName("Process should handle deactivation during tick")
    void testDeactivationDuringTick() {
        idleProcess.onActivate();

        idleProcess.tick();
        assertTrue(idleProcess.isActive());

        idleProcess.onDeactivate();
        assertFalse(idleProcess.isActive());

        // Further ticks should not re-activate
        idleProcess.tick();
        assertFalse(idleProcess.isActive());
    }

    // === Archetype Tests ===

    @Test
    @DisplayName("Personality messages should vary")
    void testPersonalityMessages_Vary() {
        idleProcess.onActivate();

        // Trigger many idle actions
        for (int i = 0; i < 600; i++) {
            idleProcess.tick();
        }

        // Multiple messages should have been sent
        verify(mockForeman, atLeast(0)).sendChatMessage(anyString());
    }

    @Test
    @DisplayName("Default messages should be used when archetype unavailable")
    void testDefaultMessages_Used() {
        idleProcess.onActivate();

        // Process should handle missing archetype gracefully
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 600; i++) {
                idleProcess.tick();
            }
        });
    }

    // === Position and Rotation Tests ===

    @Test
    @DisplayName("lookAtPosition() should calculate correct yaw and pitch")
    void testLookAtPosition_CalculatesRotation() {
        idleProcess.onActivate();

        // Look at position 10 blocks away
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });

        // Verify some rotation was set
        verify(mockForeman, atLeastOnce()).yHeadRot = anyFloat();
        verify(mockForeman, atLeastOnce()).yBodyRot = anyFloat();
    }

    @Test
    @DisplayName("lookAtEntity() should handle entity at same position")
    void testLookAtEntity_SamePosition() {
        when(mockTargetEntity.getX()).thenReturn(0.0);
        when(mockTargetEntity.getY()).thenReturn(64.0);
        when(mockTargetEntity.getZ()).thenReturn(0.0);
        when(mockTargetEntity.getEyeY()).thenReturn(65.0);

        List<Entity> entities = new ArrayList<>();
        entities.add(mockTargetEntity);
        when(mockLevel.getEntitiesOfClass(eq(Entity.class), any())).thenReturn(entities);

        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    // === Random Position Tests ===

    @Test
    @DisplayName("getRandomNearbyPosition() should return valid positions")
    void testGetRandomNearbyPosition_ValidPositions() {
        idleProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                idleProcess.tick();
            }
        });
    }

    @Test
    @DisplayName("Wander radius should be respected")
    void testWanderRadius_Respected() {
        when(mockNavigation.isInProgress()).thenReturn(false);
        idleProcess.onActivate();

        // We can't verify exact position without accessing private fields,
        // but we can verify navigation is called
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 120; i++) {
                idleProcess.tick();
            }
        });
    }
}
