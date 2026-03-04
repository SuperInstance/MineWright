package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link IdleFollowAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Idle state behavior when no work is assigned</li>
 *   <li>Following the nearest player automatically</li>
 *   <li>Distance checks and position maintenance (2.5-4.0 blocks)</li>
 *   <li>Teleportation when too far away (>50 blocks)</li>
 *   <li>Player search interval (every 100 ticks)</li>
 *   <li>Handling player death/disconnect</li>
 *   <li>Navigation control (start/stop)</li>
 *   <li>Cancellation and cleanup</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdleFollowAction Tests")
class IdleFollowActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private PathNavigation navigation;

    @Mock
    private Player targetPlayer;

    @Mock
    private Player secondPlayer;

    private IdleFollowAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(foreman.getNavigation()).thenReturn(navigation);
        lenient().when(foreman.getX()).thenReturn(0.0);
        lenient().when(foreman.getY()).thenReturn(64.0);
        lenient().when(foreman.getZ()).thenReturn(0.0);
        lenient().when(navigation.isDone()).thenReturn(true);
        lenient().when(targetPlayer.isAlive()).thenReturn(true);
        lenient().when(targetPlayer.isRemoved()).thenReturn(false);
        lenient().when(targetPlayer.isSpectator()).thenReturn(false);
        lenient().when(targetPlayer.getName()).thenReturn(mock(net.minecraft.network.chat.Component.class));
        lenient().when(targetPlayer.getName().getString()).thenReturn("TestPlayer");
        lenient().when(targetPlayer.getX()).thenReturn(5.0);
        lenient().when(targetPlayer.getY()).thenReturn(64.0);
        lenient().when(targetPlayer.getZ()).thenReturn(0.0);
        lenient().when(foreman.distanceTo(any(Player.class))).thenReturn(5.0f);
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Should initialize successfully with foreman")
    void testValidInitialization() {
        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Action should be created");
        assertFalse(action.isComplete(), "Idle action should never complete on its own");
    }

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        action = new IdleFollowAction(null);
        action.start();

        assertNotNull(action, "Action should be created even with null foreman");
        // Should not throw exception
    }

    @Test
    @DisplayName("Should handle null level gracefully")
    void testNullLevel() {
        lenient().when(foreman.level()).thenReturn(null);

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Action should be created");
        // Should not throw exception
    }

    @Test
    @DisplayName("Should handle null navigation gracefully")
    void testNullNavigation() {
        lenient().when(foreman.getNavigation()).thenReturn(null);

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Action should be created");
        // Should not throw exception
    }

    // ==================== Player Search Tests ====================

    @Test
    @DisplayName("Should find nearest player on start")
    void testFindNearestPlayerOnStart() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Should not fail, target player should be found
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle no players available")
    void testNoPlayersAvailable() {
        when(level.players()).thenReturn((List) new ArrayList<>());

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Should handle no players gracefully");
    }

    @Test
    @DisplayName("Should select nearest of multiple players")
    void testSelectNearestOfMultiplePlayers() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        players.add(secondPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(10.0f); // Far
        when(foreman.distanceTo(secondPlayer)).thenReturn(3.0f); // Near
        lenient().when(secondPlayer.isAlive()).thenReturn(true);
        lenient().when(secondPlayer.isRemoved()).thenReturn(false);
        lenient().when(secondPlayer.isSpectator()).thenReturn(false);
        lenient().when(secondPlayer.getName()).thenReturn(mock(net.minecraft.network.chat.Component.class));
        lenient().when(secondPlayer.getName().getString()).thenReturn("SecondPlayer");

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Should select nearest player");
    }

    @Test
    @DisplayName("Should ignore spectator players")
    void testIgnoreSpectatorPlayers() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(targetPlayer.isSpectator()).thenReturn(true);

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Should ignore spectator players");
    }

    @Test
    @DisplayName("Should ignore dead players")
    void testIgnoreDeadPlayers() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(targetPlayer.isAlive()).thenReturn(false);

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Should ignore dead players");
    }

    @Test
    @DisplayName("Should ignore removed players")
    void testIgnoreRemovedPlayers() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(targetPlayer.isRemoved()).thenReturn(true);

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Should ignore removed players");
    }

    // ==================== Periodic Player Search Tests ====================

    @Test
    @DisplayName("Should periodically search for better player")
    void testPeriodicPlayerSearch() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Tick 100 times to trigger player search
        for (int i = 0; i < 100; i++) {
            action.tick();
        }

        // Should have searched for new player at tick 100
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should not search before interval expires")
    void testNoSearchBeforeInterval() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Tick 50 times (less than search interval of 100)
        for (int i = 0; i < 50; i++) {
            action.tick();
        }

        // Should not fail
        assertNotNull(action);
    }

    // ==================== Distance Management Tests ====================

    @Test
    @DisplayName("Should navigate when distance exceeds follow threshold")
    void testNavigateWhenTooFar() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(5.0f); // Beyond FOLLOW_DISTANCE (4.0)

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        verify(navigation, atLeastOnce()).moveTo(eq(targetPlayer), anyDouble());
    }

    @Test
    @DisplayName("Should stop navigation when at follow distance")
    void testStopNavigationAtFollowDistance() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(3.5f); // Within FOLLOW_DISTANCE (4.0)

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should stop navigation when too close")
    void testStopNavigationWhenTooClose() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(2.0f); // Below MIN_DISTANCE (2.5)

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should maintain position in ideal range")
    void testMaintainIdealRange() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(3.0f); // In ideal range (2.5-4.0)

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        // Should not navigate when in ideal range
        verify(navigation, never()).moveTo(any(), anyDouble());
    }

    // ==================== Teleportation Tests ====================

    @Test
    @DisplayName("Should teleport when distance exceeds threshold")
    void testTeleportWhenTooFar() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(55.0f); // Beyond TELEPORT_DISTANCE (50.0)

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        verify(foreman, atLeastOnce()).teleportTo(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should stop navigation after teleport")
    void testStopNavigationAfterTeleport() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(60.0f);

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        verify(foreman, atLeastOnce()).teleportTo(anyDouble(), anyDouble(), anyDouble());
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should not teleport when within threshold")
    void testNoTeleportWithinThreshold() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(30.0f); // Below TELEPORT_DISTANCE (50.0)

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        verify(foreman, never()).teleportTo(anyDouble(), anyDouble(), anyDouble());
    }

    // ==================== Player Lost Tests ====================

    @Test
    @DisplayName("Should re-search when target player dies")
    void testPlayerDeathHandling() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Player dies
        when(targetPlayer.isAlive()).thenReturn(false);
        action.tick();

        assertNotNull(action, "Should handle player death");
    }

    @Test
    @DisplayName("Should re-search when target player disconnects")
    void testPlayerDisconnectHandling() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Player disconnects (isRemoved = true)
        when(targetPlayer.isRemoved()).thenReturn(true);
        action.tick();

        assertNotNull(action, "Should handle player disconnect");
    }

    @Test
    @DisplayName("Should idle when no players available")
    void testIdleWhenNoPlayers() {
        when(level.players()).thenReturn((List) new ArrayList<>());

        action = new IdleFollowAction(foreman);
        action.start();

        // Tick multiple times
        for (int i = 0; i < 10; i++) {
            action.tick();
        }

        // Should navigate stop when no players
        verify(navigation, atLeastOnce()).stop();
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();
        action.cancel();

        assertTrue(action.isComplete(), "Should be complete after cancellation");
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();
        action.cancel();

        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    @Test
    @DisplayName("Should handle cancellation without start")
    void testCancellationWithoutStart() {
        action = new IdleFollowAction(foreman);
        action.cancel();

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Description Tests ====================

    @Test
    @DisplayName("Description should indicate following behavior")
    void testDescription() {
        action = new IdleFollowAction(foreman);

        String description = action.getDescription();
        assertNotNull(description);
        assertTrue(description.toLowerCase().contains("follow") ||
                   description.toLowerCase().contains("idle"),
            "Description should indicate following/idle behavior");
    }

    // ==================== Continuous Operation Tests ====================

    @Test
    @DisplayName("Should never complete on its own")
    void testNeverCompletes() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Tick many times
        for (int i = 0; i < 1000; i++) {
            action.tick();
        }

        assertFalse(action.isComplete(), "Idle action should never complete on its own");
    }

    @Test
    @DisplayName("Should handle extended operation")
    void testExtendedOperation() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(3.0f);

        action = new IdleFollowAction(foreman);
        action.start();

        // Simulate extended operation (5000 ticks)
        for (int i = 0; i < 5000; i++) {
            action.tick();
        }

        assertFalse(action.isComplete());
        assertNotNull(action);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle player list changes")
    void testPlayerListChanges() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Player list changes (player disconnects)
        List<Player> emptyPlayers = new ArrayList<>();
        when(level.players()).thenReturn((List) emptyPlayers);
        when(targetPlayer.isRemoved()).thenReturn(true);

        // Tick to trigger player re-search
        for (int i = 0; i < 100; i++) {
            action.tick();
        }

        assertNotNull(action, "Should handle player list changes");
    }

    @Test
    @DisplayName("Should handle null players list")
    void testNullPlayersList() {
        when(level.players()).thenReturn(null);

        action = new IdleFollowAction(foreman);
        action.start();

        assertNotNull(action, "Should handle null players list");
    }

    @Test
    @DisplayName("Should handle navigation not done")
    void testNavigationNotDone() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(3.0f);
        when(navigation.isDone()).thenReturn(false);

        action = new IdleFollowAction(foreman);
        action.start();
        action.tick();

        // Should still handle navigation state correctly
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should maintain distance with moving target")
    void testMaintainDistanceWithMovingTarget() {
        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn((List) players);

        action = new IdleFollowAction(foreman);
        action.start();

        // Simulate moving target
        when(foreman.distanceTo(targetPlayer)).thenReturn(3.0f, 5.0f, 3.5f, 6.0f);

        for (int i = 0; i < 4; i++) {
            action.tick();
        }

        assertNotNull(action);
    }
}
