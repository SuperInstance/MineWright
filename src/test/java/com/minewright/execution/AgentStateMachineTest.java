package com.minewright.execution;

import com.minewright.event.EventBus;
import com.minewright.event.StateTransitionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AgentStateMachine}.
 *
 * Tests cover:
 * <ul>
 *   <li>State initialization and getters</li>
 *   <li>Valid state transitions</li>
 *   <li>Invalid state transitions</li>
 *   <li>Event publishing on transitions</li>
 *   <li>Thread safety of state transitions</li>
 *   <li>Forced transitions (bypassing validation)</li>
 *   <li>State machine reset</li>
 *   <li>Utility methods (canAcceptCommands, isActive)</li>
 *   <li>Valid transitions query</li>
 * </ul>
 */
@DisplayName("AgentStateMachine Tests")
class AgentStateMachineTest {

    private EventBus eventBus;
    private AgentStateMachine stateMachine;
    private static final String AGENT_ID = "test-agent";

    @BeforeEach
    void setUp() {
        eventBus = mock(EventBus.class);
        stateMachine = new AgentStateMachine(eventBus, AGENT_ID);
    }

    @Test
    @DisplayName("Initial state is IDLE")
    void testInitialState() {
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState(),
            "State machine should start in IDLE state");
    }

    @Test
    @DisplayName("Constructor with null event bus works")
    void testConstructorWithNullEventBus() {
        AgentStateMachine sm = new AgentStateMachine(null, AGENT_ID);
        assertEquals(AgentState.IDLE, sm.getCurrentState());
        assertEquals(AGENT_ID, sm.getAgentId());
    }

    @Test
    @DisplayName("Constructor with default agent ID")
    void testConstructorWithDefaultAgentId() {
        AgentStateMachine sm = new AgentStateMachine(eventBus);
        assertEquals("default", sm.getAgentId());
    }

    @Test
    @DisplayName("Valid transition: IDLE to PLANNING")
    void testValidIdleToPlanning() {
        boolean result = stateMachine.transitionTo(AgentState.PLANNING);

        assertTrue(result, "Transition from IDLE to PLANNING should succeed");
        assertEquals(AgentState.PLANNING, stateMachine.getCurrentState());

        verify(eventBus).publish(any(StateTransitionEvent.class));
    }

    @Test
    @DisplayName("Valid transition: PLANNING to EXECUTING")
    void testValidPlanningToExecuting() {
        stateMachine.transitionTo(AgentState.PLANNING);
        boolean result = stateMachine.transitionTo(AgentState.EXECUTING);

        assertTrue(result, "Transition from PLANNING to EXECUTING should succeed");
        assertEquals(AgentState.EXECUTING, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: PLANNING to FAILED")
    void testValidPlanningToFailed() {
        stateMachine.transitionTo(AgentState.PLANNING);
        boolean result = stateMachine.transitionTo(AgentState.FAILED);

        assertTrue(result, "Transition from PLANNING to FAILED should succeed");
        assertEquals(AgentState.FAILED, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: PLANNING to IDLE")
    void testValidPlanningToIdle() {
        stateMachine.transitionTo(AgentState.PLANNING);
        boolean result = stateMachine.transitionTo(AgentState.IDLE);

        assertTrue(result, "Transition from PLANNING to IDLE should succeed");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: EXECUTING to COMPLETED")
    void testValidExecutingToCompleted() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        boolean result = stateMachine.transitionTo(AgentState.COMPLETED);

        assertTrue(result, "Transition from EXECUTING to COMPLETED should succeed");
        assertEquals(AgentState.COMPLETED, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: EXECUTING to FAILED")
    void testValidExecutingToFailed() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        boolean result = stateMachine.transitionTo(AgentState.FAILED);

        assertTrue(result, "Transition from EXECUTING to FAILED should succeed");
        assertEquals(AgentState.FAILED, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: EXECUTING to PAUSED")
    void testValidExecutingToPaused() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        boolean result = stateMachine.transitionTo(AgentState.PAUSED);

        assertTrue(result, "Transition from EXECUTING to PAUSED should succeed");
        assertEquals(AgentState.PAUSED, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: PAUSED to EXECUTING (resume)")
    void testValidPausedToExecuting() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.PAUSED);
        boolean result = stateMachine.transitionTo(AgentState.EXECUTING);

        assertTrue(result, "Transition from PAUSED to EXECUTING (resume) should succeed");
        assertEquals(AgentState.EXECUTING, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: PAUSED to IDLE (cancel)")
    void testValidPausedToIdle() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.PAUSED);
        boolean result = stateMachine.transitionTo(AgentState.IDLE);

        assertTrue(result, "Transition from PAUSED to IDLE (cancel) should succeed");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: COMPLETED to IDLE")
    void testValidCompletedToIdle() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.COMPLETED);
        boolean result = stateMachine.transitionTo(AgentState.IDLE);

        assertTrue(result, "Transition from COMPLETED to IDLE should succeed");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Valid transition: FAILED to IDLE")
    void testValidFailedToIdle() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.FAILED);
        boolean result = stateMachine.transitionTo(AgentState.IDLE);

        assertTrue(result, "Transition from FAILED to IDLE should succeed");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Invalid transition: IDLE to EXECUTING (skips PLANNING)")
    void testInvalidIdleToExecuting() {
        boolean result = stateMachine.transitionTo(AgentState.EXECUTING);

        assertFalse(result, "Direct transition from IDLE to EXECUTING should fail");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState(),
            "State should remain IDLE after invalid transition");

        verify(eventBus, never()).publish(any(StateTransitionEvent.class));
    }

    @Test
    @DisplayName("Invalid transition: EXECUTING to PLANNING")
    void testInvalidExecutingToPlanning() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        boolean result = stateMachine.transitionTo(AgentState.PLANNING);

        assertFalse(result, "Transition from EXECUTING to PLANNING should fail");
        assertEquals(AgentState.EXECUTING, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Invalid transition: COMPLETED to EXECUTING")
    void testInvalidCompletedToExecuting() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.COMPLETED);
        boolean result = stateMachine.transitionTo(AgentState.EXECUTING);

        assertFalse(result, "Transition from COMPLETED to EXECUTING should fail");
        assertEquals(AgentState.COMPLETED, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Invalid transition: null state")
    void testInvalidNullTransition() {
        boolean result = stateMachine.transitionTo(null);

        assertFalse(result, "Transition to null state should fail");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("canTransitionTo returns true for valid transitions")
    void testCanTransitionToValid() {
        stateMachine.transitionTo(AgentState.PLANNING);

        assertTrue(stateMachine.canTransitionTo(AgentState.EXECUTING),
            "Should be able to transition from PLANNING to EXECUTING");
        assertTrue(stateMachine.canTransitionTo(AgentState.FAILED),
            "Should be able to transition from PLANNING to FAILED");
        assertTrue(stateMachine.canTransitionTo(AgentState.IDLE),
            "Should be able to transition from PLANNING to IDLE");
    }

    @Test
    @DisplayName("canTransitionTo returns false for invalid transitions")
    void testCanTransitionToInvalid() {
        assertFalse(stateMachine.canTransitionTo(AgentState.EXECUTING),
            "Should not be able to transition from IDLE to EXECUTING");
        assertFalse(stateMachine.canTransitionTo(null),
            "Should not be able to transition to null state");
    }

    @Test
    @DisplayName("Event is published with correct parameters on transition")
    void testEventPublishedOnTransition() {
        stateMachine.transitionTo(AgentState.PLANNING, "test reason");

        ArgumentCaptor<StateTransitionEvent> eventCaptor = ArgumentCaptor.forClass(StateTransitionEvent.class);
        verify(eventBus).publish(eventCaptor.capture());

        StateTransitionEvent event = eventCaptor.getValue();
        assertEquals(AGENT_ID, event.getAgentId());
        assertEquals(AgentState.IDLE, event.getFromState());
        assertEquals(AgentState.PLANNING, event.getToState());
        assertEquals("test reason", event.getReason());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Event is not published on failed transition")
    void testEventNotPublishedOnFailedTransition() {
        stateMachine.transitionTo(AgentState.EXECUTING); // Invalid transition

        verify(eventBus, never()).publish(any(StateTransitionEvent.class));
    }

    @Test
    @DisplayName("Event is not published when event bus is null")
    void testNullEventBusNoPublish() {
        AgentStateMachine sm = new AgentStateMachine(null, AGENT_ID);
        sm.transitionTo(AgentState.PLANNING);

        // Should not throw exception
        assertEquals(AgentState.PLANNING, sm.getCurrentState());
    }

    @Test
    @DisplayName("Force transition bypasses validation")
    void testForceTransition() {
        // This would normally be invalid
        stateMachine.forceTransition(AgentState.EXECUTING, "emergency override");

        assertEquals(AgentState.EXECUTING, stateMachine.getCurrentState());

        ArgumentCaptor<StateTransitionEvent> eventCaptor = ArgumentCaptor.forClass(StateTransitionEvent.class);
        verify(eventBus).publish(eventCaptor.capture());

        StateTransitionEvent event = eventCaptor.getValue();
        assertEquals(AgentState.IDLE, event.getFromState());
        assertEquals(AgentState.EXECUTING, event.getToState());
        assertTrue(event.getReason().contains("FORCED"));
    }

    @Test
    @DisplayName("Force transition to null does nothing")
    void testForceTransitionToNull() {
        stateMachine.forceTransition(null, "test");
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Reset sets state to IDLE")
    void testResetFromPlanning() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.reset();

        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());

        ArgumentCaptor<StateTransitionEvent> eventCaptor = ArgumentCaptor.forClass(StateTransitionEvent.class);
        verify(eventBus, times(2)).publish(eventCaptor.capture()); // Once for PLANNING, once for reset
    }

    @Test
    @DisplayName("Reset from IDLE does not publish event")
    void testResetFromIdle() {
        stateMachine.reset();

        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
        verify(eventBus, never()).publish(any(StateTransitionEvent.class));
    }

    @Test
    @DisplayName("Reset from FAILED publishes event")
    void testResetFromFailed() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.FAILED);
        reset(eventBus);

        stateMachine.reset();

        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
        verify(eventBus).publish(any(StateTransitionEvent.class));
    }

    @Test
    @DisplayName("canAcceptCommands returns true for IDLE")
    void testCanAcceptCommandsIdle() {
        assertTrue(stateMachine.canAcceptCommands(),
            "IDLE state should accept commands");
    }

    @Test
    @DisplayName("canAcceptCommands returns true for COMPLETED")
    void testCanAcceptCommandsCompleted() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.COMPLETED);

        assertTrue(stateMachine.canAcceptCommands(),
            "COMPLETED state should accept commands");
    }

    @Test
    @DisplayName("canAcceptCommands returns true for FAILED")
    void testCanAcceptCommandsFailed() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.FAILED);

        assertTrue(stateMachine.canAcceptCommands(),
            "FAILED state should accept commands");
    }

    @Test
    @DisplayName("canAcceptCommands returns false for PLANNING")
    void testCanAcceptCommandsPlanning() {
        stateMachine.transitionTo(AgentState.PLANNING);

        assertFalse(stateMachine.canAcceptCommands(),
            "PLANNING state should not accept new commands");
    }

    @Test
    @DisplayName("canAcceptCommands returns false for EXECUTING")
    void testCanAcceptCommandsExecuting() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);

        assertFalse(stateMachine.canAcceptCommands(),
            "EXECUTING state should not accept new commands");
    }

    @Test
    @DisplayName("canAcceptCommands returns false for PAUSED")
    void testCanAcceptCommandsPaused() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.PAUSED);

        assertFalse(stateMachine.canAcceptCommands(),
            "PAUSED state should not accept new commands");
    }

    @Test
    @DisplayName("isActive returns true for PLANNING")
    void testIsActivePlanning() {
        stateMachine.transitionTo(AgentState.PLANNING);

        assertTrue(stateMachine.isActive(),
            "PLANNING state should be active");
    }

    @Test
    @DisplayName("isActive returns true for EXECUTING")
    void testIsActiveExecuting() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);

        assertTrue(stateMachine.isActive(),
            "EXECUTING state should be active");
    }

    @Test
    @DisplayName("isActive returns false for IDLE")
    void testIsActiveIdle() {
        assertFalse(stateMachine.isActive(),
            "IDLE state should not be active");
    }

    @Test
    @DisplayName("isActive returns false for PAUSED")
    void testIsActivePaused() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.PAUSED);

        assertFalse(stateMachine.isActive(),
            "PAUSED state should not be active");
    }

    @Test
    @DisplayName("isActive returns false for COMPLETED")
    void testIsActiveCompleted() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.COMPLETED);

        assertFalse(stateMachine.isActive(),
            "COMPLETED state should not be active");
    }

    @Test
    @DisplayName("isActive returns false for FAILED")
    void testIsActiveFailed() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.FAILED);

        assertFalse(stateMachine.isActive(),
            "FAILED state should not be active");
    }

    @Test
    @DisplayName("getValidTransitions returns correct transitions for IDLE")
    void testGetValidTransitionsIdle() {
        Set<AgentState> validTransitions = stateMachine.getValidTransitions();

        assertEquals(1, validTransitions.size());
        assertTrue(validTransitions.contains(AgentState.PLANNING));
    }

    @Test
    @DisplayName("getValidTransitions returns correct transitions for PLANNING")
    void testGetValidTransitionsPlanning() {
        stateMachine.transitionTo(AgentState.PLANNING);
        Set<AgentState> validTransitions = stateMachine.getValidTransitions();

        assertEquals(3, validTransitions.size());
        assertTrue(validTransitions.contains(AgentState.EXECUTING));
        assertTrue(validTransitions.contains(AgentState.FAILED));
        assertTrue(validTransitions.contains(AgentState.IDLE));
    }

    @Test
    @DisplayName("getValidTransitions returns correct transitions for EXECUTING")
    void testGetValidTransitionsExecuting() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        Set<AgentState> validTransitions = stateMachine.getValidTransitions();

        assertEquals(3, validTransitions.size());
        assertTrue(validTransitions.contains(AgentState.COMPLETED));
        assertTrue(validTransitions.contains(AgentState.FAILED));
        assertTrue(validTransitions.contains(AgentState.PAUSED));
    }

    @Test
    @DisplayName("getValidTransitions returns correct transitions for PAUSED")
    void testGetValidTransitionsPaused() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.PAUSED);
        Set<AgentState> validTransitions = stateMachine.getValidTransitions();

        assertEquals(2, validTransitions.size());
        assertTrue(validTransitions.contains(AgentState.EXECUTING));
        assertTrue(validTransitions.contains(AgentState.IDLE));
    }

    @Test
    @DisplayName("getValidTransitions returns correct transitions for COMPLETED")
    void testGetValidTransitionsCompleted() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.EXECUTING);
        stateMachine.transitionTo(AgentState.COMPLETED);
        Set<AgentState> validTransitions = stateMachine.getValidTransitions();

        assertEquals(1, validTransitions.size());
        assertTrue(validTransitions.contains(AgentState.IDLE));
    }

    @Test
    @DisplayName("getValidTransitions returns correct transitions for FAILED")
    void testGetValidTransitionsFailed() {
        stateMachine.transitionTo(AgentState.PLANNING);
        stateMachine.transitionTo(AgentState.FAILED);
        Set<AgentState> validTransitions = stateMachine.getValidTransitions();

        assertEquals(1, validTransitions.size());
        assertTrue(validTransitions.contains(AgentState.IDLE));
    }

    @Test
    @DisplayName("getAgentId returns the agent ID")
    void testGetAgentId() {
        assertEquals(AGENT_ID, stateMachine.getAgentId());
    }

    @Test
    @DisplayName("Thread safety: concurrent transitions")
    void testConcurrentTransitions() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Start from IDLE, only one thread should succeed in transitioning to PLANNING
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    if (stateMachine.transitionTo(AgentState.PLANNING)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(1, successCount.get(),
            "Only one transition should succeed from concurrent attempts");
        assertEquals(AgentState.PLANNING, stateMachine.getCurrentState(),
            "State should be PLANNING after successful transition");
    }

    @Test
    @DisplayName("Full workflow: IDLE -> PLANNING -> EXECUTING -> COMPLETED -> IDLE")
    void testFullWorkflow() {
        assertTrue(stateMachine.transitionTo(AgentState.PLANNING));
        assertEquals(AgentState.PLANNING, stateMachine.getCurrentState());

        assertTrue(stateMachine.transitionTo(AgentState.EXECUTING));
        assertEquals(AgentState.EXECUTING, stateMachine.getCurrentState());

        assertTrue(stateMachine.transitionTo(AgentState.COMPLETED));
        assertEquals(AgentState.COMPLETED, stateMachine.getCurrentState());

        assertTrue(stateMachine.transitionTo(AgentState.IDLE));
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Full workflow with pause: IDLE -> PLANNING -> EXECUTING -> PAUSED -> EXECUTING -> COMPLETED")
    void testFullWorkflowWithPause() {
        assertTrue(stateMachine.transitionTo(AgentState.PLANNING));
        assertTrue(stateMachine.transitionTo(AgentState.EXECUTING));
        assertTrue(stateMachine.transitionTo(AgentState.PAUSED));
        assertEquals(AgentState.PAUSED, stateMachine.getCurrentState());

        assertTrue(stateMachine.transitionTo(AgentState.EXECUTING));
        assertEquals(AgentState.EXECUTING, stateMachine.getCurrentState());

        assertTrue(stateMachine.transitionTo(AgentState.COMPLETED));
        assertEquals(AgentState.COMPLETED, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Full workflow with failure: IDLE -> PLANNING -> FAILED -> IDLE")
    void testFullWorkflowWithFailure() {
        assertTrue(stateMachine.transitionTo(AgentState.PLANNING));
        assertTrue(stateMachine.transitionTo(AgentState.FAILED));
        assertEquals(AgentState.FAILED, stateMachine.getCurrentState());

        assertTrue(stateMachine.transitionTo(AgentState.IDLE));
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Transition with null reason is handled")
    void testTransitionWithNullReason() {
        assertTrue(stateMachine.transitionTo(AgentState.PLANNING, null));

        ArgumentCaptor<StateTransitionEvent> eventCaptor = ArgumentCaptor.forClass(StateTransitionEvent.class);
        verify(eventBus).publish(eventCaptor.capture());

        assertNull(eventCaptor.getValue().getReason());
    }

    // ==================== Enhanced Concurrent Access Tests ====================

    @Test
    @DisplayName("Thread safety: rapid concurrent transitions from multiple threads")
    void testRapidConcurrentTransitions() throws InterruptedException {
        int threadCount = 20;
        int iterationsPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterationsPerThread; j++) {
                        // Try to transition from current state to next valid state
                        AgentState current = stateMachine.getCurrentState();
                        AgentState target = getNextValidState(current);
                        if (target != null && stateMachine.transitionTo(target)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");

        // Verify state machine is still in a valid state
        AgentState finalState = stateMachine.getCurrentState();
        assertNotNull(finalState, "Final state should not be null");

        // All transitions should either succeed or fail cleanly
        assertEquals(threadCount * iterationsPerThread,
            successCount.get() + failureCount.get(),
            "Total operations should match expected count");
    }

    @Test
    @DisplayName("Thread safety: concurrent reads during transitions")
    void testConcurrentReadsDuringTransitions() throws InterruptedException {
        int readerCount = 10;
        int writerCount = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount + writerCount);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);
        AtomicInteger inconsistencies = new AtomicInteger(0);

        // Reader threads - continuously read current state
        for (int i = 0; i < readerCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        AgentState state = stateMachine.getCurrentState();
                        if (state == null) {
                            inconsistencies.incrementAndGet();
                        }
                        readCount.incrementAndGet();
                        Thread.sleep(1); // Small delay to increase overlap
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Writer threads - perform state transitions
        for (int i = 0; i < writerCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 30; j++) {
                        AgentState current = stateMachine.getCurrentState();
                        AgentState target = getNextValidState(current);
                        if (target != null) {
                            stateMachine.transitionTo(target);
                            writeCount.incrementAndGet();
                        }
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(0, inconsistencies.get(), "No null states should be observed");
        assertEquals(readerCount * 100, readCount.get(), "All reads should complete");
    }

    @Test
    @DisplayName("Thread safety: compareAndSet race condition")
    void testCompareAndSetRaceCondition() throws InterruptedException {
        int threadCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger transitionSuccesses = new AtomicInteger(0);
        AtomicInteger transitionFailures = new AtomicInteger(0);

        // All threads try to transition from IDLE to PLANNING simultaneously
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    boolean result = stateMachine.transitionTo(AgentState.PLANNING);
                    if (result) {
                        transitionSuccesses.incrementAndGet();
                    } else {
                        transitionFailures.incrementAndGet();
                    }
                } catch (Exception e) {
                    transitionFailures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");

        // Exactly one transition should succeed due to compareAndSet
        assertEquals(1, transitionSuccesses.get(),
            "Only one thread should win the compareAndSet race");
        assertEquals(threadCount - 1, transitionFailures.get(),
            "All other threads should lose the race");
        assertEquals(AgentState.PLANNING, stateMachine.getCurrentState(),
            "State should be PLANNING after successful transition");
    }

    @Test
    @DisplayName("Thread safety: mixed transitions and resets")
    void testMixedTransitionsAndResets() throws InterruptedException {
        int transitionerCount = 5;
        int resetterCount = 2;
        int iterations = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(transitionerCount + resetterCount);
        AtomicInteger transitionAttempts = new AtomicInteger(0);
        AtomicInteger resetAttempts = new AtomicInteger(0);

        // Threads that perform normal transitions
        for (int i = 0; i < transitionerCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        AgentState current = stateMachine.getCurrentState();
                        AgentState target = getNextValidState(current);
                        if (target != null) {
                            stateMachine.transitionTo(target);
                            transitionAttempts.incrementAndGet();
                        }
                        Thread.sleep(2);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Threads that periodically reset the state machine
        for (int i = 0; i < resetterCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations / 2; j++) {
                        Thread.sleep(10);
                        stateMachine.reset();
                        resetAttempts.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");

        // State machine should still be in a valid state
        assertNotNull(stateMachine.getCurrentState(), "State should not be null");
        assertTrue(transitionAttempts.get() > 0, "Some transitions should have been attempted");
        assertTrue(resetAttempts.get() > 0, "Some resets should have occurred");
    }

    @Test
    @DisplayName("Thread safety: force transition during concurrent operations")
    void testForceTransitionDuringConcurrentOperations() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount + 1);
        AtomicInteger normalTransitions = new AtomicInteger(0);

        // Normal transition threads
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 20; j++) {
                        AgentState current = stateMachine.getCurrentState();
                        AgentState target = getNextValidState(current);
                        if (target != null && stateMachine.transitionTo(target)) {
                            normalTransitions.incrementAndGet();
                        }
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Force transition thread
        Thread forceThread = new Thread(() -> {
            try {
                startLatch.await();
                for (int j = 0; j < 5; j++) {
                    Thread.sleep(10);
                    stateMachine.forceTransition(AgentState.EXECUTING, "Emergency override " + j);
                    Thread.sleep(5);
                    stateMachine.forceTransition(AgentState.IDLE, "Reset " + j);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        forceThread.start();

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");

        // State machine should end in a valid state
        AgentState finalState = stateMachine.getCurrentState();
        assertNotNull(finalState, "Final state should not be null");
    }

    @Test
    @DisplayName("Thread safety: concurrent canTransitionTo calls")
    void testConcurrentCanTransitionToCalls() throws InterruptedException {
        int threadCount = 15;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger queryCount = new AtomicInteger(0);
        AtomicInteger nullResults = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        // Query if we can transition to various states
                        for (AgentState state : AgentState.values()) {
                            boolean result = stateMachine.canTransitionTo(state);
                            queryCount.incrementAndGet();
                            // Result should always be a valid boolean, never throw exception
                        }
                    }
                } catch (Exception e) {
                    nullResults.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(0, nullResults.get(), "No exceptions should be thrown");
        assertEquals(threadCount * 100 * AgentState.values().length, queryCount.get(),
            "All queries should complete");
    }

    @Test
    @DisplayName("Thread safety: event bus is not corrupted by concurrent events")
    void testEventBusNotCorruptedByConcurrentEvents() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger publishedEvents = new AtomicInteger(0);

        // Use a real event bus implementation for this test
        EventBus realEventBus = new com.minewright.event.SimpleEventBus();
        AgentStateMachine sm = new AgentStateMachine(realEventBus, "concurrent-test");

        // Subscribe to count events
        realEventBus.subscribe(StateTransitionEvent.class, event -> {
            publishedEvents.incrementAndGet();
        });

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        sm.transitionTo(AgentState.PLANNING);
                        Thread.sleep(1);
                        sm.reset();
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");

        // At least some events should have been published
        assertTrue(publishedEvents.get() > 0, "Some events should have been published");
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    @DisplayName("State machine handles rapid state changes correctly")
    void testRapidStateChanges() {
        // Perform rapid transitions without thread interference
        for (int i = 0; i < 100; i++) {
            assertTrue(stateMachine.transitionTo(AgentState.PLANNING));
            assertTrue(stateMachine.transitionTo(AgentState.EXECUTING));
            stateMachine.reset();
        }

        assertEquals(AgentState.IDLE, stateMachine.getCurrentState(),
            "State should return to IDLE after rapid changes");
    }

    @Test
    @DisplayName("State machine handles all state enum values")
    void testAllStateEnumValues() {
        // Verify all enum values are valid
        for (AgentState state : AgentState.values()) {
            assertNotNull(state.getDisplayName(), "Display name should not be null");
            assertNotNull(state.getDescription(), "Description should not be null");
            assertNotNull(state.toString(), "toString should not be null");
        }

        // Verify all states are reachable
        assertTrue(stateMachine.transitionTo(AgentState.PLANNING));
        assertTrue(stateMachine.transitionTo(AgentState.EXECUTING));
        assertTrue(stateMachine.transitionTo(AgentState.PAUSED));
        assertTrue(stateMachine.transitionTo(AgentState.EXECUTING));
        assertTrue(stateMachine.transitionTo(AgentState.COMPLETED));
        assertTrue(stateMachine.transitionTo(AgentState.IDLE));
        assertTrue(stateMachine.transitionTo(AgentState.PLANNING));
        assertTrue(stateMachine.transitionTo(AgentState.FAILED));
        assertTrue(stateMachine.transitionTo(AgentState.IDLE));
    }

    // ==================== Helper Methods ====================

    /**
     * Gets the next valid state in the transition cycle for testing.
     * This helps create more realistic concurrent tests where threads
     * attempt valid transitions rather than random ones.
     */
    private AgentState getNextValidState(AgentState current) {
        if (current == null) {
            return AgentState.IDLE;
        }

        switch (current) {
            case IDLE:
                return AgentState.PLANNING;
            case PLANNING:
                // Randomly choose between EXECUTING, FAILED, or IDLE
                int choice = (int) (Math.random() * 3);
                switch (choice) {
                    case 0: return AgentState.EXECUTING;
                    case 1: return AgentState.FAILED;
                    default: return AgentState.IDLE;
                }
            case EXECUTING:
                // Randomly choose between COMPLETED, FAILED, or PAUSED
                int execChoice = (int) (Math.random() * 3);
                switch (execChoice) {
                    case 0: return AgentState.COMPLETED;
                    case 1: return AgentState.FAILED;
                    default: return AgentState.PAUSED;
                }
            case PAUSED:
                // Randomly choose between EXECUTING or IDLE
                return Math.random() < 0.5 ? AgentState.EXECUTING : AgentState.IDLE;
            case COMPLETED:
                return AgentState.IDLE;
            case FAILED:
                return AgentState.IDLE;
            default:
                return AgentState.IDLE;
        }
    }
}
