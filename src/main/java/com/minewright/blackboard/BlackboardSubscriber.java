package com.minewright.blackboard;

/**
 * Interface for objects that subscribe to blackboard knowledge updates.
 *
 * <p><b>Purpose:</b></p>
 * <p>BlackboardSubscriber allows agents and system components to receive notifications
 * when knowledge is posted to or removed from the blackboard. This enables reactive
 * behavior without constant polling, improving efficiency and coordination.</p>
 *
 * <p><b>Subscription Model:</b></p>
 * <p>Subscribers register interest in specific {@link KnowledgeArea}s and receive
 * callbacks only for entries in those areas. This allows agents to focus on relevant
 * information and ignore noise from unrelated domains.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Callback methods may be invoked from multiple threads. Implementations must be
 * thread-safe or use appropriate synchronization. The blackboard system does not
 * guarantee which thread invokes callbacks.</p>
 *
 * <p><b>Performance Considerations:</b></p>
 * <ul>
 *   <li>Callbacks should return quickly to avoid blocking the blackboard</li>
 *   <li>Heavy processing should be offloaded to separate threads</li>
 *   <li>Consider using queues for burst updates (e.g., world scanning)</li>
 * </ul>
 *
 * <p><b>Example Implementation:</b></p>
 * <pre>{@code
 * public class ThreatResponseAgent implements BlackboardSubscriber {
 *     private final Blackboard blackboard;
 *
 *     public void initialize() {
 *         // Subscribe only to threat updates
 *         blackboard.subscribe(KnowledgeArea.THREATS, this);
 *     }
 *
 *     @Override
 *     public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
 *         if (entry.getType() == EntryType.FACT) {
 *             // React to confirmed threats
 *             String threat = (String) entry.getValue();
 *             respondToThreat(threat);
 *         }
 *     }
 *
 *     @Override
 *     public void onEntryRemoved(KnowledgeArea area, String key) {
 *         // Clear threat if it's removed
 *         if (key.startsWith("hostile_")) {
 *             clearThreat(key);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see Blackboard
 * @see KnowledgeArea
 * @see BlackboardEntry
 * @since 1.0.0
 */
public interface BlackboardSubscriber {
    /**
     * Called when a new entry is posted to a subscribed knowledge area.
     *
     * <p>This callback is invoked synchronously during the post operation.
     * Implementations should return quickly to avoid blocking the blackboard.</p>
     *
     * <p><b>Thread Safety:</b> This method may be called from multiple threads.
     * Implementations must be thread-safe.</p>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Trigger reactive behavior (e.g., respond to threats)</li>
     *   <li>Update internal state based on shared knowledge</li>
     *   <li>Coordinate actions based on agent status updates</li>
     *   <li>Log or monitor blackboard activity</li>
     * </ul>
     *
     * @param area The knowledge area receiving the new entry
     * @param entry The new blackboard entry being posted
     */
    void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry);

    /**
     * Called when an entry is removed from a subscribed knowledge area.
     *
     * <p>Entries are removed when:
     * <ul>
     *   <li>Explicitly removed via {@link Blackboard#remove(KnowledgeArea, String)}</li>
     *   <li>Evicted due to staleness (age exceeds maximum)</li>
     *   <li>Replaced by a newer entry with the same key</li>
     * </ul></p>
     *
     * <p><b>Thread Safety:</b> This method may be called from multiple threads.
     * Implementations must be thread-safe.</p>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Cleanup cached data related to removed entries</li>
     *   <li>Clear alerts when threats are resolved</li>
     *   <li>Stop tracking agents that are removed</li>
     *   <li>Release resources associated with the entry</li>
     * </ul>
     *
     * @param area The knowledge area the entry was removed from
     * @param key The key of the removed entry
     */
    void onEntryRemoved(KnowledgeArea area, String key);

    /**
     * Optional method for handling errors during notification.
     *
     * <p>If a subscriber throws an exception in {@link #onEntryPosted} or
     * {@link #onEntryRemoved}, the blackboard will catch it and call this method
     * if implemented. This allows subscribers to handle their own errors without
     * disrupting the blackboard for other subscribers.</p>
     *
     * <p><b>Default Implementation:</b> Logs the error and continues.</p>
     *
     * @param area The knowledge area being processed when the error occurred
     * @param entry The entry being processed (null for removal errors)
     * @param key The key being processed
     * @param throwable The exception thrown by the subscriber
     */
    default void onNotificationError(KnowledgeArea area, BlackboardEntry<?> entry,
                                    String key, Throwable throwable) {
        // Default: Log and continue. Implementations can override for custom handling.
        System.err.printf("BlackboardSubscriber error for area %s, key %s: %s%n",
                area.getId(), key, throwable.getMessage());
    }

    /**
     * Returns whether this subscriber is interested in updates from a given area.
     *
     * <p>This method is called during subscription to filter areas. The default
     * implementation returns true for all areas. Subscribers can override to
     * implement more selective filtering based on area properties.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * @Override
     * public boolean acceptsArea(KnowledgeArea area) {
     *     // Only subscribe to high-frequency areas
     *     return area == KnowledgeArea.THREATS || area == KnowledgeArea.WORLD_STATE;
     * }
     * }</pre>
     *
     * @param area The knowledge area to check
     * @return true if this subscriber wants updates from this area
     */
    default boolean acceptsArea(KnowledgeArea area) {
        return true;
    }
}
