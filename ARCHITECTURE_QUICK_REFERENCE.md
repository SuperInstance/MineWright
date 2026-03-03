# Architecture Quick Reference

**Cheat sheet for Steve AI architecture patterns and key implementation details.**

---

## Three-Layer Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                    │
│                                                                │
│  Components: TaskPlanner, GLMCascadeRouter, LLM Clients       │
│  Update Rate: 30-60 seconds or event-driven                   │
│  Token Usage: LOW (batched, infrequent)                       │
│  Purpose: Planning, strategy, conversation, script generation │
└────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                   │
│                                                                │
│  Components: Behavior Trees, HTN, FSMs, Utility AI            │
│  Update Rate: Every tick (20 TPS)                             │
│  Token Usage: ZERO (runs locally)                             │
│  Purpose: Tactical execution, reactive behaviors              │
└────────────────────────────────────────────────────────────────┘
                              │
                              │ Executes via
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                     │
│                                                                │
│  Components: ActionExecutor, Actions, Minecraft API           │
│  Update Rate: Every tick (20 TPS)                             │
│  Purpose: Block interactions, movement, inventory             │
└────────────────────────────────────────────────────────────────┘
```

---

## LLM Integration Architecture

### Cascade Router Flow

```
User Command
     │
     ├─► InputSanitizer.validate()
     │
     ├─► GLMCascadeRouter.processWithCascade()
     │       │
     │       ├─► preprocessMessage() with flashx
     │       │       │
     │       │       └─► Returns: {cleanedMessage, complexity, recommendedModel}
     │       │
     │       └─► routeToModel()
     │               │
     │               ├─► Try LocalLLMClient (if available & simple)
     │               │       └─► FREE! No API cost
     │               │
     │               ├─► Try cloud model (flashx → flash → glm5)
     │               │
     │               └─► Fallback chain with failure tracking
     │
     └─► ResponseParser.parseAIResponse()
             │
             └─► Returns: List<Task>
```

### LLM Client Hierarchy

```
AsyncLLMClient (interface)
    ├── AsyncOpenAIClient    → GPT-4, GPT-3.5
    ├── AsyncGroqClient      → Llama 3.1, 3.3
    ├── AsyncGeminiClient    → Gemini 1.5
    └── LocalLLMClient       → Ollama, vLLM (FREE)

BatchingLLMClient (wrapper)
    └── Combines multiple prompts → single API call
    └── Rate limit management
    └── Priority queue (user > background)

CascadeRouter
    └── ComplexityAnalyzer → TaskComplexity
    └── Tier selection: FAST | BALANCED | SMART
    └── LLMCache (40-60% hit rate)
```

### Model Selection Logic

| Complexity | Tier | Model | Use Case |
|------------|------|-------|----------|
| TRIVIAL | Cache | N/A | Repeated prompts |
| SIMPLE | FAST | llama-3.1-8b | Greetings, follow, wait |
| MODERATE | BALANCED | llama-3.3-70b | Gather, build, craft |
| COMPLEX | SMART | glm-5, gpt-4 | Strategy, debugging, multi-step |

---

## Action Execution Architecture

### Execution Flow

```
Task (from LLM)
     │
     ├─► ActionExecutor.execute(task)
     │       │
     │       ├─► InterceptorChain
     │       │       ├── LoggingInterceptor
     │       │       ├── MetricsInterceptor
     │       │       └── EventPublishingInterceptor
     │       │
     │       ├─► ActionFactory.create(task)
     │       │       └─► Returns: BaseAction subclass
     │       │
     │       └─► action.tick() [called every game tick]
     │               └─► Returns: true when complete
     │
     └─► ActionResult {success, message, metrics}
```

### Action Lifecycle

```
BaseAction
    │
    ├── onInitialize()    → Setup resources
    ├── onTick()          → Called 20x per second
    ├── onComplete()      → Cleanup on success
    ├── onFail()          → Cleanup on failure
    │
    └── isComplete()      → Check completion status
```

### Available Actions

| Action | Purpose | Key Parameters |
|--------|---------|----------------|
| `MineBlockAction` | Break blocks | block, quantity, location |
| `PlaceBlockAction` | Place blocks | block, location |
| `BuildStructureAction` | Build structures | structure, dimensions |
| `GatherResourceAction` | Collect items | resource, quantity |
| `CraftItemAction` | Crafting | item, quantity |
| `CombatAction` | Fight mobs | target |
| `FollowPlayerAction` | Follow entity | player |
| `PathfindAction` | Navigate | x, y, z |

---

## Behavior Tree Architecture

### Node Types

```
BTNode (abstract)
    │
    ├── Composite Nodes
    │       ├── SequenceNode   → All children must succeed
    │       ├── SelectorNode   → First success wins
    │       └── ParallelNode   → All children run concurrently
    │
    ├── Decorator Nodes
    │       ├── InverterNode   → Invert child result
    │       ├── RepeaterNode   → Repeat child N times
    │       └── TimerNode      → Time-limited execution
    │
    └── Leaf Nodes
            ├── ActionNode     → Execute an action
            └── ConditionNode  → Check world state
```

### Process Arbitration

```
ProcessManager
    │
    ├── Priority Queue:
    │       1. SurvivalProcess     (Priority 100)
    │       2. TaskExecutionProcess (Priority 50)
    │       3. FollowProcess        (Priority 30)
    │       4. IdleProcess          (Priority 10)
    │
    └── Only ONE process active at a time
    └── Preemption: Higher priority interrupts lower
```

---

## HTN Planner Architecture

### HTN Components

```
HTNPlanner
    │
    ├── Domain
    │       ├── Methods[]      → Ways to accomplish tasks
    │       └── Primitives[]   → Atomic actions
    │
    ├── HTNWorldState
    │       ├── agentHealth
    │       ├── inventory
    │       ├── nearbyBlocks
    │       └── targetPosition
    │
    └── Planning Process:
            1. Find applicable methods
            2. Check preconditions against world state
            3. Decompose compound tasks
            4. Return primitive task sequence
```

### Method Structure

```java
class HTNMethod {
    String name;                    // "Mine iron ore"
    Predicate<WorldState> preconditions;  // Has pickaxe?
    List<HTNTask> subtasks;         // [Goto, Mine, Collect]
    Consumer<WorldState> effects;   // Add iron to inventory
}
```

---

## Memory System Architecture

### Memory Types

```
CompanionMemory
    │
    ├── ConversationMemory
    │       ├── Short-term (last 10 exchanges)
    │       └── Long-term (important moments)
    │
    ├── SemanticMemory (Vector Store)
    │       ├── Embeddings (OpenAI or local)
    │       └── Cosine similarity search
    │
    └── EpisodicMemory
            ├── Events with timestamps
            └── Relationship evolution
```

### Vector Search Flow

```
Query → TextEmbedder.embed() → float[1536]
                                    │
                                    ▼
                            VectorStore.search()
                                    │
                                    ▼
                            Top-K similar memories
```

---

## Pathfinding Architecture

### Hierarchical Pathfinding

```
Long Distance (>100 blocks)
     │
     ├─► HierarchicalPathfinder
     │       ├── Chunk-level routing
     │       └── Returns waypoints
     │
     └─► AStarPathfinder (per segment)
             ├── Node pooling (memory efficient)
             ├── Heuristics (Manhattan, Euclidean)
             └── MovementValidator (safe blocks)
```

### Goal System (Baritone-style)

```java
NavigationGoal goal = Goals.any(
    Goals.block(100, 64, 200),      // Specific block
    Goals.nearEntity(player),        // Near player
    Goals.runAway(mob, 20)           // Escape from mob
);

// Goal evaluation:
// - isInGoal(pos) → boolean
// - heuristic(pos) → double (cost estimate)
```

---

## Skill Library Architecture

### Voyager-Style Skills

```
SkillLibrary
    │
    ├── Skills stored as:
    │       ├── Code (JavaScript/GraalVM)
    │       ├── Vector embedding
    │       ├── Success rate
    │       └── Dependencies
    │
    ├── Skill Retrieval:
    │       query → embed → cosine similarity → top-k skills
    │
    └── Skill Learning Loop:
            1. Execute task
            2. Extract pattern from success
            3. Generate skill with LLM
            4. Validate with Critic Agent
            5. Store if success rate > 80%
```

---

## Recovery System Architecture

### Stuck Detection

```
StuckDetector monitors:
    │
    ├── Position stuck (same pos for 5s)
    ├── Progress stuck (no task progress for 10s)
    ├── State stuck (same state for 30s)
    └── Path stuck (pathfinding failed 3x)
    │
    └── Triggers RecoveryManager
```

### Recovery Strategies

| Strategy | When Used | Action |
|----------|-----------|--------|
| `RepathStrategy` | Path issues | Recalculate path |
| `TeleportStrategy` | Completely stuck | Emergency teleport |
| `AbortStrategy` | Unrecoverable | Cancel task, report |

---

## Configuration Architecture

```toml
# config/steve-common.toml

[llm]
provider = "openai"           # Primary provider
cascade_enabled = true        # Intelligent routing
batching_enabled = true       # Rate limit management

[openai]
apiKey = "${OPENAI_API_KEY}"  # Env var support
model = "gpt-4"
maxTokens = 4096
temperature = 0.7

[groq]
apiKey = "${GROQ_API_KEY}"
model = "llama3-70b-8192"

[local]
enabled = true                # Free local inference
url = "http://localhost:11434/v1/chat/completions"
model = "llama3.2"
```

---

## Event System Architecture

```
EventBus (pub/sub)
    │
    ├── Events:
    │       ├── StateTransitionEvent
    │       ├── ActionCompletedEvent
    │       ├── ActionStartedEvent
    │       └── AgentMessageEvent
    │
    └── Subscribers:
            ├── MetricsCollector
            ├── ConversationTracker
            └── UI Updaters
```

---

## Key Design Patterns Used

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Plugin** | `plugin/` | Extensible action registration |
| **Interceptor Chain** | `execution/` | Cross-cutting concerns |
| **State Machine** | `execution/` | Clear agent states |
| **Observer** | `event/` | Event-driven coordination |
| **Strategy** | `recovery/strategies/` | Pluggable recovery behaviors |
| **Factory** | `plugin/ActionFactory` | Action instantiation |
| **Composite** | `behavior/composite/` | Behavior tree nodes |
| **Blackboard** | `blackboard/` | Shared knowledge |

---

## Performance Characteristics

| Metric | Value | Notes |
|--------|-------|-------|
| **Tick Rate** | 20 TPS | Minecraft standard |
| **LLM Latency** | 1-5s | Depends on model |
| **Cache Hit Rate** | 40-60% | Semantic caching |
| **Pathfinding** | <50ms | For 100-block paths |
| **Memory per Agent** | ~5MB | With full history |

---

## Thread Safety Patterns

```java
// Concurrent collections throughout
ConcurrentHashMap<String, AtomicInteger> failureCount;

// Lock-free counters
AtomicInteger taskCounter = new AtomicInteger(0);

// Volatile for visibility
private volatile boolean available = false;

// CompletableFuture for async
CompletableFuture<String> response = client.sendAsync(prompt);
```

---

*Last Updated: 2026-03-03*
