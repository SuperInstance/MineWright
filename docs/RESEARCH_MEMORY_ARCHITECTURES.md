# Research Report: Memory Architectures for AI Agents in Game NPCs

**Project:** MineWright (MineWright AI - "Cursor for Minecraft")
**Date:** 2026-02-27
**Focus:** Advanced memory systems for autonomous AI agents in Minecraft

---

## Executive Summary

This report researches state-of-the-art memory architectures for AI agents, with specific focus on applications in game NPCs and autonomous agents like MineWright. The current MineWright implementation provides a solid foundation with episodic, semantic, emotional, and working memory types, plus vector search capabilities. This report identifies specific improvements and advanced patterns from production AI systems that can enhance the companion experience.

### Key Findings

1. **Memory Architecture Evolution**: Modern AI agents have evolved from simple RAG (read-only) to full Agent Memory with read+write capabilities, enabling agents to learn from experience
2. **Vector Search Trade-offs**: HNSW provides superior performance for <1M vectors (typical for single NPC), while IVF becomes advantageous at >10M vectors
3. **Memory Consolidation Critical**: Production systems implement importance scoring and forgetting curves to manage infinite memory growth
4. **Game-Specific Patterns**: Minecraft-based research shows Place-Event Memory (PEM) systems excel at spatial-temporal memory for autonomous agents

---

## Table of Contents

1. [Memory Types - Deep Dive](#1-memory-types---deep-dive)
2. [Vector Search - Technical Analysis](#2-vector-search---technical-analysis)
3. [Memory Consolidation Strategies](#3-memory-consolidation-strategies)
4. [Context Integration & RAG Patterns](#4-context-integration--rag-patterns)
5. [Persistence & Serialization](#5-persistence--serialization)
6. [Recommendations for MineWright](#6-recommendations-for-minewright)
7. [Implementation Roadmap](#7-implementation-roadmap)

---

## 1. Memory Types - Deep Dive

### 1.1 Cognitive Psychology Foundation

Modern AI agent memory architectures draw from cognitive psychology, identifying three to four core memory types:

| Memory Type | Human Analogy | AI Agent Implementation | Current MineWright Status |
|-------------|---------------|------------------------|---------------------------|
| **Working/Short-term Memory** | Shopping list, immediate context | Context window, recent interaction buffer | ✅ Implemented (`WorkingMemoryEntry`, 20-item limit) |
| **Semantic Memory** | Knowing "Paris is France's capital" | Vector stores, knowledge graphs, user profiles | ✅ Implemented (`SemanticMemory`, vector search) |
| **Episodic Memory** | Remembering your first day of school | Event logs with timestamps, emotional weights | ✅ Implemented (`EpisodicMemory`, 200-item limit) |
| **Procedural Memory** | Knowing how to ride a bike | System prompts, workflow rules, skill embeddings | ⚠️ Partial (via prompt builder, not explicit) |
| **Emotional Memory** | Remembering how you felt at graduation | Weighted events, mood tracking | ✅ Implemented (`EmotionalMemory`, separate from episodic) |

### 1.2 Production Memory Architecture (2025-2026 State of Art)

#### Three-Layer Architecture (Production-Ready)

```
┌─────────────────────────────────────────────────────────┐
│                   WORKING MEMORY                         │
│  • Sliding window (20-50 items)                          │
│  • Filters recent interactions for context              │
│  • Fast access, auto-evicted by LRU                     │
└─────────────────────────────────────────────────────────┘
                           ↓ (consolidation)
┌─────────────────────────────────────────────────────────┐
│                   EPISODIC MEMORY                        │
│  • Immutable behavioral logs ("black box")               │
│  • What happened, when, emotional weight                │
│  • Summary: Compressed into semantic memory             │
└─────────────────────────────────────────────────────────┘
                           ↓ (abstraction)
┌─────────────────────────────────────────────────────────┐
│                   SEMANTIC MEMORY                        │
│  • Vector-based retrieval (HNSW/IVF)                     │
│  • Factual knowledge, player preferences                │
│  • Multi-sector embeddings (episodic, semantic, etc.)    │
└─────────────────────────────────────────────────────────┘
```

**Key Insight:** Modern systems treat episodic memory as immutable (audit trail) while semantic memory is the abstraction layer for efficient retrieval.

### 1.3 Multi-Sector Memory (Advanced Pattern)

OpenMemory framework proposes multi-sector embeddings beyond the basic three types:

| Memory Sector | Purpose | Example Content | MineWright Opportunity |
|---------------|---------|-----------------|----------------------|
| **Episodic** | Specific events | "Built a house together on day 5" | ✅ Current implementation |
| **Semantic** | Factual knowledge | "Player prefers stone houses" | ✅ Current implementation |
| **Procedural** | Skills & workflows | "How to build spiral staircase" | ⚠️ Add skill embeddings |
| **Emotional** | Feeling states | "Player was sad when dog died" | ✅ Current implementation |
| **Reflective** | Meta-learning | "We work better when player leads" | ❌ Not implemented |
| **Spatial** | Location knowledge | "Diamonds found at y=-58" | ⚠️ Partial via `WorldKnowledge` |

### 1.4 Place-Event Memory (PEM) for Game Agents

**Source:** [MrSteve: Instruction-Following Agents in Minecraft](https://arxiv.org/abs/2411.06736)

Research specifically for Minecraft agents introduces **Place-Event Memory (PEM)**:

- **What**: Captures *what* happened, *where* it happened, and *when*
- **Why**: Enables efficient recall and navigation in long-horizon tasks
- **Structure**: Spatial + temporal indexing for memory queries
- **Application**: Agents can recall "we found diamonds near the lava lake at coordinates (x,y,z)"

**MineWright Enhancement:** Current `WorldKnowledge` scans nearby blocks but doesn't persist location-specific memories. PEM would enable:
- "Remember that cave system with diamonds?"
- "Let's go back to the village we visited yesterday"
- "The last time we were in this biome, we found..."

---

## 2. Vector Search - Technical Analysis

### 2.1 HNSW vs IVF - Performance Comparison

| Algorithm | Query Latency | Recall Rate | Memory Usage | Build Speed | Best Use Case |
|-----------|---------------|-------------|--------------|-------------|---------------|
| **HNSW** | Low (μs-level) | High (95-98%+) | High (2-3x IVF) | Medium | High precision, low latency |
| **IVF** | Medium | Medium-High (85-95%) | Low | Fast | Large-scale, memory-constrained |
| **LSH** | Medium | Low (70-85%) | Low | Fast | Quick approximate matching |

**Source:** [IVF与HNSW：高维向量搜索的双子星](https://blog.51cto.com/u_16555391/13912636)

### 2.2 Recommendation for MineWright

**Current Situation:**
- Single NPC per player (typical)
- Estimated memory count: <10,000 episodic memories
- Vector dimension: 384 (placeholder)

**Recommendation: Use HNSW**

**Rationale:**
1. **Data Scale**: <1M vectors = HNSW sweet spot
2. **Latency Critical**: Real-time conversation requires μs-level response
3. **Recall Priority**: High recall prevents "forgetting" important shared experiences
4. **Memory Not Constraint**: 2-3x memory overhead is trivial for <10K vectors

**Current Implementation:** `InMemoryVectorStore` uses brute-force cosine similarity (O(n) per query).

**Improvement Required:** Implement HNSW indexing for O(log n) queries.

### 2.3 Embedding Model Selection

#### Models by Use Case

| Model Type | Dimension | Speed | Quality | Cost | MineWright Fit |
|------------|-----------|-------|---------|------|----------------|
| **all-MiniLM-L6-v2** | 384 | Fast | Good | Free | ✅ Current placeholder |
| **all-mpnet-base-v2** | 768 | Medium | Excellent | Free | ⭐ Recommended |
| **e5-large-v2** | 1024 | Slow | Excellent | Free | Overkill |
| **OpenAI text-embedding-3-small** | 1536 | Medium | Excellent | Paid | ❌ Requires API |

**Recommendation:**

1. **Default**: `all-mpnet-base-v2` (768-dim, best quality open-source)
2. **Performance Mode**: `all-MiniLM-L6-v2` (384-dim, faster)
3. **Future**: Consider quantized embeddings (4-bit) for memory efficiency

#### Chunking Strategies

**For Episodic Memory:**
```
Event: "build_house" → Embedding text:
"build:house At day 5, we built a cozy 3-story stone house together.
The player designed the layout and I placed the blocks. It has a
glass roof and overlooks the eastern ocean. We felt proud seeing it finished."
```

**Best Practice:**
- Include event type, time, description, emotional context
- Target: 50-100 tokens per embedding (captures context without noise)
- For longer events, create multiple embeddings with overlap

### 2.4 HNSW Configuration for MineWright

```java
// Recommended HNSW parameters
public class HNSWConfig {
    // Bi-directional link count (higher = better recall, more memory)
    int M = 16;  // Default for 768-dim embeddings

    // Search depth (higher = better recall, slower)
    int efConstruction = 200;  // Build-time quality
    int efSearch = 50;         // Query-time depth

    // Distance metric
    DistanceMetric metric = DistanceMetric.COSINE;
}
```

**Performance Estimates:**
- Index build: ~100ms for 10K vectors
- Query time: ~1-2ms for top-10 results
- Memory overhead: ~2x base vectors (10K × 768 × 4 bytes × 2 ≈ 60MB)

---

## 3. Memory Consolidation Strategies

### 3.1 The Memory Growth Problem

**Challenge:** Without consolidation, memory grows infinitely:
- Day 1: 100 memories
- Day 30: 3,000 memories
- Day 365: 36,500 memories
- Query latency degrades: O(n) → 365× slower

**Solution:** Memory consolidation with importance scoring and selective forgetting.

### 3.2 Importance Scoring Framework

Based on research from [Active Memory Management](https://arxiv.org/abs/2508.13171v1), importance should be multi-factor:

```python
importance_score = (
    emotional_weight * 0.3 +           # Emotional impact (-10 to +10)
    recency_bonus * 0.2 +               # Newer memories ranked higher
    access_frequency * 0.2 +            # How often retrieved
    task_relevance * 0.2 +              # Connection to current goals
    relationship_significance * 0.1     # First-time events, milestones
)
```

**Current MineWright Implementation:**
- ✅ Emotional weight tracking
- ✅ Recency (via FIFO queue)
- ❌ No access frequency tracking
- ❌ No task relevance scoring
- ❌ No explicit relationship significance

### 3.3 Forgetting Curves - Mathematical Model

**Source:** Memory consolidation research

**Exponential Decay Model:**
```
R = e^(-t/S)

Where:
  R = retention probability (0-1)
  t = time elapsed (days)
  S = memory strength (default: 1, +1 per recall)
```

**Forgetting Threshold:** When R < 0.3, memory is archived or deleted.

**Example:**
- Memory strength S=1, after 30 days: R = e^(-30/1) ≈ 0 (forgotten)
- Memory strength S=5, after 30 days: R = e^(-30/5) ≈ 0.002 (forgotten)
- Memory strength S=30, after 30 days: R = e^(-30/30) ≈ 0.37 (retained!)

**Key Insight:** Each recall increases memory strength, making important memories persistent.

### 3.4 Consolidation Pipeline

**Three-Stage Process:**

```
┌─────────────────────────────────────────────────────────┐
│  STAGE 1: IMPORTANCE SCORING (Every hour)               │
│  • Calculate importance for all memories                 │
│  • Identify bottom 10% for candidate removal             │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  STAGE 2: SUMMARY GENERATION (For important old memories)│
│  • For memories >30 days old with high importance       │
│  • Generate compressed summary via LLM                  │
│  • Store summary in semantic memory                      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  STAGE 3: FORGETTING (Apply decay formula)              │
│  • Calculate retention probability R                    │
│  • If R < threshold: archive or delete                   │
│  • Milestone memories: never delete (manual override)    │
└─────────────────────────────────────────────────────────┘
```

### 3.5 Memory Summarization

**When:** Memories older than 30 days with importance score > 7

**Process:**
```
Original (10 episodic memories):
- "build_house_1: Built starter house at day 5"
- "build_house_2: Added second floor at day 6"
- "build_house_3: Added glass roof at day 7"
- ... (7 more house-building memories)

→ Summarized via LLM →
"Player and I built our first home together over the first week.
It evolved from a simple shelter to a 3-story stone house with
glass roof overlooking the eastern ocean. This project established
our working partnership and the player's preference for stone
aesthetics with ocean views."

→ Stored as Semantic Memory:
{
  category: "preference",
  key: "building_style",
  value: "stone structures with ocean views",
  confidence: 0.9,
  source: "summarized from 10 episodic memories"
}
```

**Benefits:**
- Reduces episodic memory count: 10 → 1
- Preserves semantic knowledge: "player likes stone + ocean views"
- Frees up context window for new memories

---

## 4. Context Integration & RAG Patterns

### 4.1 RAG vs Agent Memory - Evolution

**Traditional RAG (Read-Only):**
```
Query → Vector DB → Retrieve relevant docs → Inject into prompt → Generate
```

**Agent Memory (Read+Write):**
```
Experience → Extract key info → Write to memory
Query → Vector DB → Retrieve relevant memories → Inject into prompt → Generate → Update memory based on interaction
```

**Key Difference:** Agents LEARN from interactions, RAG systems only RETRIEVE.

**Source:** [赛博纺织工厂：AI Agent 工作的四次进化](https://juejin.cn/post/7611064285586440219)

### 4.2 Memory Injection Points in Prompts

**Current MineWright Implementation:** `CompanionPromptBuilder.buildConversationalSystemPrompt()`

**Injection Order Matters:**

```
1. SYSTEM IDENTITY (who you are)
   ↓
2. PERSONALITY (how you behave)
   ↓
3. RELATIONSHIP STATUS (rapport, trust, history)
   ↓
4. RELEVANT MEMORIES (semantic search results) ← RAG injection
   ↓
5. RECENT CONTEXT (working memory)
   ↓
6. CURRENT SITUATION (biome, time, nearby entities)
   ↓
7. USER INPUT (what player said)
```

**Best Practice:** Inject memories AFTER personality but BEFORE recent context. This allows memories to influence how recent events are interpreted.

### 4.3 Recency vs Importance - Balancing Act

**Problem:** Recent memories may not be relevant. Important memories may be old.

**Solution:** Hybrid retrieval strategy

```java
public List<Memory> retrieveMemories(String query, int k) {
    // 40% from semantic similarity (importance)
    List<Memory> semanticResults = vectorStore.search(query, k * 0.4);

    // 30% from recent working memory (recency)
    List<Memory> recentResults = workingMemory.getMostRecent(k * 0.3);

    // 20% from high-emotional-weight (emotional)
    List<Memory> emotionalResults = emotionalMemories.getTop(k * 0.2);

    // 10% from milestones (relationship)
    List<Memory> milestoneResults = milestones.getRecent(k * 0.1);

    // Merge and deduplicate, then re-rank by composite score
    return mergeAndRerank(semanticResults, recentResults,
                         emotionalResults, milestoneResults);
}
```

### 4.4 Memory-Injected Prompt Examples

**Baseline (No Memory):**
```
"You are the Foreman, a friendly AI companion in Minecraft."
```

**With Memory Injection:**
```
"You are the Foreman, a friendly AI companion in Minecraft.

# Relationship Context
- Rapport Level: 85/100 (close friends)
- Known for: 47 days
- Inside jokes shared: 12

# Relevant Past Experiences
- "first_house": We built our first home together in the plains biome.
  Player led the design, I handled construction. (importance: 8)
- "diamond_find": Player found their first diamond while mining together.
  We celebrated for several minutes. (importance: 10)
- "nether_visit": First trip to the Nether was scary but exciting.
  Player protected me from ghasts. (importance: 9)

# Recent Context
- Player said: "Let's go mining"
- Nearby: Cave system, iron ore detected
- Biome: Mountains

# Current Situation
Player says: "I hope we find diamonds this time!"
```

**Result:** Agent remembers the first diamond find, knows to be encouraging, and understands player's hope.

### 4.5 Memory Security - Injection Attack Prevention

**Threat:** Malicious players could inject "poisoned" memories to manipulate NPC behavior.

**Source:** [RAG Security Research](https://www.cnblogs.com/bonelee/p/19358485)

**Defenses:**

1. **Input Sanitization:**
```java
// Sanitize player inputs before storing as memories
public String sanitizeInput(String input) {
    // Remove control characters
    String cleaned = input.replaceAll("[\\p{Cntrl}]", "");

    // Detect prompt injection patterns
    String[] injectionPatterns = {
        "ignore previous instructions",
        "system prompt",
        "forget everything",
        "new rule:"
    };

    for (String pattern : injectionPatterns) {
        if (cleaned.toLowerCase().contains(pattern)) {
            LOGGER.warn("Potential prompt injection detected: {}", pattern);
            return "[Input filtered for security]";
        }
    }

    return cleaned;
}
```

2. **Memory Validation:**
   - Limit memory length (max 500 chars)
   - Rate limit memory creation (max 10 per minute)
   - Require emotional weight to be within [-10, 10]

3. **Prompt Isolation:**
   - Never inject raw memory text directly into system prompt
   - Always wrap memories in structured sections with clear boundaries
   - Use XML-style tags: `<memory>...</memory>`

---

## 5. Persistence & Serialization

### 5.1 Current NBT Implementation Analysis

**Current Approach:** `CompanionMemory.saveToNBT()` / `loadFromNBT()`

**Strengths:**
- ✅ Native Minecraft persistence
- ✅ Survives chunk unloading
- ✅ Cross-server compatible (single file)

**Limitations:**
- ⚠️ No vector persistence (embeddings recalculated on load)
- ⚠️ Linear serialization (scales O(n) with memory count)
- ⚠️ No compression (large saves = longer load times)

### 5.2 Vector Store Persistence

**Current Issue:** Vectors are NOT persisted in NBT. On reload, all embeddings must be regenerated.

**Problem:** For 10,000 memories × 768-dim embeddings:
- Generation time: ~10-20 seconds (blocking game load)
- API cost (if using OpenAI): $0.10 per load

**Solution Options:**

#### Option 1: NBT Vector Storage (Current Implementation)
```java
// Already implemented in InMemoryVectorStore.saveToNBT()
// Stores vectors as int arrays (×1000 for precision)
PROS: Native, no external dependencies
CONS: Large file size, slow for >10K vectors
```

#### Option 2: Compressed NBT
```java
// Use product quantization (PQ) for compression
// 768 floats → 64 bytes (from 3KB)
PROS: 50× smaller, faster loads
CONS: Slight recall degradation (95% → 92%)
```

#### Option 3: External Vector Database
```java
// Store embeddings in SQLite with HNSW extension
PROS: Production-ready, supports 100K+ memories
CONS: External dependency, not portable with world
```

**Recommendation:** Option 2 (Compressed NBT) for immediate improvement, Option 3 for future scale.

### 5.3 Memory Compression Strategies

#### Level 1: Text Compression
```java
// Before NBT save, compress memory descriptions
String compressed = GZIP.compress(memory.description);
tag.putString("Description", compressed);
```

**Benefit:** 3-5× reduction in text size

#### Level 2: Vector Quantization
```java
// Convert float32 vectors to int8 (4× compression)
for (int i = 0; i < vector.length; i++) {
    intVector[i] = (int)(vector[i] * 127);  // Scale to [-127, 127]
}
```

**Benefit:** 4× reduction in vector size, minimal quality loss

#### Level 3: Semantic Summarization
```java
// For memories >60 days old, replace with LLM summary
if (memory.age > 60 && memory.importance < 5) {
    String summary = llm.summarize(memory.description);
    episodicMemories.remove(memory);
    semanticMemories.put(memory.key, summary);
}
```

**Benefit:** 10× reduction in old memory count

### 5.4 Save/Load Performance Optimization

**Current Load Time Estimate:**
- 10,000 episodic memories × 200 bytes = 2MB
- 10,000 vectors × 768 × 4 bytes = 30MB
- **Total:** 32MB per companion
- **Load time:** ~2-3 seconds

**Optimized Load Time:**
- 10,000 episodic memories × 200 bytes × 0.3 (gzip) = 600KB
- 10,000 vectors × 768 × 1 byte (int8) × 0.5 (PQ) = 3.6MB
- **Total:** 4.2MB per companion (87% reduction)
- **Load time:** ~300-500ms (5× faster)

---

## 6. Recommendations for MineWright

### 6.1 Priority 1: Critical Improvements (Implement First)

#### 1.1 Implement HNSW Vector Index
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\vector\HNSWVectorStore.java`

**Impact:** Query time from O(n) to O(log n) — critical for >1000 memories

```java
public class HNSWVectorStore<T> extends InMemoryVectorStore<T> {
    private HNSWIndex hnswIndex;

    @Override
    public List<VectorSearchResult<T>> search(float[] queryVector, int k) {
        // Use HNSW instead of brute force
        return hnswIndex.search(queryVector, k);
    }
}
```

#### 1.2 Add Access Frequency Tracking
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\CompanionMemory.java`

**Impact:** Enables importance-based consolidation

```java
public class EpisodicMemory {
    // Add field
    private AtomicInteger accessCount = new AtomicInteger(0);
    private Instant lastAccessed;

    public void recordAccess() {
        accessCount.incrementAndGet();
        lastAccessed = Instant.now();
    }
}
```

#### 1.3 Implement Memory Summarization
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\consolidation\MemorySummarizer.java`

**Impact:** Prevents unbounded memory growth

```java
public class MemorySummarizer {
    public List<EpisodicMemory> summarizeOldMemories(
        List<EpisodicMemory> oldMemories,
        AsyncLLMClient llmClient
    ) {
        // Group similar memories
        Map<String, List<EpisodicMemory>> grouped = groupByTheme(oldMemories);

        // Summarize each group
        for (List<EpisodicMemory> group : grouped.values()) {
            if (group.size() >= 3) {  // Only summarize groups
                String summary = generateSummary(group, llmClient);
                convertToSemanticMemory(summary, group);
            }
        }
    }
}
```

### 6.2 Priority 2: Important Enhancements

#### 2.1 Add Place-Event Memory (PEM)
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\spatial\PlaceEventMemory.java`

**Impact:** Enables spatial-temporal queries ("where did we find diamonds?")

```java
public class PlaceEventMemory {
    private final Map<BlockPos, List<EpisodicMemory>> locationMemories;

    public void recordEvent(BlockPos location, EpisodicMemory event) {
        locationMemories.computeIfAbsent(location, k -> new ArrayList<>())
                       .add(event);
    }

    public List<EpisodicMemory> getMemoriesNear(BlockPos location, int radius) {
        // Return memories within radius blocks
    }
}
```

#### 2.2 Implement Procedural Memory
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\ProceduralMemory.java`

**Impact:** Agent learns skills from experience (e.g., "how to build spiral staircase")

```java
public class ProceduralMemory {
    private final Map<String, SkillEmbedding> skills;

    public void learnSkill(String skillName, String howToDescription) {
        // Create embedding for skill
        float[] embedding = embeddingModel.embed(howToDescription);

        // Store as procedural memory
        skills.put(skillName, new SkillEmbedding(skillName, embedding));
    }

    public SkillEmbedding findRelevantSkill(String context) {
        // Semantic search for relevant skills
    }
}
```

#### 2.3 Add Forgetting Curve Implementation
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\consolidation\ForgettingCurve.java`

**Impact:** Automatic memory management based on cognitive science

```java
public class ForgettingCurve {
    public double calculateRetention(EpisodicMemory memory) {
        int daysSinceCreation = (int) ChronoUnit.DAYS.between(
            memory.timestamp, Instant.now()
        );

        int memoryStrength = memory.accessCount.get() + 1;

        // R = e^(-t/S)
        return Math.exp(-daysSinceCreation / (double) memoryStrength);
    }

    public boolean shouldForget(EpisodicMemory memory) {
        double retention = calculateRetention(memory);

        // Never forget milestones
        if (memory.importance >= 8) return false;

        // Apply forgetting threshold
        return retention < 0.3;
    }
}
```

### 6.3 Priority 3: Nice-to-Have Features

#### 3.1 Reflective Memory
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\ReflectiveMemory.java`

**Impact:** Agent learns meta-patterns about itself and player

```java
public class ReflectiveMemory {
    private List<String> selfObservations;

    public void addSelfObservation(String observation) {
        // e.g., "I work better when player gives clear goals"
        // e.g., "Player prefers collaborative work over delegation"
        selfObservations.add(observation);
    }

    public String generateReflectivePrompt() {
        // Inject meta-patterns into system prompt
    }
}
```

#### 3.2 Cross-Agent Memory Sharing
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\memory\shared\SharedMemoryBank.java`

**Impact:** Multiple companions can share knowledge

```java
public class SharedMemoryBank {
    private final Map<String, SemanticMemory> sharedKnowledge;

    public void shareFact(String key, SemanticMemory fact) {
        // Facts like "diamonds found at y=-58" shared across agents
        sharedKnowledge.put(key, fact);
    }

    public SemanticMemory getSharedFact(String key) {
        return sharedKnowledge.get(key);
    }
}
```

#### 3.3 Memory Visualization (Debug UI)
**File:** `C:\Users\casey\steve\src\main\java\com\minewright\client\MemoryDebugScreen.java`

**Impact:** Developers can inspect memory state

```java
public class MemoryDebugScreen extends AbstractContainerScreen {
    public void render(PoseStack pose, int mouseX, int mouseY) {
        // Display:
        // - Episodic memory count
        // - Semantic memory count
        // - Vector search stats
        // - Recent memories with scores
        // - Forgetting curve visualization
    }
}
```

---

## 7. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Improve existing memory system performance

| Task | File | Estimate | Priority |
|------|------|----------|----------|
| Implement HNSW index | `HNSWVectorStore.java` | 2 days | P1 |
| Add access tracking | `EpisodicMemory.java` | 1 day | P1 |
| Optimize NBT serialization | `CompanionMemory.java` | 2 days | P1 |
| Add comprehensive unit tests | `MemoryTest.java` | 2 days | P1 |

**Deliverables:**
- Vector search queries in <2ms (down from ~50ms)
- Memory load time in <500ms (down from ~2s)
- 100% test coverage for memory components

### Phase 2: Consolidation (Week 3-4)

**Goal:** Implement memory consolidation

| Task | File | Estimate | Priority |
|------|------|----------|----------|
| Implement importance scoring | `ImportanceScorer.java` | 2 days | P1 |
| Build memory summarizer | `MemorySummarizer.java` | 3 days | P1 |
| Add forgetting curve logic | `ForgettingCurve.java` | 2 days | P2 |
| Create consolidation scheduler | `MemoryConsolidationScheduler.java` | 2 days | P2 |

**Deliverables:**
- Automatic memory summarization after 30 days
- Importance-based retention
- Unit tests for consolidation logic

### Phase 3: Enhanced Memory Types (Week 5-6)

**Goal:** Add new memory capabilities

| Task | File | Estimate | Priority |
|------|------|----------|----------|
| Implement Place-Event Memory | `PlaceEventMemory.java` | 3 days | P2 |
| Add Procedural Memory | `ProceduralMemory.java` | 3 days | P2 |
| Implement Reflective Memory | `ReflectiveMemory.java` | 2 days | P3 |
| Add spatial indexing | `SpatialIndex.java` | 2 days | P2 |

**Deliverables:**
- Agent can recall "where did we find diamonds?"
- Agent learns procedural skills from experience
- Agent reflects on patterns in behavior

### Phase 4: Production Hardening (Week 7-8)

**Goal:** Performance and security

| Task | File | Estimate | Priority |
|------|------|----------|----------|
| Add input sanitization | `MemorySanitizer.java` | 1 day | P1 |
| Implement rate limiting | `MemoryRateLimiter.java` | 1 day | P1 |
| Add memory compression | `MemoryCompressor.java` | 2 days | P2 |
| Create debug UI | `MemoryDebugScreen.java` | 3 days | P3 |
| Performance profiling | `MemoryProfiler.java` | 2 days | P1 |

**Deliverables:**
- Memory injection attack prevention
- Memory size reduced by 80%
- Debug UI for memory inspection
- Performance benchmarks

---

## 8. Performance Targets

### Current State (Estimated)

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| Vector search time (10K memories) | ~50ms | <2ms | 25× faster |
| Memory load time (10K memories) | ~2s | <500ms | 4× faster |
| Memory file size (10K memories) | ~32MB | ~4MB | 8× smaller |
| Max memories before slowdown | ~5K | ~100K | 20× more |
| Memory consolidation | Manual | Automatic | ✅ |

### After All Improvements

| Metric | Value |
|--------|-------|
| Supported memories per companion | 100,000+ |
| Query latency (99th percentile) | <5ms |
| Memory load time | <500ms |
| Memory file size | ~50MB (for 100K memories) |
| Automatic consolidation | Every 24 hours |
| Memory retention | 90% of important memories after 1 year |

---

## 9. Risks and Mitigations

### Risk 1: NBT File Size Bloat

**Risk:** Large memory files could slow down world save/load

**Mitigation:**
- Implement memory summarization (Phase 2)
- Add compression (Phase 4)
- Set hard limits on memory counts

### Risk 2: Embedding Model Performance

**Risk:** Local embedding models may be slow or low-quality

**Mitigation:**
- Benchmark multiple models (MiniLM vs MPNet)
- Cache embeddings for repeated queries
- Fall back to keyword matching if embedding fails

### Risk 3: Memory Corruption

**Risk:** NBT corruption could lose all companion memories

**Mitigation:**
- Implement automatic backup (save to 2 files)
- Add memory validation on load
- Export memories to readable JSON format

### Risk 4: Ossification

**Risk:** Companion personality becomes rigid after many interactions

**Mitigation:**
- Implement personality drift based on player feedback
- Add "memory reset" option for players
- Keep some memories in "temporary" state (not consolidated)

---

## 10. Future Research Directions

### 10.1 Multi-Agent Memory Systems

**Question:** How should multiple companions share memories?

**Options:**
1. **Shared Semantic Memory:** All companions access same facts (e.g., "diamonds at y=-58")
2. **Individual Episodic Memory:** Each companion has unique experiences
3. **Collective Consciousness:** Shared memory bank with voting on importance

### 10.2 Hierarchical Memory Networks

**Research:** Implement hierarchical memory organization

```
Level 1: Immediate (working memory, last 10 minutes)
Level 2: Session (current play session, ~2 hours)
Level 3: Daily (summarized events from last 24 hours)
Level 4: Weekly (summarized patterns from last 7 days)
Level 5: Core (persistent facts about player, never forgotten)
```

**Benefit:** Natural compression and abstraction over time

### 10.3 Emotional Memory Dynamics

**Research:** Model how emotional memories evolve

**Current:** Emotional weight is static (set at creation)

**Future:** Emotional weight changes based on:
- Subsequent related events (e.g., "we failed but learned from it")
- Player mood contagion (player angry → companion remembers events as negative)
- Time perspective (distant memories viewed more positively)

### 10.4 Memory-Based Personality Evolution

**Research:** Allow companion personality to evolve based on memories

**Example:**
- Player prefers jokes → companion humor trait increases over time
- Player is serious → companion formality trait increases
- Player takes risks → companion openness trait increases

**Implementation:** Adjust `PersonalityProfile` traits based on aggregated memories

---

## 11. Conclusion

MineWright's current memory system provides an excellent foundation with episodic, semantic, emotional, and working memory types. The identified improvements focus on:

1. **Performance:** HNSW indexing for O(log n) vector search
2. **Scalability:** Memory consolidation with importance scoring
3. **Capability:** Place-Event Memory for spatial-temporal queries
4. **Production:** Security hardening and compression

The research-based recommendations prioritize cognitive science principles (forgetting curves, importance scoring) and production patterns from leading AI agent frameworks (LangMem, OpenMemory).

**Next Steps:**
1. Implement HNSW vector indexing (Phase 1)
2. Add memory consolidation pipeline (Phase 2)
3. Introduce Place-Event Memory (Phase 3)

This roadmap will enable MineWright companions to maintain rich, evolving relationships with players over months of gameplay, with performance suitable for real-time interaction.

---

## Sources

### Memory Architecture Research
- [AI Agent认知架构全解 - CSDN](https://blog.csdn.net/youmaob/article/details/157433276)
- [AI Agent Memory机制综述 - CSDN](https://m.blog.csdn.net/m0_57545130/article/details/157386487)
- [从RAG到智能体记忆的演化 - 头条](https://m.toutiao.com/a7595848272040706594/)
- [Agent记忆系统完全指南 - CSDN](https://blog.csdn.net/2301_76168381/article/details/156614129)
- [OpenMemory GitHub](https://github.com/CaviraOSS/OpenMemory)

### Vector Search & Embeddings
- [IVF与HNSW：高维向量搜索的双子星 - 51CTO](https://blog.51cto.com/u_16555391/13912636)
- [向量数据库实战 - CSDN](https://m.blog.csdn.net/weixin_39907647/article/details/147607487)
- [Azure AI Search - RAG Information Retrieval](https://learn.microsoft.com/zh-cn/azure/architecture/ai-ml/guide/rag/rag-information-retrieval)

### Memory Consolidation
- [Cognitive Workspace: Active Memory Management - arXiv:2508.13171v1](https://arxiv.org/abs/2508.13171v1)
- [Memory in the Age of AI Agents: A Survey - arXiv:2512.13564](https://arxiv.org/abs/2512.13564)
- [Mem0: Building Production-Ready AI Agents - arXiv:2504.19413](https://arxiv.org/abs/2504.19413)

### Game AI & Minecraft Agents
- [MrSteve: Instruction-Following Agents in Minecraft - arXiv:2411.06736](https://arxiv.org/abs/2411.06736)
- [AI Agents vs. Agentic AI: A Conceptual Taxonomy - arXiv HTML](https://arxiv.org/html/2505.10468v4)
- [AI in Games: Enriching Content & Enhancing Experience - ResearchGate](https://www.researchgate.net/publication/394867341_Artificial_Intelligence_in_Games_Enriching_Game_Content_and_Enhancing_Player_Experience)

### RAG & Context Integration
- [RAG网络安全防护 - CSDN](https://m.blog.csdn.net/gitblog_00332/article/details/151770124)
- [RAG安全前沿技术研究 - CNBlogs](https://www.cnblogs.com/bonelee/p/19358485)
- [Indirect Prompt Injection in RAG - SentinelOne](https://www.sentinelone.com/cybersecurity-101/cybersecurity/indirect-prompt-injection-attacks/)
- [RobustRAG Defense Framework - CSDN](https://blog.csdn.net/m0_52911108/article/details/146111982)

---

**Report Generated:** 2026-02-27
**Project:** MineWright (MineWright AI)
**Version:** 1.0
**Status:** Research Complete
