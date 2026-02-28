package com.minewright.llm.cache;

import java.util.StringJoiner;

/**
 * Statistics snapshot for the semantic LLM cache.
 *
 * <p>Provides comprehensive metrics for monitoring cache performance
 * and effectiveness. Tracks both exact and semantic hit rates separately
 * to understand the value of semantic caching.</p>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * @since 1.6.0
 */
public class CacheStats {

    /** Current number of entries in the cache */
    public final int size;

    /** Overall hit rate (exact + semantic hits / total requests) */
    public final double hitRate;

    /** Exact match hit rate (exact hits / total requests) */
    public final double exactHitRate;

    /** Semantic match hit rate (semantic hits / total requests) */
    public final double semanticHitRate;

    /** Average similarity score of semantic hits [0.0, 1.0] */
    public final double averageSimilarity;

    /** Total number of exact cache hits */
    public final long exactHits;

    /** Total number of semantic cache hits */
    public final long semanticHits;

    /** Total number of cache misses */
    public final long misses;

    /** Total number of entries evicted */
    public final long evictions;

    /** Approximate memory usage in bytes (estimate) */
    public final long estimatedMemoryBytes;

    /**
     * Creates a new cache stats snapshot.
     */
    public CacheStats(int size, double hitRate, double exactHitRate,
                      double semanticHitRate, double averageSimilarity,
                      long exactHits, long semanticHits, long misses,
                      long evictions) {
        this(size, hitRate, exactHitRate, semanticHitRate, averageSimilarity,
             exactHits, semanticHits, misses, evictions, estimateMemory(size));
    }

    /**
     * Creates a new cache stats snapshot with memory estimate.
     */
    public CacheStats(int size, double hitRate, double exactHitRate,
                      double semanticHitRate, double averageSimilarity,
                      long exactHits, long semanticHits, long misses,
                      long evictions, long estimatedMemoryBytes) {
        this.size = size;
        this.hitRate = hitRate;
        this.exactHitRate = exactHitRate;
        this.semanticHitRate = semanticHitRate;
        this.averageSimilarity = averageSimilarity;
        this.exactHits = exactHits;
        this.semanticHits = semanticHits;
        this.misses = misses;
        this.evictions = evictions;
        this.estimatedMemoryBytes = estimatedMemoryBytes;
    }

    /**
     * Returns total number of cache requests.
     *
     * @return Total requests (hits + misses)
     */
    public long getTotalRequests() {
        return exactHits + semanticHits + misses;
    }

    /**
     * Returns total number of cache hits (exact + semantic).
     *
     * @return Total hits
     */
    public long getTotalHits() {
        return exactHits + semanticHits;
    }

    /**
     * Returns the percentage of hits that were semantic matches.
     *
     * @return Semantic hit percentage [0.0, 100.0]
     */
    public double getSemanticHitPercentage() {
        long totalHits = getTotalHits();
        return totalHits > 0 ? (semanticHits * 100.0) / totalHits : 0.0;
    }

    /**
     * Returns the percentage of hits that were exact matches.
     *
     * @return Exact hit percentage [0.0, 100.0]
     */
    public double getExactHitPercentage() {
        long totalHits = getTotalHits();
        return totalHits > 0 ? (exactHits * 100.0) / totalHits : 0.0;
    }

    /**
     * Returns estimated memory usage in MB.
     *
     * @return Memory usage in megabytes
     */
    public double getEstimatedMemoryMB() {
        return estimatedMemoryBytes / (1024.0 * 1024.0);
    }

    /**
     * Estimates memory usage for a given cache size.
     *
     * @param size Cache size
     * @return Estimated memory in bytes
     */
    private static long estimateMemory(int size) {
        // Rough estimate per entry:
        // - Prompt: ~200 bytes average
        // - Response: ~1000 bytes average
        // - Embedding: 256 dimensions * 4 bytes = ~1024 bytes
        // - Metadata: ~200 bytes
        // = ~2500 bytes per entry
        return (long) (size * 2500L);
    }

    /**
     * Creates an empty stats object (zeroed values).
     *
     * @return Empty stats
     */
    public static CacheStats empty() {
        return new CacheStats(0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0);
    }

    /**
     * Returns a summary string of key metrics.
     *
     * @return Summary string
     */
    public String getSummary() {
        return String.format(
            "CacheStats[size=%d, hitRate=%.2f%%, exact=%.2f%%, semantic=%.2f%%, " +
            "avgSim=%.3f, requests=%d, memory=%.2fMB]",
            size, hitRate * 100, exactHitRate * 100, semanticHitRate * 100,
            averageSimilarity, getTotalRequests(), getEstimatedMemoryMB()
        );
    }

    /**
     * Returns a detailed multi-line report.
     *
     * @return Detailed report string
     */
    public String getDetailedReport() {
        StringJoiner report = new StringJoiner("\n");
        report.add("=== Semantic LLM Cache Statistics ===");
        report.add("");
        report.add("Cache Size:");
        report.add(String.format("  Current entries: %d", size));
        report.add(String.format("  Estimated memory: %.2f MB", getEstimatedMemoryMB()));
        report.add("");
        report.add("Hit Rates:");
        report.add(String.format("  Overall hit rate: %.2f%%", hitRate * 100));
        report.add(String.format("  Exact matches: %.2f%% (%d requests)", exactHitRate * 100, exactHits));
        report.add(String.format("  Semantic matches: %.2f%% (%d requests)", semanticHitRate * 100, semanticHits));
        report.add(String.format("  Misses: %.2f%% (%d requests)", (misses * 100.0) / getTotalRequests(), misses));
        report.add("");
        report.add("Semantic Quality:");
        report.add(String.format("  Average similarity: %.4f / 1.0", averageSimilarity));
        report.add(String.format("  Hit composition: %.1f%% exact, %.1f%% semantic",
            getExactHitPercentage(), getSemanticHitPercentage()));
        report.add("");
        report.add("Operations:");
        report.add(String.format("  Total requests: %d", getTotalRequests()));
        report.add(String.format("  Total evictions: %d", evictions));
        report.add("");
        return report.toString();
    }

    @Override
    public String toString() {
        return getSummary();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheStats that = (CacheStats) o;
        return size == that.size &&
               Double.compare(that.hitRate, hitRate) == 0 &&
               Double.compare(that.exactHitRate, exactHitRate) == 0 &&
               Double.compare(that.semanticHitRate, semanticHitRate) == 0 &&
               Double.compare(that.averageSimilarity, averageSimilarity) == 0 &&
               exactHits == that.exactHits &&
               semanticHits == that.semanticHits &&
               misses == that.misses &&
               evictions == that.evictions;
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + Double.hashCode(hitRate);
        result = 31 * result + Double.hashCode(exactHitRate);
        result = 31 * result + Double.hashCode(semanticHitRate);
        result = 31 * result + Double.hashCode(averageSimilarity);
        result = 31 * result + Long.hashCode(exactHits);
        result = 31 * result + Long.hashCode(semanticHits);
        result = 31 * result + Long.hashCode(misses);
        result = 31 * result + Long.hashCode(evictions);
        return result;
    }

    /**
     * Builder for creating CacheStats incrementally.
     */
    public static class Builder {
        private int size = 0;
        private long exactHits = 0;
        private long semanticHits = 0;
        private long misses = 0;
        private long evictions = 0;
        private double totalSimilarity = 0.0;

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder exactHits(long exactHits) {
            this.exactHits = exactHits;
            return this;
        }

        public Builder semanticHits(long semanticHits) {
            this.semanticHits = semanticHits;
            return this;
        }

        public Builder misses(long misses) {
            this.misses = misses;
            return this;
        }

        public Builder evictions(long evictions) {
            this.evictions = evictions;
            return this;
        }

        public Builder addSimilarity(double similarity) {
            this.totalSimilarity += similarity;
            return this;
        }

        public CacheStats build() {
            long total = exactHits + semanticHits + misses;
            double hitRate = total > 0 ? (double) (exactHits + semanticHits) / total : 0.0;
            double exactHitRate = total > 0 ? (double) exactHits / total : 0.0;
            double semanticHitRate = total > 0 ? (double) semanticHits / total : 0.0;
            double avgSim = semanticHits > 0 ? totalSimilarity / semanticHits : 0.0;

            return new CacheStats(
                size, hitRate, exactHitRate, semanticHitRate, avgSim,
                exactHits, semanticHits, misses, evictions
            );
        }
    }
}
