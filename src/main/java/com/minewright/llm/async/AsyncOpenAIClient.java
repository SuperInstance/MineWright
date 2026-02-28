package com.minewright.llm.async;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous OpenAI API client using Java HttpClient's sendAsync().
 *
 * <p>Provides non-blocking calls to OpenAI's chat completion API with built-in retry logic.
 * Uses CompletableFuture to return immediately without blocking the calling thread.</p>
 *
 * <p><b>API Endpoint:</b> https://api.openai.com/v1/chat/completions</p>
 *
 * <p><b>Supported Models:</b></p>
 * <ul>
 *   <li>gpt-4o (recommended)</li>
 *   <li>gpt-4-turbo</li>
 *   <li>gpt-4</li>
 *   <li>gpt-3.5-turbo</li>
 * </ul>
 *
 * <p><b>Retry Behavior:</b></p>
 * <ul>
 *   <li>Max retries: 3</li>
 *   <li>Retryable errors: HTTP 429 (rate limit), HTTP 5xx (server errors), network failures</li>
 *   <li>Backoff strategy: Exponential (1s, 2s, 4s)</li>
 *   <li>Non-retryable errors: HTTP 4xx client errors (except 429)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Thread-safe. HttpClient is thread-safe and immutable.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * AsyncOpenAIClient client = new AsyncOpenAIClient(apiKey, "gpt-4o", 1000, 0.7);
 *
 * client.sendAsync("Plan tasks for mining iron", Map.of())
 *     .thenAccept(response -> {
 *         System.out.println("Response: " + response.getContent());
 *     })
 *     .exceptionally(error -> {
 *         System.err.println("Error: " + error.getMessage());
 *         return null;
 *     });
 * </pre>
 *
 * @since 1.1.0
 */
public class AsyncOpenAIClient implements AsyncLLMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncOpenAIClient.class);
    private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
    private static final String PROVIDER_ID = "zai";

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private static final java.util.concurrent.ScheduledExecutorService RETRY_SCHEDULER =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "openai-retry-scheduler");
            t.setDaemon(true);
            return t;
        });

    /**
     * Shuts down the retry scheduler.
     * Should be called when the application is shutting down.
     */
    public static void shutdown() {
        RETRY_SCHEDULER.shutdown();
        try {
            if (!RETRY_SCHEDULER.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                RETRY_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            RETRY_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    /**
     * Constructs an AsyncOpenAIClient.
     *
     * @param apiKey      OpenAI API key (required)
     * @param model       Model to use (e.g., "gpt-4o", "gpt-3.5-turbo")
     * @param maxTokens   Maximum tokens in response (e.g., 1000)
     * @param temperature Response randomness (0.0 - 2.0, lower = more deterministic)
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public AsyncOpenAIClient(String apiKey, String model, int maxTokens, double temperature) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key cannot be null or empty");
        }

        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        LOGGER.info("AsyncOpenAIClient initialized (model: {}, maxTokens: {}, temperature: {})",
            model, maxTokens, temperature);
    }

    @Override
    public CompletableFuture<LLMResponse> sendAsync(String prompt, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();

        // Build request body
        String requestBody = buildRequestBody(prompt, params);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENAI_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(30))
            .build();

        LOGGER.debug("[openai] Sending async request (prompt length: {} chars)", prompt.length());

        return sendWithRetry(request, startTime, 0);
    }

    /**
     * Sends the HTTP request with retry logic and exponential backoff.
     *
     * <p>Retries are performed for:</p>
     * <ul>
     *   <li>HTTP 429 (rate limit errors)</li>
     *   <li>HTTP 5xx (server errors)</li>
     *   <li>Network failures (timeouts, connection errors)</li>
     * </ul>
     *
     * <p>Backoff is exponential: 1s, 2s, 4s (up to MAX_RETRIES)</p>
     *
     * @param request     The HTTP request to send
     * @param startTime   Request start time (for latency measurement)
     * @param retryCount  Current retry attempt (0-based)
     * @return CompletableFuture with the LLM response
     */
    private CompletableFuture<LLMResponse> sendWithRetry(HttpRequest request, long startTime, int retryCount) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                long latencyMs = System.currentTimeMillis() - startTime;

                if (response.statusCode() == 200) {
                    return parseResponse(response.body(), latencyMs);
                }

                // Check if error is retryable (429 rate limit or 5xx server errors)
                boolean isRetryable = response.statusCode() == 429 || response.statusCode() >= 500;

                if (isRetryable && retryCount < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << retryCount);
                    int nextRetryCount = retryCount + 1;

                    LOGGER.warn("[openai] Retryable error (status={}, attempt {}/{}), " +
                        "retrying in {}ms: body={}",
                        response.statusCode(), nextRetryCount, MAX_RETRIES, backoffMs,
                        truncate(response.body(), 200));

                    // This exception will be caught and retried
                    throw new RetryableException(
                        "Retryable HTTP error: " + response.statusCode(),
                        backoffMs,
                        nextRetryCount
                    );
                }

                // Non-retryable error or max retries exceeded
                LLMException.ErrorType errorType = determineErrorType(response.statusCode());

                LOGGER.error("[openai] API error: status={}, retryable={}, attempts={}/{}, body={}",
                    response.statusCode(), isRetryable, retryCount, MAX_RETRIES,
                    truncate(response.body(), 200));

                throw new LLMException(
                    "OpenAI API error: HTTP " + response.statusCode() +
                        (retryCount > 0 ? " (after " + retryCount + " retry attempts)" : ""),
                    errorType,
                    PROVIDER_ID,
                    isRetryable
                );
            })
            .exceptionallyCompose(error -> {
                Throwable cause = error instanceof java.util.concurrent.CompletionException ?
                    error.getCause() : error;

                // Handle retryable exceptions
                if (cause instanceof RetryableException) {
                    RetryableException re = (RetryableException) cause;
                    return scheduleRetry(request, startTime, re.backoffMs, re.nextRetryCount);
                }

                // Handle network/IO errors (these are retryable)
                if (isNetworkError(cause) && retryCount < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << retryCount);
                    int nextRetryCount = retryCount + 1;

                    LOGGER.warn("[openai] Network error (attempt {}/{}), retrying in {}ms: {}",
                        nextRetryCount, MAX_RETRIES, backoffMs, cause.getMessage());

                    return scheduleRetry(request, startTime, backoffMs, nextRetryCount);
                }

                // Re-throw LLMException wrapped in failed future
                if (cause instanceof LLMException) {
                    return CompletableFuture.failedFuture(cause);
                }

                // Unknown error after all retries
                LOGGER.error("[openai] Request failed after {} attempts: {}", retryCount, cause.getMessage());
                return CompletableFuture.failedFuture(
                    new LLMException(
                        "OpenAI request failed after " + retryCount + " attempts: " + cause.getMessage(),
                        LLMException.ErrorType.NETWORK_ERROR,
                        PROVIDER_ID,
                        true,
                        cause
                    )
                );
            });
    }

    /**
     * Schedules a retry attempt after a backoff delay.
     */
    private CompletableFuture<LLMResponse> scheduleRetry(HttpRequest request, long startTime,
                                                          long backoffMs, int nextRetryCount) {
        CompletableFuture<LLMResponse> future = new CompletableFuture<>();
        RETRY_SCHEDULER.schedule(() -> {
            sendWithRetry(request, startTime, nextRetryCount)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(result);
                    }
                });
        }, backoffMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        return future;
    }

    /**
     * Determines if an exception represents a network error that should be retried.
     */
    private boolean isNetworkError(Throwable error) {
        return error instanceof java.net.ConnectException ||
               error instanceof java.net.SocketTimeoutException ||
               error instanceof java.io.InterruptedIOException ||
               (error instanceof java.net.http.HttpTimeoutException);
    }

    /**
     * Internal exception used to signal that a request should be retried.
     */
    private static class RetryableException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final long backoffMs;
        final int nextRetryCount;

        RetryableException(String message, long backoffMs, int nextRetryCount) {
            super(message);
            this.backoffMs = backoffMs;
            this.nextRetryCount = nextRetryCount;
        }
    }

    /**
     * Builds the JSON request body for OpenAI API.
     *
     * @param prompt User prompt
     * @param params Additional parameters (can override defaults)
     * @return JSON string
     */
    private String buildRequestBody(String prompt, Map<String, Object> params) {
        JsonObject body = new JsonObject();

        // Use params if provided, otherwise use instance defaults
        String modelToUse = (String) params.getOrDefault("model", this.model);
        int maxTokensToUse = (int) params.getOrDefault("maxTokens", this.maxTokens);
        double tempToUse = (double) params.getOrDefault("temperature", this.temperature);

        body.addProperty("model", modelToUse);
        body.addProperty("max_tokens", maxTokensToUse);
        body.addProperty("temperature", tempToUse);

        // Build messages array
        JsonArray messages = new JsonArray();

        // System message (if provided)
        String systemPrompt = (String) params.get("systemPrompt");
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);
        }

        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        body.add("messages", messages);

        return body.toString();
    }

    /**
     * Parses the OpenAI API response.
     *
     * @param responseBody Raw JSON response
     * @param latencyMs    Request latency
     * @return Parsed LLMResponse
     * @throws LLMException if response cannot be parsed
     */
    private LLMResponse parseResponse(String responseBody, long latencyMs) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // Extract content from choices[0].message.content
            if (!json.has("choices") || json.getAsJsonArray("choices").isEmpty()) {
                throw new LLMException(
                    "OpenAI response missing 'choices' array",
                    LLMException.ErrorType.INVALID_RESPONSE,
                    PROVIDER_ID,
                    false
                );
            }

            JsonObject firstChoice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            String content = message.get("content").getAsString();

            // Extract token usage
            int tokensUsed = 0;
            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                tokensUsed = usage.get("total_tokens").getAsInt();
            }

            LOGGER.debug("[openai] Response received (latency: {}ms, tokens: {})", latencyMs, tokensUsed);

            return LLMResponse.builder()
                .content(content)
                .model(model)
                .providerId(PROVIDER_ID)
                .latencyMs(latencyMs)
                .tokensUsed(tokensUsed)
                .fromCache(false)
                .build();

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("[openai] Failed to parse response: {}", truncate(responseBody, 200), e);
            throw new LLMException(
                "Failed to parse OpenAI response: " + e.getMessage(),
                LLMException.ErrorType.INVALID_RESPONSE,
                PROVIDER_ID,
                false,
                e
            );
        }
    }

    /**
     * Determines the error type based on HTTP status code.
     *
     * @param statusCode HTTP status code
     * @return Corresponding ErrorType
     */
    private LLMException.ErrorType determineErrorType(int statusCode) {
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
     * @return Truncated string
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "[null]";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isHealthy() {
        // Client includes retry logic for transient failures
        return true;
    }
}
