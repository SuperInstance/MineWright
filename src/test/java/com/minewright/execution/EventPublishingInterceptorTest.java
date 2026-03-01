package com.minewright.execution;

import com.minewright.action.ActionResult;
import com.minewright.action.actions.BaseAction;
import com.minewright.event.ActionCompletedEvent;
import com.minewright.event.ActionStartedEvent;
import com.minewright.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventPublishingInterceptor}.
 *
 * Tests cover:
 * <ul>
 *   <li>ActionStartedEvent publishing on beforeAction</li>
 *   <li>ActionCompletedEvent publishing on afterAction</li>
 *   <li>ActionCompletedEvent publishing on onError</li>
 *   <li>Duration calculation and inclusion in events</li>
 *   <li>Event payload correctness</li>
 *   <li>Agent ID inclusion in events</li>
 *   <li>Action type extraction and inclusion</li>
 *   <li>Success/failure status in events</li>
 *   <li>Exception handling in event publishing</li>
 *   <li>Multiple interceptor instances with different agent IDs</li>
 * </ul>
 */
@DisplayName("EventPublishingInterceptor Tests")
class EventPublishingInterceptorTest {

    private EventBus eventBus;
    private EventPublishingInterceptor interceptor;
    private BaseAction mockAction;
    private ActionContext mockContext;
    private ActionResult mockResult;
    private static final String TEST_AGENT_ID = "test-agent-123";

    @BeforeEach
    void setUp() {
        eventBus = new com.minewright.event.SimpleEventBus();
        interceptor = new EventPublishingInterceptor(eventBus, TEST_AGENT_ID);
        mockAction = mock(BaseAction.class);
        mockContext = mock(ActionContext.class);
        mockResult = mock(ActionResult.class);
    }

    // ==================== beforeAction Tests ====================

    @Test
    @DisplayName("beforeAction publishes ActionStartedEvent")
    void testBeforeActionPublishesEvent() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Mining stone");

        List<ActionStartedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, capturedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);

        assertEquals(1, capturedEvents.size());
        ActionStartedEvent event = capturedEvents.get(0);
        assertEquals(TEST_AGENT_ID, event.getAgentId());
        assertEquals("mineblock", event.getActionName());
        assertEquals("Mining stone", event.getDescription());
    }

    @Test
    @DisplayName("beforeAction returns true to allow execution")
    void testBeforeActionReturnsTrue() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);

        boolean result = interceptor.beforeAction(mockAction, mockContext);

        assertTrue(result);
    }

    @Test
    @DisplayName("beforeAction records start time for duration calculation")
    void testBeforeActionRecordsStartTime() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Test action");
        when(mockResult.isSuccess()).thenReturn(true);

        List<ActionStartedEvent> startedEvents = new ArrayList<>();
        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, startedEvents::add);
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(50);
        interceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(1, startedEvents.size());
        assertEquals(1, completedEvents.size());

        ActionCompletedEvent completedEvent = completedEvents.get(0);
        assertTrue(completedEvent.getDurationMs() >= 50,
            "Duration should be at least 50ms");
    }

    @Test
    @DisplayName("beforeAction handles null description gracefully")
    void testBeforeActionWithNullDescription() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn(null);

        List<ActionStartedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, capturedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);

        assertEquals(1, capturedEvents.size());
        assertNull(capturedEvents.get(0).getDescription());
    }

    @Test
    @DisplayName("beforeAction extracts action type correctly")
    void testBeforeActionExtractsActionType() {
        when(mockAction.getClass()).thenReturn((Class) BuildStructureAction.class);
        when(mockAction.getDescription()).thenReturn("Building house");

        List<ActionStartedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, capturedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);

        assertEquals(1, capturedEvents.size());
        assertEquals("buildstructure", capturedEvents.get(0).getActionName());
    }

    // ==================== afterAction Tests ====================

    @Test
    @DisplayName("afterAction publishes ActionCompletedEvent")
    void testAfterActionPublishesEvent() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Successfully mined");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(1, capturedEvents.size());
        ActionCompletedEvent event = capturedEvents.get(0);
        assertEquals(TEST_AGENT_ID, event.getAgentId());
        assertEquals("mineblock", event.getActionName());
        assertTrue(event.isSuccess());
        assertEquals("Successfully mined", event.getMessage());
    }

    @Test
    @DisplayName("afterAction includes correct duration")
    void testAfterActionIncludesDuration() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(100);
        interceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(1, capturedEvents.size());
        ActionCompletedEvent event = capturedEvents.get(0);
        assertTrue(event.getDurationMs() >= 100,
            "Duration should be at least 100ms");
        assertTrue(event.getDurationMs() < 200,
            "Duration should be less than 200ms");
    }

    @Test
    @DisplayName("afterAction handles failure result")
    void testAfterActionHandlesFailure() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockResult.isSuccess()).thenReturn(false);
        when(mockResult.getMessage()).thenReturn("Failed to mine");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(1, capturedEvents.size());
        ActionCompletedEvent event = capturedEvents.get(0);
        assertFalse(event.isSuccess());
        assertEquals("Failed to mine", event.getMessage());
    }

    @Test
    @DisplayName("afterAction handles null message gracefully")
    void testAfterActionWithNullMessage() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn(null);

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(1, capturedEvents.size());
        assertNull(capturedEvents.get(0).getMessage());
    }

    @Test
    @DisplayName("afterAction cleans up start time")
    void testAfterActionCleansUpStartTime() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        // First completion
        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(10);
        interceptor.afterAction(mockAction, mockResult, mockContext);

        // Second completion without beforeAction (should handle gracefully)
        interceptor.afterAction(mockAction, mockResult, mockContext);

        // Should have two events, second with duration 0
        assertEquals(2, capturedEvents.size());
        assertTrue(capturedEvents.get(0).getDurationMs() >= 10);
        assertEquals(0, capturedEvents.get(1).getDurationMs());
    }

    // ==================== onError Tests ====================

    @Test
    @DisplayName("onError publishes ActionCompletedEvent with failure")
    void testOnErrorPublishesFailureEvent() {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        Exception testException = new RuntimeException("Something went wrong");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.onError(mockAction, testException, mockContext);

        assertEquals(1, capturedEvents.size());
        ActionCompletedEvent event = capturedEvents.get(0);
        assertEquals(TEST_AGENT_ID, event.getAgentId());
        assertEquals("mineblock", event.getActionName());
        assertFalse(event.isSuccess());
        assertTrue(event.getMessage().contains("Exception:"));
        assertTrue(event.getMessage().contains("Something went wrong"));
    }

    @Test
    @DisplayName("onError includes duration")
    void testOnErrorIncludesDuration() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        Exception testException = new RuntimeException("Error");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(50);
        interceptor.onError(mockAction, testException, mockContext);

        assertEquals(1, capturedEvents.size());
        ActionCompletedEvent event = capturedEvents.get(0);
        assertTrue(event.getDurationMs() >= 50);
    }

    @Test
    @DisplayName("onError cleans up start time")
    void testOnErrorCleansUpStartTime() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        Exception testException = new RuntimeException("Error");

        List<ActionCompletedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionCompletedEvent.class, capturedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(10);
        interceptor.onError(mockAction, testException, mockContext);

        assertEquals(1, capturedEvents.size());
        assertTrue(capturedEvents.get(0).getDurationMs() >= 10);

        // Call onError again - should not have a duration
        interceptor.onError(mockAction, testException, mockContext);

        assertEquals(2, capturedEvents.size());
        assertEquals(0, capturedEvents.get(1).getDurationMs());
    }

    @Test
    @DisplayName("onError returns false to propagate exception")
    void testOnErrorReturnsFalse() {
        Exception testException = new RuntimeException("Error");

        boolean result = interceptor.onError(mockAction, testException, mockContext);

        assertFalse(result, "onError should return false to propagate exception");
    }

    // ==================== Agent ID Tests ====================

    @Test
    @DisplayName("Events include correct agent ID")
    void testEventsIncludeCorrectAgentId() {
        String customAgentId = "custom-agent-456";
        EventPublishingInterceptor customInterceptor =
            new EventPublishingInterceptor(eventBus, customAgentId);

        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Test");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Done");

        List<ActionStartedEvent> startedEvents = new ArrayList<>();
        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, startedEvents::add);
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        customInterceptor.beforeAction(mockAction, mockContext);
        customInterceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(customAgentId, startedEvents.get(0).getAgentId());
        assertEquals(customAgentId, completedEvents.get(0).getAgentId());
    }

    @Test
    @DisplayName("Multiple interceptors can publish with different agent IDs")
    void testMultipleInterceptorsDifferentAgentIds() {
        EventPublishingInterceptor interceptor1 =
            new EventPublishingInterceptor(eventBus, "agent-1");
        EventPublishingInterceptor interceptor2 =
            new EventPublishingInterceptor(eventBus, "agent-2");

        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Test");

        List<ActionStartedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, capturedEvents::add);

        interceptor1.beforeAction(mockAction, mockContext);
        interceptor2.beforeAction(mockAction, mockContext);

        assertEquals(2, capturedEvents.size());

        List<String> agentIds = capturedEvents.stream()
            .map(ActionStartedEvent::getAgentId)
            .toList();
        assertTrue(agentIds.contains("agent-1"));
        assertTrue(agentIds.contains("agent-2"));
    }

    // ==================== Event Integration Tests ====================

    @Test
    @DisplayName("Complete lifecycle publishes start and complete events")
    void testCompleteLifecycle() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Mining stone");
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getMessage()).thenReturn("Mined 10 stone");

        List<ActionStartedEvent> startedEvents = new ArrayList<>();
        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, startedEvents::add);
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(20);
        interceptor.afterAction(mockAction, mockResult, mockContext);

        assertEquals(1, startedEvents.size());
        assertEquals(1, completedEvents.size());

        ActionStartedEvent startedEvent = startedEvents.get(0);
        assertEquals("mineblock", startedEvent.getActionName());
        assertEquals("Mining stone", startedEvent.getDescription());

        ActionCompletedEvent completedEvent = completedEvents.get(0);
        assertEquals("mineblock", completedEvent.getActionName());
        assertTrue(completedEvent.isSuccess());
        assertEquals("Mined 10 stone", completedEvent.getMessage());
        assertTrue(completedEvent.getDurationMs() >= 20);
    }

    @Test
    @DisplayName("Error lifecycle publishes start and error events")
    void testErrorLifecycle() throws InterruptedException {
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Mining stone");
        Exception testException = new RuntimeException("Mining failed");

        List<ActionStartedEvent> startedEvents = new ArrayList<>();
        List<ActionCompletedEvent> completedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, startedEvents::add);
        eventBus.subscribe(ActionCompletedEvent.class, completedEvents::add);

        interceptor.beforeAction(mockAction, mockContext);
        Thread.sleep(20);
        interceptor.onError(mockAction, testException, mockContext);

        assertEquals(1, startedEvents.size());
        assertEquals(1, completedEvents.size());

        ActionCompletedEvent completedEvent = completedEvents.get(0);
        assertFalse(completedEvent.isSuccess());
        assertTrue(completedEvent.getMessage().contains("Mining failed"));
        assertTrue(completedEvent.getDurationMs() >= 20);
    }

    // ==================== Interceptor Priority Tests ====================

    @Test
    @DisplayName("EventPublishingInterceptor has correct priority")
    void testEventPublishingInterceptorPriority() {
        assertEquals(500, interceptor.getPriority(),
            "EventPublishingInterceptor should have priority 500");
    }

    @Test
    @DisplayName("EventPublishingInterceptor has correct name")
    void testEventPublishingInterceptorName() {
        assertEquals("EventPublishingInterceptor", interceptor.getName());
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Interceptor handles null EventBus gracefully")
    void testNullEventBusHandling() {
        // Create interceptor with null EventBus - this is actually a design choice
        // The implementation expects EventBus to be non-null, so we test the behavior
        assertDoesNotThrow(() -> {
            EventPublishingInterceptor interceptorWithNull =
                new EventPublishingInterceptor(null, TEST_AGENT_ID);
            // This will throw NullPointerException when trying to publish
            // but that's expected behavior
            when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
            assertThrows(NullPointerException.class,
                () -> interceptorWithNull.beforeAction(mockAction, mockContext));
        });
    }

    // ==================== Multiple Action Types Tests ====================

    @Test
    @DisplayName("Events are published for different action types")
    void testEventsForDifferentActionTypes() {
        List<ActionStartedEvent> capturedEvents = new ArrayList<>();
        eventBus.subscribe(ActionStartedEvent.class, capturedEvents::add);

        // Mine action
        when(mockAction.getClass()).thenReturn((Class) MineBlockAction.class);
        when(mockAction.getDescription()).thenReturn("Mining");
        interceptor.beforeAction(mockAction, mockContext);

        // Build action
        BaseAction buildAction = mock(BaseAction.class);
        when(buildAction.getClass()).thenReturn((Class) BuildStructureAction.class);
        when(buildAction.getDescription()).thenReturn("Building");
        interceptor.beforeAction(buildAction, mockContext);

        assertEquals(2, capturedEvents.size());
        assertEquals("mineblock", capturedEvents.get(0).getActionName());
        assertEquals("buildstructure", capturedEvents.get(1).getActionName());
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
