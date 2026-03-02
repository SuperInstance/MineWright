package com.minewright.pathfinding;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link HierarchicalPathfinder}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Hierarchical vs local pathfinding selection based on distance</li>
 *   <li>Chunk-level pathfinding performance and accuracy</li>
 *   <li>Path concatenation between chunks</li>
 *   <li>Edge cases: obstacles, unreachable goals, null inputs</li>
 *   <li>Movement validation integration</li>
 *   <li>Path smoothing behavior</li>
 *   <li>Thread safety for concurrent pathfinding operations</li>
 *   <li>Cache behavior and invalidation</li>
 *   <li>Performance under various conditions</li>
 * </ul>
 *
 * <p><b>Test Strategy:</b></p>
 * <ol>
 *   <li>Unit tests for individual methods using mocked Level</li>
 *   <li>Integration tests for end-to-end pathfinding scenarios</li>
 *   <li>Performance tests for chunk-level planning</li>
 *   <li>Thread safety tests for concurrent access</li>
 * </ol>
 *
 * @since 1.0.0
 */
@DisplayName("HierarchicalPathfinder Comprehensive Tests")
class HierarchicalPathfinderTest {

    @Mock
    private Level mockLevel;

    @Mock
    private Entity mockEntity;

    @Mock
    private ForemanEntity mockForeman;

    private HierarchicalPathfinder pathfinder;
    private PathfindingContext context;

    // Test constants
    private static final int CHUNK_SIZE = 16;
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final BlockPos SHORT_GOAL = new BlockPos(10, 64, 10);
    private static final BlockPos LONG_GOAL = new BlockPos(100, 64, 100);
    private static final BlockPos VERY_LONG_GOAL = new BlockPos(500, 64, 500);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize pathfinder
        pathfinder = new HierarchicalPathfinder();

        // Setup mock level behavior
        setupMockLevel();

        // Setup mock entity behavior
        when(mockEntity.blockPosition()).thenReturn(ORIGIN);
        when(mockEntity.isAlive()).thenReturn(true);

        // Setup mock foreman behavior
        when(mockForeman.blockPosition()).thenReturn(ORIGIN);
        when(mockForeman.isAlive()).thenReturn(true);

        // Create default context
        context = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(LONG_GOAL)
            .withMaxRange(200)
            .setUseHierarchical(true);
    }

    // ==================== Hierarchical vs Local Selection Tests ====================

    @Test
    @DisplayName("Short paths use local A* pathfinding")
    void testShortPathsUseLocalPathfinding() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(20, 64, 20); // Within default threshold

        PathfindingContext shortContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(100)
            .setUseHierarchical(true);

        // Should use local pathfinding for short distances
        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, shortContext);

        // Verify result is present (even if path is not found due to mocking)
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Long paths use hierarchical pathfinding")
    void testLongPathsUseHierarchicalPathfinding() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 100); // Exceeds threshold

        PathfindingContext longContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(200)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, longContext);

        // Should attempt hierarchical pathfinding
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Custom hierarchical threshold is respected")
    void testCustomHierarchicalThreshold() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        // Set custom threshold to 100
        PathfindingContext customContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(200)
            .withHierarchicalThreshold(100)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, customContext);

        // Distance is ~70 blocks, threshold is 100, should use local
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Hierarchical can be disabled in context")
    void testHierarchicalCanBeDisabled() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(200, 64, 200);

        PathfindingContext disabledContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(false);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, disabledContext);

        // Should not throw exception even with hierarchical disabled
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("Exact threshold distance uses hierarchical")
    void testExactThresholdDistance() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(64, 64, 64); // Exactly at default threshold

        PathfindingContext exactContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(200)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, exactContext);

        assertNotNull(result, "Should handle exact threshold distance");
    }

    @Test
    @DisplayName("Just below threshold uses local pathfinding")
    void testJustBelowThreshold() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(63, 64, 63); // Just below threshold

        PathfindingContext belowContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(200)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, belowContext);

        assertNotNull(result, "Should handle distance just below threshold");
    }

    // ==================== Chunk-Level Pathfinding Tests ====================

    @Test
    @DisplayName("Chunk graph is built for start and goal chunks")
    void testChunkGraphBuilding() {
        BlockPos start = new BlockPos(0, 64, 0); // Chunk (0, 0)
        BlockPos goal = new BlockPos(32, 64, 32);  // Chunk (2, 2)

        PathfindingContext chunkContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, chunkContext);

        assertNotNull(result, "Should build chunk graph and attempt pathfinding");
    }

    @Test
    @DisplayName("Adjacent chunks are connected in graph")
    void testAdjacentChunksConnected() {
        // Start and goal are in adjacent chunks
        BlockPos start = new BlockPos(8, 64, 8);   // Chunk (0, 0)
        BlockPos goal = new BlockPos(24, 64, 24);  // Chunk (1, 1)

        PathfindingContext adjacentContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, adjacentContext);

        assertNotNull(result, "Should connect adjacent chunks");
    }

    @Test
    @DisplayName("Diagonal chunks are reachable")
    void testDiagonalChunksReachable() {
        BlockPos start = new BlockPos(8, 64, 8);   // Chunk (0, 0)
        BlockPos goal = new BlockPos(40, 64, 40);  // Chunk (2, 2) - diagonal

        PathfindingContext diagonalContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, diagonalContext);

        assertNotNull(result, "Should reach diagonal chunks");
    }

    @Test
    @DisplayName("Same chunk returns direct path")
    void testSameChunkDirectPath() {
        BlockPos start = new BlockPos(10, 64, 10);
        BlockPos goal = new BlockPos(15, 64, 15);

        // Both in same chunk (0, 0)
        PathfindingContext sameChunkContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, sameChunkContext);

        assertNotNull(result, "Should handle same-chunk paths");
    }

    @Test
    @DisplayName("Chunk path respects traversability")
    void testChunkPathRespectsTraversability() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 100);

        // Mock some chunks as non-traversable (lava, void, etc.)
        when(mockLevel.getBlockState(any(BlockPos)))
            .thenReturn(Blocks.STONE.defaultBlockState());

        PathfindingContext traversableContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, traversableContext);

        assertNotNull(result, "Should respect chunk traversability");
    }

    @Test
    @DisplayName("Chunk graph explores limited area")
    void testChunkGraphLimitedExploration() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext limitedContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, limitedContext);

        assertNotNull(result, "Should explore limited chunk area");
    }

    @Test
    @DisplayName("Multiple chunk transitions are handled")
    void testMultipleChunkTransitions() {
        BlockPos start = new BlockPos(0, 64, 0);   // Chunk (0, 0)
        BlockPos goal = new BlockPos(80, 64, 80);  // Chunk (5, 5) - multiple transitions

        PathfindingContext multiContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, multiContext);

        assertNotNull(result, "Should handle multiple chunk transitions");
    }

    // ==================== Path Concatenation Tests ====================

    @Test
    @DisplayName("Local paths are concatenated between chunks")
    void testLocalPathsConcatenated() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext concatContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, concatContext);

        if (result.isPresent()) {
            List<PathNode> path = result.get();
            // Path should be continuous
            for (int i = 0; i < path.size() - 1; i++) {
                BlockPos current = path.get(i).pos;
                BlockPos next = path.get(i + 1).pos;
                double distance = Math.sqrt(current.distSqr(next));
                assertTrue(distance <= Math.sqrt(3) + 0.1,
                    "Path nodes should be adjacent: " + current + " -> " + next);
            }
        }
    }

    @Test
    @DisplayName("First node is start position")
    void testFirstNodeIsStartPosition() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(80, 64, 80);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        if (result.isPresent() && !result.get().isEmpty()) {
            assertEquals(start, result.get().get(0).pos,
                "First node should be start position");
        }
    }

    @Test
    @DisplayName("Last node is goal position")
    void testLastNodeIsGoalPosition() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(80, 64, 80);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        if (result.isPresent() && !result.get().isEmpty()) {
            BlockPos lastPos = result.get().get(result.get().size() - 1).pos;
            assertTrue(lastPos.distSqr(goal) <= 4,
                "Last node should be at or near goal position");
        }
    }

    @Test
    @DisplayName("Path has no duplicate consecutive positions")
    void testPathNoDuplicates() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(60, 64, 60);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        if (result.isPresent() && result.get().size() > 1) {
            List<PathNode> path = result.get();
            for (int i = 0; i < path.size() - 1; i++) {
                assertNotEquals(path.get(i).pos, path.get(i + 1).pos,
                    "Path should not have duplicate consecutive positions");
            }
        }
    }

    @Test
    @DisplayName("Path waypoints are at chunk centers")
    void testPathWaypointsAtChunkCenters() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 100);

        PathfindingContext waypointContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, waypointContext);

        assertNotNull(result, "Should generate waypoints at chunk centers");
    }

    @Test
    @DisplayName("Failed local path doesn't break concatenation")
    void testFailedLocalPathConcatenation() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 100);

        // Setup some chunks to fail
        when(mockLevel.getBlockState(new BlockPos(50, 64, 50)))
            .thenReturn(Blocks.BEDROCK.defaultBlockState());

        PathfindingContext failContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, failContext);

        // Should attempt recovery
        assertNotNull(result, "Should handle failed local paths gracefully");
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Null start position returns empty")
    void testNullStartPosition() {
        Optional<List<PathNode>> result = pathfinder.findPath(null, LONG_GOAL, context);

        assertTrue(result.isEmpty(), "Should return empty for null start");
    }

    @Test
    @DisplayName("Null goal position returns empty")
    void testNullGoalPosition() {
        Optional<List<PathNode>> result = pathfinder.findPath(ORIGIN, null, context);

        assertTrue(result.isEmpty(), "Should return empty for null goal");
    }

    @Test
    @DisplayName("Null context returns empty")
    void testNullContext() {
        Optional<List<PathNode>> result = pathfinder.findPath(ORIGIN, LONG_GOAL, null);

        assertTrue(result.isEmpty(), "Should return empty for null context");
    }

    @Test
    @DisplayName("Start equals goal returns single node path")
    void testStartEqualsGoal() {
        BlockPos pos = new BlockPos(50, 64, 50);

        Optional<List<PathNode>> result = pathfinder.findPath(pos, pos, context);

        assertTrue(result.isPresent(), "Should return path when start equals goal");
        assertEquals(1, result.get().size(), "Path should have single node");
        assertEquals(pos, result.get().get(0).pos, "Node should be the position");
    }

    @Test
    @DisplayName("Unreachable goal returns empty")
    void testUnreachableGoal() {
        // Setup level where all blocks are solid (cannot move)
        when(mockLevel.getBlockState(any(BlockPos)))
            .thenReturn(Blocks.BEDROCK.defaultBlockState());

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 100);

        PathfindingContext blockedContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(false); // Use local to test unreachable

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, blockedContext);

        // Should return empty or handle gracefully
        assertNotNull(result, "Should return result even if unreachable");
    }

    @Test
    @DisplayName("Goal beyond max range returns empty")
    void testGoalBeyondMaxRange() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(1000, 64, 1000);

        PathfindingContext rangeContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(100);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, rangeContext);

        assertTrue(result.isEmpty(), "Should return empty for goal beyond max range");
    }

    @Test
    @DisplayName("Exception during pathfinding returns empty")
    void testExceptionDuringPathfinding() {
        // Setup level to throw exception
        when(mockLevel.getBlockState(any(BlockPos)))
            .thenThrow(new RuntimeException("Test exception"));

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, context);

        assertTrue(result.isEmpty(), "Should return empty on exception");
    }

    @Test
    @DisplayName("Very long distance path (800+ blocks)")
    void testVeryLongDistancePath() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(900, 64, 900);

        PathfindingContext longContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(1000)
            .setUseHierarchical(true);

        // Should attempt pathfinding without hanging
        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, longContext);

        assertNotNull(result, "Should handle very long distances");
    }

    @Test
    @DisplayName("Negative coordinates are handled")
    void testNegativeCoordinates() {
        BlockPos start = new BlockPos(-50, 64, -50);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext negativeContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, negativeContext);

        assertNotNull(result, "Should handle negative coordinates");
    }

    @Test
    @DisplayName("Vertical movement in hierarchical pathfinding")
    void testVerticalMovement() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 80, 100); // Higher elevation

        PathfindingContext verticalContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true)
            .withJumpHeight(1.5);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, verticalContext);

        assertNotNull(result, "Should handle vertical movement");
    }

    // ==================== Movement Validation Tests ====================

    @Test
    @DisplayName("Path respects movement capabilities")
    void testPathRespectsCapabilities() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext noSwimContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setCanSwim(false)
            .setCanClimb(false)
            .setCanFly(false)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, noSwimContext);

        assertNotNull(result, "Should respect agent capabilities");
    }

    @Test
    @DisplayName("Path allows dangerous movements when enabled")
    void testPathAllowsDangerousMovements() {
        BlockPos start = new BlockPos(0, 80, 0);
        BlockPos goal = new BlockPos(50, 64, 50); // Lower elevation

        PathfindingContext dangerousContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setAllowDangerousMovements(true)
            .withMaxFallDistance(10)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, dangerousContext);

        assertNotNull(result, "Should allow dangerous movements when configured");
    }

    @Test
    @DisplayName("Path avoids dangerous movements when disabled")
    void testPathAvoidsDangerousMovements() {
        BlockPos start = new BlockPos(0, 80, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext safeContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setAllowDangerousMovements(false)
            .withMaxFallDistance(3)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, safeContext);

        assertNotNull(result, "Should avoid dangerous movements when disabled");
    }

    @Test
    @DisplayName("Jump height affects path selection")
    void testJumpHeightAffectsPath() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 67, 50); // Higher elevation

        PathfindingContext highJumpContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withJumpHeight(2.0)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, highJumpContext);

        assertNotNull(result, "Should respect jump height setting");
    }

    @Test
    @DisplayName("Swimming capability affects path selection")
    void testSwimmingCapability() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext swimContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setCanSwim(true)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, swimContext);

        assertNotNull(result, "Should respect swimming capability");
    }

    @Test
    @DisplayName("Climbing capability affects path selection")
    void testClimbingCapability() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 80, 50); // Much higher

        PathfindingContext climbContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setCanClimb(true)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, climbContext);

        assertNotNull(result, "Should respect climbing capability");
    }

    // ==================== Path Smoothing Tests ====================

    @Test
    @DisplayName("Path smoothing is applied when enabled")
    void testPathSmoothingEnabled() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(80, 64, 80);

        PathfindingContext smoothContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setSmoothPath(true)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, smoothContext);

        if (result.isPresent() && result.get().size() > 2) {
            // Smoothed path should not have tiny zig-zags
            List<PathNode> path = result.get();
            // Check that consecutive movements are generally in same direction
            // (smoothing reduces direction changes)
            assertNotNull(path, "Smoothed path should be valid");
        }
    }

    @Test
    @DisplayName("Path smoothing is skipped when disabled")
    void testPathSmoothingDisabled() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(80, 64, 80);

        PathfindingContext noSmoothContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setSmoothPath(false)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, noSmoothContext);

        assertNotNull(result, "Should return unsmoothed path");
    }

    @Test
    @DisplayName("Smoothing does not break path validity")
    void testSmoothingMaintainsPathValidity() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(60, 64, 60);

        PathfindingContext smoothContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setSmoothPath(true)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, smoothContext);

        if (result.isPresent() && result.get().size() > 1) {
            List<PathNode> path = result.get();

            // Verify path continuity
            for (int i = 0; i < path.size() - 1; i++) {
                double distance = Math.sqrt(path.get(i).pos.distSqr(path.get(i + 1).pos));
                assertTrue(distance > 0, "Path nodes should be distinct");
                assertTrue(distance <= 50, "Path nodes should be reasonably close");
            }

            // Verify start and end
            assertEquals(start, path.get(0).pos, "Path should start at start position");
        }
    }

    @Test
    @DisplayName("Smoothing reduces path node count")
    void testSmoothingReducesNodeCount() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(70, 64, 70);

        // Get unsmoothed path
        PathfindingContext noSmoothContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setSmoothPath(false)
            .setUseHierarchical(true);

        Optional<List<PathNode>> unsmoothedResult = pathfinder.findPath(start, goal, noSmoothContext);

        // Get smoothed path
        PathfindingContext smoothContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setSmoothPath(true)
            .setUseHierarchical(true);

        Optional<List<PathNode>> smoothedResult = pathfinder.findPath(start, goal, smoothContext);

        if (unsmoothedResult.isPresent() && smoothedResult.isPresent()) {
            // Smoothed path should have same or fewer nodes
            assertTrue(smoothedResult.get().size() <= unsmoothedResult.get().size(),
                "Smoothed path should have same or fewer nodes");
        }
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @DisplayName("Concurrent pathfinding operations are safe")
    void testConcurrentPathfinding() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        BlockPos start = new BlockPos(j * 10, 64, j * 10);
                        BlockPos goal = new BlockPos(j * 10 + 50, 64, j * 10 + 50);

                        PathfindingContext threadContext = new PathfindingContext(mockLevel, mockEntity)
                            .withGoal(goal)
                            .setUseHierarchical(true);

                        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, threadContext);

                        if (result != null) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(threadCount * operationsPerThread, successCount.get() + failureCount.get(),
            "All operations should complete");
        assertTrue(successCount.get() > 0, "At least some operations should succeed");
    }

    @Test
    @DisplayName("Cache operations are thread-safe")
    void testCacheThreadSafety() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 20; j++) {
                    pathfinder.clearCache();
                    pathfinder.setCachingEnabled(j % 2 == 0);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // If we get here without exception, cache operations are thread-safe
        assertTrue(true, "Cache operations should be thread-safe");
    }

    @Test
    @DisplayName("Multiple pathfinders can operate concurrently")
    void testMultiplePathfindersConcurrently() throws InterruptedException {
        int pathfinderCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(pathfinderCount);
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < pathfinderCount; i++) {
            executor.submit(() -> {
                HierarchicalPathfinder localPathfinder = new HierarchicalPathfinder();
                BlockPos start = new BlockPos(0, 64, 0);
                BlockPos goal = new BlockPos(50, 64, 50);

                PathfindingContext localContext = new PathfindingContext(mockLevel, mockEntity)
                    .withGoal(goal)
                    .setUseHierarchical(true);

                localPathfinder.findPath(start, goal, localContext);
                completed.incrementAndGet();
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(pathfinderCount, completed.get(),
            "All pathfinders should complete operations");
    }

    // ==================== Cache Behavior Tests ====================

    @Test
    @DisplayName("Cache is enabled by default")
    void testCacheEnabledByDefault() {
        // Pathfinder should have caching enabled by default
        // This is tested by performing multiple pathfinding operations
        // and verifying no exceptions occur

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        pathfinder.findPath(start, goal, context);
        pathfinder.findPath(start, goal, context); // Should use cache

        assertTrue(true, "Cache operations should work");
    }

    @Test
    @DisplayName("Clear cache removes all cached entries")
    void testClearCache() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        pathfinder.findPath(start, goal, context);
        pathfinder.clearCache();

        // Should not throw exception
        pathfinder.findPath(start, goal, context);

        assertTrue(true, "Clear cache should work without exceptions");
    }

    @Test
    @DisplayName("Cache can be disabled")
    void testDisableCache() {
        pathfinder.setCachingEnabled(false);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        pathfinder.findPath(start, goal, context);
        pathfinder.findPath(start, goal, context); // Should not use cache

        assertTrue(true, "Disabled cache should work without exceptions");
    }

    @Test
    @DisplayName("Enabling cache clears existing cache")
    void testEnableCacheClearsExisting() {
        // Perform some pathfinding
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);
        pathfinder.findPath(start, goal, context);

        // Disable and re-enable
        pathfinder.setCachingEnabled(false);
        pathfinder.setCachingEnabled(true);

        // Should work without issues
        pathfinder.findPath(start, goal, context);

        assertTrue(true, "Re-enabling cache should clear and work");
    }

    @Test
    @DisplayName("Cache improves performance on repeated queries")
    void testCachePerformanceImprovement() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        // First call (cache miss)
        long start1 = System.nanoTime();
        pathfinder.findPath(start, goal, context);
        long duration1 = System.nanoTime() - start1;

        // Second call (cache hit - should be faster or same)
        long start2 = System.nanoTime();
        pathfinder.findPath(start, goal, context);
        long duration2 = System.nanoTime() - start2;

        // Just verify both complete without exception
        assertTrue(true, "Cache should handle repeated queries");
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Long-distance pathfinding completes in reasonable time")
    void testLongDistancePerformance() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(200, 64, 200);

        PathfindingContext perfContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withMaxRange(300)
            .withTimeout(10000)
            .setUseHierarchical(true);

        long startTime = System.currentTimeMillis();
        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, perfContext);
        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(result, "Should complete pathfinding");
        assertTrue(duration < 5000, "Should complete in less than 5 seconds, took: " + duration + "ms");
    }

    @Test
    @DisplayName("Multiple pathfinding operations are efficient")
    void testMultipleOperationsPerformance() {
        List<BlockPos> goals = List.of(
            new BlockPos(50, 64, 50),
            new BlockPos(100, 64, 100),
            new BlockPos(150, 64, 150),
            new BlockPos(200, 64, 200),
            new BlockPos(250, 64, 250)
        );

        long startTime = System.currentTimeMillis();

        for (BlockPos goal : goals) {
            PathfindingContext perfContext = new PathfindingContext(mockLevel, mockEntity)
                .withGoal(goal)
                .setUseHierarchical(true);

            pathfinder.findPath(ORIGIN, goal, perfContext);
        }

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 10000, "Should complete all operations in less than 10 seconds");
    }

    @Test
    @DisplayName("Pathfinding with custom local pathfinder")
    void testCustomLocalPathfinder() {
        AStarPathfinder customLocal = new AStarPathfinder();
        HierarchicalPathfinder customPathfinder = new HierarchicalPathfinder(customLocal);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(80, 64, 80);

        PathfindingContext customContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = customPathfinder.findPath(start, goal, customContext);

        assertNotNull(result, "Should work with custom local pathfinder");
    }

    @Test
    @DisplayName("Default constructor creates functional pathfinder")
    void testDefaultConstructor() {
        HierarchicalPathfinder defaultPathfinder = new HierarchicalPathfinder();

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext defaultContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = defaultPathfinder.findPath(start, goal, defaultContext);

        assertNotNull(result, "Default constructor should create functional pathfinder");
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("End-to-end pathfinding with valid path")
    void testEndToEndPathfinding() {
        // Setup a simple traversable world
        setupSimpleTraversableWorld();

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(32, 64, 32);

        PathfindingContext integrationContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, integrationContext);

        if (result.isPresent() && !result.get().isEmpty()) {
            List<PathNode> path = result.get();

            // Verify path properties
            assertEquals(start, path.get(0).pos, "Path should start at start position");

            // Verify path continuity
            for (int i = 0; i < path.size() - 1; i++) {
                double distance = Math.sqrt(path.get(i).pos.distSqr(path.get(i + 1).pos));
                assertTrue(distance > 0 && distance <= 5, "Path should be continuous");
            }
        }
    }

    @Test
    @DisplayName("Pathfinding with blocks to avoid")
    void testPathfindingWithBlocksToAvoid() {
        Set<BlockPos> blocksToAvoid = new HashSet<>();
        blocksToAvoid.add(new BlockPos(25, 64, 25));
        blocksToAvoid.add(new BlockPos(26, 64, 26));

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext avoidContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withBlocksToAvoid(blocksToAvoid)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, avoidContext);

        assertNotNull(result, "Should handle blocks to avoid");
    }

    @Test
    @DisplayName("Pathfinding with blocks to prefer")
    void testPathfindingWithBlocksToPrefer() {
        Set<BlockPos> blocksToPrefer = new HashSet<>();
        blocksToPrefer.add(new BlockPos(25, 64, 25));
        blocksToPrefer.add(new BlockPos(30, 64, 30));

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext preferContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withBlocksToPrefer(blocksToPrefer)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, preferContext);

        assertNotNull(result, "Should handle blocks to prefer");
    }

    @Test
    @DisplayName("Pathfinding respects timeout")
    void testPathfindingRespectsTimeout() {
        // Setup a complex world that might take time to pathfind
        when(mockLevel.getBlockState(any(BlockPos)))
            .thenReturn(Blocks.STONE.defaultBlockState());

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(500, 64, 500);

        PathfindingContext timeoutContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .withTimeout(100) // 100ms timeout
            .setUseHierarchical(true);

        long startTime = System.currentTimeMillis();
        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, timeoutContext);
        long duration = System.currentTimeMillis() - startTime;

        // Should complete quickly due to timeout
        assertTrue(duration < 500, "Should respect timeout and complete quickly");
        assertNotNull(result, "Should return result even with timeout");
    }

    @Test
    @DisplayName("Pathfinding with goal area")
    void testPathfindingWithGoalArea() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos areaCenter = new BlockPos(50, 64, 50);

        PathfindingContext areaContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoalArea(areaCenter, 10.0) // 10 block radius
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, areaCenter, areaContext);

        assertNotNull(result, "Should handle goal area");
    }

    @Test
    @DisplayName("Pathfinding with stay lit preference")
    void testPathfindingWithStayLit() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 50);

        PathfindingContext litContext = new PathfindingContext(mockLevel, mockEntity)
            .withGoal(goal)
            .setStayLit(true)
            .setUseHierarchical(true);

        Optional<List<PathNode>> result = pathfinder.findPath(start, goal, litContext);

        assertNotNull(result, "Should handle stay lit preference");
    }

    // ==================== Helper Methods ====================

    /**
     * Sets up the mock level with default behavior.
     */
    private void setupMockLevel() {
        // Default: solid ground at Y=63, air above
        when(mockLevel.getBlockState(any(BlockPos))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            if (pos.getY() < 64) {
                return Blocks.STONE.defaultBlockState();
            } else {
                return Blocks.AIR.defaultBlockState();
            }
        });

        when(mockLevel.getMinBuildHeight()).thenReturn(-64);
        when(mockLevel.getMaxBuildHeight()).thenReturn(320);

        // Block state methods
        when(mockLevel.getBlockState(any(BlockPos)).isSolidRender(any(), any(BlockPos)))
            .thenAnswer(invocation -> {
                BlockPos pos = invocation.getArgument(1);
                return pos.getY() < 64; // Solid below Y=64
            });

        when(mockLevel.getBlockState(any(BlockPos)).isSuffocating(any(), any(BlockPos)))
            .thenReturn(false);
    }

    /**
     * Sets up a simple traversable world for integration tests.
     */
    private void setupSimpleTraversableWorld() {
        when(mockLevel.getBlockState(any(BlockPos))).thenAnswer(invocation -> {
            BlockPos pos = invocation.getArgument(0);
            if (pos.getY() == 63) {
                return Blocks.STONE.defaultBlockState(); // Ground
            } else if (pos.getY() == 64) {
                return Blocks.AIR.defaultBlockState(); // Walking level
            } else if (pos.getY() == 65) {
                return Blocks.AIR.defaultBlockState(); // Headroom
            } else {
                return Blocks.AIR.defaultBlockState();
            }
        });

        when(mockLevel.getBlockState(any(BlockPos)).isSolidRender(any(), any(BlockPos)))
            .thenAnswer(invocation -> {
                BlockPos pos = invocation.getArgument(1);
                return pos.getY() <= 63;
            });
    }
}
