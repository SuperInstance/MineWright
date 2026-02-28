# Memory Architectures for AI Companions
## Research Report: Long-term Relationship Building for MineWright

**Date:** 2026-02-26
**Research Focus:** Memory systems for AI agents building long-term relationships with users
**Target Environment:** Java 17, Minecraft Forge 1.20.1

---

## Executive Summary

This research document provides a comprehensive analysis of modern memory architectures for AI companions, with specific recommendations for implementing a sophisticated memory system for the MineWright mod. The research covers leading memory frameworks (Mem0, Letta/MemGPT, LangChain), vector database options, retrieval strategies, and practical Java implementations suitable for a Minecraft mod environment.

**Key Finding:** The most effective approach for MineWright is a **hybrid multi-tier memory architecture** combining:
- **Working Memory** (in-memory, recent context)
- **Vector-based Semantic Search** (for relevant memory retrieval)
- **Relationship Tracking** (rapport, trust, emotional memories)
- **Persistent Storage** (file-based for Minecraft compatibility)

The existing `CompanionMemory` class provides an excellent foundation but requires enhancement with vector embeddings for semantic search and improved persistence mechanisms.

---

## Table of Contents

1. [Memory System Comparison](#1-memory-system-comparison)
2. [Types of Memory](#2-types-of-memory)
3. [Memory Storage Options](#3-memory-storage-options)
4. [Memory Retrieval Strategies](#4-memory-retrieval-strategies)
5. [Memory Summarization Techniques](#5-memory-summarization-techniques)
6. [Relationship Tracking](#6-relationship-tracking)
7. [Java Implementation Options](#7-java-implementation-options)
8. [Recommended Architecture for MineWright AI](#8-recommended-architecture-for-minewright-ai)
9. [Data Models](#9-data-models)
10. [Implementation Roadmap](#10-implementation-roadmap)
11. [Code Examples](#11-code-examples)
12. [References](#12-references)

---

## 1. Memory System Comparison

### 1.1 Modern Memory Frameworks

| Framework | Type | Strengths | Weaknesses | Java Support | Maturity |
|-----------|------|-----------|------------|--------------|----------|
| **Mem0** | Memory Layer Service | Production-ready, auto-extraction, auto-cleaning | Black-box, requires API/service | REST API | ★★★★★ |
| **Letta (MemGPT)** | Infinite Context OS | Virtual memory management, self-managed | Complex setup, Python-centric | REST API | ★★★★☆ |
| **LangChain/LangMem** | SDK for Memory | Semantic + Episodic + Procedural, flexible | Requires integration | LangChain4j | ★★★★★ |
| **AutoGen** | Multi-Agent Framework | Conversation memory, agent coordination | Python-only, complex config | Indirect | ★★★★☆ |

### 1.2 Mem0: AI Memory Layer

**Overview:** Mem0 is an open-source memory layer providing persistent contextual memory for AI systems. It enables intelligent agents to remember information across sessions through automated extraction, cleaning, and storage.

**Key Features:**
- **Automatic Memory Pipeline:** Extract → Clean → Store
- **OpenMemory MCP Architecture:** Launched in 2025 with 30,000+ GitHub stars
- **Cross-Session Persistence:** Memories survive beyond single conversations
- **Semantic Search:** Retrieves relevant memories when needed
- **Multi-User Support:** Can track memories for multiple users

**Architecture:**
```
Input Conversation
       ↓
[Automatic Extraction] - Identify key information
       ↓
[Automatic Cleaning] - Filter and process
       ↓
[Automatic Storage] - Vector DB + Metadata
       ↓
[Automatic Retrieval] - Context-aware search
```

**For MineWright:** Mem0's approach is inspirational but requires external service. The patterns can be replicated locally.

### 1.3 Letta (formerly MemGPT): Infinite Context

**Overview:** Developed at UC Berkeley with $10M funding, Letta treats LLMs as operating systems with virtual memory management.

**Key Innovation: Two-Tier Memory System**

| Memory Type | Analogy | Capacity | Access Speed |
|-------------|---------|----------|--------------|
| **Main Context** | RAM | Limited (4K-128K tokens) | Instant |
| **External Context** | Hard Drive | Unlimited | Search-based |

**How It Works:**
1. **Queue Manager:** User input enters FIFO queue
2. **Memory Swap:** Old messages moved to Recall Storage when context fills
3. **LLM Self-Management:** LLM uses special function calls to manage memory
   - `archival_memory_insert()` - Store long-term memories
   - `archival_memory_search()` - Retrieve relevant information
   - `update_memory()` - Modify stored information

**For MineWright:** The two-tier concept is directly applicable to Minecraft's limited context windows.

### 1.4 LangChain Memory Systems (LangMem)

**Overview:** LangChain launched LangMem in 2025, an SDK focused on agent memory with three memory types.

**Three Memory Types:**
1. **Semantic Memory:** Facts and knowledge (player preferences, world knowledge)
2. **Episodic Memory:** Past experiences and interactions (shared events)
3. **Procedural Memory:** Learned skills and patterns (building techniques)

**Memory Architecture Layers:**

| Layer | Type | Storage | Purpose | Example |
|-------|------|---------|---------|---------|
| **Short-term/Working** | Current session | In-context | Temporary, fast | "We're building a tower" |
| **User Profile** | Static preferences | Persistent store | Personalization | "Player likes cobblestone" |
| **Semantic** | Facts | Vector indices | "What do I know?" | "Player uses diamond tools" |
| **Episodic** | Events | Vector DB | "What happened?" | "We survived a creeper attack" |

**For MineWright AI:** LangMem's three-type model directly maps to the existing `CompanionMemory` structure.

---

## 2. Types of Memory

### 2.1 Episodic Memory

**Definition:** Specific events and experiences with temporal context.

**Structure:**
```java
class EpisodicMemory {
    String eventType;        // "build", "combat", "explore"
    String description;      // What happened
    Instant timestamp;       // When it happened
    int emotionalWeight;     // -10 to +10 significance
    String participants;     // Who was involved
    Location location;       // Where it happened
}
```

**Examples for MineWright AI:**
- "Built a house together at coordinates (100, 64, -200)"
- "Defeated first Ender Dragon together"
- "Player got lost in caves, MineWright led them out"
- "Accidentally flooded the house - laughed about it"

**Storage Strategy:** Vector database with timestamp metadata for time-decay relevance.

### 2.2 Semantic Memory

**Definition:** Facts and knowledge about the player and world.

**Structure:**
```java
class SemanticMemory {
    String category;         // "preference", "skill", "habit"
    String key;              // "favorite_block", "playstyle"
    Object value;            // "cobblestone", "aggressive"
    Instant learnedAt;       // When learned
    int confidence;          // 1-10 reliability score
    int accessCount;         // How often referenced
}
```

**Examples for MineWright AI:**
- "Player prefers stone tools over iron"
- "Player hates gravel scaffolding"
- "Player is an aggressive fighter"
- "Player likes to mine at Y=-58"

**Storage Strategy:** Key-value store with confidence scoring and reinforcement learning.

### 2.3 Working Memory

**Definition:** Current context and recent interactions (short-term).

**Structure:**
```java
class WorkingMemoryEntry {
    String type;             // "conversation", "action", "observation"
    String content;          // The actual content
    Instant timestamp;       // When recorded
    int relevance;           // Current relevance score
}
```

**Examples for MineWright AI:**
- "Player asked MineWright to gather wood"
- "Currently building a tower"
- "Player mentioned they're low on food"

**Storage Strategy:** In-memory circular buffer (max 20 entries).

### 2.4 Procedural Memory

**Definition:** Learned skills, patterns, and preferences for execution.

**Structure:**
```java
class ProceduralMemory {
    String skill;            // "building", "mining", "combat"
    String pattern;          // The learned pattern
    double effectiveness;    // Success rate
    Instant lastUsed;        // When last applied
}
```

**Examples for MineWright AI:**
- "Player likes 3-block wide hallways"
- "Always place torches every 8 blocks"
- "Player prefers spiral staircases"
- "Build roofs with stairs, not slabs"

**Storage Strategy:** HashMap with effectiveness scoring and periodic validation.

### 2.5 Emotional Memory

**Definition:** High-impact moments with emotional significance.

**Structure:**
```java
class EmotionalMemory {
    String eventType;        // "success", "failure", "discovery"
    String description;      // What happened
    int emotionalWeight;     // -10 to +10 intensity
    Instant timestamp;       // When it happened
    String mood;             // "joyful", "frustrated", "proud"
}
```

**Examples for MineWright AI:**
- "First time defeating the Wither together (weight: +10)"
- "Player saved MineWright from lava (weight: +9)"
- "House burned down - sad moment (weight: -7)"
- "Found diamonds together (weight: +8)"

**Storage Strategy:** Sorted list by emotional weight, limited to top 50.

### 2.6 Conversational Memory

**Definition:** Topics discussed, inside jokes, catchphrases, and shared references.

**Structure:**
```java
class ConversationalMemory {
    List<InsideJoke> insideJokes;
    Set<String> discussedTopics;
    Map<String, Integer> phraseUsage;
    Map<String, Instant> topicLastMentioned;
}

class InsideJoke {
    String context;          // Origin situation
    String punchline;        // The memorable phrase
    Instant createdAt;       // When created
    int referenceCount;      // How often referenced
}
```

**Examples for MineWright AI:**
- "Remember when you called gravel 'gravity blocks'?"
- "MineWright's catchphrase: 'Another day, another block!'"
- "Inside joke: 'Creeper therapy sessions'"

**Storage Strategy:** List with reference counting, least-referenced eviction.

---

## 3. Memory Storage Options

### 3.1 Vector Databases

#### 3.1.1 Comparison Matrix

| Database | Type | Java Client | Deployment | Best For | Complexity |
|----------|------|-------------|------------|----------|------------|
| **Pinecone** | Managed SaaS | Official SDK | Cloud | Enterprise, no infra | Low |
| **Weaviate** | Open-source | io.weaviate client | Self-hosted/Cloud | Hybrid search | Medium |
| **Qdrant** | Rust-based | langchain4j-qdrant | Self-hosted | Performance, metadata | Medium |
| **Chroma** | Python-native | HTTP API | Embedded/local | Simple RAG | Low |
| **Milvus** | Open-source | Java SDK/gRPC | Self-hosted | Large-scale | High |

#### 3.1.2 Pinecone

**Pros:**
- Fully managed, no infrastructure overhead
- Excellent Java SDK (v5.0.0)
- Automatic sharding and scaling
- Built-in embedding model support
- Spring Boot integration

**Cons:**
- Cost for production usage
- Network dependency
- Data stored externally

**Java Integration:**
```java
// Maven dependency
implementation 'io.pinecone:pinecone-client:5.0.0'

// Usage
PineconeClient client = new PineconeClientBuilder()
    .withApiKey(apiKey)
    .build();

Index<Record> index = client.getIndex("minewright-memories", "us-east-1-aws");

// Store memory
float[] embedding = embeddingModel.embed(message);
index.upsert(
    Index.upsertRequest()
        .vectors(Map.of(
            memoryId,
            VectorRecord.builder()
                .values(embedding)
                .metadata(Map.of(
                    "type", "episodic",
                    "timestamp", Instant.now().toString(),
                    "emotionalWeight", 7
                ))
                .build()
        ))
);
```

**Verdict for MineWright AI:** Overkill for single-player, good for multiplayer servers.

#### 3.1.3 Weaviate

**Pros:**
- Open-source and self-hostable
- GraphQL + REST API
- Hybrid search (keyword + vector)
- Knowledge graph integration
- Good Java client (v4.5.0+)

**Cons:**
- Requires separate service
- More complex setup
- Additional resource usage

**Java Integration:**
```xml
<dependency>
    <groupId>io.weaviate</groupId>
    <artifactId>client</artifactId>
    <version>4.5.0</version>
</dependency>
```

```java
// Initialize
WeaviateClient client = new WeaviateClient({
    "scheme": "http",
    "host": "localhost:8080"
});

// Store memory with embedding
Memory memory = new Memory()
    .setProperties(Map.of(
        "content", "We built a tower together",
        "type", "episodic",
        "timestamp", Instant.now(),
        "emotionalWeight", 7
    ));

client.data().creator()
    .withClassName("Memory")
    .withObject(memory)
    .run();
```

**Verdict for MineWright AI:** Good for multiplayer, but heavy for single-player.

#### 3.1.4 Qdrant

**Pros:**
- High performance (Rust-based)
- REST/gRPC interfaces
- Excellent metadata filtering
- LangChain4j integration
- Local deployment possible

**Cons:**
- Separate service required
- Smaller community than Weaviate

**Java Integration (via LangChain4j):**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-qdrant</artifactId>
    <version>0.29.1</version>
</dependency>
```

```java
// Initialize
QdrantEmbeddingStore store = QdrantEmbeddingStore.builder()
    .host("localhost")
    .port(6334)
    .collectionName("minewright-memories")
    .build();

// Store memory
Embedding embedding = embeddingModel.embed(memoryText).content();
TextSegment segment = TextSegment.from(memoryText,
    Metadata.from("type", "episodic", "weight", "7"));

store.add(Embedding.builder()
    .id(memoryId)
    .embedding(embedding)
    .segment(segment)
    .build());
```

**Verdict for MineWright AI:** Strong contender for multiplayer scenarios.

### 3.2 In-Memory Vector Search

For single-player Minecraft mods, an in-memory vector store is often sufficient.

#### 3.2.1 Simple In-Memory Implementation

```java
public class InMemoryVectorStore {
    private final EmbeddingModel embeddingModel;
    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();
    private final Map<String, MemoryMetadata> metadata = new ConcurrentHashMap<>();

    public List<SearchResult> search(String query, int topK) {
        float[] queryVector = embeddingModel.embed(query);

        return vectors.entrySet().stream()
            .map(e -> new SearchResult(
                e.getKey(),
                cosineSimilarity(queryVector, e.getValue()),
                metadata.get(e.getKey())
            ))
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(topK)
            .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

**Pros:**
- No external dependencies
- Fast for small datasets (<10K memories)
- Simple to implement
- Works offline

**Cons:**
- Limited scalability
- Vectors lost on shutdown (unless persisted)
- Memory usage grows with data

**Verdict for MineWright AI:** **Recommended** for single-player mode with disk persistence.

### 3.3 Graph Databases (Neo4j)

For relationship-heavy memory systems, graph databases excel at tracking connections between memories.

#### 3.3.1 Neo4j Java Embedded

**Use Case:** Tracking complex relationships like "that time we built a tower BECAUSE you like heights".

**Maven:**
```xml
<dependency>
    <groupId>org.neo4j</groupId>
    <artifactId>neo4j</artifactId>
    <version>5.12.0</version>
</dependency>
```

**Implementation:**
```java
// Create embedded database
GraphDatabaseService graphDb = new GraphDatabaseFactory()
    .newEmbeddedDatabase(new File("minewright-memories"));

// Define relationship types
enum RelTypes implements RelationshipType {
    RELATED_TO, CAUSED_BY, LEAD_TO, SIMILAR_TO
}

// Store memory with relationships
try (Transaction tx = graphDb.beginTx()) {
    // Create memory nodes
    Node towerMemory = graphDb.createNode(Label.label("Memory"));
    towerMemory.setProperty("description", "Built a tower");
    towerMemory.setProperty("type", "episodic");

    Node heightPref = graphDb.createNode(Label.label("Preference"));
    heightPref.setProperty("key", "likes_heights");
    heightPref.setProperty("value", true);

    // Create relationship
    towerMemory.createRelationshipTo(heightPref, RelTypes.RELATED_TO);

    tx.success();
}
```

**Query example:**
```java
// Find memories related to player preferences
try (Transaction tx = graphDb.beginTx()) {
    Result result = tx.execute(
        "MATCH (m:Memory)-[:RELATED_TO]->(p:Preference) " +
        "WHERE p.key = 'likes_heights' " +
        "RETURN m.description"
    );

    while (result.hasNext()) {
        Map<String, Object> row = result.next();
        System.out.println(row.get("m.description"));
    }
    tx.success();
}
```

**Verdict for MineWright AI:** Useful for advanced relationship tracking, but may be overkill for initial implementation.

### 3.4 File-Based Storage (Minecraft Native)

For maximum compatibility with Minecraft's ecosystem, file-based storage using NBT or JSON is ideal.

#### 3.4.1 NBT Storage (Minecraft Format)

```java
public class MemoryStore {
    private final Map<String, Memory> memories = new ConcurrentHashMap<>();
    private final Path savePath;

    public void saveToNBT(CompoundTag tag) {
        // Save episodic memories
        ListTag episodicList = new ListTag();
        for (Memory memory : episodicMemories.values()) {
            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("id", memory.id());
            memoryTag.putString("content", memory.content());
            memoryTag.putLong("timestamp", memory.timestamp().toEpochMilli());
            memoryTag.putInt("emotionalWeight", memory.emotionalWeight());
            episodicList.add(memoryTag);
        }
        tag.put("episodic_memories", episodicList);

        // Save semantic memories
        CompoundTag semanticTag = new CompoundTag();
        for (SemanticMemory memory : semanticMemories.values()) {
            semanticTag.putString(memory.key(), memory.value().toString());
            semanticTag.putInt(memory.key() + "_confidence", memory.confidence());
        }
        tag.put("semantic_memories", semanticTag);

        // Save vectors
        CompoundTag vectorsTag = new CompoundTag();
        for (Map.Entry<String, float[]> entry : vectors.entrySet()) {
            vectorsTag.putByteArray(entry.getKey(),
                floatArrayToByteArray(entry.getValue()));
        }
        tag.put("vectors", vectorsTag);
    }

    public void loadFromNBT(CompoundTag tag) {
        // Load episodic memories
        ListTag episodicList = tag.getList("episodic_memories", 10);
        for (Tag tag : episodicList) {
            CompoundTag memoryTag = (CompoundTag) tag;
            Memory memory = new Memory(
                memoryTag.getString("id"),
                memoryTag.getString("content"),
                Instant.ofEpochMilli(memoryTag.getLong("timestamp")),
                memoryTag.getInt("emotionalWeight")
            );
            memories.put(memory.id(), memory);
        }
    }
}
```

**Pros:**
- Native Minecraft format
- Automatically saved with world
- Works in single-player and multiplayer
- No external dependencies

**Cons:**
- NBT has size limitations
- Not as fast as binary formats
- Manual serialization required

**Verdict for MineWright AI:** **Highly Recommended** - Use NBT for persistence, in-memory for active use.

---

## 4. Memory Retrieval Strategies

### 4.1 Multi-Factor Scoring

Effective memory retrieval requires balancing multiple factors: relevance, recency, importance, and emotional salience.

#### 4.1.1 General Scoring Formula

From research on AI agent memory systems:

```
score = α × recency_score + β × relevance_score + γ × importance_score
```

Where:
- `α` (alpha) = Recency weight (typically 0.2-0.3)
- `β` (beta) = Relevance weight (typically 0.5-0.6)
- `γ` (gamma) = Importance weight (typically 0.2-0.3)

#### 4.1.2 Detailed Scoring Components

**Recency Score:**
```java
public double calculateRecencyScore(Instant memoryTime, Instant currentTime) {
    long daysSince = ChronoUnit.DAYS.between(memoryTime, currentTime);
    // Exponential decay with configurable half-life
    double decayFactor = Math.pow(0.9, daysSince); // 10% daily decay
    return decayFactor;
}
```

**Relevance Score (Semantic):**
```java
public double calculateRelevanceScore(String query, Memory memory) {
    // Use cosine similarity on embeddings
    float[] queryVector = embeddingModel.embed(query);
    float[] memoryVector = memory.getEmbedding();

    return cosineSimilarity(queryVector, memoryVector);
}
```

**Importance Score:**
```java
public double calculateImportanceScore(Memory memory) {
    // Based on emotional weight and reference count
    double emotionalFactor = Math.abs(memory.emotionalWeight()) / 10.0;
    double referenceFactor = Math.min(1.0, memory.referenceCount() / 10.0);

    return (emotionalFactor * 0.7) + (referenceFactor * 0.3);
}
```

#### 4.1.3 Complete Scoring Implementation

```java
public class MemoryRetriever {
    private static final double RECENCY_WEIGHT = 0.2;
    private static final double RELEVANCE_WEIGHT = 0.5;
    private static final double IMPORTANCE_WEIGHT = 0.3;

    public List<ScoredMemory> retrieveMemories(
        String query,
        int topK,
        Instant currentTime
    ) {
        float[] queryVector = embeddingModel.embed(query);

        return memories.values().stream()
            .map(memory -> {
                double recency = calculateRecencyScore(memory.timestamp(), currentTime);
                double relevance = cosineSimilarity(queryVector, memory.getEmbedding());
                double importance = calculateImportanceScore(memory);

                double finalScore =
                    (recency * RECENCY_WEIGHT) +
                    (relevance * RELEVANCE_WEIGHT) +
                    (importance * IMPORTANCE_WEIGHT);

                return new ScoredMemory(memory, finalScore);
            })
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(topK)
            .toList();
    }

    public record ScoredMemory(Memory memory, double score) {}
}
```

### 4.2 Hybrid Search: Semantic + Keyword

Combining vector search with keyword matching improves recall and handles edge cases.

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

### 4.3 Context-Aware Retrieval

Different contexts require different retrieval strategies.

```java
public class ContextAwareRetriever {
    public List<Memory> retrieve(String query, String context) {
        return switch (context) {
            case "building" -> retrieveForBuilding(query);
            case "conversation" -> retrieveForConversation(query);
            case "emotional_support" -> retrieveEmotional(query);
            case "planning" -> retrieveForPlanning(query);
            default -> generalRetrieve(query);
        };
    }

    private List<Memory> retrieveForBuilding(String query) {
        // Prioritize procedural and semantic memories about building
        return memories.stream()
            .filter(m -> m.type().equals("procedural") ||
                        m.type().equals("semantic"))
            .filter(m -> m.category().equals("building"))
            .sorted(comparingRelevance(query))
            .limit(5)
            .toList();
    }

    private List<Memory> retrieveForConversation(String query) {
        // Prioritize episodic and conversational memories
        return memories.stream()
            .filter(m -> m.type().equals("episodic") ||
                        m.type().equals("conversational"))
            .sorted(composingRelevanceAndRecency(query))
            .limit(5)
            .toList();
    }

    private List<Memory> retrieveEmotional(String query) {
        // Prioritize emotional memories with high weight
        return memories.stream()
            .filter(m -> m.type().equals("emotional"))
            .filter(m -> Math.abs(m.emotionalWeight()) >= 5)
            .sorted(composingEmotionalRelevance(query))
            .limit(3)
            .toList();
    }
}
```

### 4.4 Temporal Retrieval Patterns

Sometimes you need memories from specific time periods.

```java
public class TemporalRetriever {
    public List<Memory> getRecentMemories(Duration period) {
        Instant cutoff = Instant.now().minus(period);
        return memories.values().stream()
            .filter(m -> m.timestamp().isAfter(cutoff))
            .sorted(Comparator.comparing(Memory::timestamp).reversed())
            .toList();
    }

    public List<Memory> getMemoriesFromPeriod(Instant start, Instant end) {
        return memories.values().stream()
            .filter(m -> !m.timestamp().isBefore(start))
            .filter(m -> !m.timestamp().isAfter(end))
            .toList();
    }

    public List<Memory> getAnniversaryMemories() {
        // Get memories from same day in previous years
        LocalDate today = LocalDate.now();
        return memories.values().stream()
            .filter(m -> {
                LocalDate memoryDate = m.timestamp()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                return memoryDate.getMonth() == today.getMonth() &&
                       memoryDate.getDayOfMonth() == today.getDayOfMonth() &&
                       memoryDate.getYear() < today.getYear();
            })
            .toList();
    }
}
```

### 4.5 MMR (Maximal Marginal Relevance)

Reduces redundancy in retrieved memories by promoting diversity.

```java
public class MMRRetriever {
    private static final double LAMBDA = 0.7; // Balance relevance vs diversity

    public List<Memory> retrieveWithMMR(String query, int topK) {
        List<Memory> selected = new ArrayList<>();
        Set<Memory> remaining = new HashSet<>(memories.values());

        float[] queryVector = embeddingModel.embed(query);

        for (int i = 0; i < topK && !remaining.isEmpty(); i++) {
            Memory best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Memory candidate : remaining) {
                // Relevance to query
                double relevance = cosineSimilarity(queryVector,
                    candidate.getEmbedding());

                // Minimum similarity to already selected
                double minSimilarity = selected.stream()
                    .mapToDouble(m -> cosineSimilarity(
                        candidate.getEmbedding(),
                        m.getEmbedding()))
                    .min()
                    .orElse(0);

                // MMR score: balance relevance and diversity
                double mmrScore = (LAMBDA * relevance) -
                    ((1 - LAMBDA) * minSimilarity);

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    best = candidate;
                }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(best);
            }
        }

        return selected;
    }
}
```

---

## 5. Memory Summarization Techniques

### 5.1 When to Summarize

Memory summarization is crucial for:
- **Long conversations** that exceed context windows
- **Old episodic memories** that lose detail relevance
- **Maintaining narrative coherence** across sessions
- **Extracting key facts** from ongoing interactions

**Triggers for Summarization:**
1. Conversation exceeds token threshold
2. Time period passes (e.g., end of session)
3. Emotional significance detected
4. Topic shift occurs

### 5.2 Summarization Strategies

#### 5.2.1 Extractive Summarization

Selects the most important sentences from the original text.

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

        // Position in conversation (earlier = more context)
        score += 0.1;

        // Length (prefer medium-length sentences)
        int length = sentence.split(" ").length;
        if (length >= 5 && length <= 20) score += 0.2;

        return score;
    }
}
```

#### 5.2.2 Abstractive Summarization (LLM-Based)

Uses the LLM to generate new summaries that capture the essence.

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
        prompt.append("and MineWright (AI companion). Extract:\n");
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

    private void extractKeyFacts(String summary) {
        // Parse summary and store as semantic memories
        // Example: "Player likes building with cobblestone"
        // → Store as semantic memory with category "preference"
    }
}
```

#### 5.2.3 Hierarchical Summarization

Maintains summaries at multiple time granularities.

```java
public class HierarchicalSummarizer {
    private Summary sessionSummary;    // Current play session
    private Summary dailySummary;      // Last 24 hours
    private Summary weeklySummary;     // Last 7 days
    private Summary monthlySummary;    // Last 30 days
    private Summary lifetimeSummary;   // All-time highlights

    public void addConversation(List<ConversationTurn> conversation) {
        // Update session summary
        sessionSummary = updateSummary(sessionSummary, conversation);

        // Periodically roll up to higher levels
        if (shouldUpdateDaily()) {
            dailySummary = rollUpSummary(List.of(sessionSummary, dailySummary));
        }
    }

    private Summary rollUpSummary(List<Summary> summaries) {
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

### 5.3 Fact Extraction

Extracting structured facts from unstructured conversations.

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

    private List<ExtractedFact> parseFactsFromJson(String json) {
        // Parse JSON and convert to ExtractedFact objects
        // Store in appropriate memory stores
    }
}
```

### 5.4 Maintaining Narrative Coherence

Ensuring the AI's responses remain consistent with past interactions.

```java
public class NarrativeCoherenceChecker {
    public boolean checkCoherence(String proposedResponse, List<Memory> context) {
        // Check against semantic memories
        for (Memory memory : context) {
            if (contradicts(proposedResponse, memory)) {
                LOGGER.warn("Response contradicts memory: {}", memory);
                return false;
            }
        }

        // Check against established personality
        if (!consistentWithPersonality(proposedResponse)) {
            LOGGER.warn("Response inconsistent with personality");
            return false;
        }

        return true;
    }

    private boolean contradicts(String response, Memory memory) {
        // Use LLM to check for contradictions
        String prompt = """
            Does this response contradict this memory?
            Response: %s
            Memory: %s
            Answer yes or no.
            """.formatted(response, memory.content());

        String answer = llmClient.complete(prompt);
        return answer.toLowerCase().contains("yes");
    }
}
```

---

## 6. Relationship Tracking

### 6.1 Measuring Rapport

Rapport is the overall warmth and connection in the relationship.

```java
public class RapportTracker {
    private int rapportLevel = 10;  // 0-100
    private final List<RapportEvent> history = new ArrayList<>();

    public void recordInteraction(InteractionType type, boolean positive) {
        int delta = switch (type) {
            case SHARED_SUCCESS -> +5;
            case SHARED_FAILURE -> +1;  // Bonding through adversity
            case INSIDE_JOKE -> +3;
            case PERSONAL_REVELATION -> +2;
            case HELP_RECEIVED -> +2;
            case HELP_GIVEN -> +1;
            case CONFLICT -> -3;
            case BETRAYAL -> -10;
        };

        if (!positive && type != InteractionType.SHARED_FAILURE) {
            delta = -delta / 2;  // Reduced penalty for negative interactions
        }

        adjustRapport(delta);
        history.add(new RapportEvent(type, delta, Instant.now()));
    }

    public void adjustRapport(int delta) {
        rapportLevel = Math.max(0, Math.min(100, rapportLevel + delta));
        LOGGER.info("Rapport adjusted by {}: now at {}", delta, rapportLevel);
    }

    public String getRapportDescription() {
        return switch (rapportLevel) {
            case int r when r >= 80 -> "Best friends";
            case int r when r >= 60 -> "Close companions";
            case int r when r >= 40 -> "Friendly";
            case int r when r >= 20 -> "Acquaintances";
            default -> "Strangers";
        };
    }
}
```

### 6.2 Tracking Trust

Trust is based on reliability and shared experiences.

```java
public class TrustTracker {
    private int trustLevel = 5;  // 0-100
    private final Map<String, Integer> trustByCategory = new HashMap<>();

    public void recordTrustedAction(String category, boolean reliable) {
        int delta = reliable ? +2 : -5;
        trustByCategory.merge(category, delta, Integer::sum);

        // Overall trust is weighted average
        double avgTrust = trustByCategory.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(50);

        trustLevel = (int) Math.round(avgTrust);
    }

    public boolean willTrust(String category) {
        // Check if MineWright trusts the player in this context
        int categoryTrust = trustByCategory.getOrDefault(category, 50);
        return categoryTrust >= 60;
    }

    public String getTrustDescription() {
        return switch (trustLevel) {
            case int t when t >= 80 -> "Complete trust";
            case int t when t >= 60 -> "High trust";
            case int t when t >= 40 -> "Moderate trust";
            case int t when t >= 20 -> "Cautious";
            default -> "Distrustful";
        };
    }
}
```

### 6.3 Detecting Inside Jokes

Inside jokes emerge from repeated humorous references.

```java
public class InsideJokeDetector {
    private final Map<String, Integer> phraseUsage = new HashMap<>();
    private final Map<String, Instant> lastUsed = new HashMap<>();
    private final Set<String> insideJokes = ConcurrentHashMap.newKeySet();

    public void recordPhrase(String phrase, boolean laughter) {
        phraseUsage.merge(phrase, 1, Integer::sum);
        lastUsed.put(phrase, Instant.now());

        // If a phrase is used 3+ times with laughter, it's an inside joke
        if (laughter && phraseUsage.getOrDefault(phrase, 0) >= 3) {
            if (insideJokes.add(phrase)) {
                LOGGER.info("New inside joke detected: {}", phrase);
                // Reward rapport for inside joke creation
                rapportTracker.adjustRapport(5);
            }
        }
    }

    public Optional<String> getRelevantInsideJoke(String context) {
        // Find inside jokes related to current context
        return insideJokes.stream()
            .filter(joke -> isContextuallyRelevant(joke, context))
            .findFirst();
    }

    private boolean isContextuallyRelevant(String joke, String context) {
        // Simple keyword matching
        String[] jokeWords = joke.toLowerCase().split("\\s+");
        String[] contextWords = context.toLowerCase().split("\\s+");

        return Arrays.stream(jokeWords)
            .anyMatch(jw -> Arrays.stream(contextWords)
                .anyMatch(cw -> cw.contains(jw) || jw.contains(cw)));
    }
}
```

### 6.4 Player Preference Learning

Learning and adapting to player preferences over time.

```java
public class PreferenceLearner {
    private final Map<String, LearnedPreference> preferences = new ConcurrentHashMap<>();

    public void observe(Player player, String action, String result) {
        // Extract preferences from player behavior
        if (action.contains("build") && result.contains("cobblestone")) {
            recordPreference("building_material", "cobblestone", 1);
        }

        // Negative preferences (what player avoids)
        if (action.contains("break") && result.contains("gravel")) {
            recordPreference("disliked_block", "gravel", 1);
        }
    }

    public void recordPreference(String key, String value, int confidence) {
        LearnedPreference existing = preferences.get(key);

        if (existing != null && !existing.value().equals(value)) {
            // Preference changed - lower confidence
            confidence = Math.max(1, confidence - 2);
            LOGGER.info("Player preference changed: {} = {} (was {})",
                key, value, existing.value());
        }

        preferences.put(key,
            new LearnedPreference(key, value, confidence, Instant.now()));
    }

    public Optional<String> getPreference(String key) {
        LearnedPreference pref = preferences.get(key);
        if (pref == null || pref.confidence() < 3) {
            return Optional.empty();
        }
        return Optional.of(pref.value());
    }

    public record LearnedPreference(
        String key,
        String value,
        int confidence,
        Instant lastObserved
    ) {}
}
```

### 6.5 Emotional Synchronization

MineWright's mood adapts based on player's emotional state.

```java
public class EmotionalSynchronizer {
    private String currentMood = "neutral";
    private final Map<String, Instant> moodHistory = new HashMap<>();

    public void detectPlayerEmotion(String message) {
        // Use LLM to detect emotion
        String emotion = llmClient.complete("""
            Detect the emotion in this message.
            Options: happy, sad, frustrated, excited, worried, neutral
            Message: %s
            Emotion:
            """.formatted(message)).trim().toLowerCase();

        // Adjust MineWright's mood towards player's emotion
        adjustMood(emotion);

        LOGGER.debug("Player emotion: {}, MineWright's mood: {}", emotion, currentMood);
    }

    private void adjustMood(String playerEmotion) {
        // Gradually synchronize, not instant
        if (playerEmotion.equals("happy") && !currentMood.equals("cheerful")) {
            currentMood = "cheerful";
        } else if (playerEmotion.equals("sad")) {
            currentMood = "empathetic";
        } else if (playerEmotion.equals("frustrated")) {
            currentMood = "supportive";
        }
    }

    public String getResponseStyle() {
        return switch (currentMood) {
            case "cheerful" -> "enthusiastic and upbeat";
            case "empathetic" -> "gentle and understanding";
            case "supportive" -> "encouraging and helpful";
            default -> "friendly and casual";
        };
    }
}
```

---

## 7. Java Implementation Options

### 7.1 Embedding Models for Java

#### 7.1.1 HuggingFace Models via LangChain4j

**Maven Dependency:**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.29.1</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-hugging-face</artifactId>
    <version>0.29.1</version>
</dependency>
```

**Usage:**
```java
// For production, use a small efficient model
EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
    .accessToken(System.getenv("HF_API_TOKEN")) // For paid API
    .modelId("sentence-transformers/all-MiniLM-L6-v2") // Small, fast
    .waitForModel(true)
    .timeout(Duration.ofSeconds(60))
    .build();

// For offline/local use (requires more setup)
EmbeddingModel localModel = HuggingFaceEmbeddingModel.builder()
    .modelPath(Path.of("models/all-MiniLM-L6-v2"))
    .build();

// Generate embedding
float[] embedding = embeddingModel.embed(
    "We built a tower together yesterday"
).content().vector();
```

**Recommended Models for MineWright AI:**
| Model | Dimensions | Size | Speed | Accuracy | Recommendation |
|-------|------------|------|-------|----------|----------------|
| `all-MiniLM-L6-v2` | 384 | ~80MB | Very Fast | Good | **Best for Minecraft** |
| `all-MiniLM-L12-v2` | 384 | ~120MB | Fast | Better | If quality matters more |
| `bge-small-en-v1.5` | 384 | ~130MB | Fast | Very Good | **Best overall** |
| `e5-small-v2` | 384 | ~130MB | Fast | Good | Alternative |
| `bge-base-en-v1.5` | 768 | ~400MB | Medium | Excellent | For high quality |

#### 7.1.2 Spring AI Integration

If using Spring Boot (not typical for Minecraft mods, but good for reference):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
@RestController
public class MemoryController {
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public MemoryController(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/memories")
    public String addMemory(@RequestBody String text) {
        Embedding embedding = embeddingModel.embed(text);
        Document document = new Document(text, embedding);
        vectorStore.add(List.of(document));
        return "Memory stored";
    }

    @GetMapping("/memories/search")
    public List<Document> searchMemories(@RequestParam String query) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(5)
        );
    }
}
```

### 7.2 In-Memory Vector Store Implementation

A complete, production-ready in-memory vector store for MineWright AI.

```java
package com.minewright.ai.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * High-performance in-memory vector store for semantic memory search.
 * Uses cosine similarity for retrieval.
 */
public class InMemoryVectorStore {
    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();
    private final Map<String, Metadata> metadata = new ConcurrentHashMap<>();
    private final EmbeddingModel embeddingModel;

    public InMemoryVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Stores a memory with its embedding.
     */
    public void store(String id, String content, Map<String, Object> metadata) {
        float[] vector = embeddingModel.embed(content);
        vectors.put(id, vector);
        this.metadata.put(id, new Metadata(content, metadata, Instant.now()));
    }

    /**
     * Searches for similar memories using cosine similarity.
     */
    public List<SearchResult> search(String query, int topK) {
        float[] queryVector = embeddingModel.embed(query);

        return vectors.entrySet().stream()
            .map(e -> {
                double similarity = cosineSimilarity(queryVector, e.getValue());
                return new SearchResult(
                    e.getKey(),
                    similarity,
                    metadata.get(e.getKey())
                );
            })
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * Calculates cosine similarity between two vectors.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Removes old memories based on timestamp.
     */
    public void cleanupOlderThan(Duration age) {
        Instant cutoff = Instant.now().minus(age);
        metadata.entrySet().removeIf(e -> {
            boolean shouldRemove = e.getValue().timestamp().isBefore(cutoff);
            if (shouldRemove) {
                vectors.remove(e.getKey());
            }
            return shouldRemove;
        });
    }

    public record Metadata(
        String content,
        Map<String, Object> data,
        Instant timestamp
    ) {}

    public record SearchResult(
        String id,
        double score,  // -1 to 1, higher is better
        Metadata metadata
    ) {}
}
```

### 7.3 Simple File-Based Persistence

For Minecraft mod compatibility, use JSON for easy debugging.

```java
package com.minewright.ai.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * File-based persistence for memories using JSON format.
 * Each memory type is stored in a separate file.
 */
public class FileBasedMemoryStore {
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .setPrettyPrinting()
        .create();

    private final Path storageDirectory;
    private final Map<MemoryType, Map<String, ? extends Memory>> caches;

    public FileBasedMemoryStore(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        this.caches = new EnumMap<>(MemoryType.class);
        createDirectories();
        loadAll();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create memory directory", e);
        }
    }

    public void save(MemoryType type, Map<String, ? extends Memory> memories) {
        Path file = storageDirectory.resolve(type.filename());
        try {
            String json = GSON.toJson(memories);
            Files.writeString(file, json);
            caches.put(type, memories);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save memories: " + type, e);
        }
    }

    public <T extends Memory> Map<String, T> load(MemoryType type, Class<T> memoryClass) {
        @SuppressWarnings("unchecked")
        Map<String, T> cached = (Map<String, T>) caches.get(type);
        if (cached != null) {
            return cached;
        }

        Path file = storageDirectory.resolve(type.filename());
        if (!Files.exists(file)) {
            return new HashMap<>();
        }

        try {
            String json = Files.readString(file);
            Map<String, T> loaded = GSON.fromJson(json,
                TypeToken.getParameterized(Map.class, String.class, memoryClass).getType());

            caches.put(type, loaded);
            return loaded;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load memories: " + type, e);
        }
    }

    private void loadAll() {
        for (MemoryType type : MemoryType.values()) {
            Path file = storageDirectory.resolve(type.filename());
            if (Files.exists(file)) {
                try {
                    String json = Files.readString(file);
                    // Loaded lazily when requested
                } catch (IOException e) {
                    // Log but continue
                }
            }
        }
    }

    public enum MemoryType {
        EPISODIC("episodic_memories.json"),
        SEMANTIC("semantic_memories.json"),
        EMOTIONAL("emotional_memories.json"),
        CONVERSATIONAL("conversational_memories.json"),
        WORKING("working_memory.json");

        private final String filename;

        MemoryType(String filename) {
            this.filename = filename;
        }

        public String filename() {
            return filename;
        }
    }
}
```

### 7.4 Minecraft NBT Integration

Best approach for MineWright AI - use Minecraft's native format.

```java
package com.minewright.ai.memory;

import net.minecraft.nbt.*;

import java.time.Instant;
import java.util.*;

/**
 * NBT-based persistence for full Minecraft compatibility.
 * Integrates with MineWrightEntity's save system.
 */
public class NBTMemoryStore {
    private final Map<String, EpisodicMemory> episodicMemories = new ConcurrentHashMap<>();
    private final Map<String, SemanticMemory> semanticMemories = new ConcurrentHashMap<>();
    private final List<EmotionalMemory> emotionalMemories = new ArrayList<>();
    private final ConversationalMemory conversationalMemory = new ConversationalMemory();

    /**
     * Saves all memories to NBT format.
     * Called from MineWrightEntity.saveAdditional().
     */
    public void saveToNBT(CompoundTag tag) {
        // Save episodic memories
        ListTag episodicList = new ListTag();
        for (EpisodicMemory memory : episodicMemories.values()) {
            episodicList.add(serializeEpisodicMemory(memory));
        }
        tag.put("episodic_memories", episodicList);

        // Save semantic memories
        CompoundTag semanticTag = new CompoundTag();
        for (SemanticMemory memory : semanticMemories.values()) {
            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("category", memory.category());
            memoryTag.putString("key", memory.key());
            memoryTag.putString("value", String.valueOf(memory.value()));
            memoryTag.putLong("learned_at", memory.learnedAt().toEpochMilli());
            memoryTag.putInt("confidence", memory.confidence());
            semanticTag.put(memory.key(), memoryTag);
        }
        tag.put("semantic_memories", semanticTag);

        // Save emotional memories
        ListTag emotionalList = new ListTag();
        for (EmotionalMemory memory : emotionalMemories) {
            emotionalList.add(serializeEmotionalMemory(memory));
        }
        tag.put("emotional_memories", emotionalList);

        // Save conversational memory (inside jokes)
        CompoundTag conversationTag = new CompoundTag();
        ListTag jokesList = new ListTag();
        for (InsideJoke joke : conversationalMemory.getInsideJokes()) {
            CompoundTag jokeTag = new CompoundTag();
            jokeTag.putString("context", joke.context());
            jokeTag.putString("punchline", joke.punchline());
            jokeTag.putLong("created_at", joke.createdAt().toEpochMilli());
            jokeTag.putInt("reference_count", joke.referenceCount());
            jokesList.add(jokeTag);
        }
        conversationTag.put("inside_jokes", jokesList);
        tag.put("conversational_memory", conversationTag);

        // Save vectors (compressed)
        CompoundTag vectorsTag = new CompoundTag();
        for (Map.Entry<String, float[]> entry : getVectors().entrySet()) {
            vectorsTag.putByteArray(entry.getKey(),
                floatArrayToByteArray(entry.getValue()));
        }
        tag.put("vectors", vectorsTag);
    }

    /**
     * Loads all memories from NBT format.
     * Called from MineWrightEntity.readAdditional().
     */
    public void loadFromNBT(CompoundTag tag) {
        // Load episodic memories
        if (tag.contains("episodic_memories")) {
            ListTag episodicList = tag.getList("episodic_memories", 10);
            for (Tag tag : episodicList) {
                EpisodicMemory memory = deserializeEpisodicMemory((CompoundTag) tag);
                episodicMemories.put(memory.id(), memory);
            }
        }

        // Load semantic memories
        if (tag.contains("semantic_memories")) {
            CompoundTag semanticTag = tag.getCompound("semantic_memories");
            for (String key : semanticTag.getAllKeys()) {
                CompoundTag memoryTag = semanticTag.getCompound(key);
                SemanticMemory memory = new SemanticMemory(
                    memoryTag.getString("category"),
                    memoryTag.getString("key"),
                    memoryTag.getString("value"),
                    Instant.ofEpochMilli(memoryTag.getLong("learned_at")),
                    memoryTag.getInt("confidence")
                );
                semanticMemories.put(key, memory);
            }
        }

        // Load emotional memories
        if (tag.contains("emotional_memories")) {
            ListTag emotionalList = tag.getList("emotional_memories", 10);
            for (Tag tag : emotionalList) {
                EmotionalMemory memory = deserializeEmotionalMemory((CompoundTag) tag);
                emotionalMemories.add(memory);
            }
        }

        // Load conversational memory
        if (tag.contains("conversational_memory")) {
            CompoundTag conversationTag = tag.getCompound("conversational_memory");
            ListTag jokesList = conversationTag.getList("inside_jokes", 10);
            for (Tag tag : jokesList) {
                CompoundTag jokeTag = (CompoundTag) tag;
                InsideJoke joke = new InsideJoke(
                    jokeTag.getString("context"),
                    jokeTag.getString("punchline"),
                    Instant.ofEpochMilli(jokeTag.getLong("created_at")),
                    jokeTag.getInt("reference_count")
                );
                conversationalMemory.addInsideJoke(joke);
            }
        }

        // Load vectors
        if (tag.contains("vectors")) {
            CompoundTag vectorsTag = tag.getCompound("vectors");
            for (String key : vectorsTag.getAllKeys()) {
                float[] vector = byteToFloatArray(vectorsTag.getByteArray(key));
                // Store in appropriate location
            }
        }
    }

    private CompoundTag serializeEpisodicMemory(EpisodicMemory memory) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", memory.id());
        tag.putString("event_type", memory.eventType());
        tag.putString("description", memory.description());
        tag.putLong("timestamp", memory.timestamp().toEpochMilli());
        tag.putInt("emotional_weight", memory.emotionalWeight());
        // ... other fields
        return tag;
    }

    private byte[] floatArrayToByteArray(float[] array) {
        byte[] bytes = new byte[array.length * 4];
        for (int i = 0; i < array.length; i++) {
            int bits = Float.floatToIntBits(array[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }

    private float[] byteToFloatArray(byte[] bytes) {
        float[] array = new float[bytes.length / 4];
        for (int i = 0; i < array.length; i++) {
            int bits = ((bytes[i * 4] & 0xFF) << 24) |
                       ((bytes[i * 4 + 1] & 0xFF) << 16) |
                       ((bytes[i * 4 + 2] & 0xFF) << 8) |
                       (bytes[i * 4 + 3] & 0xFF);
            array[i] = Float.intBitsToFloat(bits);
        }
        return array;
    }
}
```

---

## 8. Recommended Architecture for MineWright AI

### 8.1 Architecture Overview

Based on the research and existing `CompanionMemory` implementation, the recommended architecture is:

```
┌─────────────────────────────────────────────────────────────┐
│                    MineWright Memory System                       │
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

### 8.2 Component Responsibilities

| Component | Responsibility | Technology |
|-----------|-----------------|------------|
| **WorkingMemory** | Recent context, current conversation | In-memory, circular buffer |
| **SemanticMemory** | Player facts, preferences, world knowledge | HashMap with confidence scoring |
| **EpisodicMemory** | Shared events, experiences | Vector store (in-memory) |
| **MemoryRetriever** | Search, rank, and filter memories | Hybrid search + scoring |
| **RelationshipTracker** | Track rapport, trust, emotional bonds | Metrics with time-decay |
| **PersistenceLayer** | Save/load with Minecraft world | NBT format |
| **EmbeddingModel** | Generate embeddings for semantic search | Local model or API |

### 8.3 Data Flow

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

**Building Relationship:**
```
1. Every interaction updates rapport
2. Positive outcomes increase trust
3. Inside jokes are detected and stored
4. Player preferences are learned
5. Emotional moments are highlighted
6. Personality adapts over time
```

### 8.4 Key Design Decisions

**Why In-Memory Vector Store?**
- Single-player Minecraft doesn't need distributed storage
- Simpler deployment (no external services)
- Fast enough for <10K memories
- Can persist to NBT with world

**Why NBT Persistence?**
- Native Minecraft format
- Automatic save/load
- Works in single-player and multiplayer
- No additional files to manage

**Why Hybrid Search?**
- Vector search alone misses exact matches
- Keyword search alone misses semantic meaning
- Combined provides best of both
- 70% vector / 30% keyword balance

**Why Three-Tier Memory?**
- Working memory prevents context loss
- Semantic memory enables personalization
- Episodic memory creates shared experiences
- Each tier optimized for its purpose

---

## 9. Data Models

### 9.1 Core Memory Interface

```java
package com.minewright.ai.memory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Base interface for all memory types.
 */
public interface Memory {
    /**
     * Unique identifier for this memory.
     */
    String id();

    /**
     * Type of memory (episodic, semantic, etc.).
     */
    MemoryType type();

    /**
     * When this memory was created.
     */
    Instant timestamp();

    /**
     * Relevance score for current context (0-1).
     */
    double relevance();

    /**
     * Number of times this memory has been accessed.
     */
    int accessCount();

    /**
     * Increment access counter.
     */
    void recordAccess();

    /**
     * Get metadata as a map.
     */
    Map<String, Object> metadata();

    enum MemoryType {
        EPISODIC,      // Events and experiences
        SEMANTIC,      // Facts and knowledge
        EMOTIONAL,     // High-impact moments
        CONVERSATIONAL, // Jokes and references
        PROCEDURAL,    // Learned patterns
        WORKING        // Current context
    }
}
```

### 9.2 Enhanced Episodic Memory

```java
package com.minewright.ai.memory;

import java.time.Instant;
import java.util.*;

/**
 * Enhanced episodic memory with embedding support.
 */
public class EnhancedEpisodicMemory implements Memory {
    private final String id;
    private final String eventType;
    private final String description;
    private final Instant timestamp;
    private final int emotionalWeight;
    private final float[] embedding;
    private final List<String> participants;
    private final Location location;
    private final Map<String, Object> metadata;
    private int accessCount = 0;
    private Instant lastAccessed;

    public EnhancedEpisodicMemory(
        String id,
        String eventType,
        String description,
        int emotionalWeight,
        float[] embedding,
        List<String> participants,
        Location location
    ) {
        this.id = id;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = Instant.now();
        this.emotionalWeight = emotionalWeight;
        this.embedding = embedding;
        this.participants = participants;
        this.location = location;
        this.metadata = new HashMap<>();
        this.metadata.put("event_type", eventType);
        this.metadata.put("emotional_weight", emotionalWeight);
    }

    @Override
    public String id() { return id; }

    @Override
    public MemoryType type() { return MemoryType.EPISODIC; }

    @Override
    public Instant timestamp() { return timestamp; }

    @Override
    public double relevance() {
        // Relevance = emotional factor × recency factor × access factor
        double emotionalFactor = Math.abs(emotionalWeight) / 10.0;
        double recencyFactor = calculateRecencyFactor();
        double accessFactor = Math.min(1.0, accessCount / 10.0);

        return (emotionalFactor * 0.5) +
               (recencyFactor * 0.3) +
               (accessFactor * 0.2);
    }

    private double calculateRecencyFactor() {
        if (lastAccessed == null) {
            lastAccessed = Instant.now();
        }
        long daysSince = java.time.temporal.ChronoUnit.DAYS.between(
            timestamp, Instant.now()
        );
        return Math.pow(0.9, daysSince); // 10% daily decay
    }

    @Override
    public int accessCount() { return accessCount; }

    @Override
    public void recordAccess() {
        accessCount++;
        lastAccessed = Instant.now();
    }

    public float[] embedding() { return embedding; }

    public String eventType() { return eventType; }

    public String description() { return description; }

    public int emotionalWeight() { return emotionalWeight; }

    public List<String> participants() { return participants; }

    public Optional<Location> location() { return Optional.ofNullable(location); }

    @Override
    public Map<String, Object> metadata() { return metadata; }

    /**
     * Represents a location in the Minecraft world.
     */
    public record Location(
        int x, int y, int z,
        String dimension,
        String biome
    ) {
        public static Location fromBlockPos(net.minecraft.core.BlockPos pos,
                                             String dimension) {
            return new Location(pos.getX(), pos.getY(), pos.getZ(),
                dimension, "unknown");
        }
    }
}
```

### 9.3 Enhanced Semantic Memory

```java
package com.minewright.ai.memory;

import java.time.Instant;
import java.util.*;

/**
 * Enhanced semantic memory with confidence tracking.
 */
public class EnhancedSemanticMemory implements Memory {
    private final String id;
    private final String category;
    private final String key;
    private final Object value;
    private final Instant learnedAt;
    private final Instant lastConfirmed;
    private int confidence;
    private int confirmationCount = 0;
    private int contradictionCount = 0;
    private final Map<String, Object> metadata;

    public EnhancedSemanticMemory(
        String id,
        String category,
        String key,
        Object value,
        int confidence
    ) {
        this.id = id;
        this.category = category;
        this.key = key;
        this.value = value;
        this.learnedAt = Instant.now();
        this.lastConfirmed = Instant.now();
        this.confidence = Math.max(1, Math.min(10, confidence));
        this.metadata = new HashMap<>();
        this.metadata.put("category", category);
        this.metadata.put("key", key);
    }

    @Override
    public String id() { return id; }

    @Override
    public MemoryType type() { return MemoryType.SEMANTIC; }

    @Override
    public Instant timestamp() { return learnedAt; }

    @Override
    public double relevance() {
        // Relevance = confidence × confirmation rate
        double confidenceFactor = confidence / 10.0;
        double confirmationFactor = Math.min(1.0,
            (double) confirmationCount / (confirmationCount + contradictionCount + 1));

        return confidenceFactor * 0.7 + confirmationFactor * 0.3;
    }

    @Override
    public int accessCount() {
        return confirmationCount + contradictionCount;
    }

    @Override
    public void recordAccess() {
        // Not applicable for semantic memories
    }

    /**
     * Confirm this fact, increasing confidence.
     */
    public void confirm() {
        confirmationCount++;
        lastConfirmed = Instant.now();
        confidence = Math.min(10, confidence + 1);
    }

    /**
     * Record a contradiction, decreasing confidence.
     */
    public void contradict() {
        contradictionCount++;
        confidence = Math.max(1, confidence - 2);

        // If too many contradictions, mark as unreliable
        if (contradictionCount > confirmationCount * 2) {
            metadata.put("unreliable", true);
        }
    }

    /**
     * Check if this fact is still considered reliable.
     */
    public boolean isReliable() {
        return confidence >= 3 &&
               !Boolean.TRUE.equals(metadata.get("unreliable"));
    }

    public String category() { return category; }

    public String key() { return key; }

    public Object value() { return value; }

    public int confidence() { return confidence; }

    @Override
    public Map<String, Object> metadata() { return metadata; }
}
```

### 9.4 Relationship State

```java
package com.minewright.ai.memory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Tracks the relationship state between MineWright and the player.
 */
public class RelationshipState {
    private String playerName;
    private Instant firstMeeting;
    private final AtomicInteger rapportLevel = new AtomicInteger(10);
    private final AtomicInteger trustLevel = new AtomicInteger(5);
    private final List<RelationshipEvent> history = new ArrayList<>();
    private final Set<String> insideJokes = ConcurrentHashMap.newKeySet();
    private final Map<String, LearnedPreference> preferences = new ConcurrentHashMap<>();

    /**
     * Initialize relationship on first meeting.
     */
    public void initialize(String playerName) {
        if (this.playerName == null) {
            this.playerName = playerName;
            this.firstMeeting = Instant.now();
            recordEvent("first_meeting", "Met " + playerName, 7);
        }
    }

    /**
     * Record a relationship-affecting event.
     */
    public void recordEvent(String type, String description, int impact) {
        history.add(new RelationshipEvent(type, description, impact, Instant.now()));
        adjustRapport(impact);

        // High-impact events also affect trust
        if (Math.abs(impact) >= 5) {
            adjustTrust(impact > 0 ? 2 : -3);
        }
    }

    /**
     * Adjust rapport level.
     */
    public void adjustRapport(int delta) {
        rapportLevel.set(Math.max(0, Math.min(100, rapportLevel.get() + delta)));
    }

    /**
     * Adjust trust level.
     */
    public void adjustTrust(int delta) {
        trustLevel.set(Math.max(0, Math.min(100, trustLevel.get() + delta)));
    }

    /**
     * Get relationship description based on metrics.
     */
    public String getRelationshipDescription() {
        long daysKnown = firstMeeting != null ?
            ChronoUnit.DAYS.between(firstMeeting, Instant.now()) : 0;

        return String.format("""
            Relationship with %s (known for %d days)
            Rapport: %d/100 - %s
            Trust: %d/100 - %s
            Shared inside jokes: %d
            Known preferences: %d
            """,
            playerName,
            daysKnown,
            rapportLevel.get(), getRapportLabel(),
            trustLevel.get(), getTrustLabel(),
            insideJokes.size(),
            preferences.size()
        );
    }

    private String getRapportLabel() {
        int r = rapportLevel.get();
        if (r >= 80) return "Best friends";
        if (r >= 60) return "Close companions";
        if (r >= 40) return "Friendly";
        if (r >= 20) return "Acquaintances";
        return "Strangers";
    }

    private String getTrustLabel() {
        int t = trustLevel.get();
        if (t >= 80) return "Complete trust";
        if (t >= 60) return "High trust";
        if (t >= 40) return "Moderate trust";
        if (t >= 20) return "Cautious";
        return "Distrustful";
    }

    public record RelationshipEvent(
        String type,
        String description,
        int impact,
        Instant timestamp
    ) {}

    public record LearnedPreference(
        String key,
        String value,
        int confidence,
        Instant lastObserved
    ) {}
}
```

---

## 10. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
**Goal:** Enhance existing `CompanionMemory` with vector embeddings

**Tasks:**
1. Integrate embedding model (all-MiniLM-L6-v2)
2. Create `InMemoryVectorStore` class
3. Add embeddings to `EpisodicMemory` storage
4. Implement basic semantic search
5. Add NBT persistence for vectors

**Deliverables:**
- Working semantic search
- Persistent vector storage
- Test suite for retrieval

### Phase 2: Enhanced Retrieval (Week 3)
**Goal:** Implement sophisticated retrieval strategies

**Tasks:**
1. Create `MemoryRetriever` with multi-factor scoring
2. Implement hybrid search (vector + keyword)
3. Add MMR for diversity
4. Create context-aware retrieval
5. Add temporal retrieval patterns

**Deliverables:**
- Advanced search capabilities
- Context-specific memory access
- Performance benchmarks

### Phase 3: Relationship Deepening (Week 4)
**Goal:** Enhance relationship tracking features

**Tasks:**
1. Enhance `RelationshipTracker` with rapport/trust
2. Implement inside joke detection
3. Add player preference learning
4. Create emotional synchronization
5. Build personality adaptation

**Deliverables:**
- Dynamic relationship system
- Inside joke references in conversation
- Player-specific behavior adaptation

### Phase 4: Memory Management (Week 5)
**Goal:** Implement summarization and cleanup

**Tasks:**
1. Create conversation summarizer
2. Implement fact extraction
3. Add memory consolidation
4. Create old memory cleanup
5. Add memory importance scoring

**Deliverables:**
- Automatic summarization
- Fact extraction from conversations
- Memory lifecycle management

### Phase 5: Integration & Polish (Week 6)
**Goal:** Integrate with LLM and add UI

**Tasks:**
1. Connect memory to `PromptBuilder`
2. Add memory context to LLM calls
3. Create memory debug UI
4. Add memory editing commands
5. Performance optimization

**Deliverables:**
- Fully integrated memory system
- Player-visible memory features
- Production-ready performance

---

## 11. Code Examples

### 11.1 Complete Memory Service

```java
package com.minewright.ai.memory;

import com.minewright.ai.entity.MineWrightEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Complete memory service for MineWright AI companion.
 * Integrates all memory types and provides unified interface.
 */
public class MineWrightMemoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MineWrightMemoryService.class);

    private final MineWrightEntity minewright;
    private final EmbeddingModel embeddingModel;
    private final InMemoryVectorStore vectorStore;
    private final RelationshipState relationship;
    private final FileBasedMemoryStore persistence;

    // Memory stores
    private final Map<String, EnhancedEpisodicMemory> episodicMemories = new ConcurrentHashMap<>();
    private final Map<String, EnhancedSemanticMemory> semanticMemories = new ConcurrentHashMap<>();
    private final Deque<WorkingMemoryEntry> workingMemory = new ConcurrentLinkedDeque<>();

    public MineWrightMemoryService(MineWrightEntity minewright) {
        this.minewright = minewright;
        this.embeddingModel = new LocalEmbeddingModel(); // Or API-based
        this.vectorStore = new InMemoryVectorStore(embeddingModel);
        this.relationship = new RelationshipState();
        this.persistence = new FileBasedMemoryStore(
            minewright.level().getServer().getFile("minewright_memories").toPath()
        );

        loadMemories();
    }

    // === Recording Memories ===

    /**
     * Record a shared experience with the player.
     */
    public void recordExperience(
        String eventType,
        String description,
        int emotionalWeight,
        net.minecraft.core.BlockPos location
    ) {
        String id = UUID.randomUUID().toString();
        float[] embedding = embeddingModel.embed(description);

        EnhancedEpisodicMemory memory = new EnhancedEpisodicMemory(
            id,
            eventType,
            description,
            emotionalWeight,
            embedding,
            List.of(minewright.getPlayerName()),
            EnhancedEpisodicMemory.Location.fromBlockPos(location, minewright.level().dimension().location().toString())
        );

        episodicMemories.put(id, memory);
        vectorStore.store(id, description, Map.of(
            "type", "episodic",
            "event_type", eventType,
            "emotional_weight", emotionalWeight,
            "timestamp", Instant.now().toString()
        ));

        relationship.recordEvent(eventType, description, emotionalWeight);

        LOGGER.debug("Recorded experience: {}", description);
    }

    /**
     * Learn a fact about the player.
     */
    public void learnPlayerFact(String category, String key, Object value) {
        String id = category + ":" + key;
        EnhancedSemanticMemory memory = new EnhancedSemanticMemory(
            id, category, key, value, 5 // Initial confidence
        );

        semanticMemories.put(id, memory);

        if ("preference".equals(category)) {
            relationship.preferences().put(key,
                new RelationshipState.LearnedPreference(
                    key, value.toString(), 5, Instant.now()
                )
            );
        }

        LOGGER.debug("Learned fact: {} = {}", key, value);
    }

    /**
     * Record a conversational turn.
     */
    public void recordConversation(String speaker, String message) {
        workingMemory.addFirst(new WorkingMemoryEntry(
            speaker, message, Instant.now()
        ));

        // Keep only recent entries
        while (workingMemory.size() > 20) {
            workingMemory.removeLast();
        }

        // Detect emotions and player preferences
        if (speaker.equals("player")) {
            detectEmotionalContent(message);
            extractPreferences(message);
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
        context.append(relationship.getRelationshipDescription()).append("\n");

        // Add relevant memories
        context.append("\n=== Relevant Memories ===\n");
        List<InMemoryVectorStore.SearchResult> relevantMemories =
            vectorStore.search(currentSituation, 5);

        for (InMemoryVectorStore.SearchResult result : relevantMemories) {
            if (result.score() > 0.5) { // Only include relevant memories
                EnhancedEpisodicMemory memory = episodicMemories.get(result.id());
                if (memory != null) {
                    context.append("- ").append(memory.description()).append("\n");
                }
            }
        }

        // Add recent working memory
        context.append("\n=== Recent Context ===\n");
        for (WorkingMemoryEntry entry : workingMemory) {
            context.append(entry.type()).append(": ").append(entry.content()).append("\n");
        }

        // Add known preferences
        context.append("\n=== Known Player Preferences ===\n");
        semanticMemories.values().stream()
            .filter(m -> m.category().equals("preference") && m.isReliable())
            .limit(5)
            .forEach(m -> context.append("- ")
                .append(m.key()).append(": ")
                .append(m.value()).append("\n"));

        return context.toString();
    }

    /**
     * Get an inside joke if contextually appropriate.
     */
    public Optional<String> getInsideJoke(String context) {
        return relationship.insideJokes().stream()
            .filter(joke -> isContextuallyRelevant(joke, context))
            .findFirst();
    }

    // === Persistence ===

    /**
     * Save all memories to disk.
     */
    public void saveMemories() {
        persistence.save(FileBasedMemoryStore.MemoryType.EPISODIC, episodicMemories);
        persistence.save(FileBasedMemoryStore.MemoryType.SEMANTIC, semanticMemories);
        // Save other types...
    }

    /**
     * Load all memories from disk.
     */
    private void loadMemories() {
        Map<String, EnhancedEpisodicMemory> loadedEpisodic =
            persistence.load(FileBasedMemoryStore.MemoryType.EPISODIC,
                EnhancedEpisodicMemory.class);

        episodicMemories.putAll(loadedEpisodic);

        // Rebuild vector store
        for (EnhancedEpisodicMemory memory : episodicMemories.values()) {
            vectorStore.store(memory.id(), memory.description(),
                Map.of("type", "episodic", "event_type", memory.eventType()));
        }

        LOGGER.info("Loaded {} memories", episodicMemories.size());
    }

    // === Private Helpers ===

    private void detectEmotionalContent(String message) {
        // Use LLM to detect emotion
        String emotion = embeddingModel.classifyEmotion(message);

        // Adjust relationship based on detected emotion
        if (emotion.equals("happy")) {
            relationship.adjustRapport(1);
        } else if (emotion.equals("frustrated")) {
            // Empathy - show support
            relationship.adjustRapport(1);
        }
    }

    private void extractPreferences(String message) {
        // Use LLM to extract preferences
        String prompt = """
            Extract any player preferences from this message.
            Return as JSON: {"preferences": [{"key": "...", "value": "..."}]}
            Message: %s
            """.formatted(message);

        // Parse and store preferences
        // ...
    }

    private boolean isContextuallyRelevant(String joke, String context) {
        // Simple keyword matching
        String[] jokeWords = joke.toLowerCase().split("\\s+");
        String[] contextWords = context.toLowerCase().split("\\s+");

        return Arrays.stream(jokeWords)
            .anyMatch(jw -> Arrays.stream(contextWords)
                .anyMatch(cw -> cw.contains(jw)));
    }

    public record WorkingMemoryEntry(
        String type,
        String content,
        Instant timestamp
    ) {}
}
```

### 11.2 Integration with PromptBuilder

```java
package com.minewright.ai.llm;

import com.minewright.ai.memory.MineWrightMemoryService;

/**
 * Enhanced prompt builder that incorporates memory context.
 */
public class MemoryAwarePromptBuilder extends PromptBuilder {
    private final MineWrightMemoryService memoryService;

    public MemoryAwarePromptBuilder(MineWrightMemoryService memoryService) {
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

            Respond as MineWright, taking into account your shared history and relationship with the player.
            """.formatted(
                getSystemPrompt(),
                memoryContext,
                task,
                worldContext
            );
    }

    private String getSystemPrompt() {
        String basePrompt = """
            You are MineWright, an AI companion in Minecraft.
            You have a long-term relationship with the player and remember your shared experiences.

            Personality traits:
            - Friendly and helpful
            - Enthusiastic about building
            - Loyal companion
            - Occasionally humorous

            Memory capabilities:
            - You remember shared experiences
            - You know the player's preferences
            - You can reference inside jokes
            - You adapt to the player's playstyle
            """;

        // Add relationship-specific personality
        int rapport = memoryService.getRelationship().rapportLevel().get();
        if (rapport >= 80) {
            basePrompt += "\n- You are best friends with the player\n";
        } else if (rapport >= 60) {
            basePrompt += "\n- You are close companions\n";
        } else if (rapport >= 40) {
            basePrompt += "\n- You are friendly\n";
        } else {
            basePrompt += "\n- You are still getting to know the player\n";
        }

        return basePrompt;
    }
}
```

### 11.3 Memory Commands for Players

```java
package com.minewright.ai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.minewright.ai.memory.MineWrightMemoryService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Commands for players to interact with MineWright's memory.
 */
public class MemoryCommands {

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        MineWrightMemoryService memoryService
    ) {
        // View MineWright's memories of you
        dispatcher.register(
            Commands.literal("minewright")
                .then(Commands.literal("memories")
                    .executes(ctx -> showMemories(ctx, memoryService))
                )
        );

        // View relationship status
        dispatcher.register(
            Commands.literal("minewright")
                .then(Commands.literal("relationship")
                    .executes(ctx -> showRelationship(ctx, memoryService))
                )
        );

        // Teach MineWright a preference
        dispatcher.register(
            Commands.literal("minewright")
                .then(Commands.literal("remember")
                    .then(Commands.argument("preference", StringArgumentType.string())
                        .executes(ctx -> rememberPreference(ctx, memoryService))
                    )
                )
        );

        // Forget a specific memory
        dispatcher.register(
            Commands.literal("minewright")
                .then(Commands.literal("forget")
                    .then(Commands.argument("memory_id", StringArgumentType.string())
                        .executes(ctx -> forgetMemory(ctx, memoryService))
                    )
                )
        );
    }

    private static int showMemories(
        CommandContext<CommandSourceStack> ctx,
        MineWrightMemoryService memoryService
    ) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("=== MineWright's Memories of You ===\n" +
                memoryService.getRecentMemories(10).stream()
                    .map(m -> "- " + m.description())
                    .collect(Collectors.joining("\n"))
            ), false
        );
        return 1;
    }

    private static int showRelationship(
        CommandContext<CommandSourceStack> ctx,
        MineWrightMemoryService memoryService
    ) {
        ctx.getSource().sendSuccess(() ->
            Component.literal(memoryService.getRelationship()
                .getRelationshipDescription()), false
        );
        return 1;
    }

    private static int rememberPreference(
        CommandContext<CommandSourceStack> ctx,
        MineWrightMemoryService memoryService
    ) {
        String preference = StringArgumentType.getString(ctx, "preference");
        memoryService.learnPlayerFact("preference", preference, true);
        ctx.getSource().sendSuccess(() ->
            Component.literal("Got it! I'll remember that."), false
        );
        return 1;
    }

    private static int forgetMemory(
        CommandContext<CommandSourceStack> ctx,
        MineWrightMemoryService memoryService
    ) {
        String memoryId = StringArgumentType.getString(ctx, "memory_id");
        memoryService.deleteMemory(memoryId);
        ctx.getSource().sendSuccess(() ->
            Component.literal("I've forgotten that memory."), false
        );
        return 1;
    }
}
```

---

## 12. References

### Academic Papers

1. **Mem0: Building Production-Ready AI Agents with Scalable Long-Term Memory** (2025)
   - arXiv paper on Mem0 architecture
   - https://arxiv.org/abs/2504.xxxxx

2. **MemGPT: Teaching LLMs memory management for unbounded context**
   - UC Berkeley research on virtual memory for LLMs
   - https://arxiv.org/abs/2310.08560

3. **MIRIX: Multi-Agent Memory System for LLM-Based Agents** (2025)
   - arXiv:2507.07957
   - https://arxiv.org/html/2507.07957v1

4. **Beyond Fact Retrieval: Episodic Memory for RAG with Generative Semantic Workspaces**
   - Neuro-inspired memory framework
   - https://www.x-mol.com/paper/1988835040161804288

### Frameworks & Libraries

1. **Mem0** - Open-source memory layer
   - GitHub: https://github.com/mem0ai/mem0
   - Docs: https://docs.mem0.ai

2. **Letta (MemGPT)** - Infinite context framework
   - GitHub: https://github.com/letta-ai/letta
   - Website: https://letta.ai

3. **LangChain4j** - Java LLM framework
   - GitHub: https://github.com/langchain4j/langchain4j
   - Docs: https://docs.langchain4j.ai

4. **LangChain** - Python LLM framework
   - GitHub: https://github.com/langchain-ai/langchain
   - Docs: https://python.langchain.com

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

### Research Sources

1. **AI Agent Memory Systems: OpenClaw Memory Best Practices**
   - https://juejin.cn/post/7608782906941292584

2. **Building AI Agents with Long-term Memory**
   - https://blog.csdn.net/shebao3333/article/details/158156423

3. **Three-Layer Memory Architecture Guide**
   - https://m.blog.csdn.net/2301_76168381/article/details/156614129

4. **Generative Agents Memory Analysis (Stanford Research)**
   - https://juejin.cn/post/7529878505359867930

5. **AI Agent Behavioral Science (arXiv)**
   - https://arxiv.org/html/2506.06366v1

6. **Spring AI with Embedding Models**
   - http://juejin.cn/entry/7487968956055126031

7. **Neo4j Java Embedded Development**
   - https://blog.csdn.net/u010839779/article/details/134334809

### Minecraft & Java Resources

1. **Minecraft Forge Documentation**
   - https://docs.minecraftforge.net

2. **Minecraft NBT Format**
   - https://wiki.vg/NBT

3. **Java 17 Documentation**
   - https://docs.oracle.com/en/java/javase/17/

---

## Conclusion

This research document provides a comprehensive foundation for implementing an advanced memory system for MineWright AI. The key recommendations are:

1. **Use a hybrid architecture** combining working memory, semantic memory, and episodic memory with vector embeddings
2. **Implement in-memory vector search** with cosine similarity for semantic retrieval
3. **Use NBT persistence** for full Minecraft compatibility
4. **Track relationship metrics** (rapport, trust) to create meaningful long-term bonds
5. **Enhance existing CompanionMemory** rather than replacing it

The existing `CompanionMemory` class in the codebase provides an excellent foundation, particularly its support for episodic memories, semantic facts, emotional memories, inside jokes, and relationship tracking. The enhancements should focus on:

- Adding vector embeddings for semantic search
- Implementing sophisticated retrieval strategies
- Improving persistence and NBT integration
- Connecting memory context to LLM prompting

With these improvements, MineWright AI will be able to build meaningful, long-term relationships with players, creating a truly companionable AI that remembers shared experiences and grows closer over time.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Author:** Research Compilation
**Status:** Ready for Implementation
