package com.minewright.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

/**
 * Thread-safe metrics collection for the Steve AI observability system.
 * Tracks agent performance, LLM usage, skill execution, and system health.
 *
 * <p>This collector provides:
 * <ul>
 *   <li>Agent metrics: tasks completed, success rate, execution time</li>
 *   <li>LLM metrics: tokens used, cost, latency, per-model breakdown</li>
 *   <li>Skill metrics: usage frequency, success rate, duration</li>
 *   <li>System health: memory usage, CPU load, uptime</li>
 *   <li>Event log: temporal sequence of notable events</li>
 * </ul>
 *
 * <p>All operations are thread-safe and use lock-free constructs
 * (ConcurrentHashMap, AtomicLong, DoubleAdder) for maximum performance.
 *
 * <p>Usage example:
 * <pre>{@code
 * MetricsCollector collector = MetricsCollector.getInstance();
 * collector.recordAgentTaskCompletion("steve-1", "mining", true, 1234);
 * collector.recordLLMCall("gpt-4", 1000, 500, 2345);
 * }</pre>
 */
public class MetricsCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);

    /** Singleton instance */
    private static volatile MetricsCollector instance;
    private static final Object INSTANCE_LOCK = new Object();

    /** System start time for uptime calculation */
    private final Instant startTime;

    // ========== AGENT METRICS ==========
    /** Agent task counts: agentId -> (taskType -> count) */
    private final Map<String, Map<String, AtomicLong>> agentTaskCounts = new ConcurrentHashMap<>();

    /** Agent task success counts: agentId -> (taskType -> successCount) */
    private final Map<String, Map<String, AtomicLong>> agentTaskSuccessCounts = new ConcurrentHashMap<>();

    /** Agent task execution times: agentId -> (taskType -> totalTimeNanos) */
    private final Map<String, Map<String, DoubleAdder>> agentTaskExecutionTimes = new ConcurrentHashMap<>();

    /** Agent state durations: agentId -> (state -> totalTimeNanos) */
    private final Map<String, Map<String, AtomicLong>> agentStateDurations = new ConcurrentHashMap<>();

    // ========== LLM METRICS ==========
    /** Total tokens used by model: modelName -> tokenCount */
    private final Map<String, AtomicLong> llmTokensUsed = new ConcurrentHashMap<>();

    /** Total LLM calls by model: modelName -> callCount */
    private final Map<String, AtomicLong> llmCallCounts = new ConcurrentHashMap<>();

    /** Total LLM latency by model: modelName -> totalTimeNanos */
    private final Map<String, DoubleAdder> llmTotalLatency = new ConcurrentHashMap<>();

    /** Total LLM cost by model: modelName -> totalCost */
    private final Map<String, DoubleAdder> llmTotalCost = new ConcurrentHashMap<>();

    /** Prompt tokens by model */
    private final Map<String, AtomicLong> llmPromptTokens = new ConcurrentHashMap<>();

    /** Completion tokens by model */
    private final Map<String, AtomicLong> llmCompletionTokens = new ConcurrentHashMap<>();

    // ========== SKILL METRICS ==========
    /** Skill usage counts: skillName -> usageCount */
    private final Map<String, AtomicLong> skillUsageCounts = new ConcurrentHashMap<>();

    /** Skill success counts: skillName -> successCount */
    private final Map<String, AtomicLong> skillSuccessCounts = new ConcurrentHashMap<>();

    /** Skill execution times: skillName -> totalTimeNanos */
    private final Map<String, DoubleAdder> skillExecutionTimes = new ConcurrentHashMap<>();

    // ========== SYSTEM HEALTH METRICS ==========
    /** Peak memory usage in bytes */
    private final AtomicLong peakMemoryBytes = new AtomicLong(0);

    /** Total garbage collections */
    private final AtomicLong totalGarbageCollections = new AtomicLong(0);

    /** Total time spent in GC (ms) */
    private final AtomicLong totalGcTimeMillis = new AtomicLong(0);

    /** Number of errors logged */
    private final AtomicLong errorCount = new AtomicLong(0);

    /** Number of warnings logged */
    private final AtomicLong warningCount = new AtomicLong(0);

    // ========== EVENT LOG ==========
    /** Temporal event log for analysis */
    private final List<MetricEvent> eventLog = new ArrayList<>();

    /** Maximum event log size */
    private static final int MAX_EVENT_LOG_SIZE = 10000;

    /**
     * Gets the singleton metrics collector instance.
     *
     * @return the metrics collector instance
     */
    public static MetricsCollector getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new MetricsCollector();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new metrics collector.
     */
    private MetricsCollector() {
        this.startTime = Instant.now();
        LOGGER.info("MetricsCollector initialized");
    }

    // ========== AGENT METRICS METHODS ==========

    /**
     * Records a task completion for an agent.
     *
     * @param agentId the agent identifier
     * @param taskType the type of task completed
     * @param success whether the task succeeded
     * @param durationNanos the task duration in nanoseconds
     */
    public void recordAgentTaskCompletion(String agentId, String taskType, boolean success, long durationNanos) {
        agentTaskCounts
            .computeIfAbsent(agentId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(taskType, k -> new AtomicLong(0))
            .incrementAndGet();

        if (success) {
            agentTaskSuccessCounts
                .computeIfAbsent(agentId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(taskType, k -> new AtomicLong(0))
                .incrementAndGet();
        }

        agentTaskExecutionTimes
            .computeIfAbsent(agentId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(taskType, k -> new DoubleAdder())
            .add(durationNanos);

        logEvent("agent_task_completion", Map.of(
            "agentId", agentId,
            "taskType", taskType,
            "success", String.valueOf(success),
            "durationNanos", String.valueOf(durationNanos)
        ));
    }

    /**
     * Records a state change for an agent.
     *
     * @param agentId the agent identifier
     * @param oldState the previous state
     * @param newState the new state
     */
    public void recordAgentStateChange(String agentId, String oldState, String newState) {
        logEvent("agent_state_change", Map.of(
            "agentId", agentId,
            "oldState", oldState,
            "newState", newState
        ));
    }

    /**
     * Records state duration for an agent.
     *
     * @param agentId the agent identifier
     * @param state the state name
     * @param durationNanos the duration in nanoseconds
     */
    public void recordAgentStateDuration(String agentId, String state, long durationNanos) {
        agentStateDurations
            .computeIfAbsent(agentId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(state, k -> new AtomicLong(0))
            .addAndGet(durationNanos);
    }

    // ========== LLM METRICS METHODS ==========

    /**
     * Records an LLM API call.
     *
     * @param modelName the model name (e.g., "gpt-4", "glm-5")
     * @param promptTokens the number of prompt tokens
     * @param completionTokens the number of completion tokens
     * @param latencyNanos the request latency in nanoseconds
     * @param cost the estimated cost in USD
     */
    public void recordLLMCall(String modelName, int promptTokens, int completionTokens,
                              long latencyNanos, double cost) {
        llmCallCounts.computeIfAbsent(modelName, k -> new AtomicLong(0)).incrementAndGet();
        llmTokensUsed.computeIfAbsent(modelName, k -> new AtomicLong(0))
            .addAndGet(promptTokens + completionTokens);
        llmPromptTokens.computeIfAbsent(modelName, k -> new AtomicLong(0))
            .addAndGet(promptTokens);
        llmCompletionTokens.computeIfAbsent(modelName, k -> new AtomicLong(0))
            .addAndGet(completionTokens);
        llmTotalLatency.computeIfAbsent(modelName, k -> new DoubleAdder()).add(latencyNanos);
        llmTotalCost.computeIfAbsent(modelName, k -> new DoubleAdder()).add(cost);

        logEvent("llm_call", Map.of(
            "modelName", modelName,
            "promptTokens", String.valueOf(promptTokens),
            "completionTokens", String.valueOf(completionTokens),
            "latencyNanos", String.valueOf(latencyNanos),
            "cost", String.valueOf(cost)
        ));
    }

    /**
     * Records an LLM API call with automatic cost calculation.
     *
     * @param modelName the model name
     * @param promptTokens the number of prompt tokens
     * @param completionTokens the number of completion tokens
     * @param latencyNanos the request latency in nanoseconds
     */
    public void recordLLMCall(String modelName, int promptTokens, int completionTokens, long latencyNanos) {
        double cost = estimateCost(modelName, promptTokens, completionTokens);
        recordLLMCall(modelName, promptTokens, completionTokens, latencyNanos, cost);
    }

    /**
     * Estimates the cost of an LLM call in USD.
     *
     * @param modelName the model name
     * @param promptTokens the number of prompt tokens
     * @param completionTokens the number of completion tokens
     * @return the estimated cost in USD
     */
    private double estimateCost(String modelName, int promptTokens, int completionTokens) {
        String lower = modelName.toLowerCase();
        double promptCostPer1k = 0.0;
        double completionCostPer1k = 0.0;

        if (lower.contains("gpt-4")) {
            promptCostPer1k = 0.03;
            completionCostPer1k = 0.06;
        } else if (lower.contains("gpt-3.5")) {
            promptCostPer1k = 0.0015;
            completionCostPer1k = 0.002;
        } else if (lower.contains("glm-5")) {
            promptCostPer1k = 0.01;
            completionCostPer1k = 0.02;
        } else if (lower.contains("llama") || lower.contains("groq")) {
            promptCostPer1k = 0.0001;
            completionCostPer1k = 0.0001;
        } else if (lower.contains("gemini")) {
            promptCostPer1k = 0.00025;
            completionCostPer1k = 0.0005;
        }

        return (promptTokens / 1000.0) * promptCostPer1k +
               (completionTokens / 1000.0) * completionCostPer1k;
    }

    // ========== SKILL METRICS METHODS ==========

    /**
     * Records a skill execution.
     *
     * @param skillName the skill name
     * @param success whether the skill succeeded
     * @param durationNanos the execution duration in nanoseconds
     */
    public void recordSkillExecution(String skillName, boolean success, long durationNanos) {
        skillUsageCounts.computeIfAbsent(skillName, k -> new AtomicLong(0)).incrementAndGet();

        if (success) {
            skillSuccessCounts.computeIfAbsent(skillName, k -> new AtomicLong(0)).incrementAndGet();
        }

        skillExecutionTimes.computeIfAbsent(skillName, k -> new DoubleAdder()).add(durationNanos);

        logEvent("skill_execution", Map.of(
            "skillName", skillName,
            "success", String.valueOf(success),
            "durationNanos", String.valueOf(durationNanos)
        ));
    }

    // ========== SYSTEM HEALTH METHODS ==========

    /**
     * Updates system health metrics (should be called periodically).
     */
    public void updateSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long currentPeak = peakMemoryBytes.get();
        while (usedMemory > currentPeak && !peakMemoryBytes.compareAndSet(currentPeak, usedMemory)) {
            currentPeak = peakMemoryBytes.get();
        }

        // GC metrics
        try {
            java.lang.management.MemoryMXBean memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            java.lang.management.MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long used = heapUsage.getUsed();
            long currentPeak2 = peakMemoryBytes.get();
            while (used > currentPeak2 && !peakMemoryBytes.compareAndSet(currentPeak2, used)) {
                currentPeak2 = peakMemoryBytes.get();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Records an error.
     *
     * @param source the error source
     * @param message the error message
     */
    public void recordError(String source, String message) {
        errorCount.incrementAndGet();
        logEvent("error", Map.of("source", source, "message", message));
    }

    /**
     * Records a warning.
     *
     * @param source the warning source
     * @param message the warning message
     */
    public void recordWarning(String source, String message) {
        warningCount.incrementAndGet();
        logEvent("warning", Map.of("source", source, "message", message));
    }

    // ========== SUMMARY METHODS ==========

    /**
     * Gets a summary of all agent metrics.
     *
     * @return map of agent metrics
     */
    public Map<String, Object> getAgentSummary() {
        Map<String, Object> summary = new HashMap<>();

        for (String agentId : agentTaskCounts.keySet()) {
            Map<String, Object> agentMetrics = new HashMap<>();

            Map<String, AtomicLong> tasks = agentTaskCounts.getOrDefault(agentId, Map.of());
            Map<String, AtomicLong> successes = agentTaskSuccessCounts.getOrDefault(agentId, Map.of());
            Map<String, DoubleAdder> times = agentTaskExecutionTimes.getOrDefault(agentId, Map.of());

            long totalTasks = tasks.values().stream().mapToLong(AtomicLong::get).sum();
            long totalSuccesses = successes.values().stream().mapToLong(AtomicLong::get).sum();
            double successRate = totalTasks > 0 ? (double) totalSuccesses / totalTasks : 0.0;

            double totalTimeNanos = times.values().stream().mapToDouble(DoubleAdder::sum).sum();
            double avgTaskTimeMillis = totalTasks > 0 ? (totalTimeNanos / 1_000_000.0) / totalTasks : 0.0;

            agentMetrics.put("total_tasks", totalTasks);
            agentMetrics.put("total_successes", totalSuccesses);
            agentMetrics.put("success_rate", successRate);
            agentMetrics.put("avg_task_time_ms", avgTaskTimeMillis);

            summary.put(agentId, agentMetrics);
        }

        return summary;
    }

    /**
     * Gets a summary of all LLM metrics.
     *
     * @return map of LLM metrics
     */
    public Map<String, Object> getLLMSummary() {
        Map<String, Object> summary = new HashMap<>();

        long totalTokens = llmTokensUsed.values().stream().mapToLong(AtomicLong::get).sum();
        long totalCalls = llmCallCounts.values().stream().mapToLong(AtomicLong::get).sum();
        double totalCost = llmTotalCost.values().stream().mapToDouble(DoubleAdder::sum).sum();

        long totalLatencyNanos = 0;
        for (DoubleAdder adder : llmTotalLatency.values()) {
            totalLatencyNanos += adder.sum();
        }
        double avgLatencyMillis = totalCalls > 0 ? (totalLatencyNanos / 1_000_000.0) / totalCalls : 0.0;

        summary.put("total_tokens", totalTokens);
        summary.put("total_calls", totalCalls);
        summary.put("total_cost_usd", totalCost);
        summary.put("avg_latency_ms", avgLatencyMillis);

        // Per-model breakdown
        Map<String, Object> byModel = new HashMap<>();
        for (String model : llmCallCounts.keySet()) {
            Map<String, Object> modelMetrics = new HashMap<>();
            modelMetrics.put("calls", llmCallCounts.get(model).get());
            modelMetrics.put("tokens", llmTokensUsed.getOrDefault(model, new AtomicLong(0)).get());
            modelMetrics.put("prompt_tokens", llmPromptTokens.getOrDefault(model, new AtomicLong(0)).get());
            modelMetrics.put("completion_tokens", llmCompletionTokens.getOrDefault(model, new AtomicLong(0)).get());
            modelMetrics.put("cost_usd", llmTotalCost.getOrDefault(model, new DoubleAdder()).sum());
            modelMetrics.put("avg_latency_ms",
                (llmTotalLatency.getOrDefault(model, new DoubleAdder()).sum() / 1_000_000.0) /
                Math.max(1, llmCallCounts.get(model).get()));
            byModel.put(model, modelMetrics);
        }
        summary.put("by_model", byModel);

        return summary;
    }

    /**
     * Gets a summary of all skill metrics.
     *
     * @return map of skill metrics
     */
    public Map<String, Object> getSkillSummary() {
        Map<String, Object> summary = new HashMap<>();

        for (String skillName : skillUsageCounts.keySet()) {
            long usageCount = skillUsageCounts.get(skillName).get();
            long successCount = skillSuccessCounts.getOrDefault(skillName, new AtomicLong(0)).get();
            double successRate = usageCount > 0 ? (double) successCount / usageCount : 0.0;
            double avgTimeMillis = usageCount > 0 ?
                (skillExecutionTimes.getOrDefault(skillName, new DoubleAdder()).sum() / 1_000_000.0) / usageCount : 0.0;

            Map<String, Object> skillMetrics = new HashMap<>();
            skillMetrics.put("usage_count", usageCount);
            skillMetrics.put("success_count", successCount);
            skillMetrics.put("success_rate", successRate);
            skillMetrics.put("avg_time_ms", avgTimeMillis);

            summary.put(skillName, skillMetrics);
        }

        return summary;
    }

    /**
     * Gets a summary of system health metrics.
     *
     * @return map of system health metrics
     */
    public Map<String, Object> getSystemHealthSummary() {
        Map<String, Object> summary = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        summary.put("uptime_seconds", ChronoUnit.SECONDS.between(startTime, Instant.now()));
        summary.put("max_memory_mb", maxMemory / (1024 * 1024));
        summary.put("total_memory_mb", totalMemory / (1024 * 1024));
        summary.put("used_memory_mb", usedMemory / (1024 * 1024));
        summary.put("free_memory_mb", freeMemory / (1024 * 1024));
        summary.put("peak_memory_mb", peakMemoryBytes.get() / (1024 * 1024));
        summary.put("memory_usage_percent", (maxMemory > 0 ? (100.0 * usedMemory / maxMemory) : 0.0));
        summary.put("error_count", errorCount.get());
        summary.put("warning_count", warningCount.get());
        summary.put("available_processors", runtime.availableProcessors());

        return summary;
    }

    /**
     * Gets a complete summary of all metrics.
     *
     * @return map of all metrics
     */
    public Map<String, Object> getCompleteSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("agents", getAgentSummary());
        summary.put("llm", getLLMSummary());
        summary.put("skills", getSkillSummary());
        summary.put("system_health", getSystemHealthSummary());
        summary.put("collected_at", Instant.now().toString());
        return summary;
    }

    /**
     * Gets the event log.
     *
     * @param limit maximum number of events to return
     * @return list of events
     */
    public List<MetricEvent> getEventLog(int limit) {
        synchronized (eventLog) {
            int fromIndex = Math.max(0, eventLog.size() - limit);
            return new ArrayList<>(eventLog.subList(fromIndex, eventLog.size()));
        }
    }

    /**
     * Clears all collected metrics.
     */
    public void clear() {
        agentTaskCounts.clear();
        agentTaskSuccessCounts.clear();
        agentTaskExecutionTimes.clear();
        agentStateDurations.clear();
        llmTokensUsed.clear();
        llmCallCounts.clear();
        llmTotalLatency.clear();
        llmTotalCost.clear();
        llmPromptTokens.clear();
        llmCompletionTokens.clear();
        skillUsageCounts.clear();
        skillSuccessCounts.clear();
        skillExecutionTimes.clear();
        peakMemoryBytes.set(0);
        totalGarbageCollections.set(0);
        totalGcTimeMillis.set(0);
        errorCount.set(0);
        warningCount.set(0);

        synchronized (eventLog) {
            eventLog.clear();
        }

        LOGGER.info("All metrics cleared");
    }

    /**
     * Logs an event to the event log.
     *
     * @param type the event type
     * @param attributes the event attributes
     */
    private void logEvent(String type, Map<String, String> attributes) {
        synchronized (eventLog) {
            eventLog.add(new MetricEvent(Instant.now(), type, attributes));
            if (eventLog.size() > MAX_EVENT_LOG_SIZE) {
                eventLog.remove(0);
            }
        }
    }

    /**
     * Represents a metric event in the event log.
     */
    public static class MetricEvent {
        private final Instant timestamp;
        private final String type;
        private final Map<String, String> attributes;

        public MetricEvent(Instant timestamp, String type, Map<String, String> attributes) {
            this.timestamp = timestamp;
            this.type = type;
            this.attributes = attributes;
        }

        public Instant getTimestamp() { return timestamp; }
        public String getType() { return type; }
        public Map<String, String> getAttributes() { return new HashMap<>(attributes); }
    }
}
