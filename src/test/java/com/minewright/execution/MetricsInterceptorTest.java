package com.minewright.execution;

import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetricsInterceptor}.
 *
 * Tests cover:
 * <ul>
 *   <li>Metrics collection on beforeAction</li>
 *   <li>Success/failure tracking on afterAction</li>
 *   <li>Error tracking on onError</li>
 *   <li>Duration calculation and tracking</li>
 *   <li>Metrics retrieval by action type</li>
 *   <li>Metrics retrieval for all action types</li>
 *   <li>Metrics reset functionality</li>
 *   <li>Multiple action types tracked separately</li>
 *   <li>Average duration calculation</li>
 *   <li>Success rate calculation</li>
 * </ul>
 */
@DisplayName("MetricsInterceptor Tests")
class MetricsInterceptorTest {

    private MetricsInterceptor interceptor;
    private ActionContext mockContext;
    private ActionResult mockResult;

    @BeforeEach
    void setUp() {
        interceptor = new MetricsInterceptor();
        mockContext = org.mockito.Mockito.mock(ActionContext.class);
        mockResult = org.mockito.Mockito.mock(ActionResult.class);
    }

    // ==================== beforeAction Tests ====================

    @Test
    @DisplayName("beforeAction increments execution count")
    void testBeforeActionIncrementsExecutionCount() {
        MineBlockAction action = new MineBlockAction();

        interceptor.beforeAction(action, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertNotNull(metrics);
        assertEquals(1, metrics.totalExecutions());
    }

    @Test
    @DisplayName("beforeAction returns true to allow execution")
    void testBeforeActionReturnsTrue() {
        MineBlockAction action = new MineBlockAction();

        boolean result = interceptor.beforeAction(action, mockContext);

        assertTrue(result, "beforeAction should always return true");
    }

    @Test
    @DisplayName("beforeAction records start time for duration calculation")
    void testBeforeActionRecordsStartTime() throws InterruptedException {
        MineBlockAction action = new MineBlockAction();

        interceptor.beforeAction(action, mockContext);
        Thread.sleep(10);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertNotNull(metrics);
        assertTrue(metrics.avgDurationMs() >= 10, "Average duration should be at least 10ms");
    }

    @Test
    @DisplayName("beforeAction handles null action gracefully")
    void testBeforeActionWithNullAction() {
        boolean result = interceptor.beforeAction(null, mockContext);

        assertTrue(result);
        // Should create metrics for "unknown" action type
        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("unknown");
        assertNotNull(metrics);
    }

    @Test
    @DisplayName("beforeAction tracks multiple executions")
    void testBeforeActionMultipleExecutions() {
        MineBlockAction action = new MineBlockAction();

        interceptor.beforeAction(action, mockContext);
        interceptor.beforeAction(action, mockContext);
        interceptor.beforeAction(action, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(3, metrics.totalExecutions());
    }

    // ==================== afterAction Tests ====================

    @Test
    @DisplayName("afterAction increments success count when result is successful")
    void testAfterActionIncrementsSuccessCount() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(1, metrics.successes());
        assertEquals(0, metrics.failures());
    }

    @Test
    @DisplayName("afterAction increments failure count when result is not successful")
    void testAfterActionIncrementsFailureCount() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(false);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(0, metrics.successes());
        assertEquals(1, metrics.failures());
    }

    @Test
    @DisplayName("afterAction calculates and records duration")
    void testAfterActionRecordsDuration() throws InterruptedException {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        Thread.sleep(50);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertTrue(metrics.avgDurationMs() >= 50, "Duration should be at least 50ms");
        assertTrue(metrics.totalDurationMs() >= 50, "Total duration should be at least 50ms");
    }

    @Test
    @DisplayName("afterAction calculates average duration across multiple executions")
    void testAfterActionAverageDuration() throws InterruptedException {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        // First execution with ~20ms duration
        interceptor.beforeAction(action, mockContext);
        Thread.sleep(20);
        interceptor.afterAction(action, mockResult, mockContext);

        // Second execution with ~30ms duration
        interceptor.beforeAction(action, mockContext);
        Thread.sleep(30);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(2, metrics.totalExecutions());
        // Average should be around 25ms (with some tolerance for timing variations)
        assertTrue(metrics.avgDurationMs() >= 20 && metrics.avgDurationMs() <= 40,
            "Average duration should be between 20 and 40ms");
    }

    @Test
    @DisplayName("afterAction handles null action gracefully")
    void testAfterActionWithNullAction() {
        assertDoesNotThrow(() -> interceptor.afterAction(null, mockResult, mockContext));
    }

    // ==================== onError Tests ====================

    @Test
    @DisplayName("onError increments error count")
    void testOnErrorIncrementsErrorCount() {
        MineBlockAction action = new MineBlockAction();
        Exception testException = new RuntimeException("Test error");

        interceptor.beforeAction(action, mockContext);
        interceptor.onError(action, testException, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(1, metrics.errors());
    }

    @Test
    @DisplayName("onError cleans up start time")
    void testOnErrorCleansUpStartTime() throws InterruptedException {
        MineBlockAction action = new MineBlockAction();
        Exception testException = new RuntimeException("Test error");

        interceptor.beforeAction(action, mockContext);
        Thread.sleep(10);
        interceptor.onError(action, testException, mockContext);

        // Should not throw exception or create negative metrics
        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertNotNull(metrics);
    }

    @Test
    @DisplayName("onError returns false to propagate exception")
    void testOnErrorReturnsFalse() {
        MineBlockAction action = new MineBlockAction();
        Exception testException = new RuntimeException("Test error");

        boolean result = interceptor.onError(action, testException, mockContext);

        assertFalse(result, "onError should return false to propagate exception");
    }

    @Test
    @DisplayName("onError handles null action gracefully")
    void testOnErrorWithNullAction() {
        Exception testException = new RuntimeException("Test error");

        boolean result = interceptor.onError(null, testException, mockContext);

        assertFalse(result);
    }

    // ==================== getMetrics Tests ====================

    @Test
    @DisplayName("getMetrics returns null for non-existent action type")
    void testGetMetricsForNonExistentActionType() {
        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("nonexistent");

        assertNull(metrics);
    }

    @Test
    @DisplayName("getMetrics returns snapshot for existing action type")
    void testGetMetricsForExistingActionType() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertNotNull(metrics);
        assertEquals(1, metrics.totalExecutions());
    }

    @Test
    @DisplayName("getMetrics returns immutable snapshot")
    void testGetMetricsReturnsImmutableSnapshot() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot snapshot1 = interceptor.getMetrics("mineblock");

        // Execute another action
        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        // Original snapshot should not change
        assertEquals(1, snapshot1.totalExecutions());

        // New snapshot should show updated count
        MetricsInterceptor.MetricsSnapshot snapshot2 = interceptor.getMetrics("mineblock");
        assertEquals(2, snapshot2.totalExecutions());
    }

    // ==================== getAllMetrics Tests ====================

    @Test
    @DisplayName("getAllMetrics returns empty map when no metrics collected")
    void testGetAllMetricsReturnsEmptyMap() {
        Map<String, MetricsInterceptor.MetricsSnapshot> allMetrics = interceptor.getAllMetrics();

        assertNotNull(allMetrics);
        assertTrue(allMetrics.isEmpty());
    }

    @Test
    @DisplayName("getAllMetrics returns all tracked action types")
    void testGetAllMetricsReturnsAllActionTypes() {
        // Track MineBlockAction
        MineBlockAction mineAction = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);
        interceptor.beforeAction(mineAction, mockContext);
        interceptor.afterAction(mineAction, mockResult, mockContext);

        // Track BuildStructureAction
        BuildStructureAction buildAction = new BuildStructureAction();
        interceptor.beforeAction(buildAction, mockContext);
        interceptor.afterAction(buildAction, mockResult, mockContext);

        Map<String, MetricsInterceptor.MetricsSnapshot> allMetrics = interceptor.getAllMetrics();

        assertEquals(2, allMetrics.size());
        assertTrue(allMetrics.containsKey("mineblock"));
        assertTrue(allMetrics.containsKey("buildstructure"));
    }

    // ==================== reset Tests ====================

    @Test
    @DisplayName("reset clears all collected metrics")
    void testResetClearsAllMetrics() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        assertNotNull(interceptor.getMetrics("mineblock"));

        interceptor.reset();

        assertNull(interceptor.getMetrics("mineblock"));
        assertTrue(interceptor.getAllMetrics().isEmpty());
    }

    @Test
    @DisplayName("reset allows collection to start fresh")
    void testResetAllowsFreshCollection() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        // First collection
        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);
        assertEquals(1, interceptor.getMetrics("mineblock").totalExecutions());

        // Reset
        interceptor.reset();

        // Fresh collection
        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        assertEquals(1, interceptor.getMetrics("mineblock").totalExecutions());
    }

    @Test
    @DisplayName("reset can be called multiple times")
    void testResetMultipleTimes() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        interceptor.reset();
        interceptor.reset();
        interceptor.reset();

        // Should not throw exception
        assertDoesNotThrow(() -> interceptor.reset());
    }

    // ==================== Success Rate Tests ====================

    @Test
    @DisplayName("getSuccessRate returns 0.0 for no executions")
    void testGetSuccessRateNoExecutions() {
        MineBlockAction action = new MineBlockAction();

        // Only call beforeAction, not afterAction
        interceptor.beforeAction(action, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("getSuccessRate calculates correctly for mixed results")
    void testGetSuccessRateMixedResults() {
        MineBlockAction action = new MineBlockAction();

        // 3 successes
        for (int i = 0; i < 3; i++) {
            interceptor.beforeAction(action, mockContext);
            org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);
            interceptor.afterAction(action, mockResult, mockContext);
        }

        // 1 failure
        interceptor.beforeAction(action, mockContext);
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(false);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(0.75, metrics.getSuccessRate(), 0.001,
            "Success rate should be 0.75 (3/4)");
    }

    @Test
    @DisplayName("getSuccessRate returns 1.0 for all successes")
    void testGetSuccessRateAllSuccesses() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        for (int i = 0; i < 5; i++) {
            interceptor.beforeAction(action, mockContext);
            interceptor.afterAction(action, mockResult, mockContext);
        }

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(1.0, metrics.getSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("getSuccessRate returns 0.0 for all failures")
    void testGetSuccessRateAllFailures() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            interceptor.beforeAction(action, mockContext);
            interceptor.afterAction(action, mockResult, mockContext);
        }

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
    }

    // ==================== Multiple Action Types Tests ====================

    @Test
    @DisplayName("Metrics are tracked separately for different action types")
    void testSeparateTrackingForDifferentActionTypes() {
        // Track mine actions
        MineBlockAction mineAction = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);
        for (int i = 0; i < 3; i++) {
            interceptor.beforeAction(mineAction, mockContext);
            interceptor.afterAction(mineAction, mockResult, mockContext);
        }

        // Track build actions
        BuildStructureAction buildAction = new BuildStructureAction();
        for (int i = 0; i < 5; i++) {
            interceptor.beforeAction(buildAction, mockContext);
            interceptor.afterAction(buildAction, mockResult, mockContext);
        }

        MetricsInterceptor.MetricsSnapshot mineMetrics = interceptor.getMetrics("mineblock");
        MetricsInterceptor.MetricsSnapshot buildMetrics = interceptor.getMetrics("buildstructure");

        assertEquals(3, mineMetrics.totalExecutions());
        assertEquals(5, buildMetrics.totalExecutions());
    }

    // ==================== Interceptor Priority Tests ====================

    @Test
    @DisplayName("MetricsInterceptor has correct priority")
    void testMetricsInterceptorPriority() {
        assertEquals(900, interceptor.getPriority(),
            "MetricsInterceptor should have priority 900");
    }

    @Test
    @DisplayName("MetricsInterceptor has correct name")
    void testMetricsInterceptorName() {
        assertEquals("MetricsInterceptor", interceptor.getName());
    }

    // ==================== MetricsSnapshot toString Tests ====================

    @Test
    @DisplayName("MetricsSnapshot toString returns formatted string")
    void testMetricsSnapshotToString() {
        MineBlockAction action = new MineBlockAction();
        org.mockito.Mockito.when(mockResult.isSuccess()).thenReturn(true);

        interceptor.beforeAction(action, mockContext);
        interceptor.afterAction(action, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics = interceptor.getMetrics("mineblock");
        String str = metrics.toString();

        assertNotNull(str);
        assertTrue(str.contains("total="));
        assertTrue(str.contains("success="));
        assertTrue(str.contains("avgDuration="));
    }

    // ==================== Test Classes ====================

    static class MineBlockAction extends BaseAction {
        public MineBlockAction() { super(null, null); }
        @Override protected void onTick() {}
        @Override protected void onStart() {}
        @Override protected void onCancel() {}
        @Override public String getDescription() { return "MineBlockAction"; }
    }

    static class BuildStructureAction extends BaseAction {
        public BuildStructureAction() { super(null, null); }
        @Override protected void onTick() {}
        @Override protected void onStart() {}
        @Override protected void onCancel() {}
        @Override public String getDescription() { return "BuildStructureAction"; }
    }
}
