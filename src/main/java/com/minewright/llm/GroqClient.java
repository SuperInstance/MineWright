package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;
import com.minewright.exception.LLMClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for Groq API - BLAZING FAST inference
 * FREE tier: 30 RPM, 14,400 RPD
 * Speed: 0.5-2 seconds (vs Gemini's 10-30s)
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Throws {@link LLMClientException} for all failures</li>
 *   <li>Automatic retry with exponential backoff for retryable errors</li>
 *   <li>Detailed error messages with recovery suggestions</li>
 * </ul>
 */
public class GroqClient {
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 500;
    private static final String PROVIDER_NAME = "groq";

    private final HttpClient client;
    private final String apiKey;

    public GroqClient() {
        this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Sends a request to the Groq API.
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
            .uri(URI.create(GROQ_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        return sendWithRetry(request);
    }

    /**
     * Sends an HTTP request with retry logic for transient failures.
     */
    private String sendWithRetry(HttpRequest request) throws LLMClientException {
        LLMClientException lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                }

                // Handle HTTP errors
                lastException = handleHttpError(response);

                // Don't retry non-retryable errors
                if (!lastException.isRetryable() || attempt >= MAX_RETRIES - 1) {
                    throw lastException;
                }

                // Calculate delay with exponential backoff
                int delayMs = INITIAL_RETRY_DELAY_MS * (1 << attempt);
                MineWrightMod.LOGGER.warn("[{}] Request failed (attempt {}/{}), retrying in {}ms: {}",
                    PROVIDER_NAME, attempt + 1, MAX_RETRIES, delayMs, lastException.getMessage());
                Thread.sleep(delayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw LLMClientException.timeout(PROVIDER_NAME, 30000, e);
            } catch (LLMClientException e) {
                throw e;
            } catch (Exception e) {
                lastException = LLMClientException.networkError(PROVIDER_NAME, e);
                if (attempt >= MAX_RETRIES - 1) {
                    throw lastException;
                }
                int delayMs = INITIAL_RETRY_DELAY_MS * (1 << attempt);
                MineWrightMod.LOGGER.warn("[{}] Network error (attempt {}/{}), retrying in {}ms: {}",
                    PROVIDER_NAME, attempt + 1, MAX_RETRIES, delayMs, e.getMessage());
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw lastException;
                }
            }
        }

        throw lastException;
    }

    /**
     * Handles HTTP error responses.
     */
    private LLMClientException handleHttpError(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String body = response.body();

        if (statusCode == 429) {
            return LLMClientException.rateLimited(PROVIDER_NAME, java.time.Duration.ofSeconds(1));
        }
        if (statusCode == 401 || statusCode == 403) {
            return LLMClientException.authenticationFailed(PROVIDER_NAME);
        }
        if (statusCode >= 500) {
            return LLMClientException.serverError(PROVIDER_NAME, statusCode);
        }

        return new LLMClientException(
            "HTTP " + statusCode + ": " + extractErrorMessage(body),
            PROVIDER_NAME,
            statusCode,
            com.minewright.exception.MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "The request was rejected by the Groq API. Check the error message and adjust your prompt.",
            false
        );
    }

    /**
     * Parses the API response.
     */
    private String parseResponse(String responseBody) throws LLMClientException {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                return jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            }

            if (jsonResponse.has("error")) {
                JsonObject error = jsonResponse.getAsJsonObject("error");
                String errorMsg = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                throw LLMClientException.invalidResponse(PROVIDER_NAME, "API returned error: " + errorMsg);
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
     * Builds the request body.
     */
    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama-3.1-8b-instant");

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 500);
        requestBody.addProperty("temperature", 0.7);

        return requestBody;
    }

    /**
     * Extracts error message from response body.
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

