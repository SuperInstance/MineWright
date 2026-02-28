package com.minewright.blackboard;

import com.minewright.testutil.TestLogger;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Central shared knowledge system for agent coordination and information sharing.
 *
 * <p><b>Purpose:</b></p>
 * <p>The Blackboard pattern enables multiple agents to share and access knowledge
 * without direct coupling. Agents post observations, hypotheses, goals, and constraints
 * to partitioned knowledge areas, and subscribe to updates for reactive behavior.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Partitioned Knowledge:</b> Separate areas for different types of information</li>
 *   <li><b>Typed Entries:</b> Each entry has a type (FACT, HYPOTHESIS, GOAL, CONSTRAINT)</li>
 *   <li><b>Confidence Levels:</b> Agents can specify confidence in their knowledge</li>
 *   <li><b>Subscriptions:</b> Reactive updates when relevant knowledge changes</li>
 *   <li><b>Staleness Management:</b> Automatic eviction of outdated entries</li>
 *   <li><b>Thread Safety:</b> Full concurrent access support for multi-agent systems</li>
 * </ul>
 *
 * <p><b>Architecture:</b></p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                      Blackboard                              │
 * ├─────────────────────────────────────────────────────────────┤
 * │  WORLD_STATE  │  AGENT_STATUS  │  TASKS  │  THREATS  │ ...  │
 * │  ┌─────────┐  │  ┌──────────┐  │ ┌─────┐ │ ┌────────┐       │
 * │  │block_x  │  │  │agent_001 │  │ │task1│ │ │creeper│       │
 * │  │entity_y│  │  │agent_002 │  │ │task2│ │ │zombie │       │
 * │  └─────────┘  │  └──────────┘  │ └─────┘ │ └────────┘       │
 * ├─────────────────────────────────────────────────────────────┤
 * │                    Subscribers                               │
 * │  Agent1, Agent2, Orchestration, Monitoring...               │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Get singleton instance
 * Blackboard blackboard = Blackboard.getInstance();
 *
 * // Post a block observation
 * UUID agentId = agent.getUUID();
 * BlackboardEntry<BlockState> entry = BlackboardEntry.createFact(
 *     "block_100_64_200", blockState, agentId);
 * blackboard.post(KnowledgeArea.WORLD_STATE, entry);
 *
 * // Query for specific information
 * Optional<BlockState> block = blackboard.query(
 *     KnowledgeArea.WORLD_STATE, "block_100_64_200");
 *
 * // Subscribe to threat updates
 * blackboard.subscribe(KnowledgeArea.THREATS, new BlackboardSubscriber() {
 *     public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
 *         if (entry.getKey().startsWith("hostile_")) {
 *             respondToThreat(entry.getValue());
 *         }
 *     }
 *     public void onEntryRemoved(KnowledgeArea area, String key) {}
 * });
 *
 * // Clean up old entries
 * blackboard.evictStale(60000); // Remove entries older than 1 minute
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses {@link ConcurrentHashMap} for thread-safe entry storage</li>
 *   <li>Uses {@link CopyOnWriteArrayList} for thread-safe subscriber lists</li>
 *   <li>Uses {@link ReadWriteLock} for compound operations</li>
 *   <li>Subscriber notifications are synchronized to prevent concurrent callbacks</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b></p>
 * <ul>
 *   <li>Query operations are O(1) average case</li>
 *   <li>Area queries are O(n) where n is the area's entry count</li>
 *   <li>Subscriber notifications are O(m) where m is the subscriber count</li>
 *   <li>Consider batching updates for high-frequency posting (e.g., world scanning)</li>
 * </ul>
 *
 * @see KnowledgeArea
 * @see BlackboardEntry
 * @see BlackboardSubscriber
 * @since 1.0.0
 */
public class Blackboard {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(Blackboard.class);

    /**
     * Singleton instance of the blackboard.
     * Thread-safe lazy initialization.
     */
    private static volatile Blackboard instance;

    /**
     * Partitioned knowledge storage.
     * Outer map: KnowledgeArea -> Inner map of entries
     * Inner map: Entry key -> BlackboardEntry
     * Using ConcurrentHashMap for thread-safe concurrent access.
     */
    private final Map<KnowledgeArea, Map<String, BlackboardEntry<?>>> areas;

    /**
     * Subscribers for each knowledge area.
     * Allows agents to react to relevant knowledge updates.
     * Using CopyOnWriteArrayList for thread-safe iteration during notifications.
     */
    private final Map<KnowledgeArea, List<BlackboardSubscriber>> subscribers;

    /**
     * Lock for coordinating compound operations.
     * Used to ensure consistency during multi-step operations.
     */
    private final ReadWriteLock lock;

    /**
     * Global subscribers interested in all knowledge areas.
     * Useful for monitoring, logging, and debugging.
     */
    private final List<BlackboardSubscriber> globalSubscribers;

    /**
     * Statistics about blackboard operations.
     * Volatile for visibility across threads.
     */
    private volatile long totalPosts;
    private volatile long totalQueries;
    private volatile long totalEvictions;

    /**
     * Private constructor for singleton pattern.
     * Initializes all data structures with thread-safe implementations.
     */
    private Blackboard() {
        // Initialize knowledge areas with ConcurrentHashMap for thread safety
        this.areas = new ConcurrentHashMap<>();
        for (KnowledgeArea area : KnowledgeArea.values()) {
            areas.put(area, new ConcurrentHashMap<>());
        }

        // Initialize subscriber lists with CopyOnWriteArrayList for thread-safe iteration
        this.subscribers = new ConcurrentHashMap<>();
        for (KnowledgeArea area : KnowledgeArea.values()) {
            subscribers.put(area, new CopyOnWriteArrayList<>());
        }

        this.globalSubscribers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.totalPosts = 0;
        this.totalQueries = 0;
        this.totalEvictions = 0;

        LOGGER.info("Blackboard initialized with {} knowledge areas",
            KnowledgeArea.values().length);
    }

    /**
     * Gets the singleton instance of the blackboard.
     *
     * <p>Thread-safe lazy initialization using double-checked locking.</p>
     *
     * @return The singleton Blackboard instance
     */
    public static Blackboard getInstance() {
        if (instance == null) {
            synchronized (Blackboard.class) {
                if (instance == null) {
                    instance = new Blackboard();
                }
            }
        }
        return instance;
    }

    /**
     * Posts a new entry to the blackboard.
     *
     * <p>If an entry with the same key exists in the area, it will be replaced.
     * Subscribers will be notified of the change.</p>
     *
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called
     * concurrently from multiple agents.</p>
     *
     * @param <T> The type of the entry value
     * @param area The knowledge area to post to
     * @param entry The entry to post
     */
    public <T> void post(KnowledgeArea area, BlackboardEntry<T> entry) {
        if (area == null || entry == null) {
            throw new IllegalArgumentException("Area and entry cannot be null");
        }

        lock.writeLock().lock();
        try {
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
            BlackboardEntry<?> existing = areaMap.put(entry.getKey(), entry);

            totalPosts++;

            LOGGER.debug("Posted entry to {}: {} (type: {}, confidence: {})",
                area.getId(), entry.getKey(), entry.getType(), entry.getConfidence());

            // Notify subscribers
            notifySubscribers(area, entry);

            // If replacing an existing entry, notify removal
            if (existing != null && !existing.getKey().equals(entry.getKey())) {
                notifyRemoval(area, existing.getKey());
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates and posts a new entry in a single call.
     *
     * <p>Convenience method for posting without explicitly creating a BlackboardEntry.</p>
     *
     * @param <T> The type of the value
     * @param area The knowledge area to post to
     * @param key The entry key
     * @param value The entry value
     * @param source UUID of the agent posting this entry (null for system)
     * @param confidence Confidence level (0.0 to 1.0)
     * @param type Entry type classification
     */
    public <T> void post(KnowledgeArea area, String key, T value, UUID source,
                        double confidence, BlackboardEntry.EntryType type) {
        BlackboardEntry<T> entry = new BlackboardEntry<>(key, value, source, confidence, type);
        post(area, entry);
    }

    /**
     * Queries for a specific entry in a knowledge area.
     *
     * <p>Returns an Optional containing the entry value if found, or an empty Optional otherwise.</p>
     *
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called concurrently.</p>
     *
     * @param <T> The expected type of the entry value
     * @param area The knowledge area to query
     * @param key The entry key to look up
     * @return Optional containing the value if found, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> query(KnowledgeArea area, String key) {
        if (area == null || key == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            totalQueries++;
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
            BlackboardEntry<?> entry = areaMap.get(key);

            if (entry != null) {
                return Optional.of((T) entry.getValue());
            }
            return Optional.empty();

        } catch (ClassCastException e) {
            LOGGER.warn("Type mismatch querying {} for key {}: {}",
                area.getId(), key, e.getMessage());
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Queries for the complete BlackboardEntry (including metadata).
     *
     * <p>Use this when you need access to confidence, timestamp, source agent, etc.</p>
     *
     * @param <T> The expected type of the entry value
     * @param area The knowledge area to query
     * @param key The entry key to look up
     * @return Optional containing the full entry if found, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<BlackboardEntry<T>> queryEntry(KnowledgeArea area, String key) {
        if (area == null || key == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            totalQueries++;
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
            BlackboardEntry<?> entry = areaMap.get(key);

            if (entry != null) {
                return Optional.of((BlackboardEntry<T>) entry);
            }
            return Optional.empty();

        } catch (ClassCastException e) {
            LOGGER.warn("Type mismatch querying entry {} for key {}: {}",
                area.getId(), key, e.getMessage());
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Queries all entries in a knowledge area.
     *
     * <p>Returns a list of entries with their values. Use this for scanning
     * an entire area (e.g., finding all threats, all agent positions).</p>
     *
     * <p><b>Performance:</b> O(n) where n is the number of entries in the area.</p>
     *
     * @param <T> The expected type of the entry values
     * @param area The knowledge area to query
     * @return List of all entries in the area (empty list if none)
     */
    @SuppressWarnings("unchecked")
    public <T> List<BlackboardEntry<T>> queryArea(KnowledgeArea area) {
        if (area == null) {
            return Collections.emptyList();
        }

        lock.readLock().lock();
        try {
            totalQueries++;
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);

            return areaMap.values().stream()
                .map(entry -> (BlackboardEntry<T>) entry)
                .collect(Collectors.toList());

        } catch (ClassCastException e) {
            LOGGER.warn("Type mismatch querying area {}: {}",
                area.getId(), e.getMessage());
            return Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Queries entries by type within a knowledge area.
     *
     * <p>Useful for filtering, e.g., getting only FACT entries or only GOAL entries.</p>
     *
     * @param <T> The expected type of the entry values
     * @param area The knowledge area to query
     * @param type The entry type to filter by
     * @return List of matching entries (empty list if none)
     */
    @SuppressWarnings("unchecked")
    public <T> List<BlackboardEntry<T>> queryByType(KnowledgeArea area,
                                                     BlackboardEntry.EntryType type) {
        return queryArea(area).stream()
            .filter(entry -> entry.getType() == type)
            .map(entry -> (BlackboardEntry<T>) entry)
            .collect(Collectors.toList());
    }

    /**
     * Queries entries by source agent within a knowledge area.
     *
     * <p>Useful for getting all knowledge posted by a specific agent.</p>
     *
     * @param <T> The expected type of the entry values
     * @param area The knowledge area to query
     * @param sourceAgent UUID of the source agent
     * @return List of matching entries (empty list if none)
     */
    @SuppressWarnings("unchecked")
    public <T> List<BlackboardEntry<T>> queryBySource(KnowledgeArea area, UUID sourceAgent) {
        return queryArea(area).stream()
            .filter(entry -> sourceAgent.equals(entry.getSourceAgent()))
            .map(entry -> (BlackboardEntry<T>) entry)
            .collect(Collectors.toList());
    }

    /**
     * Subscribes to updates for a specific knowledge area.
     *
     * <p>The subscriber will be notified whenever entries are posted to or removed
     * from the specified area.</p>
     *
     * <p><b>Thread Safety:</b> This method is thread-safe. Subscribers can be added
     * from any thread.</p>
     *
     * @param area The knowledge area to subscribe to
     * @param subscriber The subscriber to register
     */
    public void subscribe(KnowledgeArea area, BlackboardSubscriber subscriber) {
        if (area == null || subscriber == null) {
            throw new IllegalArgumentException("Area and subscriber cannot be null");
        }

        if (!subscriber.acceptsArea(area)) {
            LOGGER.debug("Subscriber rejected area {}", area.getId());
            return;
        }

        List<BlackboardSubscriber> areaSubscribers = subscribers.get(area);
        if (!areaSubscribers.contains(subscriber)) {
            areaSubscribers.add(subscriber);
            LOGGER.debug("Subscriber added to area {}: {}",
                area.getId(), subscriber.getClass().getSimpleName());
        }
    }

    /**
     * Subscribes to all knowledge areas.
     *
     * <p>The subscriber will receive notifications for all blackboard updates.
     * Useful for monitoring, logging, and debugging.</p>
     *
     * @param subscriber The subscriber to register
     */
    public void subscribeAll(BlackboardSubscriber subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("Subscriber cannot be null");
        }

        if (!globalSubscribers.contains(subscriber)) {
            globalSubscribers.add(subscriber);
            LOGGER.debug("Global subscriber added: {}",
                subscriber.getClass().getSimpleName());
        }
    }

    /**
     * Unsubscribes a subscriber from a knowledge area.
     *
     * @param area The knowledge area to unsubscribe from
     * @param subscriber The subscriber to remove
     */
    public void unsubscribe(KnowledgeArea area, BlackboardSubscriber subscriber) {
        if (area == null || subscriber == null) {
            return;
        }

        List<BlackboardSubscriber> areaSubscribers = subscribers.get(area);
        areaSubscribers.remove(subscriber);
        LOGGER.debug("Subscriber removed from area {}: {}",
            area.getId(), subscriber.getClass().getSimpleName());
    }

    /**
     * Unsubscribes a global subscriber from all areas.
     *
     * @param subscriber The subscriber to remove
     */
    public void unsubscribeAll(BlackboardSubscriber subscriber) {
        if (subscriber == null) {
            return;
        }

        globalSubscribers.remove(subscriber);
        LOGGER.debug("Global subscriber removed: {}",
            subscriber.getClass().getSimpleName());
    }

    /**
     * Removes an entry from the blackboard.
     *
     * <p>Subscribers will be notified of the removal.</p>
     *
     * @param area The knowledge area to remove from
     * @param key The key of the entry to remove
     * @return true if an entry was removed, false if not found
     */
    public boolean remove(KnowledgeArea area, String key) {
        if (area == null || key == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
            BlackboardEntry<?> removed = areaMap.remove(key);

            if (removed != null) {
                notifyRemoval(area, key);
                LOGGER.debug("Removed entry from {}: {}", area.getId(), key);
                return true;
            }
            return false;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Evicts stale entries from all knowledge areas.
     *
     * <p>An entry is considered stale if its age exceeds maxAgeMs.</p>
     *
     * <p><b>Performance:</b> O(n) across all areas. Consider running periodically
     * rather than on every tick.</p>
     *
     * @param maxAgeMs Maximum age in milliseconds before an entry is considered stale
     * @return Number of entries evicted
     */
    public int evictStale(long maxAgeMs) {
        int totalEvicted = 0;

        for (KnowledgeArea area : KnowledgeArea.values()) {
            totalEvicted += evictStale(area, maxAgeMs);
        }

        if (totalEvicted > 0) {
            LOGGER.info("Evicted {} stale entries from blackboard", totalEvicted);
        }

        return totalEvicted;
    }

    /**
     * Evicts stale entries from a specific knowledge area.
     *
     * <p>Uses the area's default max age if maxAgeMs is not specified.</p>
     *
     * @param area The knowledge area to clean up
     * @param maxAgeMs Maximum age in milliseconds (or null to use area default)
     * @return Number of entries evicted
     */
    public int evictStale(KnowledgeArea area, Long maxAgeMs) {
        if (area == null) {
            return 0;
        }

        long maxAge = maxAgeMs != null ? maxAgeMs : area.getDefaultMaxAgeMs();
        int evicted = 0;

        lock.writeLock().lock();
        try {
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
            Iterator<Map.Entry<String, BlackboardEntry<?>>> iterator =
                areaMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, BlackboardEntry<?>> entry = iterator.next();
                if (entry.getValue().isStale(maxAge)) {
                    iterator.remove();
                    notifyRemoval(area, entry.getKey());
                    evicted++;
                }
            }

            totalEvictions += evicted;

            if (evicted > 0) {
                LOGGER.debug("Evicted {} stale entries from {} (maxAge: {}ms)",
                    evicted, area.getId(), maxAge);
            }

        } finally {
            lock.writeLock().unlock();
        }

        return evicted;
    }

    /**
     * Clears all entries from a knowledge area.
     *
     * <p>Use with caution. Subscribers will be notified of all removals.</p>
     *
     * @param area The knowledge area to clear
     * @return Number of entries removed
     */
    public int clearArea(KnowledgeArea area) {
        if (area == null) {
            return 0;
        }

        lock.writeLock().lock();
        try {
            Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
            int size = areaMap.size();

            // Notify all subscribers before clearing
            for (String key : areaMap.keySet()) {
                notifyRemoval(area, key);
            }

            areaMap.clear();
            LOGGER.info("Cleared {} entries from area {}", size, area.getId());
            return size;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the number of entries in a knowledge area.
     *
     * @param area The knowledge area
     * @return Number of entries, or 0 if area doesn't exist
     */
    public int getEntryCount(KnowledgeArea area) {
        if (area == null) {
            return 0;
        }

        Map<String, BlackboardEntry<?>> areaMap = areas.get(area);
        return areaMap != null ? areaMap.size() : 0;
    }

    /**
     * Gets the total number of entries across all knowledge areas.
     *
     * @return Total entry count
     */
    public int getTotalEntryCount() {
        return areas.values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    /**
     * Gets statistics about blackboard operations.
     *
     * @return String containing operation statistics
     */
    public String getStatistics() {
        return String.format(
            "Blackboard Statistics:\n" +
            "  Total Posts: %d\n" +
            "  Total Queries: %d\n" +
            "  Total Evictions: %d\n" +
            "  Current Entries: %d\n" +
            "  Area Breakdown:\n%s",
            totalPosts, totalQueries, totalEvictions, getTotalEntryCount(),
            getAreaBreakdown()
        );
    }

    /**
     * Gets a breakdown of entries by knowledge area.
     *
     * @return Formatted string showing entry counts per area
     */
    private String getAreaBreakdown() {
        StringBuilder sb = new StringBuilder();
        for (KnowledgeArea area : KnowledgeArea.values()) {
            int count = getEntryCount(area);
            sb.append(String.format("    %s: %d entries\n", area.getId(), count));
        }
        return sb.toString();
    }

    /**
     * Notifies subscribers of a new entry.
     *
     * <p>Synchronized to prevent concurrent callbacks to the same subscriber.</p>
     *
     * @param area The knowledge area
     * @param entry The new entry
     */
    private void notifySubscribers(KnowledgeArea area, BlackboardEntry<?> entry) {
        // Notify area-specific subscribers
        List<BlackboardSubscriber> areaSubscribers = subscribers.get(area);
        for (BlackboardSubscriber subscriber : areaSubscribers) {
            notifySafely(subscriber, area, entry, entry.getKey());
        }

        // Notify global subscribers
        for (BlackboardSubscriber subscriber : globalSubscribers) {
            notifySafely(subscriber, area, entry, entry.getKey());
        }
    }

    /**
     * Notifies subscribers of an entry removal.
     *
     * @param area The knowledge area
     * @param key The key of the removed entry
     */
    private void notifyRemoval(KnowledgeArea area, String key) {
        // Notify area-specific subscribers
        List<BlackboardSubscriber> areaSubscribers = subscribers.get(area);
        for (BlackboardSubscriber subscriber : areaSubscribers) {
            notifyRemovalSafely(subscriber, area, key);
        }

        // Notify global subscribers
        for (BlackboardSubscriber subscriber : globalSubscribers) {
            notifyRemovalSafely(subscriber, area, key);
        }
    }

    /**
     * Safely notifies a subscriber of a new entry, handling errors.
     *
     * @param subscriber The subscriber to notify
     * @param area The knowledge area
     * @param entry The new entry
     * @param key The entry key
     */
    private void notifySafely(BlackboardSubscriber subscriber, KnowledgeArea area,
                             BlackboardEntry<?> entry, String key) {
        try {
            subscriber.onEntryPosted(area, entry);
        } catch (Throwable t) {
            try {
                subscriber.onNotificationError(area, entry, key, t);
            } catch (Throwable innerError) {
                LOGGER.error("Subscriber error handler failed for area {}",
                    area.getId(), innerError);
            }
        }
    }

    /**
     * Safely notifies a subscriber of an entry removal, handling errors.
     *
     * @param subscriber The subscriber to notify
     * @param area The knowledge area
     * @param key The key of the removed entry
     */
    private void notifyRemovalSafely(BlackboardSubscriber subscriber, KnowledgeArea area,
                                    String key) {
        try {
            subscriber.onEntryRemoved(area, key);
        } catch (Throwable t) {
            try {
                subscriber.onNotificationError(area, null, key, t);
            } catch (Throwable innerError) {
                LOGGER.error("Subscriber error handler failed for area {}",
                    area.getId(), innerError);
            }
        }
    }

    /**
     * Resets the blackboard to initial state.
     *
     * <p><b>Warning:</b> This clears all knowledge and notifies all subscribers
     * of removals. Use only for testing or system reset.</p>
     */
    public void reset() {
        LOGGER.warn("Resetting blackboard - clearing all knowledge");

        for (KnowledgeArea area : KnowledgeArea.values()) {
            clearArea(area);
        }

        totalPosts = 0;
        totalQueries = 0;
        totalEvictions = 0;
    }
}
