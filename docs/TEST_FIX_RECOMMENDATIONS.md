# Test Fix Recommendations

**Date:** 2026-03-01
**Purpose:** Provide actionable solutions for failing tests

## Critical Issue: Minecraft Entity Class Initialization

### Problem

500+ tests fail with this error pattern:

```
java.lang.NoClassDefFoundError: Could not initialize class net.minecraft.world.entity.PathfinderMob
java.lang.NoClassDefFoundError: Could not initialize class net.minecraft.world.entity.Mob
java.lang.NoClassDefFoundError: Could not initialize class net.minecraft.world.entity.LivingEntity
java.lang.NoClassDefFoundError: Could not initialize class com.minewright.entity.ForemanEntity
```

**Root Cause:** Minecraft Forge classes require:
- Forge game layers to be loaded
- Native libraries (LWJGL)
- Complete classpath with all Forge mixins
- Game registry initialization

Standard JUnit tests don't provide this environment.

### Solution 1: Forge Test Framework (RECOMMENDED)

**Pros:**
- Official Forge testing approach
- Real Minecraft objects
- Most realistic test environment

**Cons:**
- More complex setup
- Slower test execution
- Requires Forge dependencies

**Implementation:**

```gradle
// build.gradle
repositories {
    maven {
        name = 'Forge Maven'
        url = 'https://maven.minecraftforge.net/'
    }
}

dependencies {
    testImplementation 'net.minecraftforge:forge:1.20.1-47.3.12'
    testImplementation 'org.spongepowered:mixin:0.8.5:processor'
}

test {
    useJUnitPlatform()
    systemProperty 'forge.testing.testFramework', 'true'
    systemProperty 'forge.logging.markers', 'SCAN,REGISTRIES,REGISTRYDUMP'
    systemProperty 'forge.logging.console.level', 'debug'
}
```

**Test Example:**

```java
@ExtendWith(ForgeTestExtensions.class)
class ActionExecutorIntegrationTest {

    @Test
    void testExecuteTask(@FinalProjectServer server) {
        // Create real ForemanEntity in test environment
        ForemanEntity foreman = new ForemanEntity(...);
        ActionExecutor executor = new ActionExecutor(foreman);

        Task task = new Task("mine", Map.of("block", "stone"));
        executor.execute(task);

        // Assertions
        assertTrue(executor.getCurrentTask().isComplete());
    }
}
```

**Effort:** 20-30 hours to refactor existing tests

### Solution 2: Extract Interfaces (ALTERNATIVE)

**Pros:**
- Fast test execution
- Standard Mockito mocking
- Clean separation of concerns

**Cons:**
- Major code refactoring
- Runtime overhead
- May not catch all integration issues

**Implementation:**

**Step 1: Create interfaces**

```java
// src/main/java/com/minewright/entity/IEntity.java
public interface IEntity {
    BlockPos getBlockPos();
    Level getLevel();
    // ... other methods
}

// src/main/java/com/minewright/entity/IForemanEntity.java
public interface IForemanEntity extends IEntity {
    void setTask(Task task);
    Task getTask();
    // ... other methods
}
```

**Step 2: Update concrete class**

```java
// src/main/java/com/minewright/entity/ForemanEntity.java
public class ForemanEntity extends PathfinderMob implements IForemanEntity {
    // Existing implementation
}
```

**Step 3: Update dependencies**

```java
// Before
public class ActionExecutor {
    private final ForemanEntity foreman;
    public ActionExecutor(ForemanEntity foreman) {
        this.foreman = foreman;
    }
}

// After
public class ActionExecutor {
    private final IForemanEntity foreman;
    public ActionExecutor(IForemanEntity foreman) {
        this.foreman = foreman;
    }
}
```

**Step 4: Mock in tests**

```java
@Test
void testExecuteTask() {
    IForemanEntity mockForeman = mock(IForemanEntity.class);
    when(mockForeman.getBlockPos()).thenReturn(new BlockPos(0, 64, 0));

    ActionExecutor executor = new ActionExecutor(mockForeman);
    // ... test logic
}
```

**Effort:** 40-60 hours for full codebase refactoring

### Solution 3: Integration Test Suite (HYBRID)

**Pros:**
- Best of both worlds
- Unit tests stay fast
- Integration tests validate real behavior

**Cons:**
- Two test suites to maintain
- Longer CI pipeline

**Implementation:**

**Keep existing tests as unit tests:**
- Test logic that doesn't need Minecraft objects
- Use simple mocks where possible

**Add integration test suite:**

```gradle
// build.gradle
sourceSets {
    integrationTest {
        compileClasspath += sourceSets.main.output
        runtimeClasspath += sourceSets.main.output
    }
}

configurations {
    integrationTestImplementation.extendsFrom testImplementation
    integrationTestRuntimeOnly.extendsFrom testRuntimeOnly
}

task integrationTest(type: Test) {
    description = 'Runs integration tests with Forge'
    group = 'verification'
    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath
    useJUnitPlatform()
    systemProperty 'forge.testing.testFramework', 'true'
}
```

**Test structure:**

```
src/test/java/com/minewright/
├── action/
│   └── ActionExecutorTest.java (unit tests, no Minecraft objects)
├── memory/
│   └── MemorySystemTest.java (unit tests)
└── ...

src/integrationTest/java/com/minewright/
├── action/
│   └── ActionExecutorIntegrationTest.java (with Forge)
├── entity/
│   └── ForemanEntityIntegrationTest.java (with Forge)
└── ...
```

**Effort:** 30-40 hours

## Issue: Unsafe Reflection Access

### Problem

BTBlackboard tests fail with:

```
java.lang.RuntimeException: Failed to create test blackboard using Unsafe
```

### Solution: Add JVM Flags

**Option A: Add to build.gradle**

```gradle
test {
    jvmArgs(
        '--add-opens', 'java.base/jdk.internal.misc=ALL-UNNAMED',
        '--add-opens', 'java.base/sun.nio.ch=ALL-UNNAMED',
        '--add-opens', 'java.base/java.lang.reflect=ALL-UNNAMED'
    )
}
```

**Option B: Create .jvmargs file**

```
// test.jvmargs
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
```

**Option C: Refactor to MethodHandles**

```java
// Before (using Unsafe)
private static final Unsafe UNSAFE = getUnsafe();

// After (using MethodHandles)
private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
private static final MethodHandle PUT_BOOLEAN_METHOD = ...;
```

**Effort:** 4-6 hours (Option A or B), 8-12 hours (Option C)

## Issue: Script Parser Bugs

### Problem

9 script parser tests fail with:
- Parenthesis matching errors
- String parameter parsing errors
- YAML format parsing errors
- Missing exception throws

### Solution: Fix Parser Logic

**Step 1: Add detailed logging**

```java
// ScriptParser.java
private void error(String message) {
    LOGGER.error("Parse error at line {}, column {}: '{}'",
        line, column, currentChar);
    LOGGER.error("Buffer: {}", buffer);
    throw new ScriptParseException(message);
}
```

**Step 2: Fix parenthesis matching**

```java
// ScriptParser.java - parseSimpleAction()
private void consume(char expected) {
    if (currentChar != expected) {
        error("Expected '" + expected + "', found '" + currentChar + "'");
    }
    advance();
}

// Fix: Skip whitespace before checking
private void consume(char expected) {
    skipWhitespace();
    if (currentChar != expected) {
        error("Expected '" + expected + "', found '" + currentChar + "'");
    }
    advance();
}
```

**Step 3: Fix string parsing**

```java
// ScriptParser.java - parseStringParameter()
private String parseStringParameter() {
    char quote = currentChar;
    advance(); // Skip opening quote

    StringBuilder sb = new StringBuilder();
    while (currentChar != quote && !isAtEnd()) {
        if (currentChar == '\\') {
            advance();
            sb.append(escapeChar(currentChar));
        } else {
            sb.append(currentChar);
        }
        advance();
    }

    if (isAtEnd()) {
        error("Unterminated string literal");
    }

    advance(); // Skip closing quote
    return sb.toString();
}
```

**Step 4: Fix YAML format parsing**

```java
// ScriptParser.java - parseNode()
private ScriptNode parseNode() {
    skipWhitespace();
    skipIndentation(); // Handle YAML-like indentation

    if (isIdentifierStart(currentChar)) {
        return parseActionOrComposite();
    }

    // ... rest of parsing
}
```

**Effort:** 8-12 hours

## Priority Order

### Phase 1: Unblock Testing (Week 1)

1. **Add JVM flags for Unsafe access** (2 hours)
   - Add `--add-opens` flags to build.gradle
   - Re-run BTBlackboard tests
   - Verify 46 tests now pass

2. **Fix script parser bugs** (8-12 hours)
   - Add detailed logging
   - Fix parenthesis matching
   - Fix string parsing
   - Fix YAML format parsing
   - Re-run ScriptParser tests
   - Verify 9 tests now pass

**Expected Results:** 55 more tests passing (1,401 total)

### Phase 2: Implement Forge Test Framework (Week 2-3)

3. **Set up Forge test framework** (10 hours)
   - Add Forge test dependencies
   - Configure test environment
   - Create test utilities
   - Write sample integration test

4. **Migrate critical tests** (20 hours)
   - ActionExecutor tests (46 tests)
   - Action implementations (150+ tests)
   - Pathfinding tests (50+ tests)
   - Behavior tree tests (100+ tests)

**Expected Results:** 350+ more tests passing (1,750+ total, 90%+ pass rate)

### Phase 3: Improve Coverage (Week 4-5)

5. **Add missing tests** (30 hours)
   - HTN planner coverage
   - Cascade router coverage
   - Multi-agent coordination coverage
   - Error handling coverage

6. **Set up CI/CD** (10 hours)
   - GitHub Actions workflow
   - Automated test runs
   - Coverage reporting

**Expected Results:** 60%+ test coverage, 95%+ pass rate

## Quick Wins

### 1. Disable Failing Tests Temporarily

```java
@Disabled("Waiting for Forge test framework setup")
@Test
void testActionExecutorWithRealEntity() {
    // Test code
}
```

**Benefit:** Clean test reports while working on fixes

### 2. Create Test Utilities

```java
// src/test/java/com/minewright/test/ForgeTestUtils.java
public class ForgeTestUtils {
    public static ForemanEntity createTestForeman(Level level) {
        // Helper to create test entities
    }

    public static Level createTestLevel() {
        // Helper to create test world
    }
}
```

**Benefit:** Reduce test setup code duplication

### 3. Add Test Tags

```java
@Tag("unit")
@Test
void testMemoryStorage() {
    // Fast unit test
}

@Tag("integration")
@Test
void testFullActionExecution() {
    // Slower integration test
}
```

**Benefit:** Run fast tests frequently, slow tests less often

## Next Actions

1. **Review this document** with team
2. **Choose solution approach** (Forge framework vs interfaces vs hybrid)
3. **Create implementation plan** with timeline
4. **Start with Phase 1** (quick wins)
5. **Track progress** in project management tool

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Maintained By:** Claude Orchestrator
