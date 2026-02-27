# Memory System Improvements - Enhanced Analysis

**Analysis Date:** 2026-02-27
**Project:** Steve AI (Minecraft Mod)
**Package:** `com.minewright.memory`
**Analysis Focus:** Memory Leaks, Thread Safety, Context Windowing, Caching, Persistence

---

## Executive Summary

This document provides an enhanced analysis of the memory system with specific focus on:
1. **Memory leak patterns** - Potential unbounded growth scenarios
2. **Thread safety issues** - Race conditions and concurrent access problems
3. **Conversation context windowing** - Missing conversation history management
4. **World knowledge caching efficiency** - No spatial/temporal caching strategy
5. **Memory persistence design** - NBT serialization limitations

**Critical Findings:**
- **Memory Leak Risk:** HIGH - Unbounded semantic memory growth, no cache size limits
- **Thread Safety Issues:** MEDIUM - Race conditions in vector store operations
- **Missing Context Window:** HIGH - No conversation history tracking
- **No World Caching:** MEDIUM - Re-scans world on every access
- **Persistence Limitations:** LOW - NBT works but has size constraints

---

## 1. Memory Eviction Policies Analysis

### 1.1 Current Eviction Implementation

**CompanionMemory.java:**

| Memory Type | Storage | Limit | Eviction Policy |
|-------------|---------|-------|-----------------|
| Episodic | `ConcurrentLinkedDeque` | 200 | FIFO (removeLast) |
| Semantic | `ConcurrentHashMap` | **NONE** | No eviction |
| Emotional | `ArrayList` | 50 | Sorted removal |
| Working | `ConcurrentLinkedDeque` | 20 | FIFO |
| Inside Jokes | `ArrayList` (inner) | 30 | Reference-count based |

### 1.2 Critical Issues

#### Issue 1: Unbounded Semantic Memory Growth

**Location:** `CompanionMemory.java` lines 79, 256-267

```java
private final Map<String, SemanticMemory> semanticMemories;

public void learnPlayerFact(String category, String key, Object value) {
    String compositeKey = category + ":" + key;
    semanticMemories.put(compositeKey, new SemanticMemory(...));
    // NO EVICTION - grows indefinitely
}
```

**Risk:** After long gameplay, semantic memory can grow to thousands of entries.

**Evidence of unbounded growth:**
- Every player fact learned is stored permanently
- No consolidation of similar facts
- No confidence-based decay
- Each fact includes timestamp and metadata

**Recommended Eviction Strategy:**

```java
public class SemanticMemoryEvictionPolicy {
    private static final int MAX_SEMANTIC_MEMORIES = 500;
    private static final int CONFIDENCE_THRESHOLD = 3;

    public void learnPlayerFact(String category, String key, Object value) {
        String compositeKey = category + ":" + key;
        SemanticMemory memory = new SemanticMemory(category, key, value, Instant.now());

        semanticMemories.put(compositeKey, memory);

        // Evict if over limit
        if (semanticMemories.size() > MAX_SEMANTIC_MEMORIES) {
            evictLowConfidenceOrOldMemories();
        }
    }

    private void evictLowConfidenceOrOldMemories() {
        semanticMemories.entrySet().removeIf(entry -> {
            SemanticMemory mem = entry.getValue();
            Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);

            // Remove low confidence or old memories
            return mem.confidence < CONFIDENCE_THRESHOLD ||
                   mem.learnedAt.isBefore(cutoff);
        });
    }
}
```

#### Issue 2: Vector Store Orphaned Entries

**Location:** `CompanionMemory.java` lines 226-233

```java
while (episodicMemories.size() > MAX_EPISODIC_MEMORIES) {
    EpisodicMemory removed = episodicMemories.removeLast();
    Integer vectorId = memoryToVectorId.remove(removed);
    if (vectorId != null) {
        memoryVectorStore.remove(vectorId);
    }
}
```

**Potential Issue:** Race condition where `memoryToVectorId` mapping is removed before vector store cleanup.

**Thread Safety Risk:**
```java
// Thread 1: Evicting
EpisodicMemory removed = episodicMemories.removeLast();
Integer vectorId = memoryToVectorId.remove(removed);  // ← Gap here

// Thread 2: Searching (concurrent access)
float[] queryEmbedding = embeddingModel.embed(query);
memoryVectorStore.search(queryEmbedding, k);  // ← Could access removed vector
```

**Fix:** Atomic removal operation:

```java
private synchronized void evictOldestMemory() {
    if (episodicMemories.size() <= MAX_EPISODIC_MEMORIES) {
        return;
    }

    EpisodicMemory removed = episodicMemories.removeLast();

    // Atomic removal of mapping and vector
    VectorRemovalResult result = removeFromVectorStore(removed);
    if (result.success) {
        LOGGER.debug("Evicted memory {}: {}", result.vectorId, removed.eventType);
    }
}

private static class VectorRemovalResult {
    final boolean success;
    final Integer vectorId;

    VectorRemovalResult(boolean success, Integer vectorId) {
        this.success = success;
        this.vectorId = vectorId;
    }
}
```

#### Issue 3: Embedding Cache No Size Limit

**Location:** `PlaceholderEmbeddingModel.java` line 34

```java
private final ConcurrentHashMap<String, float[]> cache;

@Override
public float[] embed(String text) {
    return cache.computeIfAbsent(text, this::generateEmbedding);
    // UNBOUNDED CACHE - grows with every unique text
}
```

**Risk:** Memory leak through accumulation of unique text embeddings.

**Recommended Fix:**

```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class BoundedEmbeddingModel implements EmbeddingModel {
    private final Cache<String, float[]> cache;

    public BoundedEmbeddingModel() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    }

    @Override
    public float[] embed(String text) {
        return cache.get(text, this::generateEmbedding);
    }
}
```

### 1.3 Eviction Policy Recommendations

#### Priority 1: Add Semantic Memory Limit

```java
// In CompanionMemory.java
private static final int MAX_SEMANTIC_MEMORIES = 500;
private static final long SEMANTIC_MEMORY_MAX_AGE_DAYS = 90;
```

#### Priority 2: Add Vector Store Size Check

```java
// Log warning when vector store grows too large
public void recordExperience(String eventType, String description, int emotionalWeight) {
    // ... existing code ...

    if (memoryVectorStore.size() > 1000) {
        LOGGER.warn("Vector store size ({}) exceeds recommended limit (1000). "
                  + "Consider implementing HNSW for better performance.",
                  memoryVectorStore.size());
    }
}
```

#### Priority 3: Implement Importance-Based Eviction

```java
public class ImportanceBasedEvictionPolicy {
    private double calculateImportance(EpisodicMemory memory) {
        double score = 0.0;

        // Emotional weight (40%)
        score += Math.abs(memory.emotionalWeight) * 0.4;

        // Recency (30%)
        long daysOld = ChronoUnit.DAYS.between(
            memory.timestamp,
            Instant.now()
        );
        score += Math.max(0, 1 - (daysOld / 365.0)) * 0.3;

        // Event type importance (30%)
        score += getEventTypeImportance(memory.eventType) * 0.3;

        return score;
    }

    private double getEventTypeImportance(String eventType) {
        // Milestone events are most important
        if (eventType.contains("first_")) return 1.0;
        if (eventType.equals("milestone")) return 1.0;
        if (eventType.equals("success")) return 0.8;
        if (eventType.equals("failure")) return 0.6;
        return 0.3; // Default for routine events
    }
}
```

---

## 2. Memory Leak Patterns

### 2.1 StructureRegistry Static List

**Location:** `StructureRegistry.java` line 14

```java
private static final List<BuiltStructure> structures = new ArrayList<>();

public static void register(BlockPos pos, int width, int height, int depth, String type) {
    BuiltStructure structure = new BuiltStructure(pos, width, height, depth, type);
    structures.add(structure);  // NEVER CLEARED
}
```

**Memory Leak:** Static list grows unbounded. Never cleared even when structures are destroyed.

**Impact:** After building 1000 structures, list holds references preventing GC.

**Recommended Fix:**

```java
public class StructureRegistry {
    private static final List<BuiltStructure> structures = new ArrayList<>();
    private static final int MAX_STRUCTURES = 500;

    public static void register(BlockPos pos, int width, int height, int depth, String type) {
        BuiltStructure structure = new BuiltStructure(pos, width, height, depth, type);

        structures.add(structure);

        // Evict oldest if over limit
        if (structures.size() > MAX_STRUCTURES) {
            structures.remove(0);
            LOGGER.debug("Structure registry full, evicted oldest entry");
        }
    }

    public static void unregister(BlockPos pos, int width, int height, int depth) {
        structures.removeIf(structure ->
            structure.position.equals(pos) &&
            structure.width == width &&
            structure.height == height &&
            structure.depth == depth
        );
    }
}
```

### 2.2 MilestoneTracker FirstOccurrences Map

**Location:** `MilestoneTracker.java` line 49

```java
private final Map<String, Instant> firstOccurrences;

private Optional<Milestone> checkFirstMilestone(...) {
    String firstKey = "first_" + eventType;

    if (!firstOccurrences.containsKey(firstKey)) {
        firstOccurrences.put(firstKey, Instant.now());  // NEVER REMOVED
        // ... create milestone ...
    }
}
```

**Issue:** Every unique event type is stored forever.

**Risk:** If event types include dynamic content (e.g., `"diamond_found_at_123_45_678"`), map grows unbounded.

**Recommended Fix:**

```java
public class MilestoneTracker {
    private static final int MAX_FIRST_OCCURRENCES = 100;
    private static final Set<String> TRACKED_EVENT_TYPES = Set.of(
        "diamond_found",
        "nether_visit",
        "structure_built",
        "enemy_defeated",
        "night_survived",
        "gift_exchanged"
    );

    private Optional<Milestone> checkFirstMilestone(...) {
        // Only track whitelisted event types
        if (!TRACKED_EVENT_TYPES.contains(eventType)) {
            return Optional.empty();
        }

        String firstKey = "first_" + eventType;

        if (!firstOccurrences.containsKey(firstKey)) {
            firstOccurrences.put(firstKey, Instant.now());

            // Evict if over limit
            if (firstOccurrences.size() > MAX_FIRST_OCCURRENCES) {
                evictOldestFirstOccurrence();
            }

            // ... create milestone ...
        }
    }

    private void evictOldestFirstOccurrence() {
        firstOccurrences.entrySet().stream()
            .min(Comparator.comparing(Map.Entry::getValue))
            .ifPresent(entry -> {
                firstOccurrences.remove(entry.getKey());
                LOGGER.debug("Evicted first occurrence: {}", entry.getKey());
            });
    }
}
```

### 2.3 SessionTopics Set Never Cleared

**Location:** `CompanionMemory.java` line 150

```java
private final Set<String> sessionTopics;

public Set<String> getSessionTopics() {
    return Collections.unmodifiableSet(sessionTopics);
}
```

**Issue:** `sessionTopics` accumulates but is never cleared between sessions.

**Recommended Fix:**

```java
public class CompanionMemory {
    public void startNewSession() {
        sessionStart = Instant.now();
        sessionTopics.clear();

        // Archive old topics to long-term memory
        conversationalMemory.archiveTopics(sessionTopics);

        LOGGER.debug("Started new session, cleared topics");
    }

    // Call this when player disconnects or new session starts
}
```

### 2.4 Memory Leak Detection

**Add monitoring code:**

```java
public class MemoryLeakDetector {
    private static final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public static void track(String componentName, int size) {
        counters.computeIfAbsent(componentName, k -> new AtomicInteger())
                .set(size);
    }

    public static void logStats() {
        counters.forEach((component, count) -> {
            LOGGER.info("Memory component '{}': size={}", component, count.get());

            // Warn if suspiciously large
            if (count.get() > 1000) {
                LOGGER.warn("Potential memory leak in '{}': size={}",
                           component, count.get());
            }
        });
    }

    // Call periodically (e.g., every 5 minutes)
    public static void scheduleMonitoring() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            MemoryLeakDetector::logStats,
            5, 5, TimeUnit.MINUTES
        );
    }
}

// In CompanionMemory
public void recordExperience(...) {
    episodicMemories.addFirst(memory);
    MemoryLeakDetector.track("episodic_memories", episodicMemories.size());
    MemoryLeakDetector.track("semantic_memories", semanticMemories.size());
    // ...
}
```

---

## 3. Thread Safety Analysis

### 3.1 ConcurrentLinkedDeque Size Check Race

**Location:** `CompanionMemory.java` lines 226-233

```java
// Thread 1
while (episodicMemories.size() > MAX_EPISODIC_MEMORIES) {  // ← Check
    EpisodicMemory removed = episodicMemories.removeLast();  // ← Act
    // ...
}

// Thread 2 (concurrent)
episodicMemories.addFirst(newMemory);  // ← Can add between check and remove
```

**Race Condition:** Multiple threads can exceed limit simultaneously.

**Current Protection:** `ConcurrentLinkedDeque` operations are atomic, but `size()` is not.

**Fix:** Use bounded deque or atomic operations:

```java
public class BoundedMemoryDeque<T> {
    private final AtomicInteger size = new AtomicInteger(0);
    private final ConcurrentLinkedDeque<T> deque = new ConcurrentLinkedDeque<>();
    private final int maxSize;

    public void addFirst(T item) {
        deque.addFirst(item);

        // Atomic increment and check
        int newSize = size.incrementAndGet();
        if (newSize > maxSize) {
            // Remove last atomically
            T removed = deque.removeLast();
            size.decrementAndGet();
            onEviction(removed);
        }
    }

    private void onEviction(T removed) {
        // Handle eviction
    }
}
```

### 3.2 EmotionalMemory Sort Race

**Location:** `CompanionMemory.java` lines 272-287

```java
private void recordEmotionalMemory(...) {
    EmotionalMemory memory = new EmotionalMemory(...);
    emotionalMemories.add(memory);  // ← NOT THREAD-SAFE

    // Sort is NOT atomic with add
    emotionalMemories.sort((a, b) -> Integer.compare(
        Math.abs(b.emotionalWeight), Math.abs(a.emotionalWeight)
    ));  // ← Can interleave with other adds

    // Cap at 50
    if (emotionalMemories.size() > 50) {
        emotionalMemories.remove(emotionalMemories.size() - 1);
    }
}
```

**Issue:** `ArrayList` is not thread-safe for concurrent modification.

**Fix:** Use concurrent collection or synchronization:

```java
public class CompanionMemory {
    private final List<EmotionalMemory> emotionalMemories =
        Collections.synchronizedList(new ArrayList<>());

    private void recordEmotionalMemory(...) {
        synchronized (emotionalMemories) {
            EmotionalMemory memory = new EmotionalMemory(...);
            emotionalMemories.add(memory);

            // Now sort is safe
            emotionalMemories.sort(Comparator.comparingInt(
                m -> -Math.abs(m.emotionalWeight)
            ));

            if (emotionalMemories.size() > 50) {
                emotionalMemories.remove(50);
            }
        }
    }
}
```

### 3.3 Vector Store Search Race

**Location:** `InMemoryVectorStore.java` lines 97-121

```java
public List<VectorSearchResult<T>> search(float[] queryVector, int k) {
    // Stream over values - can change during iteration
    List<VectorSearchResult<T>> results = vectors.values().stream()
        .map(entry -> {
            double similarity = cosineSimilarity(queryVector, entry.vector);
            return new VectorSearchResult<>(entry.data, similarity, entry.id);
        })
        // ...
}
```

**Issue:** `ConcurrentHashMap.values()` returns weakly consistent view, but entries can be added/removed during iteration.

**Impact:** Search might miss newly added memories or include partially removed ones.

**Fix:** Snapshot during search:

```java
public List<VectorSearchResult<T>> search(float[] queryVector, int k) {
    // Create snapshot for consistent search
    Collection<VectorEntry<T>> snapshot = List.copyOf(vectors.values());

    List<VectorSearchResult<T>> results = snapshot.stream()
        .map(entry -> {
            double similarity = cosineSimilarity(queryVector, entry.vector);
            return new VectorSearchResult<>(entry.data, similarity, entry.id);
        })
        // ...
}
```

### 3.4 MilestoneTracker Counter Race

**Location:** `MilestoneTracker.java` line 195

```java
private Optional<Milestone> checkCountMilestone(...) {
    String counterKey = "count_" + eventType;
    int newCount = counters.merge(counterKey, 1, Integer::sum);

    // Check for milestone AFTER increment
    for (int threshold : countMilestones) {
        if (newCount == threshold) {  // ← Race: two threads could both trigger
            // ...
        }
    }
}
```

**Race:** Two threads incrementing counter simultaneously could both trigger same milestone.

**Fix:** Atomic milestone check:

```java
private Optional<Milestone> checkCountMilestone(...) {
    String counterKey = "count_" + eventType;

    // Atomic increment AND milestone check
    int newCount = counters.compute(counterKey, (key, current) -> {
        int next = (current == null) ? 1 : current + 1;

        // Check if this increment crosses a milestone threshold
        for (int threshold : countMilestones) {
            if (next == threshold) {
                // Trigger milestone atomically
                milestoneTriggered(key, threshold);
            }
        }

        return next;
    });

    // Check for pending milestone
    return getPendingMilestone(counterKey);
}
```

### 3.5 Thread Safety Summary

| Component | Collection | Thread-Safe | Issue | Fix Priority |
|-----------|------------|-------------|-------|--------------|
| Episodic Memories | `ConcurrentLinkedDeque` | YES | Size check race | MEDIUM |
| Semantic Memories | `ConcurrentHashMap` | YES | None | N/A |
| Emotional Memories | `ArrayList` | NO | Concurrent sort | HIGH |
| Inside Jokes | `ArrayList` | NO | Concurrent add | MEDIUM |
| Working Memory | `ConcurrentLinkedDeque` | YES | Size check race | LOW |
| Vector Store | `ConcurrentHashMap` | YES | Iteration consistency | LOW |
| First Occurrences | `ConcurrentHashMap` | YES | None | N/A |
| Counters | `ConcurrentHashMap` | YES | Milestone race | MEDIUM |

---

## 4. Conversation Context Windowing

### 4.1 Current State: No Context Tracking

**Location:** `ConversationManager.java` lines 88-128

```java
private CompletableFuture<String> generateConversationalResponse(...) {
    String systemPrompt = CompanionPromptBuilder.buildConversationalSystemPrompt(memory);
    String userPrompt = CompanionPromptBuilder.buildConversationalUserPrompt(message, memory, minewright);

    // NO conversation history passed to LLM
    // Each message is processed independently
}
```

**Issue:** LLM has no context of previous messages in current conversation.

**Impact:**
- Can't maintain coherent multi-turn conversations
- Can't reference previous statements
- Loses track of what was just discussed

### 4.2 Recommended Implementation

#### Step 1: Add Conversation Turn Tracking

```java
public class ConversationManager {
    private final Deque<ConversationTurn> conversationHistory;
    private static final int MAX_CONVERSATION_HISTORY = 10;

    public static class ConversationTurn {
        public final String speaker; // "player" or "foreman"
        public final String message;
        public final Instant timestamp;
        public final String topic;  // Extracted topic

        public ConversationTurn(String speaker, String message, String topic) {
            this.speaker = speaker;
            this.message = message;
            this.topic = topic;
            this.timestamp = Instant.now();
        }

        public String formatForLLM() {
            return String.format("[%s]: %s", speaker, message);
        }
    }

    public CompletableFuture<String> generateConversationalResponse(
            String playerName, String message, AsyncLLMClient llmClient) {

        // Record player message
        String topic = extractTopic(message);
        conversationHistory.addFirst(new ConversationTurn("player", message, topic));

        // Trim if over limit
        while (conversationHistory.size() > MAX_CONVERSATION_HISTORY) {
            conversationHistory.removeLast();
        }

        // Build conversation history
        String history = buildConversationHistory();

        // Include in prompt
        String userPrompt = CompanionPromptBuilder.buildConversationalUserPrompt(
            message, memory, minewright, history  // ← Add history
        );

        return llmClient.sendAsync(userPrompt, params)
            .thenApply(response -> {
                String responseText = response.getContent().trim();

                // Record foreman response
                conversationHistory.addFirst(
                    new ConversationTurn("foreman", responseText, topic)
                );

                return responseText;
            });
    }

    private String buildConversationHistory() {
        // Build in chronological order (oldest first)
        List<ConversationTurn> chronological = new ArrayList<>(conversationHistory);
        Collections.reverse(chronological);

        StringBuilder sb = new StringBuilder();
        sb.append("Recent conversation:\n");

        for (ConversationTurn turn : chronological) {
            sb.append(turn.formatForLLM()).append("\n");
        }

        return sb.toString();
    }
}
```

#### Step 2: Update Prompt Builder

```java
public class CompanionPromptBuilder {
    public static String buildConversationalUserPrompt(
            String message, CompanionMemory memory, ForemanEntity minewright,
            String conversationHistory) {  // ← Add parameter

        StringBuilder prompt = new StringBuilder();

        // Add conversation history
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append(conversationHistory).append("\n");
        }

        // Add current message
        prompt.append("Player says: ").append(message).append("\n");

        // Add context
        prompt.append("\nContext:\n");
        prompt.append(memory.getRelationshipContext());
        prompt.append(memory.getWorkingMemoryContext());

        return prompt.toString();
    }
}
```

#### Step 3: Add Conversation Topic Tracking

```java
public class ConversationTopicTracker {
    private final Deque<String> recentTopics;
    private static final int MAX_TOPICS = 5;

    public void addTopic(String topic) {
        recentTopics.addFirst(topic);
        if (recentTopics.size() > MAX_TOPICS) {
            recentTopics.removeLast();
        }
    }

    public String getTopicSummary() {
        if (recentTopics.isEmpty()) {
            return "No recent topics";
        }

        return "Recent topics: " + String.join(", ", recentTopics);
    }

    public boolean isTopicChange(String newTopic) {
        if (recentTopics.isEmpty()) {
            return false;
        }

        String lastTopic = recentTopics.peekFirst();
        return !lastTopic.equalsIgnoreCase(newTopic);
    }
}
```

### 4.3 Advanced: Context Summarization

For long conversations, implement summarization:

```java
public class ConversationSummarizer {
    private static final int SUMMARY_THRESHOLD = 10;

    public String summarizeIfNeeded(List<ConversationTurn> history) {
        if (history.size() < SUMMARY_THRESHOLD) {
            return null;  // No summary needed
        }

        // Summarize older turns
        List<ConversationTurn> older = history.subList(
            history.size() / 2,
            history.size()
        );

        return generateSummary(older);
    }

    private String generateSummary(List<ConversationTurn> turns) {
        // Extract key topics
        Set<String> topics = turns.stream()
            .map(t -> t.topic)
            .collect(Collectors.toSet());

        // Count speakers
        Map<String, Long> speakerCounts = turns.stream()
            .collect(Collectors.groupingBy(
                t -> t.speaker,
                Collectors.counting()
            ));

        return String.format(
            "Earlier discussed: %s (mostly %s speaking)",
            topics,
            speakerCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown")
        );
    }
}
```

---

## 5. World Knowledge Caching Efficiency

### 5.1 Current Implementation: No Caching

**Location:** `WorldKnowledge.java` lines 29-74

```java
public WorldKnowledge(ForemanEntity minewright) {
    this.minewright = minewright;
    scan();  // ← Scans on every creation
}

private void scan() {
    scanBiome();
    scanBlocks();  // ← Iterates 16-block radius (~4096 blocks)
    scanEntities();
}

private void scanBlocks() {
    for (int x = -scanRadius; x <= scanRadius; x += 2) {
        for (int y = -scanRadius; y <= scanRadius; y += 2) {
            for (int z = -scanRadius; z <= scanRadius; z += 2) {
                // Check block state
                BlockPos checkPos = minewrightPos.offset(x, y, z);
                BlockState state = level.getBlockState(checkPos);
                // ...
            }
        }
    }
}
```

**Performance Issue:** Scans ~4096 blocks every time `WorldKnowledge` is created.

**Cost:** ~10-50ms per scan depending on chunk loading.

### 5.2 Recommended Caching Strategy

#### Option A: Temporal Caching (Simple)

```java
public class WorldKnowledge {
    private final ForemanEntity minewright;
    private final int scanRadius = 16;

    // Cached data
    private Map<Block, Integer> nearbyBlocks;
    private List<Entity> nearbyEntities;
    private String biomeName;

    // Cache management
    private Instant lastScanTime;
    private BlockPos lastScanPosition;
    private static final Duration CACHE_LIFETIME = Duration.ofSeconds(5);
    private static final int POSITION_CHANGE_THRESHOLD = 8;  // blocks

    public WorldKnowledge(ForemanEntity minewright) {
        this.minewright = minewright;
        this.lastScanTime = Instant.EPOCH;
        this.lastScanPosition = minewright.blockPosition();
        scan();
    }

    private boolean shouldRefreshCache() {
        Instant now = Instant.now();
        BlockPos currentPos = minewright.blockPosition();

        // Refresh if cache expired
        if (Duration.between(lastScanTime, now).compareTo(CACHE_LIFETIME) > 0) {
            return true;
        }

        // Refresh if moved significantly
        double distance = lastScanPosition.distSqr(currentPos);
        if (distance > POSITION_CHANGE_THRESHOLD * POSITION_CHANGE_THRESHOLD) {
            return true;
        }

        return false;
    }

    public String getNearbyBlocksSummary() {
        refreshIfNeeded();

        if (nearbyBlocks.isEmpty()) {
            return "none";
        }

        // ... existing code ...
    }

    private void refreshIfNeeded() {
        if (shouldRefreshCache()) {
            LOGGER.debug("Refreshing world knowledge cache");
            scan();
            lastScanTime = Instant.now();
            lastScanPosition = minewright.blockPosition();
        }
    }

    private void scan() {
        scanBiome();
        scanBlocks();
        scanEntities();
    }
}
```

#### Option B: Spatial Chunk Caching (Advanced)

```java
public class SpatialWorldCache {
    private final Map<ChunkPos, ChunkKnowledge> chunkCache;
    private static final int MAX_CHUNKS = 25;  // 5x5 area
    private static final Duration CHUNK_CACHE_LIFETIME = Duration.ofMinutes(5);

    public static class ChunkKnowledge {
        public final ChunkPos pos;
        public final Map<Block, Integer> blockCounts;
        public final String dominantBiome;
        public final Instant cachedAt;
        public final List<DiscoveredStructure> structures;

        public ChunkKnowledge(ChunkPos pos, Map<Block, Integer> blocks, String biome) {
            this.pos = pos;
            this.blockCounts = blocks;
            this.dominantBiome = biome;
            this.cachedAt = Instant.now();
            this.structures = new ArrayList<>();
        }

        public boolean isExpired() {
            return Duration.between(cachedAt, Instant.now())
                .compareTo(CHUNK_CACHE_LIFETIME) > 0;
        }
    }

    public ChunkKnowledge getChunkKnowledge(ChunkPos pos, Level level) {
        // Check cache
        ChunkKnowledge cached = chunkCache.get(pos);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        // Scan chunk
        ChunkKnowledge knowledge = scanChunk(pos, level);

        // Cache it
        chunkCache.put(pos, knowledge);
        evictIfOverLimit();

        return knowledge;
    }

    private void evictIfOverLimit() {
        while (chunkCache.size() > MAX_CHUNKS) {
            // Remove oldest or furthest chunk
            chunkCache.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().cachedAt))
                .ifPresent(entry -> {
                    chunkCache.remove(entry.getKey());
                    LOGGER.debug("Evicted chunk cache: {}", entry.getKey());
                });
        }
    }

    private ChunkKnowledge scanChunk(ChunkPos pos, Level level) {
        Map<Block, Integer> blockCounts = new HashMap<>();

        // Scan only this chunk
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();

                    if (block != Blocks.AIR) {
                        blockCounts.merge(block, 1, Integer::sum);
                    }
                }
            }
        }

        // Get biome
        BlockPos center = new BlockPos(minX + 8, 64, minZ + 8);
        Biome biome = level.getBiome(center).value();
        String biomeName = biomeToString(biome);

        return new ChunkKnowledge(pos, blockCounts, biomeName);
    }
}
```

### 5.3 Caching Performance Comparison

| Approach | Scan Time | Memory | Accuracy | Implementation |
|----------|-----------|--------|----------|----------------|
| **Current (No Cache)** | 10-50ms | 0KB | 100% | - |
| **Temporal Cache** | <1ms (cached) | ~50KB | 95% | LOW |
| **Spatial Chunk Cache** | <1ms (cached) | ~500KB | 98% | MEDIUM |
| **Hybrid (Both)** | <1ms (cached) | ~550KB | 98% | MEDIUM |

**Recommendation:** Start with temporal caching. Add spatial chunk caching if needed.

---

## 6. Memory Persistence Design

### 6.1 Current NBT Implementation

**Location:** `CompanionMemory.java` lines 691-837

**Strengths:**
- Complete serialization of all memory types
- Minecraft native format
- Works with world saves

**Limitations:**

#### Limitation 1: No Vector Store Persistence

**Issue:** `InMemoryVectorStore` has `saveToNBT()` but `CompanionMemory` doesn't call it.

**Location:** `CompanionMemory.java` saveToNBT() method doesn't save vector store.

```java
public void saveToNBT(CompoundTag tag) {
    // Saves all memories...
    // BUT: Does NOT save memoryVectorStore or memoryToVectorId

    // Missing:
    // memoryVectorStore.saveToNBT(tag);
}
```

**Impact:** On world reload, all embeddings must be regenerated (slow).

**Fix:** Add vector store persistence:

```java
public void saveToNBT(CompoundTag tag) {
    // ... existing code ...

    // Save vector store
    CompoundTag vectorStoreTag = new CompoundTag();
    memoryVectorStore.saveToNBT(vectorStoreTag);

    // Save memory-to-vector ID mapping
    ListTag mappingList = new ListTag();
    for (Map.Entry<EpisodicMemory, Integer> entry : memoryToVectorId.entrySet()) {
        CompoundTag mappingTag = new CompoundTag();
        mappingTag.putString("EventType", entry.getKey().eventType);
        mappingTag.putString("Description", entry.getKey().description);
        mappingTag.putLong("Timestamp", entry.getKey().timestamp.toEpochMilli());
        mappingTag.putInt("VectorId", entry.getValue());
        mappingList.add(mappingTag);
    }
    tag.put("MemoryVectorMapping", mappingList);
    tag.put("VectorStore", vectorStoreTag);
}

public void loadFromNBT(CompoundTag tag) {
    // ... existing code ...

    // Load vector store
    if (tag.contains("VectorStore")) {
        CompoundTag vectorStoreTag = tag.getCompound("VectorStore");
        memoryVectorStore.loadFromNBT(vectorStoreTag, vectorId -> {
            // Find memory by vector ID from mapping
            return findMemoryByVectorId(tag, vectorId);
        });

        // Rebuild memory-to-vector mapping
        loadMemoryVectorMapping(tag);
    }
}

private EpisodicMemory findMemoryByVectorId(CompoundTag tag, int vectorId) {
    ListTag mappingList = tag.getList("MemoryVectorMapping", 10);
    for (int i = 0; i < mappingList.size(); i++) {
        CompoundTag mappingTag = mappingList.getCompound(i);
        if (mappingTag.getInt("VectorId") == vectorId) {
            return new EpisodicMemory(
                mappingTag.getString("EventType"),
                mappingTag.getString("Description"),
                0,  // Not saved in mapping
                Instant.ofEpochMilli(mappingTag.getLong("Timestamp"))
            );
        }
    }
    return null;
}

private void loadMemoryVectorMapping(CompoundTag tag) {
    ListTag mappingList = tag.getList("MemoryVectorMapping", 10);
    for (int i = 0; i < mappingList.size(); i++) {
        CompoundTag mappingTag = mappingList.getCompound(i);

        EpisodicMemory memory = new EpisodicMemory(
            mappingTag.getString("EventType"),
            mappingTag.getString("Description"),
            0,
            Instant.ofEpochMilli(mappingTag.getLong("Timestamp"))
        );

        int vectorId = mappingTag.getInt("VectorId");
        memoryToVectorId.put(memory, vectorId);
    }
}
```

#### Limitation 2: NBT Size Constraints

**Issue:** NBT tags have practical size limits (~1MB per tag).

**Risk:** With 200 episodic memories + embeddings, NBT size can exceed limits.

**Calculation:**
- 200 memories × 200 bytes each = 40KB
- 200 embeddings × 384 floats × 4 bytes = 300KB
- Overhead: ~100KB
- **Total: ~440KB** (under limit, but close)

**Mitigation:**

```java
public class CompressedNBTIO {
    public static void saveCompressed(CompoundTag tag, OutputStream out) throws IOException {
        // Compress NBT data
        byte[] uncompressed = NbtIo.write(tag);
        byte[] compressed = compress(uncompressed);

        out.write(compressed);
    }

    public static CompoundTag loadCompressed(InputStream in) throws IOException {
        byte[] compressed = in.readAllBytes();
        byte[] uncompressed = decompress(compressed);

        return NbtIo.read(new ByteArrayInputStream(uncompressed));
    }

    private static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
            gzipOut.write(data);
        }
        return byteOut.toByteArray();
    }

    private static byte[] decompress(byte[] compressed) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (GZIPInputStream gzipIn = new GZIPInputStream(
                new ByteArrayInputStream(compressed))) {
            gzipIn.transferTo(byteOut);
        }
        return byteOut.toByteArray();
    }
}
```

#### Limitation 3: No Incremental Persistence

**Issue:** Entire memory saved on every world save.

**Impact:** Slow saves with large memories.

**Opportunity:** Implement incremental saves:

```java
public class IncrementalMemoryPersistence {
    private long lastSaveTime = 0;
    private final Set<String> modifiedKeys = ConcurrentHashMap.newKeySet();

    public void markModified(String key) {
        modifiedKeys.add(key);
    }

    public void saveIncremental(CompoundTag tag) {
        // Only save modified entries
        for (String key : modifiedKeys) {
            Object value = memoryStorage.get(key);
            if (value != null) {
                serializeValue(tag, key, value);
            }
        }

        lastSaveTime = System.currentTimeMillis();
        modifiedKeys.clear();
    }

    public boolean needsSave() {
        return !modifiedKeys.isEmpty();
    }
}
```

### 6.2 Persistence Performance Optimization

**Optimization 1: Lazy Loading**

```java
public class LazyLoadingCompanionMemory extends CompanionMemory {
    private boolean episodicMemoriesLoaded = false;
    private boolean semanticMemoriesLoaded = false;

    @Override
    public List<EpisodicMemory> getRecentMemories(int count) {
        ensureEpisodicMemoriesLoaded();
        return super.getRecentMemories(count);
    }

    private void ensureEpisodicMemoriesLoaded() {
        if (!episodicMemoriesLoaded) {
            // Load from disk/NBT
            loadEpisodicMemories();
            episodicMemoriesLoaded = true;
        }
    }
}
```

**Optimization 2: Async Persistence**

```java
public class AsyncMemoryPersistence {
    private final ExecutorService saveExecutor =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Memory-Save");
            t.setDaemon(true);
            return t;
        });

    public void saveAsync(CompoundTag tag, Consumer<Boolean> callback) {
        saveExecutor.submit(() -> {
            try {
                saveToNBT(tag);
                callback.accept(true);
            } catch (Exception e) {
                LOGGER.error("Async save failed", e);
                callback.accept(false);
            }
        });
    }
}
```

---

## 7. Relationship Milestone Tracking

### 7.1 Current Implementation Analysis

**Location:** `MilestoneTracker.java`

**Strengths:**
- Comprehensive milestone types (FIRST, ANNIVERSARY, COUNT, ACHIEVEMENT)
- Good persistence support
- Context-aware message generation

**Issues:**

#### Issue 1: No Milestone Expiration

**Problem:** Milestones never expire, even if no longer relevant.

**Example:** "First Diamond" remains significant even after finding 10,000 diamonds.

**Recommendation:** Add milestone freshness scoring:

```java
public class MilestoneFreshnessScorer {
    public double calculateFreshness(Milestone milestone, Instant now) {
        long daysSince = ChronoUnit.DAYS.between(milestone.achievedAt, now);

        switch (milestone.type) {
            case FIRST:
                // "First" milestones stay fresh longer
                return Math.max(0, 1.0 - (daysSince / 365.0));

            case ANNIVERSARY:
                // Anniversaries decay faster
                return Math.max(0, 1.0 - (daysSince / 30.0));

            case COUNT:
                // Count milestones decay moderately
                return Math.max(0, 1.0 - (daysSince / 90.0));

            case ACHIEVEMENT:
                // Achievements stay fresh longest
                return Math.max(0, 1.0 - (daysSince / 180.0));

            default:
                return 0.5;
        }
    }

    public List<Milestone> getFreshMilestones(List<Milestone> all, int limit) {
        Instant now = Instant.now();

        return all.stream()
            .sorted((a, b) -> Double.compare(
                calculateFreshness(b, now),
                calculateFreshness(a, now)
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

#### Issue 2: Limited Event Type Coverage

**Current tracked events (6 total):**
- `diamond_found`
- `nether_visit`
- `structure_built`
- `enemy_defeated`
- `night_survived`
- `gift_exchanged`

**Missing events:**
- Trading with villagers
- Enchanting items
- Brewing potions
- Finding rare biomes
- Taming animals
- Exploring new dimensions

**Recommendation:** Pluggable milestone system:

```java
public interface MilestonePlugin {
    String getPluginId();
    List<String> getSupportedEventTypes();
    Optional<Milestone> checkEvent(ForemanEntity foreman, String eventType, Object context);
}

public class MilestonePluginRegistry {
    private final Map<String, MilestonePlugin> plugins = new ConcurrentHashMap<>();

    public void registerPlugin(MilestonePlugin plugin) {
        plugins.put(plugin.getPluginId(), plugin);
        LOGGER.info("Registered milestone plugin: {}", plugin.getPluginId());
    }

    public Optional<Milestone> checkWithPlugins(
            ForemanEntity foreman, String eventType, Object context) {

        // Check built-in milestones first
        Optional<Milestone> builtin = checkBuiltInMilestone(foreman, eventType, context);
        if (builtin.isPresent()) {
            return builtin;
        }

        // Check plugins
        for (MilestonePlugin plugin : plugins.values()) {
            if (plugin.getSupportedEventTypes().contains(eventType)) {
                Optional<Milestone> pluginMilestone =
                    plugin.checkEvent(foreman, eventType, context);
                if (pluginMilestone.isPresent()) {
                    return pluginMilestone;
                }
            }
        }

        return Optional.empty();
    }
}
```

---

## 8. Priority Implementation Roadmap

### Phase 1: Critical Fixes (Week 1)

**Priority: CRITICAL**

1. **Fix Emotional Memory Thread Safety**
   - Wrap in synchronized block
   - Impact: Prevents crashes
   - Effort: 1 hour

2. **Add Semantic Memory Limit**
   - Cap at 500 entries
   - Implement eviction policy
   - Impact: Prevents memory leak
   - Effort: 2 hours

3. **Add Conversation Context Window**
   - Track last 10 turns
   - Include in LLM prompts
   - Impact: Better conversations
   - Effort: 4 hours

### Phase 2: Performance (Week 2)

**Priority: HIGH**

4. **Implement World Knowledge Caching**
   - Temporal cache (5 seconds)
   - Position-based refresh
   - Impact: 10-50ms saved per scan
   - Effort: 4 hours

5. **Add Embedding Cache Size Limit**
   - Cap at 1000 entries
   - Use Caffeine cache
   - Impact: Prevents memory leak
   - Effort: 2 hours

6. **Fix Vector Store Persistence**
   - Save embeddings to NBT
   - Rebuild mappings on load
   - Impact: Faster world loads
   - Effort: 4 hours

### Phase 3: Enhanced Features (Week 3-4)

**Priority: MEDIUM**

7. **Implement Importance-Based Eviction**
   - Score memories by importance
   - Evict low-importance first
   - Impact: Better memory retention
   - Effort: 8 hours

8. **Add Topic Extraction**
   - KeyBERT or simple NLP
   - Track conversation topics
   - Impact: Smarter conversations
   - Effort: 8 hours

9. **Replace Placeholder Embedding Model**
   - Integrate DJL + Sentence-BERT
   - Test semantic search
   - Impact: Working vector search
   - Effort: 16 hours

### Phase 4: Advanced Features (Week 5-6)

**Priority: LOW**

10. **Implement Spatial Memory**
    - Chunk-based caching
    - Resource discovery tracking
    - Impact: "Remember where" feature
    - Effort: 12 hours

11. **Add HNSW Vector Search**
    - Integrate HNSW library
    - Benchmark performance
    - Impact: Scalable search
    - Effort: 8 hours

12. **Implement Milestone Plugin System**
    - Pluggable milestones
    - Community extensions
    - Impact: Extensibility
    - Effort: 8 hours

---

## 9. Testing Recommendations

### 9.1 Memory Leak Tests

```java
@Test
public void testSemanticMemoryDoesNotLeak() {
    CompanionMemory memory = new CompanionMemory();

    // Add 1000 facts (over limit)
    for (int i = 0; i < 1000; i++) {
        memory.learnPlayerFact("test", "fact" + i, "value" + i);
    }

    // Should not exceed MAX_SEMANTIC_MEMORIES
    assertThat(memory.getSemanticMemoryCount())
        .isLessThan(CompanionMemory.MAX_SEMANTIC_MEMORIES);
}

@Test
public void testEmbeddingCacheDoesNotGrowUnbounded() {
    PlaceholderEmbeddingModel model = new PlaceholderEmbeddingModel();

    // Embed 10,000 unique texts
    for (int i = 0; i < 10_000; i++) {
        model.embed("unique text " + i);
    }

    // Cache should be capped
    assertThat(model.getCacheSize())
        .isLessThan(1000);
}
```

### 9.2 Thread Safety Tests

```java
@Test
public void testConcurrentMemoryRecording() throws InterruptedException {
    CompanionMemory memory = new CompanionMemory();
    int threadCount = 10;
    int memoriesPerThread = 100;

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        new Thread(() -> {
            try {
                startLatch.await();

                for (int i = 0; i < memoriesPerThread; i++) {
                    memory.recordExperience(
                        "test_event_" + threadId,
                        "description " + i,
                        5
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        }).start();
    }

    startLatch.countDown();
    endLatch.await(30, TimeUnit.SECONDS);

    // Verify memory integrity
    assertThat(memory.getEpisodicMemoryCount())
        .isLessThanOrEqualTo(CompanionMemory.MAX_EPISODIC_MEMORIES);
}
```

### 9.3 Conversation Context Tests

```java
@Test
public void testConversationHistoryIsMaintained() {
    ConversationManager manager = createTestManager();

    // Simulate conversation
    manager.processMessage("Player1", "Hello", mockLLM);
    manager.processMessage("Player1", "How are you?", mockLLM);
    manager.processMessage("Player1", "What's the weather?", mockLLM);

    // Verify context is maintained
    String lastResponse = manager.getLastResponse();
    assertThat(lastResponse)
        .contains("weather");  // Should reference topic
}
```

---

## 10. Conclusion

The memory system demonstrates solid architectural design but has several critical issues that need immediate attention:

### Critical Issues (Fix Immediately)

1. **Unbounded semantic memory growth** - Will cause memory leaks over time
2. **Emotional memory thread safety** - Will cause crashes in multiplayer
3. **Missing conversation context** - Results in poor conversational AI

### High Priority (Fix Soon)

4. **No world knowledge caching** - Wasteful scanning on every access
5. **Vector store not persisted** - Slow world loads, must regenerate embeddings
6. **Embedding cache unbounded** - Potential memory leak

### Implementation Strategy

**Week 1:** Fix critical issues (memory leaks, thread safety)
**Week 2:** Performance improvements (caching, persistence)
**Week 3-4:** Feature enhancements (topic extraction, real embeddings)
**Week 5-6:** Advanced features (spatial memory, HNSW)

With these improvements, the memory system will be production-ready and provide an engaging companion AI experience.

---

**Document Version:** 2.0
**Last Updated:** 2026-02-27
**Status:** Enhanced Analysis Complete
