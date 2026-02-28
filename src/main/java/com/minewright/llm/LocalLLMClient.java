package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Client for local LLM models running in Docker (e.g., vLLM, Ollama, llama.cpp).
 *
 * <p>Uses the OpenAI-compatible API format that many local LLM servers provide.</p>
 *
 * <p><b>Default Configuration:</b></p>
 * <ul>
 *   <li>URL: http://localhost:8000/v1/chat/completions (vLLM default)</li>
 *   <li>Model: ai/smollm2-vllm:360M</li>
 * </ul>
 *
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>No API costs - completely free</li>
 *   <li>No rate limits</li>
 *   <li>Works offline</li>
 *   <li>Fast for simple tasks</li>
 *   <li>Privacy - data stays local</li>
 *   <li>Vision support for multimodal models</li>
 * </ul>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Light conversation and banter</li>
 *   <li>Simple command preprocessing</li>
 *   <li>Quick decisions during work</li>
 *   <li>Fallback when cloud APIs unavailable</li>
 *   <li>Screenshot analysis with vision models</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class LocalLLMClient {
    private static final Logger LOGGER = TestLogger.getLogger(LocalLLMClient.class);

    // Common local LLM server URLs
    public static final String VLLM_URL = "http://localhost:8000/v1/chat/completions";
    public static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";
    public static final String LLAMACPP_URL = "http://localhost:8080/v1/chat/completions";
    public static final String LMSTUDIO_URL = "http://localhost:1234/v1/chat/completions";

    // Default settings
    private static final String DEFAULT_URL = VLLM_URL;
    private static final String DEFAULT_MODEL = "ai/smollm2-vllm:360M";
    private static final int TIMEOUT_SECONDS = 120;

    private final HttpClient client;
    private final String serverUrl;
    private final String modelName;
    private volatile boolean available = false;
    private volatile boolean supportsVision = false;
    private volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 60000; // Check availability every minute

    public LocalLLMClient() {
        this(DEFAULT_URL, DEFAULT_MODEL);
    }

    public LocalLLMClient(String serverUrl, String modelName) {
        this.serverUrl = serverUrl;
        this.modelName = modelName;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        // Check availability on startup
        checkAvailability();
    }

    /**
     * Checks if the local LLM server is available.
     */
    public boolean isAvailable() {
        // Cache the availability check
        if (System.currentTimeMillis() - lastCheckTime > CHECK_INTERVAL_MS) {
            checkAvailability();
        }
        return available;
    }

    /**
     * Checks if the model supports vision capabilities.
     *
     * @return true if the model can process images, false otherwise
     */
    public boolean isVisionSupported() {
        return supportsVision;
    }

    /**
     * Performs availability check and detects vision capabilities.
     */
    private void checkAvailability() {
        lastCheckTime = System.currentTimeMillis();
        try {
            // Try a simple request to check if server is up
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl.replace("/chat/completions", "/models")))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            available = response.statusCode() == 200;

            if (available) {
                LOGGER.info("[LocalLLM] Server available at {} with model {}", serverUrl, modelName);
                detectVisionCapabilities(response.body());
            } else {
                supportsVision = false;
            }
        } catch (Exception e) {
            available = false;
            supportsVision = false;
            LOGGER.debug("[LocalLLM] Server not available: {}", e.getMessage());
        }
    }

    /**
     * Detects if the model supports vision by parsing the /models response.
     * vLLM returns model information including capabilities.
     */
    private void detectVisionCapabilities(String modelsResponse) {
        try {
            JsonObject json = JsonParser.parseString(modelsResponse).getAsJsonObject();
            if (json.has("data")) {
                JsonArray models = json.getAsJsonArray("data");
                for (int i = 0; i < models.size(); i++) {
                    JsonObject model = models.get(i).getAsJsonObject();
                    String modelId = model.get("id").getAsString();

                    // Check if this is our model
                    if (modelId.equals(modelName) || modelId.contains(modelName.split(":")[0])) {
                        // Look for vision-related keywords in model info
                        String modelLower = modelId.toLowerCase();
                        supportsVision = modelLower.contains("vision") ||
                                        modelLower.contains("vlm") ||
                                        modelLower.contains("multimodal") ||
                                        modelLower.contains("smollm2"); // smollm2 may have vision

                        if (supportsVision) {
                            LOGGER.info("[LocalLLM] Model {} supports vision", modelId);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[LocalLLM] Could not detect vision capabilities: {}", e.getMessage());
            // Assume no vision on error
            supportsVision = false;
        }
    }

    /**
     * Sends a synchronous request to the local LLM.
     *
     * @param systemPrompt System context
     * @param userPrompt   User message
     * @return Response text, or null if unavailable/error
     */
    public String sendRequest(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            LOGGER.debug("[LocalLLM] Server not available");
            return null;
        }

        try {
            JsonObject requestBody = buildRequestBody(systemPrompt, userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                String content = parseResponse(response.body());
                LOGGER.info("[LocalLLM] Response received ({}ms, {} chars)", latency,
                    content != null ? content.length() : 0);
                return content;
            } else {
                LOGGER.error("[LocalLLM] Error {}: {}", response.statusCode(), response.body());
                available = false; // Mark as unavailable on error
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("[LocalLLM] Request failed: {}", e.getMessage());
            available = false;
            return null;
        }
    }

    /**
     * Sends an async request to the local LLM.
     */
    public CompletableFuture<String> sendRequestAsync(String systemPrompt, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> sendRequest(systemPrompt, userPrompt));
    }

    /**
     * Quick chat - simpler prompt format for fast responses.
     */
    public String quickChat(String message) {
        return sendRequest("You are a helpful Minecraft assistant. Be brief and friendly.", message);
    }

    /**
     * Classify complexity - determines if a task needs cloud LLM.
     *
     * @param message The user message to classify
     * @param hasImage Whether the request includes image data (requires vision support)
     * @return "local" if can handle locally, "cloud" if needs cloud LLM
     */
    public String classifyComplexity(String message, boolean hasImage) {
        if (!isAvailable()) {
            return "cloud"; // Default to cloud if local unavailable
        }

        // If image is present and model doesn't support vision, need cloud
        if (hasImage && !supportsVision) {
            LOGGER.info("[LocalLLM] Image present but model doesn't support vision, routing to cloud");
            return "cloud";
        }

        // Vision input increases complexity
        String complexityModifier = hasImage ? " (includes image analysis)" : "";

        String systemPrompt = """
            Classify this Minecraft command's complexity.
            Respond with exactly one word:
            - "simple" for: greetings, follow, wait, simple lookups
            - "moderate" for: gather resources, simple building, crafting, basic image queries
            - "complex" for: large builds, strategy, debugging, multi-step plans, detailed scene analysis

            Only respond with one word: simple, moderate, or complex
            """;

        String result = sendRequest(systemPrompt, message + complexityModifier);
        if (result != null) {
            result = result.toLowerCase().trim();
            if (result.contains("simple")) return "local";
            if (result.contains("moderate")) return "local"; // Can try local first
            if (result.contains("complex")) return "cloud";
        }
        return "cloud"; // Default to cloud on uncertainty
    }

    /**
     * Classify complexity - determines if a task needs cloud LLM.
     * Overloaded method for backward compatibility.
     *
     * @param message The user message to classify
     * @return "local" if can handle locally, "cloud" if needs cloud LLM
     */
    public String classifyComplexity(String message) {
        return classifyComplexity(message, false);
    }

    /**
     * Analyzes a screenshot using the vision model.
     *
     * @param imageData Base64-encoded image data (PNG/JPG)
     * @param query Question to ask about the image
     * @return Analysis response, or null if unavailable/vision not supported
     */
    public String analyzeScreenshot(String imageData, String query) {
        if (!isAvailable() || !supportsVision) {
            LOGGER.debug("[LocalLLM] Vision analysis not available");
            return null;
        }

        return sendVisionRequest(query, imageData);
    }

    /**
     * Analyzes a screenshot with a custom system prompt.
     *
     * @param systemPrompt System context for analysis
     * @param imageData Base64-encoded image data (PNG/JPG)
     * @param query Question to ask about the image
     * @return Analysis response, or null if unavailable/vision not supported
     */
    public String analyzeScreenshot(String systemPrompt, String imageData, String query) {
        if (!isAvailable() || !supportsVision) {
            LOGGER.debug("[LocalLLM] Vision analysis not available");
            return null;
        }

        return sendVisionRequestWithSystem(systemPrompt, query, imageData);
    }

    /**
     * Sends a request with image data (multimodal).
     *
     * @param userPrompt Text prompt accompanying the image
     * @param base64Image Base64-encoded image data
     * @return Response text, or null if unavailable/error
     */
    public String sendVisionRequest(String userPrompt, String base64Image) {
        return sendVisionRequestWithSystem(
            "You are a helpful Minecraft assistant. Analyze the screenshot and provide detailed observations.",
            userPrompt,
            base64Image
        );
    }

    /**
     * Sends a vision request with custom system prompt.
     *
     * @param systemPrompt System context for analysis
     * @param userPrompt Text prompt accompanying the image
     * @param base64Image Base64-encoded image data
     * @return Response text, or null if unavailable/error
     */
    public String sendVisionRequestWithSystem(String systemPrompt, String userPrompt, String base64Image) {
        if (!isAvailable() || !supportsVision) {
            LOGGER.debug("[LocalLLM] Vision request not available");
            return null;
        }

        try {
            JsonObject requestBody = buildVisionRequestBody(systemPrompt, userPrompt, base64Image);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                String content = parseResponse(response.body());
                LOGGER.info("[LocalLLM] Vision response received ({}ms, {} chars)", latency,
                    content != null ? content.length() : 0);
                return content;
            } else {
                LOGGER.error("[LocalLLM] Vision request error {}: {}", response.statusCode(), response.body());
                available = false; // Mark as unavailable on error
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("[LocalLLM] Vision request failed: {}", e.getMessage());
            available = false;
            return null;
        }
    }

    /**
     * Sends a synchronous request to the local LLM with optional image support.
     *
     * @param systemPrompt System context
     * @param userPrompt   User message
     * @param base64Image  Optional base64-encoded image data (null for text-only)
     * @return Response text, or null if unavailable/error
     */
    public String sendRequest(String systemPrompt, String userPrompt, String base64Image) {
        if (!isAvailable()) {
            LOGGER.debug("[LocalLLM] Server not available");
            return null;
        }

        // Route to vision request if image provided and model supports it
        if (base64Image != null && !base64Image.isEmpty()) {
            if (supportsVision) {
                return sendVisionRequestWithSystem(systemPrompt, userPrompt, base64Image);
            } else {
                LOGGER.warn("[LocalLLM] Image provided but model doesn't support vision, sending text-only request");
            }
        }

        return sendRequest(systemPrompt, userPrompt);
    }

    /**
     * Sends an async request to the local LLM with optional image support.
     */
    public CompletableFuture<String> sendRequestAsync(String systemPrompt, String userPrompt, String base64Image) {
        return CompletableFuture.supplyAsync(() -> sendRequest(systemPrompt, userPrompt, base64Image));
    }

    /**
     * Builds the JSON request body for text-only requests.
     */
    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("max_tokens", 2048);
        body.addProperty("temperature", 0.7);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        body.add("messages", messages);
        return body;
    }

    /**
     * Builds the JSON request body for vision/multimodal requests.
     * Uses OpenAI-compatible format with content array containing text and image_url.
     */
    private JsonObject buildVisionRequestBody(String systemPrompt, String userPrompt, String base64Image) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("max_tokens", 4096); // More tokens for vision tasks
        body.addProperty("temperature", 0.7);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();

        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

        // Build user message with multimodal content
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        // Content array with text and image
        JsonArray content = new JsonArray();

        // Add text content
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", userPrompt);
        content.add(textContent);

        // Add image content
        JsonObject imageContent = new JsonObject();
        imageContent.addProperty("type", "image_url");

        JsonObject imageUrl = new JsonObject();
        String mimeType = detectImageMimeType(base64Image);
        imageUrl.addProperty("url", "data:" + mimeType + ";base64," + base64Image);
        imageContent.add("image_url", imageUrl);

        content.add(imageContent);

        userMsg.add("content", content);
        messages.add(userMsg);

        body.add("messages", messages);
        return body;
    }

    /**
     * Detects MIME type from base64 image data.
     * Defaults to PNG if cannot detect.
     */
    private String detectImageMimeType(String base64Data) {
        try {
            // Check magic bytes in base64
            if (base64Data.startsWith("/9j/")) {
                return "image/jpeg";
            } else if (base64Data.startsWith("iVBORw0KGgo") || base64Data.startsWith("iVBORw0K")) {
                return "image/png";
            } else if (base64Data.startsWith("R0lGODlh") || base64Data.startsWith("R0lGODdh")) {
                return "image/gif";
            } else if (base64Data.startsWith("Qk0")) {
                return "image/bmp";
            } else if (base64Data.startsWith("UklGR")) {
                return "image/webp";
            }
        } catch (Exception e) {
            LOGGER.debug("[LocalLLM] Could not detect image MIME type, defaulting to PNG");
        }
        return "image/png"; // Default to PNG
    }

    /**
     * Parses the response JSON to extract content.
     */
    private String parseResponse(String json) {
        try {
            JsonObject response = JsonParser.parseString(json).getAsJsonObject();
            if (response.has("choices")) {
                JsonArray choices = response.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        return firstChoice.getAsJsonObject("message").get("content").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[LocalLLM] Failed to parse response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Gets the server URL.
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Gets the model name.
     */
    public String getModelName() {
        return modelName;
    }
}
