# Patterns Guide

This document describes the key architectural and design patterns used in Steve AI.

## Table of Contents

1. [Architectural Patterns](#architectural-patterns)
2. [Design Patterns](#design-patterns)
3. [Concurrency Patterns](#concurrency-patterns)
4. [Integration Patterns](#integration-patterns)

---

## Architectural Patterns

### Three-Layer Architecture

**Concept:** LLMs plan and coordinate (Brain Layer), while traditional game AI executes (Script/Physical Layer).

**Benefits:**
- 60 FPS execution without blocking on LLM calls
- 10-20x fewer tokens than pure LLM approaches
- Rich personalities with ongoing dialogue

**Implementation:**
- **Brain Layer:** LLM agents for strategic planning (updates every 30-60 seconds)
- **Script Layer:** Behavior trees, FSMs, scripts for tactical execution (updates every tick)
- **Physical Layer:** Direct Minecraft API calls (updates every tick)

### Plugin Architecture

**Concept:** Extensible action system through plugins.

**Benefits:**
- Add new actions without modifying core code
- Decoupled components that can be loaded/unloaded
- Priority-based conflict resolution

**Implementation:**
```java
// Register action via plugin
registry.register("mine",
    (steve, task, ctx) -> new MineAction(steve, task),
    100,  // Priority
    "core"  // Plugin ID
);

// Create action via registry
BaseAction action = registry.createAction("mine", steve, task, context);
```

### Blackboard Pattern

**Concept:** Shared knowledge system for agent coordination.

**Benefits:**
- Loose coupling between agents
- Emergent coordination through shared information
- No central coordinator required

**Implementation:**
```java
// Post observation
Blackboard.getInstance().post(KnowledgeArea.WORLD_STATE, entry);

// Query for information
Optional<BlockState> block = Blackboard.getInstance().query(
    KnowledgeArea.WORLD_STATE, "block_100_64_200"
);

// Subscribe to updates
Blackboard.getInstance().subscribe(KnowledgeArea.THREATS, subscriber);
```

---

## Design Patterns

### Strategy Pattern

**Location:** LLM clients, Interceptors

**Purpose:** Pluggable algorithms for different providers or behaviors

**Example:**
```java
// Strategy interface
public interface AsyncLLMClient {
    CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params);
}

// Concrete strategies
public class AsyncOpenAIClient implements AsyncLLMClient { ... }
public class AsyncGroqClient implements AsyncLLMClient { ... }
```

### Decorator Pattern

**Location:** ResilientLLMClient, Interceptor Chain

**Purpose:** Add behavior without modifying original class

**Example:**
```java
// Base client
AsyncLLMClient rawClient = new AsyncOpenAIClient(apiKey, model);

// Decorate with resilience
AsyncLLMClient resilientClient = new ResilientLLMClient(rawClient, cache, fallbackHandler);

// Decorate with batching
AsyncLLMClient batchClient = new BatchingLLMClient(resilientClient);
```

### Chain of Responsibility

**Location:** InterceptorChain, ActionInterceptor

**Purpose:** Process requests through a chain of handlers

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

### Observer Pattern

**Location:** EventBus, State Machine Events

**Purpose:** Publish-subscribe for decoupled communication

**Example:**
```java
// Subscribe to events
eventBus.subscribe(StateTransitionEvent.class, event -> {
    LOGGER.info("State changed: {} -> {}",
        event.getFromState(), event.getToState());
});

// Publish events
eventBus.publish(new StateTransitionEvent(agentId, fromState, toState));
```

### State Pattern

**Location:** AgentStateMachine, AgentState

**Purpose:** Encapsulate state-specific behavior

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

### Factory Pattern

**Location:** ActionFactory, ActionRegistry

**Purpose:** Create objects without specifying exact class

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

### Builder Pattern

**Location:** ActionContext, PathfindingContext

**Purpose:** Construct complex objects step-by-step

**Example:**
```java
ActionContext context = ActionContext.builder()
    .serviceContainer(container)
    .eventBus(eventBus)
    .stateMachine(stateMachine)
    .interceptorChain(interceptorChain)
    .build();
```

### Composite Pattern

**Location:** Behavior Trees

**Purpose:** Treat individual and composite objects uniformly

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

### Singleton Pattern

**Location:** ActionRegistry, CascadeConfig, Blackboard

**Purpose:** Ensure single instance of registry/config

**Example:**
```java
// Thread-safe singleton with double-checked locking
private static volatile MyClass instance;
private static final Object lock = new Object();

public static MyClass getInstance() {
    if (instance == null) {
        synchronized (lock) {
            if (instance == null) {
                instance = new MyClass();
            }
        }
    }
    return instance;
}
```

### Future/Promise Pattern

**Location:** CompletableFuture for async LLM calls

**Purpose:** Represent pending result of async operation

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

## Concurrency Patterns

### Tick-Based Execution

**Concept:** Execute actions tick-by-tick without blocking

**Benefits:**
- Prevents server freezing
- Smooth 60 FPS agent behavior
- Predictable performance

**Implementation:**
```java
public class MineAction extends BaseAction {
    @Override
    protected void onTick() {
        // Called once per game tick (20 TPS)
        // Track internal state and return when complete
    }

    @Override
    public boolean isComplete() {
        return getBlocksMined() >= getTarget();
    }
}
```

### Lock-Free Collections

**Concept:** Use concurrent collections to avoid locks

**Benefits:**
- No deadlocks
- High throughput
- Better scalability

**Implementation:**
```java
// ConcurrentHashMap for thread-safe maps
private final ConcurrentHashMap<String, TaskAssignment> workerAssignments;

// LinkedBlockingQueue for producer-consumer pattern
private final BlockingQueue<Task> taskQueue;

// CopyOnWriteArrayList for iteration safety
private final List<EmotionalMemory> emotionalMemories;
```

### Atomic Primitives

**Concept:** Use atomic types for thread-safe operations

**Benefits:**
- Lock-free thread safety
- CAS operations for coordination
- No race conditions

**Implementation:**
```java
// AtomicBoolean for state flags
private final AtomicBoolean isPlanning = new AtomicBoolean(false);

// AtomicInteger for counters
private final AtomicInteger rapportLevel = new AtomicInteger(10);

// AtomicLong for counters (volatile long is NOT atomic!)
private final AtomicLong totalRequests = new AtomicLong(0);
```

### Compare-And-Set (CAS)

**Concept:** Lock-free state transitions

**Benefits:**
- Thread-safe state changes
- No locks required
- Optimistic concurrency

**Implementation:**
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

---

## Integration Patterns

### Cascade Router

**Concept:** Route requests to appropriate LLM tier based on complexity

**Benefits:**
- 40-60% cost reduction
- Automatic model selection
- Fallback on failure

**Implementation:**
```java
// Route based on task complexity
TaskComplexity complexity = analyzer.analyze(command, foreman, worldKnowledge);
LLMTier selectedTier = selectTier(complexity);

// Tier selection
TRIVIAL   → CACHE or Small Model
SIMPLE    → Small Model (GLM-4.7-Air)
MODERATE  → Medium Model (GLM-5)
COMPLEX   → Large Model (GPT-4)
NOVEL     → Large Model (GPT-4)
```

### Semantic Caching

**Concept:** Cache LLM responses based on semantic similarity

**Benefits:**
- 30-50% fewer API calls
- Reduced costs
- Faster responses

**Implementation:**
```java
// Check cache with semantic similarity
String cacheKey = embed(prompt);
Optional<LLMResponse> cached = cache.findSimilar(cacheKey, threshold);
if (cached.isPresent()) {
    return cached.get();
}

// Execute LLM call and cache result
LLMResponse response = llmClient.sendAsync(prompt).get();
cache.store(cacheKey, response);
```

### Batching

**Concept:** Aggregate multiple requests into single API call

**Benefits:**
- 20-30% cost reduction
- Better rate limit utilization
- Reduced API overhead

**Implementation:**
```java
// Submit high-priority user prompt (immediate)
public CompletableFuture<String> submitUserPrompt(String prompt, Map<String, Object> context) {
    heartbeat.onUserActivity();  // Disable idle mode
    return batcher.submitUserPrompt(prompt, context);
}

// Submit background task (batched)
public CompletableFuture<String> submitBackgroundPrompt(String prompt, Map<String, Object> context) {
    return batcher.submitBackgroundPrompt(prompt, context);
}
```

### Resilience Patterns

**Concept:** Retry, circuit breaker, rate limiting, fallback

**Benefits:**
- Graceful degradation
- Automatic recovery
- Protection against failures

**Implementation:**
```java
// Decorate with resilience patterns
Supplier<CompletableFuture<LLMResponse>> decorated =
    RateLimiter.decorateSupplier(rateLimiter,
        Bulkhead.decorateSupplier(bulkhead,
            CircuitBreaker.decorateSupplier(circuitBreaker,
                Retry.decorateSupplier(retry, supplier)
            )
        )
    );
```

---

## Specialized Patterns

### Contract Net Protocol

**Location:** Multi-agent coordination

**Purpose:** Distributed task allocation through bidding

**Implementation:**
```java
// Manager announces task
contractNet.announceTask(task);

// Workers submit bids
contractNet.submitBid(taskId, agentId, bid);

// Manager awards task
contractNet.awardTask(taskId, winningAgentId);
```

### HTN Planning

**Location:** Hierarchical Task Network planner

**Purpose:** Decompose complex tasks into primitive actions

**Implementation:**
```java
// Compound task decomposes into methods
Compound Task: "build_house"
    ├─ Method 1: "build_with_wood"
    │   ├─ Primitive: "gather_wood"
    │   ├─ Primitive: "craft_planks"
    │   └─ Primitive: "build_structure"
    └─ Method 2: "build_with_stone"
        ├─ Primitive: "mine_stone"
        └─ Primitive: "build_structure"
```

### Behavior Trees

**Location:** AI behavior system

**Purpose:** Hierarchical and composable behaviors

**Implementation:**
```java
// Compose behavior tree
BTNode behavior = new SequenceNode(
    new ConditionNode(() -> hasItem("axe")),
    new ActionNode(new MoveToNearestTreeAction()),
    new RepeaterNode(new ActionNode(new MineBlockAction()), 10)
);

// Execute tick-by-tick
NodeStatus status = behavior.tick(blackboard);
```

### Goal Composition

**Location:** Navigation system (Baritone-inspired)

**Purpose:** Compose multiple navigation goals

**Implementation:**
```java
// ANY goal - reach any of the targets
NavigationGoal anyGoal = new CompositeNavigationGoal(CompositionType.ANY);
anyGoal.addGoal(new GetToBlockGoal(BlockType.OAK_LOG));
anyGoal.addGoal(new GetToBlockGoal(BlockType.BIRCH_LOG));

// ALL goal - reach all targets in order
NavigationGoal allGoal = new CompositeNavigationGoal(CompositionType.ALL);
allGoal.addGoal(new GetToBlockGoal(BlockType.CRAFTING_TABLE));
allGoal.addGoal(new GetToBlockGoal(BlockType.CHEST));
```

---

## Anti-Patterns to Avoid

### Blocking the Game Thread

**Don't:**
```java
// BAD - Blocks game thread during LLM call
LLMResponse response = llmClient.send(prompt);  // Takes 30-60 seconds
```

**Do:**
```java
// GOOD - Async LLM call
CompletableFuture<LLMResponse> future = llmClient.sendAsync(prompt);
// Check future.isDone() in tick()
```

### Volatile for Counters

**Don't:**
```java
// BAD - volatile long is NOT atomic for read-modify-write
private volatile long counter;

public void increment() {
    counter++;  // NOT ATOMIC!
}
```

**Do:**
```java
// GOOD - AtomicLong for atomic operations
private final AtomicLong counter = new AtomicLong(0);

public void increment() {
    counter.incrementAndGet();  // ATOMIC
}
```

### Empty Catch Blocks

**Don't:**
```java
// BAD - Empty catch block
try {
    doSomething();
} catch (Exception e) {
    // Ignore
}
```

**Do:**
```java
// GOOD - Log exception with stack trace
try {
    doSomething();
} catch (Exception e) {
    LOGGER.error("Failed to do something", e);  // Always log
}
```

## References

- **Architecture Overview:** `docs/ARCHITECTURE_OVERVIEW.md`
- **Package Reference:** `docs/PACKAGE_REFERENCE.md`
- **Build Guide:** `docs/BUILD_GUIDE.md`
- **Refactoring History:** `docs/REFACTORING_HISTORY.md`
