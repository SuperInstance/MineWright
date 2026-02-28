package com.minewright.llm.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Semantic LLM cache using text similarity for intelligent cache hits.
 *
 * <p>This cache extends traditional exact-match caching by finding semantically
 * similar prompts. When a new prompt is received:</p>
 *
 * <ol>
 *   <li>Generate an embedding vector for the prompt</li>
 *   <li>Find cached entries above the similarity threshold</li>
 *   <li>Return the highest similarity match if found</li>
 *   <li>Otherwise, cache miss - prompt the LLM</li>
 * </ol>
 *
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>Higher cache hit rates for similar but non-identical prompts</li>
 *   <li>Reduced API costs for common query patterns</li>
 *   <li>Faster response times for cached similar queries</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Uses ReadWriteLock for efficient concurrent access.
 * Multiple readers can access simultaneously, writers have exclusive access.</p>
 *
 * @since 1.6.0
 */
public class SemanticLLMCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticLLMCache.class);

    private final List<SemanticCacheEntry> entries;
    private final TextEmbedder embedder;
    private final double similarityThreshold;
    private final int maxCacheSize;
    private final long maxAgeMs;

    // Statistics
    private final AtomicLong exactHitCount = new AtomicLong(0);
    private final AtomicLong semanticHitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final AtomicLong totalSimilarityScore = new AtomicLong(0); // Scaled by 1000

    // Lock for thread-safe operations
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new semantic LLM cache with default settings.
     */
    public SemanticLLMCache() {
        this(new SimpleTextEmbedder(), 0.85, 500, 5 * 60 * 1000);
    }

    /**
     * Creates a new semantic LLM cache with custom settings.
     *
     * @param embedder            The text embedder to use
     * @param similarityThreshold Minimum similarity for cache hit [0.0, 1.0]
     * @param maxCacheSize        Maximum number of entries
     * @param maxAgeMs            Maximum age for entries before expiration
     */
    public SemanticLLMCache(TextEmbedder embedder, double similarityThreshold,
                            int maxCacheSize, long maxAgeMs) {
        this.embedder = embedder;
        this.similarityThreshold = similarityThreshold;
        this.maxCacheSize = maxCacheSize;
        this.maxAgeMs = maxAgeMs;
        this.entries = new CopyOnWriteArrayList<>();

        LOGGER.info("Initialized SemanticLLMCache: embedder={}, threshold={}, max={}, ttl={}min",
            embedder.getName(), similarityThreshold, maxCacheSize, maxAgeMs / 60000);
    }

    /**
     * Retrieves a cached response if a similar prompt exists.
     *
     * <p>Search strategy:</p>
     * <ol>
     *   <li>Check for exact prompt match (fast path)</li>
     *   <li>If no exact match, generate embedding for query</li>
     *   <li>Find all entries above similarity threshold</li>
     *   <li>Return highest similarity match if found</li>
     * </ol>
     *
     * @param prompt     The query prompt
     * @param model      The LLM model
     * @param providerId The LLM provider ID
     * @return Optional containing the cached response, or empty if not found
     */
    public Optional<String> get(String prompt, String model, String providerId) {
        lock.readLock().lock();
        try {
            // Fast path: exact match check
            Optional<SemanticCacheEntry> exactMatch = findExactMatch(prompt, model, providerId);
            if (exactMatch.isPresent()) {
                exactHitCount.incrementAndGet();
                SemanticCacheEntry entry = exactMatch.get();
                entry.incrementHitCount();
                LOGGER.debug("Exact cache hit for prompt='{}', similarity=1.0",
                    truncate(prompt, 40));
                return Optional.of(entry.getResponse());
            }

            // Semantic search
            EmbeddingVector queryEmbedding = embedder.embed(prompt);

            SemanticCacheEntry bestMatch = null;
            double bestSimilarity = 0.0;

            for (SemanticCacheEntry entry : entries) {
                // Skip entries for different models/providers
                if (!entry.getModel().equals(model) || !entry.getProviderId().equals(providerId)) {
                    continue;
                }

                // Skip expired entries
                if (entry.isOlderThan(maxAgeMs)) {
                    continue;
                }

                double similarity = queryEmbedding.cosineSimilarity(entry.getEmbedding());

                if (similarity >= similarityThreshold && similarity > bestSimilarity) {
                    bestMatch = entry;
                    bestSimilarity = similarity;
                }
            }

            if (bestMatch != null) {
                semanticHitCount.incrementAndGet();
                totalSimilarityScore.addAndGet((long) (bestSimilarity * 1000));
                bestMatch.incrementHitCount();
                LOGGER.debug("Semantic cache hit for prompt='{}', similarity={:.4f}",
                    truncate(prompt, 40), bestSimilarity);
                return Optional.of(bestMatch.getResponse());
            }

            missCount.incrementAndGet();
            LOGGER.debug("Cache miss for prompt='{}'", truncate(prompt, 40));
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Stores a response in the cache.
     *
     * <p>Automatically evicts old entries if at capacity.</p>
     *
     * @param prompt     The prompt
     * @param model      The LLM model
     * @param providerId The LLM provider ID
     * @param response   The response to cache
     */
    public void put(String prompt, String model, String providerId, String response) {
        put(prompt, model, providerId, response, 0);
    }

    /**
     * Stores a response in the cache with token count.
     *
     * @param prompt     The prompt
     * @param model      The LLM model
     * @param providerId The LLM provider ID
     * @param response   The response to cache
     * @param tokensUsed Number of tokens used
     */
    public void put(String prompt, String model, String providerId,
                    String response, int tokensUsed) {
        lock.writeLock().lock();
        try {
            // Check if already exists (update instead of add)
            Optional<SemanticCacheEntry> existing = findExactMatch(prompt, model, providerId);
            if (existing.isPresent()) {
                // Entry exists - just update hit count (response should be the same)
                existing.get().incrementHitCount();
                LOGGER.debug("Updated existing cache entry for prompt='{}'",
                    truncate(prompt, 40));
                return;
            }

            // Evict if at capacity
            while (entries.size() >= maxCacheSize) {
                evictLeastUsed();
            }

            // Generate embedding and create entry
            EmbeddingVector embedding = embedder.embed(prompt);

            // Update embedder statistics
            if (embedder instanceof SimpleTextEmbedder simpleEmbedder) {
                simpleEmbedder.updateDocumentStatistics(prompt);
            }

            SemanticCacheEntry entry = new SemanticCacheEntry(
                prompt, model, providerId, embedding, response, tokensUsed, null
            );

            entries.add(entry);
            LOGGER.debug("Cached response for prompt='{}', tokens={}, size={}/{}",
                truncate(prompt, 40), tokensUsed, entries.size(), maxCacheSize);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Finds an exact match for the prompt.
     */
    private Optional<SemanticCacheEntry> findExactMatch(String prompt, String model, String providerId) {
        return entries.stream()
            .filter(e -> e.getPrompt().equals(prompt))
            .filter(e -> e.getModel().equals(model))
            .filter(e -> e.getProviderId().equals(providerId))
            .filter(e -> !e.isOlderThan(maxAgeMs))
            .findFirst();
    }

    /**
     * Evicts entries older than the specified age.
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return Number of entries evicted
     */
    public int evictOlderThan(long maxAgeMs) {
        lock.writeLock().lock();
        try {
            int beforeSize = entries.size();
            entries.removeIf(entry -> entry.isOlderThan(maxAgeMs));
            int evicted = beforeSize - entries.size();
            evictionCount.addAndGet(evicted);
            if (evicted > 0) {
                LOGGER.info("Evicted {} entries older than {}ms", evicted, maxAgeMs);
            }
            return evicted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Evicts the least recently used entries to maintain cache size.
     */
    private void evictLeastUsed() {
        if (entries.isEmpty()) return;

        // Find entry with worst eviction score
        SemanticCacheEntry worst = entries.stream()
            .min((a, b) -> Double.compare(a.getEvictionScore(), b.getEvictionScore()))
            .orElse(null);

        if (worst != null) {
            entries.remove(worst);
            evictionCount.incrementAndGet();
            LOGGER.debug("Evicted entry: score={:.4f}, hits={}, age={}ms",
                worst.getEvictionScore(), worst.getHitCount(), worst.getAge());
        }
    }

    /**
     * Evicts entries to reach the target size.
     *
     * @param targetSize Target cache size
     * @return Number of entries evicted
     */
    public int evictLeastUsed(int targetSize) {
        lock.writeLock().lock();
        try {
            int evicted = 0;
            while (entries.size() > targetSize && !entries.isEmpty()) {
                evictLeastUsed();
                evicted++;
            }
            return evicted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            int size = entries.size();
            entries.clear();
            LOGGER.info("Cleared {} cache entries", size);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns cache statistics.
     *
     * @return Cache statistics snapshot
     */
    public CacheStats getStats() {
        long exactHits = exactHitCount.get();
        long semanticHits = semanticHitCount.get();
        long misses = missCount.get();
        long total = exactHits + semanticHits + misses;

        double hitRate = total > 0 ? (double) (exactHits + semanticHits) / total : 0.0;
        double exactHitRate = total > 0 ? (double) exactHits / total : 0.0;
        double semanticHitRate = total > 0 ? (double) semanticHits / total : 0.0;

        long totalSimScore = totalSimilarityScore.get();
        double avgSimilarity = semanticHits > 0
            ? (totalSimScore / 1000.0) / semanticHits
            : 0.0;

        return new CacheStats(
            entries.size(),
            hitRate,
            exactHitRate,
            semanticHitRate,
            avgSimilarity,
            exactHits,
            semanticHits,
            misses,
            evictionCount.get()
        );
    }

    /**
     * Returns the current number of entries.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns the similarity threshold.
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Returns the text embedder.
     */
    public TextEmbedder getEmbedder() {
        return embedder;
    }

    /**
     * Logs current cache statistics.
     */
    public void logStats() {
        CacheStats stats = getStats();
        LOGGER.info(
            "SemanticLLMCache Stats - Size: {}/{}, Hit Rate: {:.2f}%, " +
            "Exact: {:.2f}%, Semantic: {:.2f}%, Avg Sim: {:.4f}, Evictions: {}",
            stats.size, maxCacheSize,
            stats.hitRate * 100,
            stats.exactHitRate * 100,
            stats.semanticHitRate * 100,
            stats.averageSimilarity,
            stats.evictions
        );
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Returns all entries (for debugging/monitoring).
     */
    public List<SemanticCacheEntry> getEntries() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(entries);
        } finally {
            lock.readLock().unlock();
        }
    }
}
