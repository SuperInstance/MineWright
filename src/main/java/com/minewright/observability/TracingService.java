package com.minewright.observability;

import com.minewright.observability.span.SpanContext;
import com.minewright.observability.span.SpanKind;
import com.minewright.observability.span.SpanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Central tracing service for distributed tracing in the MineWright system.
 * Manages span lifecycle, thread-local context propagation, and trace export.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Thread-local span stack for automatic parent-child relationships</li>
 *   <li>Factory methods for creating specialized spans (LLM, Skill, Contract Net)</li>
 *   <li>Export to JSON and CSV formats for analysis</li>
 *   <li>Statistics tracking for span counts and durations</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * try (TraceSpan span = TracingService.startLLMSpan("planTasks", "gpt-4")) {
 *     span.setAttribute("prompt.length", prompt.length());
 *     // ... do work ...
 *     span.end(SpanStatus.OK);
 * }
 * }</pre>
 */
public class TracingService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Thread-local stack of active spans for automatic parent-child relationships */
    private static final ThreadLocal<List<TraceSpan>> THREAD_SPAN_STACK = ThreadLocal.withInitial(ArrayList::new);

    /** Global registry of all completed spans for export */
    private final LinkedBlockingQueue<TraceSpan> completedSpans;

    /** Configuration for this tracing service instance */
    private final TracingConfig config;

    /** Statistics tracking */
    private final AtomicLong totalSpansCreated = new AtomicLong(0);
    private final AtomicLong totalSpansCompleted = new AtomicLong(0);
    private final AtomicLong totalSpansErrored = new AtomicLong(0);
    private final Map<String, AtomicLong> spansByKind = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> spansByName = new ConcurrentHashMap<>();

    /** Singleton instance */
    private static volatile TracingService instance;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Gets the singleton tracing service instance.
     *
     * @return the tracing service instance
     */
    public static TracingService getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new TracingService(TracingConfig.loadDefault());
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new tracing service with the given configuration.
     *
     * @param config the tracing configuration
     */
    public TracingService(TracingConfig config) {
        this.config = config;
        this.completedSpans = new LinkedBlockingQueue<>(config.getExportQueueSize());
        LOGGER.info("TracingService initialized with config: {}", config);
    }

    /**
     * Starts a new root span with automatic parent from thread-local context.
     *
     * @param name the span name
     * @param kind the span kind
     * @return the new span
     */
    public static TraceSpan startSpan(String name, SpanKind kind) {
        return getInstance().createSpan(name, kind, (TraceSpan) null);
    }

    /**
     * Starts a new child span with the specified parent.
     *
     * @param name the span name
     * @param kind the span kind
     * @param parent the parent span
     * @return the new span
     */
    public static TraceSpan startSpan(String name, SpanKind kind, TraceSpan parent) {
        return getInstance().createSpan(name, kind, parent);
    }

    /**
     * Starts a new span from an existing context.
     *
     * @param name the span name
     * @param kind the span kind
     * @param context the span context
     * @return the new span
     */
    public static TraceSpan startSpan(String name, SpanKind kind, SpanContext context) {
        return getInstance().createSpan(name, kind, context);
    }

    /**
     * Creates a new LLM-specific span with automatic attributes.
     *
     * @param operation the LLM operation (e.g., "chat.completion", "embeddings")
     * @param model the model name (e.g., "gpt-4", "glm-5")
     * @return the new LLM span
     */
    public static TraceSpan startLLMSpan(String operation, String model) {
        TracingService service = getInstance();
        if (!service.config.isLLMTracingEnabled()) {
            return null;
        }
        TraceSpan span = service.createSpan("llm." + operation, SpanKind.CLIENT, (TraceSpan) null);
        if (span != null) {
            span.setAttribute("llm.operation", operation);
            span.setAttribute("llm.model", model);
            span.setAttribute("llm.provider", extractProvider(model));
        }
        return span;
    }

    /**
     * Creates a new skill-specific span with automatic attributes.
     *
     * @param skillName the name of the skill being executed
     * @param skillType the type of skill (e.g., "mining", "building", "composed")
     * @return the new skill span
     */
    public static TraceSpan startSkillSpan(String skillName, String skillType) {
        TracingService service = getInstance();
        if (!service.config.isSkillTracingEnabled()) {
            return null;
        }
        TraceSpan span = service.createSpan("skill." + skillName, SpanKind.INTERNAL, (TraceSpan) null);
        if (span != null) {
            span.setAttribute("skill.name", skillName);
            span.setAttribute("skill.type", skillType);
        }
        return span;
    }

    /**
     * Creates a new Contract Net protocol span with automatic attributes.
     *
     * @param phase the protocol phase (e.g., "announcement", "bidding", "award")
     * @param taskType the type of task being coordinated
     * @return the new Contract Net span
     */
    public static TraceSpan startContractNetSpan(String phase, String taskType) {
        TracingService service = getInstance();
        if (!service.config.isContractNetTracingEnabled()) {
            return null;
        }
        TraceSpan span = service.createSpan("contract_net." + phase, SpanKind.INTERNAL, (TraceSpan) null);
        if (span != null) {
            span.setAttribute("contract_net.phase", phase);
            span.setAttribute("contract_net.task_type", taskType);
        }
        return span;
    }

    /**
     * Gets the current active span from the thread-local stack.
     *
     * @return the current span, or null if no active span
     */
    public static TraceSpan getCurrentSpan() {
        List<TraceSpan> stack = THREAD_SPAN_STACK.get();
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /**
     * Gets the current span context for propagation.
     *
     * @return the current span context, or null if no active span
     */
    public static SpanContext getCurrentContext() {
        TraceSpan current = getCurrentSpan();
        return current != null ? current.toContext() : null;
    }

    /**
     * Creates a new span with automatic parent from thread-local context.
     *
     * @param name the span name
     * @param kind the span kind
     * @param explicitParent explicit parent span (overrides thread-local)
     * @return the new span
     */
    private TraceSpan createSpan(String name, SpanKind kind, TraceSpan explicitParent) {
        if (!config.isEnabled()) {
            return null;
        }

        totalSpansCreated.incrementAndGet();
        spansByName.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
        spansByKind.computeIfAbsent(kind.name(), k -> new AtomicLong(0)).incrementAndGet();

        TraceSpan parent = explicitParent != null ? explicitParent : getCurrentSpan();
        TraceSpan span = new TraceSpan(name, kind, parent);

        // Push to thread-local stack
        List<TraceSpan> stack = THREAD_SPAN_STACK.get();
        stack.add(span);

        LOGGER.debug("Started span: {} (traceId: {}, spanId: {})", name, span.getTraceId(), span.getSpanId());
        return span;
    }

    /**
     * Creates a new span from an existing context.
     *
     * @param name the span name
     * @param kind the span kind
     * @param context the span context
     * @return the new span
     */
    private TraceSpan createSpan(String name, SpanKind kind, SpanContext context) {
        if (!config.isEnabled() || context == null) {
            return null;
        }

        totalSpansCreated.incrementAndGet();
        spansByName.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
        spansByKind.computeIfAbsent(kind.name(), k -> new AtomicLong(0)).incrementAndGet();

        TraceSpan span = new TraceSpan(name, kind, context);

        // Push to thread-local stack
        List<TraceSpan> stack = THREAD_SPAN_STACK.get();
        stack.add(span);

        LOGGER.debug("Started span from context: {} (traceId: {}, spanId: {})", name, span.getTraceId(), span.getSpanId());
        return span;
    }

    /**
     * Ends a span and records it in the completed spans queue.
     *
     * @param span the span to end
     * @param status the completion status
     */
    public void endSpan(TraceSpan span, SpanStatus status) {
        if (span == null) {
            return;
        }

        span.end(status);

        // Pop from thread-local stack
        List<TraceSpan> stack = THREAD_SPAN_STACK.get();
        if (!stack.isEmpty() && stack.get(stack.size() - 1) == span) {
            stack.remove(stack.size() - 1);
        }

        // Record statistics
        totalSpansCompleted.incrementAndGet();
        if (status == SpanStatus.ERROR) {
            totalSpansErrored.incrementAndGet();
        }

        // Add to completed spans queue (non-blocking)
        completedSpans.offer(span);

        LOGGER.debug("Ended span: {} (status: {}, duration: {}ms)", span.getName(), status, span.getDurationMillis());
    }

    /**
     * Ends a span with OK status.
     *
     * @param span the span to end
     */
    public void endSpan(TraceSpan span) {
        endSpan(span, SpanStatus.OK);
    }

    /**
     * Exports all completed spans to JSON format.
     *
     * @param exportPath the directory to export to
     * @return the number of spans exported
     * @throws IOException if export fails
     */
    public int exportToJson(String exportPath) throws IOException {
        if (!config.isJsonExportEnabled()) {
            LOGGER.warn("JSON export is disabled");
            return 0;
        }

        Path dir = Paths.get(exportPath);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path file = dir.resolve("traces_" + timestamp + ".json");

        List<TraceSpan> spans = new ArrayList<>();
        completedSpans.drainTo(spans);

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("{\n  \"spans\": [\n");
            for (int i = 0; i < spans.size(); i++) {
                TraceSpan span = spans.get(i);
                writer.write("    " + span.toOpenTelemetryJson());
                if (i < spans.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("  ]\n}\n");
        }

        LOGGER.info("Exported {} spans to JSON: {}", spans.size(), file);
        return spans.size();
    }

    /**
     * Exports all completed spans to CSV format.
     *
     * @param exportPath the directory to export to
     * @return the number of spans exported
     * @throws IOException if export fails
     */
    public int exportToCsv(String exportPath) throws IOException {
        if (!config.isCsvExportEnabled()) {
            LOGGER.warn("CSV export is disabled");
            return 0;
        }

        Path dir = Paths.get(exportPath);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        Path file = dir.resolve("traces_" + timestamp + ".csv");

        List<TraceSpan> spans = new ArrayList<>();
        completedSpans.drainTo(spans);

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            // Write header
            writer.write("trace_id,span_id,parent_id,name,kind,status,duration_ms,start_time\n");

            // Write rows
            for (TraceSpan span : spans) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%d,%s\n",
                    escapeCsv(span.getTraceId()),
                    escapeCsv(span.getSpanId()),
                    escapeCsv(span.getParentId()),
                    escapeCsv(span.getName()),
                    escapeCsv(span.getKind().name()),
                    escapeCsv(span.getStatus().name()),
                    span.getDurationMillis(),
                    escapeCsv(span.getStartTime().toString())
                ));
            }
        }

        LOGGER.info("Exported {} spans to CSV: {}", spans.size(), file);
        return spans.size();
    }

    /**
     * Gets the current statistics for all spans.
     *
     * @return a map of statistic names to values
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        stats.put("total_spans_created", totalSpansCreated.get());
        stats.put("total_spans_completed", totalSpansCompleted.get());
        stats.put("total_spans_errored", totalSpansErrored.get());
        stats.put("queued_spans", (long) completedSpans.size());

        // Add counts by kind
        for (Map.Entry<String, AtomicLong> entry : spansByKind.entrySet()) {
            stats.put("spans_kind_" + entry.getKey().toLowerCase(), entry.getValue().get());
        }

        // Add counts by name (top 10)
        spansByName.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(10)
            .forEach(entry -> stats.put("spans_name_" + entry.getKey().toLowerCase().replace(".", "_"), entry.getValue().get()));

        return stats;
    }

    /**
     * Clears all completed spans from the queue.
     *
     * @return the number of spans cleared
     */
    public int clearCompletedSpans() {
        List<TraceSpan> spans = new ArrayList<>();
        completedSpans.drainTo(spans);
        int count = spans.size();
        LOGGER.debug("Cleared {} completed spans", count);
        return count;
    }

    /**
     * Gets the number of completed spans currently queued.
     *
     * @return the queue size
     */
    public int getCompletedSpanCount() {
        return completedSpans.size();
    }

    /**
     * Gets the configuration for this tracing service.
     *
     * @return the configuration
     */
    public TracingConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
        LOGGER.info("TracingService closing - exporting remaining spans");
        try {
            int exported = exportToJson(config.getExportDirectory());
            LOGGER.info("Exported {} spans on shutdown", exported);
        } catch (IOException e) {
            LOGGER.error("Failed to export spans on shutdown", e);
        }
        clearCompletedSpans();
        THREAD_SPAN_STACK.remove();
    }

    /**
     * Extracts the provider name from a model string.
     *
     * @param model the model name
     * @return the provider name
     */
    private static String extractProvider(String model) {
        if (model == null) {
            return "unknown";
        }
        String lower = model.toLowerCase();
        if (lower.contains("gpt") || lower.contains("openai")) {
            return "openai";
        } else if (lower.contains("glm") || lower.contains("zai")) {
            return "zai";
        } else if (lower.contains("llama") || lower.contains("groq")) {
            return "groq";
        } else if (lower.contains("gemini")) {
            return "google";
        }
        return "unknown";
    }

    /**
     * Escapes a value for CSV format.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
