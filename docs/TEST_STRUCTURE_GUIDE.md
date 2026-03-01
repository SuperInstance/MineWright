# Test Structure Quick Reference

**Last Updated:** 2026-03-01

## Test File Organization

```
src/test/java/com/minewright/
├── action/
│   └── ActionExecutorTest.java          (38 tests)
├── execution/
│   ├── AgentStateMachineTest.java       (48 tests)
│   ├── InterceptorChainTest.java        (27 tests)
│   └── [other execution tests]
├── coordination/
│   ├── AgentCapabilityTest.java
│   └── CapabilityRegistryTest.java
├── decision/
│   └── UtilityFactorsTest.java
├── llm/
│   └── cascade/
│       ├── CascadeRouterTest.java
│       └── ComplexityAnalyzerTest.java
└── [other test packages]
```

## Test Commands Reference

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "*AgentStateMachineTest"
```

### Run Specific Test Method
```bash
./gradlew test --tests "*AgentStateMachineTest.testValidIdleToPlanning"
```

### Run Tests for a Package
```bash
./gradlew test --tests "com.minewright.execution.*"
```

### Run with No Daemon (Windows)
```bash
./gradlew --stop
./gradlew test --no-daemon
```

### Run with Verbose Output
```bash
./gradlew test --info
./gradlew test --debug
```

## Test Template

```java
package com.minewright.[package];

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("[Component Name] Tests")
class [ComponentName]Test {

    @Mock
    private MockDependency mockDependency;

    private ComponentUnderTest component;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        component = new ComponentUnderTest(mockDependency);
    }

    @Test
    @DisplayName("Brief description of what is tested")
    void testMethodBehavior() {
        // Arrange
        [setup test data]

        // Act
        [execute method under test]

        // Assert
        [verify results]
        assertEquals(expected, actual);
    }
}
```

## Common Assertions

```java
// Equality
assertEquals(expected, actual);
assertNotEquals(expected, actual);

// Boolean
assertTrue(condition);
assertFalse(condition);

// Nullability
assertNull(object);
assertNotNull(object);

// Exceptions
assertThrows(ExpectedException.class, () -> methodToTest());

// No exception
assertDoesNotThrow(() -> methodToTest());

// Multiple assertions
assertAll("group name",
    () -> assertEquals(expected1, actual1),
    () -> assertEquals(expected2, actual2)
);
```

## Mockito Common Patterns

```java
// Verify method was called
verify(mock).methodName(arg1, arg2);

// Verify method was never called
verify(mock, never()).methodName(any());

// Verify call count
verify(mock, times(3)).methodName(any());

// Stub method return
when(mock.methodName(any())).thenReturn(value);

// Stub method to throw
when(mock.methodName(any())).thenThrow(new Exception());

// Capture arguments
ArgumentCaptor<Type> captor = ArgumentCaptor.forClass(Type.class);
verify(mock).method(captor.capture());
Type value = captor.getValue();
```

## Test Naming Conventions

### Test Methods
- Use `@DisplayName` for readable test names
- Method name should describe what is tested
- Prefix with `test` for test methods
- Use `testValid[State]To[State]` for state transitions

Examples:
```java
@Test
@DisplayName("Valid transition: IDLE to PLANNING")
void testValidIdleToPlanning() { }

@Test
@DisplayName("Invalid transition: IDLE to EXECUTING")
void testInvalidIdleToExecuting() { }

@Test
@DisplayName("Can accept commands in IDLE state")
void testCanAcceptCommandsIdle() { }
```

### Test Organization
Group related tests:
```java
// Initialization tests
@Test @DisplayName("Initial state is IDLE") void testInitialState() { }
@Test @DisplayName("Constructor with null event bus works") void testConstructorWithNullEventBus() { }

// Valid transition tests
@Test @DisplayName("Valid transition: IDLE to PLANNING") void testValidIdleToPlanning() { }
@Test @DisplayName("Valid transition: PLANNING to EXECUTING") void testValidPlanningToExecuting() { }

// Error handling tests
@Test @DisplayName("Invalid transition: null state") void testInvalidNullTransition() { }
```

## Testing Best Practices

### DO's
1. **Test one thing per test** - Keep tests focused
2. **Use descriptive names** - `@DisplayName` is your friend
3. **Arrange-Act-Assert** - Structure tests clearly
4. **Mock external dependencies** - Don't test the world
5. **Test edge cases** - null, empty, negative values
6. **Test thread safety** - Use concurrent test patterns
7. **Verify error handling** - Test exception paths

### DON'Ts
1. **Don't test implementation details** - Test behavior
2. **Don't test private methods** - They're implementation
3. **Don't write flaky tests** - Tests should be deterministic
4. **Don't ignore test failures** - Fix or update the test
5. **Don't test third-party code** - Trust your dependencies
6. **Don't write tests that depend on execution order** - Each test should be independent

## Test Coverage Goals

| Component Type | Target Coverage |
|----------------|-----------------|
| **Core Logic** | 90%+ |
| **State Machines** | 95%+ |
| **Utility Classes** | 80%+ |
| **Data Models** | 70%+ |
| **Integration Points** | 85%+ |

## Continuous Integration

### Pre-Commit Checklist
- [ ] All tests pass locally
- [ ] New code has tests
- [ ] No test warnings
- [ ] Tests run in reasonable time (< 5 min)

### Pre-Pull Request Checklist
- [ ] All tests pass
- [ ] Coverage hasn't decreased
- [ ] No flaky tests
- [ ] Performance tests pass

## Debugging Tests

### Enable Debug Logging
```bash
./gradlew test --debug --tests "*TestName"
```

### Run Single Test
```bash
./gradlew test --tests "*TestClass.testMethodName"
```

### View Test Report
After running tests, open:
```
build/reports/tests/test/index.html
```

### Common Issues

| Issue | Solution |
|-------|----------|
| **File lock error** | Run `./gradlew --stop` then test with `--no-daemon` |
| **Tests not found** | Check package and class name matches |
| **Mockito errors** | Ensure `@BeforeEach` calls `openMocks()` |
| **NullPointerException** | Verify all mocks are initialized |
| **Tests pass in IDE but fail in Gradle** | Check classpath dependencies |

---

**For detailed test coverage analysis, see:** `TEST_COVERAGE_ANALYSIS.md`
