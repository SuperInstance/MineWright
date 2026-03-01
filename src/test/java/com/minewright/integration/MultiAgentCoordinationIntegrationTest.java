package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.orchestration.AgentRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Example integration test demonstrating multi-agent coordination scenarios.
 *
 * <p><b>Purpose:</b></p>
 * <p>This test class demonstrates how to use the integration test framework
 * to test multi-agent coordination scenarios in the MineWright mod.</p>
 *
 * <p><b>Test Scenarios:</b></p>
 * <ul>
 *   <li>Single foreman entity task execution</li>
 *   <li>Foreman with multiple workers coordinating tasks</li>
 *   <li>State transitions during task execution</li>
 *   <li>Async command processing with timeout handling</li>
 * </ul>
 *
 * @see IntegrationTestBase
 * @see IntegrationTestFramework
 * @since 1.0.0
 */
@DisplayName("Multi-Agent Coordination Integration Tests")
class MultiAgentCoordinationIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Single foreman entity executes simple command")
    void testSingleForemanCommand() {
        // Create a foreman entity
        ForemanEntity foreman = createForeman("Steve");

        // Build and execute scenario
        TestScenarioBuilder scenario = createScenario("Single Foreman Command")
            .withForeman("Steve", AgentRole.FOREMAN)
            .withCommand("echo hello")
            .expectState(AgentState.COMPLETED)
            .expectSuccess(true);

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        // Verify result
        assertSuccess(result);
        assertStateEquals(foreman, AgentState.COMPLETED);
    }

    @Test
    @DisplayName("Foreman coordinates multiple workers")
    void testForemanCoordinatesWorkers() {
        // Create entities
        ForemanEntity foreman = createForeman("Steve", AgentRole.FOREMAN);
        Map<String, ForemanEntity> workers = createWorkers("mining", "Miner1", "Miner2", "Miner3");

        // Build scenario with foreman and workers
        TestScenarioBuilder scenario = createScenario("Multi-Agent Mining")
            .withEntity("foreman", foreman)
            .withEntity("worker1", workers.get("Miner1"))
            .withEntity("worker2", workers.get("Miner2"))
            .withEntity("worker3", workers.get("Miner3"))
            .withCommand("mine 100 stone")
            .expectSuccess(true)
            .expectDuration(100, 10000); // Expect completion between 100ms and 10s

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        // Verify all entities completed
        assertSuccess(result);
        assertEntityCompletes(foreman, 5000);
        assertEntitiesComplete(workers.values(), 5000);
    }

    @Test
    @DisplayName("State machine transitions during task execution")
    void testStateMachineTransitions() {
        ForemanEntity foreman = createForeman("Steve");

        TestScenarioBuilder scenario = createScenario("State Transition Test")
            .withEntity("foreman", foreman)
            .withCommand("test command")
            .expectState(stateMachine -> {
                // Validate state machine reached expected states
                assertNotNull(stateMachine, "State machine should not be null");
                assertEquals(AgentState.COMPLETED, stateMachine.getCurrentState(),
                    "Final state should be COMPLETED");
            });

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        assertSuccess(result);
    }

    @Test
    @DisplayName("Async scenario execution")
    void testAsyncScenarioExecution() {
        ForemanEntity foreman = createForeman("Steve");

        TestScenarioBuilder scenario = createScenario("Async Test")
            .withEntity("foreman", foreman)
            .withCommand("async command")
            .expectSuccess(true);

        // Execute asynchronously
        var futureResult = executeScenarioAsync(scenario);

        // Wait for completion
        try {
            TestScenarioBuilder.TestResult result = waitForCompletion(futureResult, 5000);
            assertSuccess(result);
        } catch (Exception e) {
            fail("Async execution failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Event-driven coordination")
    void testEventDrivenCoordination() {
        ForemanEntity foreman = createForeman("Steve");

        // Subscribe to state transition events
        final int[] transitionCount = {0};
        onEvent(com.minewright.event.StateTransitionEvent.class, event -> {
            transitionCount[0]++;
        });

        TestScenarioBuilder scenario = createScenario("Event Test")
            .withEntity("foreman", foreman)
            .withCommand("test")
            .expectSuccess(true);

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        assertSuccess(result);
        assertTrue(transitionCount[0] > 0, "Should have received state transition events");
    }

    @Test
    @DisplayName("World state manipulation during scenario")
    void testWorldStateManipulation() {
        // Set up initial world state
        setWorldState("stone_count", 0);
        setWorldState("target_stone", 50);

        ForemanEntity foreman = createForeman("Steve");

        TestScenarioBuilder scenario = createScenario("World State Test")
            .withSetup(() -> {
                // Additional setup before scenario execution
                setWorldState("test_phase", "mining");
            })
            .withEntity("foreman", foreman)
            .withCommand("mine 50 stone")
            .expectResult(result -> {
                // Validate world state after execution
                Integer stoneCount = getWorldState("stone_count");
                assertNotNull(stoneCount, "Stone count should be set");
                assertTrue(stoneCount >= 0, "Stone count should be non-negative");
            })
            .withTeardown(() -> {
                // Cleanup after scenario
                clearWorldState();
            });

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        assertSuccess(result);
    }

    @Test
    @DisplayName("Tick simulation during task execution")
    void testTickSimulation() {
        ForemanEntity foreman = createForeman("Steve");
        final long[] tickCounter = {0};

        TestScenarioBuilder scenario = createScenario("Tick Simulation Test")
            .withEntity("foreman", foreman)
            .withCommand("test")
            .expectSuccess(true);

        // Simulate ticks during execution
        executeScenario(scenario);

        // Verify ticks occurred
        long finalTickCount = getTickCount();
        assertTrue(finalTickCount > 0, "Should have simulated ticks");
    }

    @Test
    @DisplayName("Multiple sequential tasks")
    void testMultipleSequentialTasks() {
        ForemanEntity foreman = createForeman("Steve");

        // Execute multiple scenarios sequentially
        TestScenarioBuilder scenario1 = createScenario("Task 1")
            .withEntity("foreman", foreman)
            .withCommand("task 1")
            .expectSuccess(true);

        TestScenarioBuilder scenario2 = createScenario("Task 2")
            .withEntity("foreman", foreman)
            .withCommand("task 2")
            .expectSuccess(true);

        TestScenarioBuilder.TestResult result1 = executeScenario(scenario1);
        assertSuccess(result1);

        TestScenarioBuilder.TestResult result2 = executeScenario(scenario2);
        assertSuccess(result2);
    }

    @Test
    @DisplayName("Timeout handling for long-running tasks")
    void testTimeoutHandling() {
        ForemanEntity foreman = createForeman("Steve");

        TestScenarioBuilder scenario = createScenario("Timeout Test")
            .withEntity("foreman", foreman)
            .withCommand("long running task")
            .withTimeout(1000) // 1 second timeout
            .expectSuccess(false); // Expected to timeout

        TestScenarioBuilder.TestResult result = executeScenario(scenario);

        // Should fail due to timeout
        assertFailure(result);
    }

    @Test
    @DisplayName("Complex multi-agent scenario with multiple capabilities")
    void testComplexMultiAgentScenario() {
        // Create entities with different capabilities
        ForemanEntity foreman = createForeman("Steve", AgentRole.FOREMAN);
        ForemanEntity miner = createWorker("Miner", "mining");
        ForemanEntity builder = createWorker("Builder", "building");
        ForemanEntity hauler = createWorker("Hauler", "hauling");

        // Set up world state for complex scenario
        setWorldState("resources", Map.of(
            "stone", 0,
            "wood", 0,
            "structures", 0
        ));

        TestScenarioBuilder scenario = createScenario("Complex Multi-Agent")
            .withSetup(() -> {
                setWorldState("phase", "execution");
            })
            .withEntity("foreman", foreman)
            .withEntity("miner", miner)
            .withEntity("builder", builder)
            .withEntity("hauler", hauler)
            .withCommand("build a house with 50 stone and 100 wood")
            .expectSuccess(true)
            .expectResult(result -> {
                // Validate resources were gathered
                @SuppressWarnings("unchecked")
                Map<String, Integer> resources = getWorldState("resources");
                assertNotNull(resources, "Resources map should not be null");
                assertTrue(resources.getOrDefault("stone", 0) >= 50,
                    "Should have gathered at least 50 stone");
            })
            .withTeardown(() -> {
                clearWorldState();
            });

        TestScenarioBuilder.TestResult result = executeScenario(scenario, 30000);

        assertSuccess(result);
        assertEntitiesComplete(java.util.Set.of(miner, builder, hauler), 30000);
    }

    // ==================== Helper Methods ====================

    private void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

    private void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void fail(String message) {
        throw new AssertionError(message);
    }
}
