# AI Agent Onboarding Guide

**Welcome to the Steve AI Project!**

This document provides everything you need to know to be productive immediately.

---

## Quick Start (30 Seconds)

```bash
# 1. Verify build compiles
./gradlew compileJava compileTestJava

# 2. Run tests (may fail on Windows due to file locks - see KNOWN_ISSUES.md)
./gradlew test

# 3. Build the mod
./gradlew build
```

**If compilation passes, you're ready to work!**

---

## Project Identity

| Aspect | Value |
|--------|-------|
| **Name** | Steve AI - "Cursor for Minecraft" |
| **Type** | Minecraft Forge Mod (1.20.1) |
| **Language** | Java 17 |
| **Status** | 85% Production Ready |
| **Mission** | Autonomous AI agents that play Minecraft with you |

## Core Philosophy: "One Abstraction Away"

```
┌─────────────────────────────────────┐
│     BRAIN LAYER (Strategic)         │  ← LLM Plans (30-60s updates)
│     GLM-5, GPT-4, Claude            │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│    SCRIPT LAYER (Operational)       │  ← Behavior Trees, FSMs (20 TPS)
│    BT, HTN, Utility AI              │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│    PHYSICAL LAYER (Actions)         │  ← Minecraft API (20 TPS)
│    Block, Movement, Inventory       │
└─────────────────────────────────────┘
```

**Key Insight**: LLMs plan, traditional game AI executes. This = fast, cheap, characterful agents.

---

## Project Structure

```
steve/
├── src/main/java/com/minewright/
│   ├── llm/                    # LLM integration (GLM, OpenAI, Groq, Gemini)
│   │   ├── GLMCascadeRouter.java   # Intelligent model selection
│   │   ├── LocalLLMClient.java     # Free local inference (Ollama)
│   │   ├── TaskPlanner.java        # Main planning orchestration
│   │   ├── async/                  # Async LLM clients
│   │   ├── batch/                  # Batching for rate limits
│   │   ├── cache/                  # Response caching (40-60% hit rate)
│   │   └── cascade/                # Tier-based routing
│   │
│   ├── action/                 # Task execution system
│   │   ├── ActionExecutor.java     # Core execution engine
│   │   ├── actions/                # Individual actions (Mine, Build, etc.)
│   │   └── Task.java               # Task data structure
│   │
│   ├── behavior/               # Behavior trees & process arbitration
│   │   ├── ProcessManager.java     # Priority-based behavior selection
│   │   ├── composite/              # Sequence, Selector, Parallel
│   │   ├── decorator/              # Inverter, Repeater, Timer
│   │   └── processes/              # Survival, Task, Idle, Follow
│   │
│   ├── htn/                    # Hierarchical Task Network planner
│   │   ├── HTNPlanner.java         # Method-based planning
│   │   ├── HTNWorldState.java      # World state representation
│   │   └── Domain.java             # Task domain definition
│   │
│   ├── memory/                 # Memory systems
│   │   ├── CompanionMemory.java    # Conversation tracking
│   │   ├── embedding/              # Vector embeddings
│   │   └── vector/                 # Semantic search
│   │
│   ├── pathfinding/            # Navigation
│   │   ├── AStarPathfinder.java    # A* with node pooling
│   │   ├── HierarchicalPathfinder.java  # Long-distance routing
│   │   └── MovementValidator.java  # Safe traversal checks
│   │
│   ├── skill/                  # Voyager-style skill library
│   │   ├── SkillLibrary.java       # Vector-indexed skills
│   │   ├── SkillAutoGenerator.java # LLM skill generation
│   │   └── PatternExtractor.java   # Learn from execution
│   │
│   ├── humanization/           # Human-like behavior
│   │   ├── MistakeSimulator.java   # Probabilistic mistakes
│   │   ├── IdleBehaviorController.java  # Natural idle actions
│   │   └── SessionManager.java     # Fatigue simulation
│   │
│   ├── recovery/               # Stuck detection & recovery
│   │   ├── StuckDetector.java      # Detect stuck conditions
│   │   ├── RecoveryManager.java    # Coordinate recovery
│   │   └── strategies/             # Repath, Teleport, Abort
│   │
│   ├── goal/                   # Navigation goals (Baritone-style)
│   │   ├── NavigationGoal.java     # Goal interface
│   │   ├── CompositeNavigationGoal.java  # ANY/ALL composition
│   │   └── Goals.java              # Factory methods
│   │
│   ├── profile/                # Task profiles (Honorbuddy-style)
│   │   ├── TaskProfile.java        # Declarative task sequences
│   │   └── ProfileExecutor.java    # Execute profiles
│   │
│   ├── rules/                  # Item rules engine
│   │   ├── ItemRule.java           # Declarative filtering
│   │   └── RuleEvaluator.java      # Evaluate rules
│   │
│   ├── security/               # Input sanitization
│   │   └── InputSanitizer.java     # Prompt injection prevention
│   │
│   └── execution/              # State machine & interceptors
│       ├── AgentStateMachine.java  # IDLE→PLANNING→EXECUTING→...
│       └── InterceptorChain.java   # Logging, Metrics, Events
│
├── src/test/java/com/minewright/   # 91 test files
│
├── docs/research/              # 60+ research documents
│   ├── MINECRAFT_AI_SOTA_2024_2025.md  # State-of-the-art analysis
│   ├── VOYAGER_SKILL_SYSTEM.md         # Skill library patterns
│   └── BARITONE_MINEFLAYER_ANALYSIS.md # Goal composition
│
├── config/                     # Configuration
│   ├── steve-common.toml       # Main config file
│   ├── checkstyle/             # Code style rules
│   └── spotbugs/               # Bug detection rules
│
├── CLAUDE.md                   # Main project guide (READ THIS FIRST)
├── AGENT_ONBOARDING.md         # This file
├── ARCHITECTURE_QUICK_REFERENCE.md  # Architecture cheat sheet
├── CURRENT_PRIORITIES.md       # What to work on next
└── KNOWN_ISSUES.md             # Known issues & workarounds
```

---

## Key Classes to Understand

### Tier 1: Must Understand (Core System)

| Class | Purpose | Location |
|-------|---------|----------|
| `TaskPlanner` | LLM orchestration, planning | `llm/TaskPlanner.java` |
| `GLMCascadeRouter` | Intelligent model selection | `llm/GLMCascadeRouter.java` |
| `ActionExecutor` | Task execution engine | `action/ActionExecutor.java` |
| `AgentStateMachine` | State transitions | `execution/AgentStateMachine.java` |

### Tier 2: Important (AI Systems)

| Class | Purpose | Location |
|-------|---------|----------|
| `HTNPlanner` | Hierarchical task planning | `htn/HTNPlanner.java` |
| `ProcessManager` | Behavior arbitration | `behavior/ProcessManager.java` |
| `SkillLibrary` | Vector-indexed skills | `skill/SkillLibrary.java` |
| `AStarPathfinder` | Navigation | `pathfinding/AStarPathfinder.java` |

### Tier 3: Useful (Utilities)

| Class | Purpose | Location |
|-------|---------|----------|
| `InputSanitizer` | Security | `security/InputSanitizer.java` |
| `StuckDetector` | Recovery | `recovery/StuckDetector.java` |
| `HumanizationUtils` | Natural behavior | `humanization/HumanizationUtils.java` |

---

## Build Commands

```bash
# Compilation
./gradlew compileJava              # Compile main source
./gradlew compileTestJava          # Compile tests

# Testing
./gradlew test                     # Run all tests
./gradlew test --tests MineActionTest  # Run specific test

# Building
./gradlew build                    # Build the mod JAR
./gradlew shadowJar                # Build distribution JAR (with dependencies)

# Quality
./gradlew checkstyleMain           # Check code style
./gradlew spotbugsMain             # Find bugs

# Running
./gradlew runClient                # Launch Minecraft client
./gradlew runServer                # Launch Minecraft server
```

---

## Configuration

**Main config file**: `config/steve-common.toml`

```toml
[llm]
provider = "openai"  # openai, groq, gemini

[openai]
apiKey = "${OPENAI_API_KEY}"  # Use env var for security
model = "gpt-4"

[groq]
apiKey = "${GROQ_API_KEY}"
model = "llama3-70b-8192"
```

**Environment Variables** (recommended for API keys):
```bash
# Linux/Mac
export OPENAI_API_KEY="sk-..."
export GROQ_API_KEY="gsk_..."

# Windows PowerShell
$env:OPENAI_API_KEY="sk-..."
```

---

## Current Implementation Status

| System | Status | Completion |
|--------|--------|------------|
| LLM Integration | ✅ Complete | 100% |
| Action Execution | ✅ Complete | 100% |
| Behavior Trees | ✅ Complete | 100% |
| HTN Planner | ✅ Complete | 100% |
| Pathfinding | ✅ Complete | 95% |
| Humanization | ✅ Complete | 100% |
| Recovery System | ✅ Complete | 100% |
| Profile System | ✅ Complete | 100% |
| Skill Library | 🔄 Foundation | 70% |
| Skill Learning | 🔄 Partial | 40% |
| Multi-Agent Coord | 🔄 Partial | 50% |

---

## Code Style Guidelines

1. **4-space indentation**
2. **120 character line limit**
3. **PascalCase classes, camelCase methods**
4. **JavaDoc for public APIs**
5. **Never use empty catch blocks** - always log exceptions
6. **Use environment variables for secrets**
7. **Sanitize all user input before LLM calls**

---

## Testing Guidelines

1. **Tests are in `src/test/java/com/minewright/`**
2. **Use JUnit 5** (`@Test`, `@BeforeEach`, etc.)
3. **Use Mockito for mocking**
4. **Current coverage: ~40%** (aiming for 60%)
5. **Critical untested classes**: ActionExecutor, AgentStateMachine

---

## Security Notes

1. **InputSanitizer** validates all user commands before LLM calls
2. **Environment variables** for API keys (never hardcode)
3. **GraalVM sandbox** for script execution (no file/network access)
4. **Full exception logging** (no silent failures)

---

## Where to Start Working

**See `CURRENT_PRIORITIES.md` for the current work priorities.**

Quick recommendations:
1. Add tests for `ActionExecutor` and `AgentStateMachine`
2. Implement skill composition system
3. Complete multi-agent coordination
4. Re-enable Checkstyle/SpotBugs quality checks

---

## Getting Help

1. **Read `CLAUDE.md`** - Comprehensive project guide
2. **Check `docs/research/`** - 60+ research documents
3. **Review `KNOWN_ISSUES.md`** - Common problems and solutions
4. **Look at existing tests** - `src/test/java/com/minewright/`

---

## Session State (for context continuity)

**Last Updated**: 2026-03-03
**Branch**: clean-main
**Last Commit**: Wave 29 (cffd41f)
**Build Status**: ✅ Compiles
**Test Status**: ⚠️ File lock issue (Windows)

**Active Work**:
- Onboarding documentation (this file)
- LLM infrastructure improvements completed

**Next Agent Should**:
1. Read this file and CLAUDE.md
2. Check CURRENT_PRIORITIES.md
3. Continue with highest priority task
4. Commit and push when done

---

*Good luck, and may your agents be intelligent!*
