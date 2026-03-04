package com.minewright.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields for automatic NBT serialization.
 *
 * <p>When a field is annotated with {@code @NBTField}, the {@link NBTSerializer}
 * will automatically include it in save/load operations. The field can have a
 * custom NBT key name, and can be marked as optional to allow missing values.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * @NBTSerializable
 * public class PlayerData {
 *     // Simple field - uses field name as NBT key
 *     @NBTField
 *     private String playerName;
 *
 *     // Field with custom NBT key
 *     @NBTField("id")
 *     private int playerId;
 *
 *     // Optional field - won't fail if missing during load
 *     @NBTField(required = false)
 *     private String lastLoginIp;
 *
 *     // Custom key + optional
 *     @NBTField(value = "last_seen", required = false)
 *     private Instant lastSeenTime;
 * }
 * }</pre>
 *
 * <p><b>Supported Field Types:</b></p>
 * <ul>
 *   <li><b>Primitives:</b> int, float, double, boolean, long, short, byte</li>
 *   <li><b>Objects:</b> String, BlockPos, UUID, Instant</li>
 *   <li><b>Collections:</b> List&lt;T&gt;, Set&lt;T&gt;, Map&lt;K,V&gt;</li>
 *   <li><b>Atomic:</b> AtomicInteger, AtomicLong, AtomicBoolean</li>
 *   <li><b>Enums:</b> Any enum (stored as string)</li>
 *   <li><b>Nested:</b> Objects annotated with {@link NBTSerializable}</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * Annotated fields are accessed in a thread-safe manner by NBTSerializer.
 * However, the fields themselves should be thread-safe if accessed concurrently
 * (e.g., use AtomicInteger instead of int for concurrent access).</p>
 *
 * <p><b>Versioning:</b></p>
 * Use the {@code required = false} attribute for new fields to maintain
 * backwards compatibility with older NBT data that doesn't have those fields.</p>
 *
 * @see NBTSerializable
 * @see NBTSerializer
 * @since 1.4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NBTField {

    /**
     * The NBT tag name to use for this field.
     *
     * <p>If empty (default), the field name is used as the NBT key.
     * Use this to customize the NBT structure or maintain backwards
     * compatibility with existing NBT data.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Field name "playerId" -> NBT key "id"
     * @NBTField("id")
     * private int playerId;
     * }</pre>
     *
     * @return The NBT tag name, or empty string to use field name
     */
    String value() default "";

    /**
     * Whether this field is required during deserialization.
     *
     * <p>If {@code true} (default), the field must exist in the NBT data
     * during loading, and an exception will be thrown if it's missing.</p>
     *
     * <p>If {@code false}, the field is optional:
     * <ul>
     *   <li>During save: null values are skipped</li>
     *   <li>During load: missing fields are silently ignored</li>
     *   <li>During load: corrupted fields log a warning and continue</li>
     * </ul></p>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Add new fields while maintaining backwards compatibility</li>
     *   <li>Allow optional features or extensions</li>
     *   <li>Handle gracefully missing data from older versions</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Required field (default)
     * @NBTField(required = true)
     * private String playerName;  // Must exist in NBT
     *
     * // Optional field
     * @NBTField(required = false)
     * private String nickname;  // Can be missing in NBT
     * }</pre>
     *
     * @return true if field is required, false if optional
     */
    boolean required() default true;
}
