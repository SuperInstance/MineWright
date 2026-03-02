package com.minewright.execution;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Enhanced interceptor for collecting comprehensive action execution metrics.
 *
 * <p>Tracks execution counts, success/failure rates, average durations,
 * tick execution times, and LLM API call timings. Metrics are stored in-memory
 * and can be retrieved for monitoring/observability.</p>
 *
 * <p><b>Collected Metrics:</b></p>
 * <ul>
 *   <li>Total executions per action type</li>
 *   <li>Success count per action type</li>
 *   <li>Failure count per action type</li>
 *   <li>Total duration per action type</li>
 *   <li>Average duration (calculated)</li>
 *   <li>Tick execution time metrics (min, max, avg)</li>
 *   <li>LLM API call timing metrics</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe and can be used by multiple
 * agents concurrently.</p>
 *
 * @since 1.1.0
 */
public class MetricsInterceptor implements ActionInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsInterceptor.class);

    /**
     * Metrics storage per action type.
     */
    private final ConcurrentHashMap<String, ActionMetrics> metricsMap;

    /**
     * Start time tracking for duration calculation.
     */
    private final ActionUtils.ActionTimer actionTimer = new ActionUtils.ActionTimer();

    /**
     * Global tick execution time metrics.
     */
    private final TickMetrics tickMetrics = new TickMetrics();

    /**
     * LLM API call timing metrics.
     */
    private final LLMMetrics llmMetrics = new LLMMetrics();

    public MetricsInterceptor() {
        this.metricsMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean beforeAction(BaseAction action, ActionContext context) {
        String actionType = ActionUtils.extractActionType(action);

        // Record start time
        actionTimer.recordStart(action);

        // Increment execution count
        getOrCreateMetrics(actionType).incrementExecutions();

        return true;
    }

    @Override
    public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
        String actionType = ActionUtils.extractActionType(action);
        ActionMetrics metrics = getOrCreateMetrics(actionType);

        // Calculate duration
        long duration = actionTimer.getElapsedAndRemove(action);

        // Update metrics
        metrics.addDuration(duration);
        if (result.isSuccess()) {
            metrics.incrementSuccesses();
        } else {
            metrics.incrementFailures();
        }

        LOGGER.debug("[METRICS] {} - duration: {}ms, total: {}, success rate: {:.1f}%",
            actionType, duration, metrics.getTotalExecutions(), metrics.getSuccessRate() * 100);
    }

    @Override
    public boolean onError(BaseAction action, Exception exception, ActionContext context) {
        String actionType = ActionUtils.extractActionType(action);
        getOrCreateMetrics(actionType).incrementErrors();

        // Clean up start time
        actionTimer.remove(action);

        return false;
    }

    @Override
    public int getPriority() {
        return 900; // High priority, after logging
    }

    @Override
    public String getName() {
        return "MetricsInterceptor";
    }

    /**
     * Gets or creates metrics for an action type.
     */
    private ActionMetrics getOrCreateMetrics(String actionType) {
        return metricsMap.computeIfAbsent(actionType, k -> new ActionMetrics());
    }

    /**
     * Returns metrics for a specific action type.
     *
     * @param actionType Action type
     * @return Metrics snapshot, or null if none exist
     */
    public MetricsSnapshot getMetrics(String actionType) {
        ActionMetrics metrics = metricsMap.get(actionType);
        return metrics != null ? metrics.snapshot() : null;
    }

    /**
     * Returns metrics for all action types.
     *
     * @return Map of action type to metrics snapshot
     */
    public Map<String, MetricsSnapshot> getAllMetrics() {
        ConcurrentHashMap<String, MetricsSnapshot> result = new ConcurrentHashMap<>();
        metricsMap.forEach((key, value) -> result.put(key, value.snapshot()));
        return result;
    }

    /**
     * Gets the global tick metrics.
     *
     * @return Tick metrics snapshot
     */
    public TickMetricsSnapshot getTickMetrics() {
        return tickMetrics.snapshot();
    }

    /**
     * Gets the LLM API timing metrics.
     *
     * @return LLM metrics snapshot
     */
    public LLMMetricsSnapshot getLLMMetrics() {
        return llmMetrics.snapshot();
    }

    /**
     * Records a tick execution time.
     * <p>Call this from the tick loop to track tick performance.</p>
     *
     * @param durationMs Tick duration in milliseconds
     */
    public void recordTickTime(long durationMs) {
        tickMetrics.recordTick(durationMs);
    }

    /**
     * Records an LLM API call duration.
     *
     * @param providerName The LLM provider name (e.g., "openai", "groq")
     * @param durationMs Call duration in milliseconds
     * @param success Whether the call was successful
     */
    public void recordLLMCall(String providerName, long durationMs, boolean success) {
        llmMetrics.recordCall(providerName, durationMs, success);
    }

    /**
     * Resets all metrics.
     */
    public void reset() {
        metricsMap.clear();
        actionTimer.clear();
        tickMetrics.reset();
        llmMetrics.reset();
        LOGGER.info("Metrics reset");
    }

    /**
     * Internal mutable metrics container.
     */
    private static class ActionMetrics {
        private final LongAdder totalExecutions = new LongAdder();
        private final LongAdder successes = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();

        void incrementExecutions() { totalExecutions.increment(); }
        void incrementSuccesses() { successes.increment(); }
        void incrementFailures() { failures.increment(); }
        void incrementErrors() { errors.increment(); }
        void addDuration(long duration) { totalDuration.add(duration); }

        long getTotalExecutions() { return totalExecutions.sum(); }

        double getSuccessRate() {
            long total = totalExecutions.sum();
            return total > 0 ? (double) successes.sum() / total : 0.0;
        }

        MetricsSnapshot snapshot() {
            long total = totalExecutions.sum();
            long avgDuration = total > 0 ? totalDuration.sum() / total : 0;
            return new MetricsSnapshot(
                total,
                successes.sum(),
                failures.sum(),
                errors.sum(),
                totalDuration.sum(),
                avgDuration
            );
        }
    }

    /**
     * Tick execution time metrics.
     */
    private static class TickMetrics {
        private final LongAdder totalTicks = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();
        private volatile long minDuration = Long.MAX_VALUE;
        private volatile long maxDuration = Long.MIN_VALUE;

        void recordTick(long durationMs) {
            totalTicks.increment();
            totalDuration.add(durationMs);

            // Update min/max (simple volatile - may race but acceptable for metrics)
            if (durationMs < minDuration) {
                minDuration = durationMs;
            }
            if (durationMs > maxDuration) {
                maxDuration = durationMs;
            }
        }

        TickMetricsSnapshot snapshot() {
            long ticks = totalTicks.sum();
            long avgDuration = ticks > 0 ? totalDuration.sum() / ticks : 0;
            return new TickMetricsSnapshot(
                ticks,
                avgDuration,
                minDuration == Long.MAX_VALUE ? 0 : minDuration,
                maxDuration == Long.MIN_VALUE ? 0 : maxDuration
            );
        }

        void reset() {
            totalTicks.reset();
            totalDuration.reset();
            minDuration = Long.MAX_VALUE;
            maxDuration = Long.MIN_VALUE;
        }
    }

    /**
     * LLM API call timing metrics.
     */
    private static class LLMMetrics {
        private final ConcurrentHashMap<String, ProviderMetrics> providerMetrics = new ConcurrentHashMap<>();

        void recordCall(String providerName, long durationMs, boolean success) {
            ProviderMetrics metrics = providerMetrics.computeIfAbsent(
                providerName, k -> new ProviderMetrics());
            metrics.recordCall(durationMs, success);
        }

        LLMMetricsSnapshot snapshot() {
            ConcurrentHashMap<String, ProviderMetricsSnapshot> snapshots = new ConcurrentHashMap<>();
            providerMetrics.forEach((key, value) -> snapshots.put(key, value.snapshot()));
            return new LLMMetricsSnapshot(snapshots);
        }

        void reset() {
            providerMetrics.clear();
        }

        private static class ProviderMetrics {
            private final LongAdder totalCalls = new LongAdder();
            private final LongAdder successes = new LongAdder();
            private final LongAdder failures = new LongAdder();
            private final LongAdder totalDuration = new LongAdder();
            private volatile long minDuration = Long.MAX_VALUE;
            private volatile long maxDuration = Long.MIN_VALUE;

            void recordCall(long durationMs, boolean success) {
                totalCalls.increment();
                totalDuration.add(durationMs);
                if (success) {
                    successes.increment();
                } else {
                    failures.increment();
                }

                if (durationMs < minDuration) {
                    minDuration = durationMs;
                }
                if (durationMs > maxDuration) {
                    maxDuration = durationMs;
                }
            }

            ProviderMetricsSnapshot snapshot() {
                long calls = totalCalls.sum();
                long avgDuration = calls > 0 ? totalDuration.sum() / calls : 0;
                return new ProviderMetricsSnapshot(
                    calls,
                    successes.sum(),
                    failures.sum(),
                    avgDuration,
                    minDuration == Long.MAX_VALUE ? 0 : minDuration,
                    maxDuration == Long.MIN_VALUE ? 0 : maxDuration
                );
            }
        }
    }

    /**
     * Immutable metrics snapshot for external use.
     */
    public record MetricsSnapshot(
        long totalExecutions,
        long successes,
        long failures,
        long errors,
        long totalDurationMs,
        long avgDurationMs
    ) {
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successes / totalExecutions : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "Metrics{total=%d, success=%d, fail=%d, errors=%d, avgDuration=%dms, successRate=%.1f%%}",
                totalExecutions, successes, failures, errors, avgDurationMs, getSuccessRate() * 100);
        }
    }

    /**
     * Tick metrics snapshot.
     */
    public record TickMetricsSnapshot(
        long totalTicks,
        long avgDurationMs,
        long minDurationMs,
        long maxDurationMs
    ) {
        @Override
        public String toString() {
            return String.format(
                "TickMetrics{ticks=%d, avg=%dms, min=%dms, max=%dms}",
                totalTicks, avgDurationMs, minDurationMs, maxDurationMs);
        }
    }

    /**
     * LLM metrics snapshot.
     */
    public record LLMMetricsSnapshot(
        Map<String, ProviderMetricsSnapshot> providers
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("LLMMetrics{");
            providers.forEach((provider, metrics) -> {
                sb.append(provider).append("=").append(metrics).append(", ");
            });
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * LLM provider metrics snapshot.
     */
    public record ProviderMetricsSnapshot(
        long totalCalls,
        long successes,
        long failures,
        long avgDurationMs,
        long minDurationMs,
        long maxDurationMs
    ) {
        public double getSuccessRate() {
            return totalCalls > 0 ? (double) successes / totalCalls : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "Provider{calls=%d, avg=%dms, min=%dms, max=%dms, successRate=%.1f%%}",
                totalCalls, avgDurationMs, minDurationMs, maxDurationMs, getSuccessRate() * 100);
        }
    }
}
