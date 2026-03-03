# Steve AI - Unified Vision Document

**Document Status:** THE source of truth for Steve AI project direction
**Version:** 1.0
**Created:** 2026-03-03
**Next Review:** 2026-06-03 (quarterly)
**Maintained By:** Project Leadership

---

## Document Purpose

This document consolidates findings from 6 comprehensive audits into a **single unified vision** for the Steve AI project. It serves as the definitive source of truth for:

1. **Current State Assessment** - Where we are now
2. **Target Vision** - Where we want to be
3. **Unified Action Plan** - How to get there
4. **Quality Standards** - What "good" looks like
5. **Success Metrics** - How to measure progress

**No more scattered priorities** - this document aligns all audits, removes duplication, and provides one clear direction.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Assessment](#current-state-assessment)
3. [Target Vision](#target-vision)
4. [Consolidated Action Plan](#consolidated-action-plan)
5. [Architecture Target](#architecture-target)
6. [Quality Standards](#quality-standards)
7. [Success Metrics](#success-metrics)
8. [Implementation Timeline](#implementation-timeline)

---

## Executive Summary

### Project Health: B+ (82/100)

The Steve AI project is a **sophisticated, well-architected system** with strong foundations but clear areas for improvement. After consolidating 6 comprehensive audits (Architecture, Code Quality, Security, Performance, Thread Safety, Test Coverage, Documentation, and Action Execution Bugs), we have a clear picture of the current state and path forward.

### Key Strengths

| Area | Strength | Impact |
|------|----------|--------|
| **Architecture** | Clear three-layer design (Brain → Script → Physical) | Maintainable, extensible |
| **Design Patterns** | State, Observer, Strategy, Factory, Interceptor | Professional code quality |
| **Thread Safety** | Extensive use of concurrent collections | Production-ready concurrency |
| **Documentation** | 671,982 lines across 539 files | Comprehensive knowledge base |
| **Test Coverage** | 3,933 tests across 122 files | Solid testing foundation |
| **Security** | Input sanitization, environment variable support | Production-ready security posture |

### Critical Issues Summary (P0)

| Issue | Category | Impact | Fix Time |
|-------|----------|--------|----------|
| **Memory Leaks** | Performance/Thread Safety | Resource exhaustion | 4 hours |
| **Empty Catch Blocks** | Security/Code Quality | Silent failures, debugging difficulty | 2 hours |
| **God Objects** | Architecture | Maintainability nightmare | 40 hours |
| **Unbounded Collections** | Performance | Memory exhaustion risk | 3 hours |
| **Test Compilation** | Test Coverage | Tests cannot run | 2 hours |
| **Missing Input Validation** | Security | Prompt injection via async paths | 1 hour |
| **CopyOnWriteArrayList Overuse** | Performance | O(n²) operations | 2 hours |

**Total P0 Fix Time:** 54 hours (~1.5 weeks focused work)

### Overall Assessment by Category

| Category | Score | Grade | Priority |
|----------|-------|-------|----------|
| **Architecture** | 85/100 | A- | P1 - High |
| **Code Quality** | 78/100 | B+ | P1 - High |
| **Security** | 84/100 | B+ | P0 - Critical |
| **Performance** | 82/100 | B+ | P0 - Critical |
| **Thread Safety** | 72/100 | B- | P0 - Critical |
| **Test Coverage** | 70/100 | C+ | P1 - High |
| **Documentation** | 75/100 | B | P2 - Medium |

**Composite Score: 82/100 (B+)**

---

## Current State Assessment

### Project Metrics

| Metric | Value | Assessment |
|--------|-------|------------|
| **Source Files** | 326 Java files | Large enterprise codebase |
| **Source Lines** | 115,920 lines | Substantial implementation |
| **Test Files** | 122 test files | Good test foundation |
| **Test Lines** | 77,312 lines | Comprehensive test suite |
| **Documentation Files** | 539 markdown files | Exceptional documentation |
| **Documentation Lines** | 671,982 lines | Industry-leading knowledge base |
| **Packages** | 50 packages | Well-organized structure |
| **Test/Source Ratio** | 1:1.5 | Healthy balance |
| **JavaDoc Coverage** | 61% | Needs improvement |
| **Test Coverage** | 40% (increasing) | Room for improvement |

### What's Working Well

#### 1. Architecture Excellence

**Three-Layer Architecture** - Clear separation of concerns:
```
Brain Layer (LLM) → Script Layer (BT/HTN/FSM) → Physical Layer (Actions)
```

**Strengths:**
- LLMs handle strategic planning only (30-60 second updates)
- Traditional game AI handles tactical execution (20 TPS)
- 10-20x cost reduction through smart layer separation
- No blocking on LLM calls during gameplay

**Design Patterns Used Correctly:**
- State Pattern (AgentStateMachine) - explicit transitions, thread-safe
- Observer Pattern (EventBus) - clean pub/sub, priority ordering
- Strategy Pattern (RecoveryStrategy) - pluggable behaviors
- Factory Pattern (ActionRegistry) - SPI-based plugins
- Interceptor Pattern (InterceptorChain) - clean cross-cutting

#### 2. Thread Safety Foundation

**Excellent Practices:**
- Extensive use of `ConcurrentHashMap` (no synchronized maps)
- `AtomicInteger` for counters (no volatile integer issues)
- `CopyOnWriteArrayList` for read-heavy lists (correct usage)
- Proper async/sync separation (no blocking game thread)

**No Circular Dependencies** - Clean dependency graph with clear layering

#### 3. Comprehensive Testing

**Test Infrastructure:**
- 3,933 test methods across 122 files
- Excellent core component coverage (execution, skill, behavior, pathfinding)
- Zero disabled tests (all tests active)
- Strong use of JUnit 5 features (nested tests, parameterized tests)

**Test Quality Highlights:**
- AgentStateMachineTest (1,048 lines, 60+ tests) - comprehensive state transitions
- PatternExtractorTest (1,051 lines, 38 tests) - edge case coverage
- Thread safety testing with concurrent access patterns

#### 4. Documentation Excellence

**Quantity:** 671,982 lines across 539 files (industry-leading)

**Quality:**
- Comprehensive research documentation (301 research documents)
- Architecture diagrams and technical deep-dives
- API reference and configuration guides
- Character and agent guides

**Organization:**
- Clear hierarchy (research/, architecture/, agent-guides/)
- Cross-referenced content
- Historical evolution tracking

#### 5. Security Consciousness

**Strong Practices:**
- Comprehensive input sanitization (`InputSanitizer.java`)
- Environment variable support for API keys
- No hardcoded credentials
- Secure HttpClient usage with timeouts
- Proper exception handling in async clients

### Critical Issues Summary

#### P0: Memory Leaks (Performance + Thread Safety)

**Issue 1: Uncanceled CompletableFuture in ActionExecutor**
- **Location:** `ActionExecutor.java:398-412`
- **Problem:** `stopCurrentAction()` cancels actions but not `planningFuture`
- **Impact:** Memory leaks, wasted API credits, orphaned LLM calls
- **Fix:** Add future cancellation in stop method
- **Time:** 1 hour

**Issue 2: EventBus Subscriptions Never Cleaned Up**
- **Location:** EventPublishingInterceptor integration
- **Problem:** Subscriptions accumulate when entities removed
- **Impact:** Memory leak accumulating dead entity references
- **Fix:** Track subscriptions, clean up on disposal
- **Time:** 1 hour

**Issue 3: Unbounded CopyOnWriteArrayList in CompanionMemory**
- **Location:** `CompanionMemory.java:180`
- **Problem:** Emotional memories list grows unbounded, O(n²) copies
- **Impact:** Memory exhaustion, GC pressure, server lag
- **Fix:** Replace with PriorityQueue + size limit
- **Time:** 2 hours

#### P0: Silent Failures (Security + Code Quality)

**Issue 4: Empty Catch Block in TracingConfig**
- **Location:** `TracingConfig.java:47`
- **Problem:** Configuration errors silently ignored
- **Impact:** Security configuration failures invisible
- **Fix:** Add proper error logging
- **Time:** 0.5 hours

**Issue 5: Missing Input Validation on Async Paths**
- **Location:** `TaskPlanner.java` - `planTasksAsync()` method
- **Problem:** Async methods bypass InputSanitizer checks
- **Impact:** Prompt injection attacks possible
- **Fix:** Add validation to all entry points
- **Time:** 1 hour

#### P0: Architectural Debt (Code Quality)

**Issue 6: God Objects**
- **Locations:**
  - `CompanionMemory.java` (1,890 lines)
  - `ForemanEntity.java` (1,242 lines)
  - `MineWrightConfig.java` (1,730 lines)
- **Problem:** Too many responsibilities, difficult to maintain
- **Impact:** Technical debt accumulation, slow development
- **Fix:** Extract subsystems (see Architecture Target)
- **Time:** 40 hours

#### P0: Test Infrastructure Broken

**Issue 7: Test Compilation Failure**
- **Problem:** 100 compilation errors from raw CompletableFuture types
- **Impact:** Tests cannot run, no safety net
- **Fix:** Add generic type arguments throughout
- **Time:** 2 hours

#### P0: Performance Hotspots

**Issue 8: O(n²) Emotional Memory Sorting**
- **Location:** `CompanionMemory.java:278-294`
- **Problem:** Sort + CopyOnWriteArrayList = O(n²) on every insertion
- **Impact:** 2,500 operations per memory recording
- **Fix:** Use PriorityQueue (O(log n) insertion)
- **Time:** 2 hours

**Issue 9: String Concatenation in Loops**
- **Locations:** Multiple files (15 files with System.out, others)
- **Problem:** String += in loops creates O(n²) allocations
- **Impact:** Unnecessary GC pressure
- **Fix:** Use StringBuilder
- **Time:** 3 hours

#### P0: Thread Safety Issues

**Issue 10: Race Condition in CollaborativeBuildManager**
- **Location:** `CollaborativeBuildManager.java:181-210`
- **Problem:** Non-atomic section assignment, check-then-act race
- **Impact:** Multiple foremen assigned to same section, lost updates
- **Fix:** Use `ConcurrentHashMap.computeIfAbsent()` for atomicity
- **Time:** 1 hour

**Issue 11: Non-Volatile State Flags**
- **Location:** `ActionExecutor.java` (multiple fields)
- **Problem:** `isPlanning`, `pendingCommand` not volatile
- **Impact:** Stale state reads across threads
- **Fix:** Add volatile modifiers
- **Time:** 1 hour

**Issue 12: TaskQueue Concurrent Modification**
- **Location:** `ActionExecutor.java:199-200`
- **Problem:** LinkedList not thread-safe, async/gamethread concurrent access
- **Impact:** ConcurrentModificationException, corrupted queue
- **Fix:** Use ConcurrentLinkedQueue
- **Time:** 1 hour

### Consolidated Issue Count by Severity

| Severity | Count | Categories | Fix Time |
|----------|-------|------------|----------|
| **P0 - Critical** | 12 | Memory, Security, Performance, Thread Safety | 54 hours |
| **P1 - High** | 23 | Architecture, Code Quality, Performance | 86 hours |
| **P2 - Medium** | 31 | Documentation, Test Coverage, Code Quality | 120 hours |
| **P3 - Low** | 18 | Best Practices, Technical Debt | 48 hours |
| **Total** | **84** | **All categories** | **308 hours** |

---

## Target Vision

### Vision Statement

**Steve AI will be the premier AI-powered Minecraft mod, combining cutting-edge LLM technology with traditional game AI to create intelligent, characterful autonomous agents that learn, adapt, and collaborate.**

### Desired State by Category

#### 1. Architecture (Target: A 95/100)

**Current:** 85/100 (A-)
**Target:** 95/100 (A)

**Key Characteristics:**
- **Clean Hexagonal Architecture** - Clear dependency flow
- **No God Objects** - All classes <500 lines
- **Repository Pattern** - Persistence abstraction
- **Dependency Injection** - No singletons
- **Plugin Architecture** - Extensible action system
- **Clear Layering** - Brain → Script → Physical

**Concrete Targets:**
```
Max class size:              500 lines (currently 1,890)
Max incoming dependencies: 20 (currently 59)
Singletons:                  0 (currently 8+)
Package dependencies:        Clear layered (currently mixed)
Repository interfaces:      100% coverage (currently 0%)
```

**Before/After Example:**

```java
// BEFORE (ForemanEntity, 1,242 lines)
public class ForemanEntity extends PathfinderMob {
    private ActionExecutor actionExecutor;
    private ProactiveDialogueManager dialogueManager;
    private ProcessManager processManager;
    private StuckDetector stuckDetector;
    private RecoveryManager recoveryManager;
    private SessionManager sessionManager;
    private CompanionMemory memory;
    // ... 100+ more lines of initialization
}

// AFTER (ForemanEntity, ~200 lines)
public class ForemanEntity extends PathfinderMob {
    private final AgentController controller;  // Delegates to subsystems

    public void tick() {
        controller.tick();
    }
}

// NEW: AgentController (coordinates subsystems)
public class AgentController {
    private final ActionSubsystem action;
    private final DialogueSubsystem dialogue;
    private final TacticalSubsystem tactical;
    private final RecoverySubsystem recovery;

    public void tick() {
        subsystems.tickAll();
    }
}
```

#### 2. Code Quality (Target: A 90/100)

**Current:** 78/100 (B+)
**Target:** 90/100 (A-)

**Key Characteristics:**
- **All files <500 lines** - Focused, maintainable classes
- **Zero code duplication** - DRY principle throughout
- **100% JavaDoc on public APIs** - Complete documentation
- **Proper encapsulation** - No public mutable fields
- **Consistent naming** - Clear conventions followed
- **Production logging** - No System.out.println

**Concrete Targets:**
```
Files > 500 lines:          0 (currently 12)
Code duplication:           <5% (currently ~15%)
JavaDoc coverage:          100% public APIs (currently 61%)
System.out usage:          0 files (currently 15)
Empty catch blocks:        0 (currently 30+)
Methods > 50 lines:        0 (currently TBD)
Cyclomatic complexity:     <10 per method (currently TBD)
```

**Quality Standards:**

```java
// STANDARD: Class size and structure
public class ExampleClass {  // Max 500 lines
    // 1. Static fields
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleClass.class);

    // 2. Instance fields (final where possible)
    private final Dependency dependency;
    private volatile State state;

    // 3. Constructor (dependency injection)
    @Inject
    public ExampleClass(Dependency dependency) {
        this.dependency = Objects.requireNonNull(dependency, "dependency");
    }

    // 4. Public API methods (with JavaDoc)
    /**
     * Performs operation with clear description.
     *
     * @param param the input parameter
     * @return the result
     * @throws IllegalArgumentException if param is invalid
     */
    public Result operation(Param param) {
        validate(param);
        return internalOperation(param);
    }

    // 5. Private helper methods
    private void validate(Param param) {
        if (param == null) {
            throw new IllegalArgumentException("param cannot be null");
        }
    }
}
```

#### 3. Security (Target: A 95/100)

**Current:** 84/100 (B+)
**Target:** 95/100 (A)

**Key Characteristics:**
- **Zero empty catch blocks** - All errors logged
- **Complete input validation** - All entry points sanitized
- **No credential exposure** - Logging redacts secrets
- **Rate limiting** - API abuse prevention
- **Audit logging** - Security events tracked
- **Resource limits** - No unbounded growth

**Concrete Targets:**
```
Empty catch blocks:         0 (currently 30+)
Input validation coverage: 100% entry points (currently 90%)
API key exposure:          0 partial keys logged (currently partial)
Rate limiting:            Implemented (currently not)
Unbounded collections:    0 (currently 4)
Memory leaks:             0 (currently 3)
```

**Security Standards:**

```java
// STANDARD: Input validation at all entry points
public class TaskPlanner {
    public CompletableFuture<ParsedResponse> planTasksAsync(
        ForemanEntity foreman,
        String command
    ) {
        // 1. Validate API key
        if (!MineWrightConfig.hasValidApiKey()) {
            LOGGER.error("[Async] Cannot plan tasks: API key not configured");
            return CompletableFuture.completedFuture(null);
        }

        // 2. Validate input for suspicious patterns
        if (InputSanitizer.containsSuspiciousPatterns(command)) {
            String reason = InputSanitizer.getSuspiciousPatternDescription(command);
            LOGGER.warn("[Async] Command rejected: {}. Command: {}", reason, command);
            return CompletableFuture.completedFuture(null);
        }

        // 3. Validate command length
        if (command.length() > MAX_COMMAND_LENGTH) {
            LOGGER.warn("[Async] Command too long: {} chars", command.length());
            return CompletableFuture.completedFuture(null);
        }

        // 4. Process validated command
        // ... rest of method
    }
}

// STANDARD: Never log credentials
public class MineWrightConfig {
    public void logApiKeyStatus() {
        if (hasValidApiKey()) {
            // GOOD: Only log presence, never content
            LOGGER.info("API key configured ({} chars)", apiKey.length());
        } else {
            LOGGER.warn("API key not configured");
        }
    }
}

// STANDARD: Always log exceptions
public class ExampleClass {
    public void riskyOperation() {
        try {
            doSomethingRisky();
        } catch (Exception e) {
            // GOOD: Log with context and stack trace
            LOGGER.error("Operation failed in context: contextInfo={}", context, e);
            // Optionally: fallback behavior
            fallbackOperation();
        }
    }
}
```

#### 4. Performance (Target: A 90/100)

**Current:** 82/100 (B+)
**Target:** 90/100 (A-)

**Key Characteristics:**
- **No O(n²) operations** in hot paths
- **Efficient data structures** - Right tool for the job
- **Memory pools** - Object reuse in hot paths
- **Caching** - Expensive operations cached
- **Lazy loading** - Resources loaded on demand
- **Batching** - Bulk operations where possible

**Concrete Targets:**
```
Memory recording time:     <2ms (currently 5-10ms) - 5x improvement
Memory search time:       <10ms (currently 20-50ms) - 3x improvement
LLM cache hit rate:       >60% (currently 0%) - 60% fewer calls
Memory per agent:         <30MB (currently ~50MB) - 40% reduction
Tick time (p99):          <5ms (currently 8ms) - 37.5% faster
O(n²) operations:        0 (currently 3)
Unbounded collections:    0 (currently 4)
```

**Performance Standards:**

```java
// STANDARD: Use appropriate data structures
public class MemoryManager {
    // BAD: O(n²) sorting with CopyOnWriteArrayList
    private final List<EmotionalMemory> emotionalMemories = new CopyOnWriteArrayList<>();

    // GOOD: O(log n) insertion with PriorityQueue
    private final PriorityQueue<EmotionalMemory> emotionalMemories =
        new PriorityQueue<>(50,
            Comparator.comparingInt((EmotionalMemory m) -> Math.abs(m.emotionalWeight))
                .reversed()
        );

    public void addMemory(EmotionalMemory memory) {
        emotionalMemories.add(memory);  // O(log n) instead of O(n²)
        if (emotionalMemories.size() > 50) {
            emotionalMemories.poll();  // O(log n) removal
        }
    }
}

// STANDARD: Cache expensive operations
public class TaskPlanner {
    private final Map<String, CompletableFuture<ParsedResponse>> planCache =
        new LinkedHashMap<>(50, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > 100;  // Max 100 cached plans
            }
        };

    public void processCommand(String command) {
        String fingerprint = normalizeCommand(command);

        // Check cache first
        CompletableFuture<ParsedResponse> cached = planCache.get(fingerprint);
        if (cached != null) {
            LOGGER.debug("Using cached plan for: {}", command);
            this.planningFuture = cached;
            return;
        }

        // Create new plan and cache it
        CompletableFuture<ParsedResponse> plan = planTasksAsync(command);
        planCache.put(fingerprint, plan);
        this.planningFuture = plan;
    }
}

// STANDARD: Pre-size collections
public class ContextBuilder {
    public String buildContext(List<Memory> memories) {
        // GOOD: Pre-size StringBuilder for average case
        StringBuilder sb = new StringBuilder(1024);  // Expect ~1KB output

        for (Memory memory : memories) {
            sb.append(memory.toContextString()).append("\n");
        }

        return sb.toString();
    }
}
```

#### 5. Thread Safety (Target: A 90/100)

**Current:** 72/100 (B-)
**Target:** 90/100 (A-)

**Key Characteristics:**
- **No race conditions** - All state changes atomic
- **Volatile visibility** - Shared state properly marked
- **Lock-free collections** - Concurrent collections everywhere
- **Proper resource cleanup** - Executors shut down
- **No compound actions** - Check-then-act eliminated
- **Tested concurrency** - Stress tests for race conditions

**Concrete Targets:**
```
Race conditions:           0 (currently 5 identified)
Non-volatile shared state: 0 (currently 6 fields)
Unbounded collections:    0 (currently 4)
Unclosed resources:       0 (currently 2 executors)
Compound operations:      0 (currently 8 check-then-act)
Thread-safe tests:        100% critical paths (currently 20%)
```

**Thread Safety Standards:**

```java
// STANDARD: Atomic compound operations
public class CollaborativeBuildManager {
    // BAD: Check-then-act race condition
    public Integer assignForemanToSection_OLD(Build build, String foremanName) {
        Integer sectionIndex = build.foremanToSectionMap.get(foremanName);
        if (sectionIndex == null) {
            sectionIndex = computeSectionIndex(build);  // Race here!
            build.foremanToSectionMap.put(foremanName, sectionIndex);
        }
        return sectionIndex;
    }

    // GOOD: Atomic operation with computeIfAbsent
    public Integer assignForemanToSection(Build build, String foremanName) {
        return build.foremanToSectionMap.computeIfAbsent(
            foremanName,
            k -> computeSectionIndex(build)  // Atomic computation
        );
    }
}

// STANDARD: Volatile for visibility
public class ActionExecutor {
    // BAD: Non-volatile shared state
    private boolean isPlanning = false;
    private String pendingCommand;

    // GOOD: Volatile for cross-thread visibility
    private volatile boolean isPlanning = false;
    private volatile String pendingCommand;
    private volatile CompletableFuture<ParsedResponse> planningFuture;
}

// STANDARD: Thread-safe collections
public class ActionExecutor {
    // BAD: LinkedList not thread-safe
    private final Queue<Task> taskQueue = new LinkedList<>();

    // GOOD: ConcurrentLinkedQueue for lock-free thread safety
    private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<>();
}

// STANDARD: Proper resource cleanup
public class EventBus implements AutoCloseable {
    private final ExecutorService executor;

    public EventBus() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "event-bus");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 6. Test Coverage (Target: A 85/100)

**Current:** 70/100 (C+)
**Target:** 85/100 (A-)

**Key Characteristics:**
- **70%+ line coverage** - Critical paths fully tested
- **Integration tests** - Real environment testing
- **Stress tests** - Concurrency validated
- **No flaky tests** - All tests deterministic
- **Fast execution** - Test suite <5 minutes
- **Clear organization** - Unit/integration/stress separated

**Concrete Targets:**
```
Line coverage:             70% (currently 40%)
Branch coverage:           60% (currently TBD)
Integration tests:        20 scenarios (currently 0)
Stress tests:              10 concurrency tests (currently 0)
Flaky tests:               0 (currently 29 identified)
Test execution time:       <5 minutes (currently TBD)
Tests compile successfully: 100% (currently 0% - 100 errors)
```

**Testing Standards:**

```java
// STANDARD: Test structure
class ActionExecutorTest {

    @BeforeEach
    void setUp() {
        // Arrange: Set up test fixtures
        executor = new ActionExecutor(mockForeman);
    }

    @Test
    @DisplayName("should complete action when all parameters are valid")
    void shouldCompleteActionWhenAllParametersAreValid() {
        // Arrange: Set up preconditions
        Task task = createValidTask();

        // Act: Execute the action
        executor.executeTask(task);
        waitForCompletion();

        // Assert: Verify outcomes
        assertTrue(executor.getCurrentAction().isComplete());
        assertEquals(ActionResult.success(), executor.getCurrentAction().getResult());
    }

    @Test
    @DisplayName("should handle invalid parameters gracefully")
    void shouldHandleInvalidParametersGracefully() {
        // Test error paths
        Task invalidTask = createInvalidTask();

        executor.executeTask(invalidTask);
        waitForCompletion();

        // Verify error handling
        assertTrue(executor.getCurrentAction().getResult().isFailure());
        assertNotNull(executor.getCurrentAction().getResult().getMessage());
    }
}

// STANDARD: Concurrency testing
@Test
@DisplayName("should handle concurrent planning cancellation")
void shouldHandleConcurrentPlanningCancellation() throws InterruptedException {
    ActionExecutor executor = new ActionExecutor(foreman);
    CountDownLatch startLatch = new CountDownLatch(1);
    int threadCount = 10;
    AtomicInteger successCount = new AtomicInteger(0);

    // Start multiple threads
    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                startLatch.await();  // Synchronize start
                executor.processCommand("test command");
                executor.stopCurrentAction();
                if (!executor.isPlanning()) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                // Test fails on exception
                fail("Unexpected exception: " + e);
            }
        }).start();
    }

    startLatch.countDown();  // Release all threads
    Thread.sleep(1000);  // Wait for completion

    // Only one thread should succeed
    assertEquals(1, successCount.get());
}
```

#### 7. Documentation (Target: A 90/100)

**Current:** 75/100 (B)
**Target:** 90/100 (A-)

**Key Characteristics:**
- **Single source of truth** - No duplicate content
- **Current and accurate** - All docs match code
- **Complete JavaDoc** - All public APIs documented
- **Clear diagrams** - Visual architecture representations
- **Usage examples** - Real-world code samples
- **Maintenance guide** - How to keep docs current

**Concrete Targets:**
```
Documentation files:      400 (currently 539) - 26% reduction
Duplicate content:        0 (currently ~500KB)
JavaDoc coverage:         100% public APIs (currently 61%)
Accuracy issues:          0 (currently 12 P0 + 25 P1)
Package documentation:    100% (currently 12%)
Broken links:             0 (currently ~100)
Diagrams:                 10 Mermaid diagrams (currently 0)
```

---

## Consolidated Action Plan

### Unified Priority Matrix

After consolidating all 6 audits, removing duplicates, and merging related items, we have **84 unique action items** organized by priority:

| Priority | Count | Total Time | Focus |
|----------|-------|------------|-------|
| **P0 - Critical** | 12 | 54 hours | Memory leaks, security, test compilation |
| **P1 - High** | 23 | 86 hours | Architecture, performance, thread safety |
| **P2 - Medium** | 31 | 120 hours | Documentation, test coverage, code quality |
| **P3 - Low** | 18 | 48 hours | Best practices, technical debt |
| **Total** | **84** | **308 hours** | Complete transformation |

### Phase 1: Critical Fixes (Week 1-2) - 54 hours

**Goal:** Eliminate critical vulnerabilities and restore test infrastructure

| Task | Category | Time | Dependencies |
|------|----------|------|--------------|
| 1. Fix CompletableFuture cleanup | Memory | 1h | None |
| 2. Fix EventBus subscription leaks | Memory | 1h | None |
| 3. Replace CopyOnWriteArrayList with PriorityQueue | Performance | 2h | None |
| 4. Add logging to empty catch blocks | Security | 2h | None |
| 5. Add input validation to async paths | Security | 1h | None |
| 6. Fix test compilation errors | Tests | 2h | None |
| 7. Fix CollaborativeBuildManager race condition | Thread Safety | 1h | None |
| 8. Add volatile to shared state flags | Thread Safety | 1h | None |
| 9. Replace taskQueue with ConcurrentLinkedQueue | Thread Safety | 1h | None |
| 10. Fix unbounded collections (4 instances) | Performance | 3h | None |
| 11. Replace string concatenation with StringBuilder | Performance | 3h | None |
| 12. Fix O(n²) emotional memory sorting | Performance | 2h | None |

**Deliverables:**
- ✅ No memory leaks
- ✅ All tests compile and run
- ✅ No race conditions in critical paths
- ✅ Production-ready security posture
- ✅ Performance hotspots eliminated

**Acceptance Criteria:**
- [ ] All tests pass (100% success rate)
- [ ] No O(n²) operations in hot paths
- [ ] Memory profiler shows stable usage over 1 hour
- [ ] Thread sanitizer reports zero data races
- [ ] Security scan shows zero critical vulnerabilities

### Phase 2: High-Priority Refactoring (Week 3-5) - 86 hours

**Goal:** Improve architecture, eliminate technical debt, enhance performance

| Task | Category | Time | Dependencies |
|------|----------|------|--------------|
| 1. Refactor ForemanEntity (extract subsystems) | Architecture | 16h | Phase 1 complete |
| 2. Refactor CompanionMemory (split into 4 classes) | Architecture | 12h | Phase 1 complete |
| 3. Refactor MineWrightConfig (split into 3 classes) | Architecture | 8h | Phase 1 complete |
| 4. Replace singletons with dependency injection | Architecture | 12h | Phase 1 complete |
| 5. Introduce repository pattern for persistence | Architecture | 8h | Phase 1 complete |
| 6. Extract Agent interface | Architecture | 4h | Phase 1 complete |
| 7. Implement command caching for LLM calls | Performance | 4h | Phase 1 complete |
| 8. Optimize context building (remove stream overhead) | Performance | 4h | Phase 1 complete |
| 9. Add embedding cache (LRU) | Performance | 2h | Phase 1 complete |
| 10. Precompute lowercase strings for memory search | Performance | 2h | Phase 1 complete |
| 11. Extract NBT serialization utility | Code Quality | 8h | Phase 1 complete |
| 12. Create abstract async LLM client base | Code Quality | 6h | Phase 1 complete |

**Deliverables:**
- ✅ Clean hexagonal architecture
- ✅ No god objects (all classes <500 lines)
- ✅ Dependency injection throughout
- ✅ Repository pattern for persistence
- ✅ 60-80% reduction in LLM API calls
- ✅ Eliminated code duplication

**Acceptance Criteria:**
- [ ] Max class size: 500 lines
- [ ] Max incoming dependencies: 20
- [ ] Singletons: 0
- [ ] Code duplication: <5%
- [ ] LLM cache hit rate: >60%
- [ ] Architecture audit score: 95/100

### Phase 3: Medium-Priority Improvements (Week 6-9) - 120 hours

**Goal:** Complete documentation, improve test coverage, polish code quality

| Task | Category | Time | Dependencies |
|------|----------|------|--------------|
| 1. Merge duplicate documentation (4 consolidations) | Documentation | 20h | Phase 2 complete |
| 2. Create package-info.java for all packages | Documentation | 12h | Phase 2 complete |
| 3. Add JavaDoc to undocumented public APIs | Documentation | 16h | Phase 2 complete |
| 4. Fix all broken internal links (~100) | Documentation | 8h | Phase 2 complete |
| 5. Create Mermaid diagrams (5 diagrams) | Documentation | 12h | Phase 2 complete |
| 6. Add integration tests (20 scenarios) | Tests | 24h | Phase 1 complete |
| 7. Add stress tests (10 concurrency tests) | Tests | 12h | Phase 1 complete |
| 8. Fix flaky tests (29 tests) | Tests | 8h | Phase 1 complete |
| 9. Improve test data management (factories) | Code Quality | 8h | Phase 1 complete |
| 10. Extract GUI widget library | Code Quality | 12h | Phase 2 complete |
| 11. Split remaining large files | Code Quality | 20h | Phase 2 complete |

**Deliverables:**
- ✅ Single source of truth for documentation
- ✅ 100% JavaDoc coverage on public APIs
- ✅ 70%+ test coverage
- ✅ Integration test suite
- ✅ No flaky tests
- ✅ Clear visual diagrams

**Acceptance Criteria:**
- [ ] Documentation files: 400 (26% reduction)
- [ ] Duplicate content: 0
- [ ] JavaDoc coverage: 100%
- [ ] Test coverage: 70%
- [ ] Flaky tests: 0
- [ ] Broken links: 0

### Phase 4: Low-Priority Polish (Week 10+) - 48 hours

**Goal:** Address technical debt, implement best practices

| Task | Category | Time | Dependencies |
|------|----------|------|--------------|
| 1. Add @SuppressWarnings explanations | Code Quality | 2h | Phase 3 complete |
| 2. Fix naming inconsistencies | Code Quality | 4h | Phase 3 complete |
| 3. Reorganize packages (split mixed concerns) | Code Quality | 3h | Phase 3 complete |
| 4. Add final modifiers to immutable fields | Code Quality | 6h | Phase 3 complete |
| 5. Move demo classes to test module | Code Quality | 2h | Phase 3 complete |
| 6. Implement command pattern | Architecture | 8h | Phase 2 complete |
| 7. Add API layer (facades) | Architecture | 8h | Phase 2 complete |
| 8. Add CQRS pattern (optional) | Architecture | 12h | Phase 2 complete |
| 9. Performance benchmarks | Performance | 6h | Phase 2 complete |
| 10. Documentation maintenance guide | Documentation | 4h | Phase 3 complete |

**Deliverables:**
- ✅ Consistent naming conventions
- ✅ Clean package organization
- ✅ Command pattern for actions
- ✅ API layer for external access
- ✅ Performance regression tests
- ✅ Maintenance processes documented

**Acceptance Criteria:**
- [ ] Naming inconsistencies: 0
- [ ] Mixed package concerns: 0
- [ ] Performance benchmarks: 10 critical paths
- [ ] Maintenance guide: Complete

---

## Architecture Target

### Clean Hexagonal Architecture

The target architecture follows **hexagonal architecture principles** with clear separation between domain, application, and infrastructure layers.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Interface Layer (Web/CLI)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │   REST API   │  │  CLI Command │  │ Forge Events │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ Planning     │  │  Execution   │  │Coordination  │        │
│  │  Service     │  │  Service     │  │  Service     │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Domain Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │    Agent     │  │   Command    │  │   Memory     │        │
│  │   (Entity)   │  │  (Entity)    │  │  (Entity)    │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ depends on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │Persistence   │  │  LLM Clients │  │  Minecraft   │        │
│  │(NBT/Files)   │  │ (HTTP/Cache) │  │  Adapter     │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

### Dependency Rules

**Strict Layering:**
1. **Domain** has NO dependencies on other layers
2. **Application** depends only on Domain
3. **Infrastructure** depends only on Domain
4. **Interface** depends on Application and Infrastructure

**Allowed Dependencies:**
```
Interface → Application → Domain ← Infrastructure
```

**Forbidden Dependencies:**
```
Domain → Anything (domain is core, no dependencies)
Infrastructure → Application (no business logic in infrastructure)
Interface → Domain (use Application as facade)
```

### Package Structure Target

```
com.minewright/
├── api/                        # External API layer
│   ├── rest/                   # REST controllers (future)
│   ├── cli/                    # Command handlers
│   └── forge/                  # Forge event handlers
│       └── ForemanEntity.java  # Thin adapter to domain
├── application/                # Application services
│   ├── planning/
│   │   ├── PlanningService.java
│   │   └── TaskPlanner.java
│   ├── execution/
│   │   └── ExecutionService.java
│   └── coordination/
│       └── CoordinationService.java
├── domain/                     # Core domain (no dependencies!)
│   ├── agent/
│   │   ├── Agent.java          # Interface
│   │   ├── AgentController.java
│   │   └── AgentSubsystem.java
│   ├── command/
│   │   ├── Command.java
│   │   └── CommandResult.java
│   ├── memory/
│   │   ├── Memory.java
│   │   └── MemoryRepository.java  # Interface only
│   └── model/
│       ├── Task.java
│       └── ActionResult.java
├── infrastructure/             # External integrations
│   ├── persistence/
│   │   ├── NBTRepository.java
│   │   └── FileRepository.java
│   ├── llm/
│   │   ├── OpenAIClient.java
│   │   └── GroqClient.java
│   └── minecraft/
│       ├── EntityAdapter.java
│       └── WorldAdapter.java
└── test/                       # Test utilities
    └── testutil/
        ├── MockMinecraftServer.java
        └── TestDataFactory.java
```

### Design Pattern Guidelines

#### Repository Pattern (NEW)

**Purpose:** Abstract persistence from domain

```java
// Domain layer: Interface only
public interface EntityRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
}

// Infrastructure layer: Implementation
public class ForemanEntityRepository implements EntityRepository<ForemanEntity, String> {
    @Override
    public CompoundTag save(ForemanEntity entity) {
        CompoundTag tag = new CompoundTag();
        tag.putString("entityName", entity.getEntityName());
        // Save other fields...
        return tag;
    }

    @Override
    public ForemanEntity load(CompoundTag tag) {
        String name = tag.getString("entityName");
        // Reconstruct entity...
        return new ForemanEntity(...);
    }
}

// Usage in domain
public class AgentService {
    private final EntityRepository<Agent, String> repository;

    public void saveAgent(Agent agent) {
        repository.save(agent);
    }
}
```

#### Dependency Injection (REPLACE SINGLETONS)

**Purpose:** Improve testability and clarity

```java
// BAD: Singleton
ActionRegistry registry = ActionRegistry.getInstance();

// GOOD: Constructor injection
public class ActionExecutor {
    private final ActionRegistry registry;
    private final EventBus eventBus;

    @Inject
    public ActionExecutor(
        ForemanEntity foreman,
        ActionRegistry registry,
        EventBus eventBus
    ) {
        this.foreman = foreman;
        this.registry = Objects.requireNonNull(registry);
        this.eventBus = Objects.requireNonNull(eventBus);
    }
}

// DI container configuration
public class AgentDIContainer {
    private final ServiceContainer container;

    public AgentDIContainer() {
        this.container = new SimpleServiceContainer();

        // Register services
        container.register(EventBus.class, new SimpleEventBus());
        container.register(ActionRegistry.class, new ActionRegistry());
        container.register(TaskPlanner.class, new TaskPlanner(
            container.getService(EventBus.class),
            container.getService(ActionRegistry.class)
        ));
    }

    public <T> T getService(Class<T> type) {
        return container.getService(type);
    }
}
```

#### Command Pattern (NEW)

**Purpose:** Encapsulate actions as objects

```java
// Command interface
public interface Command {
    CommandResult execute(Context context);
    boolean canExecute(Context context);
    void undo(Context context);
}

// Concrete command
public class MineBlockCommand implements Command {
    private final BlockType blockType;
    private final int quantity;

    public MineBlockCommand(BlockType blockType, int quantity) {
        this.blockType = blockType;
        this.quantity = quantity;
    }

    @Override
    public CommandResult execute(Context context) {
        // Execute mining logic
        return CommandResult.success("Mined " + quantity + " " + blockType);
    }

    @Override
    public boolean canExecute(Context context) {
        // Validate preconditions
        return context.hasTool() && context.hasNearbyBlocks(blockType);
    }

    @Override
    public void undo(Context context) {
        // Undo logic (if applicable)
    }
}

// Command executor
public class CommandExecutor {
    public CommandResult execute(Command command, Context context) {
        if (!command.canExecute(context)) {
            return CommandResult.failure("Cannot execute command");
        }
        return command.execute(context);
    }
}
```

---

## Quality Standards

### Code Quality Standards

#### File Size Standards

**Rules:**
- Max class size: 500 lines
- Max method size: 50 lines
- Max parameter count: 5 parameters
- Max nesting depth: 4 levels

**Enforcement:**
```java
// BAD: Class too large (1,890 lines)
public class CompanionMemory {
    // 50+ fields
    // 100+ methods
}

// GOOD: Split into focused classes
public class CompanionMemory {
    private final MemoryStore store;
    private final PersonalitySystem personality;
    private final RelationshipTracker relationships;
    // 20 coordination methods
}

public class MemoryStore {
    // Focused on storage only
    // ~400 lines
}

public class PersonalitySystem {
    // Focused on personality
    // ~300 lines
}
```

#### Method Complexity Standards

**Rules:**
- Max cyclomatic complexity: 10
- Max nesting depth: 4 levels
- Max return statements: 3
- Max exceptions thrown: 3

**Enforcement:**
```java
// BAD: Too complex (complexity > 10)
public void complexMethod(Input input) {
    if (condition1) {
        if (condition2) {
            if (condition3) {
                if (condition4) {
                    // Too deep!
                }
            }
        }
    }
}

// GOOD: Extract to helper methods
public void complexMethod(Input input) {
    if (shouldProcess(input)) {
        processInput(input);
    }
}

private boolean shouldProcess(Input input) {
    return input.isValid() && hasCapacity() && isAuthorized(input);
}

private void processInput(Input input) {
    // Process logic
}
```

#### Naming Standards

**Classes:** PascalCase, nouns or noun phrases
```java
// GOOD
ActionExecutor, TaskPlanner, CompanionMemory

// BAD
actionExecutor, taskPlanner, DoSomething
```

**Methods:** camelCase, verbs or verb phrases
```java
// GOOD
executeTask(), planTasks(), recordExperience()

// BAD
ExecuteTask(), task(), data()
```

**Constants:** UPPER_SNAKE_CASE
```java
// GOOD
private static final int MAX_QUEUE_SIZE = 1000;
private static final String DEFAULT_NAME = "Steve";

// BAD
private static final int maxSize = 1000;
private static final String name = "Steve";
```

#### Encapsulation Standards

**Rules:**
- All fields private (except constants)
- Public getters/setters only when needed
- Immutable classes where possible
- Defensive copies on mutable returns

```java
// GOOD: Proper encapsulation
public class Agent {
    private final String id;
    private volatile State state;
    private final List<Task> tasks = new ArrayList<>();

    public String getId() {
        return id;  // Immutable, safe to return
    }

    public State getState() {
        return state;  // Volatile for visibility
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);  // Defensive copy
    }
}

// BAD: Exposed mutable state
public class Agent {
    public List<Task> tasks;  // Direct access!

    public List<Task> getTasks() {
        return tasks;  // Returns mutable list!
    }
}
```

### Documentation Standards

#### JavaDoc Standards

**Required JavaDoc:**
- All public classes
- All public methods
- All protected methods
- All record classes
- All enum types

**Format:**
```java
/**
 * Brief description of what the class/method does.
 *
 * <p>Additional details about usage, behavior, or implementation.
 * Can span multiple paragraphs.
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * ActionExecutor executor = new ActionExecutor(foreman);
 * executor.processCommand("mine diamond");
 * }</pre>
 *
 * @see RelatedClass
 * @since 1.0
 * @author Author Name
 */
public class ActionExecutor {
    /**
     * Processes a natural language command and generates a task plan.
     *
     * <p>This method validates the command, checks for suspicious patterns,
     * and sends it to the LLM for planning. The planning happens asynchronously
     * to avoid blocking the game thread.
     *
     * @param command the natural language command to process (must not be null)
     * @return CompletableFuture that completes with the parsed response
     * @throws IllegalArgumentException if command is null or empty
     * @throws IllegalStateException if API key is not configured
     * @see InputSanitizer#containsSuspiciousPatterns(String)
     * @since 1.0
     */
    public CompletableFuture<ParsedResponse> processCommand(String command) {
        // Implementation...
    }
}
```

#### Package Documentation Standards

**Required for all packages:**

```java
/**
 * Provides action execution and task planning capabilities.
 *
 * <p>This package contains the core action execution system including:
 * <ul>
 *   <li>ActionExecutor - Coordinates task execution</li>
 *   <li>ActionResult - Represents execution outcomes</li>
 *   <li>ActionContext - Provides execution context</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ActionExecutor executor = new ActionExecutor(foreman);
 * executor.processCommand("build house");
 * }</pre>
 *
 * @since 1.0
 */
package com.minewright.action;
```

### Security Standards

#### Input Validation Standards

**All entry points must validate:**
1. Null checks
2. Length limits
3. Format validation
4. Suspicious pattern detection
5. SQL/script injection prevention

```java
// STANDARD: Comprehensive input validation
public class TaskPlanner {
    private static final int MAX_COMMAND_LENGTH = 1000;

    public CompletableFuture<ParsedResponse> planTasksAsync(
        ForemanEntity foreman,
        String command
    ) {
        // 1. Null check
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }

        // 2. Length check
        if (command.length() > MAX_COMMAND_LENGTH) {
            LOGGER.warn("Command too long: {} chars", command.length());
            return CompletableFuture.completedFuture(null);
        }

        // 3. Suspicious pattern check
        if (InputSanitizer.containsSuspiciousPatterns(command)) {
            String reason = InputSanitizer.getSuspiciousPatternDescription(command);
            LOGGER.warn("Command rejected: {}. Command: {}", reason, command);
            return CompletableFuture.completedFuture(null);
        }

        // 4. Format validation (if applicable)
        if (!isValidCommandFormat(command)) {
            LOGGER.warn("Invalid command format: {}", command);
            return CompletableFuture.completedFuture(null);
        }

        // 5. Process validated command
        return doPlanTasksAsync(foreman, command);
    }
}
```

#### Error Handling Standards

**Never silently catch exceptions:**

```java
// BAD: Empty catch block
try {
    riskyOperation();
} catch (Exception e) {
    // Error silently ignored
}

// GOOD: Log and handle
try {
    riskyOperation();
} catch (SpecificException e) {
    LOGGER.warn("Operation failed: {}", e.getMessage(), e);
    // Fallback behavior
    fallbackOperation();
} catch (Exception e) {
    LOGGER.error("Unexpected error in operation", e);
    throw new OperationFailedException("Operation failed", e);
}
```

#### Logging Standards

**Never log sensitive data:**

```java
// BAD: Logs partial API key
LOGGER.info("API key configured: {}", apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4));

// GOOD: Only logs presence
LOGGER.info("API key configured ({} chars)", apiKey.length());

// BAD: Logs full user input
LOGGER.info("Processing command: {}", command);

// GOOD: Logs sanitized input
LOGGER.info("Processing command ({} chars)", command.length());
```

### Performance Standards

#### Hot Path Optimization

**Identify and optimize hot paths:**
- Methods called every tick (20 TPS)
- Methods called for every entity
- Methods in tight loops

**Standards:**
```java
// BAD: O(n) operation in hot path
public void tick() {
    for (Entity entity : entities) {  // 100+ entities
        if (entitiesToRemove.contains(entity)) {  // O(n) contains!
            remove(entity);
        }
    }
}

// GOOD: O(1) lookup in hot path
private final Set<Entity> entitiesToRemove = ConcurrentHashMap.newKeySet();

public void tick() {
    for (Entity entity : entities) {
        if (entitiesToRemove.remove(entity)) {  // O(1) remove!
            handleRemoval(entity);
        }
    }
}
```

#### Memory Allocation Standards

**Minimize allocations in hot paths:**
```java
// BAD: Creates new objects every tick
public void tick() {
    String result = "Processing " + entity.getName() + " at " + entity.getPosition();
    LOGGER.debug(result);
}

// GOOD: Reuses objects, uses StringBuilder
private final StringBuilder logBuffer = new StringBuilder(256);

public void tick() {
    logBuffer.setLength(0);  // Clear buffer
    logBuffer.append("Processing ")
              .append(entity.getName())
              .append(" at ")
              .append(entity.getPosition());
    LOGGER.debug(logBuffer.toString());
}
```

### Thread Safety Standards

#### Volatile for Visibility

**Shared state must be volatile:**
```java
// GOOD: Volatile for cross-thread visibility
public class ActionExecutor {
    private volatile boolean isPlanning = false;
    private volatile String pendingCommand;
    private volatile CompletableFuture<ParsedResponse> planningFuture;
}
```

#### Atomic Operations

**Use atomic operations for compound actions:**
```java
// BAD: Check-then-act race condition
public void incrementIfBelow(int max) {
    if (counter < max) {  // Race here!
        counter++;
    }
}

// GOOD: Atomic operation
private final AtomicInteger counter = new AtomicInteger(0);

public void incrementIfBelow(int max) {
    counter.updateAndGet(current ->
        current < max ? current + 1 : current
    );
}
```

#### Thread-Safe Collections

**Use appropriate concurrent collections:**
```java
// GOOD: Concurrent collections for shared state
private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<>();
private final Map<String, Agent> agents = new ConcurrentHashMap<>();
private final List<Listener> listeners = new CopyOnWriteArrayList<>();
```

---

## Success Metrics

### Overall Success Criteria

The unified vision is achieved when **all** of the following criteria are met:

#### Architecture Success (Target: 95/100)

- [ ] Max class size: 500 lines (currently 1,890)
- [ ] Max incoming dependencies: 20 (currently 59)
- [ ] Singletons: 0 (currently 8+)
- [ ] Repository interfaces: 100% (currently 0%)
- [ ] Hexagonal architecture: Implemented (currently partial)
- [ ] God objects: 0 (currently 3)

#### Code Quality Success (Target: 90/100)

- [ ] Files >500 lines: 0 (currently 12)
- [ ] Code duplication: <5% (currently ~15%)
- [ ] JavaDoc coverage: 100% public APIs (currently 61%)
- [ ] System.out usage: 0 files (currently 15)
- [ ] Empty catch blocks: 0 (currently 30+)
- [ ] Cyclomatic complexity <10: 100% methods (currently TBD)

#### Security Success (Target: 95/100)

- [ ] Empty catch blocks: 0 (currently 30+)
- [ ] Input validation coverage: 100% entry points (currently 90%)
- [ ] API key exposure: 0 partial keys logged (currently partial)
- [ ] Rate limiting: Implemented (currently not)
- [ ] Unbounded collections: 0 (currently 4)
- [ ] Memory leaks: 0 (currently 3)
- [ ] Security scan: 0 critical vulnerabilities (currently 2)

#### Performance Success (Target: 90/100)

- [ ] Memory recording time: <2ms (currently 5-10ms)
- [ ] Memory search time: <10ms (currently 20-50ms)
- [ ] LLM cache hit rate: >60% (currently 0%)
- [ ] Memory per agent: <30MB (currently ~50MB)
- [ ] Tick time (p99): <5ms (currently 8ms)
- [ ] O(n²) operations: 0 (currently 3)
- [ ] Unbounded collections: 0 (currently 4)

#### Thread Safety Success (Target: 90/100)

- [ ] Race conditions: 0 (currently 5 identified)
- [ ] Non-volatile shared state: 0 (currently 6 fields)
- [ ] Unclosed resources: 0 (currently 2 executors)
- [ ] Compound operations: 0 (currently 8 check-then-act)
- [ ] Thread-safe tests: 100% critical paths (currently 20%)
- [ ] Thread sanitizer: 0 data races (currently TBD)

#### Test Coverage Success (Target: 85/100)

- [ ] Line coverage: 70% (currently 40%)
- [ ] Integration tests: 20 scenarios (currently 0)
- [ ] Stress tests: 10 concurrency tests (currently 0)
- [ ] Flaky tests: 0 (currently 29)
- [ ] Test execution time: <5 minutes (currently TBD)
- [ ] Test compilation: 100% success (currently 0% - 100 errors)

#### Documentation Success (Target: 90/100)

- [ ] Documentation files: 400 (26% reduction from 539)
- [ ] Duplicate content: 0 (currently ~500KB)
- [ ] JavaDoc coverage: 100% public APIs (currently 61%)
- [ ] Accuracy issues: 0 (currently 12 P0 + 25 P1)
- [ ] Package documentation: 100% (currently 12%)
- [ ] Broken links: 0 (currently ~100)
- [ ] Diagrams: 10 Mermaid diagrams (currently 0)

### Progress Tracking

#### Weekly Metrics Dashboard

**Update weekly during standup:**

```
Week: ___/___

P0 Progress: ___/12 tasks (___%)
  - Memory leaks: ___/3
  - Security: ___/2
  - Tests: ___/1
  - Performance: ___/4
  - Thread Safety: ___/2

P1 Progress: ___/23 tasks (___%)
  - Architecture: ___/6
  - Performance: ___/4
  - Code Quality: ___/8
  - Thread Safety: ___/5

Quality Metrics:
  - Architecture Score: ___/100 (target: 95)
  - Code Quality Score: ___/100 (target: 90)
  - Security Score: ___/100 (target: 95)
  - Performance Score: ___/100 (target: 90)
  - Thread Safety Score: ___/100 (target: 90)
  - Test Coverage Score: ___/100 (target: 85)
  - Documentation Score: ___/100 (target: 90)

Overall Progress: ___/84 tasks (___%)
Time Invested: ___ hours (target: 308 hours)
```

#### Definition of Done

**Each P0 item is "done" when:**
- [ ] Code changes implemented
- [ ] Unit tests added/updated
- [ ] Code review approved
- [ ] Documentation updated
- [ ] No new issues introduced
- [ ] Performance validated (no regression)
- [ ] Security validated (no vulnerabilities)

**Each phase is "done" when:**
- [ ] All items in phase completed
- [ ] Integration tests pass
- [ ] No critical bugs remaining
- [ ] Performance benchmarks met
- [ ] Documentation complete
- [ ] Demo/walkthrough given to team

---

## Implementation Timeline

### 10-Week Transformation Plan

#### Week 1-2: Critical Fixes (54 hours)

**Goal:** Eliminate critical vulnerabilities and restore test infrastructure

**Deliverables:**
- No memory leaks
- All tests compile and run
- No race conditions in critical paths
- Production-ready security posture
- Performance hotspots eliminated

**Ceremony:**
- Daily standups (15 min)
- End-of-week demo (30 min)
- Code review for all changes
- Update metrics dashboard

**Week 1 Checklist:**
- [ ] Fix CompletableFuture cleanup (1h)
- [ ] Fix EventBus subscription leaks (1h)
- [ ] Replace CopyOnWriteArrayList with PriorityQueue (2h)
- [ ] Add logging to empty catch blocks (2h)
- [ ] Add input validation to async paths (1h)
- [ ] Fix test compilation errors (2h)
- [ ] Fix CollaborativeBuildManager race condition (1h)
- [ ] Add volatile to shared state flags (1h)

**Week 2 Checklist:**
- [ ] Replace taskQueue with ConcurrentLinkedQueue (1h)
- [ ] Fix unbounded collections - 4 instances (3h)
- [ ] Replace string concatenation with StringBuilder (3h)
- [ ] Fix O(n²) emotional memory sorting (2h)
- [ ] Memory leak validation tests (2h)
- [ ] Thread sanitizer integration (2h)
- [ ] Performance baseline benchmarks (2h)
- [ ] End-of-phase demo and metrics update

#### Week 3-5: High-Priority Refactoring (86 hours)

**Goal:** Improve architecture, eliminate technical debt, enhance performance

**Deliverables:**
- Clean hexagonal architecture
- No god objects (all classes <500 lines)
- Dependency injection throughout
- Repository pattern for persistence
- 60-80% reduction in LLM API calls

**Ceremony:**
- Daily standups (15 min)
- Mid-phase architecture review (1 hour)
- End-of-week demos (30 min)
- Code review for all changes

**Week 3 Checklist:**
- [ ] Refactor ForemanEntity (16h)
  - Extract AgentController
  - Extract subsystems
  - Reduce to <300 lines
- [ ] Begin CompanionMemory refactoring (6h)

**Week 4 Checklist:**
- [ ] Complete CompanionMemory refactoring (12h total)
  - Extract MemoryStore
  - Extract PersonalitySystem
  - Extract RelationshipTracker
  - Extract NBTSerializer
- [ ] Refactor MineWrightConfig (8h)
  - Extract ConfigSpecs
  - Extract ConfigValidator
  - Extract ConfigDefaults

**Week 5 Checklist:**
- [ ] Replace singletons with dependency injection (12h)
  - Create AgentDIContainer
  - Inject dependencies via constructor
  - Remove getInstance() calls
- [ ] Introduce repository pattern (8h)
  - Create EntityRepository interface
  - Implement NBTRepository
  - Use in entity classes
- [ ] Extract Agent interface (4h)
- [ ] Implement command caching for LLM calls (4h)
- [ ] Optimize context building (4h)
- [ ] Add embedding cache (2h)
- [ ] Precompute lowercase strings (2h)
- [ ] End-of-phase demo and metrics update

#### Week 6-9: Medium-Priority Improvements (120 hours)

**Goal:** Complete documentation, improve test coverage, polish code quality

**Deliverables:**
- Single source of truth for documentation
- 100% JavaDoc coverage on public APIs
- 70%+ test coverage
- Integration test suite
- No flaky tests
- Clear visual diagrams

**Ceremony:**
- Daily standups (15 min)
- Bi-weekly documentation reviews (30 min)
- End-of-week demos (30 min)
- Code review for all changes

**Week 6-7 Checklist:**
- [ ] Merge duplicate documentation (20h)
  - Architecture docs: 3 → 1
  - Skill system docs: 6 → 2
  - Audit docs: 17 → 1
  - Pathfinding docs: 3 → 1
- [ ] Create package-info.java (12h)
- [ ] Add JavaDoc to undocumented APIs (16h)
- [ ] Fix broken internal links (8h)

**Week 8 Checklist:**
- [ ] Create Mermaid diagrams (12h)
  - System architecture diagram
  - Component interaction diagram
  - State machine diagram
  - Data flow diagram
  - Skill composition diagram
- [ ] Add integration tests (24h)
  - Real Minecraft server testing
  - End-to-end scenarios
  - Multi-agent coordination
- [ ] Add stress tests (12h)
  - Concurrency stress tests
  - Load tests
  - Memory leak tests

**Week 9 Checklist:**
- [ ] Fix flaky tests (8h)
- [ ] Improve test data management (8h)
- [ ] Extract GUI widget library (12h)
- [ ] Split remaining large files (20h)
- [ ] End-of-phase demo and metrics update

#### Week 10+: Low-Priority Polish (48 hours)

**Goal:** Address technical debt, implement best practices

**Deliverables:**
- Consistent naming conventions
- Clean package organization
- Command pattern for actions
- API layer for external access
- Performance regression tests

**Ceremony:**
- Weekly standups (15 min)
- End-of-sprint demos (30 min)
- Final retrospective (1 hour)

**Week 10+ Checklist:**
- [ ] Add @SuppressWarnings explanations (2h)
- [ ] Fix naming inconsistencies (4h)
- [ ] Reorganize packages (3h)
- [ ] Add final modifiers (6h)
- [ ] Move demo classes to test module (2h)
- [ ] Implement command pattern (8h)
- [ ] Add API layer (8h)
- [ ] Add CQRS pattern (12h)
- [ ] Performance benchmarks (6h)
- [ ] Documentation maintenance guide (4h)
- [ ] Final demo and retrospective

### Risk Mitigation

#### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **Refactoring breaks existing functionality** | Medium | High | Comprehensive test coverage before refactoring |
| **Performance optimizations introduce bugs** | Low | Medium | Benchmark before/after, gradual rollout |
| **Test infrastructure changes delay development** | Low | Low | Parallel work streams, prioritize critical tests |
| **Team context switching overhead** | Medium | Medium | Focus on one phase at a time, complete before moving on |

#### Timeline Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **Underestimated effort** | Medium | Medium | 20% time buffer per phase, adjust priorities |
| **Critical bugs discovered during refactoring** | Low | High | P0 fixes first, regression testing |
| **Team availability changes** | Low | Medium | Cross-train team members, document thoroughly |

### Success Criteria by Phase

**Phase 1 Success (Week 2):**
- All 12 P0 items completed
- All tests compile and pass
- Memory profiler shows stable usage
- Thread sanitizer reports zero data races
- Security scan shows zero critical vulnerabilities

**Phase 2 Success (Week 5):**
- All 23 P1 items completed
- Max class size: 500 lines
- Singletons: 0
- LLM cache hit rate: >60%
- Architecture audit score: 95/100

**Phase 3 Success (Week 9):**
- All 31 P2 items completed
- Documentation files: 400 (26% reduction)
- Test coverage: 70%
- JavaDoc coverage: 100%
- Flaky tests: 0

**Phase 4 Success (Week 10+):**
- All 18 P3 items completed
- Naming inconsistencies: 0
- Performance benchmarks: 10 critical paths
- Maintenance guide: Complete

**Overall Success (Week 10+):**
- All 84 items completed
- Composite score: 90/100 (A-)
- Zero critical vulnerabilities
- Production-ready codebase
- Team satisfied with improvements

---

## Conclusion

This unified vision document consolidates findings from 6 comprehensive audits into **one clear direction** for the Steve AI project. It eliminates duplicate priorities, resolves contradictions, and provides a single source of truth for project improvement.

### Key Takeaways

1. **Strong Foundation** - The project has excellent architecture, documentation, and testing foundations
2. **Clear Issues** - 84 identified issues, organized by priority
3. **Achievable Goals** - 308 hours (~10 weeks focused work) to reach A- grade
4. **Measurable Progress** - Concrete metrics for each category
5. **Actionable Plan** - Week-by-week implementation timeline

### Next Steps

1. **Review this document** with the team
2. **Assign P0 tasks** to team members (Week 1-2)
3. **Set up metrics dashboard** for tracking progress
4. **Begin Phase 1** - Critical Fixes
5. **Weekly standups** to track progress against plan

### Success Vision

**By completing this unified vision, Steve AI will achieve:**
- A-grade architecture (95/100) with clean hexagonal design
- A-grade code quality (90/100) with no god objects
- A-grade security (95/100) with zero critical vulnerabilities
- A-grade performance (90/100) with optimized hot paths
- A-grade thread safety (90/100) with zero race conditions
- A-grade test coverage (85/100) with comprehensive suite
- A-grade documentation (90/100) with single source of truth

**Overall Target Grade: A- (90/100)**

This represents a transformation from "good" to "excellent," positioning Steve AI as a best-in-class AI-powered Minecraft mod.

---

**Document Status:** ACTIVE - Single source of truth
**Last Updated:** 2026-03-03
**Next Review:** 2026-03-10 (weekly during implementation)
**Maintained By:** Project Leadership
**Version:** 1.0

---

## Appendix A: Audit Sources

This document consolidates findings from:

1. **Architecture Audit** (docs/audits/ARCHITECTURE_AUDIT.md)
   - Package dependency analysis
   - Design pattern inventory
   - Architectural concerns (god objects, coupling)
   - Refactoring recommendations

2. **Code Quality Audit** (docs/audits/CODE_QUALITY_AUDIT.md)
   - Code complexity hotspots
   - Duplicate code identification
   - Naming and convention issues
   - API design problems
   - Java best practices compliance

3. **Security Audit** (docs/audits/SECURITY_AUDIT.md)
   - Input validation gaps
   - Resource management issues
   - Data safety concerns
   - Error handling problems
   - Security best practices

4. **Performance Audit** (docs/audits/PERFORMANCE_AUDIT.md)
   - Algorithmic efficiency issues
   - Memory usage problems
   - I/O and database concerns
   - Concurrency bottlenecks
   - LLM integration optimization

5. **Thread Safety Audit** (docs/audits/THREAD_SAFETY_AUDIT.md)
   - Race condition identification
   - Concurrent modification issues
   - Compound operation problems
   - Resource leak detection
   - Thread safety best practices

6. **Test Coverage Audit** (docs/audits/TEST_COVERAGE_AUDIT.md)
   - Coverage gaps analysis
   - Test quality assessment
   - Integration test needs
   - Test compilation issues
   - Mock usage problems

7. **Documentation Audit** (docs/audits/DOCUMENTATION_AUDIT.md)
   - Content inventory and organization
   - Accuracy and consistency analysis
   - Completeness gaps
   - Redundancy identification
   - Quality standards

8. **Action Execution Bugs Audit** (docs/audits/ACTION_EXECUTION_BUGS.md)
   - Memory leak identification
   - Thread safety issues
   - Resource leak detection
   - Edge case analysis
   - Bug fix recommendations

**Consolidation Method:**
- Merged duplicate issues across audits
- Resolved contradictions between audits
- Prioritized by severity and impact
- Unified action plan with dependencies
- Single source of truth for project direction

---

## Appendix B: Glossary

**Terms used in this document:**

- **God Object** - A class that knows too much or does too much (typically >500 lines)
- **CopyOnWriteArrayList** - Thread-safe list that copies entire array on each write
- **CompletableFuture** - Java's future/promise abstraction for async operations
- **Hexagonal Architecture** - Architectural style with clear layer separation
- **Repository Pattern** - Abstraction layer between domain and persistence
- **Dependency Injection** - Pattern where dependencies are provided rather than created
- **Race Condition** - Undesired behavior due to timing of concurrent operations
- **Cyclomatic Complexity** - Measure of code complexity based on control flow
- **O(n²)** - Quadratic time complexity (nested loops over same data)
- **Defensive Copy** - Returning a copy of mutable data to prevent modification
- **Volatile** - Java keyword ensuring visibility of changes across threads
- **Atomic Reference** - Thread-safe variable that supports atomic operations
- **NBT** - Named Binary Tag (Minecraft's serialization format)
- **LLM** - Large Language Model (GPT-4, Claude, etc.)
- **Three-Layer Architecture** - Brain (LLM) → Script (BT/HTN) → Physical (Actions)
- **P0/P1/P2/P3** - Priority levels (Critical/High/Medium/Low)
- **TPS** - Ticks Per Second (Minecraft runs at 20 TPS)
- **Check-then-act** - Race condition pattern where check and action are not atomic

---

## Appendix C: Quick Reference

### Critical File Locations

**Files to Split (P0):**
- `src/main/java/com/minewright/memory/CompanionMemory.java` (1,890 lines)
- `src/main/java/com/minewright/entity/ForemanEntity.java` (1,242 lines)
- `src/main/java/com/minewright/config/MineWrightConfig.java` (1,730 lines)

**Files to Fix (P0):**
- `src/main/java/com/minewright/observability/TracingConfig.java:47` (empty catch)
- `src/main/java/com/minewright/action/ActionExecutor.java:398` (memory leak)
- `src/main/java/com/minewright/action/CollaborativeBuildManager.java:181` (race condition)
- `src/main/java/com/minewright/memory/CompanionMemory.java:180` (O(n²) sort)

**Test Files (P0):**
- `src/test/java/com/minewright/llm/async/AsyncOpenAIClientTest.java` (compilation)
- `src/test/java/com/minewright/llm/async/AsyncGroqClientTest.java` (compilation)

### Quick Commands

```bash
# Build and test
./gradlew clean build test

# Run specific test
./gradlew test --tests ActionExecutorTest

# Check code coverage
./gradlew test jacocoTestReport

# Run security scan
./gradlew spotbugsMain

# Run performance benchmarks
./gradlew benchmark

# Generate documentation
./gradlew javadoc

# Format code
./gradlew spotlessApply

# Check style
./gradlew checkstyleMain
```

### Important Metrics

**Current (2026-03-03):**
- Source files: 326
- Source lines: 115,920
- Test files: 122
- Test lines: 77,312
- Documentation files: 539
- Documentation lines: 671,982
- Overall score: 82/100 (B+)

**Target (2026-05-03):**
- Overall score: 90/100 (A-)
- Architecture: 95/100 (A)
- Code Quality: 90/100 (A-)
- Security: 95/100 (A)
- Performance: 90/100 (A-)
- Thread Safety: 90/100 (A-)
- Test Coverage: 85/100 (A-)
- Documentation: 90/100 (A-)

---

**END OF UNIFIED VISION DOCUMENT**

This document is **THE source of truth** for Steve AI project direction. All other prioritization documents should be considered superseded by this unified vision. Update this document as progress is made and new information emerges.

**Remember:** One vision, one direction, one path to excellence.
