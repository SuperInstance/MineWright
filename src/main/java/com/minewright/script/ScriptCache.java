package com.minewright.script;

import com.minewright.llm.cache.EmbeddingVector;
import com.minewright.llm.cache.SimpleTextEmbedder;
import com.minewright.llm.cache.TextEmbedder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Semantic cache for generated automation scripts.
 *
 * <p>This cache provides intelligent script retrieval based on semantic similarity
 * between natural language commands, enabling reuse of previously generated scripts
 * for similar tasks. Key features:</p>
 *
 * <ul>
 *   <li><b>Semantic Search:</b> Find similar scripts using TF-IDF embeddings</li>
 *   <li><b>LRU Eviction:</b> Automatically remove least recently used scripts</li>
 *   <li><b>Success Tracking:</b> Monitor and prioritize high-performing scripts</li>
 *   <li><b>Thread Safety:</b> Concurrent access support for multi-agent environments</li>
 * </ul>
 *
 * <p><b>Cache Strategy:</b></p>
 * <pre>
 * 1. When a command is received, generate its embedding
 * 2. Search for semantically similar cached scripts (above threshold)
 * 3. If found, return the cached script (cache hit)
 * 4. If not found, generate new script via LLM and cache it
 * 5. Track execution success/failure for each script
 * 6. Periodically cleanup low-performing or stale entries
 * </pre>
 *
 * <p><b>Thread Safety:</b> This class uses ConcurrentHashMap and ReadWriteLock
 * for thread-safe operations.</p>
 *
 * @since 1.6.0
 */
public class ScriptCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptCache.class);

    // Default configuration values
    private static final int DEFAULT_MAX_SIZE = 100;
    private static final double DEFAULT_MIN_SIMILARITY = 0.75;
    private static final long DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000; // 24 hours
    private static final int DEFAULT_MIN_EXECUTIONS = 3;
    private static final double DEFAULT_MIN_SUCCESS_RATE = 0.4;

    // Cache storage: script ID -> cached script entry
    private final ConcurrentHashMap<String, CachedScript> scriptsById;

    // Semantic index: command text -> script IDs with similarity scores
    private final Map<String, List<ScriptSimilarity>> semanticIndex;

    // Lock for protecting semantic index updates
    private final ReadWriteLock indexLock;

    // Text embedder for semantic similarity
    private final TextEmbedder embedder;

    // Cache configuration
    private final int maxSize;
    private final double minSimilarity;
    private final long maxAgeMs;
    private final int minExecutions;
    private final double minSuccessRate;

    // Cache statistics
    private final AtomicInteger totalHits;
    private final AtomicInteger totalMisses;
    private final AtomicInteger totalEvictions;
    private final AtomicBoolean cleanupInProgress;

    /**
     * Creates a new script cache with default configuration.
     */
    public ScriptCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_MIN_SIMILARITY, DEFAULT_MAX_AGE_MS,
             DEFAULT_MIN_EXECUTIONS, DEFAULT_MIN_SUCCESS_RATE);
    }

    /**
     * Creates a new script cache with custom configuration.
     *
     * @param maxSize Maximum number of scripts to cache
     * @param minSimilarity Minimum similarity threshold (0.0 to 1.0)
     * @param maxAgeMs Maximum age for cached entries (milliseconds)
     * @param minExecutions Minimum executions before success rate matters
     * @param minSuccessRate Minimum success rate to retain script
     */
    public ScriptCache(int maxSize, double minSimilarity, long maxAgeMs,
                       int minExecutions, double minSuccessRate) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            throw new IllegalArgumentException("minSimilarity must be in [0, 1]");
        }
        if (maxAgeMs <= 0) {
            throw new IllegalArgumentException("maxAgeMs must be positive");
        }
        if (minExecutions < 0) {
            throw new IllegalArgumentException("minExecutions must be non-negative");
        }
        if (minSuccessRate < 0.0 || minSuccessRate > 1.0) {
            throw new IllegalArgumentException("minSuccessRate must be in [0, 1]");
        }

        this.scriptsById = new ConcurrentHashMap<>();
        this.semanticIndex = new HashMap<>();
        this.indexLock = new ReentrantReadWriteLock();
        this.embedder = new SimpleTextEmbedder();
        this.maxSize = maxSize;
        this.minSimilarity = minSimilarity;
        this.maxAgeMs = maxAgeMs;
        this.minExecutions = minExecutions;
        this.minSuccessRate = minSuccessRate;
        this.totalHits = new AtomicInteger(0);
        this.totalMisses = new AtomicInteger(0);
        this.totalEvictions = new AtomicInteger(0);
        this.cleanupInProgress = new AtomicBoolean(false);

        LOGGER.info("ScriptCache initialized: maxSize={}, minSimilarity={}, maxAge={}ms",
                    maxSize, minSimilarity, maxAgeMs);
    }

    /**
     * Finds a cached script similar to the given command.
     *
     * <p>Searches for scripts with semantic similarity above the threshold.
     * Returns the highest-scoring script if found, empty otherwise.</p>
     *
     * @param command The natural language command
     * @param minSimilarity Minimum similarity threshold (overrides default)
     * @return Optional script if similar script found
     */
    public Optional<Script> findSimilar(String command, double minSimilarity) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            throw new IllegalArgumentException("minSimilarity must be in [0, 1]");
        }

        // Generate embedding for the command
        EmbeddingVector commandEmbedding;
        try {
            commandEmbedding = embedder.embed(command);
        } catch (Exception e) {
            LOGGER.error("Failed to embed command: {}", command, e);
            return Optional.empty();
        }

        // Search for similar scripts
        CachedScript bestMatch = null;
        double bestScore = minSimilarity;

        indexLock.readLock().lock();
        try {
            for (CachedScript cached : scriptsById.values()) {
                // Skip scripts below success threshold
                if (!cached.isPerformant(minExecutions, minSuccessRate)) {
                    continue;
                }

                // Skip stale scripts
                if (cached.isStale(maxAgeMs)) {
                    continue;
                }

                // Calculate similarity
                double similarity = commandEmbedding.cosineSimilarity(cached.getEmbedding());
                if (similarity > bestScore) {
                    bestMatch = cached;
                    bestScore = similarity;
                }
            }
        } finally {
            indexLock.readLock().unlock();
        }

        if (bestMatch != null) {
            bestMatch.recordAccess();
            totalHits.incrementAndGet();
            LOGGER.debug("Cache hit for command '{}': similarity={}",
                        truncate(command, 30), String.format("%.3f", bestScore));
            return Optional.of(bestMatch.getScript());
        }

        totalMisses.incrementAndGet();
        LOGGER.debug("Cache miss for command '{}'", truncate(command, 30));
        return Optional.empty();
    }

    /**
     * Finds a cached script similar to the given command using default threshold.
     *
     * @param command The natural language command
     * @return Optional script if similar script found
     */
    public Optional<Script> findSimilar(String command) {
        return findSimilar(command, this.minSimilarity);
    }

    /**
     * Stores a script in the cache with semantic indexing.
     *
     * <p>The script is indexed by its command text for future similarity searches.
     * If the cache is full, performs LRU eviction.</p>
     *
     * @param command The natural language command
     * @param script The script to cache
     */
    public void store(String command, Script script) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        if (script == null) {
            throw new IllegalArgumentException("Script cannot be null");
        }

        String scriptId = script.getId();
        if (scriptId == null) {
            LOGGER.warn("Cannot cache script with null ID");
            return;
        }

        // Check if script already exists
        if (scriptsById.containsKey(scriptId)) {
            LOGGER.debug("Script {} already cached, skipping", scriptId);
            return;
        }

        // Evict if necessary
        if (scriptsById.size() >= maxSize) {
            evictLRU();
        }

        // Generate embedding for the command
        EmbeddingVector embedding;
        try {
            embedding = embedder.embed(command);
        } catch (Exception e) {
            LOGGER.error("Failed to embed command for storage: {}", command, e);
            return;
        }

        // Create cached entry
        CachedScript cached = new CachedScript(script, command, embedding);

        // Store in cache
        scriptsById.put(scriptId, cached);

        // Update semantic index
        indexLock.writeLock().lock();
        try {
            semanticIndex.computeIfAbsent(command, k -> new ArrayList<>())
                         .add(new ScriptSimilarity(scriptId, 1.0));
        } finally {
            indexLock.writeLock().unlock();
        }

        LOGGER.debug("Cached script {} for command '{}': cacheSize={}",
                    scriptId, truncate(command, 30), scriptsById.size());

        // Update embedder statistics
        if (embedder instanceof SimpleTextEmbedder) {
            ((SimpleTextEmbedder) embedder).updateDocumentStatistics(command);
        }
    }

    /**
     * Records a successful execution for a script.
     *
     * @param scriptId The script ID
     */
    public void recordSuccess(String scriptId) {
        CachedScript cached = scriptsById.get(scriptId);
        if (cached != null) {
            cached.recordSuccess();
            LOGGER.debug("Recorded success for script {}", scriptId);
        } else {
            LOGGER.warn("Cannot record success for unknown script {}", scriptId);
        }
    }

    /**
     * Records a failed execution for a script.
     *
     * @param scriptId The script ID
     */
    public void recordFailure(String scriptId) {
        CachedScript cached = scriptsById.get(scriptId);
        if (cached != null) {
            cached.recordFailure();
            LOGGER.debug("Recorded failure for script {}", scriptId);
        } else {
            LOGGER.warn("Cannot record failure for unknown script {}", scriptId);
        }
    }

    /**
     * Removes low-performing or stale scripts from the cache.
     *
     * <p>Cleanup criteria:</p>
 * <ul>
     *   <li>Scripts older than maxAgeMs</li>
     *   <li>Scripts with success rate below minSuccessRate (after minExecutions)</li>
     *   <li>Scripts that haven't been accessed recently</li>
     * </ul>
     *
     * @return Number of scripts removed
     */
    public int cleanup() {
        // Prevent concurrent cleanups
        if (!cleanupInProgress.compareAndSet(false, true)) {
            LOGGER.debug("Cleanup already in progress, skipping");
            return 0;
        }

        try {
            LOGGER.info("Starting script cache cleanup: size={}", scriptsById.size());

            int removedCount = 0;
            List<String> toRemove = new ArrayList<>();

            for (CachedScript cached : scriptsById.values()) {
                if (cached.isStale(maxAgeMs)) {
                    toRemove.add(cached.getScript().getId());
                    LOGGER.debug("Marking stale script {} for removal", cached.getScript().getId());
                } else if (!cached.isPerformant(minExecutions, minSuccessRate)) {
                    toRemove.add(cached.getScript().getId());
                    LOGGER.debug("Marking low-performing script {} for removal: successRate={}",
                                cached.getScript().getId(),
                                String.format("%.2f", cached.getSuccessRate()));
                }
            }

            // Remove marked scripts
            for (String scriptId : toRemove) {
                if (removeInternal(scriptId)) {
                    removedCount++;
                }
            }

            LOGGER.info("Cache cleanup complete: removed {} scripts, size={}",
                       removedCount, scriptsById.size());

            return removedCount;
        } finally {
            cleanupInProgress.set(false);
        }
    }

    /**
     * Removes a script from the cache.
     *
     * @param scriptId The script ID to remove
     * @return true if the script was removed, false if not found
     */
    public boolean remove(String scriptId) {
        if (scriptId == null) {
            return false;
        }
        boolean removed = removeInternal(scriptId);
        if (removed) {
            LOGGER.debug("Removed script {} from cache", scriptId);
        }
        return removed;
    }

    /**
     * Clears all scripts from the cache.
     */
    public void clear() {
        int size = scriptsById.size();
        scriptsById.clear();

        indexLock.writeLock().lock();
        try {
            semanticIndex.clear();
        } finally {
            indexLock.writeLock().unlock();
        }

        LOGGER.info("Cleared script cache: removed {} scripts", size);
    }

    /**
     * Gets cache statistics.
     *
     * @return Cache statistics snapshot
     */
    public CacheStats getStats() {
        int totalExecutions = scriptsById.values().stream()
                                       .mapToInt(CachedScript::getExecutionCount)
                                       .sum();

        int totalSuccesses = scriptsById.values().stream()
                                       .mapToInt(CachedScript::getSuccessCount)
                                       .sum();

        double overallSuccessRate = totalExecutions > 0
            ? (double) totalSuccesses / totalExecutions
            : 0.0;

        return new CacheStats(
            scriptsById.size(),
            maxSize,
            totalHits.get(),
            totalMisses.get(),
            totalEvictions.get(),
            totalExecutions,
            totalSuccesses,
            overallSuccessRate,
            getHitRate()
        );
    }

    /**
     * Gets the current cache hit rate.
     *
     * @return Hit rate (0.0 to 1.0)
     */
    public double getHitRate() {
        int total = totalHits.get() + totalMisses.get();
        return total > 0 ? (double) totalHits.get() / total : 0.0;
    }

    /**
     * Gets the number of scripts currently cached.
     *
     * @return Cache size
     */
    public int size() {
        return scriptsById.size();
    }

    /**
     * Checks if the cache is empty.
     *
     * @return true if cache contains no scripts
     */
    public boolean isEmpty() {
        return scriptsById.isEmpty();
    }

    /**
     * Gets the maximum cache size.
     *
     * @return Maximum number of scripts
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Gets all cached scripts.
     *
     * @return Collection of cached scripts
     */
    public Collection<Script> getAllScripts() {
        return scriptsById.values().stream()
                         .map(CachedScript::getScript)
                         .collect(Collectors.toList());
    }

    /**
     * Evicts the least recently used script from the cache.
     */
    private void evictLRU() {
        CachedScript lru = scriptsById.values().stream()
                                       .min(Comparator.comparingLong(CachedScript::getLastAccessed))
                                       .orElse(null);

        if (lru != null) {
            String scriptId = lru.getScript().getId();
            if (removeInternal(scriptId)) {
                totalEvictions.incrementAndGet();
                LOGGER.debug("Evicted LRU script {}", scriptId);
            }
        }
    }

    /**
     * Internal removal method that updates both storage and index.
     */
    private boolean removeInternal(String scriptId) {
        CachedScript removed = scriptsById.remove(scriptId);
        if (removed != null) {
            // Update semantic index
            indexLock.writeLock().lock();
            try {
                String command = removed.getCommand();
                List<ScriptSimilarity> entries = semanticIndex.get(command);
                if (entries != null) {
                    entries.removeIf(entry -> entry.scriptId.equals(scriptId));
                    if (entries.isEmpty()) {
                        semanticIndex.remove(command);
                    }
                }
            } finally {
                indexLock.writeLock().unlock();
            }
            return true;
        }
        return false;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Cached script entry with performance tracking.
     */
    private static class CachedScript {
        private final Script script;
        private final String command;
        private final EmbeddingVector embedding;
        private final long createdAt;
        private volatile long lastAccessed;
        private final AtomicInteger executionCount;
        private final AtomicInteger successCount;

        CachedScript(Script script, String command, EmbeddingVector embedding) {
            this.script = script;
            this.command = command;
            this.embedding = embedding;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessed = createdAt;
            this.executionCount = new AtomicInteger(0);
            this.successCount = new AtomicInteger(0);
        }

        Script getScript() {
            return script;
        }

        String getCommand() {
            return command;
        }

        EmbeddingVector getEmbedding() {
            return embedding;
        }

        long getLastAccessed() {
            return lastAccessed;
        }

        void recordAccess() {
            lastAccessed = System.currentTimeMillis();
        }

        void recordSuccess() {
            executionCount.incrementAndGet();
            successCount.incrementAndGet();
        }

        void recordFailure() {
            executionCount.incrementAndGet();
        }

        int getExecutionCount() {
            return executionCount.get();
        }

        int getSuccessCount() {
            return successCount.get();
        }

        double getSuccessRate() {
            int executions = executionCount.get();
            return executions > 0 ? (double) successCount.get() / executions : 0.0;
        }

        boolean isStale(long maxAge) {
            return (System.currentTimeMillis() - createdAt) > maxAge;
        }

        boolean isPerformant(int minExecutions, double minSuccessRate) {
            int executions = executionCount.get();
            if (executions < minExecutions) {
                return true; // Not enough data to judge
            }
            return getSuccessRate() >= minSuccessRate;
        }
    }

    /**
     * Script similarity record for semantic index.
     */
    private static class ScriptSimilarity {
        private final String scriptId;
        private final double similarity;

        ScriptSimilarity(String scriptId, double similarity) {
            this.scriptId = scriptId;
            this.similarity = similarity;
        }
    }

    /**
     * Cache statistics snapshot.
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final int totalHits;
        private final int totalMisses;
        private final int totalEvictions;
        private final int totalExecutions;
        private final int totalSuccesses;
        private final double overallSuccessRate;
        private final double hitRate;

        CacheStats(int currentSize, int maxSize, int totalHits, int totalMisses,
                   int totalEvictions, int totalExecutions, int totalSuccesses,
                   double overallSuccessRate, double hitRate) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.totalEvictions = totalEvictions;
            this.totalExecutions = totalExecutions;
            this.totalSuccesses = totalSuccesses;
            this.overallSuccessRate = overallSuccessRate;
            this.hitRate = hitRate;
        }

        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public int getTotalHits() { return totalHits; }
        public int getTotalMisses() { return totalMisses; }
        public int getTotalEvictions() { return totalEvictions; }
        public int getTotalExecutions() { return totalExecutions; }
        public int getTotalSuccesses() { return totalSuccesses; }
        public double getOverallSuccessRate() { return overallSuccessRate; }
        public double getHitRate() { return hitRate; }

        @Override
        public String toString() {
            return String.format(
                "CacheStats{size=%d/%d, hits=%d, misses=%d, evictions=%d, " +
                "executions=%d, successes=%d, successRate=%.2f, hitRate=%.2f}",
                currentSize, maxSize, totalHits, totalMisses, totalEvictions,
                totalExecutions, totalSuccesses, overallSuccessRate, hitRate
            );
        }
    }
}
