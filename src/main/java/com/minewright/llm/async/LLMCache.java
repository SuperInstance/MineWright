package com.minewright.llm.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple LRU cache for LLM responses using Java built-in collections.
 *
 * <p>Replaces Caffeine to avoid Forge modular classloading issues.
 * Uses ConcurrentHashMap with TTL-based expiration.</p>
 *
 * <p><b>Cache Configuration:</b></p>
 * <ul>
 *   <li>Maximum size: 500 entries</li>
 *   <li>TTL: 5 minutes (expireAfterWrite)</li>
 *   <li>Eviction: LRU (Least Recently Used)</li>
 * </ul>
 *
 * @since 1.1.0
 */
public class LLMCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMCache.class);

    private static final int MAX_CACHE_SIZE = 500;
    private static final long TTL_MS = 5 * 60 * 1000; // 5 minutes

    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final ConcurrentLinkedDeque<String> accessOrder; // For LRU eviction
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    /**
     * Constructs a new LLMCache with default configuration.
     */
    public LLMCache() {
        LOGGER.info("Initializing LLM cache (max size: {}, TTL: {} minutes)", MAX_CACHE_SIZE, TTL_MS / 60000);
        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentLinkedDeque<>();
        LOGGER.info("LLM cache initialized successfully");
    }

    /**
     * Retrieves a cached response if available and not expired.
     */
    public Optional<LLMResponse> get(String prompt, String model, String providerId) {
        String key = generateKey(prompt, model, providerId);
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < TTL_MS) {
                // Cache hit - update access order
                hitCount.incrementAndGet();
                accessOrder.remove(key);
                accessOrder.addLast(key);
                LOGGER.debug("Cache HIT for provider={}, model={}, promptHash={}",
                    providerId, model, key.substring(0, 8));
                return Optional.of(entry.response);
            } else {
                // Expired - remove it
                cache.remove(key);
                accessOrder.remove(key);
            }
        }

        missCount.incrementAndGet();
        LOGGER.debug("Cache MISS for provider={}, model={}, promptHash={}",
            providerId, model, key.substring(0, 8));
        return Optional.empty();
    }

    /**
     * Stores a response in the cache.
     */
    public void put(String prompt, String model, String providerId, LLMResponse response) {
        String key = generateKey(prompt, model, providerId);

        // Evict if at capacity
        while (cache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }

        // Mark response as cached
        LLMResponse cachedResponse = response.withCacheFlag(true);
        cache.put(key, new CacheEntry(cachedResponse));
        accessOrder.addLast(key);

        LOGGER.debug("Cached response for provider={}, model={}, promptHash={}, tokens={}",
            providerId, model, key.substring(0, 8), response.getTokensUsed());
    }

    /**
     * Evicts the oldest (least recently used) entry.
     */
    private void evictOldest() {
        String oldest = accessOrder.pollFirst();
        if (oldest != null) {
            cache.remove(oldest);
            evictionCount.incrementAndGet();
        }
    }

    /**
     * Generates a cache key from prompt, model, and provider using SHA-256.
     */
    private String generateKey(String prompt, String model, String providerId) {
        String composite = providerId + ":" + model + ":" + prompt;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(composite.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 unavailable
            return String.valueOf(composite.hashCode());
        }
    }

    /**
     * Returns cache statistics for monitoring.
     */
    public CacheStatsSnapshot getStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;
        return new CacheStatsSnapshot(hitRate, hits, misses, evictionCount.get());
    }

    /**
     * Returns the approximate number of entries in the cache.
     */
    public long size() {
        return cache.size();
    }

    /**
     * Invalidates all entries in the cache.
     */
    public void clear() {
        long sizeBefore = cache.size();
        cache.clear();
        accessOrder.clear();
        LOGGER.info("Cache cleared, removed ~{} entries", sizeBefore);
    }

    /**
     * Logs current cache statistics at INFO level.
     */
    public void logStats() {
        CacheStatsSnapshot stats = getStats();
        LOGGER.info("LLM Cache Stats - Size: {}/{}, Hit Rate: {:.2f}%, Hits: {}, Misses: {}, Evictions: {}",
            size(),
            MAX_CACHE_SIZE,
            stats.hitRate * 100,
            stats.hits,
            stats.misses,
            stats.evictions
        );
    }

    /**
     * Internal cache entry with timestamp.
     */
    private static class CacheEntry {
        final LLMResponse response;
        final long timestamp;

        CacheEntry(LLMResponse response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Simple cache stats snapshot (replaces Caffeine's CacheStats).
     */
    public static class CacheStatsSnapshot {
        public final double hitRate;
        public final long hits;
        public final long misses;
        public final long evictions;

        public CacheStatsSnapshot(double hitRate, long hits, long misses, long evictions) {
            this.hitRate = hitRate;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
        }
    }
}
