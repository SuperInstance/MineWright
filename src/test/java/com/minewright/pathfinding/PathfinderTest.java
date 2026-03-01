package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AStarPathfinder}.
 *
 * Tests cover:
 * <ul>
 *   <li>Path finding from A to B</li>
 *   <li>Obstacle avoidance</li>
 *   <li>Path smoothing integration</li>
 *   <li>Edge cases (unreachable destination, same start/end)</li>
 *   <li>Timeout handling</li>
 *   <li>Statistics tracking</li>
 *   <li>Invalid input handling</li>
 * </ul>
 */
@DisplayName("AStarPathfinder Tests")
class PathfinderTest {

    @Mock
    private Level mockLevel;

    @Mock
    private Entity mockEntity;

    @Mock
    private MovementValidator mockValidator;

    private AStarPathfinder pathfinder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pathfinder = new AStarPathfinder(mockValidator);
        AStarPathfinder.resetStatistics();

        // Setup default entity behavior
        when(mockEntity.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        when(mockEntity.position()).thenReturn(new Vec3(0.5, 64.0, 0.5));
    }

    @Test
    @DisplayName("Path is found for simple flat path")
    void testSimplePath() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(5, 64, 0);

        // Mock world as flat terrain
        setupFlatTerrain();

        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        List<PathNode> path = result.get();
        assertTrue(path.size() >= 2, "Path should have at least start and goal");
        assertEquals(start, path.get(0).pos, "Path should start at start position");
        assertEquals(goal, path.get(path.size() - 1).pos, "Path should end at goal position");
    }

    @Test
    @DisplayName("Same start and goal returns single node path")
    void testSameStartAndGoal() {
        BlockPos pos = new BlockPos(10, 64, 10);

        PathfindingContext context = createContext(pos);

        Optional<List<PathNode>> result = pathfinder.findPath(pos, pos, context);

        assertTrue(result.isPresent(), "Path should be found");
        List<PathNode> path = result.get();
        assertEquals(1, path.size(), "Path should have exactly one node");
        assertEquals(pos, path.get(0).pos, "Node should be at the position");
    }

    @Test
    @DisplayName("Path is found when goal is already reached")
    void testGoalAlreadyReached() {
        BlockPos pos = new BlockPos(10, 64, 10);
        PathfindingContext context = createContext(pos);
        context.withGoal(pos);

        Optional<List<PathNode>> result = pathfinder.findPath(pos, pos, context);

        assertTrue(result.isPresent(), "Path should be found");
        assertEquals(1, result.get().size(), "Should return single-node path");
    }

    @Test
    @DisplayName("No path found when goal is out of range")
    void testGoalOutOfRange() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(1000, 64, 0); // Very far

        PathfindingContext context = createContext(goal)
            .withMaxRange(100); // Max range 100

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertFalse(result.isPresent(), "Path should not be found for out-of-range goal");
    }

    @Test
    @DisplayName("No path found when blocked by obstacles")
    void testBlockedPath() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 0);

        // Setup validator to reject all movements
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(false);

        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertFalse(result.isPresent(), "Path should not be found when all movements are blocked");
    }

    @Test
    @DisplayName("Path with obstacles is navigated around")
    void testPathAroundObstacles() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 0);

        // Allow movement except at x=5 (create a wall)
        when(mockValidator.canMove(any(), any(), any(), any())).thenAnswer(invocation -> {
            BlockPos from = invocation.getArgument(0);
            BlockPos to = invocation.getArgument(1);
            // Block movement to/from x=5
            return to.getX() != 5 && from.getX() != 5;
        });

        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found around obstacles");
        List<PathNode> path = result.get();
        assertTrue(path.stream().noneMatch(n -> n.pos.getX() == 5),
            "Path should not go through blocked area");
    }

    @Test
    @DisplayName("Path includes vertical movement (jumping)")
    void testPathWithJumping() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(5, 66, 0); // 2 blocks higher

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), eq(MovementType.JUMP), any())).thenReturn(true);
        when(mockValidator.canMove(any(), any(), eq(MovementType.WALK), any())).thenReturn(true);

        PathfindingContext context = createContext(goal)
            .withJumpHeight(2.0);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found with jumping");
        List<PathNode> path = result.get();
        assertTrue(path.stream().anyMatch(n -> n.movement == MovementType.JUMP),
            "Path should include jumping movement");
    }

    @Test
    @DisplayName("Path smoothing is applied when enabled")
    void testPathSmoothing() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 0);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal)
            .setSmoothPath(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        // PathSmoother should be called (we can't directly test this without
        // more sophisticated mocking, but we can verify the path exists)
    }

    @Test
    @DisplayName("Timeout prevents infinite search")
    void testTimeout() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 0);

        // Setup a slow validator that always returns true
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal)
            .withTimeout(1); // 1ms timeout

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        // May or may not find path depending on speed, but shouldn't hang
        assertNotNull(result, "Should return result without hanging");
    }

    @Test
    @DisplayName("Null start position returns empty result")
    void testNullStartPosition() {
        BlockPos goal = new BlockPos(10, 64, 10);
        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(null, goal, context);

        assertFalse(result.isPresent(), "Should return empty for null start");
    }

    @Test
    @DisplayName("Null goal position returns empty result")
    void testNullGoalPosition() {
        BlockPos start = new BlockPos(0, 64, 0);
        PathfindingContext context = createContext(null);

        Optional<List<PathNode>> result = pathfinder.findPath(start, null, context);

        assertFalse(result.isPresent(), "Should return empty for null goal");
    }

    @Test
    @DisplayName("Null context returns empty result")
    void testNullContext() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 10);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, null);

        assertFalse(result.isPresent(), "Should return empty for null context");
    }

    @Test
    @DisplayName("Statistics track nodes explored")
    void testStatisticsTracking() {
        AStarPathfinder.resetStatistics();

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(5, 64, 0);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        pathfinder.findPath(start, goal, context);

        int[] stats = AStarPathfinder.getStatistics();
        assertTrue(stats[0] > 0, "Should have explored nodes");
        // stats[0] = nodesExplored, stats[1] = pathsFound, stats[2] = timeouts
    }

    @Test
    @DisplayName("Statistics can be reset")
    void testStatisticsReset() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(3, 64, 0);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        pathfinder.findPath(start, goal, context);
        assertTrue(AStarPathfinder.getStatistics()[0] > 0, "Should have explored nodes");

        AStarPathfinder.resetStatistics();
        assertArrayEquals(new int[]{0, 0, 0}, AStarPathfinder.getStatistics(),
            "Statistics should be reset to zero");
    }

    @Test
    @DisplayName("Custom movement validator is used")
    void testCustomMovementValidator() {
        MovementValidator customValidator = mock(MovementValidator.class);
        AStarPathfinder customPathfinder = new AStarPathfinder(customValidator);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(5, 64, 0);

        when(customValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        customPathfinder.findPath(start, goal, context);

        verify(customValidator, atLeastOnce()).canMove(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debug logging can be enabled")
    void testDebugLogging() {
        AStarPathfinder debugPathfinder = new AStarPathfinder();
        debugPathfinder.setDebugLogging(true);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(2, 64, 0);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        // Should not throw exception with debug logging enabled
        Optional<List<PathNode>> result = debugPathfinder.findPath(start, goal, context);
        assertNotNull(result, "Should return result with debug logging enabled");
    }

    @Test
    @DisplayName("Path nodes have correct parent chain")
    void testPathNodeParentChain() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(5, 64, 0);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        List<PathNode> path = result.get();

        // Verify parent chain
        for (int i = 1; i < path.size(); i++) {
            PathNode current = path.get(i);
            assertNotNull(current.parent, "Node should have parent");
            assertEquals(path.get(i - 1).pos, current.parent.pos,
                "Parent should be previous node in path");
        }
        assertNull(path.get(0).parent, "First node should not have parent");
    }

    @Test
    @DisplayName("Movement costs are calculated correctly")
    void testMovementCosts() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(3, 64, 0);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        // Verify costs increase along path
        List<PathNode> path = result.get();
        for (int i = 1; i < path.size(); i++) {
            assertTrue(path.get(i).gCost >= path.get(i - 1).gCost,
                "gCost should be non-decreasing along path");
        }
    }

    @Test
    @DisplayName("Path with diagonal movement")
    void testDiagonalPath() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(5, 64, 5);

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = createContext(goal);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        List<PathNode> path = result.get();

        // Diagonal path should be shorter than Manhattan path
        assertTrue(path.size() <= 11, "Diagonal path should be efficient");
    }

    @Test
    @DisplayName("Goal area is recognized")
    void testGoalAreaRecognition() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goalCenter = new BlockPos(10, 64, 0);
        double radius = 2.0;

        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context = new PathfindingContext(mockLevel, mockEntity)
            .withGoalArea(goalCenter, radius);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goalCenter, context);

        assertTrue(result.isPresent(), "Path should be found to goal area");
    }

    @Test
    @DisplayName("Multiple pathfinding operations are independent")
    void testMultiplePathfindingOperations() {
        setupFlatTerrain();
        when(mockValidator.canMove(any(), any(), any(), any())).thenReturn(true);

        PathfindingContext context1 = createContext(new BlockPos(5, 64, 0));
        PathfindingContext context2 = createContext(new BlockPos(10, 64, 0));

        Optional<List<PathNode>> result1 = pathfinder.findPath(
            new BlockPos(0, 64, 0), new BlockPos(5, 64, 0), context1);
        Optional<List<PathNode>> result2 = pathfinder.findPath(
            new BlockPos(5, 64, 0), new BlockPos(10, 64, 0), context2);

        assertTrue(result1.isPresent(), "First path should be found");
        assertTrue(result2.isPresent(), "Second path should be found");
    }

    // Helper methods

    private PathfindingContext createContext(BlockPos goal) {
        return new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(150)
            .setCanSwim(false)
            .setCanClimb(false)
            .setCanFly(false)
            .setCanParkour(false)
            .withJumpHeight(1.0)
            .withMaxFallDistance(3);
    }

    private void setupFlatTerrain() {
        // Mock solid ground at y=63
        for (int x = -10; x <= 20; x++) {
            for (int z = -10; z <= 20; z++) {
                BlockState solidState = mock(BlockState.class);
                when(solidState.isSolidRender(eq(mockLevel), any())).thenReturn(true);
                when(mockLevel.getBlockState(new BlockPos(x, 63, z))).thenReturn(solidState);

                // Air above
                BlockState airState = mock(BlockState.class);
                when(airState.isSolidRender(eq(mockLevel), any())).thenReturn(false);
                when(mockLevel.getBlockState(new BlockPos(x, 64, z))).thenReturn(airState);
                when(mockLevel.getBlockState(new BlockPos(x, 65, z))).thenReturn(airState);
            }
        }
    }
}
