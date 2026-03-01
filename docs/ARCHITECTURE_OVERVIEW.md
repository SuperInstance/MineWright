# Steve AI - Architecture Overview

**Version:** 2.0
**Last Updated:** 2026-02-28
**Project:** MineWright Mod for Minecraft 1.20.1

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Component Diagram](#component-diagram)
3. [Core Components](#core-components)
4. [Data Flow](#data-flow)
5. [Concurrency Model](#concurrency-model)
6. [Plugin System](#plugin-system)
7. [Key Design Patterns](#key-design-patterns)

---

## System Overview

### "One Abstraction Away" Philosophy

Steve AI implements a **three-layer architecture** where LLMs plan and coordinate, while traditional game AI executes in real-time. This separation creates agents that are:

- **Fast** - 60 FPS execution without blocking on LLM calls
- **Cost-efficient** - 10-20x fewer tokens than pure LLM approaches
- **Characterful** - Rich personalities, ongoing dialogue, relationship evolution

### Three-Layer Architecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                               │
│                         LLM Agents                                        │
│                                                                           │
│   Planning, strategy, logistics                                          │
│   Conversations with player and other agents                             │
│   Creating and refining automation scripts                               │
│   High-level goal setting                                                 │
│                                                                           │
│   Token Usage: LOW (batched, infrequent calls)                           │
│   Update Frequency: Every 30-60 seconds or on events                      │
└────────────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ Generates & Refines
                                   ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                              │
│                    Behavior Automations                                   │
│                                                                           │
│   Behavior trees, FSMs, macro scripts                                     │
│   Pathfinding, mining, building patterns                                  │
│   Combat AI, resource gathering, exploration                              │
│   Reactive behaviors (danger response, opportunities)                     │
│   Idle behaviors (wandering, chatting, self-improvement)                 │
│                                                                           │
│   Token Usage: ZERO (runs locally)                                        │
│   Update Frequency: Every tick (20 TPS)                                   │
└────────────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ Executes via
                                   ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                                 │
│                     Minecraft API                                         │
│                                                                           │
│   Block interactions, movement, inventory                                 │
│   Entity tracking, world sensing                                          │
│   Direct game API calls                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Principles

| Principle | Implementation | Benefit |
|-----------|----------------|---------|
| **Non-blocking LLM** | `CompletableFuture` with async clients | Server never freezes during AI planning |
| **Tick-based execution** | `BaseAction.tick()` called every game tick | Smooth 60 FPS agent behavior |
| **Lock-free coordination** | `ConcurrentHashMap`, `AtomicBoolean` | No deadlocks, high throughput |
| **Event-driven communication** | `EventBus` for pub/sub messaging | Loose coupling between components |
| **Plugin architecture** | `ActionRegistry` with factory pattern | Extensible without modifying core code |

---

## Component Diagram

### High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            HUMAN PLAYER                                         │
└─────────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ Natural Language Command
                                     │ "/foreman order Steve build a house"
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         ACTION EXECUTOR                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │  Task Queue (LinkedBlockingQueue)                                       │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
│  ┌────────────────┐    ┌───────────────┐    ┌──────────────────────────────┐  │
│  │ State Machine  │───▶│ Interceptor   │───▶│       Action Registry        │  │
│  │ (IDLE/PLANNING/ │    │    Chain      │    │   (Plugin Action Factories)  │  │
│  │  EXECUTING)     │    │              │    │                              │  │
│  └────────────────┘    └───────────────┘    └──────────────────────────────┘  │
│                                     │                                          │
│                                     ▼                                          │
│                        ┌────────────────────┐                                  │
│                        │  Current Action    │                                  │
│                        │  (BaseAction)      │                                  │
│                        └────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ tick() every 50ms
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           ACTION EXECUTION LAYERS                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        PHYSICAL ACTIONS                                  │  │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌──────────────┐            │  │
│  │  │ Pathfind  │ │  Mine     │ │  Place    │ │  Craft      │ ...         │  │
│  │  │   Action  │ │  Action   │ │  Action   │ │   Action     │             │  │
│  │  └───────────┘ └───────────┘ └───────────┘ └──────────────┘             │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        SCRIPT LAYER                                      │  │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────┐  │  │
│  │  │   Behavior  │    │     HTN     │    │     Pathfinding             │  │  │
│  │  │     Tree    │    │   Planner   │    │   (A* + Hierarchical)       │  │  │
│  │  └─────────────┘    └─────────────┘    └─────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ Minecraft API Calls
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          MINECRAFT SERVER                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                     WORLD STATE                                           │  │
│  │  Block positions, entities, inventories, player state                   │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### LLM Integration Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            BRAIN LAYER                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                         CASCADE ROUTER                                   │  │
│  │   Analyzes task complexity → Routes to appropriate LLM tier             │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                      │
│                    ┌────────────────────┼────────────────────┐                │
│                    ▼                    ▼                    ▼                │
│  ┌───────────────────────┐ ┌───────────────────────┐ ┌─────────────────────┐ │
│  │     SIMPLE TASKS      │ │    COMPLEX TASKS      │ │    NOVEL TASKS      │ │
│  │   (GLM-4.7-Air)       │ │      (GLM-5)          │ │     (GPT-4)         │ │
│  │                       │ │                       │ │                     │ │
│  │   - Simple commands   │ │   - Multi-step plans  │ │   - Edge cases      │ │
│  │   - Pattern matching  │ │   - Coordination      │ │   - Fallback         │ │
│  │   - Cached queries    │ │   - Dialogue          │ │   - Learning        │ │
│  └───────────────────────┘ └───────────────────────┘ └─────────────────────┘ │
│                                         │                                      │
│                                         ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                      RESILIENCE LAYER                                    │  │
│  │  ┌────────────┐ ┌──────────┐ ┌────────────┐ ┌──────────┐ ┌─────────┐  │  │
│  │  │   Cache    │ │   Retry  │ │Circuit Br. │ │Rate Limit│ │ Bulkhead│  │  │
│  │  │ (40-60%    │ │ (Exp     │ │ (Fail fast)│ │ (Quota   │ │ (Concur)│  │  │
│  │  │  hit rate) │ │  backoff)│ │           │ │  mgmt)   │ │         │  │  │
│  │  └────────────┘ └──────────┘ └────────────┘ └──────────┘ └─────────┘  │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                      │
│                                         ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                      BATCHING LAYER                                       │  │
│  │   Aggregates background tasks to respect rate limits                     │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Multi-Agent Orchestration

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      ORCHESTRATION SERVICE                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                     AGENT COMMUNICATION BUS                              │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │ Task Assign  │  │ Progress     │  │ Help Request │                 │  │
│  │  │ Messages     │  │ Updates      │  │ Messages     │                 │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                 │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                      │
│                    ┌─────────────────────┼─────────────────────┐              │
│                    ▼                     ▼                     ▼              │
│  ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐ │
│  │      FOREMAN        │   │      WORKER 1       │   │      WORKER 2       │ │
│  │  (Orchestrator)     │   │   (Specialist)      │   │   (Specialist)      │ │
│  │                     │   │                     │   │                     │ │
│  │  - Receives player  │   │  - Receives tasks   │   │  - Receives tasks   │ │
│  │    commands         │   │  - Executes actions │   │  - Executes actions │ │
│  │  - Decomposes plan  │   │  - Reports progress │   │  - Reports progress │ │
│  │  - Assigns tasks    │   │  - Requests help    │   │  - Requests help    │ │
│  └─────────────────────┘   └─────────────────────┘   └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. ActionExecutor

**Location:** `src/main/java/com/minewright/action/ActionExecutor.java`

**Purpose:** Central execution engine that manages task queues and executes actions tick-by-tick.

**Key Features:**
- **Non-blocking LLM calls:** Uses `CompletableFuture` for async planning
- **Tick-based execution:** Calls `action.tick()` every game tick (20 TPS)
- **Plugin architecture:** Creates actions via `ActionRegistry`
- **State machine:** Tracks agent state (IDLE, PLANNING, EXECUTING, WAITING, ERROR)
- **Interceptor chain:** Cross-cutting concerns (logging, metrics, events)
- **Tick budget enforcement:** Limits AI operation time to prevent server lag

**Threading Model:**
```java
// Game thread calls tick() every 50ms
public void tick() {
    // 1. Check if async LLM planning completed (non-blocking)
    if (isPlanning.get() && planningFuture != null && planningFuture.isDone()) {
        ResponseParser.ParsedResponse response = planningFuture.getNow(null);
        // Process results and queue tasks
    }

    // 2. Execute current action if running
    if (currentAction != null && !currentAction.isComplete()) {
        currentAction.tick();  // Non-blocking action progress
    }

    // 3. Start next task from queue if ready
    if (!taskQueue.isEmpty() && ticksSinceLastAction >= ACTION_TICK_DELAY) {
        Task nextTask = taskQueue.poll();
        executeTask(nextTask);
    }
}
```

**Why This Design:**
- Prevents server freezing during LLM calls (30-60 seconds)
- Allows multiple agents to run concurrently without blocking
- Provides predictable performance with tick budget enforcement

### 2. AgentStateMachine

**Location:** `src/main/java/com/minewright/execution/AgentStateMachine.java`

**Purpose:** Manages agent state transitions with explicit validation.

**Valid State Transitions:**
```
IDLE → PLANNING (new command received)
PLANNING → EXECUTING (planning complete)
PLANNING → FAILED (planning error)
EXECUTING → COMPLETED (all tasks done)
EXECUTING → FAILED (execution error)
EXECUTING → PAUSED (user pause request)
PAUSED → EXECUTING (resume)
PAUSED → IDLE (cancel)
COMPLETED → IDLE (ready for next command)
FAILED → IDLE (reset after error)
```

**Thread Safety:**
```java
// AtomicReference for lock-free state updates
private final AtomicReference<AgentState> currentState;

// Compare-and-set for thread-safe transitions
public boolean transitionTo(AgentState targetState) {
    AgentState fromState = currentState.get();
    if (!canTransitionTo(targetState)) {
        return false;
    }
    if (currentState.compareAndSet(fromState, targetState)) {
        // Publish event to EventBus
        eventBus.publish(new StateTransitionEvent(agentId, fromState, targetState));
        return true;
    }
    return false;
}
```

**Why This Design:**
- Explicit state validation prevents invalid transitions
- Event publication enables monitoring and coordination
- Thread-safe without locks (using CAS operations)

### 3. LLM Clients

#### AsyncLLMClient

**Location:** `src/main/java/com/minewright/llm/async/AsyncLLMClient.java`

**Purpose:** Non-blocking interface for LLM providers (OpenAI, Groq, Gemini).

**Key Methods:**
```java
public interface AsyncLLMClient {
    CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params);
    String getProviderId();
    boolean isHealthy();
}
```

**Implementations:**
- `AsyncOpenAIClient` - GPT-4, GPT-3.5
- `AsyncGroqClient` - Llama 3 70B
- `AsyncGeminiClient` - Gemini Pro

#### ResilientLLMClient

**Location:** `src/main/java/com/minewright/llm/resilience/ResilientLLMClient.java`

**Purpose:** Decorator that adds resilience patterns using Resilience4j.

**Request Flow:**
```
1. Check cache → HIT: return cached response
2. Check rate limiter → FULL: wait or reject
3. Check bulkhead → FULL: wait or reject
4. Check circuit breaker → OPEN: fallback
5. Execute request with retry
6. SUCCESS: cache response, return
7. FAILURE: trigger fallback handler
```

**Resilience Patterns:**
```java
private Supplier<CompletableFuture<LLMResponse>> decorateWithResilience(
    Supplier<CompletableFuture<LLMResponse>> supplier
) {
    // Apply Retry (innermost)
    Supplier<CompletableFuture<LLMResponse>> withRetry =
        Retry.decorateSupplier(retry, supplier);

    // Apply Circuit Breaker
    Supplier<CompletableFuture<LLMResponse>> withCircuitBreaker =
        CircuitBreaker.decorateSupplier(circuitBreaker, withRetry);

    // Apply Bulkhead
    Supplier<CompletableFuture<LLMResponse>> withBulkhead =
        Bulkhead.decorateSupplier(bulkhead, withCircuitBreaker);

    // Apply Rate Limiter (outermost)
    Supplier<CompletableFuture<LLMResponse>> withRateLimiter =
        RateLimiter.decorateSupplier(rateLimiter, withBulkhead);

    return withRateLimiter;
}
```

**Why This Design:**
- Graceful degradation when LLM providers fail
- Automatic retry with exponential backoff
- Rate limiting to prevent quota exhaustion
- Circuit breaker to fail fast during outages

#### BatchingLLMClient

**Location:** `src/main/java/com/minewright/llm/batch/BatchingLLMClient.java`

**Purpose:** Aggregates background tasks to respect rate limits while keeping user interactions responsive.

**Key Features:**
- User prompts sent immediately (minimal batching)
- Background tasks batched aggressively
- Heartbeat scheduler adapts batching rate based on usage
- Exponential backoff when rate limits are hit

```java
// Submit high-priority user prompt
public CompletableFuture<String> submitUserPrompt(String prompt, Map<String, Object> context) {
    heartbeat.onUserActivity();  // Disable idle mode
    return batcher.submitUserPrompt(prompt, context);
}

// Submit background task (batched)
public CompletableFuture<String> submitBackgroundPrompt(String prompt, Map<String, Object> context) {
    return batcher.submitBackgroundPrompt(prompt, context);
}
```

**Why This Design:**
- Reduces API costs by batching background tasks
- Maintains responsiveness for user interactions
- Adapts to rate limits automatically

### 4. Memory System

#### CompanionMemory

**Location:** `src/main/java/com/minewright/memory/CompanionMemory.java`

**Purpose:** Advanced memory system supporting relationship building and semantic search.

**Memory Types:**
```java
// Episodic memories - specific events and experiences
public void recordExperience(String eventType, String description, int emotionalWeight);

// Semantic memories - facts about the player
public void learnPlayerFact(String category, String key, Object value);

// Emotional memories - high-impact moments
private void recordEmotionalMemory(String eventType, String description, int emotionalWeight);

// Inside jokes and conversational memories
public void recordInsideJoke(String context, String punchline);

// Working memory - recent context
public void addToWorkingMemory(String type, String content);
```

**Relationship Tracking:**
```java
// Rapport level (0-100) based on interactions
private final AtomicInteger rapportLevel;

// Trust level based on shared successes/failures
private final AtomicInteger trustLevel;

// Milestone tracker for relationship milestones
private final MilestoneTracker milestoneTracker;
```

**Vector Search:**
```java
// Semantic search for relevant memories
public List<EpisodicMemory> findRelevantMemories(String query, int k) {
    float[] queryEmbedding = embeddingModel.embed(query);
    List<VectorSearchResult<EpisodicMemory>> results =
        memoryVectorStore.search(queryEmbedding, k);
    return results.stream()
        .map(VectorSearchResult::getData)
        .collect(Collectors.toList());
}
```

**Thread Safety:**
```java
// Thread-safe collections for concurrent access
private final Deque<EpisodicMemory> episodicMemories;  // ArrayDeque (single-threaded tick)
private final Map<String, SemanticMemory> semanticMemories;  // ConcurrentHashMap
private final List<EmotionalMemory> emotionalMemories;  // CopyOnWriteArrayList
```

**Why This Design:**
- Enables rich, evolving relationships with players
- Semantic search provides context-aware responses
- Thread-safe for concurrent access from LLM threads

#### InMemoryVectorStore

**Location:** `src/main/java/com/minewright/memory/vector/InMemoryVectorStore.java`

**Purpose:** Thread-safe vector store supporting cosine similarity search.

**Key Operations:**
```java
// Add vector with associated data
public int add(float[] vector, T data);

// Find k most similar vectors
public List<VectorSearchResult<T>> search(float[] queryVector, int k);

// Compute cosine similarity
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

**Why This Design:**
- Enables semantic search across memories
- Thread-safe for concurrent access
- Supports NBT persistence for world saves

### 5. Behavior Trees

**Location:** `src/main/java/com/minewright/behavior/`

**Purpose:** Hierarchical AI behavior modeling using composable nodes.

**Node Types:**

```java
// Composite nodes - control flow
BTNode sequence = new SequenceNode(
    new ConditionNode(() -> hasItem("axe")),
    new ActionNode(new MoveToNearestTreeAction()),
    new ActionNode(new MineBlockAction())
);

// Decorator nodes - modify child behavior
BTNode repeater = new RepeaterNode(new ActionNode(new MineBlockAction()), 10);

// Selector nodes - try children in order
BTNode selector = new SelectorNode(
    new ActionNode(new AttackAction()),
    new ActionNode(new FleeAction()),
    new ActionNode(new IdleAction())
);
```

**Node Interface:**
```java
public interface BTNode {
    NodeStatus tick(BTBlackboard blackboard);  // SUCCESS, FAILURE, RUNNING
    void reset();  // Clear internal state
    default boolean isComplete() { return false; }
}
```

**Blackboard Context:**
```java
public class BTBlackboard {
    private final ForemanEntity entity;
    private final Map<String, Object> data;

    public <T> T get(String key, Class<T> type);
    public void put(String key, Object value);
    public ForemanEntity getEntity();
}
```

**Why This Design:**
- Hierarchical and composable behaviors
- Reusable action nodes
- Non-blocking (returns RUNNING for long operations)

### 6. HTN Planner

**Location:** `src/main/java/com/minewright/htn/HTNPlanner.java`

**Purpose:** Hierarchical Task Network planner for decomposing compound tasks into primitive actions.

**Planning Algorithm:**
```java
private List<HTNTask> decomposeRecursive(HTNTask task, PlanningContext context, int depth) {
    // Base case: primitive task
    if (task.isPrimitive()) {
        return Collections.singletonList(task);
    }

    // Recursive case: compound task
    List<HTNMethod> methods = domain.getApplicableMethods(task.getName(), context.worldState);

    // Try each method in priority order
    for (HTNMethod method : methods) {
        List<HTNTask> decomposed = tryMethod(method, task, context, depth);
        if (decomposed != null) {
            return decomposed;  // Success
        }
    }

    return null;  // No method succeeded
}
```

**Task Decomposition:**
```
Compound Task: "build_house"
    │
    ├─ Method 1: "build_with_wood"
    │     ├─ Primitive: "gather_wood"
    │     ├─ Primitive: "craft_planks"
    │     └─ Primitive: "build_structure"
    │
    └─ Method 2: "build_with_stone"
          ├─ Primitive: "mine_stone"
          └─ Primitive: "build_structure"
```

**Why This Design:**
- Forward decomposition (more intuitive than backward GOAP)
- Hierarchical (matches human task breakdown)
- Supports multiple methods for flexibility

### 7. Pathfinding

#### AStarPathfinder

**Location:** `src/main/java/com/minewright/pathfinding/AStarPathfinder.java`

**Purpose:** Enhanced A* pathfinding with Minecraft-specific optimizations.

**Algorithm:**
```java
public Optional<List<PathNode>> findPath(BlockPos start, BlockPos goal, PathfindingContext context) {
    PriorityQueue<PathNode> openSet = new PriorityQueue<>();
    Set<BlockPos> closedSet = new HashSet<>();

    PathNode startNode = new PathNode(start, null, 0, heuristic(start, goal));
    openSet.add(startNode);

    while (!openSet.isEmpty()) {
        PathNode current = openSet.poll();  // Get lowest fCost

        if (current.pos.equals(goal)) {
            return reconstructPath(current);  // Goal reached
        }

        closedSet.add(current.pos);
        expandNeighbors(current, goal, context, openSet, closedSet);
    }

    return Optional.empty();  // No path found
}
```

**Movement Types:**
```java
public enum MovementType {
    WALK(1.0),
    JUMP(1.5),
    FALL(0.8),
    SWIM(2.0),
    CLIMB(1.2),
    PARKOUR(3.0),  // Dangerous
    FLY(0.5);      // Creative mode
}
```

**Optimizations:**
- Early termination when goal reached
- Node caching to reduce GC pressure
- Timeout protection (prevents infinite loops)
- Path smoothing to remove unnecessary waypoints

#### HierarchicalPathfinder

**Location:** `src/main/java/com/minewright/pathfinding/HierarchicalPathfinder.java`

**Purpose:** Multi-level pathfinding for long distances.

**Hierarchy:**
```
Level 2: Abstract graph (chunks)
    │
    │ Refine path
    ▼
Level 1: Local A* (blocks)
```

**Why This Design:**
- Fast pathfinding for long distances
- Scales to large worlds
- Falls back to A* for short paths

### 8. CascadeRouter

**Location:** `src/main/java/com/minewright/llm/cascade/CascadeRouter.java`

**Purpose:** Intelligent LLM request routing based on task complexity.

**Routing Strategy:**
```java
public CompletableFuture<LLMResponse> route(String command, Map<String, Object> context) {
    // Step 1: Check cache
    if (config.isCachingEnabled()) {
        Optional<LLMResponse> cached = checkCache(command, context);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }
    }

    // Step 2: Analyze complexity
    TaskComplexity complexity = analyzer.analyze(command, foreman, worldKnowledge);

    // Step 3: Select tier based on complexity
    LLMTier selectedTier = selectTier(complexity);

    // Step 4: Execute with fallback
    return executeWithFallback(command, context, complexity, selectedTier, 0);
}
```

**Tier Selection:**
```java
TRIVIAL   → CACHE or Small Model
SIMPLE    → Small Model (GLM-4.7-Air)
MODERATE  → Medium Model (GLM-5)
COMPLEX   → Large Model (GPT-4)
NOVEL     → Large Model (GPT-4)
```

**Why This Design:**
- 40-60% cost reduction through intelligent model selection
- Automatic fallback on failure
- Cache hits for repeated queries

### 9. OrchestratorService

**Location:** `src/main/java/com/minewright/orchestration/OrchestratorService.java`

**Purpose:** Coordinates multiple agents in hierarchical foreman/worker pattern.

**Task Distribution:**
```java
private void distributeTasks(PlanExecution plan, Collection<ForemanEntity> availableSteves) {
    List<ForemanEntity> workers = availableSteves.stream()
        .filter(s -> !s.getEntityName().equals(foremanId))
        .filter(s -> !workerAssignments.containsKey(s.getEntityName()))
        .collect(Collectors.toList());

    int workerIndex = 0;
    for (Task task : plan.getRemainingTasks()) {
        if (workers.isEmpty()) {
            assignTaskToAgent(plan, task, foremanId);  // Foreman handles it
        } else {
            // Round-robin assignment
            ForemanEntity worker = workers.get(workerIndex % workers.size());
            assignTaskToAgent(plan, task, worker.getEntityName());
            workerIndex++;
        }
    }
}
```

**Communication:**
```java
// Foreman assigns task to worker
AgentMessage message = AgentMessage.taskAssignment(
    foremanId, "Foreman", agentId,
    task.getAction(), task.getParameters()
);
communicationBus.publish(message);

// Worker reports completion
AgentMessage completion = AgentMessage.taskComplete(
    agentId, agentName,
    assignment.getAssignmentId(),
    result
);
communicationBus.publish(completion);
```

**Why This Design:**
- Enables parallel task execution
- Dynamic load balancing
- Graceful fallback to solo mode

---

## Data Flow

### Command Execution Flow

```
User Command: "/foreman order Steve build a house"
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. INPUT PROCESSING                                         │
│    - Parse command and extract intent                      │
│    - Validate agent exists and is available                │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. STATE TRANSITION                                         │
│    IDLE → PLANNING                                          │
│    (AgentStateMachine.transitionTo(PLANNING))              │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. ASYNC LLM PLANNING                                       │
│    - Submit to CascadeRouter                                │
│    - Analyze complexity                                     │
│    - Route to appropriate LLM tier                          │
│    - Returns CompletableFuture (non-blocking)               │
└─────────────────────────────────────────────────────────────┘
        │
        ├─────────────────────────────────────────────────────┤
        │                                                     │
        │ (Game continues normally - no blocking!)           │
        │                                                     │
        ▼                                                     ▼
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│ 4. GAME TICK LOOP (20 TPS)      │     │ LLM THREAD:                     │
│    - Check planning future      │────▶│ - Process prompt                │
│    - If done, queue tasks       │     │ - Generate task list            │
│    - Execute current action     │     │ - Complete future               │
│    - Start next task if ready   │     │                                 │
└─────────────────────────────────┘     └─────────────────────────────────┘
        │                                                     │
        │ (planning completes)                               │
        └─────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. TASK QUEUING                                            │
│    - Parse LLM response into Task objects                  │
│    - Add to taskQueue (LinkedBlockingQueue)                │
│    - Transition: PLANNING → EXECUTING                      │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. ACTION EXECUTION                                         │
│    For each task in queue:                                  │
│    a. Create action via ActionRegistry                      │
│    b. Run interceptor chain (beforeAction)                  │
│    c. Call action.start()                                   │
│    d. Tick action every game tick until isComplete()       │
│    e. Run interceptor chain (afterAction)                   │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. MINECRAFT API CALLS                                      │
│    - Pathfinding: A* search to target                      │
│    - Movement: Navigate path nodes                         │
│    - Mining: Break block at position                       │
│    - Placement: Place block at position                    │
│    - Crafting: Use crafting menu                           │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. COMPLETION                                               │
│    - ActionResult returned (SUCCESS/FAILURE)                │
│    - Update memory with action completed                    │
│    - Check if more tasks in queue                          │
│    - If yes, start next task                               │
│    - If no, transition: EXECUTING → COMPLETED → IDLE       │
└─────────────────────────────────────────────────────────────┘
```

### Error Handling Flow

```
Action Execution Fails
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. ACTION EXCEPTION                                         │
│    - Catch exception in action.tick()                       │
│    - Create ActionResult.failure(exception)                 │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. INTERCEPTOR CHAIN                                        │
│    - Execute onError interceptors                           │
│    - MetricsInterceptor records failure                     │
│    - EventPublishingInterceptor publishes failure event     │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. RESULT CHECK                                             │
│    - If result.requiresReplanning():                        │
│      → Trigger replanning with LLM                          │
│    - Else:                                                  │
│      → Log error, notify player                             │
│      → Continue with next task                              │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. STATE TRANSITION                                         │
│    EXECUTING → FAILED → IDLE                                │
│    (AgentStateMachine handles recovery)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Concurrency Model

### Threading Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              THREADS                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  MAIN THREAD (Minecraft Server)                                     │ │
│  │  - Runs game logic                                                  │ │
│  │  - Calls ActionExecutor.tick() every 50ms                          │ │
│  │  - Executes action.tick() for current action                       │ │
│  │  - Single-threaded for world modifications                         │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│           │                                                               │
│           │ Non-blocking async calls                                     │
│           ▼                                                               │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  LLM EXECUTOR THREAD POOL (LLMExecutorService)                      │ │
│  │  - Executes async LLM API calls                                     │ │
│  │  - Separate from game thread (no blocking)                          │ │
│  │  - Completes CompletableFuture when done                             │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│           │                                                               │
│           │ Result callback                                               │
│           ▼                                                               │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  CALLBACK THREAD (LLM executor)                                     │ │
│  │  - Processes LLM response                                           │ │
│  │  - Queues tasks in taskQueue                                        │ │
│  │  - Uses thread-safe collections                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  ORCHESTRATION THREAD (Optional)                                    │ │
│  │  - Handles multi-agent coordination                                 │ │
│  │  - Processes messages from AgentCommunicationBus                    │ │
│  │  - Distributes tasks to workers                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### Thread Safety Patterns

**1. Lock-Free Collections:**
```java
// ConcurrentHashMap for thread-safe maps
private final ConcurrentHashMap<String, TaskAssignment> workerAssignments;

// LinkedBlockingQueue for producer-consumer pattern
private final BlockingQueue<Task> taskQueue;

// CopyOnWriteArrayList for iteration safety
private final List<EmotionalMemory> emotionalMemories;
```

**2. Atomic Primitives:**
```java
// AtomicBoolean for state flags
private final AtomicBoolean isPlanning = new AtomicBoolean(false);

// AtomicInteger for counters
private final AtomicInteger rapportLevel = new AtomicInteger(10);

// AtomicReference for state
private final AtomicReference<AgentState> currentState;
```

**3. Volatile Fields:**
```java
// Volatile for visibility across threads
private volatile CompletableFuture<ParsedResponse> planningFuture;
private volatile String pendingCommand;
private String currentGoal;
```

**4. Compare-And-Set (CAS) Operations:**
```java
// Thread-safe state transition
if (currentState.compareAndSet(fromState, targetState)) {
    // Success - state changed
    eventBus.publish(new StateTransitionEvent(...));
    return true;
}
// Failed - concurrent modification
return false;
```

**5. Immutable Data:**
```java
// Immutable value objects
public static final class EpisodicMemory {
    public final String eventType;
    public final String description;
    public final int emotionalWeight;
    public final Instant timestamp;
    // No setters - immutable
}
```

### Synchronization Points

**Minimal Synchronization Design:**
- Game thread: Single-threaded for world access
- LLM threads: Independent, use CAS for coordination
- Shared state: Lock-free collections (ConcurrentHashMap, BlockingQueue)
- Events: EventBus for decoupled communication

**Why This Design:**
- No deadlocks (no lock contention)
- High throughput (lock-free data structures)
- Predictable performance (bounded operations per tick)

---

## Plugin System

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PLUGIN SYSTEM                                     │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │                     ActionRegistry (Singleton)                      ││
│  │  - Central registry of action factories                            ││
│  │  - Thread-safe action creation                                     ││
│  │  - Priority-based conflict resolution                               ││
│  └────────────────────────────────────────────────────────────────────┘│
│           │                                                              │
│           │ register(actionName, factory, priority, pluginId)          │
│           ▼                                                              │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │                     ActionFactory Interface                          ││
│  │  BaseAction create(ForemanEntity, Task, ActionContext)              ││
│  └────────────────────────────────────────────────────────────────────┘│
│           ▲                                                              │
│           │ implements                                                   │
│           │                                                              │
│  ┌────────┴──────────────────────────────────────────────────────────┐ │
│  │                              │                                     │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐                 │ │
│  │  │  CoreActionsPlugin  │  │  CustomPlugin       │                 │ │
│  │  │  - mine             │  │  - Custom actions   │                 │ │
│  │  │  - build            │  │  - Domain-specific  │                 │ │
│  │  │  - craft            │  │    behaviors         │                 │ │
│  │  │  - gather           │  │                     │                 │ │
│  │  └─────────────────────┘  └─────────────────────┘                 │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Registration Example

```java
// In CoreActionsPlugin.java
public class CoreActionsPlugin implements ActionPlugin {
    @Override
    public void registerActions(ActionRegistry registry) {
        // Register with priority for conflict resolution
        registry.register("mine",
            (steve, task, ctx) -> new MineBlockAction(steve, task),
            100,  // High priority
            "core"
        );

        registry.register("build",
            (steve, task, ctx) -> new BuildStructureAction(steve, task),
            100,
            "core"
        );

        registry.register("craft",
            (steve, task, ctx) -> new CraftItemAction(steve, task),
            100,
            "core"
        );
    }
}

// Plugin loading via SPI
PluginManager.loadPlugins();
```

### Action Creation

```java
// In ActionExecutor.createAction()
private BaseAction createAction(Task task) {
    String actionType = task.getAction();
    ActionRegistry registry = ActionRegistry.getInstance();

    // Try registry-based creation first
    if (registry.hasAction(actionType)) {
        BaseAction action = registry.createAction(
            actionType,
            foreman,
            task,
            actionContext
        );
        if (action != null) {
            return action;
        }
    }

    // Fallback to legacy switch statement
    return createActionLegacy(task);
}
```

### Why This Design:

- **Extensible:** Add new actions without modifying core code
- **Decoupled:** Plugins are independent and can be loaded/unloaded
- **Priority-based:** Resolves conflicts automatically
- **Type-safe:** Compile-time checking of action factories

---

## Key Design Patterns

### 1. Strategy Pattern

**Location:** LLM clients (`AsyncLLMClient`), Interceptors (`ActionInterceptor`)

**Purpose:** Pluggable algorithms for different providers or behaviors.

**Example:**
```java
// Strategy interface
public interface AsyncLLMClient {
    CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params);
    String getProviderId();
}

// Concrete strategies
public class AsyncOpenAIClient implements AsyncLLMClient { ... }
public class AsyncGroqClient implements AsyncLLMClient { ... }
public class AsyncGeminiClient implements AsyncLLMClient { ... }
```

### 2. Decorator Pattern

**Location:** `ResilientLLMClient`, Interceptor Chain

**Purpose:** Add behavior without modifying original class.

**Example:**
```java
// Base client
AsyncLLMClient rawClient = new AsyncOpenAIClient(apiKey, model);

// Decorate with resilience
AsyncLLMClient resilientClient = new ResilientLLMClient(
    rawClient,
    cache,
    fallbackHandler
);

// Decorate with batching
AsyncLLMClient batchClient = new BatchingLLMClient(resilientClient);
```

### 3. Chain of Responsibility

**Location:** `InterceptorChain`, `ActionInterceptor`

**Purpose:** Process requests through a chain of handlers.

**Example:**
```java
// Build interceptor chain
InterceptorChain chain = new InterceptorChain();
chain.addInterceptor(new LoggingInterceptor());
chain.addInterceptor(new MetricsInterceptor());
chain.addInterceptor(new EventPublishingInterceptor(eventBus));

// Execute chain
if (chain.executeBeforeAction(action, context)) {
    action.start();
    chain.executeAfterAction(action, result, context);
}
```

### 4. Observer Pattern

**Location:** `EventBus`, State Machine Events

**Purpose:** Publish-subscribe for decoupled communication.

**Example:**
```java
// Subscribe to events
eventBus.subscribe(StateTransitionEvent.class, event -> {
    LOGGER.info("State changed: {} -> {}",
        event.getFromState(),
        event.getToState()
    );
});

// Publish events
eventBus.publish(new StateTransitionEvent(agentId, fromState, toState));
```

### 5. State Pattern

**Location:** `AgentStateMachine`, `AgentState`

**Purpose:** Encapsulate state-specific behavior.

**Example:**
```java
public enum AgentState {
    IDLE(true, false),
    PLANNING(false, true),
    EXECUTING(false, true),
    PAUSED(false, false),
    COMPLETED(true, false),
    FAILED(true, false);

    private final boolean canAcceptCommands;
    private final boolean isActive;
}
```

### 6. Factory Pattern

**Location:** `ActionFactory`, `ActionRegistry`

**Purpose:** Create objects without specifying exact class.

**Example:**
```java
// Factory interface
@FunctionalInterface
public interface ActionFactory {
    BaseAction create(ForemanEntity steve, Task task, ActionContext context);
}

// Registry with factories
ActionRegistry registry = ActionRegistry.getInstance();
registry.register("mine", (steve, task, ctx) -> new MineBlockAction(steve, task));
```

### 7. Builder Pattern

**Location:** `ActionContext`, `PathfindingContext`

**Purpose:** Construct complex objects step-by-step.

**Example:**
```java
ActionContext context = ActionContext.builder()
    .serviceContainer(container)
    .eventBus(eventBus)
    .stateMachine(stateMachine)
    .interceptorChain(interceptorChain)
    .build();
```

### 8. Singleton Pattern

**Location:** `ActionRegistry`, `CascadeConfig`

**Purpose:** Ensure single instance of registry/config.

**Example:**
```java
public class ActionRegistry {
    private static final ActionRegistry INSTANCE = new ActionRegistry();

    public static ActionRegistry getInstance() {
        return INSTANCE;
    }

    private ActionRegistry() { ... }  // Private constructor
}
```

### 9. Composite Pattern

**Location:** Behavior Trees (`BTNode`, `CompositeNode`)

**Purpose:** Treat individual and composite objects uniformly.

**Example:**
```java
// Composite node (Sequence)
BTNode sequence = new SequenceNode(
    new ConditionNode(() -> hasAxe),
    new ActionNode(new MoveToTreeAction()),
    new ActionNode(new MineAction())
);

// Treat same as leaf node
sequence.tick(blackboard);
```

### 10. Future/Promise Pattern

**Location:** `CompletableFuture` for async LLM calls

**Purpose:** Represent pending result of async operation.

**Example:**
```java
// Start async operation
CompletableFuture<ParsedResponse> future = taskPlanner.planTasksAsync(foreman, command);

// Chain callbacks
future.thenAccept(response -> {
    taskQueue.addAll(response.getTasks());
}).exceptionally(error -> {
    LOGGER.error("Planning failed", error);
    return null;
});

// Non-blocking check in tick()
if (future.isDone()) {
    ParsedResponse response = future.getNow(null);
    // Process result
}
```

---

## File References

### Core Execution

| Component | File Location |
|-----------|---------------|
| ActionExecutor | `src/main/java/com/minewright/action/ActionExecutor.java` |
| AgentStateMachine | `src/main/java/com/minewright/execution/AgentStateMachine.java` |
| InterceptorChain | `src/main/java/com/minewright/execution/InterceptorChain.java` |
| BaseAction | `src/main/java/com/minewright/action/actions/BaseAction.java` |
| Task | `src/main/java/com/minewright/action/Task.java` |

### LLM Integration

| Component | File Location |
|-----------|---------------|
| AsyncLLMClient | `src/main/java/com/minewright/llm/async/AsyncLLMClient.java` |
| ResilientLLMClient | `src/main/java/com/minewright/llm/resilience/ResilientLLMClient.java` |
| BatchingLLMClient | `src/main/java/com/minewright/llm/batch/BatchingLLMClient.java` |
| CascadeRouter | `src/main/java/com/minewright/llm/cascade/CascadeRouter.java` |
| TaskPlanner | `src/main/java/com/minewright/llm/TaskPlanner.java` |
| ResponseParser | `src/main/java/com/minewright/llm/ResponseParser.java` |

### Memory System

| Component | File Location |
|-----------|---------------|
| CompanionMemory | `src/main/java/com/minewright/memory/CompanionMemory.java` |
| InMemoryVectorStore | `src/main/java/com/minewright/memory/vector/InMemoryVectorStore.java` |
| EmbeddingModel | `src/main/java/com/minewright/memory/embedding/EmbeddingModel.java` |
| ConversationManager | `src/main/java/com/minewright/memory/ConversationManager.java` |

### AI Systems

| Component | File Location |
|-----------|---------------|
| BTNode | `src/main/java/com/minewright/behavior/BTNode.java` |
| SequenceNode | `src/main/java/com/minewright/behavior/composite/SequenceNode.java` |
| SelectorNode | `src/main/java/com/minewright/behavior/composite/SelectorNode.java` |
| HTNPlanner | `src/main/java/com/minewright/htn/HTNPlanner.java` |
| AStarPathfinder | `src/main/java/com/minewright/pathfinding/AStarPathfinder.java` |
| HierarchicalPathfinder | `src/main/java/com/minewright/pathfinding/HierarchicalPathfinder.java` |

### Orchestration

| Component | File Location |
|-----------|---------------|
| OrchestratorService | `src/main/java/com/minewright/orchestration/OrchestratorService.java` |
| AgentCommunicationBus | `src/main/java/com/minewright/orchestration/AgentCommunicationBus.java` |
| AgentMessage | `src/main/java/com/minewright/orchestration/AgentMessage.java` |

### Plugin System

| Component | File Location |
|-----------|---------------|
| ActionRegistry | `src/main/java/com/minewright/plugin/ActionRegistry.java` |
| ActionFactory | `src/main/java/com/minewright/plugin/ActionFactory.java` |
| PluginManager | `src/main/java/com/minewright/plugin/PluginManager.java` |
| CoreActionsPlugin | `src/main/java/com/minewright/plugin/CoreActionsPlugin.java` |

### Configuration

| Component | File Location |
|-----------|---------------|
| MineWrightConfig | `src/main/java/com/minewright/config/MineWrightConfig.java` |
| ResilienceConfig | `src/main/java/com/minewright/llm/resilience/ResilienceConfig.java` |
| CascadeConfig | `src/main/java/com/minewright/llm/cascade/CascadeConfig.java` |

---

## Summary

Steve AI implements a sophisticated three-layer architecture that separates strategic planning (LLM) from tactical execution (traditional game AI). This design enables:

- **Performance:** 60 FPS execution without blocking on LLM calls
- **Cost Efficiency:** 10-20x fewer tokens through intelligent routing and caching
- **Extensibility:** Plugin architecture for adding new actions and behaviors
- **Reliability:** Resilience patterns (retry, circuit breaker, rate limiting, fallback)
- **Character:** Rich memory and personality systems for engaging companions

The architecture follows industry best practices including lock-free concurrency, event-driven communication, and composable design patterns. All components are designed for thread safety and high throughput in a concurrent environment.

---

**Document Version:** 2.0
**Last Updated:** 2026-02-28
**Maintained By:** Development Team
