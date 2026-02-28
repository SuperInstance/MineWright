package com.minewright.blackboard;

/**
 * Enumeration of partitioned knowledge areas in the blackboard system.
 *
 * <p><b>Purpose:</b></p>
 * <p>KnowledgeArea provides logical separation of different types of information,
 * allowing agents to efficiently query relevant data and subscribe to specific
 * topics of interest. This partitioning reduces noise and improves coordination
 * by enabling targeted knowledge sharing.</p>
 *
 * <p><b>Partitioning Strategy:</b></p>
 * <p>Each area represents a distinct domain of knowledge with its own update
 * frequency, relevance to different agent types, and staleness characteristics.</p>
 *
 * <p><b>Usage Patterns:</b></p>
 * <ul>
 *   <li><b>Querying:</b> Agents query specific areas to avoid filtering irrelevant data</li>
 *   <li><b>Subscriptions:</b> Agents subscribe to areas they need to monitor</li>
 *   <li><b>Eviction:</b> Different areas have different staleness tolerances</li>
 *   <li><b>Prioritization:</b> High-frequency areas (like THREATS) get priority updates</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Post a block observation to world state
 * blackboard.post(KnowledgeArea.WORLD_STATE, "block_100_64_200", blockData, agentId);
 *
 * // Query for agent positions
 * List<BlackboardEntry<Position>> positions = blackboard.queryArea(KnowledgeArea.AGENT_STATUS);
 *
 * // Subscribe to threat updates
 * blackboard.subscribe(KnowledgeArea.THREATS, this::onThreatDetected);
 * }</pre>
 *
 * @see Blackboard
 * @see BlackboardEntry
 * @since 1.0.0
 */
public enum KnowledgeArea {
    /**
     * World state information including blocks, entities, biomes, and environmental data.
     *
     * <p><b>Update Frequency:</b> High (every tick for active agents)</p>
     * <p><b>Staleness Tolerance:</b> Low (world changes constantly)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"block_100_64_200" -> BlockState data</li>
     *   <li>"entity_zombie_001" -> Entity position/health</li>
     *   <li>"biome_forest" -> Biome characteristics</li>
     * </ul>
     * <p><b>Subscribers:</b> All agents (situational awareness)</p>
     */
    WORLD_STATE("world_state", 5000L),

    /**
     * Agent status information including health, position, inventory, and activity state.
     *
     * <p><b>Update Frequency:</b> Medium (on state change)</p>
     * <p><b>Staleness Tolerance:</b> Low (agent status affects coordination)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"agent_steve_001" -> Position, health, current task</li>
     *   <li>"agent_steve_002" -> Inventory contents, equipment</li>
     *   <li>"agent_steve_001_state" -> IDLE, PLANNING, EXECUTING, etc.</li>
     * </ul>
     * <p><b>Subscribers:</b> All agents (coordination), orchestration system</p>
     */
    AGENT_STATUS("agent_status", 2000L),

    /**
     * Task information including active, pending, and completed tasks.
     *
     * <p><b>Update Frequency:</b> Medium (on task state change)</p>
     * <p><b>Staleness Tolerance:</b> Medium (task queues change slowly)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"task_mine_001" -> Task status, assigned agent, progress</li>
     *   <li>"task_build_002" -> Structure progress, required materials</li>
     *   <li>"task_queue_size" -> Number of pending tasks</li>
     * </ul>
     * <p><b>Subscribers:</b> Foreman agents (task distribution), orchestration</p>
     */
    TASKS("tasks", 10000L),

    /**
     * Resource information including available materials, storage locations, and stockpile levels.
     *
     * <p><b>Update Frequency:</b> Low (on inventory change)</p>
     * <p><b>Staleness Tolerance:</b> Medium (resource counts change slowly)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"resource_diamond" -> Available count, location</li>
     *   <li>"storage_main" -> Chest contents, position</li>
     *   <li>"resource_iron" -> Current stockpile, predicted needs</li>
     * </ul>
     * <p><b>Subscribers:</b> All agents (resource planning), foreman (allocation)</p>
     */
    RESOURCES("resources", 15000L),

    /**
     * Threat information including hostile mobs, environmental dangers, and security alerts.
     *
     * <p><b>Update Frequency:</b> High (immediate on detection)</p>
     * <p><b>Staleness Tolerance:</b> Very low (threats require immediate response)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"hostile_creeper_001" -> Position, distance, path to base</li>
     *   <li>"danger_lava" -> Area affected, safe routes</li>
     *   <li>"threat_level" -> Current security status</li>
     * </ul>
     * <p><b>Subscribers:</b> All agents (survival), combat-specialized agents</p>
     */
    THREATS("threats", 1000L),

    /**
     * Build plan information including structure templates, build progress, and construction goals.
     *
     * <p><b>Update Frequency:</b> Medium (on build progress)</p>
     * <p><b>Staleness Tolerance:</b> Low (build plans must stay synchronized)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"build_house_001" -> Template ID, progress %, assigned agents</li>
     *   <li>"section_0_0" -> Build status, blocks placed, remaining</li>
     *   <li>"build_queue" -> Pending structures, priority</li>
     * </ul>
     * <p><b>Subscribers:</b> Builder agents, foreman (coordination), collaborative build manager</p>
     */
    BUILD_PLANS("build_plans", 3000L),

    /**
     * Player preferences and interaction history.
     *
     * <p><b>Update Frequency:</b> Low (on player action)</p>
     * <p><b>Staleness Tolerance:</b> High (preferences change rarely)</p>
     * <p><b>Example Entries:</b></p>
     * <ul>
     *   <li>"pref_build_style" -> Modern, medieval, etc.</li>
     *   <li>"command_history" -> Recent player commands for context</li>
     *   <li>"pref_voice" -> Voice enabled/disabled</li>
     * </ul>
     * <p><b>Subscribers:</b> Foreman agents (command interpretation), task planner</p>
     */
    PLAYER_PREFS("player_prefs", 60000L);

    /**
     * Unique identifier for this knowledge area.
     * Used as a namespace prefix for entry keys.
     */
    private final String id;

    /**
     * Default maximum age for entries in this area before they are considered stale.
     * Areas with rapidly changing data have shorter timeouts.
     */
    private final long defaultMaxAgeMs;

    /**
     * Creates a knowledge area with the specified ID and default staleness timeout.
     *
     * @param id Unique identifier for this area
     * @param defaultMaxAgeMs Default maximum age in milliseconds before entries are stale
     */
    KnowledgeArea(String id, long defaultMaxAgeMs) {
        this.id = id;
        this.defaultMaxAgeMs = defaultMaxAgeMs;
    }

    /**
     * Gets the unique identifier for this knowledge area.
     *
     * @return Area ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the default maximum age for entries in this area.
     *
     * @return Maximum age in milliseconds
     */
    public long getDefaultMaxAgeMs() {
        return defaultMaxAgeMs;
    }

    /**
     * Creates a fully qualified key for this knowledge area.
     *
     * <p>Combines the area ID with the entry key to create a namespace-unique identifier.
     * Example: WORLD_STATE.qualify("block_100") -> "world_state:block_100"</p>
     *
     * @param key Entry key within this area
     * @return Fully qualified key string
     */
    public String qualify(String key) {
        return id + ":" + key;
    }

    /**
     * Extracts the local key from a fully qualified key.
     *
     * @param qualifiedKey Fully qualified key (e.g., "world_state:block_100")
     * @return Local key (e.g., "block_100"), or null if not from this area
     */
    public String unqualify(String qualifiedKey) {
        if (qualifiedKey != null && qualifiedKey.startsWith(id + ":")) {
            return qualifiedKey.substring(id.length() + 1);
        }
        return null;
    }
}
