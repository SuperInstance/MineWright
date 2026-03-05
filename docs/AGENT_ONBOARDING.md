# Agent Onboarding Guide - MineWright Project

**Version:** 1.0
**Created:** 2026-03-04
**Purpose:** Guide for AI agents (and humans) to understand and work on this codebase

---

## Welcome, Agent!

You are joining the MineWright project - a sophisticated Minecraft mod that creates autonomous AI companions. This guide will help you understand the system and contribute effectively.

### Quick Start Checklist

Before you start working, complete these steps:

- [ ] Read this entire document (15 minutes)
- [ ] Review `CLAUDE.md` for project vision and conventions (10 minutes)
- [ ] Check `FUTURE_ROADMAP.md` for current priorities (5 minutes)
- [ ] Run `./gradlew build` to verify your environment (2 minutes)
- [ ] Pick a task from the roadmap or create your own

---

## 1. System Overview

### What is MineWright?

MineWright is "Cursor for Minecraft" - AI agents that play Minecraft with you. Users type natural language commands, and AI-controlled Foreman entities execute them through LLM-powered planning.

### Core Philosophy: "One Abstraction Away"

The system uses three layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│   • Planning, strategy, conversations                           │
│   • Token Usage: LOW (batched, infrequent)                      │
│   • Update: Every 30-60 seconds or on events                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│   • Behavior trees, FSMs, scripts                               │
│   • Token Usage: ZERO (runs locally)                            │
│   • Update: Every tick (20 TPS)                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Executes via
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
│   • Block interactions, movement, inventory                     │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture Maturity

| Component | Status | Lines | Test Coverage |
|-----------|--------|-------|---------------|
| Action Execution | ✅ Complete | 915 | 50%+ |
| LLM Integration | ✅ Complete | 775 | 26% |
| Memory System | ✅ Complete | 400 | 20% |
| Multi-Agent | ✅ Complete | 789 | 15% |
| Script DSL | 🔄 Partial | 695 | 30% |

---

## 2. Critical Files to Understand

### Entry Points (Read These First)

1. **`src/main/java/com/minewright/MineWrightMod.java`**
   - Mod initialization and entry point
   - Service bootstrap
   - Entity registration

2. **`src/main/java/com/minewright/entity/ForemanEntity.java`** (~700 lines)
   - Main AI entity
   - Tick-based updates (20 TPS)
   - Delegates to: EntityState, ActionCoordinator, CommunicationHandler

3. **`src/main/java/com/minewright/action/ActionExecutor.java`** (~915 lines)
   - Non-blocking execution engine
   - Plugin architecture
   - State machine: IDLE → PLANNING → EXECUTING → COMPLETED

### Key Subsystems

```
src/main/java/com/minewright/
├── action/          # Task execution (24 files)
├── behavior/        # Behavior trees (17 files)
├── llm/             # LLM integration (50 files)
├── memory/          # Memory systems (12 files)
├── orchestration/   # Multi-agent (6 files)
├── script/          # DSL system (13 files)
├── skill/           # Skill library (25 files)
└── pathfinding/     # A* navigation (8 files)
```

---

## 3. Before You Write Code: Audit First!

### The Audit Protocol

**CRITICAL:** Before making any changes, you MUST audit the relevant code.

#### Step 1: Locate Related Code

```bash
# Find files related to a topic
grep -r "keyword" src/main/java --include="*.java" -l

# Find large files that might need refactoring
find src/main/java -name "*.java" -exec wc -l {} \; | sort -rn | head -20

# Check test coverage for a package
ls src/test/java/com/minewright/PACKAGE_NAME/ 2>/dev/null | wc -l
```

#### Step 2: Read and Understand

For each file you'll modify:
1. Read the entire file
2. Identify its responsibilities
3. Find its dependencies
4. Check for existing tests
5. Look for TODO/FIXME comments

#### Step 3: Document Your Findings

Create or update the audit document:
```markdown
# Audit: [Feature Name]
**Date:** YYYY-MM-DD
**Files Reviewed:** [list]
**Current State:** [description]
**Issues Found:** [list]
**Recommendations:** [list]
```

### Questions to Ask During Audit

1. **Complexity:** Is this file too large (>500 lines)?
2. **Responsibility:** Does it do one thing well?
3. **Dependencies:** Are they appropriate or excessive?
4. **Testing:** Are there tests? Do they cover edge cases?
5. **Thread Safety:** Are concurrent operations handled correctly?
6. **Error Handling:** Are exceptions handled properly?
7. **Documentation:** Is the code self-documenting?

---

## 4. Code Style and Conventions

### Formatting

```java
// 4-space indentation
// 120 character line limit
// PascalCase classes, camelCase methods/variables

/**
 * Brief description of what this class does.
 *
 * <p>More detailed explanation if needed.</p>
 */
public class ExampleClass {
    private final Dependency dependency;

    public void doSomething(Parameter param) {
        // Implementation
    }
}
```

### Patterns We Use

1. **Plugin Architecture** - Actions registered via `ActionRegistry`
2. **State Machine** - Explicit transitions with validation
3. **Interceptor Chain** - Cross-cutting concerns (logging, metrics)
4. **Facade Pattern** - `CompanionMemory` hides complexity
5. **Delegation Pattern** - Large classes delegate to specialists

### Thread Safety Rules

- **NEVER** block the game thread (20 TPS requirement)
- **ALWAYS** use `CompletableFuture` for async operations
- **ALWAYS** use `ConcurrentHashMap` for shared state
- **ALWAYS** use `volatile` or `AtomicX` for cross-thread visibility

---

## 5. How to Contribute

### Adding a New Feature

1. **Audit First** - Understand existing code
2. **Design** - Document your approach
3. **Implement** - Write clean, tested code
4. **Test** - Unit tests + integration tests
5. **Document** - Update CLAUDE.md if needed
6. **Commit** - Clear, descriptive commit message

### Refactoring Guidelines

When refactoring large classes (>500 lines):

1. **Identify Responsibilities** - List what the class does
2. **Extract Components** - One responsibility per new class
3. **Use Delegation** - Original class delegates to components
4. **Maintain API** - Keep public interface unchanged
5. **Add Tests** - Ensure refactoring doesn't break behavior

### Example: Refactoring a God Class

```java
// BEFORE: God class with 800+ lines
public class BigClass {
    public void doA() { ... }
    public void doB() { ... }
    public void doC() { ... }
    // ... 50 more methods
}

// AFTER: Focused coordinator + specialists
public class BigClass {
    private final ComponentA componentA;
    private final ComponentB componentB;
    private final ComponentC componentC;

    public void doA() { componentA.execute(); }
    public void doB() { componentB.execute(); }
    public void doC() { componentC.execute(); }
}
```

---

## 6. Testing Requirements

### Test Coverage Goals

| Package Type | Minimum Coverage |
|--------------|------------------|
| Core (action, execution) | 60% |
| LLM Integration | 40% |
| Memory Systems | 40% |
| New Features | 50% |

### Test Structure

```java
@DisplayName("Feature Name Tests")
class FeatureTest {

    @BeforeEach
    void setUp() {
        // Initialize test state
    }

    @Nested
    @DisplayName("Specific Behavior")
    class BehaviorTests {

        @Test
        @DisplayName("Should do X when Y")
        void shouldDoXWhenY() {
            // Arrange
            // Act
            // Assert
        }
    }
}
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ClassNameTest

# Run with coverage
./gradlew test jacocoTestReport
```

---

## 7. Common Tasks

### Task: Add a New Action Type

1. Create class extending `BaseAction`:
```java
public class NewAction extends BaseAction {
    public NewAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onTick() {
        // Called 20 times per second
        // Return when complete
    }

    @Override
    public boolean isComplete() {
        return completed;
    }
}
```

2. Register in `CoreActionsPlugin.java`:
```java
registry.register("new_action", (foreman, task, ctx) -> new NewAction(foreman, task));
```

3. Add tests in `NewActionTest.java`

### Task: Add a New LLM Provider

1. Implement `AsyncLLMClient` interface
2. Add to `TaskPlanner.java` provider selection
3. Add configuration in `MineWrightConfig`
4. Test with mock responses

### Task: Fix a Thread Safety Issue

1. Identify the shared state
2. Choose appropriate synchronization:
   - `ConcurrentHashMap` for maps
   - `AtomicX` for counters
   - `volatile` for single variables
   - `synchronized` for complex operations
3. Add concurrent access tests
4. Document the thread safety guarantee

---

## 8. Improvement Opportunities

### High Priority (Look for These)

1. **Large Files** (>500 lines) - Need refactoring
2. **Missing Tests** - Packages without test coverage
3. **TODO/FIXME Comments** - Documented improvements
4. **Thread Safety** - Look for non-atomic check-then-act

### Current Refactoring Candidates

| File | Lines | Issue | Priority |
|------|-------|-------|----------|
| ConfigDocumentation.java | 907 | Large | P2 |
| MilestoneTracker.java | 899 | Large | P2 |
| AStarPathfinder.java | 840 | Complex | P3 |
| ContractNetManager.java | 800 | Large | P2 |
| TaskProgress.java | 790 | Data class | P3 |

### Packages Needing Tests

- `config/` - No tests
- `di/` - No tests
- `personality/` - No tests
- `structure/` - No tests
- `voice/` - No tests

---

## 9. Documentation Resources

### Essential Reading

| Document | Purpose | Location |
|----------|---------|----------|
| CLAUDE.md | Project instructions | Root |
| FUTURE_ROADMAP.md | Current priorities | docs/ |
| INDEX.md | Documentation index | docs/ |

### Architecture Documentation

| Document | Purpose |
|----------|---------|
| docs/architecture/TECHNICAL_DEEP_DIVE.md | System design |
| docs/architecture/CASCADE_ROUTER.md | LLM routing |
| docs/architecture/MULTI_AGENT_COORDINATION.md | Multi-agent |

### Research Documentation

| Document | Status | Purpose |
|----------|--------|---------|
| MINECRAFT_AI_SOTA_2024_2025.md | Current | Latest research |
| VOYAGER_SKILL_SYSTEM.md | Applied | Skill patterns |
| BARITONE_MINEFLAYER_ANALYSIS.md | Applied | Pathfinding |

### Audit Documentation

All audit reports are in `docs/audits/`. Key audits:

- `SCRIPT_PARSER_REFACTOR.md` - God class elimination example
- `SPOTBUGS_CRITICAL_FIXES_COMPLETE.md` - Security fixes
- `THREAD_SAFETY_FIXES_WAVE53.md` - Thread safety patterns

---

## 10. Emergency Procedures

### Build Fails

1. Check error messages for missing imports
2. Run `./gradlew clean build`
3. Verify Java 17 is being used
4. Check for API changes in dependencies

### Tests Fail

1. Run single test to isolate: `./gradlew test --tests ClassNameTest`
2. Check test output for assertion failures
3. Verify mock setup is correct
4. Check for threading issues (race conditions)

### LLM Calls Fail

1. Check API key configuration
2. Verify network connectivity
3. Check rate limiting (use batching client)
4. Test with mock LLM client

---

## 11. Commit and Push Guidelines

### Commit Message Format

```
[Category]: Brief description

- Detailed change 1
- Detailed change 2

Fixes #issue (if applicable)
```

Categories:
- `[Fix]` - Bug fixes
- `[Feature]` - New features
- `[Refactor]` - Code restructuring
- `[Test]` - Test additions/changes
- `[Docs]` - Documentation updates
- `[Perf]` - Performance improvements

### Before Pushing

1. Run `./gradlew build` - Must pass
2. Run `./gradlew test` - Must pass
3. Update FUTURE_ROADMAP.md if significant
4. Check for sensitive data (API keys, etc.)

---

## 12. Getting Help

### Where to Look

1. **Code Comments** - Many files have detailed explanations
2. **Test Files** - Tests show intended behavior
3. **Audit Documents** - `docs/audits/` has detailed analysis
4. **Research Documents** - `docs/research/` explains patterns

### Key Files for Specific Topics

| Topic | File to Read |
|-------|--------------|
| Actions | `ActionExecutor.java` |
| LLM Integration | `TaskPlanner.java` |
| Memory | `CompanionMemory.java` |
| Multi-Agent | `OrchestratorService.java` |
| Behavior Trees | `behavior/ProcessManager.java` |
| Pathfinding | `pathfinding/AStarPathfinder.java` |

---

## 13. Success Metrics

### How to Know You're Doing Well

- ✅ Build passes (`./gradlew build`)
- ✅ Tests pass (`./gradlew test`)
- ✅ No new SpotBugs warnings
- ✅ Code coverage maintained or improved
- ✅ Documentation updated if needed
- ✅ Commit history is clean and descriptive

### Quality Indicators

| Metric | Target | Current |
|--------|--------|---------|
| Build Success | 100% | 100% |
| Test Pass Rate | 100% | 100% |
| Code Coverage | 50%+ | ~45% |
| Large Files (>500 lines) | <10 | ~20 |
| TODO/FIXME Count | <10 | ~4 |

---

## 14. Final Checklist Before Starting Work

Complete this checklist for every task:

- [ ] I have read the relevant code
- [ ] I have checked for existing tests
- [ ] I have reviewed related documentation
- [ ] I understand the thread safety requirements
- [ ] I have a plan for testing my changes
- [ ] I know which files I will modify
- [ ] I have checked FUTURE_ROADMAP.md for alignment

---

## Appendix: Quick Reference Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests ClassNameTest

# Clean build artifacts
./gradlew clean

# Run Minecraft client
./gradlew runClient

# Check for large files
find src/main/java -name "*.java" -exec wc -l {} \; | sort -rn | head -20

# Find TODO comments
grep -rn "TODO\|FIXME" src/main/java --include="*.java"

# Check test coverage by package
ls src/test/java/com/minewright/*/ | wc -l
```

---

**Welcome to the team! Your contributions will help make MineWright the best Minecraft AI companion system ever built.**

*Document Version: 1.0 | Last Updated: 2026-03-04*
