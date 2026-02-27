# MineWright Testing Strategy

## Executive Summary

This document provides a comprehensive analysis of the current testing state of the MineWright Minecraft mod and recommends improvements for achieving robust test coverage. The codebase (~18,000 lines of Java) implements an AI-driven autonomous agent system with complex async operations, state machines, plugin architecture, and LLM integration.

**Current State:** Minimal test coverage with only placeholder test files.

**Target:** Comprehensive unit, integration, and contract testing with >80% coverage.

---

## 1. Current Testing Status

### 1.1 Existing Tests

Located in `C:\Users\casey\steve\src\test\java\com\minewright\`:

| Test File | Status | Coverage |
|-----------|--------|----------|
| `action/ActionExecutorTest.java` | Placeholder only (TODO comments) | 0% |
| `llm/TaskPlannerTest.java` | Placeholder only (TODO comments) | 0% |
| `memory/WorldKnowledgeTest.java` | Placeholder only (TODO comments) | 0% |
| `structure/StructureGeneratorsTest.java` | Placeholder only (TODO comments) | 0% |

**Finding:** All existing tests are empty skeletons with only TODO comments. No actual test implementations exist.

### 1.2 Test Infrastructure

**Build Configuration:**
- JUnit 5.9.3 configured in `build.gradle`
- Test task properly configured: `useJUnitPlatform()`
- No test dependencies beyond basic JUnit

**Missing Dependencies:**
- No mocking framework (Mockito recommended)
- No assertion libraries (AssertJ recommended)
- No test containers for integration testing
- No Minecraft test fixtures or mocks

---

## 2. Architecture Analysis & Test Coverage Gaps

### 2.1 Critical Components Without Tests

#### Core Execution Flow (HIGH PRIORITY)
- **`ActionExecutor`** (500+ lines) - Central orchestrator, tick-based execution
- **`AgentStateMachine`** (280 lines) - State transitions, thread safety
- **`InterceptorChain`** - Cross-cutting concerns
- **All Action Implementations** - 10+ action types

#### LLM Integration (HIGH PRIORITY)
- **`TaskPlanner`** (360 lines) - Async planning, batching, fallbacks
- **`LLMCache`** (200 lines) - Caching with TTL, LRU eviction
- **`BatchingLLMClient`** (350 lines) - Rate limiting, prompt batching
- **`ResponseParser`** - Parse structured LLM responses

#### Plugin Architecture (MEDIUM PRIORITY)
- **`ActionRegistry`** (270 lines) - Dynamic action registration
- **`PluginManager`** (300 lines) - SPI discovery, dependency resolution
- **`ActionFactory`** - Factory pattern implementation

#### Event System (MEDIUM PRIORITY)
- **`EventBus`** / **`SimpleEventBus`** - Pub/sub messaging
- **Event handlers** - Subscription management

#### Memory & Knowledge (MEDIUM PRIORITY)
- **`WorldKnowledge`** (140 lines) - World scanning, block/entity detection
- **`CompanionMemory`** - Conversation history
- **`ForemanMemory`** - Agent-specific state

#### Utility & Infrastructure (LOW PRIORITY)
- **`ServiceContainer`** - Dependency injection
- **`StructureGenerators`** (370 lines) - Procedural generation
- **`CodeExecutionEngine`** - GraalVM JS sandboxing

### 2.2 Risk Assessment

| Component | Risk Level | Reason |
|-----------|------------|--------|
| AgentStateMachine | HIGH | Complex state logic, thread safety |
| LLMCache | HIGH | Concurrency, cache correctness |
| ActionRegistry | MEDIUM | Singleton state, concurrent registration |
| BatchingLLMClient | HIGH | Async operations, rate limiting |
| PluginManager | MEDIUM | Dependency resolution, SPI loading |
| ActionExecutor | HIGH | Tick-based execution, async coordination |

---

## 3. Recommended Testing Patterns for Minecraft Mods

### 3.1 Unit Testing Strategy

#### Pattern 1: Minecraft-Agnostic Testing
Test business logic without Minecraft dependencies by using interfaces and mocks.

```java
@Test
void testAgentStateMachineValidTransitions() {
    EventBus mockBus = mock(EventBus.class);
    AgentStateMachine machine = new AgentStateMachine(mockBus, "test-agent");

    // Test IDLE -> PLANNING transition
    assertTrue(machine.canTransitionTo(AgentState.PLANNING));
    assertTrue(machine.transitionTo(AgentState.PLANNING));
    assertEquals(AgentState.PLANNING, machine.getCurrentState());

    verify(mockBus).publish(any(StateTransitionEvent.class));
}
```

#### Pattern 2: Fake/Mock Minecraft Objects
Create lightweight test doubles for Minecraft classes.

```java
class FakeForemanEntity extends ForemanEntity {
    private final Queue<Task> taskQueue = new LinkedList<>();

    public FakeForemanEntity() {
        super(EntityType.FOREMAN, TestLevel.INSTANCE);
    }

    // Override only necessary methods
    @Override
    public ServerLevel level() {
        return TestLevel.INSTANCE;
    }
}
```

#### Pattern 3: Parameterized Tests for Actions
Test action implementations with various parameters.

```java
@ParameterizedTest
@CsvSource({
    "mine, stone, 10, true",
    "mine, diamond, 1, true",
    "mine, invalid_block, 1, false"
})
void testTaskValidation(String action, String block, int quantity, boolean valid) {
    Task task = new Task(action, Map.of("block", block, "quantity", quantity));
    TaskPlanner planner = new TaskPlanner();
    assertEquals(valid, planner.validateTask(task));
}
```

### 3.2 Integration Testing Strategy

#### Pattern 1: In-Process Minecraft Server
Use Forge's test framework to run a lightweight server.

```java
@SpringBootTest(classes = {MineWrightMod.class})
@ExtendWith(MinecraftExtension.class)
class ActionExecutionIntegrationTest {

    @Test
    void testCompleteMiningWorkflow(TestServer server) {
        // Spawn test entity
        ForemanEntity foreman = server.spawnEntity(ForemanEntity.class);

        // Execute action
        foreman.getExecutor().processNaturalLanguageCommand("Mine 10 stone");

        // Wait for completion
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> !foreman.getExecutor().isExecuting());

        // Verify results
        assertTrue(foreman.getInventory().hasItem(Items.STONE));
    }
}
```

#### Pattern 2: Contract Testing for LLM Clients
Test LLM integration without actual API calls.

```java
@Test
void testAsyncLLMClientCaching() {
    AsyncLLMClient client = spy(new AsyncOpenAIClient("test-key", "model", 100, 0.7));
    LLMCache cache = new LLMCache();

    // First call
    CompletableFuture<LLMResponse> first = client.sendAsync("test", params);

    // Verify cache miss
    assertEquals(0, cache.getStats().hits);

    // Second call (should hit cache)
    CompletableFuture<LLMResponse> second = client.sendAsync("test", params);

    // Verify cache hit
    assertEquals(1, cache.getStats().hits);
}
```

#### Pattern 3: End-to-End Plugin Testing
Test plugin loading and action registration.

```java
@Test
void testPluginLoading() {
    ActionRegistry registry = ActionRegistry.getInstance();
    ServiceContainer container = new SimpleServiceContainer();
    PluginManager manager = PluginManager.getInstance();

    // Load plugins
    manager.loadPlugins(registry, container);

    // Verify actions registered
    assertTrue(registry.hasAction("mine"));
    assertTrue(registry.hasAction("build"));
    assertTrue(registry.hasAction("craft"));

    // Verify factory creates correct actions
    BaseAction action = registry.createAction("mine", foreman, task, context);
    assertTrue(action instanceof MineBlockAction);
}
```

### 3.3 Async Testing Patterns

#### Pattern 1: CompletableFuture Testing
Test async operations without blocking.

```java
@Test
void testAsyncTaskPlanning() {
    TaskPlanner planner = new TaskPlanner();
    ForemanEntity foreman = new FakeForemanEntity();

    CompletableFuture<ResponseParser.ParsedResponse> future =
        planner.planTasksAsync(foreman, "Build a house");

    // Wait for completion with timeout
    await().atMost(10, TimeUnit.SECONDS)
        .until(future::isDone);

    ResponseParser.ParsedResponse result = future.join();
    assertNotNull(result);
    assertFalse(result.getTasks().isEmpty());
}
```

#### Pattern 2: Tick Simulation
Test tick-based components with simulated time.

```java
@Test
void testActionExecutorTickLoop() {
    ForemanEntity foreman = new FakeForemanEntity();
    ActionExecutor executor = new ActionExecutor(foreman);

    executor.processNaturalLanguageCommand("Mine 5 stone");

    // Simulate 100 ticks
    for (int i = 0; i < 100; i++) {
        executor.tick();
    }

    // Verify action completed
    assertTrue(executor.getCurrentActionProgress() == 100);
}
```

---

## 4. Unit Test Improvements by Component

### 4.1 ActionExecutor Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\action\ActionExecutorTest.java`

```java
class ActionExecutorTest {
    private ForemanEntity mockForeman;
    private ActionExecutor executor;

    @BeforeEach
    void setUp() {
        mockForeman = mock(ForemanEntity.class);
        when(mockForeman.level()).thenReturn(mock(ServerLevel.class));
        when(mockForeman.getSteveName()).thenReturn("TestSteve");
        executor = new ActionExecutor(mockForeman);
    }

    @Test
    void testProcessNaturalLanguageCommandStartsAsyncPlanning() {
        executor.processNaturalLanguageCommand("Build a house");

        assertTrue(executor.isPlanning());
        verify(mockForeman, never()).sendChatMessage(anyString());
    }

    @Test
    void testStopCurrentActionCancelsActiveAction() {
        Task task = new Task("mine", Map.of("block", "stone", "quantity", 10));
        executor.queueTask(task);
        executor.tick();

        executor.stopCurrentAction();

        assertFalse(executor.isExecuting());
        assertNull(executor.getCurrentAction());
    }

    @Test
    void testQueueTaskAddsToQueue() {
        Task task = new Task("mine", Map.of("block", "stone", "quantity", 10));
        executor.queueTask(task);

        assertTrue(executor.isExecuting());
    }
}
```

### 4.2 TaskPlanner Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\llm\TaskPlannerTest.java`

```java
class TaskPlannerTest {
    private TaskPlanner planner;

    @BeforeEach
    void setUp() {
        planner = new TaskPlanner();
    }

    @Test
    void testValidateTaskWithValidParameters() {
        Task task = new Task("mine", Map.of("block", "stone", "quantity", 10));
        assertTrue(planner.validateTask(task));
    }

    @Test
    void testValidateTaskWithMissingParameters() {
        Task task = new Task("mine", Map.of("block", "stone")); // Missing quantity
        assertFalse(planner.validateTask(task));
    }

    @Test
    void testValidateAndFilterTasks() {
        List<Task> tasks = List.of(
            new Task("mine", Map.of("block", "stone", "quantity", 10)),
            new Task("mine", Map.of("block", "stone")), // Invalid
            new Task("craft", Map.of("item", "sword", "quantity", 1))
        );

        List<Task> filtered = planner.validateAndFilterTasks(tasks);
        assertEquals(2, filtered.size());
    }
}
```

### 4.3 AgentStateMachine Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\execution\AgentStateMachineTest.java` (NEW)

```java
class AgentStateMachineTest {
    private EventBus mockBus;
    private AgentStateMachine machine;

    @BeforeEach
    void setUp() {
        mockBus = mock(EventBus.class);
        machine = new AgentStateMachine(mockBus, "test-agent");
    }

    @Test
    void testInitialStateIsIdle() {
        assertEquals(AgentState.IDLE, machine.getCurrentState());
    }

    @Test
    void testValidIdleToPlanningTransition() {
        assertTrue(machine.canTransitionTo(AgentState.PLANNING));
        assertTrue(machine.transitionTo(AgentState.PLANNING));
        assertEquals(AgentState.PLANNING, machine.getCurrentState());
    }

    @Test
    void testInvalidPlanningToIdleTransition() {
        machine.transitionTo(AgentState.PLANNING);
        assertFalse(machine.canTransitionTo(AgentState.IDLE));
        assertFalse(machine.transitionTo(AgentState.IDLE));
    }

    @Test
    void testStateTransitionPublishesEvent() {
        machine.transitionTo(AgentState.PLANNING, "test reason");

        ArgumentCaptor<StateTransitionEvent> eventCaptor =
            ArgumentCaptor.forClass(StateTransitionEvent.class);
        verify(mockBus).publish(eventCaptor.capture());

        StateTransitionEvent event = eventCaptor.getValue();
        assertEquals("test-agent", event.getAgentId());
        assertEquals(AgentState.IDLE, event.getFromState());
        assertEquals(AgentState.PLANNING, event.getToState());
    }

    @Test
    void testResetReturnsToIdle() {
        machine.transitionTo(AgentState.PLANNING);
        machine.transitionTo(AgentState.EXECUTING);

        machine.reset();

        assertEquals(AgentState.IDLE, machine.getCurrentState());
    }
}
```

### 4.4 LLMCache Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\llm\async\LLMCacheTest.java` (NEW)

```java
class LLMCacheTest {
    private LLMCache cache;

    @BeforeEach
    void setUp() {
        cache = new LLMCache();
    }

    @Test
    void testCacheMissOnFirstAccess() {
        Optional<LLMResponse> result = cache.get("test prompt", "model", "provider");
        assertTrue(result.isEmpty());
        assertEquals(1, cache.getStats().misses);
    }

    @Test
    void testCacheHitAfterPut() {
        LLMResponse response = new LLMResponse("content", 100, 500, true);
        cache.put("test prompt", "model", "provider", response);

        Optional<LLMResponse> result = cache.get("test prompt", "model", "provider");
        assertTrue(result.isPresent());
        assertEquals(1, cache.getStats().hits);
    }

    @Test
    void testCacheEvictionWhenFull() {
        // Fill cache to capacity (500 entries)
        for (int i = 0; i < 500; i++) {
            LLMResponse response = new LLMResponse("content" + i, 100, 500, true);
            cache.put("prompt" + i, "model", "provider", response);
        }

        long sizeBefore = cache.size();
        assertEquals(500, sizeBefore);

        // Add one more - should evict oldest
        LLMResponse response = new LLMResponse("new", 100, 500, true);
        cache.put("new", "model", "provider", response);

        assertEquals(500, cache.size());
        assertTrue(cache.getStats().evictions > 0);
    }

    @Test
    void testExpiredEntryReturnsMiss() {
        LLMResponse response = new LLMResponse("content", 100, 500, true);
        cache.put("test", "model", "provider", response);

        // Wait for TTL (5 minutes) - in real test, use configurable TTL
        // For now, test clear functionality
        cache.clear();

        Optional<LLMResponse> result = cache.get("test", "model", "provider");
        assertTrue(result.isEmpty());
    }
}
```

### 4.5 ActionRegistry Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\plugin\ActionRegistryTest.java` (NEW)

```java
class ActionRegistryTest {
    private ActionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = ActionRegistry.getInstance();
        registry.clear(); // Reset for each test
    }

    @Test
    void testRegisterAction() {
        ActionFactory factory = (foreman, task, ctx) -> mock(BaseAction.class);
        registry.register("test_action", factory);

        assertTrue(registry.hasAction("test_action"));
        assertEquals(1, registry.getActionCount());
    }

    @Test
    void testCreateActionReturnsCorrectInstance() {
        ActionFactory factory = (foreman, task, ctx) -> mock(BaseAction.class);
        registry.register("test_action", factory);

        ForemanEntity foreman = mock(ForemanEntity.class);
        Task task = mock(Task.class);
        ActionContext context = mock(ActionContext.class);

        BaseAction action = registry.createAction("test_action", foreman, task, context);

        assertNotNull(action);
    }

    @Test
    void testPriorityResolution() {
        ActionFactory lowPriority = (foreman, task, ctx) -> mock(BaseAction.class);
        ActionFactory highPriority = (foreman, task, ctx) -> mock(BaseAction.class);

        registry.register("action", lowPriority, 1, "plugin1");
        registry.register("action", highPriority, 10, "plugin2");

        assertEquals("plugin2", registry.getPluginForAction("action"));
    }
}
```

---

## 5. Integration Test Strategies

### 5.1 LLM Integration Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\llm\LLMIntegrationTest.java` (NEW)

```java
@ExtendWith(MockitoExtension.class)
class LLMIntegrationTest {

    @Test
    void testTaskPlanningEndToEnd() {
        // Use fake LLM client for testing
        TaskPlanner planner = new TaskPlanner();
        ForemanEntity foreman = new FakeForemanEntity();

        CompletableFuture<ResponseParser.ParsedResponse> future =
            planner.planTasksAsync(foreman, "Build a small wooden house");

        await().atMost(10, TimeUnit.SECONDS)
            .until(future::isDone);

        ResponseParser.ParsedResponse result = future.join();
        assertNotNull(result);
        assertFalse(result.getTasks().isEmpty());
        assertTrue(result.getPlan().toLowerCase().contains("build"));
    }
}
```

### 5.2 Action Execution Integration Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\action\ActionIntegrationTest.java` (NEW)

```java
class ActionIntegrationTest {
    private ForemanEntity foreman;
    private ActionExecutor executor;
    private FakeServerLevel testLevel;

    @BeforeEach
    void setUp() {
        testLevel = new FakeServerLevel();
        foreman = new FakeForemanEntity(testLevel);
        executor = new ActionExecutor(foreman);
    }

    @Test
    void testCompleteMiningWorkflow() {
        // Place stone blocks in world
        testLevel.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        testLevel.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        testLevel.setBlock(new BlockPos(0, 1, 2), Blocks.STONE);

        // Queue mining tasks
        executor.queueTask(new Task("mine", Map.of("block", "stone", "quantity", 3)));

        // Simulate ticks until complete
        int ticks = 0;
        while (executor.isExecuting() && ticks < 1000) {
            executor.tick();
            ticks++;
        }

        // Verify blocks were mined
        assertEquals(Blocks.AIR, testLevel.getBlock(new BlockPos(0, 1, 0)));
        assertEquals(Blocks.AIR, testLevel.getBlock(new BlockPos(0, 1, 1)));
        assertEquals(Blocks.AIR, testLevel.getBlock(new BlockPos(0, 1, 2)));
    }
}
```

### 5.3 Plugin System Integration Tests

**Location:** `C:\Users\casey\steve\src\test\java\com\minewright\plugin\PluginIntegrationTest.java` (NEW)

```java
class PluginIntegrationTest {

    @Test
    void testCoreActionsPluginLoads() {
        ActionRegistry registry = ActionRegistry.getInstance();
        registry.clear();

        ServiceContainer container = new SimpleServiceContainer();
        PluginManager manager = PluginManager.getInstance();

        manager.loadPlugins(registry, container);

        // Verify all core actions are registered
        String[] coreActions = {"mine", "build", "craft", "place", "pathfind",
                                "attack", "follow", "gather"};

        for (String action : coreActions) {
            assertTrue(registry.hasAction(action),
                      "Action '" + action + "' should be registered");
        }
    }

    @Test
    void testPluginDependencyResolution() {
        // Test that plugins with dependencies load in correct order
        // This requires creating test plugins with dependencies
    }
}
```

---

## 6. Required Test Infrastructure

### 6.1 Build Configuration Updates

**File:** `C:\Users\casey\steve\build.gradle`

```gradle
dependencies {
    // Existing dependencies...

    // Testing dependencies
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.3'

    // Add these:
    testImplementation 'org.mockito:mockito-core:5.3.1'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.3.1'
    testImplementation 'org.assertj:assertj-core:3.24.2'
    testImplementation 'org.awaitility:awaitility:4.2.0'

    // Minecraft testing (if using Forge test framework)
    testImplementation 'net.minecraftforge:forge:1.20.1-47.4.16:test'
}

test {
    useJUnitPlatform()

    // Add test configuration
    maxParallelForks = 1 // Minecraft tests may not be parallel-safe
    jvmArgs += '-Dforge.logging.console.level=debug'

    // Generate test coverage report
    finalizedBy jacocoTestReport
}

// Code coverage
apply plugin: 'jacoco'

jacoco {
    toolVersion = "0.8.10"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}

test {
    jacoco {
        excludes = ['**/entity/**', '**/client/**'] // Exclude client-only code
    }
}
```

### 6.2 Test Utilities Package

**Create:** `C:\Users\casey\steve\src\test\java\com\minewright\testutil\`

```java
// FakeForemanEntity.java
public class FakeForemanEntity extends ForemanEntity {
    private final FakeServerLevel testLevel;
    private final ForemanMemory memory;

    public FakeForemanEntity() {
        super(EntityType.FOREMAN, FakeServerLevel.INSTANCE);
        this.testLevel = FakeServerLevel.INSTANCE;
        this.memory = new ForemanMemory(this);
    }

    @Override
    public ServerLevel level() {
        return testLevel;
    }

    @Override
    public ForemanMemory getMemory() {
        return memory;
    }

    // Override other necessary methods...
}

// FakeServerLevel.java
public class FakeServerLevel implements ServerLevel {
    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();

    public static final FakeServerLevel INSTANCE = new FakeServerLevel();

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    public void setBlock(BlockPos pos, Block block) {
        blocks.put(pos, block.defaultBlockState());
    }

    // Implement other necessary methods...
}

// TaskBuilder.java - Test data builder
public class TaskBuilder {
    private String action;
    private Map<String, Object> params = new HashMap<>();

    public static TaskBuilder aTask() {
        return new TaskBuilder();
    }

    public TaskBuilder withAction(String action) {
        this.action = action;
        return this;
    }

    public TaskBuilder withParam(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    public Task build() {
        return new Task(action, params);
    }
}
```

### 6.3 Test Resources

**Create:** `C:\Users\casey\steve\src\test\resources\`

```
test/
└── resources/
    ├── test_responses/          # Mock LLM responses
    │   ├── valid_task_plan.json
    │   ├── invalid_response.json
    │   └── cached_response.json
    ├── test_structures/         # Test structure templates
    │   ├── simple_house.nbt
    │   └── test_tower.nbt
    └── test_config/             # Test configurations
        └── test-mod-config.toml
```

---

## 7. Testing Best Practices for This Codebase

### 7.1 Test Organization

```
src/test/java/com/minewright/
├── unit/                       # Pure unit tests (no Minecraft deps)
│   ├── action/
│   ├── llm/
│   ├── execution/
│   └── plugin/
├── integration/                # Integration tests (with Minecraft context)
│   ├── action/
│   ├── llm/
│   └── workflow/
├── contract/                   # Contract tests for external APIs
│   └── llm/
└── testutil/                   # Test utilities and fixtures
    ├── fakes/
    ├── mocks/
    └── builders/
```

### 7.2 Naming Conventions

- **Test Classes:** `[ClassName]Test`
- **Test Methods:** `test[MethodName][Scenario]`
  - Example: `testAgentStateMachineValidTransitions`
  - Example: `testLLMCacheEvictionWhenFull`

### 7.3 Test Coverage Goals

| Component | Target Coverage | Priority |
|-----------|----------------|----------|
| AgentStateMachine | 90% | HIGH |
| LLMCache | 85% | HIGH |
| ActionRegistry | 80% | MEDIUM |
| TaskPlanner | 75% | HIGH |
| ActionExecutor | 70% | HIGH |
| PluginManager | 75% | MEDIUM |
| EventBus | 80% | MEDIUM |
| All Actions | 60% | MEDIUM |

### 7.4 Async Testing Guidelines

1. **Always use timeouts** on async operations to prevent hanging tests
2. **Use Awaitility** for waiting on async conditions
3. **Mock time** for time-sensitive operations
4. **Test failure scenarios** (timeouts, exceptions, cancellations)

```java
await().atMost(5, TimeUnit.SECONDS)
    .untilAsserted(() -> {
        assertEquals(AgentState.EXECUTING, machine.getCurrentState());
    });
```

### 7.5 Minecraft-Specific Guidelines

1. **Avoid direct Minecraft API calls in unit tests** - use fakes/mocks
2. **Test Minecraft interactions in integration tests only**
3. **Use test-specific worlds** - don't modify real saves
4. **Clean up test entities** - prevent memory leaks

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Update build.gradle with test dependencies
- [ ] Create test utilities package (fakes, builders)
- [ ] Set up JaCoCo for coverage reporting
- [ ] Write AgentStateMachine tests (high value, low complexity)

### Phase 2: Core Logic (Week 2)
- [ ] Write LLMCache tests (critical for correctness)
- [ ] Write ActionRegistry tests (plugin system foundation)
- [ ] Write EventBus tests (used everywhere)
- [ ] Write TaskPlanner unit tests (parameter validation)

### Phase 3: Actions & Execution (Week 3)
- [ ] Write ActionExecutor tests (core orchestrator)
- [ ] Write BaseAction subclass tests (MineBlock, PlaceBlock, etc.)
- [ ] Write InterceptorChain tests
- [ ] Write ResponseParser tests

### Phase 4: Integration (Week 4)
- [ ] Set up integration test infrastructure
- [ ] Write LLM integration tests
- [ ] Write plugin loading integration tests
- [ ] Write end-to-end workflow tests

### Phase 5: Coverage & Polish (Week 5)
- [ ] Achieve 80% code coverage target
- [ ] Add performance tests for cache/batching
- [ ] Add contract tests for LLM clients
- [ ] Document test patterns for contributors

---

## 9. Continuous Integration

### 9.1 CI Pipeline Configuration

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew test

      - name: Generate coverage report
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

### 9.2 Pre-Commit Hooks

```bash
# .git/hooks/pre-commit
#!/bin/bash
./gradlew test --quiet
if [ $? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi
```

---

## 10. Metrics & Success Criteria

### 10.1 Coverage Metrics

- **Overall Target:** 80% line coverage
- **Critical Components:** 90% coverage
- **Integration Coverage:** 60% (by line count)

### 10.2 Test Quality Metrics

- **Test Execution Time:** < 2 minutes for full suite
- **Flaky Test Rate:** < 1%
- **Test-to-Code Ratio:** > 1:1 (lines of test to lines of code)

### 10.3 Success Indicators

- [ ] All critical paths have integration tests
- [ ] No regressions in existing functionality
- [ ] Tests catch bugs before production
- [ ] New contributors can write tests easily
- [ ] CI/CD pipeline runs tests automatically

---

## 11. Conclusion

The MineWright mod currently has **zero functional test coverage** despite having placeholder test files. This presents significant risk given the complexity of the codebase:

- Async LLM operations
- Concurrent state management
- Plugin architecture
- Tick-based execution
- Cache and rate limiting

**Recommended Priority:**

1. **Immediate (Week 1):** Set up test infrastructure and test critical state management
2. **Short-term (Weeks 2-4):** Test core execution flow and LLM integration
3. **Medium-term (Month 2):** Achieve 80% coverage with integration tests

By implementing this testing strategy, the project will achieve:
- **Higher code quality** through test-driven development
- **Faster development** with fewer regressions
- **Easier onboarding** for new contributors
- **Production readiness** with confidence in core systems

---

## Appendix: References

- **JUnit 5 Documentation:** https://junit.org/junit5/docs/current/user-guide/
- **Mockito Documentation:** https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- **AssertJ Documentation:** https://assertj.github.io/doc/
- **Awaitility Documentation:** https://awaitility.appspot.com/docs/latest/
- **Forge Testing Guide:** https://docs.minecraftforge.net/en/latest/contributing/developer/
- **Minecraft Forge Wiki:** https://mcforge.readthedocs.io/

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Claude (Testing Strategy Analysis)
