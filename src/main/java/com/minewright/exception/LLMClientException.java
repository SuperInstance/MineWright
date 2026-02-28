package com.minewright.exception;

import java.time.Duration;

/**
 * Exception thrown when LLM client operations fail.
 *
 * <p>LLM client exceptions can occur due to:
 * <ul>
 *   <li>Authentication failures (invalid API keys)</li>
 *   <li>Rate limiting (too many requests)</li>
 *   <li>Network errors (connection issues)</li>
 *   <li>Timeouts (provider not responding)</li>
 *   <li>Invalid responses (malformed JSON, missing fields)</li>
 *   <li>Server errors (provider issues)</li>
 * </ul>
 *
 * <p><b>Recovery:</b> LLM errors can often be recovered by:
 * <ul>
 *   <li>Retrying with exponential backoff (for rate limits and server errors)</li>
 *   <li>Switching to a different provider (fallback)</li>
 *   <li>Fixing the API key or configuration</li>
 *   <li>Reducing request frequency</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class LLMClientException extends MineWrightException {
    private static final long serialVersionUID = 1L;

    private final String provider;
    private final int statusCode;
    private final boolean retryable;
    private final Duration retryAfter;

    /**
     * Constructs a new LLMClientException.
     *
     * @param message            Error message
     * @param provider           The LLM provider (openai, groq, gemini)
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param retryable          Whether the request can be retried
     */
    public LLMClientException(String message, String provider, ErrorCode errorCode,
                             String recoverySuggestion, boolean retryable) {
        super(message, errorCode, recoverySuggestion);
        this.provider = provider;
        this.statusCode = -1;
        this.retryable = retryable;
        this.retryAfter = null;
    }

    /**
     * Constructs a new LLMClientException with HTTP status code.
     *
     * @param message            Error message
     * @param provider           The LLM provider
     * @param statusCode         HTTP status code
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param retryable          Whether the request can be retried
     */
    public LLMClientException(String message, String provider, int statusCode,
                             ErrorCode errorCode, String recoverySuggestion, boolean retryable) {
        super(message, errorCode, recoverySuggestion);
        this.provider = provider;
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.retryAfter = null;
    }

    /**
     * Constructs a new LLMClientException with full details.
     *
     * @param message            Error message
     * @param provider           The LLM provider
     * @param statusCode         HTTP status code (-1 if not applicable)
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param retryable          Whether the request can be retried
     * @param retryAfter         Suggested retry delay (null if not specified)
     * @param cause              Underlying cause
     */
    public LLMClientException(String message, String provider, int statusCode,
                             ErrorCode errorCode, String recoverySuggestion,
                             boolean retryable, Duration retryAfter, Throwable cause) {
        super(message, errorCode, recoverySuggestion, "Provider: " + provider, cause);
        this.provider = provider;
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the LLM provider.
     *
     * @return Provider name
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return Status code, or -1 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns whether this request is retryable.
     *
     * @return true if retry is recommended
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Returns the suggested retry delay.
     *
     * @return Retry delay, or null if not specified
     */
    public Duration getRetryAfter() {
        return retryAfter;
    }

    // Static factory methods for common errors

    /**
     * Creates an exception for authentication errors.
     *
     * @param provider The provider
     * @return LLMClientException instance
     */
    public static LLMClientException authenticationFailed(String provider) {
        return new LLMClientException(
            "Authentication failed for " + provider + ". Check your API key.",
            provider,
            401,
            ErrorCode.LLM_AUTH_ERROR,
            "Your " + provider.toUpperCase() + " API key is invalid or expired. " +
            "Please update the apiKey in config/minewright-common.toml and use /reload.",
            false
        );
    }

    /**
     * Creates an exception for rate limit errors.
     *
     * @param provider   The provider
     * @param retryAfter Suggested retry delay
     * @return LLMClientException instance
     */
    public static LLMClientException rateLimited(String provider, Duration retryAfter) {
        String suggestion = "Rate limit exceeded for " + provider.toUpperCase() + ". ";
        if (retryAfter != null) {
            long seconds = retryAfter.getSeconds();
            suggestion += "Wait " + seconds + " seconds before retrying, or switch to a different provider. ";
        } else {
            suggestion += "Wait a minute before retrying, or switch to a different provider. ";
        }
        suggestion += "Consider upgrading your API tier for higher limits.";

        return new LLMClientException(
            "Rate limit exceeded for " + provider,
            provider,
            429,
            ErrorCode.LLM_RATE_LIMIT,
            suggestion,
            true,
            retryAfter,
            null
        );
    }

    /**
     * Creates an exception for timeout errors.
     *
     * @param provider  The provider
     * @param timeoutMs The timeout duration
     * @param cause     Underlying cause
     * @return LLMClientException instance
     */
    public static LLMClientException timeout(String provider, long timeoutMs, Throwable cause) {
        return new LLMClientException(
            "Request to " + provider + " timed out after " + timeoutMs + "ms",
            provider,
            -1,
            ErrorCode.LLM_TIMEOUT,
            "The " + provider.toUpperCase() + " API did not respond in time. " +
            "This could be due to network issues or high load on the provider. " +
            "Try again or switch to a different provider.",
            true,
            null,
            cause
        );
    }

    /**
     * Creates an exception for invalid responses.
     *
     * @param provider The provider
     * @param reason   Why the response is invalid
     * @return LLMClientException instance
     */
    public static LLMClientException invalidResponse(String provider, String reason) {
        return new LLMClientException(
            "Invalid response from " + provider + ": " + reason,
            provider,
            -1,
            ErrorCode.LLM_INVALID_RESPONSE,
            "The " + provider.toUpperCase() + " API returned an unexpected response. " +
            "This could indicate a change in the API or a temporary issue. " +
            "Try again or switch to a different provider. " +
            "If the issue persists, please report it as a bug.",
            true
        );
    }

    /**
     * Creates an exception for network errors.
     *
     * @param provider The provider
     * @param cause    Underlying cause
     * @return LLMClientException instance
     */
    public static LLMClientException networkError(String provider, Throwable cause) {
        return new LLMClientException(
            "Network error communicating with " + provider,
            provider,
            -1,
            ErrorCode.LLM_NETWORK_ERROR,
            "Could not reach the " + provider.toUpperCase() + " API. " +
            "Check your internet connection and try again. " +
            "If the issue persists, the provider may be experiencing downtime.",
            true,
            null,
            cause
        );
    }

    /**
     * Creates an exception for server errors.
     *
     * @param provider   The provider
     * @param statusCode HTTP status code (5xx)
     * @return LLMClientException instance
     */
    public static LLMClientException serverError(String provider, int statusCode) {
        return new LLMClientException(
            "Server error from " + provider + ": HTTP " + statusCode,
            provider,
            statusCode,
            ErrorCode.LLM_PROVIDER_ERROR,
            "The " + provider.toUpperCase() + " API is experiencing issues. " +
            "This is a temporary problem on their end. " +
            "Retry the request in a few moments, or switch to a different provider.",
            true
        );
    }

    /**
     * Creates an exception for configuration errors.
     *
     * @param provider The provider
     * @param issue    The configuration issue
     * @return LLMClientException instance
     */
    public static LLMClientException configurationError(String provider, String issue) {
        return new LLMClientException(
            "Configuration error for " + provider + ": " + issue,
            provider,
            -1,
            ErrorCode.LLM_CONFIG_ERROR,
            "Fix the configuration issue in config/minewright-common.toml and use /reload.",
            false
        );
    }

    @Override
    public String toString() {
        return String.format("LLMClientException[provider='%s', statusCode=%d, retryable=%s, code=%d, message='%s']",
            provider, statusCode, retryable, getErrorCode().getCode(), getMessage());
    }
}
