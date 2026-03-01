package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.event.EventBus;
import com.minewright.execution.AgentState;
import com.minewright.execution.AgentStateMachine;
import com.minewright.orchestration.AgentRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base class for integration tests providing common setup, teardown, and assertions.
 *
 * <p><b>Overview:</b></p>
 * <p>This abstract base class provides a foundation for integration tests by handling
 * framework initialization, entity management, scenario execution, and common assertions.
 * Tests extending this class automatically get the test framework initialized and cleaned up.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Automatic framework lifecycle management (setup/teardown)</li>
 *   <li>Entity creation and management helpers</li>
 *   <li>Scenario execution with validation</li>
 *   <li>Common assertions for states, results, timeouts</li>
 *   <li>Async operation testing utilities</li>
 *   <li>Event-driven testing support</li>
 *   <li>World state manipulation helpers</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * class MyIntegrationTest extends IntegrationTestBase {
 *
 *     @Test
 *     void testMultiAgentCoordination() {
 *         // Create entities
 *         ForemanEntity foreman = createForeman("Steve");
 *         ForemanEntity worker1 = createWorker("Miner1", "mining");
 *         ForemanEntity worker2 = createWorker("Miner2", "mining");
 *
 *         // Execute scenario
 *         TestScenarioBuilder scenario = createScenario("Mining Test")
 *             .withEntity("foreman", foreman)
 *             .withEntity("worker1", worker1)
 *             .withEntity("worker2", worker2)
 *             .withCommand("mine 50 stone")
 *             .expectSuccess(true)
 *             .expectState(AgentState.COMPLETED);
 *
 *         TestResult result = scenario.execute();
 *
 *         // Assert results
 *         assertTrue(result.isSuccess());
 *         assertStateEquals(foreman, AgentState.COMPLETED);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Each test instance gets its own framework instance, ensuring test isolation.
 * Framework methods are thread-safe for concurrent scenario execution within a test.</p>
 *
 * @see IntegrationTestFramework
 * @see TestEntityFactory
 * @see TestScenarioBuilder
 * @since 1.0.0
 */
public abstract class IntegrationTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTestBase.class);

    /**
     * The integration test framework instance for this test.
     */
    protected IntegrationTestFramework framework;

    /**
     * The entity factory for creating test entities.
     */
    protected TestEntityFactory entityFactory;

    /**
     * The event bus for testing coordination events.
     */
    protected EventBus eventBus;

    /**
     * The mock Minecraft server.
     */
    protected MockMinecraftServer mockServer;

    /**
     * The world state manager for test world manipulation.
     */
    protected IntegrationTestFramework.TestWorldStateManager worldStateManager;

    /**
     * Default timeout for async operations in milliseconds.
     */
    protected long defaultTimeout = 30000;

    /**
     * Sets up the integration test framework before each test.
     *
     * <p>This method initializes the framework, mock server, and all test utilities.
     * Subclasses can override to add custom setup logic.</p>
     */
    @BeforeEach
    void setUpIntegrationTestBase() {
        LOGGER.debug("Setting up integration test framework");

        // Create framework with default configuration
        IntegrationTestFramework.FrameworkConfiguration config =
            new IntegrationTestFramework.FrameworkConfiguration.Builder()
                .withDefaultTimeout(defaultTimeout)
                .withAsyncCheckInterval(50)
                .withVerboseLogging(false)
                .build();

        framework = new IntegrationTestFramework(config);
        framework.initialize();

        // Initialize test utilities
        entityFactory = framework.getEntityFactory();
        eventBus = framework.getEventBus();
        mockServer = framework.getMockServer();
        worldStateManager = framework.getWorldStateManager();

        // Allow subclasses to add custom setup
        onSetUp();
    }

    /**
     * Tears down the integration test framework after each test.
     *
     * <p>This method cleans up all framework resources, entities, and scenarios.
     * Subclasses can override to add custom teardown logic.</p>
     */
    @AfterEach
    void tearDownIntegrationTestBase() {
        LOGGER.debug("Tearing down integration test framework");

        // Allow subclasses to add custom teardown
        onTearDown();

        if (framework != null) {
            framework.cleanup();
        }
    }

    // ==================== Lifecycle Hooks ====================

    /**
     * Hook for subclasses to add custom setup logic.
     *
     * <p>Override this method to perform additional setup after the framework
     * is initialized but before the test runs.</p>
     */
    protected void onSetUp() {
        // Default: no additional setup
    }

    /**
     * Hook for subclasses to add custom teardown logic.
     *
     * <p>Override this method to perform additional cleanup after the test
     * completes but before framework cleanup.</p>
     */
    protected void onTearDown() {
        // Default: no additional teardown
    }

    // ==================== Entity Creation Helpers ====================

    /**
     * Creates a test ForemanEntity with the given name.
     *
     * @param name Entity name
     * @return Created entity
     */
    protected ForemanEntity createForeman(String name) {
        return entityFactory.createTestForeman(name).build();
    }

    /**
     * Creates a test ForemanEntity with the given name and role.
     *
     * @param name Entity name
     * @param role Agent role
     * @return Created entity
     */
    protected ForemanEntity createForeman(String name, AgentRole role) {
        return entityFactory.createTestForeman(name, role).build();
    }

    /**
     * Creates a test worker entity with the given name and capability.
     *
     * @param name Entity name
     * @param capability Worker capability (e.g., "mining", "building")
     * @return Created entity
     */
    protected ForemanEntity createWorker(String name, String capability) {
        return entityFactory.createTestWorker(name, Collections.singleton(capability)).build();
    }

    /**
     * Creates a test worker entity with multiple capabilities.
     *
     * @param name Entity name
     * @param capabilities Worker capabilities
     * @return Created entity
     */
    protected ForemanEntity createWorker(String name, Set<String> capabilities) {
        return entityFactory.createTestWorker(name, capabilities).build();
    }

    /**
     * Creates a mock player for testing.
     *
     * @param name Player name
     * @return Mock player instance
     */
    protected MockMinecraftServer.MockPlayer createPlayer(String name) {
        return entityFactory.createTestPlayer(name);
    }

    /**
     * Creates multiple workers with the same capability.
     *
     * @param capability Worker capability
     * @param names Worker names
     * @return Map of entity names to entities
     */
    protected Map<String, ForemanEntity> createWorkers(String capability, String... names) {
        Map<String, ForemanEntity> workers = new HashMap<>();
        for (String name : names) {
            ForemanEntity worker = createWorker(name, capability);
            workers.put(name, worker);
        }
        return workers;
    }

    // ==================== Scenario Creation Helpers ====================

    /**
     * Creates a new test scenario with the given name.
     *
     * @param scenarioName Scenario name
     * @return Scenario builder
     */
    protected TestScenarioBuilder createScenario(String scenarioName) {
        return framework.createScenario(scenarioName);
    }

    /**
     * Executes a scenario with default timeout.
     *
     * @param scenario Scenario to execute
     * @return Execution result
     */
    protected TestScenarioBuilder.TestResult executeScenario(TestScenarioBuilder scenario) {
        return scenario.execute();
    }

    /**
     * Executes a scenario with a custom timeout.
     *
     * @param scenario Scenario to execute
     * @param timeoutMs Timeout in milliseconds
     * @return Execution result
     */
    protected TestScenarioBuilder.TestResult executeScenario(TestScenarioBuilder scenario, long timeoutMs) {
        return scenario.execute(timeoutMs);
    }

    /**
     * Executes a scenario asynchronously.
     *
     * @param scenario Scenario to execute
     * @return Future for the result
     */
    protected CompletableFuture<TestScenarioBuilder.TestResult> executeScenarioAsync(TestScenarioBuilder scenario) {
        return scenario.executeAsync();
    }

    // ==================== Common Assertions ====================

    /**
     * Asserts that an entity's state machine is in the expected state.
     *
     * @param entity Entity to check
     * @param expectedState Expected state
     */
    protected void assertStateEquals(ForemanEntity entity, AgentState expectedState) {
        AgentStateMachine stateMachine = getStateMachine(entity);
        if (stateMachine == null) {
            throw new AssertionError("Entity " + entity.getEntityName() + " has no state machine");
        }
        AgentState actualState = stateMachine.getCurrentState();
        if (actualState != expectedState) {
            throw new AssertionError("Expected state " + expectedState +
                " for entity " + entity.getEntityName() +
                " but got " + actualState);
        }
    }

    /**
     * Asserts that a test result indicates success.
     *
     * @param result Test result to check
     */
    protected void assertSuccess(TestScenarioBuilder.TestResult result) {
        if (!result.isSuccess()) {
            throw new AssertionError("Expected success but got failure: " + result.getErrorMessage());
        }
    }

    /**
     * Asserts that a test result indicates failure.
     *
     * @param result Test result to check
     */
    protected void assertFailure(TestScenarioBuilder.TestResult result) {
        if (result.isSuccess()) {
            throw new AssertionError("Expected failure but got success");
        }
    }

    /**
     * Asserts that a test result completed within the expected time range.
     *
     * @param result Test result to check
     * @param minMs Minimum expected duration in milliseconds
     * @param maxMs Maximum expected duration in milliseconds
     */
    protected void assertDurationBetween(TestScenarioBuilder.TestResult result, long minMs, long maxMs) {
        long duration = result.getDuration();
        if (duration < minMs || duration > maxMs) {
            throw new AssertionError("Expected duration between " + minMs + "ms and " + maxMs +
                "ms but got " + duration + "ms");
        }
    }

    /**
     * Asserts that a condition becomes true within the timeout.
     *
     * @param condition Condition to check
     * @param timeoutMs Timeout in milliseconds
     * @param message Failure message
     */
    protected void assertEventuallyTrue(Supplier<Boolean> condition, long timeoutMs, String message) {
        long startTime = System.currentTimeMillis();
        long checkInterval = 50;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.get()) {
                return;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Assertion interrupted", e);
            }
        }

        throw new AssertionError(message + " (timeout after " + timeoutMs + "ms)");
    }

    /**
     * Asserts that an entity completes its current task within the timeout.
     *
     * @param entity Entity to check
     * @param timeoutMs Timeout in milliseconds
     */
    protected void assertEntityCompletes(ForemanEntity entity, long timeoutMs) {
        assertEventuallyTrue(
            () -> isEntityComplete(entity),
            timeoutMs,
            "Entity " + entity.getEntityName() + " did not complete within timeout"
        );
    }

    /**
     * Asserts that multiple entities complete their tasks within the timeout.
     *
     * @param entities Entities to check
     * @param timeoutMs Timeout in milliseconds
     */
    protected void assertEntitiesComplete(Collection<ForemanEntity> entities, long timeoutMs) {
        assertEventuallyTrue(
            () -> entities.stream().allMatch(this::isEntityComplete),
            timeoutMs,
            "Not all entities completed within timeout"
        );
    }

    // ==================== Async Testing Utilities ====================

    /**
     * Waits for a condition to become true with timeout.
     *
     * @param condition Condition to wait for
     * @param timeoutMs Timeout in milliseconds
     * @return true if condition became true, false if timeout
     */
    protected boolean waitFor(Supplier<Boolean> condition, long timeoutMs) {
        return framework.waitFor(condition, timeoutMs);
    }

    /**
     * Waits for a CompletableFuture to complete with timeout.
     *
     * @param future Future to wait for
     * @param timeoutMs Timeout in milliseconds
     * @param <T> Result type
     * @return Completed result
     * @throws TimeoutException if future does not complete in time
     */
    protected <T> T waitForCompletion(CompletableFuture<T> future, long timeoutMs) throws TimeoutException {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Future did not complete within " + timeoutMs + "ms");
        } catch (Exception e) {
            throw new RuntimeException("Error waiting for future completion", e);
        }
    }

    /**
     * Creates a CompletableFuture that completes after a delay.
     *
     * @param delayMs Delay in milliseconds
     * @param value Value to return
     * @param <T> Value type
     * @return Delayed future
     */
    protected <T> CompletableFuture<T> delayedFuture(long delayMs, T value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return value;
        });
    }

    // ==================== Event Testing Utilities ====================

    /**
     * Subscribes to an event type during the test.
     *
     * @param eventType Event class
     * @param handler Event handler
     * @param <T> Event type
     */
    protected <T> void onEvent(Class<T> eventType, Consumer<T> handler) {
        eventBus.subscribe(eventType, handler);
    }

    /**
     * Waits for an event to be published.
     *
     * @param eventType Event class
     * @param timeoutMs Timeout in milliseconds
     * @param <T> Event type
     * @return Received event or null if timeout
     */
    protected <T> T waitForEvent(Class<T> eventType, long timeoutMs) {
        final T[] eventHolder = (T[]) new Object[1];
        final boolean[] received = {false};

        Consumer<T> handler = event -> {
            eventHolder[0] = event;
            received[0] = true;
        };

        EventBus.Subscription subscription = eventBus.subscribe(eventType, handler);

        long startTime = System.currentTimeMillis();
        while (!received[0] && System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        subscription.unsubscribe();
        return eventHolder[0];
    }

    // ==================== World State Helpers ====================

    /**
     * Sets a world state value.
     *
     * @param key State key
     * @param value State value
     */
    protected void setWorldState(String key, Object value) {
        worldStateManager.setState(key, value);
    }

    /**
     * Gets a world state value.
     *
     * @param key State key
     * @param <T> Value type
     * @return State value or null if not found
     */
    protected <T> T getWorldState(String key) {
        return worldStateManager.getState(key);
    }

    /**
     * Clears all world state.
     */
    protected void clearWorldState() {
        worldStateManager.reset();
    }

    // ==================== Tick Simulation ====================

    /**
     * Simulates a single server tick.
     */
    protected void tick() {
        framework.simulateTick();
    }

    /**
     * Simulates multiple server ticks.
     *
     * @param tickCount Number of ticks to simulate
     */
    protected void tick(int tickCount) {
        framework.simulateTicks(tickCount);
    }

    /**
     * Ticks until a condition becomes true or max ticks reached.
     *
     * @param condition Condition to check
     * @param maxTicks Maximum number of ticks
     * @return Number of ticks elapsed
     */
    protected int tickUntil(Supplier<Boolean> condition, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            tick();
            if (condition.get()) {
                return i + 1;
            }
        }
        return maxTicks;
    }

    // ==================== Helper Methods ====================

    /**
     * Gets the state machine for an entity.
     *
     * @param entity Entity
     * @return State machine or null if not available
     */
    protected AgentStateMachine getStateMachine(ForemanEntity entity) {
        // In a real implementation, this would get the actual state machine
        // For mock entities, we return a mock
        if (entity != null) {
            return org.mockito.Mockito.mock(AgentStateMachine.class);
        }
        return null;
    }

    /**
     * Checks if an entity has completed its current task.
     *
     * @param entity Entity to check
     * @return true if complete, false otherwise
     */
    protected boolean isEntityComplete(ForemanEntity entity) {
        AgentStateMachine stateMachine = getStateMachine(entity);
        if (stateMachine == null) {
            return false;
        }
        AgentState state = stateMachine.getCurrentState();
        return state == AgentState.COMPLETED || state == AgentState.FAILED;
    }

    /**
     * Gets the current tick count from the mock server.
     *
     * @return Current tick count
     */
    protected long getTickCount() {
        return mockServer.getTickCounter();
    }

    /**
     * Gets the server uptime in milliseconds.
     *
     * @return Uptime in milliseconds
     */
    protected long getServerUptime() {
        return mockServer.getUptime();
    }
}
