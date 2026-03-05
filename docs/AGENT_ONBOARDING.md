# Agent Onboarding - Unified Knowledge Entry Point

**Version:** 2.0 - Orchestrated Development Edition
**Last Updated:** 2026-03-05
**Purpose:** Central onboarding for all AI agents joining the MineWright project
**Status:** Active - Orchestrated Development Mode

---

## Welcome to MineWright

You are joining **MineWright**, an AI-powered Minecraft companions project at an exciting time. We are executing a **30-round orchestrated development plan** to reduce the codebase to irreducible complexity while improving quality, coverage, and maintainability.

This document is your **starting point** for understanding how to contribute effectively.

---

## Quick Start (15 minutes)

```
1. Read this document (5 min)
   ├─ Understand the project
   ├─ Know your role
   └─ Learn the workflow

2. Read your specialized onboarding (10 min)
   ├─ See docs/agents/ directory
   ├─ Find your agent type
   └─ Learn your responsibilities

3. Join the current round
   └─ Coordinate with the Orchestrator
```

---

## Project Overview

### What is MineWright?

MineWright is "Cursor for Minecraft" - autonomous AI agents that play Minecraft through natural language commands. Users type commands, and AI-controlled Foreman entities execute them via LLM-powered planning.

**Core Philosophy:** "One Abstraction Away" - LLMs plan and coordinate; traditional game AI (behavior trees, FSMs, scripts) executes in real-time.

### Current Mode: Orchestrated Development

We are in **Streamlining & Refactoring Mode** - executing a systematic 30-round plan to achieve irreducible complexity:

- **Goal:** Reduce code from 115,937 LOC to <100,000 LOC
- **Coverage:** Increase from 40% to 60%+ test coverage
- **Complexity:** Reduce average complexity from 8 to <6
- **Duplication:** Eliminate from 12% to <5% duplication

### How Orchestrated Development Works

```
┌─────────────────────────────────────────────────────────────────┐
│                    THE ORCHESTRATOR                             │
│                 (Claude - Team Lead)                            │
│                                                                  │
│  1. Plans rounds with specific goals                            │
│  2. Coordinates specialized agents                              │
│  3. Synthesizes findings                                        │
│  4. Maintains quality standards                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Deploys teams for each round
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPECIALIZED AGENTS                            │
│                                                                  │
│  • Code Analyst - Deep code analysis                            │
│  • Refactoring Specialist - Code structure improvement          │
│  • Testing Engineer - Test coverage and quality                 │
│  • Performance Optimizer - Speed and efficiency                 │
│  • Bug Investigator - Issue diagnosis and resolution            │
│  • Documentation Specialist - Knowledge creation                │
│  • Quality Assurance Analyst - Standards enforcement            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Agent Types

### 1. Code Analyst

**Purpose:** Deep analysis of code structure, patterns, and issues

**When Deployed:**
- Understanding a new subsystem
- Finding code duplication
- Identifying architectural patterns
- Analyzing complexity hotspots

**Output:** Analysis reports with findings, metrics, and recommendations

**Onboarding:** [CODE_ANALYST_ONBOARDING.md](agents/CODE_ANALYST_ONBOARDING.md)

### 2. Refactoring Specialist

**Purpose:** Improve code structure while preserving behavior

**When Deployed:**
- Consolidating duplicate code
- Simplifying complex methods
- Breaking up large classes
- Applying design patterns

**Output:** Refactored code with preserved behavior

**Onboarding:** [REFACTORING_SPECIALIST_ONBOARDING.md](agents/REFACTORING_SPECIALIST_ONBOARDING.md)

### 3. Testing Engineer

**Purpose:** Ensure code quality through comprehensive testing

**When Deployed:**
- Writing unit tests
- Improving coverage
- Creating integration tests
- Adding regression tests

**Output:** Comprehensive test suite with high coverage

**Onboarding:** [TESTING_ENGINEER_ONBOARDING.md](agents/TESTING_ENGINEER_ONBOARDING.md)

### 4. Performance Optimizer

**Purpose:** Optimize code performance while maintaining correctness

**When Deployed:**
- Identifying bottlenecks
- Optimizing hot paths
- Reducing memory usage
- Improving algorithms

**Output:** Faster, more efficient code

**Onboarding:** [PERFORMANCE_OPTIMIZER_ONBOARDING.md](agents/PERFORMANCE_OPTIMIZER_ONBOARDING.md)

### 5. Bug Investigator

**Purpose:** Investigate, diagnose, and help resolve bugs

**When Deployed:**
- Reproducing issues
- Finding root causes
- Proposing solutions
- Verifying fixes

**Output:** Bug reports with root cause analysis and solutions

**Onboarding:** [BUG_INVESTIGATOR_ONBOARDING.md](agents/BUG_INVESTIGATOR_ONBOARDING.md)

### 6. Documentation Specialist

**Purpose:** Create and maintain comprehensive documentation

**When Deployed:**
- Writing API documentation
- Creating user guides
- Updating technical docs
- Organizing knowledge

**Output:** Clear, comprehensive documentation

**Onboarding:** [DOCUMENTATION_SPECIALIST_ONBOARDING.md](agents/DOCUMENTATION_SPECIALIST_ONBOARDING.md)

### 7. Quality Assurance Analyst

**Purpose:** Ensure code quality through analysis and validation

**When Deployed:**
- Running static analysis
- Reviewing code quality
- Tracking metrics
- Enforcing standards

**Output:** Quality reports and metrics

**Onboarding:** [QUALITY_ANALYST_ONBOARDING.md](agents/QUALITY_ANALYST_ONBOARDING.md)

---

## Workflow

### Round Structure

Each development round follows this structure:

```
┌─────────────────────────────────────────────────────────────────┐
│                        ROUND N                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. ANALYSIS PHASE                                              │
│     ├─ Identify what needs doing                                │
│     ├─ Deploy Code Analysts                                     │
│     └─ Gather findings                                          │
│                                                                 │
│  2. PLANNING PHASE                                              │
│     ├─ Prioritize findings                                      │
│     ├─ Design approach                                          │
│     └─ Plan agent deployment                                    │
│                                                                 │
│  3. EXECUTION PHASE                                             │
│     ├─ Deploy specialized agents                                │
│     ├─ Monitor progress                                         │
│     └─ Coordinate work                                          │
│                                                                 │
│  4. SYNTHESIS PHASE                                             │
│     ├─ Compile results                                          │
│     ├─ Document changes                                         │
│     └─ Extract lessons                                          │
│                                                                 │
│  5. QUALITY GATES                                               │
│     ├─ All tests pass                                           │
│     ├─ Build succeeds                                           │
│     ├─ No new warnings                                          │
│     └─ Coverage maintained                                      │
│                                                                 │
│  6. NEXT ROUND PLANNING                                         │
│     ├─ Review metrics                                           │
│     ├─ Update roadmap                                           │
│     └─ Plan next round                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Agent Collaboration Patterns

**Sequential Handoff:**
```
Code Analyst → Refactoring Specialist → Testing Engineer → QA Analyst
```

**Parallel Analysis:**
```
Code Analyst 1 ─┐
Code Analyst 2 ─┼→ Synthesis → Planning
Code Analyst 3 ─┘
```

**Review Loop:**
```
Refactoring Specialist → Testing Engineer → (if fails) → Refactoring Specialist
```

---

## Knowledge Ecosystem

### Foundational Documents

**Start Here:**
- [README.md](../README.md) - Project overview for everyone
- [CLAUDE.md](../CLAUDE.md) - Orchestrator's command center
- [KNOWLEDGE_INDEX.md](KNOWLEDGE_INDEX.md) - Gateway to all documentation

**Meta-Skills:**
- [META_COGNITION.md](META_COGNITION.md) - How to think effectively
- [INVESTIGATION_PROTOCOLS.md](INVESTIGATION_PROTOCOLS.md) - How to explore codebases
- [KNOWLEDGE_SYNTHESIS.md](KNOWLEDGE_SYNTHESIS.md) - How everything connects

**Design Understanding:**
- [ARCHITECTURAL_WISDOM.md](ARCHITECTURAL_WISDOM.md) - Why design decisions were made
- [PATTERN_LANGUAGE.md](PATTERN_LANGUAGE.md) - Patterns used in the codebase

**Strategic Planning:**
- [FUTURE_ROADMAP.md](FUTURE_ROADMAP.md) - 30-round development plan

**Day-to-Day Work:**
- [WORK_PATTERNS.md](WORK_PATTERNS.md) - Established patterns and practices
- [IMPROVEMENT_OPPORTUNITIES.md](IMPROVEMENT_OPPORTUNITIES.md) - Known improvement areas

### Specialized Knowledge

**Architecture:**
- [architecture/TECHNICAL_DEEP_DIVE.md](architecture/TECHNICAL_DEEP_DIVE.md) - Deep technical details
- [architecture/](architecture/) - More architecture documents

**Capabilities:**
- [agent-guides/GUIDE_INDEX.md](agent-guides/GUIDE_INDEX.md) - Index of capability guides
- [agent-guides/](agent-guides/) - Detailed guides for each capability

**Agents:**
- [agents/](agents/) - Specialized agent onboarding documents (you are here)

### Analysis and Audits

**Audits:**
- [audits/](audits/) - Code audit findings and analysis
- [audits/BASELINE_METRICS.md](audits/BASELINE_METRICS.md) - Baseline measurements

---

## System Overview

### Architecture

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

### Key Files

**Entry Points:**
- `src/main/java/com/minewright/MineWrightMod.java` - Mod initialization
- `src/main/java/com/minewright/entity/ForemanEntity.java` - Main AI entity (~700 LOC)
- `src/main/java/com/minewright/action/ActionExecutor.java` - Execution engine (~915 LOC)

**Key Subsystems:**
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

## Your First Steps

### Step 1: Identify Your Role

Are you:
- The **Orchestrator**? Read [CLAUDE.md](../CLAUDE.md)
- A **Code Analyst**? Read [CODE_ANALYST_ONBOARDING.md](agents/CODE_ANALYST_ONBOARDING.md)
- A **Refactoring Specialist**? Read [REFACTORING_SPECIALIST_ONBOARDING.md](agents/REFACTORING_SPECIALIST_ONBOARDING.md)
- A **Testing Engineer**? Read [TESTING_ENGINEER_ONBOARDING.md](agents/TESTING_ENGINEER_ONBOARDING.md)
- A **Performance Optimizer**? Read [PERFORMANCE_OPTIMIZER_ONBOARDING.md](agents/PERFORMANCE_OPTIMIZER_ONBOARDING.md)
- A **Bug Investigator**? Read [BUG_INVESTIGATOR_ONBOARDING.md](agents/BUG_INVESTIGATOR_ONBOARDING.md)
- A **Documentation Specialist**? Read [DOCUMENTATION_SPECIALIST_ONBOARDING.md](agents/DOCUMENTATION_SPECIALIST_ONBOARDING.md)
- A **Quality Assurance Analyst**? Read [QUALITY_ANALYST_ONBOARDING.md](agents/QUALITY_ANALYST_ONBOARDING.md)

### Step 2: Read Your Specialized Onboarding

Each specialized onboarding document includes:
- Your mission and responsibilities
- Your framework and process
- Your tools and techniques
- Your best practices
- Collaboration guidelines

### Step 3: Study the Meta-Skills

Regardless of your role, study these:
1. [META_COGNITION.md](META_COGNITION.md) - How to think effectively
2. [INVESTIGATION_PROTOCOLS.md](INVESTIGATION_PROTOCOLS.md) - How to explore codebases
3. [PATTERN_LANGUAGE.md](PATTERN_LANGUAGE.md) - Patterns in the codebase

### Step 4: Understand the Project

- Read [README.md](../README.md) for project overview
- Read [ARCHITECTURAL_WISDOM.md](ARCHITECTURAL_WISDOM.md) for design rationale
- Read [FUTURE_ROADMAP.md](FUTURE_ROADMAP.md) for strategic plan

### Step 5: Join the Current Round

Coordinate with the Orchestrator to:
- Understand current round goals
- Receive your assignment
- Begin contributing

---

## Code Style and Conventions

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

## Quality Standards

### Code Quality

- All tests must pass
- No new SpotBugs critical issues
- Checkstyle violations minimized
- Coverage maintained or improved

### Professional Quality

- Clear commit messages
- Well-documented changes
- Follow established patterns
- Coordinate with team

### Documentation Quality

- Update docs with changes
- Explain your reasoning
- Record lessons learned
- Share your findings

---

## Quick Reference

### Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests MyTest

# Generate coverage report
./gradlew test jacocoTestReport

# Run SpotBugs
./gradlew spotbugsMain

# Run Checkstyle
./gradlew checkstyleMain
```

### Key Locations

**Source Code:**
- Production: `src/main/java/com/minewright/`
- Tests: `src/test/java/com/minewright/`

**Documentation:**
- This file: `docs/AGENT_ONBOARDING.md`
- Specialized onboarding: `docs/agents/`
- Architecture: `docs/architecture/`
- Guides: `docs/agent-guides/`

### Common Tasks

**Add a New Action:**
1. Create class extending `BaseAction`
2. Register in `CoreActionsPlugin.java`
3. Add tests

**Add a New LLM Provider:**
1. Implement `AsyncLLMClient` interface
2. Add to `TaskPlanner.java` provider selection
3. Add configuration in `MineWrightConfig`
4. Test with mock responses

**Fix Thread Safety:**
1. Identify shared state
2. Choose appropriate synchronization
3. Add concurrent access tests
4. Document the guarantee

---

## Success Criteria

### For Each Round

- [ ] Goals achieved
- [ ] Quality gates passed
- [ ] Findings documented
- [ ] Lessons learned
- [ ] Next round planned

### For Each Agent

- [ ] Understood responsibilities
- [ ] Followed framework
- [ ] Produced quality output
- [ ] Collaborated effectively
- [ ] Documented work

---

## The Promise

> Each generation of agents stands on the shoulders of those who came before, reaching ever higher.

This knowledge ecosystem exists to make you more effective. The specialized onboarding documents, meta-skills guides, and strategic planning documents are all designed to help you contribute meaningfully to the project.

**Learn from those who came before. Contribute to those who will follow.**

---

## Appendix

### A. Agent Quick Reference

| Agent | Focus | Output | Onboarding |
|-------|-------|--------|------------|
| Code Analyst | Analysis | Reports with findings | [CODE_ANALYST](agents/CODE_ANALYST_ONBOARDING.md) |
| Refactoring Specialist | Structure | Improved code | [REFACTORING](agents/REFACTORING_SPECIALIST_ONBOARDING.md) |
| Testing Engineer | Quality | Comprehensive tests | [TESTING](agents/TESTING_ENGINEER_ONBOARDING.md) |
| Performance Optimizer | Speed | Optimized code | [PERFORMANCE](agents/PERFORMANCE_OPTIMIZER_ONBOARDING.md) |
| Bug Investigator | Issues | Bug reports with solutions | [BUGS](agents/BUG_INVESTIGATOR_ONBOARDING.md) |
| Documentation Specialist | Knowledge | Clear documentation | [DOCS](agents/DOCUMENTATION_SPECIALIST_ONBOARDING.md) |
| QA Analyst | Standards | Quality reports | [QA](agents/QUALITY_ANALYST_ONBOARDING.md) |

### B. Learning Pathway

**New Agent (1 day):**
1. AGENT_ONBOARDING.md (this file) - 30 min
2. Specialized onboarding - 2 hours
3. META_COGNITION.md (sections 1-3) - 1 hour
4. PATTERN_LANGUAGE.md (scan) - 30 min
5. Join current round - remainder

**Experienced Agent (1 week):**
1. Complete all foundational documents
2. Deep dive into 1-2 subsystems
3. Master your specialization
4. Contribute to knowledge ecosystem

### C. Getting Help

1. **Check documentation** - The answer may already be documented
2. **Ask the Orchestrator** - Coordinate with me for guidance
3. **Consult specialists** - Other agents may have expertise
4. **Document the answer** - Help future agents

### D. Quick Commands

```bash
# Find related code
grep -r "keyword" src/main/java --include="*.java" -l

# Check for large files
find src/main/java -name "*.java" -exec wc -l {} \; | sort -rn | head -20

# Find TODO comments
grep -rn "TODO\|FIXME" src/main/java --include="*.java"

# Check test coverage
./gradlew test jacocoTestReport
```

---

**Document Version:** 2.0
**Last Updated:** 2026-03-05
**Maintained By:** Claude Orchestrator
**Status:** Active - Orchestrated Development Mode

---

**You are here. The work awaits. Begin your journey.**
