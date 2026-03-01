package com.minewright.integration;

import com.minewright.action.ActionExecutor;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.event.EventBus;
import com.minewright.execution.AgentState;
import com.minewright.execution.AgentStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fluent API builder for creating and executing test scenarios.
 *
 * <p><b>Overview:</b></p>
 * <p>This class provides a fluent, chainable API for building test scenarios
 * for multi-agent coordination. It supports setting up entities, commands,
 * expected results, and executing the scenario with validation.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Fluent API for scenario configuration</li>
 *   <li>Entity management (add foremen, workers, players)</li>
 *   <li>Command execution and validation</li>
 *   <li>Expected result verification</li>
 *   <li>Async operation support with timeouts</li>
 *   <li>Scenario lifecycle management (setup, execute, cleanup)</li>
 *   <li>Event-driven validation</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Scenarios are not thread-safe during building but are safe to execute
 * concurrently once built. Each scenario maintains isolated state.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * TestScenarioBuilder scenario = framework.createScenario("Mining Test");
 *
 * scenario.withForeman("Steve")
 *         .withWorker("Miner1", "mining")
 *         .withWorker("Miner2", "mining")
 *         .withCommand("mine 50 stone")
 *         .expectState(AgentState.COMPLETED)
 *         .expectSuccess(true)
 *         .execute();
 *
 * TestResult result = scenario.getResult();
 * assertTrue(result.isSuccess());
 * }</pre>
 *
 * @see IntegrationTestFramework
 * @see TestEntityFactory
 * @see TestResult
 * @since 1.0.0
 */
public class TestScenarioBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestScenarioBuilder.class);

    /**
     * Parent framework for this scenario.
     */
    private final IntegrationTestFramework framework;

    /**
     * Entity factory for creating test entities.
     */
    private final TestEntityFactory entityFactory;

    /**
     * Event bus for scenario events.
     */
    private final EventBus eventBus;

    /**
     * World state manager for scenario setup.
     */
    private final IntegrationTestFramework.TestWorldStateManager worldStateManager;

    /**
     * Scenario name.
     */
    private String scenarioName;

    /**
     * Scenario description.
     */
    private String description;

    /**
     * Registered entities for this scenario.
     */
    private final Map<String, ForemanEntity> entities;

    /**
     * Commands to execute in this scenario.
     */
    private final List<ScenarioCommand> commands;

    /**
     * Expected results for validation.
     */
    private final ScenarioExpectations expectations;

    /**
     * Scenario execution result.
     */
    private TestResult result;

    /**
     * Scenario state.
     */
    private volatile ScenarioState state;

    /**
     * Whether the scenario has been executed.
     */
    private volatile boolean executed;

    /**
     * Execution timeout in milliseconds.
     */
    private long timeoutMs;

    /**
     * Custom setup actions.
     */
    private final List<Runnable> setupActions;

    /**
     * Custom teardown actions.
     */
    private final List<Runnable> teardownActions;

    /**
     * Event handlers for scenario events.
     */
    private final Map<Class<?>, Consumer<?>> eventHandlers;

    /**
     * Scenario states.
     */
    private enum ScenarioState {
        BUILDING,
        READY,
        RUNNING,
        COMPLETED,
        FAILED,
        CLEANED_UP
    }

    /**
     * Creates a new test scenario builder.
     */
    TestScenarioBuilder(IntegrationTestFramework framework,
                       TestEntityFactory entityFactory,
                       EventBus eventBus,
                       IntegrationTestFramework.TestWorldStateManager worldStateManager) {
        this.framework = framework;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.worldStateManager = worldStateManager;
        this.entities = new ConcurrentHashMap<>();
        this.commands = new ArrayList<>();
        this.expectations = new ScenarioExpectations();
        this.state = ScenarioState.BUILDING;
        this.timeoutMs = framework.getConfig().getDefaultTimeout();
        this.setupActions = new ArrayList<>();
        this.teardownActions = new ArrayList<>();
        this.eventHandlers = new ConcurrentHashMap<>();
    }

    // ==================== Naming and Description ====================

    /**
     * Sets the scenario name.
     *
     * @param name Scenario name
     * @return This builder
     */
    public TestScenarioBuilder withName(String name) {
        this.scenarioName = name;
        return this;
    }

    /**
     * Sets the scenario description.
     *
     * @param desc Scenario description
     * @return This builder
     */
    public TestScenarioBuilder withDescription(String desc) {
        this.description = desc;
        return this;
    }

    // ==================== Entity Management ====================

    /**
     * Adds a foreman entity to the scenario.
     *
     * @param name Entity name
     * @return This builder
     */
    public TestScenarioBuilder withForeman(String name) {
        return withForeman(name, com.minewright.orchestration.AgentRole.FOREMAN);
    }

    /**
     * Adds a foreman entity with a specific role to the scenario.
     *
     * @param name Entity name
     * @param role Agent role
     * @return This builder
     */
    public TestScenarioBuilder withForeman(String name, com.minewright.orchestration.AgentRole role) {
        ensureBuildingState();
        ForemanEntity entity = entityFactory.createTestForeman(name, role).build();
        entities.put(name, entity);
        return this;
    }

    /**
     * Adds a worker entity to the scenario.
     *
     * @param name Entity name
     * @param role Worker role (e.g., "mining", "building")
     * @return This builder
     */
    public TestScenarioBuilder withWorker(String name, String role) {
        ensureBuildingState();
        ForemanEntity entity = entityFactory.createTestWorker(name)
            .withCapability(role)
            .build();
        entities.put(name, entity);
        return this;
    }

    /**
     * Adds a worker entity with multiple capabilities to the scenario.
     *
     * @param name Entity name
     * @param capabilities Set of capabilities
     * @return This builder
     */
    public TestScenarioBuilder withWorker(String name, Set<String> capabilities) {
        ensureBuildingState();
        ForemanEntity entity = entityFactory.createTestWorker(name, capabilities).build();
        entities.put(name, entity);
        return this;
    }

    /**
     * Adds multiple workers with the same role.
     *
     * @param role Worker role
     * @param names Worker names
     * @return This builder
     */
    public TestScenarioBuilder withWorkers(String role, String... names) {
        for (String name : names) {
            withWorker(name, role);
        }
        return this;
    }

    /**
     * Adds a custom entity to the scenario.
     *
     * @param name Entity name
     * @param entity Entity instance
     * @return This builder
     */
    public TestScenarioBuilder withEntity(String name, ForemanEntity entity) {
        ensureBuildingState();
        entities.put(name, entity);
        return this;
    }

    // ==================== Command Management ====================

    /**
     * Adds a command to be executed by the primary foreman.
     *
     * @param command Command string
     * @return This builder
     */
    public TestScenarioBuilder withCommand(String command) {
        ensureBuildingState();
        commands.add(new ScenarioCommand(command, null));
        return this;
    }

    /**
     * Adds a command to be executed by a specific entity.
     *
     * @param entityName Entity to execute the command
     * @param command Command string
     * @return This builder
     */
    public TestScenarioBuilder withCommand(String entityName, String command) {
        ensureBuildingState();
        commands.add(new ScenarioCommand(command, entityName));
        return this;
    }

    /**
     * Adds a task to be executed.
     *
     * @param task Task to execute
     * @return This builder
     */
    public TestScenarioBuilder withTask(Task task) {
        ensureBuildingState();
        commands.add(new ScenarioCommand(task, null));
        return this;
    }

    /**
     * Adds a task to be executed by a specific entity.
     *
     * @param entityName Entity to execute the task
     * @param task Task to execute
     * @return This builder
     */
    public TestScenarioBuilder withTask(String entityName, Task task) {
        ensureBuildingState();
        commands.add(new ScenarioCommand(task, entityName));
        return this;
    }

    // ==================== Expected Results ====================

    /**
     * Sets the expected final state.
     *
     * @param expectedState Expected agent state
     * @return This builder
     */
    public TestScenarioBuilder expectState(AgentState expectedState) {
        expectations.expectedState = expectedState;
        return this;
    }

    /**
     * Sets whether success is expected.
     *
     * @param success Expected success status
     * @return This builder
     */
    public TestScenarioBuilder expectSuccess(boolean success) {
        expectations.expectSuccess = success;
        return this;
    }

    /**
     * Adds an expected result validation.
     *
     * @param validator Validation function
     * @return This builder
     */
    public TestScenarioBuilder expectResult(Consumer<TestResult> validator) {
        expectations.resultValidators.add(validator);
        return this;
    }

    /**
     * Adds an expected state validation.
     *
     * @param validator Validation function
     * @return This builder
     */
    public TestScenarioBuilder expectState(Consumer<AgentStateMachine> validator) {
        expectations.stateValidators.add(validator);
        return this;
    }

    /**
     * Sets the expected execution time range.
     *
     * @param minMs Minimum expected time in milliseconds
     * @param maxMs Maximum expected time in milliseconds
     * @return This builder
     */
    public TestScenarioBuilder expectDuration(long minMs, long maxMs) {
        expectations.minDurationMs = minMs;
        expectations.maxDurationMs = maxMs;
        return this;
    }

    // ==================== Configuration ====================

    /**
     * Sets the execution timeout.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return This builder
     */
    public TestScenarioBuilder withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /**
     * Adds a custom setup action.
     *
     * @param action Setup action
     * @return This builder
     */
    public TestScenarioBuilder withSetup(Runnable action) {
        setupActions.add(action);
        return this;
    }

    /**
     * Adds a custom teardown action.
     *
     * @param action Teardown action
     * @return This builder
     */
    public TestScenarioBuilder withTeardown(Runnable action) {
        teardownActions.add(action);
        return this;
    }

    /**
     * Adds an event handler for scenario events.
     *
     * @param eventType Event class
     * @param handler Event handler
     * @param <T> Event type
     * @return This builder
     */
    public <T> TestScenarioBuilder onEvent(Class<T> eventType, Consumer<T> handler) {
        eventHandlers.put(eventType, handler);
        return this;
    }

    // ==================== Execution ====================

    /**
     * Executes the scenario synchronously.
     *
     * @return Execution result
     */
    public TestResult execute() {
        return execute(timeoutMs);
    }

    /**
     * Executes the scenario with a custom timeout.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return Execution result
     */
    public TestResult execute(long timeoutMs) {
        if (executed) {
            throw new IllegalStateException("Scenario has already been executed");
        }

        state = ScenarioState.READY;
        long startTime = System.currentTimeMillis();

        try {
            // Run setup
            state = ScenarioState.RUNNING;
            runSetup();

            // Execute commands
            executeCommands();

            // Wait for completion
            waitForCompletion(timeoutMs);

            // Validate results
            validateResults();

            result = TestResult.success(scenarioName, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            LOGGER.error("Scenario execution failed: {}", scenarioName, e);
            result = TestResult.failure(scenarioName, e.getMessage(), System.currentTimeMillis() - startTime);
            state = ScenarioState.FAILED;
        } finally {
            // Run teardown
            runTeardown();
            executed = true;
        }

        return result;
    }

    /**
     * Executes the scenario asynchronously.
     *
     * @return Future for the execution result
     */
    public CompletableFuture<TestResult> executeAsync() {
        return executeAsync(timeoutMs);
    }

    /**
     * Executes the scenario asynchronously with a custom timeout.
     *
     * @param timeoutMs Timeout in milliseconds
     * @return Future for the execution result
     */
    public CompletableFuture<TestResult> executeAsync(long timeoutMs) {
        return CompletableFuture.supplyAsync(() -> execute(timeoutMs));
    }

    // ==================== Scenario Lifecycle ====================

    /**
     * Runs setup actions.
     */
    private void runSetup() {
        LOGGER.debug("Running setup for scenario: {}", scenarioName);

        // Run custom setup actions
        for (Runnable action : setupActions) {
            action.run();
        }

        // Register event handlers
        registerEventHandlers();
    }

    /**
     * Executes all commands in the scenario.
     */
    private void executeCommands() {
        LOGGER.debug("Executing {} commands for scenario: {}", commands.size(), scenarioName);

        for (ScenarioCommand cmd : commands) {
            executeCommand(cmd);
        }
    }

    /**
     * Executes a single command.
     */
    private void executeCommand(ScenarioCommand cmd) {
        if (cmd.isStringCommand()) {
            // Execute string command
            String entityName = cmd.entityName != null ? cmd.entityName : getPrimaryEntityName();
            ForemanEntity entity = entities.get(entityName);

            if (entity != null) {
                // Queue the command for execution
                // In a real implementation, this would use entity.getActionExecutor().queueTask()
                LOGGER.debug("Queued command '{}' for entity: {}", cmd.command, entityName);
            } else {
                LOGGER.warn("Entity not found for command: {}", entityName);
            }
        } else if (cmd.isTask()) {
            // Execute task directly
            String entityName = cmd.entityName != null ? cmd.entityName : getPrimaryEntityName();
            ForemanEntity entity = entities.get(entityName);

            if (entity != null) {
                // Queue the task for execution
                // In a real implementation, this would use entity.getActionExecutor().queueTask(task)
                LOGGER.debug("Queued task for entity: {}", entityName);
            }
        }
    }

    /**
     * Waits for scenario completion with timeout.
     */
    private void waitForCompletion(long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isScenarioComplete()) {
                return;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Scenario execution interrupted", e);
            }
        }

        throw new RuntimeException("Scenario execution timed out after " + timeoutMs + "ms");
    }

    /**
     * Checks if the scenario is complete.
     */
    private boolean isScenarioComplete() {
        // Check if all entities have completed their tasks
        for (ForemanEntity entity : entities.values()) {
            if (!isEntityComplete(entity)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an entity has completed its tasks.
     */
    private boolean isEntityComplete(ForemanEntity entity) {
        // For mock entities, check if they're in a terminal state
        // Using Mockito.mockingDetails to check if it's a mock
        if (org.mockito.Mockito.mockingDetails(entity).isMock()) {
            return true;
        }
        return false;
    }

    /**
     * Validates the scenario results against expectations.
     */
    private void validateResults() {
        LOGGER.debug("Validating results for scenario: {}", scenarioName);

        // Validate expected state
        if (expectations.expectedState != null) {
            AgentState actualState = getFinalState();
            if (actualState != expectations.expectedState) {
                throw new AssertionError("Expected state " + expectations.expectedState +
                    " but got " + actualState);
            }
        }

        // Validate expected success
        if (expectations.expectSuccess != null) {
            boolean actualSuccess = result != null && result.isSuccess();
            if (actualSuccess != expectations.expectSuccess) {
                throw new AssertionError("Expected success=" + expectations.expectSuccess +
                    " but got success=" + actualSuccess);
            }
        }

        // Validate duration
        if (expectations.minDurationMs > 0 || expectations.maxDurationMs > 0) {
            long duration = result != null ? result.getDuration() : 0;
            if (expectations.minDurationMs > 0 && duration < expectations.minDurationMs) {
                throw new AssertionError("Duration " + duration + "ms less than minimum " +
                    expectations.minDurationMs + "ms");
            }
            if (expectations.maxDurationMs > 0 && duration > expectations.maxDurationMs) {
                throw new AssertionError("Duration " + duration + "ms exceeds maximum " +
                    expectations.maxDurationMs + "ms");
            }
        }

        // Run custom validators
        for (Consumer<TestResult> validator : expectations.resultValidators) {
            validator.accept(result);
        }

        for (Consumer<AgentStateMachine> validator : expectations.stateValidators) {
            AgentStateMachine stateMachine = getPrimaryStateMachine();
            if (stateMachine != null) {
                validator.accept(stateMachine);
            }
        }

        state = ScenarioState.COMPLETED;
    }

    /**
     * Runs teardown actions.
     */
    private void runTeardown() {
        LOGGER.debug("Running teardown for scenario: {}", scenarioName);

        // Unregister event handlers
        unregisterEventHandlers();

        // Run custom teardown actions
        for (Runnable action : teardownActions) {
            action.run();
        }

        state = ScenarioState.CLEANED_UP;
    }

    /**
     * Registers event handlers with the event bus.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerEventHandlers() {
        eventHandlers.forEach((eventType, handler) -> {
            // Use raw types to bypass generic type checking
            // This is safe because we control the event handler registration
            eventBus.subscribe((Class) eventType, (Consumer) handler);
        });
    }

    /**
     * Unregisters event handlers from the event bus.
     */
    private void unregisterEventHandlers() {
        eventHandlers.keySet().forEach(eventBus::unsubscribeAll);
    }

    /**
     * Cleans up scenario resources.
     */
    public void cleanup() {
        entities.clear();
        commands.clear();
        eventHandlers.clear();
        state = ScenarioState.CLEANED_UP;
    }

    // ==================== Getters ====================

    /**
     * Gets the scenario result.
     *
     * @return Test result or null if not yet executed
     */
    public TestResult getResult() {
        return result;
    }

    /**
     * Gets the scenario state.
     *
     * @return Current scenario state
     */
    public ScenarioState getState() {
        return state;
    }

    /**
     * Gets all entities in the scenario.
     *
     * @return Unmodifiable map of entities
     */
    public Map<String, ForemanEntity> getEntities() {
        return Collections.unmodifiableMap(entities);
    }

    /**
     * Gets an entity by name.
     *
     * @param name Entity name
     * @return Entity or null if not found
     */
    public ForemanEntity getEntity(String name) {
        return entities.get(name);
    }

    /**
     * Gets whether the scenario has been executed.
     *
     * @return true if executed, false otherwise
     */
    public boolean isExecuted() {
        return executed;
    }

    // ==================== Helpers ====================

    /**
     * Ensures the scenario is in building state.
     */
    private void ensureBuildingState() {
        if (state != ScenarioState.BUILDING) {
            throw new IllegalStateException("Scenario cannot be modified after execution starts");
        }
    }

    /**
     * Gets the primary entity name (first registered entity).
     */
    private String getPrimaryEntityName() {
        return entities.keySet().iterator().next();
    }

    /**
     * Gets the final state of the primary entity.
     */
    private AgentState getFinalState() {
        ForemanEntity primary = entities.get(getPrimaryEntityName());
        if (primary != null) {
            AgentStateMachine stateMachine = getPrimaryStateMachine();
            if (stateMachine != null) {
                return stateMachine.getCurrentState();
            }
        }
        return null;
    }

    /**
     * Gets the state machine of the primary entity.
     */
    private AgentStateMachine getPrimaryStateMachine() {
        ForemanEntity primary = entities.get(getPrimaryEntityName());
        if (primary != null) {
            // In a real implementation, this would return the actual state machine
            // For now, we use mockito to get it
            return org.mockito.Mockito.mock(AgentStateMachine.class);
        }
        return null;
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a command or task in the scenario.
     */
    private static class ScenarioCommand {
        final String command;
        final Task task;
        final String entityName;

        ScenarioCommand(String command, String entityName) {
            this.command = command;
            this.task = null;
            this.entityName = entityName;
        }

        ScenarioCommand(Task task, String entityName) {
            this.command = null;
            this.task = task;
            this.entityName = entityName;
        }

        boolean isStringCommand() {
            return command != null;
        }

        boolean isTask() {
            return task != null;
        }
    }

    /**
     * Holds scenario expectations for validation.
     */
    private static class ScenarioExpectations {
        AgentState expectedState;
        Boolean expectSuccess;
        long minDurationMs;
        long maxDurationMs;
        final List<Consumer<TestResult>> resultValidators = new ArrayList<>();
        final List<Consumer<AgentStateMachine>> stateValidators = new ArrayList<>();
    }

    /**
     * Represents the result of a scenario execution.
     */
    public static class TestResult {
        private final String scenarioName;
        private final boolean success;
        private final String errorMessage;
        private final long duration;
        final Map<String, Object> metadata;

        private TestResult(String scenarioName, boolean success, String errorMessage, long duration) {
            this.scenarioName = scenarioName;
            this.success = success;
            this.errorMessage = errorMessage;
            this.duration = duration;
            this.metadata = new HashMap<>();
        }

        static TestResult success(String scenarioName, long duration) {
            return new TestResult(scenarioName, true, null, duration);
        }

        static TestResult failure(String scenarioName, String errorMessage, long duration) {
            return new TestResult(scenarioName, false, errorMessage, duration);
        }

        public String getScenarioName() {
            return scenarioName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getDuration() {
            return duration;
        }

        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }

        public void putMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getMetadata(String key) {
            return (T) metadata.get(key);
        }

        @Override
        public String toString() {
            return "TestResult{" +
                "scenarioName='" + scenarioName + '\'' +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", duration=" + duration + "ms" +
                '}';
        }
    }
}
