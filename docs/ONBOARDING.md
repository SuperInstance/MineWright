# Steve AI - Onboarding Guide

**Last Updated:** 2026-02-28
**Status:** Active Development (85% Code Complete)

---

## Welcome to Steve AI

Steve AI is "Cursor for Minecraft" - autonomous AI agents that play Minecraft with you. Users type natural language commands, and AI-controlled Steve entities execute them through LLM-powered planning.

**Core Philosophy:** "One Abstraction Away" - LLMs plan and coordinate, while traditional game AI (behavior trees, FSMs, scripts) executes in real-time.

This creates agents that are:
- **Fast** - 60 FPS execution without blocking on LLM calls
- **Cost-efficient** - 10-20x fewer tokens than pure LLM approaches
- **Characterful** - Rich personalities, ongoing dialogue, relationship evolution

---

## Quick Start (5 Minutes)

### Prerequisites
- Java 17 installed
- Minecraft Forge 1.20.1
- An LLM API key (z.ai/GLM recommended, OpenAI/Groq/Gemini supported)

### Build & Run

```bash
# Clone and navigate to project
cd /path/to/steve

# Build the mod
./gradlew build

# Run client (for testing)
./gradlew runClient

# Run server
./gradlew runServer

# Run tests
./gradlew test
```

### Configure API Keys

Edit `config/minewright-common.toml` (created after first run):

```toml
[ai]
provider = "openai"  # Uses z.ai GLM-5 (recommended)

[openai]
apiKey = "your-zai-api-key"  # Get from console.z.ai
model = "glm-5"
maxTokens = 8000
temperature = 0.7
```

### Test in Game

1. Start Minecraft with the mod loaded
2. Press **K** to open the command GUI
3. Type: `/foreman spawn Steve`
4. Issue a command: `/foreman order Steve "Build a stone house"`

---

## Project Purpose

### What Is Steve AI?

Steve AI is a sophisticated multi-agent system that combines:
- **Natural Language Understanding** - LLM-powered task planning
- **Multi-Agent Coordination** - Foreman/worker pattern with spatial partitioning
- **Real-Time Execution** - Tick-based action system with zero blocking
- **Characterful AI** - Rich personalities, dialogue, relationships
- **Production Architecture** - State machines, event buses, lock-free coordination

### "One Abstraction Away" Philosophy

The core insight: LLMs should plan and refine, not execute every action. Traditional game AI handles real-time execution.

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│   • Planning, strategy, logistics                              │
│   • Conversations with player and other agents                 │
│   • Creating and refining automation scripts                   │
│   Token Usage: LOW (batched, infrequent calls)                 │
│   Update Frequency: Every 30-60 seconds or on events           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│   • Behavior trees, FSMs, macro scripts                        │
│   • Pathfinding, mining, building patterns                     │
│   Token Usage: ZERO (runs locally)                             │
│   Update Frequency: Every tick (20 TPS)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Executes via
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
│   • Block interactions, movement, inventory                    │
│   • Direct game API calls                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Current State

### Code Implementation: 85% Complete

**Fully Implemented:**
- ✅ Plugin system with ActionRegistry and ActionFactory
- ✅ State machine with AgentStateMachine (explicit transition validation)
- ✅ Interceptor chain (Logging, Metrics, EventPublishing)
- ✅ Event bus for agent coordination
- ✅ Async LLM clients (OpenAI, Groq, Gemini, z.ai/GLM)
- ✅ Batching LLM client for API efficiency
- ✅ Resilience patterns (retry, circuit breaker, rate limiting)
- ✅ Voice system framework (STT/TTS)
- ✅ Memory system with conversation tracking
- ✅ Vector search for semantic memory retrieval
- ✅ Code execution engine (GraalVM JS sandbox)
- ✅ Foreman archetype system with 8 personalities
- ✅ Multi-agent orchestration framework
- ✅ Behavior Tree Runtime Engine (composite/leaf/decorator nodes)
- ✅ HTN (Hierarchical Task Network) Planner
- ✅ Advanced Pathfinding (A*, hierarchical, path smoothing)
- ✅ Cascade Router (tier-based model selection)
- ✅ Evaluation Infrastructure (metrics collection, benchmarks)

**Partially Implemented:**
- 🔄 Action implementations (basic mining, building done - advanced features needed)
- 🔄 Multi-agent coordination (framework exists, needs protocol implementation)
- 🔄 Script layer generation (LLM→Script pipeline designed, not coded)
- 🔄 Skill auto-generation (infrastructure ready, learning loop not implemented)

**Not Started:**
- ⏳ Script DSL for automation patterns
- ⏳ MUD automation research integration
- ⏳ Small model fine-tuning (Cascade router exists, actual specialization not done)
- ⏳ Comprehensive evaluation pipeline

### Test Coverage: ~13%

**Critical Gap:** Only 29 test files for 223 source files. Priority: Add tests for core components (ActionExecutor, AgentStateMachine, InterceptorChain).

### Dissertation: A-Grade (92/100)

**Progress:** 60% integrated, targeting A+ (97+) with 10-15 hours of work.

---

## Architecture Overview

### Three-Layer Architecture

| Layer | Purpose | Technology | Update Rate |
|-------|---------|------------|-------------|
| **Brain** | Strategic planning, conversation | LLM (GLM-5, GPT-4) | 30-60s or event-driven |
| **Script** | Tactical execution, automation | BT, FSM, Scripts | Per tick (20 TPS) |
| **Physical** | Game world interaction | Minecraft API | Per tick (20 TPS) |

### Key Components

**ActionExecutor** (`C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`)
- Tick-based execution (non-blocking)
- Plugin architecture for extensible actions
- Interceptor chain for logging, metrics, events

**AgentStateMachine** (`C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`)
- States: IDLE, PLANNING, EXECUTING, WAITING, ERROR
- Event-driven transitions with explicit validation
- Thread-safe state management

**CascadeRouter** (`C:\Users\casey\steve\src\main\java\com\minewright\llm\cascade\`)
- Analyzes task complexity
- Routes to appropriate LLM tier (simple vs complex models)
- 40-60% cost reduction through intelligent model selection

---

## Key Files to Read

### Start Here (Essential)

1. **`C:\Users\casey\steve\CLAUDE.md`** (Line 1-668)
   - Main project guide and orchestration patterns
   - Complete architecture reference
   - Current state and priorities

2. **`C:\Users\casey\steve\src\main\java\com\minewright\MineWrightMod.java`** (Line 1-147)
   - Main mod entry point
   - Component initialization
   - Event registration

3. **`C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`** (Line 1-754)
   - Core execution engine
   - Tick-based execution model
   - Async LLM integration

4. **`C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentStateMachine.java`** (Line 1-285)
   - State machine implementation
   - Transition validation
   - Event publishing

5. **`C:\Users\casey\steve\src\main\java\com\minewright\action\actions\BaseAction.java`** (Line 1-235)
   - Base class for all actions
   - Action lifecycle (start, tick, cancel)
   - Error handling patterns

### Next Steps (Important)

6. **`C:\Users\casey\steve\src\main\java\com\minewright\plugin\ActionRegistry.java`** (Line 1-270)
   - Plugin architecture
   - Action registration pattern
   - Factory pattern implementation

7. **`C:\Users\casey\steve\src\main\java\com\minewright\config\MineWrightConfig.java`** (Line 1-1278)
   - Configuration system
   - All config options documented
   - Validation logic

8. **`C:\Users\casey\steve\src\main\java\com\minewright\execution\InterceptorChain.java`**
   - Cross-cutting concerns
   - Logging, metrics, events

9. **`C:\Users\casey\steve\src\main\java\com\minewright\event\EventBus.java`**
   - Event system
   - Agent coordination

10. **`C:\Users\casey\steve\build.gradle`** (Line 1-235)
    - Build configuration
    - Dependencies
    - Run configurations

---

## Common Tasks

### How to Add a New Action

**Step 1: Create Action Class**

```java
package com.minewright.action.actions;

public class MyCustomAction extends BaseAction {
    private final String myParameter;

    public MyCustomAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.myParameter = task.getStringParameter("param");
    }

    @Override
    protected void onStart() {
        // Validate and initialize
        requireParameter("param", "Parameter");
    }

    @Override
    protected void onTick() {
        // Called each game tick (20 TPS)
        // Do your work here, return when complete via succeed()
        // Progress tracking: do small chunks per tick

        if (isWorkComplete()) {
            succeed("Task completed successfully");
        }
    }

    @Override
    protected void onCancel() {
        // Cleanup when action is cancelled
    }

    @Override
    public String getDescription() {
        return "Performing custom action";
    }
}
```

**Step 2: Register Action**

```java
// In CoreActionsPlugin.java or create a new plugin
ActionRegistry registry = ActionRegistry.getInstance();
registry.register("myaction",
    (foreman, task, ctx) -> new MyCustomAction(foreman, task));
```

**Step 3: Test**

```java
@Test
public void testMyCustomAction() {
    ForemanEntity foreman = mockForeman();
    Task task = new Task("myaction", Map.of("param", "value"));

    MyCustomAction action = new MyCustomAction(foreman, task);
    action.start();

    // Tick until complete
    while (!action.isComplete()) {
        action.tick();
    }

    assertTrue(action.getResult().isSuccess());
}
```

### How to Test

**Unit Tests:**

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests MyCustomActionTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

**Integration Tests:**

```bash
# Run integration tests
./gradlew integrationTest
```

**Test Utilities:**

- `TaskBuilder` - Build test tasks easily
- `MockForemanEntity` - Mock entity for testing
- Test files are in `src/test/java/com/minewright/`

### How to Debug

**Enable Debug Logging:**

Edit `config/minewright-common.toml`:

```toml
[behavior]
enableChatResponses = true

# Add to your log4j.xml or use in-game
/debug forge
```

**Common Issues:**

**LLM API Timeout**
- Symptom: Tasks hang, no response
- Solution: Switch to faster provider (Groq), enable batching, increase timeout
- Check: `MineWrightConfig.OPENAI_API_KEY` is set correctly

**Agent Stuck**
- Symptom: Agent not moving, task not progressing
- Solution: Check pathfinding, verify navigation, increase stuck detection sensitivity
- Check logs: Look for pathfinding errors or action failures

**Out of Memory**
- Symptom: Mod crashes with OOM
- Solution: Reduce `MAX_ACTIVE_CREW_MEMBERS` in config, optimize structure generation, increase JVM heap
- Run with: `./gradlew runClient -Xmx4G`

### How to Add Configuration

**Step 1: Add Config Field**

```java
// In MineWrightConfig.java
public static final ForgeConfigSpec.ConfigValue<String> MY_CONFIG;

static {
    builder.push("mysection");
    MY_CONFIG = builder
        .comment("My configuration option")
        .define("myOption", "defaultValue");
    builder.pop();
}
```

**Step 2: Use in Code**

```java
String value = MineWrightConfig.MY_CONFIG.get();
```

**Step 3: Add Validation**

```java
// In validateNewFeatures()
if (MY_FEATURE_ENABLED.get()) {
    // Validate settings
    LOGGER.info("My Feature: enabled (option: {})", MY_CONFIG.get());
}
```

---

## Where Things Are

### Directory Structure

```
steve/
├── src/
│   ├── main/java/com/minewright/
│   │   ├── action/              # Task execution
│   │   │   ├── actions/         # Action implementations
│   │   │   ├── ActionExecutor.java
│   │   │   ├── ActionResult.java
│   │   │   └── Task.java
│   │   ├── behavior/            # Behavior tree nodes
│   │   ├── blackboard/          # Shared knowledge system
│   │   ├── client/              # GUI and input
│   │   ├── command/             # Command registration
│   │   ├── communication/       # Inter-agent messaging
│   │   ├── config/              # Configuration management
│   │   ├── coordination/        # Multi-agent coordination
│   │   ├── decision/            # Utility AI
│   │   ├── di/                  # Dependency injection
│   │   ├── entity/              # Entity definitions
│   │   ├── evaluation/          # Benchmarking
│   │   ├── event/               # Event system
│   │   ├── execution/           # State machine, interceptors
│   │   ├── htn/                 # Hierarchical task network
│   │   ├── llm/                 # LLM integration
│   │   │   ├── async/           # Async LLM clients
│   │   │   ├── batch/           # Batching infrastructure
│   │   │   ├── cascade/         # Cascade router
│   │   │   ├── cache/           # LLM response caching
│   │   │   └── resilience/      # Error handling
│   │   ├── memory/              # Persistence and retrieval
│   │   ├── mentorship/          # Learning system
│   │   ├── orchestration/       # Multi-agent orchestration
│   │   ├── pathfinding/         # Navigation
│   │   ├── personality/         # AI personality system
│   │   ├── plugin/              # Plugin architecture
│   │   ├── skill/               # Skill library
│   │   ├── structure/           # Procedural generation
│   │   ├── util/                # Utilities
│   │   └── voice/               # Voice integration
│   ├── test/java/com/minewright/
│   │   └── [mirror of main structure]
│   └── test/resources/
├── docs/
│   ├── research/                # Research documents (62 files)
│   ├── architecture/            # Architecture diagrams
│   └── examples/                # Code examples
├── config/                      # Generated config files
├── build.gradle                 # Build configuration
└── CLAUDE.md                    # Main project guide
```

### Package Summary

| Package | Purpose | Key Classes |
|---------|---------|-------------|
| `action` | Task execution | ActionExecutor, ActionResult |
| `action.actions` | Individual actions | MineAction, BuildAction, GatherAction |
| `behavior` | Behavior tree runtime | BehaviorNode, SequenceNode, SelectorNode |
| `execution` | State machine, interceptors | AgentStateMachine, InterceptorChain |
| `llm` | LLM integration | PromptBuilder, ResponseParser |
| `llm.async` | Async LLM clients | AsyncLLMClient, LLMExecutorService |
| `llm.batch` | Batching infrastructure | BatchingLLMClient, PromptBatcher |
| `llm.resilience` | Resilience patterns | ResilientLLMClient, LLMFallbackHandler |
| `plugin` | Plugin architecture | ActionRegistry, PluginManager |
| `memory` | Persistence and retrieval | CompanionMemory, ConversationManager |
| `orchestration` | Multi-agent coordination | OrchestratorService, AgentCommunicationBus |
| `personality` | AI personality system | ForemanArchetypeConfig, PersonalityTraits |
| `pathfinding` | Navigation | HierarchicalPathfinder, PathNode |
| `htn` | Hierarchical task network | HTNPlanner, Method, Task |
| `event` | Event system | EventBus, StateTransitionEvent |
| `config` | Configuration management | MineWrightConfig, ConfigManager |

---

## Getting Help

### Key Documentation

| Document | Purpose | Location |
|----------|---------|----------|
| **CLAUDE.md** | Main project guide | Root directory |
| **docs/README.md** | Documentation index (62 docs) | `docs/README.md` |
| **ONBOARDING.md** | This file | `docs/ONBOARDING.md` |
| **build.gradle** | Build configuration | Root directory |

### Research Documentation

Located in `docs/research/`:

- **NPC_SCRIPTING_EVOLUTION.md** - Philosophical foundation
- **VIVA_VOCE_CYCLE2_SYNTHESIS.md** - Dissertation progress
- **PRE_LLM_GAME_AUTOMATION.md** - MUD automation history
- **SCRIPT_GENERATION_SYSTEM.md** - LLM→Script pipeline
- **TESTING_STRATEGY.md** - Testing approach
- **PERFORMANCE_PROFILING.md** - Profiling guide

### Quick Links

- **Source Code:** `src/main/java/com/minewright/`
- **Research Docs:** `docs/research/`
- **Config:** `config/minewright-common.toml`
- **Build Output:** `build/libs/`
- **Tests:** `src/test/java/com/minewright/`

### Common Commands Reference

| Command | Arguments | Description |
|---------|-----------|-------------|
| `/foreman spawn` | `<name>` | Spawn a new Steve agent |
| `/foreman list` | | List all active agents |
| `/foreman remove` | `<name>` | Remove a Steve |
| `/foreman order` | `<name> <command>` | Issue work order |
| **K** | | Open command GUI |
| **ESC** | | Close GUI |

### Build Commands Reference

```bash
# Build
./gradlew build

# Run
./gradlew runClient
./gradlew runServer

# Test
./gradlew test
./gradlew integrationTest

# Distribution
./gradlew shadowJar reobfShadowJar

# Clean
./gradlew clean
```

---

## Next Steps

### For New Developers

1. **Read the Essentials** (1-2 hours)
   - CLAUDE.md (full document)
   - ActionExecutor.java (execution model)
   - AgentStateMachine.java (state management)
   - BaseAction.java (action lifecycle)

2. **Build and Run** (15 minutes)
   - Follow Quick Start guide
   - Spawn a foreman in game
   - Issue a test command

3. **Explore the Code** (2-3 hours)
   - Browse package structure
   - Read key implementation files
   - Review existing actions

4. **Make Your First Contribution** (2-4 hours)
   - Add a simple action
   - Write tests for it
   - Submit a pull request

### For AI Agents

**Priority Actions:**
1. Fix security issues (empty catch block, input validation)
2. Add tests for core components (ActionExecutor, AgentStateMachine)
3. Complete dissertation Chapter 3 integration
4. Implement script DSL for automation patterns

**Research vs Implementation Balance:**
- 40% research (exploring patterns, documenting findings)
- 40% implementation (writing code, fixing bugs)
- 20% testing and documentation

**Golden Rule:** Always keep at least one agent on actual code.

---

## Status Summary

**Project Status:** Research & Development with Active Building Phase

**Completion:**
- Code: 85% complete
- Tests: 13% coverage (critical gap)
- Dissertation: A-grade (92/100), 10-15 hours to A+

**Priority Actions:**
1. Fix security issues
2. Add tests for core components
3. Complete dissertation integration
4. Implement script DSL

**Dual Mission:**
1. Build a great mod - Functional, fun, AI-powered Minecraft companions
2. Systemize agent science - Document patterns, contribute to AI research, produce dissertation(s)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Maintained By:** Steve AI Development Team
**Next Review:** After major architecture changes or dissertation completion
