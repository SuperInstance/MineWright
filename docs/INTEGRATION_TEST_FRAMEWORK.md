# Integration Test Framework

## Overview

The Integration Test Framework provides a comprehensive testing environment for the MineWright Minecraft mod, enabling testing of multi-agent coordination scenarios without requiring an actual Minecraft server.

## Features

- **Mock Minecraft Server**: Lightweight mock server for testing without full Minecraft infrastructure
- **Entity Factory**: Create test entities (ForemanEntity, workers) with proper initialization
- **Scenario Builder**: Fluent API for building and executing test scenarios
- **Event-Driven Testing**: Subscribe to and validate coordination events
- **Async Operation Support**: Handle async operations with configurable timeouts
- **World State Management**: Manipulate test world state for scenario setup
- **Tick Simulation**: Simulate game ticks for testing time-dependent behaviors

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              IntegrationTestFramework                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  MockMinecraftServer                                   │  │
│  │  - Server lifecycle (start, stop, tick)                │  │
│  │  - Player/Entity registry                              │  │
│  │  - World management                                     │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  TestEntityFactory                                     │  │
│  │  - Create test entities (ForemanEntity, workers)       │  │
│  │  - Entity lifecycle management                         │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  TestScenarioBuilder                                   │  │
│  │  - Fluent API for scenario building                    │  │
│  │  - Command execution and validation                     │  │
│  │  - Expected result verification                         │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  EventBus                                               │  │
│  │  - Event subscription and validation                    │  │
│  │  - Coordination event tracking                          │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  WorldStateManager                                      │  │
│  │  - Test world state manipulation                        │  │
│  │  - Scenario setup/teardown                              │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Usage

### Basic Test Setup

Extend `IntegrationTestBase` for automatic framework lifecycle management:

```java
class MyIntegrationTest extends IntegrationTestBase {

    @Test
    void testScenario() {
        // Framework is automatically initialized
        ForemanEntity foreman = createForeman("Steve");

        TestScenarioBuilder scenario = createScenario("My Test")
            .withForeman("Steve")
            .withCommand("mine 10 stone")
            .expectSuccess(true);

        TestResult result = executeScenario(scenario);
        assertTrue(result.isSuccess());
    }
}
```

### Creating Entities

```java
// Create a foreman entity
ForemanEntity foreman = createForeman("Steve");

// Create a foreman with specific role
ForemanEntity foreman = createForeman("Steve", AgentRole.WORKER);

// Create a worker with capability
ForemanEntity miner = createWorker("Miner", "mining");

// Create a worker with multiple capabilities
ForemanEntity builder = createWorker("Builder", Set.of("building", "hauling"));

// Create multiple workers
Map<String, ForemanEntity> miners = createWorkers("mining", "Miner1", "Miner2", "Miner3");
```

### Building Scenarios

```java
TestScenarioBuilder scenario = createScenario("Mining Test")
    // Add entities
    .withForeman("Steve")
    .withWorker("Miner1", "mining")
    .withWorker("Miner2", "mining")

    // Add commands
    .withCommand("mine 50 stone")
    .withCommand("build structure")

    // Set expectations
    .expectSuccess(true)
    .expectState(AgentState.COMPLETED)
    .expectDuration(100, 10000) // 100ms to 10s

    // Custom validation
    .expectResult(result -> {
        assertTrue(result.getDuration() < 5000);
    })

    // Custom setup/teardown
    .withSetup(() -> {
        setWorldState("stone_available", 100);
    })
    .withTeardown(() -> {
        clearWorldState();
    });
```

### Executing Scenarios

```java
// Synchronous execution with default timeout
TestResult result = scenario.execute();

// Synchronous execution with custom timeout
TestResult result = scenario.execute(5000); // 5 second timeout

// Asynchronous execution
CompletableFuture<TestResult> future = scenario.executeAsync();

// Wait for async completion
TestResult result = waitForCompletion(future, 5000);
```

### Assertions

```java
// Assert state
assertStateEquals(foreman, AgentState.COMPLETED);

// Assert success/failure
assertSuccess(result);
assertFailure(result);

// Assert duration
assertDurationBetween(result, 100, 5000);

// Assert eventual condition
assertEventuallyTrue(
    () -> isEntityComplete(foreman),
    5000,
    "Entity did not complete"
);

// Assert entity completion
assertEntityCompletes(foreman, 5000);
assertEntitiesComplete(Set.of(foreman, worker1, worker2), 5000);
```

### World State Management

```java
// Set world state
setWorldState("resources", Map.of("stone", 100, "wood", 50));

// Get world state
Map<String, Integer> resources = getWorldState("resources");

// Clear all world state
clearWorldState();

// Use in scenarios
scenario.withSetup(() -> setWorldState("phase", "execution"))
        .withTeardown(() -> clearWorldState());
```

### Event-Driven Testing

```java
// Subscribe to events
onEvent(StateTransitionEvent.class, event -> {
    LOGGER.info("State transition: {}", event);
});

// Wait for specific event
StateTransitionEvent event = waitForEvent(StateTransitionEvent.class, 5000);

// Use in scenarios
scenario.onEvent(StateTransitionEvent.class, event -> {
    transitionCount.incrementAndGet();
});
```

### Tick Simulation

```java
// Simulate single tick
tick();

// Simulate multiple ticks
tick(100);

// Tick until condition
int ticks = tickUntil(
    () -> isEntityComplete(foreman),
    1000 // max ticks
);
```

### Async Operations

```java
// Wait for condition
boolean completed = waitFor(
    () -> isEntityComplete(foreman),
    5000
);

// Wait for future completion
TestResult result = waitForCompletion(future, 5000);

// Create delayed future
CompletableFuture<String> delayed = delayedFuture(1000, "done");
```

## Test Configuration

Configure the framework with custom options:

```java
IntegrationTestFramework.FrameworkConfiguration config =
    new IntegrationTestFramework.FrameworkConfiguration.Builder()
        .withDefaultTimeout(30000)      // 30 second default timeout
        .withAsyncCheckInterval(50)     // Check async operations every 50ms
        .withVerboseLogging(true)       // Enable debug logging
        .withMaxEntities(100)           // Maximum entities per test
        .withMaxScenarios(50)           // Maximum scenarios per test
        .build();

IntegrationTestFramework framework = new IntegrationTestFramework(config);
framework.initialize();
```

## Examples

### Example 1: Single Agent Command

```java
@Test
void testSingleAgent() {
    ForemanEntity foreman = createForeman("Steve");

    TestResult result = createScenario("Single Agent")
        .withForeman("Steve")
        .withCommand("echo hello")
        .expectSuccess(true)
        .execute();

    assertSuccess(result);
}
```

### Example 2: Multi-Agent Coordination

```java
@Test
void testMultiAgentCoordination() {
    ForemanEntity foreman = createForeman("Steve", AgentRole.FOREMAN);
    Map<String, ForemanEntity> workers = createWorkers("mining", "Miner1", "Miner2");

    TestResult result = createScenario("Multi-Agent Mining")
        .withEntity("foreman", foreman)
        .withEntity("miner1", workers.get("Miner1"))
        .withEntity("miner2", workers.get("Miner2"))
        .withCommand("mine 100 stone")
        .expectSuccess(true)
        .execute();

    assertSuccess(result);
    assertEntitiesComplete(workers.values(), 10000);
}
```

### Example 3: Event Validation

```java
@Test
void testEventValidation() {
    final AtomicInteger eventCount = new AtomicInteger(0);

    onEvent(StateTransitionEvent.class, event -> {
        eventCount.incrementAndGet();
    });

    ForemanEntity foreman = createForeman("Steve");

    createScenario("Event Test")
        .withForeman("Steve")
        .withCommand("test")
        .expectResult(result -> {
            assertTrue(eventCount.get() > 0, "Should receive events");
        })
        .execute();
}
```

### Example 4: Complex Scenario with Setup/Teardown

```java
@Test
void testComplexScenario() {
    // Setup world state
    setWorldState("resources", Map.of("stone", 0, "wood", 0));

    ForemanEntity foreman = createForeman("Steve");
    ForemanEntity builder = createWorker("Builder", "building");

    TestResult result = createScenario("Build House")
        .withSetup(() -> {
            setWorldState("target", "house");
            setWorldState("materials_needed", 100);
        })
        .withEntity("foreman", foreman)
        .withEntity("builder", builder)
        .withCommand("build house")
        .expectSuccess(true)
        .expectResult(r -> {
            Map<String, Integer> resources = getWorldState("resources");
            assertTrue(resources.get("wood") >= 100);
        })
        .withTeardown(() -> {
            clearWorldState();
        })
        .execute();

    assertSuccess(result);
}
```

## Best Practices

1. **Always cleanup**: Use `@AfterEach` to ensure framework cleanup
2. **Use appropriate timeouts**: Set timeouts based on expected operation duration
3. **Validate state**: Use assertions to verify expected states and results
4. **Test isolation**: Each test should be independent and not share state
5. **Event cleanup**: Unsubscribe from events in teardown to prevent memory leaks
6. **World state**: Clear world state between tests to ensure isolation
7. **Async handling**: Always specify timeouts for async operations
8. **Error messages**: Provide descriptive error messages for assertions

## Thread Safety

- Each test instance gets its own framework instance for isolation
- Framework methods are thread-safe for concurrent scenario execution
- Entity tracking uses `ConcurrentHashMap` for safe concurrent access
- Event bus supports concurrent subscribers and publishers

## Troubleshooting

### Scenario Timeout

If scenarios timeout unexpectedly:
1. Check if entity is actually completing tasks
2. Verify state machine transitions are correct
3. Increase timeout if needed: `.withTimeout(60000)`
4. Check for deadlock or blocking operations

### Entity Not Found

If entities are not found:
1. Ensure entities are registered with the scenario
2. Use `.withEntity(name, entity)` to explicitly register
3. Check entity names match exactly

### Event Not Received

If events are not received:
1. Verify event subscription is set up before scenario execution
2. Check event type matches expected class
3. Use `waitForEvent()` with appropriate timeout
4. Ensure event bus is properly configured

### State Assertion Failures

If state assertions fail:
1. Verify state machine is properly initialized
2. Check expected state matches actual state transitions
3. Use logging to trace state changes
4. Validate async operations have completed before asserting

## Files

- `IntegrationTestFramework.java` - Main framework class
- `IntegrationTestBase.java` - Base class for integration tests
- `MockMinecraftServer.java` - Mock server implementation
- `TestEntityFactory.java` - Entity factory for test entities
- `TestScenarioBuilder.java` - Fluent API for scenario building
- `MultiAgentCoordinationIntegrationTest.java` - Example tests

## References

- JUnit 5: https://junit.org/junit5/docs/current/user-guide/
- Mockito: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- MineWright Architecture: `docs/ARCHITECTURE_OVERVIEW.md`
