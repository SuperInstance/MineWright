package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.config.MineWrightConfig;
import com.minewright.exception.LLMClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for OpenAI API (and compatible endpoints like z.ai).
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Throws {@link LLMClientException} for all failures</li>
 *   <li>Automatic retry with exponential backoff for retryable errors</li>
 *   <li>Detailed error messages with recovery suggestions</li>
 *   <li>Proper handling of rate limits, timeouts, and network errors</li>
 * </ul>
 *
 * <p><b>Retry Behavior:</b>
 * <ul>
 *   <li>Rate limits (429): Retry up to 5 times with exponential backoff</li>
 *   <li>Server errors (5xx): Retry up to 5 times</li>
 *   <li>Network errors: Retry up to 5 times</li>
 *   <li>Auth errors (401): No retry - fix the API key</li>
 *   <li>Client errors (4xx except 429): No retry - fix the request</li>
 * </ul>
 */
public class OpenAIClient {
    private static final Logger LOGGER = TestLogger.getLogger(OpenAIClient.class);
    private static final String OPENAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final int MAX_RETRY_DELAY_MS = 32000;
    private static final String PROVIDER_NAME = "openai";

    private final HttpClient client;
    private final String apiKey;

    public OpenAIClient() {
        this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Sends a request to the OpenAI API.
     *
     * @param systemPrompt System prompt for context
     * @param userPrompt   User prompt to process
     * @return The response text
     * @throws LLMClientException if the request fails
     */
    public String sendRequest(String systemPrompt, String userPrompt) throws LLMClientException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw LLMClientException.configurationError(PROVIDER_NAME,
                "API key is not configured. Set 'apiKey' in config/minewright-common.toml");
        }

        JsonObject requestBody = buildRequestBody(systemPrompt, userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENAI_API_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        return sendWithRetry(request);
    }

    /**
     * Sends an HTTP request with retry logic for transient failures.
     *
     * @param request The HTTP request
     * @return Response body content
     * @throws LLMClientException if all retries fail
     */
    private String sendWithRetry(HttpRequest request) throws LLMClientException {
        LLMClientException lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    if (responseBody == null || responseBody.isEmpty()) {
                        throw LLMClientException.invalidResponse(PROVIDER_NAME,
                            "Server returned empty response body");
                    }
                    return parseResponse(responseBody);
                }

                // Handle HTTP errors
                lastException = handleHttpError(response, attempt);

                // Don't retry non-retryable errors
                if (!lastException.isRetryable()) {
                    throw lastException;
                }

                // Calculate delay with exponential backoff
                int delayMs = calculateRetryDelay(attempt);
                LOGGER.warn("[{}] Request failed (attempt {}/{}), retrying in {}ms: {}",
                    PROVIDER_NAME, attempt + 1, MAX_RETRIES, delayMs, lastException.getMessage());

                Thread.sleep(delayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw LLMClientException.timeout(PROVIDER_NAME, 60000, e);
            } catch (LLMClientException e) {
                // Re-throw LLM exceptions directly
                throw e;
            } catch (Exception e) {
                // Handle unexpected exceptions (usually network errors)
                lastException = LLMClientException.networkError(PROVIDER_NAME, e);

                if (attempt < MAX_RETRIES - 1) {
                    int delayMs = calculateRetryDelay(attempt);
                    LOGGER.warn("[{}] Network error (attempt {}/{}), retrying in {}ms: {}",
                        PROVIDER_NAME, attempt + 1, MAX_RETRIES, delayMs, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw lastException;
                    }
                } else {
                    throw lastException;
                }
            }
        }

        // All retries exhausted
        throw lastException;
    }

    /**
     * Handles HTTP error responses.
     *
     * @param response The HTTP response
     * @param attempt  Current attempt number
     * @return LLMClientException for the error
     */
    private LLMClientException handleHttpError(HttpResponse<String> response, int attempt) {
        int statusCode = response.statusCode();
        String body = response.body();

        // Rate limit
        if (statusCode == 429) {
            java.time.Duration retryAfter = extractRetryAfter(body);
            return LLMClientException.rateLimited(PROVIDER_NAME, retryAfter);
        }

        // Authentication error
        if (statusCode == 401 || statusCode == 403) {
            return LLMClientException.authenticationFailed(PROVIDER_NAME);
        }

        // Server error (retryable)
        if (statusCode >= 500) {
            return LLMClientException.serverError(PROVIDER_NAME, statusCode);
        }

        // Other client errors (not retryable)
        return new LLMClientException(
            "HTTP " + statusCode + ": " + extractErrorMessage(body),
            PROVIDER_NAME,
            statusCode,
            com.minewright.exception.MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "The request was rejected by the " + PROVIDER_NAME.toUpperCase() + " API. " +
            "Check the error message and adjust your prompt or parameters.",
            false
        );
    }

    /**
     * Parses the API response.
     *
     * @param responseBody Response body from API
     * @return Parsed content
     * @throws LLMClientException if response is invalid
     */
    private String parseResponse(String responseBody) throws LLMClientException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject firstChoice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (firstChoice.has("message")) {
                    JsonObject message = firstChoice.getAsJsonObject("message");
                    if (message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }

            // Check for error in response body
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMsg = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                throw LLMClientException.invalidResponse(PROVIDER_NAME,
                    "API returned error: " + errorMsg);
            }

            throw LLMClientException.invalidResponse(PROVIDER_NAME,
                "Response missing expected 'choices' field");

        } catch (LLMClientException e) {
            throw e;
        } catch (Exception e) {
            throw LLMClientException.invalidResponse(PROVIDER_NAME,
                "Failed to parse JSON: " + e.getMessage());
        }
    }

    /**
     * Builds the request body for the API call.
     */
    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", MineWrightConfig.OPENAI_MODEL.get());
        body.addProperty("temperature", MineWrightConfig.TEMPERATURE.get());
        body.addProperty("max_tokens", MineWrightConfig.MAX_TOKENS.get());

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        body.add("messages", messages);

        return body;
    }

    /**
     * Calculates retry delay with exponential backoff.
     *
     * @param attempt Current attempt (0-indexed)
     * @return Delay in milliseconds
     */
    private int calculateRetryDelay(int attempt) {
        int delay = INITIAL_RETRY_DELAY_MS * (1 << attempt);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    /**
     * Extracts retry-after duration from response body.
     *
     * @param body Response body
     * @return Retry duration, or null if not specified
     */
    private java.time.Duration extractRetryAfter(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                // Check for retry_after_ms or similar fields
                if (error.has("retry_after_ms")) {
                    return java.time.Duration.ofMillis(error.get("retry_after_ms").getAsLong());
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }

    /**
     * Extracts error message from response body.
     *
     * @param body Response body
     * @return Error message
     */
    private String extractErrorMessage(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return body.length() > 100 ? body.substring(0, 100) + "..." : body;
    }
}

