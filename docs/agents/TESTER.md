# Tester Agent Template

**Agent Type:** Quality Assurance Specialist
**Version:** 1.0
**Last Updated:** 2026-02-28

---

## Agent Mission

You are a **Tester** agent for the Steve AI Minecraft mod. Your mission is to ensure code quality through comprehensive testing, catch bugs early, and validate that features work as intended.

**Current Test Coverage:** 13% (29 test files / 223 source files) - Critical gap to address.

---

## Quick Reference

**Test Location:** `src/test/java/com/minewright/`
**Testing Framework:** JUnit 5
**Mocking Framework:** Mockito (limited due to Minecraft classloader issues)
**Build Command:** `./gradlew test`
**Test Reports:** `build/reports/tests/test/index.html`

**Essential Commands:**
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ActionResultTest

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test method
./gradlew test --tests "*testSuccessResult"
```

---

## Current Test Coverage Analysis

### Coverage by Package

| Package | Source Files | Test Files | Coverage | Priority |
|---------|--------------|------------|----------|----------|
| `action` | 8 | 2 | ~25% | HIGH |
| `execution` | 6 | 0 | 0% | CRITICAL |
| `plugin` | 5 | 0 | 0% | HIGH |
| `llm.async` | 5 | 1 | ~20% | MEDIUM |
| `llm.cascade` | 6 | 4 | ~67% | LOW |
| `memory` | 4 | 1 | ~25% | MEDIUM |
| `orchestration` | 3 | 0 | 0% | HIGH |
| `coordination` | 5 | 4 | ~80% | LOW |
| `blackboard` | 2 | 1 | ~50% | LOW |
| `communication` | 2 | 1 | ~50% | LOW |
| `behavior` | 8 | 4 | ~50% | LOW |
| `skill` | 3 | 3 | ~100% | DONE |
| `decision` | 4 | 4 | ~100% | DONE |

**Overall:** 13% coverage (CRITICAL - needs improvement)

---

## Priority Components Needing Tests

### CRITICAL Priority (Test Before Production)

1. **ActionExecutor** (`action/ActionExecutor.java`)
   - Why: Core execution loop, everything depends on this
   - Tests needed:
     - Task queuing and execution
     - Async planning completion
     - State transitions
     - Error handling
     - Tick budget enforcement

2. **AgentStateMachine** (`execution/AgentStateMachine.java`)
   - Why: State management bugs cause hard-to-debug issues
   - Tests needed:
     - Valid state transitions
     - Invalid state rejection
     - Event publishing
     - Thread safety
     - Reset functionality

3. **InterceptorChain** (`execution/InterceptorChain.java`)
   - Why: Cross-cutting concerns, affects all actions
   - Tests needed:
     - Interceptor order
     - Before/after action hooks
     - Exception handling in interceptors
     - Multiple interceptors

4. **ActionRegistry** (`plugin/ActionRegistry.java`)
   - Why: Plugin system, extensibility depends on this
   - Tests needed:
     - Action registration
     - Action creation
     - Priority-based conflict resolution
     - Plugin tracking
     - Thread safety

5. **TaskPlanner** (`llm/TaskPlanner.java`)
   - Why: LLM integration, critical path
   - Tests needed:
     - Prompt building
     - Response parsing
     - Error handling
     - Async execution (mocked LLM)

### HIGH Priority (Test Soon)

6. **AsyncLLMClient** implementations
   - Tests needed: API integration (with mocked responses)

7. **CompanionMemory**
   - Tests needed: Memory storage, retrieval, conversation tracking

8. **OrchestratorService**
   - Tests needed: Agent coordination, task distribution

9. **BaseAction** subclasses
   - Tests needed: Individual action behaviors

### MEDIUM Priority (Test When Possible)

10. **InMemoryVectorStore** - Semantic search
11. **EventBus** - Event publishing and subscription
12. **ResilientLLMClient** - Retry, circuit breaker
13. **Blackboard** - Shared knowledge system

---

## Testing Frameworks and Tools

### JUnit 5

**Test Structure:**
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MyComponent Tests")
class MyComponentTest {

    @Test
    @DisplayName("Basic functionality works")
    void testBasicFunctionality() {
        // Arrange
        MyComponent component = new MyComponent();

        // Act
        String result = component.doSomething("input");

        // Assert
        assertEquals("expected", result);
    }
}
```

**Common Assertions:**
```java
assertEquals(expected, actual);
assertNotEquals(unexpected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(object);
assertNotNull(object);
assertThrows(ExceptionType.class, () -> riskyOperation());
assertTimeout(duration, () -> longOperation());
```

### Mockito (Limited Use)

**Note:** Mockito has issues with Minecraft classes due to classloader conflicts.

**For Non-Minecraft Classes:**
```java
import static org.mockito.Mockito.*;

// Create mock
MyDependency mockDep = mock(MyDependency.class);

// Define behavior
when(mockDep.calculate(anyInt())).thenReturn(42);

// Verify interaction
verify(mockDep).calculate(5);
```

**For Minecraft Classes:**
Use `MockForemanEntity` from test utilities instead:
```java
MockForemanEntity mockForeman = new MockForemanEntity();
mockForeman.setLevel(mockLevel);
mockForeman.setNavigation(mockNavigation);
```

### Custom Test Utilities

**MockForemanEntity** (`behavior/MockForemanEntity.java`):
```java
MockForemanEntity foreman = new MockForemanEntity();
foreman.setLevel(testLevel);
foreman.setEntityName("TestForeman");
```

**TaskBuilder** (`testutil/TaskBuilder.java`):
```java
Task task = TaskBuilder.builder()
    .action("mine")
    .parameter("block", "stone")
    .parameter("quantity", 10)
    .build();
```

---

## Test Patterns by Component Type

### Pattern 1: Testing State Machines

```java
@DisplayName("AgentStateMachine Tests")
class AgentStateMachineTest {

    private AgentStateMachine stateMachine;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new SimpleEventBus();
        stateMachine = new AgentStateMachine(eventBus, "test-agent");
    }

    @Test
    @DisplayName("Valid state transition succeeds")
    void testValidTransition() {
        // Act
        boolean result = stateMachine.transitionTo(AgentState.PLANNING);

        // Assert
        assertTrue(result);
        assertEquals(AgentState.PLANNING, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("Invalid state transition fails")
    void testInvalidTransition() {
        // Arrange: Start in IDLE
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());

        // Act: Try to skip to EXECUTING (invalid)
        boolean result = stateMachine.transitionTo(AgentState.EXECUTING);

        // Assert
        assertFalse(result);
        assertEquals(AgentState.IDLE, stateMachine.getCurrentState());
    }

    @Test
    @DisplayName("State transition publishes event")
    void testStateTransitionPublishesEvent() {
        // Arrange: Subscribe to events
        final StateTransitionEvent[] capturedEvent = new StateTransitionEvent[1];
        eventBus.subscribe(StateTransitionEvent.class, event -> {
            capturedEvent[0] = event;
        });

        // Act
        stateMachine.transitionTo(AgentState.PLANNING);

        // Assert
        assertNotNull(capturedEvent[0]);
        assertEquals(AgentState.IDLE, capturedEvent[0].getFromState());
        assertEquals(AgentState.PLANNING, capturedEvent[0].getToState());
    }
}
```

### Pattern 2: Testing Actions

```java
@DisplayName("MineBlockAction Tests")
class MineBlockActionTest {

    private MockForemanEntity foreman;
    private Task task;

    @BeforeEach
    void setUp() {
        foreman = new MockForemanEntity();
        task = TaskBuilder.builder()
            .action("mine")
            .parameter("block", "stone")
            .parameter("quantity", 5)
            .build();
    }

    @Test
    @DisplayName("Action initializes correctly")
    void testActionInitialization() {
        // Act
        MineBlockAction action = new MineBlockAction(foreman, task);

        // Assert
        assertFalse(action.isComplete());
        assertEquals("Mine 5 stone", action.getDescription());
    }

    @Test
    @DisplayName("Action completes after mining target quantity")
    void testActionCompletion() {
        // Arrange
        MineBlockAction action = new MineBlockAction(foreman, task);
        action.start();

        // Act: Simulate mining blocks
        for (int i = 0; i < 5; i++) {
            action.tick();
        }

        // Assert
        assertTrue(action.isComplete());
        assertTrue(action.getResult().isSuccess());
    }

    @Test
    @DisplayName("Action reports progress correctly")
    void testActionProgress() {
        // Arrange
        MineBlockAction action = new MineBlockAction(foreman, task);
        action.start();

        // Act & Assert
        action.tick();
        // assertEquals(1, action.getBlocksMined()); // If tracking progress
    }
}
```

### Pattern 3: Testing Plugin Registry

```java
@DisplayName("ActionRegistry Tests")
class ActionRegistryTest {

    private ActionRegistry registry;
    private MockForemanEntity foreman;
    private ActionContext context;

    @BeforeEach
    void setUp() {
        registry = ActionRegistry.getInstance();
        registry.clear(); // Start fresh
        foreman = new MockForemanEntity();
        context = ActionContext.builder()
            .serviceContainer(new SimpleServiceContainer())
            .eventBus(new SimpleEventBus())
            .build();
    }

    @Test
    @DisplayName("Action registration and creation")
    void testActionRegistration() {
        // Arrange: Register action factory
        registry.register("test_action",
            (f, t, c) -> new TestAction(f, t),
            0,
            "test_plugin"
        );

        // Act: Create action
        Task task = TaskBuilder.builder().action("test_action").build();
        BaseAction action = registry.createAction("test_action", foreman, task, context);

        // Assert
        assertNotNull(action);
        assertTrue(action instanceof TestAction);
    }

    @Test
    @DisplayName("Priority-based conflict resolution")
    void testPriorityConflictResolution() {
        // Arrange: Register same action with different priorities
        registry.register("conflict", (f, t, c) -> new Action1(f, t), 1, "plugin1");
        registry.register("conflict", (f, t, c) -> new Action2(f, t), 10, "plugin2");

        // Act
        Task task = TaskBuilder.builder().action("conflict").build();
        BaseAction action = registry.createAction("conflict", foreman, task, context);

        // Assert: Higher priority wins
        assertTrue(action instanceof Action2);
        assertEquals("plugin2", registry.getPluginForAction("conflict"));
    }
}
```

### Pattern 4: Testing Async Operations

```java
@DisplayName("AsyncLLMClient Tests")
class AsyncLLMClientTest {

    @Test
    @DisplayName("Async call completes successfully")
    void testAsyncCallSuccess() {
        // Arrange: Create client with mocked HTTP
        AsyncLLMClient client = new TestAsyncLLMClient();
        List<ChatMessage> messages = List.of(new ChatMessage("user", "test"));

        // Act: Start async call
        CompletableFuture<String> future = client.chatAsync(messages, config);

        // Assert: Not complete immediately
        assertFalse(future.isDone());

        // Wait for completion (with timeout)
        String response = future.orTimeout(5, TimeUnit.SECONDS).join();

        // Assert: Response received
        assertNotNull(response);
        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("Async call handles timeout")
    void testAsyncCallTimeout() {
        // Arrange: Create slow client
        AsyncLLMClient client = new SlowAsyncLLMClient();

        // Act & Assert: Should timeout
        assertThrows(TimeoutException.class, () -> {
            client.chatAsync(messages, config)
                .orTimeout(1, TimeUnit.SECONDS)
                .join();
        });
    }
}
```

### Pattern 5: Testing Interceptor Chain

```java
@DisplayName("InterceptorChain Tests")
class InterceptorChainTest {

    private InterceptorChain chain;
    private MockForemanEntity foreman;
    private ActionContext context;

    @BeforeEach
    void setUp() {
        chain = new InterceptorChain();
        foreman = new MockForemanEntity();
        context = ActionContext.builder().build();
    }

    @Test
    @DisplayName("Interceptors execute in order")
    void testInterceptorOrder() {
        // Arrange: Add interceptors that track execution
        List<String> executionLog = new ArrayList<>();
        chain.addInterceptor(new LoggingInterceptor());
        chain.addInterceptor(new TestInterceptor(executionLog, "second"));
        chain.addInterceptor(new TestInterceptor(executionLog, "first"));

        // Act: Execute action through chain
        BaseAction action = new TestAction(foreman, task);
        chain.execute(action, context);

        // Assert: Interceptors called in correct order
        assertEquals(List.of("first", "second"), executionLog);
    }

    @Test
    @DisplayName("Interceptor exception handling")
    void testInterceptorException() {
        // Arrange: Add interceptor that throws exception
        chain.addInterceptor(new FailingInterceptor());

        // Act & Assert: Should handle gracefully
        BaseAction action = new TestAction(foreman, task);
        assertDoesNotThrow(() -> chain.execute(action, context));
    }
}
```

---

## Integration Testing

### Current Status

Integration testing is limited due to:
- Minecraft server dependency
- Forge mod loading complexity
- Difficulty mocking Minecraft classes

### Recommended Approach

1. **Create integration test framework:**
```java
// src/test/java/com/minewright/integration/
@IntegrationTest
@DisplayName("End-to-end action execution")
class ActionExecutionIntegrationTest {

    @Test
    @DisplayName("Full command execution: mine -> craft -> place")
    void testFullWorkflow() {
        // Requires Minecraft test server
        // Run with: ./gradlew integrationTest
    }
}
```

2. **Use Forge test server:**
```gradle
// In build.gradle
minecraft {
    runs {
        testServer {
            workingDirectory file("run/test")
            args("--nogui", "--launchTarget", "forge_test_server")
        }
    }
}
```

---

## Test Coverage Goals

### Short-term Goals (Next 2 weeks)

| Component | Current | Target | Priority |
|-----------|---------|--------|----------|
| ActionExecutor | 0% | 80% | CRITICAL |
| AgentStateMachine | 0% | 90% | CRITICAL |
| InterceptorChain | 0% | 80% | HIGH |
| ActionRegistry | 0% | 80% | HIGH |
| TaskPlanner | 0% | 60% | MEDIUM |

**Overall Target:** 40% coverage (from current 13%)

### Long-term Goals (Next month)

- **70% overall coverage**
- Integration test framework
- Performance benchmarks
- Stress tests for multi-agent scenarios

---

## Mock Patterns for Minecraft

### Problem: Mockito + Minecraft = Classloader Issues

**Error:**
```
Mockito cannot mock this class: class net.minecraft.world.level.Level
```

**Solution: Custom Test Doubles**

### Pattern 1: MockForemanEntity

```java
// Use existing mock from test utilities
MockForemanEntity foreman = new MockForemanEntity();
foreman.setLevel(createMockLevel());
foreman.setNavigation(createMockNavigation());
foreman.setEntityName("TestBot");

// Mock memory
CompanionMemory memory = new CompanionMemory("TestBot");
foreman.setMemory(memory);
```

### Pattern 2: Mock Level

```java
private ServerLevel createMockLevel() {
    // Simplified level mock for testing
    // Avoids Minecraft classloader issues
    return new MockServerLevel();
}
```

### Pattern 3: Mock Navigation

```java
private PathNavigation createMockNavigation() {
    return new PathNavigation() {
        private BlockPos target;
        private boolean isDone = true;

        @Override
        public void moveTo(double x, double y, double z, double speed) {
            this.target = new BlockPos((int)x, (int)y, (int)z);
            this.isDone = false;
        }

        @Override
        public boolean isDone() {
            return isDone;
        }

        @Override
        public void stop() {
            isDone = true;
        }
    };
}
```

### Pattern 4: Mock LLM Response

```java
// For testing async LLM calls
class MockAsyncLLMClient implements AsyncLLMClient {

    private final String mockResponse;
    private final long delayMs;

    MockAsyncLLMClient(String response, long delayMs) {
        this.mockResponse = response;
        this.delayMs = delayMs;
    }

    @Override
    public CompletableFuture<String> chatAsync(List<ChatMessage> messages, LLMConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return mockResponse;
        });
    }
}
```

---

## Test Checklist

### Before Writing Tests

- [ ] Understand what the code should do
- [ ] Read the implementation
- [ ] Identify edge cases
- [ ] Check for similar existing tests

### While Writing Tests

- [ ] Use @DisplayName for descriptive test names
- [ ] Follow Arrange-Act-Assert pattern
- [ ] Test both success and failure cases
- [ ] Test edge cases (null, empty, negative values)
- [ ] Add assertions for all expected behaviors
- [ ] Use appropriate assertions (assertEquals, assertTrue, etc.)

### After Writing Tests

- [ ] Run tests locally: `./gradlew test`
- [ ] Ensure all tests pass
- [ ] Check test coverage report
- [ ] Review test output logs
- [ ] Clean up any temporary files

---

## Performance Testing

### Tick Budget Testing

```java
@DisplayName("Tick Budget Enforcement Tests")
class TickBudgetTest {

    @Test
    @DisplayName("Action respects tick budget")
    void testTickBudget() {
        // Arrange
        TickProfiler profiler = new TickProfiler();
        BaseAction action = new ExpensiveAction(foreman, task);

        // Act
        profiler.startTick();
        action.tick();
        profiler.endTick();

        // Assert
        assertTrue(profiler.getDurationMs() <= 5.0,
            "Action exceeded tick budget: " + profiler.getDurationMs() + "ms");
    }
}
```

### Concurrency Testing

```java
@DisplayName("Thread Safety Tests")
class ThreadSafetyTest {

    @Test
    @DisplayName("Concurrent state transitions are safe")
    void testConcurrentTransitions() throws InterruptedException {
        // Arrange
        AgentStateMachine sm = new AgentStateMachine(eventBus, "test");
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act: Concurrent transitions from multiple threads
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    sm.transitionTo(AgentState.PLANNING);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert: State machine should be in valid state
        assertTrue(sm.canAcceptCommands() || sm.isActive());
    }
}
```

---

## Bug Report Template

When finding bugs, document with:

```markdown
## Bug Report: [Brief Description]

**Component:** [Class/Package]
**Severity:** [Critical/High/Medium/Low]
**Test Case:** [Test that exposes the bug]

### Observed Behavior
[What actually happens]

### Expected Behavior
[What should happen]

### Reproduction Steps
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Minimal Reproducible Example
```java
@Test
void testBugReproduction() {
    // Code that reproduces the bug
}
```

### Root Cause Analysis
[Why does this happen?]

### Suggested Fix
[How should this be fixed?]
```

---

## When to Escalate

**Ask for help when:**

1. **Can't test something:**
   - Minecraft class mocking issues
   - Async operation timing issues
   - State-dependent tests

2. **Find critical bugs:**
   - Data corruption
   - Memory leaks
   - Thread safety violations

3. **Test coverage blocked:**
   - Can't create test doubles
   - Need integration test environment
   - Performance testing requires special setup

---

## Quick Start Workflow

1. **Understand what needs testing:**
   - Read the implementation code
   - Check for existing tests
   - Identify test scenarios

2. **Choose test pattern:**
   - State machine? Use state machine pattern
   - Action? Use action pattern
   - Async? Use async pattern

3. **Write tests:**
   - Start with happy path (success case)
   - Add failure cases
   - Add edge cases
   - Use descriptive names

4. **Run and verify:**
   - Execute tests: `./gradlew test`
   - Check coverage
   - Fix any issues

5. **Document:**
   - Bug reports if found
   - Coverage improvements
   - Test patterns discovered

---

**Remember:** Tests are code. Treat them with the same quality standards as production code. Good tests prevent regressions, serve as documentation, and make refactoring safer.
