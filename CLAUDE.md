# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Steve AI (MineWright)** - "Cursor for Minecraft": A multi-agent AI system for Minecraft that combines LLM-driven planning with real-time game execution. Users issue natural language commands, and AI agents coordinate to complete tasks.

**Key Concept**: "One Abstraction Away" - LLMs plan and coordinate (Brain Layer), while traditional game AI executes (Script/Physical Layer). This enables 60 FPS execution without blocking on LLM calls.

**Recent Updates (2026-03-03 - Wave 42):**
- ✅ **ScriptParser God Class Refactoring**: Split 1,029-line class into 6 focused classes (91% reduction)
  - ScriptLexer (410 lines), YAMLFormatParser (800 lines), BraceFormatParser (458 lines)
  - ScriptASTBuilder (301 lines), ScriptValidator (454 lines), ScriptParseException (30 lines)
- ✅ **Critical SpotBugs Fixes**: Fixed 5 HIGH severity thread-safety issues
  - Blackboard counters: Changed volatile long to AtomicLong (3 fixes)
  - ConfigManager: Added double-checked locking for singleton
  - VoiceManager: Added double-checked locking for singleton

## Build Commands

```bash
# Build the mod (development JAR)
./gradlew build

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer

# Run tests
./gradlew test

# Run a single test
./gradlew test --tests MyTest

# Build distribution JAR with dependencies (for distribution)
./gradlew shadowJar

# Obfuscate distribution JAR
./gradlew shadowJar reobfShadowJar

# Generate coverage report
./gradlew jacocoCoverageReport

# Check code quality
./gradlew checkstyle
./gradlew spotbugs

# Clean build artifacts
./gradlew clean
```

**Build Output:**
- Development JAR: `build/libs/minewright-1.0.0.jar`
- Distribution JAR: `build/libs/minewright-1.0.0-all.jar` (includes dependencies)

**Configuration:** `config/minewright-common.toml`

## High-Level Architecture

### Three-Layer Design

```
BRAIN LAYER (Strategic)                 SCRIPT LAYER (Operational)         PHYSICAL LAYER (Actions)
LLM Agents                              Behavior Trees, FSMs, Scripts       Minecraft API
- Planning, strategy, logistics         - Pathfinding, mining, building     - Block interactions
- Conversations, dialogue               - Combat AI, resource gathering     - Entity tracking
- Creating and refining scripts         - Reactive behaviors                - Direct game API calls
Update: Every 30-60 seconds             Update: Every tick (20 TPS)         Update: Every tick (20 TPS)
```

### Package Structure

| Package | Purpose | Key Classes |
|---------|---------|-------------|
| `action/` | Task execution system | ActionExecutor, BaseAction, Task, ActionResult |
| `behavior/` | Behavior tree runtime & process arbitration | BehaviorProcess, ProcessManager, BTNode |
| `htn/` | Hierarchical Task Network planner | HTNPlanner, Method, Domain, HTNWorldState |
| `blackboard/` | Shared knowledge system for agent coordination | Blackboard, BlackboardEntry, KnowledgeArea |
| `llm/` | LLM integration with async/batch/cache/cascade | PromptBuilder, ResponseParser, AsyncLLMClient |
| `llm/cascade/` | Tier-based model selection for cost optimization | CascadeRouter, TaskComplexity, LLMTier |
| `execution/` | State machine with interceptors and event bus | AgentStateMachine, InterceptorChain |
| `coordination/` | Contract Net Protocol for multi-agent bidding | TaskBid, TaskAnnouncement, AgentRole |
| `pathfinding/` | A* with hierarchical planning and path smoothing | HierarchicalPathfinder, MovementValidator |
| `memory/` | Persistence and semantic vector search | CompanionMemory, ConversationManager, InMemoryVectorStore |
| `skill/` | Voyager-style skill library with composition | Skill, SkillComposer, ComposedSkill, CompositionStep |
| `profile/` | Task profile system (Honorbuddy-inspired) | TaskProfile, ProfileExecutor, ProfileRegistry |
| `recovery/` | Stuck detection and recovery strategies | StuckDetector, RecoveryManager, StuckType |
| `humanization/` | Human-like behavior utilities | HumanizationUtils, MistakeSimulator, IdleBehaviorController |
| `goal/` | Navigation goal composition (Baritone-inspired) | NavigationGoal, CompositeNavigationGoal, GetToBlockGoal |
| `rules/` | Declarative item rules engine | ItemRule, RuleEvaluator, ItemRuleRegistry |
| `script/` | Script DSL parsing and execution | Script, ScriptParser, ScriptGenerator, ScriptLexer, YAMLFormatParser, BraceFormatParser, ScriptASTBuilder |
| `plugin/` | Plugin architecture with SPI | ActionRegistry, ActionPlugin, PluginManager |
| `personality/` | AI personality system | ForemanArchetypeConfig, PersonalityTraits |
| `voice/` | TTS/STT integration | VoiceSystem, SpeechToText, TextToSpeech, VoiceManager |
| `config/` | Configuration management | ConfigManager, MineWrightConfig, ConfigChangeListener, ConfigVersion |

## Key Patterns

### Plugin Architecture

Actions are registered via `ActionRegistry` using factory functions:

```java
// In CoreActionsPlugin.java
registry.register("mine", (steve, task, ctx) -> new MineAction(steve, task));
registry.register("build", (steve, task, ctx) -> new BuildAction(steve, task));
```

The `PluginManager` loads plugins via SPI (Service Provider Interface).

### Tick-Based Execution

All actions extend `BaseAction` and implement tick-based execution:

```java
public class MineAction extends BaseAction {
    @Override
    protected void onTick() {
        // Called once per game tick (20 TPS)
        // Return true when complete by setting result
    }
}
```

This prevents server freezing - actions track internal state and return `isComplete()` when done.

### Async LLM Calls

LLM planning returns `CompletableFuture` for non-blocking execution:

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

```
LoggingInterceptor -> MetricsInterceptor -> EventPublishingInterceptor -> Action
```

### State Machine

`AgentStateMachine` tracks states:

```
IDLE -> PLANNING -> EXECUTING -> COMPLETED -> IDLE
                  ↓
                FAILED -> IDLE
```

### Blackboard Pattern

`Blackboard` enables shared knowledge across agents:

```java
// Post observations
Blackboard.getInstance().post(KnowledgeArea.WORLD_STATE, entry);

// Query for information
Optional<BlockState> block = Blackboard.getInstance().query(
    KnowledgeArea.WORLD_STATE, "block_100_64_200");

// Subscribe to updates
Blackboard.getInstance().subscribe(KnowledgeArea.THREATS, subscriber);
```

### Configuration System

Configuration uses Forge Config with versioning and change notifications:

```java
// Register listener
ConfigManager.getInstance().registerListener(mySystem);

// Reload (called automatically by /reload)
ConfigManager.getInstance().reloadConfig();
```

### Security

All user input must be sanitized before LLM processing:

```java
// Sanitize user command to prevent prompt injection attacks
String sanitizedCommand = InputSanitizer.forCommand(command);
```

API keys support environment variable resolution:

```toml
[openai]
apiKey = "${OPENAI_API_KEY}"  # Resolved from environment
```

## Important Implementation Notes

### Thread Safety

- Use `ConcurrentHashMap` for shared collections
- Use `AtomicInteger` and `AtomicLong` for counters (volatile ++ is NOT atomic!)
- Use `CopyOnWriteArrayList` for subscriber lists
- LLM calls are async via `CompletableFuture`
- Never block the game thread - use tick-based polling instead
- Singletons: Use double-checked locking pattern with volatile instance variable:
  ```java
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

### Error Handling

- All exceptions extend `MineWrightException` with error codes
- Actions use `ActionResult` for success/failure reporting
- Recovery strategies are pluggable via `RecoveryStrategy` interface
- Use `InputSanitizer` for all user input to prevent prompt injection

### Code Quality

- Test coverage target: 40% (per JaCoCo configuration)
- Checkstyle and SpotBugs are configured but warnings are currently ignored
- Never use empty catch blocks - always log exceptions
- Use `TestLogger` instead of direct logging in test-facing code

### Dependencies

- **GraalVM JS 24.1.2** - Relocated to avoid conflicts
- **Resilience4j 2.3.0** - Circuit breaker, retry, rate limiting
- **Caffeine 3.1.8** - High-performance caching
- **Apache Commons Codec 1.17.1** - Utilities
- **JUnit 5.11.4** - Testing
- **Mockito 5.15.2** - Mocking

## Testing

Tests are located in `src/test/java/com/minewright/`.

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ActionExecutorTest

# Run specific test method
./gradlew test --tests "ActionExecutorTest.testExecuteAction"

# Generate coverage report
./gradlew jacocoCoverageReport
```

## In-Game Commands

| Command | Description |
|---------|-------------|
| `/foreman spawn <name>` | Spawn a new Steve agent |
| `/foreman list` | List all active agents |
| `/foreman remove <name>` | Remove a Steve |
| `/foreman order <name> <command>` | Issue work order |
| Press **K** | Open command GUI |

## Common Issues

### LLM API Timeout
- Symptom: Tasks hang, no response
- Solution: Switch to faster provider (Groq), enable batching, increase timeout

### Agent Stuck
- Symptom: Agent not moving, task not progressing
- Solution: Check pathfinding, verify navigation, increase stuck detection sensitivity

### Out of Memory
- Symptom: Mod crashes with OOM
- Solution: Reduce max agents in config, optimize structure generation, increase JVM heap

### Empty Catch Blocks
- Never use empty catch blocks - always log exceptions with stack trace
- The `InputSanitizer`, `StructureTemplateLoader` fixes show the proper pattern

## References

- **Architecture Deep Dive:** `docs/architecture/TECHNICAL_DEEP_DIVE.md`
- **Action API:** `docs/ACTION_API.md`
- **Research Documents:** `docs/research/`
- **Configuration:** `config/minewright-common.toml`
- **Build Output:** `build/libs/`
