package com.minewright.llm.cascade;

import com.minewright.testutil.TestLogger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

/**
 * Tracks model failures and implements skip logic for unreliable models.
 *
 * <p><b>Failure Handling:</b></p>
 * <ul>
 *   <li>Models are skipped after 3 consecutive failures</li>
 *   <li>Failure count resets after 1 minute</li>
 *   <li>Pre cascades repeated calls to failing models</li>
 *   <li>Thread-safe for concurrent access</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class ModelFailureTracker {
    private static final Logger LOGGER = TestLogger.getLogger(ModelFailureTracker.class);

    private static final int MAX_FAILURES_BEFORE_SKIP = 3;
    private static final long FAILURE_RESET_MS = 60000; // Reset after 1 minute

    private final ConcurrentHashMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastFailureTime = new ConcurrentHashMap<>();

    /**
     * Checks if a model should be skipped due to recent failures.
     *
     * @param model Model identifier
     * @return true if model should be skipped
     */
    public boolean shouldSkipModel(String model) {
        AtomicInteger failures = failureCount.get(model);
        if (failures == null || failures.get() < MAX_FAILURES_BEFORE_SKIP) {
            return false;
        }

        // Check if we should reset the failure count
        Long lastFailure = lastFailureTime.get(model);
        if (lastFailure != null && System.currentTimeMillis() - lastFailure > FAILURE_RESET_MS) {
            failureCount.put(model, new AtomicInteger(0));
            return false;
        }

        return true;
    }

    /**
     * Records a failure for a model.
     *
     * @param model Model identifier
     */
    public void recordFailure(String model) {
        failureCount.computeIfAbsent(model, k -> new AtomicInteger(0)).incrementAndGet();
        lastFailureTime.put(model, System.currentTimeMillis());
        LOGGER.debug("[FailureTracker] Model {} failed (total: {})",
            model, failureCount.get(model).get());
    }

    /**
     * Records a success for a model, resetting failure count.
     *
     * @param model Model identifier
     */
    public void recordSuccess(String model) {
        int previousFailures = failureCount.getOrDefault(model, new AtomicInteger(0)).get();
        failureCount.put(model, new AtomicInteger(0));
        lastFailureTime.remove(model);

        if (previousFailures > 0) {
            LOGGER.info("[FailureTracker] Model {} recovered after {} failures",
                model, previousFailures);
        }
    }

    /**
     * Gets the current failure count for a model.
     *
     * @param model Model identifier
     * @return Current failure count
     */
    public int getFailureCount(String model) {
        AtomicInteger count = failureCount.get(model);
        return count == null ? 0 : count.get();
    }

    /**
     * Resets all failure tracking (for testing).
     */
    public void reset() {
        failureCount.clear();
        lastFailureTime.clear();
        LOGGER.info("[FailureTracker] All failure tracking reset");
    }
}
