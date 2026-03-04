# Package Reference

This document provides a comprehensive reference to all packages in the Steve AI codebase.

## Package Overview

| Package | Purpose | Key Classes |
|---------|---------|-------------|
| `action/` | Task execution system | ActionExecutor, BaseAction, Task, ActionResult |
| `action/actions/` | Individual action implementations | MineAction, BuildAction, CraftAction, GatherAction |
| `behavior/` | Behavior tree runtime & process arbitration | BehaviorProcess, ProcessManager, BTNode |
| `behavior/composite/` | Composite behavior tree nodes | SequenceNode, SelectorNode, ParallelNode |
| `behavior/leaf/` | Leaf behavior tree nodes | ActionNode, ConditionNode |
| `behavior/decorator/` | Decorator behavior tree nodes | RetryNode, RepeatNode, InverterNode |
| `htn/` | Hierarchical Task Network planner | HTNPlanner, HTNMethod, HTNDomain, HTNWorldState |
| `blackboard/` | Shared knowledge system for agent coordination | Blackboard, BlackboardEntry, KnowledgeArea |
| `llm/` | LLM integration core | PromptBuilder, ResponseParser, TaskPlanner |
| `llm/async/` | Async LLM clients | AsyncLLMClient, AsyncOpenAIClient, AsyncGroqClient |
| `llm/batch/` | Batching infrastructure | BatchingLLMClient, PromptBatcher |
| `llm/cache/` | Semantic caching | SemanticLLMCache, TextEmbedder |
| `llm/cascade/` | Tier-based model selection for cost optimization | CascadeRouter, TaskComplexity, LLMTier |
| `llm/resilience/` | Resilience patterns (retry, circuit breaker, rate limiting) | ResilientLLMClient, LLMFallbackHandler |
| `execution/` | State machine with interceptors and event bus | AgentStateMachine, InterceptorChain |
| `coordination/` | Contract Net Protocol for multi-agent bidding | TaskBid, TaskAnnouncement, ContractNetManager |
| `dialogue/` | Proactive dialogue system for AI agents | ProactiveDialogueManager, DialogueTriggerChecker |
| `pathfinding/` | A* with hierarchical planning and path smoothing | AStarPathfinder, HierarchicalPathfinder |
| `memory/` | Persistence, semantic vector search, and personality | MemoryStore, ConversationManager |
| `memory/embedding/` | Text embedding models | EmbeddingModel, LocalEmbeddingModel |
| `memory/vector/` | Semantic vector search | InMemoryVectorStore, VectorSearchResult |
| `skill/` | Voyager-style skill library with composition | Skill, SkillComposer, ComposedSkill |
| `profile/` | Task profile system (Honorbuddy-inspired) | TaskProfile, ProfileExecutor, ProfileRegistry |
| `recovery/` | Stuck detection and recovery strategies | StuckDetector, RecoveryManager, StuckType |
| `recovery/strategies/` | Recovery strategy implementations | RepathStrategy, TeleportStrategy, AbortStrategy |
| `humanization/` | Human-like behavior utilities | HumanizationUtils, MistakeSimulator |
| `goal/` | Navigation goal composition (Baritone-inspired) | NavigationGoal, CompositeNavigationGoal |
| `rules/` | Declarative item rules engine | ItemRule, RuleEvaluator, ItemRuleRegistry |
| `script/` | Script DSL parsing and execution | Script, ScriptParser, ScriptGenerator |
| `plugin/` | Plugin architecture with SPI | ActionRegistry, ActionPlugin, PluginManager |
| `personality/` | AI personality system | ForemanArchetypeConfig, PersonalityTraits |
| `voice/` | TTS/STT integration | VoiceSystem, SpeechToText, TextToSpeech |
| `config/` | Configuration management | ConfigManager, MineWrightConfig |
| `client/` | Client-side GUI and input | ForemanOfficeGUI, ForemanOverlayScreen |
| `client/gui/` | GUI component refactoring (Wave 46) | GUIRenderer, InputHandler, MessagePanel |
| `entity/` | Entity coordination refactoring (Wave 46) | ForemanEntity, ActionCoordinator |
| `observability/` | Distributed tracing and metrics | TracingService, MetricsCollector, TraceSpan |
| `security/` | Input sanitization and validation | InputSanitizer |
| `command/` | Command registration | CommandRegistry |
| `communication/` | Inter-agent messaging | AgentMessage, CommunicationBus |
| `decision/` | Utility AI and decision making | UtilityScore |
| `di/` | Dependency injection | SimpleServiceContainer |
| `evaluation/` | Metrics and benchmarking | MetricsCollector |
| `event/` | Event system | EventBus, StateTransitionEvent |
| `mentorship/` | Teaching and learning system | MentorshipManager |
| `orchestration/` | Multi-agent orchestration | OrchestratorService, AgentCommunicationBus |
| `structure/` | Procedural generation | StructureGenerators |
| `util/` | Utility classes | TickProfiler, ActionUtils |

## Detailed Package Descriptions

### action/

**Purpose:** Core task execution system

**Key Classes:**
- `ActionExecutor` - Central execution engine, manages task queues and executes actions tick-by-tick
- `BaseAction` - Abstract base class for all actions
- `Task` - Represents a unit of work to be executed
- `ActionResult` - Represents the result of an action execution
- `ActionContext` - Context object passed to actions during execution

**Responsibilities:**
- Queue and prioritize tasks
- Create actions via ActionRegistry
- Execute actions in tick-based manner
- Manage action lifecycle (start, tick, complete)

### behavior/

**Purpose:** Behavior tree runtime engine and process arbitration system

**Key Classes:**
- `BTNode` - Base interface for all behavior tree nodes
- `ProcessManager` - Priority-based behavior arbitration
- `BehaviorProcess` - Interface for behavior processes

**Subpackages:**
- `composite/` - Control flow nodes (Sequence, Selector, Parallel)
- `leaf/` - Action and condition nodes
- `decorator/` - Modifier nodes (Retry, Repeat, Inverter)

**Responsibilities:**
- Execute behavior trees tick-by-tick
- Arbitrate between multiple behavior processes
- Provide composable AI behavior system

### htn/

**Purpose:** Hierarchical Task Network planner

**Key Classes:**
- `HTNPlanner` - Decomposes compound tasks into primitive actions
- `HTNMethod` - Alternative ways to accomplish tasks
- `HTNDomain` - Task decomposition knowledge base
- `HTNWorldState` - Current world state representation
- `HTNTask` - Tasks (compound or primitive)

**Responsibilities:**
- Decompose complex tasks into primitive actions
- Select appropriate methods based on preconditions
- Generate executable task sequences

### blackboard/

**Purpose:** Shared knowledge system for agent coordination

**Key Classes:**
- `Blackboard` - Central knowledge repository
- `BlackboardEntry` - Individual knowledge entries
- `KnowledgeArea` - Categories of knowledge (WORLD_STATE, THREATS, etc.)

**Responsibilities:**
- Store and retrieve shared knowledge
- Support subscriptions to knowledge areas
- Enable agent coordination through shared information

### llm/

**Purpose:** Core LLM integration

**Key Classes:**
- `PromptBuilder` - Build prompts for LLM calls
- `ResponseParser` - Parse LLM responses
- `TaskPlanner` - Plan tasks using LLM

**Subpackages:**
- `async/` - Async LLM clients for different providers
- `batch/` - Batching infrastructure for efficiency
- `cache/` - Semantic caching to reduce API calls
- `cascade/` - Tier-based model selection
- `resilience/` - Retry, circuit breaker, rate limiting

**Responsibilities:**
- Integrate with multiple LLM providers
- Provide async, non-blocking LLM calls
- Optimize costs through intelligent routing and caching

### memory/

**Purpose:** Memory system with persistence and semantic search

**Key Classes:**
- `MemoryStore` - Core memory storage
- `ConversationManager` - Conversation history tracking
- `ForemanMemory` - Foreman-specific memory
- `WorldKnowledge` - World state knowledge
- `PersonalitySystem` - Personality and relationship tracking

**Subpackages:**
- `embedding/` - Text embedding models
- `vector/` - Semantic vector search

**Responsibilities:**
- Store episodic, semantic, and emotional memories
- Enable semantic search via vector embeddings
- Track relationships and personality

### skill/

**Purpose:** Voyager-style skill library with composition

**Key Classes:**
- `Skill` - Represents a learned skill
- `SkillComposer` - Compose complex skills from simpler ones
- `ComposedSkill` - Executable composed skill
- `CompositionStep` - Individual composition step

**Responsibilities:**
- Store learned skills with semantic indexing
- Enable skill composition for complex tasks
- Track skill performance and success rates

### pathfinding/

**Purpose:** Advanced pathfinding with hierarchical planning

**Key Classes:**
- `AStarPathfinder` - Enhanced A* with Minecraft-specific optimizations
- `HierarchicalPathfinder` - Multi-level pathfinding for long distances
- `PathNode` - Represents a node in the path
- `MovementValidator` - Validates movement safety

**Responsibilities:**
- Find paths from A to B
- Optimize paths for Minecraft movement
- Support long-distance pathfinding via hierarchy

### recovery/

**Purpose:** Stuck detection and recovery

**Key Classes:**
- `StuckDetector` - Detect when agent is stuck
- `RecoveryManager` - Coordinate recovery attempts
- `StuckType` - Types of stuck conditions

**Subpackages:**
- `strategies/` - Recovery strategy implementations

**Responsibilities:**
- Detect when agent is stuck (position, progress, state, path)
- Execute recovery strategies (repath, teleport, abort)
- Track recovery statistics

### humanization/

**Purpose:** Human-like behavior utilities

**Key Classes:**
- `HumanizationUtils` - Gaussian jitter, reaction times
- `MistakeSimulator` - Probabilistic mistake triggering
- `IdleBehaviorController` - Human-like idle behaviors
- `SessionManager` - Play session tracking

**Responsibilities:**
- Add human-like variation to behaviors
- Simulate mistakes and imperfections
- Track fatigue and play sessions

### coordination/

**Purpose:** Multi-agent coordination via Contract Net Protocol

**Key Classes:**
- `ContractNetManager` - Manages contract net protocol
- `TaskBid` - Bid for task execution
- `TaskAnnouncement` - Announce available tasks
- `ContractNetProtocol` - Protocol implementation

**Responsibilities:**
- Enable distributed task allocation
- Support task bidding and awarding
- Coordinate multiple agents without central control

### dialogue/

**Purpose:** Proactive dialogue system

**Key Classes:**
- `ProactiveDialogueManager` - Main dialogue manager
- `DialogueTriggerChecker` - Check trigger conditions
- `DialogueCommentGenerator` - Generate dialogue content

**Responsibilities:**
- Trigger dialogue based on game events
- Generate contextual dialogue
- Track dialogue statistics

### config/

**Purpose:** Configuration management

**Key Classes:**
- `ConfigManager` - Manages configuration loading and reloading
- `MineWrightConfig` - Main configuration class

**Subpackages:**
- Various specialized config classes (LLMConfig, BehaviorConfig, etc.)

**Responsibilities:**
- Load and validate configuration
- Support configuration reloading
- Provide type-safe configuration access

### plugin/

**Purpose:** Plugin architecture for extensibility

**Key Classes:**
- `ActionRegistry` - Registry of action factories
- `ActionPlugin` - Interface for action plugins
- `PluginManager` - Loads and manages plugins

**Responsibilities:**
- Enable plugin-based action registration
- Support SPI-based plugin loading
- Resolve priority conflicts

### security/

**Purpose:** Input sanitization and validation

**Key Classes:**
- `InputSanitizer` - Sanitize user input to prevent prompt injection

**Responsibilities:**
- Detect and prevent prompt injection attacks
- Validate user input before LLM processing
- Log security events

### observability/

**Purpose:** Distributed tracing and metrics

**Key Classes:**
- `TracingService` - Distributed tracing
- `MetricsCollector` - Metrics collection
- `TraceSpan` - Individual trace spans

**Responsibilities:**
- Track distributed traces
- Collect performance metrics
- Enable observability and debugging

## Package Dependencies

### Core Dependencies

Most packages depend on these core packages:
- `action/` - Task execution
- `execution/` - State machine and interceptors
- `event/` - Event system
- `config/` - Configuration

### LLM Dependencies

- `llm/` and subpackages are relatively independent
- Used by `action/`, `dialogue/`, `skill/`

### AI System Dependencies

- `behavior/` uses `action/` for leaf nodes
- `htn/` uses `action/` for primitive tasks
- `pathfinding/` used by many action implementations

### Memory Dependencies

- `memory/` used by `dialogue/`, `personality/`, `skill/`
- `memory/vector/` provides semantic search

## File Organization

```
src/main/java/com/minewright/
├── action/                  # Task execution
├── behavior/               # Behavior trees
├── blackboard/             # Shared knowledge
├── client/                 # Client-side GUI
├── command/                # Commands
├── communication/          # Inter-agent messaging
├── config/                 # Configuration
├── coordination/           # Multi-agent coordination
├── decision/               # Utility AI
├── dialogue/               # Proactive dialogue
├── di/                     # Dependency injection
├── entity/                 # Entity coordination
├── evaluation/             # Metrics
├── event/                  # Event system
├── execution/              # State machine
├── goal/                   # Navigation goals
├── htn/                    # HTN planner
├── humanization/           # Human-like behavior
├── llm/                    # LLM integration
├── memory/                 # Memory system
├── mentorship/             # Teaching system
├── observability/          # Observability
├── orchestration/          # Multi-agent orchestration
├── pathfinding/            # Pathfinding
├── personality/            # Personality system
├── plugin/                 # Plugin system
├── profile/                # Task profiles
├── recovery/               # Stuck detection
├── rules/                  # Item rules
├── script/                 # Script DSL
├── security/               # Security
├── skill/                  # Skill library
├── structure/              # Procedural generation
├── util/                   # Utilities
└── voice/                  # Voice integration
```

## References

- **Architecture Overview:** `docs/ARCHITECTURE_OVERVIEW.md`
- **Patterns Guide:** `docs/PATTERNS_GUIDE.md`
- **Build Guide:** `docs/BUILD_GUIDE.md`
