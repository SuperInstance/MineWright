# Pattern Language - MineWright Codebase Patterns

**Version:** 1.0
**Created:** 2026-03-05
**Purpose:** Document the pattern language of the MineWright codebase
**Target:** Future agents who need to recognize and use codebase patterns

---

## Table of Contents

1. [Pattern Language Overview](#pattern-language-overview)
2. [Structural Patterns](#structural-patterns)
3. [Behavioral Patterns](#behavioral-patterns)
4. [Communication Patterns](#communication-patterns)
5. [Coordination Patterns](#coordination-patterns)
6. [Data Patterns](#data-patterns)
7. [Lifecycle Patterns](#lifecycle-patterns)
8. [Error Handling Patterns](#error-handling-patterns)
9. [Performance Patterns](#performance-patterns)
10. [Testing Patterns](#testing-patterns)

---

## Pattern Language Overview

### What is a Pattern Language?

A pattern language is a structured method of describing good design practices within a field of expertise. Unlike isolated design patterns, a pattern language shows how patterns relate to and build upon each other.

### How to Use This Document

1. **Learn the patterns** - Understand each pattern's intent and structure
2. **Recognize patterns** - Identify them in the existing codebase
3. **Apply patterns** - Use them when solving similar problems
4. **Combine patterns** - Patterns work together to solve complex problems

### Pattern Categories

```
┌─────────────────────────────────────────────────────────────────┐
│                    STRUCTURAL PATTERNS                          │
│  How code is organized and components relate to each other       │
├─────────────────────────────────────────────────────────────────┤
│                    BEHAVIORAL PATTERNS                          │
│  How objects behave and distribute responsibility               │
├─────────────────────────────────────────────────────────────────┤
│                    COMMUNICATION PATTERNS                       │
│  How components exchange information                           │
├─────────────────────────────────────────────────────────────────┤
│                    COORDINATION PATTERNS                        │
│  How multiple agents work together                              │
├─────────────────────────────────────────────────────────────────┤
│                    DATA PATTERNS                                │
│  How data is stored, accessed, and transformed                 │
├─────────────────────────────────────────────────────────────────┤
│                    LIFECYCLE PATTERNS                           │
│  How objects are created, used, and destroyed                 │
├─────────────────────────────────────────────────────────────────┤
│                    ERROR HANDLING PATTERNS                      │
│  How errors are detected and recovered from                    │
├─────────────────────────────────────────────────────────────────┤
│                    PERFORMANCE PATTERNS                         │
│  How to achieve efficient execution                            │
├─────────────────────────────────────────────────────────────────┤
│                    TESTING PATTERNS                             │
│  How to test effectively                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Structural Patterns

### Pattern 1.1: Layered Architecture

**Intent:** Separate concerns into distinct layers with clear boundaries.

**Structure:**
```
┌─────────────────────────────────────────────────────────────────┐
│                        BRAIN LAYER                              │
│  Planning, strategy, natural language understanding             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Plans
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       SCRIPT LAYER                              │
│  Behavior execution, state machines, pathfinding               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Commands
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      PHYSICAL LAYER                             │
│  Block interaction, movement, inventory management             │
└─────────────────────────────────────────────────────────────────┘
```

**When to Use:**
- System has multiple levels of abstraction
- Lower levels need to operate independently of higher levels
- Different update frequencies for different levels

**Examples in Codebase:**
- `llm/` package → Brain layer
- `action/`, `behavior/`, `pathfinding/` packages → Script layer
- Entity interactions with Minecraft → Physical layer

**Related Patterns:**
- 1.2 Plugin Architecture (for layer extensibility)
- 3.1 Async Request-Response (for layer communication)

---

### Pattern 1.2: Plugin Architecture

**Intent:** Allow runtime registration of extensions without modifying core code.

**Structure:**
```java
// Core interface
public interface Action {
    void onStart();
    void onTick();
    void onCancel();
    boolean isComplete();
}

// Registry
public class ActionRegistry {
    private static final Map<String, Class<? extends Action>> actions = new HashMap<>();

    public static void register(String name, Class<? extends Action> actionClass) {
        actions.put(name, actionClass);
    }

    public static Action create(String name) {
        Class<? extends Action> clazz = actions.get(name);
        return clazz.getDeclaredConstructor().newInstance();
    }
}

// Usage
ActionRegistry.register("mine", MineAction.class);
ActionRegistry.register("build", BuildAction.class);
```

**When to Use:**
- Extensions need to be added without recompiling core
- Multiple independent teams may contribute extensions
- Want to enable community contributions

**Examples in Codebase:**
- `ActionRegistry` for action plugins
- `SkillLibrary` for skill plugins
- `PersonalityRegistry` for personality plugins

**Benefits:**
- Extensibility without modification
- Testability (plugins can be tested in isolation)
- Discoverability (plugins can list themselves)

**Tradeoffs:**
- More complex than hardcoded solutions
- Requires well-defined interfaces
- Plugin compatibility must be managed

---

### Pattern 1.3: Component-Based Entity

**Intent:** Compose entities from pluggable components rather than deep inheritance.

**Structure:**
```java
// Entity is a container for components
public class ForemanEntity extends Entity {
    private EntityState state;
    private ActionCoordinator coordinator;
    private CommunicationHandler communication;
    private MemorySystem memory;

    // Delegate to components
    public void tick() {
        state.tick();
        coordinator.tick();
        communication.tick();
        memory.tick();
    }
}

// Each component handles one concern
class EntityState {
    private AgentStateMachine stateMachine;
    private BlockPos targetPosition;
    // ... state management
}

class ActionCoordinator {
    private ActionExecutor executor;
    private TaskQueue queue;
    // ... action coordination
}
```

**When to Use:**
- Entities have many independent responsibilities
- Responsibilities may be combined in different ways
- Want to avoid deep inheritance hierarchies

**Examples in Codebase:**
- `ForemanEntity` delegates to multiple components
- `CompanionMemory` composed of specialized memory types

**Benefits:**
- Flexible composition
- Single responsibility per component
- Easy to add/remove components
- Better testability

**Tradeoffs:**
- More indirection
- Component communication can be complex
- Requires good component boundaries

---

### Pattern 1.4: Registry Pattern

**Intent:** Provide centralized access to shared objects or types.

**Structure:**
```java
public class SomeRegistry {
    private static final Map<String, Type> registry = new ConcurrentHashMap<>();

    public static void register(String key, Type value) {
        registry.put(key, value);
    }

    public static Type get(String key) {
        return registry.get(key);
    }

    public static Collection<Type> getAll() {
        return registry.values();
    }
}
```

**When to Use:**
- Need global access to objects
- Objects need to be discovered by name/type
- Want centralized management

**Examples in Codebase:**
- `ActionRegistry` - action types
- `SkillLibrary` - learned skills
- `ProfileRegistry` - task profiles
- `PersonalityRegistry` - personality types

**Benefits:**
- Centralized access
- Discoverability
- Easy to add/remove entries

**Tradeoffs:**
- Global state (can be abused)
- Potential for naming conflicts
- Can become a god object

---

## Behavioral Patterns

### Pattern 2.1: State Machine

**Intent:** Manage an object's behavior through explicit state transitions.

**Structure:**
```java
public enum AgentState {
    IDLE,
    PLANNING,
    EXECUTING,
    STUCK,
    COMPLETED,
    CANCELLED
}

public class AgentStateMachine {
    private AgentState currentState = AgentState.IDLE;
    private final Map<AgentState, Set<AgentState>> validTransitions = Map.of(
        AgentState.IDLE, Set.of(AgentState.PLANNING, AgentState.CANCELLED),
        AgentState.PLANNING, Set.of(AgentState.EXECUTING, AgentState.CANCELLED),
        AgentState.EXECUTING, Set.of(AgentState.COMPLETED, AgentState.STUCK, AgentState.CANCELLED),
        AgentState.STUCK, Set.of(AgentState.PLANNING, AgentState.CANCELLED)
    );

    public void transition(AgentState newState) {
        if (!validTransitions.get(currentState).contains(newState)) {
            throw new IllegalStateException("Invalid transition: " + currentState + " -> " + newState);
        }
        currentState = newState;
    }
}
```

**When to Use:**
- Object has distinct modes of behavior
- State transitions are constrained
- Need to track and visualize state

**Examples in Codebase:**
- `AgentStateMachine` for agent lifecycle
- `ActionExecutor` states for action execution
- `ScriptExecutor` states for script execution

**Benefits:**
- Explicit states and transitions
- Prevents invalid states
- Easy to visualize and debug
- Testable transitions

**Tradeoffs:**
- Can be complex for many states
- State explosion for orthogonal concerns
- Consider hierarchical state machines for complexity

---

### Pattern 2.2: Tick-Based Update

**Intent:** Execute behavior incrementally over multiple game ticks.

**Structure:**
```java
public interface Tickable {
    void tick();
}

public class SomeAction implements Tickable {
    private int ticksElapsed = 0;
    private final int durationTicks = 60; // 3 seconds

    @Override
    public void tick() {
        ticksElapsed++;

        // Do a small amount of work each tick
        if (ticksElapsed % 10 == 0) {
            doPartialWork();
        }

        // Check completion
        if (ticksElapsed >= durationTicks) {
            onComplete();
        }
    }
}
```

**When to Use:**
- Operations can't complete in one frame
- Need to maintain frame rate
- Want progress indication

**Examples in Codebase:**
- All `Action` implementations
- `ForemanEntity.tick()`
- `ScriptExecutor.tick()`

**Benefits:**
- Non-blocking execution
- Maintains frame rate
- Can be cancelled mid-execution
- Progress tracking

**Tradeoffs:**
- Can't use blocking operations
- State must be preserved across ticks
- More complex than single-shot execution

---

### Pattern 2.3: Strategy Pattern

**Intent:** Define a family of algorithms and make them interchangeable.

**Structure:**
```java
// Strategy interface
public interface PathfindingStrategy {
    List<BlockPos> findPath(BlockPos start, BlockPos end);
}

// Concrete strategies
public class AStarPathfinding implements PathfindingStrategy {
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // A* algorithm
    }
}

public class DijkstraPathfinding implements PathfindingStrategy {
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // Dijkstra's algorithm
    }
}

// Context uses strategy
public class Navigator {
    private PathfindingStrategy strategy;

    public void setStrategy(PathfindingStrategy strategy) {
        this.strategy = strategy;
    }

    public List<BlockPos> navigate(BlockPos start, BlockPos end) {
        return strategy.findPath(start, end);
    }
}
```

**When to Use:**
- Multiple ways to do the same thing
- Need to switch algorithms at runtime
- Want to test algorithms in isolation

**Examples in Codebase:**
- `LLMClient` implementations (OpenAI, Groq, Gemini)
- `PathfindingStrategy` implementations
- `RecoveryStrategy` implementations

**Benefits:**
- Interchangeable algorithms
- Easy to add new strategies
- Testable in isolation

**Tradeoffs:**
- More classes than simple if-else
- Strategy selection logic needed
- May be overkill for simple cases

---

### Pattern 2.4: Chain of Responsibility

**Intent:** Pass a request along a chain of handlers until one handles it.

**Structure:**
```java
public interface ActionInterceptor {
    void intercept(ActionContext context, InterceptorChain chain);
}

public class InterceptorChain {
    private final List<ActionInterceptor> interceptors;
    private int current = 0;

    public void proceed(ActionContext context) {
        if (current < interceptors.size()) {
            ActionInterceptor interceptor = interceptors.get(current++);
            interceptor.intercept(context, this);
        }
    }
}

// Example interceptors
public class ValidationInterceptor implements ActionInterceptor {
    public void intercept(ActionContext context, InterceptorChain chain) {
        if (isValid(context)) {
            chain.proceed(context); // Pass to next
        } else {
            throw new ValidationException();
        }
    }
}

public class LoggingInterceptor implements ActionInterceptor {
    public void intercept(ActionContext context, InterceptorChain chain) {
        log(context);
        chain.proceed(context); // Always pass to next
    }
}
```

**When to Use:**
- Multiple processing steps in sequence
- Want flexible ordering of steps
- Steps may be added/removed dynamically

**Examples in Codebase:**
- `ActionInterceptor` chain for action processing
- LLM response processing pipeline
- Event handling pipeline

**Benefits:**
- Flexible composition
- Easy to add/remove handlers
- Each handler has single responsibility

**Tradeoffs:**
- Harder to debug execution flow
- Order can matter
- Performance overhead

---

## Communication Patterns

### Pattern 3.1: Async Request-Response

**Intent:** Execute slow operations asynchronously without blocking.

**Structure:**
```java
public class AsyncLLMClient {
    private final HttpClient client;

    public CompletableFuture<String> chat(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            // Make HTTP request (blocking, but in separate thread)
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            return response.body();
        }, executor); // Use custom executor
    }
}

// Usage
CompletableFuture<String> future = llmClient.chat("Build a house");
future.thenAccept(response -> {
    // Handle response when ready
    System.out.println("Got response: " + response);
});
```

**When to Use:**
- Operations take >50ms
- Can't block the calling thread
- Want to process other work while waiting

**Examples in Codebase:**
- All `LLMClient` implementations
- HTTP requests to external APIs
- Long-running computations

**Benefits:**
- Non-blocking
- Better resource utilization
- Can compose multiple async operations

**Tradeoffs:**
- More complex than synchronous code
- Error handling is trickier
- Debugging is harder
- Risk of thread pool exhaustion

---

### Pattern 3.2: Publish-Subscribe (Event Bus)

**Intent:** Components communicate without knowing each other.

**Structure:**
```java
public interface EventBus {
    <E extends Event> void publish(E event);
    <E extends Event> void subscribe(Class<E> eventType, EventHandler<E> handler);
    void unsubscribe(Object handler);
}

// Publisher
public class TaskExecutor {
    private final EventBus eventBus;

    public void completeTask(Task task) {
        // Do work
        eventBus.publish(new TaskCompleteEvent(task));
    }
}

// Subscriber
public class TaskTracker {
    public TaskTracker(EventBus eventBus) {
        eventBus.subscribe(TaskCompleteEvent.class, this::onTaskComplete);
    }

    private void onTaskComplete(TaskCompleteEvent event) {
        // Handle completion
    }
}
```

**When to Use:**
- Multiple components need to know about events
- Want loose coupling
- Events may have multiple subscribers

**Examples in Codebase:**
- `SimpleEventBus` for game events
- `AgentEventBus` for agent events
- `WorldEventBus` for world changes

**Benefits:**
- Loose coupling
- Multiple subscribers
- Easy to add new listeners

**Tradeoffs:**
- Harder to trace execution
- Potential for memory leaks (forgotten unsubscribe)
- No guaranteed delivery order
- Can be abused for direct communication

---

### Pattern 3.3: Blackboard

**Intent:** Shared workspace where components contribute knowledge.

**Structure:**
```java
public class Blackboard {
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public void remove(String key) {
        data.remove(key);
    }
}

// Agents contribute knowledge
blackboard.put("iron_ore_location", new BlockPos(100, 64, 200));
blackboard.put("zombie_spawner", new BlockPos(150, 60, 250));

// Agents read knowledge
Optional<BlockPos> ironLocation = blackboard.get("iron_ore_location", BlockPos.class);
```

**When to Use:**
- Multiple independent contributors
- Don't know who will consume the knowledge
- Want emergent behavior from contributions

**Examples in Codebase:**
- `Blackboard` class for shared agent knowledge
- World state caching
- Shared planning data

**Benefits:**
- Flexible knowledge sharing
- No direct dependencies
- Emergent behavior

**Tradeoffs:**
- No structured contracts
- Potential for naming conflicts
- Harder to trace data flow
- Can become dumping ground

---

## Coordination Patterns

### Pattern 4.1: Contract Net Protocol

**Intent:** Coordinate multiple agents through negotiation.

**Structure:**
```java
// Manager announces task
public class ContractNetManager {
    public void announceTask(Task task) {
        // Send to all available agents
        for (Agent agent : availableAgents) {
            agent.receiveCallForProposal(task);
        }
    }

    // Collect bids
    public void receiveBid(Agent agent, Bid bid) {
        bids.put(agent, bid);
    }

    // Award contract
    public void awardContract() {
        Agent bestAgent = selectBestAgent(bids);
        bestAgent.receiveContractAward(task);
    }
}

// Agents respond with bids
public class Agent {
    public void receiveCallForProposal(Task task) {
        if (canHandle(task)) {
            Bid bid = calculateBid(task);
            manager.receiveBid(this, bid);
        }
    }
}
```

**When to Use:**
- Multiple agents with different capabilities
- Tasks can be done by multiple agents
- Want to allocate tasks optimally

**Examples in Codebase:**
- `ContractNetManager` for multi-agent task allocation
- Task rebalancing across agents
- Resource auctioning

**Benefits:**
- Optimal allocation
- Agent autonomy
- Dynamic adaptation

**Tradeoffs:**
- Communication overhead
- Negotiation delay
- Complexity

---

### Pattern 4.2: Leader-Follower

**Intent:** One agent coordinates, others execute.

**Structure:**
```java
public class ForemanAgent extends Agent {
    private List<WorkerAgent> workers = new ArrayList<>();

    public void buildHouse(HouseSpec spec) {
        // Break into subtasks
        List<Task> tasks = decompose(spec);

        // Assign to workers
        for (Task task : tasks) {
            WorkerAgent worker = selectWorker(task);
            worker.assignTask(task);
        }

        // Coordinate execution
        coordinateTasks(tasks);
    }
}

public class WorkerAgent extends Agent {
    public void assignTask(Task task) {
        this.currentTask = task;
        execute(task);
    }
}
```

**When to Use:**
- Clear hierarchy exists
- Central coordination needed
- Workers are interchangeable

**Examples in Codebase:**
- Foreman entities coordinating worker entities
- Task distribution in multi-agent builds
- Central planning with distributed execution

**Benefits:**
- Clear authority
- Simple coordination
- Easy to understand

**Tradeoffs:**
- Single point of failure
- Leader bottleneck
- Less flexible

---

### Pattern 4.3: Hierarchical Task Network

**Intent:** Decompose complex tasks into hierarchies of subtasks.

**Structure:**
```java
public class HTNTask {
    private String name;
    private List<HTNTask> subtasks = new ArrayList<>();
    private DecompositionMethod method;

    public void execute(Agent agent) {
        if (subtasks.isEmpty()) {
            // Primitive task - execute directly
            executePrimitive(agent);
        } else {
            // Compound task - decompose
            method.decompose(this, agent);
            for (HTNTask subtask : subtasks) {
                subtask.execute(agent);
            }
        }
    }
}

// Task network:
// "Build House"
//   ├── "Gather Materials"
//   │   ├── "Mine Stone"
//   │   └── "Chop Wood"
//   ├── "Prepare Foundation"
//   └── "Build Walls"
```

**When to Use:**
- Complex, multi-step tasks
- Tasks can be decomposed in multiple ways
- Need flexible task execution

**Examples in Codebase:**
- `HTNPlanner` for complex task planning
- Task profile hierarchies
- Script task decomposition

**Benefits:**
- Handles complexity
- Multiple decomposition methods
- Reusable task networks

**Tradeoffs:**
- Complex to implement
- Planning can be expensive
- Hard to debug

---

## Data Patterns

### Pattern 5.1: Immutable Data

**Intent:** Use immutable objects to prevent unexpected changes.

**Structure:**
```java
public final class TaskSpec {
    private final String type;
    private final Map<String, Object> parameters;

    public TaskSpec(String type, Map<String, Object> parameters) {
        this.type = type;
        this.parameters = Map.copyOf(parameters); // Defensive copy
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return parameters; // Safe because it's immutable
    }

    // No setters!
}
```

**When to Use:**
- Data passed between threads
- Want to ensure consistency
- Data shouldn't change after creation

**Examples in Codebase:**
- `Task` objects
- `Command` objects
- Configuration objects
- Event objects

**Benefits:**
- Thread-safe by default
- No defensive copying needed
- Easier to reason about
- Can cache freely

**Tradeoffs:**
- Must create new objects for changes
- More object allocations
- Can't use for mutable state

---

### Pattern 5.2: Builder Pattern

**Intent:** Construct complex objects step by step.

**Structure:**
```java
public class ActionConfig {
    private final String actionType;
    private final int timeout;
    private final int retryCount;
    private final boolean logEnabled;

    private ActionConfig(Builder builder) {
        this.actionType = builder.actionType;
        this.timeout = builder.timeout;
        this.retryCount = builder.retryCount;
        this.logEnabled = builder.logEnabled;
    }

    public static class Builder {
        private String actionType;
        private int timeout = 60; // default
        private int retryCount = 3; // default
        private boolean logEnabled = true; // default

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder logEnabled(boolean logEnabled) {
            this.logEnabled = logEnabled;
            return this;
        }

        public ActionConfig build() {
            return new ActionConfig(this);
        }
    }
}

// Usage
ActionConfig config = new ActionConfig.Builder()
    .actionType("mine")
    .timeout(120)
    .retryCount(5)
    .build();
```

**When to Use:**
- Objects have many parameters
- Many parameters are optional
- Want readable object construction

**Examples in Codebase:**
- `ActionConfig.Builder`
- `LLMRequest.Builder`
- `TaskSpec.Builder`

**Benefits:**
- Readable construction
- Optional parameters handled well
- Can validate before building
- Can create immutable objects

**Tradeoffs:**
- More code than simple constructor
- Can be overkill for simple objects

---

### Pattern 5.3: Repository Pattern

**Intent:** Abstract data access behind an interface.

**Structure:**
```java
public interface AgentRepository {
    void save(AgentState state);
    Optional<AgentState> load(UUID id);
    void delete(UUID id);
    List<AgentState> loadAll();
}

public class FileAgentRepository implements AgentRepository {
    private final Path saveDir;

    public void save(AgentState state) {
        Path file = saveDir.resolve(state.getId() + ".json");
        Files.writeString(file, toJson(state));
    }

    public Optional<AgentState> load(UUID id) {
        Path file = saveDir.resolve(id + ".json");
        if (Files.exists(file)) {
            return Optional.of(fromJson(Files.readString(file)));
        }
        return Optional.empty();
    }
}

public class InMemoryAgentRepository implements AgentRepository {
    private final Map<UUID, AgentState> store = new HashMap<>();

    public void save(AgentState state) {
        store.put(state.getId(), state);
    }

    public Optional<AgentState> load(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
```

**When to Use:**
- Need to swap data storage implementation
- Want to test without real storage
- Multiple storage types needed

**Examples in Codebase:**
- `AgentRepository` for agent persistence
- `MemoryRepository` for agent memories
- `SkillRepository` for learned skills

**Benefits:**
- Swappable implementations
- Easy to test with mocks
- Single responsibility

**Tradeoffs:**
- More abstraction
- May not need flexibility
- Additional layer to maintain

---

## Lifecycle Patterns

### Pattern 6.1: Resource Acquisition Is Initialization (RAII)

**Intent:** Bind resource lifecycle to object lifecycle.

**Structure:**
```java
public class ScopedResource implements AutoCloseable {
    private final Resource resource;

    public ScopedResource() {
        this.resource = acquire();
    }

    public Resource get() {
        return resource;
    }

    @Override
    public void close() {
        resource.release();
    }
}

// Usage - automatic cleanup
try (ScopedResource scoped = new ScopedResource()) {
    Resource resource = scoped.get();
    // Use resource
} // Automatically released
```

**When to Use:**
- Resources need cleanup
- Want exception-safe cleanup
- Resource lifecycle matches object lifecycle

**Examples in Codebase:**
- Entity spawning/despawning
- Action start/complete/cancel
- Transaction management

**Benefits:**
- Automatic cleanup
- Exception-safe
- Clear lifecycle

**Tradeoffs:**
- Requires AutoCloseable
- Can't defer acquisition
- Must remember to use try-with-resources

---

### Pattern 6.2: Initialization-On-Demand

**Intent:** Defer expensive initialization until needed.

**Structure:**
```java
public class LazyExpensiveObject {
    private volatile ExpensiveObject instance;

    public ExpensiveObject get() {
        ExpensiveObject result = instance;
        if (result == null) {
            synchronized (this) {
                result = instance;
                if (result == null) {
                    instance = result = createExpensiveObject();
                }
            }
        }
        return result;
    }

    private ExpensiveObject createExpensiveObject() {
        // Expensive creation
        return new ExpensiveObject();
    }
}
```

**When to Use:**
- Object creation is expensive
- Object may not be needed
- Want to minimize startup time

**Examples in Codebase:**
- LLM client initialization
- Cache initialization
- Optional subsystems

**Benefits:**
- Faster startup
- Only create if needed
- Thread-safe with double-check

**Tradeoffs:**
- Slight performance overhead on access
- More complex than eager init
- First access may be slow

---

## Error Handling Patterns

### Pattern 7.1: Result Type

**Intent:** Represent success or failure without exceptions.

**Structure:**
```java
public final class Result<T, E> {
    private final T value;
    private final E error;
    private final boolean isSuccess;

    private Result(T value, E error, boolean isSuccess) {
        this.value = value;
        this.error = error;
        this.isSuccess = isSuccess;
    }

    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null, true);
    }

    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error, false);
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public T getValue() {
        if (!isSuccess) {
            throw new IllegalStateException("Not a success");
        }
        return value;
    }

    public E getError() {
        if (isSuccess) {
            throw new IllegalStateException("Not a failure");
        }
        return error;
    }
}

// Usage
Result<Action, String> result = parseAction(input);
if (result.isSuccess()) {
    Action action = result.getValue();
    execute(action);
} else {
    String error = result.getError();
    log.error("Failed to parse action: " + error);
}
```

**When to Use:**
- Expected failures (not exceptional)
- Want to avoid exception overhead
- Need to pass errors through async code

**Examples in Codebase:**
- Action parsing results
- LLM response parsing
- Configuration validation

**Benefits:**
- Explicit error handling
- No exception overhead
- Works with async

**Tradeoffs:**
- More verbose than exceptions
- Can be ignored (not forced)
- Not idiomatic Java

---

### Pattern 7.2: Retry with Exponential Backoff

**Intent:** Retry failing operations with increasing delays.

**Structure:**
```java
public class RetryPolicy {
    private final int maxAttempts;
    private final long initialDelayMs;
    private final double backoffMultiplier;

    public <T> T execute(Supplier<T> operation) {
        int attempt = 0;
        long delayMs = initialDelayMs;

        while (attempt < maxAttempts) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new RuntimeException("Failed after " + maxAttempts + " attempts", e);
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
                delayMs *= backoffMultiplier;
            }
        }
        throw new AssertionError("Should not reach here");
    }
}

// Usage
RetryPolicy retry = new RetryPolicy(3, 1000, 2.0);
String result = retry.execute(() -> llmClient.chat(prompt));
```

**When to Use:**
- Operations may fail transiently
- Want resilience to temporary failures
- External service calls

**Examples in Codebase:**
- LLM API calls with `Resilience4j`
- HTTP requests
- Database connections

**Benefits:**
- Resilience to transient failures
- Configurable behavior
- Reduces overall failure rate

**Tradeoffs:**
- Delays failure detection
- Can increase load
- More complex error handling

---

## Performance Patterns

### Pattern 8.1: Object Pooling

**Intent:** Reuse objects instead of creating/destroying.

**Structure:**
```java
public class ObjectPool<T> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final int maxSize;

    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
    }

    public T acquire() {
        T obj = pool.poll();
        if (obj == null) {
            obj = factory.get();
        }
        return obj;
    }

    public void release(T obj) {
        if (pool.size() < maxSize) {
            pool.offer(obj);
        }
    }
}

// Usage for pathfinding nodes
ObjectPool<PathNode> nodePool = new ObjectPool<>(() -> new PathNode(), 1000);

PathNode node = nodePool.acquire();
// Use node
nodePool.release(node);
```

**When to Use:**
- Objects are expensive to create
- Many short-lived objects
- Causing GC pressure

**Examples in Codebase:**
- Pathfinding node pools
- Event object pools
- Buffer pools

**Benefits:**
- Reduced GC pressure
- Faster allocation
- Better performance

**Tradeoffs:**
- More complex code
- Must reset objects before reuse
- Potential for stale data

---

### Pattern 8.2: Caching

**Intent:** Store expensive computation results for reuse.

**Structure:**
```java
public class Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    private final Function<K, V> computeFunction;

    public Cache(int maxSize, Function<K, V> computeFunction) {
        this.maxSize = maxSize;
        this.computeFunction = computeFunction;
    }

    public V get(K key) {
        return cache.computeIfAbsent(key, k -> {
            // Evict if at capacity (LRU would be better)
            if (cache.size() >= maxSize) {
                cache.clear();
            }
            return computeFunction.apply(k);
        });
    }

    public void invalidate(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }
}

// Usage
Cache<String, List<BlockPos>> pathCache = new Cache(1000, this::findPath);

List<BlockPos> path = pathCache.get(start + "->" + end);
```

**When to Use:**
- Expensive computations
- Repeated with same inputs
- Results don't change often

**Examples in Codebase:**
- `SemanticCache` for LLM responses
- Pathfinding cache
- Configuration cache

**Benefits:**
- Avoid repeated work
- Faster responses
- Reduced resource usage

**Tradeoffs:**
- Stale data
- Memory overhead
- Cache invalidation complexity

---

## Testing Patterns

### Pattern 9.1: Test Builder

**Intent:** Simplify test object creation.

**Structure:**
```java
public class ActionTestBuilder {
    private String type = "test";
    private int duration = 100;
    private boolean canCancel = true;

    public static ActionTestBuilder anAction() {
        return new ActionTestBuilder();
    }

    public ActionTestBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public ActionTestBuilder withDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public ActionTestBuilder thatCannotCancel() {
        this.canCancel = false;
        return this;
    }

    public Action build() {
        return new TestAction(type, duration, canCancel);
    }
}

// Usage in tests
@Test
void testShortAction() {
    Action action = ActionTestBuilder.anAction()
        .withType("mine")
        .withDuration(10)
        .build();

    // Test...
}
```

**When to Use:**
- Test objects are complex to create
- Many tests need similar objects
- Want readable tests

**Examples in Codebase:**
- Test builders for actions
- Test builders for agents
- Test builders for tasks

**Benefits:**
- Readable tests
- Reusable test setup
- Easy to modify

**Tradeoffs:**
- More code to maintain
- May not need for simple tests

---

### Pattern 9.2: Test Fixture

**Intent:** Set up common test state.

**Structure:**
```java
public abstract class ActionTestBase {
    protected ActionExecutor executor;
    protected TestWorld world;
    protected TestAgent agent;

    @BeforeEach
    void setUp() {
        world = new TestWorld();
        agent = new TestAgent(world);
        executor = new ActionExecutor(agent);
    }

    @AfterEach
    void tearDown() {
        executor.cleanup();
        world.cleanup();
    }

    protected void placeAgentAt(BlockPos pos) {
        agent.setPosition(pos);
    }

    protected void giveItem(Item item, int count) {
        agent.getInventory().add(item, count);
    }
}

// Specific test
class MineActionTest extends ActionTestBase {
    @Test
    void testMiningIron() {
        placeAgentAt(new BlockPos(0, 64, 0));
        world.placeBlock(new BlockPos(0, 63, 0), Blocks.IRON_ORE);

        Action action = new MineAction(Blocks.IRON_ORE, 1);
        executor.execute(action);

        waitForCompletion(action);
        assertEquals(1, agent.getInventory().count(Items.IRON_INGOT));
    }
}
```

**When to Use:**
- Multiple tests need similar setup
- Setup is complex
- Want consistent test environment

**Examples in Codebase:**
- Base classes for action tests
- Base classes for LLM tests
- Base classes for agent tests

**Benefits:**
- Consistent setup
- Less duplication
- Easier to add tests

**Tradeoffs:**
- Base class can become large
- May force unnecessary setup
- Harder to understand without reading base

---

## Pattern Index

### By Purpose

| Purpose | Patterns |
|---------|----------|
| **Organization** | Layered Architecture, Plugin Architecture, Component-Based Entity |
| **Behavior** | State Machine, Tick-Based Update, Strategy, Chain of Responsibility |
| **Communication** | Async Request-Response, Pub-Sub, Blackboard |
| **Coordination** | Contract Net, Leader-Follower, HTN |
| **Data** | Immutable Data, Builder, Repository |
| **Lifecycle** | RAII, Initialization-On-Demand |
| **Errors** | Result Type, Retry with Backoff |
| **Performance** | Object Pooling, Caching |
| **Testing** | Test Builder, Test Fixture |

### By Frequency in Codebase

| Pattern | Frequency | Where Used |
|---------|-----------|------------|
| Async Request-Response | Very High | LLM clients, HTTP calls |
| State Machine | High | Agent lifecycle, actions |
| Strategy | High | LLM providers, pathfinding |
| Plugin Architecture | High | Actions, skills |
| Builder | Medium | Configuration |
| Repository | Medium | Persistence |
| Pub-Sub | Medium | Events |
| Blackboard | Medium | Multi-agent |

---

## Conclusion

**Pattern Language Principles:**

1. **Patterns solve recurring problems** - Use them when you recognize the problem
2. **Patterns can be combined** - Real solutions use multiple patterns together
3. **Patterns are not rules** - Adapt them to your specific needs
4. **Patterns evolve** - The codebase's patterns will continue to evolve

**When Applying Patterns:**

1. **Recognize the problem** - What are you trying to solve?
2. **Find matching patterns** - Which patterns address this problem?
3. **Adapt the pattern** - How can this pattern work in your context?
4. **Document your usage** - Help others recognize the pattern

**The Meta-Pattern:**
> All patterns in this codebase serve the core philosophy: "One Abstraction Away"
> — LLMs plan, traditional AI executes, at appropriate timescales.

---

**Document Version:** 1.0
**Last Updated:** 2026-03-05
**Maintained By:** MineWright Project
**Status:** Active - Pattern Language Reference
