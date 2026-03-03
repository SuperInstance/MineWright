package com.minewright.observability.span;

import java.util.Objects;

/**
 * Context for span propagation across thread and process boundaries.
 * Contains the trace ID, span ID, and other metadata needed to continue a trace.
 */
public class SpanContext {
    private final String traceId;
    private final String spanId;
    private final String parentId;
    private final String traceState;

    /**
     * Creates a new span context.
     *
     * @param traceId the trace ID (16-byte hex string)
     * @param spanId the span ID (8-byte hex string)
     * @param parentId the parent span ID, or null if root
     * @param traceState additional trace state for vendor-specific data
     */
    public SpanContext(String traceId, String spanId, String parentId, String traceState) {
        this.traceId = Objects.requireNonNull(traceId, "traceId cannot be null");
        this.spanId = Objects.requireNonNull(spanId, "spanId cannot be null");
        this.parentId = parentId;
        this.traceState = traceState;
    }

    /**
     * Creates a new span context without trace state.
     */
    public SpanContext(String traceId, String spanId, String parentId) {
        this(traceId, spanId, parentId, null);
    }

    /**
     * Gets the trace ID.
     *
     * @return the 16-byte hex trace ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Gets the span ID.
     *
     * @return the 8-byte hex span ID
     */
    public String getSpanId() {
        return spanId;
    }

    /**
     * Gets the parent span ID.
     *
     * @return the parent span ID, or null if this is a root span
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Gets the trace state for vendor-specific data.
     *
     * @return the trace state, or null if not set
     */
    public String getTraceState() {
        return traceState;
    }

    /**
     * Checks if this context is valid (has non-null IDs).
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return traceId != null && !traceId.isEmpty()
            && spanId != null && !spanId.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpanContext that = (SpanContext) o;
        return Objects.equals(traceId, that.traceId)
            && Objects.equals(spanId, that.spanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId);
    }

    @Override
    public String toString() {
        return "SpanContext{" +
            "traceId='" + traceId + '\'' +
            ", spanId='" + spanId + '\'' +
            ", parentId='" + parentId + '\'' +
            '}';
    }
}
