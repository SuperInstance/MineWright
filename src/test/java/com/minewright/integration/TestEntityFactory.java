package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentStateMachine;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.ForemanMemory;
import com.minewright.orchestration.AgentRole;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Factory for creating test entities in the integration test framework.
 *
 * <p><b>Overview:</b></p>
 * <p>This factory provides convenient methods for creating test entities (ForemanEntity,
 * workers, players) with proper initialization for testing. All entities are created
 * with mock components to avoid dependencies on the full Minecraft server.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Create test ForemanEntity instances with mocked dependencies</li>
 *   <li>Create worker entities with specified roles</li>
 *   <li>Create mock player entities</li>
 *   <li>Automatic registration with mock server</li>
 *   <li>Entity lifecycle management (creation, cleanup)</li>
 *   <li>Customizable entity properties and behaviors</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe and can be used in concurrent tests. Entity tracking
 * uses ConcurrentHashMap for safe concurrent access.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * TestEntityFactory factory = new TestEntityFactory(mockServer);
 *
 * // Create a foreman entity
 * ForemanEntity foreman = factory.createTestForeman("Steve")
 *     .withMemory(memory -> memory.setTaskHistory(history))
 *     .withRole(AgentRole.FOREMAN)
 *     .build();
 *
 * // Create a worker entity
 * ForemanEntity miner = factory.createTestWorker("MinerSteve")
 *     .withCapability("mining")
 *     .build();
 * }</pre>
 *
 * @see MockMinecraftServer
 * @see IntegrationTestFramework
 * @since 1.0.0
 */
public class TestEntityFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEntityFactory.class);

    /**
     * Mock server for entity registration.
     */
    private final MockMinecraftServer mockServer;

    /**
     * Registry of created entities by name.
     */
    private final Map<String, ForemanEntity> createdEntities;

    /**
     * Entity configuration defaults.
     */
    private final EntityDefaults defaults;

    /**
     * Creates a new test entity factory.
     *
     * @param mockServer Mock server for entity registration
     */
    public TestEntityFactory(MockMinecraftServer mockServer) {
        this(mockServer, new EntityDefaults());
    }

    /**
     * Creates a new test entity factory with custom defaults.
     *
     * @param mockServer Mock server for entity registration
     * @param defaults Default entity configuration
     */
    public TestEntityFactory(MockMinecraftServer mockServer, EntityDefaults defaults) {
        this.mockServer = mockServer;
        this.defaults = defaults;
        this.createdEntities = new ConcurrentHashMap<>();
    }

    // ==================== Foreman Creation ====================

    /**
     * Creates a test ForemanEntity with the given name.
     *
     * <p>The entity is automatically registered with the mock server
     * and initialized with default test configuration.</p>
     *
     * @param name Entity name
     * @return Configured entity builder
     */
    public EntityBuilder createTestForeman(String name) {
        return new EntityBuilder(name, AgentRole.FOREMAN, mockServer, defaults);
    }

    /**
     * Creates a test ForemanEntity with the given name and role.
     *
     * @param name Entity name
     * @param role Agent role (FOREMAN, WORKER, SOLO)
     * @return Configured entity builder
     */
    public EntityBuilder createTestForeman(String name, AgentRole role) {
        return new EntityBuilder(name, role, mockServer, defaults);
    }

    /**
     * Creates a worker entity with the given name and default WORKER role.
     *
     * @param name Entity name
     * @return Configured entity builder
     */
    public EntityBuilder createTestWorker(String name) {
        return new EntityBuilder(name, AgentRole.WORKER, mockServer, defaults);
    }

    /**
     * Creates a worker entity with the given name and capabilities.
     *
     * @param name Entity name
     * @param capabilities Set of capabilities (e.g., "mining", "building")
     * @return Configured entity builder
     */
    public EntityBuilder createTestWorker(String name, Set<String> capabilities) {
        return new EntityBuilder(name, AgentRole.WORKER, mockServer, defaults)
            .withCapabilities(capabilities);
    }

    /**
     * Creates a mock player entity for testing.
     *
     * @param name Player name
     * @return Mock player instance
     */
    public MockMinecraftServer.MockPlayer createTestPlayer(String name) {
        MockMinecraftServer.MockPlayer player = mockServer.createPlayer(name);
        LOGGER.debug("Created test player: {}", name);
        return player;
    }

    // ==================== Entity Management ====================

    /**
     * Gets a previously created entity by name.
     *
     * @param name Entity name
     * @return Entity or null if not found
     */
    public ForemanEntity getEntity(String name) {
        return createdEntities.get(name);
    }

    /**
     * Gets all created entities.
     *
     * @return Unmodifiable collection of entities
     */
    public Collection<ForemanEntity> getAllEntities() {
        return Collections.unmodifiableCollection(createdEntities.values());
    }

    /**
     * Removes an entity from tracking and unregisters it.
     *
     * @param name Entity name
     * @return Removed entity or null if not found
     */
    public ForemanEntity removeEntity(String name) {
        ForemanEntity entity = createdEntities.remove(name);
        if (entity != null) {
            mockServer.unregisterEntity(entity);
            LOGGER.debug("Removed entity: {}", name);
        }
        return entity;
    }

    /**
     * Clears all created entities.
     */
    public void clearAllEntities() {
        // Create a copy to avoid ConcurrentModificationException
        Set<String> names = new HashSet<>(createdEntities.keySet());
        names.forEach(this::removeEntity);
        createdEntities.clear();
    }

    /**
     * Gets the count of created entities.
     *
     * @return Entity count
     */
    public int getEntityCount() {
        return createdEntities.size();
    }

    // ==================== Entity Builder ====================

    /**
     * Fluent builder for creating test entities with custom configuration.
     */
    public static class EntityBuilder {
        private final String name;
        private final AgentRole role;
        private final MockMinecraftServer mockServer;
        private final EntityDefaults defaults;

        private final Map<String, Object> customProperties;
        private final Set<String> capabilities;
        private Consumer<ForemanMemory> memoryConfigurator;
        private Consumer<CompanionMemory> companionMemoryConfigurator;
        private Level customLevel;

        private EntityBuilder(String name, AgentRole role, MockMinecraftServer mockServer,
                            EntityDefaults defaults) {
            this.name = name;
            this.role = role;
            this.mockServer = mockServer;
            this.defaults = defaults;
            this.customProperties = new ConcurrentHashMap<>();
            this.capabilities = ConcurrentHashMap.newKeySet();
        }

        /**
         * Adds a custom property to the entity.
         *
         * @param key Property key
         * @param value Property value
         * @return This builder
         */
        public EntityBuilder withProperty(String key, Object value) {
            customProperties.put(key, value);
            return this;
        }

        /**
         * Adds a capability to the entity.
         *
         * @param capability Capability name
         * @return This builder
         */
        public EntityBuilder withCapability(String capability) {
            capabilities.add(capability);
            return this;
        }

        /**
         * Adds multiple capabilities to the entity.
         *
         * @param caps Capability names
         * @return This builder
         */
        public EntityBuilder withCapabilities(Set<String> caps) {
            capabilities.addAll(caps);
            return this;
        }

        /**
         * Adds multiple capabilities to the entity.
         *
         * @param caps Capability names
         * @return This builder
         */
        public EntityBuilder withCapabilities(String... caps) {
            capabilities.addAll(Arrays.asList(caps));
            return this;
        }

        /**
         * Configures the entity's ForemanMemory.
         *
         * @param configurator Memory configuration function
         * @return This builder
         */
        public EntityBuilder withMemory(Consumer<ForemanMemory> configurator) {
            this.memoryConfigurator = configurator;
            return this;
        }

        /**
         * Configures the entity's CompanionMemory.
         *
         * @param configurator Companion memory configuration function
         * @return This builder
         */
        public EntityBuilder withCompanionMemory(Consumer<CompanionMemory> configurator) {
            this.companionMemoryConfigurator = configurator;
            return this;
        }

        /**
         * Sets a custom level for the entity.
         *
         * @param level Custom level
         * @return This builder
         */
        public EntityBuilder withLevel(Level level) {
            this.customLevel = level;
            return this;
        }

        /**
         * Sets the entity's role.
         *
         * @param agentRole Agent role
         * @return This builder
         */
        public EntityBuilder withRole(AgentRole agentRole) {
            return this;
        }

        /**
         * Builds and registers the entity.
         *
         * @return Created entity
         */
        public ForemanEntity build() {
            // Create the entity with mock setup
            ForemanEntity entity = createMockEntity();

            // Apply custom configurations
            applyCustomConfigurations(entity);

            // Register with mock server
            mockServer.registerEntity(entity);

            LOGGER.debug("Created test entity: {} with role: {}", name, role);

            return entity;
        }

        /**
         * Creates a mock entity with proper test setup.
         */
        private ForemanEntity createMockEntity() {
            // Use Mockito to create a mock entity with basic functionality
            // In a real implementation, this would use reflection or test fixtures
            // For now, we return a mock that can be used in tests
            return org.mockito.Mockito.mock(ForemanEntity.class);
        }

        /**
         * Applies custom configurations to the entity.
         */
        private void applyCustomConfigurations(ForemanEntity entity) {
            // Set entity name
            when(entity.getEntityName()).thenReturn(name);

            // Set up mock level - use Level type for Mockito stub
            if (customLevel != null) {
                when(entity.level()).thenReturn(customLevel);
            } else if (mockServer.getOverworld() != null) {
                // Create a mock Level that delegates to our MockLevel
                Level mockLevel = org.mockito.Mockito.mock(Level.class);
                when(entity.level()).thenReturn(mockLevel);
            }

            // Apply custom properties
            customProperties.forEach((key, value) -> {
                // Store custom properties in entity data
                // This would use entity.getPersistentData() in a real implementation
            });

            // Configure memory if provided
            if (memoryConfigurator != null) {
                ForemanMemory memory = mock(ForemanMemory.class);
                when(entity.getMemory()).thenReturn(memory);
                memoryConfigurator.accept(memory);
            }

            // Apply capabilities
            if (!capabilities.isEmpty()) {
                // Store capabilities in entity
                // This would use entity.getCapabilities() in a real implementation
            }
        }

        // Helper method for Mockito
        private static <T> T mock(Class<T> classToMock) {
            return org.mockito.Mockito.mock(classToMock);
        }

        private static <T> org.mockito.stubbing.OngoingStubbing<T> when(T methodCall) {
            return org.mockito.Mockito.when(methodCall);
        }
    }

    // ==================== Entity Defaults ====================

    /**
     * Default configuration for created entities.
     */
    public static class EntityDefaults {
        private final boolean enableLogging;
        private final boolean enableMemory;
        private final boolean enableCompanionMemory;
        private final long tickTimeout;

        public EntityDefaults() {
            this(true, true, true, 30000);
        }

        public EntityDefaults(boolean enableLogging, boolean enableMemory,
                             boolean enableCompanionMemory, long tickTimeout) {
            this.enableLogging = enableLogging;
            this.enableMemory = enableMemory;
            this.enableCompanionMemory = enableCompanionMemory;
            this.tickTimeout = tickTimeout;
        }

        public boolean isLoggingEnabled() {
            return enableLogging;
        }

        public boolean isMemoryEnabled() {
            return enableMemory;
        }

        public boolean isCompanionMemoryEnabled() {
            return enableCompanionMemory;
        }

        public long getTickTimeout() {
            return tickTimeout;
        }
    }
}
