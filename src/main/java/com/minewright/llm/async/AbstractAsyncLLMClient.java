package com.minewright.llm.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Abstract base class for async LLM clients.
 * <p>
 * Provides common HTTP client setup, request building, response parsing, and error handling.
 * Subclasses only need to implement provider-specific details:
 * <ul>
 *   <li>API endpoint URL</li>
 *   <li>Request body format (JSON structure)</li>
 *   <li>Response parsing (content extraction)</li>
 *   <li>Authentication method (if non-standard)</li>
 * </ul>
 * <p>
 * <b>Template Method Pattern:</b> The {@link #sendAsync(String, Map)} method defines the
 * skeleton of the HTTP request workflow, while abstract methods handle provider-specific details.
 * <p>
 * <b>Common Functionality:</b>
 * <ul>
 *   <li>HttpClient initialization with configurable timeout</li>
 *   <li>HTTP request building (headers, body, timeout)</li>
 *   <li>Timeout handling with overall request deadline</li>
 *   <li>Exception handling and error type mapping</li>
 *   <li>Latency measurement</li>
 *   <li>Request/response logging</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> Thread-safe. HttpClient is thread-safe and immutable.
 * Subclasses must ensure their implementation is also thread-safe.
 *
 * @since 1.2.0
 */
public abstract class AbstractAsyncLLMClient implements AsyncLLMClient {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Default HTTP connection timeout in seconds.
     */
    protected static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;

    /**
     * Default HTTP request timeout in seconds.
     */
    protected static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 30;

    /**
     * Maximum overall timeout for LLM requests including retries.
     * Prevents requests from hanging indefinitely.
     */
    protected final long overallTimeoutMs;

    /**
     * HTTP client for sending requests.
     * Thread-safe and immutable.
     */
    protected final HttpClient httpClient;

    /**
     * API key for authentication.
     */
    protected final String apiKey;

    /**
     * Model name to use for requests.
     */
    protected final String model;

    /**
     * Maximum tokens in response.
     */
    protected final int maxTokens;

    /**
     * Response randomness (0.0 - 2.0).
     */
    protected final double temperature;

    /**
     * Provider ID for logging and error reporting.
     */
    protected final String providerId;

    /**
     * Constructs an AbstractAsyncLLMClient.
     *
     * @param apiKey           API key for authentication (required)
     * @param model            Model name (e.g., "gpt-4o", "llama-3.1-8b-instant")
     * @param maxTokens        Maximum tokens in response
     * @param temperature      Response randomness (0.0 - 2.0)
     * @param providerId       Provider ID for logging (e.g., "openai", "groq", "gemini")
     * @param overallTimeoutMs Overall timeout for requests including retries
     * @param requestTimeout   HTTP request timeout in seconds
     * @throws IllegalArgumentException if apiKey or providerId is null/empty
     */
    protected AbstractAsyncLLMClient(
            String apiKey,
            String model,
            int maxTokens,
            double temperature,
            String providerId,
            long overallTimeoutMs,
            int requestTimeout) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(providerId + " API key cannot be null or empty");
        }
        if (providerId == null || providerId.isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be null or empty");
        }

        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.providerId = providerId;
        this.overallTimeoutMs = overallTimeoutMs;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS))
                .build();

        logger.info("{} initialized (model: {}, maxTokens: {}, temperature: {})",
                getClass().getSimpleName(), model, maxTokens, temperature);
    }

    /**
     * Returns the API endpoint URL for this provider.
     * <p>
     * Subclasses must implement to provide the full URL.
     *
     * @return Full API endpoint URL (e.g., "https://api.openai.com/v1/chat/completions")
     */
    protected abstract String getApiEndpoint();

    /**
     * Builds the provider-specific request body.
     * <p>
     * Subclasses must implement to create the JSON structure required by their provider.
     * Different providers use different formats:
     * <ul>
     *   <li>OpenAI/Groq: messages array with role/content</li>
     *   <li>Gemini: contents array with parts array</li>
     * </ul>
     *
     * @param prompt The user prompt
     * @param params Additional parameters (may include systemPrompt, model, maxTokens, temperature)
     * @return JSON string for request body
     */
    protected abstract String buildRequestBody(String prompt, Map<String, Object> params);

    /**
     * Parses the provider-specific response.
     * <p>
     * Subclasses must implement to extract content from their provider's response format.
     * Different providers use different structures:
     * <ul>
     *   <li>OpenAI/Groq: choices[0].message.content</li>
     *   <li>Gemini: candidates[0].content.parts[0].text</li>
     * </ul>
     *
     * @param responseBody Raw JSON response body
     * @param latencyMs    Request latency in milliseconds
     * @return Parsed LLMResponse
     * @throws LLMException if response cannot be parsed
     */
    protected abstract LLMResponse parseResponse(String responseBody, long latencyMs);

    /**
     * Builds authentication headers for the request.
     * <p>
     * Default implementation adds Bearer token authorization.
     * Subclasses can override for different auth methods (e.g., Gemini uses query param).
     *
     * @param builder HttpRequest.Builder to add headers to
     */
    protected void addAuthHeaders(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + apiKey);
    }

    /**
     * Gets the HTTP request timeout in seconds.
     * <p>
     * Subclasses can override for provider-specific timeouts.
     *
     * @return Timeout in seconds (default: 30)
     */
    protected int getRequestTimeout() {
        return DEFAULT_REQUEST_TIMEOUT_SECONDS;
    }

    /**
     * Template method: sends async request to LLM provider.
     * <p>
     * This method implements the common workflow:
     * <ol>
     *   <li>Build request body (provider-specific)</li>
     *   <li>Create HTTP request with headers</li>
     *   <li>Send request asynchronously</li>
     *   <li>Parse response (provider-specific)</li>
     *   <li>Handle errors and timeouts</li>
     * </ol>
     *
     * @param prompt The text prompt to send
     * @param params Additional parameters
     * @return CompletableFuture with the LLM response
     */
    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();

        String requestBody = buildRequestBody(prompt, params);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(getApiEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(getRequestTimeout()));

        addAuthHeaders(requestBuilder);

        HttpRequest request = requestBuilder.build();

        logger.debug("[{}] Sending async request (prompt length: {} chars)",
                providerId, prompt.length());

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long latencyMs = System.currentTimeMillis() - startTime;

                    if (response.statusCode() != 200) {
                        handleErrorResponse(response);
                    }

                    return parseResponse(response.body(), latencyMs);
                })
                .orTimeout(overallTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> handleException(throwable));
    }

    /**
     * Handles HTTP error responses.
     * <p>
     * Determines error type and throws appropriate LLMException.
     *
     * @param response HTTP response with error status code
     * @throws LLMException always
     */
    protected void handleErrorResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        LLMException.ErrorType errorType = determineErrorType(statusCode);
        boolean retryable = statusCode == 429 || statusCode >= 500;

        logger.error("[{}] API error: status={}, body={}",
                providerId, statusCode, truncate(response.body(), 200));

        throw new LLMException(
                providerId + " API error: HTTP " + statusCode,
                errorType,
                providerId,
                retryable
        );
    }

    /**
     * Handles exceptions from the async request.
     * <p>
     * Converts TimeoutException and other exceptions to appropriate exceptions.
     *
     * @param throwable The exception thrown
     * @return A dummy response (never actually returned, always throws)
     * @throws LLMException always (wrapped in RuntimeException if needed)
     */
    @SuppressWarnings("unchecked")
    protected LLMResponse handleException(Throwable throwable) {
        Throwable cause = throwable instanceof java.util.concurrent.CompletionException
                ? throwable.getCause()
                : throwable;

        if (cause instanceof java.util.concurrent.TimeoutException) {
            logger.error("[{}] Overall timeout exceeded ({}ms) - request cancelled",
                    providerId, overallTimeoutMs);
            throw new LLMException(
                    "Request timeout: exceeded " + overallTimeoutMs + "ms",
                    LLMException.ErrorType.TIMEOUT,
                    providerId,
                    true
            );
        }

        if (cause instanceof LLMException) {
            throw (LLMException) cause;
        }

        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }

        throw new LLMException(
                "Request failed: " + cause.getMessage(),
                LLMException.ErrorType.NETWORK_ERROR,
                providerId,
                true,
                cause
        );
    }

    /**
     * Determines the error type based on HTTP status code.
     * <p>
     * Maps standard HTTP status codes to LLMException.ErrorType values.
     *
     * @param statusCode HTTP status code
     * @return Corresponding ErrorType
     */
    protected LLMException.ErrorType determineErrorType(int statusCode) {
        return switch (statusCode) {
            case 429 -> LLMException.ErrorType.RATE_LIMIT;
            case 401, 403 -> LLMException.ErrorType.AUTH_ERROR;
            case 400 -> LLMException.ErrorType.CLIENT_ERROR;
            case 408 -> LLMException.ErrorType.TIMEOUT;
            default -> {
                if (statusCode >= 500) {
                    yield LLMException.ErrorType.SERVER_ERROR;
                }
                yield LLMException.ErrorType.CLIENT_ERROR;
            }
        };
    }

    /**
     * Truncates a string for logging.
     *
     * @param str       String to truncate
     * @param maxLength Maximum length
     * @return Truncated string with "..." if truncated
     */
    protected String truncate(String str, int maxLength) {
        if (str == null) return "[null]";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
