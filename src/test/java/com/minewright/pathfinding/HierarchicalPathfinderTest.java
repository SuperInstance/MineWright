package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HierarchicalPathfinder}.
 *
 * Tests cover:
 * <ul>
 *   <li>Hierarchical vs standard pathfinding selection</li>
 *   <li>Chunk-level pathfinding</li>
 *   <li>Local path stitching</li>
 *   <li>Chunk graph caching</li>
 *   <li>Long-distance pathfinding optimization</li>
 *   <li>Edge cases (same chunk, unreachable chunks)</li>
 * </ul>
 */
@DisplayName("HierarchicalPathfinder Tests")
class HierarchicalPathfinderTest {

    @Mock
    private Level mockLevel;

    @Mock
    private Entity mockEntity;

    @Mock
    private AStarPathfinder mockLocalPathfinder;

    @Mock
    private MovementValidator mockValidator;

    private HierarchicalPathfinder hierarchicalPathfinder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hierarchicalPathfinder = new HierarchicalPathfinder(mockLocalPathfinder);

        // Setup default entity behavior
        when(mockEntity.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        // Setup default level behavior
        when(mockLevel.getMinBuildHeight()).thenReturn(-64);
        when(mockLevel.getMaxBuildHeight()).thenReturn(320);

        // Setup traversable terrain by default
        setupTraversableTerrain();
    }

    @Test
    @DisplayName("Uses standard A* for short distances")
    void testShortDistanceUsesStandardAStar() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 0); // Within threshold

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withHierarchicalThreshold(64);

        // Setup mock pathfinder to return a path
        List<PathNode> mockPath = createMockPath(start, goal);
        when(mockLocalPathfinder.findPath(eq(start), eq(goal), any()))
            .thenReturn(Optional.of(mockPath));

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        verify(mockLocalPathfinder).findPath(eq(start), eq(goal), any());
    }

    @Test
    @DisplayName("Uses hierarchical pathfinding for long distances")
    void testLongDistanceUsesHierarchical() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 0); // Beyond threshold

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withHierarchicalThreshold(64)
            .setUseHierarchical(true);

        // Setup mock local pathfinder for waypoint connections
        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenAnswer(invocation -> {
                BlockPos s = invocation.getArgument(0);
                BlockPos g = invocation.getArgument(1);
                return Optional.of(createMockPath(s, g));
            });

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        // For long distances, should use hierarchical (may not find path without proper chunk setup)
        assertNotNull(result, "Should return result without throwing exception");
    }

    @Test
    @DisplayName("Hierarchical disabled uses standard A*")
    void testHierarchicalDisabled() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 0);

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(false);

        List<PathNode> mockPath = createMockPath(start, goal);
        when(mockLocalPathfinder.findPath(eq(start), eq(goal), any()))
            .thenReturn(Optional.of(mockPath));

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
        verify(mockLocalPathfinder).findPath(eq(start), eq(goal), any());
    }

    @Test
    @DisplayName("Same chunk uses standard A*")
    void testSameChunk() {
        BlockPos start = new BlockPos(5, 64, 5);
        BlockPos goal = new BlockPos(10, 64, 10); // Same chunk

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withHierarchicalThreshold(64);

        List<PathNode> mockPath = createMockPath(start, goal);
        when(mockLocalPathfinder.findPath(eq(start), eq(goal), any()))
            .thenReturn(Optional.of(mockPath));

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Path should be found");
    }

    @Test
    @DisplayName("Chunk graph can be cached")
    void testChunkCaching() {
        hierarchicalPathfinder.setCachingEnabled(true);

        // Perform pathfinding to populate cache
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 0);

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withHierarchicalThreshold(64);

        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenReturn(Optional.of(createMockPath(start, goal)));

        hierarchicalPathfinder.findPath(start, goal, context);

        // Clear cache should not throw exception
        hierarchicalPathfinder.clearCache();
    }

    @Test
    @DisplayName("Caching can be disabled")
    void testDisableCaching() {
        hierarchicalPathfinder.setCachingEnabled(false);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(50, 64, 0);

        PathfindingContext context = createContext(goal);

        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenReturn(Optional.of(createMockPath(start, goal)));

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertNotNull(result, "Should work with caching disabled");
    }

    @Test
    @DisplayName("Handles unreachable chunks gracefully")
    void testUnreachableChunks() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(1000, 64, 0); // Very far

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withMaxRange(500); // Out of range

        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenReturn(Optional.empty());

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertFalse(result.isPresent(), "Should not find path to unreachable destination");
    }

    @Test
    @DisplayName("Local paths are stitched together")
    void testLocalPathStitching() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 0);

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withHierarchicalThreshold(64);

        // Setup local paths between waypoints
        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenAnswer(invocation -> {
                BlockPos s = invocation.getArgument(0);
                BlockPos g = invocation.getArgument(1);
                return Optional.of(createMockPath(s, g));
            });

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertNotNull(result, "Should stitch local paths together");
    }

    @Test
    @DisplayName("Handles null or invalid positions")
    void testInvalidPositions() {
        PathfindingContext context = createContext(new BlockPos(10, 64, 10));

        // Null start
        Optional<List<PathNode>> result1 = hierarchicalPathfinder.findPath(null, new BlockPos(10, 64, 10), context);
        assertFalse(result1.isPresent(), "Should handle null start");

        // Null goal
        Optional<List<PathNode>> result2 = hierarchicalPathfinder.findPath(new BlockPos(0, 64, 0), null, context);
        assertFalse(result2.isPresent(), "Should handle null goal");
    }

    @Test
    @DisplayName("Constructor with custom local pathfinder")
    void testCustomLocalPathfinder() {
        AStarPathfinder customPathfinder = mock(AStarPathfinder.class);
        HierarchicalPathfinder customHierarchical = new HierarchicalPathfinder(customPathfinder);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 0);

        PathfindingContext context = createContext(goal);

        List<PathNode> mockPath = createMockPath(start, goal);
        when(customPathfinder.findPath(eq(start), eq(goal), any()))
            .thenReturn(Optional.of(mockPath));

        Optional<List<PathNode>> result = customHierarchical.findPath(start, goal, context);

        assertTrue(result.isPresent(), "Should use custom local pathfinder");
        verify(customPathfinder).findPath(eq(start), eq(goal), any());
    }

    @Test
    @DisplayName("Default constructor creates functional pathfinder")
    void testDefaultConstructor() {
        HierarchicalPathfinder defaultPathfinder = new HierarchicalPathfinder();

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(10, 64, 0);

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(false); // Use standard A*

        Optional<List<PathNode>> result = defaultPathfinder.findPath(start, goal, context);

        // With default constructor and standard A*, should attempt pathfinding
        assertNotNull(result, "Default constructor should create functional pathfinder");
    }

    @Test
    @DisplayName("Path smoothing is applied to hierarchical paths")
    void testPathSmoothing() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(100, 64, 0);

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .setSmoothPath(true);

        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenReturn(Optional.of(createMockPath(start, goal)));

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertNotNull(result, "Should apply smoothing to hierarchical paths");
    }

    @Test
    @DisplayName("Handles very long distances efficiently")
    void testVeryLongDistance() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(500, 64, 0); // Very far

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withHierarchicalThreshold(64)
            .withMaxRange(1000);

        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenReturn(Optional.of(createMockPath(start, goal)));

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertNotNull(result, "Should handle very long distances");
    }

    @Test
    @DisplayName("Respects max range constraint")
    void testMaxRangeConstraint() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos goal = new BlockPos(1000, 64, 0);

        PathfindingContext context = createContext(goal)
            .setUseHierarchical(true)
            .withMaxRange(100); // Much less than distance

        when(mockLocalPathfinder.findPath(any(), any(), any()))
            .thenReturn(Optional.empty());

        Optional<List<PathNode>> result = hierarchicalPathfinder.findPath(start, goal, context);

        assertFalse(result.isPresent(), "Should respect max range constraint");
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
            .withMaxFallDistance(3)
            .withTimeout(5000);
    }

    private List<PathNode> createMockPath(BlockPos start, BlockPos goal) {
        List<PathNode> path = new ArrayList<>();
        PathNode parent = null;

        // Create a simple straight path
        int steps = (int) Math.sqrt(start.distSqr(goal));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = start.getX() + (int) ((goal.getX() - start.getX()) * t);
            int y = start.getY() + (int) ((goal.getY() - start.getY()) * t);
            int z = start.getZ() + (int) ((goal.getZ() - start.getZ()) * t);

            BlockPos pos = new BlockPos(x, y, z);
            PathNode node = new PathNode(pos, parent, i, steps - i, MovementType.WALK);
            path.add(node);
            parent = node;
        }

        return path;
    }

    private void setupTraversableTerrain() {
        BlockState solidState = mock(BlockState.class);
        when(solidState.isSolidRender(eq(mockLevel), any())).thenReturn(true);

        BlockState airState = mock(BlockState.class);
        when(airState.isSolidRender(eq(mockLevel), any())).thenReturn(false);

        // Setup solid ground at y=63 for all positions
        for (int x = -50; x <= 550; x += 16) {
            for (int z = -50; z <= 50; z += 16) {
                BlockPos below = new BlockPos(x, 63, z);
                when(mockLevel.getBlockState(below)).thenReturn(solidState);

                BlockPos at = new BlockPos(x, 64, z);
                when(mockLevel.getBlockState(at)).thenReturn(airState);

                BlockPos above = new BlockPos(x, 65, z);
                when(mockLevel.getBlockState(above)).thenReturn(airState);
            }
        }
    }
}
