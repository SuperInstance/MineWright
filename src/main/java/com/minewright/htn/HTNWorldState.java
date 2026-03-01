package com.minewright.htn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the world state for Hierarchical Task Network (HTN) planning.
 *
 * <p><b>World State:</b></p>
 * <p>World state captures the current conditions of the game world that affect
 * planning decisions. It is used to evaluate method preconditions and guide
 * decomposition choices.</p>
 *
 * <p><b>Common World State Properties:</b></p>
 * <ul>
 *   <li><b>Inventory:</b> hasWood, hasStone, toolType, itemCount</li>
 *   <li><b>Position:</b> positionX, positionY, positionZ, nearTarget</li>
 *   <li><b>World:</b> timeOfDay, biome, weatherCondition</li>
 *   <li><b>Agent:</b> healthLevel, hungerLevel, currentAction</li>
 *   <li><b>Task:</b> hasMaterials, craftingTableNearby, fuelAvailable</li>
 * </ul>
 *
 * <p><b>Immutability & Cloning:</b></p>
 * <p>HTN planning requires backtracking, so world states must support efficient
 * snapshot creation. This class provides both mutable and immutable modes:</p>
 * <ul>
 *   <li><b>Mutable:</b> For building initial state</li>
 *   <li><b>Immutable snapshots:</b> Created via {@link #snapshot()}, cannot be modified</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Build initial world state
 * HTNWorldState state = HTNWorldState.builder()
 *     .property("hasWood", true)
 *     .property("woodCount", 64)
 *     .property("toolType", "diamond_axe")
 *     .property("positionX", 100)
 *     .property("positionZ", 200)
 *     .build();
 *
 * // Check state in method preconditions
 * method.precondition(s -> s.getBoolean("hasWood") && s.getInt("woodCount") >= 64);
 *
 * // Create snapshot for backtracking
 * HTNWorldState snapshot = state.snapshot();
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe for concurrent reads. Use {@link Builder} or
 * {@link #createMutable()} for thread-safe mutation.</p>
 *
 * @see HTNMethod
 * @see HTNPlanner
 *
 * @since 1.0.0
 */
public class HTNWorldState {
    /**
     * The underlying property storage.
     * ConcurrentHashMap for thread-safe access.
     */
    private final Map<String, Object> properties;

    /**
     * Whether this state is immutable (a snapshot).
     * Snapshots cannot be modified and throw on mutation attempts.
     */
    private final boolean isImmutable;

    /**
     * Cached hash code for efficient comparison.
     */
    private transient Integer hashCodeCache;

    /**
     * Private constructor for immutable snapshots.
     */
    private HTNWorldState(Map<String, Object> properties, boolean isImmutable) {
        this.properties = isImmutable ? Collections.unmodifiableMap(new HashMap<>(properties))
                                       : new ConcurrentHashMap<>(properties);
        this.isImmutable = isImmutable;
    }

    /**
     * Creates a new empty mutable world state.
     *
     * @return A new mutable HTNWorldState
     */
    public static HTNWorldState createMutable() {
        return new HTNWorldState(new ConcurrentHashMap<>(), false);
    }

    /**
     * Creates a world state with an initial property.
     *
     * @param key   Property key
     * @param value Property value
     * @return A new mutable HTNWorldState
     */
    public static HTNWorldState withProperty(String key, Object value) {
        HTNWorldState state = createMutable();
        state.setProperty(key, value);
        return state;
    }

    /**
     * Creates a builder for constructing world states.
     *
     * @return A new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an immutable snapshot of this state.
     * Snapshots are used during backtracking in HTN planning.
     *
     * @return An immutable copy of this state
     */
    public HTNWorldState snapshot() {
        return new HTNWorldState(this.properties, true);
    }

    /**
     * Creates a mutable copy of this state.
     * Useful for branching exploration in planning.
     *
     * @return A mutable copy of this state
     */
    public HTNWorldState copyMutable() {
        return new HTNWorldState(this.properties, false);
    }

    /**
     * Gets a property value.
     *
     * @param key Property key
     * @return Property value, or null if not found
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Gets a property value with default.
     *
     * @param key          Property key
     * @param defaultValue Default value if not found
     * @return Property value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Gets a boolean property value.
     *
     * @param key Property key
     * @return Boolean value, or false if not found/not a boolean
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Gets a boolean property value with default.
     *
     * @param key          Property key
     * @param defaultValue Default value
     * @return Boolean value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * Gets an integer property value.
     *
     * @param key Property key
     * @return Integer value, or 0 if not found/not a number
     */
    public int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * Gets an integer property value with default.
     *
     * @param key          Property key
     * @param defaultValue Default value
     * @return Integer value or default
     */
    public int getInt(String key, int defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Gets a long property value.
     *
     * @param key Property key
     * @return Long value, or 0 if not found/not a number
     */
    public long getLong(String key) {
        return getLong(key, 0L);
    }

    /**
     * Gets a long property value with default.
     *
     * @param key          Property key
     * @param defaultValue Default value
     * @return Long value or default
     */
    public long getLong(String key, long defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Gets a double property value.
     *
     * @param key Property key
     * @return Double value, or 0.0 if not found/not a number
     */
    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    /**
     * Gets a double property value with default.
     *
     * @param key          Property key
     * @param defaultValue Default value
     * @return Double value or default
     */
    public double getDouble(String key, double defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Gets a string property value.
     *
     * @param key Property key
     * @return String value, or null if not found
     */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * Gets a string property value with default.
     *
     * @param key          Property key
     * @param defaultValue Default value
     * @return String value or default
     */
    public String getString(String key, String defaultValue) {
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Checks if a property exists.
     *
     * @param key Property key
     * @return true if property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Checks if multiple properties all exist.
     *
     * @param keys Property keys to check
     * @return true if all properties exist
     */
    public boolean hasProperties(String... keys) {
        for (String key : keys) {
            if (!properties.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets a property value.
     *
     * @param key   Property key
     * @param value Property value
     * @throws IllegalStateException if this state is immutable
     */
    public void setProperty(String key, Object value) {
        if (isImmutable) {
            throw new IllegalStateException("Cannot modify immutable snapshot. Use copyMutable() first.");
        }
        properties.put(key, value);
    }

    /**
     * Sets multiple properties at once.
     *
     * @param newProperties Map of properties to set
     * @throws IllegalStateException if this state is immutable
     */
    public void setProperties(Map<String, Object> newProperties) {
        if (isImmutable) {
            throw new IllegalStateException("Cannot modify immutable snapshot. Use copyMutable() first.");
        }
        if (newProperties != null) {
            properties.putAll(newProperties);
        }
    }

    /**
     * Removes a property.
     *
     * @param key Property key to remove
     * @throws IllegalStateException if this state is immutable
     */
    public void removeProperty(String key) {
        if (isImmutable) {
            throw new IllegalStateException("Cannot modify immutable snapshot. Use copyMutable() first.");
        }
        properties.remove(key);
    }

    /**
     * Clears all properties.
     *
     * @throws IllegalStateException if this state is immutable
     */
    public void clear() {
        if (isImmutable) {
            throw new IllegalStateException("Cannot modify immutable snapshot. Use copyMutable() first.");
        }
        properties.clear();
    }

    /**
     * Gets all property keys.
     *
     * @return Set of all property keys
     */
    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    /**
     * Gets the number of properties.
     *
     * @return Property count
     */
    public int size() {
        return properties.size();
    }

    /**
     * Checks if this state has no properties.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * Checks if this state is immutable.
     *
     * @return true if this is a snapshot
     */
    public boolean isImmutable() {
        return isImmutable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTNWorldState that = (HTNWorldState) o;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        if (hashCodeCache == null) {
            hashCodeCache = Objects.hash(properties);
        }
        return hashCodeCache;
    }

    @Override
    public String toString() {
        return "HTNWorldState{" +
               "properties=" + properties +
               ", immutable=" + isImmutable +
               '}';
    }

    /**
     * Builder for constructing HTNWorldState instances.
     */
    public static class Builder {
        private final Map<String, Object> properties = new HashMap<>();

        /**
         * Sets a property value.
         *
         * @param key   Property key
         * @param value Property value
         * @return This builder for chaining
         */
        public Builder property(String key, Object value) {
            properties.put(key, value);
            return this;
        }

        /**
         * Sets a boolean property.
         *
         * @param key   Property key
         * @param value Boolean value
         * @return This builder for chaining
         */
        public Builder property(String key, boolean value) {
            properties.put(key, value);
            return this;
        }

        /**
         * Sets an integer property.
         *
         * @param key   Property key
         * @param value Integer value
         * @return This builder for chaining
         */
        public Builder property(String key, int value) {
            properties.put(key, value);
            return this;
        }

        /**
         * Sets a long property.
         *
         * @param key   Property key
         * @param value Long value
         * @return This builder for chaining
         */
        public Builder property(String key, long value) {
            properties.put(key, value);
            return this;
        }

        /**
         * Sets a double property.
         *
         * @param key   Property key
         * @param value Double value
         * @return This builder for chaining
         */
        public Builder property(String key, double value) {
            properties.put(key, value);
            return this;
        }

        /**
         * Sets multiple properties at once.
         *
         * @param properties Map of properties to add
         * @return This builder for chaining
         */
        public Builder properties(Map<String, Object> properties) {
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }

        /**
         * Builds a mutable HTNWorldState.
         *
         * @return A new mutable HTNWorldState
         */
        public HTNWorldState buildMutable() {
            return new HTNWorldState(new ConcurrentHashMap<>(properties), false);
        }

        /**
         * Builds an immutable HTNWorldState snapshot.
         *
         * @return An immutable HTNWorldState
         */
        public HTNWorldState buildImmutable() {
            return new HTNWorldState(properties, true);
        }

        /**
         * Builds a mutable HTNWorldState (default).
         *
         * @return A new mutable HTNWorldState
         */
        public HTNWorldState build() {
            return buildMutable();
        }
    }
}
