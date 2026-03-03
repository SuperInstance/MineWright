# Known Issues and Workarounds

**Common problems, their causes, and how to fix them.**

Last Updated: 2026-03-03

---

## Critical Issues

### 1. Windows File Lock on Test Output

**Symptom**:
```
Execution failed for task ':test'.
> java.io.IOException: Unable to delete directory '...\build\test-results\test\binary'
    Failed to delete some children...
    - ...\build\test-results\test\binary\output.bin
```

**Cause**: Windows holds file locks on test output files. A previous Gradle daemon or IDE process has the file open.

**Workarounds**:
1. **Close all IDEs** (IntelliJ, Eclipse, VS Code) and retry
2. **Kill Gradle daemons**: `./gradlew --stop`
3. **Restart computer** (most reliable)
4. **Use WSL**: Run tests in Windows Subsystem for Linux

**Permanent Fix**: Add to `build.gradle`:
```groovy
test {
    // Use separate output directory per run
    outputs.dir(file("$buildDir/test-results/test-${System.currentTimeMillis()}"))
}
```

---

### 2. API Key Not Found

**Symptom**:
```
[ERROR] Cannot plan tasks: API key not configured.
Please check config/minewright-common.toml
```

**Cause**: API key not set in config or environment variable.

**Solution**:
1. **Set environment variable** (recommended):
   ```bash
   # Linux/Mac
   export OPENAI_API_KEY="sk-..."

   # Windows PowerShell
   $env:OPENAI_API_KEY="sk-..."
   ```

2. **Or set in config file** `config/steve-common.toml`:
   ```toml
   [openai]
   apiKey = "sk-..."  # Not recommended for git repos
   ```

3. **Verify configuration**:
   ```java
   boolean hasKey = MineWrightConfig.hasValidApiKey();
   ```

---

### 3. LLM Request Timeout

**Symptom**:
```
[Cascade] Task planning timed out after 180000ms for command: ...
```

**Cause**: LLM API taking too long to respond, or network issues.

**Solutions**:
1. **Check API status**: Verify provider is not experiencing outages
2. **Use faster model**: Switch to Groq for faster inference
3. **Increase timeout** (not recommended):
   ```java
   // In TaskPlanner.java
   private static final long PLAN_TASKS_TIMEOUT_MS = 300000; // 5 minutes
   ```
4. **Enable local LLM**: Set up Ollama for free, fast inference
   ```bash
   ollama run llama3.2
   ```

---

## Build Issues

### 4. Gradle Out of Memory

**Symptom**:
```
Expiring Daemon because JVM heap space is exhausted
```

**Solution**: Increase JVM heap in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G -XX:+HeapDumpOnOutOfMemoryError
```

### 5. Forge Setup Fails

**Symptom**:
```
Failed to apply plugin [id 'net.minecraftforge.gradle']
```

**Solution**:
1. Ensure Java 17 is installed: `java -version`
2. Check Gradle version: `./gradlew --version` (need 8.x)
3. Delete `.gradle` folder and retry
4. Check internet connection (needs to download Minecraft)

### 6. Annotation Processor Errors

**Symptom**:
```
Bad service configuration file, or exception thrown while constructing Processor
```

**Solution**:
1. Clean build: `./gradlew clean`
2. Invalidate IDE caches
3. Delete `build/` and `.gradle/` folders
4. Re-import project in IDE

---

## Runtime Issues

### 7. Agent Not Spawning

**Symptom**: `/foreman spawn Test` command does nothing

**Causes & Solutions**:

1. **Entity not registered**:
   ```java
   // Check in MineWrightMod.java
   @SubscribeEvent
   public static void onRegisterEntities(final RegisterEvent event) {
       // Entity registration code
   }
   ```

2. **Missing entity type**:
   ```java
   // Verify entity type is created
   public static final EntityType<ForemanEntity> FOREMAN =
       EntityType.Builder.of(ForemanEntity::new, MobCategory.CREATURE)
           .build("foreman");
   ```

3. **World is null**: Check you're not calling during world load

### 8. Agent Stuck in Place

**Symptom**: Agent spawned but not moving

**Diagnosis**:
1. Check pathfinding: `StuckDetector.isStuck()`
2. Check state machine: `AgentStateMachine.getCurrentState()`
3. Check navigation: `pathfinder.findPath()`

**Solutions**:
1. Force repath: Call `recoveryManager.recover(StuckType.POSITION_STUCK)`
2. Check for obstacles: `MovementValidator.isSafe(pos)`
3. Verify goal is set: `agent.getCurrentGoal()`

### 9. LLM Returns Invalid JSON

**Symptom**:
```
[ERROR] Failed to parse AI response
```

**Cause**: LLM returned malformed or non-JSON response

**Solution**: The `ResponseParser` handles this, but you can improve prompts:
```java
// In PromptBuilder.java
private static final String JSON_FORMAT_INSTRUCTION = """
    IMPORTANT: Respond ONLY with valid JSON in this exact format:
    {"plan": "...", "tasks": [{"action": "...", "params": {...}}]}

    Do not include any text before or after the JSON.
    """;
```

---

## IDE Issues

### 10. IntelliJ Can't Find Minecraft Classes

**Symptom**: Red underlines on Minecraft classes like `Level`, `BlockPos`

**Solution**:
1. Run `./gradlew genIntellijRuns`
2. Refresh Gradle project
3. Invalidate caches: File → Invalidate Caches → Invalidate and Restart

### 11. VS Code Java Extension Issues

**Symptom**: Java extension keeps loading, can't resolve imports

**Solution**:
1. Install "Gradle for Java" extension
2. Run `Java: Clean Java Language Server Workspace`
3. Add to `.vscode/settings.json`:
   ```json
   {
     "java.project.sourcePaths": ["src/main/java", "src/test/java"],
     "java.configurations.runtimes": [
       {
         "name": "JavaSE-17",
         "path": "/path/to/jdk-17",
         "default": true
       }
     ]
   }
   ```

---

## Test Issues

### 12. Mockito Can't Mock Minecraft Classes

**Symptom**:
```
Mockito cannot mock this class: class net.minecraft.world.level.Level
```

**Cause**: Minecraft classes are final or have private constructors

**Solution**: Use interfaces or create test doubles:
```java
// Instead of mocking Minecraft classes
public interface WorldAccess {
    Block getBlock(BlockPos pos);
    void setBlock(BlockPos pos, Block block);
}

// Create test implementation
class TestWorld implements WorldAccess {
    private final Map<BlockPos, Block> blocks = new HashMap<>();

    @Override
    public Block getBlock(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR);
    }
}
```

### 13. Tests Pass Locally But Fail in CI

**Symptom**: GitHub Actions tests fail but local passes

**Causes & Solutions**:

1. **Different Java version**: Ensure CI uses Java 17
2. **Environment variables**: Set API keys in GitHub Secrets
3. **File path issues**: Use `Path` instead of `File` for cross-platform
4. **Timing issues**: Add explicit waits or use `awaitility`

---

## Configuration Issues

### 14. Config File Not Loading

**Symptom**: Default values used instead of config values

**Solution**:
1. Check file location: `config/steve-common.toml`
2. Check file format (TOML):
   ```toml
   [section]
   key = "value"  # Correct
   key=value      # May fail
   ```
3. Verify Forge config system is initialized

### 15. Config Changes Not Applied

**Symptom**: Changed config but behavior unchanged

**Solution**:
1. Restart game/server (config read at startup)
2. Check for config change events:
   ```java
   @SubscribeEvent
   public void onConfigChange(ConfigChangeEvent event) {
       // Reload config values
   }
   ```

---

## Performance Issues

### 16. High Memory Usage

**Symptom**: Minecraft using >4GB RAM with mod

**Diagnosis**:
1. Check agent count: Each agent uses ~5MB
2. Check memory system: `CompanionMemory.size()`
3. Check pathfinding cache: `PathfinderCache.size()`

**Solutions**:
1. Limit max agents in config
2. Add memory size limits:
   ```java
   // In CompanionMemory.java
   private static final int MAX_MEMORIES = 1000;
   ```
3. Clear caches periodically

### 17. Low TPS (Ticks Per Second)

**Symptom**: Game lagging, TPS < 20

**Diagnosis**:
1. Profile with `TickProfiler`
2. Check action execution time
3. Check pathfinding calculations

**Solutions**:
1. Optimize pathfinding: Use hierarchical for long distances
2. Batch LLM calls: Enable `batching_enabled = true`
3. Reduce per-tick work in actions

---

## Debugging Tips

### Enable Debug Logging

In `config/steve-common.toml`:
```toml
[logging]
level = "DEBUG"
llm_requests = true
action_execution = true
```

### Check Agent State

```java
// In-game command or debug
/foreman status <name>

// Or programmatically
AgentStateMachine machine = foreman.getStateMachine();
AgentState state = machine.getCurrentState();
LOGGER.info("Agent state: {}", state);
```

### Trace LLM Calls

```java
// Enable in GLMCascadeRouter
LOGGER.debug("[Cascade] Request to model: {}", model);
LOGGER.debug("[Cascade] Response time: {}ms", latency);
LOGGER.debug("[Cascade] Tokens used: {}", tokens);
```

---

## Getting More Help

1. **Check GitHub Issues**: https://github.com/SuperInstance/MineWright/issues
2. **Read Documentation**: `docs/research/` has detailed guides
3. **Review Code**: Well-documented, look at similar implementations
4. **Ask in Discord**: (If available)

---

## Reporting New Issues

When reporting, include:

1. **Steps to reproduce**
2. **Expected behavior**
3. **Actual behavior**
4. **Logs** (from `logs/latest.log`)
5. **Environment**:
   - Java version: `java -version`
   - Minecraft version
   - Forge version
   - OS (Windows/Mac/Linux)

---

*Last Updated: 2026-03-03*
*Report new issues at: https://github.com/SuperInstance/MineWright/issues*
