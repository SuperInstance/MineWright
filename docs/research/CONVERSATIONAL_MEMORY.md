# Conversational Memory and Context Management Research

**Research Date:** 2026-02-27
**For:** Steve AI CompanionMemory Improvements
**Focus:** Sliding window vs summarization, vector-based retrieval, priority scoring, memory decay, relationship tracking

---

## Executive Summary

Modern conversational AI memory systems have evolved significantly in 2024-2025. The research reveals several key patterns:

1. **Hybrid architectures dominate** - Successful systems combine sliding windows, summarization, and vector retrieval
2. **Episodic memory is critical** - Recording specific events with timestamps and emotional weight creates richer relationships
3. **Memory importance scoring** - Not all memories are equal; dynamic priority prevents context bloat
4. **Relationship tracking matters** - Long-term rapport, trust, and shared experiences create persistent engagement

Our current `CompanionMemory` implementation has excellent foundations (episodic/semantic/emotional memories, vector search) but lacks several advanced features identified in this research.

---

## 1. Sliding Window vs Summarization

### Current Implementation
- **MAX_EPISODIC_MEMORIES = 200** - Hard cap with FIFO eviction
- **MAX_WORKING_MEMORY = 20** - Recent context buffer
- No automatic summarization of old memories

### Research Findings

| Approach | Pros | Cons | Best For |
|----------|------|------|----------|
| **Sliding Window** | Simple, low latency, preserves details | Loses old context, fixed size limit | Recent conversations, active sessions |
| **Summarization** | Preserves key information, reduces tokens | Can lose nuance, async processing needed | Long-term memory, cross-session context |
| **Hybrid (Recommended)** | Balances detail and overview | More complex | Production systems |

**Key Insight:** Context should be treated as a finite resource with diminishing marginal returns. Bigger context windows don't guarantee better results.

### Recommended Improvements for CompanionMemory

1. **Tiered Memory System**
   ```java
   - Working Memory (20 entries) -> Immediate context
   - Short-term Episodic (200 entries) -> Current session
   - Long-term Summarized -> Condensed memories
   - Core Facts -> Semantic memory (player preferences)
   ```

2. **Progressive Summarization**
   ```java
   // When episodic memory exceeds threshold:
   // 1. Group memories by topic/time period
   // 2. Generate summary using LLM
   // 3. Store summary in long-term storage
   // 4. Keep most significant episodic memories intact
   ```

3. **Sliding Window with Overlap**
   ```java
   // Instead of pure FIFO:
   // - Keep last 10 entries always available
   // - Sample from middle (10% every 10 entries)
   // - Keep first 5 entries (session start)
   // This preserves conversation boundaries
   ```

---

## 2. Vector-Based Memory Retrieval

### Current Implementation
- `InMemoryVectorStore` with `PlaceholderEmbeddingModel`
- `findRelevantMemories(String query, int k)` for semantic search
- Fallback to keyword matching if vector search fails

### Research Findings

**Memory-Augmented Retrieval** is the leading approach:

1. **Historical context as query weights** - Past conversation "checkpoints" influence retrieval relevance
2. **Three-layer architecture**: Memory + Retrieval + Tools
3. **Vector databases** (Redis, Pinecone, Weaviate) for production scale

**Best Practices:**
- Use conversation history checkpoints to boost retrieval relevance
- Combine semantic search with temporal recency
- Re-rank results using hybrid scoring (similarity + recency + importance)

### Recommended Improvements

1. **Replace Placeholder Embedding Model**
   ```java
   // Options:
   - Local: Sentence-BERT (SBERT)
   - API: OpenAI text-embedding-3-small
   - Lightweight: all-MiniLM-L6-v2
   ```

2. **Hybrid Scoring for Retrieval**
   ```java
   float relevanceScore = cosineSimilarity(query, memory) * 0.5;
   float recencyScore = timeDecay(memory.timestamp, hours=24) * 0.3;
   float importanceScore = memory.emotionalWeight / 10.0 * 0.2;
   float combinedScore = relevanceScore + recencyScore + importanceScore;
   ```

3. **Query Expansion with Context**
   ```java
   // When searching for "mining":
   // - Expand with "ores", "caves", "pickaxe"
   // - Include player preferences (preferred ore types)
   // - Boost recent mining-related memories
   ```

---

## 3. Priority Scoring for Context Inclusion

### Current Implementation
- Emotional weight (-10 to +10) stored but limited use
- FIFO eviction doesn't consider importance
- No dynamic prioritization for LLM context building

### Research Findings

**8 Major AI Memory Optimization Strategies** (2024):

1. Full Memory (baseline)
2. Memory compression
3. Sliding window
4. **Hierarchical memory** (short/long-term)
5. **Vector-based retrieval**
6. Knowledge graphs
7. Multi-tier caching
8. **AgentRAG** (retrieval with agent state)

**Priority Signals:**
- Recency (exponential decay)
- Emotional weight/importance
- Query relevance (dynamic)
- Relationship milestones (first meeting, shared success)
- Frequency of reference

### Recommended Improvements

1. **Dynamic Memory Scoring**
   ```java
   public float computeMemoryScore(EpisodicMemory memory, Instant now) {
       // Time decay (half-life of 7 days)
       float ageScore = (float) Math.exp(-ChronoUnit.DAYS.between(memory.timestamp, now) / 7.0);

       // Emotional importance
       float importanceScore = Math.min(1.0f, Math.abs(memory.emotionalWeight) / 10.0f);

       // Access frequency
       float accessScore = Math.min(1.0f, memory.accessCount / 10.0f);

       return ageScore * 0.4f + importanceScore * 0.4f + accessScore * 0.2f;
   }
   ```

2. **Smart Eviction Policy**
   ```java
   // Instead of FIFO:
   // 1. Compute scores for all memories
   // 2. Evict lowest-scoring when over capacity
   // 3. Never evict memories with weight >= 8
   // 4. Always keep "first_meeting" and milestone memories
   ```

3. **Context Building with Prioritization**
   ```java
   public String buildLLMContext(String query, int maxTokens) {
       List<ScoredMemory> candidates = new ArrayList<>();

       // Always include: first meeting, recent context
       // Add: high-importance memories
       // Add: semantically relevant memories (top-k from vector search)
       // Sort by combined score
       // Truncate to maxTokens
   }
   ```

---

## 4. Memory Decay and Importance

### Current Implementation
- No explicit decay mechanism
- Emotional weight static after creation
- No memory consolidation

### Research Findings

**Memory Decay Patterns:**
- **Exponential decay** is most natural (matches human memory)
- **Spaced repetition** strengthens important memories
- **Sleep consolidation** - important events become stronger over time
- **Emotional memories resist decay**

**MemGPT Approach (2024):**
- Simulates human memory hierarchy
- Working memory (limited, fast)
- Episodic memory (unlimited, slower)
- Semantic memory (facts, generalized)
- Explicit memory management operations

### Recommended Improvements

1. **Exponential Time Decay**
   ```java
   public float getTimeDecay(Instant timestamp, Instant now) {
       long hours = ChronoUnit.HOURS.between(timestamp, now);
       double halfLife = 168.0; // 7 days
       return (float) Math.pow(0.5, hours / halfLife);
   }
   ```

2. **Memory Consolidation**
   ```java
   // Run weekly or when memory count exceeds threshold:
   // 1. Group memories by topic
   // 2. If group has > 5 similar memories:
   //    - Generate summary
   //    - Store as semantic fact
   //    - Keep only most emotional episodic memory
   ```

3. **Importance Evolution**
   ```java
   // When a memory is referenced:
   memory.accessCount++;
   memory.lastAccessed = Instant.now();
   // Increase importance if recently accessed
   if (accessWasSignificant) {
       memory.emotionalWeight = Math.min(10, memory.emotionalWeight + 1);
   }
   ```

---

## 5. Relationship Tracking Over Time

### Current Implementation
- **Excellent foundation**: rapport (0-100), trust (0-100), interactionCount
- `Relationship` class with `Mood` enum
- `MilestoneTracker` for relationship milestones
- First meeting timestamp, days known

### Research Findings

**Amazon Bedrock AgentCore Memory (Dec 2025):**
- Records context, reasoning processes, operations, results
- Analyzes patterns to optimize future decisions
- Structured episodic memory

**Google's Reasoning Memory Framework:**
- Accumulates reasoning experiences
- Generalizes patterns
- Reuses for self-improvement

**Key Relationship Metrics:**
- **Rapport**: Overall warmth and connection
- **Trust**: Reliability and dependability
- **Shared experiences**: Co-op tasks, successes, failures
- **Communication patterns**: Topic preferences, humor
- **Reciprocity**: Back-and-forth engagement

### Recommended Improvements

1. **Relationship Milestone Auto-Detection**
   ```java
   public void checkMilestones() {
       if (interactionCount.get() == 10) {
           milestoneTracker.record("getting_to_know", "After 10 interactions");
       }
       if (rapportLevel.get() >= 50 && !hasMilestone("friends")) {
           milestoneTracker.record("friends", "Reached rapport 50");
       }
       if (getDaysKnown() >= 7 && interactionCount.get() >= 50) {
           milestoneTracker.record("week_buddy", "Known for a week with frequent interactions");
       }
   }
   ```

2. **Dynamic Personality Response**
   ```java
   // Personality should evolve based on relationship:
   // - High rapport -> more casual, more humor
   // - Low trust -> more formal, cautious
   // - Shared successes -> more confident
   public void updatePersonalityFromRelationship() {
       if (rapportLevel.get() > 70) {
           personality.formality = Math.max(0, personality.formality - 5);
           personality.humor = Math.min(100, personality.humor + 5);
       }
   }
   ```

3. **Interaction Pattern Learning**
   ```java
   // Track patterns:
   // - Typical session length
   // - Preferred activities
   // - Conversation topics frequency
   // - Response time expectations
   public void recordInteractionPattern(String type, int duration) {
       sessionMetrics.computeIfAbsent(type, k -> new SessionMetrics())
                     .record(duration);
   }
   ```

---

## 6. Episodic Memory Best Practices

### Current Implementation
- `EpisodicMemory` class with eventType, description, emotionalWeight, timestamp
- Stored in `ConcurrentLinkedDeque` for efficient add/remove
- Added to vector store for semantic search

### Research Findings

**Episodic Memory in AI Agents (2024-2025):**

1. **Event Granularity Matters**
   - Too coarse: "Played together" (useless)
   - Too fine: "Moved block at x,y,z" (noisy)
   - Just right: "Built a tower together, fell off, laughed" (action + outcome + emotion)

2. **Narrative Structure**
   - Events should form coherent stories
   - Cause and effect relationships
   - Character development (both player and companion)

3. **Memory Clustering**
   - Group related events into "chapters"
   - Identify themes (building, exploring, combat)
   - Track progress over time

### Recommended Improvements

1. **Richer Event Types**
   ```java
   // Current: eventType as String
   // Better: Structured event types
   public enum EventType {
       FIRST_MEETING, SHARED_SUCCESS, SHARED_FAILURE,
       BUILDING_TOGETHER, EXPLORING, COMBAT,
       CONVERSATION, JOKE_SHARED, MILESTONE_REACHED
   }
   ```

2. **Memory Relationships**
   ```java
   public class EpisodicMemory {
       // ... existing fields ...
       public List<String> relatedMemoryIds; // Links to related events
       public String chapter; // "The Great Castle Project"
       public List<String> participants; // Multi-player support
   }
   ```

3. **Story Extraction**
   ```java
   public List<Story> extractStories() {
       // Group memories by time and topic
       // Identify narrative arcs
       // Return coherent stories for prompting
   }
   ```

---

## 7. Conversation Summarization Techniques

### Current Implementation
- No automatic summarization
- Manual description in episodic memories

### Research Findings

**Summarization Approaches:**

| Technique | Description | Complexity | Quality |
|-----------|-------------|------------|---------|
| **Extractive** | Select key sentences | Low | Medium |
| **Abstractive (LLM)** | Generate new summary | Medium | High |
| **Running Summary** | Cumulative summary | Medium | High |
| **Hierarchical** | Summary of summaries | High | Very High |

**Azure Language Service Features:**
- Recap (one-paragraph summary)
- Issues & Resolutions
- Chapter Titles
- Narrative (detailed notes)
- Follow-up Tasks

**Key Challenges in Conversations:**
- Informal language, slang, interruptions
- Scattered information
- Flexible discourse structure
- Multi-party dynamics

### Recommended Improvements

1. **Session Summarization**
   ```java
   public String summarizeSession(List<EpisodicMemory> sessionMemories) {
       // Group by topic
       // Identify emotional arc
       // Extract key moments
       // Generate summary using LLM
       // Store as semantic memory
   }
   ```

2. **Chapter Creation**
   ```java
   public class Chapter {
       String title; // "The Day We Built the Castle"
       Instant startTime, endTime;
       List<EpisodicMemory> memories;
       String summary;
       String emotionalTone; // "triumphant", "frustrating", "relaxing"
   }
   ```

3. **Async Summarization**
   ```java
   // Don't block on summarization:
   CompletableFuture.supplyAsync(() -> summarizeSession(memories))
                    .thenAccept(this::storeSummary);
   ```

---

## 8. Implementation Priority Matrix

| Feature | Impact | Effort | Priority |
|---------|--------|--------|----------|
| Dynamic memory scoring + smart eviction | High | Medium | **P0** |
| Hybrid context building (summary + episodic) | High | Medium | **P0** |
| Real embedding model (replace placeholder) | High | Low | **P0** |
| Time decay implementation | Medium | Low | **P1** |
| Memory consolidation (summarization) | Medium | High | **P1** |
| Relationship milestone auto-detection | Medium | Low | **P1** |
| Chapter/story extraction | Medium | High | **P2** |
| Session summarization | Low | Medium | **P2** |
| Multi-player memory tracking | Low | High | **P3** |

---

## 9. Code Examples

### Smart Context Builder

```java
public String buildOptimizedContext(String query, int maxTokens) {
    List<ScoredMemory> scored = new ArrayList<>();

    // 1. Always include first meeting (if exists)
    episodicMemories.stream()
        .filter(m -> "first_meeting".equals(m.eventType))
        .findFirst()
        .ifPresent(m -> scored.add(new ScoredMemory(m, 1000.0f)));

    // 2. Recent working memory (high priority)
    workingMemory.stream()
        .limit(5)
        .forEach(m -> scored.add(new ScoredMemory(m, 500.0f)));

    // 3. High-emotional memories
    episodicMemories.stream()
        .filter(m -> Math.abs(m.emotionalWeight) >= 7)
        .forEach(m -> {
            float score = computeMemoryScore(m, Instant.now());
            scored.add(new ScoredMemory(m, score * 2.0f));
        });

    // 4. Semantically relevant memories
    List<EpisodicMemory> relevant = findRelevantMemories(query, 10);
    relevant.forEach(m -> {
        float score = computeMemoryScore(m, Instant.now());
        scored.add(new ScoredMemory(m, score));
    });

    // Sort and truncate
    scored.sort((a, b) -> Float.compare(b.score, a.score));

    // Build context, respecting token limit
    StringBuilder context = new StringBuilder();
    int tokens = 0;
    for (ScoredMemory sm : scored) {
        String text = sm.memory.toContextString();
        int estimatedTokens = text.length() / 4;
        if (tokens + estimatedTokens > maxTokens) break;
        context.append(text).append("\n");
        tokens += estimatedTokens;
    }

    return context.toString();
}
```

### Memory Consolidation Service

```java
public class MemoryConsolidationService {
    private static final int CONSOLIDATION_THRESHOLD = 50;
    private static final long CONSOLIDATION_INTERVAL_DAYS = 7;

    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkConsolidationNeeded() {
        Instant lastCheck = getLastConsolidation();
        long daysSince = ChronoUnit.DAYS.between(lastCheck, Instant.now());

        if (daysSince >= CONSOLIDATION_INTERVAL_DAYS ||
            episodicMemories.size() > CONSOLIDATION_THRESHOLD) {
            consolidateMemories();
        }
    }

    private void consolidateMemories() {
        // 1. Group by topic and time period
        Map<String, List<EpisodicMemory>> groups = groupMemories();

        // 2. For each group with many memories
        for (Map.Entry<String, List<EpisodicMemory>> entry : groups.entrySet()) {
            if (entry.getValue().size() >= 5) {
                consolidateGroup(entry.getKey(), entry.getValue());
            }
        }
    }

    private void consolidateGroup(String topic, List<EpisodicMemory> memories) {
        // Generate summary
        String summary = llmClient.summarizeMemories(memories);

        // Extract semantic facts
        List<SemanticMemory> facts = extractFacts(memories);

        // Keep most emotional episodic memory
        EpisodicMemory mostEmotional = memories.stream()
            .max(Comparator.comparingInt(m -> Math.abs(m.emotionalWeight)))
            .orElse(null);

        // Update stores
        semanticMemories.putAll(toMap(facts));
        episodicMemories.removeAll(memories);
        if (mostEmotional != null) {
            episodicMemories.add(mostEmotional);
        }
    }
}
```

---

## 10. Sources

- [2024 AI Native Application Working Memory Research](https://m.blog.csdn.net/2502_91678797/article/details/148723905)
- [8 Major AI Memory Optimization Strategies](https://developer.aliyun.com/article/1674328)
- [AI Memory Technology Deep Dive](https://m.blog.csdn.net/enjoyedu/article/details/156850794)
- [MemGPT - Teaching LLMs Memory Management](https://m.blog.csdn.net/gitblog_00231/article/details/151458204)
- [LLM Context Window Optimization](https://blog.csdn.net/ghgjvhjklkjkss/article/details/154209972)
- [Claude Context Window Management](https://blog.csdn.net/2401_85325726/article/details/149833986)
- [Memory-Augmented Retrieval](https://m.blog.csdn.net/weixin_41455464/article/details/156655583)
- [Retrieval-Augmented Agent Design](https://m.blog.csdn.net/qq_qingtian/article/details/155191579)
- [RAG with Spring AI](https://m.blog.csdn.net/TheChosenOnev/article/details/155862346)
- [Hierarchical Memory in AI Agents](https://m.163.com/dy/article/KFF9MGJA0556DR95.html)
- [AI Agent Memory Mechanisms Overview](https://m.blog.csdn.net/m0_57545130/article/details/157386487)
- [Truly Strong Agents Grow Layered Memory Systems](https://juejin.cn/post/7611006514891997203)

---

## Appendix: Current CompanionMemory Analysis

### Strengths
1. Excellent multi-type memory architecture (episodic, semantic, emotional, conversational, working)
2. Vector search foundation with InMemoryVectorStore
3. Relationship tracking with rapport, trust, mood
4. MilestoneTracker for significant moments
5. NBT persistence for world save/load
6. PersonalityProfile with Big Five traits
7. Inside joke tracking with reference counting
8. Thread-safe concurrent collections

### Gaps Identified
1. Placeholder embedding model needs real implementation
2. FIFO eviction is suboptimal (no importance consideration)
3. No time decay mechanism
4. No automatic summarization/consolidation
5. Context building not optimized for token efficiency
6. Static emotional weights (no evolution)
7. No memory relationship tracking
8. Limited session-level summarization

### Next Steps
1. Implement P0 features from priority matrix
2. Replace PlaceholderEmbeddingModel with SBERT or API-based model
3. Add smart eviction with dynamic scoring
4. Implement time decay for memory scoring
5. Add async memory consolidation service
