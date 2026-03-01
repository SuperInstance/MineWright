package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.event.EventBus;
import com.minewright.event.SimpleEventBus;
import com.minewright.execution.AgentStateMachine;
import com.minewright.memory.ForemanMemory;
import com.minewright.memory.CompanionMemory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Comprehensive integration test framework for MineWright Minecraft mod.
 *
 * <p><b>Overview:</b></p>
 * <p>This framework provides a complete testing environment for multi-agent coordination
 * scenarios without requiring an actual Minecraft server. It manages entity lifecycle,
 * world state, test scenarios, and provides utilities for setup/teardown.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Mock Minecraft server and world management</li>
 *   <li>Entity factory for creating test entities (ForemanEntity, workers)</li>
 *   <li>World state management and manipulation helpers</li>
 *   <li>Test scenario builders with fluent API</li>
 *   <li>Setup/teardown utilities for clean test isolation</li>
 *   <li>Async operation timeout handling</li>
 *   <li>Event bus integration for testing coordination</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * IntegrationTestFramework framework = new IntegrationTestFramework();
 * framework.initialize();
 *
 * try {
 *     TestEntityFactory factory = framework.getEntityFactory();
 *     ForemanEntity foreman = factory.createTestForeman("Steve");
 *
 *     TestScenarioBuilder scenario = framework.createScenario("Mining Test");
 *     scenario.withForeman(foreman)
 *             .withCommand("mine 10 stone")
 *             .expectResult(TaskResult.success())
 *             .execute();
 * } finally {
 *     framework.cleanup();
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Framework methods are thread-safe for concurrent test execution. Each test instance
 * maintains isolated state through ConcurrentHashMap-based entity tracking.</p>
 *
 * @see MockMinecraftServer
 * @see TestEntityFactory
 * @see TestScenarioBuilder
 * @see IntegrationTestBase
 * @since 1.0.0
 */
public class IntegrationTestFramework {

    /**
     * Mock Minecraft server for testing.
     */
    private final MockMinecraftServer mockServer;

    /**
     * Entity factory for creating test entities.
     */
    private final TestEntityFactory entityFactory;

    /**
     * Test scenario builder factory.
     */
    private final Supplier<TestScenarioBuilder> scenarioBuilderFactory;

    /**
     * Event bus for testing coordination events.
     */
    private final EventBus eventBus;

    /**
     * Registry of active test entities by name.
     */
    private final Map<String, ForemanEntity> activeEntities;

    /**
     * Registry of test scenarios by name.
     */
    private final Map<String, TestScenarioBuilder> activeScenarios;

    /**
     * World state manager for test world manipulation.
     */
    private final TestWorldStateManager worldStateManager;

    /**
     * Whether the framework has been initialized.
     */
    private volatile boolean initialized = false;

    /**
     * Framework configuration options.
     */
    private final FrameworkConfiguration config;

    /**
     * Creates a new integration test framework with default configuration.
     */
    public IntegrationTestFramework() {
        this(new FrameworkConfiguration.Builder().build());
    }

    /**
     * Creates a new integration test framework with custom configuration.
     *
     * @param config Framework configuration options
     */
    public IntegrationTestFramework(FrameworkConfiguration config) {
        this.config = config;
        this.mockServer = new MockMinecraftServer(config);
        this.entityFactory = new TestEntityFactory(mockServer);
        this.eventBus = new SimpleEventBus();
        this.activeEntities = new ConcurrentHashMap<>();
        this.activeScenarios = new ConcurrentHashMap<>();
        this.worldStateManager = new TestWorldStateManager(mockServer);
        this.scenarioBuilderFactory = () -> new TestScenarioBuilder(
            this, entityFactory, eventBus, worldStateManager
        );
    }

    /**
     * Initializes the test framework, setting up the mock server and test environment.
     *
     * <p>This method must be called before using any other framework methods.</p>
     *
     * @throws IllegalStateException if framework is already initialized
     */
    public void initialize() {
        synchronized (this) {
            if (initialized) {
                throw new IllegalStateException("Framework already initialized");
            }
            mockServer.start();
            initialized = true;
        }
    }

    /**
     * Cleans up the test framework, releasing all resources and clearing state.
     *
     * <p>This method should be called in test teardown (e.g., @AfterEach).</p>
     */
    public void cleanup() {
        synchronized (this) {
            if (!initialized) {
                return;
            }

            // Stop all active scenarios
            activeScenarios.values().forEach(TestScenarioBuilder::cleanup);
            activeScenarios.clear();

            // Remove all active entities
            activeEntities.clear();

            // Reset world state
            worldStateManager.reset();

            // Stop mock server
            mockServer.stop();

            initialized = false;
        }
    }

    /**
     * Returns whether the framework has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets the mock Minecraft server.
     *
     * @return Mock server instance
     * @throws IllegalStateException if framework not initialized
     */
    public MockMinecraftServer getMockServer() {
        ensureInitialized();
        return mockServer;
    }

    /**
     * Gets the entity factory for creating test entities.
     *
     * @return Entity factory instance
     * @throws IllegalStateException if framework not initialized
     */
    public TestEntityFactory getEntityFactory() {
        ensureInitialized();
        return entityFactory;
    }

    /**
     * Gets the event bus for testing coordination events.
     *
     * @return Event bus instance
     * @throws IllegalStateException if framework not initialized
     */
    public EventBus getEventBus() {
        ensureInitialized();
        return eventBus;
    }

    /**
     * Gets the world state manager for test world manipulation.
     *
     * @return World state manager instance
     * @throws IllegalStateException if framework not initialized
     */
    public TestWorldStateManager getWorldStateManager() {
        ensureInitialized();
        return worldStateManager;
    }

    /**
     * Creates a new test scenario builder with the given name.
     *
     * @param scenarioName Name of the scenario
     * @return New scenario builder instance
     * @throws IllegalStateException if framework not initialized
     */
    public TestScenarioBuilder createScenario(String scenarioName) {
        ensureInitialized();
        TestScenarioBuilder scenario = scenarioBuilderFactory.get();
        scenario.withName(scenarioName);
        activeScenarios.put(scenarioName, scenario);
        return scenario;
    }

    /**
     * Registers an entity with the framework for tracking.
     *
     * @param name Entity name
     * @param entity Entity instance
     */
    public void registerEntity(String name, ForemanEntity entity) {
        activeEntities.put(name, entity);
    }

    /**
     * Unregisters an entity from the framework.
     *
     * @param name Entity name
     */
    public void unregisterEntity(String name) {
        activeEntities.remove(name);
    }

    /**
     * Gets a registered entity by name.
     *
     * @param name Entity name
     * @return Entity instance or null if not found
     */
    @Nullable
    public ForemanEntity getEntity(String name) {
        return activeEntities.get(name);
    }

    /**
     * Gets all registered entities.
     *
     * @return Unmodifiable map of entity names to entities
     */
    public Map<String, ForemanEntity> getAllEntities() {
        return Collections.unmodifiableMap(activeEntities);
    }

    /**
     * Simulates a game tick for all active entities.
     *
     * <p>This advances the mock server's tick counter and calls tick() on all
     * registered entities.</p>
     */
    public void simulateTick() {
        ensureInitialized();
        mockServer.tick();
        activeEntities.values().forEach(entity -> {
            if (entity.isAlive()) {
                entity.tick();
            }
        });
    }

    /**
     * Simulates multiple game ticks.
     *
     * @param tickCount Number of ticks to simulate
     */
    public void simulateTicks(int tickCount) {
        for (int i = 0; i < tickCount; i++) {
            simulateTick();
        }
    }

    /**
     * Waits for async operations to complete with timeout.
     *
     * @param condition Condition to wait for
     * @param timeoutMs Timeout in milliseconds
     * @return true if condition was met, false if timeout
     */
    public boolean waitFor(Supplier<Boolean> condition, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        long checkInterval = config.getAsyncCheckInterval();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.get()) {
                return true;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Gets the framework configuration.
     *
     * @return Configuration instance
     */
    public FrameworkConfiguration getConfig() {
        return config;
    }

    /**
     * Ensures the framework is initialized before use.
     *
     * @throws IllegalStateException if framework not initialized
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Framework not initialized. Call initialize() first.");
        }
    }

    /**
     * Framework configuration options.
     */
    public static class FrameworkConfiguration {
        private final long defaultTimeout;
        private final long asyncCheckInterval;
        private final boolean enableVerboseLogging;
        private final int maxEntities;
        private final int maxScenarios;

        private FrameworkConfiguration(Builder builder) {
            this.defaultTimeout = builder.defaultTimeout;
            this.asyncCheckInterval = builder.asyncCheckInterval;
            this.enableVerboseLogging = builder.enableVerboseLogging;
            this.maxEntities = builder.maxEntities;
            this.maxScenarios = builder.maxScenarios;
        }

        public long getDefaultTimeout() {
            return defaultTimeout;
        }

        public long getAsyncCheckInterval() {
            return asyncCheckInterval;
        }

        public boolean isVerboseLoggingEnabled() {
            return enableVerboseLogging;
        }

        public int getMaxEntities() {
            return maxEntities;
        }

        public int getMaxScenarios() {
            return maxScenarios;
        }

        /**
         * Builder for framework configuration.
         */
        public static class Builder {
            private long defaultTimeout = 30000; // 30 seconds
            private long asyncCheckInterval = 50; // 50ms
            private boolean enableVerboseLogging = false;
            private int maxEntities = 100;
            private int maxScenarios = 50;

            public Builder withDefaultTimeout(long timeoutMs) {
                this.defaultTimeout = timeoutMs;
                return this;
            }

            public Builder withAsyncCheckInterval(long intervalMs) {
                this.asyncCheckInterval = intervalMs;
                return this;
            }

            public Builder withVerboseLogging(boolean enabled) {
                this.enableVerboseLogging = enabled;
                return this;
            }

            public Builder withMaxEntities(int max) {
                this.maxEntities = max;
                return this;
            }

            public Builder withMaxScenarios(int max) {
                this.maxScenarios = max;
                return this;
            }

            public FrameworkConfiguration build() {
                return new FrameworkConfiguration(this);
            }
        }
    }

    /**
     * Test world state manager for manipulating test world state.
     */
    public static class TestWorldStateManager {
        private final MockMinecraftServer server;
        private final Map<String, Object> worldState;

        public TestWorldStateManager(MockMinecraftServer server) {
            this.server = server;
            this.worldState = new ConcurrentHashMap<>();
        }

        /**
         * Sets a world state value.
         */
        public void setState(String key, Object value) {
            worldState.put(key, value);
        }

        /**
         * Gets a world state value.
         */
        @SuppressWarnings("unchecked")
        public <T> T getState(String key) {
            return (T) worldState.get(key);
        }

        /**
         * Checks if a world state value exists.
         */
        public boolean hasState(String key) {
            return worldState.containsKey(key);
        }

        /**
         * Removes a world state value.
         */
        public void removeState(String key) {
            worldState.remove(key);
        }

        /**
         * Clears all world state.
         */
        public void reset() {
            worldState.clear();
        }

        /**
         * Gets all world state keys.
         */
        public Set<String> getStateKeys() {
            return Collections.unmodifiableSet(worldState.keySet());
        }
    }
}
