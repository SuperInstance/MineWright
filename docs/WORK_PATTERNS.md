# Work Patterns - MineWright Project

**Version:** 1.0
**Created:** 2026-03-04
**Purpose:** Document established patterns and best practices for consistent development

---

## Overview

This document catalogs the patterns, practices, and conventions established through years of development on MineWright. Following these patterns ensures consistency, maintainability, and quality.

---

## 1. Refactoring Patterns

### 1.1 God Class Elimination Pattern

**Problem:** A class grows too large (>500 lines) with multiple responsibilities.

**Solution:** Extract specialized components and delegate.

**Example (SmartCascadeRouter Refactoring):**

```java
// BEFORE: God class with 899 lines, 7 responsibilities
public class SmartCascadeRouter {
    public ComplexityScore analyzeComplexity(String prompt) { ... }
    public ModelTier selectTier(ComplexityScore score) { ... }
    public boolean isCacheable(String prompt) { ... }
    public String getCached(String prompt) { ... }
    public CompletableFuture<String> routeAsync(String prompt) { ... }
    public void updateMetrics(RouteResult result) { ... }
    public MetricsReport generateReport() { ... }
}

// AFTER: Coordinator (254 lines) + 3 specialists
public class SmartCascadeRouter {
    private final ComplexityAnalyzer complexityAnalyzer;  // 150 lines
    private final ModelSelector modelSelector;            // 120 lines
    private final RoutingMetrics metrics;                 // 100 lines

    public CompletableFuture<String> routeAsync(String prompt) {
        ComplexityScore score = complexityAnalyzer.analyze(prompt);
        ModelTier tier = modelSelector.selectTier(score);
        return executeWithTier(prompt, tier);
    }
}
```

**Steps:**
1. List all responsibilities of the class
2. Group related methods into cohesive components
3. Create new classes for each component
4. Have original class delegate to components
5. Maintain original public API
6. Add/update tests

**When to Apply:**
- Class exceeds 500 lines
- Class has more than 5 distinct responsibilities
- Changes to one feature require understanding unrelated code

### 1.2 Extract Component Pattern

**Problem:** A method is too long or does too much.

**Solution:** Extract to a focused helper class.

```java
// BEFORE: Long method in main class
public class ActionExecutor {
    public void executeTasks(List<Task> tasks) {
        // 50 lines of validation
        // 30 lines of ordering
        // 40 lines of execution
        // 20 lines of cleanup
    }
}

// AFTER: Delegated to components
public class ActionExecutor {
    private final TaskValidator validator;
    private final TaskOrderer orderer;
    private final TaskRunner runner;

    public void executeTasks(List<Task> tasks) {
        validator.validate(tasks);
        List<Task> ordered = orderer.orderByDependencies(tasks);
        runner.executeAll(ordered);
    }
}
```

---

## 2. Thread Safety Patterns

### 2.1 Atomic Check-and-Act Pattern

**Problem:** Race condition when checking state and then acting on it.

```java
// DANGEROUS: Race condition
if (!map.containsKey(key)) {
    map.put(key, value);  // Another thread might have put first!
}
```

**Solution:** Use atomic methods.

```java
// SAFE: Atomic operation
map.computeIfAbsent(key, k -> createValue(k));
```

**Applied In:**
- `CollaborativeBuildManager.assignForemanToSection()` - uses `computeIfAbsent()`
- `OrchestratorService.assignTaskToAgent()` - uses `putIfAbsent()`

### 2.2 Idempotent Operations Pattern

**Problem:** Duplicate operations under concurrent access.

**Solution:** Use idempotent methods that don't overwrite existing values.

```java
// DANGEROUS: Silently overwrites
workerAssignments.put(agentId, assignment);

// SAFE: Detects and handles duplicates
TaskAssignment existing = workerAssignments.putIfAbsent(agentId, assignment);
if (existing != null) {
    LOGGER.warn("Worker {} already has assignment", agentId);
    return;
}
```

### 2.3 Timeout Protection Pattern

**Problem:** Async operations can hang indefinitely.

**Solution:** Always add timeouts to blocking operations.

```java
// DANGEROUS: Could block forever
ResponseParser.ParsedResponse response = future.get();

// SAFE: Timeout with recovery
try {
    ResponseParser.ParsedResponse response = future.get(1, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    LOGGER.error("Operation timed out");
    stateMachine.forceTransition(AgentState.IDLE, "timeout");
}
```

### 2.4 Snapshot Iteration Pattern

**Problem:** ConcurrentModificationException during iteration.

**Solution:** Create snapshot before iteration.

```java
// DANGEROUS: Concurrent modification risk
for (SubscriberEntry<?> entry : subscribers) {
    // If unsubscribe happens during iteration, exception!
}

// SAFE: Snapshot iteration
Object[] snapshot = subscribers.toArray();
for (Object entryObj : snapshot) {
    SubscriberEntry<?> entry = (SubscriberEntry<?>) entryObj;
    // Safe even if unsubscribed during iteration
}
```

---

## 3. Non-Blocking Design Patterns

### 3.1 Tick-Based Execution Pattern

**Problem:** Blocking operations freeze the game (20 TPS requirement).

**Solution:** Break work into small chunks, do a little each tick.

```java
public abstract class BaseAction {
    private boolean completed = false;

    // Called 20 times per second
    protected abstract void onTick();

    public final void tick() {
        if (!completed) {
            onTick();
        }
    }

    public boolean isComplete() {
        return completed;
    }
}
```

**Rules:**
- Never call `Thread.sleep()` in tick methods
- Never block on I/O in tick methods
- Never call `future.get()` without timeout in tick methods
- Return quickly if work can continue next tick

### 3.2 Async Result Pattern

**Problem:** Need to process async results without blocking.

**Solution:** Poll `isDone()` in tick loop, process when ready.

```java
public class ActionExecutor {
    private CompletableFuture<Result> pendingFuture;

    public void tick() {
        if (pendingFuture != null && pendingFuture.isDone()) {
            try {
                Result result = pendingFuture.get(1, TimeUnit.SECONDS);
                handleResult(result);
            } catch (TimeoutException e) {
                handleTimeout();
            }
            pendingFuture = null;
        }
    }
}
```

---

## 4. Plugin Architecture Pattern

### 4.1 Action Registration Pattern

**Problem:** Need extensible action system without core code changes.

**Solution:** Plugin architecture with factory registration.

```java
// 1. Define factory interface
@FunctionalInterface
public interface ActionFactory {
    BaseAction create(ForemanEntity foreman, Task task, ActionContext ctx);
}

// 2. Create registry
public class ActionRegistry {
    private final Map<String, ActionFactory> factories = new ConcurrentHashMap<>();

    public void register(String actionType, ActionFactory factory) {
        factories.put(actionType, factory);
    }

    public BaseAction create(String actionType, ForemanEntity foreman, Task task) {
        ActionFactory factory = factories.get(actionType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown action: " + actionType);
        }
        return factory.create(foreman, task, ActionContext.DEFAULT);
    }
}

// 3. Register in plugin
public class CoreActionsPlugin implements Plugin {
    @Override
    public void register(ActionRegistry registry) {
        registry.register("mine", (foreman, task, ctx) -> new MineAction(foreman, task));
        registry.register("build", (foreman, task, ctx) -> new BuildAction(foreman, task));
    }
}
```

### 4.2 Interceptor Chain Pattern

**Problem:** Need cross-cutting concerns (logging, metrics) without tangling code.

**Solution:** Interceptor chain (like servlet filters).

```java
public interface ActionInterceptor {
    ActionResult intercept(Action action, ActionChain chain);
}

public class LoggingInterceptor implements ActionInterceptor {
    @Override
    public ActionResult intercept(Action action, ActionChain chain) {
        LOGGER.info("Starting action: {}", action.getType());
        ActionResult result = chain.proceed(action);
        LOGGER.info("Completed action: {} with result {}", action.getType(), result);
        return result;
    }
}

// Chain execution
ActionResult result = interceptorChain.intercept(action);
```

---

## 5. Memory Management Patterns

### 5.1 Object Pooling Pattern

**Problem:** Creating objects causes GC pressure in hot paths.

**Solution:** Pool and reuse objects.

```java
public class PathNodePool {
    private final Queue<PathNode> pool = new ConcurrentLinkedQueue<>();

    public PathNode acquire(int x, int y, int z) {
        PathNode node = pool.poll();
        if (node != null) {
            node.reset(x, y, z);
            return node;
        }
        return new PathNode(x, y, z);
    }

    public void release(PathNode node) {
        pool.offer(node);
    }
}
```

**Applied In:**
- `AStarPathfinder` - Pools pathfinding nodes
- `PathSmoothing` - Pools vectors

### 5.2 Bounded Collection Pattern

**Problem:** Collections grow unbounded, causing OOM.

**Solution:** Use bounded collections with eviction.

```java
// Using Caffeine cache
private final Cache<String, Conversation> conversations = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(Duration.ofHours(1))
    .build();

// Using custom LRU
public class BoundedMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
```

---

## 6. Error Handling Patterns

### 6.1 Graceful Degradation Pattern

**Problem:** Failures shouldn't crash the game.

**Solution:** Catch, log, and recover.

```java
public void tick() {
    try {
        executeCurrentTask();
    } catch (Exception e) {
        LOGGER.error("Task execution failed", e);
        stateMachine.transition(AgentState.ERROR);
        scheduleRecovery();
    }
}
```

### 6.2 Recovery Strategy Pattern

**Problem:** Different failures need different recovery approaches.

**Solution:** Strategy pattern for recovery.

```java
public interface RecoveryStrategy {
    RecoveryResult attempt(ForemanEntity entity, StuckType type);
}

public class RepathStrategy implements RecoveryStrategy {
    @Override
    public RecoveryResult attempt(ForemanEntity entity, StuckType type) {
        entity.recalculatePath();
        return RecoveryResult.RETRY;
    }
}

public class TeleportStrategy implements RecoveryStrategy {
    @Override
    public RecoveryResult attempt(ForemanEntity entity, StuckType type) {
        entity.teleportToSafety();
        return RecoveryResult.RESOLVED;
    }
}
```

---

## 7. Testing Patterns

### 7.1 Test Structure Pattern

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
            GivenState given = new GivenState();

            // Act
            ActualResult actual = whenActionIsPerformed();

            // Assert
            assertThat(actual).isEqualTo(expectedResult);
        }
    }
}
```

### 7.2 Mock Pattern for Minecraft Classes

**Problem:** Minecraft classes are hard to mock.

**Solution:** Create interface wrappers.

```java
// Interface for testability
public interface WorldAccess {
    BlockState getBlockState(BlockPos pos);
    void setBlock(BlockPos pos, BlockState state);
}

// Production implementation
public class MinecraftWorldAccess implements WorldAccess {
    private final Level level;

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }
}

// Test implementation
public class MockWorldAccess implements WorldAccess {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }
}
```

---

## 8. Documentation Patterns

### 8.1 JavaDoc Pattern

```java
/**
 * Executes a list of tasks in dependency order.
 *
 * <p>This method validates tasks, orders them by dependencies,
 * and executes them through the interceptor chain.</p>
 *
 * <p>Thread Safety: This method is thread-safe and can be called
 * from any thread. Results are published to the game thread.</p>
 *
 * @param tasks the tasks to execute, must not be null
 * @throws IllegalArgumentException if tasks is empty or contains null
 * @throws IllegalStateException if executor is shutting down
 * @see TaskValidator#validate(List)
 * @see TaskOrderer#orderByDependencies(List)
 */
public void executeTasks(List<Task> tasks) {
    // ...
}
```

### 8.2 Code Comment Pattern

```java
// IMPROVEMENT OPPORTUNITY [P2]: Brief description
// Rationale: Why this matters
// Approach: Suggested implementation
// Impact: Expected benefit
```

---

## 9. Configuration Patterns

### 9.1 Environment Variable Pattern

**Problem:** Secrets shouldn't be in config files.

**Solution:** Support environment variable references.

```toml
# config/minewright-common.toml
[openai]
apiKey = "${OPENAI_API_KEY}"
```

```java
public String getResolvedApiKey() {
    String key = config.apiKey();
    if (key.startsWith("${") && key.endsWith("}")) {
        String envVar = key.substring(2, key.length() - 1);
        return System.getenv(envVar);
    }
    return key;
}
```

### 9.2 Validation Pattern

```java
public class ConfigValidator {
    public void validate(MineWrightConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.maxAgents() < 1 || config.maxAgents() > 100) {
            errors.add("maxAgents must be between 1 and 100");
        }

        if (config.apiKey() == null || config.apiKey().isBlank()) {
            errors.add("apiKey is required");
        }

        if (!errors.isEmpty()) {
            throw new ConfigException("Invalid configuration: " + errors);
        }
    }
}
```

---

## 10. Code Review Checklist

### Before Submitting PR

- [ ] Code follows formatting rules (4-space indent, 120 char line limit)
- [ ] No new TODO comments without linked issues
- [ ] Thread safety verified (concurrent access, atomic operations)
- [ ] No blocking operations in tick methods
- [ ] Tests added for new functionality
- [ ] JavaDoc added for public APIs
- [ ] No hardcoded secrets or credentials
- [ ] Error handling is comprehensive
- [ ] Logging is appropriate (not too verbose, not silent)

### During Review

- [ ] Pattern consistency with existing code
- [ ] No god class creation (keep under 500 lines)
- [ ] Dependency direction is correct (no circular deps)
- [ ] Memory leaks checked (closes resources, bounded collections)
- [ ] Performance impact acceptable

---

## 11. Anti-Patterns to Avoid

### 11.1 God Class Anti-Pattern

**Don't:** Let classes grow beyond 500 lines.

```java
// BAD: Class with 800 lines doing everything
public class GameManager {
    public void spawn() { ... }
    public void despawn() { ... }
    public void executeTask() { ... }
    public void planTask() { ... }
    public void handleChat() { ... }
    public void saveState() { ... }
    public void loadState() { ... }
    // ... 50 more methods
}
```

**Do:** Extract to focused components.

### 11.2 Blocking Anti-Pattern

**Don't:** Block in tick methods.

```java
// BAD: Blocks game thread
public void tick() {
    Result result = llmClient.generateSync(prompt);  // BLOCKS!
    processResult(result);
}
```

**Do:** Use async with polling.

```java
// GOOD: Non-blocking
public void tick() {
    if (future != null && future.isDone()) {
        processResult(future.getNow(null));
        future = null;
    }
}

public void startPlanning() {
    future = llmClient.generateAsync(prompt);
}
```

### 11.3 Mutable State Anti-Pattern

**Don't:** Share mutable state without synchronization.

```java
// BAD: Race condition
public int counter;

public void increment() {
    counter++;  // NOT ATOMIC!
}
```

**Do:** Use atomic operations.

```java
// GOOD: Thread-safe
public AtomicInteger counter = new AtomicInteger();

public void increment() {
    counter.incrementAndGet();
}
```

---

## 12. Quick Reference

### Common Code Snippets

**Concurrent HashMap:**
```java
private final ConcurrentHashMap<String, Value> map = new ConcurrentHashMap<>();
map.computeIfAbsent(key, k -> createValue(k));
```

**Async with Timeout:**
```java
try {
    Result result = future.get(1, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    handleTimeout();
}
```

**Bounded Cache:**
```java
private final Cache<K, V> cache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(Duration.ofMinutes(10))
    .build();
```

**Singleton (Thread-Safe):**
```java
public class Singleton {
    private static volatile Singleton instance;

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

---

**Document Version:** 1.0
**Last Updated:** 2026-03-04
**Maintained By:** Agent Orchestration System
