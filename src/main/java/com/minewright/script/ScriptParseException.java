package com.minewright.script;

/**
 * Exception thrown when script parsing fails.
 *
 * @since 1.3.0
 */
public class ScriptParseException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new parse exception.
     *
     * @param message The error message
     */
    public ScriptParseException(String message) {
        super(message);
    }

    /**
     * Creates a new parse exception with a cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public ScriptParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
