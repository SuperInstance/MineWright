package com.minewright.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for NBTSerializer utility.
 *
 * <p>Tests cover all supported types including primitives, objects, collections,
 * atomic types, enums, nested objects, and edge cases.</p>
 *
 * @since 1.4.0
 */
@DisplayName("NBTSerializer Tests")
class NBTSerializerTest {

    private CompoundTag tag;

    @BeforeEach
    void setUp() {
        tag = new CompoundTag();
    }

    // ========================================================================
    // Primitive Types Tests
    // ========================================================================

    @Test
    @DisplayName("Should serialize and deserialize int fields")
    void testIntSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            int intValue = 42;

            @NBTField("custom_id")
            int customIntValue = 100;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(42, tag.getInt("intValue"));
        assertEquals(100, tag.getInt("custom_id"));

        TestData loaded = new TestData();
        loaded.intValue = 0;
        loaded.customIntValue = 0;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(42, loaded.intValue);
        assertEquals(100, loaded.customIntValue);
    }

    @Test
    @DisplayName("Should serialize and deserialize long fields")
    void testLongSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            long longValue = 123456789L;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(123456789L, tag.getLong("longValue"));

        TestData loaded = new TestData();
        loaded.longValue = 0L;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(123456789L, loaded.longValue);
    }

    @Test
    @DisplayName("Should serialize and deserialize float fields")
    void testFloatSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            float floatValue = 3.14f;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(3.14f, tag.getFloat("floatValue"), 0.001f);

        TestData loaded = new TestData();
        loaded.floatValue = 0.0f;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(3.14f, loaded.floatValue, 0.001f);
    }

    @Test
    @DisplayName("Should serialize and deserialize double fields")
    void testDoubleSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            double doubleValue = 2.71828;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(2.71828, tag.getDouble("doubleValue"), 0.00001);

        TestData loaded = new TestData();
        loaded.doubleValue = 0.0;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(2.71828, loaded.doubleValue, 0.00001);
    }

    @Test
    @DisplayName("Should serialize and deserialize boolean fields")
    void testBooleanSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            boolean booleanValue = true;

            @NBTField
            boolean falseValue = false;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.getBoolean("booleanValue"));
        assertFalse(tag.getBoolean("falseValue"));

        TestData loaded = new TestData();
        loaded.booleanValue = false;
        loaded.falseValue = true;

        NBTSerializer.loadFields(tag, loaded);

        assertTrue(loaded.booleanValue);
        assertFalse(loaded.falseValue);
    }

    @Test
    @DisplayName("Should serialize and deserialize short fields")
    void testShortSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            short shortValue = 32767;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(32767, tag.getShort("shortValue"));

        TestData loaded = new TestData();
        loaded.shortValue = 0;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(32767, loaded.shortValue);
    }

    @Test
    @DisplayName("Should serialize and deserialize byte fields")
    void testByteSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            byte byteValue = 127;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(127, tag.getByte("byteValue"));

        TestData loaded = new TestData();
        loaded.byteValue = 0;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(127, loaded.byteValue);
    }

    // ========================================================================
    // Object Types Tests
    // ========================================================================

    @Test
    @DisplayName("Should serialize and deserialize String fields")
    void testStringSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            String stringValue = "Hello, World!";
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals("Hello, World!", tag.getString("stringValue"));

        TestData loaded = new TestData();
        loaded.stringValue = null;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals("Hello, World!", loaded.stringValue);
    }

    @Test
    @DisplayName("Should serialize and deserialize Instant fields")
    void testInstantSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            Instant timestamp = Instant.parse("2024-01-15T12:30:45Z");
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(1705319445000L, tag.getLong("timestamp"));

        TestData loaded = new TestData();
        loaded.timestamp = null;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(Instant.parse("2024-01-15T12:30:45Z"), loaded.timestamp);
    }

    @Test
    @DisplayName("Should serialize and deserialize UUID fields")
    void testUUIDSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            UUID uuid = UUID.randomUUID();
        }

        TestData data = new TestData();
        UUID originalUuid = data.uuid;
        NBTSerializer.saveFields(tag, data);

        assertEquals(originalUuid, tag.getUUID("uuid"));

        TestData loaded = new TestData();
        loaded.uuid = null;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(originalUuid, loaded.uuid);
    }

    @Test
    @DisplayName("Should serialize and deserialize BlockPos fields")
    void testBlockPosSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            BlockPos position = new BlockPos(10, 64, -20);
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(new BlockPos(10, 64, -20), BlockPos.of(tag.getLong("position")));

        TestData loaded = new TestData();
        loaded.position = BlockPos.ZERO;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(new BlockPos(10, 64, -20), loaded.position);
    }

    // ========================================================================
    // Atomic Types Tests
    // ========================================================================

    @Test
    @DisplayName("Should serialize and deserialize AtomicInteger fields")
    void testAtomicIntegerSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            AtomicInteger counter = new AtomicInteger(42);
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(42, tag.getInt("counter"));

        TestData loaded = new TestData();
        loaded.counter.set(0);

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(42, loaded.counter.get());
    }

    @Test
    @DisplayName("Should serialize and deserialize AtomicLong fields")
    void testAtomicLongSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            AtomicLong counter = new AtomicLong(123456789L);
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals(123456789L, tag.getLong("counter"));

        TestData loaded = new TestData();
        loaded.counter.set(0L);

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(123456789L, loaded.counter.get());
    }

    @Test
    @DisplayName("Should serialize and deserialize AtomicBoolean fields")
    void testAtomicBooleanSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            AtomicBoolean flag = new AtomicBoolean(true);
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.getBoolean("flag"));

        TestData loaded = new TestData();
        loaded.flag.set(false);

        NBTSerializer.loadFields(tag, loaded);

        assertTrue(loaded.flag.get());
    }

    // ========================================================================
    // Enum Types Tests
    // ========================================================================

    @Test
    @DisplayName("Should serialize and deserialize enum fields")
    void testEnumSerialization() {
        enum TestEnum {
            FIRST_VALUE, SECOND_VALUE, THIRD_VALUE
        }

        @NBTSerializable
        class TestData {
            @NBTField
            TestEnum enumValue = TestEnum.SECOND_VALUE;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertEquals("SECOND_VALUE", tag.getString("enumValue"));

        TestData loaded = new TestData();
        loaded.enumValue = TestEnum.FIRST_VALUE;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(TestEnum.SECOND_VALUE, loaded.enumValue);
    }

    // ========================================================================
    // Collection Types Tests
    // ========================================================================

    @Test
    @DisplayName("Should serialize and deserialize List<String> fields")
    void testStringListSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            List<String> stringList = new ArrayList<>(Arrays.asList("one", "two", "three"));
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("stringList"));

        TestData loaded = new TestData();
        loaded.stringList.clear();

        NBTSerializer.loadFields(tag, loaded);

        assertNotNull(loaded.stringList);
        assertFalse(loaded.stringList.isEmpty());
    }

    @Test
    @DisplayName("Should serialize and deserialize List<Integer> fields")
    void testIntegerListSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            List<Integer> intList = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("intList"));

        TestData loaded = new TestData();
        loaded.intList.clear();

        NBTSerializer.loadFields(tag, loaded);

        assertNotNull(loaded.intList);
        assertFalse(loaded.intList.isEmpty());
    }

    @Test
    @DisplayName("Should serialize and deserialize Set<String> fields")
    void testStringSetSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            Set<String> stringSet = new HashSet<>(Arrays.asList("a", "b", "c"));
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("stringSet"));

        TestData loaded = new TestData();
        loaded.stringSet.clear();

        NBTSerializer.loadFields(tag, loaded);

        assertNotNull(loaded.stringSet);
        assertFalse(loaded.stringSet.isEmpty());
    }

    @Test
    @DisplayName("Should serialize and deserialize Map<String, Integer> fields")
    void testStringIntegerMapSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            Map<String, Integer> map = new HashMap<>();
            {
                map.put("one", 1);
                map.put("two", 2);
                map.put("three", 3);
            }
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("map"));
        assertEquals(1, tag.getCompound("map").getInt("one"));
        assertEquals(2, tag.getCompound("map").getInt("two"));
        assertEquals(3, tag.getCompound("map").getInt("three"));

        TestData loaded = new TestData();
        loaded.map.clear();

        NBTSerializer.loadFields(tag, loaded);

        assertNotNull(loaded.map);
        assertEquals(3, loaded.map.size());
        assertEquals(1, loaded.map.get("one"));
        assertEquals(2, loaded.map.get("two"));
        assertEquals(3, loaded.map.get("three"));
    }

    @Test
    @DisplayName("Should serialize and deserialize Map<String, String> fields")
    void testStringStringMapSerialization() {
        @NBTSerializable
        class TestData {
            @NBTField
            Map<String, String> map = new HashMap<>();
            {
                map.put("key1", "value1");
                map.put("key2", "value2");
            }
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("map"));
        assertEquals("value1", tag.getCompound("map").getString("key1"));
        assertEquals("value2", tag.getCompound("map").getString("key2"));

        TestData loaded = new TestData();
        loaded.map.clear();

        NBTSerializer.loadFields(tag, loaded);

        assertNotNull(loaded.map);
        assertEquals(2, loaded.map.size());
        assertEquals("value1", loaded.map.get("key1"));
        assertEquals("value2", loaded.map.get("key2"));
    }

    // ========================================================================
    // Nested Object Tests
    // ========================================================================

    @Test
    @DisplayName("Should serialize and deserialize nested objects")
    void testNestedObjectSerialization() {
        @NBTSerializable
        class InnerData {
            @NBTField
            String name = "Inner";

            @NBTField
            int value = 100;
        }

        @NBTSerializable
        class OuterData {
            @NBTField
            String outerName = "Outer";

            @NBTField
            InnerData inner = new InnerData();
        }

        OuterData data = new OuterData();
        NBTSerializer.saveFields(tag, data);

        assertEquals("Outer", tag.getString("outerName"));
        assertTrue(tag.contains("inner"));

        CompoundTag innerTag = tag.getCompound("inner");
        assertEquals("Inner", innerTag.getString("name"));
        assertEquals(100, innerTag.getInt("value"));

        OuterData loaded = new OuterData();
        loaded.inner = new InnerData();
        loaded.inner.name = null;
        loaded.inner.value = 0;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals("Outer", loaded.outerName);
        assertNotNull(loaded.inner);
        assertEquals("Inner", loaded.inner.name);
        assertEquals(100, loaded.inner.value);
    }

    @Test
    @DisplayName("Should use saveNested and loadNested for nested objects")
    void testSaveLoadNestedMethods() {
        @NBTSerializable
        class InnerData {
            @NBTField
            String message = "Nested message";

            @NBTField
            int count = 42;
        }

        InnerData data = new InnerData();
        NBTSerializer.saveNested(tag, "innerData", data);

        assertTrue(tag.contains("innerData"));
        CompoundTag innerTag = tag.getCompound("innerData");
        assertEquals("Nested message", innerTag.getString("message"));
        assertEquals(42, innerTag.getInt("count"));

        InnerData loaded = new InnerData();
        loaded.message = null;
        loaded.count = 0;

        NBTSerializer.loadNested(tag, "innerData", loaded);

        assertEquals("Nested message", loaded.message);
        assertEquals(42, loaded.count);
    }

    // ========================================================================
    // Optional Fields Tests
    // ========================================================================

    @Test
    @DisplayName("Should skip optional fields when missing during load")
    void testOptionalFieldMissing() {
        @NBTSerializable
        class TestData {
            @NBTField
            String requiredField = "required";

            @NBTField(required = false)
            String optionalField = "optional";
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        // Remove optional field
        tag.remove("optionalField");

        TestData loaded = new TestData();
        loaded.requiredField = null;
        loaded.optionalField = null;

        // Should not throw exception
        NBTSerializer.loadFields(tag, loaded);

        assertEquals("required", loaded.requiredField);
        // Optional field should remain null or default
        assertNull(loaded.optionalField);
    }

    @Test
    @DisplayName("Should throw exception for missing required fields")
    void testRequiredFieldMissing() {
        @NBTSerializable
        class TestData {
            @NBTField
            String requiredField = "required";
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        // Remove required field
        tag.remove("requiredField");

        TestData loaded = new TestData();
        loaded.requiredField = null;

        // Should throw exception
        assertThrows(NBTSerializer.NBTSerializationException.class, () -> {
            NBTSerializer.loadFields(tag, loaded);
        });
    }

    @Test
    @DisplayName("Should use custom NBT key from annotation")
    void testCustomNBTKey() {
        @NBTSerializable
        class TestData {
            @NBTField("custom_key")
            String value = "test";
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("custom_key"));
        assertFalse(tag.contains("value"));
        assertEquals("test", tag.getString("custom_key"));

        TestData loaded = new TestData();
        loaded.value = null;

        NBTSerializer.loadFields(tag, loaded);

        assertEquals("test", loaded.value);
    }

    // ========================================================================
    // Edge Cases and Error Handling Tests
    // ========================================================================

    @Test
    @DisplayName("Should handle null values for optional fields")
    void testNullOptionalField() {
        @NBTSerializable
        class TestData {
            @NBTField(required = false)
            String nullableField = null;
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        // Null optional field should not be saved
        assertFalse(tag.contains("nullableField"));

        TestData loaded = new TestData();
        loaded.nullableField = "default";

        // Loading should preserve the default value since field is missing
        NBTSerializer.loadFields(tag, loaded);

        assertEquals("default", loaded.nullableField);
    }

    @Test
    @DisplayName("Should throw exception for null tag parameter")
    void testNullTagParameter() {
        @NBTSerializable
        class TestData {
            @NBTField
            String value = "test";
        }

        TestData data = new TestData();

        assertThrows(NullPointerException.class, () -> {
            NBTSerializer.saveFields(null, data);
        });

        assertThrows(NullPointerException.class, () -> {
            NBTSerializer.loadFields(null, data);
        });
    }

    @Test
    @DisplayName("Should throw exception for null object parameter")
    void testNullObjectParameter() {
        assertThrows(NullPointerException.class, () -> {
            NBTSerializer.saveFields(tag, null);
        });

        assertThrows(NullPointerException.class, () -> {
            NBTSerializer.loadFields(tag, null);
        });
    }

    @Test
    @DisplayName("Should handle empty class with no annotated fields")
    void testEmptyClass() {
        @NBTSerializable
        class EmptyData {
            // No @NBTField annotations
            String plainField = "not serialized";
        }

        EmptyData data = new EmptyData();
        NBTSerializer.saveFields(tag, data);

        // No fields should be saved
        assertTrue(tag.isEmpty());

        EmptyData loaded = new EmptyData();
        loaded.plainField = "unchanged";

        NBTSerializer.loadFields(tag, loaded);

        // Plain field should remain unchanged
        assertEquals("unchanged", loaded.plainField);
    }

    @Test
    @DisplayName("Should skip static and transient fields")
    void testSkipStaticAndTransientFields() {
        @NBTSerializable
        class TestData {
            @NBTField
            String instanceField = "instance";

            @NBTField
            static String staticField = "static";

            @NBTField
            transient String transientField = "transient";
        }

        TestData data = new TestData();
        NBTSerializer.saveFields(tag, data);

        // Only instance field should be saved
        assertTrue(tag.contains("instanceField"));
        assertFalse(tag.contains("staticField"));
        assertFalse(tag.contains("transientField"));
    }

    @Test
    @DisplayName("Should handle complex nested structures")
    void testComplexNestedStructure() {
        @NBTSerializable
        class Level3Data {
            @NBTField
            int level3Value = 333;
        }

        @NBTSerializable
        class Level2Data {
            @NBTField
            int level2Value = 22;

            @NBTField
            Level3Data level3 = new Level3Data();
        }

        @NBTSerializable
        class Level1Data {
            @NBTField
            int level1Value = 1;

            @NBTField
            Level2Data level2 = new Level2Data();
        }

        Level1Data data = new Level1Data();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.contains("level1Value"));
        assertTrue(tag.contains("level2"));

        CompoundTag level2Tag = tag.getCompound("level2");
        assertTrue(level2Tag.contains("level2Value"));
        assertTrue(level2Tag.contains("level3"));

        CompoundTag level3Tag = level2Tag.getCompound("level3");
        assertTrue(level3Tag.contains("level3Value"));

        Level1Data loaded = new Level1Data();
        loaded.level1Value = 0;
        loaded.level2 = new Level2Data();

        NBTSerializer.loadFields(tag, loaded);

        assertEquals(1, loaded.level1Value);
        assertNotNull(loaded.level2);
        assertEquals(22, loaded.level2.level2Value);
        assertNotNull(loaded.level2.level3);
        assertEquals(333, loaded.level2.level3.level3Value);
    }

    // ========================================================================
    // Real-World Scenario Tests
    // ========================================================================

    @Test
    @DisplayName("Should handle player data serialization")
    void testPlayerDataSerialization() {
        @NBTSerializable
        class PlayerStats {
            @NBTField
            int health = 20;

            @NBTField
            int maxHealth = 20;

            @NBTField
            int experience = 0;

            @NBTField
            int level = 1;
        }

        @NBTSerializable
        class PlayerData {
            @NBTField
            String playerName = "Steve";

            @NBTField
            UUID playerUuid = UUID.randomUUID();

            @NBTField
            BlockPos lastPosition = new BlockPos(100, 64, -200);

            @NBTField
            Instant lastLoginTime = Instant.now();

            @NBTField
            PlayerStats stats = new PlayerStats();

            @NBTField(required = false)
            String nickname = null;
        }

        PlayerData data = new PlayerData();
        NBTSerializer.saveFields(tag, data);

        assertEquals("Steve", tag.getString("playerName"));
        assertEquals(data.playerUuid, tag.getUUID("playerUuid"));
        assertTrue(tag.contains("lastPosition"));
        assertTrue(tag.contains("lastLoginTime"));
        assertTrue(tag.contains("stats"));
        assertFalse(tag.contains("nickname")); // null optional field

        PlayerData loaded = new PlayerData();
        loaded.playerName = null;
        loaded.playerUuid = null;
        loaded.lastPosition = BlockPos.ZERO;
        loaded.lastLoginTime = null;
        loaded.stats = new PlayerStats();

        NBTSerializer.loadFields(tag, loaded);

        assertEquals("Steve", loaded.playerName);
        assertEquals(data.playerUuid, loaded.playerUuid);
        assertEquals(new BlockPos(100, 64, -200), loaded.lastPosition);
        assertNotNull(loaded.lastLoginTime);
        assertEquals(20, loaded.stats.health);
        assertEquals(1, loaded.stats.level);
        assertNull(loaded.nickname);
    }

    @Test
    @DisplayName("Should handle configuration data serialization")
    void testConfigDataSerialization() {
        @NBTSerializable
        class ConfigData {
            @NBTField
            boolean debugMode = true;

            @NBTField
            int maxConnections = 100;

            @NBTField
            double timeoutSeconds = 30.0;

            @NBTField
            List<String> serverUrls = new ArrayList<>(Arrays.asList(
                "http://server1.example.com",
                "http://server2.example.com"
            ));

            @NBTField
            Map<String, String> properties = new HashMap<>();
            {
                properties.put("key1", "value1");
                properties.put("key2", "value2");
            }
        }

        ConfigData data = new ConfigData();
        NBTSerializer.saveFields(tag, data);

        assertTrue(tag.getBoolean("debugMode"));
        assertEquals(100, tag.getInt("maxConnections"));
        assertEquals(30.0, tag.getDouble("timeoutSeconds"), 0.001);
        assertTrue(tag.contains("serverUrls"));
        assertTrue(tag.contains("properties"));

        ConfigData loaded = new ConfigData();
        loaded.debugMode = false;
        loaded.maxConnections = 0;
        loaded.timeoutSeconds = 0.0;
        loaded.serverUrls.clear();
        loaded.properties.clear();

        NBTSerializer.loadFields(tag, loaded);

        assertTrue(loaded.debugMode);
        assertEquals(100, loaded.maxConnections);
        assertEquals(30.0, loaded.timeoutSeconds, 0.001);
        assertNotNull(loaded.serverUrls);
        assertFalse(loaded.serverUrls.isEmpty());
        assertNotNull(loaded.properties);
        assertEquals(2, loaded.properties.size());
    }
}
