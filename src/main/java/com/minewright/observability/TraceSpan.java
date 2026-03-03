package com.minewright.observability;

import com.minewright.observability.span.SpanContext;
import com.minewright.observability.span.SpanKind;
import com.minewright.observability.span.SpanStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A distributed tracing span representing a unit of work in the system.
 * Spans form a tree structure through parent-child relationships and can be exported
 * in OpenTelemetry format for analysis.
 */
public class TraceSpan {
    private final String spanId;
    private final String traceId;
    private final String parentId;
    private final String name;
    private final SpanKind kind;
    private final long startEpochNanos;
    private long endEpochNanos;
    private final Map<String, Object> attributes;
    private final List<SpanEvent> events;
    private SpanStatus status;
    private String statusDescription;
    private Throwable exception;
    private final Instant startTime;

    /**
     * Creates a new root span.
     *
     * @param name the span name
     * @param kind the kind of span
     */
    public TraceSpan(String name, SpanKind kind) {
        this(name, kind, (TraceSpan) null);
    }

    /**
     * Creates a new child span.
     *
     * @param name the span name
     * @param kind the kind of span
     * @param parent the parent span
     */
    public TraceSpan(String name, SpanKind kind, TraceSpan parent) {
        this.name = name;
        this.kind = kind;
        this.spanId = generateSpanId();
        this.traceId = parent != null ? parent.traceId : generateTraceId();
        this.parentId = parent != null ? parent.spanId : null;
        this.startEpochNanos = System.nanoTime();
        this.startTime = Instant.now();
        this.attributes = new ConcurrentHashMap<>();
        this.events = new ArrayList<>();
        this.status = SpanStatus.UNSET;
    }

    /**
     * Creates a new span from an existing context.
     *
     * @param name the span name
     * @param kind the kind of span
     * @param context the span context
     */
    public TraceSpan(String name, SpanKind kind, SpanContext context) {
        this.name = name;
        this.kind = kind;
        this.spanId = generateSpanId();
        this.traceId = context.getTraceId();
        this.parentId = context.getParentId();
        this.startEpochNanos = System.nanoTime();
        this.startTime = Instant.now();
        this.attributes = new ConcurrentHashMap<>();
        this.events = new ArrayList<>();
        this.status = SpanStatus.UNSET;
    }

    /**
     * Generates a random 8-byte hex span ID.
     */
    private static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Generates a random 16-byte hex trace ID.
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Records an event on this span with a timestamp.
     *
     * @param name the event name
     * @param attributes the event attributes
     */
    public void addEvent(String name, Map<String, Object> attributes) {
        events.add(new SpanEvent(name, System.nanoTime(), attributes));
    }

    /**
     * Records an event on this span.
     *
     * @param name the event name
     */
    public void addEvent(String name) {
        addEvent(name, Collections.emptyMap());
    }

    /**
     * Sets an attribute on this span.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    /**
     * Sets an attribute on this span.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, long value) {
        attributes.put(key, value);
    }

    /**
     * Sets an attribute on this span.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, double value) {
        attributes.put(key, value);
    }

    /**
     * Sets an attribute on this span.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, boolean value) {
        attributes.put(key, value);
    }

    /**
     * Records an exception on this span and sets status to ERROR.
     *
     * @param throwable the exception
     */
    public void recordException(Throwable throwable) {
        this.exception = throwable;
        this.status = SpanStatus.ERROR;
        this.statusDescription = throwable.getMessage();

        // Add exception as an event
        Map<String, Object> exceptionAttrs = new HashMap<>();
        exceptionAttrs.put("exception.type", throwable.getClass().getName());
        exceptionAttrs.put("exception.message", throwable.getMessage());
        exceptionAttrs.put("exception.stacktrace", getStackTrace(throwable));
        addEvent("exception", exceptionAttrs);
    }

    /**
     * Marks the span as completed.
     */
    public void end() {
        end(SpanStatus.OK);
    }

    /**
     * Marks the span as completed with a status.
     *
     * @param status the completion status
     */
    public void end(SpanStatus status) {
        if (endEpochNanos == 0) {
            this.endEpochNanos = System.nanoTime();
            this.status = status;
        }
    }

    /**
     * Gets the duration of the span in nanoseconds.
     * Returns 0 if the span has not ended.
     *
     * @return the duration in nanoseconds
     */
    public long getDurationNanos() {
        return endEpochNanos > 0 ? endEpochNanos - startEpochNanos : 0;
    }

    /**
     * Gets the duration of the span in milliseconds.
     * Returns 0 if the span has not ended.
     *
     * @return the duration in milliseconds
     */
    public long getDurationMillis() {
        return getDurationNanos() / 1_000_000;
    }

    /**
     * Converts this span to OpenTelemetry JSON format.
     *
     * @return JSON string representation
     */
    public String toOpenTelemetryJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(name)).append("\",");
        sb.append("\"kind\":\"").append(kind.name()).append("\",");
        sb.append("\"traceId\":\"").append(traceId).append("\",");
        sb.append("\"spanId\":\"").append(spanId).append("\",");
        if (parentId != null) {
            sb.append("\"parentSpanId\":\"").append(parentId).append("\",");
        }
        sb.append("\"startTimeUnixNano\":").append(startTime.toEpochMilli() * 1_000_000).append(",");
        sb.append("\"endTimeUnixNano\":").append(endEpochNanos > 0 ? (startTime.toEpochMilli() + getDurationMillis()) * 1_000_000 : 0).append(",");
        sb.append("\"status\":{");
        sb.append("\"code\":\"").append(status.name()).append("\"");
        if (statusDescription != null) {
            sb.append(",\"description\":\"").append(escapeJson(statusDescription)).append("\"");
        }
        sb.append("},");
        sb.append("\"attributes\":{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("},");
        sb.append("\"events\":[");
        first = true;
        for (SpanEvent event : events) {
            if (!first) sb.append(",");
            first = false;
            sb.append(event.toJson());
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Gets a formatted stack trace string.
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in JSON strings.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    // Getters

    public String getSpanId() { return spanId; }
    public String getTraceId() { return traceId; }
    public String getParentId() { return parentId; }
    public String getName() { return name; }
    public SpanKind getKind() { return kind; }
    public long getStartEpochNanos() { return startEpochNanos; }
    public long getEndEpochNanos() { return endEpochNanos; }
    public Instant getStartTime() { return startTime; }
    public Map<String, Object> getAttributes() { return new HashMap<>(attributes); }
    public List<SpanEvent> getEvents() { return new ArrayList<>(events); }
    public SpanStatus getStatus() { return status; }
    public String getStatusDescription() { return statusDescription; }
    public Throwable getException() { return exception; }

    /**
     * Represents an event that occurred during the span.
     */
    public static class SpanEvent {
        private final String name;
        private final long timestampNanos;
        private final Map<String, Object> attributes;

        public SpanEvent(String name, long timestampNanos, Map<String, Object> attributes) {
            this.name = name;
            this.timestampNanos = timestampNanos;
            this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"name\":\"").append(escapeJson(name)).append("\",");
            sb.append("\"timeUnixNano\":").append(timestampNanos).append(",");
            sb.append("\"attributes\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(escapeJson((String) value)).append("\"");
                } else {
                    sb.append(value);
                }
            }
            sb.append("}");
            sb.append("}");
            return sb.toString();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        }

        public String getName() { return name; }
        public long getTimestampNanos() { return timestampNanos; }
        public Map<String, Object> getAttributes() { return new HashMap<>(attributes); }
    }

    public SpanContext toContext() {
        return new SpanContext(traceId, spanId, parentId);
    }
}
