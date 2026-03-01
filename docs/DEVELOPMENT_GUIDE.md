# Steve AI Development Guide

**Version:** 1.0.0
**Last Updated:** 2026-02-28
**Target:** Java developers contributing to the MineWright Minecraft mod

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Building the Project](#2-building-the-project)
3. [Configuration](#3-configuration)
4. [Testing](#4-testing)
5. [Debugging](#5-debugging)
6. [Code Style](#6-code-style)
7. [Adding a New Action](#7-adding-a-new-action)
8. [Adding a New LLM Provider](#8-adding-a-new-llm-provider)
9. [Performance Profiling](#9-performance-profiling)
10. [Common Issues](#10-common-issues)

---

## 1. Prerequisites

### Required Software

| Tool | Version | Purpose | Download |
|------|---------|---------|----------|
| **Java Development Kit (JDK)** | 17 | Language runtime | [Adoptium](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) |
| **Gradle** | 8.x (included) | Build system | Bundled with project (`./gradlew`) |
| **Minecraft Forge** | 1.20.1-47.4.16 | Mod framework | Auto-installed by Gradle |
| **IDE** | IntelliJ IDEA 2023+ / Eclipse 2023+ | Development | [IntelliJ IDEA](https://www.jetbrains.com/idea/) (recommended) |

### Verify Installation

```bash
# Check Java version (must be 17)
java -version

# Expected output: openjdk version "17.0.x" or similar
```

### Environment Setup (Security Best Practices)

**IMPORTANT:** Never hard-code API keys in source code or commit them to git.

#### Option 1: Environment Variables (Recommended)

Set your API key as an environment variable:

**Windows (PowerShell):**
```powershell
$env:STEVE_AI_API_KEY="your-api-key-here"
```

**Windows (Command Prompt):**
```cmd
set STEVE_AI_API_KEY=your-api-key-here
```

**Linux/macOS:**
```bash
export STEVE_AI_API_KEY="your-api-key-here"
```

Then in your config file, reference the environment variable (config file supports this):

```toml
[openai]
apiKey = "${STEVE_AI_API_KEY}"
```

#### Option 2: Direct Config Entry (Development Only)

Edit `run/config/steve-common.toml` after first run:

```toml
[openai]
apiKey = "your-api-key-here"
```

**Security Warning:** Never commit `run/config/steve-common.toml` to version control!

---

## 2. Building the Project

### First-Time Setup

```bash
# Clone repository (if not already done)
git clone <repository-url>
cd steve

# Run Gradle setup (downloads dependencies, deobfuscates Minecraft)
./gradlew setupDecompileWorkspace

# On Windows, use:
gradlew.bat setupDecompileWorkspace
```

### Build Commands

```bash
# Standard build (creates development JAR)
./gradlew build

# Run Minecraft client for testing
./gradlew runClient

# Run dedicated server for testing
./gradlew runServer

# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ActionResultTest

# Build distribution JAR (includes all dependencies)
./gradlew shadowJar

# Obfuscate distribution JAR (for release)
./gradlew shadowJar reobfShadowJar
```

### Output Locations

| Build Type | Output Location | Use Case |
|------------|----------------|----------|
| **Development JAR** | `build/libs/minewright-1.0.0.jar` | Development, testing |
| **Distribution JAR** | `build/libs/minewright-1.0.0-all.jar` | Distribution, release |
| **Obfuscated JAR** | `build/reobfJar/output.jar` | Production release |

### Installing the Mod

**For Development:** Use `./gradlew runClient`

**For Installation:**
1. Copy `build/libs/minewright-1.0.0-all.jar` to:
   - **Client:** `.minecraft/mods/`
   - **Server:** `minecraft_server/mods/`
2. Launch Minecraft with Forge 1.20.1
3. Configure API key (see [Configuration](#3-configuration))

---

## 3. Configuration

### Config File Location

The mod creates a config file on first run:

| Platform | Config Location |
|----------|----------------|
| **Windows** | `.minecraft/config/steve-common.toml` |
| **Linux/macOS** | `~/.minecraft/config/steve-common.toml` |
| **Development** | `run/config/steve-common.toml` |

### Minimum Configuration

```toml
[ai]
provider = "openai"  # Uses z.ai API (recommended)

[openai]
apiKey = "your-zai-api-key-here"  # Get from console.z.ai
model = "glm-5"  # Options: glm-5, glm-4-flash, gpt-4
maxTokens = 8000
temperature = 0.7
```

### Full Configuration Example

```toml
[ai]
provider = "openai"  # Options: openai, groq, gemini

[openai]
apiKey = "your-api-key"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

[behavior]
actionTickDelay = 20  # 20 ticks = 1 second
enableChatResponses = true
maxActiveCrewMembers = 10

[voice]
enabled = false
mode = "logging"  # Options: disabled, logging, real

[skill_library]
enabled = true
max_skills = 100
success_threshold = 0.7

[cascade_router]
enabled = true
similarity_threshold = 0.85

[performance]
aiTickBudgetMs = 5  # AI operations must complete in 5ms
budgetWarningThreshold = 80
strictBudgetEnforcement = true
```

### Config Reload

Edit the config file and use the in-game command:

```
/reload
```

No server restart required!

### Getting API Keys

| Provider | API URL | Model Options | Notes |
|----------|---------|---------------|-------|
| **z.ai (recommended)** | https://console.z.ai | glm-5, glm-4-flash | Chinese provider, fast & affordable |
| **OpenAI** | https://platform.openai.com | gpt-4, gpt-3.5-turbo | Official OpenAI API |
| **Groq** | https://console.groq.com | llama3-70b-8192 | Very fast inference |
| **Gemini** | https://ai.google.dev | gemini-pro | Google's LLM |

---

## 4. Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ActionResultTest

# Run specific test method
./gradlew test --tests ActionResultTest.testSuccessResult

# Run tests with coverage report
./gradlew test jacocoTestReport
```

### Test Coverage

**Current Coverage:** ~13% (as of 2026-02-28)

**Coverage by Module:**
| Module | Coverage | Status |
|--------|----------|--------|
| `action` | 25% | Good |
| `config` | 30% | Good |
| `llm` | 15% | Needs improvement |
| `memory` | 10% | Needs improvement |
| `behavior` | 5% | Needs tests |

**Note:** Minecraft entity testing is challenging due to:
- Requires Minecraft test framework
- Mockito classloader conflicts with Forge
- Many tests use `MockForemanEntity` instead of real entities

### Writing Tests

**Test Structure:**

```java
package com.minewright.action;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ActionResultTest {

    @Test
    @DisplayName("Success result should have success=true")
    void testSuccessResult() {
        ActionResult result = ActionResult.success("Task completed");

        assertTrue(result.isSuccess());
        assertEquals("Task completed", result.getMessage());
        assertFalse(result.requiresReplanning());
    }

    @Test
    @DisplayName("Failure result should have success=false")
    void testFailureResult() {
        ActionResult result = ActionResult.failure("Task failed", false);

        assertFalse(result.isSuccess());
        assertEquals("Task failed", result.getMessage());
        assertFalse(result.requiresReplanning());
    }
}
```

**Test Utilities:**

The project provides `TaskBuilder` for easy test construction:

```java
import com.minewright.testutil.TaskBuilder;

@Test
void testActionWithTask() {
    Task task = TaskBuilder.builder()
        .action("mine")
        .parameter("block", "iron_ore")
        .parameter("quantity", 10)
        .build();

    assertEquals("mine", task.getAction());
    assertEquals(10, task.getIntParameter("quantity"));
}
```

### Test Limitations

**Current Challenges:**
1. **Entity Mocking:** Cannot easily mock `ForemanEntity` due to Minecraft classloader
2. **World Access:** Tests run without Minecraft world (use `MockForemanEntity`)
3. **Async Testing:** LLM calls are async (use `CompletableFuture.join()` for testing)

**Workarounds:**
- Use `MockForemanEntity` for entity tests
- Test action logic in isolation
- Mock LLM responses for unit tests

---

## 5. Debugging

### IDE Debugging Setup

#### IntelliJ IDEA (Recommended)

1. **Create Run Configuration:**
   - Run → Edit Configurations
   - Add new Gradle configuration
   - Tasks: `runClient`
   - VM Options: `-Xmx4G`

2. **Enable Debug Mode:**
   - Run → Debug 'runClient'
   - Or use the bug icon in the toolbar

3. **Set Breakpoints:**
   - Click in the gutter next to line numbers
   - Red dot = breakpoint
   - Press F9 to continue, F8 to step over

#### Eclipse

1. **Create Debug Configuration:**
   - Run → Debug Configurations
   - New Gradle Project configuration
   - Working directory: `$workspace_loc/steve`
   - Tasks: `runClient`

2. **Debug Perspective:**
   - Window → Perspective → Open Perspective → Debug

### Logging

**Log Levels:**
```
TRACE  → Very detailed debugging
DEBUG  → Debugging information
INFO   → General information (default)
WARN   → Warning messages
ERROR  → Error messages
```

**Config File Logging:**

Edit `run/config/steve-common.toml` (or use Forge logging):

```toml
# Forge logging is configured in run/config/logging.properties
# Set default logging level
.level=INFO

# Set specific package logging
com.minewright.level=DEBUG
com.minewright.action.level=TRACE
```

**In-Game Logging:**

Logs appear in:
- Console output (when running `./gradlew runClient`)
- `run/logs/latest.log` file
- In-game chat (when debug mode is enabled)

### Common Debugging Scenarios

**Scenario 1: Agent Not Responding**

```java
// Check if LLM client is healthy
LOGGER.info("LLM client healthy: {}", llmClient.isHealthy());

// Check API key configuration
LOGGER.info("API key configured: {}", MineWrightConfig.hasValidApiKey());

// Check pending tasks
LOGGER.info("Pending tasks: {}", taskQueue.size());
```

**Scenario 2: Action Failing Silently**

```java
// Enable action logging
LOGGER.info("Starting action: {}", action.getDescription());

// Check result
ActionResult result = action.getResult();
if (!result.isSuccess()) {
    LOGGER.error("Action failed: {}", result.getMessage());
    LOGGER.error("Error context: {}", result.getErrorContext());
}
```

**Scenario 3: Performance Issues**

```java
// Profile tick execution
TickProfiler profiler = new TickProfiler();
profiler.startTick();

// ... AI operations ...

if (profiler.isOverBudget()) {
    LOGGER.warn("AI operations exceeded budget: {}ms / {}ms",
        profiler.getElapsedMs(), profiler.getBudgetMs());
}

profiler.stopTick();
```

### Remote Debugging (Server)

If you need to debug a running server:

1. **Start server with debug flags:**
   ```bash
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
        -jar forge-1.20.1-47.4.16-universal.jar nogui
   ```

2. **Connect IDE to remote JVM:**
   - IntelliJ: Run → Edit Configurations → Remote → +
   - Host: localhost, Port: 5005
   - Click Debug

---

## 6. Code Style

### Formatting Rules

| Rule | Value |
|------|-------|
| **Indentation** | 4 spaces (NO tabs) |
| **Line Length** | 120 characters (soft limit) |
| **Encoding** | UTF-8 |
| **Braces** | K&R style (opening brace on same line) |

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| **Classes** | PascalCase | `MineBlockAction` |
| **Methods** | camelCase | `getBlockAtPosition()` |
| **Variables** | camelCase | `targetBlock` |
| **Constants** | UPPER_SNAKE_CASE | `MAX_TICKS` |
| **Packages** | lowercase | `com.minewright.action` |

### JavaDoc Requirements

**All public APIs must have JavaDoc:**

```java
/**
 * Mines blocks of a specific type within a radius.
 *
 * <p>This action searches for blocks within the specified radius,
 * mines them until the target quantity is reached, then returns success.</p>
 *
 * <p><b>Parameters:</b></p>
 * <ul>
 *   <li>{@code block} - Block type to mine (e.g., "iron_ore")</li>
 *   <li>{@code quantity} - Number of blocks to mine (default: 8)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * Task task = TaskBuilder.builder()
 *     .action("mine")
 *     .parameter("block", "iron_ore")
 *     .parameter("quantity", 16)
 *     .build();
 * MineBlockAction action = new MineBlockAction(foreman, task);
 * }</pre>
 *
 * @see BaseAction
 * @see ActionResult
 * @since 1.0.0
 */
public class MineBlockAction extends BaseAction {
    // ...
}
```

### Code Examples

**Good:**

```java
// Clear, descriptive variable names
private int blocksMined;
private final Block targetBlock;

// Proper exception handling with context
try {
    mineBlock(currentTarget);
} catch (ActionException e) {
    LOGGER.error("Failed to mine block at {}: {}", currentTarget, e.getMessage());
    handleActionException(e);
}

// Null-safe operations
BlockPos target = getCurrentTarget();
if (target != null && level.getBlockState(target).is(targetBlock)) {
    level.destroyBlock(target, true);
}
```

**Bad:**

```java
// Poor variable names
private int x;
private Block b;

// Silent exception handling
try {
    mineBlock(currentTarget);
} catch (Exception e) {
    // Do nothing (bad!)
}

// Null-unsafe operations
level.destroyBlock(getCurrentTarget(), true);  // May NPE
```

### Compiler Warnings

The build uses strict compiler flags:

```gradle
options.compilerArgs += [
    '-Xlint:all',
    '-Xlint:-processing',
    '-Xlint:-deprecation',  // Suppress Minecraft API deprecations
    '-Xlint:-removal'       // Suppress Minecraft API removals
]
```

**Fix warnings before committing code!**

---

## 7. Adding a New Action

Actions are the primary way agents interact with the world. Each action extends `BaseAction` and implements three lifecycle methods.

### Step-by-Step Guide

**Step 1: Create the Action Class**

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.exception.ActionException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

public class SmeltItemAction extends BaseAction {
    private final String itemType;
    private final int quantity;
    private int smeltedCount;

    public SmeltItemAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
        this.itemType = task.getStringParameter("item");
        this.quantity = task.getIntParameter("quantity", 1);
        this.smeltedCount = 0;
    }

    @Override
    protected void onStart() throws ActionException {
        // Validate parameters
        requireParameter("item", "item type");

        // Initialize state
        LOGGER.info("[{}] Starting smelting: {}x {}",
            foreman.getEntityName(), quantity, itemType);

        // Find or create furnace
        findNearestFurnace();
    }

    @Override
    protected void onTick() {
        // Check if complete
        if (smeltedCount >= quantity) {
            succeed("Smelted " + smeltedCount + " " + itemType);
            return;
        }

        // Smelt one item
        if (canSmelt()) {
            smeltItem();
            smeltedCount++;
        }
    }

    @Override
    protected void onCancel() {
        // Clean up resources
        foreman.getNavigation().stop();
        LOGGER.info("[{}] Cancelled smelting at {}/{}",
            foreman.getEntityName(), smeltedCount, quantity);
    }

    @Override
    public String getDescription() {
        return String.format("Smelt %d %s (%d/%d)",
            quantity, itemType, smeltedCount, quantity);
    }

    // Helper methods
    private void findNearestFurnace() {
        // Implementation here
    }

    private boolean canSmelt() {
        // Check fuel, input, etc.
        return true;
    }

    private void smeltItem() {
        // Place item in furnace, wait for completion
    }
}
```

**Step 2: Register the Action**

Create a plugin class (or add to existing `CoreActionsPlugin`):

```java
package com.minewright.action.plugins;

import com.minewright.action.ActionFactory;
import com.minewright.action.ActionRegistry;
import com.minewright.action.actions.SmeltItemAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.action.Task;

public class CoreActionsPlugin {

    public static void registerActions(ActionRegistry registry) {
        registry.register("smelt", (ForemanEntity foreman, Task task, Object context) ->
            new SmeltItemAction(foreman, task));
    }
}
```

**Step 3: Test the Action**

```java
@Test
@DisplayName("SmeltItemAction should smelt items")
void testSmeltItemAction() {
    // Create task
    Task task = TaskBuilder.builder()
        .action("smelt")
        .parameter("item", "iron_ore")
        .parameter("quantity", 8)
        .build();

    // Create action
    SmeltItemAction action = new SmeltItemAction(mockForeman, task);

    // Start action
    action.start();
    assertFalse(action.isComplete());

    // Simulate ticks
    for (int i = 0; i < 100; i++) {
        action.tick();
        if (action.isComplete()) break;
    }

    // Verify result
    ActionResult result = action.getResult();
    assertTrue(result.isSuccess());
    assertEquals("Smelted 8 iron_ore", result.getMessage());
}
```

**Step 4: Use the Action**

Users can now command agents to smelt items:

```
/foreman order Steve "smelt 64 iron ore"
```

### Action Best Practices

1. **Always call `super()` constructor** to set up base state
2. **Validate parameters in `onStart()`** - fail fast
3. **Return immediately if complete** in `onTick()` - don't waste ticks
4. **Clean up in `onCancel()`** - stop navigation, release resources
5. **Log key events** - helps debugging
6. **Use `ActionResult` for results** - provides rich error context
7. **Never block in `onTick()`** - use async for long operations

---

## 8. Adding a New LLM Provider

The project uses a pluggable `AsyncLLMClient` interface for LLM providers.

### Step-by-Step Guide

**Step 1: Implement AsyncLLMClient Interface**

```java
package com.minewright.llm.async;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CustomLLMClient implements AsyncLLMClient {

    private static final String API_URL = "https://api.example.com/v1/chat";
    private static final String PROVIDER_ID = "custom";
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLLMClient.class);

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public CustomLLMClient(String apiKey, String model, int maxTokens, double temperature) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        // Build request body
        String requestBody = buildRequestBody(prompt, params);

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        // Send async request
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                } else {
                    throw new LLMException("API error: " + response.statusCode());
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("LLM request failed", throwable);
                throw new LLMException("Request failed: " + throwable.getMessage(), throwable);
            });
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isHealthy() {
        // Check circuit breaker state (if using ResilientLLMClient wrapper)
        return true;
    }

    private String buildRequestBody(String prompt, Map<String, Object> params) {
        // Build JSON request body
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("prompt", prompt);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);
        return body.toString();
    }

    private LLMResponse parseResponse(String responseBody) {
        // Parse JSON response
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        String content = json.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .get("message").getAsJsonObject()
            .get("content").getAsString();

        return new LLMResponse(content, "model", mapOf("tokens", 100));
    }
}
```

**Step 2: Register the Provider**

Update `LLMClientFactory` (create if not exists):

```java
package com.minewright.llm;

public class LLMClientFactory {

    public static AsyncLLMClient createClient(String provider, String apiKey) {
        switch (provider.toLowerCase()) {
            case "openai":
                return new AsyncOpenAIClient(apiKey, "glm-5", 8000, 0.7);
            case "groq":
                return new AsyncGroqClient(apiKey, "llama3-70b", 8192, 0.7);
            case "custom":
                return new CustomLLMClient(apiKey, "custom-model", 4000, 0.5);
            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }
}
```

**Step 3: Update Config**

Add to config file:

```toml
[ai]
provider = "custom"

[custom]
apiKey = "your-custom-api-key"
model = "custom-model"
maxTokens = 4000
temperature = 0.5
```

**Step 4: Test the Client**

```java
@Test
@DisplayName("CustomLLMClient should return responses")
void testCustomClient() {
    CustomLLMClient client = new CustomLLMClient("test-key", "model", 1000, 0.7);

    CompletableFuture<LLMResponse> future = client.sendAsync(
        "Test prompt",
        Map.of("model", "model")
    );

    LLMResponse response = future.join();
    assertNotNull(response.getContent());
    assertFalse(response.getContent().isEmpty());
}
```

### LLM Client Best Practices

1. **Never block** - use `HttpClient.sendAsync()` not `send()`
2. **Handle retries** - wrap with `ResilientLLMClient` for resilience
3. **Parse errors gracefully** - return `LLMException` for all failures
4. **Log requests/responses** - at DEBUG level for troubleshooting
5. **Set timeouts** - don't wait forever for API responses
6. **Use thread pools** - `LLMExecutorService` provides dedicated threads

---

## 9. Performance Profiling

The project includes `TickProfiler` for monitoring AI operation performance.

### Using TickProfiler

**Basic Usage:**

```java
@Override
public void tick() {
    TickProfiler profiler = new TickProfiler();
    profiler.startTick();

    // ... AI operations ...

    if (profiler.isOverBudget()) {
        LOGGER.warn("AI operations exceeded budget!");
        // Defer remaining work to next tick
        return;
    }

    profiler.stopTick();
}
```

**Advanced Usage:**

```java
// Profile specific operations
TickProfiler profiler = new TickProfiler(10L);  // 10ms budget
profiler.startTick();

// Operation 1
long op1Start = System.nanoTime();
doPathfinding();
long op1Elapsed = (System.nanoTime() - op1Start) / 1_000_000L;

if (profiler.isOverWarningThreshold()) {
    LOGGER.warn("Pathfinding took {}ms (budget: {}ms)",
        op1Elapsed, profiler.getBudgetMs());
}

// Operation 2
long op2Start = System.nanoTime();
doActionSelection();
long op2Elapsed = (System.nanoTime() - op2Start) / 1_000_000L;

profiler.stopTick();
```

### Configuring Tick Budget

Edit `config/steve-common.toml`:

```toml
[performance]
aiTickBudgetMs = 5  # AI must complete in 5ms
budgetWarningThreshold = 80  # Warn at 80% of budget (4ms)
strictBudgetEnforcement = true  # Defer work when over budget
```

### Profiling Output

**Normal Output:**
```
[DEBUG] Tick usage: 3ms / 5ms (60% of budget)
```

**Warning Output:**
```
[WARN] AI tick usage approaching limit: 4ms / 5ms (80% used)
```

**Error Output:**
```
[ERROR] Tick budget exceeded: 7ms / 5ms (40% over budget)
```

### Performance Guidelines

| Operation | Target Time | Max Time |
|-----------|-------------|----------|
| **Pathfinding** | <1ms | 2ms |
| **Action Selection** | <0.5ms | 1ms |
| **State Updates** | <0.5ms | 1ms |
| **Total AI Budget** | <5ms | 10ms |

**If operations exceed budget:**
1. Reduce search space (fewer nodes in pathfinding)
2. Cache results (semantic cache, LLM cache)
3. Defer work to next tick (batch processing)
4. Increase budget (if server can handle it)

---

## 10. Common Issues

### Issue 1: "Failed to load minecraft manifests"

**Symptom:** Gradle build fails with manifest download errors

**Solution:**
```bash
# Clear Gradle cache
./gradlew clean --refresh-dependencies

# On Windows:
gradlew.bat clean --refresh-dependencies
```

### Issue 2: "API key not configured"

**Symptom:** Agents don't respond, console shows "API key is not configured"

**Solution:**
1. Check config file exists: `run/config/steve-common.toml`
2. Verify API key is set (not empty)
3. Reload config: `/reload` in-game
4. Check logs: `run/logs/latest.log`

### Issue 3: "Out of Memory" during build

**Symptom:** Gradle build crashes with `OutOfMemoryError`

**Solution:**
```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx4G -XX:MaxMetaspaceSize=512m"

# On Windows (PowerShell):
$env:GRADLE_OPTS="-Xmx4G -XX:MaxMetaspaceSize=512m"
```

### Issue 4: Agent stuck doing nothing

**Symptom:** Agent spawns but doesn't execute tasks

**Diagnosis:**
1. Check agent state: `/foreman list`
2. Check pending tasks: Look at logs for "No tasks to execute"
3. Check LLM health: Look for "LLM client unhealthy" in logs

**Solution:**
```java
// Force replan
/foreman order <name> "replan"

// Check API key is valid
// Test LLM connectivity
curl -H "Authorization: Bearer $API_KEY" \
  https://api.z.ai/api/paas/v4/chat/completions
```

### Issue 5: "Circuit breaker is OPEN"

**Symptom:** All LLM calls fail immediately, logs show "Circuit breaker OPEN"

**Cause:** Too many failed LLM requests (rate limiting, network errors)

**Solution:**
1. Wait for circuit breaker to reset (default: 60 seconds)
2. Check API key is valid
3. Check API quota (rate limits)
4. Reduce request frequency

### Issue 6: Tests fail with "MockForemanEntity not found"

**Symptom:** Unit tests fail to compile or run

**Cause:** Test infrastructure not properly set up

**Solution:**
```bash
# Clean and rebuild
./gradlew clean build

# Run tests with full classpath
./gradlew test --info

# Check test sources exist
ls src/test/java/com/minewright/behavior/MockForemanEntity.java
```

### Issue 7: Hot reload not working

**Symptom:** Code changes don't appear in-game

**Solution:**
```bash
# Full rebuild required (no hot reload in Minecraft)
./gradlew clean build runClient
```

### Issue 8: "Cannot find ForemanEntity class"

**Symptom:** Compilation fails with "cannot find symbol: class ForemanEntity"

**Cause:** Minecraft classes not deobfuscated

**Solution:**
```bash
# Setup decompiled workspace
./gradlew setupDecompileWorkspace

# Regenerate resources
./gradlew genSources
```

### Getting Help

If none of these solutions work:

1. **Check logs:** `run/logs/latest.log` (search for "ERROR" or "WARN")
2. **Enable debug logging:** Set `com.minewright.level=DEBUG` in config
3. **GitHub Issues:** Check [GitHub Issues](https://github.com/your-repo/issues)
4. **Discord/Community:** Join the community Discord for help

---

## Quick Reference

### Essential Commands

```bash
# Build
./gradlew build

# Run client
./gradlew runClient

# Run tests
./gradlew test

# Create distribution JAR
./gradlew shadowJar reobfShadowJar

# Clean build
./gradlew clean build
```

### In-Game Commands

```
/foreman spawn <name>        - Spawn a new agent
/foreman list                 - List all agents
/foreman remove <name>        - Remove an agent
/foreman order <name> <cmd>   - Issue work order
/reload                       - Reload config
```

### Key Files

| File | Purpose |
|------|---------|
| `build.gradle` | Build configuration |
| `CLAUDE.md` | Project overview |
| `config/steve-common.toml` | Mod configuration |
| `src/main/java/com/minewright/action/actions/` | Action implementations |
| `src/main/java/com/minewright/llm/async/` | LLM clients |
| `src/test/java/` | Unit tests |

---

## Next Steps

1. **Read CLAUDE.md** - Project overview and architecture
2. **Explore actions** - See `src/main/java/com/minewright/action/actions/`
3. **Write a test** - Add test coverage to existing code
4. **Implement an action** - Follow "Adding a New Action" guide
5. **Profile performance** - Use TickProfiler to optimize

Happy coding! 🎮

---

**Document Version:** 1.0.0
**Last Updated:** 2026-02-28
**Maintained By:** Steve AI Development Team
