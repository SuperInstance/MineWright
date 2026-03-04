# NBT Serialization Migration Guide

## Overview

The `NBTSerializer` utility eliminates boilerplate NBT serialization code by using annotations and reflection. This guide shows how to migrate existing manual serialization code to use the automatic system.

## Before and After Comparison

### Manual Serialization (Before)

```java
public class PlayerMemory {
    private String playerName;
    private int playerId;
    private UUID playerUuid;
    private BlockPos lastPosition;
    private Instant lastLoginTime;
    private int health;
    private int maxHealth;
    private long experiencePoints;
    private boolean isOnline;

    public void saveToNBT(CompoundTag tag) {
        tag.putString("PlayerName", playerName);
        tag.putInt("PlayerId", playerId);
        tag.putUUID("PlayerUuid", playerUuid);
        tag.putLong("LastPosition", lastPosition.asLong());
        tag.putLong("LastLoginTime", lastLoginTime.toEpochMilli());
        tag.putInt("Health", health);
        tag.putInt("MaxHealth", maxHealth);
        tag.putLong("ExperiencePoints", experiencePoints);
        tag.putBoolean("IsOnline", isOnline);
    }

    public void loadFromNBT(CompoundTag tag) {
        playerName = tag.getString("PlayerName");
        playerId = tag.getInt("PlayerId");
        playerUuid = tag.getUUID("PlayerUuid");
        lastPosition = BlockPos.of(tag.getLong("LastPosition"));
        lastLoginTime = Instant.ofEpochMilli(tag.getLong("LastLoginTime"));
        health = tag.getInt("Health");
        maxHealth = tag.getInt("MaxHealth");
        experiencePoints = tag.getLong("ExperiencePoints");
        isOnline = tag.getBoolean("IsOnline");
    }
}
```

**Lines of code:** ~30 (excluding field declarations)

### Automatic Serialization (After)

```java
@NBTSerializable
public class PlayerMemory {
    @NBTField("PlayerName")
    private String playerName;

    @NBTField("PlayerId")
    private int playerId;

    @NBTField("PlayerUuid")
    private UUID playerUuid;

    @NBTField("LastPosition")
    private BlockPos lastPosition;

    @NBTField("LastLoginTime")
    private Instant lastLoginTime;

    @NBTField("Health")
    private int health;

    @NBTField("MaxHealth")
    private int maxHealth;

    @NBTField("ExperiencePoints")
    private long experiencePoints;

    @NBTField("IsOnline")
    private boolean isOnline;

    public void saveToNBT(CompoundTag tag) {
        NBTSerializer.saveFields(tag, this);
    }

    public void loadFromNBT(CompoundTag tag) {
        NBTSerializer.loadFields(tag, this);
    }
}
```

**Lines of code:** ~10 (excluding field declarations)

**Reduction:** ~67% less code, no maintenance burden for adding new fields

## Step-by-Step Migration Process

### Step 1: Add Imports

Add the necessary imports to your class:

```java
import com.minewright.util.NBTSerializable;
import com.minewright.util.NBTField;
import com.minewright.util.NBTSerializer;
```

### Step 2: Annotate the Class

Add the `@NBTSerializable` annotation to your class:

```java
@NBTSerializable
public class MyClass {
    // fields and methods
}
```

### Step 3: Annotate Serializable Fields

Add `@NBTField` annotations to each field that should be serialized:

```java
@NBTSerializable
public class MyClass {
    @NBTField
    private String name;

    @NBTField
    private int value;

    @NBTField
    private boolean flag;
}
```

### Step 4: Replace saveToNBT Method

Replace the manual save implementation with the utility call:

**Before:**
```java
public void saveToNBT(CompoundTag tag) {
    tag.putString("Name", name);
    tag.putInt("Value", value);
    tag.putBoolean("Flag", flag);
    // ... 20 more lines
}
```

**After:**
```java
public void saveToNBT(CompoundTag tag) {
    NBTSerializer.saveFields(tag, this);
}
```

### Step 5: Replace loadFromNBT Method

Replace the manual load implementation with the utility call:

**Before:**
```java
public void loadFromNBT(CompoundTag tag) {
    name = tag.getString("Name");
    value = tag.getInt("Value");
    flag = tag.getBoolean("Flag");
    // ... 20 more lines
}
```

**After:**
```java
public void loadFromNBT(CompoundTag tag) {
    NBTSerializer.loadFields(tag, this);
}
```

## Custom NBT Key Names

If you need to preserve existing NBT key names for backwards compatibility, use the `value` attribute:

```java
@NBTField("CustomKeyName")  // Uses "CustomKeyName" in NBT instead of "fieldName"
private String fieldName;
```

### Example: Preserving Legacy NBT Keys

```java
@NBTSerializable
public class LegacyData {
    @NBTField("PlayerName")  // Legacy key
    private String playerName;

    @NBTField("PlayerId")    // Legacy key
    private int playerId;
}
```

## Optional Fields for Backwards Compatibility

When adding new fields to existing classes, mark them as optional to maintain backwards compatibility with older NBT data:

```java
@NBTSerializable
public class PlayerData {
    // Existing required fields
    @NBTField
    private String playerName;

    @NBTField
    private int playerId;

    // New optional field - won't fail if missing in old NBT data
    @NBTField(required = false)
    private String nickname;
}
```

### Loading Old NBT Data

When loading NBT data that was saved before the new field was added:

```java
// Old NBT (saved before nickname field existed)
CompoundTag oldTag = new CompoundTag();
oldTag.putString("playerName", "Steve");
oldTag.putInt("playerId", 123);
// No "nickname" field

PlayerData data = new PlayerData();
NBTSerializer.loadFields(oldTag, data);

// Result:
// data.playerName = "Steve" (loaded)
// data.playerId = 123 (loaded)
// data.nickname = null (missing field, but optional so no error)
```

## Complex Type Examples

### Collections

```java
@NBTSerializable
public class MemoryData {
    @NBTField
    private List<String> recentActions = new ArrayList<>();

    @NBTField
    private Set<String> tags = new HashSet<>();

    @NBTField
    private Map<String, Integer> scores = new HashMap<>();
}
```

### Atomic Types

```java
@NBTSerializable
public class CounterData {
    @NBTField
    private AtomicInteger counter = new AtomicInteger(0);

    @NBTField
    private AtomicLong timestamp = new AtomicLong(0L);

    @NBTField
    private AtomicBoolean flag = new AtomicBoolean(false);
}
```

### Enums

```java
@NBTSerializable
public class EnumData {
    enum Status {
        ACTIVE, INACTIVE, PENDING
    }

    @NBTField
    private Status status = Status.ACTIVE;
}
```

### Nested Objects

```java
@NBTSerializable
public class InnerData {
    @NBTField
    private String name = "inner";

    @NBTField
    private int value = 100;
}

@NBTSerializable
public class OuterData {
    @NBTField
    private String name = "outer";

    @NBTField
    private InnerData inner = new InnerData();
}
```

## Real-World Migration Example

### Original: CompanionMemory.java (Partial)

**Before (manual serialization - ~200 lines):**

```java
public class CompanionMemory {
    private final AtomicInteger rapportLevel;
    private final AtomicInteger trustLevel;
    private final AtomicInteger interactionCount;
    private Instant firstMeeting;
    private String playerName;
    private final Map<String, Object> playerPreferences;
    private final Map<String, Integer> playstyleMetrics;
    // ... many more fields

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("RapportLevel", rapportLevel.get());
        tag.putInt("TrustLevel", trustLevel.get());
        tag.putInt("InteractionCount", interactionCount.get());

        if (firstMeeting != null) {
            tag.putLong("FirstMeeting", firstMeeting.toEpochMilli());
        }

        if (playerName != null) {
            tag.putString("PlayerName", playerName);
        }

        // Save player preferences
        CompoundTag preferencesTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : playerPreferences.entrySet()) {
            saveValueToNBT(preferencesTag, entry.getKey(), entry.getValue());
        }
        tag.put("PlayerPreferences", preferencesTag);

        // Save playstyle metrics
        CompoundTag playstyleTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : playstyleMetrics.entrySet()) {
            playstyleTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("PlaystyleMetrics", playstyleTag);

        // ... 50+ more lines
    }

    public void loadFromNBT(CompoundTag tag) {
        rapportLevel.set(tag.getInt("RapportLevel"));
        trustLevel.set(tag.getInt("TrustLevel"));
        interactionCount.set(tag.getInt("InteractionCount"));

        long firstMeetingEpoch = tag.getLong("FirstMeeting");
        if (firstMeetingEpoch != 0) {
            firstMeeting = Instant.ofEpochMilli(firstMeetingEpoch);
        }

        playerName = tag.contains("PlayerName") ? tag.getString("PlayerName") : null;

        // Load player preferences
        CompoundTag preferencesTag = tag.getCompound("PlayerPreferences");
        if (!preferencesTag.isEmpty()) {
            playerPreferences.clear();
            for (String key : preferencesTag.getAllKeys()) {
                Object value = preferencesTag.contains(key, 99)
                    ? preferencesTag.getInt(key)
                    : preferencesTag.getString(key);
                playerPreferences.put(key, value);
            }
        }

        // Load playstyle metrics
        CompoundTag playstyleTag = tag.getCompound("PlaystyleMetrics");
        if (!playstyleTag.isEmpty()) {
            playstyleMetrics.clear();
            for (String key : playstyleTag.getAllKeys()) {
                playstyleMetrics.put(key, playstyleTag.getInt(key));
            }
        }

        // ... 50+ more lines
    }

    // Helper methods
    private void saveValueToNBT(CompoundTag tag, String key, Object value) {
        if (value instanceof String) {
            tag.putString(key, (String) value);
            tag.putString(key + "Type", "string");
        } else if (value instanceof Integer) {
            tag.putInt(key, (Integer) value);
            tag.putString(key + "Type", "int");
        } else if (value instanceof Boolean) {
            tag.putBoolean(key, (Boolean) value);
            tag.putString(key + "Type", "boolean");
        }
        // ... more type handling
    }
}
```

**After (automatic serialization - ~20 lines):**

```java
@NBTSerializable
public class CompanionMemory {
    @NBTField("RapportLevel")
    private final AtomicInteger rapportLevel = new AtomicInteger(0);

    @NBTField("TrustLevel")
    private final AtomicInteger trustLevel = new AtomicInteger(0);

    @NBTField("InteractionCount")
    private final AtomicInteger interactionCount = new AtomicInteger(0);

    @NBTField("FirstMeeting")
    private Instant firstMeeting;

    @NBTField("PlayerName")
    private String playerName;

    @NBTField("PlayerPreferences")
    private final Map<String, Object> playerPreferences = new ConcurrentHashMap<>();

    @NBTField("PlaystyleMetrics")
    private final Map<String, Integer> playstyleMetrics = new ConcurrentHashMap<>();

    // ... all other fields with @NBTField annotations

    public void saveToNBT(CompoundTag tag) {
        NBTSerializer.saveFields(tag, this);
    }

    public void loadFromNBT(CompoundTag tag) {
        NBTSerializer.loadFields(tag, this);
    }

    // No helper methods needed - all handled by NBTSerializer
}
```

**Benefits:**
- **90% reduction** in serialization code (200 lines -> 20 lines)
- **No maintenance** for adding new fields - just add @NBTField
- **Type-safe** - compile-time checking of annotations
- **Consistent** - same pattern across all classes

## Migration Checklist

For each class that needs migration:

- [ ] Add `@NBTSerializable` annotation to class
- [ ] Add `@NBTField` annotation to each serializable field
- [ ] Preserve NBT key names using `@NBTField("KeyName")` for backwards compatibility
- [ ] Mark new fields as `@NBTField(required = false)` for backwards compatibility
- [ ] Replace `saveToNBT()` body with `NBTSerializer.saveFields(tag, this)`
- [ ] Replace `loadFromNBT()` body with `NBTSerializer.loadFields(tag, this)`
- [ ] Remove helper methods (e.g., `saveValueToNBT`, `loadValueFromNBT`)
- [ ] Test saving and loading with existing NBT data
- [ ] Test backwards compatibility with old NBT files

## Testing Migrated Classes

After migration, verify that:

1. **NBT Structure Remains Compatible:**
   ```java
   CompoundTag tag = new CompoundTag();
   originalInstance.saveToNBT(tag);

   // Verify key names match expected NBT structure
   assertTrue(tag.contains("ExpectedKeyName"));
   assertEquals(expectedValue, tag.getType("ExpectedKeyName"));
   ```

2. **Load Old NBT Data:**
   ```java
   CompoundTag oldTag = loadLegacyNBT(); // Load NBT from old version
   newInstance.loadFromNBT(oldTag);

   // Verify data loaded correctly
   assertEquals(expectedValue, newInstance.getField());
   ```

3. **Save and Load Cycle:**
   ```java
   CompoundTag tag = new CompoundTag();
   original.saveToNBT(tag);

   MyClass loaded = new MyClass();
   loaded.loadFromNBT(tag);

   assertEquals(original, loaded);
   ```

## Files to Migrate

Based on the code audit, the following files would benefit from NBTSerializer migration:

### High Priority (Most Duplication)
- `CompanionMemory.java` - ~200 lines of serialization code
- `MilestoneTracker.java` - ~80 lines of serialization code
- `ForemanMemory.java` - ~20 lines of serialization code
- `InMemoryVectorStore.java` - ~60 lines of serialization code
- `PersonalityProfile` (inner class) - ~50 lines of serialization code

### Medium Priority
- `ConversationMemory` (inner class) - ~40 lines of serialization code
- `EpisodicMemory` (inner class) - ~30 lines of serialization code
- `SemanticMemory` (inner class) - ~20 lines of serialization code
- `EmotionalMemory` (inner class) - ~20 lines of serialization code

### Estimated Impact
- **Total lines removed:** ~500 lines of boilerplate code
- **Maintenance burden eliminated:** No need to update serialization when adding fields
- **Bug reduction:** Type-safe annotations reduce copy-paste errors
- **Consistency:** Uniform serialization pattern across codebase

## Best Practices

1. **Always use @NBTSerializable on classes with @NBTField**
   - Improves documentation
   - Enables nested object serialization

2. **Preserve NBT key names for backwards compatibility**
   - Use `@NBTField("LegacyKeyName")` instead of `@NBTField`

3. **Mark new fields as optional when extending existing classes**
   - Use `@NBTField(required = false)` for new fields
   - Prevents loading errors with old NBT data

4. **Use atomic types for thread-safe counters**
   - `AtomicInteger` instead of `int`
   - `AtomicLong` instead of `long`

5. **Keep NBT key names in PascalCase for consistency**
   - `"PlayerName"` instead of `"playerName"`
   - Matches existing Minecraft NBT conventions

## Troubleshooting

### Issue: Class not serializing fields

**Cause:** Missing `@NBTSerializable` annotation on class or `@NBTField` on fields

**Solution:**
```java
@NBTSerializable  // Add this
public class MyClass {
    @NBTField      // And this
    private String field;
}
```

### Issue: Field not loading from NBT

**Cause:** NBT key name mismatch

**Solution:** Use custom key name to match existing NBT:
```java
@NBTField("ExistingKeyName")  // Match the key in NBT file
private String fieldName;
```

### Issue: Loading throws exception for missing field

**Cause:** Field is marked as required but missing in NBT

**Solution:** Mark field as optional:
```java
@NBTField(required = false)  // Don't fail if missing
private String newField;
```

### Issue: Nested object not serializing

**Cause:** Nested class doesn't have `@NBTSerializable` annotation

**Solution:**
```java
@NBTSerializable  // Add to nested class
public class InnerClass {
    @NBTField
    private String field;
}

@NBTSerializable
public class OuterClass {
    @NBTField
    private InnerClass inner;  // Will now serialize correctly
}
```

## Performance Considerations

The NBTSerializer uses reflection for field discovery but caches metadata for performance:

1. **First call per class:** Slower (discovers and caches fields)
2. **Subsequent calls:** Fast (uses cached metadata)
3. **Thread-safe:** Can be called concurrently from multiple threads
4. **Memory overhead:** Minimal (one cache entry per class)

Performance is comparable to manual serialization for most use cases, with significant developer productivity gains.

## Conclusion

Migrating to NBTSerializer provides:
- **90% reduction** in serialization boilerplate code
- **Eliminated maintenance** for adding/removing fields
- **Type safety** through compile-time annotation checking
- **Backwards compatibility** through custom key names and optional fields
- **Consistency** across the entire codebase

Start with high-priority classes (CompanionMemory, MilestoneTracker) and migrate incrementally while testing for NBT compatibility.
