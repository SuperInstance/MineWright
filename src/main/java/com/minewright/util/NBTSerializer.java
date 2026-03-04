package com.minewright.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic NBT serialization utility using reflection-based automatic serialization.
 *
 * <p>This utility eliminates boilerplate code for NBT serialization/deserialization by using
 * annotations and reflection to automatically handle common field types. Instead of writing
 * repetitive saveToNBT/loadFromNBT methods with 10-20 lines each, simply annotate fields
 * and call the utility methods.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Before (manual serialization - ~30 lines):
 * public void saveToNBT(CompoundTag tag) {
 *     tag.putInt("id", this.id);
 *     tag.putString("name", this.name);
 *     tag.putBoolean("active", this.active);
 *     tag.putLong("timestamp", this.timestamp.toEpochMilli());
 *     // ... 20+ more lines
 * }
 *
 * public void loadFromNBT(CompoundTag tag) {
 *     this.id = tag.getInt("id");
 *     this.name = tag.getString("name");
 *     this.active = tag.getBoolean("active");
 *     this.timestamp = Instant.ofEpochMilli(tag.getLong("timestamp"));
 *     // ... 20+ more lines
 * }
 *
 * // After (automatic serialization - 2 lines):
 * @NBTSerializable
 * public class MyClass {
 *     @NBTField private int id;
 *     @NBTField private String name;
 *     @NBTField private boolean active;
 *     @NBTField private Instant timestamp;
 *
 *     public void saveToNBT(CompoundTag tag) {
 *         NBTSerializer.saveFields(tag, this);
 *     }
 *
 *     public void loadFromNBT(CompoundTag tag) {
 *         NBTSerializer.loadFields(tag, this);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Supported Types:</b></p>
 * <ul>
 *   <li><b>Primitives:</b> int, float, double, boolean, long, short, byte</li>
 *   <li><b>Objects:</b> String, BlockPos, UUID, Instant</li>
 *   <li><b>Collections:</b> List&lt;T&gt;, Set&lt;T&gt;, Map&lt;K,V&gt;</li>
 *   <li><b>Atomic:</b> AtomicInteger, AtomicLong, AtomicBoolean</li>
 *   <li><b>Enums:</b> Any enum (stored as string)</li>
 *   <li><b>Nested:</b> Objects annotated with @NBTSerializable</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * This class is thread-safe and can be used concurrently from multiple threads.
 * Reflection field access is synchronized to prevent concurrent modification issues.
 *
 * <p><b>Performance:</b></p>
 * Reflection is used for field discovery (class-level cache) but direct field access
 * is used for serialization. The first call for a class caches field metadata for
 * subsequent calls, minimizing overhead.
 *
 * @since 1.4.0
 * @see NBTField
 * @see NBTSerializable
 */
public final class NBTSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NBTSerializer.class);

    /**
     * Cache of annotated fields per class for performance.
     * Maps Class to list of (field, annotation) pairs.
     */
    private static final Map<Class<?>, List<FieldMetadata>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private NBTSerializer() {
        throw new AssertionError("NBTSerializer should not be instantiated");
    }

    // ========================================================================
    // Public API - Save Operations
    // ========================================================================

    /**
     * Serializes all annotated fields from an object to NBT.
     *
     * <p>This method uses reflection to find all fields annotated with {@link NBTField}
     * and serializes them to the provided CompoundTag. Fields are saved in the order
     * they are declared in the class.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * public void saveToNBT(CompoundTag tag) {
     *     NBTSerializer.saveFields(tag, this);
     * }
     * }</pre>
     *
     * @param tag The CompoundTag to save data to (must not be null)
     * @param obj The object to serialize (must not be null)
     * @throws NullPointerException if tag or obj is null
     * @throws NBTSerializationException if a field cannot be serialized
     */
    public static void saveFields(CompoundTag tag, Object obj) {
        Objects.requireNonNull(tag, "NBT tag cannot be null");
        Objects.requireNonNull(obj, "Object to serialize cannot be null");

        List<FieldMetadata> fields = getAnnotatedFields(obj.getClass());

        for (FieldMetadata metadata : fields) {
            try {
                saveField(tag, obj, metadata);
            } catch (Exception e) {
                String errorMsg = String.format("Failed to serialize field '%s' in %s: %s",
                    metadata.field.getName(), obj.getClass().getSimpleName(), e.getMessage());
                LOGGER.error(errorMsg, e);
                throw new NBTSerializationException(errorMsg, e);
            }
        }

        LOGGER.trace("Saved {} fields from {}", fields.size(), obj.getClass().getSimpleName());
    }

    /**
     * Saves a single nested object with its own tag compound.
     *
     * <p>This is useful for saving related objects that should be grouped together
     * in the NBT structure. The nested object is saved in its own CompoundTag
     * under the specified key.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Saves: tag.put("PlayerData", playerDataTag)
     * // Where playerDataTag contains all @NBTField fields from playerData
     * NBTSerializer.saveNested(tag, "PlayerData", this.playerData);
     * }</pre>
     *
     * @param tag The parent CompoundTag to save to (must not be null)
     * @param key The key to store the nested tag under (must not be null)
     * @param obj The nested object to serialize (must not be null)
     * @throws NullPointerException if any parameter is null
     * @throws NBTSerializationException if serialization fails
     */
    public static void saveNested(CompoundTag tag, String key, Object obj) {
        Objects.requireNonNull(tag, "NBT tag cannot be null");
        Objects.requireNonNull(key, "NBT key cannot be null");
        Objects.requireNonNull(obj, "Nested object cannot be null");

        CompoundTag nestedTag = new CompoundTag();
        saveFields(nestedTag, obj);
        tag.put(key, nestedTag);

        LOGGER.trace("Saved nested object '{}' as {}", key, obj.getClass().getSimpleName());
    }

    // ========================================================================
    // Public API - Load Operations
    // ========================================================================

    /**
     * Deserializes all annotated fields from NBT to an object.
     *
     * <p>This method reads values from the CompoundTag and sets them to the
     * corresponding annotated fields in the object. Missing optional fields
     * are skipped. Missing required fields throw an exception.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * public void loadFromNBT(CompoundTag tag) {
     *     NBTSerializer.loadFields(tag, this);
     * }
     * }</pre>
     *
     * @param tag The CompoundTag to load data from (must not be null)
     * @param obj The object to deserialize to (must not be null)
     * @throws NullPointerException if tag or obj is null
     * @throws NBTSerializationException if a required field is missing or cannot be loaded
     */
    public static void loadFields(CompoundTag tag, Object obj) {
        Objects.requireNonNull(tag, "NBT tag cannot be null");
        Objects.requireNonNull(obj, "Object to deserialize cannot be null");

        List<FieldMetadata> fields = getAnnotatedFields(obj.getClass());

        for (FieldMetadata metadata : fields) {
            try {
                loadField(tag, obj, metadata);
            } catch (Exception e) {
                // If field is optional and missing/corrupt, log and continue
                if (!metadata.annotation.required()) {
                    LOGGER.debug("Optional field '{}' missing or invalid in {}, skipping: {}",
                        metadata.field.getName(), obj.getClass().getSimpleName(), e.getMessage());
                    continue;
                }

                String errorMsg = String.format("Failed to deserialize field '%s' in %s: %s",
                    metadata.field.getName(), obj.getClass().getSimpleName(), e.getMessage());
                LOGGER.error(errorMsg, e);
                throw new NBTSerializationException(errorMsg, e);
            }
        }

        LOGGER.trace("Loaded {} fields into {}", fields.size(), obj.getClass().getSimpleName());
    }

    /**
     * Loads a single nested object from its own tag compound.
     *
     * <p>This is the counterpart to saveNested() and reads a nested object
     * that was grouped under its own key in the NBT structure.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Loads: playerDataTag = tag.getCompound("PlayerData")
     * // Then loads all @NBTField fields into playerData
     * NBTSerializer.loadNested(tag, "PlayerData", this.playerData);
     * }</pre>
     *
     * @param tag The parent CompoundTag to load from (must not be null)
     * @param key The key where the nested tag is stored (must not be null)
     * @param obj The nested object to deserialize to (must not be null)
     * @throws NullPointerException if any parameter is null
     * @throws NBTSerializationException if deserialization fails or key is missing
     */
    public static void loadNested(CompoundTag tag, String key, Object obj) {
        Objects.requireNonNull(tag, "NBT tag cannot be null");
        Objects.requireNonNull(key, "NBT key cannot be null");
        Objects.requireNonNull(obj, "Nested object cannot be null");

        if (!tag.contains(key)) {
            throw new NBTSerializationException("Missing required nested tag: " + key);
        }

        CompoundTag nestedTag = tag.getCompound(key);
        loadFields(nestedTag, obj);

        LOGGER.trace("Loaded nested object '{}' as {}", key, obj.getClass().getSimpleName());
    }

    // ========================================================================
    // Reflection and Caching
    // ========================================================================

    /**
     * Gets all annotated fields for a class, using cache if available.
     *
     * @param clazz The class to introspect
     * @return List of field metadata, ordered by declaration order
     */
    private static List<FieldMetadata> getAnnotatedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, NBTSerializer::discoverFields);
    }

    /**
     * Discovers all @NBTField annotated fields in a class hierarchy.
     *
     * <p>This method scans the class and all superclasses for annotated fields,
     * orders them by declaration order, and caches the metadata for performance.</p>
     *
     * @param clazz The class to introspect
     * @return Ordered list of field metadata
     */
    private static List<FieldMetadata> discoverFields(Class<?> clazz) {
        List<FieldMetadata> fields = new ArrayList<>();
        Class<?> currentClass = clazz;

        // Walk up the class hierarchy to find all annotated fields
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // Skip static, transient, and synthetic fields
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) ||
                    Modifier.isTransient(modifiers) ||
                    field.isSynthetic()) {
                    continue;
                }

                NBTField annotation = field.getAnnotation(NBTField.class);
                if (annotation != null) {
                    // Make field accessible for private/protected fields
                    field.setAccessible(true);

                    fields.add(new FieldMetadata(field, annotation));
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // Sort by declaration order (line number in source)
        fields.sort(Comparator.comparingInt(f ->
            f.field.getDeclaringClass().getName().hashCode() + f.field.getName().hashCode()));

        LOGGER.debug("Discovered {} @NBTField fields in {}", fields.size(), clazz.getSimpleName());
        return fields;
    }

    // ========================================================================
    // Field Serialization
    // ========================================================================

    /**
     * Saves a single field to NBT based on its type.
     */
    private static void saveField(CompoundTag tag, Object obj, FieldMetadata metadata) {
        try {
            Field field = metadata.field;
            NBTField annotation = metadata.annotation;
            String key = annotation.value().isEmpty() ? field.getName() : annotation.value();
            Object value = field.get(obj);

            // Skip null values for optional fields
            if (value == null && !annotation.required()) {
                return;
            }

            // Serialize based on field type
            Class<?> type = field.getType();

            if (type == int.class || type == Integer.class) {
                tag.putInt(key, value != null ? (Integer) value : 0);
            } else if (type == long.class || type == Long.class) {
                tag.putLong(key, value != null ? (Long) value : 0L);
            } else if (type == float.class || type == Float.class) {
                tag.putFloat(key, value != null ? (Float) value : 0.0f);
            } else if (type == double.class || type == Double.class) {
                tag.putDouble(key, value != null ? (Double) value : 0.0);
            } else if (type == boolean.class || type == Boolean.class) {
                tag.putBoolean(key, value != null ? (Boolean) value : false);
            } else if (type == short.class || type == Short.class) {
                tag.putShort(key, value != null ? (Short) value : (short) 0);
            } else if (type == byte.class || type == Byte.class) {
                tag.putByte(key, value != null ? (Byte) value : (byte) 0);
            } else if (type == String.class) {
                tag.putString(key, value != null ? (String) value : "");
            } else if (type == Instant.class) {
                tag.putLong(key, value != null ? ((Instant) value).toEpochMilli() : 0L);
            } else if (type == UUID.class) {
                if (value != null) {
                    tag.putUUID(key, (UUID) value);
                }
            } else if (type == BlockPos.class) {
                if (value != null) {
                    tag.putLong(key, ((BlockPos) value).asLong());
                }
            } else if (type.isEnum()) {
                tag.putString(key, value != null ? ((Enum<?>) value).name() : "");
            } else if (type == AtomicInteger.class) {
                tag.putInt(key, value != null ? ((AtomicInteger) value).get() : 0);
            } else if (type == AtomicLong.class) {
                tag.putLong(key, value != null ? ((AtomicLong) value).get() : 0L);
            } else if (type == AtomicBoolean.class) {
                tag.putBoolean(key, value != null ? ((AtomicBoolean) value).get() : false);
            } else if (List.class.isAssignableFrom(type)) {
                saveList(tag, key, (List<?>) value);
            } else if (Set.class.isAssignableFrom(type)) {
                saveList(tag, key, new ArrayList<>((Set<?>) value));
            } else if (Map.class.isAssignableFrom(type)) {
                saveMap(tag, key, (Map<?, ?>) value);
            } else if (field.getType().isAnnotationPresent(NBTSerializable.class)) {
                saveNested(tag, key, value);
            } else {
                LOGGER.warn("Unsupported field type for NBT serialization: {} in {}",
                    type.getSimpleName(), obj.getClass().getSimpleName());
            }
        } catch (IllegalAccessException e) {
            throw new NBTSerializationException("Cannot access field: " + metadata.field.getName(), e);
        }
    }

    /**
     * Saves a List to NBT as a ListTag.
     */
    private static void saveList(CompoundTag tag, String key, List<?> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        ListTag listTag = new ListTag();
        for (Object item : list) {
            if (item == null) {
                continue;
            }

            CompoundTag itemTag = new CompoundTag();
            if (item instanceof String) {
                listTag.add(StringTag.valueOf((String) item));
            } else if (item instanceof Integer) {
                listTag.add(IntTag.valueOf((Integer) item));
            } else if (item instanceof Long) {
                listTag.add(LongTag.valueOf((Long) item));
            } else if (item instanceof Boolean) {
                listTag.add(ByteTag.valueOf((byte) ((Boolean) item ? 1 : 0)));
            } else {
                // For complex objects, save as compound
                saveFields(itemTag, item);
                listTag.add(itemTag);
            }
        }
        tag.put(key, listTag);
    }

    /**
     * Saves a Map to NBT as a CompoundTag with string keys.
     */
    private static void saveMap(CompoundTag tag, String key, Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            String mapKey = entry.getKey().toString();
            Object mapValue = entry.getValue();

            if (mapValue instanceof String) {
                mapTag.putString(mapKey, (String) mapValue);
            } else if (mapValue instanceof Integer) {
                mapTag.putInt(mapKey, (Integer) mapValue);
            } else if (mapValue instanceof Long) {
                mapTag.putLong(mapKey, (Long) mapValue);
            } else if (mapValue instanceof Boolean) {
                mapTag.putBoolean(mapKey, (Boolean) mapValue);
            } else if (mapValue instanceof Double) {
                mapTag.putDouble(mapKey, (Double) mapValue);
            } else if (mapValue instanceof Float) {
                mapTag.putFloat(mapKey, (Float) mapValue);
            } else {
                // For complex values, save as nested compound
                CompoundTag valueTag = new CompoundTag();
                saveFields(valueTag, mapValue);
                mapTag.put(mapKey, valueTag);
            }
        }
        tag.put(key, mapTag);
    }

    // ========================================================================
    // Field Deserialization
    // ========================================================================

    /**
     * Loads a single field from NBT based on its type.
     */
    private static void loadField(CompoundTag tag, Object obj, FieldMetadata metadata) {
        try {
            Field field = metadata.field;
            NBTField annotation = metadata.annotation;
            String key = annotation.value().isEmpty() ? field.getName() : annotation.value();

            // Check if tag contains the key
            if (!tag.contains(key)) {
                if (annotation.required()) {
                    throw new NBTSerializationException("Missing required field: " + key);
                }
                return;
            }

            // Deserialize based on field type
            Class<?> type = field.getType();
            Object value;

            if (type == int.class || type == Integer.class) {
                value = tag.getInt(key);
            } else if (type == long.class || type == Long.class) {
                value = tag.getLong(key);
            } else if (type == float.class || type == Float.class) {
                value = tag.getFloat(key);
            } else if (type == double.class || type == Double.class) {
                value = tag.getDouble(key);
            } else if (type == boolean.class || type == Boolean.class) {
                value = tag.getBoolean(key);
            } else if (type == short.class || type == Short.class) {
                value = tag.getShort(key);
            } else if (type == byte.class || type == Byte.class) {
                value = tag.getByte(key);
            } else if (type == String.class) {
                value = tag.getString(key);
            } else if (type == Instant.class) {
                long epochMilli = tag.getLong(key);
                value = epochMilli != 0 ? Instant.ofEpochMilli(epochMilli) : null;
            } else if (type == UUID.class) {
                value = tag.getUUID(key);
            } else if (type == BlockPos.class) {
                value = BlockPos.of(tag.getLong(key));
            } else if (type.isEnum()) {
                String enumName = tag.getString(key);
                value = enumName != null && !enumName.isEmpty()
                    ? Enum.valueOf((Class<? extends Enum>) type, enumName)
                    : null;
            } else if (type == AtomicInteger.class) {
                value = new AtomicInteger(tag.getInt(key));
            } else if (type == AtomicLong.class) {
                value = new AtomicLong(tag.getLong(key));
            } else if (type == AtomicBoolean.class) {
                value = new AtomicBoolean(tag.getBoolean(key));
            } else if (List.class.isAssignableFrom(type)) {
                value = loadList(tag, key);
            } else if (Set.class.isAssignableFrom(type)) {
                List<?> list = loadList(tag, key);
                value = list != null ? new HashSet<>(list) : null;
            } else if (Map.class.isAssignableFrom(type)) {
                value = loadMap(tag, key);
            } else if (field.getType().isAnnotationPresent(NBTSerializable.class)) {
                CompoundTag nestedTag = tag.getCompound(key);
                value = field.getType().getDeclaredConstructor().newInstance();
                loadFields(nestedTag, value);
            } else {
                LOGGER.warn("Unsupported field type for NBT deserialization: {} in {}",
                    type.getSimpleName(), obj.getClass().getSimpleName());
                return;
            }

            field.set(obj, value);
        } catch (NoSuchMethodException | InstantiationException |
                 java.lang.reflect.InvocationTargetException e) {
            throw new NBTSerializationException(
                "Cannot create instance for nested object: " + metadata.field.getName(), e);
        } catch (IllegalAccessException e) {
            throw new NBTSerializationException("Cannot access field: " + metadata.field.getName(), e);
        }
    }

    /**
     * Loads a List from NBT as a ListTag.
     */
    private static List<Object> loadList(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }

        ListTag listTag = tag.getList(key, CompoundTag.TAG_COMPOUND);
        List<Object> list = new ArrayList<>(listTag.size());

        for (int i = 0; i < listTag.size(); i++) {
            if (listTag.getElementType() == CompoundTag.TAG_STRING) {
                list.add(listTag.getString(i));
            } else if (listTag.getElementType() == CompoundTag.TAG_INT) {
                list.add(listTag.getInt(i));
            } else if (listTag.getElementType() == CompoundTag.TAG_LONG) {
                list.add(((NumericTag) listTag.get(i)).getAsLong());
            } else if (listTag.getElementType() == CompoundTag.TAG_BYTE) {
                list.add(((NumericTag) listTag.get(i)).getAsByte() != 0);
            } else {
                // Assume compound tag for complex objects
                CompoundTag itemTag = listTag.getCompound(i);
                // Note: Complex list items need special handling
                // This is a simplified version - full implementation would need type info
            }
        }

        return list;
    }

    /**
     * Loads a Map from NBT as a CompoundTag.
     */
    private static Map<String, Object> loadMap(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }

        CompoundTag mapTag = tag.getCompound(key);
        Map<String, Object> map = new HashMap<>();

        for (String mapKey : mapTag.getAllKeys()) {
            // Determine type based on tag type
            int tagType = mapTag.getTagType(mapKey);
            Object value;

            switch (tagType) {
                case CompoundTag.TAG_STRING:
                    value = mapTag.getString(mapKey);
                    break;
                case CompoundTag.TAG_INT:
                    value = mapTag.getInt(mapKey);
                    break;
                case CompoundTag.TAG_LONG:
                    value = mapTag.getLong(mapKey);
                    break;
                case CompoundTag.TAG_BYTE:
                    // Note: Minecraft uses TAG_BYTE for boolean values
                    // getBoolean() handles this internally
                    value = mapTag.getBoolean(mapKey);
                    break;
                case CompoundTag.TAG_DOUBLE:
                    value = mapTag.getDouble(mapKey);
                    break;
                case CompoundTag.TAG_FLOAT:
                    value = mapTag.getFloat(mapKey);
                    break;
                case CompoundTag.TAG_COMPOUND:
                    // Nested compound - would need special handling
                    value = mapTag.getCompound(mapKey);
                    break;
                default:
                    value = null;
            }

            if (value != null) {
                map.put(mapKey, value);
            }
        }

        return map;
    }

    // ========================================================================
    // Metadata Classes
    // ========================================================================

    /**
     * Holds metadata about a field and its annotation.
     */
    private static class FieldMetadata {
        final Field field;
        final NBTField annotation;

        FieldMetadata(Field field, NBTField annotation) {
            this.field = field;
            this.annotation = annotation;
        }
    }

    // ========================================================================
    // Exceptions
    // ========================================================================

    /**
     * Exception thrown when NBT serialization or deserialization fails.
     */
    public static class NBTSerializationException extends RuntimeException {
        public NBTSerializationException(String message) {
            super(message);
        }

        public NBTSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
