# Memory Persistence with NBT Format for MineWright Mod

**Author:** System Research
**Date:** 2025-01-10
**Minecraft Version:** 1.20.1 (Forge)
**Target Classes:** `CompanionMemory.java`, `InMemoryVectorStore.java`

---

## Table of Contents

1. [Overview](#overview)
2. [NBT Fundamentals for Minecraft Mods](#nbt-fundamentals)
3. [Memory System Architecture](#memory-system-architecture)
4. [NBT Structure Design](#nbt-structure-design)
5. [Implementation Details](#implementation-details)
6. [Vector Store Persistence](#vector-store-persistence)
7. [World-Specific vs Global Memory](#world-specific-vs-global-memory)
8. [Version Migration Strategy](#version-migration)
9. [Compression for Large Datasets](#compression)
10. [Performance Considerations](#performance-considerations)
11. [Testing Strategy](#testing-strategy)
12. [Migration from In-Memory to Persistent](#migration-strategy)

---

## Overview

The MineWright mod implements an advanced companion AI system with persistent memory capabilities. This document details the NBT (Named Binary Tag) serialization strategy for persisting:

- **CompanionMemory**: Relationship data, episodic memories, semantic facts, emotional moments
- **InMemoryVectorStore**: Embedding vectors for semantic search
- **MilestoneTracker**: Relationship milestones and achievements
- **ForemanMemory**: Task queue and recent actions

### Current Status

The codebase already implements NBT serialization for `CompanionMemory` and `InMemoryVectorStore`, but lacks persistence for the vector store's semantic search capabilities and requires optimization for large datasets.

---

## NBT Fundamentals for Minecraft Mods

### What is NBT?

NBT (Named Binary Tag) is Minecraft's binary serialization format used for:
- World save data (`level.dat`, `region` files)
- Entity data (`entity.dat`)
- Item data (`NBTTagCompound` in ItemStack)
- Player data (`playerdata/*.dat`)

### NBT Tag Types

| Tag Type | ID | Java Type | Description |
|----------|----|----|-------------|
| `End` | 0 | - | Tag list terminator |
| `Byte` | 1 | `byte` | Signed 8-bit integer |
| `Short` | 2 | `short` | Signed 16-bit integer |
| `Int` | 3 | `int` | Signed 32-bit integer |
| `Long` | 4 | `long` | Signed 64-bit integer |
| `Float` | 5 | `float` | 32-bit IEEE float |
| `Double` | 6 | `double` | 64-bit IEEE double |
| `ByteArray` | 7 | `byte[]` | Byte array |
| `String` | 8 | `String` | UTF-8 string |
| `List` | 9 | `ListTag` | Typed list |
| `Compound` | 10 | `CompoundTag` | Map/dictionary of tags |
| `IntArray` | 11 | `int[]` | Integer array |
| `LongArray` | 12 | `long[]` | Long array |

### NBT API in Minecraft Forge 1.20.1

```java
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntTag;

// Creating a compound tag
CompoundTag tag = new CompoundTag();
tag.putString("Name", "Foreman");
tag.putInt("Age", 42);

// Creating a list tag
ListTag list = new ListTag();
list.add(StringTag.valueOf("Item1"));
list.add(StringTag.valueOf("Item2"));
tag.put("Items", list);

// Reading from tag
String name = tag.getString("Name");
int age = tag.getInt("Age");
```

---

## Memory System Architecture

### Component Hierarchy

```
ForemanEntity
├── ForemanMemory (simple memory - already persisted)
│   ├── currentGoal: String
│   ├── taskQueue: Queue<String>
│   └── recentActions: LinkedList<String>
│
└── CompanionMemory (complex memory - partially persisted)
    ├── Relationship Data
    │   ├── rapportLevel: AtomicInteger
    │   ├── trustLevel: AtomicInteger
    │   ├── interactionCount: AtomicInteger
    │   ├── playerName: String
    │   └── firstMeeting: Instant
    │
    ├── Memory Stores
    │   ├── episodicMemories: Deque<EpisodicMemory>
    │   ├── semanticMemories: Map<String, SemanticMemory>
    │   ├── emotionalMemories: List<EmotionalMemory>
    │   └── conversationalMemory: ConversationalMemory
    │
    ├── Personality
    │   └── personality: PersonalityProfile
    │
    ├── MilestoneTracker
    │   └── milestoneTracker: MilestoneTracker
    │
    └── Vector Search Infrastructure
        ├── embeddingModel: EmbeddingModel
        ├── memoryVectorStore: InMemoryVectorStore<EpisodicMemory>
        └── memoryToVectorId: Map<EpisodicMemory, Integer>
```

---

## NBT Structure Design

### CompanionMemory NBT Structure

```nbt
CompanionMemory: {
    // Version identifier for migration
    "Version": 1,
    "DataVersion": 3120,  // Minecraft data version

    // Relationship metadata
    "RapportLevel": 75,
    "TrustLevel": 60,
    "InteractionCount": 1234,
    "FirstMeeting": 1704067200000,  // Unix timestamp (ms)
    "PlayerName": "Steve",

    // Episodic memories (event-based)
    "EpisodicMemories": [
        {
            "EventType": "build",
            "Description": "Built a house together",
            "EmotionalWeight": 7,
            "Timestamp": 1704067200000,
            "VectorId": 42  // Reference to vector store
        },
        // ... up to 200 memories
    ],

    // Semantic memories (facts about player)
    "SemanticMemories": [
        {
            "Key": "preference:building_style",
            "Category": "preference",
            "FactKey": "building_style",
            "ValueType": "string",
            "Value": "modern",
            "LearnedAt": 1704067200000,
            "Confidence": 5
        },
        // ... more facts
    ],

    // Emotional memories (high-impact moments)
    "EmotionalMemories": [
        {
            "EventType": "first_diamond",
            "Description": "Found first diamond together",
            "EmotionalWeight": 10,
            "Timestamp": 1704067200000
        },
        // ... up to 50 memories
    ],

    // Conversational data
    "InsideJokes": [
        {
            "Context": "When the creeper exploded the chicken coop",
            "Punchline": "Poultry in motion!",
            "CreatedAt": 1704067200000,
            "ReferenceCount": 15
        }
    ],

    "DiscussedTopics": ["mining", "building", "exploring"],
    "PhraseUsage": {
        "Let's get to work!": 45,
        "Right then,": 32
    },

    // Player data
    "PlayerPreferences": {
        "favorite_block": "cobblestone",
        "work_style": "methodical"
    },
    "PlaystyleMetrics": {
        "blocks_mined": 1234,
        "blocks_placed": 5678
    },

    // Personality profile
    "Personality": {
        "Openness": 70,
        "Conscientiousness": 80,
        "Extraversion": 60,
        "Agreeableness": 75,
        "Neuroticism": 30,
        "Humor": 65,
        "Encouragement": 80,
        "Formality": 40,
        "FavoriteBlock": "cobblestone",
        "WorkStyle": "methodical",
        "Mood": "cheerful",
        "Catchphrases": ["Right then,", "Let's get to work!"]
    },

    // Milestone tracker
    "MilestoneTracker": {
        "Milestones": [...],
        "FirstOccurrences": {...},
        "Counters": {...},
        "LastAnniversaryCheck": 1704067200000
    }
}
```

### InMemoryVectorStore NBT Structure

```nbt
InMemoryVectorStore: {
    "Version": 1,
    "Dimension": 384,  // Embedding dimension
    "NextId": 200,     // Next available ID

    // Vector entries with compressed embeddings
    "Vectors": [
        {
            "Id": 0,
            "MemoryId": 1704067200000,  // Reference to episodic memory
            "Vector": [1000, -234, 567, ...],  // Float x 1000 as int
            "Checksum": 1234567890  // For data integrity
        },
        // ... up to MAX_EPISODIC_MEMORIES
    ]
}
```

---

## Implementation Details

### Entity Integration Pattern

The `ForemanEntity` already demonstrates the correct pattern for NBT persistence:

```java
public class ForemanEntity extends PathfinderMob {
    private CompanionMemory companionMemory;

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // Save companion memory
        CompoundTag companionMemoryTag = new CompoundTag();
        this.companionMemory.saveToNBT(companionMemoryTag);
        tag.put("CompanionMemory", companionMemoryTag);

        // Note: Vector store should be saved within CompanionMemory
        // not separately, to maintain referential integrity
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // Load companion memory
        if (tag.contains("CompanionMemory")) {
            this.companionMemory.loadFromNBT(tag.getCompound("CompanionMemory"));
        }
    }
}
```

### Save Method Implementation

```java
public void saveToNBT(CompoundTag tag) {
    // Version tracking for migration
    tag.putInt("Version", CURRENT_VERSION);
    tag.putInt("DataVersion", MinecraftDataVersion.VERSION);

    // ... existing save code ...

    // CRITICAL: Save vector store data
    CompoundTag vectorStoreTag = new CompoundTag();
    saveVectorStoreToNBT(vectorStoreTag);
    tag.put("VectorStore", vectorStoreTag);

    LOGGER.debug("CompanionMemory saved: {} episodic, {} semantic, {} vectors",
        episodicMemories.size(), semanticMemories.size(),
        memoryVectorStore.size());
}

private void saveVectorStoreToNBT(CompoundTag tag) {
    // Save dimension and next ID
    tag.putInt("Dimension", memoryVectorStore.getDimension());
    tag.putInt("NextId", memoryToVectorId.size());

    // Save vectors with memory references
    ListTag vectorsList = new ListTag();
    for (Map.Entry<EpisodicMemory, Integer> entry : memoryToVectorId.entrySet()) {
        EpisodicMemory memory = entry.getKey();
        int vectorId = entry.getValue();

        CompoundTag vectorTag = new CompoundTag();
        vectorTag.putInt("VectorId", vectorId);

        // Use memory timestamp as unique identifier
        vectorTag.putLong("MemoryTimestamp", memory.timestamp.toEpochMilli());

        // Get and save the vector
        float[] vector = memoryVectorStore.getVectorById(vectorId);
        int[] compressedVector = compressVector(vector);
        vectorTag.putIntArray("Vector", compressedVector);

        // Add checksum for integrity
        vectorTag.putInt("Checksum", computeChecksum(vector));

        vectorsList.add(vectorTag);
    }
    tag.put("Vectors", vectorsList);
}
```

### Load Method Implementation

```java
public void loadFromNBT(CompoundTag tag) {
    // Version check and migration
    int version = tag.contains("Version") ? tag.getInt("Version") : 0;
    if (version < CURRENT_VERSION) {
        migrateFromVersion(tag, version);
        return;
    }

    // ... existing load code ...

    // CRITICAL: Load vector store data
    if (tag.contains("VectorStore")) {
        loadVectorStoreFromNBT(tag.getCompound("VectorStore"));
    }

    LOGGER.info("CompanionMemory loaded: {} episodic, {} semantic, {} vectors",
        episodicMemories.size(), semanticMemories.size(),
        memoryVectorStore.size());
}

private void loadVectorStoreFromNBT(CompoundTag tag) {
    int dimension = tag.getInt("Dimension");
    if (dimension != memoryVectorStore.getDimension()) {
        LOGGER.warn("Vector dimension mismatch: expected {}, got {}",
            memoryVectorStore.getDimension(), dimension);
        // Could migrate dimensions here if needed
    }

    ListTag vectorsList = tag.getList("Vectors", 10); // 10 = CompoundTag

    for (int i = 0; i < vectorsList.size(); i++) {
        CompoundTag vectorTag = vectorsList.getCompound(i);
        int vectorId = vectorTag.getInt("VectorId");
        long memoryTimestamp = vectorTag.getLong("MemoryTimestamp");
        int[] compressedVector = vectorTag.getIntArray("Vector");
        int storedChecksum = vectorTag.getInt("Checksum");

        // Decompress vector
        float[] vector = decompressVector(compressedVector);

        // Verify integrity
        int computedChecksum = computeChecksum(vector);
        if (computedChecksum != storedChecksum) {
            LOGGER.error("Checksum mismatch for vector {}, skipping", vectorId);
            continue;
        }

        // Find corresponding episodic memory
        EpisodicMemory memory = findMemoryByTimestamp(memoryTimestamp);
        if (memory != null) {
            // Reconstruct vector store entry
            memoryVectorStore.addDirect(vectorId, vector, memory);
            memoryToVectorId.put(memory, vectorId);
        } else {
            LOGGER.warn("No memory found for timestamp {}, orphan vector {}",
                memoryTimestamp, vectorId);
        }
    }
}
```

---

## Vector Store Persistence

### Challenge: Float Arrays in NBT

NBT doesn't natively support float arrays. The current implementation converts floats to integers by multiplying by 1000:

```java
// Current approach in InMemoryVectorStore
int[] vectorInt = new int[vector.length];
for (int i = 0; i < vector.length; i++) {
    vectorInt[i] = Math.round(vector[i] * 1000.0f);
}
entryTag.putIntArray("Vector", vectorInt);
```

**Limitations:**
- Precision loss (3 decimal places)
- Large memory footprint (384 floats = 1536 bytes uncompressed)
- No compression for redundant data

### Improved Vector Compression

```java
/**
 * Compresses a vector using delta encoding and variable-length encoding.
 *
 * @param vector The input vector (normalized, typically -1.0 to 1.0)
 * @return Compressed byte array
 */
private byte[] compressVectorOptimal(float[] vector) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    try {
        // Use delta encoding: store first value, then differences
        float prevValue = 0.0f;

        for (float value : vector) {
            // Scale to 16-bit signed integer (-32767 to 32767)
            int scaled = (int)(value * 32767.0f);

            // Delta from previous value
            int delta = scaled - (int)(prevValue * 32767.0f);

            // Variable-length encoding for smaller deltas
            if (delta >= -64 && delta <= 63) {
                // Single byte encoding
                dos.writeByte(delta & 0x7F);
            } else if (delta >= -8192 && delta <= 8191) {
                // Two byte encoding
                dos.writeByte(0x80 | ((delta >> 8) & 0x3F));
                dos.writeByte(delta & 0xFF);
            } else {
                // Four byte encoding (rare)
                dos.writeByte(0xC0);
                dos.writeInt(delta);
            }

            prevValue = value;
        }

    } catch (IOException e) {
        LOGGER.error("Vector compression failed", e);
        return new byte[0];
    }

    return baos.toByteArray();
}

/**
 * Decompresses a vector from optimal compressed format.
 */
private float[] decompressVectorOptimal(byte[] compressed) {
    List<Float> values = new ArrayList<>();
    ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
    DataInputStream dis = new DataInputStream(bais);

    try {
        float currentValue = 0.0f;

        while (dis.available() > 0) {
            int firstByte = dis.readByte() & 0xFF;
            int delta;

            if ((firstByte & 0x80) == 0) {
                // Single byte: -64 to 63
                delta = (byte)firstByte;
            } else if ((firstByte & 0xC0) == 0x80) {
                // Two bytes: -8192 to 8191
                int secondByte = dis.readByte() & 0xFF;
                delta = ((firstByte & 0x3F) << 8) | secondByte;
                if (delta >= 8192) delta -= 16384; // Sign extend
            } else {
                // Four bytes
                delta = dis.readInt();
            }

            currentValue += delta / 32767.0f;
            values.add(currentValue);
        }

    } catch (IOException e) {
        LOGGER.error("Vector decompression failed", e);
        return new float[0];
    }

    float[] result = new float[values.size()];
    for (int i = 0; i < result.length; i++) {
        result[i] = values.get(i);
    }
    return result;
}
```

### Quantization for Further Compression

For extreme size requirements, use 8-bit quantization:

```java
/**
 * Quantizes a vector to 8-bit representation.
 * Loses precision but achieves 4x compression.
 */
private byte[] quantizeVector8Bit(float[] vector) {
    byte[] quantized = new byte[vector.length];

    for (int i = 0; i < vector.length; i++) {
        // Clamp to -1.0 to 1.0 range
        float clamped = Math.max(-1.0f, Math.min(1.0f, vector[i]));
        // Scale to -128 to 127
        quantized[i] = (byte)(clamped * 127.0f);
    }

    return quantized;
}

/**
 * Dequantizes an 8-bit vector back to float.
 */
private float[] dequantizeVector8Bit(byte[] quantized) {
    float[] vector = new float[quantized.length];

    for (int i = 0; i < quantized.length; i++) {
        vector[i] = quantized[i] / 127.0f;
    }

    return vector;
}
```

---

## World-Specific vs Global Memory

### World-Specific Memory (Default)

Memory is tied to specific worlds/saves:

```
saves/
  MyWorld/
    entities/
      foreman_12345.dat  (Contains CompanionMemory)
```

**Advantages:**
- Clean separation between playthroughs
- Automatic cleanup when world is deleted
- No cross-world contamination

**Disadvantages:**
- Memories don't transfer between worlds
- Player must rebuild relationship each world

### Global Memory (Optional Extension)

Store shared memories across all worlds:

```java
/**
 * Global memory manager that persists across all worlds.
 * Stored in a separate file in the Minecraft directory.
 */
public class GlobalMemoryManager {
    private static final String GLOBAL_MEMORY_FILE = "minewright_global_memory.dat";
    private static GlobalMemoryManager INSTANCE;

    private GlobalCompanionMemory globalMemory;
    private final Path savePath;

    private GlobalMemoryManager() {
        this.savePath = Paths.get(".", GLOBAL_MEMORY_FILE);
        load();
    }

    public static GlobalMemoryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GlobalMemoryManager();
        }
        return INSTANCE;
    }

    /**
     * Gets memories that should transfer between worlds.
     * Examples: Player personality, long-term preferences, inside jokes
     */
    public GlobalMemoryTransferable getTransferableMemory() {
        return globalMemory.createTransferSnapshot();
    }

    /**
     * Merges world-specific memories into global memory.
     */
    public void mergeFromWorld(CompanionMemory worldMemory) {
        // Merge high-rapport memories
        // Merge inside jokes
        // Merge personality insights
        save();
    }

    private void load() {
        if (Files.exists(savePath)) {
            try {
                byte[] data = Files.readAllBytes(savePath);
                CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(data));
                globalMemory = new GlobalCompanionMemory(tag);
            } catch (IOException e) {
                LOGGER.error("Failed to load global memory", e);
                globalMemory = new GlobalCompanionMemory();
            }
        } else {
            globalMemory = new GlobalCompanionMemory();
        }
    }

    private void save() {
        try {
            CompoundTag tag = globalMemory.saveToNBT();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, baos);
            Files.write(savePath, baos.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Failed to save global memory", e);
        }
    }
}
```

---

## Version Migration Strategy

### Version Tracking

```java
public class CompanionMemory {
    private static final int CURRENT_VERSION = 2;
    private static final int MIN_COMPATIBLE_VERSION = 1;

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Version", CURRENT_VERSION);
        // ... rest of save code
    }

    public void loadFromNBT(CompoundTag tag) {
        int version = tag.contains("Version") ? tag.getInt("Version") : 0;

        if (version < MIN_COMPATIBLE_VERSION) {
            LOGGER.error("Memory version {} is too old, cannot load", version);
            return;
        }

        if (version > CURRENT_VERSION) {
            LOGGER.warn("Memory version {} is newer than current {}, attempting load",
                version, CURRENT_VERSION);
        }

        if (version < CURRENT_VERSION) {
            migrateFromVersion(tag, version);
        } else {
            loadCurrentVersion(tag);
        }
    }

    private void migrateFromVersion(CompoundTag tag, int fromVersion) {
        LOGGER.info("Migrating memory from version {} to {}", fromVersion, CURRENT_VERSION);

        switch (fromVersion) {
            case 0:
                migrateV0ToV1(tag);
                // Fall through
            case 1:
                migrateV1ToV2(tag);
                break;
            default:
                LOGGER.warn("Unknown migration from version {}", fromVersion);
        }
    }

    private void migrateV0ToV1(CompoundTag tag) {
        // Version 0 didn't have vector store
        LOGGER.info("Adding vector store to version 0 memory");

        // Create vector store and populate with embeddings
        // for existing episodic memories
        if (tag.contains("EpisodicMemories")) {
            ListTag episodicList = tag.getList("EpisodicMemories", 10);

            for (int i = 0; i < episodicList.size(); i++) {
                CompoundTag memoryTag = episodicList.getCompound(i);
                String text = memoryTag.getString("EventType") + ": " +
                             memoryTag.getString("Description");

                // Generate embedding for old memory
                float[] embedding = embeddingModel.embed(text);
                // Store in new vector store structure
            }
        }
    }

    private void migrateV1ToV2(CompoundTag tag) {
        // Version 1 to 2: Added milestone tracker
        LOGGER.info("Adding milestone tracker to version 1 memory");

        if (!tag.contains("MilestoneTracker")) {
            CompoundTag milestoneTag = new CompoundTag();
            // Initialize empty milestone tracker
            tag.put("MilestoneTracker", milestoneTag);
        }
    }
}
```

### Schema Change Patterns

#### Pattern 1: Adding New Fields

```java
// In load method, always provide defaults
if (tag.contains("NewField")) {
    this.newField = tag.getString("NewField");
} else {
    this.newField = "default_value";  // Migration default
}
```

#### Pattern 2: Renaming Fields

```java
// Support both old and new names
if (tag.contains("NewName")) {
    this.field = tag.getString("NewName");
} else if (tag.contains("OldName")) {
    this.field = tag.getString("OldName");  // Backward compatibility
} else {
    this.field = "default";
}
```

#### Pattern 3: Type Changes

```java
// Changed from int to double
if (tag.contains("Version") && tag.getInt("Version") >= 2) {
    this.value = tag.getDouble("Value");
} else {
    // Old version stored as int (scaled by 100)
    this.value = tag.getInt("Value") / 100.0;
}
```

---

## Compression for Large Datasets

### NBT Compression Levels

Minecraft already uses GZIP compression for NBT files, but we can apply additional compression:

```java
/**
 * Saves memory with additional compression for large datasets.
 */
public void saveToNBTCompressed(CompoundTag tag, OutputStream out) throws IOException {
    // Apply compression if data is large
    if (shouldCompress(tag)) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.write(tag, new DataOutputStream(baos));

        byte[] uncompressed = baos.toByteArray();

        if (uncompressed.length > 1024 * 100) {  // > 100KB
            // Use higher compression level
            try (GZIPOutputStream gzos = new GZIPOutputStream(out, 9)) {
                gzos.write(uncompressed);
            }
        } else {
            out.write(uncompressed);
        }
    } else {
        NbtIo.write(tag, new DataOutputStream(out));
    }
}

private boolean shouldCompress(CompoundTag tag) {
    // Compress if:
    // - More than 50 episodic memories
    // - More than 100 semantic memories
    // - Vector store size > 100KB
    return tag.getList("EpisodicMemories", 10).size() > 50 ||
           tag.getList("SemanticMemories", 10).size() > 100;
}
```

### Selective Memory Persistence

For very large memory datasets, implement selective persistence:

```java
/**
 * Saves only the most important memories.
 */
public void saveToNBTSelective(CompoundTag tag, int maxMemories) {
    // Sort memories by importance
    List<EpisodicMemory> sortedMemories = new ArrayList<>(episodicMemories);
    sortedMemories.sort((a, b) -> {
        // Sort by: emotional weight + recency
        double scoreA = a.emotionalWeight +
            (Instant.now().minusMillis(a.timestamp.toEpochMilli())).toHours() / 24.0;
        double scoreB = b.emotionalWeight +
            (Instant.now().minusMillis(b.timestamp.toEpochMilli())).toHours() / 24.0;
        return Double.compare(scoreB, scoreA);
    });

    // Save only top N
    ListTag episodicList = new ListTag();
    for (int i = 0; i < Math.min(maxMemories, sortedMemories.size()); i++) {
        EpisodicMemory memory = sortedMemories.get(i);
        CompoundTag memoryTag = new CompoundTag();
        // ... save memory
        episodicList.add(memoryTag);
    }
    tag.put("EpisodicMemories", episodicList);

    LOGGER.info("Saved {} of {} memories (selective persistence)",
        Math.min(maxMemories, sortedMemories.size()), sortedMemories.size());
}
```

---

## Performance Considerations

### Save Performance

**Current Issues:**
- Vector store save is O(n) where n = number of memories
- Each vector requires 384 integers (1536 bytes)
- 200 memories = ~300KB of vector data

**Optimization Strategies:**

1. **Async Saving**

```java
private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();

public void saveAsync(Consumer<CompoundTag> callback) {
    saveExecutor.submit(() -> {
        CompoundTag tag = new CompoundTag();
        saveToNBT(tag);
        callback.accept(tag);
    });
}
```

2. **Differential Saving**

```java
private Set<EpisodicMemory> dirtyMemories = ConcurrentHashMap.newKeySet();

public void markMemoryDirty(EpisodicMemory memory) {
    dirtyMemories.add(memory);
}

private void saveDirtyOnly(CompoundTag tag) {
    ListTag dirtyList = new ListTag();
    for (EpisodicMemory memory : dirtyMemories) {
        // Save only dirty memories
        dirtyList.add(serializeMemory(memory));
    }
    tag.put("DirtyMemories", dirtyList);
    dirtyMemories.clear();
}
```

3. **Batch Vector Save**

```java
private void saveVectorsBatched(CompoundTag tag, int batchSize) {
    List<EpisodicMemory> memories = new ArrayList<>(memoryToVectorId.keySet());

    for (int i = 0; i < memories.size(); i += batchSize) {
        int end = Math.min(i + batchSize, memories.size());
        List<EpisodicMemory> batch = memories.subList(i, end);

        // Save batch
        ListTag batchTag = new ListTag();
        for (EpisodicMemory memory : batch) {
            batchTag.add(serializeVector(memory));
        }
        tag.put("VectorBatch_" + (i / batchSize), batchTag);
    }
}
```

### Load Performance

**Current Issues:**
- Vector decompression happens synchronously
- Embedding regeneration for old saves

**Optimization Strategies:**

1. **Lazy Loading**

```java
private boolean vectorsLoaded = false;

public List<EpisodicMemory> findRelevantMemories(String query, int k) {
    if (!vectorsLoaded) {
        loadVectorsLazily();
    }
    // ... search logic
}

private void loadVectorsLazily() {
    if (tag.contains("VectorStore")) {
        // Load vectors in background
        CompletableFuture.runAsync(() -> {
            loadVectorStoreFromNBT(tag.getCompound("VectorStore"));
            vectorsLoaded = true;
        });
    }
}
```

2. **Progressive Loading**

```java
public void loadProgressive(CompoundTag tag, Consumer<Float> progressCallback) {
    int totalSteps = 5;  // Load in 5 stages

    progressCallback.accept(0.0f);
    loadRelationshipData(tag);
    progressCallback.accept(0.2f);

    loadEpisodicMemories(tag);
    progressCallback.accept(0.4f);

    loadSemanticMemories(tag);
    progressCallback.accept(0.6f);

    loadVectorStore(tag);
    progressCallback.accept(0.8f);

    loadPersonality(tag);
    progressCallback.accept(1.0f);
}
```

3. **Caching Deserialized Objects**

```java
private static final Cache<String, float[]> VECTOR_CACHE = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(1, TimeUnit.HOURS)
    .build();

private float[] loadVectorWithCache(CompoundTag vectorTag) {
    String cacheKey = vectorTag.getString("Id");

    return VECTOR_CACHE.get(cacheKey, key -> {
        // Decompress vector
        int[] compressed = vectorTag.getIntArray("Vector");
        return decompressVector(compressed);
    });
}
```

### Memory Usage

**Current:**
- 200 episodic memories x ~500 bytes = ~100KB
- 200 vectors x 384 floats x 4 bytes = ~300KB
- Semantic memories: ~50KB
- **Total: ~450KB per companion**

**Optimization:**
- Use 8-bit quantization: 300KB → 75KB
- Deduplicate similar embeddings
- Implement memory limits with intelligent eviction

---

## Testing Strategy

### Unit Tests for NBT Serialization

```java
@Test
public void testCompanionMemoryRoundTrip() {
    // Create memory with test data
    CompanionMemory original = new CompanionMemory();
    original.initializeRelationship("TestPlayer");
    original.recordExperience("test_event", "Test description", 5);
    original.learnPlayerFact("preference", "test", "value");

    // Save to NBT
    CompoundTag tag = new CompoundTag();
    original.saveToNBT(tag);

    // Load from NBT
    CompanionMemory loaded = new CompanionMemory();
    loaded.loadFromNBT(tag);

    // Verify
    assertEquals("TestPlayer", loaded.getPlayerName());
    assertEquals(1, loaded.getRecentMemories(10).size());
    assertEquals(5, loaded.getRapportLevel());
}

@Test
public void testVectorStoreSerialization() {
    InMemoryVectorStore<TestData> store = new InMemoryVectorStore<>(384);

    // Add test vectors
    float[] vector1 = new float[384];
    Arrays.fill(vector1, 0.5f);
    TestData data1 = new TestData("test1");
    int id1 = store.add(vector1, data1);

    // Save to NBT
    CompoundTag tag = new CompoundTag();
    store.saveToNBT(tag);

    // Load from NBT
    InMemoryVectorStore<TestData> loaded = new InMemoryVectorStore<>(384);
    loaded.loadFromNBT(tag, id -> {
        if (id == id1) return data1;
        return null;
    });

    // Verify
    assertEquals(1, loaded.size());
    assertEquals(384, loaded.getDimension());
}

@Test
public void testCompressionAccuracy() {
    float[] original = new float[384];
    Random random = new Random(42);
    for (int i = 0; i < original.length; i++) {
        original[i] = (random.nextFloat() * 2.0f) - 1.0f;
    }

    // Compress and decompress
    int[] compressed = compressVector(original);
    float[] decompressed = decompressVector(compressed);

    // Verify accuracy (within 0.001 tolerance)
    for (int i = 0; i < original.length; i++) {
        assertEquals(original[i], decompressed[i], 0.001f);
    }
}

@Test
public void testVersionMigration() {
    // Create version 1 NBT
    CompoundTag v1Tag = new CompoundTag();
    v1Tag.putInt("Version", 1);
    // ... add v1 data

    // Load and migrate
    CompanionMemory memory = new CompanionMemory();
    memory.loadFromNBT(v1Tag);

    // Verify migration happened
    assertEquals(2, memory.getVersion());
    assertNotNull(memory.getMilestoneTracker());
}
```

### Integration Tests

```java
@Test
public void testWorldSaveLoad() {
    // Setup test world
    MinecraftServer server = createTestServer();
    ServerLevel level = server.overworld();

    // Spawn foreman
    ForemanEntity foreman = spawnForeman(level);
    foreman.getCompanionMemory().initializeRelationship("TestPlayer");
    foreman.getCompanionMemory().recordExperience("test", "Test", 5);

    // Save world
    saveWorld(server);

    // Unload world
    unloadWorld(server);

    // Load world
    ServerLevel reloadedLevel = loadWorld(server);
    ForemanEntity reloadedForeman = findForeman(reloadedLevel);

    // Verify memory persisted
    assertEquals("TestPlayer", reloadedForeman.getCompanionMemory().getPlayerName());
    assertEquals(1, reloadedForeman.getCompanionMemory().getRecentMemories(10).size());
}
```

### Performance Tests

```java
@Test
public void testLargeMemorySavePerformance() {
    CompanionMemory memory = new CompanionMemory();

    // Add 200 memories (max)
    for (int i = 0; i < 200; i++) {
        memory.recordExperience("test", "Memory " + i, i % 10 - 5);
    }

    // Measure save time
    long start = System.nanoTime();
    CompoundTag tag = new CompoundTag();
    memory.saveToNBT(tag);
    long duration = System.nanoTime() - start;

    // Should complete in < 100ms
    assertTrue(duration < 100_000_000L);
}

@Test
public void testVectorStoreSearchPerformance() {
    InMemoryVectorStore<String> store = new InMemoryVectorStore<>(384);

    // Add 1000 vectors
    for (int i = 0; i < 1000; i++) {
        float[] vector = generateRandomVector(384);
        store.add(vector, "item_" + i);
    }

    // Measure search time
    long start = System.nanoTime();
    float[] query = generateRandomVector(384);
    List<VectorSearchResult<String>> results = store.search(query, 10);
    long duration = System.nanoTime() - start;

    // Should complete in < 50ms
    assertTrue(duration < 50_000_000L);
    assertEquals(10, results.size());
}
```

---

## Migration Strategy from In-Memory to Persistent

### Phase 1: Preparation

1. **Audit Current State**
   ```java
   // Run this to identify unpersisted data
   public class MemoryAudit {
       public static void main(String[] args) {
           ForemanEntity foreman = getTestForeman();
           CompanionMemory memory = foreman.getCompanionMemory();

           System.out.println("Episodic memories: " + memory.episodicMemories.size());
           System.out.println("Semantic memories: " + memory.semanticMemories.size());
           System.out.println("Vector store entries: " + memory.memoryVectorStore.size());
           System.out.println("Milestones: " + memory.milestoneTracker.getMilestones().size());
       }
   }
   ```

2. **Add Version Tracking**
   ```java
   public class CompanionMemory {
       private static final int VERSION_PRE_PERSISTENCE = 0;
       private static final int VERSION_INITIAL_PERSISTENCE = 1;
       private static final int VERSION_VECTOR_STORE = 2;
       private static final int CURRENT_VERSION = VERSION_VECTOR_STORE;
   }
   ```

### Phase 2: Implement Vector Store Persistence

1. **Extend InMemoryVectorStore**
   ```java
   public class InMemoryVectorStore<T> {

       // New method to get vector by ID (needed for persistence)
       public float[] getVectorById(int id) {
           VectorEntry<T> entry = vectors.get(id);
           return entry != null ? entry.vector : null;
       }

       // New method to add vector with specific ID (needed for loading)
       public void addDirect(int id, float[] vector, T data) {
           vectors.put(id, new VectorEntry<>(vector, data, id));
           if (id >= nextId.get()) {
               nextId.set(id + 1);
           }
       }
   }
   ```

2. **Add Persistence Methods**
   ```java
   public class CompanionMemory {

       private void saveVectorStoreToNBT(CompoundTag tag) {
           // Implementation from earlier
       }

       private void loadVectorStoreFromNBT(CompoundTag tag) {
           // Implementation from earlier
       }

       private EpisodicMemory findMemoryByTimestamp(long timestamp) {
           return episodicMemories.stream()
               .filter(m -> m.timestamp.toEpochMilli() == timestamp)
               .findFirst()
               .orElse(null);
       }
   }
   ```

### Phase 3: Testing & Validation

1. **Create Test Suite**
   - Unit tests for each component
   - Integration tests for save/load
   - Performance tests for large datasets

2. **Beta Testing**
   ```java
   // Enable persistence for beta testers
   if (config.betaMode) {
       LOGGER.info("Beta mode: Full memory persistence enabled");
   } else {
       LOGGER.info("Stable mode: Basic memory persistence");
   }
   ```

### Phase 4: Gradual Rollout

1. **Feature Flag**
   ```java
   public class MemoryConfig {
       public boolean vectorStorePersistence = false;  // Off by default
       public boolean advancedCompression = false;
       public int maxMemories = 200;
   }
   ```

2. **Monitoring**
   ```java
   public class MemoryMetrics {
       private static final Histogram saveTime = Histogram.build()
           .name("memory_save_duration_ms")
           .help("Memory save duration in milliseconds")
           .register();

       public static void recordSave(long durationMs) {
           saveTime.observe(durationMs);
       }
   }
   ```

### Phase 5: Full Rollout

1. **Enable by default**
   ```java
   public class MemoryConfig {
       public boolean vectorStorePersistence = true;  // Now enabled
   }
   ```

2. **Data Migration for Existing Players**
   ```java
   public class MemoryMigration {
       public static void migrateExistingPlayer(ServerPlayer player) {
           ForemanEntity foreman = findForemanForPlayer(player);
           if (foreman != null) {
               CompanionMemory memory = foreman.getCompanionMemory();

               // Check if migration needed
               if (memory.needsMigration()) {
                   LOGGER.info("Migrating memory for player {}", player.getName());
                   memory.migrate();

                   // Force save
                   player.getServerLevel().save();
               }
           }
       }
   }
   ```

---

## Summary

This document provides a comprehensive strategy for persisting the MineWright mod's memory systems using Minecraft's NBT format. Key takeaways:

### Implementation Checklist

- [x] CompanionMemory NBT serialization (already implemented)
- [x] MilestoneTracker NBT serialization (already implemented)
- [ ] InMemoryVectorStore NBT serialization with vector compression
- [ ] Version migration system for schema changes
- [ ] Performance optimization for large datasets
- [ ] Comprehensive test suite
- [ ] Gradual rollout with feature flags

### File Sizes

With compression and 8-bit quantization:
- 200 episodic memories: ~100KB
- 200 vectors (8-bit): ~75KB
- Semantic memories: ~50KB
- **Total per companion: ~225KB** (down from ~450KB)

### Performance Targets

- Save time: <100ms for 200 memories
- Load time: <50ms for 200 memories
- Vector search: <10ms for top-10 results

### Next Steps

1. Implement vector store persistence in `InMemoryVectorStore`
2. Add compression methods for float arrays
3. Create comprehensive test suite
4. Implement version migration system
5. Add monitoring and metrics
6. Beta test with selected players
7. Full rollout to all players

---

## References

- [Minecraft Wiki - NBT Format](https://minecraft.fandom.com/wiki/NBT_format)
- [Forge Documentation - Data Storage](https://docs.minecraftforge.net/en/latest/datastorage/)
- Current Implementation: `C:\Users\casey\steve\src\main\java\com\minewright\memory\CompanionMemory.java`
- Current Implementation: `C:\Users\casey\steve\src\main\java\com\minewright\memory\vector\InMemoryVectorStore.java`
