package com.minewright.llm.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single entry in the semantic LLM cache.
 *
 * <p>Each cache entry stores:</p>
 * <ul>
 *   <li>The original prompt text</li>
 *   <li>Its embedding vector for similarity comparison</li>
 *   <li>The cached LLM response</li>
 *   <li>Timestamp for TTL-based eviction</li>
 *   <li>Hit count for LRU-based eviction</li>
 *   <li>Optional metadata for debugging and analytics</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class uses atomic operations for hit count
 * and is suitable for concurrent access.</p>
 *
 * @since 1.6.0
 */
public class SemanticCacheEntry {

    private final String prompt;
    private final String model;
    private final String providerId;
    private final EmbeddingVector embedding;
    private final String response;
    private final long timestamp;
    private final Map<String, Object> metadata;
    private final AtomicInteger hitCount;
    private final int tokensUsed;

    /**
     * Creates a new semantic cache entry.
     *
     * @param prompt    The original prompt text
     * @param model     The LLM model used
     * @param providerId The LLM provider ID
     * @param embedding The embedding vector for semantic comparison
     * @param response  The cached LLM response
     */
    public SemanticCacheEntry(String prompt, String model, String providerId,
                               EmbeddingVector embedding, String response) {
        this(prompt, model, providerId, embedding, response, 0, new HashMap<>());
    }

    /**
     * Creates a new semantic cache entry with full metadata.
     *
     * @param prompt     The original prompt text
     * @param model      The LLM model used
     * @param providerId The LLM provider ID
     * @param embedding  The embedding vector for semantic comparison
     * @param response   The cached LLM response
     * @param tokensUsed Number of tokens used in the response
     * @param metadata   Additional metadata
     */
    public SemanticCacheEntry(String prompt, String model, String providerId,
                               EmbeddingVector embedding, String response,
                               int tokensUsed, Map<String, Object> metadata) {
        this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.providerId = Objects.requireNonNull(providerId, "Provider ID cannot be null");
        this.embedding = Objects.requireNonNull(embedding, "Embedding cannot be null");
        this.response = Objects.requireNonNull(response, "Response cannot be null");
        this.tokensUsed = tokensUsed;
        this.metadata = new HashMap<>(metadata);
        this.timestamp = System.currentTimeMillis();
        this.hitCount = new AtomicInteger(0);
    }

    /**
     * Returns the original prompt text.
     *
     * @return The prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Returns the LLM model used for this entry.
     *
     * @return The model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Returns the provider ID.
     *
     * @return The provider ID
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Returns the embedding vector for semantic comparison.
     *
     * @return The embedding vector
     */
    public EmbeddingVector getEmbedding() {
        return embedding;
    }

    /**
     * Returns the cached response.
     *
     * @return The LLM response
     */
    public String getResponse() {
        return response;
    }

    /**
     * Returns the timestamp when this entry was created.
     *
     * @return Creation time in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the age of this entry.
     *
     * @return Age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Returns the number of times this entry has been accessed.
     *
     * @return Hit count
     */
    public int getHitCount() {
        return hitCount.get();
    }

    /**
     * Increments the hit count atomically.
     */
    public void incrementHitCount() {
        hitCount.incrementAndGet();
    }

    /**
     * Returns the number of tokens used in the response.
     *
     * @return Token count
     */
    public int getTokensUsed() {
        return tokensUsed;
    }

    /**
     * Returns metadata value for the given key.
     *
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Returns all metadata.
     *
     * @return Copy of metadata map
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Checks if this entry is older than the specified age.
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return true if entry is older than maxAgeMs
     */
    public boolean isOlderThan(long maxAgeMs) {
        return getAge() > maxAgeMs;
    }

    /**
     * Calculates a score for LRU eviction (lower = more evictable).
     *
     * <p>Combines age and hit count to prioritize entries that are both
     * old and rarely used.</p>
     *
     * @return Eviction score (lower is more evictable)
     */
    public double getEvictionScore() {
        double ageMinutes = getAge() / 60000.0;
        int hits = getHitCount();
        // Score = age / (hits + 1) - older and less-used items score higher
        return ageMinutes / (hits + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticCacheEntry that = (SemanticCacheEntry) o;
        return timestamp == that.timestamp &&
               tokensUsed == that.tokensUsed &&
               Objects.equals(prompt, that.prompt) &&
               Objects.equals(model, that.model) &&
               Objects.equals(providerId, that.providerId) &&
               Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prompt, model, providerId, timestamp);
    }

    @Override
    public String toString() {
        return String.format(
            "SemanticCacheEntry{prompt='%s', model='%s', provider='%s', " +
            "age=%dms, hits=%d, tokens=%d, embedding=%s}",
            truncate(prompt, 30),
            model,
            providerId,
            getAge(),
            getHitCount(),
            tokensUsed,
            embedding
        );
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Builder pattern for creating semantic cache entries.
     */
    public static class Builder {
        private String prompt;
        private String model;
        private String providerId;
        private EmbeddingVector embedding;
        private String response;
        private int tokensUsed;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder embedding(EmbeddingVector embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder tokensUsed(int tokensUsed) {
            this.tokensUsed = tokensUsed;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public SemanticCacheEntry build() {
            return new SemanticCacheEntry(
                prompt, model, providerId, embedding, response,
                tokensUsed, metadata
            );
        }
    }
}
