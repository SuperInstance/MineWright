package com.minewright.observability.span;

/**
 * The kind of span, describing the relationship between the span and its trace.
 */
public enum SpanKind {
    /**
     * Indicates that the span is a root span or represents an internal operation.
     */
    INTERNAL,

    /**
     * Indicates that the span describes a synchronous remote call to another service.
     */
    CLIENT,

    /**
     * Indicates that the span describes a synchronous remote call from another service.
     */
    SERVER,

    /**
     * Indicates that the span describes a call to an external service, typically a database or message queue.
     */
    PRODUCER,

    /**
     * Indicates that the span describes a call from an external service, typically a database or message queue.
     */
    CONSUMER
}
