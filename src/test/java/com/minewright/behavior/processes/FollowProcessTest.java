package com.minewright.behavior.processes;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for FollowProcess.
 *
 * <p>Tests cover owner tracking and follow behavior:
 * <ul>
 *   <li>setOwnerUUID(), getOwnerUUID(), hasOwner(), getOwnerEntity()</li>
 *   <li>moveTowardTarget(), findFollowTarget(), findPlayerByUUID()</li>
 *   <li>Teleport behavior when target is too far</li>
 *   <li>Distance checking and follow radius management</li>
 * </ul>
 *
 * @since 1.2.0
 */
@DisplayName("FollowProcess Tests")
class FollowProcessTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private Level mockLevel;

    @Mock
    private PathNavigation mockNavigation;

    @Mock
    private Player mockOwnerPlayer;

    @Mock
    private Player mockOtherPlayer;

    private FollowProcess followProcess;

    private final UUID ownerUUID = UUID.randomUUID();
    private final UUID otherPlayerUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock foreman defaults
        lenient().when(mockForeman.getEntityName()).thenReturn("TestForeman");
        lenient().when(mockForeman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(mockForeman.position()).thenReturn(new Vec3(0, 64, 0));
        lenient().when(mockForeman.level()).thenReturn(mockLevel);
        lenient().when(mockForeman.getNavigation()).thenReturn(mockNavigation);
        lenient().when(mockForeman.isAlive()).thenReturn(true);

        // Setup mock navigation defaults
        lenient().when(mockNavigation.isInProgress()).thenReturn(false);
        lenient().when(mockNavigation.getTargetPos()).thenReturn(null);

        // Setup mock level defaults
        lenient().when(mockLevel.players()).thenReturn(new ArrayList<>());

        // Setup mock owner player defaults
        lenient().when(mockOwnerPlayer.getUUID()).thenReturn(ownerUUID);
        lenient().when(mockOwnerPlayer.isAlive()).thenReturn(true);
        lenient().when(mockOwnerPlayer.blockPosition()).thenReturn(new BlockPos(10, 64, 10));
        lenient().when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 10));
        lenient().when(mockOwnerPlayer.getName()).thenReturn(mock(Component.literal("OwnerPlayer")));

        // Setup mock other player defaults
        lenient().when(mockOtherPlayer.getUUID()).thenReturn(otherPlayerUUID);
        lenient().when(mockOtherPlayer.isAlive()).thenReturn(true);
        lenient().when(mockOtherPlayer.blockPosition()).thenReturn(new BlockPos(20, 64, 20));
        lenient().when(mockOtherPlayer.position()).thenReturn(new Vec3(20, 64, 20));
        lenient().when(mockOtherPlayer.getName()).thenReturn(mock(Component.literal("OtherPlayer")));

        followProcess = new FollowProcess(mockForeman);
    }

    // === Basic Process Tests ===

    @Test
    @DisplayName("Process name should be 'Follow'")
    void testGetName() {
        assertEquals("Follow", followProcess.getName());
    }

    @Test
    @DisplayName("Process priority should be 25 (medium-low)")
    void testGetPriority() {
        assertEquals(25, followProcess.getPriority());
    }

    @Test
    @DisplayName("Process should not be active initially")
    void testInitialState() {
        assertFalse(followProcess.isActive());
    }

    @Test
    @DisplayName("Follow target should be null initially")
    void testInitialFollowTarget() {
        assertNull(followProcess.getFollowTarget());
    }

    // === Owner Tracking Tests ===

    @Test
    @DisplayName("getOwnerUUID() should return null initially")
    void testGetOwnerUUID_Initial() {
        assertNull(followProcess.getOwnerUUID());
    }

    @Test
    @DisplayName("setOwnerUUID() should set owner UUID")
    void testSetOwnerUUID() {
        followProcess.setOwnerUUID(ownerUUID);

        assertEquals(ownerUUID, followProcess.getOwnerUUID());
    }

    @Test
    @DisplayName("setOwnerUUID() with null should clear owner")
    void testSetOwnerUUID_Null() {
        followProcess.setOwnerUUID(ownerUUID);
        assertNotNull(followProcess.getOwnerUUID());

        followProcess.setOwnerUUID(null);
        assertNull(followProcess.getOwnerUUID());
    }

    @Test
    @DisplayName("hasOwner() should return false initially")
    void testHasOwner_Initial() {
        assertFalse(followProcess.hasOwner());
    }

    @Test
    @DisplayName("hasOwner() should return true after setting owner")
    void testHasOwner_AfterSet() {
        followProcess.setOwnerUUID(ownerUUID);

        assertTrue(followProcess.hasOwner());
    }

    @Test
    @DisplayName("hasOwner() should return false after clearing owner")
    void testHasOwner_AfterClear() {
        followProcess.setOwnerUUID(ownerUUID);
        assertTrue(followProcess.hasOwner());

        followProcess.clearOwner();
        assertFalse(followProcess.hasOwner());
    }

    @Test
    @DisplayName("getOwnerEntity() should return null when no owner set")
    void testGetOwnerEntity_NoOwner() {
        assertNull(followProcess.getOwnerEntity());
    }

    @Test
    @DisplayName("getOwnerEntity() should return null when owner not in level")
    void testGetOwnerEntity_OwnerNotInLevel() {
        followProcess.setOwnerUUID(ownerUUID);
        when(mockLevel.players()).thenReturn(new ArrayList<>());

        Player owner = followProcess.getOwnerEntity();
        assertNull(owner);
    }

    @Test
    @DisplayName("getOwnerEntity() should return owner when in level")
    void testGetOwnerEntity_OwnerInLevel() {
        followProcess.setOwnerUUID(ownerUUID);

        List<Player> players = new ArrayList<>();
        players.add(mockOwnerPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        Player owner = followProcess.getOwnerEntity();
        assertNotNull(owner);
        assertEquals(mockOwnerPlayer, owner);
    }

    @Test
    @DisplayName("getOwnerEntity() should find correct owner among multiple players")
    void testGetOwnerEntity_MultiplePlayers() {
        followProcess.setOwnerUUID(ownerUUID);

        List<Player> players = new ArrayList<>();
        players.add(mockOtherPlayer);
        players.add(mockOwnerPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        Player owner = followProcess.getOwnerEntity();
        assertNotNull(owner);
        assertEquals(ownerUUID, owner.getUUID());
        assertEquals(mockOwnerPlayer, owner);
    }

    @Test
    @DisplayName("clearOwner() should remove owner UUID")
    void testClearOwner() {
        followProcess.setOwnerUUID(ownerUUID);
        assertTrue(followProcess.hasOwner());

        followProcess.clearOwner();

        assertFalse(followProcess.hasOwner());
        assertNull(followProcess.getOwnerUUID());
    }

    // === Follow Target Tests ===

    @Test
    @DisplayName("setFollowTarget() should set target entity")
    void testSetFollowTarget() {
        followProcess.setFollowTarget(mockOwnerPlayer);

        assertEquals(mockOwnerPlayer, followProcess.getFollowTarget());
    }

    @Test
    @DisplayName("setFollowTarget() with null should clear target")
    void testSetFollowTarget_Null() {
        followProcess.setFollowTarget(mockOwnerPlayer);
        assertNotNull(followProcess.getFollowTarget());

        followProcess.setFollowTarget(null);
        assertNull(followProcess.getFollowTarget());
    }

    @Test
    @DisplayName("canRun() should return false when no target available")
    void testCanRun_NoTarget() {
        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(null);

        assertFalse(followProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return false when target is too close")
    void testCanRun_TargetTooClose() {
        // Player at distance 2 (within MIN_FOLLOW_DISTANCE of 3)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(2, 64, 0));
        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(mockOwnerPlayer);

        assertFalse(followProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should return true when target is far enough")
    void testCanRun_TargetFarEnough() {
        // Player at distance 10 (outside MIN_FOLLOW_DISTANCE of 3)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(mockOwnerPlayer);

        assertTrue(followProcess.canRun());
    }

    @Test
    @DisplayName("canRun() should prioritize owner over nearest player")
    void testCanRun_PrioritizesOwner() {
        followProcess.setOwnerUUID(ownerUUID);

        List<Player> players = new ArrayList<>();
        players.add(mockOwnerPlayer);
        players.add(mockOtherPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        // Owner is at distance 10
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        // Other player is at distance 5 (closer)
        when(mockOtherPlayer.position()).thenReturn(new Vec3(5, 64, 0));

        // Should prefer owner even though other player is closer
        assertTrue(followProcess.canRun());
    }

    // === Tick Tests ===

    @Test
    @DisplayName("tick() should increment ticks active")
    void testTick_IncrementsTicks() {
        followProcess.onActivate();

        followProcess.tick();
        assertEquals(1, followProcess.getTicksActive());

        followProcess.tick();
        assertEquals(2, followProcess.getTicksActive());
    }

    @Test
    @DisplayName("tick() should handle null follow target")
    void testTick_NullFollowTarget() {
        followProcess.onActivate();

        assertDoesNotThrow(() -> followProcess.tick());
    }

    @Test
    @DisplayName("tick() should handle dead follow target")
    void testTick_DeadFollowTarget() {
        followProcess.setFollowTarget(mockOwnerPlayer);
        when(mockOwnerPlayer.isAlive()).thenReturn(false);

        followProcess.onActivate();

        assertDoesNotThrow(() -> followProcess.tick());
    }

    @Test
    @DisplayName("tick() should teleport when target is too far")
    void testTick_TeleportWhenTooFar() {
        // Target at distance 70 (outside MAX_TELEPORT_DISTANCE of 64)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(70, 64, 0));
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        assertDoesNotThrow(() -> followProcess.tick());

        // Verify teleportTo was called
        verify(mockForeman, atLeastOnce()).teleportTo(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("tick() should not teleport when target is close")
    void testTick_NoTeleportWhenClose() {
        // Target at distance 10 (well within MAX_TELEPORT_DISTANCE of 64)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        assertDoesNotThrow(() -> followProcess.tick());

        // Verify teleportTo was NOT called
        verify(mockForeman, never()).teleportTo(anyDouble(), anyDouble(), anyDouble());
    }

    // === moveTowardTarget Tests ===

    @Test
    @DisplayName("moveTowardTarget() should not throw with null target")
    void testMoveTowardTarget_NullTarget() {
        followProcess.setFollowTarget(null);
        followProcess.onActivate();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                followProcess.tick();
            }
        });
    }

    @Test
    @DisplayName("moveTowardTarget() should not throw with null navigation")
    void testMoveTowardTarget_NullNavigation() {
        when(mockForeman.getNavigation()).thenReturn(null);
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        assertDoesNotThrow(() -> followProcess.tick());
    }

    @Test
    @DisplayName("moveTowardTarget() should start navigation when not in progress")
    void testMoveTowardTarget_StartNavigation() {
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        when(mockNavigation.isInProgress()).thenReturn(false);
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Wait for update interval (10 ticks)
        for (int i = 0; i < 10; i++) {
            followProcess.tick();
        }

        // Verify navigation.moveTo was called (with Entity overload)
        verify(mockNavigation, atLeastOnce()).moveTo(any(Entity.class), anyDouble());
    }

    @Test
    @DisplayName("moveTowardTarget() should stop and restart when target moves far")
    void testMoveTowardTarget_RestartsOnTargetMove() {
        BlockPos initialTarget = new BlockPos(10, 64, 10);

        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 10));
        when(mockNavigation.isInProgress()).thenReturn(true);
        when(mockNavigation.getTargetPos()).thenReturn(initialTarget);
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Wait for update interval
        for (int i = 0; i < 10; i++) {
            followProcess.tick();
        }

        // Move target far away (more than sqrt(100) = 10 blocks)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(25, 64, 25));
        when(mockNavigation.getTargetPos()).thenReturn(initialTarget);

        // Wait for another update interval
        for (int i = 0; i < 10; i++) {
            followProcess.tick();
        }

        // Verify navigation was stopped and restarted
        verify(mockNavigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("moveTowardTarget() should not restart when target moves little")
    void testMoveTowardTarget_DoesNotRestartOnSmallMove() {
        BlockPos initialTarget = new BlockPos(10, 64, 10);

        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 10));
        when(mockNavigation.isInProgress()).thenReturn(true);
        when(mockNavigation.getTargetPos()).thenReturn(initialTarget);
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Wait for update interval
        for (int i = 0; i < 10; i++) {
            followProcess.tick();
        }

        // Move target slightly (less than 10 blocks)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(15, 64, 15));
        when(mockNavigation.getTargetPos()).thenReturn(initialTarget);

        // Wait for another update interval
        for (int i = 0; i < 10; i++) {
            followProcess.tick();
        }

        // Verify navigation was NOT stopped
        verify(mockNavigation, never()).stop();
    }

    // === findFollowTarget Tests ===

    @Test
    @DisplayName("findFollowTarget() should return null when level is null")
    void testFindFollowTarget_NullLevel() {
        when(mockForeman.level()).thenReturn(null);

        followProcess.onActivate();

        assertFalse(followProcess.canRun());
    }

    @Test
    @DisplayName("findFollowTarget() should return owner when set and in range")
    void testFindFollowTarget_OwnerInRange() {
        followProcess.setOwnerUUID(ownerUUID);

        List<Player> players = new ArrayList<>();
        players.add(mockOwnerPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        // Owner at distance 10 (within MAX_TELEPORT_DISTANCE)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));

        assertTrue(followProcess.canRun());
    }

    @Test
    @DisplayName("findFollowTarget() should return null when owner out of range")
    void testFindFollowTarget_OwnerOutOfRange() {
        followProcess.setOwnerUUID(ownerUUID);

        List<Player> players = new ArrayList<>();
        players.add(mockOwnerPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        // Owner at distance 100 (outside MAX_TELEPORT_DISTANCE of 64)
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(100, 64, 0));

        assertFalse(followProcess.canRun());
    }

    @Test
    @DisplayName("findFollowTarget() should return nearest player when no owner")
    void testFindFollowTarget_NoOwner() {
        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(mockOtherPlayer);

        assertTrue(followProcess.canRun());
    }

    // === findPlayerByUUID Tests ===

    @Test
    @DisplayName("findPlayerByUUID() should return null when level is null")
    void testFindPlayerByUUID_NullLevel() {
        when(mockForeman.level()).thenReturn(null);

        Player found = followProcess.getOwnerEntity();
        assertNull(found);
    }

    @Test
    @DisplayName("findPlayerByUUID() should return null for null UUID")
    void testFindPlayerByUUID_NullUUID() {
        List<Player> players = new ArrayList<>();
        players.add(mockOwnerPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        // Don't set owner UUID
        Player found = followProcess.getOwnerEntity();
        assertNull(found);
    }

    @Test
    @DisplayName("findPlayerByUUID() should return null when player not found")
    void testFindPlayerByUUID_PlayerNotFound() {
        followProcess.setOwnerUUID(ownerUUID);
        when(mockLevel.players()).thenReturn(new ArrayList<>());

        Player found = followProcess.getOwnerEntity();
        assertNull(found);
    }

    @Test
    @DisplayName("findPlayerByUUID() should find player by UUID")
    void testFindPlayerByUUID_PlayerFound() {
        followProcess.setOwnerUUID(ownerUUID);

        List<Player> players = new ArrayList<>();
        players.add(mockOtherPlayer);
        players.add(mockOwnerPlayer);
        when(mockLevel.players()).thenReturn((List) players);

        Player found = followProcess.getOwnerEntity();
        assertNotNull(found);
        assertEquals(mockOwnerPlayer, found);
    }

    // === distanceTo Tests ===

    @Test
    @DisplayName("distanceTo() should calculate correct distance")
    void testDistanceTo_CalculatesCorrectly() {
        when(mockForeman.position()).thenReturn(new Vec3(0, 64, 0));
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));

        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Distance should be 10
        assertDoesNotThrow(() -> followProcess.tick());
    }

    // === Activation/Deactivation Tests ===

    @Test
    @DisplayName("onActivate() should activate process")
    void testOnActivate() {
        followProcess.onActivate();
        assertTrue(followProcess.isActive());
    }

    @Test
    @DisplayName("onActivate() should send chat message")
    void testOnActivate_SendsMessage() {
        followProcess.onActivate();

        verify(mockForeman).sendChatMessage(contains("follow"));
    }

    @Test
    @DisplayName("onDeactivate() should deactivate process")
    void testOnDeactivate() {
        followProcess.onActivate();
        assertTrue(followProcess.isActive());

        followProcess.onDeactivate();
        assertFalse(followProcess.isActive());
    }

    @Test
    @DisplayName("onDeactivate() should clear follow target")
    void testOnDeactivate_ClearsTarget() {
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();
        followProcess.onDeactivate();

        assertNull(followProcess.getFollowTarget());
    }

    // === Update Interval Tests ===

    @Test
    @DisplayName("tick() should respect update interval")
    void testTick_RespectsUpdateInterval() {
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Navigation should only be called after 10 ticks
        for (int i = 0; i < 9; i++) {
            followProcess.tick();
        }

        // Not yet called navigation
        verify(mockNavigation, never()).moveTo(any(Entity.class), anyDouble());

        // 10th tick should trigger navigation
        followProcess.tick();
        verify(mockNavigation, atLeastOnce()).moveTo(any(Entity.class), anyDouble());
    }

    // === Logging Tests ===

    @Test
    @DisplayName("tick() should log every 100 ticks")
    void testTick_LoggingInterval() {
        when(mockOwnerPlayer.position()).thenReturn(new Vec3(10, 64, 0));
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Tick 100 times - should log at least once
        for (int i = 0; i < 100; i++) {
            assertDoesNotThrow(() -> followProcess.tick());
        }

        assertEquals(100, followProcess.getTicksActive());
    }

    // === Edge Cases ===

    @Test
    @DisplayName("Process should handle null level during tick")
    void testTick_NullLevel() {
        when(mockForeman.level()).thenReturn(null);
        followProcess.onActivate();

        assertDoesNotThrow(() -> followProcess.tick());
    }

    @Test
    @DisplayName("Process should handle target loss gracefully")
    void testTargetLoss_GracefulHandling() {
        followProcess.setFollowTarget(mockOwnerPlayer);
        followProcess.onActivate();

        // Target disappears
        when(mockOwnerPlayer.isAlive()).thenReturn(false);
        when(mockLevel.getNearestPlayer(eq(mockForeman), anyDouble())).thenReturn(null);

        assertDoesNotThrow(() -> followProcess.tick());
    }

    @Test
    @DisplayName("Process should handle owner UUID changes")
    void testOwnerUUID_Changes() {
        followProcess.setOwnerUUID(ownerUUID);
        assertEquals(ownerUUID, followProcess.getOwnerUUID());

        UUID newOwnerUUID = UUID.randomUUID();
        followProcess.setOwnerUUID(newOwnerUUID);
        assertEquals(newOwnerUUID, followProcess.getOwnerUUID());
    }

    @Test
    @DisplayName("Process should handle multiple activation/deactivation cycles")
    void testMultipleActivationDeactivationCycles() {
        for (int i = 0; i < 5; i++) {
            followProcess.onActivate();
            assertTrue(followProcess.isActive());

            followProcess.tick();
            followProcess.onDeactivate();
            assertFalse(followProcess.isActive());
        }
    }
}
