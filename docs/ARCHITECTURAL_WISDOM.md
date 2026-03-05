# Architectural Wisdom - Design Principles and Decisions

**Version:** 1.0
**Created:** 2026-03-05
**Purpose:** Document the "why" behind architectural decisions
**Target:** Future agents who need to understand design rationale

---

## Table of Contents

1. [Core Design Philosophy](#core-design-philosophy)
2. [Layered Architecture](#layered-architecture)
3. [Key Architectural Decisions](#key-architectural-decisions)
4. [Patterns and Their Rationale](#patterns-and-their-rationale)
5. [Tradeoffs and Alternatives](#tradeoffs-and-alternatives)
6. [Evolution and Lessons Learned](#evolution-and-lessons-learned)

---

## Core Design Philosophy

### "One Abstraction Away" Principle

**The Core Insight:**
> LLMs are excellent at high-level planning but terrible at low-level control.
> Traditional game AI is excellent at low-level control but can't plan.
> Put them together: LLM plans, traditional AI executes.

**Manifestation:**

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (LLM)                           │
│                                                                 │
│  • Understands natural language                                 │
│  • Plans strategy and logistics                                 │
│  • Makes high-level decisions                                   │
│  • Manages complexity                                            │
│                                                                 │
│  Update Frequency: Every 30-60 seconds or on events             │
│  Token Usage: LOW (batched, infrequent)                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates plans, scripts
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Traditional AI)                 │
│                                                                 │
│  • Executes behavior trees                                      │
│  • Runs finite state machines                                   │
│  • Follows scripts                                              │
│  • Handles real-time control                                    │
│                                                                 │
│  Update Frequency: Every tick (20 TPS)                          │
│  Token Usage: ZERO (runs locally)                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Issues commands
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Minecraft)                    │
│                                                                 │
│  • Interacts with blocks                                        │
│  • Moves entities                                               │
│  • Manages inventory                                            │
│  • Senses world state                                           │
│                                                                 │
│  Update Frequency: Every tick (20 TPS)                          │
└─────────────────────────────────────────────────────────────────┘
```

**Why This Works:**

1. **Each layer operates at its natural timescale**
   - LLM: Seconds (planning timescale)
   - Script: Ticks (execution timescale)
   - Physical: Ticks (game timescale)

2. **Each layer uses appropriate technology**
   - LLM: Natural language understanding
   - Script: Deterministic algorithms
   - Physical: Game API

3. **No blocking across layers**
   - LLM calls are async, don't block game
   - Scripts execute independently
   - Physical layer always responsive

**The Result:**
- 10-20x fewer LLM tokens (plans execute locally)
- 60 FPS gameplay (no blocking LLM calls)
- Rich behavior (LLM creativity + deterministic execution)

### Design Tenets

**1. Separation of Concerns**
- Each layer has a single responsibility
- Layers communicate through well-defined interfaces
- Changes in one layer don't cascade

**2. Appropriate Technology for Each Problem**
- LLM for: Planning, strategy, conversation
- Traditional AI for: Execution, control, reaction
- Game API for: World interaction

**3. Async-First Design**
- LLM calls are non-blocking
- Actions execute over multiple ticks
- Events drive communication

**4. Fail-Safe Behavior**
- LLM failure → fall back to scripts
- Script failure → fall back to basic actions
- System always has safe defaults

---

## Layered Architecture

### Why Three Layers?

**Alternatives Considered:**

| Approach | Pros | Cons | Decision |
|----------|------|------|----------|
| **LLM-only** | Simple, flexible | Expensive, slow, unreliable | ❌ Rejected |
| **Script-only** | Fast, deterministic | Inflexible, hard to configure | ❌ Rejected |
| **Two-layer (LLM + Direct)** | Simpler | No abstraction for common patterns | ❌ Rejected |
| **Three-layer (current)** | Flexible, fast, reusable | More complex | ✅ **Chosen** |

**The Sweet Spot:**

Three layers provide:
- **Flexibility** (LLM planning)
- **Efficiency** (local execution)
- **Reusability** (script patterns)

### Layer Boundaries

**Brain Layer Responsibility:**
- ✅ Understanding natural language
- ✅ Planning complex tasks
- ✅ Making strategic decisions
- ✅ Handling conversations
- ❌ NOT: Real-time control
- ❌ NOT: Block-by-block execution
- ❌ NOT: Reacting to immediate threats

**Script Layer Responsibility:**
- ✅ Executing behavior trees
- ✅ Running state machines
- ✅ Following predefined patterns
- ✅ Coordinating multiple actions
- ❌ NOT: Understanding language
- ❌ NOT: Making strategic decisions
- ❌ NOT: Direct world interaction

**Physical Layer Responsibility:**
- ✅ Placing/breaking blocks
- ✅ Moving entities
- ✅ Managing inventory
- ✅ Sensing the world
- ❌ NOT: Planning
- ❌ NOT: Pattern execution
- ❌ NOT: Strategic decisions

### Communication Between Layers

**Pattern: Command Pattern with Async Execution**

```java
// Brain layer generates command
Command command = llmClient.plan("build a house");

// Script layer interprets command
Script script = scriptParser.parse(command);

// Physical layer executes over time
actionExecutor.execute(script);
```

**Why Async?**
- LLM calls take 500-5000ms
- Game runs at 50ms per tick
- Can't block the game thread
- Commands must execute over time

---

## Key Architectural Decisions

### Decision 1: Plugin-Based Action System

**Problem:**
- Need extensible action system
- Don't know all actions in advance
- Want to allow community contributions

**Solution:**
```java
public interface Action {
    void onStart();
    void onTick();
    void onCancel();
    boolean isComplete();
}

// Actions registered at runtime
ActionRegistry.register(MineAction.class);
ActionRegistry.register(BuildAction.class);
```

**Why Plugin Architecture:**

1. **Extensibility**: Add new actions without modifying core
2. **Testability**: Test actions in isolation
3. **Discoverability**: Actions can list themselves
4. **Flexibility**: Actions can be added/removed dynamically

**Alternatives Considered:**
- ❌ Enum-based actions: Not extensible, hard to add parameters
- ❌ Hardcoded switch statement: Violates Open/Closed Principle
- ❌ Inheritance hierarchy: Too rigid, doesn't compose

### Decision 2: Event-Driven Communication

**Problem:**
- Components need to communicate without tight coupling
- Multiple subscribers may need same events
- Don't want to hardcode dependencies

**Solution:**
```java
public interface EventBus {
    void publish(Event event);
    void subscribe(Class<? extends Event> eventType, EventHandler handler);
}

// Components subscribe to events
eventBus.subscribe(TaskCompleteEvent.class, this::onTaskComplete);

// Components publish events
eventBus.publish(new TaskCompleteEvent(taskId));
```

**Why Event-Driven:**

1. **Decoupling**: Publishers don't know subscribers
2. **Scalability**: Easy to add new subscribers
3. **Flexibility**: Communication pattern, not hard dependencies
4. **Testability**: Easy to mock events

**Tradeoffs:**
- ✅ Pros: Loose coupling, flexible, scalable
- ❌ Cons: Harder to trace execution, potential for memory leaks

**Mitigation:**
- Document event flows
- Auto-unsubscribe on component destruction
- Profile for event handler performance

### Decision 3: Blackboard Pattern for Shared Knowledge

**Problem:**
- Multiple agents need to share information
- Don't want direct agent-to-agent dependencies
- Need flexible knowledge representation

**Solution:**
```java
public class Blackboard {
    public void put(String key, Object value);
    public <T> T get(String key, Class<T> type);
    public void remove(String key);
}

// Agents write to blackboard
blackboard.put("iron_location", new BlockPos(100, 64, 200));

// Agents read from blackboard
BlockPos location = blackboard.get("iron_location", BlockPos.class);
```

**Why Blackboard:**

1. **Flexibility**: Any agent can contribute any knowledge
2. **Decoupling**: Agents don't know each other
3. **Emergence**: Complex behaviors emerge from simple contributions
4. **Simplicity**: Simple key-value interface

**Alternatives Considered:**
- ❌ Direct messaging: Too many pairwise connections (O(n²))
- ❌ Shared database: Too heavy, slow access
- ❌ Message bus: Too structured for exploratory knowledge

### Decision 4: State Machine for Agent Lifecycle

**Problem:**
- Agents have complex lifecycle states
- States have specific transitions
- Need to track and visualize agent state

**Solution:**
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
    public void transition(AgentState from, AgentState to);
    public AgentState getCurrentState();
}
```

**Why State Machine:**

1. **Clarity**: States and transitions are explicit
2. **Safety**: Invalid transitions are caught
3. **Debuggability**: Easy to visualize state
4. **Testability**: Each state/transition can be tested

**State Transitions:**
```
    IDLE ──────→ PLANNING ──────→ EXECUTING ──────→ COMPLETED
        ↑            │                │
        │            ↓                ↓
        └──────── CANCELLED ←────── STUCK
```

### Decision 5: Interceptor Chain for Actions

**Problem:**
- Need cross-cutting concerns (logging, validation, retry)
- Don't want to duplicate code across actions
- Want composable behavior

**Solution:**
```java
public interface ActionInterceptor {
    void intercept(ActionContext context, InterceptorChain chain);
}

public class InterceptorChain {
    private List<ActionInterceptor> interceptors;

    public void proceed(ActionContext context) {
        // Each interceptor can:
        // 1. Modify the context
        // 2. Handle the action themselves
        // 3. Pass to next interceptor
    }
}
```

**Why Interceptor Chain:**

1. **Separation of Concerns**: Each interceptor handles one concern
2. **Composability**: Interceptors can be combined
3. **Reusability**: Same interceptors apply to all actions
4. **Flexibility**: Easy to add/remove interceptors

**Common Interceptors:**
- ValidationInterceptor
- LoggingInterceptor
- RetryInterceptor
- MetricsInterceptor

---

## Patterns and Their Rationale

### Pattern 1: Strategy Pattern for LLM Providers

**Problem:**
- Multiple LLM providers (OpenAI, Groq, Gemini)
- Different APIs, different capabilities
- Want to switch providers easily

**Solution:**
```java
public interface LLMClient {
    CompletableFuture<String> chat(String prompt);
    boolean isHealthy();
}

public class OpenAIClient implements LLMClient { ... }
public class GroqClient implements LLMClient { ... }
public class GeminiClient implements LLMClient { ... }
```

**Rationale:**
- **Abstraction**: Common interface for all providers
- **Swapability**: Change provider without changing code
- **Testing**: Easy to mock for tests
- **Competition**: Can use cheapest/fastest provider

### Pattern 2: Builder Pattern for Configuration

**Problem:**
- Actions have complex configuration
- Many optional parameters
- Want readable configuration code

**Solution:**
```java
Action action = MineAction.builder()
    .blockType(Blocks.IRON_ORE)
    .quantity(20)
    .depth(-48)
    .branchMining(true)
    .build();
```

**Rationale:**
- **Readability**: Configuration reads like English
- **Flexibility**: Optional parameters easily omitted
- **Immutability**: Built objects can be immutable
- **Validation**: Builder can validate before building

### Pattern 3: Observer Pattern for World Changes

**Problem:**
- Multiple components need to know about world changes
- Don't want to poll for changes
- Want reactive updates

**Solution:**
```java
public interface WorldObserver {
    void onBlockPlaced(BlockPos pos, BlockState state);
    void onBlockBroken(BlockPos pos);
    void onEntityAdded(Entity entity);
}

world.addObserver(blockTracker);
world.addObserver(pathfinder);
```

**Rationale:**
- **Reactivity**: Components react to changes immediately
- **Efficiency**: No polling overhead
- **Decoupling**: Observers don't know the source of changes
- **Scalability**: Easy to add/remove observers

### Pattern 4: Repository Pattern for Persistence

**Problem:**
- Need to save/load agent state
- Don't want persistence logic everywhere
- Want to swap storage mechanisms

**Solution:**
```java
public interface AgentRepository {
    void save(AgentState state);
    Optional<AgentState> load(UUID agentId);
    void delete(UUID agentId);
}

public class FileAgentRepository implements AgentRepository { ... }
public class DatabaseAgentRepository implements AgentRepository { ... }
```

**Rationale:**
- **Abstraction**: Business logic separated from persistence
- **Testability**: Easy to mock with in-memory repository
- **Flexibility**: Can change storage without changing business logic
- **Single Responsibility**: Each repository handles one type of data

### Pattern 5: Factory Pattern for Entity Creation

**Problem:**
- Entities have complex initialization
- Need different entity types
- Want to ensure valid entities

**Solution:**
```java
public interface EntityFactory {
    ForemanEntity createForeman(Level level, BlockPos pos, String name);
    WorkerEntity createWorker(Level level, BlockPos pos, ForemanEntity foreman);
}

public class MineWrightEntityFactory implements EntityFactory {
    public ForemanEntity createForeman(Level level, BlockPos pos, String name) {
        // Complex initialization logic
        // Set up dependencies
        // Validate state
        return foreman;
    }
}
```

**Rationale:**
- **Encapsulation**: Creation logic in one place
- **Validation**: Ensure valid entities
- **Flexibility**: Easy to add new entity types
- **Testing**: Can mock entity creation

---

## Tradeoffs and Alternatives

### Tradeoff 1: Complexity vs. Flexibility

**Decision:**
- ✅ Chose flexibility (plugin architecture, event system)
- ❌ Accept higher complexity

**Rationale:**
- Minecraft is complex and extensible by design
- Users will want to customize behavior
- Future requirements are unknown
- Complexity can be managed with good documentation

**Mitigation:**
- Comprehensive documentation
- Clear examples
- Common patterns documented
- Good abstractions to hide complexity

### Tradeoff 2: Performance vs. Abstraction

**Decision:**
- ✅ Chose abstraction (interfaces, patterns)
- ❌ Accept some performance overhead

**Rationale:**
- Premature optimization is evil
- Abstractions enable optimization
- Most performance gains come from algorithms, not micro-optimizations
- Can optimize hot paths later

**Mitigation:**
- Profile before optimizing
- Optimize hot paths aggressively
- Keep cold paths simple and clear
- Document performance characteristics

### Tradeoff 3: Synchronous vs. Asynchronous

**Decision:**
- ✅ Chose async for LLM calls
- ✅ Chose sync for in-game actions
- ❌ Accept complexity of async/sync boundary

**Rationale:**
- LLM calls are inherently slow (500-5000ms)
- Can't block game thread
- In-game actions are fast (per tick)
- Sync is simpler where possible

**Mitigation:**
- Clear async boundaries
- CompletableFuture for async operations
- Document what's async and what's sync
- Test for race conditions

### Tradeoff 4: Generality vs. Specificity

**Decision:**
- ✅ Chose general-purpose systems
- ❌ Accept some indirection

**Rationale:**
- General systems can be composed for specific needs
- Specific solutions often need generalization later
- Better to build general systems once
- Composition enables flexibility

**Example:**
- General Action interface vs. specific MineAction, BuildAction, etc.
- General EventBus vs. specific events
- General Blackboard vs. specific knowledge types

---

## Evolution and Lessons Learned

### Major Refactorings

**1. God Class Elimination (2025)**
- **Problem**: Classes like ScriptParser (1,029 lines), CompanionMemory (1,890 lines)
- **Solution**: Extracted specialized components
- **Result**: More maintainable, testable, understandable
- **Lesson**: Large classes are a smell, not a symptom. Look for multiple responsibilities.

**2. Async Architecture (2025)**
- **Problem**: LLM calls blocking game thread
- **Solution**: Made LLM calls async with CompletableFuture
- **Result**: Smooth gameplay even during planning
- **Lesson**: Identify blocking operations early and make them async by default.

**3. Event System Introduction (2024)**
- **Problem**: Tight coupling between components
- **Solution**: Introduced event bus for communication
- **Result**: Easier to extend and modify
- **Lesson**: Prefer events over direct method calls for cross-component communication.

**4. Pattern Consolidation (2025)**
- **Problem**: Duplicate code across similar actions
- **Solution**: Extracted common patterns (ValidatingAction, TimeLimitedAction)
- **Result**: Less duplication, easier to add new actions
- **Lesson**: Look for patterns, not just individual solutions.

### What We'd Do Differently

**1. Start with Async**
- **Mistake**: Made LLM calls sync initially
- **Fix**: Had to refactor to async later
- **Lesson**: If it might block, make it async from day one

**2. Define Interfaces First**
- **Mistake**: Implemented concrete classes, extracted interfaces later
- **Fix**: Start with interfaces, implement after
- **Lesson**: Interfaces define contracts; implement to the interface

**3. Testing from Day One**
- **Mistake**: Added tests after implementing features
- **Fix**: Write tests alongside or before implementation
- **Lesson**: TDD forces better design and catches bugs earlier

**4. Document as You Go**
- **Mistake**: Added documentation after features were complete
- **Fix**: Document decisions as you make them
- **Lesson**: Future you (and others) will thank you

### Architectural Principles Refined

**Original Principles:**
1. Keep it simple
2. Make it work
3. Make it fast

**Refined Principles:**
1. Make it clear (explicit over implicit)
2. Make it composable (small pieces, well-defined interfaces)
3. Make it observable (events, logging, metrics)
4. Make it testable (isolated, deterministic)
5. Make it simple (remove unnecessary complexity)

---

## Decision Framework for Future Decisions

### When Adding New Features

**Questions to Ask:**

1. **Which layer does this belong in?**
   - Brain (LLM)?
   - Script (traditional AI)?
   - Physical (game API)?
   - Or does it span layers?

2. **What patterns apply?**
   - Is this a new action? → Plugin pattern
   - Is this communication? → Event pattern
   - Is this state? → State machine pattern
   - Is this configuration? → Builder pattern

3. **What are the tradeoffs?**
   - Complexity vs. flexibility
   - Performance vs. abstraction
   - Generality vs. specificity
   - Sync vs. async

4. **How will this be tested?**
   - Can I test it in isolation?
   - Can I mock dependencies?
   - What are the edge cases?

5. **How will this evolve?**
   - What future requirements might emerge?
   - How extensible does this need to be?
   - What's the likely maintenance burden?

### When Refactoring

**Signs Refactoring is Needed:**

1. **God Classes** (>500 lines)
   - Extract components
   - Delegate responsibilities

2. **Duplicate Code**
   - Extract common patterns
   - Create abstractions

3. **Tight Coupling**
   - Introduce interfaces
   - Use events for communication

4. **Low Test Coverage**
   - Simplify design
   - Extract dependencies
   - Use dependency injection

---

## Conclusion: Architectural Wisdom

**Core Insights:**

1. **Right Abstraction Level** - Match the abstraction to the problem
2. **Appropriate Patterns** - Use patterns that fit the domain
3. **Explicit Over Implicit** - Make state and behavior visible
4. **Async by Default** - Anything that might block should be async
5. **Events for Communication** - Prefer publish/subscribe over direct calls
6. **Testability Matters** - Design for testability from the start
7. **Document Decisions** - Write down the "why," not just the "what"

**The Goal:**
> Build systems that are flexible enough to evolve, simple enough to understand, and robust enough to rely on.

---

**Document Version:** 1.0
**Last Updated:** 2026-03-05
**Maintained By:** MineWright Project
**Status:** Active - Architectural Decision Record
