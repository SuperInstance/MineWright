# Testing Quick Reference

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests com.minewright.execution.AgentStateMachineTest

# Run with coverage report
./gradlew test jacocoTestReport

# Run tests with console output
./gradlew test --info
```

## Test Structure

```
src/test/java/com/minewright/
├── action/
│   └── ActionExecutorTest.java          # Action execution tests
├── execution/
│   └── AgentStateMachineTest.java       # State machine tests
├── llm/
│   ├── TaskPlannerTest.java             # Task planning & validation
│   └── ResponseParserTest.java          # LLM response parsing
├── memory/
│   └── WorldKnowledgeTest.java          # World scanning tests
├── plugin/
│   └── ActionRegistryTest.java          # Plugin registry tests
├── structure/
│   └── StructureGeneratorsTest.java     # Structure generation tests
└── testutil/
    ├── TaskBuilder.java                 # Test data builder
    └── FakeServerLevel.java             # Mock world implementation
```

## Common Test Patterns

### Creating Tasks

```java
// Using builder
Task task = TaskBuilder.aTask("mine")
    .withBlock("stone")
    .withQuantity(10)
    .build();

// Using presets
Task task = TaskBuilder.Presets.mineStone(10);
Task task = TaskBuilder.Presets.buildHouse("house", 5, 4, 5);
Task task = TaskBuilder.Presets.craftItem("oak_planks", 20);
```

### Mocking Components

```java
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private ServerLevel mockLevel;

    @BeforeEach
    void setUp() {
        lenient().when(mockForeman.level()).thenReturn(mockLevel);
        lenient().when(mockForeman.getSteveName()).thenReturn("TestSteve");
    }
}
```

### Async Testing

```java
// Use Awaitility for async operations
await().atMost(5, TimeUnit.SECONDS)
    .until(() -> future.isDone());

// Or simple timeout
assertTrue(future.get(5, TimeUnit.SECONDS) != null);
```

### Parameterized Tests

```java
@ParameterizedTest
@ValueSource(strings = {"stone", "iron_ore", "diamond_ore"})
void testWithDifferentBlocks(String block) {
    Task task = TaskBuilder.aTask("mine")
        .withBlock(block)
        .withQuantity(10)
        .build();

    assertTrue(planner.validateTask(task));
}
```

## Test Naming Conventions

- **Test Class:** `[ClassName]Test`
- **Test Method:** `test[MethodName][Scenario]`
  - `testValidateValidMineTask`
  - `testStateTransitionPublishesEvent`
  - `testCreateActionReturnsCorrectInstance`

## Coverage Goals

| Component | Target | Current |
|-----------|--------|---------|
| AgentStateMachine | 90% | - |
| LLMCache | 85% | - |
| ActionRegistry | 80% | - |
| TaskPlanner | 75% | - |
| ActionExecutor | 70% | - |

## Test Dependencies

```gradle
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
testImplementation 'org.mockito:mockito-core:5.3.1'
testImplementation 'org.mockito:mockito-junit-jupiter:5.3.1'
testImplementation 'org.assertj:assertj-core:3.24.2'
testImplementation 'org.awaitility:awaitility:4.2.0'
testImplementation 'org.junit.jupiter:junit-jupiter-params:5.9.3'
```

## Writing New Tests

1. **Identify what to test:** Public methods, state transitions, edge cases
2. **Create test class:** In appropriate package under `src/test/java/`
3. **Set up mocks:** Use `@Mock` and `@BeforeEach`
4. **Write test methods:** Follow naming convention
5. **Use assertions:** `assertEquals`, `assertTrue`, `assertNotNull`
6. **Run tests:** `./gradlew test`

## Example Test Template

```java
@ExtendWith(MockitoExtension.class)
class MyComponentTest {

    @Mock
    private Dependency mockDependency;

    private MyComponent component;

    @BeforeEach
    void setUp() {
        component = new MyComponent(mockDependency);
    }

    @Test
    void testSomething() {
        // Arrange
        when(mockDependency.getValue()).thenReturn(42);

        // Act
        int result = component.calculate();

        // Assert
        assertEquals(42, result);
        verify(mockDependency).getValue();
    }
}
```
