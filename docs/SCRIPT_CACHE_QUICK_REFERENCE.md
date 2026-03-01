# ScriptCache Quick Reference

**For Developers**

---

## Instant Setup

```java
// Default configuration (recommended for most cases)
ScriptCache cache = new ScriptCache();
```

**Default Settings:**
- Max scripts: 100
- Min similarity: 75%
- Max age: 24 hours
- Min executions: 3
- Min success rate: 40%

---

## Common Patterns

### Pattern 1: Basic Cache Lookup

```java
public Script getScript(String command) {
    return cache.findSimilar(command)
        .orElseGet(() -> {
            Script script = llmService.generateScript(command);
            cache.store(command, script);
            return script;
        });
}
```

### Pattern 2: Track Execution Results

```java
public void executeWithTracking(Script script) {
    try {
        executor.execute(script);
        cache.recordSuccess(script.getId());
    } catch (Exception e) {
        cache.recordFailure(script.getId());
        throw e;
    }
}
```

### Pattern 3: Periodic Cleanup

```java
@Scheduled(fixedRate = 3600000)  // Every hour
public void maintainCache() {
    int removed = cache.cleanup();
    logger.info("Cache cleanup: {} scripts removed", removed);
}
```

---

## Configuration Examples

### High-Precision Cache

```java
// Only reuse very similar commands
ScriptCache cache = new ScriptCache(
    100,    // maxSize
    0.90,   // minSimilarity (90%)
    86400000,  // maxAgeMs (24 hours)
    5,      // minExecutions
    0.7     // minSuccessRate (70%)
);
```

### High-Recall Cache

```java
// Reuse broadly similar commands
ScriptCache cache = new ScriptCache(
    200,    // maxSize (larger)
    0.60,   // minSimilarity (60%)
    86400000,  // maxAgeMs (24 hours)
    2,      // minExecutions (lower)
    0.3     // minSuccessRate (30%)
);
```

### Development Cache

```java
// Lenient settings for testing
ScriptCache cache = new ScriptCache(
    50,     // maxSize (smaller)
    0.70,   // minSimilarity (70%)
    3600000,   // maxAgeMs (1 hour)
    1,      // minExecutions (very low)
    0.0     // minSuccessRate (0% - don't filter)
);
```

---

## Monitoring

### Check Cache Health

```java
ScriptCache.CacheStats stats = cache.getStats();

logger.info("Cache Stats:");
logger.info("  Size: {}/{}", stats.getCurrentSize(), stats.getMaxSize());
logger.info("  Hit Rate: {:.1f}%", stats.getHitRate() * 100);
logger.info("  Total Executions: {}", stats.getTotalExecutions());
logger.info("  Success Rate: {:.1f}%", stats.getOverallSuccessRate() * 100);
logger.info("  Evictions: {}", stats.getTotalEvictions());
```

### Expected Values

| Metric | Healthy | Warning | Critical |
|--------|---------|---------|----------|
| Hit Rate | > 60% | 40-60% | < 40% |
| Success Rate | > 70% | 50-70% | < 50% |
| Cache Size | 60-80% | 80-95% | > 95% |

---

## Troubleshooting

### Problem: Low Hit Rate

**Symptoms:** Hit rate < 40%

**Solutions:**
1. Lower `minSimilarity` threshold (try 0.65)
2. Increase `maxSize` to store more scripts
3. Check if commands are too varied
4. Review embedding quality

### Problem: Low Success Rate

**Symptoms:** Overall success rate < 50%

**Solutions:**
1. Lower `minSuccessRate` threshold to 0.3
2. Increase `minExecutions` to get more data
3. Review script generation quality
4. Check if scripts are domain-appropriate

### Problem: Cache Always Full

**Symptoms:** Current size = Max size

**Solutions:**
1. Increase `maxSize`
2. Run `cleanup()` more frequently
3. Lower `minSuccessRate` to remove more scripts
4. Reduce `maxAgeMs` for faster eviction

### Problem: Too Many False Positives

**Symptoms:** Retrieved scripts don't match commands

**Solutions:**
1. Increase `minSimilarity` threshold (try 0.85)
2. Review semantic similarity quality
3. Consider upgrading embedding model
4. Add domain-specific vocabulary

---

## Performance Tips

### 1. Warm Up the Cache

```java
// Pre-load with common commands
List<String> commonCommands = List.of(
    "build a house",
    "mine for diamonds",
    "craft tools",
    "attack enemies"
);

for (String command : commonCommands) {
    Script script = scriptGenerator.generate(command);
    cache.store(command, script);
}
```

### 2. Batch Cleanup

```java
// Don't run cleanup too frequently
@Scheduled(fixedRate = 3600000)  // Every hour, not every minute
public void scheduledCleanup() {
    cache.cleanup();
}
```

### 3. Monitor and Adjust

```java
// Periodically review and adjust configuration
public void reviewCachePerformance() {
    CacheStats stats = cache.getStats();

    if (stats.getHitRate() < 0.5) {
        logger.warn("Low hit rate, consider lowering minSimilarity");
    }

    if (stats.getCurrentSize() >= stats.getMaxSize() * 0.9) {
        logger.warn("Cache nearly full, consider increasing maxSize");
    }

    if (stats.getOverallSuccessRate() < 0.6) {
        logger.warn("Low success rate, review script generation quality");
    }
}
```

---

## API Quick Reference

### Core Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `findSimilar(command)` | `Optional<Script>` | Find similar script (default threshold) |
| `findSimilar(command, threshold)` | `Optional<Script>` | Find similar script (custom threshold) |
| `store(command, script)` | `void` | Store script in cache |
| `recordSuccess(scriptId)` | `void` | Track successful execution |
| `recordFailure(scriptId)` | `void` | Track failed execution |
| `cleanup()` | `int` | Remove stale/low-performing scripts |
| `getStats()` | `CacheStats` | Get cache statistics |
| `clear()` | `void` | Remove all scripts |
| `remove(scriptId)` | `boolean` | Remove specific script |

### CacheStats Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getCurrentSize()` | `int` | Number of cached scripts |
| `getMaxSize()` | `int` | Maximum cache size |
| `getTotalHits()` | `int` | Total cache hits |
| `getTotalMisses()` | `int` | Total cache misses |
| `getTotalEvictions()` | `int` | Total evictions |
| `getTotalExecutions()` | `int` | Total script executions |
| `getTotalSuccesses()` | `int` | Total successful executions |
| `getHitRate()` | `double` | Cache hit rate (0-1) |
| `getOverallSuccessRate()` | `double` | Overall success rate (0-1) |

---

## Common Pitfalls

### ❌ Don't: Cache Every Script

```java
// Bad: Caches everything, including one-off commands
cache.store(command, script);  // Always stores
```

### ✅ Do: Cache Selectively

```java
// Good: Only cache reusable commands
if (isReusableCommand(command)) {
    cache.store(command, script);
}
```

### ❌ Don't: Ignore Failures

```java
// Bad: Doesn't track performance
executor.execute(script);
```

### ✅ Do: Track All Executions

```java
// Good: Tracks both success and failure
try {
    executor.execute(script);
    cache.recordSuccess(script.getId());
} catch (Exception e) {
    cache.recordFailure(script.getId());
    throw e;
}
```

### ❌ Don't: Set Thresholds Too High

```java
// Bad: Almost no cache hits
new ScriptCache(100, 0.99, ...);  // 99% similarity = nearly exact match only
```

### ✅ Do: Use Reasonable Thresholds

```java
// Good: Balanced precision and recall
new ScriptCache(100, 0.75, ...);  // 75% similarity = semantic matches
```

---

## Testing

### Unit Test Example

```java
@Test
void testCacheHit() {
    ScriptCache cache = new ScriptCache();

    Script script = Script.builder()
        .metadata(Script.ScriptMetadata.builder()
            .id("test-1")
            .name("Test")
            .build())
        .scriptNode(ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build())
        .build();

    cache.store("build a house", script);

    Optional<Script> found = cache.findSimilar("construct a house");
    assertTrue(found.isPresent());
    assertEquals("test-1", found.get().getId());
}
```

### Integration Test Example

```java
@Test
void testEndToEnd() {
    ScriptCache cache = new ScriptCache();
    ScriptGenerator generator = new ScriptGenerator(llmClient);

    // First call - cache miss
    Script script1 = generator.generateWithCache("build a house", cache);
    assertEquals(1, cache.size());

    // Second call - cache hit
    Script script2 = generator.generateWithCache("construct a house", cache);
    assertEquals(script1.getId(), script2.getId());
}
```

---

## Best Practices

1. **Start with defaults**, then tune based on metrics
2. **Monitor hit rate** - aim for > 60%
3. **Track executions** - always record success/failure
4. **Run cleanup periodically** - every hour is good
5. **Review stats weekly** - adjust configuration as needed
6. **Test before deploying** - use development cache first
7. **Document configuration** - note why you chose specific values

---

## Resources

- **Full Documentation:** `docs/SCRIPT_CACHE_IMPLEMENTATION.md`
- **Implementation Summary:** `docs/SCRIPT_CACHE_SUMMARY.md`
- **Source Code:** `src/main/java/com/minewright/script/ScriptCache.java`
- **Tests:** `src/test/java/com/minewright/script/ScriptCacheTest.java`

---

**Last Updated:** 2026-03-01
**Version:** 1.0
