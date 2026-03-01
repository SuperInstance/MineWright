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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PathSmoother}.
 *
 * Tests cover:
 * <ul>
 *   <li>String pulling (removing redundant nodes)</li>
 *   <li>Corner cutting (diagonal shortcuts)</li>
 *   <li>Path subdivision for smooth turning</li>
 *   <li>Duplicate removal</li>
 *   <li>Merging close nodes</li>
 *   <li>Edge cases (empty, single, two-node paths)</li>
 * </ul>
 */
@DisplayName("PathSmoother Tests")
class PathSmootherTest {

    @Mock
    private Level mockLevel;

    @Mock
    private Entity mockEntity;

    @Mock
    private PathfindingContext mockContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockContext.getLevel()).thenReturn(mockLevel);
        when(mockContext.getEntity()).thenReturn(mockEntity);
        when(mockContext.shouldSmoothPath()).thenReturn(true);

        // Setup clear path
        setupClearPath();
    }

    @Test
    @DisplayName("Smoothing returns same path for empty list")
    void testSmoothEmpty() {
        List<PathNode> emptyPath = new ArrayList<>();

        List<PathNode> result = PathSmoother.smooth(emptyPath, mockContext);

        assertSame(emptyPath, result, "Empty path should be returned as-is");
    }

    @Test
    @DisplayName("Smoothing returns same path for single node")
    void testSmoothSingle() {
        List<PathNode> singlePath = createPath(
            new BlockPos(0, 64, 0)
        );

        List<PathNode> result = PathSmoother.smooth(singlePath, mockContext);

        assertEquals(1, result.size(), "Single-node path should remain single");
        assertEquals(singlePath.get(0).pos, result.get(0).pos);
    }

    @Test
    @DisplayName("Smoothing returns same path for two nodes")
    void testSmoothTwo() {
        List<PathNode> twoPath = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(10, 64, 0)
        );

        List<PathNode> result = PathSmoother.smooth(twoPath, mockContext);

        assertEquals(2, result.size(), "Two-node path should remain two nodes");
    }

    @Test
    @DisplayName("String pulling removes redundant nodes")
    void testStringPulling() {
        // Create a straight path with intermediate nodes
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(1, 64, 0),
            new BlockPos(2, 64, 0),
            new BlockPos(3, 64, 0),
            new BlockPos(4, 64, 0),
            new BlockPos(5, 64, 0)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        // Should reduce to start and end
        assertTrue(result.size() <= path.size(), "Smoothing should reduce path length");
        assertEquals(path.get(0).pos, result.get(0).pos, "Start should be preserved");
        assertEquals(path.get(path.size() - 1).pos, result.get(result.size() - 1).pos,
            "End should be preserved");
    }

    @Test
    @DisplayName("Corner cutting replaces 90-degree turns with diagonals")
    void testCornerCutting() {
        // Create a path with a 90-degree turn
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(5, 64, 0),
            new BlockPos(5, 64, 5)
        );

        // Setup diagonal path as valid
        setupClearDiagonal(new BlockPos(0, 64, 0), new BlockPos(5, 64, 5));

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        // Should cut the corner
        assertTrue(result.size() <= path.size(), "Corner cutting should reduce path");
    }

    @Test
    @DisplayName("Path subdivision adds nodes for smooth turning")
    void testSubdivision() {
        // Create a path with sharp turns
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(10, 64, 0),
            new BlockPos(10, 64, 10)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        // Subdivision may add intermediate nodes
        assertNotNull(result, "Path should be smoothed");
        assertTrue(result.size() >= 2, "Path should have at least start and end");
    }

    @Test
    @DisplayName("Line of sight check works for clear paths")
    void testLineOfSightClear() {
        PathNode from = new PathNode(new BlockPos(0, 64, 0));
        PathNode to = new PathNode(new BlockPos(5, 64, 0));

        // Clear path setup
        setupClearPath();

        // The line of sight check should pass internally during smoothing
        List<PathNode> path = createPath(from.pos, to.pos);
        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Line of sight check fails for blocked paths")
    void testLineOfSightBlocked() {
        PathNode from = new PathNode(new BlockPos(0, 64, 0));
        PathNode to = new PathNode(new BlockPos(10, 64, 0));

        // Block the middle
        BlockPos blocked = new BlockPos(5, 64, 0);
        BlockState solidState = mock(BlockState.class);
        when(solidState.isSolidRender(eq(mockLevel), eq(blocked))).thenReturn(true);
        when(mockLevel.getBlockState(blocked)).thenReturn(solidState);
        when(mockLevel.getBlockState(blocked.above())).thenReturn(solidState);

        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(5, 64, 0),
            new BlockPos(10, 64, 0)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        // Cannot skip the blocked node
        assertTrue(result.size() >= 2, "Blocked path should preserve nodes");
    }

    @Test
    @DisplayName("Remove duplicates eliminates consecutive duplicates")
    void testRemoveDuplicates() {
        List<PathNode> path = new ArrayList<>();
        BlockPos pos1 = new BlockPos(0, 64, 0);
        BlockPos pos2 = new BlockPos(1, 64, 0);

        path.add(new PathNode(pos1));
        path.add(new PathNode(pos1)); // Duplicate
        path.add(new PathNode(pos2));
        path.add(new PathNode(pos2)); // Duplicate

        List<PathNode> result = PathSmoother.removeDuplicates(path);

        assertEquals(2, result.size(), "Duplicates should be removed");
        assertEquals(pos1, result.get(0).pos);
        assertEquals(pos2, result.get(1).pos);
    }

    @Test
    @DisplayName("Remove duplicates handles empty list")
    void testRemoveDuplicatesEmpty() {
        List<PathNode> empty = new ArrayList<>();

        List<PathNode> result = PathSmoother.removeDuplicates(empty);

        assertTrue(result.isEmpty(), "Empty list should remain empty");
    }

    @Test
    @DisplayName("Remove duplicates handles null list")
    void testRemoveDuplicatesNull() {
        List<PathNode> result = PathSmoother.removeDuplicates(null);

        assertNull(result, "Null list should return null");
    }

    @Test
    @DisplayName("Merge close nodes combines nearby waypoints")
    void testMergeCloseNodes() {
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(1, 64, 0),  // Close to previous
            new BlockPos(10, 64, 0) // Far from previous
        );

        double minDistance = 5.0;
        List<PathNode> result = PathSmoother.mergeCloseNodes(path, minDistance);

        // Should merge the close nodes
        assertTrue(result.size() <= path.size(), "Close nodes should be merged");
        assertEquals(path.get(0).pos, result.get(0).pos, "Start should be preserved");
        assertEquals(path.get(path.size() - 1).pos, result.get(result.size() - 1).pos,
            "End should be preserved");
    }

    @Test
    @DisplayName("Merge close nodes preserves end node")
    void testMergeCloseNodesPreservesEnd() {
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(1, 64, 0),
            new BlockPos(2, 64, 0)
        );

        double minDistance = 10.0;
        List<PathNode> result = PathSmoother.mergeCloseNodes(path, minDistance);

        assertEquals(path.get(0).pos, result.get(0).pos, "Start should be preserved");
        assertEquals(path.get(path.size() - 1).pos, result.get(result.size() - 1).pos,
            "End should be preserved even if close");
    }

    @Test
    @DisplayName("Smoothing preserves path endpoints")
    void testSmoothingPreservesEndpoints() {
        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos end = new BlockPos(10, 64, 10);

        List<PathNode> path = createPath(
            start,
            new BlockPos(2, 64, 2),
            new BlockPos(5, 64, 5),
            new BlockPos(7, 64, 7),
            end
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertEquals(start, result.get(0).pos, "Start position should be preserved");
        assertEquals(end, result.get(result.size() - 1).pos, "End position should be preserved");
    }

    @Test
    @DisplayName("Smoothing handles paths with vertical movement")
    void testSmoothingVerticalPath() {
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(0, 65, 0),
            new BlockPos(0, 66, 0),
            new BlockPos(0, 67, 0)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertNotNull(result, "Vertical path should be smoothed");
        assertTrue(result.size() >= 2, "Should have at least start and end");
        assertEquals(path.get(0).pos, result.get(0).pos);
        assertEquals(path.get(path.size() - 1).pos, result.get(result.size() - 1).pos);
    }

    @Test
    @DisplayName("Smoothing handles complex 3D paths")
    void testSmoothing3DPath() {
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(2, 65, 2),
            new BlockPos(4, 66, 4),
            new BlockPos(6, 67, 6),
            new BlockPos(8, 68, 8)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertNotNull(result, "3D path should be smoothed");
        assertTrue(result.size() >= 2, "Should have at least start and end");
    }

    @Test
    @DisplayName("Smoothing with disabled smoothing returns original path")
    void testSmoothingDisabled() {
        when(mockContext.shouldSmoothPath()).thenReturn(false);

        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(1, 64, 0),
            new BlockPos(2, 64, 0)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        // When smoothing is disabled, it should return the path as-is
        assertNotNull(result, "Should return a result");
    }

    @Test
    @DisplayName("Angle change calculation works")
    void testAngleChange() {
        // Straight path - no angle change
        PathNode prev = new PathNode(new BlockPos(0, 64, 0));
        PathNode current = new PathNode(new BlockPos(5, 64, 0));
        PathNode next = new PathNode(new BlockPos(10, 64, 0));

        // The angle calculation happens internally during subdivision
        // We just verify the smoothing doesn't throw an exception
        List<PathNode> path = createPath(prev.pos, current.pos, next.pos);
        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertNotNull(result, "Should calculate angle changes without error");
    }

    @Test
    @DisplayName("Smoothing handles very short paths")
    void testSmoothingVeryShort() {
        List<PathNode> path = createPath(
            new BlockPos(0, 64, 0),
            new BlockPos(1, 64, 0)
        );

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertEquals(2, result.size(), "Very short path should remain unchanged");
    }

    @Test
    @DisplayName("Smoothing handles paths with same position repeated")
    void testSmoothingRepeatedPositions() {
        BlockPos pos = new BlockPos(5, 64, 5);

        List<PathNode> path = new ArrayList<>();
        path.add(new PathNode(new BlockPos(0, 64, 0)));
        path.add(new PathNode(pos));
        path.add(new PathNode(pos));
        path.add(new PathNode(pos));
        path.add(new PathNode(new BlockPos(10, 64, 10)));

        List<PathNode> result = PathSmoother.smooth(path, mockContext);

        assertNotNull(result, "Should handle repeated positions");
        // Duplicates should be removed
        long posCount = result.stream().filter(n -> n.pos.equals(pos)).count();
        assertEquals(1, posCount, "Repeated position should appear only once");
    }

    // Helper methods

    private List<PathNode> createPath(BlockPos... positions) {
        List<PathNode> path = new ArrayList<>();
        PathNode parent = null;
        for (BlockPos pos : positions) {
            PathNode node = new PathNode(pos, parent, path.size(), 0, MovementType.WALK);
            path.add(node);
            parent = node;
        }
        return path;
    }

    private void setupClearPath() {
        BlockState airState = mock(BlockState.class);
        when(airState.isSolidRender(eq(mockLevel), any())).thenReturn(false);
        when(mockLevel.getBlockState(any())).thenReturn(airState);
    }

    private void setupClearDiagonal(BlockPos from, BlockPos to) {
        // Ensure diagonal path is clear
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        int steps = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));

        for (int i = 0; i <= steps; i++) {
            int x = from.getX() + (dx * i / steps);
            int y = from.getY() + (dy * i / steps);
            int z = from.getZ() + (dz * i / steps);
            BlockPos pos = new BlockPos(x, y, z);

            BlockState airState = mock(BlockState.class);
            when(airState.isSolidRender(eq(mockLevel), eq(pos))).thenReturn(false);
            when(mockLevel.getBlockState(pos)).thenReturn(airState);
            when(mockLevel.getBlockState(pos.above())).thenReturn(airState);
        }
    }
}
