# New Architecture Systems - Steve AI

**Documentation of Newly Implemented AI Systems**
**Date:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"
**Purpose:** Document behavior trees, embedding models, and HTN planner integration

---

## Table of Contents

1. [Behavior Tree System](#1-behavior-tree-system)
2. [Embedding Model System](#2-embedding-model-system)
3. [HTN Planner System](#3-htn-planner-system)
4. [System Integration Architecture](#4-system-integration-architecture)
5. [Data Flow Diagrams](#5-data-flow-diagrams)
6. [Usage Patterns and Examples](#6-usage-patterns-and-examples)

---

## 1. Behavior Tree System

### 1.1 System Overview

The behavior tree system provides hierarchical, reactive AI decision-making. It complements the existing action system by adding real-time response capabilities and complex behavior composition.

```
BEHAVIOR TREE SYSTEM ARCHITECTURE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                     BEHAVIOR TREE ROOT                              │
│                  (Reactive Task Selection)                          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        COMPOSITE NODES                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │   SequenceNode   │  │   SelectorNode   │  │   ParallelNode   │  │
│  │   (AND logic)    │  │   (OR logic)     │  │  (Concurrent)    │  │
│  │                  │  │                  │  │                  │  │
│  │ Executes all     │  │ Tries until      │  │ Executes all     │  │
│  │ children in      │  │ one succeeds     │  │ simultaneously   │  │
│  │ order, fails     │  │                  │  │ with policy      │
│  │ if any fail      │  │                  │  │                  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        DECORATOR NODES                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │   InverterNode   │  │   RepeaterNode   │  │   CooldownNode   │  │
│  │                  │  │                  │  │                  │  │
│  │ Inverts child    │  │ Repeats child    │  │ Adds cooldown    │  │
│  │ result           │  │ N times          │  │ between runs     │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          LEAF NODES                                 │
│  ┌──────────────────┐  ┌──────────────────┐                         │
│  │   ActionNode     │  │   ConditionNode  │                         │
│  │                  │  │                  │                         │
│  │ Wraps BaseAction │  │ Boolean check    │                         │
│  │ from action      │  │ with lambda      │                         │
│  │ system           │  │                  │                         │
│  └──────────────────┘  └──────────────────┘                         │
└─────────────────────────────────────────────────────────────────────┘

SHARED CONTEXT:
┌─────────────────────────────────────────────────────────────────────┐
│                        BTBlackboard                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Entity Reference: ForemanEntity                             │   │
│  │  Scoped Storage: ConcurrentHashMap<String, Object>         │   │
│  │                                                             │   │
│  │  Example Entries:                                          │   │
│  │    "target.position" → BlockPos(100, 64, 200)              │   │
│  │    "path.current" → 5                                       │   │
│  │    "inventory.has_wood" → true                              │   │
│  │    "combat.last_attack_time" → 1234567890                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 NodeStatus Enum

```
NodeStatus - Behavior Tree Execution States
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│  SUCCESS              │  FAILURE              │  RUNNING            │
│  ───────────────────  │  ───────────────────  │  ─────────────────  │
│                       │                       │                     │
│  Node completed       │  Node failed to       │  Node still         │
│  successfully         │  complete             │  executing          │
│                       │                       │                     │
│  Sequence: Continue   │  Sequence: Stop       │  Sequence: Return   │
│  to next child        │  immediately          │  RUNNING            │
│                       │                       │                     │
│  Selector: Return     │  Selector: Try next   │  Selector: Return   │
│  SUCCESS immediately  │  child                │  RUNNING            │
│                       │                       │                     │
│  Parallel: Policy     │  Parallel: Policy     │  Parallel: Continue │
│  dependent            │  dependent            │  ticking            │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Example Behavior Tree

```
RESOURCE GATHERING BEHAVIOR TREE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                        SelectorNode                                  │
│                     (Try options in order)                           │
└────────────────────────────┬────────────────────────────────────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
    │  Sequence   │   │  Sequence   │   │   Wander    │
    │ (Iron Ore)  │   │ (Coal Ore)  │   │  (Default)  │
    └──────┬──────┘   └──────┬──────┘   └─────────────┘
           │                 │
           ▼                 ▼
    ┌─────────────────────────────────────┐
    │            SequenceNode              │
    │  (Execute in order, fail if any fail)│
    └─────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┐
    │               │               │
    ▼               ▼               ▼
┌─────────┐   ┌─────────┐   ┌─────────┐
│ Condition│  │Condition │  │Action   │
│has_iron │  │near_iron │  │MoveTo   │
│pickaxe  │  │ore      │  │Target   │
└─────────┘   └─────────┘   └─────────┘
                                 │
                                 ▼
                          ┌─────────┐
                          │Action   │
                          │MineBlock│
                          └─────────┘
```

### 1.4 Integration with Action System

```
BEHAVIOR TREE → ACTION SYSTEM BRIDGE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                        ActionNode                                    │
│                      (Leaf Node)                                    │
├─────────────────────────────────────────────────────────────────────┤
│  Wraps: BaseAction                                                  │
│  tick() → action.tick()                                             │
│  isComplete() → action.isComplete()                                 │
│  getResult() → action.getResult()                                   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   BaseAction System                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Action Registry → Action Factory → BaseAction Instance    │   │
│  │                                                             │   │
│  │  Actions:                                                   │   │
│  │  • MineBlockAction                                          │   │
│  │  • PlaceBlockAction                                         │   │
│  │  • NavigateAction                                           │   │
│  │  • BuildStructureAction                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

RESULT MAPPING:
┌─────────────────────────────────────────────────────────────────────┐
│  ActionResult        │  NodeStatus                                  │
│  ─────────────────   │  ─────────────────                           │
│  success             │  SUCCESS                                     │
│  failure (any)       │  FAILURE                                     │
│  running             │  RUNNING                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Embedding Model System

### 2.1 System Overview

The embedding model system provides semantic text vectorization for memory retrieval, caching, and similarity search.

```
EMBEDDING MODEL SYSTEM ARCHITECTURE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                     CompositeEmbeddingModel                          │
│                   (Primary + Fallback)                              │
├─────────────────────────────────────────────────────────────────────┤
│  Primary: LocalEmbeddingModel (ONNX)                                │
│  Fallback: OpenAIEmbeddingModel (API)                               │
└────────────────────────────┬────────────────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
┌─────────────────────────────┐  ┌─────────────────────────────────┐
│   LocalEmbeddingModel       │  │   OpenAIEmbeddingModel          │
│   (ONNX Runtime)            │  │   (text-embedding-3-small)      │
├─────────────────────────────┤  ├─────────────────────────────────┤
│ • Zero API cost             │  │ • 1536 dimensions               │
│ • Fast inference            │  │ • LRU cache with TTL            │
│ • Sentence-BERT style       │  │ • Batch support (100 texts)     │
│ • Works offline            │  │ • Circuit breaker               │
│ • 384 dimensions           │  │ • Retry with exponential backoff │
└─────────────────────────────┘  │ • Resilience4j patterns         │
                                └─────────────────────────────────┘

SHARED INTERFACE:
┌─────────────────────────────────────────────────────────────────────┐
│                        EmbeddingModel                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  float[] embed(String text)                                │   │
│  │  CompletableFuture<float[]> embedAsync(String text)         │   │
│  │  float[][] embedBatch(String[] texts)                      │   │
│  │  int getDimension()                                        │   │
│  │  String getModelName()                                     │   │
│  │  boolean isAvailable()                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 OpenAI Embedding Model

```
OPENAI EMBEDDING MODEL WITH RESILIENCE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                      embed(text)                                     │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      CACHE CHECK                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  ConcurrentHashMap<Integer, CacheEntry> cache               │   │
│  │                                                             │   │
│  │  CacheEntry:                                                │   │
│  │    - float[] embedding                                      │   │
│  │    - long timestamp                                         │   │
│  │                                                             │   │
│  │  LRU Eviction: ConcurrentLinkedDeque accessOrder            │   │
│  │  TTL: 1 hour                                                │   │
│  │  Max Size: 1000 entries                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                    Cache Miss? │ Cache Hit
                             │         │
                             ▼         ▼
┌─────────────────────────┐   ┌─────────────────────────────────────┐
│   RESILIENCE LAYER      │   │  Return cached embedding             │
│  ┌──────────────────┐  │   │  Update access order (LRU)           │
│  │  Circuit        │  │   │  Increment cache hit counter          │
│  │  Breaker        │  │   └─────────────────────────────────────┘
│  │  (Open state →  │
│  │   fast fail)    │
│  └────────┬─────────┘
│           │
│  ┌────────┴─────────┐
│  │  Retry          │
│  │  (3 attempts,   │
│  │   exponential   │
│  │   backoff)      │
│  └────────┬─────────┘
└───────────┼───────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      HTTP REQUEST                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  POST https://api.openai.com/v1/embeddings                  │   │
│  │  Headers:                                                   │   │
│  │    Authorization: Bearer <API_KEY>                          │   │
│  │    Content-Type: application/json                            │   │
│  │  Body:                                                      │   │
│  │    {                                                        │   │
│  │      "model": "text-embedding-3-small",                     │   │
│  │      "input": "text to embed"                               │   │
│  │    }                                                        │   │
│  │  Timeout: 30 seconds                                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      RESPONSE PARSING                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Parse JSON → Extract embedding vector (1536 floats)       │   │
│  │  Track token usage (for cost monitoring)                    │   │
│  │  Cache result                                               │   │
│  │  Return embedding                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

CIRCUIT BREAKER CONFIGURATION:
┌─────────────────────────────────────────────────────────────────────┐
│  Sliding Window: 10 calls                                          │
│  Failure Threshold: 50%                                            │
│  Open State Duration: 30 seconds                                   │
│  Half-Open Permitted Calls: 3                                      │
│  Recorded Exceptions: IOException, TimeoutException                │
└─────────────────────────────────────────────────────────────────────┘

RETRY CONFIGURATION:
┌─────────────────────────────────────────────────────────────────────┐
│  Max Attempts: 3                                                    │
│  Initial Backoff: 1000ms                                           │
│  Backoff Multiplier: 2x (exponential)                              │
│  Retry On: IOException, TimeoutException                           │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 Embedding Usage in Memory System

```
EMBEDDING → VECTOR STORE INTEGRATION
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                    CompanionMemory                                  │
│                  (Memory Storage System)                            │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      MEMORY STORAGE                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Conversation Memory:                                        │   │
│  │    - List<Message> messages                                   │   │
│  │    - Embed each message for semantic search                   │   │
│  │                                                             │   │
│  │  World Knowledge:                                             │   │
│  │    - Map<String, Object> facts                                │   │
│  │    - Embed queries for relevant fact retrieval                │   │
│  │                                                             │   │
│  │  Skill Library:                                               │   │
│  │    - Cached task sequences                                    │   │
│  │    - Embed for similarity-based skill retrieval               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   SEMANTIC RETRIEVAL                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Query: "How do I build a roof?"                             │   │
│  │                                                             │   │
│  │  1. Embed query: float[] queryVector = model.embed(query)    │   │
│  │                                                             │   │
│  │  2. Cosine similarity search:                                │   │
│  │     similarity = dot(query, stored) / (||query|| * ||stored||)│   │
│  │                                                             │   │
│  │  3. Return top K most similar memories                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

CACHE INTEGRATION:
┌─────────────────────────────────────────────────────────────────────┐
│                    LLMCache + Embeddings                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Cache Key: SHA-256(prompt + context)                        │   │
│  │  Cache Value: LLMResponse                                     │   │
│  │                                                             │   │
│  │  SEMANTIC CACHE LOOKUP:                                      │   │
│  │    1. Embed current prompt                                    │   │
│  │    2. Find cached prompts with >0.90 similarity              │   │
│  │    3. If found, return cached response                        │   │
│  │    4. Otherwise, make LLM call and cache result               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. HTN Planner System

### 3.1 System Overview

Hierarchical Task Network (HTN) planning decomposes high-level compound tasks into primitive executable actions using forward-chaining decomposition.

```
HTN PLANNER ARCHITECTURE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                         HTNPlanner                                   │
│                      (Planning Engine)                               │
├─────────────────────────────────────────────────────────────────────┤
│  decompose(task, worldState) → List<HTNTask>                        │
│                                                             │   │
│  Algorithm: Forward decomposition with backtracking                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  decomposeRecursive(task, state, depth):                     │   │
│  │    if task.isPrimitive(): return [task]  // Base case        │   │
│  │    if task.isCompound():                                     │   │
│  │      methods = domain.getApplicableMethods(task, state)      │   │
│  │      for method in methods (by priority):                    │   │
│  │        subtasks = []                                         │   │
│  │        for subtask in method.subtasks:                       │   │
│  │          decomposed = decomposeRecursive(subtask, ...)       │   │
│  │          if decomposed == null: break  // Backtrack          │   │
│  │          subtasks.addAll(decomposed)                         │   │
│  │        return subtasks  // Success                           │   │
│  │      return null  // All methods failed                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          HTNDomain                                   │
│                    (Method Library)                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Compound Task → List<HTNMethod>                             │   │
│  │                                                             │   │
│  │  "build_house" → [                                           │   │
│  │    Method1: Use materials from inventory (priority: 10)     │   │
│  │    Method2: Gather materials then build (priority: 5)       │   │
│  │  ]                                                           │   │
│  │                                                             │   │
│  │  "gather_wood" → [                                           │   │
│  │    Method1: Use nearest tree (priority: 10)                 │   │
│  │    Method2: Plant sapling then harvest (priority: 1)        │   │
│  │  ]                                                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 Task Decomposition Example

```
HTN DECOMPOSITION: "build_house"
========================================================================

STEP 1: Initial Compound Task
┌─────────────────────────────────────────────────────────────────────┐
│  HTNTask                                                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Name: build_house                                          │   │
│  │  Type: COMPOUND                                            │   │
│  │  Parameters: {material: "oak_planks", size: "5x5"}         │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
STEP 2: Get Applicable Methods
┌─────────────────────────────────────────────────────────────────────┐
│  getApplicableMethods("build_house", worldState)                   │
│                                                             │   │
│  Methods found:                                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Method: build_from_inventory                               │   │
│  │  Priority: 10                                              │   │
│  │  Preconditions: {has_materials: true}                        │   │
│  │  Subtasks: [navigate, build_walls, build_roof, place_door]  │   │
│  │                                                             │   │
│  │  Method: gather_and_build                                   │   │
│  │  Priority: 5                                               │   │
│  │  Preconditions: {} (always available)                       │   │
│  │  Subtasks: [gather_materials, navigate, build_walls, ...]   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
STEP 3: Try Method 1 (build_from_inventory)
┌─────────────────────────────────────────────────────────────────────┐
│  Checking preconditions...                                         │
│  worldState.getProperty("has_materials") = false                   │
│  PRECONDITION FAILED → Try next method                             │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
STEP 4: Try Method 2 (gather_and_build)
┌─────────────────────────────────────────────────────────────────────┐
│  Preconditions: None → PASS                                        │
│                                                             │   │
│  Decomposing subtasks:                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  1. gather_materials (COMPOUND)                             │   │
│  │     → Decompose recursively...                              │   │
│  │     → [mine_oak_log, craft_planks, mine_oak_log, ...]      │   │
│  │                                                             │   │
│  │  2. navigate (PRIMITIVE)                                    │   │
│  │     → Already executable                                    │   │
│  │                                                             │   │
│  │  3. build_walls (COMPOUND)                                  │   │
│  │     → Decompose recursively...                              │   │
│  │     → [place_block_at_0_0_0, place_block_at_1_0_0, ...]   │   │
│  │                                                             │   │
│  │  4. build_roof (COMPOUND)                                   │   │
│  │     → Decompose recursively...                              │   │
│  │                                                             │   │
│  │  5. place_door (PRIMITIVE)                                  │   │
│  │     → Already executable                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
STEP 5: Final Primitive Task Sequence
┌─────────────────────────────────────────────────────────────────────┐
│  [mine_oak_log, mine_oak_log, ...,                                  │
│   craft_oak_planks, craft_oak_planks, ...,                         │
│   navigate_to_site,                                                │
│   place_block_at_0_0_0, place_block_at_1_0_0, ...,                 │
│   place_block_at_0_4_0, place_block_at_1_4_0, ...,                 │
│   place_door]                                                       │
│                                                             │   │
│  47 primitive tasks → Action Registry → BaseAction instances       │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.3 HTN vs Behavior Tree vs LLM

```
PLANNING SYSTEM COMPARISON
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│  HTN PLANNER                  │  BEHAVIOR TREE        │  LLM PLAN   │
│  ─────────────────────────   │  ──────────────────   │  ────────── │
│                               │                       │             │
│  Hierarchical decomposition   │  Reactive execution   │  One-shot   │
│  Forward chaining             │  Tick-based           │  generation │
│                               │                       │             │
│  Deterministic                │  Deterministic        │  Probabilistic│
│  (given state)                │  (logic-based)        │             │
│                               │                       │             │
│  Fast                         │  Very fast            │  Slow       │
│  (method lookup)              │  (logic evaluation)   │  (API call) │
│                               │                       │             │
│  Best for:                    │  Best for:            │  Best for:  │
│  - Known patterns             │  - Reactive response  │  - Novel    │
│  - Structured tasks           │  - Real-time logic    │    tasks    │
│  - Multi-step procedures      │  - Interruptible      │  - Complex  │
│                               │    actions            │    planning │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. System Integration Architecture

### 4.1 Three-Layer Hybrid with New Systems

```
UPDATED THREE-LAYER HYBRID ARCHITECTURE
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                    LAYER 1: DIALOGUE & UNDERSTANDING                 │
│  ─────────────────────────────────────────────────────────────────  │
│  • Dialogue State Machine handles player input                      │
│  • Intent classification routes to appropriate handler               │
│  • Conversation memory for context tracking                          │
│  • LLM used for complex/ambiguous commands                           │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    LAYER 2: PLANNING & DECOMPOSITION                 │
│  ─────────────────────────────────────────────────────────────────  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  CASCADE ROUTER: Analyzes task complexity                    │   │
│  │  ┌───────────────────────────────────────────────────────┐ │   │
│  │  │ TRIVIAL/SIMPLE → HTN Planner (fast, deterministic)    │ │   │
│  │  │ MODERATE → Behavior Tree (reactive, interruptible)    │ │   │
│  │  │ COMPLEX → LLM One-Shot (flexible, expensive)          │ │   │
│  │  └───────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  HTN PLANNER: Common patterns (build, mine, craft)          │   │
│  │  → Decomposes compound tasks into primitive actions          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  BEHAVIOR TREE: Reactive task selection                     │   │
│  │  → Handles dynamic environments, interruptions               │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  LLM FALLBACK: Novel tasks, complex planning                 │   │
│  │  → Generates JSON task sequence                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  SKILL LIBRARY: Cache successful patterns                    │   │
│  │  → Indexed by embeddings for semantic retrieval              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    LAYER 3: EXECUTION & COORDINATION                 │
│  ─────────────────────────────────────────────────────────────────  │
│  • Action Registry creates actions from tasks                        │
│  • State Machine tracks agent state                                  │
│  • Interceptor Chain provides AOP capabilities                       │
│  • Tick-based execution (20/sec) for non-blocking                    │
│  • Event Bus for loose coupling                                      │
│  • Behavior Tree blackboard for context                              │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Cascade Router Integration

```
CASCADE ROUTER WITH HTN AND BEHAVIOR TREES
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                         USER COMMAND                                 │
│                      "Build a Minecraft house"                       │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      COMPLEXITY ANALYZER                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  analyzeComplexity("Build a Minecraft house")              │   │
│  │  → TaskComplexity.SIMPLE (well-known pattern)               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      CACHE CHECK                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Semantic search with embeddings:                           │   │
│  │  queryEmbedding = model.embed("build house")                │   │
│  │  similarity = cosine(query, cached_prompts)                 │   │
│  │                                                             │   │
│  │  Result: CACHE MISS (0.85 < 0.90 threshold)                │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      PLANNING SYSTEM SELECTION                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  TaskComplexity.SIMPLE → HTN PLANNER                        │   │
│  │                                                             │   │
│  │  Why HTN?                                                   │   │
│  │  • "build_house" is a known pattern in HTN domain           │   │
│  │  • Deterministic decomposition (no LLM needed)              │   │
│  │  • Fast (method lookup, not API call)                       │   │
│  │  • Produces 47 primitive tasks                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      HTN PLANNER                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Input: build_house (compound task)                         │   │
│  │  World State: {has_materials: false, has_axe: true}         │   │
│  │                                                             │   │
│  │  Method Selection:                                          │   │
│  │    Method1: build_from_inventory → FAILED (no materials)   │   │
│  │    Method2: gather_and_build → SUCCESS                      │   │
│  │                                                             │   │
│  │  Output: 47 primitive tasks                                 │   │
│  │  [mine_oak_log x16, craft_planks x64, ..., build_walls,    │   │
│  │   build_roof, place_door]                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      WRAP IN BEHAVIOR TREE                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Create reactive wrapper for HTN plan:                      │   │
│  │                                                             │   │
│  │  BTNode executionTree = new SequenceNode(                   │   │
│  │    new ActionNode(new NavigateAction(targetPos)),           │   │
│  │    new SelectorNode(                                       │   │
│  │      new SequenceNode(                                     │   │
│  │        new ConditionNode(() → hasMaterials()),             │   │
│  │        new ActionNode(new BuildWallsAction())              │   │
│  │      ),                                                     │   │
│  │      new ActionNode(new GatherMaterialsAction())           │   │
│  │    )                                                        │   │
│  │  );                                                         │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      EXECUTE WITH BT BLACKBOARD                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  BTBlackboard blackboard = new BTBlackboard(foremanEntity); │   │
│  │                                                             │   │
│  │  while (true) {                                             │   │
│  │    NodeStatus status = executionTree.tick(blackboard);     │   │
│  │    if (status == NodeStatus.SUCCESS) break;                │   │
│  │    if (status == NodeStatus.FAILURE) { replan(); break; }  │   │
│  │    Thread.sleep(50); // 20 ticks per second                 │   │
│  │  }                                                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 Embedding Model in Cache

```
SEMANTIC CACHE WITH EMBEDDINGS
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                      CACHE REQUEST                                  │
│  Prompt: "Build a small wooden house"                               │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      EXACT MATCH CHECK                              │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Key: SHA-256("Build a small wooden house" + context)       │   │
│  │  Result: NOT FOUND                                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SEMANTIC SEARCH                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  1. Generate query embedding:                                │   │
│  │     queryVector = model.embed("Build a small wooden house")  │   │
│  │                                                             │   │
│  │  2. Calculate similarity with cached prompts:               │   │
│  │     similarity = cosine(queryVector, cachedPromptVector)     │   │
│  │                                                             │   │
│  │  Cached Prompts:                                             │   │
│  │    "construct a wooden shelter" → 0.92 similarity ✓         │   │
│  │    "build an oak house" → 0.88 similarity                   │   │
│  │    "create a castle" → 0.65 similarity                      │   │
│  │                                                             │   │
│  │  3. Return cached response if > 0.90 threshold              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                    SEMANTIC HIT │ SEMANTIC MISS
                             │         │
                             ▼         ▼
┌─────────────────────────┐   ┌─────────────────────────────────────┐
│  Return cached         │   │  Route to LLM/HTN/BT               │
│  response              │   │  Cache the result with embedding    │
└─────────────────────────┘   └─────────────────────────────────────┘
```

---

## 5. Data Flow Diagrams

### 5.1 Complete Planning Flow

```
COMPLETE PLANNING DATA FLOW
========================================================================

USER INPUT
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PARSE INTENT                                                       │
│  "Build a house" → Intent: BUILD_STRUCTURE                         │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CHECK SKILL LIBRARY (with embeddings)                              │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Embed query → Search skill embeddings → Find match?        │   │
│  │  "build house" → 0.95 match → "build_oak_house" skill       │   │
│  │  → Return 47 pre-planned tasks (instant!)                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼ (if no skill match)
┌─────────────────────────────────────────────────────────────────────┐
│  ANALYZE COMPLEXITY                                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Single task? NO                                            │   │
│  │  Well-known pattern? YES                                    │   │
│  │  Requires coordination? NO                                  │   │
│  │  Novel approach? NO                                         │   │
│  │  → TaskComplexity.SIMPLE                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SELECT PLANNING SYSTEM                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  SIMPLE → HTN Planner (deterministic, fast)                 │   │
│  │  MODERATE → Behavior Tree (reactive, interruptible)         │   │
│  │  COMPLEX → LLM (flexible, expensive)                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼ (SIMPLE → HTN)
┌─────────────────────────────────────────────────────────────────────┐
│  HTN DECOMPOSITION                                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  build_house → gather_and_build method → 47 primitive tasks│   │
│  │  [mine, craft, navigate, build_walls, build_roof, door]    │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  WRAP IN BEHAVIOR TREE (for reactivity)                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  SequenceNode(                                               │   │
│  │    SelectorNode(                                             │   │
│  │      SequenceNode(hasMaterials, buildWalls),                │   │
│  │      gatherMaterials                                          │   │
│  │    ),                                                         │   │
│  │    buildRoof,                                                 │   │
│  │    placeDoor                                                  │   │
│  │  )                                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CREATE ACTIONS                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  ActionRegistry.create(task) → BaseAction                   │   │
│  │  • MineBlockAction                                           │   │
│  │  • CraftItemAction                                           │   │
│  │  • NavigateAction                                            │   │
│  │  • BuildStructureAction                                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  TICK-BASED EXECUTION                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  while (!action.isComplete()) {                             │   │
│  │    action.tick();  // 20 times per second                   │   │
│  │  }                                                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 Behavior Tree Tick Flow

```
BEHAVIOR TREE EXECUTION FLOW
========================================================================

GAME TICK (20 per second)
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  UPDATE BT BLACKBOARD                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  - Sync entity position                                     │   │
│  │  - Update inventory state                                   │   │
│  │  - Check environment conditions                             │   │
│  │  - Update combat state                                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  TICK ROOT NODE                                                     │
│  root.tick(blackboard) → NodeStatus                                 │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SEQUENCE NODE (Execute all children)                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  For each child:                                             │   │
│  │    status = child.tick(blackboard)                           │   │
│  │    if status == FAILURE: return FAILURE                      │   │
│  │    if status == RUNNING: return RUNNING                      │   │
│  │    if status == SUCCESS: continue to next child              │   │
│  │  return SUCCESS (all children succeeded)                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SELECTOR NODE (Try until success)                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  For each child:                                             │   │
│  │    status = child.tick(blackboard)                           │   │
│  │    if status == SUCCESS: return SUCCESS                      │   │
│  │    if status == RUNNING: return RUNNING                      │   │
│  │    if status == FAILURE: try next child                      │   │
│  │  return FAILURE (all children failed)                        │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ACTION NODE (Execute wrapped action)                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  if (!started) { action.start(); started = true; }          │   │
│  │  action.tick();                                              │   │
│  │  if (action.isComplete()) {                                  │   │
│  │    return action.isSuccess() ? SUCCESS : FAILURE;           │   │
│  │  }                                                           │   │
│  │  return RUNNING;                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CONDITION NODE (Check predicate)                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  boolean result = condition.test();                          │   │
│  │  return result ? SUCCESS : FAILURE;                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. Usage Patterns and Examples

### 6.1 Reactive Behavior with Interruptions

```
BEHAVIOR TREE: COMBAT RESPONSE WITH INTERRUPTS
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│                        SelectorNode                                  │
│                     (Priority-based response)                       │
└────────────────────────────┬────────────────────────────────────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
    │  Sequence   │   │  Sequence   │   │  Sequence   │
    │  (Combat)   │   │ (Flee)      │   │  (Idle)     │
    └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
           │                 │                 │
           ▼                 ▼                 ▼
    ┌─────────────────────────────────────────────────────┐
    │ SequenceNode                                         │
    │  1. Condition: isInCombat() → SUCCESS/FAILURE        │
    │  2. Condition: hasWeapon() → SUCCESS/FAILURE         │
    │  3. Action: AttackAction() → RUNNING → SUCCESS       │
    └─────────────────────────────────────────────────────┘

INTERRUPTION SCENARIO:
    │
    │ Agent is mining (Idle sequence running)
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  THREAT DETECTED                                                    │
│  blackboard.put("combat.in_combat", true)                          │
└─────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  NEXT TICK: Combat Sequence selected                                │
│  - Mining action cancelled                                          │
│  - Attack action started                                            │
│  - Reactive response in < 50ms                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 HTN for Structured Building

```
HTN: STRUCTURED BUILDING DECOMPOSITION
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│  COMPOUND TASK: build_structure                                     │
│  Parameters: {type: "house", material: "oak_planks", size: "5x5"}   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  METHOD: build_from_scratch                                         │
│  Priority: 10                                                       │
│  Preconditions: {} (always available)                               │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SUBTASKS (in order):                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  1. gather_materials (COMPOUND)                              │   │
│  │     → [mine_logs, craft_planks]                              │   │
│  │                                                             │   │
│  │  2. prepare_site (COMPOUND)                                  │   │
│  │     → [navigate_to_site, clear_area, level_ground]           │   │
│  │                                                             │   │
│  │  3. build_foundation (PRIMITIVE)                             │   │
│  │     → place_blocks at y=0                                    │   │
│  │                                                             │   │
│  │  4. build_walls (PRIMITIVE)                                  │   │
│  │     → place_blocks at y=1 to y=3                            │   │
│  │                                                             │   │
│  │  5. build_roof (COMPOUND)                                    │   │
│  │     → [place_roof_blocks, add_details]                       │   │
│  │                                                             │   │
│  │  6. add_furniture (COMPOUND)                                 │   │
│  │     → [place_door, place_windows, place_decor]               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  FULLY DECOMPOSED: 87 primitive actions                              │
│  [mine_log, mine_log, ..., craft_planks, ..., navigate, clear,     │
│   place_block_0_0_0, place_block_1_0_0, ..., place_door, ...]     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.3 Semantic Skill Retrieval

```
SKILL LIBRARY: EMBEDDING-BASED RETRIEVAL
========================================================================

┌─────────────────────────────────────────────────────────────────────┐
│  STORE SKILL                                                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Task: "build simple oak house"                             │   │
│  │  Result: 47 primitive tasks (from HTN)                      │   │
│  │                                                             │   │
│  │  1. Generate embedding:                                     │   │
│  │     skillEmbedding = model.embed("build simple oak house")  │   │
│  │                                                             │   │
│  │  2. Store in skill library:                                 │   │
│  │     skills.put(skillId, {                                    │   │
│  │       name: "build_simple_oak_house",                        │   │
│  │       embedding: skillEmbedding,                             │   │
│  │       tasks: taskList,                                       │   │
│  │       usage_count: 1                                         │   │
│  │     })                                                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  RETRIEVE SKILL                                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Query: "construct a wooden shelter"                        │   │
│  │                                                             │   │
│  │  1. Generate query embedding:                                │   │
│  │     queryEmbedding = model.embed("construct a wooden        │   │
│  │       shelter")                                              │   │
│  │                                                             │   │
│  │  2. Calculate similarities:                                  │   │
│  │     for (skill : skills) {                                   │   │
│  │       similarity = cosine(queryEmbedding,                   │   │
│  │         skill.embedding)                                     │   │
│  │     }                                                        │   │
│  │                                                             │   │
│  │  3. Find best match:                                         │   │
│  │     build_simple_oak_house: 0.92 similarity ✓               │   │
│  │     build_castle: 0.65 similarity                            │   │
│  │                                                             │   │
│  │  4. Return cached tasks (instant, no planning!)              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Conclusion

The new architecture systems enhance Steve AI's capabilities significantly:

1. **Behavior Trees**: Enable reactive, interruptible AI with real-time decision-making
2. **Embedding Models**: Provide semantic memory retrieval and caching for 60-80% hit rates
3. **HTN Planner**: Offers deterministic, fast decomposition of known patterns

These systems integrate seamlessly with the existing "One Abstraction Away" architecture, maintaining the core principle of **one LLM call per task** while adding layers of efficiency and reactivity.

---

**Related Documents:**
- [ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md) - Core architecture diagrams
- [CHAPTER_8_ARCHITECTURE_COMPARISON.md](CHAPTER_8_ARCHITECTURE_COMPARISON.md) - Framework comparison
- [BEHAVIOR_TREES_DESIGN.md](BEHAVIOR_TREES_DESIGN.md) - Behavior tree implementation details
- [HTN_PLANNER.md](HTN_PLANNER.md) - HTN system documentation (if exists)
