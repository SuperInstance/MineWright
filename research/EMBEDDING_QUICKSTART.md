# Embedding Model Quickstart Guide

**Fast-track implementation for MineWright memory system**

---

## TL;DR: Recommended Approach

```gradle
// Add to build.gradle
implementation 'dev.langchain4j:langchain4j:0.34.0'
implementation 'dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.34.0'
```

```java
// Replace PlaceholderEmbeddingModel with:
EmbeddingModel embeddingModel = new LangChainEmbeddingModel();
```

That's it! You now have production-quality semantic embeddings.

---

## Implementation Checklist

- [ ] Add LangChain4j dependencies to build.gradle
- [ ] Create `LangChainEmbeddingModel.java`
- [ ] Update dependency injection configuration
- [ ] Test with existing memory system
- [ ] Add Caffeine caching layer
- [ ] (Optional) Upgrade to jVector for faster search
- [ ] (Optional) Add pre-computation for historical data

---

## Three-Line Implementation

If you want the absolute minimal implementation:

```java
public class SimpleEmbeddingModel implements EmbeddingModel {
    private final EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();

    public float[] embed(String text) {
        return model.embed(text).content().vectorAsList()
            .stream().mapToFloat(Float::floatValue).toArray();
    }

    public int getDimension() { return 384; }
    public String getModelName() { return "all-MiniLM-L6-v2"; }
}
```

---

## Performance Expectations

With LangChain4j + all-MiniLM-L6-v2:

| Operation | Expected Time |
|-----------|---------------|
| First load (cold start) | ~2-3 seconds |
| Single embedding | ~50-100ms |
| Cached embedding | <1ms |
| 100 embeddings (batch) | ~2-3 seconds |
| Vector search (1000 items) | ~10ms |

---

## Common Issues & Solutions

### Issue 1: Out of Memory

**Problem:** Minecraft crashes with "Java heap space"

**Solution:** Allocate more RAM to Minecraft
```bash
java -Xmx4G -Xms2G ...
```

### Issue 2: Slow First Load

**Problem:** Game freezes when creating first MineWright

**Solution:** Load model async at startup
```java
CompletableFuture.runAsync(() -> {
    embeddingModel = new LangChainEmbeddingModel();
});
```

### Issue 3: Model Not Found

**Problem:** "Failed to download model" error

**Solution:** Check internet connection or bundle model
```java
// Option A: Download once, ship with mod
// Option B: Fallback to placeholder
if (!modelAvailable) {
    return new PlaceholderEmbeddingModel();
}
```

### Issue 4: Slow Embedding Generation

**Problem:** Memory search causes lag spikes

**Solution:** Enable caching
```java
private final Cache<String, float[]> cache = Caffeine.newBuilder()
    .maximumSize(1000)
    .build();
```

---

## Testing Your Implementation

```java
// Quick sanity check
EmbeddingModel model = new LangChainEmbeddingModel();

// Test 1: Dimension
float[] e = model.embed("hello world");
assert e.length == 384;

// Test 2: Consistency
float[] e1 = model.embed("test");
float[] e2 = model.embed("test");
assert Arrays.equals(e1, e2);

// Test 3: Semantic similarity
float[] house = model.embed("minecraft house");
float[] building = model.embed("minecraft building");
float[] apple = model.embed("red apple");

double house_building = cosineSimilarity(house, building);
double house_apple = cosineSimilarity(house, apple);

assert house_building > house_apple; // Related > Unrelated
```

---

## Migration from Placeholder

**Before:**
```java
EmbeddingModel model = new PlaceholderEmbeddingModel(384, "placeholder");
```

**After:**
```java
EmbeddingModel model = new LangChainEmbeddingModel();
```

Everything else stays the same! The interface is identical.

---

## When to Upgrade

**Stay with LangChain4j if:**
- You have <100K memory entries
- Query latency <100ms is acceptable
- You want simplicity

**Upgrade to ONNX if:**
- You need faster embedding generation
- You want quantization (INT8) for 4x speedup
- You're comfortable with model conversion

**Upgrade to jVector if:**
- You have >10K memory entries
- Query latency is becoming an issue
- You need millisecond-level search

---

## Build Configuration

**Minimum (build.gradle):**
```gradle
dependencies {
    implementation 'dev.langchain4j:langchain4j:0.34.0'
    implementation 'dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.34.0'
}

shadowJar {
    relocate 'dev.langchain4j', 'com.minewright.shaded.dev.langchain4j'
}
```

**With jVector upgrade:**
```gradle
dependencies {
    // ... LangChain4j above ...
    implementation 'io.github.jbellis:jvector:4.0.0'
}
```

---

## Performance Tuning

### 1. Enable Caching (Critical)

```java
private final Cache<String, float[]> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .recordStats()
    .build();

public float[] embed(String text) {
    return cache.get(text, t -> generateEmbedding(t));
}
```

### 2. Batch Embeddings

```java
// Instead of:
for (String text : texts) {
    embed(text);  // Slow
}

// Do this:
float[][] embeddings = embedBatch(texts);  // 2-3x faster
```

### 3. Pre-compute Static Data

```java
// At startup, embed all historical memory
for (MemoryEntry entry : history) {
    float[] embedding = model.embed(entry.getText());
    store.add(embedding, entry);
}
```

---

## Monitoring

Add metrics to track embedding performance:

```java
public class EmbeddingModelWithMetrics implements EmbeddingModel {
    private final EmbeddingModel delegate;
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalTime = new AtomicLong(0);

    public float[] embed(String text) {
        long start = System.nanoTime();
        float[] result = delegate.embed(text);
        long elapsed = System.nanoTime() - start;

        totalCalls.incrementAndGet();
        totalTime.addAndGet(elapsed);

        return result;
    }

    public double getAverageLatencyMs() {
        long calls = totalCalls.get();
        return calls == 0 ? 0 : (totalTime.get() / 1_000_000.0) / calls;
    }
}
```

---

## FAQ

**Q: Will this work offline?**
A: Yes! After the first download, the model is cached locally.

**Q: How much disk space?**
A: ~100MB for the model file.

**Q: How much memory?**
A: ~200MB RAM while loaded.

**Q: Will this lag my game?**
A: No, if you use async embedding and caching. The first load takes ~2-3 seconds.

**Q: Can I use a different model?**
A: Yes, LangChain4j supports bge-small, e5-small, and others. See main doc.

**Q: What about GPU acceleration?**
A: LangChain4j is CPU-only. For GPU, use ONNX Runtime.

---

## Next Steps

1. **Now:** Add LangChain4j dependency and implement
2. **Week 1:** Test with existing memory system
3. **Month 1:** Add caching and pre-computation
4. **Quarter 1:** Consider ONNX/jVector upgrades if needed

---

## Support

- Main documentation: `research/JAVA_EMBEDDING_MODELS.md`
- LangChain4j docs: https://docs.langchain4j.dev/
- GitHub issues: https://github.com/langchain4j/langchain4j

---

*Quickstart guide for MineWright. Last updated: 2025-02-26*
