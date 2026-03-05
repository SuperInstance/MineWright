# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MineWright** - "Cursor for Minecraft": A multi-agent AI system for Minecraft that combines LLM-driven planning with real-time game execution. Users issue natural language commands, and AI companions (called Foremen) coordinate to complete tasks.

**Key Concept**: "One Abstraction Away" - LLMs plan and coordinate (Brain Layer), while traditional game AI executes (Script/Physical Layer). This enables 60 FPS execution without blocking on LLM calls.

**Current Status (2026-03-05):**
- **Code**: 90% complete (237 files, 86,500+ lines)
- **Tests**: ~58% coverage (143 test files, 48,000+ lines)
- **Security**: All critical issues addressed
- **Recent**: Waves 55-57 complete - Documentation, test cleanup, coverage expansion

## Quick Start

```bash
# Build the mod
./gradlew build

# Run client for testing
./gradlew runClient

# Run tests
./gradlew test

# Build distribution JAR
./gradlew shadowJar reobfShadowJar
```

**Configuration:** `config/minewright-common.toml`

**Build Output:**
- Development: `build/libs/minewright-1.0.0.jar`
- Distribution: `build/libs/minewright-1.0.0-all.jar`

## High-Level Architecture

```
BRAIN LAYER (Strategic)                 SCRIPT LAYER (Operational)         PHYSICAL LAYER (Actions)
LLM Agents                              Behavior Trees, FSMs, Scripts       Minecraft API
- Planning, strategy, logistics         - Pathfinding, mining, building     - Block interactions
- Conversations, dialogue               - Combat AI, resource gathering     - Entity tracking
- Creating and refining scripts         - Reactive behaviors                - Direct game API calls
Update: Every 30-60 seconds             Update: Every tick (20 TPS)         Update: Every tick (20 TPS)
```

## Key Packages

| Package | Purpose | Key Classes |
|---------|---------|-------------|
| `action/` | Task execution system | ActionExecutor, BaseAction, Task |
| `behavior/` | Behavior tree runtime | BTNode, ProcessManager |
| `llm/` | LLM integration | PromptBuilder, AsyncLLMClient, CascadeRouter |
| `coordination/` | Multi-agent coordination | ContractNetManager, TaskBid |
| `memory/` | Persistence and semantic search | MemoryStore, InMemoryVectorStore |
| `pathfinding/` | A* and hierarchical pathfinding | AStarPathfinder, HierarchicalPathfinder |
| `recovery/` | Stuck detection and recovery | StuckDetector, RecoveryManager |
| `dialogue/` | Proactive dialogue system | ProactiveDialogueManager, DialogueTriggerChecker |
| `config/` | Configuration management | ConfigManager, MineWrightConfig |

**See:** `docs/PACKAGE_REFERENCE.md` for complete package reference

## Key Patterns

### Plugin Architecture

```java
// Register action via plugin
registry.register("mine", (foreman, task, ctx) -> new MineAction(foreman, task));
```

### Tick-Based Execution

```java
public class MineAction extends BaseAction {
    @Override
    protected void onTick() {
        // Called once per game tick (20 TPS)
        // Track internal state and return when complete
    }
}
```

### Async LLM Calls

```java
llmClient.planAsync(command)
    .thenAccept(tasks -> {
        // Handle result when ready
        actionExecutor.executeTasks(tasks);
    });
```

### Interceptor Chain

```
LoggingInterceptor → MetricsInterceptor → EventPublishingInterceptor → Action
```

### State Machine

```
IDLE → PLANNING → EXECUTING → COMPLETED → IDLE
                  ↓
                FAILED → IDLE
```

### Blackboard Pattern

```java
// Post observations
Blackboard.getInstance().post(KnowledgeArea.WORLD_STATE, entry);

// Query for information
Optional<BlockState> block = Blackboard.getInstance().query(
    KnowledgeArea.WORLD_STATE, "block_100_64_200");

// Subscribe to updates
Blackboard.getInstance().subscribe(KnowledgeArea.THREATS, subscriber);
```

**See:** `docs/PATTERNS_GUIDE.md` for complete patterns reference

## Implementation Notes

**Thread Safety:**
- Use `ConcurrentHashMap`, `AtomicInteger`, `AtomicLong` (volatile ++ is NOT atomic!)
- LLM calls are async via `CompletableFuture`
- Never block the game thread - use tick-based polling

**Error Handling:**
- All exceptions extend `MineWrightException`
- Actions use `ActionResult` for success/failure reporting
- Use `InputSanitizer` for all user input

**Code Quality:**
- Test coverage: ~58% (target: 60%)
- Never use empty catch blocks - always log exceptions
- Recent refactoring eliminated 9 god classes

**Security:**
- Sanitize all user input before LLM processing
- Use environment variables for API keys: `${OPENAI_API_KEY}`

## In-Game Commands

| Command | Description |
|---------|-------------|
| `/minewright spawn <name>` | Spawn a new Foreman companion |
| `/minewright list` | List all active Foremen |
| `/minewright remove <name>` | Remove a Foreman |
| `/minewright order <name> <command>` | Issue work order |
| Press **K** | Open command GUI |

## Common Issues

- **LLM API Timeout:** Switch to faster provider (Groq), enable batching
- **Agent Stuck:** Check pathfinding, verify navigation, increase stuck detection sensitivity
- **Out of Memory:** Reduce max agents in config, increase JVM heap

## Key Dependencies

- **Minecraft Forge 1.20.1** - Mod framework
- **GraalVM JS 24.1.2** - Code execution (relocated)
- **Resilience4j 2.3.0** - Circuit breaker, retry, rate limiting
- **Caffeine 3.1.8** - High-performance caching
- **JUnit 5.11.4** - Testing

## References

**Core Documentation:**
- `docs/ARCHITECTURE_OVERVIEW.md` - Complete system architecture
- `docs/PACKAGE_REFERENCE.md` - All packages and classes
- `docs/PATTERNS_GUIDE.md` - Architectural and design patterns
- `docs/BUILD_GUIDE.md` - Build commands and configuration
- `docs/REFACTORING_HISTORY.md` - Recent refactoring work

**Research & Audits:**
- `docs/research/` - AI and game automation research
- `docs/audits/` - Code audit findings and fixes
- `docs/FUTURE_ROADMAP.md` - Planned improvements

## Summary

MineWright combines LLM planning with real-time game execution for autonomous Minecraft companions called Foremen.

**Priority Actions:**
1. Add tests for core components (ActionExecutor, AgentStateMachine)
2. Complete dissertation Chapter 3 integration
3. Implement script DSL for automation patterns

---

**Version:** 4.1 (Rebranded) | **Updated:** 2026-03-05
