package com.minewright.execution;

import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import com.minewright.event.ActionCompletedEvent;
import com.minewright.event.ActionStartedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link InterceptorChain} with real interceptor implementations.
 *
 * Tests cover:
 * <ul>
 *   <li>Full interceptor chain execution with LoggingInterceptor</li>
 *   <li>Full interceptor chain execution with MetricsInterceptor</li>
 *   <li>Full interceptor chain execution with EventPublishingInterceptor</li>
 *   <li>All interceptors working together</li>
 *   <li>Interceptor execution order (priority-based)</li>
 *   <li>Chain continuation and termination</li>
 *   <li>Exception handling across interceptors</li>
 *   <li>Thread-safety of concurrent chain execution</li>
 *   <li>Metrics accuracy across chain execution</li>
 *   <li>Event publishing across chain execution</li>
 * </ul>
 */
@DisplayName("InterceptorChain Integration Tests")
class InterceptorChainIntegrationTest {

    private InterceptorChain chain;
    private LoggingInterceptor loggingInterceptor;
    private MetricsInterceptor metricsInterceptor;
    private EventPublishingInterceptor eventPublishingInterceptor;
    private com.minewright.event.SimpleEventBus eventBus;

    private BaseAction mockAction;
    private ActionContext mockContext;
    private ActionResult mockResult;

    private static final String TEST_AGENT_ID = "integration-test-agent";

    @BeforeEach
    void setUp() {
        chain = new InterceptorChain();
        eventBus = new com.minewright.event.SimpleEventBus();

        loggingInterceptor = new LoggingInterceptor();
        metricsInterceptor = new MetricsInterceptor();
        eventPublishingInterceptor = new EventPublishingInterceptor(eventBus, TEST_AGENT_ID);

        // Add interceptors in non-priority order to test sorting
        chain.addInterceptor(metricsInterceptor);
        chain.addInterceptor(eventPublishingInterceptor);
        chain.addInterceptor(loggingInterceptor);

        mockAction = mock(BaseAction.class);
        mockContext = mock(ActionContext.class);
        mockResult = mock(ActionResult.class);
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    // ==================== Execution Order Tests ====================

    @Test
    @DisplayName("Interceptors are executed in priority order for beforeAction")
    void testBeforeActionPriorityOrder() {
        List<String> executionOrder = new ArrayList<>();

        // Create custom interceptors to track execution order
        ActionInterceptor lowPriority = new TestInterceptor("Low", 10, executionOrder);
        ActionInterceptor mediumPriority = new TestInterceptor("Medium", 50, executionOrder);
        ActionInterceptor highPriority = new TestInterceptor("High", 100, executionOrder);

        InterceptorChain testChain = new InterceptorChain();
        testChain.addInterceptor(lowPriority);
        testChain.addInterceptor(mediumPriority);
        testChain.addInterceptor(highPriority);

        testChain.executeBeforeAction(mockAction, mockContext);

        // Should be: High, Medium, Low
        assertEquals(3, executionOrder.size());
        assertEquals("High", executionOrder.get(0));
        assertEquals("Medium", executionOrder.get(1));
        assertEquals("Low", executionOrder.get(2));
    }

    @Test
    @DisplayName("Interceptors are executed in reverse order for afterAction")
    void testAfterActionReverseOrder() {
        List<String> executionOrder = new ArrayList<>();

        ActionInterceptor lowPriority = new TestInterceptor("Low", 10, executionOrder);
        ActionInterceptor mediumPriority = new TestInterceptor("Medium", 50, executionOrder);
        ActionInterceptor highPriority = new TestInterceptor("High", 100, executionOrder);

        InterceptorChain testChain = new InterceptorChain();
        testChain.addInterceptor(lowPriority);
        testChain.addInterceptor(mediumPriority);
        testChain.addInterceptor(highPriority);

        testChain.executeAfterAction(mockAction, mockResult, mockContext);

        // Should be: Low, Medium, High (reverse of beforeAction)
        assertEquals(3, executionOrder.size());
        assertEquals("Low", executionOrder.get(0));
        assertEquals("Medium", executionOrder.get(1));
        assertEquals("High", executionOrder.get(2));
    }

    @Test
    @DisplayName("Real interceptors have correct priority order")
    void testRealInterceptorPriorityOrder() {
        List<ActionInterceptor> interceptors = chain.getInterceptors();

        // Expected order: Logging (1000), Metrics (900), EventPublishing (500)
        assertEquals(3, interceptors.size());
        assertEquals("LoggingInterceptor", interceptors.get(0).getName());
        assertEquals("MetricsInterceptor", interceptors.get(1).getName());
        assertEquals("EventPublishingInterceptor", interceptors.get(2).getName());
    }

    // ==================== Full Chain Execution Tests ====================

    @Test
    @DisplayName("Full chain execution for successful action")
    void testFullChainSuccessExecution() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Mined 10 stone");

        List<ActionStartedEvent> startedEvents = new ArrayList<>();
        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, startedEvents::add);
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        // Execute full lifecycle
        boolean beforeResult = chain.executeBeforeAction(mockAction, mockContext);
        assertTrue(beforeResult);

        chain.executeAfterAction(mockAction, mockResult, mockContext);

        // Verify metrics
        MetricsInterceptor.MetricsSnapshot metrics = metricsInterceptor.getMetrics("mineblock");
        assertNotNull(metrics);
        assertEquals(1, metrics.totalExecutions());
        assertEquals(1, metrics.successes());
        assertEquals(0, metrics.failures());

        // Verify events
        assertEquals(1, startedEvents.size());
        assertEquals(1, completedEvents.size());
        assertEquals(TEST_AGENT_ID, startedEvents.get(0).getAgentId());
        assertEquals(TEST_AGENT_ID, completedEvents.get(0).getAgentId());
    }

    @Test
    @DisplayName("Full chain execution for failed action")
    void testFullChainFailureExecution() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn("No stone found");

        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        // Execute full lifecycle
        chain.executeBeforeAction(mockAction, mockContext);
        chain.executeAfterAction(mockAction, mockResult, mockContext);

        // Verify metrics
        MetricsInterceptor.MetricsSnapshot metrics = metricsInterceptor.getMetrics("mineblock");
        assertEquals(1, metrics.totalExecutions());
        assertEquals(0, metrics.successes());
        assertEquals(1, metrics.failures());

        // Verify events
        assertEquals(1, completedEvents.size());
        assertFalse(completedEvents.get(0).isSuccess());
    }

    @Test
    @DisplayName("Full chain execution for error action")
    void testFullChainErrorExecution() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();
        when(mockAction.getDescription()).thenReturn("Mining stone");
        Exception testException = new RuntimeException("Mining failed");

        // Execute error lifecycle
        chain.executeBeforeAction(mockAction, mockContext);
        boolean suppressed = chain.executeOnError(mockAction, testException, mockContext);

        assertFalse(suppressed);

        // Verify metrics
        MetricsInterceptor.MetricsSnapshot metrics = metricsInterceptor.getMetrics("mineblock");
        assertEquals(1, metrics.totalExecutions());
        assertEquals(1, metrics.errors());
    }

    // ==================== Chain Continuation/Termination Tests ====================

    @Test
    @DisplayName("Chain continues when all beforeAction interceptors return true")
    void testChainContinuesWhenAllBeforeApprove() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();

        ActionInterceptor alwaysApprove = mock(ActionInterceptor.class);
        when(alwaysApprove.getName()).thenReturn("AlwaysApprove");
        when(alwaysApprove.getPriority()).thenReturn(100);
        when(alwaysApprove.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(alwaysApprove);

        boolean result = chain.executeBeforeAction(mockAction, mockContext);

        assertTrue(result);
    }

    @Test
    @DisplayName("Chain terminates when beforeAction interceptor returns false")
    void testChainTerminatesOnBeforeRejection() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();

        ActionInterceptor firstApprove = mock(ActionInterceptor.class);
        when(firstApprove.getName()).thenReturn("First");
        when(firstApprove.getPriority()).thenReturn(100);
        when(firstApprove.beforeAction(any(), any())).thenReturn(true);

        ActionInterceptor secondReject = mock(ActionInterceptor.class);
        when(secondReject.getName()).thenReturn("Second");
        when(secondReject.getPriority()).thenReturn(50);
        when(secondReject.beforeAction(any(), any())).thenReturn(false);

        ActionInterceptor thirdNotCalled = mock(ActionInterceptor.class);
        when(thirdNotCalled.getName()).thenReturn("Third");
        when(thirdNotCalled.getPriority()).thenReturn(10);
        when(thirdNotCalled.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(firstApprove);
        chain.addInterceptor(secondReject);
        chain.addInterceptor(thirdNotCalled);

        boolean result = chain.executeBeforeAction(mockAction, mockContext);

        assertFalse(result);
        verify(firstApprove).beforeAction(mockAction, mockContext);
        verify(secondReject).beforeAction(mockAction, mockContext);
        verify(thirdNotCalled, never()).beforeAction(any(), any());
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Chain continues after exception in beforeAction")
    void testChainContinuesAfterBeforeActionException() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();

        ActionInterceptor failingInterceptor = mock(ActionInterceptor.class);
        when(failingInterceptor.getName()).thenReturn("Failing");
        when(failingInterceptor.getPriority()).thenReturn(100);
        when(failingInterceptor.beforeAction(any(), any())).thenThrow(new RuntimeException("Test error"));

        ActionInterceptor succeedingInterceptor = mock(ActionInterceptor.class);
        when(succeedingInterceptor.getName()).thenReturn("Succeeding");
        when(succeedingInterceptor.getPriority()).thenReturn(50);
        when(succeedingInterceptor.beforeAction(any(), any())).thenReturn(true);

        chain.addInterceptor(failingInterceptor);
        chain.addInterceptor(succeedingInterceptor);

        // Should not throw exception
        boolean result = chain.executeBeforeAction(mockAction, mockContext);

        // Failing interceptor threw exception, but succeeding one should still be called
        assertTrue(result);
        verify(succeedingInterceptor).beforeAction(mockAction, mockContext);
    }

    @Test
    @DisplayName("Chain continues after exception in afterAction")
    void testChainContinuesAfterAfterActionException() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();

        ActionInterceptor failingInterceptor = mock(ActionInterceptor.class);
        when(failingInterceptor.getName()).thenReturn("Failing");
        when(failingInterceptor.getPriority()).thenReturn(100);
        doThrow(new RuntimeException("Test error"))
            .when(failingInterceptor).afterAction(any(), any(), any());

        ActionInterceptor succeedingInterceptor = mock(ActionInterceptor.class);
        when(succeedingInterceptor.getName()).thenReturn("Succeeding");
        when(succeedingInterceptor.getPriority()).thenReturn(50);

        chain.addInterceptor(failingInterceptor);
        chain.addInterceptor(succeedingInterceptor);

        // Should not throw exception
        assertDoesNotThrow(() -> chain.executeAfterAction(mockAction, mockResult, mockContext));

        verify(succeedingInterceptor).afterAction(mockAction, mockResult, mockContext);
    }

    // ==================== Thread-Safety Tests ====================

    @Test
    @DisplayName("Chain is thread-safe for concurrent execution")
    void testChainThreadSafety() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int ACTIONS_PER_THREAD = 50;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ACTIONS_PER_THREAD; j++) {
                        BaseAction action = mock(BaseAction.class);
                        doReturn((Class) MineBlockAction.class).when(action).getClass();
                        when(action.getDescription()).thenReturn("Action " + j);

                        ActionResult result = mock(ActionResult.class);
                        when(result.isSuccess()).thenReturn(j % 2 == 0);
                        when(result.getMessage()).thenReturn("Result " + j);

                        chain.executeBeforeAction(action, mockContext);
                        chain.executeAfterAction(action, result, mockContext);

                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(THREAD_COUNT * ACTIONS_PER_THREAD, successCount.get());

        // Verify metrics are consistent
        MetricsInterceptor.MetricsSnapshot metrics = metricsInterceptor.getMetrics("mineblock");
        assertEquals(THREAD_COUNT * ACTIONS_PER_THREAD, metrics.totalExecutions());
    }

    // ==================== Metrics Integration Tests ====================

    @Test
    @DisplayName("Metrics are accurately collected across chain execution")
    void testMetricsAccuracy() throws InterruptedException {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        // Execute multiple actions
        for (int i = 0; i < 10; i++) {
            chain.executeBeforeAction(mockAction, mockContext);
            Thread.sleep(5);
            chain.executeAfterAction(mockAction, mockResult, mockContext);
        }

        MetricsInterceptor.MetricsSnapshot metrics = metricsInterceptor.getMetrics("mineblock");

        assertEquals(10, metrics.totalExecutions());
        assertEquals(10, metrics.successes());
        assertEquals(0, metrics.failures());
        assertEquals(0, metrics.errors());
        // Average duration should be at least 5ms
        assertTrue(metrics.avgDurationMs() >= 5);
    }

    @Test
    @DisplayName("Metrics track different action types separately")
    void testMetricsSeparateTracking() {
        BaseAction mineAction = mock(BaseAction.class);
        doReturn((Class) MineBlockAction.class).when(mineAction).getClass();

        BaseAction buildAction = mock(BaseAction.class);
        doReturn((Class) BuildStructureAction.class).when(buildAction).getClass();

        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        // Execute mine actions
        for (int i = 0; i < 3; i++) {
            chain.executeBeforeAction(mineAction, mockContext);
            chain.executeAfterAction(mineAction, mockResult, mockContext);
        }

        // Execute build actions
        for (int i = 0; i < 5; i++) {
            chain.executeBeforeAction(buildAction, mockContext);
            chain.executeAfterAction(buildAction, mockResult, mockContext);
        }

        Map<String, MetricsInterceptor.MetricsSnapshot> allMetrics = metricsInterceptor.getAllMetrics();

        assertTrue(allMetrics.containsKey("mineblock"));
        assertTrue(allMetrics.containsKey("buildstructure"));

        assertEquals(3, allMetrics.get("mineblock").totalExecutions());
        assertEquals(5, allMetrics.get("buildstructure").totalExecutions());
    }

    // ==================== Event Publishing Integration Tests ====================

    @Test
    @DisplayName("Events are published correctly across chain execution")
    void testEventPublishingIntegration() throws InterruptedException {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Mined 10 stone");

        List<ActionStartedEvent> startedEvents = new ArrayList<>();
        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, startedEvents::add);
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        // Execute multiple actions
        for (int i = 0; i < 5; i++) {
            chain.executeBeforeAction(mockAction, mockContext);
            Thread.sleep(10);
            chain.executeAfterAction(mockAction, mockResult, mockContext);
        }

        assertEquals(5, startedEvents.size());
        assertEquals(5, completedEvents.size());

        // Verify durations
        for (ActionCompletedEvent event : completedEvents) {
            assertTrue(event.getDurationMs() >= 10, "Duration should be at least 10ms");
        }

        // Verify agent ID
        for (ActionStartedEvent event : startedEvents) {
            assertEquals(TEST_AGENT_ID, event.getAgentId());
        }
    }

    // ==================== Chain Management Tests ====================

    @Test
    @DisplayName("Chain can be dynamically modified")
    void testDynamicChainModification() {
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        // Execute with original chain
        chain.executeBeforeAction(mockAction, mockContext);
        chain.executeAfterAction(mockAction, mockResult, mockContext);

        MetricsInterceptor.MetricsSnapshot metrics1 = metricsInterceptor.getMetrics("mineblock");
        assertEquals(1, metrics1.totalExecutions());

        // Remove logging interceptor
        chain.removeInterceptor(loggingInterceptor);
        assertEquals(2, chain.size());

        // Execute again - metrics should still be collected
        chain.executeBeforeAction(mockAction, mockContext);
        chain.executeAfterAction(mockAction, mockResult, mockContext);

        // Metrics should still be collected
        MetricsInterceptor.MetricsSnapshot metrics2 = metricsInterceptor.getMetrics("mineblock");
        assertEquals(2, metrics2.totalExecutions());
    }

    @Test
    @DisplayName("Chain can be cleared and rebuilt")
    void testChainClearAndRebuild() {
        assertEquals(3, chain.size());

        chain.clear();
        assertEquals(0, chain.size());

        // Verify metrics are not collected when chain is empty
        doReturn((Class) MineBlockAction.class).when(mockAction).getClass();

        chain.executeBeforeAction(mockAction, mockContext);

        assertNull(metricsInterceptor.getMetrics("mineblock"));

        // Rebuild chain
        chain.addInterceptor(metricsInterceptor);
        chain.addInterceptor(loggingInterceptor);

        assertEquals(2, chain.size());

        // Now metrics should be collected
        chain.executeBeforeAction(mockAction, mockContext);

        assertNotNull(metricsInterceptor.getMetrics("mineblock"));
    }

    // ==================== Helper Classes ====================

    /**
     * Test interceptor that records its execution order.
     */
    private static class TestInterceptor implements ActionInterceptor {
        private final String name;
        private final int priority;
        private final List<String> executionOrder;

        public TestInterceptor(String name, int priority, List<String> executionOrder) {
            this.name = name;
            this.priority = priority;
            this.executionOrder = executionOrder;
        }

        @Override
        public boolean beforeAction(BaseAction action, ActionContext context) {
            executionOrder.add(name);
            return true;
        }

        @Override
        public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
            executionOrder.add(name);
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getName() {
            return name;
        }
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
