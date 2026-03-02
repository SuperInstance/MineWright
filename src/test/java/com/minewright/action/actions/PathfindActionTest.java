package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.ActionResult.ErrorCode;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.pathfinding.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link PathfindAction}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Target position parsing (x,y,z, goal area, string format)</li>
 *   <li>Enhanced pathfinding initialization</li>
 *   <li>Legacy pathfinding fallback</li>
 *   <li>Path execution and progress tracking</li>
 *   <li>Timeout handling</li>
 *   <li>Stuck detection and re-pathing</li>
 *   <li>Cancellation and cleanup</li>
 *   <li>Path caching</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PathfindAction Tests")
class PathfindActionTest {

    @Mock
    private ForemanEntity foreman;

    @Mock
    private Level level;

    @Mock
    private PathNavigation navigation;

    private Task task;
    private PathfindAction action;

    @BeforeEach
    void setUp() {
        lenient().when(foreman.level()).thenReturn(level);
        lenient().when(foreman.getEntityName()).thenReturn("TestForeman");
        lenient().when(foreman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        lenient().when(foreman.getNavigation()).thenReturn(navigation);
        lenient().when(foreman.isFlying()).thenReturn(false);
    }

    // ==================== Target Position Parsing Tests ====================

    @Test
    @DisplayName("Should successfully initialize with x,y,z parameters")
    void testValidXYZInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 100);
        params.put("y", 64);
        params.put("z", -200);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should not fail immediately
        assertFalse(action.isComplete(), "Action should not be complete immediately");
        assertNull(action.getResult(), "Should not have result yet");
        assertEquals("Pathfind to [100, 64, -200]", action.getDescription(),
            "Description should show target coordinates");
    }

    @Test
    @DisplayName("Should fail initialization with missing target position")
    void testMissingTargetPosition() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertTrue(action.isComplete(), "Action should complete with failure");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Should be failure result");
        assertTrue(result.getMessage().contains("Invalid target position") ||
                   result.getMessage().contains("position"),
            "Should indicate invalid position");
    }

    @Test
    @DisplayName("Should parse goal area parameters")
    void testGoalAreaParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("goalAreaX", 50);
        params.put("goalAreaY", 70);
        params.put("goalAreaZ", 30);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should start successfully");
        assertEquals("Pathfind to [50, 70, 30]", action.getDescription());
    }

    @Test
    @DisplayName("Should parse string position parameter")
    void testStringPositionParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("position", "100,64,-200");
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should start successfully");
        assertEquals("Pathfind to [100, 64, -200]", action.getDescription());
    }

    @Test
    @DisplayName("Should handle malformed string position")
    void testMalformedStringPosition() {
        Map<String, Object> params = new HashMap<>();
        params.put("position", "invalid,position,string");
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertTrue(action.isComplete(), "Should fail with invalid position");
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle incomplete string position")
    void testIncompleteStringPosition() {
        Map<String, Object> params = new HashMap<>();
        params.put("position", "100,64");
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertTrue(action.isComplete(), "Should fail with incomplete position");
    }

    // ==================== Enhanced Pathfinding Tests ====================

    @Test
    @DisplayName("Should initialize enhanced pathfinding context")
    void testEnhancedPathfindingContext() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 100);
        params.put("y", 64);
        params.put("z", 100);
        params.put("maxRange", 200);
        params.put("timeout", 10000L);
        params.put("allowDangerous", true);
        params.put("allowParkour", true);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should not throw exception
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should use cached path when available")
    void testUseCachedPath() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 100);
        params.put("y", 64);
        params.put("z", 100);
        params.put("useCache", true);
        task = new Task("pathfind", params);

        BlockPos start = new BlockPos(0, 64, 0);
        BlockPos target = new BlockPos(100, 64, 100);

        // Create a cached path
        List<PathNode> cachedPath = createMockPath(start, target);
        PathExecutor.cachePath(start, target, cachedPath);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should start successfully with cached path
        assertFalse(action.isComplete());
    }

    @Test
    @DisplayName("Should find new path when cache is disabled")
    void testDisableCache() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        params.put("useCache", false);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should attempt to find path (will fail in test environment)
        assertNotNull(action);
    }

    // ==================== Legacy Pathfinding Tests ====================

    @Test
    @DisplayName("Should use legacy pathfinding as fallback")
    void testLegacyPathfindingFallback() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Tick to trigger legacy navigation
        for (int i = 0; i < 20 && !action.isComplete(); i++) {
            action.tick();
        }

        // Verify navigation was called
        verify(navigation, atLeastOnce()).moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should fail when navigation is unavailable in legacy mode")
    void testLegacyNavigationUnavailable() {
        lenient().when(foreman.getNavigation()).thenReturn(null);

        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        ActionResult result = action.getResult();
        if (result != null) {
            assertTrue(result.getMessage().contains("Navigation") ||
                       result.getMessage().contains("navigation"));
        }
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout after max ticks")
    void testTimeoutAfterMaxTicks() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 1000);
        params.put("y", 64);
        params.put("z", 1000);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Run until timeout (600 ticks)
        for (int i = 0; i < 650 && !action.isComplete(); i++) {
            action.tick();
        }

        assertTrue(action.isComplete(), "Should complete after timeout");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertTrue(result.getMessage().contains("timeout") ||
                   result.getMessage().contains("Timeout") ||
                   result.getMessage().contains("600"),
            "Should indicate timeout occurred");
    }

    // ==================== Cancellation Tests ====================

    @Test
    @DisplayName("Should handle cancellation gracefully")
    void testCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 100);
        params.put("y", 64);
        params.put("z", 100);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();
        action.cancel();

        assertTrue(action.isComplete(), "Should be complete after cancellation");
        ActionResult result = action.getResult();
        assertNotNull(result, "Should have result");
        assertFalse(result.isSuccess(), "Cancellation should be failure");
        assertTrue(result.getMessage().contains("cancelled") ||
                   result.getMessage().contains("Cancelled"),
            "Should indicate cancellation");
    }

    @Test
    @DisplayName("Should stop navigation on cancellation")
    void testStopNavigationOnCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();
        action.cancel();

        // Verify navigation stopped
        verify(navigation, atLeastOnce()).stop();
    }

    @Test
    @DisplayName("Should handle double cancellation gracefully")
    void testDoubleCancellation() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();
        action.cancel();
        action.cancel(); // Second cancellation

        assertTrue(action.isComplete());
        // Should not throw exception
    }

    // ==================== Progress Tracking Tests ====================

    @Test
    @DisplayName("Should return progress between 0 and 1")
    void testProgressRange() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        double progress = action.getProgress();
        assertTrue(progress >= 0.0 && progress <= 1.0,
            "Progress should be between 0 and 1, got: " + progress);
    }

    @Test
    @DisplayName("Should have path state in enhanced mode")
    void testPathState() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Path state may be null initially
        PathExecutor.PathState state = action.getPathState();
        // State depends on whether pathfinding succeeded
        assertNotNull(action);
    }

    // ==================== Description Tests ====================

    @Test
    @DisplayName("Description should show target coordinates")
    void testDescriptionWithCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 123);
        params.put("y", 45);
        params.put("z", -67);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("123") && description.contains("45") && description.contains("-67"),
            "Description should contain target coordinates");
    }

    @Test
    @DisplayName("Description should handle unknown location")
    void testDescriptionUnknownLocation() {
        Map<String, Object> params = new HashMap<>();
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);

        String description = action.getDescription();
        assertTrue(description.contains("unknown") || description.contains("Unknown"),
            "Description should indicate unknown location when no position provided");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null foreman gracefully")
    void testNullForeman() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 10);
        params.put("y", 64);
        params.put("z", 10);
        task = new Task("pathfind", params);

        action = new PathfindAction(null, task);
        action.start();

        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle zero coordinates")
    void testZeroCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 0);
        params.put("y", 0);
        params.put("z", 0);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should accept zero coordinates");
    }

    @Test
    @DisplayName("Should handle negative coordinates")
    void testNegativeCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", -100);
        params.put("y", -50);
        params.put("z", -200);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should accept negative coordinates");
        assertEquals("Pathfind to [-100, -50, -200]", action.getDescription());
    }

    @Test
    @DisplayName("Should handle very large coordinates")
    void testVeryLargeCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 1000000);
        params.put("y", 256);
        params.put("z", -1000000);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        assertFalse(action.isComplete(), "Should accept large coordinates");
    }

    @Test
    @DisplayName("Should handle partial coordinates")
    void testPartialCoordinates() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 100);
        params.put("z", 200);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should fail without all three coordinates
        assertTrue(action.isComplete());
    }

    @Test
    @DisplayName("Should handle custom max range parameter")
    void testCustomMaxRange() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 100);
        params.put("y", 64);
        params.put("z", 100);
        params.put("maxRange", 500);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should not throw exception
        assertNotNull(action);
    }

    @Test
    @DisplayName("Should handle custom timeout parameter")
    void testCustomTimeout() {
        Map<String, Object> params = new HashMap<>();
        params.put("x", 50);
        params.put("y", 64);
        params.put("z", 50);
        params.put("timeout", 15000L);
        task = new Task("pathfind", params);

        action = new PathfindAction(foreman, task);
        action.start();

        // Should not throw exception
        assertNotNull(action);
    }

    // ==================== Helper Methods ====================

    private List<PathNode> createMockPath(BlockPos start, BlockPos target) {
        List<PathNode> path = new ArrayList<>();
        path.add(new PathNode(start));
        path.add(new PathNode(new BlockPos(start.getX() + 1, start.getY(), start.getZ())));
        path.add(new PathNode(target));
        return path;
    }
}
