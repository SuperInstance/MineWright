# Integration Test Framework - Quick Reference

## Quick Start

```java
class MyTest extends IntegrationTestBase {
    @Test
    void test() {
        ForemanEntity foreman = createForeman("Steve");
        TestResult result = createScenario("Test")
            .withForeman("Steve")
            .withCommand("test")
            .expectSuccess(true)
            .execute();
        assertSuccess(result);
    }
}
```

## Entity Creation

| Method | Description | Example |
|--------|-------------|---------|
| `createForeman(name)` | Create foreman entity | `createForeman("Steve")` |
| `createForeman(name, role)` | Create foreman with role | `createForeman("Steve", AgentRole.WORKER)` |
| `createWorker(name, capability)` | Create worker | `createWorker("Miner", "mining")` |
| `createWorker(name, capabilities)` | Create multi-capability worker | `createWorker("Builder", Set.of("build", "haul"))` |
| `createWorkers(capability, names...)` | Create multiple workers | `createWorkers("mining", "M1", "M2")` |

## Scenario Builder Methods

### Entity Management

| Method | Description |
|--------|-------------|
| `.withForeman(name)` | Add foreman entity |
| `.withForeman(name, role)` | Add foreman with role |
| `.withWorker(name, role)` | Add worker entity |
| `.withWorker(name, capabilities)` | Add worker with capabilities |
| `.withWorkers(role, names...)` | Add multiple workers |
| `.withEntity(name, entity)` | Add custom entity |

### Commands

| Method | Description |
|--------|-------------|
| `.withCommand(command)` | Add command for primary entity |
| `.withCommand(entityName, command)` | Add command for specific entity |
| `.withTask(task)` | Add task for primary entity |
| `.withTask(entityName, task)` | Add task for specific entity |

### Expectations

| Method | Description |
|--------|-------------|
| `.expectState(state)` | Expected final state |
| `.expectSuccess(success)` | Expected success status |
| `.expectResult(validator)` | Custom result validation |
| `.expectState(validator)` | Custom state validation |
| `.expectDuration(minMs, maxMs)` | Expected duration range |

### Configuration

| Method | Description |
|--------|-------------|
| `.withTimeout(timeoutMs)` | Set execution timeout |
| `.withSetup(action)` | Add setup action |
| `.withTeardown(action)` | Add teardown action |
| `.onEvent(eventType, handler)` | Add event handler |

### Execution

| Method | Description |
|--------|-------------|
| `.execute()` | Execute synchronously (default timeout) |
| `.execute(timeoutMs)` | Execute with custom timeout |
| `.executeAsync()` | Execute asynchronously |
| `.executeAsync(timeoutMs)` | Execute async with timeout |

## Assertions

| Method | Description | Example |
|--------|-------------|---------|
| `assertStateEquals(entity, state)` | Assert entity state | `assertStateEquals(foreman, COMPLETED)` |
| `assertSuccess(result)` | Assert success | `assertSuccess(result)` |
| `assertFailure(result)` | Assert failure | `assertFailure(result)` |
| `assertDurationBetween(result, min, max)` | Assert duration | `assertDurationBetween(result, 100, 5000)` |
| `assertEventuallyTrue(condition, timeout, msg)` | Assert eventual condition | `assertEventuallyTrue(() -> done, 5000, "msg")` |
| `assertEntityCompletes(entity, timeout)` | Assert entity completes | `assertEntityCompletes(foreman, 5000)` |
| `assertEntitiesComplete(entities, timeout)` | Assert all complete | `assertEntitiesComplete(workers, 5000)` |

## World State

| Method | Description | Example |
|--------|-------------|---------|
| `setWorldState(key, value)` | Set state | `setWorldState("count", 10)` |
| `getWorldState(key)` | Get state | `Integer count = getWorldState("count")` |
| `clearWorldState()` | Clear all state | `clearWorldState()` |

## Event Testing

| Method | Description | Example |
|--------|-------------|---------|
| `onEvent(eventType, handler)` | Subscribe to event | `onEvent(StateEvent::class, handler)` |
| `waitForEvent(eventType, timeout)` | Wait for event | `waitForEvent(StateEvent::class, 5000)` |

## Tick Simulation

| Method | Description | Example |
|--------|-------------|---------|
| `tick()` | Simulate one tick | `tick()` |
| `tick(count)` | Simulate multiple ticks | `tick(100)` |
| `tickUntil(condition, max)` | Tick until condition | `tickUntil(() -> done, 1000)` |

## Async Operations

| Method | Description | Example |
|--------|-------------|---------|
| `waitFor(condition, timeout)` | Wait for condition | `waitFor(() -> done, 5000)` |
| `waitForCompletion(future, timeout)` | Wait for future | `waitForCompletion(future, 5000)` |
| `delayedFuture(delay, value)` | Create delayed future | `delayedFuture(1000, "done")` |

## TestResult Methods

| Method | Description |
|--------|-------------|
| `isSuccess()` | Check if successful |
| `getErrorMessage()` | Get error message |
| `getDuration()` | Get duration in ms |
| `getMetadata()` | Get metadata map |
| `putMetadata(key, value)` | Add metadata |
| `getMetadata(key)` | Get metadata value |

## AgentState Values

| State | Description |
|-------|-------------|
| `IDLE` | Not processing |
| `PLANNING` | Planning tasks |
| `EXECUTING` | Executing tasks |
| `COMPLETED` | Task completed |
| `FAILED` | Task failed |
| `PAUSED` | Task paused |

## AgentRole Values

| Role | Description |
|------|-------------|
| `SOLO` | Independent agent |
| `FOREMAN` | Coordinating agent |
| `WORKER` | Working agent |

## Common Patterns

### Basic Test

```java
@Test
void basicTest() {
    ForemanEntity foreman = createForeman("Steve");
    TestResult result = createScenario("Test")
        .withForeman("Steve")
        .withCommand("test")
        .expectSuccess(true)
        .execute();
    assertSuccess(result);
}
```

### Multi-Agent Test

```java
@Test
void multiAgentTest() {
    ForemanEntity foreman = createForeman("Steve");
    Map<String, ForemanEntity> workers = createWorkers("mining", "M1", "M2");
    TestResult result = createScenario("Multi")
        .withEntity("foreman", foreman)
        .withEntity("w1", workers.get("M1"))
        .withEntity("w2", workers.get("M2"))
        .withCommand("mine 100 stone")
        .execute();
    assertSuccess(result);
}
```

### Event Test

```java
@Test
void eventTest() {
    onEvent(StateTransitionEvent.class, e -> events.add(e));
    ForemanEntity foreman = createForeman("Steve");
    createScenario("Event")
        .withForeman("Steve")
        .withCommand("test")
        .expectResult(r -> assertTrue(events.size() > 0))
        .execute();
}
```

### World State Test

```java
@Test
void worldStateTest() {
    setWorldState("count", 0);
    ForemanEntity foreman = createForeman("Steve");
    TestResult result = createScenario("World")
        .withSetup(() -> setWorldState("phase", "test"))
        .withForeman("Steve")
        .withCommand("test")
        .expectResult(r -> assertEquals(10, getWorldState("count")))
        .withTeardown(clearWorldState())
        .execute();
}
```

### Async Test

```java
@Test
void asyncTest() {
    ForemanEntity foreman = createForeman("Steve");
    TestScenarioBuilder scenario = createScenario("Async")
        .withForeman("Steve")
        .withCommand("test");
    CompletableFuture<TestResult> future = executeScenarioAsync(scenario);
    TestResult result = waitForCompletion(future, 5000);
    assertSuccess(result);
}
```

### Tick Simulation Test

```java
@Test
void tickTest() {
    ForemanEntity foreman = createForeman("Steve");
    createScenario("Tick")
        .withForeman("Steve")
        .withCommand("test")
        .execute();
    long ticks = tickUntil(() -> isEntityComplete(foreman), 1000);
    assertTrue(ticks < 1000);
}
```

## Configuration

```java
IntegrationTestFramework.FrameworkConfiguration config =
    new IntegrationTestFramework.FrameworkConfiguration.Builder()
        .withDefaultTimeout(30000)      // Default timeout: 30s
        .withAsyncCheckInterval(50)     // Async check: 50ms
        .withVerboseLogging(true)       // Debug logging
        .withMaxEntities(100)           // Max entities
        .withMaxScenarios(50)           // Max scenarios
        .build();
```

## Tips

1. Always use `@BeforeEach` and `@AfterEach` from base class
2. Set appropriate timeouts for operations
3. Clear world state between tests
4. Use descriptive scenario names
5. Validate both success and state
6. Use assertions with clear messages
7. Test both happy path and error cases
8. Keep tests isolated and independent

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Timeout | Increase timeout or check entity completion |
| Entity not found | Register entity with `.withEntity()` |
| Event not received | Check subscription and event type |
| State mismatch | Verify state transitions are correct |
| Test fails intermittently | Check for race conditions, add waits |
