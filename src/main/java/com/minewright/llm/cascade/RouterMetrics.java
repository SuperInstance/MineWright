package com.minewright.llm.cascade;

import com.minewright.testutil.TestLogger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * Tracks metrics and statistics for the smart cascade router.
 *
 * <p><b>Metrics Tracked:</b></p>
 * <ul>
 *   <li>Total requests processed</li>
 *   <li>Local vs cloud request distribution</li>
 *   <li>Model usage counts per model</li>
 *   <li>Vision request counts</li>
 *   <li>Estimated costs</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class RouterMetrics {
    private static final Logger LOGGER = TestLogger.getLogger(RouterMetrics.class);

    // Model identifiers (must match SmartModelRouter)
    public static final String LOCAL_SMOLVLM = "smolvlm";
    public static final String LOCAL_LLAMA = "llama3.2";
    public static final String MODEL_FLASHX = "glm-4.7-flashx";
    public static final String MODEL_FLASH = "glm-4.7-flash";
    public static final String MODEL_GLM5 = "glm-5";
    public static final String MODEL_VISION = "glm-4.6v";

    // Request counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong localHits = new AtomicLong(0);
    private final AtomicLong cloudRequests = new AtomicLong(0);
    private final AtomicLong visionRequests = new AtomicLong(0);

    // Cost tracking
    private final AtomicLong totalCostCents = new AtomicLong(0);

    // Per-model usage tracking
    private final ConcurrentHashMap<String, AtomicLong> modelUsage = new ConcurrentHashMap<>();

    public RouterMetrics() {
        // Initialize model usage counters
        modelUsage.put(LOCAL_SMOLVLM, new AtomicLong(0));
        modelUsage.put(LOCAL_LLAMA, new AtomicLong(0));
        modelUsage.put(MODEL_FLASHX, new AtomicLong(0));
        modelUsage.put(MODEL_FLASH, new AtomicLong(0));
        modelUsage.put(MODEL_GLM5, new AtomicLong(0));
        modelUsage.put(MODEL_VISION, new AtomicLong(0));
    }

    // ------------------------------------------------------------------------
    // Increment Methods
    // ------------------------------------------------------------------------

    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
    }

    public void incrementLocalHits() {
        localHits.incrementAndGet();
    }

    public void incrementCloudRequests() {
        cloudRequests.incrementAndGet();
    }

    public void incrementVisionRequests() {
        visionRequests.incrementAndGet();
    }

    public void incrementModelUsage(String model) {
        modelUsage.computeIfAbsent(model, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void addCostCents(long cents) {
        totalCostCents.addAndGet(cents);
    }

    // ------------------------------------------------------------------------
    // Getter Methods
    // ------------------------------------------------------------------------

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getLocalHits() {
        return localHits.get();
    }

    public long getCloudRequests() {
        return cloudRequests.get();
    }

    public long getVisionRequests() {
        return visionRequests.get();
    }

    public double getTotalCostDollars() {
        return totalCostCents.get() / 100.0;
    }

    public long getModelUsage(String model) {
        AtomicLong count = modelUsage.get(model);
        return count == null ? 0 : count.get();
    }

    public double getLocalHitRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) localHits.get() / total : 0.0;
    }

    // ------------------------------------------------------------------------
    // Logging and Reporting
    // ------------------------------------------------------------------------

    /**
     * Logs current router statistics.
     */
    public void logStats() {
        LOGGER.info("=== Smart Cascade Router Statistics ===");
        LOGGER.info("Total Requests: {}", totalRequests.get());
        LOGGER.info("Local Hits: {} ({:.1f}%)",
            localHits.get(),
            totalRequests.get() > 0 ? (localHits.get() * 100.0 / totalRequests.get()) : 0);
        LOGGER.info("Cloud Requests: {}", cloudRequests.get());
        LOGGER.info("Vision Requests: {}", visionRequests.get());
        LOGGER.info("Estimated Cost: ${:.2f}", getTotalCostDollars());

        LOGGER.info("Model Usage:");
        modelUsage.forEach((model, count) -> {
            if (count.get() > 0) {
                LOGGER.info("  {}: {} requests", model, count.get());
            }
        });
    }

    /**
     * Returns a summary of metrics as a string.
     */
    public String getSummary() {
        return String.format(
            "Requests: %d | Local: %d (%.1f%%) | Cloud: %d | Vision: %d | Cost: $%.2f",
            totalRequests.get(),
            localHits.get(),
            getLocalHitRate() * 100,
            cloudRequests.get(),
            visionRequests.get(),
            getTotalCostDollars()
        );
    }

    // ------------------------------------------------------------------------
    // Reset
    // ------------------------------------------------------------------------

    /**
     * Resets all metrics.
     */
    public void reset() {
        totalRequests.set(0);
        localHits.set(0);
        cloudRequests.set(0);
        visionRequests.set(0);
        totalCostCents.set(0);
        modelUsage.forEach((model, count) -> count.set(0));
        LOGGER.info("RouterMetrics reset");
    }
}
