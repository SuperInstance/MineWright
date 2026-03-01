package com.minewright.behavior;

import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shared context data for behavior tree execution.
 *
 * <p><b>Purpose:</b></p>
 * <p>BTBlackboard provides a key-value store for sharing data between behavior tree
 * nodes. Unlike the coordination blackboard ({@link com.minewright.blackboard.Blackboard}),
 * which is for multi-agent coordination, this blackboard is for single behavior tree
 * execution context.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Type-Safe Access:</b> Generic methods for getting/setting typed values</li>
 *   <li><b>Default Values:</b> Support for default values when keys don't exist</li>
 *   <li><b>Lazy Values:</b> Support for suppliers that compute values on-demand</li>
 *   <li><b>Scoped Keys:</b> Namespace support to avoid key collisions</li>
 *   <li><b>Thread Safety:</b> Concurrent access support for parallel nodes</li>
 *   <li><b>Entity Reference:</b> Direct access to the ForemanEntity</li>
 * </ul>
 *
 * <p><b>Architecture:</b></p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     BTBlackboard                              │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Entity Reference: ForemanEntity                             │
 * │  Storage: ConcurrentHashMap&lt;String, Object&gt;                │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Scoped Storage:                                              │
 * │  "target.position" -> BlockPos(100, 64, 200)                 │
 * │  "path.current" -> 5                                         │
 * │  "inventory.has_wood" -> true                                │
 * │  "action.timeout" -> 100                                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Entity Access Methods:                                       │
 * │  - getEntity()                                              │
 * │  - getLevel()                                               │
 * │  - getInventory()                                           │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Create blackboard with entity reference
 * BTBlackboard blackboard = new BTBlackboard(foremanEntity);
 *
 * // Store values (scoped by convention)
 * blackboard.put("target.position", targetPos);
 * blackboard.put("path.current", 0);
 * blackboard.put("action.timeout", 100);
 *
 * // Retrieve with default values
 * BlockPos target = blackboard.get("target.position", BlockPos.ZERO);
 * int pathIndex = blackboard.getInt("path.current", 0);
 * boolean hasWood = blackboard.getBoolean("inventory.has_wood", false);
 *
 * // Use suppliers for lazy evaluation
 * double distance = blackboard.getLazy("distance_to_target",
 *     () -> blackboard.getEntity().position().distanceTo(targetPos));
 *
 * // Scoped access to avoid collisions
 * blackboard.putScoped("move", "target", targetPos);
 * BlockPos moveTarget = blackboard.getScoped("move", "target", BlockPos.ZERO);
 *
 * // Clear a scope when done
 * blackboard.clearScope("move");
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>Uses {@link ConcurrentHashMap} for thread-safe storage</li>
 *   <li>Safe for concurrent access from parallel behavior tree nodes</li>
 *   <li>Individual get/put operations are atomic</li>
 *   <li>Compound operations should use external synchronization</li>
 * </ul>
 *
 * <p><b>Comparison to Coordination Blackboard:</b></p>
 * <table border="1">
 *   <tr><th>Feature</th><th>BTBlackboard</th><th>Coordination Blackboard</th></tr>
 *   <tr><td>Purpose</td><td>Behavior tree context</td><td>Multi-agent coordination</td></tr>
 *   <tr><td>Lifecycle</td><td>Per behavior tree execution</td><td>Global singleton</td></tr>
 *   <tr><td>Entries</td><td>Typed key-value pairs</td><td>Typed entries with metadata</td></tr>
 *   <tr><td>Subscriptions</td><td>None</td><td>Reactive notifications</td></tr>
 *   <tr><td>Eviction</td><td>Manual clear</td><td>Automatic staleness</td></tr>
 * </table>
 *
 * @see BTNode
 * @see com.minewright.blackboard.Blackboard
 * @see ForemanEntity
 * @since 1.0.0
 */
public class BTBlackboard {
    private static final Logger LOGGER = TestLogger.getLogger(BTBlackboard.class);

    /**
     * The ForemanEntity this behavior tree is executing for.
     * Never null after construction.
     */
    private final ForemanEntity entity;

    /**
     * Thread-safe storage for arbitrary key-value pairs.
     * Uses ConcurrentHashMap for safe concurrent access from parallel nodes.
     */
    private final Map<String, Object> data;

    /**
     * Scope separator for scoped key access.
     */
    private static final String SCOPE_SEPARATOR = ".";

    /**
     * Creates a new behavior tree blackboard for the given entity.
     *
     * @param entity The ForemanEntity this behavior tree is executing for
     * @throws IllegalArgumentException if entity is null
     */
    public BTBlackboard(ForemanEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        this.entity = entity;
        this.data = new ConcurrentHashMap<>();
        LOGGER.debug("[{}] Created BTBlackboard", entity.getEntityName());
    }

    /**
     * Gets the ForemanEntity this blackboard is associated with.
     *
     * @return The ForemanEntity
     */
    public ForemanEntity getEntity() {
        return entity;
    }

    /**
     * Gets the Minecraft level (world) for the entity.
     *
     * @return The entity's level
     */
    public net.minecraft.world.level.Level getLevel() {
        return entity.level();
    }

    /**
     * Stores a value in the blackboard.
     *
     * <p>Values can be retrieved later with {@link #get(String, Object)}.
     * Null values are stored (use {@link #remove(String)} to delete).</p>
     *
     * @param key The key to store the value under
     * @param value The value to store (can be null)
     * @throws IllegalArgumentException if key is null
     */
    public void put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        data.put(key, value);
        LOGGER.trace("[{}] BTBlackboard.put: {} = {}", entity.getEntityName(), key, value);
    }

    /**
     * Stores a value in a scoped namespace.
     *
     * <p>Scoped keys help avoid collisions between different behavior subtrees.
     * The key is stored as "scope.key" internally.</p>
     *
     * @param scope The scope namespace (e.g., "move", "attack", "mine")
     * @param key The key within the scope
     * @param value The value to store
     * @throws IllegalArgumentException if scope or key is null
     */
    public void putScoped(String scope, String key, Object value) {
        if (scope == null || key == null) {
            throw new IllegalArgumentException("Scope and key cannot be null");
        }
        put(scopedKey(scope, key), value);
    }

    /**
     * Gets a value from the blackboard, returning a default if not found.
     *
     * @param <T> The expected type of the value
     * @param key The key to look up
     * @param defaultValue The default value if key is not found
     * @return The value, or defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            LOGGER.warn("[{}] Type mismatch getting '{}': expected {}, got {}",
                entity.getEntityName(), key, defaultValue.getClass(), value.getClass());
            return defaultValue;
        }
    }

    /**
     * Gets a value from the blackboard, returning null if not found.
     *
     * @param <T> The expected type of the value
     * @param key The key to look up
     * @return The value, or null if not found
     */
    public <T> T get(String key) {
        return get(key, null);
    }

    /**
     * Gets a scoped value from the blackboard.
     *
     * @param <T> The expected type of the value
     * @param scope The scope namespace
     * @param key The key within the scope
     * @param defaultValue The default value if not found
     * @return The value, or defaultValue if not found
     */
    public <T> T getScoped(String scope, String key, T defaultValue) {
        return get(scopedKey(scope, key), defaultValue);
    }

    /**
     * Gets a boolean value from the blackboard.
     *
     * @param key The key to look up
     * @param defaultValue The default value if not found
     * @return The boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key, defaultValue);
    }

    /**
     * Gets an int value from the blackboard.
     *
     * @param key The key to look up
     * @param defaultValue The default value if not found
     * @return The int value
     */
    public int getInt(String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Gets a long value from the blackboard.
     *
     * @param key The key to look up
     * @param defaultValue The default value if not found
     * @return The long value
     */
    public long getLong(String key, long defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Gets a double value from the blackboard.
     *
     * @param key The key to look up
     * @param defaultValue The default value if not found
     * @return The double value
     */
    public double getDouble(String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Gets a String value from the blackboard.
     *
     * @param key The key to look up
     * @param defaultValue The default value if not found
     * @return The String value
     */
    public String getString(String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Gets a value, computing it lazily if not present.
     *
     * <p>If the key is not present, the supplier is called to compute the value.
     * The computed value is stored in the blackboard for future access.</p>
     *
     * @param <T> The type of the value
     * @param key The key to look up
     * @param supplier The supplier to compute the value if not present
     * @return The value (cached or computed)
     */
    public <T> T getLazy(String key, Supplier<T> supplier) {
        if (containsKey(key)) {
            return get(key);
        }
        T value = supplier.get();
        put(key, value);
        return value;
    }

    /**
     * Checks if a key is present in the blackboard.
     *
     * @param key The key to check
     * @return true if the key exists (even if value is null)
     */
    public boolean containsKey(String key) {
        return key != null && data.containsKey(key);
    }

    /**
     * Checks if a scoped key is present in the blackboard.
     *
     * @param scope The scope namespace
     * @param key The key within the scope
     * @return true if the scoped key exists
     */
    public boolean containsScoped(String scope, String key) {
        return containsKey(scopedKey(scope, key));
    }

    /**
     * Removes a key from the blackboard.
     *
     * @param key The key to remove
     * @return The previous value, or null if not present
     */
    public Object remove(String key) {
        if (key == null) {
            return null;
        }
        Object removed = data.remove(key);
        LOGGER.trace("[{}] BTBlackboard.remove: {} (was: {})",
            entity.getEntityName(), key, removed);
        return removed;
    }

    /**
     * Removes a scoped key from the blackboard.
     *
     * @param scope The scope namespace
     * @param key The key within the scope
     * @return The previous value, or null if not present
     */
    public Object removeScoped(String scope, String key) {
        return remove(scopedKey(scope, key));
    }

    /**
     * Clears all keys in a scope.
     *
     * <p>Removes all keys that start with "scope." from the blackboard.</p>
     *
     * @param scope The scope to clear
     * @return The number of keys removed
     */
    public int clearScope(String scope) {
        if (scope == null) {
            return 0;
        }
        String prefix = scope + SCOPE_SEPARATOR;
        int removed = 0;
        Iterator<Map.Entry<String, Object>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                it.remove();
                removed++;
            }
        }
        LOGGER.debug("[{}] Cleared scope '{}': {} keys removed",
            entity.getEntityName(), scope, removed);
        return removed;
    }

    /**
     * Clears all data from the blackboard.
     */
    public void clear() {
        int size = data.size();
        data.clear();
        LOGGER.debug("[{}] Cleared BTBlackboard: {} keys removed",
            entity.getEntityName(), size);
    }

    /**
     * Gets the number of entries in the blackboard.
     *
     * @return The entry count
     */
    public int size() {
        return data.size();
    }

    /**
     * Checks if the blackboard is empty.
     *
     * @return true if no entries are stored
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Gets all keys in the blackboard.
     *
     * @return Unmodifiable set of all keys
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /**
     * Gets all keys in a scope.
     *
     * @param scope The scope namespace
     * @return Set of keys in the scope (without scope prefix)
     */
    public Set<String> getScopedKeys(String scope) {
        if (scope == null) {
            return Collections.emptySet();
        }
        String prefix = scope + SCOPE_SEPARATOR;
        Set<String> scopedKeys = new HashSet<>();
        for (String key : data.keySet()) {
            if (key.startsWith(prefix)) {
                scopedKeys.add(key.substring(prefix.length()));
            }
        }
        return scopedKeys;
    }

    /**
     * Creates a scoped key by combining scope and key.
     *
     * @param scope The scope namespace
     * @param key The key within the scope
     * @return The scoped key (e.g., "move.target")
     */
    private String scopedKey(String scope, String key) {
        return scope + SCOPE_SEPARATOR + key;
    }

    /**
     * Creates a snapshot of the current blackboard state.
     *
     * <p>Useful for debugging or saving state before risky operations.</p>
     *
     * @return A new BTBlackboard with a copy of all data
     */
    public BTBlackboard snapshot() {
        BTBlackboard copy = new BTBlackboard(entity);
        copy.data.putAll(this.data);
        return copy;
    }

    /**
     * Copies all data from another blackboard into this one.
     *
     * <p>Existing keys are overwritten. The entity reference is not changed.</p>
     *
     * @param other The blackboard to copy from
     */
    public void copyFrom(BTBlackboard other) {
        if (other != null) {
            this.data.putAll(other.data);
            LOGGER.debug("[{}] Copied {} keys from another BTBlackboard",
                entity.getEntityName(), other.data.size());
        }
    }

    @Override
    public String toString() {
        return String.format("BTBlackboard[%s, %d entries]",
            entity.getEntityName(), data.size());
    }
}
