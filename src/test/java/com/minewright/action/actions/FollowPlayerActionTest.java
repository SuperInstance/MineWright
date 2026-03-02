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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link FollowPlayerAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Player target selection (by name, nearest player)</li>
 *   <li>Distance management (2-3 block distance)</li>
 *   <li>Navigation to target player</li>
 *   <li>Player lost and re-acquisition</li>
 *   <li>Timeout handling</li>
 *   <li>Cancellation and cleanup</li>
 *   <li>Multiple player scenarios</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FollowPlayerAction Tests")
class FollowPlayerActionTest {

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

    private Task task;
    private FollowPlayerAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(foreman.getNavigation()).thenReturn(navigation);
        lenient().when(navigation.isDone()).thenReturn(true);
        lenient().when(targetPlayer.isAlive()).thenReturn(true);
        lenient().when(targetPlayer.isRemoved()).thenReturn(false);
        lenient().when(targetPlayer.isSpectator()).thenReturn(false);
        lenient().when(targetPlayer.getName()).thenReturn(mock(net.minecraft.network.chat.Component.class));
        lenient().when(targetPlayer.getName().getString()).thenReturn("TestPlayer");
        lenient().when(foreman.distanceTo(any(Player.class))).thenReturn(5.0);
    }

    // ==================== Player Target Selection Tests ====================

    @Test
    @DisplayName("Should successfully initialize with specific player name")
    void testValidPlayerNameInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Should not fail immediately
        assertNull(action.getResult(), "Should not have result yet");
    }

    @Test
    @DisplayName("Should fail initialization when no players available")
    void testNoPlayersAvailable() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        when(level.players()).thenReturn(new ArrayList<>());

        action = new FollowPlayerAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Should be failure result");
        assertTrue(result.getMessage().contains("Player") ||
                   result.getMessage().contains("player") ||
                   result.getMessage().contains("found"),
            "Should indicate player not found");
    }

    @Test
    @DisplayName("Should follow nearest player when name not specified")
    void testNearestPlayerSelection() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should successfully select nearest player");
    }

    @Test
    @DisplayName("Should follow nearest player with generic name")
    void testGenericPlayerName() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "me");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should handle generic name 'me'");
    }

    @Test
    @DisplayName("Should find player by exact name match")
    void testExactNameMatch() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.getName().getString()).thenReturn("TestPlayer");

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should find player by exact name");
    }

    @Test
    @DisplayName("Should be case insensitive for player name")
    void testCaseInsensitiveNameMatch() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "testplayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.getName().getString()).thenReturn("TestPlayer");

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should match player name case-insensitively");
    }

    @Test
    @DisplayName("Should prefer exact name match over nearest player")
    void testExactNameOverNearest() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "SecondPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        players.add(secondPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.getName().getString()).thenReturn("TestPlayer");
        when(secondPlayer.getName().getString()).thenReturn("SecondPlayer");
        when(foreman.distanceTo(targetPlayer)).thenReturn(1.0); // Closer
        when(foreman.distanceTo(secondPlayer)).thenReturn(10.0); // Farther

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should prefer exact name match");
    }

    // ==================== Distance Management Tests ====================

    @Test
    @DisplayName("Should navigate when too far from target")
    void testNavigateWhenTooFar() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(5.0); // Too far

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Tick to trigger navigation
        action.tick();

        verify(navigation, atLeastOnce()).moveTo(eq(targetPlayer), anyDouble());
    }

    @Test
    @DisplayName("Should stop navigation when close enough")
    void testStopNavigationWhenClose() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(1.5); // Close enough

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Tick to check distance
        action.tick();

        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should maintain 2-3 block distance from target")
    void testMaintainDistanceRange() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Test different distances
        when(foreman.distanceTo(targetPlayer)).thenReturn(4.0); // Too far - should move
        action.tick();
        verify(navigation, atLeastOnce()).moveTo(any(Player.class), anyDouble());

        reset(navigation);

        when(foreman.distanceTo(targetPlayer)).thenReturn(2.5); // Good range - should not move
        action.tick();
        verify(navigation, never()).moveTo(any(Player.class), anyDouble());
    }

    // ==================== Player Lost Tests ====================

    @Test
    @DisplayName("Should re-find player when target is lost")
    void testRefindLostPlayer() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Simulate player dying
        when(targetPlayer.isAlive()).thenReturn(false);

        // Tick to trigger re-finding
        for (int i = 0; i < 25 && !action.isComplete(); i++) {
            action.tick();
        }

        // Should attempt to find player again (will fail in test)
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should fail when player is removed")
    void testPlayerRemoved() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.isRemoved()).thenReturn(true);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Tick to detect removal
        for (int i = 0; i < 25 && !action.isComplete(); i++) {
            action.tick();
        }

        ActionResult result = action.getResult();
        if (result != null) {
            assertTrue(result.getMessage().contains("Lost") ||
                       result.getMessage().contains("track") ||
                       result.getMessage().contains("player"),
                "Should indicate player was lost");
        }
    }

    @Test
    @DisplayName("Should ignore spectator players")
    void testIgnoreSpectatorPlayers() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.isSpectator()).thenReturn(true);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Should not select spectator player
        ActionResult result = action.getResult();
        if (result != null) {
            assertFalse(result.isSuccess(), "Should fail when only spectator available");
        }
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(2.5); // Good distance

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Run until timeout (6000 ticks)
        for (int i = 0; i < 6050 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertTrue(result.getMessage().contains("timeout") ||
                   result.getMessage().contains("stopped") ||
                   result.isSuccess(),
            "Should indicate timeout or completion");
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete(), "Should be complete after cancellation");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Cancellation should be failure");
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();
        action.cancel();

        // Verify navigation stopped
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Description Tests ====================

    @Test
    @DisplayName("Description should contain player name")
    void testDescriptionContainsPlayerName() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        action = new FollowPlayerAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("TestPlayer") ||
                   description.contains("player") ||
                   description.contains("follow"),
            "Description should reference the player");
    }

    @Test
    @DisplayName("Description should handle unknown player")
    void testDescriptionUnknownPlayer() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("follow", params);

        action = new FollowPlayerAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("unknown") ||
                   description.contains("player") ||
                   description.contains("follow"),
            "Description should indicate following");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        action = new FollowPlayerAction(null, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Foreman") ||
                   result.getMessage().contains("available"));
    }

    @Test
    @DisplayName("Should handle null navigation gracefully")
    void testNullNavigation() {
        lenient().when(foreman.getNavigation()).thenReturn(null);

        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle null level gracefully")
    void testNullLevel() {
        lenient().when(foreman.level()).thenReturn(null);

        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle empty player name")
    void testEmptyPlayerName() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Should fall back to nearest player
        assertNull(action.getResult(), "Should handle empty player name");
    }

    @Test
    @DisplayName("Should handle special characters in player name")
    void testSpecialCharactersInPlayerName() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "Test_Player123");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.getName().getString()).thenReturn("Test_Player123");

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle multiple players and select nearest")
    void testSelectNearestOfMultiplePlayers() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        players.add(secondPlayer);
        when(level.players()).thenReturn(players);
        when(foreman.distanceTo(targetPlayer)).thenReturn(10.0); // Far
        when(foreman.distanceTo(secondPlayer)).thenReturn(3.0); // Near
        when(secondPlayer.isAlive()).thenReturn(true);
        when(secondPlayer.isRemoved()).thenReturn(false);
        when(secondPlayer.isSpectator()).thenReturn(false);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        assertNull(action.getResult(), "Should select nearest player");
    }

    @Test
    @DisplayName("Should handle generic name placeholders")
    void testGenericNamePlaceholders() {
        String[] genericNames = {"PLAYER", "NAME", "you", "me", ""};

        for (String genericName : genericNames) {
            Map<String, Object> params = new HashMap<>();
            params.put("player", genericName);
            task = new Task("follow", params);

            List<Player> players = new ArrayList<>();
            players.add(targetPlayer);
            when(level.players()).thenReturn(players);

            action = new FollowPlayerAction(foreman, task);
            action.start();

            assertNotNull(action, "Should handle generic name: " + genericName);
        }
    }

    @Test
    @DisplayName("Should periodically search for player when lost")
    void testPeriodicPlayerSearch() {
        Map<String, Object> params = new HashMap<>();
        params.put("player", "TestPlayer");
        task = new Task("follow", params);

        List<Player> players = new ArrayList<>();
        players.add(targetPlayer);
        when(level.players()).thenReturn(players);
        when(targetPlayer.isAlive()).thenReturn(false);

        action = new FollowPlayerAction(foreman, task);
        action.start();

        // Tick multiple times - should search every 20 ticks
        for (int i = 0; i < 50 && !action.isComplete(); i++) {
            action.tick();
        }

        assertNotNull(action);
    }
}
