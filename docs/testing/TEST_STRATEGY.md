# MineWright Testing Strategy

## Executive Summary

This document provides a comprehensive testing strategy for the MineWright Minecraft mod (formerly MineWright AI), an AI-driven autonomous agent system for Minecraft 1.20.1. The codebase implements complex async operations, state machines, plugin architecture, and LLM integration requiring robust testing.

**Current State:** Minimal test coverage with only placeholder test files.

**Target:** Comprehensive unit, integration, and contract testing with >80% coverage.

**Key Challenges:**
- Minecraft Forge dependencies require specialized mocking
- Async LLM operations need careful testing patterns
- Tick-based execution requires simulation approaches
- Plugin architecture demands isolation testing

---

## 1. Current Testing Status

### 1.1 Existing Tests

Located in `src/test/java/com/minewright/`:

| Test File | Status | Lines | Coverage |
|-----------|--------|-------|----------|
| `action/ActionExecutorTest.java` | Placeholder (TODO only) | 17 | 0% |
| `llm/TaskPlannerTest.java` | Placeholder (TODO only) | 17 | 0% |
| `memory/WorldKnowledgeTest.java` | Placeholder (TODO only) | 17 | 0% |
| `structure/StructureGeneratorsTest.java` | Placeholder (TODO only) | 17 | 0% |

**Finding:** All existing tests are empty skeletons with TODO comments. No actual implementations.

### 1.2 Test Infrastructure Analysis

**Build Configuration (`build.gradle`):**
```gradle
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.3'
```

**Missing Critical Dependencies:**
- Mockito (mocking framework)
- AssertJ (rich assertions)
- Awaitility (async testing)
- JaCoCo (code coverage)
- Minecraft Forge test fixtures

---

## 2. Architecture Analysis & Test Coverage Gaps

### 2.1 Critical Components Requiring Tests

#### HIGH PRIORITY - Core Execution

| Component | Lines | Complexity | Risk | Test Focus |
|-----------|-------|------------|------|------------|
| `ActionExecutor` | 507 | High | HIGH | Tick execution, async coordination, queue management |
| `AgentStateMachine` | 285 | Medium | HIGH | State transitions, thread safety, event publishing |
| `TaskPlanner` | 368 | High | HIGH | Async planning, batching, fallback logic |
| `LLMCache` | ~200 | Medium | HIGH | Concurrency, eviction, TTL |
| `BatchingLLMClient` | ~350 | High | HIGH | Rate limiting, queue management |

#### HIGH PRIORITY - Actions

| Component | Lines | Complexity | Test Focus |
|-----------|-------|------------|------------|
| `MineBlockAction` | 376 | High | Mining logic, torch placement, direction handling |
| `PlaceBlockAction` | 86 | Low | Block placement validation |
| `BuildStructureAction` | ~200 | Medium | Structure generation, block placement |
| `BaseAction` | 50 | Low | Lifecycle management, cancellation |

#### MEDIUM PRIORITY - Plugin System

| Component | Lines | Complexity | Test Focus |
|-----------|-------|------------|------------|
| `ActionRegistry` | 270 | Medium | Registration, priority resolution, thread safety |
| `PluginManager` | ~300 | High | SPI loading, dependency resolution |
| `ActionFactory` | Interface | Low | Factory contract compliance |

#### MEDIUM PRIORITY - Event System

| Component | Lines | Complexity | Test Focus |
|-----------|-------|------------|------------|
| `SimpleEventBus` | ~100 | Low | Pub/sub, listener management |
| `EventPublishingInterceptor` | ~80 | Low | Event emission on actions |
| `StateTransitionEvent` | ~50 | Low | Event payload validation |

#### MEDIUM PRIORITY - LLM Integration

| Component | Lines | Complexity | Test Focus |
|-----------|-------|------------|------------|
| `ResponseParser` | 148 | Medium | JSON parsing, error handling, extraction |
| `AsyncLLMClient` implementations | ~150 each | High | Async execution, error handling |
| `PromptBuilder` | ~200 | Medium | Prompt construction, context building |
| `FallbackResponseSystem` | ~100 | Low | Fallback logic |

#### LOW PRIORITY - Utilities

| Component | Lines | Complexity | Test Focus |
|-----------|-------|------------|------------|
| `WorldKnowledge` | 147 | Low | Scanning logic, block detection |
| `StructureGenerators` | 369 | Low | Structure algorithms, block placement |
| `ServiceContainer` | ~50 | Low | DI container behavior |
| `BlockNameMapper` | ~100 | Low | Block name normalization |

### 2.2 Risk Assessment Matrix

```
HIGH RISK (Test Immediately):
- AgentStateMachine: Thread safety, complex state transitions
- LLMCache: Concurrency bugs, cache correctness
- BatchingLLMClient: Rate limiting, async coordination
- ActionExecutor: Tick execution, async planning integration
- MineBlockAction: Complex world interaction

MEDIUM RISK (Test Soon):
- ActionRegistry: Singleton state, concurrent registration
- ResponseParser: JSON parsing, error recovery
- TaskPlanner: Multiple code paths, fallback logic
- PluginManager: Dependency resolution, SPI loading

LOW RISK (Test Eventually):
- StructureGenerators: Pure functions, low complexity
- WorldKnowledge: Simple scanning logic
- EventBus: Standard pub/sub pattern
```

---

## 3. Mock Strategies for Minecraft Forge

### 3.1 Challenge: Minecraft Dependencies

Minecraft Forge has deep integration with:
- `net.minecraft.world.level.Level` - World access
- `net.minecraft.world.entity.Entity` - Entity base class
- `net.minecraft.core.BlockPos` - Position handling
- `net.minecraft.world.level.block.state.BlockState` - Block data
- `net.minecraftforge.event.*` - Event system

### 3.2 Mock Strategy: Interface Extraction

**Pattern:** Extract interfaces for Minecraft-dependent code.

```java
// Create testable interface
public interface WorldAccessor {
    BlockState getBlockState(BlockPos pos);
    void setBlock(BlockPos pos, BlockState state);
    List<Entity> getEntitiesInRange(AABB range);
}

// Implementation uses real Minecraft objects
public class ForgeWorldAccessor implements WorldAccessor {
    private final Level level;

    public ForgeWorldAccessor(Level level) {
        this.level = level;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }
    // ...
}

// Test uses fake
public class FakeWorldAccessor implements WorldAccessor {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public void setBlock(BlockPos pos, BlockState state) {
        blocks.put(pos, state);
    }
    // ...
}
```

### 3.3 Mock Strategy: Fake Entity

```java
public class FakeForemanEntity extends ForemanEntity {
    private final FakeServerLevel testLevel;
    private final ForemanMemory memory;
    private final ActionExecutor executor;
    private String name = "TestSteve";

    public FakeForemanEntity(FakeServerLevel testLevel) {
        super(EntityTypes.FOREMAN, testLevel);
        this.testLevel = testLevel;
        this.memory = new ForemanMemory(this);
        this.executor = new ActionExecutor(this);
    }

    @Override
    public ServerLevel level() {
        return testLevel;
    }

    @Override
    public ForemanMemory getMemory() {
        return memory;
    }

    @Override
    public String getSteveName() {
        return name;
    }

    @Override
    public BlockPos blockPosition() {
        return new BlockPos(0, 64, 0);
    }

    @Override
    public void sendChatMessage(String message) {
        // Track messages for testing
        testLevel.addChatMessage(name, message);
    }

    // Test-specific methods
    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSentMessages() {
        return testLevel.getChatMessages(name);
    }
}
```

### 3.4 Mock Strategy: Fake Server Level

```java
public class FakeServerLevel implements ServerLevel {
    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();
    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private final Map<String, List<String>> chatMessages = new ConcurrentHashMap<>();

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        blocks.put(pos, state);
        return true;
    }

    @Override
    public List<Entity> getEntities(Entity except, AABB boundingBox) {
        return entities.stream()
            .filter(e -> e.boundingBox.intersects(boundingBox))
            .filter(e -> e != except)
            .toList();
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropDrops) {
        blocks.remove(pos);
        return true;
    }

    // Test-specific methods
    public void setBlock(BlockPos pos, Block block) {
        blocks.put(pos, block.defaultBlockState());
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void addChatMessage(String sender, String message) {
        chatMessages.computeIfAbsent(sender, k -> new ArrayList<>()).add(message);
    }

    public List<String> getChatMessages(String sender) {
        return chatMessages.getOrDefault(sender, List.of());
    }

    public void clear() {
        blocks.clear();
        entities.clear();
        chatMessages.clear();
    }
}
```

### 3.5 Mock Strategy: Mockito for Simple Cases

```java
@ExtendWith(MockitoExtension.class)
class ActionExecutorTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private ServerLevel mockLevel;

    @BeforeEach
    void setUp() {
        lenient().when(mockForeman.level()).thenReturn(mockLevel);
        lenient().when(mockForeman.getSteveName()).thenReturn("TestSteve");
        lenient().when(mockForeman.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
    }

    @Test
    void testProcessNaturalLanguageCommand() {
        ActionExecutor executor = new ActionExecutor(mockForeman);

        executor.processNaturalLanguageCommand("Build a house");

        assertTrue(executor.isPlanning());
    }
}
```

---

## 4. Unit Testing Approach for Actions

### 4.1 Base Action Testing

Test the lifecycle that all actions inherit:

```java
class BaseActionTest {

    @Test
    void testActionLifecycle() {
        FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());
        Task task = new Task("test", Map.of());
        TestAction action = new TestAction(foreman, task);

        // Initial state
        assertFalse(action.isComplete());
        assertFalse(action.started);

        // Start action
        action.start();
        assertTrue(action.started);
        assertFalse(action.isComplete());

        // Complete action
        action.complete();
        assertTrue(action.isComplete());
        ActionResult result = action.getResult();
        assertTrue(result.isSuccess());
    }

    @Test
    void testActionCancellation() {
        FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());
        Task task = new Task("test", Map.of());
        TestAction action = new TestAction(foreman, task);

        action.start();
        action.cancel();

        assertTrue(action.isComplete());
        assertFalse(action.getResult().isSuccess());
        assertTrue(action.cancelled);
    }

    // Test implementation
    private static class TestAction extends BaseAction {
        public TestAction(ForemanEntity foreman, Task task) {
            super(foreman, task);
        }

        @Override
        protected void onStart() {
            // Test setup
        }

        @Override
        protected void onTick() {
            // Test tick behavior
        }

        @Override
        protected void onCancel() {
            // Test cleanup
        }

        @Override
        public String getDescription() {
            return "Test action";
        }

        public void complete() {
            result = ActionResult.success("Test complete");
        }
    }
}
```

### 4.2 MineBlockAction Testing

```java
class MineBlockActionTest {
    private FakeServerLevel testLevel;
    private FakeForemanEntity foreman;

    @BeforeEach
    void setUp() {
        testLevel = new FakeServerLevel();
        foreman = new FakeForemanEntity(testLevel);
    }

    @Test
    void testMineValidBlock() {
        // Place stone blocks
        BlockPos stonePos = new BlockPos(0, 64, 0);
        testLevel.setBlock(stonePos, Blocks.STONE);

        // Create mining task
        Task task = new Task("mine", Map.of(
            "block", "stone",
            "quantity", 1
        ));

        MineBlockAction action = new MineBlockAction(foreman, task);
        action.start();

        // Simulate ticks until complete
        int ticks = 0;
        while (!action.isComplete() && ticks < 1000) {
            action.tick();
            ticks++;
        }

        // Verify result
        assertTrue(action.isComplete());
        assertTrue(action.getResult().isSuccess());
        assertEquals(Blocks.AIR, testLevel.getBlockState(stonePos).getBlock());
    }

    @Test
    void testMineInvalidBlock() {
        Task task = new Task("mine", Map.of(
            "block", "invalid_block",
            "quantity", 1
        ));

        MineBlockAction action = new MineBlockAction(foreman, task);
        action.start();

        // Should fail immediately
        assertTrue(action.isComplete());
        assertFalse(action.getResult().isSuccess());
        assertTrue(action.getResult().getMessage().contains("Invalid block"));
    }

    @Test
    void testMineBlockWithParameters() {
        Task task = new Task("mine", Map.of(
            "block", "diamond_ore",
            "quantity", 5
        ));

        MineBlockAction action = new MineBlockAction(foreman, task);
        action.start();

        // Verify description
        String description = action.getDescription();
        assertTrue(description.contains("diamond_ore"));
        assertTrue(description.contains("5"));
    }
}
```

### 4.3 PlaceBlockAction Testing

```java
class PlaceBlockActionTest {
    private FakeServerLevel testLevel;
    private FakeForemanEntity foreman;

    @BeforeEach
    void setUp() {
        testLevel = new FakeServerLevel();
        foreman = new FakeForemanEntity(testLevel);
    }

    @Test
    void testPlaceBlockInAir() {
        BlockPos targetPos = new BlockPos(5, 64, 5);
        Task task = new Task("place", Map.of(
            "block", "oak_planks",
            "x", 5,
            "y", 64,
            "z", 5
        ));

        PlaceBlockAction action = new PlaceBlockAction(foreman, task);
        action.start();

        // Simulate ticks
        int ticks = 0;
        while (!action.isComplete() && ticks < 200) {
            action.tick();
            ticks++;
        }

        // Verify block placed
        assertTrue(action.isComplete());
        assertEquals(Blocks.OAK_PLANKS, testLevel.getBlockState(targetPos).getBlock());
    }

    @Test
    void testPlaceBlockInOccupiedSpace() {
        BlockPos targetPos = new BlockPos(5, 64, 5);
        testLevel.setBlock(targetPos, Blocks.STONE);

        Task task = new Task("place", Map.of(
            "block", "oak_planks",
            "x", 5,
            "y", 64,
            "z", 5
        ));

        PlaceBlockAction action = new PlaceBlockAction(foreman, task);
        action.start();

        // Should fail - position not empty
        assertTrue(action.isComplete());
        assertFalse(action.getResult().isSuccess());
    }
}
```

---

## 5. Integration Testing for LLM Calls

### 5.1 Challenge: External API Dependencies

LLM calls have:
- Network latency (30-60 seconds)
- Rate limits
- API costs
- Non-deterministic responses

### 5.2 Strategy: Contract Testing

Define contracts for LLM behavior and test against those.

```java
interface LLMClientContract {
    CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params);
    boolean isHealthy();
    void close();
}

// Production implementation
class AsyncOpenAIClient implements LLMClientContract {
    // Real OpenAI calls
}

// Test implementation
class FakeLLMClient implements LLMClientContract {
    private final Map<String, LLMResponse> responses;
    private boolean healthy = true;

    public FakeLLMClient(Map<String, LLMResponse> responses) {
        this.responses = responses;
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        if (!healthy) {
            return CompletableFuture.failedFuture(new LLMException("Service unhealthy"));
        }

        // Return based on prompt content
        String key = prompt.hashCode() + "";
        LLMResponse response = responses.get(key);

        if (response == null) {
            return CompletableFuture.failedFuture(new LLMException("No response configured"));
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setUnhealthy() {
        this.healthy = false;
    }
}
```

### 5.3 Integration Test: Task Planning

```java
@ExtendWith(MockitoExtension.class)
class TaskPlannerIntegrationTest {

    @Test
    void testTaskPlanningWithFakeLLM() {
        // Create fake LLM with predefined response
        String jsonResponse = """
            {
              "reasoning": "User wants to build a house",
              "plan": "Build a small wooden house with 5x5 footprint",
              "tasks": [
                {
                  "action": "gather",
                  "parameters": {"resource": "oak_log", "quantity": 20}
                },
                {
                  "action": "build",
                  "parameters": {"structure": "house", "blocks": ["oak_planks"], "dimensions": [5, 4, 5]}
                }
              ]
            }
            """;

        LLMResponse llmResponse = new LLMResponse(jsonResponse, 500, 150, false);
        FakeLLMClient fakeLLM = new FakeLLMClient(Map.of(
            "build".hashCode() + "", llmResponse
        ));

        // Inject fake client
        TaskPlanner planner = new TaskPlanner(fakeLLM);
        FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());

        // Execute
        CompletableFuture<ParsedResponse> future = planner.planTasksAsync(foreman, "Build a house");

        // Wait for completion
        await().atMost(5, TimeUnit.SECONDS)
            .until(future::isDone);

        ParsedResponse result = future.join();

        // Verify
        assertNotNull(result);
        assertEquals(2, result.getTasks().size());
        assertEquals("gather", result.getTasks().get(0).getAction());
        assertEquals("build", result.getTasks().get(1).getAction());
    }

    @Test
    void testTaskPlanningWithLLMFailure() {
        FakeLLMClient fakeLLM = new FakeLLMClient(Map.of());
        fakeLLM.setUnhealthy();

        TaskPlanner planner = new TaskPlanner(fakeLLM);
        FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());

        CompletableFuture<ParsedResponse> future = planner.planTasksAsync(foreman, "Build a house");

        await().atMost(5, TimeUnit.SECONDS)
            .until(future::isDone);

        ParsedResponse result = future.join();
        assertNull(result);
    }
}
```

### 5.4 Integration Test: Response Parser

```java
class ResponseParserIntegrationTest {

    @Test
    void testParseValidResponse() {
        String validJson = """
            {
              "reasoning": "Test reasoning",
              "plan": "Test plan",
              "tasks": [
                {
                  "action": "mine",
                  "parameters": {
                    "block": "stone",
                    "quantity": 10
                  }
                }
              ]
            }
            """;

        ParsedResponse result = ResponseParser.parseAIResponse(validJson);

        assertNotNull(result);
        assertEquals("Test reasoning", result.getReasoning());
        assertEquals("Test plan", result.getPlan());
        assertEquals(1, result.getTasks().size());

        Task task = result.getTasks().get(0);
        assertEquals("mine", task.getAction());
        assertEquals("stone", task.getStringParameter("block"));
        assertEquals(10, task.getIntParameter("quantity"));
    }

    @Test
    void testParseResponseWithMarkdownCodeBlocks() {
        String responseWithMarkdown = """
            Here's my plan:

            ```json
            {
              "plan": "Build structure",
              "tasks": []
            }
            ```

            Hope this helps!
            """;

        ParsedResponse result = ResponseParser.parseAIResponse(responseWithMarkdown);

        assertNotNull(result);
        assertEquals("Build structure", result.getPlan());
    }

    @Test
    void testParseMalformedResponse() {
        String malformedJson = """
            {
              "plan": "test",
              "tasks": [
                {"action": "mine", "parameters": {"block": "stone"}}
                {"action": "build", "parameters": {"structure": "house"}}
              ]
            }
            """;

        // Should fix missing comma
        ParsedResponse result = ResponseParser.parseAIResponse(malformedJson);

        assertNotNull(result);
        assertEquals(2, result.getTasks().size());
    }
}
```

---

## 6. Performance Testing Approach

### 6.1 Cache Performance Testing

```java
class LLMCachePerformanceTest {

    @Test
    void testCacheConcurrency() throws InterruptedException {
        LLMCache cache = new LLMCache();
        int threads = 10;
        int operationsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        long startTime = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = threadId + "-" + (i % 100); // 100 unique keys
                        LLMResponse response = new LLMResponse("content", 100, 500, false);
                        cache.put(key, "model", "provider", response);
                        cache.get(key, "model", "provider");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long duration = System.nanoTime() - startTime;

        executor.shutdown();

        // Verify cache correctness
        assertEquals(100, cache.size());

        // Log performance
        double opsPerMs = (threads * operationsPerThread * 2.0) / (duration / 1_000_000.0);
        System.out.println("Cache operations: " + opsPerMs + " ops/ms");

        // Should handle at least 1000 ops/ms
        assertTrue(opsPerMs > 1000, "Cache too slow: " + opsPerMs + " ops/ms");
    }

    @Test
    void testCacheEvictionPerformance() {
        LLMCache cache = new LLMCache();

        // Fill cache beyond capacity (500 entries)
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            LLMResponse response = new LLMResponse("content" + i, 100, 500, false);
            cache.put("key" + i, "model", "provider", response);
        }
        long duration = System.nanoTime() - startTime;

        // Verify size maintained
        assertEquals(500, cache.size());

        // Verify eviction didn't degrade performance
        double msPerOp = (duration / 1_000_000.0) / 1000.0;
        assertTrue(msPerOp < 1.0, "Eviction too slow: " + msPerOp + " ms/op");
    }
}
```

### 6.2 Action Executor Performance

```java
class ActionExecutorPerformanceTest {

    @Test
    void testTickPerformance() {
        FakeServerLevel testLevel = new FakeServerLevel();
        FakeForemanEntity foreman = new FakeForemanEntity(testLevel);
        ActionExecutor executor = new ActionExecutor(foreman);

        // Queue multiple tasks
        for (int i = 0; i < 100; i++) {
            Task task = new Task("mine", Map.of("block", "stone", "quantity", 1));
            executor.queueTask(task);
        }

        // Measure tick performance
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            executor.tick();
        }
        long duration = System.nanoTime() - startTime;

        double avgTickTimeMs = (duration / 1_000_000.0) / 1000.0;

        // Each tick should be very fast (< 1ms)
        assertTrue(avgTickTimeMs < 1.0, "Tick too slow: " + avgTickTimeMs + " ms");
        System.out.println("Average tick time: " + avgTickTimeMs + " ms");
    }
}
```

---

## 7. Regression Testing for Commands

### 7.1 Command Test Suite

```java
class CommandRegressionTest {

    @ParameterizedTest
    @CsvSource({
        // Basic commands
        "'mine 10 stone', mine, stone, 10",
        "'build a house', build, house, 0",
        "'craft 5 swords', craft, swords, 5",

        // Complex commands
        "'build a 5x5x4 wooden house with oak planks', build, house, 0",
        "'mine 20 iron ore and smelt it', mine, iron_ore, 20",

        // Edge cases
        "'', , , 0",
        "'invalid command', , , 0"
    })
    void testCommandParsing(String command, String expectedAction, String expectedBlock, int expectedQuantity) {
        TaskPlanner planner = new TaskPlanner();

        // Test validation
        if (expectedAction != null && !expectedAction.isEmpty()) {
            Task task = new Task(expectedAction,
                expectedBlock != null ? Map.of("block", expectedBlock, "quantity", expectedQuantity) : Map.of());
            assertTrue(planner.validateTask(task),
                "Command should be valid: " + command);
        }
    }

    @Test
    void testCommonCommandPatterns() {
        // Test regex patterns used in command processing
        String[] patterns = {
            "mine (\\d+) (.+)",
            "build (a )?(.+)",
            "craft (\\d+) (.+)",
            "place (.+) at (\\d+), (\\d+), (\\d+)",
            "attack (.+)",
            "follow (.+)"
        };

        // Verify patterns compile
        for (String pattern : patterns) {
            assertDoesNotThrow(() -> Pattern.compile(pattern),
                "Pattern should compile: " + pattern);
        }
    }
}
```

### 7.2 LLM Response Regression Tests

```java
class LLMResponseRegressionTest {

    @Test
    void testStandardResponses() {
        // Load saved responses from resources
        String[] responseFiles = {
            "test_responses/valid_mining_response.json",
            "test_responses/valid_building_response.json",
            "test_responses/valid_crafting_response.json",
            "test_responses/malformed_response_fixed.json"
        };

        for (String file : responseFiles) {
            String response = loadTestResource(file);
            ParsedResponse parsed = ResponseParser.parseAIResponse(response);

            assertNotNull(parsed, "Should parse: " + file);
            assertFalse(parsed.getTasks().isEmpty(), "Should have tasks: " + file);

            // Validate all tasks
            for (Task task : parsed.getTasks()) {
                TaskPlanner planner = new TaskPlanner();
                assertTrue(planner.validateTask(task),
                    "Task should be valid in " + file + ": " + task.getAction());
            }
        }
    }

    private String loadTestResource(String path) {
        try {
            return Files.readString(Paths.get("src/test/resources/" + path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test resource: " + path, e);
        }
    }
}
```

---

## 8. Priority Test Cases

### 8.1 Critical Path Tests (Must Have)

```java
// 1. Async planning doesn't block game thread
@Test
void testAsyncPlanningNonBlocking() {
    FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());
    ActionExecutor executor = new ActionExecutor(foreman);

    long startTime = System.nanoTime();
    executor.processNaturalLanguageCommand("Build a house");
    long duration = System.nanoTime() - startTime;

    // Should return immediately (< 10ms)
    assertTrue(duration < 10_000_000, "Should not block: " + duration / 1_000_000 + " ms");
    assertTrue(executor.isPlanning(), "Should be planning");
}

// 2. State machine prevents invalid transitions
@Test
void testStateMachineInvalidTransitions() {
    EventBus bus = new SimpleEventBus();
    AgentStateMachine machine = new AgentStateMachine(bus, "test");

    // Can't go from IDLE to EXECUTING (must go through PLANNING)
    assertFalse(machine.canTransitionTo(AgentState.EXECUTING));
    assertFalse(machine.transitionTo(AgentState.EXECUTING));
    assertEquals(AgentState.IDLE, machine.getCurrentState());
}

// 3. Cache is thread-safe
@Test
void testCacheThreadSafety() throws InterruptedException {
    LLMCache cache = new LLMCache();
    int threads = 5;
    int opsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);

    for (int t = 0; t < threads; t++) {
        executor.submit(() -> {
            try {
                for (int i = 0; i < opsPerThread; i++) {
                    cache.put("key" + i, "model", "provider",
                        new LLMResponse("content", 100, 500, false));
                    cache.get("key" + i, "model", "provider");
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executor.shutdown();

    // Should not throw exceptions, size should be consistent
    assertEquals(opsPerThread, cache.size());
}

// 4. Action cancellation cleans up properly
@Test
void testActionCancellation() {
    FakeServerLevel level = new FakeServerLevel();
    FakeForemanEntity foreman = new FakeForemanEntity(level);

    Task task = new Task("mine", Map.of("block", "stone", "quantity", 10));
    MineBlockAction action = new MineBlockAction(foreman, task);

    action.start();
    action.cancel();

    assertTrue(action.isComplete());
    assertTrue(action.cancelled);
    assertFalse(action.getResult().isSuccess());
}

// 5. Registry resolves conflicts correctly
@Test
void testActionRegistryPriorityResolution() {
    ActionRegistry registry = ActionRegistry.getInstance();
    registry.clear();

    ActionFactory lowPriority = (f, t, c) -> mock(BaseAction.class);
    ActionFactory highPriority = (f, t, c) -> mock(BaseAction.class);

    registry.register("action", lowPriority, 1, "plugin1");
    registry.register("action", highPriority, 10, "plugin2");

    assertEquals("plugin2", registry.getPluginForAction("action"));
}
```

### 8.2 Edge Case Tests (Should Have)

```java
// Empty/malformed LLM responses
@Test
void testEmptyLLMResponse() {
    ParsedResponse result = ResponseParser.parseAIResponse("");
    assertNull(result);
}

@Test
void testMalformedJSONWithTypicalAIErrors() {
    String malformed = """
        {
          "plan": "test",
          "tasks": [
            {"action": "mine"}{"action": "build"}
          ]
        }
        """;

    ParsedResponse result = ResponseParser.parseAIResponse(malformed);
    assertNotNull(result);
    assertEquals(2, result.getTasks().size());
}

// Task queue overflow
@Test
void testTaskQueueOverflow() {
    FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());
    ActionExecutor executor = new ActionExecutor(foreman);

    // Add many tasks
    for (int i = 0; i < 1000; i++) {
        executor.queueTask(new Task("test", Map.of()));
    }

    // Should handle gracefully
    executor.tick();
    assertTrue(executor.isExecuting());
}

// Rapid state changes
@Test
void testRapidStateTransitions() {
    EventBus bus = new SimpleEventBus();
    AgentStateMachine machine = new AgentStateMachine(bus, "test");

    for (int i = 0; i < 100; i++) {
        machine.transitionTo(AgentState.PLANNING);
        machine.transitionTo(AgentState.EXECUTING);
        machine.transitionTo(AgentState.COMPLETED);
        machine.reset();
    }

    assertEquals(AgentState.IDLE, machine.getCurrentState());
}

// Network timeout simulation
@Test
void testLLMTimeout() {
    FakeLLMClient slowLLM = new FakeLLMClient(Map.of()) {
        @Override
        public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
            // Never completes
            return new CompletableFuture<>();
        }
    };

    TaskPlanner planner = new TaskPlanner(slowLLM);
    FakeForemanEntity foreman = new FakeForemanEntity(new FakeServerLevel());

    CompletableFuture<ParsedResponse> future = planner.planTasksAsync(foreman, "test");

    // Should timeout or handle gracefully
    assertThrows(TimeoutException.class, () -> {
        future.get(5, TimeUnit.SECONDS);
    });
}
```

---

## 9. Build Configuration

### 9.1 Updated build.gradle

```gradle
dependencies {
    // Minecraft
    minecraft 'net.minecraftforge:forge:1.20.1-47.4.16'

    // Existing dependencies...

    // Testing dependencies
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.3'

    // Mocking
    testImplementation 'org.mockito:mockito-core:5.3.1'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.3.1'

    // Assertions
    testImplementation 'org.assertj:assertj-core:3.24.2'

    // Async testing
    testImplementation 'org.awaitility:awaitility:4.2.0'

    // Parameterized tests
    testImplementation 'org.junit.jupiter:junit-jupiter-params:5.9.3'
}

test {
    useJUnitPlatform()

    // Configuration
    maxParallelForks = 1
    jvmArgs += '-Dforge.logging.console.level=debug'

    // Fail on test issues
    failFast = false

    // Coverage
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
        csv.required = false
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/entity/**',
                '**/client/**',
                '**/testutil/**'
            ])
        }))
    }
}

test {
    jacoco {
        excludes = [
            '**/entity/**',
            '**/client/**',
            '**/testutil/**'
        ]
    }
}
```

---

## 10. Implementation Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Update build.gradle with test dependencies
- [ ] Create test utilities package
- [ ] Implement FakeForemanEntity, FakeServerLevel
- [ ] Set up JaCoCo coverage reporting
- [ ] Write AgentStateMachine tests
- [ ] Write LLMCache tests

### Phase 2: Core Logic (Week 2)
- [ ] Write ActionRegistry tests
- [ ] Write EventBus tests
- [ ] Write TaskPlanner validation tests
- [ ] Write ResponseParser tests
- [ ] Create test resources directory with sample responses

### Phase 3: Actions (Week 3)
- [ ] Write BaseAction lifecycle tests
- [ ] Write MineBlockAction tests
- [ ] Write PlaceBlockAction tests
- [ ] Write BuildStructureAction tests
- [ ] Write ActionExecutor tick tests

### Phase 4: Integration (Week 4)
- [ ] Create FakeLLMClient for testing
- [ ] Write TaskPlanner integration tests
- [ ] Write plugin loading tests
- [ ] Write end-to-end workflow tests
- [ ] Add performance benchmarks

### Phase 5: Coverage & Polish (Week 5)
- [ ] Achieve 80% code coverage
- [ ] Add regression test suite
- [ ] Document testing patterns
- [ ] Set up CI pipeline
- [ ] Create testing guide for contributors

---

## 11. Success Metrics

### Coverage Targets
- **Overall:** 80% line coverage
- **Critical Components:** 90% coverage
  - AgentStateMachine
  - LLMCache
  - ActionRegistry
  - TaskPlanner

### Quality Metrics
- **Test Execution Time:** < 3 minutes for full suite
- **Flaky Test Rate:** < 1%
- **Test-to-Code Ratio:** > 1:1

### Functional Metrics
- [ ] All critical paths have integration tests
- [ ] No regressions in existing functionality
- [ ] Tests catch bugs before production
- [ ] CI/CD pipeline runs tests automatically

---

**Document Version:** 2.0
**Last Updated:** 2026-02-27
**Author:** Testing Strategy Analysis
**Project:** MineWright (MineWright AI) - Minecraft 1.20.1 Mod
