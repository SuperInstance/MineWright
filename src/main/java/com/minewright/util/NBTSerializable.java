package com.minewright.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type-level annotation to mark classes as NBT-serializable.
 *
 * <p>When a class is annotated with {@code @NBTSerializable}, it indicates that
 * the class is designed to work with {@link NBTSerializer} for automatic NBT
 * serialization and deserialization of its fields.</p>
 *
 * <p>This annotation serves two purposes:</p>
 * <ol>
 *   <li><b>Documentation:</b> Clearly indicates that the class uses NBTSerializer</li>
 *   <li><b>Nested Objects:</b> Allows nested objects to be serialized automatically</li>
 * </ol>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Simple class with automatic serialization
 * @NBTSerializable
 * public class PlayerStats {
 *     @NBTField private int health;
 *     @NBTField private int mana;
 *     @NBTField private int experience;
 *
 *     public void saveToNBT(CompoundTag tag) {
 *         NBTSerializer.saveFields(tag, this);
 *     }
 *
 *     public void loadFromNBT(CompoundTag tag) {
 *         NBTSerializer.loadFields(tag, this);
 *     }
 * }
 *
 * // Class with nested serializable object
 * @NBTSerializable
 * public class PlayerData {
 *     @NBTField private String name;
 *     @NBTField private UUID uuid;
 *
 *     // Automatically serializes all fields in PlayerStats
 *     @NBTField
 *     private PlayerStats stats;
 *
 *     public void saveToNBT(CompoundTag tag) {
 *         NBTSerializer.saveFields(tag, this);
 *         // Saves:
 *         // tag.putString("name", name);
 *         // tag.putUUID("uuid", uuid);
 *         // CompoundTag statsTag = new CompoundTag();
 *         // stats.saveToNBT(statsTag);
 *         // tag.put("stats", statsTag);
 *     }
 *
 *     public void loadFromNBT(CompoundTag tag) {
 *         NBTSerializer.loadFields(tag, this);
 *         // Loads all nested fields automatically
 *     }
 * }
 * }</pre>
 *
 * <p><b>Nested Objects:</b></p>
 * <p>When a field is itself a {@code @NBTSerializable} class, NBTSerializer will
 * automatically serialize it as a nested CompoundTag. This allows for complex
 * object graphs with automatic save/load support.</p>
 *
 * <p><b>Inheritance:</b></p>
 * <p>NBTSerializer scans the entire class hierarchy, so fields from parent classes
 * are also serialized. Parent classes don't need to be annotated if only the child
 * class uses automatic serialization, but annotating them improves documentation.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Classes marked with this annotation should be thread-safe if instances are
 * shared across threads. NBTSerializer uses reflection which is synchronized,
 * but concurrent access to the same object instance requires proper synchronization.</p>
 *
 * @see NBTField
 * @see NBTSerializer
 * @since 1.4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NBTSerializable {

    /**
     * Version identifier for the serialization format.
     *
     * <p>This can be used to handle backwards compatibility when the NBT format
     * changes. Classes can check this version during loading to apply migration
     * logic for older data formats.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Version 1 of the format
     * @NBTSerializable(version = 1)
     * public class PlayerData {
     *     @NBTField private String name;
     * }
     *
     * // Version 2 adds a new field
     * @NBTSerializable(version = 2)
     * public class PlayerData {
     *     @NBTField private String name;
     *     @NBTField(required = false)  // New field, optional for backwards compat
     *     private String displayName;
     *
     *     public void loadFromNBT(CompoundTag tag) {
     *         int version = tag.getInt("serializationVersion");
     *         if (version < 2) {
     *             // Migrate from version 1: set displayName = name
     *             this.displayName = this.name;
     *         }
     *         NBTSerializer.loadFields(tag, this);
     *     }
     * }
     * }</pre>
     *
     * @return The version number of this serialization format
     */
    int version() default 1;
}
