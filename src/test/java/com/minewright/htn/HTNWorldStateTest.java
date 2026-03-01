package com.minewright.htn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link HTNWorldState}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Builder pattern for state creation</li>
 *   <li>Property storage and retrieval</li>
 *   <li>Type-safe getters (boolean, int, long, double, string)</li>
 *   <li>Immutability and snapshot functionality</li>
 *   <li>Mutable copy creation</li>
 *   <li>Property existence checks</li>
 *   <li>Equality and hash code</li>
 *   <li>Thread safety considerations</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 */
@DisplayName("HTNWorldState Tests")
class HTNWorldStateTest {

    @Test
    @DisplayName("createMutable creates empty mutable state")
    void testCreateMutable() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertFalse(state.isImmutable());
        assertTrue(state.isEmpty());
        assertEquals(0, state.size());
    }

    @Test
    @DisplayName("withProperty creates state with single property")
    void testWithProperty() {
        HTNWorldState state = HTNWorldState.withProperty("hasWood", true);

        assertFalse(state.isImmutable());
        assertEquals(1, state.size());
        assertTrue(state.getBoolean("hasWood"));
    }

    @Test
    @DisplayName("Builder creates mutable state by default")
    void testBuilderCreatesMutable() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .property("woodCount", 64)
            .build();

        assertFalse(state.isImmutable());
        assertEquals(2, state.size());
    }

    @Test
    @DisplayName("Builder buildImmutable creates immutable state")
    void testBuilderBuildImmutable() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        assertTrue(state.isImmutable());
    }

    @Test
    @DisplayName("Builder buildMutable creates mutable state")
    void testBuilderBuildMutable() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .buildMutable();

        assertFalse(state.isImmutable());
    }

    @Test
    @DisplayName("Set property on mutable state")
    void testSetProperty() {
        HTNWorldState state = HTNWorldState.createMutable();

        state.setProperty("hasWood", true);
        state.setProperty("woodCount", 64);

        assertEquals(2, state.size());
        assertTrue(state.getBoolean("hasWood"));
        assertEquals(64, state.getInt("woodCount"));
    }

    @Test
    @DisplayName("Set property on immutable state throws")
    void testSetPropertyOnImmutableThrows() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        assertThrows(IllegalStateException.class,
            () -> state.setProperty("woodCount", 64),
            "Cannot modify immutable snapshot");
    }

    @Test
    @DisplayName("Set properties from map")
    void testSetProperties() {
        HTNWorldState state = HTNWorldState.createMutable();

        Map<String, Object> props = Map.of(
            "hasWood", true,
            "woodCount", 64,
            "toolType", "diamond_axe"
        );

        state.setProperties(props);

        assertEquals(3, state.size());
        assertTrue(state.getBoolean("hasWood"));
        assertEquals(64, state.getInt("woodCount"));
        assertEquals("diamond_axe", state.getString("toolType"));
    }

    @Test
    @DisplayName("Set properties with null map does nothing")
    void testSetPropertiesNull() {
        HTNWorldState state = HTNWorldState.withProperty("hasWood", true);

        state.setProperties(null);

        assertEquals(1, state.size());
    }

    @Test
    @DisplayName("Remove property from mutable state")
    void testRemoveProperty() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("hasWood", true);
        state.setProperty("woodCount", 64);

        state.removeProperty("woodCount");

        assertEquals(1, state.size());
        assertTrue(state.hasProperty("hasWood"));
        assertFalse(state.hasProperty("woodCount"));
    }

    @Test
    @DisplayName("Remove property from immutable state throws")
    void testRemovePropertyOnImmutableThrows() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        assertThrows(IllegalStateException.class,
            () -> state.removeProperty("hasWood"),
            "Cannot modify immutable snapshot");
    }

    @Test
    @DisplayName("Clear removes all properties")
    void testClear() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("hasWood", true);
        state.setProperty("woodCount", 64);

        state.clear();

        assertTrue(state.isEmpty());
        assertEquals(0, state.size());
    }

    @Test
    @DisplayName("Clear on immutable state throws")
    void testClearOnImmutableThrows() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        assertThrows(IllegalStateException.class,
            state::clear,
            "Cannot modify immutable snapshot");
    }

    @Test
    @DisplayName("GetProperty returns value or null")
    void testGetProperty() {
        HTNWorldState state = HTNWorldState.withProperty("hasWood", true);

        assertEquals(true, state.getProperty("hasWood"));
        assertNull(state.getProperty("missing"));
    }

    @Test
    @DisplayName("GetProperty with default returns value or default")
    void testGetPropertyWithDefault() {
        HTNWorldState state = HTNWorldState.withProperty("hasWood", true);

        assertEquals(true, state.getProperty("hasWood", false));
        assertEquals("default", state.getProperty("missing", "default"));
    }

    @Test
    @DisplayName("GetBoolean returns boolean value")
    void testGetBoolean() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("hasWood", true);
        state.setProperty("hasStone", false);

        assertTrue(state.getBoolean("hasWood"));
        assertFalse(state.getBoolean("hasStone"));
    }

    @Test
    @DisplayName("GetBoolean returns default for missing key")
    void testGetBooleanDefault() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertFalse(state.getBoolean("missing"));
        assertTrue(state.getBoolean("missing", true));
    }

    @Test
    @DisplayName("GetBoolean converts string to boolean")
    void testGetBooleanStringConversion() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("enabled", "true");
        state.setProperty("disabled", "false");

        assertTrue(state.getBoolean("enabled"));
        assertFalse(state.getBoolean("disabled"));
    }

    @Test
    @DisplayName("GetInt returns integer value")
    void testGetInt() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("woodCount", 64);
        state.setProperty("stoneCount", 128);

        assertEquals(64, state.getInt("woodCount"));
        assertEquals(128, state.getInt("stoneCount"));
    }

    @Test
    @DisplayName("GetInt returns default for missing key")
    void testGetIntDefault() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertEquals(0, state.getInt("missing"));
        assertEquals(100, state.getInt("missing", 100));
    }

    @Test
    @DisplayName("GetInt converts number types")
    void testGetIntConversion() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("longValue", 64L);
        state.setProperty("doubleValue", 32.5);
        state.setProperty("floatValue", 16.25f);

        assertEquals(64, state.getInt("longValue"));
        assertEquals(32, state.getInt("doubleValue"));
        assertEquals(16, state.getInt("floatValue"));
    }

    @Test
    @DisplayName("GetLong returns long value")
    void testGetLong() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("positionX", 100L);
        state.setProperty("positionZ", 200L);

        assertEquals(100L, state.getLong("positionX"));
        assertEquals(200L, state.getLong("positionZ"));
    }

    @Test
    @DisplayName("GetLong returns default for missing key")
    void testGetLongDefault() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertEquals(0L, state.getLong("missing"));
        assertEquals(1000L, state.getLong("missing", 1000L));
    }

    @Test
    @DisplayName("GetDouble returns double value")
    void testGetDouble() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("ratio", 0.75);
        state.setProperty("pi", 3.14159);

        assertEquals(0.75, state.getDouble("ratio"), 0.001);
        assertEquals(3.14159, state.getDouble("pi"), 0.00001);
    }

    @Test
    @DisplayName("GetDouble returns default for missing key")
    void testGetDoubleDefault() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertEquals(0.0, state.getDouble("missing"), 0.001);
        assertEquals(1.5, state.getDouble("missing", 1.5), 0.001);
    }

    @Test
    @DisplayName("GetString returns string value")
    void testGetString() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("toolType", "diamond_axe");
        state.setProperty("material", "oak_planks");

        assertEquals("diamond_axe", state.getString("toolType"));
        assertEquals("oak_planks", state.getString("material"));
    }

    @Test
    @DisplayName("GetString returns default for missing key")
    void testGetStringDefault() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertNull(state.getString("missing"));
        assertEquals("default", state.getString("missing", "default"));
    }

    @Test
    @DisplayName("GetString converts non-string to string")
    void testGetStringConversion() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("count", 64);
        state.setProperty("enabled", true);

        assertEquals("64", state.getString("count"));
        assertEquals("true", state.getString("enabled"));
    }

    @Test
    @DisplayName("HasProperty checks property existence")
    void testHasProperty() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("hasWood", true);
        state.setProperty("woodCount", 64);

        assertTrue(state.hasProperty("hasWood"));
        assertTrue(state.hasProperty("woodCount"));
        assertFalse(state.hasProperty("missing"));
    }

    @Test
    @DisplayName("HasProperties checks multiple properties")
    void testHasProperties() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("hasWood", true);
        state.setProperty("woodCount", 64);
        state.setProperty("hasStone", true);

        assertTrue(state.hasProperties("hasWood", "woodCount"));
        assertTrue(state.hasProperties("hasWood", "woodCount", "hasStone"));
        assertFalse(state.hasProperties("hasWood", "woodCount", "missing"));
        assertFalse(state.hasProperties("missing"));
    }

    @Test
    @DisplayName("GetPropertyNames returns all property keys")
    void testGetPropertyNames() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("hasWood", true);
        state.setProperty("woodCount", 64);
        state.setProperty("toolType", "axe");

        Set<String> names = state.getPropertyNames();

        assertEquals(3, names.size());
        assertTrue(names.contains("hasWood"));
        assertTrue(names.contains("woodCount"));
        assertTrue(names.contains("toolType"));
    }

    @Test
    @DisplayName("GetPropertyNames returns unmodifiable set")
    void testGetPropertyNamesUnmodifiable() {
        HTNWorldState state = HTNWorldState.withProperty("hasWood", true);

        Set<String> names = state.getPropertyNames();

        assertThrows(UnsupportedOperationException.class,
            () -> names.add("newProp"),
            "Property names set should be unmodifiable");
    }

    @Test
    @DisplayName("Size returns property count")
    void testSize() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertEquals(0, state.size());

        state.setProperty("prop1", "value1");
        assertEquals(1, state.size());

        state.setProperty("prop2", "value2");
        assertEquals(2, state.size());

        state.removeProperty("prop1");
        assertEquals(1, state.size());
    }

    @Test
    @DisplayName("IsEmpty checks for empty state")
    void testIsEmpty() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertTrue(state.isEmpty());

        state.setProperty("hasWood", true);

        assertFalse(state.isEmpty());
    }

    @Test
    @DisplayName("IsImmutable checks immutability")
    void testIsImmutable() {
        HTNWorldState mutable = HTNWorldState.createMutable();
        HTNWorldState immutable = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        assertFalse(mutable.isImmutable());
        assertTrue(immutable.isImmutable());
    }

    @Test
    @DisplayName("Snapshot creates immutable copy")
    void testSnapshot() {
        HTNWorldState original = HTNWorldState.createMutable();
        original.setProperty("hasWood", true);
        original.setProperty("woodCount", 64);

        HTNWorldState snapshot = original.snapshot();

        assertEquals(original.getPropertyNames(), snapshot.getPropertyNames());
        assertEquals(original.getBoolean("hasWood"), snapshot.getBoolean("hasWood"));
        assertEquals(original.getInt("woodCount"), snapshot.getInt("woodCount"));
        assertTrue(snapshot.isImmutable());
    }

    @Test
    @DisplayName("Snapshot is independent of original")
    void testSnapshotIndependent() {
        HTNWorldState original = HTNWorldState.createMutable();
        original.setProperty("hasWood", true);

        HTNWorldState snapshot = original.snapshot();

        // Original can still be modified
        original.setProperty("woodCount", 64);

        assertTrue(snapshot.hasProperty("hasWood"));
        assertFalse(snapshot.hasProperty("woodCount"));
    }

    @Test
    @DisplayName("CopyMutable creates mutable copy")
    void testCopyMutable() {
        HTNWorldState original = HTNWorldState.createMutable();
        original.setProperty("hasWood", true);
        original.setProperty("woodCount", 64);

        HTNWorldState copy = original.copyMutable();

        assertEquals(original.getPropertyNames(), copy.getPropertyNames());
        assertEquals(original.getBoolean("hasWood"), copy.getBoolean("hasWood"));
        assertEquals(original.getInt("woodCount"), copy.getInt("woodCount"));
        assertFalse(copy.isImmutable());
    }

    @Test
    @DisplayName("CopyMutable is independent of original")
    void testCopyMutableIndependent() {
        HTNWorldState original = HTNWorldState.createMutable();
        original.setProperty("hasWood", true);

        HTNWorldState copy = original.copyMutable();

        // Both can be modified independently
        original.setProperty("woodCount", 64);
        copy.setProperty("stoneCount", 128);

        assertTrue(original.hasProperty("woodCount"));
        assertFalse(original.hasProperty("stoneCount"));
        assertFalse(copy.hasProperty("woodCount"));
        assertTrue(copy.hasProperty("stoneCount"));
    }

    @Test
    @DisplayName("CopyMutable from immutable state")
    void testCopyMutableFromImmutable() {
        HTNWorldState immutable = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        HTNWorldState mutable = immutable.copyMutable();

        assertFalse(mutable.isImmutable());
        assertTrue(mutable.hasProperty("hasWood"));

        // Mutable copy can be modified
        mutable.setProperty("woodCount", 64);
        assertTrue(mutable.hasProperty("woodCount"));
    }

    @Test
    @DisplayName("Equality based on properties")
    void testEquality() {
        HTNWorldState state1 = HTNWorldState.builder()
            .property("hasWood", true)
            .property("woodCount", 64)
            .build();

        HTNWorldState state2 = HTNWorldState.builder()
            .property("hasWood", true)
            .property("woodCount", 64)
            .build();

        assertEquals(state1, state2);
        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    @DisplayName("Inequality when properties differ")
    void testInequality() {
        HTNWorldState state1 = HTNWorldState.withProperty("hasWood", true);
        HTNWorldState state2 = HTNWorldState.withProperty("hasWood", false);

        assertNotEquals(state1, state2);
    }

    @Test
    @DisplayName("Equality ignores immutability")
    void testEqualityIgnoresImmutability() {
        HTNWorldState mutable = HTNWorldState.builder()
            .property("hasWood", true)
            .buildMutable();

        HTNWorldState immutable = HTNWorldState.builder()
            .property("hasWood", true)
            .buildImmutable();

        assertEquals(mutable, immutable,
            "Immutability should not affect equality");
    }

    @Test
    @DisplayName("HashCode is cached")
    void testHashCodeCached() {
        HTNWorldState state = HTNWorldState.withProperty("hasWood", true);

        int hash1 = state.hashCode();
        int hash2 = state.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("ToString contains properties and immutability")
    void testToString() {
        HTNWorldState state = HTNWorldState.builder()
            .property("hasWood", true)
            .property("woodCount", 64)
            .buildImmutable();

        String str = state.toString();

        assertTrue(str.contains("hasWood"));
        assertTrue(str.contains("woodCount"));
        assertTrue(str.contains("immutable=true"));
    }

    @Test
    @DisplayName("Overwrite property value")
    void testOverwriteProperty() {
        HTNWorldState state = HTNWorldState.createMutable();

        state.setProperty("woodCount", 64);
        assertEquals(64, state.getInt("woodCount"));

        state.setProperty("woodCount", 128);
        assertEquals(128, state.getInt("woodCount"));
        assertEquals(1, state.size());
    }

    @Test
    @DisplayName("Builder property type overloads")
    void testBuilderPropertyOverloads() {
        HTNWorldState state = HTNWorldState.builder()
            .property("boolValue", true)
            .property("intValue", 42)
            .property("longValue", 1000L)
            .property("doubleValue", 3.14)
            .property("stringValue", "test")
            .build();

        assertTrue(state.getBoolean("boolValue"));
        assertEquals(42, state.getInt("intValue"));
        assertEquals(1000L, state.getLong("longValue"));
        assertEquals(3.14, state.getDouble("doubleValue"), 0.001);
        assertEquals("test", state.getString("stringValue"));
    }

    @Test
    @DisplayName("Null property value is stored")
    void testNullPropertyValue() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("optional", null);

        assertTrue(state.hasProperty("optional"));
        assertNull(state.getProperty("optional"));
    }

    @Test
    @DisplayName("Complex object as property value")
    void testComplexObjectValue() {
        class CustomObject {
            final String value;
            CustomObject(String value) { this.value = value; }
        }

        CustomObject custom = new CustomObject("test");

        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("custom", custom);

        assertSame(custom, state.getProperty("custom"));
    }

    @Test
    @DisplayName("Generic getProperty with type parameter")
    void testGenericGetProperty() {
        HTNWorldState state = HTNWorldState.createMutable();
        state.setProperty("count", 64);

        Integer count = state.getProperty("count", 0);
        assertEquals(64, count);

        String missing = state.getProperty("missing", "default");
        assertEquals("default", missing);
    }

    @Test
    @DisplayName("Builder can be reused")
    void testBuilderReuse() {
        HTNWorldState.Builder builder = HTNWorldState.builder()
            .property("hasWood", true);

        HTNWorldState state1 = builder.property("woodCount", 64).build();
        HTNWorldState state2 = builder.property("woodCount", 128).build();

        assertEquals(64, state1.getInt("woodCount"));
        assertEquals(128, state2.getInt("woodCount"));
        assertTrue(state1.getBoolean("hasWood"));
        assertTrue(state2.getBoolean("hasWood"));
    }

    @Test
    @DisplayName("Snapshot of snapshot is immutable")
    void testSnapshotOfSnapshot() {
        HTNWorldState original = HTNWorldState.withProperty("hasWood", true);
        HTNWorldState snapshot1 = original.snapshot();
        HTNWorldState snapshot2 = snapshot1.snapshot();

        assertTrue(snapshot1.isImmutable());
        assertTrue(snapshot2.isImmutable());
        assertEquals(snapshot1, snapshot2);
    }

    @Test
    @DisplayName("Empty state is valid")
    void testEmptyState() {
        HTNWorldState state = HTNWorldState.createMutable();

        assertTrue(state.isEmpty());
        assertEquals(0, state.size());
        assertEquals(0, state.getPropertyNames().size());
        assertFalse(state.hasProperty("anything"));
    }
}
