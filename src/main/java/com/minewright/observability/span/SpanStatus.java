package com.minewright.observability.span;

/**
 * The status of a span execution.
 */
public enum SpanStatus {
    /**
     * The span completed successfully.
     */
    OK,

    /**
     * The span contains an error.
     */
    ERROR,

    /**
     * The span was cancelled before completion.
     */
    CANCELLED,

    /**
     * The span is still in progress.
     */
    UNSET
}
