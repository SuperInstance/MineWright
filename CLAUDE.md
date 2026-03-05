# MineWright - Claude Code Project Guide

**Project:** MineWright - AI-Powered Minecraft Companions
**Version:** 5.0
**Last Updated:** 2026-03-05
**Status:** Production-Ready

---

## Executive Summary

**MineWright** is "Cursor for Minecraft" — a sophisticated Minecraft mod that brings autonomous AI companions to the game. Users interact with these AI agents through natural language commands, and the agents execute complex tasks using a hybrid architecture combining LLM-powered planning with traditional game AI.

**Core Innovation:** The "One Abstraction Away" architecture — LLMs handle high-level planning and strategy (updating every 30-60 seconds), while traditional AI (behavior trees, FSMs, pathfinding) handles real-time execution (20 ticks per second). This design enables rich AI behavior with minimal LLM token usage and zero blocking of the game thread.

**Technical Achievement:** Production-grade multi-agent coordination with contract net protocol, semantic caching, cascade routing for cost optimization, skill learning from experience, persistent memory with vector search, and personality-driven dialogue systems.

---

## Table of Contents

1. [Project Identity](#project-identity)
2. [Architecture Deep Dive](#architecture-deep-dive)
3. [Codebase Navigation](#codebase-navigation)
4. [Development Workflow](#development-workflow)
5. [Testing & Quality](#testing--quality)
6. [Documentation Ecosystem](#documentation-ecosystem)
7. [Quick Reference](#quick-reference)

---

## Project Identity

### Vision

> **"Type what you want. They figure out how."**

MineWright aims to make Minecraft accessible through natural language. Players should be able to describe what they want in plain English — "Build a castle," "Mine 20 diamonds," "Set up a wheat farm" — and AI companions handle the execution details.

### Core Philosophy: "One Abstraction Away"

The system recognizes that different AI paradigms excel at different timescales:

| Paradigm | Excels At | Timescale | Token Cost |
|----------|-----------|-----------|------------|
| **LLMs** | Planning, strategy, natural language | 30-60 seconds | Low (batched) |
| **Traditional AI** | Real-time control, execution | Per tick (20 TPS) | Zero |
| **Game API** | World interaction | Per tick (20 TPS) | N/A |

By combining these paradigms at their natural boundaries, MineWright achieves:
- **60 FPS gameplay** — No blocking LLM calls
- **10-20x fewer tokens** — LLM plans, traditional AI executes
- **Rich behavior** — LLM creativity + deterministic execution
- **Scalability** — Multiple agents coordinate without conflicts

### Technology Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **Platform** | Minecraft Forge 1.20.1 | Stable, well-documented modding API |
| **Language** | Java 17 | Modern Java features, excellent performance |
| **LLM Providers** | Groq, OpenAI, Gemini, GLM | Provider-agnostic interface |
| **Concurrency** | ConcurrentHashMap, AtomicInteger | Lock-free multi-agent coordination |
| **Caching** | Caffeine 3.1.8 | High-performance, W-TinyLFU eviction |
| **Resilience** | Resilience4j 2.3.0 | Retry, circuit breaker, rate limiting |
| **Scripting** | GraalVM JS 24.1.2 | Dynamic code execution, hot-reloading |
| **Networking** | Java 11+ HttpClient | Async HTTP, connection pooling |

---

## Architecture Deep Dive

### Three-Layer Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│                                                                  │
│  Responsibilities:                                               │
│  • Natural language understanding                               │
│  • Task planning and decomposition                               │
│  • Strategic decision-making                                    │
│  • Conversation and personality                                 │
│                                                                  │
│  Update: Every 30-60 seconds or on events                       │
│  Token Usage: LOW (batched, infrequent)                         │
│  Blocking: NEVER (async)                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates: Plans, Scripts, Commands
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│                                                                  │
│  Responsibilities:                                               │
│  • Execute behavior trees                                       │
│  • Run finite state machines                                    │
│  • Follow macro scripts                                         │
│  • Coordinate pathfinding                                       │
│                                                                  │
│  Update: Every tick (20 TPS)                                    │
│  Token Usage: ZERO (runs locally)                               │
│  Blocking: NEVER (incremental execution)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Issues: Block placements, Movement, Actions
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
│                                                                  │
│  Responsibilities:                                               │
│  • Place and break blocks                                       │
│  • Move entities                                                │
│  • Manage inventory                                             │
│  • Sense world state                                            │
│                                                                  │
│  Update: Every tick (20 TPS)                                    │
│  Thread: Minecraft server thread                                │
└─────────────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

**Decision 1: Async-First LLM Integration**
- **Why:** LLM calls take 500-5000ms; can't block game thread
- **How:** CompletableFuture with custom executor
- **Result:** Smooth gameplay even during complex planning

**Decision 2: Plugin-Based Action System**
- **Why:** Actions should be extensible without modifying core
- **How:** ActionRegistry with runtime registration
- **Result:** Community can add custom actions

**Decision 3: Event-Driven Communication**
- **Why:** Components need loose coupling
- **How:** SimpleEventBus with publish/subscribe
- **Result:** Easy to extend, test, and modify

**Decision 4: Blackboard for Shared Knowledge**
- **Why:** Multiple agents need to share discoveries
- **How:** ConcurrentHashMap-based shared workspace
- **Result:** Emergent team intelligence

**Decision 5: State Machine for Agent Lifecycle**
- **Why:** Agents have distinct modes with constrained transitions
- **How:** Enum-based state machine with validation
- **Result:** Explicit, debuggable, testable states

### Data Flow: Command to Execution

```
User Input (Natural Language)
    │
    ├─→ "/minewright order Alex 'Build a house'"
    │
    ▼
Input Validation & Sanitization
    │
    ▼
Task Planner (LLM)
    │  • Understands command
    │  • Plans subtasks
    │  • Generates structured plan
    │
    ▼
Script Parser
    │  • Converts plan to executable script
    │  • Validates dependencies
    │
    ▼
Action Queue
    │  • Prioritizes actions
    │  • Manages dependencies
    │
    ▼
Action Executor (Tick-Based)
    │  • Executes one action at a time
    │  • Each action runs over multiple ticks
    │  • Handles failures and retries
    │
    ▼
Minecraft World
```

---

## Codebase Navigation

### Project Statistics

| Metric | Value | Notes |
|--------|-------|-------|
| **Source Files** | 400+ Java files | Production code |
| **Source Lines** | 115,937 LOC | Measured with cloc |
| **Test Files** | 155 test files | JUnit 5 |
| **Test Lines** | 99,357 LOC | ~40% coverage |
| **Packages** | 40 | Organized by concern |
| **Documentation** | 500+ files | Markdown, diagrams |

### Package Structure

**Core Systems (Priority: HIGH):**
```
com.minewright
├── action/              # Task execution engine (945 LOC)
│   └── actions/         # Individual action implementations
├── llm/                 # LLM integration (16,280 LOC)
│   ├── async/           # Async client wrappers
│   ├── batch/           # Request batching
│   ├── cache/           # Semantic caching
│   ├── cascade/         # Tier-based routing
│   └── resilience/      # Retry, circuit breaker
├── entity/              # Foreman entities (773 LOC)
├── pathfinding/         # A* pathfinding (861 LOC)
├── skill/               # Skill learning system
└── script/              # Script parsing (800 LOC)
```

**Coordination Systems (Priority: MEDIUM):**
```
├── orchestration/       # Multi-agent orchestration
├── coordination/        # Contract Net Protocol
├── communication/       # Inter-agent messaging
└── blackboard/          # Shared knowledge
```

**Support Systems (Priority: LOW):**
```
├── config/              # Configuration management
├── personality/         # AI personality system
├── memory/              # Persistence and vector search
├── voice/               # Optional TTS/STT
├── event/               # Event bus
├── execution/           # State machines
└── util/                # Utilities
```

### Entry Points for Understanding

**Start Here:**
1. `MineWrightMod.java` — Mod initialization, service bootstrap
2. `ForemanEntity.java` — Main AI entity, tick loop
3. `ActionExecutor.java` — Task execution engine

**For LLM Integration:**
1. `LLMClient.java` — Provider interface
2. `GroqClient.java` — Example implementation
3. `PromptBuilder.java` — Context-aware prompt construction
4. `ResponseParser.java` — Structured response extraction

**For Multi-Agent:**
1. `ContractNetManager.java` — Task negotiation
2. `OrchestratorService.java` — Multi-agent coordination
3. `Blackboard.java` — Shared knowledge system

**For Actions:**
1. `Action.java` — Base interface
2. `MineAction.java` — Simple example
3. `BuildAction.java` — Complex example

---

## Development Workflow

### Build Commands

```bash
# Standard build (compilation + tests)
./gradlew build

# Quick build (skip tests)
./gradlew build -x test

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests ActionExecutorTest

# Generate coverage report
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html

# Static analysis
./gradlew spotbugsMain      # Find bugs
./gradlew checkstyleMain    # Style checking

# Launch for development
./gradlew runClient         # Start test client
./gradlew runServer         # Start test server
```

### Quality Gates

Before committing changes, ensure:
- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew build`
- [ ] No new SpotBugs warnings: `./gradlew spotbugsMain`
- [ ] No new Checkstyle violations: `./gradlew checkstyleMain`
- [ ] Coverage not decreased: Check JaCoCo report

### Configuration

**Development Config:** `config/minewright-common.toml`

```toml
# AI Provider
[llm]
provider = "groq"  # groq, openai, gemini, zai

# Groq (Free, Fast)
[groq]
apiKey = "${GROQ_API_KEY}"
model = "llama3-70b-8192"

# Behavior
[behavior]
maxActiveCrew = 10
actionTickDelay = 20
enableChatResponses = true

# Performance
[performance]
aiTickBudgetMs = 5
enableSemanticCache = true
```

**Environment Variables:**
```bash
export GROQ_API_KEY="gsk_your_key_here"
export OPENAI_API_KEY="sk-your_key_here"
```

### Code Style

- **Indentation:** 4 spaces (no tabs)
- **Line limit:** 120 characters
- **Naming:** PascalCase (classes), camelCase (methods/variables), UPPER_SNAKE_CASE (constants)
- **Documentation:** JavaDoc for public APIs
- **Ordering:** static fields → instance fields → constructors → methods

### Common Patterns

**1. State Machine Pattern:**
```java
public enum AgentState {
    IDLE, PLANNING, EXECUTING, COMPLETED, CANCELLED
}

public void transition(AgentState from, AgentState to) {
    if (!validTransitions.get(from).contains(to)) {
        throw new IllegalStateException("Invalid transition");
    }
    currentState = to;
}
```

**2. Builder Pattern:**
```java
ActionConfig config = ActionConfig.builder()
    .type("mine")
    .timeout(120)
    .retryCount(3)
    .build();
```

**3. Strategy Pattern:**
```java
public interface LLMClient {
    CompletableFuture<String> chat(String prompt);
}

// Implementations: GroqClient, OpenAIClient, GeminiClient
```

**4. Async with Retry:**
```java
public CompletableFuture<Result> executeWithRetry(Request request) {
    return Retry.decorateAsync(
        retryConfig,
        () -> llmClient.chat(request.prompt())
    ).apply(request);
}
```

---

## Testing & Quality

### Test Structure

```
src/test/java/com/minewright/
├── action/              # Action tests
├── llm/                 # LLM integration tests
├── pathfinding/         # Pathfinding tests
├── script/              # Script parsing tests
└── testutil/            # Test utilities
```

### Test Patterns

**Unit Tests:**
```java
@Test
void testMineActionCompletesWithTargetQuantity() {
    // Given
    MineAction action = new MineAction(Blocks.IRON_ORE, 10);
    MockWorld world = new MockWorld();
    world.placeBlock(new BlockPos(0, 0, 0), Blocks.IRON_ORE);

    // When
    action.start(world);
    while (!action.isComplete()) {
        action.tick();
    }

    // Then
    assertEquals(10, action.getMinedCount());
}
```

**Integration Tests:**
```java
@Test
void testLLMClientEndToEnd() {
    // Given
    LLMClient client = new GroqClient(apiKey, model);

    // When
    CompletableFuture<String> future = client.chat("Say 'test'");
    String response = future.join();

    // Then
    assertNotNull(response);
    assertTrue(response.toLowerCase().contains("test"));
}
```

### Coverage Goals

| Package | Current | Target |
|---------|---------|--------|
| `action/` | 50%+ | 70%+ |
| `llm/` | 26% | 50%+ |
| `pathfinding/` | 40% | 60%+ |
| `script/` | 30% | 50%+ |
| Overall | 40% | 50%+ |

---

## Documentation Ecosystem

### For AI Agents (Claude Code)

**Start Here:**
1. [docs/KNOWLEDGE_INDEX.md](docs/KNOWLEDGE_INDEX.md) — Gateway to all knowledge
2. [docs/AGENT_ONBOARDING.md](docs/AGENT_ONBOARDING.md) — Getting started guide

**Deep Knowledge:**
3. [docs/META_COGNITION.md](docs/META_COGNITION.md) — How to think effectively
4. [docs/INVESTIGATION_PROTOCOLS.md](docs/INVESTIGATION_PROTOCOLS.md) — How to explore code
5. [docs/ARCHITECTURAL_WISDOM.md](docs/ARCHITECTURAL_WISDOM.md) — Why design decisions
6. [docs/PATTERN_LANGUAGE.md](docs/PATTERN_LANGUAGE.md) — Patterns in codebase
7. [docs/KNOWLEDGE_SYNTHESIS.md](docs/KNOWLEDGE_SYNTHESIS.md) — How it all connects

### For Human Developers

**Project Overview:**
- [README.md](README.md) — Project landing page

**Architecture:**
- [docs/architecture/TECHNICAL_DEEP_DIVE.md](docs/architecture/TECHNICAL_DEEP_DIVE.md)
- [docs/architecture/MULTI_AGENT_COORDINATION.md](docs/architecture/MULTI_AGENT_COORDINATION.md)

**Capabilities:**
- [docs/agent-guides/GUIDE_INDEX.md](docs/agent-guides/GUIDE_INDEX.md)

**Personality System:**
- [docs/characters/MASTER_CHARACTER_GUIDE.md](docs/characters/MASTER_CHARACTER_GUIDE.md)

**Development:**
- [docs/FUTURE_ROADMAP.md](docs/FUTURE_ROADMAP.md)

---

## Quick Reference

### In-Game Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/minewright spawn <name>` | Create companion | `/minewright spawn Alex` |
| `/minewright list` | List companions | `/minewright list` |
| `/minewright order <name> <cmd>` | Give task | `/minewright order Alex "Mine stone"` |
| `/minewright remove <name>` | Remove companion | `/minewright remove Alex` |
| **K** (key) | Open command GUI | Press K in-game |

### Key Classes

| Class | Purpose | Package |
|-------|---------|---------|
| `ForemanEntity` | Main AI entity | `entity` |
| `ActionExecutor` | Executes actions | `action` |
| `TaskPlanner` | LLM-powered planning | `llm` |
| `PromptBuilder` | Builds LLM prompts | `llm` |
| `ResponseParser` | Parses LLM responses | `llm` |
| `CascadeRouter` | Model selection | `llm.cascade` |
| `SemanticCache` | Request caching | `llm.cache` |
| `ContractNetManager` | Multi-agent negotiation | `coordination` |
| `AStarPathfinder` | Pathfinding | `pathfinding` |
| `SkillLibrary` | Learned skills | `skill` |

### Common Tasks

**Add a new action:**
```java
public class MyAction extends Action {
    @Override
    public void onStart() { /* init */ }

    @Override
    public void onTick() { /* execute incrementally */ }

    @Override
    public boolean isComplete() { /* check done */ }
}

// Register
ActionRegistry.register("my_action", MyAction.class);
```

**Add a new LLM provider:**
```java
public class MyLLMClient implements LLMClient {
    @Override
    public CompletableFuture<String> chat(String prompt) {
        // Implement async chat
    }
}

// Use
LLMClient client = new MyLLMClient(apiKey, model);
```

**Debug an agent:**
1. Enable debug logging in config
2. Check logs for agent state transitions
3. Look for pathfinding failures
4. Verify action queue processing

---

## Project Status

**Maturity:** Production-ready

**Recent Achievements:**
- ✅ Eliminated 11 god classes (91% avg reduction)
- ✅ Thread safety improvements (5 critical fixes)
- ✅ Performance optimization (95% faster emotional memory)
- ✅ Test coverage expansion (config, personality, DI packages)
- ✅ Complete rebrand from "Steve AI" to "MineWright"
- ✅ Comprehensive knowledge transfer framework (5000+ lines)

**Current Focus:**
- Code streamlining and refactoring
- Test coverage improvement
- Documentation enhancement
- Community preparation

**Known Issues:**
- See [docs/IMPROVEMENT_OPPORTUNITIES.md](docs/IMPROVEMENT_OPPORTUNITIES.md)

---

## Contributing

We welcome contributions! Areas of interest:
- New action implementations
- Additional LLM provider integrations
- Test coverage improvements
- Documentation enhancements
- Bug fixes and optimizations

**Process:**
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Ensure all tests pass
5. Submit a pull request

**See Also:** [CONTRIBUTING.md](CONTRIBUTING.md)

---

## License

MIT License — see [LICENSE](LICENSE)

---

**Document Version:** 5.0
**Last Updated:** 2026-03-05
**Maintained By:** MineWright Project
**Status:** Active - Primary Project Guide
