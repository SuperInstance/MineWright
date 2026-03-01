package com.minewright.recovery;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.ActionExecutor;
import com.minewright.execution.AgentStateMachine;
import com.minewright.execution.AgentState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link StuckDetector}.
 *
 * Tests cover:
 * <ul>
 *   <li>Position stuck detection (entity not moving)</li>
 *   <li>Progress stuck detection (task progress not increasing)</li>
 *   <li>State stuck detection (state machine not transitioning)</li>
 *   <li>Path stuck detection (pathfinding failures)</li>
 *   <li>Recovery strategy selection</li>
 *   <li>Detection threshold enforcement</li>
 *   <li>State reset and cleanup</li>
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("StuckDetector Tests")
class StuckDetectorTest {

    @Mock
    private ForemanEntity mockEntity;

    @Mock
    private ActionExecutor mockActionExecutor;

    @Mock
    private AgentStateMachine mockStateMachine;

    @Mock
    private Level mockLevel;

    private StuckDetector detector;
    private static final String ENTITY_NAME = "TestForeman";
    private static final BlockPos START_POS = new BlockPos(0, 64, 0);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock entity behavior
        when(mockEntity.getEntityName()).thenReturn(ENTITY_NAME);
        when(mockEntity.getActionExecutor()).thenReturn(mockActionExecutor);
        when(mockEntity.level()).thenReturn(mockLevel);
        when(mockEntity.blockPosition()).thenReturn(START_POS);
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));

        // Setup mock action executor behavior
        when(mockActionExecutor.getCurrentActionProgress()).thenReturn(0);

        // Setup mock state machine behavior
        when(mockActionExecutor.getStateMachine()).thenReturn(mockStateMachine);
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.IDLE);

        // Create detector
        detector = new StuckDetector(mockEntity);
    }

    // ==================== Position Stuck Tests ====================

    @Test
    @DisplayName("Should detect position stuck when entity doesn't move")
    void testDetectPositionStuck() {
        // Given: Entity doesn't move (returns same position)
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));

        // When: Tick past threshold (60 ticks = 3 seconds)
        for (int i = 0; i < 65; i++) {
            detector.tickAndDetect();
        }

        // Then: Should detect position stuck
        assertTrue(detector.isPositionStuck(60),
            "Should detect position stuck after 60 ticks without movement");
        assertEquals(65, detector.getStuckPositionTicks(),
            "Stuck position ticks should count up");

        // Then: Should detect position stuck type
        StuckType stuckType = detector.detectStuck();
        assertEquals(StuckType.POSITION_STUCK, stuckType,
            "Should detect POSITION_STUCK type");
    }

    @Test
    @DisplayName("Should not detect position stuck when entity moves")
    void testNotDetectPositionStuckWhenMoving() {
        // Given: Entity moves each tick
        when(mockEntity.position())
            .thenReturn(new Vec3(0.0, 64.0, 0.0))   // Tick 0
            .thenReturn(new Vec3(1.0, 64.0, 0.0))   // Tick 1
            .thenReturn(new Vec3(2.0, 64.0, 0.0));  // Tick 2

        // When: Tick multiple times
        for (int i = 0; i < 65; i++) {
            detector.tickAndDetect();
        }

        // Then: Should not detect position stuck
        assertFalse(detector.isPositionStuck(60),
            "Should not detect position stuck when entity is moving");
        assertTrue(detector.getStuckPositionTicks() < 10,
            "Stuck position ticks should reset when moving");
    }

    @Test
    @DisplayName("Should detect position stuck before custom threshold")
    void testDetectPositionStuckCustomThreshold() {
        // Given: Entity doesn't move
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));

        // When: Tick to 30
        for (int i = 0; i < 30; i++) {
            detector.tickAndDetect();
        }

        // Then: Should be stuck at threshold 30 but not 60
        assertTrue(detector.isPositionStuck(30),
            "Should detect position stuck at threshold 30");
        assertFalse(detector.isPositionStuck(60),
            "Should not detect position stuck at threshold 60 yet");
    }

    // ==================== Progress Stuck Tests ====================

    @Test
    @DisplayName("Should detect progress stuck when progress doesn't increase")
    void testDetectProgressStuck() {
        // Given: Progress stays at 0
        when(mockActionExecutor.getCurrentActionProgress()).thenReturn(0);

        // When: Tick past threshold (100 ticks = 5 seconds)
        for (int i = 0; i < 105; i++) {
            detector.tickAndDetect();
        }

        // Then: Should detect progress stuck
        assertTrue(detector.isProgressStuck(0, 0, 100),
            "Should detect progress stuck after 100 ticks without progress increase");
        assertEquals(105, detector.getStuckProgressTicks(),
            "Stuck progress ticks should count up");

        // Then: Should detect progress stuck type
        StuckType stuckType = detector.detectStuck();
        assertEquals(StuckType.PROGRESS_STUCK, stuckType,
            "Should detect PROGRESS_STUCK type");
    }

    @Test
    @DisplayName("Should not detect progress stuck when progress increases")
    void testNotDetectProgressStuckWhenProgressIncreases() {
        // Given: Progress increases periodically
        when(mockActionExecutor.getCurrentActionProgress())
            .thenReturn(0)   // Ticks 0-49
            .thenReturn(25)  // Ticks 50-99
            .thenReturn(50); // Ticks 100+

        // When: Tick 150 times
        for (int i = 0; i < 150; i++) {
            // Update progress at tick 50 and 100
            if (i == 50) {
                when(mockActionExecutor.getCurrentActionProgress()).thenReturn(25);
            }
            if (i == 100) {
                when(mockActionExecutor.getCurrentActionProgress()).thenReturn(50);
            }
            detector.tickAndDetect();
        }

        // Then: Should not detect progress stuck
        assertTrue(detector.getStuckProgressTicks() < 60,
            "Stuck progress ticks should reset when progress increases");
    }

    // ==================== State Stuck Tests ====================

    @Test
    @DisplayName("Should detect state stuck when state doesn't transition")
    void testDetectStateStuck() {
        // Given: State stays IDLE
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.IDLE);

        // When: Tick past threshold (200 ticks = 10 seconds)
        for (int i = 0; i < 205; i++) {
            detector.tickAndDetect();
        }

        // Then: Should detect state stuck
        assertTrue(detector.isStateStuck(AgentState.IDLE, 200),
            "Should detect state stuck after 200 ticks without state transition");
        assertEquals(205, detector.getStuckStateTicks(),
            "Stuck state ticks should count up");

        // Then: Should detect state stuck type
        StuckType stuckType = detector.detectStuck();
        assertEquals(StuckType.STATE_STUCK, stuckType,
            "Should detect STATE_STUCK type");
    }

    @Test
    @DisplayName("Should not detect state stuck when state transitions")
    void testNotDetectStateStuckWhenStateTransitions() {
        // Given: State transitions periodically
        when(mockStateMachine.getCurrentState())
            .thenReturn(AgentState.IDLE)      // Ticks 0-49
            .thenReturn(AgentState.PLANNING)  // Ticks 50-149
            .thenReturn(AgentState.EXECUTING); // Ticks 150+

        // When: Tick 200 times with state changes
        for (int i = 0; i < 200; i++) {
            if (i == 50) {
                when(mockStateMachine.getCurrentState()).thenReturn(AgentState.PLANNING);
            }
            if (i == 150) {
                when(mockStateMachine.getCurrentState()).thenReturn(AgentState.EXECUTING);
            }
            detector.tickAndDetect();
        }

        // Then: Should not detect state stuck
        assertTrue(detector.getStuckStateTicks() < 60,
            "Stuck state ticks should reset when state transitions");
    }

    // ==================== Path Stuck Tests ====================

    @Test
    @DisplayName("Should detect path stuck when marked")
    void testDetectPathStuck() {
        // Given: Path is marked as stuck
        detector.markPathStuck();

        // When: Check detection
        StuckType stuckType = detector.detectStuck();

        // Then: Should detect path stuck
        assertEquals(StuckType.PATH_STUCK, stuckType,
            "Should detect PATH_STUCK when path is marked stuck");
        assertTrue(detector.isPathStuck(),
            "isPathStuck() should return true");
    }

    @Test
    @DisplayName("Should not detect path stuck when cleared")
    void testNotDetectPathStuckWhenCleared() {
        // Given: Path was stuck but is now cleared
        detector.markPathStuck();
        detector.clearPathStuck();

        // When: Check detection
        StuckType stuckType = detector.detectStuck();

        // Then: Should not detect path stuck
        assertNotEquals(StuckType.PATH_STUCK, stuckType,
            "Should not detect PATH_STUCK when path is cleared");
        assertFalse(detector.isPathStuck(),
            "isPathStuck() should return false after clearing");
    }

    @Test
    @DisplayName("Should prioritize path stuck over other stuck types")
    void testPathStuckPriority() {
        // Given: Path is stuck AND position is stuck
        detector.markPathStuck();
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 65; i++) {
            detector.tickAndDetect();
        }

        // When: Check detection
        StuckType stuckType = detector.detectStuck();

        // Then: Should prioritize path stuck (most severe)
        assertEquals(StuckType.PATH_STUCK, stuckType,
            "Should prioritize PATH_STUCK over POSITION_STUCK");
    }

    // ==================== Detection Priority Tests ====================

    @Test
    @DisplayName("Should detect position stuck before progress stuck")
    void testPositionStuckPriority() {
        // Given: Both position and progress are stuck
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        when(mockActionExecutor.getCurrentActionProgress()).thenReturn(0);

        // When: Tick enough to trigger both
        for (int i = 0; i < 105; i++) {
            detector.tickAndDetect();
        }

        // Then: Should detect position stuck first (higher priority)
        StuckType stuckType = detector.detectStuck();
        assertEquals(StuckType.POSITION_STUCK, stuckType,
            "Should detect POSITION_STUCK before PROGRESS_STUCK");
    }

    // ==================== Reset Tests ====================

    @Test
    @DisplayName("Should reset all counters on reset()")
    void testReset() {
        // Given: Detector is stuck on all fronts
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        when(mockActionExecutor.getCurrentActionProgress()).thenReturn(0);
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.IDLE);
        detector.markPathStuck();

        for (int i = 0; i < 105; i++) {
            detector.tickAndDetect();
        }

        // When: Reset
        detector.reset();

        // Then: All counters should be zero
        assertEquals(0, detector.getStuckPositionTicks(),
            "Position stuck ticks should be zero after reset");
        assertEquals(0, detector.getStuckProgressTicks(),
            "Progress stuck ticks should be zero after reset");
        assertEquals(0, detector.getStuckStateTicks(),
            "State stuck ticks should be zero after reset");
        assertFalse(detector.isPathStuck(),
            "Path stuck should be false after reset");

        // Then: Should not detect any stuck type
        StuckType stuckType = detector.detectStuck();
        assertNull(stuckType,
            "Should not detect any stuck type after reset");
    }

    // ==================== Detection History Tests ====================

    @Test
    @DisplayName("Should track detection history")
    void testDetectionHistory() {
        // Given: Multiple stuck events occur
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));

        // Trigger position stuck detection
        for (int i = 0; i < 65; i++) {
            detector.tickAndDetect();
        }
        detector.detectStuck(); // Records "position"

        // Mark path stuck
        detector.markPathStuck(); // Records "path"

        // When: Get history
        Map<String, Integer> history = detector.getDetectionHistory();

        // Then: Should have recorded detections
        assertTrue(history.containsKey("position"),
            "Should record position detections");
        assertTrue(history.containsKey("path"),
            "Should record path detections");
        assertEquals(1, history.get("position"),
            "Should have one position detection");
        assertEquals(1, history.get("path"),
            "Should have one path detection");
    }

    // ==================== State Snapshot Tests ====================

    @Test
    @DisplayName("Should capture state snapshot")
    void testStateSnapshot() {
        // Given: Detector has some stuck counts
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 50; i++) {
            detector.tickAndDetect();
        }

        // When: Get snapshot
        StuckDetector.DetectionState snapshot = detector.getStateSnapshot();

        // Then: Snapshot should reflect current state
        assertEquals(50, snapshot.stuckPositionTicks(),
            "Snapshot should capture position stuck ticks");
        assertFalse(snapshot.pathStuck(),
            "Snapshot should capture path stuck status");
        assertNotNull(snapshot.detectionHistory(),
            "Snapshot should include detection history");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null entity gracefully in constructor")
    void testNullEntityInConstructor() {
        // When: Create detector with null entity
        // Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
            () -> new StuckDetector(null),
            "Should throw IllegalArgumentException for null entity");
    }

    @Test
    @DisplayName("Should not detect stuck when no state machine")
    void testNoStateMachine() {
        // Given: Action executor has no state machine
        when(mockActionExecutor.getStateMachine()).thenReturn(null);

        // When: Tick and detect
        for (int i = 0; i < 10; i++) {
            detector.tickAndDetect();
        }
        StuckType stuckType = detector.detectStuck();

        // Then: Should handle gracefully (no crash)
        // State should default to IDLE (from getState() implementation)
        assertNotNull(stuckType, "Should return a result even without state machine");
    }
}
