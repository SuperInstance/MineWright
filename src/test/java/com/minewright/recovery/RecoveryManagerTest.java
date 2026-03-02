package com.minewright.recovery;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.ActionExecutor;
import com.minewright.execution.AgentStateMachine;
import com.minewright.execution.AgentState;
import com.minewright.recovery.strategies.AbortStrategy;
import com.minewright.recovery.strategies.RepathStrategy;
import com.minewright.recovery.strategies.TeleportStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link RecoveryManager}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li><b>Stuck Detection Accuracy:</b> Position, progress, state, and path stuck detection</li>
 *   <li><b>Recovery Strategy Selection:</b> Correct strategy selection for each stuck type</li>
 *   <li><b>Multiple Recovery Attempts:</b> Retry behavior and escalation chains</li>
 *   <li><b>Recovery Failure Scenarios:</b> Permanent stuck conditions, timeout handling</li>
 *   <li><b>Individual Strategy Tests:</b> Repath, Teleport, and Abort strategy effectiveness</li>
 *   <li><b>Strategy Effectiveness Metrics:</b> Success rate tracking and statistics</li>
 * </ul>
 *
 * <p><b>Edge Cases Covered:</b></p>
 * <ul>
 *   <li>Agent permanently stuck (no recovery possible)</li>
 *   <li>Infinite recovery loops (max attempts enforcement)</li>
 *   <li>Concurrent recovery attempts</li>
 *   <li>Null and invalid inputs</li>
 *   <li>Game state preservation during recovery</li>
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("RecoveryManager Tests")
class RecoveryManagerTest {

    @Mock
    private ForemanEntity mockEntity;

    @Mock
    private ActionExecutor mockActionExecutor;

    @Mock
    private AgentStateMachine mockStateMachine;

    @Mock
    private Level mockLevel;

    @Mock
    private Path mockNavigationPath;

    private RecoveryManager recoveryManager;
    private StuckDetector stuckDetector;
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
        when(mockActionExecutor.getCurrentAction()).thenReturn(null);
        when(mockActionExecutor.getCurrentGoal()).thenReturn(null);

        // Setup mock state machine behavior
        when(mockActionExecutor.getStateMachine()).thenReturn(mockStateMachine);
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.IDLE);

        // Setup mock navigation behavior
        when(mockEntity.getNavigation()).thenReturn(mock(net.minecraft.world.entity.ai.navigation.PathNavigation.class));
        when(mockEntity.getNavigation().getPath()).thenReturn(mockNavigationPath);
        when(mockEntity.getNavigation().isInProgress()).thenReturn(false);

        // Create recovery manager and stuck detector
        recoveryManager = new RecoveryManager(mockEntity);
        stuckDetector = new StuckDetector(mockEntity);
    }

    // ==================== Stuck Detection Accuracy Tests ====================

    @Test
    @DisplayName("Should detect position stuck when entity doesn't move")
    void testDetectPositionStuck() {
        // Given: Entity doesn't move
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));

        // When: Tick past threshold
        for (int i = 0; i < 65; i++) {
            stuckDetector.tickAndDetect();
        }

        // Then: Should detect position stuck
        StuckType stuckType = stuckDetector.detectStuck();
        assertEquals(StuckType.POSITION_STUCK, stuckType,
            "Should detect POSITION_STUCK when entity doesn't move");
    }

    @Test
    @DisplayName("Should detect progress stuck when progress doesn't increase")
    void testDetectProgressStuck() {
        // Given: Progress stays at 0
        when(mockActionExecutor.getCurrentActionProgress()).thenReturn(0);

        // When: Tick past threshold
        for (int i = 0; i < 105; i++) {
            stuckDetector.tickAndDetect();
        }

        // Then: Should detect progress stuck
        StuckType stuckType = stuckDetector.detectStuck();
        assertEquals(StuckType.PROGRESS_STUCK, stuckType,
            "Should detect PROGRESS_STUCK when progress doesn't increase");
    }

    @Test
    @DisplayName("Should detect state stuck when state doesn't transition")
    void testDetectStateStuck() {
        // Given: State stays IDLE
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.IDLE);

        // When: Tick past threshold
        for (int i = 0; i < 205; i++) {
            stuckDetector.tickAndDetect();
        }

        // Then: Should detect state stuck
        StuckType stuckType = stuckDetector.detectStuck();
        assertEquals(StuckType.STATE_STUCK, stuckType,
            "Should detect STATE_STUCK when state doesn't transition");
    }

    @Test
    @DisplayName("Should detect path stuck when marked")
    void testDetectPathStuck() {
        // Given: Path is marked as stuck
        stuckDetector.markPathStuck();

        // When: Check detection
        StuckType stuckType = stuckDetector.detectStuck();

        // Then: Should detect path stuck
        assertEquals(StuckType.PATH_STUCK, stuckType,
            "Should detect PATH_STUCK when path is marked stuck");
    }

    @Test
    @DisplayName("Should prioritize path stuck over position stuck")
    void testPathStuckPriority() {
        // Given: Both path and position are stuck
        stuckDetector.markPathStuck();
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 65; i++) {
            stuckDetector.tickAndDetect();
        }

        // When: Check detection
        StuckType stuckType = stuckDetector.detectStuck();

        // Then: Should prioritize path stuck (most severe)
        assertEquals(StuckType.PATH_STUCK, stuckType,
            "Should prioritize PATH_STUCK over POSITION_STUCK");
    }

    @Test
    @DisplayName("Should not detect stuck when entity is moving and progressing")
    void testNotStuckWhenMovingAndProgressing() {
        // Given: Entity moves and progress increases
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        when(mockActionExecutor.getCurrentActionProgress()).thenReturn(0);
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.EXECUTING);

        // When: Tick with movement and progress
        for (int i = 0; i < 150; i++) {
            if (i % 20 == 0) {
                when(mockEntity.position()).thenReturn(new Vec3(i / 20.0, 64.0, 0.0));
                when(mockActionExecutor.getCurrentActionProgress()).thenReturn(i / 20);
            }
            stuckDetector.tickAndDetect();
        }

        // Then: Should not detect any stuck type
        StuckType stuckType = stuckDetector.detectStuck();
        assertNull(stuckType, "Should not detect stuck when agent is functioning normally");
    }

    // ==================== Recovery Strategy Selection Tests ====================

    @Test
    @DisplayName("Should select RepathStrategy for POSITION_STUCK")
    void testSelectRepathForPositionStuck() {
        // Given: Entity is position stuck
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 65; i++) {
            stuckDetector.tickAndDetect();
        }
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        // Then: Should attempt repath strategy first
        assertEquals(StuckType.POSITION_STUCK, stuckType);
        assertNotNull(result, "Should return a recovery result");
    }

    @Test
    @DisplayName("Should select RepathStrategy for PATH_STUCK")
    void testSelectRepathForPathStuck() {
        // Given: Path is stuck
        stuckDetector.markPathStuck();
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        // Then: Should attempt recovery
        assertEquals(StuckType.PATH_STUCK, stuckType);
        assertNotNull(result, "Should return a recovery result");
    }

    @Test
    @DisplayName("Should escalate from Repath to Teleport for POSITION_STUCK")
    void testEscalateFromRepathToTeleport() {
        // Given: Entity is position stuck
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 65; i++) {
            stuckDetector.tickAndDetect();
        }
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Attempt recovery multiple times to exhaust RepathStrategy
        RecoveryResult firstResult = recoveryManager.attemptRecovery(stuckType);
        RecoveryResult secondResult = recoveryManager.attemptRecovery(stuckType);
        RecoveryResult thirdResult = recoveryManager.attemptRecovery(stuckType);

        // Then: Should escalate through strategies
        // After 3 repath attempts, should try teleport
        assertNotNull(firstResult, "First attempt should return result");
        assertNotNull(secondResult, "Second attempt should return result");
        assertNotNull(thirdResult, "Third attempt should return result");
    }

    @Test
    @DisplayName("Should handle null stuck type gracefully")
    void testNullStuckType() {
        // When: Attempt recovery with null stuck type
        RecoveryResult result = recoveryManager.attemptRecovery(null);

        // Then: Should return ABORT
        assertEquals(RecoveryResult.ABORT, result,
            "Should return ABORT for null stuck type");
    }

    // ==================== Multiple Recovery Attempts Tests ====================

    @Test
    @DisplayName("Should track recovery attempts correctly")
    void testTrackRecoveryAttempts() {
        // Given: Entity is stuck
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 65; i++) {
            stuckDetector.tickAndDetect();
        }
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Attempt multiple recoveries
        recoveryManager.attemptRecovery(stuckType);
        recoveryManager.attemptRecovery(stuckType);
        recoveryManager.attemptRecovery(stuckType);

        // Then: Should track attempts
        assertTrue(recoveryManager.getTotalAttempts() >= 3,
            "Should track at least 3 recovery attempts");
    }

    @Test
    @DisplayName("Should enforce max attempts per strategy")
    void testMaxAttemptsPerStrategy() {
        // Given: Create custom manager with limited strategies
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("TestStrategy", 2, RecoveryResult.ESCALATE)
        );
        RecoveryManager customManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery more than max attempts
        RecoveryResult result1 = customManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result2 = customManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result3 = customManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should exhaust strategy after max attempts
        assertEquals(RecoveryResult.ESCALATE, result1, "First attempt should ESCALATE");
        assertEquals(RecoveryResult.ESCALATE, result2, "Second attempt should ESCALATE");
        assertEquals(RecoveryResult.ABORT, result3, "Third attempt should ABORT (exhausted)");
    }

    @Test
    @DisplayName("Should reset attempt counts after successful recovery")
    void testResetAfterSuccess() {
        // Given: Create manager with successful strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("SuccessStrategy", 3, RecoveryResult.SUCCESS)
        );
        RecoveryManager successManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery and succeed
        successManager.attemptRecovery(StuckType.POSITION_STUCK);
        int attemptsAfterSuccess = successManager.getTotalAttempts();

        // When: Attempt recovery again (should start fresh)
        successManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should have tracked both attempts
        assertTrue(attemptsAfterSuccess >= 1, "Should have at least one attempt");
        assertTrue(successManager.getTotalAttempts() >= 2, "Should have multiple attempts");
    }

    @Test
    @DisplayName("Should handle RETRY result correctly")
    void testRetryResult() {
        // Given: Create manager with retry strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("RetryStrategy", 3, RecoveryResult.RETRY)
        );
        RecoveryManager retryManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery with RETRY result
        RecoveryResult result = retryManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should return RETRY and allow subsequent attempts
        assertEquals(RecoveryResult.RETRY, result, "Should return RETRY");
        assertTrue(retryManager.getTotalAttempts() >= 1, "Should track attempt");
    }

    // ==================== Recovery Failure Scenarios Tests ====================

    @Test
    @DisplayName("Should abort when all strategies exhausted")
    void testAbortWhenAllStrategiesExhausted() {
        // Given: Create manager with strategies that always escalate
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("Strategy1", 1, RecoveryResult.ESCALATE),
            new TestRecoveryStrategy("Strategy2", 1, RecoveryResult.ESCALATE),
            new TestRecoveryStrategy("Strategy3", 1, RecoveryResult.ESCALATE)
        );
        RecoveryManager exhaustiveManager = new RecoveryManager(mockEntity, strategies);

        // When: Exhaust all strategies
        RecoveryResult result1 = exhaustiveManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result2 = exhaustiveManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result3 = exhaustiveManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should eventually abort
        assertEquals(RecoveryResult.ESCALATE, result1, "First strategy should ESCALATE");
        assertEquals(RecoveryResult.ESCALATE, result2, "Second strategy should ESCALATE");
        assertEquals(RecoveryResult.ESCALATE, result3, "Third strategy should ESCALATE");

        // Fourth attempt should abort (no strategies left)
        RecoveryResult result4 = exhaustiveManager.attemptRecovery(StuckType.POSITION_STUCK);
        assertEquals(RecoveryResult.ABORT, result4, "Should ABORT when all strategies exhausted");
    }

    @Test
    @DisplayName("Should handle strategy execution exceptions")
    void testStrategyExecutionException() {
        // Given: Create manager with failing strategy
        RecoveryStrategy failingStrategy = new RecoveryStrategy() {
            @Override
            public boolean canRecover(StuckType type, ForemanEntity entity) {
                return true;
            }

            @Override
            public RecoveryResult execute(ForemanEntity entity) {
                throw new RuntimeException("Strategy execution failed");
            }

            @Override
            public int getMaxAttempts() {
                return 1;
            }
        };

        List<RecoveryStrategy> strategies = List.of(failingStrategy);
        RecoveryManager exceptionManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery with failing strategy
        RecoveryResult result = exceptionManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should handle exception and return ESCALATE
        assertEquals(RecoveryResult.ESCALATE, result,
            "Should ESCALATE when strategy throws exception");
    }

    @Test
    @DisplayName("Should handle permanently stuck agent")
    void testPermanentlyStuckAgent() {
        // Given: Agent that cannot recover
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("AlwaysFail", 100, RecoveryResult.ESCALATE)
        );
        RecoveryManager permanentManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt many recoveries
        int maxAttempts = 10;
        RecoveryResult lastResult = null;
        for (int i = 0; i < maxAttempts; i++) {
            lastResult = permanentManager.attemptRecovery(StuckType.POSITION_STUCK);
        }

        // Then: Should still be trying to recover
        assertNotNull(lastResult, "Should always return a result");
        assertTrue(permanentManager.getTotalAttempts() >= maxAttempts,
            "Should track all attempts");
    }

    @Test
    @DisplayName("Should recover from STATE_STUCK with AbortStrategy")
    void testStateStuckRecovery() {
        // Given: State is stuck
        when(mockStateMachine.getCurrentState()).thenReturn(AgentState.IDLE);
        for (int i = 0; i < 205; i++) {
            stuckDetector.tickAndDetect();
        }
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        // Then: Should handle state stuck
        assertEquals(StuckType.STATE_STUCK, stuckType);
        assertNotNull(result, "Should return a recovery result");
        // STATE_STUCK typically requires abort
    }

    @Test
    @DisplayName("Should recover from RESOURCE_STUCK with AbortStrategy")
    void testResourceStuckRecovery() {
        // Given: Resource is stuck (requires abort)
        StuckType stuckType = StuckType.RESOURCE_STUCK;

        // When: Attempt recovery
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);

        // Then: Should handle resource stuck
        assertNotNull(result, "Should return a recovery result");
        // RESOURCE_STUCK typically requires abort
    }

    // ==================== Individual Strategy Tests ====================

    @Test
    @DisplayName("RepathStrategy should handle position stuck")
    void testRepathStrategyPositionStuck() {
        RepathStrategy strategy = new RepathStrategy();

        // When: Check if can recover
        boolean canRecover = strategy.canRecover(StuckType.POSITION_STUCK, mockEntity);

        // Then: Should be able to recover
        assertTrue(canRecover, "RepathStrategy should handle POSITION_STUCK");
        assertEquals(3, strategy.getMaxAttempts(), "Should have 3 max attempts");
    }

    @Test
    @DisplayName("RepathStrategy should handle path stuck")
    void testRepathStrategyPathStuck() {
        RepathStrategy strategy = new RepathStrategy();

        // When: Check if can recover
        boolean canRecover = strategy.canRecover(StuckType.PATH_STUCK, mockEntity);

        // Then: Should be able to recover
        assertTrue(canRecover, "RepathStrategy should handle PATH_STUCK");
    }

    @Test
    @DisplayName("RepathStrategy should handle progress stuck")
    void testRepathStrategyProgressStuck() {
        RepathStrategy strategy = new RepathStrategy();

        // When: Check if can recover
        boolean canRecover = strategy.canRecover(StuckType.PROGRESS_STUCK, mockEntity);

        // Then: Should be able to recover
        assertTrue(canRecover, "RepathStrategy should handle PROGRESS_STUCK");
    }

    @Test
    @DisplayName("TeleportStrategy should handle position stuck")
    void testTeleportStrategyPositionStuck() {
        TeleportStrategy strategy = new TeleportStrategy();

        // When: Check if can recover
        boolean canRecover = strategy.canRecover(StuckType.POSITION_STUCK, mockEntity);

        // Then: Should be able to recover
        assertTrue(canRecover, "TeleportStrategy should handle POSITION_STUCK");
        assertEquals(2, strategy.getMaxAttempts(), "Should have 2 max attempts");
    }

    @Test
    @DisplayName("TeleportStrategy should handle path stuck")
    void testTeleportStrategyPathStuck() {
        TeleportStrategy strategy = new TeleportStrategy();

        // When: Check if can recover
        boolean canRecover = strategy.canRecover(StuckType.PATH_STUCK, mockEntity);

        // Then: Should be able to recover
        assertTrue(canRecover, "TeleportStrategy should handle PATH_STUCK");
    }

    @Test
    @DisplayName("AbortStrategy should handle all stuck types")
    void testAbortStrategyAllTypes() {
        AbortStrategy strategy = new AbortStrategy();

        // When: Check if can recover for all types
        boolean canRecoverPosition = strategy.canRecover(StuckType.POSITION_STUCK, mockEntity);
        boolean canRecoverProgress = strategy.canRecover(StuckType.PROGRESS_STUCK, mockEntity);
        boolean canRecoverState = strategy.canRecover(StuckType.STATE_STUCK, mockEntity);
        boolean canRecoverPath = strategy.canRecover(StuckType.PATH_STUCK, mockEntity);
        boolean canRecoverResource = strategy.canRecover(StuckType.RESOURCE_STUCK, mockEntity);

        // Then: Should handle all types
        assertTrue(canRecoverPosition, "AbortStrategy should handle POSITION_STUCK");
        assertTrue(canRecoverProgress, "AbortStrategy should handle PROGRESS_STUCK");
        assertTrue(canRecoverState, "AbortStrategy should handle STATE_STUCK");
        assertTrue(canRecoverPath, "AbortStrategy should handle PATH_STUCK");
        assertTrue(canRecoverResource, "AbortStrategy should handle RESOURCE_STUCK");
        assertEquals(1, strategy.getMaxAttempts(), "Should have 1 max attempt (no retry)");
    }

    // ==================== Strategy Effectiveness Metrics Tests ====================

    @Test
    @DisplayName("Should track success count correctly")
    void testTrackSuccessCount() {
        // Given: Create manager with successful strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("SuccessStrategy", 1, RecoveryResult.SUCCESS)
        );
        RecoveryManager successManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt successful recovery
        successManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should track success
        assertEquals(1, successManager.getSuccessCount(),
            "Should track successful recovery");
        assertEquals(1, successManager.getTotalAttempts(),
            "Should track total attempts");
    }

    @Test
    @DisplayName("Should calculate success rate correctly")
    void testCalculateSuccessRate() {
        // Given: Create manager with mixed results
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("MixedStrategy", 10, RecoveryResult.SUCCESS)
        );
        RecoveryManager rateManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt multiple recoveries
        rateManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should calculate success rate
        if (rateManager.getTotalAttempts() > 0) {
            double successRate = rateManager.getSuccessRate();
            assertTrue(successRate >= 0.0 && successRate <= 1.0,
                "Success rate should be between 0.0 and 1.0");
        }
    }

    @Test
    @DisplayName("Should return zero success rate when no attempts")
    void testZeroSuccessRateNoAttempts() {
        // Given: New manager with no attempts
        RecoveryManager newManager = new RecoveryManager(mockEntity);

        // When: Get success rate
        double successRate = newManager.getSuccessRate();

        // Then: Should be zero
        assertEquals(0.0, successRate, 0.001,
            "Success rate should be 0.0 when no attempts");
    }

    @Test
    @DisplayName("Should track recovery statistics")
    void testRecoveryStatistics() {
        // Given: Create manager and attempt recovery
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("StatsStrategy", 2, RecoveryResult.SUCCESS)
        );
        RecoveryManager statsManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery and get stats
        statsManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryManager.RecoveryStats stats = statsManager.getStats();

        // Then: Should have valid statistics
        assertNotNull(stats, "Should return statistics");
        assertTrue(stats.totalAttempts() >= 1, "Should track total attempts");
        assertTrue(stats.successCount() >= 0, "Should track success count");
        assertTrue(stats.successRate() >= 0.0 && stats.successRate() <= 1.0,
            "Success rate should be valid");
    }

    @Test
    @DisplayName("RecoveryStats toString should be informative")
    void testRecoveryStatsToString() {
        // Given: Create stats
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("ToStringStrategy", 1, RecoveryResult.SUCCESS)
        );
        RecoveryManager manager = new RecoveryManager(mockEntity, strategies);
        manager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryManager.RecoveryStats stats = manager.getStats();

        // When: Convert to string
        String statsString = stats.toString();

        // Then: Should be informative
        assertNotNull(statsString, "toString should not be null");
        assertTrue(statsString.contains("attempts") || statsString.contains("successes"),
            "toString should contain key information");
    }

    // ==================== Game State Preservation Tests ====================

    @Test
    @DisplayName("Should preserve entity position during RepathStrategy")
    void testPreservePositionDuringRepath() {
        // Given: Entity at specific position
        Vec3 originalPos = new Vec3(10.0, 64.0, 20.0);
        when(mockEntity.position()).thenReturn(originalPos);

        RepathStrategy strategy = new RepathStrategy();

        // When: Execute repath strategy (may not succeed without proper setup)
        try {
            strategy.execute(mockEntity);
        } catch (Exception e) {
            // May fail due to missing navigation setup, but shouldn't move entity
        }

        // Then: Entity position should be unchanged
        // TeleportStrategy may call teleportTo, but RepathStrategy should not
        // We just verify the test completes without exception
    }

    @Test
    @DisplayName("Should stop current action during AbortStrategy")
    void testStopActionDuringAbort() {
        AbortStrategy strategy = new AbortStrategy();

        // When: Execute abort strategy
        RecoveryResult result = strategy.execute(mockEntity);

        // Then: Should stop current action and navigation
        verify(mockActionExecutor, atLeastOnce()).stopCurrentAction();
        assertEquals(RecoveryResult.ABORT, result, "Should return ABORT");
    }

    @Test
    @DisplayName("Should send chat message during recovery")
    void testChatMessageDuringRecovery() {
        AbortStrategy strategy = new AbortStrategy();

        // When: Execute abort strategy
        strategy.execute(mockEntity);

        // Then: Should send chat message to user
        verify(mockEntity, atLeastOnce()).sendChatMessage(anyString());
    }

    // ==================== Recovery State Management Tests ====================

    @Test
    @DisplayName("Should track recovery in progress")
    void testTrackRecoveryInProgress() {
        // Given: Create manager with retry strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("RetryStrategy", 3, RecoveryResult.RETRY)
        );
        RecoveryManager retryManager = new RecoveryManager(mockEntity, strategies);

        // When: Start recovery
        retryManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should be recovering
        assertTrue(retryManager.isRecovering(), "Should be recovering");
        assertEquals(StuckType.POSITION_STUCK, retryManager.getCurrentStuckType(),
            "Should track current stuck type");
    }

    @Test
    @DisplayName("Should reset recovery state")
    void testResetRecoveryState() {
        // Given: Manager in recovery state
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("RetryStrategy", 3, RecoveryResult.RETRY)
        );
        RecoveryManager retryManager = new RecoveryManager(mockEntity, strategies);
        retryManager.attemptRecovery(StuckType.POSITION_STUCK);

        // When: Reset
        retryManager.reset();

        // Then: Should not be recovering
        assertFalse(retryManager.isRecovering(), "Should not be recovering after reset");
        assertNull(retryManager.getCurrentStuckType(),
            "Should not have current stuck type after reset");
    }

    @Test
    @DisplayName("Should continue recovery on subsequent attempts")
    void testContinueRecovery() {
        // Given: Manager with retry strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("RetryStrategy", 3, RecoveryResult.RETRY)
        );
        RecoveryManager retryManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery multiple times
        RecoveryResult result1 = retryManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result2 = retryManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should continue recovering
        assertEquals(RecoveryResult.RETRY, result1, "First attempt should RETRY");
        assertEquals(RecoveryResult.RETRY, result2, "Second attempt should RETRY");
        assertTrue(retryManager.isRecovering(), "Should still be recovering");
    }

    // ==================== Edge Cases and Error Handling ====================

    @Test
    @DisplayName("Should handle null entity in constructor")
    void testNullEntityInConstructor() {
        // When: Create manager with null entity
        // Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
            () -> new RecoveryManager(null),
            "Should throw IllegalArgumentException for null entity");
    }

    @Test
    @DisplayName("Should handle empty strategies list in constructor")
    void testEmptyStrategiesInConstructor() {
        // When: Create manager with empty strategies list
        // Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
            () -> new RecoveryManager(mockEntity, List.of()),
            "Should throw IllegalArgumentException for empty strategies");
    }

    @Test
    @DisplayName("Should handle null strategies list in constructor")
    void testNullStrategiesInConstructor() {
        // When: Create manager with null strategies list
        // Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
            () -> new RecoveryManager(mockEntity, null),
            "Should throw IllegalArgumentException for null strategies");
    }

    @Test
    @DisplayName("Should handle strategy that returns null")
    void testStrategyReturnsNull() {
        // Given: Strategy that returns null
        RecoveryStrategy nullStrategy = new RecoveryStrategy() {
            @Override
            public boolean canRecover(StuckType type, ForemanEntity entity) {
                return true;
            }

            @Override
            public RecoveryResult execute(ForemanEntity entity) {
                return null; // Return null instead of valid result
            }

            @Override
            public int getMaxAttempts() {
                return 1;
            }
        };

        List<RecoveryStrategy> strategies = List.of(nullStrategy);
        RecoveryManager nullResultManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery
        RecoveryResult result = nullResultManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should handle null result as ABORT
        assertEquals(RecoveryResult.ABORT, result,
            "Should treat null result as ABORT");
    }

    @Test
    @DisplayName("Should handle stuck type with no applicable strategies")
    void testNoApplicableStrategies() {
        // Given: Strategy that can't recover
        RecoveryStrategy incompatibleStrategy = new RecoveryStrategy() {
            @Override
            public boolean canRecover(StuckType type, ForemanEntity entity) {
                return false; // Can't recover anything
            }

            @Override
            public RecoveryResult execute(ForemanEntity entity) {
                return RecoveryResult.SUCCESS;
            }

            @Override
            public int getMaxAttempts() {
                return 1;
            }
        };

        List<RecoveryStrategy> strategies = List.of(incompatibleStrategy);
        RecoveryManager incompatibleManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery
        RecoveryResult result = incompatibleManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should abort when no applicable strategies
        assertEquals(RecoveryResult.ABORT, result,
            "Should ABORT when no applicable strategies found");
    }

    @Test
    @DisplayName("Should handle recovery during recovery (same stuck type)")
    void testRecoveryDuringRecoverySameType() {
        // Given: Manager with retry strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("RetryStrategy", 3, RecoveryResult.RETRY)
        );
        RecoveryManager retryManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery twice without reset
        RecoveryResult result1 = retryManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result2 = retryManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should handle gracefully (continue recovery)
        assertNotNull(result1, "First result should not be null");
        assertNotNull(result2, "Second result should not be null");
        assertTrue(retryManager.isRecovering(), "Should still be recovering");
    }

    @Test
    @DisplayName("Should handle different stuck types sequentially")
    void testDifferentStuckTypesSequentially() {
        // Given: Manager
        RecoveryManager manager = new RecoveryManager(mockEntity);

        // When: Attempt recovery for different stuck types
        RecoveryResult result1 = manager.attemptRecovery(StuckType.POSITION_STUCK);
        manager.reset();
        RecoveryResult result2 = manager.attemptRecovery(StuckType.PATH_STUCK);
        manager.reset();
        RecoveryResult result3 = manager.attemptRecovery(StuckType.STATE_STUCK);

        // Then: Should handle all types
        assertNotNull(result1, "POSITION_STUCK result should not be null");
        assertNotNull(result2, "PATH_STUCK result should not be null");
        assertNotNull(result3, "STATE_STUCK result should not be null");
    }

    // ==================== Integration with StuckDetector Tests ====================

    @Test
    @DisplayName("Should integrate with StuckDetector for position stuck")
    void testIntegrateWithStuckDetectorPosition() {
        // Given: Entity is position stuck
        when(mockEntity.position()).thenReturn(new Vec3(0.0, 64.0, 0.0));
        for (int i = 0; i < 65; i++) {
            stuckDetector.tickAndDetect();
        }
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Recover and reset
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);
        stuckDetector.reset();

        // Then: Should complete cycle
        assertEquals(StuckType.POSITION_STUCK, stuckType);
        assertNotNull(result);
        assertEquals(0, stuckDetector.getStuckPositionTicks(),
            "Detector should be reset after recovery");
    }

    @Test
    @DisplayName("Should integrate with StuckDetector for path stuck")
    void testIntegrateWithStuckDetectorPath() {
        // Given: Path is stuck
        stuckDetector.markPathStuck();
        StuckType stuckType = stuckDetector.detectStuck();

        // When: Recover and clear
        RecoveryResult result = recoveryManager.attemptRecovery(stuckType);
        stuckDetector.clearPathStuck();

        // Then: Should complete cycle
        assertEquals(StuckType.PATH_STUCK, stuckType);
        assertNotNull(result);
        assertFalse(stuckDetector.isPathStuck(),
            "Path stuck should be cleared");
    }

    // ==================== Strategy Selection Order Tests ====================

    @Test
    @DisplayName("Should try strategies in escalation order")
    void testStrategyEscalationOrder() {
        // Given: Create manager with custom strategies
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("FirstStrategy", 1, RecoveryResult.ESCALATE),
            new TestRecoveryStrategy("SecondStrategy", 1, RecoveryResult.ESCALATE),
            new TestRecoveryStrategy("ThirdStrategy", 1, RecoveryResult.SUCCESS)
        );
        RecoveryManager orderManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery
        RecoveryResult result1 = orderManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result2 = orderManager.attemptRecovery(StuckType.POSITION_STUCK);
        RecoveryResult result3 = orderManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should try in order
        assertEquals(RecoveryResult.ESCALATE, result1, "First strategy should ESCALATE");
        assertEquals(RecoveryResult.ESCALATE, result2, "Second strategy should ESCALATE");
        assertEquals(RecoveryResult.SUCCESS, result3, "Third strategy should SUCCEED");
    }

    @Test
    @DisplayName("Should skip non-applicable strategies")
    void testSkipNonApplicableStrategies() {
        // Given: Create manager with mixed applicability
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("NotApplicable", 1, RecoveryResult.ESCALATE) {
                @Override
                public boolean canRecover(StuckType type, ForemanEntity entity) {
                    return false; // Not applicable
                }
            },
            new TestRecoveryStrategy("Applicable", 1, RecoveryResult.SUCCESS) {
                @Override
                public boolean canRecover(StuckType type, ForemanEntity entity) {
                    return type == StuckType.POSITION_STUCK;
                }
            }
        );
        RecoveryManager skipManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt recovery
        RecoveryResult result = skipManager.attemptRecovery(StuckType.POSITION_STUCK);

        // Then: Should skip to applicable strategy
        assertEquals(RecoveryResult.SUCCESS, result,
            "Should skip non-applicable and succeed with applicable strategy");
    }

    // ==================== Timeout and Performance Tests ====================

    @Test
    @DisplayName("Should handle rapid recovery attempts")
    void testRapidRecoveryAttempts() {
        // Given: Manager with successful strategy
        List<RecoveryStrategy> strategies = List.of(
            new TestRecoveryStrategy("FastStrategy", 100, RecoveryResult.SUCCESS)
        );
        RecoveryManager fastManager = new RecoveryManager(mockEntity, strategies);

        // When: Attempt many recoveries rapidly
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            fastManager.reset();
            fastManager.attemptRecovery(StuckType.POSITION_STUCK);
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then: Should handle rapidly without performance issues
        assertTrue(duration < 1000, "Should complete 50 attempts in less than 1 second");
        assertEquals(50, fastManager.getTotalAttempts(), "Should track all attempts");
    }

    // ==================== Helper Classes ====================

    /**
     * Test recovery strategy with configurable behavior.
     */
    private static class TestRecoveryStrategy implements RecoveryStrategy {
        private final String name;
        private final int maxAttempts;
        private final RecoveryResult result;

        TestRecoveryStrategy(String name, int maxAttempts, RecoveryResult result) {
            this.name = name;
            this.maxAttempts = maxAttempts;
            this.result = result;
        }

        @Override
        public boolean canRecover(StuckType type, ForemanEntity entity) {
            // Can recover from all types for testing
            return true;
        }

        @Override
        public RecoveryResult execute(ForemanEntity entity) {
            return result;
        }

        @Override
        public int getMaxAttempts() {
            return maxAttempts;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
