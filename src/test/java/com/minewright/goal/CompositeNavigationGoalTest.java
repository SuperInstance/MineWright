package com.minewright.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the CompositeNavigationGoal and goal composition system.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>ANY goal composition (complete any child)</li>
 *   <li>ALL goal composition (complete all children)</li>
 *   <li>Goal priority handling</li>
 *   <li>Goal conflict resolution</li>
 *   <li>Dynamic goal switching</li>
 *   <li>GetToBlockGoal functionality</li>
 *   <li>GetToEntityGoal functionality</li>
 *   <li>RunAwayGoal functionality</li>
 *   <li>Edge cases (null goals, empty composites)</li>
 * </ul>
 *
 * @see CompositeNavigationGoal
 * @see NavigationGoal
 * @see Goals
 * @since 1.0.0
 */
@DisplayName("Composite Navigation Goal Tests")
class CompositeNavigationGoalTest {

    private WorldState mockWorldState;
    private Level mockLevel;
    private BlockPos origin;
    private BlockPos pos1;
    private BlockPos pos2;
    private BlockPos pos3;
    private BlockPos farPos;

    @BeforeEach
    void setUp() {
        mockWorldState = mock(WorldState.class);
        mockLevel = mock(Level.class);
        origin = new BlockPos(0, 64, 0);
        pos1 = new BlockPos(10, 64, 0);
        pos2 = new BlockPos(0, 64, 10);
        pos3 = new BlockPos(10, 64, 10);
        farPos = new BlockPos(100, 64, 100);

        // Setup default mock behavior
        when(mockWorldState.getCurrentPosition()).thenReturn(origin);
        when(mockWorldState.getLevel()).thenReturn(mockLevel);
        when(mockWorldState.distanceTo(any(BlockPos.class))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            return Math.sqrt(origin.distSqr(pos));
        });
        when(mockLevel.getMinBuildHeight()).thenReturn(-64);
        when(mockLevel.getMaxBuildHeight()).thenReturn(320);
        when(mockLevel.getGameTime()).thenReturn(100L);
    }

    // ========== ANY Composition Tests ==========

    @Test
    @DisplayName("ANY composition: Complete when one sub-goal is satisfied")
    void testAnyComposition_OneSubGoalSatisfied() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        when(mockWorldState.getCurrentPosition()).thenReturn(pos1);
        assertTrue(composite.isComplete(mockWorldState),
            "ANY composition should be complete when at least one sub-goal is satisfied");
    }

    @Test
    @DisplayName("ANY composition: Incomplete when no sub-goals are satisfied")
    void testAnyComposition_NoSubGoalsSatisfied() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        assertFalse(composite.isComplete(mockWorldState),
            "ANY composition should not be complete when no sub-goals are satisfied");
    }

    @Test
    @DisplayName("ANY composition: Returns minimum heuristic (closest goal)")
    void testAnyComposition_MinimumHeuristic() {
        NavigationGoal goal1 = Goals.gotoPos(pos1); // distance 10
        NavigationGoal goal2 = Goals.gotoPos(pos2); // distance 10
        NavigationGoal goal3 = Goals.gotoPos(pos3); // distance ~14.14
        NavigationGoal composite = Goals.anyOf(goal1, goal2, goal3);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos3)).thenReturn(14.14);

        assertEquals(10.0, composite.getHeuristic(mockWorldState), 0.01,
            "ANY composition should return minimum heuristic");
    }

    @Test
    @DisplayName("ANY composition: Returns closest goal's target position")
    void testAnyComposition_ClosestTargetPosition() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(farPos);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(farPos)).thenReturn(100.0);

        BlockPos target = composite.getTargetPosition(mockWorldState);
        assertEquals(pos1, target,
            "ANY composition should return closest goal's target position");
    }

    @Test
    @DisplayName("ANY composition: Valid when at least one sub-goal is valid")
    void testAnyComposition_Validity() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        assertTrue(composite.isValid(mockWorldState),
            "ANY composition should be valid when at least one sub-goal is valid");
    }

    // ========== ALL Composition Tests ==========

    @Test
    @DisplayName("ALL composition: Complete only when all sub-goals are satisfied")
    void testAllComposition_AllSubGoalsMustBeSatisfied() {
        NavigationGoal goal1 = Goals.nearPos(pos1, 0.1);
        NavigationGoal goal2 = Goals.nearPos(pos2, 0.1);
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        when(mockWorldState.getCurrentPosition()).thenReturn(pos1);
        when(mockWorldState.isWithin(pos1, 0.1)).thenReturn(true);
        when(mockWorldState.isWithin(pos2, 0.1)).thenReturn(false);

        assertFalse(composite.isComplete(mockWorldState),
            "ALL composition should not be complete when only some sub-goals are satisfied");
    }

    @Test
    @DisplayName("ALL composition: Complete when all sub-goals are satisfied")
    void testAllComposition_AllSubGoalsSatisfied() {
        NavigationGoal goal1 = Goals.nearPos(origin, 100.0);
        NavigationGoal goal2 = Goals.nearPos(origin, 100.0);
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        when(mockWorldState.isWithin(origin, 100.0)).thenReturn(true);

        assertTrue(composite.isComplete(mockWorldState),
            "ALL composition should be complete when all sub-goals are satisfied");
    }

    @Test
    @DisplayName("ALL composition: Returns sum of all heuristics")
    void testAllComposition_SumHeuristic() {
        NavigationGoal goal1 = Goals.gotoPos(pos1); // distance 10
        NavigationGoal goal2 = Goals.gotoPos(pos2); // distance 10
        NavigationGoal goal3 = Goals.gotoPos(pos3); // distance ~14.14
        NavigationGoal composite = Goals.allOf(goal1, goal2, goal3);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos3)).thenReturn(14.14);

        assertEquals(34.14, composite.getHeuristic(mockWorldState), 0.01,
            "ALL composition should return sum of all heuristics");
    }

    @Test
    @DisplayName("ALL composition: Returns furthest goal's target position")
    void testAllComposition_FurthestTargetPosition() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(farPos);
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(farPos)).thenReturn(100.0);

        BlockPos target = composite.getTargetPosition(mockWorldState);
        assertEquals(farPos, target,
            "ALL composition should return furthest goal's target position");
    }

    @Test
    @DisplayName("ALL composition: Valid only when all sub-goals are valid")
    void testAllComposition_Validity() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        assertTrue(composite.isValid(mockWorldState),
            "ALL composition should be valid when all sub-goals are valid");
    }

    // ========== Goal Priority and Conflict Resolution Tests ==========

    @Test
    @DisplayName("Goal priority: ANY composition prefers closer goals")
    void testGoalPriority_AnyPrefersCloser() {
        NavigationGoal closeGoal = Goals.gotoPos(pos1);
        NavigationGoal mediumGoal = Goals.gotoPos(pos2);
        NavigationGoal farGoal = Goals.gotoPos(farPos);
        NavigationGoal composite = Goals.anyOf(closeGoal, mediumGoal, farGoal);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(20.0);
        when(mockWorldState.distanceTo(farPos)).thenReturn(100.0);

        assertEquals(10.0, composite.getHeuristic(mockWorldState), 0.01,
            "ANY composition should prefer closer goals");
    }

    @Test
    @DisplayName("Goal priority: ALL composition accounts for all distances")
    void testGoalPriority_AllAccountsForAll() {
        NavigationGoal closeGoal = Goals.gotoPos(pos1);
        NavigationGoal farGoal = Goals.gotoPos(farPos);
        NavigationGoal composite = Goals.allOf(closeGoal, farGoal);

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(farPos)).thenReturn(100.0);

        assertEquals(110.0, composite.getHeuristic(mockWorldState), 0.01,
            "ALL composition should account for all distances");
    }

    @Test
    @DisplayName("Goal conflict: Conflicting ANY goals resolve to closest")
    void testGoalConflict_AnyResolvesToClosest() {
        NavigationGoal goal1 = Goals.gotoPos(new BlockPos(10, 64, 0));
        NavigationGoal goal2 = Goals.gotoPos(new BlockPos(-10, 64, 0));
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        when(mockWorldState.distanceTo(new BlockPos(10, 64, 0))).thenReturn(10.0);
        when(mockWorldState.distanceTo(new BlockPos(-10, 64, 0))).thenReturn(10.0);

        // Both are equidistant, should pick one deterministically
        double heuristic = composite.getHeuristic(mockWorldState);
        assertEquals(10.0, heuristic, 0.01,
            "Conflicting ANY goals should resolve to closest");
    }

    @Test
    @DisplayName("Goal conflict: Conflicting ALL goals require visiting all")
    void testGoalConflict_AllRequiresAll() {
        NavigationGoal goal1 = Goals.gotoPos(new BlockPos(10, 64, 0));
        NavigationGoal goal2 = Goals.gotoPos(new BlockPos(-10, 64, 0));
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        when(mockWorldState.getCurrentPosition()).thenReturn(origin);
        assertFalse(composite.isComplete(mockWorldState),
            "Conflicting ALL goals should require visiting all");

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(10, 64, 0));
        assertFalse(composite.isComplete(mockWorldState),
            "Should still be incomplete when only at first goal");
    }

    // ========== Dynamic Goal Switching Tests ==========

    @Test
    @DisplayName("Dynamic switching: ANY composition updates target as position changes")
    void testDynamicSwitching_AnyUpdatesTarget() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal composite = Goals.anyOf(goal1, goal2);

        // Initially closer to pos1
        when(mockWorldState.getCurrentPosition()).thenReturn(origin);
        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(10.0);
        assertEquals(pos1, composite.getTargetPosition(mockWorldState),
            "Should target pos1 initially");

        // Move closer to pos2
        when(mockWorldState.getCurrentPosition()).thenReturn(pos2);
        when(mockWorldState.distanceTo(pos1)).thenReturn(14.14);
        when(mockWorldState.distanceTo(pos2)).thenReturn(0.0);
        assertEquals(pos2, composite.getTargetPosition(mockWorldState),
            "Should switch to pos2 when closer");
    }

    @Test
    @DisplayName("Dynamic switching: ALL composition tracks completion progress")
    void testDynamicSwitching_AllTracksProgress() {
        NavigationGoal goal1 = Goals.nearPos(pos1, 0.1);
        NavigationGoal goal2 = Goals.nearPos(pos2, 0.1);
        NavigationGoal composite = Goals.allOf(goal1, goal2);

        // At origin - none complete
        when(mockWorldState.getCurrentPosition()).thenReturn(origin);
        when(mockWorldState.isWithin(pos1, 0.1)).thenReturn(false);
        when(mockWorldState.isWithin(pos2, 0.1)).thenReturn(false);
        assertFalse(composite.isComplete(mockWorldState));

        // At pos1 - one complete
        when(mockWorldState.getCurrentPosition()).thenReturn(pos1);
        when(mockWorldState.isWithin(pos1, 0.1)).thenReturn(true);
        when(mockWorldState.isWithin(pos2, 0.1)).thenReturn(false);
        assertFalse(composite.isComplete(mockWorldState));

        // At pos2 - all complete
        when(mockWorldState.getCurrentPosition()).thenReturn(pos2);
        when(mockWorldState.isWithin(pos1, 0.1)).thenReturn(true);
        when(mockWorldState.isWithin(pos2, 0.1)).thenReturn(true);
        assertTrue(composite.isComplete(mockWorldState));
    }

    // ========== GetToBlockGoal Tests ==========

    @Test
    @DisplayName("GetToBlockGoal: Finds nearest block of target type")
    void testGetToBlockGoal_FindsNearest() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, 64, 2.0);
        BlockPos nearestIron = new BlockPos(5, 64, 0);

        when(mockWorldState.findNearestBlock(ironOre, 64)).thenReturn(nearestIron);

        assertEquals(nearestIron, goal.getTargetPosition(mockWorldState),
            "Should find nearest iron ore");
    }

    @Test
    @DisplayName("GetToBlockGoal: Is complete when near target block")
    void testGetToBlockGoal_CompleteWhenNear() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, 64, 2.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(1, 64, 0));
        when(mockWorldState.isLoaded(any(BlockPos.class))).thenReturn(true);
        when(mockWorldState.getBlockState(any(BlockPos.class))).thenReturn(ironOre);

        assertTrue(goal.isComplete(mockWorldState),
            "Should be complete when near target block");
    }

    @Test
    @DisplayName("GetToBlockGoal: Not complete when target block not nearby")
    void testGetToBlockGoal_NotCompleteWhenFar() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, 64, 2.0);

        when(mockWorldState.isLoaded(any(BlockPos.class))).thenReturn(true);
        when(mockWorldState.getBlockState(any(BlockPos.class)))
            .thenReturn(Blocks.STONE.defaultBlockState());

        assertFalse(goal.isComplete(mockWorldState),
            "Should not be complete when target block not nearby");
    }

    @Test
    @DisplayName("GetToBlockGoal: Returns MAX_VALUE heuristic when no blocks found")
    void testGetToBlockGoal_NoBlocksReturnsMaxHeuristic() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, 64, 2.0);

        when(mockWorldState.findNearestBlock(ironOre, 64)).thenReturn(null);

        assertEquals(Double.MAX_VALUE, goal.getHeuristic(mockWorldState),
            "Should return MAX_VALUE when no blocks found");
    }

    @Test
    @DisplayName("GetToBlockGoal: Invalid when no target blocks exist")
    void testGetToBlockGoal_InvalidWhenNoBlocks() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, 64, 2.0);

        when(mockWorldState.findNearestBlock(ironOre, 64)).thenReturn(null);

        assertFalse(goal.isValid(mockWorldState),
            "Should be invalid when no target blocks exist");
    }

    @Test
    @DisplayName("GetToBlockGoal: Cache can be cleared")
    void testGetToBlockGoal_ClearCache() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, 64, 2.0);

        assertDoesNotThrow(goal::clearCache,
            "Clearing cache should not throw exception");
    }

    // ========== GetToEntityGoal Tests ==========

    @Test
    @DisplayName("GetToEntityGoal: Finds nearest entity of target type")
    void testGetToEntityGoal_FindsNearest() {
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(Player.class, 64, 2.0);
        Player mockPlayer = mock(Player.class);
        BlockPos playerPos = new BlockPos(5, 64, 0);

        when(mockPlayer.blockPosition()).thenReturn(playerPos);
        when(mockPlayer.isAlive()).thenReturn(true);
        when(mockWorldState.findNearestEntity(Player.class, 64)).thenReturn(mockPlayer);

        assertEquals(playerPos, goal.getTargetPosition(mockWorldState),
            "Should find nearest player");
    }

    @Test
    @DisplayName("GetToEntityGoal: Is complete when near target entity")
    void testGetToEntityGoal_CompleteWhenNear() {
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(Player.class, 64, 2.0);
        Player mockPlayer = mock(Player.class);
        BlockPos playerPos = new BlockPos(1, 64, 0);

        when(mockPlayer.blockPosition()).thenReturn(playerPos);
        when(mockPlayer.isAlive()).thenReturn(true);
        when(mockWorldState.findNearestEntity(Player.class, 64)).thenReturn(mockPlayer);
        when(mockWorldState.isWithin(playerPos, 2.0)).thenReturn(true);

        assertTrue(goal.isComplete(mockWorldState),
            "Should be complete when near target entity");
    }

    @Test
    @DisplayName("GetToEntityGoal: Not complete when entity not nearby")
    void testGetToEntityGoal_NotCompleteWhenFar() {
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(Player.class, 64, 2.0);

        when(mockWorldState.findNearestEntity(Player.class, 64)).thenReturn(null);

        assertFalse(goal.isComplete(mockWorldState),
            "Should not be complete when entity not nearby");
    }

    @Test
    @DisplayName("GetToEntityGoal: Returns MAX_VALUE heuristic when no entities found")
    void testGetToEntityGoal_NoEntitiesReturnsMaxHeuristic() {
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(Player.class, 64, 2.0);

        when(mockWorldState.findNearestEntity(Player.class, 64)).thenReturn(null);

        assertEquals(Double.MAX_VALUE, goal.getHeuristic(mockWorldState),
            "Should return MAX_VALUE when no entities found");
    }

    @Test
    @DisplayName("GetToEntityGoal: Invalid when no target entities exist")
    void testGetToEntityGoal_InvalidWhenNoEntities() {
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(Player.class, 64, 2.0);

        when(mockWorldState.findNearestEntity(Player.class, 64)).thenReturn(null);

        assertFalse(goal.isValid(mockWorldState),
            "Should be invalid when no target entities exist");
    }

    @Test
    @DisplayName("GetToEntityGoal: Cache can be cleared")
    void testGetToEntityGoal_ClearCache() {
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(Player.class, 64, 2.0);

        assertDoesNotThrow(goal::clearCache,
            "Clearing cache should not throw exception");
    }

    // ========== RunAwayGoal Tests ==========

    @Test
    @DisplayName("RunAwayGoal: Complete when at safe distance")
    void testRunAwayGoal_CompleteWhenSafe() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);
        BlockPos safePos = new BlockPos(15, 64, 0);

        assertTrue(goal.isComplete(safePos),
            "Should be complete when at safe distance");
    }

    @Test
    @DisplayName("RunAwayGoal: Not complete when too close to danger")
    void testRunAwayGoal_NotCompleteWhenUnsafe() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);
        BlockPos unsafePos = new BlockPos(5, 64, 0);

        assertFalse(goal.isComplete(unsafePos),
            "Should not be complete when too close to danger");
    }

    @Test
    @DisplayName("RunAwayGoal: Zero heuristic when already safe")
    void testRunAwayGoal_ZeroHeuristicWhenSafe() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(15, 64, 0));

        assertEquals(0.0, goal.getHeuristic(mockWorldState), 0.001,
            "Should have zero heuristic when already safe");
    }

    @Test
    @DisplayName("RunAwayGoal: Positive heuristic when need to escape")
    void testRunAwayGoal_PositiveHeuristicWhenUnsafe() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(5, 64, 0));

        assertEquals(5.0, goal.getHeuristic(mockWorldState), 0.001,
            "Should have positive heuristic when need to escape");
    }

    @Test
    @DisplayName("RunAwayGoal: Target position is away from danger")
    void testRunAwayGoal_TargetPositionAwayFromDanger() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 20.0);

        when(mockWorldState.getCurrentPosition()).thenReturn(new BlockPos(5, 64, 0));

        BlockPos target = goal.getTargetPosition(mockWorldState);

        assertTrue(target.getX() > 5,
            "Target position should be away from danger");
    }

    @Test
    @DisplayName("RunAwayGoal: Always valid")
    void testRunAwayGoal_AlwaysValid() {
        BlockPos danger = new BlockPos(0, 64, 0);
        RunAwayGoal goal = new RunAwayGoal(danger, 10.0);

        assertTrue(goal.isValid(mockWorldState),
            "RunAwayGoal should always be valid");
    }

    // ========== Edge Cases and Error Handling ==========

    @Test
    @DisplayName("Edge case: ANY composition with empty goals throws exception")
    void testEdgeCase_AnyEmptyGoalsThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            Goals.anyOf(new NavigationGoal[0]);
        }, "ANY composition with empty goals should throw exception");
    }

    @Test
    @DisplayName("Edge case: ALL composition with empty goals throws exception")
    void testEdgeCase_AllEmptyGoalsThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            Goals.allOf(new NavigationGoal[0]);
        }, "ALL composition with empty goals should throw exception");
    }

    @Test
    @DisplayName("Edge case: ANY composition with null goals throws exception")
    void testEdgeCase_AnyNullGoalsThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            Goals.anyOf((NavigationGoal[]) null);
        }, "ANY composition with null goals should throw exception");
    }

    @Test
    @DisplayName("Edge case: ALL composition with null goals throws exception")
    void testEdgeCase_AllNullGoalsThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            Goals.allOf((NavigationGoal[]) null);
        }, "ALL composition with null goals should throw exception");
    }

    @Test
    @DisplayName("Edge case: CompositeNavigationGoal with empty list throws exception")
    void testEdgeCase_CompositeWithEmptyListThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CompositeNavigationGoal(Collections.emptyList(),
                CompositeNavigationGoal.CompositionType.ANY);
        }, "CompositeNavigationGoal with empty list should throw exception");
    }

    @Test
    @DisplayName("Edge case: CompositeNavigationGoal with null list throws exception")
    void testEdgeCase_CompositeWithNullListThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CompositeNavigationGoal((List<NavigationGoal>) null,
                CompositeNavigationGoal.CompositionType.ANY);
        }, "CompositeNavigationGoal with null list should throw exception");
    }

    @Test
    @DisplayName("Edge case: Single goal in ANY returns unwrapped goal")
    void testEdgeCase_SingleGoalInAnyReturnsUnwrapped() {
        NavigationGoal single = Goals.gotoPos(pos1);
        NavigationGoal composite = Goals.anyOf(single);

        assertSame(single, composite,
            "ANY composition with single goal should return unwrapped goal");
    }

    @Test
    @DisplayName("Edge case: Single goal in ALL returns unwrapped goal")
    void testEdgeCase_SingleGoalInAllReturnsUnwrapped() {
        NavigationGoal single = Goals.gotoPos(pos1);
        NavigationGoal composite = Goals.allOf(single);

        assertSame(single, composite,
            "ALL composition with single goal should return unwrapped goal");
    }

    // ========== Equals and HashCode Tests ==========

    @Test
    @DisplayName("Equals: Same composite goals are equal")
    void testEquals_SameCompositesAreEqual() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);

        CompositeNavigationGoal composite1 = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );
        CompositeNavigationGoal composite2 = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );

        assertEquals(composite1, composite2,
            "Same composite goals should be equal");
    }

    @Test
    @DisplayName("Equals: Different composition types are not equal")
    void testEquals_DifferentTypesNotEqual() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);

        CompositeNavigationGoal anyComposite = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );
        CompositeNavigationGoal allComposite = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ALL
        );

        assertNotEquals(anyComposite, allComposite,
            "Different composition types should not be equal");
    }

    @Test
    @DisplayName("Equals: Different goal lists are not equal")
    void testEquals_DifferentGoalsNotEqual() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);
        NavigationGoal goal3 = Goals.gotoPos(pos3);

        CompositeNavigationGoal composite1 = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );
        CompositeNavigationGoal composite2 = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal3),
            CompositeNavigationGoal.CompositionType.ANY
        );

        assertNotEquals(composite1, composite2,
            "Different goal lists should not be equal");
    }

    @Test
    @DisplayName("HashCode: Equal composites have same hash code")
    void testHashCode_EqualCompositesSameHash() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);

        CompositeNavigationGoal composite1 = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );
        CompositeNavigationGoal composite2 = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );

        assertEquals(composite1.hashCode(), composite2.hashCode(),
            "Equal composites should have same hash code");
    }

    // ========== ToString Tests ==========

    @Test
    @DisplayName("ToString: Contains composition type and goals")
    void testToString_ContainsTypeAndGoals() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);

        CompositeNavigationGoal composite = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );

        String str = composite.toString();
        assertTrue(str.contains("ANY"), "ToString should contain composition type");
        assertTrue(str.contains("goals"), "ToString should contain goals");
    }

    // ========== Getter Tests ==========

    @Test
    @DisplayName("GetGoals: Returns immutable list of sub-goals")
    void testGetGoals_ReturnsImmutableList() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);

        CompositeNavigationGoal composite = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );

        List<NavigationGoal> goals = composite.getGoals();
        assertNotNull(goals, "Goals list should not be null");
        assertEquals(2, goals.size(), "Goals list should contain 2 goals");

        // Verify immutability by attempting to modify
        assertThrows(UnsupportedOperationException.class, () -> {
            goals.add(Goals.gotoPos(pos3));
        }, "Goals list should be immutable");
    }

    @Test
    @DisplayName("GetType: Returns correct composition type")
    void testGetType_ReturnsCorrectType() {
        NavigationGoal goal1 = Goals.gotoPos(pos1);
        NavigationGoal goal2 = Goals.gotoPos(pos2);

        CompositeNavigationGoal anyComposite = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ANY
        );
        CompositeNavigationGoal allComposite = new CompositeNavigationGoal(
            Arrays.asList(goal1, goal2),
            CompositeNavigationGoal.CompositionType.ALL
        );

        assertEquals(CompositeNavigationGoal.CompositionType.ANY, anyComposite.getType(),
            "Should return ANY type");
        assertEquals(CompositeNavigationGoal.CompositionType.ALL, allComposite.getType(),
            "Should return ALL type");
    }

    // ========== GetToBlockGoal Getter Tests ==========

    @Test
    @DisplayName("GetToBlockGoal: Getters return correct values")
    void testGetToBlockGoal_Getters() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        int searchRadius = 50;
        double completionDistance = 3.0;
        GetToBlockGoal goal = new GetToBlockGoal(ironOre, searchRadius, completionDistance);

        assertEquals(ironOre, goal.getTargetBlockState(),
            "Should return correct target block state");
        assertEquals(searchRadius, goal.getSearchRadius(),
            "Should return correct search radius");
        assertEquals(completionDistance, goal.getCompletionDistance(), 0.001,
            "Should return correct completion distance");
    }

    // ========== GetToEntityGoal Getter Tests ==========

    @Test
    @DisplayName("GetToEntityGoal: Getters return correct values")
    void testGetToEntityGoal_Getters() {
        Class<Player> entityType = Player.class;
        int searchRadius = 50;
        double completionDistance = 3.0;
        GetToEntityGoal<Player> goal = new GetToEntityGoal<>(
            entityType, searchRadius, completionDistance);

        assertEquals(entityType, goal.getEntityType(),
            "Should return correct entity type");
        assertEquals(searchRadius, goal.getSearchRadius(),
            "Should return correct search radius");
        assertEquals(completionDistance, goal.getCompletionDistance(), 0.001,
            "Should return correct completion distance");
    }

    // ========== RunAwayGoal Getter Tests ==========

    @Test
    @DisplayName("RunAwayGoal: Getters return correct values")
    void testRunAwayGoal_Getters() {
        BlockPos danger = new BlockPos(10, 64, 20);
        double safeDistance = 15.0;
        int searchRadius = 50;
        RunAwayGoal goal = new RunAwayGoal(danger, safeDistance, searchRadius);

        assertEquals(danger, goal.getDangerPos(),
            "Should return correct danger position");
        assertEquals(safeDistance, goal.getSafeDistance(), 0.001,
            "Should return correct safe distance");
        assertEquals(searchRadius, goal.getSearchRadius(),
            "Should return correct search radius");
    }

    // ========== Complex Scenario Tests ==========

    @Test
    @DisplayName("Complex scenario: Nested composite goals")
    void testComplexScenario_NestedComposites() {
        // Create nested composite: anyOf(pos1, allOf(pos2, pos3))
        NavigationGoal innerComposite = Goals.allOf(
            Goals.gotoPos(pos2),
            Goals.gotoPos(pos3)
        );
        NavigationGoal outerComposite = Goals.anyOf(
            Goals.gotoPos(pos1),
            innerComposite
        );

        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos2)).thenReturn(10.0);
        when(mockWorldState.distanceTo(pos3)).thenReturn(14.14);

        // Should choose the closer option (pos1 with distance 10)
        double heuristic = outerComposite.getHeuristic(mockWorldState);
        assertEquals(10.0, heuristic, 0.01,
            "Nested composite should choose closer option");
    }

    @Test
    @DisplayName("Complex scenario: Multiple goal types in composition")
    void testComplexScenario_MultipleGoalTypes() {
        BlockState ironOre = Blocks.IRON_ORE.defaultBlockState();
        BlockPos ironPos = new BlockPos(5, 64, 0);

        GetToBlockGoal blockGoal = new GetToBlockGoal(ironOre, 64, 2.0);
        NavigationGoal posGoal = Goals.gotoPos(pos1);
        NavigationGoal composite = Goals.anyOf(blockGoal, posGoal);

        when(mockWorldState.findNearestBlock(ironOre, 64)).thenReturn(ironPos);
        when(mockWorldState.distanceTo(ironPos)).thenReturn(5.0);
        when(mockWorldState.distanceTo(pos1)).thenReturn(10.0);

        assertEquals(ironPos, composite.getTargetPosition(mockWorldState),
            "Should choose closer goal regardless of type");
        assertEquals(5.0, composite.getHeuristic(mockWorldState), 0.01,
            "Should return heuristic of closer goal");
    }
}
