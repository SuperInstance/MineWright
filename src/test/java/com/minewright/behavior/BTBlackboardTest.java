package com.minewright.behavior;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BTBlackboard}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Key-value storage and retrieval</li>
 *   <li>Type-safe access methods</li>
 *   <li>Scoped key access</li>
 *   <li>Thread-safe operations</li>
 *   <li>Edge cases (null handling, missing keys)</li>
 * </ul>
 */
@DisplayName("BTBlackboard Tests")
class BTBlackboardTest {

    private BTBlackboard blackboard;

    @BeforeEach
    void setUp() {
        blackboard = createMockBlackboard();
    }

    /**
     * Creates a test blackboard without needing a real ForemanEntity.
     * Uses sun.misc.Unsafe to allocate instance bypassing constructor.
     */
    private BTBlackboard createMockBlackboard() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Object unsafe = theUnsafeField.get(null);
            java.lang.reflect.Method allocateInstance =
                unsafeClass.getMethod("allocateInstance", Class.class);
            BTBlackboard bb = (BTBlackboard) allocateInstance.invoke(unsafe, BTBlackboard.class);

            // Initialize the entity and data fields directly
            Field dataField = BTBlackboard.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(bb, new ConcurrentHashMap<>());

            // Create a mock entity
            MockForemanEntity mockEntity = new MockForemanEntity("TestEntity");

            Field entityField = BTBlackboard.class.getDeclaredField("entity");
            entityField.setAccessible(true);
            entityField.set(bb, mockEntity);

            return bb;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test blackboard using Unsafe", e);
        }
    }

    // ------------------------------------------------------------------------
    // Basic Storage Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Put and get simple value")
    void testPutAndGet() {
        blackboard.put("test_key", "test_value");

        String result = blackboard.get("test_key");

        assertEquals("test_value", result,
            "Should retrieve stored value");
    }

    @Test
    @DisplayName("Get with default value returns default when key missing")
    void testGetWithDefault() {
        String result = blackboard.get("missing_key", "default_value");

        assertEquals("default_value", result,
            "Should return default value for missing key");
    }

    @Test
    @DisplayName("Get with default returns stored value when key exists")
    void testGetWithDefaultKeyExists() {
        blackboard.put("test_key", "stored_value");

        String result = blackboard.get("test_key", "default_value");

        assertEquals("stored_value", result,
            "Should return stored value, not default");
    }

    @Test
    @DisplayName("Put and get null value")
    void testPutAndGetNull() {
        blackboard.put("null_key", null);

        String result = blackboard.get("null_key", "default");

        assertNull(result,
            "Should store and retrieve null value");
        assertTrue(blackboard.containsKey("null_key"),
            "Null value key should still exist");
    }

    @Test
    @DisplayName("Put replaces existing value")
    void testPutReplace() {
        blackboard.put("key", "value1");
        blackboard.put("key", "value2");

        String result = blackboard.get("key");

        assertEquals("value2", result,
            "Should replace existing value");
    }

    @Test
    @DisplayName("ContainsKey returns true for existing key")
    void testContainsKeyTrue() {
        blackboard.put("test_key", "value");

        assertTrue(blackboard.containsKey("test_key"),
            "Should return true for existing key");
    }

    @Test
    @DisplayName("ContainsKey returns false for missing key")
    void testContainsKeyFalse() {
        assertFalse(blackboard.containsKey("missing_key"),
            "Should return false for missing key");
    }

    @Test
    @DisplayName("ContainsKey returns false for null key")
    void testContainsKeyNull() {
        assertFalse(blackboard.containsKey(null),
            "Should return false for null key");
    }

    // ------------------------------------------------------------------------
    // Typed Access Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("GetBoolean returns true for true value")
    void testGetBooleanTrue() {
        blackboard.put("flag", true);

        boolean result = blackboard.getBoolean("flag", false);

        assertTrue(result,
            "Should return true");
    }

    @Test
    @DisplayName("GetBoolean returns default for missing key")
    void testGetBooleanDefault() {
        boolean result = blackboard.getBoolean("missing", true);

        assertTrue(result,
            "Should return default value");
    }

    @Test
    @DisplayName("GetBoolean converts Boolean object")
    void testGetBooleanObject() {
        blackboard.put("flag", Boolean.TRUE);

        boolean result = blackboard.getBoolean("flag", false);

        assertTrue(result,
            "Should convert Boolean object to primitive");
    }

    @Test
    @DisplayName("GetInt returns integer value")
    void testGetInt() {
        blackboard.put("count", 42);

        int result = blackboard.getInt("count", 0);

        assertEquals(42, result,
            "Should return integer value");
    }

    @Test
    @DisplayName("GetInt converts Long to int")
    void testGetIntFromLong() {
        blackboard.put("count", 42L);

        int result = blackboard.getInt("count", 0);

        assertEquals(42, result,
            "Should convert Long to int");
    }

    @Test
    @DisplayName("GetInt converts Double to int")
    void testGetIntFromDouble() {
        blackboard.put("count", 42.7);

        int result = blackboard.getInt("count", 0);

        assertEquals(42, result,
            "Should convert Double to int (truncated)");
    }

    @Test
    @DisplayName("GetLong returns long value")
    void testGetLong() {
        blackboard.put("timestamp", 123456789L);

        long result = blackboard.getLong("timestamp", 0L);

        assertEquals(123456789L, result,
            "Should return long value");
    }

    @Test
    @DisplayName("GetDouble returns double value")
    void testGetDouble() {
        blackboard.put("price", 19.99);

        double result = blackboard.getDouble("price", 0.0);

        assertEquals(19.99, result, 0.001,
            "Should return double value");
    }

    @Test
    @DisplayName("GetString returns string value")
    void testGetString() {
        blackboard.put("name", "TestEntity");

        String result = blackboard.getString("name", "Unknown");

        assertEquals("TestEntity", result,
            "Should return string value");
    }

    @Test
    @DisplayName("GetString converts object to string")
    void testGetStringConversion() {
        blackboard.put("obj", 123);

        String result = blackboard.getString("obj", "default");

        assertEquals("123", result,
            "Should convert object to string");
    }

    @Test
    @DisplayName("GetString returns default for null value")
    void testGetStringDefaultForNull() {
        blackboard.put("null_val", null);

        String result = blackboard.getString("null_val", "default");

        assertEquals("default", result,
            "Should return default for null value");
    }

    // ------------------------------------------------------------------------
    // Scoped Access Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("PutScoped creates scoped key")
    void testPutScoped() {
        blackboard.putScoped("move", "target", "position_value");

        String result = blackboard.getScoped("move", "target", "default");

        assertEquals("position_value", result,
            "Should store and retrieve scoped value");
    }

    @Test
    @DisplayName("Scoped key uses dot separator")
    void testScopedKeySeparator() {
        blackboard.putScoped("scope1", "key1", "value1");

        // Access with full key
        String result = blackboard.get("scope1.key1");

        assertEquals("value1", result,
            "Scoped key should use dot separator");
    }

    @Test
    @DisplayName("ContainsScoped checks scoped key")
    void testContainsScoped() {
        blackboard.putScoped("combat", "target", "enemy");

        assertTrue(blackboard.containsScoped("combat", "target"),
            "Should find scoped key");
        assertFalse(blackboard.containsScoped("combat", "missing"),
            "Should not find missing scoped key");
    }

    @Test
    @DisplayName("GetScopedKeys returns keys in scope")
    void testGetScopedKeys() {
        blackboard.putScoped("move", "target", "pos1");
        blackboard.putScoped("move", "path", "path1");
        blackboard.putScoped("combat", "target", "enemy");

        Set<String> moveKeys = blackboard.getScopedKeys("move");

        assertEquals(2, moveKeys.size(),
            "Should return 2 keys in move scope");
        assertTrue(moveKeys.contains("target"),
            "Should contain 'target' key");
        assertTrue(moveKeys.contains("path"),
            "Should contain 'path' key");
        assertFalse(moveKeys.contains("enemy"),
            "Should not contain keys from other scopes");
    }

    @Test
    @DisplayName("ClearScope removes all keys in scope")
    void testClearScope() {
        blackboard.putScoped("move", "target", "pos1");
        blackboard.putScoped("move", "path", "path1");
        blackboard.putScoped("combat", "target", "enemy");

        int removed = blackboard.clearScope("move");

        assertEquals(2, removed,
            "Should remove 2 keys from move scope");
        assertFalse(blackboard.containsKey("move.target"),
            "move.target should be removed");
        assertFalse(blackboard.containsKey("move.path"),
            "move.path should be removed");
        assertTrue(blackboard.containsKey("combat.target"),
            "combat.target should still exist");
    }

    @Test
    @DisplayName("ClearScope with null scope returns 0")
    void testClearScopeNull() {
        blackboard.put("test", "value");

        int removed = blackboard.clearScope(null);

        assertEquals(0, removed,
            "Should return 0 for null scope");
        assertTrue(blackboard.containsKey("test"),
            "Keys should not be removed");
    }

    @Test
    @DisplayName("RemoveScoped removes scoped key")
    void testRemoveScoped() {
        blackboard.putScoped("move", "target", "pos1");

        Object removed = blackboard.removeScoped("move", "target");

        assertEquals("pos1", removed,
            "Should return removed value");
        assertFalse(blackboard.containsScoped("move", "target"),
            "Scoped key should be removed");
    }

    // ------------------------------------------------------------------------
    // Lazy Evaluation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("GetLazy computes value on first access")
    void testGetLazyComputes() {
        blackboard.put("counter", 0);

        Integer result = blackboard.getLazy("computed", () -> {
            int counter = blackboard.getInt("counter", 0);
            return counter + 100;
        });

        assertEquals(100, result,
            "Should compute lazy value");
        assertEquals(100, (Integer) blackboard.get("computed"),
            "Should cache computed value");
    }

    @Test
    @DisplayName("GetLazy returns cached value on subsequent access")
    void testGetLazyCached() {
        // First access
        Integer result1 = blackboard.getLazy("cached", () -> {
            return 42;
        });

        // Second access (should use cache)
        Integer result2 = blackboard.getLazy("cached", () -> {
            return 999; // Different value
        });

        assertEquals(42, result1,
            "First access should compute");
        assertEquals(42, result2,
            "Second access should use cache");
    }

    // ------------------------------------------------------------------------
    // Removal and Clear Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Remove removes key and returns value")
    void testRemove() {
        blackboard.put("test", "value");

        Object removed = blackboard.remove("test");

        assertEquals("value", removed,
            "Should return removed value");
        assertFalse(blackboard.containsKey("test"),
            "Key should be removed");
    }

    @Test
    @DisplayName("Remove with null key returns null")
    void testRemoveNullKey() {
        blackboard.put("test", "value");

        Object removed = blackboard.remove(null);

        assertNull(removed,
            "Should return null for null key");
        assertTrue(blackboard.containsKey("test"),
            "Existing keys should not be affected");
    }

    @Test
    @DisplayName("Remove with missing key returns null")
    void testRemoveMissingKey() {
        Object removed = blackboard.remove("missing");

        assertNull(removed,
            "Should return null for missing key");
    }

    @Test
    @DisplayName("Clear removes all entries")
    void testClear() {
        blackboard.put("key1", "value1");
        blackboard.put("key2", "value2");
        blackboard.putScoped("scope", "key", "value3");

        assertFalse(blackboard.isEmpty(),
            "Should not be empty");

        blackboard.clear();

        assertTrue(blackboard.isEmpty(),
            "Should be empty after clear");
        assertEquals(0, blackboard.size(),
            "Size should be 0");
    }

    // ------------------------------------------------------------------------
    // Utility Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Size returns entry count")
    void testSize() {
        assertEquals(0, blackboard.size(),
            "Initial size should be 0");

        blackboard.put("key1", "value1");
        assertEquals(1, blackboard.size());

        blackboard.put("key2", "value2");
        assertEquals(2, blackboard.size());

        blackboard.remove("key1");
        assertEquals(1, blackboard.size());
    }

    @Test
    @DisplayName("IsEmpty returns true when empty")
    void testIsEmpty() {
        assertTrue(blackboard.isEmpty(),
            "Should be empty initially");

        blackboard.put("key", "value");

        assertFalse(blackboard.isEmpty(),
            "Should not be empty after adding entry");
    }

    @Test
    @DisplayName("GetKeys returns all keys")
    void testGetKeys() {
        blackboard.put("key1", "value1");
        blackboard.put("key2", "value2");

        Set<String> keys = blackboard.getKeys();

        assertEquals(2, keys.size(),
            "Should return 2 keys");
        assertTrue(keys.contains("key1"),
            "Should contain key1");
        assertTrue(keys.contains("key2"),
            "Should contain key2");
    }

    @Test
    @DisplayName("GetKeys returns unmodifiable set")
    void testGetKeysUnmodifiable() {
        blackboard.put("key", "value");

        Set<String> keys = blackboard.getKeys();

        assertThrows(UnsupportedOperationException.class,
            () -> keys.add("new_key"),
            "Should throw on modification attempt");
    }

    // ------------------------------------------------------------------------
    // Snapshot and Copy Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Snapshot creates independent copy")
    void testSnapshot() {
        blackboard.put("key1", "value1");
        blackboard.put("key2", "value2");

        BTBlackboard snapshot = blackboard.snapshot();

        // Verify snapshot has same data
        assertEquals("value1", snapshot.get("key1"));
        assertEquals("value2", snapshot.get("key2"));

        // Modify original
        blackboard.put("key1", "modified");
        blackboard.put("key3", "new");

        // Snapshot should be unchanged
        assertEquals("value1", snapshot.get("key1"),
            "Snapshot should not be affected by original changes");
        assertNull(snapshot.get("key3"),
            "Snapshot should not have new keys");
    }

    @Test
    @DisplayName("CopyFrom copies all entries")
    void testCopyFrom() {
        blackboard.put("key1", "value1");

        BTBlackboard other = createMockBlackboard();
        other.put("key2", "value2");

        blackboard.copyFrom(other);

        assertTrue(blackboard.containsKey("key1"),
            "Original keys should be preserved");
        assertTrue(blackboard.containsKey("key2"),
            "Should copy keys from other");
        assertEquals("value2", blackboard.get("key2"));
    }

    @Test
    @DisplayName("CopyFrom with null does nothing")
    void testCopyFromNull() {
        blackboard.put("key", "value");

        blackboard.copyFrom(null);

        assertEquals("value", blackboard.get("key"),
            "Should not be affected");
    }

    // ------------------------------------------------------------------------
    // Entity Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("GetEntity returns entity")
    void testGetEntity() {
        assertNotNull(blackboard.getEntity(),
            "Should return entity");
        assertEquals("TestEntity", blackboard.getEntity().getEntityName(),
            "Should return correct entity");
    }

    // ------------------------------------------------------------------------
    // ToString Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("ToString includes entity name and entry count")
    void testToString() {
        blackboard.put("key1", "value1");
        blackboard.put("key2", "value2");

        String str = blackboard.toString();

        assertTrue(str.contains("TestEntity"),
            "Should include entity name");
        assertTrue(str.contains("2"),
            "Should include entry count");
    }

    // ------------------------------------------------------------------------
    // Edge Cases and Type Safety Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Get with type mismatch returns default")
    void testGetTypeMismatch() {
        blackboard.put("number", 42);

        String result = blackboard.get("number", "default");

        assertEquals("default", result,
            "Type mismatch should return default");
    }

    @Test
    @DisplayName("Multiple types can coexist")
    void testMultipleTypes() {
        blackboard.put("string", "text");
        blackboard.put("integer", 42);
        blackboard.put("floating", 3.14);
        blackboard.put("boolean", true);

        assertEquals("text", blackboard.get("string"));
        assertEquals(42, blackboard.getInt("integer", 0));
        assertEquals(3.14, blackboard.getDouble("floating", 0.0), 0.001);
        assertTrue(blackboard.getBoolean("boolean", false));
    }

    @Test
    @DisplayName("Put null key throws exception")
    void testPutNullKey() {
        assertThrows(IllegalArgumentException.class,
            () -> blackboard.put(null, "value"),
            "Should throw on null key");
    }

    @Test
    @DisplayName("PutScoped null scope throws exception")
    void testPutScopedNullScope() {
        assertThrows(IllegalArgumentException.class,
            () -> blackboard.putScoped(null, "key", "value"),
            "Should throw on null scope");
    }

    @Test
    @DisplayName("PutScoped null key throws exception")
    void testPutScopedNullKey() {
        assertThrows(IllegalArgumentException.class,
            () -> blackboard.putScoped("scope", null, "value"),
            "Should throw on null key");
    }

    // ------------------------------------------------------------------------
    // Helper class for testing
    // ------------------------------------------------------------------------

    /**
     * Simple mock ForemanEntity for testing.
     */
    private static class MockForemanEntity {
        private final String name;

        MockForemanEntity(String name) {
            this.name = name;
        }

        public String getEntityName() {
            return name;
        }

        public Object level() {
            return null; // Not used in tests
        }
    }
}
