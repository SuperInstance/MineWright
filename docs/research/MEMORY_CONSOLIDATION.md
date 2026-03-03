# Memory Consolidation and Forgetting for AI Agents

**Document Version:** 1.0
**Date:** 2026-03-02
**Author:** Claude Orchestrator
**Status:** Research Document

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Memory Consolidation](#memory-consolidation)
3. [Forgetting Mechanisms](#forgetting-mechanisms)
4. [Memory Organization](#memory-organization)
5. [Case Studies](#case-studies)
6. [Application to MineWright](#application-to-minewright)
7. [Implementation Algorithms](#implementation-algorithms)
8. [References](#references)

---

## Executive Summary

Memory consolidation and forgetting are fundamental cognitive processes that enable intelligent systems to maintain relevant information while discarding noise. This document researches biological and artificial memory systems to extract principles for enhancing AI agent memory architectures.

**Key Findings:**

1. **Hierarchical Memory Architecture** - Effective AI agents require layered memory systems (working, episodic, semantic)
2. **Importance-Based Forgetting** - Memories should be retained based on relevance, not just recency
3. **Progressive Summarization** - Old memories should be compressed, not simply deleted
4. **Access-Dependent Strengthening** - Frequently accessed memories become more resilient
5. **Sleep-Inspired Consolidation** - Periodic offline processing improves long-term retention

**Current MineWright Status:**
- ✅ Implemented: Basic memory consolidation (MemoryConsolidationService)
- ✅ Implemented: Importance-based memory scoring
- ✅ Implemented: Vector semantic search
- ⏳ Missing: Sophisticated decay functions
- ⏳ Missing: Interference-based forgetting
- ⏳ Missing: Sleep/periodic consolidation triggers
- ⏳ Missing: Conversation summarization

---

## Memory Consolidation

### Biological Foundation

Memory consolidation is the process by which fragile short-term memories are transformed into stable long-term memories. This process occurs through:

1. **Synaptic Consolidation** - Immediate strengthening of neural connections (minutes to hours)
2. **Systems Consolidation** - Reorganization of brain regions (days to years)
3. **Memory Replay** - Reactivation of memories during rest (sleep)

**Key Insight:** Consolidation is not a one-time event but a continuous process of reorganization and strengthening.

### AI Agent Consolidation Approaches

#### 1. Importance-Based Consolidation

Transfer memories from short-term to long-term based on importance scores:

```python
def importance_score(memory):
    # Time decay with 7-day half-life
    age_score = exp(-days_since_creation / 7.0)

    # Emotional importance (normalize -10 to +10 range to 0-1)
    importance_score = abs(emotional_weight) / 10.0

    # Access frequency (cap at 10 accesses for max score)
    access_score = min(1.0, access_count / 10.0)

    # Recent access bonus (if accessed in last 24 hours)
    recent_access_bonus = 0.2 if hours_since_access < 24 else 0.0

    # Combined weighted score
    return (age_score * 0.4 +
            importance_score * 0.4 +
            access_score * 0.2 +
            recent_access_bonus)
```

#### 2. Sleep-Inspired Consolidation

Periodic consolidation during idle periods (inspired by biological sleep):

```python
async def sleep_consolidation(agent):
    """
    Runs during agent idle periods or sleep cycles.
    Consolidates recent memories into long-term storage.
    """
    while agent.is_alive():
        await agent.sleep_cycle()

        # Get recent episodic memories
        recent_memories = agent.memory.get_recent_memories(hours=24)

        # Group by topic and emotional significance
        groups = group_memories_by_topic(recent_memories)

        # Consolidate each group
        for topic, memories in groups.items():
            if len(memories) >= CONSOLIDATION_THRESHOLD:
                # Generate summary
                summary = await generate_summary(memories)

                # Store as semantic memory
                agent.memory.learn_fact(topic, summary)

                # Mark episodic memories for potential deletion
                for mem in memories:
                    if not mem.is_protected():
                        mem.consolidated = True
```

#### 3. Replay-Based Consolidation

Inspired by hippocampal replay during sleep:

```python
async def replay_consolidation(agent):
    """
    Replays important memories to strengthen neural pathways.
    In AI: re-processes memories through embedding models.
    """
    # Get high-importance memories accessed in last 24h
    important_memories = [
        m for m in agent.memory.episodic
        if m.importance > 0.7 and m.last_accessed < hours_ago(24)
    ]

    # Replay through embedding model to strengthen associations
    for memory in important_memories:
        # Re-encode with current context
        new_embedding = await agent.embedder.encode(
            memory.description,
            context=agent.current_context
        )

        # Update embedding (strengthens association)
        agent.memory.update_embedding(memory.id, new_embedding)

        # Boost importance score
        memory.importance *= 1.1
```

### Progressive Summarization

Memories should be progressively summarized as they age:

```
Level 0 (Raw): "Player asked me to build a house at coordinates 100, 64, -200.
                They want it made of oak logs, 10x10x5 blocks.
                They said 'make it cozy' and 'include a fireplace'."

Level 1 (1 day): "Player requested 10x10x5 oak house at 100, 64, -200 with fireplace."

Level 2 (1 week): "Player prefers cozy oak houses with fireplaces."

Level 3 (1 month): "Player likes cozy, warm aesthetics in buildings."
```

---

## Forgetting Mechanisms

### The Ebbinghaus Forgetting Curve

**Mathematical Expression:**

```
R = e^(-t/S)
```

Where:
- **R** = Memory retention percentage
- **t** = Time since learning
- **S** = Stability parameter (reflects memory strength)

**Key Insight:** Forgetting begins immediately after learning, initially very fast, then gradually slows down.

### Forgetting Mechanisms

#### 1. Time-Based Decay

```python
def time_decay(memory, current_time):
    """
    Applies exponential decay based on time since last access.
    """
    time_since_access = current_time - memory.last_accessed

    # Stability parameter (higher = slower decay)
    stability = memory.base_stability * (1 + memory.access_count * 0.1)

    # Ebbinghaus decay function
    retention = math.exp(-time_since_access / stability)

    return retention
```

#### 2. Interference-Based Forgetting

```python
def interference_forgetting(target_memory, all_memories):
    """
    Reduces memory strength based on interference from similar memories.
    """
    interference = 0.0

    for other in all_memories:
        if other.id == target_memory.id:
            continue

        # Calculate similarity
        similarity = cosine_similarity(
            target_memory.embedding,
            other.embedding
        )

        # Retroactive interference: newer memories interfere with older
        if other.timestamp > target_memory.timestamp:
            interference += similarity * 0.3

        # Proactive interference: older memories interfere with newer
        else:
            interference += similarity * 0.1

    # Reduce memory strength based on interference
    return max(0.0, target_memory.strength - interference)
```

#### 3. Importance-Weighted Retention

```python
def importance_weighted_forgetting(memory):
    """
    Important memories decay more slowly.
    """
    base_decay_rate = 0.1

    # Emotional impact reduces decay
    emotional_factor = 1.0 - (abs(memory.emotional_weight) / 20.0)

    # Access frequency reduces decay
    access_factor = 1.0 / (1.0 + memory.access_count * 0.2)

    # Milestone memories never decay
    if memory.is_milestone:
        return 0.0

    # Combined decay rate
    decay_rate = base_decay_rate * emotional_factor * access_factor

    return decay_rate
```

#### 4. Chunking and Summarization

```python
async def chunk_and_summarize(memories):
    """
    Groups related memories and summarizes them.
    """
    # Group by topic and time
    groups = group_by_topic_and_time(memories)

    results = []
    for topic, group in groups.items():
        if len(group) >= CHUNK_THRESHOLD:
            # Generate summary
            summary = await llm_summarize(group)

            # Create semantic memory
            semantic = SemanticMemory(
                category="consolidated",
                key=topic,
                value=summary,
                confidence=0.8
            )

            # Mark episodic memories for deletion
            for mem in group:
                if not mem.is_protected():
                    mem.consolidated = True

            results.append(semantic)

    return results
```

---

## Memory Organization

### Episodic vs Semantic Memory

**Episodic Memory:**
- Stores "what," "where," and "when" of events
- Example: "Yesterday at 3pm, we built a tower together"
- Provides "How-to" knowledge through experience
- Requires contextual retrieval

**Semantic Memory:**
- General facts, concepts, rules
- Example: "The player likes building tall structures"
- Provides "What-is" knowledge
- Context-independent retrieval

**Transfer Process:**

```python
async def episodic_to_semantic(episodic_memories):
    """
    Extracts semantic knowledge from episodic experiences.
    """
    patterns = extract_patterns(episodic_memories)

    semantic_facts = []
    for pattern in patterns:
        # Extract general rule
        rule = await extract_rule(pattern)

        # Verify with multiple instances
        instances = [m for m in episodic_memories if matches(m, pattern)]
        if len(instances) >= CONFIDENCE_THRESHOLD:
            semantic_fact = SemanticMemory(
                category="learned_pattern",
                key=pattern,
                value=rule,
                confidence=len(instances) / len(episodic_memories)
            )
            semantic_facts.append(semantic_fact)

    return semantic_facts
```

### Memory Hierarchies

```
┌─────────────────────────────────────────────────────────────────┐
│                    WORKING MEMORY                               │
│                  (Current Session)                              │
│   • Recent conversation context (last 10 messages)             │
│   • Current task state                                          │
│   • Immediate environment                                       │
│   • Capacity: ~20 entries                                       │
│   • Duration: Session-only                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Consolidation
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  EPISODIC MEMORY                                │
│               (Recent Experiences)                              │
│   • Specific events with timestamps                             │
│   • Emotional weights                                          │
│   • Access tracking                                             │
│   • Capacity: ~200 memories                                     │
│   • Duration: Days to weeks                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Summarization
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SEMANTIC MEMORY                                │
│              (Generalized Knowledge)                            │
│   • Player preferences                                         │
│   • Learned patterns                                           │
│   • Summarized experiences                                     │
│   • Capacity: ~1000 facts                                       │
│   • Duration: Permanent (until overridden)                      │
└─────────────────────────────────────────────────────────────────┘
```

### Association Networks

Memories should be connected through associations:

```python
class AssociativeMemory:
    def __init__(self):
        self.memories = {}  # memory_id -> Memory
        self.associations = defaultdict(set)  # memory_id -> {related_ids}

    def associate(self, memory1_id, memory2_id, strength=1.0):
        """Creates bidirectional association between memories."""
        self.associations[memory1_id].add((memory2_id, strength))
        self.associations[memory2_id].add((memory1_id, strength))

    def spread_activation(self, seed_memory_id, activation_threshold=0.5):
        """
        Spreads activation through association network.
        Returns list of activated memories sorted by activation level.
        """
        activated = {seed_memory_id: 1.0}
        queue = [seed_memory_id]

        while queue:
            current_id = queue.pop(0)
            current_activation = activated[current_id]

            if current_activation < activation_threshold:
                continue

            # Spread to neighbors
            for neighbor_id, strength in self.associations[current_id]:
                new_activation = current_activation * strength * 0.8

                if neighbor_id not in activated:
                    activated[neighbor_id] = new_activation
                    queue.append(neighbor_id)
                else:
                    activated[neighbor_id] = max(
                        activated[neighbor_id],
                        new_activation
                    )

        # Sort by activation level
        return sorted(
            [(mid, act) for mid, act in activated.items()],
            key=lambda x: x[1],
            reverse=True
        )
```

### Index Structures

Efficient memory retrieval requires multi-dimensional indexing:

```python
class MemoryIndex:
    def __init__(self):
        # Temporal index
        self.by_time = SortedList(key=lambda m: m.timestamp)

        # Semantic index (vector store)
        self.by_semantic = VectorStore(dimension=768)

        # Emotional index
        self.by_emotion = SortedList(key=lambda m: abs(m.emotional_weight))

        # Type index
        self.by_type = defaultdict(list)

        # Access frequency index
        self.by_access = SortedList(key=lambda m: m.access_count)

    def add(self, memory):
        """Add memory to all indices."""
        self.by_time.add(memory)
        self.by_semantic.add(memory.embedding, memory)
        self.by_emotion.add(memory)
        self.by_type[memory.event_type].append(memory)
        self.by_access.add(memory)

    def query(self,
              time_range=None,
              semantic_query=None,
              emotion_threshold=5,
              event_type=None,
              min_access=0):
        """
        Multi-dimensional memory query.
        Returns intersection of all specified criteria.
        """
        results = set(self.by_time)

        if time_range:
            results &= set(
                m for m in self.by_time
                if time_range[0] <= m.timestamp <= time_range[1]
            )

        if semantic_query:
            semantic_results = self.by_semantic.search(semantic_query, k=50)
            results &= set(m.data for m in semantic_results)

        if emotion_threshold:
            results &= set(
                m for m in self.by_emotion
                if abs(m.emotional_weight) >= emotion_threshold
            )

        if event_type:
            results &= set(self.by_type[event_type])

        if min_access > 0:
            results &= set(
                m for m in self.by_access
                if m.access_count >= min_access
            )

        return sorted(results, key=lambda m: m.importance_score(), reverse=True)
```

---

## Case Studies

### Human Memory Models

#### Atkinson-Shiffrin Model (1968)

**Three-Stage Model:**
1. **Sensory Memory** - < 1 second, large capacity
2. **Short-Term Memory** - 15-30 seconds, 7±2 items
3. **Long-Term Memory** - Permanent, unlimited capacity

**Key Insight:** Information must be actively rehearsed to transfer from short-term to long-term memory.

#### Baddeley's Working Memory Model (1974)

**Multi-Component System:**
- **Central Executive** - Controls attention
- **Phonological Loop** - Verbal information
- **Visuospatial Sketchpad** - Visual/spatial information
- **Episodic Buffer** - Integrates information

**Key Insight:** Working memory is not a single store but multiple specialized systems.

### AI Memory Systems

#### MemGPT (UC Berkeley, 2024)

**Infinite Context via Hierarchical Memory:**
- **LLM Context Window** = Working memory
- **Vector Database** = Long-term episodic memory
- **File System** = Archival storage

**Key Innovation:** Explicit memory management with "memory operations" (read, write, search).

#### ReAct Agent (2023)

**Reasoning + Acting:**
- Uses "thought" and "action" interleaving
- Maintains scratchpad for intermediate reasoning
- No persistent memory across sessions

**Limitation:** No long-term memory or forgetting mechanisms.

#### Voyager (2023)

**Skill Library + Memory:**
- **Episodic Memory** - Failed/successful task executions
- **Semantic Memory** - Learned skills as code
- **Self-verification** - Prunes ineffective skills

**Key Innovation:** Automatic skill extraction from experience.

### Game NPC Memory

#### Ubisoft Companion AI (2024)

**Challenges Identified:**
- Current systems remember ~1.5 hours of gameplay
- Need long-term memory for dozens of hours
- True friends don't remember every word, just impactful events
- **Need:** Importance-based memory retention

#### Personalized NPCs (MDPI Journal, 2024)

**Ebbinghaus-Inspired Forgetting:**
- Two memory modules with consolidation mechanism
- Access frequency determines retention
- Long-term memory stores critical historical interactions

#### RAG-Based NPC Memory

**Retrieval-Augmented Generation:**
- Personal memory database per NPC
- Records every interaction: dialogue, choices, emotions
- NPCs recall and reference past interactions naturally
- Semantic search for relevant memory retrieval

---

## Application to MineWright

### Current Implementation Analysis

**Strengths:**
1. ✅ **Hierarchical Memory Architecture**
   - Working memory (recent context)
   - Episodic memory (specific events)
   - Semantic memory (player facts)
   - Emotional memory (significant moments)

2. ✅ **Importance-Based Memory Scoring**
   - `computeMemoryScore()` combines age, importance, access
   - Smart eviction based on score, not just FIFO
   - Protected memories (milestones, high emotion)

3. ✅ **Vector Semantic Search**
   - `findRelevantMemories()` uses embeddings
   - Fallback to keyword matching
   - Records access for importance evolution

4. ✅ **Basic Consolidation Service**
   - Groups memories by topic and time
   - Generates summaries (LLM or algorithmic)
   - Stores as semantic facts

**Gaps:**
1. ⏳ **No Explicit Decay Function**
   - Importance score exists but no automatic decay
   - No periodic recalculation of scores

2. ⏳ **No Interference-Based Forgetting**
   - Similar memories don't compete
   - No proactive/retroactive interference

3. ⏳ **No Sleep-Inspired Consolidation**
   - Consolidation only triggered by count/time threshold
   - No idle period processing

4. ⏳ **Limited Conversation Summarization**
   - No progressive summarization of conversations
   - Working memory not periodically compressed

### Enhancement Recommendations

#### Priority 1: Add Explicit Decay Functions

**Implementation:**

```java
// In CompanionMemory.java

/**
 * Applies time-based decay to all memories.
 * Should be called periodically (e.g., daily).
 */
public void applyMemoryDecay() {
    Instant now = Instant.now();

    for (EpisodicMemory memory : episodicMemories) {
        // Skip protected memories
        if (memory.isProtected()) {
            continue;
        }

        // Calculate decayed importance
        long daysSinceAccess = ChronoUnit.DAYS.between(memory.getLastAccessed(), now);
        float decayFactor = (float) Math.exp(-daysSinceAccess / 7.0); // 7-day half-life

        // Reduce effective importance for eviction consideration
        float currentImportance = computeMemoryScore(memory) * decayFactor;

        // If importance falls below threshold, mark for consolidation
        if (currentImportance < 0.2 && daysSinceAccess > 30) {
            memory.setConsolidationCandidate(true);
        }
    }

    LOGGER.debug("Applied memory decay to {} memories", episodicMemories.size());
}
```

#### Priority 2: Implement Interference-Based Forgetting

**Implementation:**

```java
/**
 * Reduces memory strength based on interference from similar memories.
 * This implements proactive and retroactive interference.
 */
public float computeInterferencePenalty(EpisodicMemory targetMemory) {
    float totalInterference = 0.0f;

    for (EpisodicMemory other : episodicMemories) {
        if (other == targetMemory) {
            continue;
        }

        // Calculate semantic similarity (using cosine similarity of embeddings)
        float similarity = computeSemanticSimilarity(targetMemory, other);

        // Skip if not similar enough
        if (similarity < 0.5f) {
            continue;
        }

        // Retroactive interference: newer memories interfere with older
        if (other.timestamp.isAfter(targetMemory.timestamp)) {
            totalInterference += similarity * 0.3f;
        }
        // Proactive interference: older memories interfere with newer
        else {
            totalInterference += similarity * 0.1f;
        }
    }

    return Math.min(1.0f, totalInterference);
}

/**
 * Computes semantic similarity between two memories using their embeddings.
 */
private float computeSemanticSimilarity(EpisodicMemory m1, EpisodicMemory m2) {
    try {
        float[] emb1 = embeddingModel.embed(m1.description);
        float[] emb2 = embeddingModel.embed(m2.description);
        return cosineSimilarity(emb1, emb2);
    } catch (Exception e) {
        LOGGER.warn("Failed to compute semantic similarity", e);
        return 0.0f;
    }
}

/**
 * Cosine similarity between two vectors.
 */
private float cosineSimilarity(float[] v1, float[] v2) {
    float dotProduct = 0.0f;
    float norm1 = 0.0f;
    float norm2 = 0.0f;

    for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
        dotProduct += v1[i] * v2[i];
        norm1 += v1[i] * v1[i];
        norm2 += v2[i] * v2[i];
    }

    return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
}
```

#### Priority 3: Sleep-Inspired Consolidation

**Implementation:**

```java
// In ForemanEntity.java

/**
 * Called when the entity is idle (not executing tasks).
 * Performs sleep-inspired memory consolidation.
 */
private void performIdleConsolidation() {
    if (random.nextFloat() < 0.05) { // 5% chance per idle tick
        // Apply memory decay
        memory.applyMemoryDecay();

        // Replay important memories
        replayRecentMemories();

        // Check if consolidation is needed
        if (memory.getRecentMemories(Integer.MAX_VALUE).size() > 100) {
            triggerConsolidation();
        }
    }
}

/**
 * Replays recent important memories to strengthen them.
 * Inspired by hippocampal replay during sleep.
 */
private void replayRecentMemories() {
    // Get high-importance memories accessed in last 24h
    Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

    List<EpisodicMemory> recentImportant = memory.getRecentMemories(50).stream()
        .filter(m -> m.getLastAccessed().isAfter(cutoff))
        .filter(m -> memory.computeMemoryScore(m) > 0.7f)
        .collect(Collectors.toList());

    // Replay by re-encoding embeddings
    for (EpisodicMemory memory : recentImportant) {
        // This strengthens the memory pathway
        memory.recordAccess(); // Increment access count

        // Could triggerdream-like reflection here
        if (random.nextFloat() < 0.1) {
            LOGGER.debug("Replaying memory: {}", memory.description);
        }
    }
}

/**
 * Triggers asynchronous consolidation.
 */
private void triggerConsolidation() {
    AsyncLLMClient llmClient = getLlmClient();
    if (llmClient != null) {
        MemoryConsolidationService consolidationService =
            new MemoryConsolidationService(llmClient);

        consolidationService.checkAndConsolidate(memory)
            .thenAccept(result -> {
                if (result.success) {
                    LOGGER.info("Consolidated {} memory groups, removed {} memories",
                        result.groupsConsolidated, result.memoriesRemoved);
                }
            });
    }
}
```

#### Priority 4: Conversation Summarization

**Implementation:**

```java
/**
 * Summarizes recent working memory into a compressed format.
 * Should be called when working memory approaches capacity.
 */
public CompletableFuture<Void> summarizeWorkingMemory() {
    if (workingMemory.size() < MAX_WORKING_MEMORY * 0.8) {
        return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.supplyAsync(() -> {
        // Get entries to summarize (older than 1 hour)
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

        List<WorkingMemoryEntry> toSummarize = workingMemory.stream()
            .filter(e -> e.timestamp.isBefore(cutoff))
            .collect(Collectors.toList());

        if (toSummarize.isEmpty()) {
            return null;
        }

        // Group by type
        Map<String, List<WorkingMemoryEntry>> grouped = toSummarize.stream()
            .collect(Collectors.groupingBy(e -> e.type));

        // Generate summaries for each group
        for (Map.Entry<String, List<WorkingMemoryEntry>> entry : grouped.entrySet()) {
            String type = entry.getKey();
            List<WorkingMemoryEntry> entries = entry.getValue();

            if (entries.size() >= 3) {
                // Create summary
                String summary = generateWorkingMemorySummary(type, entries);

                // Replace entries with summary
                for (WorkingMemoryEntry e : entries) {
                    workingMemory.remove(e);
                }

                // Add summary entry
                workingMemory.addFirst(new WorkingMemoryEntry(
                    type + "_summary",
                    summary,
                    Instant.now()
                ));
            }
        }

        LOGGER.debug("Summarized {} working memory entries", toSummarize.size());
        return null;
    });
}

/**
 * Generates a summary of working memory entries.
 */
private String generateWorkingMemorySummary(String type, List<WorkingMemoryEntry> entries) {
    // Count by content patterns
    Map<String, Integer> patterns = new HashMap<>();
    for (WorkingMemoryEntry entry : entries) {
        String pattern = extractPattern(entry.content);
        patterns.merge(pattern, 1, Integer::sum);
    }

    // Generate summary
    StringBuilder summary = new StringBuilder();
    summary.append("Recent activity (").append(entries.size()).append(" events): ");

    List<Map.Entry<String, Integer>> sorted = patterns.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(3)
        .collect(Collectors.toList());

    for (int i = 0; i < sorted.size(); i++) {
        if (i > 0) summary.append(", ");
        Map.Entry<String, Integer> entry = sorted.get(i);
        summary.append(entry.getKey()).append(" (").append(entry.getValue()).append("x)");
    }

    return summary.toString();
}

/**
 * Extracts a pattern from content for summarization.
 */
private String extractPattern(String content) {
    // Simple pattern extraction - could be enhanced with NLP
    String[] words = content.toLowerCase().split("\\s+");
    if (words.length > 0) {
        return words[0]; // Use first word as pattern
    }
    return "misc";
}
```

### Updated Memory Score Algorithm

**Enhanced version incorporating all mechanisms:**

```java
/**
 * Computes a comprehensive memory score based on multiple factors.
 * This is the enhanced version with decay, interference, and reinforcement.
 *
 * @param memory The memory to score
 * @return A score from 0.0 (least important) to 1.0 (most important)
 */
public float computeMemoryScore(EpisodicMemory memory) {
    Instant now = Instant.now();

    // === 1. Time Decay (Ebbinghaus forgetting curve) ===
    long daysSinceCreation = ChronoUnit.DAYS.between(memory.timestamp, now);
    long daysSinceAccess = ChronoUnit.DAYS.between(memory.getLastAccessed(), now);

    // Base stability increases with access count
    float stability = 7.0f * (1.0f + memory.getAccessCount() * 0.1f);

    // Decay based on time since last access
    float ageScore = (float) Math.exp(-daysSinceAccess / stability);

    // === 2. Emotional Importance ===
    float importanceScore = Math.min(1.0f, Math.abs(memory.emotionalWeight) / 10.0f);

    // === 3. Access Frequency ===
    float accessScore = Math.min(1.0f, memory.getAccessCount() / 10.0f);

    // === 4. Recent Access Bonus ===
    long hoursSinceAccess = ChronoUnit.HOURS.between(memory.getLastAccessed(), now);
    float recentAccessBonus = hoursSinceAccess < 24 ? 0.2f : 0.0f;

    // === 5. Interference Penalty (NEW) ===
    float interferencePenalty = computeInterferencePenalty(memory);
    float interferenceScore = 1.0f - interferencePenalty;

    // === 6. Milestone Protection ===
    float milestoneBonus = memory.isMilestone ? 1.0f : 0.0f;

    // === Combined Weighted Score ===
    float baseScore = ageScore * 0.3f +
                     importanceScore * 0.3f +
                     accessScore * 0.2f +
                     recentAccessBonus;

    // Apply interference and milestone modifiers
    float finalScore = baseScore * interferenceScore + milestoneBonus;

    return Math.min(1.0f, Math.max(0.0f, finalScore));
}
```

---

## Implementation Algorithms

### Algorithm 1: Periodic Memory Maintenance

**Purpose:** Runs daily to apply decay and trigger consolidation

```python
async def daily_memory_maintenance(agent):
    """
    Should be called once per game day or real-world day.
    Performs all memory maintenance tasks.
    """
    # 1. Apply time-based decay
    agent.memory.apply_decay()

    # 2. Check for consolidation candidates
    candidates = agent.memory.get_consolidation_candidates()

    if len(candidates) >= CONSOLIDATION_THRESHOLD:
        # 3. Group candidates by topic
        groups = group_by_topic(candidates)

        # 4. Consolidate each group
        for topic, memories in groups.items():
            if len(memories) >= GROUP_MIN_SIZE:
                # Generate summary
                summary = await generate_summary(memories)

                # Store as semantic memory
                agent.memory.learn_fact(
                    category="consolidated_memory",
                    key=topic,
                    value=summary
                )

                # Mark for removal
                for mem in memories:
                    if not mem.is_protected():
                        mem.mark_for_removal()

        # 5. Remove consolidated memories
        agent.memory.remove_marked()

    # 6. Validate memory state
    agent.memory.validate()
```

### Algorithm 2: Smart Memory Eviction

**Purpose:** Determines which memories to remove when capacity is reached

```python
def smart_eviction(memories, capacity):
    """
    Evicts memories based on comprehensive scoring.
    Keeps protected and high-importance memories.
    """
    if len(memories) <= capacity:
        return []  # No eviction needed

    # Score all memories
    scored = [(m, compute_memory_score(m)) for m in memories]

    # Separate protected and non-protected
    protected = [(m, s) for m, s in scored if m.is_protected()]
    candidates = [(m, s) for m, s in scored if not m.is_protected()]

    # Sort candidates by score (lowest first)
    candidates.sort(key=lambda x: x[1])

    # Calculate how many to remove
    to_remove_count = len(memories) - capacity

    # If we have enough unprotected candidates
    if len(candidates) >= to_remove_count:
        return [m for m, s in candidates[:to_remove_count]]

    # Otherwise, remove all unprotected and some protected
    evicted = [m for m, s in candidates]

    # Sort protected by score and remove lowest
    protected.sort(key=lambda x: x[1])
    remaining = to_remove_count - len(evicted)
    evicted.extend([m for m, s in protected[:remaining]])

    return evicted
```

### Algorithm 3: Progressive Summarization

**Purpose:** Creates increasingly abstract summaries over time

```python
async def progressive_summarization(memory):
    """
    Creates multiple levels of summarization for a memory.
    """
    # Level 0: Raw memory (already exists)
    # "Player asked me to build a house at 100, 64, -200.
    #  They want oak logs, 10x10x5, cozy with fireplace."

    # Level 1: Compressed (1 day old)
    if memory.age >= days(1) and not memory.has_summary(1):
        summary_1 = await compress_details(memory)
        # "10x10x5 oak house at 100, 64, -200 with fireplace"
        memory.add_summary(level=1, text=summary_1)

    # Level 2: Pattern extraction (1 week old)
    if memory.age >= days(7) and not memory.has_summary(2):
        related = find_related_memories(memory, days=7)
        if len(related) >= 3:
            pattern = await extract_pattern(related)
            # "Player prefers cozy oak houses with fireplaces"
            memory.add_summary(level=2, text=pattern)

    # Level 3: Semantic generalization (1 month old)
    if memory.age >= days(30) and not memory.has_summary(3):
        all_time_related = find_related_memories(memory, all_time=True)
        if len(all_time_related) >= 10:
            generalization = await generalize(all_time_related)
            # "Player likes cozy, warm aesthetics in buildings"
            memory.add_summary(level=3, text=generalization)

            # Move to semantic memory
            memory.agent.learn_fact(
                category="preference",
                key="building_style",
                value=generalization
            )

            # Mark episodic for removal
            memory.consolidated = True
```

### Algorithm 4: Sleep Consolidation

**Purpose:** Mimics biological memory replay during sleep

```python
async def sleep_consolidation_cycle(agent, duration_minutes=30):
    """
    Performs memory consolidation during agent sleep/idle periods.
    """
    start_time = current_time()

    while elapsed_time(start_time) < duration_minutes:
        # Phase 1: Recent memory replay (first 10 minutes)
        if elapsed_time(start_time) < 10:
            # Get recent important memories
            recent = get_recent_memories(hours=24)
            important = [m for m in recent if m.importance > 0.7]

            # Replay by re-encoding
            for memory in random.sample(important, min(5, len(important))):
                await replay_memory(agent, memory)
                await sleep(minutes=1)

        # Phase 2: Pattern extraction (next 10 minutes)
        elif elapsed_time(start_time) < 20:
            # Group related memories
            groups = group_by_semantic_similarity(
                agent.memory.get_all_memories(),
                threshold=0.7
            )

            # Extract patterns from groups
            for group in groups:
                if len(group) >= 3:
                    pattern = await extract_pattern_from_group(group)
                    if pattern:
                        agent.memory.learn_fact(
                            category="learned_pattern",
                            key=pattern.topic,
                            value=pattern.description
                        )

        # Phase 3: Consolidation summaries (last 10 minutes)
        else:
            # Find consolidation candidates
            candidates = get_consolidation_candidates(agent.memory)

            if len(candidates) >= 5:
                # Generate summaries
                for group in group_memories(candidates):
                    summary = await generate_summary(group)
                    agent.memory.learn_fact(
                        category="consolidated",
                        key=group.topic,
                        value=summary
                    )

        await sleep(minutes=2)

async def replay_memory(agent, memory):
    """
    Replays a memory to strengthen it.
    """
    # Re-encode with current context
    new_embedding = await agent.embed(
        memory.description,
        context=agent.current_context
    )

    # Update embedding (strengthens pathway)
    agent.memory.update_embedding(memory.id, new_embedding)

    # Increment access count
    memory.access_count += 1
    memory.last_accessed = current_time()

    # Boost importance slightly
    memory.importance = min(1.0, memory.importance * 1.05)
```

### Algorithm 5: Adaptive Forget Rate

**Purpose:** Dynamically adjusts forgetting based on agent experience

```python
class AdaptiveForgetting:
    def __init__(self):
        self.base_forget_rate = 0.1
        self.memory_pressure = 0.0  # 0.0 to 1.0
        self.recent_errors = 0  # Times agent forgot something important

    def compute_forget_rate(self, memory):
        """
        Computes dynamic forgetting rate for a specific memory.
        """
        # Base rate
        rate = self.base_forget_rate

        # Adjust based on memory pressure
        if self.memory_pressure > 0.8:
            rate *= 2.0  # Forget faster when memory is full
        elif self.memory_pressure < 0.3:
            rate *= 0.5  # Forget slower when plenty of space

        # Adjust based on recent errors
        if self.recent_errors > 5:
            rate *= 0.3  # Forget much slower after forgetting errors

        # Memory-specific adjustments
        if memory.importance > 0.8:
            rate *= 0.1  # Important memories decay very slowly
        elif memory.access_count > 10:
            rate *= 0.3  # Frequently accessed memories decay slowly

        return rate

    def record_error(self):
        """Called when agent forgets something important."""
        self.recent_errors += 1
        # Decay error count over time
        async decay_errors():
            await sleep(hours=1)
            self.recent_errors = max(0, self.recent_errors - 1)

    def update_memory_pressure(self, current_size, max_size):
        """Updates memory pressure based on current usage."""
        self.memory_pressure = current_size / max_size
```

---

## References

### Academic Sources

1. **Ebbinghaus, H. (1885)** - "Memory: A Contribution to Experimental Psychology"
   - Original forgetting curve research

2. **Atkinson, R.C., & Shiffrin, R.M. (1968)** - "Human memory: A proposed system and its control processes"
   - Multi-store model of memory

3. **Baddeley, A. (1974)** - "The episodic buffer: a new component of working memory?"
   - Working memory model

4. **Tulving, E. (1972)** - "Episodic and semantic memory"
   - Distinction between episodic and semantic memory

5. **McClelland, J.L., McNaughton, B.L., & O'Reilly, R.C. (1995)** - "Why there are complementary learning systems in the hippocampus and neocortex"
   - Memory consolidation theory

### AI/ML Sources

6. **MemGPT (UC Berkeley, 2024)**
   - Hierarchical memory for LLM agents
   - https://arxiv.org/abs/2310.08560

7. **Voyager (2023)**
   - Skill learning from experience
   - https://arxiv.org/abs/2305.16291

8. **ReAct (2023)**
   - Reasoning and acting in language models
   - https://arxiv.org/abs/2210.03629

9. **Memory-Augmented Transformers (2025)**
   - Systematic review of memory in transformers
   - https://arxiv.org/html/2508.10824v1

10. **Titans (Google Research, 2024)**
    - Neural memory with learning at test time
    - https://arxiv.org/abs/2401.13987

### Game AI Sources

11. **Ubisoft Companion AI Research (2024)**
    - Challenges in long-term NPC memory
    - https://www.ubisoft.com/en-us/ai-research

12. **Personalized NPCs (MDPI Journal, 2024)**
    - Ebbinghaus-inspired forgetting for game characters
    - https://www.mdpi.com/journal/games

13. **RAG-Based NPC Memory**
    - Retrieval-augmented generation for persistent character memory
    - https://arxiv.org/abs/2305.14314

### Web Resources

14. **Agent快速入门 - 60个AI Agent术语总结 (2025)**
    - Short-term vs long-term memory in agents
    - https://m.blog.csdn.net/weixin_72959097/article/details/148870486

15. **Agent记忆模块详解 (2025)**
    - Engineering practices for agent memory
    - https://m.blog.csdn.net/m0_63171455/article/details/154843125

16. **400篇参考文献重磅综述：人脑×Agent记忆系统 (2025)**
    - Comprehensive review of brain-inspired agent memory
    - https://k.sina.cn/article_3996876140_ee3b7d6c027016bt8.html

17. **MemoryBank Architecture Analysis (2025)**
    - Dynamic forgetting model implementation
    - https://blog.csdn.net/2401_84204413/article/details/152273384

18. **Project Synapse: Hierarchical Multi-Agent Framework (2025)**
    - Hybrid memory architecture integration
    - https://arxiv.org/html/2601.08156v1

---

## Conclusion

Memory consolidation and forgetting are critical capabilities for creating believable, long-lived AI agents. MineWright already has a strong foundation with its hierarchical memory system, importance-based scoring, and basic consolidation service.

**Key Takeaways:**

1. **Biological Inspiration Works** - Techniques from cognitive science (Ebbinghaus curve, interference, sleep replay) translate well to AI systems

2. **Multi-Signal Scoring** - Memory importance should combine time, emotion, access, and interference signals

3. **Progressive Summarization** - Old memories should be compressed, not deleted, preserving knowledge while reducing storage

4. **Adaptive Forgetting** - Forget rates should adjust based on memory pressure and performance

5. **Sleep Consolidation** - Periodic offline processing (even during idle ticks) significantly improves long-term retention

**Next Steps for MineWright:**

1. Implement explicit decay functions with Ebbinghaus curve
2. Add interference-based forgetting using semantic similarity
3. Create idle tick consolidation for sleep-inspired processing
4. Build conversation summarization for working memory compression
5. Add adaptive forgetting based on memory pressure

---

**Document Status:** Complete
**Next Review:** After implementation of Priority 1 enhancements
**Maintained By:** Claude Orchestrator

---
