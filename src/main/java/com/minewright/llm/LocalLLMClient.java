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
import java.util.concurrent.CompletableFuture;

/**
 * Client for local LLM models running in Docker (e.g., Ollama, llama.cpp).
 *
 * <p>Uses the OpenAI-compatible API format that many local LLM servers provide.</p>
 *
 * <p><b>Default Configuration:</b></p>
 * <ul>
 *   <li>URL: http://localhost:11434/v1/chat/completions (Ollama default)</li>
 *   <li>Model: llama3.2</li>
 * </ul>
 *
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>No API costs - completely free</li>
 *   <li>No rate limits</li>
 *   <li>Works offline</li>
 *   <li>Fast for simple tasks</li>
 *   <li>Privacy - data stays local</li>
 * </ul>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Light conversation and banter</li>
 *   <li>Simple command preprocessing</li>
 *   <li>Quick decisions during work</li>
 *   <li>Fallback when cloud APIs unavailable</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class LocalLLMClient {
    private static final Logger LOGGER = TestLogger.getLogger(LocalLLMClient.class);

    // Common local LLM server URLs
    public static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";
    public static final String LLAMACPP_URL = "http://localhost:8080/v1/chat/completions";
    public static final String LMSTUDIO_URL = "http://localhost:1234/v1/chat/completions";

    // Default settings
    private static final String DEFAULT_URL = OLLAMA_URL;
    private static final String DEFAULT_MODEL = "llama3.2";
    private static final int TIMEOUT_SECONDS = 120;

    private final HttpClient client;
    private final String serverUrl;
    private final String modelName;
    private volatile boolean available = false;
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
     * Performs availability check.
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
            }
        } catch (Exception e) {
            available = false;
            LOGGER.debug("[LocalLLM] Server not available: {}", e.getMessage());
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
     * @return "local" if can handle locally, "cloud" if needs cloud LLM
     */
    public String classifyComplexity(String message) {
        if (!isAvailable()) {
            return "cloud"; // Default to cloud if local unavailable
        }

        String systemPrompt = """
            Classify this Minecraft command's complexity.
            Respond with exactly one word:
            - "simple" for: greetings, follow, wait, simple lookups
            - "moderate" for: gather resources, simple building, crafting
            - "complex" for: large builds, strategy, debugging, multi-step plans

            Only respond with one word: simple, moderate, or complex
            """;

        String result = sendRequest(systemPrompt, message);
        if (result != null) {
            result = result.toLowerCase().trim();
            if (result.contains("simple")) return "local";
            if (result.contains("moderate")) return "local"; // Can try local first
            if (result.contains("complex")) return "cloud";
        }
        return "cloud"; // Default to cloud on uncertainty
    }

    /**
     * Builds the JSON request body.
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
