package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the navigation goal system.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Basic position goals</li>
 *   <li>Composite goals (ANY/ALL)</li>
 *   <li>Block search goals</li>
 *   <li>Entity tracking goals</li>
 *   <li>Run away goals</li>
 *   <li>Heuristic calculations</li>
 *   <li>Goal completion logic</li>
 * </ul>
 *
 * @see NavigationGoal
 * @see Goals
 * @since 1.0.0
 */
class NavigationGoalTest {

    private WorldState mockWorldState;
    private BlockPos origin;
    private BlockPos pos1;
    private BlockPos pos2;
    private BlockPos pos3;

    @BeforeEach
    void setUp() {
        mockWorldState = mock(WorldState.class);
        origin = new BlockPos(0, 64, 0);
        pos1 = new BlockPos(10, 64, 0);
        pos2 = new BlockPos(0, 64, 10);
        pos3 = new BlockPos(10, 64, 10);

        // Setup default mock behavior
        when(mockWorldState.getCurrentPosition()).thenReturn(origin);
        when(mockWorldState.distanceTo(any(BlockPos.class))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            return Math.sqrt(origin.distSqr(pos));
        });
    }

    // ========== Position Goal Tests ==========

    @Test
    void testPositionGoal_IsComplete_AtTarget() {
        NavigationGoal goal = Goals.gotoPos(10, 64, 20);
        BlockPos target = new BlockPos(10, 64, 20);

        assertTrue(goal.isComplete(target));
    }

    @Test
    void testPositionGoal_IsComplete_NotAtTarget() {
        NavigationGoal goal = Goals.gotoPos(10, 64, 20);
        BlockPos wrong = new BlockPos(5, 64, 10);

        assertFalse(goal.isComplete(wrong));
    }

    @Test
    void testPositionGoal_GetTargetPosition() {
        BlockPos expected = new BlockPos(10, 64, 20);
        NavigationGoal goal = Goals.gotoPos(expected);

        assertEquals(expected, goal.getTargetPosition(mockWorldState));
    }

    @Test
    void testPositionGoal_GetHeuristic() {
        NavigationGoal goal = Goals.gotoPos(pos1);

        // Distance from (0,64,0) to (10,64,0) is 10
        assertEquals(10.0, goal.getHeuristic(mockWorldState), 0.001);
    }

    // ========== Near Position Goal Tests ==========

    @Test
    void testNearPositionGoal_IsComplete_WithinRadius() {
        NavigationGoal goal = Goals.nearPos(origin, 5.0);

        // Position within radius
        assertTrue(goal.isComplete(new BlockPos(3, 64, 4))); // distance = 5
    }

    @Test
    void testNearPositionGoal_IsComplete_OutsideRadius() {
        NavigationGoal goal = Goals.nearPos(origin, 5.0);

        // Position outside radius
        assertFalse(goal.isComplete(new BlockPos(10, 64, 0))); // distance = 10
    }

    @Test
    void testNearPositionGoal_GetHeuristic_EdgeOfRadius() {
        NavigationGoal goal = Goals.nearPos(pos1, 5.0);

        // Distance from origin to pos1 is 10
        // Heuristic should be 10 - 5 = 5 (distance to edge of radius)
        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        assertEquals(5.0, goal.getHeuristic(mockWorldState), 0.001);
    }

    // ========== Composite ANY Goal Tests ==========

    @Test
    void testCompositeAny_IsComplete_OneSatisfied() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        // At pos1, so composite should be complete
        when(mockWorldState.getCurrentPosition()).thenReturn(pos1);
        assertTrue(composite.isComplete(mockWorldState));
    }

    @Test
    void testCompositeAny_IsComplete_NoneSatisfied() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        // At origin, neither goal satisfied
        assertFalse(composite.isComplete(mockWorldState));
    }

    @Test
    void testCompositeAny_GetHeuristic_Minimum() {
        NavigationGoal goal1 = Goals.gotoPos(pos1); // distance 10
        NavigationGoal goal2 = Goals.gotoPos(pos2); // distance 10
        NavigationGoal goal3 = Goals.gotoPos(pos3); // distance ~14.14
        NavigationGoal composite = Goals.anyOf(goal1, goal2, goal3);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos3)).thenReturn(14.14);

        // Should return minimum heuristic
        assertEquals(10.0, composite.getHeuristic(mockWorldState), 0.01);
    }

    @Test
    void testCompositeAny_SingleGoalOptimization() {
        NavigationGoal single = Goals.gotoPos(pos1);
        NavigationGoal composite = Goals.anyOf(single);

        // Should return the same goal, not wrap it
        assertSame(single, composite);
    }

    // ========== Composite ALL Goal Tests ==========

    @Test
    void testCompositeAll_IsComplete_AllSatisfied() {
        NavigationGoal goal1 = Goals.nearPos(pos1, 0.1); // Nearly at pos1
        NavigationGoal goal2 = Goals.nearPos(pos2, 0.1); // Nearly at pos2
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        // At origin, neither is satisfied (too far)
        assertFalse(composite.isComplete(mockWorldState));

        // At pos1, only one is satisfied
        when(mockWorldState.getCurrentPosition()).thenReturn(pos1);
        when(mockWorldState.isWithin(pos1, 0.1)).thenReturn(true);
        when(mockWorldState.isWithin(pos2, 0.1)).thenReturn(false);
        assertFalse(composite.isComplete(mockWorldState));
    }

    @Test
    void testCompositeAll_GetHeuristic_Sum() {
        NavigationGoal goal1 = Goals.gotoPos(pos1); // distance 10
        NavigationGoal goal2 = Goals.gotoPos(pos2); // distance 10
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(10.0);

        // Should return sum of all heuristics
        assertEquals(20.0, composite.getHeuristic(mockWorldState), 0.001);
    }

    // ========== GetToBlockGoal Tests ==========

    @Test
    void testGetToBlockGoal_IsComplete_NearBlock() {
        GetToBlockGoal goal = new GetToBlockGoal(Blocks.IRON_ORE.defaultBlockState(), 64, 2.0);

        // Mock world state with iron ore nearby
        when(mockWorldState.isLoaded(any(BlockPos.class))).thenReturn(true);
        when(mockWorldState.getBlockState(any(BlockPos.class)))
            .thenReturn(Blocks.IRON_ORE.defaultBlockState());
        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(1, 64, 0));

        assertTrue(goal.isComplete(mockWorldState));
    }

    @Test
    void testGetToBlockGoal_IsComplete_NoBlockNearby() {
        GetToBlockGoal goal = new GetToBlockGoal(Blocks.IRON_ORE.defaultBlockState(), 64, 2.0);

        // Mock world state with no iron ore
        when(mockWorldState.isLoaded(any(BlockPos.class))).thenReturn(true);
        when(mockWorldState.getBlockState(any(BlockPos.class)))
            .thenReturn(Blocks.STONE.defaultBlockState());

        assertFalse(goal.isComplete(mockWorldState));
    }

    @Test
    void testGetToBlockGoal_GetTargetPosition_FindsNearest() {
        BlockPos nearestIron = new BlockPos(5, 64, 0);
        GetToBlockGoal goal = new GetToBlockGoal(Blocks.IRON_ORE.defaultBlockState(), 64, 2.0);

        when(mockWorldState.findNearestBlock(any(), anyInt())).thenReturn(nearestIron);

        assertEquals(nearestIron, goal.getTargetPosition(mockWorldState));
    }

    @Test
    void testGetToBlockGoal_GetHeuristic_NoBlocksFound() {
        GetToBlockGoal goal = new GetToBlockGoal(Blocks.IRON_ORE.defaultBlockState(), 64, 2.0);

        when(mockWorldState.findNearestBlock(any(), anyInt())).thenReturn(null);

        assertEquals(Double.MAX_VALUE, goal.getHeuristic(mockWorldState));
    }

    // ========== GetToEntityGoal Tests ==========

    @Test
    void testGetToEntityGoal_IsComplete_NearEntity() {
        GetToEntityGoal<net.minecraft.world.entity.player.Player> goal =
            new GetToEntityGoal<>(net.minecraft.world.entity.player.Player.class, 64, 2.0);

        // Mock player nearby
        net.minecraft.world.entity.player.Player mockPlayer = mock(net.minecraft.world.entity.player.Player.class);
        when(mockPlayer.blockPosition()).thenReturn(new BlockPos(1, 64, 0));
        when(mockPlayer.isAlive()).thenReturn(true);
        when(mockWorldState.findNearestEntity(any(), anyInt())).thenReturn(mockPlayer);
        when(mockWorldState.isWithin(any(BlockPos.class), anyDouble())).thenReturn(true);

        assertTrue(goal.isComplete(mockWorldState));
    }

    @Test
    void testGetToEntityGoal_IsComplete_NoEntityFound() {
        GetToEntityGoal<net.minecraft.world.entity.player.Player> goal =
            new GetToEntityGoal<>(net.minecraft.world.entity.player.Player.class, 64, 2.0);

        when(mockWorldState.findNearestEntity(any(), anyInt())).thenReturn(null);

        assertFalse(goal.isComplete(mockWorldState));
    }

    @Test
    void testGetToEntityGoal_GetHeuristic_NoEntityFound() {
        GetToEntityGoal<net.minecraft.world.entity.player.Player> goal =
            new GetToEntityGoal<>(net.minecraft.world.entity.player.Player.class, 64, 2.0);

        when(mockWorldState.findNearestEntity(any(), anyInt())).thenReturn(null);

        assertEquals(Double.MAX_VALUE, goal.getHeuristic(mockWorldState));
    }

    @Test
    void testGetToEntityGoal_ClearCache() {
        GetToEntityGoal<net.minecraft.world.entity.player.Player> goal =
            new GetToEntityGoal<>(net.minecraft.world.entity.player.Player.class, 64, 2.0);

        // Should not throw exception
        assertDoesNotThrow(goal::clearCache);
    }

    // ========== RunAwayGoal Tests ==========

    @Test
    void testRunAwayGoal_IsComplete_SafeDistance() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        // Position 15 blocks away is safe
        assertTrue(goal.isComplete(new BlockPos(15, 64, 0)));
    }

    @Test
    void testRunAwayGoal_IsComplete_UnsafeDistance() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        // Position 5 blocks away is unsafe
        assertFalse(goal.isComplete(new BlockPos(5, 64, 0)));
    }

    @Test
    void testRunAwayGoal_GetHeuristic_AlreadySafe() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(15, 64, 0));

        // Already safe, heuristic should be 0
        assertEquals(0.0, goal.getHeuristic(mockWorldState), 0.001);
    }

    @Test
    void testRunAwayGoal_GetHeuristic_NeedToEscape() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(5, 64, 0));

        // 5 blocks away, need 5 more to be safe
        assertEquals(5.0, goal.getHeuristic(mockWorldState), 0.001);
    }

    @Test
    void testRunAwayGoal_GetTargetPosition_DirectionAwayFromDanger() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 20.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(5, 64, 0));

        BlockPos target = goal.getTargetPosition(mockWorldState);

        // Target should be in direction away from danger (positive X)
        assertTrue(target.getX() > 5);
    }

    // ========== Factory Method Tests ==========

    @Test
    void testGoals_GotoPos_ThreeArgs() {
        NavigationGoal goal = Goals.gotoPos(10, 64, 20);

        assertNotNull(goal);
        assertTrue(goal.isComplete(new BlockPos(10, 64, 20)));
    }

    @Test
    void testGoals_GotoPos_BlockPos() {
        BlockPos pos = new BlockPos(10, 64, 20);
        NavigationGoal goal = Goals.gotoPos(pos);

        assertNotNull(goal);
        assertEquals(pos, goal.getTargetPosition(mockWorldState));
    }

    @Test
    void testGoals_GotoBlock() {
        NavigationGoal goal = Goals.gotoBlock(Blocks.IRON_ORE);

        assertNotNull(goal);
        assertTrue(goal instanceof GetToBlockGoal);
    }

    @Test
    void testGoals_GotoAnyBlock() {
        NavigationGoal goal = Goals.gotoAnyBlock(Blocks.IRON_ORE, Blocks.GOLD_ORE);

        assertNotNull(goal);
        assertTrue(goal instanceof CompositeNavigationGoal);
    }

    @Test
    void testGoals_RunAway() {
        BlockPos danger = new BlockPos(0, 64, 0);
        NavigationGoal goal = Goals.runAway(danger, 20.0);

        assertNotNull(goal);
        assertTrue(goal instanceof RunAwayGoal);
    }

    // ========== Edge Cases ==========

    @Test
    void testCompositeAny_EmptyGoals_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Goals.anyOf(new NavigationGoal[0]);
        });
    }

    @Test
    void testCompositeAll_EmptyGoals_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Goals.allOf(new NavigationGoal[0]);
        });
    }

    @Test
    void testGoals_InvalidEntityType() {
        GetToEntityGoal<net.minecraft.world.entity.monster.Creeper> goal =
            new GetToEntityGoal<>(net.minecraft.world.entity.monster.Creeper.class, 64, 2.0);

        // No creepers nearby
        when(mockWorldState.findNearestEntity(any(), anyInt())).thenReturn(null);

        assertFalse(goal.isValid(mockWorldState));
        assertEquals(Double.MAX_VALUE, goal.getHeuristic(mockWorldState));
    }
}
