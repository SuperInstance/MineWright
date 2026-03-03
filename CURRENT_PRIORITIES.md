# Current Priorities

**What to work on next in the Steve AI project.**

Last Updated: 2026-03-03

---

## Priority Matrix

| Priority | Task | Effort | Impact | Status |
|----------|------|--------|--------|--------|
| **P0** | Add tests for ActionExecutor | Medium | High | 🔴 Not Started |
| **P0** | Add tests for AgentStateMachine | Medium | High | 🔴 Not Started |
| **P1** | Implement skill composition system | High | Very High | 🟡 Foundation Ready |
| **P1** | Complete multi-agent coordination | Medium | High | 🟡 50% Done |
| **P2** | Re-enable Checkstyle/SpotBugs | Low | Medium | 🔴 Disabled |
| **P2** | Complete dissertation Chapter 3 | Medium | High | 🟡 60% Done |
| **P3** | Script DSL implementation | High | Medium | 🔴 Not Started |
| **P3** | LLM→Script generation pipeline | High | Medium | 🟡 50% Done |

---

## P0: Critical (Do First)

### 1. Add Tests for ActionExecutor

**Why**: Core execution engine, no tests = risk of regression

**Location**: `src/test/java/com/minewright/action/ActionExecutorTest.java`

**What to Test**:
```java
@Test void testExecuteSingleTask()
@Test void testExecuteMultipleTasksInOrder()
@Test void testTaskFailureHandling()
@Test void testInterceptorChainCalled()
@Test void testStateTransitions()
@Test void testConcurrentTaskExecution()
```

**Approach**:
1. Mock ForemanEntity and Task
2. Create mock actions that succeed/fail
3. Verify state transitions
4. Verify interceptor calls

### 2. Add Tests for AgentStateMachine

**Why**: State management is critical, untested = bugs

**Location**: `src/test/java/com/minewright/execution/AgentStateMachineTest.java`

**What to Test**:
```java
@Test void testIdleToPlanningTransition()
@Test void testPlanningToExecutingTransition()
@Test void testExecutingToCompletedTransition()
@Test void testInvalidTransitionRejected()
@Test void testErrorStateRecovery()
@Test void testStatePersistence()
```

**Approach**:
1. Create state machine instance
2. Trigger valid transitions
3. Verify state changes
4. Test invalid transitions are rejected

---

## P1: High Priority

### 3. Implement Skill Composition System

**Why**: Voyager-style skill composition = 3.3x better performance

**Current State**: Foundation exists (SkillLibrary, embeddings), composition not implemented

**What to Build**:
```java
class SkillComposer {
    // Compose simple skills into complex ones
    Skill compose(List<Skill> skills, String goal);

    // Validate skill dependencies
    boolean validateDependencies(Skill skill);

    // Execute composed skill
    CompletableFuture<SkillResult> executeComposed(Skill composed);
}
```

**Implementation Steps**:
1. Create `SkillComposer.java` in `skill/` package
2. Implement dependency graph validation
3. Add composition execution with rollback
4. Store composed skills in library
5. Add tests for composition

**Reference**: `docs/research/VOYAGER_SKILL_SYSTEM.md`

### 4. Complete Multi-Agent Coordination

**Why**: Multiple agents working together = emergent intelligence

**Current State**: Contract Net Protocol framework exists, bidding not implemented

**What to Build**:
```java
class ContractNetProtocol {
    // Agents bid on tasks
    List<TaskBid> collectBids(TaskAnnouncement announcement);

    // Select best bid based on:
    // - Capability match
    // - Current workload
    // - Proximity to task
    TaskBid selectBestBid(List<TaskBid> bids);

    // Award contract and monitor execution
    void awardContract(TaskBid winner);
}
```

**Implementation Steps**:
1. Implement bidding logic in `ContractNetManager`
2. Add capability matching
3. Add workload tracking
4. Add proximity-based scoring
5. Test with multiple mock agents

**Reference**: `docs/research/CREW_COORDINATION_PATTERNS.md`

---

## P2: Medium Priority

### 5. Re-enable Checkstyle and SpotBugs

**Why**: Code quality enforcement prevents technical debt

**Current State**: Configured but `ignoreFailures = true`

**What to Do**:
1. Run `./gradlew checkstyleMain` and fix warnings
2. Run `./gradlew spotbugsMain` and fix bugs
3. Change `ignoreFailures = false` in `build.gradle`
4. Add to CI pipeline

**Files to Modify**: `build.gradle`

### 6. Complete Dissertation Chapter 3

**Why**: Academic milestone, A+ grade target

**Current State**: 60% integrated

**What's Needed**:
- Emotional AI section completion
- More citations from 2024-2025 research
- Limitations discussion
- Integration with code examples

**Reference**: `docs/research/DISSERTATION_INTEGRATION_SUMMARY.md`

---

## P3: Future Work

### 7. Script DSL Implementation

**Why**: Declarative automation scripts reduce LLM token usage

**Current State**: Designed but not implemented

**What to Build**:
```javascript
// Example DSL syntax
SCRIPT "mine_iron"
  TRIGGER "need_iron"
  PRECONDITION "has_pickaxe"

  SEQUENCE {
    PATHFIND nearest("iron_ore")
    MINE "iron_ore" quantity(10)
    RETURN_TO player
  }

  ON_FAIL {
    SAY "I couldn't find enough iron!"
    REQUEST_HELP
  }
END
```

**Implementation Steps**:
1. Define ANTLR grammar or use programmatic builder
2. Create `ScriptDSLParser.java`
3. Create `ScriptDSLExecutor.java`
4. Add to skill library
5. LLM generates DSL instead of JSON

### 8. LLM→Script Generation Pipeline

**Why**: Automate script creation from natural language

**Current State**: 50% complete (prompt engineering done, execution pending)

**What to Build**:
```java
class ScriptGenerator {
    // Generate script from natural language
    CompletableFuture<Script> generateFromPrompt(String userRequest);

    // Validate generated script
    ValidationResult validate(Script script);

    // Refine based on execution feedback
    Script refine(Script script, ExecutionFeedback feedback);
}
```

---

## Quick Wins (Do Anytime)

These are small improvements that can be done in parallel:

| Task | Effort | Impact |
|------|--------|--------|
| Add JavaDoc to public APIs | Low | Medium |
| Remove unused imports | Low | Low |
| Add more inline comments | Low | Medium |
| Update CLAUDE.md metrics | Low | Low |
| Fix typos in documentation | Low | Low |
| Add logging statements | Low | Medium |

---

## Research Opportunities

If you want to do research instead of coding:

1. **Analyze Baritone source code** - Document goal composition patterns
2. **Study Voyager skill system** - Extract composition algorithms
3. **Research DEPS planning** - Compare with our HTN planner
4. **Study DreamerV3** - World model architecture for Minecraft
5. **Research LLM tool use** - Function calling best practices

**Output**: Create markdown documents in `docs/research/`

---

## What NOT to Do

Don't waste time on these:

- ❌ Optimizing premature performance (profile first)
- ❌ Adding features not in priorities (scope creep)
- ❌ Rewriting working code (if it works, don't touch)
- ❌ Over-engineering solutions (keep it simple)
- ❌ Adding unnecessary abstractions (YAGNI)

---

## Session Checklist

Before ending your session:

- [ ] Code compiles: `./gradlew compileJava`
- [ ] No new TODOs added without tracking
- [ ] Changes committed with descriptive message
- [ ] Push to repository: `git push origin clean-main`
- [ ] Update this file if priorities changed

---

## Need Help?

1. Read `CLAUDE.md` for comprehensive project guide
2. Read `AGENT_ONBOARDING.md` for quick start
3. Check `docs/research/` for domain knowledge
4. Look at existing tests for patterns
5. Review `ARCHITECTURE_QUICK_REFERENCE.md` for design patterns

---

*Last Updated: 2026-03-03*
*Next Review: After completing P0 tasks*
