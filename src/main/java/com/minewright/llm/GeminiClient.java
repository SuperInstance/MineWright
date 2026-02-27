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
 * Client for Google Gemini API
 * FREE tier: 15 RPM, 1500 RPD
 * Paid: ~10x cheaper than GPT-3.5
 * Using gemini-2.5-flash with high token limit for thinking mode
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Throws {@link LLMClientException} for all failures</li>
 *   <li>Automatic retry with exponential backoff for retryable errors</li>
 *   <li>Detailed error messages with recovery suggestions</li>
 * </ul>
 */
public class GeminiClient {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final String PROVIDER_NAME = "gemini";

    private final HttpClient client;
    private final String apiKey;

    public GeminiClient() {
        this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Sends a request to the Gemini API.
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
        String urlWithKey = GEMINI_API_URL + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlWithKey))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
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
                    String responseBody = response.body();
                    if (responseBody == null || responseBody.isEmpty()) {
                        throw LLMClientException.invalidResponse(PROVIDER_NAME,
                            "Server returned empty response body");
                    }
                    return parseResponse(responseBody);
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
                throw LLMClientException.timeout(PROVIDER_NAME, 60000, e);
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
            return LLMClientException.rateLimited(PROVIDER_NAME, java.time.Duration.ofSeconds(60));
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
            "The request was rejected by the Gemini API. Check the error message and adjust your prompt.",
            false
        );
    }

    /**
     * Parses the API response.
     */
    private String parseResponse(String responseBody) throws LLMClientException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // Gemini response format: candidates[0].content.parts[0].text
            if (json.has("candidates") && json.getAsJsonArray("candidates").size() > 0) {
                JsonObject firstCandidate = json.getAsJsonArray("candidates").get(0).getAsJsonObject();

                if (firstCandidate.has("finishReason")) {
                    String finishReason = firstCandidate.get("finishReason").getAsString();
                    if ("MAX_TOKENS".equals(finishReason)) {
                        MineWrightMod.LOGGER.warn("[{}] Response was cut off due to MAX_TOKENS limit",
                            PROVIDER_NAME);
                    }
                }

                if (firstCandidate.has("content")) {
                    JsonObject content = firstCandidate.getAsJsonObject("content");
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        JsonObject firstPart = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (firstPart.has("text")) {
                            return firstPart.get("text").getAsString();
                        }
                    }
                }
            }

            // Check for error in response
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMsg = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                throw LLMClientException.invalidResponse(PROVIDER_NAME, "API returned error: " + errorMsg);
            }

            throw LLMClientException.invalidResponse(PROVIDER_NAME,
                "Response missing expected 'candidates' field");

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
        JsonObject body = new JsonObject();

        // Gemini uses "contents" array with "parts"
        JsonArray contents = new JsonArray();

        // System instruction (Gemini 1.5+ format)
        JsonObject systemContent = new JsonObject();
        systemContent.addProperty("role", "user");
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt + "\n\n" + userPrompt);
        systemParts.add(systemPart);
        systemContent.add("parts", systemParts);
        contents.add(systemContent);

        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", MineWrightConfig.TEMPERATURE.get());
        generationConfig.addProperty("maxOutputTokens", MineWrightConfig.MAX_TOKENS.get());
        body.add("generationConfig", generationConfig);

        return body;
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

