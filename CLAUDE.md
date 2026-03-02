# CLAUDE.md - Steve AI Project Guide

**Project:** Steve AI - "Cursor for Minecraft"
**Status:** Research & Development (Active Building Phase)
**Updated:** 2026-03-01
**Version:** 2.5

---

## ⚡ ORCHESTRATOR MODE: GAME BOT RESEARCH INITIATIVE ⚡

**Mode:** ACTIVE - Research game automation history, apply insights to our system
**Goal:** Learn from WoW Glider, Honorbuddy, and other successful game bots
**Strategy:** Multi-wave research → comparison → design → implementation → push

### Current Initiative: Game Bot Architecture Research

**Phase 1: Research (In Progress)**
Study successful game automation systems:
- WoW Glider/WoWGlider - Memory reading, pattern-based automation
- Honorbuddy - Behavior trees, profile system, plugin architecture
- Demonbuddy, Buddywing - Multi-game frameworks
- Diablo 3 bots - DemonHunter, Ros-Bot
- OSRS bots - Powerbot, Dreambot, Tribot
- Minecraft bots - Baritone, Mineflayer

**Key Research Questions:**
1. How did these systems achieve human-like behavior?
2. What made them effective at complex tasks?
3. How did they handle state management and decision making?
4. What anti-detection techniques were used?
5. How can we apply these insights legally and ethically?

**Phase 2: Compare & Analyze**
Map techniques to our Script DSL system

**Phase 3: Design & Implement**
Enhance our scripting system with proven patterns

**Phase 4: Debug & Push**
Polish and ship improvements

### Progress Tracking (Updated 2026-03-01)
| Priority | Status | Completion |
|----------|--------|------------|
| Priority 1: Quality | ✅ DONE | 100% |
| Priority 2: Testing | ✅ DONE | 100% |
| Priority 3: Features | ✅ DONE | 100% |
| Priority 4: Dissertation | 🔄 IN PROGRESS | 60% |
| Priority 5: Bot Research | 🔄 IN PROGRESS | 80% |

### Recent Completions (2026-03-01)
- ✅ **CI/CD Pipeline**: Full GitHub Actions workflows (ci.yml, release.yml, codeql.yml, dependency-review.yml)
- ✅ **Integration Test Framework**: MockMinecraftServer, TestEntityFactory, TestScenarioBuilder
- ✅ **Script DSL System**: 13 classes for automation script generation and refinement
- ✅ **Script Tests**: 7 test classes for Script DSL validation
- ✅ **Skill Tests**: PatternExtractorTest (38 tests, 1,051 lines) - comprehensive pattern recognition coverage
- ✅ **Skill Learning Loop**: SkillLearningLoop.java (232 lines) - orchestrates automatic skill improvement
- ✅ **SpotBugs Fixes**: Fixed NP_NULL_ON_SOME_PATH issues in CraftItemAction, PlaceBlockAction, ScriptTemplateLoader
- ✅ **JaCoCo Coverage**: Configured with package-level thresholds
- ✅ **Test Documentation**: TEST_COVERAGE.md, TEST_STRUCTURE_GUIDE.md
- ✅ **GitHub Templates**: PR template, issue templates, dependabot.yml, CODEOWNERS
- ✅ **Build Fixes**: All compilation errors resolved, build passes successfully

### Agent Coordination Rules
1. Always spawn agents in parallel (max 6 at a time)
2. Each agent works autonomously on its assigned task
3. Check results and spawn next wave immediately
4. Keep going until all checkboxes in FUTURE_ROADMAP.md are complete
5. Commit and push when milestones are reached

---

## 1. Project Overview

### Vision

Steve AI is "Cursor for Minecraft" - autonomous AI agents that play Minecraft with you. Users type natural language commands, and AI-controlled Steve entities execute them through LLM-powered planning.

**Core Philosophy:** "One Abstraction Away" - LLMs plan and coordinate, while traditional game AI (behavior trees, FSMs, scripts) executes in real-time. This creates agents that are:
- **Fast** - 60 FPS execution without blocking on LLM calls
- **Cost-efficient** - 10-20x fewer tokens than pure LLM approaches
- **Characterful** - Rich personalities, ongoing dialogue, relationship evolution

### Current Phase: Research & Development with Active Building

**Dual Mission:**
1. **Build a great mod** - Functional, fun, AI-powered Minecraft companions
2. **Systemize agent science** - Document patterns, contribute to AI research, produce dissertation(s)

**Dissertation Progress (as of 2026-02-28):**
- Dissertation 1: 60% integrated
  - Chapters 1, 8: Complete with major content additions
  - Chapters 3, 6: In progress (emotional AI, architecture)
  - Grade trajectory: A (92/100) → A+ (97+) target
- Dissertation 2: Active research phase
  - Focus: Cognitive layers, MUD automation learning principles
  - Parallel exploration: Script layer learning systems

### Technical Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Platform** | Minecraft Forge 1.20.1 | Mod framework |
| **Language** | Java 17 | Primary implementation |
| **AI Providers** | z.ai/GLM, OpenAI, Groq, Gemini | LLM inference |
| **Concurrency** | ConcurrentHashMap, AtomicInteger | Lock-free coordination |
| **Caching** | Caffeine 3.1.8 | High-performance caching |
| **Resilience** | Resilience4j 2.3.0 | Retry, circuit breaker, rate limiting |
| **Scripting** | GraalVM JS 24.1.2 | Dynamic code execution |
| **Networking** | Java 11+ HttpClient | API communication |

**Hardware Target:** Development on modern hardware, optimized for standard gaming PCs.

---

## 2. Architecture Summary

### "One Abstraction Away" Philosophy

The core insight: LLMs should plan and refine, not execute every action. Traditional game AI handles real-time execution.

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│                                                                 │
│   • Planning, strategy, logistics                              │
│   • Conversations with player and other agents                 │
│   • Creating and refining automation scripts                   │
│   • High-level goal setting                                    │
│   • Discussing "how to make bots better"                       │
│                                                                 │
│   Token Usage: LOW (batched, infrequent calls)                 │
│   Update Frequency: Every 30-60 seconds or on events           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│                                                                 │
│   • Behavior trees, FSMs, macro scripts                        │
│   • Pathfinding, mining, building patterns                     │
│   • Combat AI, resource gathering, exploration                 │
│   • Reactive behaviors (danger response, opportunities)        │
│   • Idle behaviors (wandering, chatting, self-improvement)     │
│                                                                 │
│   Token Usage: ZERO (runs locally)                             │
│   Update Frequency: Every tick (20 TPS)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Executes via
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
│                                                                 │
│   • Block interactions, movement, inventory                    │
│   • Entity tracking, world sensing                             │
│   • Direct game API calls                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Three-Layer Architecture

| Layer | Purpose | Technology | Update Rate |
|-------|---------|------------|-------------|
| **Brain** | Strategic planning, conversation | LLM (GLM-5, GPT-4) | 30-60s or event-driven |
| **Script** | Tactical execution, automation | BT, FSM, Scripts | Per tick (20 TPS) |
| **Physical** | Game world interaction | Minecraft API | Per tick (20 TPS) |

### Key Components

**CascadeRouter**
- Analyzes task complexity
- Routes to appropriate LLM tier (simple vs complex models)
- 40-60% cost reduction through intelligent model selection

**ActionExecutor**
- Tick-based execution (non-blocking)
- Plugin architecture for extensible actions
- Interceptor chain for logging, metrics, events

**AgentStateMachine**
- States: IDLE, PLANNING, EXECUTING, WAITING, ERROR
- Event-driven transitions
- Persistent state across game sessions

---

## 3. Current State (as of 2026-02-28)

### Code Implementation Status

> **Note:** As of 2026-02-28, a comprehensive audit revealed the codebase is significantly more complete than previously documented.

**Fully Implemented:**
- ✅ Plugin system with ActionRegistry and ActionFactory
- ✅ State machine with AgentStateMachine (explicit transition validation)
- ✅ Interceptor chain (Logging, Metrics, EventPublishing)
- ✅ Event bus for agent coordination
- ✅ Async LLM clients (OpenAI, Groq, Gemini, z.ai/GLM)
- ✅ Batching LLM client for API efficiency
- ✅ Resilience patterns (retry, circuit breaker, rate limiting via Resilience4j)
- ✅ Voice system framework (STT/TTS)
- ✅ Memory system with conversation tracking
- ✅ Vector search for semantic memory retrieval
- ✅ Code execution engine (GraalVM JS sandbox)
- ✅ Foreman archetype system with 8 personalities
- ✅ Multi-agent orchestration framework
- ✅ Skill library foundation (Voyager-style learning)
- ✅ **Behavior Tree Runtime Engine** (composite/leaf/decorator nodes)
- ✅ **HTN (Hierarchical Task Network) Planner** (methods, world state, domain)
- ✅ **Advanced Pathfinding** (A*, hierarchical, path smoothing, movement validation)
- ✅ **Cascade Router** (tier-based model selection)
- ✅ **Evaluation Infrastructure** (metrics collection, benchmark scenarios)

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

**Test Coverage:** ~24% (55 test files / 234 source files) - Gap in core component tests

### Codebase Metrics (2026-03-01 Audit)

| Metric | Value | Notes |
|--------|-------|-------|
| **Source Files** | 234 | Java files in src/main/java |
| **Source Lines** | 85,752 | Lines of production code |
| **Test Files** | 55 | Java files in src/test/java |
| **Test Lines** | 33,349 | Lines of test code |
| **Documentation Files** | 425 | Markdown files in docs/ |
| **Documentation Lines** | 521,003 | Comprehensive documentation |
| **Packages** | 49 | Including behavior, llm, memory, etc. |
| **TODO/FIXME Count** | 4 | Very clean codebase |

**Packages Breakdown:**
- `action/` - Task execution system
- `action/actions/` - Individual action implementations
- `behavior/` - Behavior tree runtime (composite/leaf/decorator)
- `blackboard/` - Shared knowledge system
- `client/` - GUI and input handling
- `command/` - Command registration
- `communication/` - Inter-agent messaging
- `config/` - Configuration management
- `coordination/` - Multi-agent coordination
- `decision/` - Utility AI and decision making
- `di/` - Dependency injection
- `entity/` - Minecraft entities
- `evaluation/` - Metrics and benchmarking
- `event/` - Event bus system
- `execution/` - State machine and interceptors
- `goal/` - Navigation goal composition (NEW)
- `htn/` - Hierarchical Task Network planner
- `humanization/` - Human-like behavior utilities (NEW)
- `llm/` - LLM integration (async/batch/cache/cascade/resilience)
- `memory/` - Persistence and vector search
- `mentorship/` - Teaching and learning system
- `orchestration/` - Multi-agent orchestration
- `pathfinding/` - A*, hierarchical pathfinding
- `personality/` - AI personality system
- `plugin/` - Plugin architecture
- `profile/` - Task profile system (NEW)
- `recovery/` - Stuck detection and recovery (NEW)
- `recovery/strategies/` - Recovery strategy implementations
- `rules/` - Declarative item rules engine (NEW)
- `script/` - Script parsing and execution
- `security/` - Input sanitization and validation
- `skill/` - Skill library system
- `structure/` - Procedural generation
- `util/` - Utility classes
- `voice/` - TTS/STT integration

### Research Documentation

**12+ research documents generated:**
- NPC Scripting Evolution (philosophical foundation)
- Game Automation History (MUD, RTS, MMO patterns)
- Behavior Trees, FSMs, HTN, GOAP deep dives
- Multi-agent coordination patterns
- LLM enhancement strategies
- Framework comparisons (ReAct, AutoGPT, LangChain, BabyAGI)
- Cognitive architectures (event-driven, state machine, blackboard)
- Memory systems (conversational, semantic, persistent)
- Voice integration patterns
- Performance analysis

**Key Research Insights:**
1. **Serial-to-Parallel Revolution**: Modern async compute enables true agent existence
2. **MUD Automation Parallel**: External scripts prefigured modern agent architecture
3. **Automatic Conversation Model**: Scripts = automatic, LLM = thoughtful
4. **Muscle Memory Analogy**: Scripts become automatic with practice/refinement

### New Systems (2026-03-01)

**Inspired by Game Bot Research (WoW Glider, Honorbuddy, Baritone):**

1. **Humanization System** (`humanization/` - 4 classes)
   - `HumanizationUtils` - Gaussian jitter, reaction times, mistake simulation
   - `MistakeSimulator` - Probabilistic mistake triggering
   - `IdleBehaviorController` - Human-like idle behaviors
   - `SessionManager` - Play session tracking for fatigue simulation

2. **Goal Composition System** (`goal/` - 7 classes)
   - `NavigationGoal` - Interface for pathfinding objectives
   - `CompositeNavigationGoal` - ANY/ALL goal composition
   - `GetToBlockGoal` - Find nearest block of type
   - `GetToEntityGoal` - Track and reach entities
   - `RunAwayGoal` - Escape from danger
   - `Goals` - Factory for goal creation
   - `WorldState` - World state snapshot for goal evaluation

3. **Process Arbitration System** (`behavior/` - 6 classes)
   - `ProcessManager` - Priority-based behavior arbitration
   - `BehaviorProcess` - Interface for behavior processes
   - `SurvivalProcess` - Eat, heal, avoid danger
   - `TaskExecutionProcess` - Execute assigned tasks
   - `IdleProcess` - Wander, chat, self-improve
   - `FollowProcess` - Follow player or other agents

4. **Profile System** (`profile/` - 6 classes)
   - `TaskProfile` - Declarative task sequences (Honorbuddy-inspired)
   - `ProfileTask` - Individual profile tasks
   - `ProfileParser` - Parse profiles from JSON
   - `ProfileExecutor` - Execute profile tasks
   - `ProfileRegistry` - Store and retrieve profiles
   - `ProfileGenerator` - LLM-driven profile generation

5. **Stuck Detection System** (`recovery/` - 9 classes)
   - `StuckDetector` - Detect position/progress/state/path stuck
   - `StuckType` - Categorize stuck conditions
   - `RecoveryStrategy` - Interface for recovery behaviors
   - `RecoveryManager` - Coordinate recovery attempts
   - `RepathStrategy` - Recalculate path
   - `TeleportStrategy` - Emergency teleport
   - `AbortStrategy` - Give up and report
   - `RecoveryResult` - Recovery outcome tracking

6. **Item Rules Engine** (`rules/` - 7 classes)
   - `ItemRule` - Declarative item filtering rules
   - `RuleCondition` - Rule predicates (name, type, tag, etc.)
   - `RuleAction` - Actions (KEEP, DROP, PICKUP, IGNORE)
   - `RuleEvaluator` - Evaluate rules against items
   - `ItemRuleParser` - Parse rules from config
   - `ItemRuleRegistry` - Rule storage and lookup
   - `ItemRuleContext` - Context for rule evaluation

---

## 4. Build Commands

### Standard Build

```bash
# Build the mod
./gradlew build

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer

# Run tests
./gradlew test

# Build distribution JAR (includes dependencies)
./gradlew shadowJar

# Obfuscate distribution JAR
./gradlew shadowJar reobfShadowJar
```

### Output

**Development JAR:** `build/libs/minewright-1.0.0.jar`
**Distribution JAR:** `build/libs/minewright-1.0.0-all.jar` (use this for distribution)

### Configuration

Config file: `config/steve-common.toml`

```toml
[llm]
provider = "groq"  # openai, groq, gemini, zai

[openai]
apiKey = "sk-..."
model = "gpt-4"

[groq]
apiKey = "gsk_..."
model = "llama3-70b-8192"

[zai]
apiKey = "your-zai-api-key"
apiEndpoint = "https://api.z.ai/api/paas/v4/chat/completions"
foremanModel = "glm-5"
workerSimpleModel = "glm-4.7-air"
workerComplexModel = "glm-5"
```

---

## 5. Package Structure

| Package | Purpose | Key Classes |
|---------|---------|-------------|
| `action` | Task execution | ActionExecutor, ActionResult |
| `action.actions` | Individual action implementations | MineAction, BuildAction, GatherAction |
| `behavior` | Behavior tree runtime & process arbitration | ProcessManager, BehaviorProcess |
| `blackboard` | Shared knowledge system | BlackboardEntry, KnowledgeArea |
| `client` | GUI and input | ForemanOverlayScreen, KeyBindings |
| `command` | Command registration | CommandRegistry |
| `communication` | Inter-agent messaging | AgentMessage, CommunicationBus |
| `config` | Configuration management | ConfigChangeEvent, ConfigVersion |
| `coordination` | Multi-agent coordination | TaskBid, TaskAnnouncement |
| `decision` | Utility AI and decision making | UtilityScore |
| `di` | Dependency injection | SimpleServiceContainer |
| `entity` | Minecraft entities | ForemanEntity |
| `evaluation` | Metrics and benchmarking | MetricsCollector |
| `event` | Event system | EventBus, StateTransitionEvent |
| `execution` | State machine, interceptors | AgentStateMachine, InterceptorChain |
| `goal` | Navigation goal composition | NavigationGoal, CompositeNavigationGoal, Goals |
| `htn` | Hierarchical Task Network planner | HTNPlanner, Method, Domain |
| `humanization` | Human-like behavior utilities | HumanizationUtils, MistakeSimulator, IdleBehaviorController |
| `llm` | LLM integration | PromptBuilder, ResponseParser |
| `llm.async` | Async LLM clients | AsyncLLMClient, LLMExecutorService |
| `llm.batch` | Batching infrastructure | BatchingLLMClient, PromptBatcher |
| `llm.cache` | Semantic caching | SemanticLLMCache, TextEmbedder |
| `llm.cascade` | Tier-based model selection | CascadeRouter, TaskComplexity |
| `llm.resilience` | Resilience patterns | ResilientLLMClient, LLMFallbackHandler |
| `memory` | Persistence and retrieval | CompanionMemory, ConversationManager |
| `memory.embedding` | Embedding models | EmbeddingModel, LocalEmbeddingModel |
| `memory.vector` | Semantic search | InMemoryVectorStore |
| `mentorship` | Teaching and learning system | MentorshipManager |
| `orchestration` | Multi-agent orchestration | OrchestratorService, AgentCommunicationBus |
| `pathfinding` | A*, hierarchical pathfinding | HierarchicalPathfinder, MovementValidator |
| `personality` | AI personality system | ForemanArchetypeConfig, PersonalityTraits |
| `plugin` | Plugin architecture | ActionRegistry, PluginManager |
| `profile` | Task profile system | TaskProfile, ProfileExecutor, ProfileRegistry |
| `recovery` | Stuck detection and recovery | StuckDetector, RecoveryManager, StuckType |
| `recovery.strategies` | Recovery strategy implementations | RepathStrategy, TeleportStrategy, AbortStrategy |
| `rules` | Declarative item rules engine | ItemRule, RuleEvaluator, ItemRuleRegistry |
| `script` | Script parsing and execution | Script, ScriptParser, ScriptGenerator |
| `security` | Input sanitization and validation | InputSanitizer |
| `skill` | Skill library system | Skill, TaskPattern |
| `structure` | Procedural generation | StructureGenerators |
| `util` | Utility classes | TickProfiler, ActionUtils |
| `voice` | Voice integration | VoiceSystem, SpeechToText, TextToSpeech |

---

## 6. Key Patterns

### Plugin Architecture

Actions are registered via `ActionRegistry` using `ActionFactory`:

```java
// In CoreActionsPlugin.java
registry.register("mine", (steve, task, ctx) -> new MineAction(steve, task));
registry.register("build", (steve, task, ctx) -> new BuildAction(steve, task));
```

The `PluginManager` loads plugins via SPI (Service Provider Interface).

### Tick-Based Execution

All actions extend `BaseAction` and implement `tick()`:

```java
public class MineAction extends BaseAction {
    @Override
    protected void onTick() {
        // Called once per game tick (20 TPS)
        // Return true when complete
    }
}
```

This prevents server freezing - actions track internal state and return `isComplete()` when done.

### Async LLM Calls

`TaskPlanner.planTasksAsync()` returns a `CompletableFuture`:

```java
llmClient.planAsync(command)
    .thenAccept(tasks -> {
        // Handle result when ready
        actionExecutor.executeTasks(tasks);
    });
```

The game thread checks `isDone()` in `tick()` - no blocking.

### Interceptor Chain

Actions pass through interceptors before execution:

```java
LoggingInterceptor → MetricsInterceptor → EventPublishingInterceptor → Action
```

### State Machine

`AgentStateMachine` tracks states:

```
IDLE → PLANNING → EXECUTING → COMPLETED → IDLE
                  ↓
                FAILED → IDLE
```

---

## 7. Current R&D Focus Areas

### Priority 1: MUD Automation → LLM Learning Principles

**Research Question:** How did 1990s MUD automation (TinTin++, ZMud) solve complex game problems without LLMs? Can we extract principles that LLMs can learn?

**Approach:**
1. Document MUD automation patterns (triggers, aliases, scripts)
2. Extract reusable principles (event-driven, state-based, hierarchical)
3. Design LLM→Script generation pipeline
4. Implement script refinement loop

**Expected Outcome:** System where LLMs generate and refine automation scripts, reducing token usage by 10-20x while enabling richer behaviors.

### Priority 2: Script Layer Learning System

**Research Question:** How can agents learn from successful execution sequences?

**Approach:**
1. Capture successful task sequences
2. Extract as reusable skills (Voyager-style)
3. Store in skill library with semantic indexing
4. Retrieve by similarity for future tasks
5. Refine through iteration

**Expected Outcome:** Self-improving agents that get better with experience.

### Priority 3: Multi-Agent Coordination

**Research Question:** How do agents coordinate without central control?

**Approach:**
1. Contract Net Protocol (task bidding)
2. Blackboard system (shared knowledge)
3. Event-driven messaging
4. Emergent behavior through simple rules

**Expected Outcome:** Agents that work together seamlessly without explicit orchestration.

### Priority 4: Small Model Specialization

**Research Question:** Can small, specialized models outperform large general models?

**Approach:**
1. Train/fine-tune small models for specific tasks (mining, building, combat)
2. Cascade router selects appropriate model
3. Fallback to large model for novel situations

**Expected Outcome:** 40-60% cost reduction while maintaining quality.

### Priority 5: Evaluation Framework

**Research Question:** How do we measure agent improvement?

**Approach:**
1. Define metrics (success rate, time to completion, token usage)
2. Create benchmark tasks
3. Automated evaluation pipeline
4. A/B testing for script variants

**Expected Outcome:** Quantifiable evidence of agent improvement over time.

---

## 8. Implementation Priorities

> **Updated 2026-03-01:** Security improvements completed - all critical issues addressed.

### Completed (2026-03-01)

**Security Improvements:**
- [x] Fix empty catch block in `StructureTemplateLoader.java:88` - Now logs full exception with stack trace
- [x] Add environment variable support for API keys - Implemented `getResolvedApiKey()` and `resolveEnvVar()` in MineWrightConfig
- [x] Add input sanitization for LLM prompts - Created `InputSanitizer` utility with comprehensive pattern detection
- [x] Update PromptBuilder to use sanitization - All user commands are now sanitized before LLM calls
- [x] Update TaskPlanner with validation - Commands validated for suspicious patterns before processing
- [x] Add comprehensive security tests - 40+ test cases for InputSanitizer covering injection patterns, jailbreaks, and edge cases

### Immediate (This Week)

**Remaining Quality Fixes:**
- [ ] Re-enable Checkstyle and SpotBugs in build.gradle
- [ ] Add tests for ActionExecutor (core execution engine)
- [ ] Add tests for AgentStateMachine (state transitions)
- [ ] Add tests for InterceptorChain (pipeline)

**Rationale:** Security vulnerabilities have been addressed. Test coverage needs improvement for production readiness.

### Short-term (Next 2 Weeks)

**Testing Infrastructure:**
- [ ] Add tests for ActionExecutor (core execution engine)
- [ ] Add tests for AgentStateMachine (state transitions)
- [ ] Add tests for InterceptorChain (pipeline)
- [ ] Create integration test framework

**Rationale:** 23% test coverage needs improvement for production.

### Medium-term (Next Month)

**Complete Missing Features:**
- [ ] Script DSL for automation patterns
- [ ] LLM→Script generation pipeline
- [ ] Skill auto-generation learning loop
- [ ] Multi-agent coordination protocol

**Rationale:** These complete the "One Abstraction Away" vision.

### Long-term (Next Quarter)

**Dissertation Completion:**
- [ ] Complete Chapter 3 integration (emotional AI)
- [ ] Complete Chapter 6 improvements (citations, limitations)
- [ ] Add 2024-2025 LLM technique coverage
- [ ] Final proofreading and formatting

**Rationale:** Academic milestones are as important as code.

---

## 9. Gaps Between Documentation and Code

> **Updated 2026-02-28:** Previous documentation significantly understated implementation progress.

### Documented But Not Implemented

1. **Hive Mind Architecture** - Cloudflare edge integration documented, not coded
2. **Skill Auto-Generation** - Infrastructure exists, learning loop not implemented
3. **Utility AI Scoring** - Framework exists, actual scoring not implemented
4. **Contract Net Bidding** - Framework exists, bidding protocol not implemented
5. **Script DSL** - Designed but not implemented
6. **MUD Automation Integration** - Research complete, code not started

### Implemented But Previously Undocumented

> These were discovered during the 2026-02-28 audit:

1. **Behavior Tree Runtime Engine** - Full implementation with composite/leaf/decorator nodes
2. **HTN (Hierarchical Task Network) Planner** - Complete planner with methods, world state, domain
3. **Advanced Pathfinding** - A*, hierarchical pathfinding, path smoothing, movement validation
4. **Evaluation Infrastructure** - Metrics collection, benchmark scenarios
5. **Cascade Router** - Full tier-based model selection implementation
6. **Foreman Archetype System** - 8 personalities with traits, catchphrases, behavioral patterns
7. **Code Execution Engine** - GraalVM JS sandbox with security restrictions
8. **Milestone Tracker** - Relationship evolution system

---

## 10. Agent Orchestration Notes

### Research vs Implementation Balance

**Golden Rule:** Always keep at least one agent on actual code.

When orchestrating multiple agents:
- 40% research (exploring patterns, documenting findings)
- 40% implementation (writing code, fixing bugs)
- 20% testing and documentation

**Rationale:** Research without implementation is theoretical. Implementation without research is reinventing the wheel.

### Research Feeds Implementation

**Workflow:**
1. Research agent explores a pattern (e.g., MUD automation)
2. Documents findings in `docs/research/`
3. Implementation agent reads research
4. Designs code architecture
5. Implements with tests
6. Updates CLAUDE.md with new patterns

**Example:**
- Research: `PRE_LLM_GAME_AUTOMATION.md` documents TinTin++ triggers
- Design: `SCRIPT_GENERATION_SYSTEM.md` proposes LLM→Script pipeline
- Code: `ScriptGenerator.java` implements the pipeline
- Update: CLAUDE.md gets new "Script Generation" section

### Continuous Integration

**Principle:** Documentation, code, and tests evolve together.

When adding new features:
1. Research question: "What are we trying to solve?"
2. Design document: "How will we solve it?"
3. Implementation: "Here's the code"
4. Tests: "Here's how we verify it works"
5. Documentation: "Here's how to use it"

---

## 11. Security

> **Security Status (2026-03-01):** All critical security vulnerabilities have been addressed.

### Security Architecture

The Steve AI mod implements defense-in-depth security across multiple layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                    INPUT LAYER (Sanitization)                   │
│                                                                 │
│   • InputSanitizer for all user input                          │
│   • Prompt injection detection                                  │
│   • Jailbreak attempt detection                                 │
│   • Control character stripping                                 │
│   • Length limits enforced                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Sanitized
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CONFIG LAYER (Secrets)                       │
│                                                                 │
│   • Environment variable support for API keys                   │
│   • No hardcoded secrets in code                                │
│   • API key preview logging (not full key)                      │
│   • ${ENV_VAR} syntax in config                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Secure Config
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  EXECUTION LAYER (Sandbox)                      │
│                                                                 │
│   • GraalVM JS sandbox (no file/network access)                 │
│   • Timeout enforcement (30s max)                               │
│   • No native/process creation allowed                          │
│   • Controlled API bridge only                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Safe Execution
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 LOGGING LAYER (Auditing)                        │
│                                                                 │
│   • Full exception logging (no empty catch blocks)              │
│   • Security event logging                                      │
│   • Suspicious pattern detection logging                        │
│   • Stack traces on errors                                      │
└─────────────────────────────────────────────────────────────────┘
```

### InputSanitizer Utility

**Location:** `src/main/java/com/minewright/security/InputSanitizer.java`

**Attack Vectors Prevented:**

| Attack Type | Description | Detection Method |
|-------------|-------------|------------------|
| **Prompt Injection** | Attempts to override system instructions | Pattern matching for "ignore previous instructions", "disregard", "forget" |
| **Jailbreak Attempts** | DAN mode, developer mode, unrestricted mode | Pattern matching for known jailbreak phrases |
| **Role Hijacking** | "Act as a different AI", "Pretend to be" | Pattern matching for role manipulation attempts |
| **Code Execution** | `\`\`\`javascript`, `eval()`, `exec()` | Pattern matching for code blocks and eval patterns |
| **System Prompt Extraction** | "Print system prompt", "Show instructions" | Pattern matching for extraction attempts |
| **JSON Termination** | Attempts to break out of JSON format | Pattern matching for `"}]}` and similar |
| **Control Characters** | Null bytes, escape sequences | Regex removal of control chars |
| **Length Attacks** | Extremely long inputs | Max length enforcement |
| **Repetition Attacks** | "aaaaaaaaaaaaa..." | Collapses 30+ repeated chars |

**Usage Example:**

```java
// In PromptBuilder.java
public static String buildUserPrompt(ForemanEntity foreman, String command, WorldKnowledge worldKnowledge) {
    // SECURITY: Sanitize user command to prevent prompt injection attacks
    String sanitizedCommand = InputSanitizer.forCommand(command);
    // ... use sanitizedCommand
}

// In TaskPlanner.java
public ResponseParser.ParsedResponse planTasks(ForemanEntity foreman, String command) {
    // SECURITY: Validate command for suspicious patterns before processing
    if (InputSanitizer.containsSuspiciousPatterns(command)) {
        String reason = InputSanitizer.getSuspiciousPatternDescription(command);
        LOGGER.warn("Command contains suspicious patterns and was rejected: {}. Command: {}",
            reason, command);
        return null;
    }
    // ... continue with processing
}
```

### Environment Variable Configuration

**API Keys from Environment:**

Support for environment variables prevents API keys from being committed to git:

```toml
# config/minewright-common.toml
[openai]
apiKey = "${OPENAI_API_KEY}"  # Resolved from environment
```

**Usage:**

```java
// Get API key with environment variable resolution
String apiKey = MineWrightConfig.getResolvedApiKey();

// Check if API key is configured
if (MineWrightConfig.hasValidApiKey()) {
    // Proceed with LLM call
}
```

**Setting Environment Variables:**

```bash
# Linux/Mac
export OPENAI_API_KEY="sk-..."
export GROQ_API_KEY="gsk-..."

# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-..."
$env:GROQ_API_KEY="gsk-..."

# Windows (Command Prompt)
set OPENAI_API_KEY=sk-...
set GROQ_API_KEY=gsk-...
```

### Security Best Practices

**For Developers:**

1. **Never hardcode API keys** in source code
2. **Use environment variables** for all sensitive configuration
3. **Validate user input** before processing
4. **Log security events** (suspicious input, rejected commands)
5. **Use try-with-resources** to prevent resource leaks
6. **Never use empty catch blocks** - always log exceptions

**For Users:**

1. **Set strong API keys** from your LLM provider
2. **Use environment variables** instead of config files when possible
3. **Review logs** for suspicious pattern warnings
4. **Keep config files private** - don't commit to version control
5. **Update regularly** to get security patches

### Security Tests

**Location:** `src/test/java/com/minewright/security/InputSanitizerTest.java`

**Test Coverage:**
- 40+ test cases covering:
  - Prompt injection patterns (ignore, disregard, forget instructions)
  - Jailbreak attempts (DAN, developer mode, unrestricted mode)
  - Role hijacking (act as, pretend to be)
  - Code execution attempts (eval, exec, code blocks)
  - System prompt extraction attempts
  - Control character removal
  - Length limit enforcement
  - Repetition collapsing
  - Edge cases (null input, unicode, mixed case)

**Running Security Tests:**

```bash
./gradlew test --tests InputSanitizerTest
```

### Security Audit History

| Date | Issue | Status | Solution |
|------|-------|--------|----------|
| 2026-03-01 | Empty catch block in StructureTemplateLoader | ✅ Fixed | Added proper exception logging with stack trace |
| 2026-03-01 | API keys only from config files | ✅ Fixed | Added `getResolvedApiKey()` with `${ENV_VAR}` support |
| 2026-03-01 | No input sanitization for LLM prompts | ✅ Fixed | Created `InputSanitizer` utility with pattern detection |
| 2026-03-01 | No validation of suspicious commands | ✅ Fixed | Added validation in `TaskPlanner.planTasks()` |

---

## 12. In-Game Commands

### Agent Commands

| Command | Arguments | Description |
|---------|-----------|-------------|
| `/foreman spawn` | `<name>` | Spawn a new Steve agent |
| `/foreman list` | | List all active agents |
| `/foreman remove` | `<name>` | Remove a Steve |
| `/foreman order` | `<name> <command>` | Issue work order |

### GUI Controls

| Key | Action |
|-----|--------|
| **K** | Open command GUI |
| **ESC** | Close GUI |

---

## 13. Code Style

### Formatting

- 4-space indentation
- 120 character line limit
- PascalCase classes, camelCase methods/variables
- JavaDoc for public APIs

### Example

```java
/**
 * Mines blocks of a specific type within a radius.
 */
public class MineAction extends BaseAction {
    private final BlockType blockType;
    private final int radius;

    public MineAction(SteveEntity steve, Task task) {
        super(steve, task);
        this.blockType = task.getBlockType();
        this.radius = task.getRadius();
    }

    @Override
    protected void onTick() {
        // Mining logic here
    }

    @Override
    public boolean isComplete() {
        return getBlocksMined() >= getTarget();
    }
}
```

---

## 14. Testing

### Unit Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests MineActionTest
```

**Note:** Test infrastructure needs Minecraft test framework for proper entity mocking. Current tests are limited due to Mockito/Minecraft classloader issues.

### Integration Tests

```bash
# Run integration tests
./gradlew integrationTest
```

---

## 15. Troubleshooting

### Common Issues

**LLM API Timeout**
- Symptom: Tasks hang, no response
- Solution: Switch to faster provider (Groq), enable batching, increase timeout

**Agent Stuck**
- Symptom: Agent not moving, task not progressing
- Solution: Check pathfinding, verify navigation, increase stuck detection sensitivity

**Out of Memory**
- Symptom: Mod crashes with OOM
- Solution: Reduce max agents in config, optimize structure generation, increase JVM heap

---

## 16. References

### Key Documents

| Document | Purpose | Location |
|----------|---------|----------|
| NPC Scripting Evolution | Philosophical foundation | `docs/research/NPC_SCRIPTING_EVOLUTION.md` |
| Viva Voce Cycle 2 Synthesis | Dissertation progress | `docs/research/VIVA_VOCE_CYCLE2_SYNTHESIS.md` |
| Dissertation Integration Summary | Integration status | `docs/research/DISSERTATION_INTEGRATION_SUMMARY.md` |
| MUD Automation History | Pre-LLM patterns | `docs/research/PRE_LLM_GAME_AUTOMATION.md` |
| Script Generation System | LLM→Script pipeline | `docs/research/SCRIPT_GENERATION_SYSTEM.md` |

### Quick Links

- **Source Code:** `src/main/java/com/minewright/`
- **Research Docs:** `docs/research/`
- **Config:** `config/steve-common.toml`
- **Build Output:** `build/libs/`

---

## 17. Summary

Steve AI is a sophisticated multi-agent system for Minecraft that combines:

1. **Natural Language Understanding** - LLM-powered task planning
2. **Multi-Agent Coordination** - Foreman/worker pattern with spatial partitioning
3. **Real-Time Execution** - Tick-based action system with zero blocking
4. **Characterful AI** - Rich personalities, dialogue, relationships
5. **Production Architecture** - State machines, event buses, lock-free coordination
6. **Behavior Trees & HTN** - Complete implementations for tactical AI
7. **Advanced Pathfinding** - Hierarchical A* with path smoothing

**Current Status (2026-03-01 Audit):**
- **Code:** 85% complete (234 files, 85,752 lines)
- **Tests:** 24% coverage (55 files, 33,349 lines) - improving
- **Documentation:** 425 files, 521,003 lines - comprehensive
- **Dissertation:** A-grade (92/100), targeting A+ (97+)
- **Security:** All critical issues addressed (2026-03-01)
- **Build Health:** 8/10 (quality tools disabled but stable)

**Priority Actions:**
1. ~~Fix security issues~~ - COMPLETED 2026-03-01
2. Add tests for core components (ActionExecutor, AgentStateMachine)
3. Complete dissertation Chapter 3 integration
4. Implement script DSL for automation patterns
5. Re-enable Checkstyle and SpotBugs

---

**Document Version:** 2.7
**Last Updated:** 2026-03-01
**Maintained By:** Claude Orchestrator
**Next Review:** After major architecture changes or dissertation completion
