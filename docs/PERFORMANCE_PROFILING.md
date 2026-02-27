# Performance Profiling Guide - MineWright Minecraft Mod

**Version:** 1.1.0
**Last Updated:** 2025-01-10
**Status:** Comprehensive profiling instrumentation guide

---

## Table of Contents

1. [Overview](#overview)
2. [Current Performance Instrumentation](#current-performance-instrumentation)
3. [Profiling Hook Implementation](#profiling-hook-implementation)
4. [Metrics Collection Patterns](#metrics-collection-patterns)
5. [Performance Dashboard Concepts](#performance-dashboard-concepts)
6. [Optimization Targets](#optimization-targets)
7. [JFR Integration Guide](#jfr-integration-guide)
8. [Action-Specific Performance Analysis](#action-specific-performance-analysis)
9. [Memory Profiling](#memory-profiling)

---

## Overview

MineWright is a complex Minecraft mod with AI-driven agents, tick-based execution, and LLM integration. Performance bottlenecks can occur in multiple areas:

- **Tick execution:** Actions run 20 times per second per agent
- **LLM calls:** Async API calls with variable latency (1-30 seconds)
- **Pathfinding:** Minecraft's navigation system is computationally expensive
- **Memory systems:** Vector search and episodic memory retrieval
- **Collaborative building:** Multi-agent coordination overhead

This guide provides comprehensive instrumentation patterns to identify and resolve performance issues.

---

## Current Performance Instrumentation

### Existing Metrics Infrastructure

MineWright 1.1.0 includes built-in performance monitoring via the interceptor chain:

#### 1. MetricsInterceptor (`C:\Users\casey\steve\src\main\java\com\minewright\execution\MetricsInterceptor.java`)

**What it tracks:**
- Total executions per action type
- Success/failure counts
- Average duration per action
- Error counts

**Current implementation:**
```java
public class MetricsInterceptor implements ActionInterceptor {
    private final ConcurrentHashMap<String, ActionMetrics> metricsMap;
    private final ConcurrentHashMap<Integer, Long> startTimes;

    @Override
    public boolean beforeAction(BaseAction action, ActionContext context) {
        startTimes.put(System.identityHashCode(action), System.currentTimeMillis());
        getOrCreateMetrics(actionType).incrementExecutions();
        return true;
    }

    @Override
    public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
        long duration = System.currentTimeMillis() - startTime;
        metrics.addDuration(duration);
        // Logged at debug level
    }
}
```

**Limitations:**
- No tick-level granularity (only aggregates entire action duration)
- No per-tick timing data
- No memory profiling
- Logs at DEBUG level (may not be visible in production)
- No percentile statistics (p50, p95, p99)

#### 2. EventPublishingInterceptor (`C:\Users\casey\steve\src\main\java\com\minewright\execution\EventPublishingInterceptor.java`)

**What it tracks:**
- Action lifecycle events (started, completed)
- Duration per action
- Event bus publication

**Current implementation:**
```java
@Override
public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
    long duration = System.currentTimeMillis() - startTime;
    ActionCompletedEvent event = new ActionCompletedEvent(
        agentId, actionName, result.isSuccess(), result.getMessage(), duration
    );
    eventBus.publish(event);
}
```

#### 3. LLM Response Tracking (`C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMResponse.java`)

**What it tracks:**
- End-to-end latency (`latencyMs`)
- Token usage (`tokensUsed`)
- Cache status (`fromCache`)
- Provider ID

**Logged in TaskPlanner:**
```java
MineWrightMod.LOGGER.info("[Async] Plan received: {} ({} tasks, {}ms, {} tokens, cache: {})",
    parsedResponse.getPlan(),
    parsedResponse.getTasks().size(),
    response.getLatencyMs(),
    response.getTokensUsed(),
    response.isFromCache());
```

#### 4. Cache Statistics (`C:\Users\casey\steve\src\main\java\com\minewright\llm\async\LLMCache.java`)

**What it tracks:**
- Hit rate percentage
- Total hits/misses
- Eviction count
- Current size

**Access method:**
```java
CacheStatsSnapshot stats = llmCache.getStats();
// Returns: hitRate, hits, misses, evictions
```

### Gaps in Current Instrumentation

1. **No tick-level profiling:** Cannot identify which specific tick operations are slow
2. **No memory allocation tracking:** Unknown how much memory actions consume
3. **No GC pause monitoring:** Garbage collection impact unknown
4. **No thread pool metrics:** Async executor utilization not tracked
5. **No pathfinding metrics:** Minecraft navigation cost unknown
6. **No vector search timing:** Semantic memory search performance unknown
7. **No per-agent metrics:** Cannot profile individual agents in multi-agent scenarios
8. **No blocking detection:** Cannot identify when game thread is blocked

---

## Profiling Hook Implementation

### 1. TickExecutionProfiler - Granular Tick Timing

Add detailed per-tick instrumentation to identify slow operations:

```java
package com.minewright.profiling;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Profiles tick execution at a granular level.
 *
 * <p>Tracks time spent in each phase of tick execution:
 * <ul>
 *   <li>Planning checks (async LLM completion)</li>
 *   <li>Action execution (tick() calls)</li>
 *   <li>Task queuing</li>
 *   <li>Idle behavior</li>
 * </ul>
 *
 * <p><b>Usage:</b> Wrap ActionExecutor.tick() calls with profiling.</p>
 */
public class TickExecutionProfiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TickExecutionProfiler.class);
    private static final long TICK_TIME_BUDGET_MS = 50; // Target: 50ms per tick (20 TPS)

    private final ConcurrentHashMap<String, PhaseMetrics> phaseMetrics;
    private final AtomicLong totalTicks = new AtomicLong(0);
    private final AtomicLong slowTicks = new AtomicLong(0);
    private final LongAdder tickTimeOverhead = new LongAdder();

    public TickExecutionProfiler() {
        this.phaseMetrics = new ConcurrentHashMap<>();
    }

    /**
     * Profiles a single tick execution phase.
     *
     * @param phaseName Phase identifier (e.g., "planning_check", "action_tick", "idle_behavior")
     * @param runnable Phase implementation
     */
    public void profilePhase(String phaseName, Runnable runnable) {
        long startTime = System.nanoTime();
        try {
            runnable.run();
        } finally {
            long durationNs = System.nanoTime() - startTime;
            recordPhase(phaseName, durationNs);
        }
    }

    /**
     * Records timing for a phase.
     */
    private void recordPhase(String phaseName, long durationNs) {
        PhaseMetrics metrics = phaseMetrics.computeIfAbsent(phaseName, k -> new PhaseMetrics());
        metrics.record(durationNs);

        // Warn if phase exceeded budget
        long durationMs = durationNs / 1_000_000;
        if (durationMs > TICK_TIME_BUDGET_MS) {
            LOGGER.warn("[TICK PROFILING] Phase '{}' took {}ms (exceeds {}ms budget)",
                phaseName, durationMs, TICK_TIME_BUDGET_MS);
        }
    }

    /**
     * Marks the end of a complete tick cycle.
     */
    public void endTick(long tickStartNanos) {
        totalTicks.incrementAndGet();
        long tickDurationMs = (System.nanoTime() - tickStartNanos) / 1_000_000;

        if (tickDurationMs > TICK_TIME_BUDGET_MS) {
            slowTicks.incrementAndGet();
            LOGGER.warn("[TICK PROFILING] Slow tick: {}ms (budget: {}ms)",
                tickDurationMs, TICK_TIME_BUDGET_MS);
        }

        if (totalTicks.get() % 100 == 0) {
            logSummary();
        }
    }

    /**
     * Logs a summary of profiling statistics.
     */
    public void logSummary() {
        LOGGER.info("[TICK PROFILING] Summary (last {} ticks):", totalTicks.get());
        LOGGER.info("  Slow ticks: {}/{} ({}%)",
            slowTicks.get(), totalTicks.get(),
            (slowTicks.get() * 100.0 / totalTicks.get()));

        phaseMetrics.forEach((phase, metrics) -> {
            PhaseStatsSnapshot stats = metrics.snapshot();
            LOGGER.info("  Phase '{}': avg={}ms, p95={}ms, p99={}ms, max={}ms, calls={}",
                phase,
                stats.avgDurationMs,
                stats.p95DurationMs,
                stats.p99DurationMs,
                stats.maxDurationMs,
                stats.callCount);
        });
    }

    /**
     * Resets all profiling data.
     */
    public void reset() {
        phaseMetrics.clear();
        totalTicks.set(0);
        slowTicks.set(0);
        tickTimeOverhead.reset();
        LOGGER.info("Tick profiling data reset");
    }

    /**
     * Mutable metrics for a single phase.
     */
    private static class PhaseMetrics {
        private final LongAdder totalDurationNs = new LongAdder();
        private final LongAdder callCount = new LongAdder();
        private final ConcurrentHashMap<Integer, Long> recentSamples = new ConcurrentHashMap<>();
        private volatile long maxDurationNs = 0;
        private int sampleIndex = 0;

        private static final int SAMPLE_SIZE = 100;

        void record(long durationNs) {
            totalDurationNs.add(durationNs);
            callCount.increment();
            updateMax(durationNs);
            recordSample(durationNs);
        }

        private synchronized void updateMax(long durationNs) {
            if (durationNs > maxDurationNs) {
                maxDurationNs = durationNs;
            }
        }

        private synchronized void recordSample(long durationNs) {
            recentSamples.put(sampleIndex++, durationNs);
            if (sampleIndex >= SAMPLE_SIZE) {
                sampleIndex = 0;
            }
        }

        PhaseStatsSnapshot snapshot() {
            long count = callCount.sum();
            if (count == 0) {
                return new PhaseStatsSnapshot(0, 0, 0, 0, 0);
            }

            long avgNs = totalDurationNs.sum() / count;
            long[] percentiles = computePercentiles();

            return new PhaseStatsSnapshot(
                count,
                avgNs / 1_000_000,
                percentiles[0] / 1_000_000,
                percentiles[1] / 1_000_000,
                maxDurationNs / 1_000_000
            );
        }

        private long[] computePercentiles() {
            if (recentSamples.isEmpty()) {
                return new long[]{0, 0};
            }

            long[] samples = recentSamples.values().stream()
                .mapToLong(Long::longValue)
                .sorted()
                .toArray();

            int p95Index = (int) (samples.length * 0.95);
            int p99Index = (int) (samples.length * 0.99);

            return new long[]{
                samples[Math.min(p95Index, samples.length - 1)],
                samples[Math.min(p99Index, samples.length - 1)]
            };
        }
    }

    /**
     * Immutable snapshot of phase statistics.
     */
    public record PhaseStatsSnapshot(
        long callCount,
        long avgDurationMs,
        long p95DurationMs,
        long p99DurationMs,
        long maxDurationMs
    ) {}
}
```

**Integration with ActionExecutor:**

```java
// In ActionExecutor.java
private final TickExecutionProfiler tickProfiler = new TickExecutionProfiler();

public void tick() {
    long tickStart = System.nanoTime();

    // Phase 1: Check async planning
    tickProfiler.profilePhase("planning_check", () -> {
        if (isPlanning && planningFuture != null && planningFuture.isDone()) {
            // ... existing planning completion code ...
        }
    });

    // Phase 2: Execute current action
    tickProfiler.profilePhase("action_tick", () -> {
        if (currentAction != null) {
            if (currentAction.isComplete()) {
                // ... existing completion code ...
            } else {
                if (ticksSinceLastAction % 100 == 0) {
                    MineWrightMod.LOGGER.debug("Ticking action: {}",
                        currentAction.getDescription());
                }
                currentAction.tick();
            }
        }
    });

    // Phase 3: Queue next task
    tickProfiler.profilePhase("task_queue", () -> {
        if (ticksSinceLastAction >= MineWrightConfig.ACTION_TICK_DELAY.get()) {
            if (!taskQueue.isEmpty()) {
                Task nextTask = taskQueue.poll();
                executeTask(nextTask);
                ticksSinceLastAction = 0;
            }
        }
    });

    // Phase 4: Idle behavior
    tickProfiler.profilePhase("idle_behavior", () -> {
        if (taskQueue.isEmpty() && currentAction == null && currentGoal == null) {
            // ... existing idle follow code ...
        }
    });

    tickProfiler.endTick(tickStart);
    ticksSinceLastAction++;
}
```

### 2. MemoryProfiler - Memory Allocation Tracking

Track memory usage by action type and agent:

```java
package com.minewright.profiling;

import com.minewright.MineWrightMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Profiles memory usage patterns during action execution.
 *
 * <p>Tracks heap memory before/after actions to identify memory leaks
 * or high-allocation actions.</p>
 */
public class MemoryProfiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryProfiler.class);
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    private final ConcurrentHashMap<String, MemoryMetrics> memoryMetrics;
    private final AtomicLong baselineMemoryBytes;

    public MemoryProfiler() {
        this.memoryMetrics = new ConcurrentHashMap<>();
        this.baselineMemoryBytes = new AtomicLong(getCurrentHeapUsage());
    }

    /**
     * Profiles memory usage for an action.
     *
     * @param actionType Action identifier
     * @param runnable Action to profile
     */
    public void profileAction(String actionType, Runnable runnable) {
        long memoryBefore = getCurrentHeapUsage();
        long start = System.nanoTime();

        try {
            runnable.run();
        } finally {
            long memoryAfter = getCurrentHeapUsage();
            long duration = System.nanoTime() - start;
            record(actionType, memoryBefore, memoryAfter, duration);
        }
    }

    private long getCurrentHeapUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }

    private void record(String actionType, long before, long after, long durationNs) {
        MemoryMetrics metrics = memoryMetrics.computeIfAbsent(actionType, k -> new MemoryMetrics());
        long deltaBytes = after - before;
        long deltaMs = durationNs / 1_000_000;

        metrics.recordAllocation(deltaBytes);
        metrics.recordDuration(deltaMs);

        // Warn if action allocated > 10MB
        if (deltaBytes > 10_000_000) {
            LOGGER.warn("[MEMORY PROFILING] Action '{}' allocated {}MB in {}ms",
                actionType, deltaBytes / 1_000_000, deltaMs);
        }
    }

    /**
     * Logs current memory statistics.
     */
    public void logSummary() {
        long currentHeap = getCurrentHeapUsage();
        long totalDelta = currentHeap - baselineMemoryBytes.get();

        LOGGER.info("[MEMORY PROFILING] Heap usage: {}MB (baseline: {}MB, delta: {}MB)",
            currentHeap / 1_000_000,
            baselineMemoryBytes.get() / 1_000_000,
            totalDelta / 1_000_000);

        memoryMetrics.forEach((action, metrics) -> {
            MemoryStatsSnapshot stats = metrics.snapshot();
            LOGGER.info("  Action '{}': avgAlloc={}KB, maxAlloc={}KB, avgDuration={}ms, calls={}",
                action,
                stats.avgAllocationBytes / 1024,
                stats.maxAllocationBytes / 1024,
                stats.avgDurationMs,
                stats.callCount);
        });
    }

    /**
     * Suggests GC if memory usage is high.
     */
    public void suggestGC() {
        long currentHeap = getCurrentHeapUsage();
        long totalDelta = currentHeap - baselineMemoryBytes.get();

        if (totalDelta > 100_000_000) { // > 100MB delta
            LOGGER.warn("[MEMORY PROFILING] High memory usage detected: {}MB above baseline",
                totalDelta / 1_000_000);
            LOGGER.warn("[MEMORY PROFILING] Consider calling System.gc()");
        }
    }

    /**
     * Resets baseline to current heap usage.
     */
    public void resetBaseline() {
        baselineMemoryBytes.set(getCurrentHeapUsage());
        LOGGER.info("[MEMORY PROFILING] Baseline reset to {}MB",
            baselineMemoryBytes.get() / 1_000_000);
    }

    private static class MemoryMetrics {
        private final AtomicLong totalAllocation = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong callCount = new AtomicLong(0);
        private volatile long maxAllocation = 0;

        void recordAllocation(long bytes) {
            totalAllocation.addAndGet(bytes);
            updateMax(bytes);
        }

        void recordDuration(long ms) {
            totalDuration.addAndGet(ms);
            callCount.incrementAndGet();
        }

        private synchronized void updateMax(long bytes) {
            if (bytes > maxAllocation) {
                maxAllocation = bytes;
            }
        }

        MemoryStatsSnapshot snapshot() {
            long count = callCount.get();
            return new MemoryStatsSnapshot(
                count,
                count > 0 ? totalAllocation.get() / count : 0,
                maxAllocation,
                count > 0 ? totalDuration.get() / count : 0
            );
        }
    }

    public record MemoryStatsSnapshot(
        long callCount,
        long avgAllocationBytes,
        long maxAllocationBytes,
        long avgDurationMs
    ) {}
}
```

### 3. AsyncLatencyProfiler - LLM Call Latency Tracking

Track async LLM performance with percentiles:

```java
package com.minewright.profiling;

import com.minewright.llm.async.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Profiles LLM call latency and token usage.
 *
 * <p>Tracks latency percentiles, token efficiency, and cache effectiveness.</p>
 */
public class AsyncLatencyProfiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncLatencyProfiler.class);

    private final ConcurrentHashMap<String, ProviderMetrics> providerMetrics;
    private final AtomicLong totalRequests = new AtomicLong(0);

    public AsyncLatencyProfiler() {
        this.providerMetrics = new ConcurrentHashMap<>();
    }

    /**
     * Records an LLM response.
     */
    public void recordResponse(String providerId, LLMResponse response) {
        totalRequests.incrementAndGet();

        ProviderMetrics metrics = providerMetrics.computeIfAbsent(
            providerId, k -> new ProviderMetrics());

        metrics.recordLatency(response.getLatencyMs());
        metrics.recordTokens(response.getTokensUsed());
        metrics.recordCache(response.isFromCache());

        // Log slow requests
        if (response.getLatencyMs() > 5000 && !response.isFromCache()) {
            LOGGER.warn("[LLM PROFILING] Slow {} request: {}ms, {} tokens",
                providerId, response.getLatencyMs(), response.getTokensUsed());
        }
    }

    /**
     * Logs latency statistics.
     */
    public void logSummary() {
        LOGGER.info("[LLM PROFILING] Total requests: {}", totalRequests.get());

        providerMetrics.forEach((provider, metrics) -> {
            ProviderStatsSnapshot stats = metrics.snapshot();
            LOGGER.info("  Provider '{}': p50={}ms, p95={}ms, p99={}ms, avgTokens={}, cacheHitRate={}%",
                provider,
                stats.p50LatencyMs,
                stats.p95LatencyMs,
                stats.p99LatencyMs,
                stats.avgTokens,
                stats.cacheHitRate * 100);
        });
    }

    private static class ProviderMetrics {
        private final LongAdder totalLatency = new LongAdder();
        private final LongAdder totalTokens = new LongAdder();
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong requestCount = new AtomicLong(0);

        // Simple percentile approximation
        private final ConcurrentHashMap<Integer, Long> latencySamples = new ConcurrentHashMap<>();
        private int sampleIndex = 0;
        private static final int MAX_SAMPLES = 1000;

        void recordLatency(long ms) {
            totalLatency.add(ms);
            requestCount.incrementAndGet();
            addSample(ms);
        }

        void recordTokens(int tokens) {
            totalTokens.add(tokens);
        }

        void recordCache(boolean fromCache) {
            if (fromCache) {
                cacheHits.incrementAndGet();
            }
        }

        private synchronized void addSample(long ms) {
            latencySamples.put(sampleIndex++, ms);
            if (sampleIndex >= MAX_SAMPLES) {
                sampleIndex = 0;
            }
        }

        ProviderStatsSnapshot snapshot() {
            long count = requestCount.get();
            long avgLatency = count > 0 ? totalLatency.sum() / count : 0;
            long avgTokens = count > 0 ? totalTokens.sum() / count : 0;
            double cacheHitRate = count > 0 ? (double) cacheHits.get() / count : 0.0;

            long[] percentiles = computePercentiles(count);

            return new ProviderStatsSnapshot(
                avgLatency,
                percentiles[0], // p50
                percentiles[1], // p95
                percentiles[2], // p99
                avgTokens,
                cacheHitRate
            );
        }

        private long[] computePercentiles(long count) {
            if (latencySamples.isEmpty()) {
                return new long[]{0, 0, 0};
            }

            long[] samples = latencySamples.values().stream()
                .mapToLong(Long::longValue)
                .sorted()
                .toArray();

            int p50 = (int) (samples.length * 0.50);
            int p95 = (int) (samples.length * 0.95);
            int p99 = (int) (samples.length * 0.99);

            return new long[]{
                samples[Math.min(p50, samples.length - 1)],
                samples[Math.min(p95, samples.length - 1)],
                samples[Math.min(p99, samples.length - 1)]
            };
        }
    }

    public record ProviderStatsSnapshot(
        long avgLatencyMs,
        long p50LatencyMs,
        long p95LatencyMs,
        long p99LatencyMs,
        long avgTokens,
        double cacheHitRate
    ) {}
}
```

### 4. PathfindingProfiler - Navigation Cost Tracking

Track Minecraft pathfinding performance:

```java
package com.minewright.profiling;

import com.minewright.MineWrightMod;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Profiles pathfinding performance.
 *
 * <p>Tracks pathfinding success rate, duration, and distance.</p>
 */
public class PathfindingProfiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathfindingProfiler.class);

    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong successfulPaths = new AtomicLong(0);
    private final AtomicLong timeoutPaths = new AtomicLong(0);
    private final LongAdder totalDistance = new LongAdder();
    private final LongAdder totalDuration = new LongAdder();

    private final ConcurrentHashMap<String, PathfindingMetrics> byAgent;

    public PathfindingProfiler() {
        this.byAgent = new ConcurrentHashMap<>();
    }

    /**
     * Records a pathfinding attempt.
     *
     * @param agentName Agent attempting pathfinding
     * @param start Start position
     * @param end Target position
     * @param durationMs Time taken
     * @param success Whether path was reached
     * @param timedOut Whether timeout occurred
     */
    public void recordPathfind(String agentName, BlockPos start, BlockPos end,
                               long durationMs, boolean success, boolean timedOut) {
        totalAttempts.incrementAndGet();
        totalDuration.add(durationMs);

        double distance = Math.sqrt(start.distSqr(end));
        totalDistance.add((long) distance);

        if (success) {
            successfulPaths.incrementAndGet();
        }
        if (timedOut) {
            timeoutPaths.incrementAndGet();
        }

        PathfindingMetrics metrics = byAgent.computeIfAbsent(
            agentName, k -> new PathfindingMetrics());
        metrics.record(durationMs, distance, success, timedOut);

        LOGGER.debug("[PATHFINDING] {}: {}ms, {} blocks, success={}, timeout={}",
            agentName, durationMs, (long) distance, success, timedOut);
    }

    /**
     * Logs pathfinding statistics.
     */
    public void logSummary() {
        long total = totalAttempts.get();
        long success = successfulPaths.get();
        long timeouts = timeoutPaths.get();
        long avgDistance = total > 0 ? totalDistance.sum() / total : 0;
        long avgDuration = total > 0 ? totalDuration.sum() / total : 0;

        LOGGER.info("[PATHFINDING] Summary: {}/{} successful ({}%), {} timeouts, avg {} blocks, {}ms",
            success, total,
            total > 0 ? (success * 100.0 / total) : 0,
            timeouts,
            avgDistance,
            avgDuration);

        byAgent.forEach((agent, metrics) -> {
            PathfindingStatsSnapshot stats = metrics.snapshot();
            LOGGER.info("  Agent '{}': successRate={}%, timeoutRate={}%, avgDistance={} blocks",
                agent,
                stats.successRate * 100,
                stats.timeoutRate * 100,
                stats.avgDistance);
        });
    }

    private static class PathfindingMetrics {
        private final LongAdder totalDuration = new LongAdder();
        private final LongAdder totalDistance = new LongAdder();
        private final AtomicLong attempts = new AtomicLong(0);
        private final AtomicLong successes = new AtomicLong(0);
        private final AtomicLong timeouts = new AtomicLong(0);

        void record(long durationMs, double distance, boolean success, boolean timeout) {
            totalDuration.add(durationMs);
            totalDistance.add((long) distance);
            attempts.incrementAndGet();
            if (success) successes.incrementAndGet();
            if (timeout) timeouts.incrementAndGet();
        }

        PathfindingStatsSnapshot snapshot() {
            long attempts = this.attempts.get();
            return new PathfindingStatsSnapshot(
                attempts > 0 ? successes.get() / (double) attempts : 0.0,
                attempts > 0 ? timeouts.get() / (double) attempts : 0.0,
                attempts > 0 ? totalDistance.sum() / attempts : 0.0
            );
        }
    }

    public record PathfindingStatsSnapshot(
        double successRate,
        double timeoutRate,
        double avgDistance
    ) {}
}
```

---

## Metrics Collection Patterns

### 1. Sampling-Based Profiling

For minimal overhead, use sampling instead of instrumentation:

```java
package com.minewright.profiling;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Profiles using random sampling to minimize overhead.
 *
 * <p>Only profiles 1% of ticks by default.</p>
 */
public class SamplingProfiler {

    private final double sampleRate;
    private final Random random = new Random();
    private final AtomicLong sampledTicks = new AtomicLong(0);
    private final AtomicLong totalTicks = new AtomicLong(0);

    public SamplingProfiler(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Returns true if this tick should be profiled.
     */
    public boolean shouldSample() {
        totalTicks.incrementAndGet();
        if (random.nextDouble() < sampleRate) {
            sampledTicks.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Gets the actual sample rate achieved.
     */
    public double getActualSampleRate() {
        long total = totalTicks.get();
        return total > 0 ? (double) sampledTicks.get() / total : 0.0;
    }
}
```

### 2. Sliding Window Metrics

Track recent performance without storing all history:

```java
package com.minewright.profiling;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks metrics over a sliding window of recent events.
 *
 * <p>Uses a circular buffer for O(1) updates.</p>
 */
public class SlidingWindowMetrics {

    private final long[] window;
    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicLong count = new AtomicLong(0);

    public SlidingWindowMetrics(int windowSize) {
        this.window = new long[windowSize];
    }

    /**
     * Records a value in the window.
     */
    public void record(long value) {
        int idx = index.getAndIncrement() % window.length;
        window[idx] = value;
        count.incrementAndGet();
    }

    /**
     * Gets the average value in the window.
     */
    public double getAverage() {
        long sum = 0;
        int size = Math.min((int) count.get(), window.length);
        for (int i = 0; i < size; i++) {
            sum += window[i];
        }
        return size > 0 ? (double) sum / size : 0.0;
    }

    /**
     * Gets the maximum value in the window.
     */
    public long getMaximum() {
        long max = Long.MIN_VALUE;
        int size = Math.min((int) count.get(), window.length);
        for (int i = 0; i < size; i++) {
            if (window[i] > max) {
                max = window[i];
            }
        }
        return max;
    }

    /**
     * Gets the percentile value in the window.
     */
    public long getPercentile(double percentile) {
        int size = Math.min((int) count.get(), window.length);
        if (size == 0) return 0;

        // Copy and sort (inefficient but simple)
        long[] sorted = new long[size];
        System.arraycopy(window, 0, sorted, 0, size);
        java.util.Arrays.sort(sorted);

        int idx = (int) (size * percentile);
        return sorted[Math.min(idx, size - 1)];
    }
}
```

---

## Performance Dashboard Concepts

### 1. In-Game Metrics Overlay

Render performance stats directly in Minecraft:

```java
package com.minewright.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders performance metrics as an in-game overlay.
 */
public class PerformanceOverlay implements IGuiOverlay {

    private static final int TEXT_COLOR = 0xFF00FF00; // Green
    private static final int WARNING_COLOR = 0xFFFFFF00; // Yellow

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick,
                       int screenWidth, int screenHeight) {

        Font font = gui.getMinecraft().font;
        List<String> lines = collectMetrics();

        int x = 10;
        int y = 10;

        for (String line : lines) {
            int color = line.contains("WARNING") ? WARNING_COLOR : TEXT_COLOR;
            GuiComponent.drawString(poseStack, font, line, x, y, color);
            y += font.lineHeight + 2;
        }
    }

    private List<String> collectMetrics() {
        List<String> lines = new ArrayList<>();

        // Tick metrics
        lines.add("=== MineWright Performance ===");

        // ActionExecutor metrics
        if (com.minewright.MineWrightMod.getActionExecutor() != null) {
            var executor = com.minewright.MineWrightMod.getActionExecutor();
            lines.add("Agent: " + executor.getForeman().getSteveName());
            lines.add("Action: " + (executor.getCurrentAction() != null ?
                executor.getCurrentAction().getDescription() : "None"));
            lines.add("Queue: " + executor.getTaskQueue().size() + " tasks");
        }

        // LLM metrics
        if (com.minewright.MineWrightMod.getTaskPlanner() != null) {
            var cacheStats = com.minewright.MineWrightMod.getTaskPlanner()
                .getLLMCache().getStats();
            lines.add("LLM Cache: " + String.format("%.1f%% hit rate",
                cacheStats.hitRate * 100));
        }

        return lines;
    }
}
```

### 2. HTTP Metrics Endpoint (for external dashboards)

Expose metrics via HTTP for Grafana/Prometheus:

```java
package com.minewright.profiling;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Exposes metrics on an HTTP endpoint for external monitoring.
 */
public class MetricsServer {

    private final HttpServer server;
    private final TickExecutionProfiler tickProfiler;
    private final MemoryProfiler memoryProfiler;
    private final AsyncLatencyProfiler llmProfiler;

    public MetricsServer(int port,
                         TickExecutionProfiler tickProfiler,
                         MemoryProfiler memoryProfiler,
                         AsyncLatencyProfiler llmProfiler) throws IOException {

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.tickProfiler = tickProfiler;
        this.memoryProfiler = memoryProfiler;
        this.llmProfiler = llmProfiler;

        server.createContext("/metrics", handleMetrics());
        server.createContext("/health", handleHealth());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private HttpHandler handleMetrics() {
        return exchange -> {
            String response = generatePrometheusMetrics();
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        };
    }

    private HttpHandler handleHealth() {
        return exchange -> {
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        };
    }

    private String generatePrometheusMetrics() {
        StringBuilder sb = new StringBuilder();

        // Tick metrics
        sb.append("# HELP minewright_tick_duration_ms Tick duration in milliseconds\n");
        sb.append("# TYPE minewright_tick_duration_ms gauge\n");
        // ... append actual metrics ...

        return sb.toString();
    }
}
```

---

## Optimization Targets

Based on profiling analysis, target these thresholds:

### 1. Tick Execution

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Tick duration | < 50ms | > 50ms | > 100ms |
| Action tick time | < 10ms | > 20ms | > 50ms |
| Planning check | < 1ms | > 5ms | > 10ms |
| Idle overhead | < 1ms | > 5ms | > 10ms |

### 2. LLM Performance

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| p50 latency | < 2s | > 5s | > 10s |
| p95 latency | < 5s | > 15s | > 30s |
| Cache hit rate | > 40% | < 20% | < 10% |
| Tokens per request | < 1000 | > 2000 | > 5000 |

### 3. Action Duration

| Action Type | Target | Warning | Critical |
|-------------|--------|---------|----------|
| Pathfind | < 10s | > 20s | > 30s |
| Mine | < 30s | > 60s | > 120s |
| Build | < 300s | > 600s | > 1200s |
| Craft | < 10s | > 20s | > 30s |
| Place | < 30s | > 60s | > 90s |

### 4. Memory Usage

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Heap delta per action | < 5MB | > 10MB | > 50MB |
| Vector store size | < 1000 | > 5000 | > 10000 |
| Episodic memories | < 200 | > 400 | > 1000 |

---

## JFR Integration Guide

Java Flight Recorder (JFR) provides low-overhead production profiling.

### 1. Enable JFR for Minecraft

```bash
# Start Minecraft with JFR recording
java -XX:StartFlightRecording=filename=minewright.jfr,dumponexit=true -jar forge.jar
```

### 2. Add Custom JFR Events

```java
package com.minewright.profiling;

import jdk.jfr.Event;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * JFR event for action execution.
 */
@Name("com.minewright.ActionExecution")
@Label("MineWright Action Execution")
@Category("MineWright")
public class ActionExecutionEvent extends Event {

    @Label("Agent Name")
    private String agentName;

    @Label("Action Type")
    private String actionType;

    @Label("Duration (ms)")
    private long durationMs;

    @Label("Success")
    private boolean success;

    // Getters and setters
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public void setSuccess(boolean success) { this.success = success; }
}

/**
 * JFR event for LLM calls.
 */
@Name("com.minewright.LLMCall")
@Label("MineWright LLM Call")
@Category("MineWright")
public class LLMCallEvent extends Event {

    @Label("Provider")
    private String provider;

    @Label("Latency (ms)")
    private long latencyMs;

    @Label("Tokens Used")
    private int tokensUsed;

    @Label("From Cache")
    private boolean fromCache;

    // Getters and setters
    public void setProvider(String provider) { this.provider = provider; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public void setTokensUsed(int tokensUsed) { this.tokensUsed = tokensUsed; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
}
```

### 3. Emit JFR Events in Interceptors

```java
public class JFRMetricsInterceptor implements ActionInterceptor {

    @Override
    public void afterAction(BaseAction action, ActionResult result, ActionContext context) {
        ActionExecutionEvent event = new ActionExecutionEvent();
        event.begin();
        event.setAgentName(context.getStateMachine().getAgentId());
        event.setActionType(extractActionType(action));
        event.setDurationMs(calculateDuration(action));
        event.setSuccess(result.isSuccess());
        event.commit();
    }
}
```

### 4. Analyze JFR Recordings

```bash
# Print JFR summary
jfr print --events com.minewright.* minewright.jfr

# Analyze with JDK Mission Control
jmc minewright.jfr
```

---

## Action-Specific Performance Analysis

### 1. PathfindAction

**Current bottlenecks:**
- Minecraft navigation system runs on main thread
- No timeout enforcement (hardcoded 600 ticks)
- No path caching
- Re-calculates path on `isDone()` if not at target

**Profiling hooks:**
```java
@Override
protected void onTick() {
    long start = System.nanoTime();
    ticksRunning++;

    // Profile navigation check
    if (foreman.getNavigation().isDone() &&
        !foreman.blockPosition().closerThan(targetPos, 2.0)) {

        long navStart = System.nanoTime();
        foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(),
            targetPos.getZ(), 1.0);
        long navDuration = System.nanoTime() - navStart;

        if (navDuration > 1_000_000) { // > 1ms
            MineWrightMod.LOGGER.warn("[PATHFIND] Navigation move took {}ms",
                navDuration / 1_000_000);
        }
    }

    long tickDuration = System.nanoTime() - start;
    if (tickDuration > 5_000_000) { // > 5ms
        MineWrightMod.LOGGER.warn("[PATHFIND] Tick took {}ms", tickDuration / 1_000_000);
    }
}
```

### 2. BuildStructureAction

**Current bottlenecks:**
- CollaborativeBuildManager synchronization overhead
- Block placement occurs every tick (no batching)
- Particle effects for every block
- `getProgressPercentage()` called frequently

**Profiling hooks:**
```java
@Override
protected void onTick() {
    long start = System.nanoTime();
    ticksRunning++;

    for (int i = 0; i < BLOCKS_PER_TICK; i++) {
        long blockStart = System.nanoTime();

        BlockPlacement placement =
            CollaborativeBuildManager.getNextBlock(collaborativeBuild,
                foreman.getSteveName());

        if (placement != null) {
            // ... block placement code ...
        }

        long blockDuration = System.nanoTime() - blockStart;
        if (blockDuration > 10_000_000) { // > 10ms per block
            MineWrightMod.LOGGER.warn("[BUILD] Block placement took {}ms",
                blockDuration / 1_000_000);
        }
    }

    long tickDuration = System.nanoTime() - start;
    if (tickDuration > 50_000_000) { // > 50ms total
        MineWrightMod.LOGGER.warn("[BUILD] Tick took {}ms", tickDuration / 1_000_000);
    }
}
```

### 3. MineBlockAction

**Current bottlenecks:**
- Pathfinding to ore
- Block break animation (20 ticks hardcoded)
- No tool efficiency tracking

**Profiling hooks:**
```java
// Track mining phases
private enum MiningPhase { PATHFIND, APPROACH, BREAKING, COLLECT }
private MiningPhase currentPhase;
private long phaseStartTime;

@Override
protected void onTick() {
    long now = System.nanoTime();

    if (currentPhase == null || phaseChanged()) {
        long phaseDuration = (now - phaseStartTime) / 1_000_000;
        MineWrightMod.LOGGER.info("[MINE] Phase {} took {}ms",
            currentPhase, phaseDuration);
        currentPhase = determinePhase();
        phaseStartTime = now;
    }

    // ... existing mining code ...
}
```

---

## Memory Profiling

### 1. CompanionMemory Usage

**Current memory allocation:**
- `MAX_EPISODIC_MEMORIES = 200`
- `MAX_WORKING_MEMORY = 20`
- `MAX_INSIDE_JOKES = 30`
- Vector store: 1 vector per episodic memory (384 floats = 1.5KB each)

**Estimated usage:**
- Episodic memories: ~200 * 1KB = 200KB
- Vector store: ~200 * 1.5KB = 300KB
- Semantic memories: Variable (depends on player interactions)
- Total per agent: ~1-2MB

**Profiling hook:**
```java
// In CompanionMemory.java
public void logMemoryUsage() {
    long episodicSize = episodicMemories.size() * 1024; // Estimate 1KB each
    long vectorStoreSize = memoryVectorStore.size() * 384 * 4; // 384 floats * 4 bytes
    long semanticSize = semanticMemories.size() * 512; // Estimate 512B each

    long total = episodicSize + vectorStoreSize + semanticSize;

    LOGGER.info("[MEMORY] CompanionMemory: {} episodic ({}KB), {} vectors ({}KB), {} semantic ({}KB), total: {}KB",
        episodicMemories.size(), episodicSize / 1024,
        memoryVectorStore.size(), vectorStoreSize / 1024,
        semanticMemories.size(), semanticSize / 1024,
        total / 1024);
}
```

### 2. InMemoryVectorStore Analysis

**Current implementation:**
- Stores vectors as `float[]` arrays
- Each vector = 384 floats * 4 bytes = 1.5KB
- No compression
- Linear search for similarity (O(n))

**Profiling hook:**
```java
// In InMemoryVectorStore.java
public List<VectorSearchResult<T>> search(float[] queryVector, int k) {
    long start = System.nanoTime();

    // ... existing search code ...

    long duration = System.nanoTime() - start;
    long durationMs = duration / 1_000_000;
    long vectorCount = vectors.size();

    LOGGER.debug("[VECTOR] Search: {} vectors, k={}, {}ms, {}ms/vector",
        vectorCount, k, durationMs,
        vectorCount > 0 ? durationMs / vectorCount : 0);

    if (durationMs > 50) {
        LOGGER.warn("[VECTOR] Slow search: {} vectors in {}ms",
            vectorCount, durationMs);
    }

    return results;
}
```

### 3. NBT Serialization Profiling

**NBT save/load can be expensive:**

```java
// In CompanionMemory.java
public void saveToNBT(CompoundTag tag) {
    long start = System.nanoTime();

    // ... existing save code ...

    long duration = System.nanoTime() - start;
    LOGGER.info("[NBT] Saved {} episodic + {} semantic memories in {}ms",
        episodicMemories.size(), semanticMemories.size(),
        duration / 1_000_000);
}
```

---

## Summary

This guide provides comprehensive instrumentation for profiling MineWright performance:

1. **TickExecutionProfiler** - Per-tick timing with percentiles
2. **MemoryProfiler** - Heap allocation tracking
3. **AsyncLatencyProfiler** - LLM call latency with cache metrics
4. **PathfindingProfiler** - Navigation success rate and duration
5. **JFR Integration** - Production-ready low-overhead profiling
6. **In-Game Dashboard** - Real-time metrics overlay
7. **HTTP Metrics Server** - External monitoring integration

**Next steps:**
1. Add profilers to ActionExecutor
2. Enable JFR recording in production
3. Set up Grafana dashboard for metrics visualization
4. Establish baseline performance for common actions
5. Optimize identified bottlenecks (pathfinding, NBT serialization, vector search)

---

**Document Location:** `C:\Users\casey\steve\docs\PERFORMANCE_PROFILING.md`
