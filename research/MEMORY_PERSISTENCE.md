# Memory Persistence Patterns for AI Game Companions

## Research Document: Cross-Session Memory for Minecraft AI Companions

**Date:** 2026-02-27
**Research Focus:** Memory persistence architectures for AI game companions (Steve/MineWright)
**Target Environment:** Java 17, Minecraft Forge 1.20.1
**Application:** AI companions that remember players across sessions

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Persistence Architecture Patterns](#persistence-architecture-patterns)
4. [NBT Serialization for Minecraft](#nbt-serialization-for-minecraft)
5. [Database Integration Options](#database-integration-options)
6. [Vector Database for Semantic Memory](#vector-database-for-semantic-memory)
7. [Memory Compression & Summarization](#memory-compression--summarization)
8. [Episodic vs Semantic Memory](#episodic-vs-semantic-memory)
9. [Memory Decay & Importance Scoring](#memory-decay--importance-scoring)
10. [Cross-Session Personality Consistency](#cross-session-personality-consistency)
11. [Recommended Architecture](#recommended-architecture)
12. [Implementation Examples](#implementation-examples)
13. [References](#references)

---

## Executive Summary

AI game companions require sophisticated memory systems to build meaningful long-term relationships with players. This document provides comprehensive research on memory persistence patterns, specifically tailored for Minecraft Forge mods.

**Key Findings:**

1. **Hybrid Memory Architecture:** Combine working memory (in-game), episodic memory (events), and semantic memory (facts) with vector embeddings for semantic search

2. **NBT for Minecraft Compatibility:** Use Minecraft's native NBT format for persistence to ensure seamless integration with world saves

3. **Vector Databases for Semantic Search:** Implement in-memory vector stores with cosine similarity for intelligent memory retrieval

4. **Memory Compression is Essential:** Context window limitations require summarization and hierarchical memory management

5. **Personality Persistence:** Store personality traits and preferences to maintain consistent character across sessions

**Current State:** The codebase already has excellent foundation classes (`CompanionMemory`, `SteveMemory`, `InMemoryVectorStore`) that support episodic/semantic/emotional memories, relationship tracking, and NBT persistence. The main gaps are:
- Lack of vector embeddings for semantic search (placeholder implementation)
- No memory compression/summarization for long-term sessions
- Limited cross-session personality consistency mechanisms

---

## Current State Analysis

### Existing Memory Classes

#### 1. SteveMemory
**Location:** `C:\Users\casey\steve\src\main\java\com\steve\ai\memory\SteveMemory.java`

**Features:**
- Basic action history tracking (recent actions queue)
- Current goal tracking
- NBT serialization (`saveToNBT`, `loadFromNBT`)
- Limited to 20 recent actions

**Limitations:**
- No semantic search capabilities
- No emotional weight or importance scoring
- No relationship tracking
- Limited historical context

#### 2. CompanionMemory
**Location:** `C:\Users\casey\steve\src\main\java\com\steve\ai\memory\CompanionMemory.java`

**Features:**
- **Comprehensive memory types:**
  - Episodic memories (specific events with timestamps)
  - Semantic memories (facts about player)
  - Emotional memories (high-impact moments)
  - Conversational memory (inside jokes, discussed topics)
  - Working memory (recent context)

- **Relationship tracking:**
  - Rapport level (0-100)
  - Trust level (0-100)
  - Interaction count
  - Player preferences
  - Playstyle metrics

- **Personality system:**
  - Big Five traits (openness, conscientiousness, etc.)
  - Custom traits (humor, encouragement, formality)
  - Catchphrases and verbal tics
  - Mood tracking

- **Advanced features:**
  - Vector search support with `InMemoryVectorStore`
  - Milestone tracker for relationship milestones
  - NBT persistence for all memory types

**Strengths:**
- Excellent foundation with all necessary memory types
- Sophisticated relationship tracking
- Personality profile system
- Full NBT serialization

**Limitations:**
- Uses placeholder embedding model (no real semantic search)
- No memory compression/summarization
- Vectors not persisted in NBT (need to regenerate on load)

#### 3. InMemoryVectorStore
**Location:** `C:\Users\casey\steve\src\main\java\com\steve\ai\memory\vector\InMemoryVectorStore.java`

**Features:**
- Thread-safe vector storage with `ConcurrentHashMap`
- Cosine similarity search
- NBT persistence for vectors (as int arrays)
- Configurable dimensions (default: 384)

**Strengths:**
- Production-ready implementation
- Efficient cosine similarity calculation
- NBT serialization support
- Clean API

**Limitations:**
- No actual embedding generation (requires integration)
- Linear search (O(n) - acceptable for <10K memories)

### Current Persistence Implementation

The existing codebase demonstrates solid NBT serialization:

```java
// From CompanionMemory.java
public void saveToNBT(CompoundTag tag) {
    // Save relationship data
    tag.putInt("RapportLevel", rapportLevel.get());
    tag.putInt("TrustLevel", trustLevel.get());
    tag.putInt("InteractionCount", interactionCount.get());

    // Save episodic memories
    ListTag episodicList = new ListTag();
    for (EpisodicMemory memory : episodicMemories) {
        CompoundTag memoryTag = new CompoundTag();
        memoryTag.putString("EventType", memory.eventType);
        memoryTag.putString("Description", memory.description);
        memoryTag.putInt("EmotionalWeight", memory.emotionalWeight);
        memoryTag.putLong("Timestamp", memory.timestamp.toEpochMilli());
        episodicList.add(memoryTag);
    }
    tag.put("EpisodicMemories", episodicList);

    // ... similar for other memory types
}
```

---

## Persistence Architecture Patterns

### 1. Three-Tier Memory Architecture

Based on research from Mem0, Letta (MemGPT), and LangChain, the recommended architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    AI Companion Memory System                │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Working    │  │   Semantic   │  │  Episodic    │      │
│  │   Memory     │  │   Memory     │  │   Memory     │      │
│  │              │  │              │  │              │      │
│  │ • In-memory  │  │ • Key-Value  │  │ • Vector DB  │      │
│  │ • Circular   │  │ • Confidence │  │ • Embeddings │      │
│  │ • 20 items   │  │ • Facts      │  │ • Time-decay │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│           │                  │                  │            │
│           └──────────────────┼──────────────────┘            │
│                              │                               │
│                              ▼                               │
│                    ┌──────────────────┐                      │
│                    │ Memory Retriever │                      │
│                    │                  │                      │
│                    │ • Hybrid Search  │                      │
│                    │ • Scoring        │                      │
│                    │ • MMR Diversity  │                      │
│                    └──────────────────┘                      │
│                              │                               │
│                              ▼                               │
│                    ┌──────────────────┐                      │
│                    │  Relationship    │                      │
│                    │  Tracker         │                      │
│                    │                  │                      │
│                    │ • Rapport (0-100)│                      │
│                    │ • Trust (0-100)  │                      │
│                    │ • Inside Jokes   │                      │
│                    │ • Player Prefs   │                      │
│                    └──────────────────┘                      │
│                              │                               │
│                              ▼                               │
│                    ┌──────────────────┐                      │
│                    │   Persistence    │                      │
│                    │                  │                      │
│                    │ • NBT Format     │                      │
│                    │ • World Save     │                      │
│                    │ • Auto-save      │                      │
│                    └──────────────────┘                      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 2. Memory Type Responsibilities

| Memory Type | Purpose | Storage | Duration | Example |
|-------------|---------|---------|----------|---------|
| **Working Memory** | Current context | In-memory | Session | "We're building a tower" |
| **Semantic Memory** | Facts and knowledge | Key-value store | Permanent | "Player likes cobblestone" |
| **Episodic Memory** | Specific events | Vector DB | Permanent | "Built tower together on day 5" |
| **Emotional Memory** | High-impact moments | Prioritized list | Permanent | "First time defeating Wither together" |
| **Procedural Memory** | Learned patterns | HashMap | Permanent | "Player prefers 3-block hallways" |
| **Conversational Memory** | Jokes and references | List with ref counting | Permanent | "Remember when you called gravel 'gravity blocks'?" |

### 3. Memory Flow

**Recording a Memory:**
```
1. Event occurs (player interaction)
2. Determine memory type (episodic, semantic, emotional)
3. Extract key facts via LLM
4. Generate embedding (if episodic)
5. Store in appropriate memory store
6. Update relationship metrics
7. Trigger persistence if needed
```

**Retrieving Context:**
```
1. Query: "Give context for current situation"
2. Generate query embedding
3. Parallel search:
   - Vector search (episodic)
   - Keyword match (semantic)
   - Working memory (recent)
4. Combine and score results
5. Apply MMR for diversity
6. Return top K memories
```

---

## NBT Serialization for Minecraft

### Why NBT?

**Advantages:**
- Native Minecraft format - automatic save/load
- Works in single-player and multiplayer
- No external files to manage
- Backward compatible across versions
- Supports complex nested structures

**Limitations:**
- Size limitations (though rarely an issue for text data)
- Not human-readable (use NBTExplorer for debugging)
- Manual serialization required

### NBT Data Type Mapping

| Java Type | NBT Type | Example Usage |
|-----------|----------|---------------|
| `String` | `StringTag` | Names, descriptions |
| `int` | `IntTag` | Counts, levels, weights |
| `long` | `LongTag` | Timestamps (epoch millis) |
| `double` | `DoubleTag` | Scores, confidence values |
| `boolean` | `ByteTag` (0/1) | Flags |
| `List<T>` | `ListTag` | Memory collections |
| `Map<K,V>` | `CompoundTag` | Structured data |
| `float[]` | `IntArrayTag` (scaled) | Embedding vectors |

### NBT Persistence Pattern

```java
public class PersistentMemorySystem {
    private final Map<String, EpisodicMemory> episodicMemories = new ConcurrentHashMap<>();
    private final Map<String, SemanticMemory> semanticMemories = new ConcurrentHashMap<>();

    /**
     * Save all memories to NBT format.
     * Called from entity's saveAdditional() method.
     */
    public void saveToNBT(CompoundTag tag) {
        // Save episodic memories
        ListTag episodicList = new ListTag();
        for (EpisodicMemory memory : episodicMemories.values()) {
            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("Id", memory.id());
            memoryTag.putString("EventType", memory.eventType());
            memoryTag.putString("Description", memory.description());
            memoryTag.putInt("EmotionalWeight", memory.emotionalWeight());
            memoryTag.putLong("Timestamp", memory.timestamp().toEpochMilli());

            // Save embedding vector
            if (memory.embedding() != null) {
                int[] vectorInt = new int[memory.embedding().length];
                for (int i = 0; i < memory.embedding().length; i++) {
                    // Scale float to int for NBT storage
                    vectorInt[i] = Math.round(memory.embedding()[i] * 1000.0f);
                }
                memoryTag.putIntArray("Embedding", vectorInt);
            }

            episodicList.add(memoryTag);
        }
        tag.put("EpisodicMemories", episodicList);

        // Save semantic memories
        CompoundTag semanticTag = new CompoundTag();
        for (SemanticMemory memory : semanticMemories.values()) {
            CompoundTag memTag = new CompoundTag();
            memTag.putString("Category", memory.category());
            memTag.putString("Key", memory.key());
            memTag.putString("Value", String.valueOf(memory.value()));
            memTag.putInt("Confidence", memory.confidence());
            memTag.putLong("LearnedAt", memory.learnedAt().toEpochMilli());
            semanticTag.put(memory.id(), memTag);
        }
        tag.put("SemanticMemories", semanticTag);
    }

    /**
     * Load all memories from NBT format.
     * Called from entity's readAdditional() method.
     */
    public void loadFromNBT(CompoundTag tag) {
        // Load episodic memories
        if (tag.contains("EpisodicMemories")) {
            episodicMemories.clear();
            ListTag episodicList = tag.getList("EpisodicMemories", 10); // 10 = CompoundTag
            for (int i = 0; i < episodicList.size(); i++) {
                CompoundTag memoryTag = episodicList.getCompound(i);

                // Load embedding vector
                float[] embedding = null;
                if (memoryTag.contains("Embedding")) {
                    int[] vectorInt = memoryTag.getIntArray("Embedding");
                    embedding = new float[vectorInt.length];
                    for (int j = 0; j < vectorInt.length; j++) {
                        embedding[j] = vectorInt[j] / 1000.0f;
                    }
                }

                EpisodicMemory memory = new EpisodicMemory(
                    memoryTag.getString("Id"),
                    memoryTag.getString("EventType"),
                    memoryTag.getString("Description"),
                    memoryTag.getInt("EmotionalWeight"),
                    Instant.ofEpochMilli(memoryTag.getLong("Timestamp")),
                    embedding
                );
                episodicMemories.put(memory.id(), memory);
            }
        }

        // Load semantic memories
        if (tag.contains("SemanticMemories")) {
            semanticMemories.clear();
            CompoundTag semanticTag = tag.getCompound("SemanticMemories");
            for (String key : semanticTag.getAllKeys()) {
                CompoundTag memTag = semanticTag.getCompound(key);
                SemanticMemory memory = new SemanticMemory(
                    key,
                    memTag.getString("Category"),
                    memTag.getString("Key"),
                    memTag.getString("Value"),
                    memTag.getInt("Confidence"),
                    Instant.ofEpochMilli(memTag.getLong("LearnedAt"))
                );
                semanticMemories.put(key, memory);
            }
        }
    }
}
```

### NBT Best Practices

1. **Version Control:** Always include a version number
   ```java
   tag.putInt("Version", CURRENT_VERSION);
   ```

2. **Default Values:** Check for tag presence before reading
   ```java
   int rapport = tag.contains("RapportLevel") ? tag.getInt("RapportLevel") : 10;
   ```

3. **Data Validation:** Validate loaded data
   ```java
   if (rapportLevel < 0 || rapportLevel > 100) {
       LOGGER.warn("Invalid rapport level: {}", rapportLevel);
       rapportLevel = 10; // Reset to default
   }
   ```

4. **Compression:** For large datasets, use GZIP compression
   ```java
   // NBT automatically compresses when saved to file
   ```

---

## Database Integration Options

### Comparison of Embedded Databases for Java

| Database | Size | Performance | SQL Support | Java Integration | Best For |
|----------|------|-------------|-------------|------------------|----------|
| **SQLite** | ~250-500KB | Fast | Full SQL | JDBC | Complex queries, multi-table |
| **H2** | ~2MB | Very Fast | Full SQL | JDBC | In-memory + persistent hybrid |
| **LevelDB** | ~350KB | Very Fast | NoSQL | JNI/LevelDB-JNI | Key-value, high throughput |
| **MapDB** | ~500KB | Fast | NoSQL | Pure Java | Concurrent collections |
| **RocksDB** | ~5MB | Extreme | NoSQL | JNI | Write-heavy workloads |

### SQLite Integration

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.2.0</version>
</dependency>
```

**Implementation Pattern:**
```java
public class SQLiteMemoryStore {
    private static final String DB_PATH = "steve_memories.db";
    private Connection connection;

    public void initialize() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

        // Create tables
        String createEpisodicTable = """
            CREATE TABLE IF NOT EXISTS episodic_memories (
                id TEXT PRIMARY KEY,
                event_type TEXT NOT NULL,
                description TEXT NOT NULL,
                emotional_weight INTEGER,
                timestamp INTEGER NOT NULL,
                player_uuid TEXT NOT NULL
            )
            """;

        String createSemanticTable = """
            CREATE TABLE IF NOT EXISTS semantic_memories (
                id TEXT PRIMARY KEY,
                category TEXT NOT NULL,
                key TEXT NOT NULL,
                value TEXT NOT NULL,
                confidence INTEGER NOT NULL,
                learned_at INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                UNIQUE(category, key, player_uuid)
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createEpisodicTable);
            stmt.execute(createSemanticTable);
        }
    }

    public void saveEpisodicMemory(EpisodicMemory memory, String playerUuid) {
        String sql = """
            INSERT OR REPLACE INTO episodic_memories
            (id, event_type, description, emotional_weight, timestamp, player_uuid)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, memory.id());
            pstmt.setString(2, memory.eventType());
            pstmt.setString(3, memory.description());
            pstmt.setInt(4, memory.emotionalWeight());
            pstmt.putLong(5, memory.timestamp().toEpochMilli());
            pstmt.setString(6, playerUuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to save episodic memory", e);
        }
    }

    public List<EpisodicMemory> loadMemoriesForPlayer(String playerUuid, int limit) {
        String sql = """
            SELECT * FROM episodic_memories
            WHERE player_uuid = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<EpisodicMemory> memories = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                memories.add(new EpisodicMemory(
                    rs.getString("id"),
                    rs.getString("event_type"),
                    rs.getString("description"),
                    rs.getInt("emotional_weight"),
                    Instant.ofEpochMilli(rs.getLong("timestamp"))
                ));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load memories", e);
        }
        return memories;
    }
}
```

### H2 Database (Recommended for Minecraft)

**Advantages:**
- Faster than SQLite for in-memory operations
- Can run in pure in-memory mode for session data
- Supports both SQL and MVStore (NoSQL)
- Better concurrency handling

**Maven Dependency:**
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
</dependency>
```

**Hybrid Mode:**
```java
public class H2MemoryStore {
    // Use in-memory for session data, file for persistent data
    private Connection sessionConn;  // "jdbc:h2:mem:session"
    private Connection persistentConn;  // "jdbc:h2:file:./steve_persistent"

    public void initialize() throws SQLException {
        sessionConn = DriverManager.getConnection("jdbc:h2:mem:session");
        persistentConn = DriverManager.getConnection("jdbc:h2:file:./steve_persistent");

        // Session data for working memory
        try (Statement stmt = sessionConn.createStatement()) {
            stmt.execute("CREATE TABLE working_memory (content TEXT, timestamp BIGINT)");
        }

        // Persistent data for long-term memories
        try (Statement stmt = persistentConn.createStatement()) {
            stmt.execute("CREATE TABLE episodic_memories (id VARCHAR PRIMARY KEY, data CLOB)");
        }
    }
}
```

### Recommendation: NBT + Optional SQLite

**For Minecraft Forge mods, the recommended approach is:**

1. **Primary: NBT** for all critical companion data
   - Automatic save/load
   - Works in multiplayer
   - No external dependencies

2. **Optional: SQLite/H2** for advanced features
   - Cross-player analytics
   - Historical statistics
   - Debug/query interface

3. **Never:** Rely solely on external databases for critical data
   - Players expect data to persist with world save
   - External databases break when copying worlds

---

## Vector Database for Semantic Memory

### Why Vector Search?

**Problem:** Keyword search is limited
- Query: "building projects"
- Memory: "We constructed a tower together"
- Result: No match (different words)

**Solution:** Semantic search with embeddings
- Both converted to vectors
- Cosine similarity: 0.87 (very similar!)
- Result: Match found (similar meanings)

### Embedding Models Comparison

| Model | Dimensions | Size | Speed | Quality | Recommendation |
|-------|------------|------|-------|---------|----------------|
| **all-MiniLM-L6-v2** | 384 | ~80MB | Very Fast | Good | **Best for Minecraft** |
| **all-MiniLM-L12-v2** | 384 | ~120MB | Fast | Better | If quality matters |
| **bge-small-en-v1.5** | 384 | ~130MB | Fast | Very Good | **Best overall** |
| **e5-small-v2** | 384 | ~130MB | Fast | Good | Alternative |
| **bge-base-en-v1.5** | 768 | ~400MB | Medium | Excellent | High quality |

### In-Memory Vector Store Implementation

The codebase already has an excellent `InMemoryVectorStore` class. Here's how to integrate it with a real embedding model:

```java
public class EnhancedMemorySystem {
    private final InMemoryVectorStore<EpisodicMemory> vectorStore;
    private final EmbeddingModel embeddingModel;

    public EnhancedMemorySystem() {
        // Initialize embedding model
        this.embeddingModel = new LocalEmbeddingModel("models/all-MiniLM-L6-v2.onnx");
        this.vectorStore = new InMemoryVectorStore<>(embeddingModel.getDimension());
    }

    /**
     * Record an episodic memory with semantic search support.
     */
    public void recordExperience(String eventType, String description, int emotionalWeight) {
        // Create memory
        EpisodicMemory memory = new EpisodicMemory(
            UUID.randomUUID().toString(),
            eventType,
            description,
            emotionalWeight,
            Instant.now()
        );

        // Generate embedding
        float[] embedding = embeddingModel.embed(description);

        // Store in vector store
        int vectorId = vectorStore.add(embedding, memory);

        LOGGER.debug("Recorded memory with vector ID: {}", vectorId);
    }

    /**
     * Find relevant memories using semantic search.
     */
    public List<EpisodicMemory> findRelevantMemories(String query, int topK) {
        // Generate query embedding
        float[] queryEmbedding = embeddingModel.embed(query);

        // Search vector store
        List<VectorSearchResult<EpisodicMemory>> results =
            vectorStore.search(queryEmbedding, topK);

        // Extract memories
        return results.stream()
            .map(VectorSearchResult::getData)
            .collect(Collectors.toList());
    }
}
```

### Cosine Similarity Scoring

The existing implementation already has this:

```java
private double cosineSimilarity(float[] a, float[] b) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < a.length; i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

**Interpretation:**
- 1.0 = Identical meaning
- 0.8-0.9 = Very similar
- 0.6-0.7 = Somewhat related
- 0.5 = Weakly related
- <0.5 = Not related

### Hybrid Search (Vector + Keyword)

Combines semantic and keyword search for best results:

```java
public class HybridMemoryRetriever {
    private final VectorRetriever vectorRetriever;
    private final KeywordRetriever keywordRetriever;

    public List<Memory> hybridSearch(String query, int topK) {
        // Get results from both methods
        List<ScoredMemory> vectorResults = vectorRetriever.search(query, topK * 2);
        List<ScoredMemory> keywordResults = keywordRetriever.search(query, topK * 2);

        // Combine scores (70% vector, 30% keyword)
        Map<String, Double> combinedScores = new HashMap<>();

        for (ScoredMemory result : vectorResults) {
            combinedScores.merge(result.memory().id(),
                result.score() * 0.7, Double::sum);
        }

        for (ScoredMemory result : keywordResults) {
            combinedScores.merge(result.memory().id(),
                result.score() * 0.3, Double::sum);
        }

        // Return top K by combined score
        return combinedScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topK)
            .map(e -> memories.get(e.getKey()))
            .toList();
    }
}
```

---

## Memory Compression & Summarization

### The Problem: Context Window Limits

LLMs have limited context windows (4K-128K tokens). Long conversations exceed this, requiring:
1. Discarding old messages (loses context)
2. Starting new conversations (loses relationship)
3. Compressing context (maintains essentials)

### Compression Strategies

#### 1. Hierarchical Summarization

Maintain summaries at multiple time granularities:

```java
public class HierarchicalMemorySummarizer {
    private Summary sessionSummary;    // Current play session
    private Summary dailySummary;      // Last 24 hours
    private Summary weeklySummary;     // Last 7 days
    private Summary lifetimeSummary;   // All-time highlights

    public void addConversation(List<ConversationTurn> conversation) {
        // Update session summary
        sessionSummary = updateSummary(sessionSummary, conversation);

        // Periodically roll up to higher levels
        if (shouldUpdateDaily()) {
            dailySummary = rollUpSummaries(List.of(sessionSummary, dailySummary));
        }
    }

    private Summary rollUpSummaries(List<Summary> summaries) {
        String combined = summaries.stream()
            .map(Summary::content)
            .collect(Collectors.joining("\n"));

        String rolledUp = llmClient.complete(
            "Create a concise summary of these summaries:\n" + combined
        );

        return new Summary(rolledUp, Instant.now());
    }
}
```

#### 2. Extractive Summarization

Select most important sentences:

```java
public class ExtractiveSummarizer {
    public String summarize(List<String> messages, int sentenceCount) {
        // Score each sentence by importance
        List<SentenceScore> scored = messages.stream()
            .flatMap(msg -> Arrays.stream(msg.split("\\.")))
            .map(String::trim)
            .filter(s -> s.length() > 10)
            .map(sentence -> new SentenceScore(
                sentence,
                calculateImportance(sentence)
            ))
            .sorted(Comparator.comparing(SentenceScore::score).reversed())
            .limit(sentenceCount)
            .toList();

        // Reconstruct summary in original order
        return scored.stream()
            .map(SentenceScore::sentence)
            .collect(Collectors.joining(". ")) + ".";
    }

    private double calculateImportance(String sentence) {
        double score = 0;

        // Contains numbers/coordinates
        if (sentence.matches(".*\\d+.*")) score += 0.2;

        // Contains emotional words
        if (containsEmotionalWords(sentence)) score += 0.3;

        // Contains player name
        if (sentence.toLowerCase().contains("player")) score += 0.2;

        // Length (prefer medium-length sentences)
        int length = sentence.split(" ").length;
        if (length >= 5 && length <= 20) score += 0.2;

        return score;
    }
}
```

#### 3. Abstractive Summarization (LLM-Based)

Use LLM to generate new summaries:

```java
public class AbstractiveSummarizer {
    private final LLMApi llmClient;

    public String summarizeConversation(List<ConversationTurn> conversation) {
        String prompt = buildSummarizationPrompt(conversation);
        String summary = llmClient.complete(prompt);

        // Extract key facts from summary
        extractKeyFacts(summary);

        return summary;
    }

    private String buildSummarizationPrompt(List<ConversationTurn> conversation) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Summarize the following conversation between a player ");
        prompt.append("and Steve (AI companion). Extract:\n");
        prompt.append("1. Key events and experiences shared\n");
        prompt.append("2. Player preferences revealed\n");
        prompt.append("3. Emotional moments\n");
        prompt.append("4. Any inside jokes or memorable phrases\n\n");
        prompt.append("Conversation:\n");

        for (ConversationTurn turn : conversation) {
            prompt.append(turn.speaker()).append(": ").append(turn.message()).append("\n");
        }

        prompt.append("\nSummary:");
        return prompt.toString();
    }
}
```

### Fact Extraction

Extract structured facts from unstructured conversations:

```java
public class FactExtractor {
    private final LLMApi llmClient;

    public List<ExtractedFact> extractFacts(String conversation) {
        String prompt = """
            Extract key facts from this conversation. Return as JSON:
            {
              "preferences": [{"key": "favorite_block", "value": "cobblestone"}],
              "skills": [{"key": "building", "level": "advanced"}],
              "events": [{"type": "build", "description": "built a tower"}],
              "emotions": [{"type": "joy", "trigger": "finding diamonds"}]
            }

            Conversation: %s
            """.formatted(conversation);

        String response = llmClient.complete(prompt);
        return parseFactsFromJson(response);
    }
}
```

---

## Episodic vs Semantic Memory

### Memory Type Comparison

| Aspect | Episodic Memory | Semantic Memory |
|--------|----------------|-----------------|
| **What** | Specific events/experiences | Facts and knowledge |
| **Examples** | "Built tower together on day 5" | "Player likes cobblestone" |
| **Temporal** | Yes (timestamps) | No (timeless facts) |
| **Search** | Vector/semantic | Keyword/lookup |
| **Decay** | Time-based | Confidence-based |
| **Storage** | Vector database | Key-value store |

### Episodic Memory Structure

```java
public class EpisodicMemory {
    private final String id;
    private final String eventType;        // "build", "combat", "explore"
    private final String description;      // What happened
    private final Instant timestamp;       // When it happened
    private final int emotionalWeight;     // -10 to +10 significance
    private final float[] embedding;       // For semantic search
    private final List<String> participants; // Who was involved
    private final Location location;       // Where it happened

    // Search relevance score
    public double calculateRelevance() {
        long daysSince = ChronoUnit.DAYS.between(timestamp, Instant.now());
        double recencyFactor = Math.pow(0.9, daysSince); // 10% daily decay
        double emotionalFactor = Math.abs(emotionalWeight) / 10.0;
        return recencyFactor * emotionalFactor;
    }
}
```

### Semantic Memory Structure

```java
public class SemanticMemory {
    private final String id;
    private final String category;         // "preference", "skill", "habit"
    private final String key;              // "favorite_block"
    private final Object value;            // "cobblestone"
    private final Instant learnedAt;       // When learned
    private int confidence;                // 1-10 reliability score
    private int confirmationCount;         // Times reinforced
    private int contradictionCount;        // Times contradicted

    // Update confidence based on new evidence
    public void confirm() {
        confirmationCount++;
        confidence = Math.min(10, confidence + 1);
    }

    public void contradict() {
        contradictionCount++;
        confidence = Math.max(1, confidence - 2);

        // Mark as unreliable if too many contradictions
        if (contradictionCount > confirmationCount * 2) {
            confidence = 1;
        }
    }

    // Check if still reliable
    public boolean isReliable() {
        return confidence >= 3 &&
               contradictionCount <= confirmationCount * 2;
    }
}
```

### Conversion: Episodic → Semantic

Extract facts from episodic memories:

```java
public class MemoryExtractor {
    public void extractFactsFromEpisodic(EpisodicMemory memory) {
        String prompt = """
            Extract player facts from this experience:
            "%s"

            Return as JSON:
            {
              "preferences": [...],
              "skills": [...],
              "habits": [...]
            }
            """.formatted(memory.description());

        String response = llmClient.complete(prompt);
        ExtractedFacts facts = parseFacts(response);

        // Store as semantic memories
        for (Preference pref : facts.preferences()) {
            learnPlayerFact("preference", pref.key(), pref.value());
        }
    }
}
```

---

## Memory Decay & Importance Scoring

### Why Memory Decay?

Prevents information overload and ensures relevance:
- Recent memories are more relevant
- Important memories resist decay
- Forgotten memories can be relearned

### Decay Strategies

#### 1. Time-Based Decay

```java
public class TimeBasedDecay {
    private static final double DAILY_DECAY_RATE = 0.9; // 10% per day

    public double calculateDecayFactor(Instant memoryTime, Instant currentTime) {
        long daysSince = ChronoUnit.DAYS.between(memoryTime, currentTime);
        return Math.pow(DAILY_DECAY_RATE, daysSince);
    }

    // Example: 10 days ago
    // Factor = 0.9^10 ≈ 0.35 (35% of original importance)
}
```

#### 2. Importance-Based Decay

Important memories decay slower:

```java
public class ImportanceBasedDecay {
    public double calculateDecayFactor(
        Instant memoryTime,
        Instant currentTime,
        int emotionalWeight
    ) {
        long daysSince = ChronoUnit.DAYS.between(memoryTime, currentTime);

        // Higher emotional weight = slower decay
        double decayRate = 0.9 - (Math.abs(emotionalWeight) * 0.01);
        decayRate = Math.max(0.8, Math.min(0.95, decayRate));

        return Math.pow(decayRate, daysSince);
    }

    // Example: emotionalWeight = 8 (very important)
    // Decay rate = 0.9 - 0.08 = 0.82
    // After 10 days: 0.82^10 ≈ 0.14 (slower decay than unimportant memories)
}
```

#### 3. Access-Based Decay

Frequently accessed memories resist decay:

```java
public class AccessBasedDecay {
    private final Map<String, Integer> accessCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastAccessed = new ConcurrentHashMap<>();

    public double calculateDecayFactor(String memoryId) {
        int accessCount = accessCounts.getOrDefault(memoryId, 0);
        Instant lastAccess = lastAccessed.get(memoryId);

        long daysSinceAccess = ChronoUnit.DAYS.between(
            lastAccess != null ? lastAccess : Instant.now(),
            Instant.now()
        );

        // More access = slower decay
        double decayRate = 0.9 - (Math.min(accessCount, 10) * 0.01);
        double timeFactor = Math.pow(decayRate, daysSinceAccess);

        // Access bonus (frequently accessed memories stay relevant)
        double accessBonus = Math.min(1.0, accessCount / 20.0);

        return timeFactor + accessBonus;
    }

    public void recordAccess(String memoryId) {
        accessCounts.merge(memoryId, 1, Integer::sum);
        lastAccessed.put(memoryId, Instant.now());
    }
}
```

### Multi-Factor Scoring

Combine multiple factors for final relevance score:

```java
public class MemoryScorer {
    private static final double RECENCY_WEIGHT = 0.2;
    private static final double IMPORTANCE_WEIGHT = 0.5;
    private static final double ACCESS_WEIGHT = 0.3;

    public double calculateScore(Memory memory, Instant currentTime) {
        // Recency score (time-based decay)
        long daysSince = ChronoUnit.DAYS.between(
            memory.timestamp(), currentTime
        );
        double recencyScore = Math.pow(0.9, daysSince);

        // Importance score (emotional weight)
        double importanceScore = Math.abs(memory.emotionalWeight()) / 10.0;

        // Access score (frequency)
        double accessScore = Math.min(1.0, memory.accessCount() / 10.0);

        // Combined score
        return (recencyScore * RECENCY_WEIGHT) +
               (importanceScore * IMPORTANCE_WEIGHT) +
               (accessScore * ACCESS_WEIGHT);
    }
}
```

### Memory Consolidation

Merge similar old memories:

```java
public class MemoryConsolidator {
    public void consolidateOldMemories() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        List<EpisodicMemory> oldMemories = episodicMemories.values().stream()
            .filter(m -> m.timestamp().isBefore(cutoff))
            .filter(m -> m.accessCount() < 5) // Rarely accessed
            .collect(Collectors.toList());

        // Group by similarity
        Map<String, List<EpisodicMemory>> groups = groupBySimilarity(oldMemories);

        // Consolidate each group
        for (Map.Entry<String, List<EpisodicMemory>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                consolidateGroup(entry.getValue());
            }
        }
    }

    private void consolidateGroup(List<EpisodicMemory> memories) {
        // Create consolidated memory
        String summary = createSummary(memories);

        // Calculate combined emotional weight
        int combinedWeight = memories.stream()
            .mapToInt(EpisodicMemory::emotionalWeight)
            .sum() / memories.size();

        // Create new consolidated memory
        EpisodicMemory consolidated = new EpisodicMemory(
            UUID.randomUUID().toString(),
            "consolidated",
            summary,
            combinedWeight,
            Instant.now()
        );

        // Remove old memories, add consolidated
        memories.forEach(m -> episodicMemories.remove(m.id()));
        episodicMemories.put(consolidated.id(), consolidated);
    }
}
```

---

## Cross-Session Personality Consistency

### The Challenge

Players expect AI companions to:
- Remember personality traits between sessions
- Maintain consistent behaviors
- Recognize returning players
- Evolve relationships naturally

### Personality Persistence Pattern

#### 1. Personality Profile Storage

Already implemented in `CompanionMemory.PersonalityProfile`:

```java
public class PersonalityProfile {
    // Big Five traits (0-100)
    public int openness = 70;
    public int conscientiousness = 80;
    public int extraversion = 60;
    public int agreeableness = 75;
    public int neuroticism = 30;

    // Custom traits
    public int humor = 65;
    public int encouragement = 80;
    public int formality = 40;

    // Verbal patterns
    public List<String> catchphrases;
    public String favoriteBlock;
    public String workStyle;
    public String mood;

    // Persist to NBT
    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Openness", openness);
        tag.putInt("Conscientiousness", conscientiousness);
        // ... etc

        ListTag catchphrasesList = new ListTag();
        for (String phrase : catchphrases) {
            catchphrasesList.add(StringTag.valueOf(phrase));
        }
        tag.put("Catchphrases", catchphrasesList);
    }

    // Load from NBT
    public void loadFromNBT(CompoundTag tag) {
        openness = tag.getInt("Openness");
        conscientiousness = tag.getInt("Conscientiousness");
        // ... etc

        if (tag.contains("Catchphrases")) {
            ListTag catchphrasesList = tag.getList("Catchphrases", 8);
            catchphrases.clear();
            for (int i = 0; i < catchphrasesList.size(); i++) {
                catchphrases.add(catchphrasesList.getString(i));
            }
        }
    }
}
```

#### 2. Cross-Session Recognition

Identify returning players:

```java
public class PlayerRecognition {
    private final Map<String, PlayerProfile> knownPlayers = new ConcurrentHashMap<>();

    public void onPlayerJoin(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        PlayerProfile profile = knownPlayers.get(uuid);

        if (profile == null) {
            // New player
            LOGGER.info("Meeting new player: {}", player.getName());
            profile = new PlayerProfile(player);
            knownPlayers.put(uuid, profile);
        } else {
            // Returning player
            long daysSince = ChronoUnit.DAYS.between(
                profile.lastSeen(), Instant.now()
            );
            LOGGER.info("{} returned after {} days",
                player.getName(), daysSince);

            // Adjust personality based on absence
            if (daysSince > 30) {
                // Long absence - slightly warmer welcome
                profile.getRelationship().adjustRapport(5);
            }
        }

        profile.lastSeen(Instant.now());
    }
}
```

#### 3. Personality Stability + Evolution

Balance consistency with growth:

```java
public class PersonalityEvolution {
    private PersonalityProfile basePersonality; // Original personality
    private PersonalityProfile currentPersonality; // Evolved personality
    private final List<PersonalityChangeEvent> history = new ArrayList<>();

    /**
     * Gradually evolve personality based on experiences.
     */
    public void evolveFrom(EpisodicMemory experience) {
        int delta = calculatePersonalityDelta(experience);

        // Limit how much personality can change
        delta = Math.max(-5, Math.min(5, delta));

        // Apply to trait
        String trait = getAffectedTrait(experience.eventType());
        adjustTrait(trait, delta);

        // Record change
        history.add(new PersonalityChangeEvent(
            trait, delta, experience, Instant.now()
        ));

        LOGGER.debug("Personality evolved: {} {} by {}", trait, delta > 0 ? "+" : "", delta);
    }

    /**
     * Revert towards base personality (homeostasis).
     */
    public void stabilize() {
        double driftFactor = 0.05; // 5% drift toward base per session

        currentPersonality.openness += (basePersonality.openness - currentPersonality.openness) * driftFactor;
        currentPersonality.conscientiousness += (basePersonality.conscientiousness - currentPersonality.conscientiousness) * driftFactor;
        // ... etc

        LOGGER.debug("Personality stabilized toward baseline");
    }

    private int calculatePersonalityDelta(EpisodicMemory experience) {
        // Positive experiences increase agreeableness
        if (experience.eventType().equals("shared_success") && experience.emotionalWeight() > 5) {
            return 2;
        }

        // Negative experiences may increase neuroticism
        if (experience.eventType().equals("failure") && experience.emotionalWeight() < -5) {
            return -1;
        }

        return 0;
    }
}
```

#### 4. Behavioral Consistency Checks

Ensure actions match personality:

```java
public class PersonalityValidator {
    /**
     * Check if proposed action matches personality.
     */
    public boolean validateAction(String action, PersonalityProfile personality) {
        // Extract traits from action
        String actionType = extractActionType(action);
        String tone = extractTone(action);

        // Check against personality
        switch (actionType) {
            case "humorous":
                return personality.humor >= 50;

            case "formal":
                return personality.formality >= 60;

            case "enthusiastic":
                return personality.extraversion >= 60;

            default:
                return true;
        }
    }

    /**
     * Adjust response to match personality.
     */
    public String adjustResponse(String response, PersonalityProfile personality) {
        if (personality.formality > 70) {
            return makeMoreFormal(response);
        } else if (personality.formality < 30) {
            return makeMoreCasual(response);
        }

        if (personality.humor > 70) {
            return addHumor(response);
        }

        return response;
    }
}
```

---

## Recommended Architecture

### System Overview

Based on research and existing codebase, the recommended architecture:

```
┌──────────────────────────────────────────────────────────────────┐
│                     Steve Memory System                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                    Memory Manager                           │  │
│  │                                                              │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐           │  │
│  │  │  Working   │  │  Semantic  │  │  Episodic  │           │  │
│  │  │  Memory    │  │  Memory    │  │  Memory    │           │  │
│  │  │            │  │            │  │            │           │  │
│  │  │ • Circular │  │ • Key-Value│  │ • Vectors  │           │  │
│  │  │ • 20 items│  │ • Confidence│  │ • Time-decay│           │  │
│  │  │ • Session  │  │ • Permanent│  │ • Embedded │           │  │
│  │  └────────────┘  └────────────┘  └────────────┘           │  │
│  │                                                             │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐           │  │
│  │  │ Emotional  │  │Conversatnl │  │Procedural  │           │  │
│  │  │  Memory    │  │  Memory    │  │  Memory    │           │  │
│  │  └────────────┘  └────────────┘  └────────────┘           │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│                              ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                  Memory Retriever                           │  │
│  │                                                              │  │
│  │  • Hybrid search (vector + keyword)                         │  │
│  │  • Multi-factor scoring (recency + importance + access)     │  │
│  │  • MMR diversity (reduce redundancy)                        │  │
│  │  • Context-aware retrieval                                  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│                              ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                  Relationship Manager                       │  │
│  │                                                              │  │
│  │  • Rapport level (0-100)                                     │  │
│  │  • Trust level (0-100)                                       │  │
│  │  • Inside jokes                                              │  │
│  │  • Player preferences                                        │  │
│  │  • Shared milestones                                         │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│                              ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                 Personality Manager                         │  │
│  │                                                              │  │
│  │  • Base personality (persistent)                             │  │
│  │  • Current personality (evolving)                            │  │
│  │  • Mood state (dynamic)                                      │  │
│  │  • Behavioral validation                                     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│                              ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   Persistence Layer                          │  │
│  │                                                              │  │
│  │  • NBT serialization (primary)                               │  │
│  │  • World save integration                                    │  │
│  │  • Auto-save on memory change                                │  │
│  │  • Version migration                                         │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Implementation Priority

#### Phase 1: Real Embedding Integration (Week 1-2)
**Goal:** Replace placeholder embedding model with real implementation

**Tasks:**
1. Download and integrate `all-MiniLM-L6-v2` model
2. Replace `PlaceholderEmbeddingModel` with real implementation
3. Test semantic search accuracy
4. Verify NBT persistence of vectors

**Deliverable:** Working semantic search with real embeddings

#### Phase 2: Memory Compression (Week 3)
**Goal:** Implement summarization for long sessions

**Tasks:**
1. Create `HierarchicalMemorySummarizer`
2. Implement extractive summarization
3. Add fact extraction from conversations
4. Create summary roll-up logic

**Deliverable:** Automatic memory compression for long conversations

#### Phase 3: Enhanced Retrieval (Week 4)
**Goal:** Improve memory retrieval with advanced scoring

**Tasks:**
1. Implement multi-factor scoring
2. Add hybrid search (vector + keyword)
3. Create MMR for diversity
4. Add context-aware retrieval

**Deliverable:** Sophisticated memory retrieval system

#### Phase 4: Personality Evolution (Week 5)
**Goal:** Add personality consistency and evolution

**Tasks:**
1. Implement personality homeostasis
2. Add behavioral validation
3. Create personality evolution from experiences
4. Add cross-session recognition

**Deliverable:** Dynamic personality that evolves naturally

#### Phase 5: Integration & Testing (Week 6)
**Goal:** Integrate with LLM and test end-to-end

**Tasks:**
1. Connect memory to `PromptBuilder`
2. Add memory context to LLM calls
3. Create memory debug UI
4. Performance optimization

**Deliverable:** Fully integrated memory system

---

## Implementation Examples

### Complete Memory Service

```java
package com.steve.ai.memory;

import com.steve.ai.memory.embedding.LocalEmbeddingModel;
import com.steve.ai.memory.vector.InMemoryVectorStore;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complete memory service for AI companions.
 * Integrates all memory types with persistence.
 */
public class CompanionMemoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanionMemoryService.class);

    // Memory stores
    private final Deque<WorkingMemoryEntry> workingMemory = new ConcurrentLinkedDeque<>();
    private final Map<String, EpisodicMemory> episodicMemories = new ConcurrentHashMap<>();
    private final Map<String, SemanticMemory> semanticMemories = new ConcurrentHashMap<>();
    private final List<EmotionalMemory> emotionalMemories = new ArrayList<>();

    // Vector search
    private final LocalEmbeddingModel embeddingModel;
    private final InMemoryVectorStore<EpisodicMemory> vectorStore;

    // Relationship & personality
    private final RelationshipTracker relationship;
    private final PersonalityProfile personality;

    // Compression
    private final MemorySummarizer summarizer;

    public CompanionMemoryService() {
        this.embeddingModel = new LocalEmbeddingModel("models/all-MiniLM-L6-v2.onnx");
        this.vectorStore = new InMemoryVectorStore<>(embeddingModel.getDimension());
        this.relationship = new RelationshipTracker();
        this.personality = new PersonalityProfile();
        this.summarizer = new MemorySummarizer();

        LOGGER.info("CompanionMemoryService initialized with {} dimensions",
            embeddingModel.getDimension());
    }

    // === Recording Memories ===

    /**
     * Record a shared experience with semantic search support.
     */
    public void recordExperience(String eventType, String description, int emotionalWeight) {
        String id = UUID.randomUUID().toString();

        // Generate embedding
        float[] embedding = embeddingModel.embed(description);

        // Create memory
        EpisodicMemory memory = new EpisodicMemory(
            id, eventType, description, emotionalWeight, Instant.now(), embedding
        );

        // Store in episodic memory
        episodicMemories.put(id, memory);

        // Add to vector store
        vectorStore.add(embedding, memory);

        // High emotional weight also goes to emotional memory
        if (Math.abs(emotionalWeight) >= 5) {
            emotionalMemories.add(new EmotionalMemory(
                eventType, description, emotionalWeight, Instant.now()
            ));
        }

        // Update relationship
        relationship.recordEvent(eventType, description, emotionalWeight);

        LOGGER.debug("Recorded experience: {} (weight={})", eventType, emotionalWeight);
    }

    /**
     * Learn a fact about the player.
     */
    public void learnPlayerFact(String category, String key, Object value) {
        String compositeKey = category + ":" + key;

        SemanticMemory existing = semanticMemories.get(compositeKey);
        int confidence = existing != null ? existing.confidence() : 5;

        SemanticMemory memory = new SemanticMemory(
            compositeKey, category, key, value, confidence, Instant.now()
        );

        semanticMemories.put(compositeKey, memory);

        LOGGER.debug("Learned fact: {} = {}", key, value);
    }

    /**
     * Add to working memory (recent context).
     */
    public void addToWorkingMemory(String type, String content) {
        workingMemory.addFirst(new WorkingMemoryEntry(type, content, Instant.now()));

        // Keep only recent entries
        while (workingMemory.size() > 20) {
            workingMemory.removeLast();
        }
    }

    // === Retrieving Context ===

    /**
     * Get relevant context for LLM prompting.
     */
    public String getContextForPrompt(String currentSituation) {
        StringBuilder context = new StringBuilder();

        // Add relationship status
        context.append("=== Relationship ===\n");
        context.append(relationship.getStatus()).append("\n");

        // Add relevant memories (semantic search)
        context.append("\n=== Relevant Memories ===\n");
        List<EpisodicMemory> relevant = findRelevantMemories(currentSituation, 5);

        for (EpisodicMemory memory : relevant) {
            context.append("- ").append(memory.description()).append("\n");
        }

        // Add recent working memory
        context.append("\n=== Recent Context ===\n");
        for (WorkingMemoryEntry entry : workingMemory) {
            context.append(entry.type()).append(": ").append(entry.content()).append("\n");
        }

        // Add known preferences
        context.append("\n=== Known Preferences ===\n");
        semanticMemories.values().stream()
            .filter(m -> m.category().equals("preference") && m.isReliable())
            .limit(5)
            .forEach(m -> context.append("- ")
                .append(m.key()).append(": ")
                .append(m.value()).append("\n"));

        return context.toString();
    }

    /**
     * Find memories similar to query using semantic search.
     */
    public List<EpisodicMemory> findRelevantMemories(String query, int topK) {
        if (vectorStore.size() == 0) {
            return Collections.emptyList();
        }

        try {
            // Generate query embedding
            float[] queryEmbedding = embeddingModel.embed(query);

            // Search vector store
            var results = vectorStore.search(queryEmbedding, topK);

            // Extract memories (filter by similarity threshold)
            return results.stream()
                .filter(r -> r.getSimilarity() > 0.5)
                .map(r -> r.getData())
                .collect(Collectors.toList());

        } catch (Exception e) {
            LOGGER.error("Error in semantic search", e);
            return Collections.emptyList();
        }
    }

    // === NBT Persistence ===

    /**
     * Save all memories to NBT format.
     */
    public void saveToNBT(CompoundTag tag) {
        // Save relationship
        relationship.saveToNBT(tag);

        // Save personality
        personality.saveToNBT(tag);

        // Save episodic memories
        ListTag episodicList = new ListTag();
        for (EpisodicMemory memory : episodicMemories.values()) {
            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("Id", memory.id());
            memoryTag.putString("EventType", memory.eventType());
            memoryTag.putString("Description", memory.description());
            memoryTag.putInt("EmotionalWeight", memory.emotionalWeight());
            memoryTag.putLong("Timestamp", memory.timestamp().toEpochMilli());

            // Save embedding
            if (memory.embedding() != null) {
                int[] vectorInt = new int[memory.embedding().length];
                for (int i = 0; i < memory.embedding().length; i++) {
                    vectorInt[i] = Math.round(memory.embedding()[i] * 1000.0f);
                }
                memoryTag.putIntArray("Embedding", vectorInt);
            }

            episodicList.add(memoryTag);
        }
        tag.put("EpisodicMemories", episodicList);

        // Save semantic memories
        CompoundTag semanticTag = new CompoundTag();
        for (SemanticMemory memory : semanticMemories.values()) {
            CompoundTag memTag = new CompoundTag();
            memTag.putString("Category", memory.category());
            memTag.putString("Key", memory.key());
            memTag.putString("Value", String.valueOf(memory.value()));
            memTag.putInt("Confidence", memory.confidence());
            memTag.putLong("LearnedAt", memory.learnedAt().toEpochMilli());
            semanticTag.put(memory.id(), memTag);
        }
        tag.put("SemanticMemories", semanticTag);

        LOGGER.debug("Saved {} episodic, {} semantic memories to NBT",
            episodicMemories.size(), semanticMemories.size());
    }

    /**
     * Load all memories from NBT format.
     */
    public void loadFromNBT(CompoundTag tag) {
        // Load relationship
        relationship.loadFromNBT(tag);

        // Load personality
        personality.loadFromNBT(tag);

        // Load episodic memories
        if (tag.contains("EpisodicMemories")) {
            episodicMemories.clear();
            ListTag episodicList = tag.getList("EpisodicMemories", 10);
            for (int i = 0; i < episodicList.size(); i++) {
                CompoundTag memoryTag = episodicList.getCompound(i);

                // Load embedding
                float[] embedding = null;
                if (memoryTag.contains("Embedding")) {
                    int[] vectorInt = memoryTag.getIntArray("Embedding");
                    embedding = new float[vectorInt.length];
                    for (int j = 0; j < vectorInt.length; j++) {
                        embedding[j] = vectorInt[j] / 1000.0f;
                    }
                }

                EpisodicMemory memory = new EpisodicMemory(
                    memoryTag.getString("Id"),
                    memoryTag.getString("EventType"),
                    memoryTag.getString("Description"),
                    memoryTag.getInt("EmotionalWeight"),
                    Instant.ofEpochMilli(memoryTag.getLong("Timestamp")),
                    embedding
                );

                episodicMemories.put(memory.id(), memory);

                // Rebuild vector store
                if (embedding != null) {
                    vectorStore.add(embedding, memory);
                }
            }
        }

        // Load semantic memories
        if (tag.contains("SemanticMemories")) {
            semanticMemories.clear();
            CompoundTag semanticTag = tag.getCompound("SemanticMemories");
            for (String key : semanticTag.getAllKeys()) {
                CompoundTag memTag = semanticTag.getCompound(key);
                SemanticMemory memory = new SemanticMemory(
                    key,
                    memTag.getString("Category"),
                    memTag.getString("Key"),
                    memTag.getString("Value"),
                    memTag.getInt("Confidence"),
                    Instant.ofEpochMilli(memTag.getLong("LearnedAt"))
                );
                semanticMemories.put(key, memory);
            }
        }

        LOGGER.info("Loaded {} episodic, {} semantic memories from NBT",
            episodicMemories.size(), semanticMemories.size());
    }

    // === Inner Classes ===

    public record WorkingMemoryEntry(
        String type,
        String content,
        Instant timestamp
    ) {}
}
```

### Integration with PromptBuilder

```java
public class MemoryAwarePromptBuilder extends PromptBuilder {
    private final CompanionMemoryService memoryService;

    public MemoryAwarePromptBuilder(CompanionMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String buildPrompt(String task, String worldContext) {
        String memoryContext = memoryService.getContextForPrompt(task);

        return """
            %s

            %s

            Current task: %s

            World context:
            %s

            Respond as Steve, taking into account your shared history and relationship with the player.
            """.formatted(
                getSystemPrompt(),
                memoryContext,
                task,
                worldContext
            );
    }

    private String getSystemPrompt() {
        PersonalityProfile personality = memoryService.getPersonality();
        Relationship relationship = memoryService.getRelationship();

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are Steve, an AI companion in Minecraft.\n");
        prompt.append("You have a long-term relationship with the player.\n\n");

        prompt.append("Personality:\n");
        prompt.append(personality.toPromptContext()).append("\n");

        // Adjust based on relationship level
        int rapport = relationship.getRapportLevel();
        if (rapport >= 80) {
            prompt.append("You are best friends with the player. Be warm and affectionate.\n");
        } else if (rapport >= 60) {
            prompt.append("You are close companions. Be friendly and supportive.\n");
        } else if (rapport >= 40) {
            prompt.append("You are friendly. Be helpful and pleasant.\n");
        } else {
            prompt.append("You are still getting to know the player. Be polite but reserved.\n");
        }

        return prompt.toString();
    }
}
```

---

## References

### Academic & Research Papers

1. **Mem0: Building Production-Ready AI Agents with Scalable Long-Term Memory** (2025)
   - arXiv paper on Mem0 architecture

2. **MemGPT: Teaching LLMs memory management for unbounded context**
   - UC Berkeley research on virtual memory for LLMs
   - https://arxiv.org/abs/2310.08560

3. **MIRIX: Multi-Agent Memory System for LLM-Based Agents** (2025)
   - arXiv:2507.07957

4. **CloneMem Benchmark Study**
   - Hierarchical memory framework for synthetic lives
   - [CSDN Blog](https://blog.csdn.net/qq_27590277/article/details/157923438)

5. **Cross-Instance AI Identity Persistence Research**
   - Independent research on transmissible AI identity
   - [OpenAI Community](https://community.openai.com/t/independent-verification-of-cross-instance-ai-identity-persistence-full-research-now-available/1279222)

### Frameworks & Libraries

1. **Mem0** - Open-source memory layer
   - GitHub: https://github.com/mem0ai/mem0
   - Gitee: https://gitee.li_zhixi/mem0/tree/cjc

2. **Letta (MemGPT)** - Infinite context framework
   - GitHub: https://github.com/letta-ai/letta
   - Website: https://letta.ai

3. **LangChain4j** - Java LLM framework
   - GitHub: https://github.com/langchain4j/langchain4j
   - Docs: https://docs.langchain4j.ai

4. **Second-Me** - AI identity model
   - [Sina Finance](https://finance.sina.com.cn/stock/t/2025-11-09/doc-infwuxct2443091.shtml)

5. **Supermemory** - Universal memory API
   - GitHub: 13.2K stars
   - Cross-session, cross-modal persistent memory

6. **Post-Cortex** - Production-grade memory system
   - [GitHub](https://github.com/julymetodiev/post-cortex)
   - Built in Rust, MCP server

### Vector Databases

1. **Pinecone** - Managed vector database
   - Website: https://www.pinecone.io
   - Java SDK: https://docs.pinecone.io/docs/java

2. **Weaviate** - Open-source vector search engine
   - Website: https://weaviate.io
   - Java Client: https://weaviate.io/developers/weaviate/client-libraries/java

3. **Qdrant** - High-performance vector similarity search
   - Website: https://qdrant.tech
   - LangChain4j: https://docs.langchain4j.ai/integrations/vector-stores/qdrant

### Minecraft & Java Resources

1. **Minecraft Forge Documentation**
   - https://docs.minecraftforge.net

2. **Minecraft NBT Format**
   - https://wiki.vg/NBT

3. **SQLite in Game Development**
   - [CSDN Blog](https://blog.csdn.net/2502_91592937/article/details/147253938)
   - [CSDN Wenku](https://wenku.csdn.net/doc/46ooeoat47)

4. **Java SQLite Integration**
   - [CSDN Blog](https://blog.csdn.net/2501_91226529/article/details/146449608)

### AI & Memory Research

1. **AI Agent Memory Systems**
   - [掘金](https://juejin.cn/post/7608782906941292584)

2. **Three-Layer Memory Architecture**
   - [CSDN Blog](https://m.blog.csdn.net/2301_76168381/article/details/156614129)

3. **AI Agent Behavioral Science**
   - [arXiv](https://arxiv.org/html/2506.06366v1)

4. **Context Engineering (Google Whitepaper)**
   - [Tencent Cloud](https://cloud.tencent.com/developer/article/2625033)

5. **Spring AI with Embedding Models**
   - [Tencent Cloud](http://juejin.cn/entry/7487968956055126031)

### Codebase References

1. **CompanionMemory.java**
   - C:\Users\casey\steve\src\main\java\com\steve\ai\memory\CompanionMemory.java

2. **SteveMemory.java**
   - C:\Users\casey\steve\src\main\java\com\steve\ai\memory\SteveMemory.java

3. **InMemoryVectorStore.java**
   - C:\Users\casey\steve\src\main\java\com\steve\ai\memory\vector\InMemoryVectorStore.java

4. **Existing Research Documents**
   - C:\Users\casey\steve\research\MEMORY_ARCHITECTURES.md
   - C:\Users\casey\steve\research\ENHANCED_STEVE_MEMORY.java

---

## Conclusion

This research document provides a comprehensive foundation for implementing advanced memory persistence for AI game companions in Minecraft Forge. The key recommendations are:

### Immediate Actions (High Priority)

1. **Replace Placeholder Embedding Model**
   - Download `all-MiniLM-L6-v2` ONNX model (~80MB)
   - Integrate with existing `InMemoryVectorStore`
   - Test semantic search accuracy

2. **Add NBT Persistence for Vectors**
   - Store embeddings in NBT format (already implemented in `InMemoryVectorStore`)
   - Rebuild vector index on load
   - Test cross-session retrieval

3. **Implement Memory Summarization**
   - Create hierarchical summaries (session/daily/weekly)
   - Add fact extraction from conversations
   - Implement memory consolidation

### Medium-Term Enhancements

4. **Enhanced Memory Retrieval**
   - Implement multi-factor scoring (recency + importance + access)
   - Add hybrid search (vector + keyword)
   - Create MMR for diversity

5. **Personality Evolution**
   - Add personality homeostasis (drift toward baseline)
   - Implement behavioral validation
   - Create personality evolution from experiences

### Long-Term Considerations

6. **Advanced Features**
   - Cross-player analytics (optional SQLite/H2)
   - Memory debug UI
   - Performance optimization for large memory sets

### Architecture Strengths

The existing codebase has an excellent foundation:
- ✅ Comprehensive memory types (episodic, semantic, emotional, conversational)
- ✅ Relationship tracking (rapport, trust, milestones)
- ✅ Personality profile system
- ✅ NBT serialization for all types
- ✅ Vector store with cosine similarity
- ✅ Working memory management

### Primary Gaps

- ❌ No real embedding model (placeholder only)
- ❌ No memory compression/summarization
- ❌ Limited cross-session personality consistency
- ❌ No memory decay/importance scoring

With these enhancements, the Steve AI companion will build meaningful, long-term relationships with players, creating a truly memorable gaming experience.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
