# Performance Optimization Quick Reference

**For MineWright Mod Development**

## Golden Rules

1. **NEVER block the main thread** - All LLM calls must be async
2. **NEVER modify game state from background threads** - Schedule to main thread
3. **ALWAYS use thread-safe collections** - ConcurrentHashMap, CopyOnWriteArrayList
4. **ALWAYS cache LLM responses** - 40-60% cache hit rates achievable
5. **ALWAYS profile before optimizing** - VisualVM, Spark, metrics

## Threading Cheat Sheet

```java
// ✅ SAFE: Async LLM call
CompletableFuture<Response> future = client.sendAsync(prompt);

// ✅ SAFE: Read-only world access from async
Level level = minewright.level();  // Safe reference
BlockState state = level.getBlockState(pos);  // Unsafe! Must copy first

// ❌ UNSAFE: Block changes from async
level.setBlock(pos, newState);  // CRASH!
// ✅ FIX: Schedule to main thread
Minecraft.getInstance().execute(() -> level.setBlock(pos, newState));

// ❌ UNSAFE: Entity modifications from async
minewright.setPos(x, y, z);  // CRASH!
// ✅ FIX: Use server.execute()
if (!level.isClientSide) {
    ((ServerLevel)level).server.execute(() -> minewright.setPos(x, y, z));
}
```

## Performance Targets

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Main thread blocking | <1ms per tick | MetricsInterceptor |
| LLM latency impact | 0ms (async) | Tick timing |
| Memory per Foreman | <100MB | VisualVM heap histogram |
| Cache hit rate | >50% | LLMCache.getStats() |
| TPS under load | 20 TPS | /tps command |

## Quick Optimization Checklist

- [ ] All LLM calls return CompletableFuture immediately
- [ ] No Thread.sleep() or blocking waits in tick handlers
- [ ] World data accessed once, cached, then processed async
- [ ] Collections use ConcurrentHashMap or CopyOnWriteArrayList
- [ ] Cached data has size limits (LRU eviction)
- [ ] Entity references use UUIDs or WeakReferences
- [ ] NBT saves only essential data (not entire world scans)
- [ ] Expensive operations throttled (every N ticks)
- [ ] Event handlers use early exit patterns
- [ ] Thread pools properly shut down on server stop

## Common Pitfalls (DON'T DO THIS)

```java
// ❌ Blocks tick for 30 seconds
String response = llmClient.sendRequestSync(prompt);

// ❌ Memory leak - never cleared
private static final Map<UUID, ForemanData> CACHE = new HashMap<>();

// ❌ Thread safety issue
private final List<ForemanEntity> entities = new ArrayList<>();

// ❌ Modifies game state async
CompletableFuture.runAsync(() -> {
    level.setBlock(pos, state);  // CRASH!
});
```

## Profiling Commands

```bash
# Enable JMX for VisualVM
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false

# Enable heap dumps on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=./crash-reports/

# Use G1GC for better pause times
-XX:+UseG1GC
-XX:MaxGCPauseMillis=40
```

## Key Files for Performance

- `C:\Users\casey\minewright\src\main\java\com\minewright\llm\async\LLMExecutorService.java` - Thread pool management
- `C:\Users\casey\minewright\src\main\java\com\minewright\llm\async\LLMCache.java` - Response caching
- `C:\Users\casey\minewright\src\main\java\com\minewright\execution\MetricsInterceptor.java` - Performance tracking
- `C:\Users\casey\minewright\src\main\java\com\minewright\action\ActionExecutor.java` - Tick-based execution

## Quick Reference Links

- [Full Performance Guide](MINECRAFT_PERFORMANCE.md) - Comprehensive optimization strategies
- [VisualVM Download](https://visualvm.github.io/) - JVM profiling tool
- [Spark Profiler](https://www.curseforge.com/minecraft/mc-mods/spark) - In-game profiler
- [Minecraft Forge Docs](https://docs.minecraftforge.net/) - Official documentation
