# Vector Search Optimization for MineWright

## Executive Summary

The current `InMemoryVectorStore` implementation uses O(n) linear scan for vector similarity search, which becomes prohibitively slow as the dataset grows. This document researches and recommends optimization strategies for vector search in the MineWright Minecraft mod.

**Current Implementation:** `/src/main/java/com/minewright/memory/vector/InMemoryVectorStore.java`
- **Algorithm:** Linear scan with cosine similarity
- **Complexity:** O(n) per search, O(n log n) for sorting results
- **Current Limit:** 200 episodic memories (MAX_EPISODIC_MEMORIES)
- **Problem:** Each search iterates through ALL vectors, computing similarity for each

---

## 1. HNSW (Hierarchical Navigable Small World) Algorithm

### Overview

HNSW is a graph-based approximate nearest neighbor (ANN) algorithm that provides O(log n) search complexity with high recall (95-99%). It builds a multi-layer graph structure similar to a skip list, enabling fast traversal to find similar vectors.

### How It Works

```
Layer 2 (sparse):  o-----o       o-----o
                    \         /
Layer 1 (medium):    o---o---o---o---o
                      \     |     /
Layer 0 (dense):  o-o-o-o-o-o-o-o-o-o-o-o-o-o-o
```

1. **Layered Structure:** Top layers are sparse (long-range connections), bottom layer is dense (local connections)
2. **Search Process:** Start at top layer, greedily move to nearest neighbor, descend to next layer
3. **Insertion:** New elements added with probabilistic layer assignment

### Key Parameters

| Parameter | Purpose | Trade-off |
|-----------|---------|-----------|
| **M** (16) | Max connections per node | Higher = better recall, more memory |
| **efConstruction** (200) | Candidates considered during build | Higher = better index quality, slower build |
| **efSearch** (50) | Candidates considered during query | Higher = better recall, slower search |

### Java Libraries for HNSW

#### 1.1 JVector (Recommended for Java)

**Source:** [JVector GitHub](https://github.com/javadocio/javadoc) - Built on HNSW and DiskANN foundations

**Pros:**
- Pure Java implementation (no JNI/native dependencies)
- Compatible with Minecraft Forge modding
- Supports MemorySegmentReader (Java 20+) or SimpleMappedReader (Java 11+)
- Composable and extensible
- Good performance for medium-scale datasets (up to millions of vectors)

**Cons:**
- Newer library, smaller community
- Documentation may be limited

**Implementation Sketch:**
```java
import io.github.jvector.VectorDim;
import io.github.jvector.JVector;

// Initialize
JVector<float[]> vectorStore = new JVector<>(384); // dimension

// Add vectors
vectorStore.addVector(vectorId, embedding);

// Search
List<Neighbor> results = vectorStore.search(queryEmbedding, k);
```

#### 1.2 hnswlib (via JNI)

**Source:** [hnswlib GitHub](https://github.com/nmslib/hnswlib) - C++ with Python bindings

**Pros:**
- Industry-standard, battle-tested implementation
- Excellent performance
- Supports incremental updates and deletions
- Lower memory footprint

**Cons:**
- Requires JNI integration (complex in Minecraft Forge context)
- Native library deployment challenges (OS-specific binaries)
- Adds complexity to mod distribution

**Implementation Sketch:**
```java
import hnswlib.Hnswlib;

// Initialize (via JNI wrapper)
Hnswlib hnsw = new Hnswlib("cosine", 384);
hnsw.initMaxElements(1000);
hnsw.setEf(200); // efConstruction

// Add vectors
hnsw.addPoint(vector, vectorId);

// Search
int[] results = hnsw.knnQuery(query, k);
```

#### 1.3 Faiss Java Bindings

**Source:** [Faiss](https://github.com/facebookresearch/faiss) - Facebook Research

**Pros:**
- Widely used in production
- Comprehensive algorithm support (HNSW, IVF, PQ, etc.)
- GPU acceleration support

**Cons:**
- Large native library
- JNI complexity
- Overkill for small-medium datasets

**Implementation Sketch:**
```java
import com.facebook.faiss.IndexHNSWFlat;
import com.facebook.faiss.MetricType;

// Initialize
IndexHNSWFlat index = new IndexHNSWFlat(384, 16); // dim, M
index.setMetricType(MetricType.METRIC_INNER_PRODUCT);

// Add vectors
index.add(vectors);

// Search
float[][] distances = new float[1][k];
long[][] labels = new long[1][k];
index.search(1, query, k, distances, labels);
```

#### 1.4 Redis/Elasticsearch HNSW

**Source:** [Redis Vector Search](https://redis.ac.cn/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/), [Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html)

**Pros:**
- Mature, production-ready
- Easy integration if already using Redis/ES
- Good documentation

**Cons:**
- External service dependency (not suitable for embedded Minecraft mod)
- Network overhead
- Deployment complexity

---

## 2. PriorityQueue-Based Top-K Selection

### Overview

A simpler optimization that maintains O(n) complexity but reduces constant factors by using a bounded PriorityQueue instead of sorting all results.

### Algorithm

Instead of:
1. Compute similarity for all n vectors
2. Sort all n results: O(n log n)
3. Take top k

Do:
1. Maintain min-heap of size k
2. For each vector, if similarity > heap.min, replace
3. Final heap contains top-k: O(n log k)

### Java Implementation

```java
import java.util.PriorityQueue;

public List<VectorSearchResult<T>> search(float[] queryVector, int k) {
    // Min-heap to track top-k results
    PriorityQueue<VectorSearchResult<T>> topK = new PriorityQueue<>(
        k, Comparator.comparingDouble(VectorSearchResult::getSimilarity)
    );

    // Scan all vectors
    for (VectorEntry<T> entry : vectors.values()) {
        double similarity = cosineSimilarity(queryVector, entry.vector);

        if (topK.size() < k) {
            topK.offer(new VectorSearchResult<>(entry.data, similarity, entry.id));
        } else if (similarity > topK.peek().getSimilarity()) {
            topK.poll();
            topK.offer(new VectorSearchResult<>(entry.data, similarity, entry.id));
        }
    }

    // Convert to sorted list
    List<VectorSearchResult<T>> results = new ArrayList<>(topK);
    results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
    return results;
}
```

### Complexity Analysis

| Metric | Current (Full Sort) | PriorityQueue |
|--------|-------------------|---------------|
| Time | O(n + n log n) = O(n log n) | O(n log k) |
| Space | O(n) for results list | O(k) for heap |
| Best Case | k << n | k << n |

**Improvement:** When k=10 and n=200, log k ≈ 3.3 vs log n ≈ 7.6, ~2.3x faster sorting.

### Pros
- Simple implementation (no external dependencies)
- Immediate performance gain
- Reduced memory allocation
- Works with existing architecture

### Cons
- Still O(n) scan
- No early termination
- Doesn't scale beyond thousands of vectors

**Recommendation:** Implement this as a quick win before investing in HNSW.

---

## 3. Spatial Partitioning Approaches

### 3.1 IVF (Inverted File Index)

Partition vectors into clusters (Voronoi cells). Search only probes nearest clusters to query.

**Algorithm:**
1. Build: Run k-means clustering, assign each vector to a cluster
2. Search: Find nearest clusters to query, only scan those clusters

**Complexity:** O(n/c + c) where c = number of clusters probed

**Java Implementation Sketch:**
```java
class IVFVectorStore<T> {
    private Map<Integer, List<VectorEntry<T>>> clusters;
    private float[][] centroids; // k-means centers

    public List<VectorSearchResult<T>> search(float[] query, int k, int nProbes) {
        // Find nearest clusters
        int[] nearestClusters = findNearestCentroids(query, nProbes);

        // Search only those clusters
        PriorityQueue<VectorSearchResult<T>> topK = new PriorityQueue<>(k, ...);
        for (int clusterId : nearestClusters) {
            for (VectorEntry<T> entry : clusters.get(clusterId)) {
                // ... similarity computation and heap maintenance
            }
        }
        // ...
    }
}
```

### 3.2 Product Quantization (PQ)

Compress vectors by splitting into sub-vectors and quantizing each. Compute approximate similarity using lookup tables.

**Algorithm:**
1. Train: Split dimension into m subspaces, run k-means on each
2. Encode: Replace each sub-vector with cluster ID
3. Search: Use precomputed distance tables for fast lookup

**Complexity:** Memory-efficient, slightly slower query due to table lookups

### 3.3 Locality-Sensitive Hashing (LSH)

Hash similar vectors to same buckets with high probability.

**Algorithm:**
1. Build: Hash vectors using random projections
2. Search: Hash query, retrieve candidates from same buckets

**Complexity:** O(n/c + c) where c = number of collisions

**Java Libraries:**
- Apache Mahout LSH implementation
- Custom implementation (simple for cosine similarity)

**Implementation Sketch:**
```java
class LSHVectorStore<T> {
    private float[][] randomProjections; // Random hyperplanes
    private Map<Long, List<VectorEntry<T>>> buckets;

    public long hash(float[] vector) {
        long hash = 0;
        for (int i = 0; i < randomProjections.length; i++) {
            // Compute dot product with random projection
            double dot = 0;
            for (int j = 0; j < vector.length; j++) {
                dot += vector[j] * randomProjections[i][j];
            }
            // Set bit if positive
            if (dot > 0) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    public List<VectorSearchResult<T>> search(float[] query, int k) {
        long queryHash = hash(query);
        List<VectorEntry<T>> candidates = buckets.getOrDefault(queryHash, Collections.emptyList());

        // Compute exact similarity only on candidates
        return exactSearch(candidates, query, k);
    }
}
```

---

## 4. Trade-offs: Complexity vs Performance

### Comparison Table

| Approach | Build Time | Query Time | Memory | Accuracy | Complexity | Dependencies |
|----------|-----------|------------|--------|----------|------------|--------------|
| **Linear Scan (Current)** | O(n) | O(n) | O(n) | 100% | Low | None |
| **PriorityQueue** | O(n) | O(n log k) | O(n) | 100% | Low | None |
| **HNSW (JVector)** | O(n log n) | O(log n) | O(n × M) | 95-99% | Medium | JVector |
| **HNSW (hnswlib)** | O(n log n) | O(log n) | O(n × M) | 95-99% | High (JNI) | hnswlib |
| **IVF** | O(n × k-means) | O(n/c) | O(n) | 80-95% | Medium | None |
| **LSH** | O(n) | O(n/c) | O(n) | 70-90% | Medium | None |
| **PQ** | O(n × k-means) | O(n × m) | O(n/m) | 80-95% | High | None |

### When to Use Each

**< 100 vectors:** Linear scan is fine
**100-1000 vectors:** PriorityQueue optimization
**1000-10000 vectors:** HNSW (JVector) or IVF
**10000+ vectors:** HNSW with tuning or external vector DB

---

## 5. Implementation Recommendations

### Phase 1: Quick Win (PriorityQueue)

**Effort:** 1 hour
**Impact:** 2-3x faster search for k << n

```java
// Replace InMemoryVectorStore.search() with PriorityQueue implementation
// No API changes, backward compatible
```

**Benefits:**
- Zero dependencies
- Immediate improvement
- Low risk

### Phase 2: HNSW Integration (Recommended)

**Effort:** 1-2 days
**Impact:** O(log n) scaling to 100K+ vectors

**Recommended Library:** JVector (pure Java)

**Implementation Steps:**

1. Add dependency to `build.gradle`:
```gradle
dependencies {
    implementation 'io.github.jvector:jvector:2.0.0'
}
```

2. Create adapter interface:
```java
public interface VectorStore<T> {
    int add(float[] vector, T data);
    List<VectorSearchResult<T>> search(float[] query, int k);
    boolean remove(int id);
    int size();
}
```

3. Implement HNSW variant:
```java
public class HNSWVectorStore<T> implements VectorStore<T> {
    private final JVector<float[]> index;
    private final Map<Integer, T> dataMap;

    public HNSWVectorStore(int dimension) {
        this.index = new JVector<>(dimension);
        this.index.setM(16);  // max connections
        this.index.setEf(200); // construction quality
        this.dataMap = new ConcurrentHashMap<>();
    }

    @Override
    public int add(float[] vector, T data) {
        int id = index.nextId();
        index.addVector(vector, id);
        dataMap.put(id, data);
        return id;
    }

    @Override
    public List<VectorSearchResult<T>> search(float[] query, int k) {
        index.setEfSearch(Math.max(k, 50)); // dynamic ef
        List<Neighbor> neighbors = index.search(query, k);
        return neighbors.stream()
            .map(n -> new VectorSearchResult<>(
                dataMap.get(n.id()), n.distance(), n.id()
            ))
            .collect(Collectors.toList());
    }
}
```

4. Update `CompanionMemory`:
```java
// Change constructor
private final VectorStore<EpisodicMemory> memoryVectorStore;

public CompanionMemory() {
    // Use HNSW for production, fallback to linear for testing
    this.memoryVectorStore = new HNSWVectorStore<>(384);
    // this.memoryVectorStore = new InMemoryVectorStore<>(384);
}
```

### Phase 3: Hybrid Approach (Future)

Combine indexing with smart caching:
- Cache recent queries and results
- Use HNSW for cache misses
- Incremental index updates (no rebuild required)

```java
public class CachedVectorStore<T> implements VectorStore<T> {
    private final VectorStore<T> delegate;
    private final Cache<QueryKey, List<VectorSearchResult<T>>> cache;

    public List<VectorSearchResult<T>> search(float[] query, int k) {
        QueryKey key = new QueryKey(query, k);
        return cache.get(key, () -> delegate.search(query, k));
    }
}
```

---

## 6. Performance Benchmarks (Expected)

### Query Time (ms) by Dataset Size

| Vectors | Linear Scan | PriorityQueue | HNSW (ef=50) |
|---------|-------------|---------------|--------------|
| 100 | 0.5 | 0.3 | 0.2 |
| 500 | 2.5 | 1.5 | 0.3 |
| 1000 | 5.0 | 3.0 | 0.4 |
| 5000 | 25.0 | 15.0 | 0.6 |
| 10000 | 50.0 | 30.0 | 0.8 |
| 50000 | 250.0 | 150.0 | 2.0 |
| 100000 | 500.0 | 300.0 | 3.5 |

*Note: Benchmarks assume 384-dim float vectors, cosine similarity, k=10*

### Memory Usage

| Approach | Per Vector Overhead |
|----------|---------------------|
| Linear Scan | 0 bytes |
| PriorityQueue | 0 bytes (during search only) |
| HNSW (M=16) | ~16 pointers ≈ 128 bytes |

---

## 7. Migration Strategy

### Step 1: Add PriorityQueue Optimization
- Modify `InMemoryVectorStore.search()`
- Test with existing unit tests
- Verify performance improvement

### Step 2: Introduce VectorStore Interface
- Extract interface from current implementation
- Update `CompanionMemory` to use interface
- Add factory for selecting implementation

### Step 3: Implement HNSWVectorStore
- Add JVector dependency
- Implement interface
- Add unit tests
- Compare accuracy vs linear scan

### Step 4: Gradual Rollout
- Add configuration option: `vectorStore.type = linear|hnsw`
- Start with linear (safe default)
- Allow users to opt-in to HNSW
- Monitor and tune parameters

### Step 5: Persistence
- Implement HNSW index save/load to NBT
- Convert existing linear stores on first load
- Handle version migration

---

## 8. Configuration Options

Add to `config/steve-common.toml`:

```toml
[vectorStore]
# Type: linear, priorityQueue, hnsw
type = "hnsw"

# HNSW parameters
[vectorStore.hnsw]
M = 16              # Max connections per node (4-64)
efConstruction = 200 # Build-time quality (50-500)
efSearch = 50       # Query-time quality (k to 500)

# Caching
[vectorStore.cache]
enabled = true
maxSize = 100
expireAfterAccess = "5m"
```

---

## 9. Testing Strategy

### Unit Tests

```java
class VectorStoreTest {
    @Test
    void testSearchAccuracy() {
        // Compare HNSW vs linear scan results
        // Assert top-k overlap > 95%
    }

    @Test
    void testAddRemove() {
        // Verify add/remove consistency
    }

    @Test
    void testPersistence() {
        // Save to NBT, load, verify equality
    }
}
```

### Performance Tests

```java
class VectorStoreBenchmark {
    @Benchmark
    void linearSearch() { /* ... */ }

    @Benchmark
    void priorityQueueSearch() { /* ... */ }

    @Benchmark
    void hnswSearch() { /* ... */ }
}
```

### Integration Tests

- Load world with 10K memories
- Measure query latency
- Verify memory footprint
- Test concurrent access (multiple Steves)

---

## 10. Conclusion and Next Steps

### Summary

1. **Immediate (PriorityQueue):** Simple optimization, no dependencies, 2-3x improvement
2. **Short-term (HNSW via JVector):** Best balance of performance and complexity, O(log n) scaling
3. **Long-term (Hybrid):** Add caching, smart updates, external vector DB for very large datasets

### Recommended Path Forward

```
Current State (Linear Scan)
    ↓ [1 hour]
PriorityQueue Optimization
    ↓ [1-2 days]
HNSW Integration (JVector)
    ↓ [Optional]
Caching + Tuning
```

### Key Takeaways

- For MineWright's current scale (200 memories), PriorityQueue is sufficient
- HNSW future-proofs for larger datasets (1000+ memories)
- JVector is the best HNSW choice for Java/Minecraft Forge
- Avoid JNI dependencies (hnswlib, Faiss) due to mod distribution complexity
- Maintain fallback to linear scan for compatibility

---

## References

### Sources

- [Spring AI 2.0.0-M1: HNSW Index Parameters](https://juejin.cn/post/7588355695101886473) - HNSW parameter configuration (M, efConstruction, efRuntime)
- [Vector Database Index Selection: IVF_FLAT, HNSW, ANNOY (Java)](https://m.blog.csdn.net/canjun_wen/article/details/155609275) - Java implementation guide with Faiss bindings
- [NVIDIA GPU-Accelerated Vector Search in Elasticsearch](https://juejin.cn/post/7483436318777917466) - cuVS Java API, HNSW support
- [Faiss, Annoy, HNSW Performance Comparison](http://www.hooos.com/data-9470) - Benchmark comparison and code examples
- [HNSW Algorithm Introduction](https://blog.csdn.net/weixin_46351593/article/details/146318977) - Parameter explanations (L, M_max, efConstruction)
- [Billion-Scale Disk-based Nearest-Neighbor Index](https://arxiv.org/abs/2511.15557) - HNSW limitations and disk-based alternatives
- [ANN vs KNN Overview](https://news.sina.cn/j_uc.d.html?docid=iznctke652918) - ANN advantages and use cases
- [JVector README](https://www.cnblogs.com/kukix/p/18946999) - Java-native HNSW/DiskANN implementation
- [hnswlib GitHub](https://github.com/nmslib/hnswlib) - C++ HNSW implementation with Python bindings
- [Redis Vector Search Documentation](https://redis.ac.cn/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/) - Redis HNSW implementation
- [Top-K Problem Solutions](https://juejin.cn/post/6981729016084430856) - PriorityQueue algorithm explanation
- [Lucene Top-K PriorityQueue](https://m.blog.csdn.net/duck_genuine/article/details/7596444) - Apache Lucene implementation
- [VecFlow: High-Performance Vector Management](https://arxiv.org/html/2506.00812v1) - Vector data management research

### Further Reading

- Malkov, Y. A., & Yashunin, D. A. (2018). "Efficient and robust approximate nearest neighbor search using Hierarchical Navigable Small World graphs." arXiv preprint.
- Aumüller, M., Bernhardsson, E., & Faithfull, A. (2020). "ANN-Benchmarks: A benchmarking system for approximate nearest neighbor algorithms." IEEE Transactions on Big Data.

---

**Document Version:** 1.0
**Last Updated:** 2025-02-27
**Author:** Research based on 2025 state of vector search technology
**Project:** MineWright Minecraft Mod - Steve AI
