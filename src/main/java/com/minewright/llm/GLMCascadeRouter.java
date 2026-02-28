package com.minewright.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewright.testutil.TestLogger;
import com.minewright.config.MineWrightConfig;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cascade router for z.ai GLM models with intelligent model selection.
 *
 * <p><b>Available Models:</b></p>
 * <ul>
 *   <li><b>GLM-5</b> (glm-5): Best quality, slowest - for complex reasoning</li>
 *   <li><b>glm-4.7-flashx</b>: Fastest, cheapest - for preprocessing and simple tasks</li>
 *   <li><b>glm-4.7-flash</b>: Fast, good balance - fallback when flashx fails</li>
 *   <li><b>glm-4.6v</b>: Vision model - for screenshots and image analysis</li>
 * </ul>
 *
 * <p><b>Cascade Flow:</b></p>
 * <ol>
 *   <li>Preprocess message with flashx to clean up and assess complexity</li>
 *   <li>flashx decides: use itself, flash, or escalate to GLM-5</li>
 *   <li>On flashx failure: fall back to flash, then skip if still failing</li>
 *   <li>For vision tasks: route to glm-4.6v</li>
 * </ol>
 *
 * @since 1.3.0
 */
public class GLMCascadeRouter {
    private static final Logger LOGGER = TestLogger.getLogger(GLMCascadeRouter.class);

    // Model identifiers for z.ai
    public static final String MODEL_GLM5 = "glm-5";
    public static final String MODEL_FLASHX = "glm-4.7-flashx";
    public static final String MODEL_FLASH = "glm-4.7-flash";
    public static final String MODEL_VISION = "glm-4.6v";

    // API endpoint
    private static final String ZAI_API_URL = "https://api.z.ai/api/paas/v4/chat/completions";

    // Failure tracking for fallback logic
    private final ConcurrentHashMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES_BEFORE_SKIP = 3;
    private static final long FAILURE_RESET_MS = 60000; // Reset failure count after 1 minute
    private final ConcurrentHashMap<String, Long> lastFailureTime = new ConcurrentHashMap<>();

    // Local LLM client for free, fast inference
    private final LocalLLMClient localLLM;

    private final HttpClient client;
    private final String apiKey;

    // Preprocessing prompt for flashx
    private static final String PREPROCESS_PROMPT = """
        You are a message preprocessor for a Minecraft AI assistant.
        Your job is to:
        1. Clean up the user's message (fix typos, clarify intent)
        2. Assess the complexity: TRIVIAL, SIMPLE, MODERATE, or COMPLEX
        3. Recommend which model should handle this: flashx, flash, or glm5

        Respond in this exact JSON format:
        {"cleaned_message": "...", "complexity": "SIMPLE", "recommended_model": "flashx", "reasoning": "Brief reason"}

        Guidelines:
        - TRIVIAL/SIMPLE: Quick lookups, simple commands -> flashx
        - MODERATE: Multi-step tasks, some reasoning -> flash
        - COMPLEX: Building plans, strategy, debugging -> glm5
        """;

    public GLMCascadeRouter() {
        this.apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.localLLM = new LocalLLMClient();

        if (localLLM.isAvailable()) {
            LOGGER.info("[Cascade] Local LLM (Llama 3.2) available - will use for simple tasks");
        } else {
            LOGGER.info("[Cascade] Local LLM not available - using cloud models only");
            LOGGER.info("[Cascade] Start Ollama with: ollama run llama3.2");
        }
    }

    /**
     * Processes a message with intelligent model selection.
     *
     * <p>Flow: preprocess with flashx -> route to appropriate model</p>
     *
     * @param systemPrompt System context
     * @param userMessage  User's raw message
     * @return Response from the selected model
     */
    public String processWithCascade(String systemPrompt, String userMessage) {
        // Step 1: Preprocess with flashx to assess complexity
        PreprocessResult preprocess = preprocessMessage(userMessage);

        String cleanedMessage = preprocess.cleanedMessage();
        String recommendedModel = preprocess.recommendedModel();

        LOGGER.info("[Cascade] Complexity: {}, Recommended: {}, Message: {}",
            preprocess.complexity, recommendedModel, cleanedMessage.substring(0, Math.min(50, cleanedMessage.length())));

        // Step 2: Route to appropriate model with fallback
        return routeToModel(systemPrompt, cleanedMessage, recommendedModel);
    }

    /**
     * Preprocesses a message using flashx to assess complexity.
     */
    private PreprocessResult preprocessMessage(String message) {
        // Check if flashx is in skip mode (too many failures)
        if (shouldSkipModel(MODEL_FLASHX)) {
            LOGGER.warn("[Cascade] flashx in skip mode due to failures, using fallback");
            return new PreprocessResult(message, "MODERATE", MODEL_FLASH, "flashx skipped due to failures");
        }

        try {
            String response = sendRequest(MODEL_FLASHX, PREPROCESS_PROMPT, message, 500);

            // Parse JSON response
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String cleaned = json.has("cleaned_message") ? json.get("cleaned_message").getAsString() : message;
            String complexity = json.has("complexity") ? json.get("complexity").getAsString() : "MODERATE";
            String recommended = json.has("recommended_model") ? json.get("recommended_model").getAsString() : "flash";
            String reasoning = json.has("reasoning") ? json.get("reasoning").getAsString() : "";

            // Validate recommended model
            if (!recommended.equals("flashx") && !recommended.equals("flash") && !recommended.equals("glm5")) {
                recommended = "flash";  // Default to flash if invalid
            }

            return new PreprocessResult(cleaned, complexity, recommended, reasoning);

        } catch (Exception e) {
            LOGGER.error("[Cascade] Preprocessing failed: {}", e.getMessage());
            recordFailure(MODEL_FLASHX);
            // Fallback: use original message with moderate complexity
            return new PreprocessResult(message, "MODERATE", MODEL_FLASH, "preprocessing failed");
        }
    }

    /**
     * Routes to the appropriate model with fallback chain.
     */
    private String routeToModel(String systemPrompt, String message, String recommendedModel) {
        // For simple/flashx tasks, try local LLM first (FREE!)
        if ((recommendedModel.equals("flashx") || recommendedModel.equals("flash")) &&
            localLLM.isAvailable()) {
            try {
                LOGGER.info("[Cascade] Trying local LLM (Llama 3.2) first - FREE!");
                String response = localLLM.sendRequest(systemPrompt, message);
                if (response != null && !response.isEmpty()) {
                    LOGGER.info("[Cascade] Local LLM succeeded - no API cost!");
                    return response;
                }
            } catch (Exception e) {
                LOGGER.warn("[Cascade] Local LLM failed, falling back to cloud: {}", e.getMessage());
            }
        }

        // Build fallback chain based on recommendation
        String[] fallbackChain = switch (recommendedModel) {
            case "glm5" -> new String[]{MODEL_GLM5, MODEL_FLASH, MODEL_FLASHX};
            case "flashx" -> new String[]{MODEL_FLASHX, MODEL_FLASH, MODEL_GLM5};
            default -> new String[]{MODEL_FLASH, MODEL_FLASHX, MODEL_GLM5};
        };

        // Try each model in the fallback chain
        for (String model : fallbackChain) {
            if (shouldSkipModel(model)) {
                LOGGER.debug("[Cascade] Skipping {} due to recent failures", model);
                continue;
            }

            try {
                LOGGER.info("[Cascade] Trying cloud model: {}", model);
                String response = sendRequest(model, systemPrompt, message, 4096);
                recordSuccess(model);
                return response;
            } catch (Exception e) {
                LOGGER.error("[Cascade] Model {} failed: {}", model, e.getMessage());
                recordFailure(model);
            }
        }

        // All cloud models failed - try local as last resort
        if (localLLM.isAvailable()) {
            LOGGER.info("[Cascade] All cloud models failed, trying local LLM as fallback");
            String response = localLLM.sendRequest(systemPrompt, message);
            if (response != null && !response.isEmpty()) {
                return response;
            }
        }

        // All models failed - return error message
        return "Error: All LLM models are currently unavailable. Please try again later.";
    }

    /**
     * Sends a request to z.ai with the specified model.
     */
    private String sendRequest(String model, String systemPrompt, String userMessage, int maxTokens) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("API key not configured");
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ZAI_API_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
        }

        // Parse response
        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        if (responseJson.has("choices")) {
            JsonArray choices = responseJson.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                if (firstChoice.has("message")) {
                    return firstChoice.getAsJsonObject("message").get("content").getAsString();
                }
            }
        }

        throw new Exception("Invalid response format");
    }

    /**
     * Processes a vision request with glm-4.6v.
     *
     * @param systemPrompt System context
     * @param userMessage  User message
     * @param imageBase64  Base64-encoded image (PNG/JPEG)
     * @return Response from vision model
     */
    public String processVisionRequest(String systemPrompt, String userMessage, String imageBase64) {
        try {
            LOGGER.info("[Cascade] Processing vision request with glm-4.6v");

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL_VISION);
            requestBody.addProperty("max_tokens", 2048);

            JsonArray messages = new JsonArray();

            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);

            // Build multimodal content
            JsonArray content = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", userMessage);
            content.add(textPart);

            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", "data:image/png;base64," + imageBase64);
            imagePart.add("image_url", imageUrl);
            content.add(imagePart);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.add("content", content);
            messages.add(userMsg);

            requestBody.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ZAI_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.error("[Cascade] Vision request failed: {}", response.body());
                return "Vision analysis failed: API error";
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            if (responseJson.has("choices")) {
                JsonArray choices = responseJson.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    return choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                }
            }

            return "Vision analysis failed: Invalid response";

        } catch (Exception e) {
            LOGGER.error("[Cascade] Vision request error: {}", e.getMessage());
            return "Vision analysis failed: " + e.getMessage();
        }
    }

    // === Failure Tracking ===

    private boolean shouldSkipModel(String model) {
        AtomicInteger failures = failureCount.get(model);
        if (failures == null || failures.get() < MAX_FAILURES_BEFORE_SKIP) {
            return false;
        }

        // Check if we should reset the failure count
        Long lastFailure = lastFailureTime.get(model);
        if (lastFailure != null && System.currentTimeMillis() - lastFailure > FAILURE_RESET_MS) {
            failureCount.put(model, new AtomicInteger(0));
            return false;
        }

        return true;
    }

    private void recordFailure(String model) {
        failureCount.computeIfAbsent(model, k -> new AtomicInteger(0)).incrementAndGet();
        lastFailureTime.put(model, System.currentTimeMillis());
    }

    private void recordSuccess(String model) {
        failureCount.put(model, new AtomicInteger(0));
        lastFailureTime.remove(model);
    }

    // === Data Classes ===

    private record PreprocessResult(
        String cleanedMessage,
        String complexity,
        String recommendedModel,
        String reasoning
    ) {}
}
