package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.exception.LLMClientException;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-level client for vLLM API - OpenAI-compatible local inference server.
 *
 * <p><b>Base URL:</b> http://localhost:8000/v1 (default vLLM port)</p>
 *
 * <p><b>Endpoints Supported:</b></p>
 * <ul>
 *   <li>{@code /v1/chat/completions} - Chat completions with multimodal support</li>
 *   <li>{@code /v1/models} - List available models and detect default model</li>
 * </ul>
 *
 * <p><b>Multimodal Support:</b></p>
 * <ul>
 *   <li>Text-only messages</li>
 *   <li>Text + image messages (base64 encoded)</li>
 *   <li>Multiple images per request</li>
 * </ul>
 *
 * <p><b>Error Handling:</b></p>
 * <ul>
 *   <li>Throws {@link LLMClientException} for all failures</li>
 *   <li>Automatic retry with exponential backoff for retryable errors</li>
 *   <li>Connection validation on startup</li>
 *   <li>Model name auto-detection from /v1/models endpoint</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Text-only request
 * VLLMClient client = new VLLMClient();
 * String response = client.chat("You are a helpful assistant.", "Hello, how are you?");
 *
 * // Vision request (multimodal)
 * String imageBase64 = "iVBORw0KGgoAAAANSUhEUg...";
 * VisionContent visionContent = VisionContent.withImage("What do you see?", imageBase64);
 * String response = client.chat("Describe images accurately.", visionContent);
 * </pre>
 *
 * <p><b>Starting vLLM Server:</b></p>
 * <pre>
 * vllm serve meta-llama/Llama-3-8b-Instruct \
 *   --port 8000 \
 *   --chat-template ./chat_template.jinja \
 *   --max-model-len 4096
 * </pre>
 *
 * @since 1.4.0
 */
public class VLLMClient {
    private static final Logger LOGGER = TestLogger.getLogger(VLLMClient.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:8000/v1";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String MODELS_ENDPOINT = "/models";
    private static final String PROVIDER_NAME = "vllm";
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final int MAX_RETRY_DELAY_MS = 32000;

    private final HttpClient client;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final Duration connectTimeout;

    private volatile String cachedModelName;
    private volatile boolean isAvailable;
    private volatile long lastAvailabilityCheck;
    private static final long AVAILABILITY_CHECK_INTERVAL_MS = 30000; // 30 seconds

    /**
     * Creates a VLLMClient with default settings.
     * <ul>
     *   <li>Base URL: http://localhost:8000/v1</li>
     *   <li>Request timeout: 120 seconds</li>
     *   <li>Connect timeout: 10 seconds</li>
     *   <li>Auto-detects model name on first request</li>
     * </ul>
     */
    public VLLMClient() {
        this(DEFAULT_BASE_URL, Duration.ofSeconds(120), Duration.ofSeconds(10));
    }

    /**
     * Creates a VLLMClient with custom base URL.
     *
     * @param baseUrl The base URL for the vLLM server (e.g., "http://localhost:8080/v1")
     */
    public VLLMClient(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(120), Duration.ofSeconds(10));
    }

    /**
     * Creates a VLLMClient with full configuration.
     *
     * @param baseUrl        The base URL for the vLLM server
     * @param requestTimeout Timeout for individual requests
     * @param connectTimeout Connection timeout
     */
    public VLLMClient(String baseUrl, Duration requestTimeout, Duration connectTimeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.requestTimeout = requestTimeout;
        this.connectTimeout = connectTimeout;

        this.client = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();

        // Check availability and detect model on startup
        checkAvailability();
    }

    /**
     * Sends a text-only chat completion request.
     *
     * <p>This is a convenience method that wraps the prompt in a VisionContent
     * with no images.</p>
     *
     * @param systemPrompt System prompt for context
     * @param userPrompt   User prompt to process
     * @return The response text
     * @throws LLMClientException if the request fails
     */
    public String chat(String systemPrompt, String userPrompt) throws LLMClientException {
        return chat(systemPrompt, VisionContent.textOnly(userPrompt));
    }

    /**
     * Sends a chat completion request with optional vision content.
     *
     * <p>Supports both text-only and multimodal (text + images) requests.
     * Images should be base64-encoded PNG/JPEG data.</p>
     *
     * @param systemPrompt System prompt for context
     * @param content      The prompt content (text or multimodal)
     * @return The response text
     * @throws LLMClientException if the request fails
     */
    public String chat(String systemPrompt, VisionContent content) throws LLMClientException {
        if (!isAvailable()) {
            throw LLMClientException.networkError(PROVIDER_NAME,
                new IllegalStateException("vLLM server is not available at " + baseUrl));
        }

        JsonObject requestBody = buildChatRequestBody(systemPrompt, content, getModelName());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + CHAT_COMPLETIONS_ENDPOINT))
            .header("Content-Type", "application/json")
            .timeout(requestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        return sendWithRetry(request);
    }

    /**
     * Sends an asynchronous chat completion request.
     *
     * @param systemPrompt System prompt for context
     * @param content      The prompt content (text or multimodal)
     * @return CompletableFuture containing the response text
     */
    public CompletableFuture<String> chatAsync(String systemPrompt, VisionContent content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return chat(systemPrompt, content);
            } catch (LLMClientException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Sends an asynchronous text-only chat completion request.
     *
     * @param systemPrompt System prompt for context
     * @param userPrompt   User prompt to process
     * @return CompletableFuture containing the response text
     */
    public CompletableFuture<String> chatAsync(String systemPrompt, String userPrompt) {
        return chatAsync(systemPrompt, VisionContent.textOnly(userPrompt));
    }

    /**
     * Checks if the vLLM server is available.
     *
     * <p>Availability is cached for 30 seconds to avoid excessive probing.</p>
     *
     * @return true if the server is responding, false otherwise
     */
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastAvailabilityCheck > AVAILABILITY_CHECK_INTERVAL_MS) {
            checkAvailability();
        }
        return isAvailable;
    }

    /**
     * Performs an availability check by querying the /models endpoint.
     */
    private void checkAvailability() {
        lastAvailabilityCheck = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + MODELS_ENDPOINT))
                .timeout(connectTimeout)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                isAvailable = true;
                // Try to extract model name from response
                extractModelName(response.body());
                LOGGER.info("[vLLM] Server available at {} with model: {}",
                    baseUrl, cachedModelName != null ? cachedModelName : "auto-detected");
            } else {
                isAvailable = false;
                LOGGER.debug("[vLLM] Server returned status {}", response.statusCode());
            }
        } catch (Exception e) {
            isAvailable = false;
            LOGGER.debug("[vLLM] Availability check failed: {}", e.getMessage());
        }
    }

    /**
     * Extracts the model name from the /models endpoint response.
     *
     * <p>The /models endpoint returns:
     * <pre>
     * {
     *   "object": "list",
     *   "data": [
     *     {"id": "meta-llama/Llama-3-8b-Instruct", "object": "model", ...}
     *   ]
     * }
     * </pre>
     * We extract the first model's ID as the default model name.</p>
     *
     * @param responseBody The response body from /models
     */
    private void extractModelName(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("data")) {
                JsonArray models = json.getAsJsonArray("data");
                if (models.size() > 0) {
                    JsonObject firstModel = models.get(0).getAsJsonObject();
                    cachedModelName = firstModel.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[vLLM] Failed to extract model name: {}", e.getMessage());
        }
    }

    /**
     * Gets the model name to use for requests.
     *
     * <p>If auto-detected, returns the cached model name from /models.
     * If not yet detected, attempts detection.
     * Returns "default" as last resort.</p>
     *
     * @return The model name
     */
    private String getModelName() {
        if (cachedModelName == null) {
            checkAvailability();
        }
        return cachedModelName != null ? cachedModelName : "default";
    }

    /**
     * Returns the detected model name.
     *
     * @return The model name, or null if not yet detected
     */
    public String getDetectedModelName() {
        return cachedModelName;
    }

    /**
     * Returns the base URL.
     *
     * @return The base URL for the vLLM server
     */
    public String getBaseUrl() {
        return baseUrl;
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
                    return parseChatResponse(responseBody);
                }

                // Handle HTTP errors
                lastException = handleHttpError(response, attempt);

                // Don't retry non-retryable errors
                if (!lastException.isRetryable()) {
                    throw lastException;
                }

                // Calculate delay with exponential backoff
                int delayMs = calculateRetryDelay(attempt);
                LOGGER.warn("[vLLM] Request failed (attempt {}/{}), retrying in {}ms: {}",
                    attempt + 1, MAX_RETRIES, delayMs, lastException.getMessage());

                Thread.sleep(delayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw LLMClientException.timeout(PROVIDER_NAME, requestTimeout.toMillis(), e);
            } catch (LLMClientException e) {
                throw e;
            } catch (Exception e) {
                lastException = LLMClientException.networkError(PROVIDER_NAME, e);

                if (attempt < MAX_RETRIES - 1) {
                    int delayMs = calculateRetryDelay(attempt);
                    LOGGER.warn("[vLLM] Network error (attempt {}/{}), retrying in {}ms: {}",
                        attempt + 1, MAX_RETRIES, delayMs, e.getMessage());
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

        throw lastException;
    }

    /**
     * Handles HTTP error responses.
     */
    private LLMClientException handleHttpError(HttpResponse<String> response, int attempt) {
        int statusCode = response.statusCode();
        String body = response.body();

        if (statusCode == 429) {
            return LLMClientException.rateLimited(PROVIDER_NAME, extractRetryAfter(body));
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
            "The request was rejected by the vLLM server. Check the error message and adjust your request.",
            false
        );
    }

    /**
     * Parses the chat completion response.
     */
    private String parseChatResponse(String responseBody) throws LLMClientException {
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
     * Builds the request body for chat completions.
     */
    private JsonObject buildChatRequestBody(String systemPrompt, VisionContent content, String modelName) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("max_tokens", 2048);
        body.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        // System message
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);
        }

        // User message (potentially multimodal)
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        if (content.isMultimodal()) {
            // Multimodal content: array of text + images
            JsonArray contentArray = new JsonArray();

            // Add text part
            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", content.getText());
            contentArray.add(textPart);

            // Add image parts
            for (String imageBase64 : content.getImages()) {
                JsonObject imagePart = new JsonObject();
                imagePart.addProperty("type", "image_url");
                JsonObject imageUrl = new JsonObject();
                imageUrl.addProperty("url", "data:image/png;base64," + imageBase64);
                imagePart.add("image_url", imageUrl);
                contentArray.add(imagePart);
            }

            userMessage.add("content", contentArray);
        } else {
            // Text-only content
            userMessage.addProperty("content", content.getText());
        }

        messages.add(userMessage);
        body.add("messages", messages);

        return body;
    }

    /**
     * Calculates retry delay with exponential backoff.
     */
    private int calculateRetryDelay(int attempt) {
        int delay = INITIAL_RETRY_DELAY_MS * (1 << attempt);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    /**
     * Extracts retry-after duration from response body.
     */
    private Duration extractRetryAfter(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                if (error.has("retry_after_ms")) {
                    return Duration.ofMillis(error.get("retry_after_ms").getAsLong());
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
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

    /**
     * Content container for chat requests that may include images.
     *
     * <p>Supports both text-only and multimodal (text + images) content.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>
     * // Text-only
     * VisionContent content = VisionContent.textOnly("What is the weather?");
     *
     * // Text with one image
     * VisionContent content = VisionContent.withImage("What do you see?", "iVBORw0...");
     *
     * // Text with multiple images
     * VisionContent content = VisionContent.withImages("Compare these.", List.of("iVBORw0...", "iVBORw1..."));
     * </pre>
     */
    public static class VisionContent {
        private final String text;
        private final List<String> images; // Base64-encoded image data (without data URI prefix)

        private VisionContent(String text, List<String> images) {
            this.text = text;
            this.images = images != null ? new ArrayList<>(images) : List.of();
        }

        /**
         * Creates a text-only content object.
         *
         * @param text The text prompt
         * @return A VisionContent with no images
         */
        public static VisionContent textOnly(String text) {
            return new VisionContent(text, List.of());
        }

        /**
         * Creates multimodal content with a single image.
         *
         * @param text        The text prompt
         * @param imageBase64 Base64-encoded image data (without data URI prefix)
         */
        public static VisionContent withImage(String text, String imageBase64) {
            return new VisionContent(text, imageBase64 != null ? List.of(imageBase64) : List.of());
        }

        /**
         * Creates multimodal content with multiple images.
         *
         * @param text         The text prompt
         * @param imagesBase64 List of base64-encoded image data (without data URI prefix)
         */
        public static VisionContent withImages(String text, List<String> imagesBase64) {
            return new VisionContent(text, imagesBase64);
        }

        /**
         * Returns the text prompt.
         */
        public String getText() {
            return text;
        }

        /**
         * Returns the list of base64-encoded images.
         */
        public List<String> getImages() {
            return new ArrayList<>(images);
        }

        /**
         * Returns whether this content includes images.
         */
        public boolean isMultimodal() {
            return images != null && !images.isEmpty();
        }
    }

    /**
     * Wrapper exception for unchecked exception propagation in CompletableFuture.
     */
    private static class CompletionException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CompletionException(Throwable cause) {
            super(cause);
        }
    }
}
