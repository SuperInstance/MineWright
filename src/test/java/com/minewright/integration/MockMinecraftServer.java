package com.minewright.integration;

import com.minewright.entity.ForemanEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock Minecraft server implementation for integration testing.
 *
 * <p><b>Overview:</b></p>
 * <p>This class provides a lightweight mock of the Minecraft server, allowing tests
 * to run without the full Minecraft server infrastructure. It manages server lifecycle,
 * player management, world management, and tick simulation.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Server lifecycle management (start, stop, tick simulation)</li>
 *   <li>Player registry for mock players</li>
 *   <li>World management with test levels</li>
 *   <li>Tick simulation with controllable tick rate</li>
 *   <li>Entity registry for tracking test entities</li>
 *   <li>Configuration management for test settings</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe and can be used in concurrent tests. All state
 * management uses ConcurrentHashMap and atomic primitives.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * MockMinecraftServer server = new MockMinecraftServer();
 * server.start();
 *
 * try {
 *     server.tick(); // Simulate one game tick
 *
 *     ForemanEntity entity = server.getEntity("test-foreman");
 *     entity.tick(); // Tick the entity
 * } finally {
 *     server.stop();
 * }
 * }</pre>
 *
 * @see IntegrationTestFramework
 * @see TestEntityFactory
 * @since 1.0.0
 */
public class MockMinecraftServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMinecraftServer.class);

    /**
     * Server configuration options.
     */
    private final IntegrationTestFramework.FrameworkConfiguration config;

    /**
     * Whether the server is running.
     */
    private final AtomicBoolean running;

    /**
     * Current tick counter.
     */
    private final AtomicLong tickCounter;

    /**
     * Registry of active players by name.
     */
    private final Map<String, MockPlayer> players;

    /**
     * Registry of active entities by UUID.
     */
    private final Map<UUID, ForemanEntity> entities;

    /**
     * Registry of test worlds by dimension ID.
     */
    private final Map<String, MockLevel> worlds;

    /**
     * Server properties/configuration.
     */
    private final Map<String, Object> properties;

    /**
     * Server start time for tracking uptime.
     */
    private volatile long startTime;

    /**
     * Creates a new mock Minecraft server with default configuration.
     */
    public MockMinecraftServer() {
        this(new IntegrationTestFramework.FrameworkConfiguration.Builder().build());
    }

    /**
     * Creates a new mock Minecraft server with custom configuration.
     *
     * @param config Framework configuration
     */
    public MockMinecraftServer(IntegrationTestFramework.FrameworkConfiguration config) {
        this.config = config;
        this.running = new AtomicBoolean(false);
        this.tickCounter = new AtomicLong(0);
        this.players = new ConcurrentHashMap<>();
        this.entities = new ConcurrentHashMap<>();
        this.worlds = new ConcurrentHashMap<>();
        this.properties = new ConcurrentHashMap<>();
    }

    /**
     * Starts the mock server, initializing all subsystems.
     *
     * <p>This method initializes the server state, creates default worlds,
     * and prepares the server for tick simulation.</p>
     *
     * @throws IllegalStateException if server is already running
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            startTime = System.currentTimeMillis();
            tickCounter.set(0);

            // Create default overworld
            createWorld("overworld", 0);

            LOGGER.debug("MockMinecraftServer started");
        } else {
            throw new IllegalStateException("Server is already running");
        }
    }

    /**
     * Stops the mock server, releasing all resources.
     *
     * <p>This method clears all registries and resets server state.
     * Subsequent calls to start() will reinitialize the server.</p>
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // Clear all registries
            players.clear();
            entities.clear();
            worlds.clear();
            properties.clear();

            LOGGER.debug("MockMinecraftServer stopped");
        }
    }

    /**
     * Simulates a single server tick.
     *
     * <p>This advances the tick counter and processes any pending
     * server-level operations.</p>
     *
     * @throws IllegalStateException if server is not running
     */
    public void tick() {
        if (!running.get()) {
            throw new IllegalStateException("Server is not running");
        }

        long tick = tickCounter.incrementAndGet();

        if (config.isVerboseLoggingEnabled()) {
            LOGGER.debug("Server tick: {}", tick);
        }

        // Process world ticks
        worlds.values().forEach(world -> world.tick(tick));
    }

    /**
     * Simulates multiple server ticks.
     *
     * @param count Number of ticks to simulate
     */
    public void tick(int count) {
        for (int i = 0; i < count; i++) {
            tick();
        }
    }

    /**
     * Returns whether the server is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Gets the current tick counter value.
     *
     * @return Current tick number
     */
    public long getTickCounter() {
        return tickCounter.get();
    }

    /**
     * Gets the server uptime in milliseconds.
     *
     * @return Uptime in milliseconds, or 0 if server not running
     */
    public long getUptime() {
        if (!running.get()) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    // ==================== Player Management ====================

    /**
     * Creates and registers a mock player.
     *
     * @param name Player name
     * @return Mock player instance
     */
    public MockPlayer createPlayer(String name) {
        MockPlayer player = new MockPlayer(name);
        players.put(name, player);
        return player;
    }

    /**
     * Gets a registered player by name.
     *
     * @param name Player name
     * @return Mock player or null if not found
     */
    public MockPlayer getPlayer(String name) {
        return players.get(name);
    }

    /**
     * Removes a player from the registry.
     *
     * @param name Player name
     * @return Removed player or null if not found
     */
    public MockPlayer removePlayer(String name) {
        return players.remove(name);
    }

    /**
     * Gets all registered players.
     *
     * @return Unmodifiable collection of players
     */
    public Collection<MockPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /**
     * Gets the count of registered players.
     *
     * @return Player count
     */
    public int getPlayerCount() {
        return players.size();
    }

    // ==================== Entity Management ====================

    /**
     * Registers an entity with the server.
     *
     * @param entity Entity to register
     */
    public void registerEntity(ForemanEntity entity) {
        entities.put(entity.getUUID(), entity);
    }

    /**
     * Unregisters an entity from the server.
     *
     * @param entity Entity to unregister
     */
    public void unregisterEntity(ForemanEntity entity) {
        entities.remove(entity.getUUID());
    }

    /**
     * Gets an entity by UUID.
     *
     * @param uuid Entity UUID
     * @return Entity or null if not found
     */
    public ForemanEntity getEntity(UUID uuid) {
        return entities.get(uuid);
    }

    /**
     * Gets all registered entities.
     *
     * @return Unmodifiable collection of entities
     */
    public Collection<ForemanEntity> getEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    /**
     * Gets the count of registered entities.
     *
     * @return Entity count
     */
    public int getEntityCount() {
        return entities.size();
    }

    // ==================== World Management ====================

    /**
     * Creates a new test world.
     *
     * @param name World name (e.g., "overworld", "nether", "end")
     * @param dimensionId Dimension ID
     * @return Mock level instance
     */
    public MockLevel createWorld(String name, int dimensionId) {
        MockLevel level = new MockLevel(name, dimensionId);
        worlds.put(name, level);
        return level;
    }

    /**
     * Gets a world by name.
     *
     * @param name World name
     * @return Mock level or null if not found
     */
    public MockLevel getWorld(String name) {
        return worlds.get(name);
    }

    /**
     * Gets the default overworld.
     *
     * @return Overworld level or null if not created
     */
    public MockLevel getOverworld() {
        return getWorld("overworld");
    }

    /**
     * Gets all worlds.
     *
     * @return Unmodifiable collection of worlds
     */
    public Collection<MockLevel> getWorlds() {
        return Collections.unmodifiableCollection(worlds.values());
    }

    // ==================== Server Properties ====================

    /**
     * Sets a server property.
     *
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Gets a server property.
     *
     * @param key Property key
     * @return Property value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    /**
     * Gets a server property with default value.
     *
     * @param key Property key
     * @param defaultValue Default value if not found
     * @return Property value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Checks if a property exists.
     *
     * @param key Property key
     * @return true if exists, false otherwise
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Gets all server properties.
     *
     * @return Unmodifiable map of properties
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    // ==================== Mock Player Class ====================

    /**
     * Mock player for testing.
     */
    public static class MockPlayer {
        private final String name;
        private final UUID uuid;
        private final Map<String, Object> data;

        public MockPlayer(String name) {
            this.name = name;
            this.uuid = UUID.randomUUID();
            this.data = new ConcurrentHashMap<>();
        }

        public String getName() {
            return name;
        }

        public UUID getUUID() {
            return uuid;
        }

        public void setData(String key, Object value) {
            data.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getData(String key) {
            return (T) data.get(key);
        }

        @Override
        public String toString() {
            return "MockPlayer{name='" + name + "', uuid=" + uuid + "}";
        }
    }

    /**
     * Mock level (world) for testing.
     */
    public static class MockLevel {
        private final String name;
        private final int dimensionId;
        private final AtomicLong tickCounter;
        private final Map<String, Object> worldData;

        public MockLevel(String name, int dimensionId) {
            this.name = name;
            this.dimensionId = dimensionId;
            this.tickCounter = new AtomicLong(0);
            this.worldData = new ConcurrentHashMap<>();
        }

        public String getName() {
            return name;
        }

        public int getDimensionId() {
            return dimensionId;
        }

        public long getTickCounter() {
            return tickCounter.get();
        }

        public void tick(long tick) {
            tickCounter.set(tick);
        }

        public void setWorldData(String key, Object value) {
            worldData.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getWorldData(String key) {
            return (T) worldData.get(key);
        }

        /**
         * Returns false for test worlds (not client-side).
         */
        public boolean isClientSide() {
            return false;
        }

        @Override
        public String toString() {
            return "MockLevel{name='" + name + "', dimension=" + dimensionId + "}";
        }
    }
}
