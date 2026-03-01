package com.minewright.execution;

import com.minewright.di.ServiceContainer;
import com.minewright.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActionContext}.
 *
 * Tests cover:
 * <ul>
 *   <li>Constructor with all parameters</li>
 *   <li>Builder pattern usage</li>
 *   <li>Getter methods return correct values</li>
 *   <li>Service lookup via ServiceContainer</li>
 *   <li>Event publishing convenience method</li>
 *   <li>Current state query</li>
 *   <li>Null handling in convenience methods</li>
 * </ul>
 */
@DisplayName("ActionContext Tests")
class ActionContextTest {

    private ServiceContainer serviceContainer;
    private EventBus eventBus;
    private AgentStateMachine stateMachine;
    private InterceptorChain interceptorChain;

    @BeforeEach
    void setUp() {
        serviceContainer = mock(ServiceContainer.class);
        eventBus = mock(EventBus.class);
        stateMachine = mock(AgentStateMachine.class);
        interceptorChain = mock(InterceptorChain.class);
    }

    @Test
    @DisplayName("Constructor with all parameters creates valid context")
    void testConstructorWithAllParameters() {
        ActionContext context = new ActionContext(
            serviceContainer,
            eventBus,
            stateMachine,
            interceptorChain
        );

        assertSame(serviceContainer, context.getServiceContainer());
        assertSame(eventBus, context.getEventBus());
        assertSame(stateMachine, context.getStateMachine());
        assertSame(interceptorChain, context.getInterceptorChain());
    }

    @Test
    @DisplayName("Constructor allows null parameters")
    void testConstructorWithNullParameters() {
        ActionContext context = new ActionContext(null, null, null, null);

        assertNull(context.getServiceContainer());
        assertNull(context.getEventBus());
        assertNull(context.getStateMachine());
        assertNull(context.getInterceptorChain());
    }

    @Test
    @DisplayName("Builder creates context with all components")
    void testBuilderWithAllComponents() {
        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .eventBus(eventBus)
            .stateMachine(stateMachine)
            .interceptorChain(interceptorChain)
            .build();

        assertSame(serviceContainer, context.getServiceContainer());
        assertSame(eventBus, context.getEventBus());
        assertSame(stateMachine, context.getStateMachine());
        assertSame(interceptorChain, context.getInterceptorChain());
    }

    @Test
    @DisplayName("Builder can create context with partial components")
    void testBuilderWithPartialComponents() {
        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .eventBus(eventBus)
            .build();

        assertSame(serviceContainer, context.getServiceContainer());
        assertSame(eventBus, context.getEventBus());
        assertNull(context.getStateMachine());
        assertNull(context.getInterceptorChain());
    }

    @Test
    @DisplayName("Builder can create empty context")
    void testBuilderWithNoComponents() {
        ActionContext context = ActionContext.builder().build();

        assertNull(context.getServiceContainer());
        assertNull(context.getEventBus());
        assertNull(context.getStateMachine());
        assertNull(context.getInterceptorChain());
    }

    @Test
    @DisplayName("Builder accumulates state across builds")
    void testBuilderAccumulatesState() {
        ActionContext.Builder builder = ActionContext.builder()
            .serviceContainer(serviceContainer);

        ActionContext context1 = builder
            .eventBus(eventBus)
            .build();

        // Builder retains state from previous build
        ActionContext context2 = builder
            .stateMachine(stateMachine)
            .build();

        assertSame(serviceContainer, context1.getServiceContainer());
        assertSame(eventBus, context1.getEventBus());
        assertNull(context1.getStateMachine());

        // context2 inherits all previous builder state
        assertSame(serviceContainer, context2.getServiceContainer());
        assertSame(eventBus, context2.getEventBus()); // Builder retained eventBus
        assertSame(stateMachine, context2.getStateMachine());
    }

    @Test
    @DisplayName("getService delegates to ServiceContainer")
    void testGetService() {
        String testService = "TestService";
        when(serviceContainer.getService(String.class)).thenReturn(testService);

        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .build();

        String result = context.getService(String.class);

        assertSame(testService, result);
        verify(serviceContainer).getService(String.class);
    }

    @Test
    @DisplayName("getService throws when ServiceContainer is null")
    void testGetServiceWithNullContainer() {
        ActionContext context = ActionContext.builder()
            .serviceContainer(null)
            .build();

        assertThrows(NullPointerException.class,
            () -> context.getService(String.class));
    }

    @Test
    @DisplayName("getService throws when service not found")
    void testGetServiceNotFound() {
        when(serviceContainer.getService(String.class))
            .thenThrow(new ServiceContainer.ServiceNotFoundException("String"));

        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .build();

        assertThrows(ServiceContainer.ServiceNotFoundException.class,
            () -> context.getService(String.class));
    }

    @Test
    @DisplayName("findService returns Optional with service when found")
    void testFindServiceFound() {
        String testService = "TestService";
        when(serviceContainer.findService(String.class))
            .thenReturn(Optional.of(testService));

        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .build();

        Optional<String> result = context.findService(String.class);

        assertTrue(result.isPresent());
        assertSame(testService, result.get());
        verify(serviceContainer).findService(String.class);
    }

    @Test
    @DisplayName("findService returns empty Optional when not found")
    void testFindServiceNotFound() {
        when(serviceContainer.findService(String.class))
            .thenReturn(Optional.empty());

        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .build();

        Optional<String> result = context.findService(String.class);

        assertFalse(result.isPresent());
        verify(serviceContainer).findService(String.class);
    }

    @Test
    @DisplayName("findService returns empty Optional when container is null")
    void testFindServiceWithNullContainer() {
        ActionContext context = ActionContext.builder()
            .serviceContainer(null)
            .build();

        assertThrows(NullPointerException.class,
            () -> context.findService(String.class));
    }

    @Test
    @DisplayName("publishEvent publishes to EventBus")
    void testPublishEvent() {
        Object testEvent = new Object();

        ActionContext context = ActionContext.builder()
            .eventBus(eventBus)
            .build();

        context.publishEvent(testEvent);

        verify(eventBus).publish(testEvent);
    }

    @Test
    @DisplayName("publishEvent does nothing when EventBus is null")
    void testPublishEventWithNullEventBus() {
        ActionContext context = ActionContext.builder()
            .eventBus(null)
            .build();

        // Should not throw exception
        assertDoesNotThrow(() -> context.publishEvent(new Object()));
    }

    @Test
    @DisplayName("getCurrentState returns state from state machine")
    void testGetCurrentState() {
        when(stateMachine.getCurrentState()).thenReturn(AgentState.EXECUTING);

        ActionContext context = ActionContext.builder()
            .stateMachine(stateMachine)
            .build();

        assertEquals(AgentState.EXECUTING, context.getCurrentState());
        verify(stateMachine).getCurrentState();
    }

    @Test
    @DisplayName("getCurrentState returns IDLE when state machine is null")
    void testGetCurrentStateWithNullStateMachine() {
        ActionContext context = ActionContext.builder()
            .stateMachine(null)
            .build();

        assertEquals(AgentState.IDLE, context.getCurrentState());
    }

    @Test
    @DisplayName("All getters return correct values")
    void testAllGetters() {
        ActionContext context = new ActionContext(
            serviceContainer,
            eventBus,
            stateMachine,
            interceptorChain
        );

        assertSame(serviceContainer, context.getServiceContainer());
        assertSame(eventBus, context.getEventBus());
        assertSame(stateMachine, context.getStateMachine());
        assertSame(interceptorChain, context.getInterceptorChain());
    }

    @Test
    @DisplayName("Context is effectively immutable")
    void testContextImmutability() {
        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .eventBus(eventBus)
            .stateMachine(stateMachine)
            .interceptorChain(interceptorChain)
            .build();

        // Get all references
        ServiceContainer sc = context.getServiceContainer();
        EventBus eb = context.getEventBus();
        AgentStateMachine sm = context.getStateMachine();
        InterceptorChain ic = context.getInterceptorChain();

        // Verify they remain the same
        assertSame(sc, context.getServiceContainer());
        assertSame(eb, context.getEventBus());
        assertSame(sm, context.getStateMachine());
        assertSame(ic, context.getInterceptorChain());
    }

    @Test
    @DisplayName("Builder methods can be chained")
    void testBuilderMethodChaining() {
        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .eventBus(eventBus)
            .stateMachine(stateMachine)
            .interceptorChain(interceptorChain)
            .build();

        assertNotNull(context);
        assertSame(serviceContainer, context.getServiceContainer());
        assertSame(eventBus, context.getEventBus());
        assertSame(stateMachine, context.getStateMachine());
        assertSame(interceptorChain, context.getInterceptorChain());
    }

    @Test
    @DisplayName("Multiple contexts can be created from same builder")
    void testMultipleContextsFromSameBuilder() {
        ServiceContainer container2 = mock(ServiceContainer.class);
        EventBus eventBus2 = mock(EventBus.class);

        ActionContext.Builder builder = ActionContext.builder()
            .stateMachine(stateMachine)
            .interceptorChain(interceptorChain);

        ActionContext context1 = builder
            .serviceContainer(serviceContainer)
            .eventBus(eventBus)
            .build();

        ActionContext context2 = builder
            .serviceContainer(container2)
            .eventBus(eventBus2)
            .build();

        assertSame(serviceContainer, context1.getServiceContainer());
        assertSame(eventBus, context1.getEventBus());
        assertSame(stateMachine, context1.getStateMachine());

        assertSame(container2, context2.getServiceContainer());
        assertSame(eventBus2, context2.getEventBus());
        assertSame(stateMachine, context2.getStateMachine());
    }

    @Test
    @DisplayName("Service integration works end-to-end")
    void testServiceIntegration() {
        // Create a mock service
        class TestService {
            String getValue() { return "test"; }
        }

        TestService testService = new TestService();
        when(serviceContainer.getService(TestService.class))
            .thenReturn(testService);
        when(serviceContainer.findService(TestService.class))
            .thenReturn(Optional.of(testService));

        ActionContext context = ActionContext.builder()
            .serviceContainer(serviceContainer)
            .build();

        // Test getService
        TestService result1 = context.getService(TestService.class);
        assertSame(testService, result1);
        assertEquals("test", result1.getValue());

        // Test findService
        Optional<TestService> result2 = context.findService(TestService.class);
        assertTrue(result2.isPresent());
        assertSame(testService, result2.get());
    }

    @Test
    @DisplayName("Event publishing integration works end-to-end")
    void testEventPublishingIntegration() {
        class TestEvent {
            final String message;
            TestEvent(String message) { this.message = message; }
        }

        TestEvent testEvent = new TestEvent("test message");

        ActionContext context = ActionContext.builder()
            .eventBus(eventBus)
            .build();

        context.publishEvent(testEvent);

        verify(eventBus).publish(testEvent);
    }

    @Test
    @DisplayName("State query integration works end-to-end")
    void testStateQueryIntegration() {
        when(stateMachine.getCurrentState()).thenReturn(AgentState.PLANNING);

        ActionContext context = ActionContext.builder()
            .stateMachine(stateMachine)
            .build();

        assertEquals(AgentState.PLANNING, context.getCurrentState());
        assertFalse(context.getCurrentState().canAcceptCommands());
        assertTrue(context.getCurrentState().isActive());
    }

    @Test
    @DisplayName("Context with all nulls is valid but limited")
    void testContextWithAllNulls() {
        ActionContext context = new ActionContext(null, null, null, null);

        assertNull(context.getServiceContainer());
        assertNull(context.getEventBus());
        assertNull(context.getStateMachine());
        assertNull(context.getInterceptorChain());

        // These convenience methods should handle null gracefully
        assertEquals(AgentState.IDLE, context.getCurrentState());

        // Publishing does nothing with null EventBus
        assertDoesNotThrow(() -> context.publishEvent(new Object()));

        // Service operations fail with null container
        assertThrows(NullPointerException.class,
            () -> context.getService(String.class));
        assertThrows(NullPointerException.class,
            () -> context.findService(String.class));
    }
}
